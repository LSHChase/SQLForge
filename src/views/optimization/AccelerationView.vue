<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  buildReportImportRows,
  resolveRuntimeDatasourceCode,
  resolveRuntimeTenantId
} from '../../config/tenantDefaults.mjs'
import {
  createParseBatch,
  createRewriteTrial,
  formatRuntimeError,
  getCombinedParseStatus,
  getGovernanceDatasources,
  getParseBatch,
  getReportBatch,
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
import RewriteValidationView from '../rewrite-validation/RewriteValidationView.vue'
import { formatBeijingTimestamp } from '../common/beijingTime.mjs'
import { buildDatasourceOptions, withCurrentOption } from '../common/formComponentGovernance'
import {
  findDatasourceOption,
  firstDatasourceForEngine,
  groupGovernanceDatasources
} from '../common/governanceDatasourceOptions.mjs'
import { riskDisplayText as sharedRiskDisplayText } from '../common/issueSceneHelp.mjs'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()

const combinedTerminalStatuses = new Set(['ACCESS_SUCCEEDED', 'PARTIAL_SUCCEEDED', 'FAILED'])
const DEEP_PARSE_SESSION_PREFIX = 'sqlforge:query-analysis:deep-parse:'
const initialTenantId = resolveRuntimeTenantId()
const initialDatasourceCode = resolveRuntimeDatasourceCode()

const form = reactive({
  tenantId: initialTenantId,
  datasourceCode: initialDatasourceCode,
  bindingMode: 'POSITIONAL',
  parserMode: 'APACHE_CALCITE',
  connectionRequired: true,
  sqlText: "SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id",
  sqlTemplateText: '',
  bindParametersText: '{\n  "limit": 100\n}',
  commentContextText: '{\n  "report_code": "RPT_SALES_DAILY",\n  "stage": "PROD",\n  "engine_hint": "hetu"\n}'
})

const parseBatchForm = reactive({
  tenantId: initialTenantId,
  batchName: 'batch-alpha',
  importMode: 'TABULAR_FILE',
  fileType: 'CSV',
  templateVersion: 'v1',
  datasourceCode: initialDatasourceCode,
  parserMode: 'APACHE_CALCITE',
  structureParseOnly: false,
  directInputMode: 'SQL_LINES',
  rawContent:
    "SELECT * FROM orders WHERE dt = '2026-04-01';\nSELECT * FROM vw_orders WHERE dt = '2026-04-02';"
})

const retryForm = reactive({
  failureFilter: 'UNAVAILABLE',
  datasourceCode: initialDatasourceCode,
  forceRecheckAvailability: false
})

const reportBatchForm = reactive({
  tenantId: initialTenantId,
  batchName: 'report-batch-alpha',
  fileType: 'TXT',
  reportCodeField: 'report_code',
  datasourceCode: initialDatasourceCode,
  parserMode: 'APACHE_CALCITE',
  stage: 'PROD',
  priority: 'high',
  rawContent: buildReportImportRows({ datasourceCode: initialDatasourceCode })
})

const running = ref(false)
const lastRunMode = ref('combined')
const parseResult = ref(null)
const rewriteTrialRun = ref(null)
const errorMessage = ref('')

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

const governanceDatasources = ref([])

const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)
const fieldHelpDialogVisible = ref(false)
const fieldHelpDialogTitle = ref('')
const fieldHelpDialogMessage = ref('')

const loading = reactive({
  createParseBatch: false,
  ingestParseBatch: false,
  refreshParseBatch: false,
  retryParseBatch: false,
  importReportBatch: false,
  resolveReportBatch: false,
  refreshReportBatch: false,
  rewriteTrial: false
})

const isChinese = computed(() => locale.value === 'zh-CN')
const isRewriteValidationEntry = computed(() => route.query.mode === 'rewriteValidation')
const pageEyebrow = computed(() =>
  isRewriteValidationEntry.value
    ? t('acceleration.rewriteValidationEyebrow')
    : t('acceleration.eyebrow')
)
const pageTitle = computed(() =>
  isRewriteValidationEntry.value
    ? t('acceleration.rewriteValidationTitle')
    : t('acceleration.title')
)
const pageSummary = computed(() =>
  isRewriteValidationEntry.value
    ? t('acceleration.rewriteValidationSummary')
    : t('acceleration.summary')
)
const bindingModeOptions = ['POSITIONAL', 'NAMED']
const parserModeOptions = [
  { label: 'Apache Calcite', value: 'APACHE_CALCITE' },
  { label: 'Apache Calcite + Hetu EXPLAIN', value: 'APACHE_CALCITE_WITH_PLAN' }
]
const datasourceOptions = computed(() => buildDatasourceOptions(governanceDatasources.value))
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
    { key: 'parseTaskId', label: t('inline.viewsOptimizationAccelerationView.text001'), value: structureParse.value.parseTaskId },
    { key: 'sqlFingerprint', label: t('inline.viewsOptimizationAccelerationView.text002'), value: structureParse.value.sqlFingerprint },
    { key: 'analysisStatus', label: t('inline.viewsOptimizationAccelerationView.text003'), value: structureParse.value.analysisStatus },
    { key: 'structureAnalysisStatus', label: t('inline.viewsOptimizationAccelerationView.text004'), value: structureParse.value.structureAnalysisStatus },
    { key: 'syntaxStatus', label: t('inline.viewsOptimizationAccelerationView.text005'), value: structureParse.value.syntaxStatus },
    { key: 'complexityLevel', label: t('inline.viewsOptimizationAccelerationView.text006'), value: structureParse.value.complexityLevel },
    { key: 'sqlType', label: t('inline.viewsOptimizationAccelerationView.text007'), value: structureParse.value.sqlType },
    { key: 'priorityLevel', label: t('inline.viewsOptimizationAccelerationView.text008'), value: structureParse.value.priorityLevel },
    { key: 'priorityScore', label: t('inline.viewsOptimizationAccelerationView.text009'), value: structureParse.value.priorityScore },
    { key: 'important', label: t('inline.viewsOptimizationAccelerationView.text010'), value: booleanLabel(structureParse.value.important) },
    { key: 'urgent', label: t('inline.viewsOptimizationAccelerationView.text011'), value: booleanLabel(structureParse.value.urgent) },
    { key: 'failureReason', label: t('inline.viewsOptimizationAccelerationView.text012'), value: structureParse.value.failureReason },
    {
      key: 'failurePosition',
      label: t('inline.viewsOptimizationAccelerationView.text013'),
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
    { key: 'parserEngine', label: t('inline.viewsOptimizationAccelerationView.text014'), value: feature.parserEngine },
    { key: 'scanMode', label: t('inline.viewsOptimizationAccelerationView.text015'), value: feature.scanMode },
    { key: 'joinType', label: t('inline.viewsOptimizationAccelerationView.text016'), value: feature.joinType },
    { key: 'computeDensity', label: t('inline.viewsOptimizationAccelerationView.text017'), value: feature.computeDensity },
    { key: 'resourceType', label: t('inline.viewsOptimizationAccelerationView.text018'), value: feature.resourceType },
    { key: 'slaLevel', label: t('inline.viewsOptimizationAccelerationView.text019'), value: feature.slaLevel },
    { key: 'tableCount', label: t('inline.viewsOptimizationAccelerationView.text020'), value: feature.tableCount },
    { key: 'joinCount', label: t('inline.viewsOptimizationAccelerationView.text021'), value: feature.joinCount },
    { key: 'predicateCount', label: t('inline.viewsOptimizationAccelerationView.text022'), value: feature.predicateCount },
    { key: 'orderByExpressionCount', label: t('inline.viewsOptimizationAccelerationView.text023'), value: feature.orderByExpressionCount },
    { key: 'duplicateOrderByKeyCount', label: t('inline.viewsOptimizationAccelerationView.text024'), value: feature.duplicateOrderByKeyCount },
    { key: 'duplicateGroupByKeyCount', label: t('inline.viewsOptimizationAccelerationView.text025'), value: feature.duplicateGroupByKeyCount },
    { key: 'groupByWithoutAggregate', label: t('inline.viewsOptimizationAccelerationView.text026'), value: booleanLabel(feature.groupByWithoutAggregate) },
    { key: 'aggregateFunctionCount', label: t('inline.viewsOptimizationAccelerationView.text027'), value: feature.aggregateFunctionCount },
    { key: 'stringProjectionCount', label: t('inline.viewsOptimizationAccelerationView.text028'), value: feature.stringProjectionCount },
    { key: 'stringConcatenationCount', label: t('inline.viewsOptimizationAccelerationView.text029'), value: feature.stringConcatenationCount },
    { key: 'largeStringAggregateCount', label: t('inline.viewsOptimizationAccelerationView.text030'), value: feature.largeStringAggregateCount },
    { key: 'repeatedSubqueryCount', label: t('inline.viewsOptimizationAccelerationView.text031'), value: feature.repeatedSubqueryCount },
    { key: 'windowFunctionCount', label: t('inline.viewsOptimizationAccelerationView.text032'), value: feature.windowFunctionCount },
    { key: 'repeatedExpressionCount', label: t('inline.viewsOptimizationAccelerationView.text033'), value: feature.repeatedExpressionCount }
  ].filter(item => hasDisplayValue(item.value))
})

