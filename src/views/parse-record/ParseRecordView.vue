<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  exportGovernanceQueryHistory,
  formatRuntimeError,
  getGovernanceDatasources,
  getGovernanceQueryHistoryDetail,
  getGovernanceQueryHistoryPage,
  getGovernanceTraceDetail,
  listParseBatches,
  listReportBatches,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'
import { buildDatasourceOptions, buildTenantOptions, withCurrentOption } from '../common/formComponentGovernance'

const { locale } = useI18n()
const route = useRoute()
const router = useRouter()

const DEFAULT_HISTORY_CONTEXT_TENANT_ID = 'tenant-a'
const normalizeQueryValue = value => String(value || '').trim()

const form = reactive({
  tenantId: '',
  reportCode: '',
  datasourceCode: '',
  stage: '',
  bizDate: '',
  queryDateStart: '',
  queryDateEnd: '',
  accessChannel: '',
  status: '',
  logicalObjectType: '',
  engine: '',
  submittedBy: '',
  cacheHit: '',
  rewriteApplied: '',
  accelerationApplied: '',
  parameterizedSql: '',
  sortBy: '',
  sortOrder: '',
  submittedStart: '',
  submittedEnd: '',
  traceId: '',
  taskId: '',
  reportId: ''
})

const loading = reactive({
  page: false,
  detail: false,
  lookup: false,
  export: false
})

const page = ref(null)
const datasourceOptions = ref([])
const datasourceOptionsLoadFailed = ref(false)
const queryDateRange = ref([])
const submittedAtRange = ref([])
const selectedHistoryId = ref('')
const detailDialogVisible = ref(false)
const evidenceDrawerVisible = ref(false)
const activeDialogTab = ref('overview')
const batchHistoryTab = ref('parse')
const selectedHistoryDetail = ref(null)
const errorMessage = ref('')
const batchHistoryErrorMessage = ref('')
const exportDialogVisible = ref(false)
const exportResult = ref(null)
const exportForm = reactive({
  exportFormat: 'JSON',
  includeTraceDetail: true,
  exportReason: 'frontend-history-forensics'
})

