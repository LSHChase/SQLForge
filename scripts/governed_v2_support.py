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

try:
    from scripts.beijing_time import now_beijing, now_beijing_iso
except ModuleNotFoundError:
    from beijing_time import now_beijing, now_beijing_iso

ROOT = Path(__file__).resolve().parent.parent
STATE_DIR = ROOT / ".codex" / "state"
TASK_RESERVATION_DIR = STATE_DIR / "task-id-reservations"
INTAKE_DIR = STATE_DIR / "intake"
TASK_SHAPING_DIR = STATE_DIR / "task-shaping"
CLOSEOUT_DIR = STATE_DIR / "closeout"
MULTI_AGENT_DIR = STATE_DIR / "multi-agent"
CURRENT_TASK_PATH = STATE_DIR / "current-task.json"
SESSION_CONTEXT_PATH = STATE_DIR / "session-context.json"
CHAT_ROUTER_STATE_PATH = INTAKE_DIR / "chat-router-state.json"
MASTER_PLAN_PATH = ROOT / "docs" / "plans" / "master-execution-plan.md"
TASK_SPEC_PATH = ROOT / "docs" / "plans" / "task-spec-matrix.md"
TASK_GOV_PATH = ROOT / "docs" / "plans" / "task-governance-extension-matrix.md"
TASKS_PATH = ROOT / "tasks.md"
TASKS_DONE_PATH = ROOT / "tasks-done.md"
VALIDATION_LOG_PATH = ROOT / "docs" / "quality" / "validation-log.md"

RESERVATION_STALE_HOURS = 24
TEMPLATE_FIELD_PATTERN = re.compile(r"^(需求|治理需求|实现任务|实现治理任务|输出物|限制)\s*[：:]\s*(.*)$")
TASK_ID_PATTERN = re.compile(r"^[A-Z]+-[0-9]{3}$")
CONTROL_LINE_PATTERN = re.compile(r"^(先生成(?:执行|任务)模板给我确认，不要直接执行。?|确认没问题，开始执行。?)$")
FIELD_ALIASES = {
    "需求": "business_requirement",
    "治理需求": "governance_requirement",
    "实现任务": "existing_task",
    "实现治理任务": "existing_governance_task",
}
MODE_SINGLE_AGENT = "single-agent"
MODE_MULTI_AGENT = "multi-agent-full-auto"
MODE_PREVIEW_ONLY = "preview-only"
RESERVATION_ACTIVE_STATUSES = {"reserved", "candidate_ready", "blocked", "failed", "materializing"}
RESERVATION_NON_BLOCKING_STATUSES = {"materialized", "released", "paused", "archived", "abandoned"}
RESERVATION_RESUMABLE_STATUSES = {"paused"}
RESERVATION_RESHAPING_REQUIRED_STATUSES = {"released", "archived", "abandoned"}


def now_iso() -> str:
    return now_beijing_iso()


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


def reservation_status(payload: dict[str, Any]) -> str:
    return str(payload.get("status", "")).strip() or "unknown"


def reservation_is_active(payload: dict[str, Any]) -> bool:
    return reservation_status(payload) in RESERVATION_ACTIVE_STATUSES


def reservation_is_non_blocking(payload: dict[str, Any]) -> bool:
    return reservation_status(payload) in RESERVATION_NON_BLOCKING_STATUSES


def reservation_requires_reshaping(payload: dict[str, Any]) -> bool:
    return reservation_status(payload) in RESERVATION_RESHAPING_REQUIRED_STATUSES or bool(payload.get("dry_run"))


def reservation_may_resume(payload: dict[str, Any]) -> bool:
    return reservation_status(payload) in RESERVATION_RESUMABLE_STATUSES


def reservation_runtime_summary_paths(payload: dict[str, Any]) -> list[Path]:
    run_id = str(payload.get("run_id", "")).strip()
    if not run_id:
        return []
    return [
        INTAKE_DIR / run_id / "intake-summary.json",
        TASK_SHAPING_DIR / run_id / "run-summary.json",
    ]


