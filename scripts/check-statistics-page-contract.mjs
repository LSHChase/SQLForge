import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const source = [
  'src/views/parse-statistics/ParseStatisticsCenterView.vue',
  'src/locales/zh-CN.js',
  'src/locales/en-US.js'
]
  .map(relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8'))
  .join('\n')

const requiredTokens = [
  'data-testid="statistics-page"',
  'sql parse statistics',
  'data-testid="statistics-refresh"',
  'data-testid="statistics-priority-matrix"',
  'data-testid="statistics-issue-scene"',
  'data-testid="statistics-important-urgent"',
  'data-testid="statistics-endpoint-error"',
  'data-testid="statistics-detail-dialog"',
  'data-testid="statistics-detail-tabs"',
  'data-testid="statistics-detail-relation"',
  'data-testid="statistics-detail-raw-json"',
  'normalizeDisplayList',
  'displayList(row.issueScenes)',
  'Promise.allSettled',
  'statisticErrors',
  '关联 SQL/报表',
  '原始 JSON',
  'Parse overview',
  'Issue distribution',
  'Priority matrix',
  'Important or urgent list',
  'By report',
  'By SQL',
  'Severity view',
  'Priority view',
  'Logical object view',
  'Parse status samples'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Statistics page contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Statistics page contract check passed.')
