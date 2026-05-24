import { createPinia, defineStore } from 'pinia'

export const pinia = createPinia()

export const useTenantStore = defineStore('tenantStore', {
  state: () => ({
    tenantId: 'tenant-a',
    tenantName: 'tenant-a',
    defaultEngine: 'HETU',
    backupEngine: 'HIVE',
    tenantOptions: []
  }),
  actions: {
    setTenant(payload = {}) {
      if (payload.tenantId) {
        this.tenantId = payload.tenantId
      }
      if (Object.prototype.hasOwnProperty.call(payload, 'tenantName')) {
        this.tenantName = payload.tenantName
      } else if (payload.label) {
        this.tenantName = payload.label
      } else if (payload.tenantId) {
        this.tenantName = payload.tenantId
      }
      if (payload.defaultEngine) {
        this.defaultEngine = payload.defaultEngine
      }
      if (payload.backupEngine) {
        this.backupEngine = payload.backupEngine
      }
    },
    setTenantEngines(payload = {}) {
      if (payload.defaultEngine) {
        this.defaultEngine = payload.defaultEngine
      }
      if (payload.backupEngine) {
        this.backupEngine = payload.backupEngine
      }
    },
    setTenantOptions(options = []) {
      this.tenantOptions = options
        .map(option => ({
          label: option.label || option.tenantName || option.tenantId || option.value,
          value: option.value || option.tenantId,
          tenantId: option.tenantId || option.value,
          defaultEngine: option.defaultEngine,
          backupEngine: option.backupEngine
        }))
        .filter(option => option.value)
    }
  }
})

export const useUserStore = defineStore('userStore', {
  state: () => ({
    userId: 'admin',
    displayName: '',
    role: 'ADMIN'
  }),
  actions: {
    setUser(payload) {
      this.userId = payload.userId
      this.displayName = payload.displayName
      this.role = payload.role
    }
  }
})

export const useGlobalConfigStore = defineStore('globalConfigStore', {
  state: () => ({
    locale: 'zh-CN',
    theme: 'dark',
    compactMode: false
  }),
  actions: {
    setLocale(locale) {
      this.locale = locale
    },
    setTheme(theme) {
      this.theme = theme
      this.applyTheme()
    },
    toggleTheme() {
      this.theme = this.theme === 'light' ? 'dark' : 'light'
      this.applyTheme()
    },
    applyTheme() {
      if (typeof document === 'undefined') {
        return
      }
      document.documentElement.classList.toggle('dark', this.theme === 'dark')
    }
  }
})
