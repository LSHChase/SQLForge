export const ROUTE_PATHS = {
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

export const LEGACY_ROUTE_REDIRECTS = {
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

export const ROOT_ROUTE_DEFINITION = {
  path: '/',
  redirect: ROUTE_PATHS.dashboard
}

const routeMeta = ({
  module,
  submodule,
  pageKind,
  titleKey,
  descriptionKey,
  navGroup = 'main',
  extra = {}
}) => ({
  menu: true,
  navGroup,
  module,
  submodule,
  pageKind,
  titleKey,
  descriptionKey,
  ...extra
})

const componentRoute = (routeKey, name, componentKey, meta) => ({
  path: ROUTE_PATHS[routeKey],
  name,
  componentKey,
  meta
})

export const APP_ROUTE_DEFINITIONS = [
  componentRoute(
    'dashboard',
    'Dashboard',
    'DashboardView',
    routeMeta({
      module: 'dashboard',
      submodule: 'overview',
      pageKind: 'overview',
      titleKey: 'dashboard.title',
      descriptionKey: 'dashboard.summary'
    })
  ),
  componentRoute(
    'sqlQuery',
    'SqlQuery',
    'SqlQueryView',
    routeMeta({
      module: 'sql-query',
      submodule: 'workbench',
      pageKind: 'workbench',
      titleKey: 'sqlQuery.title',
      descriptionKey: 'sqlQuery.summary'
    })
  ),
  componentRoute(
    'acceleration',
    'Acceleration',
    'AccelerationView',
    routeMeta({
      module: 'parse-acceleration',
      submodule: 'parse',
      pageKind: 'workbench',
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    })
  ),
  componentRoute(
    'accelerationGovernanceWorkbench',
    'AccelerationGovernanceWorkbench',
    'AccelerationGovernanceWorkbenchView',
    routeMeta({
      navGroup: 'reference',
      module: 'reference-pages',
      submodule: 'governance-workbench',
      pageKind: 'reference',
      titleKey: 'accelerationGovernanceWorkbench.title',
      descriptionKey: 'accelerationGovernanceWorkbench.summary'
    })
  ),
  componentRoute(
    'benchmark',
    'Benchmark',
    'BenchmarkView',
    routeMeta({
      module: 'benchmark',
      submodule: 'benchmark-workspace',
      pageKind: 'workspace',
      titleKey: 'benchmark.title',
      descriptionKey: 'benchmark.summary'
    })
  ),
  componentRoute(
    'routingGovernance',
    'RoutingGovernance',
    'RoutingGovernanceView',
    routeMeta({
      module: 'routing',
      submodule: 'routing-policy',
      pageKind: 'governance',
      titleKey: 'routingGovernance.title',
      descriptionKey: 'routingGovernance.summary'
    })
  ),
  componentRoute(
    'recommendationCenter',
    'RecommendationCenter',
    'RecommendationCenterView',
    routeMeta({
      module: 'parse-acceleration',
      submodule: 'rewrite',
      pageKind: 'governance',
      titleKey: 'recommendationCenter.title',
      descriptionKey: 'recommendationCenter.summary'
    })
  ),
  componentRoute(
    'accessCenter',
    'AccessCenter',
    'AccessCenterView',
    routeMeta({
      module: 'access',
      submodule: 'access-overview',
      pageKind: 'governance',
      titleKey: 'accessCenter.title',
      descriptionKey: 'accessCenter.summary'
    })
  ),
  componentRoute(
    'alertCenter',
    'AlertCenter',
    'AlertCenterView',
    routeMeta({
      module: 'system',
      submodule: 'alerts',
      pageKind: 'operations',
      titleKey: 'alertCenter.title',
      descriptionKey: 'alertCenter.summary'
    })
  ),
  componentRoute(
    'system',
    'System',
    'SystemView',
    routeMeta({
      module: 'system',
      submodule: 'config',
      pageKind: 'management',
      titleKey: 'system.title',
      descriptionKey: 'system.summary'
    })
  ),
  componentRoute(
    'parseBatchCenter',
    'ParseBatchCenter',
    'ParseBatchCenterView',
    routeMeta({
      module: 'parse-acceleration',
      submodule: 'batch',
      pageKind: 'workspace',
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    })
  ),
  componentRoute(
    'parseStatisticsCenter',
    'ParseStatisticsCenter',
    'ParseStatisticsCenterView',
    routeMeta({
      module: 'parse-acceleration',
      submodule: 'statistics',
      pageKind: 'statistics',
      titleKey: 'parseStatisticsCenter.title',
      descriptionKey: 'parseStatisticsCenter.summary'
    })
  ),
  componentRoute(
    'assetCatalog',
    'AssetCatalog',
    'AssetCatalogView',
    routeMeta({
      module: 'assets',
      submodule: 'catalog',
      pageKind: 'catalog',
      titleKey: 'assetCatalog.title',
      descriptionKey: 'assetCatalog.summary'
    })
  ),
  componentRoute(
    'sqlHistory',
    'SqlHistory',
    'SqlHistoryView',
    routeMeta({
      navGroup: 'governanceHistory',
      module: 'sql-history',
      submodule: 'history',
      pageKind: 'history',
      titleKey: 'sqlHistory.title',
      descriptionKey: 'sqlHistory.summary'
    })
  ),
  componentRoute(
    'parseRecord',
    'ParseRecord',
    'ParseRecordView',
    routeMeta({
      navGroup: 'governanceHistory',
      module: 'parse-acceleration',
      submodule: 'history',
      pageKind: 'history',
      titleKey: 'parseRecord.title',
      descriptionKey: 'parseRecord.summary',
      extra: {
        historyWorkbenchTab: 'sqlHistory'
      }
    })
  ),
  componentRoute(
    'repairEvidence',
    'RepairEvidence',
    'RepairEvidenceView',
    routeMeta({
      navGroup: 'governanceHistory',
      module: 'sql-history',
      submodule: 'forensics',
      pageKind: 'repair',
      titleKey: 'repairEvidence.title',
      descriptionKey: 'repairEvidence.summary'
    })
  ),
  componentRoute(
    'auditForensics',
    'AuditForensics',
    'AuditForensicsView',
    routeMeta({
      navGroup: 'governanceHistory',
      module: 'sql-history',
      submodule: 'forensics',
      pageKind: 'forensics',
      titleKey: 'auditForensics.title',
      descriptionKey: 'auditForensics.summary'
    })
  ),
  componentRoute(
    'auditTroubleshooting',
    'AuditTroubleshooting',
    'AuditTroubleshootingView',
    routeMeta({
      navGroup: 'governanceOps',
      module: 'system',
      submodule: 'alerts',
      pageKind: 'remediation',
      titleKey: 'auditTroubleshooting.title',
      descriptionKey: 'auditTroubleshooting.summary'
    })
  ),
  componentRoute(
    'runtimeGates',
    'RuntimeGates',
    'RuntimeGatesView',
    routeMeta({
      navGroup: 'governanceOps',
      module: 'system',
      submodule: 'runtime',
      pageKind: 'gates',
      titleKey: 'runtimeGates.title',
      descriptionKey: 'runtimeGates.summary'
    })
  ),
  componentRoute(
    'recoveryDrill',
    'RecoveryDrill',
    'RecoveryDrillView',
    routeMeta({
      navGroup: 'governanceOps',
      module: 'system',
      submodule: 'runtime',
      pageKind: 'drill',
      titleKey: 'recoveryDrill.title',
      descriptionKey: 'recoveryDrill.summary'
    })
  )
]

export const DELIVERY_PROGRESS_ROUTE_DEFINITION = componentRoute(
  'deliveryProgress',
  'DeliveryProgress',
  'DeliveryProgressView',
  routeMeta({
    navGroup: 'temporary',
    module: 'delivery-progress',
    submodule: 'delivery-workbench',
    pageKind: 'temporary',
    titleKey: 'deliveryProgress.title',
    descriptionKey: 'deliveryProgress.summary',
    extra: {
      temporary: true,
      envLimited: true
    }
  })
)

export const LEGACY_ROUTE_DEFINITIONS = [
  {
    path: LEGACY_ROUTE_REDIRECTS.routingGovernance,
    redirect: ROUTE_PATHS.routingGovernance
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.recommendationCenter,
    redirect: ROUTE_PATHS.recommendationCenter
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.accessCenter,
    redirect: ROUTE_PATHS.accessCenter
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.alertCenter,
    redirect: ROUTE_PATHS.alertCenter
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.parseBatchCenter,
    redirect: to => ({
      path: ROUTE_PATHS.acceleration,
      query: {
        ...to.query,
        workspace: 'batch'
      }
    })
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.parseStatisticsCenter,
    redirect: to => ({
      path: ROUTE_PATHS.parseStatisticsCenter,
      query: {
        ...to.query,
        analytics: String(to.query.analytics || 'issue')
      }
    })
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.assetCatalog,
    redirect: ROUTE_PATHS.assetCatalog
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.parseRecord,
    redirect: ROUTE_PATHS.parseRecord
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.repairEvidence,
    redirect: ROUTE_PATHS.repairEvidence
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.auditForensics,
    redirect: ROUTE_PATHS.auditForensics
  },
  {
    path: LEGACY_ROUTE_REDIRECTS.auditTroubleshooting,
    redirect: ROUTE_PATHS.auditTroubleshooting
  }
]

export const normalizeNavigationQuery = query =>
  Object.entries(query || {})
    .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== '')
    .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey))

