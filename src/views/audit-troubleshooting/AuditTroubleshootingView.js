/* eslint-disable no-unused-vars */
import './AuditTroubleshootingView.css'

import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  formatRuntimeError,
  getGovernanceMessageStats,
  getGovernanceTraceDetail,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  lookupGovernanceTraces,
  retryGovernanceFailedMessages
} from '../../services/runtimeGateApi'


const __sfc__ = {
  __name: 'AuditTroubleshootingView',
  setup(__props, { expose: __expose }) {
  __expose();

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a',
  remediationTenantId: 'system',
  traceId: '',
  taskId: '',
  reportId: '',
  windowStart: '',
  windowEnd: '',
  limit: 12
})

const loadingLookup = ref(false)
const loadingDetail = ref(false)
const loadingStats = ref(false)
const retrying = ref(false)
const lookupResults = ref([])
const detail = ref(null)
const stats = ref(null)
const statsBeforeRetry = ref(null)
const statsAfterRetry = ref(null)
const retryResult = ref(null)
const errorMessage = ref('')
const hasMore = ref(false)
const nextCursor = ref('')
const activeFilters = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const activeTraceId = computed(() => detail.value?.traceId || '')
const matchedCount = computed(() => lookupResults.value.length)
const compensationCount = computed(() =>
  lookupResults.value.filter(trace => isCompensationTrace(trace.traceId)).length
)
const reportLinkedCount = computed(() =>
  lookupResults.value.filter(trace => hasDisplayValue(trace.reportId) || (trace.exportRecordCount || 0) > 0).length
)
const searchCriteria = computed(() =>
  [
    {
      key: 'traceId',
      label: isChinese.value ? 'Trace 反查' : 'Trace lookup',
      value: form.traceId
    },
    {
      key: 'taskId',
      label: isChinese.value ? 'Task 反查' : 'Task lookup',
      value: form.taskId
    },
    {
      key: 'reportId',
      label: isChinese.value ? 'Report 反查' : 'Report lookup',
      value: form.reportId
    }
  ].filter(item => hasDisplayValue(item.value))
)
const selectedSummary = computed(() =>
  lookupResults.value.find(trace => trace.traceId === activeTraceId.value) || null
)
const failureType = computed(() => {
  if (!detail.value) {
    return '-'
  }
  if (detail.value.serviceCode === 'QUERY_EXECUTION' && detail.value.degraded === true) {
    return 'QUERY_DEGRADED_RECOVERY'
  }
  if (detail.value.serviceCode === 'SQL_OPTIMIZATION' && detail.value.latestStatus === 'FAILED') {
    return 'OPTIMIZATION_FAILURE'
  }
  if (detail.value.serviceCode === 'BENCHMARK_ENGINE' && detail.value.latestStatus === 'FAILED') {
    return 'BENCHMARK_FAILURE'
  }
  if ((detail.value.nonSuccessEventCount || 0) > 0) {
    return 'AUDIT_FAILURE_CHAIN'
  }
  return 'STEADY_STATE'
})
const compensationState = computed(() => {
  if (!detail.value) {
    return '-'
  }
  if (isCompensationTrace(detail.value.traceId)) {
    return 'COMPENSATION_TRACE'
  }
  if (compensationCount.value > 0) {
    return 'COMPENSATED_VISIBLE'
  }
  return 'NOT_VISIBLE'
})
const writeBackState = computed(() => {
  if (!detail.value) {
    return '-'
  }
  if ((detail.value.exportRecordCount || 0) > 0 || hasDisplayValue(detail.value.reportId)) {
    return 'WRITEBACK_VISIBLE'
  }
  return 'WRITEBACK_PENDING'
})
const queueImpact = computed(() => {
  if (!stats.value) {
    return '-'
  }
  if ((stats.value.failed || 0) > 0) {
    return 'FAILED_BACKLOG'
  }
  if ((stats.value.pending || 0) > 0) {
    return 'PENDING_BACKLOG'
  }
  return 'STEADY_QUEUE'
})
const failedDelta = computed(() => {
  if (!statsBeforeRetry.value || !statsAfterRetry.value) {
    return 0
  }
  return statsBeforeRetry.value.failed - statsAfterRetry.value.failed
})
const retryImproved = computed(() => failedDelta.value >= 1 || (retryResult.value?.retriedCount || 0) >= 1)
const acceptanceState = computed(() => {
  if (retryResult.value) {
    return retryImproved.value ? 'REPAIRED' : 'RETRY_ACCEPTED'
  }
  if (compensationState.value === 'COMPENSATION_TRACE' || writeBackState.value === 'WRITEBACK_VISIBLE') {
    return 'READY_FOR_ACCEPTANCE'
  }
  if (queueImpact.value === 'FAILED_BACKLOG') {
    return 'REMEDIATION_REQUIRED'
  }
  return 'UNDER_INVESTIGATION'
})
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
const decisionCards = computed(() => [
  {
    key: 'failure-type',
    label: isChinese.value ? '失败类型' : 'Failure type',
    value: failureType.value
  },
  {
    key: 'compensation-state',
    label: isChinese.value ? '补偿状态' : 'Compensation state',
    value: compensationState.value
  },
  {
    key: 'writeback-state',
    label: isChinese.value ? '回写状态' : 'Write-back state',
    value: writeBackState.value
  },
  {
    key: 'queue-impact',
    label: isChinese.value ? '队列影响' : 'Queue impact',
    value: queueImpact.value
  },
  {
    key: 'acceptance-state',
    label: isChinese.value ? '验收信号' : 'Acceptance state',
    value: acceptanceState.value
  }
])

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

