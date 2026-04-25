#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

TASK_PACK=""
CANDIDATE_PLAN=""
REQUIREMENTS_SOURCE=""
REVIEW_MODEL=""
REVIEW_PROFILE=""
SKIP_REVIEW=false
DRY_RUN=false

usage() {
  cat <<'EOF'
Usage: ./scripts/task_materialize.sh --task-pack <FILE> [options]

Options:
  --task-pack <FILE>         Candidate task pack JSON path.
  --candidate-plan <FILE>    Candidate execution plan markdown path. Defaults to sibling candidate-execution-plan.md.
  --requirements-source <FILE>  Raw requirement markdown path. Defaults to sibling raw-requirement.md.
  --review-model <MODEL>     Optional Codex model override for governance review.
  --review-profile <PROFILE> Optional Codex profile override for governance review.
  --skip-review              Skip the task-governance-reviewer Codex step and rely on deterministic gate checks only.
  --dry-run                  Print the planned materialization without mutating repo-tracked files.
  -h, --help                 Show this help message.
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --task-pack)
      TASK_PACK="${2:-}"
      shift 2
      ;;
    --candidate-plan)
      CANDIDATE_PLAN="${2:-}"
      shift 2
      ;;
    --requirements-source)
      REQUIREMENTS_SOURCE="${2:-}"
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
    --skip-review)
      SKIP_REVIEW=true
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

if [[ -z "${TASK_PACK}" ]]; then
  usage >&2
  exit 1
fi

python3 - "${REPO_ROOT}" "${TASK_PACK}" "${CANDIDATE_PLAN}" "${REQUIREMENTS_SOURCE}" "${REVIEW_MODEL}" "${REVIEW_PROFILE}" "${SKIP_REVIEW}" "${DRY_RUN}" <<'PY'
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
from pathlib import Path

from scripts.governed_v2_support import (
    append_executed_command,
    build_suggestion,
    command_to_text,
    relative_to_root,
    update_reservation,
    write_run_summary,
)


REVIEW_SCHEMA = {
    "type": "object",
    "additionalProperties": False,
    "required": ["go_no_go", "blockers", "notes"],
    "properties": {
        "go_no_go": {"type": "string", "enum": ["go", "no-go"]},
        "blockers": {"type": "array", "items": {"type": "string"}},
        "notes": {"type": "array", "items": {"type": "string"}},
    },
}

REQUIRED_FIELDS = {
    "task_id": str,
    "title": str,
    "task_class": str,
    "priority": str,
    "story_id": str,
    "story_title": str,
    "depends_on": list,
    "scope": str,
    "adr_refs": list,
    "rule_refs": list,
    "context_aliases": list,
    "contract": str,
    "tech": list,
    "layer": list,
    "tests": list,
    "env": str,
    "human_confirmation_point": str,
    "requires_human_decision": bool,
    "data_impact": str,
    "rollback_recovery": str,
    "task_summary": str,
    "residual_risk": list,
    "materialization_ready": bool,
}


def fail(message: str) -> None:
    raise SystemExit(message)


def resolve_repo_relative(repo_root: Path, raw: str) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (repo_root / candidate).resolve()


def load_json(path: Path) -> dict:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        fail(f"Missing required file: {path}")
        raise exc
    except json.JSONDecodeError as exc:
        fail(f"Invalid json in {path}: {exc}")
        raise exc


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


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


def run(command: list[str], cwd: Path) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, text=True, capture_output=True)


def run_or_fail(command: list[str], cwd: Path) -> str:
    result = run(command, cwd)
    if result.returncode != 0:
        fail(result.stderr.strip() or result.stdout.strip() or "command failed")
    return result.stdout


