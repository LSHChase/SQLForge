#!/usr/bin/env python3
"""Summarize and safely clean SQLForge governed runtime state."""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from scripts.governed_v2_support import (
    CLOSEOUT_DIR,
    INTAKE_DIR,
    MULTI_AGENT_DIR,
    TASK_SHAPING_DIR,
    abandon_reservation,
    archive_reservation,
    find_reservation_path,
    list_reservation_paths,
    pause_reservation,
    read_json,
    relative_to_root,
    release_reservation,
    reservation_is_active,
    reservation_is_stale,
    reservation_may_resume,
    reservation_requires_reshaping,
    reservation_status,
    resume_reservation,
    runtime_dashboard,
    task_exists_anywhere,
    write_json,
)

ARCHIVE_DIR = ROOT / ".codex" / "state" / "archive"


def reservation_cleanup_preview() -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    for path in list_reservation_paths():
        payload = read_json(path, {})
        task_id = str(payload.get("task_id", "")).strip()
        status = reservation_status(payload)
        if task_id and reservation_is_stale(payload, path) and not task_exists_anywhere(task_id) and reservation_is_active(payload):
            items.append(
                {
                    "kind": "reservation",
                    "path": relative_to_root(path),
                    "task_id": task_id,
                    "current_status": status,
                    "action": "release",
                }
            )
        elif task_id and status == "released" and reservation_requires_reshaping(payload):
            items.append(
                {
                    "kind": "reservation",
                    "path": relative_to_root(path),
                    "task_id": task_id,
                    "current_status": status,
                    "action": "archive",
                }
            )
    return items


def runtime_evidence_preview() -> list[dict[str, Any]]:
    known_run_ids = {
        str(read_json(path, {}).get("run_id", "")).strip()
        for path in list_reservation_paths()
        if str(read_json(path, {}).get("run_id", "")).strip()
    }
    items: list[dict[str, Any]] = []
    for runtime_dir, kind in [
        (INTAKE_DIR, "intake"),
        (TASK_SHAPING_DIR, "task-shaping"),
        (MULTI_AGENT_DIR, "multi-agent"),
        (CLOSEOUT_DIR, "closeout"),
    ]:
        if not runtime_dir.exists():
            continue
        for child in sorted(item for item in runtime_dir.iterdir() if item.is_dir()):
            if kind in {"intake", "task-shaping"} and child.name in known_run_ids:
                continue
            items.append(
                {
                    "kind": kind,
                    "path": relative_to_root(child),
                    "run_id": child.name,
                    "action": "archive-reviewed-runtime-evidence",
                }
            )
    return items


