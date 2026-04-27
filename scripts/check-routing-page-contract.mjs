import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/routing-governance/RoutingGovernanceView.vue',
    tokens: [
      'data-testid="routing-page"',
      'data-testid="routing-current-policy"',
      'routing-route-decision',
      'data-testid="routing-comment-protocol"',
      'routing execution evidence',
      'engineHint',
      'effectiveRouteOrder',
      'readonlyBoundary',
      'routeDecision',
      'recommendationRefs',
      'Open parse-record page',
      'View current policy source',
      'Create rule',
      'Edit rule'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: [
      'getHetuRouteCalibration',
      '/api/query-execution/internal/hetu/route-calibration'
    ]
  },
  {
    path: 'src/config/routePaths.mjs',
    tokens: ['routingGovernance', '/governance/routing']
  },
  {
    path: 'src/router/index.js',
    tokens: ['RoutingGovernanceView', "name: 'RoutingGovernance'"]
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

console.log('routing page contract ok')
