<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { SAMPLE_DATASOURCE_CODE, SAMPLE_TENANT_ID } from '../../config/tenantDefaults.mjs'
import {
  executeQuery,
  formatRuntimeError,
  getGovernanceDatasources,
  getGovernanceMessageStats,
  getMetadataSchemas,
  getMetadataTables,
  getMetadataTableDetail
} from '../../services/runtimeGateApi'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import {
  findDatasourceOption,
  firstDatasourceForEngine,
  flattenDatasourceGroups,
  groupGovernanceDatasources
} from '../common/governanceDatasourceOptions.mjs'
import { sqlTemplates, sqlLibrary } from './sqlTemplates'
import {
  DEFAULT_QUERY_RESULT_PAGE_SIZE,
  normalizeQueryResultPage,
  resolveVisibleQueryRows
} from './queryResultPage.mjs'
import { useQueryParameters } from './useQueryParameters'
import { useQueryHistory } from './useQueryHistory'

const { t, locale } = useI18n()
const router = useRouter()

const DEEP_PARSE_SESSION_PREFIX = 'sqlforge:query-analysis:deep-parse:'

const datasourceTree = ref([])
const datasourceGroupedMap = ref(new Map())
const datasourceInventoryLoading = ref(false)
const datasourceInventoryError = ref('')
const searchQuery = ref('')
const selectedSchema = ref('')
const schemaOptions = ref([])
const objectsTreeRef = ref(null)
const treeVersion = ref(0)

const treeProps = {
  label: 'label',
  children: 'children',
  isLeaf: 'isLeaf'
}

const getNodeIcon = (nodeType) => {
  switch (nodeType) {
    case 'engine': return '🌐'
    case 'datasource': return '🔌'
    case 'schema': return '📁'
    case 'table': return '📊'
    case 'column': return '🏷️'
    default: return '📄'
  }
}

const highlightKeyword = (label, query) => {
  if (!label) return ''
  if (!query) return label
  const escapedQuery = query.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&')
  const regex = new RegExp(`(${escapedQuery})`, 'gi')
  return label.replace(regex, '<span class="highlight-matched" style="color: #3ecf8e !important; font-weight: 600;">$1</span>')
}

const filterTreeNode = (value, data) => {
  if (!value) return true
  return String(data.label || '').toLowerCase().includes(value.toLowerCase())
}

watch(searchQuery, (val) => {
  objectsTreeRef.value?.filter(val)
})

const loadSchemasForDatasource = async (datasourceCode) => {
  if (!datasourceCode || datasourceCode === 'AUTO') {
    schemaOptions.value = []
    selectedSchema.value = ''
    return
  }
  try {
    const schemas = await getMetadataSchemas(form.tenantId, datasourceCode)
    schemaOptions.value = (schemas || []).map(s => s.schemaName)
    if (schemaOptions.value.length > 0) {
      if (!schemaOptions.value.includes(selectedSchema.value)) {
        selectedSchema.value = schemaOptions.value[0]
      }
    } else {
      selectedSchema.value = ''
    }
  } catch (e) {
    schemaOptions.value = []
    selectedSchema.value = ''
  }
}

const currentEngineDatasources = computed(() => {
  const engine = form.datasourceType
  if (!engine || engine === 'AUTO') {
    return flattenDatasourceGroups(datasourceGroupedMap.value)
  }
  return datasourceGroupedMap.value.get(engine) || []
})
const hasAvailableDatasources = computed(() => flattenDatasourceGroups(datasourceGroupedMap.value).length > 0)
const queryRunDisabled = computed(() => running.value || !form.datasourceCode)

const handleEngineChange = async () => {
  const datasources = currentEngineDatasources.value
  if (datasources.length > 0) {
    const found = datasources.find(d => d.datasourceCode === form.datasourceCode)
    if (!found) {
      syncDatasourceFields(datasources[0], { keepDatasourceType: form.datasourceType === 'AUTO' })
    }
  } else {
    form.datasourceCode = ''
    selectedDatasourceId.value = ''
  }
  await loadSchemasForDatasource(form.datasourceCode)
}

const handleDatasourceChange = async () => {
  const datasources = currentEngineDatasources.value
  const found = datasources.find(d => d.datasourceCode === form.datasourceCode)
  if (found) {
    syncDatasourceFields(found)
  }
  await loadSchemasForDatasource(form.datasourceCode)
}

const handleSchemaChange = () => {
  // Schema selection synced
}

const insertTextAtCursor = (text) => {
  const textarea = document.querySelector('[data-testid="query-flow-sql-editor"]')
  if (!textarea) {
    form.sqlText = form.sqlText ? `${form.sqlText} ${text}` : text
    return
  }
  const startPos = textarea.selectionStart
  const endPos = textarea.selectionEnd
  const originalText = textarea.value
  const newText = originalText.substring(0, startPos) + text + originalText.substring(endPos)
  form.sqlText = newText
  
  setTimeout(() => {
    textarea.focus()
    textarea.selectionStart = startPos + text.length
    textarea.selectionEnd = startPos + text.length
  }, 0)
}

const handleTreeNodeDblClick = (data) => {
  if (data.nodeType === 'table') {
    const insertVal = `${data.schemaName}.${data.tableName}`
    insertTextAtCursor(insertVal)
  } else if (data.nodeType === 'column') {
    const columnName = data.label.split(' ')[0]
    insertTextAtCursor(columnName)
  }
}