const isCompensationTrace = traceId => String(traceId || '').startsWith(GOVERNANCE_COMPENSATION_TRACE_PREFIX)

const normalizeFilters = () => ({
  traceId: String(form.traceId || '').trim(),
  taskId: String(form.taskId || '').trim(),
  reportId: String(form.reportId || '').trim(),
  windowStart: String(form.windowStart || '').trim(),
  windowEnd: String(form.windowEnd || '').trim()
})

const syncRouteQuery = query => {
  router.replace({
    path: ROUTE_PATHS.auditTroubleshooting,
    query
  })
}

const buildDrillQuery = source => {
  const filters = activeFilters.value
    ? activeFilters.value
    : {
        traceId: source?.traceId,
        taskId: source?.taskId,
        reportId: source?.reportId
      }
  const query = {
    tenantId: form.tenantId,
    remediationTenantId: form.remediationTenantId,
    limit: String(form.limit)
  }
  if (hasDisplayValue(filters?.traceId)) {
    query.traceId = String(filters.traceId)
  }
  if (hasDisplayValue(filters?.taskId)) {
    query.taskId = String(filters.taskId)
  }
  if (hasDisplayValue(filters?.reportId)) {
    query.reportId = String(filters.reportId)
  }
  if (hasDisplayValue(filters?.windowStart)) {
    query.windowStart = String(filters.windowStart)
  }
  if (hasDisplayValue(filters?.windowEnd)) {
    query.windowEnd = String(filters.windowEnd)
  }
  return query
}

const loadQueueStats = async () => {
  loadingStats.value = true
  try {
    stats.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingStats.value = false
  }
}

const loadTraceDetail = async traceId => {
  if (!traceId) {
    detail.value = null
    return
  }

  loadingDetail.value = true
  errorMessage.value = ''

  try {
    detail.value = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-audit-troubleshooting-trace-detail'
    })
  } catch (error) {
    detail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingDetail.value = false
  }
}

const applyLookupPage = async (pageResponse, append = false) => {
  const items = Array.isArray(pageResponse?.items) ? pageResponse.items : []
  lookupResults.value = append ? [...lookupResults.value, ...items] : items
  hasMore.value = Boolean(pageResponse?.hasMore)
  nextCursor.value = pageResponse?.nextCursor || ''

  if (!append) {
    await loadTraceDetail(lookupResults.value[0]?.traceId || '')
  }
}

