/* eslint-disable no-unused-vars */
import './AccelerationView.css'

import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getGovernanceMessageStats,
  getOptimizationTaskStatus,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  submitOptimizationTask,
  waitForOptimizationTask
} from '../../services/runtimeGateApi'


const __sfc__ = {
  __name: 'AccelerationView',
  setup(__props, { expose: __expose }) {
  __expose();

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  taskType: 'REWRITE',
  sqlText: 'SELECT * FROM orders',
  datasourceType: 'HETU'
})

const running = ref(false)
const lastScenario = ref('success')
const submitResult = ref(null)
const taskStatus = ref(null)
const compensationStatus = ref(null)
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
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
const taskTypeOptions = computed(() => [
  {
    value: 'REWRITE',
    label: isChinese.value ? '改写建议' : 'Rewrite suggestion'
  },
  {
    value: 'ACCELERATION_SUGGESTION',
    label: isChinese.value ? '加速建议' : 'Acceleration suggestion'
  }
])

const resetEvidence = () => {
  submitResult.value = null
  taskStatus.value = null
  compensationStatus.value = null
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  errorMessage.value = ''
}

const applySuccessPreset = () => {
  form.taskType = 'REWRITE'
  form.sqlText = 'SELECT * FROM orders'
  form.datasourceType = 'HETU'
}

const applyFailurePreset = () => {
  form.taskType = 'ACCELERATION_SUGGESTION'
  form.sqlText = 'SELECT * FROM orders /*FAIL_OPTIMIZATION*/'
  form.datasourceType = 'HETU'
}

