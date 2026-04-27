<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  createParseBatch,
  formatRuntimeError,
  getCombinedParseStatus,
  getGovernanceQueryHistoryDetail,
  getGovernanceQueryHistoryPage,
  getGovernanceTraceDetail,
  getParseBatch,
  getParseStatisticsByIssueScene,
  getParseStatisticsByReport,
  getParseStatisticsBySql,
  getParseStatisticsImportantUrgent,
  getParseStatisticsOverview,
  getParseStatisticsPriorityMatrix,
  getReportBatch,
  importReportBatch,
  ingestParseBatch,
  lookupGovernanceTraces,
  parseStructureSql,
  resolveReportBatchSqls,
  retryParseBatchAccess,
  submitCombinedParse,
  waitForCombinedParse
} from '../../services/runtimeGateApi'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()

const terminalStatuses = new Set(['ACCESS_SUCCEEDED', 'PARTIAL_SUCCEEDED', 'FAILED', 'STRUCTURE_ONLY'])
const combinedTerminalStatuses = new Set(['ACCESS_SUCCEEDED', 'PARTIAL_SUCCEEDED', 'FAILED'])

const form = reactive({
  tenantId: 'tenant-a',
  datasourceCode: 'hetu_main',
  bindingMode: 'POSITIONAL',
  connectionRequired: true,
  sqlText: "SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id",
  sqlTemplateText: '',
  bindParametersText: '{\n  "limit": 100\n}',
  commentContextText: '{\n  "report_code": "RPT_SALES_DAILY",\n  "stage": "PROD",\n  "engine_hint": "hetu"\n}'
})

const parseBatchForm = reactive({
  tenantId: 'tenant-a',
  batchName: 'batch-alpha',
  importMode: 'TABULAR_FILE',
  fileType: 'CSV',
  templateVersion: 'v1',
  datasourceCode: 'hetu_main',
  structureParseOnly: false,
  directInputMode: 'SQL_LINES',
  rawContent:
    "SELECT * FROM orders WHERE dt = '2026-04-01';\nSELECT * FROM vw_orders WHERE dt = '2026-04-02';"
})

const retryForm = reactive({
  failureFilter: 'UNAVAILABLE',
  datasourceCode: 'hetu_main',
  forceRecheckAvailability: false
})

const reportBatchForm = reactive({
  tenantId: 'tenant-a',
  batchName: 'report-batch-alpha',
  fileType: 'TXT',
  reportCodeField: 'report_code',
  datasourceCode: 'hetu_main',
  stage: 'PROD',
  priority: 'high',
  rawContent: 'RPT_A|Revenue Report|hetu_main|PROD|high\nRPT_B|Ops Report|hetu_main|PROD|medium\n'
})

const historyForm = reactive({
  tenantId: 'tenant-a',
  reportCode: '',
  accessChannel: '',
  status: '',
  engine: '',
  submittedStart: '',
  submittedEnd: '',
  traceId: '',
  taskId: '',
  reportId: ''
})

const running = ref(false)
const lastRunMode = ref('combined')
const parseResult = ref(null)
const errorMessage = ref('')
const analyticsErrorMessage = ref('')
const historyErrorMessage = ref('')

const batchDialogVisible = ref(false)
const activeBatchWorkspace = ref('parse')
const parseUploadFile = ref(null)
const reportUploadFile = ref(null)
const parseBatchSessions = ref([])
const reportBatchSessions = ref([])
const parseBatchDetail = ref(null)
const reportBatchDetail = ref(null)
const parseCreateDialogVisible = ref(false)
const parseImportDialogVisible = ref(false)
const parseTemplateDialogVisible = ref(false)
const parseDetailDrawerVisible = ref(false)
const reportImportDialogVisible = ref(false)
const reportDetailDrawerVisible = ref(false)

const activeAnalyticsTab = ref('issue')
const statisticsDetailDialogVisible = ref(false)
const statisticsDetailTitle = ref('')
const statisticsDetailPayload = ref(null)
const overview = ref(null)
const issueScenes = ref([])
const sqlStats = ref([])
const reportStats = ref([])
const priorityMatrix = ref([])
const importantUrgent = ref([])

const historyPage = ref(null)
const selectedHistoryId = ref('')
const historyDetailDialogVisible = ref(false)
const historyDetailDrawerVisible = ref(false)
const activeHistoryDialogTab = ref('overview')
const selectedHistoryDetail = ref(null)

const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)

const loading = reactive({
  analytics: false,
  historyPage: false,
  historyDetail: false,
  historyLookup: false,
  createParseBatch: false,
  ingestParseBatch: false,
  refreshParseBatch: false,
  retryParseBatch: false,
  importReportBatch: false,
  resolveReportBatch: false,
  refreshReportBatch: false
})

const isChinese = computed(() => locale.value === 'zh-CN')
const bindingModeOptions = ['POSITIONAL', 'NAMED']
const parseFileTypeOptions = ['CSV', 'TXT', 'SQL', 'XLS', 'XLSX', 'ET']
const reportFileTypeOptions = ['TXT', 'CSV', 'XLSX']
const parseImportModeOptions = ['TABULAR_FILE', 'SQL_FILE', 'REPORT_CATALOG']
const directInputModeOptions = ['SQL_LINES', 'TABULAR_TEXT']

const activeStatus = computed(() => parseResult.value?.status || 'IDLE')
const activeConclusion = computed(() => parseResult.value?.conclusion || null)
const structureParse = computed(() => parseResult.value?.structureParse || null)
const accessParse = computed(() => parseResult.value?.accessParse || null)
const statusHistory = computed(() => parseResult.value?.statusHistory || [])
const logicalObjectHits = computed(() => structureParse.value?.logicalObjectHits || [])
const structureIssues = computed(() => structureParse.value?.issues || [])

const structureHighlights = computed(() => {
  if (!structureParse.value) {
    return []
  }
  return [
    { key: 'parseTaskId', label: isChinese.value ? 'Parse Task' : 'Parse task', value: structureParse.value.parseTaskId },
    { key: 'syntaxStatus', label: isChinese.value ? '语法状态' : 'Syntax status', value: structureParse.value.syntaxStatus },
    { key: 'complexityLevel', label: isChinese.value ? '复杂度' : 'Complexity', value: structureParse.value.complexityLevel },
    { key: 'sqlType', label: isChinese.value ? 'SQL 类型' : 'SQL type', value: structureParse.value.sqlType },
    { key: 'priorityLevel', label: isChinese.value ? '优先级' : 'Priority', value: structureParse.value.priorityLevel },
    { key: 'priorityScore', label: isChinese.value ? '评分' : 'Score', value: structureParse.value.priorityScore },
    { key: 'important', label: isChinese.value ? '重要' : 'Important', value: booleanLabel(structureParse.value.important) },
    { key: 'urgent', label: isChinese.value ? '紧急' : 'Urgent', value: booleanLabel(structureParse.value.urgent) }
  ].filter(item => hasDisplayValue(item.value))
})

const accessHighlights = computed(() => {
  if (!accessParse.value) {
    return []
  }
  return [
    { key: 'serviceStatus', label: isChinese.value ? '服务状态' : 'Service status', value: accessParse.value.serviceStatus },
    { key: 'connectionStatus', label: isChinese.value ? '连接状态' : 'Connection status', value: accessParse.value.connectionStatus },
    { key: 'objectResolutionStatus', label: isChinese.value ? '对象解析' : 'Object resolution', value: accessParse.value.objectResolutionStatus },
    { key: 'partitionStatus', label: isChinese.value ? '分区状态' : 'Partition status', value: accessParse.value.partitionStatus },
    { key: 'dataFreshnessStatus', label: isChinese.value ? '新鲜度' : 'Freshness', value: accessParse.value.dataFreshnessStatus },
    { key: 'slaStatus', label: isChinese.value ? 'SLA' : 'SLA', value: accessParse.value.slaStatus },
    { key: 'compatibilityStatus', label: isChinese.value ? '兼容性' : 'Compatibility', value: accessParse.value.compatibilityStatus }
  ].filter(item => hasDisplayValue(item.value))
})

const summaryCards = computed(() => {
  if (!parseResult.value) {
    return []
  }
  return [
    {
      label: isChinese.value ? '运行模式' : 'Run mode',
      value: lastRunMode.value === 'combined'
        ? (isChinese.value ? '综合解析' : 'Combined parse')
        : (isChinese.value ? '仅结构解析' : 'Structure only')
    },
    { label: isChinese.value ? '综合状态' : 'Overall status', value: activeConclusion.value?.overallStatus || activeStatus.value },
    { label: isChinese.value ? 'Access 可用' : 'Access available', value: booleanLabel(activeConclusion.value?.accessAvailable) },
    { label: isChinese.value ? '降级原因' : 'Degrade reason', value: activeConclusion.value?.degradeReason || parseResult.value?.degradeReason }
  ].filter(item => hasDisplayValue(item.value))
})

const requestSummary = computed(() => [
  { label: isChinese.value ? '租户' : 'Tenant', value: form.tenantId },
  { label: isChinese.value ? '数据源' : 'Datasource', value: form.datasourceCode || (isChinese.value ? '未指定' : 'Not specified') },
  { label: isChinese.value ? '绑定模式' : 'Binding mode', value: form.bindingMode },
  { label: isChinese.value ? 'Access Parse' : 'Access parse', value: form.connectionRequired ? 'ON' : 'OFF' }
])

const parseWorkbenchSummary = computed(() => [
  { label: isChinese.value ? '批量入口' : 'Batch entry', value: isChinese.value ? '弹窗工作区' : 'Dialog workspace' },
  { label: isChinese.value ? '统计视角' : 'Statistics views', value: 4 },
  { label: isChinese.value ? '历史模式' : 'History mode', value: hasLookupCriteria.value ? 'INDEXED' : 'PAGE' }
])

const parseBatchStatusCards = computed(() => {
  if (!parseBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '批次状态' : 'Batch status', parseBatchDetail.value.status),
    card(isChinese.value ? '总记录数' : 'Total records', parseBatchDetail.value.totalRecords),
    card(isChinese.value ? '成功' : 'Success', parseBatchDetail.value.successRecords),
    card(isChinese.value ? '部分成功' : 'Partial success', parseBatchDetail.value.partialSuccessRecords),
    card(isChinese.value ? '失败' : 'Failed', parseBatchDetail.value.failedRecords),
    card(isChinese.value ? 'Structure 成功率' : 'Structure rate', formatNumber(parseBatchDetail.value.structureParseSuccessRate)),
    card(isChinese.value ? 'Access 成功率' : 'Access rate', formatNumber(parseBatchDetail.value.accessParseSuccessRate))
  ].filter(item => hasDisplayValue(item.value))
})