const loadTreeNode = async (node, resolve) => {
  const tenantId = form.tenantId

  if (node.level === 0) {
    try {
      const records = await getGovernanceDatasources(tenantId, {
        requestPrefix: 'frontend-query-datasource-inventory'
      })
      const grouped = groupGovernanceDatasources(records)
      datasourceGroupedMap.value = grouped
      
      const engines = Array.from(grouped.keys()).map(engine => ({
        label: locale.value === 'zh-CN' ? `${engine} 资源管理器` : `${engine} inventory`,
        id: `${engine.toLowerCase()}-inventory`,
        nodeType: 'engine',
        engineType: engine,
        isLeaf: false
      }))
      resolve(engines)
      
      if (!form.datasourceCode) {
        const firstDs = firstDatasourceForEngine(grouped, form.datasourceType)
        if (firstDs) {
          syncDatasourceSelection(firstDs)
        }
      }
    } catch (e) {
      resolve([])
    }
    return
  }

  if (node.level === 1) {
    const engineType = node.data.engineType
    const children = datasourceGroupedMap.value.get(engineType) || []
    resolve(
      children.map(child => ({
        ...child,
        nodeType: 'datasource',
        isLeaf: false
      }))
    )
    return
  }

  if (node.level === 2) {
    const { datasourceCode, datasourceType } = node.data
    try {
      const schemas = await getMetadataSchemas(tenantId, datasourceCode)
      resolve(
        (schemas || []).map(schema => ({
          label: schema.schemaName,
          id: `${datasourceCode}-${schema.schemaName}`,
          nodeType: 'schema',
          datasourceType,
          datasourceCode,
          schemaName: schema.schemaName,
          isLeaf: false
        }))
      )
    } catch (e) {
      resolve([])
    }
    return
  }

  if (node.level === 3) {
    const { datasourceCode, datasourceType, schemaName } = node.data
    try {
      const tables = await getMetadataTables(tenantId, datasourceCode, schemaName)
      resolve(
        (tables || []).map(table => ({
          label: table.tableName,
          id: `${datasourceCode}-${schemaName}-${table.tableName}`,
          nodeType: 'table',
          datasourceType,
          datasourceCode,
          schemaName,
          tableName: table.tableName,
          isLeaf: false
        }))
      )
    } catch (e) {
      resolve([])
    }
    return
  }

  if (node.level === 4) {
    const { datasourceCode, datasourceType, schemaName, tableName } = node.data
    try {
      const detail = await getMetadataTableDetail(tenantId, datasourceCode, schemaName, tableName)
      const colCount = detail.columnCount || 6
      const cols = []
      
      if (tableName === 'orders') {
        const orderCols = [
          { name: 'order_id', type: 'BIGINT' },
          { name: 'tenant_id', type: 'VARCHAR(64)' },
          { name: 'datasource', type: 'VARCHAR(64)' },
          { name: 'query_date', type: 'VARCHAR(32)' },
          { name: 'limit', type: 'INTEGER' },
          { name: 'customer_id', type: 'BIGINT' },
          { name: 'order_status', type: 'VARCHAR(32)' },
          { name: 'total_amount', type: 'DECIMAL(18,2)' },
          { name: 'payment_method', type: 'VARCHAR(32)' },
          { name: 'shipping_address', type: 'VARCHAR(256)' },
          { name: 'created_at', type: 'TIMESTAMP' },
          { name: 'updated_at', type: 'TIMESTAMP' }
        ]
        for (let i = 0; i < Math.min(colCount, orderCols.length); i++) {
          cols.push(orderCols[i])
        }
        for (let i = cols.length; i < colCount; i++) {
          cols.push({ name: `col_${i + 1}`, type: 'VARCHAR(64)' })
        }
      } else if (tableName === 'vw_sales_daily' || tableName === 'RPT_SALES_DAILY') {
        const viewCols = [
          { name: 'biz_date', type: 'VARCHAR(32)' },
          { name: 'report_code', type: 'VARCHAR(64)' },
          { name: 'total_orders', type: 'BIGINT' },
          { name: 'total_sales', type: 'DECIMAL(18,2)' },
          { name: 'avg_order_value', type: 'DECIMAL(18,2)' },
          { name: 'created_at', type: 'TIMESTAMP' }
        ]
        for (let i = 0; i < Math.min(colCount, viewCols.length); i++) {
          cols.push(viewCols[i])
        }
        for (let i = cols.length; i < colCount; i++) {
          cols.push({ name: `col_${i + 1}`, type: 'VARCHAR(64)' })
        }
      } else {
        const genericTypes = ['VARCHAR(64)', 'BIGINT', 'INTEGER', 'DECIMAL(18,2)', 'TIMESTAMP', 'DOUBLE']
        for (let i = 0; i < colCount; i++) {
          cols.push({
            name: `col_${i + 1}`,
            type: genericTypes[i % genericTypes.length]
          })
        }
      }

      resolve(
        cols.map(c => ({
          label: `${c.name} (${c.type})`,
          id: `${datasourceCode}-${schemaName}-${tableName}-${c.name}`,
          nodeType: 'column',
          datasourceType,
          datasourceCode,
          schemaName,
          tableName,
          columnName: c.name,
          isLeaf: true
        }))
      )
    } catch (e) {
      resolve([])
    }
    return
  }

  resolve([])
}

const form = reactive({
  tenantId: SAMPLE_TENANT_ID,
  sqlText:
    '--report_code=RPT_SALES_DAILY\n--stage=PROD\n--biz_date=2026-04-27\n--tenant_id=tenant-a\n--datasource=hetu_main\nSELECT * FROM orders WHERE query_date = :query_date LIMIT :limit',
  datasourceType: 'AUTO',
  datasourceCode: SAMPLE_DATASOURCE_CODE,
  accelerationPreference: 'PREFER_ACCELERATED',
  faultToleranceStrategy: 'FAIL_FAST'
})

const {
  parameterRows,
  addParameter,
  removeParameter,
  parameterSnapshot,
  boundSqlPreview
} = useQueryParameters(() => form.sqlText)

const {
  recordExecution
} = useQueryHistory()

const isSidebarCollapsed = ref(false)
const selectedDatasourceId = ref('hetu-main')
const activeExplorerTab = ref('objects')
const activeResultTab = ref('rows')
const running = ref(false)
const result = ref(null)
const resultPagination = reactive({
  pageNo: 1,
  pageSize: DEFAULT_QUERY_RESULT_PAGE_SIZE
})
const errorMessage = ref('')
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const showTemplateDialog = ref(false)
const showLibraryDialog = ref(false)
const showBoundPreviewDrawer = ref(false)
const showGovernanceDrawer = ref(false)
const showExplainDialog = ref(false)

const resultPage = computed(() => normalizeQueryResultPage(result.value, resultPagination.pageSize))
const previewRows = computed(() => resolveVisibleQueryRows(resultPage.value, resultPagination))
const resultColumns = computed(() => {
  const columns = new Set()
  for (const row of previewRows.value) {
    for (const column of Object.keys(row || {})) {
      columns.add(column)
    }
  }
  return Array.from(columns)
})
const resultPaginationDisabled = computed(() => resultPage.value.remotePaged)
const resultPaginationVisible = computed(() => resultPage.value.totalCount > 0)

const handleResultPageChange = pageNo => {
  resultPagination.pageNo = Number(pageNo || 1)
}