const structureResourceHighlights = computed(() => {
  const estimate = structureParse.value?.estimatedResourceCost
  if (!estimate) {
    return []
  }
  return [
    { key: 'overall', label: t('inline.viewsOptimizationAccelerationView.text034'), value: estimate.overall },
    { key: 'cpu', label: 'CPU', value: estimate.cpu },
    { key: 'io', label: 'IO', value: estimate.io },
    { key: 'memory', label: t('inline.viewsOptimizationAccelerationView.text035'), value: estimate.memory },
    { key: 'network', label: t('inline.viewsOptimizationAccelerationView.text036'), value: estimate.network },
    { key: 'resultSize', label: t('inline.viewsOptimizationAccelerationView.text037'), value: estimate.resultSize }
  ].filter(item => hasDisplayValue(item.value))
})

const accessHighlights = computed(() => {
  if (!accessParse.value) {
    return []
  }
  return [
    { key: 'serviceStatus', label: t('inline.viewsOptimizationAccelerationView.text038'), value: accessParse.value.serviceStatus },
    { key: 'connectionStatus', label: t('inline.viewsOptimizationAccelerationView.text039'), value: accessParse.value.connectionStatus },
    { key: 'objectResolutionStatus', label: t('inline.viewsOptimizationAccelerationView.text040'), value: accessParse.value.objectResolutionStatus },
    { key: 'partitionStatus', label: t('inline.viewsOptimizationAccelerationView.text041'), value: accessParse.value.partitionStatus },
    { key: 'dataFreshnessStatus', label: t('inline.viewsOptimizationAccelerationView.text042'), value: accessParse.value.dataFreshnessStatus },
    { key: 'slaStatus', label: t('inline.viewsOptimizationAccelerationView.text043'), value: accessParse.value.slaStatus },
    { key: 'compatibilityStatus', label: t('inline.viewsOptimizationAccelerationView.text044'), value: accessParse.value.compatibilityStatus }
  ].filter(item => hasDisplayValue(item.value))
})

const planHighlights = computed(() => {
  if (!planAnalysis.value) {
    return []
  }
  return [
    { key: 'planStatus', label: t('inline.viewsOptimizationAccelerationView.text045'), value: planAnalysis.value.status },
    { key: 'datasourceCode', label: t('inline.viewsOptimizationAccelerationView.text046'), value: planAnalysis.value.datasourceCode },
    { key: 'costMs', label: t('inline.viewsOptimizationAccelerationView.text047'), value: planAnalysis.value.costMs },
    { key: 'failureReason', label: t('inline.viewsOptimizationAccelerationView.text048'), value: planAnalysis.value.failureReason }
  ].filter(item => hasDisplayValue(item.value))
})

const summaryCards = computed(() => {
  if (!parseResult.value) {
    return []
  }
  return [
    {
      label: t('inline.viewsOptimizationAccelerationView.text049'),
      value: lastRunMode.value === 'combined'
        ? (t('inline.viewsOptimizationAccelerationView.text050'))
        : (t('inline.viewsOptimizationAccelerationView.text051'))
    },
    { label: t('inline.viewsOptimizationAccelerationView.text052'), value: structureParse.value?.featureSummary?.parserEngine || form.parserMode },
    { label: t('inline.viewsOptimizationAccelerationView.text053'), value: structureParse.value?.analysisStatus },
    { label: t('inline.viewsOptimizationAccelerationView.text054'), value: structureParse.value?.planAnalysis?.status },
    { label: t('inline.viewsOptimizationAccelerationView.text055'), value: activeConclusion.value?.overallStatus || activeStatus.value },
    { label: t('inline.viewsOptimizationAccelerationView.text056'), value: booleanLabel(activeConclusion.value?.accessAvailable) },
    { label: t('inline.viewsOptimizationAccelerationView.text057'), value: activeConclusion.value?.degradeReason || parseResult.value?.degradeReason },
    {
      label: t('inline.viewsOptimizationAccelerationView.text058'),
      value: parseResult.value.historyPersisted ? (t('inline.viewsOptimizationAccelerationView.text059')) : parseResult.value.historyPersistenceStatus
    },
    { label: 'History ID', value: parseResult.value.historyId }
  ].filter(item => hasDisplayValue(item.value))
})

