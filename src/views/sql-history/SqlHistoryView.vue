<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  exportGovernanceQueryHistory,
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getQueryHistoryRewriteRecords
} from '../../services/runtimeGateApi'
import { engineOptions } from '../common/formComponentGovernance'
import MetricCard from '../common/MetricCard.vue'
import SectionHeader from '../common/SectionHeader.vue'
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

const rewriteValidationStatusValueOptions = [
  { label: 'NOT_VALIDATED', value: 'NOT_VALIDATED' },
  { label: 'VALIDATING', value: 'VALIDATING' },
  { label: 'EQUIVALENT', value: 'EQUIVALENT' },
  { label: 'DIVERGED', value: 'DIVERGED' },
  { label: 'FAILED', value: 'FAILED' },
  { label: 'EXPIRED', value: 'EXPIRED' }
]

const rewriteSourceTypeValueOptions = [
  { label: 'PARSE', value: 'PARSE' },
  { label: 'QUERY', value: 'QUERY' }
]

const logicalObjectTypeValueOptions = [
  { label: 'BUSINESS_VIEW', value: 'BUSINESS_VIEW' },
  { label: 'DB_VIEW', value: 'DB_VIEW' },
  { label: 'TABLE', value: 'TABLE' }
]

const sortFieldValueOptions = [
  { label: 'submittedAt', value: 'submittedAt' },
  { label: 'finishedAt', value: 'finishedAt' },
  { label: 'status', value: 'status' },
  { label: 'rowCount', value: 'rowCount' },
  { label: 'queryDateStart', value: 'queryDateStart' }
]

const sortOrderValueOptions = [
  { label: 'DESC', value: 'DESC' },
  { label: 'ASC', value: 'ASC' }
]

const exportFormatOptions = [
  { label: 'JSON', value: 'JSON' },
  { label: 'MARKDOWN', value: 'MARKDOWN' }
]

const DETAIL_TAB_NAMES = ['overview', 'execution', 'sql', 'rewriteRecords', 'signals', 'refs', 'audit']
const normalizeRouteQueryValue = value => (Array.isArray(value) ? String(value[0] || '').trim() : String(value || '').trim())
const normalizeDetailTab = value => {
  const tabName = normalizeRouteQueryValue(value)
  return DETAIL_TAB_NAMES.includes(tabName) ? tabName : ''
}

const loading = reactive({
  detail: false,
  export: false,
  rewriteRecords: false
})

const selectedHistoryDetail = ref(null)
const rewriteRecordsResponse = ref(null)
const rewriteRecordsLoadedHistoryId = ref('')
const rewriteRecordErrorMessage = ref('')
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

