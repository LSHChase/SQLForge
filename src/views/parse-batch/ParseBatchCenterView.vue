<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  createParseBatch,
  formatRuntimeError,
  getParseBatch,
  getReportBatch,
  getReportBatchParseStatistics,
  importReportBatch,
  ingestParseBatch,
  listParseBatches,
  listReportBatches,
  resolveReportBatchSqls,
  retryParseBatchAccess
} from '../../services/runtimeGateApi'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import { issueSceneHelpText } from '../common/issueSceneHelp.mjs'

const { locale } = useI18n()
const route = useRoute()

const activeWorkspace = ref('parse')
const parseUploadFile = ref(null)
const reportUploadFile = ref(null)
const parseBatchSessions = ref([])
const reportBatchSessions = ref([])
const parseBatchDetail = ref(null)
const reportBatchDetail = ref(null)
const errorMessage = ref('')
const parseCreateDialogVisible = ref(false)
const parseImportDialogVisible = ref(false)
const parseTemplateDialogVisible = ref(false)
const reportTemplateDialogVisible = ref(false)
const reportImportDialogVisible = ref(false)
const batchSelectorDrawerVisible = ref(false)
const batchSelectorKind = ref('parse')
const parseResultDialogVisible = ref(false)
const parseStatisticsDialogVisible = ref(false)
const reportResultDialogVisible = ref(false)
const reportStatisticsDialogVisible = ref(false)
const activeParseResultTab = ref('overview')
const activeParseStatisticsTab = ref('issue')
const activeReportResultTab = ref('groups')
const activeReportGroupDetailTab = ref('overview')
const activeReportStatisticsTab = ref('issueScene')
const parseItemDetailDialogVisible = ref(false)
const reportItemDetailDialogVisible = ref(false)
const reportGroupDetailDialogVisible = ref(false)
const fieldHelpDialogVisible = ref(false)
const fieldHelpDialogTitle = ref('')
const fieldHelpDialogMessage = ref('')
const selectedParseItem = ref(null)
const selectedReportItem = ref(null)
const selectedReportGroupCode = ref('')
const reportSqlDetail = ref(null)
const reportSqlDetailSearchCode = ref('')

const reportSqlPagination = reactive({
  pageNumber: 1,
  pageSize: 25
})

const parseBatchListPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
  pageCount: 0
})

const reportBatchListPagination = reactive({
  pageNo: 1,
  pageSize: 10,
  totalCount: 0,
  pageCount: 0
})

const reportStatisticsSqlPagination = reactive({
  pageNumber: 1,
  pageSize: 25,
  reportCode: ''
})

const loading = reactive({
  createParseBatch: false,
  ingestParseBatch: false,
  refreshParseBatch: false,
  retryParseBatch: false,
  importReportBatch: false,
  resolveReportBatch: false,
  refreshReportBatch: false,
  reportSqlDetail: false,
  reportStatistics: false
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
  reportCodeField: 'report_code',
  datasourceCode: 'hetu_main',
  parserMode: 'JSQLPARSER',
  stage: 'PROD',
  priority: 'high',
  rawContent:
    "report_code,sql_1,sql_2,sql_3\n"
    + "RPT_A,\"SELECT * FROM orders WHERE dt = '2026-04-01'\",\"SELECT * FROM revenue WHERE dt = '2026-04-01'\",\"SELECT count(*) FROM customers\"\n"
    + "RPT_B,\"SELECT * FROM ops_events WHERE dt = '2026-04-01'\",,\n"
})

const parseFileTypeOptions = ['CSV', 'TXT', 'SQL', 'XLS', 'XLSX', 'ET']
const parseImportModeOptions = ['TABULAR_FILE', 'SQL_FILE', 'REPORT_CATALOG']
const directInputModeOptions = ['SQL_LINES', 'TABULAR_TEXT']
const parserModeOptions = [
  { label: 'JSQLParser', value: 'JSQLPARSER' },
  { label: 'Apache Calcite', value: 'APACHE_CALCITE' },
  { label: 'JSQLParser + Hetu EXPLAIN', value: 'JSQLPARSER_WITH_PLAN' },
  { label: 'Apache Calcite + Hetu EXPLAIN', value: 'APACHE_CALCITE_WITH_PLAN' }
]
const DIRECT_SQL_PREVIEW_LIMIT = 5
const DASHBOARD_PREVIEW_LIMIT = 6
const DETAIL_PREVIEW_LIMIT = 25
const STATISTIC_PREVIEW_LIMIT = 50
const REPORT_SQL_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]
const LIST_PAGE_SIZE_OPTIONS = [10, 25, 50, 100]

const isChinese = computed(() => locale.value === 'zh-CN')
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
    card(isChinese.value ? 'Structure 成功率' : 'Structure rate', formatRate(parseBatchDetail.value.structureParseSuccessRate)),
    card(isChinese.value ? 'Access 成功率' : 'Access rate', formatRate(parseBatchDetail.value.accessParseSuccessRate)),
    card(isChinese.value ? 'Plan 成功率' : 'Plan rate', formatRate(parseBatchDetail.value.planAnalysisStatistics?.successRate))
  ].filter(item => hasDisplayValue(item.value))
})
const reportBatchStatusCards = computed(() => {
  if (!reportBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '导入状态' : 'Import status', reportBatchDetail.value.status, 'batchStatus'),
    card(isChinese.value ? '解析工具' : 'Parser tool', reportBatchDetail.value.parserMode, 'parserMode'),
    card(isChinese.value ? '报表总数' : 'Total reports', reportBatchDetail.value.totalReports, 'totalReports'),
    card(isChinese.value ? 'SQL 总数' : 'Total SQL', reportBatchDetail.value.totalSqls ?? reportItems.value.length, 'totalSqls'),
    card(isChinese.value ? '已解析 SQL' : 'Resolved SQL', reportBatchDetail.value.resolvedSqls ?? reportBatchDetail.value.resolvedReports, 'resolvedSqls'),
    card(isChinese.value ? '失败 SQL' : 'Failed SQL', reportBatchDetail.value.failedSqls ?? reportBatchDetail.value.failedReports, 'failedSqls'),
    card(isChinese.value ? 'Plan 成功率' : 'Plan rate', formatRate(reportBatchDetail.value.planAnalysisStatistics?.successRate), 'planRate'),
    card(isChinese.value ? '阶段' : 'Stage', reportBatchDetail.value.stage, 'stage'),
    card(isChinese.value ? '优先级' : 'Priority', reportBatchDetail.value.priority, 'priority')
  ].filter(item => hasDisplayValue(item.value))
})
const parseFailureRecords = computed(() => {
  const detail = parseBatchDetail.value || {}
  const source =
    detail.failureRecords ||
    detail.failedItems ||
    detail.recordResults ||
    detail.records ||
    []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})
const reportItems = computed(() => {
  const detail = reportBatchDetail.value || {}
  const source =
    detail.reportItems ||
    detail.items ||
    detail.reports ||
    detail.records ||
    []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})
const parseImportedRecords = computed(() => {
  const detail = parseBatchDetail.value || {}
  const source =
    detail.importedRecords ||
    detail.records ||
    detail.recordResults ||
    []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})
const parseIssueStatistics = computed(() => {
  const items = parseBatchDetail.value?.issueStatistics || []
  return Array.isArray(items) ? items : []
})
const parseReportStatistics = computed(() => {
  const items = parseBatchDetail.value?.reportStatistics || []
  return Array.isArray(items) ? items : []
})
const reportParseStatistics = computed(() => objectValue(reportBatchDetail.value?.parseStatistics))
const reportParseStatisticsOverview = computed(() => objectValue(reportParseStatistics.value.overview))
const reportIssueSceneStatistics = computed(() => arrayValue(reportParseStatistics.value.issueSceneStatistics))
const reportImportanceStatistics = computed(() => arrayValue(reportParseStatistics.value.importanceStatistics))
const reportBackendReportStatistics = computed(() => arrayValue(reportParseStatistics.value.reportStatistics))
const reportBackendSqlStatistics = computed(() => arrayValue(reportParseStatistics.value.sqlStatistics))
const reportPriorityMatrix = computed(() => arrayValue(reportParseStatistics.value.priorityMatrix))
const reportBackendLogicalObjectStatistics = computed(() =>
  arrayValue(reportParseStatistics.value.logicalObjectStatistics)
)
const reportSqlStatisticsCards = computed(() => {
  const items = reportItems.value
  const overview = reportParseStatisticsOverview.value
  const total = Number(overview.totalSqlCount ?? reportBatchDetail.value?.totalSqls ?? items.length)
  const resolved = items.filter(item => String(item.status || '').toUpperCase() === 'RESOLVED').length
  const partial = items.filter(item => String(item.status || '').toUpperCase().includes('PARTIAL')).length
  const failed = items.filter(item => String(item.status || '').toUpperCase() === 'FAILED').length
  const structureValid = items.filter(item => String(item.structureSyntaxStatus || '').toUpperCase() === 'VALID').length
  const accessConnected = items.filter(item =>
    String(item.accessServiceStatus || '').toUpperCase() === 'AVAILABLE' &&
    String(item.accessConnectionStatus || '').toUpperCase() === 'CONNECTED'
  ).length
  const fallbackLogicalObjects = new Set(items.flatMap(item => Array.isArray(item.logicalObjectKeys) ? item.logicalObjectKeys : []))
  const logicalObjectCount = reportBackendLogicalObjectStatistics.value.length || fallbackLogicalObjects.size
  return [
    card(isChinese.value ? 'SQL 总数' : 'SQL count', total, 'totalSqls'),
    card(isChinese.value ? '问题 SQL' : 'Issue SQL', overview.issueSqlCount, 'issueSqlCount'),
    card(isChinese.value ? '问题总数' : 'Issues', overview.totalIssueCount, 'totalIssueCount'),
    card(isChinese.value ? '重要 SQL' : 'Important SQL', overview.importantSqlCount, 'importantSqlCount'),
    card(isChinese.value ? '紧急 SQL' : 'Urgent SQL', overview.urgentSqlCount, 'urgentSqlCount'),
    card(isChinese.value ? '解析成功' : 'Resolved', resolved, 'resolvedSqls'),
    card(isChinese.value ? '部分解析' : 'Partial', partial, 'partialSqls'),
    card(isChinese.value ? '失败' : 'Failed', failed, 'failedSqls'),
    card(isChinese.value ? '结构成功率' : 'Structure rate', formatPercent(rate(structureValid, total)), 'structureRate'),
    card(isChinese.value ? 'Access 连通率' : 'Access connected', formatPercent(rate(accessConnected, total)), 'accessRate'),
    card(isChinese.value ? '问题场景' : 'Issue scenes', overview.issueSceneCount ?? reportIssueSceneStatistics.value.length, 'issueScenes'),
    card(
      isChinese.value ? '可合并报表' : 'Merge candidates',
      reportParseStatistics.value.mergeCandidateReportCount,
      'mergeCandidateReportCount'
    ),
    card(isChinese.value ? '逻辑对象' : 'Logical objects', logicalObjectCount, 'logicalObjects')
  ].filter(item => hasDisplayValue(item.value))
})
function buildReportGroups(items) {
  const groups = new Map()
  items.forEach(item => {
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
    return {
      ...group,
      total,
      resolved,
      failed,
      structureRate: rate(structureValid, total),
      accessRate: rate(accessConnected, total),
      previewItems: group.items.slice(0, DETAIL_PREVIEW_LIMIT),
      omittedItemCount: Math.max(0, total - DETAIL_PREVIEW_LIMIT)
    }
  })
}
const reportGroups = computed(() => buildReportGroups(reportItems.value))
const reportSqlDetailItems = computed(() => {
  const detail = reportSqlDetail.value || {}
  const source = detail.reportItems || []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})
