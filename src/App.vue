<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  buildNavigationBreadcrumb,
  buildNavigationKey,
  createNavigationTree,
  findActiveNavigationItem
} from './config/routePaths.mjs'
import { deliveryProgressEnabled, referencePagesEnabled } from './config/runtimeFlags'
import {
  formatRuntimeError,
  getGovernanceTenantConfigForContext,
  getGovernanceTenantConfigOptions,
  updateGovernanceTenantEngines
} from './services/runtimeGateApi'
import { useGlobalConfigStore, useTenantStore, useUserStore } from './stores'
import { engineOptions, uniqueOptions, withCurrentOption } from './views/common/formComponentGovernance'

const route = useRoute()
const { t, locale } = useI18n()
const globalConfigStore = useGlobalConfigStore()
const tenantStore = useTenantStore()
const userStore = useUserStore()
const workspaceForm = reactive({
  contextTenantId: tenantStore.tenantId,
  tenantId: tenantStore.tenantId,
  defaultEngine: tenantStore.defaultEngine,
  backupEngine: tenantStore.backupEngine
})
const workspaceLoading = reactive({
  options: false,
  config: false,
  save: false
})

const navLabel = key => t(key)
const itemLabel = item => t(item.menuLabel || item.titleKey)
const itemBadgeLabel = item => {
  if (!item?.badge) {
    return ''
  }
  return t(item.badge)
}

const navigationTree = computed(() =>
  createNavigationTree({
    includeDeliveryProgress: deliveryProgressEnabled,
    includeReferencePages: referencePagesEnabled
  })
)
const activeNavItem = computed(() => findActiveNavigationItem(navigationTree.value, route))
const activeMenuKey = computed(() => activeNavItem.value?.menuKey || buildNavigationKey(route.path, route.query))
const defaultOpeneds = computed(() => activeNavItem.value?.defaultOpeneds || [])
const localeLabel = computed(() =>
  locale.value === 'zh-CN' ? t('common.localeToggleToEnglish') : t('common.localeToggleToChinese')
)
const tenantDisplayName = computed(() =>
  tenantStore.tenantId === 'system' && !tenantStore.tenantName
    ? t('common.defaultTenantName')
    : tenantStore.tenantName || tenantStore.tenantId
)
const userDisplayName = computed(() =>
  userStore.userId === 'admin' && !userStore.displayName
    ? t('common.defaultUserName')
    : userStore.displayName
)

const userBadge = computed(() => `${userDisplayName.value} · ${userStore.role}`)
const breadcrumbText = computed(() => buildNavigationBreadcrumb(activeNavItem.value, navLabel, itemLabel))
const pageTitle = computed(() => t(activeNavItem.value?.titleKey || route.meta.titleKey || 'dashboard.title'))
const workspaceTenantOptions = computed(() => {
  const options = tenantStore.tenantOptions.map(option => ({
    label: option.label || option.tenantId || option.value,
    value: option.value || option.tenantId
  }))
  if (!options.some(option => option.value === workspaceForm.tenantId)) {
    options.unshift({
      label: tenantStore.tenantName || workspaceForm.tenantId,
      value: workspaceForm.tenantId
    })
  }
  return uniqueOptions(options)
})
const workspaceDefaultEngineOptions = computed(() => withCurrentOption(engineOptions, workspaceForm.defaultEngine))
const workspaceBackupEngineOptions = computed(() => withCurrentOption(engineOptions, workspaceForm.backupEngine))
const workspaceBusy = computed(() => workspaceLoading.options || workspaceLoading.config || workspaceLoading.save)
const workspaceDirty = computed(() =>
  workspaceForm.tenantId !== tenantStore.tenantId
  || workspaceForm.defaultEngine !== tenantStore.defaultEngine
  || workspaceForm.backupEngine !== tenantStore.backupEngine
)

const handleLocaleToggle = () => {
  const nextLocale = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = nextLocale
  globalConfigStore.setLocale(nextLocale)
}

const applyTenantConfig = config => {
  if (!config?.tenantId) {
    return
  }
  workspaceForm.tenantId = config.tenantId
  workspaceForm.defaultEngine = config.defaultEngine || workspaceForm.defaultEngine
  workspaceForm.backupEngine = config.backupEngine || workspaceForm.backupEngine
  tenantStore.setTenant({
    tenantId: config.tenantId,
    tenantName: config.label || config.tenantName || config.tenantId,
    defaultEngine: workspaceForm.defaultEngine,
    backupEngine: workspaceForm.backupEngine
  })
}

const syncWorkspaceFormFromStore = () => {
  workspaceForm.contextTenantId = tenantStore.tenantId
  workspaceForm.tenantId = tenantStore.tenantId
  workspaceForm.defaultEngine = tenantStore.defaultEngine
  workspaceForm.backupEngine = tenantStore.backupEngine
}

