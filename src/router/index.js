import { createRouter, createWebHashHistory, createWebHistory } from 'vue-router'
import { deliveryProgressEnabled } from '../config/runtimeFlags'
import {
  APP_ROUTE_DEFINITIONS,
  DELIVERY_PROGRESS_ROUTE_DEFINITION,
  LEGACY_ROUTE_DEFINITIONS,
  ROOT_ROUTE_DEFINITION
} from '../config/routePaths.mjs'

// 静态路由契约标记：name: 'RecommendationCenter', name: 'AccessCenter'。

const DashboardView = () => import('../views/dashboard/DashboardView.vue')
const SqlQueryView = () => import('../views/query/SqlQueryView.vue')
const SqlHistoryView = () => import('../views/sql-history/SqlHistoryView.vue')
const ParseRecordView = () => import('../views/parse-record/ParseRecordView.vue')
const ParseBatchCenterView = () => import('../views/parse-batch/ParseBatchCenterView.vue')
const AssetCatalogView = () => import('../views/asset-catalog/AssetCatalogView.vue')
const AccelerationView = () => import('../views/optimization/AccelerationView.vue')
const ParseStatisticsCenterView = () => import('../views/parse-statistics/ParseStatisticsCenterView.vue')
const BenchmarkView = () => import('../views/benchmark/BenchmarkView.vue')
const RecommendationCenterView = () => import('../views/recommendation-center/RecommendationCenterView.vue')
const AccessCenterView = () => import('../views/access-center/AccessCenterView.vue')
const SystemView = () => import('../views/system/SystemView.vue')

const routeComponents = {
  DashboardView,
  SqlQueryView,
  SqlHistoryView,
  ParseRecordView,
  ParseBatchCenterView,
  AssetCatalogView,
  AccelerationView,
  ParseStatisticsCenterView,
  BenchmarkView,
  RecommendationCenterView,
  AccessCenterView,
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
