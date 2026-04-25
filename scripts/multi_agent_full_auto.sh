#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

TASK_ID=""
REQUIREMENTS_FILE=""
PROMPT_TEXT=""
PLANNER_MODEL=""
PLANNER_PROFILE=""
FOREMAN_MODEL=""
FOREMAN_PROFILE=""
STOP_AFTER="foreman"
WAIT_TIMEOUT=1800
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/multi_agent_full_auto.sh --task <TASK_ID> (--requirements-file <FILE> | --prompt <TEXT>) [options]

Options:
  --task <TASK_ID>              Target task id.
  --requirements-file <FILE>    Markdown/text file containing the requirement input.
  --prompt <TEXT>               Inline requirement input.
  --planner-model <MODEL>       Optional model override for auto-planner.
  --planner-profile <PROFILE>   Optional profile override for auto-planner.
  --foreman-model <MODEL>       Optional model override for autonomous Main Foreman.
  --foreman-profile <PROFILE>   Optional profile override for autonomous Main Foreman.
  --stop-after <PHASE>          One of: autoplan, collect, foreman. Default: foreman.
  --wait-timeout <SECONDS>      Max seconds to wait for launched agents before collect. Default: 1800.
  --dry-run                     Print the planned orchestration without running it.
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
    --planner-model)
      PLANNER_MODEL="${2:-}"
      shift 2
      ;;
    --planner-profile)
      PLANNER_PROFILE="${2:-}"
      shift 2
      ;;
    --foreman-model)
      FOREMAN_MODEL="${2:-}"
      shift 2
      ;;
    --foreman-profile)
      FOREMAN_PROFILE="${2:-}"
      shift 2
      ;;
    --stop-after)
      STOP_AFTER="${2:-}"
      shift 2
      ;;
    --wait-timeout)
      WAIT_TIMEOUT="${2:-}"
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

case "${STOP_AFTER}" in
  autoplan|collect|foreman)
    ;;
  *)
    echo "Unsupported --stop-after value: ${STOP_AFTER}" >&2
    exit 1
    ;;
esac

PLAN_OUTPUT="docs/exec-plans/active/${TASK_ID}-full-auto-execution-plan.md"
MANIFEST_OUTPUT="docs/exec-plans/active/${TASK_ID}-multi-agent-run.json"

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "[dry-run] python3 scripts/foreman.py preflight --task ${TASK_ID} --task-class standard --prompt \"Full-auto multi-agent orchestration for ${TASK_ID}.\""
  echo "[dry-run] instantiate ${TASK_ID} if it is not already active"
  echo "[dry-run] bash scripts/multi_agent_autoplan.sh --task ${TASK_ID} --requirements-file|--prompt ..."
  echo "[dry-run] bash scripts/multi_agent_prepare.sh --task ${TASK_ID} --manifest ${MANIFEST_OUTPUT}"
  echo "[dry-run] bash scripts/multi_agent_launch.sh --manifest ${MANIFEST_OUTPUT}"
  echo "[dry-run] wait for launched agents to stop"
  echo "[dry-run] bash scripts/multi_agent_collect.sh --manifest ${MANIFEST_OUTPUT}"
  if [[ "${STOP_AFTER}" == "foreman" ]]; then
    echo "[dry-run] launch autonomous Main Foreman with ${PLAN_OUTPUT} and collect summary"
  fi
  exit 0
fi

if [[ -n "${REQUIREMENTS_FILE}" ]]; then
  PREFLIGHT_PROMPT="Full-auto multi-agent orchestration for ${TASK_ID}; requirements file: ${REQUIREMENTS_FILE}"
else
  PREFLIGHT_PROMPT="Full-auto multi-agent orchestration for ${TASK_ID}; inline requirement prompt provided."
fi

python3 scripts/foreman.py preflight --task "${TASK_ID}" --task-class standard --prompt "${PREFLIGHT_PROMPT}"

TASK_ACTIVE="$(
  python3 - "${REPO_ROOT}" "${TASK_ID}" <<'PY'
from __future__ import annotations

import re
import sys
from pathlib import Path


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


