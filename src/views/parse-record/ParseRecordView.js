/* eslint-disable no-unused-vars */
import './ParseRecordView.css'

import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  formatRuntimeError,
  getGovernanceTraceDetail,
  getGovernanceTraceSummaries,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'


const __sfc__ = {
  __name: 'ParseRecordView',
  setup(__props, { expose: __expose }) {
  __expose();

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a',
  traceId: '',
  taskId: '',
  reportId: '',
  windowStart: '',
  windowEnd: '',
  limit: 12
})

const loadingList = ref(false)
const loadingLookup = ref(false)
const loadingDetail = ref(false)
const recentTraces = ref([])
const lookupResults = ref([])
const detail = ref(null)
const errorMessage = ref('')
const hasMore = ref(false)
const nextCursor = ref('')
const activeFilters = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const displayedTraces = computed(() => (activeFilters.value ? lookupResults.value : recentTraces.value))
const displayedCount = computed(() => displayedTraces.value.length)
const recentCount = computed(() => recentTraces.value.length)
const nonSuccessCount = computed(() =>
  displayedTraces.value.filter(trace => {
    const status = String(trace.latestStatus || '').toUpperCase()
    return (trace.nonSuccessEventCount || 0) > 0 || (status && status !== 'SUCCESS' && status !== 'SUCCEEDED')
  }).length
)
const hasLookupCriteria = computed(
  () => hasDisplayValue(form.traceId) || hasDisplayValue(form.taskId) || hasDisplayValue(form.reportId)
)
const pageMode = computed(() => {
  if (!activeFilters.value) {
    return isChinese.value ? 'RECENT' : 'RECENT'
  }
  if (hasDisplayValue(activeFilters.value.traceId)) {
    return 'TRACE'
  }
  if (hasDisplayValue(activeFilters.value.taskId)) {
    return 'TASK'
  }
  if (hasDisplayValue(activeFilters.value.reportId)) {
    return 'REPORT'
  }
  return 'INDEXED'
})
const lookupCriteria = computed(() =>
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
  displayedTraces.value.find(trace => trace.traceId === detail.value?.traceId) || null
)
const detailHighlights = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    {
      key: 'taskId',
      label: isChinese.value ? '任务 ID' : 'Task ID',
      value: detail.value.taskId
    },
    {
      key: 'reportId',
      label: isChinese.value ? '报告 ID' : 'Report ID',
      value: detail.value.reportId
    },
    {
      key: 'sqlFingerprint',
      label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint',
      value: detail.value.sqlFingerprint
    },
    {
      key: 'errorCode',
      label: isChinese.value ? '错误码' : 'Error code',
      value: detail.value.errorCode
    },
    {
      key: 'targetEngine',
      label: isChinese.value ? '目标引擎' : 'Target engine',
      value: detail.value.targetEngine
    },
    {
      key: 'degraded',
      label: isChinese.value ? '降级执行' : 'Degraded',
      value: typeof detail.value.degraded === 'boolean' ? String(detail.value.degraded) : ''
    }
  ].filter(item => displayValue(item.value) !== '-')
})

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

const normalizeFilters = () => ({
  traceId: String(form.traceId || '').trim(),
  taskId: String(form.taskId || '').trim(),
  reportId: String(form.reportId || '').trim(),
  windowStart: String(form.windowStart || '').trim(),
  windowEnd: String(form.windowEnd || '').trim()
})