def collect_story_catalog(master_plan: str) -> dict[str, dict]:
    phase = ""
    catalog: dict[str, dict] = {}
    for line in master_plan.splitlines():
        phase_match = re.match(r"^###\s+(Phase-[A-Z])(?:\s+.+)?$", line)
        if phase_match:
            phase = phase_match.group(1)
            continue
        story_match = re.match(r"^##### Story `([^`]+)`\s+(.+)$", line)
        if story_match:
            catalog[story_match.group(1)] = {
                "phase": phase,
                "story_id": story_match.group(1),
                "story_title": story_match.group(2).strip(),
            }
    return catalog


def task_exists_anywhere(repo_root: Path, task_id: str) -> bool:
    paths = [
        repo_root / "docs" / "plans" / "master-execution-plan.md",
        repo_root / "docs" / "plans" / "task-spec-matrix.md",
        repo_root / "docs" / "plans" / "task-governance-extension-matrix.md",
        repo_root / "tasks.md",
        repo_root / "tasks-done.md",
    ]
    needle = f"`{task_id}`"
    for path in paths:
        if not path.exists():
            continue
        text = path.read_text(encoding="utf-8")
        if task_id in text or needle in text:
            return True
    return False


def format_backtick_list(values: list[str]) -> str:
    cleaned = [value.strip() for value in values if value.strip()]
    if not cleaned:
        return "N/A"
    return ",".join(f"`{value}`" for value in cleaned)


def insert_after_table_header(content: str, anchor_pattern: str, row: str) -> str:
    lines = content.splitlines()
    anchor_index = None
    for index, line in enumerate(lines):
        if re.match(anchor_pattern, line):
            anchor_index = index
            break
    if anchor_index is None:
        fail(f"Failed to find anchor pattern: {anchor_pattern}")
    table_header_index = None
    for index in range(anchor_index, len(lines)):
        if lines[index].startswith("| Task ID |"):
            table_header_index = index
            break
    if table_header_index is None:
        fail(f"Failed to find task table after anchor pattern: {anchor_pattern}")
    insert_at = table_header_index + 2
    while insert_at < len(lines) and lines[insert_at].startswith("|"):
        insert_at += 1
    lines.insert(insert_at, row)
    return "\n".join(lines).rstrip() + "\n"


def insert_phase_matrix_row(content: str, phase: str, row: str) -> str:
    lines = content.splitlines()
    phase_index = None
    for index, line in enumerate(lines):
        if line.strip() == f"## {phase}":
            phase_index = index
            break
    if phase_index is None:
        fail(f"Failed to find phase section: {phase}")
    table_header_index = None
    for index in range(phase_index, len(lines)):
        if lines[index].startswith("| Task ID |"):
            table_header_index = index
            break
        if index > phase_index and lines[index].startswith("## "):
            break
    if table_header_index is None:
        fail(f"Failed to find phase task matrix table for {phase}")
    insert_at = table_header_index + 2
    while insert_at < len(lines) and lines[insert_at].startswith("|"):
        insert_at += 1
    lines.insert(insert_at, row)
    return "\n".join(lines).rstrip() + "\n"


def ensure_coverage_row(content: str, path_value: str, row: str) -> str:
    if f"| `{path_value}` |" in content:
        return content
    marker = "## Completeness Statement"
    index = content.find(marker)
    if index == -1:
        fail("document-coverage-matrix.md is missing the completeness statement section")
    return content[:index].rstrip() + row + "\n\n" + content[index:]


