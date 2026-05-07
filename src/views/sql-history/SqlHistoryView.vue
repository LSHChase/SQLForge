<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  exportGovernanceQueryHistory,
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceQueryHistoryPage,
  getGovernanceTraceDetail,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'

const { locale } = useI18n()
const route = useRoute()
const router = useRouter()

const SQL_EXECUTION_HISTORY_TYPE = 'QUERY_EXECUTION'
const DEFAULT_HISTORY_CONTEXT_TENANT_ID = 'tenant-a'
const LIST_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]

const normalizeQueryValue = value => String(value || '').trim()

const form = reactive({
  tenantId: '',
  reportCode: '',
  datasourceCode: '',
  status: '',
  accessChannel: '',
  engine: '',
  submittedBy: '',
  cacheHit: '',
  rewriteApplied: '',
  accelerationApplied: '',
  sortBy: '',
  sortOrder: '',
  traceId: '',
  taskId: '',
  reportId: ''
})

const pagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
  pageCount: 0
})

const loading = reactive({
  page: false,
  detail: false,
  lookup: false,
  export: false
})

const page = ref(null)
const selectedHistoryDetail = ref(null)
const detailDrawerVisible = ref(false)
const evidenceDrawerVisible = ref(false)
const exportDialogVisible = ref(false)
const exportResult = ref(null)
const activeDetailTab = ref('overview')
const errorMessage = ref('')

const exportForm = reactive({
  exportFormat: 'JSON',
  includeTraceDetail: true,
  exportReason: 'frontend-sql-history-forensics'
})

const isChinese = computed(() => locale.value === 'zh-CN')
const rows = computed(() => page.value?.items || [])
const routeTenantId = computed(() => normalizeQueryValue(route.query.tenantId))
const requestTenantId = computed(() => normalizeQueryValue(form.tenantId) || routeTenantId.value || DEFAULT_HISTORY_CONTEXT_TENANT_ID)
const classificationSummary = computed(() => page.value?.classificationSummary || {})
const statusCounts = computed(() => classificationSummary.value.statusCounts || {})
const accessChannelCounts = computed(() => classificationSummary.value.accessChannelCounts || {})
const hasLookupCriteria = computed(() =>
  Boolean(normalizeQueryValue(form.traceId) || normalizeQueryValue(form.taskId) || normalizeQueryValue(form.reportId))
)
const auditEvents = computed(() => selectedHistoryDetail.value?.traceDetail?.auditEvents || [])
const traceQueryHistories = computed(() => selectedHistoryDetail.value?.traceDetail?.queryHistories || [])
const executionSummary = computed(() => objectValue(selectedHistoryDetail.value?.executionSummary))
const sqlState = computed(() => objectValue(selectedHistoryDetail.value?.sqlState))
const parseEvidence = computed(() => ({
  structureParseSummary: selectedHistoryDetail.value?.structureParseSummary,
  accessParseSummary: selectedHistoryDetail.value?.accessParseSummary,
  queryContext: selectedHistoryDetail.value?.queryContext
}))

const summaryCards = computed(() => [
  { label: isChinese.value ? '当前页记录' : 'Current page', value: rows.value.length },
  { label: isChinese.value ? '执行历史总数' : 'Execution records', value: pagination.totalCount },
  { label: isChinese.value ? '成功' : 'Success', value: statusCounts.value.SUCCESS || statusCounts.value.SUCCEEDED || 0 },
  { label: isChinese.value ? '异常/部分成功' : 'Non-success', value: Number(statusCounts.value.PARTIAL || 0) + Number(statusCounts.value.FAILED || 0) },
  { label: isChinese.value ? '接入渠道' : 'Access channels', value: Object.keys(accessChannelCounts.value).length }
])

