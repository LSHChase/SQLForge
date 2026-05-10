import { createPinia, defineStore } from 'pinia'

export const pinia = createPinia()

export const useTenantStore = defineStore('tenantStore', {
  state: () => ({
    tenantId: 'system',
    tenantName: '',
    defaultEngine: 'HETU',
    backupEngine: 'HIVE'
  }),
  actions: {
    setTenant(payload) {
      this.tenantId = payload.tenantId
      this.tenantName = payload.tenantName
      this.defaultEngine = payload.defaultEngine
      this.backupEngine = payload.backupEngine
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
