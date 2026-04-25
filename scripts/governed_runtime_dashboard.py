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
    list_reservation_paths,
    read_json,
    relative_to_root,
    release_reservation,
    reservation_is_stale,
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
        if task_id and reservation_is_stale(payload, path) and not task_exists_anywhere(task_id):
            items.append(
                {
                    "kind": "reservation",
                    "path": relative_to_root(path),
                    "task_id": task_id,
                    "current_status": payload.get("status", ""),
                    "action": "release",
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
        path = ROOT / str(item["path"])
        if path.exists():
            release_reservation(path, "governed runtime dashboard stale cleanup", previous_status=item.get("current_status", ""))
            applied.append({**item, "applied": True})
    return applied


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
    args = parser.parse_args()

    reservation_preview = reservation_cleanup_preview()
    runtime_preview = runtime_evidence_preview()
    applied: list[dict[str, Any]] = []
    if args.release_stale_reservations:
        applied.extend(apply_release_stale_reservations(reservation_preview))
    if args.archive_reviewed_runtime_evidence:
        applied.extend(archive_runtime_evidence(runtime_preview))

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