const syncRouteQuery = query => {
  router.replace({
    path: ROUTE_PATHS.parseRecord,
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

const loadTraceDetail = async (traceId, synchronizeInput = false) => {
  if (!traceId) {
    detail.value = null
    return
  }

  loadingDetail.value = true
  errorMessage.value = ''
  if (synchronizeInput) {
    form.traceId = traceId
  }

  try {
    detail.value = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-parse-record-trace-detail'
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

const loadRecentTraces = async (preserveSelectedTrace = false) => {
  loadingList.value = true
  errorMessage.value = ''
  const previousTraceId = preserveSelectedTrace ? detail.value?.traceId : ''

  try {
    const traces = await getGovernanceTraceSummaries(form.tenantId, form.limit, {
      requestPrefix: 'frontend-parse-record-trace-summaries'
    })
    recentTraces.value = Array.isArray(traces) ? traces : []

    if (!activeFilters.value) {
      const nextTraceId = previousTraceId || recentTraces.value[0]?.traceId || ''
      if (nextTraceId) {
        await loadTraceDetail(nextTraceId)
      } else {
        detail.value = null
      }
    }
  } catch (error) {
    recentTraces.value = []
    if (!activeFilters.value) {
      detail.value = null
    }
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingList.value = false
  }
}

const refreshEvidence = async () => {
  activeFilters.value = null
  lookupResults.value = []
  hasMore.value = false
  nextCursor.value = ''
  syncRouteQuery({
    tenantId: form.tenantId,
    limit: String(form.limit)
  })
  await loadRecentTraces(true)
}

const runLookup = async () => {
  if (!hasLookupCriteria.value) {
    errorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项后再执行长窗口反查。'
      : 'Enter at least one of traceId, taskId, or reportId before running the indexed lookup.'
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
    limit: String(form.limit),
    ...activeFilters.value
  })

  try {
    const lookupPage = await lookupGovernanceTraces(form.tenantId, activeFilters.value, form.limit, {
      requestPrefix: 'frontend-parse-record-lookups'
    })
    await applyLookupPage(lookupPage, false)
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
        requestPrefix: 'frontend-parse-record-lookups-more'
      }
    )
    await applyLookupPage(lookupPage, true)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingLookup.value = false
  }
}

const clearLookup = async () => {
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  form.windowStart = ''
  form.windowEnd = ''
  lookupResults.value = []
  hasMore.value = false
  nextCursor.value = ''
  activeFilters.value = null
  syncRouteQuery({
    tenantId: form.tenantId,
    limit: String(form.limit)
  })
  await loadRecentTraces()
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

const openAuditForensics = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.auditForensics,
    query: buildDrillQuery(source)
  })
}

const eventHighlights = event => {
  const request = event?.requestParams || {}
  const response = event?.responseSummary || {}
  return [
    {
      label: isChinese.value ? '任务' : 'Task',
      value: response.taskId || request.taskId
    },
    {
      label: isChinese.value ? '报告' : 'Report',
      value: response.reportId || request.reportId
    },
    {
      label: isChinese.value ? '指纹' : 'Fingerprint',
      value: request.sqlFingerprint
    },
    {
      label: isChinese.value ? '错误码' : 'Error',
      value: response.errorCode
    },
    {
      label: isChinese.value ? '目标引擎' : 'Engine',
      value: response.targetEngine
    },
    {
      label: isChinese.value ? '降级' : 'Degraded',
      value: typeof response.degraded === 'boolean' ? String(response.degraded) : ''
    }
  ].filter(item => displayValue(item.value) !== '-')
}

onMounted(async () => {
  if (hasDisplayValue(route.query.tenantId)) {
    form.tenantId = String(route.query.tenantId)
  }
  if (hasDisplayValue(route.query.limit)) {
    form.limit = Number(route.query.limit) || 12
  }
  form.traceId = String(route.query.traceId || '')
  form.taskId = String(route.query.taskId || '')
  form.reportId = String(route.query.reportId || '')
  form.windowStart = String(route.query.windowStart || '')
  form.windowEnd = String(route.query.windowEnd || '')

  await loadRecentTraces()
  if (form.traceId || form.taskId || form.reportId) {
    await runLookup()
  }
})

