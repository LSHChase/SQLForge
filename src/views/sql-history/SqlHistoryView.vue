<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  exportGovernanceQueryHistory,
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceTraceDetail,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'
import { engineOptions } from '../common/formComponentGovernance'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import { useSqlHistoryList } from './useSqlHistoryList'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const SQL_EXECUTION_HISTORY_TYPE = 'QUERY_EXECUTION'
const PAGE_KICKER = 'QUERY_EXECUTION history'
const LIST_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]

const statusValueOptions = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'PARTIAL', value: 'PARTIAL' },
  { label: 'FAILED', value: 'FAILED' }
]

const accessChannelValueOptions = [
  { label: 'PAGE', value: 'PAGE' },
  { label: 'API', value: 'API' },
  { label: 'JDBC_AGENT', value: 'JDBC_AGENT' },
  { label: 'SDK', value: 'SDK' },
  { label: 'CLIENT', value: 'CLIENT' }
]

const booleanValueOptions = [
  { label: 'true', value: 'true' },
  { label: 'false', value: 'false' }
]

const sortFieldValueOptions = [
  { label: 'submittedAt', value: 'submittedAt' },
  { label: 'finishedAt', value: 'finishedAt' },
  { label: 'status', value: 'status' },
  { label: 'rowCount', value: 'rowCount' }
]

const sortOrderValueOptions = [
  { label: 'DESC', value: 'DESC' },
  { label: 'ASC', value: 'ASC' }
]

const exportFormatOptions = [
  { label: 'JSON', value: 'JSON' },
  { label: 'MARKDOWN', value: 'MARKDOWN' }
]

const loading = reactive({
  detail: false,
  lookup: false,
  export: false
})

const selectedHistoryDetail = ref(null)
const detailDrawerVisible = ref(false)
const evidenceDrawerVisible = ref(false)
const exportDialogVisible = ref(false)
const exportResult = ref(null)
const activeDetailTab = ref('overview')
const workflowErrorMessage = ref('')

const exportForm = reactive({
  exportFormat: 'JSON',
  includeTraceDetail: true,
  exportReason: 'frontend-sql-history-forensics'
})

const routeTenantId = computed(() => String(route.query.tenantId || '').trim())
const {
  searchForm,
  pageInfo,
  loadingList,
  tableRows,
  classificationSummary,
  listErrorMessage,
  listStatus,
  lastQueryAt,
  requestTenantId,
  hasLookupCriteria,
  pageWindow,
  currentTenantOptions,
  currentDatasourceOptions,
  datasourceOptionsLoadFailed,
  initializeList,
  search: searchList,
  clearFilters: clearListFilters,
  handlePageChange,
  handlePageSizeChange,
  syncRouteTenant,
  normalizeQueryValue
} = useSqlHistoryList({
  routeTenantId,
  historyType: SQL_EXECUTION_HISTORY_TYPE
})
const statusCounts = computed(() => classificationSummary.value.statusCounts || {})
const accessChannelCounts = computed(() => classificationSummary.value.accessChannelCounts || {})
const auditEvents = computed(() => selectedHistoryDetail.value?.traceDetail?.auditEvents || [])
const executionSummary = computed(() => objectValue(selectedHistoryDetail.value?.executionSummary))
const sqlState = computed(() => objectValue(selectedHistoryDetail.value?.sqlState))

const withAllOption = options => [
  { label: t('sqlHistory.options.all'), value: '' },
  ...options
]

const withDefaultOption = options => [
  { label: t('sqlHistory.options.default'), value: '' },
  ...options
]

const statusFilterOptions = computed(() => withAllOption(statusValueOptions))
const accessChannelOptions = computed(() => withAllOption(accessChannelValueOptions))
const targetEngineOptions = computed(() => withAllOption(engineOptions))
const booleanFilterOptions = computed(() => withAllOption(booleanValueOptions))
const sortFieldOptions = computed(() => withDefaultOption(sortFieldValueOptions))
const sortOrderOptions = computed(() => withDefaultOption(sortOrderValueOptions))

