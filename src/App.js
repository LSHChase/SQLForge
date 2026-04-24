/* eslint-disable no-unused-vars */
import './App.css'

import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { constantRoutes } from './router'
import { useGlobalConfigStore, useTenantStore, useUserStore } from './stores'


const __sfc__ = {
  __name: 'App',
  setup(__props, { expose: __expose }) {
  __expose();

const route = useRoute()
const { t, locale } = useI18n()
const globalConfigStore = useGlobalConfigStore()
const tenantStore = useTenantStore()
const userStore = useUserStore()

const navGroupOrder = ['main', 'governanceHistory', 'governanceOps', 'temporary']
const menuSections = computed(() =>
  navGroupOrder
    .map(groupKey => {
      const items = constantRoutes.filter(item => item.meta?.menu && item.meta?.navGroup === groupKey)
      if (!items.length) {
        return null
      }
      return {
        key: groupKey,
        label: t(`common.navGroups.${groupKey}`),
        items
      }
    })
    .filter(Boolean)
)
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

const __returned__ = { route, t, locale, globalConfigStore, tenantStore, userStore, navGroupOrder, menuSections, localeLabel, themeLabel, pageDescription, workspaceSummary, userBadge, buildRoutePills, activeRoutePills, handleThemeToggle, handleLocaleToggle, computed, onMounted, get useI18n() { return useI18n }, get useRoute() { return useRoute }, get constantRoutes() { return constantRoutes }, get useGlobalConfigStore() { return useGlobalConfigStore }, get useTenantStore() { return useTenantStore }, get useUserStore() { return useUserStore } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { toDisplayString as _toDisplayString, createElementVNode as _createElementVNode, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, normalizeClass as _normalizeClass, createCommentVNode as _createCommentVNode, resolveComponent as _resolveComponent, withCtx as _withCtx, createBlock as _createBlock, createVNode as _createVNode, createTextVNode as _createTextVNode, resolveDynamicComponent as _resolveDynamicComponent, Transition as _Transition } from "vue"

const _hoisted_1 = { class: "brand-panel" }
const _hoisted_2 = { class: "brand-kicker" }
const _hoisted_3 = { class: "brand-title" }
const _hoisted_4 = { class: "brand-summary" }
const _hoisted_5 = { class: "brand-meta" }
const _hoisted_6 = { class: "brand-pill" }
const _hoisted_7 = { class: "brand-pill brand-pill-muted" }
const _hoisted_8 = { class: "sidebar-section" }
const _hoisted_9 = { class: "sidebar-section-label sqlforge-code-label" }
const _hoisted_10 = { class: "menu-section-stack" }
const _hoisted_11 = { class: "menu-section-title sqlforge-code-label" }
const _hoisted_12 = { class: "menu-item-content" }
const _hoisted_13 = { class: "menu-item-label" }
const _hoisted_14 = {
  key: 0,
  class: "menu-item-pills"
}
const _hoisted_15 = { class: "sidebar-runtime" }
const _hoisted_16 = { class: "sidebar-section-label sqlforge-code-label" }
const _hoisted_17 = { class: "runtime-card" }
const _hoisted_18 = { class: "runtime-row" }
const _hoisted_19 = { class: "runtime-row" }
const _hoisted_20 = { class: "runtime-row" }
const _hoisted_21 = { class: "page-heading" }
const _hoisted_22 = { class: "page-kicker" }
const _hoisted_23 = { class: "page-title-row" }
const _hoisted_24 = { class: "page-title" }
const _hoisted_25 = { class: "page-title-pills" }
const _hoisted_26 = { class: "page-status-pill" }
const _hoisted_27 = { class: "page-summary" }
const _hoisted_28 = { class: "header-actions" }
const _hoisted_29 = { class: "workspace-card" }
const _hoisted_30 = { class: "workspace-label sqlforge-code-label" }
const _hoisted_31 = { class: "workspace-summary" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_menu_item = _resolveComponent("el-menu-item")
  const _component_el_menu = _resolveComponent("el-menu")
  const _component_el_scrollbar = _resolveComponent("el-scrollbar")
  const _component_el_aside = _resolveComponent("el-aside")
  const _component_el_button = _resolveComponent("el-button")
  const _component_el_header = _resolveComponent("el-header")
  const _component_router_view = _resolveComponent("router-view")
  const _component_el_main = _resolveComponent("el-main")
  const _component_el_container = _resolveComponent("el-container")
  const _component_el_config_provider = _resolveComponent("el-config-provider")

  return (_openBlock(), _createBlock(_component_el_config_provider, null, {
    default: _withCtx(() => [
      _createVNode(_component_el_container, { class: "app-shell" }, {
        default: _withCtx(() => [
          _createVNode(_component_el_aside, {
            class: "app-sidebar",
            width: "280px"
          }, {
            default: _withCtx(() => [
              _createElementVNode("div", _hoisted_1, [
                _createElementVNode("p", _hoisted_2, _toDisplayString($setup.t('common.platformTagline')), 1 /* TEXT */),
                _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('common.appName')), 1 /* TEXT */),
                _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('common.brandSummary')), 1 /* TEXT */),
                _createElementVNode("div", _hoisted_5, [
                  _createElementVNode("span", _hoisted_6, _toDisplayString($setup.tenantStore.tenantName), 1 /* TEXT */),
                  _createElementVNode("span", _hoisted_7, _toDisplayString($setup.userBadge), 1 /* TEXT */)
                ])
              ]),
              _createElementVNode("div", _hoisted_8, [
                _createElementVNode("p", _hoisted_9, _toDisplayString($setup.t('common.sidebarLabel')), 1 /* TEXT */),
                _createVNode(_component_el_scrollbar, { class: "menu-scroll" }, {
                  default: _withCtx(() => [
                    _createElementVNode("div", _hoisted_10, [
                      (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.menuSections, (section) => {
                        return (_openBlock(), _createElementBlock("section", {
                          key: section.key,
                          class: "menu-section"
                        }, [
                          _createElementVNode("p", _hoisted_11, _toDisplayString(section.label), 1 /* TEXT */),
                          _createVNode(_component_el_menu, {
                            "default-active": $setup.route.path,
                            class: "app-menu",
                            router: ""
                          }, {
                            default: _withCtx(() => [
                              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(section.items, (item) => {
                                return (_openBlock(), _createBlock(_component_el_menu_item, {
                                  key: item.path,
                                  index: item.path
                                }, {
                                  default: _withCtx(() => [
                                    _createElementVNode("div", _hoisted_12, [
                                      _createElementVNode("span", _hoisted_13, _toDisplayString($setup.t(item.meta.titleKey)), 1 /* TEXT */),
                                      ($setup.buildRoutePills(item.meta).length)
                                        ? (_openBlock(), _createElementBlock("div", _hoisted_14, [
                                            (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.buildRoutePills(item.meta), (pill) => {
                                              return (_openBlock(), _createElementBlock("span", {
                                                key: `${item.path}-${pill.key}`,
                                                class: _normalizeClass(["menu-item-pill", pill.className])
                                              }, _toDisplayString(pill.label), 3 /* TEXT, CLASS */))
                                            }), 128 /* KEYED_FRAGMENT */))
                                          ]))
                                        : _createCommentVNode("v-if", true)
                                    ])
                                  ]),
                                  _: 2 /* DYNAMIC */
                                }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["index"]))
                              }), 128 /* KEYED_FRAGMENT */))
                            ]),
                            _: 2 /* DYNAMIC */
                          }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["default-active"])
                        ]))
                      }), 128 /* KEYED_FRAGMENT */))
                    ])
                  ]),
                  _: 1 /* STABLE */
                })
              ]),
              _createElementVNode("div", _hoisted_15, [
                _createElementVNode("p", _hoisted_16, _toDisplayString($setup.t('common.runtimeLabel')), 1 /* TEXT */),
                _createElementVNode("div", _hoisted_17, [
                  _createElementVNode("div", _hoisted_18, [
                    _createElementVNode("span", null, _toDisplayString($setup.t('common.defaultEngine')), 1 /* TEXT */),
                    _createElementVNode("strong", null, _toDisplayString($setup.tenantStore.defaultEngine), 1 /* TEXT */)
                  ]),
                  _createElementVNode("div", _hoisted_19, [
                    _createElementVNode("span", null, _toDisplayString($setup.t('common.backupEngine')), 1 /* TEXT */),
                    _createElementVNode("strong", null, _toDisplayString($setup.tenantStore.backupEngine), 1 /* TEXT */)
                  ]),
                  _createElementVNode("div", _hoisted_20, [
                    _createElementVNode("span", null, _toDisplayString($setup.t('common.currentTenant')), 1 /* TEXT */),
                    _createElementVNode("strong", null, _toDisplayString($setup.tenantStore.tenantId), 1 /* TEXT */)
                  ])
                ])
              ])
            ]),
            _: 1 /* STABLE */
          }),
          _createVNode(_component_el_container, { class: "app-main" }, {
            default: _withCtx(() => [
              _createVNode(_component_el_header, { class: "app-header" }, {
                default: _withCtx(() => [
                  _createElementVNode("div", _hoisted_21, [
                    _createElementVNode("p", _hoisted_22, _toDisplayString($setup.t('common.currentWorkspace')), 1 /* TEXT */),
                    _createElementVNode("div", _hoisted_23, [
                      _createElementVNode("h2", _hoisted_24, _toDisplayString($setup.t($setup.route.meta.titleKey || 'dashboard.title')), 1 /* TEXT */),
                      _createElementVNode("div", _hoisted_25, [
                        _createElementVNode("span", _hoisted_26, _toDisplayString($setup.t('common.desktopMode')), 1 /* TEXT */),
                        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.activeRoutePills, (pill) => {
                          return (_openBlock(), _createElementBlock("span", {
                            key: pill.key,
                            class: _normalizeClass(["page-status-pill", pill.className])
                          }, _toDisplayString(pill.label), 3 /* TEXT, CLASS */))
                        }), 128 /* KEYED_FRAGMENT */))
                      ])
                    ]),
                    _createElementVNode("p", _hoisted_27, _toDisplayString($setup.pageDescription), 1 /* TEXT */)
                  ]),
                  _createElementVNode("div", _hoisted_28, [
                    _createElementVNode("div", _hoisted_29, [
                      _createElementVNode("p", _hoisted_30, _toDisplayString($setup.t('common.workspaceLabel')), 1 /* TEXT */),
                      _createElementVNode("p", _hoisted_31, _toDisplayString($setup.workspaceSummary), 1 /* TEXT */)
                    ]),
                    _createVNode(_component_el_button, {
                      text: "",
                      class: "header-action",
                      onClick: $setup.handleLocaleToggle
                    }, {
                      default: _withCtx(() => [
                        _createTextVNode(_toDisplayString($setup.localeLabel), 1 /* TEXT */)
                      ]),
                      _: 1 /* STABLE */
                    }),
                    _createVNode(_component_el_button, {
                      class: "header-action header-action-outline",
                      onClick: $setup.handleThemeToggle
                    }, {
                      default: _withCtx(() => [
                        _createTextVNode(_toDisplayString($setup.themeLabel), 1 /* TEXT */)
                      ]),
                      _: 1 /* STABLE */
                    })
                  ])
                ]),
                _: 1 /* STABLE */
              }),
              _createVNode(_component_el_main, { class: "page-container" }, {
                default: _withCtx(() => [
                  _createVNode(_component_router_view, null, {
                    default: _withCtx(({ Component }) => [
                      _createVNode(_Transition, {
                        name: "fade-slide",
                        mode: "out-in"
                      }, {
                        default: _withCtx(() => [
                          (_openBlock(), _createBlock(_resolveDynamicComponent(Component)))
                        ]),
                        _: 2 /* DYNAMIC */
                      }, 1024 /* DYNAMIC_SLOTS */)
                    ]),
                    _: 1 /* STABLE */
                  })
                ]),
                _: 1 /* STABLE */
              })
            ]),
            _: 1 /* STABLE */
          })
        ]),
        _: 1 /* STABLE */
      })
    ]),
    _: 1 /* STABLE */
  }))
}
__sfc__.__scopeId = 'data-v-7a7a37b1'
__sfc__.render = render
__sfc__.__file = "src/App.js"

export default __sfc__
