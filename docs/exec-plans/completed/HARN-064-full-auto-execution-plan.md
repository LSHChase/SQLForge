## Candidate Execution Plan: HARN-064

### Landing Story
- Story: `D-STORY-010` | Phase-D | 解析统计与优先级分层
- Task class: `standard`

### Scope Shape
- Define batch-level parsing statistics for the “批量解析中报表导入解析” flow after a user selects batch parsing.
- Align the statistics model with existing SQL parsing statistics, but leave concrete reuse of APIs, components, schemas, or aggregation code to the implementation phase after repository reading.
- Ensure parsing history can query or display the same batch/history statistics.
- Standardize the minimum statistics dimensions:
  - 问题场景
  - 重要程度
  - 报表视角
  - SQL 清单
  - 优先级视角
  - 逻辑对象视角

### Governance Sequence
1. Main Foreman runs `python3 scripts/foreman.py preflight` before any implementation because this is non-trivial.
2. User confirmation for HARN-064 materialization has been recorded on 2026-05-05; materialization must continue through `python3 scripts/foreman.py preflight` and the task pack gate.
3. Implementation must instantiate through `python3 scripts/foreman.py instantiate HARN-064`.
4. Code reading should confirm actual ownership across batch parsing, report import parsing, parsing history, existing SQL parsing statistics, logical object parsing, and frontend/API boundaries.
5. Implementation should add the smallest coherent backend/statistics contract first, then expose history/batch query behavior, then add UI integration if in task scope.
6. Validation must run through `python3 scripts/foreman.py validate HARN-064`.
7. Before closeout, run `python3 scripts/task_audit.py --check --phase pre-closeout`.
8. Closeout must use `python3 scripts/foreman.py closeout HARN-064`.
9. After closeout, run `python3 scripts/task_audit.py --check --phase post-closeout`.
10. As a standard task, use single-task single-commit discipline and avoid delivery tag/write-back unless later reclassified.

### Suggested Implementation Slices For Task-Shaper
- Statistics requirement and contract slice: define accepted dimensions, aggregation keys, and history/batch lookup behavior.
- Backend/service slice: compute or retrieve statistics for selected batch report-import parsing results.
- History integration slice: expose statistics by batch or historical parse record without duplicating incompatible aggregation logic.
- Frontend/display slice: show statistics in batch parsing and parsing history surfaces, subject to codebase boundary confirmation.
- Test/documentation slice: cover aggregation, empty/error states, dimension completeness, and document the confirmed statistics contract.
