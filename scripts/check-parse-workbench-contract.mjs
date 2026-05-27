import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/optimization/AccelerationView.vue')
const rewriteValidationViewPath = path.join(root, 'src/views/rewrite-validation/RewriteValidationView.vue')
const helperPath = path.join(root, 'src/views/common/issueSceneHelp.mjs')
const zhLocalePath = path.join(root, 'src/locales/zh-CN.js')
const enLocalePath = path.join(root, 'src/locales/en-US.js')
const source = [
  fs.readFileSync(viewPath, 'utf8'),
  fs.readFileSync(rewriteValidationViewPath, 'utf8'),
  fs.readFileSync(helperPath, 'utf8'),
  fs.readFileSync(zhLocalePath, 'utf8'),
  fs.readFileSync(enLocalePath, 'utf8')
].join('\n')

const requiredTokens = [
  'data-testid="parse-workbench-page"',
  'class="parse-workbench__grid"',
  'SQL解析',
  'SQL Parse',
  "route.query.mode === 'rewriteValidation'",
  'pageEyebrow',
  'pageTitle',
  'pageSummary',
  'data-testid="parse-workbench-title"',
  'data-testid="parse-workbench-summary"',
  'data-testid="parse-workbench-rewrite-validation-boundary"',
  'rewriteValidationTitle',
  'rewriteValidationSummary',
  'rewriteValidationBoundary',
  'single sql input',
  'parse result',
  'Parse result note',
  'History ID',
  'fieldHelpDialogVisible',
  'fieldHelpDialogTitle',
  'fieldHelpDialogMessage',
  'help-dot',
  'data-testid="parse-workbench-submit"',
  'data-testid="parse-workbench-datasource-code"',
  'data-testid="parse-workbench-parser-mode"',
  'data-testid="parse-workbench-parse-batch-parser-mode"',
  'data-testid="parse-workbench-report-batch-parser-mode"',
  'data-testid="parse-workbench-structure-preview"',
  'data-testid="parse-workbench-refresh-status"',
  'data-testid="parse-workbench-open-history"',
  'data-testid="parse-workbench-open-recommendations"',
  'openRecommendationResultsForParseResult',
  'data-testid="rewrite-validation-page"',
  'data-testid="rewrite-validation-submit"',
  'data-testid="rewrite-validation-status"',
  'data-testid="rewrite-validation-sql-compare"',
  'data-testid="rewrite-validation-create-record"',
  'data-testid="rewrite-validation-create-run"',
  'data-testid="rewrite-validation-run-table"',
  'submitOptimizationTask',
  'waitForOptimizationTask',
  'createSqlRewriteRecord',
  'createRewriteValidationRun',
  'taskType: \'REWRITE\'',
  'comparisonStatus: \'NOT_COMPARED\'',
  'sourceKind: form.sourceKind',
  'sourceCategory: \'SQL_PARSE\'',
  'sourceType: \'PARSE\'',
  'historyId',
  'parseTaskId',
  'structureOnlyResult.historyId',
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
  'Apache Calcite',
  'APACHE_CALCITE',
  'APACHE_CALCITE_WITH_PLAN',
  'planAnalysis',
  'planStatus',
  'datasourceCode',
  'failureReason',
  'planText',
  'analysisStatus',
  'ROUTE_PATHS.parseStatisticsCenter',
  'parserMode: String(form.parserMode',
  'parserMode: parseBatchForm.parserMode',
  'parserMode: reportBatchForm.parserMode',
  '解析工具 / Parser tool'
]

const missing = requiredTokens.filter(token => !source.includes(token))
const forbiddenTokens = [
  'data-testid="parse-workbench-open-statistics"',
  'data-testid="parse-workbench-statistics-entry"',
  'data-testid="parse-workbench-open-statistics-secondary"',
  'function openStatisticsCenter()'
]
const presentForbidden = forbiddenTokens.filter(token => source.includes(token))

if (missing.length > 0 || presentForbidden.length > 0) {
  console.error('Parse workbench contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  for (const token of presentForbidden) {
    console.error(`- forbidden token: ${token}`)
  }
  process.exit(1)
}

console.log('Parse workbench contract check passed.')
