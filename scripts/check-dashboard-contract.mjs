import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/dashboard/DashboardView.vue',
    tokens: [
      'data-testid="dashboard-page"',
      'data-testid="dashboard-kpi-card"',
      'data-testid="dashboard-issue-distribution"',
      'data-testid="dashboard-access-distribution"',
      'data-testid="dashboard-todo-item"',
      'data-testid="dashboard-activity-item"',
      'parse-statistics',
      'query-history',
      'dispatch events',
      'message stats'
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
      'getDispatchEvents'
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
