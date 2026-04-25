#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

TASK_ID=""
REQUIREMENTS_FILE=""
PROMPT_TEXT=""
PLAN_OUTPUT=""
MANIFEST_OUTPUT=""
MODEL=""
PROFILE=""
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/multi_agent_autoplan.sh --task <TASK_ID> (--requirements-file <FILE> | --prompt <TEXT>) [options]

Options:
  --task <TASK_ID>              Bound task id. Must match current foreman preflight context.
  --requirements-file <FILE>    Markdown/text file containing the requirement input.
  --prompt <TEXT>               Inline requirement input.
  --plan-output <FILE>          Output markdown plan path. Defaults to docs/exec-plans/active/<TASK_ID>-full-auto-execution-plan.md.
  --manifest-output <FILE>      Output manifest path. Defaults to docs/exec-plans/active/<TASK_ID>-multi-agent-run.json.
  --model <MODEL>               Optional model override for auto-planner.
  --profile <PROFILE>           Optional Codex profile override for auto-planner.
  --dry-run                     Render prompt/schema/output paths without running codex exec.
  -h, --help                    Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task)
      TASK_ID="${2:-}"
      shift 2
      ;;
    --requirements-file)
      REQUIREMENTS_FILE="${2:-}"
      shift 2
      ;;
    --prompt)
      PROMPT_TEXT="${2:-}"
      shift 2
      ;;
    --plan-output)
      PLAN_OUTPUT="${2:-}"
      shift 2
      ;;
    --manifest-output)
      MANIFEST_OUTPUT="${2:-}"
      shift 2
      ;;
    --model)
      MODEL="${2:-}"
      shift 2
      ;;
    --profile)
      PROFILE="${2:-}"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "${TASK_ID}" ]]; then
  usage >&2
  exit 1
fi

if [[ -n "${REQUIREMENTS_FILE}" && -n "${PROMPT_TEXT}" ]]; then
  echo "Use either --requirements-file or --prompt, not both." >&2
  exit 1
fi

if [[ -z "${REQUIREMENTS_FILE}" && -z "${PROMPT_TEXT}" ]]; then
  echo "One of --requirements-file or --prompt is required." >&2
  exit 1
fi

python3 - "${REPO_ROOT}" "${TASK_ID}" "${REQUIREMENTS_FILE}" "${PROMPT_TEXT}" "${PLAN_OUTPUT}" "${MANIFEST_OUTPUT}" "${MODEL}" "${PROFILE}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path


REQUIRED_FORBIDDEN = {
    "tasks.md",
    "tasks-done.md",
    "INBOX.md",
    "docs/quality/validation-log.md",
}
VALID_ROLES = {"explorer", "worker", "validator"}
VALID_MCP_PROFILE_ROLES = {"explorer", "validator"}
VALID_MCP_CATEGORIES = {
    "observability_logs",
    "deployment_evidence",
    "object_storage_metadata",
    "external_requirements_tickets",
}
VALID_MCP_SOURCES = {"local-user-config", "env", "external-secret-store"}


def fail(message: str) -> None:
    raise SystemExit(message)


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


def run(command: list[str], cwd: Path, stdin_text: str | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, text=True, input=stdin_text, capture_output=True)


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        fail(f"Missing required json file: {path}")
        raise exc
    except json.JSONDecodeError as exc:
        fail(f"Invalid json in {path}: {exc}")
        raise exc


def extract_section(content: str, heading: str) -> str:
    marker = f"## {heading}\n"
    start = content.find(marker)
    if start == -1:
        return ""
    start += len(marker)
    end = content.find("\n## ", start)
    if end == -1:
        end = len(content)
    return content[start:end]


def codex_exec_mode() -> str:
    mode = os.environ.get("SQLFORGE_CODEX_EXEC_MODE", "bypass").strip()
    if mode in {"", "bypass", "full-auto", "read-only", "workspace-write", "danger-full-access"}:
        return mode or "bypass"
    fail(
        "Unsupported SQLFORGE_CODEX_EXEC_MODE value: "
        f"{mode!r}. Expected bypass, full-auto, read-only, workspace-write, or danger-full-access."
    )


