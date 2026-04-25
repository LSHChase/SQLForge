#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

REQUIREMENTS_FILE=""
PROMPT_TEXT=""
RUN_ID=""
TASK_PREFIX="HARN"
MODEL=""
PROFILE=""
STOP_AFTER="pack"
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/requirements_to_plan.sh (--requirements-file <FILE> | --prompt <TEXT>) [options]

Options:
  --requirements-file <FILE>  Markdown/text file containing the raw requirement input.
  --prompt <TEXT>             Inline requirement input.
  --run-id <ID>               Stable run id. Defaults to a timestamp-based id.
  --task-prefix <PREFIX>      Task id prefix to allocate. Defaults to HARN.
  --model <MODEL>             Optional Codex model override for shaping prompts.
  --profile <PROFILE>         Optional Codex profile override for shaping prompts.
  --stop-after <PHASE>        One of: normalize, plan, pack. Default: pack.
  --dry-run                   Print the planned shaping run without executing Codex.
  -h, --help                  Show this help message.
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
    --model)
      MODEL="${2:-}"
      shift 2
      ;;
    --profile)
      PROFILE="${2:-}"
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
  normalize|plan|pack)
    ;;
  *)
    echo "Unsupported --stop-after value: ${STOP_AFTER}" >&2
    exit 1
    ;;
esac

python3 - "${REPO_ROOT}" "${REQUIREMENTS_FILE}" "${PROMPT_TEXT}" "${RUN_ID}" "${TASK_PREFIX}" "${MODEL}" "${PROFILE}" "${STOP_AFTER}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

from scripts.governed_v2_support import (
    append_executed_command,
    build_suggestion,
    command_to_text,
    release_reservation,
    relative_to_root,
    reserve_task_id,
    update_reservation,
    write_run_summary,
)


NORMALIZE_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": [
        "summary",
        "goals",
        "success_criteria",
        "constraints",
        "out_of_scope",
        "recommended_task_class",
        "notes",
    ],
    "properties": {
        "summary": {"type": "string"},
        "goals": {"type": "array", "items": {"type": "string"}},
        "success_criteria": {"type": "array", "items": {"type": "string"}},
        "constraints": {"type": "array", "items": {"type": "string"}},
        "out_of_scope": {"type": "array", "items": {"type": "string"}},
        "recommended_task_class": {
            "type": "string",
            "enum": ["advisory", "trivial", "standard", "delivery"],
        },
        "notes": {"type": "array", "items": {"type": "string"}},
    },
}

PLAN_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": [
        "summary",
        "story_id",
        "dependencies",
        "plan_markdown",
        "validation_focus",
        "notes",
    ],
    "properties": {
        "summary": {"type": "string"},
        "story_id": {"type": "string"},
        "dependencies": {"type": "array", "items": {"type": "string"}},
        "plan_markdown": {"type": "string"},
        "validation_focus": {"type": "array", "items": {"type": "string"}},
        "notes": {"type": "array", "items": {"type": "string"}},
    },
}

TASK_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": [
        "title",
        "task_class",
        "priority",
        "depends_on",
        "scope",
        "adr_refs",
        "rule_refs",
        "context_aliases",
        "contract",
        "tech",
        "layer",
        "tests",
        "env",
        "human_confirmation_point",
        "requires_human_decision",
        "data_impact",
        "rollback_recovery",
        "task_summary",
        "residual_risk",
    ],
    "properties": {
        "title": {"type": "string"},
        "task_class": {
            "type": "string",
            "enum": ["advisory", "trivial", "standard", "delivery"],
        },
        "priority": {"type": "string"},
        "depends_on": {"type": "array", "items": {"type": "string"}},
        "scope": {"type": "string"},
        "adr_refs": {"type": "array", "items": {"type": "string"}},
        "rule_refs": {"type": "array", "items": {"type": "string"}},
        "context_aliases": {"type": "array", "items": {"type": "string"}},
        "contract": {"type": "string"},
        "tech": {"type": "array", "items": {"type": "string"}},
        "layer": {"type": "array", "items": {"type": "string"}},
        "tests": {"type": "array", "items": {"type": "string"}},
        "env": {"type": "string"},
        "human_confirmation_point": {"type": "string"},
        "requires_human_decision": {"type": "boolean"},
        "data_impact": {"type": "string"},
        "rollback_recovery": {"type": "string"},
        "task_summary": {"type": "string"},
        "residual_risk": {"type": "array", "items": {"type": "string"}},
    },
}


