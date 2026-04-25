#!/usr/bin/env python3
"""Deterministic validation for repository-local Codex runtime integration."""

from __future__ import annotations

import json
import re
import subprocess
import sys
from contextlib import contextmanager
from pathlib import Path
from typing import Dict, Iterator


ROOT = Path(__file__).resolve().parent.parent
CURRENT_TASK_PATH = ROOT / ".codex" / "state" / "current-task.json"
SESSION_CONTEXT_PATH = ROOT / ".codex" / "state" / "session-context.json"
MASTER_PLAN_PATH = ROOT / "docs" / "plans" / "master-execution-plan.md"
TASKS_PATH = ROOT / "tasks.md"
TASKS_DONE_PATH = ROOT / "tasks-done.md"


def run(command: list[str], stdin: str | None = None, timeout: int = 30) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        cwd=ROOT,
        input=stdin,
        capture_output=True,
        text=True,
        timeout=timeout,
    )


def expect(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(message)


def load_json(path: Path) -> Dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def select_unbound_matrix_task() -> str:
    task_ids = re.findall(r"`([A-Z]+-TASK-\d+)`", MASTER_PLAN_PATH.read_text(encoding="utf-8"))
    ledger_text = TASKS_PATH.read_text(encoding="utf-8") + "\n" + TASKS_DONE_PATH.read_text(encoding="utf-8")
    ledger_ids = set(re.findall(r"([A-Z]+-TASK-\d+)", ledger_text))
    for task_id in task_ids:
        if task_id not in ledger_ids:
            return task_id
    raise SystemExit("No unbound matrix task is available for instantiate dry-run validation.")


@contextmanager
def preserved_state(paths: list[Path]) -> Iterator[None]:
    originals = {}
    for path in paths:
        originals[path] = path.read_text(encoding="utf-8") if path.exists() else None
    try:
        yield
    finally:
        for path, content in originals.items():
            if content is None:
                if path.exists():
                    path.unlink()
            else:
                save_text(path, content)


def run_hook(path: str, payload: Dict[str, object]) -> Dict[str, object]:
    result = run(["python3", path], stdin=json.dumps(payload))
    expect(result.returncode == 0, f"{path} failed: {result.stderr or result.stdout}")
    output = result.stdout.strip() or "{}"
    return json.loads(output)


def maybe_attempt_codex_exec() -> str:
    try:
        result = run(
            [
                "codex",
                "exec",
                "--json",
                "--sandbox",
                "read-only",
                "--skip-git-repo-check",
                "Reply with OK only.",
            ],
            timeout=45,
        )
    except subprocess.TimeoutExpired:
        return "skipped-timeout"
    if result.returncode == 0:
        return "passed"
    if any(token in (result.stderr + result.stdout).lower() for token in ("login", "auth", "credential", "unauthorized")):
        return "skipped-auth"
    return "failed"


def main() -> int:
    with preserved_state([CURRENT_TASK_PATH, SESSION_CONTEXT_PATH]):
        version_check = run(["codex", "--version"])
        expect(version_check.returncode == 0, version_check.stderr or "codex --version failed")

        preflight = run(
            [
                "python3",
                "scripts/foreman.py",
                "preflight",
                "--task",
                "HARN-008",
                "--task-class",
                "standard",
                "--prompt",
                "Close the 6 remaining governance runtime gaps under strict mode.",
            ]
        )
        expect(preflight.returncode == 0, preflight.stderr or preflight.stdout)
        current = load_json(CURRENT_TASK_PATH)
        session = load_json(SESSION_CONTEXT_PATH)
        expect(current.get("task_id") == "HARN-008", "preflight did not bind HARN-008")
        expect(current.get("phase") == "preflight", "preflight did not update runtime phase")
        expect(bool(session.get("authority_digest")), "preflight did not materialize authority digest")

        instantiate_task_id = select_unbound_matrix_task()
        instantiate = run(["python3", "scripts/foreman.py", "instantiate", instantiate_task_id, "--dry-run"])
        expect(instantiate.returncode == 0, instantiate.stderr or instantiate.stdout)
        instantiate_payload = json.loads(instantiate.stdout)
        expect(instantiate_payload["task_id"] == instantiate_task_id, "matrix instantiate returned the wrong task id")
        expect(bool(instantiate_payload["task_class"]), "matrix instantiate did not infer task class")
        expect(bool(instantiate_payload["depends_on"]), "matrix instantiate did not read dependencies")
        expect(bool(instantiate_payload["human_confirmation_point"]), "matrix instantiate missed governance extension fields")

        delivery = run(
            [
                "python3",
                "scripts/foreman.py",
                "delivery-closeout",
                "F-TASK-009",
                "--tag",
                "dry-run-F-TASK-009",
                "--dry-run",
            ]
        )
        expect(delivery.returncode == 0, delivery.stderr or delivery.stdout)
        delivery_payload = json.loads(delivery.stdout)
        expect(delivery_payload["target"] == "F-TASK-009", "delivery-closeout dry-run did not preserve target")

        user_prompt_payload = run_hook(
            ".codex/hooks/user_prompt_submit.py",
            {"hook_event_name": "UserPromptSubmit", "prompt": "Continue HARN-008 runtime hardening."},
        )
        expect("hookSpecificOutput" in user_prompt_payload, "UserPromptSubmit hook missing hookSpecificOutput")
        expect(
            user_prompt_payload["hookSpecificOutput"].get("hookEventName") == "UserPromptSubmit",
            "UserPromptSubmit hook emitted wrong event name",
        )
        expect(
            bool(user_prompt_payload["hookSpecificOutput"].get("additionalContext")),
            "UserPromptSubmit hook did not emit additionalContext",
        )

        pre_tool_payload = run_hook(
            ".codex/hooks/pre_tool_use.py",
            {"tool_name": "Bash", "tool_input": {"command": "git add ."}},
        )
        expect(
            pre_tool_payload.get("hookSpecificOutput", {}).get("permissionDecision") == "deny",
            "PreToolUse hook did not deny audit-breaking command",
        )

        permission_deny = run_hook(
            ".codex/hooks/permission_request.py",
            {"tool_name": "Bash", "tool_input": {"command": "git push origin HEAD"}},
        )
        expect(
            permission_deny.get("hookSpecificOutput", {}).get("permissionDecision") == "deny",
            "PermissionRequest hook did not deny publish/push command",
        )

        permission_pass = run_hook(
            ".codex/hooks/permission_request.py",
            {"tool_name": "Bash", "tool_input": {"command": "python3 scripts/foreman.py preflight"}},
        )
        expect(
            "hookSpecificOutput" not in permission_pass,
            "PermissionRequest hook should not auto-decide ordinary in-repo commands",
        )

        implementation_state = load_json(CURRENT_TASK_PATH)
        implementation_state["phase"] = "implementation"
        implementation_state["status"] = "active"
        save_text(CURRENT_TASK_PATH, json.dumps(implementation_state, ensure_ascii=True, indent=2) + "\n")
        stop_allow = run_hook(".codex/hooks/stop.py", {"hook_event_name": "Stop"})
        expect(stop_allow == {}, "Stop hook should pass through during implementation phase")

        done_ready_state = load_json(CURRENT_TASK_PATH)
        done_ready_state["phase"] = "done_ready"
        done_ready_state["status"] = "active"
        done_ready_state.setdefault("closeout", {})["done_ready"] = True
        save_text(CURRENT_TASK_PATH, json.dumps(done_ready_state, ensure_ascii=True, indent=2) + "\n")
        stop_block = run_hook(".codex/hooks/stop.py", {"hook_event_name": "Stop"})
        expect(stop_block.get("decision") == "block", "Stop hook did not block done_ready stop")

        codex_exec_status = maybe_attempt_codex_exec()
        report = {
            "codex_version": version_check.stdout.strip(),
            "authority_digest_count": len(session.get("authority_digest", [])),
            "instantiate_matrix_task": instantiate_payload["task_id"],
            "codex_exec_status": codex_exec_status,
        }
        sys.stdout.write(json.dumps(report, ensure_ascii=True, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