const rows = computed(() => page.value?.items || [])
const parseBatchHistoryRows = ref([])
const reportBatchHistoryRows = ref([])
const isChinese = computed(() => locale.value === 'zh-CN')
const routeTenantId = computed(() => normalizeQueryValue(route.query.tenantId))
const requestTenantId = computed(() => normalizeQueryValue(form.tenantId) || routeTenantId.value || DEFAULT_HISTORY_CONTEXT_TENANT_ID)
const tenantOptions = computed(() => buildTenantOptions(form.tenantId, rows.value))
const classificationSummary = computed(() => page.value?.classificationSummary || {})
const sortModeLabel = computed(() => {
  const sortBy = normalizeQueryValue(form.sortBy)
  const sortOrder = normalizeQueryValue(form.sortOrder)
  if (!sortBy && !sortOrder) {
    return isChinese.value ? '后端默认' : 'Backend default'
  }
  return `${sortBy || 'submittedAt'} ${sortOrder || 'DESC'}`
})
const card = (label, value) => ({ label, value })
const parseBatchHistorySummary = computed(() => {
  const totalRecords = parseBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.totalRecords || 0), 0)
  const failedBatches = parseBatchHistoryRows.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(isChinese.value ? '批次总数' : 'Batches', parseBatchHistoryRows.value.length),
    card(isChinese.value ? '总记录数' : 'Records', totalRecords),
    card(isChinese.value ? '失败批次' : 'Failed batches', failedBatches)
  ]
})
const reportBatchHistorySummary = computed(() => {
  const totalReports = reportBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.totalReports || 0), 0)
  const resolvedReports = reportBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.resolvedReports || 0), 0)
  const failedBatches = reportBatchHistoryRows.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(isChinese.value ? '批次总数' : 'Batches', reportBatchHistoryRows.value.length),
    card(isChinese.value ? '报表总数' : 'Reports', totalReports),
    card(isChinese.value ? '已解析' : 'Resolved', resolvedReports),
    card(isChinese.value ? '失败批次' : 'Failed batches', failedBatches)
  ]
})
const pageSummaryCards = computed(() => [
  { label: isChinese.value ? '当前页记录' : 'Current page', value: rows.value.length },
  { label: isChinese.value ? '成功' : 'Success', value: classificationSummary.value.SUCCESS || classificationSummary.value.succeeded || 0 },
  { label: isChinese.value ? '异常/部分成功' : 'Non-success', value: classificationSummary.value.PARTIAL || classificationSummary.value.FAILED || classificationSummary.value.nonSuccess || 0 },
  { label: isChinese.value ? '接入渠道' : 'Access channels', value: Object.keys(classificationSummary.value.accessChannelCounts || {}).length }
])
const detailSummaryCards = computed(() => {
  if (!selectedHistoryDetail.value) {
    return []
  }
  return [
    { label: 'History ID', value: selectedHistoryDetail.value.historyId },
    { label: 'Trace ID', value: selectedHistoryDetail.value.traceId },
    { label: isChinese.value ? '报表编码' : 'Report code', value: selectedHistoryDetail.value.reportCode },
    { label: isChinese.value ? '服务编码' : 'Service code', value: selectedHistoryDetail.value.traceDetail?.serviceCode || selectedHistoryDetail.value.historyType },
    { label: isChinese.value ? '结果状态' : 'Result status', value: selectedHistoryDetail.value.resultStatus },
    { label: isChinese.value ? '目标引擎' : 'Target engine', value: selectedHistoryDetail.value.targetEngine },
    { label: isChinese.value ? '接入渠道' : 'Access channel', value: selectedHistoryDetail.value.accessChannel },
    { label: isChinese.value ? '提交时间' : 'Submitted at', value: formatTimestamp(selectedHistoryDetail.value.submittedAt) }
  ]
})
const traceSummaryCards = computed(() => {
  const traceDetail = selectedHistoryDetail.value?.traceDetail
  if (!traceDetail) {
    return []
  }
  return [
    { label: isChinese.value ? '最新状态' : 'Latest status', value: traceDetail.latestStatus },
    { label: isChinese.value ? '审计事件数' : 'Audit event count', value: traceDetail.auditEventCount },
    { label: isChinese.value ? '异常事件数' : 'Non-success events', value: traceDetail.nonSuccessEventCount },
    { label: isChinese.value ? '关联历史数' : 'Query history count', value: traceDetail.queryHistoryCount }
  ]
})
const sqlStateHighlights = computed(() => {
  const sqlState = selectedHistoryDetail.value?.sqlState || {}
  return [
    { key: 'sqlFingerprint', label: isChinese.value ? '执行指纹' : 'SQL fingerprint', value: sqlState.sqlFingerprint || selectedHistoryDetail.value?.sqlFingerprint },
    { key: 'sqlTemplateFingerprint', label: isChinese.value ? '模板指纹' : 'Template fingerprint', value: sqlState.sqlTemplateFingerprint || selectedHistoryDetail.value?.sqlTemplateFingerprint },
    { key: 'boundSqlFingerprint', label: isChinese.value ? '绑定指纹' : 'Bound fingerprint', value: sqlState.boundSqlFingerprint || selectedHistoryDetail.value?.boundSqlFingerprint },
    { key: 'bindingMode', label: isChinese.value ? '绑定模式' : 'Binding mode', value: sqlState.bindingMode || selectedHistoryDetail.value?.bindingMode },
    { key: 'bindingRenderStatus', label: isChinese.value ? '渲染状态' : 'Binding render', value: sqlState.bindingRenderStatus || selectedHistoryDetail.value?.bindingRenderStatus },
    { key: 'parameterizedSqlFlag', label: isChinese.value ? '参数化' : 'Parameterized', value: String(sqlState.parameterizedSqlFlag ?? selectedHistoryDetail.value?.parameterizedSqlFlag ?? '-') }
  ]
})
const sqlVariants = computed(() =>
  [
    { key: 'sqlText', label: isChinese.value ? '原始 SQL' : 'Original SQL', value: selectedHistoryDetail.value?.sqlText },
    { key: 'sqlTemplateText', label: isChinese.value ? '模板 SQL' : 'Template SQL', value: selectedHistoryDetail.value?.sqlTemplateText },
    { key: 'boundSqlText', label: isChinese.value ? '绑定 SQL' : 'Bound SQL', value: selectedHistoryDetail.value?.boundSqlText }
  ].filter(item => hasDisplayValue(item.value))
)
const signalGroups = computed(() =>
  [
    { key: 'commentContext', title: isChinese.value ? '注释上下文' : 'Comment context', payload: selectedHistoryDetail.value?.commentContext },
    { key: 'queryDateSummary', title: isChinese.value ? '查询日期摘要' : 'Query-date summary', payload: selectedHistoryDetail.value?.queryDateSummary },
    { key: 'executionSummary', title: isChinese.value ? '执行摘要' : 'Execution summary', payload: selectedHistoryDetail.value?.executionSummary },
    { key: 'structureParseSummary', title: isChinese.value ? '结构解析' : 'Structure parse', payload: selectedHistoryDetail.value?.structureParseSummary },
    { key: 'accessParseSummary', title: isChinese.value ? '访问解析' : 'Access parse', payload: selectedHistoryDetail.value?.accessParseSummary },
    { key: 'routeDecision', title: isChinese.value ? '路由决策' : 'Route decision', payload: selectedHistoryDetail.value?.routeDecision },
    { key: 'bindingSummary', title: isChinese.value ? '绑定摘要' : 'Binding summary', payload: selectedHistoryDetail.value?.bindingSummary }
  ].filter(group => isNonEmpty(group.payload))
)
const referenceGroups = computed(() =>
  [
    { key: 'recommendationRefs', title: isChinese.value ? '推荐关联' : 'Recommendation refs', items: selectedHistoryDetail.value?.recommendationRefs || [] },
    { key: 'benchmarkRefs', title: isChinese.value ? '压测关联' : 'Benchmark refs', items: selectedHistoryDetail.value?.benchmarkRefs || [] },
    { key: 'alertRefs', title: isChinese.value ? '告警关联' : 'Alert refs', items: selectedHistoryDetail.value?.alertRefs || [] },
    { key: 'auditRefs', title: isChinese.value ? '审计关联' : 'Audit refs', items: selectedHistoryDetail.value?.auditRefs || [] }
  ].filter(group => Array.isArray(group.items) && group.items.length > 0)
)
const logicalObjectHits = computed(() => normalizeArray(selectedHistoryDetail.value?.logicalObjectHits))
const traceQueryHistories = computed(() => selectedHistoryDetail.value?.traceDetail?.queryHistories || [])
const auditEvents = computed(() => selectedHistoryDetail.value?.traceDetail?.auditEvents || [])
const hasLookupCriteria = computed(() =>
  hasDisplayValue(form.traceId) || hasDisplayValue(form.taskId) || hasDisplayValue(form.reportId)
)

