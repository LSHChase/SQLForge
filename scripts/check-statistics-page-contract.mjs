import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/optimization/AccelerationView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'data-testid="statistics-page"',
  'parse statistics center',
  'data-testid="statistics-refresh"',
  'data-testid="statistics-priority-matrix"',
  'data-testid="statistics-issue-scene"',
  'data-testid="statistics-important-urgent"',
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
