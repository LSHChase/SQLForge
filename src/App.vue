<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ROUTE_PATHS } from './config/routePaths.mjs'
import { useGlobalConfigStore, useTenantStore, useUserStore } from './stores'

const route = useRoute()
const { t, locale } = useI18n()
const globalConfigStore = useGlobalConfigStore()
const tenantStore = useTenantStore()
const userStore = useUserStore()

const navLabel = value => (locale.value === 'zh-CN' ? value.zh : value.en)

const navigationTree = computed(() => [
  {
    key: 'dashboard',
    label: { zh: 'Dashboard', en: 'Dashboard' },
    sections: [
      {
        key: 'overview',
        label: { zh: '总览驾驶舱', en: 'Overview cockpit' },
        items: [{ path: ROUTE_PATHS.dashboard, titleKey: 'dashboard.title' }]
      }
    ]
  },
  {
    key: 'sql-query',
    label: { zh: 'SQL 查询', en: 'SQL Query' },
    sections: [
      {
        key: 'workbench',
        label: { zh: '查询工作台', en: 'Query workbench' },
        items: [{ path: ROUTE_PATHS.sqlQuery, titleKey: 'sqlQuery.title' }]
      }
    ]
  },
  {
    key: 'sql-history',
    label: { zh: 'SQL 历史', en: 'SQL History' },
    sections: [
      {
        key: 'history',
        label: { zh: '历史列表', en: 'History list' },
        items: [{ path: ROUTE_PATHS.parseRecord, titleKey: 'parseRecord.title' }]
      },
      {
        key: 'forensics',
        label: { zh: '取证与修复', en: 'Forensics and repair' },
        items: [
          { path: ROUTE_PATHS.repairEvidence, titleKey: 'repairEvidence.title' },
          { path: ROUTE_PATHS.auditForensics, titleKey: 'auditForensics.title' }
        ]
      }
    ]
  },
  {
    key: 'parse-acceleration',
    label: { zh: '解析与加速', en: 'Parsing and Acceleration' },
    sections: [
      {
        key: 'parse',
        label: { zh: '解析工作流', en: 'Parsing workflow' },
        items: [
          { path: ROUTE_PATHS.acceleration, titleKey: 'acceleration.title' },
          { path: ROUTE_PATHS.parseBatchCenter, titleKey: 'parseBatchCenter.title' },
          { path: ROUTE_PATHS.parseStatisticsCenter, titleKey: 'parseStatisticsCenter.title' }
        ]
      },
      {
        key: 'rewrite',
        label: { zh: '改写与推荐', en: 'Rewrite and Recommendation' },
        items: [{ path: ROUTE_PATHS.recommendationCenter, titleKey: 'recommendationCenter.title' }]
      }
    ]
  },
  {
    key: 'routing',
    label: { zh: '路由治理', en: 'Routing Governance' },
    sections: [
      {
        key: 'routing-policy',
        label: { zh: '当前策略与历史', en: 'Policy and history' },
        items: [{ path: ROUTE_PATHS.routingGovernance, titleKey: 'routingGovernance.title' }]
      }
    ]
  },
  {
    key: 'assets',
    label: { zh: '数据资产', en: 'Data Assets' },
    sections: [
      {
        key: 'catalog',
        label: { zh: '资产目录', en: 'Asset catalog' },
        items: [{ path: ROUTE_PATHS.assetCatalog, titleKey: 'assetCatalog.title' }]
      }
    ]
  },
  {
    key: 'benchmark',
    label: { zh: '压测中心', en: 'Benchmark Center' },
    sections: [
      {
        key: 'benchmark-workspace',
        label: { zh: '任务与报告', en: 'Tasks and reports' },
        items: [{ path: ROUTE_PATHS.benchmark, titleKey: 'benchmark.title' }]
      }
    ]
  },
  {
    key: 'system',
    label: { zh: '系统管理', en: 'System Management' },
    sections: [
      {
        key: 'config',
        label: { zh: '配置与接入', en: 'Config and integration' },
        items: [{ path: ROUTE_PATHS.system, titleKey: 'system.title' }]
      },
      {
        key: 'alerts',
        label: { zh: '告警与处置', en: 'Alerts and Remediation' },
        items: [
          { path: ROUTE_PATHS.alertCenter, titleKey: 'alertCenter.title' },
          { path: ROUTE_PATHS.auditTroubleshooting, titleKey: 'auditTroubleshooting.title' }
        ]
      },
      {
        key: 'runtime',
        label: { zh: '运行治理', en: 'Runtime Governance' },
        items: [
          { path: ROUTE_PATHS.runtimeGates, titleKey: 'runtimeGates.title' },
          { path: ROUTE_PATHS.recoveryDrill, titleKey: 'recoveryDrill.title' }
        ]
      }
    ]
  },
  {
    key: 'access',
    label: { zh: '开放接入', en: 'Open Access' },
    sections: [
      {
        key: 'access-overview',
        label: { zh: '接入总览与策略', en: 'Access overview and policy' },
        items: [{ path: ROUTE_PATHS.accessCenter, titleKey: 'accessCenter.title' }]
      }
    ]
  }
])

