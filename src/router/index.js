import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/dashboard/DashboardView.vue'
import SqlQueryView from '../views/query/SqlQueryView.vue'
import ParseRecordView from '../views/parse-record/ParseRecordView.vue'
import RepairEvidenceView from '../views/repair-evidence/RepairEvidenceView.vue'
import AuditForensicsView from '../views/audit-forensics/AuditForensicsView.vue'
import AuditTroubleshootingView from '../views/audit-troubleshooting/AuditTroubleshootingView.vue'
import AccelerationView from '../views/optimization/AccelerationView.vue'
import BenchmarkView from '../views/benchmark/BenchmarkView.vue'
import SystemView from '../views/system/SystemView.vue'
import { deliveryProgressEnabled } from '../config/runtimeFlags'

export const constantRoutes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView,
    meta: {
      menu: true,
      titleKey: 'dashboard.title',
      descriptionKey: 'dashboard.summary'
    }
  },
  {
    path: '/sql-query',
    name: 'SqlQuery',
    component: SqlQueryView,
    meta: {
      menu: true,
      titleKey: 'sqlQuery.title',
      descriptionKey: 'sqlQuery.summary'
    }
  },
  {
    path: '/parse-record',
    name: 'ParseRecord',
    component: ParseRecordView,
    meta: {
      menu: true,
      titleKey: 'parseRecord.title',
      descriptionKey: 'parseRecord.summary'
    }
  },
  {
    path: '/repair-evidence',
    name: 'RepairEvidence',
    component: RepairEvidenceView,
    meta: {
      menu: true,
      titleKey: 'repairEvidence.title',
      descriptionKey: 'repairEvidence.summary'
    }
  },
  {
    path: '/audit-forensics',
    name: 'AuditForensics',
    component: AuditForensicsView,
    meta: {
      menu: true,
      titleKey: 'auditForensics.title',
      descriptionKey: 'auditForensics.summary'
    }
  },
  {
    path: '/audit-troubleshooting',
    name: 'AuditTroubleshooting',
    component: AuditTroubleshootingView,
    meta: {
      menu: true,
      titleKey: 'auditTroubleshooting.title',
      descriptionKey: 'auditTroubleshooting.summary'
    }
  },
  {
    path: '/benchmark',
    name: 'Benchmark',
    component: BenchmarkView,
    meta: {
      menu: true,
      titleKey: 'benchmark.title',
      descriptionKey: 'benchmark.summary'
    }
  },
  {
    path: '/acceleration',
    name: 'Acceleration',
    component: AccelerationView,
    meta: {
      menu: true,
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    }
  },
  {
    path: '/system',
    name: 'System',
    component: SystemView,
    meta: {
      menu: true,
      titleKey: 'system.title',
      descriptionKey: 'system.summary'
    }
  }
]

if (deliveryProgressEnabled) {
  constantRoutes.splice(2, 0, {
    path: '/delivery-progress',
    name: 'DeliveryProgress',
    component: () => import('../views/delivery/DeliveryProgressView.vue'),
    meta: {
      menu: true,
      temporary: true,
      envLimited: true,
      titleKey: 'deliveryProgress.title',
      descriptionKey: 'deliveryProgress.summary'
    }
  })
}

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

export default router
