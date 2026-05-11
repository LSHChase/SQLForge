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
const fullTree = createNavigationTree({ includeDeliveryProgress: true })
check(!defaultTree.some(item => item.key === 'delivery-progress'), 'delivery-progress must be hidden from default navigation tree.')
check(fullTree.some(item => item.key === 'delivery-progress'), 'delivery-progress must be present when explicitly enabled.')

const sqlHistoryModule = fullTree.find(item => item.key === 'sql-history')
const parseModule = fullTree.find(item => item.key === 'parse-acceleration')
check(Array.isArray(sqlHistoryModule?.items) && !sqlHistoryModule.sections, 'SQL history must flatten directly to module items.')
check(Array.isArray(parseModule?.items) && !parseModule.sections, 'Parsing and acceleration must flatten directly to module items.')

const navItems = flattenNavigationItems(fullTree)
const requiredNavTargets = [
  ROUTE_PATHS.dashboard,
  ROUTE_PATHS.sqlQuery,
  ROUTE_PATHS.sqlHistory,
  ROUTE_PATHS.repairEvidence,
  ROUTE_PATHS.auditForensics,
  ROUTE_PATHS.acceleration,
  ROUTE_PATHS.accelerationGovernanceWorkbench,
  ROUTE_PATHS.parseStatisticsCenter,
  ROUTE_PATHS.parseBatchCenter,
  ROUTE_PATHS.parseRecord,
  ROUTE_PATHS.recommendationCenter,
  ROUTE_PATHS.routingGovernance,
  ROUTE_PATHS.assetCatalog,
  ROUTE_PATHS.benchmark,
  ROUTE_PATHS.system,
  ROUTE_PATHS.alertCenter,
  ROUTE_PATHS.auditTroubleshooting,
  ROUTE_PATHS.runtimeGates,
  ROUTE_PATHS.recoveryDrill,
  ROUTE_PATHS.accessCenter,
  ROUTE_PATHS.deliveryProgress
]

for (const path of requiredNavTargets) {
  check(navItems.some(item => item.path === path), `Navigation tree is missing menu path: ${path}`)
}

const runtimeItem = findActiveNavigationItem(fullTree, { path: ROUTE_PATHS.runtimeGates, query: {} })
check(runtimeItem?.moduleKey === 'system', 'Runtime gates active item must stay under the system module.')
check(runtimeItem?.sectionKey === 'runtime', 'Runtime gates active item must stay under the runtime section.')
check(
  JSON.stringify(runtimeItem?.defaultOpeneds) === JSON.stringify(['system', 'system:runtime']),
  'Runtime gates default open menu state drifted.'
)

const parseRecordItem = findActiveNavigationItem(fullTree, { path: ROUTE_PATHS.parseRecord, query: {} })
check(parseRecordItem?.moduleKey === 'parse-acceleration', 'Parse record active item must stay under parsing and acceleration.')
check(JSON.stringify(parseRecordItem?.defaultOpeneds) === JSON.stringify(['parse-acceleration']), 'Parse record open state drifted.')

const accelerationWorkbenchItem = findActiveNavigationItem(fullTree, { path: ROUTE_PATHS.accelerationGovernanceWorkbench, query: {} })
check(
  accelerationWorkbenchItem?.moduleKey === 'parse-acceleration',
  'Acceleration governance workbench active item must stay under parsing and acceleration.'
)
check(
  accelerationWorkbenchItem?.menuLabel === 'navigation.items.accelerationGovernanceWorkbench',
  'Acceleration governance workbench menu label drifted.'
)
check(
  JSON.stringify(accelerationWorkbenchItem?.defaultOpeneds) === JSON.stringify(['parse-acceleration']),
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
    'navigation.modules.system',
    'navigation.sections.runtimeGovernance',
    'navigation.items.runtimeGates'
  ]),
  'Workspace breadcrumb metadata drifted for runtime gates.'
)

for (const path of requiredNavTargets.filter(path => path !== ROUTE_PATHS.deliveryProgress)) {
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
