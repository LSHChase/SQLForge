#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

MANIFEST_PATH=""

usage() {
  cat <<'EOF'
Usage: ./scripts/multi_agent_collect.sh --manifest <FILE>

Options:
  --manifest <FILE>      Active manifest json path.
  -h, --help             Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --manifest)
      MANIFEST_PATH="${2:-}"
      shift 2
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

python3 - "${REPO_ROOT}" "${MANIFEST_PATH}" <<'PY'
from __future__ import annotations

import fnmatch
import json
import os
import subprocess
import sys
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(message)


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        fail(f"Missing required file: {path}")
        raise exc
    except json.JSONDecodeError as exc:
        fail(f"Invalid json in {path}: {exc}")
        raise exc


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


def git_changed_files(worktree: Path) -> list[str]:
    changed: set[str] = set()
    commands = [
        ["git", "-C", str(worktree), "diff", "--name-only", "--relative"],
        ["git", "-C", str(worktree), "diff", "--cached", "--name-only", "--relative"],
        ["git", "-C", str(worktree), "ls-files", "--others", "--exclude-standard"],
    ]
    for command in commands:
        result = subprocess.run(command, cwd=worktree, text=True, capture_output=True)
        if result.returncode != 0:
            fail(result.stderr.strip() or result.stdout.strip() or f"Failed to inspect worktree {worktree}")
        for line in result.stdout.splitlines():
            value = line.strip().replace("\\", "/")
            if value and not value.startswith(".codex/state/"):
                changed.add(value)
    return sorted(changed)


def path_matches(path: str, pattern: str) -> bool:
    normalized_path = path.replace("\\", "/").lstrip("./")
    normalized_pattern = pattern.replace("\\", "/").lstrip("./")
    if fnmatch.fnmatch(normalized_path, normalized_pattern):
        return True
    if normalized_pattern.endswith("/**"):
        prefix = normalized_pattern[:-3].rstrip("/")
        return normalized_path == prefix or normalized_path.startswith(prefix + "/")
    if not any(token in normalized_pattern for token in ["*", "?", "["]):
        return normalized_path == normalized_pattern or normalized_path.startswith(normalized_pattern.rstrip("/") + "/")
    return False


def running(pid: int | None) -> bool:
    if pid is None:
        return False
    try:
        os.kill(pid, 0)
    except OSError:
        return False
    return True


repo_root = Path(sys.argv[1]).resolve()
manifest_path = resolve_repo_relative(repo_root, sys.argv[2])
manifest = load_json(manifest_path)
task_id = manifest.get("task_id")
if not task_id:
    fail("Manifest is missing task_id.")

run_root = resolve_repo_relative(repo_root, str(manifest.get("run_root", f".codex/state/multi-agent/{task_id}")))
meta_dir = run_root / "meta"
messages_dir = run_root / "messages"
summary_path = run_root / "collect-summary.md"
json_summary_path = run_root / "collect-summary.json"
run_root.mkdir(parents=True, exist_ok=True)

agent_summaries = []
accepted = []
rejected = []
file_to_agents: dict[str, list[str]] = {}

for agent in manifest.get("agents", []):
    name = str(agent["name"])
    role = str(agent["role"])
    worktree = resolve_repo_relative(repo_root, str(agent["worktree"]))
    meta_path = meta_dir / f"{name}.json"
    meta = load_json(meta_path) if meta_path.exists() else {}
    changed_files = git_changed_files(worktree)
    forbidden_hits = [
        path
        for path in changed_files
        if any(path_matches(path, pattern) for pattern in agent.get("forbidden_paths", []))
    ]
    if role == "worker":
        out_of_scope = [
            path
            for path in changed_files
            if not any(path_matches(path, pattern) for pattern in agent.get("ownership", []))
        ]
    else:
        out_of_scope = changed_files if changed_files else []

    status = "acceptable"
    if role in {"explorer", "validator"} and changed_files:
        status = "rejected"
    if forbidden_hits or out_of_scope:
        status = "rejected"
    if not changed_files:
        status = "no_patch" if role == "worker" else "read_only"

    pid = meta.get("pid") if isinstance(meta.get("pid"), int) else None
    runtime_state = "running" if running(pid) else "stopped"
    last_message_path = Path(meta.get("last_message_path", messages_dir / f"{name}.md"))
    last_message = last_message_path.read_text(encoding="utf-8").strip() if last_message_path.exists() else ""

    summary = {
        "name": name,
        "role": role,
        "worktree": str(worktree),
        "status": status,
        "runtime_state": runtime_state,
        "changed_files": changed_files,
        "forbidden_hits": forbidden_hits,
        "out_of_scope": out_of_scope,
        "last_message_path": str(last_message_path),
        "last_message_excerpt": last_message[:400],
    }
    agent_summaries.append(summary)

    if status == "acceptable":
        accepted.append({"agent": name, "changed_files": changed_files})
    elif status == "rejected":
        rejected.append(
            {
                "agent": name,
                "changed_files": changed_files,
                "forbidden_hits": forbidden_hits,
                "out_of_scope": out_of_scope,
            }
        )

    for changed in changed_files:
        file_to_agents.setdefault(changed, []).append(name)