const isRewriteHistoryEntry = computed(
  () => normalizeRouteQueryValue(route.query.hasRewriteRecord) === 'true' || normalizeDetailTab(route.query.detailTab) === 'rewriteRecords'
)
const pageKicker = computed(() => (isRewriteHistoryEntry.value ? t('navigation.modules.rewriteGovernance') : PAGE_KICKER))
const pageTitle = computed(() => (isRewriteHistoryEntry.value ? t('navigation.items.rewriteHistory') : t('sqlHistory.title')))
const pageSummary = computed(() => t('sqlHistory.summary'))
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
const rewriteAudit = computed(() => objectValue(selectedHistoryDetail.value?.rewriteAudit))
const sqlState = computed(() => objectValue(selectedHistoryDetail.value?.sqlState))
const recommendationRefRows = computed(() =>
  normalizeArray(selectedHistoryDetail.value?.recommendationRefs)
    .map(item => (item && typeof item === 'object' ? item : { recommendationId: item }))
    .filter(item => hasDisplayValue(referenceValue(item, ['recommendationId', 'id'])))
)
const firstRecommendationRef = computed(() => recommendationRefRows.value[0] || null)
const linkedParseHistoryId = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  const queryContext = objectValue(detail.queryContext)
  const structureParse = objectValue(detail.structureParseSummary)
  return firstValue(
    detail.parseHistoryId,
    detail.sqlParseHistoryId,
    structureParse.parseHistoryId,
    queryContext.parseHistoryId
  )
})
const parseHistoryLinkQuery = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  return compactObject({
    tenantId: requestTenantId.value,
    historyWorkbenchTab: 'sqlHistory',
    historyId: linkedParseHistoryId.value,
    traceId: detail.traceId,
    taskId: firstValue(detail.parseTaskId, detail.traceDetail?.taskId),
    reportId: firstValue(detail.reportId, detail.reportCode)
  })
})
const hasParseHistoryLink = computed(() =>
  ['historyId', 'traceId', 'taskId', 'reportId'].some(key => hasDisplayValue(parseHistoryLinkQuery.value[key]))
)
const parseHistoryLinkText = computed(() =>
  linkedParseHistoryId.value || selectedHistoryDetail.value?.traceId || selectedHistoryDetail.value?.reportCode || '-'
)

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
const rewriteValidationStatusOptions = computed(() => withAllOption(rewriteValidationStatusValueOptions))
const rewriteSourceTypeOptions = computed(() => withAllOption(rewriteSourceTypeValueOptions))
const logicalObjectTypeOptions = computed(() => withAllOption(logicalObjectTypeValueOptions))
const sortFieldOptions = computed(() => withDefaultOption(sortFieldValueOptions))
const sortOrderOptions = computed(() => withDefaultOption(sortOrderValueOptions))
const rewriteRecordFiltersDisabled = computed(() => searchForm.hasRewriteRecord === 'false')

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
    key: 'stage',
    type: 'input',
    label: t('sqlHistory.filters.stage'),
    placeholder: t('sqlHistory.filters.stagePlaceholder')
  },
  {
    key: 'bizDate',
    type: 'date',
    label: t('sqlHistory.filters.bizDate'),
    placeholder: t('sqlHistory.filters.datePlaceholder')
  },
  {
    key: 'queryDateStart',
    type: 'date',
    label: t('sqlHistory.filters.queryDateStart'),
    placeholder: t('sqlHistory.filters.datePlaceholder')
  },
  {
    key: 'queryDateEnd',
    type: 'date',
    label: t('sqlHistory.filters.queryDateEnd'),
    placeholder: t('sqlHistory.filters.datePlaceholder')
  },
  {
    key: 'status',
    type: 'select',
    label: t('sqlHistory.filters.status'),
    options: statusFilterOptions.value,
    testId: 'sql-history-status-filter'
  },
  {
    key: 'logicalObjectType',
    type: 'select',
    label: t('sqlHistory.filters.logicalObjectType'),
    options: logicalObjectTypeOptions.value
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
    key: 'submittedStart',
    type: 'date',
    label: t('sqlHistory.filters.submittedStart'),
    placeholder: t('sqlHistory.filters.datePlaceholder')
  },
  {
    key: 'submittedEnd',
    type: 'date',
    label: t('sqlHistory.filters.submittedEnd'),
    placeholder: t('sqlHistory.filters.datePlaceholder')
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
    key: 'hasRewriteRecord',
    type: 'select',
    label: t('sqlHistory.filters.hasRewriteRecord'),
    options: booleanFilterOptions.value,
    testId: 'sql-history-has-rewrite-record-filter'
  },
  {
    key: 'rewriteValidationStatus',
    type: 'select',
    label: t('sqlHistory.filters.rewriteValidationStatus'),
    options: rewriteValidationStatusOptions.value,
    disabled: rewriteRecordFiltersDisabled.value,
    testId: 'sql-history-rewrite-validation-status-filter'
  },
  {
    key: 'rewriteSourceType',
    type: 'select',
    label: t('sqlHistory.filters.rewriteSourceType'),
    options: rewriteSourceTypeOptions.value,
    disabled: rewriteRecordFiltersDisabled.value,
    testId: 'sql-history-rewrite-source-type-filter'
  },
  {
    key: 'recommendationId',
    type: 'input',
    label: t('sqlHistory.filters.recommendationId'),
    placeholder: t('sqlHistory.filters.recommendationIdPlaceholder'),
    disabled: rewriteRecordFiltersDisabled.value,
    testId: 'sql-history-recommendation-id-filter'
  },
  {
    key: 'accelerationApplied',
    type: 'select',
    label: t('sqlHistory.filters.accelerationApplied'),
    options: booleanFilterOptions.value
  },
  {
    key: 'parameterizedSql',
    type: 'select',
    label: t('sqlHistory.filters.parameterizedSql'),
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
  },
  {
    key: 'queryDateResolved',
    label: t('sqlHistory.metrics.queryDateResolved'),
    value: tableRows.value.filter(item => String(item.queryDateStatus || '').toUpperCase() === 'RESOLVED').length
  },
  {
    key: 'parameterizedSql',
    label: t('sqlHistory.metrics.parameterizedSql'),
    value: tableRows.value.filter(item => item.parameterizedSqlFlag === true).length
  }
])

const listStatusLabel = computed(() => t(`sqlHistory.queryStatus.${listStatus.value}`))
const lastQueryText = computed(() => (lastQueryAt.value ? formatTimestamp(lastQueryAt.value) : '-'))
const paginationStateText = computed(() =>
  t('sqlHistory.footer.lastQuery', { status: listStatusLabel.value, time: lastQueryText.value })
)
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
  defaultFirstOption: Boolean(field.defaultFirstOption),
  disabled: Boolean(field.disabled)
})

const inputFieldProps = field => ({
  placeholder: field.placeholder,
  clearable: true,
  disabled: Boolean(field.disabled)
})

const dateFieldProps = field => ({
  placeholder: field.placeholder,
  clearable: true,
  valueFormat: 'YYYY-MM-DD'
})

const filterFieldClass = field => `field-block-${field.type || 'input'}`

const filterFieldStyle = field => ({
  '--filter-field-min': `clamp(184px, ${filterFieldWidthCh(field)}ch, 360px)`
})

const filterFieldWidthCh = field => {
  const optionTextLength = Math.max(
    0,
    ...(field.options || []).map(option => visualTextLength(option.label))
  )
  const textLength = Math.max(
    visualTextLength(field.label),
    visualTextLength(field.placeholder),
    optionTextLength
  )
  const typeBase = field.type === 'date' ? 18 : field.type === 'select' ? 20 : 19
  return Math.min(34, Math.max(typeBase, textLength + 6))
}

const visualTextLength = value =>
  Array.from(String(value || '')).reduce((length, character) => {
    return length + (/[\u3400-\u9fff]/.test(character) ? 2 : 1)
  }, 0)

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