export const buildNavigationKey = (path, query = {}) => {
  const params = new URLSearchParams()
  normalizeNavigationQuery(query).forEach(([key, value]) => {
    params.set(key, String(value))
  })
  const queryText = params.toString()
  return queryText ? `${path}?${queryText}` : path
}

export const buildNavigationTarget = (path, query = {}) => ({
  path,
  query,
  menuKey: buildNavigationKey(path, query)
})

const navItem = (routeKey, titleKey, menuLabel, extra = {}) => ({
  ...buildNavigationTarget(ROUTE_PATHS[routeKey], extra.query),
  routeKey,
  titleKey,
  menuLabel,
  badge: extra.badge,
  referencePage: Boolean(extra.referencePage)
})

export const NAVIGATION_TREE = [
  {
    key: 'dashboard',
    label: 'navigation.modules.dashboard',
    directItem: navItem('dashboard', 'dashboard.title', 'navigation.items.dashboardHome')
  },
  {
    key: 'sql-query',
    label: 'navigation.modules.sqlQuery',
    directItem: navItem('sqlQuery', 'sqlQuery.title', 'navigation.items.sqlWorkbench')
  },
  {
    key: 'sql-history',
    label: 'navigation.modules.sqlHistory',
    directItem: navItem('sqlHistory', 'sqlHistory.title', 'navigation.items.historyList')
  },
  {
    key: 'parse-acceleration',
    label: 'navigation.modules.parseAcceleration',
    items: [
      navItem('acceleration', 'acceleration.title', 'navigation.items.sqlParse'),
      navItem('parseBatchCenter', 'acceleration.title', 'navigation.items.batchParseCenter')
    ]
  },
  {
    key: 'parse-history',
    label: 'navigation.modules.parseHistory',
    directItem: navItem('parseRecord', 'parseRecord.title', 'navigation.items.parseHistorySearch')
  },
  {
    key: 'recommendations',
    label: 'navigation.modules.recommendations',
    directItem: navItem('recommendationCenter', 'recommendationCenter.title', 'navigation.items.accelerationRewriteCenter')
  },
  {
    key: 'auxiliary-governance',
    label: 'navigation.modules.auxiliaryGovernance',
    sections: [
      {
        key: 'audit-trace',
        label: 'navigation.sections.auditTrace',
        items: [
          navItem('auditForensics', 'auditForensics.title', 'navigation.items.auditForensics'),
          navItem('repairEvidence', 'repairEvidence.title', 'navigation.items.repairEvidence'),
          navItem('routingGovernance', 'routingGovernance.title', 'navigation.items.routingEvidence')
        ]
      },
      {
        key: 'alerts',
        label: 'navigation.sections.alertsRemediation',
        items: [
          navItem('alertCenter', 'alertCenter.title', 'navigation.items.alertCenter'),
          navItem('auditTroubleshooting', 'auditTroubleshooting.title', 'navigation.items.troubleshooting')
        ]
      },
      {
        key: 'runtime',
        label: 'navigation.sections.runtimeGovernance',
        items: [
          navItem('runtimeGates', 'runtimeGates.title', 'navigation.items.runtimeGates'),
          navItem('recoveryDrill', 'recoveryDrill.title', 'navigation.items.recoveryDrill')
        ]
      }
    ]
  },
  {
    key: 'assets',
    label: 'navigation.modules.assets',
    directItem: navItem('assetCatalog', 'assetCatalog.title', 'navigation.items.assetCatalog')
  },
  {
    key: 'benchmark',
    label: 'navigation.modules.benchmark',
    directItem: navItem('benchmark', 'benchmark.title', 'navigation.items.benchmarkWorkbench')
  },
  {
    key: 'system',
    label: 'navigation.modules.system',
    directItem: navItem('system', 'system.title', 'navigation.items.systemManagement')
  },
  {
    key: 'access',
    label: 'navigation.modules.access',
    directItem: navItem('accessCenter', 'accessCenter.title', 'navigation.items.openAccess')
  },
  {
    key: 'reference-pages',
    label: 'navigation.modules.referencePages',
    items: [
      navItem('accelerationGovernanceWorkbench', 'accelerationGovernanceWorkbench.title', 'navigation.items.accelerationGovernanceWorkbench', {
        badge: 'navigation.badges.reference',
        referencePage: true
      }),
      navItem('deliveryProgress', 'deliveryProgress.title', 'navigation.items.deliveryWorkbench', {
        badge: 'navigation.badges.temporary',
        referencePage: true
      })
    ]
  }
]