const runLookup = async () => {
  if (!hasDisplayValue(form.traceId) && !hasDisplayValue(form.taskId) && !hasDisplayValue(form.reportId)) {
    errorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项后再执行处置决策反查。'
      : 'Enter at least one of traceId, taskId, or reportId before running the remediation lookup.'
    lookupResults.value = []
    detail.value = null
    hasMore.value = false
    nextCursor.value = ''
    activeFilters.value = null
    return
  }

  loadingLookup.value = true
  errorMessage.value = ''
  activeFilters.value = normalizeFilters()
  syncRouteQuery({
    tenantId: form.tenantId,
    remediationTenantId: form.remediationTenantId,
    limit: String(form.limit),
    ...activeFilters.value
  })

  try {
    const lookupPage = await lookupGovernanceTraces(form.tenantId, activeFilters.value, form.limit, {
      requestPrefix: 'frontend-audit-troubleshooting-lookups'
    })
    await Promise.all([
      applyLookupPage(lookupPage, false),
      loadQueueStats()
    ])
  } catch (error) {
    lookupResults.value = []
    detail.value = null
    hasMore.value = false
    nextCursor.value = ''
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingLookup.value = false
  }
}

const loadMoreResults = async () => {
  if (!hasMore.value || !nextCursor.value || !activeFilters.value) {
    return
  }

  loadingLookup.value = true
  errorMessage.value = ''

  try {
    const lookupPage = await lookupGovernanceTraces(
      form.tenantId,
      {
        ...activeFilters.value,
        cursor: nextCursor.value
      },
      form.limit,
      {
        requestPrefix: 'frontend-audit-troubleshooting-lookups-more'
      }
    )
    await applyLookupPage(lookupPage, true)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingLookup.value = false
  }
}

const retryFailedMessages = async () => {
  retrying.value = true
  errorMessage.value = ''
  retryResult.value = null

  try {
    statsBeforeRetry.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats-before-retry'
    })
    retryResult.value = await retryGovernanceFailedMessages(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-retry'
    })
    statsAfterRetry.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats-after-retry'
    })
    stats.value = statsAfterRetry.value
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    retrying.value = false
  }
}

const clearLookup = async () => {
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  form.windowStart = ''
  form.windowEnd = ''
  lookupResults.value = []
  detail.value = null
  errorMessage.value = ''
  hasMore.value = false
  nextCursor.value = ''
  activeFilters.value = null
  retryResult.value = null
  statsBeforeRetry.value = null
  statsAfterRetry.value = null
  syncRouteQuery({
    tenantId: form.tenantId,
    remediationTenantId: form.remediationTenantId,
    limit: String(form.limit)
  })
  await loadQueueStats()
}

const openSystemBacklog = () => {
  router.push('/system')
}

const openRepairEvidence = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.repairEvidence,
    query: buildDrillQuery(source)
  })
}

const openParseRecord = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: buildDrillQuery(source)
  })
}

onMounted(async () => {
  if (hasDisplayValue(route.query.tenantId)) {
    form.tenantId = String(route.query.tenantId)
  }
  if (hasDisplayValue(route.query.remediationTenantId)) {
    form.remediationTenantId = String(route.query.remediationTenantId)
  }
  if (hasDisplayValue(route.query.limit)) {
    form.limit = Number(route.query.limit) || 12
  }
  form.traceId = String(route.query.traceId || '')
  form.taskId = String(route.query.taskId || '')
  form.reportId = String(route.query.reportId || '')
  form.windowStart = String(route.query.windowStart || '')
  form.windowEnd = String(route.query.windowEnd || '')

  await loadQueueStats()
  if (form.traceId || form.taskId || form.reportId) {
    await runLookup()
  }
})

