import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { SAMPLE_TENANT_ID } from '../../config/tenantDefaults.mjs'
import {
  exportSqlParseHistory,
  formatRuntimeError,
  getGovernanceDatasources,
  getSqlParseHistoryDetail,
  getSqlParseHistoryPage,
  listParseBatches,
  listReportBatches,
  getReportBatch,
  getReportBatchIssueSceneDetail,
  getReportBatchParseStatistics
} from '../../services/runtimeGateApi'
import { buildDatasourceOptions, buildTenantOptions, withCurrentOption } from '../common/formComponentGovernance'
import { issueSceneHelpText, riskDisplayText as sharedRiskDisplayText } from '../common/issueSceneHelp.mjs'

export function useParseRecordView() {
const { locale, t } = useI18n()
const route = useRoute()
const router = useRouter()

const DEFAULT_HISTORY_CONTEXT_TENANT_ID = SAMPLE_TENANT_ID
const normalizeQueryValue = value => String(value || '').trim()
const HISTORY_WORKBENCH_TABS = new Set(['sqlHistory', 'batchHistory'])
const BATCH_HISTORY_TABS = new Set(['parse', 'report'])

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
  reportBatchDetail: false,
  reportBatchIssueSceneDetail: false,
  reportBatchItemDetails: false,
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
const activeHistoryWorkbenchTab = ref('sqlHistory')
const batchHistoryTab = ref('parse')
const activeReportBatchDetailTab = ref('overview')
const activeReportBatchStatisticsTab = ref('issueScene')
const selectedHistoryDetail = ref(null)
const selectedReportBatchDetail = ref(null)
const selectedReportIssueSceneDetail = ref(null)
const reportBatchItemDetails = ref({})
const selectedReportSqlDetailItem = ref(null)
const errorMessage = ref('')
const batchHistoryErrorMessage = ref('')
const reportBatchItemDetailErrorMessage = ref('')
const reportBatchDetailDrawerVisible = ref(false)
const reportBatchIssueSceneDetailDialogVisible = ref(false)
const reportSqlParseDetailDialogVisible = ref(false)
const exportDialogVisible = ref(false)
const exportResult = ref(null)
const historyPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
  pageCount: 0
})
const parseBatchHistoryPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
  pageCount: 0
})
const reportBatchHistoryPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
  pageCount: 0
})
const reportBatchSqlPagination = reactive({
  pageNumber: 1,
  pageSize: 25,
  reportCode: ''
})
const reportBatchIssueScenePagination = reactive({
  pageNumber: 1,
  pageSize: 25,
  reportCode: '',
  logicalObjectKey: ''
})
const reportBatchIssueSceneReportPagination = reactive({
  pageNumber: 1,
  pageSize: 10
})
const reportBatchIssueSceneLogicalObjectPagination = reactive({
  pageNumber: 1,
  pageSize: 10
})
const reportBatchIssueStatisticsPagination = reactive({
  pageNumber: 1,
  pageSize: 10
})
const REPORT_SQL_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]
const REPORT_STATISTIC_PAGE_SIZE_OPTIONS = [10, 25, 50]
const LIST_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]
function pageItems(items, pagination) {
  const source = Array.isArray(items) ? items : []
  const pageNumber = Math.max(1, Number(pagination?.pageNumber || 1))
  const pageSize = Math.max(1, Number(pagination?.pageSize || 10))
  const start = (pageNumber - 1) * pageSize
  return source.slice(start, start + pageSize)
}

function resetReportBatchIssueSceneLocalPages() {
  reportBatchIssueSceneReportPagination.pageNumber = 1
  reportBatchIssueSceneLogicalObjectPagination.pageNumber = 1
}

const exportForm = reactive({
  exportFormat: 'JSON',
  includeTraceDetail: true,
  exportReason: 'frontend-history-forensics'
})

