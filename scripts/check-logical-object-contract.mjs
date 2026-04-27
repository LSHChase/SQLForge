import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const viewPath = path.join(root, 'src/views/asset-catalog/AssetCatalogView.vue')
const source = fs.readFileSync(viewPath, 'utf8')

const requiredTokens = [
  'freshness / sla / heat',
  'Freshness, SLA, and heat proxy',
  'data-testid="logical-object-usage-heat"',
  'data-testid="logical-object-related-sql"',
  'Usage heat proxy',
  'Related SQL candidates',
  'metadata snapshot aggregate',
  'viewCode',
  'reportCode',
  'physicalTargets',
  'dependencies'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Logical object contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('Logical object contract check passed.')
