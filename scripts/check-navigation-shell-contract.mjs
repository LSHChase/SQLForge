import { readFileSync } from 'node:fs'
import {
  APP_ROUTE_DEFINITIONS,
  DELIVERY_PROGRESS_ROUTE_DEFINITION,
  LEGACY_ROUTE_DEFINITIONS,
  LEGACY_ROUTE_REDIRECTS,
  ROUTE_PATHS,
  buildNavigationBreadcrumb,
  buildNavigationKey,
  createNavigationTree,
  findActiveNavigationItem,
  flattenNavigationItems
} from '../src/config/routePaths.mjs'

const appSource = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')
const routerSource = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')

const errors = []

const check = (condition, message) => {
  if (!condition) {
    errors.push(message)
  }
}

const findRoute = path => APP_ROUTE_DEFINITIONS.find(route => route.path === path)
const findLegacyRoute = path => LEGACY_ROUTE_DEFINITIONS.find(route => route.path === path)

const expectedRoutePaths = {
  dashboard: '/dashboard',
  sqlQuery: '/sql-query',
  acceleration: '/acceleration',
  accelerationGovernanceWorkbench: '/governance/acceleration-workbench',
  benchmark: '/benchmark',
  routingGovernance: '/governance/routing',
  recommendationCenter: '/governance/recommendations',
  accessCenter: '/governance/access',
  alertCenter: '/governance/alerts',
  parseBatchCenter: '/governance/parse/batches',
  parseStatisticsCenter: '/governance/parse/statistics',
  assetCatalog: '/governance/assets/catalog',
  system: '/system',
  sqlHistory: '/governance/history/sql-history',
  parseRecord: '/governance/history/parse-record',
  repairEvidence: '/governance/history/repair-evidence',
  auditForensics: '/governance/history/audit-forensics',
  auditTroubleshooting: '/governance/ops/remediation',
  runtimeGates: '/governance/ops/runtime-gates',
  recoveryDrill: '/governance/ops/recovery-drill',
  deliveryProgress: '/delivery-progress'
}

const expectedLegacyRedirects = {
  parseRecord: '/parse-record',
  parseBatchCenter: '/parse-batches',
  parseStatisticsCenter: '/parse-statistics',
  routingGovernance: '/routing-governance',
  recommendationCenter: '/recommendations',
  accessCenter: '/open-access',
  alertCenter: '/alerts',
  assetCatalog: '/asset-catalog',
  repairEvidence: '/repair-evidence',
  auditForensics: '/audit-forensics',
  auditTroubleshooting: '/audit-troubleshooting'
}

for (const [key, value] of Object.entries(expectedRoutePaths)) {
  check(ROUTE_PATHS[key] === value, `ROUTE_PATHS.${key} drifted: expected ${value}, got ${ROUTE_PATHS[key]}`)
}

for (const [key, value] of Object.entries(expectedLegacyRedirects)) {
  check(
    LEGACY_ROUTE_REDIRECTS[key] === value,
    `LEGACY_ROUTE_REDIRECTS.${key} drifted: expected ${value}, got ${LEGACY_ROUTE_REDIRECTS[key]}`
  )
}

const appRequiredTokens = [
  'adaptiveNavigation',
  'createNavigationTree',
  'findActiveNavigationItem',
  'buildNavigationBreadcrumb',
  'buildNavigationKey',
  'referencePagesEnabled',
  'menu-item-badge',
  'menu-module-item',
  'menu-item-caption',
  'breadcrumbText',
  'activeMenuKey'
]

for (const token of appRequiredTokens) {
  check(appSource.includes(token), `App shell is missing token: ${token}`)
}

const routerRequiredTokens = [
  'APP_ROUTE_DEFINITIONS',
  'LEGACY_ROUTE_DEFINITIONS',
  'DELIVERY_PROGRESS_ROUTE_DEFINITION',
  'DeliveryProgressView',
  'ParseStatisticsCenterView',
  'routeFromDefinition'
]

