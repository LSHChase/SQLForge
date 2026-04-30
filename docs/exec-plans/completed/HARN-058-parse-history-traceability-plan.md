## Candidate Execution Plan: HARN-058

- Task: `HARN-058`
- Title: 修复解析历史记录投影与报表导入明细
- Scope: 修复单条 SQL、批量解析和报表导入解析记录在解析历史页不可识别/不可筛选的问题，补齐 query_history 投影上下文、报表导入 SQL 级与报表级解析明细展示、测试与文档。

## Execution Steps

1. 通过 `python3 scripts/foreman.py preflight` 和任务台账确认 HARN-058 上下文。
2. 核对单条 SQL、批量解析、报表导入到 governance `query_history` 的写入链路。
3. 将 `GovernanceParseHistoryTraceabilityApplicationService` 的 query history 写入改为按 `historyId` upsert，确保综合解析后续 access 结果能合并回同一历史记录。
4. 归一化 parse history 的 `queryContext`：补齐 `commentContext`、`queryDateSummary`、`logicalObjectHits`、`bindingSummary`、`structureParseSummary` 和 `accessParseSummary`。
5. 为 legacy 扁平 `commentContext` 增加投影 fallback，避免旧写入形态导致 `report_code`、`stage`、`biz_date` 不可筛选。
6. 在批量解析和报表导入 access parse 完成后回写合并解析历史。
7. 在批量解析中心补齐报表导入 SQL 级详情和报表级统计，包括 SQL 文本、parseTaskId、结构/access 状态、问题场景和逻辑对象命中。
8. 在解析历史页增加报表导入详情抽屉，并支持 SQL 解析页通过 `historyId` 直接打开历史详情。
9. 补充单元测试、前端 contract 脚本、文档和验证日志。
10. 执行 `python3 scripts/foreman.py validate HARN-058`、`python3 scripts/task_audit.py --check --phase pre-closeout`，再 closeout。

## Constraints

- 不创建重复 `query_history` 行；同一 `parseTaskId` 固定映射到 `history-parse-<parseTaskId>`。
- 不把报表导入详情只放在批量中心；解析历史页必须能直接查看报表导入批次与 SQL 级解析证据。
- 不回退 HARN-057 的解析历史默认空查询条件。
- 不把加密/脱敏后的敏感字段还原到前端展示。

## Validation

- `mvn -pl governance -Dtest=GovernanceParseHistoryTraceabilityApplicationServiceTest,GovernanceProtectedPersistenceServiceTest,TraceabilitySchemaMappingTest test`
- `mvn -pl sql-optimization -Dtest=ParseBatchApplicationServiceTest,ReportBatchApplicationServiceTest test`
- `node scripts/check-history-page-contract.mjs`
- `node scripts/check-history-detail-contract.mjs`
- `node scripts/check-batch-import-contract.mjs`
- `node scripts/check-parse-workbench-contract.mjs`
- `npm run build`
- `python3 scripts/foreman.py validate HARN-058`
- `python3 scripts/task_audit.py --check --phase pre-closeout`
