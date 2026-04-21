#!/usr/bin/env python3
"""Unified Codex/foreman governance entrypoint for SQLForge."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List


ROOT = Path(__file__).resolve().parent.parent
DOCS_DIR = ROOT / "docs"
CODEX_DIR = ROOT / ".codex"
POLICY_DIR = CODEX_DIR / "policy"
STATE_DIR = CODEX_DIR / "state"
TASKS_PATH = ROOT / "tasks.md"
VALIDATION_LOG_PATH = DOCS_DIR / "quality" / "validation-log.md"
SESSION_CONTEXT_PATH = STATE_DIR / "session-context.json"
CURRENT_TASK_PATH = STATE_DIR / "current-task.json"
BLUEPRINT_PATH = DOCS_DIR / "plans" / "codex-governance-integration-blueprint.md"
ACTIVE_PLAN_PATH = DOCS_DIR / "exec-plans" / "active" / "HARN-007-codex-runtime-integration-plan.md"

AUTHORITY_ENTRY = [
    "docs/README.md",
    "docs/plans/document-truth-baseline.md",
    "docs/architecture/init.md",
    "docs/rules/codex-rules.md",
    "docs/quality/validation-rules.md",
    "docs/plans/master-execution-plan.md",
    "docs/plans/phase-prerequisite-matrix.md",
    "docs/plans/task-spec-matrix.md",
    "docs/plans/task-governance-extension-matrix.md",
    "tasks.md",
    "tasks-done.md",
    "INBOX.md",
]

DEFAULT_POLICY_FILES = {
    "authority-map.json": {
        "source": "docs/README.md",
        "entries": AUTHORITY_ENTRY,
    },
    "task-policy.json": {
        "task_classes": {
            "advisory": {"requires_ledger": False, "delivery_scope": False},
            "trivial": {"requires_ledger": False, "delivery_scope": False},
            "standard": {"requires_ledger": True, "delivery_scope": False},
            "delivery": {"requires_ledger": True, "delivery_scope": True},
        }
    },
    "closeout-policy.json": {
        "task_closeout_steps": [
            "validate",
            "write_context_closeout",
            "archive_task",
            "task_audit_pre_closeout",
            "single_task_commit",
            "task_audit_post_closeout"
        ],
        "delivery_closeout_extra_steps": ["tag", "writeback_delivery_records"]
    },
    "source-anchors.json": {
        "blueprint": "docs/plans/codex-governance-integration-blueprint.md",
        "active_exec_plan": "docs/exec-plans/active/HARN-007-codex-runtime-integration-plan.md",
        "authority_entry": AUTHORITY_ENTRY,
    }
}


@dataclass
class CommandResult:
    code: int
    stdout: str
    stderr: str


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def read_json(path: Path, default: Dict[str, Any] | None = None) -> Dict[str, Any]:
    if not path.exists():
        return {} if default is None else default
    return json.loads(read_text(path))


def write_json(path: Path, payload: Dict[str, Any]) -> None:
    write_text(path, json.dumps(payload, ensure_ascii=True, indent=2) + "\n")


def run(command: List[str], cwd: Path | None = None) -> CommandResult:
    process = subprocess.run(
        command,
        cwd=cwd or ROOT,
        capture_output=True,
        text=True,
    )
    return CommandResult(process.returncode, process.stdout, process.stderr)


def now_iso() -> str:
    return run(["date", "--iso-8601=seconds"]).stdout.strip()


def compact(text: str, limit: int = 160) -> str:
    normalized = " ".join(text.split())
    return normalized[: limit - 3] + "..." if len(normalized) > limit else normalized


def hash_payload(payload: Dict[str, Any]) -> str:
    return hashlib.sha256(json.dumps(payload, sort_keys=True).encode("utf-8")).hexdigest()


def classify_task(prompt: str, explicit: str | None = None) -> str:
    if explicit:
        return explicit
    lowered = prompt.lower()
    if any(word in lowered for word in ("分析", "analysis", "explain", "why", "review only")):
        return "advisory"
    if any(word in lowered for word in ("phase", "delivery", "f-task", "tag", "write-back", "writeback")):
        return "delivery"
    if any(word in lowered for word in ("typo", "small doc", "trivial", "rename one line")):
        return "trivial"
    return "standard"


def requires_ledger(task_class: str) -> bool:
    return bool(DEFAULT_POLICY_FILES["task-policy.json"]["task_classes"][task_class]["requires_ledger"])


def build_preflight_context(prompt: str, task: str, task_class: str) -> Dict[str, Any]:
    return {
        "version": 1,
        "task_id": task,
        "task_class": task_class,
        "summary": (
            f"Strict-mode preflight for {task or 'unbound task'} classified as {task_class}. "
            f"Authority entry starts at {AUTHORITY_ENTRY[0]} and ends at {AUTHORITY_ENTRY[-1]}."
        ),
        "authority_entry": AUTHORITY_ENTRY,
        "prompt_excerpt": compact(prompt or ""),
        "requires_ledger": requires_ledger(task_class),
        "generated_at": now_iso(),
    }


def write_runtime_state(task_id: str, task_class: str, phase: str, bound_to_ledger: bool) -> Dict[str, Any]:
    state = {
        "version": 1,
        "task_id": task_id,
        "task_class": task_class,
        "phase": phase,
        "status": "active" if phase != "blocked" else "blocked",
        "plan_ref": f"docs/exec-plans/active/{ACTIVE_PLAN_PATH.name}" if task_id else "",
        "bound_to_ledger": bound_to_ledger,
        "strict_mode": True,
        "requires_human_decision": False,
        "delivery_scope": task_class == "delivery",
        "preflight": {
            "completed": phase != "idle",
            "timestamp": now_iso(),
            "context_hash": "",
        },
        "validation": {
            "last_run_at": "",
            "passed": False,
            "evidence_refs": [],
        },
        "closeout": {
            "done_ready": False,
            "pre_closeout_audit_passed": False,
            "commit_created": False,
            "post_closeout_audit_passed": False,
            "delivery_writeback_completed": False,
        },
        "traceability": {
            "tasks_md_ref": f"tasks.md#{task_id}" if task_id and bound_to_ledger else "",
            "inbox_ref": "",
            "done_md_ref": "",
        },
        "updated_at": now_iso(),
    }
    write_json(CURRENT_TASK_PATH, state)
    return state


def command_preflight(args: argparse.Namespace) -> int:
    task_id = args.task or ""
    task_class = classify_task(args.prompt or "", args.task_class)
    context = build_preflight_context(args.prompt or "", task_id, task_class)
    write_json(SESSION_CONTEXT_PATH, context)
    state = write_runtime_state(task_id, task_class, "preflight", bool(task_id and requires_ledger(task_class)))
    state["preflight"]["context_hash"] = "sha256:" + hash_payload(context)
    write_json(CURRENT_TASK_PATH, state)
    print(context["summary"])
    return 0


def active_task_exists(task_id: str) -> bool:
    return f"### {task_id}:" in read_text(TASKS_PATH)


def insert_in_progress_block(content: str, block: str) -> str:
    marker = "## In Progress\n"
    start = content.find(marker)
    if start == -1:
        raise SystemExit("tasks.md is missing the '## In Progress' section.")
    start += len(marker)
    end = content.find("\n## ", start)
    if end == -1:
        end = len(content)
    section = content[start:end]
    replacement = "\n" + block.strip() + "\n\n"
    if "_No tasks._" in section:
        section = section.replace("_No tasks._", replacement.strip())
    else:
        section = replacement + section.lstrip("\n")
    return content[:start] + section + content[end:]


def command_instantiate(args: argparse.Namespace) -> int:
    if active_task_exists(args.task_id):
        raise SystemExit(f"Task {args.task_id} already exists in tasks.md.")
    content = read_text(TASKS_PATH)
    block = f"""### {args.task_id}: {args.title}