def fail(message: str) -> None:
    raise SystemExit(message)


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


def now_compact() -> str:
    return datetime.now(timezone.utc).astimezone().strftime("%Y%m%d%H%M%S")


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        fail(f"Missing required file: {path}")
        raise exc
    except json.JSONDecodeError as exc:
        fail(f"Invalid json in {path}: {exc}")
        raise exc


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
    schema_path: Path,
    result_path: Path,
    model: str,
    profile: str,
) -> list[str]:
    command = ["codex", "exec", "-C", str(workspace), "--disable", "codex_hooks"]
    mode = codex_exec_mode()
    if mode == "bypass":
        command.append("--dangerously-bypass-approvals-and-sandbox")
    elif mode == "full-auto":
        command.append("--full-auto")
    else:
        command.extend(["--sandbox", mode])
    command.extend(["--color", "never", "--output-schema", str(schema_path), "-o", str(result_path)])
    if profile:
        command.extend(["-p", profile])
    if model:
        command.extend(["-m", model])
    return command


def run_codex(
    repo_root: Path,
    prompt_path: Path,
    schema: dict,
    result_path: Path,
    log_path: Path,
    model: str,
    profile: str,
) -> dict:
    schema_path = result_path.with_suffix(".schema.json")
    write_json(schema_path, schema)
    command = build_codex_command(repo_root, schema_path, result_path, model, profile)
    result = subprocess.run(
        command,
        cwd=repo_root,
        text=True,
        input=prompt_path.read_text(encoding="utf-8"),
        capture_output=True,
    )
    log_path.write_text((result.stdout or "") + (result.stderr or ""), encoding="utf-8")
    if result.returncode != 0:
        fail(f"Codex shaping step failed; see {log_path}")
    return load_json(result_path)


def collect_story_catalog(master_plan: str) -> list[dict]:
    phase = ""
    stories: list[dict] = []
    for line in master_plan.splitlines():
        phase_match = re.match(r"^###\s+(Phase-[A-Z])(?:\s+.+)?$", line)
        if phase_match:
            phase = phase_match.group(1)
            continue
        story_match = re.match(r"^##### Story `([^`]+)`\s+(.+)$", line)
        if story_match:
            stories.append(
                {
                    "phase": phase,
                    "story_id": story_match.group(1),
                    "story_title": story_match.group(2).strip(),
                }
            )
    return stories


def next_task_id(repo_root: Path, prefix: str) -> str:
    sources = [
        repo_root / "docs" / "plans" / "master-execution-plan.md",
        repo_root / "docs" / "plans" / "task-spec-matrix.md",
        repo_root / "docs" / "plans" / "task-governance-extension-matrix.md",
        repo_root / "tasks.md",
        repo_root / "tasks-done.md",
        repo_root / ".codex" / "state" / "current-task.json",
    ]
    pattern = re.compile(rf"{re.escape(prefix)}-(\d{{3}})\b")
    max_number = 0
    for source in sources:
        if not source.exists():
            continue
        text = source.read_text(encoding="utf-8")
        for match in pattern.findall(text):
            max_number = max(max_number, int(match))
    return f"{prefix}-{max_number + 1:03d}"


def render_normalized_markdown(payload: dict) -> str:
    lines = [
        "# Normalized Requirement",
        "",
        "## Summary",
        "",
        payload["summary"],
        "",
        "## Goals",
        "",
    ]
    for item in payload["goals"]:
        lines.append(f"- {item}")
    lines.extend(["", "## Success Criteria", ""])
    for item in payload["success_criteria"]:
        lines.append(f"- {item}")
    lines.extend(["", "## Constraints", ""])
    for item in payload["constraints"]:
        lines.append(f"- {item}")
    lines.extend(["", "## Out Of Scope", ""])
    for item in payload["out_of_scope"]:
        lines.append(f"- {item}")
    lines.extend(["", "## Recommended Task Class", "", f"- `{payload['recommended_task_class']}`", "", "## Notes", ""])
    for item in payload["notes"]:
        lines.append(f"- {item}")
    return "\n".join(lines).rstrip() + "\n"


