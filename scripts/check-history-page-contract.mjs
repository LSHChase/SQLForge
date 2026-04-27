import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/parse-record/ParseRecordView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'data-testid="parse-record-page"',
  'data-testid="parse-record-refresh"',
  'data-testid="parse-record-run-lookup"',
  'data-testid="parse-record-load-more"',
  'data-testid="parse-record-filter-select"',
  'data-testid="parse-record-sort-select"',
  'data-testid="parse-record-status-filter"',
  'data-testid="parse-record-sort-mode"',
  'data-testid="parse-record-page-mode"',
  'data-testid="parse-record-trace-item"',
  'data-testid="parse-record-detail-trace-id"',
  'History classification',
  'Sort mode'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('History page contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('History page contract check passed.')
