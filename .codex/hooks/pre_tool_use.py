#!/usr/bin/env python3
"""Bash-time Codex hook for SQLForge governance guardrails."""

from __future__ import annotations

from shared import (
    current_task_class,
    detect_bash_command,
    emit_pretool_allow,
    emit_pretool_deny,
    is_bound_to_ledger,
    read_stdin_json,
)


DENIED_EXACT = {
    "git add .",
    "git add -A",
    "git add --all",
    "git commit -a",
    "git checkout --",
    "git reset --hard",
    "git clean -fd",
    "git clean -fdx",
}


def main() -> int:
    payload = read_stdin_json()
    command = detect_bash_command(payload).strip()
    lowered = command.lower()

    if not command:
        emit_pretool_allow()
        return 0

    if command in DENIED_EXACT or "rm -rf" in lowered:
        emit_pretool_deny(f"Blocked audit-breaking command: {command}")
        return 0

    if current_task_class() in {"standard", "delivery"} and not is_bound_to_ledger():
        if any(
            token in lowered
            for token in ("git add", "git commit", "mvn ", "npm ", "sed -i", "perl -0pi", "cat >", "python3 scripts/foreman.py closeout")
        ):
            emit_pretool_deny("Non-trivial implementation work requires an active ledger-bound task before Bash execution.")
            return 0

    emit_pretool_allow()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
