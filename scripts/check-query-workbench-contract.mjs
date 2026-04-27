import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/query/SqlQueryView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'class="query-workbench__grid"',
  'class="query-rail surface-card"',
  'class="editor-rail surface-card"',
  'class="result-rail surface-card"',
  'data-testid="query-flow-page"',
  'data-testid="query-flow-submit"',
  'data-testid="query-flow-submit-recovery"',
  'governance summary',
  'result tabs',
  'el-tree',
  'el-tabs',
  'Bound SQL preview'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Query workbench contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Query workbench contract check passed.')
