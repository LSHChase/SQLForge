<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceTraceDetail,
  getGovernanceTraceSummaries,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'

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
const loadingHistoryDetail = ref(false)
const recentTraces = ref([])
const lookupResults = ref([])
const detail = ref(null)
const historyDetail = ref(null)
const errorMessage = ref('')
const hasMore = ref(false)
const nextCursor = ref('')
const activeFilters = ref(null)
const viewOptions = reactive({
  statusFilter: 'ALL',
  sortMode: 'LAST_SEEN_DESC'
})

const isChinese = computed(() => locale.value === 'zh-CN')
const baseTraces = computed(() => (activeFilters.value ? lookupResults.value : recentTraces.value))
const statusBuckets = computed(() => {
  const counts = {
    ALL: baseTraces.value.length,
    NON_SUCCESS: 0,
    SUCCESS: 0,
    QUERY: 0,
    OPTIMIZATION: 0,
    BENCHMARK: 0
  }
  for (const trace of baseTraces.value) {
    if (isNonSuccessTrace(trace)) {
      counts.NON_SUCCESS += 1
    } else {
      counts.SUCCESS += 1
    }
    const serviceCode = String(trace.serviceCode || '').toUpperCase()
    if (serviceCode.includes('QUERY')) {
      counts.QUERY += 1
    } else if (serviceCode.includes('OPTIMIZATION')) {
      counts.OPTIMIZATION += 1
    } else if (serviceCode.includes('BENCHMARK')) {
      counts.BENCHMARK += 1
    }
  }
  return counts
})
const displayedTraces = computed(() => {
  const filtered = baseTraces.value.filter(trace => matchesStatusFilter(trace, viewOptions.statusFilter))
  const sorted = [...filtered]
  sorted.sort((left, right) => compareTrace(left, right, viewOptions.sortMode))
  return sorted
})
const displayedCount = computed(() => displayedTraces.value.length)
const recentCount = computed(() => recentTraces.value.length)
const nonSuccessCount = computed(() =>
  displayedTraces.value.filter(trace => {
    return isNonSuccessTrace(trace)
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
const filterOptions = computed(() => [
  {
    value: 'ALL',
    label: isChinese.value ? `全部 (${statusBuckets.value.ALL})` : `All (${statusBuckets.value.ALL})`
  },
  {
    value: 'NON_SUCCESS',
    label: isChinese.value
      ? `异常/补偿 (${statusBuckets.value.NON_SUCCESS})`
      : `Non-success (${statusBuckets.value.NON_SUCCESS})`
  },
  {
    value: 'SUCCESS',
    label: isChinese.value ? `成功 (${statusBuckets.value.SUCCESS})` : `Success (${statusBuckets.value.SUCCESS})`
  },
  {
    value: 'QUERY',
    label: isChinese.value ? `查询 (${statusBuckets.value.QUERY})` : `Query (${statusBuckets.value.QUERY})`
  },
  {
    value: 'OPTIMIZATION',
    label: isChinese.value
      ? `优化 (${statusBuckets.value.OPTIMIZATION})`
      : `Optimization (${statusBuckets.value.OPTIMIZATION})`
  },
  {
    value: 'BENCHMARK',
    label: isChinese.value
      ? `压测 (${statusBuckets.value.BENCHMARK})`
      : `Benchmark (${statusBuckets.value.BENCHMARK})`
  }
])
const sortOptions = computed(() => [
  {
    value: 'LAST_SEEN_DESC',
    label: isChinese.value ? '最近时间优先' : 'Newest first'
  },
  {
    value: 'LAST_SEEN_ASC',
    label: isChinese.value ? '最早时间优先' : 'Oldest first'
  },
  {
    value: 'STATUS_RISK',
    label: isChinese.value ? '异常优先' : 'Risk first'
  },
  {
    value: 'AUDIT_EVENTS_DESC',
    label: isChinese.value ? '审计事件数优先' : 'Audit events first'
  }
])
const selectedSummary = computed(() =>
  displayedTraces.value.find(trace => trace.traceId === detail.value?.traceId) || null
)
const selectedHistorySummary = computed(() => {
  if (!historyDetail.value?.historyId) {
    return detail.value?.queryHistories?.[0] || null
  }
  return (detail.value?.queryHistories || []).find(item => item.historyId === historyDetail.value.historyId) || null
})
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
const sqlStateHighlights = computed(() => {
  if (!historyDetail.value?.sqlState) {
    return []
  }
  return [
    {
      key: 'sqlFingerprint',
      label: isChinese.value ? '执行指纹' : 'SQL fingerprint',
      value: historyDetail.value.sqlState.sqlFingerprint
    },
    {
      key: 'sqlTemplateFingerprint',
      label: isChinese.value ? '模板指纹' : 'Template fingerprint',
      value: historyDetail.value.sqlState.sqlTemplateFingerprint
    },
    {
      key: 'boundSqlFingerprint',
      label: isChinese.value ? '绑定指纹' : 'Bound fingerprint',
      value: historyDetail.value.sqlState.boundSqlFingerprint
    },
    {
      key: 'bindingMode',
      label: isChinese.value ? '绑定模式' : 'Binding mode',
      value: historyDetail.value.sqlState.bindingMode
    },
    {
      key: 'bindingRenderStatus',
      label: isChinese.value ? '渲染状态' : 'Binding render',
      value: historyDetail.value.sqlState.bindingRenderStatus
    },
    {
      key: 'parameterizedSqlFlag',
      label: isChinese.value ? '参数化' : 'Parameterized',
      value: typeof historyDetail.value.sqlState.parameterizedSqlFlag === 'boolean'
        ? String(historyDetail.value.sqlState.parameterizedSqlFlag)
        : ''
    }
  ].filter(item => displayValue(item.value) !== '-')
})
const queryHistorySummaryCards = computed(() => {
  if (!historyDetail.value) {
    return []
  }
  return [
    {
      label: isChinese.value ? 'History ID' : 'History ID',
      value: historyDetail.value.historyId
    },
    {
      label: isChinese.value ? '结果 ID' : 'Result ID',
      value: historyDetail.value.resultId
    },
    {
      label: isChinese.value ? '数据源' : 'Datasource',
      value: historyDetail.value.datasourceCode || historyDetail.value.datasourceType
    },
    {
      label: isChinese.value ? '报表编码' : 'Report code',
      value: historyDetail.value.reportCode
    },
    {
      label: isChinese.value ? '提交人' : 'Submitted by',
      value: historyDetail.value.submittedBy
    },
    {
      label: isChinese.value ? '提交时间' : 'Submitted at',
      value: formatTimestamp(historyDetail.value.submittedAt)
    }
  ].filter(item => displayValue(item.value) !== '-')
})
const sqlVariants = computed(() => {
  if (!historyDetail.value) {
    return []
  }
  return [
    {
      key: 'sqlText',
      label: isChinese.value ? '原始 SQL' : 'Original SQL',
      value: historyDetail.value.sqlText
    },
    {
      key: 'sqlTemplateText',
      label: isChinese.value ? '模板 SQL' : 'Template SQL',
      value: historyDetail.value.sqlTemplateText
    },
    {
      key: 'boundSqlText',
      label: isChinese.value ? '绑定 SQL' : 'Bound SQL',
      value: historyDetail.value.boundSqlText
    }
  ].filter(item => hasDisplayValue(item.value))
})
const queryHistorySignalGroups = computed(() => {
  if (!historyDetail.value) {
    return []
  }
  return [
    {
      key: 'commentContext',
      title: isChinese.value ? '注释上下文' : 'Comment context',
      payload: historyDetail.value.commentContext
    },
    {
      key: 'queryDateSummary',
      title: isChinese.value ? '查询日期摘要' : 'Query-date summary',
      payload: historyDetail.value.queryDateSummary
    },
    {
      key: 'executionSummary',
      title: isChinese.value ? '执行摘要' : 'Execution summary',
      payload: historyDetail.value.executionSummary
    },
    {
      key: 'structureParseSummary',
      title: isChinese.value ? '结构解析' : 'Structure parse',
      payload: historyDetail.value.structureParseSummary
    },
    {
      key: 'accessParseSummary',
      title: isChinese.value ? '访问解析' : 'Access parse',
      payload: historyDetail.value.accessParseSummary
    },
    {
      key: 'routeDecision',
      title: isChinese.value ? '路由决策' : 'Route decision',
      payload: historyDetail.value.routeDecision
    },
    {
      key: 'cacheSummary',
      title: isChinese.value ? '缓存摘要' : 'Cache summary',
      payload: historyDetail.value.cacheSummary
    },
    {
      key: 'bindingSummary',
      title: isChinese.value ? '绑定摘要' : 'Binding summary',
      payload: historyDetail.value.bindingSummary
    }
  ].filter(group => isNonEmptyObject(group.payload))
})
const referenceGroups = computed(() => {
  if (!historyDetail.value) {
    return []
  }
  return [
    {
      key: 'recommendationRefs',
      title: isChinese.value ? '推荐关联' : 'Recommendation refs',
      items: historyDetail.value.recommendationRefs || []
    },
    {
      key: 'benchmarkRefs',
      title: isChinese.value ? '压测关联' : 'Benchmark refs',
      items: historyDetail.value.benchmarkRefs || []
    },
    {
      key: 'alertRefs',
      title: isChinese.value ? '告警关联' : 'Alert refs',
      items: historyDetail.value.alertRefs || []
    },
    {
      key: 'auditRefs',
      title: isChinese.value ? '审计关联' : 'Audit refs',
      items: historyDetail.value.auditRefs || []
    }
  ].filter(group => Array.isArray(group.items) && group.items.length > 0)
})
const historyLogicalObjectHits = computed(() => historyDetail.value?.logicalObjectHits || [])

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

const isNonEmptyObject = value => value && typeof value === 'object' && !Array.isArray(value) && Object.keys(value).length > 0

const formatJson = value => JSON.stringify(value, null, 2)

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

const parseTime = value => {
  if (!hasDisplayValue(value)) {
    return 0
  }
  const parsed = Date.parse(String(value))
  return Number.isNaN(parsed) ? 0 : parsed
}

const isNonSuccessTrace = trace => {
  const status = String(trace?.latestStatus || '').toUpperCase()
  return (trace?.nonSuccessEventCount || 0) > 0 || (status && status !== 'SUCCESS' && status !== 'SUCCEEDED')
}

const matchesStatusFilter = (trace, filter) => {
  if (filter === 'ALL') {
    return true
  }
  if (filter === 'NON_SUCCESS') {
    return isNonSuccessTrace(trace)
  }
  if (filter === 'SUCCESS') {
    return !isNonSuccessTrace(trace)
  }
  const serviceCode = String(trace?.serviceCode || '').toUpperCase()
  return serviceCode.includes(filter)
}

const compareTrace = (left, right, sortMode) => {
  if (sortMode === 'LAST_SEEN_ASC') {
    return parseTime(left.lastSeenAt) - parseTime(right.lastSeenAt)
  }
  if (sortMode === 'STATUS_RISK') {
    const riskDelta = Number(isNonSuccessTrace(right)) - Number(isNonSuccessTrace(left))
    if (riskDelta !== 0) {
      return riskDelta
    }
    return parseTime(right.lastSeenAt) - parseTime(left.lastSeenAt)
  }
  if (sortMode === 'AUDIT_EVENTS_DESC') {
    const eventDelta = (right.auditEventCount || 0) - (left.auditEventCount || 0)
    if (eventDelta !== 0) {
      return eventDelta
    }
    return parseTime(right.lastSeenAt) - parseTime(left.lastSeenAt)
  }
  return parseTime(right.lastSeenAt) - parseTime(left.lastSeenAt)
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
    const firstHistoryId = detail.value?.queryHistories?.[0]?.historyId || ''
    if (firstHistoryId) {
      await loadQueryHistoryDetail(firstHistoryId)
    } else {
      historyDetail.value = null
    }
  } catch (error) {
    detail.value = null
    historyDetail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingDetail.value = false
  }
}

const loadQueryHistoryDetail = async historyId => {
  if (!historyId) {
    historyDetail.value = null
    return
  }
  loadingHistoryDetail.value = true
  errorMessage.value = ''
  try {
    historyDetail.value = await getGovernanceQueryHistoryDetail(form.tenantId, historyId, {
      requestPrefix: 'frontend-parse-record-query-history-detail'
    })
  } catch (error) {
    historyDetail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingHistoryDetail.value = false
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
  historyDetail.value = null
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
</script>

<template>
  <section class="runtime-page" data-testid="parse-record-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('parseRecord.title') }}</h1>
        <p class="runtime-summary">{{ t('parseRecord.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页同时展示最近历史窗口与 indexed 长窗口反查，支持按 trace、task、report 追溯更老记录，并把命中结果跳转到修复证据和审计取证页。'
            : 'This page combines the recent history window with indexed long-window lookup so you can trace older records by trace, task, or report and jump directly into repair evidence or audit forensics.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">trace controls</p>
            <h2 class="section-title">
              {{ isChinese ? '历史入口、长窗口反查与分页命中' : 'History entry, indexed lookup and paged matches' }}
            </h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户上下文' : 'Tenant context' }}</span>
            <el-input v-model="form.tenantId" data-testid="parse-record-tenant-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '返回数量' : 'Lookup limit' }}</span>
            <el-input v-model="form.limit" data-testid="parse-record-limit" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '窗口起点' : 'Window start' }}</span>
            <el-input
              v-model="form.windowStart"
              data-testid="parse-record-window-start"
              :placeholder="isChinese ? '2026-04-01T00:00:00' : '2026-04-01T00:00:00'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '窗口终点' : 'Window end' }}</span>
            <el-input
              v-model="form.windowEnd"
              data-testid="parse-record-window-end"
              :placeholder="isChinese ? '2026-04-22T23:59:59' : '2026-04-22T23:59:59'"
            />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? 'Trace ID' : 'Trace ID' }}</span>
            <el-input
              v-model="form.traceId"
              data-testid="parse-record-trace-id"
              :placeholder="isChinese ? '输入 trace id 反查单条执行链' : 'Enter a trace id to look up one execution chain'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Task ID' : 'Task ID' }}</span>
            <el-input
              v-model="form.taskId"
              data-testid="parse-record-task-id"
              :placeholder="isChinese ? '输入异步任务 id 追更老历史' : 'Enter an async task id to trace older history'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Report ID' : 'Report ID' }}</span>
            <el-input
              v-model="form.reportId"
              data-testid="parse-record-report-id"
              :placeholder="isChinese ? '输入报告 id 追写回证据' : 'Enter a report id to trace write-back evidence'"
            />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="loadingList"
            data-testid="parse-record-refresh"
            @click="refreshEvidence"
          >
            {{ isChinese ? '刷新最近历史' : 'Refresh recent traces' }}
          </el-button>
          <el-button
            :loading="loadingLookup"
            data-testid="parse-record-run-lookup"
            @click="runLookup"
          >
            {{ isChinese ? '执行长窗口反查' : 'Run indexed lookup' }}
          </el-button>
          <el-button data-testid="parse-record-clear-lookup" @click="clearLookup">
            {{ isChinese ? '清空条件' : 'Clear criteria' }}
          </el-button>
          <el-button
            v-if="hasMore"
            :loading="loadingLookup"
            data-testid="parse-record-load-more"
            @click="loadMoreResults"
          >
            {{ isChinese ? '加载更早结果' : 'Load older results' }}
          </el-button>
        </div>

        <div class="lookup-chip-list">
          <span class="lookup-chip lookup-chip-strong">
            {{ isChinese ? '筛选' : 'Filter' }}:
            <strong data-testid="parse-record-status-filter">{{ viewOptions.statusFilter }}</strong>
          </span>
          <span class="lookup-chip lookup-chip-strong">
            {{ isChinese ? '排序' : 'Sort' }}:
            <strong data-testid="parse-record-sort-mode">{{ viewOptions.sortMode }}</strong>
          </span>
          <span class="lookup-chip">
            {{ isChinese ? '视图模式' : 'View mode' }}:
            <strong data-testid="parse-record-page-mode">{{ pageMode }}</strong>
          </span>
          <span
            v-for="item in lookupCriteria"
            :key="item.key"
            class="lookup-chip"
          >
            {{ item.label }}: {{ item.value }}
          </span>
          <span v-if="!lookupCriteria.length" class="lookup-chip lookup-chip-muted">
            {{
              isChinese
                ? '未输入 trace/task/report 时，左侧显示最近历史窗口。'
                : 'Without trace, task, or report criteria the recent history window is shown.'
            }}
          </span>
        </div>

        <div class="view-controls">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '历史分类' : 'History classification' }}</span>
            <el-select v-model="viewOptions.statusFilter" data-testid="parse-record-filter-select">
              <el-option
                v-for="option in filterOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '排序方式' : 'Sort mode' }}</span>
            <el-select v-model="viewOptions.sortMode" data-testid="parse-record-sort-select">
              <el-option
                v-for="option in sortOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>
        </div>

        <div class="summary-card-grid">
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '当前显示 trace' : 'Displayed traces' }}</span>
            <strong data-testid="parse-record-display-count">{{ displayedCount }}</strong>
          </article>
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '最近 trace 数' : 'Recent traces' }}</span>
            <strong data-testid="parse-record-recent-count">{{ recentCount }}</strong>
          </article>
          <article class="summary-card summary-card-warning">
            <span class="summary-card-label">{{ isChinese ? '异常/补偿 trace' : 'Non-success traces' }}</span>
            <strong data-testid="parse-record-non-success-count">{{ nonSuccessCount }}</strong>
          </article>
        </div>

        <div
          v-if="hasMore"
          class="result-banner result-banner-warning"
          data-testid="parse-record-has-more"
        >
          <strong>{{ isChinese ? '仍有更早历史' : 'Older history available' }}</strong>
          <span>{{ nextCursor || '-' }}</span>
        </div>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="parse-record-error"
        >
          {{ errorMessage }}
        </div>

        <p v-else-if="!displayedTraces.length" class="empty-state">
          {{
            isChinese
              ? '刷新后会显示最近的 query / optimization / benchmark 追溯记录，或按 trace/task/report 拉取更老历史。'
              : 'Refresh to load recent query, optimization, and benchmark traces, or search by trace, task, or report to pull older history.'
          }}
        </p>

        <div class="trace-list">
          <button
            v-for="trace in displayedTraces"
            :key="trace.traceId"
            type="button"
            class="trace-item"
            :class="{ 'trace-item-active': detail?.traceId === trace.traceId }"
            data-testid="parse-record-trace-item"
            @click="loadTraceDetail(trace.traceId)"
          >
            <div class="trace-item-header">
              <div>
                <p class="trace-item-service sqlforge-code-label">{{ trace.serviceCode || '-' }}</p>
                <h3>{{ trace.resourceId || trace.traceId }}</h3>
              </div>
              <span
                class="trace-status-pill"
                :class="
                  trace.latestStatus === 'SUCCESS' || trace.latestStatus === 'SUCCEEDED'
                    ? 'trace-status-success'
                    : 'trace-status-warning'
                "
              >
                {{ trace.latestStatus || '-' }}
              </span>
            </div>
            <p class="trace-item-meta">
              {{ trace.operationType || '-' }} · {{ formatTimestamp(trace.lastSeenAt) }}
            </p>
            <div class="trace-item-foot">
              <span>{{ isChinese ? '审计事件' : 'Audit events' }}: {{ trace.auditEventCount }}</span>
              <span>{{ isChinese ? '任务' : 'Task' }}: {{ displayValue(trace.taskId) }}</span>
              <span>{{ isChinese ? '报告' : 'Report' }}: {{ displayValue(trace.reportId) }}</span>
            </div>
          </button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">trace detail</p>
            <h2 class="section-title">
              {{ isChinese ? '历史诊断、分页跳转与审计时间线' : 'Historical diagnosis, drill-through and audit timeline' }}
            </h2>
          </div>
        </div>

        <p v-if="!detail && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '选择左侧 trace 后，这里会显示审计事件、关联任务/报告，并可跳转到修复证据或审计取证页。'
              : 'After you select a trace, the linked audit events, task/report references and drill-through actions render here.'
          }}
        </p>

        <template v-if="detail">
          <div
            class="result-banner"
            :class="
              detail.latestStatus === 'SUCCESS' || detail.latestStatus === 'SUCCEEDED'
                ? 'result-banner-success'
                : 'result-banner-warning'
            "
          >
            <strong data-testid="parse-record-detail-trace-id">{{ detail.traceId }}</strong>
            <span data-testid="parse-record-detail-status">{{ detail.latestStatus || '-' }}</span>
          </div>

          <div class="action-row action-row-wrap drill-action-row">
            <el-button
              type="primary"
              data-testid="parse-record-open-repair-evidence"
              @click="openRepairEvidence"
            >
              {{ isChinese ? '打开修复证据' : 'Open repair evidence' }}
            </el-button>
            <el-button
              data-testid="parse-record-open-audit-forensics"
              @click="openAuditForensics"
            >
              {{ isChinese ? '打开审计取证' : 'Open audit forensics' }}
            </el-button>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '服务编码' : 'Service code' }}</span>
              <strong data-testid="parse-record-detail-service-code">{{ detail.serviceCode || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源类型' : 'Resource type' }}</span>
              <strong>{{ detail.resourceType || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源标识' : 'Resource id' }}</span>
              <strong data-testid="parse-record-detail-resource-id">{{ detail.resourceId || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '最后发生时间' : 'Last seen at' }}</span>
              <strong>{{ formatTimestamp(detail.lastSeenAt) }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '审计事件数' : 'Audit events' }}</span>
              <strong data-testid="parse-record-detail-audit-count">{{ detail.auditEventCount || 0 }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '异常事件数' : 'Non-success events' }}</span>
              <strong>{{ detail.nonSuccessEventCount || 0 }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '历史记录数' : 'History records' }}</span>
              <strong data-testid="parse-record-detail-query-history-count">{{ detail.queryHistoryCount || 0 }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '导出记录数' : 'Export records' }}</span>
              <strong data-testid="parse-record-detail-export-count">{{ detail.exportRecordCount || 0 }}</strong>
            </div>
          </div>

          <div class="highlight-grid">
            <div
              v-for="item in detailHighlights"
              :key="item.key"
              class="highlight-chip"
            >
              <span>{{ item.label }}</span>
              <strong :data-testid="`parse-record-detail-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                {{ displayValue(item.value) }}
              </strong>
            </div>
          </div>

          <div class="history-detail-panel">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">query history detail</p>
                <h3 class="detail-subtitle">
                  {{ isChinese ? 'SQL 三态、解析结果与关联取证' : 'SQL tri-state, parse signals, and related forensics' }}
                </h3>
              </div>
            </div>

            <p v-if="!detail.queryHistories?.length" class="empty-state">
              {{ isChinese ? '当前 trace 没有 query history 详情。' : 'No query-history detail is linked to this trace yet.' }}
            </p>

            <template v-else>
              <div class="query-history-list">
                <button
                  v-for="history in detail.queryHistories"
                  :key="history.historyId"
                  type="button"
                  class="history-pill"
                  :class="{ 'history-pill-active': selectedHistorySummary?.historyId === history.historyId }"
                  :data-testid="`parse-record-history-${history.historyId}`"
                  @click="loadQueryHistoryDetail(history.historyId)"
                >
                  <strong>{{ history.reportCode || history.historyId }}</strong>
                  <span>{{ history.historyType || '-' }} · {{ history.datasourceCode || history.datasourceType || '-' }}</span>
                </button>
              </div>

              <p v-if="loadingHistoryDetail" class="empty-state">
                {{ isChinese ? '正在加载 query history 详情…' : 'Loading query-history detail…' }}
              </p>

              <template v-else-if="historyDetail">
                <div class="summary-card-grid history-summary-grid">
                  <article
                    v-for="item in queryHistorySummaryCards"
                    :key="item.label"
                    class="summary-card"
                  >
                    <span class="summary-card-label">{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </article>
                </div>

                <div class="highlight-grid history-sql-state-grid">
                  <div
                    v-for="item in sqlStateHighlights"
                    :key="item.key"
                    class="highlight-chip"
                  >
                    <span>{{ item.label }}</span>
                    <strong :data-testid="`parse-record-history-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                      {{ displayValue(item.value) }}
                    </strong>
                  </div>
                </div>

                <div class="sql-variant-list">
                  <article
                    v-for="item in sqlVariants"
                    :key="item.key"
                    class="sql-variant-card"
                  >
                    <div class="sql-variant-card__header">
                      <span class="summary-card-label">{{ item.label }}</span>
                    </div>
                    <pre class="code-block" :data-testid="`parse-record-${item.key}`">{{ item.value }}</pre>
                  </article>
                </div>

                <div v-if="historyLogicalObjectHits.length" class="reference-group">
                  <div class="section-heading">
                    <div>
                      <p class="section-kicker sqlforge-code-label">logical objects</p>
                      <h3 class="detail-subtitle">{{ isChinese ? '逻辑对象命中' : 'Logical object hits' }}</h3>
                    </div>
                  </div>
                  <div class="timeline-card-meta">
                    <span
                      v-for="(item, index) in historyLogicalObjectHits"
                      :key="`${item.objectKey || item.logicalObjectKey || item.objectName || 'logical'}-${index}`"
                      class="timeline-meta-pill"
                    >
                      {{ item.objectType || item.logicalObjectType || 'OBJECT' }}:
                      {{ item.objectKey || item.logicalObjectKey || item.objectName || '-' }}
                    </span>
                  </div>
                </div>

                <div class="signal-grid">
                  <article
                    v-for="group in queryHistorySignalGroups"
                    :key="group.key"
                    class="signal-card"
                  >
                    <div class="signal-card__header">
                      <span class="summary-card-label">{{ group.title }}</span>
                    </div>
                    <pre class="code-block" :data-testid="`parse-record-${group.key}`">{{ formatJson(group.payload) }}</pre>
                  </article>
                </div>

                <div v-if="referenceGroups.length" class="reference-grid">
                  <article
                    v-for="group in referenceGroups"
                    :key="group.key"
                    class="reference-group"
                  >
                    <div class="signal-card__header">
                      <span class="summary-card-label">{{ group.title }}</span>
                    </div>
                    <div class="reference-list">
                      <pre
                        v-for="(item, index) in group.items"
                        :key="`${group.key}-${index}`"
                        class="code-block code-block-compact"
                        :data-testid="`parse-record-${group.key}-${index}`"
                      >{{ formatJson(item) }}</pre>
                    </div>
                  </article>
                </div>
              </template>
            </template>
          </div>

          <div class="timeline-list">
            <article
              v-for="event in detail.auditEvents"
              :key="event.id"
              class="timeline-card"
              data-testid="parse-record-audit-event"
            >
              <div class="timeline-card-header">
                <div>
                  <p class="timeline-card-id sqlforge-code-label">{{ event.serviceCode }}</p>
                  <h3>{{ event.operationType }} · {{ event.targetId }}</h3>
                </div>
                <span
                  class="trace-status-pill"
                  :class="
                    event.status === 'SUCCESS' || event.status === 'SUCCEEDED'
                      ? 'trace-status-success'
                      : 'trace-status-warning'
                  "
                >
                  {{ event.status }}
                </span>
              </div>
              <p class="timeline-card-line">
                {{ isChinese ? '请求链路' : 'Request chain' }}:
                {{ event.requestId }} / {{ event.traceId }}
              </p>
              <p class="timeline-card-line timeline-card-line-muted">
                {{ formatTimestamp(event.createTime) }} · {{ event.costMs || 0 }}ms
              </p>
              <div class="timeline-card-meta">
                <span
                  v-for="item in eventHighlights(event)"
                  :key="`${event.id}-${item.label}`"
                  class="timeline-meta-pill"
                >
                  {{ item.label }}: {{ displayValue(item.value) }}
                </span>
              </div>
            </article>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.08), transparent 45%),
    var(--sqlforge-surface-2);
  box-shadow: none;
}

