<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { constantRoutes } from './router'
import { useGlobalConfigStore } from './stores'

const route = useRoute()
const { t, locale } = useI18n()
const globalConfigStore = useGlobalConfigStore()

const menuRoutes = computed(() => constantRoutes.filter(item => item.meta?.menu))
const localeLabel = computed(() => (locale.value === 'zh-CN' ? 'EN' : '中'))
const themeLabel = computed(() =>
  globalConfigStore.theme === 'dark' ? t('common.switchToLight') : t('common.switchToDark')
)

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
      <el-aside class="app-sidebar" width="252px">
        <div class="brand-panel">
          <p class="brand-kicker">{{ t('common.platformTagline') }}</p>
          <h1>{{ t('common.appName') }}</h1>
          <p class="brand-summary">{{ t('dashboard.summary') }}</p>
        </div>
        <el-scrollbar class="menu-scroll">
          <el-menu :default-active="route.path" class="app-menu" router>
            <el-menu-item
              v-for="item in menuRoutes"
              :key="item.path"
              :index="item.path"
            >
              <span>{{ t(item.meta.titleKey) }}</span>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <el-container class="app-main">
        <el-header class="app-header">
          <div>
            <p class="page-kicker">{{ t('common.currentWorkspace') }}</p>
            <h2 class="page-title">{{ t(route.meta.titleKey || 'dashboard.title') }}</h2>
          </div>
          <div class="header-actions">
            <el-button text class="header-action" @click="handleLocaleToggle">
              {{ localeLabel }}
            </el-button>
            <el-button class="header-action" @click="handleThemeToggle">
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
    radial-gradient(circle at top left, rgba(64, 158, 255, 0.22), transparent 34%),
    radial-gradient(circle at bottom right, rgba(24, 144, 255, 0.12), transparent 32%),
    linear-gradient(180deg, var(--sqlforge-bg-elevated) 0%, var(--sqlforge-bg) 100%);
  color: var(--sqlforge-text-primary);
}

.app-sidebar {
  display: flex;
  flex-direction: column;
  padding: 20px 18px;
  border-right: 1px solid var(--sqlforge-border);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(255, 255, 255, 0.5));
  backdrop-filter: blur(14px);
}

:global(html.dark) .app-sidebar {
  background: linear-gradient(180deg, rgba(14, 23, 42, 0.92), rgba(15, 23, 42, 0.82));
}

.brand-panel {
  margin-bottom: 18px;
  padding: 20px;
  border: 1px solid var(--sqlforge-border);
  border-radius: 22px;
  background: var(--sqlforge-panel-strong);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.brand-panel h1,
.page-title {
  margin: 8px 0 0;
}

.brand-kicker,
.page-kicker {
  margin: 0;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.brand-summary {
  margin: 12px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.menu-scroll {
  flex: 1;
}

.app-menu {
  border-right: none;
  background: transparent;
}

.app-main {
  min-width: 0;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28px 32px 0;
  height: auto;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.header-action {
  border-radius: 999px;
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

@media (max-width: 960px) {
  .app-shell {
    flex-direction: column;
  }

  .app-sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--sqlforge-border);
  }

  .app-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 24px 20px 0;
  }

  .page-container {
    padding: 20px;
  }
}
</style>