const metricCardProps = item => ({
  label: item.label,
  value: item.value
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
    key: 'stageCode',
    prop: 'stageCode',
    label: t('sqlHistory.table.stage'),
    minWidth: 110
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
    key: 'queryDate',
    label: t('sqlHistory.table.queryDate'),
    minWidth: 180,
    slot: 'queryDate'
  },
  {
    key: 'parameterizedSqlFlag',
    label: t('sqlHistory.table.sqlState'),
    minWidth: 140,
    slot: 'sqlState'
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

const rewriteAuditCards = computed(() => {
  const audit = rewriteAudit.value
  return [
    {
      key: 'rewriteApplied',
      label: t('sqlHistory.rewriteAudit.rewriteApplied'),
      value: booleanDisplay(audit.rewriteApplied),
      testId: 'sql-history-rewrite-audit-applied'
    },
    {
      key: 'rewriteRecordId',
      label: t('sqlHistory.rewriteAudit.rewriteRecordId'),
      value: audit.rewriteRecordId,
      testId: 'sql-history-rewrite-audit-record-id'
    },
    {
      key: 'runtimeBindingId',
      label: t('sqlHistory.rewriteAudit.runtimeBindingId'),
      value: audit.runtimeBindingId,
      testId: 'sql-history-rewrite-audit-runtime-binding-id'
    },
    {
      key: 'ruleVersion',
      label: t('sqlHistory.rewriteAudit.ruleVersion'),
      value: firstValue(audit.ruleVersion, audit.rewriteRuleVersion),
      testId: 'sql-history-rewrite-audit-rule-version'
    },
    {
      key: 'runtimeRuleVersion',
      label: t('sqlHistory.rewriteAudit.runtimeRuleVersion'),
      value: audit.runtimeRuleVersion,
      testId: 'sql-history-rewrite-audit-runtime-rule-version'
    },
    {
      key: 'runtimeRewriteStatus',
      label: t('sqlHistory.rewriteAudit.runtimeRewriteStatus'),
      value: audit.runtimeRewriteStatus,
      testId: 'sql-history-rewrite-audit-runtime-status'
    },
    {
      key: 'activationStatusSnapshot',
      label: t('sqlHistory.rewriteAudit.activationStatusSnapshot'),
      value: firstValue(audit.activationStatusSnapshot, audit.rewriteActivationStatusSnapshot),
      testId: 'sql-history-rewrite-audit-activation-status'
    },
    {
      key: 'rewriteFallbackReason',
      label: t('sqlHistory.rewriteAudit.rewriteFallbackReason'),
      value: audit.rewriteFallbackReason,
      testId: 'sql-history-rewrite-audit-fallback-reason'
    }
  ].filter(item => hasDisplayValue(item.value))
})

const contextCards = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  const queryDateSummary = objectValue(detail.queryDateSummary)
  return [
    { label: t('sqlHistory.detail.resultId'), value: detail.resultId },
    { label: t('sqlHistory.detail.stage'), value: detail.stageCode },
    { label: t('sqlHistory.detail.bizDate'), value: detail.bizDate },
    { label: t('sqlHistory.detail.datasourceType'), value: detail.datasourceType },
    { label: t('sqlHistory.detail.queryDateStart'), value: queryDateSummary.queryDateStart },
    { label: t('sqlHistory.detail.queryDateEnd'), value: queryDateSummary.queryDateEnd },
    { label: t('sqlHistory.detail.queryDateStatus'), value: queryDateSummary.queryDateStatus }
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
  },
  {
    label: t('sqlHistory.sql.parameterizedSql'),
    value: booleanDisplay(sqlState.value.parameterizedSqlFlag)
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
      value: selectedHistoryDetail.value?.sqlTemplateText,
      autoFormat: false
    },
    {
      key: 'boundSqlText',
      label: t('sqlHistory.sql.boundSql'),
      value: selectedHistoryDetail.value?.boundSqlText,
      autoFormat: false
    }
  ].filter(item => hasDisplayValue(item.value))
)

const parseSignalCards = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  return [
    {
      key: 'commentContext',
      title: t('sqlHistory.signals.commentContext'),
      payload: detail.commentContext
    },
    {
      key: 'queryDateSummary',
      title: t('sqlHistory.signals.queryDateSummary'),
      payload: detail.queryDateSummary
    },
    {
      key: 'logicalObjectHits',
      title: t('sqlHistory.signals.logicalObjectHits'),
      payload: detail.logicalObjectHits
    },
    {
      key: 'structureParseSummary',
      title: t('sqlHistory.signals.structureParseSummary'),
      payload: detail.structureParseSummary
    },
    {
      key: 'accessParseSummary',
      title: t('sqlHistory.signals.accessParseSummary'),
      payload: detail.accessParseSummary
    },
    {
      key: 'bindingSummary',
      title: t('sqlHistory.signals.bindingSummary'),
      payload: detail.bindingSummary
    },
    {
      key: 'routeDecision',
      title: t('sqlHistory.signals.routeDecision'),
      payload: detail.routeDecision
    },
    {
      key: 'cacheSummary',
      title: t('sqlHistory.signals.cacheSummary'),
      payload: detail.cacheSummary
    }
  ].filter(item => isNonEmpty(item.payload))
})

const referenceGroups = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  return [
    { key: 'recommendationRefs', title: t('sqlHistory.refs.recommendationRefs'), items: detail.recommendationRefs || [] },
    { key: 'benchmarkRefs', title: t('sqlHistory.refs.benchmarkRefs'), items: detail.benchmarkRefs || [] },
    { key: 'auditRefs', title: t('sqlHistory.refs.auditRefs'), items: detail.auditRefs || [] },
    { key: 'alertRefs', title: t('sqlHistory.refs.alertRefs'), items: detail.alertRefs || [] }
  ]
})