const rewriteTrialItems = computed(() => Array.isArray(rewriteTrialRun.value?.items) ? rewriteTrialRun.value.items : [])
const primaryRewriteTrialItem = computed(() => rewriteTrialItems.value[0] || null)
const canCreateRewriteTrial = computed(() =>
  Boolean(parseResult.value && hasDisplayValue(form.sqlText) && (structureParse.value || parseResult.value.parseTaskId))
)
const rewriteTrialCards = computed(() => {
  if (!rewriteTrialRun.value) {
    return []
  }
  return [
    card(t('rewriteTrial.status'), rewriteTrialRun.value.trialStatus),
    card(t('rewriteTrial.acceptedSql'), rewriteTrialRun.value.acceptedCount),
    card(t('rewriteTrial.candidateGenerated'), rewriteTrialRun.value.candidateGeneratedCount || rewriteTrialRun.value.recommendedCount),
    card(t('rewriteTrial.noSafeRewrite'), rewriteTrialRun.value.noSafeRewriteCount),
    card(t('rewriteTrial.recommendationId'), primaryRewriteTrialItem.value?.recommendationId)
  ].filter(item => hasDisplayValue(item.value))
})

const requestSummary = computed(() => [
  { label: t('inline.viewsOptimizationAccelerationView.text060'), value: form.tenantId },
  { label: t('inline.viewsOptimizationAccelerationView.text061'), value: form.datasourceCode || (t('inline.viewsOptimizationAccelerationView.text062')) },
  { label: t('inline.viewsOptimizationAccelerationView.text063'), value: form.bindingMode },
  { label: t('inline.viewsOptimizationAccelerationView.text064'), value: form.parserMode },
  { label: t('inline.viewsOptimizationAccelerationView.text065'), value: form.connectionRequired ? 'ON' : 'OFF' }
])

const parseBatchStatusCards = computed(() => {
  if (!parseBatchDetail.value) {
    return []
  }
  return [
    card(t('inline.viewsOptimizationAccelerationView.text066'), parseBatchDetail.value.status),
    card(t('inline.viewsOptimizationAccelerationView.text067'), parseBatchDetail.value.parserMode),
    card(t('inline.viewsOptimizationAccelerationView.text068'), parseBatchDetail.value.totalRecords),
    card(t('inline.viewsOptimizationAccelerationView.text069'), parseBatchDetail.value.successRecords),
    card(t('inline.viewsOptimizationAccelerationView.text070'), parseBatchDetail.value.partialSuccessRecords),
    card(t('inline.viewsOptimizationAccelerationView.text071'), parseBatchDetail.value.failedRecords),
    card(t('inline.viewsOptimizationAccelerationView.text072'), formatNumber(parseBatchDetail.value.structureParseSuccessRate)),
    card(t('inline.viewsOptimizationAccelerationView.text073'), formatNumber(parseBatchDetail.value.accessParseSuccessRate)),
    card(t('inline.viewsOptimizationAccelerationView.text074'), formatNumber(parseBatchDetail.value.planAnalysisStatistics?.successRate))
  ].filter(item => hasDisplayValue(item.value))
})

