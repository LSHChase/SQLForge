import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/parse-batch/ParseBatchCenterView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'data-testid="batch-import-page"',
  'template download + upload',
  'report catalog import',
  'data-testid="batch-import-create"',
  'data-testid="batch-import-download-template"',
  'data-testid="batch-import-ingest"',
  'data-testid="batch-import-retry-access"',
  'data-testid="batch-import-parse-batch-item"',
  'data-testid="batch-import-parse-detail"',
  'data-testid="batch-import-failure-record"',
  'data-testid="batch-import-report-import"',
  'data-testid="batch-import-report-resolve"',
  'data-testid="batch-import-report-item"',
  'Template-column contract',
  'Failure records',
  'Report items'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Batch import contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Batch import contract check passed.')
