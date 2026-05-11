import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const parseRecordDir = path.join(root, 'src/views/parse-record')
const sqlHistoryViewPath = path.join(root, 'src/views/sql-history/SqlHistoryView.vue')
const sqlHistoryListPath = path.join(root, 'src/views/sql-history/useSqlHistoryList.js')
const apiPath = path.join(root, 'src/services/runtimeGateApi.js')
const helperPath = path.join(root, 'src/views/common/issueSceneHelp.mjs')
const routePathsPath = path.join(root, 'src/config/routePaths.mjs')
const routerPath = path.join(root, 'src/router/index.js')
const mainPath = path.join(root, 'src/main.js')
const readParseRecordSource = () => fs
  .readdirSync(parseRecordDir)
  .filter(file => /\.(?:vue|js|css)$/.test(file))
  .sort()
  .map(file => fs.readFileSync(path.join(parseRecordDir, file), 'utf8'))
  .join('\n')

const parseRecordSource = readParseRecordSource()
const source = [
  parseRecordSource,
  fs.readFileSync(sqlHistoryViewPath, 'utf8'),
  fs.readFileSync(helperPath, 'utf8')
].join('\n')
const sqlHistorySource = fs.readFileSync(sqlHistoryViewPath, 'utf8')
const sqlHistoryListSource = fs.readFileSync(sqlHistoryListPath, 'utf8')
const sqlHistoryContractSource = `${sqlHistorySource}\n${sqlHistoryListSource}`
const apiSource = fs.readFileSync(apiPath, 'utf8')
const routePathsSource = fs.readFileSync(routePathsPath, 'utf8')
const routerSource = fs.readFileSync(routerPath, 'utf8')
const mainSource = fs.readFileSync(mainPath, 'utf8')
const pageApiStart = apiSource.indexOf('export const getGovernanceQueryHistoryPage')
const pageApiEnd = apiSource.indexOf('export const getHetuRouteCalibration')
const queryHistoryPageApiSource = pageApiStart >= 0 && pageApiEnd > pageApiStart
  ? apiSource.slice(pageApiStart, pageApiEnd)
  : ''

const requiredTokens = [
  'data-testid="parse-record-page"',
  'data-testid="parse-record-refresh"',
  'data-testid="parse-record-run-lookup"',
  'data-testid="parse-record-filter-select"',
  'data-testid="parse-record-sort-select"',
  'data-testid="parse-record-status-filter"',
  'data-testid="parse-record-datasource-filter"',
  'data-testid="parse-record-page-mode"',
  'data-testid="parse-record-trace-item"',
  'data-testid="parse-record-detail-trace-id"',
  'data-testid="parse-record-export"',
  'data-testid="parse-record-export-result"',
  'activeHistoryWorkbenchTab',
  "ref('sqlHistory')",
  'data-testid="parse-record-history-workbench-tabs"',
  'data-testid="parse-record-sql-history-tab"',
  'data-testid="parse-record-batch-report-history-tab"',
  'getSqlParseHistoryPage',
  'frontend-parse-record-parse-history-page',
  'name="sqlHistory"',
  'name="batchHistory"',
  'data-testid="parse-record-report-batch-detail"',
  'data-testid="parse-record-report-detail-tabs"',
  'data-testid="parse-record-report-group"',
  'data-testid="parse-record-report-sql-detail"',
  'data-testid="parse-record-report-sql-filter"',
  'data-testid="parse-record-report-sql-detail-statistics"',
  'data-testid="parse-record-report-statistics-issue-scene"',
  'data-testid="parse-record-report-statistics-importance"',
  'data-testid="parse-record-report-statistics-report-view"',
  'data-testid="parse-record-report-statistics-sql-list"',
  'data-testid="parse-record-report-statistics-priority"',
  'data-testid="parse-record-report-statistics-logical-object"',
  'data-testid="parse-record-report-sql-parse-detail"',
  'data-testid="parse-record-report-sql-risk"',
  'data-testid="parse-record-report-sql-issue"',
  'loadReportBatchItemDetail',
  'reportHistoryIsPersisted',
  'reportItemLocalDetail',
  'buildReportItemFallbackDetail',
  'getReportBatchParseStatistics',
  'frontend-parse-record-report-sql-parse-history-detail',
  'frontend-parse-record-report-batch-statistics',
  "activeReportBatchDetailTab.value = 'statistics'",
  "activeReportBatchStatisticsTab.value = 'issueScene'",
  'issueLocationText',
  'issueSceneHelpText',
  'issueSceneCodesForItem',
  'issue-scene-help',
  'SQL_TOO_LONG',
  'issue-scene-code-button',
  'aria-label="issue scene help"',
  'itemTotalCount',
  'SQL parse history table',
  'batch history',
  'parse record workbench',
  "tenantId: ''",
  "sortBy: ''",
  "sortOrder: ''",
  'requestTenantId: requestTenantId.value',
  'data-testid="parse-record-engine-filter"'
]

