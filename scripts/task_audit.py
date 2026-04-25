#!/usr/bin/env python3
"""Audit task ledgers against repository governance rules."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from datetime import date
from pathlib import Path
from typing import Dict, List


ROOT = Path(__file__).resolve().parent.parent
TASKS_PATH = ROOT / "tasks.md"
DONE_PATH = ROOT / "tasks-done.md"
INBOX_PATH = ROOT / "INBOX.md"
STATUS_PATTERN = re.compile(r"^- Status:\s*(.+)$", re.MULTILINE)
COMMIT_PATTERN = re.compile(r"^- Commit subject:\s*`?(.+?)`?$", re.MULTILINE)
COMPLETED_AT_PATTERN = re.compile(r"^- Completed at:\s*(.+)$", re.MULTILINE)
PROGRESS_LOG_DATE_PATTERN = re.compile(r"^\s*-\s+(\d{4}-\d{2}-\d{2}):", re.MULTILINE)
INBOX_REF_PATTERN = re.compile(r"^- INBOX ref:\s*(.+)$", re.MULTILINE)
TASK_REFS_PATTERN = re.compile(r"^- Task refs:\s*(.+)$", re.MULTILINE)
PLAN_REFS_PATTERN = re.compile(r"^- Plan refs:\s*(.+)$", re.MULTILINE)
PLAN_REF_PATTERN = re.compile(r"^- Plan ref:\s*(.+)$", re.MULTILINE)
NEEDED_DECISION_PATTERN = re.compile(r"^- Needed decision:\s*(.+)$", re.MULTILINE)
R168_EFFECTIVE_DATE = "2026-04-21"
REQUIRED_CONTEXT_CLOSEOUT_MARKERS = [
    "Context closeout:",
    "Completed scope:",
    "Validation evidence:",
    "Residual risk:",
    "Next step:",
]
PHASE_PRE_CLOSEOUT = "pre-closeout"
PHASE_POST_CLOSEOUT = "post-closeout"
ALLOWED_PHASES = {PHASE_PRE_CLOSEOUT, PHASE_POST_CLOSEOUT}
UNRESOLVED_HUMAN_MARKERS = (
    "Human decision:",
    "Review reason:",
    "Escalation:",
    "Next action:",
    "INBOX ref:",
)
UNRESOLVED_HUMAN_PHRASES = (
    "等待人类",
    "等待 review",
    "等待审批",
    "awaiting human",
    "awaiting review",
    "pending human",
    "pending review",
)
TASK_LOG_ONLY_PREFIX = "task-log-only:"
INBOX_ID_PATTERN = re.compile(r"^INBOX-[A-Z0-9-]+$")
UNRESOLVED_HUMAN_FIELD_PATTERNS = {
    marker: re.compile(rf"^\s*-\s*{re.escape(marker)}", re.MULTILINE) for marker in UNRESOLVED_HUMAN_MARKERS
}
BLOCKED_REQUIRED_FIELD_PATTERNS = {
    "Next action:": re.compile(r"^\s*-\s*Next action:\s*.+$", re.MULTILINE),
    "Escalation:": re.compile(r"^\s*-\s*Escalation:\s*.+$", re.MULTILINE),
    "Human decision:": re.compile(r"^\s*-\s*Human decision:\s*.+$", re.MULTILINE),
    "INBOX ref:": re.compile(r"^\s*-\s*INBOX ref:\s*.+$", re.MULTILINE),
}
IN_REVIEW_REQUIRED_FIELD_PATTERNS = {
    "Review reason:": re.compile(r"^\s*-\s*Review reason:\s*.+$", re.MULTILINE),
    "Human decision:": re.compile(r"^\s*-\s*Human decision:\s*.+$", re.MULTILINE),
    "INBOX ref:": re.compile(r"^\s*-\s*INBOX ref:\s*.+$", re.MULTILINE),
}


def extract_section(content: str, heading: str, next_headings: List[str]) -> str:
    start_marker = f"## {heading}"
    start = content.find(start_marker)
    if start == -1:
        return ""
    start += len(start_marker)
    end = len(content)
    for marker in next_headings:
        index = content.find(f"## {marker}", start)
        if index != -1:
            end = min(end, index)
    return content[start:end]


def extract_task_blocks(content: str) -> List[Dict[str, str]]:
    matches = list(re.finditer(r"^###\s+([A-Z0-9-]+):\s+(.+)$", content, re.MULTILINE))
    blocks: List[Dict[str, str]] = []
    for index, match in enumerate(matches):
        start = match.start()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(content)
        block = content[start:end].strip()
        blocks.append(
            {
                "task_id": match.group(1).strip(),
                "title": match.group(2).strip(),
                "body": block,
            }
        )
    return blocks


def strip_task_blocks(content: str) -> str:
    stripped_parts: List[str] = []
    cursor = 0
    matches = list(re.finditer(r"^###\s+([A-Z0-9-]+):\s+(.+)$", content, re.MULTILINE))
    section_matches = list(re.finditer(r"^##\s+.+$", content, re.MULTILINE))
    for index, match in enumerate(matches):
        start = match.start()
        end_candidates = [len(content)]
        if index + 1 < len(matches):
            end_candidates.append(matches[index + 1].start())
        for section_match in section_matches:
            if section_match.start() > start:
                end_candidates.append(section_match.start())
                break
        end = min(end_candidates)
        stripped_parts.append(content[cursor:start])
        cursor = end
    stripped_parts.append(content[cursor:])
    return "".join(stripped_parts)


def status_of(block: Dict[str, str]) -> str:
    match = STATUS_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def commit_subject_of(block: Dict[str, str]) -> str:
    match = COMMIT_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def completed_at_of(block: Dict[str, str]) -> str:
    match = COMPLETED_AT_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def latest_progress_date_of(block: Dict[str, str]) -> str:
    matches = PROGRESS_LOG_DATE_PATTERN.findall(block["body"])
    return matches[-1] if matches else ""


def inbox_ref_of(block: Dict[str, str]) -> str:
    match = INBOX_REF_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def task_refs_of(block: Dict[str, str]) -> List[str]:
    match = TASK_REFS_PATTERN.search(block["body"])
    if not match:
        return []
    return [item.strip() for item in match.group(1).split(",") if item.strip()]


def plan_refs_of(block: Dict[str, str]) -> List[str]:
    match = PLAN_REFS_PATTERN.search(block["body"])
    if not match:
        return []
    return [item.strip() for item in match.group(1).split(",") if item.strip()]


def plan_ref_of(block: Dict[str, str]) -> str:
    match = PLAN_REF_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def needed_decision_of(block: Dict[str, str]) -> str:
    match = NEEDED_DECISION_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def requires_context_closeout(block: Dict[str, str], done_ledger: bool) -> bool:
    if done_ledger:
        completed_at = completed_at_of(block)
        return bool(completed_at and completed_at >= R168_EFFECTIVE_DATE)

    status = status_of(block)
    if status != "in_review":
        return False

    latest_progress_date = latest_progress_date_of(block)
    return bool(latest_progress_date and latest_progress_date >= R168_EFFECTIVE_DATE)


def validate_context_closeout(block: Dict[str, str], errors: List[str]) -> None:
    missing = [marker for marker in REQUIRED_CONTEXT_CLOSEOUT_MARKERS if marker not in block["body"]]
    if missing:
        errors.append(
            f"{block['task_id']} is missing required Context closeout markers:\n- " + "\n- ".join(missing)
        )


def validate_done_ledger_order(done_blocks: List[Dict[str, str]], errors: List[str]) -> None:
    previous_completed_at = ""
    for block in done_blocks:
        completed_at = completed_at_of(block)
        if not completed_at:
            errors.append(f"{block['task_id']} in tasks-done.md is missing 'Completed at:'.")
            continue

        if previous_completed_at and completed_at > previous_completed_at:
            errors.append(
                f"{block['task_id']} is out of order in tasks-done.md; newer completed tasks must be prepended above older ones."
            )
            return

        previous_completed_at = completed_at


def validate_done_ledger_structure(done_content: str, errors: List[str]) -> None:
    marker = "## Done"
    start = done_content.find(marker)
    if start == -1:
        errors.append("tasks-done.md is missing the '## Done' section.")
        return

    tail = done_content[start + len(marker) :]
    unexpected_headings = re.findall(r"^##\s+(.+)$", tail, re.MULTILINE)
    if unexpected_headings:
        errors.append(
            "tasks-done.md contains unexpected section headings after '## Done':\n- "
            + "\n- ".join(unexpected_headings)
        )

    done_section = extract_section(done_content, "Done", [])
    leftover = strip_task_blocks(done_section)
    leftover_lines = [line for line in leftover.splitlines() if line.strip() and line.strip() != "_No tasks._"]
    if leftover_lines:
        preview = "\n- ".join(line.strip() for line in leftover_lines[:5])
        errors.append("tasks-done.md contains non-task stray content inside the done section:\n- " + preview)


def validate_done_plan_ref(block: Dict[str, str], errors: List[str]) -> None:
    plan_ref = plan_ref_of(block)
    if not plan_ref:
        return
    if plan_ref.startswith("docs/exec-plans/active/"):
        errors.append(
            f"{block['task_id']} in tasks-done.md has archived Plan ref pointing at active exec plans: {plan_ref}"
        )
        return
    if plan_ref.startswith("docs/exec-plans/completed/") and not (ROOT / plan_ref).exists():
        errors.append(f"{block['task_id']} in tasks-done.md has missing completed Plan ref: {plan_ref}")


def git_subjects() -> List[str]:
    result = subprocess.run(
        ["git", "log", "--format=%s"],
        cwd=ROOT,
        capture_output=True,
        check=True,
        text=True,
    )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def worktree_has_changes() -> bool:
    result = subprocess.run(
        ["git", "status", "--short"],
        cwd=ROOT,
        capture_output=True,
        check=True,
        text=True,
    )
    return bool(result.stdout.strip())


def validate_pending_commit_state(done_blocks: List[Dict[str, str]], subjects: List[str], errors: List[str]) -> List[str]:
    pending_blocks = []
    for block in done_blocks:
        subject = commit_subject_of(block)
        if subject and subject not in subjects:
            pending_blocks.append(block)

    if not pending_blocks:
        return []

    if len(pending_blocks) != 1:
        errors.append(
            "Only one archived task may be pending git history during pre-commit closeout:\n- "
            + "\n- ".join(block["task_id"] for block in pending_blocks)
        )
        return []

    pending_block = pending_blocks[0]
    if not done_blocks or pending_block["task_id"] != done_blocks[0]["task_id"]:
        errors.append(
            f"{pending_block['task_id']} is pending git history but is not the newest archived task in tasks-done.md."
        )
        return []

    completed_at = completed_at_of(pending_block)
    if completed_at != date.today().isoformat():
        errors.append(
            f"{pending_block['task_id']} is pending git history but Completed at is not today ({date.today().isoformat()})."
        )
        return []

    if not worktree_has_changes():
        errors.append(
            f"{pending_block['task_id']} is pending git history but the worktree is clean; current closeout state is inconsistent."
        )
        return []

    return [pending_block["task_id"]]


def validate_human_decision_state(block: Dict[str, str], errors: List[str]) -> None:
    body = block["body"]
    status = status_of(block)
    inbox_ref = inbox_ref_of(block)
    if status == "blocked":
        missing = [marker for marker, pattern in BLOCKED_REQUIRED_FIELD_PATTERNS.items() if not pattern.search(body)]
        if missing:
            errors.append(
                f"{block['task_id']} is blocked but missing required human-decision markers:\n- "
                + "\n- ".join(missing)
            )
            return
        validate_inbox_ref(block, inbox_ref, errors)
        return

    if status == "in_review":
        missing = [marker for marker, pattern in IN_REVIEW_REQUIRED_FIELD_PATTERNS.items() if not pattern.search(body)]
        if missing:
            errors.append(
                f"{block['task_id']} is in_review but missing required review markers:\n- "
                + "\n- ".join(missing)
            )
            return
        validate_inbox_ref(block, inbox_ref, errors)
        return

    if status in {"todo", "in_progress"}:
        unexpected_markers = [
            marker for marker, pattern in UNRESOLVED_HUMAN_FIELD_PATTERNS.items() if pattern.search(body)
        ]
        if unexpected_markers:
            errors.append(
                f"{block['task_id']} is {status} but still contains unresolved human-decision markers:\n- "
                + "\n- ".join(unexpected_markers)
            )
        lowered = body.lower()
        phrases = [phrase for phrase in UNRESOLVED_HUMAN_PHRASES if phrase in lowered or phrase in body]
        if phrases:
            errors.append(
                f"{block['task_id']} is {status} but still contains waiting-for-human phrases:\n- "
                + "\n- ".join(phrases)
            )


def validate_inbox_ref(block: Dict[str, str], inbox_ref: str, errors: List[str]) -> None:
    if not inbox_ref:
        return

    if inbox_ref.startswith(TASK_LOG_ONLY_PREFIX):
        if not inbox_ref[len(TASK_LOG_ONLY_PREFIX) :].strip():
            errors.append(
                f"{block['task_id']} uses '{TASK_LOG_ONLY_PREFIX}' but does not explain why the issue stays only in the task log."
            )
        return

    if not INBOX_ID_PATTERN.match(inbox_ref):
        errors.append(
            f"{block['task_id']} has invalid INBOX ref '{inbox_ref}'; expected 'INBOX-...' or '{TASK_LOG_ONLY_PREFIX} <reason>'."
        )
        return

    if inbox_ref not in INBOX_INDEX:
        errors.append(f"{block['task_id']} references missing INBOX item '{inbox_ref}'.")
        return

    inbox_task_refs = task_refs_of(INBOX_INDEX[inbox_ref])
    if block["task_id"] not in inbox_task_refs:
        errors.append(
            f"{block['task_id']} references '{inbox_ref}' but the INBOX item does not list the task in 'Task refs:'."
        )


INBOX_INDEX = {
    block["task_id"]: block for block in extract_task_blocks(INBOX_PATH.read_text(encoding="utf-8"))
}


def validate_inbox_blocks(
    inbox_blocks: List[Dict[str, str]],
    active_ids: List[str],
    done_ids: List[str],
    errors: List[str],
) -> None:
    inbox_ids = [block["task_id"] for block in inbox_blocks]
    duplicates = sorted(set(inbox_id for inbox_id in inbox_ids if inbox_ids.count(inbox_id) > 1))
    if duplicates:
        errors.append("Duplicate INBOX ids found:\n- " + "\n- ".join(duplicates))

    known_task_ids = set(active_ids) | set(done_ids)
    for block in inbox_blocks:
        inbox_id = block["task_id"]
        if not INBOX_ID_PATTERN.match(inbox_id):
            errors.append(f"{inbox_id} in INBOX.md does not use the required 'INBOX-*' identifier format.")

        status = status_of(block)
        if not status:
            errors.append(f"{inbox_id} in INBOX.md is missing 'Status:'.")

        task_refs = task_refs_of(block)
        plan_refs = plan_refs_of(block)
        if not task_refs and not plan_refs:
            errors.append(f"{inbox_id} in INBOX.md must contain 'Task refs:' or 'Plan refs:'.")

        for task_ref in task_refs:
            if task_ref not in known_task_ids:
                errors.append(f"{inbox_id} in INBOX.md references unknown task id '{task_ref}' in 'Task refs:'.")

        if not needed_decision_of(block):
            errors.append(f"{inbox_id} in INBOX.md is missing 'Needed decision:'.")


def audit(phase: str) -> List[str]:
    errors: List[str] = []
    tasks_content = TASKS_PATH.read_text(encoding="utf-8")
    done_content = DONE_PATH.read_text(encoding="utf-8")
    inbox_content = INBOX_PATH.read_text(encoding="utf-8")

    if "## Done" in tasks_content:
        errors.append("tasks.md must not contain a '## Done' section.")
    if "Status: done" in tasks_content:
        errors.append("tasks.md must not contain tasks with 'Status: done'.")

    todo = extract_section(tasks_content, "Todo", ["In Progress", "In Review", "Blocked"])
    in_progress = extract_section(tasks_content, "In Progress", ["In Review", "Blocked"])
    in_review = extract_section(tasks_content, "In Review", ["Blocked"])
    blocked = extract_section(tasks_content, "Blocked", [])

    active_blocks = (
        extract_task_blocks(todo)
        + extract_task_blocks(in_progress)
        + extract_task_blocks(in_review)
        + extract_task_blocks(blocked)
    )
    done_blocks = extract_task_blocks(done_content)
    inbox_blocks = extract_task_blocks(inbox_content)

    validate_done_ledger_structure(done_content, errors)
    validate_done_ledger_order(done_blocks, errors)

    active_ids = [block["task_id"] for block in active_blocks]
    done_ids = [block["task_id"] for block in done_blocks]

    duplicates = sorted(set(task_id for task_id in active_ids if active_ids.count(task_id) > 1))
    duplicates += sorted(set(task_id for task_id in done_ids if done_ids.count(task_id) > 1))
    if duplicates:
        errors.append("Duplicate task ids found:\n- " + "\n- ".join(sorted(set(duplicates))))

    overlap = sorted(set(active_ids) & set(done_ids))
    if overlap:
        errors.append("Task ids must not exist in both tasks.md and tasks-done.md:\n- " + "\n- ".join(overlap))

    validate_inbox_blocks(inbox_blocks, active_ids, done_ids, errors)

    for block in active_blocks:
        status = status_of(block)
        if status not in {"todo", "in_progress", "in_review", "blocked"}:
            errors.append(f"{block['task_id']} in tasks.md has invalid status '{status}'.")
            continue
        validate_human_decision_state(block, errors)
        if requires_context_closeout(block, done_ledger=False):
            validate_context_closeout(block, errors)

    subjects = git_subjects()
    allowed_pending_ids = []
    if phase == PHASE_PRE_CLOSEOUT:
        allowed_pending_ids = validate_pending_commit_state(done_blocks, subjects, errors)
    for block in done_blocks:
        status = status_of(block)
        if status != "done":
            errors.append(f"{block['task_id']} in tasks-done.md must have status 'done', found '{status}'.")
            continue
        subject = commit_subject_of(block)
        if not subject:
            errors.append(f"{block['task_id']} in tasks-done.md is missing 'Commit subject:'.")
            continue
        if subject not in subjects and (phase != PHASE_PRE_CLOSEOUT or block["task_id"] not in allowed_pending_ids):
            errors.append(f"{block['task_id']} commit subject not found in git history: {subject}")
        if requires_context_closeout(block, done_ledger=True):
            validate_context_closeout(block, errors)
        validate_done_plan_ref(block, errors)

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit SQLForge task ledgers.")
    parser.add_argument("--check", action="store_true", help="Run the default task audit.")
    parser.add_argument(
        "--phase",
        choices=sorted(ALLOWED_PHASES),
        default=PHASE_PRE_CLOSEOUT,
        help="Audit phase to evaluate. Defaults to pre-closeout for backward compatibility.",
    )
    args = parser.parse_args()

    if not args.check:
        parser.error("Only --check is supported.")

    errors = audit(args.phase)
    if errors:
        print("Task audit failed:\n", file=sys.stderr)
        for error in errors:
            print(error, file=sys.stderr)
            print("", file=sys.stderr)
        return 1

    print(f"Task audit passed ({args.phase}):")
    print("- tasks.md contains no done tasks")
    print("- tasks and tasks-done have no duplicate ids")
    print("- blocked and in_review tasks contain required human-decision metadata and valid INBOX refs")
    print("- todo and in_progress tasks contain no unresolved human-decision markers")
    if args.phase == PHASE_PRE_CLOSEOUT:
        print("- tasks-done commit subjects exist in git history, except at most one newest same-day closeout task pending its commit")
    else:
        print("- tasks-done commit subjects all exist in git history")
    print("- tasks-done ordering is newest-first for closeout evaluation")
    print("- R-168 Context closeout markers exist for applicable in-review/done tasks")
    print("- archived done-task Plan refs do not point at active exec plans")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
