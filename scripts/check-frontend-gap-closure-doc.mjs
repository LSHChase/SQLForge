import { readFileSync } from 'node:fs'

const path = 'docs/product/frontend-retrospective-gap-closure-baseline.md'
const content = readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')

const requiredTokens = [
  '解析工作台',
  '批量解析中心',
  '解析结果中心',
  '加速与改写中心',
  '路由治理',
  'Dashboard',
  'sample-aware KPI',
  'PULL_ONLY',
  'query-aware',
  'Remaining Pending Gaps'
]

for (const token of requiredTokens) {
  if (!content.includes(token)) {
    throw new Error(`Missing token ${JSON.stringify(token)} in ${path}`)
  }
}

console.log('frontend gap-closure doc ok')