const loadDatasourceOptions = async () => {
  datasourceOptionsLoadFailed.value = false
  try {
    const nextDatasources = await getGovernanceDatasources(requestTenantId.value, {
      requestPrefix: 'frontend-parse-record-datasource-options'
    })
    datasourceOptions.value = buildDatasourceOptions(nextDatasources)
  } catch {
    datasourceOptions.value = withCurrentOption([], form.datasourceCode)
    datasourceOptionsLoadFailed.value = true
  }
}

const syncDateRangeFields = () => {
  const [queryStart = '', queryEnd = ''] = Array.isArray(queryDateRange.value) ? queryDateRange.value : []
  const [submittedStart = '', submittedEnd = ''] = Array.isArray(submittedAtRange.value) ? submittedAtRange.value : []
  form.queryDateStart = queryStart || ''
  form.queryDateEnd = queryEnd || ''
  form.submittedStart = submittedStart || ''
  form.submittedEnd = submittedEnd || ''
}

const loadPage = async () => {
  syncDateRangeFields()
  loading.page = true
  errorMessage.value = ''
  try {
    page.value = await getGovernanceQueryHistoryPage(
      {
        tenantId: normalizeQueryValue(form.tenantId),
        requestTenantId: requestTenantId.value,
        reportCode: form.reportCode,
        datasourceCode: form.datasourceCode,
        stage: form.stage,
        bizDate: form.bizDate,
        queryDateStart: form.queryDateStart,
        queryDateEnd: form.queryDateEnd,
        accessChannel: form.accessChannel,
        status: form.status,
        logicalObjectType: form.logicalObjectType,
        engine: form.engine,
        submittedBy: form.submittedBy,
        cacheHit: parseBooleanFilter(form.cacheHit),
        rewriteApplied: parseBooleanFilter(form.rewriteApplied),
        accelerationApplied: parseBooleanFilter(form.accelerationApplied),
        parameterizedSql: parseBooleanFilter(form.parameterizedSql),
        submittedStart: form.submittedStart,
        submittedEnd: form.submittedEnd,
        sortBy: form.sortBy,
        sortOrder: form.sortOrder,
        pageNo: 1,
        pageSize: 10
      },
      {
        requestPrefix: 'frontend-parse-record-history-page'
      }
    )
  } catch (error) {
    page.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadBatchHistories = async () => {
  batchHistoryErrorMessage.value = ''
  const [parseResult, reportResult] = await Promise.allSettled([
    listParseBatches(requestTenantId.value),
    listReportBatches(requestTenantId.value)
  ])
  parseBatchHistoryRows.value = parseResult.status === 'fulfilled' && Array.isArray(parseResult.value) ? parseResult.value : []
  reportBatchHistoryRows.value = reportResult.status === 'fulfilled' && Array.isArray(reportResult.value) ? reportResult.value : []
  if (parseResult.status === 'rejected') {
    batchHistoryErrorMessage.value = formatRuntimeError(parseResult.reason)
  }
  if (reportResult.status === 'rejected' && !batchHistoryErrorMessage.value) {
    batchHistoryErrorMessage.value = formatRuntimeError(reportResult.reason)
  }
}

const refreshWorkbench = async () => {
  await Promise.all([loadDatasourceOptions(), loadPage(), loadBatchHistories()])
}

const openHistoryDetail = async (historyId, preloadedTraceDetail = null) => {
  if (!historyId) {
    return
  }
  loading.detail = true
  errorMessage.value = ''
  activeDialogTab.value = 'overview'
  selectedHistoryId.value = historyId
  try {
    const detail = await getGovernanceQueryHistoryDetail(requestTenantId.value, historyId, {
      requestPrefix: 'frontend-parse-record-query-history-detail'
    })
    if (preloadedTraceDetail && !detail.traceDetail) {
      detail.traceDetail = preloadedTraceDetail
    }
    selectedHistoryDetail.value = detail
    detailDialogVisible.value = true
  } catch (error) {
    selectedHistoryDetail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const runIndexedLookup = async () => {
  if (!hasLookupCriteria.value) {
    errorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项。'
      : 'Enter at least one of traceId, taskId, or reportId.'
    return
  }
  loading.lookup = true
  errorMessage.value = ''
  try {
    const lookupPage = await lookupGovernanceTraces(
      requestTenantId.value,
      {
        traceId: form.traceId,
        taskId: form.taskId,
        reportId: form.reportId
      },
      5,
      {
        requestPrefix: 'frontend-parse-record-lookups'
      }
    )
    const firstTraceId = lookupPage?.items?.[0]?.traceId
    if (!firstTraceId) {
      errorMessage.value = isChinese.value ? '没有命中记录。' : 'No history matched the lookup criteria.'
      return
    }
    const traceDetail = await getGovernanceTraceDetail(requestTenantId.value, firstTraceId, 20, {
      requestPrefix: 'frontend-parse-record-trace-detail'
    })
    const firstHistoryId = traceDetail?.queryHistories?.[0]?.historyId
    if (!firstHistoryId) {
      errorMessage.value = isChinese.value ? '命中了 trace，但没有可展示的 query history。' : 'A trace was found but no query-history detail is available.'
      return
    }
    await openHistoryDetail(firstHistoryId, traceDetail)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lookup = false
  }
}

const clearFilters = async () => {
  form.tenantId = ''
  form.reportCode = ''
  form.datasourceCode = ''
  form.stage = ''
  form.bizDate = ''
  form.queryDateStart = ''
  form.queryDateEnd = ''
  queryDateRange.value = []
  form.accessChannel = ''
  form.status = ''
  form.logicalObjectType = ''
  form.engine = ''
  form.submittedBy = ''
  form.cacheHit = ''
  form.rewriteApplied = ''
  form.accelerationApplied = ''
  form.parameterizedSql = ''
  form.sortBy = ''
  form.sortOrder = ''
  form.submittedStart = ''
  form.submittedEnd = ''
  submittedAtRange.value = []
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  await refreshWorkbench()
}

const openExportDialog = () => {
  if (!selectedHistoryDetail.value?.historyId) {
    return
  }
  exportResult.value = null
  exportDialogVisible.value = true
}

const runExport = async () => {
  if (!selectedHistoryDetail.value?.historyId) {
    return
  }
  loading.export = true
  errorMessage.value = ''
  try {
    exportResult.value = await exportGovernanceQueryHistory(
      requestTenantId.value,
      {
        historyId: selectedHistoryDetail.value.historyId,
        exportFormat: exportForm.exportFormat,
        includeTraceDetail: exportForm.includeTraceDetail,
        exportReason: exportForm.exportReason
      },
      {
        requestPrefix: 'frontend-parse-record-export'
      }
    )
  } catch (error) {
    exportResult.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.export = false
  }
}

const openRepairEvidence = () => {
  if (!selectedHistoryDetail.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.repairEvidence,
    query: {
      tenantId: requestTenantId.value,
      traceId: selectedHistoryDetail.value.traceId || '',
      reportId: selectedHistoryDetail.value.reportId || ''
    }
  })
}

const openAuditForensics = () => {
  if (!selectedHistoryDetail.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.auditForensics,
    query: {
      tenantId: requestTenantId.value,
      traceId: selectedHistoryDetail.value.traceId || '',
      reportId: selectedHistoryDetail.value.reportId || ''
    }
  })
}

const openParseBatchCenter = batchId => {
  if (!batchId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseBatchCenter,
    query: {
      tenantId: requestTenantId.value,
      kind: 'parse',
      batchId
    }
  })
}

const openReportBatchCenter = batchId => {
  if (!batchId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseBatchCenter,
    query: {
      tenantId: requestTenantId.value,
      kind: 'report',
      batchId
    }
  })
}

