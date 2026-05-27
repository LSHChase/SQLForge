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
OUTPUT_TEXT=""
CONSTRAINTS_TEXT=""
TEMPLATE_KIND=""
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/governed_intake.sh (--task <TASK_ID> | --requirements-file <FILE> | --prompt <TEXT>) [options]
       ./scripts/governed_intake.sh --confirm-run <RUN_ID> [--constraints <TEXT>] [--dry-run]

Options:
  --task <TASK_ID>               Existing formal task to route into governed execution.
  --requirements-file <FILE>     Raw requirement input for no-task shaping.
  --prompt <TEXT>                Inline raw requirement input for no-task shaping.
  --run-id <ID>                  Stable intake run id.
  --confirm-run <RUN_ID>         Execute a previously prepared intake run.
  --output <TEXT>                Structured output field captured in the execution preview.
  --constraints <TEXT>           Structured constraints field captured in the execution preview.
  --template-kind <KIND>         One of: business_requirement, governance_requirement, existing_task, existing_governance_task.
  --task-prefix <PREFIX>         Task prefix for no-task shaping. Defaults to HARN.
  --shaping-model <MODEL>        Optional model override for requirements_to_plan.sh.
  --shaping-profile <PROFILE>    Optional profile override for requirements_to_plan.sh.
  --dry-run                      Preview downstream execution without mutating repo-tracked files.
  -h, --help                     Show this help message.
EOF
}

append_summary_command() {
  local run_id="$1"
  local command_text="$2"
  local status="$3"
  local notes="$4"
  python3 - "${REPO_ROOT}" "${run_id}" "${command_text}" "${status}" "${notes}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import append_executed_command

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
command_text = sys.argv[3]
status = sys.argv[4]
notes = sys.argv[5]
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
append_executed_command(summary_path, command_text, status, notes)
PY
}

task_active() {
  python3 - "${REPO_ROOT}" "$1" <<'PY'
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
}

run_single_agent_route() {
  local task_id="$1"
  local requirements_ref="$2"
  local dry_run="$3"
  local preflight_prompt="Governed intake single-agent execution for ${task_id}; requirements artifact: ${requirements_ref}"

  if [[ "${dry_run}" == "true" ]]; then
    echo "[dry-run] python3 scripts/foreman.py preflight --task ${task_id} --task-class standard --prompt \"Governed intake single-agent execution for ${task_id}; requirements artifact: ${requirements_ref}\""
    echo "[dry-run] instantiate ${task_id} if it is not already active"
    echo "[dry-run] continue single-agent Main Foreman execution in the current Codex session"
    return 0
  fi

  python3 scripts/foreman.py preflight --task "${task_id}" --task-class standard --prompt "${preflight_prompt}"
  append_summary_command "${CONFIRM_RUN}" "python3 scripts/foreman.py preflight --task ${task_id} --task-class standard --prompt <governed-intake-single-agent>" "passed" "single-agent preflight"

  if [[ "$(task_active "${task_id}")" != "yes" ]]; then
    python3 scripts/foreman.py instantiate "${task_id}"
    append_summary_command "${CONFIRM_RUN}" "python3 scripts/foreman.py instantiate ${task_id}" "passed" "single-agent instantiate"
    python3 scripts/foreman.py preflight --task "${task_id}" --task-class standard --prompt "${preflight_prompt}"
    append_summary_command "${CONFIRM_RUN}" "python3 scripts/foreman.py preflight --task ${task_id} --task-class standard --prompt <governed-intake-single-agent>" "passed" "single-agent rebind after instantiate"
  fi
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
    --output)
      OUTPUT_TEXT="${2:-}"
      shift 2
      ;;
    --constraints)
      CONSTRAINTS_TEXT="${2:-}"
      shift 2
      ;;
    --template-kind)
      TEMPLATE_KIND="${2:-}"
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
  if [[ -n "${TASK_ID}" || -n "${PROMPT_TEXT}" || -n "${REQUIREMENTS_FILE}" || -n "${OUTPUT_TEXT}" || -n "${TEMPLATE_KIND}" ]]; then
    echo "--confirm-run must be used on its own, except for optional --constraints and --dry-run." >&2
    exit 1
  fi

  if [[ -n "${CONSTRAINTS_TEXT}" ]]; then
    python3 - "${REPO_ROOT}" "${CONFIRM_RUN}" "${CONSTRAINTS_TEXT}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import (
    build_execution_preview,
    lookup_task_profile,
    read_json,
    read_text,
    relative_to_root,
    render_requirements_artifact,
    render_execution_preview_markdown,
    write_json,
    write_run_summary,
    write_text,
)

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
extra_constraints = sys.argv[3].strip()
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
payload = read_json(summary_path, {})
if not payload:
    raise SystemExit(f"Missing intake summary for {run_id}")