def review_candidate_task(
    repo_root: Path,
    task_pack: dict,
    plan_markdown: str,
    pack_path: Path,
    prompt_path: Path,
    result_path: Path,
    log_path: Path,
    model: str,
    profile: str,
) -> dict:
    write_json(result_path.with_suffix(".schema.json"), REVIEW_SCHEMA)
    prompt = (
        (repo_root / "docs" / "agent-prompts" / "task-governance-reviewer.md").read_text(encoding="utf-8").rstrip()
        + "\n\n## Runtime Assignment\n\n"
        + f"- Candidate task pack path: `{pack_path}`\n"
        + "- Only return blockers for actual governance-stop conditions.\n"
        + "- Main Foreman remains the only final closer.\n"
        + "\n## Candidate Task Pack\n\n"
        + json.dumps(task_pack, ensure_ascii=False, indent=2)
        + "\n\n## Candidate Execution Plan\n\n"
        + plan_markdown.rstrip()
        + "\n"
    )
    prompt_path.write_text(prompt, encoding="utf-8")
    command = build_codex_command(
        repo_root,
        result_path.with_suffix(".schema.json"),
        result_path,
        model,
        profile,
    )
    result = subprocess.run(command, cwd=repo_root, text=True, input=prompt, capture_output=True)
    log_path.write_text((result.stdout or "") + (result.stderr or ""), encoding="utf-8")
    if result.returncode != 0:
        fail(f"Task governance review failed; see {log_path}")
    return load_json(result_path)


repo_root = Path(sys.argv[1]).resolve()
task_pack_path = resolve_repo_relative(repo_root, sys.argv[2])
candidate_plan_raw = sys.argv[3]
requirements_source_raw = sys.argv[4]
review_model = sys.argv[5]
review_profile = sys.argv[6]
skip_review = sys.argv[7].lower() == "true"
dry_run = sys.argv[8].lower() == "true"

task_pack = load_json(task_pack_path)
for field, expected_type in REQUIRED_FIELDS.items():
    if field not in task_pack:
        fail(f"Candidate task pack is missing field: {field}")
    if not isinstance(task_pack[field], expected_type):
        fail(f"Candidate task pack field {field} has invalid type; expected {expected_type.__name__}")

task_id = task_pack["task_id"]
candidate_plan_path = resolve_repo_relative(
    repo_root,
    candidate_plan_raw or str(task_pack_path.parent / "candidate-execution-plan.md"),
)
requirements_source_path = resolve_repo_relative(
    repo_root,
    requirements_source_raw or str(task_pack_path.parent / "raw-requirement.md"),
)
if not candidate_plan_path.exists():
    fail(f"Candidate execution plan does not exist: {candidate_plan_path}")
if not requirements_source_path.exists():
    fail(f"Requirements source does not exist: {requirements_source_path}")

run_root = task_pack_path.parent
run_summary_path = run_root / "materialization-summary.json"
review_prompt_path = run_root / "prompts" / "task-governance-reviewer.md"
review_result_path = run_root / "governance-review.json"
review_log_path = run_root / "logs" / "task-governance-reviewer.log"
review_prompt_path.parent.mkdir(parents=True, exist_ok=True)
review_log_path.parent.mkdir(parents=True, exist_ok=True)

master_plan_path = repo_root / "docs" / "plans" / "master-execution-plan.md"
task_spec_path = repo_root / "docs" / "plans" / "task-spec-matrix.md"
task_gov_path = repo_root / "docs" / "plans" / "task-governance-extension-matrix.md"
coverage_path = repo_root / "docs" / "plans" / "document-coverage-matrix.md"
tasks_path = repo_root / "tasks.md"
story_catalog = collect_story_catalog(master_plan_path.read_text(encoding="utf-8"))

reservation_ref = str(task_pack.get("reservation_ref", "")).strip()
reservation_path = resolve_repo_relative(repo_root, reservation_ref) if reservation_ref else None

write_run_summary(
    run_summary_path,
    {
        "task_id": task_id,
        "path_selected": "task-materialization",
        "execution_state": "planning",
        "final_outcome": "in_progress",
        "candidate_task_pack": relative_to_root(task_pack_path),
        "candidate_execution_plan": relative_to_root(candidate_plan_path),
        "requirements_source": relative_to_root(requirements_source_path),
        "reservation_ref": reservation_ref,
        "blockers": [],
        "executed_commands": [],
        "suggestions": [],
        "recommended_next_step": "Run governance review and formal materialization.",
    },
)


