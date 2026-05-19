import { readFileSync } from 'node:fs'
import {
  APP_ROUTE_DEFINITIONS,
  ROUTE_PATHS,
  createNavigationTree,
  findActiveNavigationItem,
  flattenNavigationItems
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
const apiSource = readFileSync(
  new URL('../src/services/runtimeGateApi.js', import.meta.url),
  'utf8'
)
const zhLocaleSource = readFileSync(new URL('../src/locales/zh-CN.js', import.meta.url), 'utf8')
const enLocaleSource = readFileSync(new URL('../src/locales/en-US.js', import.meta.url), 'utf8')

const route = APP_ROUTE_DEFINITIONS.find(item => item.name === 'AccelerationGovernanceWorkbench')
check(ROUTE_PATHS.accelerationGovernanceWorkbench === '/governance/acceleration-workbench', 'Workbench route path drifted.')
check(route?.path === ROUTE_PATHS.accelerationGovernanceWorkbench, 'Workbench route definition is missing.')
check(route?.componentKey === 'AccelerationGovernanceWorkbenchView', 'Workbench route must render AccelerationGovernanceWorkbenchView.')
check(route?.meta?.module === 'reference-pages', 'Workbench route meta must identify reference pages.')
check(route?.meta?.navGroup === 'reference', 'Workbench navGroup must remain reference-only.')
check(route?.meta?.pageKind === 'reference', 'Workbench pageKind must remain reference.')

check(
  zhLocaleSource.includes("pageTitle: '加速治理流程模拟参考页'"),
  'Chinese workbench pageTitle must identify the flow simulation reference page.'
)
check(
  enLocaleSource.includes("pageTitle: 'Acceleration Governance Flow Simulation Reference Page'"),
  'English workbench pageTitle must identify the flow simulation reference page.'
)
check(
  zhLocaleSource.includes("summary: '流程模拟参考页") &&
    zhLocaleSource.includes("boundarySummary: '本页按流程模拟参考页"),
  'Chinese workbench copy must keep the flow simulation reference-page boundary.'
)
check(
  enLocaleSource.includes("summary: 'Flow simulation reference page") &&
    enLocaleSource.includes("boundarySummary: 'This page is retained as a flow simulation reference page"),
  'English workbench copy must keep the flow simulation reference-page boundary.'
)

const defaultNavigationItems = flattenNavigationItems(createNavigationTree())
const fullNavigationTree = createNavigationTree({ includeDeliveryProgress: true, includeReferencePages: true })
const referenceWorkbenchItem = findActiveNavigationItem(fullNavigationTree, {
  path: ROUTE_PATHS.accelerationGovernanceWorkbench,
  query: {}
})
check(
  !defaultNavigationItems.some(item => item.routeKey === 'accelerationGovernanceWorkbench'),
  'Workbench must not appear in the default formal navigation.'
)
check(
  referenceWorkbenchItem?.moduleKey === 'reference-pages' && referenceWorkbenchItem?.badge === 'navigation.badges.reference',
  'Workbench must be available only under the reference-pages navigation group when explicitly enabled.'
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
  'HARN-138_REAL_INTERFACE_TABS',
  'createAccelerationCandidate',
  'getRecommendationDiff',
  'submitAccelerationPlan',
  'reviewAccelerationPlan',
  'applyAccelerationPlan',
  'verifyAccelerationPlan',
  'rollbackAccelerationPlan',
  'createRewriteValidationRun',
  'getQueryHistoryRewriteRecords',
  'SQL_REWRITE_RESULT_DIVERGENCE',
  'monitoringPauseEvidenceRows',
  'acceleration-workbench-auto-pause-evidence',
  'acceleration-workbench-pause-evidence-row',
  'autoApplyPaused',
  'executeBaselineQuery',
  'executeAcceleratedQuery',
  'sourceEditorVisible',
  'pagedCandidateRows',
  'pagedRewriteRecordRows',
  'pagedValidationRuns',
  '<el-dialog',
  '<el-pagination',
  'acceleration-workbench-create-candidate',
  'acceleration-workbench-load-diff',
  'acceleration-workbench-acceleration-artifact',
  'diffAccelerationArtifact',
  'acceleration-workbench-submit-plan',
  'acceleration-workbench-approve-plan',
  'acceleration-workbench-apply-plan',
  'acceleration-workbench-verify-plan',
  'acceleration-workbench-rollback-plan',
  'acceleration-workbench-baseline-query',
  'acceleration-workbench-accelerated-query',
  'acceleration-workbench-last-evidence'
]

for (const token of requiredTokens) {
  check(source.includes(token), `Workbench source is missing token: ${token}`)
}

const forbiddenTokens = [
  'fetch(',
  'mock success',
  'MOCK_SUCCESS',
  'HARN-137_READ_ONLY_SHELL',
  'disabledUntilNextTask'
]

for (const token of forbiddenTokens) {
  check(!source.includes(token), `Workbench shell must not contain business submit token: ${token}`)
}

check(!/<PageHero\b/.test(source), 'Workbench shell must not use PageHero.')
check(!/<el-card\b/.test(source), 'Workbench shell must not introduce el-card/card nesting.')
const requiredApiTokens = [
  'export const createAccelerationCandidate',
  'export const getAccelerationCandidates',
  'export const getAccelerationCandidate',
  'export const getRecommendationDiff',
  'export const submitAccelerationPlan',
  'export const getAccelerationPlan',
  'export const reviewAccelerationPlan',
  'export const applyAccelerationPlan',
  'export const verifyAccelerationPlan',
  'export const rollbackAccelerationPlan',
  'export const createSqlRewriteRecord',
  'export const getSqlRewriteRecords',
  'export const getSqlRewriteRecord',
  'export const createRewriteValidationRun',
  'export const getRewriteValidationRuns',
  'export const getQueryHistoryRewriteRecords'
]

for (const token of requiredApiTokens) {
  check(apiSource.includes(token), `Runtime API source is missing token: ${token}`)
}

check(!source.includes('acceleration-workbench-future-action'), 'HARN-138 must replace disabled future actions with guarded real buttons.')
check(!source.includes('class="workbench-panel"'), 'Workbench must not reintroduce stacked workbench-panel sections.')
check(!source.includes('class="flow-node"'), 'Workbench must not reintroduce flow cards in the main page.')
check(!source.includes('class="jump-button"'), 'Workbench must not reintroduce navigation cards in the main page.')
check(source.includes(':disabled="!canApplyPlan"'), 'Apply button must be gated by plan status.')
check(source.includes(':disabled="!canVerifyPlan"'), 'Verify button must be gated by plan status.')
check(source.includes(':disabled="!canRollbackPlan"'), 'Rollback button must be gated by plan status.')
check(source.includes('APPLIED') && source.includes('appliedPendingVerification'), 'APPLIED must map to pending verification, not active.')

if (errors.length > 0) {
  console.error('Acceleration workbench contract check failed.')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

console.log('acceleration workbench contract ok')
