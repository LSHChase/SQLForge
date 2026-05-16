import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/recommendation-center/RecommendationCenterView.vue',
    tokens: [
      'data-testid="recommendation-page"',
      'data-testid="recommendation-filter"',
      'data-testid="recommendation-detail"',
      'data-testid="recommendation-focus-summary"',
      'data-testid="recommendation-dispatch-contract"',
      'data-testid="recommendation-dispatch-event"',
      'data-testid="recommendation-trace-refs"',
      'data-testid="recommendation-review-guard"',
      'data-testid="recommendation-sql-diff"',
      'data-testid="recommendation-sql-compare"',
      'data-testid="recommendation-text-diff"',
      'data-testid="recommendation-ast-summary-diff"',
      'data-testid="recommendation-rule-diff"',
      'data-testid="recommendation-rule-chain"',
      'data-testid="recommendation-rule-chain-summary"',
      'data-testid="recommendation-preconditions"',
      'data-testid="recommendation-preconditions-summary"',
      'data-testid="recommendation-semantic-risks"',
      'data-testid="recommendation-semantic-risks-summary"',
      'data-testid="recommendation-unapplied-rules"',
      'data-testid="recommendation-unapplied-rules-summary"',
      'data-testid="recommendation-rewrite-lifecycle"',
      'data-testid="recommendation-rewrite-lifecycle-state"',
      'data-testid="recommendation-rewrite-record-select"',
      'data-testid="recommendation-rewrite-lifecycle-status"',
      'data-testid="recommendation-rewrite-lifecycle-actions"',
      'data-testid="recommendation-rewrite-review-note"',
      'data-testid="recommendation-rewrite-action-reason"',
      'data-testid="recommendation-rewrite-approve"',
      'data-testid="recommendation-rewrite-reject"',
      'data-testid="recommendation-rewrite-publish"',
      'data-testid="recommendation-rewrite-pause"',
      'data-testid="recommendation-rewrite-unpublish"',
      'data-testid="recommendation-rewrite-publish-eligibility"',
      'data-testid="recommendation-rewrite-validation-run-table"',
      'data-testid="recommendation-rewrite-refusal-reasons"',
      'data-testid="recommendation-alert-linkage"',
      'data-testid="recommendation-open-alert-center"',
      'coordinationMode',
      'PULL_ONLY',
      'dispatchEvents',
      'benefitLevel',
      'riskLevel',
      'recommendedSqlText',
      'logicalObjectKey',
      'getRecommendationDiff',
      'textDiff',
      'compareRows',
      'comparePaneClass',
      'hunkTagType',
      'astSummaryDiff',
      'focusSummaryCards',
      'ruleChain',
      'preconditions',
      'semanticRisks',
      'unappliedRules',
      'summarizeEvidenceItem',
      "activeDetailTab.value = recommendationDiff.value ? 'sqlDiff' : 'summary'",
      'normalizeDetailTab(route.query.tab || route.query.detailTab)',
      'manualReviewRequired',
      'reviewSqlRewriteRecord',
      'getSqlRewriteRecords',
      'getSqlRewriteRecord',
      'getRewriteValidationRuns',
      'getRewritePublishEligibility',
      'publishSqlRewriteRecord',
      'pauseSqlRewriteRecord',
      'unpublishSqlRewriteRecord',
      'reviewStatus',
      'publishStatus',
      'publishEligibilityCards',
      'rewriteValidationRuns',
      'refusalReasons',
      'runtimeBindingId',
      'runtimeRuleVersion',
      'runtimeBindingScope',
      'approvedNotPublished',
      'runtimeActive',
      'APPROVED',
      'REJECTED',
      'PUBLISHED',
      'PAUSED',
      'UNPUBLISHED',
      'alertLinkageCards',
      'openAlertCenter',
      'SQL_REWRITE_RESULT_DIVERGENCE',
      '<el-table',
      '<el-pagination',
      '<el-drawer',
      'pagedRecommendations',
      'openEvidenceDrawer'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: [
      'getRecommendations',
      'getRecommendationDiff',
      'getRecommendationTrace',
      'getDispatchContract',
      'getDispatchEvents',
      'reviewSqlRewriteRecord',
      'getRewritePublishEligibility',
      'getRewriteValidationRuns',
      'publishSqlRewriteRecord',
      'pauseSqlRewriteRecord',
      'unpublishSqlRewriteRecord',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/review',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/publish-eligibility',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/validation-runs',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/publish',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/pause',
      '/api/sql-optimization/rewrite-records/${encodeURIComponent(rewriteRecordId)}/unpublish'
    ]
  },
  {
    path: 'src/config/routePaths.mjs',
    tokens: ['recommendationCenter', '/governance/recommendations']
  },
  {
    path: 'src/router/index.js',
    tokens: ['RecommendationCenterView', "name: 'RecommendationCenter'"]
  }
]

for (const target of targets) {
  const content = readFileSync(new URL(`../${target.path}`, import.meta.url), 'utf8')
  for (const token of target.tokens) {
    if (!content.includes(token)) {
      throw new Error(`Missing token ${JSON.stringify(token)} in ${target.path}`)
    }
  }
  if (target.path === 'src/views/recommendation-center/RecommendationCenterView.vue') {
    const forbiddenTokens = ['class="recommendation-row"', 'class="dispatch-event-row"', 'class="evidence-group"', '<el-card']
    for (const token of forbiddenTokens) {
      if (content.includes(token)) {
        throw new Error(`Recommendation page must not reintroduce stacked card/list token ${JSON.stringify(token)}`)
      }
    }
    assertOrderedTokens(target.path, content, [
      'name="summary"',
      'name="sqlDiff"',
      'name="rulesRisk"',
      'name="sqlEvidence"',
      'name="rewriteLifecycle"',
      'name="dispatchContract"',
      'name="traceability"',
      'name="dispatchEvents"'
    ])
    assertOrderedTokens(target.path, content, [
      'data-testid="recommendation-focus-summary"',
      'data-testid="recommendation-sql-diff"',
      'data-testid="recommendation-rule-chain-summary"',
      'data-testid="recommendation-rewrite-lifecycle"',
      'data-testid="recommendation-rewrite-validation-runs"',
      'data-testid="recommendation-dispatch-contract"'
    ])
  }
}

function assertOrderedTokens(path, content, orderedTokens) {
  let previousIndex = -1
  for (const token of orderedTokens) {
    const index = content.indexOf(token)
    if (index === -1) {
      throw new Error(`Missing ordered token ${JSON.stringify(token)} in ${path}`)
    }
    if (index <= previousIndex) {
      throw new Error(`推荐结果页 token 顺序错误：${path} 中 ${JSON.stringify(token)} 未位于前一个 token 之后`)
    }
    previousIndex = index
  }
}

console.log('recommendation page contract ok')
