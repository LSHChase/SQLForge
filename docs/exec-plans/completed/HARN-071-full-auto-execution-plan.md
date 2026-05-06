# HARN-071 Full-Auto Execution Plan

- Selected story: `D-STORY-009` | Phase-D | 批量解析与报表清单解析
- Cross-context: `D-STORY-010` 解析统计与优先级分层, `E-STORY-007` SQL 查询与历史前端增强
- Task class: `standard`
- Execution mode: `single-agent`

## Requirement

优化批量解析尤其是报表导入的功能，使其达到解析能力与 SQL 解析一致、使用和展示（输入输出）能够达到人类使用简单、页面展示合理（注意细节，尤其是格式化、高亮、取出空格等）、统计汇总的解析问题清晰，利于人类分析和治理大批量 sql（约 50M 大小的 sql，可能有 10 万个）。

## Scope

1. Inspect the current batch parse, report import, single SQL parser, SQL display, and statistics contracts before editing.
2. Align batch/report SQL normalization with the single SQL parser boundary without changing SQL execution semantics: trim surrounding cell/file whitespace, preserve meaningful SQL text, keep parser diagnostics and issue fields consistent.
3. Improve large input handling for repository-supported import paths, with explicit 50M / 100k-SQL guardrails where practical: streaming or bounded preview behavior, clear limits, and no full-page rendering of every SQL by default.
4. Improve batch/report import UI input and output ergonomics: readable import guidance, preview/output SQL formatting, highlighting, whitespace handling, responsive containers, and drill-down that remains useful for large batches.
5. Make parse statistics and issue summaries clearer for human governance: surface parse status, failed/partial categories, issue scene/severity/priority/report/logical object signals, and top problem lists without requiring raw JSON reading.
6. Add focused backend, frontend contract, and documentation coverage for the confirmed implementation.

## Out Of Scope

- No new microservice.
- No permission model expansion unless repository inspection proves an existing contract needs additive exposure.
- No production SQL execution, data loading, or external report-system integration.
- No schema migration or historical backfill unless repository evidence proves it is unavoidable and the task records a rollback path.
- No semantic SQL rewrite of persisted SQL or execution payloads; formatting is UI/display scoped unless explicitly limited to import-cell outer trimming.

## Validation Focus

- Batch/report import uses the same structure-parse pipeline and diagnostics as single SQL parsing for representative valid and invalid SQL.
- Surrounding whitespace in imported SQL cells/files is trimmed, while meaningful SQL content, comments, strings, quotes, semicolons, and line breaks remain safe for parsing.
- Large batch behavior is bounded and testable: summary-first display, no unbounded SQL list rendering, and a documented 50M / 100k-SQL expectation.
- Statistics and issue summary views are understandable without raw JSON and support drill-down into failed/problem SQL.
- Existing HARN-061/HARN-062/HARN-063/HARN-064/HARN-066/HARN-070 contracts continue to pass.

## Standard Chain

1. `python3 scripts/foreman.py preflight`
2. `bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/intake-20260506055117/candidate-task-pack.json --skip-review`
3. Implement scoped code, tests, and docs.
4. `python3 scripts/foreman.py validate HARN-071`
5. `python3 scripts/task_audit.py --check --phase pre-closeout`
6. `python3 scripts/foreman.py closeout HARN-071`
7. `python3 scripts/task_audit.py --check --phase post-closeout`

## Notes

- The automated shaping reviewer could not run because the nested Codex process hit an external usage limit. This plan is therefore hand-shaped by Main Foreman and must be materialized with `--skip-review`, relying on deterministic materialization gates and the normal foreman/task-audit closeout chain.
