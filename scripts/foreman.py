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
CLOSEOUT_DIR = STATE_DIR / "closeout"
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
MCP_RULE_IDS = ["R-170", "R-171", "R-172", "R-173", "R-174", "R-175", "R-176"]
MCP_VALIDATION_RULE_IDS = ["R-170", "R-171", "R-172", "R-173", "R-174", "R-175", "R-176"]
MCP_POLICY_DOCS = [
    "docs/security/connectors.md",
    "docs/operations/codex-mcp-playbook.md",
    "docs/operations/multi-agent-playbook.md",
    "docs/README.md",
    "docs/operations/README.md",
]
MCP_ALLOWED_CATEGORIES = [
    {
        "id": "observability_logs",
        "label": "观测/日志",
        "scope": "read-only",
        "allowed_operations": ["list", "search", "read", "tail", "fetch-metadata"],
        "forbidden_operations": ["ack", "silence", "close-alert", "delete", "change-retention"],
        "evidence_examples": ["告警上下文", "日志检索结果", "指标快照元数据"],
    },
    {
        "id": "deployment_evidence",
        "label": "部署证据",
        "scope": "read-only",
        "allowed_operations": ["list", "read", "inspect-status", "fetch-metadata"],
        "forbidden_operations": ["deploy", "rollback", "approve", "promote", "delete"],
        "evidence_examples": ["发布状态", "构建产物元数据", "发布日志"],
    },
    {
        "id": "object_storage_metadata",
        "label": "对象存储元数据",
        "scope": "read-only",
        "allowed_operations": ["list", "head", "read-metadata", "inspect-version-history"],
        "forbidden_operations": ["upload", "delete", "restore", "retag", "change-retention"],
        "evidence_examples": ["bucket/prefix 列表", "object head 元数据", "版本信息"],
    },
    {
        "id": "external_requirements_tickets",
        "label": "外部需求/工单检索",
        "scope": "read-only",
        "allowed_operations": ["search", "list", "read", "download-readonly-attachment"],
        "forbidden_operations": ["create", "comment", "transition", "assign", "close"],
        "evidence_examples": ["需求单内容", "工单状态", "附件只读副本"],
    },
]
MCP_FORBIDDEN_SERVER_TYPES = [
    "write-capable cloud control",
    "ssh",
    "k8s exec / apply / rollout control",
    "database execution",
    "ticket mutation / workflow transition",
    "object upload / delete / restore",
]
MCP_AUTOMATION_ENTRYPOINTS = [
    "python3 scripts/foreman.py compile-governance",
    "python3 scripts/validate_codex_runtime.py",
    "python3 scripts/foreman.py validate <TASK_ID>",
    "bash scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest <FILE>",
    "bash scripts/multi_agent_launch.sh --manifest <FILE>",
    "bash scripts/multi_agent_collect.sh --manifest <FILE>",
]

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
    section_matches = list(re.finditer(r"^##\s+.+$", content, re.MULTILINE))
    blocks: List[Dict[str, Any]] = []
    for position, match in enumerate(matches):
        start = match.start()
        end_candidates = [len(content)]
        if position + 1 < len(matches):
            end_candidates.append(matches[position + 1].start())
        for section_match in section_matches:
            if section_match.start() > start:
                end_candidates.append(section_match.start())
                break
        end = min(end_candidates)
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


def tracked_status_short() -> List[str]:
    result = run(["git", "status", "--short", "--untracked-files=no"])
    if result.code != 0:
        raise SystemExit(result.stderr.strip() or result.stdout.strip() or "git status failed")
    return [line for line in result.stdout.splitlines() if line.strip()]


def validate_command(command: List[str], label: str, rules: str) -> str | None:
    result = run(command)
    command_text = " ".join(command)
    append_validation_log(label, command_text, rules, "passed" if result.code == 0 else "failed")
    if result.code == 0:
        return None
    return f"{command_text}\n{result.stderr.strip() or result.stdout.strip()}"


