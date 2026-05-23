<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  executeQuery,
  formatRuntimeError,
  getGovernanceDatasources,
  getGovernanceMessageStats
} from '../../services/runtimeGateApi'
import MetricCard from '../common/MetricCard.vue'
import SectionHeader from '../common/SectionHeader.vue'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import { formatSqlText } from '../common/sqlFormatting.mjs'
import { sqlTemplates, sqlLibrary } from './sqlTemplates'
import {
  DEFAULT_QUERY_RESULT_PAGE_SIZE,
  normalizeQueryResultPage,
  resolveVisibleQueryRows
} from './queryResultPage.mjs'
import { useQueryParameters } from './useQueryParameters'
import { useQueryHistory } from './useQueryHistory'

const { t } = useI18n()
const router = useRouter()

const DEEP_PARSE_SESSION_PREFIX = 'sqlforge:query-analysis:deep-parse:'

const datasourceTree = ref([])

const form = reactive({
  tenantId: 'tenant-a',
  sqlText:
    '--report_code=RPT_SALES_DAILY\n--stage=PROD\n--biz_date=2026-04-27\n--tenant_id=tenant-a\n--datasource=hetu_main\nSELECT * FROM orders WHERE query_date = :query_date LIMIT :limit',
  datasourceType: 'AUTO',
  datasourceCode: 'hetu_main',
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
  executionHistory,
  recordExecution
} = useQueryHistory()

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
const resultPaginationVisible = computed(() => resultPage.value.totalCount > resultPagination.pageSize)
const selectedDatasource = computed(() => {
  for (const group of datasourceTree.value) {
    for (const item of group.children || []) {
      if (item.id === selectedDatasourceId.value) {
        return item
      }
    }
  }
  return datasourceTree.value[0]?.children?.[0] || { label: form.datasourceCode || '-', datasourceType: form.datasourceType, datasourceCode: form.datasourceCode }
})
const datasourceOptions = computed(() => ['AUTO', 'TRINO', 'HETU', 'HIVE'])
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
const queryHeroPills = computed(() => [
  form.tenantId || 'tenant-a',
  form.datasourceType,
  selectedDatasource.value.label
])
const queryHeroMetrics = computed(() => [
  {
    key: 'resultRows',
    label: t('sqlQuery.metrics.resultRows'),
    value: resultPage.value.totalCount,
    trend: result.value?.status || t('sqlQuery.metrics.pending'),
    detail: t('sqlQuery.metrics.resultRowsDetail'),
    tone: previewRows.value.length > 0 ? 'success' : 'neutral'
  },
  {
    key: 'validationTips',
    label: t('sqlQuery.metrics.validationTips'),
    value: validationTips.value.length,
    trend: validationTips.value.length > 0 ? t('sqlQuery.metrics.review') : t('sqlQuery.metrics.ready'),
    detail: t('sqlQuery.metrics.validationTipsDetail'),
    tone: validationTips.value.length > 0 ? 'warning' : 'success'
  },
  {
    key: 'recentRuns',
    label: t('sqlQuery.metrics.recentRuns'),
    value: executionHistory.value.length,
    trend: t('sqlQuery.metrics.sessionOnly'),
    detail: t('sqlQuery.metrics.recentRunsDetail'),
    tone: executionHistory.value.length > 0 ? 'neutral' : 'warning'
  }
])
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
    { label: 'routeProfile', value: metadata.routeProfile || '-' },
    { label: 'attemptedModes', value: listText(metadata.attemptedModes) },
    { label: t('inline.viewsQuerySqlQueryView.text019'), value: form.faultToleranceStrategy },
    { label: t('inline.viewsQuerySqlQueryView.text020'), value: form.accelerationPreference }
  ]
})
const recommendationRows = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    {
      label: t('inline.viewsQuerySqlQueryView.text021'),
      value: metadata.cacheGovernanceStatus === 'HIT' ? (t('inline.viewsQuerySqlQueryView.text022')) : (t('inline.viewsQuerySqlQueryView.text023'))
    },
    {
      label: t('inline.viewsQuerySqlQueryView.text024'),
      value: t('inline.viewsQuerySqlQueryView.text025')
    }
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
  { label: t('sqlQuery.historyAssociation.historyBoundary'), value: t('sqlQuery.historyAssociation.backendHistory') }
])
const queryMetricProps = item => ({
  label: item.label,
  value: item.value,
  trend: item.trend,
  detail: item.detail,
  tone: item.tone
})
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

