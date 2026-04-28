import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/optimization/AccelerationView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'data-testid="parse-workbench-page"',
  'data-testid="parse-workspace-entry"',
  'class="parse-workbench__grid"',
  'Explicit secondary entries',
  'Batch parse center',
  'Parse result center',
  'Acceleration and rewrite center',
  'single sql input',
  'combined conclusion',
  'Batch parsing dialog',
  'parse statistics center',
  'query history workbench',
  'data-testid="batch-import-page"',
  'data-testid="statistics-refresh"',
  'data-testid="parse-record-refresh"',
  'data-testid="parse-workbench-submit"',
  'data-testid="parse-workbench-structure-preview"',
  'data-testid="parse-workbench-refresh-status"',
  'data-testid="parse-workbench-status"',
  'data-testid="parse-workbench-overall-status"',
  'data-testid="parse-workbench-structure-card"',
  'data-testid="parse-workbench-query-intent"',
  'data-testid="parse-workbench-feature-summary"',
  'data-testid="parse-workbench-resource-estimate"',
  'data-testid="parse-workbench-risk-checklist"',
  'data-testid="parse-workbench-risk"',
  'data-testid="parse-workbench-access-card"',
  'data-testid="parse-workbench-history-entry"',
  'data-testid="parse-workbench-issue"',
  'Logical object hits',
  'Access parse card',
  'Structure parse card',
  'Query intent labels',
  'Feature dimensions',
  'Estimated resource cost',
  'Risk checklist',
  'SQL fingerprint'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Parse workbench contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Parse workbench contract check passed.')