.runtime-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 20px;
  padding: 28px;
}

.runtime-eyebrow,
.section-kicker,
.trace-item-service,
.timeline-card-id {
  margin: 0 0 8px;
  color: #0f766e;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.runtime-title,
.section-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.runtime-summary,
.runtime-note,
.trace-item-meta,
.timeline-card-line,
.timeline-card-line-muted,
.empty-state {
  color: var(--sqlforge-text-secondary);
}

.runtime-note {
  margin: 0;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(35, 35, 35, 0.9);
  line-height: 1.6;
}

.runtime-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.92fr) minmax(0, 1.08fr);
  gap: 20px;
}

.runtime-grid > .surface-card {
  padding: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.form-grid,
.evidence-grid,
.summary-card-grid,
.highlight-grid {
  display: grid;
  gap: 14px;
}

.form-grid,
.evidence-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card-grid,
.highlight-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.field-block,
.summary-card,
.evidence-item,
.highlight-chip,
.trace-item,
.timeline-card {
  border-radius: 18px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(35, 35, 35, 0.92);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.field-block-wide {
  grid-column: span 2;
}

.field-label,
.summary-card-label,
.evidence-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.action-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.action-row-wrap {
  flex-wrap: wrap;
}

.drill-action-row {
  margin: 18px 0;
}

.summary-card,
.evidence-item,
.highlight-chip {
  padding: 16px 18px;
}

.summary-card,
.highlight-chip {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-card strong,
.evidence-item strong,
.highlight-chip strong {
  font-size: 18px;
  color: #0f172a;
}

.summary-card-warning {
  background: rgba(254, 242, 242, 0.9);
  border-color: rgba(248, 113, 113, 0.18);
}

.lookup-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.lookup-chip,
.timeline-meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
  font-size: 13px;
}

.lookup-chip-strong {
  background: rgba(186, 230, 253, 0.85);
  color: #0f172a;
}

.view-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.lookup-chip-muted {
  background: rgba(241, 245, 249, 0.9);
  color: #64748b;
}

.result-banner {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-banner-success {
  background: rgba(236, 253, 245, 0.9);
  color: #166534;
}

.result-banner-warning {
  background: rgba(255, 247, 237, 0.92);
  color: #9a3412;
}

.result-banner-danger {
  background: rgba(254, 242, 242, 0.92);
  color: #b91c1c;
}

.trace-list,
.timeline-list,
.signal-grid,
.reference-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.history-detail-panel {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(148, 163, 184, 0.18);
}

.detail-subtitle {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.query-history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-pill {
  display: grid;
  gap: 4px;
  text-align: left;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(35, 35, 35, 0.92);
  cursor: pointer;
}

.history-pill span {
  color: #475569;
  font-size: 13px;
}

.history-pill-active {
  border-color: rgba(14, 165, 233, 0.4);
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.12);
}

.history-summary-grid,
.history-sql-state-grid,
.sql-variant-list {
  margin-top: 18px;
}

.sql-variant-list {
  display: grid;
  gap: 14px;
}

.sql-variant-card,
.signal-card,
.reference-group {
  border-radius: 18px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(35, 35, 35, 0.92);
  padding: 16px 18px;
}

.signal-grid,
.reference-grid {
  display: grid;
}

.signal-card__header,
.sql-variant-card__header {
  margin-bottom: 10px;
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.92);
  color: #dbeafe;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.code-block-compact {
  font-size: 12px;
}

.reference-list {
  display: grid;
  gap: 10px;
}

.trace-item,
.timeline-card {
  width: 100%;
  text-align: left;
  padding: 16px 18px;
}

.trace-item {
  cursor: pointer;
}

.trace-item-active {
  border-color: rgba(14, 165, 233, 0.4);
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.12);
}

.trace-item-header,
.timeline-card-header,
.trace-item-foot,
.timeline-card-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.trace-item-header,
.timeline-card-header {
  align-items: flex-start;
}

.trace-item-header h3,
.timeline-card-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
}

.trace-item-meta,
.timeline-card-line {
  margin: 10px 0 0;
}

.trace-item-foot,
.timeline-card-meta {
  margin-top: 12px;
  flex-wrap: wrap;
  color: #475569;
  font-size: 13px;
}

.trace-status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.trace-status-success {
  background: rgba(220, 252, 231, 0.95);
  color: #166534;
}

.trace-status-warning {
  background: rgba(255, 237, 213, 0.95);
  color: #9a3412;
}

.empty-state {
  margin: 18px 0 0;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(241, 245, 249, 0.9);
}

@media (max-width: 1100px) {
  .runtime-grid,
  .runtime-hero,
  .form-grid,
  .evidence-grid,
  .summary-card-grid,
  .view-controls,
  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .field-block-wide {
    grid-column: span 1;
  }
}
</style>
