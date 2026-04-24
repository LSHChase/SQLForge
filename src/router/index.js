import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { deliveryProgressEnabled } from '../config/runtimeFlags'
import { LEGACY_ROUTE_REDIRECTS, ROUTE_PATHS } from '../config/routePaths.mjs'

const DashboardView = () => import('../views/dashboard/DashboardView.vue')
const SqlQueryView = () => import('../views/query/SqlQueryView.vue')
const ParseRecordView = () => import('../views/parse-record/ParseRecordView.vue')
const RepairEvidenceView = () => import('../views/repair-evidence/RepairEvidenceView.vue')
const AuditForensicsView = () => import('../views/audit-forensics/AuditForensicsView.vue')
const AuditTroubleshootingView = () => import('../views/audit-troubleshooting/AuditTroubleshootingView.vue')
const RuntimeGatesView = () => import('../views/runtime-gates/RuntimeGatesView.vue')
const RecoveryDrillView = () => import('../views/recovery-drill/RecoveryDrillView.vue')
const AccelerationView = () => import('../views/optimization/AccelerationView.vue')
const BenchmarkView = () => import('../views/benchmark/BenchmarkView.vue')
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
      titleKey: 'benchmark.title',
      descriptionKey: 'benchmark.summary'
    }
  },
  {
    path: ROUTE_PATHS.system,
    name: 'System',
    component: SystemView,
    meta: {
      menu: true,
      navGroup: 'main',
      titleKey: 'system.title',
      descriptionKey: 'system.summary'
    }
  },
  {
    path: ROUTE_PATHS.parseRecord,
    name: 'ParseRecord',
    component: ParseRecordView,
    meta: {
      menu: true,
      navGroup: 'governanceHistory',
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
      titleKey: 'recoveryDrill.title',
      descriptionKey: 'recoveryDrill.summary'
    }
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
