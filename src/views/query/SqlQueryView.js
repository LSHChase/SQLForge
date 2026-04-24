/* eslint-disable no-unused-vars */
import './SqlQueryView.css'

import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  executeQuery,
  formatRuntimeError,
  getGovernanceMessageStats
} from '../../services/runtimeGateApi'


const __sfc__ = {
  __name: 'SqlQueryView',
  setup(__props, { expose: __expose }) {
  __expose();

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  sqlText: 'SELECT * FROM orders',
  datasourceType: 'HETU',
  accelerationPreference: 'PREFER_ACCELERATED',
  faultToleranceStrategy: 'FAIL_FAST'
})

const running = ref(false)
const lastScenario = ref('success')
const result = ref(null)
const errorMessage = ref('')
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const previewRows = computed(() => result.value?.rows || [])
const retryPath = computed(() => result.value?.retryPath || [])
const queuePendingDelta = computed(() => {
  if (!queueStatsBefore.value || !queueStatsAfter.value) {
    return 0
  }
  return queueStatsAfter.value.pending - queueStatsBefore.value.pending
})
const queueTotalDelta = computed(() => {
  if (!queueStatsBefore.value || !queueStatsAfter.value) {
    return 0
  }
  return queueStatsAfter.value.total - queueStatsBefore.value.total
})
const compensationDetected = computed(() => queuePendingDelta.value >= 1 || queueTotalDelta.value >= 1)

const datasourceOptions = computed(() => ['HETU', 'HIVE'])
const accelerationOptions = computed(() => [
  {
    value: 'NONE',
    label: isChinese.value ? '不偏好加速' : 'No acceleration preference'
  },
  {
    value: 'PREFER_ACCELERATED',
    label: isChinese.value ? '优先加速链路' : 'Prefer accelerated path'
  }
])
const toleranceOptions = computed(() => [
  {
    value: 'FAIL_FAST',
    label: isChinese.value ? '快速失败' : 'Fail fast'
  },
  {
    value: 'RETRY_THEN_FALLBACK',
    label: isChinese.value ? '重试后回退' : 'Retry then fallback'
  }
])

const resetEvidence = () => {
  result.value = null
  errorMessage.value = ''
  queueStatsBefore.value = null
  queueStatsAfter.value = null
}

const applySuccessPreset = () => {
  form.sqlText = 'SELECT * FROM orders'
  form.datasourceType = 'HETU'
  form.accelerationPreference = 'PREFER_ACCELERATED'
  form.faultToleranceStrategy = 'FAIL_FAST'
}

const applyRecoveryPreset = () => {
  form.sqlText = 'SELECT * FROM orders'
  form.datasourceType = 'HETU'
  form.accelerationPreference = 'PREFER_ACCELERATED'
  form.faultToleranceStrategy = 'RETRY_THEN_FALLBACK'
}