FRONTEND_PAGE_VALIDATE_PATTERN = re.compile(r"^(src/(views|components)/.*\.vue|src/locales/)")


def validation_changed_paths() -> List[str]:
    paths: set[str] = set()
    for command in [
        ["git", "diff", "--name-only"],
        ["git", "diff", "--cached", "--name-only"],
        ["git", "ls-files", "--others", "--exclude-standard"],
    ]:
        result = run(command)
        if result.code == 0:
            paths.update(line.strip() for line in result.stdout.splitlines() if line.strip())
    return sorted(paths)


def frontend_page_validation_touched() -> bool:
    return any(FRONTEND_PAGE_VALIDATE_PATTERN.match(path) for path in validation_changed_paths())


def command_validate(args: argparse.Namespace) -> int:
    hook_files = sorted(str(path.relative_to(ROOT)) for path in (CODEX_DIR / "hooks").glob("*.py"))
    commands: List[tuple[List[str], str]] = [
        (["python3", "-m", "py_compile", "scripts/foreman.py", *hook_files], "`R-133`, `R-168`"),
        (["node", "scripts/lint-repository-knowledge.js"], "`R-131`, `R-133`"),
    ]
    if args.task.startswith("HARN-"):
        commands.append((["python3", "scripts/validate_codex_runtime.py"], "`R-133`, `R-168`"))
        commands.append((["python3", "scripts/foreman.py", "compile-governance", "--check"], "`R-133`, `R-168`"))
    if frontend_page_validation_touched():
        commands.extend(
            [
                (["npm", "run", "lint"], "`R-124`, `R-184`"),
                (["npm", "run", "build"], "`R-124`, `R-184`"),
                (["npm", "run", "test:form-governance"], "`R-180`, `R-184`"),
                (["npm", "run", "test:sql-ui-contract"], "`R-180`, `R-184`"),
                (["npm", "run", "test:frontend-page-governance"], "`R-177`, `R-178`, `R-179`, `R-180`, `R-181`, `R-182`, `R-183`, `R-184`"),
            ]
        )
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


