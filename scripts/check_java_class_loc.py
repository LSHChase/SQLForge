#!/usr/bin/env python3
import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, List, Sequence


ROOT = Path(__file__).resolve().parents[1]
JAVA_CLASS_PATTERN = re.compile(r"\b(class|interface|enum)\s+([A-Za-z_$][A-Za-z0-9_$]*)\b")


@dataclass
class JavaClassSpan:
    path: Path
    kind: str
    name: str
    start_line: int
    end_line: int
    code_lines: int


def java_files(paths: Sequence[str]) -> List[Path]:
    if not paths:
        roots = [ROOT / module for module in ("benchmark-engine", "governance", "query-execution", "sql-optimization", "sqlforge-shared")]
    else:
        roots = [(ROOT / raw).resolve() if not Path(raw).is_absolute() else Path(raw).resolve() for raw in paths]
    result: List[Path] = []
    for root in roots:
        if root.is_file() and root.suffix == ".java":
            result.append(root)
        elif root.is_dir():
            result.extend(root.glob("src/main/java/**/*.java"))
            if "src/main/java" in str(root):
                result.extend(root.glob("**/*.java"))
    return sorted(set(path.resolve() for path in result))


def strip_comments_preserve_lines(text: str) -> str:
    output: List[str] = []
    index = 0
    in_block = False
    in_string = False
    in_char = False
    escaped = False
    while index < len(text):
        char = text[index]
        nxt = text[index + 1] if index + 1 < len(text) else ""
        if in_block:
            if char == "\n":
                output.append("\n")
            elif char == "*" and nxt == "/":
                in_block = False
                index += 1
            else:
                output.append(" ")
        elif in_string:
            output.append(char if char == "\n" else "x")
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                in_string = False
        elif in_char:
            output.append(char if char == "\n" else "x")
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == "'":
                in_char = False
        elif char == "/" and nxt == "*":
            output.append(" ")
            output.append(" ")
            in_block = True
            index += 1
        elif char == "/" and nxt == "/":
            output.append(" ")
            index += 2
            while index < len(text) and text[index] != "\n":
                output.append(" ")
                index += 1
            continue
        else:
            output.append(char)
            if char == '"':
                in_string = True
            elif char == "'":
                in_char = True
        index += 1
    return "".join(output)


def line_starts(text: str) -> List[int]:
    starts = [0]
    for match in re.finditer("\n", text):
        starts.append(match.end())
    return starts


def line_number(starts: Sequence[int], index: int) -> int:
    low = 0
    high = len(starts)
    while low + 1 < high:
        mid = (low + high) // 2
        if starts[mid] <= index:
            low = mid
        else:
            high = mid
    return low + 1


def matching_brace(text: str, open_index: int) -> int:
    depth = 0
    for index in range(open_index, len(text)):
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
            if depth == 0:
                return index
    return len(text) - 1


def count_code_lines(lines: Sequence[str], start_line: int, end_line: int) -> int:
    return sum(1 for line in lines[start_line - 1:end_line] if line.strip())


def class_spans(path: Path) -> List[JavaClassSpan]:
    raw = path.read_text(encoding="utf-8")
    stripped = strip_comments_preserve_lines(raw)
    starts = line_starts(stripped)
    lines = stripped.splitlines()
    result: List[JavaClassSpan] = []
    for match in JAVA_CLASS_PATTERN.finditer(stripped):
        brace_index = stripped.find("{", match.end())
        if brace_index == -1:
            continue
        close_index = matching_brace(stripped, brace_index)
        start = line_number(starts, match.start())
        end = line_number(starts, close_index)
        result.append(
            JavaClassSpan(
                path=path,
                kind=match.group(1),
                name=match.group(2),
                start_line=start,
                end_line=end,
                code_lines=count_code_lines(lines, start, end),
            )
        )
    return result


def relative(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def render_table(spans: Iterable[JavaClassSpan]) -> str:
    rows = sorted(spans, key=lambda item: (-item.code_lines, relative(item.path), item.start_line))
    return "\n".join(
        f"{item.code_lines}\t{item.kind}\t{item.name}\t{relative(item.path)}:{item.start_line}"
        for item in rows
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="检查 Java 类的非注释、非空代码行数。")
    parser.add_argument("paths", nargs="*", help="仓库相对模块、源码根目录或 Java 文件。")
    parser.add_argument("--limit", type=int, default=200, help="每个类允许的最大代码行数。")
    parser.add_argument("--fail", action="store_true", help="存在任一超限类时返回非零退出码。")
    parser.add_argument("--json", action="store_true", help="输出 JSON，而不是制表符分隔行。")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    spans: List[JavaClassSpan] = []
    for path in java_files(args.paths):
        spans.extend(class_spans(path))
    offenders = [span for span in spans if span.code_lines > args.limit]
    if args.json:
        print(json.dumps([
            {
                "codeLines": span.code_lines,
                "kind": span.kind,
                "name": span.name,
                "path": relative(span.path),
                "startLine": span.start_line,
                "endLine": span.end_line,
            }
            for span in sorted(offenders, key=lambda item: (-item.code_lines, relative(item.path), item.start_line))
        ], ensure_ascii=False, indent=2))
    else:
        output = render_table(offenders)
        if output:
            print(output)
    if args.fail and offenders:
        print(f"超过 {args.limit} 行代码的 Java 类数量：{len(offenders)}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