def sync_candidate_runtime_state(payload: dict[str, Any], status: str, reason: str = "") -> None:
    status_meta = {
        "paused": {
            "confirmation_state": "paused",
            "final_outcome": "candidate_paused",
            "recommended_next_step": "Resume the paused candidate with governed_runtime_dashboard.py --resume-candidate, or reshape a new candidate package if the authority boundary changed.",
        },
        "archived": {
            "confirmation_state": "archived",
            "final_outcome": "candidate_archived",
            "recommended_next_step": "Preserve the archived candidate as evidence and rerun governed intake/shaping to create a fresh candidate package if work should resume.",
        },
        "abandoned": {
            "confirmation_state": "abandoned",
            "final_outcome": "candidate_abandoned",
            "recommended_next_step": "Keep the abandoned candidate as historical evidence only; rerun governed intake/shaping if the demand needs to re-enter.",
        },
        "candidate_ready": {
            "confirmation_state": "awaiting-confirmation",
            "final_outcome": "candidate_ready",
            "recommended_next_step": "Review the candidate package and use governed_intake.sh --confirm-run only when the candidate is explicitly resumed and still authoritative.",
        },
    }
    meta = status_meta.get(status)
    if meta is None:
        return
    for summary_path in reservation_runtime_summary_paths(payload):
        if not summary_path.exists():
            continue
        summary = read_json(summary_path, {})
        summary["execution_state"] = "completed"
        summary["confirmation_state"] = meta["confirmation_state"]
        summary["final_outcome"] = meta["final_outcome"]
        summary["recommended_next_step"] = meta["recommended_next_step"]
        if reason:
            summary[f"{status}_reason"] = reason
        write_run_summary(summary_path, summary)


def reservation_is_stale(payload: dict[str, Any], path: Path) -> bool:
    created_raw = str(payload.get("created_at", "")).strip()
    if created_raw:
        try:
            created_at = datetime.fromisoformat(created_raw)
        except ValueError:
            created_at = datetime.fromtimestamp(path.stat().st_mtime, tz=timezone.utc)
    else:
        created_at = datetime.fromtimestamp(path.stat().st_mtime, tz=timezone.utc)
    return created_at < now_beijing() - timedelta(hours=RESERVATION_STALE_HOURS)


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
    previous_status = reservation_status(payload)
    history = list(payload.get("status_history", []))
    payload["status"] = status
    payload["updated_at"] = now_iso()
    payload.update(extra)
    if previous_status != status or extra:
        history.append(
            {
                "from_status": previous_status,
                "to_status": status,
                "changed_at": payload["updated_at"],
                "details": {key: value for key, value in extra.items() if key != "status_history"},
            }
        )
        payload["status_history"] = history
    write_json(path, payload)
    return payload


def release_reservation(path: Path, reason: str, **extra: Any) -> dict[str, Any]:
    return update_reservation(path, "released", release_reason=reason, released_at=now_iso(), **extra)


def pause_reservation(path: Path, reason: str, **extra: Any) -> dict[str, Any]:
    payload = update_reservation(path, "paused", pause_reason=reason, paused_at=now_iso(), **extra)
    sync_candidate_runtime_state(payload, "paused", reason)
    return payload


def archive_reservation(path: Path, reason: str, **extra: Any) -> dict[str, Any]:
    payload = update_reservation(path, "archived", archive_reason=reason, archived_at=now_iso(), **extra)
    sync_candidate_runtime_state(payload, "archived", reason)
    return payload


def abandon_reservation(path: Path, reason: str, **extra: Any) -> dict[str, Any]:
    payload = update_reservation(path, "abandoned", abandon_reason=reason, abandoned_at=now_iso(), **extra)
    sync_candidate_runtime_state(payload, "abandoned", reason)
    return payload


def resume_reservation(path: Path, reason: str, **extra: Any) -> dict[str, Any]:
    payload = update_reservation(path, "candidate_ready", resume_reason=reason, resumed_at=now_iso(), **extra)
    sync_candidate_runtime_state(payload, "candidate_ready", reason)
    return payload


def list_reservation_paths() -> list[Path]:
    if not TASK_RESERVATION_DIR.exists():
        return []
    return sorted(TASK_RESERVATION_DIR.glob("*.json"))


