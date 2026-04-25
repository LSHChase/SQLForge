#!/usr/bin/env python3
"""Local, read-only MCP governance doctor for SQLForge."""

from __future__ import annotations

import argparse
import json
import tomllib
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent.parent
POLICY_PATH = ROOT / ".codex" / "policy" / "mcp-policy.json"
CONFIG_PATH = ROOT / ".codex" / "config.toml"
CONNECTORS_PATH = ROOT / "docs" / "security" / "connectors.md"
PLAYBOOK_PATH = ROOT / "docs" / "operations" / "codex-mcp-playbook.md"
MULTI_AGENT_PLAYBOOK_PATH = ROOT / "docs" / "operations" / "multi-agent-playbook.md"
DOCS_README_PATH = ROOT / "docs" / "README.md"
OPERATIONS_README_PATH = ROOT / "docs" / "operations" / "README.md"
MANIFEST_TEMPLATE_PATH = ROOT / "docs" / "exec-plans" / "templates" / "multi-agent-run.template.json"

EXPECTED_CATEGORY_IDS = [
    "observability_logs",
    "deployment_evidence",
    "object_storage_metadata",
    "external_requirements_tickets",
]
CATEGORY_HEADINGS = [
    "### 观测/日志 Onboarding",
    "### 部署证据 Onboarding",
    "### 对象存储元数据 Onboarding",
    "### 外部需求/工单检索 Onboarding",
]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(read_text(path))


def load_toml(path: Path) -> dict[str, Any]:
    return tomllib.loads(read_text(path))


