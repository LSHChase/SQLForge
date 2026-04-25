#!/usr/bin/env python3
"""Adapt SQLForge Codex natural-language task templates into governed intake commands."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from scripts.governed_v2_support import (
    INTAKE_DIR,
    compact,
    detect_template_kind,
    now_iso,
    parse_template_fields,
    relative_to_root,
    write_json,
)

SCHEMA_VERSION = 2
TASK_ID_PATTERN = re.compile(r"^[A-Z]+-[0-9]{3}$")
FIELD_ALIASES = {
    "需求": "business_requirement",
    "治理需求": "governance_requirement",
    "实现任务": "existing_task",
    "实现治理任务": "existing_governance_task",
}


def parse_template(text: str) -> dict[str, Any]:
    fields = parse_template_fields(text)
    template_kind = detect_template_kind(text)
    task_key = "实现治理任务" if "实现治理任务" in fields else "实现任务" if "实现任务" in fields else ""
    requirement_key = "治理需求" if "治理需求" in fields else "需求" if "需求" in fields else ""
    output = fields.get("输出物", "").strip()
    constraints = fields.get("限制", "").strip()

    if task_key:
        task_id = fields[task_key].strip().splitlines()[0].strip()
        if not TASK_ID_PATTERN.match(task_id):
            raise SystemExit(f"Invalid task id in {task_key}: {task_id}")
        path_selected = "existing-task"
        prompt = ""
    elif requirement_key:
        requirement = fields[requirement_key].strip()
        if not requirement:
            raise SystemExit(f"Missing content for {requirement_key}.")
        path_selected = "no-task-shaping"
        prompt_parts = [f"{requirement_key}：{requirement}"]
        if output:
            prompt_parts.append(f"输出物：{output}")
        if constraints:
            prompt_parts.append(f"限制：{constraints}")
        prompt_parts.append("先生成执行模板给我确认，不要直接执行。")
        prompt = "\n".join(prompt_parts)
        task_id = ""
    else:
        raise SystemExit("Template must contain one of: 需求：, 治理需求：, 实现任务：, 实现治理任务：")

    return {
        "schema_version": SCHEMA_VERSION,
        "template_kind": template_kind,
        "path_selected": path_selected,
        "task_id": task_id,
        "prompt": prompt,
        "output": output,
        "constraints": constraints,
        "template_fields": fields,
        "recommended_next_step": "Run the governed intake command, review the generated summary/template, then confirm explicitly.",
    }


def default_run_id() -> str:
    return "template-adapter-" + datetime.now(timezone.utc).astimezone().strftime("%Y%m%d%H%M%S")


def build_governed_intake_command(payload: dict[str, Any], run_id: str) -> list[str]:
    command = ["bash", "scripts/governed_intake.sh", "--run-id", run_id]
    if payload["path_selected"] == "existing-task":
        command.extend(["--task", str(payload["task_id"])])
    else:
        command.extend(["--prompt", str(payload["prompt"])])
    if payload.get("template_kind"):
        command.extend(["--template-kind", str(payload["template_kind"])])
    if payload.get("output"):
        command.extend(["--output", str(payload["output"])])
    if payload.get("constraints"):
        command.extend(["--constraints", str(payload["constraints"])])
    return command


def main() -> int:
    parser = argparse.ArgumentParser(description="Adapt Codex natural-language templates to governed_intake commands.")
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument("--template-text", help="Inline Codex natural-language template text.")
    source.add_argument("--template-file", help="File containing a Codex natural-language template.")
    parser.add_argument("--run-id", default="", help="Stable adapter run id for machine-readable output.")
    parser.add_argument("--execute", action="store_true", help="Execute the generated governed_intake command after writing the summary.")
    args = parser.parse_args()

    template_text = args.template_text if args.template_text is not None else Path(args.template_file).read_text(encoding="utf-8")
    payload = parse_template(template_text)
    run_id = args.run_id.strip() or default_run_id()
    run_root = INTAKE_DIR / run_id
    summary_path = run_root / "template-adapter-summary.json"
    payload["governed_intake_command"] = build_governed_intake_command(payload, run_id)
    payload.update(
        {
            "run_id": run_id,
            "raw_template": template_text,
            "summary": compact(template_text, 500),
            "execution_state": "executing" if args.execute else "previewed",
            "executed_at": now_iso(),
            "summary_ref": relative_to_root(summary_path),
        }
    )
    write_json(summary_path, payload)

    if args.execute:
        result = subprocess.run(payload["governed_intake_command"], cwd=ROOT, capture_output=True, text=True)
        payload["governed_intake_exit_code"] = result.returncode
        payload["governed_intake_stdout"] = result.stdout.strip()
        payload["governed_intake_stderr"] = result.stderr.strip()
        payload["execution_state"] = "completed"
        write_json(summary_path, payload)
        print(relative_to_root(summary_path))
        return result.returncode

    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
