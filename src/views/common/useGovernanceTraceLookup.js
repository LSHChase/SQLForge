import { computed, reactive, ref } from 'vue'
import {
  formatRuntimeError,
  getGovernanceTraceDetail,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'
import {
  hasDisplayValue,
  isCompensationTrace,
  resolveTraceRepairSignal
} from './governanceTrace'

const normalizeFilters = form => ({
  traceId: String(form.traceId || '').trim(),
  taskId: String(form.taskId || '').trim(),
  reportId: String(form.reportId || '').trim(),
  windowStart: String(form.windowStart || '').trim(),
  windowEnd: String(form.windowEnd || '').trim()
})

const appendFilterQuery = (query, filters) => {
  for (const key of ['traceId', 'taskId', 'reportId', 'windowStart', 'windowEnd']) {
    if (hasDisplayValue(filters?.[key])) {
      query[key] = String(filters[key])
    }
  }
  return query
}

export function useGovernanceTraceLookup(options) {
  const form = reactive({
    tenantId: options.defaultTenantId || 'tenant-a',
    traceId: '',
    taskId: '',
    reportId: '',
    windowStart: '',
    windowEnd: '',
    limit: options.defaultLimit || 12
  })

  const loadingLookup = ref(false)
  const loadingDetail = ref(false)
  const lookupResults = ref([])
  const detail = ref(null)
  const errorMessage = ref('')
  const hasMore = ref(false)
  const nextCursor = ref('')
  const activeFilters = ref(null)

  const activeTraceId = computed(() => detail.value?.traceId || '')
  const matchedCount = computed(() => lookupResults.value.length)
  const compensationCount = computed(() =>
    lookupResults.value.filter(trace => isCompensationTrace(trace.traceId)).length
  )
  const reportLinkedCount = computed(() =>
    lookupResults.value.filter(trace => hasDisplayValue(trace.reportId) || (trace.exportRecordCount || 0) > 0).length
  )
  const repairSignalCount = computed(() =>
    lookupResults.value.filter(trace => resolveTraceRepairSignal(trace) !== 'STEADY_STATE').length
  )
  const nonSuccessCount = computed(() =>
    lookupResults.value.filter(trace => resolveTraceRepairSignal(trace) === 'FAILURE_CHAIN').length
  )
  const searchCriteria = computed(() =>
    ['traceId', 'taskId', 'reportId', 'windowStart', 'windowEnd']
      .map(key => ({ key, value: form[key] }))
      .filter(item => hasDisplayValue(item.value))
  )
  const selectedSummary = computed(() =>
    lookupResults.value.find(trace => trace.traceId === activeTraceId.value) || null
  )

  const syncRouteQuery = query => {
    if (!options.router || !options.routePath) {
      return
    }
    options.router.replace({
      path: options.routePath,
      query
    })
  }

  const buildRouteQuery = extraQuery => {
    const query = {
      tenantId: form.tenantId,
      limit: String(form.limit),
      ...(extraQuery || {})
    }
    return appendFilterQuery(query, activeFilters.value || normalizeFilters(form))
  }

  const buildDrillQuery = (source, extraQuery) => {
    const filters = activeFilters.value || {
      traceId: source?.traceId,
      taskId: source?.taskId,
      reportId: source?.reportId,
      windowStart: form.windowStart,
      windowEnd: form.windowEnd
    }
    const query = {
      tenantId: form.tenantId,
      limit: String(form.limit),
      ...(extraQuery || {})
    }
    return appendFilterQuery(query, filters)
  }

  const loadTraceDetail = async traceId => {
    if (!traceId) {
      detail.value = null
      return
    }

    loadingDetail.value = true
    errorMessage.value = ''

    try {
      detail.value = await getGovernanceTraceDetail(form.tenantId, traceId, options.detailLimit || 20, {
        requestPrefix: `${options.requestPrefix}-trace-detail`
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

  const runLookup = async extraQuery => {
    if (!hasDisplayValue(form.traceId) && !hasDisplayValue(form.taskId) && !hasDisplayValue(form.reportId)) {
      errorMessage.value = options.requiredMessage()
      lookupResults.value = []
      detail.value = null
      hasMore.value = false
      nextCursor.value = ''
      activeFilters.value = null
      return false
    }

    loadingLookup.value = true
    errorMessage.value = ''
    activeFilters.value = normalizeFilters(form)
    syncRouteQuery(buildRouteQuery(extraQuery))

    try {
      const lookupPage = await lookupGovernanceTraces(form.tenantId, activeFilters.value, form.limit, {
        requestPrefix: `${options.requestPrefix}-lookups`
      })
      await applyLookupPage(lookupPage, false)
      return true
    } catch (error) {
      lookupResults.value = []
      detail.value = null
      hasMore.value = false
      nextCursor.value = ''
      errorMessage.value = formatRuntimeError(error)
      return false
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
          requestPrefix: `${options.requestPrefix}-lookups-more`
        }
      )
      await applyLookupPage(lookupPage, true)
    } catch (error) {
      errorMessage.value = formatRuntimeError(error)
    } finally {
      loadingLookup.value = false
    }
  }

  const clearLookup = extraQuery => {
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
    syncRouteQuery({
      tenantId: form.tenantId,
      limit: String(form.limit),
      ...(extraQuery || {})
    })
  }

  const hydrateFromRoute = route => {
    if (hasDisplayValue(route.query.tenantId)) {
      form.tenantId = String(route.query.tenantId)
    }
    if (hasDisplayValue(route.query.limit)) {
      form.limit = Number(route.query.limit) || options.defaultLimit || 12
    }
    form.traceId = String(route.query.traceId || '')
    form.taskId = String(route.query.taskId || '')
    form.reportId = String(route.query.reportId || '')
    form.windowStart = String(route.query.windowStart || '')
    form.windowEnd = String(route.query.windowEnd || '')
  }

  return {
    form,
    loadingLookup,
    loadingDetail,
    lookupResults,
    detail,
    errorMessage,
    hasMore,
    nextCursor,
    activeFilters,
    activeTraceId,
    matchedCount,
    compensationCount,
    reportLinkedCount,
    repairSignalCount,
    nonSuccessCount,
    searchCriteria,
    selectedSummary,
    runLookup,
    loadMoreResults,
    clearLookup,
    loadTraceDetail,
    buildDrillQuery,
    hydrateFromRoute
  }
}

