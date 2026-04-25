#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

TASK_ID=""
MANIFEST_PATH=""
BOOTSTRAP_IF_MISSING=false
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest <FILE> [options]

Options:
  --task <TASK_ID>           Bound task id. Must match current foreman preflight context.
  --manifest <FILE>          Active manifest json path.
  --bootstrap-if-missing     Create the manifest from the template if it does not exist yet.
  --dry-run                  Validate static shape and print the planned worktree actions only.
  -h, --help                 Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task)
      TASK_ID="${2:-}"
      shift 2
      ;;
    --manifest)
      MANIFEST_PATH="${2:-}"
      shift 2
      ;;
    --bootstrap-if-missing)
      BOOTSTRAP_IF_MISSING=true
      shift
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

if [[ -z "${TASK_ID}" || -z "${MANIFEST_PATH}" ]]; then
  usage >&2
  exit 1
fi

python3 - "${REPO_ROOT}" "${TASK_ID}" "${MANIFEST_PATH}" "${BOOTSTRAP_IF_MISSING}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable


REQUIRED_TOP_LEVEL = ["task_id", "mode", "main_worktree", "agents"]
REQUIRED_AGENT_FIELDS = [
    "name",
    "role",
    "worktree",
    "prompt_file",
    "ownership",
    "forbidden_paths",
    "validation_scope",
]
REQUIRED_FORBIDDEN = {
    "tasks.md",
    "tasks-done.md",
    "INBOX.md",
    "docs/quality/validation-log.md",
}
VALID_ROLES = {"explorer", "worker", "validator"}
VALID_MODES = {"semi-auto", "full-auto"}


def fail(message: str) -> None:
    raise SystemExit(message)


def run(command: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, text=True, capture_output=True)


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


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


def ensure_manifest_exists(repo_root: Path, manifest_path: Path, task_id: str, bootstrap: bool) -> None:
    if manifest_path.exists():
        return
    if not bootstrap:
        fail(f"Manifest does not exist: {manifest_path}")
    template_path = repo_root / "docs" / "exec-plans" / "templates" / "multi-agent-run.template.json"
    if not template_path.exists():
        fail(f"Template is missing: {template_path}")
    manifest_path.parent.mkdir(parents=True, exist_ok=True)
    manifest_path.write_text(template_path.read_text(encoding="utf-8").replace("TASK-ID", task_id), encoding="utf-8")


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
    others = [candidate for candidate in active_ids if candidate != task_id]
    if others:
        fail(
            "multi-agent prepare refuses to start while another in-progress task exists: "
            + ", ".join(sorted(others))
        )


def validate_manifest_shape(manifest: dict, task_id: str) -> list[dict]:
    for field in REQUIRED_TOP_LEVEL:
        if field not in manifest:
            fail(f"Manifest is missing top-level field: {field}")
    if manifest["task_id"] != task_id:
        fail(f"Manifest task_id {manifest['task_id']} does not match --task {task_id}")
    if manifest["mode"] not in VALID_MODES:
        fail(
            "Manifest mode must be one of "
            + ", ".join(sorted(repr(item) for item in VALID_MODES))
            + f", got {manifest['mode']!r}"
        )
    agents = manifest["agents"]
    if not isinstance(agents, list) or not agents:
        fail("Manifest agents must be a non-empty array.")
    seen_names: set[str] = set()
    for agent in agents:
        for field in REQUIRED_AGENT_FIELDS:
            if field not in agent:
                fail(f"Agent is missing required field {field}: {agent}")
        name = str(agent["name"])
        if name in seen_names:
            fail(f"Duplicate agent name: {name}")
        seen_names.add(name)
        if agent["role"] not in VALID_ROLES:
            fail(f"Unsupported agent role for {name}: {agent['role']}")
        if not isinstance(agent["ownership"], list):
            fail(f"Agent ownership must be an array for {name}")
        if not isinstance(agent["forbidden_paths"], list):
            fail(f"Agent forbidden_paths must be an array for {name}")
        if not isinstance(agent["validation_scope"], list):
            fail(f"Agent validation_scope must be an array for {name}")
        if agent["role"] == "worker":
            forbidden = {normalize_pattern(item) for item in agent["forbidden_paths"]}
            missing = sorted(REQUIRED_FORBIDDEN - forbidden)
            if missing:
                fail(f"Worker {name} is missing required forbidden paths: {', '.join(missing)}")
    return agents