const reportSqlDetailFailureItems = computed(() =>
  reportSqlDetailItems.value.filter(item =>
    String(item.status || '').toUpperCase() === 'FAILED' ||
    hasDisplayValue(item.failureReason)
  )
)
const reportSqlDetailGroups = computed(() => buildReportGroups(reportSqlDetailItems.value))
const selectedReportGroup = computed(() =>
  reportSqlDetailGroups.value.find(group => group.reportCode === selectedReportGroupCode.value) ||
  reportGroups.value.find(group => group.reportCode === selectedReportGroupCode.value) ||
  null
)
const selectedReportGroupFailureItems = computed(() =>
  (selectedReportGroup.value?.items || []).filter(item =>
    String(item.status || '').toUpperCase() === 'FAILED' ||
    hasDisplayValue(item.failureReason)
  )
)
const reportSqlDetailTotalCount = computed(() =>
  Number(reportSqlDetail.value?.itemTotalCount ?? reportSqlDetailItems.value.length)
)
const reportGroupDetailTitle = computed(() => {
  if (!selectedReportGroup.value) {
    return isChinese.value ? '本报表 SQL 明细' : 'Report SQL detail'
  }
  return isChinese.value
    ? `${selectedReportGroup.value.reportCode} 本报表 SQL 明细`
    : `${selectedReportGroup.value.reportCode} report SQL detail`
})
const reportIssueStatistics = computed(() => {
  if (reportIssueSceneStatistics.value.length) {
    return reportIssueSceneStatistics.value.map(item => ({
      ...item,
      ratio: item.sqlRatio
    }))
  }
  const counts = new Map()
  reportItems.value.forEach(item => {
    const scenes = Array.isArray(item.issueScenes) ? item.issueScenes : []
    scenes.forEach(scene => counts.set(scene, (counts.get(scene) || 0) + 1))
  })
  return Array.from(counts.entries())
    .map(([issueScene, affectedSqlCount]) => ({
      issueScene,
      affectedSqlCount,
      ratio: rate(affectedSqlCount, reportItems.value.length)
    }))
    .sort((left, right) => right.affectedSqlCount - left.affectedSqlCount)
})
const reportLogicalObjectStatistics = computed(() => {
  if (reportBackendLogicalObjectStatistics.value.length) {
    return reportBackendLogicalObjectStatistics.value.map(item => ({
      ...item,
      hitCount: item.sqlCount
    }))
  }
  const counts = new Map()
  reportItems.value.forEach(item => {
    const keys = Array.isArray(item.logicalObjectKeys) ? item.logicalObjectKeys : []
    keys.forEach(objectKey => counts.set(objectKey, (counts.get(objectKey) || 0) + 1))
  })
  return Array.from(counts.entries())
    .map(([objectKey, hitCount]) => ({ objectKey, hitCount }))
    .sort((left, right) => right.hitCount - left.hitCount)
})
const parseImportedRecordsPreview = computed(() => previewList(parseImportedRecords.value, DETAIL_PREVIEW_LIMIT))
const parseImportedRecordsOmittedCount = computed(() =>
  omittedFromPreview(
    parseImportedRecords.value.length,
    parseImportedRecordsPreview.value.length,
    parseBatchDetail.value?.omittedItemCount
  )
)
const parseFailureRecordsPreview = computed(() => previewList(parseFailureRecords.value, DETAIL_PREVIEW_LIMIT))
const parseFailureRecordsOmittedCount = computed(() =>
  omittedFromPreview(
    parseFailureRecords.value.length,
    parseFailureRecordsPreview.value.length,
    parseBatchDetail.value?.omittedFailureCount
  )
)
const parseIssueStatisticsPreview = computed(() => previewList(parseIssueStatistics.value, STATISTIC_PREVIEW_LIMIT))
const parseIssueStatisticsOmittedCount = computed(() =>
  omittedFromPreview(parseIssueStatistics.value.length, parseIssueStatisticsPreview.value.length)
)
const parseReportStatisticsPreview = computed(() => previewList(parseReportStatistics.value, STATISTIC_PREVIEW_LIMIT))
const parseReportStatisticsOmittedCount = computed(() =>
  omittedFromPreview(parseReportStatistics.value.length, parseReportStatisticsPreview.value.length)
)
const reportGroupsDashboardPreview = computed(() => previewList(reportGroups.value, DASHBOARD_PREVIEW_LIMIT))
const reportGroupsDashboardOmittedCount = computed(() => {
  const totalReports = Number(reportBatchDetail.value?.totalReports)
  const sourceCount = Number.isFinite(totalReports) && totalReports > reportGroups.value.length
    ? totalReports
    : reportGroups.value.length
  return omittedFromPreview(sourceCount, reportGroupsDashboardPreview.value.length)
})
const reportIssueStatisticsPreview = computed(() => previewList(reportIssueStatistics.value, STATISTIC_PREVIEW_LIMIT))
const reportIssueStatisticsOmittedCount = computed(() =>
  omittedFromPreview(reportIssueStatistics.value.length, reportIssueStatisticsPreview.value.length)
)
const reportImportanceStatisticsPreview = computed(() =>
  previewList(reportImportanceStatistics.value, STATISTIC_PREVIEW_LIMIT)
)
const reportImportanceStatisticsOmittedCount = computed(() =>
  omittedFromPreview(reportImportanceStatistics.value.length, reportImportanceStatisticsPreview.value.length)
)
const reportViewStatistics = computed(() =>
  reportBackendReportStatistics.value.length ? reportBackendReportStatistics.value : reportGroups.value
)
const reportViewStatisticsPreview = computed(() => previewList(reportViewStatistics.value, STATISTIC_PREVIEW_LIMIT))
const reportViewStatisticsOmittedCount = computed(() =>
  omittedFromPreview(reportViewStatistics.value.length, reportViewStatisticsPreview.value.length)
)
const reportSqlStatisticsPreview = computed(() => previewList(reportBackendSqlStatistics.value, STATISTIC_PREVIEW_LIMIT))
const reportSqlStatisticsOmittedCount = computed(() =>
  omittedFromPreview(
    reportBackendSqlStatistics.value.length,
    reportSqlStatisticsPreview.value.length,
    reportParseStatistics.value?.omittedSqlStatisticCount
  )
)
const reportPriorityMatrixPreview = computed(() => previewList(reportPriorityMatrix.value, STATISTIC_PREVIEW_LIMIT))
const reportPriorityMatrixOmittedCount = computed(() =>
  omittedFromPreview(reportPriorityMatrix.value.length, reportPriorityMatrixPreview.value.length)
)
const reportLogicalObjectStatisticsPreview = computed(() =>
  previewList(reportLogicalObjectStatistics.value, STATISTIC_PREVIEW_LIMIT)
)
const reportLogicalObjectStatisticsOmittedCount = computed(() =>
  omittedFromPreview(reportLogicalObjectStatistics.value.length, reportLogicalObjectStatisticsPreview.value.length)
)
const templateColumns = computed(() => parseBatchDetail.value?.templateColumns || [])
const directSqlRows = computed(() => {
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
const directSqlPreview = computed(() => previewList(directSqlRows.value, DIRECT_SQL_PREVIEW_LIMIT))
const directSqlOmittedCount = computed(() =>
  omittedFromPreview(directSqlRows.value.length, directSqlPreview.value.length)
)
const parseSessionsSummary = computed(() =>
  `${parseBatchListPagination.totalCount || parseBatchSessions.value.length} ${isChinese.value ? '个会话' : 'sessions'}`
)
const reportSessionsSummary = computed(() =>
  `${reportBatchListPagination.totalCount || reportBatchSessions.value.length} ${isChinese.value ? '个批次' : 'batches'}`
)
const card = (label, value, key = '') => ({ label, value, key })

const hasDisplayValue = value =>
  !(value === null || value === undefined || String(value).trim() === '')

const objectValue = value => {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value
  }
  return {}
}

const arrayValue = value => Array.isArray(value) ? value : []

const previewList = (items, limit) => Array.isArray(items) ? items.slice(0, limit) : []

const omittedFromPreview = (sourceCount, shownCount, upstreamOmitted = 0) =>
  Math.max(0, Number(sourceCount || 0) - Number(shownCount || 0)) +
  Math.max(0, Number(upstreamOmitted || 0))

const formatRate = value => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${Number(value).toFixed(2)}`
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

const formatInstant = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').replace('Z', ' UTC')
}

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (value && typeof value === 'object') {
    return JSON.stringify(value)
  }
  if (value === null || value === undefined || String(value).trim() === '') {
    return '-'
  }
  return String(value)
}

const formatJson = value => JSON.stringify(value || {}, null, 2)

const issueSceneValues = value => arrayValue(value).filter(scene => hasDisplayValue(scene))

const issueSceneHelp = scene => issueSceneHelpText(scene, isChinese.value)

const issueSceneListHelp = value => issueSceneValues(value).map(issueSceneHelp).filter(Boolean).join(' / ')

const helpTextForKey = key => {
  const glossary = {
    batchStatus: isChinese.value ? '当前报表批次的导入或解析生命周期状态。' : 'Current lifecycle status of the report-import batch.',
    totalReports: isChinese.value ? '批次内去重后的报表编码数量。' : 'Number of distinct report codes in the batch.',
    totalSqls: isChinese.value ? '批次内 SQL 总数；详情清单通过分页逐页展示。' : 'Total SQL rows in the batch; detail lists are paginated.',
    resolvedSqls: isChinese.value ? '已完成结构解析且 Access 状态可接受的 SQL 数量。' : 'SQL rows whose structure parse and access state are acceptable.',
    failedSqls: isChinese.value ? '未完全解析成功的 SQL 数量。' : 'SQL rows that did not fully resolve successfully.',
    stage: isChinese.value ? '报表导入或解析使用的环境阶段。' : 'Environment stage used by report import or parsing.',
    priority: isChinese.value ? '导入批次或 SQL 行的治理优先级。' : 'Governance priority for the batch or SQL row.',
    issueSqlCount: isChinese.value ? '至少命中一个问题场景的 SQL 数。' : 'SQL rows with at least one issue scene.',
    totalIssueCount: isChinese.value ? '所有 SQL 命中的问题总次数。' : 'Total issue hits across SQL rows.',
    importantSqlCount: isChinese.value ? '命中 important 判定的 SQL 数。' : 'SQL rows marked important by issue scoring.',
    urgentSqlCount: isChinese.value ? '命中 urgent 判定的 SQL 数。' : 'SQL rows marked urgent by issue scoring.',
    partialSqls: isChinese.value ? '结构解析通过但 Access 或后续信号未完全成功的 SQL 数。' : 'SQL rows with valid structure but partial access or downstream signals.',
    structureRate: isChinese.value ? '当前已加载 SQL 行中语法状态为 VALID 的比例。' : 'Ratio of currently loaded SQL rows with VALID syntax.',
    accessRate: isChinese.value ? '当前已加载 SQL 行中 Access 服务可用且连接成功的比例。' : 'Ratio of currently loaded SQL rows with available and connected access parse.',
    issueScenes: isChinese.value ? 'SQL 结构解析命中的问题场景集合。' : 'Issue scenes detected by structure parsing.',
    mergeCandidateReportCount: isChinese.value ? '同一报表内多条 SQL 命中保守静态合并候选规则的报表数。' : 'Reports where multiple SQL rows match the conservative static merge-candidate rule.',
    logicalObjects: isChinese.value ? '解析或 Access 过程识别到的表、视图等逻辑对象。' : 'Logical objects such as tables or views found during parsing.'
  }
  return glossary[key] || ''
}

const openFieldHelp = (key, label) => {
  const message = helpTextForKey(key)
  if (!message) {
    return
  }
  fieldHelpDialogTitle.value = label
  fieldHelpDialogMessage.value = message
  fieldHelpDialogVisible.value = true
}

const detailField = (zhLabel, enLabel, value, wide = false, key = '') => ({
  label: isChinese.value ? zhLabel : enLabel,
  value,
  wide,
  key
})

const parseItemDetailFields = computed(() => {
  const item = selectedParseItem.value || {}
  return [
    detailField('记录 ID', 'Item ID', item.itemId),
    detailField('序号', 'Sequence', item.sequenceNumber),
    detailField('报表代码', 'Report code', item.reportCode),
    detailField('报表名称', 'Report name', item.reportName),
    detailField('状态', 'Status', item.status),
    detailField('解析任务', 'Parse task', item.parseTaskId),
    detailField('解析历史', 'Parse history', item.historyId),
    detailField('历史写入', 'History write', item.historyPersistenceStatus),
    detailField('数据源', 'Datasource', item.datasourceCode),
    detailField('阶段', 'Stage', item.stage),
    detailField('业务日期', 'Biz date', item.bizDate),
    detailField('优先级', 'Priority', item.priority),
    detailField('负责人', 'Owner', item.owner),
    detailField('结构解析', 'Structure parse', item.structureSyntaxStatus),
    detailField('组合状态', 'Analysis status', item.analysisStatus),
    detailField('执行计划', 'Plan analysis', item.planAnalysisStatus),
    detailField('Access 服务', 'Access service', item.accessServiceStatus),
    detailField('Access 连接', 'Access connection', item.accessConnectionStatus),
    detailField('失败行', 'Failure line', item.failureLine),
    detailField('失败列', 'Failure column', item.failureColumn),
    detailField('失败偏移', 'Failure offset', item.failureOffset),
    detailField('失败 token', 'Failure token', item.failureToken),
    detailField('失败原因', 'Failure reason', item.failureReason, true),
    detailField('定位摘要', 'Diagnostic summary', item.diagnosticSummary || buildDiagnosticSummary(item), true),
    detailField('失败片段', 'Failure snippet', item.failureSnippet, true),
    detailField('问题场景', 'Issue scenes', issueSceneCodesForItem(item), true, 'issueScenes'),
    detailField('逻辑对象', 'Logical objects', item.logicalObjectKeys, true),
    detailField('执行计划摘要', 'Plan analysis summary', item.planAnalysis, true),
    detailField('创建时间', 'Created at', formatInstant(item.createdAt)),
    detailField('更新时间', 'Updated at', formatInstant(item.updatedAt))
  ]
})

const reportItemDetailFields = computed(() => {
  const item = selectedReportItem.value || {}
  return [
    detailField('记录 ID', 'Item ID', item.itemId),
    detailField('序号', 'Sequence', item.sequenceNumber),
    detailField('报表代码', 'Report code', item.reportCode),
    detailField('报表名称', 'Report name', item.reportName),
    detailField('SQL 列', 'SQL column', item.sqlColumnName),
    detailField('报表内 SQL 序号', 'SQL ordinal in report', item.sqlOrdinalInReport),
    detailField('状态', 'Status', item.status),
    detailField('解析任务', 'Parse task', item.parseTaskId),
    detailField('数据源', 'Datasource', item.datasourceCode),
    detailField('阶段', 'Stage', item.stage),
    detailField('优先级', 'Priority', item.priority),
    detailField('结构解析', 'Structure parse', item.structureSyntaxStatus),
    detailField('组合状态', 'Analysis status', item.analysisStatus),
    detailField('执行计划', 'Plan analysis', item.planAnalysisStatus),
    detailField('Access 服务', 'Access service', item.accessServiceStatus),
    detailField('Access 连接', 'Access connection', item.accessConnectionStatus),
    detailField('失败行', 'Failure line', item.failureLine),
    detailField('失败列', 'Failure column', item.failureColumn),
    detailField('失败偏移', 'Failure offset', item.failureOffset),
    detailField('失败 token', 'Failure token', item.failureToken),
    detailField('失败原因', 'Failure reason', item.failureReason, true),
    detailField('定位片段', 'Location snippets', issueLocationText(item), true),
    detailField('问题场景', 'Issue scenes', issueSceneCodesForItem(item), true, 'issueScenes'),
    detailField('逻辑对象', 'Logical objects', item.logicalObjectKeys, true),
    detailField('执行计划摘要', 'Plan analysis summary', item.planAnalysis, true),
    detailField('创建时间', 'Created at', formatInstant(item.createdAt)),
    detailField('更新时间', 'Updated at', formatInstant(item.updatedAt))
  ]
})

const clearError = () => {
  errorMessage.value = ''
}

const hasIssueOrFailure = item => {
  if (!item) {
    return false
  }
  const status = String(item.status || '').toUpperCase()
  return status.includes('FAILED') ||
    hasDisplayValue(item.failureReason) ||
    hasDisplayValue(item.diagnosticSummary) ||
    (Array.isArray(item.issueScenes) && item.issueScenes.length > 0)
}

const buildDiagnosticSummary = item => {
  if (!item) {
    return ''
  }
  if (hasDisplayValue(item.diagnosticSummary)) {
    return item.diagnosticSummary
  }
  const parts = []
  const push = (label, value) => {
    if (hasDisplayValue(value)) {
      parts.push(`${label}=${displayValue(value)}`)
    }
  }
  push('sequence', item.sequenceNumber)
  push('report', item.reportCode)
  push('sqlColumn', item.sqlColumnName)
  push('sqlOrdinal', item.sqlOrdinalInReport)
  push('parseTask', item.parseTaskId)
  push('status', item.status)
  push('line', item.failureLine)
  push('col', item.failureColumn)
  push('offset', item.failureOffset)
  push('token', item.failureToken)
  push('near', item.failureSnippet)
  push('issues', item.issueScenes)
  push('reason', item.failureReason)
  return parts.join(' | ')
}

const issueLocationItems = item => {
  const locations = arrayValue(item?.issueLocations)
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

const upsertSession = (collection, item) => {
  const next = collection.value.filter(entry => entry.batchId !== item.batchId)
  collection.value = [item, ...next]
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

const loadBatchHistories = async () => {
  clearError()
  const [parseResult, reportResult] = await Promise.allSettled([
    listParseBatches(parseBatchForm.tenantId, parseBatchListPagination),
    listReportBatches(reportBatchForm.tenantId, reportBatchListPagination)
  ])
  if (parseResult.status === 'fulfilled') {
    const parsePage = normalizePagedList(parseResult.value, parseBatchListPagination.pageSize)
    parseBatchSessions.value = parsePage.items
    applyPaginationResult(parseBatchListPagination, parsePage, parseBatchListPagination.pageSize)
  }
  if (reportResult.status === 'fulfilled') {
    const reportPage = normalizePagedList(reportResult.value, reportBatchListPagination.pageSize)
    reportBatchSessions.value = reportPage.items
    applyPaginationResult(reportBatchListPagination, reportPage, reportBatchListPagination.pageSize)
  }
  if (parseResult.status === 'rejected') {
    errorMessage.value = formatRuntimeError(parseResult.reason)
  }
  if (reportResult.status === 'rejected' && !errorMessage.value) {
    errorMessage.value = formatRuntimeError(reportResult.reason)
  }
}

const encodeArrayBufferToBase64 = buffer => {
  const bytes = new Uint8Array(buffer)
  const chunkSize = 0x8000
  let binary = ''
  for (let index = 0; index < bytes.length; index += chunkSize) {
    const chunk = bytes.subarray(index, index + chunkSize)
    binary += String.fromCharCode(...chunk)
  }
  return window.btoa(binary)
}

const encodeTextToBase64 = text => {
  const encoder = new TextEncoder()
  return encodeArrayBufferToBase64(encoder.encode(text).buffer)
}

const escapeCsvCell = value => `"${String(value || '').replace(/"/g, '""')}"`

