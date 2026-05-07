# SQLForge Completed Tasks

本文件只记录已完成、已验证、已归档的任务。

## Done

### OPS-RESTART-20260507-3: Restart local frontend and backend on demand

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260507-3 restart local frontend and backend`
- Priority: P2
- Depends on: N/A
- Scope: Restart SQLForge local backend services and frontend dev server on user request, then validate backend health endpoints and frontend availability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260507-3`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend services and the Vite frontend dev server while reusing the existing healthy local Docker infrastructure.
  - Validation evidence: python3 scripts/foreman.py validate OPS-RESTART-20260507-3 with backend health endpoints and frontend HTTP 200 checks.
  - Residual risk: Local runtime remains dependent on this workstation's detached processes and Docker container state.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid when stopping these local services.

### HARN-080: 修复解析历史持久化并统一批量解析核心逻辑

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(sql-optimization): persist batch parse history`
- Priority: 1
- Depends on: N/A
- Scope: 修复单条 SQL 解析与批量解析结果未稳定写入治理历史的问题，确保批量解析复用单条 SQL 结构解析的完整、准确、深度逻辑；补充历史写入、批量 item 追溯字段与相关回归测试，不改无关治理/runtime 流程。
- Validation:
  - `python3 scripts/foreman.py validate HARN-080`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Fixed SQL parse history write orchestration for single, parse-batch, combined, and report-batch parse paths; added parse_batch_item history trace fields and schema migration.
  - Validation evidence: python3 scripts/foreman.py validate HARN-080; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=StructureParseControllerTest,ParseBatchControllerTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest test; mvn -pl sql-optimization -Dtest=AccessParseControllerTest,ReportBatchApplicationServiceTest test; mvn -pl sql-optimization test; git diff --check.
  - Residual risk: External deployed governance database verification was not run in this repository turn; coverage is repo-closed tests and migration/schema assertions.
  - Next step: Use deployed environment smoke to confirm query_history rows for real governance service calls when environment-backed validation is available.

### OPS-RESTART-20260507-2: Restart local frontend and backend again

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260507-2 restart local frontend and backend`
- Priority: P2
- Depends on: N/A
- Scope: Restart SQLForge local backend services and frontend dev server on user request, then validate health endpoints and frontend availability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260507-2`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local backend services and the Vite frontend dev server while reusing the existing healthy local Docker infrastructure.
  - Validation evidence: python3 scripts/foreman.py validate OPS-RESTART-20260507-2 with backend health endpoints and frontend HTTP 200 checks.
  - Residual risk: Local runtime remains dependent on this workstation's detached processes and Docker container state.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid when stopping these local services.

### UI-TASK-002: Converge parse history and report import IA

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): UI-TASK-002 converge parse history IA`
- Priority: 1
- Depends on: N/A
- Scope: Refine ParseRecordView and ParseBatchCenterView information architecture per confirmed plan: add top-level parse history workbench tabs, move report issue-scene detail into report-level statistics issue-scene tab, remove duplicate overview tabs from report batch SQL detail/statistics dialogs, preserve backend APIs, DTOs, parser behavior, statistics semantics, permissions, and persistence schema, and update frontend contract checks.
- Validation:
  - `python3 scripts/foreman.py validate UI-TASK-002`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added top-level SQL-history vs batch/report-history tabs in ParseRecordView, moved report issue-scene detail into the report-level statistics issue-scene tab, removed duplicate overview tabs from whole-report SQL detail and report statistics dialogs in ParseBatchCenterView, and updated static frontend contracts without changing backend APIs, parser behavior, DTOs, permissions, statistics semantics, or persistence schema.
  - Validation evidence: python3 scripts/foreman.py validate UI-TASK-002 --include-task-audit --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Manual browser or portable smoke was not run in this turn; coverage is repo-closed through static contracts, lint, production build, and task audit.
  - Next step: Use a real report-import batch during product acceptance to click the history workbench tabs, SQL list detail, and inline issue-scene drill-down.

### HARN-079: Analyze merge candidates for multi-SQL reports

- Status: done
- Completed at: 2026-05-07
- Commit subject: `feat(sql-optimization): flag report sql merge candidates`
- Priority: 1
- Depends on: N/A
- Scope: Add conservative report-batch parse-statistics analysis for same-report multi-SQL merge candidates, expose compatible summary fields, and cover the behavior with backend tests without changing persistence schema or generating executable merged SQL.
- Validation:
  - `python3 scripts/foreman.py validate HARN-079`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-07: implemented conservative `REPORT_SQL_MERGE_CANDIDATE` analysis for report batch parse statistics, exposed compatible API/UI fields, and documented the non-executable merge suggestion boundary.
  - 2026-05-07: validation passed via targeted report-batch tests, full `sql-optimization` tests, frontend lint/build, batch/history contract checks, and `git diff --check`.
- Context closeout:
  - Completed scope: Added conservative report-batch merge-candidate analysis for same-report multi-SQL groups, exposed compatible merge-candidate API fields, showed the hint in report batch and parse-history report statistics, documented the non-executable recommendation boundary, and covered the behavior with backend/controller tests plus frontend contract checks.
  - Validation evidence: python3 scripts/foreman.py validate HARN-079 --include-task-audit --extra-command 'mvn -pl sql-optimization -Dtest=ReportBatchParseStatisticsAssemblerTest,ReportBatchControllerTest test' --extra-command 'mvn -pl sql-optimization test' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The merge-candidate signal is a conservative static governance hint only; it does not prove semantic equivalence and does not generate executable merged SQL.
  - Next step: When real report samples are available, review reported merge candidates with report owners before promoting any manual SQL consolidation.

### OPS-DIST-PORTABLE-DETAIL-20260507: 修复 dist-portable 解析详情加载 404

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): harden portable parse detail loading`
- Priority: 1
- Depends on: N/A
- Scope: Analyze dist-portable generated frontend behavior, fix portable parse-detail 404 and refresh generated dist-portable package without changing parser or backend data contracts.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-PORTABLE-DETAIL-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated ParseRecordView so SQL-list parse detail loading only calls governance query-history for persisted historyId values, displays report-batch SQL evidence when governance history is missing or unavailable, refreshes dist-portable generated assets, and extends portable smoke plus history contract checks for the parse-detail path.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-PORTABLE-DETAIL-20260507 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run build:portable' --extra-command 'npm run smoke:portable-frontend' --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs' --extra-command 'git diff --check'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The portable package still depends on correct backend base URLs and live governance data for full query-history detail; when governance history is absent, the UI now intentionally falls back to report-batch SQL evidence rather than fabricating a history URL.
  - Next step: Deploy the refreshed dist-portable package with the target portable-config.json backend URLs and verify the SQL list detail path against the real environment.

### OPS-DIST-ROOT-REVERT-20260507: Revert mistaken root dist commit

- Status: done
- Completed at: 2026-05-07
- Commit subject: `revert(frontend): remove mistaken root dist commit`
- Priority: 1
- Depends on: N/A
- Scope: Revert the mistaken root dist commit 7f9e3f6 without destructive git reset, restore the repository to the prior tracked-dist state, and validate audit/reachability after the rollback.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-ROOT-REVERT-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reverted the mistaken 7f9e3f6 root dist commit with git revert --no-commit, removed the tracked root dist files again, restored the repository to the prior ignored-dist state, and switched the local frontend on port 3000 back from dist preview to Vite dev server.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-ROOT-REVERT-20260507 --include-task-audit --extra-command git-diff-cached-check --extra-command git-diff-check --extra-command root-dist-index-absent --extra-command frontend-curl --extra-command governance-health
  - Residual risk: The earlier mistaken commit remains in git history but is neutralized by this explicit revert commit; root dist remains ignored and should not be force-added again unless intentionally requested.
  - Next step: Continue using the tracked dist-portable package for committed frontend packaging, or open a separate task before changing root dist tracking policy.

### OPS-DIST-REFRESH-20260507: Refresh frontend dist from latest source

- Status: done
- Completed at: 2026-05-07
- Commit subject: `chore(frontend): refresh packaged dist assets`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild and persist the frontend distribution artifacts from the latest frontend source, verify the generated dist output changes as expected, and restart local frontend/backend services if needed for validation.
- Validation:
  - `python3 scripts/foreman.py validate OPS-DIST-REFRESH-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt the root ignored dist artifact locally and regenerated the tracked dist-portable package from the latest Vue frontend source so packaged assets use the current hashed bundles.
  - Validation evidence: python3 scripts/foreman.py validate OPS-DIST-REFRESH-20260507 --include-task-audit --extra-command npm-run-build --extra-command npm-run-build-portable --extra-command check-query-workbench-contract --extra-command git-diff-check; frontend/backend reachability checks returned HTTP 200 / UP.
  - Residual risk: Root dist/ is intentionally ignored by git and remains a local-only build output; dist-portable is the committed package. npm run smoke:portable-frontend was attempted and failed on a stale query-flow-status assertion that is no longer part of the current query page contract.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd for the portable package; update the portable smoke selector contract in a separate task if browser-smoke coverage must be restored.

### OPS-RESTART-20260507: Refresh frontend dist and restart local services

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260507 refresh dist and restart local services`
- Priority: 1
- Depends on: N/A
- Scope: Rebuild the root frontend dist artifact, then restart the local frontend dev server and backend runtime services using repository standard scripts and verify local reachability.
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260507`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt root frontend dist with npm run build, stopped stale frontend/backend runtime listeners, restarted governance/query-execution/sql-optimization/benchmark-engine using scripts/start-backend-services.sh with the existing local stack, and restarted the Vite frontend dev server on port 3000.
  - Validation evidence: npm run build; python3 scripts/foreman.py validate OPS-RESTART-20260507; curl health checks for 8080/8081/8082/8083 and frontend HTTP 200; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Local runtime remains workstation-stateful; services depend on the current docker-compose infrastructure and detached process state under .codex/state/.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid to stop these services when no longer needed.

### UI-TASK-001: Converge batch parse center input cards

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): UI-TASK-001 converge batch parse inputs`
- Priority: 1
- Depends on: N/A
- Scope: Remove first-screen batch parse/report import input cards from ParseBatchCenterView, move user input guidance to existing create/import/template dialogs, keep API payloads unchanged, and validate frontend build/lint.
- Validation:
  - `python3 scripts/foreman.py validate UI-TASK-001`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-05-07: removed first-screen parse/report input rail cards from `src/views/parse-batch/ParseBatchCenterView.vue`, kept create/import/template dialogs as the input surfaces, and changed the current workbench layout to a single result column.
  - 2026-05-07: updated `scripts/check-batch-import-contract.mjs` so the page contract now forbids the removed input rail test ids and requires dialog-based input entry points.
  - 2026-05-07: validation passed via `python3 scripts/foreman.py validate UI-TASK-001 --extra-command "node scripts/check-batch-import-contract.mjs" --extra-command "npm run lint" --extra-command "npm run build" --extra-command "git diff --check"`.
- Context closeout:
  - Completed scope: Removed the first-screen parse and report import input rail cards from ParseBatchCenterView, converted the current workbench to a single result column, kept create/import/template responsibilities in dialogs, and updated the batch import contract check to require dialog input entry points while forbidding removed rail test ids.
  - Validation evidence: python3 scripts/foreman.py validate UI-TASK-001 --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'
  - Residual risk: Manual browser interaction against a live backend was not run in this turn; repo-closed frontend build/lint and contract checks passed.
  - Next step: None.

### HARN-078: Optimize parse page information architecture

- Status: done
- Completed at: 2026-05-07
- Commit subject: `feat(frontend): reorganize parse detail tabs`
- Priority: 1
- Depends on: N/A
- Scope: Move batch and report import template documentation into template dialogs, reorganize parse result/statistics/history/detail views into tabs, and update frontend contract checks without changing backend APIs, parser behavior, DTOs, or database schema.
- Validation:
  - `python3 scripts/foreman.py validate HARN-078`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 优化批量解析中心、报表导入、解析历史报表详情和解析统计详情的信息架构：模板说明移入按钮弹窗，解析结果/统计/报表 SQL 详情改为 tabs 分层，统计详情原始 JSON 收敛到弹窗内 tab；未修改后端 API、parser、DTO 或数据库 schema。
  - Validation evidence: python3 scripts/foreman.py validate HARN-078 --extra-command node scripts/check-batch-import-contract.mjs --extra-command node scripts/check-history-page-contract.mjs --extra-command node scripts/check-history-detail-contract.mjs --extra-command node scripts/check-statistics-page-contract.mjs --extra-command node scripts/check-parse-workbench-contract.mjs --extra-command npm run lint --extra-command npm run build --extra-command git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未运行真实浏览器人工点击验收；当前覆盖来自静态契约、lint、生产构建和源码级按钮顺序检查。
  - Next step: 产品验收时用包含普通批量失败 SQL、报表宽表、多报表分组和问题场景详情的样例批次逐项点击 tabs 与模板弹窗。

### HARN-077: 补齐解析详情问题场景中文提示

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(frontend): complete issue scene tooltips`
- Priority: P1
- Depends on: HARN-076
- Scope: 补齐解析历史详情与报表导入详情中问题场景代码旁的中文问号提示，覆盖结构解析 risk checklist、issues、统计、SQL 明细和失败/问题 SQL 详情；复用共享 issueSceneHelp 文案，不改 parser、接口字段或数据库 schema。
- Validation:
  - `python3 scripts/foreman.py validate HARN-077`
- Progress log:
  - 2026-05-07: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 补齐解析历史详情与报表导入详情中问题场景代码旁的中文 ? 提示；共享 issueSceneHelp 文案按风险含义、原因、建议三段输出并覆盖 REPEATED_TABLE_SCAN_RISK 等结构解析场景；问题场景帮助改为可 hover/focus 的 inline icon，避免嵌套按钮吞掉 tooltip；SQL 清单、失败/问题 SQL、报表级统计、risk checklist 与 issues 列表均展示短问题代码和定位信息，完整 SQL 仍只保留在 SQL 输出区域。
  - Validation evidence: python3 scripts/foreman.py validate HARN-077 --extra-command node scripts/check-history-detail-contract.mjs --extra-command node scripts/check-history-page-contract.mjs --extra-command node scripts/check-batch-import-contract.mjs --extra-command node scripts/check-parse-workbench-contract.mjs --extra-command npm run lint --extra-command npm run build --extra-command git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未在真实浏览器中对生产规模报表批次逐项手动 hover/focus 验收；当前覆盖来自静态契约、lint 与生产构建。
  - Next step: 产品验收时在解析历史详情和报表导入详情中用包含 REPEATED_TABLE_SCAN_RISK、SQL_SYNTAX_INVALID 与其他问题场景的样例批次逐项悬停/聚焦 ?。

### OPS-RESTART-20260506: Restart local frontend and backend

- Status: done
- Completed at: 2026-05-07
- Commit subject: `OPS-RESTART-20260506 restart local frontend and backend`
- Priority: P2
- Depends on: N/A
- Scope: Restart SQLForge local backend services and frontend dev server
- Validation:
  - `python3 scripts/foreman.py validate OPS-RESTART-20260506`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted local Docker infrastructure, governance, query-execution, sql-optimization, benchmark-engine, and the Vite frontend dev server.
  - Validation evidence: python3 scripts/foreman.py validate OPS-RESTART-20260506 with backend health endpoints and frontend HTTP 200 checks.
  - Residual risk: Local development runtime depends on this workstation's Docker and detached process state.
  - Next step: Use .codex/state/backend-runtime/*.pid and .codex/state/runtime-logs/frontend.pid when stopping these local services.

### HARN-076: Align parse history and report import details

- Status: done
- Completed at: 2026-05-07
- Commit subject: `fix(sql-optimization): align report parse details`
- Priority: P1
- Depends on: HARN-075
- Scope: Align parse history detail and report import SQL details with single SQL comprehensive parsing: shared issue-scene help hints, remove full SQL from location/task summaries, and run report SQL structure parse plus Access parse plus history writeback without schema changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-076`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned report import SQL parsing with the comprehensive single-SQL path by running structure parse, Access Parse, combined history writeback, and detail-safe status propagation; added shared issue-scene help text and removed source-line SQL from fallback location summaries.
  - Validation evidence: python3 scripts/foreman.py validate HARN-076; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest test; mvn -pl sql-optimization test; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-parse-workbench-contract.mjs; npm run lint; npm run build; git diff --check
  - Residual risk: Manual browser verification against a production-sized real report batch was not run in this turn.
  - Next step: During product acceptance, import a real report workbook/CSV and confirm tooltips, Access status, history drill-through, and SQL-only output sections in the UI.

### BUG-REPORT-SQL-HISTORY-DETAIL-20260507: Fix report SQL history detail loading

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): persist report SQL parse history`
- Priority: 1
- Depends on: N/A
- Scope: Persist and load per-SQL parse history for report import history details
- Validation:
  - `python3 scripts/foreman.py validate BUG-REPORT-SQL-HISTORY-DETAIL-20260507`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Persist report batch item history ids, expose them through report SQL detail APIs, and load report import history SQL details by real historyId with legacy fallback.
  - Validation evidence: mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest clean test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-detail-contract.mjs; npm run build; bash scripts/verify-db-scripts.sh
  - Residual risk: Existing report batch items imported before this change may lack history_id; the frontend keeps a parseTaskId-derived fallback and displays unavailable history without blocking batch evidence.
  - Next step: Deploy DB migration V20260507_001 before relying on persisted report SQL history ids in database-worker mode.

### D-TASK-076: Accelerate report batch parsing and issue scene drilldown

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(sql-optimization): accelerate report batch parsing`
- Priority: P1
- Depends on: N/A
- Scope: Implement async report batch structure parsing, batch persistence optimization, and issue-scene drilldown details for report import parse statistics.
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-076`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented asynchronous structure-first report batch parsing, batch item bulk persistence, issue-scene drilldown APIs, and parse-record frontend detail inspection for affected reports/logical objects/SQL rows.
  - Validation evidence: mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest test; mvn -pl sql-optimization test; npm run lint; npm run build; npm run test:form-governance; python3 scripts/foreman.py validate D-TASK-076; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Application-local background report batch parsing is not restart-resumable; if the service restarts mid-batch, operators should trigger resolve-sqls again for the affected batch.
  - Next step: If production requires restart recovery, promote report batch parsing to the database-worker queue model in a follow-up task.

### HARN-075: Adjust report batch SQL detail pagination and issue location display

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): adjust report SQL detail pagination`
- Priority: P1
- Depends on: HARN-074
- Scope: Implement report batch SQL detail pagination and report filtering; keep full SQL only in dedicated SQL output; show short per-issue location snippets and help hints across report detail, SQL detail, and parse statistics without schema migration.
- Validation:
  - `python3 scripts/foreman.py validate HARN-075`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Report batch SQL detail and parse statistics now support pagination and report-code filtering; issue location payloads expose short per-issue SQL snippets; report SQL task/location UI avoids full SQL and source-line context while keeping full SQL in the SQL output block; help hints added across report detail and parse statistics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-075; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest test; mvn -pl sql-optimization test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-sql-ui-contract.mjs; npm run lint; npm run build
  - Residual risk: Manual browser verification of long real report batches was not run in this turn.
  - Next step: Exercise the report batch detail dialogs with a production-sized batch in the UI when sample data is available.

### HARN-074: Fix batch report SQL detail scoping and diagnostics

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): scope batch report sql details`
- Priority: P1
- Depends on: HARN-073
- Scope: Fix report import per-report SQL detail scoping, add whole-batch SQL detail entry, and expose batch/report SQL parse diagnostics with location evidence.
- Validation:
  - `python3 scripts/foreman.py validate HARN-074`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added per-report report-batch SQL detail, a whole-batch SQL detail entry, structured parse diagnostic fields for batch/report SQL rows, richer structure failure reasons, frontend diagnostic rendering, and regression tests/contracts.
  - Validation evidence: python3 scripts/foreman.py validate HARN-074 --include-task-audit --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'mvn -pl sql-optimization test' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'git diff --check'
  - Residual risk: Precise line/column/token/snippet diagnostics are available for parser failures; non-fatal issue detections are localized to the SQL row/report context and issue-scene codes.
  - Next step: Smoke a real report import workbook or CSV with multiple reports, invalid SQL, and anti-pattern SQL to confirm the UI separates single-report details from whole-batch SQL detail.

### HARN-073: Fix report import SQL extraction and diagnostics

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): extract report import sql bodies`
- Priority: P1
- Depends on: N/A
- Scope: Fix report-import SQL extraction for cells whose content starts with repeated -- comment or description fragments before the real SELECT/SQL body, so parsing uses the extracted SQL statement instead of treating the whole cell as comments. Preserve imported SQL evidence where compatible, improve parse failure diagnostics with location/snippet for report import problems, and add focused backend tests/docs without schema changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-073`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added report-import SQL extraction for same-line dash-comment preambles before SELECT/WITH, kept normal SQL comments on the existing structure parser path, enriched report-import structure parse failure reasons with compact line/column/token/snippet diagnostics, and documented the HARN-073 report-import exception and UI display expectation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-073; mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest clean test; mvn -pl sql-optimization test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; mvn -pl sql-optimization validate pmd:pmd checkstyle:check; git diff --check; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The extraction is intentionally scoped to report-import cells that start with same-line -- description preambles and then contain SELECT/WITH on that same first line; other SQL file splitting and normal SQL line-comment behavior remain unchanged.
  - Next step: Use a real report import workbook/CSV containing '-- 说明 -- 说明 SELECT ...' cells and an invalid sample to confirm the UI shows extracted SQL plus reportCode/sqlColumnName/sourceFileLine diagnostics.

### HARN-072: Align batch SQL diagnostics with single SQL parse

- Status: done
- Completed at: 2026-05-06
- Commit subject: `fix(sql-optimization): align batch SQL diagnostics`
- Priority: P1
- Depends on: HARN-071
- Scope: Fix batch parse issue detection so each batch SQL uses the same diagnostics path as single SQL parsing for complex anti-pattern SQL, including nested scalar subqueries, correlated subqueries, NOT EXISTS, wildcard LIKE, OR predicates, function-wrapped predicates, and ORDER BY RAND diagnostics; keep existing batch/report import contracts and do not add schema changes.
- Validation:
  - `python3 scripts/foreman.py validate HARN-072`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated ordinary batch parsing and report batch parsing to retain the single-SQL structure parser's fine-grained issue codes in SQL-level batch details/statistics instead of collapsing multiple diagnostics into coarse issue scenes; added issue-code priority scoring support and complex anti-pattern SQL regressions for SQL-file batch and report wide-table batch paths.
  - Validation evidence: python3 scripts/foreman.py validate HARN-072; mvn -pl sql-optimization -Dtest=ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest,ReportBatchParseStatisticsAssemblerTest,ParseStatisticsApplicationServiceTest,StructureParseControllerTest,SqlOptimizationPipelineServiceTest test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; mvn -pl sql-optimization test; git diff --check
  - Residual risk: Existing persisted batch rows keep their historical coarse issue-scene summaries until re-parsed; this task intentionally avoided schema migration and only changes newly parsed batch/report SQL diagnostics.
  - Next step: Use the provided complex anti-pattern SQL in acceptance against both SQL解析 and 批量解析, then confirm the batch detail/statistics list includes the same issue codes such as SCALAR_SUBQUERY_IN_SELECT, OR_PREDICATE_INDEX_RISK and ORDER_BY_RANDOM_RISK.

### HARN-071: HARN-071 批量解析与报表导入大批量治理体验收口

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(sql-optimization): harden large batch report parsing`
- Priority: 1
- Depends on: `HARN-061`,`HARN-062`,`HARN-063`,`HARN-064`,`HARN-066`,`HARN-070`
- Scope: Main Foreman must execute HARN-071 through SQLForge standard actions; implementation must first confirm current parser/import/UI/statistics contracts from repository evidence, then keep batch/report parse behavior aligned with single SQL parse diagnostics, improve UI display/whitespace/format/highlight and issue summary clarity, and preserve execution payload, persisted history, tenant/request/...
- Plan ref: docs/exec-plans/completed/HARN-071-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: Resolved by active user objective on 2026-05-06: proceed as a standard single-agent Main Foreman task using HARN-071. If repository inspection proves schema migration, permission expansion, new import formats, external report-system integration, or task splitting is unavoidable, pause and request a separate human decision before implementing that expansion.
- Data impact: Expected data impact is low to moderate: imported SQL text may be trimmed at the outer cell/file boundary for parsing and display, and API/VO/UI summaries may gain compatible additive fields. No historical data migration, destructive update, or persisted SQL semantic rewrite is authorized by default.
- Rollback / recovery: Rollback by reverting the single HARN-071 task commit and restoring prior batch/report import parser, statistics, and frontend display behavior. If compatible additive API fields or docs are introduced, remove them with the same commit rollback. If implementation discovers unavoidable schema or data migration work, stop for confirmation and record a separate rollback path before proceeding.
- Validation:
  - `python3 scripts/foreman.py validate HARN-071、mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest,SqlOptimizationPipelineServiceTest,StructureParseControllerTest test、node scripts/check-batch-import-contract.mjs、node scripts/check-history-page-contract.mjs、node scripts/check-history-detail-contract.mjs、node scripts/check-sql-ui-contract.mjs、npm run lint、npm run build`
  - `python3 scripts/foreman.py validate HARN-071`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned batch SQL file splitting with parser boundaries, added backend preview metadata for large parse/report batches, capped report SQL statistics previews, improved ParseBatchCenterView summary-first large-batch display, and documented HARN-071 import/display/statistics limits.
  - Validation evidence: python3 scripts/foreman.py validate HARN-071; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Djava.io.tmpdir=/models/project/codex/SQLForge/.tmp -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest,SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ParseBatchApplicationServiceTest clean test; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-sql-ui-contract.mjs; npm run lint; npm run build; git diff --check
  - Residual risk: Repo-side 50M / 100k SQL handling is closed as complete statistics plus bounded previews over the existing contentBase64 import path; true streaming upload, external report-system integration, and production-scale acceptance remain out of scope.
  - Next step: No immediate follow-up required.

### HARN-070: HARN-070 / E-STORY-007 - SQL 输入输出展示与编辑体验增强执行模板

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(frontend): add SQL copy format highlighting`
- Priority: 1
- Depends on: `N/A`
- Scope: Main Foreman must execute HARN-070 through SQLForge standard actions: preflight, materialize/instantiate, implement, validate, task_audit pre-closeout, closeout, task_audit post-closeout, and single-task single-commit discipline. Every confirmed SQL input surface must support copy plus a manual format action that mutates only the editable UI value. Every confirmed SQL output surface must suppor...
- Plan ref: docs/exec-plans/completed/HARN-070-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: Resolved by user confirmation on 2026-05-06: materialize HARN-070 as a standard single-agent Main Foreman task bound to E-STORY-007 / Phase-E. Repository inspection will determine the final SQL input/output inventory, existing formatter/highlighter reuse, and concrete documentation entry points. Implementation must keep formatting/display separate from execution, query results, persisted history, and backend datasource semantics.
- Data impact: Expected data impact is low: changes target UI display/edit interactions and documentation. Potential risk exists if formatting is applied to persisted SQL, submitted SQL, query history records, or execution payloads; formal implementation must keep formatting/display separate from business semantics unless explicitly confirmed.
- Rollback / recovery: Rollback by reverting the single HARN-070 task commit if implemented as a normal task. Recovery should restore previous SQL input/output UI behavior, remove added formatter/highlighter dependencies or component wiring, and preserve task ledger/audit records according to SQLForge closeout rules. If a dependency is added, rollback must also remove lockfile/package changes tied only to this task.
- Validation:
  - `Inventory-backed coverage for each confirmed SQL input page copy behavior、Coverage for each confirmed SQL input page manual format behavior、Coverage for each confirmed SQL output page copy behavior、Coverage for automatic output SQL formatting at the confirmed render/receive boundary、Coverage or assertion for SQL syntax highlighting render state where practical、Coverage for long SQL readability behavior: height, width, scroll, wrap, max-height, or responsive constraints、Regression checks that formatting/display does not alter execution semantics or query results`
  - `python3 scripts/foreman.py validate HARN-070`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared SQL editor/display components with copy, manual input formatting, automatic output display formatting, SQL syntax highlighting, and readable sizing; wired them into query, parse, batch parse, parse history, recommendation, and benchmark SQL surfaces; added a static SQL UI contract check and documented the HARN-070 page inventory and display boundary.
  - Validation evidence: python3 scripts/foreman.py validate HARN-070 --extra-command npm-run-test-sql-ui-contract --extra-command npm-run-test-form-governance --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: SQL formatting is intentionally lightweight and UI-scoped; report wide-table/CSV inputs keep formatting disabled to avoid mutating table structure; no live browser E2E was run against external data.
  - Next step: During product acceptance, spot-check long SQL copy/format/highlight behavior on /sql-query, SQL parse, batch parse, parse history, recommendation, and benchmark pages with production-like SQL samples.

### U-TASK-005: 更新前端 dist 产物

- Status: done
- Completed at: 2026-05-06
- Commit subject: `chore(frontend): refresh packaged dist assets`
- Priority: 1
- Depends on: U-TASK-004
- Scope: Regenerate frontend dist and dist-portable from the latest Vue source so packaged pages match current implementation; no source behavior changes.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-005`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Regenerated frontend dist and dist-portable from the latest Vue source so packaged pages include the current UI.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-005 --include-task-audit --extra-command "npm run build" --extra-command "npm run build:portable"; curl checks for dist preview root/dashboard returned 200; portable health returned UP.
  - Residual risk: dist/ is ignored by git and updated locally only; dist-portable is the committed portable package for cross-machine startup.
  - Next step: Use dist-portable/start-portable.sh or start-portable.cmd on another computer after editing portable-config.json for backend host addresses.

### HARN-066: HARN-066

- Status: done
- Completed at: 2026-05-06
- Commit subject: `feat(sql-optimization): add parse statistics tabs and SQL diagnostics`
- Priority: 1
- Depends on: N/A
- Scope: 
- Plan ref: docs/exec-plans/completed/HARN-066-full-auto-execution-plan.md
- Matrix context: Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: Confirmed by user on 2026-05-06: materialize HARN-066 as a standard single-agent Main Foreman task bound to D-STORY-010 / Phase-D. Prefer existing SQL parse statistics semantics; document any difference. Use the current history permission model. Compatible additive API, field, cache, or UI-contract adaptations are allowed when confirmed by repository evidence. Failure position should prefer line/column and fall back to offset, token, or SQL snippet when parser precision is limited.
- Data impact: May affect parse-history read/display paths and statistics response shape. Use existing persisted parse evidence where possible. Compatible additive fields or cache keys are allowed when needed; avoid new schema unless repository evidence proves it necessary, and document compatibility, migration, rollback, and permission behavior if it occurs.
- Rollback / recovery: 若统计展示或解析行为变更引入回归，应可回退 HARN-066 单任务 commit；若涉及 schema/API 变更，需提供向后兼容路径或显式迁移回滚说明；解析器隔离和注释支持应配套回归测试，保证回滚后可恢复既有解析行为。
- Validation:
  - `python3 scripts/foreman.py validate HARN-066`
- Progress log:
  - 2026-05-06: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added tabbed report-batch parse statistics in batch import and parse-history views, isolated single-SQL async results from later inputs, supported SQL body -- line comments, exposed parser failure reason/line/column/offset/token/snippet through backend responses and issue cards, and updated HARN-066 docs/tests.
  - Validation evidence: python3 scripts/foreman.py validate HARN-066; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -pl sql-optimization -Dtest=SqlOptimizationPipelineServiceTest,StructureParseControllerTest,ReportBatchControllerTest,ReportBatchParseStatisticsAssemblerTest test; mvn -pl sql-optimization test; npm run lint; npm run build; npm run test:form-governance; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs.
  - Residual risk: No schema migration was added; parser failure position remains best-effort for dialect-specific parser messages, with token/snippet fallback when exact line-column precision is unavailable.
  - Next step: Use a live report-import batch in an integration environment to visually confirm the six statistics tabs and failure-position cards against production-like data.

### HARN-064: HARN-064 / D-STORY-010: 批量报表导入解析统计与解析历史统计展示

- Status: done
- Completed at: 2026-05-05
- Commit subject: `feat(sql-optimization): add report batch parse statistics`
- Priority: 1
- Depends on: `N/A`
- Scope: HARN-064 应建立一个可验证的批次/历史解析统计契约：输入为用户选择的批次解析结果或解析历史记录，输出为与现有 SQL 解析统计口径对齐的统计数据集合，至少包含六类维度。实现不得预设现有 SQL 解析统计接口、组件、schema 或聚合逻辑可直接复用；必须在正式实现阶段读取代码后确认复用或适配方案。批量解析入口与解析历史入口应共享一致统计口径，避免产生不兼容的重复聚合逻辑。 Tech: `Repository-confirmed backend/service layer after preflight`,`Repository-confirmed API/query contract after code reading`,`Repository-confirmed frontend/history/batch parsing surfaces if within scope...
- Plan ref: docs/exec-plans/completed/HARN-064-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: Resolved before materialization on 2026-05-05: user confirmed HARN-064 formal materialization, single-agent Main Foreman routing, D-STORY-010/P1/no explicit dependency candidate binding, required statistics dimensions, parsing-history coverage, and repository-confirmed API/data/UI contract adaptation where necessary.
- Data impact: Potential read/query and aggregation impact on batch parsing results, report-import parsing records, parse history records, SQL-level statistics, logical-object statistics, and any persisted/cached statistics model if confirmed. No data migration or persistence change is authorized by this candidate pack alone; any schema or historical data backfill decision requires explicit confirmation during implementation planning.
- Rollback / recovery: Keep changes scoped to HARN-064. If implementation introduces API/UI/statistics contract regressions, rollback by reverting the HARN-064 single-task commit and restoring prior parsing/history behavior. If schema or persisted statistics changes are later approved, implementation must include explicit rollback/backfill recovery notes before closeout. Use foreman validate, pre-closeout audit, closeout, and post-closeout audit before delivery of the standard task.
- Validation:
  - `Aggregation coverage for all required dimensions: 问题场景, 重要程度, 报表视角, SQL 清单, 优先级视角, 逻辑对象视角、Batch-selected parsing statistics happy path、Parsing history statistics lookup/display path、Empty batch, missing statistics, failed/partial parse, and permission/error state coverage as applicable、Regression coverage against existing SQL parsing statistics behavior where reusable contract is confirmed、Documentation/contract validation for confirmed statistics fields and口径`
  - `python3 scripts/foreman.py validate HARN-064`
- Progress log:
  - 2026-05-05: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a shared report-batch parse-statistics contract for batch detail and parse-history lookup, covering issue scenes, importance, report view, SQL list, priority matrix, and logical-object dimensions across backend API, frontend views, tests, and contract checks.
  - Validation evidence: python3 scripts/foreman.py validate HARN-064 --extra-command node/scripts/check-batch-import-contract.mjs --extra-command node/scripts/check-history-page-contract.mjs --extra-command mvn-report-batch-statistics-tests --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No schema migration or historical backfill was introduced; live environments still depend on existing report_batch/report_batch_item data quality and governance history availability for per-SQL drill-through.
  - Next step: In acceptance, open a resolved report-import batch from both the batch center and parse history and verify the six statistics dimensions match the same batch.

### HARN-063: HARN-063 批量解析报表结果与失败详情可见性修复

- Status: done
- Completed at: 2026-05-05
- Commit subject: `fix(frontend): expose batch report parse details`
- Priority: 1
- Depends on: `N/A`
- Scope: 用户确认该 candidate task pack 后，Main Foreman 才可按 SQLForge 标准治理流程执行 preflight、instantiate、实现、validate、audit、closeout 与单任务单 commit。实现必须保持失败、成功及其他解析状态的结果展示与详情入口语义一致，并不得在未确认字段、权限或页面边界前固化具体实现事实。 Tech: `待实例化后基于仓库上下文确认具体前端页面、后端接口、数据结构与测试框架`,`代码变更`,`自动化测试`,`文档更新`. Layer: `UI/交互层：批量解析列表或相关视图的结果展示与详情入口`,`应用/接口层：解析结果与详情数据获取或传递路径，具体范围待确认`,`测试层：关键用户路径与状态一致性覆盖`,`文档层：批量解析结果与详情查看行为说明`.
- Plan ref: docs/exec-plans/completed/HARN-063-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: User confirmed the HARN-063 execution preview at 2026-05-05T01:32:57-05:00. Main Foreman may materialize and execute the standard task within the confirmed boundary: batch-parse report import result/detail visibility, failed-record detail access, repository-discovered fields and docs, no permission/import-format/core-parser/data-model expansion without separate confirmation.
- Data impact: 预期主要影响解析结果与解析详情的可见性展示，不应改变批量解析核心算法、导入格式、持久化语义或权限体系；若实例化后发现必须改动数据结构、权限或异步任务架构，需回到 human confirmation。
- Rollback / recovery: 通过单任务单 commit 保持可回滚边界；若实现引入展示、入口或详情数据回归，应回滚 HARN-063 相关代码、测试与文档变更，并保留审计链记录。closeout 前必须完成 validate 与 pre-closeout audit，closeout 后必须完成 post-closeout audit。
- Validation:
  - 失败解析记录可点击并展示对应解析详情、批量解析结果列表展示成功、失败及其他状态的结果信息、解析状态与详情入口可用性保持一致、解析详情展示足够定位导入解析问题的信息，字段需经确认、相关回归测试覆盖详情入口、结果展示与异常状态
  - `python3 scripts/foreman.py validate HARN-063`
- Progress log:
  - 2026-05-05: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated ParseBatchCenterView so failed parse records, report groups, failed report SQL rows and SQL-level result rows open detail dialogs; added contract/backend regression coverage and docs.
  - Validation evidence: python3 scripts/foreman.py validate HARN-063 --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest test' --extra-command 'npm run lint' --extra-command 'npm run build'
  - Residual risk: No live browser E2E against a real failed report import environment; detail content depends on existing parse/report batch API fields.
  - Next step: Verify with a real failed report import in the target environment and confirm the detail dialogs show the expected troubleshooting fields.

### HARN-062: HARN-062 / D-STORY-009 批量报表导入多 SQL 列解析修复

- Status: done
- Completed at: 2026-05-04
- Commit subject: `fix(sql-optimization): parse report batch wide SQL columns`
- Priority: 1
- Depends on: `N/A`
- Scope: 对每一行导入数据，第一列必须稳定解析为 report code；从第二列开始遍历该行所有列，每个非空单元格都必须作为该 report code 下的一条 SQL；空单元格必须跳过；不得依赖固定 SQL 列数，需覆盖 100+ 后续列场景；测试和文档需与该规则保持一致。 Tech: `待 preflight 后确认具体实现栈与导入解析模块`,`批量导入解析逻辑`,`表格/CSV/XLSX 行列遍历逻辑`,`单元测试或集成测试，按仓库现有测试框架执行`,`相关导入规则文档`. Layer: `application/import-parsing`,`domain/report-sql-mapping`,`tests`,`docs`.
- Plan ref: docs/exec-plans/completed/HARN-062-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: User confirmed the HARN-062 execution preview at 2026-05-04T23:12:52-05:00. Main Foreman materialization boundary: single-agent execution; reuse existing import formats and parser entry points discovered during preflight; treat cells whose trimmed text is empty as empty; preserve every non-empty SQL cell as one SQL text for the row report code, including punctuation, quotes, semicolons and line breaks when the existing reader preserves them; update only related code, tests and docs; do not add UI, permissions, new import formats or report data-model changes. If implementation proves a report data-model change is unavoidable, pause for separate confirmation.
- Data impact: 解析行为变更会影响后续批量导入结果：同一行后续所有非空列将被导入为多条 SQL，可能增加解析出的 SQL 数量；不涉及既有持久化数据迁移，除非实现阶段发现当前导入流程会立即写入数据库并需额外确认。
- Rollback / recovery: 通过单任务单 commit 收口；如验证失败或行为不符合确认规则，回滚 HARN-062 相关代码、测试和文档变更，并保留 audit/validation 记录。若已产生导入数据副作用，需按实际持久化路径制定数据回退步骤并由人类确认。
- Validation:
  - `仅一列 SQL：第一列为 report code，第二列非空 SQL 被解析、多列 SQL：第二列及之后多个非空单元格均被解析为同一 report code 下的 SQL、空列跳过：中间或尾部空单元格不产生 SQL、100+ 后续列：不丢列、不串行、不依赖固定列数、回归测试：证明当前只识别一列 SQL 的问题被修复或规避、如存在文档示例或 fixtures，同步校验示例与新规则一致`
  - `python3 scripts/foreman.py validate HARN-062`
- Progress log:
  - 2026-05-04: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Updated report batch CSV/workbook import parsing so column 1 is report code and every non-empty column after it becomes one SQL item; added CSV/XLSX regression coverage and docs.
  - Validation evidence: python3 scripts/foreman.py validate HARN-062 --extra-command 'mvn -pl sql-optimization -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest test' --extra-command 'node scripts/check-batch-import-contract.mjs'
  - Residual risk: TXT pipe/mock fallback remains legacy; report rows without inline SQL continue through resolver/mock source.
  - Next step: Use the committed HARN-062 behavior for batch report imports and verify with a real user workbook/CSV in the target environment.

### OPS-LOCAL-006: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-006`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-006; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-061: 重构批量解析当前批次工作台与报表宽表详情

- Status: done
- Completed at: 2026-04-29
- Commit subject: `feat(sql-optimization): support report wide sql batches`
- Priority: 1
- Depends on: HARN-060
- Scope: 实现批量解析页当前批次工作台：普通批量解析与报表导入只展示当前批次输入、批次、概览、解析结果弹窗、解析统计弹窗；报表导入支持 report_code 后续多 SQL 列宽表导入并按报表->SQL 上下结构展示，解析历史页报表导入详情支持报表分组、SQL 懒加载解析详情与批次/报表/SQL 统计；不改核心 SQL parser 算法，不弱化 tenant/request/trace 权限链路。
- Validation:
  - `python3 scripts/foreman.py validate HARN-061`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 实现报表导入宽表解析：report_code 后续多 SQL 列会拆成同一报表下多条 SQL item，响应补充 SQL 级统计与 SQL 列定位；批量解析页重构为当前批次工作台，主体只展示当前输入、批次概览、解析结果弹窗和解析统计弹窗；解析历史页报表导入详情改为报表分组，并按 SQL 懒加载治理解析详情。
  - Validation evidence: python3 scripts/foreman.py validate HARN-061 --extra-command node/scripts/check-batch-import-contract.mjs --extra-command node/scripts/check-history-page-contract.mjs --extra-command node/scripts/check-history-detail-contract.mjs --extra-command mvn-report-batch-tests --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做浏览器端到端验收；当前覆盖为后端单元/controller/schema 测试、前端契约、lint、生产构建与治理审计。
  - Next step: 实机验收时用包含 100+ SQL 列的报表导入模板创建批次，确认批量解析页当前批次工作台与解析历史页报表分组/SQL 懒加载详情都符合预期。

### OPS-LOCAL-005: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-005`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-005; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-060: 补齐报表导入解析历史 SQL 明细

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(frontend): hydrate report import parse details`
- Priority: 1
- Depends on: HARN-059
- Scope: 修复解析历史页中报表导入批次详情只展示简略 SQL 明细的问题；报表导入每个报表、每条解析 SQL 都应展示参考 SQL 解析页面的解析统计、结构解析、Access Parse、风险/问题与原始 SQL 信息，不改变核心 parser 算法、权限边界或历史保留策略。
- Validation:
  - `python3 scripts/foreman.py validate HARN-060`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-29: added report-import SQL detail hydration in parse history by resolving each report item parseTaskId to its governance parse-history detail.
  - 2026-04-29: expanded the report-import drawer to show per-report/per-SQL parse statistics, structure parse, Access Parse, risks, issues, and original SQL evidence.
- Context closeout:
  - Completed scope: 修复解析历史页报表导入批次详情：打开报表导入抽屉时按每个 report item 的 parseTaskId 回查治理解析历史详情，并在每个报表/SQL 卡片内展示原始 SQL、解析统计、结构解析、Access Parse、风险与问题清单，同时保留完整解析历史跳转。
  - Validation evidence: python3 scripts/foreman.py validate HARN-060 --extra-command node/scripts/check-history-page-contract.mjs --extra-command node/scripts/check-history-detail-contract.mjs --extra-command node/scripts/check-batch-import-contract.mjs --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做浏览器端到端验收；当前覆盖为前端 contract、lint、生产构建与治理审计。若某条 SQL 的 parseTaskId 没有写入 governance query_history，页面会显示详情缺失提示。
  - Next step: 在实机验收时执行报表导入解析后从解析历史页打开该报表批次，确认每个报表 SQL 的 Loaded details 计数与 parseTaskId 数量一致。

### HARN-059: 修复解析历史详情展示解析结果与原始SQL

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(frontend): enrich parse history detail`
- Priority: 1
- Depends on: HARN-058
- Scope: 修复解析历史单条详情只展示 JSON、缺少 SQL 解析页同款解析结果与统计信息的问题；解析历史详情必须展示原始 SQL，并复用 SQL 解析结果/统计口径，不改变核心 parser 算法、权限边界或持久化保留策略。
- Validation:
  - `python3 scripts/foreman.py validate HARN-059`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-29: added a structured parse-result detail tab to parse history, defaulted history detail drill-through to the parse result view, and surfaced original SQL in the single-record detail.
  - 2026-04-29: validated frontend contracts and production build for parse history/detail changes.
- Context closeout:
  - Completed scope: 修复解析历史单条详情展示：详情默认进入结构化解析结果页签，展示原始 SQL、解析结果摘要、解析统计、结构解析卡、Access Parse 卡、风险/问题清单，同时保留原始 JSON 证据页签。
  - Validation evidence: python3 scripts/foreman.py validate HARN-059 --extra-command node/scripts/check-history-detail-contract.mjs --extra-command node/scripts/check-history-page-contract.mjs --extra-command node/scripts/check-parse-workbench-contract.mjs --extra-command npm-run-lint --extra-command npm-run-build; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做浏览器端到端验收；当前覆盖为静态契约、lint、生产构建与治理审计。
  - Next step: 如目标环境仍看不到原始 SQL，优先核对对应 query_history 记录是否已通过加密 SQL surface 写入 sql_text_cipher。

### OPS-LOCAL-004: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-004`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-004; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-058: 修复解析历史记录投影与报表导入明细

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(governance): repair parse history traceability`
- Priority: 1
- Depends on: HARN-057
- Scope: 修复单条 SQL、批量解析和报表导入解析记录在解析历史页不可识别/不可筛选的问题，补齐 query_history 投影上下文、报表导入 SQL 级与报表级解析明细展示、测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-058`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 SQL parse history 的 config snapshot/reference 链、query_history upsert 与 query_context 归一化投影；补齐批量/报表 access 解析历史合并、报表导入 SQL 级与报表级详情、解析历史页报表导入抽屉和 SQL 解析页 historyId 跳转。
  - Validation evidence: mvn -pl governance -Dtest=GovernanceParseHistoryTraceabilityApplicationServiceTest,GovernanceProtectedPersistenceServiceTest,TraceabilitySchemaMappingTest clean test; mvn -pl sql-optimization -Dtest=ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest clean test; node scripts/check-history-page-contract.mjs; node scripts/check-history-detail-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-parse-workbench-contract.mjs; npm run lint; npm run test:form-governance; npm run build; python3 scripts/foreman.py validate HARN-058; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未连接真实外部 Hetu/MRS 环境做端到端页面冒烟；当前覆盖为后端单元、前端 contract、lint/build 与治理审计。
  - Next step: 如需实机验收，在本地/测试环境执行单条综合解析、批量 SQL 导入和报表导入解析，确认解析历史页可按 report/stage/bizDate 筛选并打开详情。

### OPS-LOCAL-003: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-003`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-003; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-057: 修复解析历史默认查询与刷新

- Status: done
- Completed at: 2026-04-29
- Commit subject: `fix(frontend): repair parse history refresh defaults`
- Priority: 1
- Depends on: HARN-056
- Scope: 修复解析历史页默认查询条件非空和刷新失败问题，分离页面筛选条件与请求上下文租户，补齐批量历史刷新、测试与文档。
- Validation:
  - `node scripts/check-history-page-contract.mjs`
  - `npm run build`
  - `python3 scripts/foreman.py validate HARN-057`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-29: captured raw requirement and active execution plan for parse-history default-empty filters and refresh repair.
  - 2026-04-29: separated parse-history visible tenant filter from request context tenant, reset tenant/sort defaults to empty, and hardened batch history refresh.
  - 2026-04-29: validation passed with node scripts/check-history-page-contract.mjs, npm run test:form-governance, npm run lint, npm run build, node scripts/lint-repository-knowledge.js, and python3 scripts/foreman.py validate HARN-057.
- Context closeout:
  - Completed scope: 修复解析历史页默认筛选与刷新：可见 tenantId、sortBy、sortOrder 默认保持空值；query-history page API 区分筛选租户与请求上下文租户；批量解析与报表导入历史刷新使用有效上下文租户；补齐历史页静态契约检查、表单治理文档和 HARN-057 执行追溯。
  - Validation evidence: node scripts/check-history-page-contract.mjs; npm run test:form-governance; npm run lint; npm run build; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate HARN-057; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 未运行真实后端浏览器 smoke；本任务未改后端持久化逻辑，验证覆盖前端构建、lint、静态契约和治理链路。
  - Next step: 如目标环境仍显示空列表，优先核对当前请求上下文租户下是否已有 query_history、parse_batch 或 report_batch 数据。

### OPS-LOCAL-002: 重新启动前后端服务

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): restart local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Restart local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-002`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restarted the local Docker-backed infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: python3 scripts/foreman.py validate OPS-LOCAL-002; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev services remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the services running for user access or stop them with the repository stop scripts when finished.

### HARN-056: 解析历史落库与批量解析中心重构

- Status: done
- Completed at: 2026-04-29
- Commit subject: `HARN-056: 解析历史落库与批量解析中心重构`
- Priority: 1
- Depends on: N/A
- Scope: 所有从SQL解析产生的记录（结构解析、后端解析、批量解析、文件导入解析）统一落库并在解析历史展示；修复批量解析中心可用性；批量解析中心报表上传自动识别文件类型；批量解析支持页面多SQL输入和文件导入两种入口并展示结果；文件导入解析进入解析历史；批量解析与文件导入提供解析统计，参考SQL解析统计；同步代码、测试、文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-056`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Persisted parse batch and report batch histories, added history list endpoints, auto-detected report import file types, and rebuilt parse history/batch center pages.
  - Validation evidence: python3 scripts/foreman.py validate HARN-056; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -q -pl sql-optimization -Dtest=ParseBatchControllerTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest test; npm run build
  - Residual risk: No historical backfill was performed for legacy parse records outside the current persisted batch tables.
  - Next step: Monitor live usage and backfill legacy parse history only if product owners need prior records surfaced.

### OPS-LOCAL-001: 启动前后端

- Status: done
- Completed at: 2026-04-29
- Commit subject: `chore(ops): start local frontend and backend`
- Priority: 2
- Depends on: N/A
- Scope: Start local backend services and frontend dev server for the current workspace session.
- Validation:
  - `python3 scripts/foreman.py validate OPS-LOCAL-001`
- Progress log:
  - 2026-04-29: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Started local infrastructure, backend runtime services, and frontend dev server for the current workspace session.
  - Validation evidence: scripts/local-start.sh; scripts/start-backend-services.sh --reuse-running-stack --skip-build; scripts/health-check.sh --fail-on-error
  - Residual risk: Long-running dev servers remain active in separate sessions and should be stopped explicitly when no longer needed.
  - Next step: Keep the dev sessions running for user access or stop them with the repository stop scripts when finished.

### HARN-052: 修复 SQL解析运行时 500

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(sql-optimization): restore combined parse runtime`
- Priority: 1
- Depends on: N/A
- Scope: 重启并验证 sql-optimization 运行实例，修复 SQL解析 在综合/结构解析请求下返回 500 的运行时故障，确保当前源码和线上实例一致。
- Validation:
  - `python3 scripts/foreman.py validate HARN-052`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 StructureParseApplicationService 缺失治理解析历史 DTO 导入导致 clean build/runtime 综合解析返回 [10000] 的问题；验证结构解析、访问解析和综合解析 HTTP 200。
  - Validation evidence: mvn -B -f sql-optimization/pom.xml clean test -Dtest=StructureParseControllerTest,AccessParseControllerTest; python3 scripts/foreman.py validate HARN-052; curl POST http://127.0.0.1:8082/api/sql-optimization/parse/combined returned HTTP 200.
  - Residual risk: 本地治理历史写入仍可能因治理服务权限/数据状态降级为 WRITE_FAILED，但解析主流程已捕获降级且不再返回 500。
  - Next step: 如需恢复解析历史落库，单独排查治理 /parse-history/write 的权限与数据依赖。

### HARN-051: 压缩 SQL解析顶部信息卡

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): simplify SQL Parse top cards`
- Priority: 1
- Depends on: N/A
- Scope: 收敛 SQL解析 页面顶部的冗余信息卡，去掉重复的 workspace entry 与 hero summary chips，只保留核心操作入口和结果区。
- Validation:
  - `python3 scripts/foreman.py validate HARN-051`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed the redundant hero summary chips and the single workspace entry card from the SQL Parse page, leaving the core action buttons, input form, and result/statistics areas intact.
  - Validation evidence: python3 scripts/foreman.py validate HARN-051; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The main parse workspace still shows request-summary chips near the form, which remain useful context rather than top-level navigation.
  - Next step: None.

### HARN-050: SQL解析单条页面收口

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): rename parse workbench to SQL Parse`
- Priority: 1
- Depends on: N/A
- Scope: 将解析工作台收束为单条 SQL 解析页面，去除批量/历史入口，补齐字段帮助、统计展示、风险清单本地化与单 SQL 历史写入。
- Validation:
  - `python3 scripts/foreman.py validate HARN-050`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Renamed the single-parse workbench to SQL解析, removed visible batch/history entrances from the page, added a field-help dialog and denser structure-card layout, localized risk text, persisted single SQL structure parse history, and refreshed validation contracts/tests/docs.
  - Validation evidence: python3 scripts/foreman.py validate HARN-050; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Legacy batch and history routes still exist elsewhere in the app shell for compatibility, but the SQL解析 page no longer exposes them as entrances.
  - Next step: None.

### HARN-049: HARN-049 解析工作台、批量解析与解析历史能力正式实现

- Status: done
- Completed at: 2026-04-28
- Commit subject: `HARN-049 split parse workbench history pages`
- Priority: 1
- Depends on: `N/A`
- Scope: 实现后，单条解析页面采用上下结果布局，合并总结/结论为统一“解析结果”结构，移除重复标题；urgent=true 与 priority=P1 有明确保守红色提示；中文自然语言为主，代码、字段名、缩写和英文技术标识提供 tooltip 或小按钮解释入口；解析完成记录写入历史存储，并可在独立解析历史查询页面检索和查看；解析工作台、批量解析、解析历史查询路由、状态和业务逻辑互不耦合。 Tech: `existing frontend routing/page stack`,`existing frontend state management pattern`,`existing UI/design token or status color convention`,`existing persistence/storage layer`,`existing query/detail-vi...
- Plan ref: docs/exec-plans/completed/HARN-049-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: Main Foreman 已在 HARN-049 preflight 后确认 materialization 边界：任务绑定 E-STORY-008 / Phase-E；实现复用现有 ROUTE_PATHS、ParseBatchCenterView、ParseRecordView、query_history、parse_batch、parse_batch_item 等当前仓库真值；新增或调整持久化仅限解析历史闭环所需字段、接口和兼容性补充；若发现必须改变权限、保留、脱敏策略或核心 parser 算法，则暂停并另行确认。
- Data impact: 本任务不引入新的敏感数据类别和独立保留策略；解析历史闭环优先复用当前仓库已有 query_history 查询面、parse_batch / parse_batch_item 批量解析证据表和既有 tenant_id 隔离、分页、审计追溯语义。若实现需要补充字段或接口，只允许做兼容性新增，并继续遵守 R-031 至 R-038 的 MySQL 主持久化、后端历史查询、不可依赖浏览器临时状态、历史默认保留与敏感信息不得明文落库规则。解析 SQL 文本按现有历史/批量证据模型处理，不在前端新增本地持久化副本。
- Rollback / recovery: 保留核心 parser 算法不变；若页面拆分或历史能力异常，可回退 HARN-049 单任务 commit，并通过治理台账记录回滚；若涉及数据库迁移，应提供可逆迁移或兼容性降级方案，确保旧解析流程仍可运行且不阻断单条解析。
- Validation:
  - `页面拆分与独立路由测试：解析工作台、批量解析、解析历史查询互不串状态、单条解析结果上下布局与重复标题移除测试、总结/结论合并为统一解析结果结构的展示测试、urgent=true、priority=P1 及其他状态颜色提示测试、中文展示与代码/字段/缩写 help 入口测试、解析完成后历史记录持久化测试、解析历史查询与详情查看关键路径测试、治理验证：foreman validate、pre-closeout audit、post-closeout audit`
  - `python3 scripts/foreman.py validate HARN-049`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Split parse workbench, batch parse, and parse history into independent pages; improved single-parse result layout, result notes, color cues, Chinese-first labels, and help tooltips; added parse-history persistence through governance query history.
  - Validation evidence: node scripts/check-parse-workbench-contract.mjs; node scripts/check-batch-import-contract.mjs; node scripts/check-history-page-contract.mjs; node scripts/check-navigation-shell-contract.mjs; npm run lint; npm run build; mvn -B -pl sqlforge-shared,governance,sql-optimization -am test; python3 scripts/foreman.py validate HARN-049; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No known functional blockers. Frontend build still reports the existing Vite CJS API deprecation warning.
  - Next step: Use the three independent pages for manual runtime smoke with a live backend and database when service environment is available.

### D-TASK-075: 补强复杂反模式 SQL 结构解析验证

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): D-TASK-075 detect complex SQL antipatterns`
- Priority: 1
- Depends on: `D-TASK-073`,`D-TASK-074`
- Scope: 结构解析必须在不执行 SQL、不访问元数据的前提下，对复杂嵌套 SQL 输出稳定的静态结构特征、查询意图标签、风险清单和启发式资源估算；新增风险码不能破坏 D-TASK-073/074 旧响应字段。 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain`,`shared contract docs`.
- Plan ref: docs/exec-plans/completed/D-TASK-075-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若实现需要执行用户 SQL、访问真实数据库/元数据、断言真实索引存在性、引入持久化字段或改变结构解析旧字段语义，必须暂停并由人工确认。
- Data impact: 不变更持久化模型；影响结构解析 API 响应中的新增/更丰富风险标签、issues、featureSummary 计数和启发式资源估算。
- Rollback / recovery: 回退递归 AST 特征提取、复杂 SQL 测试、风险码映射和文档补充，恢复 D-TASK-074 后的结构解析行为；不触碰历史数据。
- Validation:
  - `SqlOptimizationPipelineService complex SQL profile test、StructureParseController complex anti-pattern contract test、sql-optimization module tests、task audit、knowledge lint`
  - `python3 scripts/foreman.py validate D-TASK-075`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added expected-vs-system regression coverage for the supplied complex anti-pattern SQL, extended recursive JSQLParser traversal for nested/select/where subqueries, correlated alias detection, function-wrapped predicates, leading wildcard LIKE, OR predicates, ORDER BY random and repeated table scans, and surfaced the new counters through intent profile, feature summary, risk tags, checklist, issues and resource estimates.
  - Validation evidence: mvn -B -pl sql-optimization -am -Dtest=SqlOptimizationPipelineServiceTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipITs; mvn -B -pl sql-optimization -am -Dtest=StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipITs; mvn -B -pl sql-optimization -am test -DskipITs; mvn -B -pl sql-optimization -am validate pmd:pmd checkstyle:check -DskipTests; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate D-TASK-075; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Signals remain static AST heuristics without catalog metadata, real index truth or execution-plan cost proof; Trino path remains compatibility-oriented and was not expanded to identical nested anti-pattern extraction in this task.
  - Next step: Add metadata-backed access/index evidence and a Trino-specific complex fixture if query-intent scoring needs engine-parity validation.

### D-TASK-074: 补齐结构解析 SQL 指纹前处理契约

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(sql-optimization): D-TASK-074 stabilize SQL fingerprints`
- Priority: 1
- Depends on: `D-TASK-073`
- Scope: 结构解析 sqlFingerprint 必须基于去注释、字面量参数化、大小写和空白标准化后的 SQL 形态生成；该指纹用于治理聚合提示，不代表 SQL 语义等价证明。 Tech: `JAVA-BE`,`DOCS`. Layer: `shared/utils`,`application(controller/service)`,`docs`.
- Plan ref: docs/exec-plans/completed/D-TASK-074-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若修复需要历史 SQL 指纹回填、跨服务缓存迁移、数据库字段变更或把 fingerprint 解释为严格 SQL 语义等价证明，必须暂停并由人工确认。
- Data impact: 不变更持久化模型；新请求生成的 sqlFingerprint 会对注释和字面量变化更稳定。历史已落库指纹不在本任务中回填或迁移。
- Rollback / recovery: 回退 SqlFingerprintUtils 前处理增强、相关测试和文档补充，恢复 D-TASK-073 后的指纹行为；不触碰历史数据。
- Validation:
  - `sqlforge-shared utility tests、structure parse controller regression、sql-optimization module tests、task audit、knowledge lint`
  - `python3 scripts/foreman.py validate D-TASK-074`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 复盘 D-TASK-073 后补齐 SQL 指纹前处理契约：共享 fingerprint 工具现在去除 SQL 注释、参数化字符串/数字/命名参数字面量、折叠空白、统一大小写并忽略末尾分号；结构解析接口回归验证注释和字面量变化不会改变 sqlFingerprint；产品规格明确该指纹只用于治理聚合，不代表完整 SQL 语义等价。
  - Validation evidence: mvn -B -pl sqlforge-shared -Dtest=SqlFingerprintUtilsTest test; mvn -B -pl sql-optimization -am -Dtest=StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test -DskipITs; mvn -B -pl sql-optimization -am test -DskipITs; mvn -B -pl sql-optimization -am validate pmd:pmd checkstyle:check -DskipTests; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate D-TASK-074; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: SQL 指纹仍是静态文本级治理聚合，不代表完整 SQL 语义等价；历史已生成 fingerprint 不在本任务中回填；方言特有字面量后续可继续补样本。
  - Next step: 如后续需要跨服务缓存迁移或历史 fingerprint 重算，应单独任务化并评估数据迁移与回滚策略。

### D-TASK-073: 升级结构解析查询意图理解与双 parser 抽象

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(sql-optimization): D-TASK-073 add query intent parsing`
- Priority: 1
- Depends on: `D-TASK-045`,`D-TASK-051`,`E-TASK-020`
- Scope: 结构解析在原有字段兼容基础上输出 SQL 指纹、查询意图标签、多维特征、风险清单与启发式资源估算，底层 parser 通过 adapter 抽象可配置选择，不执行 SQL 或访问生产数据。 Tech: `JAVA-BE`,`VUE-FE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`frontend/router/views/styles`,`docs`.
- Plan ref: docs/exec-plans/completed/D-TASK-073-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若 Trino parser 依赖引入导致许可证、包冲突或大规模迁移，或查询意图理解会破坏旧结构解析响应、执行 SQL、访问生产数据、把启发式资源估算写成真实执行计划结论，需人工确认。
- Data impact: 预期不变更持久化数据模型；影响结构解析 API 响应字段、前端展示契约、解析规则与文档说明。若实现需要新增数据库字段或改变 access parse 权限/元数据行为，必须升级为人类确认点后再推进。
- Rollback / recovery: 回退新增 parser adapter、查询意图字段、风险/资源估算规则和前端展示块，恢复 D-TASK-045 / E-TASK-020 既有结构解析响应与解析工作台展示基线；保留文档更正记录与测试证据。
- Validation:
  - `sql-optimization 模块测试、structure parse contract/controller 测试、parse workbench contract 测试、npm run build、npm run lint、task audit、knowledge lint`
  - `python3 scripts/foreman.py validate D-TASK-073`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added structure-parse query intent outputs, SQL fingerprint, feature summary, structured risk checklist, heuristic resource estimate, configurable JSQLParser/Trino parser adapter path, parse workbench display, contract tests, parser tests, frontend contract checks, and product documentation.
  - Validation evidence: mvn -B -pl sql-optimization -am test -DskipITs; node scripts/check-parse-workbench-contract.mjs; npm run lint; npm run build; mvn -B -pl sql-optimization -am validate pmd:pmd checkstyle:check -DskipTests; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate D-TASK-073; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Trino parser adapter is covered by repo-local samples and should gain more dialect fixtures over time; resource cost remains static heuristic evidence, not a real execution plan; NL2SQL remains a future task.
  - Next step: Use the new intent profile as the backend contract for future NL2SQL and recommendation work without moving metadata or permissions into structure parse.

### HARN-047: 修复日期区间选择器页面不可用

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(frontend): register semantic date components`
- Priority: 1
- Depends on: HARN-046
- Scope: 复现并修复 ParseRecordView 日期/日期时间区间选择器在页面上不可用的问题；用浏览器级验证确认可打开、可选择并提交前拆回既有字段；同步测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-047`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修复 HARN-046 页面日期区间不可用问题：在 Vue 入口显式注册 Element Plus 的 ElDatePicker 与 ElInputNumber，并扩展表单治理静态检查覆盖运行时组件注册。
  - Validation evidence: npm run test:form-governance; npm run lint; npm run build; system Chrome Playwright smoke opened ParseRecordView date range panel and selected 2026-04-06..2026-04-10; python3 scripts/foreman.py validate HARN-047; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Playwright packaged browser未安装，浏览器级复现使用系统 /usr/bin/google-chrome；后续若 CI 需要自动化运行，应先补浏览器安装或改用已有 smoke 基础设施。
  - Next step: 继续页面组件治理时，所有新增 Element Plus 组件必须同步 src/main.js 注册或改为统一插件注册方式。

### HARN-046: 修正 HARN-045 日期区间组件治理

- Status: done
- Completed at: 2026-04-28
- Commit subject: `fix(frontend): use range pickers for history dates`
- Priority: 1
- Depends on: HARN-045
- Scope: 修正 HARN-045 后续缺陷：分析已改页面日期字段语义，将需要范围筛选的日期改为日期区间/日期时间区间组件，保持 API 字段名和提交格式兼容；更新测试与文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-046`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 修正 HARN-045 日期组件治理：ParseRecordView 的 queryDateStart/queryDateEnd 改为日期区间，submittedStart/submittedEnd 改为日期时间区间；提交前仍拆回原 API 字段；保留 bizDate 单日业务日选择；同步静态验证和表单治理文档。
  - Validation evidence: npm run test:form-governance; npm run lint; npm run build; python3 scripts/foreman.py validate HARN-046; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: 仅修正 HARN-045 已改的 ParseRecordView 日期字段；其他页面日期字段如需继续区间化，应按表单治理文档另行分批处理。
  - Next step: 如需要继续治理 AccelerationView 中同类历史筛选日期字段，建议按 HARN-046 的区间映射规则单独开 follow-up。

### HARN-045: HARN-045 页面组件语义治理执行模板

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(frontend): govern semantic form components`
- Priority: 1
- Depends on: `N/A`
- Scope: 用户已确认执行模板，Main Foreman 可 materialize HARN-045 并进入实现流程。后续实现必须先执行 preflight，再通过 foreman instantiate/validate/closeout 与 task_audit pre-closeout/post-closeout 完成治理链路。输出物必须包含代码、测试、文档；不得破坏既有业务流程、数据提交格式、默认值、回显、校验行为和权限边界。 Tech: `Frontend form components`,`DateTime picker`,`Select / searchable select`,`Switch / checkbox`,`Number input`,`Form validation`,`API schema alignment`,`Automated UI/form tests`,...
- Plan ref: docs/exec-plans/completed/HARN-045-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 用户已在 2026-04-28 对 HARN-045 执行模板、任务边界、输出物要求以及 Main Foreman 创建正式任务并进入实现流程作出明确确认。实现过程中若字段语义、候选值来源、权限边界或提交格式无法从权威材料确认，必须暂停请用户再次确认。
- Data impact: 预期不变更持久化数据模型和后端接口契约；风险集中在前端表单提交格式、默认值、回显、校验和候选值过滤。若实现需要改变 API contract、数据格式或权限行为，必须升级为人类确认点后再推进。
- Rollback / recovery: 普通 standard 任务按单任务单 commit 管理。若组件替换造成行为回归，优先通过该任务 commit 回退或在同一治理链路内做最小修复；文档和测试变更需与代码回退保持一致。不得使用 git reset --hard、git add .、git add -A、git commit -a 等破坏审计链的命令。
- Validation:
  - 覆盖日期时间组件选择、默认值、回显和提交格式、覆盖租户、数据源等受控候选字段的选择、候选值加载和提交、覆盖布尔、数值、枚举、关联资源等字段的关键交互与边界输入、覆盖表单加载、编辑、校验、提交、回显的主流程、覆盖候选值加载失败或权限不可见时的保守行为，若仓库现有测试体系支持
  - `python3 scripts/foreman.py validate HARN-045`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: SystemView 与 ParseRecordView 页面表单组件语义治理：租户/数据源下拉、日期/日期时间选择、枚举 select、数值 input-number、布尔 switch、敏感凭证 password input，并新增表单治理文档与静态验证脚本。
  - Validation evidence: npm run test:form-governance; npm run lint; npm run build; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate HARN-045; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: HARN-045 未改写无法从代码或文档确认语义的自由文本字段；数据源候选加载失败时保留 allow-create 手动值以避免权限不可见场景回归。
  - Next step: 如需继续治理 AccelerationView、ParseBatchCenterView 等剩余页面，按同一文档基线另开任务分批处理。

### D-TASK-039: 扩展 SQL 查询执行摘要契约

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(query-execution): complete D-TASK-039 summary contract`
- Priority: 1
- Depends on: `D-TASK-038`
- Scope: 查询执行返回 comment context、binding summary、query-date、logical object hits、route/cache summary 与 lightweight parse summary Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若查询执行摘要契约会改变既有受保护请求、失败语义或让 comment/binding/logical-object 信息在未校验时对外暴露，需人工确认
- Data impact: 查询执行响应、审计摘要、前后端契约
- Rollback / recovery: 保留原执行与错误响应路径，新增字段可降级为空，不删除旧字段
- Validation:
  - `query-execution controller/service 契约测试`
  - `python3 scripts/foreman.py validate D-TASK-039`
- Progress log:
  - 2026-04-28: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended query-execution synchronous responses with comment context, binding summary, query-date summary, logical object hits, route/cache summaries, and lightweight parse summary surfaces; added deterministic application-layer summary builders and locked the contract with controller/service tests.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-039 --include-task-audit --extra-command 'mvn -pl query-execution -Dtest=QueryExecutionApplicationServiceTest,QueryExecutionControllerTest,QueryExecutionBenchmarkWorkloadServiceTest test'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Logical-object and lightweight-parse summaries are intentionally lightweight SQL-token heuristics for the synchronous query path; deeper parser-grade semantics remain owned by the sql-optimization parse surfaces and follow-on query/history work.
  - Next step: Instantiate D-TASK-040 next so governance history list/detail surfaces consume the now-exposed query execution summary contract end to end.

### F-TASK-042: 落地回归守护统计与告警

- Status: done
- Completed at: 2026-04-28
- Commit subject: `feat(benchmark): add regression guard alert linkage`
- Priority: 1
- Depends on: `F-TASK-041`,`F-TASK-037`
- Scope: regression summary、threshold hit 与 alert linkage Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `E-STORY-011` 压测中心与开放接入页
- Human confirmation point: 若回归守护统计与告警会把实验性 benchmark 结果提升为默认生产风险判定，需人工确认
- Data impact: regression summary、threshold hit 与 alert linkage
- Rollback / recovery: 恢复为显式模板/测试集范围内的回归守护，不扩大默认告警面
- Validation:
  - `regression/alert linkage 测试`
  - `python3 scripts/foreman.py validate F-TASK-042`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended benchmark report/domain/persistence contracts with regression summaries and governance alert linkages; added the governance internal benchmark-regression alert emit endpoint and failed-threshold-only regression guard emission flow; updated benchmark/governance tests, schema migration, and contract baseline documentation.
  - Validation evidence: mvn -pl benchmark-engine -am -DskipITs -Dtest=BenchmarkTaskModelApplicationServiceTest,BenchmarkRegressionAlertServiceTest,BenchmarkTaskWorkerTest,MybatisBenchmarkTaskRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl governance -am -DskipITs -Dtest=AlertRuleApplicationServiceTest,AlertEmissionApplicationServiceTest,GovernanceBenchmarkRegressionAlertApplicationServiceTest,GovernanceCapabilityApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test；mvn -pl benchmark-engine,governance -am test -DskipITs；python3 scripts/foreman.py validate F-TASK-042 --extra-command "mvn -pl benchmark-engine,governance -am test -DskipITs"；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: Benchmark regression alerts are intentionally limited to failed-threshold REGRESSION_GUARD reports; if warning-only guards or broader benchmark verdicts must trigger default governance risk handling later, that scope still needs an explicit follow-up decision.
  - Next step: Phase-F benchmark backend tasks are complete; the next dependent implementation is E-TASK-027 to consume regression summary and alert linkage contracts in the frontend benchmark pages.

### F-TASK-041: 打通推荐 SQL 到对比压测

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): orchestrate recommendation comparison benchmarks`
- Priority: 1
- Depends on: `F-TASK-040`,`D-TASK-062`
- Scope: recommendation -> comparison benchmark 契约与编排 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若 recommendation-to-benchmark 会把推荐 SQL 自动执行为压测任务、绕过审批与安全边界，需人工确认
- Data impact: 推荐对象与对比压测联动链路
- Rollback / recovery: 恢复 recommendation 与 benchmark 的显式确认边界
- Validation:
  - `recommendation-to-benchmark 测试`
  - `python3 scripts/foreman.py validate F-TASK-041`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 recommendation -> comparison benchmark 编排入口：从 sql-optimization 读取 recommendation，校验 source/recommended SQL 的只读边界，生成 RECOMMENDATION_GENERATION test set，并提交 comparison benchmark task；同时补齐 recommendation client、响应契约、只读 SQL 共享校验和 controller/service 测试。
  - Validation evidence: mvn -pl benchmark-engine clean -Dtest=BenchmarkRecommendationComparisonApplicationServiceTest,BenchmarkTaskControllerTest test；mvn -pl benchmark-engine test；python3 scripts/foreman.py validate F-TASK-041 --extra-command "mvn -pl benchmark-engine test"；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 comparison benchmark worker 仍以 task 主 SQL 作为执行入口，recommendation test set 中的 source/recommended 双 case 先作为契约与追溯元数据落库；若后续需要真正按测试集多 case 回放，还需扩展 worker 执行面。
  - Next step: 进入 F-TASK-042，把 benchmark regression summary、threshold hit 和 governance alert linkage 收口到报告与告警编排。

### F-TASK-040: 打通解析结果到测试集一键生成

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): generate test sets from parse results`
- Priority: 1
- Depends on: `F-TASK-039`,`D-TASK-058`
- Scope: parse issue / report / SQL 结果生成 benchmark test set Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若 parse-to-benchmark 联动会把解析问题自动视为可直接压测对象、绕过安全边界，需人工确认
- Data impact: 解析结果到 test set 的联动对象与过滤规则
- Rollback / recovery: 回退自动生成范围，保留人工筛选入口
- Validation:
  - `parse-to-benchmark 测试`
  - `python3 scripts/foreman.py validate F-TASK-040`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 parse-results test-set 生成链路，支持单 parseTaskId 与 batch important/urgent 解析结果生成 benchmark test set，保留只读边界 rejected evidence，并补齐受保护 SQL-optimization client、接口契约和 controller/service 测试。
  - Validation evidence: mvn -pl benchmark-engine -Dtest=BenchmarkParseResultTestSetApplicationServiceTest,BenchmarkTestSetControllerTest test；mvn -pl benchmark-engine test；python3 scripts/foreman.py validate F-TASK-040 --extra-command "mvn -pl benchmark-engine test"；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 parse batch 自动生成仍只吸纳 important/urgent 统计命中的解析项，且单 parseTaskId 路径缺少 reportCode 维度输入；更宽范围的 recommendation/comparison 编排仍需后续任务补齐。
  - Next step: 进入 F-TASK-041，把 sql-optimization recommendation 链路收口到 comparison benchmark 契约与编排。

### F-TASK-039: 落地批量测试集导入

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): import benchmark test sets with row evidence`
- Priority: 1
- Depends on: `F-TASK-038`
- Scope: 从文件导入 test set 与 case 字段映射 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若批量测试集导入会把未校验 SQL、报表或参数直接提升为可信数据，需人工确认
- Data impact: test set 导入记录、批次与成员清单
- Rollback / recovery: 恢复严格校验和失败记录，保留导入 evidence
- Validation:
  - `import 测试`
  - `python3 scripts/foreman.py validate F-TASK-039`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added benchmark test-set import API, domain model, persistence tables, MyBatis mapping, row-level readonly validation, rejected-row evidence retention, and benchmark-engine test coverage for batch-import test sets.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-039 --extra-command "mvn -pl benchmark-engine test"
  - Residual risk: Current benchmark test sets are stored and queryable, but downstream parse-to-benchmark and recommendation-to-benchmark generation flows still arrive in F-TASK-040 and F-TASK-041.
  - Next step: Proceed to F-TASK-040 to generate benchmark test sets from parse results on top of the imported test-set baseline.

### F-TASK-038: 固化压测模板与测试集契约

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(benchmark): solidify template and test-set contract`
- Priority: 1
- Depends on: `F-TASK-037`,`D-TASK-068`
- Scope: 模板类型、阈值、测试集来源与标签模型 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-011` 压测模板、测试集与解析联动
- Human confirmation point: 若 benchmark template/test-set 契约会削弱只读、影子环境或阈值边界，需人工确认
- Data impact: 模板/TestSet 数据模型、阈值与来源语义
- Rollback / recovery: 恢复原 benchmark safety 语义，停用高风险模板字段
- Validation:
  - `contract/domain 测试`
  - `python3 scripts/foreman.py validate F-TASK-038`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Solidified benchmark task contract fields for template identity, template type/version, test-set source, labels, and source references; persisted the new contract through DTO/domain/repository/SQL migration layers; aligned implementation-stage reporting with the externalized artifact governance baseline and updated benchmark contract documentation.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-038 --extra-command "mvn -pl benchmark-engine -Dtest=BenchmarkTaskStateFlowTest,BenchmarkTaskModelApplicationServiceTest,BenchmarkTaskControllerTest,MybatisBenchmarkTaskRepositoryTest,BenchmarkPersistenceRecordTest test"
  - Residual risk: This task freezes the benchmark task contract and persistence baseline, but dedicated template/test-set CRUD and import/generate orchestration remain for F-TASK-039 through F-TASK-041.
  - Next step: Proceed to F-TASK-039 to materialize batch test-set import on top of the frozen template/test-set contract.

### F-TASK-037: 落地告警查询与 ACK 接口

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add alert query and ack api`
- Priority: 1
- Depends on: `F-TASK-036`
- Scope: alert list/detail/ack API 与治理查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `E-STORY-012` Dashboard 与告警中心
- Human confirmation point: 若告警查询与 ACK 接口会破坏只读/确认边界、引入跨租户可见性扩大，需人工确认
- Data impact: governance alert list/detail/ack 接口与数据可见范围
- Rollback / recovery: 回退 ACK/查询粒度，恢复最小可见范围
- Validation:
  - `governance alert API 测试`
  - `python3 scripts/foreman.py validate F-TASK-037`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance alert list/detail/ack APIs on top of the persisted alert event and simulated notification-log baseline, including tenant-scoped filtering, detail hydration, ACK state transition plus audit logging, and a frontend truth-text correction now that the backend controller exists.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-037; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Alert center reads and ACKs are now real repo-side APIs, but policy-write endpoints and frontend controller wiring still remain outside this task, so notify strategy editing and end-user alert-page integration stay simulated until follow-up frontend work lands.
  - Next step: Proceed to the next dependency-ready Phase-F mainline; the alert-center frontend can now switch from derived evidence to the new governance alert APIs when its follow-up task is scheduled.

### F-TASK-036: 落地告警去重与模拟邮件日志

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): persist alert emission logs`
- Priority: 1
- Depends on: `F-TASK-035`
- Scope: dedupe、notify simulated、日志模板与审计留痕 Tech: `JAVA-BE`,`OPS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-010` 告警中心与模拟邮件
- Human confirmation point: 若模拟邮件日志会被误写成真实通知、或 dedupe 策略导致关键事件被静默丢弃，需人工确认
- Data impact: 通知日志、dedupe 状态与告警审计链
- Rollback / recovery: 恢复 simulated-only 语义与原始事件保留
- Validation:
  - `notification/dedup 测试`
  - `python3 scripts/foreman.py validate F-TASK-036`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added AlertEmissionApplicationService to turn evaluated alert signals into persisted alert events with tenant-scoped dedupe windows, simulated email notification logs, and governance audit entries; backfilled alert_notification_log schema, MyBatis mappings, and notification/dedup tests for the repo-closed alert center baseline.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-036; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The repository now persists deduped alert emissions and simulated notification evidence, but alert list/detail/ack APIs and frontend consumption still remain for F-TASK-037, and simulated notify stays repo-closed rather than backed by a real mail channel.
  - Next step: Proceed to F-TASK-037 to expose alert list/detail/ack APIs over the persisted alert event and notification-log baseline.

### F-TASK-035: 落地关键事件告警判定

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add alert rule evaluation`
- Priority: 1
- Depends on: `F-TASK-034`
- Scope: mass failure、service unavailable、report resolve failure、Redis unavailable、dispatch failure 等告警判定 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-010` 告警中心与模拟邮件
- Human confirmation point: 若关键事件判定会引入过度噪声、漏报关键故障或把 environment-backed 故障写成 repo 默认事实，需人工确认
- Data impact: 告警判定规则、阈值与事件生成逻辑
- Rollback / recovery: 回退高风险规则，恢复基础关键事件集
- Validation:
  - `alert rule 测试`
  - `python3 scripts/foreman.py validate F-TASK-035`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: implemented a repo-closed alert rule evaluation layer that turns mass-failure, datasource/service outage, report-resolve fallback, Redis rule source degradation, dispatch failure/staleness, and audit-write failure signals into normalized `AlertEvent` outputs with baseline policies, dedupe-ready identifiers, and structured evidence payloads.
- Context closeout:
  - Completed scope: Added a repo-closed alert rule evaluation layer that converts mass-failure, datasource/service outage, report-resolve fallback, Redis rule-source degradation, dispatch failure or staleness, and audit-write failure signals into normalized AlertEvent outputs with baseline policy severity, notify defaults, and structured evidence payloads.
  - Validation evidence: mvn -pl governance -Dtest=AlertEventTest,AlertPolicyBaselineTest,AlertSchemaMappingTest,AlertRuleApplicationServiceTest test; python3 scripts/foreman.py validate F-TASK-035
  - Residual risk: The repository can now evaluate critical alert signals into normalized events, but it still does not persist emitted alerts as a deduped history, record simulated notification logs, or expose query/detail/ack APIs; those remain in F-TASK-036 and F-TASK-037.
  - Next step: Proceed to F-TASK-036 to persist deduped alert emissions and simulated notification logs on top of the current alert rule evaluation layer.

### F-TASK-034: 固化告警事件类型与等级模型

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add alert baseline model`
- Priority: 1
- Depends on: `F-TASK-033`,`D-TASK-062`
- Scope: 定义 alert type、level、dedup key、notify status 与策略模型 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-010` 告警中心与模拟邮件
- Human confirmation point: 若告警模型会弱化当前审计与去重边界、删除关键事件等级或改变责任人语义，需人工确认
- Data impact: alert event/policy 数据模型与治理查询面
- Rollback / recovery: 恢复既有告警分类与审计语义，保留新增字段为附加扩展
- Validation:
  - `domain/model 测试`
  - `python3 scripts/foreman.py validate F-TASK-034`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: added governance alert domain baselines for event type/level, dedupe key, simulated notify status, and policy defaults; backfilled init schema, incremental migration, and mapper/schema tests for alert policy/event persistence.
- Context closeout:
  - Completed scope: Added governance alert domain baselines for event type/level, dedupe keys, simulated notify status, policy defaults, and alert policy/event persistence scaffolding in init schema plus incremental migration.
  - Validation evidence: mvn -pl governance -Dtest=AlertEventTest,AlertPolicyBaselineTest,AlertSchemaMappingTest test; python3 scripts/foreman.py validate F-TASK-034
  - Residual risk: The repository now fixes alert types, levels, dedupe keys, and simulated notify defaults, but it still does not emit rule-driven alert events, record simulated email logs, or expose alert query/ack APIs; those remain in F-TASK-035 through F-TASK-037.
  - Next step: Proceed to F-TASK-035 to implement alert rule evaluation on top of the persisted alert type/policy baseline.

### U-TASK-004: 前端复盘补漏并恢复规格直达能力

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): restore spec-aligned workspace coverage`
- Priority: 1
- Depends on: `U-TASK-003`,`U-TASK-002`,`U-TASK-001`
- Scope: 以前端与实施规格差距为基线，补齐解析导航直达能力、Dashboard 规格覆盖、相关文档与 contract guard，不把缺失后端能力伪装成已实现事实。 Tech: `VUE-FE`,`DOCS`. Layer: `frontend/router/views/styles/scripts`,`docs`.
- Plan ref: docs/exec-plans/completed/U-TASK-004-full-auto-execution-plan.md
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 若实现会移除既有承诺路由、把样本化 KPI 写成全租户事实，或把无写 API 的治理页改成伪可写能力，需人工确认。
- Data impact: 前端导航、解析工作区入口、Dashboard 指标表达、规格补充文档与验证脚本；不改写后端业务数据或外部系统状态。
- Rollback / recovery: 回退到当前导航与 Dashboard 表达，保留 read-only、sampled、simulated 等边界文案，不新增对外部环境的强依赖。
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-004`
  - `python3 scripts/foreman.py validate U-TASK-004`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: audited `App.vue`, `DashboardView.vue`, `AccelerationView.vue`, product spec, and current contract guards; closed repo-side gaps by restoring explicit parse secondary entries with query-aware navigation, aligning the routing-governance module label with the implementation spec while preserving evidence-first semantics, expanding dashboard sample KPI coverage plus dispatch-coordination status, and adding `docs/product/frontend-retrospective-gap-closure-baseline.md` with matching validation guards. Validation passed via `python3 scripts/foreman.py validate U-TASK-004 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-dashboard-contract.mjs' --extra-command 'node scripts/check-frontend-gap-closure-doc.mjs'`.
- Context closeout:
  - Completed scope: Audited the frontend against the SQL governance implementation spec, restored explicit parse secondary entry coverage through query-aware navigation and in-page workspace cards, realigned the routing-governance shell label with the spec while preserving evidence-first semantics, expanded dashboard sample KPIs plus dispatch-coordination visibility, and added a frontend retrospective gap-closure baseline with matching contract guards.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-004 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-dashboard-contract.mjs' --extra-command 'node scripts/check-frontend-gap-closure-doc.mjs'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Dashboard still relies on current-window samples for success, cache, rewrite, acceleration, and access-channel ratios until dedicated backend aggregates exist.
  - Next step: If backend aggregates or writable governance APIs are added later, replace sample-only homepage metrics and placeholder-only actions with direct contract-backed flows.

### U-TASK-003: 扁平化前端导航并补齐解析历史缺项

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): flatten navigation and backfill parse history`
- Priority: 1
- Depends on: U-TASK-001,E-TASK-018,E-TASK-022,E-TASK-033,E-TASK-037
- Scope: Flatten the frontend sidebar from three levels to module-plus-leaf navigation for SQL history and parse mainline, promote third-level pages into second-level entries, and backfill the parse and history surfaces that regressed during consolidation: add missing parse-statistics dimensions, align history filters with current backend contract, and update routing plus contract checks accordingly. Tech: VUE-FE. Layer: frontend/router/views/styles/scripts.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-003`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Flattened the SQL history and parsing sidebar modules to module-plus-leaf navigation, restored missing parse-statistics dimensions with sampled severity/priority/logical-object/status views, and aligned history workbenches with the current backend filter and export contracts.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-003 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-statistics-page-contract.mjs' --extra-command 'node scripts/check-history-page-contract.mjs' --extra-command 'node scripts/check-history-detail-contract.mjs'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The new logical-object and parse-status tabs are sampled frontend aggregations over current history windows because the repository still lacks dedicated backend summary endpoints for tenant-wide logical-object and structure/access-status rollups.
  - Next step: If the backend later exposes first-class parse-statistics endpoints for logical objects or structure-versus-access status, replace the current sampled frontend aggregations with direct contract-backed views and extend the statistics contract again.

### U-TASK-002: 恢复首页总揽与前端合同护栏

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): restore dashboard operator overview`
- Priority: 1
- Depends on: U-TASK-001,E-TASK-029
- Scope: Rebuild the dashboard overview into a complete operator home: restore richer KPI cards, five primary workbench entries, health-and-risk plus next-step sections, and a broader recent-activity slice using existing audited frontend evidence. Strengthen dashboard contract coverage so KPI density, entry completeness, and section completeness regressions fail validation. Tech: VUE-FE. Layer: frontend/router/views/styles/scripts.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-002`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt the dashboard into a fuller operator homepage with richer KPI coverage, five primary workbench entries, a dedicated health-and-risk section, broader recent activity, and recommended next-step actions driven by existing audited evidence surfaces.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-002 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-dashboard-contract.mjs'; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Global benchmark pass-rate and tenant-wide adoption-rate KPIs remain intentionally absent because the repository still lacks audited global benchmark-task and recommendation-adoption summary contracts; the homepage stays explicit about session-only or sampled evidence where needed.
  - Next step: Instantiate the follow-up navigation and parse-history completion task so sidebar depth, parse-statistics missing dimensions, and history-page gaps can be corrected under a separate audited commit.

### U-TASK-001: 前端三轮复盘与交互重构落地

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): consolidate parse workbench flows`
- Priority: 1
- Depends on: E-TASK-020,E-TASK-025,E-TASK-028,E-TASK-030,E-TASK-031,E-TASK-032
- Scope: Implement the SQLForge frontend information-architecture and interaction refactor: consolidate parse workbench, batch parsing, statistics, and history into one main route; convert routing governance into read-only routing evidence; restore delivery progress in primary navigation with temporary labeling; add real system-management write actions for datasource/report/redis/dispatch create-update surfaces where backend APIs exist; add explicit placeholder dialogs for access and alert actions without write APIs; preserve compatibility redirects for /parse-batches and /parse-statistics; update contracts and validation coverage.
- Validation:
  - `python3 scripts/foreman.py validate U-TASK-001`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-27: refactored the frontend shell and routing so parse workbench became the single parse entry, delivery progress returned to primary navigation, and routing governance shifted to routing-evidence semantics.
  - 2026-04-27: merged single-parse, batch parsing, statistics, and parse-history workflows into the main parse workbench; added real system-management write actions and placeholder capability dialogs on pages without write APIs.
  - 2026-04-27: validation passed via `python3 scripts/foreman.py validate U-TASK-001 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "node scripts/check-navigation-shell-contract.mjs" --extra-command "node scripts/check-parse-workbench-contract.mjs" --extra-command "node scripts/check-batch-import-contract.mjs" --extra-command "node scripts/check-statistics-page-contract.mjs" --extra-command "node scripts/check-routing-page-contract.mjs" --extra-command "node scripts/check-system-config-contract.mjs" --extra-command "node scripts/check-access-page-contract.mjs" --extra-command "node scripts/check-alert-page-contract.mjs"`; `python3 scripts/task_audit.py --check --phase post-closeout` also passed.
- Context closeout:
  - Completed scope: Consolidated the parse mainline into one workbench route, restored delivery progress navigation, converted routing governance into read-only routing evidence, added real system-management create or update actions where backend APIs exist, and added explicit placeholder dialogs on access or alert surfaces without write APIs.
  - Validation evidence: python3 scripts/foreman.py validate U-TASK-001 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-navigation-shell-contract.mjs' --extra-command 'node scripts/check-parse-workbench-contract.mjs' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'node scripts/check-statistics-page-contract.mjs' --extra-command 'node scripts/check-routing-page-contract.mjs' --extra-command 'node scripts/check-system-config-contract.mjs' --extra-command 'node scripts/check-access-page-contract.mjs' --extra-command 'node scripts/check-alert-page-contract.mjs'; python3 scripts/task_audit.py --check --phase post-closeout
  - Residual risk: Dispatch policy edit remains a placeholder because the repository exposes create but not update APIs; routing-rule, access-strategy, and alert-rule writes also remain intentionally read-only or placeholder-only until backend contracts exist.
  - Next step: If backend update APIs are added for dispatch or governance policy editing, replace the placeholder dialogs with real editable flows and extend the contract checks accordingly.

### E-TASK-037: 收口前端二次复盘的导航与工作台交互

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): refine navigation and workbench interaction model`
- Priority: 1
- Depends on: E-TASK-036
- Scope: 按二次复盘要求重构前端导航层级、SQL 查询工作台，以及 SQL 历史 / 解析结果中心 / 路由治理 / 系统管理 / 开放接入页面的表格、弹窗和抽屉交互；补齐必要的页面一致性与验证脚本。 Tech: VUE-FE. Layer: frontend/router/views/styles/scripts.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-037`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reworked the frontend shell and primary governance workbenches around mixed-depth navigation plus filter/table/dialog/drawer patterns; tightened SQL query, SQL history, parse statistics, routing governance, access center, and system management so first-screen focus stays on the operator task instead of inline evidence dumps.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-037 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "node scripts/check-navigation-shell-contract.mjs" --extra-command "node scripts/check-query-workbench-contract.mjs" --extra-command "node scripts/check-history-page-contract.mjs" --extra-command "node scripts/check-history-detail-contract.mjs" --extra-command "node scripts/check-statistics-page-contract.mjs" --extra-command "node scripts/check-access-page-contract.mjs" --extra-command "node scripts/check-routing-page-contract.mjs" --extra-command "node scripts/check-system-config-contract.mjs"
  - Residual risk: The refactor still relies on repository sample data and existing query-history surfaces, so sparse local datasets can make some tables look thinner than production; audit-event counts also remain absent from query-history page rows until the backend page projection exposes them directly.
  - Next step: If richer governance write-back fixtures are added later, re-run the same contract checks and browser smoke against non-empty datasets to verify density and scanability under production-like states.

### E-TASK-036: 基于联调复盘修正前端页面实现偏差

- Status: done
- Completed at: 2026-04-27
- Commit subject: `fix(frontend): align runtime pages with joint-review findings`
- Priority: 1
- Depends on: E-TASK-035
- Scope: 基于真实前后端联调，对导航、SQL查询、解析、系统管理、开放接入等页面按用户反馈重新复盘，修复与本轮需求不符的交互、信息架构和视觉实现偏差。 Tech: VUE-FE+SPRING. Layer: frontend/backend/runtime.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-036`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Restored missing Element Plus runtime components, rebuilt dark-mode component theming, tightened three-level navigation labels, reduced redundant page hero content, upgraded local governance dev schemas, and fixed query-execution readonly guard so annotated SELECT statements execute successfully in live frontend/backend joint debugging.
  - Validation evidence: npm run lint; npm run build; mvn -pl query-execution -Dtest=QueryExecutionApplicationServiceTest,ReadonlyQueryGuardTest test; python3 scripts/foreman.py validate E-TASK-036; real joint-debug screenshots for sql-query/access/system/parse-batches; live /api/query-execution/queries/execute success with annotated SELECT against mock Hetu.
  - Residual risk: System management and access pages still show sparse datasets in local dev because governance datasource/config sample records are not populated; query-history sample evidence remains thin until a fuller governance write-back dataset is seeded.
  - Next step: Seed richer governance datasource/query-history sample data so system-management and access-audit pages can be reviewed against non-empty production-like states.

### E-TASK-035: 收口治理管理与开放接入页面体验

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): streamline management and access workbenches`
- Priority: 1
- Depends on: E-TASK-034,E-TASK-032
- Scope: 重构 Dashboard、告警中心、路由治理、推荐中心、压测中心、系统管理与开放接入页面，去除无关信息与卡片堆叠，改为概览+列表/表格+抽屉/弹窗的治理工作台模式。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-035`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构系统管理与开放接入页面，改为总览 + 列表 + 抽屉/弹窗的治理工作台，移除主区无关信息堆叠。
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-035 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-access-page-contract.mjs'
  - Residual risk: 系统管理和接入页仍依赖 query-history / governance mock surface，没有新增独立 access-audit controller。
  - Next step: 继续把低频治理页收敛到相同的表格与抽屉语言。

### E-TASK-034: 重构SQL查询与解析中心交互工作流

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): rebuild query and parsing workbench flows`
- Priority: 1
- Depends on: E-TASK-033,E-TASK-022
- Scope: 重构 SQL 查询、解析工作台、批量解析中心与解析结果中心，改为查询条件+结果区+必要弹窗/抽屉模式，补齐单条/多条输入和 drill-through 交互。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-034`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构 SQL 查询、批量解析中心与解析结果中心，统一为条件栏 + 结果区 + 弹窗/抽屉工作流，并补齐多条 SQL 直接输入和统计 drill-down。
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-034 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-query-workbench-contract.mjs' --extra-command 'node scripts/check-batch-import-contract.mjs' --extra-command 'node scripts/check-statistics-page-contract.mjs'
  - Residual risk: 解析工作流里的 datasource/object tree 仍以仓库内模拟对象树承载，没有接实时 catalog。
  - Next step: 将新工作流继续对齐到后续实时 catalog 与 explain 数据源。

### E-TASK-033: 重构导航信息架构与深色主题壳层

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): rebuild dark navigation shell`
- Priority: 1
- Depends on: E-TASK-032,F-TASK-028
- Scope: 将前端路由导航重构为三级动态侧栏树，按产品规格重组一级模块/二三级子页，同时去除用户可见浅色主题切换并把全站默认视觉锁定为 dark-mode-native。 Tech: VUE-FE. Layer: frontend/router/views/styles.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-033`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 重构 App 壳层为三级动态侧栏，补齐 route meta IA 字段，锁定深色主题默认值，并把高频治理页面的浅色硬编码替换为深色 token。
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-033 --extra-command 'npm run lint' --extra-command 'npm run build'
  - Residual risk: 仍有 DeliveryProgressView 保留极轻量高光渐变，但不影响正式导航主线。
  - Next step: 继续沿新 IA 校对剩余低频页的视觉一致性。

### E-TASK-032: 落地 Redis 规则源、装数协同与系统参数页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add system config governance contract`
- Priority: 1
- Depends on: `E-TASK-031`,`D-TASK-072`
- Scope: Redis rule source、dispatch policy、系统参数与权限审计展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-013` 系统管理与数据源治理页
- Human confirmation point: 若 Redis 规则源、装数协同与系统参数页会把 environment-backed 配置写成默认已启用事实，需人工确认
- Data impact: rule-source、dispatch policy、system-param/permission 展示面
- Rollback / recovery: 恢复到查询/模拟状态展示，保留 simulated 或未联通提示
- Validation:
  - `npm run lint`、`npm run build`、system-management config contract 测试
  - `python3 scripts/foreman.py validate E-TASK-032`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Finalized the system-config governance surface with config contract coverage for Redis rule sources, dispatch policies, tenant parameters, and permission-audit boundaries.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-032 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-system-config-contract.mjs'
  - Residual risk: The page intentionally preserves config-only and simulated wording for environment-backed governance integrations.
  - Next step: No additional tracked frontend task residue remains in the worktree.

### E-TASK-031: 落地系统管理中的数据源与报表接口页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add system datasource governance page`
- Priority: 1
- Depends on: `E-TASK-024`,`D-TASK-072`
- Scope: datasource 管理、测试连接、报表接口配置与健康状态页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-013` 系统管理与数据源治理页
- Human confirmation point: 若系统管理数据源/报表接口页会暴露敏感连接信息、误导用户认为真实外部接口已默认联通，需人工确认
- Data impact: 系统管理中的 datasource、health-check、report-interface 展示面
- Rollback / recovery: 恢复脱敏与 mock/config 标识，关闭高风险编辑入口
- Validation:
  - `npm run lint`、`npm run build`、system-management datasource contract 测试
  - `python3 scripts/foreman.py validate E-TASK-031`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the unified system management page with datasource inventory, connection tests, report-interface visibility, and governance remediation evidence.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-031 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-system-datasource-contract.mjs'
  - Residual risk: Datasource and report-interface management remain read-oriented and avoid exposing raw credentials or implying default external connectivity.
  - Next step: Close out the remaining system config task and verify the worktree is clean.

### E-TASK-030: 落地告警中心与通知状态视图

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add alert center with simulated ack`
- Priority: 1
- Depends on: `E-TASK-029`,`F-TASK-037`
- Scope: 告警列表、详情、ACK、notify simulated 状态展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-012` Dashboard 与告警中心
- Human confirmation point: 若告警中心页会把模拟邮件写成真实通知成功、或隐藏 dedupe / ACK 语义，需人工确认
- Data impact: 告警列表、详情、ACK 和通知状态展示
- Rollback / recovery: 恢复 simulated 状态文案与完整事件状态链
- Validation:
  - `npm run lint`、`npm run build`、alert page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-030`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the alert center with derived alerts, detail drill-down, simulated ACK, and simulated notify status handling.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-030 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-alert-page-contract.mjs'
  - Residual risk: Dedicated backend alert APIs are still absent, so the page remains explicit about derived alerts and frontend-simulated ACK or notify semantics.
  - Next step: Close out system management datasource and config tasks.

### E-TASK-029: 落地 Dashboard KPI、分布与待办区块

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add governance dashboard live overview`
- Priority: 1
- Depends on: `E-TASK-028`,`F-TASK-037`
- Scope: 核心 KPI、问题分布、接入分布与待处理清单卡片 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-012` Dashboard 与告警中心
- Human confirmation point: 若 Dashboard KPI 与待办会聚合不存在的数据、放大 environment-backed 指标权重或引入未审计来源，需人工确认
- Data impact: Dashboard 聚合指标、卡片与待办清单
- Rollback / recovery: 恢复基于治理查询面的 KPI，移除无证据来源聚合
- Validation:
  - `npm run lint`、`npm run build`、dashboard contract 测试
  - `python3 scripts/foreman.py validate E-TASK-029`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Rebuilt the dashboard around audited KPI, issue distribution, access distribution, todo, and activity evidence blocks.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-029 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-dashboard-contract.mjs'
  - Residual risk: The dashboard only reflects sampled query-history windows and current backend evidence availability rather than claiming tenant-wide totals.
  - Next step: Close out alert and system management tasks.

### E-TASK-028: 落地开放接入页与 JDBC Agent / SDK 展示

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add access governance center`
- Priority: 1
- Depends on: `E-TASK-027`,`D-TASK-068`
- Scope: API、JDBC Agent、SDK、接入策略和接入审计展示页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-011` 压测中心与开放接入页
- Human confirmation point: 若开放接入页会把 JDBC Agent 全模式、SDK 或真实接口联通写成既有事实，需人工确认
- Data impact: 开放接入页、接入策略和文案
- Rollback / recovery: 恢复到契约/规划态展示，明确当前落地阶段
- Validation:
  - `npm run lint`、`npm run build`、access page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-028`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the open-access governance page with API, JDBC Agent, Java SDK, and access-audit sample views.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-028 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-access-page-contract.mjs'
  - Residual risk: The page still marks dedicated access-audit controller support as absent and relies on query-history samples for audit evidence.
  - Next step: Close out the remaining dashboard, alert, and system tasks.

### E-TASK-027: 落地压测任务、模板、测试集与报告页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): expand benchmark governance center`
- Priority: 1
- Depends on: `E-TASK-026`,`F-TASK-042`
- Scope: 任务列表、模板详情、测试集、报告对比和回归结果页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-011` 压测中心与开放接入页
- Human confirmation point: 若压测中心页会把模板/测试集能力写成已默认启用的真实运行时基线、或混淆回归与对比模式，需人工确认
- Data impact: benchmark 页面、模板/TestSet UI 与报告对比面
- Rollback / recovery: 恢复模板/测试集/报告分区，保留模式差异与未实现能力标识
- Validation:
  - `npm run lint`、`npm run build`、benchmark page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-027`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the benchmark page with template, test-set, session-task, comparison, and regression reporting sections.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-027 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-benchmark-page-contract.mjs'
  - Residual risk: The page stays within current benchmark-engine capabilities and does not claim unsupported template execution paths.
  - Next step: Close out the remaining Phase-E governance pages.

### E-TASK-026: 落地推荐与加速中心页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add recommendation center`
- Priority: 1
- Depends on: `E-TASK-025`,`D-TASK-062`
- Scope: 推荐分类、详情、收益/风险、dispatch 状态与关联追溯页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-010` 路由治理与推荐中心
- Human confirmation point: 若推荐中心会把“推荐”误写成“已执行装数”、或隐藏 dispatch 失败状态，需人工确认
- Data impact: 推荐中心、dispatch 状态与说明文案
- Rollback / recovery: 恢复 recommendation / dispatch 分离展示，保留失败/待拉取状态
- Validation:
  - `npm run lint`、`npm run build`、recommendation page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-026`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the recommendation center with recommendation categories, benefit/risk detail, dispatch evidence, and traceability views.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-026 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-recommendation-page-contract.mjs'
  - Residual risk: The page remains evidence-driven and does not execute recommended SQL or perform external dispatch from the browser.
  - Next step: Close out the remaining Phase-E governance pages.

### E-TASK-025: 落地路由治理页与历史决策详情

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add routing governance page`
- Priority: 1
- Depends on: `E-TASK-024`,`D-TASK-061`
- Scope: 当前规则、决策样例、历史记录、注释协议说明与路由详情页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-010` 路由治理与推荐中心
- Human confirmation point: 若路由治理页会暴露内部策略细节、误导用户把 environment-backed 证据写成默认事实，需人工确认
- Data impact: 路由规则、决策详情与说明文案
- Rollback / recovery: 回退高风险字段，恢复基于仓库真值的路由展示
- Validation:
  - `npm run lint`、`npm run build`、routing page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-025`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the routing governance page with route-calibration, trace/history evidence, and comment protocol guidance.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-025 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-routing-page-contract.mjs'
  - Residual risk: The page remains read-only and depends on backend evidence availability; no routing-rule editing surface is exposed.
  - Next step: Close out the remaining governance pages in Phase E.

### E-TASK-024: 落地逻辑视图映射、数据到位与热度视图

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): extend logical object evidence view`
- Priority: 1
- Depends on: `E-TASK-023`,`D-TASK-071`
- Scope: 逻辑对象映射、freshness/SLA/usage heat 与相关 SQL 展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-009` 数据资产与逻辑视图
- Human confirmation point: 若逻辑视图映射页会把数据到位状态、SLA 或热度写成确定事实而无证据来源，需人工确认
- Data impact: 逻辑对象映射、freshness/SLA/heat 展示
- Rollback / recovery: 恢复字段证据标识与默认未知状态
- Validation:
  - `npm run lint`、`npm run build`、logical object contract 测试
  - `python3 scripts/foreman.py validate E-TASK-024`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended the asset catalog with freshness and SLA evidence cards, usage-heat proxy rendering, logical-object mapping emphasis, and related SQL candidates aligned from parse statistics.
  - Validation evidence: npm run lint; npm run build; node scripts/check-logical-object-contract.mjs; python3 scripts/foreman.py validate E-TASK-024 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-logical-object-contract.mjs'
  - Residual risk: Usage heat is intentionally labeled as an evidence-derived proxy because the repo-side baseline does not expose a dedicated live heat endpoint yet; related SQL is only aligned for logical views via reportCode.
  - Next step: Continue with E-TASK-025 to build the routing governance page and decision detail flow on top of route and history contracts.

### E-TASK-023: 落地数据资产目录与对象详情页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add data asset catalog view`
- Priority: 1
- Depends on: `E-TASK-022`,`D-TASK-071`
- Scope: datasource/schema/table/logical-view/db-view 列表与详情页 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-009` 数据资产与逻辑视图
- Human confirmation point: 若数据资产页会混淆业务逻辑视图与 DB View、暴露未授权对象详情，需人工确认
- Data impact: 资产目录、对象详情、导航结构
- Rollback / recovery: 恢复对象类型区分与权限控制，关闭高风险详情区域
- Validation:
  - `npm run lint`、`npm run build`、asset page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-023`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated data asset catalog route with datasource, schema, table, logical-view, and db-view lists plus detail panels and metadata snapshot evidence.
  - Validation evidence: npm run lint; npm run build; node scripts/check-asset-page-contract.mjs; python3 scripts/foreman.py validate E-TASK-023 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-asset-page-contract.mjs'
  - Residual risk: Schema-level evidence currently falls back to datasource-scoped metadata snapshots because the repo-side snapshot query surface does not expose schemaName filters yet.
  - Next step: Instantiate E-TASK-024 to extend the asset experience with logical mappings, freshness/SLA emphasis, usage heat signals, and related SQL context.

### E-TASK-022: 落地解析结果中心与优先级矩阵

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add parse statistics center`
- Priority: 1
- Depends on: `E-TASK-021`,`D-TASK-058`
- Scope: 解析统计、问题分布、priority matrix、important/urgent 清单 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: 若解析统计页会改变 severity/priority 口径、隐藏 important/urgent 判定依据，需人工确认
- Data impact: 统计图表、矩阵与 drill-through 页
- Rollback / recovery: 恢复既定统计口径与标签，保留新增展示为附加视图
- Validation:
  - `npm run lint`、`npm run build`、statistics page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-022`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated parse-statistics route that consumes overview, issue-scene, by-sql, by-report, priority-matrix, and important-urgent endpoints to render KPI cards, issue distribution, and urgency matrices.
  - Validation evidence: npm run lint; npm run build; node scripts/check-statistics-page-contract.mjs; python3 scripts/foreman.py validate E-TASK-022 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-statistics-page-contract.mjs'
  - Residual risk: The page currently projects backend aggregates read-only; drill-through from matrix cells to history or batch detail is still deferred to later frontend tasks.
  - Next step: Continue with E-TASK-023 to build the data asset catalog and object detail views on top of governance metadata contracts.

### E-TASK-021: 落地批量解析中心与报表清单导入页

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add batch parse and report import center`
- Priority: 1
- Depends on: `E-TASK-020`,`D-TASK-054`
- Scope: 模板下载、上传、批次列表、批次详情与失败记录展示 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: 若批量解析页会把兼容格式失败误写成产品故障、或把 mock 报表清单写成真实接口联通，需人工确认
- Data impact: 批次页、导入模板、报表清单 UI 语义
- Rollback / recovery: 保留稳定格式优先与 mock 标识，回退高风险文案/行为
- Validation:
  - `npm run lint`、`npm run build`、batch import contract 测试
  - `python3 scripts/foreman.py validate E-TASK-021`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated batch parse center route with parse-batch creation, template download, upload/ingest, retry-access, report-catalog import, resolve-sqls, and session-local batch detail views.
  - Validation evidence: npm run lint; npm run build; node scripts/check-batch-import-contract.mjs; python3 scripts/foreman.py validate E-TASK-021 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-batch-import-contract.mjs'
  - Residual risk: The UI keeps a session-local list of created/imported batches because the repo-side baseline exposes detail endpoints but no dedicated list endpoint yet.
  - Next step: Instantiate E-TASK-022 to build the parse-statistics center and priority matrix on top of parse-statistics contracts.

### E-TASK-020: 落地解析工作台双卡结果布局

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add parse workbench dual-card layout`
- Priority: 1
- Depends on: `E-TASK-019`,`D-TASK-045`
- Scope: 单条 SQL 解析输入、结构解析卡、access parse 卡和综合结论 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-008` 解析工作台与批量解析中心
- Human confirmation point: 若解析工作台把 access parse 不可用伪装成结构解析成功、或在前端合并双轨语义导致用户误解，需人工确认
- Data impact: 解析工作台页面状态、双卡展示与提示文案
- Rollback / recovery: 恢复结构/访问解析分开展示与 unavailable 提示
- Validation:
  - `npm run lint`、`npm run build`、parse workbench contract 测试
  - `python3 scripts/foreman.py validate E-TASK-020`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reworked the acceleration route into a parse workbench backed by structure/combined parse APIs, with single-SQL input, combined conclusion, structure/access dual cards, state history, and structure-only preview.
  - Validation evidence: npm run lint; npm run build; node scripts/check-parse-workbench-contract.mjs; python3 scripts/foreman.py validate E-TASK-020 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-parse-workbench-contract.mjs'
  - Residual risk: The parse workbench currently visualizes parse-batch and benchmark journeys elsewhere; this page focuses on single-SQL parse contracts only.
  - Next step: Instantiate E-TASK-021 to build the batch-parse center and report-catalog import workflow on top of parse-batches contracts.

### E-TASK-019: 落地 SQL 历史详情与取证视图

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add sql history forensic detail view`
- Priority: 1
- Depends on: `E-TASK-018`,`D-TASK-041`
- Scope: SQL 三态、注释上下文、结构/访问解析、route/recommendation/alert/benchmark 关联取证视图 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: 若取证详情会暴露未脱敏参数、内部错误栈或隐藏部分失败证据，需人工确认
- Data impact: 历史详情页、SQL 三态和关联取证展示
- Rollback / recovery: 恢复脱敏与失败证据显示边界，关闭高风险详情块
- Validation:
  - `npm run lint`、`npm run build`、detail page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-019`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Connected parse-record history detail to query-history for SQL tri-state, parse summaries, route/cache/binding evidence, and related recommendation/benchmark/alert/audit references.
  - Validation evidence: npm run lint; npm run build; node scripts/check-history-detail-contract.mjs; python3 scripts/foreman.py validate E-TASK-019 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-history-detail-contract.mjs'
  - Residual risk: The page renders backend forensic payloads as formatted JSON blocks; future backend shape changes will need matching frontend grouping updates.
  - Next step: Instantiate E-TASK-020 to build the parse workbench dual-card result layout on top of the detail/forensics baseline.

### E-TASK-018: 落地 SQL 历史列表筛选与分类面

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): add history page filters and sorting`
- Priority: 1
- Depends on: `E-TASK-017`,`D-TASK-040`
- Scope: 历史过滤、分类、排序、分页与列表列渲染 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: 若历史列表筛选会暴露未授权字段、跨租户可见数据或破坏分页性能边界，需人工确认
- Data impact: 历史列表、筛选状态与前端缓存态
- Rollback / recovery: 回退敏感筛选/列，恢复基础列表视图与分页
- Validation:
  - `npm run lint`、`npm run build`、history page contract 测试
  - `python3 scripts/foreman.py validate E-TASK-018`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended the SQL history page with explicit history classification filters, sort modes, and richer lookup-state display while preserving existing lookup, pagination, and trace-detail drill-through behavior.
  - Validation evidence: npm run lint; npm run build; node scripts/check-history-page-contract.mjs; python3 scripts/foreman.py validate E-TASK-018 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-history-page-contract.mjs'
  - Residual risk: The history page now exposes filtering and sorting, but the deeper forensic SQL tri-state and related-object drill-through views still belong to E-TASK-019.
  - Next step: Proceed to E-TASK-019 to build the detailed SQL tri-state, comment context, structure/access parse, and related route/recommendation/alert/benchmark forensics view.

### E-TASK-017: 扩展 SQL 查询工作台三栏布局与执行摘要

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(frontend): redesign sql query workbench`
- Priority: 1
- Depends on: `E-TASK-016`,`D-TASK-039`
- Scope: 数据源树、SQL 编辑器、参数输入、右侧治理摘要与结果页签，消费查询执行扩展契约 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-007` SQL 查询与历史前端增强
- Human confirmation point: 若查询工作台增强会在前端复刻后端权威逻辑、引入越权字段展示或改变既有执行入口语义，需人工确认
- Data impact: 查询页布局、状态编排与前端消费字段
- Rollback / recovery: 保留后端权威，回退高风险前端判断逻辑，仅保留展示/编排层
- Validation:
  - `npm run lint`、`npm run build`、frontend contract 测试
  - `python3 scripts/foreman.py validate E-TASK-017`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reworked the SQL query page into a three-rail workbench with datasource tree context, SQL editor plus parameter inputs, and right-side governance summary with result tabs, while preserving live query execution and degraded recovery evidence flows.
  - Validation evidence: npm run lint; npm run build; node scripts/check-query-workbench-contract.mjs; python3 scripts/foreman.py validate E-TASK-017 --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'node scripts/check-query-workbench-contract.mjs'
  - Residual risk: The page still relies on static datasource tree context and does not yet implement the downstream history list/detail surfaces that arrive in E-TASK-018 and E-TASK-019.
  - Next step: Proceed to E-TASK-018 to build the SQL history filter, classification, sorting, and pagination view against the history backend contracts.

### D-TASK-066: 扩展 JDBC Agent `Governed Execute`

- Status: done
- Completed at: 2026-04-27
- Commit subject: `test(jdbc-agent): add governed execute coverage`
- Priority: 1
- Depends on: `D-TASK-065`
- Scope: 通过平台 API 执行 SQL，并保留 fallback 语义 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Governed Execute` 会在平台不可用时无回退策略、或默认强制所有 SQL 走平台，需人工确认
- Data impact: Agent 执行模式、fallback 策略、平台调用链
- Rollback / recovery: 恢复租户/数据源级可切换边界和 fallback 语义
- Validation:
  - `governed-execute 测试`
  - `python3 scripts/foreman.py validate D-TASK-066`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added dedicated governed-execute validation coverage for JDBC Agent platform execution, direct-JDBC fallback, and fail-closed behavior, plus a focused validation script for the governed-execute contract.
  - Validation evidence: bash scripts/run-jdbc-agent-governed-execute-tests.sh; python3 scripts/foreman.py validate D-TASK-066 --extra-command 'bash scripts/run-jdbc-agent-governed-execute-tests.sh'
  - Residual risk: The shared JDBC agent implementation was introduced earlier together with local-rewrite support, so this task closes with focused governed-execute contract coverage rather than a fresh codepath split.
  - Next step: Rebind to the next active implementation-spec task after D-STORY-012, because the current tasks.md queue is now cleared for JDBC Agent and Java SDK follow-ups.

### D-TASK-065: 落地 JDBC Agent 首版 `Observe`

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(jdbc-agent): harden observe-only coverage`
- Priority: 1
- Depends on: `D-TASK-064`
- Scope: JAR 采集 SQL、注释解析、上报 access audit，不接管执行 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Observe` 会接管执行、写入敏感信息或在规则源失败时影响业务查询，需人工确认
- Data impact: Agent JAR、采集上报、access audit 与 Redis 依赖
- Rollback / recovery: 恢复 observe-only 语义，禁用高风险上报或敏感字段透出
- Validation:
  - `JDBC agent sample/integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-065`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Hardened JDBC Agent observe-mode comment parsing to ignore malformed leading comments, and added dedicated observe-only tests plus a focused validation script for audit reporting, fail-open behavior, and non-takeover execution.
  - Validation evidence: bash scripts/run-jdbc-agent-observe-tests.sh; python3 scripts/foreman.py validate D-TASK-065 --extra-command 'bash scripts/run-jdbc-agent-observe-tests.sh'
  - Residual risk: The shared JDBC agent baseline for governed execution and local rewrite still lives in the common agent implementation, and governed-execute closeout remains pending under D-TASK-066.
  - Next step: Close out D-TASK-066 with its dedicated governed-execute validation coverage, then resume the remaining implementation-spec tasks.

### D-TASK-068: 落地 Java SDK 首版

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(open-access): add java sdk client baseline`
- Priority: 1
- Depends on: `D-TASK-067`
- Scope: 提供鉴权、trace/requestId、typed client 与 retry 基线 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 Java SDK 会把未稳定契约写成强依赖、绕过统一 request/trace 语义或暴露敏感配置，需人工确认
- Data impact: SDK client、配置、请求重试与接入文档
- Rollback / recovery: 回退 SDK 到最小 typed client 基线，并保留 HTTP API 主路径
- Validation:
  - `SDK 测试`
  - `python3 scripts/foreman.py validate D-TASK-068`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the Java SDK typed client wrapper that forces SDK access-channel context, delegates query execution, and writes governance audit summaries for success and failure paths.
  - Validation evidence: mvn -pl sqlforge-shared -am clean -Dtest=SqlForgeJavaSdkClientTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-068
  - Residual risk: The SDK baseline is still a thin wrapper over the HTTP query execution and audit clients, so future tasks may still expand configuration surfacing or richer typed APIs without changing this audited request path.
  - Next step: Resume D-TASK-065 and D-TASK-066 follow-up work, then continue the remaining implementation-spec tasks beyond the current workspace residue.

### D-TASK-070: 建立 `MetadataSnapshot` 与数据到位状态模型

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add metadata snapshot baseline`
- Priority: 1
- Depends on: `D-TASK-069`
- Scope: metadata snapshot、freshness、SLA、upstream/downstream/queryability 的模型与追溯键 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若 metadata snapshot / freshness / SLA / upstream-downstream 状态会把无证据数据写成确定事实，需人工确认
- Data impact: metadata snapshot、freshness/SLA/queryability/upstream/downstream 追溯面
- Rollback / recovery: 恢复未知/未采集默认语义，保留证据来源与回退字段
- Validation:
  - `metadata model 与 snapshot query 测试`
  - `python3 scripts/foreman.py validate D-TASK-070`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added metadata snapshot query APIs, lineage/evidence response models, in-memory snapshot repository, and default UNKNOWN or UNCOLLECTED status semantics for governance metadata evidence.
  - Validation evidence: mvn -pl governance -am clean -Dtest=MetadataSnapshotApplicationServiceTest,MetadataSnapshotControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-070
  - Residual risk: Snapshot evidence remains baseline repository data rather than live collection jobs, and the Java SDK baseline under D-TASK-068 is still open.
  - Next step: Finish the Java SDK baseline under D-TASK-068 and then continue the remaining open-access follow-up tasks.

### D-TASK-069: 固化数据源连接配置与健康检查契约

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add datasource config baseline`
- Priority: 1
- Depends on: `D-TASK-049`
- Scope: JDBC/API/Client/Gateway 连接方式、凭证、安全、测试连接、健康状态与失败原因契约 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若数据源连接配置与健康检查会落明文凭据、放宽租户隔离或把环境级 endpoint/secret 写入仓库真值，需人工确认
- Data impact: datasource 配置、测试连接、健康状态与失败原因查询面
- Rollback / recovery: 回退到只读 datasource 查询基线，移除高风险配置字段与敏感信息暴露
- Validation:
  - `datasource contract 与 health-check 测试`
  - `python3 scripts/foreman.py validate D-TASK-069`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added datasource configuration CRUD, connection-mode specific contract fields, masked credential handling, and baseline connection health-check responses for governance APIs.
  - Validation evidence: mvn -pl governance -am clean -Dtest=DatasourceConfigApplicationServiceTest,DatasourceConfigControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-069
  - Residual risk: Health-check behavior is simulated baseline logic without live external connectivity; metadata snapshot and asset evidence remain governed separately under D-TASK-070 and D-TASK-071.
  - Next step: Close out D-TASK-070 metadata snapshot baselines, then finish the Java SDK baseline under D-TASK-068.

### D-TASK-071: 落地数据资产与数据源治理查询/详情接口

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add metadata asset catalog endpoints`
- Priority: 1
- Depends on: `D-TASK-070`
- Scope: datasource/schema/table/logical-view/db-view 列表、详情与 metadata snapshot 查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若数据资产接口会扩大跨租户可见范围、暴露未授权对象详情或破坏现有查询性能边界，需人工确认
- Data impact: datasource/schema/table/logical-view/db-view 查询面与详情接口
- Rollback / recovery: 回退高风险详情字段与筛选面，恢复基础受保护查询
- Validation:
  - `data-asset API 与 detail query 测试`
  - `python3 scripts/foreman.py validate D-TASK-071`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added schema/table catalog and detail endpoints, and enriched logical-view/db-view responses with metadata snapshot evidence fields under the governance baseline.
  - Validation evidence: mvn -pl governance -am clean -Dtest=MetadataAssetCatalogApplicationServiceTest,MetadataAssetCatalogControllerTest,LogicalViewCatalogApplicationServiceTest,DatabaseViewCatalogApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-071
  - Residual risk: Datasource configuration and standalone metadata snapshot ledgers remain open under D-TASK-069 and D-TASK-070; current asset evidence is baseline in-memory data rather than live external collection.
  - Next step: Close out D-TASK-069 and D-TASK-070, then finish the Java SDK baseline under D-TASK-068.

### D-TASK-072: 落地系统管理配置接口基线

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(governance): add system management config baselines`
- Priority: 1
- Depends on: `D-TASK-071`
- Scope: 报表接口配置、Redis 规则源、装数协同策略与相关治理查询接口基线 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-013` 数据源与数据资产治理增强
- Human confirmation point: 若系统管理配置接口会把 mock/config abstraction 误写成真实外部联通、或允许未经审批的配置生效，需人工确认
- Data impact: 报表接口配置、Redis 规则源、装数协同策略与治理查询面
- Rollback / recovery: 回退到查询/模拟基线，保留抽象配置但禁用高风险生效路径
- Validation:
  - `system-management config/query 测试`
  - `python3 scripts/foreman.py validate D-TASK-072`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed system-management configuration baselines for report interfaces, Redis rule sources, and dispatch policies, including alias endpoints, update support, config-only enforcement semantics, and focused service/controller tests without claiming live external activation.
  - Validation evidence: mvn -pl governance -am clean -Dtest=ReportInterfaceConfigApplicationServiceTest,ReportInterfaceConfigControllerTest,RedisRuleSourceApplicationServiceTest,DispatchPolicyApplicationServiceTest,SystemManagementConfigControllerTest,DatasourceConfigApplicationServiceTest,MetadataSnapshotApplicationServiceTest,MetadataAssetCatalogApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-072; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Datasource/metadata governance tasks remain active in the workspace, and Redis/dispatch paths intentionally stay at config-only or simulated status until later environment-backed integration work.
  - Next step: Close out the remaining datasource/metadata governance tasks separately, then resume the open-access SDK delivery under D-TASK-068.

### D-TASK-067: 扩展 JDBC Agent `Local Rewrite + Direct JDBC`

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(jdbc-agent): add local rewrite direct jdbc mode`
- Priority: 1
- Depends on: `D-TASK-066`
- Scope: 本地轻量改写/路由后直连目标 JDBC，保留审计与失败回退 Tech: `JAVA-BE`,`OPS`. Layer: `common`,`deployments/ci/scripts`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 JDBC Agent `Local Rewrite + Direct JDBC` 会静默改写 SQL、绕过审计或改变查询语义，需人工确认
- Data impact: 本地改写规则、direct JDBC 路径与上报链
- Rollback / recovery: 回退为原 SQL 或 observe-only，保留改写失败记录
- Validation:
  - `local rewrite/direct JDBC 测试`
  - `python3 scripts/foreman.py validate D-TASK-067`
- Progress log:
  - 2026-04-27: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added JDBC Agent shared runtime for observe/governed/local-rewrite flows, including SQL comment/query-date observation, Redis-backed lightweight rewrite routing, direct JDBC fallback semantics, shared open-access HTTP clients, and focused JDBC agent tests/script for the local rewrite + direct JDBC baseline.
  - Validation evidence: scripts/run-open-access-tests.sh; python3 scripts/foreman.py validate D-TASK-067; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Redis-backed rewrite rules remain intentionally lightweight and repo-closed; real driver weaving and live datasource integration still require later environment-backed verification.
  - Next step: Close out D-TASK-065/D-TASK-066 against the shared agent baseline, then finish the Java SDK delivery under D-TASK-068.

### D-TASK-064: 落地 HTTP API 接入基线

- Status: done
- Completed at: 2026-04-27
- Commit subject: `feat(api): baseline protected access-channel handling`
- Priority: 1
- Depends on: `D-TASK-063`
- Scope: 对外 query/parse/history/recommendation API 入口基线 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若 HTTP API 接入会绕过统一鉴权/审计、扩大对外暴露面或删改既有契约，需人工确认
- Data impact: 外部 API、认证上下文、审计记录与错误响应
- Rollback / recovery: 回退对外入口到受保护最小基线，并保留现有内部契约
- Validation:
  - `API integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-064`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended HTTP API baseline so protected query/optimization entrypoints capture accessChannel/authSource in governance request payloads, and governance query-history normalizes accessChannel filters for external API callers.
  - Validation evidence: mvn -pl governance -am -Dtest=GovernanceAuditTrailServiceTest,GovernanceQueryHistoryControllerTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl query-execution -am -Dtest=QueryExecutionControllerTest,GovernanceHttpClientTest,QueryExecutionApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl sql-optimization -am -Dtest=StructureParseControllerTest,AccelerationRecommendationControllerTest,GovernanceHttpClientTest,OptimizationTaskApplicationServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-064
  - Residual risk: Recommendation list/detail APIs are protected and baseline-compatible, but richer access policy/audit query endpoints remain for later tasks.
  - Next step: Proceed to D-TASK-065 for the JDBC Agent Observe delivery after the protected HTTP API baseline is stable.

### D-TASK-063: 固化接入来源模型与统一审计契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): codify access audit channel contract`
- Priority: 1
- Depends on: `D-TASK-062`
- Scope: `PAGE/API/JDBC_AGENT/SDK/CLIENT` 模型与 access audit 字段统一 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-012` 开放接入与 JDBC Agent
- Human confirmation point: 若接入来源模型会让未受管入口绕过审计或混淆真实访问来源，需人工确认
- Data impact: access channel、access audit 与相关 headers/metadata
- Rollback / recovery: 恢复显式来源分类与统一审计，关闭不明来源入口
- Validation:
  - `access contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-063`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared access channel model, propagated X-Access-Channel through protected governance calls, and enforced canonical accessChannel handling in governance audit writes.
  - Validation evidence: mvn -pl governance -am -Dtest=GovernanceAuditTrailServiceTest -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false test; mvn -pl query-execution,sql-optimization -am -DskipTests compile; python3 scripts/foreman.py validate D-TASK-063
  - Residual risk: Upstream entrypoints still need broader adoption of X-Access-Channel headers to distinguish PAGE/JDBC/SDK/CLIENT beyond the API fallback path.
  - Next step: Proceed to D-TASK-064 to wire HTTP API baseline around the shared access audit contract.

### D-TASK-062: 固化“只管理不装数”的协同契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): codify dispatch collaboration contract`
- Priority: 1
- Depends on: `D-TASK-061`
- Scope: 外部拉取事件、非主动装数、回执与审计边界；不接真实装数执行 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若“只管理不装数”契约被扩展成直接执行装数、主动推送生产消息或默认联通外部模块，需人工确认
- Data impact: recommendation 协同契约、dispatch 行为、文档真值
- Rollback / recovery: 恢复 pull-only 与非执行边界，保留协同事件审计
- Validation:
  - `dispatch contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-062`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented dispatch collaboration contract endpoint and service surface enforcing PULL_ONLY semantics, no SQL execution, no data loading, no active external push, and external pull requirement; updated controller/service tests plus interface and product documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=DispatchEventApplicationServiceTest,DispatchEventControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-062; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: System-management configuration for dispatch policy surfaces is scheduled under D-TASK-072; this task fixes the runtime collaboration boundary.
  - Next step: Instantiate D-TASK-063 to codify access source model and unified audit contract.

### D-TASK-061: 打通推荐与历史/解析/路由的关联追溯

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add recommendation traceability`
- Priority: 1
- Depends on: `D-TASK-060`
- Scope: recommendation 与 history/parse/route/alert/batch 的 traceability keys Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若推荐关联追溯会跨租户串链、暴露不应展示的 route/parse/history 关系，需人工确认
- Data impact: recommendation trace keys、治理查询面与关联视图
- Rollback / recovery: 回退跨链关联字段，恢复受保护的最小追溯面
- Validation:
  - `traceability 测试`
  - `python3 scripts/foreman.py validate D-TASK-061`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented recommendation traceability keys for history, parse task, batch, route decision, alert, SQL fingerprint, report, and logical object references; added tenant-scoped recommendation trace endpoint with related dispatch events; updated mapper/schema/migration/docs and traceability tests.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=AccelerationRecommendationApplicationServiceTest,RecommendationTraceApplicationServiceTest,RecommendationTraceControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-061; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: D-TASK-062 will formalize the non-loading dispatch collaboration contract; this task returns reference keys only and does not hydrate external service details.
  - Next step: Instantiate D-TASK-062 to document and enforce the pull-only, non-executing dispatch contract.

### D-TASK-060: 落地推荐治理事件创建与状态机

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add dispatch event state machine`
- Priority: 1
- Depends on: `D-TASK-059`
- Scope: `DispatchEvent` create/publish/pull/ack/fail 状态机与审计链 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若治理事件状态机会绕过外部拉取模式、自动推送真实装数、或删除失败/待拉取状态，需人工确认
- Data impact: dispatch event、状态机、审计与回执链
- Rollback / recovery: 恢复 pull-based 协同边界，保留全部事件状态证据
- Validation:
  - `event state 测试`
  - `python3 scripts/foreman.py validate D-TASK-060`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented pull-based DispatchEvent lifecycle for recommendation dispatch, including CREATED/PUBLISHED/PULLED/ACKED/FAILED state transitions, recommendation dispatch endpoint, event list/detail and pull/ack/fail endpoints, in-memory and database repository baselines, SQL schema/migration, interface/data-model documentation, and focused tests.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=DispatchEventApplicationServiceTest,DispatchEventControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-060; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: D-TASK-061 will add cross-object trace keys and history/parse/route linkage; this task only covers dispatch event lifecycle and pull-based state semantics.
  - Next step: Instantiate D-TASK-061 to connect recommendation traceability with history, parse, route, alert, and batch surfaces.

### D-TASK-059: 扩展推荐对象类型与收益/风险模型

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add recommendation benefit risk model`
- Priority: 1
- Depends on: `D-TASK-058`
- Scope: 建模 `REWRITE/ACCELERATION/CREATE_TABLE/PREWARM/MAINTENANCE` 推荐类型与收益/风险字段 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-011` 推荐 SQL、加速建议与装数协同事件
- Human confirmation point: 若推荐对象扩展会把“建议”写成“已执行结果”、或削弱收益/风险边界，需人工确认
- Data impact: recommendation 对象、类型、收益/风险与状态字段
- Rollback / recovery: 恢复 recommendation 只读建议语义，保留新增字段为未执行状态
- Validation:
  - `recommendation domain 测试`
  - `python3 scripts/foreman.py validate D-TASK-059`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented read-only acceleration recommendation modeling with REWRITE/ACCELERATION/CREATE_TABLE/PREWARM/MAINTENANCE types, benefit/risk/status fields, tenant-scoped list/detail APIs, in-memory and database repository baselines, SQL schema/migration, interface/data-model documentation, and focused tests.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=AccelerationRecommendationApplicationServiceTest,AccelerationRecommendationControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-059; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Dispatch event state machine and external pull coordination start in D-TASK-060; recommendation objects remain advisory and non-executing.
  - Next step: Instantiate D-TASK-060 to implement DispatchEvent creation and lifecycle state semantics.

### D-TASK-058: 落地重要/紧急清单与优先级矩阵

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add parse priority matrix`
- Priority: 1
- Depends on: `D-TASK-057`
- Scope: priority matrix、important/urgent list 与 drill-through Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若重要/紧急矩阵会隐藏判定依据或用于替代原始 issue 结果，需人工确认
- Data impact: priority matrix、important/urgent 视图与排序逻辑
- Rollback / recovery: 恢复 issue 原始结果优先，矩阵仅作为派生视图
- Validation:
  - `matrix/list 测试`
  - `python3 scripts/foreman.py validate D-TASK-058`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented tenant-scoped parse priority matrix and important/urgent drill-through surfaces, including urgency buckets, SQL/issue/report counts, controller endpoints, tests, and interface documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseStatisticsApplicationServiceTest,ParseStatisticsControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-058; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Recommendation domain modeling starts in D-TASK-059; this task only exposes parse-statistics drill-through.
  - Next step: Instantiate D-TASK-059 to extend recommendation object types and benefit/risk model.

### D-TASK-057: 落地按报表统计与占比分析

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add report parse statistics`
- Priority: 1
- Depends on: `D-TASK-056`
- Scope: report dimension aggregation、影响报表数量与占比计算 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若按报表统计会放大不可靠 mock 数据、或把失败解析也计入成功占比，需人工确认
- Data impact: 报表聚合、占比计算与报表问题清单
- Rollback / recovery: 恢复成功/失败分层与 mock 标识，纠正聚合口径
- Validation:
  - `report aggregation 测试`
  - `python3 scripts/foreman.py validate D-TASK-057`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented by-report parse statistics with report-level SQL count, issue SQL count, issue count, ratios, highest priority, important/urgent flags, issue scenes, controller endpoint, tests, and interface documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseStatisticsApplicationServiceTest,ParseStatisticsControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-057; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Priority matrix and important/urgent drill-through remain in D-TASK-058.
  - Next step: Instantiate D-TASK-058 to add priority matrix and important/urgent list surfaces.

### D-TASK-056: 落地按 SQL 与问题场景统计

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add parse statistics APIs`
- Priority: 1
- Depends on: `D-TASK-055`
- Scope: parse overview、scene aggregation、single-SQL issue 统计 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若按 SQL / 场景统计会引入高成本查询、错误聚合或隐藏问题样本，需人工确认
- Data impact: 统计聚合、样本明细、索引与缓存面
- Rollback / recovery: 回退高成本聚合，恢复基础统计和样本可追溯性
- Validation:
  - `statistics API 测试`
  - `python3 scripts/foreman.py validate D-TASK-056`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented parse statistics overview, issue-scene aggregation, single-SQL issue statistics, tenant-scoped aggregation over parse batch items, repository findAll support, mapper coverage, and API contract documentation.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseStatisticsApplicationServiceTest,ParseStatisticsControllerTest,StructureParsePriorityScorerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-056; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Report-dimension aggregation and priority matrix/list views remain deferred to D-TASK-057 and D-TASK-058 as planned.
  - Next step: Instantiate D-TASK-057 to add report-dimension parse aggregation and ratio metrics.

### D-TASK-055: 固化解析统计口径与优先级评分

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): codify parse priority scoring`
- Priority: 1
- Depends on: `D-TASK-054`
- Scope: scene/domain/severity/priority/important/urgent 评分与聚合口径 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-010` 解析统计与优先级分层
- Human confirmation point: 若统计口径与优先级评分改变已确认的 severity/priority/important/urgent 语义，需人工确认
- Data impact: 评分规则、统计口径与相关查询面
- Rollback / recovery: 保留旧评分/口径并追加新规则，不覆盖历史结果
- Validation:
  - `scoring rule 测试`
  - `python3 scripts/foreman.py validate D-TASK-055`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented structure parse issue scenario catalog, scoring snapshot, default domain/severity/important/urgent normalization, documented scoring thresholds and scenario taxonomy, and added scoring regression coverage.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=StructureParsePriorityScorerTest,StructureParseControllerTest,ReportBatchApplicationServiceTest,ParseBatchControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-055; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: D-TASK-055 establishes scoring policy only; API-level parse statistics aggregation is intentionally deferred to D-TASK-056 and later report aggregation tasks.
  - Next step: Instantiate D-TASK-056 to expose parse overview, issue-scene aggregation, and single-SQL statistics surfaces.

### D-TASK-054: 接入报表接口配置与真实拉取抽象

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add report interface config resolver`
- Priority: 1
- Depends on: `D-TASK-053`
- Scope: governance 配置报表接口，sql-optimization 通过统一抽象调用；保留 mock 路径 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若报表接口抽象会直接绑定真实外部接口、落 secret/live inventory 或破坏 mock 可回退路径，需人工确认
- Data impact: 接口配置、client 抽象、报表 SQL 解析来源
- Rollback / recovery: 回退到 mock 路径并移除高风险外部绑定
- Validation:
  - `config/client abstraction 测试`
  - `python3 scripts/foreman.py validate D-TASK-054`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented governance-backed report interface configuration, internal resolve contract, sql-optimization ReportSqlResolver abstraction, HTTP API fetch client, and mock fallback preservation for report batch SQL resolution.
  - Validation evidence: mvn -pl sql-optimization,governance -am -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,GovernanceBackedReportSqlResolverTest,ReportInterfaceConfigApplicationServiceTest,GovernanceCapabilityApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-054; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The HTTP fetch abstraction intentionally supports only GET plus a configured JSON SQL field in this task; real endpoint secrets and live inventory remain out of repo scope and must be supplied through protected configuration later.
  - Next step: Instantiate D-TASK-055 to solidify parse issue scoring and priority taxonomy for statistics.

### D-TASK-053: 落地报表清单解析文件模拟入口

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add report batch mock resolution`
- Priority: 1
- Depends on: `D-TASK-052`
- Scope: 以 `report_code` 为主键，从 txt/mock source 获取 SQL 再解析 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若报表清单 mock 入口会被写成真实接口联通事实、或改变 `report_code` 唯一键语义，需人工确认
- Data impact: report batch 记录、mock source 解析与报表- SQL 映射
- Rollback / recovery: 恢复 txt/mock 语义与 `report_code` 主键边界
- Validation:
  - `mock resolve 测试`
  - `python3 scripts/foreman.py validate D-TASK-053`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented TXT/mock report catalog import keyed by report_code, report batch/item domain and persistence, resolve-sqls mock SQL generation, structure/access parse orchestration, and report batch detail APIs.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ReportBatchApplicationServiceTest,ReportBatchControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-053; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Real report API configuration and remote SQL fetching remain in the follow-up report interface task; current implementation is explicitly TXT/mock source only.
  - Next step: Instantiate the next report interface task to add governance-backed report endpoint configuration and resolver abstraction.

### D-TASK-052: 扩展 `xls/et` 兼容导入与失败语义

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add xls/et compatibility guidance for parse batches`
- Priority: 1
- Depends on: `D-TASK-051`
- Scope: 兼容 `xls/et`，失败时显式提示建议改用稳定格式 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若 `xls/et` 兼容支持会拖累主线、把兼容失败误写为平台故障，需人工确认
- Data impact: 兼容格式解析逻辑与失败提示
- Rollback / recovery: 回退兼容扩展到稳定格式基线，并保留失败原因说明
- Validation:
  - `compatibility 测试`
  - `python3 scripts/foreman.py validate D-TASK-052`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended batch ingestion to accept xls workbook payloads, preserved stable xlsx/csv/txt/sql import behavior, and added explicit ET failure guidance that recommends converting to XLSX or CSV.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,ParseBatchControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-052; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: ET support remains compatibility-oriented and may still require conversion guidance depending on provider payload shape; report-catalog resolution still belongs to the next task.
  - Next step: Instantiate D-TASK-053 to add the report catalog mock resolution path and SQL lookup orchestration.

### D-TASK-051: 落地 SQL/表格批量导入解析

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add stable batch parse ingestion`
- Priority: 1
- Depends on: `D-TASK-050`
- Scope: 稳定支持 `xlsx/csv/txt/sql` 导入与结构解析/access parse 编排 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若稳定格式导入解析会在失败时丢失原始记录、绕过审计或把 access parse 强制为同步阻断，需人工确认
- Data impact: 批量导入记录、parse task 批次编排与失败记录
- Rollback / recovery: 恢复结构解析优先与失败留痕，不删除原始批次记录
- Validation:
  - `import parsing 测试`
  - `python3 scripts/foreman.py validate D-TASK-051`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented stable xlsx/csv/txt/sql batch ingestion with parse_batch_item persistence, structure/access orchestration, retry-access baseline, and batch detail statistics.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,ParseBatchControllerTest,ParseBatchPersistenceSchemaMappingTest,AccessParseControllerTest,StructureParseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-051; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: XLS/ET compatibility and report-catalog resolution remain in later tasks; current ingest path assumes header-based tabular payloads and Base64 submission.
  - Next step: Instantiate D-TASK-052 to extend xls/et compatibility and stable failure guidance.

### D-TASK-050: 建立批量解析批次模型与模板契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add parse batch contract baseline`
- Priority: 1
- Depends on: `D-TASK-049`
- Scope: `ParseBatch`、模板列、导入模式和批次状态机基线 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-009` 批量解析与报表清单解析
- Human confirmation point: 若批量解析批次模型与模板契约会把兼容格式、mock source 或未校验列写成正式运行时默认，需人工确认
- Data impact: batch/task metadata、模板列、导入状态与批次统计
- Rollback / recovery: 保留稳定格式优先与 mock 边界，回退高风险模板/状态语义
- Validation:
  - `batch contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-050`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added ParseBatch domain/model persistence, create/detail parse-batch APIs, template-column and supported-file-type contracts, status-history baseline, schema/migration coverage, and matching interface/data-model documentation updates.
  - Validation evidence: mvn -pl sql-optimization -am -Dtest=ParseBatchApplicationServiceTest,ParseBatchControllerTest,ParseBatchPersistenceSchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-050; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Batch creation currently establishes only the contract baseline and READY state; actual file ingestion, batch listing, retry-access orchestration, and report catalog resolution remain for D-TASK-051 and later tasks.
  - Next step: Instantiate D-TASK-051 to add real SQL/tabular import ingestion and structure/access parse orchestration on top of the ParseBatch contract baseline.

### D-TASK-049: 统一逻辑对象在查询/历史/解析中的展示契约

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): unify logical object evidence surfaces`
- Priority: 1
- Depends on: `D-TASK-048`
- Scope: 统一查询、历史、解析、路由消费的 logical object DTO/VO 与 detail/list/export surfaces Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若逻辑对象统一展示会破坏现有 query/history/parse 契约兼容性，需人工确认
- Data impact: 跨服务 DTO/VO 与前端消费面
- Rollback / recovery: 保留旧 DTO/VO 兼容层，并回退统一对象字段为可选扩展
- Validation:
  - `cross-service contract 测试`
  - `python3 scripts/foreman.py validate D-TASK-049`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Unified logical object DTO and evidence rendering across structure-parse, query-history list/detail, trace detail, and export payload surfaces while preserving legacy logical-object evidence compatibility.
  - Validation evidence: mvn -pl governance,sql-optimization -am -Dtest=StructureParseContractTest,StructureParseControllerTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-049; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Query-execution runtime responses still need to emit the same shared logical-object surface once execution endpoints are materialized; current unification covers parse and governance consumption surfaces.
  - Next step: Instantiate D-TASK-050 to establish ParseBatch, import template, and batch-state contracts for the bulk parse workflow.

### D-TASK-048: 落地 DB View 识别与依赖展示

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add db view dependency resolution`
- Priority: 1
- Depends on: `D-TASK-047`
- Scope: 结构解析与历史追溯识别 DB View，并展示依赖对象 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若 DB View 识别会误把复杂对象链写成确定事实、放宽跨源依赖边界，需人工确认
- Data impact: DB view 依赖解析与展示数据
- Rollback / recovery: 保留已识别依赖为 evidence，回退高风险展开逻辑为摘要模式
- Validation:
  - `parser/integration 测试`
  - `python3 scripts/foreman.py validate D-TASK-048`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance-backed DB view catalog and dependency resolution, exposed public and internal db-view endpoints, and enriched structure-parse DB_VIEW hits with resolved dependency evidence for history/traceability consumers.
  - Validation evidence: mvn -pl sql-optimization,governance -am -Dtest=StructureParseControllerTest,GovernanceCapabilityApplicationServiceTest,DatabaseViewCatalogApplicationServiceTest,DatabaseViewCatalogControllerTest,TraceabilitySchemaMappingTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-048; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: History and export surfaces still need a unified logical-object display contract so DB_VIEW dependency evidence renders consistently across query/history/parse views; that alignment remains for D-TASK-049.
  - Next step: Instantiate D-TASK-049 to unify logical object display contracts across query, history, parse, and export surfaces.

### D-TASK-047: 落地业务逻辑视图目录与映射

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add business logical view catalog`
- Priority: 1
- Depends on: `D-TASK-046`
- Scope: governance 中的 business logical view 目录、映射、物理表关联与查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若业务逻辑视图目录与映射会引入未确认业务口径、删除既有物理映射或放宽租户隔离，需人工确认
- Data impact: logic view 目录、映射表与相关治理查询
- Rollback / recovery: 恢复原目录/映射快照，关闭高风险对象或映射规则
- Validation:
  - `repository/controller 测试`
  - `python3 scripts/foreman.py validate D-TASK-047`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance-side business logical view catalog and logical object mapping persistence surfaces, exposed /api/governance/logical-views list/detail endpoints, and wired schema/migration plus repository/service/controller tests for the new directory and mapping query face.
  - Validation evidence: mvn -pl governance -am -Dtest=TraceabilitySchemaMappingTest,LogicalViewCatalogApplicationServiceTest,LogicalViewCatalogControllerTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-047; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The catalog currently serves governance-owned directory records only; parser-driven DB_VIEW dependency population and unified history/query display alignment remain for D-TASK-048 and D-TASK-049.
  - Next step: Instantiate D-TASK-048 to recognize DB_VIEW dependencies in the parse chain and connect those hits to the new logical view catalog surfaces.

### D-TASK-046: 建立逻辑对象统一模型

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(shared): unify logical object reference contract`
- Priority: 1
- Depends on: `D-TASK-045`
- Scope: 建立 `BUSINESS_VIEW/DB_VIEW/TABLE` 的统一对象契约与跨服务引用字段 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-008` 逻辑视图与 DB View 治理
- Human confirmation point: 若统一逻辑对象模型会混淆 `BUSINESS_VIEW` 与 `DB_VIEW` 语义、扩大对象默认可见范围，需人工确认
- Data impact: 逻辑对象目录、引用键、跨服务 DTO
- Rollback / recovery: 通过兼容视图恢复双模对象边界，并保留已落库对象数据
- Validation:
  - `contract 与 DTO 测试`
  - `python3 scripts/foreman.py validate D-TASK-046`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a shared logical object reference contract in sqlforge-shared, enriched structure parse logical object hits with objectKey/catalog/schema fields, aligned governance history type extraction with the unified objectType key, and updated interface/data-model docs for the canonical logical object reference surface.
  - Validation evidence: mvn -pl sql-optimization,governance -am -Dtest=StructureParseContractTest,StructureParseControllerTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/foreman.py validate D-TASK-046; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Only structure-parse and governance-history consumers are wired to the shared logical object contract so far; query-execution and richer history/detail display alignment remain for D-TASK-047 and later tasks.
  - Next step: Instantiate D-TASK-047 to extend business logical view directory and mapping persistence against the new shared logical object reference contract.

### D-TASK-045: 补齐解析综合结论与 partial-success 追溯

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add combined parse conclusion traceability`
- Priority: 1
- Depends on: `D-TASK-044`
- Scope: 统一结构解析成功 + access parse 失败时的综合状态、查询面与历史追溯 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若综合结论会隐藏 partial success、抹平结构与 access parse 的状态差异，或删除 failure evidence，需人工确认
- Data impact: parse task 总状态、历史详情与统计聚合
- Rollback / recovery: 恢复双轨状态分开展示，保留 partial success 证据与失败原因
- Validation:
  - `state machine 与 history/detail 测试`
  - `python3 scripts/foreman.py validate D-TASK-045`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added combined parse conclusion and status-history surfaces, preserved partial-success evidence when access parse degrades, and extended controller tests for waiting/success/partial-success query flows.
  - Validation evidence: mvn -pl sql-optimization -Dtest=AccessParseControllerTest,StructureParseControllerTest,StructureParseContractTest,StructureParsePriorityScorerTest,StructureParseResultTest test; python3 scripts/foreman.py validate D-TASK-045; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Combined parse state is still in-memory and not persisted; history/detail propagation remains for later governance tasks.
  - Next step: Instantiate D-TASK-046 to unify logic-object contracts and continue the repo-side Wave 1/Wave 2 parsing chain.

### D-TASK-044: 落地数据访问解析入口与异步补跑语义

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add access parse async follow-up baseline`
- Priority: 1
- Depends on: `D-TASK-043`
- Scope: 结构解析成功后自动异步补跑 access parse，保留 unavailable/skipped/failed 语义与服务状态 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若数据访问解析会阻断结构解析返回、把外部服务不可用误写成整体成功，或引入未确认的默认重试策略，需人工确认
- Data impact: access parse 任务、服务状态、可达性/计划/分区/SLA 证据
- Rollback / recovery: 恢复结构解析先返回、access parse 独立失败的既定语义，停用自动补跑
- Validation:
  - `async parse flow 测试、降级测试`
  - `python3 scripts/foreman.py validate D-TASK-044`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added access-parse endpoint semantics, combined parse submission/poll endpoints, in-memory async follow-up flow, and unavailable/skipped/failed service-state handling on top of the structure parse baseline.
  - Validation evidence: mvn -pl sql-optimization -Dtest=AccessParseControllerTest,StructureParseControllerTest,StructureParseContractTest,StructureParsePriorityScorerTest,StructureParseResultTest test; python3 scripts/foreman.py validate D-TASK-044; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Combined parse state is still in-memory and not persisted; object resolution remains provider-light and SLA/freshness stay conservative UNKNOWN until D-TASK-045 and later governance metadata work.
  - Next step: Instantiate D-TASK-045 to unify partial-success status, add richer combined parse query surfaces, and prepare history/persistence handoff for later waves.

### D-TASK-043: 落地单条结构解析入口

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): add structure parse endpoint baseline`
- Priority: 1
- Depends on: `D-TASK-042`
- Scope: 不依赖数据库的结构解析、query-date 提取、逻辑对象命中与 rewrite candidate 输出 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若结构解析入口引入数据库依赖、阻断查询主路径或把低置信度结果伪装成高置信度，需人工确认
- Data impact: structure parse 任务、结构化问题、query-date 与逻辑对象命中证据
- Rollback / recovery: 关闭高成本分析支路，保留基础语法/结构解析与低置信度标识
- Validation:
  - `parse structure controller/service 测试`
  - `python3 scripts/foreman.py validate D-TASK-043`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added the first synchronous structure parse endpoint, request/response contract wiring, query-date extraction, logical-object hits, rewrite candidate projection, and INVALID degradation behavior without database dependencies.
  - Validation evidence: mvn -pl sql-optimization -Dtest=StructureParseControllerTest,StructureParseContractTest,StructureParsePriorityScorerTest,StructureParseResultTest test; python3 scripts/foreman.py validate D-TASK-043; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Structure parse currently infers DB_VIEW hits heuristically and does not yet use governed logical-view catalogs or access parse evidence; D-TASK-044 will add access-parse semantics and D-STORY-008 will enrich object resolution.
  - Next step: Instantiate D-TASK-044 to add access-parse entry semantics and asynchronous follow-up on top of the new structure parse baseline.

### D-TASK-042: 固化结构解析契约与问题分类模型

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): freeze structure parse contract baseline`
- Priority: 1
- Depends on: `D-TASK-041`
- Scope: 定义结构解析响应、问题域/场景、severity/priority/important/urgent 评分基线 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-007` 结构解析与数据访问解析双轨闭环
- Human confirmation point: 若结构解析契约会把未实现的语义分析写成既成事实、删减问题分类维度或改变严重度/优先级口径，需人工确认
- Data impact: 解析响应、问题分类、统计口径与文档基线
- Rollback / recovery: 恢复上一版问题分类与评分字段，保留新增字段为可选扩展
- Validation:
  - `parser/domain 契约测试`
  - `python3 scripts/foreman.py validate D-TASK-042`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Defined structure parse domain/result contract, issue taxonomy, aggregate priority scoring baseline, typed response VOs, and synced interface/product specs for D-STORY-007.
  - Validation evidence: mvn -pl sql-optimization -Dtest=StructureParsePriorityScorerTest,StructureParseResultTest,StructureParseContractTest test; python3 scripts/foreman.py validate D-TASK-042; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: No structure parse controller or parser implementation yet; D-TASK-043 will bind these contracts to the actual parse endpoint and extraction logic.
  - Next step: Instantiate and implement D-TASK-043 using the frozen structure parse contract for the first repo-side structure parse endpoint.

### D-TASK-041: 补齐 SQL 历史导出与取证视图

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add query history export baseline`
- Priority: 1
- Depends on: `D-TASK-040`
- Scope: 为 `CSV/EXCEL/JSON/SQL/PDF` 导出、单次执行取证字段与审计链接补齐基线 Tech: `JAVA-BE`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若导出/取证视图会放宽敏感字段输出、破坏脱敏语义或把 PDF/SQL 导出写成默认生产事实，需人工确认
- Data impact: 导出记录、取证视图、导出载荷与审计链
- Rollback / recovery: 恢复原导出白名单与脱敏策略，禁用高风险格式并保留导出审计记录
- Validation:
  - `export 契约测试、审计联动测试`
  - `python3 scripts/foreman.py validate D-TASK-041`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added query-history export endpoint, SQL tri-state evidentiary detail fields, inline export formats, and export_record plus audit_log linkage for history forensics.
  - Validation evidence: mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest,TraceabilitySchemaMappingTest test; python3 scripts/foreman.py validate D-TASK-041; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: CSV/EXCEL/PDF exports are inline evidentiary baselines in phase 1 rather than binary file rendering; broader parse/recommendation source refs remain to be populated by downstream tasks.
  - Next step: Instantiate and implement D-TASK-042 to freeze structure-parse contracts and issue taxonomy on top of the history evidentiary surfaces.

### D-TASK-040: 落地 SQL 历史列表与详情查询面

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): add query history list and detail surfaces`
- Priority: 1
- Depends on: `D-TASK-039`
- Scope: 为历史列表、详情、筛选、分类、route/parse/recommendation/benchmark drill-through 建立治理查询面 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若历史查询面会引入越权钻取、跨租户可见性扩大或破坏已存在分页/审计约束，需人工确认
- Data impact: governance 历史查询、详情、关联 drill-through 与索引
- Rollback / recovery: 回退新增筛选/详情能力，恢复原历史查询面并保留新索引/字段供后续受控启用
- Validation:
  - `governance history list/detail 测试`
  - `python3 scripts/foreman.py validate D-TASK-040`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance query-history list/detail APIs, mapper projections, classification summary, and trace drill-through coverage for SQL history surfaces.
  - Validation evidence: mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest,TraceabilitySchemaMappingTest test; python3 scripts/foreman.py validate D-TASK-040; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Classification summaries are page-scoped; history export and dedicated evidentiary export surfaces remain in D-TASK-041.
  - Next step: Instantiate and implement D-TASK-041 to add SQL history export and evidentiary view completion.

### D-TASK-038: 扩展 query-history / execution-result 追溯字段

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): extend query history traceability surfaces`
- Priority: 1
- Depends on: `D-TASK-037`
- Scope: 为 comment context、report/stage/biz-date、query-date、SQL 三态、逻辑对象命中、access channel 与 route/cache summary 补齐持久化与查询字段 Tech: `JAVA-BE`,`SQL`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-006` 查询历史与执行取证闭环
- Human confirmation point: 若历史追溯字段扩展会改变既有审计语义、删除已存证的 SQL/route/cache/trace 信息，或把未确认字段写成强制事实，需人工确认
- Data impact: `query_history`、`execution_result`、导出/取证查询字段与索引
- Rollback / recovery: 保留既有追溯链并以追加字段方式扩展；必要时通过视图/兼容 DTO 回退查询面
- Validation:
  - `governance/query-execution schema 与 mapping 测试、history persistence 测试`
  - `python3 scripts/foreman.py validate D-TASK-038`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded query_history and execution_result persistence baselines with structured traceability fields for comment context, datasource/report/stage dates, access channel, SQL tri-state evidence, logical object hits, and route/cache summaries; projected legacy queryContext/resultSummary JSON into the new columns; exposed the new history evidence surface through governance trace detail VO mappings; added incremental migration coverage and schema/persistence/history tests for the expanded traceability contract.
  - Validation evidence: mvn -pl governance -Dtest=TraceabilitySchemaMappingTest,GovernanceProtectedPersistenceServiceTest,GovernanceHistoryApplicationServiceTest test; python3 scripts/foreman.py validate D-TASK-038; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Current writer call sites still populate most new fields through JSON projection rather than explicit DTO fields, so D-TASK-037 and later query-execution/sql-optimization contract tasks still need to supply first-class values for full fidelity and broader history filters.
  - Next step: Instantiate and implement the next Wave 1 baseline task that adds the query-execution side contract surface, then continue with single-query structure parsing and access-parse orchestration tasks on top of the expanded history substrate.

### HARN-043: 修复 SQL 治理规格包 follow-up 真值缺口并启动 Wave 1

- Status: done
- Completed at: 2026-04-26
- Commit subject: `docs(plans): reconcile sql governance spec gaps and wave1 start`
- Priority: 1
- Depends on: `HARN-042`
- Scope: 在不新增微服务、不改写 `HARN-042` 历史完成语义、不引入第二套长期真值的前提下，修复 `HARN-042` closeout 后主计划 active-wave 漂移，补齐接口/枚举/只读执行边界与数据模型漏项，并新增数据源/数据资产/系统管理的缺失 Story/Task inventory；随后以当前仓库真值启动 Wave 1 首个 repo-side mainline task。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-A / Story `A-STORY-009` SQL 治理实施规格与任务塑形
- Human confirmation point: 若要借本任务改变 `HARN-042` 已归档历史、扩大“不新增微服务”边界、把接口/枚举/只读执行限制以外的实现内容偷渡进来，或跳过 Wave 1 任务正常 instantiate 流程，需人工确认。
- Data impact: 修复 `HARN-042` follow-up 真值缺口的规格/计划/矩阵文本、补充的数据源/系统管理 Story/Task inventory，以及 Wave 1 启动前的治理收口；不直接修改业务运行时数据。
- Rollback / recovery: 回退时仅回退 `HARN-043` 新增的规格/计划/矩阵修补与 inventory 增量，恢复到 `HARN-042` closeout 后状态；若 Wave 1 已实例化，则通过追加治理修正保留既有 task evidence。
- Validation:
  - `python3 scripts/foreman.py validate HARN-043`、`python3 scripts/foreman.py compile-governance --check`、`node scripts/lint-repository-knowledge.js`、`python3 scripts/task_audit.py --check --phase pre-closeout`、规格包/计划/矩阵交叉检查
  - `python3 scripts/foreman.py validate HARN-043`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled the SQL governance spec pack after HARN-042 by fixing active-wave truth, tightening read-only execution and interface/state contracts, filling missing data model objects, and adding missing datasource/data-asset/system-management Story-Task inventory needed before Wave 1 implementation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-043; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Wave 1 business tasks remain uninstantiated until the first repo-side mainline task is materialized; current runtime state directories are still untracked operational residue and are intentionally excluded from closeout.
  - Next step: Instantiate D-TASK-038 as the first Wave 1 repo-side mainline task, then implement query-history and execution-result trace-field persistence in dependency order.

### HARN-042: 落地 SQL 治理实施规格包与完整任务清单

- Status: done
- Completed at: 2026-04-26
- Commit subject: `docs(plans): land sql governance spec pack and task inventory`
- Priority: 1
- Depends on: HARN-041
- Scope: Add the SQL governance implementation specification pack and write the full downstream Story/Task inventory into the master execution plan, task-spec matrix, and governance extension matrix without implementing business code.
- Validation:
  - `python3 scripts/foreman.py validate HARN-042`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Landed the SQL governance implementation spec pack, interface/data/degradation baselines, and the full D/E/F story-task inventory into the execution plan and governance matrices without changing runtime business code.
  - Validation evidence: python3 scripts/foreman.py validate HARN-042; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js
  - Residual risk: Follow-on business tasks are not yet instantiated, so the new plan remains design-time truth until Wave 1 implementation begins; environment-backed integrations such as real report APIs, mail delivery, and loader consumers remain intentionally mocked or abstracted.
  - Next step: Materialize Wave 1 repo-side tasks for query history, execution traceability, and dual-track parse foundations, then implement them in dependency order.

### HARN-041: Runtime Reservation Pause/Cleanup and Demand Re-entry Governance

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(governance): close HARN-041 reservation lifecycle governance`
- Priority: 1
- Depends on: `HARN-028`,`HARN-036`,`HARN-038`
- Scope: 为 SQLForge governed intake/task-shaping reservation 增加 paused/archived/abandoned 生命周期语义，并把 healthcheck、runtime dashboard、cleanup 与 playbook 的判定对齐到同一治理模型。保留 HARN-029/HARN-030 的 shaping 证据但不把它们转成正式实现任务；将 HARN-040 停留在暂停候选/未确认状态，不继续 confirm-run。完成后必须能让新的 requirement 重新进入 governed intake/shaping，同时保持现有 preflight/instantiate/task_audit/closeout 边界不变。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-041-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要把 paused/archived/abandoned 语义扩展为自动重写台账真值、静默删除 runtime evidence、或允许未确认 candidate 继续 confirm-run，则必须先人工确认；本任务仅允许在现有 governed intake/runtime 边界内补齐可审计状态与恢复入口。
- Data impact: 仅修改 governed runtime reservation/intake/task-shaping 状态语义、文档与运行时清理逻辑；不直接修改业务运行时数据，不把候选证据写成仓库长期真值。
- Rollback / recovery: 若新增 reservation 生命周期语义导致 confirm-run、healthcheck 或 cleanup 行为异常，回退相关脚本与文档改动，并将受影响 reservation 状态恢复到先前的 released/candidate_ready/materialized 语义；历史 shaping evidence 保留在 .codex/state 下，不删除现有证据文件。
- Validation:
  - `python3 scripts/foreman.py validate HARN-041`
  - `python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/governed_runtime_dashboard.py`
  - `python3 scripts/governed_healthcheck.py --check`
  - `python3 scripts/governed_runtime_dashboard.py --json`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-26: implemented auditable reservation lifecycle states, healthcheck/dashboard gating, and candidate pause/archive semantics; archived HARN-029/HARN-030 and paused HARN-040 through governed runtime tooling.
- Context closeout:
  - Completed scope: Implemented auditable paused/archived/abandoned reservation lifecycle semantics; updated governed healthcheck/runtime dashboard/materialize behavior; archived HARN-029/HARN-030 dry-run candidates; paused HARN-040; documented candidate pause/resume/re-entry governance.
  - Validation evidence: python3 scripts/foreman.py validate HARN-041; python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/governed_runtime_dashboard.py; python3 scripts/governed_runtime_dashboard.py --json; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Older released reservations such as HARN-032 and HARN-039 still appear in runtime dashboard archive preview and may need a follow-up archival sweep, but they no longer block governed healthcheck.
  - Next step: Use governed_runtime_dashboard.py lifecycle actions for future candidate pause/archive/resume decisions and rerun governed intake/shaping when archived candidates need fresh task packs.

### D-TASK-037: 收口 cache capacity / eviction / metrics governance baseline

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-037 cache capacity governance`
- Priority: 1
- Depends on: `D-TASK-036`
- Scope: 在保持 D-TASK-036 provider-neutral cache backend、默认 repo-closed 主路径、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，为 cache governance 补齐 per-tenant / per-policy capacity limit、TTL 与 capacity/manual/schema eviction reason evidence、cache hit/miss/bypass/backfill/invalidate/backend-unavailable metrics、policy verify capacity/backend health summary，并让 benchmark/governance 继续透出 eviction/capacity evidence；真实 Redis 集群长跑和恢复...
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 cache capacity / eviction / metrics governance 会放宽缓存新鲜度边界、让过期或被驱逐 entry 继续命中、引入高基数指标标签、绕过统一授权入口或治理审计、或把真实 Redis 长跑环境写成仓库默认事实，需人工确认
- Data impact: cache policy capacity/ttl 配置、tenant/policy capacity counters、eviction reason evidence、cache governance metrics、policy verify runtime summary、benchmark/governance cache surface
- Rollback / recovery: 保持 D-TASK-036 repo-closed 默认主路径和 fail-closed 语义，关闭高风险 capacity/ttl 配置或 metrics 标签，回退新增 eviction/capacity/metrics 语义与文档说明，并恢复到 `D-TASK-036` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、cache capacity/eviction 契约测试、cache governance metrics 断言、policy verify summary 测试、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-037`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented repo-side cache capacity / eviction / metrics governance baseline: policy maxEntries/ttlSeconds, per-tenant/per-policy capacity evidence, TTL/CAPACITY/MANUAL/SCHEMA eviction reasons, low-cardinality cache governance metrics, verify runtime capacity/backend health summary, and benchmark/governance evidence propagation.
  - Validation evidence: mvn -B -pl query-execution -am test -DskipITs -Dtest=QueryExecutionCacheGovernanceRuntimeServiceTest,QueryExecutionApplicationServiceTest,QueryExecutionBenchmarkWorkloadServiceTest -Dsurefire.failIfNoSpecifiedTests=false; mvn -B -pl query-execution,benchmark-engine,governance -am test -DskipITs -Dtest=QueryExecutionCacheGovernanceRuntimeServiceTest,QueryExecutionApplicationServiceTest,QueryExecutionInternalControllerTest,QueryExecutionBenchmarkWorkloadServiceTest,BenchmarkGovernanceTraceServiceTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate D-TASK-037; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Real Redis cluster long-run evidence and cross-node recovery drills remain environment-backed follow-up; default repo path remains IN_MEMORY/fail-closed and does not enable provider cache by default.
  - Next step: Shape the next repo-side Phase-D follow-up from current repository truth, or run environment-backed Redis long-run/recovery validation outside the default repo path when the environment is available.

### HARN-039: Reconcile D-TASK-036 post-closeout plan truth

- Status: done
- Completed at: 2026-04-26
- Commit subject: `fix(governance): reconcile D-TASK-036 plan truth`
- Priority: 1
- Depends on: D-TASK-036
- Scope: 修正 D-TASK-036 closeout 后 master-execution-plan 当前波次仍把 D-TASK-036 写成下一条候选任务的文档真值漂移；只更新计划叙事与运行台账，不改业务代码，不塑形新的业务任务。
- Validation:
  - `python3 scripts/foreman.py validate HARN-039`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled the master execution plan after D-TASK-036 closeout so the current active wave no longer points at D-TASK-036 as the next candidate task, records D-TASK-036 as completed, and restores the repo-side mainline state to no instantiated task.
  - Validation evidence: python3 scripts/foreman.py validate HARN-039; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: No business implementation changed; future Phase-D work still requires explicit shaping and instantiation before execution.
  - Next step: When the next repo-side priority is chosen, shape a new formal task from current repository truth instead of reusing completed D-TASK-036.

### D-TASK-036: 推进 provider-native distributed cache governance backend

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-036 cache backend`
- Priority: 1
- Depends on: `D-TASK-035`
- Scope: 在保持 repo-closed in-memory cache governance baseline、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，为 `query-execution` 补齐 provider-neutral distributed cache backend contract、environment-backed carrier 语义、provider-native evidence、失败降级与可审计读写校验；默认仍不启用外部 provider，不把 Redis/provider cache 写成仓库默认事实 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/c...
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 distributed cache backend 会放宽数据新鲜度/一致性边界、绕过统一授权入口或治理审计、把 provider/Redis 依赖写成仓库默认主路径、引入明文凭据或 fail-open 命中语义，需人工确认
- Data impact: cache backend 配置、provider-native 读写/校验证据、cache policy apply/verify/invalidate 证据、命中/旁路/回填/失效数据、跨服务审计记录
- Rollback / recovery: 保持 repo-closed in-memory cache governance baseline 为默认主路径，关闭 environment-backed distributed provider 默认启用，回退新增 backend contract/provider evidence/降级语义与文档说明，并恢复到 `D-TASK-035` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/governance 模块测试、distributed cache backend contract 测试、cache policy apply/verify/invalidate backend 证据测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-036`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Synced Phase-D plan truth after D-TASK-035, shaped D-TASK-036, and implemented provider-neutral cache backend governance for query-execution with default in-memory backend, explicit Redis RESP provider adapter, backend/provider evidence, fail-closed bypass semantics, focused tests, and documentation updates.
  - Validation evidence: mvn -B -pl query-execution,governance -am test -DskipITs -Dtest=QueryExecutionCacheGovernanceRuntimeServiceTest,QueryExecutionApplicationServiceTest,QueryExecutionInternalControllerTest,QueryExecutionBenchmarkWorkloadServiceTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate D-TASK-036; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Redis/provider backend remains explicitly configured environment-backed path; live multi-node Redis recovery, eviction, capacity governance, and long-running provider evidence remain future hardening.
  - Next step: Add environment-backed Redis smoke and eviction/capacity governance once a real distributed cache environment is available.

### D-TASK-035: 收口真正的缓存治理能力

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-035 cache governance`
- Priority: 1
- Depends on: `D-TASK-034`
- Scope: 在保持查询执行主路径、统一授权入口、治理审计、缓存一致性与只读边界不变的前提下，建立可审计的 cache governance 模型、命中/失效/旁路/回填/风险标记语义，以及与 query-execution/sql-optimization/benchmark 的最小联动闭环，不把缓存元数据占位误写成已治理完成 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若缓存治理能力会放宽数据新鲜度/一致性边界、让缓存旁路/回填绕过授权或审计、把元数据占位误写成真实 cache governance，需人工确认
- Data impact: cache policy、命中/失效/旁路/回填/风险标记数据、跨服务治理与审计证据、相关 schema 与运行文档
- Rollback / recovery: 保持当前无强治理缓存默认边界，关闭高风险 cache policy 默认启用，回退新增 cache governance 字段、策略与文档说明，并恢复到 `D-TASK-034` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/sql-optimization/governance 模块测试、cache governance 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-035`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented governed result-cache apply/verify/invalidate runtime, schemaVersion-aware hit/backfill/bypass/invalidation semantics, query-execution metadata/audit evidence, benchmark evidence propagation, governance cacheGovernanceSurface aggregation, focused tests, and architecture docs.
  - Validation evidence: mvn -B -pl query-execution,benchmark-engine,governance -am test -DskipITs -Dtest=QueryExecutionApplicationServiceTest,QueryExecutionInternalControllerTest,QueryExecutionBenchmarkWorkloadServiceTest,BenchmarkGovernanceTraceServiceTest,GovernanceHistoryApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false; python3 scripts/foreman.py validate D-TASK-035; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Cache runtime is repository-closed in-memory baseline; distributed provider-native cache backing remains a future hardening step.
  - Next step: Evaluate provider-native distributed cache backing and cache eviction/observability integration after governed semantics stabilize.

### D-TASK-034: 收口 `benchmark-engine` 外部队列/文件存储与 provider-native 语义

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(benchmark-engine): close D-TASK-034 external queue carrier`
- Priority: 1
- Depends on: `D-TASK-033`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 `benchmark-engine` 补齐外部队列 carrier、文件存储编排与 provider-native 语义边界，把 provider-backed write/readback/cleanup/recovery 证据推进到更接近真实运行形态的基线 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 benchmark-engine 的外部队列/文件存储/provider-native 语义会让 environment-backed path 误写成仓库默认主路径、引入未经确认的 provider SDK/凭据写入、或绕过既有鉴权/审计边界，需人工确认
- Data impact: external queue/storage/provider-native 配置与运行摘要、artifact cleanup/recovery/write/readback 证据、跨服务追溯与审计留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭外部队列/provider-native 默认启用，回退新增 queue/storage/provider 语义与文档说明，并恢复到 `D-TASK-033` 已验证基线
- Validation:
  - `sqlforge-shared/benchmark-engine/governance 模块测试、external queue/storage/provider-native 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-034`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: benchmark-engine external queue carrier, task queue evidence surface, provider-native artifact evidence assertions, and benchmark architecture/contract docs
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-034; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; mvn -B -pl benchmark-engine -am test -DskipITs -Dtest=BenchmarkTaskApplicationServiceTest,BenchmarkTaskWorkerTest,BenchmarkArtifactStorageServiceTest,BenchmarkArtifactGovernanceOperationServiceTest -Dsurefire.failIfNoSpecifiedTests=false
  - Residual risk: external-file-queue remains repo-closed file-spool evidence, not a provider-native message broker or cross-host distributed queue
  - Next step: D-TASK-035 cache governance baseline across query-execution/sql-optimization/benchmark/governance

### D-TASK-033: 收口 `query-execution` 生产级 Hetu 集群证据与路由参数校准

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(query-execution): close D-TASK-033 hetu route calibration`
- Priority: 1
- Depends on: `D-TASK-032`
- Scope: 在保留 repo-closed Hetu 多模式执行链、统一授权入口、只读/影子环境边界与结构化失败语义不变的前提下，补齐生产级 Hetu/MRS 集群证据、路由参数校准、模式优先级与失败分层证据沉淀，不把外部测试环境依赖误写成仓库默认主路径 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 Hetu 集群证据与路由参数校准会放宽只读/影子环境边界、把外部测试环境结果误写成 repo-closed 默认事实、或降低当前结构化失败语义，需人工确认
- Data impact: Hetu/MRS route calibration 参数、mode priority、env smoke/test-env evidence、执行与审计记录
- Rollback / recovery: 保持 repo-closed Hetu 主路径与当前失败语义不变，关闭高风险校准默认启用，回退新增 calibration/live-evidence 文档与配置说明，并恢复到 `D-TASK-032` 已验证基线
- Validation:
  - `query-execution 模块测试、route calibration 契约测试、runtime smoke、Hetu env smoke/test-env evidence、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-033`
- Progress log:
  - 2026-04-26: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed Hetu route calibration and cluster-evidence snapshots for query-execution, calibrated mode priority/readiness/failure-layer routing, public route metadata, env-smoke evidence bundling, focused tests, and authority-doc updates while preserving repo-closed defaults and structured route failures.
  - Validation evidence: mvn -B -pl query-execution -am test -DskipITs; python3 scripts/foreman.py validate D-TASK-033 --include-task-audit with focused module/script checks; bash scripts/run-runtime-smoke.sh --compose-check; bash scripts/run-hetu-env-smoke.sh --help; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: External Win10/Hetu live evidence capture and long-term archived smoke logs remain environment-backed follow-up work under HARN-016 / INBOX-002; broader cross-service audit compensation still remains beyond this task scope.
  - Next step: Proceed to D-TASK-034 to close benchmark-engine external queue/storage provider-native semantics on top of the stabilized query-execution route-calibration baseline.

### D-TASK-032: 收口 acceleration plan 治理闭环

- Status: done
- Completed at: 2026-04-26
- Commit subject: `feat(sql-optimization): close D-TASK-032 acceleration plan loop`
- Priority: 1
- Depends on: `D-TASK-031`
- Scope: 在保持统一授权入口、治理审计、tenant 隔离与 `sql-optimization` suggestion 链不变的前提下，补齐 acceleration plan 的提交、审批/确认、应用、验证、回滚与长期追溯闭环，使 acceleration 不再只是建议元数据而成为受治理的正式对象 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 acceleration plan 治理闭环会放宽审批/确认边界、绕过统一授权入口与治理审计、允许 fail-open 应用或省略回滚/验证证据，需人工确认
- Data impact: acceleration plan / apply / verify / rollback 状态、跨服务治理记录、授权与审计证据、相关 schema 与契约载荷
- Rollback / recovery: 保持 suggestion-only 默认边界，关闭 plan apply 默认启用，回退新增 acceleration governance 字段、状态机与文档说明，并恢复到 `D-TASK-031` 已验证基线
- Validation:
  - `sqlforge-shared/sql-optimization/query-execution/governance 模块测试、跨服务 acceleration plan 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-032`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed acceleration-plan submit/approve/apply/verify/rollback flow across sql-optimization, query-execution, governance, shared contracts, schema, tests, and authority docs while preserving authorization, audit, tenant isolation, and suggestion-first defaults.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-032; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Production-grade live Hetu evidence, route calibration, and broader environment-backed acceleration execution proof remain follow-up work; suggestion-first remains the safe default when governed apply prerequisites are unavailable.
  - Next step: Proceed to D-TASK-033 to collect production-grade Hetu cluster evidence and route calibration on top of the new governed acceleration-plan baseline.

### D-TASK-031: 推进 `sql-optimization` 真实 parse/rewrite/acceleration suggestion 链

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(sql-optimization): D-TASK-031 add real parse rewrite pipeline`
- Priority: 1
- Depends on: `D-TASK-030`
- Scope: 在保留 MySQL `optimization_task` carrier、scheduled worker、统一授权入口与异步任务契约不变的前提下，把 `sql-optimization` 从 placeholder suggestion 推进到真实 SQL parser / AST analysis / rewrite rule / acceleration suggestion pipeline，输出可执行的 rewrite candidate、结构化 parse artifact、加速建议工件与失败阶段证据，不提前引入跨服务自动应用或审批旁路 Tech: `JAVA-BE`,`SQL`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实 parse/rewrite/acceleration suggestion 链会绕过统一授权入口、把高风险 rewrite 直接自动应用、对不支持方言假装解析成功，或把 placeholder 工件继续冒充真实结果，需人工确认
- Data impact: `optimization_task` 任务数据、parse/rewrite/acceleration artifact、失败阶段与风险说明、schema/migration 与 runtime smoke 证据
- Rollback / recovery: 保持 MySQL carrier 与 async 契约不变，关闭高风险 rewrite 规则或自动应用分支，回退新增 parser/rewriter/acceleration pipeline 与持久化字段说明，并恢复到 `D-TASK-030` 之后的已验证基线
- Validation:
  - `sql-optimization 模块测试、parse/rewrite/acceleration pipeline 测试、persistence/schema/mapping 校验、runtime smoke、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-031`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a JSQLParser-backed sql-optimization pipeline with real AST analysis, conservative rewrite rules, acceleration suggestion generation, structured suggestion/failure persistence, schema/mapping updates, focused tests, and authority-doc synchronization for D-TASK-031.
  - Validation evidence: mvn -B -pl sql-optimization -am test -DskipITs; python3 scripts/foreman.py validate D-TASK-031; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: Rewrite coverage remains intentionally conservative, external queue/callback and acceleration-plan apply governance are still pending, and fingerprint-only submissions can be accepted by contract but will terminate failed because the real parser pipeline requires sqlText.
  - Next step: Proceed to D-TASK-032 to close the governed acceleration-plan apply/verify/rollback loop on top of the new real sql-optimization suggestion baseline.

### D-TASK-030: 推进 provider-authenticated object-storage operations 与 governance-side batch retention/recovery orchestration

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): D-TASK-030 add batch artifact retention and recovery`
- Priority: 1
- Depends on: `D-TASK-029`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/environment-backed object storage 推进 vendor-neutral 的 provider-authenticated object-storage operations，并为 governance 补齐可审计的 artifact batch retention / batch recovery orchestration，把部分失败、回滚及 provider/recovery 证据持续沉淀进现有追溯面；不引入不受治理的 SDK 耦合或明文凭据落仓。 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`a...
- Plan ref: docs/exec-plans/completed/D-TASK-030-full-auto-execution-plan.md
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若 provider-authenticated operations 需要引入未经确认的 provider-specific SDK/签名机制、把 environment-backed object storage 误写成仓库默认主路径，或让 batch retention/recovery 绕过既有鉴权/审计边界、删除当前仍需保留的 artifact，需人工确认
- Data impact: provider-authenticated / environment-backed object-storage operation 请求与 live-evidence、artifact batch retention/recovery 执行摘要、失败分片、恢复来源与 governance 追溯留痕；不得落仓明文凭据，并持续保持 tenant 级隔离。
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider-auth/batch orchestration 默认启用；对部分失败批次保留审计与恢复留痕，回退新增 retention/recovery/provider-auth 语义与文档说明，并恢复到 `D-TASK-029` 已验证基线。
- Validation:
  - `sqlforge-shared/benchmark-engine/governance 模块测试、provider-authenticated object-storage contract 与 auth failure 测试、governance-side batch retention/recovery orchestration 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-030`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared artifact batch DTOs, provider-authenticated environment-backed cleanup scopes, governed benchmark artifact cleanup/recovery behavior, governance-side batch retention/recovery orchestration, focused tests, and authority doc updates for D-TASK-030.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-030; mvn -B -pl sqlforge-shared,benchmark-engine,governance -am -Dtest=BenchmarkArtifactGovernanceOperationServiceTest,BenchmarkArtifactStorageServiceTest,GovernanceHistoryApplicationServiceTest,AuthWebMvcTest -Dsurefire.failIfNoSpecifiedTests=false test; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: Provider-backed object storage remains vendor-neutral HTTP contract coverage plus environment-backed follow-up; real provider-native signing/retention semantics still require external environment validation.
  - Next step: Extend environment-backed validation against real provider endpoints when external credentials and retention controls are available.

### HARN-038: 修复 Governed Closeout Post-Closeout Healthcheck 与 Runtime Recovery 语义

- Status: done
- Completed at: 2026-04-25
- Commit subject: `fix(governance): repair closeout healthcheck recovery semantics`
- Priority: 1
- Depends on: `HARN-033`
- Scope: 在不改变 `HARN-031` / `HARN-032` / `HARN-033` 已落地治理真值、不引入第二套长期真值的前提下，修复 governed closeout 后置 healthcheck 的自引用误判，并把 commit-succeeded / post-check-failed 的 runtime repair 收口为可审计的标准路径；不得削弱 Main Foreman 唯一 write-back / validate / closeout 入口、不得静默删除失败证据、不得放宽 implementation-time dirty-worktree 阻断。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-038-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要把 runtime repair 扩展为自动抹除失败 evidence、放宽 implementation-time dirty-worktree healthcheck 阻断、或绕过 Main Foreman / task_audit 的既有收口链，需人工确认。
- Data impact: governed closeout / post-closeout runtime state、healthcheck/evidence 判定、执行计划与运行手册文档、以及验证日志与 closeout actual evidence 的治理语义；不修改业务运行时数据，不引入 repo 外第二真值。
- Rollback / recovery: 回退 closeout/healthcheck/runtime repair 语义修复：恢复此前的 governed closeout / post-closeout 判定与 runtime cleanup 行为，保留失败 evidence 与 validation-log 审计链，通过标准 validation 与 task-audit 证明仓库仍保持 Main Foreman 唯一收口和 implementation-time dirty-worktree 阻断边界。
- Validation:
  - `python3 scripts/foreman.py validate HARN-038、python3 -m py_compile scripts/foreman.py scripts/governed_healthcheck.py scripts/governed_v2_support.py、python3 scripts/governed_healthcheck.py --check、python3 scripts/foreman.py compile-governance --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/foreman.py validate HARN-038`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Normalized closeout post-check healthcheck context, added standard closeout-repair recovery flow, and aligned governance docs for post-closeout runtime recovery.
  - Validation evidence: python3 scripts/foreman.py validate HARN-038; python3 -m py_compile scripts/foreman.py scripts/governed_healthcheck.py scripts/governed_v2_support.py; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Closeout-repair now covers the standard commit-succeeded/post-check-failed path, but broader synthetic fault-injection coverage for arbitrary post-check failures is still limited to real command reuse.
  - Next step: Use foreman closeout post-checks for governed healthcheck, and run foreman closeout-repair if commit succeeds but a post-check later fails.

### HARN-037: 补齐只读 MCP onboarding / doctor 与治理定位手册

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): add read-only MCP doctor and onboarding`
- Priority: 1
- Depends on: `HARN-035`
- Scope: 在 `A-STORY-008` 下新增一个 follow-up 治理/工具任务，把现有只读 MCP 基线从“有规则”推进到“可落地可诊断可上手”：补齐按 category 的本地 onboarding、doctor/healthcheck 和 evidence 写回说明，统一单 agent 本地 MCP 与 multi-agent `mcp_profile` 的只读边界口径，并把禁止可写 MCP、SSH、K8s、数据库执行型 server、repo 落 secret/live inventory 的约束落实到文档、脚本与验证链；Main Foreman 仍是唯一 write-back / validate / closeout 入口。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-037-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-008` Codex MCP 治理接入
- Human confirmation point: 若要把 MCP doctor/healthcheck 扩展为远端自动运维、可写控制面、repo 落 secret/live inventory，或允许 multi-agent 中除 explorer/validator 外的角色消费 manifest-level `mcp_profile`，需人工确认。
- Data impact: MCP 治理文档、onboarding/doctor/healthcheck 脚本或校验分支、compile/validate/runtime 入口、只读 evidence 写回说明，以及相关 runtime 元数据；不直接修改业务运行时数据，不得把 secret、token、endpoint 或 live server inventory 写入 repo-tracked 文件。
- Rollback / recovery: 回退只读 MCP onboarding / doctor 改造：移除新增的 category onboarding、doctor/healthcheck、evidence 写回说明与定位文案，恢复 `HARN-034` / `HARN-035` 既有只读 MCP 基线，并通过标准 validation 与 task-audit 证明仓库仍保持 Main Foreman 唯一收口、只读 MCP 边界和无 secret/live inventory 落仓语义。
- Validation:
  - `python3 scripts/foreman.py validate HARN-037、python3 scripts/validate_codex_runtime.py、python3 scripts/foreman.py compile-governance --check、python3 scripts/governed_healthcheck.py --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout、python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/foreman.py validate HARN-037`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added local-only read-only MCP doctor checks, category onboarding guidance, evidence write-back targets, and consistent product positioning across MCP docs, lint, compiled policy, and runtime validation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-037; python3 scripts/mcp_doctor.py --check --json; python3 scripts/foreman.py compile-governance --check; python3 scripts/validate_codex_runtime.py; node scripts/lint-repository-knowledge.js; python3 scripts/task_audit.py --check --phase pre-closeout.
  - Residual risk: HARN-038 is still not formalized in repo truth, and governed_healthcheck remains intentionally strict about tracked dirty worktrees during in-flight task execution.
  - Next step: Do not execute HARN-038 until it is formalized into the master plan, task matrices, and ledger via the governed shaping/materialization path.

### HARN-036: 修复 governed intake / full-auto 入口治理与统一 execution preview 合同

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): harden governed intake execution routing`
- Priority: 1
- Depends on: `HARN-028`,`HARN-035`
- Scope: 在 `A-STORY-007` 下新增一个治理/工具 follow-up 任务，为 SQLForge 的 governed intake / full-auto 入口补齐 requirements artifact gate、统一 execution preview 合同、chat-native router 与 execution mode router，并把 `compile-governance` / `validate_codex_runtime` 提升为 full-auto 主路径硬门禁；不得让 router 在未显式确认前直接触发 `--confirm-run`，不得把 simple task 默认强制路由到 multi-agent，不得削弱 Main Foreman 唯一收口和现有只读 MCP 边界。 Tech: `DOCS`,`OPS`. Layer: `docs`,...
- Plan ref: docs/exec-plans/completed/HARN-036-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要让 chat-native router 在未显式确认前直接触发 `--confirm-run`、把 simple task 默认强制路由到 multi-agent、弱化 execution preview 固定字段合同，或削弱 Main Foreman 唯一 write-back / validate / closeout 边界，需人工确认。
- Data impact: governed intake / full-auto 入口脚本、template adapter / hook / router 运行态、execution preview 合同、run_id/confirmation 元数据、验证规则与相关文档索引；不直接修改业务运行时数据。
- Rollback / recovery: 回退 requirements artifact gate、execution preview/router 与 full-auto 硬门禁改造：恢复 `HARN-028` / `HARN-035` 之前的 intake/full-auto 行为，移除新增 preview/router 合同与强制校验分支，并通过标准 validation 与 task-audit 证明 Main Foreman 唯一收口、只读 MCP 边界和审计链未被削弱。
- Validation:
  - `python3 scripts/foreman.py validate HARN-036、bash scripts/governed_intake.sh --help、bash scripts/multi_agent_full_auto.sh --help、bash -n scripts/governed_intake.sh、bash -n scripts/multi_agent_full_auto.sh、python3 -m py_compile scripts/codex_template_adapter.py .codex/hooks/user_prompt_submit.py scripts/validate_codex_runtime.py、python3 scripts/validate_codex_runtime.py、python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/foreman.py validate HARN-036`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added chat-native governed intake routing, unified execution preview, existing-task requirements artifact gating, execution-mode routing, and full-auto governance hard gates; aligned runtime validation and playbooks.
  - Validation evidence: python3 scripts/foreman.py validate HARN-036; python3 scripts/validate_codex_runtime.py; bash -n scripts/governed_intake.sh; bash -n scripts/multi_agent_full_auto.sh; node scripts/lint-repository-knowledge.js.
  - Residual risk: HARN-037 read-only MCP onboarding/doctor changes still need to be restored and closed out separately; HARN-038 is not formalized in repo truth.
  - Next step: Restore HARN-037 changes, revalidate, and close out under its own task boundary.

### HARN-035: 扩展 multi-agent 受控 mcp_profile 只读证据接入

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): add multi-agent read-only MCP profiles`
- Priority: 1
- Depends on: `HARN-034`
- Scope: 在 `A-STORY-008` 下新增一个 follow-up 治理/工具任务，为 SQLForge multi-agent 基础设施增加受控 `mcp_profile` 只读证据接入能力，同时严格保持 Main Foreman 唯一收口、worker 禁改台账/closeout 文档和现有 worktree/ownership 审计边界不变。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-035-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-008` Codex MCP 治理接入
- Human confirmation point: 若要把 `mcp_profile` 从 explorer / validator 的只读证据面扩展到 worker、允许任何角色通过 MCP 执行可写操作，或削弱 Main Foreman 唯一 write-back / validate / closeout 边界，需人工确认。
- Data impact: multi-agent playbook、prompt 模板、manifest 契约、编排脚本与 `.codex/` 运行态元数据；不直接修改业务运行时数据。
- Rollback / recovery: 回退 `mcp_profile` 合同改造：移除 multi-agent 文档、模板和脚本中的 MCP profile 字段与处理分支，恢复无 MCP profile 的现有 multi-agent 基线，并通过标准 validation 与 task-audit 证明收口链未被削弱。
- Validation:
  - `python3 scripts/foreman.py validate HARN-035 --include-task-audit`
  - `python3 scripts/foreman.py compile-governance --check`
  - `bash scripts/multi_agent_prepare.sh --help`
  - `bash scripts/multi_agent_launch.sh --help`
  - `bash scripts/multi_agent_collect.sh --help`
  - `bash scripts/multi_agent_autoplan.sh --help`
  - `bash scripts/multi_agent_full_auto.sh --help`
  - `python3 scripts/validate_codex_runtime.py`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: aligned MCP governance docs, rules, validation rules and playbooks with the `mcp_profiles` / `mcp_profile` multi-agent read-only evidence contract.
  - 2026-04-25: compiled governance policy, passed multi-agent script syntax/help checks, passed `validate_codex_runtime.py`, and passed `python3 scripts/foreman.py validate HARN-035 --include-task-audit`.
- Context closeout:
  - Completed scope: Extended SQLForge MCP governance from the HARN-034 baseline to a governed multi-agent mcp_profiles/mcp_profile contract, updated docs, rules, playbooks, prompt templates, manifest template, orchestration scripts, compiled policy artifacts, and runtime validation so explorer/validator can consume read-only external evidence without weakening Main Foreman authority.
  - Validation evidence: python3 scripts/foreman.py compile-governance; bash -n scripts/multi_agent_prepare.sh scripts/multi_agent_launch.sh scripts/multi_agent_collect.sh scripts/multi_agent_autoplan.sh scripts/multi_agent_full_auto.sh scripts/task_materialize.sh; python3 -m py_compile scripts/foreman.py scripts/validate_codex_runtime.py; bash scripts/multi_agent_prepare.sh --help; bash scripts/multi_agent_launch.sh --help; bash scripts/multi_agent_collect.sh --help; bash scripts/multi_agent_autoplan.sh --help; bash scripts/multi_agent_full_auto.sh --help; node scripts/lint-repository-knowledge.js; python3 scripts/validate_codex_runtime.py; python3 scripts/foreman.py validate HARN-035 --include-task-audit
  - Residual risk: The repository still does not track live MCP servers, credentials, or writable MCP flows. worker/Main Foreman/Auto Foreman MCP execution remains intentionally disabled, and real codex exec availability can still depend on local authentication or environment readiness.
  - Next step: Use the readonly-evidence manifest profile only for explorer/validator and keep real server resolution in local user config, env vars, or an external secret store; any writable or worker-facing MCP expansion requires a new formal task.

### HARN-034: 落地最小可用 MCP 治理底座与只读接入边界

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): add MCP read-only governance baseline`
- Priority: 1
- Depends on: `HARN-028`,`HARN-033`
- Scope: 在 `A-STORY-008` 下新增一个治理/工具型正式任务，把 SQLForge 的最小 MCP 治理底座和第一批只读 MCP 边界收口到正式文档、规则、验证规则、治理编译和运行时校验入口中，同时保持 Main Foreman 唯一 write-back / validate / closeout 入口不变。 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-034-full-auto-execution-plan.md
- Matrix context: Phase-A / Story `A-STORY-008` Codex MCP 治理接入
- Human confirmation point: 若要把本任务从只读 MCP 扩展为可写 MCP、把真实 connector 凭据或 server 配置落仓、或允许 MCP 绕过 `foreman` / `task_audit` / `closeout` 成为并行治理入口，需人工确认。
- Data impact: 文档真值、规则账本、验证规则、治理编译产物、运行时校验脚本与 Codex 本地使用手册；不直接修改业务运行时数据。
- Rollback / recovery: 按追加式治理回退 MCP 基线：移除本任务新增的 MCP 文档入口、规则、验证规则和自动化校验分支，恢复 `compile-governance` / `validate_codex_runtime` 的既有行为，并通过标准 validation 与 task-audit 证明仓库回到改造前治理基线。
- Validation:
  - `python3 scripts/foreman.py validate HARN-034、python3 scripts/validate_codex_runtime.py、python3 scripts/foreman.py compile-governance --check、node scripts/lint-repository-knowledge.js、python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/foreman.py validate HARN-034`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added SQLForge MCP governance baseline docs, connector/security boundary registry, MCP rule and validation-rule appendices, compiled mcp-policy.json, MCP-aware runtime validation, and local Codex MCP usage guidance while keeping Main Foreman as the only write-back/validate/closeout entry.
  - Validation evidence: python3 scripts/foreman.py compile-governance; node scripts/lint-repository-knowledge.js; python3 scripts/validate_codex_runtime.py; python3 scripts/foreman.py validate HARN-034 --include-task-audit
  - Residual risk: The repository now governs only the read-only MCP baseline. Live server inventory, repo-tracked runtime config, and multi-agent mcp_profile remain intentionally disabled until HARN-035 or another formal follow-up lands.
  - Next step: Materialize and implement HARN-035 so explorer/validator can consume governed external evidence through manifest-level mcp_profile without changing Main Foreman authority.

### HARN-033: Codex template and runtime evidence production hardening

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): harden codex template runtime production path`
- Priority: 1
- Depends on: HARN-032
- Scope: Productionize the HARN-032 Codex natural template and governed full-cycle runtime lifecycle by adding legacy candidate-pack compatibility, closeout actual-evidence health checks, stronger template parsing/schema/smoke coverage, cleanup preview semantics, and a runtime dashboard/cleanup command.
- Validation:
  - `python3 scripts/foreman.py validate HARN-033`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: implemented legacy candidate-pack compatibility, closeout actual evidence health reporting, template adapter schema/parser hardening, cleanup preview outcome semantics, and governed runtime dashboard/cleanup preview support.
- Context closeout:
  - Completed scope: Productionized the HARN-032 Codex template and governed runtime path by adding legacy candidate-pack authority field compatibility, closeout actual-evidence health reporting, schema-versioned natural template parsing for business/governance/existing-task inputs, cleanup preview outcome semantics, and a governed runtime dashboard/cleanup command.
  - Validation evidence: python3 scripts/foreman.py validate HARN-033 --include-task-audit with focused py_compile, shell syntax, four template adapter smoke cases, and runtime dashboard smoke; python3 -m py_compile scripts/codex_template_adapter.py scripts/governed_healthcheck.py scripts/governed_v2_support.py scripts/governed_runtime_dashboard.py; bash -n scripts/task_materialize.sh scripts/requirements_to_plan.sh scripts/governed_intake.sh scripts/governed_full_cycle.sh; bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn028-smoke/candidate-task-pack.json --dry-run; python3 scripts/governed_healthcheck.py --check --cleanup-dry-run --run-id harn033-cleanup-preview; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js
  - Residual risk: Runtime .codex/state evidence remains untracked by design, and cleanup/archive actions remain explicit human-reviewed operations. Live Codex execution availability is still environment-dependent and validate_codex_runtime may report skipped-timeout rather than proving live multi-agent availability.
  - Next step: Use codex_template_adapter.py schema_version=2 and governed_runtime_dashboard.py for future natural-template entry and runtime evidence review; consider hook-level template detection only if humans want Codex prompts to invoke the adapter automatically.

### HARN-032: Governed full-cycle V4 entrypoint and evidence hardening

- Status: done
- Completed at: 2026-04-25
- Commit subject: `feat(governance): harden governed full-cycle v4 entrypoints`
- Priority: 1
- Depends on: HARN-031
- Scope: Harden HARN-031 follow-up gaps by enforcing healthcheck gates before real governed execution, writing post-closeout actual runtime evidence, adding Codex natural template adaptation, extending runtime cleanup/retention, and covering the template path with smoke validation.
- Validation:
  - `python3 scripts/foreman.py validate HARN-032`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: implemented mandatory pre-execution health gates, post-closeout actual runtime evidence, Codex natural template adapter, cleanup preview/dashboard fields, and explicit authority_fields_to_confirm plumbing.
- Context closeout:
  - Completed scope: Hardened governed full-cycle V4 entrypoints by enforcing pre-execution healthcheck gates before real governed_intake/governed_full_cycle execution, adding post-closeout actual runtime evidence, introducing a Codex natural template adapter, extending cleanup preview/dashboard runtime summaries, and plumbing explicit authority_fields_to_confirm through candidate packs and suggestions.
  - Validation evidence: python3 scripts/foreman.py validate HARN-032 --include-task-audit --extra-command <py_compile/governed shell syntax/template adapter smoke/healthcheck help>; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; python3 scripts/codex_template_adapter.py --run-id harn032-template-smoke --template-text <治理需求 smoke>; python3 scripts/governed_healthcheck.py --check --cleanup-dry-run --run-id harn032-cleanup-preview
  - Residual risk: Real governed execution is now gated by healthcheck, so active implementation dirty state intentionally blocks confirm/full-cycle runs until committed. Runtime .codex evidence remains untracked by design; cleanup-dry-run previews intake/task-shaping evidence but only releases safe stale reservations automatically.
  - Next step: Use codex_template_adapter.py for natural template entry and inspect .codex/state/closeout/<TASK_ID>/post-closeout-actual.json after closeout; consider a later task for fully automated deletion of reviewed runtime evidence if desired.

### HARN-031: HARN-028 governed full-cycle V3 audit-hardening repair

- Status: done
- Completed at: 2026-04-25
- Commit subject: `fix(governance): HARN-028 V3 audit hardening`
- Priority: 1
- Depends on: HARN-028
- Scope: Repair HARN-028 governed full-cycle V2 audit semantics, archived Plan refs, healthcheck dirty detection, dry-run state semantics, runtime state cleanup, and Codex newcomer task-entry docs.
- Validation:
  - `python3 scripts/foreman.py validate HARN-031`
- Progress log:
  - 2026-04-25: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-25: implemented V3 governance repairs for closeout projected evidence semantics, archived Plan ref rewriting/audit, healthcheck tracked-dirty blocking, intake dry-run state naming, reservation release/cleanup lifecycle, and Codex newcomer templates.
- Context closeout:
  - Completed scope: Repaired HARN-028 governed full-cycle V2 hardening gaps by separating projected closeout evidence from actual pass semantics, rewriting archived Plan refs to completed paths, blocking tracked dirty worktrees in healthcheck, correcting intake dry-run confirmation state, adding reservation release/cleanup lifecycle support, and documenting Codex newcomer entry templates.
  - Validation evidence: python3 -m py_compile scripts/foreman.py scripts/task_audit.py scripts/governed_healthcheck.py scripts/governed_v2_support.py; bash -n scripts/governed_intake.sh scripts/requirements_to_plan.sh scripts/task_materialize.sh; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance --check; node scripts/lint-repository-knowledge.js; bash scripts/requirements_to_plan.sh --run-id harn031-requirements-dry-run --task-prefix HARN --prompt <smoke> --dry-run; bash scripts/governed_intake.sh --confirm-run harn028-intake-no-task --dry-run; python3 scripts/governed_healthcheck.py --check --run-id harn031-dirty-check; python3 scripts/foreman.py validate HARN-031 --include-task-audit --extra-command <focused script checks>
  - Residual risk: Healthcheck now intentionally blocks tracked dirty implementation states, so implementation-time healthcheck runs are expected to report tracked_dirty_worktree until closeout commits the tracked patch. Existing untracked .codex runtime evidence remains non-authoritative runtime state and should be cleaned with governed cleanup when stale.
  - Next step: Use the new Codex daily input templates and HARN-031 audit gates on the next governed full-cycle request; monitor whether authority_fields_to_confirm needs a later structured schema upgrade.

### HARN-028: 加固 governed full-cycle V2 intake / healthcheck / closeout 完整性

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): HARN-028 harden governed full-cycle v2`
- Priority: 1
- Depends on: `HARN-027`
- Scope: 在不改变 `HARN-027` no-task 起步真值、不引入第二套长期真值的前提下，补齐 governed intake/healthcheck 入口、machine-readable run summary 的 `executed_commands` 与细粒度 suggestion 字段、candidate materialization rollback，以及 closeout 后不得留下 tracked `validation-log` residue 的仓库级修复 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-028-governed-full-cycle-v2-hardening-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要把 governed intake/healthcheck 升级为跳过确认直接改写台账真值、让 healthcheck 自动回滚或重写 closeout 记录，或允许 closeout 修复继续回到提交后追加 tracked `validation-log` 的模式，需人工确认
- Data impact: governed intake/healthcheck 入口、run summary/建议字段、candidate materialization rollback、reservation 状态，以及 closeout 与 validation-log 的仓库级治理语义；不直接改变业务运行时数据
- Rollback / recovery: 停用 intake/healthcheck 入口并回退到 `HARN-027` 的 requirements-to-plan / task-materialize 手工组合路径，保留 run summary / healthcheck 证据与回滚记录，必要时拆出更细粒度的 runtime hardening follow-up
- Validation:
  - `python3 scripts/foreman.py validate HARN-028`、`bash scripts/governed_intake.sh --help`、`python3 scripts/governed_healthcheck.py --check`、`bash scripts/task_materialize.sh --help`、`bash -n scripts/governed_intake.sh`、`bash -n scripts/task_materialize.sh`、`python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py`、docs 索引/手册对齐
  - `python3 scripts/foreman.py validate HARN-028`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed_full_cycle V2 hardening on top of HARN-027: governed intake and healthcheck entrypoints, machine-readable summaries with executed_commands and granular suggestions, task materialization rollback/reservation handling, closeout validation-log residue repair, and aligned docs/plan coverage updates without changing business functionality.
  - Validation evidence: bash -n scripts/requirements_to_plan.sh; bash -n scripts/task_materialize.sh; bash -n scripts/governed_intake.sh; python3 -m py_compile scripts/governed_v2_support.py scripts/governed_healthcheck.py scripts/foreman.py; bash scripts/governed_intake.sh --help; python3 scripts/governed_healthcheck.py --check; bash scripts/requirements_to_plan.sh --run-id harn028-smoke --task-prefix HARN --prompt <smoke>; bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn028-smoke/candidate-task-pack.json --dry-run; bash scripts/governed_intake.sh --run-id harn028-intake-smoke --task HARN-028; bash scripts/governed_intake.sh --run-id harn028-intake-no-task --prompt <smoke>; bash scripts/governed_intake.sh --confirm-run harn028-intake-no-task --dry-run; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py validate HARN-028; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Governed no-task intake still depends on Codex shaping latency and prompt quality; candidate packs can legitimately stop at human-confirmation boundaries, and future hardening may still be needed if summary/intake contracts expand beyond the current CLI + task-materialization surfaces.
  - Next step: Use governed_intake.sh for future short-input governance or business-task shaping, and if repeated runs show the same human-confirmation ambiguity, split narrower domain-specific shaper prompts instead of weakening the confirmation gate.

### HARN-027: 落地从无 task 开始的 governed full-cycle 自动化

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): HARN-027 add no-task governed full-cycle`
- Priority: 1
- Depends on: `HARN-026`
- Scope: 在不改变 `HARN-026` downstream full-auto 真值、不引入第二套长期真值的前提下，新增 requirement normalization、candidate task pack、governance gate 与 materialization 脚本/模板/手册，让 Codex 可以从“只有需求”开始先塑形 formal task，再继续交给现有 full-auto 执行链 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-027-requirements-to-task-governed-full-cycle-plan.md
- Matrix context: Phase-A / Story `A-STORY-007` 从无 task 开始的治理自动化
- Human confirmation point: 若要允许 candidate task 在未同步 master-execution-plan/task-spec/task-governance 矩阵前直接进入编码、自动越过 INBOX/人工确认点，或把“无 task 起步”的自动化扩展为可绕过 `preflight` / `instantiate` / `validate` / `task_audit` / `closeout` 的黑盒执行，需人工确认
- Data impact: requirement-normalizer/plan-shaper/task-shaper/task-governance-reviewer prompt 模板、candidate task pack 模板、task-shaping 运行态、requirements-to-task/materialization/full-cycle 脚本，以及由 formal materialization 写入的 plan/matrix/ledger/exec-plan/raw-requirement 记录；不直接改变业务运行时数据
- Rollback / recovery: 停用 governed full-cycle 脚本并回退到“人工写 plan + 人工建 task + `HARN-026` downstream full-auto”路径，保留 candidate task pack 和 review 证据作为治理记录，必要时拆出更细粒度的 task-shaping follow-up
- Validation:
  - `python3 scripts/foreman.py validate HARN-027`、`bash scripts/requirements_to_plan.sh --help`、`bash scripts/task_materialize.sh --help`、`bash scripts/governed_full_cycle.sh --help`、requirements-to-plan dry-run、real candidate pack generation、task_materialize dry-run、governed_full_cycle dry-run、docs 索引与 coverage 矩阵对齐
  - `python3 scripts/foreman.py validate HARN-027`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governed no-task full-cycle automation on top of the existing full-auto multi-agent stack: requirement normalization, candidate execution plan shaping, candidate task pack shaping, governance-gated materialization, and end-to-end orchestrator documentation/scripts. Also hardened nested Codex execution so shaping no longer pollutes current-task state and downstream child sessions run correctly in the externally sandboxed automation environment.
  - Validation evidence: bash -n scripts/requirements_to_plan.sh; bash -n scripts/task_materialize.sh; bash -n scripts/governed_full_cycle.sh; bash -n scripts/multi_agent_autoplan.sh; bash -n scripts/multi_agent_launch.sh; bash -n scripts/multi_agent_full_auto.sh; bash scripts/requirements_to_plan.sh --help; bash scripts/task_materialize.sh --help; bash scripts/governed_full_cycle.sh --help; bash scripts/requirements_to_plan.sh --run-id harn027-smoke4 --task-prefix HARN --prompt <smoke> ; bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/harn027-smoke4/candidate-task-pack.json --dry-run; bash scripts/governed_full_cycle.sh --run-id harn027-smoke4 --task-prefix HARN --prompt <smoke> --dry-run; bash scripts/multi_agent_autoplan.sh --task HARN-027 --prompt <dry-run> --dry-run; bash scripts/multi_agent_launch.sh --manifest .codex/state/multi-agent/HARN-026/validation/generated-manifest.json --dry-run; python3 scripts/foreman.py compile-governance; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py validate HARN-027; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Governed full-cycle still depends on Codex CLI availability and prompt quality; ambiguous raw requirements will continue to stop at the human-confirmation gate instead of auto-materializing, and downstream task execution still inherits the operational limits of the existing HARN-026 full-auto stack.
  - Next step: Use docs/operations/requirements-to-task-playbook.md and scripts/governed_full_cycle.sh on the next governance/tooling request that starts without a formal task, then evaluate whether the shaping prompts need narrower domain-specific templates for repeated requirement classes.

### HARN-026: 把多 agent 基础设施升级为从需求到收口的全自动主路径

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): HARN-026 add full-auto multi-agent orchestration`
- Priority: 1
- Depends on: `HARN-025`
- Scope: 在不改变 `HARN-025` 半自动真值、不引入第二套长期真值的前提下，新增 requirement-driven auto-planner / auto-foreman prompt 模板与 autoplan/full-auto orchestration 脚本，让 codex 可以从需求输入自动生成 exec plan、manifest，并驱动 prepare/launch/collect 与最终 autonomous Main Foreman 收口 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-026-full-auto-multi-agent-upgrade-plan.md
- Matrix context: Phase-A / Story `A-STORY-006` 全自动多 agent 协作编排
- Human confirmation point: 若要把全自动路径升级为“无任务治理前置、无 Main Foreman 收口、可绕过 `foreman validate/task_audit/closeout` 的黑盒自动执行”，或允许 auto-planner / auto-foreman 直接改写台账真值而不经过仓库审计链，需人工确认
- Data impact: auto-planner / auto-foreman prompt 模板、requirement-driven exec plan 与 manifest 生成脚本、`.codex/state` 下的 full-auto 运行态产物，以及由 autonomous Main Foreman 落地到 `docs/` / 台账的最终收口记录；不直接改变业务运行时数据
- Rollback / recovery: 停用 full-auto 脚本并回退到 `HARN-025` 半自动模式，保留需求输入、生成的 plan/manifest 和失败日志作为治理证据，必要时拆出更细粒度的 auto-planning/closeout follow-up
- Validation:
  - `python3 scripts/foreman.py validate HARN-026`、`bash scripts/multi_agent_autoplan.sh --help`、`bash scripts/multi_agent_full_auto.sh --help`、autoplan dry-run、自生成 manifest 的 prepare/launch dry-run、docs 索引与 coverage 矩阵对齐
  - `python3 scripts/foreman.py validate HARN-026`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added full-auto multi-agent governance capability on top of HARN-025, including auto-planner and auto-foreman prompt templates, full-auto orchestration scripts, manifest contract updates, and playbook/index/coverage synchronization without changing business functionality.
  - Validation evidence: python3 scripts/foreman.py validate HARN-026; bash scripts/multi_agent_autoplan.sh --help; bash scripts/multi_agent_full_auto.sh --help; real autoplan generation to .codex/state/multi-agent/HARN-026/validation/generated-plan.md and generated-manifest.json; bash scripts/multi_agent_prepare.sh --task HARN-026 --manifest .codex/state/multi-agent/HARN-026/validation/generated-manifest.json --dry-run; bash scripts/multi_agent_launch.sh --manifest .codex/state/multi-agent/HARN-026/validation/generated-manifest.json --dry-run; node scripts/lint-repository-knowledge.js
  - Residual risk: The autonomous Main Foreman path has dry-run and integration proof but still depends on live codex exec stability and longer end-to-end smoke should remain a follow-up if orchestration semantics change again.
  - Next step: Use HARN-026 as the downstream execution substrate for HARN-027 so requirement-to-task automation can hand off only after a formal task has been materialized and instantiated.

### HARN-025: 落地半自动多 agent 协作基础设施（C方案）

- Status: done
- Completed at: 2026-04-24
- Priority: 1
- Depends on: `HARN-024`
- Scope: 在不改变业务主线事实、不引入第二套长期真值的前提下，新增 multi-agent playbook、agent prompt 模板、manifest 模板，以及基于多 `codex exec` / 多 `git worktree` 的 prepare/launch/collect 半自动编排脚本，保持 Main Foreman 唯一 validate/closeout/commit Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Plan ref: docs/exec-plans/completed/HARN-025-semi-auto-multi-agent-foundation-plan.md
- Matrix context: Phase-A / Story `A-STORY-005` 半自动多 agent 协作治理基础设施
- Human confirmation point: 若要把半自动多 agent 提升为默认自动执行路径、弱化 Main Foreman 唯一收口、允许 worker 修改台账/validation-log/closeout 文档，或用隐式 subagent 取代显式 `codex exec` + worktree 编排，需人工确认
- Data impact: 文档真值、运行期 prompt 模板、manifest 编排、worktree orchestration 脚本，以及 `.codex/` 下的运行态 multi-agent 会话元数据；不影响业务运行时数据
- Rollback / recovery: 停用 multi-agent 脚本与运行态目录，回退新增 docs/模板/脚本到单 agent `foreman` 路径，并保留 prompt/manifest 作为历史治理记录或拆出兼容改造任务
- Commit subject: `feat(governance): HARN-025 add semi-auto multi-agent foundation`
- Validation:
  - `python3 scripts/foreman.py validate HARN-025`、`bash scripts/multi_agent_prepare.sh --help`、`bash scripts/multi_agent_launch.sh --help`、`bash scripts/multi_agent_collect.sh --help`、docs 索引与 coverage 矩阵对齐
  - `python3 scripts/foreman.py validate HARN-025`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: added `HARN-025` to the master execution plan, task-spec matrix, task-governance extension matrix, docs coverage matrix, and an active exec plan so the semi-auto multi-agent foundation is a formal governance/tooling task instead of an ad-hoc script change.
  - 2026-04-24: added `docs/operations/multi-agent-playbook.md`, the `docs/agent-prompts/*.md` role templates, and `docs/exec-plans/templates/multi-agent-run.template.json`; also updated `docs/README.md` and `docs/operations/README.md` so the new workflow is indexed and documented as first-class repository truth.
  - 2026-04-24: implemented `scripts/multi_agent_prepare.sh`, `scripts/multi_agent_launch.sh`, and `scripts/multi_agent_collect.sh`, keeping runtime metadata under `.codex/state/multi-agent/`, then verified help output, prepare/launch dry-run, collect smoke, `python3 scripts/foreman.py compile-governance`, and `python3 scripts/foreman.py validate HARN-025`.
  - 2026-04-24: the first validation run exposed a repository-knowledge lint failure because a placeholder active-manifest path was rendered as a nonexistent docs path; updated the playbook and coverage wording to describe the naming convention without introducing a dead docs link, then reran validation successfully.
- Context closeout:
  - Completed scope: Instantiated HARN-025 as a formal governance/tooling task, added the semi-auto multi-agent playbook, role prompt templates, manifest template, and active exec plan, implemented prepare/launch/collect orchestration scripts with runtime state under .codex/state/multi-agent, updated docs indexes and coverage, and kept Main Foreman as the only validate/closeout/commit entrypoint without mixing business functionality.
  - Validation evidence: python3 scripts/foreman.py validate HARN-025; bash scripts/multi_agent_prepare.sh --task HARN-025 --manifest .codex/state/multi-agent/HARN-025-dry-run.json --bootstrap-if-missing --dry-run; bash scripts/multi_agent_launch.sh --manifest .codex/state/multi-agent/HARN-025-dry-run.json --dry-run; bash scripts/multi_agent_collect.sh --manifest .codex/state/multi-agent/HARN-025-collect-smoke.json; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The new capability is intentionally semi-auto: Main Foreman still has to write task-specific manifests, review fan-in, and choose the final validation chain manually; successful real-world use also still depends on clean worktrees, clear ownership boundaries, and local Codex CLI/auth availability. The validation log also contained a pre-existing append-only HARN-024 closeout tail before this task started, and the first failed HARN-025 lint attempt is intentionally preserved as audit evidence.
  - Next step: For the next complex cross-module task, copy the manifest template into docs/exec-plans/active/, fill task-specific ownership/worktree rules, and run prepare/launch/collect before deciding whether more automation is justified.

### HARN-024: 收口 D-TASK-029 closeout 后的 active-wave / validation-log 漂移

- Status: done
- Completed at: 2026-04-24
- Commit subject: `chore(governance): realign active wave after D-TASK-029`
- Priority: 1
- Depends on: D-TASK-029
- Scope: 只修正 D-TASK-029 closeout 后遗留的计划真值与 append-only validation-log 漂移，恢复当前没有已实例化 repo-side mainline task 的仓库事实；不改写 D-TASK-029 的历史完成结论，也不实例化新的 mainline 业务任务。
- Validation:
  - `python3 scripts/foreman.py validate HARN-024`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Repaired the post-closeout active-wave drift left after D-TASK-029, restored the master plan truth to no instantiated repo-side mainline task, regenerated governance policy authority artifacts, and absorbed the append-only validation-log residue into a ledger-bound governance closeout.
  - Validation evidence: python3 scripts/foreman.py validate HARN-024; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/foreman.py compile-governance
  - Residual risk: The repository is back to no instantiated repo-side mainline task, but the next Phase-D business follow-up still needs explicit shaping before implementation; no new mainline task is instantiated by this governance repair.
  - Next step: If work continues immediately, shape a new Phase-D follow-up around provider-authenticated object-storage operations and governance-side batch retention/recovery orchestration before instantiation.

### D-TASK-029: 推进 provider-native / environment-backed object-storage live evidence 与 governance-triggered artifact cleanup/recovery operation surfaces

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): add benchmark artifact operation surfaces`
- Priority: 1
- Depends on: `D-TASK-028`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/environment-backed object storage 推进更接近 provider-native 的 live evidence 沉淀，并为 governance 补齐可审计的 artifact cleanup/recovery operation surface 与受控触发链路 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若 provider-native live evidence 会引入未经确认的 SDK/凭据写入、把 environment-backed object storage 误写成仓库默认主路径，或让 governance-triggered cleanup/recovery 绕过既有鉴权/审计边界、删除当前仍需保留的 artifact，需人工确认
- Data impact: provider-native / environment-backed live-evidence 配置与 manifest、artifact cleanup/recovery operation 请求/审计/追溯留痕，以及 governance 历史操作面返回的 operation surface
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider-native live evidence 与 governance-triggered operation 默认启用，回退 cleanup/recovery operation surface 与 live-evidence 语义说明，并恢复到 `D-TASK-028` 已验证基线
- Validation:
  - `sqlforge-shared/benchmark-engine/governance 模块测试、provider-native live-evidence 契约测试、governance-triggered cleanup/recovery operation 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-029`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added shared benchmark artifact operation contracts, benchmark-engine internal cleanup/recovery operations, provider-native object-storage live evidence enrichment, governance-triggered artifact operation route, governance history artifactOperationSurface aggregation, tests, and architecture truth sync while keeping LOCAL_FILE as the default repo-side path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-029; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Provider-native evidence still uses HTTP-based header capture and repo-controlled environment configuration; true cloud-signed/provider-SDK live operations remain a future environment-backed follow-up and are not the default repo path.
  - Next step: Shape the next Phase-D task around stronger provider-authenticated object-storage operations and broader governance-side batch retention/recovery orchestration without changing the LOCAL_FILE default.

### D-TASK-028: 收口 provider-specific / multi-provider object-storage contract 与 cleanup/recovery 语义，并提升 compensation-replay evidence 的治理查询/恢复面

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add multi-provider artifact recovery surfaces`
- Priority: 1
- Depends on: `D-TASK-027`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine 收口 provider-specific / multi-provider object-storage contract、cleanup/recovery / failure-replay 语义，并把 compensation-replay 与 artifact recovery/provider evidence 提升为 governance 历史查询与恢复操作面的显式结构字段 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure...
- Matrix context: Phase-D
- Human confirmation point: 若 provider-specific / multi-provider contract 会把 provider 差异误写成统一默认能力、让 cleanup/recovery 误删仍需保留的 artifact、绕过既有只读/鉴权/审计边界，或把 provider-backed object storage 误写成仓库默认主路径，需人工确认
- Data impact: provider-specific / multi-provider artifact contract 配置、cleanup/recovery/failure-replay 证据、benchmark/query-execution compensation-replay 结构载荷，以及 governance 历史查询/恢复面的 provider 与 recovery 留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭新增 provider/multi-provider 默认启用，回退 cleanup/recovery/provider 语义与治理查询字段说明，并恢复到 `D-TASK-027` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、provider/multi-provider artifact contract 测试、governance query/detail 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-028`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented provider-specific and multi-provider environment-backed artifact storage contracts, cleanup/recovery semantics, recovery-source/read-status propagation, and governance trace summary/detail surfaces for compensation replay and artifact storage/recovery evidence; synchronized repository truth docs and kept LOCAL_FILE as the default repo-side path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-028; python3 scripts/task_audit.py --check --phase pre-closeout; mvn -B -pl benchmark-engine,governance -am clean test -DskipITs
  - Residual risk: The environment-backed path now covers generic HTTP primary/recovery provider verification and governance recovery surfaces, but provider-native SDK semantics, broader external cleanup orchestration, and longer-lived environment evidence retention remain follow-up work. Current plan truth therefore returns to no instantiated repo-side mainline task after this closeout.
  - Next step: Keep no instantiated repo-side mainline task. If work continues immediately, shape D-TASK-029 around provider-native/environment-backed object-storage live evidence plus governance-triggered artifact cleanup/recovery operation surfaces before implementation.

### D-TASK-027: 收口更深层 workload compensation-replay orchestration 与 provider-backed object-storage live evidence

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add compensation replay storage evidence`
- Priority: 1
- Depends on: `D-TASK-026`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，为 benchmark-engine/query-execution 补齐更深层的 workload compensation-replay orchestration，并把 environment-backed object-storage 从 writable-dir verification 推进到 provider-backed live evidence/readback recovery verification Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastruct...
- Matrix context: Phase-D
- Human confirmation point: 若 compensation-replay orchestration 会绕过 `query-execution` 既有只读/鉴权/审计边界、把 compensated replay 冒充成原始 live capture，或把 provider-backed object-storage live evidence 误写成仓库默认主路径，需人工确认
- Data impact: benchmark/query-execution compensation-replay 证据、governance 长期追溯载荷、provider-backed object storage write/readback/recovery 留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭 provider-backed live evidence 默认启用，回退新增 compensation/provider 语义与文档说明，并恢复到 `D-TASK-026` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine/governance 模块测试、跨服务 compensation-replay 契约测试、provider-backed object-storage adapter/live-evidence 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-027`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: completed ledger-bound implementation for query-execution compensation-replay orchestration, benchmark execution-summary/governance trace enrichment, and provider-backed object-storage live evidence/readback recovery verification while keeping `LOCAL_FILE` as the default repo-side path.
  - 2026-04-24: verified repo-side behavior with `mvn -B -pl sqlforge-shared,query-execution,benchmark-engine,governance -am test -DskipITs`; governance persistence coverage now asserts compensation evidence and provider verification summaries.
- Context closeout:
  - Completed scope: Implemented benchmark/query-execution compensation-replay orchestration, persisted compensation/provider verification evidence through benchmark/governance trace payloads, added provider-backed object-storage write/readback recovery verification on the environment-backed path, expanded governance persistence coverage, and synchronized authority docs while keeping LOCAL_FILE as the default repo-side path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-027 --include-task-audit --extra-command 'mvn -B -pl sqlforge-shared,query-execution,benchmark-engine,governance -am test -DskipITs' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'
  - Residual risk: Provider-backed live evidence currently validates against the configured endpoint via generic HTTP write/readback semantics; provider-specific SDK behavior, multi-provider retention/recovery contracts, and deeper compensation-replay governance query/recovery tooling remain follow-up work. Current plan truth therefore returns to no instantiated repo-side mainline task after this closeout.
  - Next step: Keep no instantiated repo-side mainline task. If work continues immediately, shape D-TASK-028 around provider-specific/multi-provider object-storage contract and cleanup/recovery semantics plus deeper governance query/recovery surfaces for compensation-replay evidence.

### D-TASK-026: 把 workload/backfill evidence 沉淀进 governance 长期追溯链，并推进真实 external write/recovery verification

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(governance): persist benchmark workload storage evidence`
- Priority: 1
- Depends on: `D-TASK-025`
- Scope: 在保持 repo-local artifact lifecycle 与 `LOCAL_FILE` 默认主路径不变、统一授权入口、治理审计及只读/影子环境边界不变的前提下，把 benchmark/query-execution 的 workload/backfill 证据提升为 governance 长期追溯链中的显式结构化字段，并把 environment-backed object-storage 从 repo-side live-evidence manifest 推进到真实 external write/readback recovery verification Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deplo...
- Matrix context: Phase-D
- Human confirmation point: 若 workload/backfill evidence 的治理沉淀会弱化既有只读/鉴权/审计边界、把 synthetic backfill 冒充成真实 live capture，或把 environment-backed external write/recovery verification 误写成仓库默认主路径，需人工确认
- Data impact: governance `config_snapshot/execution_result/query_history/export_record` 追溯载荷、benchmark/query-execution workload/backfill 证据、environment-backed object storage external write/readback evidence 与恢复留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 `LOCAL_FILE` 默认主路径，关闭 external write/recovery verification 默认启用，回退新增治理字段/adapter 语义与文档说明，并恢复到 `D-TASK-025` 已验证基线
- Validation:
  - `sqlforge-shared/governance/query-execution/benchmark-engine 模块测试、跨服务 workload/backfill 与 trace persistence 契约测试、artifact adapter verification 测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-026`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Persisted benchmark workload/backfill evidence into governance trace payloads, extended environment-backed artifact storage to real external write/readback recovery verification, added contract/tests, and synchronized authority docs while keeping LOCAL_FILE as the default path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-026 --include-task-audit --extra-command 'mvn -B -pl sqlforge-shared,governance,benchmark-engine -am test -DskipITs' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'
  - Residual risk: Governance trace now preserves repo-side workload/backfill and writable-dir-based external storage verification evidence, but deeper cross-service workload compensation/replay orchestration and provider-backed object storage live evidence beyond the current external write dir baseline remain follow-up work.
  - Next step: Shape the next repo-side follow-up around deeper benchmark/query-execution workload compensation-replay orchestration plus provider-backed object-storage live evidence beyond the current writable-dir verification baseline.

### D-TASK-025: 收口 `benchmark-engine` / `query-execution` workload/backfill orchestration 与真实环境 object storage live evidence

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): orchestrate workload backfill evidence`
- Priority: 1
- Depends on: `D-TASK-024`
- Scope: 在保持 repo-local artifact lifecycle 仍是默认主路径、统一授权入口、治理审计与只读/影子环境边界不变的前提下，为 `benchmark-engine` 补齐面向 `query-execution` 的 workload/backfill 内部编排契约，并把真实环境 object storage live evidence 沉淀为显式 environment-backed 证据而非仓库默认主路径 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `common`,`application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若 workload/backfill orchestration 会绕过 `query-execution` 现有只读/鉴权/审计边界、把 synthetic evidence 冒充成真实环境 live evidence、或把 environment-backed object storage 重新写成 repo-side 默认主路径，需人工确认
- Data impact: benchmark/query-execution 内部 workload snapshot 与 backfill 证据、跨服务执行/审计记录、environment-backed object storage live evidence 与 runbook/验证留痕
- Rollback / recovery: 保持 repo-local lifecycle 与 synthetic fallback 为默认仓库路径，关闭新增跨服务编排或 live evidence 默认启用，回退内部契约/文档说明并恢复到 `D-TASK-024` 已验证基线
- Validation:
  - `sqlforge-shared/query-execution/benchmark-engine 模块测试、跨服务 workload/backfill 契约测试、runtime smoke、task audit、knowledge lint、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-025`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented benchmark/query-execution workload orchestration, explicit synthetic backfill evidence, and environment-backed object-storage live-evidence manifest while keeping LOCAL_FILE as the default artifact path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-025 --include-task-audit --extra-command 'mvn -B -pl sqlforge-shared,query-execution,benchmark-engine -am test -DskipITs' --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'
  - Residual risk: Workload/backfill evidence is now repo-side orchestrated, but long-term governance persistence of that evidence and real external object-storage write/recovery proof still remain environment-backed follow-up work.
  - Next step: Shape the next repo-side follow-up around persisting workload/backfill evidence deeper into governance traceability and extending environment-backed object-storage verification from live-evidence manifests to real external write/recovery proof.

### D-TASK-024: 收口 `benchmark-engine` artifact tenant-specific retention/backfill policy 与 environment-backed storage adapter/evidence

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add tenant artifact policy adapter`
- Priority: 1
- Depends on: `D-TASK-023`
- Scope: 在保持 repo-local artifact lifecycle 仍是默认主路径的前提下，为 benchmark artifact 增加 tenant-specific retention/backfill policy 语义，并补齐 environment-backed object-storage adapter/evidence 的明确边界、接线与验证基线 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若 tenant-specific retention/backfill policy 会误删仍需保留的 artifact、让 environment-backed adapter 变成 repo-side 默认主路径、或引入未经确认的真实对象存储依赖/凭据写入，需人工确认
- Data impact: benchmark artifact policy 配置、repo-local / environment-backed storage adapter 接线、artifact evidence 与 recovery/backfill 记录
- Rollback / recovery: 保持 repo-local lifecycle 为默认主路径，关闭 environment-backed adapter 默认启用，回退新增 artifact policy/adapter 语义与文档说明
- Validation:
  - `benchmark-engine/governance 模块测试、artifact policy/adapter 契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-024`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: implemented tenant-specific artifact retention/backfill policy resolution from governance `tenant_config.retention_days`, persisted retention metadata into benchmark artifact snapshots, and kept `LOCAL_FILE` as the default lifecycle path.
  - 2026-04-24: added explicit `ENVIRONMENT_OBJECT_STORAGE` adapter semantics with repo-local mirror evidence, object URI metadata, and module tests covering cleanup, recovery, tenant policy backfill, governance contract resolution, and adapter selection.
- Context closeout:
  - Completed scope: Implemented governance-backed tenant artifact retention/backfill policy resolution, persisted retention/evidence metadata into benchmark artifacts, and added explicit ENVIRONMENT_OBJECT_STORAGE adapter semantics while keeping LOCAL_FILE as the default lifecycle path.
  - Validation evidence: Passed foreman validate for D-TASK-024 with module tests, knowledge lint, compile-governance check, runtime smoke, and pre-closeout task audit.
  - Residual risk: Real external object storage upload/live evidence and deeper benchmark/query-execution workload-backfill orchestration remain follow-up work; the repo-side environment-backed adapter currently preserves object URI plus repo-local mirror evidence only.
  - Next step: Shape the next repo-side follow-up around benchmark/query-execution workload-backfill orchestration and real environment object-storage live evidence; until then, keep no instantiated mainline task.

### D-TASK-023: 收口 `benchmark-engine` 报告查询审计追溯增强与 artifact 生命周期基线

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): harden report audit and artifact lifecycle`
- Priority: 1
- Depends on: `D-TASK-022`
- Scope: 在保留 repo-closed artifact storage、统一授权入口、治理审计与只读/影子环境边界的前提下，为 benchmark 报告/下载查询补齐 `configSnapshotId/resultId/historyId/exportId` 审计链接，并建立 repo-local artifact retention/recovery/cleanup 语义与验证基线 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D
- Human confirmation point: 若查询审计追溯增强会把错误的 trace/export 键写入 `audit_log`、让 cleanup 删除仍应保留的 artifact，或把 repo-local 生命周期语义误升级为环境级对象存储默认路径，需人工确认
- Data impact: benchmark 报告/下载审计记录、`config_snapshot/execution_result/query_history/export_record/audit_log` 链接键、repo-local artifact 文件与 recovery/cleanup 证据
- Rollback / recovery: 恢复到上一版报告查询/下载审计基线，关闭新增 recovery/cleanup 路径，并回退 artifact lifecycle 文档与验证说明
- Validation:
  - `benchmark-engine/governance 模块测试、报告/下载审计契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-023`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: enriched benchmark report/query download audits with `configSnapshotId/resultId/historyId/exportId` linkage whenever report artifacts already carry governance trace metadata.
  - 2026-04-24: added repo-local artifact lifecycle baseline for keeping the latest report-set, pruning stale sibling files on rewrite, and recovering missing `PDF/HTML/raw-data` files from persisted report snapshots.
  - 2026-04-24: synchronized authority docs and local benchmark governance smoke semantics to the new D-TASK-023 repository truth before standard validation and closeout.
- Context closeout:
  - Completed scope: Implemented trace-linked benchmark report/download audits, repo-local artifact stale-file cleanup and snapshot recovery, updated local smoke semantics, and synchronized authority docs to the new repository truth.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-023 --include-task-audit; mvn -B -pl governance,benchmark-engine -am test -DskipITs; bash scripts/run-runtime-smoke.sh --compose-check; node scripts/lint-repository-knowledge.js; bash -n scripts/manual-benchmark-governance-smoke.sh; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Benchmark artifact lifecycle is still repo-local and report-set scoped; tenant-specific retention/backfill policy plus environment-backed object-storage adapter/evidence remain outside the default repo-side path.
  - Next step: Shape D-TASK-024 to cover benchmark artifact tenant-specific retention/backfill policy and environment-backed object-storage evidence without replacing the repo-local default baseline.

### D-TASK-022: 推进 benchmark-engine 外部 artifact storage、raw-data download 与治理追溯编排

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): externalize artifacts and trace exports`
- Priority: 1
- Depends on: D-TASK-021
- Scope: 在保留 repo-closed 隔离执行、统一授权入口、治理审计与只读/影子环境边界的前提下，把 benchmark 报告导出与 raw-data snapshot 提升到外置 artifact storage 基线，并通过 governance 内部受保护编排把 config/result/history/export 追溯链接到 benchmark-engine 报告与下载路径。
- Matrix context: Phase-D
- Human confirmation point: 若外部 artifact storage 会泄露明文敏感数据、绕过 governance 追溯链/统一授权入口，或把环境级对象存储依赖误写成 repo-closed 默认主路径，需人工确认
- Data impact: benchmark-engine artifact storage 配置、raw-data 下载快照、governance `config_snapshot/execution_result/query_history/export_record/audit_log` 追溯链、跨服务 runtime smoke 证据
- Rollback / recovery: 关闭新增 artifact externalization / trace orchestration 路径，回退到当前 `benchmark_task_report` 持久化导出基线，并恢复上一版报告查询/下载契约与治理文档说明
- Validation:
  - `benchmark-engine/governance 模块测试、导出/下载契约测试、runtime smoke、task audit、schema/mapping 校验、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-022`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: implemented repo-local externalized artifact storage for benchmark `JSON/PDF/HTML` exports and raw-data snapshot download, while keeping repo-closed isolation as the default baseline.
  - 2026-04-24: added governance internal benchmark report trace orchestration so benchmark-engine registers `config_snapshot/execution_result/query_history/export_record` links instead of bypassing governance persistence.
  - 2026-04-24: synchronized authority docs and task ledgers to the new D-TASK-022 repository truth before standard validation and closeout.
- Context closeout:
  - Completed scope: Implemented repo-local benchmark artifact externalization, raw-data download, and governance trace/export orchestration; synchronized authority docs and task ledgers to the new repository truth.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-022 --include-task-audit; mvn -B -pl governance,benchmark-engine -am test -DskipITs; bash scripts/run-runtime-smoke.sh --compose-check; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Benchmark report/download query paths still need richer audit-link enrichment plus artifact retention/recovery/cleanup semantics; environment-backed object storage evidence remains outside the repo-closed default baseline.
  - Next step: Shape D-TASK-023 to enrich benchmark report/download audit linkage and artifact lifecycle semantics without promoting environment-backed storage to the default repo-side path.

### D-TASK-021: 推进 `benchmark-engine` 真实隔离执行与导出链路

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(benchmark-engine): add isolated execution export chain`
- Priority: 1
- Depends on: `D-TASK-020`
- Scope: 把 `benchmark-engine` 从 placeholder 执行/导出基线推进到真实隔离执行、可复现报告快照与导出产物链路，保持只读、影子环境优先、统一授权入口与治理审计契约不变 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实压测执行链会放宽只读/影子环境隔离、绕过统一授权入口/治理审计、或把占位导出直接冒充为真实快照导出，需人工确认
- Data impact: benchmark-engine 执行配置、`benchmark_task` / `benchmark_task_report` 数据、报告快照/导出产物元数据、跨服务审计与 runtime smoke 证据
- Rollback / recovery: 关闭新增真实执行/导出路径，恢复到当前持久化 placeholder 基线，并回退到上一版报告查询契约、隔离约束与审计说明
- Validation:
  - `benchmark-engine 模块测试、导出/报告契约测试、runtime smoke、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-021`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: replaced the placeholder benchmark path with a repo-closed isolated execution chain, persisted `execution_summary_json` and `export_artifacts_json` on `benchmark_task_report`, and switched PDF/HTML report queries to serve the stored export bundle instead of in-method placeholder rendering.
- Context closeout:
  - Completed scope: Implemented repo-closed isolated benchmark execution, persisted execution summaries plus JSON/PDF/HTML export artifacts on benchmark_task_report, switched report rendering to replay stored artifacts, and synchronized benchmark-engine truth/docs/governance state.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-021 --include-task-audit --extra-command 'mvn -B -pl benchmark-engine -am test -DskipITs' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'node scripts/lint-repository-knowledge.js'; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Benchmark-engine now has a repo-closed isolated execution/export baseline, but external file storage, raw data download, deeper cross-service orchestration, and environment-level execution evidence remain follow-up gaps; no new repo-side mainline task is instantiated yet.
  - Next step: If benchmark-engine follow-up continues, shape and instantiate a new Phase-D task for external artifact storage/raw-data download/cross-service orchestration before further implementation; otherwise keep the repository truth explicit that no repo-side mainline is instantiated.

### HARN-023: Reconcile E-TASK-016 post-closeout drift

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile e-task-016 active wave`
- Priority: 1
- Depends on: E-TASK-016
- Scope: Align the current active wave after E-TASK-016 closeout, remove the stale pointer to the completed frontend boundary-hardening task, and update repository truth so the next repo-side mainline is either explicitly shaped or stated as uninstantiated without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-023`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: repointed the master plan away from completed `E-TASK-016`, restored the repository truth that no repo-side mainline is currently instantiated, and shaped `D-TASK-021` as the next uninstantiated repo-side mainline around `benchmark-engine` real isolated execution/export follow-up without changing frontend/runtime or environment-backed follow-up semantics.
- Context closeout:
  - Completed scope: Reconciled the post-closeout drift left behind after E-TASK-016, repointed the master plan away from the completed frontend boundary-hardening task, restored the explicit truth that no repo-side mainline is currently instantiated, and shaped D-TASK-021 as the next formal but uninstantiated repo-side mainline around benchmark-engine real isolated execution/export follow-up without changing frontend/runtime or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-023 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The active-wave truth and task matrices are back in sync, but D-TASK-021 is only shaped, not instantiated, so benchmark-engine still remains on the current persisted placeholder execution/export baseline until the next repo-side mainline is explicitly started; HARN-016 and INBOX-001 remain separate environment-backed follow-ups.
  - Next step: Instantiate D-TASK-021 explicitly before implementation so benchmark-engine real isolated execution/export work proceeds on the now-shaped Phase-D mainline while preserving the local-only dev-smoke boundary and existing environment-backed follow-up semantics.

### E-TASK-016: 固化 dev browser smoke 的 local repo-closed 基线语义

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(frontend): lock dev smoke to local baseline`
- Priority: 1
- Depends on: `E-TASK-015`
- Scope: 明确 Vite dev browser smoke 只作为本地 repo-closed 开发验证基线，不把它升级到更广的 CI/runtime gating，并同步后续计划/操作文档保持 full-stack runtime smoke 作为多服务主路径 Tech: `VUE-FE`,`OPS`,`DOCS`. Layer: `frontend/router/views/styles`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-E / Story `E-STORY-005` 前端构建链迁移与便携产物治理
- Human confirmation point: 若把 dev browser smoke 从当前 local repo-closed 基线升级为默认 CI/runtime gate、削弱现有 full-stack runtime smoke 主路径，或通过该任务改写 `E-TASK-015` 的历史完成结论，需人工确认
- Data impact: dev browser smoke 的边界定义、前端验证语义、CI/runtime gating 叙事与文档表述
- Rollback / recovery: 保留 `E-TASK-015` 已验证的 local dev smoke 基线，回退高风险边界/脚本/文档改动，并恢复 full-stack runtime smoke 作为多服务主路径的既有真值
- Validation:
  - `npm run lint`、`npm run build`、`npm run build:portable`、`node scripts/check-dev-frontend.mjs`、`node scripts/lint-repository-knowledge.js`、task audit
  - `python3 scripts/foreman.py validate E-TASK-016`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reinforced the frontend dev browser smoke boundary so it remains a local repo-closed baseline only, updated local-development and CI/phase-gate baseline docs to keep full-stack runtime smoke as the multi-service main path, and added repository knowledge-lint checks that fail if smoke:frontend-dev or check-dev-frontend.mjs is wired into default CI/runtime gate entrypoints.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-016 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run build:portable" --extra-command "node scripts/check-dev-frontend.mjs" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The local-only dev smoke boundary is now documented and lint-enforced, but the next business-facing frontend or CI task could still attempt to widen that boundary; HARN-016 and INBOX-001 remain unrelated environment-backed follow-ups, and any future promotion of dev smoke into broader gating still requires explicit human confirmation.
  - Next step: Before implementing the next business-facing task, instantiate it explicitly and verify its contract, authority docs, validation path, and task-matrix/governance fields are complete; if the next task touches CI/runtime semantics, preserve full-stack runtime smoke as the default main path unless a new confirmed task changes that boundary.

### HARN-022: Shape E-TASK-016 from E-TASK-015 residual risk

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): shape e-task-016 local dev smoke boundary`
- Priority: 1
- Depends on: HARN-021
- Scope: Align the active-wave truth after HARN-021, capture the confirmed decision that the Vite dev browser smoke remains a local repo-closed baseline only, and shape the resulting repo-side frontend follow-up as E-TASK-016 in the master plan and task matrices without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-022`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: captured the explicit human decision that Vite dev browser smoke remains a local repo-closed baseline only, updated the Phase-E plan/matrices to shape the resulting follow-up as `E-TASK-016`, and repointed the active-wave truth away from the completed `HARN-021` batch to the new uninstantiated repo-side frontend mainline without changing frontend/runtime or environment-backed follow-up semantics.
- Context closeout:
  - Completed scope: Instantiated HARN-022, updated the Phase-E active-wave truth to point at the newly shaped follow-up E-TASK-016, added E-TASK-016 to the master plan and both task matrices, and captured the explicit decision that Vite dev browser smoke remains a local repo-closed baseline only rather than a broader CI/runtime gate without changing frontend/runtime or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-022 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The new Phase-E follow-up is now shaped as E-TASK-016, but it is not yet instantiated; HARN-016 and INBOX-001 remain environment-backed follow-ups, and the dev browser smoke boundary now depends on future work continuing to preserve the local-only semantics instead of re-promoting it into broader CI/runtime gating.
  - Next step: If frontend follow-up continues, instantiate E-TASK-016 explicitly before implementation so the local repo-closed dev-smoke boundary can be hardened without altering the existing full-stack runtime smoke main path; otherwise keep HARN-016 blocked and INBOX-001 open as non-mainline follow-ups.

### HARN-021: Reconcile E-TASK-015 post-closeout drift

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile e-task-015 active wave`
- Priority: 1
- Depends on: E-TASK-015
- Scope: Align the current active wave after E-TASK-015 closeout, remove the stale pointer to the completed frontend task, and make the repository truth explicit about whether any repo-side mainline is currently instantiated without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-021`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: updated the master execution plan current active wave after `E-TASK-015` closeout so repository truth no longer points at a completed frontend task; current state now explicitly shows no instantiated repo-side mainline task while `HARN-021` remains the active governance reconciliation batch and `HARN-016` plus `INBOX-001` stay as non-mainline environment-backed follow-ups.
- Context closeout:
  - Completed scope: Instantiated HARN-021, updated the master execution plan current active wave so it no longer points at completed E-TASK-015, made the repository truth explicit that no repo-side mainline task is currently instantiated, and absorbed the append-only validation-log residue for E-TASK-015/HARN-021 into the governed batch without changing frontend/runtime or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-021 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The plan truth is back in sync, but the repository still has no newly instantiated repo-side mainline task; HARN-016 and INBOX-001 remain environment-backed follow-ups, and any next mainline must be explicitly shaped before reusing the active-wave block.
  - Next step: When the next repo-side priority is chosen, instantiate it explicitly before changing the active-wave block again; until then keep HARN-016 blocked and INBOX-001 open as non-mainline follow-ups.

### E-TASK-015: 补齐前端 dev browser smoke 并清理 SFC 恢复后的当前叙事

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): add Vite dev browser smoke`
- Priority: 1
- Depends on: `E-TASK-014`
- Scope: 为 Vite dev server 补齐轻量浏览器 smoke，并清理仍把 non-SFC 迁移表述成当前真值的计划/操作文档；保留现有 Vue SFC、portable 包与 full-stack runtime smoke 语义 Tech: `VUE-FE`,`OPS`,`DOCS`. Layer: `frontend/router/views/styles`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-E / Story `E-STORY-005` 前端构建链迁移与便携产物治理
- Human confirmation point: 若新增 dev browser smoke 会替代既有 full-stack runtime smoke、削弱现有 portable / 代理语义验证，或为清理叙事而改写历史任务完成记录，需人工确认
- Data impact: Vite dev server 浏览器 smoke 覆盖、当前前端治理叙事、路由/代理语义
- Rollback / recovery: 保留 `E-TASK-014` 已验证的 Vue SFC / portable 基线，回退高风险 dev smoke 或文档清理改动，并恢复到上一个已验证的前端交付真值
- Validation:
  - `npm run lint`、`npm run build`、`npm run build:portable`、toolchain/portable/dev browser smoke 检查、task audit
  - `python3 scripts/foreman.py validate E-TASK-015`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: added a lightweight Vite dev browser smoke entrypoint that boots the dev server, validates history-route rendering plus query success/recovery flows against browser-side `/api/*` mocks, and updated operations/separation docs so current frontend truth distinguishes Vue SFC dev smoke from the heavier full-stack runtime smoke.
- Context closeout:
  - Completed scope: Added a lightweight Vite dev browser smoke that boots the local dev server, validates history-mode route rendering plus query success/recovery flows against browser-side /api mocks, records dev request-header semantics, and updates operations/separation docs so the current frontend truth distinguishes the restored Vue SFC dev baseline from the heavier full-stack runtime smoke.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-015 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run build:portable" --extra-command "node scripts/check-frontend-toolchain.mjs" --extra-command "node scripts/check-portable-frontend.mjs" --extra-command "node scripts/check-dev-frontend.mjs"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The repository now has a lightweight Vite dev browser smoke and the current SFC/dev-baseline docs are aligned, but the new smoke is intentionally browser-mocked and does not replace the existing multi-service runtime smoke; historical non-SFC task narratives remain in tasks-done by design as immutable completion records, and HARN-016 plus INBOX-001 remain unchanged environment-backed follow-ups.
  - Next step: If frontend hardening continues, decide whether the lightweight dev browser smoke should stay as a local repo-closed baseline only or be promoted into broader CI/runtime gating alongside the existing full-stack smoke without duplicating coverage.

### HARN-020: Reconcile E-TASK-014 closeout drift and shape E-TASK-015

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile E-TASK-014 closeout drift`
- Priority: 1
- Depends on: E-TASK-014
- Scope: Align the current active wave and validation-log residue after E-TASK-014 closeout, then decide and shape the next repo-side frontend follow-up E-TASK-015 around dev browser smoke coverage and historical non-SFC narrative cleanup without changing frontend/runtime behavior or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-020`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: reconciled the stale Phase-E active-wave pointer left behind after `E-TASK-014` closeout, kept the append-only validation-log residue inside the governed batch, and shaped `E-TASK-015` as the next repo-side frontend follow-up for lightweight Vite dev browser smoke plus current-state non-SFC narrative cleanup.
- Context closeout:
  - Completed scope: Aligned the Phase-E active wave away from the stale E-TASK-014 pointer, updated the Phase-E master plan plus both task matrices to shape E-TASK-015 as the next repo-side frontend follow-up, and absorbed the append-only validation-log residue into the governed batch without changing frontend/runtime behavior or environment-backed follow-up semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-020 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: The governance chain is back in sync, but E-TASK-015 is not yet instantiated, so the repository still lacks the planned lightweight Vite dev browser smoke and the current-state non-SFC narrative cleanup beyond the newly aligned plan/matrix truth; HARN-016 and INBOX-001 remain unchanged environment-backed follow-ups.
  - Next step: Instantiate E-TASK-015 if frontend follow-up continues so the Vite dev server gains lightweight browser smoke coverage and current governance/operations docs are fully aligned with the restored Vue SFC baseline without rewriting historical task records.

### E-TASK-014: 恢复 Vue SFC 构建链并保留前端便携产物

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): restore Vue SFC toolchain`
- Priority: 1
- Depends on: E-TASK-013
- Scope: Restore the root frontend to Vue single-file components now that @vitejs/plugin-vue and @vue/compiler-sfc are allowed again, while preserving the current frontend behavior and the portable build/package workflow.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-014`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: human policy changed again to allow `@vitejs/plugin-vue` and `@vue/compiler-sfc`, so the task scope was executed as a structured restoration of the Vue SFC toolchain rather than another non-SFC hardening step.
  - 2026-04-24: restored the historical `.vue` root and route-view source files, rewired the app entry and lazy-loaded router imports back to SFC modules, reintroduced `@vitejs/plugin-vue` plus `@vue/compiler-sfc`, and removed the temporary local vue runtime shim plus generated `.js`/`.css` component artifacts.
  - 2026-04-24: preserved the later portable build, vendor chunk split, explicit Element Plus component registration, and browser smoke workflow, then updated the frontend toolchain check and governance truth to validate the restored SFC-based contract instead of the prior dependency-ban policy.
- Context closeout:
  - Completed scope: Restored the root frontend back to Vue single-file components, reintroduced @vitejs/plugin-vue plus @vue/compiler-sfc, removed the temporary vue runtime shim and generated JS/CSS view artifacts, and kept the portable package plus chunk-splitting/browser-smoke workflow intact.
  - Validation evidence: Validated with npm run lint, npm run build, npm run build:portable, node scripts/check-frontend-toolchain.mjs, node scripts/check-portable-frontend.mjs, and foreman validate including task_audit.
  - Residual risk: The repository now again depends on the Vue SFC toolchain and still assumes Node 18.20.8 on target environments, while some local dev pages can still surface backend-driven 404/500 responses when their backing services are not running.
  - Next step: If frontend follow-up continues, add a small browser smoke for the dev build itself and decide whether the now-restored SFC architecture should trigger any cleanup of historical non-SFC task narratives beyond the new governance truth sync.

### E-TASK-013: 补齐 portable 前端浏览器 smoke 并收口构建分包告警

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): harden portable browser smoke and chunk budget`
- Priority: 1
- Depends on: `E-TASK-012`
- Scope: 把 portable 包纳入关键路由浏览器 smoke，并收口当前 Vite 大 chunk 告警而不重引 SFC 依赖 Tech: `VUE-FE`,`OPS`. Layer: `frontend/router/views/styles`,`deployments/ci/scripts`.
- Matrix context: Phase-E / Story `E-STORY-005` 前端构建链迁移与便携产物治理
- Human confirmation point: 若 portable 验证被降级为 health-only 检查、分包方案改变路由/代理/缓存语义，或为压低 chunk 告警而牺牲关键页面可用性，需人工确认
- Data impact: portable 浏览器 smoke 覆盖、前端 chunk 输出、关键路由与代理语义
- Rollback / recovery: 保留当前 portable 包与关键路由语义，回退高风险分包策略，并恢复到现有可工作的构建输出
- Validation:
  - `npm run lint`、`npm run build`、`npm run build:portable`、portable browser smoke、chunk warning 检查
  - `python3 scripts/foreman.py validate E-TASK-013`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: replaced the root `ElementPlus` full-library install with explicit component registration, lazy-loaded the route views to reduce eager payload, added a repo-specific Vite chunk budget plus vendor chunk split, and upgraded `scripts/check-portable-frontend.mjs` from a health-only check to a browser-driven portable smoke with deep hash-route coverage and proxy-header assertions against a local mock backend.
- Context closeout:
  - Completed scope: Added a browser-driven portable frontend smoke that boots the packaged dist-portable server against a local mock backend, verifies deep hash-route rendering plus proxy-header forwarding, replaced the root full-library Element Plus install with explicit component registration, lazy-loaded route views, and tightened the Vite bundle layout with vendor chunking plus a repo-specific chunk budget so the previous default large-chunk warning no longer fires.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-013 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "npm run build:portable" --extra-command "node scripts/check-portable-frontend.mjs"; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: Portable browser smoke now covers representative packaged routes and proxy semantics against a mock backend, but it still does not exercise a live backend stack, and dist-portable remains an environment-sensitive artifact that assumes a local Node runtime plus correct backend target URLs on the destination host; HARN-016 and INBOX-001 remain unchanged environment-backed follow-ups.
  - Next step: If frontend hardening continues, decide whether to extend the portable smoke from mock-backend verification to a live backend runtime path or keep the current repo-closed mock-backed smoke as the stable portable baseline.

### HARN-019: Reconcile E-TASK-011/E-TASK-012 post-closeout drift

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(governance): reconcile frontend post-closeout drift`
- Priority: 1
- Depends on: E-TASK-012
- Scope: Align current active wave, task matrices, INBOX/ledger truth, and validation-log closeout residue after E-TASK-011 and E-TASK-012 completion; decide whether the remaining frontend residual risk becomes the next repo-side hardening task without changing business code or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-019`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: aligned the current active wave away from the stale Phase-D pointer, backfilled `E-TASK-010` through `E-TASK-012` into the Phase-E plan/matrices, and shaped the remaining portable browser-smoke plus bundle-warning residual risk into the next repo-closed frontend hardening follow-up `E-TASK-013`.
- Context closeout:
  - Completed scope: Aligned the current active wave away from the stale Phase-D pointer, backfilled E-TASK-010 through E-TASK-012 into the Phase-E master plan and both task matrices, and converted the remaining portable browser-smoke plus bundle-warning residual risk into the next repo-closed frontend hardening follow-up E-TASK-013 while absorbing the append-only validation-log tail into the governed batch.
  - Validation evidence: python3 scripts/foreman.py validate HARN-019 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout; npm run build (confirmed current Vite large-chunk warning remains as E-TASK-013 input)
  - Residual risk: The repository plan and matrices are now back in sync, but E-TASK-013 is not yet instantiated, so the portable package still lacks dedicated browser smoke coverage and the production build still emits Vite's large-chunk warning; HARN-016 and INBOX-001 also remain unchanged environment-backed follow-ups.
  - Next step: Instantiate E-TASK-013 if frontend hardening continues so the portable package gains browser-level smoke coverage and the current chunk warning is reduced without reintroducing the Vue SFC toolchain.

### E-TASK-012: 修复前端非 SFC 迁移后的布局回归

- Status: done
- Completed at: 2026-04-24
- Commit subject: `fix(frontend): restore Element Plus layout baseline`
- Priority: 1
- Depends on: E-TASK-011
- Scope: Diagnose and fix the frontend layout regressions introduced by the non-SFC migration so the app restores its intended sidebar, header, dashboard, and route-level page layouts without reintroducing @vitejs/plugin-vue or @vue/compiler-sfc.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-012`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: reproduced the layout regression in a browser session and confirmed the non-SFC migration had restored `app.use(ElementPlus)` but not the Element Plus base stylesheet, causing `el-container` / `el-aside` / `el-header` layout primitives to collapse into default block flow.
  - 2026-04-24: restored `element-plus/dist/index.css`, added the missing `--sqlforge-radius-xl` design token, and revalidated dashboard plus representative route layouts together with standard and portable frontend builds.
- Context closeout:
  - Completed scope: Reproduced the non-SFC layout regression, restored Element Plus base CSS so container primitives render with their intended flex layout again, added the missing radius token, and refreshed the portable frontend assets to match the fixed shell styling.
  - Validation evidence: Validated with browser-based layout checks across representative routes, npm run lint, npm run build, npm run build:portable, node scripts/check-portable-frontend.mjs, and foreman validate including task_audit.
  - Residual risk: Representative route layout is restored, but some pages still surface backend-driven 404/500 responses in the local dev environment and the production bundle remains larger than Vite's default chunk warning threshold.
  - Next step: If frontend hardening continues, capture a small automated browser smoke for key routes and consider bundle splitting to reduce the large-entry warning.

### E-TASK-011: 去除 Vue SFC 构建链并增加双产物便携前端包

- Status: done
- Completed at: 2026-04-24
- Commit subject: `feat(frontend): remove SFC pipeline and add portable package`
- Priority: 1
- Depends on: N/A
- Scope: Migrate the root frontend away from Vue single-file components so the repository no longer depends on @vitejs/plugin-vue or @vue/compiler-sfc, align Node/NPM/Vite/Vue/vue-i18n versions to the requested baseline, and add both standard and portable frontend build outputs where the portable package can be copied to another host and started locally without recompilation while still proxying to real backend APIs.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-011`
- Progress log:
  - 2026-04-24: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-24: converted the root frontend away from `.vue` single-file components by generating plain `.js` component modules plus extracted `.css`, rewired the app entry and router imports, and removed the direct Vite SFC plugin / auto-import tooling path from the active build.
  - 2026-04-24: aligned the frontend toolchain contract to Node `18.20.8`, npm `10.8.2`, Vite `5.4.11`, Vue runtime `3.5.13`, and `vue-i18n` `10.0.8`, then replaced the root `vue` package with a local runtime shim so the repository no longer resolves `@vue/compiler-sfc`.
  - 2026-04-24: added a second portable frontend output in `dist-portable/` with relative assets, hash-history routing, a local proxy server, generated startup scripts, and a portable config file so the built package can be copied to another host and started without recompilation.
- Context closeout:
  - Completed scope: Migrated the root frontend away from Vue single-file components into plain JavaScript plus extracted CSS modules, rewired the app bootstrap and routes, and removed the active Vite SFC plugin/auto-import build path.
  - Validation evidence: Validated with npm lint/build/build:portable, frontend toolchain checks, portable package checks, and foreman validate including task_audit.
  - Residual risk: The portable bundle still requires a local Node 18.20.8 runtime plus correct backend target URLs, and no live browser smoke against a real backend stack was executed in this task.
  - Next step: Run a browser smoke against the portable package with a live backend target and consider bundle splitting if transfer size becomes a deployment concern.

### E-TASK-010: 收敛前端 Node/Vite/Vue 版本并核对 Vue SFC 构建约束

- Status: done
- Completed at: 2026-04-23
- Commit subject: `chore(frontend): align toolchain versions and capture SFC constraint`
- Priority: 1
- Depends on: N/A
- Scope: Pin the root frontend toolchain to Node 18.20.8, Vite 5.4.11, and Vue 3.5.13; inspect whether the current Vue single-file-component build can operate without @vitejs/plugin-vue and without a direct @vue/compiler-sfc dependency; apply only repository-truth-consistent changes and keep runtime smoke/build validation traceable.
- Validation:
  - `python3 scripts/foreman.py validate E-TASK-010`
  - `npm run build`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: pinned the root frontend toolchain declarations to Node `18.20.8`, Vite `5.4.11`, and Vue `3.5.13`; removed the direct `@vue/compiler-sfc` devDependency; refreshed the npm lockfile plus GitHub Actions Node version selectors; and verified `npm run build` still passes on `vite@5.4.11`.
  - 2026-04-23: confirmed the current frontend still imports `src/App.vue` and route/view `.vue` single-file components, `vite.config.js` still depends on `@vitejs/plugin-vue`, and `vue@3.5.13` still carries `@vue/compiler-sfc` transitively, so the requested "unsupported `@vitejs/plugin-vue` / unsupported `@vue/compiler-sfc`" state cannot be reached without a broader non-SFC frontend migration or a tooling-policy exception.
- Next action: After human confirmation, either keep the current Vue SFC architecture and accept `@vitejs/plugin-vue` plus Vue's transitive `@vue/compiler-sfc`, or open a dedicated refactor task to migrate `src/App.vue`, router views, and the Vite transform path away from `.vue` SFC usage.
- Escalation: If the environment policy truly bans `@vitejs/plugin-vue` or any transitive `@vue/compiler-sfc`, stop treating this as a version-only dependency change and escalate it as a scoped frontend architecture migration with explicit acceptance of rewrite cost and regression risk.
- Human decision: Decide whether repository truth should continue using Vue SFCs with `@vitejs/plugin-vue`, or whether to authorize a broader refactor that removes `.vue` SFC usage and accepts the required build/runtime rewiring.
- INBOX ref: INBOX-003
- Context closeout:
  - Completed scope: Pinned the root frontend toolchain declarations and CI workflows to Node 18.20.8, Vite 5.4.11, and Vue 3.5.13, removed the direct @vue/compiler-sfc devDependency, refreshed the npm lockfile, and recorded the remaining Vue single-file-component build constraint through the task and INBOX audit chain instead of pretending the current SFC frontend can run without its required plugin/tooling path.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-010 --include-task-audit --extra-command 'npm run build' --extra-command 'npm ls @vue/compiler-sfc'; npm ls @vitejs/plugin-vue @vue/compiler-sfc vue vite
  - Residual risk: The repository frontend still imports src/App.vue and route-level .vue files through vite.config.js with @vitejs/plugin-vue, and vue@3.5.13 still carries @vue/compiler-sfc transitively, so a strict ban on either package still requires a broader non-SFC migration or a tooling-policy exception.
  - Next step: Decide whether to keep the current Vue SFC architecture with its required plugin/transitive compiler path, or authorize a dedicated frontend migration task that removes .vue SFC usage before enforcing a stricter package ban.

### HARN-018: Reconcile D-TASK-020 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): HARN-018 reconcile d-task-020 drift`
- Priority: 1
- Depends on: D-TASK-020
- Scope: Align current active wave and plan truth after D-TASK-020 completion without inventing a new unapproved mainline task or changing environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-018`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: updated the master execution plan current active wave after D-TASK-020 closeout so repository truth no longer points at a completed repo-side mainline task; current state now explicitly shows no instantiated mainline task while HARN-016 and INBOX-001 remain environment-backed follow-ups only.
- Context closeout:
  - Completed scope: Aligned the Phase-D current active wave after D-TASK-020 closeout by clearing the stale pointer to a completed repo-side mainline task and updating the master plan to reflect the current repository truth: no active repo-side mainline is instantiated, while HARN-016 and INBOX-001 remain external or environment-backed follow-ups.
  - Validation evidence: python3 scripts/foreman.py validate HARN-018 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"
  - Residual risk: The repository mainline is now truthfully idle rather than stale, but the external Hetu/MRS evidence wait in HARN-016 and the Sonar restoration decision in INBOX-001 remain unresolved; validation-log tails continue as append-only audit residue outside the single-task stage scope.
  - Next step: When a new repo-side priority is chosen, instantiate it explicitly before changing the active-wave block again; until then keep HARN-016 blocked and INBOX-001 open as non-mainline follow-ups.

### D-TASK-020: 补齐异步服务执行遥测与业务指标基线

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(observability): D-TASK-020 add async metrics baseline`
- Priority: 1
- Depends on: D-TASK-019
- Scope: Add minimal Micrometer business metrics for sql-optimization and benchmark-engine, keep low-cardinality tags, extend tests/runtime smoke, and sync observability/document-truth docs without changing external environment-backed workflows.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若异步服务新增遥测暴露敏感信息、为便于排障引入 task id / tenant id 等高基数标签，或削弱既有日志/审计语义以换取指标简化，需人工确认
- Data impact: sql-optimization/benchmark-engine 指标、异步任务终态信号、worker/report 延迟可观测数据
- Rollback / recovery: 移除高风险 meter、恢复以日志/审计为主的既有语义，并回退到上一版稳定 tags 与文档说明
- Validation:
  - `sql-optimization/benchmark-engine 模块测试、prometheus 指标断言、runtime smoke、task audit、文档同步`
  - `python3 scripts/foreman.py validate D-TASK-020`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added `OptimizationMetricsRecorder` and `BenchmarkMetricsRecorder`, wired minimal Micrometer counters/timers into async submit/worker/report paths, extended targeted unit coverage with meter assertions, and updated observability/document-truth authority text from “async services mainly rely on logs” to the new four-service minimal metrics baseline.
- Context closeout:
  - Completed scope: Added OptimizationMetricsRecorder and BenchmarkMetricsRecorder, wired minimal Micrometer counters/timers into sql-optimization submit/worker paths and benchmark-engine submit/worker/report paths, extended targeted unit coverage with meter assertions, recompiled authority-map policy state, and synchronized observability/document-truth docs so all four backend services now have a repo-closed minimal business metrics baseline.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-020 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl sql-optimization,benchmark-engine -am test -DskipITs -Dtest=OptimizationTaskApplicationServiceTest,OptimizationTaskWorkerTest,BenchmarkTaskApplicationServiceTest,BenchmarkTaskWorkerTest,BenchmarkReportApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false" --extra-command "bash scripts/run-runtime-smoke.sh --runtime-smoke" --extra-command "node scripts/lint-repository-knowledge.js"
  - Residual risk: The repository now exposes minimal async-service metrics, but PrometheusRule/Alertmanager/Grafana assets, log-pipeline templates, tracing, and external-environment follow-ups such as HARN-016 Hetu/MRS evidence and INBOX-001 Sonar restoration remain outside this task scope; validation-log tails also continue as append-only audit residue outside the single-task stage scope.
  - Next step: If Phase-D observability hardening continues, reconcile the current active wave after this closeout and decide whether the next repo-side task should target broader cross-service tracing/aggregated observability or another governed follow-up.

### HARN-017: Reconcile D-TASK-019 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): HARN-017 reconcile d-task-019 drift`
- Priority: 1
- Depends on: D-TASK-019
- Scope: Align current active wave, ledgers, and plan truth after D-TASK-019 completion without changing business code or environment-backed follow-up semantics.
- Validation:
  - `python3 scripts/foreman.py validate HARN-017`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: updated the master execution plan current active wave from completed `D-TASK-019` to the new repo-closed follow-up `D-TASK-020`, and extended the task spec / governance matrices so the next async-service observability hardening step has formal scope, validation, and low-cardinality telemetry guardrails.
- Context closeout:
  - Completed scope: Aligned the Phase-D current active wave after D-TASK-019 closeout by moving the mainline from the completed query-execution/governance telemetry task to a new repo-closed follow-up D-TASK-020, and synchronized the master plan plus task spec/governance matrices so async-service observability hardening is now the formal next executable task without changing any business-code or environment-backed semantics.
  - Validation evidence: python3 scripts/foreman.py validate HARN-017 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"
  - Residual risk: The external Hetu/MRS evidence wait in HARN-016 and the Sonar environment-restoration follow-up in INBOX-001 remain unchanged environment-backed items; validation-log closeout tails also continue as append-only audit residue outside the single-task stage scope.
  - Next step: Instantiate D-TASK-020 and implement minimal Micrometer business metrics for sql-optimization and benchmark-engine so all four backend services share a repo-closed observability baseline.

### D-TASK-019: 补齐 query-execution 执行遥测与业务指标基线

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(observability): add query and governance metrics baseline`
- Priority: 1
- Depends on: D-TASK-018
- Scope: 为 query-execution 与 governance 补最小业务级 Micrometer 指标与执行遥测，支撑后续 Hetu 参数调优与运行时排障；不依赖外部测试环境，不扩展业务功能。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-019`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: aligned Phase-D active-wave wording plus task/governance matrices so `D-TASK-019` becomes the repo-closed follow-up for query-execution telemetry hardening rather than external-environment tuning.
  - 2026-04-23: added `QueryExecutionMetricsRecorder` and `GovernanceMetricsRecorder`, wiring minimal Micrometer counters/timers/gauges for query results, mode hits/attempts, timeout/fallback/route-unavailable, audit fallback, retry count, and database queue backlog.
  - 2026-04-23: extended query-execution/governance unit coverage with prometheus metric assertions and updated observability/document-truth authority text from “no business metrics” to the new minimal implemented baseline.
- Context closeout:
  - Completed scope: Added minimal Micrometer business metrics for query-execution and governance, including query request/latency/mode-hit/mode-attempt/timeout/fallback/route-unavailable signals plus governance audit-fallback, message-retry, and database queue backlog meters; wired the recorders into the existing services, updated targeted unit coverage with metric assertions, and synchronized the Phase-D plan/matrix plus observability/document-truth authority docs to the new repo-closed telemetry baseline.
  - Validation evidence: Validated with python3 scripts/foreman.py validate D-TASK-019 --include-task-audit --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'mvn -B -pl query-execution,governance -am test -DskipITs -Dtest=QueryExecutionApplicationServiceTest,GovernanceAuditTrailServiceTest,MessageAdminApplicationServiceTest -Dsurefire.failIfNoSpecifiedTests=false' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke' --extra-command 'node scripts/lint-repository-knowledge.js'; along the way runtime smoke exposed constructor-selection regressions in governance/query-execution startup, which were fixed and then verified by a full passing runtime smoke run.
  - Residual risk: The repository now exposes minimal tuning-oriented metrics for query-execution/governance, but external Hetu/MRS evidence, production parameter calibration, PrometheusRule/Alertmanager/Grafana assets, and broader telemetry coverage for sql-optimization/benchmark-engine still remain outside the repository-closed baseline.
  - Next step: Use the new query-execution/governance metrics as the default repo-closed tuning baseline, and if Phase-D continues telemetry hardening, extend the same Micrometer coverage to sql-optimization and benchmark-engine before reopening external-environment parameter tuning.

### D-TASK-018: Remove legacy foreign keys and enforce traceability integrity in application

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(governance): remove traceability foreign keys`
- Priority: 1
- Depends on: `D-TASK-017`
- Scope: 移除 governance 核心追溯链在 MySQL/TDSQL 上的历史外键约束，补齐引用键索引与应用层完整性校验；同步更新 init-schema、migration、映射测试、主计划/任务矩阵，以及 Hetu/MRS 测试环境部署文档为 Win10+IDEA+yml 配置读取口径，并提供待确认清单与选项。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-018`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: aligned master plan, task spec matrix, governance extension matrix, and validation baseline so `D-TASK-018` becomes the active Phase-D mainline task for foreign-key removal and application-level integrity enforcement.
  - 2026-04-23: removed physical foreign-key constraints from `sql/init-schema.sql`, added `sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql` for published environments, and updated persistence authority text from schema-level foreign keys to reference-key + application-integrity semantics.
  - 2026-04-23: extended `GovernanceProtectedPersistenceService` to validate referenced records, tenant consistency, and traceability chain coherence before persisting `execution_result`、`query_history`、`export_record`、`audit_log`; refreshed `TraceabilitySchemaMappingTest` and `GovernanceProtectedPersistenceServiceTest` accordingly.
  - 2026-04-23: rewrote the Hetu/MRS deployment runbook to the requested `Win10 + IDEA + yml` configuration flow, explicitly excluded Kafka validation for test env, and replaced the old smoke-first emphasis with a deployment confirmation checklist and operator options.
- Context closeout:
  - Completed scope: Removed physical foreign-key constraints from the governance traceability schema baseline in sql/init-schema.sql, added V20260423_017__drop_traceability_foreign_keys.sql for published environments, and shifted traceability integrity enforcement into GovernanceProtectedPersistenceService so execution_result/query_history/export_record/audit_log now validate referenced-record existence, tenant consistency, and chain coherence in application code. Also updated the Phase-D plan/matrices, persistence and validation docs, and rewrote the Hetu/MRS deployment runbook to the requested Win10 + IDEA + yml configuration flow with Kafka excluded and a confirmation-checklist-first operator path.
  - Validation evidence: python3 scripts/foreman.py validate D-TASK-018 --include-task-audit --extra-command 'mvn -B -pl governance -am clean test -Dtest=GovernanceProtectedPersistenceServiceTest,TraceabilitySchemaMappingTest,GovernanceAuditTrailServiceTest -Dsurefire.failIfNoSpecifiedTests=false' --extra-command 'python3 scripts/foreman.py compile-governance --check'; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; rg -n 'CONSTRAINT fk_|FOREIGN KEY' sql/init-schema.sql sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql governance/src/test/java/com/company/governance/infrastructure/persistence/TraceabilitySchemaMappingTest.java
  - Residual risk: The repository-side schema contract is now aligned with R-169, but already published databases still require the new drop-foreign-key migration to be executed, and the Win10 test-environment deployment still depends on human confirmation of Hetu mode, yml layout, auth path, and whether live evidence commands should be issued next.
  - Next step: Confirm the deployment checklist options from the updated Hetu/MRS runbook, apply the new migration in the target test database, start governance and query-execution from IDEA with the chosen Win10 yml profile, and then decide whether to request a second-step live evidence command set for JDBC/REST/CLIENT.

### HARN-015: Add no-foreign-key rule for MySQL/TDSQL

- Status: done
- Completed at: 2026-04-23
- Commit subject: `docs(rules): add no-foreign-key mysql policy`
- Priority: 1
- Depends on: N/A
- Scope: 将 MySQL/TDSQL 禁止外键约束 追加为正式仓库规则，并同步到相关持久化/实现文档与治理审计链。
- Validation:
  - `python3 scripts/foreman.py validate HARN-015`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: appended `R-169` to the rule ledger, synchronized the human constraint history, and updated the persistence baseline from “foreign-key relationships” to “reference-key relationships” for MySQL / TDSQL.
  - 2026-04-23: documented that legacy schema-level foreign keys remain a historical implementation drift to be removed by a dedicated future schema-governance task, while prohibiting any new foreign-key expansion immediately.
- Context closeout:
  - Completed scope: Added the new repository rule R-169 to prohibit physical foreign-key constraints on MySQL/TDSQL, synchronized the human constraint history, and updated the persistence baseline so relationship modeling now uses reference keys, indexes, and application-level integrity instead of foreign keys; also documented that existing schema-level foreign keys are legacy drift requiring a dedicated cleanup task rather than a pattern to continue.
  - Validation evidence: python3 scripts/foreman.py validate HARN-015 --include-task-audit; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py compile-governance --check
  - Residual risk: The rule and authority documents are now aligned, but sql/init-schema.sql and historical migrations still contain legacy foreign-key constraints from before R-169. A dedicated schema-governance task is still required to remove those constraints from implemented DDL.
  - Next step: Open a focused schema-governance follow-up to remove legacy foreign-key constraints from sql/init-schema.sql and matching migrations, replacing them with indexes and application-level integrity checks while preserving traceability and audit semantics.

### HARN-014: Capture HARN-013 post-closeout governance tail

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): capture HARN-013 post-closeout tail`
- Priority: 1
- Depends on: N/A
- Scope: 把 HARN-013 closeout 后遗留的 .codex/policy/authority-map.json 同步件与 docs/quality/validation-log.md post-closeout 审计尾项纳入正常审计链，不扩展业务或部署文档范围。
- Validation:
  - `python3 scripts/foreman.py validate HARN-014`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Captured the HARN-013 post-closeout governance tail by staging the synced .codex policy authority map and append-only validation-log entries produced after the deployment-runbook closeout, without changing the completed deployment documentation scope or any business implementation.
  - Validation evidence: python3 scripts/foreman.py validate HARN-014 --include-task-audit --extra-command 'python3 scripts/foreman.py compile-governance --check'; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: The repository audit chain is clean again, but real Hetu/MRS runtime success still depends on external environment deployment, credentials, reachable orders data, and environment-owned evidence retention.
  - Next step: Use the deployed runbook from HARN-013 to stand up governance and query-execution in the test environment, run bash scripts/run-hetu-env-smoke.sh for JDBC/REST/CLIENT evidence, and archive the returned log and response proof outside the repository.

### HARN-013: Document Hetu/MRS test-environment deployment runbook

- Status: done
- Completed at: 2026-04-23
- Commit subject: `docs(deploy): add hetu test-environment runbook`
- Priority: 1
- Depends on: N/A
- Scope: 新增真实 Hetu/MRS 测试环境部署与取证 runbook，覆盖 governance/query-execution 部署清单、配置项、启动顺序、scripts/run-hetu-env-smoke.sh 执行方法、JDBC/REST/CLIENT 证据留档要求，并同步现有部署入口文档。
- Validation:
  - `python3 scripts/foreman.py validate HARN-013`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added `docs/deployments/hetu-test-environment-deployment-runbook.md` to document the minimal governance/query-execution deployment path, exact test-environment inputs, mode-specific Hetu configuration, smoke execution, and evidence retention workflow for real Hetu/MRS validation.
  - 2026-04-23: linked the new runbook from docs deployment entrypoints and registered it in `document-coverage-matrix.md` so repository knowledge lint and deployment truth stay aligned.
- Context closeout:
  - Completed scope: Added a dedicated Hetu/MRS test-environment deployment runbook for governance and query-execution, including prerequisites, build/package commands, database initialization, test-profile and prod-profile auth considerations, mode-specific JDBC/REST/CLIENT configuration, smoke execution, and evidence retention guidance; linked the runbook from deployment entrypoints and registered it in the document coverage matrix.
  - Validation evidence: python3 scripts/foreman.py validate HARN-013 --include-task-audit --extra-command 'bash scripts/run-hetu-env-smoke.sh --help'; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: The repository now contains an operator-ready deployment/runbook path, but real JDBC/REST/CLIENT success evidence still depends on an external Hetu/MRS environment with reachable endpoints, a queryable orders dataset, valid credentials, and environment-specific auth/network tuning.
  - Next step: Deploy governance and query-execution to the target test environment with the documented profile and Hetu mode settings, run bash scripts/run-hetu-env-smoke.sh against the real endpoint, and archive the returned SUCCESS/HETU/HETU_REAL_INTEGRATION evidence under your environment-owned evidence store.

### HARN-012: Reconcile D-TASK-017 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): reconcile D-TASK-017 post-closeout drift`
- Priority: 1
- Depends on: N/A
- Scope: 修正 D-TASK-017 closeout 后遗留的两类治理漂移：把 docs/plans/master-execution-plan.md 的 active-wave 指针从已完成的 D-TASK-017 挪走，并将 docs/quality/validation-log.md 中未纳入提交的 post-closeout 尾项重新纳入正常审计链；不改写 D-TASK-017 的历史完成结论，也不扩展业务实现范围。
- Validation:
  - `python3 scripts/foreman.py validate HARN-012`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: confirmed the only dirty-worktree tail after D-TASK-017 closeout was append-only validation evidence in docs/quality/validation-log.md, while docs/plans/master-execution-plan.md still pointed the active wave at an already completed mainline task.
  - 2026-04-23: updated the master execution plan so D-TASK-017 is treated as completed repository-side work and the remaining Hetu/MRS follow-up is explicitly downgraded to environment-backed evidence rather than an active mainline implementation task; recompiled governance policy files after the plan truth changed.
- Context closeout:
  - Completed scope: Aligned post-closeout governance state after D-TASK-017 by clearing the stale active-wave pointer in the master execution plan, capturing the previously uncommitted validation-log tail into a governed repair batch, and returning the repository audit chain to a clean state without rewriting D-TASK-017 historical conclusions.
  - Validation evidence: Validated with python3 scripts/foreman.py validate HARN-012 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "node scripts/lint-repository-knowledge.js"; python3 scripts/task_audit.py --check --phase pre-closeout; and python3 scripts/foreman.py compile-governance after the master-plan truth update.
  - Residual risk: The repository-side drift is closed, but real Hetu/MRS evidence remains environment-backed and still depends on external execution of scripts/run-hetu-env-smoke.sh plus environment-specific credentials and runtime tuning.
  - Next step: Use the now-clean repository state to coordinate external Hetu/MRS smoke execution and archive the returned JDBC/REST/CLIENT evidence; only open a new repo-side task if production-style Hetu parameter tuning or another governed follow-up is approved.

### D-TASK-017: 落实 `query-execution` 真实 Hetu 集成与 smoke 分层

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(query-execution): integrate real hetu execution chain`
- Priority: 1
- Depends on: `D-TASK-016`
- Scope: 让 HETU 主路径切到真实 JDBC/REST/CLIENT 接入，补齐 JDBC 驱动接线、Hetu client 协议执行、严格路由与错误语义，并提供 repo-closed runtime smoke 与 environment-backed Hetu/MRS smoke 入口 Tech: `JAVA-BE`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实 Hetu 集成重新退回 `SIMULATED` 冒充成功、放宽只读边界、绕过统一授权入口，或在未确认外部依赖时默认启用高风险生产参数，需人工确认
- Data impact: query-execution Hetu 连接配置、执行链路、审计记录、runtime/env smoke 证据
- Rollback / recovery: 恢复受控模式顺序、严格 HETU 路由失败语义、统一授权前置检查，并回退到上一版受控配置与文档说明
- Validation:
  - `query-execution 模块测试、跨模式适配测试、runtime smoke、Hetu env smoke 脚本/文档、task audit`
  - `python3 scripts/foreman.py validate D-TASK-017`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added real Hetu integration primitives in `query-execution`, including `io.hetu.core:hetu-jdbc` wiring, strict HETU route-unavailable semantics, Hetu client protocol execution, and updated controller/service/adapter coverage.
  - 2026-04-23: updated runtime/env smoke assets with a local mock Hetu coordinator, runtime smoke real-mode assertions, and a dedicated `scripts/run-hetu-env-smoke.sh` entry for external Hetu/MRS environments.
  - 2026-04-23: `mvn -B -pl query-execution -am test -DskipITs`, `bash scripts/run-runtime-smoke.sh --runtime-smoke`, `bash -n scripts/run-runtime-smoke.sh scripts/manual-query-governance-smoke.sh scripts/run-hetu-env-smoke.sh`, `python3 -m py_compile scripts/mock-hetu-server.py`, and `python3 scripts/foreman.py compile-governance` passed; `python3 scripts/foreman.py compile-governance --check` passed after recompiling `.codex/policy/authority-map.json`.
- Context closeout:
  - Completed scope: Promoted query-execution to the real Hetu integration stage by wiring the Hetu JDBC driver, enforcing strict HETU route-unavailable semantics instead of simulated success, implementing Hetu client-protocol execution, updating runtime/env smoke assets with a mock Hetu coordinator and external Hetu smoke entry, and syncing the affected architecture/deployment truth documents.
  - Validation evidence: Validated with python3 scripts/foreman.py validate D-TASK-017 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution -am test -DskipITs" --extra-command "bash scripts/run-runtime-smoke.sh --runtime-smoke" --extra-command "bash -n scripts/run-runtime-smoke.sh scripts/manual-query-governance-smoke.sh scripts/run-hetu-env-smoke.sh" --extra-command "python3 -m py_compile scripts/mock-hetu-server.py" --extra-command "bash scripts/run-hetu-env-smoke.sh --help"; runtime smoke proved query-execution returned HETU_REAL_INTEGRATION with CLIENT mode on the real Hetu path.
  - Residual risk: Repository-closed validation now covers real Hetu mode execution through the local mock coordinator and strict route semantics, but long-lived external Hetu/MRS evidence, deployment credentials, and production parameter calibration still depend on environment-backed execution of scripts/run-hetu-env-smoke.sh against a provisioned cluster.
  - Next step: Proceed to the next governed follow-up after the external environment owner captures Hetu/MRS smoke evidence with scripts/run-hetu-env-smoke.sh, or continue with downstream query-execution production tuning if Phase-D priorities still target Hetu operations hardening.

### D-TASK-016: 收口治理授权矩阵并下沉统一授权入口

- Status: done
- Completed at: 2026-04-23
- Commit subject: `feat(governance): unify authorization matrix enforcement`
- Priority: 1
- Depends on: `D-TASK-015`
- Scope: 在 governance 落地角色矩阵、资源模型、数据源授权矩阵，把真实授权决策下沉为 query/sql-optimization/benchmark 统一入口，并补齐授权成功/拒绝/跨租户/吊销后访问与权限变更审计 Tech: `JAVA-BE`,`SQL`,`OPS`,`DOCS`. Layer: `application(controller/service)/domain/infrastructure`,`deployments/ci/scripts`,`docs`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若角色矩阵、资源模型或数据源授权矩阵被放宽为 fail-open，或三服务重新分叉授权入口，需人工确认
- Data impact: governance 授权配置、跨服务授权决策、审计记录与 runtime smoke 证据
- Rollback / recovery: 恢复统一授权入口、默认拒绝语义、被吊销访问阻断，以及权限变更审计补录
- Validation:
  - `governance/三服务模块测试、runtime smoke、task audit、契约/安全文档同步`
  - `python3 scripts/foreman.py validate D-TASK-016`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Governance role/resource/datasource matrices are active, authorization now flows through a single decision entrypoint for query-execution, sql-optimization, and benchmark-engine, and smoke/test coverage now includes allow/deny/cross-tenant/revoked cases plus permission-change auditing.
  - Validation evidence: python3 scripts/foreman.py compile-governance --check; mvn -B -pl governance,query-execution,sql-optimization,benchmark-engine -am -DskipITs test; bash scripts/run-runtime-smoke.sh --runtime-smoke; python3 scripts/foreman.py validate D-TASK-016 --include-task-audit --extra-command ...
  - Residual risk: Authorization matrices are still config-backed with runtime mutation held in-process; distributed persistence and external IAM synchronization remain future work.
  - Next step: Proceed with the next priority: real Hetu integration in query-execution while keeping the unified governance authorization entrypoint unchanged.

### F-TASK-033: 补齐测试环境最小 smoke 门禁

- Status: done
- Completed at: 2026-04-23
- Commit subject: `ops(smoke): add minimal test-environment smoke gate`
- Priority: 1
- Depends on: `F-TASK-032`
- Scope: 提供环境无关的最小 smoke 入口，供外部测试环境 CI/CD 在部署后执行四个后端 health、前端可达性、query/sql-optimization/benchmark 到 governance 的最小业务链路，以及受保护请求头有效性验证；本地 runtime smoke 契约保持不变 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若要把测试环境 minimal smoke 升级为仓库 repo-closed 主路径替代项、重新引入对测试环境内部 DB/容器的强绑定，或要求外部环境 owner 接受新的破坏式认证/访问前提，需人工确认
- Data impact: 环境无关 smoke 脚本、外部测试环境 CI/CD 接入方式、health/API 断言语义与部署文档真值
- Rollback / recovery: 保留新增入口为 environment-backed 部署后验证层，回退对外部环境的强绑定假设，并继续维持本地 runtime smoke 作为 repo-closed 主路径
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-033`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`bash scripts/run-env-smoke.sh --help`、`bash scripts/run-env-smoke.sh --check-config`、`bash scripts/run-runtime-smoke.sh --compose-check`、`node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py validate F-TASK-033`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added an environment-agnostic minimal smoke entrypoint for external test-environment CI/CD, introduced shared smoke HTTP/JSON helpers reused by the existing manual governance smokes, and synchronized plan/truth/deployment/local-development docs so test-environment minimal smoke is explicitly separated from repo-closed runtime smoke and Sonar/Kafka fallback semantics; task numbering was intentionally advanced to F-TASK-033 because historical F-TASK-032 already exists and could not be rewritten.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-033; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; bash scripts/run-runtime-smoke.sh --compose-check; bash scripts/run-env-smoke.sh --help; bash scripts/run-env-smoke.sh --check-config; FRONTEND_BASE_URL=http://localhost:3001 bash scripts/run-env-smoke.sh; bash -n scripts/smoke-lib.sh scripts/run-env-smoke.sh scripts/manual-query-governance-smoke.sh scripts/manual-sql-optimization-governance-smoke.sh scripts/manual-benchmark-governance-smoke.sh
  - Residual risk: The repository now provides the minimal test-environment smoke entrypoint, but the external test environment still has an independent owner and no Codex runtime; it only gains a real deployment-after-smoke closure once that owner wires scripts/run-env-smoke.sh into its CI/CD and preserves execution evidence.
  - Next step: Have the external test-environment owner call bash scripts/run-env-smoke.sh after deployment and archive the resulting logs/status as environment-backed smoke evidence; no repository-side mainline task remains after this closeout.

### HARN-011: Reconcile F-TASK-032 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): reconcile F-TASK-032 post-closeout drift`
- Priority: 1
- Depends on: N/A
- Scope: Clear the stale active-wave pointer to F-TASK-032 in docs/plans/master-execution-plan.md and mark F-TASK-032 as completed in docs/deployments/phase-gate-baseline.md without rewriting any historical task conclusions.
- Validation:
  - `python3 scripts/foreman.py validate HARN-011`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned post-closeout governance state after F-TASK-032 by clearing the stale active-wave pointer in the master execution plan, marking F-TASK-032 as completed in the phase-gate follow-up mapping, and recompiling the authority-map policy to match the updated plan truth.
  - Validation evidence: python3 scripts/foreman.py compile-governance; python3 scripts/foreman.py validate HARN-011; python3 scripts/task_audit.py --check --phase pre-closeout
  - Residual risk: F-TASK-032 remains complete; only future human-directed Sonar environment restoration in INBOX-001 could change the current default gate semantics.
  - Next step: No repository-side follow-up remains for the Sonar fallback governance round unless humans choose to restore mandatory Sonar or stronger environment-bound release gating.

### F-TASK-032: 去除 Sonar fallback 的隐性自动恢复接线，并分离 provisioning / enable 语义

- Status: done
- Completed at: 2026-04-23
- Commit subject: `ci(gates): separate sonar provisioning from enable semantics`
- Priority: 1
- Depends on: `F-TASK-031`
- Scope: 去除 release workflow 默认 `quality-gate` environment 绑定，给主 CI 增加显式 Sonar enable 条件，并同步 Sonar runbook、INBOX 与部署基线，使 provisioning 不再等于自动恢复强制 Sonar Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若要把“环境已 provision”重新视为“默认自动启用 Sonar 强制门禁”，或恢复 release workflow 的环境级默认绑定，需人工确认
- Data impact: CI/release workflow 触发条件、Sonar enable flag、环境恢复 runbook、INBOX 语义与部署基线
- Rollback / recovery: 恢复当前显式 enable 语义，保留 provisioning 证据与恢复入口；如要再次升级为默认强制，需拆新任务追加治理记录
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-032`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`bash scripts/run-sonar.sh`、`bash scripts/run-sonar.sh --require-config`、`node scripts/lint-repository-knowledge.js`、workflow / docs 语义核对
  - `python3 scripts/foreman.py validate F-TASK-032`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed the default release quality-gate environment binding, required explicit SONAR_ENABLE_DEFAULT enablement before CI/release workflows consume provisioned Sonar config, and synchronized the Sonar runbook, INBOX, plan, and deployment baselines around provisioning-versus-enable semantics.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-032; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/run-sonar.sh; bash scripts/run-sonar.sh --require-config (expected failure without SONAR_HOST_URL/SONAR_TOKEN); node scripts/lint-repository-knowledge.js
  - Residual risk: Restoring Sonar as a default hard gate still requires human-controlled provisioning plus explicit enablement or a new follow-up task; external test-environment CI/CD still lacks repo-equivalent smoke and cannot replace repo-closed gates.
  - Next step: No repository-side follow-up remains unless humans decide to restore mandatory Sonar or reintroduce stronger environment-bound release gating.

### HARN-010: Reconcile F-TASK-031 post-closeout drift

- Status: done
- Completed at: 2026-04-23
- Commit subject: `fix(governance): reconcile F-TASK-031 post-closeout drift`
- Priority: 1
- Depends on: F-TASK-031
- Scope: 对齐 F-TASK-031 closeout 后遗留的 active-wave 指针、phase-gate follow-up 描述与 validation-log commit hash 证据，不改写 F-TASK-031 历史完成结论。
- Validation:
  - `python3 scripts/foreman.py validate HARN-010`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned post-closeout governance state after F-TASK-031 by clearing stale active-wave/follow-up pointers and repairing runtime validation so it no longer hardcodes an already-archived task id.
  - Validation evidence: python3 -m py_compile scripts/validate_codex_runtime.py; python3 scripts/foreman.py validate HARN-010; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js
  - Residual risk: F-TASK-031 remains complete; only future human-directed environment restoration work in INBOX-001 could change default Sonar semantics.
  - Next step: No repository-side follow-up remains for F-TASK-031; only act again if humans choose to restore mandatory Sonar or other environment-backed gates.

### F-TASK-031: 将 Sonar 与环境级门禁降级为 fallback，并建立双层门禁语义

- Status: done
- Completed at: 2026-04-23
- Commit subject: `ci(gates): downgrade sonar and env gates to fallback`
- Priority: 1
- Depends on: `F-TASK-030`
- Scope: 把 repo-closed 主路径与 environment-backed 增强项显式拆层，修正 Sonar / 真实 Kafka / release gate 默认语义，同时保留独立脚本与 workflow 作为 fallback 入口 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若要把 Sonar 或真实 Kafka 再次恢复为仓库默认硬阻断，或改变 repo-closed / environment-backed 双层边界，需人工确认
- Data impact: phase gate/release gate workflow、脚本默认值、门禁文档口径、INBOX 环境恢复项
- Rollback / recovery: 恢复 fallback 语义、保留环境恢复 runbook 与 INBOX 追踪，必要时再拆独立任务重新升级为强制门禁
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-031`、`bash scripts/run-phase-gates.sh --gate entry`、`bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase1plus`、`bash scripts/run-phase-gates.sh --gate compliance`、`bash scripts/run-sonar.sh`、`bash scripts/run-sonar.sh --require-config`、`node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py validate F-TASK-031`
- Progress log:
  - 2026-04-23: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reframed repository governance around repo-closed primary gates plus environment-backed fallback gates, updated R-117/docs/workflows/scripts/INBOX semantics, and preserved Sonar/real Kafka entrypoints without default blocking.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-031; python3 scripts/task_audit.py --check --phase pre-closeout; bash scripts/run-phase-gates.sh --gate entry; bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase1plus; bash scripts/run-phase-gates.sh --gate compliance; bash scripts/run-sonar.sh; bash scripts/run-sonar.sh --require-config (expected fallback failure without config); node scripts/lint-repository-knowledge.js
  - Residual risk: Restoring Sonar or real Kafka as default hard gates still requires explicit environment provisioning and a new follow-up task; external test environment CI/CD still lacks repo-equivalent smoke and cannot replace repo-closed gates.
  - Next step: Only if humans want to restore mandatory Sonar gating, use INBOX-001 to provision quality-gate or repo-level Sonar secrets and create a new upgrade task; otherwise current repo-closed semantics are complete.

### F-TASK-030: 提升覆盖率并补齐 Sonar 发布环境

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ci(sonar): uplift coverage and wire release gate config`
- Priority: 1
- Depends on: `F-TASK-029`
- Scope: 把 phase1plus 聚合覆盖率提升到 85%+，补齐 Sonar 所需 secrets / 发布环境接线，并验证自动 release gate 可稳定放行 Tech: `OPS`,`DOCS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若 coverage 提升方案会删除既有测试、放宽 85% 门槛，或 Sonar 发布环境接线涉及敏感 secrets 管理策略调整需人工确认
- Data impact: 覆盖率结果、CI/release 环境变量、Sonar 扫描结果与相关测试资产
- Rollback / recovery: 恢复到当前自动阻断发布路径，保留覆盖率 / Sonar 失败证据，并回退新增测试或 workflow 环境接线
- Next action: 在 GitHub Settings 中为正式发布链补齐 `quality-gate` environment 或仓库级 Sonar secrets / vars，然后重新触发 `Phase Gate` `delivery|full` 或 `Release Phase Gate`
- Escalation: 需要具备仓库 Settings 权限的人类完成外部 `SONAR_HOST_URL`、`SONAR_TOKEN` 与可选 `SONAR_PROJECT_KEY` / `SONAR_PROJECT_NAME` / `SONAR_QUALITY_GATE_WAIT` 配置
- Human decision: 确认正式发布链是否统一以 `quality-gate` environment 作为 Sonar 配置源，并完成外部 secrets / vars provisioning
- INBOX ref: INBOX-001
- Validation:
  - `bash scripts/run-coverage.sh --phase phase1plus`、`bash scripts/run-sonar.sh --require-config`、release gate workflow / phase gate 验证
  - `python3 scripts/foreman.py validate F-TASK-030`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-23: added high-leverage repository / governance client tests across `benchmark-engine`、`query-execution`、`sql-optimization`; `bash scripts/run-coverage.sh --phase phase1plus` now passes at `86.9763% (5356/6158)`.
  - 2026-04-23: wired `Phase Gate` Sonar environment injection, bound `Release Phase Gate` to GitHub Actions environment `quality-gate`, added `docs/deployments/sonar-quality-gate-provisioning.md`, and updated deployment / truth docs.
  - 2026-04-23: `python3 scripts/foreman.py validate F-TASK-030` passed; remaining blocker is external GitHub Settings provisioning captured in `INBOX-001`.
- Context closeout:
  - Completed scope: Raised repository phase1plus aggregated line coverage to 86.9763% with targeted benchmark-engine/query-execution/sql-optimization tests, wired Sonar configuration through CI, Phase Gate, and Release Phase Gate workflow paths, added the Sonar provisioning runbook, and synchronized the deployment/truth/plan documents to the repository-side release-gate baseline.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-030; python3 scripts/task_audit.py --check; mvn -B test; bash scripts/run-coverage.sh --phase phase1plus; bash scripts/run-runtime-smoke.sh --runtime-smoke
  - Residual risk: End-to-end Sonar-required release validation still depends on external GitHub Settings provisioning of SONAR_HOST_URL/SONAR_TOKEN and optional quality-gate vars or environment, which remain outside the repository and were intentionally not faked in local development.
  - Next step: When GitHub Settings access and a real Sonar backend are available, provision the required Sonar secrets/vars, rerun bash scripts/run-sonar.sh --require-config plus the Phase Gate delivery/full path and Release Phase Gate, and then close the remaining external provisioning follow-up.

### OPS-GOV-002: 新增后端四服务一键启动脚本

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ops(local): add backend four-service startup script`
- Priority: 2
- Depends on: N/A
- Scope: scripts + local backend startup docs
- Validation:
  - `python3 scripts/foreman.py validate OPS-GOV-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added scripts/start-backend-services.sh to bootstrap the local dependency stack, optionally build backend modules, inject dev crypto key defaults, start governance/query-execution/sql-optimization/benchmark-engine, and wait for the 8080-8083 health endpoints; updated local setup and operations docs so the backend-only startup path, options, log directory, and health checks match repository truth.
  - Validation evidence: python3 scripts/foreman.py validate OPS-GOV-002
  - Residual risk: The helper depends on local Docker, Maven, Java, and free 8080-8083 ports; startup still fails fast when prerequisite services or pid files are already present, which is expected for local runtime safety.
  - Next step: Use bash scripts/start-backend-services.sh for backend-only local bring-up, and extend the runtime orchestration only if future tasks need tighter frontend/start-stop integration.

### F-TASK-029: 收口 release automation 与门禁稳定性

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ci(release): automate release phase gate and stabilize coverage entry`
- Priority: 1
- Depends on: `F-TASK-027`,`F-TASK-028`
- Scope: 稳定 coverage 入口、明确 Sonar 强制约束、并把 phase gate 绑定到 release metadata 自动触发链 Tech: `OPS`,`DOCS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`docs`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-F / Story `F-STORY-005` 发布门禁自动化与稳定性收口
- Human confirmation point: 若 release automation 会改变 delivery tag / write-back 语义、放宽 Sonar 必需约束或把 phase gate 自动触发绑定到错误发布事件需人工确认
- Data impact: workflow、release metadata、coverage / Sonar 门禁结果与相关测试稳定性
- Rollback / recovery: 恢复手工 phase gate 入口、保留自动化元数据证据，并回退到上一个可追溯发布路径
- Validation:
  - `release gate workflow、`bash scripts/run-coverage.sh --phase phase1plus`、Sonar-required gate 路径与 CI/workflow 验证`
  - `python3 scripts/foreman.py validate F-TASK-029`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Closed F-TASK-029 by stabilizing the benchmark-engine test context so phase1plus coverage can run deterministically again, adding an automated Release Phase Gate workflow that binds full phase-gate execution to checkpoint tag and release metadata, and updating the CI/phase-gate/document-truth/master-plan corpus so release automation, coverage blocking semantics, and Sonar-required behavior are recorded against current repository fact instead of manual follow-up notes.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-029 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test -DskipITs" --extra-command "python3 - <<\"PY\"\nimport subprocess\nimport sys\nproc = subprocess.run([\"bash\", \"scripts/run-coverage.sh\", \"--phase\", \"phase1plus\"], text=True, capture_output=True)\noutput = proc.stdout + proc.stderr\nsys.stdout.write(output)\nif proc.returncode == 2 and \"Coverage threshold not met.\" in output and \"Required minimum line coverage: 85.00%\" in output:\n    sys.exit(0)\nprint(\"Expected phase1plus coverage gate to fail with threshold evidence.\", file=sys.stderr)\nsys.exit(1)\nPY" --extra-command "python3 - <<\"PY\"\nimport subprocess\nimport sys\nproc = subprocess.run([\"bash\", \"scripts/run-sonar.sh\", \"--require-config\"], text=True, capture_output=True)\noutput = proc.stdout + proc.stderr\nsys.stdout.write(output)\nif proc.returncode != 0 and \"Missing required Sonar configuration\" in output:\n    sys.exit(0)\nprint(\"Expected sonar gate to fail fast when required configuration is missing.\", file=sys.stderr)\nsys.exit(1)\nPY" --extra-command "rg -n \"Release Phase Gate|checkpoint/\\\\*\\\\*|release-phase-gate-metadata|--gate full --coverage-phase phase1plus --require-sonar --run-real-kafka-gate|76\\.4047%|release\\.published|coverage uplift / Sonar secrets provisioning\" .github/workflows/release-phase-gate.yml docs/deployments/ci-capability-baseline.md docs/deployments/phase-gate-baseline.md docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The release automation path is now explicit and automatically blocking on checkpoint tags/releases, but repository-wide phase1plus coverage is still only 76.4047% versus the required 85%, and Sonar still depends on externally provisioned SONAR_HOST_URL/SONAR_TOKEN secrets before the automated release gate can pass end to end.
  - Next step: Instantiate the next follow-up task to raise repository coverage to the phase1plus threshold and provision Sonar secrets/CI environment so the automated release phase gate can move from deterministic blocker to stable pass path.

### F-TASK-003: 补齐环境提醒与恢复指引

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(deploy): reconcile environment and recovery guidance`
- Priority: 1
- Depends on: `F-TASK-002`
- Scope: 输出独立 MySQL/Kafka/恢复提醒 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-001` 部署文档与编排
- Human confirmation point: RPO/RTO 或环境提醒口径改变需人工确认
- Data impact: 运维文档、恢复指引
- Rollback / recovery: 追加更正提醒
- Validation:
  - 文档与规则一致
  - `python3 scripts/foreman.py validate F-TASK-003`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled F-TASK-003 against repository truth by updating operator-facing deployment guidance so local setup explicitly warns that local docker compose services do not replace production independent MySQL/TDSQL, Kafka, and formal recovery arrangements; linked the formal backup/recovery baseline into the primary operator path; and aligned the master execution plan plus document truth baseline with the current environment-reminder and recovery-guidance facts.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-003 --include-task-audit --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "rg -n \"本地 .*不得替代生产独立环境|backup-recovery-baseline|RPO/RTO|verify_kafka_runtime_config|run-kafka-runtime-gate|独立 MySQL/TDSQL|恢复责任人\" docs/deployments/local-setup.md docs/deployments/huawei-cloud-setup.md docs/deployments/backup-recovery-baseline.md docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: Phase-F environment and recovery reminders are now aligned at the documentation layer, but formal production recovery automation, object-storage restore scripts, and deeper release-trigger hardening remain later-phase concerns outside this 1-9 reconciliation batch.
  - Next step: Report completion of the requested 1-9 sequence, then propose the next highest-value task beyond this batch only if the user asks for further execution.

### F-TASK-002: 对齐 compose 与脚本说明

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(deploy): reconcile compose and script guidance`
- Priority: 1
- Depends on: `F-TASK-001`
- Scope: 对齐本地/离线/可选 Kafka 说明 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-001` 部署文档与编排
- Human confirmation point: compose 语义破坏式变化需人工确认
- Data impact: 编排配置、脚本入口
- Rollback / recovery: 恢复旧 compose 语义并补兼容脚本
- Validation:
  - `docker compose config`、文档一致性
  - `python3 scripts/foreman.py validate F-TASK-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled F-TASK-002 against repository truth by aligning local and offline deployment docs with the actual docker-compose files and local-start/local-stop flow: documented that default local startup remains DATABASE-mode without automatically starting Kafka, clarified docker compose versus docker-compose fallback usage, and corrected the frontend proxy troubleshooting section to match the real 8080/8081/8082/8083 backend split.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-002 --include-task-audit --extra-command "docker-compose config" --extra-command "rg -n \"optional profile|R-144 DATABASE|localhost:8080|8081|8082|8083|docker compose up -d|docker-compose\" docs/deployments/local-setup.md docs/deployments/offline-setup.md docs/plans/master-execution-plan.md docker-compose.yml"
  - Residual risk: Compose and script guidance now matches repository truth, but F-TASK-003 still needs to close out the environment reminders and recovery-guidance truth so deployment docs, backup baselines, and operator warnings all read consistently.
  - Next step: Instantiate F-TASK-003 next and reconcile environment reminders, backup/recovery guidance, and remaining operator-facing deployment caveats against the current repository documents.

### F-TASK-001: 补齐华为云部署文档

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(deploy): reconcile huawei cloud deployment truth`
- Priority: 1
- Depends on: `B-TASK-005`
- Scope: 完整描述华为云部署拓扑和生产切换 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-001` 部署文档与编排
- Human confirmation point: 生产部署拓扑调整需人工确认
- Data impact: 部署文档和配置模板
- Rollback / recovery: 追加修正文档并恢复旧拓扑说明
- Validation:
  - 文档存在、入口索引、语义检查
  - `python3 scripts/foreman.py validate F-TASK-001`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled F-TASK-001 against repository truth by recording that the Huawei Cloud deployment document already exists, is linked from the authority stack, and covers the 4-service private-cloud topology, KAFKA production messaging mode, independent frontend/backend deployment, and backup-recovery cross references; this task is now an archive/truth-closeout rather than a missing-document implementation.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-001 --include-task-audit --extra-command "node scripts/lint-repository-knowledge.js" --extra-command "rg -n \"华为云|4 个微服务|KAFKA|backup-recovery-baseline|local-setup|offline-setup\" docs/deployments/huawei-cloud-setup.md docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The Huawei Cloud deployment truth is now aligned, but F-TASK-002 still needs to reconcile local/offline compose and script guidance where minor wording drift remains, and F-TASK-003 must finish the environment reminder and recovery-guidance truth closeout.
  - Next step: Instantiate F-TASK-002 next and align compose plus script documentation with the actual docker-compose files, local-start/local-stop flows, Kafka profile usage, and frontend proxy wording.

### E-TASK-006: 深色设计系统组件化

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): reconcile design system implementation truth`
- Priority: 1
- Depends on: `E-TASK-004`
- Scope: 把主题 token 和组件规则转成实现 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-002` 业务页面拆分
- Human confirmation point: 设计系统 token 删除需人工确认
- Data impact: 前端主题变量
- Rollback / recovery: 恢复原 token 映射
- Validation:
  - `token 生效、build、lint`
  - `python3 scripts/foreman.py validate E-TASK-006`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled E-TASK-006 against repository truth by recording that the current frontend already implements the SQLForge design-system token layer in src/styles/element-plus-theme.css, applies a dark-first theme through the global store and App shell, and reuses shared token semantics across dashboard, business, and governance pages; this task is therefore archived as truth closeout rather than a net-new componentization effort.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-006 --include-task-audit --extra-command "npm run lint" --extra-command "npm run build" --extra-command "rg -n \"element-plus-theme\\.css|sqlforge-color-brand|sqlforge-font-sans|sqlforge-font-mono|toggleTheme|theme: 'dark'|sqlforge-code-label|sqlforge-section-title|route-card\" src/styles/element-plus-theme.css src/stores/index.js src/App.vue src/views/dashboard/DashboardView.vue src/views/query/SqlQueryView.vue src/views/optimization/AccelerationView.vue src/views/benchmark/BenchmarkView.vue src/views/system/SystemView.vue docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: Phase-E truth is now substantially aligned for route shells, live capability consumption, and tokenized theme implementation, but the remaining queue shifts to Phase-F documentation reconciliation so deployment and recovery artifacts match the already-landed repository facts with the same strictness.
  - Next step: Instantiate F-TASK-001 next and reconcile the existing Huawei Cloud deployment documentation against repository truth before closing out F-TASK-002 and F-TASK-003.

### E-TASK-005: 接入已存在治理接口能力

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): reconcile governed page capability truth`
- Priority: 1
- Depends on: `E-TASK-004`,`Phase-C`
- Scope: 页面消费已交付后端能力 Tech: `VUE-FE`,`JAVA-BE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-002` 业务页面拆分
- Human confirmation point: API 契约破坏式变化需人工确认
- Data impact: 前端接口调用、缓存态
- Rollback / recovery: 恢复旧 API 适配层或 mock 路径
- Validation:
  - `UI/API 契约、可见性`
  - `python3 scripts/foreman.py validate E-TASK-005`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled E-TASK-005 against repository truth by recording that the current business and governance pages already consume delivered backend capabilities through src/services/runtimeGateApi.js, including query execution, optimization, benchmark, tenant config, message retry/stats, and governance history lookup/detail APIs; updated the master plan active wave so this task is treated as truth closeout rather than new feature work.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-005 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"executeQuery|submitOptimizationTask|submitBenchmarkTask|getBenchmarkReport|getGovernanceTenantConfig|getGovernanceMessageStats|retryGovernanceFailedMessages|getGovernanceTraceSummaries|lookupGovernanceTraces|getGovernanceTraceDetail\" src/services/runtimeGateApi.js src/views/query/SqlQueryView.vue src/views/optimization/AccelerationView.vue src/views/benchmark/BenchmarkView.vue src/views/system/SystemView.vue src/views/parse-record/ParseRecordView.vue src/views/repair-evidence/RepairEvidenceView.vue src/views/audit-forensics/AuditForensicsView.vue src/views/audit-troubleshooting/AuditTroubleshootingView.vue docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The frontend capability truth is now aligned, but E-TASK-006 still needs to reconcile design-system and token implementation coverage so Phase-E does not overstate how much of the visual system has been fully archived against current source reality.
  - Next step: Instantiate E-TASK-006 next and reconcile the current design-system/token implementation truth, then continue into the Phase-F documentation reconciliation batch F-TASK-001~003.

### E-TASK-004: 建立五大业务页路由骨架

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): reconcile business route shell truth`
- Priority: 1
- Depends on: `E-TASK-001`
- Scope: 建立 5 个独立业务路由 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-002` 业务页面拆分
- Human confirmation point: 五大业务页若改为合并页需人工确认
- Data impact: 前端路由结构
- Rollback / recovery: 恢复独立路由和页面壳层
- Validation:
  - 每页独立入口、build
  - `python3 scripts/foreman.py validate E-TASK-004`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Reconciled E-TASK-004 against repository truth by updating the master execution plan active wave and the document truth baseline to reflect that the dashboard, core business routes, and governance history/ops route shells already exist in the current frontend, so this task is now an archive/truth-closeout rather than a fresh implementation batch.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-004 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"ROUTE_PATHS\\.sqlQuery|ROUTE_PATHS\\.acceleration|ROUTE_PATHS\\.benchmark|ROUTE_PATHS\\.system|ROUTE_PATHS\\.parseRecord|ROUTE_PATHS\\.repairEvidence|ROUTE_PATHS\\.auditForensics|ROUTE_PATHS\\.auditTroubleshooting|ROUTE_PATHS\\.runtimeGates|ROUTE_PATHS\\.recoveryDrill\" src/router/index.js src/config/routePaths.mjs docs/plans/document-truth-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: The route shell truth is now aligned, but E-TASK-005 and E-TASK-006 still need to reconcile live governance capability consumption and design-system/token coverage so the frontend archive state matches the repository facts end to end.
  - Next step: Instantiate E-TASK-005 next and reconcile which already-delivered governance capabilities are actually consumed by the current business and governance pages, without re-implementing the route shells.

### E-TASK-008: 清理潜在越界逻辑

- Status: done
- Completed at: 2026-04-22
- Commit subject: `refactor(frontend): move protected headers to dev proxy`
- Priority: 1
- Depends on: `E-TASK-007`
- Scope: 清理前端中的权威业务判断 Tech: `VUE-FE`,`JAVA-BE`. Layer: `frontend/router/views/styles`,`application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 若将权威逻辑重新放回前端需人工确认
- Data impact: 前端状态和判断逻辑
- Rollback / recovery: 恢复后端权威边界并移除越界逻辑
- Validation:
  - 边界抽查、build、lint
  - `python3 scripts/foreman.py validate E-TASK-008`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Removed frontend-owned protected authentication header construction from src/services/runtimeGateApi.js, replaced it with dev-only proxy hint headers, taught vite.config.js to inject protected request context at the Vite proxy boundary for local integration, updated the separation baseline document, and advanced the master execution plan active wave to E-TASK-008 while preserving the Phase-E boundary-governance sequence.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-008 --include-task-audit --extra-command "node scripts/check-frontend-backend-separation.js" --extra-command "npm run lint" --extra-command "npm run build" --extra-command "rg -n \"X-SQLForge-Dev-|createProtectedApiProxy|X-Tenant-Id|X-User-Id|X-Role-Codes|X-Request-Id|X-Trace-Id|X-Auth-Source|X-Issued-At|X-Expires-At\" src/services/runtimeGateApi.js vite.config.js docs/quality/frontend-backend-separation-baseline.md docs/plans/master-execution-plan.md"
  - Residual risk: Protected request-context synthesis is no longer in frontend business code, but development still depends on Vite proxy injection for local auth simulation; the next reconciliation batch must align partially consumed E-TASK-004~006 and F-TASK-001~003 with repository truth without re-implementing already landed capabilities.
  - Next step: Instantiate the first reconciliation task for the partially consumed E-TASK-004~006 and F-TASK-001~003 batch, starting with the highest-value Phase-E archive/truth alignment item before proceeding through the remaining repository-truth closeout tasks.

### E-TASK-007: 扩展分离检查清单

- Status: done
- Completed at: 2026-04-22
- Commit subject: `chore(ci): strengthen frontend backend separation checks`
- Priority: 1
- Depends on: `Phase-C`
- Scope: 强化前后端分离校验 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-E / Story `E-STORY-003` 前后端分离持续治理
- Human confirmation point: 分离检查口径放宽需人工确认
- Data impact: 脚本规则，无业务数据
- Rollback / recovery: 恢复严格检查项
- Validation:
  - 分离检查脚本通过
  - `python3 scripts/foreman.py validate E-TASK-007`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the frontend/backend separation gate to auto-discover every Maven backend module, added runtime and authority-boundary detections for protected frontend header construction and hard-coded backend hosts, tightened noisy false-positive heuristics, updated the separation baseline document, and switched the master execution plan active wave to the Phase-E boundary-governance mainline with E-TASK-007 as the current work item.
  - Validation evidence: python3 scripts/foreman.py validate E-TASK-007 --include-task-audit --extra-command "node scripts/check-frontend-backend-separation.js" --extra-command "rg -n \"discoverBackendRoots|X-Tenant-Id|sql-optimization|benchmark-engine|Phase-E / E-STORY-003|E-TASK-007\" scripts/check-frontend-backend-separation.js docs/quality/frontend-backend-separation-baseline.md docs/plans/master-execution-plan.md tasks.md"
  - Residual risk: The separation gate now surfaces the highest-value boundary drift, but src/services/runtimeGateApi.js still constructs protected request headers as a temporary frontend-owned smoke helper; that warning remains intentional until E-TASK-008 removes or rehomes the logic.
  - Next step: Instantiate E-TASK-008 next and remove the frontend-owned protected request header logic flagged by the strengthened separation gate, then continue to reconciliation tasks for partially consumed E-TASK-004~006 and F-TASK-001~003.

### A-TASK-012: 抽取 shared 认证与治理客户端支撑

- Status: done
- Completed at: 2026-04-22
- Commit subject: `refactor(shared): extract auth and governance client support`
- Priority: 1
- Depends on: A-TASK-011
- Scope: 把 query-execution、sql-optimization、benchmark-engine 重复的认证请求元数据与治理内部客户端支撑下沉到 sqlforge-shared，消除跨服务漂移并补 R-126 验证。
- Validation:
  - `python3 scripts/foreman.py validate A-TASK-012`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extracted shared request-metadata context, header-auth establishment, and protected governance request helpers into sqlforge-shared; rewired query-execution, sql-optimization, and benchmark-engine interceptors plus governance clients to consume the shared support; removed duplicated per-service RequestMetadataContext implementations; updated governance regression tests; and aligned the master plan active wave plus task matrices for A-TASK-012.
  - Validation evidence: python3 scripts/foreman.py validate A-TASK-012 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution,sql-optimization,benchmark-engine -am test -DskipITs"
  - Residual risk: The shared extraction currently covers header-based auth context and governance internal client support only; wider shared cleanup across other duplicated service contracts remains pending, and the next mainline still needs frontend/backend separation enforcement plus repository-truth reconciliation for partially consumed E/F tasks.
  - Next step: Resume the recommended mainline by instantiating E-TASK-007 to strengthen frontend/backend separation checks, then E-TASK-008 and the remaining reconciliation tasks for partially consumed E-TASK-004~006 and F-TASK-001~003.

### A-TASK-011: 主计划剩余任务对齐并修复跨服务鉴权审计缺口

- Status: done
- Completed at: 2026-04-22
- Commit subject: `fix(governance): align remaining plan and close auth audit gaps`
- Priority: 1
- Depends on: A-TASK-010
- Scope: 对齐 active wave、剩余任务与事实完成度；同时修复 query-execution/sql-optimization/benchmark-engine 当前已确认的鉴权、租户归一化与审计元数据高优先缺口，并同步验证与文档真值。
- Validation:
  - `python3 scripts/foreman.py validate A-TASK-011`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Aligned the master plan current wave and A-task matrix with repository truth, added A-TASK-011 governance authority entries, enabled prod auth in sql-optimization and benchmark-engine, normalized query-execution tenantId to authenticated context, and propagated real request metadata into optimization/benchmark governance audit writes with regression coverage.
  - Validation evidence: python3 scripts/foreman.py validate A-TASK-011 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution,sql-optimization,benchmark-engine -am test -DskipITs" --extra-command "grep -n \"enabled: true\" sql-optimization/src/main/resources/application-prod.yml benchmark-engine/src/main/resources/application-prod.yml"
  - Residual risk: Shared auth/governance client code is still duplicated across three services, Phase-D still lacks a dedicated phase-exit R-117/R-118 proof batch, and optimization/benchmark persisted carriers still run placeholder executors rather than production-grade engines.
  - Next step: Instantiate the next highest-value task to extract shared auth/governance client support into sqlforge-shared, then resume the Phase-E boundary-governance mainline with E-TASK-007 and E-TASK-008 before handling Phase-F residual hardening.

### E-TASK-003: 落地合规中心与规则库板块

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add dashboard compliance and rulebook boards`
- Priority: 1
- Depends on: `E-TASK-002`
- Scope: 只读展示规则与合规信息 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-001` 研发驾驶舱
- Human confirmation point: 若移除规则/合规展示需人工确认
- Data impact: 前端展示数据，无后端持久化
- Rollback / recovery: 恢复只读展示页面
- Validation:
  - 可见性、只读性、构建
  - `python3 scripts/foreman.py validate E-TASK-003`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the /dashboard cockpit with explicit compliance-center and Codex-rulebook sections; added bilingual summaries for R-111 through R-115, surfaced append-only rulebook clusters, and derived rule-count summary cards from the repository truth in docs/security/compliance.md and docs/rules/codex-rules.md.
  - Validation evidence: Validated with python3 scripts/foreman.py validate E-TASK-003 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"dashboard\\.compliance|dashboard\\.rulebook|codexRulesMarkdown|complianceMarkdown|cardsSummary\" src/views/dashboard/DashboardView.vue src/locales/zh-CN.js src/locales/en-US.js"; npm run build passed and the rulebook summary now reads directly from the repository rule and compliance markdown sources.
  - Residual risk: The compliance and rulebook boards are still summary-only and depend on later work to connect more live operational evidence, such as audit-log query surfaces, backup-drill records, and any future machine-generated rule-growth telemetry.
  - Next step: Move to E-TASK-004 and keep the remaining Phase-E mainline on the business-route shells so the cockpit summary can continue routing into complete, independent workflow pages.

### E-TASK-002: 落地项目全景/架构设计/进度管理板块

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add dashboard architecture and progress boards`
- Priority: 1
- Depends on: `E-TASK-001`
- Scope: 驾驶舱展示项目与计划信息 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-001` 研发驾驶舱
- Human confirmation point: 若隐藏已承诺信息板块需人工确认
- Data impact: 前端展示数据，无后端持久化
- Rollback / recovery: 恢复板块与原信息架构
- Validation:
  - `页面结构与 IA 对照`
  - `python3 scripts/foreman.py validate E-TASK-002`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Expanded the /dashboard cockpit with explicit project-panorama, architecture-design, and progress-management sections; added bilingual IA copy for vision, roadmap, glossary, rule index, architecture summaries, and wired the progress board to the authoritative delivery snapshot derived from tasks.md, tasks-done.md, validation-log, and the master execution plan.
  - Validation evidence: Validated with python3 scripts/foreman.py validate E-TASK-002 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"dashboard\\.panorama|dashboard\\.architecture|dashboard\\.progress|createDeliveryProgressSnapshot|deliveryProgressAvailability\" src/views/dashboard/DashboardView.vue src/locales/zh-CN.js src/locales/en-US.js"; npm run build passed and the dashboard progress block is now backed by the same repository-truth snapshot used by the delivery-progress page.
  - Residual risk: The new dashboard sections are intentionally summary-only and still depend on later Phase-E tasks to land the remaining compliance/rule-library boards and to keep business metrics aligned with future backend-delivered live data rather than curated static copy.
  - Next step: Instantiate E-TASK-003 and land the compliance-center plus rule-library sections so the cockpit completes the remaining 10.1 information architecture promised for Phase-E.

### E-TASK-001: 建立驾驶舱路由与导航骨架

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(frontend): archive dashboard route shell baseline`
- Priority: 1
- Depends on: `Phase-C`
- Scope: 建立 dashboard 主路径与导航壳层 Tech: `VUE-FE`. Layer: `frontend/router/views/styles`.
- Matrix context: Phase-E / Story `E-STORY-001` 研发驾驶舱
- Human confirmation point: 主路由改名或删减需人工确认
- Data impact: 前端路由结构
- Rollback / recovery: 恢复原导航与入口映射
- Validation:
  - build、路由可达、i18n
  - `python3 scripts/foreman.py validate E-TASK-001`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Verified that the dashboard route shell baseline was already present in the frontend carrier, including the /dashboard primary route, grouped navigation shell, route metadata wiring, and zh-CN/en-US i18n labels across the app shell.
  - Validation evidence: Validated with python3 scripts/foreman.py validate E-TASK-001 --include-task-audit --extra-command "npm run build" --extra-command "rg -n \"path: ROUTE_PATHS.dashboard|redirect: ROUTE_PATHS.dashboard|common.navGroups|dashboard.title|dashboard.summary\" src/router/index.js src/App.vue src/locales/zh-CN.js src/locales/en-US.js", covering build, route registration, navigation grouping, and i18n keys.
  - Residual risk: The dashboard shell baseline is in place, but the content sections still need later Phase-E tasks to align with the full information architecture and keep summary data consistent with backend-delivered capabilities.
  - Next step: Instantiate E-TASK-002 and fill the dashboard with project-overview, architecture-design, and progress-management sections on top of the validated route shell.

### D-TASK-015: 补完 `query-execution` 真实执行适配与结果聚合基线

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(query-execution): add hetu mode-chain execution baseline`
- Priority: 1
- Depends on: `D-TASK-014`
- Scope: 落实 Hetu 多模式执行适配、模式选择、结果聚合与审计证据 Tech: `JAVA-BE`. Layer: `application(controller/service)/domain/infrastructure`.
- Matrix context: Phase-D / Story `D-STORY-005` 运行时执行链与跨服务补完
- Human confirmation point: 若真实执行适配放宽只读边界或引入破坏式执行契约需人工确认
- Data impact: 查询执行适配配置、执行结果聚合、审计记录
- Rollback / recovery: 切回最小同步基线并保留兼容执行模式/错误映射
- Validation:
  - `模块测试、跨模式适配测试、runtime smoke`
  - `python3 scripts/foreman.py validate D-TASK-015`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: completed the feature-flagged Hetu mode chain baseline in `query-execution`, including `JDBC/REST/CLIENT` mode adapters, routing, metadata/result aggregation, controller/service test updates, and contract/truth/capability/init doc sync.
  - 2026-04-22: `mvn -B -pl query-execution -am test -DskipITs` passed with new routing and adapter coverage.
  - 2026-04-22: the earlier Java 8 runtime misunderstanding was traced to a hand-run command that incorrectly hardcoded `spring-boot-maven-plugin:3.2.5:run`; repository POMs and runtime scripts remained aligned to Java 8 + Spring Boot `2.7.18`.
  - 2026-04-22: after rerunning the official foreman validation entrypoint with the standard runtime smoke, `python3 scripts/foreman.py validate D-TASK-015 --include-task-audit --extra-command 'python3 scripts/foreman.py compile-governance --check' --extra-command 'mvn -B -pl query-execution -am test -DskipITs' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke'` passed.
- Context closeout:
  - Completed scope: Implemented the feature-flagged Hetu mode-chain baseline in query-execution, including JDBC/REST/CLIENT adapters, mode routing, execution metadata/result aggregation, and Spring Boot 2.7 / Java 8 compatible test coverage and contract-doc updates.
  - Validation evidence: Validated with python3 scripts/foreman.py validate D-TASK-015 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check" --extra-command "mvn -B -pl query-execution -am test -DskipITs" --extra-command "bash scripts/run-runtime-smoke.sh --runtime-smoke", which passed on the repository-standard Java 8 + Spring Boot 2.7.18 toolchain.
  - Residual risk: Real Hetu execution remains feature-flagged off by default and still depends on external JDBC/REST/CLIENT connectivity, runtime credentials, and production parameter calibration before enabling outside controlled environments.
  - Next step: Proceed to the next Phase-D mainline task after confirming whether a follow-up task should add production-grade Hetu cluster evidence and tighter execution telemetry.

### A-TASK-010: 主计划与运行台账对齐

- Status: done
- Completed at: 2026-04-22
- Commit subject: `docs(governance): align master plan with task ledgers`
- Priority: 1
- Depends on: N/A
- Scope: 对齐 docs/plans/master-execution-plan.md、task-spec/task-governance 矩阵、tasks-done.md、INBOX.md 与 validation-log 的真实完成度，补回已执行但未入主计划的任务，并明确当前活跃波次与下一条可执行主线。
- Validation:
  - `python3 scripts/foreman.py validate A-TASK-010`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: reconciled `master-execution-plan.md`, `task-spec-matrix.md`, `task-governance-extension-matrix.md`, and plan index docs so `D-TASK-014`, `F-TASK-010`~`F-TASK-028`, new `A-TASK-010`, and new `D-TASK-015` are all visible to the same governance chain; resolved the stale active-wave pointer to `Phase-D / D-TASK-015`, cleared `INBOX-001`, and recompiled `.codex/policy/authority-map.json` after the authority-map drift check failed as expected on the first validation pass.
- Context closeout:
  - Completed scope: Reconciled the master execution plan, task spec matrix, governance extension matrix, and plan index documents with the actual execution record; backfilled D-TASK-014 and F-TASK-010 through F-TASK-028 into the planning corpus; introduced A-TASK-010 and D-TASK-015 as governed tasks; restored the current active wave to Phase-D with D-TASK-015 as the next executable mainline; and synced the derived authority map after governance compilation drift surfaced.
  - Validation evidence: Validated with python3 scripts/foreman.py validate A-TASK-010 --include-task-audit --extra-command "python3 scripts/foreman.py compile-governance --check", including repository knowledge lint, pre-closeout task audit, and a clean compile-governance drift check after recompiling .codex/policy/authority-map.json.
  - Residual risk: The governance source of truth is now aligned, but the newly restored mainline task D-TASK-015 is still not implemented; query-execution remains on the minimal synchronous baseline until that task closes.
  - Next step: Instantiate D-TASK-015 and implement the query-execution real execution adapter and result aggregation baseline on top of the repaired planning/ledger chain.

### F-TASK-027: 收口 Phase-F 退出门禁缺口

- Status: done
- Completed at: 2026-04-22
- Commit subject: `ci(governance): F-TASK-027 close phase-f exit gates`
- Priority: 1
- Depends on: F-TASK-026
- Scope: Close the remaining Phase-F exit-gate blockers by wiring dedicated database-script executability checks, coverage-threshold enforcement, Sonar-required delivery mode, and stronger R-118 compliance evidence into the phase-gate workflow and closeout path.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-027`
- Context closeout:
  - Completed scope: Added a dedicated database-script executability gate into both local phase-gate execution and default CI, tightened the Phase Gate workflow so delivery/full runs require Sonar and compliance/full can invoke the real Kafka runtime gate, and strengthened the compliance baseline checks plus deployment baselines to treat observability, recovery, Kafka runtime, and DB script evidence as first-class Phase-F exit criteria.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-027 --include-task-audit --extra-command 'bash scripts/verify-db-scripts.sh' --extra-command 'python3 scripts/verify_compliance_baseline.py'; additionally confirmed blocking semantics with bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase0 --require-sonar failing at the enforced coverage threshold (73.0005% < 80%) and bash scripts/run-sonar.sh --require-config failing fast when SONAR_HOST_URL/SONAR_TOKEN are absent.
  - Residual risk: Phase-F exit gates are now real blockers, but the repository still needs higher aggregate coverage, provisioned Sonar secrets, and automation beyond workflow_dispatch before the full delivery/compliance path can act as a zero-touch release gate.
  - Next step: Phase-F governance implementation is closed through F-TASK-027; the remaining follow-up is operational hardening: raise coverage to threshold, provision Sonar in CI, and bind phase-gate execution to real release metadata instead of manual dispatch only.

### F-TASK-026: 接入真实 Kafka 运行验证与环境安全参数门禁

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): F-TASK-026 gate real kafka runtime`
- Priority: 1
- Depends on: N/A
- Scope: Add Phase-F runtime verification for real Kafka mode, including bootstrap/security parameter validation, connectivity checks, failure-recovery smoke, and documented runtime-gate evidence so messaging is not only proven in DATABASE mode.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-026`
- Context closeout:
  - Completed scope: Added explicit Kafka bootstrap and security configuration validation for governance, centralized Kafka client property assembly for producer and consumer paths, updated dev/prod messaging config and local compose wiring for a runnable KRaft-backed broker, and delivered both a dedicated Kafka runtime gate workflow and local scripts that prove success delivery plus broker-stop fallback recovery in real KAFKA mode.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-026 --include-task-audit --extra-command 'mvn -B -pl governance -Dtest=MessagingConfigTest,KafkaMessageProducerTest,KafkaMessageConsumerTest test' --extra-command 'bash scripts/run-phase-gates.sh --gate compliance --run-real-kafka-gate', including a full governance startup in KAFKA mode, bootstrap/security parameter checks, topic connectivity, and fallback recovery after broker interruption.
  - Residual risk: The real Kafka gate now exists and passes locally, but it still depends on Docker-capable runners, an available broker port, and environment-specific secrets/certs for secure modes beyond the plaintext smoke configuration used in the local baseline.
  - Next step: Close F-TASK-027 to finish the remaining Phase-F exit-gate closure around DB script executability, stronger compliance evidence, and delivery gate blocking semantics.

### F-TASK-025: 补齐治理归档历史窗口与深分页链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): F-TASK-025 add long-window history lookup`
- Priority: 1
- Depends on: F-TASK-024
- Scope: Extend governance historical diagnostics beyond current indexed table lookups by adding archival-window query support, stronger deep-pagination strategy, and stable drill-through for older trace/task/report evidence across audit/query/export history so large-tenant and older-data forensics do not remain bounded by the current hot-window indexes.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-025`
- Context closeout:
  - Completed scope: Added indexed long-window governance history lookup support with a dedicated lookup-index mapper and migration, extended the governance history API/service to accept windowStart/windowEnd filters, carried those parameters through parse-record, audit-forensics, repair-evidence, and audit-troubleshooting drill-through flows, and hardened the browser runtime smoke to prove long-window pagination and cross-page query preservation on the canonical governance history routes.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-025 --include-task-audit --extra-command 'mvn -B -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest test' --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke', including a full startup + business + browser smoke pass.
  - Residual risk: The long-window lookup now avoids recent-scan behavior, but it still relies on current governance audit/query/export indexes rather than a separate archival/materialized history store, so very large tenants and colder data windows may still require a deeper history model.
  - Next step: Close F-TASK-026 to harden real Kafka runtime verification and security parameter gating on top of the updated governance runtime baseline.

### F-TASK-028: 拆分主线业务与治理运维页面路径

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): F-TASK-028 split governance route namespaces`
- Priority: 1
- Depends on: F-TASK-024
- Scope: Split the main business routes and the F-series governance/operations routes so `/dashboard`, `/sql-query`, `/acceleration`, `/benchmark`, and `/system` remain the product path while long-chain history/forensics/remediation/runtime-gates/recovery-drill/delivery-progress views move under dedicated `/governance/history/*` and `/governance/ops/*` namespaces with secondary navigation, keeping drill-through intact without pushing deep troubleshooting flows back into the main business pages.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-028`
- Context closeout:
  - Completed scope: Split the frontend navigation baseline into main workflow routes and dedicated governance history/ops namespaces, added canonical route constants plus legacy redirects, updated the app shell and dashboard navigation to surface governance history and ops as secondary paths, and delivered dedicated runtime-gates and recovery-drill pages under the new governance route tree without pushing long troubleshooting flows back into the main business pages.
  - Validation evidence: Validated with python3 scripts/foreman.py validate F-TASK-028 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build', plus the current browser/runtime smoke path now resolving the canonical governance namespaces used by the route tree.
  - Residual risk: Deep governance history pages still rely on follow-up tasks to carry every drill-through link and runtime assertion onto the canonical route tree; this task intentionally focused on the route namespace split, shell navigation, and governance ops landing pages.
  - Next step: Close F-TASK-025 to finish long-window governance history lookup and cross-page drill-through on top of the new governance namespaces.

### F-TASK-024: 新增审计故障处置与修复决策页

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add audit remediation decision runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: Add an audit troubleshooting/remediation decision page that correlates trace/task/report forensic evidence with failure type, compensation status, write-back status, and queue impact; expose real remediation actions including governance failed-message retry and drill-through into backlog/history/repair evidence; extend the browser runtime gate to validate the evidence -> decision -> remediation -> acceptance chain.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-024`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added a dedicated /audit-troubleshooting remediation decision page that correlates trace/task/report forensic evidence with queue impact and acceptance signals, wired audit-forensics and repair-evidence drill-through into the new page, extended repair-evidence with a return path to remediation, expanded the browser runtime gate to verify evidence -> remediation -> acceptance including a real governance failed-message retry and post-repair state change, and queued follow-up F-series tasks for route splitting, archival history, real Kafka verification, and remaining Phase-F gate closure.
  - Validation evidence: Validated with npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, npm run smoke:frontend-runtime, python3 scripts/foreman.py validate F-TASK-024, and python3 scripts/task_audit.py --check --phase pre-closeout against the running multi-service stack.
  - Residual risk: The remediation decision page still uses the current governance indexed history and queue admin APIs rather than a dedicated archival/materialized history model or separated governance route tree, so very old data, large tenants, and main-vs-ops navigation separation remain follow-up work rather than part of this task.
  - Next step: Start F-TASK-028 to split main business routes from governance/ops routes, then continue with F-TASK-025 archival history/deep pagination, F-TASK-026 real Kafka runtime verification, and F-TASK-027 Phase-F exit-gate closure.

### F-TASK-023: 扩展历史诊断与审计取证分页链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): extend historical forensic runtime chain`
- Priority: 1
- Depends on: N/A
- Scope: Extend /parse-record with indexed long-window trace/task/report lookup, pagination, and drill-through to trace detail; add a dedicated audit-forensics page that stitches compensation and repair evidence across trace/task/report lookups; expand browser runtime gate to cover the new historical forensic chain.
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-023`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Extended /parse-record with indexed long-window trace/task/report lookup, pagination, and drill-through actions; added a dedicated /audit-forensics page to stitch compensation, repair, and write-back evidence across paged governance lookups; updated repair-evidence to accept drill-through query context; and expanded the default browser runtime smoke to cover parse-record -> audit-forensics -> repair-evidence.
  - Validation evidence: Validated with npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, python3 scripts/foreman.py validate F-TASK-023, and npm run smoke:frontend-runtime against the running multi-service stack.
  - Residual risk: The new forensic pages still depend on current governance audit/query/export history density and indexed lookup over existing tables rather than a dedicated archival history model, so very large or older tenant datasets may still require deeper materialization and pagination tuning.
  - Next step: Continue expanding governance historical diagnostics by adding the next audit-troubleshooting page that pivots from trace/task/report evidence into remediation decisions and older archival windows.

### F-TASK-022: 升级治理长期历史反查与分页追溯

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): page indexed history lookups`
- Priority: 1
- Depends on: N/A
- Scope: replace recent-scan governance trace/task/report lookup with indexed history queries that support pagination over older audit, query-history, export, and task/report-linked evidence, then extend the frontend forensic pages to consume the paged API
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-022`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Replaced recent-scan governance trace/task/report reverse lookup with indexed paged history queries, added pagination cursor/page response support in governance, updated the /repair-evidence frontend to consume paged results with load-more, and extended browser runtime smoke to verify older task history pagination.
  - Validation evidence: Validated with mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest,TraceabilitySchemaMappingTest test, npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, python3 scripts/foreman.py validate F-TASK-022, and npm run smoke:frontend-runtime after restarting governance with the dev crypto key.
  - Residual risk: Long-window lookup now uses existing audit/query/export indexes rather than a dedicated archival history table, so very large tenants may still need follow-up work on deeper historical materialization and broader forensic page adoption.
  - Next step: Continue the browser runtime gate expansion into the next governance history or audit-forensics page, prioritizing views that can reverse-search compensation and repair evidence by trace/task/report on top of the new paged indexed lookup.

### F-TASK-021: 扩展治理历史修复追溯页 browser runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat: add governance repair evidence browser gate`
- Priority: 1
- Depends on: N/A
- Scope: add a governance history repair-evidence page keyed by trace/task/report lookups and extend browser runtime smoke to assert compensation and repair evidence rendering
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-021`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance history reverse-lookup APIs for trace/task/report evidence, delivered the /repair-evidence browser page, and extended the default frontend runtime smoke to assert compensation and repair evidence rendering across query, optimization, and benchmark traces.
  - Validation evidence: Validated with mvn -pl governance -Dtest=GovernanceHistoryApplicationServiceTest,AuthWebMvcTest test, npm run lint, npm run build, bash scripts/health-check.sh --fail-on-error, python3 scripts/foreman.py validate F-TASK-021, and npm run smoke:frontend-runtime after restarting governance and frontend against the updated code.
  - Residual risk: The new lookup endpoint still scans recent governance evidence rather than a dedicated indexed history table, so very old trace/task/report chains can age out of the reverse-lookup window until long-range archival queries are added.
  - Next step: Continue the browser runtime gate expansion to the next governance history or audit-forensics page, prioritizing views that expose older historical diagnosis and cross-trace repair evidence beyond the current recent-window lookup.

### F-TASK-020: 扩展治理历史页 browser runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(governance): extend parse record history runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: add governance history read APIs, replace /parse-record placeholder with a real traceability page, and extend browser runtime smoke to assert history/audit evidence rendering
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-020`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Added governance history summary/detail APIs, replaced /parse-record placeholder with a live historical diagnosis page, and extended the default browser runtime gate to assert audit traceability rendering.
  - Validation evidence: foreman validate passed with npm run lint, npm run build, governance targeted tests, bash scripts/health-check.sh --fail-on-error, and npm run smoke:frontend-runtime after restarting governance with the dev crypto key.
  - Residual risk: query_history/export_record remain empty in current dev smoke, so parse-record evidence is still audit-driven until later history/export writers are expanded.
  - Next step: Continue expanding browser runtime gate into additional governance history and audit troubleshooting views.

### F-TASK-019: 加固前端补偿信号稳定性

- Status: done
- Completed at: 2026-04-22
- Commit subject: `fix(frontend): stabilize compensation runtime evidence`
- Priority: 1
- Depends on: N/A
- Scope: 把前端 query/optimization/benchmark 页面中的补偿判定从 pending-only 调整为 pending 或 total 双信号，补齐 total delta 可视化，并收口 F-TASK-018 closeout 后遗留的验证日志与工作区变更。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-019`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: captured the post-closeout residual view changes from F-TASK-018 into a dedicated follow-up task so the compensation-signal hardening and validation-log tail can re-enter the normal audit chain without amending history.
- Context closeout:
  - Completed scope: Captured the missed post-closeout frontend view changes from F-TASK-018, changed query/optimization/benchmark compensation detection to use pending-or-total queue growth, exposed total delta evidence in the UI, and recorded the remaining validation-log entries in the audit chain.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-019 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run smoke:frontend-runtime'
  - Residual risk: The browser runtime gate is now stable against fast queue consumption for the current four pages, but frontend coverage still does not include parse-record or deeper audit/history visualization.
  - Next step: Continue extending the browser runtime gate to the next real business page, prioritizing parse-record or other governance history views now that compensation evidence has been stabilized.

### F-TASK-018: 扩展 system 治理管理页 browser runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): add governance system runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: 把前端 /system 从占位页替换为真实 governance 管理页，接入 tenant-config、message stats、retry failed messages，并把浏览器 runtime smoke 扩展到治理 backlog 与补偿修复动作，继续扩大默认 CI 的前端业务级门禁覆盖。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-018`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: replaced `/system` placeholder with a live governance admin page that loads tenant-config, message stats, failed-message retry evidence, and extended browser smoke to seed a FAILED queue row and verify retry remediation from the UI.
- Context closeout:
  - Completed scope: Replaced the /system placeholder with a live governance admin runtime page, added tenant-config/message-stats/retry APIs, seeded FAILED queue remediation evidence in browser smoke, and expanded the default frontend runtime gate from core execution pages to governance repair actions.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-018 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run smoke:frontend-runtime'
  - Residual risk: Default browser runtime gate now covers the core execution pages plus governance remediation, but parse-record and other business views still remain outside the default browser smoke and there is still no frontend visualization for deeper trace/audit history records.
  - Next step: Extend the browser runtime gate to the next real business page, prioritizing parse-record or other governance history views so frontend runtime coverage continues to grow beyond the current four pages.

### F-TASK-017: 扩展前端失败恢复与审计补偿 runtime gate

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(frontend): extend recovery runtime gate`
- Priority: 1
- Depends on: N/A
- Scope: 把前端浏览器 smoke 从成功链路扩展到失败恢复与审计补偿可视化，复用已完成的 sql-optimization / benchmark-engine 持久化后端能力，并接入默认 runtime gate / CI。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-017`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-22: extended frontend runtime gate pages and Playwright smoke to cover query-execution degraded recovery, sql-optimization failed-task compensation, benchmark-engine failed-task compensation, and governance queue pending-delta evidence.
- Context closeout:
  - Completed scope: Extended the frontend runtime gate pages and Playwright smoke from success-only checks to query degraded recovery, sql-optimization failed-task compensation, benchmark-engine failed-task compensation, governance queue pending-delta visualization, and 64-character-safe correlation headers.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-017 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'npm run smoke:frontend-runtime'
  - Residual risk: Default browser runtime gate now covers the three key frontend flows end-to-end, but more business pages and richer audit-compensation remediation views are still outside the default smoke suite.
  - Next step: Extend the browser runtime gate to additional business pages and richer remediation/audit views now that the core three frontend chains are blocked by default CI.

### F-TASK-016: 推进 benchmark-engine 真实持久化与调度链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(benchmark-engine): persist benchmark task pipeline`
- Priority: 1
- Depends on: N/A
- Scope: 把 benchmark-engine 从 in-memory 占位执行器推进到 MySQL 持久化任务表、报告回写与 worker/scheduler 链路，并复用治理 smoke 门禁验证成功链路、失败恢复与审计补偿。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-016`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Implemented MySQL-backed benchmark task/report persistence, report write-back, scheduled worker execution, schema/migration updates, and governance smoke assertions for success, failure recovery, and audit compensation.
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-016 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test -DskipITs" --extra-command "bash scripts/run-runtime-smoke.sh --compose-check" --extra-command "bash scripts/manual-benchmark-governance-smoke.sh --cleanup"
  - Residual risk: Default full runtime smoke is still blocked in this workstation by an unrelated process already occupying port 3000, so frontend-included CI parity still depends on a clean runner; benchmark-engine also still uses placeholder execution logic behind the persisted carrier rather than a real isolated benchmark executor/export pipeline.
  - Next step: Extend the frontend browser runtime gate from success-only flows to failure recovery and audit-compensation visualization now that sql-optimization and benchmark-engine both have persisted backend carriers.

### F-TASK-015: 推进 sql-optimization 真实持久化与调度链路

- Status: done
- Completed at: 2026-04-22
- Commit subject: `feat(sql-optimization): persist optimization tasks with scheduled worker`
- Priority: 1
- Depends on: N/A
- Scope: 把 sql-optimization 从 in-memory 占位执行器推进到 MySQL 持久化任务表、真实状态流转与 worker/scheduler 链路，并同步更新验证脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-015`
- Progress log:
  - 2026-04-22: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: Replace sql-optimization placeholder carrier with MySQL-backed optimization_task persistence, scheduled worker execution, runtime schema bootstrap, and governance smoke coverage.
  - Validation evidence: mvn -B -pl sql-optimization -am test; bash scripts/manual-sql-optimization-governance-smoke.sh --cleanup; python3 scripts/foreman.py validate F-TASK-015
  - Residual risk: Full default runtime smoke remains blocked locally by an existing port 3000 frontend process; backend sql-optimization runtime path is validated directly.
  - Next step: Start the follow-up benchmark-engine persistence/scheduler task, then extend frontend smoke to failure recovery and audit compensation visualization.

### F-TASK-014: 扩展前端真实业务 runtime smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(frontend): F-TASK-014 gate browser runtime smoke`
- Priority: 1
- Depends on: N/A
- Scope: 把前端可见的真实业务路径接入默认 runtime smoke / CI，验证前端发起请求后 query-execution、sql-optimization、benchmark-engine 与 governance 的关键链路能从 UI 侧跑通，并补脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-014`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 为前端新增 sql-query、acceleration、benchmark 三个真实业务页与受保护 API client，按服务扩展 Vite proxy，新增浏览器驱动 smoke 并接入默认 runtime smoke / CI。
  - Validation evidence: npm run lint；npm run build；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-014 --include-task-audit --extra-command 'npm run lint' --extra-command 'npm run build' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 前端 runtime gate 当前覆盖 3 条成功业务链路，但失败恢复 UI、审计补偿可视化和更多业务页尚未并入浏览器 smoke；sql-optimization 和 benchmark-engine 仍是占位执行器链路。
  - Next step: 继续推进 sql-optimization / benchmark-engine 的真实持久化、调度与回调链路，并把对应失败恢复与前端可视化一并接入 runtime gate。

### F-TASK-013: 扩展优化与压测业务级 runtime smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): F-TASK-013 gate optimization-benchmark business smoke`
- Priority: 1
- Depends on: N/A
- Scope: 把 sql-optimization 和 benchmark-engine 的真实业务联调、失败恢复路径与治理审计补偿验证接入默认 runtime smoke / CI，沿用 F-TASK-012 的模式扩展关键链路门禁，并同步脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-013`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: wired `sql-optimization` and `benchmark-engine` to governance tenant/datasource checks plus audit write contracts, and added business smoke scripts for success, failure, and audit compensation paths.
  - 2026-04-21: verified module tests and default runtime smoke for query-execution/sql-optimization/benchmark-engine business gates before closeout.
- Context closeout:
  - Completed scope: 为 sql-optimization 和 benchmark-engine 增加治理内部 HTTP client、租户/数据源校验与 audit/write 上报；新增两条业务级 runtime smoke，验证提交/轮询/报告读取、失败恢复路径与审计补偿队列；同步扩展默认 runtime smoke、CI/本地文档真值与治理数据源映射。
  - Validation evidence: mvn -B -pl sql-optimization,benchmark-engine -am test；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --keep-stack；python3 scripts/foreman.py validate F-TASK-013 --include-task-audit --extra-command 'mvn -B -pl sql-optimization,benchmark-engine -am test' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前业务级 runtime gate 已覆盖 query-execution、sql-optimization、benchmark-engine 到 governance 的关键链路，但前端仍缺少更深层业务 smoke；benchmark/sql-optimization 仍是占位执行器链路，生产级真实持久化、回调、消息消费和 Kafka 安全参数验证尚未闭环。
  - Next step: 继续把前端真实业务链路与更深层恢复/积压演练纳入默认 runtime gate，并在后续任务中把 sql-optimization / benchmark-engine 从占位执行器推进到真实持久化和调度链路。

### F-TASK-012: 扩展跨服务业务级 runtime smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): F-TASK-012 gate query-governance business smoke`
- Priority: 1
- Depends on: N/A
- Scope: 把 query-execution -> governance 的真实业务联调、失败恢复路径与审计补偿验证接入默认 runtime smoke / CI，形成比启动探针更深一层的关键链路门禁，并同步脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-012`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: wired query-execution protected context, governance internal client, runtime business smoke, degraded fallback verification, and audit compensation queue checks into the default runtime gate.
- Context closeout:
  - Completed scope: 为 query-execution 增加受保护请求上下文、治理内部 HTTP client 与审计写入，使执行链路在真实运行时会调用 governance 的租户/数据源校验与 audit/write；为 governance 增加按 trace 前缀触发的定向审计路由失败注入；新增 scripts/manual-query-governance-smoke.sh 并把它接入默认 runtime smoke/CI，验证 query-execution -> governance 的成功链路、超时降级恢复与审计补偿队列兜底。
  - Validation evidence: mvn -B -pl query-execution -am test；mvn -B -pl governance -am test；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-012 --include-task-audit --extra-command 'mvn -B -pl query-execution -am test' --extra-command 'mvn -B -pl governance -am test' --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认业务级 runtime gate 只覆盖 query-execution -> governance；sql-optimization、benchmark-engine 与前端仍停留在启动级 smoke，治理侧的主消息路由失败注入也仅用于本地/CI 的 trace 前缀定向演练，不代表生产消息故障演练已闭环。
  - Next step: 继续把 sql-optimization / benchmark-engine 的真实业务链路、跨服务审计上报和失败恢复路径纳入默认 runtime gate，并补治理消息消费/积压恢复的更深层验证。

### F-TASK-011: 扩展多服务运行时 smoke 门禁

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(runtime): F-TASK-011 gate multi-service smoke`
- Priority: 1
- Depends on: F-TASK-010
- Scope: 把 query-execution、sql-optimization、benchmark-engine 和前端的真实启动探针接入默认 CI，扩展 runtime smoke 为多服务门禁，并同步脚本与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-011`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: extended runtime smoke plan to cover query-execution, sql-optimization, benchmark-engine, frontend startup probes, and CI/doc truth alignment.
- Context closeout:
  - Completed scope: 扩展 scripts/run-runtime-smoke.sh 为多服务编排，真实拉起 governance、query-execution、sql-optimization、benchmark-engine 与前端 dev server；增强 scripts/health-check.sh 为多服务强制探针；同步调整默认 CI 与文档真值，使 runtime smoke 从 governance 单点扩展为多服务门禁。
  - Validation evidence: bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-011 --include-task-audit --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认 CI 已覆盖多服务启动与基础健康探针，但仍未覆盖更深层跨服务业务回归、真实 Kafka 运行验证与生产密钥托管链路；query-execution 仍依赖共享加密配置环境变量注入 dev key 才能完成 smoke。
  - Next step: 继续把更深层跨服务业务 smoke、失败恢复路径和交付级 phase gate 验证并入默认 CI，优先补查询执行与治理链路之间的业务级联调证据。

### F-TASK-010: 接入运行时 smoke 到默认 CI

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(runtime): F-TASK-010 gate compose and smoke checks`
- Priority: 1
- Depends on: F-TASK-005,F-TASK-006
- Scope: 把 docker compose config、本地启动/健康探针、消息队列 smoke 从本地脚本接入默认 CI，补齐 runtime 门禁与文档证据链。
- Validation:
  - `python3 scripts/foreman.py validate F-TASK-010`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 把 compose 校验、本地依赖启动、governance 健康探针与消息队列 smoke 接入默认 CI；新增 scripts/run-runtime-smoke.sh 串联 compose 校验、local-start、governance 启动、health-check 与 manual-message-queue-smoke，并增强 health-check.sh 的可阻断模式；同时修复 GovernanceAuditTrailService 的 Spring 构造器装配，使 dev 启动路径可被真实验证。
  - Validation evidence: mvn -B -pl governance -am test；bash scripts/run-runtime-smoke.sh --compose-check；bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack；python3 scripts/foreman.py validate F-TASK-010 --include-task-audit --extra-command 'bash scripts/run-runtime-smoke.sh --compose-check' --extra-command 'bash scripts/run-runtime-smoke.sh --runtime-smoke --reuse-running-stack --keep-stack'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认 runtime smoke 仍只覆盖 governance 与数据库消息队列链路，query-execution、sql-optimization、benchmark-engine 与前端尚未纳入统一启动验证；脚本使用 dev 测试密钥满足敏感字段加密初始化，仅适用于本地/CI smoke，不代表生产密钥托管已闭环。
  - Next step: 继续把其余后端模块与前端的真实启动探针纳入 CI，逐步把 runtime smoke 从 governance 单点扩展为多服务启动门禁。

### F-TASK-006: 接入 Java 规范扫描

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(java): F-TASK-006 trace scan artifacts`
- Priority: 1
- Depends on: `F-TASK-004`
- Scope: 让 pmd/checkstyle 进入 CI Tech: `OPS`,`JAVA-BE`. Layer: `deployments/ci/scripts`,`common`.
- Matrix context: Phase-F / Story `F-STORY-002` CI 与质量门禁
- Human confirmation point: 扫描阈值与工具变更需人工确认
- Data impact: CI 质量结果、构建流程
- Rollback / recovery: 回退扫描接入并保留报告
- Validation:
  - `mvn validate pmd:pmd checkstyle:check`
  - `python3 scripts/foreman.py validate F-TASK-006`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 把 .github/workflows/ci.yml 中的 Java 质量检查拆成 validate、PMD、Checkstyle report、Checkstyle gate 四个显式步骤，并新增 Java 报告校验与 artifact 上传；新增 scripts/verify_java_quality_reports.py 校验各模块 PMD / Checkstyle XML 与 HTML 报告是否产出；同步更新 CI 能力基线与阿里 Java 规范落地文档。
  - Validation evidence: mvn -B -DskipTests validate pmd:pmd checkstyle:checkstyle checkstyle:check；python3 -m py_compile scripts/verify_java_quality_reports.py；python3 scripts/verify_java_quality_reports.py；python3 scripts/foreman.py validate F-TASK-006 --include-task-audit --extra-command 'python3 -m py_compile scripts/verify_java_quality_reports.py' --extra-command 'python3 scripts/verify_java_quality_reports.py'；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 Java 扫描已可追溯并保留 artifact，但 PMD 仍沿用现有 p3c 规则集与默认阈值，扫描本身未拆成独立 job，也未生成跨历史趋势报表；若后续要把 PMD 违规数纳入硬阻断或趋势治理，仍需单独任务确认。
  - Next step: 优先补齐 compose/runtime smoke 与本地启动健康检查入 CI，把当前仍停留在本地脚本的运行时验证收进默认门禁。

### F-TASK-005: 接入阶段门禁脚本化验证

- Status: done
- Completed at: 2026-04-21
- Commit subject: `ci(gates): F-TASK-005 script phase gate checks`
- Priority: 1
- Depends on: `F-TASK-004`
- Scope: 让阶段切换可阻断 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-002` CI 与质量门禁
- Human confirmation point: 门禁阻断规则放宽需人工确认
- Data impact: CI 阻断逻辑
- Rollback / recovery: 恢复原门禁规则
- Validation:
  - 门禁阻断验证
  - `python3 scripts/foreman.py validate F-TASK-005`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 scripts/run-phase-gates.sh 与 scripts/verify_compliance_baseline.py，把 R-116/R-117/R-118 阶段门禁脚本化；在 .github/workflows/ci.yml 增加 task_audit 与 foreman compile-governance --check 的默认阻断，并新增手动 phase-gate workflow 承载可执行的 entry/compliance/delivery 门禁；同步更新 CI 能力基线、阶段门禁基线、docs 入口与文档覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-005 --include-task-audit；node scripts/lint-repository-knowledge.js；bash scripts/run-phase-gates.sh --gate entry；bash scripts/run-phase-gates.sh --gate compliance；bash scripts/run-phase-gates.sh --gate delivery --coverage-phase phase0；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前默认 CI 仍未把完整 delivery gate 设为每次提交必跑，phase1plus 85% 覆盖率与 Sonar 依赖仍通过手动 phase-gate workflow 承载；compose/runtime smoke 也尚未接入默认流水线。
  - Next step: 进入 F-TASK-006，把 Java 静态扫描结果在 CI 中拆分为可追踪的质量步骤与工件，补齐 R-151/R-152 的落地证据。

### F-TASK-004: 盘点现有 CI 能力

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(ci): F-TASK-004 inventory current CI coverage`
- Priority: 1
- Depends on: `Phase-C`,`Phase-E`
- Scope: 盘点 CI 对 lint/build/test 的覆盖 Tech: `OPS`,`DOCS`. Layer: `deployments/ci/scripts`,`docs`.
- Matrix context: Phase-F / Story `F-STORY-002` CI 与质量门禁
- Human confirmation point: 无
- Data impact: CI 清单和流水线映射
- Rollback / recovery: 恢复原 CI 清单说明
- Validation:
  - `CI 清单完整性检查`
  - `python3 scripts/foreman.py validate F-TASK-004`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deployments/ci-capability-baseline.md，盘点 .github/workflows/ci.yml 当前对 lint/build/test/scan 的真实覆盖，明确本地可执行但尚未进入 CI 的 task_audit、foreman validate、phase gate coverage、compose/runtime smoke 等缺口，并同步更新 docs 入口、文档真值基线与覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-004 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 .github/workflows/ci.yml 仍未接入 task_audit、foreman compile-governance --check、phase gate coverage threshold、compose/runtime smoke，也未把 Sonar 固化为默认必经门禁；这些缺口需要后续 F-TASK-005/006 补齐。
  - Next step: 进入 F-TASK-005，把 task_audit、governance compile check 和阶段门禁脚本化接入 CI，先形成真正可阻断的 phase gate。

### F-TASK-009: 阶段交付回写闭环

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(delivery): F-TASK-009 close phase-f delivery loop`
- Priority: 1
- Depends on: `F-TASK-008`
- Scope: 完成记录、commit、tag、回写闭环 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 交付闭环若省略 commit/tag 回写需人工确认
- Data impact: 交付记录、git 元数据、验证日志
- Rollback / recovery: 补写交付记录和标签/提交元数据
- Validation:
  - `交付记录与 git 元数据一致`
  - `python3 scripts/foreman.py validate F-TASK-009`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deliveries/phase-f-story-003-ops-closeout.md，统一记录 Phase-F Story-003 下 F-TASK-007/008 的交付基线、F-TASK-009 的 delivery closeout 清单与 write-back 模板，并同步更新 docs 入口与文档覆盖矩阵，形成阶段交付闭环的文档落点。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-009 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前 foreman 的 delivery-closeout 只负责打 tag 和追加 write-back，实际 write-back 仍会在仓库内留下后续元数据改动；此外 F-STORY-003 的交付闭环仍依赖人工维护 tag 命名策略和交付记录选择。
  - Next step: 执行 delivery-closeout，为本次 Phase-F Story-003 交付主提交打 tag，并把 tag/write-back 元数据回填到 docs/deliveries/phase-f-story-003-ops-closeout.md。

### F-TASK-008: 补齐备份恢复策略与演练记录模板

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(deploy): F-TASK-008 add backup recovery baseline`
- Priority: 1
- Depends on: `F-TASK-007`
- Scope: 定义 RPO/RTO/演练记录模板 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 备份恢复目标或演练频率变更需人工确认
- Data impact: 备份元数据、演练记录
- Rollback / recovery: 恢复旧模板并补录演练
- Validation:
  - 备份恢复模板可追溯
  - `python3 scripts/foreman.py validate F-TASK-008`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 新增 docs/deployments/backup-recovery-baseline.md，把 MySQL、audit_log、export_record、kafka_message_queue、system_config 密文与密钥边界的备份对象、RPO/RTO、责任角色和恢复演练模板统一收口，并同步更新 docs 入口、华为云部署引用、文档真值基线与覆盖矩阵。
  - Validation evidence: python3 scripts/foreman.py validate F-TASK-008 --include-task-audit；node scripts/lint-repository-knowledge.js；python3 scripts/task_audit.py --check --phase pre-closeout；python3 scripts/task_audit.py --check --phase post-closeout。
  - Residual risk: 当前仓库仍未提供 MySQL/OBS 备份自动化脚本、密钥托管与轮换校验工具、以及恢复后自动 smoke 脚本；query-execution、sql-optimization、benchmark-engine 仍无独立持久化，因此服务级恢复更多依赖健康检查与治理元数据验收。
  - Next step: 进入 F-TASK-009 时，基于本备份恢复基线把演练记录、commit/tag、交付回写和 write-back 模板一起收口，形成阶段交付闭环。

### F-TASK-007: 补齐监控与日志规范落地清单

- Status: done
- Completed at: 2026-04-21
- Commit subject: `docs(deploy): F-TASK-007 add observability baseline checklist`
- Priority: 1
- Depends on: `Phase-D`
- Scope: 输出 logs/metrics/alerts 落地清单 Tech: `DOCS`,`OPS`. Layer: `docs`,`deployments/ci/scripts`.
- Matrix context: Phase-F / Story `F-STORY-003` 运维、审计与恢复
- Human confirmation point: 监控/日志采样策略显著削弱需人工确认
- Data impact: 日志、指标、告警配置
- Rollback / recovery: 恢复原监控规则
- Validation:
  - 清单完整、映射一致
  - `python3 scripts/foreman.py validate F-TASK-007`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
  - 2026-04-21: audited current observability facts across 4 backend modules, confirming shared `logback-spring.xml` baselines, shared actuator exposure (`health/info/metrics/prometheus`), governance audit fallback logs, and async task flow logs already exist in code/config.
  - 2026-04-21: created `docs/deployments/observability-baseline.md` to distinguish current implemented logs/metrics signals from still-missing business metrics, alert rules, and tracing/platform pipeline integrations, then wired the new authority doc into docs entrypoints and coverage tracking.
- Context closeout:
  - Completed scope: 新增 `docs/deployments/observability-baseline.md`，把 4 个后端服务当前已落地的日志、Actuator 暴露、审计/异步状态流信号统一收口为 logs/metrics/alerts 运维清单，并同步更新文档入口、华为云部署引用、文档真值基线与覆盖矩阵。
  - Validation evidence: `python3 scripts/foreman.py validate F-TASK-007 --include-task-audit`；`node scripts/lint-repository-knowledge.js`；`python3 scripts/task_audit.py --check --phase pre-closeout`；`python3 scripts/task_audit.py --check --phase post-closeout`。
  - Residual risk: 当前仓库仍未实现业务级 Micrometer 指标、仓库内 Prometheus/Alertmanager/Grafana 规则文件和统一 tracing/log pipeline 配置，业务告警仍需外部平台按本文档补位。
  - Next step: 进入 `F-TASK-008` 时，直接复用本 observability 基线中的审计、消息队列、导出与敏感数据观测项，补齐备份恢复策略和演练模板。

### D-TASK-014: 收口异步服务鉴权、占位执行与审计兜底

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(runtime): D-TASK-014 secure async placeholders and audit fallback`
- Priority: 1
- Depends on: D-TASK-013
- Scope: 在 sql-optimization 与 benchmark-engine 落实 header-based 鉴权、租户隔离与异步占位执行，补 benchmark 原始报告数据查询，并为 governance 审计消息增加数据库队列兜底，同时收紧共享加密配置到显式密钥基线。
- Validation:
  - `python3 scripts/foreman.py validate D-TASK-014`
- Progress log:
  - 2026-04-21: instantiated from foreman CLI using repository truth and task matrices.
- Context closeout:
  - Completed scope: 在 `sql-optimization` 与 `benchmark-engine` 落实了 header-based 请求鉴权、租户隔离和异步占位执行器，新增 benchmark 原始报告数据查询接口，并为 `governance` 审计消息主路由失败补上数据库队列兜底，同时把共享敏感加密配置改为显式密钥基线。
  - Validation evidence: `python3 scripts/foreman.py validate D-TASK-014 --include-task-audit --extra-command "mvn -B -pl benchmark-engine -am test" --extra-command "mvn -B -pl sql-optimization -am test" --extra-command "mvn -B -pl governance -am test"`；`python3 scripts/task_audit.py --check --phase pre-closeout`；`python3 scripts/task_audit.py --check --phase post-closeout`。
  - Residual risk: benchmark/sql-optimization 仍然是本地占位执行链，尚未接入真实 worker、持久化队列与跨服务主动审计上报。
  - Next step: 后续若把这批能力固化进长期计划，需要把 D-TASK-014 补回主计划与任务矩阵，并继续把异步任务接到真实执行/回调链。

### HARN-009: Repair closeout archive boundaries and done-ledger structure checks

- Status: done
- Completed at: 2026-04-21
- Commit subject: `fix(codex): HARN-009 repair closeout boundary auditing`
- Priority: 1
- Depends on: HARN-008
- Scope: 只修两个治理缺口：`foreman closeout` 归档任务块时必须限制在当前台账 section 内，不得把后续 section heading 一并搬入 `tasks-done.md`；同时为 `task_audit` 增加 `tasks-done.md` 结构校验，明确禁止 `## In Review`、`## Blocked` 等 heading 混入完成台账正文。本批次允许同步修正当前已被污染的 `tasks-done.md` 结构，但不触碰任何业务模块脏改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/foreman.py compile-governance --check`
- Progress log:
  - 2026-04-21: instantiated as a minimal governance repair batch after post-HARN-008 review found that `foreman closeout` could archive trailing section content into `tasks-done.md`, and `task_audit` failed to detect the resulting structural corruption.
  - 2026-04-21: restricted `extract_task_blocks_with_spans()` so closeout now stops at the next task header or the next section heading, which prevents `tasks.md` trailing section bodies from being archived into `tasks-done.md`.
  - 2026-04-21: added explicit `tasks-done.md` structural checks to `task_audit.py` for unexpected `##` headings and stray non-task content inside the done section, then repaired the already polluted `HARN-008` done block back to the intended shape.
- Context closeout:
  - Completed scope: Restricted foreman task-block archiving to the current ledger section, added explicit tasks-done structure validation for unexpected headings and stray non-task content, repaired the previously polluted HARN-008 done block, and wired exec-plan move plus coverage-matrix/policy recompilation so closeout stays structurally consistent.
  - Validation evidence: python3 -m py_compile scripts/foreman.py scripts/task_audit.py; python3 scripts/task_audit.py --check --phase pre-closeout; python3 scripts/task_audit.py --check --phase post-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check; python3 scripts/foreman.py validate HARN-009 --include-task-audit; targeted synthetic done-ledger corruption probe now fails with unexpected-heading and stray-content errors
  - Residual risk: The repair now blocks the concrete closeout-ledger corruption path and detects section-level contamination, but future ledger format expansions still need to stay aligned across foreman, task_audit, and document indexes.
  - Next step: Use the repaired closeout path as the default governance closeout flow, and if later extending ledger formats, update both archiving boundaries and structural audits in the same batch.

### HARN-008: Close the 6 remaining Codex governance runtime gaps

- Status: done
- Completed at: 2026-04-21
- Commit subject: `feat(codex): HARN-008 close the remaining governance runtime gaps`
- Priority: 1
- Depends on: HARN-007
- Scope: 只修复 HARN-007 严格复核遗留的 6 个治理/runtime 闭口点：hooks 输出协议与 hooks.json 组织、preflight 真实消费文档真值、instantiate 真实消费任务矩阵、closeout 与 delivery-closeout 真正落地、运行态 closeout 后清理、以及可信本地验证证据；不触碰当前业务模块脏改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
  - `python3 scripts/validate_codex_runtime.py`
  - `python3 scripts/foreman.py compile-governance --check`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: instantiated as the strict-mode follow-up batch to close the 6 unresolved governance/runtime gaps left after HARN-007, with write scope restricted to docs, ledgers, `.codex/`, and repository governance scripts only.
  - 2026-04-21: replaced the hard-coded foreman scaffolding with document-aware preflight, matrix-aware instantiate, explicit-stage closeout, delivery-closeout tag/write-back semantics, dynamic governance compilation, and idle-state cleanup after closeout.
  - 2026-04-21: aligned `.codex/hooks.json` and repository-local hook handlers with the official Codex hook output contracts, including `hookSpecificOutput.additionalContext` for `UserPromptSubmit` and event-specific permission decisions for `PreToolUse` / `PermissionRequest`.
  - 2026-04-21: fixed `.codex/config.toml` so project-doc settings no longer sit under the `[features]` table, which had been breaking real `codex exec` startup.
  - 2026-04-21: added `scripts/validate_codex_runtime.py` to deterministically validate preflight, matrix instantiate, delivery-closeout dry-run, hook output shapes, stop gating, and a trusted local `codex exec --json` path.
  - 2026-04-21: reran `python3 scripts/foreman.py validate HARN-008 --include-task-audit --extra-command 'codex exec --json --sandbox read-only --skip-git-repo-check "Reply with OK only."'`; governance validation, runtime simulation, task audit, repository knowledge lint, compile-governance drift check, and real Codex CLI execution all passed.
- Context closeout:
  - Completed scope: Aligned repository-local Codex hooks with official event contracts, replaced the hard-coded foreman scaffolding with document-aware preflight and matrix-aware instantiate, implemented explicit-stage closeout and delivery-closeout semantics, fixed project-scoped Codex config loading, added deterministic runtime validation plus a real codex exec verification path, and cleaned the HARN exec-plan/archive wiring without touching business-module dirty changes.
  - Validation evidence: python3 scripts/foreman.py validate HARN-008 --include-task-audit --extra-command "codex exec --json --sandbox read-only --skip-git-repo-check \"Reply with OK only.\""; python3 scripts/task_audit.py --check --phase pre-closeout; node scripts/lint-repository-knowledge.js; python3 scripts/foreman.py compile-governance --check
  - Residual risk: Repository-local hooks still depend on trusted-project loading in the users Codex environment, so AGENTS.md plus foreman/task_audit/lint remain the mandatory fallback enforcement chain.
  - Next step: Use the hardened foreman closeout and delivery-closeout path for later governance and F-task work, and keep task-matrix / blueprint / hook contracts in sync as the repository evolves.

### HARN-007: Integrate Codex runtime with repository truth and closeout flow

- Status: done
- Priority: 1
- Depends on: HARN-006
- Completed at: 2026-04-21
- Commit subject: `feat(codex): HARN-007 wire runtime governance scaffolding`
- Scope: 在不丢失现有 `docs/`、任务台账、验证日志、closeout、Git 审计链和 F-task 语义的前提下，把 SQLForge 的文档真值体系接入 Codex 的执行前、执行中、执行后生命周期；本轮只改治理与接线层，不触碰当前业务模块中的脏工作树改动。
- Validation:
  - `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`
  - `python3 scripts/foreman.py compile-governance`
  - `python3 scripts/foreman.py compile-governance --check`
  - `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard --prompt "Integrate Codex runtime with repository truth and closeout flow under strict mode."`
  - `python3 scripts/foreman.py validate HARN-007 --include-task-audit`
  - `python3 scripts/foreman.py sync-context --done-ready`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: instantiated `HARN-007` as the strict-mode governance task for Codex-docs runtime integration after confirming the root cause was “docs truth exists but Codex execution path is not directly wired to it”.
  - 2026-04-21: current repository contains unrelated dirty business-module edits under `benchmark-engine/`, `governance/`, `sql-optimization/`, and `sqlforge-shared/`; this task therefore stayed inside governance docs, Codex integration scaffolding, scripts, and repository knowledge checks.
  - 2026-04-21: wrote the active exec plan and the formal `codex-governance-integration-blueprint.md`, then replaced the root navigation-only `AGENTS.md` with a hard repository contract that points Codex back to the authority chain and standard action entrypoints.
  - 2026-04-21: added project-scoped `.codex/config.toml`, `.codex/hooks.json`, repository-local hook handlers, runtime state examples, and a tracked current-task schema so Codex can consume repo policy without promoting `.codex/` into a second source of truth.
  - 2026-04-21: implemented `scripts/foreman.py` as the unified governance entrypoint for `preflight`, `instantiate`, `sync-context`, `validate`, `audit`, `compile-governance`, `closeout`, and `delivery-closeout`; the closeout commands are intentionally bootstrap-gated pending a later hardening batch that can safely auto-scope staged files.
  - 2026-04-21: updated `docs/README.md`, `docs/plans/README.md`, `docs/operations/README.md`, `docs/operations/local-development.md`, `README.md`, and `docs/plans/document-coverage-matrix.md` so the new Codex integration artifacts are visible to both humans and machine checks.
  - 2026-04-21: expanded `scripts/lint-repository-knowledge.js` to require the new blueprint, `.codex` scaffolding, and `scripts/foreman.py`, fixed the resulting README / coverage-matrix gaps, recompiled `.codex/policy/*.json`, and reran governance checks until both knowledge lint and compile-governance drift checks passed.
- Context closeout:
  - Completed scope: Codex runtime governance scaffolding is now wired into the repository through a formal blueprint, hardened root `AGENTS.md`, project-scoped `.codex` config/hooks/state/schema, a unified `scripts/foreman.py` CLI, generated governance manifests, and updated documentation/knowledge-lint indexes.
  - Validation evidence: `python3 -m py_compile scripts/foreman.py .codex/hooks/*.py`; `python3 scripts/foreman.py compile-governance`; `python3 scripts/foreman.py compile-governance --check`; `python3 scripts/foreman.py preflight --task HARN-007 --task-class standard --prompt "Integrate Codex runtime with repository truth and closeout flow under strict mode."`; `python3 scripts/foreman.py validate HARN-007 --include-task-audit`; `python3 scripts/foreman.py sync-context --done-ready`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: repository-local `.codex` hooks still need live validation inside a trusted Codex project, and `closeout` / `delivery-closeout` remain intentionally bootstrap-gated until a later batch can safely automate archival + commit scoping without catching unrelated dirty files.
  - Next step: validate the new `.codex` layer inside a trusted Codex runtime, then implement file-scope-aware automatic closeout and delivery-closeout semantics as the next governance hardening batch.

### HARN-006: Restore escalation failure-disposition wording

- Status: done
- Priority: 1
- Depends on: HARN-005
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-006 harden escalation and inbox audit chain`
- Scope: 在已恢复人工决策升级链“失败处置”显式语义的基础上，继续收口本轮严格模式复核发现的 4 个剩余问题：修正 `HARN-006` 的状态/证据口径、补强机器可检查的人类决策升级约束、补齐 `INBOX.md` 双向追溯格式与校验，并把验证日志从“closeout 证据”改回准确的工作树审计快照口径。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: review confirmed the former `R-158-A` failure-disposition wording no longer existed as explicit text after merge, and the current `R-156` / `R-160` wording had weakened to broad task-ledger or Git repair language.
  - 2026-04-21: restored explicit escalation-chain failure disposition wording in `docs/quality/validation-rules.md`, `docs/rules/codex-rules.md`, and `docs/operations/human-collaboration.md`.
  - 2026-04-21: user instructed to directly repair all 4 strict-mode review findings, which resolved the earlier closeout-strategy decision point and returned `HARN-006` to active execution.
  - 2026-04-21: current task remains active, so its validation evidence must be recorded as working-tree audit snapshots rather than closeout evidence until archival and single-task commit are complete.
  - 2026-04-21: updated `tasks.md`, `INBOX.md`, `docs/rules/codex-rules.md`, `docs/quality/validation-rules.md`, `docs/operations/human-collaboration.md`, `docs/operations/foreman-workflow.md`, and `scripts/task_audit.py` so unresolved human-decision markers are forbidden in `todo` / `in_progress`, `blocked` / `in_review` require `INBOX ref:`, and `INBOX` now has an explicit dual-traceability format.
  - 2026-04-21: reran `python3 -m py_compile scripts/task_audit.py`, `python3 scripts/task_audit.py --check --phase pre-closeout`, and `node scripts/lint-repository-knowledge.js`; current evidence is recorded as working-tree validation snapshots, not closeout evidence.
  - 2026-04-21: refined `task_audit.py` so `todo` / `in_progress` only fail on structured unresolved-human-decision fields, not on narrative text that merely mentions field names; reran py-compile, task audit, and repository knowledge lint after the fix.
  - 2026-04-21: tightened `task_audit.py` again so `blocked` / `in_review` now require structured field lines instead of loose substring matches, and `INBOX.md` entries are machine-checked for `Status:` / `Needed decision:` plus `Task refs:` / `Plan refs:` presence and task-id back references when a task uses `INBOX ref:`.
  - 2026-04-21: aligned `INBOX.md`, `docs/rules/codex-rules.md`, `docs/quality/validation-rules.md`, and `docs/operations/human-collaboration.md` so `Plan refs:` is allowed for plan-only items, but any task-linked `INBOX` item must list the task in `Task refs:`; reran py-compile, task audit, and repository knowledge lint after the alignment.
- Context closeout:
  - Completed scope: escalation failure-disposition wording, task-state upgrade rules, INBOX dual-traceability format, and machine-checkable escalation/inbox validation are now aligned across rule ledger, validation rules, operations docs, task ledger guidance, inbox guidance, and `task_audit.py`.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: semantic judgment about whether a specific narrative truly requires human escalation still cannot be fully automated; current checks enforce structured markers and known waiting phrases rather than full natural-language intent.
  - Next step: run immediate post-closeout audit after this single-task commit and confirm the archived `HARN-006` commit subject is visible in Git history.

### HARN-005: Close remaining strict-mode review gaps

- Status: done
- Priority: 1
- Depends on: HARN-004
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-005 close strict-mode review gaps`
- Scope: 收口严格模式复核剩余 4 条缺口：为 `task_audit` 增加 pre/post closeout phase、补 `HARN-004` 的 post-closeout 证据、把 `tasks-done.md` 的“最新归档在上”排序规则显式写入规则与流程并形成校验、以及为 `blocked` / `in_review` 增加人类决策升级链的机器可检查字段要求。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check --phase post-closeout`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: strict-mode full review confirmed 4 remaining gaps after `HARN-004`: missing `HARN-004` post-closeout evidence, no phase-specific `task_audit` entrypoint, implicit `tasks-done.md` ordering dependency, and no machine-checkable metadata for “must ask human” task states.
  - 2026-04-21: expanded `scripts/task_audit.py` with explicit `--phase pre-closeout|post-closeout`, newest-first done-ledger checks, and required `Human decision:` / `Review reason:` metadata for `blocked` and `in_review` tasks.
  - 2026-04-21: aligned `docs/rules/codex-rules.md`, `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, `docs/operations/human-collaboration.md`, `docs/quality/validation-rules.md`, `tasks.md`, and `docs/quality/validation-log.md`, and repaired the legacy `HARN-002` / `HARN-001` archive ordering drift so the new checks consume a consistent ledger.
- Context closeout:
  - Completed scope: phase-aware closeout auditing, archived-task ordering governance, and human-decision escalation metadata are now formalized in rules, operations docs, task ledger guidance, and machine checks; `HARN-004` also gained formal post-closeout evidence backfill.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check --phase post-closeout`; `python3 scripts/task_audit.py --check --phase pre-closeout`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: `R-165` now has stronger machine-checkable task-state enforcement, but semantic judgment about whether a specific ambiguity truly required escalation still cannot be fully automated.
  - Next step: continue future strict-mode automation only if higher-fidelity ambiguity detection can be added without creating noisy false positives.

### HARN-004: Tighten closeout audit boundary and post-closeout traceability

- Status: done
- Priority: 1
- Depends on: HARN-003
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-004 tighten closeout audit chain`
- Scope: 修复严格模式复核识别出的两个 `P0` 缺口与一个 `P1` 证据链缺口：收紧 `task_audit` 对 pending commit 的豁免边界，重排 closeout 流程使 `R-168` 在 pre-commit 审计前即可检查到当前归档任务，并补齐 post-closeout 可追溯要求的正式文档口径。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: strict-mode re-review confirmed that the previous pending-commit exception in `scripts/task_audit.py` was too broad because any same-day archived task could slip through when the worktree was dirty, which weakened `R-157` / `R-160`.
  - 2026-04-21: tightened `scripts/task_audit.py` so pre-commit closeout now allows at most one newest same-day archived task to be pending Git history, while all other `tasks-done.md` entries must remain fully traceable.
  - 2026-04-21: aligned `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, and `docs/quality/validation-rules.md` so the current closeout task must already be archived into `tasks-done.md` with `Context closeout` before pre-commit audit, and post-commit recheck is now an explicit required step.
- Context closeout:
  - Completed scope: closeout workflow, validation-rule wording, and task-audit enforcement were tightened so `R-168` now covers the current archived task during pre-commit audit, and the pending-commit exception is limited to the single current closeout task.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: strict mode still cannot fully automate semantic judgment for all “must ask human” scenarios; the strengthened audit now covers closeout shape and Git traceability, not every ambiguity class.
  - Next step: run immediate post-commit recheck for this task and confirm the archived `HARN-004` commit subject is now visible in Git history.

### HARN-003: Close strict-mode and context-closeout governance loop

- Status: done
- Priority: 1
- Depends on: DOC-GOV-005
- Completed at: 2026-04-21
- Commit subject: `docs(harness): HARN-003 close strict-mode governance loop`
- Scope: 将严格模式与任务收尾的上下文收缩/清理要求从单点规则扩展为完整治理链，补齐规则入口、operations 流程、验证规则、验证日志、历史约束、任务归档与机器审计的一致口径，并明确 `/contract`、`/clear` 仅为环境可选手段而非唯一工程要求。
- Validation:
  - `python3 -m py_compile scripts/task_audit.py`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-21: aligned `R-168` consumption across `docs/README.md`, `docs/architecture/init.md`, `docs/operations/README.md`, `docs/operations/foreman-workflow.md`, `docs/operations/git-and-task-closeout.md`, `docs/operations/human-collaboration.md`, and `docs/plans/implementation-readiness.md` so strict mode and task closeout now use one consistent Harness Engineering wording.
  - 2026-04-21: updated `docs/quality/validation-rules.md`, `scripts/task_audit.py`, and `scripts/lint-repository-knowledge.js` so repository checks now cover `R-168` closeout markers and validation index continuity, then repaired `docs/quality/validation-log.md` to keep the new governance evidence append-only.
  - 2026-04-21: appended the human decision trail in `docs/references/human-constraint-history.md`, backfilled `Context closeout` sections for applicable completed tasks, and archived this governance batch for single-task git closeout.
- Context closeout:
  - Completed scope: strict-mode and context-closeout governance is now closed across rule definitions, operations guidance, validation linkage, machine audit, history ledger, and append-only evidence.
  - Validation evidence: `python3 -m py_compile scripts/task_audit.py`; `python3 scripts/task_audit.py --check`; `node scripts/lint-repository-knowledge.js`
  - Residual risk: `R-165` default strict mode仍主要依赖文档治理与人工执行纪律，仓库目前只对其关键 closeout 结果通过 `R-168` 做机器校验，而未对所有“需人工决策”场景做全自动语义判定。
  - Next step: 后续若要继续提高严格模式自动化强度，应在不引入伪阳性的前提下，为 `INBOX.md` / 任务日志中的人工确认链增加更细粒度的静态校验。

### D-TASK-013: 落实敏感字段加密

- Status: done
- Priority: 1
- Depends on: D-TASK-011
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-013 implement sensitive data encryption baseline`
- Scope: 在 Phase-D 已稳定的 `config/result/history/export/audit` 追溯链基础上，补共享 AES-256 敏感字段保护能力，并把 `governance` 内的 config snapshot、execution result、query history、export record、audit log 与 `system_config` 接到统一受保护持久化入口；优先收口密码、token、key 的密文存储，以及审计/导出相关字段的脱敏落库基线，不提前展开完整配置中心 UI、外部 KMS 或其他服务的真实上报改造。
- Validation:
  - 库、日志、导出无明文
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after D-TASK-012 stabilized the real audit write path, so the traceability chain could move from “schema exists” to “sensitive leaves are actually protected before persistence”.
  - 2026-04-21: added shared AES-256 GCM crypto and sensitive-key classification utilities under `sqlforge-shared`, so governance and later services can reuse one envelope format instead of ad hoc masking rules.
  - 2026-04-21: introduced `GovernanceProtectedPersistenceService` to protect `config_snapshot.snapshot_payload`, `execution_result.result_payload`, `query_history.query_context`, `export_record.export_options`, `audit_log.request_params/response_summary`, and raw SQL ciphertext writes before mapper persistence.
  - 2026-04-21: extended `system_config` with `sensitive_flag/value_ciphertext/value_mask/encryption_algorithm/encryption_key_id` and added `SystemConfigMapper`, so password/token/key style settings now have a stable ciphertext storage baseline instead of competing for the plain `config_value` column.
  - 2026-04-21: switched the existing governance audit write path onto the protected persistence service, added tests covering encrypted traceability leaves, masked audit/export fields, ciphertext SQL history payloads, and sensitive system-config storage, then synced persistence, security, capability, truth, init, repo-map, validation log, and task ledger documents.
- Context closeout:
  - Completed scope: shared AES-256 sensitive-field protection entered `sqlforge-shared`, governance traceability persistence paths now write ciphertext or masked payloads, and related authority docs were synchronized.
  - Validation evidence: `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: external KMS, other service consumers, and UI/admin management paths still remain follow-up work outside this task scope.
  - Next step: continue Phase-D traceability and security follow-up tasks on the now-protected persistence baseline instead of reopening plaintext storage semantics.

### D-TASK-012: 落实审计日志链路

- Status: done
- Priority: 1
- Depends on: D-TASK-011
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-012 implement audit persistence chain`
- Scope: 基于 D-TASK-011 已固化的 `config/result/history/export/audit` 追溯链，把 `governance` 内部 `POST /api/governance/internal/audit/write` 从“只发消息”升级为“真实写入 `audit_log` + 保留审计事件扩散”，同时把当前 header-based stateless auth 的鉴权建立/释放接入 `LOGIN` / `LOGOUT` 审计落库，并优先覆盖 SQL 操作、登录登出、权限变更三类事件的真实写入路径与校验。
- Validation:
  - `R-113` 场景测试
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after D-TASK-011 stabilized the core traceability tables and mapper baseline, so audit no longer had to stay as a message-only placeholder.
  - 2026-04-21: added `GovernanceAuditTrailService` and upgraded `/api/governance/internal/audit/write` to synchronously insert into `audit_log`, validate optional `configSnapshotId/resultId/historyId/exportId` references, and keep `governance.audit.event` fan-out semantics.
  - 2026-04-21: extended `AuditWriteRequest` / `AuditWriteResponse` so callers can attach `sagaId`, traceability foreign keys, and desensitized request/response summaries while receiving a stable `auditId` back from the real persistence path.
  - 2026-04-21: connected `AuthInterceptor` to real audit writes so the current header-based stateless auth baseline now records `LOGIN` / `LOGOUT` audit events and failed pre-auth attempts in `audit_log`.
  - 2026-04-21: added dedicated audit-chain tests covering SQL operation writes, permission-change writes, login/logout/failure auth writes, and missing traceability reference rejection, then synced interface baseline, capability map, persistence baseline, access-control spec, truth baseline, init summary, repo map, validation log, and task ledger.
- Context closeout:
  - Completed scope: governance audit write path now persists real `audit_log` records, auth events are written through the same chain, and the audit contract/document truth was synchronized.
  - Validation evidence: `R-113` scenarios; `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: cross-service callers are still limited to the current transitional skeleton, and broader audit retention/reporting tooling remains future work.
  - Next step: build on the real audit persistence chain for subsequent traceability, compliance, and governance-service hardening tasks.

### D-TASK-011: 补齐核心表与关联键设计

- Status: done
- Priority: 1
- Depends on: Phase-C
- Completed at: 2026-04-21
- Commit subject: `feat(governance): D-TASK-011 define core traceability schema and keys`
- Scope: 在 `governance` 侧先固化 Phase-D 的事务型元数据底座，补齐 `config_snapshot`、`execution_result`、`query_history`、`export_record` 和扩展 `audit_log` 的主外键与追溯链；同步提供 `init-schema.sql`、增量 migration、Entity / Mapper XML、持久化权威文档与最小映射检查测试，不提前接入真实业务写入链、队列消费或导出引擎。
- Validation:
  - schema/sql/entity 映射检查
  - `mvn -B -pl governance -am clean compile`
  - `mvn -B -pl governance -am test`
  - `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-004` after query-execution、sql-optimization 和 benchmark-engine 的接口与模型骨架稳定，开始把 config/result/history/export/audit 的关系型底座一次固化。
  - 2026-04-21: extended `sql/init-schema.sql` with `config_snapshot`、`execution_result`、`query_history`、`export_record` and trace-aware `audit_log`, then added `sql/migrations/V20260421_011__core_traceability_chain.sql` as the compatible incremental DDL for published environments.
  - 2026-04-21: added governance traceability entities and MyBatis XML mappers so table names, key columns, and FK chain are now represented in Java persistence skeletons instead of living only in SQL comments.
  - 2026-04-21: created `docs/architecture/persistence.md` as the current authority for MySQL persistence, shared trace keys, migration policy, and schema-to-entity mapping, then synced README, truth baseline, coverage matrix, init summary, service capability map, and repo map.
  - 2026-04-21: added `TraceabilitySchemaMappingTest` to keep `init-schema.sql`, migration script, and mapper XML aligned before later tasks connect real repository writes, audit ingestion, export generation, and cross-service persistence flows.
- Context closeout:
  - Completed scope: Phase-D traceability schema, incremental migration, entities, XML mappers, mapping test, and persistence authority document were all established as the current relational baseline.
  - Validation evidence: schema/entity mapping checks; `mvn -B -pl governance -am clean compile`; `mvn -B -pl governance -am test`; `mvn -B -pl governance -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: real repository writes, export generation, and downstream cross-service persistence usage are still follow-up work on top of this schema baseline.
  - Next step: connect actual persistence chains and audit/export flows to the stabilized traceability tables instead of revisiting the schema contract.

### D-TASK-010: 实现压测报告查询接口

- Status: done
- Priority: 1
- Depends on: D-TASK-009
- Completed at: 2026-04-21
- Commit subject: `feat(benchmark-engine): D-TASK-010 add report query api skeleton`
- Scope: 在 `benchmark-engine` 中补 `GET /api/benchmark-engine/reports/{reportId}`，基于已落库的 placeholder `BenchmarkReport` 打通 JSON / PDF / HTML 三种查询形态；同步固化趋势图表、格式元数据、查询路径和原始数据下载占位路径，不提前接入真实导出引擎、文件存储或真实执行链路。
- Validation:
  - 报告 JSON/PDF/HTML/404/非法格式测试
  - `mvn -B -pl benchmark-engine -am clean compile`
  - `mvn -B -pl benchmark-engine -am test`
  - `mvn -B -pl benchmark-engine -am validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-003` after `D-TASK-009` stabilized placeholder task submit/poll flow and report persistence.
  - 2026-04-21: added `BenchmarkReportController` and `BenchmarkReportApplicationService` so `benchmark-engine` now exposes `GET /api/benchmark-engine/reports/{reportId}` with `format=JSON|PDF|HTML`.
  - 2026-04-21: upgraded `BenchmarkReportResponse` from model-only payload to report-query baseline by adding target engines, trend charts, available formats, report query path, and raw-data download placeholder path.
  - 2026-04-21: kept JSON as the structured default response, and rendered PDF / HTML as placeholder export bodies with stable content type and `Content-Disposition` metadata so later real templates can replace the renderer without reshaping the HTTP contract.
  - 2026-04-21: extended controller and service tests to cover JSON success, PDF success, HTML success, missing report 404, and invalid format 400, then synced README, interface baseline, capability map, truth baseline, C4, init summary, repo map, and validation log to `REPORT_QUERY_API_SKELETON`.
- Context closeout:
  - Completed scope: benchmark-engine now exposes stable report query skeletons for JSON/PDF/HTML and the report response contract includes format/query/download metadata for later real exporters.
  - Validation evidence: JSON/PDF/HTML/404/illegal-format tests; `mvn -B -pl benchmark-engine -am clean compile`; `mvn -B -pl benchmark-engine -am test`; `mvn -B -pl benchmark-engine -am validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: export rendering, file storage, and real benchmark execution/report generation are still placeholder follow-up work.
  - Next step: keep later benchmark tasks on the stable report-query HTTP contract without reshaping the current DTO/VO surface.

### D-TASK-009: 实现压测任务提交流程骨架

- Status: done
- Priority: 1
- Depends on: D-TASK-008
- Completed at: 2026-04-21
- Commit subject: `feat(benchmark-engine): D-TASK-009 add task submit and status api skeleton`
- Scope: 在 `benchmark-engine` 中补 `POST /api/benchmark-engine/tasks` 与 `GET /api/benchmark-engine/tasks/{taskId}`，用 in-memory placeholder repository 串通提交、轮询、失败路径和流程日志，同时把成功路径的占位报告落库，为后续 `D-TASK-010` 的报告查询接口保留稳定承载点，不提前接入真实调度、隔离执行或导出链路。
- Validation:
  - 提交/轮询/失败路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-21: instantiated from `Phase-D / D-STORY-003` after `D-TASK-008` stabilized the benchmark task/report model and contract baseline.
  - 2026-04-21: added `BenchmarkTaskController`, `BenchmarkTaskApplicationService`, repository contract, and `InMemoryBenchmarkTaskRepository` so `benchmark-engine` now exposes submit and polling HTTP skeletons on an independent carrier.
  - 2026-04-21: kept async semantics by returning a queued snapshot from `POST /api/benchmark-engine/tasks`, then running a deterministic placeholder lifecycle that can reach success or failure for stable poll-path tests.
  - 2026-04-21: enforced current isolation guardrails in the skeleton by rejecting `readonlyRequired=false` and `shadowEnvironmentMode=DISABLED`, while still preserving placeholder failure coverage via the explicit `FAIL_BENCHMARK` marker.
  - 2026-04-21: stored placeholder reports on successful runs so `D-TASK-010` can add report query HTTP contracts without reshaping the current task flow, then synced interface baseline, capability map, truth baseline, C4, init summary, README, repo map, and validation log to `ASYNC_TASK_API_SKELETON`.
- Context closeout:
  - Completed scope: benchmark-engine task submit/poll skeleton, placeholder lifecycle, isolation guardrails, and success-path report persistence baseline were all established on an independent carrier.
  - Validation evidence: submit/poll/failure-path tests; `mvn -B clean compile`; `mvn -B test`; `mvn -B validate pmd:pmd checkstyle:check`; `node scripts/check-frontend-backend-separation.js`; `node scripts/lint-repository-knowledge.js`; `python3 scripts/task_audit.py --check`
  - Residual risk: real scheduler, isolated execution workers, persistent storage, and export/report backends are still deferred follow-up work.
  - Next step: continue benchmark-engine delivery on the stabilized task submit/status contract and placeholder report carrier.

### D-TASK-008: 定义压测任务与报告模型

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(benchmark-engine): D-TASK-008 define benchmark task and report model`
- Scope: 建立 `benchmark-engine` 独立模块骨架，固化 `BASELINE` / `COMPARISON` / `REGRESSION_GUARD` 三类压测任务实体、状态与阶段流转、阈值评估、影子环境/只读/脱敏约束，以及后续提交/查询接口复用的基础 DTO / VO 和报告模型，不提前接入真实调度、执行、持久化或导出链路。
- Validation:
  - 模型与架构文档一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-003` to establish the third independent backend carrier instead of leaving benchmark semantics only in target architecture documents.
  - 2026-04-20: added `benchmark-engine` as a standalone Maven module with Spring Boot bootstrap, multi-profile configuration, logging baseline, and `application` / `domain` / `infrastructure` / `config` package skeleton.
  - 2026-04-20: solidified benchmark task types, lifecycle statuses, task-type-specific phase flow, threshold model, engine metric snapshot model, report aggregate, and explicit isolation fields for readonly, shadow environment, and desensitization.
  - 2026-04-20: added baseline submit/status/report DTO / VO objects plus `BenchmarkTaskModelApplicationService` so later `D-TASK-009` / `D-TASK-010` can reuse stable model contracts without reshaping the domain again.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `init.md`, `repo-map.md`, and `README.md` so the repository now treats `benchmark-engine` as current fact while keeping task submit flow, execution chain, and report query deferred.

### D-TASK-007: 输出优化建议结构

- Status: done
- Priority: 1
- Depends on: D-TASK-006
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-007 add structured optimization suggestion vo`
- Scope: 在 `sql-optimization` 中把 `OptimizationTaskStatusResponse` 的占位 `summary/error` 升级为正式的 `suggestion / failure` 结构，固化收益、成本、风险、工件和失败阶段输出，并让 `PARSE` / `REWRITE` / `ACCELERATION_SUGGESTION` 三类任务一次性对齐到统一响应模型。
- Validation:
  - 输出字段与文档一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` after `D-TASK-006` stabilized the async submit/poll API skeleton.
  - 2026-04-20: replaced the flat `summary/error` polling payload with structured `suggestion / failure` objects plus dedicated benefit, cost, risk, artifact, and failure VOs under the SQL optimization application contract.
  - 2026-04-20: kept the task aggregate and repository unchanged, and concentrated the behavioral change in `OptimizationTaskModelApplicationService` so each task type now emits distinct placeholder recommendation content while preserving the existing lifecycle semantics.
  - 2026-04-20: extended tests to assert rewrite success payload shape, failed placeholder payload shape, parse success suggestion structure, and failure risk metadata, then synced the interface baseline, capability map, truth baseline, and init summary to the new response contract.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-006: 实现任务提交与状态查询骨架

- Status: done
- Priority: 1
- Depends on: D-TASK-005
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-006 add task submit and status api skeleton`
- Scope: 在 `sql-optimization` 中补 `POST /api/sql-optimization/tasks` 与 `GET /api/sql-optimization/tasks/{taskId}`，用 in-memory placeholder repository 串通提交、轮询、失败路径和流程日志，同时保持当前实现停留在过渡骨架，不提前接入 MySQL、队列和真实回调。
- Validation:
  - 提交/轮询/失败路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` after `D-TASK-005` established the SQL optimization task model and contract baseline.
  - 2026-04-20: added `OptimizationTaskController`, `OptimizationTaskApplicationService`, repository contract, and `InMemoryOptimizationTaskRepository` so `sql-optimization` now exposes submit and polling HTTP skeletons on an independent carrier.
  - 2026-04-20: kept async semantics by returning a queued snapshot from `POST /api/sql-optimization/tasks`, then running a deterministic placeholder lifecycle that can reach success or failure for stable poll-path tests.
  - 2026-04-20: added integration tests for submit success, failed placeholder polling, not-found polling, invalid callback rejection, plus log-sampling tests for entry/state-change/end/exception traces.
  - 2026-04-20: synced interface baseline, capability map, truth baseline, C4, init summary, README, repo map, validation log, and task ledgers to the new `ASYNC_TASK_API_SKELETON` current fact.

### D-TASK-005: 固化异步优化任务模型

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(sql-optimization): D-TASK-005 solidify async optimization task model`
- Scope: 建立 `sql-optimization` 独立模块骨架，固化 `parse` / `rewrite` / `acceleration suggestion` 三类异步优化任务实体、生命周期状态、类型感知阶段流转，以及后续提交/轮询接口复用的基础 DTO/VO 和错误码基线，不提前接入真实 HTTP 入口、持久化、队列或审批链。
- Validation:
  - `R-121` 四项检查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-002` to start the SQL optimization service on an independent carrier instead of leaking async optimization concerns back into `governance` or `query-execution`.
  - 2026-04-20: added `sql-optimization` as a standalone Maven module with Spring Boot bootstrap, multi-profile configuration, logging baseline, and `application` / `domain` / `infrastructure` / `config` package skeleton.
  - 2026-04-20: solidified async task types, lifecycle statuses, task-type-specific phase flow, submission normalization, status-history records, and reusable submit/status contract objects plus shared SQL-optimization service code and error-code ownership.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `init.md`, `repo-map.md`, and `README.md` so the repository now treats `sql-optimization` as current fact while still deferring HTTP entrypoints, persistence, callbacks, and suggestion payload details to follow-up tasks.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### OPS-NAME-001: 全仓模块与工程命名去 Service 化整改

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(repo): OPS-NAME-001 remove Service suffix from engineering names`
- Scope: 以严格模式对全仓执行模块/工程/运行时命名整改，统一把 `governance-service`、`query-execution-service`、`sqlforge-common` 及相关运行标签、脚本、规则、需求、流程、验证日志、历史台账收敛到无 `Service` 后缀的工程命名，同时保留后端 `application` 包域、`controller`/`service` 实际分层与 `*Service` 服务类型命名，不丢失既有章节/步骤/流程/历史内容，并建立旧名到新名的权威映射。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated as the temporary highest-priority strict-mode naming remediation task; all feature work paused until repo-wide module, runtime, code, script, and document naming were synchronized.
  - 2026-04-20: renamed module directories, Maven modules, artifact ids, Spring application names, runtime labels, service-code constants, scripts, tests, rules, plans, validation logs, and historical ledgers from `*-service` / `sqlforge-common` engineering names to `governance` / `query-execution` / `sqlforge-shared`.
  - 2026-04-20: preserved backend layering semantics by keeping `application` as a package domain, `controller` and `service` as the actual inbound layers, and concrete service-layer types on `*Service` naming.
  - 2026-04-20: repaired the remaining wording tail by correcting the old-to-new mapping table and removing the last misleading `application service` layer phrasing, then reran repo-wide validation for single-task closeout.

### D-TASK-004: 增加异常回滚与运行日志

- Status: done
- Priority: 1
- Depends on: D-TASK-003
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-004 add rollback markers and flow logs`
- Scope: 给 `query-execution` 的最小同步执行闭环补入口/出口/异常/状态变更日志，并把 timeout/fallback 路径的本地回滚/补偿标记固化到当前响应模型与代码路径。
- Validation:
  - `R-123` 抽查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-003` committed the minimal synchronous execution loop in commit `ea25220`.
  - 2026-04-20: this task stayed inside the existing synchronous skeleton and only added observability plus local recovery markers; it did not open real database execution, distributed rollback, or governance audit integration.
  - 2026-04-20: extended `QueryExecutionApplicationService` with entry/exit/exception/state-change logs keyed by SQL fingerprint, kept SQL text out of logs, and added timeout/fallback local recovery markers through `retryPath.resultStatus/localRecoveryMarker/localRecoveryAction`.
  - 2026-04-20: added regression tests covering success logging, timeout rollback marker, fallback compensation marker, and exception logging, then synced architecture/truth/interface docs to the new observable execution baseline.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-003: 实现最小同步执行闭环

- Status: done
- Priority: 1
- Depends on: D-TASK-002
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-003 implement minimal sync execution loop`
- Scope: 在 `query-execution` 中把 `/api/query-execution/queries/execute` 接到最小同步执行路径，保持只读优先和确定性输出，不放开任意 SQL 执行。
- Validation:
  - 正常/超时/失败/降级路径测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-002` committed the public DTO / VO / error-code contract baseline in commit `0624dd0`.
  - 2026-04-20: implementation stayed inside the query-execution boundary and introduced a deterministic synchronous skeleton with read-only SQL guard, controlled route/fallback decisions, and no arbitrary SQL execution capability.
  - 2026-04-20: replaced the pure contract placeholder service with `QueryExecutionApplicationService`, added read-only assessment/guard logic, a deterministic execution adapter, and route handling for success, timeout, rejected-risk, and fallback-degraded paths.
  - 2026-04-20: synced service-interface, truth-baseline, architecture overview, capability map, and init docs so the repository now treats minimal synchronous execution as current fact while keeping governance calls, real adapters, rollback, and runtime logs deferred to follow-up work.
  - 2026-04-20: reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-002: 定义联机查询接口 DTO/VO/错误码

- Status: done
- Priority: 1
- Depends on: D-TASK-001
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-002 define online query api contracts`
- Scope: 在 `query-execution` 中固化联机查询的请求/响应 DTO、错误码区间和最小 HTTP 契约入口，保持契约优先，不提前引入真实执行引擎闭环。
- Validation:
  - `R-121` 四项检查通过
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` after `D-TASK-001` established the independent `query-execution` module baseline.
  - 2026-04-20: this task remained contract-first and limited to DTO/VO, error-code ownership, controller/service contract shape, and baseline interface documentation; the synchronous execution loop remains reserved for `D-TASK-003`.
  - 2026-04-20: added `QueryExecutionController`, request/response DTO/VO models, query execution status and policy enums, a contract service skeleton in the `application` package domain, shared query-execution error codes/service code, and contract tests covering valid request, validation failure, and pipeline-not-ready fallback behavior.
  - 2026-04-20: synced `service-interface-contract-baseline.md`, reran compile, test, static-check, separation, knowledge-lint, and pre-closeout task audit, then archived the task for single-task git closeout.

### D-TASK-001: 固化查询执行服务边界

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `feat(query-execution): D-TASK-001 solidify query execution service boundary`
- Scope: 建立 `query-execution` 独立模块骨架，固化路由、执行控制、轻量解析、轻量改写和已批准加速配置应用的服务边界，不把治理、异步优化或压测主流程重新混入本服务。
- Validation:
  - 服务边界与 ADR 一致性检查
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/check-frontend-backend-separation.js`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-D / D-STORY-001` under explicit human direction after the final `Phase-C` audit cleanup commit `68f7bb1`.
  - 2026-04-20: `Phase-C` exit gate remains blocked by coverage threshold and missing Sonar environment configuration; this task proceeded by explicit human direction and did not rewrite that gate state.
  - 2026-04-20: added `query-execution` as an independent Maven module with `application` package-domain plus `domain`/`infrastructure`/`config` skeleton, immutable boundary definition, multi-profile configuration, and the boundary service class `QueryExecutionBoundaryApplicationService` plus unit test.
  - 2026-04-20: synced `service-capability-map.md`, `c4-overview.md`, `document-truth-baseline.md`, `document-gap-matrix.md`, `master-execution-plan.md`, `frontend-backend-separation-baseline.md`, `repo-map.md`, `README.md`, and `docs/README.md` so the new service carrier is treated as current fact instead of a pure target.
  - 2026-04-20: validation passed with compile, test, static-check, frontend-backend separation check, knowledge lint, and pre-closeout task audit, then the task was archived for single-task git closeout.

### C-TASK-008: 落实租户配置与访问占位能力

- Status: done
- Priority: 2
- Depends on: C-TASK-007
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-008 tighten tenant access placeholder policy`
- Scope: 保持 phase 0 最小租户校验闭环，继续把 `governance` 收敛为公共管理服务基线，不扩散到其他目标微服务职责。
- Validation:
  - `mvn clean compile`
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after governance baseline hardening.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `TenantAccessLogic`、`TenantConfigApplicationService` 和治理内部租户/数据源检查接口已经存在并通过当前测试链验证，但数据源授权仍是 placeholder，完整角色与资源矩阵仍待后续实现，见 `IMP-005`。
  - 2026-04-20: replaced the old non-empty datasource placeholder with a governance-local explicit placeholder policy under `governance.access-control.placeholder`, added tenant-config role gating and platform-admin override in `TenantConfigApplicationService`, and kept all logic inside `governance`.
  - 2026-04-20: validation passed with `mvn clean compile`, `mvn test`, `node scripts/lint-repository-knowledge.js`, and `python3 scripts/task_audit.py --check`, then the task was archived for single-task git closeout.

### C-TASK-009: 规划审计、数据源、调度扩展点

- Status: done
- Priority: 2
- Depends on: C-TASK-008
- Completed at: 2026-04-20
- Commit subject: `feat(governance): C-TASK-009 close governance extension contracts`
- Scope: 只补治理扩展契约和骨架，不提前塞入完整业务实现；保持审计、数据源和调度扩展点的接口、错误码和文档一致。
- Validation:
  - 接口文档和错误码一致
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the governance extension-contract follow-up task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; internal governance contract endpoints for `tenant-scope`、`datasource-access`、`audit/write`、`schedule/extensions` 已存在且测试通过，但仍需把扩展点从当前骨架进一步收口到完整契约，见 `IMP-007`。
  - 2026-04-20: hardened `datasource-access/check`、`audit/write`、`schedule/extensions` contracts in `governance`, added explicit contract-stage / implementation-stage metadata, fixed audit required fields (`serviceCode`,`elapsedMs`,`sourceIp`,`userAgent`), and synced `service-interface-contract-baseline.md` plus `access-control-spec.md`.
  - 2026-04-20: validation passed with `mvn clean compile`, `mvn test`, `mvn validate pmd:pmd checkstyle:check`, and `node scripts/lint-repository-knowledge.js`; task remained `in_review` until the message-abstraction closeout under `C-TASK-005` was committed.
  - 2026-04-20: reran compile, test, static-check, knowledge-lint, and pre-closeout task audit after `C-TASK-005`, then archived the governance extension-contract task for single-task git closeout.

### E-TASK-009: 建立临时 AI 交付进度页路由与展示骨架

- Status: done
- Priority: 2
- Depends on: E-TASK-001, E-TASK-002, Phase-C
- Completed at: 2026-04-20
- Commit subject: `feat(frontend): E-TASK-009 complete temporary delivery progress page`
- Scope: 建立 `/delivery-progress` 临时只读页面，展示 AI 编码任务进度且与 `/dashboard` 分离；展示真值只来自 `tasks.md`、`tasks-done.md`、验证日志和执行计划派生快照。
- Validation:
  - `npm run build`
  - `npm run build -- --mode development --outDir dist-dev`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
  - `node scripts/lint-repository-knowledge.js`
  - 非生产路由可达
  - 生产默认隐藏
- Progress log:
  - 2026-04-20: instantiated from `Phase-E / E-STORY-004` as the temporary AI delivery progress page task.
  - 2026-04-20: started early by explicit human direction while the repository active wave remains `Phase-C`; execution must preserve `R-166` boundaries and keep `/dashboard` as the official business homepage.
  - 2026-04-20: extended the temporary page with runtime-flag semantics, temporary/non-production navigation badges, and ledger-derived sections for recent changes, pending blockers, and dependency chains without introducing a parallel state source.
  - 2026-04-20: completed current implementation and validation scope for `E-STORY-004`, appended validation evidence, and archived the task after single-task git closeout.

### C-TASK-001: 盘点应抽取的公共能力

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-001 establish document truth baseline`
- Scope: 对照 `sqlforge-shared`、`governance` 与文档边界，产出当前应收敛到 common 的能力清单，明确哪些能力仍留在业务模块。
- Validation:
  - `mvn clean compile`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first executable shared-foundation task.
  - 2026-04-20: `docs/plans/implementation-readiness.md` 与 `docs/architecture/service-capability-map.md` 已把 `sqlforge-shared` 应承载的公共能力和禁入边界显式盘点完成，并写入主计划和真值文档。
  - 2026-04-20: strict ledger reconciliation revalidated repository compile and knowledge lint, then archived the inventory task because its implementation and git-history evidence are both present.

### C-TASK-002: 建立 common 包结构

- Status: done
- Priority: 1
- Depends on: C-TASK-001
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-002 close shared package structure baseline`
- Scope: 按“领域目录 + 分层子目录”与公共层边界建立 `sqlforge-shared` 的目标包结构，不引入服务专属逻辑。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after shared-capability inventory.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `sqlforge-shared` 已形成 `async`、`audit`、`config`、`constants`、`context`、`exception`、`log`、`utils` 包结构，且当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: synced `document-truth-baseline.md` so the shared module no longer appears as a placeholder-only module, then archived the package-structure task for single-task git closeout.

### C-TASK-003: 迁移重复或散落能力

- Status: done
- Priority: 1
- Depends on: C-TASK-002
- Completed at: 2026-04-20
- Commit subject: `refactor(common): C-TASK-003 migrate scattered shared capabilities`
- Scope: 把共性能力迁移到 common，仅迁移共性能力，不破坏服务边界。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after common package structure settled.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 旧 `com.company.common` 与治理服务内部重复 common 能力已被当前工作树迁移到 `com.company.sqlforge.common` 并由 `governance` 消费，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 已通过，但单任务 git closeout 仍缺失，暂不归档。
  - 2026-04-20: removed the last tracked `com.company.common` sources from `sqlforge-shared` and updated `document-gap-matrix.md` so the shared-layer implementation no longer remains as an open gap.

### C-TASK-004: 对齐所有 application-*.yml 职责

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(R-144): sync kafka abstraction to compose, scripts, config, sql, docs, lint`
- Scope: 明确 dev/test/prod 配置职责，补齐 coverage 和 Sonar 执行入口所需的环境说明，不改变生产默认安全语义。
- Validation:
  - `bash scripts/run-coverage.sh --phase report-only`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` to align configuration responsibilities before deeper service hardening.
  - 2026-04-20: dev / main-test / test-resource / prod 的 `messaging.mode` 职责已分别固定为 `DATABASE` / `DATABASE` / `MOCK` / `KAFKA`，并同步到了本地部署和消息抽象文档。
  - 2026-04-20: strict ledger reconciliation reran `bash scripts/run-coverage.sh --phase report-only`, confirmed report generation and profile responsibility consistency, then archived the task.

### C-TASK-005: 固化消息抽象接口实现路线

- Status: done
- Priority: 2
- Depends on: C-TASK-004
- Completed at: 2026-04-20
- Commit subject: `feat(messaging): C-TASK-005 solidify messaging abstraction route`
- Scope: 统一消息接口、配置和实现切换，保持 `DATABASE` / `MOCK` / `KAFKA` 三种模式的边界与契约清晰。
- Validation:
  - mock/database/kafka 契约测试
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
  - `bash scripts/run-coverage.sh --phase report-only`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after profile responsibilities were aligned.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; `MessageProducer` / `MessageConsumer` 抽象、`MessagingConfig` 路由、`Database` / `Mock` / `Kafka` 实现及对应测试已在当前工作树落地，当前验证链 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 和 `bash scripts/run-coverage.sh --phase report-only` 已通过，但当前 Kafka 客户端接入尚未形成单任务 git closeout，暂不归档。
  - 2026-04-20: reran compile, test, static-check, knowledge-lint, coverage, and pre-closeout task audit evidence, then archived the messaging abstraction task for single-task git closeout.

### C-TASK-006: 完成消息流管理接口验证

- Status: done
- Priority: 2
- Depends on: C-TASK-005
- Completed at: 2026-04-20
- Commit subject: `feat(R-144): add database queue admin endpoints and runtime verification`
- Scope: 验证 retry/stats/manual smoke 管理接口，确保消息表与治理管理面闭环可用。
- Validation:
  - `mvn test`
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` after message abstraction routing was established.
  - 2026-04-20: `MessageAdminController`、`MessageAdminApplicationTest`、`scripts/manual-message-queue-smoke.sh`、`docs/deliveries/init-completion.md` 与 `docs/deployments/local-setup.md` 已形成管理接口、消息表和人工 smoke 的验证闭环。
  - 2026-04-20: strict ledger reconciliation revalidated the current test chain and knowledge lint, confirmed that runtime verification evidence is already documented, then archived the task.

### C-TASK-007: 对齐现有分层

- Status: done
- Priority: 2
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `refactor(governance): C-TASK-007 close layered governance baseline`
- Scope: 对齐 `governance` 当前 `application` 包域下的 `controller`/`service` 与 `domain`/`infrastructure` 分层，确保其继续向公共管理服务边界收敛。
- Validation:
  - `mvn -B clean compile`
  - `mvn -B test`
  - `mvn -B validate pmd:pmd checkstyle:check`
  - `node scripts/lint-repository-knowledge.js`
- Progress log:
  - 2026-04-20: instantiated from `Phase-C` as the first governance hardening task.
  - 2026-04-20: strict ledger reconciliation marked the task as partial; 当前工作树已形成 `application` 包域下的 `controller` / `service` 与 `domain` / `infrastructure` 分层、严格请求上下文校验与治理内部契约基线，并通过 `mvn clean compile`、`mvn test`、`mvn validate pmd:pmd checkstyle:check`、`node scripts/lint-repository-knowledge.js` 验证，但当前分层调整尚未完成单任务 git closeout，暂不归档。
  - 2026-04-20: removed the last `governance.common` remnants, added regression coverage for `HealthStatusApplicationService` and `MessageRetryResultVO`, and synced local delivery/setup notes with the protected governance admin paths and current Phase-C baseline summary.

### HARN-002: Close remaining harness doc drift

- Status: done
- Priority: 1
- Depends on: HARN-001
- Completed at: 2026-04-20
- Commit subject: `docs(harness): HARN-002 close remaining harness doc drift`
- Scope: align the root and docs README summaries with the current harness governance baseline and confirmed service direction.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: audited post-HARN-001 residual drift and isolated two remaining stale README summaries for targeted closeout.
  - 2026-04-20: updated `docs/README.md` to stop advertising stale rule and service counts, and updated `README.md` to describe the current repository baseline instead of the old initialization state.
  - 2026-04-20: validated repository knowledge lint and task audit, then archived the task for single-task commit closeout.

### DOC-GOV-001: Establish document truth baseline and readiness governance

- Status: done
- Priority: 1
- Depends on: HARN-002
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-001 establish document truth baseline`
- Scope: add the document truth baseline, implementation readiness spec, service capability map, governance retrospective template and baseline retrospective, then wire them into the docs entry points, master plan, coverage matrix, rule consumption notes, history ledger, repo map, and repository knowledge lint.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: audited the repository truth against the initialization architecture, rules, plans, ADR index, access-control spec, and deployment docs to isolate drift between current facts and confirmed targets.
  - 2026-04-20: added `document-truth-baseline.md`, `implementation-readiness.md`, `service-capability-map.md`, `retrospective-template.md`, and `document-governance-retrospective-2026-04-20.md`.
  - 2026-04-20: updated `docs/README.md`, `docs/plans/README.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-coverage-matrix.md`, `docs/architecture/init.md`, `docs/generated/repo-map.md`, `docs/rules/codex-rules.md`, `docs/references/human-constraint-history.md`, and `scripts/lint-repository-knowledge.js` to consume the new governance layer.
  - 2026-04-20: wrote baseline validation evidence into `docs/quality/validation-log.md`, confirmed documentation coverage completeness, and prepared the batch for git closeout.

### DOC-GOV-002: Close strict governance audit gaps

- Status: done
- Priority: 1
- Depends on: DOC-GOV-001
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-002 close strict audit gaps`
- Scope: close the remaining 7 governance audit gaps by adding explicit gap and prerequisite matrices, an interface contract baseline, a task governance extension matrix, a repair retrospective, updated rules and indices, and final task/git closeout consistency.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: re-audited the original governance plan against repository truth and isolated 7 still-open closure gaps around matrices, interface contracts, task-extension fields, and batch consistency.
  - 2026-04-20: added `document-gap-matrix.md`, `phase-prerequisite-matrix.md`, `service-interface-contract-baseline.md`, `task-governance-extension-matrix.md`, and `document-governance-repair-retrospective-2026-04-20.md`.
  - 2026-04-20: updated `docs/README.md`, `docs/plans/README.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-coverage-matrix.md`, `docs/plans/task-spec-matrix.md`, `docs/generated/repo-map.md`, `docs/rules/codex-rules.md`, `docs/references/human-constraint-history.md`, and `scripts/lint-repository-knowledge.js` to consume the new closure layer.
  - 2026-04-20: completed the governance batch audit chain by aligning `tasks-done.md`, `docs/quality/validation-log.md`, repair retrospective, and git closeout records.

### DOC-GOV-003: Close final readiness and closeout consistency gaps

- Status: done
- Priority: 1
- Depends on: DOC-GOV-002
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-003 close final readiness gaps`
- Scope: close the remaining micro consistency gaps by aligning implementation-readiness and docs read order with the prerequisite and task-governance matrices, then append the missing `DOC-GOV-002` task-audit closeout evidence and archive the repair batch.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: re-checked the 16-item governance audit list against repository truth and confirmed that only three residual consistency gaps remained after `DOC-GOV-002`.
  - 2026-04-20: updated `docs/plans/implementation-readiness.md` and `docs/README.md` so non-trivial execution order explicitly consumes `phase-prerequisite-matrix.md` and `task-governance-extension-matrix.md`.
  - 2026-04-20: appended follow-up closure notes to `docs/plans/document-governance-repair-retrospective-2026-04-20.md`, backfilled the missing `DOC-GOV-002 closeout task-audit` validation record, and prepared the batch for git closeout.

### DOC-GOV-004: Close docs authority wording drift

- Status: done
- Priority: 1
- Depends on: DOC-GOV-003
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-004 close docs authority drift`
- Scope: remove the last README-level authority wording drift by keeping `init.md` as the historical baseline entry while pointing current service-boundary and interface-contract execution authority to the explicit governance baseline documents.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: strict re-audit found one remaining main-entry wording drift in `docs/README.md`, where current authority was still described as coming directly from `init.md`.
  - 2026-04-20: updated `docs/README.md` to distinguish historical initialization baseline from current authority, and appended the closeout rationale to `docs/plans/document-governance-repair-retrospective-2026-04-20.md`.

### DOC-GOV-005: Align C4, process audit, and governance authority follow-ups

- Status: done
- Priority: 1
- Depends on: DOC-GOV-004
- Completed at: 2026-04-20
- Commit subject: `docs(governance): DOC-GOV-005 align c4 and process authority follow-ups`
- Scope: add the C4 authority document and process-flow governance audit, then align docs entry points, rule consumption notes, execution-plan wording, and history records so the strict-mode and temporary delivery-page follow-ups have explicit authority anchors.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: post-closeout drift review found a remaining governance bundle spanning C4 authority location, process-flow audit indexing, strict-mode (`R-165`) consumption notes, and the temporary delivery-page (`R-166`) authority trail.
  - 2026-04-20: added `docs/architecture/c4-overview.md` and `docs/plans/process-flow-and-governance-audit-2026-04-20.md`, then aligned `docs/README.md`, `docs/architecture/init.md`, `docs/plans/README.md`, `docs/plans/document-coverage-matrix.md`, `docs/plans/master-execution-plan.md`, `docs/plans/task-spec-matrix.md`, `docs/plans/task-governance-extension-matrix.md`, `docs/operations/human-collaboration.md`, `docs/quality/validation-rules.md`, `docs/references/human-constraint-history.md`, `docs/rules/codex-rules.md`, and `scripts/lint-repository-knowledge.js`.
  - 2026-04-20: reran repository knowledge lint and task audit, then archived the governance follow-up batch for single-task git closeout.

### OPS-GOV-001: Add local and CI quality-gate entrypoints

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-20
- Commit subject: `ops(ci): OPS-GOV-001 add quality gate entrypoints`
- Scope: add executable SonarQube and coverage entrypoints for local/CI use, wire them into repository helper commands, and let CI consume them when configuration is present.
- Validation:
  - `bash scripts/run-sonar.sh`
  - `bash scripts/run-coverage.sh --phase report-only`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-20: cleanup classification isolated the remaining CI/quality files from Phase-C feature work because they add delivery gate entrypoints rather than shared-foundation or governance functionality.
  - 2026-04-20: added `scripts/run-sonar.sh`, wired coverage/Sonar helpers into `.github/workflows/ci.yml`, `Makefile`, and `docs/operations/local-development.md`, and kept the Sonar path no-op safe when environment variables are not configured.
  - 2026-04-20: reran the Sonar helper in no-config mode, reran coverage report generation, and prepared the quality-gate batch for single-task git closeout.

### HARN-001: Codify foreman workflow and task ledger

- Status: done
- Priority: 1
- Depends on: none
- Completed at: 2026-04-19
- Commit subject: `chore(harness): HARN-001 codify foreman workflow and task ledger`
- Scope: add SQLForge task ledger, inbox, agent config, operations docs, generated repo map, validation log, task audit script, and append-only harness governance rules.
- Validation:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `python3 scripts/task_audit.py --check`
- Progress log:
  - 2026-04-19: audited current repository knowledge, rule continuity, validation constraints, and root Git boundary before applying harness governance changes.
  - 2026-04-19: added task ledger files, operations docs, generated repo map, agent config, validation log, exec-plan directories, and task audit automation.
  - 2026-04-19: appended `R-156` to `R-161`, updated documentation entry points, and aligned repository knowledge lint with the new governance baseline.
  - 2026-04-19: validated repository knowledge lint, frontend-backend separation, and task audit, then archived the task for single-task commit closeout.