const runSuccessFlow = async () => {
  applySuccessPreset()
  lastScenario.value = 'success'
  running.value = true
  resetEvidence()

  try {
    result.value = await executeQuery({
      ...form
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const runRecoveryFlow = async () => {
  applyRecoveryPreset()
  lastScenario.value = 'recovery'
  running.value = true
  resetEvidence()

  try {
    queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-query-governance-stats-before'
    })
    result.value = await executeQuery({
      ...form,
      queryContext: {
        timeoutMs: 30
      }
    })
    queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-query-governance-stats-after'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const __returned__ = { t, locale, form, running, lastScenario, result, errorMessage, queueStatsBefore, queueStatsAfter, isChinese, previewRows, retryPath, queuePendingDelta, queueTotalDelta, compensationDetected, datasourceOptions, accelerationOptions, toleranceOptions, resetEvidence, applySuccessPreset, applyRecoveryPreset, runSuccessFlow, runRecoveryFlow, computed, reactive, ref, get useI18n() { return useI18n }, get executeQuery() { return executeQuery }, get formatRuntimeError() { return formatRuntimeError }, get getGovernanceMessageStats() { return getGovernanceMessageStats } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, resolveComponent as _resolveComponent, createVNode as _createVNode, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, createBlock as _createBlock, withCtx as _withCtx, createTextVNode as _createTextVNode, createCommentVNode as _createCommentVNode, normalizeClass as _normalizeClass } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "query-flow-page"
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
const _hoisted_13 = { class: "field-block" }
const _hoisted_14 = { class: "field-label" }
const _hoisted_15 = { class: "field-block field-block-wide" }
const _hoisted_16 = { class: "field-block" }
const _hoisted_17 = { class: "field-label" }
const _hoisted_18 = { class: "field-block" }
const _hoisted_19 = { class: "field-label" }
const _hoisted_20 = { class: "action-row action-row-wrap" }
const _hoisted_21 = { class: "surface-card" }
const _hoisted_22 = { class: "section-heading" }
const _hoisted_23 = { class: "section-title" }
const _hoisted_24 = {
  key: 0,
  class: "empty-state"
}
const _hoisted_25 = {
  key: 1,
  class: "result-banner result-banner-danger",
  "data-testid": "query-flow-error"
}
const _hoisted_26 = { "data-testid": "query-flow-status" }
const _hoisted_27 = { "data-testid": "query-flow-engine" }
const _hoisted_28 = { class: "evidence-grid" }
const _hoisted_29 = { class: "evidence-item" }
const _hoisted_30 = { class: "evidence-label" }
const _hoisted_31 = { class: "evidence-item" }
const _hoisted_32 = { class: "evidence-label" }
const _hoisted_33 = { "data-testid": "query-flow-degraded" }
const _hoisted_34 = { class: "evidence-item" }
const _hoisted_35 = { class: "evidence-label" }
const _hoisted_36 = { "data-testid": "query-flow-retry-path-size" }
const _hoisted_37 = { class: "evidence-item" }
const _hoisted_38 = { class: "evidence-label" }
const _hoisted_39 = { class: "evidence-item" }
const _hoisted_40 = { class: "evidence-label" }
const _hoisted_41 = { "data-testid": "query-flow-degrade-reason" }
const _hoisted_42 = { class: "evidence-item" }
const _hoisted_43 = { class: "evidence-label" }
const _hoisted_44 = { "data-testid": "query-flow-row-count" }
const _hoisted_45 = {
  key: 0,
  class: "trace-card",
  "data-testid": "query-flow-retry-path"
}
const _hoisted_46 = {
  key: 1,
  class: "compensation-card"
}
const _hoisted_47 = { "data-testid": "query-flow-compensation-status" }
const _hoisted_48 = { class: "evidence-grid" }
const _hoisted_49 = { class: "evidence-item" }
const _hoisted_50 = { class: "evidence-label" }
const _hoisted_51 = { "data-testid": "query-flow-queue-pending-before" }
const _hoisted_52 = { class: "evidence-item" }
const _hoisted_53 = { class: "evidence-label" }
const _hoisted_54 = { "data-testid": "query-flow-queue-pending-after" }
const _hoisted_55 = { class: "evidence-item" }
const _hoisted_56 = { class: "evidence-label" }
const _hoisted_57 = { "data-testid": "query-flow-queue-pending-delta" }
const _hoisted_58 = { class: "evidence-item" }
const _hoisted_59 = { class: "evidence-label" }
const _hoisted_60 = { "data-testid": "query-flow-queue-total-delta" }
const _hoisted_61 = { class: "evidence-item" }
const _hoisted_62 = { class: "evidence-label" }
const _hoisted_63 = { class: "result-json" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_input = _resolveComponent("el-input")
  const _component_el_option = _resolveComponent("el-option")
  const _component_el_select = _resolveComponent("el-select")
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[5] || (_cache[5] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "frontend runtime gate", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('sqlQuery.title')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('sqlQuery.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '该页面同时覆盖成功执行与降级恢复场景。补偿场景会先读取 governance 队列 stats，再触发浏览器侧真实请求并检查 pending 增量。'
            : 'This page covers both the success path and degraded recovery. The compensation path reads governance queue stats before and after the live browser request and checks for a pending-count increase.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      _createElementVNode("article", _hoisted_7, [
        _createElementVNode("div", _hoisted_8, [
          _createElementVNode("div", null, [
            _cache[6] || (_cache[6] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "live request", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_9, _toDisplayString($setup.isChinese ? '执行参数' : 'Execution request'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_10, [
          _createElementVNode("label", _hoisted_11, [
            _createElementVNode("span", _hoisted_12, _toDisplayString($setup.isChinese ? '租户' : 'Tenant'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.tenantId,
              "onUpdate:modelValue": _cache[0] || (_cache[0] = $event => (($setup.form.tenantId) = $event))
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_13, [
            _createElementVNode("span", _hoisted_14, _toDisplayString($setup.isChinese ? '目标引擎' : 'Target engine'), 1 /* TEXT */),
            _createVNode(_component_el_select, {
              modelValue: $setup.form.datasourceType,
              "onUpdate:modelValue": _cache[1] || (_cache[1] = $event => (($setup.form.datasourceType) = $event))
            }, {
              default: _withCtx(() => [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.datasourceOptions, (option) => {
                  return (_openBlock(), _createBlock(_component_el_option, {
                    key: option,
                    label: option,
                    value: option
                  }, null, 8 /* PROPS */, ["label", "value"]))
                }), 128 /* KEYED_FRAGMENT */))
              ]),
              _: 1 /* STABLE */
            }, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_15, [
            _cache[7] || (_cache[7] = _createElementVNode("span", { class: "field-label" }, "SQL", -1 /* HOISTED */)),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.sqlText,
              "onUpdate:modelValue": _cache[2] || (_cache[2] = $event => (($setup.form.sqlText) = $event)),
              type: "textarea",
              rows: 6
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_16, [
            _createElementVNode("span", _hoisted_17, _toDisplayString($setup.isChinese ? '加速偏好' : 'Acceleration preference'), 1 /* TEXT */),
            _createVNode(_component_el_select, {
              modelValue: $setup.form.accelerationPreference,
              "onUpdate:modelValue": _cache[3] || (_cache[3] = $event => (($setup.form.accelerationPreference) = $event))
            }, {
              default: _withCtx(() => [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.accelerationOptions, (option) => {
                  return (_openBlock(), _createBlock(_component_el_option, {
                    key: option.value,
                    label: option.label,
                    value: option.value
                  }, null, 8 /* PROPS */, ["label", "value"]))
                }), 128 /* KEYED_FRAGMENT */))
              ]),
              _: 1 /* STABLE */
            }, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_18, [
            _createElementVNode("span", _hoisted_19, _toDisplayString($setup.isChinese ? '容错策略' : 'Fault tolerance'), 1 /* TEXT */),
            _createVNode(_component_el_select, {
              modelValue: $setup.form.faultToleranceStrategy,
              "onUpdate:modelValue": _cache[4] || (_cache[4] = $event => (($setup.form.faultToleranceStrategy) = $event))
            }, {
              default: _withCtx(() => [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.toleranceOptions, (option) => {
                  return (_openBlock(), _createBlock(_component_el_option, {
                    key: option.value,
                    label: option.label,
                    value: option.value
                  }, null, 8 /* PROPS */, ["label", "value"]))
                }), 128 /* KEYED_FRAGMENT */))
              ]),
              _: 1 /* STABLE */
            }, 8 /* PROPS */, ["modelValue"])
          ])
        ]),
        _createElementVNode("div", _hoisted_20, [
          _createVNode(_component_el_button, {
            type: "primary",
            loading: $setup.running && $setup.lastScenario === 'success',
            "data-testid": "query-flow-submit",
            onClick: $setup.runSuccessFlow
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行成功链路' : 'Run success gate'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            loading: $setup.running && $setup.lastScenario === 'recovery',
            "data-testid": "query-flow-submit-recovery",
            onClick: $setup.runRecoveryFlow
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行降级恢复 + 补偿' : 'Run degraded recovery + compensation'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"])
        ])
      ]),
      _createElementVNode("article", _hoisted_21, [
        _createElementVNode("div", _hoisted_22, [
          _createElementVNode("div", null, [
            _cache[8] || (_cache[8] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "runtime evidence", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_23, _toDisplayString($setup.isChinese ? '返回结果' : 'Runtime evidence'), 1 /* TEXT */)
          ])
        ]),
        (!$setup.result && !$setup.errorMessage)
          ? (_openBlock(), _createElementBlock("p", _hoisted_24, _toDisplayString($setup.isChinese
              ? '点击左侧按钮后，这里会展示 query-execution 的真实响应，以及补偿场景的 governance 队列证据。'
              : 'After you trigger a scenario, the live query-execution response and any governance queue compensation evidence will render here.'), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.errorMessage)
          ? (_openBlock(), _createElementBlock("div", _hoisted_25, _toDisplayString($setup.errorMessage), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.result)
          ? (_openBlock(), _createElementBlock(_Fragment, { key: 2 }, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", $setup.result.status === 'SUCCESS' ? 'result-banner-success' : 'result-banner-warning'])
              }, [
                _createElementVNode("strong", _hoisted_26, _toDisplayString($setup.result.status), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_27, _toDisplayString($setup.result.metadata?.targetEngine || '-'), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_28, [
                _createElementVNode("div", _hoisted_29, [
                  _createElementVNode("span", _hoisted_30, _toDisplayString($setup.isChinese ? 'SQL 指纹' : 'SQL fingerprint'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.result.sqlFingerprint), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_31, [
                  _createElementVNode("span", _hoisted_32, _toDisplayString($setup.isChinese ? '降级执行' : 'Degraded'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_33, _toDisplayString($setup.result.degraded ? 'true' : 'false'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_34, [
                  _createElementVNode("span", _hoisted_35, _toDisplayString($setup.isChinese ? '补偿重试步数' : 'Retry path size'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_36, _toDisplayString($setup.retryPath.length), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_37, [
                  _createElementVNode("span", _hoisted_38, _toDisplayString($setup.isChinese ? '实现阶段' : 'Implementation stage'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.result.implementationStage), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_39, [
                  _createElementVNode("span", _hoisted_40, _toDisplayString($setup.isChinese ? '降级原因' : 'Degrade reason'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_41, _toDisplayString($setup.result.degradeReason || '-'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_42, [
                  _createElementVNode("span", _hoisted_43, _toDisplayString($setup.isChinese ? '返回行数' : 'Rows returned'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_44, _toDisplayString($setup.previewRows.length), 1 /* TEXT */)
                ])
              ]),
              ($setup.retryPath.length > 0)
                ? (_openBlock(), _createElementBlock("div", _hoisted_45, [
                    (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.retryPath, (step, index) => {
                      return (_openBlock(), _createElementBlock("div", {
                        key: `${step.engine}-${index}`,
                        class: "trace-step"
                      }, [
                        _createElementVNode("strong", null, _toDisplayString(step.engine), 1 /* TEXT */),
                        _createElementVNode("span", null, _toDisplayString(step.resultStatus), 1 /* TEXT */),
                        _createElementVNode("span", null, _toDisplayString(step.elapsedMs) + "ms", 1 /* TEXT */)
                      ]))
                    }), 128 /* KEYED_FRAGMENT */))
                  ]))
                : _createCommentVNode("v-if", true),
              ($setup.queueStatsBefore && $setup.queueStatsAfter)
                ? (_openBlock(), _createElementBlock("div", _hoisted_46, [
                    _createElementVNode("div", {
                      class: _normalizeClass(["result-banner", $setup.compensationDetected ? 'result-banner-success' : 'result-banner-danger'])
                    }, [
                      _createElementVNode("strong", _hoisted_47, _toDisplayString($setup.compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED'), 1 /* TEXT */),
                      _createElementVNode("span", null, _toDisplayString($setup.isChinese ? 'governance queue pending' : 'governance queue pending'), 1 /* TEXT */)
                    ], 2 /* CLASS */),
                    _createElementVNode("div", _hoisted_48, [
                      _createElementVNode("div", _hoisted_49, [
                        _createElementVNode("span", _hoisted_50, _toDisplayString($setup.isChinese ? '补偿前 pending' : 'Pending before'), 1 /* TEXT */),
                        _createElementVNode("strong", _hoisted_51, _toDisplayString($setup.queueStatsBefore.pending), 1 /* TEXT */)
                      ]),
                      _createElementVNode("div", _hoisted_52, [
                        _createElementVNode("span", _hoisted_53, _toDisplayString($setup.isChinese ? '补偿后 pending' : 'Pending after'), 1 /* TEXT */),
                        _createElementVNode("strong", _hoisted_54, _toDisplayString($setup.queueStatsAfter.pending), 1 /* TEXT */)
                      ]),
                      _createElementVNode("div", _hoisted_55, [
                        _createElementVNode("span", _hoisted_56, _toDisplayString($setup.isChinese ? 'pending 增量' : 'Pending delta'), 1 /* TEXT */),
                        _createElementVNode("strong", _hoisted_57, _toDisplayString($setup.queuePendingDelta), 1 /* TEXT */)
                      ]),
                      _createElementVNode("div", _hoisted_58, [
                        _createElementVNode("span", _hoisted_59, _toDisplayString($setup.isChinese ? 'total 增量' : 'Total delta'), 1 /* TEXT */),
                        _createElementVNode("strong", _hoisted_60, _toDisplayString($setup.queueTotalDelta), 1 /* TEXT */)
                      ]),
                      _createElementVNode("div", _hoisted_61, [
                        _createElementVNode("span", _hoisted_62, _toDisplayString($setup.isChinese ? '失败消息数' : 'Failed messages'), 1 /* TEXT */),
                        _createElementVNode("strong", null, _toDisplayString($setup.queueStatsAfter.failed), 1 /* TEXT */)
                      ])
                    ])
                  ]))
                : _createCommentVNode("v-if", true),
              _createElementVNode("pre", _hoisted_63, _toDisplayString(JSON.stringify($setup.previewRows.slice(0, 2), null, 2)), 1 /* TEXT */)
            ], 64 /* STABLE_FRAGMENT */))
          : _createCommentVNode("v-if", true)
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-df3d3b0f'
__sfc__.render = render
__sfc__.__file = "src/views/query/SqlQueryView.js"

export default __sfc__
