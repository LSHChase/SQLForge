#!/usr/bin/env python3
"""Deterministic validation for repository-local Codex runtime integration."""

from __future__ import annotations

import json
import re
import subprocess
import sys
import tomllib
from contextlib import contextmanager
from pathlib import Path
from typing import Dict, Iterator, List


ROOT = Path(__file__).resolve().parent.parent
CURRENT_TASK_PATH = ROOT / ".codex" / "state" / "current-task.json"
SESSION_CONTEXT_PATH = ROOT / ".codex" / "state" / "session-context.json"
MASTER_PLAN_PATH = ROOT / "docs" / "plans" / "master-execution-plan.md"
TASKS_PATH = ROOT / "tasks.md"
TASKS_DONE_PATH = ROOT / "tasks-done.md"
CONFIG_PATH = ROOT / ".codex" / "config.toml"
MCP_POLICY_PATH = ROOT / ".codex" / "policy" / "mcp-policy.json"
RULES_PATH = ROOT / "docs" / "rules" / "codex-rules.md"
VALIDATION_RULES_PATH = ROOT / "docs" / "quality" / "validation-rules.md"
DOCS_README_PATH = ROOT / "docs" / "README.md"
OPERATIONS_README_PATH = ROOT / "docs" / "operations" / "README.md"
CONNECTORS_PATH = ROOT / "docs" / "security" / "connectors.md"
MCP_PLAYBOOK_PATH = ROOT / "docs" / "operations" / "codex-mcp-playbook.md"
MULTI_AGENT_PLAYBOOK_PATH = ROOT / "docs" / "operations" / "multi-agent-playbook.md"
MANIFEST_TEMPLATE_PATH = ROOT / "docs" / "exec-plans" / "templates" / "multi-agent-run.template.json"
PREPARE_SCRIPT_PATH = ROOT / "scripts" / "multi_agent_prepare.sh"
LAUNCH_SCRIPT_PATH = ROOT / "scripts" / "multi_agent_launch.sh"
COLLECT_SCRIPT_PATH = ROOT / "scripts" / "multi_agent_collect.sh"
AUTOPLAN_SCRIPT_PATH = ROOT / "scripts" / "multi_agent_autoplan.sh"


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


def load_toml(path: Path) -> Dict[str, object]:
    return tomllib.loads(path.read_text(encoding="utf-8"))


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


def expect_markers(path: Path, markers: List[str], label: str) -> None:
    content = path.read_text(encoding="utf-8")
    missing = [marker for marker in markers if marker not in content]
    expect(not missing, f"{label} missing markers: {', '.join(missing)}")


def collect_key_paths(payload: object, prefix: str = "") -> List[str]:
    if isinstance(payload, dict):
        paths: List[str] = []
        for key, value in payload.items():
            key_path = f"{prefix}.{key}" if prefix else str(key)
            paths.append(key_path)
            paths.extend(collect_key_paths(value, key_path))
        return paths
    if isinstance(payload, list):
        paths = []
        for index, value in enumerate(payload):
            key_path = f"{prefix}[{index}]"
            paths.extend(collect_key_paths(value, key_path))
        return paths
    return []