const flattenNavItems = tree =>
  tree.flatMap(module =>
    module.sections.flatMap(section =>
      section.items.map(item => ({
        ...item,
        moduleKey: module.key,
        moduleLabel: module.label,
        sectionKey: section.key,
        sectionLabel: section.label
      }))
    )
  )

const activeNavItem = computed(() =>
  flattenNavItems(navigationTree.value).find(item => item.path === route.path) || null
)
const defaultOpeneds = computed(() => {
  if (!activeNavItem.value) {
    return []
  }
  return [activeNavItem.value.moduleKey, `${activeNavItem.value.moduleKey}:${activeNavItem.value.sectionKey}`]
})
const localeLabel = computed(() => (locale.value === 'zh-CN' ? 'EN' : '中'))
const workspaceSummary = computed(() =>
  t('common.workspaceSummary', {
    tenant: tenantStore.tenantName,
    engine: tenantStore.defaultEngine
  })
)
const userBadge = computed(() => `${userStore.displayName} · ${userStore.role}`)
const pageDescription = computed(() =>
  route.meta?.descriptionKey ? t(route.meta.descriptionKey) : t('dashboard.summary')
)
const breadcrumbText = computed(() => {
  if (!activeNavItem.value) {
    return []
  }
  return [navLabel(activeNavItem.value.moduleLabel), navLabel(activeNavItem.value.sectionLabel), t(activeNavItem.value.titleKey)]
})

const handleLocaleToggle = () => {
  const nextLocale = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = nextLocale
  globalConfigStore.setLocale(nextLocale)
}

onMounted(() => {
  globalConfigStore.setTheme('dark')
  globalConfigStore.applyTheme()
  locale.value = globalConfigStore.locale
})
</script>

<template>
  <el-config-provider>
    <el-container class="app-shell">
      <el-aside class="app-sidebar" width="308px">
        <div class="brand-panel">
          <p class="brand-kicker sqlforge-code-label">{{ t('common.platformTagline') }}</p>
          <h1 class="brand-title">{{ t('common.appName') }}</h1>
          <p class="brand-summary">{{ t('common.brandSummary') }}</p>
          <div class="brand-meta">
            <span class="brand-pill">{{ tenantStore.tenantName }}</span>
            <span class="brand-pill brand-pill-muted">{{ userBadge }}</span>
          </div>
        </div>

        <div class="sidebar-section">
          <p class="sidebar-section-label sqlforge-code-label">{{ locale === 'zh-CN' ? '三级导航' : 'Three-level navigation' }}</p>
          <el-scrollbar class="menu-scroll">
            <el-menu
              :default-active="route.path"
              :default-openeds="defaultOpeneds"
              class="app-menu"
              router
            >
              <el-sub-menu
                v-for="module in navigationTree"
                :key="module.key"
                :index="module.key"
                class="menu-module"
              >
                <template #title>
                  <span class="menu-module-title">{{ navLabel(module.label) }}</span>
                </template>
                <el-sub-menu
                  v-for="section in module.sections"
                  :key="`${module.key}:${section.key}`"
                  :index="`${module.key}:${section.key}`"
                  class="menu-section"
                >
                  <template #title>
                    <span class="menu-section-title">{{ navLabel(section.label) }}</span>
                  </template>
                  <el-menu-item
                    v-for="item in section.items"
                    :key="item.path"
                    :index="item.path"
                    class="menu-leaf"
                  >
                    <span class="menu-item-label">{{ t(item.titleKey) }}</span>
                  </el-menu-item>
                </el-sub-menu>
              </el-sub-menu>
            </el-menu>
          </el-scrollbar>
        </div>

        <div class="sidebar-runtime">
          <p class="sidebar-section-label sqlforge-code-label">{{ t('common.runtimeLabel') }}</p>
          <div class="runtime-card">
            <div class="runtime-row">
              <span>{{ t('common.defaultEngine') }}</span>
              <strong>{{ tenantStore.defaultEngine }}</strong>
            </div>
            <div class="runtime-row">
              <span>{{ t('common.backupEngine') }}</span>
              <strong>{{ tenantStore.backupEngine }}</strong>
            </div>
            <div class="runtime-row">
              <span>{{ t('common.currentTenant') }}</span>
              <strong>{{ tenantStore.tenantId }}</strong>
            </div>
          </div>
        </div>
      </el-aside>

      <el-container class="app-main">
        <el-header class="app-header">
          <div class="page-heading">
            <p class="page-kicker sqlforge-code-label">{{ locale === 'zh-CN' ? '当前工作区' : 'Current workspace' }}</p>
            <h2 class="page-title">{{ t(route.meta.titleKey || 'dashboard.title') }}</h2>
            <div class="breadcrumb-strip">
              <span
                v-for="pill in breadcrumbText"
                :key="pill"
                class="page-status-pill"
              >
                {{ pill }}
              </span>
            </div>
            <p class="page-summary">{{ pageDescription }}</p>
          </div>

          <div class="header-actions">
            <div class="workspace-card">
              <p class="workspace-label sqlforge-code-label">{{ t('common.workspaceLabel') }}</p>
              <p class="workspace-summary">{{ workspaceSummary }}</p>
            </div>
            <el-button text class="header-action" @click="handleLocaleToggle">
              {{ localeLabel }}
            </el-button>
          </div>
        </el-header>

        <el-main class="page-container">
          <router-view v-slot="{ Component }">
            <transition name="fade-slide" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top right, rgba(62, 207, 142, 0.08), transparent 22%),
    radial-gradient(circle at top left, rgba(67, 67, 67, 0.24), transparent 26%),
    linear-gradient(180deg, var(--sqlforge-bg-page) 0%, var(--sqlforge-bg-page-deep) 100%);
  color: var(--sqlforge-text-primary);
}