const detailCards = computed(() => {
  const detail = selectedHistoryDetail.value
  if (!detail) {
    return []
  }
  return [
    { label: 'History ID', value: detail.historyId, testId: 'sql-history-detail-history-id' },
    { label: 'Trace ID', value: detail.traceId, testId: 'sql-history-detail-trace-id' },
    { label: isChinese.value ? 'SQL/报表标识' : 'SQL/report key', value: detail.reportCode || detail.sqlFingerprint },
    { label: isChinese.value ? '数据源' : 'Datasource', value: detail.datasourceCode },
    { label: isChinese.value ? '执行状态' : 'Execution status', value: detail.resultStatus, testId: 'sql-history-detail-status' },
    { label: isChinese.value ? '接入渠道' : 'Access channel', value: detail.accessChannel },
    { label: isChinese.value ? '目标引擎' : 'Target engine', value: detail.targetEngine, testId: 'sql-history-detail-target-engine' },
    { label: isChinese.value ? '提交人' : 'Submitted by', value: detail.submittedBy },
    { label: isChinese.value ? '提交时间' : 'Submitted at', value: formatTimestamp(detail.submittedAt) },
    { label: isChinese.value ? '审计事件数' : 'Audit event count', value: detail.traceDetail?.auditEventCount ?? auditEvents.value.length, testId: 'sql-history-detail-audit-count' }
  ]
})

const executionCards = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  return [
    { label: isChinese.value ? '缓存命中' : 'Cache hit', value: booleanDisplay(firstValue(executionSummary.value.cacheHit, detail.cacheHit)) },
    { label: isChinese.value ? '轻量改写' : 'Rewrite applied', value: booleanDisplay(firstValue(executionSummary.value.rewriteApplied, detail.rewriteApplied)) },
    { label: isChinese.value ? '加速命中' : 'Acceleration applied', value: booleanDisplay(firstValue(executionSummary.value.accelerationApplied, detail.accelerationApplied)) },
    { label: isChinese.value ? '返回行数' : 'Returned rows', value: firstValue(executionSummary.value.returnedRowCount, detail.returnedRowCount) },
    { label: isChinese.value ? '错误码' : 'Error code', value: firstValue(executionSummary.value.errorCode, detail.errorCode) },
    { label: isChinese.value ? '错误信息' : 'Error message', value: firstValue(executionSummary.value.errorMessage, detail.errorMessage) }
  ].filter(item => hasDisplayValue(item.value))
})

const sqlCards = computed(() => [
  { label: isChinese.value ? '执行指纹' : 'SQL fingerprint', value: firstValue(sqlState.value.sqlFingerprint, selectedHistoryDetail.value?.sqlFingerprint), testId: 'sql-history-detail-sql-fingerprint' },
  { label: isChinese.value ? '模板指纹' : 'Template fingerprint', value: firstValue(sqlState.value.sqlTemplateFingerprint, selectedHistoryDetail.value?.sqlTemplateFingerprint) },
  { label: isChinese.value ? '绑定指纹' : 'Bound fingerprint', value: firstValue(sqlState.value.boundSqlFingerprint, selectedHistoryDetail.value?.boundSqlFingerprint) },
  { label: isChinese.value ? '绑定模式' : 'Binding mode', value: firstValue(sqlState.value.bindingMode, selectedHistoryDetail.value?.bindingMode) },
  { label: isChinese.value ? '渲染状态' : 'Binding render', value: firstValue(sqlState.value.bindingRenderStatus, selectedHistoryDetail.value?.bindingRenderStatus) }
])

const sqlVariants = computed(() =>
  [
    { key: 'sqlText', label: isChinese.value ? '原始 SQL' : 'Original SQL', value: selectedHistoryDetail.value?.sqlText },
    { key: 'sqlTemplateText', label: isChinese.value ? '模板 SQL' : 'Template SQL', value: selectedHistoryDetail.value?.sqlTemplateText },
    { key: 'boundSqlText', label: isChinese.value ? '绑定 SQL' : 'Bound SQL', value: selectedHistoryDetail.value?.boundSqlText }
  ].filter(item => hasDisplayValue(item.value))
)

const parseBooleanFilter = value => {
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  return undefined
}

