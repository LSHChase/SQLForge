#!/usr/bin/env python3
"""Approval-time Codex hook for SQLForge governance."""

from __future__ import annotations

from shared import detect_bash_command, emit_permission_decision, emit_permission_pass, read_stdin_json


AUTO_DENY_PATTERNS = ("git push", "gh release", "npm publish", "mvn deploy")


def main() -> int:
    payload = read_stdin_json()
    command = detect_bash_command(payload)
    lowered = command.lower()

    if any(pattern in lowered for pattern in AUTO_DENY_PATTERNS):
        emit_permission_decision("deny", f"Repository policy requires explicit human approval outside Codex for: {command}")
        return 0

    emit_permission_pass()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
