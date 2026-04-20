import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/dashboard/DashboardView.vue'
import RoutePlaceholder from '../views/common/RoutePlaceholder.vue'
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
    component: RoutePlaceholder,
    props: {
      eyebrowKey: 'common.platformTagline',
      titleKey: 'sqlQuery.title',
      descriptionKey: 'sqlQuery.summary'
    },
    meta: {
      menu: true,
      titleKey: 'sqlQuery.title',
      descriptionKey: 'sqlQuery.summary'
    }
  },
  {
    path: '/parse-record',
    name: 'ParseRecord',
    component: RoutePlaceholder,
    props: {
      eyebrowKey: 'common.platformTagline',
      titleKey: 'parseRecord.title',
      descriptionKey: 'parseRecord.summary'
    },
    meta: {
      menu: true,
      titleKey: 'parseRecord.title',
      descriptionKey: 'parseRecord.summary'
    }
  },
  {
    path: '/benchmark',
    name: 'Benchmark',
    component: RoutePlaceholder,
    props: {
      eyebrowKey: 'common.platformTagline',
      titleKey: 'benchmark.title',
      descriptionKey: 'benchmark.summary'
    },
    meta: {
      menu: true,
      titleKey: 'benchmark.title',
      descriptionKey: 'benchmark.summary'
    }
  },
  {
    path: '/acceleration',
    name: 'Acceleration',
    component: RoutePlaceholder,
    props: {
      eyebrowKey: 'common.platformTagline',
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    },
    meta: {
      menu: true,
      titleKey: 'acceleration.title',
      descriptionKey: 'acceleration.summary'
    }
  },
  {
    path: '/system',
    name: 'System',
    component: RoutePlaceholder,
    props: {
      eyebrowKey: 'common.platformTagline',
      titleKey: 'system.title',
      descriptionKey: 'system.summary'
    },
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
