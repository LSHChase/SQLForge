import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/parse-record/ParseRecordView.vue')
const sqlHistoryViewPath = path.join(root, 'src/views/sql-history/SqlHistoryView.vue')
const apiPath = path.join(root, 'src/services/runtimeGateApi.js')
const helperPath = path.join(root, 'src/views/common/issueSceneHelp.mjs')
const routePathsPath = path.join(root, 'src/config/routePaths.mjs')
const routerPath = path.join(root, 'src/router/index.js')
const source = [
  fs.readFileSync(viewPath, 'utf8'),
  fs.readFileSync(sqlHistoryViewPath, 'utf8'),
  fs.readFileSync(helperPath, 'utf8')
].join('\n')
const parseRecordSource = fs.readFileSync(viewPath, 'utf8')
const sqlHistorySource = fs.readFileSync(sqlHistoryViewPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
const routePathsSource = fs.readFileSync(routePathsPath, 'utf8')
const routerSource = fs.readFileSync(routerPath, 'utf8')
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
  'data-testid="parse-record-bool-filter"',
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
  "historyType: 'SQL_PARSE'",
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
  'frontend-parse-record-report-sql-history-detail',
  'frontend-parse-record-report-batch-statistics',
  "activeReportBatchDetailTab.value = 'statistics'",
  "activeReportBatchStatisticsTab.value = 'issueScene'",
  'issueLocationText',
  'issueSceneHelpText',
  'issueSceneCodesForItem',
  'issue-scene-help',
  'issue-scene-code-button',
  'aria-label="issue scene help"',
  'itemTotalCount',
  'History classification',
  'Sort mode',
  'query history table',
  'batch history',
  'parse query-history workbench',
  'Export evidence',
  'Report-level parse statistics',
  'SQL-level parse detail',
  'Logical objects',
  'Governance hits',
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
  'data-testid="sql-history-trace-item"',
  'data-testid="sql-history-detail-drawer"',
  'data-testid="sql-history-detail-tabs"',
  'data-testid="sql-history-detail-service-code"',
  "testId: 'sql-history-detail-status'",
  "testId: 'sql-history-detail-target-engine'",
  "testId: 'sql-history-detail-sql-fingerprint'",
  "testId: 'sql-history-detail-audit-count'",
  'data-testid="sql-history-export"',
  'data-testid="sql-history-export-result"',
  "const SQL_EXECUTION_HISTORY_TYPE = 'QUERY_EXECUTION'",
  'historyType: SQL_EXECUTION_HISTORY_TYPE',
  "activeDetailTab.value = 'overview'",
  'name="parseEvidence"',
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
  "'historyType'"
]

const requiredSharedApiTokens = [
  'buildTenantQuerySuffix',
  'frontend-report-batch-parse-statistics',
  '/parse-statistics'
]

const requiredRouteTokens = [
  "sqlHistory: '/governance/history/sql-history'",
  "parseRecord: '/governance/history/parse-record'",
  'path: ROUTE_PATHS.sqlHistory',
  "name: 'SqlHistory'",
  'component: SqlHistoryView',
  "const SqlHistoryView = () => import('../views/sql-history/SqlHistoryView.vue')",
  'path: ROUTE_PATHS.parseRecord',
  "name: 'ParseRecord'",
  "historyWorkbenchTab: 'batchHistory'"
]

const forbiddenViewTokens = [
  "tenantId: 'tenant-a'",
  "sortBy: 'submittedAt'",
  "sortOrder: 'DESC'",
  '<el-button text size="small" class="help-dot" aria-label="issue scene help"',
  'aria-label="risk help"',
  'reportHistoryIdForTask',
  'history-parse-${',
  'name="issueSceneDetail"'
]

const forbiddenApiTokens = [
  "params.set('tenantId', tenantId)"
]

const missing = requiredTokens.filter(token => !source.includes(token))
const missingSqlHistory = requiredSqlHistoryTokens.filter(token => !sqlHistorySource.includes(token))
const missingApi = requiredApiTokens.filter(token => !queryHistoryPageApiSource.includes(token))
const missingSharedApi = requiredSharedApiTokens.filter(token => !apiSource.includes(token))
const routeSource = `${routePathsSource}\n${routerSource}`
const missingRoute = requiredRouteTokens.filter(token => !routeSource.includes(token))
const forbiddenView = forbiddenViewTokens.filter(token => parseRecordSource.includes(token))
const forbiddenApi = forbiddenApiTokens.filter(token => queryHistoryPageApiSource.includes(token))

if (missing.length > 0 || missingSqlHistory.length > 0 || missingApi.length > 0 || missingSharedApi.length > 0 || missingRoute.length > 0 || forbiddenView.length > 0 || forbiddenApi.length > 0) {
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
  for (const token of forbiddenView) {
    console.error(`- forbidden default filter token in view: ${token}`)
  }
  for (const token of forbiddenApi) {
    console.error(`- forbidden unconditional tenant query token in API: ${token}`)
  }
  process.exit(1)
}

console.log('History page contract check passed.')
