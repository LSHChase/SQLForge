import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/system/SystemView.vue',
    tokens: [
      'data-testid="system-redis-rule-sources"',
      'data-testid="system-redis-rule-source-card"',
      'data-testid="system-dispatch-policies"',
      'data-testid="system-dispatch-policy-card"',
      'data-testid="system-tenant-params"',
      'data-testid="system-permission-audit"',
      'CONFIG_ONLY',
      'EXTERNAL_MODULE_REQUIRED'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: ['getGovernanceRedisRuleSources', 'getGovernanceDispatchPolicies', 'getGovernanceTenantConfig']
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

console.log('system config contract ok')