const reportBatchStatusCards = computed(() => {
  if (!reportBatchDetail.value) {
    return []
  }
  return [
    card(t('inline.viewsOptimizationAccelerationView.text075'), reportBatchDetail.value.status),
    card(t('inline.viewsOptimizationAccelerationView.text076'), reportBatchDetail.value.parserMode),
    card(t('inline.viewsOptimizationAccelerationView.text077'), reportBatchDetail.value.totalReports),
    card(t('inline.viewsOptimizationAccelerationView.text078'), reportBatchDetail.value.resolvedReports),
    card(t('inline.viewsOptimizationAccelerationView.text079'), reportBatchDetail.value.failedReports),
    card(t('inline.viewsOptimizationAccelerationView.text080'), formatNumber(reportBatchDetail.value.planAnalysisStatistics?.successRate)),
    card(t('inline.viewsOptimizationAccelerationView.text081'), reportBatchDetail.value.stage),
    card(t('inline.viewsOptimizationAccelerationView.text082'), reportBatchDetail.value.priority)
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

const parseSessionsSummary = computed(() => `${parseBatchSessions.value.length} ${t('inline.viewsOptimizationAccelerationView.text083')}`)
const reportSessionsSummary = computed(() => `${reportBatchSessions.value.length} ${t('inline.viewsOptimizationAccelerationView.text084')}`)

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
    parseTaskId: t('inline.viewsOptimizationAccelerationView.text085'),
    sqlFingerprint: t('inline.viewsOptimizationAccelerationView.text086'),
    syntaxStatus: t('inline.viewsOptimizationAccelerationView.text087'),
    complexityLevel: t('inline.viewsOptimizationAccelerationView.text088'),
    sqlType: t('inline.viewsOptimizationAccelerationView.text089'),
    priorityLevel: t('inline.viewsOptimizationAccelerationView.text090'),
    priorityScore: t('inline.viewsOptimizationAccelerationView.text091'),
    urgent: t('inline.viewsOptimizationAccelerationView.text092'),
    important: t('inline.viewsOptimizationAccelerationView.text093'),
    parserEngine: t('inline.viewsOptimizationAccelerationView.text094'),
    scanMode: t('inline.viewsOptimizationAccelerationView.text095'),
    joinType: t('inline.viewsOptimizationAccelerationView.text096'),
    computeDensity: t('inline.viewsOptimizationAccelerationView.text097'),
    slaLevel: t('inline.viewsOptimizationAccelerationView.text098'),
    serviceStatus: t('inline.viewsOptimizationAccelerationView.text099'),
    connectionStatus: t('inline.viewsOptimizationAccelerationView.text100'),
    objectResolutionStatus: t('inline.viewsOptimizationAccelerationView.text101'),
    compatibilityStatus: t('inline.viewsOptimizationAccelerationView.text102')
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

function sourceProblemScene(value) {
  if (!value) {
    return ''
  }
  if (typeof value === 'string') {
    return value
  }
  return value.issueScene || value.riskCode || value.issueCode || value.rewriteCandidate || value.candidateCode || ''
}

function sourceProblemSummary(value) {
  if (!value || typeof value === 'string') {
    return ''
  }
  return localizedDisplayText(value.summary || value.detail || value.evidence || value.suggestedAction || '')
}

function buildSourceProblem(value, problemType = 'ISSUE_SCENE') {
  const issueScene = sourceProblemScene(value)
  if (!hasDisplayValue(issueScene)) {
    return null
  }
  return {
    problemType,
    issueScene,
    issueCode: typeof value === 'object' ? value.issueCode || value.riskCode || '' : '',
    severity: typeof value === 'object' ? value.severity || '' : '',
    priorityLevel: typeof value === 'object' ? value.priorityLevel || structureParse.value?.priorityLevel || '' : structureParse.value?.priorityLevel || '',
    summary: sourceProblemSummary(value),
    evidenceRef: {
      parseTaskId: parseResult.value?.parseTaskId || structureParse.value?.parseTaskId,
      parseHistoryId: parseResult.value?.parseHistoryId || parseResult.value?.historyId || structureParse.value?.parseHistoryId,
      historyId: parseResult.value?.historyId || structureParse.value?.historyId
    }
  }
}

function buildRewriteTrialSourceProblems(focusProblem = null) {
  if (focusProblem) {
    return [buildSourceProblem(focusProblem)].filter(Boolean)
  }
  const problems = []
  ;(structureParse.value?.riskTags || []).forEach(item => problems.push(buildSourceProblem(item)))
  ;(structureParse.value?.rewriteCandidates || []).forEach(item => problems.push(buildSourceProblem(item, 'REWRITE_CANDIDATE')))
  structureRiskChecklist.value.forEach(item => problems.push(buildSourceProblem(item)))
  structureIssues.value.forEach(item => problems.push(buildSourceProblem(item)))
  const seen = new Set()
  return problems.filter(item => {
    if (!item || seen.has(item.issueScene)) {
      return false
    }
    seen.add(item.issueScene)
    return true
  })
}

function sourceProblemLabel(item) {
  return item?.issueScene || item?.sourceIssueScene || item?.problemType || '-'
}

async function createRewriteTrialFromParseResult(focusProblem = null) {
  if (!canCreateRewriteTrial.value) {
    return
  }
  loading.rewriteTrial = true
  errorMessage.value = ''
  try {
    rewriteTrialRun.value = await createRewriteTrial({
      tenantId: form.tenantId,
      sqlText: form.sqlText,
      datasourceCode: form.datasourceCode,
      sourceKind: lastRunMode.value === 'combined' ? 'COMBINED_PARSE' : 'STRUCTURE_PARSE',
      sourceId: parseResult.value?.historyId || parseResult.value?.parseTaskId || structureParse.value?.parseTaskId,
      parseTaskId: parseResult.value?.parseTaskId || structureParse.value?.parseTaskId,
      parseHistoryId: parseResult.value?.parseHistoryId || parseResult.value?.historyId || structureParse.value?.parseHistoryId,
      historyId: parseResult.value?.historyId || structureParse.value?.historyId,
      sourceProblems: buildRewriteTrialSourceProblems(focusProblem)
    }, {
      requestPrefix: 'frontend-parse-workbench-rewrite-trial'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.rewriteTrial = false
  }
}

function openRewriteTrialRecommendation(item = primaryRewriteTrialItem.value) {
  if (!item?.recommendationId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.recommendationCenter,
    query: compactQuery({
      tenantId: form.tenantId,
      recommendationId: item.recommendationId,
      sourceCategory: 'SQL_PARSE',
      historyId: parseResult.value?.historyId,
      parseTaskId: parseResult.value?.parseTaskId || structureParse.value?.parseTaskId
    })
  })
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

function formatTimestamp(value) {
  return formatBeijingTimestamp(value)
}

function formatInstant(value) {
  return formatBeijingTimestamp(value)
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
    parserMode: String(form.parserMode || 'APACHE_CALCITE').trim(),
    connectionRequired: Boolean(form.connectionRequired)
  }
  const sqlTemplateText = String(form.sqlTemplateText || '').trim()
  if (sqlTemplateText) {
    payload.sqlTemplateText = sqlTemplateText
  }
  const bindParameters = parseJsonInput(form.bindParametersText, t('inline.viewsOptimizationAccelerationView.text103'))
  if (Object.keys(bindParameters).length > 0) {
    payload.bindParameters = bindParameters
  }
  const commentContext = parseJsonInput(form.commentContextText, t('inline.viewsOptimizationAccelerationView.text104'))
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
    historyId: structureOnlyResult.historyId || structureOnlyResult.parseHistoryId || '',
    parseHistoryId: structureOnlyResult.parseHistoryId || structureOnlyResult.historyId || '',
    status: 'STRUCTURE_ONLY',
    structureParse: structureOnlyResult,
    accessParse: null,
    degradeReason: 'STRUCTURE_ONLY_MODE',
    conclusion: {
      overallStatus: 'STRUCTURE_ONLY',
      summary: t('inline.viewsOptimizationAccelerationView.text105'),
      recommendedAction: t('inline.viewsOptimizationAccelerationView.text106'),
      structureAvailable: true,
      accessAvailable: false,
      degradeReason: 'STRUCTURE_ONLY_MODE'
    },
    statusHistory: [
      {
        status: 'STRUCTURE_SUCCEEDED',
        note: t('inline.viewsOptimizationAccelerationView.text107'),
        occurredAtEpochMs: Date.now()
      }
    ]
  }
}

function resetResult() {
  parseResult.value = null
  rewriteTrialRun.value = null
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

function compactQuery(query) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== '')
  )
}

function openRecommendationResultsForParseResult() {
  if (!parseResult.value) {
    return
  }
  const parseTaskId = parseResult.value.parseTaskId || structureParse.value?.parseTaskId || ''
  const historyId =
    parseResult.value.historyId ||
    parseResult.value.parseHistoryId ||
    structureParse.value?.historyId ||
    structureParse.value?.parseHistoryId ||
    ''
  const sourceObjectId = historyId || parseTaskId
  router.push({
    path: ROUTE_PATHS.recommendationCenter,
    query: compactQuery({
      tenantId: form.tenantId,
      sourceCategory: 'SQL_PARSE',
      sourceObjectId,
      historyId,
      parseTaskId,
      sourceType: 'PARSE',
      sourceKind: lastRunMode.value === 'combined' ? 'COMBINED_PARSE' : 'STRUCTURE_PARSE'
    })
  })
}

async function runStructurePreview() {
  running.value = true
  lastRunMode.value = 'structure'
  errorMessage.value = ''
  rewriteTrialRun.value = null
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
  if (form.connectionRequired && !hasDisplayValue(form.datasourceCode)) {
    return
  }
  running.value = true
  lastRunMode.value = 'combined'
  errorMessage.value = ''
  rewriteTrialRun.value = null
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
        return parseBatchForm.datasourceCode || reportBatchForm.datasourceCode || ''
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
  if (!hasDisplayValue(parseBatchForm.datasourceCode)) {
    return
  }
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
    errorMessage.value = t('inline.viewsOptimizationAccelerationView.text108')
    return
  }
  downloadTextFile(`${parseBatchDetail.value.batchId || 'parse-batch-template'}.csv`, parseTemplatePreview.value)
}