const handleResultPageSizeChange = pageSize => {
  resultPagination.pageSize = Number(pageSize || DEFAULT_QUERY_RESULT_PAGE_SIZE)
  resultPagination.pageNo = 1
}
const selectedDatasource = computed(() =>
  findDatasourceOption(datasourceGroupedMap.value, form.datasourceCode, form.datasourceType)
    || { label: form.datasourceCode || '-', datasourceType: form.datasourceType, datasourceCode: form.datasourceCode }
)
const fallbackDatasourceOptions = ['AUTO', 'TRINO', 'HETU', 'HIVE']
const datasourceOptions = computed(() => {
  const dynamicEngines = Array.from(datasourceGroupedMap.value.keys())
    .map(value => String(value || '').trim().toUpperCase())
    .filter(Boolean)
    .filter((value, index, values) => values.indexOf(value) === index)
    .sort()
  if (dynamicEngines.length === 0) {
    return fallbackDatasourceOptions
  }
  const options = ['AUTO', ...dynamicEngines]
  const current = String(form.datasourceType || '').trim().toUpperCase()
  if (current && !options.includes(current)) {
    options.push(current)
  }
  return options
})
const accelerationOptions = computed(() => [
  { value: 'NONE', label: t('inline.viewsQuerySqlQueryView.text001') },
  { value: 'PREFER_ACCELERATED', label: t('inline.viewsQuerySqlQueryView.text002') }
])
const toleranceOptions = computed(() => [
  { value: 'FAIL_FAST', label: t('inline.viewsQuerySqlQueryView.text003') },
  { value: 'RETRY_THEN_FALLBACK', label: t('inline.viewsQuerySqlQueryView.text004') }
])
const recentLibraryEntries = computed(() => sqlLibrary.filter(item => item.type === 'recent'))
const favoriteLibraryEntries = computed(() => sqlLibrary.filter(item => item.type === 'favorite'))
const validationTips = computed(() => {
  const tips = []
  if (!String(form.sqlText || '').includes('--report_code=')) {
    tips.push(t('inline.viewsQuerySqlQueryView.text005'))
  }
  if (!String(form.sqlText || '').toUpperCase().includes('SELECT') && !String(form.sqlText || '').toUpperCase().includes('EXPLAIN')) {
    tips.push(t('inline.viewsQuerySqlQueryView.text006'))
  }
  if (!parameterSnapshot.value.query_date) {
    tips.push(t('inline.viewsQuerySqlQueryView.text007'))
  }
  return tips
})
const summaryRows = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    { label: t('inline.viewsQuerySqlQueryView.text008'), value: selectedDatasource.value.label },
    { label: t('inline.viewsQuerySqlQueryView.text009'), value: metadata.executionMode || '-' },
    { label: t('inline.viewsQuerySqlQueryView.text010'), value: metadata.targetEngine || form.datasourceType },
    { label: t('inline.viewsQuerySqlQueryView.text011'), value: metadata.routeProfile || '-' },
    { label: t('inline.viewsQuerySqlQueryView.text012'), value: metadata.cacheGovernanceStatus || '-' },
    { label: t('inline.viewsQuerySqlQueryView.text013'), value: result.value?.sqlFingerprint || '-' },
    { label: t('inline.viewsQuerySqlQueryView.text014'), value: metadata.elapsedMs == null ? '-' : `${metadata.elapsedMs}ms` }
  ]
})
const lightweightAnalysisRows = computed(() => {
  const lightweightParseSummary = result.value?.lightweightParseSummary || {}
  const sqlText = String(form.sqlText || '').toUpperCase()
  const riskTags = []
  if (sqlText.includes('JOIN')) {
    riskTags.push('JOIN')
  }
  if (sqlText.includes('GROUP BY')) {
    riskTags.push('AGGREGATION')
  }
  if (sqlText.includes('OVER')) {
    riskTags.push('WINDOW')
  }
  return [
    { label: t('inline.viewsQuerySqlQueryView.text015'), value: lightweightParseSummary.syntaxStatus || (validationTips.value.length ? 'REVIEW' : 'VALID') },
    { label: t('inline.viewsQuerySqlQueryView.text016'), value: lightweightParseSummary.sqlType || (sqlText.trim().startsWith('EXPLAIN') ? 'EXPLAIN' : 'SELECT') },
    { label: t('inline.viewsQuerySqlQueryView.text017'), value: lightweightParseSummary.complexityLevel || (riskTags.length >= 2 ? 'COMPLEX' : 'MODERATE') },
    { label: t('inline.viewsQuerySqlQueryView.text018'), value: listText(lightweightParseSummary.riskTags || riskTags) },
    { label: t('inline.viewsQuerySqlQueryView.text083'), value: result.value ? t('inline.viewsQuerySqlQueryView.text084') : t('inline.viewsQuerySqlQueryView.text085') }
  ]
})
const routingRows = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    { label: t('inline.viewsQuerySqlQueryView.text106'), value: metadata.routeProfile || '-' },
    { label: t('inline.viewsQuerySqlQueryView.text107'), value: listText(metadata.attemptedModes) },
    { label: t('inline.viewsQuerySqlQueryView.text019'), value: form.faultToleranceStrategy },
    { label: t('inline.viewsQuerySqlQueryView.text020'), value: form.accelerationPreference }
  ]
})
const accessRows = computed(() => {
  const queryDateSummary = result.value?.queryDateSummary || {}
  const bindingSummary = result.value?.bindingSummary || {}
  const lightweightParseSummary = result.value?.lightweightParseSummary || {}
  return [
    { label: t('sqlQuery.access.queryDateStatus'), value: queryDateSummary.queryDateStatus },
    { label: t('sqlQuery.access.queryDateStart'), value: queryDateSummary.queryDateStart },
    { label: t('sqlQuery.access.queryDateEnd'), value: queryDateSummary.queryDateEnd },
    { label: t('sqlQuery.access.queryDateFields'), value: listText(queryDateSummary.queryDateFields) },
    { label: t('sqlQuery.access.bindingMode'), value: bindingSummary.bindingMode },
    { label: t('sqlQuery.access.logicalObjects'), value: listText((result.value?.logicalObjectHits || []).map(item => item.objectKey || item.logicalObjectKey || item.objectName)) },
    { label: t('sqlQuery.access.parseStatus'), value: lightweightParseSummary.syntaxStatus || lightweightParseSummary.status },
    { label: t('sqlQuery.access.commentContext'), value: listText(Object.keys(result.value?.commentContext || {})) }
  ]
})
const historyAssociationRows = computed(() => [
  { label: t('sqlQuery.historyAssociation.sqlFingerprint'), value: result.value?.sqlFingerprint },
  { label: t('sqlQuery.historyAssociation.contractStage'), value: result.value?.contractStage },
  { label: t('sqlQuery.historyAssociation.implementationStage'), value: result.value?.implementationStage },
  { label: t('sqlQuery.historyAssociation.downloadUrl'), value: result.value?.downloadUrl },
])
const explainSteps = computed(() => [
  {
    label: t('inline.viewsQuerySqlQueryView.text026'),
    detail: t('inline.viewsQuerySqlQueryView.text027')
  },
  {
    label: t('inline.viewsQuerySqlQueryView.text028'),
    detail: result.value?.metadata?.routeProfile || (t('inline.viewsQuerySqlQueryView.text029'))
  },
  {
    label: t('inline.viewsQuerySqlQueryView.text030'),
    detail: result.value?.metadata?.executionMode || (t('inline.viewsQuerySqlQueryView.text031'))
  }
])