def build_codex_command(
    workspace: Path,
    *,
    schema_path: Path | None = None,
    result_path: Path | None = None,
    model: str = "",
    profile: str = "",
) -> list[str]:
    command = ["codex", "exec", "-C", str(workspace)]
    mode = codex_exec_mode()
    if mode == "bypass":
        command.append("--dangerously-bypass-approvals-and-sandbox")
    elif mode == "full-auto":
        command.append("--full-auto")
    else:
        command.extend(["--sandbox", mode])
    command.extend(["--color", "never"])
    if schema_path is not None:
        command.extend(["--output-schema", str(schema_path)])
    if result_path is not None:
        command.extend(["-o", str(result_path)])
    if profile:
        command.extend(["-p", profile])
    if model:
        command.extend(["-m", model])
    return command


def normalize_pattern(raw: str) -> str:
    value = raw.strip().replace("\\", "/").lstrip("./")
    if value.endswith("/"):
        value = value[:-1]
    return value


def patterns_overlap(left: str, right: str) -> bool:
    left_norm = normalize_pattern(left)
    right_norm = normalize_pattern(right)
    if not left_norm or not right_norm:
        return False
    if left_norm == right_norm:
        return True
    left_prefix = left_norm[:-3] if left_norm.endswith("/**") else left_norm
    right_prefix = right_norm[:-3] if right_norm.endswith("/**") else right_norm
    if left_prefix == right_prefix:
        return True
    return (
        left_prefix.startswith(right_prefix + "/")
        or right_prefix.startswith(left_prefix + "/")
        or left_prefix == right_prefix
    )


def ensure_task_binding(repo_root: Path, task_id: str) -> None:
    current_task_path = repo_root / ".codex" / "state" / "current-task.json"
    current = load_json(current_task_path)
    if current.get("task_id") != task_id:
        fail(
            f"Current foreman context is bound to {current.get('task_id') or 'no task'}, "
            f"not {task_id}. Run preflight for the target task first."
        )

    tasks_text = (repo_root / "tasks.md").read_text(encoding="utf-8")
    in_progress = extract_section(tasks_text, "In Progress")
    active_ids = re.findall(r"^###\s+([A-Z0-9-]+):", in_progress, flags=re.MULTILINE)
    if task_id not in active_ids:
        fail(f"{task_id} is not active in tasks.md.")


