import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
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

const DEFAULT_HISTORY_CONTEXT_TENANT_ID = 'tenant-a'
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
const REPORT_SQL_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]
const LIST_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]
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
    return isChinese.value ? '解析历史查询' : 'Parse history search'
  }
  return isChinese.value ? 'SQL 解析记录查询' : 'SQL parse records'
})
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
const card = (label, value, key = '') => ({ label, value, key })
const parseBatchHistorySummary = computed(() => {
  const totalRecords = parseBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.totalRecords || 0), 0)
  const failedBatches = parseBatchHistoryRows.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(isChinese.value ? '当前页批次' : 'Current page', parseBatchHistoryRows.value.length),
    card(isChinese.value ? '批次总数' : 'Batches', parseBatchHistoryPagination.totalCount),
    card(isChinese.value ? '总页数' : 'Total pages', parseBatchHistoryPagination.pageCount),
    card(isChinese.value ? '总记录数' : 'Records', totalRecords),
    card(isChinese.value ? '失败批次' : 'Failed batches', failedBatches)
  ]
})
const reportBatchHistorySummary = computed(() => {
  const totalReports = reportBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.totalReports || 0), 0)
  const resolvedReports = reportBatchHistoryRows.value.reduce((sum, item) => sum + Number(item.resolvedReports || 0), 0)
  const failedBatches = reportBatchHistoryRows.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(isChinese.value ? '当前页批次' : 'Current page', reportBatchHistoryRows.value.length),
    card(isChinese.value ? '批次总数' : 'Batches', reportBatchHistoryPagination.totalCount),
    card(isChinese.value ? '总页数' : 'Total pages', reportBatchHistoryPagination.pageCount),
    card(isChinese.value ? '报表总数' : 'Reports', totalReports),
    card(isChinese.value ? '已解析' : 'Resolved', resolvedReports),
    card(isChinese.value ? '失败批次' : 'Failed batches', failedBatches)
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
    card(isChinese.value ? '批次状态' : 'Batch status', detail.status),
    card(isChinese.value ? '文件类型' : 'File type', detail.fileType),
    card(isChinese.value ? '报表总数' : 'Total reports', detail.totalReports),
    card(isChinese.value ? 'SQL 明细' : 'SQL rows', overview.totalSqlCount ?? detail.totalSqls ?? total),
    card(isChinese.value ? '问题 SQL' : 'Issue SQL', overview.issueSqlCount),
    card(isChinese.value ? '重要 SQL' : 'Important SQL', overview.importantSqlCount),
    card(isChinese.value ? '紧急 SQL' : 'Urgent SQL', overview.urgentSqlCount),
    card(isChinese.value ? '可合并报表' : 'Merge candidates', selectedReportParseStatistics.value.mergeCandidateReportCount),
    card(isChinese.value ? '已解析 SQL' : 'Resolved SQL', detail.resolvedSqls ?? detail.resolvedReports),
    card(isChinese.value ? '失败 SQL' : 'Failed SQL', detail.failedSqls ?? detail.failedReports),
    card(isChinese.value ? '结构成功率' : 'Structure rate', formatPercent(rate(structureValid, total))),
    card(isChinese.value ? 'Access 连通率' : 'Access connected', formatPercent(rate(accessConnected, total)))
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
const reportBatchIssueSceneDetailCards = computed(() => {
  const detail = objectValue(selectedReportIssueSceneDetail.value)
  if (!detail.issueScene) {
    return []
  }
  return [
    card(isChinese.value ? '问题场景' : 'Issue scene', detail.issueScene, 'issueScene'),
    card(isChinese.value ? '影响 SQL' : 'Affected SQL', detail.affectedSqlCount),
    card(isChinese.value ? '问题数' : 'Issues', detail.affectedIssueCount),
    card(isChinese.value ? '报表数' : 'Reports', detail.reportCount),
    card(isChinese.value ? '逻辑对象' : 'Logical objects', detail.logicalObjectCount),
    card(isChinese.value ? '优先级' : 'Priority', detail.priorityLevel),
    card(isChinese.value ? '严重度' : 'Severity', detail.severity)
  ].filter(item => hasDisplayValue(item.value))
})
const reportBatchIssueSceneDetailDialogTitle = computed(() => {
  const detail = objectValue(selectedReportIssueSceneDetail.value)
  const title = isChinese.value ? '问题场景明细' : 'Issue scene detail'
  const scene = detail.issueScene || '-'
  const filters = [
    reportBatchIssueScenePagination.reportCode
      ? `${isChinese.value ? '报表编码' : 'Report code'}: ${reportBatchIssueScenePagination.reportCode}`
      : '',
    reportBatchIssueScenePagination.logicalObjectKey
      ? `${isChinese.value ? '逻辑对象' : 'Logical object'}: ${reportBatchIssueScenePagination.logicalObjectKey}`
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
    card(isChinese.value ? '可追溯 SQL' : 'Traceable SQL', total),
    card(isChinese.value ? '已加载详情' : 'Loaded details', loaded),
    card(isChinese.value ? '批次内证据' : 'Batch evidence', batchEvidence),
    card(isChinese.value ? '缺失详情' : 'Missing details', Math.max(total - loaded, 0))
  ]
})
const pageSummaryCards = computed(() => [
  { label: isChinese.value ? '当前页记录' : 'Current page', value: rows.value.length },
  { label: isChinese.value ? '总条数' : 'Total records', value: historyPagination.totalCount },
  { label: isChinese.value ? '总页数' : 'Total pages', value: historyPagination.pageCount },
  { label: isChinese.value ? '成功' : 'Success', value: classificationSummary.value.SUCCESS || classificationSummary.value.succeeded || 0 },
  { label: isChinese.value ? '异常/部分成功' : 'Non-success', value: classificationSummary.value.PARTIAL || classificationSummary.value.FAILED || classificationSummary.value.nonSuccess || 0 },
  { label: isChinese.value ? '接入渠道' : 'Access channels', value: Object.keys(classificationSummary.value.accessChannelCounts || {}).length }
])
const detailSummaryCards = computed(() => {
  if (!selectedHistoryDetail.value) {
    return []
  }
  return [
    { label: 'Parse History ID', value: selectedHistoryDetail.value.parseHistoryId || selectedHistoryDetail.value.historyId },
    { label: 'Trace ID', value: selectedHistoryDetail.value.traceId },
    { label: isChinese.value ? '报表编码' : 'Report code', value: selectedHistoryDetail.value.reportCode },
    { label: isChinese.value ? '来源类型' : 'Source type', value: selectedHistoryDetail.value.sourceType },
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
    {
      key: 'sqlText',
      label: t('sqlHistory.sql.originalSql'),
      value: selectedHistoryDetail.value?.sqlText,
      autoFormat: false
    },
    { key: 'sqlTemplateText', label: isChinese.value ? '模板 SQL' : 'Template SQL', value: selectedHistoryDetail.value?.sqlTemplateText },
    { key: 'boundSqlText', label: isChinese.value ? '绑定 SQL' : 'Bound SQL', value: selectedHistoryDetail.value?.boundSqlText }
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
    card(isChinese.value ? '综合状态' : 'Overall status', historyParseStatus.value),
    card(isChinese.value ? 'Parse Task' : 'Parse task', historyParseTaskId.value),
    card(isChinese.value ? '语法状态' : 'Syntax status', historyStructureParse.value.syntaxStatus),
    card(isChinese.value ? 'Access 可用' : 'Access available', resolveAccessAvailable(historyResultSummary.value, historyAccessParse.value)),
    card(isChinese.value ? '降级原因' : 'Degrade reason', firstValue(historyResultSummary.value.degradeReason, historyAccessParse.value.degradeReason)),
    card(isChinese.value ? '历史写入' : 'History write', selectedHistoryDetail.value?.historyId ? (isChinese.value ? '已落库' : 'Saved') : ''),
    card('History ID', selectedHistoryDetail.value?.historyId)
  ].filter(item => hasDisplayValue(item.value))
)
const historyParseStatisticCards = computed(() => {
  const feature = objectValue(historyStructureParse.value.featureSummary)
  return [
    card(isChinese.value ? '问题数' : 'Issues', historyStructureIssues.value.length),
    card(isChinese.value ? '风险项' : 'Risks', historyStructureRiskChecklist.value.length),
    card(isChinese.value ? '逻辑对象' : 'Logical objects', historyLogicalObjectHits.value.length),
    card(isChinese.value ? '风险标签' : 'Risk tags', normalizeArray(historyStructureParse.value.riskTags).length),
    card(isChinese.value ? '改写候选' : 'Rewrite candidates', normalizeArray(historyStructureParse.value.rewriteCandidates).length),
    card(isChinese.value ? '表数量' : 'Tables', feature.tableCount),
    card(isChinese.value ? 'Join 数' : 'Joins', feature.joinCount),
    card(isChinese.value ? '谓词数' : 'Predicates', feature.predicateCount),
    card(isChinese.value ? '排序字段' : 'Order keys', feature.orderByExpressionCount),
    card(isChinese.value ? '重复排序 key' : 'Duplicate order keys', feature.duplicateOrderByKeyCount),
    card(isChinese.value ? '重复分组 key' : 'Duplicate group keys', feature.duplicateGroupByKeyCount),
    card(isChinese.value ? '字符串结果' : 'String result signals', feature.stringProjectionCount),
    card(isChinese.value ? '重复子查询' : 'Repeated subqueries', feature.repeatedSubqueryCount),
    card(isChinese.value ? '窗口函数' : 'Windows', feature.windowFunctionCount),
    card(isChinese.value ? '重复表达式' : 'Repeated expressions', feature.repeatedExpressionCount),
    card(isChinese.value ? '重要' : 'Important', booleanLabel(historyStructureParse.value.important)),
    card(isChinese.value ? '紧急' : 'Urgent', booleanLabel(historyStructureParse.value.urgent))
  ].filter(item => hasDisplayValue(item.value))
})
const historyStructureHighlights = computed(() =>
  [
    { key: 'parseTaskId', label: isChinese.value ? 'Parse Task' : 'Parse task', value: historyParseTaskId.value },
    { key: 'sqlFingerprint', label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint', value: firstValue(historyStructureParse.value.sqlFingerprint, selectedHistoryDetail.value?.sqlFingerprint) },
    { key: 'syntaxStatus', label: isChinese.value ? '语法状态' : 'Syntax status', value: historyStructureParse.value.syntaxStatus },
    { key: 'complexityLevel', label: isChinese.value ? '复杂度' : 'Complexity', value: historyStructureParse.value.complexityLevel },
    { key: 'sqlType', label: isChinese.value ? 'SQL 类型' : 'SQL type', value: historyStructureParse.value.sqlType },
    { key: 'priorityLevel', label: isChinese.value ? '优先级' : 'Priority', value: historyStructureParse.value.priorityLevel },
    { key: 'priorityScore', label: isChinese.value ? '评分' : 'Score', value: historyStructureParse.value.priorityScore },
    { key: 'important', label: isChinese.value ? '重要' : 'Important', value: booleanLabel(historyStructureParse.value.important) },
    { key: 'urgent', label: isChinese.value ? '紧急' : 'Urgent', value: booleanLabel(historyStructureParse.value.urgent) }
  ].filter(item => hasDisplayValue(item.value))
)
const historyStructureFeatureHighlights = computed(() => {
  const feature = objectValue(historyStructureParse.value.featureSummary)
  return [
    { key: 'parserEngine', label: isChinese.value ? 'Parser' : 'Parser', value: feature.parserEngine },
    { key: 'scanMode', label: isChinese.value ? '扫描模式' : 'Scan mode', value: feature.scanMode },
    { key: 'joinType', label: isChinese.value ? 'Join 类型' : 'Join type', value: feature.joinType },
    { key: 'computeDensity', label: isChinese.value ? '计算密度' : 'Compute density', value: feature.computeDensity },
    { key: 'resourceType', label: isChinese.value ? '资源类型' : 'Resource type', value: feature.resourceType },
    { key: 'slaLevel', label: isChinese.value ? 'SLA 等级' : 'SLA level', value: feature.slaLevel },
    { key: 'tableCount', label: isChinese.value ? '表数量' : 'Tables', value: feature.tableCount },
    { key: 'joinCount', label: isChinese.value ? 'Join 数' : 'Joins', value: feature.joinCount },
    { key: 'predicateCount', label: isChinese.value ? '谓词数' : 'Predicates', value: feature.predicateCount },
    { key: 'orderByExpressionCount', label: isChinese.value ? '排序字段' : 'Order keys', value: feature.orderByExpressionCount },
    { key: 'duplicateOrderByKeyCount', label: isChinese.value ? '重复排序 key' : 'Duplicate order keys', value: feature.duplicateOrderByKeyCount },
    { key: 'duplicateGroupByKeyCount', label: isChinese.value ? '重复分组 key' : 'Duplicate group keys', value: feature.duplicateGroupByKeyCount },
    { key: 'groupByWithoutAggregate', label: isChinese.value ? '分组无聚合' : 'Group without aggregate', value: booleanLabel(feature.groupByWithoutAggregate) },
    { key: 'aggregateFunctionCount', label: isChinese.value ? '聚合函数' : 'Aggregates', value: feature.aggregateFunctionCount },
    { key: 'stringProjectionCount', label: isChinese.value ? '字符串投影' : 'String projections', value: feature.stringProjectionCount },
    { key: 'stringConcatenationCount', label: isChinese.value ? '字符串拼接' : 'String concatenations', value: feature.stringConcatenationCount },
    { key: 'largeStringAggregateCount', label: isChinese.value ? '字符串聚合' : 'String aggregates', value: feature.largeStringAggregateCount },
    { key: 'repeatedSubqueryCount', label: isChinese.value ? '重复子查询' : 'Repeated subqueries', value: feature.repeatedSubqueryCount },
    { key: 'windowFunctionCount', label: isChinese.value ? '窗口函数' : 'Windows', value: feature.windowFunctionCount },
    { key: 'repeatedExpressionCount', label: isChinese.value ? '重复表达式' : 'Repeated expressions', value: feature.repeatedExpressionCount }
  ].filter(item => hasDisplayValue(item.value))
})
const historyStructureResourceHighlights = computed(() => {
  const estimate = objectValue(historyStructureParse.value.estimatedResourceCost)
  return [
    { key: 'overall', label: isChinese.value ? '总体' : 'Overall', value: estimate.overall },
    { key: 'cpu', label: 'CPU', value: estimate.cpu },
    { key: 'io', label: 'IO', value: estimate.io },
    { key: 'memory', label: isChinese.value ? '内存' : 'Memory', value: estimate.memory },
    { key: 'network', label: isChinese.value ? '网络' : 'Network', value: estimate.network },
    { key: 'resultSize', label: isChinese.value ? '结果集' : 'Result size', value: estimate.resultSize }
  ].filter(item => hasDisplayValue(item.value))
})
const historyAccessHighlights = computed(() =>
  [
    { key: 'serviceStatus', label: isChinese.value ? '服务状态' : 'Service status', value: historyAccessParse.value.serviceStatus },
    { key: 'connectionStatus', label: isChinese.value ? '连接状态' : 'Connection status', value: historyAccessParse.value.connectionStatus },
    { key: 'objectResolutionStatus', label: isChinese.value ? '对象解析' : 'Object resolution', value: historyAccessParse.value.objectResolutionStatus },
    { key: 'partitionStatus', label: isChinese.value ? '分区状态' : 'Partition status', value: historyAccessParse.value.partitionStatus },
    { key: 'dataFreshnessStatus', label: isChinese.value ? '新鲜度' : 'Freshness', value: historyAccessParse.value.dataFreshnessStatus },
    { key: 'slaStatus', label: 'SLA', value: historyAccessParse.value.slaStatus },
    { key: 'compatibilityStatus', label: isChinese.value ? '兼容性' : 'Compatibility', value: historyAccessParse.value.compatibilityStatus }
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
    errorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项。'
      : 'Enter at least one of traceId, taskId, or reportId.'
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
      errorMessage.value = isChinese.value ? '没有命中解析记录。' : 'No parse-history record matched the lookup criteria.'
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
        reportCode: reportBatchIssueScenePagination.reportCode,
        logicalObjectKey: reportBatchIssueScenePagination.logicalObjectKey,
        requestPrefix: 'frontend-parse-record-report-issue-scene-detail'
      }
    )
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
  await loadReportBatchIssueSceneDetail(item?.issueScene)
  if (selectedReportIssueSceneDetail.value) {
    activeReportBatchDetailTab.value = 'statistics'
    activeReportBatchStatisticsTab.value = 'issueScene'
  }
}

const applyReportBatchIssueSceneFilter = async () => {
  reportBatchIssueScenePagination.pageNumber = 1
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

const handleReportBatchIssueScenePageChange = async pageNumber => {
  reportBatchIssueScenePagination.pageNumber = pageNumber
  await loadReportBatchIssueSceneDetail(selectedReportIssueSceneDetail.value?.issueScene)
}

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
      ? (isChinese.value ? '查看失败定位并修正 SQL 后重新解析。' : 'Review the failure position, fix the SQL, and parse again.')
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
    card(isChinese.value ? '综合状态' : 'Overall status', reportItemParseStatus(item)),
    card(isChinese.value ? '解析历史' : 'Parse history', detail?.historyId),
    card(isChinese.value ? '语法状态' : 'Syntax status', firstValue(structureParse.syntaxStatus, item?.structureSyntaxStatus)),
    card(isChinese.value ? 'Access 可用' : 'Access available', resolveAccessAvailable(resultSummary, accessParse)),
    card(isChinese.value ? '降级原因' : 'Degrade reason', firstValue(resultSummary.degradeReason, accessParse.degradeReason, item?.failureReason))
  ].filter(entry => hasDisplayValue(entry.value))
}

