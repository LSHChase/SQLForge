#!/usr/bin/env python3
"""Route Codex natural-language task templates into governed intake runtime flows."""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from scripts.governed_v2_support import (
    compact,
    confirmation_constraints,
    detect_template_kind,
    is_confirmation_prompt,
    load_chat_router_state,
    now_iso,
    read_json,
    relative_to_root,
    save_chat_router_state,
)


def default_run_id() -> str:
    return "chat-router-" + datetime.now(timezone.utc).astimezone().strftime("%Y%m%d%H%M%S")


def run(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=ROOT, capture_output=True, text=True)


def preview_context(payload: dict[str, Any]) -> str:
    return "\n".join(
        [
            "Chat-native router matched a governed template and prepared an execution preview.",
            f"Run ID: {payload.get('run_id', '')}",
            f"Path: {payload.get('path_selected', '')}",
            f"Mode: {payload.get('execution_mode', '')}",
            f"Formal task: {payload.get('task_id', '') or 'N/A'}",
            f"Candidate task: {payload.get('candidate_task_id', '') or 'N/A'}",
            f"Preview: {payload.get('execution_preview_markdown_ref', '') or 'N/A'}",
            f"Requirements artifact: {payload.get('requirements_artifact_ref', '') or 'N/A'}",
            f"Confirm with: bash scripts/governed_intake.sh --confirm-run {payload.get('run_id', '')}",
            "No execution has started yet; wait for explicit confirmation before coding or materialization.",
        ]
    )


def confirm_context(run_id: str, payload: dict[str, Any], dry_run: bool) -> str:
    prefix = "dry-run confirmation previewed" if dry_run else "confirmation executed"
    return "\n".join(
        [
            f"Chat-native router {prefix} for intake run {run_id}.",
            f"Mode: {payload.get('execution_mode', '')}",
            f"Outcome: {payload.get('final_outcome', '')}",
            f"Summary: {relative_to_root(ROOT / '.codex' / 'state' / 'intake' / run_id / 'intake-summary.json')}",
            f"Next: {payload.get('recommended_next_step', '')}",
        ]
    )


def route_template(prompt_text: str, run_id: str) -> dict[str, Any]:
    adapter_command = [
        "python3",
        "scripts/codex_template_adapter.py",
        "--template-text",
        prompt_text,
        "--run-id",
        run_id,
        "--execute",
    ]
    result = run(adapter_command)
    summary_path = ROOT / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
    adapter_summary_path = ROOT / ".codex" / "state" / "intake" / run_id / "template-adapter-summary.json"
    intake_summary = read_json(summary_path, {})
    adapter_summary = read_json(adapter_summary_path, {})

    if result.returncode != 0 or not intake_summary:
        return {
            "handled": True,
            "route": "template-preview",
            "status": "failed",
            "run_id": run_id,
            "command": adapter_command,
            "stdout": result.stdout.strip(),
            "stderr": result.stderr.strip(),
            "summary_path": relative_to_root(summary_path),
            "message": "chat-native router failed to prepare governed intake preview",
        }

    save_chat_router_state(
        {
            "active_run_id": run_id,
            "last_previewed_run_id": run_id,
            "template_kind": adapter_summary.get("template_kind", ""),
            "task_id": intake_summary.get("task_id", ""),
            "path_selected": intake_summary.get("path_selected", ""),
            "execution_mode": intake_summary.get("execution_mode", ""),
            "status": "awaiting-confirmation",
            "updated_at": now_iso(),
        }
    )
    return {
        "handled": True,
        "route": "template-preview",
        "status": "prepared",
        "run_id": run_id,
        "command": adapter_command,
        "summary_path": relative_to_root(summary_path),
        "payload": intake_summary,
        "message": preview_context(intake_summary),
    }


def route_confirmation(prompt_text: str, dry_run: bool) -> dict[str, Any]:
    state = load_chat_router_state()
    run_id = str(state.get("active_run_id", "")).strip() or str(state.get("last_previewed_run_id", "")).strip()
    if not run_id:
        return {"handled": False, "route": "pass-through", "status": "no-active-run"}

    summary_path = ROOT / ".codex" / "state" / "intake" / run_id / "intake-summary.json"
    intake_summary = read_json(summary_path, {})
    if not intake_summary or intake_summary.get("confirmation_state") not in {"awaiting-confirmation", "dry_run_previewed"}:
        return {"handled": False, "route": "pass-through", "status": "not-awaiting-confirmation"}

    command = ["bash", "scripts/governed_intake.sh", "--confirm-run", run_id]
    extra_constraints = confirmation_constraints(prompt_text)
    if extra_constraints:
        command.extend(["--constraints", extra_constraints])
    if dry_run:
        command.append("--dry-run")
    result = run(command)
    refreshed = read_json(summary_path, {})

    if result.returncode == 0 and not dry_run:
        save_chat_router_state(
            {
                "active_run_id": "",
                "last_previewed_run_id": run_id,
                "last_confirmed_run_id": run_id,
                "task_id": refreshed.get("task_id", intake_summary.get("task_id", "")),
                "path_selected": refreshed.get("path_selected", intake_summary.get("path_selected", "")),
                "execution_mode": refreshed.get("execution_mode", intake_summary.get("execution_mode", "")),
                "status": refreshed.get("final_outcome", "executed"),
                "updated_at": now_iso(),
            }
        )
    elif result.returncode == 0:
        save_chat_router_state(
            {
                **state,
                "active_run_id": run_id,
                "last_previewed_run_id": run_id,
                "status": "dry_run_previewed",
                "updated_at": now_iso(),
            }
        )

    return {
        "handled": True,
        "route": "confirmation",
        "status": "executed" if result.returncode == 0 else "failed",
        "run_id": run_id,
        "command": command,
        "stdout": result.stdout.strip(),
        "stderr": result.stderr.strip(),
        "summary_path": relative_to_root(summary_path),
        "payload": refreshed,
        "message": confirm_context(run_id, refreshed, dry_run)
        if result.returncode == 0
        else f"Chat-native router failed to execute confirmation for {run_id}: {compact(result.stderr or result.stdout, 400)}",
    }


def route_prompt(prompt_text: str, run_id: str, confirm_dry_run: bool) -> dict[str, Any]:
    if detect_template_kind(prompt_text):
        return route_template(prompt_text, run_id)
    if is_confirmation_prompt(prompt_text):
        return route_confirmation(prompt_text, confirm_dry_run)
    return {"handled": False, "route": "pass-through", "status": "ignored"}


def main() -> int:
    parser = argparse.ArgumentParser(description="Route governed chat-native templates into governed intake runtime automation.")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--prompt-text", help="Inline user prompt text.")
    source.add_argument("--prompt-file", help="File containing a user prompt.")
    parser.add_argument("--run-id", default="", help="Stable run id to use when creating a preview.")
    parser.add_argument("--confirm-dry-run", action="store_true", help="Use governed_intake confirm-run --dry-run for confirmation prompts.")
    parser.add_argument("--json", action="store_true", help="Print machine-readable JSON output.")
    args = parser.parse_args()

    prompt_text = args.prompt_text if args.prompt_text is not None else Path(args.prompt_file).read_text(encoding="utf-8")
    run_id = args.run_id.strip() or default_run_id()
    payload = route_prompt(prompt_text, run_id, args.confirm_dry_run)

    if args.json:
        print(json.dumps(payload, ensure_ascii=False, indent=2))
    else:
        print(payload.get("message", compact(prompt_text, 300)))
    return 0 if payload.get("status") != "failed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
