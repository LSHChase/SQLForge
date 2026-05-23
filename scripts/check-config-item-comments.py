#!/usr/bin/env python3
"""检查并可批量补齐配置文件配置项中文注释。"""

import argparse
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

try:
    import yaml
except ImportError:  # pragma: no cover - 依赖缺失时降级为跳过 YAML 语法检查。
    yaml = None

ROOT = Path(__file__).resolve().parents[1]

CONFIG_FILE_PATTERN = re.compile(
    r"(^|/)(Dockerfile|docker-compose.*\.ya?ml|pom\.xml|package(-lock)?\.json|vite\.config\.js|"
    r".*\.config\.(js|mjs|cjs)|.*\.ya?ml|.*\.properties|.*\.xml|.*\.toml|.*\.env|"
    r".*\.ini|.*\.conf|.*\.cfg|.*\.json5?)$"
)
EXCLUDED_PATTERN = re.compile(r"(^|/)(node_modules|dist|dist-portable|target|\.codex/state|\.codex/tmp)/")
GENERATED_MARKERS = ("配置项", "配置段", "配置列表项")


def tracked_files():
    output = subprocess.check_output(["git", "ls-files"], cwd=str(ROOT), text=True)
    return [line.strip() for line in output.splitlines() if line.strip()]


def config_files():
    return [
        path for path in tracked_files()
        if CONFIG_FILE_PATTERN.search(path) and not EXCLUDED_PATTERN.search(path)
    ]


def file_kind(path):
    if path.endswith((".yml", ".yaml")):
        return "yaml"
    if path.endswith(".toml"):
        return "toml"
    if path.endswith((".properties", ".env", ".ini", ".conf", ".cfg")):
        return "kv"
    if path.endswith((".js", ".mjs", ".cjs")):
        return "js"
    if path.endswith(".json"):
        return "json"
    if path.endswith(".xml") or path.endswith("/pom.xml") or path == "pom.xml":
        return "xml"
    return "other"


def has_previous_chinese_comment(output_lines, comment_prefix):
    for previous in reversed(output_lines):
        stripped = previous.strip()
        if not stripped:
            continue
        return stripped.startswith(comment_prefix) and any(marker in stripped for marker in GENERATED_MARKERS)
    return False


def has_inline_chinese_comment(line):
    return any(marker in line for marker in GENERATED_MARKERS)


def yaml_candidates(lines):
    block_indent = None
    for index, line in enumerate(lines):
        stripped = line.strip()
        indent = len(line) - len(line.lstrip(" "))
        if block_indent is not None:
            if stripped and indent <= block_indent:
                block_indent = None
            else:
                continue
        if not stripped or stripped.startswith("#") or stripped in {"---", "..."}:
            continue
        key_match = re.match(r"^(\s*)(?:-\s*)?([A-Za-z0-9_.-]+):(?:\s|$)", line)
        list_match = re.match(r"^(\s*)-\s+(.+)$", line)
        if key_match:
            value_tail = line.split(":", 1)[1].strip()
            if value_tail in {"|", ">"} or value_tail.startswith("|") or value_tail.startswith(">"):
                block_indent = indent
            yield index, key_match.group(1), "配置项", key_match.group(2)
            continue
        if list_match and not list_match.group(2).lstrip().startswith("#"):
            yield index, list_match.group(1), "配置列表项", "列表值"


def annotate_yaml(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in yaml_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "#"):
            indent, label, name = candidate
            output.append(f"{indent}# {label} {name}：说明该配置的用途、默认值或运行影响。")
        output.append(line)
    return output


def toml_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        table_match = re.match(r"^\s*\[+([A-Za-z0-9_.-]+)\]+\s*$", line)
        key_match = re.match(r"^(\s*)([A-Za-z0-9_.-]+)\s*=", line)
        if table_match:
            yield index, "", "配置段", table_match.group(1)
        elif key_match:
            yield index, key_match.group(1), "配置项", key_match.group(2)


def annotate_toml(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in toml_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "#"):
            indent, label, name = candidate
            output.append(f"{indent}# {label} {name}：说明该配置的用途、默认值或运行影响。")
        output.append(line)
    return output


def kv_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!", ";")):
            continue
        match = re.match(r"^(\s*)([A-Za-z0-9_.-]+)\s*[:=]", line)
        if match:
            yield index, match.group(1), "配置项", match.group(2)


def annotate_kv(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in kv_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "#"):
            indent, label, name = candidate
            output.append(f"{indent}# {label} {name}：说明该配置的用途、默认值或运行影响。")
        output.append(line)
    return output


def xml_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if (
            not stripped
            or stripped.startswith(("<!--", "<?", "</", "<!", "<!--"))
            or stripped.startswith("-->")
        ):
            continue
        match = re.match(r"^(\s*)<([A-Za-z_][A-Za-z0-9_.:-]*)\b", line)
        if match:
            descriptor = match.group(2)
            id_match = re.search(r'\b(id|name)="([^"]+)"', line)
            if id_match:
                descriptor += f" {id_match.group(1)}={id_match.group(2)}"
            yield index, match.group(1), "配置项", descriptor