def find_reservation_path(task_id: str) -> Path | None:
    normalized = task_id.strip()
    if not normalized:
        return None
    path = TASK_RESERVATION_DIR / f"{normalized}.json"
    return path if path.exists() else None


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
    if issue_key == "tracked_dirty_worktree":
        return ["git status --short --untracked-files=no", "git diff -- <TRACKED_PATH>"]
    if issue_key == "closeout_tail_drift":
        return [
            "python3 scripts/task_audit.py --check --phase post-closeout",
            "python3 scripts/foreman.py compile-governance --check",
        ]
    if issue_key == "runtime_task_mismatch":
        return ["python3 scripts/foreman.py preflight --task <TASK_ID> --task-class standard --prompt '<PROMPT>'"]
    if issue_key == "reservation_conflict":
        return [
            "python3 scripts/governed_healthcheck.py --check",
            "use governed_runtime_dashboard.py to pause/archive/abandon stale candidates before retrying",
        ]
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
    authority_fields_to_confirm: list[str] | None = None,
    command_overrides: list[str] | None = None,
) -> dict[str, Any]:
    authority_fields = authority_fields_to_confirm
    if authority_fields is None:
        authority_fields = authority_field_hints(human_confirmation_point) if human_confirmation_point else []
    return {
        "issue_key": issue_key,
        "summary": summary,
        "authority_fields_to_confirm": authority_fields,
        "suggested_integrity_checks": command_overrides or integrity_check_suggestions(issue_key),
    }


