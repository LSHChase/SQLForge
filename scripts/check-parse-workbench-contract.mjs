import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/optimization/AccelerationView.vue')
const helperPath = path.join(root, 'src/views/common/issueSceneHelp.mjs')
const source = [
  fs.readFileSync(viewPath, 'utf8'),
  fs.readFileSync(helperPath, 'utf8')
].join('\n')

const requiredTokens = [
  'data-testid="parse-workbench-page"',
  'class="parse-workbench__grid"',
  'SQL解析',
  'SQL Parse',
  'single sql input',
  'parse result',
  'Parse result note',
  'History ID',
  'fieldHelpDialogVisible',
  'fieldHelpDialogTitle',
  'fieldHelpDialogMessage',
  'help-dot',
  'data-testid="statistics-refresh"',
  'data-testid="parse-workbench-submit"',
  'data-testid="parse-workbench-parser-mode"',
  'data-testid="parse-workbench-parse-batch-parser-mode"',
  'data-testid="parse-workbench-report-batch-parser-mode"',
  'data-testid="parse-workbench-structure-preview"',
  'data-testid="parse-workbench-refresh-status"',
  'data-testid="parse-workbench-open-history"',
  'data-testid="parse-workbench-status"',
  'data-testid="parse-workbench-overall-status"',
  'data-testid="parse-workbench-structure-card"',
  'data-testid="parse-workbench-query-intent"',
  'data-testid="parse-workbench-feature-summary"',
  'data-testid="parse-workbench-resource-estimate"',
  'data-testid="parse-workbench-risk-checklist"',
  'data-testid="parse-workbench-risk"',
  'data-testid="parse-workbench-plan-card"',
  'data-testid="parse-workbench-plan-analysis"',
  'data-testid="parse-workbench-plan-text"',
  'data-testid="parse-workbench-access-card"',
  'data-testid="parse-workbench-history-entry"',
  'data-testid="parse-workbench-issue"',
  'riskDisplayText(',
  'sharedRiskDisplayText',
  'REPEATED_TABLE_SCAN_RISK',
  'SQL_TOO_LONG',
  '风险含义：',
  '原因：',
  '建议：',
  'Logical object hits',
  'Access parse card',
  'Structure parse card',
  'Query intent labels',
  'Feature dimensions',
  'Order keys',
  'Duplicate order keys',
  'Duplicate group keys',
  'String projections',
  'String aggregates',
  'Repeated subqueries',
  'Estimated resource cost',
  'Risk checklist',
  'SQL fingerprint',
  'ORDER_BY_COMPLEXITY_RISK',
  'JOIN_LATENCY_RISK',
  'AGGREGATION_COMPLEXITY_RISK',
  'GROUP_BY_WITHOUT_AGGREGATE_RISK',
  'DUPLICATE_GROUP_OR_ORDER_KEY_RISK',
  'REPEATED_SUBQUERY_RISK',
  'LARGE_STRING_RESULT_RISK',
  'parserModeOptions',
  'JSQLParser',
  'Apache Calcite',
  'APACHE_CALCITE',
  'JSQLPARSER_WITH_PLAN',
  'APACHE_CALCITE_WITH_PLAN',
  'planAnalysis',
  'analysisStatus',
  'parserMode: String(form.parserMode',
  'parserMode: parseBatchForm.parserMode',
  'parserMode: reportBatchForm.parserMode',
  '解析工具 / Parser tool'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Parse workbench contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Parse workbench contract check passed.')
