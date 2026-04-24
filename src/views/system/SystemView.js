/* eslint-disable no-unused-vars */
import './SystemView.css'

import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getGovernanceMessageStats,
  getGovernanceTenantConfig,
  retryGovernanceFailedMessages
} from '../../services/runtimeGateApi'


const __sfc__ = {
  __name: 'SystemView',
  setup(__props, { expose: __expose }) {
  __expose();

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'system'
})

const loading = ref(false)
const retrying = ref(false)
const tenantConfig = ref(null)
const stats = ref(null)
const retryResult = ref(null)
const statsBeforeRetry = ref(null)
const statsAfterRetry = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
const failedDelta = computed(() => {
  if (!statsBeforeRetry.value || !statsAfterRetry.value) {
    return 0
  }
  return statsBeforeRetry.value.failed - statsAfterRetry.value.failed
})
const retryImproved = computed(() => failedDelta.value >= 1 || (retryResult.value?.retriedCount || 0) >= 1)
const queueCards = computed(() => {
  if (!stats.value) {
    return []
  }
  return [
    { key: 'total', label: isChinese.value ? '消息总数' : 'Total messages', value: stats.value.total },
    { key: 'pending', label: isChinese.value ? '待补偿' : 'Pending backlog', value: stats.value.pending },
    { key: 'failed', label: isChinese.value ? '失败待修复' : 'Failed messages', value: stats.value.failed },
    { key: 'consumed', label: isChinese.value ? '已消费' : 'Consumed', value: stats.value.consumed }
  ]
})

const loadEvidence = async () => {
  loading.value = true
  retryResult.value = null
  statsBeforeRetry.value = null
  statsAfterRetry.value = null
  errorMessage.value = ''

  try {
    const [tenantConfigResponse, statsResponse] = await Promise.all([
      getGovernanceTenantConfig(form.tenantId, {
        requestPrefix: 'frontend-system-tenant-config'
      }),
      getGovernanceMessageStats(form.tenantId, {
        requestPrefix: 'frontend-system-message-stats'
      })
    ])
    tenantConfig.value = tenantConfigResponse
    stats.value = statsResponse
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.value = false
  }
}

const retryFailedMessages = async () => {
  retrying.value = true
  errorMessage.value = ''
  retryResult.value = null

  try {
    statsBeforeRetry.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-system-message-stats-before-retry'
    })
    retryResult.value = await retryGovernanceFailedMessages(form.tenantId, {
      requestPrefix: 'frontend-system-message-retry'
    })
    statsAfterRetry.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-system-message-stats-after-retry'
    })
    stats.value = statsAfterRetry.value
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    retrying.value = false
  }
}

onMounted(() => {
  loadEvidence()
})

