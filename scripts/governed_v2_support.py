#!/usr/bin/env python3
"""Shared helpers for governed full-cycle V2 runtime support."""

from __future__ import annotations

import json
import os
import re
import subprocess
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parent.parent
STATE_DIR = ROOT / ".codex" / "state"
TASK_RESERVATION_DIR = STATE_DIR / "task-id-reservations"
INTAKE_DIR = STATE_DIR / "intake"
CURRENT_TASK_PATH = STATE_DIR / "current-task.json"
MASTER_PLAN_PATH = ROOT / "docs" / "plans" / "master-execution-plan.md"
TASK_SPEC_PATH = ROOT / "docs" / "plans" / "task-spec-matrix.md"
TASK_GOV_PATH = ROOT / "docs" / "plans" / "task-governance-extension-matrix.md"
TASKS_PATH = ROOT / "tasks.md"
TASKS_DONE_PATH = ROOT / "tasks-done.md"
VALIDATION_LOG_PATH = ROOT / "docs" / "quality" / "validation-log.md"

RESERVATION_STALE_HOURS = 24


def now_iso() -> str:
    return datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds")


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def read_json(path: Path, default: dict[str, Any] | None = None) -> dict[str, Any]:
    if not path.exists():
        return {} if default is None else json.loads(json.dumps(default))
    return json.loads(read_text(path))


def write_json(path: Path, payload: dict[str, Any]) -> None:
    write_text(path, json.dumps(payload, ensure_ascii=True, indent=2) + "\n")


def relative_to_root(path: Path) -> str:
    try:
        return str(path.resolve().relative_to(ROOT))
    except ValueError:
        return str(path.resolve())


def resolve_repo_relative(raw: str | Path) -> Path:
    candidate = Path(raw)
    if candidate.is_absolute():
        return candidate.resolve()
    return (ROOT / candidate).resolve()


def compact(text: str, limit: int = 220) -> str:
    normalized = " ".join(text.split())
    if len(normalized) <= limit:
        return normalized
    return normalized[: limit - 3] + "..."


def command_to_text(command: Iterable[str]) -> str:
    return " ".join(str(part) for part in command)


def git_status_short_tracked() -> list[str]:
    result = subprocess.run(
        ["git", "status", "--short", "--untracked-files=no"],
        cwd=ROOT,
        capture_output=True,
        check=True,
        text=True,
    )
    return [line for line in result.stdout.splitlines() if line.strip()]


def git_diff_for_paths(paths: Iterable[str]) -> str:
    path_list = [str(path).strip() for path in paths if str(path).strip()]
    command = ["git", "diff"]
    if path_list:
        command.extend(["--", *path_list])
    result = subprocess.run(
        command,
        cwd=ROOT,
        capture_output=True,
        check=True,
        text=True,
    )
    return result.stdout


def current_task_id() -> str:
    return str(read_json(CURRENT_TASK_PATH, {}).get("task_id", ""))


def current_task_phase() -> str:
    return str(read_json(CURRENT_TASK_PATH, {}).get("phase", "idle"))


def next_task_number(prefix: str) -> int:
    pattern = re.compile(rf"{re.escape(prefix)}-(\d{{3}})\b")
    max_number = 0
    for path in [
        MASTER_PLAN_PATH,
        TASK_SPEC_PATH,
        TASK_GOV_PATH,
        TASKS_PATH,
        TASKS_DONE_PATH,
        CURRENT_TASK_PATH,
    ]:
        if not path.exists():
            continue
        for match in pattern.findall(read_text(path)):
            max_number = max(max_number, int(match))
    return max_number + 1


def task_exists_anywhere(task_id: str) -> bool:
    needle = f"`{task_id}`"
    for path in [MASTER_PLAN_PATH, TASK_SPEC_PATH, TASK_GOV_PATH, TASKS_PATH, TASKS_DONE_PATH]:
        if not path.exists():
            continue
        content = read_text(path)
        if task_id in content or needle in content:
            return True
    return False


def reservation_is_stale(payload: dict[str, Any], path: Path) -> bool:
    created_raw = str(payload.get("created_at", "")).strip()
    if created_raw:
        try:
            created_at = datetime.fromisoformat(created_raw)
        except ValueError:
            created_at = datetime.fromtimestamp(path.stat().st_mtime, tz=timezone.utc)
    else:
        created_at = datetime.fromtimestamp(path.stat().st_mtime, tz=timezone.utc)
    return created_at < datetime.now(timezone.utc) - timedelta(hours=RESERVATION_STALE_HOURS)


