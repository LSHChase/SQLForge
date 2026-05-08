import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/parse-record/ParseRecordView.vue')
const helperPath = path.join(root, 'src/views/common/issueSceneHelp.mjs')
const source = [
  fs.readFileSync(viewPath, 'utf8'),
  fs.readFileSync(helperPath, 'utf8')
].join('\n')

const requiredTokens = [
  'parse history detail',
  'data-testid="parse-record-history-parse-detail"',
  'data-testid="parse-record-history-original-sql"',
  'data-testid="parse-record-history-original-sql-text"',
  'data-testid="parse-record-history-parse-statistics"',
  'data-testid="parse-record-history-structure-card"',
  'data-testid="parse-record-history-access-card"',
  'data-testid="parse-record-history-issue"',
  'data-testid="parse-record-report-sql-history-link"',
  'data-testid="parse-record-report-sql-history-detail-unavailable"',
  'data-testid="parse-record-report-detail-tabs"',
  'data-testid="parse-record-report-statistics-tabs"',
  'data-testid="parse-record-report-issue-scene-detail"',
  "activeReportBatchDetailTab.value = 'statistics'",
  "activeReportBatchStatisticsTab.value = 'issueScene'",
  ':data-testid="`parse-record-${item.key}`"',
  ':data-testid="`parse-record-${group.key}`"',
  ':data-testid="`parse-record-history-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`"',
  "key: 'sqlText'",
  "key: 'sqlTemplateText'",
  "key: 'boundSqlText'",
  "key: 'commentContext'",
  "key: 'structureParseSummary'",
  "key: 'accessParseSummary'",
  "key: 'routeDecision'",
  "key: 'boundSqlFingerprint'",
  "key: 'bindingMode'",
  'reportHistoryIdForItem',
  'reportHistoryIsPersisted',
  'reportItemLocalDetail',
  'buildReportItemFallbackDetail',
  'getSqlParseHistoryDetail',
  'exportSqlParseHistory',
  'issueSceneHelpText',
  'issueSceneCodesForItem',
  'issue-scene-help',
  'riskDisplayText',
  'REPEATED_TABLE_SCAN_RISK',
  'SQL_TOO_LONG',
  'REPORT_SQL_MERGE_CANDIDATE',
  'mergeCandidateReportCount',
  'mergeCandidateReason',
  '风险含义：',
  '原因：',
  '建议：',
  'aria-label="issue scene help"',
  'Recommendation refs',
  'Benchmark refs',
  'Alert refs',
  'Audit refs'
]

const missing = requiredTokens.filter(token => !source.includes(token))
const forbiddenTokens = [
  '<el-button text size="small" class="help-dot" aria-label="issue scene help"',
  'aria-label="risk help"',
  'reportHistoryIdForTask',
  'history-parse-${',
  'name="issueSceneDetail"',
  'query history detail',
  'getGovernanceQueryHistoryDetail',
  'exportGovernanceQueryHistory'
]
const forbidden = forbiddenTokens.filter(token => source.includes(token))

if (missing.length > 0 || forbidden.length > 0) {
  console.error('History detail contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of forbidden) {
    console.error(`- forbidden token: ${token}`)
  }
  process.exit(1)
}

console.log('History detail contract check passed.')
