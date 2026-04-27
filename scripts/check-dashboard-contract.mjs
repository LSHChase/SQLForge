import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/dashboard/DashboardView.vue',
    tokens: [
      'data-testid="dashboard-page"',
      'data-testid="dashboard-kpi-card"',
      'data-testid="dashboard-workbench-entry"',
      'data-testid="dashboard-health"',
      'data-testid="dashboard-health-card"',
      'data-testid="dashboard-issue-distribution"',
      'data-testid="dashboard-activity-stream"',
      'data-testid="dashboard-next-steps"',
      'data-testid="dashboard-next-step-item"',
      'data-testid="dashboard-activity-item"',
      "key: 'total-sql'",
      "key: 'issue-sql'",
      "key: 'important-urgent'",
      "key: 'governance-backlog'",
      "key: 'requires-dispatch'",
      "key: 'dispatch-failures'",
      "key: 'high-benefit'",
      "key: 'recent-audit'",
      "key: 'route-engine-sample'",
      'path: ROUTE_PATHS.sqlQuery',
      'path: ROUTE_PATHS.acceleration',
      'path: ROUTE_PATHS.parseRecord',
      'path: ROUTE_PATHS.benchmark',
      'path: ROUTE_PATHS.system',
      'query-history',
      'dispatch events',
      'message stats',
      'recommendations'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: [
      'getParseStatisticsOverview',
      'getParseStatisticsByIssueScene',
      'getParseStatisticsImportantUrgent',
      'getGovernanceMessageStats',
      'getGovernanceQueryHistoryPage',
      'getDispatchEvents',
      'getRecommendations'
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
}

console.log('dashboard contract ok')