const reportBatchStatusCards = computed(() => {
  if (!reportBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '导入状态' : 'Import status', reportBatchDetail.value.status),
    card(isChinese.value ? '报表总数' : 'Total reports', reportBatchDetail.value.totalReports),
    card(isChinese.value ? '已解析 SQL' : 'Resolved reports', reportBatchDetail.value.resolvedReports),
    card(isChinese.value ? '失败数' : 'Failed reports', reportBatchDetail.value.failedReports),
    card(isChinese.value ? '阶段' : 'Stage', reportBatchDetail.value.stage),
    card(isChinese.value ? '优先级' : 'Priority', reportBatchDetail.value.priority)
  ].filter(item => hasDisplayValue(item.value))
})

const parseFailureRecords = computed(() => {
  const detail = parseBatchDetail.value || {}
  const source = detail.failureRecords || detail.failedItems || detail.recordResults || detail.records || []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})

const reportItems = computed(() => {
  const detail = reportBatchDetail.value || {}
  const source = detail.reportItems || detail.items || detail.reports || detail.records || []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})

const templateColumns = computed(() => parseBatchDetail.value?.templateColumns || [])
const parseTemplatePreview = computed(() => buildTemplatePreview(templateColumns.value))
const directSqlPreview = computed(() => {
  if (parseBatchForm.directInputMode !== 'SQL_LINES') {
    return []
  }
  return parseBatchForm.rawContent
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
    .map((sqlText, index) => ({
      reportCode: `INLINE_${String(index + 1).padStart(3, '0')}`,
      datasource: parseBatchForm.datasourceCode,
      sqlText
    }))
})

const parseSessionsSummary = computed(() => `${parseBatchSessions.value.length} ${isChinese.value ? '个会话' : 'sessions'}`)
const reportSessionsSummary = computed(() => `${reportBatchSessions.value.length} ${isChinese.value ? '个批次' : 'batches'}`)

const overviewCards = computed(() => {
  if (!overview.value) {
    return []
  }
  return [
    card(isChinese.value ? 'SQL 总数' : 'Total SQL', overview.value.totalSqlCount),
    card(isChinese.value ? '问题 SQL' : 'Issue SQL', overview.value.issueSqlCount),
    card(isChinese.value ? '问题总数' : 'Total issues', overview.value.totalIssueCount),
    card(isChinese.value ? '问题场景数' : 'Issue scenes', overview.value.issueSceneCount),
    card(isChinese.value ? 'Important SQL' : 'Important SQL', overview.value.importantSqlCount),
    card(isChinese.value ? 'Urgent SQL' : 'Urgent SQL', overview.value.urgentSqlCount)
  ]
})

const priorityDistribution = computed(() => Object.entries(overview.value?.priorityDistribution || {}))

const historyRows = computed(() => historyPage.value?.items || [])
const historyClassificationSummary = computed(() => historyPage.value?.classificationSummary || {})
const pageSummaryCards = computed(() => [
  card(isChinese.value ? '当前页记录' : 'Current page', historyRows.value.length),
  card(isChinese.value ? '成功' : 'Success', historyClassificationSummary.value.SUCCESS || historyClassificationSummary.value.succeeded || 0),
  card(isChinese.value ? '异常/部分成功' : 'Non-success', historyClassificationSummary.value.PARTIAL || historyClassificationSummary.value.FAILED || historyClassificationSummary.value.nonSuccess || 0),
  card(isChinese.value ? '接入渠道' : 'Access channels', Object.keys(historyClassificationSummary.value.accessChannelCounts || {}).length)
])

const detailSummaryCards = computed(() => {
  if (!selectedHistoryDetail.value) {
    return []
  }
  return [
    card('History ID', selectedHistoryDetail.value.historyId),
    card('Trace ID', selectedHistoryDetail.value.traceId),
    card(isChinese.value ? '报表编码' : 'Report code', selectedHistoryDetail.value.reportCode),
    card(isChinese.value ? '服务编码' : 'Service code', selectedHistoryDetail.value.traceDetail?.serviceCode || selectedHistoryDetail.value.historyType),
    card(isChinese.value ? '结果状态' : 'Result status', selectedHistoryDetail.value.resultStatus),
    card(isChinese.value ? '目标引擎' : 'Target engine', selectedHistoryDetail.value.targetEngine),
    card(isChinese.value ? '接入渠道' : 'Access channel', selectedHistoryDetail.value.accessChannel),
    card(isChinese.value ? '提交时间' : 'Submitted at', formatTimestamp(selectedHistoryDetail.value.submittedAt))
  ]
})

