#!/usr/bin/env python3
"""Stop-time Codex hook for SQLForge governance closeout gating."""

from __future__ import annotations

from shared import block, current_phase, load_current_task, load_session_context, read_stdin_json, run_foreman, success


def main() -> int:
    read_stdin_json()
    task = load_current_task()
    phase = current_phase()

    if phase not in {"done_ready", "closeout", "delivery_closeout"}:
        summary = load_session_context().get("summary", "")
        success(summary if isinstance(summary, str) and summary else None)
        return 0

    task_id = task.get("task_id")
    if not task_id:
        block("Runtime state entered a closeout phase without an active task id.")
        return 0

    code, stdout, stderr = run_foreman(["audit", "--phase", "pre-closeout"])
    if code != 0:
        block(
            "Task cannot stop yet because pre-closeout governance checks failed. "
            f"Run closeout steps for {task_id} first.\n{stderr or stdout}"
        )
        return 0

    if phase == "delivery_closeout":
        block(
            f"Task {task_id} is in delivery_closeout and still requires tag/write-back completion before stop."
        )
        return 0

    block(
        f"Task {task_id} is in {phase}; archive it, create the single-task commit, "
        "and rerun post-closeout audit before ending the turn."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