for (const token of routerRequiredTokens) {
  check(routerSource.includes(token), `Router shell is missing token: ${token}`)
}

const defaultTree = createNavigationTree()
const fullTree = createNavigationTree({ includeDeliveryProgress: true, includeReferencePages: true })
const defaultModuleKeys = defaultTree.map(item => item.key)
const expectedDefaultModuleKeys = [
  'dashboard',
  'sql-query',
  'sql-history',
  'parse-acceleration',
  'parse-history',
  'recommendations',
  'rewrite-governance',
  'auxiliary-governance',
  'assets',
  'benchmark',
  'system',
  'access'
]
check(
  JSON.stringify(defaultModuleKeys) === JSON.stringify(expectedDefaultModuleKeys),
  `Default navigation module order drifted: ${defaultModuleKeys.join(', ')}`
)
check(!defaultTree.some(item => item.key === 'reference-pages'), 'reference pages must be hidden from default navigation tree.')
check(fullTree.some(item => item.key === 'reference-pages'), 'reference pages must be present when explicitly enabled.')

const sqlHistoryModule = fullTree.find(item => item.key === 'sql-history')
const parseModule = fullTree.find(item => item.key === 'parse-acceleration')
const parseHistoryModule = fullTree.find(item => item.key === 'parse-history')
const recommendationModule = fullTree.find(item => item.key === 'recommendations')
const rewriteGovernanceModule = fullTree.find(item => item.key === 'rewrite-governance')
const auxiliaryModule = fullTree.find(item => item.key === 'auxiliary-governance')
const referenceModule = fullTree.find(item => item.key === 'reference-pages')
check(sqlHistoryModule?.directItem?.routeKey === 'sqlHistory', 'SQL history must be a core direct menu entry.')
check(Array.isArray(parseModule?.items) && !parseModule.sections, 'SQL parse must flatten directly to module items.')
check(
  JSON.stringify(parseModule?.items?.map(item => item.routeKey)) === JSON.stringify(['acceleration', 'parseBatchCenter']),
  'SQL parse module must only expose single-SQL parse and batch parse center.'
)
check(parseHistoryModule?.directItem?.routeKey === 'parseRecord', 'Parse history must be a core direct menu entry.')
check(recommendationModule?.directItem?.routeKey === 'recommendationCenter', 'Recommendation results must be a core direct menu entry.')
check(
  JSON.stringify(rewriteGovernanceModule?.items?.map(item => item.menuKey)) ===
    JSON.stringify([
      '/acceleration?mode=rewriteValidation',
      '/governance/recommendations?tab=rewriteLifecycle',
      '/governance/history/sql-history?detailTab=rewriteRecords&hasRewriteRecord=true'
    ]),
  '改写治理正式导航入口必须指向 SQL 改写验证、推荐改写生命周期与 SQL 改写历史深链。'
)
check(Array.isArray(auxiliaryModule?.sections), 'Auxiliary governance must group audit, trace, alert and runtime evidence.')
check(
  Array.isArray(referenceModule?.items) &&
    referenceModule.items.some(item => item.routeKey === 'accelerationGovernanceWorkbench') &&
    referenceModule.items.some(item => item.routeKey === 'deliveryProgress'),
  'Reference pages must contain acceleration workbench and AI delivery when explicitly enabled.'
)