const traceSummaryCards = computed(() => {
  const traceDetail = selectedHistoryDetail.value?.traceDetail
  if (!traceDetail) {
    return []
  }
  return [
    card(isChinese.value ? '最新状态' : 'Latest status', traceDetail.latestStatus),
    card(isChinese.value ? '审计事件数' : 'Audit event count', traceDetail.auditEventCount),
    card(isChinese.value ? '异常事件数' : 'Non-success events', traceDetail.nonSuccessEventCount),
    card(isChinese.value ? '关联历史数' : 'Query history count', traceDetail.queryHistoryCount)
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

const historyLogicalObjectHits = computed(() => normalizeArray(selectedHistoryDetail.value?.logicalObjectHits))
const traceQueryHistories = computed(() => selectedHistoryDetail.value?.traceDetail?.queryHistories || [])
const auditEvents = computed(() => selectedHistoryDetail.value?.traceDetail?.auditEvents || [])
const hasLookupCriteria = computed(() =>
  hasDisplayValue(historyForm.traceId) || hasDisplayValue(historyForm.taskId) || hasDisplayValue(historyForm.reportId)
)

const card = (label, value) => ({ label, value })

function hasDisplayValue(value) {
  return !(value === null || value === undefined || String(value).trim() === '')
}

function displayValue(value) {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

function booleanLabel(value) {
  if (typeof value !== 'boolean') {
    return ''
  }
  return value ? 'true' : 'false'
}

function formatNumber(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${Number(value).toFixed(2)}`
}

function formatRate(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${(Number(value) * 100).toFixed(1)}%`
}

function formatTimestamp(value) {
  if (!value) {
    return '-'
  }
  const numeric = Number(value)
  if (!Number.isNaN(numeric) && numeric > 0) {
    return new Date(numeric).toLocaleString(isChinese.value ? 'zh-CN' : 'en-US')
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function formatInstant(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').replace('Z', ' UTC')
}

function formatJson(value) {
  return JSON.stringify(value, null, 2)
}

function normalizeArray(value) {
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

function isNonEmpty(value) {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).length > 0
  }
  return hasDisplayValue(value)
}

function statusClass(value) {
  const normalized = String(value || '').toUpperCase()
  if (normalized === 'SUCCESS' || normalized === 'SUCCEEDED') {
    return 'pill pill-success'
  }
  if (normalized.includes('PARTIAL')) {
    return 'pill pill-warning'
  }
  return 'pill pill-danger'
}

function parseJsonInput(rawValue, label) {
  const trimmed = String(rawValue || '').trim()
  if (!trimmed) {
    return {}
  }
  let parsed
  try {
    parsed = JSON.parse(trimmed)
  } catch (error) {
    throw new Error(
      isChinese.value
        ? `${label} 需要是合法 JSON：${error.message}`
        : `${label} must be valid JSON: ${error.message}`
    )
  }
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error(isChinese.value ? `${label} 必须是 JSON 对象。` : `${label} must be a JSON object.`)
  }
  return parsed
}

function buildRequestPayload() {
  const payload = {
    tenantId: String(form.tenantId || '').trim(),
    sqlText: String(form.sqlText || '').trim(),
    datasourceCode: String(form.datasourceCode || '').trim(),
    bindingMode: String(form.bindingMode || '').trim(),
    connectionRequired: Boolean(form.connectionRequired)
  }
  const sqlTemplateText = String(form.sqlTemplateText || '').trim()
  if (sqlTemplateText) {
    payload.sqlTemplateText = sqlTemplateText
  }
  const bindParameters = parseJsonInput(form.bindParametersText, isChinese.value ? '绑定参数' : 'Bind parameters')
  if (Object.keys(bindParameters).length > 0) {
    payload.bindParameters = bindParameters
  }
  const commentContext = parseJsonInput(form.commentContextText, isChinese.value ? '注释上下文' : 'Comment context')
  if (Object.keys(commentContext).length > 0) {
    payload.commentContext = commentContext
  }
  return payload
}

function normalizeStructureResult(structureOnlyResult) {
  return {
    parseTaskId: structureOnlyResult.parseTaskId,
    status: 'STRUCTURE_ONLY',
    structureParse: structureOnlyResult,
    accessParse: null,
    degradeReason: 'STRUCTURE_ONLY_MODE',
    conclusion: {
      overallStatus: 'STRUCTURE_ONLY',
      summary: isChinese.value
        ? '当前结果只包含结构解析证据，未触发 access parse。'
        : 'This result contains structure-parse evidence only and did not trigger access parse.',
      recommendedAction: isChinese.value
        ? '如需对象可达性与连接状态，请开启 access parse 后重新执行综合解析。'
        : 'Enable access parse and rerun the combined flow when reachability and connection evidence are required.',
      structureAvailable: true,
      accessAvailable: false,
      degradeReason: 'STRUCTURE_ONLY_MODE'
    },
    statusHistory: [
      {
        status: 'STRUCTURE_SUCCEEDED',
        note: isChinese.value ? '结构解析同步返回。' : 'Structure parse returned synchronously.',
        occurredAtEpochMs: Date.now()
      }
    ]
  }
}

function resetResult() {
  parseResult.value = null
  errorMessage.value = ''
}

function openEvidenceDrawer(title, payload) {
  evidenceDrawerTitle.value = title
  evidenceDrawerPayload.value = payload
  evidenceDrawerVisible.value = true
}

async function runStructurePreview() {
  running.value = true
  lastRunMode.value = 'structure'
  errorMessage.value = ''
  try {
    const payload = buildRequestPayload()
    const structureOnlyResult = await parseStructureSql(payload, {
      requestPrefix: 'frontend-parse-workbench-structure'
    })
    parseResult.value = normalizeStructureResult(structureOnlyResult)
  } catch (error) {
    parseResult.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

async function runCombinedParseFlow() {
  running.value = true
  lastRunMode.value = 'combined'
  errorMessage.value = ''
  try {
    const payload = buildRequestPayload()
    const initialResult = await submitCombinedParse(payload, {
      requestPrefix: 'frontend-parse-workbench-submit'
    })
    parseResult.value = initialResult
    if (!combinedTerminalStatuses.has(initialResult.status)) {
      parseResult.value = await waitForCombinedParse(initialResult.parseTaskId, payload.tenantId, {
        requestPrefix: 'frontend-parse-workbench-terminal'
      })
    }
    await Promise.allSettled([loadAnalytics(), loadHistoryPage()])
  } catch (error) {
    parseResult.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

async function refreshParseStatus() {
  const parseTaskId = parseResult.value?.parseTaskId
  if (!parseTaskId || lastRunMode.value !== 'combined') {
    return
  }
  running.value = true
  errorMessage.value = ''
  try {
    parseResult.value = await getCombinedParseStatus(parseTaskId, form.tenantId, {
      requestPrefix: 'frontend-parse-workbench-refresh'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

function clearBatchError() {
  errorMessage.value = ''
}

function upsertSession(collection, item) {
  const next = collection.value.filter(entry => entry.batchId !== item.batchId)
  collection.value = [item, ...next]
}

function encodeArrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer)
  const chunkSize = 0x8000
  let binary = ''
  for (let index = 0; index < bytes.length; index += chunkSize) {
    const chunk = bytes.subarray(index, index + chunkSize)
    binary += String.fromCharCode(...chunk)
  }
  return window.btoa(binary)
}

function encodeTextToBase64(text) {
  const encoder = new TextEncoder()
  return encodeArrayBufferToBase64(encoder.encode(text).buffer)
}

function escapeCsvCell(value) {
  return `"${String(value || '').replace(/"/g, '""')}"`
}

function buildInlineSqlCsv() {
  const rows = directSqlPreview.value.map(item => [item.reportCode, item.datasource, item.sqlText].map(escapeCsvCell).join(','))
  return ['report_code,datasource,sql_text', ...rows].join('\n')
}

async function loadPayloadBase64(file, rawContent, emptyMessage) {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    const normalizedContent = parseBatchForm.directInputMode === 'SQL_LINES' ? buildInlineSqlCsv() : String(rawContent)
    return {
      fileName: parseBatchForm.directInputMode === 'SQL_LINES' ? 'inline-multi-sql.csv' : 'inline-upload.txt',
      contentBase64: encodeTextToBase64(normalizedContent)
    }
  }
  throw new Error(emptyMessage)
}

async function loadReportPayloadBase64(file, rawContent, emptyMessage) {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    return {
      fileName: 'inline-report-import.txt',
      contentBase64: encodeTextToBase64(String(rawContent))
    }
  }
  throw new Error(emptyMessage)
}

function downloadTextFile(fileName, content) {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(link.href)
}

function buildTemplatePreview(columns) {
  const headers = columns.map(item => item.columnKey).join(',')
  const placeholders = columns
    .map(item => {
      if (item.columnKey === 'sql_text') {
        return "SELECT * FROM orders WHERE dt = '2026-04-01'"
      }
      if (item.columnKey === 'datasource') {
        return 'hetu_main'
      }
      if (item.columnKey === 'report_code') {
        return 'RPT_SAMPLE'
      }
      if (item.columnKey === 'stage') {
        return 'PROD'
      }
      if (item.columnKey === 'priority') {
        return 'high'
      }
      return ''
    })
    .join(',')
  return `${headers}\n${placeholders}\n`
}

function handleParseFileChange(event) {
  const [file] = event.target.files || []
  parseUploadFile.value = file || null
}

function handleReportFileChange(event) {
  const [file] = event.target.files || []
  reportUploadFile.value = file || null
}

async function createParseBatchFlow() {
  loading.createParseBatch = true
  clearBatchError()
  try {
    parseBatchDetail.value = await createParseBatch({
      tenantId: parseBatchForm.tenantId,
      batchName: parseBatchForm.batchName,
      importMode: parseBatchForm.importMode,
      fileType: parseBatchForm.fileType,
      templateVersion: parseBatchForm.templateVersion,
      datasourceCode: parseBatchForm.datasourceCode,
      structureParseOnly: parseBatchForm.structureParseOnly
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    parseCreateDialogVisible.value = false
    parseDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.createParseBatch = false
  }
}

function downloadTemplate() {
  if (!templateColumns.value.length) {
    errorMessage.value = isChinese.value
      ? '先创建批次，拿到模板列契约后再下载模板。'
      : 'Create a batch first so the template-column contract can be downloaded.'
    return
  }
  downloadTextFile(`${parseBatchDetail.value.batchId || 'parse-batch-template'}.csv`, parseTemplatePreview.value)
}

async function ingestParseBatchFlow() {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value ? '请先创建 parse batch。' : 'Create a parse batch first.'
    return
  }
  loading.ingestParseBatch = true
  clearBatchError()
  try {
    const payload = await loadPayloadBase64(
      parseUploadFile.value,
      parseBatchForm.rawContent,
      isChinese.value ? '请上传文件或填写批量内容。' : 'Upload a file or provide inline batch content.'
    )
    parseBatchDetail.value = await ingestParseBatch(parseBatchDetail.value.batchId, parseBatchForm.tenantId, {
      ...payload,
      charset: 'UTF-8'
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    parseImportDialogVisible.value = false
    parseDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.ingestParseBatch = false
  }
}

async function refreshParseBatchDetail(batchId = '') {
  const targetBatchId = batchId || parseBatchDetail.value?.batchId
  if (!targetBatchId) {
    return
  }
  loading.refreshParseBatch = true
  clearBatchError()
  try {
    parseBatchDetail.value = await getParseBatch(targetBatchId, parseBatchForm.tenantId)
    upsertSession(parseBatchSessions, parseBatchDetail.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.refreshParseBatch = false
  }
}

async function retryAccessFlow() {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value ? '请先选择一个 parse batch。' : 'Select a parse batch first.'
    return
  }
  loading.retryParseBatch = true
  clearBatchError()
  try {
    parseBatchDetail.value = await retryParseBatchAccess(parseBatchDetail.value.batchId, parseBatchForm.tenantId, {
      failureFilter: retryForm.failureFilter,
      datasourceCode: retryForm.datasourceCode,
      forceRecheckAvailability: retryForm.forceRecheckAvailability
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    parseDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.retryParseBatch = false
  }
}

async function importReportBatchFlow() {
  loading.importReportBatch = true
  clearBatchError()
  try {
    const payload = await loadReportPayloadBase64(
      reportUploadFile.value,
      reportBatchForm.rawContent,
      isChinese.value ? '请上传报表清单文件或填写模拟内容。' : 'Upload a report catalog file or provide inline mock content.'
    )
    reportBatchDetail.value = await importReportBatch({
      tenantId: reportBatchForm.tenantId,
      batchName: reportBatchForm.batchName,
      fileType: reportBatchForm.fileType,
      reportCodeField: reportBatchForm.reportCodeField,
      datasourceCode: reportBatchForm.datasourceCode,
      stage: reportBatchForm.stage,
      priority: reportBatchForm.priority,
      ...payload,
      charset: 'UTF-8'
    })
    upsertSession(reportBatchSessions, reportBatchDetail.value)
    reportImportDialogVisible.value = false
    reportDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.importReportBatch = false
  }
}

async function refreshReportBatchDetail(batchId = '') {
  const targetBatchId = batchId || reportBatchDetail.value?.batchId
  if (!targetBatchId) {
    return
  }
  loading.refreshReportBatch = true
  clearBatchError()
  try {
    reportBatchDetail.value = await getReportBatch(targetBatchId, reportBatchForm.tenantId)
    upsertSession(reportBatchSessions, reportBatchDetail.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.refreshReportBatch = false
  }
}

async function resolveReportSqlsFlow() {
  if (!reportBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value ? '请先导入一个报表批次。' : 'Import a report batch first.'
    return
  }
  loading.resolveReportBatch = true
  clearBatchError()
  try {
    reportBatchDetail.value = await resolveReportBatchSqls(reportBatchDetail.value.batchId, reportBatchForm.tenantId)
    upsertSession(reportBatchSessions, reportBatchDetail.value)
    reportDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.resolveReportBatch = false
  }
}

async function openParseSession(batchId) {
  await refreshParseBatchDetail(batchId)
  parseDetailDrawerVisible.value = true
}

async function openReportSession(batchId) {
  await refreshReportBatchDetail(batchId)
  reportDetailDrawerVisible.value = true
}

async function loadAnalytics() {
  loading.analytics = true
  analyticsErrorMessage.value = ''
  try {
    const tenantId = historyForm.tenantId
    const [
      nextOverview,
      nextIssueScenes,
      nextSqlStats,
      nextReportStats,
      nextPriorityMatrix,
      nextImportantUrgent
    ] = await Promise.all([
      getParseStatisticsOverview(tenantId),
      getParseStatisticsByIssueScene(tenantId),
      getParseStatisticsBySql(tenantId),
      getParseStatisticsByReport(tenantId),
      getParseStatisticsPriorityMatrix(tenantId),
      getParseStatisticsImportantUrgent(tenantId)
    ])
    overview.value = nextOverview
    issueScenes.value = Array.isArray(nextIssueScenes) ? nextIssueScenes : []
    sqlStats.value = Array.isArray(nextSqlStats) ? nextSqlStats : []
    reportStats.value = Array.isArray(nextReportStats) ? nextReportStats : []
    priorityMatrix.value = Array.isArray(nextPriorityMatrix) ? nextPriorityMatrix : []
    importantUrgent.value = Array.isArray(nextImportantUrgent) ? nextImportantUrgent : []
  } catch (error) {
    analyticsErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.analytics = false
  }
}

function openStatisticsDetail(title, payload) {
  statisticsDetailTitle.value = title
  statisticsDetailPayload.value = payload
  statisticsDetailDialogVisible.value = true
}

async function loadHistoryPage() {
  loading.historyPage = true
  historyErrorMessage.value = ''
  try {
    historyPage.value = await getGovernanceQueryHistoryPage(
      {
        tenantId: historyForm.tenantId,
        reportCode: historyForm.reportCode,
        accessChannel: historyForm.accessChannel,
        status: historyForm.status,
        engine: historyForm.engine,
        submittedStart: historyForm.submittedStart,
        submittedEnd: historyForm.submittedEnd,
        sortBy: 'submittedAt',
        sortOrder: 'DESC',
        pageNo: 1,
        pageSize: 10
      },
      {
        requestPrefix: 'frontend-parse-record-history-page'
      }
    )
  } catch (error) {
    historyPage.value = null
    historyErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.historyPage = false
  }
}

async function openHistoryDetail(historyId, preloadedTraceDetail = null) {
  if (!historyId) {
    return
  }
  loading.historyDetail = true
  historyErrorMessage.value = ''
  activeHistoryDialogTab.value = 'overview'
  selectedHistoryId.value = historyId
  try {
    const detail = await getGovernanceQueryHistoryDetail(historyForm.tenantId, historyId, {
      requestPrefix: 'frontend-parse-record-query-history-detail'
    })
    if (preloadedTraceDetail && !detail.traceDetail) {
      detail.traceDetail = preloadedTraceDetail
    }
    selectedHistoryDetail.value = detail
    historyDetailDialogVisible.value = true
  } catch (error) {
    selectedHistoryDetail.value = null
    historyErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.historyDetail = false
  }
}

async function runIndexedLookup() {
  if (!hasLookupCriteria.value) {
    historyErrorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项。'
      : 'Enter at least one of traceId, taskId, or reportId.'
    return
  }
  loading.historyLookup = true
  historyErrorMessage.value = ''
  try {
    const lookupPage = await lookupGovernanceTraces(
      historyForm.tenantId,
      {
        traceId: historyForm.traceId,
        taskId: historyForm.taskId,
        reportId: historyForm.reportId
      },
      5,
      {
        requestPrefix: 'frontend-parse-record-lookups'
      }
    )
    const firstTraceId = lookupPage?.items?.[0]?.traceId
    if (!firstTraceId) {
      historyErrorMessage.value = isChinese.value ? '没有命中记录。' : 'No history matched the lookup criteria.'
      return
    }
    const traceDetail = await getGovernanceTraceDetail(historyForm.tenantId, firstTraceId, 20, {
      requestPrefix: 'frontend-parse-record-trace-detail'
    })
    const firstHistoryId = traceDetail?.queryHistories?.[0]?.historyId
    if (!firstHistoryId) {
      historyErrorMessage.value = isChinese.value
        ? '命中了 trace，但没有可展示的 query history。'
        : 'A trace was found but no query-history detail is available.'
      return
    }
    await openHistoryDetail(firstHistoryId, traceDetail)
  } catch (error) {
    historyErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.historyLookup = false
  }
}

async function clearHistoryFilters() {
  historyForm.reportCode = ''
  historyForm.accessChannel = ''
  historyForm.status = ''
  historyForm.engine = ''
  historyForm.submittedStart = ''
  historyForm.submittedEnd = ''
  historyForm.traceId = ''
  historyForm.taskId = ''
  historyForm.reportId = ''
  await loadHistoryPage()
}

function openRepairEvidence() {
  if (!selectedHistoryDetail.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.repairEvidence,
    query: {
      tenantId: historyForm.tenantId,
      traceId: selectedHistoryDetail.value.traceId || '',
      reportId: selectedHistoryDetail.value.reportId || ''
    }
  })
}

function openAuditForensics() {
  if (!selectedHistoryDetail.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.auditForensics,
    query: {
      tenantId: historyForm.tenantId,
      traceId: selectedHistoryDetail.value.traceId || '',
      reportId: selectedHistoryDetail.value.reportId || ''
    }
  })
}

function applyRouteWorkspace() {
  const workspace = String(route.query.workspace || '').trim()
  if (workspace === 'batch') {
    batchDialogVisible.value = true
  }
  if (workspace === 'statistics') {
    activeAnalyticsTab.value = String(route.query.analytics || 'issue')
  }
  if (workspace === 'history') {
    activeAnalyticsTab.value = 'history'
  }
}

watch(
  () => route.query,
  () => {
    applyRouteWorkspace()
  }
)

onMounted(async () => {
  historyForm.tenantId = form.tenantId
  parseBatchForm.tenantId = form.tenantId
  reportBatchForm.tenantId = form.tenantId
  retryForm.datasourceCode = form.datasourceCode
  parseBatchForm.datasourceCode = form.datasourceCode
  reportBatchForm.datasourceCode = form.datasourceCode
  applyRouteWorkspace()
  await Promise.all([loadAnalytics(), loadHistoryPage()])
})

watch(
  () => form.tenantId,
  value => {
    historyForm.tenantId = value
    parseBatchForm.tenantId = value
    reportBatchForm.tenantId = value
  }
)

watch(
  () => form.datasourceCode,
  value => {
    retryForm.datasourceCode = value
    parseBatchForm.datasourceCode = value
    reportBatchForm.datasourceCode = value
  }
)
</script>

<template>
  <section class="runtime-page parse-workbench-page" data-testid="parse-workbench-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">sql optimization parse workbench</p>
        <h1 class="runtime-title">{{ t('acceleration.title') }}</h1>
        <p class="runtime-summary">{{ t('acceleration.summary') }}</p>
      </div>
      <div class="hero-side">
        <div class="summary-chip-row">
          <span v-for="item in parseWorkbenchSummary" :key="item.label" class="summary-chip">
            {{ item.label }}: <strong>{{ item.value }}</strong>
          </span>
        </div>
        <div class="action-row action-row-wrap">
          <el-button type="primary" @click="batchDialogVisible = true">
            {{ isChinese ? '打开批量解析' : 'Open batch parsing' }}
          </el-button>
          <el-button @click="activeAnalyticsTab = 'history'">
            {{ isChinese ? '跳到解析历史' : 'Jump to history' }}
          </el-button>
          <el-button @click="loadAnalytics">
            {{ isChinese ? '刷新统计视角' : 'Refresh statistics' }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="parse-workbench__grid">
      <article class="surface-card composer-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">single sql input</p>
            <h2 class="section-title">{{ isChinese ? '单条解析主工作区' : 'Single SQL parsing workspace' }}</h2>
          </div>
        </div>

        <div class="summary-chip-row">
          <span v-for="item in requestSummary" :key="item.label" class="summary-chip">
            {{ item.label }}: <strong>{{ item.value }}</strong>
          </span>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
            <el-input v-model="form.tenantId" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '数据源编码' : 'Datasource code' }}</span>
            <el-input
              v-model="form.datasourceCode"
              :placeholder="isChinese ? 'hetu_main / 留空触发 partial-success' : 'hetu_main / leave blank to trigger partial-success'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '绑定模式' : 'Binding mode' }}</span>
            <el-select v-model="form.bindingMode">
              <el-option v-for="option in bindingModeOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>

          <label class="field-block field-block-toggle">
            <span class="field-label">{{ isChinese ? '执行 access parse' : 'Run access parse' }}</span>
            <el-switch v-model="form.connectionRequired" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">SQL</span>
            <el-input v-model="form.sqlText" type="textarea" :rows="8" data-testid="parse-workbench-sql-input" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '模板 SQL' : 'Template SQL' }}</span>
            <el-input v-model="form.sqlTemplateText" type="textarea" :rows="4" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '绑定参数 JSON' : 'Bind parameters JSON' }}</span>
            <el-input v-model="form.bindParametersText" type="textarea" :rows="5" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '注释上下文 JSON' : 'Comment context JSON' }}</span>
            <el-input v-model="form.commentContextText" type="textarea" :rows="6" />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="running && lastRunMode === 'combined'"
            data-testid="parse-workbench-submit"
            @click="runCombinedParseFlow"
          >
            {{ isChinese ? '执行综合解析' : 'Run combined parse' }}
          </el-button>
          <el-button
            :loading="running && lastRunMode === 'structure'"
            data-testid="parse-workbench-structure-preview"
            @click="runStructurePreview"
          >
            {{ isChinese ? '仅结构解析' : 'Structure-only preview' }}
          </el-button>
          <el-button
            :disabled="!parseResult?.parseTaskId || lastRunMode !== 'combined'"
            :loading="running && lastRunMode === 'combined'"
            data-testid="parse-workbench-refresh-status"
            @click="refreshParseStatus"
          >
            {{ isChinese ? '刷新状态' : 'Refresh status' }}
          </el-button>
          <el-button @click="batchDialogVisible = true">
            {{ isChinese ? '批量解析弹窗' : 'Batch parsing dialog' }}
          </el-button>
          <el-button @click="resetResult">
            {{ isChinese ? '清空结果' : 'Reset result' }}
          </el-button>
        </div>

        <div class="hint-card">
          <strong>{{ isChinese ? '调试提示' : 'Quick tip' }}</strong>
          <p>
            {{
              isChinese
                ? '把 datasourceCode 留空可以直接看到 structure-success + access-unavailable 的 partial-success 结论；填成包含 fail / unavailable 的编码可分别触发连接失败或服务不可用。'
                : 'Leave datasourceCode blank to surface a structure-success plus access-unavailable partial-success result; include fail or unavailable in the datasource code to trigger connection-failed or service-unavailable paths.'
            }}
          </p>
        </div>
      </article>

      <article class="surface-card evidence-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">combined conclusion</p>
            <h2 class="section-title">{{ isChinese ? '综合结论与双卡结果' : 'Combined conclusion and dual cards' }}</h2>
          </div>
          <el-button v-if="parseResult" text @click="openEvidenceDrawer(isChinese ? '解析原始证据' : 'Raw parse evidence', parseResult)">
            {{ isChinese ? '查看长证据' : 'View long evidence' }}
          </el-button>
        </div>

        <p v-if="!parseResult && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '左侧输入一条 SQL 后可先做结构解析，也可直接执行 combined parse 查看 structure/access 双卡和综合状态机。'
              : 'Enter one SQL statement on the left to run a structure-only preview or a combined parse with structure/access dual cards and the aggregated state machine.'
          }}
        </p>

        <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="parse-workbench-error">
          {{ errorMessage }}
        </div>

        <template v-if="parseResult">
          <div class="result-banner" :class="terminalStatuses.has(activeStatus) ? 'result-banner-success' : 'result-banner-warning'">
            <strong data-testid="parse-workbench-status">{{ activeStatus }}</strong>
            <span>{{ parseResult.parseTaskId }}</span>
          </div>

          <div class="summary-card-grid">
            <article v-for="item in summaryCards" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong :data-testid="item.label === (isChinese ? '综合状态' : 'Overall status') ? 'parse-workbench-overall-status' : undefined">
                {{ item.value }}
              </strong>
            </article>
          </div>

          <div v-if="activeConclusion" class="conclusion-card">
            <div class="conclusion-card__header">
              <span class="summary-card-label">{{ isChinese ? '综合结论' : 'Combined conclusion' }}</span>
              <strong data-testid="parse-workbench-overall-status">{{ activeConclusion.overallStatus }}</strong>
            </div>
            <p class="result-copy">{{ activeConclusion.summary }}</p>
            <p class="result-copy result-copy-muted">{{ activeConclusion.recommendedAction }}</p>
          </div>

          <div class="parse-card-grid">
            <article class="parse-card" data-testid="parse-workbench-structure-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">structure parse</p>
                  <h3 class="detail-title">{{ isChinese ? '结构解析卡' : 'Structure parse card' }}</h3>
                </div>
                <span class="status-pill" :class="structureParse?.syntaxStatus === 'VALID' ? 'status-pill-success' : 'status-pill-warning'">
                  {{ structureParse?.syntaxStatus || '-' }}
                </span>
              </div>

              <p v-if="!structureParse" class="empty-state">
                {{ isChinese ? '还没有结构解析结果。' : 'No structure-parse result yet.' }}
              </p>

              <template v-else>
                <div class="highlight-grid">
                  <div v-for="item in structureHighlights" :key="item.key" class="highlight-chip">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '查询日期摘要' : 'Query-date summary' }}</span>
                  <div class="summary-chip-row">
                    <span class="summary-chip">
                      {{ isChinese ? '起点' : 'Start' }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateStart || '-' }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ isChinese ? '终点' : 'End' }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateEnd || '-' }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ isChinese ? '状态' : 'Status' }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateStatus || '-' }}</strong>
                    </span>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '逻辑对象命中' : 'Logical object hits' }}</span>
                  <div v-if="logicalObjectHits.length" class="pill-grid">
                    <span
                      v-for="(item, index) in logicalObjectHits"
                      :key="`${item.objectKey || item.objectName || 'object'}-${index}`"
                      class="summary-chip"
                      data-testid="parse-workbench-logical-hit"
                    >
                      {{ item.objectType }}: <strong>{{ item.objectKey || item.objectName }}</strong>
                    </span>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '未命中逻辑对象。' : 'No logical objects were resolved.' }}
                  </p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '风险标签 / 改写候选' : 'Risk tags / rewrite candidates' }}</span>
                  <div class="pill-grid">
                    <span v-for="item in structureParse.riskTags || []" :key="`risk-${item}`" class="summary-chip summary-chip-warning">
                      {{ item }}
                    </span>
                    <span v-for="item in structureParse.rewriteCandidates || []" :key="`candidate-${item}`" class="summary-chip summary-chip-success">
                      {{ item }}
                    </span>
                  </div>
                </div>

                <div class="issue-list">
                  <article
                    v-for="(issue, index) in structureIssues"
                    :key="`${issue.issueCode || 'issue'}-${index}`"
                    class="issue-card"
                    data-testid="parse-workbench-issue"
                  >
                    <div class="issue-card__header">
                      <strong>{{ issue.issueCode }}</strong>
                      <span>{{ issue.severity }} · {{ issue.priorityLevel }}</span>
                    </div>
                    <p class="issue-card__summary">{{ issue.summary }}</p>
                    <p class="issue-card__detail">{{ issue.detail }}</p>
                    <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ issue.suggestedAction }}</p>
                  </article>
                </div>
              </template>
            </article>

            <article class="parse-card" data-testid="parse-workbench-access-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">access parse</p>
                  <h3 class="detail-title">{{ isChinese ? 'Access Parse 卡' : 'Access parse card' }}</h3>
                </div>
                <span
                  class="status-pill"
                  :class="accessParse?.serviceStatus === 'AVAILABLE' && accessParse?.connectionStatus === 'CONNECTED' ? 'status-pill-success' : 'status-pill-warning'"
                >
                  {{ accessParse?.serviceStatus || (isChinese ? '未执行' : 'Not run') }}
                </span>
              </div>

              <p v-if="!accessParse" class="empty-state">
                {{
                  isChinese
                    ? '结构解析模式不会生成 access parse 结果；综合解析会在结构成功后异步补跑。'
                    : 'Structure-only mode does not generate access-parse output; combined mode schedules it after structure success.'
                }}
              </p>

              <template v-else>
                <div class="highlight-grid">
                  <div v-for="item in accessHighlights" :key="item.key" class="highlight-chip">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? 'Plan Summary' : 'Plan summary' }}</span>
                  <p class="result-copy">{{ accessParse.planSummary || '-' }}</p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '可用性告警' : 'Availability warning' }}</span>
                  <p class="result-copy result-copy-muted">{{ accessParse.availabilityWarning || '-' }}</p>
                </div>
              </template>
            </article>
          </div>

          <div class="history-panel">
            <div class="parse-card__header">
              <div>
                <p class="section-kicker sqlforge-code-label">status history</p>
                <h3 class="detail-title">{{ isChinese ? '综合状态机追溯' : 'Combined state history' }}</h3>
              </div>
            </div>

            <div class="history-list">
              <article
                v-for="(entry, index) in statusHistory"
                :key="`${entry.status || 'status'}-${index}`"
                class="history-item"
                data-testid="parse-workbench-history-entry"
              >
                <strong>{{ entry.status }}</strong>
                <span>{{ formatTimestamp(entry.occurredAtEpochMs) }}</span>
                <p>{{ entry.note || '-' }}</p>
              </article>
            </div>
          </div>
        </template>
      </article>
    </div>

    <section class="surface-card analytics-shell" data-testid="statistics-page">
      <div class="shell-header">
        <div>
          <p class="section-kicker sqlforge-code-label">parse statistics center</p>
          <h2 class="section-title">{{ isChinese ? '解析统计与历史视角' : 'Parse statistics and history views' }}</h2>
          <p class="runtime-note">
            {{
              isChinese
                ? '下半区把 issue / important-urgent / by-report / by-sql 与解析历史合到同一主路由，长证据统一进入 dialog 或 drawer。'
                : 'The lower half keeps issue, important-or-urgent, by-report, by-SQL, and parse history under one route while long evidence moves into dialogs or drawers.'
            }}
          </p>
        </div>
        <div class="action-row action-row-wrap">
          <el-button :loading="loading.analytics" data-testid="statistics-refresh" @click="loadAnalytics">
            {{ isChinese ? '刷新统计' : 'Refresh statistics' }}
          </el-button>
          <el-button :loading="loading.historyPage" data-testid="parse-record-refresh" @click="loadHistoryPage">
            {{ isChinese ? '刷新历史' : 'Refresh history' }}
          </el-button>
        </div>
      </div>

      <div v-if="analyticsErrorMessage" class="inline-banner inline-banner-danger" data-testid="statistics-error">
        {{ analyticsErrorMessage }}
      </div>
      <div v-if="historyErrorMessage" class="inline-banner inline-banner-danger">
        {{ historyErrorMessage }}
      </div>

      <section class="summary-grid summary-grid-compact">
        <article v-for="item in overviewCards" :key="item.label" class="summary-card">
          <span class="summary-card-label">{{ item.label }}</span>
          <strong>{{ item.value ?? 0 }}</strong>
        </article>
      </section>

      <div class="analytics-grid">
        <aside class="analytics-rail">
          <section class="detail-card">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Parse overview</p>
                <h3 class="detail-title">{{ isChinese ? '解析总览' : 'Parse overview' }}</h3>
              </div>
            </div>
            <div class="distribution-list">
              <div v-for="[priority, count] in priorityDistribution" :key="priority" class="distribution-item">
                <span>{{ priority }}</span>
                <strong>{{ count }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-card">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Priority matrix</p>
                <h3 class="detail-title">{{ isChinese ? '优先级矩阵' : 'Priority matrix' }}</h3>
              </div>
            </div>
            <div class="matrix-list" data-testid="statistics-priority-matrix">
              <button
                v-for="item in priorityMatrix"
                :key="`${item.priorityLevel}-${item.urgencyBucket}`"
                type="button"
                class="matrix-item"
                @click="openStatisticsDetail(`${item.priorityLevel} / ${item.urgencyBucket}`, item)"
              >
                <strong>{{ item.priorityLevel }} / {{ item.urgencyBucket }}</strong>
                <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues</span>
                <span>{{ item.reportCount }} reports</span>
              </button>
            </div>
          </section>
        </aside>

        <section class="detail-stage">
          <el-tabs v-model="activeAnalyticsTab">
            <el-tab-pane :label="isChinese ? '问题分布' : 'Issue distribution'" name="issue">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">Issue distribution</p>
                  <h3 class="detail-title">{{ isChinese ? '问题分布' : 'Issue distribution' }}</h3>
                </div>
              </div>
              <el-table :data="issueScenes" border>
                <el-table-column prop="issueScene" :label="isChinese ? '问题场景' : 'Issue scene'" min-width="180">
                  <template #default="{ row }">
                    <button type="button" class="table-link" data-testid="statistics-issue-scene" @click="openStatisticsDetail(row.issueScene, row)">
                      {{ row.issueScene }}
                    </button>
                  </template>
                </el-table-column>
                <el-table-column prop="issueDomain" :label="isChinese ? '领域' : 'Domain'" min-width="120" />
                <el-table-column prop="severity" :label="isChinese ? '严重度' : 'Severity'" min-width="120" />
                <el-table-column prop="priorityLevel" :label="isChinese ? '优先级' : 'Priority'" min-width="120" />
                <el-table-column prop="affectedSqlCount" :label="isChinese ? '影响 SQL' : 'Affected SQL'" min-width="120" />
                <el-table-column prop="affectedIssueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="100" />
                <el-table-column :label="isChinese ? '占比' : 'Ratio'" min-width="100">
                  <template #default="{ row }">{{ formatRate(row.sqlRatio) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '重要/紧急' : 'Important or urgent list'" name="important">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">Important or urgent list</p>
                  <h3 class="detail-title">{{ isChinese ? 'Important / Urgent 清单' : 'Important or urgent list' }}</h3>
                </div>
              </div>
              <el-table :data="importantUrgent" border>
                <el-table-column :label="isChinese ? '对象' : 'Item'" min-width="180">
                  <template #default="{ row }">
                    <button
                      type="button"
                      class="table-link"
                      data-testid="statistics-important-urgent"
                      @click="openStatisticsDetail(row.reportCode || row.itemId || row.parseTaskId || 'important', row)"
                    >
                      {{ row.reportCode || row.itemId || row.parseTaskId || '-' }}
                    </button>
                  </template>
                </el-table-column>
                <el-table-column :label="isChinese ? '最高优先级' : 'Highest priority'" min-width="150">
                  <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
                </el-table-column>
                <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="140" />
                <el-table-column prop="stage" :label="isChinese ? '阶段' : 'Stage'" min-width="110" />
                <el-table-column :label="isChinese ? '问题场景' : 'Issue scenes'" min-width="220">
                  <template #default="{ row }">{{ row.issueScenes?.join(', ') || '-' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '报表视角' : 'By report'" name="report">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">By report</p>
                  <h3 class="detail-title">{{ isChinese ? '报表视角' : 'By report' }}</h3>
                </div>
              </div>
              <el-table :data="reportStats" border>
                <el-table-column prop="reportCode" :label="isChinese ? '报表编码' : 'Report code'" min-width="180">
                  <template #default="{ row }">
                    <button type="button" class="table-link" @click="openStatisticsDetail(row.reportCode, row)">
                      {{ row.reportCode }}
                    </button>
                  </template>
                </el-table-column>
                <el-table-column prop="sqlCount" :label="isChinese ? 'SQL 数' : 'SQL count'" min-width="120" />
                <el-table-column prop="issueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="120" />
                <el-table-column :label="isChinese ? '最高优先级' : 'Highest priority'" min-width="150">
                  <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
                </el-table-column>
                <el-table-column :label="isChinese ? '重要 / 紧急' : 'Important / urgent'" min-width="130">
                  <template #default="{ row }">{{ booleanLabel(row.important) }} / {{ booleanLabel(row.urgent) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? 'SQL 清单' : 'By SQL'" name="sql">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">By SQL</p>
                  <h3 class="detail-title">{{ isChinese ? 'SQL 清单' : 'By SQL' }}</h3>
                </div>
              </div>
              <el-table :data="sqlStats" border>
                <el-table-column :label="isChinese ? '对象' : 'Item'" min-width="180">
                  <template #default="{ row }">
                    <button type="button" class="table-link" @click="openStatisticsDetail(row.reportCode || row.itemId || row.parseTaskId || 'sql', row)">
                      {{ row.reportCode || row.itemId || row.parseTaskId || '-' }}
                    </button>
                  </template>
                </el-table-column>
                <el-table-column :label="isChinese ? '优先级' : 'Priority'" min-width="150">
                  <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
                </el-table-column>
                <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="140" />
                <el-table-column prop="stage" :label="isChinese ? '阶段' : 'Stage'" min-width="110" />
                <el-table-column prop="issueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="110" />
                <el-table-column :label="isChinese ? '问题场景' : 'Issue scenes'" min-width="220">
                  <template #default="{ row }">{{ row.issueScenes?.join(', ') || '-' }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '解析历史' : 'Parse history'" name="history">
              <div class="history-stack">
                <div class="table-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">query history workbench</p>
                    <h3 class="detail-title">{{ isChinese ? '解析历史' : 'Parse history' }}</h3>
                  </div>
                  <div class="action-row action-row-wrap">
                    <el-button :loading="loading.historyLookup" data-testid="parse-record-run-lookup" @click="runIndexedLookup">
                      {{ isChinese ? '精确反查' : 'Indexed lookup' }}
                    </el-button>
                    <el-button @click="clearHistoryFilters">{{ isChinese ? '清空条件' : 'Clear filters' }}</el-button>
                  </div>
                </div>

                <div class="filter-grid">
                  <label class="field-block">
                    <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
                    <el-input v-model="historyForm.tenantId" />
                  </label>
                  <label class="field-block">
                    <span class="field-label">{{ isChinese ? '报表编码' : 'Report code' }}</span>
                    <el-input v-model="historyForm.reportCode" />
                  </label>
                  <label class="field-block">
                    <span class="field-label">{{ isChinese ? '结果状态' : 'Status' }}</span>
                    <el-select v-model="historyForm.status" data-testid="parse-record-filter-select">
                      <el-option label="ALL" value="" />
                      <el-option label="SUCCESS" value="SUCCESS" />
                      <el-option label="PARTIAL" value="PARTIAL" />
                      <el-option label="FAILED" value="FAILED" />
                    </el-select>
                  </label>
                  <label class="field-block">
                    <span class="field-label">{{ isChinese ? '接入渠道' : 'Access channel' }}</span>
                    <el-select v-model="historyForm.accessChannel" data-testid="parse-record-status-filter">
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
                    <el-select v-model="historyForm.engine" data-testid="parse-record-sort-select">
                      <el-option label="ALL" value="" />
                      <el-option label="HETU" value="HETU" />
                      <el-option label="HIVE" value="HIVE" />
                    </el-select>
                  </label>
                  <label class="field-block">
                    <span class="field-label">{{ isChinese ? '提交起点' : 'Submitted start' }}</span>
                    <el-input v-model="historyForm.submittedStart" placeholder="2026-04-25T00:00:00" />
                  </label>
                  <label class="field-block">
                    <span class="field-label">{{ isChinese ? '提交终点' : 'Submitted end' }}</span>
                    <el-input v-model="historyForm.submittedEnd" placeholder="2026-04-27T23:59:59" />
                  </label>
                  <label class="field-block">
                    <span class="field-label">Trace ID</span>
                    <el-input v-model="historyForm.traceId" />
                  </label>
                  <label class="field-block">
                    <span class="field-label">Task ID</span>
                    <el-input v-model="historyForm.taskId" />
                  </label>
                  <label class="field-block">
                    <span class="field-label">Report ID</span>
                    <el-input v-model="historyForm.reportId" />
                  </label>
                </div>

                <div class="summary-chip-row">
                  <span class="summary-chip">{{ isChinese ? 'History classification' : 'History classification' }}</span>
                  <span class="summary-chip">{{ isChinese ? 'Sort mode' : 'Sort mode' }}: submittedAt DESC</span>
                  <span class="summary-chip" data-testid="parse-record-page-mode">{{ hasLookupCriteria ? 'INDEXED' : 'PAGE' }}</span>
                </div>

                <section class="summary-grid summary-grid-compact">
                  <article v-for="item in pageSummaryCards" :key="item.label" class="summary-card">
                    <span class="summary-card-label">{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </article>
                </section>

                <el-table :data="historyRows" border>
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
                  <el-table-column :label="isChinese ? '服务编码' : 'Service code'" min-width="150">
                    <template #default="{ row }">{{ row.historyType || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="resultStatus" :label="isChinese ? '状态' : 'Status'" min-width="120">
                    <template #default="{ row }">
                      <span :class="statusClass(row.resultStatus)">{{ row.resultStatus || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="accessChannel" :label="isChinese ? '接入渠道' : 'Access channel'" min-width="130" />
                  <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="120" />
                  <el-table-column prop="submittedAt" :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
                    <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
                  </el-table-column>
                  <el-table-column :label="isChinese ? '审计事件数' : 'Audit event count'" min-width="120">
                    <template #default="{ row }">{{ row.auditEventCount ?? '-' }}</template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </section>

    <el-dialog v-model="batchDialogVisible" :title="isChinese ? '批量解析 Dialog' : 'Batch parsing dialog'" width="1240px" top="4vh">
      <section class="batch-dialog-shell" data-testid="batch-import-page">
        <header class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">template download + upload</p>
            <h2 class="section-title">{{ isChinese ? '创建批次、导入内容与查看结果' : 'Create batches, ingest content, and inspect results' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '批量解析不再占独立主导航；创建批次、导入内容、失败重试和报表导入都收进同一弹窗工作区。'
                  : 'Batch parsing no longer occupies a separate navigation entry; batch creation, content import, failure retries, and report import stay in one dialog workspace.'
              }}
            </p>
          </div>
          <div class="hero-inline">
            <span class="hero-pill">{{ parseSessionsSummary }}</span>
            <span class="hero-pill">{{ reportSessionsSummary }}</span>
            <span class="hero-pill hero-pill-muted">{{ isChinese ? '查询条件 + 结果区 + 抽屉' : 'Filters + results + drawers' }}</span>
          </div>
        </header>

        <el-tabs v-model="activeBatchWorkspace" class="workspace-tabs">
          <el-tab-pane :label="isChinese ? '批量解析' : 'Parse batches'" name="parse">
            <div class="workspace-toolbar shell-panel">
              <div class="toolbar-copy">
                <p class="section-kicker sqlforge-code-label">template download + upload</p>
                <h3 class="detail-title">{{ isChinese ? '批次创建、导入与补跑' : 'Batch creation, import, and recovery' }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button type="primary" data-testid="batch-import-create" @click="parseCreateDialogVisible = true">
                  {{ isChinese ? '创建批次' : 'Create batch' }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-ingest" @click="parseImportDialogVisible = true">
                  {{ isChinese ? '导入内容' : 'Ingest content' }}
                </el-button>
                <el-button :disabled="!templateColumns.length" data-testid="batch-import-download-template" @click="parseTemplateDialogVisible = true">
                  {{ isChinese ? '查看模板' : 'Preview template' }}
                </el-button>
                <el-button :loading="loading.refreshParseBatch" @click="refreshParseBatchDetail()">
                  {{ isChinese ? '刷新详情' : 'Refresh detail' }}
                </el-button>
              </div>
            </div>

            <div class="workspace-grid">
              <aside class="shell-panel session-rail">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">parse sessions</p>
                    <h3 class="detail-title">{{ isChinese ? '批次会话' : 'Batch sessions' }}</h3>
                  </div>
                </div>
                <div class="session-list">
                  <button
                    v-for="item in parseBatchSessions"
                    :key="item.batchId"
                    type="button"
                    class="session-item"
                    :class="{ 'session-item-active': parseBatchDetail?.batchId === item.batchId }"
                    data-testid="batch-import-parse-batch-item"
                    @click="openParseSession(item.batchId)"
                  >
                    <div class="session-item-top">
                      <strong>{{ item.batchName || item.batchId }}</strong>
                      <span class="status-pill">{{ item.status || 'CREATED' }}</span>
                    </div>
                    <p>{{ item.batchId }}</p>
                    <span>{{ formatInstant(item.createdAt || item.updatedAt) }}</span>
                  </button>
                  <div v-if="!parseBatchSessions.length" class="empty-state">
                    {{ isChinese ? '先创建一个 parse batch。' : 'Create a parse batch to start.' }}
                  </div>
                </div>
              </aside>

              <main class="shell-panel detail-stage">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">batch result</p>
                    <h3 class="detail-title">{{ isChinese ? '当前批次概览' : 'Current batch overview' }}</h3>
                  </div>
                  <div class="toolbar-actions">
                    <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-retry-access" @click="retryAccessFlow">
                      {{ isChinese ? '补跑 Access' : 'Retry access' }}
                    </el-button>
                    <el-button :disabled="!parseBatchDetail?.batchId" @click="parseDetailDrawerVisible = true">
                      {{ isChinese ? '打开详情抽屉' : 'Open detail drawer' }}
                    </el-button>
                  </div>
                </div>

                <div v-if="parseBatchDetail" class="summary-grid summary-grid-compact">
                  <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
                    <span class="summary-card-label">{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </article>
                </div>

                <div v-if="parseBatchDetail" class="result-layout">
                  <section class="detail-card">
                    <div class="section-heading">
                      <div>
                        <p class="section-kicker sqlforge-code-label">Template-column contract</p>
                        <h4 class="detail-title">{{ isChinese ? '模板列契约' : 'Template-column contract' }}</h4>
                      </div>
                    </div>
                    <div class="contract-list">
                      <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
                        <strong>{{ item.columnKey }}</strong>
                        <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
                      </div>
                      <div v-if="!templateColumns.length" class="empty-state">
                        {{ isChinese ? '创建批次后会返回模板列契约。' : 'Template-column contract arrives after batch creation.' }}
                      </div>
                    </div>
                  </section>

                  <section class="detail-card">
                    <div class="section-heading">
                      <div>
                        <p class="section-kicker sqlforge-code-label">Failure records</p>
                        <h4 class="detail-title">{{ isChinese ? '失败记录' : 'Failure records' }}</h4>
                      </div>
                    </div>
                    <div class="failure-list">
                      <article
                        v-for="(item, index) in parseFailureRecords.slice(0, 6)"
                        :key="item.recordId || item.id || index"
                        class="failure-item"
                        data-testid="batch-import-failure-record"
                      >
                        <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
                        <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
                        <p>{{ displayValue(item.sqlText || item.message || item.sqlPreview) }}</p>
                      </article>
                      <div v-if="!parseFailureRecords.length" class="empty-state">
                        {{ isChinese ? '当前没有失败记录。' : 'No failure records in the current batch.' }}
                      </div>
                    </div>
                  </section>
                </div>

                <div v-else class="empty-stage">
                  <strong>{{ isChinese ? '暂无 parse batch' : 'No parse batch selected' }}</strong>
                  <p>{{ isChinese ? '先创建批次，然后通过弹窗导入多条 SQL 或模板文件。' : 'Create a batch first, then import multi-SQL text or template files through dialogs.' }}</p>
                </div>
              </main>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '报表导入' : 'Report catalog import'" name="report">
            <div class="workspace-toolbar shell-panel">
              <div class="toolbar-copy">
                <p class="section-kicker sqlforge-code-label">report catalog import</p>
                <h3 class="detail-title">{{ isChinese ? '报表清单导入与 SQL 解析' : 'Report catalog import and SQL resolution' }}</h3>
                <p class="section-summary">{{ isChinese ? '二级明细继续走抽屉，不再跳到独立页面。' : 'Secondary detail stays inside drawers instead of opening a separate page.' }}</p>
              </div>
              <div class="toolbar-actions">
                <el-button type="primary" data-testid="batch-import-report-import" @click="reportImportDialogVisible = true">
                  {{ isChinese ? '导入报表批次' : 'Import report batch' }}
                </el-button>
                <el-button :loading="loading.refreshReportBatch" @click="refreshReportBatchDetail()">
                  {{ isChinese ? '刷新详情' : 'Refresh detail' }}
                </el-button>
                <el-button
                  :disabled="!reportBatchDetail?.batchId"
                  data-testid="batch-import-report-resolve"
                  @click="resolveReportSqlsFlow"
                >
                  {{ isChinese ? '解析报表 SQL' : 'Resolve report SQLs' }}
                </el-button>
              </div>
            </div>

            <div class="workspace-grid">
              <aside class="shell-panel session-rail">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">report sessions</p>
                    <h3 class="detail-title">{{ isChinese ? '报表批次' : 'Report batches' }}</h3>
                  </div>
                </div>
                <div class="session-list">
                  <button
                    v-for="item in reportBatchSessions"
                    :key="item.batchId"
                    type="button"
                    class="session-item"
                    :class="{ 'session-item-active': reportBatchDetail?.batchId === item.batchId }"
                    data-testid="batch-import-report-item"
                    @click="openReportSession(item.batchId)"
                  >
                    <div class="session-item-top">
                      <strong>{{ item.batchName || item.batchId }}</strong>
                      <span class="status-pill">{{ item.status || 'IMPORTED' }}</span>
                    </div>
                    <p>{{ item.batchId }}</p>
                    <span>{{ formatInstant(item.createdAt || item.updatedAt) }}</span>
                  </button>
                  <div v-if="!reportBatchSessions.length" class="empty-state">
                    {{ isChinese ? '先导入一个报表批次。' : 'Import a report batch to start.' }}
                  </div>
                </div>
              </aside>

              <main class="shell-panel detail-stage">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">report items</p>
                    <h3 class="detail-title">{{ isChinese ? '报表项概览' : 'Report items' }}</h3>
                  </div>
                  <el-button :disabled="!reportBatchDetail?.batchId" @click="reportDetailDrawerVisible = true">
                    {{ isChinese ? '打开详情抽屉' : 'Open detail drawer' }}
                  </el-button>
                </div>

                <div v-if="reportBatchDetail" class="summary-grid summary-grid-compact">
                  <article v-for="item in reportBatchStatusCards" :key="item.label" class="summary-card">
                    <span class="summary-card-label">{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </article>
                </div>

                <div v-if="reportBatchDetail" class="report-list">
                  <article v-for="(item, index) in reportItems.slice(0, 8)" :key="item.reportCode || item.itemId || index" class="report-item">
                    <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
                    <span>{{ displayValue(item.reportName || item.status) }}</span>
                    <p>{{ displayValue(item.datasourceCode || item.stage) }} · {{ displayValue(item.priority || item.resolutionStatus) }}</p>
                  </article>
                  <div v-if="!reportItems.length" class="empty-state">
                    {{ isChinese ? '导入后会在这里看到报表清单。' : 'Imported report items appear here.' }}
                  </div>
                </div>

                <div v-else class="empty-stage">
                  <strong>{{ isChinese ? '暂无 report batch' : 'No report batch selected' }}</strong>
                  <p>{{ isChinese ? '通过导入弹窗上传报表清单，再在详情抽屉里查看解析证据。' : 'Use the import dialog to upload the report catalog, then review evidence in the detail drawer.' }}</p>
                </div>
              </main>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </el-dialog>

    <el-dialog v-model="parseCreateDialogVisible" :title="isChinese ? '创建 Parse Batch' : 'Create parse batch'" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="parseBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '批次名称' : 'Batch name' }}</span>
          <el-input v-model="parseBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '导入模式' : 'Import mode' }}</span>
          <el-select v-model="parseBatchForm.importMode">
            <el-option v-for="option in parseImportModeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '文件类型' : 'File type' }}</span>
          <el-select v-model="parseBatchForm.fileType">
            <el-option v-for="option in parseFileTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
          <el-input v-model="parseBatchForm.datasourceCode" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">{{ isChinese ? '仅结构解析' : 'Structure-only' }}</span>
          <el-switch v-model="parseBatchForm.structureParseOnly" />
        </label>
      </div>
      <template #footer>
        <el-button @click="parseCreateDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.createParseBatch" @click="createParseBatchFlow">
          {{ isChinese ? '创建批次' : 'Create batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseImportDialogVisible" :title="isChinese ? '导入 Parse Batch 内容' : 'Ingest parse-batch content'" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '输入模式' : 'Input mode' }}</span>
          <el-select v-model="parseBatchForm.directInputMode">
            <el-option v-for="option in directInputModeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" @change="handleParseFileChange">
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '批量内容' : 'Batch content' }}</span>
          <el-input v-model="parseBatchForm.rawContent" type="textarea" :rows="8" />
        </label>
      </div>
      <template #footer>
        <el-button @click="parseImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.ingestParseBatch" @click="ingestParseBatchFlow">
          {{ isChinese ? '导入内容' : 'Ingest content' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseTemplateDialogVisible" :title="isChinese ? '模板预览' : 'Template preview'" width="780px">
      <div class="dialog-stack">
        <pre class="code-block">{{ parseTemplatePreview }}</pre>
      </div>
      <template #footer>
        <el-button @click="downloadTemplate">{{ isChinese ? '下载模板' : 'Download template' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportImportDialogVisible" :title="isChinese ? '导入报表批次' : 'Import report batch'" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="reportBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '批次名称' : 'Batch name' }}</span>
          <el-input v-model="reportBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '文件类型' : 'File type' }}</span>
          <el-select v-model="reportBatchForm.fileType">
            <el-option v-for="option in reportFileTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '报表编码字段' : 'Report code field' }}</span>
          <el-input v-model="reportBatchForm.reportCodeField" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '阶段' : 'Stage' }}</span>
          <el-input v-model="reportBatchForm.stage" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '优先级' : 'Priority' }}</span>
          <el-input v-model="reportBatchForm.priority" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" @change="handleReportFileChange">
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '清单内容' : 'Catalog content' }}</span>
          <el-input v-model="reportBatchForm.rawContent" type="textarea" :rows="7" />
        </label>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.importReportBatch" @click="importReportBatchFlow">
          {{ isChinese ? '导入批次' : 'Import batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statisticsDetailDialogVisible" :title="statisticsDetailTitle" width="760px">
      <div class="detail-grid">
        <div v-for="(value, key) in statisticsDetailPayload || {}" :key="key" class="detail-grid__item">
          <span>{{ key }}</span>
          <strong>{{ displayValue(value) }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="openEvidenceDrawer(statisticsDetailTitle, statisticsDetailPayload)">{{ isChinese ? '查看原始证据' : 'View raw evidence' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDetailDialogVisible" :title="selectedHistoryDetail?.reportCode || selectedHistoryDetail?.historyId || 'query history detail'" width="1120px">
      <div v-if="selectedHistoryDetail" class="dialog-stack">
        <div class="dialog-header">
          <div class="banner-row">
            <strong data-testid="parse-record-detail-trace-id">{{ selectedHistoryDetail.traceId || '-' }}</strong>
            <span :class="statusClass(selectedHistoryDetail.resultStatus)">{{ selectedHistoryDetail.resultStatus || '-' }}</span>
          </div>
          <div class="dialog-actions">
            <el-button type="primary" @click="openRepairEvidence">{{ isChinese ? '打开修复证据' : 'Open repair evidence' }}</el-button>
            <el-button @click="openAuditForensics">{{ isChinese ? '打开审计取证' : 'Open audit forensics' }}</el-button>
            <el-button @click="historyDetailDrawerVisible = true">{{ isChinese ? '查看原始证据' : 'View raw evidence' }}</el-button>
          </div>
        </div>

        <el-tabs v-model="activeHistoryDialogTab">
          <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
            <div class="detail-grid">
              <div v-for="item in detailSummaryCards" :key="item.label" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>
            <div v-if="traceSummaryCards.length" class="detail-grid detail-grid-secondary">
              <div v-for="item in traceSummaryCards" :key="item.label" class="detail-grid__item">
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
            <p v-if="!traceQueryHistories.length" class="empty-copy" data-testid="parse-record-detail-query-history-count">0</p>
            <p v-else class="empty-copy" data-testid="parse-record-detail-query-history-count">{{ traceQueryHistories.length }}</p>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? 'SQL 三态' : 'SQL tri-state'" name="sql">
            <div class="detail-grid">
              <div v-for="item in sqlStateHighlights" :key="item.key" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong :data-testid="`parse-record-history-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                  {{ displayValue(item.value) }}
                </strong>
              </div>
            </div>
            <div class="code-grid">
              <article v-for="item in sqlVariants" :key="item.key" class="code-card">
                <div class="code-card__header"><span>{{ item.label }}</span></div>
                <pre class="code-block" :data-testid="`parse-record-${item.key}`">{{ item.value }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '解析与路由' : 'Parse and route signals'" name="signals">
            <div class="code-grid">
              <article v-for="group in signalGroups" :key="group.key" class="code-card">
                <div class="code-card__header"><span>{{ group.title }}</span></div>
                <pre class="code-block" :data-testid="`parse-record-${group.key}`">{{ formatJson(group.payload) }}</pre>
              </article>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '关联证据' : 'References'" name="refs">
            <div v-if="historyLogicalObjectHits.length" class="detail-grid">
              <div
                v-for="(item, index) in historyLogicalObjectHits"
                :key="`${item.objectKey || item.logicalObjectKey || index}`"
                class="detail-grid__item"
              >
                <span>{{ item.objectType || item.logicalObjectType || 'OBJECT' }}</span>
                <strong>{{ item.objectKey || item.logicalObjectKey || item.objectName || '-' }}</strong>
              </div>
            </div>

            <div v-if="referenceGroups.length" class="code-grid">
              <article v-for="group in referenceGroups" :key="group.key" class="code-card">
                <div class="code-card__header"><span>{{ group.title }}</span></div>
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

    <el-drawer v-model="parseDetailDrawerVisible" :title="parseBatchDetail?.batchName || parseBatchDetail?.batchId || 'parse batch detail'" size="42%">
      <div class="drawer-stack" data-testid="batch-import-parse-detail">
        <div class="summary-grid summary-grid-compact">
          <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <pre class="code-block">{{ formatJson(parseBatchDetail || {}) }}</pre>
      </div>
    </el-drawer>

    <el-drawer v-model="reportDetailDrawerVisible" :title="reportBatchDetail?.batchName || reportBatchDetail?.batchId || 'report batch detail'" size="42%">
      <div class="drawer-stack">
        <div class="summary-grid summary-grid-compact">
          <article v-for="item in reportBatchStatusCards" :key="item.label" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <pre class="code-block">{{ formatJson(reportBatchDetail || {}) }}</pre>
      </div>
    </el-drawer>

    <el-drawer v-model="historyDetailDrawerVisible" :title="isChinese ? '原始证据' : 'Raw evidence'" size="44%">
      <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>

    <el-drawer v-model="evidenceDrawerVisible" :title="evidenceDrawerTitle || (isChinese ? '原始证据' : 'Raw evidence')" size="42%">
      <pre class="code-block">{{ formatJson(evidenceDrawerPayload || {}) }}</pre>
    </el-drawer>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card,
.shell-panel,
.summary-card,
.detail-card,
.field-block,
.detail-grid__item,
.code-card,
.history-stage-card,
.session-item,
.report-item,
.failure-item,
.contract-item,
.matrix-item,
.distribution-item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 22px;
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.08), transparent 38%),
    var(--sqlforge-surface-2);
}

.runtime-hero,
.parse-workbench__grid > article,
.analytics-shell,
.shell-panel,
.detail-card,
.summary-card,
.code-card {
  padding: 24px;
}

.runtime-hero,
.hero-side,
.composer-rail,
.evidence-rail,
.analytics-shell,
.analytics-rail,
.detail-stage,
.history-stack,
.session-list,
.drawer-stack,
.dialog-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.runtime-hero {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
}

.runtime-eyebrow,
.section-kicker,
.field-label,
.summary-card-label,
.tab-copy {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #0f766e;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.runtime-summary,
.runtime-note,
.section-summary,
.result-copy,
.issue-card__summary,
.issue-card__detail,
.history-item p,
.hero-summary,
.cell-subline,
.empty-copy,
.report-item p,
.failure-item p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.result-copy-muted {
  color: #64748b;
}

.parse-workbench__grid {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(320px, 1.05fr) minmax(360px, 1.35fr);
}

.analytics-grid,
.workspace-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(280px, 0.88fr) minmax(0, 1.12fr);
}

.section-heading,
.table-heading,
.shell-header,
.dialog-header,
.dialog-actions,
.banner-row,
.hero-inline,
.session-item-top,
.action-row {
  display: flex;
  gap: 12px;
}

.section-heading,
.table-heading,
.shell-header,
.dialog-header,
.banner-row {
  align-items: flex-start;
  justify-content: space-between;
}

.dialog-actions,
.hero-inline,
.action-row {
  align-items: center;
}

.action-row-wrap,
.summary-chip-row,
.pill-grid,
.hero-inline {
  flex-wrap: wrap;
}

.summary-chip-row,
.pill-grid {
  display: flex;
  gap: 10px;
}

.summary-chip,
.hero-pill {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: 12px;
}

.hero-pill-muted {
  color: var(--sqlforge-text-secondary);
}

.summary-chip-warning {
  background: rgba(251, 191, 36, 0.18);
}

.summary-chip-success {
  background: rgba(16, 185, 129, 0.18);
}

.form-grid,
.dialog-grid,
.filter-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.field-block-toggle {
  justify-content: flex-end;
}

.parse-card-grid,
.summary-card-grid,
.highlight-grid,
.summary-grid,
.detail-grid,
.code-grid,
.result-layout {
  display: grid;
  gap: 16px;
}

.parse-card-grid,
.result-layout {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card-grid,
.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.summary-grid-compact {
  gap: 14px;
}

.detail-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-grid-secondary {
  margin-top: 16px;
}

.detail-grid__item,
.summary-card,
.highlight-chip,
.history-item,
.issue-card,
.report-item,
.failure-item,
.contract-item,
.distribution-item,
.matrix-item {
  padding: 14px 16px;
}

.summary-card {
  min-height: 96px;
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-grid__item span,
.highlight-chip span,
.history-item span {
  color: #64748b;
  font-size: 12px;
}

.hint-card,
.conclusion-card,
.history-panel,
.parse-card,
.inline-banner,
.empty-stage {
  border-radius: 22px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(35, 35, 35, 0.92);
  padding: 18px;
}

.parse-card,
.history-panel {
  display: grid;
  gap: 14px;
}

.conclusion-card__header,
.parse-card__header,
.issue-card__header,
.history-item,
.banner-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.history-item {
  align-items: center;
}

.history-item p {
  grid-column: 1 / -1;
}

.issue-list,
.history-list,
.contract-list,
.failure-list,
.distribution-list,
.matrix-list,
.report-list {
  display: grid;
  gap: 12px;
}

.distribution-list {
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.matrix-item {
  text-align: left;
  cursor: pointer;
}

.matrix-item span,
.contract-item span {
  display: block;
  margin-top: 4px;
  color: var(--sqlforge-text-secondary);
}

.status-pill,
.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid transparent;
}

.status-pill-success,
.pill-success {
  background: rgba(16, 185, 129, 0.16);
  color: #047857;
}

.status-pill-warning,
.pill-warning {
  background: rgba(251, 191, 36, 0.16);
  color: #b45309;
}

.pill-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

.empty-state,
.empty-inline {
  margin: 0;
  color: #64748b;
}

.empty-stage {
  display: grid;
  gap: 8px;
}

.result-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border-radius: 18px;
  padding: 14px 16px;
}

.result-banner-success {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.result-banner-warning {
  background: rgba(251, 191, 36, 0.16);
  color: #92400e;
}

.result-banner-danger,
.inline-banner-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #fecaca;
  border-color: rgba(248, 113, 113, 0.35);
}

.inline-banner {
  color: var(--sqlforge-text-secondary);
}

.table-link {
  border: none;
  background: transparent;
  color: var(--sqlforge-color-brand);
  padding: 0;
  cursor: pointer;
}

.code-card__header {
  margin-bottom: 10px;
  color: var(--sqlforge-text-secondary);
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  overflow: auto;
}

.workspace-tabs :deep(.el-tabs__content) {
  padding-top: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.batch-dialog-shell {
  display: grid;
  gap: 20px;
}

.session-item,
.matrix-item {
  cursor: pointer;
}

.session-item {
  text-align: left;
}

.session-item-active {
  border-color: var(--sqlforge-color-brand-border);
}

@media (max-width: 1280px) {
  .runtime-hero,
  .parse-workbench__grid,
  .analytics-grid,
  .workspace-grid,
  .detail-grid,
  .parse-card-grid,
  .result-layout,
  .dialog-grid,
  .filter-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