def build_schema(path: Path) -> None:
    schema = {
        "type": "object",
        "additionalProperties": False,
        "required": ["summary", "plan_markdown", "manifest", "assumptions", "residual_risk"],
        "properties": {
            "summary": {"type": "string"},
            "plan_markdown": {"type": "string"},
            "manifest": {
                "type": "object",
                "additionalProperties": False,
                "required": ["agents"],
                "properties": {
                    "mcp_profiles": {
                        "type": "object",
                        "additionalProperties": {
                            "type": "object",
                            "additionalProperties": False,
                            "required": ["source", "allowed_roles", "allowed_categories"],
                            "properties": {
                                "source": {"type": "string"},
                                "allowed_roles": {
                                    "type": "array",
                                    "items": {"type": "string", "enum": ["explorer", "validator"]},
                                },
                                "allowed_categories": {
                                    "type": "array",
                                    "items": {
                                        "type": "string",
                                        "enum": [
                                            "observability_logs",
                                            "deployment_evidence",
                                            "object_storage_metadata",
                                            "external_requirements_tickets",
                                        ],
                                    },
                                },
                                "notes": {"type": "string"},
                            },
                        },
                    },
                    "agents": {
                        "type": "array",
                        "items": {
                            "type": "object",
                            "additionalProperties": False,
                            "required": [
                                "name",
                                "role",
                                "worktree",
                                "prompt_file",
                                "ownership",
                                "forbidden_paths",
                                "validation_scope",
                            ],
                            "properties": {
                                "name": {"type": "string"},
                                "role": {
                                    "type": "string",
                                    "enum": ["explorer", "worker", "validator"],
                                },
                                "worktree": {"type": "string"},
                                "prompt_file": {"type": "string"},
                                "mcp_profile": {"type": "string"},
                                "ownership": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                },
                                "forbidden_paths": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                },
                                "validation_scope": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                },
                            },
                        },
                    },
                },
            },
            "assumptions": {"type": "array", "items": {"type": "string"}},
            "residual_risk": {"type": "array", "items": {"type": "string"}},
        },
    }
    path.write_text(json.dumps(schema, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def normalize_manifest(repo_root: Path, task_id: str, manifest: dict) -> dict:
    normalized = dict(manifest)
    normalized["task_id"] = task_id
    normalized["mode"] = "full-auto"
    normalized["main_worktree"] = str(normalized.get("main_worktree", "."))
    normalized["run_root"] = str(normalized.get("run_root", f".codex/state/multi-agent/{task_id}"))
    normalized["requirements_artifact"] = str(
        normalized.get("requirements_artifact", f".codex/state/multi-agent/{task_id}/requirements.md")
    )
    normalized["generated_plan"] = str(
        normalized.get("generated_plan", f"docs/exec-plans/active/{task_id}-full-auto-execution-plan.md")
    )
    codex_settings = dict(normalized.get("codex", {}))
    codex_settings.setdefault("sandbox", "workspace-write")
    codex_settings.setdefault("full_auto", False)
    codex_settings.setdefault("bypass_approvals_and_sandbox", True)
    codex_settings.setdefault("color", "never")
    codex_settings.setdefault("extra_args", [])
    normalized["codex"] = codex_settings
    raw_mcp_profiles = normalized.get("mcp_profiles", {})
    if raw_mcp_profiles in ({}, None):
        normalized_mcp_profiles: dict[str, dict] = {}
    else:
        if not isinstance(raw_mcp_profiles, dict):
            fail("Auto-planner returned invalid mcp_profiles; expected an object.")
        normalized_mcp_profiles = {}
        for profile_name, payload in raw_mcp_profiles.items():
            if not isinstance(payload, dict):
                fail(f"Auto-planner returned invalid mcp_profiles.{profile_name}; expected an object.")
            source = str(payload.get("source", "")).strip()
            if source and source not in VALID_MCP_SOURCES:
                fail(f"Auto-planner returned unsupported mcp_profiles.{profile_name}.source: {source!r}")
            allowed_roles = payload.get("allowed_roles", [])
            allowed_categories = payload.get("allowed_categories", [])
            if not isinstance(allowed_roles, list) or not allowed_roles:
                fail(f"Auto-planner returned invalid mcp_profiles.{profile_name}.allowed_roles")
            if not isinstance(allowed_categories, list) or not allowed_categories:
                fail(f"Auto-planner returned invalid mcp_profiles.{profile_name}.allowed_categories")
            invalid_roles = sorted({str(item) for item in allowed_roles} - VALID_MCP_PROFILE_ROLES)
            if invalid_roles:
                fail(
                    f"Auto-planner returned unsupported mcp role(s) for {profile_name}: "
                    + ", ".join(invalid_roles)
                )
            invalid_categories = sorted({str(item) for item in allowed_categories} - VALID_MCP_CATEGORIES)
            if invalid_categories:
                fail(
                    f"Auto-planner returned unsupported mcp category id(s) for {profile_name}: "
                    + ", ".join(invalid_categories)
                )
            normalized_mcp_profiles[str(profile_name)] = {
                "source": source or "local-user-config",
                "allowed_roles": [str(item) for item in allowed_roles],
                "allowed_categories": [str(item) for item in allowed_categories],
                "notes": str(payload.get("notes", "")).strip(),
            }
    normalized["mcp_profiles"] = normalized_mcp_profiles

    agents = normalized.get("agents")
    if not isinstance(agents, list) or not agents:
        fail("Auto-planner returned a manifest without agents.")

    seen_names: set[str] = set()
    normalized_agents = []
    for agent in agents:
        current = dict(agent)
        name = str(current.get("name", "")).strip()
        role = str(current.get("role", "")).strip()
        if not name:
            fail(f"Auto-planner returned an agent without name: {agent}")
        if name in seen_names:
            fail(f"Auto-planner returned duplicate agent name: {name}")
        seen_names.add(name)
        if role not in VALID_ROLES:
            fail(f"Auto-planner returned unsupported role for {name}: {role!r}")
        prompt_file = str(current.get("prompt_file", "")).strip()
        if not prompt_file:
            fail(f"Auto-planner returned empty prompt_file for {name}")
        resolved_prompt = resolve_repo_relative(repo_root, prompt_file)
        if not resolved_prompt.exists():
            fail(f"Prompt template does not exist for {name}: {prompt_file}")
        ownership = current.get("ownership", [])
        forbidden_paths = list(current.get("forbidden_paths", []))
        validation_scope = current.get("validation_scope", [])
        if not isinstance(ownership, list) or not isinstance(forbidden_paths, list) or not isinstance(validation_scope, list):
            fail(f"Auto-planner returned invalid list fields for {name}")
        mcp_profile = str(current.get("mcp_profile", "")).strip()
        if role == "worker":
            for item in sorted(REQUIRED_FORBIDDEN):
                if item not in forbidden_paths:
                    forbidden_paths.append(item)
            if mcp_profile:
                fail(f"Auto-planner assigned mcp_profile to worker {name}, which is forbidden.")
        elif mcp_profile:
            if mcp_profile not in normalized_mcp_profiles:
                fail(f"Auto-planner referenced undefined mcp_profile for {name}: {mcp_profile}")
            allowed_roles = normalized_mcp_profiles[mcp_profile]["allowed_roles"]
            if role not in allowed_roles:
                fail(f"Auto-planner assigned mcp_profile {mcp_profile} to unsupported role {role} for {name}")
        worktree = str(current.get("worktree", "")).strip()
        if not worktree:
            if role == "worker":
                worktree = f"../sqlforge-{task_id.lower()}-{name}"
            else:
                worktree = "."

        normalized_agents.append(
            {
                "name": name,
                "role": role,
                "worktree": worktree,
                "prompt_file": prompt_file,
                **({"mcp_profile": mcp_profile} if mcp_profile else {}),
                "ownership": ownership,
                "forbidden_paths": forbidden_paths,
                "validation_scope": validation_scope,
                "notes": str(current.get("notes", "")).strip(),
                **({"model": current["model"]} if "model" in current else {}),
                **({"profile": current["profile"]} if "profile" in current else {}),
                **({"extra_args": current["extra_args"]} if "extra_args" in current else {}),
            }
        )

    workers = [agent for agent in normalized_agents if agent["role"] == "worker"]
    for index, left in enumerate(workers):
        for right in workers[index + 1 :]:
            for left_pattern in left["ownership"]:
                for right_pattern in right["ownership"]:
                    if patterns_overlap(str(left_pattern), str(right_pattern)):
                        fail(
                            f"Auto-planner returned overlapping worker ownership between "
                            f"{left['name']} and {right['name']}"
                        )

    normalized["agents"] = normalized_agents
    return normalized


repo_root = Path(sys.argv[1]).resolve()
task_id = sys.argv[2]
requirements_file = sys.argv[3]
prompt_text = sys.argv[4]
plan_output_raw = sys.argv[5]
manifest_output_raw = sys.argv[6]
model = sys.argv[7]
profile = sys.argv[8]
dry_run = sys.argv[9].lower() == "true"

ensure_task_binding(repo_root, task_id)

if requirements_file:
    requirements_path = resolve_repo_relative(repo_root, requirements_file)
    if not requirements_path.exists():
        fail(f"Requirements file does not exist: {requirements_path}")
    requirements_text = requirements_path.read_text(encoding="utf-8").strip()
else:
    requirements_path = None
    requirements_text = prompt_text.strip()

if not requirements_text:
    fail("Requirement input is empty.")

run_root = repo_root / ".codex" / "state" / "multi-agent" / task_id
autoplan_root = run_root / "autoplan"
prompts_dir = run_root / "prompts"
logs_dir = run_root / "logs"
for directory in [run_root, autoplan_root, prompts_dir, logs_dir]:
    directory.mkdir(parents=True, exist_ok=True)

requirements_artifact = run_root / "requirements.md"
requirements_artifact.write_text(requirements_text + "\n", encoding="utf-8")

plan_output = resolve_repo_relative(
    repo_root,
    plan_output_raw or f"docs/exec-plans/active/{task_id}-full-auto-execution-plan.md",
)
manifest_output = resolve_repo_relative(
    repo_root,
    manifest_output_raw or f"docs/exec-plans/active/{task_id}-multi-agent-run.json",
)

available_templates = sorted(
    path.relative_to(repo_root).as_posix()
    for path in (repo_root / "docs" / "agent-prompts").glob("*.md")
    if path.is_file() and path.name not in {"auto-planner.md", "auto-foreman.md", "main-foreman.md"}
)
prompt_template = (repo_root / "docs" / "agent-prompts" / "auto-planner.md").read_text(encoding="utf-8").rstrip()
rendered_prompt = (
    prompt_template
    + "\n\n## Runtime Assignment\n\n"
    + f"- Task ID: `{task_id}`\n"
    + f"- Requirements artifact: `{requirements_artifact.relative_to(repo_root).as_posix()}`\n"
    + f"- Output plan path: `{plan_output.relative_to(repo_root).as_posix()}`\n"
    + f"- Output manifest path: `{manifest_output.relative_to(repo_root).as_posix()}`\n"
    + "- Available prompt templates:\n"
    + "".join(f"  - `{item}`\n" for item in available_templates)
    + "- Manifest hard requirements:\n"
    + "  - every agent `role` must be exactly one of `explorer`, `worker`, `validator`\n"
    + "  - `task_id` must match the provided task id\n"
    + "  - `mode` must be `full-auto`\n"
    + "  - `main_worktree` must point at the primary repository worktree\n"
    + "  - only `explorer` / `validator` may declare `mcp_profile`; `worker` must not\n"
    + "  - if manifest uses MCP, return top-level `mcp_profiles` registry with symbolic profile names only and no live servers/secrets\n"
    + "  - worker forbidden paths must include `tasks.md`, `tasks-done.md`, `INBOX.md`, `docs/quality/validation-log.md`\n"
    + "  - worker ownership must not overlap\n"
    + "  - use isolated worktrees for workers\n"
    + "  - you may return only the agent array inside `manifest`; the orchestrator will inject stable top-level fields\n"
    + "- Return JSON only and do not wrap it in markdown fences.\n"
    + "\n## Requirement Input\n\n"
    + requirements_text
    + "\n"
)

rendered_prompt_path = prompts_dir / "auto-planner.md"
rendered_prompt_path.write_text(rendered_prompt, encoding="utf-8")

schema_path = autoplan_root / "auto-planner-schema.json"
result_path = autoplan_root / "auto-planner-result.json"
console_log_path = logs_dir / "auto-planner.log"
build_schema(schema_path)

command = build_codex_command(
    repo_root,
    schema_path=schema_path,
    result_path=result_path,
    model=model,
    profile=profile,
)

metadata = {
    "task_id": task_id,
    "requirements_artifact": str(requirements_artifact),
    "plan_output": str(plan_output),
    "manifest_output": str(manifest_output),
    "schema_path": str(schema_path),
    "result_path": str(result_path),
    "prompt_path": str(rendered_prompt_path),
    "command": command,
    "dry_run": dry_run,
}
(autoplan_root / "autoplan-metadata.json").write_text(
    json.dumps(metadata, ensure_ascii=True, indent=2) + "\n",
    encoding="utf-8",
)

if dry_run:
    print(f"[dry-run] auto-planner prompt: {rendered_prompt_path}")
    print(f"[dry-run] plan output: {plan_output}")
    print(f"[dry-run] manifest output: {manifest_output}")
    print("[dry-run] command: " + " ".join(command) + f" < {rendered_prompt_path}")
    raise SystemExit(0)

result = run(command, cwd=repo_root, stdin_text=rendered_prompt)
console_log_path.write_text((result.stdout or "") + (result.stderr or ""), encoding="utf-8")
if result.returncode != 0:
    fail(f"auto-planner failed; see {console_log_path}")

payload = load_json(result_path)
if not isinstance(payload.get("plan_markdown"), str):
    fail("auto-planner result is missing string field: plan_markdown")
manifest = payload.get("manifest")
if not isinstance(manifest, dict):
    fail("auto-planner result is missing object field: manifest")

normalized_manifest = normalize_manifest(repo_root, task_id, manifest)
plan_output.parent.mkdir(parents=True, exist_ok=True)
manifest_output.parent.mkdir(parents=True, exist_ok=True)
plan_output.write_text(payload["plan_markdown"].rstrip() + "\n", encoding="utf-8")
manifest_output.write_text(json.dumps(normalized_manifest, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")

summary = {
    "task_id": task_id,
    "plan_output": str(plan_output),
    "manifest_output": str(manifest_output),
    "assumptions": payload.get("assumptions", []),
    "residual_risk": payload.get("residual_risk", []),
}
(autoplan_root / "autoplan-summary.json").write_text(
    json.dumps(summary, ensure_ascii=True, indent=2) + "\n",
    encoding="utf-8",
)

print(f"auto-planner completed for {task_id}")
print(f"requirements_artifact: {requirements_artifact}")
print(f"plan_output: {plan_output}")
print(f"manifest_output: {manifest_output}")
PY
