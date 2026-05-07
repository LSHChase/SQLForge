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
  getReportBatch,
  getReportBatchIssueSceneDetail,
  getReportBatchParseStatistics,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'
import { buildDatasourceOptions, buildTenantOptions, withCurrentOption } from '../common/formComponentGovernance'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'

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
const batchHistoryTab = ref('parse')
const activeReportBatchStatisticsTab = ref('issueScene')
const selectedHistoryDetail = ref(null)
const selectedReportBatchDetail = ref(null)
const selectedReportIssueSceneDetail = ref(null)
const reportBatchItemDetails = ref({})
const errorMessage = ref('')
const batchHistoryErrorMessage = ref('')
const reportBatchItemDetailErrorMessage = ref('')
const reportBatchDetailDrawerVisible = ref(false)
const exportDialogVisible = ref(false)
const exportResult = ref(null)
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
    card(isChinese.value ? '问题场景' : 'Issue scene', detail.issueScene),
    card(isChinese.value ? '影响 SQL' : 'Affected SQL', detail.affectedSqlCount),
    card(isChinese.value ? '问题数' : 'Issues', detail.affectedIssueCount),
    card(isChinese.value ? '报表数' : 'Reports', detail.reportCount),
    card(isChinese.value ? '逻辑对象' : 'Logical objects', detail.logicalObjectCount),
    card(isChinese.value ? '优先级' : 'Priority', detail.priorityLevel),
    card(isChinese.value ? '严重度' : 'Severity', detail.severity)
  ].filter(item => hasDisplayValue(item.value))
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
  const total = selectedReportItems.value.filter(item => hasDisplayValue(item.parseTaskId) || hasDisplayValue(item.historyId)).length
  const loaded = selectedReportItems.value.filter(item => Boolean(reportItemParseDetail(item))).length
  return [
    card(isChinese.value ? '可追溯 SQL' : 'Traceable SQL', total),
    card(isChinese.value ? '已加载详情' : 'Loaded details', loaded),
    card(isChinese.value ? '缺失详情' : 'Missing details', Math.max(total - loaded, 0))
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
  activeDialogTab.value = 'parseResult'
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

const openReportBatchDetail = async row => {
  const batchId = typeof row === 'string' ? row : row?.batchId
  if (!batchId) {
    return
  }
  loading.reportBatchDetail = true
  batchHistoryErrorMessage.value = ''
  reportBatchItemDetailErrorMessage.value = ''
  reportBatchItemDetails.value = {}
  selectedReportIssueSceneDetail.value = null
  reportBatchSqlPagination.pageNumber = 1
  reportBatchSqlPagination.pageSize = 25
  reportBatchSqlPagination.reportCode = ''
  reportBatchIssueScenePagination.pageNumber = 1
  reportBatchIssueScenePagination.pageSize = 25
  reportBatchIssueScenePagination.reportCode = ''
  reportBatchIssueScenePagination.logicalObjectKey = ''
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
  } catch (error) {
    selectedReportIssueSceneDetail.value = null
    batchHistoryErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportBatchIssueSceneDetail = false
  }
}

const openReportBatchIssueSceneDetail = async item => {
  reportBatchIssueScenePagination.pageNumber = 1
  reportBatchIssueScenePagination.reportCode = ''
  reportBatchIssueScenePagination.logicalObjectKey = ''
  await loadReportBatchIssueSceneDetail(item?.issueScene)
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
        historyId: reportHistoryIdForItem(item)
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
        const detail = await getGovernanceQueryHistoryDetail(
          requestTenantId.value,
          target.historyId,
          {
            requestPrefix: 'frontend-parse-record-report-sql-history-detail'
          }
        )
        return [target.detailKey, detail]
      })
    )
    const next = { ...reportBatchItemDetails.value }
    let failedCount = 0
    results.forEach(result => {
      if (result.status === 'fulfilled') {
        next[result.value[0]] = result.value[1]
        return
      }
      failedCount += 1
    })
    reportBatchItemDetails.value = next
    if (failedCount > 0) {
      reportBatchItemDetailErrorMessage.value = isChinese.value
        ? `${failedCount} 条 SQL 的解析详情暂不可用。`
        : `${failedCount} SQL parse detail records are unavailable.`
    }
  } finally {
    loading.reportBatchItemDetails = false
  }
}