repo_root = Path(sys.argv[1]).resolve()
task_id = sys.argv[2]
tasks_text = (repo_root / "tasks.md").read_text(encoding="utf-8")
in_progress = extract_section(tasks_text, "In Progress")
active_ids = re.findall(r"^###\s+([A-Z0-9-]+):", in_progress, flags=re.MULTILINE)
print("yes" if task_id in active_ids else "no")
PY
)"

if [[ "${TASK_ACTIVE}" != "yes" ]]; then
  python3 scripts/foreman.py instantiate "${TASK_ID}"
  python3 scripts/foreman.py preflight --task "${TASK_ID}" --task-class standard --prompt "${PREFLIGHT_PROMPT}"
fi

AUTOPLAN_CMD=(bash scripts/multi_agent_autoplan.sh --task "${TASK_ID}")
if [[ -n "${REQUIREMENTS_FILE}" ]]; then
  AUTOPLAN_CMD+=(--requirements-file "${REQUIREMENTS_FILE}")
else
  AUTOPLAN_CMD+=(--prompt "${PROMPT_TEXT}")
fi
AUTOPLAN_CMD+=(--plan-output "${PLAN_OUTPUT}" --manifest-output "${MANIFEST_OUTPUT}")
if [[ -n "${PLANNER_MODEL}" ]]; then
  AUTOPLAN_CMD+=(--model "${PLANNER_MODEL}")
fi
if [[ -n "${PLANNER_PROFILE}" ]]; then
  AUTOPLAN_CMD+=(--profile "${PLANNER_PROFILE}")
fi
"${AUTOPLAN_CMD[@]}"

if [[ "${STOP_AFTER}" == "autoplan" ]]; then
  echo "full-auto stopped after autoplan for ${TASK_ID}"
  exit 0
fi

bash scripts/multi_agent_prepare.sh --task "${TASK_ID}" --manifest "${MANIFEST_OUTPUT}"
bash scripts/multi_agent_launch.sh --manifest "${MANIFEST_OUTPUT}"

python3 - "${REPO_ROOT}" "${MANIFEST_OUTPUT}" "${WAIT_TIMEOUT}" <<'PY'
from __future__ import annotations

import json
import os
import sys
import time
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
timeout_seconds = int(sys.argv[3])
manifest = load_json(manifest_path)
task_id = manifest["task_id"]
run_root = resolve_repo_relative(repo_root, str(manifest.get("run_root", f".codex/state/multi-agent/{task_id}")))
meta_dir = run_root / "meta"

deadline = time.time() + timeout_seconds
while True:
    active = []
    for agent in manifest.get("agents", []):
        meta_path = meta_dir / f"{agent['name']}.json"
        if not meta_path.exists():
            continue
        meta = load_json(meta_path)
        pid = meta.get("pid") if isinstance(meta.get("pid"), int) else None
        if running(pid):
            active.append(f"{agent['name']}:{pid}")
    if not active:
        print(f"all launched agents stopped for {task_id}")
        break
    if time.time() >= deadline:
        fail(
            f"Timed out waiting for launched agents to stop for {task_id}: "
            + ", ".join(active)
        )
    print("waiting for agents: " + ", ".join(active))
    time.sleep(5)
PY

bash scripts/multi_agent_collect.sh --manifest "${MANIFEST_OUTPUT}"

if [[ "${STOP_AFTER}" == "collect" ]]; then
  echo "full-auto stopped after collect for ${TASK_ID}"
  exit 0
fi

python3 - "${REPO_ROOT}" "${TASK_ID}" "${PLAN_OUTPUT}" "${MANIFEST_OUTPUT}" "${REQUIREMENTS_FILE}" "${PROMPT_TEXT}" "${FOREMAN_MODEL}" "${FOREMAN_PROFILE}" <<'PY'
from __future__ import annotations

import json
import shlex
import subprocess
import sys
from pathlib import Path


def fail(message: str) -> None:
    raise SystemExit(message)


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