const syncDatasourceSelection = datasource => {
  if (!datasource?.datasourceType) {
    return
  }
  selectedDatasourceId.value = datasource.id
  form.datasourceType = datasource.datasourceType
  form.datasourceCode = datasource.datasourceCode || form.datasourceCode
}

const applyTemplate = template => {
  form.sqlText = `${template.content}SELECT * FROM orders WHERE query_date = :query_date LIMIT :limit`
  showTemplateDialog.value = false
}

const loadLibrarySql = entry => {
  form.sqlText = entry.sqlText
  showLibraryDialog.value = false
}

const formatSql = () => {
  form.sqlText = formatSqlText(form.sqlText)
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

const handleResultPageChange = pageNo => {
  resultPagination.pageNo = Number(pageNo || 1)
}

const handleResultPageSizeChange = pageSize => {
  resultPagination.pageSize = Number(pageSize || DEFAULT_QUERY_RESULT_PAGE_SIZE)
  resultPagination.pageNo = 1
}

const runQuery = async scenario => {
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
  const records = await getGovernanceDatasources(form.tenantId, {
    requestPrefix: 'frontend-query-datasource-inventory'
  })
  const grouped = new Map()
  for (const record of records || []) {
    const engineType = String(record.engineType || 'UNKNOWN').toUpperCase()
    if (!grouped.has(engineType)) {
      grouped.set(engineType, [])
    }
    grouped.get(engineType).push({
      id: record.datasourceId || `${engineType}-${record.datasourceCode}`,
      label: `${record.datasourceCode}.${record.stage || 'PROD'}`,
      datasourceType: engineType,
      datasourceCode: record.datasourceCode
    })
  }
  datasourceTree.value = Array.from(grouped.entries()).map(([engineType, children]) => ({
    id: `${engineType.toLowerCase()}-inventory`,
    label: `${engineType} inventory`,
    children
  }))
  const firstDatasource = datasourceTree.value[0]?.children?.[0]
  if (firstDatasource) {
    syncDatasourceSelection(firstDatasource)
  }
}

onMounted(() => {
  loadDatasourceInventory().catch(() => {
    datasourceTree.value = []
  })
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
    <header class="query-workbench__header surface-card">
      <SectionHeader
        :eyebrow="t('sqlQuery.hero.eyebrow')"
        :title="t('sqlQuery.title')"
        :summary="t('sqlQuery.summary')"
        size="compact"
      >
        <template #actions>
          <span v-for="pill in queryHeroPills" :key="pill" class="mini-pill">{{ pill }}</span>
        </template>
      </SectionHeader>
      <div class="query-hero-metrics">
        <MetricCard
          v-for="item in queryHeroMetrics"
          :key="item.key"
          v-bind="queryMetricProps(item)"
        />
      </div>
    </header>

    <div class="query-workbench__grid">
      <aside class="query-rail surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text032') }}</p>
            <h2 class="section-title">{{ t('inline.viewsQuerySqlQueryView.text033') }}</h2>
          </div>
          <div class="utility-actions">
            <el-button text @click="showTemplateDialog = true">{{ t('inline.viewsQuerySqlQueryView.text034') }}</el-button>
            <el-button text @click="showLibraryDialog = true">{{ t('inline.viewsQuerySqlQueryView.text035') }}</el-button>
          </div>
        </div>

        <el-tabs v-model="activeExplorerTab">
          <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text036')" name="objects">
            <el-tree
              :data="datasourceTree"
              node-key="id"
              default-expand-all
              @node-click="syncDatasourceSelection"
            />
          </el-tab-pane>
          <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text037')" name="favorites">
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
          </el-tab-pane>
          <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text038')" name="recent">
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
          </el-tab-pane>
        </el-tabs>
      </aside>

      <section class="editor-rail surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text039') }}</p>
            <h2 class="section-title">{{ t('inline.viewsQuerySqlQueryView.text040') }}</h2>
          </div>
          <div class="utility-actions">
            <el-button text @click="formatSql">{{ t('inline.viewsQuerySqlQueryView.text041') }}</el-button>
            <el-button text @click="showExplainDialog = true">{{ t('inline.viewsQuerySqlQueryView.text042') }}</el-button>
          </div>
        </div>

        <div v-if="errorMessage" class="inline-banner inline-banner-danger">
          {{ errorMessage }}
        </div>
        <div v-else-if="validationTips.length" class="inline-banner">
          {{ validationTips[0] }}
        </div>

        <div class="field-grid">
          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsQuerySqlQueryView.text043') }}</span>
            <el-input v-model="form.tenantId" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsQuerySqlQueryView.text044') }}</span>
            <el-select v-model="form.datasourceType">
              <el-option
                v-for="item in datasourceOptions"
                :key="item"
                :label="item"
                :value="item"
              />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsQuerySqlQueryView.text045') }}</span>
            <el-select v-model="form.accelerationPreference">
              <el-option
                v-for="item in accelerationOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsQuerySqlQueryView.text046') }}</span>
            <el-select v-model="form.faultToleranceStrategy">
              <el-option
                v-for="item in toleranceOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </label>
        </div>

        <div class="editor-block">
          <SqlEditorField
            v-model="form.sqlText"
            :label="t('inline.viewsQuerySqlQueryView.text047')"
            :rows="14"
            :copy-label="t('inline.viewsQuerySqlQueryView.text048')"
            :format-label="t('inline.viewsQuerySqlQueryView.text049')"
            data-testid="query-flow-sql-editor"
          />
        </div>

        <div class="parameter-panel">
          <div class="parameter-panel__header">
            <div>
              <p class="section-kicker sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text050') }}</p>
              <h3 class="parameter-panel__title">{{ t('inline.viewsQuerySqlQueryView.text051') }}</h3>
            </div>
            <el-button text @click="addParameter">{{ t('inline.viewsQuerySqlQueryView.text052') }}</el-button>
          </div>

          <div class="parameter-table">
            <div class="parameter-table__head">
              <span>{{ t('inline.viewsQuerySqlQueryView.text053') }}</span>
              <span>{{ t('inline.viewsQuerySqlQueryView.text054') }}</span>
              <span>{{ t('inline.viewsQuerySqlQueryView.text055') }}</span>
            </div>
            <div
              v-for="row in parameterRows"
              :key="row.id"
              class="parameter-table__row"
            >
              <el-input v-model="row.key" />
              <el-input v-model="row.value" />
              <el-button text @click="removeParameter(row.id)">{{ t('inline.viewsQuerySqlQueryView.text056') }}</el-button>
            </div>
          </div>
        </div>

        <div class="submit-row">
          <el-button
            type="primary"
            :loading="running"
            data-testid="query-flow-submit"
            @click="runQuery('default')"
          >
            {{ t('inline.viewsQuerySqlQueryView.text057') }}
          </el-button>
          <el-button
            :loading="running"
            data-testid="query-flow-submit-recovery"
            @click="runQuery('recovery')"
          >
            {{ t('inline.viewsQuerySqlQueryView.text058') }}
          </el-button>
          <div class="submit-row__helpers">
            <el-button text @click="showBoundPreviewDrawer = true">{{ t('inline.viewsQuerySqlQueryView.text059') }}</el-button>
            <el-button text @click="showGovernanceDrawer = true">{{ t('inline.viewsQuerySqlQueryView.text060') }}</el-button>
            <el-button text data-testid="query-flow-open-deep-parse" @click="openDeepParseWorkbench">{{ t('inline.viewsQuerySqlQueryView.text086') }}</el-button>
          </div>
        </div>
      </section>

      <aside class="result-rail surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">governance summary</p>
            <h2 class="section-title">{{ t('inline.viewsQuerySqlQueryView.text061') }}</h2>
          </div>
        </div>

        <div class="summary-list">
          <div
            v-for="item in summaryRows"
            :key="item.label"
            class="summary-list__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>

        <div class="history-panel">
          <p class="section-kicker sqlforge-code-label">{{ t('inline.viewsQuerySqlQueryView.text062') }}</p>
          <p v-if="!executionHistory.length" class="muted-copy">
            {{ t('inline.viewsQuerySqlQueryView.text063') }}
          </p>
          <div v-else class="history-list">
            <div
              v-for="item in executionHistory"
              :key="item.id"
              class="history-list__item"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.status }} · {{ item.mode }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <section class="surface-card results-stage">
      <div class="panel-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">result tabs</p>
          <h2 class="section-title">{{ t('inline.viewsQuerySqlQueryView.text064') }}</h2>
        </div>
      </div>

      <el-tabs v-model="activeResultTab">
        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text065')" name="rows">
          <div v-if="previewRows.length" class="table-shell">
            <el-table :data="previewRows" border data-testid="query-result-table">
              <el-table-column
                v-for="column in resultColumns"
                :key="column"
                :prop="column"
                :label="column"
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
          <p v-else class="empty-copy">{{ t('inline.viewsQuerySqlQueryView.text066') }}</p>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text067')" name="summary">
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
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text068')" name="lightweight">
          <div class="detail-grid">
            <div
              v-for="item in lightweightAnalysisRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('sqlQuery.resultTabs.access')" name="context">
          <div class="detail-grid">
            <div
              v-for="item in accessRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text069')" name="routing">
          <div class="detail-grid">
            <div
              v-for="item in routingRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('sqlQuery.resultTabs.history')" name="history">
          <div class="detail-grid">
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

        <el-tab-pane :label="t('inline.viewsQuerySqlQueryView.text070')" name="recommendation">
          <div class="detail-grid">
            <div
              v-for="item in recommendationRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
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
      <!-- Bound SQL preview -->
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
  gap: var(--sqlforge-space-5);
}

.query-workbench__header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.95fr);
  gap: var(--sqlforge-space-5);
  align-items: stretch;
  padding: var(--sqlforge-space-5);
}

