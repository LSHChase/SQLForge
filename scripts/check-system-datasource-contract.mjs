import { readFileSync } from 'node:fs'

const targets = [
  {
    path: 'src/views/system/SystemView.vue',
    tokens: [
      'data-testid="system-management-page"',
      'data-testid="system-datasource-card"',
      'data-testid="system-datasource-detail"',
      'data-testid="system-datasource-test"',
      'data-testid="system-datasource-test-result"',
      'data-testid="system-hetu-jdbc-create"',
      'data-testid="system-datasource-engine-filter"',
      'data-testid="system-datasource-mode-filter"',
      'data-testid="system-datasource-engine-type"',
      'data-testid="system-report-interface-card"',
      'engineType',
      'connectionMode',
      'jdbcDriverClassName',
      'username',
      'credentialMask',
      'credentialSecret',
      'realJdbcProbe',
      'elapsedMs',
      'lastFailureReason',
      'resolverStatus'
    ]
  },
  {
    path: 'src/services/runtimeGateApi.js',
    tokens: [
      'getGovernanceDatasources',
      'getGovernanceDatasourceDetail',
      'testGovernanceDatasourceConnection',
      'createGovernanceDatasource',
      'updateGovernanceDatasource',
      'getGovernanceReportInterfaces'
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

console.log('system datasource contract ok')
