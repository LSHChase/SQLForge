import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/parse-record/ParseRecordView.vue')
const apiPath = path.join(root, 'src/services/runtimeGateApi.js')
const source = fs.readFileSync(viewPath, 'utf8')
const apiSource = fs.readFileSync(apiPath, 'utf8')
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
  'data-testid="parse-record-report-batch-detail"',
  'data-testid="parse-record-report-group"',
  'data-testid="parse-record-report-sql-detail"',
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
  'getReportBatchParseStatistics',
  'frontend-parse-record-report-sql-history-detail',
  'frontend-parse-record-report-batch-statistics',
  'History classification',
  'Sort mode',
  'query history table',
  'batch history',
  'query history workbench',
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

const requiredApiTokens = [
  'const filterTenantId = normalizeTenantId(filters.tenantId)',
  'const requestTenantId = normalizeTenantId(filters.requestTenantId) || filterTenantId',
  'if (filterTenantId) {',
  "params.set('tenantId', filterTenantId)",
  'tenantId: requestTenantId'
]

const requiredSharedApiTokens = [
  'buildTenantQuerySuffix',
  'frontend-report-batch-parse-statistics',
  '/parse-statistics'
]

const forbiddenViewTokens = [
  "tenantId: 'tenant-a'",
  "sortBy: 'submittedAt'",
  "sortOrder: 'DESC'"
]

const forbiddenApiTokens = [
  "params.set('tenantId', tenantId)"
]

const missing = requiredTokens.filter(token => !source.includes(token))
const missingApi = requiredApiTokens.filter(token => !queryHistoryPageApiSource.includes(token))
const missingSharedApi = requiredSharedApiTokens.filter(token => !apiSource.includes(token))
const forbiddenView = forbiddenViewTokens.filter(token => source.includes(token))
const forbiddenApi = forbiddenApiTokens.filter(token => queryHistoryPageApiSource.includes(token))

if (missing.length > 0 || missingApi.length > 0 || missingSharedApi.length > 0 || forbiddenView.length > 0 || forbiddenApi.length > 0) {
  console.error('History page contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of missingApi) {
    console.error(`- missing API token: ${token}`)
  }
  for (const token of missingSharedApi) {
    console.error(`- missing shared API token: ${token}`)
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