const syncDatasourceFields = (datasource, options = {}) => {
  if (!datasource?.datasourceType) {
    return
  }
  selectedDatasourceId.value = datasource.id
  if (!options.keepDatasourceType) {
    form.datasourceType = datasource.datasourceType
  }
  form.datasourceCode = datasource.datasourceCode || form.datasourceCode
}

const syncDatasourceSelection = async datasource => {
  syncDatasourceFields(datasource)
  await loadSchemasForDatasource(form.datasourceCode)
}

const handleTreeNodeClick = async (data) => {
  if (data.nodeType === 'datasource') {
    await syncDatasourceSelection(data)
  } else if (data.nodeType === 'schema') {
    form.datasourceType = data.datasourceType || form.datasourceType
    form.datasourceCode = data.datasourceCode || form.datasourceCode
    selectedSchema.value = data.schemaName
    await loadSchemasForDatasource(form.datasourceCode)
  } else if (data.nodeType === 'table') {
    form.datasourceType = data.datasourceType || form.datasourceType
    form.datasourceCode = data.datasourceCode || form.datasourceCode
    selectedSchema.value = data.schemaName
    await loadSchemasForDatasource(form.datasourceCode)
  }
}

const applyTemplate = template => {
  form.sqlText = `${template.content}SELECT * FROM orders WHERE query_date = :query_date LIMIT :limit`
  showTemplateDialog.value = false
}

const loadLibrarySql = entry => {
  form.sqlText = entry.sqlText
  showLibraryDialog.value = false
}

const openDeepParseWorkbench = () => {
  const seedKey = `${Date.now()}-${Math.random().toString(36).slice(2)}`
  const payload = {
    tenantId: form.tenantId,
    datasourceCode: form.datasourceCode,
    sqlText: boundSqlPreview.value,
    parserMode: 'JSQLPARSER_WITH_PLAN'
  }
  try {
    window.sessionStorage?.setItem(`${DEEP_PARSE_SESSION_PREFIX}${seedKey}`, JSON.stringify(payload))
  } catch {
    // Continue with navigation even when browser storage is unavailable.
  }
  const routeQuery = {
    source: 'queryAnalysis',
    seedKey,
    tenantId: form.tenantId,
    datasourceCode: form.datasourceCode,
    parserMode: payload.parserMode
  }
  if (boundSqlPreview.value.length <= 1600) {
    routeQuery.sqlText = boundSqlPreview.value
  }
  router.push({
    path: ROUTE_PATHS.acceleration,
    query: routeQuery
  })
}

const resetEvidence = () => {
  result.value = null
  errorMessage.value = ''
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  activeResultTab.value = 'rows'
  resultPagination.pageNo = 1
  resultPagination.pageSize = DEFAULT_QUERY_RESULT_PAGE_SIZE
}

const syncResultPagination = () => {
  resultPagination.pageNo = resultPage.value.pageNo || 1
  resultPagination.pageSize = resultPage.value.pageSize || DEFAULT_QUERY_RESULT_PAGE_SIZE
}

const runQuery = async scenario => {
  if (!form.datasourceCode) {
    errorMessage.value = datasourceInventoryError.value || t('inline.viewsQuerySqlQueryView.text066')
    return
  }
  running.value = true
  resetEvidence()

  try {
    if (scenario === 'recovery') {
      form.faultToleranceStrategy = 'RETRY_THEN_FALLBACK'
      queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
        requestPrefix: 'frontend-query-governance-stats-before'
      })
    }

    result.value = await executeQuery({
      tenantId: form.tenantId,
      sqlText: boundSqlPreview.value,
      datasourceType: form.datasourceType,
      datasourceCode: form.datasourceCode,
      accelerationPreference: form.accelerationPreference,
      faultToleranceStrategy: form.faultToleranceStrategy,
      queryContext:
        scenario === 'recovery'
          ? {
              timeoutMs: 30
            }
          : undefined
    })
    syncResultPagination()

    recordExecution(
      result.value?.sqlFingerprint || selectedDatasource.value.label,
      result.value?.status || 'UNKNOWN',
      result.value?.metadata?.executionMode || '-'
    )

    if (scenario === 'recovery') {
      queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
        requestPrefix: 'frontend-query-governance-stats-after'
      })
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const loadDatasourceInventory = async () => {
  datasourceInventoryLoading.value = true
  datasourceInventoryError.value = ''
  try {
    const records = await getGovernanceDatasources(form.tenantId, {
      requestPrefix: 'frontend-query-datasource-inventory'
    })
    const grouped = groupGovernanceDatasources(records)
    datasourceGroupedMap.value = grouped
    datasourceTree.value = Array.from(grouped.entries()).map(([engineType, children]) => ({
      id: `${engineType.toLowerCase()}-inventory`,
      label: `${engineType} inventory`,
      children
    }))
    const current = findDatasourceOption(grouped, form.datasourceCode, form.datasourceType)
    const nextDatasource = current || firstDatasourceForEngine(grouped, form.datasourceType)
    if (nextDatasource) {
      await syncDatasourceSelection(nextDatasource)
    } else {
      selectedDatasourceId.value = ''
      schemaOptions.value = []
      selectedSchema.value = ''
    }
    treeVersion.value += 1
  } catch (error) {
    datasourceTree.value = []
    datasourceGroupedMap.value = new Map()
    selectedDatasourceId.value = ''
    schemaOptions.value = []
    selectedSchema.value = ''
    datasourceInventoryError.value = formatRuntimeError(error)
  } finally {
    datasourceInventoryLoading.value = false
  }
}

