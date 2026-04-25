#!/usr/bin/env python3
"""Governed intake/materialization health checks with machine-readable summaries."""

from __future__ import annotations

import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from scripts.governed_v2_support import (
    CURRENT_TASK_PATH,
    TASKS_DONE_PATH,
    TASKS_PATH,
    VALIDATION_LOG_PATH,
    append_executed_command,
    build_suggestion,
    current_task_id,
    current_task_phase,
    git_diff_for_paths,
    git_status_short_tracked,
    list_reservation_paths,
    now_iso,
    read_json,
    read_text,
    relative_to_root,
    reservation_is_stale,
    task_exists_anywhere,
    write_run_summary,
)

REQUIRED_TASK_PACK_FIELDS = {
    "task_id",
    "title",
    "task_class",
    "story_id",
    "scope",
    "human_confirmation_point",
    "requires_human_decision",
    "materialization_ready",
}


def default_run_id() -> str:
    return "healthcheck-" + datetime.now(timezone.utc).astimezone().strftime("%Y%m%d%H%M%S")


def issue_payload(issue_key: str, summary: str, human_confirmation_point: str = "") -> dict:
    return {
        "issue_key": issue_key,
        "summary": summary,
        "suggestion": build_suggestion(
            issue_key=issue_key,
            summary=summary,
            human_confirmation_point=human_confirmation_point,
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Run governed full-cycle health checks.")
    parser.add_argument("--check", action="store_true", help="Run the health-check suite.")
    parser.add_argument("--task-pack", help="Optional candidate task pack to validate.")
    parser.add_argument("--run-id", default="", help="Stable run id for summary output.")
    parser.add_argument("--json", action="store_true", help="Print the machine-readable summary path only.")
    args = parser.parse_args()

    if not args.check:
        parser.error("Only --check is supported.")

    run_id = args.run_id.strip() or default_run_id()
    run_root = ROOT / ".codex" / "state" / "intake" / run_id
    run_root.mkdir(parents=True, exist_ok=True)
    summary_path = run_root / "healthcheck-summary.json"

    write_run_summary(
        summary_path,
        {
            "run_id": run_id,
            "path_selected": "healthcheck",
            "execution_state": "running",
            "final_outcome": "in_progress",
            "issues": [],
            "blockers": [],
            "executed_commands": [],
            "suggestions": [],
            "recommended_next_step": "Review healthcheck output before retrying governed automation.",
        },
    )

    issues: list[dict] = []

    tracked_status = git_status_short_tracked()
    append_executed_command(
        summary_path,
        "git status --short --untracked-files=no",
        "passed",
        "tracked worktree check",
    )
    validation_diff = git_diff_for_paths([relative_to_root(VALIDATION_LOG_PATH)])
    append_executed_command(
        summary_path,
        f"git diff -- {relative_to_root(VALIDATION_LOG_PATH)}",
        "passed",
        "validation-log drift check",
    )

    if validation_diff and ("closeout commit" in validation_diff or "post-closeout task-audit" in validation_diff):
        issues.append(
            issue_payload(
                "closeout_tail_drift",
                "validation-log contains uncommitted closeout/post-closeout entries; a previous closeout likely wrote tracked residue after commit.",
            )
        )

    active_task_id = current_task_id()
    active_phase = current_task_phase()
    tasks_content = read_text(TASKS_PATH)
    done_content = read_text(TASKS_DONE_PATH)
    if active_task_id and active_phase not in {"idle", "closed"} and active_task_id not in tasks_content and active_task_id not in done_content:
        issues.append(
            issue_payload(
                "runtime_task_mismatch",
                f"current-task runtime state references {active_task_id} in phase {active_phase}, but the task is absent from tasks.md/tasks-done.md.",
            )
        )

    for reservation_path in list_reservation_paths():
        payload = read_json(reservation_path, {})
        task_id = str(payload.get("task_id", "")).strip()
        if not task_id:
            continue
        if reservation_is_stale(payload, reservation_path) and not task_exists_anywhere(task_id):
            issues.append(
                issue_payload(
                    "reservation_conflict",
                    f"stale reservation remains for {task_id}: {relative_to_root(reservation_path)}",
                )
            )
            continue
        if task_exists_anywhere(task_id) and payload.get("status") not in {"materialized", "released"}:
            issues.append(
                issue_payload(
                    "reservation_conflict",
                    f"reservation {relative_to_root(reservation_path)} still exists even though {task_id} is already present in the repo truth.",
                )
            )

    if args.task_pack:
        task_pack_path = Path(args.task_pack)
        if not task_pack_path.is_absolute():
            task_pack_path = (ROOT / task_pack_path).resolve()
        payload = read_json(task_pack_path, {})
        missing = sorted(REQUIRED_TASK_PACK_FIELDS - set(payload.keys()))
        if missing:
            issues.append(
                issue_payload(
                    "materialization_blocked",
                    f"candidate task pack is missing required fields: {', '.join(missing)}",
                )
            )
        elif payload.get("requires_human_decision"):
            issues.append(
                issue_payload(
                    "materialization_blocked",
                    f"{payload.get('task_id', 'candidate task')} still requires human confirmation before materialization.",
                    str(payload.get("human_confirmation_point", "")),
                )
            )

    summary = {
        **read_json(summary_path),
        "execution_state": "completed",
        "final_outcome": "issues_found" if issues else "healthy",
        "issues": issues,
        "blockers": [item["summary"] for item in issues],
        "suggestions": [item["suggestion"] for item in issues],
        "tracked_status": tracked_status,
        "current_task_state_ref": relative_to_root(CURRENT_TASK_PATH),
        "recommended_next_step": "Apply the suggested integrity checks, then rerun the governed command."
        if issues
        else "No blocking governance/runtime issue detected.",
        "updated_at": now_iso(),
    }
    write_run_summary(summary_path, summary)

    if args.json:
        print(relative_to_root(summary_path))
        return 0 if not issues else 1

    if issues:
        print("Governed healthcheck found issues:")
        for item in issues:
            print(f"- {item['issue_key']}: {item['summary']}")
            suggestion = item["suggestion"]
            if suggestion["authority_fields_to_confirm"]:
                print("  authority_fields_to_confirm: " + ", ".join(suggestion["authority_fields_to_confirm"]))
            if suggestion["suggested_integrity_checks"]:
                print("  suggested_integrity_checks: " + ", ".join(suggestion["suggested_integrity_checks"]))
        print(f"summary: {relative_to_root(summary_path)}")
        return 1

    print("Governed healthcheck passed.")
    print(f"summary: {relative_to_root(summary_path)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
