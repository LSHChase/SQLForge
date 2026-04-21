#!/usr/bin/env python3
"""Prompt-time Codex hook for SQLForge governance preflight."""

from __future__ import annotations

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


def main() -> int:
    payload = read_stdin_json()
    prompt = detect_prompt_text(payload)
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
