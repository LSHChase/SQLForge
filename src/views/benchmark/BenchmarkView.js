/* eslint-disable no-unused-vars */
import './BenchmarkView.css'

import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getBenchmarkReport,
  getBenchmarkTaskStatus,
  getGovernanceMessageStats,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  submitBenchmarkTask,
  waitForBenchmarkTask
} from '../../services/runtimeGateApi'


const __sfc__ = {
  __name: 'BenchmarkView',
  setup(__props, { expose: __expose }) {
  __expose();

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  taskType: 'COMPARISON',
  sqlText: 'SELECT * FROM orders'
})

const running = ref(false)
const lastScenario = ref('success')
const submitResult = ref(null)
const taskStatus = ref(null)
const compensationStatus = ref(null)
const report = ref(null)
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

const resetEvidence = () => {
  submitResult.value = null
  taskStatus.value = null
  compensationStatus.value = null
  report.value = null
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  errorMessage.value = ''
}

const applySuccessPreset = () => {
  form.taskType = 'COMPARISON'
  form.sqlText = 'SELECT * FROM orders'
}

const applyFailurePreset = () => {
  form.taskType = 'BASELINE'
  form.sqlText = 'SELECT * FROM orders /*FAIL_BENCHMARK*/'
}

const runSuccessFlow = async () => {
  applySuccessPreset()
  lastScenario.value = 'success'
  running.value = true
  resetEvidence()

  try {
    submitResult.value = await submitBenchmarkTask({
      ...form,
      taskContext: {
        priority: 'HIGH',
        targetEngines: ['HETU', 'HIVE'],
        concurrency: 16,
        durationSeconds: 300,
        rampUpSeconds: 30,
        readonlyRequired: true,
        shadowEnvironmentMode: 'REQUIRED'
      }
    })
    taskStatus.value = await waitForBenchmarkTask(submitResult.value.taskId, form.tenantId)

    if (taskStatus.value.reportId) {
      report.value = await getBenchmarkReport(taskStatus.value.reportId, form.tenantId)
    }
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
      requestPrefix: 'frontend-benchmark-governance-stats-before'
    })
    submitResult.value = await submitBenchmarkTask({
      ...form
    })
    taskStatus.value = await waitForBenchmarkTask(submitResult.value.taskId, form.tenantId)
    compensationStatus.value = await getBenchmarkTaskStatus(submitResult.value.taskId, form.tenantId, {
      requestPrefix: 'frontend-benchmark-compensation-status',
      tracePrefix: GOVERNANCE_COMPENSATION_TRACE_PREFIX
    })
    queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-benchmark-governance-stats-after'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const __returned__ = { t, locale, form, running, lastScenario, submitResult, taskStatus, compensationStatus, report, queueStatsBefore, queueStatsAfter, errorMessage, isChinese, queuePendingDelta, queueTotalDelta, compensationDetected, resetEvidence, applySuccessPreset, applyFailurePreset, runSuccessFlow, runFailureCompensationFlow, computed, reactive, ref, get useI18n() { return useI18n }, get formatRuntimeError() { return formatRuntimeError }, get getBenchmarkReport() { return getBenchmarkReport }, get getBenchmarkTaskStatus() { return getBenchmarkTaskStatus }, get getGovernanceMessageStats() { return getGovernanceMessageStats }, get GOVERNANCE_COMPENSATION_TRACE_PREFIX() { return GOVERNANCE_COMPENSATION_TRACE_PREFIX }, get submitBenchmarkTask() { return submitBenchmarkTask }, get waitForBenchmarkTask() { return waitForBenchmarkTask } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, resolveComponent as _resolveComponent, createVNode as _createVNode, createTextVNode as _createTextVNode, withCtx as _withCtx, openBlock as _openBlock, createElementBlock as _createElementBlock, createCommentVNode as _createCommentVNode, normalizeClass as _normalizeClass, Fragment as _Fragment } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "benchmark-flow-page"
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
const _hoisted_16 = { class: "request-note" }
const _hoisted_17 = { class: "action-row action-row-wrap" }
const _hoisted_18 = { class: "surface-card" }
const _hoisted_19 = { class: "section-heading" }
const _hoisted_20 = { class: "section-title" }
const _hoisted_21 = {
  key: 0,
  class: "empty-state"
}
const _hoisted_22 = {
  key: 1,
  class: "result-banner result-banner-danger",
  "data-testid": "benchmark-flow-error"
}
const _hoisted_23 = {
  key: 2,
  class: "evidence-grid"
}
const _hoisted_24 = { class: "evidence-item" }
const _hoisted_25 = { class: "evidence-label" }
const _hoisted_26 = { class: "evidence-item" }
const _hoisted_27 = { class: "evidence-label" }
const _hoisted_28 = { "data-testid": "benchmark-flow-status" }
const _hoisted_29 = { "data-testid": "benchmark-flow-report-id" }
const _hoisted_30 = { class: "evidence-grid" }
const _hoisted_31 = { class: "evidence-item" }
const _hoisted_32 = { class: "evidence-label" }
const _hoisted_33 = { class: "evidence-item" }
const _hoisted_34 = { class: "evidence-label" }
const _hoisted_35 = {
  key: 0,
  class: "trace-card"
}
const _hoisted_36 = { class: "evidence-grid" }
const _hoisted_37 = { class: "evidence-item" }
const _hoisted_38 = { class: "evidence-label" }
const _hoisted_39 = { "data-testid": "benchmark-flow-failure-code" }
const _hoisted_40 = { class: "evidence-item" }
const _hoisted_41 = { class: "evidence-label" }
const _hoisted_42 = { class: "result-copy" }
const _hoisted_43 = {
  key: 4,
  class: "report-card",
  "data-testid": "benchmark-flow-report"
}
const _hoisted_44 = { class: "evidence-grid" }
const _hoisted_45 = { class: "evidence-item" }
const _hoisted_46 = { class: "evidence-label" }
const _hoisted_47 = { class: "evidence-item" }
const _hoisted_48 = { class: "evidence-label" }
const _hoisted_49 = { class: "result-copy" }
const _hoisted_50 = {
  key: 5,
  class: "compensation-card"
}
const _hoisted_51 = { "data-testid": "benchmark-flow-compensation-indicator" }
const _hoisted_52 = { "data-testid": "benchmark-flow-compensation-status" }
const _hoisted_53 = { class: "evidence-grid" }
const _hoisted_54 = { class: "evidence-item" }
const _hoisted_55 = { class: "evidence-label" }
const _hoisted_56 = { "data-testid": "benchmark-flow-queue-pending-before" }
const _hoisted_57 = { class: "evidence-item" }
const _hoisted_58 = { class: "evidence-label" }
const _hoisted_59 = { "data-testid": "benchmark-flow-queue-pending-after" }
const _hoisted_60 = { class: "evidence-item" }
const _hoisted_61 = { class: "evidence-label" }
const _hoisted_62 = { "data-testid": "benchmark-flow-queue-pending-delta" }
const _hoisted_63 = { class: "evidence-item" }
const _hoisted_64 = { class: "evidence-label" }
const _hoisted_65 = { "data-testid": "benchmark-flow-queue-total-delta" }
const _hoisted_66 = { class: "evidence-item" }
const _hoisted_67 = { class: "evidence-label" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_input = _resolveComponent("el-input")
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[3] || (_cache[3] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "frontend runtime gate", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('benchmark.title')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('benchmark.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '该页从浏览器触发 benchmark-engine 成功链路与失败补偿链路。成功场景继续读取真实 JSON report，失败场景则检查 governance 队列 pending 增量。'
            : 'This page drives both the benchmark-engine success path and the failure-compensation path from the browser. The success flow reads the live JSON report, while the failure flow checks governance queue pending growth.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      _createElementVNode("article", _hoisted_7, [
        _createElementVNode("div", _hoisted_8, [
          _createElementVNode("div", null, [
            _cache[4] || (_cache[4] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "benchmark task", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_9, _toDisplayString($setup.isChinese ? '压测请求' : 'Benchmark request'), 1 /* TEXT */)
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
            _createVNode(_component_el_input, {
              modelValue: $setup.form.taskType,
              "onUpdate:modelValue": _cache[1] || (_cache[1] = $event => (($setup.form.taskType) = $event))
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_15, [
            _cache[5] || (_cache[5] = _createElementVNode("span", { class: "field-label" }, "SQL", -1 /* HOISTED */)),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.sqlText,
              "onUpdate:modelValue": _cache[2] || (_cache[2] = $event => (($setup.form.sqlText) = $event)),
              type: "textarea",
              rows: 6
            }, null, 8 /* PROPS */, ["modelValue"])
          ])
        ]),
        _createElementVNode("div", _hoisted_16, _toDisplayString($setup.isChinese
              ? '成功预置固定为 HETU/HIVE 只读对比，失败预置使用 FAIL_BENCHMARK 注入 worker 失败。'
              : 'The success preset runs readonly HETU/HIVE comparison; the failure preset injects FAIL_BENCHMARK to drive the worker failure path.'), 1 /* TEXT */),
        _createElementVNode("div", _hoisted_17, [
          _createVNode(_component_el_button, {
            type: "primary",
            loading: $setup.running && $setup.lastScenario === 'success',
            "data-testid": "benchmark-flow-submit",
            onClick: $setup.runSuccessFlow
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行成功任务' : 'Run success task'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            loading: $setup.running && $setup.lastScenario === 'failure',
            "data-testid": "benchmark-flow-submit-failure",
            onClick: $setup.runFailureCompensationFlow
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行失败恢复 + 补偿' : 'Run failure recovery + compensation'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"])
        ])
      ]),
      _createElementVNode("article", _hoisted_18, [
        _createElementVNode("div", _hoisted_19, [
          _createElementVNode("div", null, [
            _cache[6] || (_cache[6] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "runtime evidence", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_20, _toDisplayString($setup.isChinese ? '任务与报告结果' : 'Task and report evidence'), 1 /* TEXT */)
          ])
        ]),
        (!$setup.submitResult && !$setup.errorMessage)
          ? (_openBlock(), _createElementBlock("p", _hoisted_21, _toDisplayString($setup.isChinese
              ? '成功场景会展示 report 回写结果；失败场景会展示终态错误和审计补偿队列证据。'
              : 'The success path shows report write-back evidence; the failure path shows terminal errors and audit-compensation queue evidence.'), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.errorMessage)
          ? (_openBlock(), _createElementBlock("div", _hoisted_22, _toDisplayString($setup.errorMessage), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.submitResult)
          ? (_openBlock(), _createElementBlock("div", _hoisted_23, [
              _createElementVNode("div", _hoisted_24, [
                _createElementVNode("span", _hoisted_25, _toDisplayString($setup.isChinese ? '任务 ID' : 'Task ID'), 1 /* TEXT */),
                _createElementVNode("strong", null, _toDisplayString($setup.submitResult.taskId), 1 /* TEXT */)
              ]),
              _createElementVNode("div", _hoisted_26, [
                _createElementVNode("span", _hoisted_27, _toDisplayString($setup.isChinese ? '初始状态' : 'Initial status'), 1 /* TEXT */),
                _createElementVNode("strong", null, _toDisplayString($setup.submitResult.status), 1 /* TEXT */)
              ])
            ]))
          : _createCommentVNode("v-if", true),
        ($setup.taskStatus)
          ? (_openBlock(), _createElementBlock(_Fragment, { key: 3 }, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", $setup.taskStatus.status === 'SUCCEEDED' ? 'result-banner-success' : 'result-banner-warning'])
              }, [
                _createElementVNode("strong", _hoisted_28, _toDisplayString($setup.taskStatus.status), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_29, _toDisplayString($setup.taskStatus.reportId || '-'), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_30, [
                _createElementVNode("div", _hoisted_31, [
                  _createElementVNode("span", _hoisted_32, _toDisplayString($setup.isChinese ? '目标引擎' : 'Target engines'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.targetEngines?.join(' / ') || '-'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_33, [
                  _createElementVNode("span", _hoisted_34, _toDisplayString($setup.isChinese ? '只读要求' : 'Readonly required'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.readonlyRequired ? 'true' : 'false'), 1 /* TEXT */)
                ])
              ]),
              ($setup.taskStatus.error)
                ? (_openBlock(), _createElementBlock("div", _hoisted_35, [
                    _createElementVNode("div", _hoisted_36, [
                      _createElementVNode("div", _hoisted_37, [
                        _createElementVNode("span", _hoisted_38, _toDisplayString($setup.isChinese ? '失败码' : 'Failure code'), 1 /* TEXT */),
                        _createElementVNode("strong", _hoisted_39, _toDisplayString($setup.taskStatus.error.code), 1 /* TEXT */)
                      ]),
                      _createElementVNode("div", _hoisted_40, [
                        _createElementVNode("span", _hoisted_41, _toDisplayString($setup.isChinese ? '可重试' : 'Retryable'), 1 /* TEXT */),
                        _createElementVNode("strong", null, _toDisplayString($setup.taskStatus.error.retryable ? 'true' : 'false'), 1 /* TEXT */)
                      ])
                    ]),
                    _createElementVNode("p", _hoisted_42, _toDisplayString($setup.taskStatus.error.message), 1 /* TEXT */)
                  ]))
                : _createCommentVNode("v-if", true)
            ], 64 /* STABLE_FRAGMENT */))
          : _createCommentVNode("v-if", true),
        ($setup.report)
          ? (_openBlock(), _createElementBlock("div", _hoisted_43, [
              _createElementVNode("div", _hoisted_44, [
                _createElementVNode("div", _hoisted_45, [
                  _createElementVNode("span", _hoisted_46, _toDisplayString($setup.isChinese ? '裁决' : 'Verdict'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.report.verdict), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_47, [
                  _createElementVNode("span", _hoisted_48, _toDisplayString($setup.isChinese ? '返回格式' : 'Requested format'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.report.requestedFormat), 1 /* TEXT */)
                ])
              ]),
              _createElementVNode("p", _hoisted_49, _toDisplayString($setup.isChinese
                  ? `报告 ${$setup.report.reportId} 已通过真实接口返回，支持格式 ${$setup.report.availableFormats.join(', ')}。`
                  : `Report ${$setup.report.reportId} was returned by the live API with formats ${$setup.report.availableFormats.join(', ')}.`), 1 /* TEXT */)
            ]))
          : _createCommentVNode("v-if", true),
        ($setup.compensationStatus)
          ? (_openBlock(), _createElementBlock("div", _hoisted_50, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", $setup.compensationDetected ? 'result-banner-success' : 'result-banner-danger'])
              }, [
                _createElementVNode("strong", _hoisted_51, _toDisplayString($setup.compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED'), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_52, _toDisplayString($setup.compensationStatus.status), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_53, [
                _createElementVNode("div", _hoisted_54, [
                  _createElementVNode("span", _hoisted_55, _toDisplayString($setup.isChinese ? '补偿前 pending' : 'Pending before'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_56, _toDisplayString($setup.queueStatsBefore?.pending ?? 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_57, [
                  _createElementVNode("span", _hoisted_58, _toDisplayString($setup.isChinese ? '补偿后 pending' : 'Pending after'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_59, _toDisplayString($setup.queueStatsAfter?.pending ?? 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_60, [
                  _createElementVNode("span", _hoisted_61, _toDisplayString($setup.isChinese ? 'pending 增量' : 'Pending delta'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_62, _toDisplayString($setup.queuePendingDelta), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_63, [
                  _createElementVNode("span", _hoisted_64, _toDisplayString($setup.isChinese ? 'total 增量' : 'Total delta'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_65, _toDisplayString($setup.queueTotalDelta), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_66, [
                  _createElementVNode("span", _hoisted_67, _toDisplayString($setup.isChinese ? '补偿状态查询' : 'Compensated status query'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.compensationStatus.currentPhase), 1 /* TEXT */)
                ])
              ])
            ]))
          : _createCommentVNode("v-if", true)
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-12a63631'
__sfc__.render = render
__sfc__.__file = "src/views/benchmark/BenchmarkView.js"

export default __sfc__