const loadReportBatchItemDetail = async item => {
  await loadReportBatchItemDetails([item])
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

const parseBooleanFilter = value => {
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  return undefined
}

const reportHistoryIdForTask = parseTaskId => {
  const normalized = normalizeQueryValue(parseTaskId)
  return normalized ? `history-parse-${normalized.replace(/[^A-Za-z0-9_-]/g, '-')}` : ''
}

const reportHistoryIdForItem = item =>
  normalizeQueryValue(item?.historyId) || reportHistoryIdForTask(item?.parseTaskId)

const reportItemDetailKey = item =>
  normalizeQueryValue(item?.parseTaskId) || normalizeQueryValue(item?.historyId)

const reportItemParseDetail = item => {
  const detailKey = reportItemDetailKey(item)
  return detailKey ? reportBatchItemDetails.value[detailKey] || null : null
}

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

const displayDetailValue = value => {
  if (value && typeof value === 'object') {
    return formatJson(value)
  }
  return displayValue(value)
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

onMounted(async () => {
  if (hasDisplayValue(route.query.traceId)) {
    form.traceId = String(route.query.traceId)
  }
  if (hasDisplayValue(route.query.reportId)) {
    form.reportId = String(route.query.reportId)
  }
  await refreshWorkbench()
  if (hasDisplayValue(route.query.historyId)) {
    await openHistoryDetail(String(route.query.historyId))
    return
  }
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
                  @click="openReportBatchDetail(row)"
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
            <el-table-column :label="isChinese ? '操作' : 'Actions'" min-width="120">
              <template #default="{ row }">
                <el-button text :loading="loading.reportBatchDetail" @click="openReportBatchCenter(row.batchId)">
                  {{ isChinese ? '批量中心' : 'Batch center' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-drawer
      v-model="reportBatchDetailDrawerVisible"
      :title="selectedReportBatchDetail?.batchName || selectedReportBatchDetail?.batchId || (isChinese ? '报表导入详情' : 'Report import detail')"
      size="64%"
      data-testid="parse-record-report-batch-detail"
    >
      <div class="dialog-stack">
        <div class="summary-grid">
          <article v-for="item in reportBatchDetailCards" :key="item.label" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <section class="code-card">
          <div class="code-card__header">
            <span>{{ isChinese ? '报表级解析统计' : 'Report-level parse statistics' }}</span>
          </div>
          <el-tabs
            v-model="activeReportBatchStatisticsTab"
            class="statistics-tabs"
            data-testid="parse-record-report-statistics-tabs"
          >
            <el-tab-pane :label="isChinese ? '问题场景' : 'Issue scenes'" name="issueScene">
              <div class="detail-grid">
                <button
                  v-for="item in reportBatchIssueStatistics"
                  :key="item.issueScene"
                  type="button"
                  class="detail-grid__item issue-scene-detail-button"
                  data-testid="parse-record-report-statistics-issue-scene"
                  @click="openReportBatchIssueSceneDetail(item)"
                >
                  <span>{{ item.issueScene }}</span>
                  <strong>
                    {{ item.affectedSqlCount }} SQL · {{ displayValue(item.severity) }} · {{ formatPercent(item.ratio) }}
                    <span v-if="item.reportCount"> · {{ item.reportCount }} {{ isChinese ? '报表' : 'reports' }}</span>
                    <span v-if="item.logicalObjectCount"> · {{ item.logicalObjectCount }} {{ isChinese ? '对象' : 'objects' }}</span>
                  </strong>
                </button>
                <p v-if="!reportBatchIssueStatistics.length" class="empty-copy">
                  {{ isChinese ? '当前没有问题场景统计。' : 'No issue statistics in this report batch.' }}
                </p>
              </div>
              <div
                v-if="selectedReportIssueSceneDetail"
                class="issue-scene-detail-panel"
                data-testid="parse-record-report-issue-scene-detail"
              >
                <div class="summary-chip-row">
                  <span v-for="item in reportBatchIssueSceneDetailCards" :key="item.label" class="summary-chip">
                    {{ item.label }}: <strong>{{ displayValue(item.value) }}</strong>
                  </span>
                  <span v-if="loading.reportBatchIssueSceneDetail" class="summary-chip summary-chip-warning">
                    {{ isChinese ? '正在加载场景详情' : 'Loading scene detail' }}
                  </span>
                </div>
                <div class="filter-row">
                  <el-input
                    v-model="reportBatchIssueScenePagination.reportCode"
                    :placeholder="isChinese ? '按报表编码筛选' : 'Filter by report code'"
                    clearable
                  />
                  <el-input
                    v-model="reportBatchIssueScenePagination.logicalObjectKey"
                    :placeholder="isChinese ? '按逻辑对象筛选' : 'Filter by logical object'"
                    clearable
                  />
                  <el-button :loading="loading.reportBatchIssueSceneDetail" @click="applyReportBatchIssueSceneFilter">
                    {{ isChinese ? '查询详情' : 'Search detail' }}
                  </el-button>
                </div>
                <div class="detail-grid detail-grid-secondary">
                  <div
                    v-for="item in normalizeArray(selectedReportIssueSceneDetail.reportDetails).slice(0, 8)"
                    :key="item.reportCode"
                    class="detail-grid__item"
                    data-testid="parse-record-report-issue-scene-report"
                  >
                    <span>{{ item.reportCode }}</span>
                    <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues</strong>
                    <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                  </div>
                </div>
                <div class="detail-grid detail-grid-secondary">
                  <div
                    v-for="item in normalizeArray(selectedReportIssueSceneDetail.logicalObjectDetails).slice(0, 8)"
                    :key="item.objectKey"
                    class="detail-grid__item"
                    data-testid="parse-record-report-issue-scene-object"
                  >
                    <span>{{ item.objectKey }}</span>
                    <strong>{{ item.sqlCount }} SQL · {{ item.reportCount }} reports</strong>
                    <p>{{ displayValue(item.reportCodes) }}</p>
                  </div>
                </div>
                <div class="report-sql-list">
                  <article
                    v-for="item in normalizeArray(selectedReportIssueSceneDetail.sqlStatistics)"
                    :key="item.itemId"
                    class="detail-grid__item report-sql-card"
                    data-testid="parse-record-report-issue-scene-sql"
                  >
                    <span>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId }}</span>
                    <strong>{{ item.highestPriorityLevel }} · {{ item.issueCount }} issues</strong>
                    <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                    <p>{{ isChinese ? '问题场景' : 'Issue scenes' }}: {{ displayValue(item.issueScenes) }}</p>
                  </article>
                </div>
                <el-pagination
                  v-if="Number(selectedReportIssueSceneDetail.sqlStatisticTotalCount || 0) > reportBatchIssueScenePagination.pageSize"
                  class="pagination-row"
                  layout="total, prev, pager, next"
                  :total="Number(selectedReportIssueSceneDetail.sqlStatisticTotalCount || 0)"
                  :page-size="reportBatchIssueScenePagination.pageSize"
                  :current-page="reportBatchIssueScenePagination.pageNumber"
                  @current-change="handleReportBatchIssueScenePageChange"
                />
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '重要程度' : 'Importance'" name="importance">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportImportanceStatistics"
                  :key="item.importanceBucket"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-importance"
                >
                  <span>{{ item.importanceBucket }}</span>
                  <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</strong>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '报表视角' : 'Report view'" name="report">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportBackendReportStatistics"
                  :key="item.reportCode"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-report-view"
                >
                  <span>{{ item.reportCode }}</span>
                  <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ formatPercent(item.issueSqlRatio) }}</strong>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sqlList">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportBackendSqlStatistics.slice(0, 8)"
                  :key="item.itemId"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-sql-list"
                >
                  <span>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId }}</span>
                  <strong>{{ item.highestPriorityLevel }} · {{ item.issueCount }} issues</strong>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '优先级视角' : 'Priority view'" name="priority">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportPriorityMatrix"
                  :key="`${item.priorityLevel}-${item.urgencyBucket}`"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-priority"
                >
                  <span>{{ item.priorityLevel }} · {{ item.urgencyBucket }}</span>
                  <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</strong>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '逻辑对象视角' : 'Logical objects'" name="logicalObject">
              <div class="detail-grid">
                <div
                  v-for="item in reportBatchLogicalObjectStatistics"
                  :key="item.objectKey"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-logical-object"
                >
                  <span>{{ item.objectKey }}</span>
                  <strong>{{ item.hitCount }} SQL · {{ displayValue(item.reportCodes) }}</strong>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
        <section class="code-card">
          <div class="code-card__header">
            <span>{{ isChinese ? 'SQL 级解析详情' : 'SQL-level parse detail' }}</span>
          </div>
          <div class="filter-row" data-testid="parse-record-report-sql-filter">
            <el-input
              v-model="reportBatchSqlPagination.reportCode"
              :placeholder="isChinese ? '按报表编码筛选' : 'Filter by report code'"
              clearable
            />
            <el-button :loading="loading.reportBatchDetail" @click="applyReportBatchSqlFilter">
              {{ isChinese ? '查询' : 'Search' }}
            </el-button>
          </div>
          <div class="summary-chip-row" data-testid="parse-record-report-sql-detail-statistics">
            <span v-for="item in reportBatchParseDetailSummary" :key="item.label" class="summary-chip">
              {{ item.label }}: <strong>{{ item.value }}</strong>
            </span>
            <span v-if="loading.reportBatchItemDetails" class="summary-chip summary-chip-warning">
              {{ isChinese ? '正在加载每条 SQL 的解析详情' : 'Loading per-SQL parse details' }}
            </span>
          </div>
          <p v-if="reportBatchItemDetailErrorMessage" class="empty-copy" data-testid="parse-record-report-sql-detail-load-warning">
            {{ reportBatchItemDetailErrorMessage }}
          </p>
          <div class="report-sql-list">
            <section
              v-for="group in selectedReportGroups"
              :key="group.reportCode"
              class="report-sql-group"
              data-testid="parse-record-report-group"
            >
              <div class="session-item-top">
                <strong>{{ group.reportCode }}</strong>
                <span class="summary-chip">{{ group.total }} SQL</span>
              </div>
              <p>
                {{ displayValue(group.reportName) }}
                · {{ isChinese ? '已解析' : 'Resolved' }} {{ group.resolved }}
                · {{ isChinese ? '失败' : 'Failed' }} {{ group.failed }}
                · Structure {{ formatPercent(group.structureRate) }}
                · Access {{ formatPercent(group.accessRate) }}
                · {{ isChinese ? '问题场景' : 'Issue scenes' }} {{ group.issueSceneCount }}
                · {{ isChinese ? '逻辑对象' : 'Logical objects' }} {{ group.logicalObjectCount }}
              </p>
              <div class="report-sql-list">
                <article
                  v-for="(item, index) in group.items"
                  :key="item.itemId || `${group.reportCode}-${index}`"
                  class="detail-grid__item report-sql-card"
                  data-testid="parse-record-report-sql-detail"
                >
                  <span>{{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }} · {{ displayValue(item.status) }}</span>
                  <strong>{{ displayValue(item.reportName || group.reportCode) }}</strong>
                  <p>
                    {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                    · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                    · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  </p>
                  <p data-testid="parse-record-report-sql-history-link">
                    {{ isChinese ? '解析历史' : 'Parse history' }}:
                    {{ displayValue(reportHistoryIdForItem(item)) }}
                    · {{ displayValue(item.historyPersistenceStatus) }}
                  </p>
                  <p>{{ isChinese ? '问题场景' : 'Issue scenes' }}: {{ displayValue(item.issueScenes) }}</p>
                  <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                  <p class="empty-copy">{{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}</p>
                  <SqlCodeBlock
                    v-if="item.sqlText"
                    :value="item.sqlText"
                    :label="isChinese ? 'SQL 输出' : 'SQL output'"
                    :copy-label="isChinese ? '复制' : 'Copy'"
                    compact
                    data-testid="parse-record-report-sql-code"
                  />
                  <div v-if="reportHistoryIdForItem(item) && !reportItemParseDetail(item)" class="dialog-actions">
                    <el-button text :loading="loading.reportBatchItemDetails" @click="loadReportBatchItemDetail(item)">
                      {{ isChinese ? '加载解析详情' : 'Load parse detail' }}
                    </el-button>
                  </div>
                  <div
                    v-if="reportItemParseDetail(item)"
                    class="report-parse-detail"
                    data-testid="parse-record-report-sql-parse-detail"
                  >
                    <div class="result-banner" :class="resultBannerClass(reportItemParseStatus(item))">
                      <strong>{{ reportItemParseStatus(item) || '-' }}</strong>
                      <span>{{ reportItemParseDetail(item)?.historyId || reportHistoryIdForItem(item) }}</span>
                    </div>
                    <div class="summary-chip-row">
                      <span v-for="detailItem in reportItemParseSummaryCards(item)" :key="`${item.itemId}-${detailItem.label}`" class="summary-chip">
                        {{ detailItem.label }}: <strong>{{ displayValue(detailItem.value) }}</strong>
                      </span>
                    </div>
                    <div class="detail-grid detail-grid-secondary">
                      <div v-for="detailItem in reportItemParseStatisticCards(item)" :key="`${item.itemId}-stat-${detailItem.label}`" class="detail-grid__item">
                        <span>{{ detailItem.label }}</span>
                        <strong>{{ displayValue(detailItem.value) }}</strong>
                      </div>
                    </div>
                    <div v-if="reportItemStructureHighlights(item).length" class="mini-section">
                      <span class="summary-card-label">{{ isChinese ? '结构解析卡' : 'Structure parse card' }}</span>
                      <div class="highlight-grid">
                        <div
                          v-for="detailItem in reportItemStructureHighlights(item)"
                          :key="`${item.itemId}-structure-${detailItem.key}`"
                          class="highlight-chip"
                          :class="resultValueClass(detailItem)"
                        >
                          <span>{{ detailItem.label }}</span>
                          <strong>{{ displayValue(detailItem.value) }}</strong>
                        </div>
                      </div>
                    </div>
                    <div v-if="reportItemAccessHighlights(item).length" class="mini-section">
                      <span class="summary-card-label">{{ isChinese ? 'Access Parse 卡' : 'Access parse card' }}</span>
                      <div class="highlight-grid">
                        <div
                          v-for="detailItem in reportItemAccessHighlights(item)"
                          :key="`${item.itemId}-access-${detailItem.key}`"
                          class="highlight-chip"
                          :class="resultValueClass(detailItem)"
                        >
                          <span>{{ detailItem.label }}</span>
                          <strong>{{ displayValue(detailItem.value) }}</strong>
                        </div>
                      </div>
                    </div>
                    <div v-if="normalizeArray(reportItemStructureParse(item).riskChecklist).length" class="issue-list issue-list-compact">
                      <article
                        v-for="(risk, riskIndex) in normalizeArray(reportItemStructureParse(item).riskChecklist)"
                        :key="`${item.itemId}-risk-${risk.riskCode || riskIndex}`"
                        class="issue-card"
                        data-testid="parse-record-report-sql-risk"
                      >
                        <div class="issue-card__header">
                          <strong>{{ risk.riskCode || '-' }}</strong>
                          <span>{{ risk.severity || '-' }}</span>
                        </div>
                        <p class="issue-card__summary">{{ displayDetailValue(risk.summary) }}</p>
                        <p class="issue-card__detail">{{ displayDetailValue(risk.evidence) }}</p>
                        <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ displayDetailValue(risk.suggestedAction) }}</p>
                      </article>
                    </div>
                    <div v-if="normalizeArray(reportItemStructureParse(item).issues).length" class="issue-list">
                      <article
                        v-for="(issue, issueIndex) in normalizeArray(reportItemStructureParse(item).issues)"
                        :key="`${item.itemId}-issue-${issue.issueCode || issueIndex}`"
                        class="issue-card"
                        data-testid="parse-record-report-sql-issue"
                      >
                        <div class="issue-card__header">
                          <strong>{{ issue.issueCode || '-' }}</strong>
                          <span>{{ displayValue(firstValue(issue.severity, issue.priorityLevel)) }}</span>
                        </div>
                        <p class="issue-card__summary">{{ displayDetailValue(issue.summary) }}</p>
                        <p class="issue-card__detail">{{ displayDetailValue(issue.detail) }}</p>
                        <p v-if="issue.failureLine || issue.failureColumn || issue.failureToken || issue.failureSnippet" class="issue-card__detail">
                          {{ isChinese ? '失败定位' : 'Failure position' }}:
                          <span v-if="issue.failureLine && issue.failureColumn">line {{ issue.failureLine }}, column {{ issue.failureColumn }}</span>
                          <span v-if="issue.failureToken"> · token {{ issue.failureToken }}</span>
                          <span v-if="issue.failureSnippet"> · {{ issue.failureSnippet }}</span>
                        </p>
                        <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ displayDetailValue(issue.suggestedAction) }}</p>
                      </article>
                    </div>
                    <div class="dialog-actions">
                      <el-button text @click="openHistoryDetail(reportItemParseDetail(item).historyId || reportHistoryIdForItem(item))">
                        {{ isChinese ? '打开完整解析历史' : 'Open full parse history' }}
                      </el-button>
                    </div>
                  </div>
                  <p v-else-if="reportHistoryIdForItem(item) && !loading.reportBatchItemDetails" class="empty-copy" data-testid="parse-record-report-sql-detail-missing">
                    {{ isChinese ? '点击加载解析详情后展示结构化解析统计。' : 'Load parse detail to show structured parse statistics.' }}
                  </p>
                  <p v-else class="empty-copy" data-testid="parse-record-report-sql-history-detail-unavailable">
                    {{ isChinese ? '解析历史暂不可用；当前仅展示报表批次内的 SQL 解析证据。' : 'Parse history is unavailable; this card shows report-batch SQL evidence only.' }}
                  </p>
                </article>
              </div>
            </section>
            <p v-if="!selectedReportItems.length" class="empty-copy">
              {{ isChinese ? '导入解析后会展示 SQL 明细。' : 'SQL detail appears after report SQL resolution.' }}
            </p>
            <el-pagination
              v-if="Number(selectedReportBatchDetail?.itemTotalCount || 0) > reportBatchSqlPagination.pageSize"
              class="pagination-row"
              layout="total, sizes, prev, pager, next"
              :total="Number(selectedReportBatchDetail?.itemTotalCount || 0)"
              :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
              :page-size="reportBatchSqlPagination.pageSize"
              :current-page="reportBatchSqlPagination.pageNumber"
              @current-change="handleReportBatchSqlPageChange"
              @size-change="handleReportBatchSqlPageSizeChange"
            />
          </div>
        </section>
      </div>
    </el-drawer>

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

          <el-tab-pane :label="isChinese ? '解析结果' : 'Parse result'" name="parseResult">
            <div class="history-result-panel" data-testid="parse-record-history-parse-detail">
              <article class="code-card code-card-wide" data-testid="parse-record-history-original-sql">
                <div class="code-card__header">
                  <span>{{ isChinese ? '原始 SQL' : 'Original SQL' }}</span>
                </div>
                <SqlCodeBlock
                  :value="selectedHistoryDetail.sqlText || '-'"
                  :label="isChinese ? '原始 SQL' : 'Original SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  data-testid="parse-record-history-original-sql-text"
                />
              </article>

              <div class="result-banner" :class="resultBannerClass(historyParseStatus)">
                <strong data-testid="parse-record-history-parse-status">{{ historyParseStatus || '-' }}</strong>
                <span>{{ historyParseTaskId || selectedHistoryDetail.historyId }}</span>
              </div>

              <div class="result-overview-card" data-testid="parse-record-history-parse-statistics">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">parse result</p>
                    <h3 class="detail-title">{{ isChinese ? '解析结果与统计' : 'Parse result and statistics' }}</h3>
                  </div>
                </div>
                <div v-if="historyParseSummaryCards.length" class="summary-chip-row">
                  <span v-for="item in historyParseSummaryCards" :key="item.label" class="summary-chip">
                    {{ item.label }}: <strong>{{ displayValue(item.value) }}</strong>
                  </span>
                </div>
                <div v-if="historyParseStatisticCards.length" class="detail-grid detail-grid-secondary">
                  <div v-for="item in historyParseStatisticCards" :key="item.label" class="detail-grid__item">
                    <span>{{ item.label }}</span>
                    <strong>{{ displayValue(item.value) }}</strong>
                  </div>
                </div>
                <p v-if="historyResultSummary.summary" class="result-copy">{{ historyResultSummary.summary }}</p>
                <p v-if="historyResultSummary.recommendedAction" class="result-copy result-copy-muted">
                  {{ historyResultSummary.recommendedAction }}
                </p>
              </div>

              <div
                v-if="!historyStructureHighlights.length && !historyAccessHighlights.length"
                class="empty-copy"
                data-testid="parse-record-history-parse-detail-empty"
              >
                {{ isChinese ? '当前记录没有结构化解析详情，只能查看原始证据。' : 'This record has no structured parse detail; raw evidence is still available.' }}
              </div>

              <div class="parse-card-grid">
                <article v-if="historyStructureHighlights.length" class="parse-card" data-testid="parse-record-history-structure-card">
                  <div class="parse-card__header">
                    <div>
                      <p class="section-kicker sqlforge-code-label">structure parse</p>
                      <h3 class="detail-title">{{ isChinese ? '结构解析卡' : 'Structure parse card' }}</h3>
                    </div>
                    <span class="pill" :class="statusClass(historyStructureParse.syntaxStatus || historyParseStatus)">
                      {{ historyStructureParse.syntaxStatus || '-' }}
                    </span>
                  </div>

                  <div class="highlight-grid">
                    <div
                      v-for="item in historyStructureHighlights"
                      :key="item.key"
                      class="highlight-chip"
                      :class="resultValueClass(item)"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ displayValue(item.value) }}</strong>
                    </div>
                  </div>

                  <div v-if="historyStructureIntentLabels.length" class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? '查询意图标签' : 'Query intent labels' }}</span>
                    <div class="pill-grid">
                      <span
                        v-for="item in historyStructureIntentLabels"
                        :key="`history-intent-${item}`"
                        class="summary-chip summary-chip-success"
                      >
                        {{ item }}
                      </span>
                    </div>
                  </div>

                  <div v-if="historyStructureFeatureHighlights.length" class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? '多维特征' : 'Feature dimensions' }}</span>
                    <div class="highlight-grid highlight-grid-compact">
                      <div
                        v-for="item in historyStructureFeatureHighlights"
                        :key="item.key"
                        class="highlight-chip"
                        :class="resultValueClass(item)"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ displayValue(item.value) }}</strong>
                      </div>
                    </div>
                  </div>

                  <div v-if="historyStructureResourceHighlights.length" class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? '预估资源消耗' : 'Estimated resource cost' }}</span>
                    <div class="summary-chip-row">
                      <span
                        v-for="item in historyStructureResourceHighlights"
                        :key="`history-resource-${item.key}`"
                        class="summary-chip"
                      >
                        {{ item.label }}: <strong>{{ displayValue(item.value) }}</strong>
                      </span>
                    </div>
                  </div>

                  <div class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? '查询日期摘要' : 'Query-date summary' }}</span>
                    <div class="summary-chip-row">
                      <span class="summary-chip">
                        {{ isChinese ? '起点' : 'Start' }}:
                        <strong>{{ displayValue(historyStructureParse.queryDateSummary?.queryDateStart || selectedHistoryDetail.queryDateSummary?.queryDateStart) }}</strong>
                      </span>
                      <span class="summary-chip">
                        {{ isChinese ? '终点' : 'End' }}:
                        <strong>{{ displayValue(historyStructureParse.queryDateSummary?.queryDateEnd || selectedHistoryDetail.queryDateSummary?.queryDateEnd) }}</strong>
                      </span>
                      <span class="summary-chip">
                        {{ isChinese ? '状态' : 'Status' }}:
                        <strong>{{ displayValue(historyStructureParse.queryDateSummary?.queryDateStatus || selectedHistoryDetail.queryDateSummary?.queryDateStatus) }}</strong>
                      </span>
                    </div>
                  </div>

                  <div v-if="historyLogicalObjectHits.length" class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? '逻辑对象命中' : 'Logical object hits' }}</span>
                    <div class="pill-grid">
                      <span
                        v-for="(item, index) in historyLogicalObjectHits"
                        :key="`${item.objectKey || item.logicalObjectKey || item.objectName || index}`"
                        class="summary-chip"
                      >
                        {{ item.objectType || item.logicalObjectType || 'OBJECT' }}:
                        <strong>{{ item.objectKey || item.logicalObjectKey || item.objectName || '-' }}</strong>
                      </span>
                    </div>
                  </div>

                  <div
                    v-if="normalizeArray(historyStructureParse.riskTags).length || normalizeArray(historyStructureParse.rewriteCandidates).length"
                    class="mini-section"
                  >
                    <span class="summary-card-label">{{ isChinese ? '风险标签 / 改写候选' : 'Risk tags / rewrite candidates' }}</span>
                    <div class="pill-grid">
                      <span
                        v-for="item in normalizeArray(historyStructureParse.riskTags)"
                        :key="`history-risk-${item}`"
                        class="summary-chip summary-chip-warning"
                      >
                        {{ item }}
                      </span>
                      <span
                        v-for="item in normalizeArray(historyStructureParse.rewriteCandidates)"
                        :key="`history-candidate-${item}`"
                        class="summary-chip summary-chip-success"
                      >
                        {{ item }}
                      </span>
                    </div>
                  </div>

                  <div v-if="historyStructureRiskChecklist.length" class="issue-list issue-list-compact">
                    <article
                      v-for="(risk, index) in historyStructureRiskChecklist"
                      :key="`${risk.riskCode || 'risk'}-${index}`"
                      class="issue-card"
                      data-testid="parse-record-history-risk"
                    >
                      <div class="issue-card__header">
                        <strong>{{ risk.riskCode || '-' }}</strong>
                        <span>{{ risk.severity || '-' }}</span>
                      </div>
                      <p class="issue-card__summary">{{ displayDetailValue(risk.summary) }}</p>
                      <p class="issue-card__detail">{{ displayDetailValue(risk.evidence) }}</p>
                      <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ displayDetailValue(risk.suggestedAction) }}</p>
                    </article>
                  </div>

                  <div v-if="historyStructureIssues.length" class="issue-list">
                    <article
                      v-for="(issue, index) in historyStructureIssues"
                      :key="`${issue.issueCode || 'issue'}-${index}`"
                      class="issue-card"
                      data-testid="parse-record-history-issue"
                    >
                      <div class="issue-card__header">
                        <strong>{{ issue.issueCode || '-' }}</strong>
                        <span>{{ displayValue(firstValue(issue.severity, issue.priorityLevel)) }}</span>
                      </div>
                      <p class="issue-card__summary">{{ displayDetailValue(issue.summary) }}</p>
                      <p class="issue-card__detail">{{ displayDetailValue(issue.detail) }}</p>
                      <p v-if="issue.failureLine || issue.failureColumn || issue.failureToken || issue.failureSnippet" class="issue-card__detail">
                        {{ isChinese ? '失败定位' : 'Failure position' }}:
                        <span v-if="issue.failureLine && issue.failureColumn">line {{ issue.failureLine }}, column {{ issue.failureColumn }}</span>
                        <span v-if="issue.failureToken"> · token {{ issue.failureToken }}</span>
                        <span v-if="issue.failureSnippet"> · {{ issue.failureSnippet }}</span>
                      </p>
                      <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ displayDetailValue(issue.suggestedAction) }}</p>
                    </article>
                  </div>
                </article>

                <article v-if="historyAccessHighlights.length" class="parse-card" data-testid="parse-record-history-access-card">
                  <div class="parse-card__header">
                    <div>
                      <p class="section-kicker sqlforge-code-label">access parse</p>
                      <h3 class="detail-title">{{ isChinese ? 'Access Parse 卡' : 'Access parse card' }}</h3>
                    </div>
                    <span
                      class="pill"
                      :class="statusClass(historyAccessParse.serviceStatus === 'AVAILABLE' && historyAccessParse.connectionStatus === 'CONNECTED' ? 'SUCCESS' : historyAccessParse.serviceStatus)"
                    >
                      {{ historyAccessParse.serviceStatus || '-' }}
                    </span>
                  </div>

                  <div class="highlight-grid">
                    <div
                      v-for="item in historyAccessHighlights"
                      :key="item.key"
                      class="highlight-chip"
                      :class="resultValueClass(item)"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ displayValue(item.value) }}</strong>
                    </div>
                  </div>

                  <div v-if="historyAccessParse.planSummary" class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? 'Plan Summary' : 'Plan summary' }}</span>
                    <p class="result-copy">{{ historyAccessParse.planSummary }}</p>
                  </div>

                  <div v-if="historyAccessParse.availabilityWarning" class="mini-section">
                    <span class="summary-card-label">{{ isChinese ? '可用性告警' : 'Availability warning' }}</span>
                    <p class="result-copy result-copy-muted">{{ historyAccessParse.availabilityWarning }}</p>
                  </div>
                </article>
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
                <SqlCodeBlock
                  :value="item.value"
                  :label="item.label"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  :data-testid="`parse-record-${item.key}`"
                />
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