const buildInlineSqlCsv = () => {
  const rows = directSqlRows.value.map(item =>
    [item.reportCode, item.datasource, item.sqlText].map(escapeCsvCell).join(',')
  )
  return ['report_code,datasource,sql_text', ...rows].join('\n')
}

const loadPayloadBase64 = async (file, rawContent, emptyMessage) => {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    const normalizedContent =
      parseBatchForm.directInputMode === 'SQL_LINES' ? buildInlineSqlCsv() : String(rawContent)
    return {
      fileName:
        parseBatchForm.directInputMode === 'SQL_LINES' ? 'inline-multi-sql.csv' : 'inline-upload.txt',
      contentBase64: encodeTextToBase64(normalizedContent)
    }
  }
  throw new Error(emptyMessage)
}

const loadReportPayloadBase64 = async (file, rawContent, emptyMessage) => {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    const firstLine = String(rawContent).split('\n', 1)[0] || ''
    return {
      fileName: firstLine.includes(',') ? 'inline-report-import.csv' : 'inline-report-import.txt',
      contentBase64: encodeTextToBase64(String(rawContent))
    }
  }
  throw new Error(emptyMessage)
}

const downloadTextFile = (fileName, content) => {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(link.href)
}

const buildTemplatePreview = columns => {
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

const parseTemplatePreview = computed(() => buildTemplatePreview(templateColumns.value))
const reportTemplatePreview = computed(() =>
  [
    'report_code,sql_1,sql_2,sql_3,...,sql_100',
    "\"RPT_SAMPLE\",\"SELECT * FROM orders WHERE dt = '2026-04-01'\",\"SELECT count(*) FROM revenue\",\"SELECT * FROM customers\",..."
  ].join('\n')
)

const handleParseFileChange = event => {
  const [file] = event.target.files || []
  parseUploadFile.value = file || null
}

const handleReportFileChange = event => {
  const [file] = event.target.files || []
  reportUploadFile.value = file || null
}

const createParseBatchFlow = async () => {
  loading.createParseBatch = true
  clearError()
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
    await loadBatchHistories()
    parseCreateDialogVisible.value = false
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.createParseBatch = false
  }
}

const downloadTemplate = () => {
  if (!templateColumns.value.length) {
    errorMessage.value = isChinese.value
      ? '先创建批次，拿到模板列契约后再下载模板。'
      : 'Create a batch first so the template-column contract can be downloaded.'
    return
  }
  downloadTextFile(
    `${parseBatchDetail.value.batchId || 'parse-batch-template'}.csv`,
    parseTemplatePreview.value
  )
}

const ingestParseBatchFlow = async () => {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value ? '请先创建 parse batch。' : 'Create a parse batch first.'
    return
  }
  loading.ingestParseBatch = true
  clearError()
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
    await loadBatchHistories()
    parseImportDialogVisible.value = false
    activeParseResultTab.value = 'sql'
    parseResultDialogVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.ingestParseBatch = false
  }
}