def annotate_xml(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in xml_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "<!--"):
            indent, label, name = candidate
            output.append(f"{indent}<!-- {label} {name}：说明该配置的用途、默认值或运行影响。 -->")
        output.append(line)
    return output


def js_candidates(lines):
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped or stripped.startswith(("//", "/*", "*")):
            continue
        match = re.match(r"^(\s*)([A-Za-z_$][A-Za-z0-9_$-]*|'[^']+'|\"[^\"]+\")\s*:", line)
        if match:
            yield index, match.group(1), "配置项", match.group(2).strip("'\"")


def annotate_js(lines):
    candidates = {index: (indent, label, name) for index, indent, label, name in js_candidates(lines)}
    output = []
    for index, line in enumerate(lines):
        candidate = candidates.get(index)
        if candidate and not has_inline_chinese_comment(line) and not has_previous_chinese_comment(output, "//"):
            indent, label, name = candidate
            output.append(f"{indent}// {label} {name}：说明该配置的用途、默认值或运行影响。")
        output.append(line)
    return output


ANNOTATORS = {
    "yaml": annotate_yaml,
    "toml": annotate_toml,
    "kv": annotate_kv,
    "xml": annotate_xml,
    "js": annotate_js,
}

CANDIDATES = {
    "yaml": yaml_candidates,
    "toml": toml_candidates,
    "kv": kv_candidates,
    "xml": xml_candidates,
    "js": js_candidates,
}


def missing_comments(kind, lines):
    missing = []
    candidate_indexes = {index for index, _, _, _ in CANDIDATES[kind](lines)}
    for index, line in enumerate(lines):
        if index not in candidate_indexes:
            continue
        comment_prefix = "<!--" if kind == "xml" else "//" if kind == "js" else "#"
        previous_lines = lines[:index]
        if has_inline_chinese_comment(line):
            continue
        if not has_previous_chinese_comment(previous_lines, comment_prefix):
            missing.append(index + 1)
    return missing


def main():
    parser = argparse.ArgumentParser(description="检查或补齐配置项中文注释。")
    parser.add_argument("--write", action="store_true", help="直接写回可注释配置文件。")
    parser.add_argument("--list", action="store_true", help="仅打印配置文件分类清单。")
    args = parser.parse_args()

    unsupported = []
    changed = []
    failures = []
    parse_failures = []
    categorized = {"yaml": [], "toml": [], "kv": [], "xml": [], "js": [], "json": [], "other": []}

    for relative_path in config_files():
        kind = file_kind(relative_path)
        categorized.setdefault(kind, []).append(relative_path)
        path = ROOT / relative_path
        if kind == "json":
            unsupported.append(relative_path)
            try:
                json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc:
                parse_failures.append((relative_path, str(exc)))
            continue
        annotator = ANNOTATORS.get(kind)
        if annotator is None:
            continue
        original_text = path.read_text(encoding="utf-8")
        original_lines = original_text.splitlines()
        updated_lines = annotator(original_lines)
        updated_text = "\n".join(updated_lines) + ("\n" if original_text.endswith("\n") else "")
        if args.write and updated_text != original_text:
            path.write_text(updated_text, encoding="utf-8")
            changed.append(relative_path)
            check_lines = updated_lines
        else:
            check_lines = original_lines
        missing = missing_comments(kind, check_lines)
        if missing:
            failures.append((relative_path, missing[:10]))
        if kind == "yaml" and yaml is not None:
            try:
                list(yaml.safe_load_all("\n".join(check_lines)))
            except yaml.YAMLError as exc:
                parse_failures.append((relative_path, str(exc)))
        if kind == "xml":
            try:
                ET.fromstring("\n".join(check_lines))
            except ET.ParseError as exc:
                parse_failures.append((relative_path, str(exc)))

    if args.list:
        for kind in sorted(categorized):
            print(f"{kind}: {len(categorized[kind])}")
            for path in categorized[kind]:
                print(f"  {path}")

    if changed:
        print("已补充中文注释的配置文件:")
        for path in changed:
            print(f"- {path}")
    print(f"可原位注释配置文件数量: {sum(len(categorized[kind]) for kind in ANNOTATORS)}")
    print(f"标准 JSON 不支持原位注释，已保持语法不变数量: {len(unsupported)}")
    if failures:
        print("仍缺少中文注释的配置项:")
        for path, lines in failures:
            print(f"- {path}: 行 {', '.join(str(line) for line in lines)}")
        raise SystemExit(1)
    if parse_failures:
        print("配置文件语法解析失败:")
        for path, error in parse_failures:
            print(f"- {path}: {error}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
