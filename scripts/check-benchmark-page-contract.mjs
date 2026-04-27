import { readFileSync } from 'node:fs'

const content = readFileSync(new URL('../src/views/benchmark/BenchmarkView.vue', import.meta.url), 'utf8')

const requiredTokens = [
  'data-testid="benchmark-page"',
  'data-testid="benchmark-template-card"',
  'data-testid="benchmark-test-set-card"',
  'data-testid="benchmark-session-task"',
  'data-testid="benchmark-report-compare"',
  'data-testid="benchmark-regression-results"',
  'template catalog',
  'test-set catalog',
  'SESSION_CATALOG_ONLY',
  'Comparison metrics',
  'regression results',
  'benchmark-flow-submit',
  'benchmark-flow-submit-failure'
]

for (const token of requiredTokens) {
  if (!content.includes(token)) {
    throw new Error(`Missing token ${JSON.stringify(token)} in BenchmarkView.vue`)
  }
}

console.log('benchmark page contract ok')
