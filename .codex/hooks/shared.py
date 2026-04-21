#!/usr/bin/env python3
"""Shared utilities for repository-local Codex hooks."""

from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, Tuple


ROOT = Path(__file__).resolve().parents[2]
STATE_DIR = ROOT / ".codex" / "state"
CURRENT_TASK_PATH = STATE_DIR / "current-task.json"
SESSION_CONTEXT_PATH = STATE_DIR / "session-context.json"


def read_stdin_json() -> Dict[str, Any]:
    raw = sys.stdin.read().strip()
    if not raw:
        return {}
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        return {"raw": raw}


def load_json(path: Path, default: Dict[str, Any] | None = None) -> Dict[str, Any]:
    if not path.exists():
        return {} if default is None else default
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path: Path, payload: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, ensure_ascii=True, indent=2) + "\n", encoding="utf-8")


def load_current_task() -> Dict[str, Any]:
    return load_json(CURRENT_TASK_PATH, default={})


def load_session_context() -> Dict[str, Any]:
    return load_json(SESSION_CONTEXT_PATH, default={})


def run_foreman(arguments: list[str]) -> Tuple[int, str, str]:
    process = subprocess.run(
        ["python3", str(ROOT / "scripts" / "foreman.py"), *arguments],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    return process.returncode, process.stdout.strip(), process.stderr.strip()


def emit(payload: Dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(payload, ensure_ascii=True))


def success(additional_context: str | None = None) -> None:
    payload: Dict[str, Any] = {}
    if additional_context:
        payload["additionalContext"] = additional_context
    emit(payload)


def block(reason: str) -> None:
    emit({"decision": "block", "reason": reason})


def allow(reason: str | None = None) -> None:
    payload: Dict[str, Any] = {"decision": "allow"}
    if reason:
        payload["reason"] = reason
    emit(payload)


def deny(reason: str) -> None:
    emit({"decision": "deny", "reason": reason})


def current_phase() -> str:
    return str(load_current_task().get("phase", "idle"))


def current_task_class() -> str:
    return str(load_current_task().get("task_class", "advisory"))


def is_bound_to_ledger() -> bool:
    return bool(load_current_task().get("bound_to_ledger", False))


def detect_prompt_text(payload: Dict[str, Any]) -> str:
    for key in ("prompt", "input", "message"):
        value = payload.get(key)
        if isinstance(value, str):
            return value
    return ""


def detect_bash_command(payload: Dict[str, Any]) -> str:
    if isinstance(payload.get("command"), str):
        return payload["command"]
    tool_input = payload.get("tool_input")
    if isinstance(tool_input, dict) and isinstance(tool_input.get("cmd"), str):
        return tool_input["cmd"]
    return ""


def has_strict_stop_phase() -> bool:
    return current_phase() in {"done_ready", "closeout", "delivery_closeout"}


def now_iso() -> str:
    result = subprocess.run(
        ["date", "--iso-8601=seconds"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout.strip()


def update_runtime_state(**fields: Any) -> None:
    current = load_current_task()
    current.update(fields)
    save_json(CURRENT_TASK_PATH, current)


def env_enabled(name: str) -> bool:
    return os.environ.get(name, "").lower() in {"1", "true", "yes", "on"}