def validate_mcp_governance() -> str:
    expect(MCP_POLICY_PATH.exists(), "compiled MCP policy is missing")
    policy = load_json(MCP_POLICY_PATH)
    scope = policy.get("scope", {})
    expect(isinstance(scope, dict), "MCP policy scope is invalid")
    profile = scope.get("profile")
    expect(profile == "multi-agent-read-only-evidence", "MCP policy profile must remain multi-agent-read-only-evidence")
    expect(policy.get("rules") == ["R-170", "R-171", "R-172", "R-173", "R-174", "R-175", "R-176"], "MCP policy rule set drifted")
    expect(
        policy.get("validation_rules") == ["R-170", "R-171", "R-172", "R-173", "R-174", "R-175", "R-176"],
        "MCP policy validation-rule set drifted",
    )

    categories = policy.get("read_only_categories", [])
    expect(isinstance(categories, list), "MCP policy read_only_categories must be a list")
    expected_category_ids = {
        "observability_logs",
        "deployment_evidence",
        "object_storage_metadata",
        "external_requirements_tickets",
    }
    category_ids = {str(item.get("id")) for item in categories if isinstance(item, dict)}
    expect(category_ids == expected_category_ids, "MCP policy category set drifted from the approved read-only baseline")
    for item in categories:
        expect(isinstance(item, dict), "MCP policy category entry is invalid")
        expect(item.get("scope") == "read-only", f"MCP category {item.get('id')} must remain read-only")
        expect(bool(item.get("forbidden_operations")), f"MCP category {item.get('id')} must declare forbidden operations")

    runtime_constraints = policy.get("runtime_constraints", {})
    expect(isinstance(runtime_constraints, dict), "MCP runtime constraints must be an object")
    expect(runtime_constraints.get("allow_repo_tracked_mcp_config") is False, "repo-tracked MCP config must stay disabled")
    expect(runtime_constraints.get("allow_repo_tracked_mcp_profile") is True, "manifest-level MCP profile must stay enabled for HARN-035")
    expect(runtime_constraints.get("allow_repo_tracked_server_inventory") is False, "repo-tracked MCP server inventory must stay disabled")
    expect(runtime_constraints.get("allow_repo_stored_secrets") is False, "repo-stored MCP secrets must stay disabled")
    expect(runtime_constraints.get("main_foreman_is_only_writeback_entry") is True, "Main Foreman write-back boundary drifted")
    expect(runtime_constraints.get("multi_agent_mcp_profile_enabled") is True, "multi-agent MCP profile must stay enabled in HARN-035")

    multi_agent_contract = policy.get("multi_agent_contract", {})
    expect(isinstance(multi_agent_contract, dict), "MCP multi-agent contract must be an object")
    expect(multi_agent_contract.get("manifest_registry_key") == "mcp_profiles", "MCP manifest registry key drifted")
    expect(multi_agent_contract.get("per_agent_profile_key") == "mcp_profile", "MCP per-agent profile key drifted")
    expect(multi_agent_contract.get("allowed_roles") == ["explorer", "validator"], "MCP allowed roles drifted")
    expect(multi_agent_contract.get("disallowed_roles") == ["worker"], "MCP disallowed roles drifted")

    for path in [
        CONNECTORS_PATH,
        MCP_PLAYBOOK_PATH,
        MULTI_AGENT_PLAYBOOK_PATH,
        DOCS_README_PATH,
        OPERATIONS_README_PATH,
        RULES_PATH,
        VALIDATION_RULES_PATH,
        MANIFEST_TEMPLATE_PATH,
    ]:
        expect(path.exists(), f"Required MCP governance document is missing: {path.relative_to(ROOT)}")

    expect_markers(DOCS_README_PATH, ["./security/connectors.md", "./operations/codex-mcp-playbook.md"], "docs/README.md")
    expect_markers(OPERATIONS_README_PATH, ["./codex-mcp-playbook.md"], "docs/operations/README.md")
    expect_markers(RULES_PATH, ["R-170", "R-171", "R-172", "R-173", "R-174", "R-175", "R-176"], "docs/rules/codex-rules.md")
    expect_markers(
        VALIDATION_RULES_PATH,
        ["R-170", "R-171", "R-172", "R-173", "R-174", "R-175", "R-176"],
        "docs/quality/validation-rules.md",
    )
    expect_markers(
        CONNECTORS_PATH,
        ["观测/日志", "部署证据", "对象存储元数据", "外部需求/工单检索", "Main Foreman", "explorer / validator"],
        "docs/security/connectors.md",
    )
    expect_markers(
        MCP_PLAYBOOK_PATH,
        ["compile-governance", "validate_codex_runtime.py", "mcp-policy.json", "Main Foreman", "mcp_profile", "explorer/validator"],
        "docs/operations/codex-mcp-playbook.md",
    )
    expect_markers(
        MULTI_AGENT_PLAYBOOK_PATH,
        ["mcp_profile", "mcp_profiles", "explorer / validator", "worker", "Main Foreman"],
        "docs/operations/multi-agent-playbook.md",
    )
    expect_markers(PREPARE_SCRIPT_PATH, ["mcp_profiles", "mcp_profile"], "scripts/multi_agent_prepare.sh")
    expect_markers(LAUNCH_SCRIPT_PATH, ["mcp_profiles", "mcp_profile"], "scripts/multi_agent_launch.sh")
    expect_markers(COLLECT_SCRIPT_PATH, ["mcp_profiles", "mcp_profile"], "scripts/multi_agent_collect.sh")
    expect_markers(AUTOPLAN_SCRIPT_PATH, ["mcp_profiles", "mcp_profile"], "scripts/multi_agent_autoplan.sh")

    manifest_template = load_json(MANIFEST_TEMPLATE_PATH)
    template_profiles = manifest_template.get("mcp_profiles", {})
    expect(isinstance(template_profiles, dict) and "readonly-evidence" in template_profiles, "Manifest template is missing readonly-evidence mcp profile")
    readonly_profile = template_profiles["readonly-evidence"]
    expect(readonly_profile.get("allowed_roles") == ["explorer", "validator"], "Manifest template MCP roles drifted")
    template_agents = manifest_template.get("agents", [])
    template_agent_profiles = {str(agent.get("name")): str(agent.get("mcp_profile", "")) for agent in template_agents if isinstance(agent, dict)}
    expect(template_agent_profiles.get("truth-explorer") == "readonly-evidence", "truth-explorer template MCP profile drifted")
    expect(template_agent_profiles.get("validator") == "readonly-evidence", "validator template MCP profile drifted")
    expect(template_agent_profiles.get("worker-name", "") == "", "worker template must not declare mcp_profile")

    config = load_toml(CONFIG_PATH)
    mcp_keys = [key for key in collect_key_paths(config) if "mcp" in key.lower()]
    expect(
        not mcp_keys,
        ".codex/config.toml must not declare repo-tracked live MCP runtime config: " + ", ".join(sorted(mcp_keys)),
    )
    return str(profile)


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
        mcp_policy_profile = validate_mcp_governance()

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
            "mcp_policy_profile": mcp_policy_profile,
        }
        sys.stdout.write(json.dumps(report, ensure_ascii=True, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
