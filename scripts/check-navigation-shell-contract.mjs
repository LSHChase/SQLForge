import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

const requiredTokens = [
  'Adaptive navigation',
  'buildNavKey',
  "directItem: {",
  "key: 'dashboard'",
  "key: 'delivery-progress'",
  "key: 'sql-query'",
  "key: 'parse-acceleration'",
  "key: 'routing'",
  "key: 'access'",
  'AI delivery workbench',
  'Batch parse center',
  'Parse result center',
  'Acceleration and rewrite center',
  "workspace: 'batch'",
  "workspace: 'statistics'",
  "analytics: 'issue'",
  'Routing Governance',
  'menu-item-badge',
  'menu-module-item',
  'menu-item-caption',
  'breadcrumbText',
  'activeMenuKey'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Navigation shell contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

const flatModuleChecks = [
  {
    key: 'sql-history',
    label: 'SQL history'
  },
  {
    key: 'parse-acceleration',
    label: 'Parsing and acceleration'
  }
]

for (const moduleCheck of flatModuleChecks) {
  const itemsPattern = new RegExp(`key: '${moduleCheck.key}'[\\s\\S]{0,700}?items: \\[`)
  const sectionsPattern = new RegExp(`key: '${moduleCheck.key}'[\\s\\S]{0,700}?sections:`)
  if (!itemsPattern.test(source) || sectionsPattern.test(source)) {
    console.error(`Navigation shell contract check failed: ${moduleCheck.label} must flatten directly to module items.`)
    process.exit(1)
  }
}

console.log('navigation shell contract ok')
