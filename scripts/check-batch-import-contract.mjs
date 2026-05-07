import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const contractFiles = [
  'src/views/parse-batch/ParseBatchCenterView.vue',
  'src/views/common/issueSceneHelp.mjs',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/service/ParseBatchApplicationService.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/service/ReportBatchApplicationService.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/service/ReportBatchParseStatisticsAssembler.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ParseBatchStatusResponse.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ParseBatchItemVO.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ReportBatchStatusResponse.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ReportBatchItemVO.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ReportBatchParseStatisticsVO.java'
]
const source = contractFiles
  .map(filePath => fs.readFileSync(path.join(root, filePath), 'utf8'))
  .join('\n')

const requiredTokens = [
  'data-testid="batch-import-page"',
  'current batch workbench',
  'input sql',
  'report catalog import',
  'data-testid="batch-import-current-workbench"',
  'data-testid="batch-import-current-input"',
  'data-testid="batch-import-create"',
  'data-testid="batch-import-download-template"',
  'data-testid="batch-import-parse-result-tabs"',
  'data-testid="batch-import-parse-statistics-tabs"',
  'data-testid="batch-import-ingest"',
  'data-testid="batch-import-retry-access"',
  'batch-import-parse-batch-item',
  'data-testid="batch-import-parse-detail"',
  'data-testid="batch-import-parse-statistics"',
  'data-testid="batch-import-failure-record"',
  'data-testid="batch-import-parse-failure-detail-open"',
  'data-testid="batch-import-parse-item-detail-open"',
  'data-testid="batch-import-parse-item-detail"',
  'data-testid="batch-import-report-current-workbench"',
  'data-testid="batch-import-report-current-input"',
  'data-testid="batch-import-report-import"',
  'data-testid="batch-import-report-template"',
  'data-testid="batch-import-report-resolve"',
  'batch-import-report-item',
  'data-testid="batch-import-report-group-open"',
  'data-testid="batch-import-report-batch-sql-detail"',
  'data-testid="batch-import-report-statistics"',
  'data-testid="batch-import-report-sql-statistics"',
  'data-testid="batch-import-report-sql-detail"',
  'data-testid="batch-import-report-group-detail-dialog"',
  'data-testid="batch-import-report-result-tabs"',
  'data-testid="batch-import-report-group-tabs"',
  'data-testid="batch-import-report-scoped-sql-detail"',
  'data-testid="batch-import-report-scoped-sql-row"',
  'data-testid="batch-import-report-failure-record"',
  'data-testid="batch-import-report-drawer-statistics"',
  'data-testid="batch-import-report-drawer-sql-detail"',
  'data-testid="batch-import-report-statistics-issue-scene"',
  'data-testid="batch-import-report-statistics-importance"',
  'data-testid="batch-import-report-statistics-report-view"',
  'data-testid="batch-import-report-statistics-sql-list"',
  'data-testid="batch-import-report-statistics-priority"',
  'data-testid="batch-import-report-statistics-logical-object"',
  'data-testid="batch-import-report-item-detail-open"',
  'data-testid="batch-import-report-item-detail"',
  'data-testid="batch-import-parse-diagnostic"',
  'data-testid="batch-import-report-diagnostic"',
  'getReportBatchParseStatistics',
  'report_code,sql_1,sql_2,sql_3,...,sql_100',
  'Template-column contract',
  'Failure records',
  'failed sql detail',
  'SQL-level parse detail',
  'report-level statistics',
  'parseStatistics',
  'data-testid="batch-import-large-batch-preview"',
  'data-testid="batch-import-direct-sql-preview-truncated"',
  'data-testid="batch-import-report-large-batch-preview"',
  'data-testid="batch-import-report-sql-statistics-preview"',
  'DETAIL_PREVIEW_LIMIT',
  'STATISTIC_PREVIEW_LIMIT',
  'ITEM_PREVIEW_LIMIT',
  'FAILURE_PREVIEW_LIMIT',
  'SQL_STATISTIC_PREVIEW_LIMIT',
  'SqlParseDiagnosticSupport',
  'diagnosticSummary',
  'historyId',
  'historyPersistenceStatus',
  'failureLine',
  'failureColumn',
  'failureToken',
  'failureSnippet',
  'issueLocations',
  'locationSnippet',
  'itemPageNumber',
  'itemPageSize',
  'itemTotalCount',
  'sqlStatisticPageNumber',
  'sqlStatisticTotalCount',
  'data-testid="batch-import-report-sql-filter"',
  'data-testid="batch-import-report-statistics-sql-filter"',
  'issueLocationText',
  'help-dot',
  'issueSceneHelpText',
  'issueSceneCodesForItem',
  'issueSceneListHelp',
  'issue-scene-help',
  'REPEATED_TABLE_SCAN_RISK',
  '风险含义：',
  '原因：',
  '建议：',
  'aria-label="issue scene help"',
  'parseAccessIfPossible',
  'writeParseHistoryWithAccess',
  'splitSqlStatements',
  'omittedItemCount',
  'omittedFailureCount',
  'omittedSqlStatisticCount'
]

const missing = requiredTokens.filter(token => !source.includes(token))
const forbiddenTokens = [
  '<el-button text size="small" class="help-dot" aria-label="issue scene help"'
]
const forbidden = forbiddenTokens.filter(token => source.includes(token))

if (missing.length > 0 || forbidden.length > 0) {
  console.error('Batch import contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of forbidden) {
    console.error(`- forbidden token: ${token}`)
  }
  process.exit(1)
}

console.log('Batch import contract check passed.')