def mcp_policy_payload() -> Dict[str, Any]:
    blueprint = read_text(BLUEPRINT_PATH)
    connectors_path = DOCS_DIR / "security" / "connectors.md"
    playbook_path = DOCS_DIR / "operations" / "codex-mcp-playbook.md"
    multi_agent_playbook_path = DOCS_DIR / "operations" / "multi-agent-playbook.md"
    connectors = read_text(connectors_path)
    playbook = read_text(playbook_path)
    multi_agent_playbook = read_text(multi_agent_playbook_path)
    return {
        "metadata": {
            "blueprint_sha256": hash_text(blueprint),
            "connectors_sha256": hash_text(connectors),
            "playbook_sha256": hash_text(playbook),
            "multi_agent_playbook_sha256": hash_text(multi_agent_playbook),
        },
        "scope": {
            "task_id": "HARN-035",
            "profile": "multi-agent-read-only-evidence",
            "approved_access_mode": "read-only",
            "external_evidence_requires_repo_writeback": True,
        },
        "source_anchors": {
            "policy_docs": MCP_POLICY_DOCS,
            "rulebook": relative_path(DOCS_DIR / "rules" / "codex-rules.md"),
            "validation_rulebook": relative_path(DOCS_DIR / "quality" / "validation-rules.md"),
            "local_runtime_config": relative_path(CODEX_DIR / "config.toml"),
            "multi_agent_manifest_template": relative_path(DOCS_DIR / "exec-plans" / "templates" / "multi-agent-run.template.json"),
        },
        "rules": MCP_RULE_IDS,
        "validation_rules": MCP_VALIDATION_RULE_IDS,
        "read_only_categories": MCP_ALLOWED_CATEGORIES,
        "forbidden_server_types": MCP_FORBIDDEN_SERVER_TYPES,
        "runtime_constraints": {
            "allow_repo_tracked_mcp_config": False,
            "allow_repo_tracked_mcp_profile": True,
            "allow_repo_tracked_server_inventory": False,
            "allow_repo_stored_secrets": False,
            "main_foreman_is_only_writeback_entry": True,
            "multi_agent_mcp_profile_enabled": True,
        },
        "multi_agent_contract": {
            "manifest_registry_key": "mcp_profiles",
            "per_agent_profile_key": "mcp_profile",
            "allowed_roles": ["explorer", "validator"],
            "disallowed_roles": ["worker"],
            "allowed_profile_sources": ["local-user-config", "env", "external-secret-store"],
            "local_profile_resolution_order": ["agent.profile", "agent.mcp_profile"],
        },
        "automation_entrypoints": MCP_AUTOMATION_ENTRYPOINTS,
        "human_confirmation_required_for": [
            "write-capable MCP",
            "repo-tracked MCP server inventory",
            "repo-tracked credentials or tokens",
            "worker or Main Foreman MCP execution",
            "MCP bypass of validate / closeout / write-back",
        ],
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
        "mcp-policy.json": mcp_policy_payload(),
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


def rewrite_plan_ref(body: str, move_from: str, move_to: str) -> str:
    if not move_from or not move_to:
        return body
    return body.replace(f"- Plan ref: {move_from}", f"- Plan ref: {move_to}")


def archive_task_block(
    task_id: str,
    args: argparse.Namespace,
    plan_ref_move: tuple[str, str] | None = None,
) -> tuple[str, str, str]:
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
    if plan_ref_move is not None:
        archived_body = rewrite_plan_ref(archived_body, plan_ref_move[0], plan_ref_move[1])

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


def update_document_coverage_for_exec_plan(move_from: str, move_to: str) -> None:
    coverage_path = DOCS_DIR / "plans" / "document-coverage-matrix.md"
    content = read_text(coverage_path)
    updated = content.replace(move_from, move_to)
    if updated != content:
        write_text(coverage_path, updated)


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
    move_from = relative_path(active_path)
    move_to = relative_path(completed_path)
    update_document_coverage_for_exec_plan(move_from, move_to)
    return move_from, move_to


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


def path_is_git_tracked(path: str) -> bool:
    result = run(["git", "ls-files", "--error-unmatch", "--", path])
    return result.code == 0


def filter_stageable_paths(paths: Sequence[str]) -> List[str]:
    filtered: List[str] = []
    for path in paths:
        target = ROOT / path
        if target.exists() or path_is_git_tracked(path):
            filtered.append(path)
    return filtered


def git_commit_subject(subject: str) -> str:
    result = run(["git", "commit", "-m", subject])
    if result.code != 0:
        raise SystemExit(result.stderr.strip() or result.stdout.strip() or "git commit failed")
    return run_or_raise(["git", "rev-parse", "HEAD"]).stdout.strip()


def projected_closeout_command(subject: str) -> str:
    return f"git commit -m {shlex.quote(subject)}"


def normalize_post_check_commands(task_id: str, post_checks: Sequence[str]) -> List[str]:
    normalized: List[str] = []
    for command_text in post_checks:
        tokens = shlex.split(command_text)
        if (
            len(tokens) >= 2
            and tokens[0] == "python3"
            and tokens[1] == "scripts/governed_healthcheck.py"
            and "--check" in tokens
            and "--post-closeout-task" not in tokens
        ):
            tokens.extend(["--post-closeout-task", task_id])
        normalized.append(shlex.join(tokens) if tokens else command_text)
    return normalized


def append_projected_closeout_logs(task_id: str, commit_subject: str, post_checks: Sequence[str]) -> None:
    append_validation_log(
        f"{task_id} closeout commit",
        projected_closeout_command(commit_subject) + " (projected-precommit)",
        "`R-168`",
        "projected",
    )
    append_validation_log(
        f"{task_id} post-closeout task-audit",
        "python3 scripts/task_audit.py --check --phase post-closeout (projected-precommit)",
        "`R-156`, `R-160`, `R-168`",
        "projected",
    )
    for command_text in post_checks:
        append_validation_log(
            f"{task_id} post-closeout check",
            f"{command_text} (projected-precommit)",
            "`R-131`, `R-133`, `R-168`",
            "projected",
        )


def run_without_logging(command: Sequence[str]) -> None:
    result = run(command)
    if result.code != 0:
        raise SystemExit(result.stderr.strip() or result.stdout.strip() or "command failed")


def command_result_payload(command: Sequence[str], result: CommandResult, label: str) -> Dict[str, Any]:
    return {
        "label": label,
        "command": " ".join(command),
        "exit_code": result.code,
        "status": "passed" if result.code == 0 else "failed",
        "stdout": result.stdout[-20000:],
        "stderr": result.stderr[-20000:],
        "completed_at": now_iso(),
    }


def write_post_closeout_actual_evidence(
    task_id: str,
    commit_sha: str,
    commit_subject: str,
    results: List[Dict[str, Any]],
    final_status: str,
    post_check_commands: Sequence[str],
) -> str:
    evidence_path = CLOSEOUT_DIR / task_id / "post-closeout-actual.json"
    write_json(
        evidence_path,
        {
            "task_id": task_id,
            "commit_sha": commit_sha,
            "commit_subject": commit_subject,
            "evidence_kind": "post-closeout-actual",
            "final_status": final_status,
            "post_check_commands": list(post_check_commands),
            "commands": results,
            "recorded_at": now_iso(),
        },
    )
    return relative_path(evidence_path)


def run_post_closeout_actual_checks(
    task_id: str,
    commit_sha: str,
    commit_subject: str,
    post_checks: Sequence[str],
) -> str:
    results: List[Dict[str, Any]] = []
    commands: List[tuple[str, List[str]]] = [
        ("post-closeout task-audit", ["python3", "scripts/task_audit.py", "--check", "--phase", "post-closeout"]),
    ]
    commands.extend(("post-closeout check", shlex.split(command_text)) for command_text in post_checks)

    for label, command in commands:
        result = run(command)
        results.append(command_result_payload(command, result, label))
        if result.code != 0:
            evidence_ref = write_post_closeout_actual_evidence(
                task_id,
                commit_sha,
                commit_subject,
                results,
                "failed",
                post_checks,
            )
            raise SystemExit(
                f"post-closeout actual check failed; evidence: {evidence_ref}\n"
                + (result.stderr.strip() or result.stdout.strip() or "command failed")
            )
    return write_post_closeout_actual_evidence(
        task_id,
        commit_sha,
        commit_subject,
        results,
        "passed",
        post_checks,
    )


def closeout_commit_metadata(task_id: str) -> tuple[str, str]:
    evidence_path = CLOSEOUT_DIR / task_id / "post-closeout-actual.json"
    payload = read_json(evidence_path, {})
    commit_sha = str(payload.get("commit_sha", "")).strip()
    commit_subject = str(payload.get("commit_subject", "")).strip()
    if not commit_subject:
        block = find_task_block(TASKS_DONE_PATH, task_id)
        if block is not None:
            commit_subject = commit_subject_of(block["body"]) or ""
    if not commit_sha:
        raise SystemExit(f"closeout-repair requires commit_sha in {relative_path(evidence_path)}.")
    if not commit_subject:
        raise SystemExit(f"closeout-repair requires commit_subject in {relative_path(evidence_path)} or tasks-done.md.")
    return commit_sha, commit_subject


def maybe_clear_runtime_after_closeout(
    task_id: str,
    task_class: str,
    commit_subject: str,
    commit_sha: str,
    plan_ref: str,
) -> None:
    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    if state.get("task_id") not in {"", task_id} and state.get("phase") not in {"idle", "closeout"}:
        return
    clear_runtime_after_closeout(
        task_id,
        task_class if task_class in TASK_CLASSES else "standard",
        commit_subject,
        commit_sha,
        f"tasks-done.md#{task_id}",
        plan_ref,
        delivery_writeback_completed=False,
    )


def command_closeout(args: argparse.Namespace) -> int:
    validated_stage_paths = validate_stage_paths(args.stage_path)
    state = read_json(CURRENT_TASK_PATH, default=idle_current_task_state())
    task_class = state.get("task_class", "standard")
    if state.get("task_id") == args.task:
        state["phase"] = "closeout"
        state["status"] = "active"
        state["closeout"]["done_ready"] = True
        write_runtime_state(state)

    moved_plan = move_active_plan_to_completed(args.task)
    archived_body, tasks_ref, done_ref = archive_task_block(args.task, args, moved_plan)
    if moved_plan is not None:
        command_compile_governance(argparse.Namespace(check=False))
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

    commit_subject = commit_subject_of(archived_body)
    normalized_post_checks = normalize_post_check_commands(args.task, args.post_check)
    append_projected_closeout_logs(args.task, commit_subject, normalized_post_checks)

    stage_paths = list(validated_stage_paths) + [tasks_ref, done_ref, relative_path(VALIDATION_LOG_PATH)]
    if moved_plan is not None:
        stage_paths.extend([moved_plan[0], moved_plan[1]])
    deduped_stage_paths = filter_stageable_paths(list(dict.fromkeys(stage_paths)))
    git_add_paths(deduped_stage_paths)

    commit_sha = git_commit_subject(commit_subject)
    final_plan_ref = moved_plan[1] if moved_plan is not None else resolved_completed_plan_ref(args.task, state.get("plan_ref", ""))
    try:
        post_closeout_actual_ref = run_post_closeout_actual_checks(args.task, commit_sha, commit_subject, normalized_post_checks)
    except SystemExit as exc:
        maybe_clear_runtime_after_closeout(
            args.task,
            task_class if task_class in TASK_CLASSES else "standard",
            commit_subject,
            commit_sha,
            final_plan_ref,
        )
        raise SystemExit(
            str(exc).rstrip()
            + f"\nRecovery: python3 scripts/foreman.py closeout-repair {args.task}"
        )

    residue = tracked_status_short()
    if residue:
        raise SystemExit(
            "closeout left tracked residue after commit:\n" + "\n".join(f"- {line}" for line in residue)
        )

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
    print(f"post_closeout_actual_evidence: {post_closeout_actual_ref}")
    return 0


def command_closeout_repair(args: argparse.Namespace) -> int:
    evidence_path = CLOSEOUT_DIR / args.task / "post-closeout-actual.json"
    if not evidence_path.exists():
        raise SystemExit(f"closeout-repair requires existing evidence: {relative_path(evidence_path)}")
    evidence = read_json(evidence_path, {})
    stored_post_checks = [str(item).strip() for item in evidence.get("post_check_commands", []) if str(item).strip()]
    requested_post_checks = list(args.post_check) if args.post_check else stored_post_checks
    if not requested_post_checks:
        raise SystemExit(
            "closeout-repair requires --post-check or previously recorded post_check_commands in post-closeout actual evidence."
        )

    commit_sha, commit_subject = closeout_commit_metadata(args.task)
    normalized_post_checks = normalize_post_check_commands(args.task, requested_post_checks)
    post_closeout_actual_ref = run_post_closeout_actual_checks(args.task, commit_sha, commit_subject, normalized_post_checks)

    final_plan_ref = resolved_completed_plan_ref(args.task, "")
    maybe_clear_runtime_after_closeout(args.task, "standard", commit_subject, commit_sha, final_plan_ref)
    print(f"Closeout repair completed for {args.task} at {commit_sha}")
    print(f"post_closeout_actual_evidence: {post_closeout_actual_ref}")
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

    closeout_repair = subparsers.add_parser("closeout-repair")
    closeout_repair.add_argument("task")
    closeout_repair.add_argument("--post-check", action="append", default=[])
    closeout_repair.set_defaults(func=command_closeout_repair)

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