repo_root = Path(sys.argv[1]).resolve()
task_id = sys.argv[2]
plan_output = resolve_repo_relative(repo_root, sys.argv[3])
manifest_output = resolve_repo_relative(repo_root, sys.argv[4])
requirements_file = sys.argv[5]
prompt_text = sys.argv[6]
model = sys.argv[7]
profile = sys.argv[8]

run_root = repo_root / ".codex" / "state" / "multi-agent" / task_id
prompts_dir = run_root / "prompts"
logs_dir = run_root / "logs"
messages_dir = run_root / "messages"
commands_dir = run_root / "commands"
for directory in [run_root, prompts_dir, logs_dir, messages_dir, commands_dir]:
    directory.mkdir(parents=True, exist_ok=True)

requirements_artifact = run_root / "requirements.md"
collect_summary = run_root / "collect-summary.md"
collect_json = run_root / "collect-summary.json"
if not requirements_artifact.exists():
    if requirements_file:
        source = resolve_repo_relative(repo_root, requirements_file)
        requirements_artifact.write_text(source.read_text(encoding="utf-8"), encoding="utf-8")
    else:
        requirements_artifact.write_text(prompt_text.rstrip() + "\n", encoding="utf-8")

template = (repo_root / "docs" / "agent-prompts" / "auto-foreman.md").read_text(encoding="utf-8").rstrip()
rendered_prompt = (
    template
    + "\n\n## Runtime Assignment\n\n"
    + f"- Task ID: `{task_id}`\n"
    + f"- Requirements artifact: `{requirements_artifact.relative_to(repo_root).as_posix()}`\n"
    + f"- Generated plan: `{plan_output.relative_to(repo_root).as_posix()}`\n"
    + f"- Manifest: `{manifest_output.relative_to(repo_root).as_posix()}`\n"
    + f"- Collect summary: `{collect_summary.relative_to(repo_root).as_posix()}`\n"
    + f"- Collect json: `{collect_json.relative_to(repo_root).as_posix()}`\n"
    + "- Acceptance rule: only absorb patches that collect marked acceptable.\n"
    + "- Closeout rule: you must still run foreman validate, pre-closeout audit, foreman closeout, and post-closeout audit.\n"
)
prompt_path = prompts_dir / "auto-foreman.md"
prompt_path.write_text(rendered_prompt + "\n", encoding="utf-8")

last_message_path = messages_dir / "auto-foreman.md"
console_log_path = logs_dir / "auto-foreman.log"
command = ["codex", "exec", "-C", str(repo_root), "--full-auto", "--color", "never", "-o", str(last_message_path)]
if profile:
    command.extend(["-p", profile])
if model:
    command.extend(["-m", model])

launch_script_path = commands_dir / "auto-foreman.sh"
launch_script_path.write_text(
    "#!/usr/bin/env bash\nset -euo pipefail\n"
    + " ".join(shlex.quote(part) for part in command)
    + f" < {shlex.quote(str(prompt_path))}\n",
    encoding="utf-8",
)
launch_script_path.chmod(0o755)

result = subprocess.run(["bash", str(launch_script_path)], cwd=repo_root, text=True, capture_output=True)
console_log_path.write_text((result.stdout or "") + (result.stderr or ""), encoding="utf-8")
if result.returncode != 0:
    fail(f"autonomous Main Foreman failed; see {console_log_path}")

metadata = {
    "task_id": task_id,
    "requirements_artifact": str(requirements_artifact),
    "plan_output": str(plan_output),
    "manifest_output": str(manifest_output),
    "collect_summary": str(collect_summary),
    "collect_json": str(collect_json),
    "prompt_path": str(prompt_path),
    "last_message_path": str(last_message_path),
    "console_log_path": str(console_log_path),
    "command": command,
}
(run_root / "auto-foreman-metadata.json").write_text(
    json.dumps(metadata, ensure_ascii=True, indent=2) + "\n",
    encoding="utf-8",
)

print(f"autonomous Main Foreman completed for {task_id}")
print(f"log: {console_log_path}")
print(f"last_message: {last_message_path}")
PY

echo "full-auto orchestration completed for ${TASK_ID}"