const __returned__ = { t, locale, route, router, form, loadingList, loadingLookup, loadingDetail, recentTraces, lookupResults, detail, errorMessage, hasMore, nextCursor, activeFilters, isChinese, displayedTraces, displayedCount, recentCount, nonSuccessCount, hasLookupCriteria, pageMode, lookupCriteria, selectedSummary, detailHighlights, hasDisplayValue, displayValue, formatTimestamp, normalizeFilters, syncRouteQuery, buildDrillQuery, loadTraceDetail, applyLookupPage, loadRecentTraces, refreshEvidence, runLookup, loadMoreResults, clearLookup, openRepairEvidence, openAuditForensics, eventHighlights, computed, onMounted, reactive, ref, get useI18n() { return useI18n }, get useRoute() { return useRoute }, get useRouter() { return useRouter }, get ROUTE_PATHS() { return ROUTE_PATHS }, get formatRuntimeError() { return formatRuntimeError }, get getGovernanceTraceDetail() { return getGovernanceTraceDetail }, get getGovernanceTraceSummaries() { return getGovernanceTraceSummaries }, get lookupGovernanceTraces() { return lookupGovernanceTraces } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { createElementVNode as _createElementVNode, toDisplayString as _toDisplayString, resolveComponent as _resolveComponent, createVNode as _createVNode, createTextVNode as _createTextVNode, withCtx as _withCtx, openBlock as _openBlock, createBlock as _createBlock, createCommentVNode as _createCommentVNode, renderList as _renderList, Fragment as _Fragment, createElementBlock as _createElementBlock, normalizeClass as _normalizeClass } from "vue"

const _hoisted_1 = {
  class: "runtime-page",
  "data-testid": "parse-record-page"
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
const _hoisted_17 = { class: "field-block" }
const _hoisted_18 = { class: "field-label" }
const _hoisted_19 = { class: "field-block field-block-wide" }
const _hoisted_20 = { class: "field-label" }
const _hoisted_21 = { class: "field-block" }
const _hoisted_22 = { class: "field-label" }
const _hoisted_23 = { class: "field-block" }
const _hoisted_24 = { class: "field-label" }
const _hoisted_25 = { class: "action-row action-row-wrap" }
const _hoisted_26 = { class: "lookup-chip-list" }
const _hoisted_27 = { class: "lookup-chip" }
const _hoisted_28 = { "data-testid": "parse-record-page-mode" }
const _hoisted_29 = {
  key: 0,
  class: "lookup-chip lookup-chip-muted"
}
const _hoisted_30 = { class: "summary-card-grid" }
const _hoisted_31 = { class: "summary-card" }
const _hoisted_32 = { class: "summary-card-label" }
const _hoisted_33 = { "data-testid": "parse-record-display-count" }
const _hoisted_34 = { class: "summary-card" }
const _hoisted_35 = { class: "summary-card-label" }
const _hoisted_36 = { "data-testid": "parse-record-recent-count" }
const _hoisted_37 = { class: "summary-card summary-card-warning" }
const _hoisted_38 = { class: "summary-card-label" }
const _hoisted_39 = { "data-testid": "parse-record-non-success-count" }
const _hoisted_40 = {
  key: 0,
  class: "result-banner result-banner-warning",
  "data-testid": "parse-record-has-more"
}
const _hoisted_41 = {
  key: 1,
  class: "result-banner result-banner-danger",
  "data-testid": "parse-record-error"
}
const _hoisted_42 = {
  key: 2,
  class: "empty-state"
}
const _hoisted_43 = { class: "trace-list" }
const _hoisted_44 = ["onClick"]
const _hoisted_45 = { class: "trace-item-header" }
const _hoisted_46 = { class: "trace-item-service sqlforge-code-label" }
const _hoisted_47 = { class: "trace-item-meta" }
const _hoisted_48 = { class: "trace-item-foot" }
const _hoisted_49 = { class: "surface-card" }
const _hoisted_50 = { class: "section-heading" }
const _hoisted_51 = { class: "section-title" }
const _hoisted_52 = {
  key: 0,
  class: "empty-state"
}
const _hoisted_53 = { "data-testid": "parse-record-detail-trace-id" }
const _hoisted_54 = { "data-testid": "parse-record-detail-status" }
const _hoisted_55 = { class: "action-row action-row-wrap drill-action-row" }
const _hoisted_56 = { class: "evidence-grid" }
const _hoisted_57 = { class: "evidence-item" }
const _hoisted_58 = { class: "evidence-label" }
const _hoisted_59 = { "data-testid": "parse-record-detail-service-code" }
const _hoisted_60 = { class: "evidence-item" }
const _hoisted_61 = { class: "evidence-label" }
const _hoisted_62 = { class: "evidence-item" }
const _hoisted_63 = { class: "evidence-label" }
const _hoisted_64 = { "data-testid": "parse-record-detail-resource-id" }
const _hoisted_65 = { class: "evidence-item" }
const _hoisted_66 = { class: "evidence-label" }
const _hoisted_67 = { class: "evidence-item" }
const _hoisted_68 = { class: "evidence-label" }
const _hoisted_69 = { "data-testid": "parse-record-detail-audit-count" }
const _hoisted_70 = { class: "evidence-item" }
const _hoisted_71 = { class: "evidence-label" }
const _hoisted_72 = { class: "evidence-item" }
const _hoisted_73 = { class: "evidence-label" }
const _hoisted_74 = { "data-testid": "parse-record-detail-query-history-count" }
const _hoisted_75 = { class: "evidence-item" }
const _hoisted_76 = { class: "evidence-label" }
const _hoisted_77 = { "data-testid": "parse-record-detail-export-count" }
const _hoisted_78 = { class: "highlight-grid" }
const _hoisted_79 = ["data-testid"]
const _hoisted_80 = { class: "timeline-list" }
const _hoisted_81 = { class: "timeline-card-header" }
const _hoisted_82 = { class: "timeline-card-id sqlforge-code-label" }
const _hoisted_83 = { class: "timeline-card-line" }
const _hoisted_84 = { class: "timeline-card-line timeline-card-line-muted" }
const _hoisted_85 = { class: "timeline-card-meta" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_input = _resolveComponent("el-input")
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("div", _hoisted_2, [
      _createElementVNode("div", null, [
        _cache[7] || (_cache[7] = _createElementVNode("p", { class: "runtime-eyebrow sqlforge-code-label" }, "frontend runtime gate", -1 /* HOISTED */)),
        _createElementVNode("h1", _hoisted_3, _toDisplayString($setup.t('parseRecord.title')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('parseRecord.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("p", _hoisted_5, _toDisplayString($setup.isChinese
            ? '该页同时展示最近历史窗口与 indexed 长窗口反查，支持按 trace、task、report 追溯更老记录，并把命中结果跳转到修复证据和审计取证页。'
            : 'This page combines the recent history window with indexed long-window lookup so you can trace older records by trace, task, or report and jump directly into repair evidence or audit forensics.'), 1 /* TEXT */)
    ]),
    _createElementVNode("div", _hoisted_6, [
      _createElementVNode("article", _hoisted_7, [
        _createElementVNode("div", _hoisted_8, [
          _createElementVNode("div", null, [
            _cache[8] || (_cache[8] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "trace controls", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_9, _toDisplayString($setup.isChinese ? '历史入口、长窗口反查与分页命中' : 'History entry, indexed lookup and paged matches'), 1 /* TEXT */)
          ])
        ]),
        _createElementVNode("div", _hoisted_10, [
          _createElementVNode("label", _hoisted_11, [
            _createElementVNode("span", _hoisted_12, _toDisplayString($setup.isChinese ? '租户上下文' : 'Tenant context'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.tenantId,
              "onUpdate:modelValue": _cache[0] || (_cache[0] = $event => (($setup.form.tenantId) = $event)),
              "data-testid": "parse-record-tenant-id"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_13, [
            _createElementVNode("span", _hoisted_14, _toDisplayString($setup.isChinese ? '返回数量' : 'Lookup limit'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.limit,
              "onUpdate:modelValue": _cache[1] || (_cache[1] = $event => (($setup.form.limit) = $event)),
              "data-testid": "parse-record-limit"
            }, null, 8 /* PROPS */, ["modelValue"])
          ]),
          _createElementVNode("label", _hoisted_15, [
            _createElementVNode("span", _hoisted_16, _toDisplayString($setup.isChinese ? '窗口起点' : 'Window start'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.windowStart,
              "onUpdate:modelValue": _cache[2] || (_cache[2] = $event => (($setup.form.windowStart) = $event)),
              "data-testid": "parse-record-window-start",
              placeholder: $setup.isChinese ? '2026-04-01T00:00:00' : '2026-04-01T00:00:00'
            }, null, 8 /* PROPS */, ["modelValue", "placeholder"])
          ]),
          _createElementVNode("label", _hoisted_17, [
            _createElementVNode("span", _hoisted_18, _toDisplayString($setup.isChinese ? '窗口终点' : 'Window end'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.windowEnd,
              "onUpdate:modelValue": _cache[3] || (_cache[3] = $event => (($setup.form.windowEnd) = $event)),
              "data-testid": "parse-record-window-end",
              placeholder: $setup.isChinese ? '2026-04-22T23:59:59' : '2026-04-22T23:59:59'
            }, null, 8 /* PROPS */, ["modelValue", "placeholder"])
          ]),
          _createElementVNode("label", _hoisted_19, [
            _createElementVNode("span", _hoisted_20, _toDisplayString($setup.isChinese ? 'Trace ID' : 'Trace ID'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.traceId,
              "onUpdate:modelValue": _cache[4] || (_cache[4] = $event => (($setup.form.traceId) = $event)),
              "data-testid": "parse-record-trace-id",
              placeholder: $setup.isChinese ? '输入 trace id 反查单条执行链' : 'Enter a trace id to look up one execution chain'
            }, null, 8 /* PROPS */, ["modelValue", "placeholder"])
          ]),
          _createElementVNode("label", _hoisted_21, [
            _createElementVNode("span", _hoisted_22, _toDisplayString($setup.isChinese ? 'Task ID' : 'Task ID'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.taskId,
              "onUpdate:modelValue": _cache[5] || (_cache[5] = $event => (($setup.form.taskId) = $event)),
              "data-testid": "parse-record-task-id",
              placeholder: $setup.isChinese ? '输入异步任务 id 追更老历史' : 'Enter an async task id to trace older history'
            }, null, 8 /* PROPS */, ["modelValue", "placeholder"])
          ]),
          _createElementVNode("label", _hoisted_23, [
            _createElementVNode("span", _hoisted_24, _toDisplayString($setup.isChinese ? 'Report ID' : 'Report ID'), 1 /* TEXT */),
            _createVNode(_component_el_input, {
              modelValue: $setup.form.reportId,
              "onUpdate:modelValue": _cache[6] || (_cache[6] = $event => (($setup.form.reportId) = $event)),
              "data-testid": "parse-record-report-id",
              placeholder: $setup.isChinese ? '输入报告 id 追写回证据' : 'Enter a report id to trace write-back evidence'
            }, null, 8 /* PROPS */, ["modelValue", "placeholder"])
          ])
        ]),
        _createElementVNode("div", _hoisted_25, [
          _createVNode(_component_el_button, {
            type: "primary",
            loading: $setup.loadingList,
            "data-testid": "parse-record-refresh",
            onClick: $setup.refreshEvidence
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '刷新最近历史' : 'Refresh recent traces'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            loading: $setup.loadingLookup,
            "data-testid": "parse-record-run-lookup",
            onClick: $setup.runLookup
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.isChinese ? '执行长窗口反查' : 'Run indexed lookup'), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }, 8 /* PROPS */, ["loading"]),
          _createVNode(_component_el_button, {
            "data-testid": "parse-record-clear-lookup",
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
                "data-testid": "parse-record-load-more",
                onClick: $setup.loadMoreResults
              }, {
                default: _withCtx(() => [
                  _createTextVNode(_toDisplayString($setup.isChinese ? '加载更早结果' : 'Load older results'), 1 /* TEXT */)
                ]),
                _: 1 /* STABLE */
              }, 8 /* PROPS */, ["loading"]))
            : _createCommentVNode("v-if", true)
        ]),
        _createElementVNode("div", _hoisted_26, [
          _createElementVNode("span", _hoisted_27, [
            _createTextVNode(_toDisplayString($setup.isChinese ? '视图模式' : 'View mode') + ": ", 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_28, _toDisplayString($setup.pageMode), 1 /* TEXT */)
          ]),
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.lookupCriteria, (item) => {
            return (_openBlock(), _createElementBlock("span", {
              key: item.key,
              class: "lookup-chip"
            }, _toDisplayString(item.label) + ": " + _toDisplayString(item.value), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */)),
          (!$setup.lookupCriteria.length)
            ? (_openBlock(), _createElementBlock("span", _hoisted_29, _toDisplayString($setup.isChinese
                ? '未输入 trace/task/report 时，左侧显示最近历史窗口。'
                : 'Without trace, task, or report criteria the recent history window is shown.'), 1 /* TEXT */))
            : _createCommentVNode("v-if", true)
        ]),
        _createElementVNode("div", _hoisted_30, [
          _createElementVNode("article", _hoisted_31, [
            _createElementVNode("span", _hoisted_32, _toDisplayString($setup.isChinese ? '当前显示 trace' : 'Displayed traces'), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_33, _toDisplayString($setup.displayedCount), 1 /* TEXT */)
          ]),
          _createElementVNode("article", _hoisted_34, [
            _createElementVNode("span", _hoisted_35, _toDisplayString($setup.isChinese ? '最近 trace 数' : 'Recent traces'), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_36, _toDisplayString($setup.recentCount), 1 /* TEXT */)
          ]),
          _createElementVNode("article", _hoisted_37, [
            _createElementVNode("span", _hoisted_38, _toDisplayString($setup.isChinese ? '异常/补偿 trace' : 'Non-success traces'), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_39, _toDisplayString($setup.nonSuccessCount), 1 /* TEXT */)
          ])
        ]),
        ($setup.hasMore)
          ? (_openBlock(), _createElementBlock("div", _hoisted_40, [
              _createElementVNode("strong", null, _toDisplayString($setup.isChinese ? '仍有更早历史' : 'Older history available'), 1 /* TEXT */),
              _createElementVNode("span", null, _toDisplayString($setup.nextCursor || '-'), 1 /* TEXT */)
            ]))
          : _createCommentVNode("v-if", true),
        ($setup.errorMessage)
          ? (_openBlock(), _createElementBlock("div", _hoisted_41, _toDisplayString($setup.errorMessage), 1 /* TEXT */))
          : (!$setup.displayedTraces.length)
            ? (_openBlock(), _createElementBlock("p", _hoisted_42, _toDisplayString($setup.isChinese
              ? '刷新后会显示最近的 query / optimization / benchmark 追溯记录，或按 trace/task/report 拉取更老历史。'
              : 'Refresh to load recent query, optimization, and benchmark traces, or search by trace, task, or report to pull older history.'), 1 /* TEXT */))
            : _createCommentVNode("v-if", true),
        _createElementVNode("div", _hoisted_43, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.displayedTraces, (trace) => {
            return (_openBlock(), _createElementBlock("button", {
              key: trace.traceId,
              type: "button",
              class: _normalizeClass(["trace-item", { 'trace-item-active': $setup.detail?.traceId === trace.traceId }]),
              "data-testid": "parse-record-trace-item",
              onClick: $event => ($setup.loadTraceDetail(trace.traceId))
            }, [
              _createElementVNode("div", _hoisted_45, [
                _createElementVNode("div", null, [
                  _createElementVNode("p", _hoisted_46, _toDisplayString(trace.serviceCode || '-'), 1 /* TEXT */),
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
              _createElementVNode("p", _hoisted_47, _toDisplayString(trace.operationType || '-') + " · " + _toDisplayString($setup.formatTimestamp(trace.lastSeenAt)), 1 /* TEXT */),
              _createElementVNode("div", _hoisted_48, [
                _createElementVNode("span", null, _toDisplayString($setup.isChinese ? '审计事件' : 'Audit events') + ": " + _toDisplayString(trace.auditEventCount), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.isChinese ? '任务' : 'Task') + ": " + _toDisplayString($setup.displayValue(trace.taskId)), 1 /* TEXT */),
                _createElementVNode("span", null, _toDisplayString($setup.isChinese ? '报告' : 'Report') + ": " + _toDisplayString($setup.displayValue(trace.reportId)), 1 /* TEXT */)
              ])
            ], 10 /* CLASS, PROPS */, _hoisted_44))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("article", _hoisted_49, [
        _createElementVNode("div", _hoisted_50, [
          _createElementVNode("div", null, [
            _cache[9] || (_cache[9] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "trace detail", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_51, _toDisplayString($setup.isChinese ? '历史诊断、分页跳转与审计时间线' : 'Historical diagnosis, drill-through and audit timeline'), 1 /* TEXT */)
          ])
        ]),
        (!$setup.detail && !$setup.errorMessage)
          ? (_openBlock(), _createElementBlock("p", _hoisted_52, _toDisplayString($setup.isChinese
              ? '选择左侧 trace 后，这里会显示审计事件、关联任务/报告，并可跳转到修复证据或审计取证页。'
              : 'After you select a trace, the linked audit events, task/report references and drill-through actions render here.'), 1 /* TEXT */))
          : _createCommentVNode("v-if", true),
        ($setup.detail)
          ? (_openBlock(), _createElementBlock(_Fragment, { key: 1 }, [
              _createElementVNode("div", {
                class: _normalizeClass(["result-banner", 
              $setup.detail.latestStatus === 'SUCCESS' || $setup.detail.latestStatus === 'SUCCEEDED'
                ? 'result-banner-success'
                : 'result-banner-warning'
            ])
              }, [
                _createElementVNode("strong", _hoisted_53, _toDisplayString($setup.detail.traceId), 1 /* TEXT */),
                _createElementVNode("span", _hoisted_54, _toDisplayString($setup.detail.latestStatus || '-'), 1 /* TEXT */)
              ], 2 /* CLASS */),
              _createElementVNode("div", _hoisted_55, [
                _createVNode(_component_el_button, {
                  type: "primary",
                  "data-testid": "parse-record-open-repair-evidence",
                  onClick: $setup.openRepairEvidence
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.isChinese ? '打开修复证据' : 'Open repair evidence'), 1 /* TEXT */)
                  ]),
                  _: 1 /* STABLE */
                }),
                _createVNode(_component_el_button, {
                  "data-testid": "parse-record-open-audit-forensics",
                  onClick: $setup.openAuditForensics
                }, {
                  default: _withCtx(() => [
                    _createTextVNode(_toDisplayString($setup.isChinese ? '打开审计取证' : 'Open audit forensics'), 1 /* TEXT */)
                  ]),
                  _: 1 /* STABLE */
                })
              ]),
              _createElementVNode("div", _hoisted_56, [
                _createElementVNode("div", _hoisted_57, [
                  _createElementVNode("span", _hoisted_58, _toDisplayString($setup.isChinese ? '服务编码' : 'Service code'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_59, _toDisplayString($setup.detail.serviceCode || '-'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_60, [
                  _createElementVNode("span", _hoisted_61, _toDisplayString($setup.isChinese ? '资源类型' : 'Resource type'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.detail.resourceType || '-'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_62, [
                  _createElementVNode("span", _hoisted_63, _toDisplayString($setup.isChinese ? '资源标识' : 'Resource id'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_64, _toDisplayString($setup.detail.resourceId || '-'), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_65, [
                  _createElementVNode("span", _hoisted_66, _toDisplayString($setup.isChinese ? '最后发生时间' : 'Last seen at'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.formatTimestamp($setup.detail.lastSeenAt)), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_67, [
                  _createElementVNode("span", _hoisted_68, _toDisplayString($setup.isChinese ? '审计事件数' : 'Audit events'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_69, _toDisplayString($setup.detail.auditEventCount || 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_70, [
                  _createElementVNode("span", _hoisted_71, _toDisplayString($setup.isChinese ? '异常事件数' : 'Non-success events'), 1 /* TEXT */),
                  _createElementVNode("strong", null, _toDisplayString($setup.detail.nonSuccessEventCount || 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_72, [
                  _createElementVNode("span", _hoisted_73, _toDisplayString($setup.isChinese ? '历史记录数' : 'History records'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_74, _toDisplayString($setup.detail.queryHistoryCount || 0), 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_75, [
                  _createElementVNode("span", _hoisted_76, _toDisplayString($setup.isChinese ? '导出记录数' : 'Export records'), 1 /* TEXT */),
                  _createElementVNode("strong", _hoisted_77, _toDisplayString($setup.detail.exportRecordCount || 0), 1 /* TEXT */)
                ])
              ]),
              _createElementVNode("div", _hoisted_78, [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.detailHighlights, (item) => {
                  return (_openBlock(), _createElementBlock("div", {
                    key: item.key,
                    class: "highlight-chip"
                  }, [
                    _createElementVNode("span", null, _toDisplayString(item.label), 1 /* TEXT */),
                    _createElementVNode("strong", {
                      "data-testid": `parse-record-detail-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`
                    }, _toDisplayString($setup.displayValue(item.value)), 9 /* TEXT, PROPS */, _hoisted_79)
                  ]))
                }), 128 /* KEYED_FRAGMENT */))
              ]),
              _createElementVNode("div", _hoisted_80, [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.detail.auditEvents, (event) => {
                  return (_openBlock(), _createElementBlock("article", {
                    key: event.id,
                    class: "timeline-card",
                    "data-testid": "parse-record-audit-event"
                  }, [
                    _createElementVNode("div", _hoisted_81, [
                      _createElementVNode("div", null, [
                        _createElementVNode("p", _hoisted_82, _toDisplayString(event.serviceCode), 1 /* TEXT */),
                        _createElementVNode("h3", null, _toDisplayString(event.operationType) + " · " + _toDisplayString(event.targetId), 1 /* TEXT */)
                      ]),
                      _createElementVNode("span", {
                        class: _normalizeClass(["trace-status-pill", 
                    event.status === 'SUCCESS' || event.status === 'SUCCEEDED'
                      ? 'trace-status-success'
                      : 'trace-status-warning'
                  ])
                      }, _toDisplayString(event.status), 3 /* TEXT, CLASS */)
                    ]),
                    _createElementVNode("p", _hoisted_83, _toDisplayString($setup.isChinese ? '请求链路' : 'Request chain') + ": " + _toDisplayString(event.requestId) + " / " + _toDisplayString(event.traceId), 1 /* TEXT */),
                    _createElementVNode("p", _hoisted_84, _toDisplayString($setup.formatTimestamp(event.createTime)) + " · " + _toDisplayString(event.costMs || 0) + "ms ", 1 /* TEXT */),
                    _createElementVNode("div", _hoisted_85, [
                      (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.eventHighlights(event), (item) => {
                        return (_openBlock(), _createElementBlock("span", {
                          key: `${event.id}-${item.label}`,
                          class: "timeline-meta-pill"
                        }, _toDisplayString(item.label) + ": " + _toDisplayString($setup.displayValue(item.value)), 1 /* TEXT */))
                      }), 128 /* KEYED_FRAGMENT */))
                    ])
                  ]))
                }), 128 /* KEYED_FRAGMENT */))
              ])
            ], 64 /* STABLE_FRAGMENT */))
          : _createCommentVNode("v-if", true)
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-fd8ca4ec'
__sfc__.render = render
__sfc__.__file = "src/views/parse-record/ParseRecordView.js"

export default __sfc__