def collect_key_paths(payload: object, prefix: str = "") -> list[str]:
    if isinstance(payload, dict):
        paths: list[str] = []
        for key, value in payload.items():
            key_path = f"{prefix}.{key}" if prefix else str(key)
            paths.append(key_path)
            paths.extend(collect_key_paths(value, key_path))
        return paths
    if isinstance(payload, list):
        paths: list[str] = []
        for index, value in enumerate(payload):
            key_path = f"{prefix}[{index}]"
            paths.extend(collect_key_paths(value, key_path))
        return paths
    return []


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate SQLForge MCP onboarding and governance readiness without contacting remote systems.")
    parser.add_argument("--check", action="store_true", help="Run the doctor checks.")
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON output.")
    args = parser.parse_args()

    if not args.check:
        parser.error("Only --check is supported.")

    checks: list[str] = []
    issues: list[str] = []

    if not POLICY_PATH.exists():
        issues.append(f"Missing compiled policy: {POLICY_PATH.relative_to(ROOT)}")
    else:
        policy = load_json(POLICY_PATH)
        if policy.get("scope", {}).get("profile") != "multi-agent-read-only-evidence":
            issues.append("Compiled MCP policy profile drifted from multi-agent-read-only-evidence")
        category_ids = {str(item.get("id")) for item in policy.get("read_only_categories", []) if isinstance(item, dict)}
        missing_category_ids = [item for item in EXPECTED_CATEGORY_IDS if item not in category_ids]
        if missing_category_ids:
            issues.append("Compiled MCP policy is missing approved read-only categories: " + ", ".join(missing_category_ids))
        runtime_constraints = policy.get("runtime_constraints", {})
        if runtime_constraints.get("allow_repo_tracked_mcp_config") is not False:
            issues.append("Compiled policy allows repo-tracked MCP config")
        if runtime_constraints.get("allow_repo_tracked_server_inventory") is not False:
            issues.append("Compiled policy allows repo-tracked server inventory")
        if runtime_constraints.get("allow_repo_stored_secrets") is not False:
            issues.append("Compiled policy allows repo-stored secrets")
        if runtime_constraints.get("main_foreman_is_only_writeback_entry") is not True:
            issues.append("Compiled policy drifted from Main Foreman only write-back boundary")
        if runtime_constraints.get("multi_agent_mcp_profile_enabled") is not True:
            issues.append("Compiled policy no longer enables governed multi-agent mcp_profile boundary")
        checks.append("compiled mcp-policy.json boundary verified")

    for path in [CONNECTORS_PATH, PLAYBOOK_PATH, MULTI_AGENT_PLAYBOOK_PATH, DOCS_README_PATH, OPERATIONS_README_PATH, MANIFEST_TEMPLATE_PATH]:
        if not path.exists():
            issues.append(f"Missing required MCP governance file: {path.relative_to(ROOT)}")

    if CONNECTORS_PATH.exists():
        connectors = read_text(CONNECTORS_PATH)
        for marker in [
            "受治理的只读证据增强",
            "不是远端自动运维",
            "不是可写控制面",
            "Evidence write-back target",
            "Local prerequisites",
            "Local runtime location",
            "python3 scripts/mcp_doctor.py --check",
        ]:
            if marker not in connectors:
                issues.append(f"docs/security/connectors.md missing marker: {marker}")
        for heading in CATEGORY_HEADINGS:
            if heading not in connectors:
                issues.append(f"docs/security/connectors.md missing onboarding section: {heading}")
        checks.append("connector security boundary and onboarding markers verified")

    if PLAYBOOK_PATH.exists():
        playbook = read_text(PLAYBOOK_PATH)
        for marker in [
            "受治理的只读证据增强",
            "不是远端自动运维",
            "python3 scripts/mcp_doctor.py --check",
            "governed_healthcheck.py --check",
            "Evidence write-back target",
            "observability_logs",
            "deployment_evidence",
            "object_storage_metadata",
            "external_requirements_tickets",
        ]:
            if marker not in playbook:
                issues.append(f"docs/operations/codex-mcp-playbook.md missing marker: {marker}")
        checks.append("mcp playbook markers verified")

    if MULTI_AGENT_PLAYBOOK_PATH.exists():
        multi_agent = read_text(MULTI_AGENT_PLAYBOOK_PATH)
        for marker in [
            "受治理的只读证据增强",
            "不是远端自动运维",
            "python3 scripts/mcp_doctor.py --check",
            "mcp_profile",
            "explorer / validator",
            "worker",
            "Main Foreman",
        ]:
            if marker not in multi_agent:
                issues.append(f"docs/operations/multi-agent-playbook.md missing marker: {marker}")
        checks.append("multi-agent MCP boundary markers verified")

    if DOCS_README_PATH.exists():
        docs_readme = read_text(DOCS_README_PATH)
        for marker in ["./security/connectors.md", "./operations/codex-mcp-playbook.md", "受治理的只读证据增强"]:
            if marker not in docs_readme:
                issues.append(f"docs/README.md missing MCP marker: {marker}")
        checks.append("docs/README MCP references verified")

    if OPERATIONS_README_PATH.exists():
        ops_readme = read_text(OPERATIONS_README_PATH)
        for marker in ["./codex-mcp-playbook.md", "onboarding", "doctor"]:
            if marker not in ops_readme:
                issues.append(f"docs/operations/README.md missing MCP marker: {marker}")
        checks.append("operations README MCP references verified")

    if CONFIG_PATH.exists():
        config = load_toml(CONFIG_PATH)
        mcp_keys = [key for key in collect_key_paths(config) if "mcp" in key.lower()]
        if mcp_keys:
            issues.append(".codex/config.toml contains repo-tracked MCP runtime config: " + ", ".join(sorted(mcp_keys)))
        checks.append(".codex/config.toml remains free of repo-tracked MCP runtime config")

    if MANIFEST_TEMPLATE_PATH.exists():
        manifest_template = load_json(MANIFEST_TEMPLATE_PATH)
        profiles = manifest_template.get("mcp_profiles", {})
        readonly_profile = profiles.get("readonly-evidence", {}) if isinstance(profiles, dict) else {}
        if readonly_profile.get("allowed_roles") != ["explorer", "validator"]:
            issues.append("readonly-evidence manifest profile drifted from [explorer, validator]")
        agents = manifest_template.get("agents", [])
        worker = next((item for item in agents if isinstance(item, dict) and item.get("name") == "worker-name"), {})
        if "mcp_profile" in worker:
            issues.append("worker-name in manifest template must not declare mcp_profile")
        checks.append("manifest template read-only evidence boundary verified")

    payload = {
        "status": "passed" if not issues else "failed",
        "checks": checks,
        "issues": issues,
        "read_only_categories": EXPECTED_CATEGORY_IDS,
        "positioning": "受治理的只读证据增强",
        "doctor_mode": "local-only",
        "recommended_next_step": "Run python3 scripts/validate_codex_runtime.py and governed task validation after resolving any doctor issues."
        if issues
        else "MCP governance baseline, onboarding docs, and local-only doctor checks are aligned.",
    }

    if args.json:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    else:
        if issues:
            print("MCP doctor found issues:")
            for issue in issues:
                print(f"- {issue}")
        else:
            print("MCP doctor passed.")
            for item in checks:
                print(f"- {item}")
    return 0 if not issues else 1


if __name__ == "__main__":
    raise SystemExit(main())
