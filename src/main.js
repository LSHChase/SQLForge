import { createApp } from 'vue'
import { createI18n } from 'vue-i18n'
import App from './App.vue'
import router from './router'
import { pinia, useGlobalConfigStore } from './stores'
import zhCN from './locales/zh-CN'
import enUS from './locales/en-US'
import './styles/element-plus-theme.css'

const i18n = createI18n({
  legacy: false,
  locale: 'zh-CN',
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

const app = createApp(App)

app.use(pinia)

const globalConfigStore = useGlobalConfigStore(pinia)
globalConfigStore.applyTheme()
i18n.global.locale.value = globalConfigStore.locale

app.use(router)
app.use(i18n)
app.mount('#app')