.report-sql-list {
  display: grid;
  gap: 12px;
}

.report-sql-group {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 14px;
  background: rgba(35, 35, 35, 0.88);
}

.report-sql-card {
  display: grid;
  gap: 8px;
}

.report-parse-detail {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.report-sql-card p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
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

.history-result-panel,
.parse-card,
.result-overview-card,
.mini-section,
.issue-list {
  display: grid;
  gap: 14px;
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

.issue-scene-detail-button {
  width: 100%;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.72);
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.issue-scene-detail-button:hover {
  border-color: var(--sqlforge-color-brand);
}

.issue-scene-detail-panel {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
}

.code-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.parse-card-grid,
.highlight-grid,
.pill-grid,
.summary-chip-row {
  display: grid;
  gap: 12px;
}

.parse-card-grid {
  grid-template-columns: 1fr;
}

.highlight-grid {
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}

.pill-grid,
.summary-chip-row {
  display: flex;
  flex-wrap: wrap;
}

.filter-row,
.pagination-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.filter-row {
  margin-bottom: 12px;
}

.filter-row .el-input {
  max-width: 320px;
}

.pagination-row {
  justify-content: flex-end;
  margin-top: 12px;
}

.code-card {
  padding: 14px;
}

.code-card-wide {
  grid-column: 1 / -1;
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

.summary-chip,
.highlight-chip,
.issue-card,
.parse-card,
.result-overview-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 18px;
  background: rgba(20, 24, 31, 0.72);
}

.summary-chip {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 8px 12px;
  font-size: 12px;
}

.summary-chip-warning {
  background: rgba(251, 191, 36, 0.18);
}

.summary-chip-success {
  background: rgba(16, 185, 129, 0.18);
}

.highlight-chip,
.issue-card,
.parse-card,
.result-overview-card {
  padding: 14px;
}

.highlight-chip {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.highlight-chip span,
.issue-card__detail {
  color: var(--sqlforge-text-secondary);
}

.highlight-chip-danger {
  border-color: rgba(248, 113, 113, 0.55);
  background: rgba(239, 68, 68, 0.14);
}

.highlight-chip-danger strong {
  color: #fca5a5;
}

.highlight-chip-warning {
  border-color: rgba(251, 191, 36, 0.45);
  background: rgba(251, 191, 36, 0.12);
}

.parse-card__header,
.issue-card__header,
.result-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.issue-card__summary,
.issue-card__detail,
.result-copy {
  margin: 0;
  line-height: 1.6;
}

.result-copy-muted {
  color: var(--sqlforge-text-secondary);
}

.result-banner {
  border: 1px solid transparent;
  border-radius: 18px;
  padding: 14px 16px;
}

.result-banner-success {
  background: rgba(16, 185, 129, 0.14);
  color: #86efac;
}

.result-banner-warning {
  background: rgba(251, 191, 36, 0.16);
  color: #fde68a;
}

.result-banner-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.35);
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
