import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const errors = []

const readFile = relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8')
const exists = relativePath => fs.existsSync(path.join(root, relativePath))
const check = (condition, message) => {
  if (!condition) {
    errors.push(message)
  }
}

const packageJson = JSON.parse(readFile('package.json'))
const smokeScript = readFile('scripts/run-rewrite-governance-smoke.sh')
const runbook = readFile('docs/deployments/rewrite-governance-smoke-runbook.md')
const docsReadme = readFile('docs/README.md')
const localDevelopment = readFile('docs/operations/local-development.md')
const ciBaseline = readFile('docs/deployments/ci-capability-baseline.md')

check(
  packageJson.scripts?.['smoke:rewrite-governance'] === 'bash scripts/run-rewrite-governance-smoke.sh',
  'package.json 必须用 smoke:rewrite-governance 暴露改写治理 smoke 脚本。'
)
check(
  packageJson.scripts?.['smoke:production-rewrite-closed-loop'] ===
    'node scripts/check-production-rewrite-closed-loop-browser-smoke.mjs',
  'package.json 必须保留 smoke:production-rewrite-closed-loop 指向 PRW-012 浏览器 smoke。'
)
check(!packageJson.scripts?.['smoke:acceleration-workbench'], 'package.json 不得继续暴露已移除的旧参考页 smoke。')
check(!packageJson.scripts?.['smoke:acceleration-governance'], 'package.json 不得继续暴露已移除的旧加速治理 bundle。')

const removedPaths = [
  'src/views/acceleration-governance/AccelerationGovernanceWorkbenchView.vue',
  'scripts/check-acceleration-workbench-contract.mjs',
  'scripts/check-acceleration-workbench-browser-smoke.mjs',
  'scripts/run-acceleration-rewrite-governance-smoke.sh',
  'scripts/check-acceleration-rewrite-governance-closeout.mjs',
  'docs/product/acceleration-rewrite-governance-workbench-spec.md',
  'docs/deployments/acceleration-rewrite-governance-smoke-runbook.md'
]

for (const relativePath of removedPaths) {
  check(!exists(relativePath), `已移除的旧参考页产物仍存在: ${relativePath}`)
}

const requiredSmokeTokens = [
  'check-rewrite-governance-closeout.mjs',
  'check-recommendation-page-contract.mjs',
  'check-history-page-contract.mjs',
  'npm run test:frontend-page-governance',
  'npm run smoke:production-rewrite-closed-loop',
  '--skip-browser'
]

for (const token of requiredSmokeTokens) {
  check(smokeScript.includes(token), `改写治理 smoke 脚本缺少 token: ${token}`)
}

const requiredRunbookTokens = [
  'repo-closed',
  'environment-backed',
  'HARN-016',
  'INBOX-002',
  'npm run smoke:rewrite-governance',
  'bash scripts/run-rewrite-governance-smoke.sh --skip-browser',
  'npm run smoke:production-rewrite-closed-loop',
  'PRW-012',
  'node scripts/check-recommendation-page-contract.mjs',
  'node scripts/check-history-page-contract.mjs',
  'not a default blocker'
]

for (const token of requiredRunbookTokens) {
  check(runbook.includes(token), `改写治理 runbook 缺少 token: ${token}`)
}

const docTargets = [
  {
    label: 'docs README',
    source: docsReadme,
    tokens: ['rewrite-governance-smoke-runbook.md', '改写治理 smoke runbook']
  },
  {
    label: 'local development',
    source: localDevelopment,
    tokens: ['npm run smoke:rewrite-governance', 'repo-closed', 'environment-backed']
  },
  {
    label: 'CI capability baseline',
    source: ciBaseline,
    tokens: ['npm run smoke:rewrite-governance', 'Not in CI by design', 'PRW-012']
  }
]

for (const target of docTargets) {
  for (const token of target.tokens) {
    check(target.source.includes(token), `${target.label} 缺少 token: ${token}`)
  }
}

const forbiddenActiveTokens = [
  'AccelerationGovernanceWorkbench',
  'accelerationGovernanceWorkbench',
  '/governance/acceleration-workbench',
  'check-acceleration-workbench-contract.mjs',
  'smoke:acceleration-workbench',
  'smoke:acceleration-governance',
  'run-acceleration-rewrite-governance-smoke.sh',
  'acceleration-rewrite-governance-workbench-spec.md',
  'acceleration-rewrite-governance-smoke-runbook.md'
]

for (const [label, source] of [
  ['package.json', JSON.stringify(packageJson)],
  ['rewrite smoke script', smokeScript],
  ['rewrite runbook', runbook],
  ['docs README', docsReadme],
  ['local development', localDevelopment],
  ['CI capability baseline', ciBaseline]
]) {
  for (const token of forbiddenActiveTokens) {
    check(!source.includes(token), `${label} 仍引用已移除的旧参考页 token: ${token}`)
  }
}

const forbiddenClaims = [
  'real Hetu/MRS evidence is complete',
  'real Hetu/MRS is a default blocker',
  'smoke:rewrite-governance is a release gate'
]

for (const claim of forbiddenClaims) {
  for (const [label, source] of [
    ['runbook', runbook],
    ['local development', localDevelopment],
    ['CI capability baseline', ciBaseline]
  ]) {
    check(!source.includes(claim), `${label} 包含禁止的改写治理边界声明: ${claim}`)
  }
}

if (errors.length > 0) {
  console.error('改写治理 closeout 检查失败。')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

console.log('改写治理 closeout 检查通过')