def reserve_task_id(prefix: str, run_id: str) -> tuple[str, Path]:
    TASK_RESERVATION_DIR.mkdir(parents=True, exist_ok=True)
    candidate = next_task_number(prefix)
    attempts = 0
    while attempts < 1000:
        task_id = f"{prefix}-{candidate:03d}"
        reservation_path = TASK_RESERVATION_DIR / f"{task_id}.json"
        payload = {
            "task_id": task_id,
            "task_prefix": prefix,
            "run_id": run_id,
            "status": "reserved",
            "created_at": now_iso(),
            "updated_at": now_iso(),
        }
        try:
            fd = os.open(reservation_path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o644)
        except FileExistsError:
            existing = read_json(reservation_path, {})
            if reservation_is_stale(existing, reservation_path) and not task_exists_anywhere(task_id):
                reservation_path.unlink(missing_ok=True)
                continue
            candidate += 1
            attempts += 1
            continue
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            handle.write(json.dumps(payload, ensure_ascii=True, indent=2) + "\n")
        return task_id, reservation_path
    raise RuntimeError(f"Failed to reserve a task id for prefix {prefix!r} after {attempts} attempts.")


def update_reservation(path: Path, status: str, **extra: Any) -> dict[str, Any]:
    payload = read_json(path)
    payload["status"] = status
    payload["updated_at"] = now_iso()
    payload.update(extra)
    write_json(path, payload)
    return payload


def list_reservation_paths() -> list[Path]:
    if not TASK_RESERVATION_DIR.exists():
        return []
    return sorted(TASK_RESERVATION_DIR.glob("*.json"))


def authority_field_hints(text: str) -> list[str]:
    lowered = text.lower()
    hints: list[str] = []
    mapping = [
        ("docs index", "authoritative docs index entrypoint"),
        ("index entrypoint", "authoritative docs index entrypoint"),
        ("coverage", "coverage definition and inclusion/exclusion rules"),
        ("newly added", "canonical source for identifying new artifacts"),
        ("operations manual", "manual classification and allowed exception rules"),
        ("exception", "approved exception or exclusion policy"),
        ("failure/output", "expected failure/output format"),
        ("output format", "expected failure/output format"),
        ("usage guide", "usage-guide location, format, and target audience"),
        ("authority", "authoritative source document or field"),
        ("permission", "required permission/authority field set"),
        ("auth", "authentication/authorization boundary field set"),
    ]
    for token, label in mapping:
        if token in lowered and label not in hints:
            hints.append(label)
    if not hints:
        hints.append("specific authority fields referenced by the human confirmation point")
    return hints


def integrity_check_suggestions(issue_key: str) -> list[str]:
    if issue_key == "closeout_tail_drift":
        return [
            "python3 scripts/task_audit.py --check --phase post-closeout",
            "python3 scripts/foreman.py compile-governance --check",
        ]
    if issue_key == "runtime_task_mismatch":
        return ["python3 scripts/foreman.py preflight --task <TASK_ID> --task-class standard --prompt '<PROMPT>'"]
    if issue_key == "reservation_conflict":
        return ["python3 scripts/governed_healthcheck.py --check", "bash scripts/requirements_to_plan.sh --prompt '<PROMPT>'"]
    if issue_key == "materialization_blocked":
        return [
            "bash scripts/task_materialize.sh --task-pack <TASK_PACK> --dry-run",
            "python3 scripts/governed_healthcheck.py --check --task-pack <TASK_PACK>",
        ]
    return ["python3 scripts/governed_healthcheck.py --check"]


def build_suggestion(
    *,
    issue_key: str,
    summary: str,
    human_confirmation_point: str = "",
    command_overrides: list[str] | None = None,
) -> dict[str, Any]:
    return {
        "issue_key": issue_key,
        "summary": summary,
        "authority_fields_to_confirm": authority_field_hints(human_confirmation_point) if human_confirmation_point else [],
        "suggested_integrity_checks": command_overrides or integrity_check_suggestions(issue_key),
    }


def write_run_summary(path: Path, payload: dict[str, Any]) -> None:
    payload.setdefault("executed_commands", [])
    payload.setdefault("suggestions", [])
    payload["updated_at"] = now_iso()
    write_json(path, payload)


def append_executed_command(path: Path, command: str, status: str, notes: str = "") -> dict[str, Any]:
    payload = read_json(path, {})
    executed = list(payload.get("executed_commands", []))
    executed.append(
        {
            "command": command,
            "status": status,
            "notes": notes,
            "recorded_at": now_iso(),
        }
    )
    payload["executed_commands"] = executed
    write_run_summary(path, payload)
    return payload
