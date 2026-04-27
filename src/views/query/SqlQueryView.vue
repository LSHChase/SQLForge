<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  executeQuery,
  formatRuntimeError,
  getGovernanceMessageStats
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const datasourceTree = [
  {
    id: 'favorites',
    label: 'Favorites',
    children: [
      { id: 'fav-biz-view', label: 'BUSINESS_VIEW.order_daily_rollup', datasourceType: 'HETU' },
      { id: 'fav-db-view', label: 'DB_VIEW.vw_sales_summary', datasourceType: 'HETU' }
    ]
  },
  {
    id: 'hetu-clusters',
    label: 'Hetu Clusters',
    children: [
      { id: 'hetu-main', label: 'hetu_main.sales.orders', datasourceType: 'HETU' },
      { id: 'hetu-main-logic', label: 'hetu_main.logic.customer_360', datasourceType: 'HETU' },
      { id: 'hetu-shadow', label: 'hetu_shadow.audit.query_history', datasourceType: 'HETU' }
    ]
  },
  {
    id: 'hive-datasets',
    label: 'Hive Datasets',
    children: [
      { id: 'hive-lakehouse', label: 'hive_lakehouse.dw.fact_orders', datasourceType: 'HIVE' },
      { id: 'hive-views', label: 'hive_lakehouse.view.revenue_monthly', datasourceType: 'HIVE' }
    ]
  }
]

const sqlTemplates = [
  {
    key: 'report',
    label: '--report_code + biz_date',
    content: '--report_code=RPT_SALES_DAILY\n--stage=PROD\n--biz_date=2026-04-27\n--tenant_id=tenant-a\n--datasource=hetu_main\n--engine_hint=HETU\n--priority=high\n'
  },
  {
    key: 'explain',
    label: 'Explain template',
    content: '--report_code=RPT_EXPLAIN_SAMPLE\n--stage=PROD\n--tenant_id=tenant-a\nEXPLAIN SELECT * FROM orders WHERE query_date = :query_date\n'
  },
  {
    key: 'fallback',
    label: 'Recovery template',
    content: '--report_code=RPT_RECOVERY_CHECK\n--stage=PROD\n--tenant_id=tenant-a\n--datasource=hive_lakehouse\nSELECT count(1) FROM orders WHERE query_date = :query_date\n'
  }
]

const sqlLibrary = [
  {
    key: 'recent-1',
    type: 'recent',
    title: 'Recent · sales daily',
    summary: 'Revenue rollup with query_date binding',
    sqlText: "--report_code=RPT_SALES_DAILY\nSELECT order_id, revenue FROM orders WHERE query_date = :query_date LIMIT :limit"
  },
  {
    key: 'recent-2',
    type: 'recent',
    title: 'Recent · logic view',
    summary: 'Business view sample',
    sqlText: '--report_code=RPT_CUSTOMER_360\nSELECT * FROM customer_360 WHERE biz_date = :biz_date LIMIT 50'
  },
  {
    key: 'favorite-1',
    type: 'favorite',
    title: 'Favorite · benchmark candidate',
    summary: 'Candidate SQL for benchmark and explain',
    sqlText: '--report_code=RPT_BENCHMARK_SAMPLE\nSELECT region, sum(revenue) FROM orders GROUP BY region'
  }
]

const form = reactive({
  tenantId: 'tenant-a',
  sqlText:
    '--report_code=RPT_SALES_DAILY\n--stage=PROD\n--biz_date=2026-04-27\n--tenant_id=tenant-a\n--datasource=hetu_main\nSELECT * FROM orders WHERE query_date = :query_date LIMIT :limit',
  datasourceType: 'HETU',
  accelerationPreference: 'PREFER_ACCELERATED',
  faultToleranceStrategy: 'FAIL_FAST'
})

const parameterRows = ref([
  { id: 1, key: 'query_date', value: '2026-04-27' },
  { id: 2, key: 'limit', value: '100' },
  { id: 3, key: 'biz_date', value: '2026-04-27' }
])