const searchFields = computed(() => [
  {
    key: 'tenantId',
    type: 'select',
    label: t('sqlHistory.filters.tenant'),
    placeholder: t('sqlHistory.filters.tenantPlaceholder'),
    options: currentTenantOptions.value,
    filterable: true,
    allowCreate: true,
    defaultFirstOption: true,
    testId: 'sql-history-tenant-filter'
  },
  {
    key: 'reportCode',
    type: 'input',
    label: t('sqlHistory.filters.reportKey'),
    placeholder: t('sqlHistory.filters.reportKeyPlaceholder'),
    testId: 'sql-history-report-filter'
  },
  {
    key: 'datasourceCode',
    type: 'select',
    label: t('sqlHistory.filters.datasource'),
    placeholder: t('sqlHistory.filters.datasourcePlaceholder'),
    options: currentDatasourceOptions.value,
    filterable: true,
    allowCreate: true,
    defaultFirstOption: true,
    testId: 'sql-history-datasource-filter'
  },
  {
    key: 'status',
    type: 'select',
    label: t('sqlHistory.filters.status'),
    options: statusFilterOptions.value,
    testId: 'sql-history-status-filter'
  },
  {
    key: 'accessChannel',
    type: 'select',
    label: t('sqlHistory.filters.accessChannel'),
    options: accessChannelOptions.value,
    testId: 'sql-history-access-channel-filter'
  },
  {
    key: 'engine',
    type: 'select',
    label: t('sqlHistory.filters.engine'),
    options: targetEngineOptions.value,
    testId: 'sql-history-engine-filter'
  },
  {
    key: 'submittedBy',
    type: 'input',
    label: t('sqlHistory.filters.submittedBy'),
    placeholder: t('sqlHistory.filters.submittedByPlaceholder')
  },
  {
    key: 'cacheHit',
    type: 'select',
    label: t('sqlHistory.filters.cacheHit'),
    options: booleanFilterOptions.value
  },
  {
    key: 'rewriteApplied',
    type: 'select',
    label: t('sqlHistory.filters.rewriteApplied'),
    options: booleanFilterOptions.value
  },
  {
    key: 'accelerationApplied',
    type: 'select',
    label: t('sqlHistory.filters.accelerationApplied'),
    options: booleanFilterOptions.value
  },
  {
    key: 'sortBy',
    type: 'select',
    label: t('sqlHistory.filters.sortBy'),
    options: sortFieldOptions.value
  },
  {
    key: 'sortOrder',
    type: 'select',
    label: t('sqlHistory.filters.sortOrder'),
    options: sortOrderOptions.value
  },
  {
    key: 'traceId',
    type: 'input',
    label: 'Trace ID',
    placeholder: t('sqlHistory.filters.traceIdPlaceholder'),
    testId: 'sql-history-trace-id'
  },
  {
    key: 'taskId',
    type: 'input',
    label: 'Task ID',
    placeholder: t('sqlHistory.filters.taskIdPlaceholder'),
    testId: 'sql-history-task-id'
  },
  {
    key: 'reportId',
    type: 'input',
    label: 'Report ID',
    placeholder: t('sqlHistory.filters.reportIdPlaceholder'),
    testId: 'sql-history-report-id'
  }
])

const summaryMetrics = computed(() => [
  {
    key: 'pageRows',
    label: t('sqlHistory.metrics.currentPage'),
    value: tableRows.value.length
  },
  {
    key: 'totalRows',
    label: t('sqlHistory.metrics.total'),
    value: pageInfo.total
  },
  {
    key: 'success',
    label: t('sqlHistory.metrics.success'),
    value: statusCounts.value.SUCCESS || statusCounts.value.SUCCEEDED || 0
  },
  {
    key: 'nonSuccess',
    label: t('sqlHistory.metrics.nonSuccess'),
    value: Number(statusCounts.value.PARTIAL || 0) + Number(statusCounts.value.FAILED || 0)
  },
  {
    key: 'accessChannels',
    label: t('sqlHistory.metrics.accessChannels'),
    value: Object.keys(accessChannelCounts.value).length
  }
])

