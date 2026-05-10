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
import { issueSceneHelpText } from '../common/issueSceneHelp.mjs'

export function useParseBatchCenter() {
  const { t, locale } = useI18n()
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
  const reportStatisticsIssueScenePagination = reactive({
    pageNumber: 1,
    pageSize: 10
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
      card(t('inline.viewsParseBatchUseParseBatchCenter.text001'), parseBatchDetail.value.status),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text002'), parseBatchDetail.value.parserMode),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text003'), parseBatchDetail.value.totalRecords),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text004'), parseBatchDetail.value.successRecords),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text005'), parseBatchDetail.value.partialSuccessRecords),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text006'), parseBatchDetail.value.failedRecords),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text007'), formatRate(parseBatchDetail.value.structureParseSuccessRate)),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text008'), formatRate(parseBatchDetail.value.accessParseSuccessRate)),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text009'), formatRate(parseBatchDetail.value.planAnalysisStatistics?.successRate))
    ].filter(item => hasDisplayValue(item.value))
  })
  const reportBatchStatusCards = computed(() => {
    if (!reportBatchDetail.value) {
      return []
    }
    return [
      card(t('inline.viewsParseBatchUseParseBatchCenter.text010'), reportBatchDetail.value.status, 'batchStatus'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text011'), reportBatchDetail.value.parserMode, 'parserMode'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text012'), reportBatchDetail.value.totalReports, 'totalReports'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text013'), reportBatchDetail.value.totalSqls ?? reportItems.value.length, 'totalSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text014'), reportBatchDetail.value.resolvedSqls ?? reportBatchDetail.value.resolvedReports, 'resolvedSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text015'), reportBatchDetail.value.failedSqls ?? reportBatchDetail.value.failedReports, 'failedSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text016'), formatRate(reportBatchDetail.value.planAnalysisStatistics?.successRate), 'planRate'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text017'), reportBatchDetail.value.stage, 'stage'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text018'), reportBatchDetail.value.priority, 'priority')
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
      card(t('inline.viewsParseBatchUseParseBatchCenter.text019'), total, 'totalSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text020'), overview.issueSqlCount, 'issueSqlCount'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text021'), overview.totalIssueCount, 'totalIssueCount'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text022'), overview.importantSqlCount, 'importantSqlCount'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text023'), overview.urgentSqlCount, 'urgentSqlCount'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text024'), resolved, 'resolvedSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text025'), partial, 'partialSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text026'), failed, 'failedSqls'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text027'), formatPercent(rate(structureValid, total)), 'structureRate'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text028'), formatPercent(rate(accessConnected, total)), 'accessRate'),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text029'), overview.issueSceneCount ?? reportIssueSceneStatistics.value.length, 'issueScenes'),
      card(
        t('inline.viewsParseBatchUseParseBatchCenter.text030'),
        reportParseStatistics.value.mergeCandidateReportCount,
        'mergeCandidateReportCount'
      ),
      card(t('inline.viewsParseBatchUseParseBatchCenter.text031'), logicalObjectCount, 'logicalObjects')
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
      return t('inline.viewsParseBatchUseParseBatchCenter.text032')
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
  const reportIssueStatisticsPage = computed(() => {
    const pageNumber = Math.max(1, Number(reportStatisticsIssueScenePagination.pageNumber || 1))
    const pageSize = Math.max(1, Number(reportStatisticsIssueScenePagination.pageSize || 10))
    const start = (pageNumber - 1) * pageSize
    return reportIssueStatistics.value.slice(start, start + pageSize)
  })
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
    `${parseBatchListPagination.totalCount || parseBatchSessions.value.length} ${t('inline.viewsParseBatchUseParseBatchCenter.text033')}`
  )
  const reportSessionsSummary = computed(() =>
    `${reportBatchListPagination.totalCount || reportBatchSessions.value.length} ${t('inline.viewsParseBatchUseParseBatchCenter.text034')}`
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
      batchStatus: t('inline.viewsParseBatchUseParseBatchCenter.text035'),
      totalReports: t('inline.viewsParseBatchUseParseBatchCenter.text036'),
      totalSqls: t('inline.viewsParseBatchUseParseBatchCenter.text037'),
      resolvedSqls: t('inline.viewsParseBatchUseParseBatchCenter.text038'),
      failedSqls: t('inline.viewsParseBatchUseParseBatchCenter.text039'),
      stage: t('inline.viewsParseBatchUseParseBatchCenter.text040'),
      priority: t('inline.viewsParseBatchUseParseBatchCenter.text041'),
      issueSqlCount: t('inline.viewsParseBatchUseParseBatchCenter.text042'),
      totalIssueCount: t('inline.viewsParseBatchUseParseBatchCenter.text043'),
      importantSqlCount: t('inline.viewsParseBatchUseParseBatchCenter.text044'),
      urgentSqlCount: t('inline.viewsParseBatchUseParseBatchCenter.text045'),
      partialSqls: t('inline.viewsParseBatchUseParseBatchCenter.text046'),
      structureRate: t('inline.viewsParseBatchUseParseBatchCenter.text047'),
      accessRate: t('inline.viewsParseBatchUseParseBatchCenter.text048'),
      issueScenes: t('inline.viewsParseBatchUseParseBatchCenter.text049'),
      mergeCandidateReportCount: t('inline.viewsParseBatchUseParseBatchCenter.text050'),
      logicalObjects: t('inline.viewsParseBatchUseParseBatchCenter.text051')
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
      return t('inline.viewsParseBatchUseParseBatchCenter.text052')
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
      errorMessage.value = t('inline.viewsParseBatchUseParseBatchCenter.text053')
      return
    }
    downloadTextFile(
      `${parseBatchDetail.value.batchId || 'parse-batch-template'}.csv`,
      parseTemplatePreview.value
    )
  }

  const ingestParseBatchFlow = async () => {
    if (!parseBatchDetail.value?.batchId) {
      errorMessage.value = t('inline.viewsParseBatchUseParseBatchCenter.text054')
      return
    }
    loading.ingestParseBatch = true
    clearError()
    try {
      const payload = await loadPayloadBase64(
        parseUploadFile.value,
        parseBatchForm.rawContent,
        t('inline.viewsParseBatchUseParseBatchCenter.text055')
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
      errorMessage.value = t('inline.viewsParseBatchUseParseBatchCenter.text056')
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
        t('inline.viewsParseBatchUseParseBatchCenter.text057')
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
      errorMessage.value = t('inline.viewsParseBatchUseParseBatchCenter.text058')
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
    reportStatisticsIssueScenePagination.pageNumber = 1
    reportStatisticsIssueScenePagination.pageSize = 10
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

  const handleReportStatisticsIssueScenePageChange = pageNumber => {
    reportStatisticsIssueScenePagination.pageNumber = pageNumber
  }

  const handleReportStatisticsIssueScenePageSizeChange = pageSize => {
    reportStatisticsIssueScenePagination.pageSize = pageSize
    reportStatisticsIssueScenePagination.pageNumber = 1
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

  return {
    activeWorkspace,
    parseUploadFile,
    reportUploadFile,
    parseBatchSessions,
    reportBatchSessions,
    parseBatchDetail,
    reportBatchDetail,
    errorMessage,
    parseCreateDialogVisible,
    parseImportDialogVisible,
    parseTemplateDialogVisible,
    reportTemplateDialogVisible,
    reportImportDialogVisible,
    batchSelectorDrawerVisible,
    batchSelectorKind,
    parseResultDialogVisible,
    parseStatisticsDialogVisible,
    reportResultDialogVisible,
    reportStatisticsDialogVisible,
    activeParseResultTab,
    activeParseStatisticsTab,
    activeReportResultTab,
    activeReportGroupDetailTab,
    activeReportStatisticsTab,
    parseItemDetailDialogVisible,
    reportItemDetailDialogVisible,
    reportGroupDetailDialogVisible,
    fieldHelpDialogVisible,
    fieldHelpDialogTitle,
    fieldHelpDialogMessage,
    selectedParseItem,
    selectedReportItem,
    selectedReportGroupCode,
    reportSqlDetail,
    reportSqlDetailSearchCode,
    reportSqlPagination,
    parseBatchListPagination,
    reportBatchListPagination,
    reportStatisticsSqlPagination,
    reportStatisticsIssueScenePagination,
    loading,
    parseBatchForm,
    retryForm,
    reportBatchForm,
    parseFileTypeOptions,
    parseImportModeOptions,
    directInputModeOptions,
    parserModeOptions,
    DIRECT_SQL_PREVIEW_LIMIT,
    DASHBOARD_PREVIEW_LIMIT,
    DETAIL_PREVIEW_LIMIT,
    STATISTIC_PREVIEW_LIMIT,
    REPORT_SQL_PAGE_SIZE_OPTIONS,
    LIST_PAGE_SIZE_OPTIONS,
    isChinese,
    parseBatchStatusCards,
    reportBatchStatusCards,
    parseFailureRecords,
    reportItems,
    parseImportedRecords,
    parseIssueStatistics,
    parseReportStatistics,
    reportParseStatistics,
    reportParseStatisticsOverview,
    reportIssueSceneStatistics,
    reportImportanceStatistics,
    reportBackendReportStatistics,
    reportBackendSqlStatistics,
    reportPriorityMatrix,
    reportBackendLogicalObjectStatistics,
    reportSqlStatisticsCards,
    reportGroups,
    reportSqlDetailItems,
    reportSqlDetailFailureItems,
    reportSqlDetailGroups,
    selectedReportGroup,
    selectedReportGroupFailureItems,
    reportSqlDetailTotalCount,
    reportGroupDetailTitle,
    reportIssueStatistics,
    reportLogicalObjectStatistics,
    parseImportedRecordsPreview,
    parseImportedRecordsOmittedCount,
    parseFailureRecordsPreview,
    parseFailureRecordsOmittedCount,
    parseIssueStatisticsPreview,
    parseIssueStatisticsOmittedCount,
    parseReportStatisticsPreview,
    parseReportStatisticsOmittedCount,
    reportGroupsDashboardPreview,
    reportGroupsDashboardOmittedCount,
    reportIssueStatisticsPreview,
    reportIssueStatisticsPage,
    reportIssueStatisticsOmittedCount,
    reportImportanceStatisticsPreview,
    reportImportanceStatisticsOmittedCount,
    reportViewStatistics,
    reportViewStatisticsPreview,
    reportViewStatisticsOmittedCount,
    reportSqlStatisticsPreview,
    reportSqlStatisticsOmittedCount,
    reportPriorityMatrixPreview,
    reportPriorityMatrixOmittedCount,
    reportLogicalObjectStatisticsPreview,
    reportLogicalObjectStatisticsOmittedCount,
    templateColumns,
    directSqlRows,
    directSqlPreview,
    directSqlOmittedCount,
    parseSessionsSummary,
    reportSessionsSummary,
    card,
    hasDisplayValue,
    objectValue,
    arrayValue,
    previewList,
    omittedFromPreview,
    formatRate,
    rate,
    formatPercent,
    formatInstant,
    displayValue,
    formatJson,
    issueSceneValues,
    issueSceneHelp,
    issueSceneListHelp,
    helpTextForKey,
    openFieldHelp,
    detailField,
    parseItemDetailFields,
    reportItemDetailFields,
    clearError,
    hasIssueOrFailure,
    buildDiagnosticSummary,
    issueLocationItems,
    issueSceneCodesForItem,
    issueLocationText,
    upsertSession,
    normalizePagedList,
    applyPaginationResult,
    loadBatchHistories,
    encodeArrayBufferToBase64,
    encodeTextToBase64,
    escapeCsvCell,
    buildInlineSqlCsv,
    loadPayloadBase64,
    loadReportPayloadBase64,
    downloadTextFile,
    buildTemplatePreview,
    parseTemplatePreview,
    reportTemplatePreview,
    handleParseFileChange,
    handleReportFileChange,
    createParseBatchFlow,
    downloadTemplate,
    ingestParseBatchFlow,
    refreshParseBatchDetail,
    retryAccessFlow,
    openParseItemDetail,
    importReportBatchFlow,
    loadReportBatchWithStatistics,
    loadReportSqlDetail,
    refreshReportStatistics,
    refreshReportBatchDetail,
    resolveReportSqlsFlow,
    openReportGroupDetail,
    openWholeReportBatchSqlDetail,
    openReportStatistics,
    applyWholeReportSqlFilter,
    handleReportSqlPageChange,
    handleReportSqlPageSizeChange,
    applyReportStatisticsSqlFilter,
    handleReportStatisticsSqlPageChange,
    handleReportStatisticsSqlPageSizeChange,
    handleReportStatisticsIssueScenePageChange,
    handleReportStatisticsIssueScenePageSizeChange,
    openReportItemDetail,
    openBatchSelector,
    openParseSession,
    openReportSession,
    handleBatchSelectorPageChange,
    handleBatchSelectorPageSizeChange,
    buildReportGroups
  }
}