const navigationItemVisible = (item, { includeDeliveryProgress, includeReferencePages }) => {
  if (item.routeKey === 'deliveryProgress') {
    return includeReferencePages && includeDeliveryProgress
  }
  if (item.referencePage) {
    return includeReferencePages
  }
  return true
}

const filterNavigationModule = (module, options) => {
  if (module.directItem) {
    return navigationItemVisible(module.directItem, options) ? module : null
  }
  if (Array.isArray(module.items)) {
    const items = module.items.filter(item => navigationItemVisible(item, options))
    return items.length > 0 ? { ...module, items } : null
  }
  const sections = module.sections
    .map(section => ({
      ...section,
      items: section.items.filter(item => navigationItemVisible(item, options))
    }))
    .filter(section => section.items.length > 0)
  return sections.length > 0 ? { ...module, sections } : null
}

export const createNavigationTree = ({ includeDeliveryProgress = false, includeReferencePages = false } = {}) =>
  NAVIGATION_TREE.map(module => filterNavigationModule(module, { includeDeliveryProgress, includeReferencePages })).filter(Boolean)

export const flattenNavigationItems = tree =>
  tree.flatMap(module => {
    if (module.directItem) {
      return [
        {
          ...module.directItem,
          moduleKey: module.key,
          moduleLabel: module.label,
          sectionKey: '',
          sectionLabel: null,
          depth: 2,
          moduleHasChildren: false,
          defaultOpeneds: []
        }
      ]
    }
    if (Array.isArray(module.items)) {
      return module.items.map(item => ({
        ...item,
        moduleKey: module.key,
        moduleLabel: module.label,
        sectionKey: '',
        sectionLabel: null,
        depth: 2,
        moduleHasChildren: true,
        defaultOpeneds: [module.key]
      }))
    }
    return module.sections.flatMap(section =>
      section.items.map(item => ({
        ...item,
        moduleKey: module.key,
        moduleLabel: module.label,
        sectionKey: section.key,
        sectionLabel: section.label,
        depth: 3,
        moduleHasChildren: true,
        defaultOpeneds: [module.key, `${module.key}:${section.key}`]
      }))
    )
  })

export const navigationItemMatchesRoute = (item, currentRoute) => {
  if (item.path !== currentRoute.path) {
    return false
  }
  return normalizeNavigationQuery(item.query).every(([key, value]) => String(currentRoute.query?.[key] ?? '') === String(value))
}

export const findActiveNavigationItem = (tree, currentRoute) => {
  const items = flattenNavigationItems(tree).sort(
    (left, right) => normalizeNavigationQuery(right.query).length - normalizeNavigationQuery(left.query).length
  )
  return items.find(item => navigationItemMatchesRoute(item, currentRoute)) || null
}

export const buildNavigationBreadcrumb = (activeItem, moduleLabelResolver, itemLabelResolver) => {
  if (!activeItem) {
    return []
  }
  const parts = [moduleLabelResolver(activeItem.moduleLabel)]
  if (activeItem.sectionLabel) {
    parts.push(moduleLabelResolver(activeItem.sectionLabel))
  }
  parts.push(itemLabelResolver(activeItem))
  return parts
}