def finish_and_fail(message: str, *, issue_key: str = "materialization_blocked") -> None:
    existing = load_json(run_summary_path)
    suggestions = list(existing.get("suggestions", []))
    suggestions.append(
        build_suggestion(
            issue_key=issue_key,
            summary=message,
            human_confirmation_point=task_pack.get("human_confirmation_point", ""),
        )
    )
    write_run_summary(
        run_summary_path,
        {
            **existing,
            "execution_state": "completed",
            "final_outcome": "blocked",
            "blockers": list(dict.fromkeys([*existing.get("blockers", []), message])),
            "suggestions": suggestions,
            "recommended_next_step": "Resolve the blocker, then rerun task_materialize.sh or governed_healthcheck.py.",
        },
    )
    if reservation_path is not None and reservation_path.exists():
        update_reservation(reservation_path, "blocked", blocker=message)
    fail(message)


def snapshot_file(path: Path, snapshots: dict[Path, str | None]) -> None:
    target = path.resolve()
    if target not in snapshots:
        snapshots[target] = target.read_text(encoding="utf-8") if target.exists() else None


def rollback_snapshots(snapshots: dict[Path, str | None]) -> None:
    for path, original in reversed(list(snapshots.items())):
        if original is None:
            path.unlink(missing_ok=True)
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(original, encoding="utf-8")


if task_exists_anywhere(repo_root, task_id):
    finish_and_fail(f"Task {task_id} already exists in plan, matrix, or ledgers.", issue_key="reservation_conflict")
if task_pack["story_id"] not in story_catalog:
    finish_and_fail(f"story id {task_pack['story_id']} does not exist in master-execution-plan.md")

dry_run_blockers: list[str] = []
if task_pack["requires_human_decision"]:
    blocker = (
        f"{task_id} requires human decision before materialization: "
        f"{task_pack['human_confirmation_point']}"
    )
    if dry_run:
        dry_run_blockers.append(blocker)
    else:
        finish_and_fail(blocker)

plan_markdown = candidate_plan_path.read_text(encoding="utf-8")
review_payload = {"go_no_go": "go", "blockers": [], "notes": ["Skipped reviewer; deterministic gate checks only."]}
review_command_text = ""
if not skip_review and not dry_run:
    review_command = build_codex_command(
        repo_root,
        review_result_path.with_suffix(".schema.json"),
        review_result_path,
        review_model,
        review_profile,
    )
    review_command_text = command_to_text(review_command)
    review_payload = review_candidate_task(
        repo_root,
        task_pack,
        plan_markdown,
        task_pack_path,
        review_prompt_path,
        review_result_path,
        review_log_path,
        review_model,
        review_profile,
    )
    append_executed_command(run_summary_path, review_command_text, "passed", "task-governance-reviewer")
elif skip_review:
    write_json(review_result_path, review_payload)

if review_payload["go_no_go"] != "go" or review_payload["blockers"]:
    finish_and_fail(
        f"Task governance review blocked materialization for {task_id}: "
        + "; ".join(review_payload["blockers"] or ["review returned no-go"])
    )

phase = story_catalog[task_pack["story_id"]]["phase"]
depends_on_list = [item.strip() for item in task_pack["depends_on"] if item.strip()]
depends_on_text = format_backtick_list(depends_on_list)

master_row = (
    f"| `{task_id}` | {task_pack['title']} | {task_pack['scope']} | "
    f"{depends_on_text} | {'、'.join(task_pack['tests'])} |"
)
spec_row = (
    f"| `{task_id}` | {task_pack['title']} | {format_backtick_list(task_pack['adr_refs'])} | "
    f"{format_backtick_list(task_pack['rule_refs'])} | {format_backtick_list(task_pack['context_aliases'])} | "
    f"{task_pack['contract']} | {format_backtick_list(task_pack['tech'])} | {format_backtick_list(task_pack['layer'])} | "
    f"{'、'.join(task_pack['tests'])} | {depends_on_text} | {task_pack['env']} |"
)
gov_row = (
    f"| `{task_id}` | {task_pack['human_confirmation_point']} | "
    f"{task_pack['data_impact']} | {task_pack['rollback_recovery']} |"
)