const running = ref(false)
const result = ref(null)
const errorMessage = ref('')
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const selectedDatasourceId = ref('hetu-main')
const selectedExplorerTab = ref('objects')
const activeResultTab = ref('rows')
const showTemplateDialog = ref(false)
const showLibraryDialog = ref(false)
const showBoundPreviewDialog = ref(false)
const showGovernanceDrawer = ref(false)
const showExplainDialog = ref(false)
const nextParameterId = ref(4)
const executionHistory = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')
const previewRows = computed(() => result.value?.rows || [])
const resultColumns = computed(() => {
  const firstRow = previewRows.value[0]
  return firstRow ? Object.keys(firstRow) : []
})
const parameterSnapshot = computed(() => {
  const snapshot = {}
  for (const item of parameterRows.value) {
    const key = String(item.key || '').trim()
    if (key) {
      snapshot[key] = item.value
    }
  }
  return snapshot
})
const queryDateValue = computed(() => parameterSnapshot.value.query_date || parameterSnapshot.value.biz_date || '-')
const selectedDatasource = computed(() => {
  for (const group of datasourceTree) {
    for (const item of group.children || []) {
      if (item.id === selectedDatasourceId.value) {
        return item
      }
    }
  }
  return datasourceTree[1].children[0]
})
const datasourceOptions = computed(() => ['HETU', 'HIVE'])
const accelerationOptions = computed(() => [
  { value: 'NONE', label: isChinese.value ? '不偏好加速' : 'No acceleration preference' },
  { value: 'PREFER_ACCELERATED', label: isChinese.value ? '优先加速链路' : 'Prefer accelerated path' }
])
const toleranceOptions = computed(() => [
  { value: 'FAIL_FAST', label: isChinese.value ? '快速失败' : 'Fail fast' },
  { value: 'RETRY_THEN_FALLBACK', label: isChinese.value ? '重试后回退' : 'Retry then fallback' }
])
const boundSqlPreview = computed(() => {
  let preview = form.sqlText
  for (const [key, value] of Object.entries(parameterSnapshot.value)) {
    preview = preview.replaceAll(`:${key}`, `'${value}'`)
  }
  return preview
})
const governanceSummary = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    { label: isChinese.value ? '数据源' : 'Datasource', value: selectedDatasource.value.label },
    { label: isChinese.value ? '执行模式' : 'Execution mode', value: metadata.executionMode || '-' },
    { label: isChinese.value ? '目标引擎' : 'Target engine', value: metadata.targetEngine || form.datasourceType },
    { label: isChinese.value ? '路由配置' : 'Route profile', value: metadata.routeProfile || '-' },
    { label: isChinese.value ? '缓存状态' : 'Cache status', value: metadata.cacheGovernanceStatus || '-' },
    { label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint', value: result.value?.sqlFingerprint || '-' },
    { label: isChinese.value ? '耗时' : 'Elapsed', value: metadata.elapsedMs == null ? '-' : `${metadata.elapsedMs}ms` },
    { label: isChinese.value ? 'query_date' : 'query_date', value: queryDateValue.value }
  ]
})
const validationTips = computed(() => {
  const tips = []
  if (!String(form.sqlText || '').includes('--report_code=')) {
    tips.push(isChinese.value ? '缺少 --report_code 注释。' : 'Missing --report_code annotation.')
  }
  if (!String(form.sqlText || '').toUpperCase().includes('SELECT')) {
    tips.push(isChinese.value ? '当前示例更适合 SELECT/EXPLAIN 查询。' : 'The current workbench is optimized for SELECT or EXPLAIN flows.')
  }
  if (!parameterSnapshot.value.query_date) {
    tips.push(isChinese.value ? '建议补充 query_date 参数。' : 'Add a query_date binding for audited execution.')
  }
  return tips
})
const structureSummary = computed(() => {
  const riskTags = []
  if (String(form.sqlText).toUpperCase().includes('JOIN')) {
    riskTags.push('JOIN')
  }
  if (String(form.sqlText).toUpperCase().includes('GROUP BY')) {
    riskTags.push('AGGREGATION')
  }
  if (String(form.sqlText).toUpperCase().includes('OVER')) {
    riskTags.push('WINDOW')
  }
  return {
    syntaxStatus: validationTips.value.length ? 'REVIEW' : 'VALID',
    sqlType: String(form.sqlText).trim().toUpperCase().startsWith('EXPLAIN') ? 'EXPLAIN' : 'SELECT',
    complexityLevel: riskTags.length >= 2 ? 'COMPLEX' : 'MODERATE',
    queryDateSummary: queryDateValue.value,
    riskTags: riskTags.length ? riskTags : ['NONE']
  }
})
const explainSteps = computed(() => [
  {
    label: isChinese.value ? '输入规范化' : 'Input normalization',
    detail: isChinese.value
      ? '应用注释模板和绑定参数后生成 bound SQL。'
      : 'Generate bound SQL after applying annotations and bindings.'
  },
  {
    label: isChinese.value ? '路由判断' : 'Routing decision',
    detail: result.value?.metadata?.routeProfile || (isChinese.value ? '尚未执行，暂无 route profile。' : 'No route profile until execution runs.')
  },
  {
    label: isChinese.value ? '治理输出' : 'Governance output',
    detail: result.value?.metadata?.executionMode || (isChinese.value ? '执行后回填 execution mode。' : 'Execution mode is populated after execution.')
  }
])