const rewriteRecordRows = computed(() => rewriteRecordsResponse.value?.items || [])

const hasRewriteAssociationEvidence = computed(() =>
  rewriteRecordRows.value.length > 0 ||
  recommendationRefRows.value.length > 0 ||
  hasDisplayValue(rewriteAudit.value.rewriteRecordId) ||
  hasDisplayValue(rewriteAudit.value.runtimeBindingId)
)

const rewriteLinkedButNotApplied = computed(() =>
  hasRewriteAssociationEvidence.value && rewriteAudit.value.rewriteApplied !== true
)

const rewriteRecordSummaryCards = computed(() => [
  {
    key: 'count',
    label: t('sqlHistory.rewriteRecords.count'),
    value: rewriteRecordsResponse.value?.rewriteRecordCount ?? rewriteRecordRows.value.length
  },
  {
    key: 'contractStage',
    label: t('sqlHistory.rewriteRecords.contractStage'),
    value: rewriteRecordsResponse.value?.contractStage
  },
  {
    key: 'implementationStage',
    label: t('sqlHistory.rewriteRecords.implementationStage'),
    value: rewriteRecordsResponse.value?.implementationStage
  }
])

const search = async () => {
  workflowErrorMessage.value = ''
  await searchList()
}

const clearFilters = async () => {
  workflowErrorMessage.value = ''
  await clearListFilters()
}

const syncRouteSearchFilters = () => {
  syncRouteTenant()
  searchForm.hasRewriteRecord = normalizeQueryValue(route.query.hasRewriteRecord)
  searchForm.rewriteValidationStatus = normalizeQueryValue(route.query.rewriteValidationStatus)
  searchForm.rewriteSourceType = normalizeQueryValue(route.query.rewriteSourceType)
  searchForm.recommendationId = normalizeQueryValue(route.query.recommendationId)
  if (searchForm.hasRewriteRecord === 'false') {
    searchForm.rewriteValidationStatus = ''
    searchForm.rewriteSourceType = ''
    searchForm.recommendationId = ''
  }
}

const resetRewriteRecords = () => {
  rewriteRecordsResponse.value = null
  rewriteRecordsLoadedHistoryId.value = ''
  rewriteRecordErrorMessage.value = ''
}

const loadRewriteRecords = async ({ force = false } = {}) => {
  const historyId = normalizeQueryValue(selectedHistoryDetail.value?.historyId)
  if (!historyId) {
    return
  }
  if (!force && rewriteRecordsLoadedHistoryId.value === historyId) {
    return
  }
  loading.rewriteRecords = true
  rewriteRecordErrorMessage.value = ''
  try {
    rewriteRecordsResponse.value = await getQueryHistoryRewriteRecords(requestTenantId.value, historyId, {
      requestPrefix: 'frontend-sql-history-rewrite-records'
    })
    rewriteRecordsLoadedHistoryId.value = historyId
  } catch (error) {
    rewriteRecordsResponse.value = null
    rewriteRecordsLoadedHistoryId.value = ''
    rewriteRecordErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.rewriteRecords = false
  }
}

const focusRewriteRecords = async () => {
  activeDetailTab.value = 'rewriteRecords'
  await loadRewriteRecords()
}

const handleDetailTabChange = async tabName => {
  if (tabName === 'rewriteRecords') {
    await loadRewriteRecords()
  }
}

const openParseRecordForContext = (source = {}) => {
  const detail = selectedHistoryDetail.value || {}
  const parseHistoryId = firstValue(source.parseHistoryId, linkedParseHistoryId.value)
  const query = compactObject({
    tenantId: requestTenantId.value,
    historyWorkbenchTab: 'sqlHistory',
    historyId: parseHistoryId,
    traceId: firstValue(source.traceId, detail.traceId),
    taskId: firstValue(source.parseTaskId, source.taskId, detail.parseTaskId, detail.traceDetail?.taskId),
    reportId: firstValue(source.reportId, detail.reportId, detail.reportCode)
  })
  if (!['historyId', 'traceId', 'taskId', 'reportId'].some(key => hasDisplayValue(query[key]))) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query
  })
}

