import { createRouter, createWebHistory } from 'vue-router'
import { defineComponent, h } from 'vue'

const createPlaceholderView = (title, description) =>
  defineComponent({
    name: `${title.replace(/\s+/g, '')}Page`,
    setup() {
      return () =>
        h('section', { class: 'route-page' }, [
          h('div', { class: 'route-card' }, [
            h('p', { class: 'route-eyebrow' }, 'SQLForge'),
            h('h1', { class: 'route-title' }, title),
            h('p', { class: 'route-description' }, description)
          ])
        ])
    }
  })

export const constantRoutes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: createPlaceholderView('Dashboard', 'Overview of SQL lifecycle governance posture.'),
    meta: {
      menu: true,
      titleKey: 'dashboard.title'
    }
  },
  {
    path: '/sql-query',
    name: 'SqlQuery',
    component: createPlaceholderView('SQL Query', 'Unified query submission and execution routing workspace.'),
    meta: {
      menu: true,
      titleKey: 'sqlQuery.title'
    }
  },
  {
    path: '/parse-record',
    name: 'ParseRecord',
    component: createPlaceholderView('Parse Record', 'Track parser output, rewrite status and historical diagnostics.'),
    meta: {
      menu: true,
      titleKey: 'parseRecord.title'
    }
  },
  {
    path: '/benchmark',
    name: 'Benchmark',
    component: createPlaceholderView('Benchmark Report', 'Inspect baseline, P99 latency and engine comparison metrics.'),
    meta: {
      menu: true,
      titleKey: 'benchmark.title'
    }
  },
  {
    path: '/acceleration',
    name: 'Acceleration',
    component: createPlaceholderView('Acceleration', 'Review acceleration policy, materialized view and partition guidance.'),
    meta: {
      menu: true,
      titleKey: 'acceleration.title'
    }
  },
  {
    path: '/system',
    name: 'System',
    component: createPlaceholderView('System Management', 'Manage tenants, routing defaults, audit retention and global settings.'),
    meta: {
      menu: true,
      titleKey: 'system.title'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes
})

export default router