const loadTenantOptions = async () => {
  workspaceLoading.options = true
  try {
    const options = await getGovernanceTenantConfigOptions(workspaceForm.contextTenantId || tenantStore.tenantId, {
      requestPrefix: 'frontend-app-tenant-options'
    })
    tenantStore.setTenantOptions(Array.isArray(options) ? options : [])
  } catch (error) {
    ElMessage.warning(`${t('common.workspaceLoadFailed')}: ${formatRuntimeError(error)}`)
  } finally {
    workspaceLoading.options = false
  }
}

const loadTenantConfig = async (targetTenantId, contextTenantId = tenantStore.tenantId) => {
  workspaceLoading.config = true
  try {
    const config = await getGovernanceTenantConfigForContext(contextTenantId, targetTenantId, {
      requestPrefix: 'frontend-app-tenant-config'
    })
    applyTenantConfig(config)
  } catch (error) {
    const option = tenantStore.tenantOptions.find(item => item.value === targetTenantId || item.tenantId === targetTenantId)
    if (option) {
      applyTenantConfig(option)
    }
    ElMessage.warning(`${t('common.workspaceLoadFailed')}: ${formatRuntimeError(error)}`)
  } finally {
    workspaceLoading.config = false
  }
}

const handleTenantChange = tenantId => {
  const contextTenantId = workspaceForm.contextTenantId || tenantStore.tenantId
  const option = tenantStore.tenantOptions.find(item => item.value === tenantId || item.tenantId === tenantId)
  if (option) {
    applyTenantConfig(option)
  } else {
    workspaceForm.tenantId = tenantId
  }
  loadTenantConfig(tenantId, contextTenantId)
}

const handleWorkspaceSave = async () => {
  workspaceLoading.save = true
  try {
    const config = await updateGovernanceTenantEngines(
      {
        tenantId: workspaceForm.tenantId,
        defaultEngine: workspaceForm.defaultEngine,
        backupEngine: workspaceForm.backupEngine
      },
      {
        contextTenantId: workspaceForm.contextTenantId || workspaceForm.tenantId,
        requestPrefix: 'frontend-app-tenant-config-save'
      }
    )
    applyTenantConfig(config)
    await loadTenantOptions()
    ElMessage.success(t('common.workspaceSaved'))
  } catch (error) {
    ElMessage.error(`${t('common.workspaceSaveFailed')}: ${formatRuntimeError(error)}`)
  } finally {
    workspaceLoading.save = false
  }
}

onMounted(() => {
  globalConfigStore.setTheme('dark')
  globalConfigStore.applyTheme()
  locale.value = globalConfigStore.locale
  syncWorkspaceFormFromStore()
  loadTenantOptions()
  loadTenantConfig(tenantStore.tenantId, tenantStore.tenantId)
})
</script>