def validate_ownership_conflicts(agents: list[dict]) -> None:
    workers = [agent for agent in agents if agent["role"] == "worker"]
    for index, left in enumerate(workers):
        for right in workers[index + 1 :]:
            for left_pattern in left["ownership"]:
                for right_pattern in right["ownership"]:
                    if patterns_overlap(left_pattern, right_pattern):
                        fail(
                            f"Ownership overlap detected between {left['name']} ({left_pattern}) "
                            f"and {right['name']} ({right_pattern})"
                        )


def git_top_level(path: Path) -> str:
    result = run(["git", "-C", str(path), "rev-parse", "--show-toplevel"], cwd=path)
    if result.returncode != 0:
        fail(f"{path} is not a git worktree: {result.stderr.strip() or result.stdout.strip()}")
    return result.stdout.strip()


def worktree_dirty(path: Path) -> bool:
    result = run(["git", "-C", str(path), "status", "--porcelain"], cwd=path)
    if result.returncode != 0:
        fail(result.stderr.strip() or result.stdout.strip() or f"Failed to inspect worktree {path}")
    return bool(result.stdout.strip())


def ensure_worktree(repo_root: Path, repo_top: str, path: Path, dry_run: bool) -> str:
    if path.exists():
        if git_top_level(path) != repo_top:
            fail(f"Worktree {path} does not belong to the current repository.")
        if not dry_run and worktree_dirty(path):
            fail(f"Worktree is dirty and cannot be used for multi-agent launch: {path}")
        return "reuse"
    if dry_run:
        return "would-create"
    path.parent.mkdir(parents=True, exist_ok=True)
    result = run(["git", "worktree", "add", "--detach", str(path), "HEAD"], cwd=repo_root)
    if result.returncode != 0:
        fail(result.stderr.strip() or result.stdout.strip() or f"Failed to create worktree {path}")
    if git_top_level(path) != repo_top:
        fail(f"Created worktree {path} is not bound to the current repository.")
    return "created"


def write_summary(run_root: Path, payload: dict) -> None:
    run_root.mkdir(parents=True, exist_ok=True)
    target = run_root / "prepare-summary.json"
    target.write_text(json.dumps(payload, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


repo_root = Path(sys.argv[1]).resolve()
task_id = sys.argv[2]
manifest_path = resolve_repo_relative(repo_root, sys.argv[3])
bootstrap_if_missing = sys.argv[4].lower() == "true"
dry_run = sys.argv[5].lower() == "true"

ensure_manifest_exists(repo_root, manifest_path, task_id, bootstrap_if_missing)
ensure_task_binding(repo_root, task_id)

manifest = load_json(manifest_path)
agents = validate_manifest_shape(manifest, task_id)
validate_ownership_conflicts(agents)

main_worktree = resolve_repo_relative(repo_root, str(manifest["main_worktree"]))
if not main_worktree.exists():
    fail(f"Main worktree does not exist: {main_worktree}")
repo_top = git_top_level(main_worktree)
if not dry_run and worktree_dirty(main_worktree):
    fail(f"Main worktree is dirty and cannot start a multi-agent round: {main_worktree}")

run_root = resolve_repo_relative(repo_root, str(manifest.get("run_root", f".codex/state/multi-agent/{task_id}")))
agent_actions = []
for agent in agents:
    worktree_path = resolve_repo_relative(repo_root, str(agent["worktree"]))
    if agent["role"] == "worker" and worktree_path == main_worktree:
        fail(f"Worker {agent['name']} cannot reuse the main worktree {main_worktree}")
    action = ensure_worktree(repo_root, repo_top, worktree_path, dry_run)
    agent_actions.append(
        {
            "name": agent["name"],
            "role": agent["role"],
            "worktree": str(worktree_path),
            "action": action,
        }
    )

summary = {
    "task_id": task_id,
    "manifest": str(manifest_path),
    "dry_run": dry_run,
    "main_worktree": str(main_worktree),
    "run_root": str(run_root),
    "agents": agent_actions,
}
write_summary(run_root, summary)

print(f"multi-agent prepare {'dry-run ' if dry_run else ''}passed for {task_id}")
print(f"manifest: {manifest_path}")
print(f"main_worktree: {main_worktree}")
for agent in agent_actions:
    print(f"- {agent['name']} [{agent['role']}] -> {agent['worktree']} ({agent['action']})")
PY