const runSuccessFlow = async () => {
  applySuccessPreset()
  lastScenario.value = 'success'
  running.value = true
  resetEvidence()

  try {
    submitResult.value = await submitOptimizationTask({
      ...form,
      taskContext: {
        priority: 'HIGH',
        parseDepth: 'DEEP'
      }
    })
    taskStatus.value = await waitForOptimizationTask(submitResult.value.taskId, form.tenantId)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const runFailureCompensationFlow = async () => {
  applyFailurePreset()
  lastScenario.value = 'failure'
  running.value = true
  resetEvidence()

  try {
    queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-sql-optimization-governance-stats-before'
    })
    submitResult.value = await submitOptimizationTask({
      ...form
    })
    taskStatus.value = await waitForOptimizationTask(submitResult.value.taskId, form.tenantId)
    compensationStatus.value = await getOptimizationTaskStatus(submitResult.value.taskId, form.tenantId, {
      requestPrefix: 'frontend-sql-optimization-compensation-status',
      tracePrefix: GOVERNANCE_COMPENSATION_TRACE_PREFIX
    })
    queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-sql-optimization-governance-stats-after'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const __returned__ = { t, locale, form, running, lastScenario, submitResult, taskStatus, compensationStatus, queueStatsBefore, queueStatsAfter, errorMessage, isChinese, queuePendingDelta, queueTotalDelta, compensationDetected, taskTypeOptions, resetEvidence, applySuccessPreset, applyFailurePreset, runSuccessFlow, runFailureCompensationFlow, computed, reactive, ref, get useI18n() { return useI18n }, get formatRuntimeError() { return formatRuntimeError }, get getGovernanceMessageStats() { return getGovernanceMessageStats }, get getOptimizationTaskStatus() { return getOptimizationTaskStatus }, get GOVERNANCE_COMPENSATION_TRACE_PREFIX() { return GOVERNANCE_COMPENSATION_TRACE_PREFIX }, get submitOptimizationTask() { return submitOptimizationTask }, get waitForOptimizationTask() { return waitForOptimizationTask } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, resolveComponent as _resolveComponent, createVNode as _createVNode, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, createBlock as _createBlock, withCtx as _withCtx, createTextVNode as _createTextVNode, createCommentVNode as _createCommentVNode, normalizeClass as _normalizeClass } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "optimization-flow-page"
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
const _hoisted_15 = { class: "field-block" }
const _hoisted_16 = { class: "field-label" }
const _hoisted_17 = { class: "field-block field-block-wide" }
const _hoisted_18 = { class: "action-row action-row-wrap" }
const _hoisted_19 = { class: "surface-card" }
const _hoisted_20 = { class: "section-heading" }
const _hoisted_21 = { class: "section-title" }
const _hoisted_22 = {
  key: 0,
  class: "empty-state"
}
const _hoisted_23 = {
  key: 1,
  class: "result-banner result-banner-danger",
  "data-testid": "optimization-flow-error"
}
const _hoisted_24 = {
  key: 2,
  class: "evidence-grid"
}
const _hoisted_25 = { class: "evidence-item" }
const _hoisted_26 = { class: "evidence-label" }
const _hoisted_27 = { "data-testid": "optimization-flow-task-id" }
const _hoisted_28 = { class: "evidence-item" }
const _hoisted_29 = { class: "evidence-label" }
const _hoisted_30 = { "data-testid": "optimization-flow-status" }
const _hoisted_31 = { class: "evidence-grid" }
const _hoisted_32 = { class: "evidence-item" }
const _hoisted_33 = { class: "evidence-label" }
const _hoisted_34 = { class: "evidence-item" }
const _hoisted_35 = { class: "evidence-label" }
const _hoisted_36 = {
  key: 0,
  class: "result-copy",
  "data-testid": "optimization-flow-summary"
}
const _hoisted_37 = {
  key: 1,
  class: "trace-card"
}
const _hoisted_38 = { class: "evidence-grid" }
const _hoisted_39 = { class: "evidence-item" }
const _hoisted_40 = { class: "evidence-label" }
const _hoisted_41 = { "data-testid": "optimization-flow-failure-code" }
const _hoisted_42 = { class: "evidence-item" }
const _hoisted_43 = { class: "evidence-label" }
const _hoisted_44 = { class: "evidence-item" }
const _hoisted_45 = { class: "evidence-label" }
const _hoisted_46 = { class: "evidence-item" }
const _hoisted_47 = { class: "evidence-label" }
const _hoisted_48 = { class: "result-copy" }
const _hoisted_49 = {
  key: 4,
  class: "compensation-card"
}
const _hoisted_50 = { "data-testid": "optimization-flow-compensation-indicator" }
const _hoisted_51 = { "data-testid": "optimization-flow-compensation-status" }
const _hoisted_52 = { class: "evidence-grid" }
const _hoisted_53 = { class: "evidence-item" }
const _hoisted_54 = { class: "evidence-label" }
const _hoisted_55 = { "data-testid": "optimization-flow-queue-pending-before" }
const _hoisted_56 = { class: "evidence-item" }
const _hoisted_57 = { class: "evidence-label" }
const _hoisted_58 = { "data-testid": "optimization-flow-queue-pending-after" }
const _hoisted_59 = { class: "evidence-item" }
const _hoisted_60 = { class: "evidence-label" }
const _hoisted_61 = { "data-testid": "optimization-flow-queue-pending-delta" }
const _hoisted_62 = { class: "evidence-item" }
const _hoisted_63 = { class: "evidence-label" }
const _hoisted_64 = { "data-testid": "optimization-flow-queue-total-delta" }
const _hoisted_65 = { class: "evidence-item" }
const _hoisted_66 = { class: "evidence-label" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_input = _resolveComponent("el-input")
  const _component_el_option = _resolveComponent("el-option")
  const _component_el_select = _resolveComponent("el-select")
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[4] || (_cache[4] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "frontend runtime gate", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('acceleration.title')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('acceleration.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '该页把浏览器按钮直接接到 sql-optimization 提交、终态轮询和补偿状态查询。失败场景会校验 governance 队列 pending 是否增长。'
            : 'This page wires browser actions to sql-optimization submission, terminal polling, and compensated status queries. The failure path checks whether governance queue pending count increases.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      _createElementVNode("article", _hoisted_7, [
        _createElementVNode("div", _hoisted_8, [
          _createElementVNode("div", null, [
            _cache[5] || (_cache[5] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "async task", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_9, _toDisplayString($setup.isChinese ? '优化任务参数' : 'Optimization request'), 1 /* TEXT */)
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
            _createElementVNode("span", _hoisted_14, _toDisplayString($setup.isChinese ? '任务类型' : 'Task type'), 1 /* TEXT */),
            _createVNode(_component_el_select, {
              modelValue: $setup.form.taskType,
              "onUpdate:modelValue": _cache[1] || (_cache[1] = $event => (($setup.form.taskType) = $event))
            }, {
              default: _withCtx(() => [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.taskTypeOptions, (option) => {
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
          _createElementVNode("label", _hoisted_15, [
            _createElementVNode("span", _hoisted_16, _toDisplayString($setup.isChinese ? '数据源' : 'Datasource'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.datasourceType,
              "onUpdate:modelValue": _cache[2] || (_cache[2] = $event => (($setup.form.datasourceType) = $event))
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_17, [
            _cache[6] || (_cache[6] = _createElementVNode("span", { class: "field-label" }, "SQL", -1 /* HOISTED */)),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.sqlText,
              "onUpdate:modelValue": _cache[3] || (_cache[3] = $event => (($setup.form.sqlText) = $event)),
              type: "textarea",
              rows: 6
            }, null, 8 /* PROPS */, ["modelValue"])
          ])
        ]),
        _createElementVNode("div", _hoisted_18, [
          _createVNode(_component_el_button, {
            type: "primary",
            loading: $setup.running && $setup.lastScenario === 'success',
            "data-testid": "optimization-flow-submit",
            onClick: $setup.runSuccessFlow
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行成功任务' : 'Run success task'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            loading: $setup.running && $setup.lastScenario === 'failure',
            "data-testid": "optimization-flow-submit-failure",
            onClick: $setup.runFailureCompensationFlow
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行失败恢复 + 补偿' : 'Run failure recovery + compensation'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"])
        ])
      ]),
      _createElementVNode("article", _hoisted_19, [
        _createElementVNode("div", _hoisted_20, [
          _createElementVNode("div", null, [
            _cache[7] || (_cache[7] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "runtime evidence", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_21, _toDisplayString($setup.isChinese ? '任务状态' : 'Task evidence'), 1 /* TEXT */)
          ])
        ]),
        (!$setup.submitResult && !$setup.errorMessage)
          ? (_openBlock(), _createElementBlock("p", _hoisted_22, _toDisplayString($setup.isChinese
              ? '左侧按钮会分别展示成功摘要，或失败终态 + 补偿队列证据。'
              : 'The actions on the left render either the success summary or the failed terminal state plus compensation queue evidence.'), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.errorMessage)
          ? (_openBlock(), _createElementBlock("div", _hoisted_23, _toDisplayString($setup.errorMessage), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.submitResult)
          ? (_openBlock(), _createElementBlock("div", _hoisted_24, [
              _createElementVNode("div", _hoisted_25, [
                _createElementVNode("span", _hoisted_26, _toDisplayString($setup.isChinese ? '任务 ID' : 'Task ID'), 1 /* TEXT */),
                _createElementVNode("strong", _hoisted_27, _toDisplayString($setup.submitResult.taskId), 1 /* TEXT */)
              ]),
              _createElementVNode("div", _hoisted_28, [
                _createElementVNode("span", _hoisted_29, _toDisplayString($setup.isChinese ? '初始状态' : 'Initial status'), 1 /* TEXT */),
                _createElementVNode("strong", null, _toDisplayString($setup.submitResult.status), 1 /* TEXT */)
              ])
            ]))
          : _createCommentVNode("v-if", true),
        ($setup.taskStatus)
          ? (_openBlock(), _createElementBlock(_Fragment, { key: 3 }, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", $setup.taskStatus.status === 'SUCCEEDED' ? 'result-banner-success' : 'result-banner-warning'])
              }, [
                _createElementVNode("strong", _hoisted_30, _toDisplayString($setup.taskStatus.status), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.taskStatus.currentPhase), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_31, [
                _createElementVNode("div", _hoisted_32, [
                  _createElementVNode("span", _hoisted_33, _toDisplayString($setup.isChinese ? '进度' : 'Progress'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.progressPercent) + "%", 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_34, [
                  _createElementVNode("span", _hoisted_35, _toDisplayString($setup.isChinese ? '实现阶段' : 'Implementation stage'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.implementationStage), 1 /* TEXT */)
                ])
              ]),
              ($setup.taskStatus.suggestion?.summary)
                ? (_openBlock(), _createElementBlock("p", _hoisted_36, _toDisplayString($setup.taskStatus.suggestion.summary), 1 /* TEXT */))
                : ($setup.taskStatus.failure)
                  ? (_openBlock(), _createElementBlock("div", _hoisted_37, [
                      _createElementVNode("div", _hoisted_38, [
                        _createElementVNode("div", _hoisted_39, [
                          _createElementVNode("span", _hoisted_40, _toDisplayString($setup.isChinese ? '失败码' : 'Failure code'), 1 /* TEXT */),
                          _createElementVNode("strong", _hoisted_41, _toDisplayString($setup.taskStatus.failure.code), 1 /* TEXT */)
                        ]),
                        _createElementVNode("div", _hoisted_42, [
                          _createElementVNode("span", _hoisted_43, _toDisplayString($setup.isChinese ? '可重试' : 'Retryable'), 1 /* TEXT */),
                          _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.failure.retryable ? 'true' : 'false'), 1 /* TEXT */)
                        ]),
                        _createElementVNode("div", _hoisted_44, [
                          _createElementVNode("span", _hoisted_45, _toDisplayString($setup.isChinese ? '失败阶段' : 'Failed phase'), 1 /* TEXT */),
                          _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.failure.failedPhase), 1 /* TEXT */)
                        ]),
                        _createElementVNode("div", _hoisted_46, [
                          _createElementVNode("span", _hoisted_47, _toDisplayString($setup.isChinese ? '建议动作' : 'Suggested action'), 1 /* TEXT */),
                          _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.failure.suggestedAction), 1 /* TEXT */)
                        ])
                      ]),
                      _createElementVNode("p", _hoisted_48, _toDisplayString($setup.taskStatus.failure.message), 1 /* TEXT */)
                    ]))
                  : _createCommentVNode("v-if", true)
            ], 64 /* STABLE_FRAGMENT */))
          : _createCommentVNode("v-if", true),
        ($setup.compensationStatus)
          ? (_openBlock(), _createElementBlock("div", _hoisted_49, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", $setup.compensationDetected ? 'result-banner-success' : 'result-banner-danger'])
              }, [
                _createElementVNode("strong", _hoisted_50, _toDisplayString($setup.compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED'), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_51, _toDisplayString($setup.compensationStatus.status), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_52, [
                _createElementVNode("div", _hoisted_53, [
                  _createElementVNode("span", _hoisted_54, _toDisplayString($setup.isChinese ? '补偿前 pending' : 'Pending before'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_55, _toDisplayString($setup.queueStatsBefore?.pending ?? 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_56, [
                  _createElementVNode("span", _hoisted_57, _toDisplayString($setup.isChinese ? '补偿后 pending' : 'Pending after'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_58, _toDisplayString($setup.queueStatsAfter?.pending ?? 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_59, [
                  _createElementVNode("span", _hoisted_60, _toDisplayString($setup.isChinese ? 'pending 增量' : 'Pending delta'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_61, _toDisplayString($setup.queuePendingDelta), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_62, [
                  _createElementVNode("span", _hoisted_63, _toDisplayString($setup.isChinese ? 'total 增量' : 'Total delta'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_64, _toDisplayString($setup.queueTotalDelta), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_65, [
                  _createElementVNode("span", _hoisted_66, _toDisplayString($setup.isChinese ? '补偿状态查询' : 'Compensated status query'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.compensationStatus.currentPhase), 1 /* TEXT */)
                ])
              ])
            ]))
          : _createCommentVNode("v-if", true)
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-08940eb5'
__sfc__.render = render
__sfc__.__file = "src/views/optimization/AccelerationView.js"

export default __sfc__
