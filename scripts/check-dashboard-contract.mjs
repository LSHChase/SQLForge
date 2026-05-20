import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/dashboard/DashboardView.vue',
    tokens: [
      'data-testid="dashboard-page"',
      'test-id="dashboard-kpi-card"',
      'data-testid="dashboard-workbench-entry"',
      'test-id="dashboard-health"',
      'data-testid="dashboard-health-card"',
      'test-id="dashboard-issue-distribution"',
      'data-testid="dashboard-issue-distribution-empty"',
      'test-id="dashboard-activity-stream"',
      'data-testid="dashboard-activity-empty"',
      'test-id="dashboard-next-steps"',
      'data-testid="dashboard-next-step-item"',
      'data-testid="dashboard-activity-item"',
      'coreAttentionCount',
      'auxiliarySignals',
      "key: 'query-window'",
      "key: 'parse-overview'",
      "key: 'parse-history-window'",
      "key: 'important-urgent'",
      "key: 'recommendation-results'",
      "key: 'rewrite-records'",
      "key: 'query'",
      "key: 'history'",
      "key: 'parse'",
      "key: 'parse-history'",
      "key: 'recommendations'",
      "key: 'rewrite'",
      'path: ROUTE_PATHS.sqlQuery',
      'path: ROUTE_PATHS.acceleration',
      'path: ROUTE_PATHS.sqlHistory',
      'path: ROUTE_PATHS.parseRecord',
      'path: ROUTE_PATHS.recommendationCenter',
      'query-history',
      'parse-history',
      'rewrite-records',
      'recommendations',
      'frontend-dashboard-dispatch-contract',
      'frontend-dashboard-parse-history',
      'frontend-dashboard-rewrite-records'
    ],
    forbiddenTokens: [
      "key: 'benchmark'",
      "key: 'system'",
      'path: ROUTE_PATHS.benchmark',
      ['path: ROUTE_PATHS.', 'alert', 'Center'].join(''),
      'frontend-dashboard-dispatch-events'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: [
      'getParseStatisticsOverview',
      'getParseStatisticsByIssueScene',
      'getParseStatisticsImportantUrgent',
      'getGovernanceMessageStats',
      'getDispatchContract',
      'getGovernanceQueryHistoryPage',
      'getRecommendations',
      'getSqlParseHistoryPage',
      'getSqlRewriteRecords'
    ]
  }
]

for (const target of targets) {
  const content = readFileSync(new URL(`../${target.path}`, import.meta.url), 'utf8')
  for (const token of target.tokens) {
    if (!content.includes(token)) {
      throw new Error(`Missing token ${JSON.stringify(token)} in ${target.path}`)
    }
  }
  for (const token of target.forbiddenTokens || []) {
    if (content.includes(token)) {
      throw new Error(`Forbidden token ${JSON.stringify(token)} in ${target.path}`)
    }
  }
}

console.log('dashboard contract ok')