const listStatusLabel = computed(() => t(`sqlHistory.queryStatus.${listStatus.value}`))
const lastQueryText = computed(() => (lastQueryAt.value ? formatTimestamp(lastQueryAt.value) : '-'))
const visibleErrorMessage = computed(() => workflowErrorMessage.value || listErrorMessage.value)
const emptyDescription = computed(() => {
  if (loadingList.value) {
    return t('sqlHistory.states.loading')
  }
  if (listErrorMessage.value) {
    return t('sqlHistory.states.loadFailed')
  }
  return t('sqlHistory.states.empty')
})

const selectFieldProps = field => ({
  placeholder: field.placeholder || t('sqlHistory.filters.selectPlaceholder'),
  clearable: true,
  filterable: Boolean(field.filterable),
  allowCreate: Boolean(field.allowCreate),
  defaultFirstOption: Boolean(field.defaultFirstOption)
})

const inputFieldProps = field => ({
  placeholder: field.placeholder,
  clearable: true
})

const tableColumnProps = column => ({
  prop: column.prop,
  label: column.label,
  minWidth: column.minWidth,
  showOverflowTooltip: true
})

const detailDrawerProps = computed(() => ({
  title: selectedHistoryDetail.value?.historyId || t('sqlHistory.detail.title')
}))

const sqlCodeBlockProps = item => ({
  value: item.value,
  label: item.label,
  copyLabel: t('sqlHistory.actions.copy'),
  autoFormat: item.autoFormat !== false
})

const historyTableColumns = computed(() => [
  {
    key: 'historyId',
    label: t('sqlHistory.table.historyId'),
    minWidth: 210,
    slot: 'historyId'
  },
  {
    key: 'reportKey',
    label: t('sqlHistory.table.reportKey'),
    minWidth: 180,
    slot: 'reportKey'
  },
  {
    key: 'requestTenant',
    label: t('sqlHistory.table.requestTenant'),
    minWidth: 130,
    slot: 'requestTenant'
  },
  {
    key: 'datasourceCode',
    prop: 'datasourceCode',
    label: t('sqlHistory.table.datasource'),
    minWidth: 130
  },
  {
    key: 'resultStatus',
    prop: 'resultStatus',
    label: t('sqlHistory.table.status'),
    minWidth: 120,
    slot: 'status'
  },
  {
    key: 'accessChannel',
    prop: 'accessChannel',
    label: t('sqlHistory.table.accessChannel'),
    minWidth: 130
  },
  {
    key: 'targetEngine',
    prop: 'targetEngine',
    label: t('sqlHistory.table.targetEngine'),
    minWidth: 120
  },
  {
    key: 'governanceHits',
    label: t('sqlHistory.table.governanceHits'),
    minWidth: 190,
    slot: 'governanceHits'
  },
  {
    key: 'submittedBy',
    prop: 'submittedBy',
    label: t('sqlHistory.table.submittedBy'),
    minWidth: 120
  },
  {
    key: 'submittedAt',
    prop: 'submittedAt',
    label: t('sqlHistory.table.submittedAt'),
    minWidth: 170,
    slot: 'submittedAt'
  },
  {
    key: 'auditEventCount',
    label: t('sqlHistory.table.auditEventCount'),
    minWidth: 120,
    slot: 'auditEventCount'
  }
])

const detailCards = computed(() => {
  const detail = selectedHistoryDetail.value
  if (!detail) {
    return []
  }
  return [
    { label: t('sqlHistory.detail.historyId'), value: detail.historyId, testId: 'sql-history-detail-history-id' },
    { label: t('sqlHistory.detail.traceId'), value: detail.traceId, testId: 'sql-history-detail-trace-id' },
    { label: t('sqlHistory.detail.reportKey'), value: detail.reportCode || detail.sqlFingerprint },
    { label: t('sqlHistory.detail.datasource'), value: detail.datasourceCode },
    { label: t('sqlHistory.detail.status'), value: detail.resultStatus, testId: 'sql-history-detail-status' },
    { label: t('sqlHistory.detail.accessChannel'), value: detail.accessChannel },
    { label: t('sqlHistory.detail.targetEngine'), value: detail.targetEngine, testId: 'sql-history-detail-target-engine' },
    { label: t('sqlHistory.detail.submittedBy'), value: detail.submittedBy },
    { label: t('sqlHistory.detail.submittedAt'), value: formatTimestamp(detail.submittedAt) },
    {
      label: t('sqlHistory.detail.auditEventCount'),
      value: detail.traceDetail?.auditEventCount ?? auditEvents.value.length,
      testId: 'sql-history-detail-audit-count'
    }
  ]
})