const rows = computed(() => page.value?.items || [])
const parseBatchHistoryRows = ref([])
const reportBatchHistoryRows = ref([])
const isChinese = computed(() => locale.value === 'zh-CN')
const isBatchHistoryWorkbench = computed(() => activeHistoryWorkbenchTab.value === 'batchHistory')
const historyWorkbenchKicker = computed(() => isBatchHistoryWorkbench.value ? 'parse history workbench' : 'parse record workbench')
const historyWorkbenchTitle = computed(() => {
  if (isBatchHistoryWorkbench.value) {
    return t('inline.viewsParseRecordUseParseRecordView.text001')
  }
  return t('inline.viewsParseRecordUseParseRecordView.text002')
})
const routeTenantId = computed(() => normalizeQueryValue(route.query.tenantId))
const requestTenantId = computed(() =>
  normalizeQueryValue(form.tenantId)
    || routeTenantId.value
    || DEFAULT_HISTORY_CONTEXT_TENANT_ID
)
const tenantOptions = computed(() => buildTenantOptions(form.tenantId, rows.value))
const classificationSummary = computed(() => page.value?.classificationSummary || {})
const sortModeLabel = computed(() => {
  const sortBy = normalizeQueryValue(form.sortBy)
  const sortOrder = normalizeQueryValue(form.sortOrder)
  if (!sortBy && !sortOrder) {
    return t('inline.viewsParseRecordUseParseRecordView.text003')
  }
  return `${sortBy || 'submittedAt'} ${sortOrder || 'DESC'}`
})
const card = (label, value, key = '') => ({ label, value, key })
const parseBatchHistorySummary = computed(() => {
  const totalRecords = parseBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.totalRecords || 0), 0)
  const failedBatches = parseBatchHistoryRows.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text004'), parseBatchHistoryRows.value.length),
    card(t('inline.viewsParseRecordUseParseRecordView.text005'), parseBatchHistoryPagination.totalCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text006'), parseBatchHistoryPagination.pageCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text007'), totalRecords),
    card(t('inline.viewsParseRecordUseParseRecordView.text008'), failedBatches)
  ]
})
const reportBatchHistorySummary = computed(() => {
  const totalReports = reportBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.totalReports || 0), 0)
  const resolvedReports = reportBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.resolvedReports || 0), 0)
  const failedBatches = reportBatchHistoryRows.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text009'), reportBatchHistoryRows.value.length),
    card(t('inline.viewsParseRecordUseParseRecordView.text010'), reportBatchHistoryPagination.totalCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text011'), reportBatchHistoryPagination.pageCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text012'), totalReports),
    card(t('inline.viewsParseRecordUseParseRecordView.text013'), resolvedReports),
    card(t('inline.viewsParseRecordUseParseRecordView.text014'), failedBatches)
  ]
})
const selectedReportItems = computed(() => {
  const source = selectedReportBatchDetail.value?.reportItems || []
  return Array.isArray(source) ? source.filter(item => item && typeof item === 'object') : []
})
const selectedReportParseStatistics = computed(() => objectValue(selectedReportBatchDetail.value?.parseStatistics))
const selectedReportParseStatisticsOverview = computed(() => objectValue(selectedReportParseStatistics.value.overview))
const selectedReportIssueSceneStatistics = computed(() => normalizeArray(selectedReportParseStatistics.value.issueSceneStatistics))
const selectedReportImportanceStatistics = computed(() => normalizeArray(selectedReportParseStatistics.value.importanceStatistics))
const selectedReportBackendReportStatistics = computed(() => normalizeArray(selectedReportParseStatistics.value.reportStatistics))
const selectedReportBackendSqlStatistics = computed(() => normalizeArray(selectedReportParseStatistics.value.sqlStatistics))
const selectedReportPriorityMatrix = computed(() => normalizeArray(selectedReportParseStatistics.value.priorityMatrix))
const selectedReportLogicalObjectStatistics = computed(() =>
  normalizeArray(selectedReportParseStatistics.value.logicalObjectStatistics)
)
const selectedReportGroups = computed(() => {
  const groups = new Map()
  selectedReportItems.value.forEach(item => {
    const reportCode = item.reportCode || item.itemId || 'UNSPECIFIED'
    const group = groups.get(reportCode) || {
      reportCode,
      reportName: item.reportName,
      items: []
    }
    group.items.push(item)
    groups.set(reportCode, group)
  })
  return Array.from(groups.values()).map(group => {
    const total = group.items.length
    const resolved = group.items.filter(item => String(item.status || '').toUpperCase() === 'RESOLVED').length
    const failed = group.items.filter(item => String(item.status || '').toUpperCase() === 'FAILED').length
    const structureValid = group.items.filter(item => String(item.structureSyntaxStatus || '').toUpperCase() === 'VALID').length
    const accessConnected = group.items.filter(item =>
      String(item.accessServiceStatus || '').toUpperCase() === 'AVAILABLE' &&
      String(item.accessConnectionStatus || '').toUpperCase() === 'CONNECTED'
    ).length
    const issueScenes = new Set(group.items.flatMap(item => Array.isArray(item.issueScenes) ? item.issueScenes : []))
    const logicalObjects = new Set(group.items.flatMap(item => Array.isArray(item.logicalObjectKeys) ? item.logicalObjectKeys : []))
    return {
      ...group,
      total,
      resolved,
      failed,
      structureRate: rate(structureValid, total),
      accessRate: rate(accessConnected, total),
      issueSceneCount: issueScenes.size,
      logicalObjectCount: logicalObjects.size
    }
  })
})
const reportBatchDetailCards = computed(() => {
  const detail = selectedReportBatchDetail.value
  if (!detail) {
    return []
  }
  const overview = selectedReportParseStatisticsOverview.value
  const total = selectedReportItems.value.length
  const structureValid = selectedReportItems.value.filter(item => String(item.structureSyntaxStatus || '').toUpperCase() === 'VALID').length
  const accessConnected = selectedReportItems.value.filter(item =>
    String(item.accessServiceStatus || '').toUpperCase() === 'AVAILABLE' &&
    String(item.accessConnectionStatus || '').toUpperCase() === 'CONNECTED'
  ).length
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text015'), detail.status),
    card(t('inline.viewsParseRecordUseParseRecordView.text016'), detail.fileType),
    card(t('inline.viewsParseRecordUseParseRecordView.text017'), detail.totalReports),
    card(t('inline.viewsParseRecordUseParseRecordView.text018'), overview.totalSqlCount ?? detail.totalSqls ?? total),
    card(t('inline.viewsParseRecordUseParseRecordView.text019'), overview.issueSqlCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text020'), overview.importantSqlCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text021'), overview.urgentSqlCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text022'), selectedReportParseStatistics.value.mergeCandidateReportCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text023'), detail.resolvedSqls ?? detail.resolvedReports),
    card(t('inline.viewsParseRecordUseParseRecordView.text024'), detail.failedSqls ?? detail.failedReports),
    card(t('inline.viewsParseRecordUseParseRecordView.text025'), formatPercent(rate(structureValid, total))),
    card(t('inline.viewsParseRecordUseParseRecordView.text026'), formatPercent(rate(accessConnected, total)))
  ].filter(item => hasDisplayValue(item.value))
})
const reportBatchIssueStatistics = computed(() => {
  if (selectedReportIssueSceneStatistics.value.length) {
    return selectedReportIssueSceneStatistics.value.map(item => ({
      ...item,
      ratio: item.sqlRatio
    }))
  }
  const counts = new Map()
  selectedReportItems.value.forEach(item => {
    const scenes = Array.isArray(item.issueScenes) ? item.issueScenes : []
    scenes.forEach(scene => counts.set(scene, (counts.get(scene) || 0) + 1))
  })
  return Array.from(counts.entries())
    .map(([issueScene, affectedSqlCount]) => ({
      issueScene,
      affectedSqlCount,
      ratio: rate(affectedSqlCount, selectedReportItems.value.length)
    }))
    .sort((left, right) => right.affectedSqlCount - left.affectedSqlCount)
})
const reportBatchIssueStatisticsPage = computed(() => {
  return pageItems(reportBatchIssueStatistics.value, reportBatchIssueStatisticsPagination)
})
const selectedReportIssueSceneReportDetails = computed(() =>
  normalizeArray(selectedReportIssueSceneDetail.value?.reportDetails)
)
const selectedReportIssueSceneReportDetailsPage = computed(() =>
  selectedReportIssueSceneReportDetails.value
)
const selectedReportIssueSceneReportDetailTotalCount = computed(() =>
  Number(selectedReportIssueSceneDetail.value?.reportDetailTotalCount ?? selectedReportIssueSceneReportDetails.value.length)
)
const selectedReportIssueSceneLogicalObjectDetails = computed(() =>
  normalizeArray(selectedReportIssueSceneDetail.value?.logicalObjectDetails)
)
const selectedReportIssueSceneLogicalObjectDetailsPage = computed(() =>
  selectedReportIssueSceneLogicalObjectDetails.value
)
const selectedReportIssueSceneLogicalObjectDetailTotalCount = computed(() =>
  Number(selectedReportIssueSceneDetail.value?.logicalObjectDetailTotalCount ?? selectedReportIssueSceneLogicalObjectDetails.value.length)
)
const selectedReportIssueSceneSqlStatisticTotalCount = computed(() =>
  Number(selectedReportIssueSceneDetail.value?.sqlStatisticTotalCount ?? normalizeArray(selectedReportIssueSceneDetail.value?.sqlStatistics).length)
)
const reportBatchIssueSceneDetailCards = computed(() => {
  const detail = objectValue(selectedReportIssueSceneDetail.value)
  if (!detail.issueScene) {
    return []
  }
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text027'), detail.issueScene, 'issueScene'),
    card(t('inline.viewsParseRecordUseParseRecordView.text028'), detail.affectedSqlCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text029'), detail.affectedIssueCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text030'), detail.reportCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text031'), detail.logicalObjectCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text032'), detail.priorityLevel),
    card(t('inline.viewsParseRecordUseParseRecordView.text033'), detail.severity)
  ].filter(item => hasDisplayValue(item.value))
})
const reportBatchIssueSceneDetailDialogTitle = computed(() => {
  const detail = objectValue(selectedReportIssueSceneDetail.value)
  const title = t('inline.viewsParseRecordUseParseRecordView.text034')
  const scene = detail.issueScene || '-'
  const filters = [
    reportBatchIssueScenePagination.reportCode
      ? `${t('inline.viewsParseRecordUseParseRecordView.text035')}: ${reportBatchIssueScenePagination.reportCode}`
      : '',
    reportBatchIssueScenePagination.logicalObjectKey
      ? `${t('inline.viewsParseRecordUseParseRecordView.text036')}: ${reportBatchIssueScenePagination.logicalObjectKey}`
      : ''
  ].filter(Boolean)
  return filters.length ? `${title}: ${scene} (${filters.join(' · ')})` : `${title}: ${scene}`
})
const reportBatchLogicalObjectStatistics = computed(() => {
  if (selectedReportLogicalObjectStatistics.value.length) {
    return selectedReportLogicalObjectStatistics.value.map(item => ({
      ...item,
      hitCount: item.sqlCount
    }))
  }
  const counts = new Map()
  selectedReportItems.value.forEach(item => {
    const keys = Array.isArray(item.logicalObjectKeys) ? item.logicalObjectKeys : []
    keys.forEach(objectKey => counts.set(objectKey, (counts.get(objectKey) || 0) + 1))
  })
  return Array.from(counts.entries())
    .map(([objectKey, hitCount]) => ({ objectKey, hitCount }))
    .sort((left, right) => right.hitCount - left.hitCount)
})
const reportBatchParseDetailSummary = computed(() => {
  const total = selectedReportItems.value.filter(item => hasDisplayValue(reportHistoryIdForItem(item))).length
  const loaded = selectedReportItems.value.filter(item => Boolean(reportItemLoadedHistoryDetail(item))).length
  const batchEvidence = selectedReportItems.value.filter(item => Boolean(reportItemLocalDetail(item))).length
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text037'), total),
    card(t('inline.viewsParseRecordUseParseRecordView.text038'), loaded),
    card(t('inline.viewsParseRecordUseParseRecordView.text039'), batchEvidence),
    card(t('inline.viewsParseRecordUseParseRecordView.text040'), Math.max(total - loaded, 0))
  ]
})
const pageSummaryCards = computed(() => [
  { label: t('inline.viewsParseRecordUseParseRecordView.text041'), value: rows.value.length },
  { label: t('inline.viewsParseRecordUseParseRecordView.text042'), value: historyPagination.totalCount },
  { label: t('inline.viewsParseRecordUseParseRecordView.text043'), value: historyPagination.pageCount },
  { label: t('inline.viewsParseRecordUseParseRecordView.text044'), value: classificationSummary.value.SUCCESS || classificationSummary.value.succeeded || 0 },
  { label: t('inline.viewsParseRecordUseParseRecordView.text045'), value: classificationSummary.value.PARTIAL || classificationSummary.value.FAILED || classificationSummary.value.nonSuccess || 0 },
  { label: t('inline.viewsParseRecordUseParseRecordView.text046'), value: Object.keys(classificationSummary.value.accessChannelCounts || {}).length }
])
const detailSummaryCards = computed(() => {
  if (!selectedHistoryDetail.value) {
    return []
  }
  return [
    { label: 'Parse History ID', value: selectedHistoryDetail.value.parseHistoryId || selectedHistoryDetail.value.historyId },
    { label: 'Trace ID', value: selectedHistoryDetail.value.traceId },
    { label: t('inline.viewsParseRecordUseParseRecordView.text047'), value: selectedHistoryDetail.value.reportCode },
    { label: t('inline.viewsParseRecordUseParseRecordView.text048'), value: selectedHistoryDetail.value.sourceType },
    { label: t('inline.viewsParseRecordUseParseRecordView.text049'), value: selectedHistoryDetail.value.resultStatus },
    { label: t('inline.viewsParseRecordUseParseRecordView.text050'), value: selectedHistoryDetail.value.targetEngine },
    { label: t('inline.viewsParseRecordUseParseRecordView.text051'), value: selectedHistoryDetail.value.accessChannel },
    { label: t('inline.viewsParseRecordUseParseRecordView.text052'), value: formatTimestamp(selectedHistoryDetail.value.submittedAt) }
  ]
})
const traceSummaryCards = computed(() => {
  const traceDetail = selectedHistoryDetail.value?.traceDetail
  if (!traceDetail) {
    return []
  }
  return [
    { label: t('inline.viewsParseRecordUseParseRecordView.text053'), value: traceDetail.latestStatus },
    { label: t('inline.viewsParseRecordUseParseRecordView.text054'), value: traceDetail.auditEventCount },
    { label: t('inline.viewsParseRecordUseParseRecordView.text055'), value: traceDetail.nonSuccessEventCount },
    { label: t('inline.viewsParseRecordUseParseRecordView.text056'), value: traceDetail.queryHistoryCount }
  ]
})
const recommendationRefRows = computed(() =>
  normalizeArray(selectedHistoryDetail.value?.recommendationRefs)
    .map(item => (item && typeof item === 'object' ? item : { recommendationId: item }))
    .filter(item => hasDisplayValue(referenceValue(item, ['recommendationId', 'id'])))
)
const primaryRecommendationRef = computed(() => recommendationRefRows.value[0] || null)
const queryExecutionHistoryId = computed(() => {
  const histories = normalizeArray(selectedHistoryDetail.value?.traceDetail?.queryHistories)
  const queryHistory = histories.find(item => String(item?.historyType || '').toUpperCase() === 'QUERY_EXECUTION')
  return firstValue(
    selectedHistoryDetail.value?.queryHistoryId,
    selectedHistoryDetail.value?.executionHistoryId,
    queryHistory?.historyId
  )
})
const sqlHistoryLinkQuery = computed(() => {
  const detail = selectedHistoryDetail.value || {}
  return compactObject({
    tenantId: requestTenantId.value,
    historyId: queryExecutionHistoryId.value,
    traceId: detail.traceId,
    taskId: firstValue(detail.parseTaskId, detail.traceDetail?.taskId),
    reportId: firstValue(detail.reportId, detail.reportCode)
  })
})
const hasSqlHistoryLink = computed(() =>
  ['historyId', 'traceId', 'taskId', 'reportId'].some(key => hasDisplayValue(sqlHistoryLinkQuery.value[key]))
)
const sqlStateHighlights = computed(() => {
  const sqlState = selectedHistoryDetail.value?.sqlState || {}
  return [
    { key: 'sqlFingerprint', label: t('inline.viewsParseRecordUseParseRecordView.text057'), value: sqlState.sqlFingerprint || selectedHistoryDetail.value?.sqlFingerprint },
    { key: 'sqlTemplateFingerprint', label: t('inline.viewsParseRecordUseParseRecordView.text058'), value: sqlState.sqlTemplateFingerprint || selectedHistoryDetail.value?.sqlTemplateFingerprint },
    { key: 'boundSqlFingerprint', label: t('inline.viewsParseRecordUseParseRecordView.text059'), value: sqlState.boundSqlFingerprint || selectedHistoryDetail.value?.boundSqlFingerprint },
    { key: 'bindingMode', label: t('inline.viewsParseRecordUseParseRecordView.text060'), value: sqlState.bindingMode || selectedHistoryDetail.value?.bindingMode },
    { key: 'bindingRenderStatus', label: t('inline.viewsParseRecordUseParseRecordView.text061'), value: sqlState.bindingRenderStatus || selectedHistoryDetail.value?.bindingRenderStatus },
    { key: 'parameterizedSqlFlag', label: t('inline.viewsParseRecordUseParseRecordView.text062'), value: String(sqlState.parameterizedSqlFlag ?? selectedHistoryDetail.value?.parameterizedSqlFlag ?? '-') }
  ]
})
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
      label: t('inline.viewsParseRecordUseParseRecordView.text063'),
      value: selectedHistoryDetail.value?.sqlTemplateText,
      autoFormat: false
    },
    {
      key: 'boundSqlText',
      label: t('inline.viewsParseRecordUseParseRecordView.text064'),
      value: selectedHistoryDetail.value?.boundSqlText,
      autoFormat: false
    }
  ].filter(item => hasDisplayValue(item.value))
)
const historyQueryContext = computed(() => objectValue(selectedHistoryDetail.value?.queryContext))
const historyStructureParse = computed(() =>
  firstObject(
    selectedHistoryDetail.value?.structureParseSummary,
    historyQueryContext.value.structureParseSummary,
    historyQueryContext.value.structureParse
  )
)
const historyAccessParse = computed(() =>
  firstObject(
    selectedHistoryDetail.value?.accessParseSummary,
    historyQueryContext.value.accessParseSummary,
    historyQueryContext.value.accessParse
  )
)
const historyResultSummary = computed(() =>
  firstObject(
    historyQueryContext.value.resultSummary,
    selectedHistoryDetail.value?.executionSummary?.resultSummary,
    selectedHistoryDetail.value?.executionSummary
  )
)
const historyParseStatus = computed(() =>
  firstValue(
    historyResultSummary.value.overallStatus,
    selectedHistoryDetail.value?.resultStatus,
    historyStructureParse.value.syntaxStatus
  )
)
const historyParseTaskId = computed(() =>
  firstValue(historyQueryContext.value.parseTaskId, historyStructureParse.value.parseTaskId, selectedHistoryDetail.value?.traceDetail?.taskId)
)
const historyStructureIssues = computed(() => normalizeArray(historyStructureParse.value.issues))
const historyStructureRiskChecklist = computed(() => normalizeArray(historyStructureParse.value.riskChecklist))
const historyStructureIntentLabels = computed(() => normalizeArray(historyStructureParse.value.intentProfile?.classificationLabels))
const historyLogicalObjectHits = computed(() => {
  const fromStructure = normalizeArray(historyStructureParse.value.logicalObjectHits)
  return fromStructure.length ? fromStructure : logicalObjectHits.value
})
const historyParseSummaryCards = computed(() =>
  [
    card(t('inline.viewsParseRecordUseParseRecordView.text065'), historyParseStatus.value),
    card(t('inline.viewsParseRecordUseParseRecordView.text066'), historyParseTaskId.value),
    card(t('inline.viewsParseRecordUseParseRecordView.text067'), historyStructureParse.value.syntaxStatus),
    card(t('inline.viewsParseRecordUseParseRecordView.text068'), resolveAccessAvailable(historyResultSummary.value, historyAccessParse.value)),
    card(t('inline.viewsParseRecordUseParseRecordView.text069'), firstValue(historyResultSummary.value.degradeReason, historyAccessParse.value.degradeReason)),
    card(t('inline.viewsParseRecordUseParseRecordView.text070'), selectedHistoryDetail.value?.historyId ? (t('inline.viewsParseRecordUseParseRecordView.text071')) : ''),
    card('History ID', selectedHistoryDetail.value?.historyId)
  ].filter(item => hasDisplayValue(item.value))
)
const historyParseStatisticCards = computed(() => {
  const feature = objectValue(historyStructureParse.value.featureSummary)
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text072'), historyStructureIssues.value.length),
    card(t('inline.viewsParseRecordUseParseRecordView.text073'), historyStructureRiskChecklist.value.length),
    card(t('inline.viewsParseRecordUseParseRecordView.text074'), historyLogicalObjectHits.value.length),
    card(t('inline.viewsParseRecordUseParseRecordView.text075'), normalizeArray(historyStructureParse.value.riskTags).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text076'), normalizeArray(historyStructureParse.value.rewriteCandidates).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text077'), feature.tableCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text078'), feature.joinCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text079'), feature.predicateCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text080'), feature.orderByExpressionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text081'), feature.duplicateOrderByKeyCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text082'), feature.duplicateGroupByKeyCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text083'), feature.stringProjectionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text084'), feature.repeatedSubqueryCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text085'), feature.windowFunctionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text086'), feature.repeatedExpressionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text087'), booleanLabel(historyStructureParse.value.important)),
    card(t('inline.viewsParseRecordUseParseRecordView.text088'), booleanLabel(historyStructureParse.value.urgent))
  ].filter(item => hasDisplayValue(item.value))
})
const historyStructureHighlights = computed(() =>
  [
    { key: 'parseTaskId', label: t('inline.viewsParseRecordUseParseRecordView.text089'), value: historyParseTaskId.value },
    { key: 'sqlFingerprint', label: t('inline.viewsParseRecordUseParseRecordView.text090'), value: firstValue(historyStructureParse.value.sqlFingerprint, selectedHistoryDetail.value?.sqlFingerprint) },
    { key: 'syntaxStatus', label: t('inline.viewsParseRecordUseParseRecordView.text091'), value: historyStructureParse.value.syntaxStatus },
    { key: 'complexityLevel', label: t('inline.viewsParseRecordUseParseRecordView.text092'), value: historyStructureParse.value.complexityLevel },
    { key: 'sqlType', label: t('inline.viewsParseRecordUseParseRecordView.text093'), value: historyStructureParse.value.sqlType },
    { key: 'priorityLevel', label: t('inline.viewsParseRecordUseParseRecordView.text094'), value: historyStructureParse.value.priorityLevel },
    { key: 'priorityScore', label: t('inline.viewsParseRecordUseParseRecordView.text095'), value: historyStructureParse.value.priorityScore },
    { key: 'important', label: t('inline.viewsParseRecordUseParseRecordView.text096'), value: booleanLabel(historyStructureParse.value.important) },
    { key: 'urgent', label: t('inline.viewsParseRecordUseParseRecordView.text097'), value: booleanLabel(historyStructureParse.value.urgent) }
  ].filter(item => hasDisplayValue(item.value))
)
const historyStructureFeatureHighlights = computed(() => {
  const feature = objectValue(historyStructureParse.value.featureSummary)
  return [
    { key: 'parserEngine', label: t('inline.viewsParseRecordUseParseRecordView.text098'), value: feature.parserEngine },
    { key: 'scanMode', label: t('inline.viewsParseRecordUseParseRecordView.text099'), value: feature.scanMode },
    { key: 'joinType', label: t('inline.viewsParseRecordUseParseRecordView.text100'), value: feature.joinType },
    { key: 'computeDensity', label: t('inline.viewsParseRecordUseParseRecordView.text101'), value: feature.computeDensity },
    { key: 'resourceType', label: t('inline.viewsParseRecordUseParseRecordView.text102'), value: feature.resourceType },
    { key: 'slaLevel', label: t('inline.viewsParseRecordUseParseRecordView.text103'), value: feature.slaLevel },
    { key: 'tableCount', label: t('inline.viewsParseRecordUseParseRecordView.text104'), value: feature.tableCount },
    { key: 'joinCount', label: t('inline.viewsParseRecordUseParseRecordView.text105'), value: feature.joinCount },
    { key: 'predicateCount', label: t('inline.viewsParseRecordUseParseRecordView.text106'), value: feature.predicateCount },
    { key: 'orderByExpressionCount', label: t('inline.viewsParseRecordUseParseRecordView.text107'), value: feature.orderByExpressionCount },
    { key: 'duplicateOrderByKeyCount', label: t('inline.viewsParseRecordUseParseRecordView.text108'), value: feature.duplicateOrderByKeyCount },
    { key: 'duplicateGroupByKeyCount', label: t('inline.viewsParseRecordUseParseRecordView.text109'), value: feature.duplicateGroupByKeyCount },
    { key: 'groupByWithoutAggregate', label: t('inline.viewsParseRecordUseParseRecordView.text110'), value: booleanLabel(feature.groupByWithoutAggregate) },
    { key: 'aggregateFunctionCount', label: t('inline.viewsParseRecordUseParseRecordView.text111'), value: feature.aggregateFunctionCount },
    { key: 'stringProjectionCount', label: t('inline.viewsParseRecordUseParseRecordView.text112'), value: feature.stringProjectionCount },
    { key: 'stringConcatenationCount', label: t('inline.viewsParseRecordUseParseRecordView.text113'), value: feature.stringConcatenationCount },
    { key: 'largeStringAggregateCount', label: t('inline.viewsParseRecordUseParseRecordView.text114'), value: feature.largeStringAggregateCount },
    { key: 'repeatedSubqueryCount', label: t('inline.viewsParseRecordUseParseRecordView.text115'), value: feature.repeatedSubqueryCount },
    { key: 'windowFunctionCount', label: t('inline.viewsParseRecordUseParseRecordView.text116'), value: feature.windowFunctionCount },
    { key: 'repeatedExpressionCount', label: t('inline.viewsParseRecordUseParseRecordView.text117'), value: feature.repeatedExpressionCount }
  ].filter(item => hasDisplayValue(item.value))
})
const historyStructureResourceHighlights = computed(() => {
  const estimate = objectValue(historyStructureParse.value.estimatedResourceCost)
  return [
    { key: 'overall', label: t('inline.viewsParseRecordUseParseRecordView.text118'), value: estimate.overall },
    { key: 'cpu', label: 'CPU', value: estimate.cpu },
    { key: 'io', label: 'IO', value: estimate.io },
    { key: 'memory', label: t('inline.viewsParseRecordUseParseRecordView.text119'), value: estimate.memory },
    { key: 'network', label: t('inline.viewsParseRecordUseParseRecordView.text120'), value: estimate.network },
    { key: 'resultSize', label: t('inline.viewsParseRecordUseParseRecordView.text121'), value: estimate.resultSize }
  ].filter(item => hasDisplayValue(item.value))
})
const historyAccessHighlights = computed(() =>
  [
    { key: 'serviceStatus', label: t('inline.viewsParseRecordUseParseRecordView.text122'), value: historyAccessParse.value.serviceStatus },
    { key: 'connectionStatus', label: t('inline.viewsParseRecordUseParseRecordView.text123'), value: historyAccessParse.value.connectionStatus },
    { key: 'objectResolutionStatus', label: t('inline.viewsParseRecordUseParseRecordView.text124'), value: historyAccessParse.value.objectResolutionStatus },
    { key: 'partitionStatus', label: t('inline.viewsParseRecordUseParseRecordView.text125'), value: historyAccessParse.value.partitionStatus },
    { key: 'dataFreshnessStatus', label: t('inline.viewsParseRecordUseParseRecordView.text126'), value: historyAccessParse.value.dataFreshnessStatus },
    { key: 'slaStatus', label: 'SLA', value: historyAccessParse.value.slaStatus },
    { key: 'compatibilityStatus', label: t('inline.viewsParseRecordUseParseRecordView.text127'), value: historyAccessParse.value.compatibilityStatus }
  ].filter(item => hasDisplayValue(item.value))
)
const signalGroups = computed(() =>
  [
    { key: 'commentContext', title: t('inline.viewsParseRecordUseParseRecordView.text128'), payload: selectedHistoryDetail.value?.commentContext },
    { key: 'queryDateSummary', title: t('inline.viewsParseRecordUseParseRecordView.text129'), payload: selectedHistoryDetail.value?.queryDateSummary },
    { key: 'executionSummary', title: t('inline.viewsParseRecordUseParseRecordView.text130'), payload: selectedHistoryDetail.value?.executionSummary },
    { key: 'structureParseSummary', title: t('inline.viewsParseRecordUseParseRecordView.text131'), payload: selectedHistoryDetail.value?.structureParseSummary },
    { key: 'accessParseSummary', title: t('inline.viewsParseRecordUseParseRecordView.text132'), payload: selectedHistoryDetail.value?.accessParseSummary },
    { key: 'routeDecision', title: t('inline.viewsParseRecordUseParseRecordView.text133'), payload: selectedHistoryDetail.value?.routeDecision },
    { key: 'bindingSummary', title: t('inline.viewsParseRecordUseParseRecordView.text134'), payload: selectedHistoryDetail.value?.bindingSummary }
  ].filter(group => isNonEmpty(group.payload))
)
const referenceGroups = computed(() =>
  [
    { key: 'recommendationRefs', title: t('inline.viewsParseRecordUseParseRecordView.text135'), items: selectedHistoryDetail.value?.recommendationRefs || [] },
    { key: 'benchmarkRefs', title: t('inline.viewsParseRecordUseParseRecordView.text136'), items: selectedHistoryDetail.value?.benchmarkRefs || [] },
    { key: 'alertRefs', title: t('inline.viewsParseRecordUseParseRecordView.text137'), items: selectedHistoryDetail.value?.alertRefs || [] },
    { key: 'auditRefs', title: t('inline.viewsParseRecordUseParseRecordView.text138'), items: selectedHistoryDetail.value?.auditRefs || [] }
  ].filter(group => Array.isArray(group.items) && group.items.length > 0)
)
const logicalObjectHits = computed(() => normalizeArray(selectedHistoryDetail.value?.logicalObjectHits))
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