async function ingestParseBatchFlow() {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = t('inline.viewsOptimizationAccelerationView.text109')
    return
  }
  loading.ingestParseBatch = true
  clearBatchError()
  try {
    const payload = await loadPayloadBase64(
      parseUploadFile.value,
      parseBatchForm.rawContent,
      t('inline.viewsOptimizationAccelerationView.text110')
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
    errorMessage.value = t('inline.viewsOptimizationAccelerationView.text111')
    return
  }
  if (!hasDisplayValue(retryForm.datasourceCode)) {
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
  if (!hasDisplayValue(reportBatchForm.datasourceCode)) {
    return
  }
  loading.importReportBatch = true
  clearBatchError()
  try {
    const payload = await loadReportPayloadBase64(
      reportUploadFile.value,
      reportBatchForm.rawContent,
      t('inline.viewsOptimizationAccelerationView.text112')
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
    errorMessage.value = t('inline.viewsOptimizationAccelerationView.text113')
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

function statisticsRedirectQuery() {
  const nextQuery = { ...route.query }
  delete nextQuery.workspace
  return {
    ...nextQuery,
    analytics: String(route.query.analytics || 'issue')
  }
}

function firstRouteValue(value) {
  if (Array.isArray(value)) {
    return value[0]
  }
  return value
}

function readDeepParseSeed() {
  const seedKey = String(firstRouteValue(route.query.seedKey) || '').trim()
  if (!seedKey) {
    return {}
  }
  try {
    return JSON.parse(window.sessionStorage?.getItem(`${DEEP_PARSE_SESSION_PREFIX}${seedKey}`) || '{}')
  } catch {
    return {}
  }
}

function applyRouteSingleSqlContext() {
  const seed = readDeepParseSeed()
  const tenantId = String(seed.tenantId || firstRouteValue(route.query.tenantId) || '').trim()
  const datasourceCode = String(seed.datasourceCode || firstRouteValue(route.query.datasourceCode) || '').trim()
  const parserMode = String(seed.parserMode || firstRouteValue(route.query.parserMode) || '').trim()
  const sqlText = String(seed.sqlText || firstRouteValue(route.query.sqlText) || '').trim()
  if (tenantId) {
    form.tenantId = tenantId
  }
  if (datasourceCode) {
    form.datasourceCode = datasourceCode
  }
  if (parserMode) {
    form.parserMode = parserMode
  }
  if (sqlText) {
    form.sqlText = sqlText
  }
}

async function loadGovernanceDatasources() {
  try {
    const response = await getGovernanceDatasources(form.tenantId, {
      requestPrefix: 'frontend-parse-workbench-governance-datasources'
    })
    governanceDatasources.value = Array.isArray(response) ? response : []
    const grouped = groupGovernanceDatasources(governanceDatasources.value)
    const selectedDatasource =
      findDatasourceOption(grouped, form.datasourceCode) || firstDatasourceForEngine(grouped)
    if (selectedDatasource && selectedDatasource.datasourceCode !== form.datasourceCode) {
      form.datasourceCode = selectedDatasource.datasourceCode
    }
  } catch (error) {
    governanceDatasources.value = []
  }
}

function applyRouteWorkspace() {
  const workspace = String(route.query.workspace || '').trim()
  if (workspace === 'batch') {
    router.replace({ path: ROUTE_PATHS.parseBatchCenter })
    return
  }
  if (workspace === 'statistics') {
    router.replace({
      path: ROUTE_PATHS.parseStatisticsCenter,
      query: statisticsRedirectQuery()
    })
    return
  }
  if (workspace === 'history') {
    router.replace({ path: ROUTE_PATHS.parseRecord })
  }
}

watch(
  () => route.query,
  () => {
    applyRouteSingleSqlContext()
    applyRouteWorkspace()
  }
)

onMounted(async () => {
  applyRouteSingleSqlContext()
  parseBatchForm.tenantId = form.tenantId
  reportBatchForm.tenantId = form.tenantId
  retryForm.datasourceCode = form.datasourceCode
  parseBatchForm.datasourceCode = form.datasourceCode
  reportBatchForm.datasourceCode = form.datasourceCode
  parseBatchForm.parserMode = form.parserMode
  reportBatchForm.parserMode = form.parserMode
  applyRouteWorkspace()
  await loadGovernanceDatasources()
})

watch(
  () => form.tenantId,
  value => {
    parseBatchForm.tenantId = value
    reportBatchForm.tenantId = value
    loadGovernanceDatasources()
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
  <RewriteValidationView v-if="isRewriteValidationEntry" />
  <section v-else class="runtime-page parse-workbench-page" data-testid="parse-workbench-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">{{ pageEyebrow }}</p>
        <h1 class="runtime-title" data-testid="parse-workbench-title">{{ pageTitle }}</h1>
        <p class="runtime-summary" data-testid="parse-workbench-summary">{{ pageSummary }}</p>
        <p
          v-if="isRewriteValidationEntry"
          class="runtime-summary runtime-summary-compact"
          data-testid="parse-workbench-rewrite-validation-boundary"
        >
          {{ t('acceleration.rewriteValidationBoundary') }}
        </p>
      </div>
      <div class="hero-side">
        <div class="action-row action-row-wrap">
          <el-button type="primary" @click="resetResult">
            {{ t('inline.viewsOptimizationAccelerationView.text114') }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="parse-workbench__grid">
      <article class="surface-card composer-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">single sql input</p>
            <h2 class="section-title">{{ t('inline.viewsOptimizationAccelerationView.text115') }}</h2>
          </div>
        </div>

        <div class="summary-chip-row">
          <span v-for="item in requestSummary" :key="item.label" class="summary-chip">
            {{ item.label }}: <strong>{{ item.value }}</strong>
          </span>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text116') }}</span>
            <el-input v-model="form.tenantId" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text117') }}</span>
            <el-select
              v-model="form.datasourceCode"
              filterable
              allow-create
              default-first-option
              :placeholder="t('inline.viewsOptimizationAccelerationView.text118')"
              data-testid="parse-workbench-datasource-code"
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
            <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text119') }}</span>
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
            <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text120') }}</span>
            <el-switch v-model="form.connectionRequired" />
          </label>

          <div class="field-block field-block-wide">
            <SqlEditorField
              v-model="form.sqlText"
              label="SQL"
              :rows="8"
              :copy-label="t('inline.viewsOptimizationAccelerationView.text121')"
              :format-label="t('inline.viewsOptimizationAccelerationView.text122')"
              data-testid="parse-workbench-sql-input"
            />
          </div>

          <div class="field-block field-block-wide">
            <SqlEditorField
              v-model="form.sqlTemplateText"
              :label="t('inline.viewsOptimizationAccelerationView.text123')"
              :rows="4"
              :copy-label="t('inline.viewsOptimizationAccelerationView.text124')"
              :format-label="t('inline.viewsOptimizationAccelerationView.text125')"
              data-testid="parse-workbench-template-sql-input"
            />
          </div>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text126') }}</span>
            <el-input v-model="form.bindParametersText" type="textarea" :rows="5" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text127') }}</span>
            <el-input v-model="form.commentContextText" type="textarea" :rows="6" />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :disabled="form.connectionRequired && !form.datasourceCode"
            :loading="running && lastRunMode === 'combined'"
            data-testid="parse-workbench-submit"
            @click="runCombinedParseFlow"
          >
            {{ t('inline.viewsOptimizationAccelerationView.text128') }}
          </el-button>
          <el-button
            :loading="running && lastRunMode === 'structure'"
            data-testid="parse-workbench-structure-preview"
            @click="runStructurePreview"
          >
            {{ t('inline.viewsOptimizationAccelerationView.text129') }}
          </el-button>
          <el-button
            :disabled="!parseResult?.parseTaskId || lastRunMode !== 'combined'"
            :loading="running && lastRunMode === 'combined'"
            data-testid="parse-workbench-refresh-status"
            @click="refreshParseStatus"
          >
            {{ t('inline.viewsOptimizationAccelerationView.text130') }}
          </el-button>
          <el-button @click="resetResult">
            {{ t('inline.viewsOptimizationAccelerationView.text131') }}
          </el-button>
        </div>

        <div class="hint-card">
          <strong>{{ t('inline.viewsOptimizationAccelerationView.text132') }}</strong>
          <p>
            {{
              t('inline.viewsOptimizationAccelerationView.text133')
            }}
          </p>
        </div>
      </article>

      <article class="surface-card evidence-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">parse result</p>
            <h2 class="section-title">{{ t('inline.viewsOptimizationAccelerationView.text134') }}</h2>
            <p class="section-summary">
              {{ t('inline.viewsOptimizationAccelerationView.text135') }}
            </p>
          </div>
          <el-button v-if="parseResult" text @click="openEvidenceDrawer(t('inline.viewsOptimizationAccelerationView.text136'), parseResult)">
            {{ t('inline.viewsOptimizationAccelerationView.text137') }}
          </el-button>
          <el-button v-if="parseResult?.historyId" text data-testid="parse-workbench-open-history" @click="openParseHistoryDetail">
            {{ t('inline.viewsOptimizationAccelerationView.text138') }}
          </el-button>
          <el-button v-if="parseResult" text data-testid="parse-workbench-open-recommendations" @click="openRecommendationResultsForParseResult">
            {{ t('inline.viewsOptimizationAccelerationView.text246') }}
          </el-button>
          <el-button
            v-if="parseResult"
            text
            :disabled="!canCreateRewriteTrial"
            :loading="loading.rewriteTrial"
            data-testid="parse-workbench-create-rewrite-trial"
            @click="createRewriteTrialFromParseResult()"
          >
            {{ t('rewriteTrial.create') }}
          </el-button>
        </div>

        <p v-if="!parseResult && !errorMessage" class="empty-state">
          {{
            t('inline.viewsOptimizationAccelerationView.text139')
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
              <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text140') }}</span>
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
            <article v-if="rewriteTrialRun" class="parse-card" data-testid="parse-workbench-rewrite-trial-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">rewrite trial</p>
                  <h3 class="detail-title">{{ t('rewriteTrial.title') }}</h3>
                </div>
                <span class="status-pill">{{ rewriteTrialRun.trialStatus || '-' }}</span>
              </div>
              <div class="highlight-grid highlight-grid-compact">
                <div v-for="item in rewriteTrialCards" :key="item.label" class="highlight-chip">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
              <div class="issue-list issue-list-compact">
                <article
                  v-for="item in rewriteTrialItems"
                  :key="item.trialItemId"
                  class="issue-card"
                  data-testid="parse-workbench-rewrite-trial-item"
                >
                  <div class="issue-card__header">
                    <strong>{{ item.trialStatus }}</strong>
                    <span>{{ item.validationStatus || 'NOT_VALIDATED' }}</span>
                  </div>
                  <p class="issue-card__summary">
                    {{ t('rewriteTrial.sourceProblems') }}: {{ displayValue((item.sourceProblems || []).map(sourceProblemLabel)) }}
                  </p>
                  <p class="issue-card__detail">
                    {{ t('rewriteTrial.issueRuleLinks') }}: {{ displayValue((item.issueRuleLinks || []).map(link => `${link.sourceIssueScene || '-'} / ${link.ruleCode || '-'} / ${link.trialConclusion || '-'}`)) }}
                  </p>
                  <p v-if="item.failureReason" class="issue-card__detail">{{ item.failureReason }}</p>
                  <SqlCodeBlock
                    v-if="item.candidateSql"
                    :value="item.candidateSql"
                    :label="t('rewriteTrial.candidateSql')"
                    :copy-label="t('common.actions.copy')"
                    compact
                    data-testid="parse-workbench-rewrite-trial-candidate-sql"
                  />
                  <div class="item-actions">
                    <el-button text :disabled="!item.recommendationId" @click="openRewriteTrialRecommendation(item)">
                      {{ t('rewriteTrial.recommendationDetail') }}
                    </el-button>
                  </div>
                </article>
              </div>
            </article>

            <article class="parse-card" data-testid="parse-workbench-structure-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">structure parse</p>
                  <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text141') }}</h3>
                </div>
                <span class="status-pill" :class="structureParse?.syntaxStatus === 'VALID' ? 'status-pill-success' : 'status-pill-warning'">
                  {{ structureParse?.syntaxStatus || '-' }}
                </span>
              </div>

              <p v-if="!structureParse" class="empty-state">
                {{ t('inline.viewsOptimizationAccelerationView.text142') }}
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
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text143') }}</span>
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
                    {{ t('inline.viewsOptimizationAccelerationView.text144') }}
                  </p>
                </div>

                <div class="mini-section" data-testid="parse-workbench-feature-summary">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text145') }}</span>
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
                    {{ t('inline.viewsOptimizationAccelerationView.text146') }}
                  </p>
                </div>

                <div class="mini-section" data-testid="parse-workbench-resource-estimate">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text147') }}</span>
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
                    {{ t('inline.viewsOptimizationAccelerationView.text148') }}
                  </p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text149') }}</span>
                  <div class="summary-chip-row">
                    <span class="summary-chip">
                      {{ t('inline.viewsOptimizationAccelerationView.text150') }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateStart || '-' }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ t('inline.viewsOptimizationAccelerationView.text151') }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateEnd || '-' }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ t('inline.viewsOptimizationAccelerationView.text152') }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateStatus || '-' }}</strong>
                    </span>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text153') }}</span>
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
                    {{ t('inline.viewsOptimizationAccelerationView.text154') }}
                  </p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text155') }}</span>
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
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text156') }}</span>
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
                      <p class="issue-card__detail">{{ t('inline.viewsOptimizationAccelerationView.text157') }}: {{ riskDisplayText(risk, 'suggestedAction') }}</p>
                      <div class="item-actions">
                        <el-button
                          text
                          :loading="loading.rewriteTrial"
                          data-testid="parse-workbench-risk-rewrite-trial"
                          @click="createRewriteTrialFromParseResult(risk)"
                        >
                          {{ t('rewriteTrial.trial') }}
                        </el-button>
                      </div>
                    </article>
                  </div>
                  <p v-else class="empty-inline">
                    {{ t('inline.viewsOptimizationAccelerationView.text158') }}
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
                      {{ t('inline.viewsOptimizationAccelerationView.text159') }}:
                      <span v-if="issue.failureLine && issue.failureColumn">line {{ issue.failureLine }}, column {{ issue.failureColumn }}</span>
                      <span v-if="issue.failureToken"> · token {{ issue.failureToken }}</span>
                      <span v-if="issue.failureSnippet"> · {{ issue.failureSnippet }}</span>
                    </p>
                    <p class="issue-card__detail">{{ t('inline.viewsOptimizationAccelerationView.text160') }}: {{ localizedDisplayText(issue.suggestedAction) }}</p>
                    <div class="item-actions">
                      <el-button
                        text
                        :loading="loading.rewriteTrial"
                        data-testid="parse-workbench-issue-rewrite-trial"
                        @click="createRewriteTrialFromParseResult(issue)"
                      >
                        {{ t('rewriteTrial.trial') }}
                      </el-button>
                    </div>
                  </article>
                </div>
              </template>
            </article>

            <article class="parse-card" data-testid="parse-workbench-plan-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">hetu explain plan</p>
                  <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text161') }}</h3>
                </div>
                <span
                  class="status-pill"
                  :class="planAnalysis?.status === 'SUCCESS' ? 'status-pill-success' : 'status-pill-warning'"
                >
                  {{ planAnalysis?.status || (t('inline.viewsOptimizationAccelerationView.text162')) }}
                </span>
              </div>

              <p v-if="!planAnalysis" class="empty-state">
                {{ t('inline.viewsOptimizationAccelerationView.text163') }}
              </p>

              <template v-else>
                <div class="highlight-grid">
                  <div v-for="item in planHighlights" :key="item.key" class="highlight-chip" :class="resultValueClass(item)">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
                <div class="mini-section" data-testid="parse-workbench-plan-analysis">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text164') }}</span>
                  <div v-if="planAnalysis.evidence?.length" class="pill-grid">
                    <span v-for="item in planAnalysis.evidence" :key="item" class="summary-chip">{{ item }}</span>
                  </div>
                  <p v-else class="empty-inline">
                    {{ t('inline.viewsOptimizationAccelerationView.text165') }}
                  </p>
                </div>
                <SqlCodeBlock
                  v-if="planAnalysis.planText"
                  :value="planAnalysis.planText"
                  :label="t('inline.viewsOptimizationAccelerationView.text166')"
                  :copy-label="t('inline.viewsOptimizationAccelerationView.text167')"
                  compact
                  data-testid="parse-workbench-plan-text"
                />
              </template>
            </article>

            <article class="parse-card" data-testid="parse-workbench-access-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">access parse</p>
                  <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text168') }}</h3>
                </div>
                <span
                  class="status-pill"
                  :class="accessParse?.serviceStatus === 'AVAILABLE' && accessParse?.connectionStatus === 'CONNECTED' ? 'status-pill-success' : 'status-pill-warning'"
                >
                  {{ accessParse?.serviceStatus || (t('inline.viewsOptimizationAccelerationView.text169')) }}
                </span>
              </div>

              <p v-if="!accessParse" class="empty-state">
                {{
                  t('inline.viewsOptimizationAccelerationView.text170')
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
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text171') }}</span>
                  <p class="result-copy">{{ localizedDisplayText(accessParse.planSummary) }}</p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsOptimizationAccelerationView.text172') }}</span>
                  <p class="result-copy result-copy-muted">{{ localizedDisplayText(accessParse.availabilityWarning) }}</p>
                </div>
              </template>
            </article>
          </div>

          <div class="history-panel">
            <div class="parse-card__header">
              <div>
                <p class="section-kicker sqlforge-code-label">status history</p>
                <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text173') }}</h3>
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

    <el-dialog v-model="batchDialogVisible" :title="t('inline.viewsOptimizationAccelerationView.text174')" width="1240px" top="4vh">
      <section class="batch-dialog-shell" data-testid="batch-import-page">
        <header class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">template download + upload</p>
            <h2 class="section-title">{{ t('inline.viewsOptimizationAccelerationView.text175') }}</h2>
            <p class="section-summary">
              {{
                t('inline.viewsOptimizationAccelerationView.text176')
              }}
            </p>
          </div>
          <div class="hero-inline">
            <span class="hero-pill">{{ parseSessionsSummary }}</span>
            <span class="hero-pill">{{ reportSessionsSummary }}</span>
            <span class="hero-pill hero-pill-muted">{{ t('inline.viewsOptimizationAccelerationView.text177') }}</span>
          </div>
        </header>

        <el-tabs v-model="activeBatchWorkspace" class="workspace-tabs">
          <el-tab-pane :label="t('inline.viewsOptimizationAccelerationView.text178')" name="parse">
            <div class="workspace-toolbar shell-panel">
              <div class="toolbar-copy">
                <p class="section-kicker sqlforge-code-label">template download + upload</p>
                <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text179') }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button type="primary" data-testid="batch-import-create" @click="parseCreateDialogVisible = true">
                  {{ t('inline.viewsOptimizationAccelerationView.text180') }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-ingest" @click="parseImportDialogVisible = true">
                  {{ t('inline.viewsOptimizationAccelerationView.text181') }}
                </el-button>
                <el-button :disabled="!templateColumns.length" data-testid="batch-import-download-template" @click="parseTemplateDialogVisible = true">
                  {{ t('inline.viewsOptimizationAccelerationView.text182') }}
                </el-button>
                <el-button :loading="loading.refreshParseBatch" @click="refreshParseBatchDetail()">
                  {{ t('inline.viewsOptimizationAccelerationView.text183') }}
                </el-button>
              </div>
            </div>

            <div class="workspace-grid">
              <aside class="shell-panel session-rail">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">parse sessions</p>
                    <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text184') }}</h3>
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
                    {{ t('inline.viewsOptimizationAccelerationView.text185') }}
                  </div>
                </div>
              </aside>

              <main class="shell-panel detail-stage">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">batch result</p>
                    <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text186') }}</h3>
                  </div>
                  <div class="toolbar-actions">
                    <el-button
                      :disabled="!parseBatchDetail?.batchId || !retryForm.datasourceCode"
                      data-testid="batch-import-retry-access"
                      @click="retryAccessFlow"
                    >
                      {{ t('inline.viewsOptimizationAccelerationView.text187') }}
                    </el-button>
                    <el-button :disabled="!parseBatchDetail?.batchId" @click="parseDetailDrawerVisible = true">
                      {{ t('inline.viewsOptimizationAccelerationView.text188') }}
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
                        <h4 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text189') }}</h4>
                      </div>
                    </div>
                    <div class="contract-list">
                      <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
                        <strong>{{ item.columnKey }}</strong>
                        <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
                      </div>
                      <div v-if="!templateColumns.length" class="empty-state">
                        {{ t('inline.viewsOptimizationAccelerationView.text190') }}
                      </div>
                    </div>
                  </section>

                  <section class="detail-card">
                    <div class="section-heading">
                      <div>
                        <p class="section-kicker sqlforge-code-label">Failure records</p>
                        <h4 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text191') }}</h4>
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
                          :label="t('inline.viewsOptimizationAccelerationView.text192')"
                          :copy-label="t('inline.viewsOptimizationAccelerationView.text193')"
                          compact
                        />
                        <p v-else>{{ displayValue(item.message) }}</p>
                      </article>
                      <div v-if="!parseFailureRecords.length" class="empty-state">
                        {{ t('inline.viewsOptimizationAccelerationView.text194') }}
                      </div>
                    </div>
                  </section>
                </div>

                <div v-else class="empty-stage">
                  <strong>{{ t('inline.viewsOptimizationAccelerationView.text195') }}</strong>
                  <p>{{ t('inline.viewsOptimizationAccelerationView.text196') }}</p>
                </div>
              </main>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="t('inline.viewsOptimizationAccelerationView.text197')" name="report">
            <div class="workspace-toolbar shell-panel">
              <div class="toolbar-copy">
                <p class="section-kicker sqlforge-code-label">report catalog import</p>
                <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text198') }}</h3>
                <p class="section-summary">{{ t('inline.viewsOptimizationAccelerationView.text199') }}</p>
              </div>
              <div class="toolbar-actions">
                <el-button type="primary" data-testid="batch-import-report-import" @click="reportImportDialogVisible = true">
                  {{ t('inline.viewsOptimizationAccelerationView.text200') }}
                </el-button>
                <el-button :loading="loading.refreshReportBatch" @click="refreshReportBatchDetail()">
                  {{ t('inline.viewsOptimizationAccelerationView.text201') }}
                </el-button>
                <el-button
                  :disabled="!reportBatchDetail?.batchId"
                  data-testid="batch-import-report-resolve"
                  @click="resolveReportSqlsFlow"
                >
                  {{ t('inline.viewsOptimizationAccelerationView.text202') }}
                </el-button>
              </div>
            </div>

            <div class="workspace-grid">
              <aside class="shell-panel session-rail">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">report sessions</p>
                    <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text203') }}</h3>
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
                    {{ t('inline.viewsOptimizationAccelerationView.text204') }}
                  </div>
                </div>
              </aside>

              <main class="shell-panel detail-stage">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">report items</p>
                    <h3 class="detail-title">{{ t('inline.viewsOptimizationAccelerationView.text205') }}</h3>
                  </div>
                  <el-button :disabled="!reportBatchDetail?.batchId" @click="reportDetailDrawerVisible = true">
                    {{ t('inline.viewsOptimizationAccelerationView.text206') }}
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
                    {{ t('inline.viewsOptimizationAccelerationView.text207') }}
                  </div>
                </div>

                <div v-else class="empty-stage">
                  <strong>{{ t('inline.viewsOptimizationAccelerationView.text208') }}</strong>
                  <p>{{ t('inline.viewsOptimizationAccelerationView.text209') }}</p>
                </div>
              </main>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </el-dialog>

    <el-dialog v-model="parseCreateDialogVisible" :title="t('inline.viewsOptimizationAccelerationView.text210')" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text211') }}</span>
          <el-input v-model="parseBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text212') }}</span>
          <el-input v-model="parseBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text213') }}</span>
          <el-select v-model="parseBatchForm.importMode">
            <el-option v-for="option in parseImportModeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text214') }}</span>
          <el-select v-model="parseBatchForm.fileType">
            <el-option v-for="option in parseFileTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text215') }}</span>
          <el-select v-model="parseBatchForm.datasourceCode" filterable allow-create default-first-option data-testid="parse-workbench-parse-batch-datasource-code">
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, parseBatchForm.datasourceCode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text216') }}</span>
          <el-switch v-model="parseBatchForm.structureParseOnly" />
        </label>
      </div>
      <template #footer>
        <el-button @click="parseCreateDialogVisible = false">{{ t('inline.viewsOptimizationAccelerationView.text217') }}</el-button>
        <el-button
          type="primary"
          :disabled="!parseBatchForm.datasourceCode"
          :loading="loading.createParseBatch"
          @click="createParseBatchFlow"
        >
          {{ t('inline.viewsOptimizationAccelerationView.text218') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseImportDialogVisible" :title="t('inline.viewsOptimizationAccelerationView.text219')" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text220') }}</span>
          <el-select v-model="parseBatchForm.directInputMode">
            <el-option v-for="option in directInputModeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text221') }}</span>
          <input type="file" @change="handleParseFileChange">
        </label>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="parseBatchForm.rawContent"
            :label="t('inline.viewsOptimizationAccelerationView.text222')"
            :rows="8"
            :copy-label="t('inline.viewsOptimizationAccelerationView.text223')"
            :format-label="t('inline.viewsOptimizationAccelerationView.text224')"
            :format-enabled="parseBatchForm.directInputMode === 'SQL_LINES'"
            data-testid="parse-batch-raw-sql-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="parseImportDialogVisible = false">{{ t('inline.viewsOptimizationAccelerationView.text225') }}</el-button>
        <el-button type="primary" :loading="loading.ingestParseBatch" @click="ingestParseBatchFlow">
          {{ t('inline.viewsOptimizationAccelerationView.text226') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseTemplateDialogVisible" :title="t('inline.viewsOptimizationAccelerationView.text227')" width="780px">
      <div class="dialog-stack">
        <pre class="code-block">{{ parseTemplatePreview }}</pre>
      </div>
      <template #footer>
        <el-button @click="downloadTemplate">{{ t('inline.viewsOptimizationAccelerationView.text228') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportImportDialogVisible" :title="t('inline.viewsOptimizationAccelerationView.text229')" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text230') }}</span>
          <el-input v-model="reportBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text231') }}</span>
          <el-input v-model="reportBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text232') }}</span>
          <el-select v-model="reportBatchForm.fileType">
            <el-option v-for="option in reportFileTypeOptions" :key="option" :label="option" :value="option" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text233') }}</span>
          <el-input v-model="reportBatchForm.reportCodeField" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text234') }}</span>
          <el-select v-model="reportBatchForm.datasourceCode" filterable allow-create default-first-option data-testid="parse-workbench-report-batch-datasource-code">
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, reportBatchForm.datasourceCode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text235') }}</span>
          <el-input v-model="reportBatchForm.stage" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text236') }}</span>
          <el-input v-model="reportBatchForm.priority" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ t('inline.viewsOptimizationAccelerationView.text237') }}</span>
          <input type="file" @change="handleReportFileChange">
        </label>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="reportBatchForm.rawContent"
            :label="t('inline.viewsOptimizationAccelerationView.text238')"
            :rows="7"
            :copy-label="t('inline.viewsOptimizationAccelerationView.text239')"
            :format-label="t('inline.viewsOptimizationAccelerationView.text240')"
            :format-enabled="false"
            data-testid="report-batch-raw-sql-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ t('inline.viewsOptimizationAccelerationView.text241') }}</el-button>
        <el-button
          type="primary"
          :disabled="!reportBatchForm.datasourceCode"
          :loading="loading.importReportBatch"
          @click="importReportBatchFlow"
        >
          {{ t('inline.viewsOptimizationAccelerationView.text242') }}
        </el-button>
      </template>
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

    <el-drawer v-model="evidenceDrawerVisible" :title="evidenceDrawerTitle || (t('inline.viewsOptimizationAccelerationView.text243'))" size="42%">
      <pre class="code-block">{{ formatJson(evidenceDrawerPayload || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (t('inline.viewsOptimizationAccelerationView.text244'))" width="560px">
      <p class="result-copy">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ t('inline.viewsOptimizationAccelerationView.text245') }}
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

.runtime-summary-compact {
  margin-top: 8px;
  max-width: 920px;
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
