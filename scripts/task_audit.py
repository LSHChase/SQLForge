#!/usr/bin/env python3
"""Audit task ledgers against repository governance rules."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path
from typing import Dict, List


ROOT = Path(__file__).resolve().parent.parent
TASKS_PATH = ROOT / "tasks.md"
DONE_PATH = ROOT / "tasks-done.md"
STATUS_PATTERN = re.compile(r"^- Status:\s*(.+)$", re.MULTILINE)
COMMIT_PATTERN = re.compile(r"^- Commit subject:\s*`?(.+?)`?$", re.MULTILINE)


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


def status_of(block: Dict[str, str]) -> str:
    match = STATUS_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def commit_subject_of(block: Dict[str, str]) -> str:
    match = COMMIT_PATTERN.search(block["body"])
    return match.group(1).strip() if match else ""


def git_subjects() -> List[str]:
    result = subprocess.run(
        ["git", "log", "--format=%s"],
        cwd=ROOT,
        capture_output=True,
        check=True,
        text=True,
    )
    return [line.strip() for line in result.stdout.splitlines() if line.strip()]


def audit() -> List[str]:
    errors: List[str] = []
    tasks_content = TASKS_PATH.read_text(encoding="utf-8")
    done_content = DONE_PATH.read_text(encoding="utf-8")

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

    active_ids = [block["task_id"] for block in active_blocks]
    done_ids = [block["task_id"] for block in done_blocks]

    duplicates = sorted(set(task_id for task_id in active_ids if active_ids.count(task_id) > 1))
    duplicates += sorted(set(task_id for task_id in done_ids if done_ids.count(task_id) > 1))
    if duplicates:
        errors.append("Duplicate task ids found:\n- " + "\n- ".join(sorted(set(duplicates))))

    overlap = sorted(set(active_ids) & set(done_ids))
    if overlap:
        errors.append("Task ids must not exist in both tasks.md and tasks-done.md:\n- " + "\n- ".join(overlap))

    for block in active_blocks:
        status = status_of(block)
        if status not in {"todo", "in_progress", "in_review", "blocked"}:
            errors.append(f"{block['task_id']} in tasks.md has invalid status '{status}'.")

    for block in extract_task_blocks(blocked):
        body = block["body"]
        if "Next action:" not in body or "Escalation:" not in body:
            errors.append(f"{block['task_id']} is blocked but missing 'Next action:' or 'Escalation:'.")

    subjects = git_subjects()
    for block in done_blocks:
        status = status_of(block)
        if status != "done":
            errors.append(f"{block['task_id']} in tasks-done.md must have status 'done', found '{status}'.")
            continue
        subject = commit_subject_of(block)
        if not subject:
            errors.append(f"{block['task_id']} in tasks-done.md is missing 'Commit subject:'.")
            continue
        if subject not in subjects:
            errors.append(f"{block['task_id']} commit subject not found in git history: {subject}")

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit SQLForge task ledgers.")
    parser.add_argument("--check", action="store_true", help="Run the default task audit.")
    args = parser.parse_args()

    if not args.check:
        parser.error("Only --check is supported.")

    errors = audit()
    if errors:
        print("Task audit failed:\n", file=sys.stderr)
        for error in errors:
            print(error, file=sys.stderr)
            print("", file=sys.stderr)
        return 1

    print("Task audit passed:")
    print("- tasks.md contains no done tasks")
    print("- tasks and tasks-done have no duplicate ids")
    print("- blocked tasks contain escalation metadata")
    print("- tasks-done commit subjects exist in git history")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