- Status: in_progress
- Priority: {args.priority}
- Depends on: {args.depends_on}
- Scope: {args.scope}
- Validation:
  - `python3 scripts/foreman.py validate {args.task_id}`
- Progress log:
  - {now_iso()[:10]}: instantiated from foreman CLI with task class `{args.task_class}`.
"""
    write_text(TASKS_PATH, insert_in_progress_block(content, block))
    write_runtime_state(args.task_id, args.task_class, "discovery", bound_to_ledger=requires_ledger(args.task_class))
    print(f"Instantiated {args.task_id} into tasks.md")
    return 0


def command_sync_context(args: argparse.Namespace) -> int:
    state = read_json(CURRENT_TASK_PATH, default={})
    if not state:
        raise SystemExit("current-task.json does not exist; run preflight first.")
    if args.phase:
        state["phase"] = args.phase
        state["status"] = "blocked" if args.phase == "blocked" else "active"
    if args.done_ready:
        state.setdefault("closeout", {})["done_ready"] = True
        state["phase"] = "done_ready"
    state["updated_at"] = now_iso()
    write_json(CURRENT_TASK_PATH, state)
    print(f"Updated runtime context for {state.get('task_id', 'unknown task')}")
    return 0


def append_validation_log(label: str, command: str, rules: str, status: str) -> None:
    line = f"{now_iso()} | {label} | {rules} | {status} | `{command}`\n"
    with VALIDATION_LOG_PATH.open("a", encoding="utf-8") as handle:
        handle.write(line)


def command_validate(args: argparse.Namespace) -> int:
    commands = [
        ["python3", "-m", "py_compile", "scripts/foreman.py"],
        ["node", "scripts/lint-repository-knowledge.js"],
    ]
    if args.include_task_audit:
        commands.append(["python3", "scripts/task_audit.py", "--check", "--phase", "pre-closeout"])

    failures: List[str] = []
    for command in commands:
        result = run(command)
        command_text = " ".join(command)
        append_validation_log(f"{args.task} validate", command_text, "`R-133`, `R-160`", "passed" if result.code == 0 else "failed")
        if result.code != 0:
            failures.append(f"{command_text}\n{result.stderr or result.stdout}")

    state = read_json(CURRENT_TASK_PATH, default={})
    if state:
        state.setdefault("validation", {})["last_run_at"] = now_iso()
        state["validation"]["passed"] = not failures
        state["validation"]["evidence_refs"] = ["docs/quality/validation-log.md"]
        state["phase"] = "validation" if failures else "done_ready"
        state.setdefault("closeout", {})["done_ready"] = not failures
        state["updated_at"] = now_iso()
        write_json(CURRENT_TASK_PATH, state)

    if failures:
        raise SystemExit("\n\n".join(failures))
    print(f"Validation passed for {args.task}")
    return 0


def command_audit(args: argparse.Namespace) -> int:
    result = run(["python3", "scripts/task_audit.py", "--check", "--phase", args.phase])
    sys.stdout.write(result.stdout)
    sys.stderr.write(result.stderr)
    return result.code


def compiled_payloads() -> Dict[str, Dict[str, Any]]:
    blueprint = read_text(BLUEPRINT_PATH)
    active_plan = read_text(ACTIVE_PLAN_PATH)
    common_meta = {
        "blueprint_sha256": hashlib.sha256(blueprint.encode("utf-8")).hexdigest(),
        "active_plan_sha256": hashlib.sha256(active_plan.encode("utf-8")).hexdigest(),
    }
    payloads: Dict[str, Dict[str, Any]] = {}
    for filename, payload in DEFAULT_POLICY_FILES.items():
        payloads[filename] = {"metadata": common_meta, **payload}
    return payloads


def command_compile_governance(args: argparse.Namespace) -> int:
    POLICY_DIR.mkdir(parents=True, exist_ok=True)
    mismatches: List[str] = []
    for filename, payload in compiled_payloads().items():
        target = POLICY_DIR / filename
        rendered = json.dumps(payload, ensure_ascii=True, indent=2) + "\n"
        if args.check:
            if not target.exists() or read_text(target) != rendered:
                mismatches.append(str(target.relative_to(ROOT)))
        else:
            write_text(target, rendered)
    if args.check:
        if mismatches:
            raise SystemExit("Governance policy drift detected:\n- " + "\n- ".join(mismatches))
        print("Governance policy files are in sync.")
        return 0
    print(f"Compiled governance policy files into {POLICY_DIR.relative_to(ROOT)}")
    return 0


def command_closeout(args: argparse.Namespace) -> int:
    raise SystemExit(
        "closeout scaffolding is defined, but repository-integrated automatic archival and commit "
        "will be activated after task-specific closeout text and file scopes are finalized."
    )


def command_delivery_closeout(args: argparse.Namespace) -> int:
    raise SystemExit(
        "delivery-closeout is reserved for delivery-class tasks and is not executed in this bootstrap batch."
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="SQLForge foreman / Codex governance entrypoint.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    preflight = subparsers.add_parser("preflight")
    preflight.add_argument("--prompt", default="")
    preflight.add_argument("--task", default="")
    preflight.add_argument("--task-class", choices=["advisory", "trivial", "standard", "delivery"])
    preflight.set_defaults(func=command_preflight)

    instantiate = subparsers.add_parser("instantiate")
    instantiate.add_argument("task_id")
    instantiate.add_argument("--title", required=True)
    instantiate.add_argument("--task-class", choices=["advisory", "trivial", "standard", "delivery"], default="standard")
    instantiate.add_argument("--priority", default="1")
    instantiate.add_argument("--depends-on", default="N/A")
    instantiate.add_argument("--scope", required=True)
    instantiate.set_defaults(func=command_instantiate)

    sync_context = subparsers.add_parser("sync-context")
    sync_context.add_argument("--phase", choices=["preflight", "discovery", "implementation", "validation", "done_ready", "closeout", "delivery_closeout", "closed", "blocked"])
    sync_context.add_argument("--done-ready", action="store_true")
    sync_context.set_defaults(func=command_sync_context)

    validate = subparsers.add_parser("validate")
    validate.add_argument("task")
    validate.add_argument("--include-task-audit", action="store_true")
    validate.set_defaults(func=command_validate)

    audit = subparsers.add_parser("audit")
    audit.add_argument("--phase", choices=["pre-closeout", "post-closeout"], default="pre-closeout")
    audit.set_defaults(func=command_audit)

    compile_governance = subparsers.add_parser("compile-governance")
    compile_governance.add_argument("--check", action="store_true")
    compile_governance.set_defaults(func=command_compile_governance)

    closeout = subparsers.add_parser("closeout")
    closeout.add_argument("task")
    closeout.set_defaults(func=command_closeout)

    delivery_closeout = subparsers.add_parser("delivery-closeout")
    delivery_closeout.add_argument("target")
    delivery_closeout.set_defaults(func=command_delivery_closeout)

    return parser


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