const defaultNavItems = flattenNavigationItems(defaultTree)
const fullNavItems = flattenNavigationItems(fullTree)
const requiredDefaultNavTargets = [
  ROUTE_PATHS.dashboard,
  ROUTE_PATHS.sqlQuery,
  ROUTE_PATHS.sqlHistory,
  ROUTE_PATHS.acceleration,
  ROUTE_PATHS.parseBatchCenter,
  ROUTE_PATHS.parseRecord,
  ROUTE_PATHS.recommendationCenter,
  ROUTE_PATHS.auditForensics,
  ROUTE_PATHS.repairEvidence,
  ROUTE_PATHS.routingGovernance,
  ROUTE_PATHS.alertCenter,
  ROUTE_PATHS.auditTroubleshooting,
  ROUTE_PATHS.runtimeGates,
  ROUTE_PATHS.recoveryDrill,
  ROUTE_PATHS.assetCatalog,
  ROUTE_PATHS.benchmark,
  ROUTE_PATHS.system,
  ROUTE_PATHS.accessCenter
]

for (const path of requiredDefaultNavTargets) {
  check(defaultNavItems.some(item => item.path === path), `Default navigation tree is missing menu path: ${path}`)
}
check(
  !defaultNavItems.some(item => item.path === ROUTE_PATHS.accelerationGovernanceWorkbench),
  'Acceleration governance workbench must not appear in default formal navigation.'
)
check(!defaultNavItems.some(item => item.path === ROUTE_PATHS.deliveryProgress), 'AI delivery must not appear in default formal navigation.')
check(
  fullNavItems.some(item => item.path === ROUTE_PATHS.accelerationGovernanceWorkbench),
  'Full navigation tree is missing acceleration governance workbench reference page.'
)
check(fullNavItems.some(item => item.path === ROUTE_PATHS.deliveryProgress), 'Full navigation tree is missing AI delivery reference page.')

const runtimeItem = findActiveNavigationItem(defaultTree, { path: ROUTE_PATHS.runtimeGates, query: {} })
check(runtimeItem?.moduleKey === 'auxiliary-governance', 'Runtime gates active item must stay under auxiliary governance.')
check(runtimeItem?.sectionKey === 'runtime', 'Runtime gates active item must stay under the runtime section.')
check(
  JSON.stringify(runtimeItem?.defaultOpeneds) === JSON.stringify(['auxiliary-governance', 'auxiliary-governance:runtime']),
  'Runtime gates default open menu state drifted.'
)

const parseRecordItem = findActiveNavigationItem(defaultTree, { path: ROUTE_PATHS.parseRecord, query: {} })
check(parseRecordItem?.moduleKey === 'parse-history', 'Parse record active item must stay under parse history.')
check(JSON.stringify(parseRecordItem?.defaultOpeneds) === JSON.stringify([]), 'Parse record open state drifted.')

const rewriteValidationItem = findActiveNavigationItem(defaultTree, {
  path: ROUTE_PATHS.acceleration,
  query: { mode: 'rewriteValidation' }
})
check(rewriteValidationItem?.moduleKey === 'rewrite-governance', 'SQL 改写验证深链必须命中改写治理导航分组。')
const rewriteRecordItem = findActiveNavigationItem(defaultTree, {
  path: ROUTE_PATHS.recommendationCenter,
  query: { tab: 'rewriteLifecycle' }
})
check(rewriteRecordItem?.moduleKey === 'rewrite-governance', '改写记录深链必须命中改写治理导航分组。')
const rewriteHistoryItem = findActiveNavigationItem(defaultTree, {
  path: ROUTE_PATHS.sqlHistory,
  query: { hasRewriteRecord: 'true', detailTab: 'rewriteRecords' }
})
check(rewriteHistoryItem?.moduleKey === 'rewrite-governance', '改写历史深链必须命中改写治理导航分组。')

const hiddenWorkbenchItem = findActiveNavigationItem(defaultTree, { path: ROUTE_PATHS.accelerationGovernanceWorkbench, query: {} })
check(!hiddenWorkbenchItem, 'Acceleration governance workbench must be hidden from default formal navigation.')
const accelerationWorkbenchItem = findActiveNavigationItem(fullTree, { path: ROUTE_PATHS.accelerationGovernanceWorkbench, query: {} })
check(
  accelerationWorkbenchItem?.moduleKey === 'reference-pages',
  'Acceleration governance workbench active item must stay under reference pages when explicitly enabled.'
)
check(
  accelerationWorkbenchItem?.menuLabel === 'navigation.items.accelerationGovernanceWorkbench',
  'Acceleration governance workbench menu label drifted.'
)
check(
  JSON.stringify(accelerationWorkbenchItem?.defaultOpeneds) === JSON.stringify(['reference-pages']),
  'Acceleration governance workbench open state drifted.'
)