const openHistoryDetail = async historyId => {
  const normalizedHistoryId = normalizeQueryValue(historyId)
  if (!normalizedHistoryId) {
    return
  }
  loading.detail = true
  workflowErrorMessage.value = ''
  activeDetailTab.value = 'overview'
  const routeDetailTab = normalizeDetailTab(route.query.detailTab)
  if (routeDetailTab) {
    activeDetailTab.value = routeDetailTab
  }
  resetRewriteRecords()
  try {
    selectedHistoryDetail.value = await getGovernanceQueryHistoryDetail(requestTenantId.value, normalizedHistoryId, {
      requestPrefix: 'frontend-sql-history-detail'
    })
    detailDrawerVisible.value = true
    if (activeDetailTab.value === 'rewriteRecords') {
      await loadRewriteRecords({ force: true })
    }
  } catch (error) {
    selectedHistoryDetail.value = null
    workflowErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const openRecommendationCenter = recommendationOrRecord => {
  const record = recommendationOrRecord && typeof recommendationOrRecord === 'object' ? recommendationOrRecord : {}
  const normalizedRecommendationId = normalizeQueryValue(
    referenceValue(record, ['recommendationId', 'id']) ||
      (typeof recommendationOrRecord === 'string' ? recommendationOrRecord : '')
  )
  const normalizedRewriteRecordId = normalizeQueryValue(referenceValue(record, ['rewriteRecordId']))
  if (!normalizedRecommendationId && !normalizedRewriteRecordId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.recommendationCenter,
    query: {
      tenantId: requestTenantId.value,
      recommendationId: normalizedRecommendationId,
      rewriteRecordId: normalizedRewriteRecordId,
      tab: 'rewriteLifecycle'
    }
  })
}

const openRouteDeepLink = async () => {
  const historyId = normalizeQueryValue(route.query.historyId)
  if (historyId) {
    await openHistoryDetail(historyId)
    return
  }
  if (normalizeQueryValue(route.query.traceId) || normalizeQueryValue(route.query.taskId) || normalizeQueryValue(route.query.reportId)) {
    workflowErrorMessage.value = t('sqlHistory.messages.traceLookupRemoved')
  }
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

const normalizeArray = value => (Array.isArray(value) ? value : [])

const compactObject = value =>
  Object.fromEntries(Object.entries(value).filter(([, entryValue]) => hasDisplayValue(entryValue)))

const referenceValue = (record, keys) => {
  if (!record || typeof record !== 'object') {
    return ''
  }
  return firstValue(...keys.map(key => record[key]))
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

const objectValue = value => {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value
  }
  return {}
}

const rewriteRecordAutoApplyPaused = record =>
  firstValue(
    record?.autoApplyPaused,
    record?.traceRefs?.autoApplyPaused,
    record?.traceRefs?.divergenceAlert?.autoApplyPaused,
    record?.traceRefs?.autoApplyPauseEvidence?.autoApplyPaused
  )

const rewriteRecordAlertRefs = record => {
  const refs = firstValue(
    record?.alertRefs,
    record?.traceRefs?.alertRefs,
    record?.traceRefs?.divergenceAlert?.linkages,
    record?.traceRefs?.divergenceAlert
  )
  if (Array.isArray(refs)) {
    return refs
  }
  return refs && typeof refs === 'object' ? [refs] : []
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
  syncRouteSearchFilters()
  await initializeList()
  await openRouteDeepLink()
})

watch(
  () => searchForm.hasRewriteRecord,
  value => {
    if (value === 'false') {
      searchForm.rewriteValidationStatus = ''
      searchForm.rewriteSourceType = ''
      searchForm.recommendationId = ''
    }
  }
)

watch(
  () => route.fullPath,
  async () => {
    syncRouteSearchFilters()
    pageInfo.currentPage = 1
    await initializeList()
    await openRouteDeepLink()
  }
)

</script>

<template>
  <section class="sql-history-page" data-testid="sql-history-page">
    <section class="history-workbench" :class="{ 'rewrite-context-header': isRewriteHistoryEntry }">
      <header class="sql-history-header">
        <SectionHeader
          :eyebrow="pageKicker"
          :title="pageTitle"
          :summary="pageSummary"
          size="compact"
        >
          <template #actions>
            <div class="action-row">
              <el-button type="primary" :loading="loadingList" data-testid="sql-history-refresh" @click="search">
                {{ t('sqlHistory.actions.refresh') }}
              </el-button>
              <el-button @click="clearFilters">{{ t('sqlHistory.actions.clear') }}</el-button>
            </div>
          </template>
        </SectionHeader>
        <div v-if="!isRewriteHistoryEntry" class="summary-strip" :aria-label="t('sqlHistory.metrics.label')">
          <MetricCard
            v-for="item in summaryMetrics"
            :key="item.key"
            v-bind="metricCardProps(item)"
          />
        </div>
      </header>

      <div v-if="visibleErrorMessage" class="inline-banner inline-banner-danger" data-testid="sql-history-error">
        <strong>{{ t('sqlHistory.states.errorTitle') }}</strong>
        <span>{{ visibleErrorMessage }}</span>
      </div>

      <section class="filter-panel">
        <SectionHeader
          :eyebrow="t('sqlHistory.filters.eyebrow')"
          :title="t('sqlHistory.filters.title')"
          :summary="t('sqlHistory.filters.summary')"
          size="compact"
        />
        <el-form class="filter-form" :model="searchForm" label-position="top" @submit.prevent="search">
          <div class="field-grid">
            <el-form-item
              v-for="field in searchFields"
              :key="field.key"
              class="field-block"
              :class="filterFieldClass(field)"
              :style="filterFieldStyle(field)"
            >
              <template #label>{{ field.label }}</template>
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
              <el-date-picker
                v-else-if="field.type === 'date'"
                v-model="searchForm[field.key]"
                :data-testid="field.testId"
                v-bind="dateFieldProps(field)"
              />
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

      <section class="table-panel" data-testid="sql-history-query-history-table">
        <SectionHeader
          :eyebrow="t('sqlHistory.table.kicker')"
          :title="t('sqlHistory.table.title')"
          size="compact"
        />
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
              <template v-else-if="column.slot === 'queryDate'">
                {{ displayValue(row.queryDateStart) }} / {{ displayValue(row.queryDateEnd) }}
                <div class="cell-subline">{{ displayValue(row.queryDateStatus) }}</div>
              </template>
              <template v-else-if="column.slot === 'sqlState'">
                {{ booleanShort(row.parameterizedSqlFlag) }}
                <div class="cell-subline">{{ displayValue(row.bindingMode) }}</div>
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
            {{ paginationStateText }}
          </div>
          <div class="pagination-cluster">
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
        </div>
      </section>
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
            <el-button :loading="loading.export" data-testid="sql-history-export" @click="openExportDialog">
              {{ t('sqlHistory.actions.exportEvidence') }}
            </el-button>
            <el-button @click="evidenceDrawerVisible = true">
              {{ t('sqlHistory.actions.viewRawEvidence') }}
            </el-button>
          </div>
        </div>

        <div class="association-strip" data-testid="sql-history-association-links">
          <el-button :disabled="!hasParseHistoryLink" data-testid="sql-history-open-parse-history" @click="openParseRecordForContext()">
            {{ t('sqlHistory.actions.openParseHistory') }}
            <span class="button-subline">{{ parseHistoryLinkText }}</span>
          </el-button>
          <el-button
            :disabled="!firstRecommendationRef"
            data-testid="sql-history-open-recommendation-result"
            @click="openRecommendationCenter(firstRecommendationRef)"
          >
            {{ t('sqlHistory.actions.openRecommendationResult') }}
            <span class="button-subline">{{ recommendationRefRows.length || '-' }}</span>
          </el-button>
          <el-button data-testid="sql-history-focus-rewrite-records" @click="focusRewriteRecords">
            {{ t('sqlHistory.actions.focusRewriteRecords') }}
            <span class="button-subline">{{ rewriteRecordsResponse?.rewriteRecordCount ?? rewriteRecordRows.length }}</span>
          </el-button>
        </div>

        <el-tabs v-model="activeDetailTab" data-testid="sql-history-detail-tabs" @tab-change="handleDetailTabChange">
          <el-tab-pane :label="t('sqlHistory.tabs.overview')" name="overview">
            <div class="detail-grid">
              <div v-for="item in detailCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="item.testId || undefined">{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div v-if="contextCards.length" class="detail-grid detail-grid-spaced">
              <div v-for="item in contextCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('sqlHistory.tabs.execution')" name="execution">
            <div
              v-if="rewriteLinkedButNotApplied"
              class="inline-banner inline-banner-warning"
              data-testid="sql-history-rewrite-linked-not-applied"
            >
              <strong>{{ t('sqlHistory.rewriteAudit.linkedNotAppliedTitle') }}</strong>
              <span>{{ t('sqlHistory.rewriteAudit.linkedNotAppliedMessage') }}</span>
            </div>
            <div class="detail-grid">
              <div v-for="item in executionCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div
              v-if="rewriteAuditCards.length"
              class="detail-grid detail-grid-spaced"
              data-testid="sql-history-rewrite-audit"
            >
              <div v-for="item in rewriteAuditCards" :key="item.key" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="item.testId">{{ displayValue(item.value) }}</strong>
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

          <el-tab-pane :label="t('sqlHistory.tabs.rewriteRecords')" name="rewriteRecords">
            <div class="rewrite-records-panel" data-testid="sql-history-rewrite-records-tab">
              <div class="table-heading">
                <div class="detail-grid rewrite-summary-grid">
                  <div v-for="item in rewriteRecordSummaryCards" :key="item.key" class="detail-grid__item">
                    <span>{{ item.label }}</span>
                    <strong>{{ displayValue(item.value) }}</strong>
                  </div>
                </div>
                <el-button
                  :loading="loading.rewriteRecords"
                  data-testid="sql-history-rewrite-records-refresh"
                  @click="loadRewriteRecords({ force: true })"
                >
                  {{ t('sqlHistory.actions.refreshRewriteRecords') }}
                </el-button>
              </div>
              <div
                v-if="rewriteLinkedButNotApplied"
                class="inline-banner inline-banner-warning"
                data-testid="sql-history-rewrite-records-not-applied"
              >
                <strong>{{ t('sqlHistory.rewriteAudit.linkedNotAppliedTitle') }}</strong>
                <span>{{ t('sqlHistory.rewriteAudit.linkedNotAppliedMessage') }}</span>
              </div>
              <div
                v-if="rewriteRecordErrorMessage"
                class="inline-banner inline-banner-danger"
                data-testid="sql-history-rewrite-records-error"
              >
                <strong>{{ t('sqlHistory.states.errorTitle') }}</strong>
                <span>{{ rewriteRecordErrorMessage }}</span>
              </div>
              <el-table
                v-loading="loading.rewriteRecords"
                :data="rewriteRecordRows"
                border
                data-testid="sql-history-rewrite-record-table"
              >
                <el-table-column :label="t('sqlHistory.rewriteRecords.rewriteRecordId')" min-width="190">
                  <template #default="{ row }">
                    <button
                      v-if="row.rewriteRecordId"
                      type="button"
                      class="table-link"
                      data-testid="sql-history-rewrite-record-detail-link"
                      @click="openRecommendationCenter(row)"
                    >
                      {{ row.rewriteRecordId }}
                    </button>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column :label="t('sqlHistory.rewriteRecords.recommendationId')" min-width="190">
                  <template #default="{ row }">
                    <button
                      v-if="row.recommendationId"
                      type="button"
                      class="table-link"
                      data-testid="sql-history-rewrite-record-recommendation-link"
                      @click="openRecommendationCenter(row)"
                    >
                      {{ row.recommendationId }}
                    </button>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="sourceType" :label="t('sqlHistory.rewriteRecords.sourceType')" min-width="110" />
                <el-table-column prop="sourceKind" :label="t('sqlHistory.rewriteRecords.sourceKind')" min-width="150" />
                <el-table-column prop="evidenceLevel" :label="t('sqlHistory.rewriteRecords.evidenceLevel')" min-width="150" />
                <el-table-column prop="validationStatus" :label="t('sqlHistory.rewriteRecords.validationStatus')" min-width="160" />
                <el-table-column prop="lastValidationRunId" :label="t('sqlHistory.rewriteRecords.lastValidationRunId')" min-width="180" />
                <el-table-column :label="t('sqlHistory.rewriteRecords.lastComparedAt')" min-width="170">
                  <template #default="{ row }">{{ formatTimestamp(row.lastComparedAt) }}</template>
                </el-table-column>
                <el-table-column :label="t('sqlHistory.rewriteRecords.alertStatus')" min-width="150">
                  <template #default="{ row }">
                    {{ displayValue(row.alertStatus) }}
                  </template>
                </el-table-column>
                <el-table-column :label="t('sqlHistory.rewriteRecords.autoApplyPaused')" min-width="160">
                  <template #default="{ row }">{{ displayValue(booleanDisplay(rewriteRecordAutoApplyPaused(row))) }}</template>
                </el-table-column>
                <template #empty>
                  <el-empty :description="t('sqlHistory.states.noRewriteRecords')" />
                </template>
              </el-table>

              <article
                v-for="record in rewriteRecordRows"
                :key="record.rewriteRecordId"
                class="rewrite-record-detail"
                data-testid="sql-history-rewrite-record-diff"
              >
                <div class="table-heading">
                  <div>
                    <p class="section-kicker">{{ record.rewriteRecordId }}</p>
                    <h3 class="section-title section-title-small">{{ displayValue(record.validationStatus) }}</h3>
                  </div>
                  <span :class="statusClass(record.validationStatus)">{{ displayValue(record.alertStatus) }}</span>
                </div>
                <div class="detail-grid">
                  <div class="detail-grid__item">
                    <span>{{ t('sqlHistory.rewriteRecords.sourceId') }}</span>
                    <strong>{{ displayValue(record.sourceId) }}</strong>
                  </div>
                  <div class="detail-grid__item">
                    <span>{{ t('sqlHistory.rewriteRecords.parseHistoryId') }}</span>
                    <button
                      v-if="record.parseHistoryId"
                      type="button"
                      class="table-link"
                      data-testid="sql-history-rewrite-record-parse-history-link"
                      @click="openParseRecordForContext(record)"
                    >
                      {{ record.parseHistoryId }}
                    </button>
                    <strong v-else>-</strong>
                  </div>
                  <div class="detail-grid__item">
                    <span>{{ t('sqlHistory.rewriteRecords.sqlFingerprint') }}</span>
                    <strong>{{ displayValue(record.sqlFingerprint) }}</strong>
                  </div>
                  <div class="detail-grid__item">
                    <span>{{ t('sqlHistory.rewriteRecords.manualReviewRequired') }}</span>
                    <strong>{{ displayValue(booleanDisplay(record.manualReviewRequired)) }}</strong>
                  </div>
                  <div class="detail-grid__item">
                    <span>{{ t('sqlHistory.rewriteRecords.autoApplyPaused') }}</span>
                    <strong data-testid="sql-history-rewrite-record-auto-apply-paused">{{ displayValue(booleanDisplay(rewriteRecordAutoApplyPaused(record))) }}</strong>
                  </div>
                  <div class="detail-grid__item">
                    <span>{{ t('sqlHistory.rewriteRecords.alertRefs') }}</span>
                    <strong>{{ rewriteRecordAlertRefs(record).length }}</strong>
                  </div>
                </div>
                <div class="code-grid">
                  <article v-if="hasDisplayValue(record.originalSqlText)" class="code-card">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.originalSql') }}</div>
                    <SqlCodeBlock
                      :value="record.originalSqlText"
                      :copy-label="t('sqlHistory.actions.copy')"
                      :auto-format="false"
                      data-testid="sql-history-rewrite-record-original-sql"
                    />
                  </article>
                  <article v-if="hasDisplayValue(record.recommendedSqlText)" class="code-card">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.recommendedSql') }}</div>
                    <SqlCodeBlock
                      :value="record.recommendedSqlText"
                      :copy-label="t('sqlHistory.actions.copy')"
                      data-testid="sql-history-rewrite-record-recommended-sql"
                    />
                  </article>
                  <article v-if="hasDisplayValue(record.executedSqlText)" class="code-card">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.executedSql') }}</div>
                    <SqlCodeBlock
                      :value="record.executedSqlText"
                      :copy-label="t('sqlHistory.actions.copy')"
                      data-testid="sql-history-rewrite-record-executed-sql"
                    />
                  </article>
                  <article class="code-card">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.diffSummary') }}</div>
                    <pre class="code-block">{{ formatJson(record.diffSummary || {}) }}</pre>
                  </article>
                  <article class="code-card">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.ruleChain') }}</div>
                    <pre class="code-block">{{ formatJson(record.ruleChain || []) }}</pre>
                  </article>
                  <article class="code-card">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.traceRefs') }}</div>
                    <pre class="code-block">{{ formatJson(record.traceRefs || {}) }}</pre>
                  </article>
                  <article class="code-card" data-testid="sql-history-rewrite-record-alert-refs">
                    <div class="code-card__header">{{ t('sqlHistory.rewriteRecords.alertRefs') }}</div>
                    <pre class="code-block">{{ formatJson(rewriteRecordAlertRefs(record)) }}</pre>
                  </article>
                </div>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('sqlHistory.tabs.signals')" name="signals">
            <div class="code-grid">
              <article v-for="item in parseSignalCards" :key="item.key" class="code-card">
                <div class="code-card__header">{{ item.title }}</div>
                <pre class="code-block">{{ formatJson(item.payload) }}</pre>
              </article>
            </div>
            <el-empty
              v-if="!parseSignalCards.length"
              :description="t('sqlHistory.states.noSignalEvidence')"
            />
          </el-tab-pane>

          <el-tab-pane :label="t('sqlHistory.tabs.refs')" name="refs">
            <div v-if="recommendationRefRows.length" class="reference-action-list" data-testid="sql-history-recommendation-ref-actions">
              <button
                v-for="item in recommendationRefRows"
                :key="referenceValue(item, ['recommendationId', 'id'])"
                type="button"
                class="reference-action"
                @click="openRecommendationCenter(item)"
              >
                <span>{{ t('sqlHistory.actions.openRecommendationResult') }}</span>
                <strong>{{ referenceValue(item, ['recommendationId', 'id']) }}</strong>
              </button>
            </div>
            <div class="code-grid">
              <article v-for="group in referenceGroups" :key="group.key" class="code-card">
                <div class="code-card__header">{{ group.title }}</div>
                <pre class="code-block">{{ formatJson(group.items || []) }}</pre>
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
.history-workbench,
.filter-panel,
.table-panel,
.drawer-stack,
.dialog-stack,
.rewrite-records-panel,
.rewrite-record-detail {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
}

.sql-history-page {
  padding: var(--sqlforge-space-6);
  color: var(--sqlforge-text-primary);
}

.history-workbench {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-2);
  padding: var(--sqlforge-space-5);
}

