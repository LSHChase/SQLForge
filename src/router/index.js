import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { deliveryProgressEnabled } from '../config/runtimeFlags'
import { LEGACY_ROUTE_REDIRECTS, ROUTE_PATHS } from '../config/routePaths.mjs'

const DashboardView = () => import('../views/dashboard/DashboardView.vue')
const SqlQueryView = () => import('../views/query/SqlQueryView.vue')
const SqlHistoryView = () => import('../views/sql-history/SqlHistoryView.vue')
const ParseRecordView = () => import('../views/parse-record/ParseRecordView.vue')
const ParseBatchCenterView = () => import('../views/parse-batch/ParseBatchCenterView.vue')
const AssetCatalogView = () => import('../views/asset-catalog/AssetCatalogView.vue')
const RepairEvidenceView = () => import('../views/repair-evidence/RepairEvidenceView.vue')
const AuditForensicsView = () => import('../views/audit-forensics/AuditForensicsView.vue')
const AuditTroubleshootingView = () => import('../views/audit-troubleshooting/AuditTroubleshootingView.vue')
const RuntimeGatesView = () => import('../views/runtime-gates/RuntimeGatesView.vue')
const RecoveryDrillView = () => import('../views/recovery-drill/RecoveryDrillView.vue')
const AccelerationView = () => import('../views/optimization/AccelerationView.vue')
const BenchmarkView = () => import('../views/benchmark/BenchmarkView.vue')
const RoutingGovernanceView = () => import('../views/routing-governance/RoutingGovernanceView.vue')
const RecommendationCenterView = () => import('../views/recommendation-center/RecommendationCenterView.vue')
const AccessCenterView = () => import('../views/access-center/AccessCenterView.vue')
const AlertCenterView = () => import('../views/alert-center/AlertCenterView.vue')
const SystemView = () => import('../views/system/SystemView.vue')