const loadPage = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    page.value = await getGovernanceQueryHistoryPage(
      {
        tenantId: normalizeQueryValue(form.tenantId),
        requestTenantId: requestTenantId.value,
        historyType: SQL_EXECUTION_HISTORY_TYPE,
        reportCode: form.reportCode,
        datasourceCode: form.datasourceCode,
        status: form.status,
        accessChannel: form.accessChannel,
        engine: form.engine,
        submittedBy: form.submittedBy,
        cacheHit: parseBooleanFilter(form.cacheHit),
        rewriteApplied: parseBooleanFilter(form.rewriteApplied),
        accelerationApplied: parseBooleanFilter(form.accelerationApplied),
        sortBy: form.sortBy,
        sortOrder: form.sortOrder,
        pageNo: pagination.pageNo,
        pageSize: pagination.pageSize
      },
      {
        requestPrefix: 'frontend-sql-history-page'
      }
    )
    applyPagination(page.value)
  } catch (error) {
    page.value = null
    pagination.totalCount = 0
    pagination.pageCount = 0
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const applyPagination = payload => {
  pagination.pageNo = Number(payload?.pageNo || pagination.pageNo || 1)
  pagination.pageSize = Number(payload?.pageSize || pagination.pageSize || 10)
  pagination.totalCount = Number(payload?.totalCount || 0)
  pagination.pageCount = Number(payload?.pageCount || 0)
}

const search = async () => {
  pagination.pageNo = 1
  await loadPage()
}

const clearFilters = async () => {
  form.tenantId = ''
  form.reportCode = ''
  form.datasourceCode = ''
  form.status = ''
  form.accessChannel = ''
  form.engine = ''
  form.submittedBy = ''
  form.cacheHit = ''
  form.rewriteApplied = ''
  form.accelerationApplied = ''
  form.sortBy = ''
  form.sortOrder = ''
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  await search()
}

const handlePageChange = async pageNo => {
  pagination.pageNo = pageNo
  await loadPage()
}

const handlePageSizeChange = async pageSize => {
  pagination.pageSize = pageSize
  pagination.pageNo = 1
  await loadPage()
}

const openHistoryDetail = async historyId => {
  const normalizedHistoryId = normalizeQueryValue(historyId)
  if (!normalizedHistoryId) {
    return
  }
  loading.detail = true
  errorMessage.value = ''
  activeDetailTab.value = 'overview'
  try {
    selectedHistoryDetail.value = await getGovernanceQueryHistoryDetail(requestTenantId.value, normalizedHistoryId, {
      requestPrefix: 'frontend-sql-history-detail'
    })
    detailDrawerVisible.value = true
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
        requestPrefix: 'frontend-sql-history-lookup'
      }
    )
    const firstTraceId = lookupPage?.items?.[0]?.traceId
    if (!firstTraceId) {
      errorMessage.value = isChinese.value ? '没有命中记录。' : 'No history matched the lookup criteria.'
      return
    }
    const traceDetail = await getGovernanceTraceDetail(requestTenantId.value, firstTraceId, 20, {
      requestPrefix: 'frontend-sql-history-trace-detail'
    })
    const executionHistory = (traceDetail?.queryHistories || [])
      .find(item => item.historyType === SQL_EXECUTION_HISTORY_TYPE)
    if (!executionHistory?.historyId) {
      errorMessage.value = isChinese.value
        ? '命中了 trace，但没有 QUERY_EXECUTION 执行历史。'
        : 'A trace was found but no QUERY_EXECUTION history is available.'
      return
    }
    await openHistoryDetail(executionHistory.historyId)
    if (selectedHistoryDetail.value && !selectedHistoryDetail.value.traceDetail) {
      selectedHistoryDetail.value.traceDetail = traceDetail
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lookup = false
  }
}

const openRouteDeepLink = async () => {
  const historyId = normalizeQueryValue(route.query.historyId)
  if (historyId) {
    await openHistoryDetail(historyId)
    return
  }
  form.traceId = normalizeQueryValue(route.query.traceId)
  form.taskId = normalizeQueryValue(route.query.taskId)
  form.reportId = normalizeQueryValue(route.query.reportId)
  if (hasLookupCriteria.value) {
    await runIndexedLookup()
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
        requestPrefix: 'frontend-sql-history-export'
      }
    )
  } catch (error) {
    exportResult.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.export = false
  }
}

const statusClass = value => {
  const normalized = String(value || '').toUpperCase()
  if (!normalized) {
    return 'pill'
  }
  if (normalized === 'SUCCESS' || normalized === 'SUCCEEDED') {
    return 'pill pill-success'
  }
  if (normalized.includes('PARTIAL') || normalized.includes('PENDING')) {
    return 'pill pill-warning'
  }
  return 'pill pill-danger'
}

const governanceHitText = row =>
  `cache:${booleanShort(row.cacheHit)} / rewrite:${booleanShort(row.rewriteApplied)} / accel:${booleanShort(row.accelerationApplied)}`

const booleanShort = value => {
  if (value === true) {
    return 'Y'
  }
  if (value === false) {
    return 'N'
  }
  return '-'
}

const booleanDisplay = value => {
  if (value === true || value === 'true') {
    return 'true'
  }
  if (value === false || value === 'false') {
    return 'false'
  }
  return ''
}