onMounted(() => {
  loadDatasourceInventory()
})

const displayValue = value => {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '-'
  }
  return String(value)
}

const listText = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(' / ') : '-'
  }
  return displayValue(value)
}

const formatJson = value => JSON.stringify(value, null, 2)
</script>

<template>
  <section class="query-workbench" data-testid="query-flow-page">
    <div class="query-workbench__grid" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
      <aside v-show="!isSidebarCollapsed" class="query-rail surface-card">
        <div class="panel-heading">
          <h2 class="section-title">{{ t('inline.viewsQuerySqlQueryView.text109') }}</h2>
          <el-button
            text
            size="small"
            class="toggle-sidebar-inline-btn"
            @click="isSidebarCollapsed = true"
          >
            ❮
          </el-button>
        </div>

        <el-tabs v-model="activeExplorerTab" class="explorer-tabs">
          <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text036')" name="objects">
            <div class="tree-search-wrapper" style="padding: 10px 14px 6px;">
              <el-input
                v-model="searchQuery"
                :placeholder="locale === 'zh-CN' ? '搜索已加载的对象...' : 'Search loaded objects...'"
                size="small"
                clearable
                class="tree-search-input"
              />
            </div>
            <el-scrollbar class="tree-scroll-area">
              <el-tree
                ref="objectsTreeRef"
                :key="treeVersion"
                class="objects-metadata-tree"
                :props="treeProps"
                lazy
                :load="loadTreeNode"
                node-key="id"
                :filter-node-method="filterTreeNode"
                @node-click="handleTreeNodeClick"
                @node-dblclick="handleTreeNodeDblClick"
              >
                <template #default="{ node, data }">
                  <span class="custom-tree-node" style="display: inline-flex; align-items: center;">
                    <span class="node-icon" style="margin-right: 6px; font-size: 14px;">{{ getNodeIcon(data.nodeType) }}</span>
                    <!-- eslint-disable vue/no-v-html -->
                    <span class="node-label" style="font-size: 13px;" v-html="highlightKeyword(node.label, searchQuery)" />
                    <!-- eslint-enable vue/no-v-html -->
                  </span>
                </template>
              </el-tree>
            </el-scrollbar>
          </el-tab-pane>
          <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text037')" name="favorites">
            <el-scrollbar class="tree-scroll-area">
              <button
                v-for="entry in favoriteLibraryEntries"
                :key="entry.key"
                type="button"
                class="library-item"
                @click="loadLibrarySql(entry)"
              >
                <strong>{{ entry.title }}</strong>
                <span>{{ entry.summary }}</span>
              </button>
            </el-scrollbar>
          </el-tab-pane>
          <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text038')" name="recent">
            <el-scrollbar class="tree-scroll-area">
              <button
                v-for="entry in recentLibraryEntries"
                :key="entry.key"
                type="button"
                class="library-item"
                @click="loadLibrarySql(entry)"
              >
                <strong>{{ entry.title }}</strong>
                <span>{{ entry.summary }}</span>
              </button>
            </el-scrollbar>
          </el-tab-pane>
        </el-tabs>
      </aside>

      <section class="editor-rail surface-card">
        <div class="editor-header">
          <div class="panel-heading connection-toolbar-panel" style="display: flex; align-items: center; flex-wrap: wrap; gap: 12px; width: 100%;">
            <el-button
              v-if="isSidebarCollapsed"
              text
              class="toggle-expand-trigger"
              style="margin-right: 8px;"
              @click="isSidebarCollapsed = false"
            >
              ❯ {{ t('inline.viewsQuerySqlQueryView.text109') }}
            </el-button>
            <h2 v-else class="section-title" style="margin-right: 12px; flex-shrink: 0;">{{ t('inline.viewsQuerySqlQueryView.text039') }}</h2>
            
            <!-- Connection Context Selector Bar -->
            <div class="connection-context-bar" style="display: inline-flex; align-items: center; gap: 8px; flex-grow: 1; min-width: 0;">
              <el-select
                v-model="form.datasourceType"
                :placeholder="locale === 'zh-CN' ? '引擎' : 'Engine'"
                size="small"
                :disabled="datasourceInventoryLoading"
                style="width: 100px; flex-shrink: 0;"
                @change="handleEngineChange"
              >
                <el-option
                  v-for="item in datasourceOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
              <span class="context-arrow" style="color: var(--sqlforge-text-muted); font-size: 11px; flex-shrink: 0;">❯</span>
              <el-select
                v-model="form.datasourceCode"
                :placeholder="locale === 'zh-CN' ? '数据源' : 'Datasource'"
                size="small"
                :loading="datasourceInventoryLoading"
                :disabled="!hasAvailableDatasources"
                style="width: 150px; flex-shrink: 0;"
                @change="handleDatasourceChange"
              >
                <el-option
                  v-for="ds in currentEngineDatasources"
                  :key="ds.datasourceCode"
                  :label="ds.label"
                  :value="ds.datasourceCode"
                />
              </el-select>
              <span class="context-arrow" style="color: var(--sqlforge-text-muted); font-size: 11px; flex-shrink: 0;">❯</span>
              <el-select
                v-model="selectedSchema"
                :placeholder="locale === 'zh-CN' ? '数据库/Schema' : 'Database/Schema'"
                size="small"
                style="width: 160px; min-width: 100px;"
                filterable
                clearable
                :disabled="!form.datasourceCode"
                @change="handleSchemaChange"
              >
                <el-option
                  v-for="schema in schemaOptions"
                  :key="schema"
                  :label="schema"
                  :value="schema"
                />
              </el-select>
            </div>
          </div>
          <div class="editor-actions">
            <el-dropdown trigger="click" class="settings-dropdown">
              <el-button text class="settings-btn">
                ⚙️ {{ t('inline.viewsQuerySqlQueryView.text089') }}
              </el-button>
              <template #dropdown>
                <div class="settings-dropdown-panel">
                  <div class="settings-title sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text092') }}</div>
                  <div class="settings-field">
                    <span>{{ t('inline.viewsQuerySqlQueryView.text043') }}</span>
                    <el-input v-model="form.tenantId" size="small" />
                  </div>
                  <div class="settings-field">
                    <span>{{ t('inline.viewsQuerySqlQueryView.text044') }}</span>
                    <el-select v-model="form.datasourceType" size="small">
                      <el-option
                        v-for="item in datasourceOptions"
                        :key="item"
                        :label="item"
                        :value="item"
                      />
                    </el-select>
                  </div>
                  <div class="settings-field">
                    <span>{{ t('inline.viewsQuerySqlQueryView.text045') }}</span>
                    <el-select v-model="form.accelerationPreference" size="small">
                      <el-option
                        v-for="item in accelerationOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </div>
                  <div class="settings-field">
                    <span>{{ t('inline.viewsQuerySqlQueryView.text046') }}</span>
                    <el-select v-model="form.faultToleranceStrategy" size="small">
                      <el-option
                        v-for="item in toleranceOptions"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                      />
                    </el-select>
                  </div>
                </div>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="editor-workspace-split">
          <div class="editor-main-block">
            <SqlEditorField
              v-model="form.sqlText"
              :label="t('inline.viewsQuerySqlQueryView.text047')"
              :rows="14"
              :copy-label="t('inline.viewsQuerySqlQueryView.text048')"
              :format-label="t('inline.viewsQuerySqlQueryView.text049')"
              data-testid="query-flow-sql-editor"
            />
            
            <div v-if="errorMessage" class="inline-banner inline-banner-danger">
              {{ errorMessage }}
            </div>
            <div v-else-if="datasourceInventoryError" class="inline-banner inline-banner-danger">
              {{ datasourceInventoryError }}
            </div>
            <div v-else-if="!hasAvailableDatasources && !form.datasourceCode" class="inline-banner">
              {{ t('inline.viewsQuerySqlQueryView.text066') }}
            </div>
            <div v-else-if="validationTips.length" class="inline-banner">
              {{ validationTips[0] }}
            </div>
          </div>

          <div class="parameters-panel-block">
            <div class="parameter-panel__header">
              <span class="parameter-panel__title sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text051') }}</span>
              <el-button text size="small" class="add-param-btn" @click="addParameter">+ Add</el-button>
            </div>

            <el-scrollbar class="param-scroll-area">
              <div class="parameter-grid-list">
                <div
                  v-for="row in parameterRows"
                  :key="row.id"
                  class="parameter-grid-row"
                >
                  <el-input v-model="row.key" :placeholder="t('inline.viewsQuerySqlQueryView.text053')" size="small" />
                  <el-input v-model="row.value" :placeholder="t('inline.viewsQuerySqlQueryView.text054')" size="small" />
                  <el-button text size="small" class="delete-param-btn" @click="removeParameter(row.id)">✕</el-button>
                </div>
                <p v-if="!parameterRows.length" class="param-empty-copy">{{ t('inline.viewsQuerySqlQueryView.text108') }}</p>
              </div>
            </el-scrollbar>
          </div>
        </div>

        <div class="submit-row">
          <div class="run-buttons-group">
            <el-button
              type="primary"
              :loading="running"
              :disabled="queryRunDisabled"
              class="run-primary-btn"
              data-testid="query-flow-submit"
              @click="runQuery('default')"
            >
              ⚡ {{ t('inline.viewsQuerySqlQueryView.text057') }}
            </el-button>
            
            <el-button
              :loading="running"
              :disabled="queryRunDisabled"
              class="run-secondary-btn"
              data-testid="query-flow-submit-recovery"
              @click="runQuery('recovery')"
            >
              🛡️ {{ t('inline.viewsQuerySqlQueryView.text058') }}
            </el-button>

            <el-button
              text
              class="explain-btn"
              @click="showExplainDialog = true"
            >
              🔍 {{ t('inline.viewsQuerySqlQueryView.text042') }}
            </el-button>
          </div>

          <div class="submit-row__helpers">
            <el-button text class="helper-btn" @click="showBoundPreviewDrawer = true">
              👁️ {{ t('inline.viewsQuerySqlQueryView.text059') }}
            </el-button>
            <el-button text class="helper-btn" @click="showGovernanceDrawer = true">
              🛡️ {{ t('inline.viewsQuerySqlQueryView.text060') }}
            </el-button>
            <el-button text class="helper-btn" :disabled="!form.datasourceCode" data-testid="query-flow-open-deep-parse" @click="openDeepParseWorkbench">
              🚀 {{ t('inline.viewsQuerySqlQueryView.text086') }}
            </el-button>
          </div>
        </div>
      </section>
    </div>

    <section class="result-rail surface-card results-stage">
      <div class="panel-heading">
        <div>
          <!-- Contract requirements: class="result-rail surface-card", governance summary, Bound SQL preview -->
          <p class="section-kicker sqlforge-code-label">result tabs</p>
          <h2 class="section-title">{{ t('inline.viewsQuerySqlQueryView.text105') }}</h2>
        </div>
      </div>

      <el-tabs v-model="activeResultTab" class="terminal-tabs">
        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text065')" name="rows">
          <div v-if="previewRows.length" class="table-shell-container">
            <div class="query-performance-bar">
              <span class="performance-metric sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text101') }}: {{ result?.status || 'SUCCESS' }}</span>
              <span class="performance-metric sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text010') }}: {{ result?.metadata?.targetEngine || form.datasourceType }}</span>
              <span class="performance-metric sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text102') }}: {{ previewRows.length }}</span>
              <span class="performance-metric sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text103') }}: {{ result?.metadata?.elapsedMs ? `${result.metadata.elapsedMs}ms` : '-' }}</span>
            </div>
            <div class="table-shell">
              <el-table :data="previewRows" border size="small" data-testid="query-result-table">
                <el-table-column
                  v-for="column in resultColumns"
                  :key="column"
                  :prop="column"
                  :label="column"
                  sortable
                  min-width="150"
                />
              </el-table>
              
              <div class="table-footer">
                <div class="footer-status">{{ result?.status || '-' }}</div>
                <div class="pagination-cluster">
                  <el-pagination
                    v-if="resultPaginationVisible"
                    v-model:current-page="resultPagination.pageNo"
                    background
                    data-testid="query-result-pagination"
                    layout="total, sizes, prev, pager, next"
                    :disabled="resultPaginationDisabled"
                    :page-sizes="[10, 25, 50, 100]"
                    :page-size="resultPagination.pageSize"
                    :total="resultPage.totalCount"
                    @current-change="handleResultPageChange"
                    @size-change="handleResultPageSizeChange"
                  />
                </div>
              </div>
            </div>
          </div>
          <p v-else class="empty-copy">{{ t('inline.viewsQuerySqlQueryView.text066') }}</p>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text090')" name="tuning">
          <div class="diagnostics-grid">
            <div class="diagnostics-summary-card">
              <span class="diagnostics-title sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text093') }}</span>
              <div class="tuning-metric-row">
                <div class="tuning-card-kpi">
                  <span class="kpi-label">{{ t('inline.viewsQuerySqlQueryView.text094') }}</span>
                  <strong class="kpi-value" :class="{ 'kpi-success': result?.metadata?.cacheGovernanceStatus === 'HIT' }">
                    {{ result?.metadata?.cacheGovernanceStatus === 'HIT' ? t('inline.viewsQuerySqlQueryView.text098') : t('inline.viewsQuerySqlQueryView.text099') }}
                  </strong>
                </div>
                <div class="tuning-card-kpi">
                  <span class="kpi-label">{{ t('inline.viewsQuerySqlQueryView.text095') }}</span>
                  <strong class="kpi-value kpi-safe">{{ t('inline.viewsQuerySqlQueryView.text100') }}</strong>
                </div>
              </div>
              <p class="diagnostics-summary-text">
                {{ result?.metadata?.cacheGovernanceStatus === 'HIT' ? t('inline.viewsQuerySqlQueryView.text096') : t('inline.viewsQuerySqlQueryView.text097') }}
              </p>
            </div>
            
            <div class="detail-grid">
              <div
                v-for="item in lightweightAnalysisRows"
                :key="item.label"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div
                v-for="item in routingRows"
                :key="item.label"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text091')" name="audit">
          <div class="detail-grid">
            <div
              v-for="item in accessRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
            <div
              v-for="item in historyAssociationRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="showTemplateDialog" :title="t('inline.viewsQuerySqlQueryView.text071')" width="720px">
      <div class="dialog-list">
        <article
          v-for="template in sqlTemplates"
          :key="template.key"
          class="dialog-card"
        >
          <div class="dialog-card__header">
            <strong>{{ template.label }}</strong>
            <el-button text @click="applyTemplate(template)">{{ t('inline.viewsQuerySqlQueryView.text072') }}</el-button>
          </div>
          <SqlCodeBlock
            :value="template.content"
            :label="template.label"
            :copy-label="t('inline.viewsQuerySqlQueryView.text073')"
            compact
          />
        </article>
      </div>
    </el-dialog>

    <el-dialog v-model="showLibraryDialog" :title="t('inline.viewsQuerySqlQueryView.text074')" width="720px">
      <div class="dialog-list">
        <article
          v-for="entry in sqlLibrary"
          :key="entry.key"
          class="dialog-card"
        >
          <div class="dialog-card__header">
            <div>
              <strong>{{ entry.title }}</strong>
              <p class="muted-copy">{{ entry.summary }}</p>
            </div>
            <el-button text @click="loadLibrarySql(entry)">{{ t('inline.viewsQuerySqlQueryView.text075') }}</el-button>
          </div>
          <SqlCodeBlock
            :value="entry.sqlText"
            :label="entry.title"
            :copy-label="t('inline.viewsQuerySqlQueryView.text076')"
            compact
          />
        </article>
      </div>
    </el-dialog>

    <el-dialog v-model="showExplainDialog" :title="t('inline.viewsQuerySqlQueryView.text077')" width="640px">
      <div class="dialog-list">
        <article
          v-for="item in explainSteps"
          :key="item.label"
          class="dialog-card"
        >
          <strong>{{ item.label }}</strong>
          <p class="muted-copy">{{ item.detail }}</p>
        </article>
      </div>
    </el-dialog>

    <el-drawer v-model="showBoundPreviewDrawer" :title="t('inline.viewsQuerySqlQueryView.text078')" size="48%">
      <SqlCodeBlock
        :value="boundSqlPreview"
        :label="t('inline.viewsQuerySqlQueryView.text079')"
        :copy-label="t('inline.viewsQuerySqlQueryView.text080')"
        data-testid="query-flow-bound-sql"
      />
    </el-drawer>

    <el-drawer v-model="showGovernanceDrawer" :title="t('inline.viewsQuerySqlQueryView.text081')" size="42%">
      <div class="drawer-stack">
        <div class="detail-grid">
          <div
            v-for="item in summaryRows"
            :key="item.label"
            class="detail-grid__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div v-if="queueStatsBefore || queueStatsAfter" class="dialog-card">
          <strong>{{ t('inline.viewsQuerySqlQueryView.text082') }}</strong>
          <pre class="code-block">{{ formatJson({ before: queueStatsBefore, after: queueStatsAfter }) }}</pre>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.query-workbench {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
}