const executionCards = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  return [
    {
      label: t('sqlHistory.execution.cacheHit'),
      value: booleanDisplay(firstValue(executionSummary.value.cacheHit, detail.cacheHit))
    },
    {
      label: t('sqlHistory.execution.rewriteApplied'),
      value: booleanDisplay(firstValue(executionSummary.value.rewriteApplied, detail.rewriteApplied))
    },
    {
      label: t('sqlHistory.execution.accelerationApplied'),
      value: booleanDisplay(firstValue(executionSummary.value.accelerationApplied, detail.accelerationApplied))
    },
    {
      label: t('sqlHistory.execution.returnedRows'),
      value: firstValue(executionSummary.value.returnedRowCount, detail.returnedRowCount)
    },
    {
      label: t('sqlHistory.execution.errorCode'),
      value: firstValue(executionSummary.value.errorCode, detail.errorCode)
    },
    {
      label: t('sqlHistory.execution.errorMessage'),
      value: firstValue(executionSummary.value.errorMessage, detail.errorMessage)
    }
  ].filter(item => hasDisplayValue(item.value))
})

const sqlCards = computed(() => [
  {
    label: t('sqlHistory.sql.sqlFingerprint'),
    value: firstValue(sqlState.value.sqlFingerprint, selectedHistoryDetail.value?.sqlFingerprint),
    testId: 'sql-history-detail-sql-fingerprint'
  },
  {
    label: t('sqlHistory.sql.templateFingerprint'),
    value: firstValue(sqlState.value.sqlTemplateFingerprint, selectedHistoryDetail.value?.sqlTemplateFingerprint)
  },
  {
    label: t('sqlHistory.sql.boundFingerprint'),
    value: firstValue(sqlState.value.boundSqlFingerprint, selectedHistoryDetail.value?.boundSqlFingerprint)
  },
  {
    label: t('sqlHistory.sql.bindingMode'),
    value: firstValue(sqlState.value.bindingMode, selectedHistoryDetail.value?.bindingMode)
  },
  {
    label: t('sqlHistory.sql.bindingRender'),
    value: firstValue(sqlState.value.bindingRenderStatus, selectedHistoryDetail.value?.bindingRenderStatus)
  }
])

const sqlVariants = computed(() =>
  [
    {
      key: 'sqlText',
      label: t('sqlHistory.sql.originalSql'),
      value: selectedHistoryDetail.value?.sqlText,
      autoFormat: false
    },
    {
      key: 'sqlTemplateText',
      label: t('sqlHistory.sql.templateSql'),
      value: selectedHistoryDetail.value?.sqlTemplateText
    },
    { key: 'boundSqlText', label: t('sqlHistory.sql.boundSql'), value: selectedHistoryDetail.value?.boundSqlText }
  ].filter(item => hasDisplayValue(item.value))
)

const search = async () => {
  workflowErrorMessage.value = ''
  await searchList()
}

const clearFilters = async () => {
  workflowErrorMessage.value = ''
  await clearListFilters()
}

