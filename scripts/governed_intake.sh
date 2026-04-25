#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

TASK_ID=""
PROMPT_TEXT=""
REQUIREMENTS_FILE=""
RUN_ID=""
CONFIRM_RUN=""
TASK_PREFIX="HARN"
SHAPING_MODEL=""
SHAPING_PROFILE=""
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/governed_intake.sh (--task <TASK_ID> | --requirements-file <FILE> | --prompt <TEXT>) [options]
       ./scripts/governed_intake.sh --confirm-run <RUN_ID> [--dry-run]

Options:
  --task <TASK_ID>               Existing formal task to hand off into full-auto execution.
  --requirements-file <FILE>     Raw requirement input for no-task shaping.
  --prompt <TEXT>                Inline raw requirement input for no-task shaping.
  --run-id <ID>                  Stable intake run id.
  --confirm-run <RUN_ID>         Execute a previously prepared intake run.
  --task-prefix <PREFIX>         Task prefix for no-task shaping. Defaults to HARN.
  --shaping-model <MODEL>        Optional model override for requirements_to_plan.sh.
  --shaping-profile <PROFILE>    Optional profile override for requirements_to_plan.sh.
  --dry-run                      Preview downstream execution without mutating repo-tracked files.
  -h, --help                     Show this help message.
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
    --run-id)
      RUN_ID="${2:-}"
      shift 2
      ;;
    --confirm-run)
      CONFIRM_RUN="${2:-}"
      shift 2
      ;;
    --task-prefix)
      TASK_PREFIX="${2:-}"
      shift 2
      ;;
    --shaping-model)
      SHAPING_MODEL="${2:-}"
      shift 2
      ;;
    --shaping-profile)
      SHAPING_PROFILE="${2:-}"
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

if [[ -n "${CONFIRM_RUN}" ]]; then
  if [[ -n "${TASK_ID}" || -n "${PROMPT_TEXT}" || -n "${REQUIREMENTS_FILE}" ]]; then
    echo "--confirm-run must be used on its own." >&2
    exit 1
  fi

  readarray -t CONFIRM_META < <(python3 - "${REPO_ROOT}" "${CONFIRM_RUN}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import read_json, relative_to_root

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
if not summary_path.exists():
    raise SystemExit(f"Missing intake summary: {relative_to_root(summary_path)}")
payload = read_json(summary_path)
print(payload.get("path_selected", ""))
print(payload.get("task_id", ""))
print(payload.get("candidate_task_pack", ""))
print(payload.get("requirements_archive_ref", ""))
PY
)
  PATH_SELECTED="${CONFIRM_META[0]:-}"
  TASK_ID="${CONFIRM_META[1]:-}"
  CANDIDATE_TASK_PACK="${CONFIRM_META[2]:-}"
  REQUIREMENTS_ARCHIVE_REF="${CONFIRM_META[3]:-}"

  if [[ -z "${PATH_SELECTED}" ]]; then
    echo "Invalid intake summary for ${CONFIRM_RUN}" >&2
    exit 1
  fi

  if [[ "${PATH_SELECTED}" == "existing-task" ]]; then
    CMD=(bash scripts/multi_agent_full_auto.sh --task "${TASK_ID}")
    if [[ "${DRY_RUN}" == "true" ]]; then
      CMD+=(--dry-run)
    fi
    "${CMD[@]}"
  else
    CMD=(bash scripts/task_materialize.sh --task-pack "${CANDIDATE_TASK_PACK}")
    if [[ "${DRY_RUN}" == "true" ]]; then
      CMD+=(--dry-run)
    fi
    "${CMD[@]}"

    if [[ "${DRY_RUN}" != "true" ]]; then
      if [[ -z "${REQUIREMENTS_ARCHIVE_REF}" ]]; then
        REQUIREMENTS_ARCHIVE_REF="docs/references/raw-requirements/generated/${TASK_ID}-requirement.md"
      fi
      bash scripts/multi_agent_full_auto.sh --task "${TASK_ID}" --requirements-file "${REQUIREMENTS_ARCHIVE_REF}"
    fi
  fi

  python3 - "${REPO_ROOT}" "${CONFIRM_RUN}" "${PATH_SELECTED}" "${TASK_ID}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import append_executed_command, read_json, write_run_summary

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
path_selected = sys.argv[3]
task_id = sys.argv[4]
dry_run = sys.argv[5].lower() == "true"
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
payload = read_json(summary_path)
requirements_archive_ref = payload.get("requirements_archive_ref", "") or f"docs/references/raw-requirements/generated/{task_id}-requirement.md"
if path_selected == "existing-task":
    append_executed_command(
        summary_path,
        f"bash scripts/multi_agent_full_auto.sh --task {task_id}" + (" --dry-run" if dry_run else ""),
        "passed",
        "confirm-run existing-task",
    )
else:
    append_executed_command(
        summary_path,
        f"bash scripts/task_materialize.sh --task-pack {payload.get('candidate_task_pack', '')}" + (" --dry-run" if dry_run else ""),
        "passed",
        "confirm-run materialize",
    )
    if not dry_run:
        append_executed_command(
            summary_path,
            f"bash scripts/multi_agent_full_auto.sh --task {task_id} --requirements-file {requirements_archive_ref}",
            "passed",
            "confirm-run downstream full-auto",
        )