const unknownMenuKey = buildNavigationKey('/unknown', { z: 'last', a: 'first', empty: '' })
check(unknownMenuKey === '/unknown?a=first&z=last', `Navigation key normalization drifted: ${unknownMenuKey}`)

const runtimeBreadcrumb = buildNavigationBreadcrumb(
  runtimeItem,
  value => value,
  item => item.menuLabel
)
check(
  JSON.stringify(runtimeBreadcrumb) === JSON.stringify([
    'navigation.modules.auxiliaryGovernance',
    'navigation.sections.runtimeGovernance',
    'navigation.items.runtimeGates'
  ]),
  'Workspace breadcrumb metadata drifted for runtime gates.'
)

const requiredRouteTargets = [
  ...requiredDefaultNavTargets,
  ROUTE_PATHS.accelerationGovernanceWorkbench,
  ROUTE_PATHS.parseStatisticsCenter
]

for (const path of requiredRouteTargets) {
  check(findRoute(path), `APP_ROUTE_DEFINITIONS is missing route path: ${path}`)
}
check(!findRoute(ROUTE_PATHS.deliveryProgress), 'delivery-progress must not be part of default APP_ROUTE_DEFINITIONS.')
check(
  DELIVERY_PROGRESS_ROUTE_DEFINITION.path === ROUTE_PATHS.deliveryProgress,
  'DELIVERY_PROGRESS_ROUTE_DEFINITION must own /delivery-progress separately.'
)
check(
  DELIVERY_PROGRESS_ROUTE_DEFINITION.meta?.temporary === true && DELIVERY_PROGRESS_ROUTE_DEFINITION.meta?.envLimited === true,
  'delivery-progress route must remain temporary and environment-limited.'
)

for (const path of Object.values(LEGACY_ROUTE_REDIRECTS)) {
  check(findLegacyRoute(path), `LEGACY_ROUTE_DEFINITIONS is missing legacy path: ${path}`)
}

const parseBatchRedirect = findLegacyRoute(LEGACY_ROUTE_REDIRECTS.parseBatchCenter)?.redirect({ query: { source: 'legacy' } })
check(parseBatchRedirect?.path === ROUTE_PATHS.acceleration, 'Legacy parse batch redirect must target acceleration.')
check(parseBatchRedirect?.query?.workspace === 'batch', 'Legacy parse batch redirect must preserve workspace=batch.')
check(parseBatchRedirect?.query?.source === 'legacy', 'Legacy parse batch redirect must preserve incoming query.')

const legacyStatisticsRedirect = findLegacyRoute(LEGACY_ROUTE_REDIRECTS.parseStatisticsCenter)?.redirect({ query: {} })
check(legacyStatisticsRedirect?.path === ROUTE_PATHS.parseStatisticsCenter, 'Legacy parse statistics redirect must target the statistics center.')
check(legacyStatisticsRedirect?.query?.analytics === 'issue', 'Legacy parse statistics redirect must default analytics=issue.')

const canonicalStatisticsRoute = findRoute(ROUTE_PATHS.parseStatisticsCenter)
check(canonicalStatisticsRoute?.componentKey === 'ParseStatisticsCenterView', 'Canonical parse statistics route must render ParseStatisticsCenterView.')
check(!canonicalStatisticsRoute?.redirect, 'Canonical parse statistics route must not redirect back to the parse workbench.')

if (errors.length > 0) {
  console.error('Navigation shell contract check failed.')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

console.log('navigation shell contract ok')