repo_root = Path(sys.argv[1]).resolve()
requirements_file = sys.argv[2]
prompt_text = sys.argv[3]
run_id_raw = sys.argv[4]
task_prefix = sys.argv[5].strip()
model = sys.argv[6]
profile = sys.argv[7]
stop_after = sys.argv[8]
dry_run = sys.argv[9].lower() == "true"

if not task_prefix:
    fail("task prefix cannot be empty")

run_id = run_id_raw.strip() or f"{task_prefix.lower()}-shaping-{now_compact()}"
run_root = repo_root / ".codex" / "state" / "task-shaping" / run_id
prompts_dir = run_root / "prompts"
outputs_dir = run_root / "outputs"
logs_dir = run_root / "logs"
for directory in [run_root, prompts_dir, outputs_dir, logs_dir]:
    directory.mkdir(parents=True, exist_ok=True)

if requirements_file:
    requirements_path = resolve_repo_relative(repo_root, requirements_file)
    if not requirements_path.exists():
        fail(f"Requirements file does not exist: {requirements_path}")
    raw_requirement = requirements_path.read_text(encoding="utf-8").strip()
    source_ref = str(requirements_path.relative_to(repo_root))
else:
    raw_requirement = prompt_text.strip()
    source_ref = "inline-prompt"

if not raw_requirement:
    fail("Requirement input is empty.")

raw_requirement_path = run_root / "raw-requirement.md"
raw_requirement_path.write_text(raw_requirement + "\n", encoding="utf-8")

story_catalog = collect_story_catalog((repo_root / "docs" / "plans" / "master-execution-plan.md").read_text(encoding="utf-8"))
candidate_task_id, reservation_path = reserve_task_id(task_prefix, run_id)
run_summary_path = run_root / "run-summary.json"

normalize_result_path = outputs_dir / "normalized-requirements.json"
normalize_log_path = logs_dir / "requirement-normalizer.log"
plan_result_path = outputs_dir / "plan-shaper.json"
plan_log_path = logs_dir / "plan-shaper.log"
task_result_path = outputs_dir / "task-shaper.json"
task_log_path = logs_dir / "task-shaper.log"
normalized_markdown_path = run_root / "normalized-requirements.md"
candidate_plan_path = run_root / "candidate-execution-plan.md"
candidate_task_pack_path = run_root / "candidate-task-pack.json"

normalize_prompt = (
    (repo_root / "docs" / "agent-prompts" / "requirement-normalizer.md").read_text(encoding="utf-8").rstrip()
    + "\n\n## Runtime Assignment\n\n"
    + f"- Source requirement ref: `{source_ref}`\n"
    + f"- Candidate task prefix: `{task_prefix}`\n"
    + "- Preserve user hard constraints and keep speculative content inside notes.\n"
    + "\n## Requirement Input\n\n"
    + raw_requirement
    + "\n"
)
normalize_prompt_path = prompts_dir / "requirement-normalizer.md"
normalize_prompt_path.write_text(normalize_prompt, encoding="utf-8")

write_run_summary(
    run_summary_path,
    {
        "run_id": run_id,
        "path_selected": "no-task-shaping",
        "task_id": "",
        "candidate_task_id": candidate_task_id,
        "confirmation_state": "not-required",
        "execution_state": "planning",
        "truth_sources": [
            "docs/README.md",
            "docs/plans/document-truth-baseline.md",
            "docs/architecture/init.md",
            "docs/rules/codex-rules.md",
            "docs/quality/validation-rules.md",
            "docs/plans/master-execution-plan.md",
            "docs/plans/task-spec-matrix.md",
            "docs/plans/task-governance-extension-matrix.md",
            "tasks.md",
            "tasks-done.md",
            "INBOX.md",
        ],
        "blockers": [],
        "executed_commands": [],
        "final_outcome": "in_progress",
        "recommended_next_step": "Continue shaping, then review the candidate task pack before materialization.",
        "reservation_ref": relative_to_root(reservation_path),
        "suggestions": [],
    },
)