const refreshParseBatchDetail = async batchId => {
  const targetBatchId = batchId || parseBatchDetail.value?.batchId
  if (!targetBatchId) {
    return
  }
  loading.refreshParseBatch = true
  clearError()
  try {
    parseBatchDetail.value = await getParseBatch(targetBatchId, parseBatchForm.tenantId)
    upsertSession(parseBatchSessions, parseBatchDetail.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.refreshParseBatch = false
  }
}

const retryAccessFlow = async () => {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value
      ? '请先选择一个 parse batch。'
      : 'Select a parse batch first.'
    return
  }
  loading.retryParseBatch = true
  clearError()
  try {
    parseBatchDetail.value = await retryParseBatchAccess(parseBatchDetail.value.batchId, parseBatchForm.tenantId, {
      failureFilter: retryForm.failureFilter,
      datasourceCode: retryForm.datasourceCode,
      forceRecheckAvailability: retryForm.forceRecheckAvailability
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    await loadBatchHistories()
    activeParseResultTab.value = 'sql'
    parseResultDialogVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.retryParseBatch = false
  }
}

const openParseItemDetail = item => {
  if (!item) {
    return
  }
  selectedParseItem.value = item
  parseItemDetailDialogVisible.value = true
}

const importReportBatchFlow = async () => {
  loading.importReportBatch = true
  clearError()
  try {
    const payload = await loadReportPayloadBase64(
      reportUploadFile.value,
      reportBatchForm.rawContent,
      isChinese.value ? '请上传报表清单文件或填写模拟内容。' : 'Upload a report catalog file or provide inline mock content.'
    )
    reportBatchDetail.value = await importReportBatch({
      tenantId: reportBatchForm.tenantId,
      batchName: reportBatchForm.batchName,
      reportCodeField: reportBatchForm.reportCodeField,
      datasourceCode: reportBatchForm.datasourceCode,
      parserMode: reportBatchForm.parserMode,
      stage: reportBatchForm.stage,
      priority: reportBatchForm.priority,
      ...payload,
      charset: 'UTF-8'
    })
    upsertSession(reportBatchSessions, reportBatchDetail.value)
    await loadBatchHistories()
    reportImportDialogVisible.value = false
    activeReportResultTab.value = 'sql'
    reportResultDialogVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.importReportBatch = false
  }
}

const loadReportBatchWithStatistics = async batchId => {
  const detail = await getReportBatch(batchId, reportBatchForm.tenantId)
  try {
    const statistics = await getReportBatchParseStatistics(batchId, reportBatchForm.tenantId)
    return {
      ...detail,
      parseStatistics: statistics || detail?.parseStatistics
    }
  } catch {
    return detail
  }
}

const loadReportSqlDetail = async ({ reportCode = '', pageNumber = reportSqlPagination.pageNumber, pageSize = reportSqlPagination.pageSize } = {}) => {
  const batchId = reportBatchDetail.value?.batchId
  if (!batchId) {
    return
  }
  loading.reportSqlDetail = true
  clearError()
  try {
    const normalizedReportCode = String(reportCode || '').trim()
    const detail = await getReportBatch(batchId, reportBatchForm.tenantId, {
      pageNumber,
      pageSize,
      reportCode: normalizedReportCode,
      requestPrefix: normalizedReportCode
        ? 'frontend-report-batch-report-sql-detail'
        : 'frontend-report-batch-whole-sql-detail'
    })
    reportSqlDetail.value = {
      ...detail,
      parseStatistics: reportBatchDetail.value?.parseStatistics || detail?.parseStatistics
    }
    reportSqlPagination.pageNumber = Number(detail.itemPageNumber || pageNumber)
    reportSqlPagination.pageSize = Number(detail.itemPageSize || pageSize)
    reportSqlDetailSearchCode.value = normalizedReportCode
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportSqlDetail = false
  }
}

const refreshReportStatistics = async ({
  pageNumber = reportStatisticsSqlPagination.pageNumber,
  pageSize = reportStatisticsSqlPagination.pageSize,
  reportCode = reportStatisticsSqlPagination.reportCode
} = {}) => {
  const batchId = reportBatchDetail.value?.batchId
  if (!batchId) {
    return
  }
  loading.reportStatistics = true
  clearError()
  try {
    const normalizedReportCode = String(reportCode || '').trim()
    const statistics = await getReportBatchParseStatistics(batchId, reportBatchForm.tenantId, {
      pageNumber,
      pageSize,
      reportCode: normalizedReportCode,
      requestPrefix: 'frontend-report-batch-parse-statistics-paged'
    })
    reportStatisticsSqlPagination.pageNumber = Number(statistics.sqlStatisticPageNumber || pageNumber)
    reportStatisticsSqlPagination.pageSize = Number(statistics.sqlStatisticPageSize || pageSize)
    reportStatisticsSqlPagination.reportCode = normalizedReportCode
    reportBatchDetail.value = {
      ...(reportBatchDetail.value || {}),
      parseStatistics: statistics
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportStatistics = false
  }
}

const refreshReportBatchDetail = async batchId => {
  const targetBatchId = batchId || reportBatchDetail.value?.batchId
  if (!targetBatchId) {
    return
  }
  loading.refreshReportBatch = true
  clearError()
  try {
    reportBatchDetail.value = await loadReportBatchWithStatistics(targetBatchId)
    upsertSession(reportBatchSessions, reportBatchDetail.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.refreshReportBatch = false
  }
}

const resolveReportSqlsFlow = async () => {
  if (!reportBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value
      ? '请先导入一个报表批次。'
      : 'Import a report batch first.'
    return
  }
  loading.resolveReportBatch = true
  clearError()
  try {
    reportBatchDetail.value = await resolveReportBatchSqls(reportBatchDetail.value.batchId, reportBatchForm.tenantId)
    upsertSession(reportBatchSessions, reportBatchDetail.value)
    await loadBatchHistories()
    activeReportResultTab.value = 'sql'
    reportResultDialogVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.resolveReportBatch = false
  }
}

const openReportGroupDetail = group => {
  selectedReportGroupCode.value = group?.reportCode || ''
  activeReportGroupDetailTab.value = 'overview'
  reportSqlPagination.pageNumber = 1
  loadReportSqlDetail({ reportCode: selectedReportGroupCode.value, pageNumber: 1 })
  reportGroupDetailDialogVisible.value = true
}

const openWholeReportBatchSqlDetail = () => {
  activeReportResultTab.value = 'groups'
  selectedReportGroupCode.value = ''
  reportSqlPagination.pageNumber = 1
  loadReportSqlDetail({ reportCode: '', pageNumber: 1 })
  reportResultDialogVisible.value = true
}

const openReportStatistics = async () => {
  await refreshReportStatistics({ pageNumber: 1 })
  activeReportStatisticsTab.value = 'issueScene'
  reportStatisticsDialogVisible.value = true
}

const applyWholeReportSqlFilter = () => {
  reportSqlPagination.pageNumber = 1
  loadReportSqlDetail({
    reportCode: reportSqlDetailSearchCode.value,
    pageNumber: 1
  })
}

const handleReportSqlPageChange = pageNumber => {
  reportSqlPagination.pageNumber = pageNumber
  loadReportSqlDetail({
    reportCode: reportSqlDetailSearchCode.value,
    pageNumber
  })
}

const handleReportSqlPageSizeChange = pageSize => {
  reportSqlPagination.pageSize = pageSize
  reportSqlPagination.pageNumber = 1
  loadReportSqlDetail({
    reportCode: reportSqlDetailSearchCode.value,
    pageNumber: 1,
    pageSize
  })
}

const applyReportStatisticsSqlFilter = () => {
  reportStatisticsSqlPagination.pageNumber = 1
  refreshReportStatistics({
    pageNumber: 1,
    reportCode: reportStatisticsSqlPagination.reportCode
  })
}

const handleReportStatisticsSqlPageChange = pageNumber => {
  reportStatisticsSqlPagination.pageNumber = pageNumber
  refreshReportStatistics({ pageNumber })
}

const handleReportStatisticsSqlPageSizeChange = pageSize => {
  reportStatisticsSqlPagination.pageSize = pageSize
  reportStatisticsSqlPagination.pageNumber = 1
  refreshReportStatistics({ pageNumber: 1, pageSize })
}

const openReportItemDetail = item => {
  if (!item) {
    return
  }
  selectedReportItem.value = item
  selectedReportGroupCode.value = item.reportCode || selectedReportGroupCode.value
  reportItemDetailDialogVisible.value = true
}

const openBatchSelector = async kind => {
  batchSelectorKind.value = kind
  batchSelectorDrawerVisible.value = true
  await loadBatchHistories()
}

const openParseSession = async batchId => {
  await refreshParseBatchDetail(batchId)
  batchSelectorDrawerVisible.value = false
  activeWorkspace.value = 'parse'
}

const openReportSession = async batchId => {
  await refreshReportBatchDetail(batchId)
  batchSelectorDrawerVisible.value = false
  activeWorkspace.value = 'report'
}

const handleBatchSelectorPageChange = async pageNo => {
  if (batchSelectorKind.value === 'report') {
    reportBatchListPagination.pageNo = pageNo
  } else {
    parseBatchListPagination.pageNo = pageNo
  }
  await loadBatchHistories()
}

const handleBatchSelectorPageSizeChange = async pageSize => {
  if (batchSelectorKind.value === 'report') {
    reportBatchListPagination.pageSize = pageSize
    reportBatchListPagination.pageNo = 1
  } else {
    parseBatchListPagination.pageSize = pageSize
    parseBatchListPagination.pageNo = 1
  }
  await loadBatchHistories()
}

onMounted(async () => {
  const tenantId = String(route.query.tenantId || '').trim()
  if (tenantId) {
    parseBatchForm.tenantId = tenantId
    reportBatchForm.tenantId = tenantId
  }
  await loadBatchHistories()
  const batchId = String(route.query.batchId || '').trim()
  if (!batchId) {
    return
  }
  const kind = String(route.query.kind || 'parse').trim()
  if (kind === 'report') {
    activeWorkspace.value = 'report'
    await openReportSession(batchId)
    return
  }
  activeWorkspace.value = 'parse'
  await openParseSession(batchId)
})
</script>

<template>
  <section class="runtime-page batch-import-page" data-testid="batch-import-page">
    <header class="page-hero shell-panel">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">{{ isChinese ? '批次筛选' : 'Batch filters' }}</p>
        <h2 class="runtime-title">{{ isChinese ? '创建批次、导入内容与查看结果' : 'Create batches, ingest content, and inspect results' }}</h2>
        <p class="runtime-summary">
          {{
            isChinese
              ? '首屏只保留批次入口与当前结果区；模板、导入和详情都转入弹窗或抽屉。'
              : 'The first screen stays focused on batch entry points and the current result stage. Templates, imports, and details move into dialogs or drawers.'
          }}
        </p>
      </div>
      <div class="hero-inline">
        <span class="hero-pill">{{ parseSessionsSummary }}</span>
        <span class="hero-pill">{{ reportSessionsSummary }}</span>
        <span class="hero-pill hero-pill-muted">{{ isChinese ? '查询条件 + 结果区 + 抽屉' : 'Filters + results + drawers' }}</span>
      </div>
    </header>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="batch-import-error"
    >
      {{ errorMessage }}
    </div>

    <el-tabs v-model="activeWorkspace" class="workspace-tabs">
      <el-tab-pane :label="isChinese ? '批量解析' : 'Parse batches'" name="parse">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">current batch workbench</p>
            <h2 class="section-title">{{ isChinese ? '当前批量解析批次' : 'Current parse batch' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '本页主体只展示当前批次概览、解析结果和解析统计；创建与导入参数都在弹窗中完成。'
                  : 'The page body only shows the current batch overview, parse results, and statistics. Create and ingest parameters stay in dialogs.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button data-testid="batch-import-download-template" @click="parseTemplateDialogVisible = true">
              {{ isChinese ? '批量模板' : 'Batch template' }}
            </el-button>
            <el-button type="primary" data-testid="batch-import-create" @click="parseCreateDialogVisible = true">
              {{ isChinese ? '创建批次' : 'Create batch' }}
            </el-button>
            <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-ingest" @click="parseImportDialogVisible = true">
              {{ isChinese ? '导入内容' : 'Ingest content' }}
            </el-button>
            <el-button :loading="loading.refreshParseBatch" @click="refreshParseBatchDetail()">
              {{ isChinese ? '刷新详情' : 'Refresh detail' }}
            </el-button>
            <el-button @click="openBatchSelector('parse')">
              {{ isChinese ? '选择批次' : 'Select batch' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid current-batch-grid" data-testid="batch-import-current-workbench">
          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">batch result</p>
                <h3 class="section-title">{{ isChinese ? '当前批次概览' : 'Current batch overview' }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :disabled="!parseBatchDetail?.batchId"
                  data-testid="batch-import-retry-access"
                  @click="retryAccessFlow"
                >
                  {{ isChinese ? '补跑 Access' : 'Retry access' }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-parse-detail" @click="parseResultDialogVisible = true">
                  {{ isChinese ? '解析结果' : 'Parse results' }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-parse-statistics" @click="parseStatisticsDialogVisible = true">
                  {{ isChinese ? '解析统计' : 'Parse statistics' }}
                </el-button>
              </div>
            </div>

            <div v-if="parseBatchDetail" class="summary-grid">
              <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>

            <div v-else class="empty-stage">
              <strong>{{ isChinese ? '暂无 parse batch' : 'No parse batch selected' }}</strong>
              <p>{{ isChinese ? '使用“创建批次”建立批次，再通过“导入内容”上传模板文件或粘贴多条 SQL。' : 'Use Create batch first, then Ingest content to upload a template file or paste multiple SQL statements.' }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? '报表导入' : 'Report catalog import'" name="report">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">report catalog import</p>
            <h2 class="section-title">{{ isChinese ? '当前报表导入批次' : 'Current report import batch' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '报表导入按 report_code 分组，导入参数在弹窗中完成，首屏保留批次概览与结果入口。'
                  : 'Report imports are grouped by report_code. Import parameters stay in the dialog, while the first screen keeps the batch overview and result entry points.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button data-testid="batch-import-report-template" @click="reportTemplateDialogVisible = true">
              {{ isChinese ? '宽表模板' : 'Wide template' }}
            </el-button>
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
            <el-button @click="openBatchSelector('report')">
              {{ isChinese ? '选择批次' : 'Select batch' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid current-batch-grid" data-testid="batch-import-report-current-workbench">
          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">current report batch</p>
                <h3 class="section-title">{{ isChinese ? '当前批次概览' : 'Current batch overview' }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :disabled="!reportBatchDetail?.batchId"
                  data-testid="batch-import-report-batch-sql-detail"
                  @click="openWholeReportBatchSqlDetail"
                >
                  {{ isChinese ? '整个批次 SQL 详情' : 'Whole batch SQL detail' }}
                </el-button>
                <el-button :disabled="!reportBatchDetail?.batchId" data-testid="batch-import-report-statistics" @click="openReportStatistics">
                  {{ isChinese ? '解析统计' : 'Parse statistics' }}
                </el-button>
              </div>
            </div>

            <div v-if="reportBatchDetail" class="summary-grid">
              <article v-for="item in reportBatchStatusCards" :key="item.label" class="summary-card">
                <span class="summary-card-label">
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
              </article>
            </div>

            <div v-if="reportBatchDetail" class="summary-grid" data-testid="batch-import-report-sql-statistics">
              <article v-for="item in reportSqlStatisticsCards" :key="item.label" class="summary-card">
                <span class="summary-card-label">
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
              </article>
            </div>

            <div v-if="reportBatchDetail" class="report-list" data-testid="batch-import-report-sql-detail">
              <button
                v-for="group in reportGroupsDashboardPreview"
                :key="group.reportCode"
                type="button"
                class="report-item"
                data-testid="batch-import-report-group-open"
                @click="openReportGroupDetail(group)"
              >
                <div class="session-item-top">
                  <strong>{{ group.reportCode }}</strong>
                  <span class="status-pill">{{ group.total }} SQL</span>
                </div>
                <p>{{ displayValue(group.reportName) }}</p>
                <p>
                  {{ isChinese ? '已解析' : 'Resolved' }}: {{ group.resolved }}
                  · {{ isChinese ? '失败' : 'Failed' }}: {{ group.failed }}
                  · Structure: {{ formatPercent(group.structureRate) }}
                  · Access: {{ formatPercent(group.accessRate) }}
                </p>
                <span class="detail-link">{{ isChinese ? '查看本报表 SQL 明细' : 'View this report SQL detail' }}</span>
              </button>
              <div v-if="reportGroupsDashboardOmittedCount > 0" class="preview-note">
                {{
                  isChinese
                    ? `仅展示前 ${DASHBOARD_PREVIEW_LIMIT} 个报表分组入口，另有 ${reportGroupsDashboardOmittedCount} 个分组未展开。`
                    : `Showing the first ${DASHBOARD_PREVIEW_LIMIT} report groups; ${reportGroupsDashboardOmittedCount} more groups are omitted.`
                }}
              </div>
              <div v-if="!reportItems.length" class="empty-state">
                {{ isChinese ? '导入后会在这里看到本批次报表与 SQL 概览。' : 'Imported report and SQL overview appears here.' }}
              </div>
            </div>

            <div v-else class="empty-stage">
              <strong>{{ isChinese ? '暂无 report batch' : 'No report batch selected' }}</strong>
              <p>{{ isChinese ? '使用“导入报表批次”上传文件或粘贴 report_code + 多 SQL 宽表。' : 'Use Import report batch to upload a file or paste a report_code + multi-SQL wide table.' }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>
    </el-tabs>

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
            <el-option v-for="item in parseImportModeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '文件类型' : 'File type' }}</span>
          <el-select v-model="parseBatchForm.fileType">
            <el-option v-for="item in parseFileTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '模板版本' : 'Template version' }}</span>
          <el-input v-model="parseBatchForm.templateVersion" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '默认数据源' : 'Default datasource' }}</span>
          <el-input v-model="parseBatchForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">解析工具 / Parser tool</span>
          <el-select v-model="parseBatchForm.parserMode" data-testid="batch-import-parse-batch-parser-mode">
            <el-option
              v-for="option in parserModeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '仅结构解析' : 'Structure-only batch' }}</span>
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

    <el-dialog v-model="parseImportDialogVisible" :title="isChinese ? '导入批量内容' : 'Import batch content'" width="820px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '输入方式' : 'Input mode' }}</span>
          <el-select v-model="parseBatchForm.directInputMode">
            <el-option v-for="item in directInputModeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" data-testid="batch-import-file-input" @change="handleParseFileChange">
        </label>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="parseBatchForm.rawContent"
            :label="parseBatchForm.directInputMode === 'SQL_LINES' ? (isChinese ? '多条 SQL 直接输入' : 'Direct multi-SQL input') : (isChinese ? '内联内容' : 'Inline content')"
            :rows="10"
            :copy-label="isChinese ? '复制' : 'Copy'"
            :format-label="isChinese ? '格式化' : 'Format'"
            :format-enabled="parseBatchForm.directInputMode === 'SQL_LINES'"
            data-testid="batch-import-dialog-sql-input"
          />
        </div>
      </div>

      <div v-if="directSqlPreview.length" class="preview-list">
        <article v-for="item in directSqlPreview" :key="item.reportCode" class="preview-item">
          <strong>{{ item.reportCode }}</strong>
          <span>{{ item.datasource }}</span>
          <SqlCodeBlock
            :value="item.sqlText"
            :label="item.reportCode"
            :copy-label="isChinese ? '复制' : 'Copy'"
            compact
          />
        </article>
        <div
          v-if="directSqlOmittedCount > 0"
          class="preview-note"
          data-testid="batch-import-direct-sql-preview-truncated"
        >
          {{
            isChinese
              ? `仅预览前 ${DIRECT_SQL_PREVIEW_LIMIT} 条，提交时仍导入全部 ${directSqlRows.length} 条 SQL。`
              : `Previewing the first ${DIRECT_SQL_PREVIEW_LIMIT}; submit still imports all ${directSqlRows.length} SQL rows.`
          }}
        </div>
      </div>

      <template #footer>
        <el-button @click="parseImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.ingestParseBatch" @click="ingestParseBatchFlow">
          {{ isChinese ? '确认导入' : 'Confirm import' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseTemplateDialogVisible" :title="isChinese ? '模板列契约与预览' : 'Template-column contract and preview'" width="760px">
      <div class="template-sheet">
        <p class="section-kicker sqlforge-code-label">Template-column contract</p>
        <div class="contract-list">
          <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
            <strong>{{ item.columnKey }}</strong>
            <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
          </div>
          <div v-if="!templateColumns.length" class="empty-state">
            {{ isChinese ? '先创建批次，拿到模板列契约后再下载模板。' : 'Create a batch first so the template-column contract can be downloaded.' }}
          </div>
        </div>
        <SqlCodeBlock
          v-if="templateColumns.length"
          :value="parseTemplatePreview"
          :label="isChinese ? '模板预览' : 'Template preview'"
          :copy-label="isChinese ? '复制' : 'Copy'"
          :auto-format="false"
        />
      </div>
      <template #footer>
        <el-button @click="parseTemplateDialogVisible = false">{{ isChinese ? '关闭' : 'Close' }}</el-button>
        <el-button :disabled="!templateColumns.length" @click="downloadTemplate">
          {{ isChinese ? '下载模板' : 'Download template' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportTemplateDialogVisible" :title="isChinese ? '宽表模板' : 'Wide table template'" width="760px">
      <div class="template-sheet" data-testid="batch-import-report-template-dialog">
        <p class="section-kicker sqlforge-code-label">report_code,sql_1,sql_2,sql_3,...,sql_100</p>
        <p class="result-copy">
          {{
            isChinese
              ? '每行代表一个报表，report_code 作为分组键，sql_1、sql_2 等列承载同一报表下的多条 SQL；空单元格会被忽略。'
              : 'Each row is one report. report_code is the grouping key, while sql_1, sql_2, and later columns hold SQL rows under the same report; empty cells are ignored.'
          }}
        </p>
        <SqlCodeBlock
          :value="reportTemplatePreview"
          :label="isChinese ? '宽表模板预览' : 'Wide template preview'"
          :copy-label="isChinese ? '复制' : 'Copy'"
          :auto-format="false"
        />
      </div>
      <template #footer>
        <el-button @click="reportTemplateDialogVisible = false">{{ isChinese ? '关闭' : 'Close' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="parseResultDialogVisible"
      :title="isChinese ? '当前批次解析结果' : 'Current batch parse results'"
      width="980px"
      data-testid="batch-import-parse-detail"
    >
      <el-tabs v-model="activeParseResultTab" data-testid="batch-import-parse-result-tabs">
        <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
          <div class="summary-grid">
            <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? 'SQL 明细' : 'SQL detail'" name="sql">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">SQL-level parse detail</p>
            <div
              v-if="parseImportedRecordsOmittedCount > 0"
              class="preview-note"
              data-testid="batch-import-large-batch-preview"
            >
              {{
                isChinese
                  ? `仅展示前 ${parseImportedRecordsPreview.length} 条 SQL 明细，另有 ${parseImportedRecordsOmittedCount} 条已省略；统计概览仍按完整批次计算。`
                  : `Showing the first ${parseImportedRecordsPreview.length} SQL rows; ${parseImportedRecordsOmittedCount} more are omitted while summaries still use the full batch.`
              }}
            </div>
            <div class="report-list">
              <article
                v-for="(item, index) in parseImportedRecordsPreview"
                :key="item.itemId || item.recordId || index"
                class="report-item"
                data-testid="batch-import-parse-sql-detail"
              >
                <div class="session-item-top">
                  <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
                  <span class="status-pill">{{ displayValue(item.status) }}</span>
                </div>
                <p>
                  {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                  · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                    :title="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="item.sqlColumnName || item.itemId || 'SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="batch-import-parse-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-parse-item-detail-open" @click="openParseItemDetail(item)">
                    {{ isChinese ? '查看详情' : 'View detail' }}
                  </el-button>
                </div>
              </article>
              <div v-if="!parseImportedRecords.length" class="empty-state">
                {{ isChinese ? '当前批次还没有 SQL 明细。' : 'No SQL rows in the current batch yet.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '失败记录' : 'Failure records'" name="failures">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">Failure records</p>
            <div class="failure-list">
              <article
                v-for="(item, index) in parseFailureRecordsPreview"
                :key="item.recordId || item.id || index"
                class="failure-item"
                data-testid="batch-import-failure-record"
              >
                <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
                <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <p v-if="issueSceneCodesForItem(item).length">
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                    :title="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText || item.sqlPreview"
                  :value="item.sqlText || item.sqlPreview"
                  :label="isChinese ? '失败 SQL' : 'Failed SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                />
                <p v-else>{{ displayValue(item.message) }}</p>
                <button
                  type="button"
                  class="detail-link detail-link-button"
                  data-testid="batch-import-parse-failure-detail-open"
                  @click="openParseItemDetail(item)"
                >
                  {{ isChinese ? '查看解析详情' : 'View parse detail' }}
                </button>
              </article>
              <div
                v-if="parseFailureRecordsOmittedCount > 0"
                class="preview-note"
                data-testid="batch-import-parse-failure-preview"
              >
                {{
                  isChinese
                    ? `仅展示前 ${parseFailureRecordsPreview.length} 条失败记录，另有 ${parseFailureRecordsOmittedCount} 条已省略。`
                    : `Showing the first ${parseFailureRecordsPreview.length} failed rows; ${parseFailureRecordsOmittedCount} more are omitted.`
                }}
              </div>
              <div v-if="!parseFailureRecords.length" class="empty-state">
                {{ isChinese ? '当前没有失败记录。' : 'No failure records in the current batch.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '原始证据' : 'Raw evidence'" name="raw">
          <pre class="code-block">{{ formatJson(parseBatchDetail) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="parseItemDetailDialogVisible"
      :title="isChinese ? '批量解析记录详情' : 'Parse batch item detail'"
      width="920px"
      data-testid="batch-import-parse-item-detail"
    >
      <div v-if="selectedParseItem" class="dialog-stack">
        <div class="detail-fields">
          <div
            v-for="field in parseItemDetailFields"
            :key="field.label"
            class="detail-field"
            :class="{ 'detail-field-wide': field.wide }"
          >
            <span class="summary-card-label">{{ field.label }}</span>
            <strong>
              {{ displayValue(field.value) }}
              <span
                v-if="field.key === 'issueScenes' && issueSceneListHelp(field.value)"
                class="help-dot issue-scene-help"
                tabindex="0"
                aria-label="issue scene help"
                :data-tooltip="issueSceneListHelp(field.value)"
                :title="issueSceneListHelp(field.value)"
              >?</span>
            </strong>
          </div>
        </div>
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">SQL text</p>
          <SqlCodeBlock
            :value="displayValue(selectedParseItem.sqlText || selectedParseItem.sqlTemplateText)"
            :label="isChinese ? 'SQL 文本' : 'SQL text'"
            :copy-label="isChinese ? '复制' : 'Copy'"
            data-testid="batch-import-selected-parse-sql"
          />
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="parseStatisticsDialogVisible" :title="isChinese ? '当前批次解析统计' : 'Current batch parse statistics'" width="920px">
      <el-tabs v-model="activeParseStatisticsTab" data-testid="batch-import-parse-statistics-tabs">
        <el-tab-pane :label="isChinese ? '问题场景' : 'Issue scenes'" name="issue">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">parse statistics</p>
            <div class="stat-list">
              <div v-for="item in parseIssueStatisticsPreview" :key="item.issueScene" class="contract-item">
                <strong>
                  {{ item.issueScene }}
                  <span
                    v-if="issueSceneHelp(item.issueScene)"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneHelp(item.issueScene)"
                    :title="issueSceneHelp(item.issueScene)"
                  >?</span>
                </strong>
                <span>{{ displayValue(item.affectedRecords) }} · {{ formatPercent(item.ratio) }}</span>
              </div>
              <div
                v-if="parseIssueStatisticsOmittedCount > 0"
                class="preview-note preview-note-compact"
                data-testid="batch-import-parse-statistics-preview"
              >
                {{
                  isChinese
                    ? `另有 ${parseIssueStatisticsOmittedCount} 个问题场景未展开。`
                    : `${parseIssueStatisticsOmittedCount} more issue scenes are omitted.`
                }}
              </div>
              <div v-if="!parseIssueStatistics.length" class="empty-state">
                {{ isChinese ? '当前没有问题场景统计。' : 'No issue statistics yet.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '报表维度' : 'By report'" name="report">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">report-level statistics</p>
            <div class="stat-list">
              <div v-for="item in parseReportStatisticsPreview" :key="item.reportCode" class="contract-item">
                <strong>{{ item.reportCode }}</strong>
                <span>{{ displayValue(item.sqlCount) }} SQL · {{ displayValue(item.issueCount) }} issues</span>
              </div>
              <div v-if="parseReportStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                {{
                  isChinese
                    ? `另有 ${parseReportStatisticsOmittedCount} 个报表统计项未展开。`
                    : `${parseReportStatisticsOmittedCount} more report statistic rows are omitted.`
                }}
              </div>
              <div v-if="!parseReportStatistics.length" class="empty-state">
                {{ isChinese ? '当前没有报表维度统计。' : 'No report statistics yet.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '失败记录' : 'Failure records'" name="failures">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">Failure records</p>
            <div class="failure-list">
              <article
                v-for="(item, index) in parseFailureRecordsPreview"
                :key="item.recordId || item.id || index"
                class="failure-item"
                data-testid="batch-import-failure-record"
              >
                <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
                <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <p v-if="issueSceneCodesForItem(item).length">
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                    :title="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText || item.sqlPreview"
                  :value="item.sqlText || item.sqlPreview"
                  :label="isChinese ? '失败 SQL' : 'Failed SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                />
                <p v-else>{{ displayValue(item.message) }}</p>
                <button
                  type="button"
                  class="detail-link detail-link-button"
                  data-testid="batch-import-parse-failure-detail-open"
                  @click="openParseItemDetail(item)"
                >
                  {{ isChinese ? '查看解析详情' : 'View parse detail' }}
                </button>
              </article>
              <div
                v-if="parseFailureRecordsOmittedCount > 0"
                class="preview-note"
                data-testid="batch-import-parse-failure-preview"
              >
                {{
                  isChinese
                    ? `仅展示前 ${parseFailureRecordsPreview.length} 条失败记录，另有 ${parseFailureRecordsOmittedCount} 条已省略。`
                    : `Showing the first ${parseFailureRecordsPreview.length} failed rows; ${parseFailureRecordsOmittedCount} more are omitted.`
                }}
              </div>
              <div v-if="!parseFailureRecords.length" class="empty-state">
                {{ isChinese ? '当前没有失败记录。' : 'No failure records in the current batch.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="reportImportDialogVisible" :title="isChinese ? '导入报表批次' : 'Import report batch'" width="820px">
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
          <span class="field-label">{{ isChinese ? '报表编码字段' : 'Report code field' }}</span>
          <el-input v-model="reportBatchForm.reportCodeField" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '默认数据源' : 'Default datasource' }}</span>
          <el-input v-model="reportBatchForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">解析工具 / Parser tool</span>
          <el-select v-model="reportBatchForm.parserMode" data-testid="batch-import-report-batch-parser-mode">
            <el-option
              v-for="option in parserModeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '优先级' : 'Priority' }}</span>
          <el-input v-model="reportBatchForm.priority" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" @change="handleReportFileChange">
        </label>
        <div class="field-note field-block-wide">
          {{ isChinese ? '文件类型会根据文件名和内容自动识别，无需手动选择。' : 'File type is auto-detected from the filename and payload content.' }}
        </div>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="reportBatchForm.rawContent"
            :label="isChinese ? '内联清单' : 'Inline report catalog'"
            :rows="8"
            :copy-label="isChinese ? '复制' : 'Copy'"
            :format-label="isChinese ? '格式化' : 'Format'"
            :format-enabled="false"
            data-testid="batch-import-report-dialog-sql-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.importReportBatch" @click="importReportBatchFlow">
          {{ isChinese ? '导入报表批次' : 'Import report batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reportResultDialogVisible"
      :title="isChinese ? '整个报表批次 SQL 详情' : 'Whole report batch SQL detail'"
      width="1040px"
      data-testid="batch-import-report-result-dialog"
    >
      <el-tabs v-model="activeReportResultTab" data-testid="batch-import-report-result-tabs">
        <el-tab-pane :label="isChinese ? '报表分组' : 'Report groups'" name="groups">
          <div class="report-list">
            <article
              v-for="group in reportSqlDetailGroups"
              :key="group.reportCode"
              class="report-item"
            >
              <div class="session-item-top">
                <strong>{{ group.reportCode }}</strong>
                <span class="status-pill">{{ group.total }} SQL</span>
              </div>
              <p>
                {{ displayValue(group.reportName) }}
                · {{ isChinese ? '已解析' : 'Resolved' }} {{ group.resolved }}
                · {{ isChinese ? '失败' : 'Failed' }} {{ group.failed }}
                · Structure {{ formatPercent(group.structureRate) }}
                · Access {{ formatPercent(group.accessRate) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportGroupDetail(group)">
                {{ isChinese ? '打开本报表 SQL 明细' : 'Open report SQL detail' }}
              </button>
            </article>
            <div v-if="!reportSqlDetailGroups.length" class="empty-state">
              {{ isChinese ? '当前没有报表分组。' : 'No report groups yet.' }}
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sql">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">SQL-level parse detail</p>
            <div class="filter-row" data-testid="batch-import-report-sql-filter">
              <el-input
                v-model="reportSqlDetailSearchCode"
                :placeholder="isChinese ? '按报表编码筛选' : 'Filter by report code'"
                clearable
              />
              <el-button :loading="loading.reportSqlDetail" @click="applyWholeReportSqlFilter">
                {{ isChinese ? '查询' : 'Search' }}
              </el-button>
            </div>
            <div
              v-if="reportSqlDetailTotalCount > reportSqlDetailItems.length"
              class="preview-note"
              data-testid="batch-import-report-large-batch-preview"
            >
              {{
                isChinese
                  ? `当前第 ${reportSqlPagination.pageNumber} 页展示 ${reportSqlDetailItems.length} 条 SQL，筛选后共 ${reportSqlDetailTotalCount} 条；概览仍按完整批次汇总。`
                  : `Page ${reportSqlPagination.pageNumber} shows ${reportSqlDetailItems.length} SQL rows out of ${reportSqlDetailTotalCount}; summaries still use the full batch.`
              }}
            </div>
            <div v-loading="loading.reportSqlDetail" class="report-list">
              <article
                v-for="(item, index) in reportSqlDetailItems"
                :key="item.itemId || `${item.reportCode}-${index}`"
                class="report-item"
                data-testid="batch-import-report-drawer-sql-detail"
              >
                <div class="session-item-top">
                  <strong>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }}</strong>
                  <span class="status-pill">{{ displayValue(item.status) }}</span>
                </div>
                <p>
                  {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                  · {{ isChinese ? 'SQL 序号' : 'SQL ordinal' }}: {{ displayValue(item.sqlOrdinalInReport) }}
                  · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                    :title="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-report-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="isChinese ? 'SQL 输出' : 'SQL output'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="batch-import-report-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-report-item-detail-open" @click="openReportItemDetail(item)">
                    {{ isChinese ? '查看详情' : 'View detail' }}
                  </el-button>
                </div>
              </article>
              <div v-if="!reportSqlDetailItems.length" class="empty-state">
                {{ isChinese ? '当前没有 SQL 清单。' : 'No SQL rows yet.' }}
              </div>
            </div>
            <el-pagination
              v-if="reportSqlDetailTotalCount > reportSqlPagination.pageSize"
              class="pagination-row"
              layout="total, sizes, prev, pager, next"
              :total="reportSqlDetailTotalCount"
              :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
              :page-size="reportSqlPagination.pageSize"
              :current-page="reportSqlPagination.pageNumber"
              @current-change="handleReportSqlPageChange"
              @size-change="handleReportSqlPageSizeChange"
            />
          </section>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '失败 SQL' : 'Failed SQL'" name="failures">
          <p class="section-kicker sqlforge-code-label">failed sql detail</p>
          <div class="failure-list">
            <article
              v-for="(item, index) in reportSqlDetailFailureItems"
              :key="item.itemId || index"
              class="failure-item"
              data-testid="batch-import-report-failure-record"
            >
              <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
              <span>{{ displayValue(item.failureReason || item.status) }}</span>
              <p>{{ displayValue(item.sqlColumnName || item.sqlOrdinalInReport) }} · {{ displayValue(item.parseTaskId) }}</p>
              <p
                v-if="hasIssueOrFailure(item)"
                class="diagnostic-line"
                data-testid="batch-import-report-diagnostic"
              >
                {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportItemDetail(item)">
                {{ isChinese ? '查看失败详情' : 'View failure detail' }}
              </button>
            </article>
            <div v-if="!reportSqlDetailFailureItems.length" class="empty-state">
              {{ isChinese ? '当前没有失败 SQL。' : 'No failed SQL rows.' }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="reportGroupDetailDialogVisible"
      :title="reportGroupDetailTitle"
      width="1040px"
      data-testid="batch-import-report-group-detail-dialog"
    >
      <el-tabs v-if="selectedReportGroup" v-model="activeReportGroupDetailTab" data-testid="batch-import-report-group-tabs">
        <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
          <div class="summary-grid" data-testid="batch-import-report-scoped-summary">
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '报表编码' : 'Report code' }}</span>
              <strong>{{ selectedReportGroup.reportCode }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">SQL</span>
              <strong>{{ selectedReportGroup.total }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '已解析' : 'Resolved' }}</span>
              <strong>{{ selectedReportGroup.resolved }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '失败' : 'Failed' }}</span>
              <strong>{{ selectedReportGroup.failed }}</strong>
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sql">
          <section class="detail-card" data-testid="batch-import-report-scoped-sql-detail">
            <p class="section-kicker sqlforge-code-label">single report SQL-level parse detail</p>
            <div v-loading="loading.reportSqlDetail" class="report-list">
              <article
                v-for="(item, index) in selectedReportGroup.previewItems"
                :key="item.itemId || `${selectedReportGroup.reportCode}-${index}`"
                class="report-item"
                data-testid="batch-import-report-scoped-sql-row"
              >
                <div class="session-item-top">
                  <strong>{{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }}</strong>
                  <span class="status-pill">{{ displayValue(item.status) }}</span>
                </div>
                <p>
                  {{ displayValue(item.reportName) }} · {{ displayValue(item.datasourceCode || item.stage) }}
                  · {{ displayValue(item.priority) }}
                </p>
                <p>
                  {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                  · {{ isChinese ? 'SQL 序号' : 'SQL ordinal' }}: {{ displayValue(item.sqlOrdinalInReport) }}
                  · {{ isChinese ? '状态' : 'Status' }}: {{ displayValue(item.status) }}
                </p>
                <p>
                  Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                    :title="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-report-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="isChinese ? 'SQL 输出' : 'SQL output'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="batch-import-report-scoped-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-report-item-detail-open" @click="openReportItemDetail(item)">
                    {{ isChinese ? '查看详情' : 'View detail' }}
                  </el-button>
                </div>
              </article>
              <div v-if="selectedReportGroup.omittedItemCount > 0" class="preview-note">
                {{
                  isChinese
                    ? `该报表仅展示前 ${selectedReportGroup.previewItems.length} 条 SQL，另有 ${selectedReportGroup.omittedItemCount} 条已省略。`
                    : `This report shows the first ${selectedReportGroup.previewItems.length} SQL rows; ${selectedReportGroup.omittedItemCount} more are omitted.`
                }}
              </div>
            </div>
            <el-pagination
              v-if="reportSqlDetailTotalCount > reportSqlPagination.pageSize"
              class="pagination-row"
              layout="total, sizes, prev, pager, next"
              :total="reportSqlDetailTotalCount"
              :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
              :page-size="reportSqlPagination.pageSize"
              :current-page="reportSqlPagination.pageNumber"
              @current-change="handleReportSqlPageChange"
              @size-change="handleReportSqlPageSizeChange"
            />
          </section>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '失败 SQL' : 'Failed SQL'" name="failures">
          <p class="section-kicker sqlforge-code-label">failed sql detail</p>
          <div class="failure-list">
            <article
              v-for="(item, index) in selectedReportGroupFailureItems"
              :key="item.itemId || index"
              class="failure-item"
              data-testid="batch-import-report-failure-record"
            >
              <strong>{{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }}</strong>
              <span>{{ displayValue(item.failureReason || item.status) }}</span>
              <p
                v-if="hasIssueOrFailure(item)"
                class="diagnostic-line"
                data-testid="batch-import-report-diagnostic"
              >
                {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportItemDetail(item)">
                {{ isChinese ? '查看失败详情' : 'View failure detail' }}
              </button>
            </article>
            <div v-if="!selectedReportGroupFailureItems.length" class="empty-state">
              {{ isChinese ? '本报表没有失败 SQL。' : 'No failed SQL rows in this report.' }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="reportItemDetailDialogVisible"
      :title="isChinese ? '报表 SQL 解析详情' : 'Report SQL parse detail'"
      width="920px"
      data-testid="batch-import-report-item-detail"
    >
      <div v-if="selectedReportItem" class="dialog-stack">
        <div class="detail-fields">
          <div
            v-for="field in reportItemDetailFields"
            :key="field.label"
            class="detail-field"
            :class="{ 'detail-field-wide': field.wide }"
          >
            <span class="summary-card-label">{{ field.label }}</span>
            <strong>
              {{ displayValue(field.value) }}
              <span
                v-if="field.key === 'issueScenes' && issueSceneListHelp(field.value)"
                class="help-dot issue-scene-help"
                tabindex="0"
                aria-label="issue scene help"
                :data-tooltip="issueSceneListHelp(field.value)"
                :title="issueSceneListHelp(field.value)"
              >?</span>
            </strong>
          </div>
        </div>
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">SQL output</p>
          <SqlCodeBlock
            :value="displayValue(selectedReportItem.sqlText)"
            :label="isChinese ? 'SQL 输出' : 'SQL output'"
            :copy-label="isChinese ? '复制' : 'Copy'"
            data-testid="batch-import-selected-report-sql"
          />
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="reportStatisticsDialogVisible" :title="isChinese ? '当前报表批次解析统计' : 'Current report batch parse statistics'" width="960px">
      <div class="dialog-stack">
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">report-level statistics</p>
          <el-tabs v-model="activeReportStatisticsTab" class="statistics-tabs" data-testid="batch-import-report-statistics-tabs">
            <el-tab-pane :label="isChinese ? '问题场景' : 'Issue scenes'" name="issueScene">
              <div class="stat-list">
                <div
                  v-for="item in reportIssueStatisticsPreview"
                  :key="item.issueScene"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-issue-scene"
                >
                  <strong>
                    {{ item.issueScene }}
                    <span
                      v-if="issueSceneHelp(item.issueScene)"
                      class="help-dot issue-scene-help"
                      tabindex="0"
                      aria-label="issue scene help"
                      :data-tooltip="issueSceneHelp(item.issueScene)"
                      :title="issueSceneHelp(item.issueScene)"
                    >?</span>
                  </strong>
                  <span>
                    {{ item.affectedSqlCount }} SQL
                    · {{ displayValue(item.severity) }}
                    · {{ formatPercent(item.ratio) }}
                  </span>
                </div>
                <div
                  v-if="reportIssueStatisticsOmittedCount > 0"
                  class="preview-note preview-note-compact"
                  data-testid="batch-import-report-statistics-preview"
                >
                  {{
                    isChinese
                      ? `另有 ${reportIssueStatisticsOmittedCount} 个问题场景未展开。`
                      : `${reportIssueStatisticsOmittedCount} more issue scenes are omitted.`
                  }}
                </div>
                <div v-if="!reportIssueStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有问题场景统计。' : 'No issue statistics yet.' }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '重要程度' : 'Importance'" name="importance">
              <div class="stat-list">
                <div
                  v-for="item in reportImportanceStatisticsPreview"
                  :key="item.importanceBucket"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-importance"
                >
                  <strong>{{ item.importanceBucket }}</strong>
                  <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</span>
                </div>
                <div v-if="reportImportanceStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportImportanceStatisticsOmittedCount} 个重要程度统计项未展开。`
                      : `${reportImportanceStatisticsOmittedCount} more importance rows are omitted.`
                  }}
                </div>
                <div v-if="!reportImportanceStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有重要程度统计。' : 'No importance statistics yet.' }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '报表视角' : 'Report view'" name="report">
              <div class="stat-list">
                <div
                  v-for="item in reportViewStatisticsPreview"
                  :key="item.reportCode"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-report-view"
                >
                  <strong>{{ item.reportCode }}</strong>
                  <span>
                    {{ displayValue(item.sqlCount ?? item.total) }} SQL
                    · {{ displayValue(item.issueCount ?? item.failed) }} issues
                    · {{ formatPercent(item.issueSqlRatio ?? item.structureRate) }}
                  </span>
                  <span v-if="item.mergeCandidate" class="preview-note preview-note-compact">
                    {{
                      isChinese
                        ? `建议合并复核：${displayValue(item.mergeCandidateSqlCount)} 条 SQL。${displayValue(item.mergeCandidateReason)}`
                        : `Merge review: ${displayValue(item.mergeCandidateSqlCount)} SQL. ${displayValue(item.mergeCandidateReason)}`
                    }}
                  </span>
                </div>
                <div v-if="reportViewStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportViewStatisticsOmittedCount} 个报表统计项未展开。`
                      : `${reportViewStatisticsOmittedCount} more report rows are omitted.`
                  }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sqlList">
              <div class="filter-row" data-testid="batch-import-report-statistics-sql-filter">
                <el-input
                  v-model="reportStatisticsSqlPagination.reportCode"
                  :placeholder="isChinese ? '按报表编码筛选 SQL 清单' : 'Filter SQL list by report code'"
                  clearable
                />
                <el-button :loading="loading.reportStatistics" @click="applyReportStatisticsSqlFilter">
                  {{ isChinese ? '查询' : 'Search' }}
                </el-button>
              </div>
              <div class="stat-list">
                <div
                  v-for="item in reportSqlStatisticsPreview"
                  :key="item.itemId"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-sql-list"
                >
                  <strong>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId }}</strong>
                  <span>{{ item.highestPriorityLevel }} · {{ item.issueCount }} issues · {{ displayValue(item.logicalObjectKeys) }}</span>
                  <span>
                    {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                    <span
                      v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                      class="help-dot issue-scene-help"
                      tabindex="0"
                      aria-label="issue scene help"
                      :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                      :title="issueSceneListHelp(issueSceneCodesForItem(item))"
                    >?</span>
                    {{ displayValue(issueSceneCodesForItem(item)) }}
                  </span>
                  <span>{{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}</span>
                </div>
                <div
                  v-if="reportSqlStatisticsOmittedCount > 0"
                  class="preview-note preview-note-compact"
                  data-testid="batch-import-report-sql-statistics-preview"
                >
                  {{
                    isChinese
                      ? `SQL 清单仅展示前 ${reportSqlStatisticsPreview.length} 条，另有 ${reportSqlStatisticsOmittedCount} 条未展开。`
                      : `SQL list shows the first ${reportSqlStatisticsPreview.length} rows; ${reportSqlStatisticsOmittedCount} more are omitted.`
                  }}
                </div>
                <div v-if="!reportBackendSqlStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有 SQL 清单统计。' : 'No SQL list statistics yet.' }}
                </div>
              </div>
              <el-pagination
                v-if="Number(reportParseStatistics.sqlStatisticTotalCount || 0) > reportStatisticsSqlPagination.pageSize"
                class="pagination-row"
                layout="total, sizes, prev, pager, next"
                :total="Number(reportParseStatistics.sqlStatisticTotalCount || 0)"
                :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
                :page-size="reportStatisticsSqlPagination.pageSize"
                :current-page="reportStatisticsSqlPagination.pageNumber"
                @current-change="handleReportStatisticsSqlPageChange"
                @size-change="handleReportStatisticsSqlPageSizeChange"
              />
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '优先级视角' : 'Priority view'" name="priority">
              <div class="stat-list">
                <div
                  v-for="item in reportPriorityMatrixPreview"
                  :key="`${item.priorityLevel}-${item.urgencyBucket}`"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-priority"
                >
                  <strong>{{ item.priorityLevel }} · {{ item.urgencyBucket }}</strong>
                  <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</span>
                </div>
                <div v-if="reportPriorityMatrixOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportPriorityMatrixOmittedCount} 个优先级矩阵项未展开。`
                      : `${reportPriorityMatrixOmittedCount} more priority rows are omitted.`
                  }}
                </div>
                <div v-if="!reportPriorityMatrix.length" class="empty-state">
                  {{ isChinese ? '当前没有优先级矩阵统计。' : 'No priority matrix statistics yet.' }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '逻辑对象视角' : 'Logical objects'" name="logicalObject">
              <div class="stat-list">
                <div
                  v-for="item in reportLogicalObjectStatisticsPreview"
                  :key="item.objectKey"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-logical-object"
                >
                  <strong>{{ item.objectKey }}</strong>
                  <span>{{ item.hitCount }} SQL · {{ displayValue(item.reportCodes) }}</span>
                </div>
                <div v-if="reportLogicalObjectStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportLogicalObjectStatisticsOmittedCount} 个逻辑对象未展开。`
                      : `${reportLogicalObjectStatisticsOmittedCount} more logical objects are omitted.`
                  }}
                </div>
                <div v-if="!reportLogicalObjectStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有逻辑对象命中。' : 'No logical object hits yet.' }}
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (isChinese ? '字段说明' : 'Field help')" width="560px">
      <p class="result-copy">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ isChinese ? '知道了' : 'Close' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="batchSelectorDrawerVisible"
      :title="batchSelectorKind === 'report' ? (isChinese ? '选择报表批次' : 'Select report batch') : (isChinese ? '选择解析批次' : 'Select parse batch')"
      size="38%"
    >
      <div class="session-list">
        <button
          v-for="item in (batchSelectorKind === 'report' ? reportBatchSessions : parseBatchSessions)"
          :key="item.batchId"
          type="button"
          class="session-item"
          :class="{
            'session-item-active': batchSelectorKind === 'report'
              ? reportBatchDetail?.batchId === item.batchId
              : parseBatchDetail?.batchId === item.batchId
          }"
          :data-testid="batchSelectorKind === 'report' ? 'batch-import-report-item' : 'batch-import-parse-batch-item'"
          @click="batchSelectorKind === 'report' ? openReportSession(item.batchId) : openParseSession(item.batchId)"
        >
          <div class="session-item-top">
            <strong>{{ item.batchName || item.batchId }}</strong>
            <span class="status-pill">{{ item.status || '-' }}</span>
          </div>
          <p>{{ item.batchId }}</p>
          <span>{{ formatInstant(item.createdAt || item.updatedAt) }}</span>
        </button>
      </div>
      <el-pagination
        class="pagination-row"
        layout="total, sizes, prev, pager, next"
        :total="batchSelectorKind === 'report' ? reportBatchListPagination.totalCount : parseBatchListPagination.totalCount"
        :page-sizes="LIST_PAGE_SIZE_OPTIONS"
        :page-size="batchSelectorKind === 'report' ? reportBatchListPagination.pageSize : parseBatchListPagination.pageSize"
        :current-page="batchSelectorKind === 'report' ? reportBatchListPagination.pageNo : parseBatchListPagination.pageNo"
        @current-change="handleBatchSelectorPageChange"
        @size-change="handleBatchSelectorPageSizeChange"
      />
    </el-drawer>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shell-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08), transparent 32%),
    var(--sqlforge-surface-2);
  padding: 20px;
}

.page-hero,
.workspace-toolbar,
.section-heading,
.toolbar-actions,
.hero-inline,
.session-item-top {
  display: flex;
  gap: 12px;
}

.page-hero,
.workspace-toolbar,
.section-heading {
  justify-content: space-between;
  align-items: flex-start;
}

.page-hero,
.workspace-toolbar {
  flex-wrap: wrap;
}

.runtime-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  color: var(--sqlforge-color-brand);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 12px;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.runtime-summary,
.section-summary,
.empty-state,
.empty-stage p,
.session-item p,
.session-item span,
.failure-item p,
.report-item p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-inline,
.toolbar-actions {
  flex-wrap: wrap;
}

.hero-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.hero-pill-muted {
  color: var(--sqlforge-text-muted);
}

.history-summary-grid {
  margin-top: -2px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 20px;
}

.current-batch-grid {
  grid-template-columns: minmax(0, 1fr);
}

.session-rail,
.detail-stage,
.detail-card,
.summary-card,
.session-item,
.failure-item,
.report-item,
.contract-item,
.preview-item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 16px;
  background: rgba(35, 35, 35, 0.92);
}

.session-list,
.drawer-stack,
.dialog-stack,
.contract-list,
.failure-list,
.report-list,
.stat-list,
.preview-list {
  display: grid;
  gap: 12px;
}

.session-item {
  width: 100%;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.failure-item,
.report-item {
  display: block;
  width: 100%;
  color: inherit;
  font: inherit;
  text-align: left;
}

.failure-item[role='button'],
button.report-item {
  cursor: pointer;
}

.failure-item[role='button']:hover,
button.report-item:hover {
  border-color: var(--sqlforge-color-brand-border);
}

.table-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--sqlforge-color-brand);
  font: inherit;
  text-align: left;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.field-note {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  line-height: 1.6;
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

.preview-note {
  border: 1px solid var(--sqlforge-color-brand-border);
  border-radius: 10px;
  background: rgba(16, 185, 129, 0.1);
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  padding: 10px 12px;
}

.preview-note-compact {
  font-size: 12px;
  padding: 8px 10px;
}

.diagnostic-line {
  border-left: 3px solid rgba(245, 158, 11, 0.85);
  padding-left: 10px;
  color: var(--sqlforge-text-primary);
  overflow-wrap: anywhere;
}

.detail-link {
  display: inline-flex;
  margin-top: 8px;
  color: var(--sqlforge-color-link);
  font-size: 13px;
}

.detail-link-button {
  align-self: flex-start;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.detail-link-button:hover,
.detail-link-button:focus-visible {
  text-decoration: underline;
}

.item-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.detail-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-field {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 14px;
  background: rgba(35, 35, 35, 0.92);
}

.detail-field strong {
  overflow-wrap: anywhere;
}

.detail-field-wide {
  grid-column: 1 / -1;
}

.session-item-active {
  border-color: var(--sqlforge-color-brand-border);
  box-shadow: inset 0 0 0 1px rgba(16, 185, 129, 0.25);
}

.detail-stage,
.empty-stage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid,
.dialog-grid {
  display: grid;
  gap: 12px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.result-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.detail-card,
.failure-item,
.report-item,
.contract-item,
.preview-item {
  padding: 14px 16px;
}

.summary-card-label,
.field-label {
  display: flex;
  gap: 4px;
  align-items: center;
  margin-bottom: 8px;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.help-dot {
  min-width: 20px;
  height: 20px;
  padding: 0;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 50%;
  color: var(--sqlforge-text-secondary);
  line-height: 18px;
}

.issue-scene-help {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 20px;
  margin-left: 4px;
  cursor: help;
  background: rgba(20, 24, 31, 0.92);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: none;
  vertical-align: middle;
}

.issue-scene-help::after,
.issue-scene-help::before {
  position: absolute;
  z-index: 30;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.12s ease, visibility 0.12s ease;
  visibility: hidden;
}

.issue-scene-help::after {
  bottom: calc(100% + 8px);
  left: 50%;
  width: max-content;
  max-width: min(360px, calc(100vw - 48px));
  padding: 10px 12px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 6px;
  background: #111827;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.35);
  color: #f8fafc;
  content: attr(data-tooltip);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
  text-align: left;
  text-transform: none;
  transform: translateX(-50%);
  white-space: normal;
}

.issue-scene-help::before {
  bottom: calc(100% + 3px);
  left: 50%;
  width: 8px;
  height: 8px;
  background: #111827;
  content: '';
  transform: translateX(-50%) rotate(45deg);
}

.issue-scene-help:hover::after,
.issue-scene-help:hover::before,
.issue-scene-help:focus::after,
.issue-scene-help:focus::before,
.issue-scene-help:focus-visible::after,
.issue-scene-help:focus-visible::before {
  opacity: 1;
  visibility: visible;
}

.result-copy {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.dialog-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dialog-grid-single {
  grid-template-columns: 1fr;
}

.template-sheet {
  display: grid;
  gap: 16px;
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.compact-code {
  margin-top: 10px;
  max-height: 160px;
}

.empty-stage {
  align-items: center;
  justify-content: center;
  min-height: 260px;
  border: 1px dashed var(--sqlforge-border-default);
  border-radius: 16px;
}

@media (max-width: 1100px) {
  .workspace-grid,
  .result-layout,
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
