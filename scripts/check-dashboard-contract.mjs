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
      "key: 'success-rate'",
      "key: 'failure-rate'",
      "key: 'cache-hit-rate'",
      "key: 'rewrite-hit-rate'",
      "key: 'acceleration-hit-rate'",
      "key: 'issue-sql'",
      "key: 'important-urgent'",
      "key: 'governance-backlog'",
      "key: 'deep-recommendations'",
      "key: 'coordination-mode'",
      "key: 'open-alert-sample'",
      "key: 'access-channel-sample'",
      "key: 'route-engine-sample'",
      'path: ROUTE_PATHS.sqlQuery',
      'path: ROUTE_PATHS.acceleration',
      'path: ROUTE_PATHS.sqlHistory',
      'path: ROUTE_PATHS.benchmark',
      'path: ROUTE_PATHS.system',
      'query-history',
      'dispatch events',
      'message stats',
      'recommendations',
      'frontend-dashboard-dispatch-contract'
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