conflicts = [
    {"file": path, "agents": names}
    for path, names in sorted(file_to_agents.items())
    if len(names) > 1
]

next_steps = []
if conflicts:
    next_steps.append("Resolve file-level conflicts before fan-in.")
if rejected:
    next_steps.append("Reject or manually repair out-of-scope / forbidden-path patches.")
if any(item["runtime_state"] == "running" for item in agent_summaries):
    next_steps.append("Wait for running agents or collect them again later.")
if not next_steps:
    next_steps.append("Review acceptable patches and continue with Main Foreman fan-in.")

markdown_lines = [
    f"# Multi-Agent Collect Summary: {task_id}",
    "",
    f"- Manifest: `{manifest_path}`",
    f"- Run root: `{run_root}`",
    "",
    "## Agent Status",
    "",
]
for item in agent_summaries:
    markdown_lines.append(f"### {item['name']} ({item['role']})")
    markdown_lines.append("")
    markdown_lines.append(f"- Collect status: `{item['status']}`")
    markdown_lines.append(f"- Runtime state: `{item['runtime_state']}`")
    markdown_lines.append(f"- Worktree: `{item['worktree']}`")
    markdown_lines.append(f"- Changed files: {json.dumps(item['changed_files'], ensure_ascii=True)}")
    markdown_lines.append(f"- Forbidden hits: {json.dumps(item['forbidden_hits'], ensure_ascii=True)}")
    markdown_lines.append(f"- Out of scope: {json.dumps(item['out_of_scope'], ensure_ascii=True)}")
    markdown_lines.append(f"- Last message path: `{item['last_message_path']}`")
    markdown_lines.append("")

markdown_lines.extend(["## Acceptable Patches", ""])
if accepted:
    for item in accepted:
        markdown_lines.append(f"- {item['agent']}: {json.dumps(item['changed_files'], ensure_ascii=True)}")
else:
    markdown_lines.append("- None")

markdown_lines.extend(["", "## Rejected Patches", ""])
if rejected:
    for item in rejected:
        markdown_lines.append(
            f"- {item['agent']}: changed={json.dumps(item['changed_files'], ensure_ascii=True)} "
            f"forbidden={json.dumps(item['forbidden_hits'], ensure_ascii=True)} "
            f"out_of_scope={json.dumps(item['out_of_scope'], ensure_ascii=True)}"
        )
else:
    markdown_lines.append("- None")

markdown_lines.extend(["", "## Conflicts", ""])
if conflicts:
    for item in conflicts:
        markdown_lines.append(f"- {item['file']}: {', '.join(item['agents'])}")
else:
    markdown_lines.append("- None")

markdown_lines.extend(["", "## Suggested Next Steps", ""])
for item in next_steps:
    markdown_lines.append(f"- {item}")

summary_path.write_text("\n".join(markdown_lines) + "\n", encoding="utf-8")
json_summary_path.write_text(
    json.dumps(
        {
            "task_id": task_id,
            "manifest": str(manifest_path),
            "run_root": str(run_root),
            "agents": agent_summaries,
            "acceptable_patches": accepted,
            "rejected_patches": rejected,
            "conflicts": conflicts,
            "next_steps": next_steps,
        },
        ensure_ascii=True,
        indent=2,
    )
    + "\n",
    encoding="utf-8",
)

print(f"collect completed for {task_id}")
print(f"summary: {summary_path}")
print(f"json: {json_summary_path}")
PY