const openHistoryDetail = async historyId => {
  const normalizedHistoryId = normalizeQueryValue(historyId)
  if (!normalizedHistoryId) {
    return
  }
  loading.detail = true
  workflowErrorMessage.value = ''
  activeDetailTab.value = 'overview'
  try {
    selectedHistoryDetail.value = await getGovernanceQueryHistoryDetail(requestTenantId.value, normalizedHistoryId, {
      requestPrefix: 'frontend-sql-history-detail'
    })
    detailDrawerVisible.value = true
  } catch (error) {
    selectedHistoryDetail.value = null
    workflowErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const runIndexedLookup = async () => {
  if (!hasLookupCriteria.value) {
    workflowErrorMessage.value = t('sqlHistory.messages.lookupRequired')
    return
  }
  loading.lookup = true
  workflowErrorMessage.value = ''
  try {
    const lookupPage = await lookupGovernanceTraces(
      requestTenantId.value,
      {
        traceId: searchForm.traceId,
        taskId: searchForm.taskId,
        reportId: searchForm.reportId
      },
      5,
      {
        requestPrefix: 'frontend-sql-history-lookup'
      }
    )
    const firstTraceId = lookupPage?.items?.[0]?.traceId
    if (!firstTraceId) {
      workflowErrorMessage.value = t('sqlHistory.messages.lookupEmpty')
      return
    }
    const traceDetail = await getGovernanceTraceDetail(requestTenantId.value, firstTraceId, 20, {
      requestPrefix: 'frontend-sql-history-trace-detail'
    })
    const executionHistory = (traceDetail?.queryHistories || []).find(
      item => item.historyType === SQL_EXECUTION_HISTORY_TYPE
    )
    if (!executionHistory?.historyId) {
      workflowErrorMessage.value = t('sqlHistory.messages.lookupWithoutExecution')
      return
    }
    await openHistoryDetail(executionHistory.historyId)
    if (selectedHistoryDetail.value && !selectedHistoryDetail.value.traceDetail) {
      selectedHistoryDetail.value.traceDetail = traceDetail
    }
  } catch (error) {
    workflowErrorMessage.value = formatRuntimeError(error)
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
  searchForm.traceId = normalizeQueryValue(route.query.traceId)
  searchForm.taskId = normalizeQueryValue(route.query.taskId)
  searchForm.reportId = normalizeQueryValue(route.query.reportId)
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
  workflowErrorMessage.value = ''
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
    workflowErrorMessage.value = formatRuntimeError(error)
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
  `${t('sqlHistory.governance.cache')}:${booleanShort(row.cacheHit)} / ${t(
    'sqlHistory.governance.rewrite'
  )}:${booleanShort(row.rewriteApplied)} / ${t('sqlHistory.governance.acceleration')}:${booleanShort(
    row.accelerationApplied
  )}`

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
  syncRouteTenant()
  await initializeList()
  await openRouteDeepLink()
})

watch(
  () => route.fullPath,
  async () => {
    syncRouteTenant()
    pageInfo.currentPage = 1
    await initializeList()
    await openRouteDeepLink()
  }
)
</script>

<template>
  <section class="sql-history-page" data-testid="sql-history-page">
    <header class="page-shell">
      <div class="page-copy">
        <p class="section-kicker">{{ PAGE_KICKER }}</p>
        <h1 class="section-title">{{ t('sqlHistory.title') }}</h1>
      </div>
      <div class="action-row">
        <el-button type="primary" :loading="loadingList" data-testid="sql-history-refresh" @click="search">
          {{ t('sqlHistory.actions.refresh') }}
        </el-button>
        <el-button :loading="loading.lookup" data-testid="sql-history-run-lookup" @click="runIndexedLookup">
          {{ t('sqlHistory.actions.lookup') }}
        </el-button>
        <el-button @click="clearFilters">{{ t('sqlHistory.actions.clear') }}</el-button>
      </div>
    </header>

    <div v-if="visibleErrorMessage" class="inline-banner inline-banner-danger" data-testid="sql-history-error">
      <strong>{{ t('sqlHistory.states.errorTitle') }}</strong>
      <span>{{ visibleErrorMessage }}</span>
    </div>

    <section class="filter-panel">
      <el-form class="filter-form" :model="searchForm" label-position="top" @submit.prevent="search">
        <div class="field-grid">
          <el-form-item v-for="field in searchFields" :key="field.key" :label="field.label" class="field-block">
            <el-select
              v-if="field.type === 'select'"
              v-model="searchForm[field.key]"
              :data-testid="field.testId"
              v-bind="selectFieldProps(field)"
            >
              <el-option
                v-for="option in field.options"
                :key="`${field.key}-${option.value}`"
                v-bind="option"
              />
            </el-select>
            <el-input
              v-else
              v-model="searchForm[field.key]"
              :data-testid="field.testId"
              v-bind="inputFieldProps(field)"
              @keyup.enter="search"
            />
          </el-form-item>
        </div>
      </el-form>
      <div v-if="datasourceOptionsLoadFailed" class="filter-hint" data-testid="sql-history-datasource-options-fallback">
        {{ t('sqlHistory.states.datasourceOptionsFallback') }}
      </div>
    </section>

    <section class="summary-strip" :aria-label="t('sqlHistory.metrics.label')">
      <div v-for="item in summaryMetrics" :key="item.key" class="summary-metric">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <section class="table-panel" data-testid="sql-history-query-history-table">
      <div class="table-heading">
        <div>
          <p class="section-kicker">{{ t('sqlHistory.table.kicker') }}</p>
          <h2 class="section-title section-title-small">{{ t('sqlHistory.table.title') }}</h2>
        </div>
      </div>

      <el-table
        v-loading="loadingList"
        :data="tableRows"
        :element-loading-text="t('sqlHistory.states.loading')"
        border
      >
        <el-table-column
          v-for="column in historyTableColumns"
          :key="column.key"
          v-bind="tableColumnProps(column)"
        >
          <template #default="{ row }">
            <template v-if="column.slot === 'historyId'">
              <button
                type="button"
                class="table-link"
                data-testid="sql-history-trace-item"
                @click="openHistoryDetail(row.historyId)"
              >
                {{ displayValue(row.historyId) }}
              </button>
              <div class="cell-subline">{{ displayValue(row.traceId) }}</div>
            </template>
            <template v-else-if="column.slot === 'reportKey'">
              {{ displayValue(row.reportCode || row.sqlFingerprint) }}
            </template>
            <template v-else-if="column.slot === 'requestTenant'">
              {{ displayValue(row.tenantId || requestTenantId) }}
            </template>
            <template v-else-if="column.slot === 'status'">
              <span :class="statusClass(row.resultStatus)">{{ displayValue(row.resultStatus) }}</span>
            </template>
            <template v-else-if="column.slot === 'governanceHits'">
              {{ governanceHitText(row) }}
            </template>
            <template v-else-if="column.slot === 'submittedAt'">
              {{ formatTimestamp(row.submittedAt) }}
            </template>
            <template v-else-if="column.slot === 'auditEventCount'">
              {{ row.auditEventCount ?? '-' }}
            </template>
            <template v-else>
              {{ displayValue(row[column.prop]) }}
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="emptyDescription" />
        </template>
      </el-table>

      <div class="table-footer">
        <div class="footer-status">
          <span>{{ t('sqlHistory.footer.currentPageCount', { count: tableRows.length }) }}</span>
          <span>{{ t('sqlHistory.footer.totalCount', { count: pageInfo.total }) }}</span>
          <span>{{ t('sqlHistory.footer.pageWindow', pageWindow) }}</span>
          <span>{{ t('sqlHistory.footer.lastQuery', { status: listStatusLabel, time: lastQueryText }) }}</span>
        </div>
        <el-pagination
          v-model:page-size="pageInfo.pageSize"
          v-model:current-page="pageInfo.currentPage"
          class="pagination-row"
          data-testid="sql-history-pagination"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="pageInfo.total"
          :page-sizes="LIST_PAGE_SIZE_OPTIONS"
          :disabled="loadingList"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </div>
    </section>

    <el-drawer
      v-model="detailDrawerVisible"
      size="70%"
      data-testid="sql-history-detail-drawer"
      v-bind="detailDrawerProps"
    >
      <div v-if="selectedHistoryDetail" class="drawer-stack">
        <div class="dialog-header">
          <div class="banner-row">
            <strong data-testid="sql-history-detail-service-code">{{ selectedHistoryDetail.historyType || '-' }}</strong>
            <span :class="statusClass(selectedHistoryDetail.resultStatus)">
              {{ selectedHistoryDetail.resultStatus || '-' }}
            </span>
          </div>
          <div class="action-row">
            <el-button type="primary" @click="openRepairEvidence">
              {{ t('sqlHistory.actions.openRepairEvidence') }}
            </el-button>
            <el-button @click="openAuditForensics">{{ t('sqlHistory.actions.openAuditForensics') }}</el-button>
            <el-button :loading="loading.export" data-testid="sql-history-export" @click="openExportDialog">
              {{ t('sqlHistory.actions.exportEvidence') }}
            </el-button>
            <el-button @click="evidenceDrawerVisible = true">
              {{ t('sqlHistory.actions.viewRawEvidence') }}
            </el-button>
          </div>
        </div>

        <el-tabs v-model="activeDetailTab" data-testid="sql-history-detail-tabs">
          <el-tab-pane :label="t('sqlHistory.tabs.overview')" name="overview">
            <div class="detail-grid">
              <div v-for="item in detailCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="item.testId || undefined">{{ displayValue(item.value) }}</strong>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('sqlHistory.tabs.execution')" name="execution">
            <div class="detail-grid">
              <div v-for="item in executionCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div class="code-grid">
              <article v-if="isNonEmpty(selectedHistoryDetail.routeDecision)" class="code-card">
                <div class="code-card__header">{{ t('sqlHistory.execution.routeDecision') }}</div>
                <pre class="code-block">{{ formatJson(selectedHistoryDetail.routeDecision) }}</pre>
              </article>
              <article v-if="isNonEmpty(selectedHistoryDetail.cacheSummary)" class="code-card">
                <div class="code-card__header">{{ t('sqlHistory.execution.cacheSummary') }}</div>
                <pre class="code-block">{{ formatJson(selectedHistoryDetail.cacheSummary) }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('sqlHistory.tabs.sql')" name="sql">
            <div class="detail-grid">
              <div v-for="item in sqlCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="item.testId || undefined">{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div class="code-grid">
              <article v-for="item in sqlVariants" :key="item.key" class="code-card">
                <div class="code-card__header">{{ item.label }}</div>
                <SqlCodeBlock v-bind="sqlCodeBlockProps(item)" />
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('sqlHistory.tabs.audit')" name="audit">
            <el-table :data="auditEvents" border>
              <el-table-column prop="serviceCode" :label="t('sqlHistory.audit.service')" min-width="150" />
              <el-table-column prop="operationType" :label="t('sqlHistory.audit.operation')" min-width="160" />
              <el-table-column prop="status" :label="t('sqlHistory.audit.status')" min-width="120" />
              <el-table-column :label="t('sqlHistory.audit.createdAt')" min-width="170">
                <template #default="{ row }">{{ formatTimestamp(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-drawer v-model="evidenceDrawerVisible" :title="t('sqlHistory.rawEvidence.title')" size="56%">
      <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="exportDialogVisible" :title="t('sqlHistory.export.title')" width="720px">
      <div class="dialog-stack">
        <label class="field-block field-block-plain">
          <span class="field-label">{{ t('sqlHistory.export.format') }}</span>
          <el-select v-model="exportForm.exportFormat">
            <el-option
              v-for="option in exportFormatOptions"
              :key="option.value"
              v-bind="option"
            />
          </el-select>
        </label>
        <el-checkbox v-model="exportForm.includeTraceDetail">
          {{ t('sqlHistory.export.includeTraceDetail') }}
        </el-checkbox>
        <el-button type="primary" :loading="loading.export" @click="runExport">
          {{ t('sqlHistory.export.run') }}
        </el-button>
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
  gap: var(--sqlforge-space-4);
}

.sql-history-page {
  padding: var(--sqlforge-space-6);
  color: var(--sqlforge-text-primary);
}

.page-shell,
.filter-panel,
.table-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-2);
  padding: var(--sqlforge-space-5);
}