def runtime_dashboard() -> dict[str, Any]:
    reservation_paths = list_reservation_paths()
    reservations_by_status: dict[str, int] = {}
    active_reservations = 0
    non_blocking_reservations = 0
    for path in reservation_paths:
        payload = read_json(path, {})
        status = reservation_status(payload)
        reservations_by_status[status] = reservations_by_status.get(status, 0) + 1
        if reservation_is_active(payload):
            active_reservations += 1
        if reservation_is_non_blocking(payload):
            non_blocking_reservations += 1

    def count_dirs(path: Path) -> int:
        if not path.exists():
            return 0
        return sum(1 for child in path.iterdir() if child.is_dir())

    closeout_statuses: dict[str, int] = {}
    if CLOSEOUT_DIR.exists():
        for evidence_path in CLOSEOUT_DIR.glob("*/post-closeout-actual.json"):
            status = str(read_json(evidence_path, {}).get("final_status", "unknown")) or "unknown"
            closeout_statuses[status] = closeout_statuses.get(status, 0) + 1

    return {
        "intake_runs": count_dirs(INTAKE_DIR),
        "task_shaping_runs": count_dirs(TASK_SHAPING_DIR),
        "multi_agent_runs": count_dirs(MULTI_AGENT_DIR),
        "closeout_evidence_runs": count_dirs(CLOSEOUT_DIR),
        "closeout_evidence_by_status": closeout_statuses,
        "reservation_files": len(reservation_paths),
        "reservations_by_status": reservations_by_status,
        "active_reservations": active_reservations,
        "non_blocking_reservations": non_blocking_reservations,
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


def load_session_context() -> dict[str, Any]:
    return read_json(SESSION_CONTEXT_PATH, default={})


def save_session_context(payload: dict[str, Any]) -> None:
    write_json(SESSION_CONTEXT_PATH, payload)


def load_chat_router_state() -> dict[str, Any]:
    return read_json(CHAT_ROUTER_STATE_PATH, default={})


def save_chat_router_state(payload: dict[str, Any]) -> None:
    write_json(CHAT_ROUTER_STATE_PATH, payload)


def parse_template_fields(text: str) -> dict[str, str]:
    fields: dict[str, list[str]] = {}
    current_key = ""
    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        if not line.strip():
            if current_key:
                fields[current_key].append("")
            continue
        if CONTROL_LINE_PATTERN.match(line.strip()):
            current_key = ""
            continue
        match = TEMPLATE_FIELD_PATTERN.match(line.strip())
        if match:
            current_key = match.group(1)
            fields.setdefault(current_key, [])
            first_value = match.group(2).strip()
            if first_value:
                fields[current_key].append(first_value)
            continue
        if current_key:
            fields[current_key].append(line.strip())
    return {key: "\n".join(value).strip() for key, value in fields.items()}


def detect_template_kind(text: str) -> str:
    fields = parse_template_fields(text)
    if "实现治理任务" in fields:
        return FIELD_ALIASES["实现治理任务"]
    if "实现任务" in fields:
        return FIELD_ALIASES["实现任务"]
    if "治理需求" in fields:
        return FIELD_ALIASES["治理需求"]
    if "需求" in fields:
        return FIELD_ALIASES["需求"]
    return ""


def infer_task_type(template_kind: str = "", task_id: str = "") -> str:
    if template_kind in {"governance_requirement", "existing_governance_task"}:
        return "governance"
    if template_kind in {"business_requirement", "existing_task"}:
        return "business"
    if task_id.startswith("HARN-"):
        return "governance"
    return "business"


def is_confirmation_prompt(text: str) -> bool:
    normalized = " ".join(text.split()).lower()
    markers = [
        "确认没问题，开始执行",
        "确认没问题, 开始执行",
        "确认没问题开始执行",
        "开始执行",
        "confirm and execute",
        "looks good, execute",
        "looks good execute",
    ]
    return any(marker in normalized for marker in markers)


def confirmation_constraints(text: str) -> str:
    fields = parse_template_fields(text)
    return fields.get("限制", "").strip()


def normalize_task_id(raw: str) -> str:
    return raw.strip().strip("`")


def normalize_cell(value: str) -> str:
    return value.strip().replace("\\|", "|")


def is_table_separator(line: str) -> bool:
    stripped = line.strip()
    if not (stripped.startswith("|") and stripped.endswith("|")):
        return False
    body = (
        stripped.strip("|")
        .replace("|", "")
        .replace(":", "")
        .replace("-", "")
        .replace(" ", "")
        .replace("\t", "")
    )
    return body == ""


def parse_pipe_row(line: str) -> list[str]:
    return [normalize_cell(cell) for cell in line.strip().strip("|").split("|")]


def parse_phase_table_rows(path: Path) -> list[dict[str, str]]:
    lines = read_text(path).splitlines()
    current_phase = ""
    rows: list[dict[str, str]] = []
    index = 0
    while index < len(lines):
        line = lines[index]
        phase_match = re.match(r"^##\s+(Phase-[A-Z])\s*$", line)
        if phase_match:
            current_phase = phase_match.group(1)
            index += 1
            continue
        if (
            line.strip().startswith("|")
            and index + 1 < len(lines)
            and is_table_separator(lines[index + 1])
        ):
            headers = parse_pipe_row(line)
            index += 2
            while index < len(lines) and lines[index].strip().startswith("|"):
                row_line = lines[index]
                if not is_table_separator(row_line):
                    cells = parse_pipe_row(row_line)
                    if len(cells) == len(headers):
                        entry = {headers[pos]: cells[pos] for pos in range(len(headers))}
                        entry["__phase"] = current_phase
                        rows.append(entry)
                index += 1
            continue
        index += 1
    return rows


def load_task_spec_index() -> dict[str, dict[str, str]]:
    index: dict[str, dict[str, str]] = {}
    for row in parse_phase_table_rows(TASK_SPEC_PATH):
        task_id = normalize_task_id(row.get("Task ID", ""))
        if task_id:
            index[task_id] = row
    return index


def load_task_governance_index() -> dict[str, dict[str, str]]:
    index: dict[str, dict[str, str]] = {}
    for row in parse_phase_table_rows(TASK_GOV_PATH):
        task_id = normalize_task_id(row.get("Task ID", ""))
        if task_id:
            index[task_id] = row
    return index


def master_plan_metadata(task_id: str) -> dict[str, str]:
    phase = ""
    epic = ""
    story = ""
    story_id = ""
    for line in read_text(MASTER_PLAN_PATH).splitlines():
        phase_match = re.match(r"^###\s+(Phase-[A-Z])(?:\s+.+)?$", line)
        if phase_match:
            phase = phase_match.group(1)
            continue
        epic_match = re.match(r"^####\s+(Epic\s+`[^`]+`\s+.+)$", line)
        if epic_match:
            epic = epic_match.group(1).strip()
            continue
        story_match = re.match(r"^#####\s+Story\s+`([^`]+)`\s+(.+)$", line)
        if story_match:
            story_id = story_match.group(1)
            story = story_match.group(2).strip()
            continue
        if line.lstrip().startswith("|") and f"`{task_id}`" in line:
            return {
                "phase_heading": phase,
                "epic_heading": epic,
                "story_id": story_id,
                "story_heading": story,
            }
    return {}


def lookup_task_profile(task_id: str) -> dict[str, Any]:
    if not task_id:
        return {}
    profile: dict[str, Any] = {}
    spec_index = load_task_spec_index()
    gov_index = load_task_governance_index()
    if task_id in spec_index:
        profile.update(spec_index[task_id])
    if task_id in gov_index:
        profile.update(
            {
                "Human confirmation point": gov_index[task_id].get("Human confirmation point", ""),
                "Data impact": gov_index[task_id].get("Data impact", ""),
                "Rollback / recovery": gov_index[task_id].get("Rollback / recovery", ""),
            }
        )
    profile.update(master_plan_metadata(task_id))
    profile["task_id"] = task_id
    return profile


def _split_csv_field(value: str | list[str] | None) -> list[str]:
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    if not value:
        return []
    normalized = str(value).replace("、", ",")
    parts = []
    for chunk in normalized.split(","):
        stripped = chunk.strip()
        if stripped:
            parts.append(stripped.strip("`"))
    return parts


def validation_path_from_sources(task_profile: dict[str, Any], candidate_pack: dict[str, Any]) -> list[str]:
    if candidate_pack.get("tests"):
        return [str(item).strip() for item in candidate_pack.get("tests", []) if str(item).strip()]
    return _split_csv_field(task_profile.get("Tests", ""))


def infer_mcp_relevance(task_id: str, task_profile: dict[str, Any], prompt_text: str) -> bool:
    haystack = " ".join(
        [
            task_id,
            str(task_profile.get("Rules", "")),
            str(task_profile.get("Contract", "")),
            prompt_text,
        ]
    ).lower()
    return "mcp" in haystack or any(rule in haystack for rule in ["r-170", "r-171", "r-172", "r-173", "r-174", "r-175", "r-176"])


def decide_execution_mode(
    *,
    task_id: str = "",
    task_profile: dict[str, Any] | None = None,
    candidate_pack: dict[str, Any] | None = None,
    path_selected: str = "",
    prompt_text: str = "",
    output: str = "",
    constraints: str = "",
) -> tuple[str, str]:
    profile = task_profile or {}
    pack = candidate_pack or {}
    explicit_text = " ".join([prompt_text, output, constraints]).lower()
    haystack = " ".join(
        [
            task_id,
            str(profile.get("Contract", "")),
            str(profile.get("Context", "")),
            str(profile.get("Layer", "")),
            str(profile.get("Tech", "")),
            str(pack.get("scope", "")),
            str(pack.get("contract", "")),
            ",".join(pack.get("layer", [])),
            ",".join(pack.get("tech", [])),
            prompt_text,
            output,
            constraints,
        ]
    ).lower()
    if any(token in explicit_text for token in ["multi-agent", "multi agent", "多 agent", "多agent"]) and not any(
        token in explicit_text for token in ["不要 multi-agent", "不要 multi agent", "single-agent", "single agent", "单 agent", "单agent"]
    ):
        return MODE_MULTI_AGENT, "explicit multi-agent markers detected in the routed input"
    if any(token in explicit_text for token in ["single-agent", "single agent", "单 agent", "单agent"]):
        return MODE_SINGLE_AGENT, "explicit single-agent markers detected in the routed input"
    if pack.get("requires_human_decision") is True:
        return MODE_PREVIEW_ONLY, "candidate pack still requires human confirmation before execution routing"
    if task_id.startswith("HARN-"):
        return MODE_SINGLE_AGENT, "governance/tooling tasks default to single-agent unless multi-agent is explicitly requested"
    layers = {item.lower() for item in _split_csv_field(pack.get("layer") or profile.get("Layer", ""))}
    techs = {item.lower() for item in _split_csv_field(pack.get("tech") or profile.get("Tech", ""))}
    if any(token in haystack for token in ["ownership 可切", "splittable ownership", "cross-module", "cross module", "跨模块", "parallel"]):
        return MODE_MULTI_AGENT, "ownership appears explicitly splittable across modules"
    if layers and layers.issubset({"docs", "deployments/ci/scripts"}):
        return MODE_SINGLE_AGENT, "docs/scripts-only change scope does not justify multi-agent by default"
    if techs and techs.issubset({"docs", "ops"}):
        return MODE_SINGLE_AGENT, "governance docs/ops scope defaults to single-agent"
    if path_selected == "existing-task":
        return MODE_SINGLE_AGENT, "existing-task safe default is single-agent until multi-agent need is explicit"
    return MODE_PREVIEW_ONLY, "boundary or ownership split is not explicit enough to launch execution automatically"


def planned_agents_for_mode(mode: str) -> list[str]:
    if mode == MODE_SINGLE_AGENT:
        return ["Main Foreman"]
    if mode == MODE_MULTI_AGENT:
        return ["Main Foreman", "truth-explorer", "worker-name", "validator", "auto-foreman"]
    return ["Main Foreman"]


def build_execution_preview(
    *,
    run_id: str,
    path_selected: str,
    template_kind: str,
    task_id: str = "",
    candidate_task_id: str = "",
    task_profile: dict[str, Any] | None = None,
    candidate_pack: dict[str, Any] | None = None,
    output: str = "",
    constraints: str = "",
    prompt_text: str = "",
    confirm_run_command: str = "",
    requirements_artifact_ref: str = "",
) -> dict[str, Any]:
    profile = task_profile or {}
    pack = candidate_pack or {}
    chosen_task_id = task_id or str(pack.get("task_id", "")).strip()
    mode, mode_reason = decide_execution_mode(
        task_id=chosen_task_id,
        task_profile=profile,
        candidate_pack=pack,
        path_selected=path_selected,
        prompt_text=prompt_text,
        output=output,
        constraints=constraints,
    )
    task_type = infer_task_type(template_kind, chosen_task_id)
    validation_path = validation_path_from_sources(profile, pack)
    closeout_path = []
    if chosen_task_id:
        closeout_path = [
            f"python3 scripts/foreman.py validate {chosen_task_id}",
            "python3 scripts/task_audit.py --check --phase pre-closeout",
            f"python3 scripts/foreman.py closeout {chosen_task_id}",
            "python3 scripts/task_audit.py --check --phase post-closeout",
        ]
    mcp_relevant = infer_mcp_relevance(chosen_task_id, profile, prompt_text)
    if mode == MODE_MULTI_AGENT and mcp_relevant:
        mcp_preview = {
            "enabled": True,
            "assigned_to": ["explorer", "validator"],
            "boundary": "read-only evidence only; worker, Main Foreman, and Auto Foreman stay MCP-disabled",
        }
    elif mcp_relevant:
        mcp_preview = {
            "enabled": False,
            "assigned_to": [],
            "boundary": "single-agent path keeps MCP local/user-configured only; no repo-tracked mcp_profile assignment is used",
        }
    else:
        mcp_preview = {
            "enabled": False,
            "assigned_to": [],
            "boundary": "MCP is not enabled by the current previewed path",
        }
    risks = []
    if pack.get("residual_risk"):
        risks.extend([str(item).strip() for item in pack.get("residual_risk", []) if str(item).strip()])
    elif profile.get("Human confirmation point"):
        risks.append(compact(str(profile.get("Human confirmation point", "")), 180))
    if mode == MODE_PREVIEW_ONLY:
        risks.append("Execution mode remained preview-only because ownership/boundary is not explicit enough.")
    preview = {
        "schema_version": 1,
        "run_id": run_id,
        "task_type": task_type,
        "path_selected": path_selected,
        "execution_mode": mode,
        "execution_mode_reason": mode_reason,
        "formal_task_id": task_id,
        "candidate_task_id": candidate_task_id or str(pack.get("task_id", "")).strip(),
        "outputs": output,
        "constraints": constraints,
        "planned_agents": planned_agents_for_mode(mode),
        "mcp": mcp_preview,
        "requirements_artifact_ref": requirements_artifact_ref,
        "validation_path": validation_path,
        "closeout_path": closeout_path,
        "risks": risks,
        "confirm_run_command": confirm_run_command,
        "wait_for_confirmation_text": "等待显式确认后才允许进入 confirm-run。",
    }
    return preview


def render_execution_preview_markdown(preview: dict[str, Any]) -> str:
    lines = [
        f"# Execution Preview `{preview.get('run_id', '')}`",
        "",
        f"- 任务类型: `{preview.get('task_type', '')}`",
        f"- 路径: `{preview.get('path_selected', '')}`",
        f"- 执行模式: `{preview.get('execution_mode', '')}`",
        f"- 模式理由: {preview.get('execution_mode_reason', '')}",
        f"- formal task: `{preview.get('formal_task_id', '') or 'N/A'}`",
        f"- candidate task: `{preview.get('candidate_task_id', '') or 'N/A'}`",
        f"- 输出物: {preview.get('outputs', '') or 'N/A'}",
        f"- 限制条件: {preview.get('constraints', '') or 'N/A'}",
        f"- planned agents: {', '.join(preview.get('planned_agents', [])) or 'N/A'}",
        f"- MCP enabled: `{str(preview.get('mcp', {}).get('enabled', False)).lower()}`",
        f"- MCP assigned_to: {', '.join(preview.get('mcp', {}).get('assigned_to', [])) or 'N/A'}",
        f"- MCP boundary: {preview.get('mcp', {}).get('boundary', '')}",
        f"- requirements artifact: `{preview.get('requirements_artifact_ref', '') or 'N/A'}`",
        f"- confirm-run: `{preview.get('confirm_run_command', '') or 'N/A'}`",
        f"- 等待确认: {preview.get('wait_for_confirmation_text', '')}",
        "",
        "## Validation Path",
    ]
    for item in preview.get("validation_path", []):
        lines.append(f"- `{item}`")
    lines.extend(["", "## Closeout Path"])
    for item in preview.get("closeout_path", []):
        lines.append(f"- `{item}`")
    lines.extend(["", "## Risks"])
    for item in preview.get("risks", []):
        lines.append(f"- {item}")
    return "\n".join(lines).rstrip() + "\n"


def write_execution_preview(run_root: Path, preview: dict[str, Any]) -> tuple[Path, Path]:
    json_path = run_root / "execution-preview.json"
    md_path = run_root / "execution-preview.md"
    write_json(json_path, preview)
    write_text(md_path, render_execution_preview_markdown(preview))
    return json_path, md_path


def render_requirements_artifact(
    *,
    task_id: str,
    task_type: str,
    output: str,
    constraints: str,
    preview: dict[str, Any],
    source_prompt: str = "",
) -> str:
    lines = [
        f"# Governed Requirements Artifact for `{task_id}`",
        "",
        f"- 任务类型: `{task_type}`",
        f"- 生成时间: `{now_iso()}`",
        f"- execution mode: `{preview.get('execution_mode', '')}`",
        f"- execution path: `{preview.get('path_selected', '')}`",
        f"- 输出物: {output or 'N/A'}",
        f"- 限制条件: {constraints or 'N/A'}",
        "",
    ]
    if source_prompt.strip():
        lines.extend(["## Source Prompt", "", source_prompt.strip(), ""])
    lines.extend(
        [
            "## Execution Preview Summary",
            "",
            f"- formal task: `{preview.get('formal_task_id', '') or 'N/A'}`",
            f"- candidate task: `{preview.get('candidate_task_id', '') or 'N/A'}`",
            f"- planned agents: {', '.join(preview.get('planned_agents', [])) or 'N/A'}",
            f"- MCP boundary: {preview.get('mcp', {}).get('boundary', '')}",
            "",
        ]
    )
    if preview.get("validation_path"):
        lines.append("## Validation Path")
        lines.append("")
        for item in preview.get("validation_path", []):
            lines.append(f"- `{item}`")
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"
