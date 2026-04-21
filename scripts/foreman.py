#!/usr/bin/env python3
"""Unified Codex/foreman governance entrypoint for SQLForge."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shlex
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Sequence


ROOT = Path(__file__).resolve().parent.parent
DOCS_DIR = ROOT / "docs"
CODEX_DIR = ROOT / ".codex"
POLICY_DIR = CODEX_DIR / "policy"
STATE_DIR = CODEX_DIR / "state"
TASKS_PATH = ROOT / "tasks.md"
TASKS_DONE_PATH = ROOT / "tasks-done.md"
INBOX_PATH = ROOT / "INBOX.md"
VALIDATION_LOG_PATH = DOCS_DIR / "quality" / "validation-log.md"
SESSION_CONTEXT_PATH = STATE_DIR / "session-context.json"
CURRENT_TASK_PATH = STATE_DIR / "current-task.json"
TASK_SPEC_PATH = DOCS_DIR / "plans" / "task-spec-matrix.md"
TASK_GOV_PATH = DOCS_DIR / "plans" / "task-governance-extension-matrix.md"
MASTER_PLAN_PATH = DOCS_DIR / "plans" / "master-execution-plan.md"
IMPLEMENTATION_READINESS_PATH = DOCS_DIR / "plans" / "implementation-readiness.md"
BLUEPRINT_PATH = DOCS_DIR / "plans" / "codex-governance-integration-blueprint.md"
ACTIVE_EXEC_PLAN_DIR = DOCS_DIR / "exec-plans" / "active"
COMPLETED_EXEC_PLAN_DIR = DOCS_DIR / "exec-plans" / "completed"

AUTHORITY_ENTRY = [
    "docs/README.md",
    "docs/plans/document-truth-baseline.md",
    "docs/architecture/init.md",
    "docs/rules/codex-rules.md",
    "docs/quality/validation-rules.md",
    "docs/plans/master-execution-plan.md",
    "docs/plans/phase-prerequisite-matrix.md",
    "docs/plans/task-spec-matrix.md",
    "docs/plans/task-governance-extension-matrix.md",
    "tasks.md",
    "tasks-done.md",
    "INBOX.md",
]

TASK_STATUS_SECTIONS = ["Todo", "In Progress", "In Review", "Blocked"]
TASK_CLASSES = ["none", "advisory", "trivial", "standard", "delivery"]
TASK_PHASES = [
    "idle",
    "preflight",
    "discovery",
    "implementation",
    "validation",
    "done_ready",
    "closeout",
    "delivery_closeout",
    "closed",
    "blocked",
]
TASK_POLICY = {
    "none": {"requires_ledger": False, "delivery_scope": False},
    "advisory": {"requires_ledger": False, "delivery_scope": False},
    "trivial": {"requires_ledger": False, "delivery_scope": False},
    "standard": {"requires_ledger": True, "delivery_scope": False},
    "delivery": {"requires_ledger": True, "delivery_scope": True},
}
CURRENT_TASK_SCHEMA_VERSION = 2

TASK_HEADER_PATTERN = re.compile(r"^###\s+([A-Z0-9-]+):\s+(.+)$", re.MULTILINE)
STATUS_LINE_PATTERN = re.compile(r"^- Status:\s*(.+)$", re.MULTILINE)
COMMIT_LINE_PATTERN = re.compile(r"^- Commit subject:\s*`?(.+?)`?$", re.MULTILINE)


@dataclass
class CommandResult:
    code: int
    stdout: str
    stderr: str


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def read_json(path: Path, default: Dict[str, Any] | None = None) -> Dict[str, Any]:
    if not path.exists():
        return {} if default is None else json.loads(json.dumps(default))
    return json.loads(read_text(path))


def write_json(path: Path, payload: Dict[str, Any]) -> None:
    write_text(path, json.dumps(payload, ensure_ascii=True, indent=2) + "\n")


def run(command: Sequence[str], cwd: Path | None = None) -> CommandResult:
    process = subprocess.run(
        list(command),
        cwd=cwd or ROOT,
        capture_output=True,
        text=True,
    )
    return CommandResult(process.returncode, process.stdout, process.stderr)


def run_or_raise(command: Sequence[str], cwd: Path | None = None) -> CommandResult:
    result = run(command, cwd=cwd)
    if result.code != 0:
        message = result.stderr.strip() or result.stdout.strip() or "command failed"
        raise SystemExit(f"{' '.join(command)}\n{message}")
    return result


def now_iso() -> str:
    return run_or_raise(["date", "--iso-8601=seconds"]).stdout.strip()


def today_iso() -> str:
    return now_iso()[:10]


def compact(text: str, limit: int = 200) -> str:
    normalized = " ".join(text.split())
    if len(normalized) <= limit:
        return normalized
    return normalized[: limit - 3] + "..."


def hash_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def hash_payload(payload: Dict[str, Any]) -> str:
    return hashlib.sha256(json.dumps(payload, sort_keys=True).encode("utf-8")).hexdigest()


def relative_path(path: Path) -> str:
    return str(path.relative_to(ROOT))


def normalize_task_id(raw: str) -> str:
    return raw.strip().strip("`")


def normalize_cell(value: str) -> str:
    return value.strip().replace("\\|", "|")


def is_table_separator(line: str) -> bool:
    stripped = line.strip()
    if not (stripped.startswith("|") and stripped.endswith("|")):
        return False
    body = (
        stripped.strip("|")
        .replace("|", "")
        .replace(":", "")
        .replace("-", "")
        .replace(" ", "")
        .replace("\t", "")
    )
    return body == ""


def parse_pipe_row(line: str) -> List[str]:
    return [normalize_cell(cell) for cell in line.strip().strip("|").split("|")]


def parse_phase_table_rows(path: Path) -> List[Dict[str, str]]:
    lines = read_text(path).splitlines()
    current_phase = ""
    rows: List[Dict[str, str]] = []
    index = 0
    while index < len(lines):
        line = lines[index]
        phase_match = re.match(r"^##\s+(Phase-[A-Z])\s*$", line)
        if phase_match:
            current_phase = phase_match.group(1)
            index += 1
            continue
        if (
            line.strip().startswith("|")
            and index + 1 < len(lines)
            and is_table_separator(lines[index + 1])
        ):
            headers = parse_pipe_row(line)
            index += 2
            while index < len(lines) and lines[index].strip().startswith("|"):
                row_line = lines[index]
                if not is_table_separator(row_line):
                    cells = parse_pipe_row(row_line)
                    if len(cells) == len(headers):
                        entry = {headers[pos]: cells[pos] for pos in range(len(headers))}
                        entry["__phase"] = current_phase
                        rows.append(entry)
                index += 1
            continue
        index += 1
    return rows


def load_task_spec_index() -> Dict[str, Dict[str, str]]:
    index: Dict[str, Dict[str, str]] = {}
    for row in parse_phase_table_rows(TASK_SPEC_PATH):
        task_id = normalize_task_id(row.get("Task ID", ""))
        if task_id:
            index[task_id] = row
    return index


def load_task_governance_index() -> Dict[str, Dict[str, str]]:
    index: Dict[str, Dict[str, str]] = {}
    for row in parse_phase_table_rows(TASK_GOV_PATH):
        task_id = normalize_task_id(row.get("Task ID", ""))
        if task_id:
            index[task_id] = row
    return index


def master_plan_metadata(task_id: str) -> Dict[str, str]:
    phase = ""
    epic = ""
    story = ""
    for line in read_text(MASTER_PLAN_PATH).splitlines():
        if line.startswith("### Phase-"):
            phase = line.replace("### ", "", 1).strip()
            continue
        if line.startswith("#### Epic "):
            epic = line.replace("#### ", "", 1).strip()
            continue
        if line.startswith("##### Story "):
            story = line.replace("##### ", "", 1).strip()
            continue
        if line.lstrip().startswith("|") and f"`{task_id}`" in line:
            return {
                "phase_heading": phase,
                "epic_heading": epic,
                "story_heading": story,
            }
    return {}


def extract_task_blocks_with_spans(content: str) -> List[Dict[str, Any]]:
    matches = list(TASK_HEADER_PATTERN.finditer(content))
    blocks: List[Dict[str, Any]] = []
    for position, match in enumerate(matches):
        start = match.start()
        end = matches[position + 1].start() if position + 1 < len(matches) else len(content)
        blocks.append(
            {
                "task_id": match.group(1).strip(),
                "title": match.group(2).strip(),
                "body": content[start:end].strip(),
                "start": start,
                "end": end,
            }
        )
    return blocks


def find_task_block(path: Path, task_id: str) -> Dict[str, Any] | None:
    for block in extract_task_blocks_with_spans(read_text(path)):
        if block["task_id"] == task_id:
            return block
    return None


def task_exists_anywhere(task_id: str) -> bool:
    return find_task_block(TASKS_PATH, task_id) is not None or find_task_block(TASKS_DONE_PATH, task_id) is not None


def current_active_plan(task_id: str) -> str:
    if not task_id:
        return ""
    matches = sorted(ACTIVE_EXEC_PLAN_DIR.glob(f"{task_id}-*.md"))
    return relative_path(matches[0]) if matches else ""


def task_ledger_status(task_id: str) -> str:
    block = find_task_block(TASKS_PATH, task_id)
    if block is not None:
        return status_of(block["body"])
    block = find_task_block(TASKS_DONE_PATH, task_id)
    if block is not None:
        return "done"
    return ""


def status_of(body: str) -> str:
    match = STATUS_LINE_PATTERN.search(body)
    return match.group(1).strip() if match else ""


def commit_subject_of(body: str) -> str:
    match = COMMIT_LINE_PATTERN.search(body)
    return match.group(1).strip() if match else ""


def lookup_task_profile(task_id: str) -> Dict[str, Any]:
    spec_index = load_task_spec_index()
    gov_index = load_task_governance_index()
    profile: Dict[str, Any] = {}
    if task_id in spec_index:
        profile.update(spec_index[task_id])
        profile["task_id"] = task_id
    if task_id in gov_index:
        profile.update(
            {
                "Human confirmation point": gov_index[task_id].get("Human confirmation point", ""),
                "Data impact": gov_index[task_id].get("Data impact", ""),
                "Rollback / recovery": gov_index[task_id].get("Rollback / recovery", ""),
            }
        )
    profile.update(master_plan_metadata(task_id))
    ledger_block = find_task_block(TASKS_PATH, task_id) or find_task_block(TASKS_DONE_PATH, task_id)
    if ledger_block is not None:
        profile["ledger_title"] = ledger_block["title"]
        profile["ledger_status"] = status_of(ledger_block["body"])
    plan_ref = current_active_plan(task_id)
    if plan_ref:
        profile["plan_ref"] = plan_ref
    return profile


def classify_task(prompt: str, task_id: str = "", explicit: str | None = None, profile: Dict[str, Any] | None = None) -> str:
    if explicit:
        return explicit
    if profile:
        phase = str(profile.get("__phase", ""))
        context = str(profile.get("Context", ""))
        if task_id.startswith("F-TASK-") or phase == "Phase-F" or "Delivery" in context:
            return "delivery"
    lowered = prompt.lower()
    if any(word in lowered for word in ("分析", "analysis", "explain", "why", "review only")):
        return "advisory"
    if any(word in lowered for word in ("typo", "small doc", "trivial", "rename one line")):
        return "trivial"
    return "standard"


def requires_ledger(task_class: str) -> bool:
    return bool(TASK_POLICY[task_class]["requires_ledger"])


def title_and_excerpt(path: Path) -> Dict[str, str]:
    text = read_text(path)
    title = path.name
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            title = stripped.lstrip("#").strip()
            break
    paragraphs: List[str] = []
    current: List[str] = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            if current:
                paragraph = " ".join(current)
                if not paragraph.startswith("#"):
                    paragraphs.append(paragraph)
                current = []
            continue
        if stripped.startswith("#"):
            continue
        current.append(stripped)
    if current:
        paragraphs.append(" ".join(current))
    excerpt = compact(paragraphs[0] if paragraphs else "", 220)
    return {"title": title, "excerpt": excerpt, "sha256": hash_text(text)}


def authority_digest(paths: Sequence[str]) -> List[Dict[str, str]]:
    digest: List[Dict[str, str]] = []
    for relative in paths:
        target = ROOT / relative
        if target.exists():
            summary = title_and_excerpt(target)
            digest.append({"path": relative, **summary})
    return digest


def build_preflight_context(prompt: str, task: str, task_class: str) -> Dict[str, Any]:
    digest = authority_digest(AUTHORITY_ENTRY)
    profile = lookup_task_profile(task) if task else {}
    plan_ref = profile.get("plan_ref", current_active_plan(task))
    task_source = "matrix" if profile.get("Name") else ""
    if not task_source and task and task_ledger_status(task):
        task_source = "ledger"
    summary_parts = [
        f"Strict-mode preflight for {task or 'unbound task'} classified as {task_class}.",
        f"Loaded {len(digest)} authority sources from {AUTHORITY_ENTRY[0]} through {AUTHORITY_ENTRY[-1]}.",
    ]
    if task:
        source_label = task_source or "runtime"
        summary_parts.append(f"Task profile source: {source_label}.")
    context: Dict[str, Any] = {
        "version": 2,
        "task_id": task,
        "task_class": task_class,
        "summary": " ".join(summary_parts),
        "authority_entry": AUTHORITY_ENTRY,
        "authority_digest": digest,
        "prompt_excerpt": compact(prompt or ""),
        "requires_ledger": requires_ledger(task_class),
        "task_profile": {},
        "source_refs": {
            "implementation_readiness": relative_path(IMPLEMENTATION_READINESS_PATH),
            "task_spec_matrix": relative_path(TASK_SPEC_PATH),
            "task_governance_matrix": relative_path(TASK_GOV_PATH),
            "master_execution_plan": relative_path(MASTER_PLAN_PATH),
            "plan_ref": plan_ref or "",
        },
        "generated_at": now_iso(),
    }
    if profile:
        context["task_profile"] = {
            "task_id": task,
            "name": profile.get("Name") or profile.get("ledger_title", ""),
            "phase": profile.get("__phase", ""),
            "phase_heading": profile.get("phase_heading", ""),
            "epic_heading": profile.get("epic_heading", ""),
            "story_heading": profile.get("story_heading", ""),
            "contract": profile.get("Contract", ""),
            "rules": profile.get("Rules", ""),
            "context_aliases": profile.get("Context", ""),
            "tests": profile.get("Tests", ""),
            "dependencies": profile.get("Deps", ""),
            "environment": profile.get("Env", ""),
            "human_confirmation_point": profile.get("Human confirmation point", ""),
            "data_impact": profile.get("Data impact", ""),
            "rollback_recovery": profile.get("Rollback / recovery", ""),
            "plan_ref": plan_ref or "",
        }
    return context


def idle_current_task_state() -> Dict[str, Any]:
    return {
        "version": CURRENT_TASK_SCHEMA_VERSION,
        "task_id": "",
        "task_class": "none",
        "phase": "idle",
        "status": "idle",
        "plan_ref": "",
        "bound_to_ledger": False,
        "strict_mode": True,
        "requires_human_decision": False,
        "delivery_scope": False,
        "preflight": {
            "completed": False,
            "timestamp": "",
            "context_hash": "",
        },
        "validation": {
            "last_run_at": "",
            "passed": False,
            "evidence_refs": [],
        },
        "closeout": {
            "done_ready": False,
            "archived": False,
            "archived_at": "",
            "pre_closeout_audit_passed": False,
            "commit_created": False,
            "commit_sha": "",
            "post_closeout_audit_passed": False,
            "delivery_writeback_completed": False,
            "cleanup_completed": False,
        },
        "traceability": {
            "tasks_md_ref": "",
            "inbox_ref": "",
            "done_md_ref": "",
        },
        "last_task": {},
        "updated_at": now_iso(),
    }


def runtime_state_for_task(task_id: str, task_class: str, phase: str, plan_ref: str = "") -> Dict[str, Any]:
    state = idle_current_task_state()
    state.update(
        {
            "task_id": task_id,
            "task_class": task_class,
            "phase": phase,
            "status": "blocked" if phase == "blocked" else "active",
            "plan_ref": plan_ref,
            "bound_to_ledger": bool(task_id and requires_ledger(task_class)),
            "delivery_scope": task_class == "delivery",
            "updated_at": now_iso(),
        }
    )
    state["preflight"]["completed"] = phase != "idle"
    state["preflight"]["timestamp"] = now_iso() if phase != "idle" else ""
    state["traceability"]["tasks_md_ref"] = f"tasks.md#{task_id}" if task_id and requires_ledger(task_class) else ""
    return state


def save_session_context(context: Dict[str, Any]) -> None:
    write_json(SESSION_CONTEXT_PATH, context)


def write_runtime_state(state: Dict[str, Any]) -> None:
    state["updated_at"] = now_iso()
    write_json(CURRENT_TASK_PATH, state)


def clear_runtime_after_closeout(
    task_id: str,
    task_class: str,
    commit_subject: str,
    commit_sha: str,
    done_md_ref: str,
    plan_ref: str,
    delivery_writeback_completed: bool,
) -> None:
    current = idle_current_task_state()
    current["last_task"] = {
        "task_id": task_id,
        "task_class": task_class,
        "commit_subject": commit_subject,
        "commit_sha": commit_sha,
        "done_md_ref": done_md_ref,
        "plan_ref": plan_ref,
        "closed_at": now_iso(),
        "delivery_writeback_completed": delivery_writeback_completed,
    }
    current["closeout"]["cleanup_completed"] = True
    write_runtime_state(current)
    save_session_context(
        {
            "version": 2,
            "task_id": "",
            "task_class": "none",
            "summary": f"No active task. Last closed task {task_id} committed as `{commit_subject}`.",
            "authority_entry": AUTHORITY_ENTRY,
            "authority_digest": authority_digest(AUTHORITY_ENTRY[:5]),
            "prompt_excerpt": "",
            "requires_ledger": False,
            "task_profile": {},
            "source_refs": {
                "plan_ref": plan_ref,
                "done_md_ref": done_md_ref,
            },
            "generated_at": now_iso(),
        }
    )


def command_preflight(args: argparse.Namespace) -> int:
    profile = lookup_task_profile(args.task) if args.task else {}
    task_class = classify_task(args.prompt or "", task_id=args.task or "", explicit=args.task_class, profile=profile)
    context = build_preflight_context(args.prompt or "", args.task or "", task_class)
    save_session_context(context)
    state = runtime_state_for_task(
        args.task or "",
        task_class,
        "preflight",
        plan_ref=context["source_refs"].get("plan_ref", ""),
    )
    state["preflight"]["context_hash"] = "sha256:" + hash_payload(context)
    write_runtime_state(state)
    print(context["summary"])
    return 0


def insert_in_progress_block(content: str, block: str) -> str:
    marker = "## In Progress\n"
    start = content.find(marker)
    if start == -1:
        raise SystemExit("tasks.md is missing the '## In Progress' section.")
    start += len(marker)
    end = content.find("\n## ", start)
    if end == -1:
        end = len(content)
    section = content[start:end]
    replacement = "\n" + block.strip() + "\n\n"
    if "_No tasks._" in section:
        section = section.replace("_No tasks._", replacement.strip())
    else:
        section = replacement + section.lstrip("\n")
    return content[:start] + section + content[end:]


def instantiate_preview(task_id: str, profile: Dict[str, Any], args: argparse.Namespace) -> Dict[str, Any]:
    if profile:
        title = args.title or profile.get("Name") or profile.get("ledger_title") or task_id
        task_class = classify_task(args.scope or title, task_id=task_id, explicit=args.task_class, profile=profile)
        depends_on = args.depends_on or profile.get("Deps") or "N/A"
        scope = args.scope or compact(
            " ".join(
                [
                    profile.get("Contract", ""),
                    f"Tech: {profile.get('Tech', '')}." if profile.get("Tech") else "",
                    f"Layer: {profile.get('Layer', '')}." if profile.get("Layer") else "",
                ]
            ),
            400,
        )
        validation = [
            profile.get("Tests", ""),
            f"python3 scripts/foreman.py validate {task_id}",
        ]
        return {
            "task_id": task_id,
            "title": title,
            "task_class": task_class,
            "priority": args.priority or "1",
            "depends_on": depends_on,
            "scope": scope,
            "validation": [item for item in validation if item],
            "human_confirmation_point": profile.get("Human confirmation point", ""),
            "data_impact": profile.get("Data impact", ""),
            "rollback_recovery": profile.get("Rollback / recovery", ""),
            "plan_context": {
                "phase": profile.get("__phase", ""),
                "phase_heading": profile.get("phase_heading", ""),
                "epic_heading": profile.get("epic_heading", ""),
                "story_heading": profile.get("story_heading", ""),
                "plan_ref": profile.get("plan_ref", ""),
            },
        }

    if not args.title or not args.scope:
        raise SystemExit("Tasks outside the plan matrices require both --title and --scope.")

    task_class = classify_task(args.scope, task_id=task_id, explicit=args.task_class, profile={})
    return {
        "task_id": task_id,
        "title": args.title,
        "task_class": task_class,
        "priority": args.priority or "1",
        "depends_on": args.depends_on or "N/A",
        "scope": args.scope,
        "validation": [f"python3 scripts/foreman.py validate {task_id}"],
        "human_confirmation_point": "",
        "data_impact": "",
        "rollback_recovery": "",
        "plan_context": {"phase": "", "phase_heading": "", "epic_heading": "", "story_heading": "", "plan_ref": ""},
    }


def render_task_block(preview: Dict[str, Any]) -> str:
    lines = [
        f"### {preview['task_id']}: {preview['title']}",
        "",
        "- Status: in_progress",
        f"- Priority: {preview['priority']}",
        f"- Depends on: {preview['depends_on']}",
        f"- Scope: {preview['scope']}",
    ]
    if preview["plan_context"]["plan_ref"]:
        lines.append(f"- Plan ref: {preview['plan_context']['plan_ref']}")
    if preview["plan_context"]["phase"] or preview["plan_context"]["story_heading"]:
        phase_label = " / ".join(
            item
            for item in [
                preview["plan_context"]["phase"],
                preview["plan_context"]["story_heading"],
            ]
            if item
        )
        lines.append(f"- Matrix context: {phase_label}")
    if preview["human_confirmation_point"]:
        lines.append(f"- Human confirmation point: {preview['human_confirmation_point']}")
    if preview["data_impact"]:
        lines.append(f"- Data impact: {preview['data_impact']}")
    if preview["rollback_recovery"]:
        lines.append(f"- Rollback / recovery: {preview['rollback_recovery']}")
    lines.append("- Validation:")
    for item in preview["validation"]:
        lines.append(f"  - `{item}`" if " " in item and not item.startswith("`") else f"  - {item}")
    lines.extend(
        [
            "- Progress log:",
            f"  - {today_iso()}: instantiated from foreman CLI using repository truth and task matrices.",
            "",
        ]
    )
    return "\n".join(lines)


def command_instantiate(args: argparse.Namespace) -> int:
    if task_exists_anywhere(args.task_id):
        raise SystemExit(f"Task {args.task_id} already exists in the ledgers.")
    preview = instantiate_preview(args.task_id, lookup_task_profile(args.task_id), args)
    if args.dry_run:
        sys.stdout.write(json.dumps(preview, ensure_ascii=True, indent=2) + "\n")
        return 0
    content = read_text(TASKS_PATH)
    write_text(TASKS_PATH, insert_in_progress_block(content, render_task_block(preview)))
    state = runtime_state_for_task(
        args.task_id,
        preview["task_class"],
        "discovery",
        plan_ref=preview["plan_context"]["plan_ref"],
    )
    write_runtime_state(state)
    print(f"Instantiated {args.task_id} into tasks.md")
    return 0


def command_sync_context(args: argparse.Namespace) -> int:
    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    if args.phase:
        state["phase"] = args.phase
        if args.phase == "idle":
            state["status"] = "idle"
        else:
            state["status"] = "blocked" if args.phase == "blocked" else "active"
    if args.done_ready:
        state["phase"] = "done_ready"
        state["status"] = "active"
        state.setdefault("closeout", {})["done_ready"] = True
    write_runtime_state(state)
    print(f"Updated runtime context for {state.get('task_id') or 'no active task'}")
    return 0


def append_validation_log(label: str, command: str, rules: str, status: str) -> None:
    line = f"{now_iso()} | {label} | {rules} | {status} | `{command}`\n"
    with VALIDATION_LOG_PATH.open("a", encoding="utf-8") as handle:
        handle.write(line)


def validate_command(command: List[str], label: str, rules: str) -> str | None:
    result = run(command)
    command_text = " ".join(command)
    append_validation_log(label, command_text, rules, "passed" if result.code == 0 else "failed")
    if result.code == 0:
        return None
    return f"{command_text}\n{result.stderr.strip() or result.stdout.strip()}"


def command_validate(args: argparse.Namespace) -> int:
    hook_files = sorted(str(path.relative_to(ROOT)) for path in (CODEX_DIR / "hooks").glob("*.py"))
    commands: List[tuple[List[str], str]] = [
        (["python3", "-m", "py_compile", "scripts/foreman.py", *hook_files], "`R-133`, `R-168`"),
        (["node", "scripts/lint-repository-knowledge.js"], "`R-131`, `R-133`"),
    ]
    if args.task.startswith("HARN-"):
        commands.append((["python3", "scripts/validate_codex_runtime.py"], "`R-133`, `R-168`"))
        commands.append((["python3", "scripts/foreman.py", "compile-governance", "--check"], "`R-133`, `R-168`"))
    if args.include_task_audit:
        commands.append((["python3", "scripts/task_audit.py", "--check", "--phase", "pre-closeout"], "`R-156`, `R-160`, `R-168`"))
    for command_text in args.extra_command:
        commands.append((shlex.split(command_text), "`R-133`, `R-168`"))

    failures: List[str] = []
    for command, rules in commands:
        failure = validate_command(command, f"{args.task} validate", rules)
        if failure:
            failures.append(failure)

    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    if state.get("task_id") == args.task:
        state["validation"]["last_run_at"] = now_iso()
        state["validation"]["passed"] = not failures
        state["validation"]["evidence_refs"] = ["docs/quality/validation-log.md"]
        state["phase"] = "validation" if failures else "done_ready"
        state["status"] = "active"
        state["closeout"]["done_ready"] = not failures
        write_runtime_state(state)

    if failures:
        raise SystemExit("\n\n".join(failures))
    print(f"Validation passed for {args.task}")
    return 0


def command_audit(args: argparse.Namespace) -> int:
    result = run(["python3", "scripts/task_audit.py", "--check", "--phase", args.phase])
    sys.stdout.write(result.stdout)
    sys.stderr.write(result.stderr)
    return result.code


def active_exec_plans() -> List[str]:
    return [relative_path(path) for path in sorted(ACTIVE_EXEC_PLAN_DIR.glob("*.md"))]


def completed_exec_plans() -> List[str]:
    return [relative_path(path) for path in sorted(COMPLETED_EXEC_PLAN_DIR.glob("*.md"))]


def current_task_schema_payload() -> Dict[str, Any]:
    return {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "title": "SQLForge Codex current-task state",
        "type": "object",
        "required": [
            "version",
            "task_id",
            "task_class",
            "phase",
            "status",
            "bound_to_ledger",
            "strict_mode",
            "requires_human_decision",
            "delivery_scope",
            "preflight",
            "validation",
            "closeout",
            "traceability",
            "updated_at",
        ],
        "properties": {
            "version": {"type": "integer", "minimum": 1},
            "task_id": {"type": "string"},
            "task_class": {"type": "string", "enum": TASK_CLASSES},
            "phase": {"type": "string", "enum": TASK_PHASES},
            "status": {"type": "string", "enum": ["idle", "active", "blocked", "closed"]},
            "plan_ref": {"type": "string"},
            "bound_to_ledger": {"type": "boolean"},
            "strict_mode": {"type": "boolean"},
            "requires_human_decision": {"type": "boolean"},
            "delivery_scope": {"type": "boolean"},
            "preflight": {"type": "object"},
            "validation": {"type": "object"},
            "closeout": {"type": "object"},
            "traceability": {"type": "object"},
            "last_task": {"type": "object"},
            "updated_at": {"type": "string"},
        },
    }


def compiled_payloads() -> Dict[str, Dict[str, Any]]:
    blueprint = read_text(BLUEPRINT_PATH)
    payloads: Dict[str, Dict[str, Any]] = {
        "authority-map.json": {
            "metadata": {"blueprint_sha256": hash_text(blueprint)},
            "source": "docs/README.md",
            "entries": AUTHORITY_ENTRY,
            "authority_digest": authority_digest(AUTHORITY_ENTRY[:5]),
        },
        "task-policy.json": {
            "metadata": {"blueprint_sha256": hash_text(blueprint)},
            "task_classes": TASK_POLICY,
        },
        "closeout-policy.json": {
            "metadata": {"blueprint_sha256": hash_text(blueprint)},
            "task_closeout_steps": [
                "validate",
                "write_context_closeout",
                "archive_task",
                "task_audit_pre_closeout",
                "single_task_commit",
                "task_audit_post_closeout",
                "runtime_cleanup",
            ],
            "delivery_closeout_extra_steps": ["tag", "writeback_delivery_records"],
            "stage_scope_rule": "closeout requires explicit --stage-path file arguments",
        },
        "source-anchors.json": {
            "metadata": {"blueprint_sha256": hash_text(blueprint)},
            "blueprint": relative_path(BLUEPRINT_PATH),
            "master_execution_plan": relative_path(MASTER_PLAN_PATH),
            "task_spec_matrix": relative_path(TASK_SPEC_PATH),
            "task_governance_matrix": relative_path(TASK_GOV_PATH),
            "active_exec_plans": active_exec_plans(),
            "completed_exec_plans": completed_exec_plans(),
            "authority_entry": AUTHORITY_ENTRY,
        },
        "current-task.schema.json": current_task_schema_payload(),
    }
    return payloads


def command_compile_governance(args: argparse.Namespace) -> int:
    POLICY_DIR.mkdir(parents=True, exist_ok=True)
    mismatches: List[str] = []
    for filename, payload in compiled_payloads().items():
        target = POLICY_DIR / filename
        rendered = json.dumps(payload, ensure_ascii=True, indent=2) + "\n"
        if args.check:
            if not target.exists() or read_text(target) != rendered:
                mismatches.append(relative_path(target))
        else:
            write_text(target, rendered)
    if args.check:
        if mismatches:
            raise SystemExit("Governance policy drift detected:\n- " + "\n- ".join(mismatches))
        print("Governance policy files are in sync.")
        return 0
    print(f"Compiled governance policy files into {relative_path(POLICY_DIR)}")
    return 0


def ensure_single_line_field(lines: List[str], field_prefix: str, rendered_line: str, after_prefixes: Sequence[str]) -> None:
    for index, line in enumerate(lines):
        if line.startswith(field_prefix):
            lines[index] = rendered_line
            return
    insert_at = 1
    for prefix in after_prefixes:
        for index, line in enumerate(lines):
            if line.startswith(prefix):
                insert_at = index + 1
    lines.insert(insert_at, rendered_line)


def ensure_context_closeout(body: str, args: argparse.Namespace) -> str:
    required = {
        "Completed scope:": args.completed_scope,
        "Validation evidence:": args.validation_evidence,
        "Residual risk:": args.residual_risk,
        "Next step:": args.next_step,
    }
    if "Context closeout:" not in body and any(not value for value in required.values()):
        raise SystemExit(
            "closeout requires Context closeout content. Provide --completed-scope, "
            "--validation-evidence, --residual-risk, and --next-step when the block does not already contain them."
        )
    if "Context closeout:" not in body:
        body = body.rstrip() + "\n- Context closeout:\n"
    for marker, value in required.items():
        if marker not in body:
            if not value:
                raise SystemExit(f"closeout is missing required marker '{marker}' and no CLI value was provided.")
            body = body.rstrip() + f"\n  - {marker} {value}\n"
    return body.rstrip() + "\n"


def archive_task_block(task_id: str, args: argparse.Namespace) -> tuple[str, str, str]:
    tasks_content = read_text(TASKS_PATH)
    block = find_task_block(TASKS_PATH, task_id)
    if block is None:
        raise SystemExit(f"Task {task_id} was not found in tasks.md.")
    body = block["body"]
    commit_subject = commit_subject_of(body) or (args.commit_subject or "")
    if not commit_subject:
        raise SystemExit("closeout requires a commit subject either in the task block or via --commit-subject.")

    lines = body.splitlines()
    ensure_single_line_field(lines, "- Status:", "- Status: done", ["- Depends on:", "- Priority:"])
    ensure_single_line_field(lines, "- Completed at:", f"- Completed at: {today_iso()}", ["- Depends on:", "- Priority:", "- Status:"])
    ensure_single_line_field(lines, "- Commit subject:", f"- Commit subject: `{commit_subject}`", ["- Completed at:"])
    archived_body = ensure_context_closeout("\n".join(lines).rstrip() + "\n", args)

    new_tasks_content = tasks_content[: block["start"]] + tasks_content[block["end"] :]
    new_tasks_content = normalize_tasks_md(new_tasks_content)
    new_done_content = prepend_done_block(read_text(TASKS_DONE_PATH), archived_body)
    write_text(TASKS_PATH, new_tasks_content)
    write_text(TASKS_DONE_PATH, new_done_content)
    return archived_body, relative_path(TASKS_PATH), relative_path(TASKS_DONE_PATH)


def normalize_tasks_md(content: str) -> str:
    normalized = content
    for heading in TASK_STATUS_SECTIONS:
        marker = f"## {heading}\n"
        start = normalized.find(marker)
        if start == -1:
            continue
        start += len(marker)
        end = len(normalized)
        for next_heading in TASK_STATUS_SECTIONS:
            if next_heading == heading:
                continue
            candidate = normalized.find(f"\n## {next_heading}", start)
            if candidate != -1:
                end = min(end, candidate)
        section = normalized[start:end]
        has_task = "### " in section
        if has_task:
            section = "\n" + section.strip() + "\n\n"
        else:
            section = "\n_No tasks._\n\n"
        normalized = normalized[:start] + section + normalized[end:]
    return normalized.rstrip() + "\n"


def prepend_done_block(content: str, block: str) -> str:
    marker = "## Done\n"
    start = content.find(marker)
    if start == -1:
        raise SystemExit("tasks-done.md is missing the '## Done' section.")
    start += len(marker)
    end = len(content)
    replacement = "\n" + block.strip() + "\n\n"
    section = content[start:end]
    if "_No tasks._" in section:
        section = section.replace("_No tasks._", replacement.strip())
    else:
        section = replacement + section.lstrip("\n")
    return content[:start] + section


def move_active_plan_to_completed(task_id: str) -> tuple[str, str] | None:
    active_path_text = current_active_plan(task_id)
    if not active_path_text:
        return None
    active_path = ROOT / active_path_text
    if not active_path.exists():
        return None
    completed_path = COMPLETED_EXEC_PLAN_DIR / active_path.name
    completed_path.parent.mkdir(parents=True, exist_ok=True)
    active_path.rename(completed_path)
    return relative_path(active_path), relative_path(completed_path)


def resolved_completed_plan_ref(task_id: str, fallback: str) -> str:
    completed_match = COMPLETED_EXEC_PLAN_DIR / f"{task_id}-governance-runtime-hardening-plan.md"
    if completed_match.exists():
        return relative_path(completed_match)
    generic_matches = sorted(COMPLETED_EXEC_PLAN_DIR.glob(f"{task_id}-*.md"))
    if generic_matches:
        return relative_path(generic_matches[0])
    if fallback:
        fallback_path = ROOT / fallback
        if fallback_path.exists():
            return fallback
    return ""


def run_and_log(command: List[str], label: str, rules: str) -> None:
    result = run(command)
    append_validation_log(label, " ".join(command), rules, "passed" if result.code == 0 else "failed")
    if result.code != 0:
        raise SystemExit(result.stderr.strip() or result.stdout.strip() or "command failed")


def validate_stage_paths(paths: Sequence[str]) -> List[str]:
    if not paths:
        raise SystemExit("closeout requires at least one explicit --stage-path.")
    validated: List[str] = []
    for raw in paths:
        candidate = raw.strip()
        if not candidate:
            continue
        if candidate.startswith("/"):
            absolute = Path(candidate).resolve()
            try:
                relative = absolute.relative_to(ROOT)
            except ValueError as exc:
                raise SystemExit(f"Stage path {candidate} is outside the repository root.") from exc
            candidate = str(relative)
        target = ROOT / candidate
        if target.exists() and target.is_dir():
            raise SystemExit(f"Stage path {candidate} is a directory; closeout requires explicit file paths.")
        validated.append(candidate)
    if not validated:
        raise SystemExit("closeout did not receive any valid explicit file paths.")
    return validated


def git_add_paths(paths: Sequence[str]) -> None:
    command = ["git", "add", "--", *paths]
    result = run(command)
    if result.code != 0:
        raise SystemExit(result.stderr.strip() or result.stdout.strip() or "git add failed")


def git_commit_subject(subject: str) -> str:
    result = run(["git", "commit", "-m", subject])
    if result.code != 0:
        raise SystemExit(result.stderr.strip() or result.stdout.strip() or "git commit failed")
    return run_or_raise(["git", "rev-parse", "HEAD"]).stdout.strip()


def command_closeout(args: argparse.Namespace) -> int:
    validated_stage_paths = validate_stage_paths(args.stage_path)
    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    task_class = state.get("task_class", "standard")
    if state.get("task_id") == args.task:
        state["phase"] = "closeout"
        state["status"] = "active"
        state["closeout"]["done_ready"] = True
        write_runtime_state(state)

    archived_body, tasks_ref, done_ref = archive_task_block(args.task, args)
    moved_plan = move_active_plan_to_completed(args.task)
    pre_audit_label = f"{args.task} closeout task-audit pre"
    run_and_log(
        ["python3", "scripts/task_audit.py", "--check", "--phase", "pre-closeout"],
        pre_audit_label,
        "`R-156`, `R-160`, `R-168`",
    )

    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    if state.get("task_id") == args.task:
        state["closeout"]["archived"] = True
        state["closeout"]["archived_at"] = now_iso()
        state["closeout"]["pre_closeout_audit_passed"] = True
        state["traceability"]["done_md_ref"] = f"tasks-done.md#{args.task}"
        write_runtime_state(state)

    stage_paths = list(validated_stage_paths) + [tasks_ref, done_ref]
    if moved_plan is not None:
        stage_paths.extend([moved_plan[0], moved_plan[1]])
    deduped_stage_paths = list(dict.fromkeys(stage_paths))
    git_add_paths(deduped_stage_paths)

    commit_subject = commit_subject_of(archived_body)
    commit_sha = git_commit_subject(commit_subject)
    append_validation_log(f"{args.task} closeout commit", commit_sha, "`R-168`", "passed")

    post_audit_label = f"{args.task} post-closeout task-audit"
    run_and_log(
        ["python3", "scripts/task_audit.py", "--check", "--phase", "post-closeout"],
        post_audit_label,
        "`R-156`, `R-160`, `R-168`",
    )
    for command_text in args.post_check:
        run_and_log(shlex.split(command_text), f"{args.task} post-closeout check", "`R-131`, `R-133`, `R-168`")

    final_plan_ref = moved_plan[1] if moved_plan is not None else resolved_completed_plan_ref(args.task, state.get("plan_ref", ""))
    clear_runtime_after_closeout(
        args.task,
        task_class if task_class in TASK_CLASSES else "standard",
        commit_subject,
        commit_sha,
        f"tasks-done.md#{args.task}",
        final_plan_ref,
        delivery_writeback_completed=False,
    )
    print(f"Closeout completed for {args.task} at {commit_sha}")
    return 0


def command_delivery_closeout(args: argparse.Namespace) -> int:
    profile = lookup_task_profile(args.target)
    task_class = classify_task("", task_id=args.target, explicit=None, profile=profile)
    if task_class != "delivery":
        raise SystemExit(f"delivery-closeout requires a delivery-class task; inferred class for {args.target} is {task_class}.")
    if args.dry_run:
        payload = {
            "target": args.target,
            "tag": args.tag,
            "tag_message": args.tag_message or f"Delivery closeout for {args.target}",
            "writeback_file": args.writeback_file or "",
            "writeback_text": args.writeback_text or "",
        }
        sys.stdout.write(json.dumps(payload, ensure_ascii=True, indent=2) + "\n")
        return 0
    if not args.tag:
        raise SystemExit("delivery-closeout requires --tag unless --dry-run is used.")
    tag_message = args.tag_message or f"Delivery closeout for {args.target}"
    run_or_raise(["git", "tag", "-a", args.tag, "-m", tag_message])
    append_validation_log(f"{args.target} delivery tag", f"git tag -a {args.tag} -m {tag_message}", "`R-012`, `R-117`", "passed")
    if args.writeback_file:
        target = ROOT / args.writeback_file
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("a", encoding="utf-8") as handle:
            handle.write((args.writeback_text or f"{today_iso()} | {args.target} | {args.tag}") + "\n")
        append_validation_log(
            f"{args.target} delivery writeback",
            f"append {args.writeback_file}",
            "`R-012`, `R-117`",
            "passed",
        )
    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    last_task = state.get("last_task", {})
    if state.get("task_id") == args.target:
        state["closeout"]["delivery_writeback_completed"] = True
        write_runtime_state(state)
    elif last_task.get("task_id") == args.target:
        last_task["delivery_writeback_completed"] = True
        last_task["delivery_tag"] = args.tag
        last_task["delivery_writeback_file"] = args.writeback_file or ""
        last_task["delivery_writeback_at"] = now_iso()
        state["last_task"] = last_task
        write_runtime_state(state)
    print(f"Delivery closeout completed for {args.target}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="SQLForge foreman / Codex governance entrypoint.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    preflight = subparsers.add_parser("preflight")
    preflight.add_argument("--prompt", default="")
    preflight.add_argument("--task", default="")
    preflight.add_argument("--task-class", choices=TASK_CLASSES[1:])
    preflight.set_defaults(func=command_preflight)

    instantiate = subparsers.add_parser("instantiate")
    instantiate.add_argument("task_id")
    instantiate.add_argument("--title")
    instantiate.add_argument("--task-class", choices=TASK_CLASSES[1:])
    instantiate.add_argument("--priority")
    instantiate.add_argument("--depends-on")
    instantiate.add_argument("--scope")
    instantiate.add_argument("--dry-run", action="store_true")
    instantiate.set_defaults(func=command_instantiate)

    sync_context = subparsers.add_parser("sync-context")
    sync_context.add_argument("--phase", choices=TASK_PHASES)
    sync_context.add_argument("--done-ready", action="store_true")
    sync_context.set_defaults(func=command_sync_context)

    validate = subparsers.add_parser("validate")
    validate.add_argument("task")
    validate.add_argument("--include-task-audit", action="store_true")
    validate.add_argument("--extra-command", action="append", default=[])
    validate.set_defaults(func=command_validate)

    audit = subparsers.add_parser("audit")
    audit.add_argument("--phase", choices=["pre-closeout", "post-closeout"], default="pre-closeout")
    audit.set_defaults(func=command_audit)

    compile_governance = subparsers.add_parser("compile-governance")
    compile_governance.add_argument("--check", action="store_true")
    compile_governance.set_defaults(func=command_compile_governance)

    closeout = subparsers.add_parser("closeout")
    closeout.add_argument("task")
    closeout.add_argument("--commit-subject")
    closeout.add_argument("--completed-scope")
    closeout.add_argument("--validation-evidence")
    closeout.add_argument("--residual-risk")
    closeout.add_argument("--next-step")
    closeout.add_argument("--stage-path", action="append", default=[])
    closeout.add_argument("--post-check", action="append", default=[])
    closeout.set_defaults(func=command_closeout)

    delivery_closeout = subparsers.add_parser("delivery-closeout")
    delivery_closeout.add_argument("target")
    delivery_closeout.add_argument("--tag")
    delivery_closeout.add_argument("--tag-message")
    delivery_closeout.add_argument("--writeback-file")
    delivery_closeout.add_argument("--writeback-text")
    delivery_closeout.add_argument("--dry-run", action="store_true")
    delivery_closeout.set_defaults(func=command_delivery_closeout)

    return parser


def main() -> int:
    args = build_parser().parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