<template>
  <el-config-provider>
    <el-container class="app-shell">
      <el-aside class="app-sidebar" width="308px">
        <div class="brand-panel">
          <p class="brand-kicker sqlforge-code-label">{{ t('common.platformTagline') }}</p>
          <p class="brand-title">{{ t('common.appName') }}</p>
          <p class="brand-summary">{{ t('common.brandSummary') }}</p>
          <div class="brand-meta">
            <span class="brand-pill">{{ tenantDisplayName }}</span>
            <span class="brand-pill brand-pill-muted">{{ userBadge }}</span>
          </div>
        </div>

        <div class="sidebar-section">
          <p class="sidebar-section-label sqlforge-code-label">{{ t('common.adaptiveNavigation') }}</p>
          <el-scrollbar class="menu-scroll">
            <el-menu
              :default-active="activeMenuKey"
              :default-openeds="defaultOpeneds"
              class="app-menu"
              router
            >
              <template v-for="module in navigationTree" :key="module.key">
                <el-menu-item
                  v-if="module.directItem"
                  :index="module.directItem.menuKey"
                  class="menu-module-item"
                >
                  <div class="menu-item-content">
                    <span class="menu-module-title">
                      {{ navLabel(module.label) }}
                      <span v-if="itemBadgeLabel(module.directItem)" class="menu-item-badge">{{ itemBadgeLabel(module.directItem) }}</span>
                    </span>
                    <span class="menu-item-caption">{{ itemLabel(module.directItem) }}</span>
                  </div>
                </el-menu-item>

                <el-sub-menu
                  v-else
                  :index="module.key"
                  class="menu-module"
                >
                  <template #title>
                    <span class="menu-module-title">{{ navLabel(module.label) }}</span>
                  </template>
                  <template v-if="Array.isArray(module.items)">
                    <el-menu-item
                      v-for="item in module.items"
                      :key="item.menuKey"
                      :index="item.menuKey"
                      class="menu-leaf"
                    >
                      <span class="menu-item-label">
                        {{ itemLabel(item) }}
                        <span v-if="itemBadgeLabel(item)" class="menu-item-badge">{{ itemBadgeLabel(item) }}</span>
                      </span>
                    </el-menu-item>
                  </template>
                  <template v-else>
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
                        :key="item.menuKey"
                        :index="item.menuKey"
                        class="menu-leaf"
                      >
                        <span class="menu-item-label">
                          {{ itemLabel(item) }}
                          <span v-if="itemBadgeLabel(item)" class="menu-item-badge">{{ itemBadgeLabel(item) }}</span>
                        </span>
                      </el-menu-item>
                    </el-sub-menu>
                  </template>
                </el-sub-menu>
              </template>
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
            <p class="page-kicker sqlforge-code-label">{{ t('common.currentWorkspace') }}</p>
            <h1 class="page-title">{{ pageTitle }}</h1>
            <div class="breadcrumb-strip">
              <span
                v-for="pill in breadcrumbText.filter(p => p !== pageTitle)"
                :key="pill"
                class="page-status-pill"
              >
                {{ pill }}
              </span>
            </div>
          </div>

          <div class="header-actions">
            <div class="workspace-card workspace-editor" data-testid="tenant-engine-switcher">
              <label class="workspace-field workspace-field-tenant">
                <span class="workspace-field-label sqlforge-code-label">{{ t('common.currentTenant') }}</span>
                <el-select
                  v-model="workspaceForm.tenantId"
                  filterable
                  :loading="workspaceLoading.options || workspaceLoading.config"
                  :placeholder="t('common.workspaceTenantPlaceholder')"
                  data-testid="workspace-tenant-select"
                  @change="handleTenantChange"
                >
                  <el-option
                    v-for="item in workspaceTenantOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </label>
              <label class="workspace-field">
                <span class="workspace-field-label sqlforge-code-label">{{ t('common.defaultEngine') }}</span>
                <el-select
                  v-model="workspaceForm.defaultEngine"
                  :disabled="workspaceBusy"
                  data-testid="workspace-default-engine-select"
                >
                  <el-option
                    v-for="item in workspaceDefaultEngineOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </label>
              <label class="workspace-field">
                <span class="workspace-field-label sqlforge-code-label">{{ t('common.backupEngine') }}</span>
                <el-select
                  v-model="workspaceForm.backupEngine"
                  :disabled="workspaceBusy"
                  data-testid="workspace-backup-engine-select"
                >
                  <el-option
                    v-for="item in workspaceBackupEngineOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </label>
              <el-button
                type="primary"
                class="workspace-save"
                :loading="workspaceLoading.save"
                :disabled="!workspaceDirty || workspaceLoading.config || workspaceLoading.options"
                data-testid="workspace-save"
                @click="handleWorkspaceSave"
              >
                {{ t('common.workspaceSave') }}
              </el-button>
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
  overflow-wrap: anywhere;
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

:deep(.app-menu .el-menu) {
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

.menu-item-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-module-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
}

.menu-section-title {
  font-size: 13px;
}

.menu-item-label,
.menu-item-caption {
  font-size: 13px;
}

.menu-item-caption {
  color: var(--sqlforge-text-muted);
}

.menu-item-badge {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(244, 114, 182, 0.14);
  color: #f9a8d4;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
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
  font-size: 34px;
  font-weight: 400;
  line-height: 1.04;
}

.breadcrumb-strip {
  margin-top: 12px;
}

.workspace-summary {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.header-actions {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.workspace-card {
  min-width: 640px;
}

.workspace-editor {
  display: grid;
  grid-template-columns: minmax(170px, 1.2fr) minmax(118px, 0.8fr) minmax(118px, 0.8fr) auto;
  gap: 10px;
  align-items: end;
}

.workspace-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.workspace-field-label {
  color: var(--sqlforge-text-muted);
  font-size: 11px;
}

:deep(.workspace-field .el-select) {
  width: 100%;
}

.workspace-save {
  min-height: 32px;
  padding: 0 14px;
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

@media (max-width: 1200px) {
  .app-header {
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .workspace-card {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 760px) {
  .app-shell {
    flex-direction: column;
  }

  .app-sidebar {
    width: 100% !important;
    flex: 0 0 auto;
    gap: 12px;
    padding: 12px;
    border-right: none;
    border-bottom: 1px solid var(--sqlforge-border-default);
  }

  .brand-panel {
    padding: 14px;
  }

  .brand-title {
    margin-top: 6px;
    font-size: 24px;
  }

  .brand-summary {
    margin-top: 8px;
    font-size: 14px;
    line-height: 1.45;
  }

  .sidebar-section {
    display: none;
  }

  .sidebar-runtime {
    display: none;
  }

  .app-main {
    width: 100%;
  }

  .app-header {
    gap: 16px;
    padding: 16px 14px 0;
  }

  .page-title {
    margin-top: 6px;
    font-size: 26px;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
    align-items: stretch;
  }

  .workspace-editor {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 12px;
  }

  .workspace-field-tenant,
  .workspace-save {
    grid-column: 1 / -1;
  }

  .workspace-save {
    width: 100%;
  }

  .page-container {
    padding: 16px 14px 28px;
  }
}
</style>
