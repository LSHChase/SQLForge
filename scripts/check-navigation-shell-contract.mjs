import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')

const requiredTokens = [
  'Adaptive navigation',
  "directItem: {",
  "key: 'dashboard'",
  "key: 'delivery-progress'",
  "key: 'sql-query'",
  "key: 'routing'",
  "key: 'access'",
  'AI delivery workbench',
  'menu-item-badge',
  'menu-module-item',
  'menu-item-caption',
  'sectionLabel',
  'breadcrumbText'
]

const missing = requiredTokens.filter(token => !source.includes(token))

if (missing.length > 0) {
  console.error('Navigation shell contract check failed.')
  for (const token of missing) {
    console.error(`- missing token: ${token}`)
  }
  process.exit(1)
}

console.log('navigation shell contract ok')