.query-hero-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sqlforge-space-3);
}

.mini-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-pill);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-meta);
}

.query-workbench__grid {
  display: grid;
  grid-template-columns: minmax(240px, 0.9fr) minmax(0, 1.6fr) minmax(260px, 0.95fr);
  gap: 20px;
}

.surface-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    var(--sqlforge-surface-2);
}

.query-rail,
.editor-rail,
.result-rail,
.results-stage {
  padding: 20px;
}

.panel-heading,
.parameter-panel__header,
.submit-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.section-kicker,
.field-label {
  margin: 0 0 6px;
  color: var(--sqlforge-text-muted);
}

.section-title,
.parameter-panel__title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.utility-actions,
.submit-row__helpers {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.inline-banner,
.field-block,
.editor-block,
.parameter-panel,
.summary-list__item,
.history-list__item,
.detail-grid__item,
.library-item,
.dialog-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 16px;
  background: rgba(20, 24, 31, 0.82);
}

.inline-banner {
  padding: 10px 12px;
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.field-block,
.editor-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.editor-block {
  margin-top: 16px;
}

.parameter-panel {
  margin-top: 16px;
  padding: 14px;
}

.parameter-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.parameter-table__head,
.parameter-table__row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 92px;
  gap: 10px;
  align-items: center;
}

.parameter-table__head {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.submit-row {
  margin-top: 18px;
  align-items: center;
}

.summary-list,
.history-list,
.dialog-list,
.drawer-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-list {
  margin-top: 16px;
}

.summary-list__item,
.history-list__item,
.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
}

.summary-list__item span,
.detail-grid__item span,
.muted-copy {
  color: var(--sqlforge-text-secondary);
}

.history-panel {
  margin-top: 18px;
}

.library-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  padding: 12px 14px;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.library-item span {
  color: var(--sqlforge-text-secondary);
}

.results-stage {
  min-height: 320px;
}

.table-shell {
  overflow: auto;
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.empty-copy {
  color: var(--sqlforge-text-secondary);
}

.dialog-card {
  padding: 14px;
}

.dialog-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--sqlforge-text-primary);
}

@media (max-width: 1280px) {
  .query-workbench__header,
  .query-workbench__grid {
    grid-template-columns: 1fr;
  }

  .query-hero-metrics,
  .field-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