.page-shell,
.table-heading,
.dialog-header,
.banner-row,
.action-row,
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sqlforge-space-3);
  flex-wrap: wrap;
}

.page-copy {
  max-width: 760px;
}

.section-kicker,
.field-label,
.summary-metric span,
.operation-item small,
.footer-status {
  color: var(--sqlforge-text-muted);
  font-size: var(--sqlforge-text-meta);
  line-height: 1.4;
  letter-spacing: 0;
}

.section-kicker {
  margin: 0 0 var(--sqlforge-space-2);
  font-family: var(--sqlforge-font-mono);
  text-transform: uppercase;
}

.section-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
  font-size: 24px;
  font-weight: 500;
  line-height: 1.2;
}

.section-title-small {
  font-size: var(--sqlforge-text-heading);
}

.section-summary,
.cell-subline {
  margin: var(--sqlforge-space-2) 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.field-grid,
.summary-strip,
.detail-grid,
.code-grid {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.field-grid {
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
}

.filter-hint {
  margin-top: var(--sqlforge-space-3);
  color: #f4c84a;
  font-size: var(--sqlforge-text-meta);
}

.summary-strip {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
  padding: var(--sqlforge-space-4);
}

.summary-metric {
  min-width: 0;
  border-left: 1px solid var(--sqlforge-border-default);
  padding-left: var(--sqlforge-space-3);
}

.summary-metric:first-child {
  border-left: 0;
  padding-left: 0;
}

.summary-metric strong {
  display: block;
  margin-top: var(--sqlforge-space-1);
  color: var(--sqlforge-text-primary);
  font-size: var(--sqlforge-text-heading);
}

.field-block,
.field-block-plain {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
}

.field-block :deep(.el-form-item__label) {
  color: var(--sqlforge-text-secondary);
}

.detail-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.code-grid {
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
}

.detail-grid__item,
.code-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
  padding: var(--sqlforge-space-4);
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-meta);
}