const syncDatasourceSelection = datasource => {
  if (!datasource?.datasourceType) {
    return
  }
  selectedDatasourceId.value = datasource.id
  form.datasourceType = datasource.datasourceType
}

const resetEvidence = () => {
  result.value = null
  errorMessage.value = ''
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  activeResultTab.value = 'rows'
}

const addParameter = () => {
  parameterRows.value.push({
    id: nextParameterId.value,
    key: '',
    value: ''
  })
  nextParameterId.value += 1
}

const removeParameter = rowId => {
  if (parameterRows.value.length === 1) {
    parameterRows.value[0].key = ''
    parameterRows.value[0].value = ''
    return
  }
  parameterRows.value = parameterRows.value.filter(item => item.id !== rowId)
}

const applyTemplate = template => {
  form.sqlText = `${template.content}${boundSqlPreview.value.includes('SELECT') ? '' : 'SELECT * FROM orders LIMIT 100'}`
  showTemplateDialog.value = false
}

const loadLibrarySql = entry => {
  form.sqlText = entry.sqlText
  showLibraryDialog.value = false
}

const formatSql = () => {
  form.sqlText = String(form.sqlText || '')
    .split('\n')
    .map(line => line.trimEnd())
    .join('\n')
    .trim()
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
      accelerationPreference: form.accelerationPreference,
      faultToleranceStrategy: form.faultToleranceStrategy,
      queryContext:
        scenario === 'recovery'
          ? {
              timeoutMs: 30
            }
          : undefined
    })

    executionHistory.value = [
      {
        id: `${Date.now()}`,
        title: result.value?.sqlFingerprint || selectedDatasource.value.label,
        status: result.value?.status || 'UNKNOWN',
        mode: result.value?.metadata?.executionMode || '-'
      },
      ...executionHistory.value
    ].slice(0, 6)

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

const formatJson = value => JSON.stringify(value, null, 2)
</script>

