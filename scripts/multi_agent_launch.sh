#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

MANIFEST_PATH=""
ONLY_AGENT=""
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/multi_agent_launch.sh --manifest <FILE> [options]

Options:
  --manifest <FILE>      Active manifest json path.
  --only <AGENT_NAME>    Launch only one named agent from the manifest.
  --dry-run              Print the rendered launch plan without starting codex exec.
  -h, --help             Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --manifest)
      MANIFEST_PATH="${2:-}"
      shift 2
      ;;
    --only)
      ONLY_AGENT="${2:-}"
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

if [[ -z "${MANIFEST_PATH}" ]]; then
  usage >&2
  exit 1
fi

python3 - "${REPO_ROOT}" "${MANIFEST_PATH}" "${ONLY_AGENT}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import json
import shlex
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(message)


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        fail(f"Manifest does not exist: {path}")
        raise exc
    except json.JSONDecodeError as exc:
        fail(f"Invalid json in {path}: {exc}")
        raise exc


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


def now_iso() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def quoted_command(parts: list[str], prompt_path: Path) -> str:
    rendered = " ".join(shlex.quote(part) for part in parts)
    return f"{rendered} < {shlex.quote(str(prompt_path))}"


repo_root = Path(sys.argv[1]).resolve()
manifest_path = resolve_repo_relative(repo_root, sys.argv[2])
only_agent = sys.argv[3]
dry_run = sys.argv[4].lower() == "true"

manifest = load_json(manifest_path)
task_id = manifest.get("task_id")
if not task_id:
    fail("Manifest is missing task_id.")
agents = manifest.get("agents")
if not isinstance(agents, list) or not agents:
    fail("Manifest agents must be a non-empty array.")
mcp_profiles = manifest.get("mcp_profiles", {})
if mcp_profiles is None:
    mcp_profiles = {}
if not isinstance(mcp_profiles, dict):
    fail("Manifest mcp_profiles must be an object when provided.")

selected_agents = []
for agent in agents:
    if only_agent and agent.get("name") != only_agent:
        continue
    selected_agents.append(agent)

if only_agent and not selected_agents:
    fail(f"Agent {only_agent} was not found in {manifest_path}")

run_root = resolve_repo_relative(repo_root, str(manifest.get("run_root", f".codex/state/multi-agent/{task_id}")))
prompts_dir = run_root / "prompts"
logs_dir = run_root / "logs"
messages_dir = run_root / "messages"
commands_dir = run_root / "commands"
meta_dir = run_root / "meta"
for directory in [run_root, prompts_dir, logs_dir, messages_dir, commands_dir, meta_dir]:
    directory.mkdir(parents=True, exist_ok=True)

codex_settings = manifest.get("codex", {})
default_sandbox = str(codex_settings.get("sandbox", "workspace-write"))
default_color = str(codex_settings.get("color", "never"))
default_full_auto = bool(codex_settings.get("full_auto", False))
default_bypass = bool(codex_settings.get("bypass_approvals_and_sandbox", True))
default_extra_args = list(codex_settings.get("extra_args", []))

index_payload = {
    "task_id": task_id,
    "manifest": str(manifest_path),
    "run_root": str(run_root),
    "generated_at": now_iso(),
    "dry_run": dry_run,
    "agents": [],
}