const firstValue = (...values) => {
  const match = values.find(value => hasDisplayValue(value))
  return match === undefined ? '' : match
}

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

const objectValue = value => {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value
  }
  return {}
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

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const formatJson = value => JSON.stringify(value, null, 2)

onMounted(async () => {
  form.tenantId = routeTenantId.value
  await loadPage()
  await openRouteDeepLink()
})

watch(
  () => route.fullPath,
  async () => {
    form.tenantId = routeTenantId.value
    await openRouteDeepLink()
  }
)
</script>

<template>
  <section class="sql-history-page" data-testid="sql-history-page">
    <header class="page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">QUERY_EXECUTION history</p>
        <h1 class="section-title">{{ isChinese ? 'SQL 执行历史' : 'SQL execution history' }}</h1>
        <p class="section-summary">
          {{ isChinese ? '仅展示 QUERY_EXECUTION 执行记录，解析历史保留在解析历史查询入口。' : 'Shows only QUERY_EXECUTION records; parse histories stay in the parse-record entry.' }}
        </p>
      </div>
      <div class="action-row">
        <el-button type="primary" :loading="loading.page" data-testid="sql-history-refresh" @click="search">
          {{ isChinese ? '刷新列表' : 'Refresh list' }}
        </el-button>
        <el-button :loading="loading.lookup" data-testid="sql-history-run-lookup" @click="runIndexedLookup">
          {{ isChinese ? '精确反查' : 'Indexed lookup' }}
        </el-button>
        <el-button @click="clearFilters">{{ isChinese ? '清空条件' : 'Clear filters' }}</el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger" data-testid="sql-history-error">
      {{ errorMessage }}
    </div>

    <section class="filter-panel">
      <div class="field-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="form.tenantId" data-testid="sql-history-tenant-filter" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'SQL/报表标识' : 'SQL/report key' }}</span>
          <el-input v-model="form.reportCode" data-testid="sql-history-report-filter" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
          <el-input v-model="form.datasourceCode" data-testid="sql-history-datasource-filter" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '执行状态' : 'Execution status' }}</span>
          <el-select v-model="form.status" data-testid="sql-history-status-filter">
            <el-option label="ALL" value="" />
            <el-option label="SUCCESS" value="SUCCESS" />
            <el-option label="PARTIAL" value="PARTIAL" />
            <el-option label="FAILED" value="FAILED" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '接入渠道' : 'Access channel' }}</span>
          <el-select v-model="form.accessChannel" data-testid="sql-history-access-channel-filter">
            <el-option label="ALL" value="" />
            <el-option label="PAGE" value="PAGE" />
            <el-option label="API" value="API" />
            <el-option label="JDBC_AGENT" value="JDBC_AGENT" />
            <el-option label="SDK" value="SDK" />
            <el-option label="CLIENT" value="CLIENT" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '目标引擎' : 'Target engine' }}</span>
          <el-select v-model="form.engine" data-testid="sql-history-engine-filter">
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
          <el-select v-model="form.cacheHit">
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
          <span class="field-label">{{ isChinese ? '排序字段' : 'Sort by' }}</span>
          <el-select v-model="form.sortBy" clearable>
            <el-option :label="isChinese ? '默认' : 'Default'" value="" />
            <el-option label="submittedAt" value="submittedAt" />
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
          <span class="field-label">Trace ID</span>
          <el-input v-model="form.traceId" data-testid="sql-history-trace-id" />
        </label>
        <label class="field-block">
          <span class="field-label">Task ID</span>
          <el-input v-model="form.taskId" data-testid="sql-history-task-id" />
        </label>
        <label class="field-block">
          <span class="field-label">Report ID</span>
          <el-input v-model="form.reportId" data-testid="sql-history-report-id" />
        </label>
      </div>
      <div class="chip-row">
        <span class="chip" data-testid="sql-history-history-type">{{ SQL_EXECUTION_HISTORY_TYPE }}</span>
        <span class="chip">{{ isChinese ? '反查模式' : 'Lookup mode' }}: {{ hasLookupCriteria ? 'INDEXED' : 'PAGE' }}</span>
      </div>
    </section>

    <section class="summary-grid">
      <article v-for="item in summaryCards" :key="item.label" class="summary-card">
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="table-panel" data-testid="sql-history-query-history-table">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">execution table</p>
          <h2 class="section-title">{{ isChinese ? '执行记录' : 'Execution records' }}</h2>
        </div>
      </div>

      <el-table :data="rows" border>
        <el-table-column label="History ID" min-width="210">
          <template #default="{ row }">
            <button type="button" class="table-link" data-testid="sql-history-trace-item" @click="openHistoryDetail(row.historyId)">
              {{ row.historyId }}
            </button>
            <div class="cell-subline">{{ row.traceId || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="isChinese ? 'SQL/报表标识' : 'SQL/report key'" min-width="180">
          <template #default="{ row }">{{ row.reportCode || row.sqlFingerprint || '-' }}</template>
        </el-table-column>
        <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="130" />
        <el-table-column prop="resultStatus" :label="isChinese ? '执行状态' : 'Execution status'" min-width="120">
          <template #default="{ row }">
            <span :class="statusClass(row.resultStatus)">{{ row.resultStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="accessChannel" :label="isChinese ? '接入渠道' : 'Access channel'" min-width="130" />
        <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="120" />
        <el-table-column :label="isChinese ? '治理命中' : 'Governance hits'" min-width="190">
          <template #default="{ row }">{{ governanceHitText(row) }}</template>
        </el-table-column>
        <el-table-column prop="submittedBy" :label="isChinese ? '提交人' : 'Submitted by'" min-width="120" />
        <el-table-column prop="submittedAt" :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
          <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column :label="isChinese ? '审计事件数' : 'Audit event count'" min-width="120">
          <template #default="{ row }">{{ row.auditEventCount ?? '-' }}</template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination-row"
        layout="total, sizes, prev, pager, next"
        :total="pagination.totalCount"
        :page-sizes="LIST_PAGE_SIZE_OPTIONS"
        :page-size="pagination.pageSize"
        :current-page="pagination.pageNo"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </section>

    <el-drawer
      v-model="detailDrawerVisible"
      :title="selectedHistoryDetail?.historyId || (isChinese ? '执行详情' : 'Execution detail')"
      size="70%"
      data-testid="sql-history-detail-drawer"
    >
      <div v-if="selectedHistoryDetail" class="drawer-stack">
        <div class="dialog-header">
          <div class="banner-row">
            <strong data-testid="sql-history-detail-service-code">{{ selectedHistoryDetail.historyType || '-' }}</strong>
            <span :class="statusClass(selectedHistoryDetail.resultStatus)">{{ selectedHistoryDetail.resultStatus || '-' }}</span>
          </div>
          <div class="action-row">
            <el-button type="primary" @click="openRepairEvidence">{{ isChinese ? '打开修复证据' : 'Open repair evidence' }}</el-button>
            <el-button @click="openAuditForensics">{{ isChinese ? '打开审计取证' : 'Open audit forensics' }}</el-button>
            <el-button :loading="loading.export" data-testid="sql-history-export" @click="openExportDialog">
              {{ isChinese ? '导出取证' : 'Export evidence' }}
            </el-button>
            <el-button @click="evidenceDrawerVisible = true">{{ isChinese ? '查看原始证据' : 'View raw evidence' }}</el-button>
          </div>
        </div>

        <el-tabs v-model="activeDetailTab" data-testid="sql-history-detail-tabs">
          <el-tab-pane :label="isChinese ? '执行概览' : 'Execution overview'" name="overview">
            <div class="detail-grid">
              <div v-for="item in detailCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="item.testId || undefined">{{ displayValue(item.value) }}</strong>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '执行取证' : 'Execution evidence'" name="execution">
            <div class="detail-grid">
              <div v-for="item in executionCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div class="code-grid">
              <article v-if="isNonEmpty(selectedHistoryDetail.routeDecision)" class="code-card">
                <div class="code-card__header">{{ isChinese ? '路由决策' : 'Route decision' }}</div>
                <pre class="code-block">{{ formatJson(selectedHistoryDetail.routeDecision) }}</pre>
              </article>
              <article v-if="isNonEmpty(selectedHistoryDetail.cacheSummary)" class="code-card">
                <div class="code-card__header">{{ isChinese ? '缓存摘要' : 'Cache summary' }}</div>
                <pre class="code-block">{{ formatJson(selectedHistoryDetail.cacheSummary) }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? 'SQL 三态' : 'SQL tri-state'" name="sql">
            <div class="detail-grid">
              <div v-for="item in sqlCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="item.testId || undefined">{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div class="code-grid">
              <article v-for="item in sqlVariants" :key="item.key" class="code-card">
                <div class="code-card__header">{{ item.label }}</div>
                <SqlCodeBlock :value="item.value" :label="item.label" :copy-label="isChinese ? '复制' : 'Copy'" />
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '解析证据' : 'Parse evidence'" name="parseEvidence">
            <p class="tab-copy">
              {{ isChinese ? '解析信息作为执行详情的附属证据展示，不作为 SQL 历史默认视角。' : 'Parse information is shown as supporting evidence, not as the default SQL history view.' }}
            </p>
            <div class="code-grid">
              <article v-for="(payload, key) in parseEvidence" :key="key" class="code-card">
                <div class="code-card__header">{{ key }}</div>
                <pre class="code-block">{{ formatJson(payload || {}) }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '审计关联' : 'Audit links'" name="audit">
            <p class="tab-copy" data-testid="sql-history-detail-query-history-count">{{ traceQueryHistories.length }}</p>
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
    </el-drawer>

    <el-drawer
      v-model="evidenceDrawerVisible"
      :title="isChinese ? '原始证据' : 'Raw evidence'"
      size="56%"
    >
      <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>

    <el-dialog
      v-model="exportDialogVisible"
      :title="isChinese ? '导出 SQL 执行取证' : 'Export SQL execution evidence'"
      width="720px"
    >
      <div class="dialog-stack">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '格式' : 'Format' }}</span>
          <el-select v-model="exportForm.exportFormat">
            <el-option label="JSON" value="JSON" />
            <el-option label="MARKDOWN" value="MARKDOWN" />
          </el-select>
        </label>
        <el-checkbox v-model="exportForm.includeTraceDetail">{{ isChinese ? '包含 trace 详情' : 'Include trace detail' }}</el-checkbox>
        <el-button type="primary" :loading="loading.export" @click="runExport">{{ isChinese ? '执行导出' : 'Run export' }}</el-button>
        <pre v-if="exportResult" class="code-block" data-testid="sql-history-export-result">{{ formatJson(exportResult) }}</pre>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.sql-history-page,
.drawer-stack,
.dialog-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sql-history-page {
  padding: 24px;
}

.page-shell,
.filter-panel,
.table-panel {
  background: #fff;
  border: 1px solid #d9e2ec;
  border-radius: 8px;
  padding: 20px;
}

.page-shell,
.table-heading,
.dialog-header,
.banner-row,
.action-row,
.chip-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.section-kicker,
.field-label,
.summary-card-label {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}

.section-title {
  margin: 4px 0;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.25;
}

.section-summary,
.tab-copy,
.cell-subline {
  margin: 0;
  color: #475569;
  line-height: 1.6;
}

.field-grid,
.summary-grid,
.detail-grid,
.code-grid {
  display: grid;
  gap: 12px;
}

.field-grid {
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
}

.summary-grid,
.detail-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.code-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-card,
.detail-grid__item,
.code-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px;
}

.summary-card strong,
.detail-grid__item strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  overflow-wrap: anywhere;
}

.chip,
.pill {
  display: inline-flex;
  align-items: center;
  border-radius: 999px;
  background: #eef2ff;
  color: #3730a3;
  padding: 4px 10px;
  font-size: 12px;
  line-height: 1.4;
}

.pill-success {
  background: #dcfce7;
  color: #166534;
}

.pill-warning {
  background: #fef3c7;
  color: #92400e;
}

.pill-danger {
  background: #fee2e2;
  color: #991b1b;
}

.inline-banner {
  border-radius: 8px;
  padding: 12px 14px;
}

.inline-banner-danger {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.table-link {
  border: 0;
  background: transparent;
  color: #1d4ed8;
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.table-link:hover {
  text-decoration: underline;
}

.pagination-row {
  margin-top: 16px;
  justify-content: flex-end;
}

.code-card__header {
  color: #334155;
  font-weight: 700;
  margin-bottom: 8px;
}

.code-block {
  background: #0f172a;
  border-radius: 8px;
  color: #e2e8f0;
  margin: 0;
  overflow: auto;
  padding: 12px;
  white-space: pre-wrap;
}

@media (max-width: 720px) {
  .sql-history-page {
    padding: 16px;
  }

  .page-shell,
  .table-heading,
  .dialog-header,
  .action-row {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