existing_constraints = str(payload.get("constraints", "")).strip()
if existing_constraints and extra_constraints:
    merged_constraints = existing_constraints + "\n" + extra_constraints
elif extra_constraints:
    merged_constraints = extra_constraints
else:
    merged_constraints = existing_constraints

run_root = summary_path.parent
adapter_summary = read_json(run_root / "template-adapter-summary.json", {})
source_prompt = str(adapter_summary.get("raw_template", "")).strip()
shape_root = repo_root / ".codex" / "state" / "task-shaping" / run_id
candidate_pack = {}
if payload.get("candidate_task_pack"):
    candidate_pack = read_json(repo_root / str(payload.get("candidate_task_pack")), {})
if not source_prompt and (shape_root / "raw-requirement.md").exists():
    source_prompt = read_text(shape_root / "raw-requirement.md")

task_id = str(payload.get("task_id", "")).strip()
task_profile = lookup_task_profile(task_id) if task_id else {}
preview = build_execution_preview(
    run_id=run_id,
    path_selected=str(payload.get("path_selected", "")),
    template_kind=str(payload.get("template_kind", "")),
    task_id=task_id,
    candidate_task_id=str(payload.get("candidate_task_id", "")),
    task_profile=task_profile,
    candidate_pack=candidate_pack,
    output=str(payload.get("output", "")),
    constraints=merged_constraints,
    prompt_text=source_prompt,
    confirm_run_command=f"bash scripts/governed_intake.sh --confirm-run {run_id}",
    requirements_artifact_ref=str(payload.get("requirements_artifact_ref", "")),
)

preview_json_path = run_root / "execution-preview.json"
preview_md_path = run_root / "execution-preview.md"
write_json(preview_json_path, preview)
write_text(preview_md_path, render_execution_preview_markdown(preview))

requirements_ref = str(payload.get("requirements_artifact_ref", "")).strip()
if requirements_ref:
    requirements_path = repo_root / requirements_ref
    write_text(
        requirements_path,
        render_requirements_artifact(
            task_id=task_id,
            task_type=str(preview.get("task_type", "")),
            output=str(payload.get("output", "")),
            constraints=merged_constraints,
            preview=preview,
            source_prompt=source_prompt,
        ),
    )