const statusClass = value => {
  const normalized = String(value || '').toUpperCase()
  if (normalized === 'SUCCESS' || normalized === 'SUCCEEDED') {
    return 'pill pill-success'
  }
  if (normalized.includes('PARTIAL')) {
    return 'pill pill-warning'
  }
  return 'pill pill-danger'
}

const parseBooleanFilter = value => {
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  return undefined
}

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const normalizeArray = value => {
  if (Array.isArray(value)) {
    return value
  }
  if (typeof value === 'string' && value.trim()) {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

const isNonEmpty = value => {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).length > 0
  }
  return hasDisplayValue(value)
}

const formatJson = value => JSON.stringify(value, null, 2)

onMounted(async () => {
  if (hasDisplayValue(route.query.traceId)) {
    form.traceId = String(route.query.traceId)
  }
  if (hasDisplayValue(route.query.reportId)) {
    form.reportId = String(route.query.reportId)
  }
  await refreshWorkbench()
  if (hasLookupCriteria.value) {
    await runIndexedLookup()
  }
})
</script>

<template>
  <section class="history-page" data-testid="parse-record-page">
    <header class="surface-card page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">query history workbench</p>
        <h1 class="section-title">{{ isChinese ? 'SQL 历史列表与详情' : 'SQL history list and detail' }}</h1>
        <p class="section-summary">
          {{ isChinese ? '首页只保留筛选和结果表，详情通过 dialog 展开，原始证据进入 drawer。' : 'The first screen keeps only filters and the result table; record detail opens in a dialog and raw evidence moves into a drawer.' }}
        </p>
      </div>
      <div class="action-row">
        <el-button type="primary" :loading="loading.page" data-testid="parse-record-refresh" @click="refreshWorkbench">
          {{ isChinese ? '刷新列表' : 'Refresh list' }}
        </el-button>
        <el-button :loading="loading.lookup" data-testid="parse-record-run-lookup" @click="runIndexedLookup">
          {{ isChinese ? '精确反查' : 'Indexed lookup' }}
        </el-button>
        <el-button @click="clearFilters">{{ isChinese ? '清空条件' : 'Clear filters' }}</el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">
      {{ errorMessage }}
    </div>

    <section class="surface-card filter-panel">
      <div class="field-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-select
            v-model="form.tenantId"
            filterable
            allow-create
            clearable
            default-first-option
            :placeholder="isChinese ? '空条件，使用当前上下文租户' : 'Empty filter, use current context tenant'"
            data-testid="parse-record-tenant-select"
          >
            <el-option
              v-for="item in withCurrentOption(tenantOptions, form.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '报表编码' : 'Report code' }}</span>
          <el-input v-model="form.reportCode" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
          <el-select
            v-model="form.datasourceCode"
            filterable
            allow-create
            default-first-option
            data-testid="parse-record-datasource-filter"
          >
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, form.datasourceCode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '阶段' : 'Stage' }}</span>
          <el-input v-model="form.stage" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '业务日期' : 'Biz date' }}</span>
          <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" placeholder="2026-04-27" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '查询日期区间' : 'Query date range' }}</span>
          <el-date-picker
            v-model="queryDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            unlink-panels
            :range-separator="isChinese ? '至' : 'to'"
            :start-placeholder="isChinese ? '开始日期' : 'Start date'"
            :end-placeholder="isChinese ? '结束日期' : 'End date'"
            data-testid="parse-record-query-date-range"
          />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '结果状态' : 'Status' }}</span>
          <el-select v-model="form.status" data-testid="parse-record-filter-select">
            <el-option label="ALL" value="" />
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="PARTIAL" value="PARTIAL" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '接入渠道' : 'Access channel' }}</span>
          <el-select v-model="form.accessChannel" data-testid="parse-record-status-filter">
            <el-option label="ALL" value="" />
            <el-option label="PAGE" value="PAGE" />
            <el-option label="API" value="API" />
            <el-option label="JDBC_AGENT" value="JDBC_AGENT" />
            <el-option label="SDK" value="SDK" />
            <el-option label="CLIENT" value="CLIENT" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '逻辑对象类型' : 'Logical object type' }}</span>
          <el-select v-model="form.logicalObjectType">
            <el-option label="ALL" value="" />
            <el-option label="BUSINESS_VIEW" value="BUSINESS_VIEW" />
            <el-option label="DB_VIEW" value="DB_VIEW" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '目标引擎' : 'Target engine' }}</span>
          <el-select v-model="form.engine" data-testid="parse-record-engine-filter">
            <el-option label="ALL" value="" />
            <el-option label="HETU" value="HETU" />
            <el-option label="HIVE" value="HIVE" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '提交人' : 'Submitted by' }}</span>
          <el-input v-model="form.submittedBy" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '缓存命中' : 'Cache hit' }}</span>
          <el-select v-model="form.cacheHit" data-testid="parse-record-bool-filter">
            <el-option label="ALL" value="" />
            <el-option label="true" value="true" />
            <el-option label="false" value="false" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '轻量改写' : 'Rewrite applied' }}</span>
          <el-select v-model="form.rewriteApplied">
            <el-option label="ALL" value="" />
            <el-option label="true" value="true" />
            <el-option label="false" value="false" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '加速命中' : 'Acceleration applied' }}</span>
          <el-select v-model="form.accelerationApplied">
            <el-option label="ALL" value="" />
            <el-option label="true" value="true" />
            <el-option label="false" value="false" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '参数化 SQL' : 'Parameterized SQL' }}</span>
          <el-select v-model="form.parameterizedSql">
            <el-option label="ALL" value="" />
            <el-option label="true" value="true" />
            <el-option label="false" value="false" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '提交时间区间' : 'Submitted time range' }}</span>
          <el-date-picker
            v-model="submittedAtRange"
            type="datetimerange"
            value-format="YYYY-MM-DD[T]HH:mm:ss"
            format="YYYY-MM-DD HH:mm:ss"
            unlink-panels
            :range-separator="isChinese ? '至' : 'to'"
            :start-placeholder="isChinese ? '开始时间' : 'Start time'"
            :end-placeholder="isChinese ? '结束时间' : 'End time'"
            data-testid="parse-record-submitted-at-range"
          />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '排序字段' : 'Sort by' }}</span>
          <el-select v-model="form.sortBy" clearable data-testid="parse-record-sort-select">
            <el-option :label="isChinese ? '默认' : 'Default'" value="" />
            <el-option label="submittedAt" value="submittedAt" />
            <el-option label="createTime" value="createTime" />
            <el-option label="finishedAt" value="finishedAt" />
            <el-option label="status" value="status" />
            <el-option label="rowCount" value="rowCount" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '排序方向' : 'Sort order' }}</span>
          <el-select v-model="form.sortOrder" clearable>
            <el-option :label="isChinese ? '默认' : 'Default'" value="" />
            <el-option label="DESC" value="DESC" />
            <el-option label="ASC" value="ASC" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'Trace ID' : 'Trace ID' }}</span>
          <el-input v-model="form.traceId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'Task ID' : 'Task ID' }}</span>
          <el-input v-model="form.taskId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'Report ID' : 'Report ID' }}</span>
          <el-input v-model="form.reportId" />
        </label>
      </div>

      <div class="chip-row">
        <span class="chip">{{ isChinese ? 'History classification' : 'History classification' }}</span>
        <span class="chip">{{ isChinese ? 'Sort mode' : 'Sort mode' }}: {{ sortModeLabel }}</span>
        <span class="chip" data-testid="parse-record-page-mode">{{ hasLookupCriteria ? 'INDEXED' : 'PAGE' }}</span>
        <span v-if="datasourceOptionsLoadFailed" class="chip chip-warning" data-testid="parse-record-datasource-options-fallback">
          {{ isChinese ? '数据源候选加载失败，保留手动输入' : 'Datasource options unavailable; manual value allowed' }}
        </span>
      </div>
    </section>

    <section class="summary-grid">
      <article
        v-for="item in pageSummaryCards"
        :key="item.label"
        class="summary-card"
      >
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="surface-card table-panel">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">query history table</p>
          <h2 class="section-title">{{ isChinese ? '历史列表' : 'History list' }}</h2>
        </div>
        <div class="chip-row">
          <span
            v-for="(value, key) in classificationSummary"
            :key="key"
            class="chip"
          >
            {{ key }}: {{ typeof value === 'object' ? Object.keys(value).length : value }}
          </span>
        </div>
      </div>

      <el-table :data="rows" border>
        <el-table-column :label="isChinese ? 'History / Report' : 'History / Report'" min-width="220">
          <template #default="{ row }">
            <button
              type="button"
              class="table-link"
              data-testid="parse-record-trace-item"
              @click="openHistoryDetail(row.historyId)"
            >
              {{ row.reportCode || row.historyId }}
            </button>
            <div class="cell-subline">{{ row.historyId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="140" />
        <el-table-column prop="stageCode" :label="isChinese ? '阶段' : 'Stage'" min-width="110" />
        <el-table-column :label="isChinese ? '服务编码' : 'Service code'" min-width="150">
          <template #default="{ row }">{{ row.historyType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="resultStatus" :label="isChinese ? '状态' : 'Status'" min-width="120">
          <template #default="{ row }">
            <span :class="statusClass(row.resultStatus)">{{ row.resultStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="accessChannel" :label="isChinese ? '接入渠道' : 'Access channel'" min-width="130" />
        <el-table-column :label="isChinese ? '逻辑对象类型' : 'Logical objects'" min-width="160">
          <template #default="{ row }">{{ row.logicalObjectTypes?.join(', ') || '-' }}</template>
        </el-table-column>
        <el-table-column :label="isChinese ? '治理命中' : 'Governance hits'" min-width="170">
          <template #default="{ row }">
            {{ `cache:${row.cacheHit === true ? 'Y' : row.cacheHit === false ? 'N' : '-'} / rewrite:${row.rewriteApplied === true ? 'Y' : row.rewriteApplied === false ? 'N' : '-'} / accel:${row.accelerationApplied === true ? 'Y' : row.accelerationApplied === false ? 'N' : '-'}` }}
          </template>
        </el-table-column>
        <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="120" />
        <el-table-column prop="submittedAt" :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
          <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="isChinese ? '审计事件数' : 'Audit event count'" min-width="120">
          <template #default="{ row }">{{ row.auditEventCount ?? '-' }}</template>
        </el-table-column>
      </el-table>
    </section>

    <section class="surface-card batch-history-panel">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">batch history</p>
          <h2 class="section-title">{{ isChinese ? '批量解析与报表导入历史' : 'Batch parse and report-import history' }}</h2>
        </div>
        <div class="chip-row">
          <span class="chip">{{ isChinese ? '服务端持久化' : 'Server persisted' }}</span>
          <span class="chip">{{ isChinese ? '可回跳批量中心' : 'Deep links to batch center' }}</span>
        </div>
      </div>

      <div
        v-if="batchHistoryErrorMessage"
        class="inline-banner inline-banner-danger"
        data-testid="parse-record-batch-history-error"
      >
        {{ batchHistoryErrorMessage }}
      </div>

      <el-tabs v-model="batchHistoryTab">
        <el-tab-pane :label="isChinese ? '批量解析历史' : 'Batch parse history'" name="parse">
          <section class="summary-grid batch-history-summary">
            <article v-for="item in parseBatchHistorySummary" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </section>
          <el-table :data="parseBatchHistoryRows" border>
            <el-table-column prop="batchName" :label="isChinese ? '批次名称' : 'Batch name'" min-width="200">
              <template #default="{ row }">
                <button
                  type="button"
                  class="table-link"
                  data-testid="parse-record-batch-history-parse"
                  @click="openParseBatchCenter(row.batchId)"
                >
                  {{ row.batchName || row.batchId }}
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
            <el-table-column prop="importMode" :label="isChinese ? '导入模式' : 'Import mode'" min-width="140" />
            <el-table-column prop="fileType" :label="isChinese ? '文件类型' : 'File type'" min-width="120" />
            <el-table-column prop="totalRecords" :label="isChinese ? '总记录' : 'Total records'" min-width="110" />
            <el-table-column prop="successRecords" :label="isChinese ? '成功' : 'Success'" min-width="100" />
            <el-table-column prop="failedRecords" :label="isChinese ? '失败' : 'Failed'" min-width="100" />
            <el-table-column prop="createdAt" :label="isChinese ? '创建时间' : 'Created at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '报表导入历史' : 'Report import history'" name="report">
          <section class="summary-grid batch-history-summary">
            <article v-for="item in reportBatchHistorySummary" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </section>
          <el-table :data="reportBatchHistoryRows" border>
            <el-table-column prop="batchName" :label="isChinese ? '批次名称' : 'Batch name'" min-width="200">
              <template #default="{ row }">
                <button
                  type="button"
                  class="table-link"
                  data-testid="parse-record-batch-history-report"
                  @click="openReportBatchCenter(row.batchId)"
                >
                  {{ row.batchName || row.batchId }}
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
            <el-table-column prop="fileType" :label="isChinese ? '文件类型' : 'File type'" min-width="120" />
            <el-table-column prop="totalReports" :label="isChinese ? '报表总数' : 'Total reports'" min-width="120" />
            <el-table-column prop="resolvedReports" :label="isChinese ? '已解析' : 'Resolved'" min-width="110" />
            <el-table-column prop="failedReports" :label="isChinese ? '失败' : 'Failed'" min-width="100" />
            <el-table-column prop="createdAt" :label="isChinese ? '创建时间' : 'Created at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="detailDialogVisible"
      :title="selectedHistoryDetail?.reportCode || selectedHistoryDetail?.historyId || 'query history detail'"
      width="1120px"
    >
      <div v-if="selectedHistoryDetail" class="dialog-stack">
        <div class="dialog-header">
          <div class="banner-row">
            <strong data-testid="parse-record-detail-trace-id">{{ selectedHistoryDetail.traceId || '-' }}</strong>
            <span :class="statusClass(selectedHistoryDetail.resultStatus)">{{ selectedHistoryDetail.resultStatus || '-' }}</span>
          </div>
          <div class="dialog-actions">
            <el-button type="primary" @click="openRepairEvidence">{{ isChinese ? '打开修复证据' : 'Open repair evidence' }}</el-button>
            <el-button @click="openAuditForensics">{{ isChinese ? '打开审计取证' : 'Open audit forensics' }}</el-button>
            <el-button :loading="loading.export" data-testid="parse-record-export" @click="openExportDialog">{{ isChinese ? '导出取证' : 'Export evidence' }}</el-button>
            <el-button @click="evidenceDrawerVisible = true">{{ isChinese ? '查看原始证据' : 'View raw evidence' }}</el-button>
          </div>
        </div>

        <el-tabs v-model="activeDialogTab">
          <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
            <div class="detail-grid">
              <div
                v-for="item in detailSummaryCards"
                :key="item.label"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div v-if="traceSummaryCards.length" class="detail-grid detail-grid-secondary">
              <div
                v-for="item in traceSummaryCards"
                :key="item.label"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '关联历史' : 'Linked query histories'" name="histories">
            <p class="tab-copy">query history detail</p>
            <p class="tab-copy">SQL tri-state, parse signals, and related forensics</p>
            <el-table :data="traceQueryHistories" border>
              <el-table-column prop="historyId" label="History ID" min-width="180" />
              <el-table-column prop="reportCode" :label="isChinese ? '报表编码' : 'Report code'" min-width="180" />
              <el-table-column prop="historyType" :label="isChinese ? '类型' : 'Type'" min-width="140" />
              <el-table-column :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
                <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
              </el-table-column>
            </el-table>
            <p
              v-if="!traceQueryHistories.length"
              class="empty-copy"
              data-testid="parse-record-detail-query-history-count"
            >
              0
            </p>
            <p v-else class="empty-copy" data-testid="parse-record-detail-query-history-count">
              {{ traceQueryHistories.length }}
            </p>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? 'SQL 三态' : 'SQL tri-state'" name="sql">
            <div class="detail-grid">
              <div
                v-for="item in sqlStateHighlights"
                :key="item.key"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong :data-testid="`parse-record-history-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                  {{ displayValue(item.value) }}
                </strong>
              </div>
            </div>
            <div class="code-grid">
              <article
                v-for="item in sqlVariants"
                :key="item.key"
                class="code-card"
              >
                <div class="code-card__header">
                  <span>{{ item.label }}</span>
                </div>
                <pre class="code-block" :data-testid="`parse-record-${item.key}`">{{ item.value }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '解析与路由' : 'Parse and route signals'" name="signals">
            <div class="code-grid">
              <article
                v-for="group in signalGroups"
                :key="group.key"
                class="code-card"
              >
                <div class="code-card__header">
                  <span>{{ group.title }}</span>
                </div>
                <pre class="code-block" :data-testid="`parse-record-${group.key}`">{{ formatJson(group.payload) }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '关联证据' : 'References'" name="refs">
            <div v-if="logicalObjectHits.length" class="detail-grid">
              <div
                v-for="(item, index) in logicalObjectHits"
                :key="`${item.objectKey || item.logicalObjectKey || index}`"
                class="detail-grid__item"
              >
                <span>{{ item.objectType || item.logicalObjectType || 'OBJECT' }}</span>
                <strong>{{ item.objectKey || item.logicalObjectKey || item.objectName || '-' }}</strong>
              </div>
            </div>

            <div v-if="referenceGroups.length" class="code-grid">
              <article
                v-for="group in referenceGroups"
                :key="group.key"
                class="code-card"
              >
                <div class="code-card__header">
                  <span>{{ group.title }}</span>
                </div>
                <pre class="code-block">{{ formatJson(group.items) }}</pre>
              </article>
            </div>

            <el-table :data="auditEvents" border>
              <el-table-column prop="serviceCode" :label="isChinese ? '服务' : 'Service'" min-width="150" />
              <el-table-column prop="operationType" :label="isChinese ? '操作' : 'Operation'" min-width="160" />
              <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
              <el-table-column :label="isChinese ? '时间' : 'Created at'" min-width="170">
                <template #default="{ row }">{{ formatTimestamp(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>

    <el-drawer v-model="evidenceDrawerVisible" :title="isChinese ? '原始证据' : 'Raw evidence'" size="44%">
      <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="exportDialogVisible" :title="isChinese ? '导出取证' : 'Export evidence'" width="720px">
      <div class="dialog-stack">
        <div class="field-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '导出格式' : 'Export format' }}</span>
            <el-select v-model="exportForm.exportFormat">
              <el-option label="JSON" value="JSON" />
              <el-option label="CSV" value="CSV" />
              <el-option label="EXCEL" value="EXCEL" />
              <el-option label="SQL_TEXT" value="SQL_TEXT" />
              <el-option label="PDF_REPORT" value="PDF_REPORT" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '导出原因' : 'Export reason' }}</span>
            <el-input v-model="exportForm.exportReason" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '携带 Trace 明细' : 'Include trace detail' }}</span>
            <el-switch v-model="exportForm.includeTraceDetail" />
          </label>
        </div>
        <div class="dialog-actions">
          <el-button type="primary" :loading="loading.export" @click="runExport">{{ isChinese ? '执行导出' : 'Run export' }}</el-button>
        </div>
        <div v-if="exportResult" class="code-grid">
          <article class="code-card">
            <div class="code-card__header">
              <span>{{ isChinese ? '导出摘要' : 'Export summary' }}</span>
            </div>
            <pre class="code-block" data-testid="parse-record-export-result">{{ formatJson(exportResult) }}</pre>
          </article>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.history-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.surface-card,
.summary-card,
.field-block,
.detail-grid__item,
.code-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    var(--sqlforge-surface-2);
}

.page-shell,
.filter-panel,
.table-panel,
.batch-history-panel {
  padding: 20px;
}

.page-shell,
.action-row,
.field-grid,
.summary-grid,
.chip-row,
.table-heading,
.dialog-header,
.dialog-actions {
  display: flex;
  gap: 12px;
}

.page-shell {
  align-items: flex-start;
  justify-content: space-between;
}

.section-kicker,
.field-label,
.summary-card-label,
.tab-copy {
  margin: 0 0 6px;
  color: var(--sqlforge-text-muted);
}

.section-title,
.section-summary {
  margin: 0;
}

.section-summary,
.cell-subline,
.empty-copy {
  color: var(--sqlforge-text-secondary);
}

.inline-banner {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.action-row {
  align-items: center;
  flex-wrap: wrap;
}

.field-grid {
  flex-wrap: wrap;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  min-width: 210px;
  flex: 1 1 210px;
}

.field-block-wide {
  flex-basis: 430px;
}

.field-block :deep(.el-select),
.field-block :deep(.el-date-editor) {
  width: 100%;
}

.chip-row {
  margin-top: 14px;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.chip-warning {
  border-color: rgba(245, 158, 11, 0.55);
  color: #fbbf24;
}

.summary-grid {
  flex-wrap: wrap;
}

.summary-card {
  padding: 14px;
  min-width: 170px;
  flex: 1 1 170px;
}

.table-heading,
.dialog-header {
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.table-link {
  border: none;
  background: transparent;
  color: var(--sqlforge-color-brand);
  padding: 0;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.cell-subline {
  margin-top: 4px;
  font-size: 12px;
}

.dialog-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.banner-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-actions {
  flex-wrap: wrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid-secondary {
  margin-top: 12px;
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
}

.code-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.code-card {
  padding: 14px;
}

.code-card__header {
  margin-bottom: 8px;
  color: var(--sqlforge-text-secondary);
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.pill-success {
  background: rgba(34, 197, 94, 0.12);
  color: #86efac;
}

.pill-warning {
  background: rgba(250, 204, 21, 0.12);
  color: #fde68a;
}

.pill-danger {
  background: rgba(248, 113, 113, 0.12);
  color: #fecaca;
}

@media (max-width: 1280px) {
  .page-shell,
  .table-heading,
  .dialog-header {
    flex-direction: column;
  }

  .detail-grid,
  .code-grid {
    grid-template-columns: 1fr;
  }
}
</style>
