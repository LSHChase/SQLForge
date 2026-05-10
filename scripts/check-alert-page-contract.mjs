import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/alert-center/AlertCenterView.vue',
    tokens: [
      'data-testid="alert-page"',
      'data-testid="alert-item"',
      'data-testid="alert-detail"',
      'data-testid="alert-ack"',
      'data-testid="alert-ack-status"',
      'data-testid="alert-notify-status"',
      'SIMULATED_PENDING_NOTIFY',
      'SIMULATED_NOTIFIED',
      'ACK_SIMULATED',
      'FRONTEND_SIMULATED',
      'GET /api/governance/alerts'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: ['getGovernanceMessageStats', 'getDispatchEvents', 'getParseStatisticsImportantUrgent']
  },
  {
    path: 'src/config/routePaths.mjs',
    tokens: ['alertCenter', '/governance/alerts']
  },
  {
    path: 'src/router/index.js',
    tokens: ['AlertCenterView']
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

console.log('alert page contract ok')