const __returned__ = { t, locale, form, loading, retrying, tenantConfig, stats, retryResult, statsBeforeRetry, statsAfterRetry, errorMessage, isChinese, failedDelta, retryImproved, queueCards, loadEvidence, retryFailedMessages, computed, onMounted, reactive, ref, get useI18n() { return useI18n }, get formatRuntimeError() { return formatRuntimeError }, get getGovernanceMessageStats() { return getGovernanceMessageStats }, get getGovernanceTenantConfig() { return getGovernanceTenantConfig }, get retryGovernanceFailedMessages() { return retryGovernanceFailedMessages } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, resolveComponent as _resolveComponent, createVNode as _createVNode, createTextVNode as _createTextVNode, withCtx as _withCtx, openBlock as _openBlock, createElementBlock as _createElementBlock, createCommentVNode as _createCommentVNode, Fragment as _Fragment, renderList as _renderList, normalizeClass as _normalizeClass } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "system-flow-page"
}
const _hoisted_2 = { class: "runtime-hero surface-card" }
const _hoisted_3 = { class: "runtime-title" }
const _hoisted_4 = { class: "runtime-summary" }
const _hoisted_5 = { class: "runtime-note" }
const _hoisted_6 = { class: "runtime-grid" }
const _hoisted_7 = { class: "surface-card" }
const _hoisted_8 = { class: "section-heading" }
const _hoisted_9 = { class: "section-title" }
const _hoisted_10 = { class: "form-grid" }
const _hoisted_11 = { class: "field-block" }
const _hoisted_12 = { class: "field-label" }
const _hoisted_13 = { class: "action-row action-row-wrap" }
const _hoisted_14 = { class: "remediation-card" }
const _hoisted_15 = { class: "remediation-title" }
const _hoisted_16 = { class: "remediation-copy" }
const _hoisted_17 = { class: "surface-card" }
const _hoisted_18 = { class: "section-heading" }
const _hoisted_19 = { class: "section-title" }
const _hoisted_20 = {
  key: 0,
  class: "empty-state"
}
const _hoisted_21 = {
  key: 1,
  class: "result-banner result-banner-danger",
  "data-testid": "system-flow-error"
}
const _hoisted_22 = { class: "result-banner result-banner-success" }
const _hoisted_23 = { "data-testid": "system-flow-tenant-config-status" }
const _hoisted_24 = { class: "evidence-grid" }
const _hoisted_25 = { class: "evidence-item" }
const _hoisted_26 = { class: "evidence-label" }
const _hoisted_27 = { "data-testid": "system-flow-audit-level" }
const _hoisted_28 = { class: "evidence-item" }
const _hoisted_29 = { class: "evidence-label" }
const _hoisted_30 = { class: "evidence-item" }
const _hoisted_31 = { class: "evidence-label" }
const _hoisted_32 = { class: "evidence-item" }
const _hoisted_33 = { class: "evidence-label" }
const _hoisted_34 = {
  key: 3,
  class: "queue-card-grid"
}
const _hoisted_35 = { class: "queue-card-label" }
const _hoisted_36 = ["data-testid"]
const _hoisted_37 = {
  key: 4,
  class: "compensation-card"
}
const _hoisted_38 = { "data-testid": "system-flow-retry-status" }
const _hoisted_39 = { "data-testid": "system-flow-retry-count" }
const _hoisted_40 = { class: "evidence-grid" }
const _hoisted_41 = { class: "evidence-item" }
const _hoisted_42 = { class: "evidence-label" }
const _hoisted_43 = { "data-testid": "system-flow-failed-before-retry" }
const _hoisted_44 = { class: "evidence-item" }
const _hoisted_45 = { class: "evidence-label" }
const _hoisted_46 = { "data-testid": "system-flow-failed-after-retry" }
const _hoisted_47 = { class: "evidence-item" }
const _hoisted_48 = { class: "evidence-label" }
const _hoisted_49 = { "data-testid": "system-flow-failed-delta" }
const _hoisted_50 = { class: "evidence-item" }
const _hoisted_51 = { class: "evidence-label" }
const _hoisted_52 = { "data-testid": "system-flow-repair-outcome" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_input = _resolveComponent("el-input")
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[1] || (_cache[1] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "frontend runtime gate", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('system.title')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('system.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '该页直接调用 governance 管理接口，展示租户配置、消息 backlog 和失败消息 retry 修复结果，用于把浏览器门禁继续扩到治理修复动作。'
            : 'This page calls the live governance admin APIs and renders tenant config, message backlog, and failed-message retry evidence so the browser gate can cover governance remediation actions.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      _createElementVNode("article", _hoisted_7, [
        _createElementVNode("div", _hoisted_8, [
          _createElementVNode("div", null, [
            _cache[2] || (_cache[2] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "governance admin", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_9, _toDisplayString($setup.isChinese ? '治理管理动作' : 'Governance admin actions'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_10, [
          _createElementVNode("label", _hoisted_11, [
            _createElementVNode("span", _hoisted_12, _toDisplayString($setup.isChinese ? '租户上下文' : 'Tenant context'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.tenantId,
              "onUpdate:modelValue": _cache[0] || (_cache[0] = $event => (($setup.form.tenantId) = $event)),
              "data-testid": "system-flow-tenant-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ])
        ]),
        _createElementVNode("div", _hoisted_13, [
          _createVNode(_component_el_button, {
            type: "primary",
            loading: $setup.loading,
            "data-testid": "system-flow-refresh",
            onClick: $setup.loadEvidence
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '刷新治理证据' : 'Refresh governance evidence'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            loading: $setup.retrying,
            "data-testid": "system-flow-retry",
            onClick: $setup.retryFailedMessages
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '重试失败消息' : 'Retry failed messages'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"])
        ]),
        _createElementVNode("div", _hoisted_14, [
          _createElementVNode("p", _hoisted_15, _toDisplayString($setup.isChinese ? '修复动作说明' : 'Remediation guidance'), 1 /* TEXT */),
          _createElementVNode("p", _hoisted_16, _toDisplayString($setup.isChinese
                ? '当 failed > 0 时，先确认 backlog 归因，再执行 retry。若 retriedCount 增加且 failed 降低，即视为补偿修复动作真实生效。'
                : 'When failed > 0, confirm the backlog source and then run retry. If retriedCount increases and failed decreases, the compensation repair action is considered effective.'), 1 /* TEXT */)
        ])
      ]),
      _createElementVNode("article", _hoisted_17, [
        _createElementVNode("div", _hoisted_18, [
          _createElementVNode("div", null, [
            _cache[3] || (_cache[3] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "runtime evidence", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_19, _toDisplayString($setup.isChinese ? '治理与修复证据' : 'Governance and remediation evidence'), 1 /* TEXT */)
          ])
        ]),
        (!$setup.tenantConfig && !$setup.stats && !$setup.errorMessage)
          ? (_openBlock(), _createElementBlock("p", _hoisted_20, _toDisplayString($setup.isChinese
              ? '页面会自动加载治理租户配置与消息队列统计。'
              : 'The page automatically loads the governance tenant config and message queue stats.'), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.errorMessage)
          ? (_openBlock(), _createElementBlock("div", _hoisted_21, _toDisplayString($setup.errorMessage), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.tenantConfig)
          ? (_openBlock(), _createElementBlock(_Fragment, { key: 2 }, [
              _createElementVNode("div", _hoisted_22, [
                _createElementVNode("strong", _hoisted_23, _toDisplayString($setup.tenantConfig.tenantId), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.tenantConfig.defaultEngine) + " / " + _toDisplayString($setup.tenantConfig.backupEngine), 1 /* TEXT */)
              ]),
              _createElementVNode("div", _hoisted_24, [
                _createElementVNode("div", _hoisted_25, [
                  _createElementVNode("span", _hoisted_26, _toDisplayString($setup.isChinese ? '审计级别' : 'Audit level'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_27, _toDisplayString($setup.tenantConfig.auditLevel), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_28, [
                  _createElementVNode("span", _hoisted_29, _toDisplayString($setup.isChinese ? '保留天数' : 'Retention days'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.tenantConfig.retentionDays), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_30, [
                  _createElementVNode("span", _hoisted_31, _toDisplayString($setup.isChinese ? '并发配额' : 'Concurrency quota'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.tenantConfig.quotaConcurrent), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_32, [
                  _createElementVNode("span", _hoisted_33, _toDisplayString($setup.isChinese ? '加速配额' : 'Acceleration quota'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.tenantConfig.accelerationQuota), 1 /* TEXT */)
                ])
              ])
            ], 64 /* STABLE_FRAGMENT */))
          : _createCommentVNode("v-if", true),
        ($setup.stats)
          ? (_openBlock(), _createElementBlock("div", _hoisted_34, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.queueCards, (card) => {
                return (_openBlock(), _createElementBlock("article", {
                  key: card.key,
                  class: "queue-card"
                }, [
                  _createElementVNode("span", _hoisted_35, _toDisplayString(card.label), 1 /* TEXT */),
                  _createElementVNode("strong", {
                    class: "queue-card-value",
                    "data-testid": `system-flow-queue-${card.key}`
                  }, _toDisplayString(card.value), 9 /* TEXT, PROPS */, _hoisted_36)
                ]))
              }), 128 /* KEYED_FRAGMENT */))
            ]))
          : _createCommentVNode("v-if", true),
        ($setup.retryResult)
          ? (_openBlock(), _createElementBlock("div", _hoisted_37, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", $setup.retryImproved ? 'result-banner-success' : 'result-banner-warning'])
              }, [
                _createElementVNode("strong", _hoisted_38, _toDisplayString($setup.retryResult.status), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_39, _toDisplayString($setup.retryResult.retriedCount), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_40, [
                _createElementVNode("div", _hoisted_41, [
                  _createElementVNode("span", _hoisted_42, _toDisplayString($setup.isChinese ? '重试前 failed' : 'Failed before retry'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_43, _toDisplayString($setup.statsBeforeRetry?.failed ?? 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_44, [
                  _createElementVNode("span", _hoisted_45, _toDisplayString($setup.isChinese ? '重试后 failed' : 'Failed after retry'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_46, _toDisplayString($setup.statsAfterRetry?.failed ?? 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_47, [
                  _createElementVNode("span", _hoisted_48, _toDisplayString($setup.isChinese ? 'failed 降幅' : 'Failed delta'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_49, _toDisplayString($setup.failedDelta), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_50, [
                  _createElementVNode("span", _hoisted_51, _toDisplayString($setup.isChinese ? '修复结论' : 'Repair outcome'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_52, _toDisplayString($setup.retryImproved ? 'REPAIRED' : 'RETRY_ACCEPTED'), 1 /* TEXT */)
                ])
              ])
            ]))
          : _createCommentVNode("v-if", true)
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-d7d2f3df'
__sfc__.render = render
__sfc__.__file = "src/views/system/SystemView.js"

export default __sfc__
