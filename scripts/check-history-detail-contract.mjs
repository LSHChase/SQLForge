import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const parseRecordDir = path.join(root, 'src/views/parse-record')
const helperPath = path.join(root, 'src/views/common/issueSceneHelp.mjs')
const parseRecordSource = fs
  .readdirSync(parseRecordDir)
  .filter(file => /\.(?:vue|js|css)$/.test(file))
  .sort()
  .map(file => fs.readFileSync(path.join(parseRecordDir, file), 'utf8'))
  .join('\n')
const source = [
  parseRecordSource,
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
  ':data="reportBatchIssueStatisticsPage"',
  'data-testid="parse-record-report-statistics-issue-scene"',
  'reportBatchIssueStatisticsPagination',
  'REPORT_STATISTIC_PAGE_SIZE_OPTIONS',
  '@current-change="handleReportBatchIssueStatisticsPageChange"',
  '@size-change="handleReportBatchIssueStatisticsPageSizeChange"',
  'data-testid="parse-record-report-issue-scene-detail"',
  'data-testid="parse-record-report-issue-scene-detail-dialog"',
  'data-testid="parse-record-report-issue-scene-linked-filters"',
  'data-testid="parse-record-report-issue-scene-report"',
  'data-testid="parse-record-report-issue-scene-object"',
  'data-testid="parse-record-report-issue-scene-sql"',
  ':data="normalizeArray(selectedReportIssueSceneDetail.reportDetails)"',
  ':data="normalizeArray(selectedReportIssueSceneDetail.logicalObjectDetails)"',
  ':data="normalizeArray(selectedReportIssueSceneDetail.sqlStatistics)"',
  'reportBatchIssueSceneDetailDialogVisible',
  'reportBatchIssueSceneDetailDialogTitle',
  'sqlStatisticTotalCount',
  'layout="total, sizes, prev, pager, next"',
  '@current-change="handleReportBatchIssueScenePageChange"',
  '@size-change="handleReportBatchIssueScenePageSizeChange"',
  '@row-click="selectReportBatchIssueSceneReport"',
  '@row-click="selectReportBatchIssueSceneLogicalObject"',
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
const forbiddenPatterns = [
  {
    label: 'selectedReportIssueSceneDetail.reportDetails fixed slice(0, 8)',
    pattern: /selectedReportIssueSceneDetail\.reportDetails[\s\S]{0,120}\.slice\(\s*0\s*,\s*8\s*\)/
  }
]
const forbiddenPatternHits = forbiddenPatterns.filter(item => item.pattern.test(source))

if (missing.length > 0 || forbidden.length > 0 || forbiddenPatternHits.length > 0) {
  console.error('History detail contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of forbidden) {
    console.error(`- forbidden token: ${token}`)
  }
  for (const item of forbiddenPatternHits) {
    console.error(`- forbidden pattern: ${item.label}`)
  }
  process.exit(1)
}

console.log('History detail contract check passed.')