.detail-grid__item strong {
  display: block;
  margin-top: var(--sqlforge-space-1);
  color: var(--sqlforge-text-primary);
  overflow-wrap: anywhere;
}

.pill {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-pill);
  background: var(--sqlforge-status-info);
  color: var(--sqlforge-text-secondary);
  padding: 3px var(--sqlforge-space-2);
  font-size: var(--sqlforge-text-meta);
  line-height: 1.4;
}

.pill-success {
  border-color: rgba(62, 207, 142, 0.34);
  background: var(--sqlforge-status-success);
  color: var(--sqlforge-color-brand);
}

.pill-warning {
  border-color: rgba(255, 205, 64, 0.34);
  background: var(--sqlforge-status-warning);
  color: #f4c84a;
}

.pill-danger {
  border-color: rgba(235, 85, 60, 0.34);
  background: var(--sqlforge-status-danger);
  color: #ff8a73;
}

.inline-banner {
  display: flex;
  align-items: flex-start;
  gap: var(--sqlforge-space-2);
  border-radius: var(--sqlforge-radius-sm);
  padding: var(--sqlforge-space-3) var(--sqlforge-space-4);
}

.inline-banner-danger {
  border: 1px solid rgba(235, 85, 60, 0.34);
  background: var(--sqlforge-status-danger);
  color: #ffb09f;
}

.table-link {
  border: 0;
  background: transparent;
  color: var(--sqlforge-color-link);
  cursor: pointer;
  font: inherit;
  padding: 0;
  text-align: left;
}

.table-link:hover {
  text-decoration: underline;
}

.table-footer {
  margin-top: var(--sqlforge-space-4);
}

.footer-status {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
}

.pagination-row {
  justify-content: flex-end;
}

.code-card__header {
  color: var(--sqlforge-text-secondary);
  font-weight: 700;
  margin-bottom: var(--sqlforge-space-2);
}

.code-block {
  background: var(--sqlforge-bg-page-deep);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  color: var(--sqlforge-text-primary);
  margin: 0;
  overflow: auto;
  padding: var(--sqlforge-space-3);
  white-space: pre-wrap;
}

@media (max-width: 720px) {
  .sql-history-page {
    padding: var(--sqlforge-space-4);
  }

  .page-shell,
  .table-heading,
  .dialog-header,
  .action-row,
  .table-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-metric {
    border-left: 0;
    border-top: 1px solid var(--sqlforge-border-default);
    padding-left: 0;
    padding-top: var(--sqlforge-space-3);
  }

  .summary-metric:first-child {
    border-top: 0;
    padding-top: 0;
  }
}
</style>
