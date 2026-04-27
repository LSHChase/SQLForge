import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/optimization/AccelerationView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'data-testid="parse-workbench-page"',
  'class="parse-workbench__grid"',
  'single sql input',
  'combined conclusion',
  'data-testid="parse-workbench-submit"',
  'data-testid="parse-workbench-structure-preview"',
  'data-testid="parse-workbench-refresh-status"',
  'data-testid="parse-workbench-status"',
  'data-testid="parse-workbench-overall-status"',
  'data-testid="parse-workbench-structure-card"',
  'data-testid="parse-workbench-access-card"',
  'data-testid="parse-workbench-history-entry"',
  'data-testid="parse-workbench-issue"',
  'Logical object hits',
  'Access parse card',
  'Structure parse card'
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