plan_ref = f"docs/exec-plans/active/{task_id}-full-auto-execution-plan.md"
requirements_archive_ref = f"docs/references/raw-requirements/generated/{task_id}-requirement.md"
suggestions = []
if task_pack["human_confirmation_point"]:
    suggestions.append(
        build_suggestion(
            issue_key="materialization_blocked" if task_pack["requires_human_decision"] else "reservation_conflict",
            summary="Review the recorded human-confirmation boundary before allowing formal materialization.",
            human_confirmation_point=task_pack["human_confirmation_point"],
        )
    )

if dry_run:
    write_run_summary(
        run_summary_path,
        {
            **load_json(run_summary_path),
            "execution_state": "completed",
            "final_outcome": "dry_run_ready",
            "phase": phase,
            "story_id": task_pack["story_id"],
            "formal_plan_ref": plan_ref,
            "requirements_archive_ref": requirements_archive_ref,
            "blockers": dry_run_blockers,
            "suggestions": suggestions,
            "recommended_next_step": "If the preview looks correct, rerun without --dry-run or use governed_intake.sh --confirm-run.",
        },
    )
    print(f"[dry-run] task_id: {task_id}")
    print(f"[dry-run] story_id: {task_pack['story_id']}")
    print(f"[dry-run] phase: {phase}")
    if dry_run_blockers:
        for blocker in dry_run_blockers:
            print(f"[dry-run] blocker: {blocker}")
    print(f"[dry-run] would preflight candidate task: python3 scripts/foreman.py preflight --task {task_id} --task-class {task_pack['task_class']} --prompt \"{task_pack['task_summary']}\"")
    print(f"[dry-run] would write exec plan: {plan_ref}")
    print(f"[dry-run] would archive raw requirement: {requirements_archive_ref}")
    print(f"[dry-run] would insert master plan row: {master_row}")
    print(f"[dry-run] would insert task-spec row: {spec_row}")
    print(f"[dry-run] would insert task-governance row: {gov_row}")
    print(f"[dry-run] would instantiate: python3 scripts/foreman.py instantiate {task_id}")
    raise SystemExit(0)

summary_prompt = task_pack["task_summary"] or task_pack["scope"]
preflight_command = [
    "python3",
    "scripts/foreman.py",
    "preflight",
    "--task",
    task_id,
    "--task-class",
    task_pack["task_class"],
    "--prompt",
    summary_prompt,
]
instantiate_command = ["python3", "scripts/foreman.py", "instantiate", task_id]
tracked_snapshots: dict[Path, str | None] = {}