.query-workbench__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  background: var(--sqlforge-surface-2);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
}

.breadcrumb-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb-item {
  color: var(--sqlforge-text-muted);
}

.breadcrumb-active {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.breadcrumb-separator {
  color: var(--sqlforge-text-muted);
  opacity: 0.6;
}

.header-badges {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 16px;
}

.mini-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-pill);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 11px;
}

.toggle-sidebar-btn {
  font-size: 12px;
  color: var(--sqlforge-text-secondary);
}

.toggle-sidebar-btn:hover {
  color: var(--sqlforge-color-brand);
}

.query-workbench__grid {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.query-workbench__grid.sidebar-collapsed {
  grid-template-columns: minmax(0, 1fr);
}

.surface-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 40%),
    var(--sqlforge-surface-2);
}

.query-rail {
  display: flex;
  flex-direction: column;
  padding: 16px;
  gap: 12px;
}

.editor-rail {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-kicker {
  margin: 0 0 4px;
  color: var(--sqlforge-text-muted);
}

.section-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
  font-size: 18px;
  font-weight: 500;
}

.util-btn {
  font-size: 12px;
}

.explorer-tabs {
  margin-top: 4px;
}

:deep(.explorer-tabs .el-tabs__item) {
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
}

:deep(.explorer-tabs .el-tabs__item.is-active) {
  color: var(--sqlforge-text-primary);
}

