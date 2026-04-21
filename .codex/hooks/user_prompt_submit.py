#!/usr/bin/env python3
"""Prompt-time Codex hook for SQLForge governance preflight."""

from __future__ import annotations

from shared import detect_prompt_text, load_session_context, read_stdin_json, run_foreman, success


def main() -> int:
    payload = read_stdin_json()
    prompt = detect_prompt_text(payload)

    code, stdout, stderr = run_foreman(["preflight", "--prompt", prompt])
    context = load_session_context()
    summary = context.get("summary", stdout or stderr)
    if code != 0:
        success(f"Repository preflight reported a non-fatal issue:\n{summary}")
        return 0

    success(summary if isinstance(summary, str) else stdout)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
