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
    CLOSEOUT_DIR,
    CURRENT_TASK_PATH,
    INTAKE_DIR,
    TASKS_DONE_PATH,
    TASKS_PATH,
    TASK_SHAPING_DIR,
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
    release_reservation,
    reservation_is_stale,
    runtime_dashboard,
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


def done_task_ids(limit: int = 5) -> list[str]:
    ids: list[str] = []
    for line in read_text(TASKS_DONE_PATH).splitlines():
        if line.startswith("### "):
            task_id = line.split(":", 1)[0].replace("###", "").strip()
            ids.append(task_id)
        if len(ids) >= limit:
            break
    return ids


def closeout_actual_evidence_status(task_ids: list[str]) -> list[dict]:
    statuses: list[dict] = []
    for task_id in task_ids:
        evidence_path = CLOSEOUT_DIR / task_id / "post-closeout-actual.json"
        if not evidence_path.exists():
            statuses.append(
                {
                    "task_id": task_id,
                    "path": relative_to_root(evidence_path),
                    "exists": False,
                    "final_status": "missing",
                }
            )
            continue
        payload = read_json(evidence_path, {})
        statuses.append(
            {
                "task_id": task_id,
                "path": relative_to_root(evidence_path),
                "exists": True,
                "final_status": payload.get("final_status", "unknown"),
                "commit_sha": payload.get("commit_sha", ""),
            }
        )
    return statuses


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


def issue_payload_with_authority(
    issue_key: str,
    summary: str,
    human_confirmation_point: str = "",
    authority_fields_to_confirm: list[str] | None = None,
) -> dict:
    return {
        "issue_key": issue_key,
        "summary": summary,
        "suggestion": build_suggestion(
            issue_key=issue_key,
            summary=summary,
            human_confirmation_point=human_confirmation_point,
            authority_fields_to_confirm=authority_fields_to_confirm,
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Run governed full-cycle health checks.")
    parser.add_argument("--check", action="store_true", help="Run the health-check suite.")
    parser.add_argument("--task-pack", help="Optional candidate task pack to validate.")
    parser.add_argument("--run-id", default="", help="Stable run id for summary output.")
    parser.add_argument("--cleanup-stale", action="store_true", help="Release stale runtime reservations that are safe to abandon.")
    parser.add_argument("--cleanup-dry-run", action="store_true", help="Preview stale runtime cleanup without writing state.")
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
    cleanup_preview: list[dict] = []

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

    if tracked_status:
        issues.append(
            issue_payload(
                "tracked_dirty_worktree",
                "tracked worktree contains uncommitted changes; governed automation must not continue until tracked residue is resolved: "
                + "; ".join(tracked_status[:8]),
            )
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
            cleanup_item = {
                "kind": "reservation",
                "path": relative_to_root(reservation_path),
                "task_id": task_id,
                "current_status": payload.get("status", ""),
                "action": "release",
            }
            cleanup_preview.append(cleanup_item)
            if args.cleanup_dry_run:
                continue
            if args.cleanup_stale:
                release_reservation(
                    reservation_path,
                    "governed healthcheck stale cleanup",
                    task_id=task_id,
                    previous_status=payload.get("status", ""),
                )
                append_executed_command(
                    summary_path,
                    f"release stale reservation {relative_to_root(reservation_path)}",
                    "passed",
                    "cleanup-stale",
                )
                continue
            issues.append(
                issue_payload(
                    "reservation_conflict",
                    f"stale reservation remains for {task_id}: {relative_to_root(reservation_path)}",
                )
            )
            continue
        if task_exists_anywhere(task_id) and payload.get("status") not in {"materialized", "released", "abandoned"}:
            issues.append(
                issue_payload(
                    "reservation_conflict",
                    f"reservation {relative_to_root(reservation_path)} still exists even though {task_id} is already present in the repo truth.",
                )
            )

    if args.cleanup_dry_run or args.cleanup_stale:
        known_reservation_run_ids = {
            str(read_json(path, {}).get("run_id", "")).strip()
            for path in list_reservation_paths()
            if str(read_json(path, {}).get("run_id", "")).strip()
        }
        for runtime_dir, kind in [(INTAKE_DIR, "intake"), (TASK_SHAPING_DIR, "task-shaping")]:
            if not runtime_dir.exists():
                continue
            for child in sorted(item for item in runtime_dir.iterdir() if item.is_dir()):
                if child.name in known_reservation_run_ids:
                    continue
                cleanup_preview.append(
                    {
                        "kind": kind,
                        "path": relative_to_root(child),
                        "run_id": child.name,
                        "action": "review-or-remove-unreferenced-runtime-evidence",
                    }
                )

    closeout_actual_evidence = closeout_actual_evidence_status(done_task_ids())
    for evidence in closeout_actual_evidence:
        task_id = str(evidence.get("task_id", ""))
        if task_id >= "HARN-032" and evidence.get("final_status") != "passed":
            issues.append(
                issue_payload(
                    "closeout_actual_evidence_failed",
                    f"post-closeout actual evidence for {task_id} is {evidence.get('final_status')}: {evidence.get('path')}",
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
                issue_payload_with_authority(
                    "materialization_blocked",
                    f"{payload.get('task_id', 'candidate task')} still requires human confirmation before materialization.",
                    str(payload.get("human_confirmation_point", "")),
                    list(payload.get("authority_fields_to_confirm", [])),
                )
            )

    final_outcome = "issues_found" if issues else "cleanup_preview_found" if cleanup_preview else "healthy"
    recommended_next_step = (
        "Apply the suggested integrity checks, then rerun the governed command."
        if issues
        else "Review cleanup_preview before running cleanup/archive commands."
        if cleanup_preview
        else "No blocking governance/runtime issue detected."
    )

    summary = {
        **read_json(summary_path),
        "execution_state": "completed",
        "final_outcome": final_outcome,
        "issues": issues,
        "blockers": [item["summary"] for item in issues],
        "suggestions": [item["suggestion"] for item in issues],
        "tracked_status": tracked_status,
        "cleanup_preview": cleanup_preview,
        "closeout_actual_evidence": closeout_actual_evidence,
        "runtime_dashboard": runtime_dashboard(),
        "current_task_state_ref": relative_to_root(CURRENT_TASK_PATH),
        "recommended_next_step": recommended_next_step,
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

    if cleanup_preview:
        print("Governed healthcheck found cleanup preview items.")
    else:
        print("Governed healthcheck passed.")
    print(f"summary: {relative_to_root(summary_path)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