if dry_run:
    release_reservation(reservation_path, "requirements-to-plan dry-run completed", dry_run=True)
    write_run_summary(
        run_summary_path,
        {
            **json.loads(run_summary_path.read_text(encoding="utf-8")),
            "execution_state": "completed",
            "final_outcome": "dry_run_released",
            "recommended_next_step": "Rerun without --dry-run to create candidate artifacts and an active reservation.",
        },
    )
    print(f"[dry-run] run_id: {run_id}")
    print(f"[dry-run] raw requirement: {raw_requirement_path}")
    print(f"[dry-run] normalized requirement json: {normalize_result_path}")
    print(f"[dry-run] candidate execution plan: {candidate_plan_path}")
    print(f"[dry-run] candidate task pack: {candidate_task_pack_path}")
    print(f"[dry-run] candidate task id: {candidate_task_id}")
    print(f"[dry-run] reservation ref: {relative_to_root(reservation_path)}")
    print(f"[dry-run] stop-after: {stop_after}")
    raise SystemExit(0)

normalized_payload = run_codex(
    repo_root,
    normalize_prompt_path,
    NORMALIZE_SCHEMA,
    normalize_result_path,
    normalize_log_path,
    model,
    profile,
)
append_executed_command(
    run_summary_path,
    command_to_text(build_codex_command(repo_root, normalize_result_path.with_suffix(".schema.json"), normalize_result_path, model, profile)),
    "passed",
    "requirement-normalizer",
)
normalized_markdown = render_normalized_markdown(normalized_payload)
normalized_markdown_path.write_text(normalized_markdown, encoding="utf-8")

if stop_after == "normalize":
    print(f"requirements normalization completed for {run_id}")
    print(f"run_root: {run_root}")
    print(f"candidate_task_id: {candidate_task_id}")
    raise SystemExit(0)

story_lines = "\n".join(
    f"- `{item['story_id']}` | {item['phase']} | {item['story_title']}" for item in story_catalog
)
plan_prompt = (
    (repo_root / "docs" / "agent-prompts" / "plan-shaper.md").read_text(encoding="utf-8").rstrip()
    + "\n\n## Runtime Assignment\n\n"
    + f"- Candidate task id: `{candidate_task_id}`\n"
    + "- Choose exactly one story id from the provided story catalog.\n"
    + "- Do not invent a story id.\n"
    + "- Return dependencies as task ids where possible.\n"
    + "\n## Story Catalog\n\n"
    + story_lines
    + "\n\n## Normalized Requirement\n\n"
    + normalized_markdown
)
plan_prompt_path = prompts_dir / "plan-shaper.md"
plan_prompt_path.write_text(plan_prompt, encoding="utf-8")
plan_payload = run_codex(
    repo_root,
    plan_prompt_path,
    PLAN_SCHEMA,
    plan_result_path,
    plan_log_path,
    model,
    profile,
)
append_executed_command(
    run_summary_path,
    command_to_text(build_codex_command(repo_root, plan_result_path.with_suffix(".schema.json"), plan_result_path, model, profile)),
    "passed",
    "plan-shaper",
)

catalog_entry = next((item for item in story_catalog if item["story_id"] == plan_payload["story_id"]), None)
if catalog_entry is None:
    fail(f"plan-shaper returned unknown story id: {plan_payload['story_id']}")

candidate_plan_path.write_text(plan_payload["plan_markdown"].rstrip() + "\n", encoding="utf-8")

if stop_after == "plan":
    print(f"candidate execution plan completed for {run_id}")
    print(f"run_root: {run_root}")
    print(f"candidate_task_id: {candidate_task_id}")
    print(f"story_id: {catalog_entry['story_id']}")
    raise SystemExit(0)

task_prompt = (
    (repo_root / "docs" / "agent-prompts" / "task-shaper.md").read_text(encoding="utf-8").rstrip()
    + "\n\n## Runtime Assignment\n\n"
    + f"- Fixed candidate task id: `{candidate_task_id}`\n"
    + f"- Fixed story id: `{catalog_entry['story_id']}`\n"
    + f"- Fixed story title: `{catalog_entry['story_title']}`\n"
    + f"- Fixed phase: `{catalog_entry['phase']}`\n"
    + "- The task pack must be safe for formal materialization under current SQLForge governance.\n"
    + "- `depends_on` must be an array of task ids or `N/A` if no dependency is known.\n"
    + "\n## Normalized Requirement\n\n"
    + normalized_markdown
    + "\n## Candidate Execution Plan\n\n"
    + plan_payload["plan_markdown"].rstrip()
    + "\n"
)
task_prompt_path = prompts_dir / "task-shaper.md"
task_prompt_path.write_text(task_prompt, encoding="utf-8")
task_payload = run_codex(
    repo_root,
    task_prompt_path,
    TASK_SCHEMA,
    task_result_path,
    task_log_path,
    model,
    profile,
)
append_executed_command(
    run_summary_path,
    command_to_text(build_codex_command(repo_root, task_result_path.with_suffix(".schema.json"), task_result_path, model, profile)),
    "passed",
    "task-shaper",
)