write_run_summary(
    summary_path,
    {
        **payload,
        "constraints": merged_constraints,
        "execution_mode": preview.get("execution_mode", ""),
        "execution_mode_reason": preview.get("execution_mode_reason", ""),
        "planned_agents": preview.get("planned_agents", []),
        "mcp": preview.get("mcp", {}),
        "validation_path": preview.get("validation_path", []),
        "closeout_path": preview.get("closeout_path", []),
        "risk_summary": preview.get("risks", []),
        "execution_preview_ref": relative_to_root(preview_json_path),
        "execution_preview_markdown_ref": relative_to_root(preview_md_path),
    },
)
PY
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
print(payload.get("requirements_artifact_ref", ""))
print(payload.get("execution_mode", ""))
print(payload.get("execution_preview_markdown_ref", ""))
PY
)
  PATH_SELECTED="${CONFIRM_META[0]:-}"
  TASK_ID="${CONFIRM_META[1]:-}"
  CANDIDATE_TASK_PACK="${CONFIRM_META[2]:-}"
  REQUIREMENTS_ARTIFACT_REF="${CONFIRM_META[3]:-}"
  EXECUTION_MODE="${CONFIRM_META[4]:-}"
  EXECUTION_PREVIEW_MARKDOWN_REF="${CONFIRM_META[5]:-}"

  if [[ -z "${PATH_SELECTED}" || -z "${EXECUTION_MODE}" ]]; then
    echo "Invalid intake summary for ${CONFIRM_RUN}" >&2
    exit 1
  fi

  if [[ "${DRY_RUN}" != "true" ]]; then
    python3 scripts/governed_healthcheck.py --check --run-id "${CONFIRM_RUN}-pre-confirm-health"
    append_summary_command "${CONFIRM_RUN}" "python3 scripts/governed_healthcheck.py --check --run-id ${CONFIRM_RUN}-pre-confirm-health" "passed" "confirm-run healthcheck"
  fi

  if [[ "${EXECUTION_MODE}" == "preview-only" ]]; then
    python3 - "${REPO_ROOT}" "${CONFIRM_RUN}" "${DRY_RUN}" "${EXECUTION_PREVIEW_MARKDOWN_REF}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import read_json, write_run_summary

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
dry_run = sys.argv[3].lower() == "true"
preview_ref = sys.argv[4]
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
payload = read_json(summary_path, {})
write_run_summary(
    summary_path,
    {
        **payload,
        "confirmation_state": "dry_run_previewed" if dry_run else "blocked",
        "execution_state": "completed",
        "final_outcome": "preview_only_requires_manual_routing",
        "recommended_next_step": f"Review {preview_ref} and clarify ownership/scope or force a governed single-agent path before retrying confirm-run.",
    },
)
PY
    if [[ "${DRY_RUN}" == "true" ]]; then
      echo "[dry-run] execution mode remained preview-only; no downstream execution will be launched"
      echo "[dry-run] review ${EXECUTION_PREVIEW_MARKDOWN_REF} and clarify scope/ownership before rerunning confirm-run"
      exit 0
    fi
    echo "Execution mode remained preview-only for ${CONFIRM_RUN}; clarify the boundary before retrying." >&2
    exit 1
  fi

  if [[ "${PATH_SELECTED}" == "no-task-shaping" ]]; then
    MATERIALIZE_CMD=(bash scripts/task_materialize.sh --task-pack "${CANDIDATE_TASK_PACK}")
    if [[ "${DRY_RUN}" == "true" ]]; then
      MATERIALIZE_CMD+=(--dry-run)
    fi
    "${MATERIALIZE_CMD[@]}"
    append_summary_command "${CONFIRM_RUN}" "bash scripts/task_materialize.sh --task-pack ${CANDIDATE_TASK_PACK}" "passed" "confirm-run materialize"

    if [[ -z "${REQUIREMENTS_ARTIFACT_REF}" ]]; then
      REQUIREMENTS_ARTIFACT_REF="docs/references/raw-requirements/generated/${TASK_ID}-requirement.md"
    fi
  fi

  case "${EXECUTION_MODE}" in
    single-agent)
      if [[ "${PATH_SELECTED}" == "no-task-shaping" && "${DRY_RUN}" == "true" ]]; then
        echo "[dry-run] single-agent route would bind ${TASK_ID} after materialization and continue in the current Main Foreman session"
      else
        run_single_agent_route "${TASK_ID}" "${REQUIREMENTS_ARTIFACT_REF}" "${DRY_RUN}"
      fi
      ;;
    multi-agent-full-auto)
      FULL_AUTO_CMD=(bash scripts/multi_agent_full_auto.sh --task "${TASK_ID}" --requirements-file "${REQUIREMENTS_ARTIFACT_REF}")
      if [[ "${DRY_RUN}" == "true" ]]; then
        FULL_AUTO_CMD+=(--dry-run)
      fi
      "${FULL_AUTO_CMD[@]}"
      append_summary_command "${CONFIRM_RUN}" "bash scripts/multi_agent_full_auto.sh --task ${TASK_ID} --requirements-file ${REQUIREMENTS_ARTIFACT_REF}" "passed" "confirm-run downstream full-auto"
      ;;
    *)
      echo "Unsupported execution mode in intake summary: ${EXECUTION_MODE}" >&2
      exit 1
      ;;
  esac

  python3 - "${REPO_ROOT}" "${CONFIRM_RUN}" "${EXECUTION_MODE}" "${PATH_SELECTED}" "${TASK_ID}" "${REQUIREMENTS_ARTIFACT_REF}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import read_json, write_run_summary

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
execution_mode = sys.argv[3]
path_selected = sys.argv[4]
task_id = sys.argv[5]
requirements_artifact_ref = sys.argv[6]
dry_run = sys.argv[7].lower() == "true"
summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
payload = read_json(summary_path, {})

if dry_run:
    outcome = "dry_run_ready"
    next_step = "Review the dry-run preview, then rerun confirm-run without --dry-run to execute."
elif execution_mode == "single-agent":
    outcome = "single_agent_ready"
    next_step = f"Continue implementation in the current Main Foreman session under {task_id}, then run validate/task_audit/closeout through foreman."
else:
    outcome = "executed"
    next_step = "Continue with validate/closeout evidence from the downstream task workflow."