.app-sidebar {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-6);
  padding: 24px 18px 20px;
  border-right: 1px solid var(--sqlforge-border-default);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 26%),
    var(--sqlforge-bg-page-deep);
}

.brand-panel,
.runtime-card,
.workspace-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 38%),
    var(--sqlforge-surface-3);
}

.brand-panel {
  padding: 20px;
}

.brand-kicker,
.page-kicker,
.sidebar-section-label,
.workspace-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.brand-title {
  margin: 10px 0 0;
  font-size: 32px;
  font-weight: 400;
  line-height: 1.05;
}

.brand-summary {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.brand-meta,
.breadcrumb-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.brand-meta {
  margin-top: 18px;
}

.brand-pill,
.page-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--sqlforge-radius-pill);
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.brand-pill-muted {
  border-color: var(--sqlforge-border-strong);
  background: transparent;
}

.sidebar-section {
  flex: 1;
  min-height: 0;
}

.menu-scroll {
  margin-top: 12px;
}

.app-menu {
  border-right: none;
  background: transparent;
}

:deep(.app-menu .el-sub-menu__title),
:deep(.app-menu .el-menu-item) {
  border-radius: var(--sqlforge-radius-sm);
  color: var(--sqlforge-text-secondary);
}

:deep(.app-menu .el-sub-menu__title) {
  height: auto;
  padding: 12px 14px;
  border: 1px solid transparent;
}

:deep(.app-menu .el-sub-menu .el-sub-menu__title:hover),
:deep(.app-menu .el-menu-item:hover) {
  background: var(--sqlforge-surface-2);
  border-color: var(--sqlforge-border-default);
  color: var(--sqlforge-text-primary);
}

:deep(.app-menu .el-menu-item) {
  height: auto;
  margin: 6px 0;
  padding: 10px 14px;
  border: 1px solid transparent;
  line-height: 1.35;
}

:deep(.app-menu .el-menu-item.is-active) {
  background: rgba(62, 207, 142, 0.08);
  border-color: var(--sqlforge-color-brand-border);
  color: var(--sqlforge-text-primary);
}

.menu-module-title {
  font-size: 14px;
  font-weight: 500;
}

.menu-section-title {
  font-size: 13px;
}

.menu-item-label {
  font-size: 14px;
  font-weight: 500;
}

.sidebar-runtime {
  padding-top: 4px;
}

.runtime-card,
.workspace-card {
  padding: 16px;
}

.runtime-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  color: var(--sqlforge-text-secondary);
  font-size: 14px;
}

.runtime-row + .runtime-row {
  border-top: 1px solid var(--sqlforge-border-subtle);
}

.runtime-row strong,
.workspace-summary {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.app-main {
  min-width: 0;
}

.app-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 32px 0;
  height: auto;
}

.page-heading {
  max-width: 760px;
}

.page-title {
  margin: 10px 0 0;
  font-size: 40px;
  font-weight: 400;
  line-height: 1.04;
}

.breadcrumb-strip {
  margin-top: 12px;
}

.page-summary,
.workspace-summary {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace-card {
  min-width: 280px;
}

.header-action {
  min-height: 40px;
  padding: 0 18px;
  border-radius: var(--sqlforge-radius-pill);
  color: var(--sqlforge-text-primary);
}

.page-container {
  padding: 24px 32px 32px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.24s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
