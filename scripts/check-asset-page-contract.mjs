import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/asset-catalog/AssetCatalogView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'data-testid="asset-page"',
  'data-testid="asset-filter-datasource"',
  'data-testid="asset-refresh"',
  'data-testid="asset-item"',
  'data-testid="asset-detail-panel"',
  'data-testid="asset-error"',
  ':data-testid="`asset-tab-${option.value}`"',
  "value: 'datasources'",
  "value: 'schemas'",
  "value: 'tables'",
  "value: 'logicalViews'",
  "value: 'databaseViews'",
  'data asset catalog',
  'Asset catalog',
  'Metadata snapshot evidence',
  'Logical views',
  'Database views'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Asset page contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Asset page contract check passed.')
