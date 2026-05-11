import { readFileSync } from 'node:fs'
import {
  APP_ROUTE_DEFINITIONS,
  NAVIGATION_TREE,
  ROUTE_PATHS
} from '../src/config/routePaths.mjs'

const errors = []

const check = (condition, message) => {
  if (!condition) {
    errors.push(message)
  }
}

const source = readFileSync(
  new URL('../src/views/acceleration-governance/AccelerationGovernanceWorkbenchView.vue', import.meta.url),
  'utf8'
)

const route = APP_ROUTE_DEFINITIONS.find(item => item.name === 'AccelerationGovernanceWorkbench')
check(ROUTE_PATHS.accelerationGovernanceWorkbench === '/governance/acceleration-workbench', 'Workbench route path drifted.')
check(route?.path === ROUTE_PATHS.accelerationGovernanceWorkbench, 'Workbench route definition is missing.')
check(route?.componentKey === 'AccelerationGovernanceWorkbenchView', 'Workbench route must render AccelerationGovernanceWorkbenchView.')
check(route?.meta?.module === 'parse-acceleration', 'Workbench must stay under parse-acceleration navigation module.')
check(route?.meta?.pageKind === 'governance', 'Workbench pageKind must remain governance.')

const parseModule = NAVIGATION_TREE.find(item => item.key === 'parse-acceleration')
check(
  parseModule?.items?.some(item => item.routeKey === 'accelerationGovernanceWorkbench'),
  'Workbench is missing from parse-acceleration navigation.'
)

const requiredTokens = [
  'data-testid="acceleration-governance-workbench-page"',
  'acceleration-workbench-source-type',
  'acceleration-workbench-source-kind',
  'acceleration-workbench-source-id',
  'acceleration-workbench-parse-history-id',
  'acceleration-workbench-history-id',
  'acceleration-workbench-sql-fingerprint',
  'acceleration-workbench-evidence-level',
  'ENTRY_EVIDENCE',
  'CANDIDATE_SUGGESTION',
  'SQL_DIFF',
  'PLAN_APPROVAL',
  'APPLY_VALIDATION',
  'MONITORING_ALERT',
  'ROLLBACK_DISCARD',
  'acceleration-workbench-evidence-drawer',
  'ROUTE_PATHS.parseRecord',
  'ROUTE_PATHS.sqlHistory',
  'ROUTE_PATHS.recommendationCenter',
  'ROUTE_PATHS.sqlQuery',
  'ROUTE_PATHS.alertCenter',
  'HARN-137_READ_ONLY_SHELL',
  'HARN-138'
]

for (const token of requiredTokens) {
  check(source.includes(token), `Workbench source is missing token: ${token}`)
}

const forbiddenTokens = [
  'runtimeGateApi',
  'axios',
  'fetch(',
  'createCandidate(',
  'applyPlan(',
  'verifyPlan(',
  'mock success',
  'MOCK_SUCCESS'
]

for (const token of forbiddenTokens) {
  check(!source.includes(token), `Workbench shell must not contain business submit token: ${token}`)
}

check(!/<PageHero\b/.test(source), 'Workbench shell must not use PageHero.')
check(!/<el-card\b/.test(source), 'Workbench shell must not introduce el-card/card nesting.')
check(/<el-button disabled[^>]*data-testid="acceleration-workbench-future-action"/.test(source), 'Future interface actions must stay disabled.')

if (errors.length > 0) {
  console.error('Acceleration workbench contract check failed.')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

console.log('acceleration workbench contract ok')
