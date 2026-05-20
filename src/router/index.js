import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { deliveryProgressEnabled } from '../config/runtimeFlags'
import {
  APP_ROUTE_DEFINITIONS,
  DELIVERY_PROGRESS_ROUTE_DEFINITION,
  LEGACY_ROUTE_DEFINITIONS,
  ROOT_ROUTE_DEFINITION
} from '../config/routePaths.mjs'

// 静态路由契约标记：name: 'RoutingGovernance', name: 'RecommendationCenter', name: 'AccessCenter'。

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
const ParseStatisticsCenterView = () => import('../views/parse-statistics/ParseStatisticsCenterView.vue')
const BenchmarkView = () => import('../views/benchmark/BenchmarkView.vue')
const RoutingGovernanceView = () => import('../views/routing-governance/RoutingGovernanceView.vue')
const RecommendationCenterView = () => import('../views/recommendation-center/RecommendationCenterView.vue')
const AccessCenterView = () => import('../views/access-center/AccessCenterView.vue')
const AlertCenterView = () => import('../views/alert-center/AlertCenterView.vue')
const SystemView = () => import('../views/system/SystemView.vue')

const routeComponents = {
  DashboardView,
  SqlQueryView,
  SqlHistoryView,
  ParseRecordView,
  ParseBatchCenterView,
  AssetCatalogView,
  RepairEvidenceView,
  AuditForensicsView,
  AuditTroubleshootingView,
  RuntimeGatesView,
  RecoveryDrillView,
  AccelerationView,
  ParseStatisticsCenterView,
  BenchmarkView,
  RoutingGovernanceView,
  RecommendationCenterView,
  AccessCenterView,
  AlertCenterView,
  SystemView
}

const routeFromDefinition = (definition, additionalComponents = {}) => {
  const { componentKey, ...route } = definition
  if (!componentKey) {
    return { ...route }
  }

  const component = routeComponents[componentKey] || additionalComponents[componentKey]
  if (!component) {
    throw new Error(`Unknown route component key: ${componentKey}`)
  }
  return {
    ...route,
    component
  }
}

export const constantRoutes = [
  routeFromDefinition(ROOT_ROUTE_DEFINITION),
  ...APP_ROUTE_DEFINITIONS.map(routeFromDefinition),
  ...LEGACY_ROUTE_DEFINITIONS.map(routeFromDefinition)
]

if (deliveryProgressEnabled) {
  constantRoutes.push(
    routeFromDefinition(DELIVERY_PROGRESS_ROUTE_DEFINITION, {
      DeliveryProgressView: () => import('../views/delivery/DeliveryProgressView.vue')
    })
  )
}

const historyFactory =
  import.meta.env.MODE === 'portable' ? createWebHashHistory : createWebHistory

const router = createRouter({
  history: historyFactory(import.meta.env.BASE_URL),
  routes: constantRoutes
})

export default router
