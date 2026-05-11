import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()
const errors = []

const readFile = relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8')
const check = (condition, message) => {
  if (!condition) {
    errors.push(message)
  }
}

const requireFile = relativePath => {
  const absolutePath = path.join(root, relativePath)
  check(fs.existsSync(absolutePath), `Missing required file: ${relativePath}`)
  return fs.existsSync(absolutePath) ? fs.readFileSync(absolutePath, 'utf8') : ''
}

const packageJson = JSON.parse(readFile('package.json'))
const smokeScript = requireFile('scripts/run-acceleration-rewrite-governance-smoke.sh')
const runbook = requireFile('docs/deployments/acceleration-rewrite-governance-smoke-runbook.md')
const docsReadme = requireFile('docs/README.md')
const localDevelopment = requireFile('docs/operations/local-development.md')
const ciBaseline = requireFile('docs/deployments/ci-capability-baseline.md')
const workbenchSpec = requireFile('docs/product/acceleration-rewrite-governance-workbench-spec.md')
const coverageMatrix = requireFile('docs/plans/document-coverage-matrix.md')

check(
  packageJson.scripts?.['smoke:acceleration-governance'] ===
    'bash scripts/run-acceleration-rewrite-governance-smoke.sh',
  'package.json must expose smoke:acceleration-governance with the HARN-142 smoke script.'
)

const requiredSmokeTokens = [
  'check-acceleration-rewrite-governance-closeout.mjs',
  'check-acceleration-workbench-contract.mjs',
  'check-recommendation-page-contract.mjs',
  'check-history-page-contract.mjs',
  'check-alert-page-contract.mjs',
  'npm run test:frontend-page-governance',
  'npm run smoke:acceleration-workbench',
  '--skip-browser'
]

for (const token of requiredSmokeTokens) {
  check(smokeScript.includes(token), `HARN-142 smoke script is missing token: ${token}`)
}

const requiredRunbookTokens = [
  'HARN-142',
  'repo-closed',
  'environment-backed',
  'HARN-016',
  'INBOX-002',
  'npm run smoke:acceleration-governance',
  'bash scripts/run-acceleration-rewrite-governance-smoke.sh --skip-browser',
  'npm run smoke:acceleration-workbench',
  'node scripts/check-acceleration-workbench-contract.mjs',
  'node scripts/check-recommendation-page-contract.mjs',
  'node scripts/check-history-page-contract.mjs',
  'node scripts/check-alert-page-contract.mjs',
  'not a default blocker'
]

for (const token of requiredRunbookTokens) {
  check(runbook.includes(token), `HARN-142 runbook is missing token: ${token}`)
}

const requiredDocTokens = [
  {
    label: 'docs README',
    source: docsReadme,
    tokens: ['acceleration-rewrite-governance-smoke-runbook.md', '加速与改写治理 smoke runbook']
  },
  {
    label: 'local development',
    source: localDevelopment,
    tokens: ['npm run smoke:acceleration-governance', 'repo-closed', 'environment-backed']
  },
  {
    label: 'CI capability baseline',
    source: ciBaseline,
    tokens: ['npm run smoke:acceleration-governance', 'Not in CI by design', 'HARN-142']
  },
  {
    label: 'workbench spec',
    source: workbenchSpec,
    tokens: ['npm run smoke:acceleration-governance', 'HARN-016', 'INBOX-002', 'not a default blocker']
  },
  {
    label: 'document coverage matrix',
    source: coverageMatrix,
    tokens: ['docs/deployments/acceleration-rewrite-governance-smoke-runbook.md', 'HARN-142']
  }
]

for (const target of requiredDocTokens) {
  for (const token of target.tokens) {
    check(target.source.includes(token), `${target.label} is missing token: ${token}`)
  }
}

const forbiddenClaims = [
  'real Hetu/MRS evidence is complete',
  'real Hetu/MRS is a default blocker',
  'smoke:acceleration-governance is a release gate'
]

for (const claim of forbiddenClaims) {
  for (const [label, source] of [
    ['runbook', runbook],
    ['local development', localDevelopment],
    ['CI capability baseline', ciBaseline],
    ['workbench spec', workbenchSpec]
  ]) {
    check(!source.includes(claim), `${label} contains forbidden HARN-142 boundary claim: ${claim}`)
  }
}

if (errors.length > 0) {
  console.error('Acceleration/rewrite governance closeout check failed.')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

console.log('acceleration rewrite governance closeout ok')