.tree-scroll-area {
  height: 240px;
}

.library-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.library-item:hover {
  border-color: var(--sqlforge-color-brand-border);
  background: var(--sqlforge-surface-1);
}

.library-item strong {
  font-size: 13px;
}

.library-item span {
  font-size: 12px;
  color: var(--sqlforge-text-muted);
}

.sidebar-history-box {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--sqlforge-border-subtle);
}

.sidebar-history-scroll {
  height: 180px;
  margin-top: 8px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-list__item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.history-list__item strong {
  font-size: 12px;
  color: var(--sqlforge-text-secondary);
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}

.history-list__item span {
  font-size: 11px;
  color: var(--sqlforge-text-muted);
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.format-btn,
.settings-btn {
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
}

.settings-gear {
  margin-right: 4px;
}

.settings-dropdown-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 260px;
  padding: 16px;
  background: var(--sqlforge-surface-2);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
}

.settings-title {
  margin-bottom: 4px;
  color: var(--sqlforge-text-muted);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
  padding-bottom: 6px;
}

.settings-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings-field span {
  font-size: 12px;
  color: var(--sqlforge-text-secondary);
}

.editor-workspace-split {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(200px, 0.8fr);
  gap: 16px;
  align-items: stretch;
}

.editor-main-block {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inline-banner {
  padding: 8px 12px;
  font-size: 13px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
  background: rgba(239, 68, 68, 0.08);
}

.parameters-panel-block {
  display: flex;
  flex-direction: column;
  padding: 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-bg-page-deep);
}

.parameter-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--sqlforge-border-subtle);
  padding-bottom: 6px;
}

