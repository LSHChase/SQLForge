import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/access-center/AccessCenterView.vue',
    tokens: [
      'data-testid="access-page"',
      'data-testid="access-channel-card"',
      'data-testid="access-jdbc-agent-mode"',
      'data-testid="access-sdk-card"',
      'data-testid="access-audit-item"',
      'data-testid="access-audit-detail"',
      'JDBC_AGENT',
      'SDK_QUERY_EXECUTE',
      'OBSERVE',
      'GOVERNED_EXECUTE',
      'LOCAL_REWRITE_DIRECT_JDBC',
      'GET /api/governance/access-audit',
      'query-history'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: ['getGovernanceQueryHistoryPage']
  },
  {
    path: 'src/config/routePaths.mjs',
    tokens: ['accessCenter', '/governance/access']
  },
  {
    path: 'src/router/index.js',
    tokens: ['AccessCenterView', "name: 'AccessCenter'"]
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

console.log('access page contract ok')