<template>
  <section class="query-workbench" data-testid="query-flow-page">
    <header class="query-workbench__hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">sql workbench</p>
        <h1 class="runtime-title">{{ t('sqlQuery.title') }}</h1>
        <p class="runtime-summary">{{ t('sqlQuery.summary') }}</p>
      </div>
      <div class="hero-actions">
        <el-button class="hero-button" @click="showTemplateDialog = true">
          {{ isChinese ? '注释模板' : 'Annotation template' }}
        </el-button>
        <el-button class="hero-button" @click="showLibraryDialog = true">
          {{ isChinese ? '最近 / 收藏 SQL' : 'Recent / Favorite SQL' }}
        </el-button>
        <el-button class="hero-button" @click="showGovernanceDrawer = true">
          {{ isChinese ? '治理抽屉' : 'Governance drawer' }}
        </el-button>
      </div>
    </header>

    <div class="query-workbench__grid">
      <aside class="query-rail surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">datasource tree</p>
            <h2 class="section-title">{{ isChinese ? '数据源 / 对象' : 'Datasources and objects' }}</h2>
          </div>
        </div>

        <el-tree
          class="datasource-tree"
          :data="datasourceTree"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
          highlight-current
          :current-node-key="selectedDatasourceId"
          @current-change="syncDatasourceSelection"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span>{{ data.label }}</span>
              <span v-if="data.datasourceType" class="tree-node__badge">{{ data.datasourceType }}</span>
            </div>
          </template>
        </el-tree>

        <el-tabs v-model="selectedExplorerTab" class="rail-tabs">
          <el-tab-pane :label="isChinese ? '最近' : 'Recent'" name="recent">
            <button
              v-for="entry in sqlLibrary.filter(item => item.type === 'recent')"
              :key="entry.key"
              type="button"
              class="rail-list-item"
              @click="loadLibrarySql(entry)"
            >
              <strong>{{ entry.title }}</strong>
              <span>{{ entry.summary }}</span>
            </button>
          </el-tab-pane>
          <el-tab-pane :label="isChinese ? '收藏' : 'Favorites'" name="favorites">
            <button
              v-for="entry in sqlLibrary.filter(item => item.type === 'favorite')"
              :key="entry.key"
              type="button"
              class="rail-list-item"
              @click="loadLibrarySql(entry)"
            >
              <strong>{{ entry.title }}</strong>
              <span>{{ entry.summary }}</span>
            </button>
          </el-tab-pane>
        </el-tabs>
      </aside>

      <main class="editor-rail">
        <article class="surface-card editor-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">sql editor</p>
              <h2 class="section-title">{{ isChinese ? '查询输入与执行' : 'Query input and execution' }}</h2>
            </div>
            <div class="toolbar-actions">
              <el-button text @click="formatSql">{{ isChinese ? '格式化' : 'Format' }}</el-button>
              <el-button text @click="showBoundPreviewDialog = true">Bound SQL preview</el-button>
              <el-button text @click="showExplainDialog = true">Explain</el-button>
            </div>
          </div>

          <div class="editor-form-grid">
            <label class="field-block">
              <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
              <el-input v-model="form.tenantId" />
            </label>
            <label class="field-block">
              <span class="field-label">{{ isChinese ? '引擎' : 'Engine' }}</span>
              <el-select v-model="form.datasourceType">
                <el-option
                  v-for="option in datasourceOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </label>
            <label class="field-block">
              <span class="field-label">{{ isChinese ? '加速偏好' : 'Acceleration preference' }}</span>
              <el-select v-model="form.accelerationPreference">
                <el-option
                  v-for="option in accelerationOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>
            <label class="field-block">
              <span class="field-label">{{ isChinese ? '容错策略' : 'Fault tolerance' }}</span>
              <el-select v-model="form.faultToleranceStrategy">
                <el-option
                  v-for="option in toleranceOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </label>
          </div>

          <label class="field-block field-block-wide">
            <span class="field-label">SQL</span>
            <el-input
              v-model="form.sqlText"
              type="textarea"
              :rows="14"
            />
          </label>

          <div class="editor-actions">
            <el-button
              type="primary"
              :loading="running"
              data-testid="query-flow-submit"
              @click="runQuery('standard')"
            >
              {{ isChinese ? '执行查询' : 'Execute query' }}
            </el-button>
            <el-button
              :loading="running"
              data-testid="query-flow-submit-recovery"
              @click="runQuery('recovery')"
            >
              {{ isChinese ? '执行恢复路径' : 'Run recovery path' }}
            </el-button>
          </div>

          <div v-if="validationTips.length" class="tip-strip">
            <span
              v-for="tip in validationTips"
              :key="tip"
              class="tip-pill"
            >
              {{ tip }}
            </span>
          </div>
        </article>

        <article class="surface-card editor-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">parameter inputs</p>
              <h2 class="section-title">{{ isChinese ? '参数输入' : 'Parameter input' }}</h2>
            </div>
            <el-button text @click="addParameter">{{ isChinese ? '新增参数' : 'Add parameter' }}</el-button>
          </div>

          <div class="parameter-table">
            <div
              v-for="item in parameterRows"
              :key="item.id"
              class="parameter-row"
            >
              <el-input v-model="item.key" :placeholder="isChinese ? '参数名' : 'Name'" />
              <el-input v-model="item.value" :placeholder="isChinese ? '参数值' : 'Value'" />
              <el-button text @click="removeParameter(item.id)">{{ isChinese ? '移除' : 'Remove' }}</el-button>
            </div>
          </div>
        </article>

        <article class="surface-card results-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">result tabs</p>
              <h2 class="section-title">{{ isChinese ? '结果与治理输出' : 'Results and governance output' }}</h2>
            </div>
          </div>

          <el-tabs v-model="activeResultTab">
            <el-tab-pane :label="isChinese ? '执行结果' : 'Execution results'" name="rows">
              <div
                v-if="errorMessage"
                class="result-banner result-banner-danger"
                data-testid="query-flow-error"
              >
                {{ errorMessage }}
              </div>
              <p v-else-if="!previewRows.length" class="empty-state">
                {{ isChinese ? '执行后在这里查看结果集。' : 'Run the query to inspect returned rows here.' }}
              </p>
              <template v-else>
                <div class="result-inline-meta">
                  <span><strong data-testid="query-flow-row-count">{{ previewRows.length }}</strong> rows</span>
                  <span><strong data-testid="query-flow-status">{{ result?.status || '-' }}</strong></span>
                  <span><strong data-testid="query-flow-engine">{{ result?.metadata?.targetEngine || '-' }}</strong></span>
                </div>
                <el-table :data="previewRows.slice(0, 20)" size="small">
                  <el-table-column
                    v-for="column in resultColumns"
                    :key="column"
                    :prop="column"
                    :label="column"
                    min-width="140"
                  />
                </el-table>
              </template>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '执行摘要' : 'Execution summary'" name="summary">
              <div class="summary-grid">
                <article
                  v-for="item in governanceSummary"
                  :key="item.label"
                  class="summary-tile"
                >
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>
              <div class="summary-grid summary-grid-compact">
                <article class="summary-tile">
                  <span>{{ isChinese ? '补偿前 pending' : 'Pending before retry' }}</span>
                  <strong>{{ queueStatsBefore?.pending ?? '-' }}</strong>
                </article>
                <article class="summary-tile">
                  <span>{{ isChinese ? '补偿后 pending' : 'Pending after retry' }}</span>
                  <strong>{{ queueStatsAfter?.pending ?? '-' }}</strong>
                </article>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '结构解析' : 'Structure parse'" name="structure">
              <div class="summary-grid">
                <article class="summary-tile">
                  <span>syntaxStatus</span>
                  <strong>{{ structureSummary.syntaxStatus }}</strong>
                </article>
                <article class="summary-tile">
                  <span>sqlType</span>
                  <strong>{{ structureSummary.sqlType }}</strong>
                </article>
                <article class="summary-tile">
                  <span>complexityLevel</span>
                  <strong>{{ structureSummary.complexityLevel }}</strong>
                </article>
                <article class="summary-tile">
                  <span>queryDateSummary</span>
                  <strong>{{ structureSummary.queryDateSummary }}</strong>
                </article>
              </div>
              <div class="tag-row">
                <span
                  v-for="tag in structureSummary.riskTags"
                  :key="tag"
                  class="tip-pill"
                >
                  {{ tag }}
                </span>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '数据访问解析' : 'Access parse'" name="access">
              <p class="empty-state">
                {{
                  isChinese
                    ? '当前执行页先展示执行路径、目标引擎和补偿信号；更深的数据访问解析建议从“解析工作台”继续下钻。'
                    : 'This page exposes execution mode, target engine, and compensation signals first. Use the dedicated parsing workspace for deeper access-parse drill-through.'
                }}
              </p>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '路由详情' : 'Routing detail'" name="route">
              <pre class="result-json">{{ formatJson(result?.metadata || {}) }}</pre>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '历史关联' : 'History links'" name="history">
              <div class="history-list">
                <article
                  v-for="entry in executionHistory"
                  :key="entry.id"
                  class="history-item"
                >
                  <strong>{{ entry.title }}</strong>
                  <span>{{ entry.status }} · {{ entry.mode }}</span>
                </article>
              </div>
            </el-tab-pane>
          </el-tabs>
        </article>
      </main>

      <aside class="result-rail">
        <article class="surface-card summary-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">governance summary</p>
              <h2 class="section-title">{{ isChinese ? '运行摘要' : 'Runtime summary' }}</h2>
            </div>
          </div>

          <div class="summary-grid summary-grid-compact">
            <article
              v-for="item in governanceSummary.slice(0, 4)"
              :key="item.label"
              class="summary-tile"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <div class="shortcut-list">
            <button class="rail-list-item" type="button" @click="showBoundPreviewDialog = true">
              <strong>Bound SQL preview</strong>
              <span>{{ isChinese ? '查看绑定后 SQL' : 'Inspect bound SQL text' }}</span>
            </button>
            <button class="rail-list-item" type="button" @click="showExplainDialog = true">
              <strong>Explain</strong>
              <span>{{ isChinese ? '查看执行步骤' : 'Inspect execution steps' }}</span>
            </button>
            <button class="rail-list-item" type="button" @click="showGovernanceDrawer = true">
              <strong>{{ isChinese ? '治理抽屉' : 'Governance drawer' }}</strong>
              <span>{{ isChinese ? '查看治理证据与原始元数据' : 'Open detailed governance evidence and raw metadata.' }}</span>
            </button>
          </div>
        </article>
      </aside>
    </div>

    <el-dialog
      v-model="showTemplateDialog"
      :title="isChinese ? '注释模板' : 'Annotation templates'"
      width="760px"
    >
      <div class="dialog-list">
        <button
          v-for="item in sqlTemplates"
          :key="item.key"
          type="button"
          class="dialog-card"
          @click="applyTemplate(item)"
        >
          <strong>{{ item.label }}</strong>
          <pre class="result-json result-json-compact">{{ item.content }}</pre>
        </button>
      </div>
    </el-dialog>

    <el-dialog
      v-model="showLibraryDialog"
      :title="isChinese ? '最近与收藏 SQL' : 'Recent and favorite SQL'"
      width="760px"
    >
      <div class="dialog-list">
        <button
          v-for="entry in sqlLibrary"
          :key="entry.key"
          type="button"
          class="dialog-card"
          @click="loadLibrarySql(entry)"
        >
          <strong>{{ entry.title }}</strong>
          <span>{{ entry.summary }}</span>
          <pre class="result-json result-json-compact">{{ entry.sqlText }}</pre>
        </button>
      </div>
    </el-dialog>

    <el-dialog
      v-model="showBoundPreviewDialog"
      title="Bound SQL preview"
      width="760px"
    >
      <pre class="result-json">{{ boundSqlPreview }}</pre>
    </el-dialog>

    <el-dialog
      v-model="showExplainDialog"
      title="Explain"
      width="680px"
    >
      <div class="explain-steps">
        <article
          v-for="step in explainSteps"
          :key="step.label"
          class="summary-tile"
        >
          <span>{{ step.label }}</span>
          <strong>{{ step.detail }}</strong>
        </article>
      </div>
    </el-dialog>

    <el-drawer
      v-model="showGovernanceDrawer"
      :title="isChinese ? '治理证据抽屉' : 'Governance evidence drawer'"
      size="42%"
    >
      <div class="drawer-section">
        <div class="summary-grid">
          <article
            v-for="item in governanceSummary"
            :key="item.label"
            class="summary-tile"
          >
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </div>
      <div class="drawer-section">
        <p class="section-kicker sqlforge-code-label">{{ isChinese ? '原始元数据' : 'Raw metadata' }}</p>
        <pre class="result-json">{{ formatJson(result || {}) }}</pre>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.query-workbench {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card,
.summary-tile,
.history-item,
.rail-list-item,
.dialog-card {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.surface-card {
  border-radius: 16px;
  padding: 20px;
}

.query-workbench__hero,
.query-workbench__grid,
.editor-form-grid,
.summary-grid,
.parameter-table {
  display: grid;
  gap: 16px;
}

.query-workbench__hero,
.section-heading,
.hero-actions,
.toolbar-actions,
.editor-actions,
.result-inline-meta,
.tag-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.query-workbench__hero,
.section-heading {
  justify-content: space-between;
}

.query-workbench__grid {
  grid-template-columns: 280px minmax(0, 1fr) 280px;
  align-items: start;
}

.editor-rail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.runtime-eyebrow,
.section-kicker,
.field-label,
.summary-tile span {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.runtime-title,
.section-title {
  margin: 8px 0 0;
  font-weight: 400;
  color: var(--sqlforge-text-primary);
}

.runtime-title {
  font-size: 38px;
  line-height: 1.05;
}

.runtime-summary,
.empty-state,
.rail-list-item span,
.history-item span {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-button,
.tip-pill {
  border-radius: 999px;
}

.hero-button {
  border-color: var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.tree-node__badge,
.tip-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.rail-tabs {
  margin-top: 12px;
}

.rail-list-item,
.dialog-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  padding: 14px;
  border-radius: 14px;
  text-align: left;
  cursor: pointer;
}

.dialog-list,
.shortcut-list,
.history-list,
.explain-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.editor-form-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.parameter-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
}

.tip-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.summary-grid-compact {
  grid-template-columns: 1fr;
}

.summary-tile {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-radius: 14px;
}

.summary-tile strong,
.history-item strong {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.result-inline-meta {
  margin-bottom: 12px;
  color: var(--sqlforge-text-secondary);
}

.result-banner {
  padding: 12px 14px;
  border-radius: 14px;
}

.result-banner-danger {
  border: 1px solid rgba(212, 96, 96, 0.35);
  background: rgba(120, 28, 28, 0.18);
  color: #ffd6d6;
}

.result-json {
  margin: 0;
  padding: 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 14px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-json-compact {
  padding: 10px;
  font-size: 12px;
}

.drawer-section + .drawer-section {
  margin-top: 20px;
}

@media (max-width: 1280px) {
  .query-workbench__grid {
    grid-template-columns: 1fr;
  }

  .editor-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
