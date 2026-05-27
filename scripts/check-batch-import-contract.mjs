import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const batchViewPath = 'src/views/parse-batch/ParseBatchCenterView.vue'
const contractFiles = [
  batchViewPath,
  'src/views/parse-batch/useParseBatchCenter.js',
  'src/views/parse-batch/BatchSummaryCards.vue',
  'src/views/parse-batch/BatchDetailFields.vue',
  'src/views/common/issueSceneHelp.mjs',
  'src/locales/zh-CN.js',
  'src/locales/en-US.js',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/service/ParseBatchApplicationService.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/service/ReportBatchApplicationService.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/service/ReportBatchParseStatisticsAssembler.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ParseBatchStatusResponse.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ParseBatchItemVO.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ReportBatchStatusResponse.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ReportBatchItemVO.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ParseReportStatisticVO.java',
  'sql-optimization/src/main/java/com/company/sqloptimization/application/controller/vo/ReportBatchParseStatisticsVO.java'
]
const source = contractFiles
  .map(filePath => fs.readFileSync(path.join(root, filePath), 'utf8'))
  .join('\n')
const viewSource = fs.readFileSync(path.join(root, batchViewPath), 'utf8')

const requiredTokens = [
  'data-testid="batch-import-page"',
  'current batch workbench',
  '创建与导入参数都在弹窗中完成。',
  'Create and ingest parameters stay in dialogs.',
  'report catalog import',
  'data-testid="batch-import-current-workbench"',
  'data-testid="batch-import-create"',
  'data-testid="batch-import-parse-batch-parser-mode"',
  'data-testid="batch-import-report-batch-parser-mode"',
  'data-testid="batch-import-download-template"',
  'data-testid="batch-import-dialog-sql-input"',
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
  'data-testid="batch-import-report-import"',
  'data-testid="batch-import-report-dialog-sql-input"',
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
  "activeReportResultTab = ref('groups')",
  "activeReportStatisticsTab = ref('issueScene')",
  "activeReportResultTab.value = 'groups'",
  "activeReportStatisticsTab.value = 'issueScene'",
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
  'SQL_TOO_LONG',
  'REPORT_SQL_MERGE_CANDIDATE',
  'mergeCandidateReportCount',
  'mergeCandidateReason',
  '风险含义：',
  '原因：',
  '建议：',
  'aria-label="issue scene help"',
  'parseAccessIfPossible',
  'writeParseHistoryWithAccess',
  'splitSqlStatements',
  'omittedItemCount',
  'omittedFailureCount',
  'omittedSqlStatisticCount',
  'parserModeOptions',
  'Apache Calcite',
  'APACHE_CALCITE',
  'APACHE_CALCITE_WITH_PLAN',
  'planAnalysisStatus',
  'planAnalysisStatistics',
  'analysisStatus',
  'parserMode: parseBatchForm.parserMode',
  'parserMode: reportBatchForm.parserMode',
  '解析工具 / Parser tool'
]

const missing = requiredTokens.filter(token => !source.includes(token))
const forbiddenTokens = [
  '<el-button text size="small" class="help-dot" aria-label="issue scene help"',
  'data-testid="batch-import-current-input"',
  'data-testid="batch-import-report-current-input"',
  'data-testid="batch-import-current-sql-input"',
  'data-testid="batch-import-report-current-sql-input"',
  'class="shell-panel input-rail"'
]
const forbidden = forbiddenTokens.filter(token => source.includes(token))

const sliceAfter = (content, token, endToken) => {
  const start = content.indexOf(token)
  if (start < 0) {
    return ''
  }
  const end = content.indexOf(endToken, start)
  return end > start ? content.slice(start, end) : content.slice(start)
}

const reportResultTabsSource = sliceAfter(
  viewSource,
  'data-testid="batch-import-report-result-tabs"',
  'data-testid="batch-import-report-group-detail-dialog"'
)
const reportStatisticsTabsSource = sliceAfter(
  viewSource,
  'data-testid="batch-import-report-statistics-tabs"',
  '</el-tabs>'
)
const reportResultForbiddenTokens = [
  'name="overview"',
  "isChinese ? '概览' : 'Overview'"
].filter(token => reportResultTabsSource.includes(token))
const reportStatisticsForbiddenTokens = [
  'name="overview"',
  "isChinese ? '概览' : 'Overview'",
  'data-testid="batch-import-report-drawer-statistics"'
].filter(token => reportStatisticsTabsSource.includes(token))

if (
  missing.length > 0 ||
  forbidden.length > 0 ||
  reportResultForbiddenTokens.length > 0 ||
  reportStatisticsForbiddenTokens.length > 0
) {
  console.error('Batch import contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of forbidden) {
    console.error(`- forbidden token: ${token}`)
  }
  for (const token of reportResultForbiddenTokens) {
    console.error(`- forbidden whole report SQL detail overview token: ${token}`)
  }
  for (const token of reportStatisticsForbiddenTokens) {
    console.error(`- forbidden report statistics overview token: ${token}`)
  }
  process.exit(1)
}

console.log('Batch import contract check passed.')