const requiredSqlHistoryTokens = [
  'data-testid="sql-history-page"',
  'data-testid="sql-history-refresh"',
  'data-testid="sql-history-run-lookup"',
  'data-testid="sql-history-query-history-table"',
  'data-testid="sql-history-pagination"',
  'data-testid="sql-history-datasource-options-fallback"',
  'data-testid="sql-history-trace-item"',
  'data-testid="sql-history-detail-drawer"',
  'data-testid="sql-history-detail-tabs"',
  "testId: 'sql-history-has-rewrite-record-filter'",
  "testId: 'sql-history-rewrite-validation-status-filter'",
  "testId: 'sql-history-rewrite-source-type-filter'",
  "testId: 'sql-history-recommendation-id-filter'",
  'data-testid="sql-history-rewrite-records-tab"',
  'data-testid="sql-history-rewrite-record-table"',
  'data-testid="sql-history-rewrite-record-diff"',
  'data-testid="sql-history-rewrite-record-original-sql"',
  'data-testid="sql-history-detail-service-code"',
  "testId: 'sql-history-detail-status'",
  "testId: 'sql-history-detail-target-engine'",
  "testId: 'sql-history-detail-sql-fingerprint'",
  "testId: 'sql-history-detail-audit-count'",
  'data-testid="sql-history-export"',
  'data-testid="sql-history-export-result"',
  'useSqlHistoryList',
  'getQueryHistoryRewriteRecords',
  'frontend-sql-history-rewrite-records',
  '@submit.prevent="search"',
  "const SQL_EXECUTION_HISTORY_TYPE = 'QUERY_EXECUTION'",
  'historyType: SQL_EXECUTION_HISTORY_TYPE',
  'v-model:current-page="pageInfo.currentPage"',
  'v-model:page-size="pageInfo.pageSize"',
  'layout="total, sizes, prev, pager, next, jumper"',
  "activeDetailTab.value = 'overview'",
  "label: t('sqlHistory.table.requestTenant')",
  "slot: 'requestTenant'",
  'row.tenantId || requestTenantId',
  "name=\"rewriteRecords\"",
  "key: 'hasRewriteRecord'",
  "key: 'rewriteValidationStatus'",
  "key: 'rewriteSourceType'",
  "key: 'recommendationId'",
  'frontend-sql-history-page',
  'frontend-sql-history-detail',
  'QUERY_EXECUTION history'
]

const requiredApiTokens = [
  'const filterTenantId = normalizeTenantId(filters.tenantId)',
  'const requestTenantId = normalizeTenantId(filters.requestTenantId) || filterTenantId',
  'if (filterTenantId) {',
  "params.set('tenantId', filterTenantId)",
  'tenantId: requestTenantId',
  "'historyType'",
  "'hasRewriteRecord'",
  "'rewriteValidationStatus'",
  "'rewriteSourceType'",
  "'recommendationId'"
]

const requiredSharedApiTokens = [
  'buildTenantQuerySuffix',
  'export const getSqlParseHistoryPage',
  '/api/sql-optimization/parse-history',
  'frontend-sql-parse-history-page',
  'export const getSqlParseHistoryDetail',
  'export const exportSqlParseHistory',
  'frontend-report-batch-parse-statistics',
  '/parse-statistics'
]