def apply_release_stale_reservations(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    applied: list[dict[str, Any]] = []
    for item in items:
        if item.get("action") != "release":
            continue
        path = ROOT / str(item["path"])
        if path.exists():
            release_reservation(path, "governed runtime dashboard stale cleanup", previous_status=item.get("current_status", ""))
            applied.append({**item, "applied": True})
    return applied


def mutate_candidate_status(task_id: str, action: str, reason: str) -> dict[str, Any]:
    reservation_path = find_reservation_path(task_id)
    if reservation_path is None:
        raise SystemExit(f"Reservation not found for {task_id}")
    payload = read_json(reservation_path, {})
    current_status = reservation_status(payload)
    if current_status == "materialized":
        raise SystemExit(f"{task_id} is already materialized; candidate lifecycle actions no longer apply.")
    if action == "pause":
        if current_status not in {"reserved", "candidate_ready", "blocked", "failed"}:
            raise SystemExit(f"{task_id} cannot be paused from status {current_status}.")
        updated = pause_reservation(reservation_path, reason, previous_status=current_status)
    elif action == "archive":
        if current_status == "materializing":
            raise SystemExit(f"{task_id} cannot be archived while materializing.")
        updated = archive_reservation(reservation_path, reason, previous_status=current_status)
    elif action == "abandon":
        if current_status == "materializing":
            raise SystemExit(f"{task_id} cannot be abandoned while materializing.")
        updated = abandon_reservation(reservation_path, reason, previous_status=current_status)
    elif action == "resume":
        if not reservation_may_resume(payload):
            raise SystemExit(f"{task_id} cannot be resumed from status {current_status}; reshape a new candidate instead.")
        if task_exists_anywhere(task_id):
            raise SystemExit(f"{task_id} already exists in repo truth; resume is not valid.")
        updated = resume_reservation(reservation_path, reason, previous_status=current_status)
    else:
        raise SystemExit(f"Unsupported candidate action: {action}")
    return {
        "kind": "reservation",
        "task_id": task_id,
        "path": relative_to_root(reservation_path),
        "previous_status": current_status,
        "current_status": reservation_status(updated),
        "reason": reason,
        "action": action,
        "applied": True,
    }


def archive_runtime_evidence(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    applied: list[dict[str, Any]] = []
    for item in items:
        source = ROOT / str(item["path"])
        if not source.exists():
            continue
        target = ARCHIVE_DIR / str(item["path"])
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            shutil.rmtree(target)
        shutil.move(str(source), str(target))
        applied.append({**item, "applied": True, "archive_path": relative_to_root(target)})
    return applied


def main() -> int:
    parser = argparse.ArgumentParser(description="Show governed runtime dashboard and optional cleanup actions.")
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON only.")
    parser.add_argument("--release-stale-reservations", action="store_true", help="Release stale reservations that do not map to repo truth.")
    parser.add_argument("--archive-reviewed-runtime-evidence", action="store_true", help="Archive unreferenced runtime evidence directories under .codex/state/archive/.")
    parser.add_argument("--pause-candidate", default="", help="Pause a candidate reservation by task id.")
    parser.add_argument("--archive-candidate", default="", help="Archive a candidate reservation by task id.")
    parser.add_argument("--abandon-candidate", default="", help="Abandon a candidate reservation by task id.")
    parser.add_argument("--resume-candidate", default="", help="Resume a paused candidate reservation by task id.")
    parser.add_argument("--reason", default="", help="Audit reason for candidate lifecycle mutations.")
    args = parser.parse_args()

    requested_actions = [
        bool(args.pause_candidate.strip()),
        bool(args.archive_candidate.strip()),
        bool(args.abandon_candidate.strip()),
        bool(args.resume_candidate.strip()),
    ]
    if sum(requested_actions) > 1:
        parser.error("Use only one of --pause-candidate/--archive-candidate/--abandon-candidate/--resume-candidate per invocation.")
    if any(requested_actions) and not args.reason.strip():
        parser.error("--reason is required for candidate lifecycle mutations.")

    reservation_preview = reservation_cleanup_preview()
    runtime_preview = runtime_evidence_preview()
    applied: list[dict[str, Any]] = []
    if args.release_stale_reservations:
        applied.extend(apply_release_stale_reservations(reservation_preview))
    if args.archive_reviewed_runtime_evidence:
        applied.extend(archive_runtime_evidence(runtime_preview))
    if args.pause_candidate.strip():
        applied.append(mutate_candidate_status(args.pause_candidate.strip(), "pause", args.reason.strip()))
    if args.archive_candidate.strip():
        applied.append(mutate_candidate_status(args.archive_candidate.strip(), "archive", args.reason.strip()))
    if args.abandon_candidate.strip():
        applied.append(mutate_candidate_status(args.abandon_candidate.strip(), "abandon", args.reason.strip()))
    if args.resume_candidate.strip():
        applied.append(mutate_candidate_status(args.resume_candidate.strip(), "resume", args.reason.strip()))

    payload = {
        "runtime_dashboard": runtime_dashboard(),
        "reservation_cleanup_preview": reservation_preview,
        "runtime_evidence_preview": runtime_preview,
        "applied_actions": applied,
        "recommended_next_step": "Review previews before using cleanup flags; runtime evidence remains untracked by design.",
    }
    if args.json:
        print(json.dumps(payload, ensure_ascii=True, indent=2))
        return 0

    print("Governed runtime dashboard")
    print(json.dumps(payload, ensure_ascii=True, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
