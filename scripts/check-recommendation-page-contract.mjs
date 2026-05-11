import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/recommendation-center/RecommendationCenterView.vue',
    tokens: [
      'data-testid="recommendation-page"',
      'data-testid="recommendation-filter"',
      'data-testid="recommendation-detail"',
      'data-testid="recommendation-dispatch-contract"',
      'data-testid="recommendation-dispatch-event"',
      'data-testid="recommendation-trace-refs"',
      'data-testid="recommendation-review-guard"',
      'data-testid="recommendation-sql-diff"',
      'data-testid="recommendation-text-diff"',
      'data-testid="recommendation-ast-summary-diff"',
      'data-testid="recommendation-rule-diff"',
      'data-testid="recommendation-rule-chain"',
      'data-testid="recommendation-preconditions"',
      'data-testid="recommendation-semantic-risks"',
      'data-testid="recommendation-unapplied-rules"',
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
      'astSummaryDiff',
      'ruleChain',
      'preconditions',
      'semanticRisks',
      'unappliedRules',
      'manualReviewRequired',
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
      'getDispatchEvents'
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
  }
}

console.log('recommendation page contract ok')