write_run_summary(
    summary_path,
    {
        **read_json(summary_path),
        "requirements_archive_ref": requirements_archive_ref,
        "confirmation_state": "dry_run_previewed" if dry_run else "executed",
        "execution_state": "completed",
        "final_outcome": "executed" if not dry_run else "dry_run_ready",
        "recommended_next_step": "Review the dry-run preview, then rerun confirm-run without --dry-run to execute."
        if dry_run
        else "Continue with validate/closeout evidence from the downstream task workflow.",
    },
)
PY

  echo "governed intake confirm-run completed for ${CONFIRM_RUN}"
  exit 0
fi

if [[ -n "${TASK_ID}" ]]; then
  if [[ -n "${PROMPT_TEXT}" || -n "${REQUIREMENTS_FILE}" ]]; then
    echo "--task cannot be combined with --prompt/--requirements-file." >&2
    exit 1
  fi
else
  if [[ -n "${PROMPT_TEXT}" && -n "${REQUIREMENTS_FILE}" ]]; then
    echo "Use either --prompt or --requirements-file, not both." >&2
    exit 1
  fi
  if [[ -z "${PROMPT_TEXT}" && -z "${REQUIREMENTS_FILE}" ]]; then
    echo "Provide --task, --prompt, or --requirements-file." >&2
    exit 1
  fi
fi

if [[ -z "${RUN_ID}" ]]; then
  RUN_ID="intake-$(date +%Y%m%d%H%M%S)"
fi

INTAKE_ROOT="${REPO_ROOT}/.codex/state/intake/${RUN_ID}"
mkdir -p "${INTAKE_ROOT}"

if [[ -n "${TASK_ID}" ]]; then
  python3 - "${REPO_ROOT}" "${RUN_ID}" "${TASK_ID}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import build_suggestion, write_run_summary

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
task_id = sys.argv[3]
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
write_run_summary(
    summary_path,
    {
        "run_id": run_id,
        "path_selected": "existing-task",
        "task_id": task_id,
        "confirmation_state": "awaiting-confirmation",
        "execution_state": "completed",
        "final_outcome": "candidate_ready",
        "executed_commands": [],
        "suggestions": [
            build_suggestion(
                issue_key="runtime_task_mismatch",
                summary="Confirm the task is the correct formal task before launching downstream automation.",
            )
        ],
        "recommended_next_step": f"Review the task id, then rerun ./scripts/governed_intake.sh --confirm-run {run_id}.",
    },
)
PY
  echo "Prepared existing-task intake for ${TASK_ID}"
  echo "Confirm with: bash scripts/governed_intake.sh --confirm-run ${RUN_ID}"
  exit 0
fi

REQ_CMD=(bash scripts/requirements_to_plan.sh --run-id "${RUN_ID}" --task-prefix "${TASK_PREFIX}")
if [[ -n "${REQUIREMENTS_FILE}" ]]; then
  REQ_CMD+=(--requirements-file "${REQUIREMENTS_FILE}")
else
  REQ_CMD+=(--prompt "${PROMPT_TEXT}")
fi
if [[ -n "${SHAPING_MODEL}" ]]; then
  REQ_CMD+=(--model "${SHAPING_MODEL}")
fi
if [[ -n "${SHAPING_PROFILE}" ]]; then
  REQ_CMD+=(--profile "${SHAPING_PROFILE}")
fi
"${REQ_CMD[@]}"

python3 - "${REPO_ROOT}" "${RUN_ID}" "$(printf '%q ' "${REQ_CMD[@]}" | sed 's/[[:space:]]*$//')" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import append_executed_command, read_json, relative_to_root, write_run_summary

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
command_text = sys.argv[3]
shape_root = repo_root / ".codex" / "state" / "task-shaping" / run_id
shape_summary_path = shape_root / "run-summary.json"
shape_summary = read_json(shape_summary_path)
candidate_pack_ref = relative_to_root(shape_root / "candidate-task-pack.json")
candidate_pack = read_json(shape_root / "candidate-task-pack.json")
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
write_run_summary(
    summary_path,
    {
        "run_id": run_id,
        "path_selected": "no-task-shaping",
        "task_id": candidate_pack.get("task_id", ""),
        "candidate_task_pack": candidate_pack_ref,
        "candidate_execution_plan": relative_to_root(shape_root / "candidate-execution-plan.md"),
        "requirements_archive_ref": "",
        "confirmation_state": "awaiting-confirmation",
        "execution_state": "completed",
        "final_outcome": "candidate_ready",
        "executed_commands": shape_summary.get("executed_commands", []),
        "suggestions": shape_summary.get("suggestions", []),
        "recommended_next_step": f"Review the candidate pack and rerun ./scripts/governed_intake.sh --confirm-run {run_id}.",
    },
)
append_executed_command(summary_path, command_text, "passed", "requirements-to-plan")
PY

echo "Prepared no-task intake for run ${RUN_ID}"
echo "Confirm with: bash scripts/governed_intake.sh --confirm-run ${RUN_ID}"