try:
    if reservation_path is not None and reservation_path.exists():
        update_reservation(reservation_path, "materializing", task_id=task_id)

    run_or_fail(preflight_command, repo_root)
    append_executed_command(run_summary_path, command_to_text(preflight_command), "passed", "candidate-preflight")

    snapshot_file(master_plan_path, tracked_snapshots)
    master_plan_content = master_plan_path.read_text(encoding="utf-8")
    master_plan_content = insert_after_table_header(
        master_plan_content,
        rf"^##### Story `{re.escape(task_pack['story_id'])}`\s+.+$",
        master_row,
    )
    master_plan_path.write_text(master_plan_content, encoding="utf-8")

    snapshot_file(task_spec_path, tracked_snapshots)
    task_spec_content = task_spec_path.read_text(encoding="utf-8")
    task_spec_content = insert_phase_matrix_row(task_spec_content, phase, spec_row)
    task_spec_path.write_text(task_spec_content, encoding="utf-8")

    snapshot_file(task_gov_path, tracked_snapshots)
    task_gov_content = task_gov_path.read_text(encoding="utf-8")
    task_gov_content = insert_phase_matrix_row(task_gov_content, phase, gov_row)
    task_gov_path.write_text(task_gov_content, encoding="utf-8")

    requirements_archive_path = repo_root / requirements_archive_ref
    snapshot_file(requirements_archive_path, tracked_snapshots)
    requirements_archive_path.parent.mkdir(parents=True, exist_ok=True)
    requirements_archive_path.write_text(requirements_source_path.read_text(encoding="utf-8"), encoding="utf-8")

    final_plan_path = repo_root / plan_ref
    snapshot_file(final_plan_path, tracked_snapshots)
    final_plan_path.parent.mkdir(parents=True, exist_ok=True)
    final_plan_path.write_text(plan_markdown.rstrip() + "\n", encoding="utf-8")

    snapshot_file(coverage_path, tracked_snapshots)
    coverage_content = coverage_path.read_text(encoding="utf-8")
    coverage_content = ensure_coverage_row(
        coverage_content,
        plan_ref,
        f"| `{plan_ref}` | Indexed | {task_id} formalized full-auto execution plan | Consumed | 约束 {task_id} 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope |",
    )
    coverage_content = ensure_coverage_row(
        coverage_content,
        requirements_archive_ref,
        f"| `{requirements_archive_ref}` | Archive | {task_id} raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |",
    )
    coverage_path.write_text(coverage_content, encoding="utf-8")

    task_pack["materialization_ready"] = True
    task_pack["formal_plan_ref"] = plan_ref
    task_pack["requirements_archive_ref"] = requirements_archive_ref
    write_json(task_pack_path, task_pack)

    run_or_fail(preflight_command, repo_root)
    append_executed_command(run_summary_path, command_to_text(preflight_command), "passed", "formal-preflight")

    snapshot_file(tasks_path, tracked_snapshots)
    run_or_fail(instantiate_command, repo_root)
    append_executed_command(run_summary_path, command_to_text(instantiate_command), "passed", "instantiate")
except BaseException as exc:
    rollback_snapshots(tracked_snapshots)
    message = str(exc).strip() or "task materialization failed"
    write_run_summary(
        run_summary_path,
        {
            **load_json(run_summary_path),
            "execution_state": "completed",
            "final_outcome": "failed",
            "phase": phase,
            "story_id": task_pack["story_id"],
            "formal_plan_ref": plan_ref,
            "requirements_archive_ref": requirements_archive_ref,
            "blockers": [message],
            "suggestions": suggestions
            + [
                build_suggestion(
                    issue_key="materialization_blocked",
                    summary=message,
                    human_confirmation_point=task_pack.get("human_confirmation_point", ""),
                )
            ],
            "recommended_next_step": "Inspect the blocker, run governed_healthcheck.py if needed, then retry materialization.",
        },
    )
    if reservation_path is not None and reservation_path.exists():
        update_reservation(reservation_path, "failed", failure=message)
    raise

if reservation_path is not None and reservation_path.exists():
    update_reservation(
        reservation_path,
        "materialized",
        task_id=task_id,
        formal_plan_ref=plan_ref,
        requirements_archive_ref=requirements_archive_ref,
    )

write_run_summary(
    run_summary_path,
    {
        **load_json(run_summary_path),
        "execution_state": "completed",
        "final_outcome": "materialized",
        "phase": phase,
        "story_id": task_pack["story_id"],
        "formal_plan_ref": plan_ref,
        "requirements_archive_ref": requirements_archive_ref,
        "review_result": review_payload,
        "suggestions": suggestions,
        "recommended_next_step": f"Run bash scripts/multi_agent_full_auto.sh --task {task_id} or continue via governed_intake.sh --confirm-run.",
    },
)

print(f"task materialization completed for {task_id}")
print(f"formal_plan_ref: {plan_ref}")
print(f"requirements_archive_ref: {requirements_archive_ref}")
PY