const reportItemParseStatisticCards = item => {
  const structureParse = reportItemStructureParse(item)
  const feature = objectValue(structureParse.featureSummary)
  return [
    card(isChinese.value ? '问题数' : 'Issues', normalizeArray(structureParse.issues).length || normalizeArray(item?.issueScenes).length),
    card(isChinese.value ? '风险项' : 'Risks', normalizeArray(structureParse.riskChecklist).length),
    card(isChinese.value ? '逻辑对象' : 'Logical objects', reportItemLogicalObjectHits(item).length),
    card(isChinese.value ? '风险标签' : 'Risk tags', normalizeArray(structureParse.riskTags).length),
    card(isChinese.value ? '改写候选' : 'Rewrite candidates', normalizeArray(structureParse.rewriteCandidates).length),
    card(isChinese.value ? '表数量' : 'Tables', feature.tableCount),
    card(isChinese.value ? 'Join 数' : 'Joins', feature.joinCount),
    card(isChinese.value ? '谓词数' : 'Predicates', feature.predicateCount),
    card(isChinese.value ? '排序字段' : 'Order keys', feature.orderByExpressionCount),
    card(isChinese.value ? '重复排序 key' : 'Duplicate order keys', feature.duplicateOrderByKeyCount),
    card(isChinese.value ? '重复分组 key' : 'Duplicate group keys', feature.duplicateGroupByKeyCount),
    card(isChinese.value ? '字符串结果' : 'String result signals', feature.stringProjectionCount),
    card(isChinese.value ? '重复子查询' : 'Repeated subqueries', feature.repeatedSubqueryCount),
    card(isChinese.value ? '窗口函数' : 'Windows', feature.windowFunctionCount),
    card(isChinese.value ? '优先级' : 'Priority', structureParse.priorityLevel),
    card(isChinese.value ? '评分' : 'Score', structureParse.priorityScore),
    card(isChinese.value ? '重要' : 'Important', booleanLabel(structureParse.important)),
    card(isChinese.value ? '紧急' : 'Urgent', booleanLabel(structureParse.urgent))
  ].filter(entry => hasDisplayValue(entry.value))
}

