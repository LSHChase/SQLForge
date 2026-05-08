<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  createParseBatch,
  formatRuntimeError,
  getCombinedParseStatus,
  getParseBatch,
  getReportBatch,
  getSqlParseHistoryPage,
  importReportBatch,
  ingestParseBatch,
  parseStructureSql,
  resolveReportBatchSqls,
  retryParseBatchAccess,
  submitCombinedParse,
  waitForCombinedParse
} from '../../services/runtimeGateApi'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import { riskDisplayText as sharedRiskDisplayText } from '../common/issueSceneHelp.mjs'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()

const combinedTerminalStatuses = new Set(['ACCESS_SUCCEEDED', 'PARTIAL_SUCCEEDED', 'FAILED'])

const form = reactive({
  tenantId: 'tenant-a',
  datasourceCode: 'hetu_main',
  bindingMode: 'POSITIONAL',
  parserMode: 'JSQLPARSER',
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
  parserMode: 'JSQLPARSER',
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
  parserMode: 'JSQLPARSER',
  stage: 'PROD',
  priority: 'high',
  rawContent: 'RPT_A|Revenue Report|hetu_main|PROD|high\nRPT_B|Ops Report|hetu_main|PROD|medium\n'
})

const historyForm = reactive({
  tenantId: 'tenant-a',
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
  sortBy: 'submittedAt',
  sortOrder: 'DESC',
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
const activeStatisticsDetailTab = ref('summary')
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

const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)
const fieldHelpDialogVisible = ref(false)
const fieldHelpDialogTitle = ref('')
const fieldHelpDialogMessage = ref('')

const loading = reactive({
  analytics: false,
  historyPage: false,
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
const parserModeOptions = [
  { label: 'JSQLParser', value: 'JSQLPARSER' },
  { label: 'Apache Calcite', value: 'APACHE_CALCITE' },
  { label: 'JSQLParser + Hetu EXPLAIN', value: 'JSQLPARSER_WITH_PLAN' },
  { label: 'Apache Calcite + Hetu EXPLAIN', value: 'APACHE_CALCITE_WITH_PLAN' }
]
const parseFileTypeOptions = ['CSV', 'TXT', 'SQL', 'XLS', 'XLSX', 'ET']
const reportFileTypeOptions = ['TXT', 'CSV', 'XLSX']
const parseImportModeOptions = ['TABULAR_FILE', 'SQL_FILE', 'REPORT_CATALOG']
const directInputModeOptions = ['SQL_LINES', 'TABULAR_TEXT']

const activeStatus = computed(() => parseResult.value?.status || 'IDLE')
const activeConclusion = computed(() => parseResult.value?.conclusion || null)
const structureParse = computed(() => parseResult.value?.structureParse || null)
const accessParse = computed(() => parseResult.value?.accessParse || null)
const planAnalysis = computed(() => structureParse.value?.planAnalysis || null)
const statusHistory = computed(() => parseResult.value?.statusHistory || [])
const logicalObjectHits = computed(() => structureParse.value?.logicalObjectHits || [])
const structureIssues = computed(() => structureParse.value?.issues || [])
const structureIntentLabels = computed(() => structureParse.value?.intentProfile?.classificationLabels || [])
const structureRiskChecklist = computed(() => structureParse.value?.riskChecklist || [])

const structureHighlights = computed(() => {
  if (!structureParse.value) {
    return []
  }
  return [
    { key: 'parseTaskId', label: isChinese.value ? 'Parse Task' : 'Parse task', value: structureParse.value.parseTaskId },
    { key: 'sqlFingerprint', label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint', value: structureParse.value.sqlFingerprint },
    { key: 'analysisStatus', label: isChinese.value ? '组合状态' : 'Analysis status', value: structureParse.value.analysisStatus },
    { key: 'structureAnalysisStatus', label: isChinese.value ? '结构状态' : 'Structure status', value: structureParse.value.structureAnalysisStatus },
    { key: 'syntaxStatus', label: isChinese.value ? '语法状态' : 'Syntax status', value: structureParse.value.syntaxStatus },
    { key: 'complexityLevel', label: isChinese.value ? '复杂度' : 'Complexity', value: structureParse.value.complexityLevel },
    { key: 'sqlType', label: isChinese.value ? 'SQL 类型' : 'SQL type', value: structureParse.value.sqlType },
    { key: 'priorityLevel', label: isChinese.value ? '优先级' : 'Priority', value: structureParse.value.priorityLevel },
    { key: 'priorityScore', label: isChinese.value ? '评分' : 'Score', value: structureParse.value.priorityScore },
    { key: 'important', label: isChinese.value ? '重要' : 'Important', value: booleanLabel(structureParse.value.important) },
    { key: 'urgent', label: isChinese.value ? '紧急' : 'Urgent', value: booleanLabel(structureParse.value.urgent) },
    { key: 'failureReason', label: isChinese.value ? '失败原因' : 'Failure reason', value: structureParse.value.failureReason },
    {
      key: 'failurePosition',
      label: isChinese.value ? '失败位置' : 'Failure position',
      value: structureParse.value.failureLine && structureParse.value.failureColumn
        ? `line ${structureParse.value.failureLine}, column ${structureParse.value.failureColumn}`
        : structureParse.value.failureOffset
    }
  ].filter(item => hasDisplayValue(item.value))
})

const structureFeatureHighlights = computed(() => {
  const feature = structureParse.value?.featureSummary
  if (!feature) {
    return []
  }
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

const structureResourceHighlights = computed(() => {
  const estimate = structureParse.value?.estimatedResourceCost
  if (!estimate) {
    return []
  }
  return [
    { key: 'overall', label: isChinese.value ? '总体' : 'Overall', value: estimate.overall },
    { key: 'cpu', label: 'CPU', value: estimate.cpu },
    { key: 'io', label: 'IO', value: estimate.io },
    { key: 'memory', label: isChinese.value ? '内存' : 'Memory', value: estimate.memory },
    { key: 'network', label: isChinese.value ? '网络' : 'Network', value: estimate.network },
    { key: 'resultSize', label: isChinese.value ? '结果集' : 'Result size', value: estimate.resultSize }
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

const planHighlights = computed(() => {
  if (!planAnalysis.value) {
    return []
  }
  return [
    { key: 'planStatus', label: isChinese.value ? '计划状态' : 'Plan status', value: planAnalysis.value.status },
    { key: 'datasourceCode', label: isChinese.value ? '数据源' : 'Datasource', value: planAnalysis.value.datasourceCode },
    { key: 'costMs', label: isChinese.value ? '耗时 ms' : 'Cost ms', value: planAnalysis.value.costMs },
    { key: 'failureReason', label: isChinese.value ? '失败原因' : 'Failure reason', value: planAnalysis.value.failureReason }
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
    { label: isChinese.value ? '解析工具' : 'Parser tool', value: structureParse.value?.featureSummary?.parserEngine || form.parserMode },
    { label: isChinese.value ? '解析组合状态' : 'Analysis status', value: structureParse.value?.analysisStatus },
    { label: isChinese.value ? '执行计划' : 'Plan analysis', value: structureParse.value?.planAnalysis?.status },
    { label: isChinese.value ? '综合状态' : 'Overall status', value: activeConclusion.value?.overallStatus || activeStatus.value },
    { label: isChinese.value ? 'Access 可用' : 'Access available', value: booleanLabel(activeConclusion.value?.accessAvailable) },
    { label: isChinese.value ? '降级原因' : 'Degrade reason', value: activeConclusion.value?.degradeReason || parseResult.value?.degradeReason },
    {
      label: isChinese.value ? '历史写入' : 'History write',
      value: parseResult.value.historyPersisted ? (isChinese.value ? '已落库' : 'Saved') : parseResult.value.historyPersistenceStatus
    },
    { label: 'History ID', value: parseResult.value.historyId }
  ].filter(item => hasDisplayValue(item.value))
})

const requestSummary = computed(() => [
  { label: isChinese.value ? '租户' : 'Tenant', value: form.tenantId },
  { label: isChinese.value ? '数据源' : 'Datasource', value: form.datasourceCode || (isChinese.value ? '未指定' : 'Not specified') },
  { label: isChinese.value ? '绑定模式' : 'Binding mode', value: form.bindingMode },
  { label: isChinese.value ? '解析工具' : 'Parser tool', value: form.parserMode },
  { label: isChinese.value ? 'Access Parse' : 'Access parse', value: form.connectionRequired ? 'ON' : 'OFF' }
])

const parseBatchStatusCards = computed(() => {
  if (!parseBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '批次状态' : 'Batch status', parseBatchDetail.value.status),
    card(isChinese.value ? '解析工具' : 'Parser tool', parseBatchDetail.value.parserMode),
    card(isChinese.value ? '总记录数' : 'Total records', parseBatchDetail.value.totalRecords),
    card(isChinese.value ? '成功' : 'Success', parseBatchDetail.value.successRecords),
    card(isChinese.value ? '部分成功' : 'Partial success', parseBatchDetail.value.partialSuccessRecords),
    card(isChinese.value ? '失败' : 'Failed', parseBatchDetail.value.failedRecords),
    card(isChinese.value ? 'Structure 成功率' : 'Structure rate', formatNumber(parseBatchDetail.value.structureParseSuccessRate)),
    card(isChinese.value ? 'Access 成功率' : 'Access rate', formatNumber(parseBatchDetail.value.accessParseSuccessRate)),
    card(isChinese.value ? 'Plan 成功率' : 'Plan rate', formatNumber(parseBatchDetail.value.planAnalysisStatistics?.successRate))
  ].filter(item => hasDisplayValue(item.value))
})

const reportBatchStatusCards = computed(() => {
  if (!reportBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '导入状态' : 'Import status', reportBatchDetail.value.status),
    card(isChinese.value ? '解析工具' : 'Parser tool', reportBatchDetail.value.parserMode),
    card(isChinese.value ? '报表总数' : 'Total reports', reportBatchDetail.value.totalReports),
    card(isChinese.value ? '已解析 SQL' : 'Resolved reports', reportBatchDetail.value.resolvedReports),
    card(isChinese.value ? '失败数' : 'Failed reports', reportBatchDetail.value.failedReports),
    card(isChinese.value ? 'Plan 成功率' : 'Plan rate', formatNumber(reportBatchDetail.value.planAnalysisStatistics?.successRate)),
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
const statisticsDetailSummaryEntries = computed(() =>
  Object.entries(statisticsDetailPayload.value || {})
    .filter(([, value]) => value === null || typeof value !== 'object' || Array.isArray(value))
)
const statisticsDetailRelationEntries = computed(() => {
  const payload = statisticsDetailPayload.value || {}
  return [
    ['reportCode', payload.reportCode],
    ['itemId', payload.itemId],
    ['parseTaskId', payload.parseTaskId],
    ['datasourceCode', payload.datasourceCode],
    ['stage', payload.stage],
    ['issueScenes', payload.issueScenes],
    ['issueScene', payload.issueScene],
    ['sqlCount', payload.sqlCount],
    ['issueCount', payload.issueCount],
    ['affectedSqlCount', payload.affectedSqlCount],
    ['reportCount', payload.reportCount],
    ['logicalObjectKeys', payload.logicalObjectKeys]
  ].filter(([, value]) => hasDisplayValue(value) || (Array.isArray(value) && value.length > 0))
})

const historyRows = computed(() => historyPage.value?.items || [])

const severityStats = computed(() => {
  const groups = new Map()
  issueScenes.value.forEach(item => {
    const key = String(item.severity || 'UNKNOWN')
    const current = groups.get(key) || {
      severity: key,
      issueSceneCount: 0,
      affectedSqlCount: 0,
      affectedIssueCount: 0,
      urgentCount: 0
    }
    current.issueSceneCount += 1
    current.affectedSqlCount += Number(item.affectedSqlCount || 0)
    current.affectedIssueCount += Number(item.affectedIssueCount || 0)
    if (item.urgent === true) {
      current.urgentCount += 1
    }
    groups.set(key, current)
  })
  return Array.from(groups.values()).sort((left, right) => right.affectedSqlCount - left.affectedSqlCount)
})

const priorityStats = computed(() => {
  const groups = new Map()
  issueScenes.value.forEach(item => {
    const key = String(item.priorityLevel || 'UNKNOWN')
    const current = groups.get(key) || {
      priorityLevel: key,
      issueSceneCount: 0,
      affectedSqlCount: 0,
      affectedIssueCount: 0,
      highestPriorityScore: 0
    }
    current.issueSceneCount += 1
    current.affectedSqlCount += Number(item.affectedSqlCount || 0)
    current.affectedIssueCount += Number(item.affectedIssueCount || 0)
    current.highestPriorityScore = Math.max(current.highestPriorityScore, Number(item.priorityScore || 0))
    groups.set(key, current)
  })
  return Array.from(groups.values()).sort((left, right) => right.affectedSqlCount - left.affectedSqlCount)
})

const logicalObjectStats = computed(() => {
  const groups = new Map()
  historyRows.value.forEach(row => {
    const typedHits = [
      ...(Array.isArray(row.logicalObjectTypes) ? row.logicalObjectTypes.map(type => ({ key: type, type })) : []),
      ...(Array.isArray(row.logicalObjectHits)
        ? row.logicalObjectHits.map(hit => ({
            key: hit.objectKey || hit.logicalObjectKey || hit.objectName || hit.objectType || hit.logicalObjectType || 'OBJECT',
            type: hit.objectType || hit.logicalObjectType || 'OBJECT'
          }))
        : [])
    ]
    typedHits.forEach(hit => {
      const current = groups.get(hit.key) || {
        logicalObjectKey: hit.key,
        logicalObjectType: hit.type,
        sampleCount: 0
      }
      current.sampleCount += 1
      groups.set(hit.key, current)
    })
  })
  return Array.from(groups.values()).sort((left, right) => right.sampleCount - left.sampleCount)
})

const parseStatusStats = computed(() => {
  const groups = new Map()
  historyRows.value.forEach(row => {
    const key = String(row.resultStatus || 'UNKNOWN')
    const current = groups.get(key) || {
      resultStatus: key,
      sampleCount: 0,
      cacheHitCount: 0,
      rewriteCount: 0,
      accelerationCount: 0
    }
    current.sampleCount += 1
    if (row.cacheHit === true) {
      current.cacheHitCount += 1
    }
    if (row.rewriteApplied === true) {
      current.rewriteCount += 1
    }
    if (row.accelerationApplied === true) {
      current.accelerationCount += 1
    }
    groups.set(key, current)
  })
  return Array.from(groups.values()).sort((left, right) => right.sampleCount - left.sampleCount)
})

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

function localizedDisplayText(value) {
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

function booleanLabel(value) {
  if (typeof value !== 'boolean') {
    return ''
  }
  if (isChinese.value) {
    return value ? '是' : '否'
  }
  return value ? 'true' : 'false'
}

function helpTextForKey(key) {
  const glossary = {
    parseTaskId: isChinese.value ? '本次解析任务的唯一编号，用于状态刷新和历史追溯。' : 'Unique parse task identifier for status refresh and history tracing.',
    sqlFingerprint: isChinese.value ? 'SQL 指纹用于识别结构相同或相近的 SQL。' : 'SQL fingerprint identifies structurally identical or similar SQL.',
    syntaxStatus: isChinese.value ? '语法状态，VALID 表示结构解析通过。' : 'Syntax status. VALID means structure parsing passed.',
    complexityLevel: isChinese.value ? '复杂度等级，用于判断 SQL 阅读和治理成本。' : 'Complexity level for SQL review and governance cost.',
    sqlType: isChinese.value ? 'SQL 类型，例如 SELECT、INSERT 或其他语句类别。' : 'SQL type, such as SELECT, INSERT, or another statement kind.',
    priorityLevel: isChinese.value ? '优先级。P1 表示最高优先级，需要红色高亮。' : 'Priority level. P1 is the highest priority and is highlighted red.',
    priorityScore: isChinese.value ? '优先级评分，分值越高越需要优先处理。' : 'Priority score. Higher scores need earlier handling.',
    urgent: isChinese.value ? '是否紧急。是表示需要红色提示。' : 'Whether the result is urgent. True is highlighted red.',
    important: isChinese.value ? '是否重要，用于区分治理关注度。' : 'Whether the result is important for governance attention.',
    parserEngine: isChinese.value ? 'Parser 表示当前解析引擎。' : 'Parser identifies the current parsing engine.',
    scanMode: isChinese.value ? '扫描模式，表示 SQL 读取数据的主要方式。' : 'Scan mode describes how the SQL reads data.',
    joinType: isChinese.value ? 'Join 类型，表示主要表关联方式。' : 'Join type describes the main table-join pattern.',
    computeDensity: isChinese.value ? '计算密度，表示表达式、聚合或窗口计算的集中程度。' : 'Compute density reflects expression, aggregation, or window-function weight.',
    slaLevel: isChinese.value ? 'SLA 是服务等级目标，用于提示时效要求。' : 'SLA means service level objective for timeliness.',
    serviceStatus: isChinese.value ? '服务状态，表示 Access Parse 服务是否可用。' : 'Service status shows whether Access Parse is available.',
    connectionStatus: isChinese.value ? '连接状态，表示数据源连接是否成功。' : 'Connection status shows whether datasource connectivity succeeded.',
    objectResolutionStatus: isChinese.value ? '对象解析状态，表示表、视图等对象是否能被识别。' : 'Object resolution status shows whether tables or views were resolved.',
    compatibilityStatus: isChinese.value ? '兼容性状态，表示 SQL 与目标引擎是否匹配。' : 'Compatibility status shows whether SQL matches the target engine.'
  }
  return glossary[key] || ''
}

function openFieldHelp(key, label) {
  const message = helpTextForKey(key)
  if (!message) {
    return
  }
  fieldHelpDialogTitle.value = label
  fieldHelpDialogMessage.value = message
  fieldHelpDialogVisible.value = true
}

function riskDisplayText(risk, field) {
  return localizedDisplayText(sharedRiskDisplayText(risk, field, isChinese.value))
}

function resultValueClass(item) {
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

function resultBannerClass(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized.includes('FAILED')) {
    return 'result-banner-danger'
  }
  if (normalized.includes('PARTIAL') || normalized.includes('PARSING') || normalized.includes('WAITING')) {
    return 'result-banner-warning'
  }
  return 'result-banner-success'
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
    parserMode: String(form.parserMode || 'JSQLPARSER').trim(),
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

function currentSingleSqlInputKey() {
  return JSON.stringify({
    tenantId: form.tenantId,
    datasourceCode: form.datasourceCode,
    bindingMode: form.bindingMode,
    parserMode: form.parserMode,
    connectionRequired: form.connectionRequired,
    sqlText: form.sqlText,
    sqlTemplateText: form.sqlTemplateText,
    bindParametersText: form.bindParametersText,
    commentContextText: form.commentContextText
  })
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

function openParseHistoryDetail() {
  if (!parseResult.value?.historyId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: {
      tenantId: form.tenantId,
      historyId: parseResult.value.historyId
    }
  })
}

async function runStructurePreview() {
  running.value = true
  lastRunMode.value = 'structure'
  errorMessage.value = ''
  const inputKey = currentSingleSqlInputKey()
  try {
    const payload = buildRequestPayload()
    const structureOnlyResult = await parseStructureSql(payload, {
      requestPrefix: 'frontend-parse-workbench-structure'
    })
    if (currentSingleSqlInputKey() === inputKey) {
      parseResult.value = normalizeStructureResult(structureOnlyResult)
    }
  } catch (error) {
    if (currentSingleSqlInputKey() === inputKey) {
      parseResult.value = null
      errorMessage.value = formatRuntimeError(error)
    }
  } finally {
    running.value = false
  }
}

async function runCombinedParseFlow() {
  running.value = true
  lastRunMode.value = 'combined'
  errorMessage.value = ''
  const inputKey = currentSingleSqlInputKey()
  try {
    const payload = buildRequestPayload()
    const initialResult = await submitCombinedParse(payload, {
      requestPrefix: 'frontend-parse-workbench-submit'
    })
    if (currentSingleSqlInputKey() === inputKey) {
      parseResult.value = initialResult
    }
    if (!combinedTerminalStatuses.has(initialResult.status)) {
      const terminalResult = await waitForCombinedParse(initialResult.parseTaskId, payload.tenantId, {
        requestPrefix: 'frontend-parse-workbench-terminal'
      })
      if (currentSingleSqlInputKey() === inputKey) {
        parseResult.value = terminalResult
      }
    }
    if (currentSingleSqlInputKey() === inputKey) {
      await Promise.allSettled([loadAnalytics(), loadHistoryPage()])
    }
  } catch (error) {
    if (currentSingleSqlInputKey() === inputKey) {
      parseResult.value = null
      errorMessage.value = formatRuntimeError(error)
    }
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
      parserMode: parseBatchForm.parserMode,
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
      parserMode: reportBatchForm.parserMode,
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
    await loadHistoryPage()
    const historyItems = Array.isArray(historyPage.value?.items) ? historyPage.value.items : []
    const statusCounts = historyPage.value?.classificationSummary?.statusCounts || {}
    const sourceTypeCounts = historyPage.value?.classificationSummary?.sourceTypeCounts || {}
    const accessChannelCounts = historyPage.value?.classificationSummary?.accessChannelCounts || {}
    const currentIssues = Array.isArray(structureIssues.value) ? structureIssues.value : []
    const currentHistoryIssueScenes = new Set(
      currentIssues.map(item => String(item.issueScene || '').trim()).filter(Boolean)
    )
    const currentImportantCount = currentIssues.filter(item => item.important === true).length
    const currentUrgentCount = currentIssues.filter(item => item.urgent === true).length
    const nonSuccessHistoryCount = historyItems.filter(item => String(item.resultStatus || '').toUpperCase() !== 'SUCCESS').length

    overview.value = {
      totalSqlCount: historyItems.length + (parseResult.value ? 1 : 0),
      issueSqlCount: nonSuccessHistoryCount + (currentIssues.length > 0 ? 1 : 0),
      totalIssueCount: nonSuccessHistoryCount + currentIssues.length,
      issueSceneCount: currentHistoryIssueScenes.size + Object.keys(sourceTypeCounts).length,
      importantSqlCount: currentImportantCount + nonSuccessHistoryCount,
      urgentSqlCount: currentUrgentCount + historyItems.filter(item => String(item.resultStatus || '').toUpperCase() === 'FAILED').length,
      priorityDistribution: {
        ...statusCounts,
        ...accessChannelCounts
      }
    }

    issueScenes.value = currentIssues.map((issue, index) => ({
      issueScene: issue.issueScene || `ISSUE_${index + 1}`,
      issueDomain: issue.issueDomain || '-',
      severity: issue.severity || '-',
      priorityLevel: issue.priorityLevel || '-',
      affectedSqlCount: issue.affectedSqlCount || 1,
      affectedIssueCount: issue.affectedReportCount || 1,
      sqlRatio: 1
    }))

    sqlStats.value = historyItems.map((item, index) => ({
      itemId: item.historyId || `history-${index + 1}`,
      batchId: item.sourceType || 'SQL_PARSE_HISTORY',
      parseTaskId: item.parseTaskId || item.historyId,
      reportCode: item.historyId || item.sqlFingerprint || `history-${index + 1}`,
      datasourceCode: item.datasourceCode || '-',
      stage: item.resultStatus || '-',
      sqlDigest: item.sqlFingerprint || '-',
      issueCount: String(item.resultStatus || '').toUpperCase() === 'SUCCESS' ? 0 : 1,
      highestPriorityLevel: String(item.resultStatus || '').toUpperCase() === 'SUCCESS' ? 'P4' : 'P1',
      highestPriorityScore: String(item.resultStatus || '').toUpperCase() === 'SUCCESS' ? 0 : 100,
      important: String(item.resultStatus || '').toUpperCase() !== 'SUCCESS',
      urgent: String(item.resultStatus || '').toUpperCase() === 'FAILED',
      issueScenes: item.logicalObjectTypes || []
    }))

    reportStats.value = historyItems.map((item, index) => ({
      reportCode: item.reportCode || item.sourceType || `HISTORY_${index + 1}`,
      sqlCount: 1,
      issueCount: String(item.resultStatus || '').toUpperCase() === 'SUCCESS' ? 0 : 1,
      highestPriorityLevel: String(item.resultStatus || '').toUpperCase() === 'SUCCESS' ? 'P4' : 'P1',
      highestPriorityScore: String(item.resultStatus || '').toUpperCase() === 'SUCCESS' ? 0 : 100,
      important: String(item.resultStatus || '').toUpperCase() !== 'SUCCESS',
      urgent: String(item.resultStatus || '').toUpperCase() === 'FAILED',
      issueScenes: item.logicalObjectTypes || []
    }))

    priorityMatrix.value = Object.entries(statusCounts).map(([priorityLevel, sqlCount]) => ({
      priorityLevel,
      urgencyBucket: 'HISTORY',
      sqlCount,
      issueCount: nonSuccessHistoryCount,
      reportCount: historyItems.length
    }))

    importantUrgent.value = currentIssues
      .filter(item => item.important === true || item.urgent === true)
      .map((issue, index) => ({
        reportCode: issue.issueCode || `ISSUE_${index + 1}`,
        highestPriorityLevel: issue.priorityLevel || '-',
        highestPriorityScore: issue.priorityScore || 0,
        datasourceCode: form.datasourceCode || '-',
        stage: issue.issueDomain || '-',
        issueScenes: [issue.issueScene || '-']
      }))
  } catch (error) {
    analyticsErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.analytics = false
  }
}

function openStatisticsDetail(title, payload) {
  statisticsDetailTitle.value = title
  statisticsDetailPayload.value = payload
  activeStatisticsDetailTab.value = 'summary'
  statisticsDetailDialogVisible.value = true
}

async function loadHistoryPage() {
  loading.historyPage = true
  historyErrorMessage.value = ''
  try {
    historyPage.value = await getSqlParseHistoryPage(
      {
        tenantId: historyForm.tenantId,
        reportCode: historyForm.reportCode,
        datasourceCode: historyForm.datasourceCode,
        stage: historyForm.stage,
        bizDate: historyForm.bizDate,
        queryDateStart: historyForm.queryDateStart,
        queryDateEnd: historyForm.queryDateEnd,
        accessChannel: historyForm.accessChannel,
        status: historyForm.status,
        logicalObjectType: historyForm.logicalObjectType,
        engine: historyForm.engine,
        submittedBy: historyForm.submittedBy,
        submittedStart: historyForm.submittedStart,
        submittedEnd: historyForm.submittedEnd,
        sortBy: historyForm.sortBy,
        sortOrder: historyForm.sortOrder,
        pageNo: 1,
        pageSize: 10
      },
      {
        requestPrefix: 'frontend-acceleration-parse-history-page'
      }
    )
  } catch (error) {
    historyPage.value = null
    historyErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.historyPage = false
  }
}

function applyRouteWorkspace() {
  const workspace = String(route.query.workspace || '').trim()
  if (workspace === 'batch') {
    router.replace({ path: ROUTE_PATHS.parseBatchCenter })
    return
  }
  if (workspace === 'statistics') {
    activeAnalyticsTab.value = String(route.query.analytics || 'issue')
  }
  if (workspace === 'history') {
    router.replace({ path: ROUTE_PATHS.parseRecord })
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
  parseBatchForm.parserMode = form.parserMode
  reportBatchForm.parserMode = form.parserMode
  applyRouteWorkspace()
  await loadAnalytics()
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

watch(
  () => form.parserMode,
  value => {
    parseBatchForm.parserMode = value
    reportBatchForm.parserMode = value
  }
)

watch(
  () => currentSingleSqlInputKey(),
  () => {
    if (parseResult.value || errorMessage.value) {
      parseResult.value = null
      errorMessage.value = ''
    }
  }
)
</script>

<template>
  <section class="runtime-page parse-workbench-page" data-testid="parse-workbench-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">sql optimization SQL Parse · SQL解析</p>
        <h1 class="runtime-title">{{ t('acceleration.title') }}</h1>
        <p class="runtime-summary">{{ t('acceleration.summary') }}</p>
      </div>
      <div class="hero-side">
        <div class="action-row action-row-wrap">
          <el-button type="primary" :loading="loading.analytics" @click="loadAnalytics">
            {{ isChinese ? '刷新统计视角' : 'Refresh statistics' }}
          </el-button>
          <el-button @click="resetResult">
            {{ isChinese ? '清空结果' : 'Reset result' }}
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

          <label class="field-block">
            <span class="field-label">解析工具 / Parser tool</span>
            <el-select v-model="form.parserMode" data-testid="parse-workbench-parser-mode">
              <el-option
                v-for="option in parserModeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>

          <label class="field-block field-block-toggle">
            <span class="field-label">{{ isChinese ? '执行 access parse' : 'Run access parse' }}</span>
            <el-switch v-model="form.connectionRequired" />
          </label>

          <div class="field-block field-block-wide">
            <SqlEditorField
              v-model="form.sqlText"
              label="SQL"
              :rows="8"
              :copy-label="isChinese ? '复制' : 'Copy'"
              :format-label="isChinese ? '格式化' : 'Format'"
              data-testid="parse-workbench-sql-input"
            />
          </div>

          <div class="field-block field-block-wide">
            <SqlEditorField
              v-model="form.sqlTemplateText"
              :label="isChinese ? '模板 SQL' : 'Template SQL'"
              :rows="4"
              :copy-label="isChinese ? '复制' : 'Copy'"
              :format-label="isChinese ? '格式化' : 'Format'"
              data-testid="parse-workbench-template-sql-input"
            />
          </div>

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
            <p class="section-kicker sqlforge-code-label">parse result</p>
            <h2 class="section-title">{{ isChinese ? '解析结果' : 'Parse result' }}</h2>
            <p class="section-summary">
              {{ isChinese ? '解析结果备注：结构解析、综合结论、风险判断与规则命中。' : 'Result note: structure parsing, overall verdict, risk judgment, and rule hits.' }}
            </p>
          </div>
          <el-button v-if="parseResult" text @click="openEvidenceDrawer(isChinese ? '解析原始证据' : 'Raw parse evidence', parseResult)">
            {{ isChinese ? '查看长证据' : 'View long evidence' }}
          </el-button>
          <el-button v-if="parseResult?.historyId" text data-testid="parse-workbench-open-history" @click="openParseHistoryDetail">
            {{ isChinese ? '查看解析历史' : 'Open parse history' }}
          </el-button>
        </div>

        <p v-if="!parseResult && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '上方输入一条 SQL 后可先做结构解析，也可直接执行综合解析查看结构、访问和综合结论。'
              : 'Enter one SQL statement above to run a structure-only preview or a combined parse with structure, access, and the overall verdict.'
          }}
        </p>

        <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="parse-workbench-error">
          {{ errorMessage }}
        </div>

        <template v-if="parseResult">
          <div class="result-banner" :class="resultBannerClass(activeStatus)">
            <strong data-testid="parse-workbench-status">{{ activeStatus }}</strong>
            <span>{{ parseResult.parseTaskId }}</span>
          </div>

          <div class="result-overview-card">
            <div class="conclusion-card__header">
              <span class="summary-card-label">{{ isChinese ? '解析结果备注' : 'Parse result note' }}</span>
              <strong data-testid="parse-workbench-overall-status">{{ activeConclusion?.overallStatus || activeStatus }}</strong>
            </div>
            <div class="summary-chip-row">
              <span v-for="item in summaryCards" :key="item.label" class="summary-chip">
                {{ item.label }}: <strong>{{ item.value }}</strong>
              </span>
            </div>
            <p v-if="activeConclusion" class="result-copy">{{ localizedDisplayText(activeConclusion.summary) }}</p>
            <p v-if="activeConclusion" class="result-copy result-copy-muted">{{ localizedDisplayText(activeConclusion.recommendedAction) }}</p>
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
                  <div v-for="item in structureHighlights" :key="item.key" class="highlight-chip" :class="resultValueClass(item)">
                    <span>
                      {{ item.label }}
                      <el-button
                        v-if="helpTextForKey(item.key)"
                        text
                        size="small"
                        class="help-dot"
                        aria-label="field help"
                        @click="openFieldHelp(item.key, item.label)"
                      >
                        ?
                      </el-button>
                    </span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div class="mini-section" data-testid="parse-workbench-query-intent">
                  <span class="summary-card-label">{{ isChinese ? '查询意图标签' : 'Query intent labels' }}</span>
                  <div v-if="structureIntentLabels.length" class="pill-grid">
                    <span
                      v-for="item in structureIntentLabels"
                      :key="`intent-${item}`"
                      class="summary-chip summary-chip-success"
                    >
                      {{ item }}
                    </span>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '暂无查询意图标签。' : 'No query-intent labels yet.' }}
                  </p>
                </div>

                <div class="mini-section" data-testid="parse-workbench-feature-summary">
                  <span class="summary-card-label">{{ isChinese ? '多维特征' : 'Feature dimensions' }}</span>
                  <div v-if="structureFeatureHighlights.length" class="highlight-grid highlight-grid-compact">
                    <div v-for="item in structureFeatureHighlights" :key="item.key" class="highlight-chip" :class="resultValueClass(item)">
                      <span>
                        {{ item.label }}
                        <el-button
                          v-if="helpTextForKey(item.key)"
                          text
                          size="small"
                          class="help-dot"
                          aria-label="field help"
                          @click="openFieldHelp(item.key, item.label)"
                        >
                          ?
                        </el-button>
                      </span>
                      <strong>{{ item.value }}</strong>
                    </div>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '暂无多维特征。' : 'No feature summary yet.' }}
                  </p>
                </div>

                <div class="mini-section" data-testid="parse-workbench-resource-estimate">
                  <span class="summary-card-label">{{ isChinese ? '预估资源消耗' : 'Estimated resource cost' }}</span>
                  <div v-if="structureResourceHighlights.length" class="summary-chip-row">
                    <span
                      v-for="item in structureResourceHighlights"
                      :key="`resource-${item.key}`"
                      class="summary-chip"
                    >
                      {{ item.label }}: <strong>{{ item.value }}</strong>
                    </span>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '暂无资源估算。' : 'No resource estimate yet.' }}
                  </p>
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

                <div class="mini-section" data-testid="parse-workbench-risk-checklist">
                  <span class="summary-card-label">{{ isChinese ? '风险清单' : 'Risk checklist' }}</span>
                  <div v-if="structureRiskChecklist.length" class="issue-list issue-list-compact">
                    <article
                      v-for="(risk, index) in structureRiskChecklist"
                      :key="`${risk.riskCode || 'risk'}-${index}`"
                      class="issue-card"
                      data-testid="parse-workbench-risk"
                    >
                      <div class="issue-card__header">
                        <strong>{{ risk.riskCode }}</strong>
                        <span>{{ risk.severity }}</span>
                      </div>
                      <p class="issue-card__summary">{{ riskDisplayText(risk, 'summary') }}</p>
                      <p class="issue-card__detail">{{ riskDisplayText(risk, 'evidence') }}</p>
                      <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ riskDisplayText(risk, 'suggestedAction') }}</p>
                    </article>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '未识别高风险项。' : 'No high-risk items were detected.' }}
                  </p>
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
                    <p class="issue-card__summary">{{ localizedDisplayText(issue.summary) }}</p>
                    <p class="issue-card__detail">{{ localizedDisplayText(issue.detail) }}</p>
                    <p v-if="issue.failureLine || issue.failureColumn || issue.failureToken || issue.failureSnippet" class="issue-card__detail">
                      {{ isChinese ? '失败定位' : 'Failure position' }}:
                      <span v-if="issue.failureLine && issue.failureColumn">line {{ issue.failureLine }}, column {{ issue.failureColumn }}</span>
                      <span v-if="issue.failureToken"> · token {{ issue.failureToken }}</span>
                      <span v-if="issue.failureSnippet"> · {{ issue.failureSnippet }}</span>
                    </p>
                    <p class="issue-card__detail">{{ isChinese ? '建议动作' : 'Suggested action' }}: {{ localizedDisplayText(issue.suggestedAction) }}</p>
                  </article>
                </div>
              </template>
            </article>

            <article class="parse-card" data-testid="parse-workbench-plan-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">hetu explain plan</p>
                  <h3 class="detail-title">{{ isChinese ? '执行计划卡' : 'Execution plan card' }}</h3>
                </div>
                <span
                  class="status-pill"
                  :class="planAnalysis?.status === 'SUCCESS' ? 'status-pill-success' : 'status-pill-warning'"
                >
                  {{ planAnalysis?.status || (isChinese ? '未执行' : 'Not run') }}
                </span>
              </div>

              <p v-if="!planAnalysis" class="empty-state">
                {{ isChinese ? '旧历史或无计划模式不会生成 Hetu 执行计划。' : 'Old history or non-plan modes do not include Hetu plan output.' }}
              </p>

              <template v-else>
                <div class="highlight-grid">
                  <div v-for="item in planHighlights" :key="item.key" class="highlight-chip" :class="resultValueClass(item)">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
                <div class="mini-section" data-testid="parse-workbench-plan-analysis">
                  <span class="summary-card-label">{{ isChinese ? '计划证据' : 'Plan evidence' }}</span>
                  <div v-if="planAnalysis.evidence?.length" class="pill-grid">
                    <span v-for="item in planAnalysis.evidence" :key="item" class="summary-chip">{{ item }}</span>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '暂无计划证据。' : 'No plan evidence yet.' }}
                  </p>
                </div>
                <SqlCodeBlock
                  v-if="planAnalysis.planText"
                  :value="planAnalysis.planText"
                  :label="isChinese ? 'Hetu EXPLAIN 输出' : 'Hetu EXPLAIN output'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="parse-workbench-plan-text"
                />
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
                  <div v-for="item in accessHighlights" :key="item.key" class="highlight-chip" :class="resultValueClass(item)">
                    <span>
                      {{ item.label }}
                      <el-button
                        v-if="helpTextForKey(item.key)"
                        text
                        size="small"
                        class="help-dot"
                        aria-label="field help"
                        @click="openFieldHelp(item.key, item.label)"
                      >
                        ?
                      </el-button>
                    </span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? 'Plan Summary' : 'Plan summary' }}</span>
                  <p class="result-copy">{{ localizedDisplayText(accessParse.planSummary) }}</p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '可用性告警' : 'Availability warning' }}</span>
                  <p class="result-copy result-copy-muted">{{ localizedDisplayText(accessParse.availabilityWarning) }}</p>
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
          <p class="section-kicker sqlforge-code-label">sql parse statistics</p>
          <h2 class="section-title">{{ isChinese ? '解析结果统计' : 'Parse result statistics' }}</h2>
          <p class="runtime-note">
            {{
              isChinese
                ? '统计区展示当前单 SQL 结果、历史摘要与结果追溯，不再承载批量入口。'
                : 'Statistics summarize the current single-SQL result, history digest, and traceability without batch entry points.'
            }}
          </p>
        </div>
        <div class="action-row action-row-wrap">
          <el-button :loading="loading.analytics" data-testid="statistics-refresh" @click="loadAnalytics">
            {{ isChinese ? '刷新统计' : 'Refresh statistics' }}
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

            <el-tab-pane :label="isChinese ? '严重度视角' : 'By severity'" name="severity">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">Severity view</p>
                  <h3 class="detail-title">{{ isChinese ? '严重度视角' : 'By severity' }}</h3>
                </div>
              </div>
              <el-table :data="severityStats" border>
                <el-table-column prop="severity" :label="isChinese ? '严重度' : 'Severity'" min-width="140" />
                <el-table-column prop="issueSceneCount" :label="isChinese ? '问题场景数' : 'Issue scenes'" min-width="140" />
                <el-table-column prop="affectedSqlCount" :label="isChinese ? '影响 SQL' : 'Affected SQL'" min-width="140" />
                <el-table-column prop="affectedIssueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="120" />
                <el-table-column prop="urgentCount" :label="isChinese ? '紧急场景' : 'Urgent scenes'" min-width="140" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '优先级视角' : 'By priority'" name="priority">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">Priority view</p>
                  <h3 class="detail-title">{{ isChinese ? '优先级视角' : 'By priority' }}</h3>
                </div>
              </div>
              <el-table :data="priorityStats" border>
                <el-table-column prop="priorityLevel" :label="isChinese ? '优先级' : 'Priority'" min-width="140" />
                <el-table-column prop="issueSceneCount" :label="isChinese ? '问题场景数' : 'Issue scenes'" min-width="140" />
                <el-table-column prop="affectedSqlCount" :label="isChinese ? '影响 SQL' : 'Affected SQL'" min-width="140" />
                <el-table-column prop="affectedIssueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="120" />
                <el-table-column prop="highestPriorityScore" :label="isChinese ? '最高分' : 'Highest score'" min-width="140" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '逻辑对象视角' : 'Logical object view'" name="logical-object">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">Logical object view</p>
                  <h3 class="detail-title">{{ isChinese ? '逻辑对象视角' : 'Logical object view' }}</h3>
                </div>
              </div>
              <el-table :data="logicalObjectStats" border>
                <el-table-column prop="logicalObjectType" :label="isChinese ? '对象类型' : 'Type'" min-width="150" />
                <el-table-column prop="logicalObjectKey" :label="isChinese ? '对象标识' : 'Object key'" min-width="240" />
                <el-table-column prop="sampleCount" :label="isChinese ? '命中样本' : 'Samples'" min-width="120" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '解析状态样本' : 'Parse status samples'" name="parse-status">
              <div class="table-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">Parse status samples</p>
                  <h3 class="detail-title">{{ isChinese ? '解析状态样本' : 'Parse status samples' }}</h3>
                </div>
              </div>
              <el-table :data="parseStatusStats" border>
                <el-table-column prop="resultStatus" :label="isChinese ? '结果状态' : 'Result status'" min-width="150" />
                <el-table-column prop="sampleCount" :label="isChinese ? '样本数' : 'Samples'" min-width="120" />
                <el-table-column prop="cacheHitCount" :label="isChinese ? '缓存命中' : 'Cache hit'" min-width="120" />
                <el-table-column prop="rewriteCount" :label="isChinese ? '轻量改写' : 'Rewrite'" min-width="120" />
                <el-table-column prop="accelerationCount" :label="isChinese ? '加速命中' : 'Acceleration'" min-width="130" />
              </el-table>
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
                  ? '批量解析已恢复为显式二级入口；创建批次、导入内容、失败重试和报表导入仍统一收进同一工作区。'
                  : 'Batch parsing is exposed again as an explicit secondary entry while creation, import, retries, and report ingestion still stay in one workspace.'
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
                        <p v-if="item.planAnalysisStatus || item.analysisStatus">
                          Plan: {{ displayValue(item.planAnalysisStatus) }} · Analysis: {{ displayValue(item.analysisStatus) }}
                        </p>
                        <SqlCodeBlock
                          v-if="item.sqlText || item.sqlPreview"
                          :value="item.sqlText || item.sqlPreview"
                          :label="isChinese ? '失败 SQL' : 'Failed SQL'"
                          :copy-label="isChinese ? '复制' : 'Copy'"
                          compact
                        />
                        <p v-else>{{ displayValue(item.message) }}</p>
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
                    <p v-if="item.planAnalysisStatus || item.analysisStatus">
                      Plan: {{ displayValue(item.planAnalysisStatus) }} · Analysis: {{ displayValue(item.analysisStatus) }}
                    </p>
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
        <label class="field-block">
          <span class="field-label">解析工具 / Parser tool</span>
          <el-select v-model="parseBatchForm.parserMode" data-testid="parse-workbench-parse-batch-parser-mode">
            <el-option
              v-for="option in parserModeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
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
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="parseBatchForm.rawContent"
            :label="isChinese ? '批量内容' : 'Batch content'"
            :rows="8"
            :copy-label="isChinese ? '复制' : 'Copy'"
            :format-label="isChinese ? '格式化' : 'Format'"
            :format-enabled="parseBatchForm.directInputMode === 'SQL_LINES'"
            data-testid="parse-batch-raw-sql-input"
          />
        </div>
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
          <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
          <el-input v-model="reportBatchForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">解析工具 / Parser tool</span>
          <el-select v-model="reportBatchForm.parserMode" data-testid="parse-workbench-report-batch-parser-mode">
            <el-option
              v-for="option in parserModeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
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
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="reportBatchForm.rawContent"
            :label="isChinese ? '清单内容' : 'Catalog content'"
            :rows="7"
            :copy-label="isChinese ? '复制' : 'Copy'"
            :format-label="isChinese ? '格式化' : 'Format'"
            :format-enabled="false"
            data-testid="report-batch-raw-sql-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.importReportBatch" @click="importReportBatchFlow">
          {{ isChinese ? '导入批次' : 'Import batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statisticsDetailDialogVisible" :title="statisticsDetailTitle" width="760px" data-testid="statistics-detail-dialog">
      <el-tabs v-model="activeStatisticsDetailTab" data-testid="statistics-detail-tabs">
        <el-tab-pane :label="isChinese ? '摘要' : 'Summary'" name="summary">
          <div class="detail-grid">
            <div v-for="[key, value] in statisticsDetailSummaryEntries" :key="key" class="detail-grid__item">
              <span>{{ key }}</span>
              <strong>{{ displayValue(value) }}</strong>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '关联 SQL/报表' : 'Related SQL/report'" name="relations">
          <div class="detail-grid">
            <div
              v-for="[key, value] in statisticsDetailRelationEntries"
              :key="key"
              class="detail-grid__item"
              data-testid="statistics-detail-relation"
            >
              <span>{{ key }}</span>
              <strong>{{ displayValue(value) }}</strong>
            </div>
          </div>
          <p v-if="!statisticsDetailRelationEntries.length" class="result-copy">
            {{ isChinese ? '当前记录没有关联 SQL 或报表定位。' : 'No SQL or report locator is available for this record.' }}
          </p>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '原始 JSON' : 'Raw JSON'" name="raw">
          <pre class="code-block" data-testid="statistics-detail-raw-json">{{ formatJson(statisticsDetailPayload || {}) }}</pre>
        </el-tab-pane>
      </el-tabs>
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

    <el-drawer v-model="evidenceDrawerVisible" :title="evidenceDrawerTitle || (isChinese ? '原始证据' : 'Raw evidence')" size="42%">
      <pre class="code-block">{{ formatJson(evidenceDrawerPayload || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (isChinese ? '字段说明' : 'Field help')" width="560px">
      <p class="result-copy">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ isChinese ? '知道了' : 'Close' }}
        </el-button>
      </template>
    </el-dialog>
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
  grid-template-columns: 1fr;
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
  grid-template-columns: 1fr;
}

.summary-card-grid,
.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.highlight-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
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

.help-dot {
  min-width: 20px;
  height: 20px;
  margin-left: 4px;
  padding: 0;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 50%;
  color: var(--sqlforge-text-secondary);
  line-height: 18px;
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
.result-overview-card,
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
.result-overview-card,
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