candidate_pack = {
    "task_id": candidate_task_id,
    "title": task_payload["title"],
    "task_class": task_payload["task_class"],
    "priority": task_payload["priority"],
    "story_id": catalog_entry["story_id"],
    "story_title": catalog_entry["story_title"],
    "depends_on": task_payload["depends_on"],
    "scope": task_payload["scope"],
    "adr_refs": task_payload["adr_refs"],
    "rule_refs": task_payload["rule_refs"],
    "context_aliases": task_payload["context_aliases"],
    "contract": task_payload["contract"],
    "tech": task_payload["tech"],
    "layer": task_payload["layer"],
    "tests": task_payload["tests"],
    "env": task_payload["env"],
    "human_confirmation_point": task_payload["human_confirmation_point"],
    "requires_human_decision": task_payload["requires_human_decision"],
    "data_impact": task_payload["data_impact"],
    "rollback_recovery": task_payload["rollback_recovery"],
    "task_summary": task_payload["task_summary"],
    "residual_risk": task_payload["residual_risk"],
    "materialization_ready": False,
    "reservation_ref": relative_to_root(reservation_path),
}
write_json(candidate_task_pack_path, candidate_pack)
update_reservation(
    reservation_path,
    "candidate_ready",
    candidate_task_pack=relative_to_root(candidate_task_pack_path),
    candidate_execution_plan=relative_to_root(candidate_plan_path),
)
suggestions = []
if task_payload["human_confirmation_point"]:
    suggestions.append(
        build_suggestion(
            issue_key="materialization_blocked" if task_payload["requires_human_decision"] else "reservation_conflict",
            summary="Candidate task pack includes a human-confirmation boundary that must be reviewed before formal materialization."
            if task_payload["requires_human_decision"]
            else "Candidate task pack should be reviewed against the recorded human-confirmation boundary before confirm-run.",
            human_confirmation_point=task_payload["human_confirmation_point"],
        )
    )
write_json(
    run_root / "shaping-summary.json",
    {
        "run_id": run_id,
        "task_id": candidate_task_id,
        "phase": catalog_entry["phase"],
        "story_id": catalog_entry["story_id"],
        "story_title": catalog_entry["story_title"],
        "source_ref": source_ref,
        "raw_requirement_path": str(raw_requirement_path),
        "normalized_requirements_json": str(normalize_result_path),
        "candidate_execution_plan": str(candidate_plan_path),
        "candidate_task_pack": str(candidate_task_pack_path),
        "reservation_ref": relative_to_root(reservation_path),
        "suggestions": suggestions,
        "executed_commands": [
            command_to_text(build_codex_command(repo_root, normalize_result_path.with_suffix(".schema.json"), normalize_result_path, model, profile)),
            command_to_text(build_codex_command(repo_root, plan_result_path.with_suffix(".schema.json"), plan_result_path, model, profile)),
            command_to_text(build_codex_command(repo_root, task_result_path.with_suffix(".schema.json"), task_result_path, model, profile)),
        ],
    },
)
write_run_summary(
    run_summary_path,
    {
        **json.loads(run_summary_path.read_text(encoding="utf-8")),
        "execution_state": "completed",
        "final_outcome": "candidate_artifacts_ready",
        "recommended_next_step": f"Review {relative_to_root(candidate_task_pack_path)} and then run task_materialize.sh or governed_intake confirmation.",
        "suggestions": suggestions,
    },
)

print(f"requirements-to-plan completed for {run_id}")
print(f"run_root: {run_root}")
print(f"candidate_task_id: {candidate_task_id}")
print(f"candidate_task_pack: {candidate_task_pack_path}")
print(f"candidate_execution_plan: {candidate_plan_path}")
PY
