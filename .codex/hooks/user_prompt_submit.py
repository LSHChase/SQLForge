#!/usr/bin/env python3
"""Prompt-time Codex hook for SQLForge governance preflight."""

from __future__ import annotations

import subprocess
from pathlib import Path

from shared import (
    current_task_class,
    current_task_id,
    detect_prompt_task_id,
    detect_prompt_text,
    emit_prompt_block,
    emit_user_prompt_context,
    load_session_context,
    read_stdin_json,
    run_foreman,
)


ROOT = Path(__file__).resolve().parents[2]


def run_chat_router(prompt: str) -> tuple[int, dict]:
    process = subprocess.run(
        [
            "python3",
            str(ROOT / "scripts" / "chat_native_router.py"),
            "--prompt-text",
            prompt,
            "--json",
        ],
        cwd=ROOT,
        capture_output=True,
        text=True,
    )
    payload = {}
    raw = process.stdout.strip()
    if raw:
        try:
            import json

            payload = json.loads(raw)
        except Exception:
            payload = {"status": "failed", "message": raw or process.stderr.strip()}
    return process.returncode, payload


def main() -> int:
    payload = read_stdin_json()
    prompt = detect_prompt_text(payload)
    router_code, router_payload = run_chat_router(prompt) if prompt else (0, {"handled": False})
    if router_payload.get("handled"):
        message = str(router_payload.get("message", "")).strip() or str(router_payload.get("stdout", "")).strip()
        if router_code != 0 or router_payload.get("status") == "failed":
            emit_prompt_block(message or "chat-native router failed")
            return 0
        emit_user_prompt_context(message)
        return 0

    bound_task_id = detect_prompt_task_id(prompt) or current_task_id()
    current_class = current_task_class()
    arguments = ["preflight", "--prompt", prompt]
    if bound_task_id:
        arguments.extend(["--task", bound_task_id])
    if current_class not in {"", "none"}:
        arguments.extend(["--task-class", current_class])

    code, stdout, stderr = run_foreman(arguments)
    context = load_session_context()
    summary = context.get("summary", stdout or stderr)
    if code != 0:
        emit_prompt_block(f"Repository preflight failed: {summary}")
        return 0

    emit_user_prompt_context(summary if isinstance(summary, str) and summary else stdout)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