.filter-panel,
.table-panel {
  padding-top: var(--sqlforge-space-4);
  border-top: 1px solid var(--sqlforge-border-subtle);
}

.sql-history-header {
  display: grid;
  gap: var(--sqlforge-space-4);
}

.rewrite-context-header {
  background: var(--sqlforge-surface-1);
}

.table-heading,
.dialog-header,
.banner-row,
.action-row,
.table-footer,
.association-strip,
.reference-action-list {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sqlforge-space-3);
  flex-wrap: wrap;
}

.association-strip {
  justify-content: flex-start;
  padding: var(--sqlforge-space-3);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
}

.button-subline {
  margin-left: var(--sqlforge-space-2);
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: var(--sqlforge-text-meta);
}

.reference-action-list {
  justify-content: flex-start;
  margin-bottom: var(--sqlforge-space-4);
}

.reference-action {
  display: inline-flex;
  flex-direction: column;
  gap: var(--sqlforge-space-1);
  min-width: 220px;
  padding: var(--sqlforge-space-3);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
  color: var(--sqlforge-text-primary);
  cursor: pointer;
  text-align: left;
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

.filter-form {
  width: 100%;
}

.field-grid,
.summary-strip,
.detail-grid,
.code-grid {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.field-grid {
  --filter-field-default-min: clamp(184px, 20ch, 320px);

  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
}

.filter-hint {
  margin-top: var(--sqlforge-space-3);
  color: #f4c84a;
  font-size: var(--sqlforge-text-meta);
}

.summary-strip {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.field-block,
.field-block-plain {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
}

.field-grid .field-block {
  flex: 1 1 var(--filter-field-min, var(--filter-field-default-min));
  min-width: min(100%, var(--filter-field-min, var(--filter-field-default-min)));
  max-width: min(100%, 360px);
}

.field-grid .field-block-input {
  max-width: min(100%, 420px);
}

.field-grid .field-block-date {
  max-width: min(100%, 280px);
}

.field-block :deep(.el-form-item__label) {
  color: var(--sqlforge-text-secondary);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-block :deep(.el-form-item__content),
.field-block :deep(.el-input),
.field-block :deep(.el-select),
.field-block :deep(.el-date-editor.el-input) {
  width: 100%;
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

.inline-banner-warning {
  border: 1px solid rgba(255, 205, 64, 0.34);
  background: var(--sqlforge-status-warning);
  color: #f4c84a;
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
  align-items: flex-end;
  justify-content: space-between;
}

.footer-status {
  flex: 0 1 320px;
  max-width: min(360px, 40%);
  min-width: 0;
}

.pagination-cluster {
  display: flex;
  align-items: flex-end;
  flex: 0 1 auto;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  max-width: 100%;
}

.pagination-row {
  justify-content: flex-end;
  max-width: 100%;
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

.rewrite-summary-grid {
  flex: 1 1 420px;
}

.rewrite-record-detail {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
  padding: var(--sqlforge-space-4);
}

@media (max-width: 720px) {
  .sql-history-page {
    padding: var(--sqlforge-space-4);
  }

  .table-heading,
  .dialog-header,
  .action-row,
  .table-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .footer-status {
    flex-basis: auto;
    max-width: 100%;
  }

  .pagination-cluster {
    align-items: flex-end;
  }

  .pagination-row {
    justify-content: flex-end;
  }
}
</style>