const requiredRouteTokens = [
  "sqlHistory: '/governance/history/sql-history'",
  "parseRecord: '/governance/history/parse-record'",
  "componentRoute(\n    'sqlHistory'",
  "'SqlHistory'",
  "'SqlHistoryView'",
  "const SqlHistoryView = () => import('../views/sql-history/SqlHistoryView.vue')",
  "componentRoute(\n    'parseRecord'",
  "'ParseRecord'",
  "'ParseRecordView'",
  "historyWorkbenchTab: 'batchHistory'"
]

const requiredMainTokens = [
  'ElPagination',
  'ElForm',
  'ElFormItem',
  'ElEmpty',
  'ElCheckbox',
  'app.use(ElLoading)'
]

const forbiddenViewTokens = [
  "tenantId: 'tenant-a'",
  "sortBy: 'submittedAt'",
  "sortOrder: 'DESC'",
  '<el-button text size="small" class="help-dot" aria-label="issue scene help"',
  'aria-label="risk help"',
  'reportHistoryIdForTask',
  'history-parse-${',
  'name="issueSceneDetail"',
  "historyType: 'SQL_PARSE'",
  'parse query-history workbench',
  'SQL_PARSE query history table',
  'getGovernanceQueryHistoryPage',
  'getGovernanceQueryHistoryDetail',
  'exportGovernanceQueryHistory',
  'historyWorkbenchSummary'
]

const forbiddenSqlHistoryTokens = [
  'operationStatusItems',
  'class="operation-bar"',
  "t('sqlHistory.executionSummary')",
  'sqlHistory.statusBar'
]

const forbiddenApiTokens = [
  "params.set('tenantId', tenantId)"
]

const missing = requiredTokens.filter(token => !source.includes(token))
const missingSqlHistory = requiredSqlHistoryTokens.filter(token => !sqlHistoryContractSource.includes(token))
const missingApi = requiredApiTokens.filter(token => !queryHistoryPageApiSource.includes(token))
const missingSharedApi = requiredSharedApiTokens.filter(token => !apiSource.includes(token))
const routeSource = `${routePathsSource}\n${routerSource}`
const missingRoute = requiredRouteTokens.filter(token => !routeSource.includes(token))
const missingMain = requiredMainTokens.filter(token => !mainSource.includes(token))
const forbiddenView = forbiddenViewTokens.filter(token => parseRecordSource.includes(token))
const forbiddenSqlHistory = forbiddenSqlHistoryTokens.filter(token => sqlHistorySource.includes(token))
const forbiddenApi = forbiddenApiTokens.filter(token => queryHistoryPageApiSource.includes(token))

if (missing.length > 0 || missingSqlHistory.length > 0 || missingApi.length > 0 || missingSharedApi.length > 0 || missingRoute.length > 0 || missingMain.length > 0 || forbiddenView.length > 0 || forbiddenSqlHistory.length > 0 || forbiddenApi.length > 0) {
  console.error('History page contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of missingSqlHistory) {
    console.error(`- missing SQL history token: ${token}`)
  }
  for (const token of missingApi) {
    console.error(`- missing API token: ${token}`)
  }
  for (const token of missingSharedApi) {
    console.error(`- missing shared API token: ${token}`)
  }
  for (const token of missingRoute) {
    console.error(`- missing route token: ${token}`)
  }
  for (const token of missingMain) {
    console.error(`- missing main registration token: ${token}`)
  }
  for (const token of forbiddenView) {
    console.error(`- forbidden default filter token in view: ${token}`)
  }
  for (const token of forbiddenSqlHistory) {
    console.error(`- forbidden SQL history token in view: ${token}`)
  }
  for (const token of forbiddenApi) {
    console.error(`- forbidden unconditional tenant query token in API: ${token}`)
  }
  process.exit(1)
}

console.log('History page contract check passed.')