const normalizePagedList = (payload, fallbackPageSize) => {
  if (Array.isArray(payload)) {
    return {
      items: payload,
      pageNo: 1,
      pageSize: fallbackPageSize,
      totalCount: payload.length,
      pageCount: payload.length ? 1 : 0
    }
  }
  const items = Array.isArray(payload?.items) ? payload.items : []
  const pageSize = Number(payload?.pageSize || fallbackPageSize)
  return {
    items,
    pageNo: Number(payload?.pageNo || 1),
    pageSize,
    totalCount: Number(payload?.totalCount ?? items.length),
    pageCount: Number(payload?.pageCount ?? (items.length ? 1 : 0))
  }
}

const applyPaginationResult = (target, payload, fallbackPageSize) => {
  target.pageNo = Number(payload.pageNo || 1)
  target.pageSize = Number(payload.pageSize || fallbackPageSize)
  target.totalCount = Number(payload.totalCount || 0)
  target.pageCount = Number(payload.pageCount || 0)
}

const loadPage = async () => {
  syncDateRangeFields()
  loading.page = true
  errorMessage.value = ''
  try {
    page.value = await getSqlParseHistoryPage(
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
        traceId: form.traceId,
        parseTaskId: form.taskId,
        submittedStart: form.submittedStart,
        submittedEnd: form.submittedEnd,
        sortBy: form.sortBy,
        sortOrder: form.sortOrder,
        pageNo: historyPagination.pageNo,
        pageSize: historyPagination.pageSize
      },
      {
        requestPrefix: 'frontend-parse-record-parse-history-page'
      }
    )
    const normalizedPage = normalizePagedList(page.value, historyPagination.pageSize)
    applyPaginationResult(historyPagination, normalizedPage, historyPagination.pageSize)
  } catch (error) {
    page.value = null
    applyPaginationResult(historyPagination, { pageNo: historyPagination.pageNo, pageSize: historyPagination.pageSize }, historyPagination.pageSize)
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadBatchHistories = async () => {
  batchHistoryErrorMessage.value = ''
  const [parseResult, reportResult] = await Promise.allSettled([
    listParseBatches(requestTenantId.value, parseBatchHistoryPagination),
    listReportBatches(requestTenantId.value, reportBatchHistoryPagination)
  ])
  if (parseResult.status === 'fulfilled') {
    const parsePage = normalizePagedList(parseResult.value, parseBatchHistoryPagination.pageSize)
    parseBatchHistoryRows.value = parsePage.items
    applyPaginationResult(parseBatchHistoryPagination, parsePage, parseBatchHistoryPagination.pageSize)
  } else {
    parseBatchHistoryRows.value = []
  }
  if (reportResult.status === 'fulfilled') {
    const reportPage = normalizePagedList(reportResult.value, reportBatchHistoryPagination.pageSize)
    reportBatchHistoryRows.value = reportPage.items
    applyPaginationResult(reportBatchHistoryPagination, reportPage, reportBatchHistoryPagination.pageSize)
  } else {
    reportBatchHistoryRows.value = []
  }
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

const searchWorkbench = async () => {
  historyPagination.pageNo = 1
  parseBatchHistoryPagination.pageNo = 1
  reportBatchHistoryPagination.pageNo = 1
  await refreshWorkbench()
}

const handleHistoryPageChange = async pageNo => {
  historyPagination.pageNo = pageNo
  await loadPage()
}

const handleHistoryPageSizeChange = async pageSize => {
  historyPagination.pageSize = pageSize
  historyPagination.pageNo = 1
  await loadPage()
}

const handleParseBatchHistoryPageChange = async pageNo => {
  parseBatchHistoryPagination.pageNo = pageNo
  await loadBatchHistories()
}

const handleParseBatchHistoryPageSizeChange = async pageSize => {
  parseBatchHistoryPagination.pageSize = pageSize
  parseBatchHistoryPagination.pageNo = 1
  await loadBatchHistories()
}

const handleReportBatchHistoryPageChange = async pageNo => {
  reportBatchHistoryPagination.pageNo = pageNo
  await loadBatchHistories()
}

const handleReportBatchHistoryPageSizeChange = async pageSize => {
  reportBatchHistoryPagination.pageSize = pageSize
  reportBatchHistoryPagination.pageNo = 1
  await loadBatchHistories()
}

const openHistoryDetail = async (historyId, preloadedTraceDetail = null) => {
  if (!historyId) {
    return
  }
  loading.detail = true
  errorMessage.value = ''
  activeDialogTab.value = 'parseResult'
  selectedHistoryId.value = historyId
  try {
    const detail = await getSqlParseHistoryDetail(requestTenantId.value, historyId, {
      requestPrefix: 'frontend-parse-record-parse-history-detail'
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
    errorMessage.value = t('inline.viewsParseRecordUseParseRecordView.text139')
    return
  }
  loading.lookup = true
  errorMessage.value = ''
  try {
    if (normalizeQueryValue(form.reportId) && !normalizeQueryValue(form.reportCode)) {
      form.reportCode = form.reportId
    }
    historyPagination.pageNo = 1
    await loadPage()
    if (!rows.value.length) {
      errorMessage.value = t('inline.viewsParseRecordUseParseRecordView.text140')
    }
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
  form.sortBy = ''
  form.sortOrder = ''
  form.submittedStart = ''
  form.submittedEnd = ''
  submittedAtRange.value = []
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  await searchWorkbench()
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
    exportResult.value = await exportSqlParseHistory(
      requestTenantId.value,
      {
        parseHistoryId: selectedHistoryDetail.value.parseHistoryId || selectedHistoryDetail.value.historyId,
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

const openSqlHistoryFromDetail = () => {
  if (!hasSqlHistoryLink.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.sqlHistory,
    query: sqlHistoryLinkQuery.value
  })
}

const openRecommendationCenterFromDetail = (recommendation = primaryRecommendationRef.value) => {
  const recommendationId = normalizeQueryValue(referenceValue(recommendation, ['recommendationId', 'id']))
  const rewriteRecordId = normalizeQueryValue(referenceValue(recommendation, ['rewriteRecordId']))
  if (!recommendationId && !rewriteRecordId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.recommendationCenter,
    query: compactObject({
      tenantId: requestTenantId.value,
      recommendationId,
      rewriteRecordId
    })
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

const loadReportBatchWithStatistics = async (batchId, options = {}) => {
  const detail = await getReportBatch(batchId, requestTenantId.value, {
    pageNumber: options.pageNumber,
    pageSize: options.pageSize,
    reportCode: options.reportCode,
    requestPrefix: 'frontend-parse-record-report-batch-detail'
  })
  try {
    const statistics = await getReportBatchParseStatistics(batchId, requestTenantId.value, {
      requestPrefix: 'frontend-parse-record-report-batch-statistics'
    })
    return {
      ...detail,
      parseStatistics: statistics || detail?.parseStatistics
    }
  } catch {
    return detail
  }
}

const resetReportBatchIssueSceneDetail = () => {
  selectedReportIssueSceneDetail.value = null
  reportBatchIssueSceneDetailDialogVisible.value = false
  reportBatchIssueScenePagination.pageNumber = 1
  reportBatchIssueScenePagination.pageSize = 25
  reportBatchIssueScenePagination.reportCode = ''
  reportBatchIssueScenePagination.logicalObjectKey = ''
  reportBatchIssueSceneReportPagination.pageNumber = 1
  reportBatchIssueSceneReportPagination.pageSize = 10
  reportBatchIssueSceneLogicalObjectPagination.pageNumber = 1
  reportBatchIssueSceneLogicalObjectPagination.pageSize = 10
}

const syncReportBatchIssueSceneDetailPagination = detail => {
  const reportPageNumber = Number(detail?.reportDetailPageNumber)
  const reportPageSize = Number(detail?.reportDetailPageSize)
  const logicalObjectPageNumber = Number(detail?.logicalObjectDetailPageNumber)
  const logicalObjectPageSize = Number(detail?.logicalObjectDetailPageSize)
  const sqlPageNumber = Number(detail?.sqlStatisticPageNumber)
  const sqlPageSize = Number(detail?.sqlStatisticPageSize)
  if (Number.isFinite(reportPageNumber) && reportPageNumber > 0) {
    reportBatchIssueSceneReportPagination.pageNumber = reportPageNumber
  }
  if (Number.isFinite(reportPageSize) && reportPageSize > 0) {
    reportBatchIssueSceneReportPagination.pageSize = reportPageSize
  }
  if (Number.isFinite(logicalObjectPageNumber) && logicalObjectPageNumber > 0) {
    reportBatchIssueSceneLogicalObjectPagination.pageNumber = logicalObjectPageNumber
  }
  if (Number.isFinite(logicalObjectPageSize) && logicalObjectPageSize > 0) {
    reportBatchIssueSceneLogicalObjectPagination.pageSize = logicalObjectPageSize
  }
  if (Number.isFinite(sqlPageNumber) && sqlPageNumber > 0) {
    reportBatchIssueScenePagination.pageNumber = sqlPageNumber
  }
  if (Number.isFinite(sqlPageSize) && sqlPageSize > 0) {
    reportBatchIssueScenePagination.pageSize = sqlPageSize
  }
}

const openReportBatchDetail = async row => {
  const batchId = typeof row === 'string' ? row : row?.batchId
  if (!batchId) {
    return
  }
  loading.reportBatchDetail = true
  batchHistoryErrorMessage.value = ''
  reportBatchItemDetailErrorMessage.value = ''
  reportBatchItemDetails.value = {}
  selectedReportSqlDetailItem.value = null
  reportSqlParseDetailDialogVisible.value = false
  resetReportBatchIssueSceneDetail()
  reportBatchSqlPagination.pageNumber = 1
  reportBatchSqlPagination.pageSize = 25
  reportBatchSqlPagination.reportCode = ''
  reportBatchIssueStatisticsPagination.pageNumber = 1
  reportBatchIssueStatisticsPagination.pageSize = 10
  activeReportBatchDetailTab.value = 'overview'
  activeReportBatchStatisticsTab.value = 'issueScene'
  try {
    selectedReportBatchDetail.value = await loadReportBatchWithStatistics(batchId, reportBatchSqlPagination)
    reportBatchDetailDrawerVisible.value = true
  } catch (error) {
    selectedReportBatchDetail.value = null
    batchHistoryErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportBatchDetail = false
  }
}

const refreshSelectedReportSqlPage = async () => {
  const batchId = selectedReportBatchDetail.value?.batchId
  if (!batchId) {
    return
  }
  loading.reportBatchDetail = true
  batchHistoryErrorMessage.value = ''
  try {
    selectedReportBatchDetail.value = await loadReportBatchWithStatistics(batchId, reportBatchSqlPagination)
  } catch (error) {
    batchHistoryErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportBatchDetail = false
  }
}

const loadReportBatchIssueSceneDetail = async issueScene => {
  const batchId = selectedReportBatchDetail.value?.batchId
  const normalizedIssueScene = normalizeQueryValue(issueScene)
  if (!batchId || !normalizedIssueScene) {
    return
  }
  loading.reportBatchIssueSceneDetail = true
  batchHistoryErrorMessage.value = ''
  try {
    selectedReportIssueSceneDetail.value = await getReportBatchIssueSceneDetail(
      batchId,
      normalizedIssueScene,
      requestTenantId.value,
      {
        pageNumber: reportBatchIssueScenePagination.pageNumber,
        pageSize: reportBatchIssueScenePagination.pageSize,
        reportDetailPageNumber: reportBatchIssueSceneReportPagination.pageNumber,
        reportDetailPageSize: reportBatchIssueSceneReportPagination.pageSize,
        logicalObjectDetailPageNumber: reportBatchIssueSceneLogicalObjectPagination.pageNumber,
        logicalObjectDetailPageSize: reportBatchIssueSceneLogicalObjectPagination.pageSize,
        reportCode: reportBatchIssueScenePagination.reportCode,
        logicalObjectKey: reportBatchIssueScenePagination.logicalObjectKey,
        requestPrefix: 'frontend-parse-record-report-issue-scene-detail'
      }
    )
    syncReportBatchIssueSceneDetailPagination(selectedReportIssueSceneDetail.value)
    reportBatchIssueSceneDetailDialogVisible.value = Boolean(selectedReportIssueSceneDetail.value)
  } catch (error) {
    selectedReportIssueSceneDetail.value = null
    reportBatchIssueSceneDetailDialogVisible.value = false
    batchHistoryErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportBatchIssueSceneDetail = false
  }
}

const openReportBatchIssueSceneDetail = async item => {
  selectedReportIssueSceneDetail.value = null
  reportBatchIssueSceneDetailDialogVisible.value = false
  reportBatchIssueScenePagination.pageNumber = 1
  reportBatchIssueScenePagination.reportCode = ''
  reportBatchIssueScenePagination.logicalObjectKey = ''
  resetReportBatchIssueSceneLocalPages()
  await loadReportBatchIssueSceneDetail(item?.issueScene)
  if (selectedReportIssueSceneDetail.value) {
    activeReportBatchDetailTab.value = 'statistics'
    activeReportBatchStatisticsTab.value = 'issueScene'
  }
}

const applyReportBatchIssueSceneFilter = async () => {
  reportBatchIssueScenePagination.pageNumber = 1
  resetReportBatchIssueSceneLocalPages()
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueScenePageChange = async pageNumber => {
  reportBatchIssueScenePagination.pageNumber = pageNumber
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueScenePageSizeChange = async pageSize => {
  reportBatchIssueScenePagination.pageSize = pageSize
  reportBatchIssueScenePagination.pageNumber = 1
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueStatisticsPageChange = pageNumber => {
  reportBatchIssueStatisticsPagination.pageNumber = pageNumber
}

const handleReportBatchIssueStatisticsPageSizeChange = pageSize => {
  reportBatchIssueStatisticsPagination.pageSize = pageSize
  reportBatchIssueStatisticsPagination.pageNumber = 1
}

const handleReportBatchIssueSceneReportPageChange = async pageNumber => {
  reportBatchIssueSceneReportPagination.pageNumber = pageNumber
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueSceneReportPageSizeChange = async pageSize => {
  reportBatchIssueSceneReportPagination.pageSize = pageSize
  reportBatchIssueSceneReportPagination.pageNumber = 1
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueSceneLogicalObjectPageChange = async pageNumber => {
  reportBatchIssueSceneLogicalObjectPagination.pageNumber = pageNumber
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueSceneLogicalObjectPageSizeChange = async pageSize => {
  reportBatchIssueSceneLogicalObjectPagination.pageSize = pageSize
  reportBatchIssueSceneLogicalObjectPagination.pageNumber = 1
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const clearReportBatchIssueSceneReportFilter = async () => {
  reportBatchIssueScenePagination.reportCode = ''
  reportBatchIssueScenePagination.pageNumber = 1
  resetReportBatchIssueSceneLocalPages()
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const clearReportBatchIssueSceneLogicalObjectFilter = async () => {
  reportBatchIssueScenePagination.logicalObjectKey = ''
  reportBatchIssueScenePagination.pageNumber = 1
  resetReportBatchIssueSceneLocalPages()
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const selectReportBatchIssueSceneReport = async row => {
  const nextReportCode = normalizeQueryValue(row?.reportCode)
  if (!nextReportCode) {
    return
  }
  reportBatchIssueScenePagination.reportCode = nextReportCode
  const currentObject = normalizeQueryValue(reportBatchIssueScenePagination.logicalObjectKey)
  const rowObjects = Array.isArray(row?.logicalObjectKeys) ? row.logicalObjectKeys : []
  if (currentObject && !rowObjects.includes(currentObject)) {
    reportBatchIssueScenePagination.logicalObjectKey = ''
  }
  reportBatchIssueScenePagination.pageNumber = 1
  resetReportBatchIssueSceneLocalPages()
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const selectReportBatchIssueSceneLogicalObject = async row => {
  const nextObjectKey = normalizeQueryValue(row?.objectKey)
  if (!nextObjectKey) {
    return
  }
  reportBatchIssueScenePagination.logicalObjectKey = nextObjectKey
  reportBatchIssueScenePagination.pageNumber = 1
  resetReportBatchIssueSceneLocalPages()
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const issueSceneReportRowClassName = ({ row }) =>
  normalizeQueryValue(row?.reportCode) === normalizeQueryValue(reportBatchIssueScenePagination.reportCode)
    ? 'linked-table-row-active'
    : ''

const issueSceneLogicalObjectRowClassName = ({ row }) =>
  normalizeQueryValue(row?.objectKey) === normalizeQueryValue(reportBatchIssueScenePagination.logicalObjectKey)
    ? 'linked-table-row-active'
    : ''

const applyReportBatchSqlFilter = async () => {
  reportBatchSqlPagination.pageNumber = 1
  await refreshSelectedReportSqlPage()
}

const handleReportBatchSqlPageChange = async pageNumber => {
  reportBatchSqlPagination.pageNumber = pageNumber
  await refreshSelectedReportSqlPage()
}

const handleReportBatchSqlPageSizeChange = async pageSize => {
  reportBatchSqlPagination.pageSize = pageSize
  reportBatchSqlPagination.pageNumber = 1
  await refreshSelectedReportSqlPage()
}

const loadReportBatchItemDetails = async items => {
  const detailTargets = Array.from(new Map(
    (Array.isArray(items) ? items : [])
      .map(item => ({
        detailKey: reportItemDetailKey(item),
        historyId: reportHistoryIdForItem(item),
        item
      }))
      .filter(target => target.detailKey && target.historyId && !reportBatchItemDetails.value[target.detailKey])
      .map(target => [target.detailKey, target])
  ).values())
  if (!detailTargets.length) {
    return
  }
  loading.reportBatchItemDetails = true
  reportBatchItemDetailErrorMessage.value = ''
  try {
    const results = await Promise.allSettled(
      detailTargets.map(async target => {
        const detail = await getSqlParseHistoryDetail(
          requestTenantId.value,
          target.historyId,
          {
            requestPrefix: 'frontend-parse-record-report-sql-parse-history-detail'
          }
        )
        return [target.detailKey, detail]
      })
    )
    const next = { ...reportBatchItemDetails.value }
    let failedCount = 0
    results.forEach((result, index) => {
      const target = detailTargets[index]
      if (result.status === 'fulfilled') {
        next[result.value[0]] = result.value[1]
        return
      }
      failedCount += 1
      if (target?.detailKey) {
        next[target.detailKey] = buildReportItemFallbackDetail(target.item)
      }
    })
    reportBatchItemDetails.value = next
    if (failedCount > 0) {
      reportBatchItemDetailErrorMessage.value = isChinese.value
        ? `${failedCount} 条治理解析详情暂不可用，已展示批次内 SQL 证据。`
        : `${failedCount} governance parse detail records are unavailable; report-batch SQL evidence is shown.`
    }
  } finally {
    loading.reportBatchItemDetails = false
  }
}

const loadReportBatchItemDetail = async item => {
  await loadReportBatchItemDetails([item])
  selectedReportSqlDetailItem.value = item || null
  if (reportItemParseDetail(item)) {
    reportSqlParseDetailDialogVisible.value = true
  }
}

const openReportSqlParseDetail = item => {
  selectedReportSqlDetailItem.value = item || null
  if (reportItemParseDetail(item)) {
    reportSqlParseDetailDialogVisible.value = true
  }
}

const statusClass = value => {
  const normalized = String(value || '').toUpperCase()
  if (!normalized) {
    return 'pill'
  }
  if (
    normalized === 'SUCCESS' ||
    normalized === 'SUCCEEDED' ||
    normalized === 'VALID' ||
    normalized === 'AVAILABLE' ||
    normalized === 'CONNECTED' ||
    normalized.endsWith('_SUCCEEDED')
  ) {
    return 'pill pill-success'
  }
  if (normalized.includes('PARTIAL') || normalized.includes('UNAVAILABLE') || normalized.includes('WAITING') || normalized.includes('PENDING')) {
    return 'pill pill-warning'
  }
  return 'pill pill-danger'
}

const resultBannerClass = value => {
  const normalized = String(value || '').toUpperCase()
  if (normalized.includes('FAILED') || normalized.includes('INVALID')) {
    return 'result-banner-danger'
  }
  if (normalized.includes('PARTIAL') || normalized.includes('UNAVAILABLE') || normalized.includes('WAITING')) {
    return 'result-banner-warning'
  }
  return 'result-banner-success'
}

const resultValueClass = item => {
  const key = String(item?.key || '')
  const value = String(item?.value || '').toUpperCase()
  if ((key === 'urgent' && (item.value === true || value === 'TRUE' || value === '是')) || (key === 'priorityLevel' && value === 'P1')) {
    return 'highlight-chip-danger'
  }
  if (value.includes('FAILED') || value.includes('INVALID')) {
    return 'highlight-chip-danger'
  }
  if (value.includes('PARTIAL') || value.includes('UNAVAILABLE')) {
    return 'highlight-chip-warning'
  }
  return ''
}

const reportHistoryPersistenceStatus = item => normalizeQueryValue(item?.historyPersistenceStatus).toUpperCase()

const reportHistoryIsPersisted = item => {
  const historyId = normalizeQueryValue(item?.historyId)
  if (!historyId || item?.historyPersisted === false) {
    return false
  }
  const status = reportHistoryPersistenceStatus(item)
  if (['WRITE_FAILED', 'WRITE_SKIPPED', 'NO_RESPONSE', 'NOT_PERSISTED'].includes(status)) {
    return false
  }
  return item?.historyPersisted === true || !status || ['SAVED', 'UPSERTED'].includes(status)
}

const reportHistoryIdForItem = item => (reportHistoryIsPersisted(item) ? normalizeQueryValue(item?.historyId) : '')

const reportItemDetailKey = item =>
  normalizeQueryValue(item?.historyId) || normalizeQueryValue(item?.parseTaskId) || normalizeQueryValue(item?.itemId)

const buildReportItemFallbackDetail = item => {
  if (!item || typeof item !== 'object') {
    return null
  }
  const issueScenes = issueSceneCodesForItem(item)
  const logicalObjectHits = normalizeArray(item.logicalObjectKeys).map(objectKey => ({
    objectType: 'OBJECT',
    objectKey
  }))
  const issues = issueScenes.map(issueScene => ({
    issueCode: issueScene,
    severity: item.priority,
    summary: item.diagnosticSummary || item.failureReason || issueScene,
    detail: issueLocationText(item),
    suggestedAction: item.failureReason
      ? (t('inline.viewsParseRecordUseParseRecordView.text141'))
      : '',
    failureLine: item.failureLine,
    failureColumn: item.failureColumn,
    failureToken: item.failureToken,
    failureSnippet: item.failureSnippet
  }))
  return {
    historyId: reportHistoryIdForItem(item),
    resultStatus: item.status,
    sqlText: item.sqlText,
    queryContext: {
      parseTaskId: item.parseTaskId,
      resultSummary: {
        overallStatus: item.status,
        degradeReason: item.failureReason,
        accessAvailable:
          String(item.accessServiceStatus || '').toUpperCase() === 'AVAILABLE' &&
          String(item.accessConnectionStatus || '').toUpperCase() === 'CONNECTED'
      }
    },
    structureParseSummary: {
      parseTaskId: item.parseTaskId,
      syntaxStatus: item.structureSyntaxStatus,
      priorityLevel: item.priority,
      issues,
      logicalObjectHits
    },
    accessParseSummary: {
      serviceStatus: item.accessServiceStatus,
      connectionStatus: item.accessConnectionStatus
    },
    executionSummary: {
      resultSummary: {
        overallStatus: item.status,
        degradeReason: item.failureReason
      }
    },
    logicalObjectHits
  }
}

const reportItemLoadedHistoryDetail = item => {
  const detailKey = reportItemDetailKey(item)
  return detailKey ? reportBatchItemDetails.value[detailKey] || null : null
}

const reportItemLocalDetail = item => {
  if (reportHistoryIdForItem(item)) {
    return null
  }
  if (
    hasDisplayValue(item?.sqlText) ||
    hasDisplayValue(item?.parseTaskId) ||
    hasDisplayValue(item?.structureSyntaxStatus) ||
    hasDisplayValue(item?.accessServiceStatus) ||
    issueSceneCodesForItem(item).length > 0
  ) {
    return buildReportItemFallbackDetail(item)
  }
  return null
}

const reportItemParseDetail = item => {
  return reportItemLoadedHistoryDetail(item) || reportItemLocalDetail(item)
}

const reportItemSqlOutput = item =>
  firstValue(reportItemParseDetail(item)?.sqlText, reportItemParseDetail(item)?.sqlTemplateText, item?.sqlText)

const reportItemQueryContext = item => objectValue(reportItemParseDetail(item)?.queryContext)

const reportItemStructureParse = item => {
  const detail = reportItemParseDetail(item)
  const context = reportItemQueryContext(item)
  return firstObject(detail?.structureParseSummary, context.structureParseSummary, context.structureParse)
}

const reportItemAccessParse = item => {
  const detail = reportItemParseDetail(item)
  const context = reportItemQueryContext(item)
  return firstObject(detail?.accessParseSummary, context.accessParseSummary, context.accessParse)
}

const reportItemResultSummary = item => {
  const detail = reportItemParseDetail(item)
  const context = reportItemQueryContext(item)
  return firstObject(context.resultSummary, detail?.executionSummary?.resultSummary, detail?.executionSummary)
}

const reportItemLogicalObjectHits = item => {
  const structureParse = reportItemStructureParse(item)
  const fromStructure = normalizeArray(structureParse.logicalObjectHits)
  if (fromStructure.length) {
    return fromStructure
  }
  const detail = reportItemParseDetail(item)
  const fromDetail = normalizeArray(detail?.logicalObjectHits)
  if (fromDetail.length) {
    return fromDetail
  }
  return normalizeArray(item?.logicalObjectKeys).map(objectKey => ({ objectType: 'OBJECT', objectKey }))
}

const reportItemParseStatus = item => firstValue(
  reportItemResultSummary(item).overallStatus,
  reportItemParseDetail(item)?.resultStatus,
  item?.status,
  item?.structureSyntaxStatus
)

const reportItemParseSummaryCards = item => {
  const detail = reportItemParseDetail(item)
  const resultSummary = reportItemResultSummary(item)
  const structureParse = reportItemStructureParse(item)
  const accessParse = reportItemAccessParse(item)
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text142'), reportItemParseStatus(item)),
    card(t('inline.viewsParseRecordUseParseRecordView.text143'), detail?.historyId),
    card(t('inline.viewsParseRecordUseParseRecordView.text144'), firstValue(structureParse.syntaxStatus, item?.structureSyntaxStatus)),
    card(t('inline.viewsParseRecordUseParseRecordView.text145'), resolveAccessAvailable(resultSummary, accessParse)),
    card(t('inline.viewsParseRecordUseParseRecordView.text146'), firstValue(resultSummary.degradeReason, accessParse.degradeReason, item?.failureReason))
  ].filter(entry => hasDisplayValue(entry.value))
}

const reportItemParseStatisticCards = item => {
  const structureParse = reportItemStructureParse(item)
  const feature = objectValue(structureParse.featureSummary)
  return [
    card(t('inline.viewsParseRecordUseParseRecordView.text147'), normalizeArray(structureParse.issues).length || normalizeArray(item?.issueScenes).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text148'), normalizeArray(structureParse.riskChecklist).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text149'), reportItemLogicalObjectHits(item).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text150'), normalizeArray(structureParse.riskTags).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text151'), normalizeArray(structureParse.rewriteCandidates).length),
    card(t('inline.viewsParseRecordUseParseRecordView.text152'), feature.tableCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text153'), feature.joinCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text154'), feature.predicateCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text155'), feature.orderByExpressionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text156'), feature.duplicateOrderByKeyCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text157'), feature.duplicateGroupByKeyCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text158'), feature.stringProjectionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text159'), feature.repeatedSubqueryCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text160'), feature.windowFunctionCount),
    card(t('inline.viewsParseRecordUseParseRecordView.text161'), structureParse.priorityLevel),
    card(t('inline.viewsParseRecordUseParseRecordView.text162'), structureParse.priorityScore),
    card(t('inline.viewsParseRecordUseParseRecordView.text163'), booleanLabel(structureParse.important)),
    card(t('inline.viewsParseRecordUseParseRecordView.text164'), booleanLabel(structureParse.urgent))
  ].filter(entry => hasDisplayValue(entry.value))
}

const reportItemStructureHighlights = item => {
  const structureParse = reportItemStructureParse(item)
  return [
    { key: 'parseTaskId', label: t('inline.viewsParseRecordUseParseRecordView.text165'), value: item?.parseTaskId },
    { key: 'sqlFingerprint', label: t('inline.viewsParseRecordUseParseRecordView.text166'), value: firstValue(structureParse.sqlFingerprint, reportItemParseDetail(item)?.sqlFingerprint) },
    { key: 'syntaxStatus', label: t('inline.viewsParseRecordUseParseRecordView.text167'), value: firstValue(structureParse.syntaxStatus, item?.structureSyntaxStatus) },
    { key: 'complexityLevel', label: t('inline.viewsParseRecordUseParseRecordView.text168'), value: structureParse.complexityLevel },
    { key: 'sqlType', label: t('inline.viewsParseRecordUseParseRecordView.text169'), value: structureParse.sqlType },
    { key: 'priorityLevel', label: t('inline.viewsParseRecordUseParseRecordView.text170'), value: structureParse.priorityLevel },
    { key: 'priorityScore', label: t('inline.viewsParseRecordUseParseRecordView.text171'), value: structureParse.priorityScore },
    { key: 'important', label: t('inline.viewsParseRecordUseParseRecordView.text172'), value: booleanLabel(structureParse.important) },
    { key: 'urgent', label: t('inline.viewsParseRecordUseParseRecordView.text173'), value: booleanLabel(structureParse.urgent) }
  ].filter(entry => hasDisplayValue(entry.value))
}

const reportItemAccessHighlights = item => {
  const accessParse = reportItemAccessParse(item)
  return [
    { key: 'serviceStatus', label: t('inline.viewsParseRecordUseParseRecordView.text174'), value: firstValue(accessParse.serviceStatus, item?.accessServiceStatus) },
    { key: 'connectionStatus', label: t('inline.viewsParseRecordUseParseRecordView.text175'), value: firstValue(accessParse.connectionStatus, item?.accessConnectionStatus) },
    { key: 'objectResolutionStatus', label: t('inline.viewsParseRecordUseParseRecordView.text176'), value: accessParse.objectResolutionStatus },
    { key: 'partitionStatus', label: t('inline.viewsParseRecordUseParseRecordView.text177'), value: accessParse.partitionStatus },
    { key: 'dataFreshnessStatus', label: t('inline.viewsParseRecordUseParseRecordView.text178'), value: accessParse.dataFreshnessStatus },
    { key: 'slaStatus', label: 'SLA', value: accessParse.slaStatus },
    { key: 'compatibilityStatus', label: t('inline.viewsParseRecordUseParseRecordView.text179'), value: accessParse.compatibilityStatus }
  ].filter(entry => hasDisplayValue(entry.value))
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

const localizedDisplayText = value => {
  const text = displayValue(value)
  if (text === '-') {
    return text
  }
  const parts = text
    .split(/\r?\n+/)
    .map(part => part.trim())
    .filter(Boolean)
  if (parts.length < 2) {
    return text
  }
  const hasChineseText = part => /[\u3400-\u9fff]/.test(part)
  const chineseParts = parts.filter(hasChineseText)
  const englishParts = parts.filter(part => !hasChineseText(part) && /[A-Za-z]/.test(part))
  if (!chineseParts.length || !englishParts.length) {
    return text
  }
  return (isChinese.value ? chineseParts : englishParts).join(' ')
}

const issueSceneValues = value => normalizeArray(value).filter(scene => hasDisplayValue(scene))

const issueSceneHelp = scene => issueSceneHelpText(scene, isChinese.value)

const issueSceneListHelp = value => issueSceneValues(value).map(issueSceneHelp).filter(Boolean).join(' / ')

const riskDisplayText = (risk, field) => localizedDisplayText(sharedRiskDisplayText(risk, field, isChinese.value))

const displayDetailValue = value => {
  if (value && typeof value === 'object') {
    return formatJson(value)
  }
  return localizedDisplayText(value)
}

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const issueLocationItems = item => {
  const locations = normalizeArray(item?.issueLocations)
  if (locations.length) {
    return locations.filter(location => hasDisplayValue(location?.issueScene))
  }
  if (hasDisplayValue(item?.failureSnippet) || hasDisplayValue(item?.failureToken)) {
    return [{
      issueScene: 'SQL_SYNTAX_INVALID',
      locationSnippet: item.failureSnippet,
      failureToken: item.failureToken,
      failureLine: item.failureLine,
      failureColumn: item.failureColumn
    }]
  }
  return []
}

const issueSceneCodesForItem = item => {
  const explicitScenes = issueSceneValues(item?.issueScenes)
  if (explicitScenes.length) {
    return explicitScenes
  }
  return issueLocationItems(item).map(location => location.issueScene).filter(scene => hasDisplayValue(scene))
}

const issueLocationText = item => {
  const locations = issueLocationItems(item)
  if (!locations.length) {
    return t('inline.viewsParseRecordUseParseRecordView.text180')
  }
  return locations
    .map(location => {
      const parts = [location.issueScene]
      if (hasDisplayValue(location.failureLine) && hasDisplayValue(location.failureColumn)) {
        parts.push(`line ${location.failureLine}, col ${location.failureColumn}`)
      }
      if (hasDisplayValue(location.failureToken)) {
        parts.push(`token ${location.failureToken}`)
      }
      if (hasDisplayValue(location.locationSnippet)) {
        parts.push(location.locationSnippet)
      }
      return parts.filter(Boolean).join(' · ')
    })
    .join(' / ')
}

const issueLocationTextForScene = (item, issueScene) => {
  const normalizedIssueScene = normalizeQueryValue(issueScene)
  const row = objectValue(item)
  const locations = issueLocationItems(row)
  const scopedLocations = locations.filter(location =>
    normalizeQueryValue(location.issueScene) === normalizedIssueScene
  )
  if (scopedLocations.length) {
    return issueLocationText({ ...row, issueLocations: scopedLocations })
  }
  const rowScenes = issueSceneCodesForItem(row).map(scene => normalizeQueryValue(scene))
  const rowIssueCount = Number(row.issueCount || 0)
  if (normalizedIssueScene && (rowScenes.includes(normalizedIssueScene) || rowIssueCount > 0)) {
    return `${normalizedIssueScene} · ${t('inline.viewsParseRecordUseParseRecordView.text181')}`
  }
  return issueLocationText({ ...row, issueLocations: [] })
}

const booleanLabel = value => {
  if (typeof value !== 'boolean') {
    return ''
  }
  if (isChinese.value) {
    return value ? '是' : '否'
  }
  return value ? 'true' : 'false'
}

const objectValue = value => {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value
  }
  return {}
}

const firstObject = (...values) => values.map(objectValue).find(value => Object.keys(value).length > 0) || {}

const firstValue = (...values) => {
  const match = values.find(value => hasDisplayValue(value))
  return match === undefined ? '' : match
}

const compactObject = value =>
  Object.fromEntries(Object.entries(value).filter(([, entryValue]) => hasDisplayValue(entryValue)))

const referenceValue = (record, keys) => {
  if (!record || typeof record !== 'object') {
    return ''
  }
  return firstValue(...keys.map(key => record[key]))
}

const resolveAccessAvailable = (summary, accessParse) => {
  if (typeof summary?.accessAvailable === 'boolean') {
    return booleanLabel(summary.accessAvailable)
  }
  const serviceStatus = String(accessParse?.serviceStatus || '').toUpperCase()
  const connectionStatus = String(accessParse?.connectionStatus || '').toUpperCase()
  if (!serviceStatus && !connectionStatus) {
    return ''
  }
  return booleanLabel(serviceStatus === 'AVAILABLE' && connectionStatus === 'CONNECTED')
}

const rate = (count, total) => {
  if (!total) {
    return 0
  }
  return Number(count || 0) / Number(total)
}

const formatPercent = value => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${(Number(value) * 100).toFixed(1)}%`
}

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

const hasQueryValue = key => hasDisplayValue(route.query[key])

const resolveHistoryWorkbenchTabFromRoute = () => {
  if (hasQueryValue('historyId') || hasQueryValue('parseHistoryId')) {
    return 'sqlHistory'
  }
  const queryTab = normalizeQueryValue(
    route.query.historyWorkbenchTab || route.query.historyTab || route.query.tab
  )
  if (HISTORY_WORKBENCH_TABS.has(queryTab)) {
    return queryTab
  }
  const metaTab = normalizeQueryValue(route.meta.historyWorkbenchTab)
  return HISTORY_WORKBENCH_TABS.has(metaTab) ? metaTab : 'sqlHistory'
}

const syncHistoryWorkbenchTabFromRoute = () => {
  activeHistoryWorkbenchTab.value = resolveHistoryWorkbenchTabFromRoute()
  const queryBatchTab = normalizeQueryValue(route.query.batchHistoryTab || route.query.batchTab)
  if (BATCH_HISTORY_TABS.has(queryBatchTab)) {
    batchHistoryTab.value = queryBatchTab
  }
}

const syncLookupFieldsFromRoute = () => {
  form.tenantId = routeTenantId.value
  form.traceId = hasQueryValue('traceId') ? String(route.query.traceId) : ''
  form.taskId = hasQueryValue('taskId') ? String(route.query.taskId) : ''
  form.reportId = hasQueryValue('reportId') ? String(route.query.reportId) : ''
}

const openRouteDeepLink = async () => {
  if (hasQueryValue('historyId') || hasQueryValue('parseHistoryId')) {
    await openHistoryDetail(String(route.query.historyId || route.query.parseHistoryId))
    return
  }
  if (hasLookupCriteria.value) {
    await runIndexedLookup()
  }
}

onMounted(async () => {
  syncHistoryWorkbenchTabFromRoute()
  syncLookupFieldsFromRoute()
  await refreshWorkbench()
  await openRouteDeepLink()
})

watch(
  () => route.fullPath,
  async () => {
    syncHistoryWorkbenchTabFromRoute()
    syncLookupFieldsFromRoute()
    await openRouteDeepLink()
  }
)

watch(reportBatchDetailDrawerVisible, visible => {
  if (!visible) {
    resetReportBatchIssueSceneDetail()
  }
})

  return {
    activeDialogTab,
    activeHistoryWorkbenchTab,
    activeReportBatchDetailTab,
    activeReportBatchStatisticsTab,
    applyPaginationResult,
    applyReportBatchIssueSceneFilter,
    applyReportBatchSqlFilter,
    auditEvents,
    BATCH_HISTORY_TABS,
    batchHistoryErrorMessage,
    batchHistoryTab,
    booleanLabel,
    buildReportItemFallbackDetail,
    card,
    classificationSummary,
    clearReportBatchIssueSceneLogicalObjectFilter,
    clearReportBatchIssueSceneReportFilter,
    clearFilters,
    datasourceOptions,
    datasourceOptionsLoadFailed,
    DEFAULT_HISTORY_CONTEXT_TENANT_ID,
    detailDialogVisible,
    detailSummaryCards,
    displayDetailValue,
    displayValue,
    errorMessage,
    evidenceDrawerVisible,
    exportDialogVisible,
    exportForm,
    exportResult,
    firstObject,
    firstValue,
    form,
    formatJson,
    formatPercent,
    formatTimestamp,
    handleHistoryPageChange,
    handleHistoryPageSizeChange,
    handleParseBatchHistoryPageChange,
    handleParseBatchHistoryPageSizeChange,
    handleReportBatchHistoryPageChange,
    handleReportBatchHistoryPageSizeChange,
    handleReportBatchIssueScenePageChange,
    handleReportBatchIssueScenePageSizeChange,
    handleReportBatchIssueSceneReportPageChange,
    handleReportBatchIssueSceneReportPageSizeChange,
    handleReportBatchIssueSceneLogicalObjectPageChange,
    handleReportBatchIssueSceneLogicalObjectPageSizeChange,
    handleReportBatchIssueStatisticsPageChange,
    handleReportBatchIssueStatisticsPageSizeChange,
    handleReportBatchSqlPageChange,
    handleReportBatchSqlPageSizeChange,
    hasDisplayValue,
    hasSqlHistoryLink,
    hasLookupCriteria,
    hasQueryValue,
    HISTORY_WORKBENCH_TABS,
    historyAccessHighlights,
    historyAccessParse,
    historyLogicalObjectHits,
    historyPagination,
    historyParseStatisticCards,
    historyParseStatus,
    historyParseSummaryCards,
    historyParseTaskId,
    historyQueryContext,
    historyResultSummary,
    historyStructureFeatureHighlights,
    historyStructureHighlights,
    historyStructureIntentLabels,
    historyStructureIssues,
    historyStructureParse,
    historyStructureResourceHighlights,
    historyStructureRiskChecklist,
    historyWorkbenchKicker,
    historyWorkbenchTitle,
    isBatchHistoryWorkbench,
    isChinese,
    isNonEmpty,
    issueLocationItems,
    issueLocationText,
    issueLocationTextForScene,
    issueSceneCodesForItem,
    issueSceneHelp,
    issueSceneListHelp,
    issueSceneValues,
    LIST_PAGE_SIZE_OPTIONS,
    loadBatchHistories,
    loadDatasourceOptions,
    loading,
    loadPage,
    loadReportBatchIssueSceneDetail,
    loadReportBatchItemDetail,
    loadReportBatchItemDetails,
    loadReportBatchWithStatistics,
    localizedDisplayText,
    logicalObjectHits,
    normalizeArray,
    normalizePagedList,
    normalizeQueryValue,
    objectValue,
    openExportDialog,
    openHistoryDetail,
    openParseBatchCenter,
    openRecommendationCenterFromDetail,
    openReportBatchCenter,
    openReportBatchDetail,
    openReportBatchIssueSceneDetail,
    openReportSqlParseDetail,
    openRouteDeepLink,
    openSqlHistoryFromDetail,
    page,
    pageSummaryCards,
    parseBatchHistoryPagination,
    parseBatchHistoryRows,
    parseBatchHistorySummary,
    queryDateRange,
    rate,
    recommendationRefRows,
    referenceGroups,
    refreshSelectedReportSqlPage,
    refreshWorkbench,
    REPORT_STATISTIC_PAGE_SIZE_OPTIONS,
    REPORT_SQL_PAGE_SIZE_OPTIONS,
    reportBatchDetailCards,
    reportBatchDetailDrawerVisible,
    reportBatchHistoryPagination,
    reportBatchHistoryRows,
    reportBatchHistorySummary,
    reportBatchIssueSceneDetailCards,
    reportBatchIssueSceneDetailDialogTitle,
    reportBatchIssueSceneDetailDialogVisible,
    reportBatchIssueScenePagination,
    reportBatchIssueSceneReportPagination,
    reportBatchIssueSceneLogicalObjectPagination,
    reportBatchIssueStatisticsPagination,
    reportBatchIssueStatistics,
    reportBatchIssueStatisticsPage,
    reportBatchItemDetailErrorMessage,
    reportBatchItemDetails,
    reportBatchLogicalObjectStatistics,
    reportBatchParseDetailSummary,
    reportBatchSqlPagination,
    reportHistoryIdForItem,
    reportHistoryIsPersisted,
    reportHistoryPersistenceStatus,
    reportItemAccessHighlights,
    reportItemAccessParse,
    reportItemDetailKey,
    reportItemLoadedHistoryDetail,
    reportItemLocalDetail,
    reportItemLogicalObjectHits,
    reportItemParseDetail,
    reportItemParseStatisticCards,
    reportItemParseStatus,
    reportItemParseSummaryCards,
    reportItemQueryContext,
    reportItemResultSummary,
    reportItemSqlOutput,
    reportItemStructureHighlights,
    reportItemStructureParse,
    reportSqlParseDetailDialogVisible,
    requestTenantId,
    resolveAccessAvailable,
    resolveHistoryWorkbenchTabFromRoute,
    resultBannerClass,
    resultValueClass,
    riskDisplayText,
    route,
    router,
    routeTenantId,
    rows,
    runExport,
    runIndexedLookup,
    searchWorkbench,
    selectReportBatchIssueSceneLogicalObject,
    selectReportBatchIssueSceneReport,
    selectedHistoryDetail,
    selectedHistoryId,
    primaryRecommendationRef,
    selectedReportBackendReportStatistics,
    selectedReportBackendSqlStatistics,
    selectedReportBatchDetail,
    selectedReportGroups,
    selectedReportImportanceStatistics,
    selectedReportIssueSceneDetail,
    selectedReportIssueSceneReportDetails,
    selectedReportIssueSceneReportDetailsPage,
    selectedReportIssueSceneReportDetailTotalCount,
    selectedReportIssueSceneLogicalObjectDetails,
    selectedReportIssueSceneLogicalObjectDetailsPage,
    selectedReportIssueSceneLogicalObjectDetailTotalCount,
    selectedReportIssueSceneSqlStatisticTotalCount,
    selectedReportIssueSceneStatistics,
    selectedReportItems,
    selectedReportLogicalObjectStatistics,
    selectedReportParseStatistics,
    selectedReportParseStatisticsOverview,
    selectedReportPriorityMatrix,
    selectedReportSqlDetailItem,
    signalGroups,
    sortModeLabel,
    sqlStateHighlights,
    sqlVariants,
    statusClass,
    issueSceneLogicalObjectRowClassName,
    issueSceneReportRowClassName,
    submittedAtRange,
    syncDateRangeFields,
    syncHistoryWorkbenchTabFromRoute,
    syncLookupFieldsFromRoute,
    tenantOptions,
    traceSummaryCards,
    withCurrentOption
  }
}