const __returned__ = { t, locale, route, router, form, loadingLookup, loadingDetail, loadingStats, retrying, lookupResults, detail, stats, statsBeforeRetry, statsAfterRetry, retryResult, errorMessage, hasMore, nextCursor, activeFilters, isChinese, activeTraceId, matchedCount, compensationCount, reportLinkedCount, searchCriteria, selectedSummary, failureType, compensationState, writeBackState, queueImpact, failedDelta, retryImproved, acceptanceState, queueCards, decisionCards, hasDisplayValue, displayValue, formatTimestamp, isCompensationTrace, normalizeFilters, syncRouteQuery, buildDrillQuery, loadQueueStats, loadTraceDetail, applyLookupPage, runLookup, loadMoreResults, retryFailedMessages, clearLookup, openSystemBacklog, openRepairEvidence, openParseRecord, computed, onMounted, reactive, ref, get useI18n() { return useI18n }, get useRoute() { return useRoute }, get useRouter() { return useRouter }, get ROUTE_PATHS() { return ROUTE_PATHS }, get formatRuntimeError() { return formatRuntimeError }, get getGovernanceMessageStats() { return getGovernanceMessageStats }, get getGovernanceTraceDetail() { return getGovernanceTraceDetail }, get GOVERNANCE_COMPENSATION_TRACE_PREFIX() { return GOVERNANCE_COMPENSATION_TRACE_PREFIX }, get lookupGovernanceTraces() { return lookupGovernanceTraces }, get retryGovernanceFailedMessages() { return retryGovernanceFailedMessages } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, resolveComponent as _resolveComponent, createVNode as _createVNode, createTextVNode as _createTextVNode, withCtx as _withCtx, openBlock as _openBlock, createBlock as _createBlock, createCommentVNode as _createCommentVNode, renderList as _renderList, Fragment as _Fragment, createElementBlock as _createElementBlock, normalizeClass as _normalizeClass } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "audit-troubleshooting-page"
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
const _hoisted_18 = { class: "field-label" }
const _hoisted_19 = { class: "field-block" }
const _hoisted_20 = { class: "field-label" }
const _hoisted_21 = { class: "field-block" }
const _hoisted_22 = { class: "field-label" }
const _hoisted_23 = { class: "action-row action-row-wrap" }
const _hoisted_24 = { class: "lookup-chip-list" }
const _hoisted_25 = {
  key: 0,
  class: "lookup-chip lookup-chip-muted"
}
const _hoisted_26 = { class: "summary-card-grid" }
const _hoisted_27 = { class: "summary-card" }
const _hoisted_28 = { class: "summary-card-label" }
const _hoisted_29 = { "data-testid": "audit-troubleshooting-match-count" }
const _hoisted_30 = { class: "summary-card summary-card-warning" }
const _hoisted_31 = { class: "summary-card-label" }
const _hoisted_32 = { "data-testid": "audit-troubleshooting-compensation-count" }
const _hoisted_33 = { class: "summary-card" }
const _hoisted_34 = { class: "summary-card-label" }
const _hoisted_35 = { "data-testid": "audit-troubleshooting-report-count" }
const _hoisted_36 = { class: "queue-card-grid" }
const _hoisted_37 = { class: "queue-card-label" }
const _hoisted_38 = ["data-testid"]
const _hoisted_39 = {
  key: 0,
  class: "result-banner result-banner-warning",
  "data-testid": "audit-troubleshooting-has-more"
}
const _hoisted_40 = {
  key: 1,
  class: "result-banner result-banner-danger",
  "data-testid": "audit-troubleshooting-error"
}
const _hoisted_41 = {
  key: 2,
  class: "empty-state"
}
const _hoisted_42 = { class: "trace-list" }
const _hoisted_43 = ["onClick"]
const _hoisted_44 = { class: "trace-item-header" }
const _hoisted_45 = { class: "trace-item-service sqlforge-code-label" }
const _hoisted_46 = { class: "trace-item-meta" }
const _hoisted_47 = { class: "surface-card" }
const _hoisted_48 = { class: "section-heading" }
const _hoisted_49 = { class: "section-title" }
const _hoisted_50 = {
  key: 0,
  class: "empty-state"
}
const _hoisted_51 = { "data-testid": "audit-troubleshooting-detail-trace-id" }
const _hoisted_52 = { "data-testid": "audit-troubleshooting-acceptance-banner" }
const _hoisted_53 = { class: "decision-grid" }
const _hoisted_54 = { class: "decision-card-label" }
const _hoisted_55 = ["data-testid"]
const _hoisted_56 = { class: "action-row action-row-wrap" }
const _hoisted_57 = { class: "evidence-grid" }
const _hoisted_58 = { class: "evidence-item" }
const _hoisted_59 = { class: "evidence-label" }
const _hoisted_60 = { "data-testid": "audit-troubleshooting-detail-service-code" }
const _hoisted_61 = { class: "evidence-item" }
const _hoisted_62 = { class: "evidence-label" }
const _hoisted_63 = { "data-testid": "audit-troubleshooting-detail-task-id" }
const _hoisted_64 = { class: "evidence-item" }
const _hoisted_65 = { class: "evidence-label" }
const _hoisted_66 = { "data-testid": "audit-troubleshooting-detail-report-id" }
const _hoisted_67 = { class: "evidence-item" }
const _hoisted_68 = { class: "evidence-label" }
const _hoisted_69 = {
  key: 0,
  class: "retry-summary-grid"
}
const _hoisted_70 = { class: "evidence-item" }
const _hoisted_71 = { class: "evidence-label" }
const _hoisted_72 = { "data-testid": "audit-troubleshooting-retry-status" }
const _hoisted_73 = { class: "evidence-item" }
const _hoisted_74 = { class: "evidence-label" }
const _hoisted_75 = { "data-testid": "audit-troubleshooting-retry-count" }
const _hoisted_76 = { class: "evidence-item" }
const _hoisted_77 = { class: "evidence-label" }
const _hoisted_78 = { "data-testid": "audit-troubleshooting-failed-delta" }
const _hoisted_79 = { class: "evidence-item" }
const _hoisted_80 = { class: "evidence-label" }
const _hoisted_81 = { "data-testid": "audit-troubleshooting-repair-outcome" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_input = _resolveComponent("el-input")
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[6] || (_cache[6] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "frontend runtime gate", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('auditTroubleshooting.title')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('auditTroubleshooting.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '该页把 trace/task/report 取证结果与治理消息队列影响合并成处置决策视图，支持真实 retry、跳转 backlog、回到历史诊断与打开修复证据。'
            : 'This page merges trace/task/report forensics with governance queue impact into one remediation decision view, exposing real retry, backlog drill-through, parse-record navigation, and repair evidence actions.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      _createElementVNode("article", _hoisted_7, [
        _createElementVNode("div", _hoisted_8, [
          _createElementVNode("div", null, [
            _cache[7] || (_cache[7] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "decision lookup", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_9, _toDisplayString($setup.isChinese ? '故障范围、队列影响与决策输入' : 'Failure scope, queue impact and decision inputs'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_10, [
          _createElementVNode("label", _hoisted_11, [
            _createElementVNode("span", _hoisted_12, _toDisplayString($setup.isChinese ? '业务租户' : 'Business tenant'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.tenantId,
              "onUpdate:modelValue": _cache[0] || (_cache[0] = $event => (($setup.form.tenantId) = $event)),
              "data-testid": "audit-troubleshooting-tenant-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_13, [
            _createElementVNode("span", _hoisted_14, _toDisplayString($setup.isChinese ? '治理租户' : 'Governance tenant'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.remediationTenantId,
              "onUpdate:modelValue": _cache[1] || (_cache[1] = $event => (($setup.form.remediationTenantId) = $event)),
              "data-testid": "audit-troubleshooting-remediation-tenant-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_15, [
            _createElementVNode("span", _hoisted_16, _toDisplayString($setup.isChinese ? '返回数量' : 'Lookup limit'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.limit,
              "onUpdate:modelValue": _cache[2] || (_cache[2] = $event => (($setup.form.limit) = $event)),
              "data-testid": "audit-troubleshooting-limit"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_17, [
            _createElementVNode("span", _hoisted_18, _toDisplayString($setup.isChinese ? 'Trace ID' : 'Trace ID'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.traceId,
              "onUpdate:modelValue": _cache[3] || (_cache[3] = $event => (($setup.form.traceId) = $event)),
              "data-testid": "audit-troubleshooting-trace-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_19, [
            _createElementVNode("span", _hoisted_20, _toDisplayString($setup.isChinese ? 'Task ID' : 'Task ID'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.taskId,
              "onUpdate:modelValue": _cache[4] || (_cache[4] = $event => (($setup.form.taskId) = $event)),
              "data-testid": "audit-troubleshooting-task-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_21, [
            _createElementVNode("span", _hoisted_22, _toDisplayString($setup.isChinese ? 'Report ID' : 'Report ID'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.reportId,
              "onUpdate:modelValue": _cache[5] || (_cache[5] = $event => (($setup.form.reportId) = $event)),
              "data-testid": "audit-troubleshooting-report-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ])
        ]),
        _createElementVNode("div", _hoisted_23, [
          _createVNode(_component_el_button, {
            type: "primary",
            loading: $setup.loadingLookup,
            "data-testid": "audit-troubleshooting-run-lookup",
            onClick: $setup.runLookup
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行处置反查' : 'Run remediation lookup'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            loading: $setup.loadingStats,
            "data-testid": "audit-troubleshooting-refresh-queue",
            onClick: $setup.loadQueueStats
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '刷新队列影响' : 'Refresh queue impact'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            "data-testid": "audit-troubleshooting-clear-lookup",
            onClick: $setup.clearLookup
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '清空条件' : 'Clear criteria'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }),
          ($setup.hasMore)
            ? (_openBlock(), _createBlock(_component_el_button, {
                key: 0,
                loading: $setup.loadingLookup,
                "data-testid": "audit-troubleshooting-load-more",
                onClick: $setup.loadMoreResults
              }, {
                default: _withCtx(() => [
                  _createTextVNode(_toDisplayString($setup.isChinese ? '加载更早证据' : 'Load older evidence'), 1 /* TEXT */)
                ]),
                _: 1 /* STABLE */
              }, 8 /* PROPS */, ["loading"]))
            : _createCommentVNode("v-if", true)
        ]),
        _createElementVNode("div", _hoisted_24, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.searchCriteria, (item) => {
            return (_openBlock(), _createElementBlock("span", {
              key: item.key,
              class: "lookup-chip"
            }, _toDisplayString(item.label) + ": " + _toDisplayString(item.value), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */)),
          (!$setup.searchCriteria.length)
            ? (_openBlock(), _createElementBlock("span", _hoisted_25, _toDisplayString($setup.isChinese
                ? '输入 trace / task / report 后执行处置决策反查。'
                : 'Enter a trace, task, or report id and then run the remediation lookup.'), 1 /* TEXT */))
            : _createCommentVNode("v-if", true)
        ]),
        _createElementVNode("div", _hoisted_26, [
          _createElementVNode("article", _hoisted_27, [
            _createElementVNode("span", _hoisted_28, _toDisplayString($setup.isChinese ? '命中 trace' : 'Matched traces'), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_29, _toDisplayString($setup.matchedCount), 1 /* TEXT */)
          ]),
          _createElementVNode("article", _hoisted_30, [
            _createElementVNode("span", _hoisted_31, _toDisplayString($setup.isChinese ? '补偿 trace' : 'Compensation traces'), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_32, _toDisplayString($setup.compensationCount), 1 /* TEXT */)
          ]),
          _createElementVNode("article", _hoisted_33, [
            _createElementVNode("span", _hoisted_34, _toDisplayString($setup.isChinese ? '报告回写链' : 'Report-linked traces'), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_35, _toDisplayString($setup.reportLinkedCount), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_36, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.queueCards, (card) => {
            return (_openBlock(), _createElementBlock("article", {
              key: card.key,
              class: "queue-card"
            }, [
              _createElementVNode("span", _hoisted_37, _toDisplayString(card.label), 1 /* TEXT */),
              _createElementVNode("strong", {
                class: "queue-card-value",
                "data-testid": `audit-troubleshooting-queue-${card.key}`
              }, _toDisplayString(card.value), 9 /* TEXT, PROPS */, _hoisted_38)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ]),
        ($setup.hasMore)
          ? (_openBlock(), _createElementBlock("div", _hoisted_39, [
              _createElementVNode("strong", null, _toDisplayString($setup.isChinese ? '仍有更早处置链' : 'Older remediation chains available'), 1 /* TEXT */),
              _createElementVNode("span", null, _toDisplayString($setup.nextCursor || '-'), 1 /* TEXT */)
            ]))
          : _createCommentVNode("v-if", true),
        ($setup.errorMessage)
          ? (_openBlock(), _createElementBlock("div", _hoisted_40, _toDisplayString($setup.errorMessage), 1 /* TEXT */))
          : (!$setup.lookupResults.length)
            ? (_openBlock(), _createElementBlock("p", _hoisted_41, _toDisplayString($setup.isChinese
              ? '命中结果会展示故障链与队列影响，并提供处置入口。'
              : 'Matched chains render here with queue impact and remediation actions.'), 1 /* TEXT */))
            : _createCommentVNode("v-if", true),
        _createElementVNode("div", _hoisted_42, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.lookupResults, (trace) => {
            return (_openBlock(), _createElementBlock("button", {
              key: trace.traceId,
              type: "button",
              class: _normalizeClass(["trace-item", { 'trace-item-active': $setup.activeTraceId === trace.traceId }]),
              "data-testid": "audit-troubleshooting-result-item",
              onClick: $event => ($setup.loadTraceDetail(trace.traceId))
            }, [
              _createElementVNode("div", _hoisted_44, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_45, _toDisplayString(trace.serviceCode || '-'), 1 /* TEXT */),
                  _createElementVNode("h3", null, _toDisplayString(trace.resourceId || trace.traceId), 1 /* TEXT */)
                ]),
                _createElementVNode("span", {
                  class: _normalizeClass(["trace-status-pill", 
                  trace.latestStatus === 'SUCCESS' || trace.latestStatus === 'SUCCEEDED'
                    ? 'trace-status-success'
                    : 'trace-status-warning'
                ])
                }, _toDisplayString(trace.latestStatus || '-'), 3 /* TEXT, CLASS */)
              ]),
              _createElementVNode("p", _hoisted_46, _toDisplayString(trace.traceId) + " · " + _toDisplayString($setup.formatTimestamp(trace.lastSeenAt)), 1 /* TEXT */)
            ], 10 /* CLASS, PROPS */, _hoisted_43))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("article", _hoisted_47, [
        _createElementVNode("div", _hoisted_48, [
          _createElementVNode("div", null, [
            _cache[8] || (_cache[8] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "remediation decision", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_49, _toDisplayString($setup.isChinese ? '处置动作与验收信号' : 'Remediation actions and acceptance signals'), 1 /* TEXT */)
          ])
        ]),
        (!$setup.detail && !$setup.errorMessage)
          ? (_openBlock(), _createElementBlock("p", _hoisted_50, _toDisplayString($setup.isChinese
              ? '选择左侧命中 trace 后，这里会显示失败类型、补偿状态、回写状态和验收信号。'
              : 'After you select a matched trace, the failure type, compensation state, write-back state, and acceptance signals render here.'), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.detail)
          ? (_openBlock(), _createElementBlock(_Fragment, { key: 1 }, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", 
              $setup.acceptanceState === 'REPAIRED' || $setup.acceptanceState === 'READY_FOR_ACCEPTANCE'
                ? 'result-banner-success'
                : 'result-banner-warning'
            ])
              }, [
                _createElementVNode("strong", _hoisted_51, _toDisplayString($setup.detail.traceId), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_52, _toDisplayString($setup.acceptanceState), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_53, [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.decisionCards, (card) => {
                  return (_openBlock(), _createElementBlock("article", {
                    key: card.key,
                    class: "decision-card"
                  }, [
                    _createElementVNode("span", _hoisted_54, _toDisplayString(card.label), 1 /* TEXT */),
                    _createElementVNode("strong", {
                      "data-testid": `audit-troubleshooting-${card.key}`
                    }, _toDisplayString(card.value), 9 /* TEXT, PROPS */, _hoisted_55)
                  ]))
                }), 128 /* KEYED_FRAGMENT */))
              ]),
              _createElementVNode("div", _hoisted_56, [
                _createVNode(_component_el_button, {
                  type: "primary",
                  loading: $setup.retrying,
                  "data-testid": "audit-troubleshooting-retry",
                  onClick: $setup.retryFailedMessages
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.isChinese ? '重试失败消息' : 'Retry failed messages'), 1 /* TEXT */)
                  ]),
                  _: 1 /* STABLE */
                }, 8 /* PROPS */, ["loading"]),
                _createVNode(_component_el_button, {
                  "data-testid": "audit-troubleshooting-open-system",
                  onClick: $setup.openSystemBacklog
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.isChinese ? '打开治理 backlog' : 'Open governance backlog'), 1 /* TEXT */)
                  ]),
                  _: 1 /* STABLE */
                }),
                _createVNode(_component_el_button, {
                  "data-testid": "audit-troubleshooting-open-repair-evidence",
                  onClick: $setup.openRepairEvidence
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.isChinese ? '打开修复证据' : 'Open repair evidence'), 1 /* TEXT */)
                  ]),
                  _: 1 /* STABLE */
                }),
                _createVNode(_component_el_button, {
                  "data-testid": "audit-troubleshooting-open-parse-record",
                  onClick: $setup.openParseRecord
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.isChinese ? '回到历史诊断' : 'Back to parse record'), 1 /* TEXT */)
                  ]),
                  _: 1 /* STABLE */
                })
              ]),
              _createElementVNode("div", _hoisted_57, [
                _createElementVNode("div", _hoisted_58, [
                  _createElementVNode("span", _hoisted_59, _toDisplayString($setup.isChinese ? '服务编码' : 'Service code'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_60, _toDisplayString($setup.detail.serviceCode || '-'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_61, [
                  _createElementVNode("span", _hoisted_62, _toDisplayString($setup.isChinese ? '任务 ID' : 'Task ID'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_63, _toDisplayString($setup.displayValue($setup.detail.taskId)), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_64, [
                  _createElementVNode("span", _hoisted_65, _toDisplayString($setup.isChinese ? '报告 ID' : 'Report ID'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_66, _toDisplayString($setup.displayValue($setup.detail.reportId)), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_67, [
                  _createElementVNode("span", _hoisted_68, _toDisplayString($setup.isChinese ? '最后发生时间' : 'Last seen at'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.formatTimestamp($setup.detail.lastSeenAt)), 1 /* TEXT */)
                ])
              ]),
              ($setup.retryResult)
                ? (_openBlock(), _createElementBlock("div", _hoisted_69, [
                    _createElementVNode("div", _hoisted_70, [
                      _createElementVNode("span", _hoisted_71, _toDisplayString($setup.isChinese ? '重试状态' : 'Retry status'), 1 /* TEXT */),
                      _createElementVNode("strong", _hoisted_72, _toDisplayString($setup.retryResult.status), 1 /* TEXT */)
                    ]),
                    _createElementVNode("div", _hoisted_73, [
                      _createElementVNode("span", _hoisted_74, _toDisplayString($setup.isChinese ? '重试数量' : 'Retried count'), 1 /* TEXT */),
                      _createElementVNode("strong", _hoisted_75, _toDisplayString($setup.retryResult.retriedCount), 1 /* TEXT */)
                    ]),
                    _createElementVNode("div", _hoisted_76, [
                      _createElementVNode("span", _hoisted_77, _toDisplayString($setup.isChinese ? 'failed 降幅' : 'Failed delta'), 1 /* TEXT */),
                      _createElementVNode("strong", _hoisted_78, _toDisplayString($setup.failedDelta), 1 /* TEXT */)
                    ]),
                    _createElementVNode("div", _hoisted_79, [
                      _createElementVNode("span", _hoisted_80, _toDisplayString($setup.isChinese ? '修复结果' : 'Repair outcome'), 1 /* TEXT */),
                      _createElementVNode("strong", _hoisted_81, _toDisplayString($setup.retryImproved ? 'REPAIRED' : 'RETRY_ACCEPTED'), 1 /* TEXT */)
                    ])
                  ]))
                : _createCommentVNode("v-if", true)
            ], 64 /* STABLE_FRAGMENT */))
          : _createCommentVNode("v-if", true)
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-a4ee5606'
__sfc__.render = render
__sfc__.__file = "src/views/audit-troubleshooting/AuditTroubleshootingView.js"

export default __sfc__