write_run_summary(
    summary_path,
    {
        **payload,
        "requirements_artifact_ref": requirements_artifact_ref,
        "confirmation_state": "dry_run_previewed" if dry_run else "executed",
        "execution_state": "completed",
        "final_outcome": outcome,
        "recommended_next_step": next_step,
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
  RUN_ID="intake-$(TZ=Asia/Shanghai date +%Y%m%d%H%M%S)"
fi

INTAKE_ROOT="${REPO_ROOT}/.codex/state/intake/${RUN_ID}"
mkdir -p "${INTAKE_ROOT}"

if [[ -n "${TASK_ID}" ]]; then
  python3 - "${REPO_ROOT}" "${RUN_ID}" "${TASK_ID}" "${OUTPUT_TEXT}" "${CONSTRAINTS_TEXT}" "${TEMPLATE_KIND}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import (
    build_execution_preview,
    build_suggestion,
    lookup_task_profile,
    read_json,
    relative_to_root,
    render_requirements_artifact,
    write_execution_preview,
    write_run_summary,
    write_text,
)

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
task_id = sys.argv[3]
output = sys.argv[4].strip()
constraints = sys.argv[5].strip()
template_kind = sys.argv[6].strip()
run_root = repo_root / ".codex" / "state" / "intake" / run_id
summary_path = run_root / "intake-summary.json"
adapter_summary = read_json(run_root / "template-adapter-summary.json", {})
source_prompt = str(adapter_summary.get("raw_template", "")).strip()
if not output:
    output = str(adapter_summary.get("output", "")).strip()
if not constraints:
    constraints = str(adapter_summary.get("constraints", "")).strip()
if not template_kind:
    template_kind = str(adapter_summary.get("template_kind", "")).strip()
if not template_kind:
    template_kind = "existing_governance_task" if task_id.startswith("HARN-") else "existing_task"

requirements_path = run_root / "requirements-artifact.md"
preview = build_execution_preview(
    run_id=run_id,
    path_selected="existing-task",
    template_kind=template_kind,
    task_id=task_id,
    task_profile=lookup_task_profile(task_id),
    output=output,
    constraints=constraints,
    prompt_text=source_prompt,
    confirm_run_command=f"bash scripts/governed_intake.sh --confirm-run {run_id}",
    requirements_artifact_ref=relative_to_root(requirements_path),
)
preview_json_path, preview_md_path = write_execution_preview(run_root, preview)
write_text(
    requirements_path,
    render_requirements_artifact(
        task_id=task_id,
        task_type=str(preview.get("task_type", "")),
        output=output,
        constraints=constraints,
        preview=preview,
        source_prompt=source_prompt,
    ),
)

suggestions = [
    build_suggestion(
        issue_key="runtime_task_mismatch",
        summary="Confirm the task is the correct formal task before launching downstream automation.",
    )
]
if preview.get("execution_mode") == "preview-only":
    suggestions.append(
        build_suggestion(
            issue_key="materialization_blocked",
            summary="Execution preview remained preview-only because ownership or routing signals are still insufficient.",
        )
    )

write_run_summary(
    summary_path,
    {
        "run_id": run_id,
        "path_selected": "existing-task",
        "template_kind": template_kind,
        "task_type": preview.get("task_type", ""),
        "task_id": task_id,
        "candidate_task_id": "",
        "output": output,
        "constraints": constraints,
        "requirements_artifact_ref": relative_to_root(requirements_path),
        "execution_preview_ref": relative_to_root(preview_json_path),
        "execution_preview_markdown_ref": relative_to_root(preview_md_path),
        "execution_mode": preview.get("execution_mode", ""),
        "execution_mode_reason": preview.get("execution_mode_reason", ""),
        "planned_agents": preview.get("planned_agents", []),
        "mcp": preview.get("mcp", {}),
        "validation_path": preview.get("validation_path", []),
        "closeout_path": preview.get("closeout_path", []),
        "risk_summary": preview.get("risks", []),
        "confirmation_state": "awaiting-confirmation",
        "execution_state": "completed",
        "final_outcome": "candidate_ready",
        "executed_commands": [],
        "suggestions": suggestions,
        "recommended_next_step": f"Review {relative_to_root(preview_md_path)} and rerun ./scripts/governed_intake.sh --confirm-run {run_id}.",
    },
)
PY
  echo "Prepared existing-task intake for ${TASK_ID}"
  echo "Preview: .codex/state/intake/${RUN_ID}/execution-preview.md"
  echo "Requirements artifact: .codex/state/intake/${RUN_ID}/requirements-artifact.md"
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

python3 - "${REPO_ROOT}" "${RUN_ID}" "$(printf '%q ' "${REQ_CMD[@]}" | sed 's/[[:space:]]*$//')" "${OUTPUT_TEXT}" "${CONSTRAINTS_TEXT}" "${TEMPLATE_KIND}" <<'PY'
from __future__ import annotations

import sys
from pathlib import Path

from scripts.governed_v2_support import (
    append_executed_command,
    build_execution_preview,
    build_suggestion,
    parse_template_fields,
    read_json,
    read_text,
    relative_to_root,
    write_execution_preview,
    write_run_summary,
)

repo_root = Path(sys.argv[1]).resolve()
run_id = sys.argv[2]
command_text = sys.argv[3]
output = sys.argv[4].strip()
constraints = sys.argv[5].strip()
template_kind = sys.argv[6].strip()
shape_root = repo_root / ".codex" / "state" / "task-shaping" / run_id
shape_summary_path = shape_root / "run-summary.json"
shape_summary = read_json(shape_summary_path)
candidate_pack_path = shape_root / "candidate-task-pack.json"
candidate_pack_ref = relative_to_root(candidate_pack_path)
candidate_pack = read_json(candidate_pack_path)
raw_requirement_path = shape_root / "raw-requirement.md"
raw_requirement = read_text(raw_requirement_path) if raw_requirement_path.exists() else ""
parsed_fields = parse_template_fields(raw_requirement)

if not output:
    output = parsed_fields.get("输出物", "").strip()
if not constraints:
    constraints = parsed_fields.get("限制", "").strip()

preview = build_execution_preview(
    run_id=run_id,
    path_selected="no-task-shaping",
    template_kind=template_kind,
    task_id="",
    candidate_task_id=str(candidate_pack.get("task_id", "")).strip(),
    candidate_pack=candidate_pack,
    output=output,
    constraints=constraints,
    prompt_text=raw_requirement,
    confirm_run_command=f"bash scripts/governed_intake.sh --confirm-run {run_id}",
    requirements_artifact_ref=relative_to_root(raw_requirement_path),
)
preview_json_path, preview_md_path = write_execution_preview(repo_root / ".codex" / "state" / "intake" / run_id, preview)

suggestions = list(shape_summary.get("suggestions", []))
if preview.get("execution_mode") == "preview-only":
    suggestions.append(
        build_suggestion(
            issue_key="materialization_blocked",
            summary="Execution preview remained preview-only because ownership or routing signals are still insufficient.",
            human_confirmation_point=str(candidate_pack.get("human_confirmation_point", "")),
            authority_fields_to_confirm=list(candidate_pack.get("authority_fields_to_confirm", [])),
        )
    )

summary_path = repo_root / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
write_run_summary(
    summary_path,
    {
        "run_id": run_id,
        "path_selected": "no-task-shaping",
        "template_kind": template_kind,
        "task_type": preview.get("task_type", ""),
        "task_id": candidate_pack.get("task_id", ""),
        "candidate_task_id": candidate_pack.get("task_id", ""),
        "candidate_task_pack": candidate_pack_ref,
        "candidate_execution_plan": relative_to_root(shape_root / "candidate-execution-plan.md"),
        "requirements_artifact_ref": relative_to_root(raw_requirement_path),
        "output": output,
        "constraints": constraints,
        "execution_preview_ref": relative_to_root(preview_json_path),
        "execution_preview_markdown_ref": relative_to_root(preview_md_path),
        "execution_mode": preview.get("execution_mode", ""),
        "execution_mode_reason": preview.get("execution_mode_reason", ""),
        "planned_agents": preview.get("planned_agents", []),
        "mcp": preview.get("mcp", {}),
        "validation_path": preview.get("validation_path", []),
        "closeout_path": preview.get("closeout_path", []),
        "risk_summary": preview.get("risks", []),
        "confirmation_state": "awaiting-confirmation",
        "execution_state": "completed",
        "final_outcome": "candidate_ready",
        "executed_commands": shape_summary.get("executed_commands", []),
        "suggestions": suggestions,
        "recommended_next_step": f"Review {relative_to_root(preview_md_path)} and rerun ./scripts/governed_intake.sh --confirm-run {run_id}.",
    },
)
append_executed_command(summary_path, command_text, "passed", "requirements-to-plan")
PY

echo "Prepared no-task intake for run ${RUN_ID}"
echo "Preview: .codex/state/intake/${RUN_ID}/execution-preview.md"
echo "Confirm with: bash scripts/governed_intake.sh --confirm-run ${RUN_ID}"
