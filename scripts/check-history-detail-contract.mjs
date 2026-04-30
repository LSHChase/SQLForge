import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/parse-record/ParseRecordView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'query history detail',
  'SQL tri-state, parse signals, and related forensics',
  'data-testid="parse-record-history-parse-detail"',
  'data-testid="parse-record-history-original-sql"',
  'data-testid="parse-record-history-original-sql-text"',
  'data-testid="parse-record-history-parse-statistics"',
  'data-testid="parse-record-history-structure-card"',
  'data-testid="parse-record-history-access-card"',
  'data-testid="parse-record-history-issue"',
  'data-testid="parse-record-detail-query-history-count"',
  ':data-testid="`parse-record-${item.key}`"',
  ':data-testid="`parse-record-${group.key}`"',
  ':data-testid="`parse-record-history-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`"',
  "key: 'sqlText'",
  "key: 'sqlTemplateText'",
  "key: 'boundSqlText'",
  "key: 'commentContext'",
  "key: 'structureParseSummary'",
  "key: 'accessParseSummary'",
  "key: 'routeDecision'",
  "key: 'boundSqlFingerprint'",
  "key: 'bindingMode'",
  'Recommendation refs',
  'Benchmark refs',
  'Alert refs',
  'Audit refs'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('History detail contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('History detail contract check passed.')