for agent in selected_agents:
    name = str(agent["name"])
    role = str(agent["role"])
    worktree = resolve_repo_relative(repo_root, str(agent["worktree"]))
    prompt_file = resolve_repo_relative(repo_root, str(agent["prompt_file"]))
    agent_mcp_profile = str(agent.get("mcp_profile", "")).strip()
    mcp_profile_meta = mcp_profiles.get(agent_mcp_profile, {}) if agent_mcp_profile else {}
    if not prompt_file.exists():
        fail(f"Prompt template does not exist for {name}: {prompt_file}")
    if not dry_run and not worktree.exists():
        fail(f"Worktree does not exist for {name}: {worktree}")

    rendered_prompt = (
        prompt_file.read_text(encoding="utf-8").rstrip()
        + "\n\n## Runtime Assignment\n\n"
        + f"- Task ID: `{task_id}`\n"
        + f"- Agent name: `{name}`\n"
        + f"- Role: `{role}`\n"
        + f"- Manifest path: `{manifest_path}`\n"
        + f"- Worktree: `{worktree}`\n"
        + f"- Ownership: {json.dumps(agent.get('ownership', []), ensure_ascii=True)}\n"
        + f"- Forbidden Paths: {json.dumps(agent.get('forbidden_paths', []), ensure_ascii=True)}\n"
        + f"- Validation Scope: {json.dumps(agent.get('validation_scope', []), ensure_ascii=True)}\n"
        + f"- MCP Profile: {json.dumps(agent_mcp_profile or 'none', ensure_ascii=True)}\n"
        + f"- MCP Profile Source: {json.dumps(mcp_profile_meta.get('source', 'none'), ensure_ascii=True)}\n"
        + f"- MCP Allowed Categories: {json.dumps(mcp_profile_meta.get('allowed_categories', []), ensure_ascii=True)}\n"
        + "- MCP Boundary: read-only external evidence only; no remote mutation and no repo write-back.\n"
        + f"- Notes: {json.dumps(agent.get('notes', ''), ensure_ascii=True)}\n"
        + "- Required final format:\n"
        + "  - Changed files\n"
        + "  - Implementation summary\n"
        + "  - Validation performed\n"
        + "  - Residual risk\n"
    )
    prompt_path = prompts_dir / f"{name}.md"
    prompt_path.write_text(rendered_prompt + "\n", encoding="utf-8")

    last_message_path = messages_dir / f"{name}.md"
    console_log_path = logs_dir / f"{name}.log"
    launch_script_path = commands_dir / f"{name}.sh"
    meta_path = meta_dir / f"{name}.json"

    command = ["codex", "exec", "-C", str(worktree), "--json", "--color", default_color, "-o", str(last_message_path)]
    if default_bypass:
        command.append("--dangerously-bypass-approvals-and-sandbox")
    elif default_full_auto:
        command.append("--full-auto")
    else:
        command.extend(["--sandbox", default_sandbox])
    command.extend(default_extra_args)
    effective_profile = str(agent.get("profile", "")).strip()
    if effective_profile:
        command.extend(["-p", effective_profile])
    elif agent_mcp_profile:
        command.extend(["-p", agent_mcp_profile])
    if agent.get("model"):
        command.extend(["-m", str(agent["model"])])
    if agent.get("extra_args"):
        command.extend(list(agent["extra_args"]))

    launch_script_path.write_text(
        "#!/usr/bin/env bash\nset -euo pipefail\n"
        + quoted_command(command, prompt_path)
        + "\n",
        encoding="utf-8",
    )
    launch_script_path.chmod(0o755)

    metadata = {
        "name": name,
        "role": role,
        "worktree": str(worktree),
        "prompt_path": str(prompt_path),
        "last_message_path": str(last_message_path),
        "console_log_path": str(console_log_path),
        "launch_script_path": str(launch_script_path),
        "command": command,
        "mcp_profile": agent_mcp_profile,
        "mcp_allowed_categories": list(mcp_profile_meta.get("allowed_categories", [])),
        "mcp_source": str(mcp_profile_meta.get("source", "")),
        "effective_codex_profile": effective_profile or agent_mcp_profile,
        "launched_at": now_iso(),
        "dry_run": dry_run,
    }

    if dry_run:
        print(f"[dry-run] {name}: {quoted_command(command, prompt_path)}")
    else:
        with console_log_path.open("wb") as handle:
            process = subprocess.Popen(
                ["bash", str(launch_script_path)],
                cwd=repo_root,
                stdout=handle,
                stderr=subprocess.STDOUT,
                start_new_session=True,
            )
        metadata["pid"] = process.pid
        print(f"launched {name} pid={process.pid} worktree={worktree}")

    meta_path.write_text(json.dumps(metadata, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")
    index_payload["agents"].append(metadata)

(run_root / "launch-index.json").write_text(json.dumps(index_payload, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")
print(f"launch {'dry-run ' if dry_run else ''}completed for {task_id}")
print(f"run_root: {run_root}")
PY