export const constantRoutes = [
  {
    path: '/',
    redirect: ROUTE_PATHS.dashboard
  },
  {
    path: ROUTE_PATHS.dashboard,
    name: 'Dashboard',
    component: DashboardView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'dashboard',
      submodule: 'overview',
      pageKind: 'overview',
      titleKey: 'dashboard.title',
      descriptionKey: 'dashboard.summary'
    }
  },
  {
    path: ROUTE_PATHS.sqlQuery,
    name: 'SqlQuery',
    component: SqlQueryView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'sql-query',
      submodule: 'workbench',
      pageKind: 'workbench',
      titleKey: 'sqlQuery.title',
      descriptionKey: 'sqlQuery.summary'
    }
  },
  {
    path: ROUTE_PATHS.acceleration,
    name: 'Acceleration',
    component: AccelerationView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'parse-acceleration',
      submodule: 'parse',
      pageKind: 'workbench',
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    }
  },
  {
    path: ROUTE_PATHS.benchmark,
    name: 'Benchmark',
    component: BenchmarkView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'benchmark',
      submodule: 'benchmark-workspace',
      pageKind: 'workspace',
      titleKey: 'benchmark.title',
      descriptionKey: 'benchmark.summary'
    }
  },
  {
    path: ROUTE_PATHS.routingGovernance,
    name: 'RoutingGovernance',
    component: RoutingGovernanceView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'routing',
      submodule: 'routing-policy',
      pageKind: 'governance',
      titleKey: 'routingGovernance.title',
      descriptionKey: 'routingGovernance.summary'
    }
  },
  {
    path: ROUTE_PATHS.recommendationCenter,
    name: 'RecommendationCenter',
    component: RecommendationCenterView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'parse-acceleration',
      submodule: 'rewrite',
      pageKind: 'governance',
      titleKey: 'recommendationCenter.title',
      descriptionKey: 'recommendationCenter.summary'
    }
  },
  {
    path: ROUTE_PATHS.accessCenter,
    name: 'AccessCenter',
    component: AccessCenterView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'access',
      submodule: 'access-overview',
      pageKind: 'governance',
      titleKey: 'accessCenter.title',
      descriptionKey: 'accessCenter.summary'
    }
  },
  {
    path: ROUTE_PATHS.alertCenter,
    name: 'AlertCenter',
    component: AlertCenterView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'system',
      submodule: 'alerts',
      pageKind: 'operations',
      titleKey: 'alertCenter.title',
      descriptionKey: 'alertCenter.summary'
    }
  },
  {
    path: ROUTE_PATHS.system,
    name: 'System',
    component: SystemView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'system',
      submodule: 'config',
      pageKind: 'management',
      titleKey: 'system.title',
      descriptionKey: 'system.summary'
    }
  },
  {
    path: ROUTE_PATHS.parseBatchCenter,
    name: 'ParseBatchCenter',
    component: ParseBatchCenterView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'parse-acceleration',
      submodule: 'batch',
      pageKind: 'workspace',
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    }
  },
  {
    path: ROUTE_PATHS.parseStatisticsCenter,
    redirect: to => ({
      path: ROUTE_PATHS.acceleration,
      query: {
        ...to.query,
        workspace: 'statistics',
        analytics: String(to.query.analytics || 'issue')
      }
    })
  },
  {
    path: ROUTE_PATHS.assetCatalog,
    name: 'AssetCatalog',
    component: AssetCatalogView,
    meta: {
      menu: true,
      navGroup: 'main',
      module: 'assets',
      submodule: 'catalog',
      pageKind: 'catalog',
      titleKey: 'assetCatalog.title',
      descriptionKey: 'assetCatalog.summary'
    }
  },
  {
    path: ROUTE_PATHS.sqlHistory,
    name: 'SqlHistory',
    component: SqlHistoryView,
    meta: {
      menu: true,
      navGroup: 'governanceHistory',
      module: 'sql-history',
      submodule: 'history',
      pageKind: 'history',
      titleKey: 'sqlHistory.title',
      descriptionKey: 'sqlHistory.summary'
    }
  },
  {
    path: ROUTE_PATHS.parseRecord,
    name: 'ParseRecord',
    component: ParseRecordView,
    meta: {
      menu: true,
      navGroup: 'governanceHistory',
      module: 'parse-acceleration',
      submodule: 'history',
      pageKind: 'history',
      historyWorkbenchTab: 'batchHistory',
      titleKey: 'parseRecord.title',
      descriptionKey: 'parseRecord.summary'
    }
  },
  {
    path: ROUTE_PATHS.repairEvidence,
    name: 'RepairEvidence',
    component: RepairEvidenceView,
    meta: {
      menu: true,
      navGroup: 'governanceHistory',
      module: 'sql-history',
      submodule: 'forensics',
      pageKind: 'repair',
      titleKey: 'repairEvidence.title',
      descriptionKey: 'repairEvidence.summary'
    }
  },
  {
    path: ROUTE_PATHS.auditForensics,
    name: 'AuditForensics',
    component: AuditForensicsView,
    meta: {
      menu: true,
      navGroup: 'governanceHistory',
      module: 'sql-history',
      submodule: 'forensics',
      pageKind: 'forensics',
      titleKey: 'auditForensics.title',
      descriptionKey: 'auditForensics.summary'
    }
  },
  {
    path: ROUTE_PATHS.auditTroubleshooting,
    name: 'AuditTroubleshooting',
    component: AuditTroubleshootingView,
    meta: {
      menu: true,
      navGroup: 'governanceOps',
      module: 'system',
      submodule: 'alerts',
      pageKind: 'remediation',
      titleKey: 'auditTroubleshooting.title',
      descriptionKey: 'auditTroubleshooting.summary'
    }
  },
  {
    path: ROUTE_PATHS.runtimeGates,
    name: 'RuntimeGates',
    component: RuntimeGatesView,
    meta: {
      menu: true,
      navGroup: 'governanceOps',
      module: 'system',
      submodule: 'runtime',
      pageKind: 'gates',
      titleKey: 'runtimeGates.title',
      descriptionKey: 'runtimeGates.summary'
    }
  },
  {
    path: ROUTE_PATHS.recoveryDrill,
    name: 'RecoveryDrill',
    component: RecoveryDrillView,
    meta: {
      menu: true,
      navGroup: 'governanceOps',
      module: 'system',
      submodule: 'runtime',
      pageKind: 'drill',
      titleKey: 'recoveryDrill.title',
      descriptionKey: 'recoveryDrill.summary'
    }
  },
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
      path: ROUTE_PATHS.acceleration,
      query: {
        ...to.query,
        workspace: 'statistics',
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

if (deliveryProgressEnabled) {
  constantRoutes.push({
    path: ROUTE_PATHS.deliveryProgress,
    name: 'DeliveryProgress',
    component: () => import('../views/delivery/DeliveryProgressView.vue'),
    meta: {
      menu: true,
      navGroup: 'temporary',
      temporary: true,
      envLimited: true,
      titleKey: 'deliveryProgress.title',
      descriptionKey: 'deliveryProgress.summary'
    }
  })
}

const historyFactory =
  import.meta.env.MODE === 'portable' ? createWebHashHistory : createWebHistory

const router = createRouter({
  history: historyFactory(import.meta.env.BASE_URL),
  routes: constantRoutes
})

export default router