const reportItemStructureHighlights = item => {
  const structureParse = reportItemStructureParse(item)
  return [
    { key: 'parseTaskId', label: isChinese.value ? 'Parse Task' : 'Parse task', value: item?.parseTaskId },
    { key: 'sqlFingerprint', label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint', value: firstValue(structureParse.sqlFingerprint, reportItemParseDetail(item)?.sqlFingerprint) },
    { key: 'syntaxStatus', label: isChinese.value ? '语法状态' : 'Syntax status', value: firstValue(structureParse.syntaxStatus, item?.structureSyntaxStatus) },
    { key: 'complexityLevel', label: isChinese.value ? '复杂度' : 'Complexity', value: structureParse.complexityLevel },
    { key: 'sqlType', label: isChinese.value ? 'SQL 类型' : 'SQL type', value: structureParse.sqlType },
    { key: 'priorityLevel', label: isChinese.value ? '优先级' : 'Priority', value: structureParse.priorityLevel },
    { key: 'priorityScore', label: isChinese.value ? '评分' : 'Score', value: structureParse.priorityScore },
    { key: 'important', label: isChinese.value ? '重要' : 'Important', value: booleanLabel(structureParse.important) },
    { key: 'urgent', label: isChinese.value ? '紧急' : 'Urgent', value: booleanLabel(structureParse.urgent) }
  ].filter(entry => hasDisplayValue(entry.value))
}

const reportItemAccessHighlights = item => {
  const accessParse = reportItemAccessParse(item)
  return [
    { key: 'serviceStatus', label: isChinese.value ? '服务状态' : 'Service status', value: firstValue(accessParse.serviceStatus, item?.accessServiceStatus) },
    { key: 'connectionStatus', label: isChinese.value ? '连接状态' : 'Connection status', value: firstValue(accessParse.connectionStatus, item?.accessConnectionStatus) },
    { key: 'objectResolutionStatus', label: isChinese.value ? '对象解析' : 'Object resolution', value: accessParse.objectResolutionStatus },
    { key: 'partitionStatus', label: isChinese.value ? '分区状态' : 'Partition status', value: accessParse.partitionStatus },
    { key: 'dataFreshnessStatus', label: isChinese.value ? '新鲜度' : 'Freshness', value: accessParse.dataFreshnessStatus },
    { key: 'slaStatus', label: 'SLA', value: accessParse.slaStatus },
    { key: 'compatibilityStatus', label: isChinese.value ? '兼容性' : 'Compatibility', value: accessParse.compatibilityStatus }
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
    return isChinese.value ? '无问题' : 'No issue'
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
  if (hasQueryValue('historyId')) {
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
  form.traceId = hasQueryValue('traceId') ? String(route.query.traceId) : ''
  form.taskId = hasQueryValue('taskId') ? String(route.query.taskId) : ''
  form.reportId = hasQueryValue('reportId') ? String(route.query.reportId) : ''
}

const openRouteDeepLink = async () => {
  if (hasQueryValue('historyId')) {
    await openHistoryDetail(String(route.query.historyId))
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
    handleReportBatchSqlPageChange,
    handleReportBatchSqlPageSizeChange,
    hasDisplayValue,
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
    openAuditForensics,
    openExportDialog,
    openHistoryDetail,
    openParseBatchCenter,
    openRepairEvidence,
    openReportBatchCenter,
    openReportBatchDetail,
    openReportBatchIssueSceneDetail,
    openReportSqlParseDetail,
    openRouteDeepLink,
    page,
    pageSummaryCards,
    parseBatchHistoryPagination,
    parseBatchHistoryRows,
    parseBatchHistorySummary,
    queryDateRange,
    rate,
    referenceGroups,
    refreshSelectedReportSqlPage,
    refreshWorkbench,
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
    reportBatchIssueStatistics,
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
    selectedHistoryDetail,
    selectedHistoryId,
    selectedReportBackendReportStatistics,
    selectedReportBackendSqlStatistics,
    selectedReportBatchDetail,
    selectedReportGroups,
    selectedReportImportanceStatistics,
    selectedReportIssueSceneDetail,
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
    submittedAtRange,
    syncDateRangeFields,
    syncHistoryWorkbenchTabFromRoute,
    syncLookupFieldsFromRoute,
    tenantOptions,
    traceSummaryCards,
    withCurrentOption
  }
}
