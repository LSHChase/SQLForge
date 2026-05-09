export const ROUTE_PATHS = {
  dashboard: '/dashboard',
  sqlQuery: '/sql-query',
  acceleration: '/acceleration',
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
        historyWorkbenchTab: 'batchHistory'
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
  badge: extra.badge
})

export const NAVIGATION_TREE = [
  {
    key: 'dashboard',
    label: { zh: 'Dashboard', en: 'Dashboard' },
    directItem: navItem('dashboard', 'dashboard.title', { zh: '总览首页', en: 'Overview home' })
  },
  {
    key: 'delivery-progress',
    label: { zh: 'AI 交付', en: 'AI Delivery' },
    directItem: navItem('deliveryProgress', 'deliveryProgress.title', { zh: 'AI 交付工作台', en: 'AI delivery workbench' }, {
      badge: { zh: '临时', en: 'R&D' }
    })
  },
  {
    key: 'sql-query',
    label: { zh: 'SQL 查询', en: 'SQL Query' },
    directItem: navItem('sqlQuery', 'sqlQuery.title', { zh: '查询工作台', en: 'SQL workbench' })
  },
  {
    key: 'sql-history',
    label: { zh: 'SQL 历史', en: 'SQL History' },
    items: [
      navItem('sqlHistory', 'sqlHistory.title', { zh: '历史列表', en: 'History list' }),
      navItem('repairEvidence', 'repairEvidence.title', { zh: '修复证据', en: 'Repair evidence' }),
      navItem('auditForensics', 'auditForensics.title', { zh: '审计取证', en: 'Audit forensics' })
    ]
  },
  {
    key: 'parse-acceleration',
    label: { zh: '解析与加速', en: 'Parsing and Acceleration' },
    items: [
      navItem('acceleration', 'acceleration.title', { zh: 'SQL解析', en: 'SQL Parse' }),
      navItem('parseStatisticsCenter', 'parseStatisticsCenter.title', {
        zh: '解析统计',
        en: 'Parse statistics'
      }),
      navItem('parseBatchCenter', 'acceleration.title', { zh: '批量解析中心', en: 'Batch parse center' }),
      navItem('parseRecord', 'acceleration.title', { zh: '解析历史查询', en: 'Parse history search' }),
      navItem('recommendationCenter', 'recommendationCenter.title', {
        zh: '加速与改写中心',
        en: 'Acceleration and rewrite center'
      })
    ]
  },
  {
    key: 'routing',
    label: { zh: '路由治理', en: 'Routing Governance' },
    directItem: navItem('routingGovernance', 'routingGovernance.title', {
      zh: '路由执行证据',
      en: 'Routing execution evidence'
    })
  },
  {
    key: 'assets',
    label: { zh: '数据资产', en: 'Data Assets' },
    directItem: navItem('assetCatalog', 'assetCatalog.title', { zh: '资产目录', en: 'Asset catalog' })
  },
  {
    key: 'benchmark',
    label: { zh: '压测中心', en: 'Benchmark Center' },
    directItem: navItem('benchmark', 'benchmark.title', { zh: '压测工作台', en: 'Benchmark workbench' })
  },
  {
    key: 'system',
    label: { zh: '系统管理', en: 'System Management' },
    sections: [
      {
        key: 'config',
        label: { zh: '数据源与接口', en: 'Datasources and interfaces' },
        items: [navItem('system', 'system.title', { zh: '系统管理', en: 'System management' })]
      },
      {
        key: 'alerts',
        label: { zh: '告警与处置', en: 'Alerts and remediation' },
        items: [
          navItem('alertCenter', 'alertCenter.title', { zh: '告警中心', en: 'Alert center' }),
          navItem('auditTroubleshooting', 'auditTroubleshooting.title', { zh: '故障处置', en: 'Troubleshooting' })
        ]
      },
      {
        key: 'runtime',
        label: { zh: '运行治理', en: 'Runtime governance' },
        items: [
          navItem('runtimeGates', 'runtimeGates.title', { zh: '运行时门禁', en: 'Runtime gates' }),
          navItem('recoveryDrill', 'recoveryDrill.title', { zh: '恢复演练', en: 'Recovery drill' })
        ]
      }
    ]
  },
  {
    key: 'access',
    label: { zh: '开放接入', en: 'Open Access' },
    directItem: navItem('accessCenter', 'accessCenter.title', { zh: '开放接入', en: 'Open access' })
  }
]

export const createNavigationTree = ({ includeDeliveryProgress = false } = {}) =>
  includeDeliveryProgress ? NAVIGATION_TREE : NAVIGATION_TREE.filter(item => item.key !== 'delivery-progress')

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
