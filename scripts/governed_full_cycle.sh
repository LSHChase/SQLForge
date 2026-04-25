#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

REQUIREMENTS_FILE=""
PROMPT_TEXT=""
RUN_ID=""
TASK_PREFIX="HARN"
SHAPING_MODEL=""
SHAPING_PROFILE=""
REVIEW_MODEL=""
REVIEW_PROFILE=""
EXECUTION_PLANNER_MODEL=""
EXECUTION_PLANNER_PROFILE=""
EXECUTION_FOREMAN_MODEL=""
EXECUTION_FOREMAN_PROFILE=""
STOP_AFTER="closeout"
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/governed_full_cycle.sh (--requirements-file <FILE> | --prompt <TEXT>) [options]

Options:
  --requirements-file <FILE>        Markdown/text file containing the raw requirement input.
  --prompt <TEXT>                   Inline requirement input.
  --run-id <ID>                     Stable shaping run id.
  --task-prefix <PREFIX>            Task id prefix to allocate. Defaults to HARN.
  --shaping-model <MODEL>           Optional Codex model override for requirements_to_plan.
  --shaping-profile <PROFILE>       Optional Codex profile override for requirements_to_plan.
  --review-model <MODEL>            Optional Codex model override for task materialization review.
  --review-profile <PROFILE>        Optional Codex profile override for task materialization review.
  --execution-planner-model <MODEL> Optional downstream full-auto planner model override.
  --execution-planner-profile <PROFILE> Optional downstream full-auto planner profile override.
  --execution-foreman-model <MODEL> Optional downstream auto-foreman model override.
  --execution-foreman-profile <PROFILE> Optional downstream auto-foreman profile override.
  --stop-after <PHASE>              One of: normalize, plan, materialize, collect, closeout. Default: closeout.
  --dry-run                         Print the orchestrated flow without mutating repo-tracked files.
  -h, --help                        Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
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
    --review-model)
      REVIEW_MODEL="${2:-}"
      shift 2
      ;;
    --review-profile)
      REVIEW_PROFILE="${2:-}"
      shift 2
      ;;
    --execution-planner-model)
      EXECUTION_PLANNER_MODEL="${2:-}"
      shift 2
      ;;
    --execution-planner-profile)
      EXECUTION_PLANNER_PROFILE="${2:-}"
      shift 2
      ;;
    --execution-foreman-model)
      EXECUTION_FOREMAN_MODEL="${2:-}"
      shift 2
      ;;
    --execution-foreman-profile)
      EXECUTION_FOREMAN_PROFILE="${2:-}"
      shift 2
      ;;
    --stop-after)
      STOP_AFTER="${2:-}"
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

if [[ -n "${REQUIREMENTS_FILE}" && -n "${PROMPT_TEXT}" ]]; then
  echo "Use either --requirements-file or --prompt, not both." >&2
  exit 1
fi

if [[ -z "${REQUIREMENTS_FILE}" && -z "${PROMPT_TEXT}" ]]; then
  echo "One of --requirements-file or --prompt is required." >&2
  exit 1
fi

case "${STOP_AFTER}" in
  normalize|plan|materialize|collect|closeout)
    ;;
  *)
    echo "Unsupported --stop-after value: ${STOP_AFTER}" >&2
    exit 1
    ;;
esac

if [[ -z "${RUN_ID}" ]]; then
  RUN_ID="${TASK_PREFIX,,}-full-cycle-$(date +%Y%m%d%H%M%S)"
fi

if [[ "${DRY_RUN}" == "true" ]]; then
  echo "[dry-run] bash scripts/requirements_to_plan.sh --run-id ${RUN_ID} --task-prefix ${TASK_PREFIX} ..."
  echo "[dry-run] bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/${RUN_ID}/candidate-task-pack.json ..."
  echo "[dry-run] bash scripts/multi_agent_full_auto.sh --task <generated-task-id> --requirements-file docs/references/raw-requirements/generated/<generated-task-id>-requirement.md ..."
  echo "[dry-run] stop-after: ${STOP_AFTER}"
  exit 0
fi

python3 scripts/governed_healthcheck.py --check --run-id "${RUN_ID}-pre-full-cycle-health"

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

if [[ "${STOP_AFTER}" == "normalize" ]]; then
  REQ_CMD+=(--stop-after normalize)
  "${REQ_CMD[@]}"
  exit 0
fi

if [[ "${STOP_AFTER}" == "plan" ]]; then
  REQ_CMD+=(--stop-after plan)
  "${REQ_CMD[@]}"
  exit 0
fi

"${REQ_CMD[@]}"

TASK_PACK_PATH=".codex/state/task-shaping/${RUN_ID}/candidate-task-pack.json"
RAW_REQUIREMENT_PATH=".codex/state/task-shaping/${RUN_ID}/raw-requirement.md"
PLAN_PATH=".codex/state/task-shaping/${RUN_ID}/candidate-execution-plan.md"

MAT_CMD=(bash scripts/task_materialize.sh --task-pack "${TASK_PACK_PATH}" --candidate-plan "${PLAN_PATH}" --requirements-source "${RAW_REQUIREMENT_PATH}")
if [[ -n "${REVIEW_MODEL}" ]]; then
  MAT_CMD+=(--review-model "${REVIEW_MODEL}")
fi
if [[ -n "${REVIEW_PROFILE}" ]]; then
  MAT_CMD+=(--review-profile "${REVIEW_PROFILE}")
fi
"${MAT_CMD[@]}"

if [[ "${STOP_AFTER}" == "materialize" ]]; then
  echo "governed full-cycle stopped after materialize for run ${RUN_ID}"
  exit 0
fi

TASK_ID="$(
  python3 - "${REPO_ROOT}" "${TASK_PACK_PATH}" <<'PY'
from __future__ import annotations

import json
import sys
from pathlib import Path

repo_root = Path(sys.argv[1]).resolve()
task_pack_path = Path(sys.argv[2])
if not task_pack_path.is_absolute():
    task_pack_path = repo_root / task_pack_path
payload = json.loads(task_pack_path.read_text(encoding="utf-8"))
print(payload["task_id"])
PY
)"

DOWNSTREAM_REQ="docs/references/raw-requirements/generated/${TASK_ID}-requirement.md"
FULL_AUTO_CMD=(bash scripts/multi_agent_full_auto.sh --task "${TASK_ID}" --requirements-file "${DOWNSTREAM_REQ}")
if [[ -n "${EXECUTION_PLANNER_MODEL}" ]]; then
  FULL_AUTO_CMD+=(--planner-model "${EXECUTION_PLANNER_MODEL}")
fi
if [[ -n "${EXECUTION_PLANNER_PROFILE}" ]]; then
  FULL_AUTO_CMD+=(--planner-profile "${EXECUTION_PLANNER_PROFILE}")
fi
if [[ -n "${EXECUTION_FOREMAN_MODEL}" ]]; then
  FULL_AUTO_CMD+=(--foreman-model "${EXECUTION_FOREMAN_MODEL}")
fi
if [[ -n "${EXECUTION_FOREMAN_PROFILE}" ]]; then
  FULL_AUTO_CMD+=(--foreman-profile "${EXECUTION_FOREMAN_PROFILE}")
fi
if [[ "${STOP_AFTER}" == "collect" ]]; then
  FULL_AUTO_CMD+=(--stop-after collect)
fi
"${FULL_AUTO_CMD[@]}"