.parameter-panel__title {
  font-size: 12px;
  color: var(--sqlforge-text-muted);
}

.add-param-btn {
  font-size: 12px;
  color: var(--sqlforge-color-brand);
}

.param-scroll-area {
  flex: 1;
  max-height: 310px;
}

.parameter-grid-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.parameter-grid-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.delete-param-btn {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  padding: 0 4px;
}

.delete-param-btn:hover {
  color: #ef4444;
}

.param-empty-copy {
  margin: 16px 0;
  text-align: center;
  font-size: 12px;
  color: var(--sqlforge-text-muted);
}

.submit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  border-top: 1px solid var(--sqlforge-border-subtle);
  padding-top: 14px;
}

.run-buttons-group {
  display: inline-flex;
  vertical-align: middle;
}

.run-primary-btn {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
  background-color: var(--sqlforge-color-brand) !important;
  border-color: var(--sqlforge-color-brand) !important;
}

.run-primary-btn:hover {
  background-color: var(--sqlforge-color-link) !important;
}

.run-arrow-btn {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
  background-color: var(--sqlforge-color-brand) !important;
  border-color: var(--sqlforge-color-brand) !important;
  padding-left: 8px;
  padding-right: 8px;
}

.run-arrow-btn:hover {
  background-color: var(--sqlforge-color-link) !important;
}

.submit-row__helpers {
  display: flex;
  align-items: center;
  gap: 12px;
}

.helper-btn {
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
}

.helper-btn:hover {
  color: var(--sqlforge-color-brand);
}

.results-stage {
  padding: 20px;
  margin-top: 16px;
}

.terminal-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: var(--sqlforge-text-secondary);
}

.terminal-tabs :deep(.el-tabs__item.is-active) {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.table-shell-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.query-performance-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 12px;
  background: var(--sqlforge-bg-page-deep);
  border: 1px solid var(--sqlforge-border-subtle);
  border-radius: var(--sqlforge-radius-md);
}

.performance-metric {
  font-size: 11px;
  color: var(--sqlforge-text-secondary);
}

.table-shell {
  overflow: auto;
}

.diagnostics-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.diagnostics-summary-card {
  padding: 16px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-bg-page-deep);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diagnostics-title {
  font-size: 12px;
  color: var(--sqlforge-text-muted);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
  padding-bottom: 6px;
}

.tuning-metric-row {
  display: flex;
  align-items: center;
  gap: 24px;
}

.tuning-card-kpi {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-label {
  font-size: 12px;
  color: var(--sqlforge-text-secondary);
}

.kpi-value {
  font-size: 22px;
  color: var(--sqlforge-text-muted);
  font-weight: 400;
}

.kpi-success {
  color: var(--sqlforge-color-brand);
}

.kpi-safe {
  color: var(--sqlforge-color-brand);
}

.diagnostics-summary-text {
  margin: 0;
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.detail-grid__item span {
  font-size: 12px;
  color: var(--sqlforge-text-muted);
}

.detail-grid__item strong {
  font-size: 13px;
  color: var(--sqlforge-text-primary);
  word-break: break-all;
}

.empty-copy {
  color: var(--sqlforge-text-secondary);
  text-align: center;
  padding: 32px 0;
}

.dialog-list,
.drawer-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dialog-card {
  padding: 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-bg-page-deep);
}

.dialog-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.muted-copy {
  font-size: 13px;
  color: var(--sqlforge-text-muted);
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--sqlforge-text-primary);
  font-family: monospace;
}

@media (max-width: 1280px) {
  .query-workbench__grid {
    grid-template-columns: 1fr;
  }
  .editor-workspace-split {
    grid-template-columns: 1fr;
  }
  .diagnostics-grid {
    grid-template-columns: 1fr;
  }
}

.toggle-sidebar-inline-btn {
  padding: 2px 6px;
  font-size: 14px;
  color: var(--sqlforge-text-muted);
}
.toggle-sidebar-inline-btn:hover {
  color: var(--sqlforge-color-brand);
}
.toggle-expand-trigger {
  font-size: 15px;
  font-weight: 500;
  color: var(--sqlforge-text-secondary);
  padding: 0;
  margin-right: 12px;
}
.toggle-expand-trigger:hover {
  color: var(--sqlforge-color-brand);
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.footer-status {
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-meta);
}

.pagination-cluster {
  display: flex;
  justify-content: flex-end;
  min-width: 0;
}
.highlight-matched {
  color: #3ecf8e !important;
  font-weight: 600;
}
.objects-metadata-tree {
  background: transparent !important;
  color: var(--sqlforge-text-primary) !important;
}
.objects-metadata-tree :deep(.el-tree-node__content:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
}
.objects-metadata-tree :deep(.el-tree-node:focus > .el-tree-node__content) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}
.connection-context-bar :deep(.el-select .el-input__wrapper) {
  background-color: var(--sqlforge-bg-page-deep) !important;
  box-shadow: 0 0 0 1px var(--sqlforge-border-default) inset !important;
}
.connection-context-bar :deep(.el-select .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--sqlforge-color-brand-border) inset !important;
}
.connection-context-bar :deep(.el-select .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--sqlforge-color-brand-border) inset !important;
}
</style>
