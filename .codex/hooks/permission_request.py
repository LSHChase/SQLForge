#!/usr/bin/env python3
"""Approval-time Codex hook for SQLForge governance."""

from __future__ import annotations

from shared import allow, deny, detect_bash_command, read_stdin_json


AUTO_DENY_PATTERNS = ("git push", "gh release", "npm publish", "mvn deploy")


def main() -> int:
    payload = read_stdin_json()
    command = detect_bash_command(payload)
    lowered = command.lower()

    if any(pattern in lowered for pattern in AUTO_DENY_PATTERNS):
        deny(f"Repository policy requires explicit human approval outside Codex for: {command}")
        return 0

    allow("No additional repository-specific approval override.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
