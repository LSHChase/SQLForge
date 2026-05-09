import { createApp } from 'vue'
import {
  ElAside,
  ElButton,
  ElCheckbox,
  ElConfigProvider,
  ElContainer,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElHeader,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElScrollbar,
  ElSelect,
  ElSubMenu,
  ElSwitch,
  ElTag
  ,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTree
} from 'element-plus'
import 'element-plus/dist/index.css'
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

;[
  ElAside,
  ElButton,
  ElCheckbox,
  ElConfigProvider,
  ElContainer,
  ElDatePicker,
  ElDialog,
  ElDrawer,
  ElEmpty,
  ElForm,
  ElFormItem,
  ElHeader,
  ElInput,
  ElInputNumber,
  ElMain,
  ElMenu,
  ElMenuItem,
  ElOption,
  ElPagination,
  ElScrollbar,
  ElSelect,
  ElSubMenu,
  ElSwitch,
  ElTabPane,
  ElTable,
  ElTableColumn,
  ElTabs,
  ElTag,
  ElTree
].forEach(component => {
  app.component(component.name, component)
})
app.use(ElLoading)
app.use(pinia)

const globalConfigStore = useGlobalConfigStore(pinia)
globalConfigStore.applyTheme()
i18n.global.locale.value = globalConfigStore.locale

app.use(router)
app.use(i18n)
app.mount('#app')
