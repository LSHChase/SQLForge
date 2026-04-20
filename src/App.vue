<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { constantRoutes } from './router'
import { useGlobalConfigStore, useTenantStore, useUserStore } from './stores'

const route = useRoute()
const { t, locale } = useI18n()
const globalConfigStore = useGlobalConfigStore()
const tenantStore = useTenantStore()
const userStore = useUserStore()

const menuRoutes = computed(() => constantRoutes.filter(item => item.meta?.menu))
const localeLabel = computed(() => (locale.value === 'zh-CN' ? 'EN' : '中'))
const themeLabel = computed(() =>
  globalConfigStore.theme === 'dark' ? t('common.switchToLight') : t('common.switchToDark')
)
const pageDescription = computed(() =>
  route.meta?.descriptionKey ? t(route.meta.descriptionKey) : t('dashboard.summary')
)
const workspaceSummary = computed(() =>
  t('common.workspaceSummary', {
    tenant: tenantStore.tenantName,
    engine: tenantStore.defaultEngine
  })
)
const userBadge = computed(() => `${userStore.displayName} · ${userStore.role}`)
const buildRoutePills = meta => {
  const pills = []

  if (meta?.temporary) {
    pills.push({
      key: 'temporary',
      label: t('common.temporaryPage'),
      className: 'status-pill-temporary'
    })
  }

  if (meta?.envLimited) {
    pills.push({
      key: 'envLimited',
      label: t('common.nonProductionOnly'),
      className: 'status-pill-muted'
    })
  }

  return pills
}
const activeRoutePills = computed(() => buildRoutePills(route.meta))

const handleThemeToggle = () => {
  globalConfigStore.toggleTheme()
}

const handleLocaleToggle = () => {
  const nextLocale = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = nextLocale
  globalConfigStore.setLocale(nextLocale)
}

onMounted(() => {
  globalConfigStore.applyTheme()
  locale.value = globalConfigStore.locale
})
</script>

<template>
  <el-config-provider>
    <el-container class="app-shell">
      <el-aside class="app-sidebar" width="280px">
        <div class="brand-panel">
          <p class="brand-kicker">{{ t('common.platformTagline') }}</p>
          <h1 class="brand-title">{{ t('common.appName') }}</h1>
          <p class="brand-summary">{{ t('common.brandSummary') }}</p>
          <div class="brand-meta">
            <span class="brand-pill">{{ tenantStore.tenantName }}</span>
            <span class="brand-pill brand-pill-muted">{{ userBadge }}</span>
          </div>
        </div>

        <div class="sidebar-section">
          <p class="sidebar-section-label sqlforge-code-label">{{ t('common.sidebarLabel') }}</p>
          <el-scrollbar class="menu-scroll">
            <el-menu :default-active="route.path" class="app-menu" router>
              <el-menu-item
                v-for="item in menuRoutes"
                :key="item.path"
                :index="item.path"
              >
                <div class="menu-item-content">
                  <span class="menu-item-label">{{ t(item.meta.titleKey) }}</span>
                  <div
                    v-if="buildRoutePills(item.meta).length"
                    class="menu-item-pills"
                  >
                    <span
                      v-for="pill in buildRoutePills(item.meta)"
                      :key="`${item.path}-${pill.key}`"
                      class="menu-item-pill"
                      :class="pill.className"
                    >
                      {{ pill.label }}
                    </span>
                  </div>
                </div>
              </el-menu-item>
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
            <p class="page-kicker">{{ t('common.currentWorkspace') }}</p>
            <div class="page-title-row">
              <h2 class="page-title">{{ t(route.meta.titleKey || 'dashboard.title') }}</h2>
              <div class="page-title-pills">
                <span class="page-status-pill">{{ t('common.desktopMode') }}</span>
                <span
                  v-for="pill in activeRoutePills"
                  :key="pill.key"
                  class="page-status-pill"
                  :class="pill.className"
                >
                  {{ pill.label }}
                </span>
              </div>
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
            <el-button class="header-action header-action-outline" @click="handleThemeToggle">
              {{ themeLabel }}
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
    radial-gradient(circle at top left, rgba(67, 67, 67, 0.32), transparent 26%),
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
.runtime-card {
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

.brand-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.brand-pill,
.page-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--sqlforge-radius-pill);
  border: 1px solid var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.08);
  color: var(--sqlforge-text-primary);
  font-size: 12px;
}

.brand-pill-muted {
  border-color: var(--sqlforge-border-strong);
  background: transparent;
  color: var(--sqlforge-text-secondary);
}

.status-pill-temporary {
  border-color: rgba(214, 179, 48, 0.28);
  background: rgba(214, 179, 48, 0.12);
}

.status-pill-muted {
  border-color: var(--sqlforge-border-default);
  background: transparent;
  color: var(--sqlforge-text-secondary);
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

:deep(.app-menu .el-menu-item) {
  height: auto;
  margin-bottom: 8px;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: var(--sqlforge-radius-sm);
  color: var(--sqlforge-text-secondary);
  line-height: 1.3;
}

:deep(.app-menu .el-menu-item:hover) {
  background: var(--sqlforge-surface-2);
  border-color: var(--sqlforge-border-default);
  color: var(--sqlforge-text-primary);
}

:deep(.app-menu .el-menu-item.is-active) {
  background: rgba(62, 207, 142, 0.08);
  border-color: var(--sqlforge-color-brand-border);
  color: var(--sqlforge-text-primary);
}

.menu-item-label {
  font-size: 14px;
  font-weight: 500;
}

.menu-item-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.menu-item-pills,
.page-title-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.menu-item-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-pill);
  color: var(--sqlforge-text-muted);
  font-size: 11px;
  line-height: 1.4;
}

.sidebar-runtime {
  padding-top: 4px;
}

.runtime-card {
  margin-top: 12px;
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

.runtime-row strong {
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

.page-title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.page-title {
  margin: 0;
  font-size: 36px;
  font-weight: 400;
  line-height: 1.1;
}

.page-summary {
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
  padding: 12px 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-3);
}

.workspace-summary {
  margin: 8px 0 0;
  color: var(--sqlforge-text-primary);
  font-size: 14px;
  line-height: 1.4;
}

.header-action {
  min-height: 40px;
  padding: 0 18px;
  border-radius: var(--sqlforge-radius-pill);
  color: var(--sqlforge-text-primary);
}

.header-action-outline {
  border-color: var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
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
