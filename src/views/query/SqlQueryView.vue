<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  executeQuery,
  formatRuntimeError,
  getGovernanceMessageStats
} from '../../services/runtimeGateApi'

const { locale } = useI18n()

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

const selectedDatasourceId = ref('hetu-main')
const activeExplorerTab = ref('objects')
const activeResultTab = ref('rows')
const running = ref(false)
const result = ref(null)
const errorMessage = ref('')
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const showTemplateDialog = ref(false)
const showLibraryDialog = ref(false)
const showBoundPreviewDrawer = ref(false)
const showGovernanceDrawer = ref(false)
const showExplainDialog = ref(false)
const executionHistory = ref([])
const nextParameterId = ref(4)

const isChinese = computed(() => locale.value === 'zh-CN')
const previewRows = computed(() => result.value?.rows || [])
const resultColumns = computed(() => {
  const firstRow = previewRows.value[0]
  return firstRow ? Object.keys(firstRow) : []
})
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
const recentLibraryEntries = computed(() => sqlLibrary.filter(item => item.type === 'recent'))
const favoriteLibraryEntries = computed(() => sqlLibrary.filter(item => item.type === 'favorite'))
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
const boundSqlPreview = computed(() => {
  let preview = form.sqlText
  for (const [key, value] of Object.entries(parameterSnapshot.value)) {
    preview = preview.replaceAll(`:${key}`, `'${value}'`)
  }
  return preview
})
const validationTips = computed(() => {
  const tips = []
  if (!String(form.sqlText || '').includes('--report_code=')) {
    tips.push(isChinese.value ? '缺少 --report_code 注释。' : 'Missing --report_code annotation.')
  }
  if (!String(form.sqlText || '').toUpperCase().includes('SELECT') && !String(form.sqlText || '').toUpperCase().includes('EXPLAIN')) {
    tips.push(isChinese.value ? '当前工作台更适合 SELECT / EXPLAIN。' : 'The current workbench is optimized for SELECT or EXPLAIN.')
  }
  if (!parameterSnapshot.value.query_date) {
    tips.push(isChinese.value ? '建议补充 query_date 参数。' : 'Add a query_date binding for audited execution.')
  }
  return tips
})
const summaryRows = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    { label: isChinese.value ? '数据源对象' : 'Datasource object', value: selectedDatasource.value.label },
    { label: isChinese.value ? '执行模式' : 'Execution mode', value: metadata.executionMode || '-' },
    { label: isChinese.value ? '目标引擎' : 'Target engine', value: metadata.targetEngine || form.datasourceType },
    { label: isChinese.value ? '路由配置' : 'Route profile', value: metadata.routeProfile || '-' },
    { label: isChinese.value ? '缓存状态' : 'Cache status', value: metadata.cacheGovernanceStatus || '-' },
    { label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint', value: result.value?.sqlFingerprint || '-' },
    { label: isChinese.value ? '耗时' : 'Elapsed', value: metadata.elapsedMs == null ? '-' : `${metadata.elapsedMs}ms` }
  ]
})
const structureRows = computed(() => {
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
    { label: isChinese.value ? '语法状态' : 'Syntax status', value: validationTips.value.length ? 'REVIEW' : 'VALID' },
    { label: isChinese.value ? 'SQL 类型' : 'SQL type', value: sqlText.trim().startsWith('EXPLAIN') ? 'EXPLAIN' : 'SELECT' },
    { label: isChinese.value ? '复杂度' : 'Complexity', value: riskTags.length >= 2 ? 'COMPLEX' : 'MODERATE' },
    { label: isChinese.value ? '风险标签' : 'Risk tags', value: riskTags.length ? riskTags.join(', ') : 'NONE' }
  ]
})
const routingRows = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    { label: 'routeProfile', value: metadata.routeProfile || '-' },
    { label: 'attemptedModes', value: listText(metadata.attemptedModes) },
    { label: isChinese.value ? '回退策略' : 'Fallback strategy', value: form.faultToleranceStrategy },
    { label: isChinese.value ? '加速偏好' : 'Acceleration preference', value: form.accelerationPreference }
  ]
})
const recommendationRows = computed(() => {
  const metadata = result.value?.metadata || {}
  return [
    {
      label: isChinese.value ? '推荐动作' : 'Recommended action',
      value: metadata.cacheGovernanceStatus === 'HIT' ? (isChinese.value ? '继续复用缓存链路' : 'Keep the cached route') : (isChinese.value ? '优先验证推荐中心结果' : 'Validate recommendation-center output')
    },
    {
      label: isChinese.value ? '下一步' : 'Next step',
      value: isChinese.value ? '如需长文本说明，打开 explain 或治理抽屉。' : 'Use the explain dialog or governance drawer for long-form evidence.'
    }
  ]
})
const explainSteps = computed(() => [
  {
    label: isChinese.value ? '输入规范化' : 'Input normalization',
    detail: isChinese.value ? '应用注释和参数绑定后生成 bound SQL。' : 'Generate bound SQL after annotations and parameter bindings are applied.'
  },
  {
    label: isChinese.value ? '路由判断' : 'Routing decision',
    detail: result.value?.metadata?.routeProfile || (isChinese.value ? '执行后会回填 route profile。' : 'The route profile is populated after execution.')
  },
  {
    label: isChinese.value ? '治理写回' : 'Governance write-back',
    detail: result.value?.metadata?.executionMode || (isChinese.value ? '执行完成后回填 execution mode。' : 'The execution mode is populated after execution completes.')
  }
])

const syncDatasourceSelection = datasource => {
  if (!datasource?.datasourceType) {
    return
  }
  selectedDatasourceId.value = datasource.id
  form.datasourceType = datasource.datasourceType
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
  form.sqlText = `${template.content}SELECT * FROM orders WHERE query_date = :query_date LIMIT :limit`
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

const resetEvidence = () => {
  result.value = null
  errorMessage.value = ''
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  activeResultTab.value = 'rows'
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
    <div class="query-workbench__grid">
      <aside class="query-rail surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ isChinese ? '对象与收藏' : 'Objects and favorites' }}</p>
            <h2 class="section-title">{{ isChinese ? '对象树、收藏与最近 SQL' : 'Object tree, favorites, and recent SQL' }}</h2>
          </div>
          <div class="utility-actions">
            <el-button text @click="showTemplateDialog = true">{{ isChinese ? '模板' : 'Templates' }}</el-button>
            <el-button text @click="showLibraryDialog = true">{{ isChinese ? 'SQL 库' : 'SQL library' }}</el-button>
          </div>
        </div>

        <el-tabs v-model="activeExplorerTab">
          <el-tab-pane :label="isChinese ? '对象' : 'Objects'" name="objects">
            <el-tree
              :data="datasourceTree"
              node-key="id"
              default-expand-all
              @node-click="syncDatasourceSelection"
            />
          </el-tab-pane>
          <el-tab-pane :label="isChinese ? '收藏' : 'Favorites'" name="favorites">
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
          <el-tab-pane :label="isChinese ? '最近 SQL' : 'Recent SQL'" name="recent">
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
            <p class="section-kicker sqlforge-code-label">{{ isChinese ? '查询工作台' : 'Query workbench' }}</p>
            <h2 class="section-title">{{ isChinese ? 'SQL 编辑、参数绑定与执行动作' : 'SQL editing, bindings, and execution' }}</h2>
          </div>
          <div class="utility-actions">
            <el-button text @click="formatSql">{{ isChinese ? '格式化' : 'Format SQL' }}</el-button>
            <el-button text @click="showExplainDialog = true">{{ isChinese ? 'Explain 说明' : 'Explain guide' }}</el-button>
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
            <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
            <el-input v-model="form.tenantId" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '目标引擎' : 'Target engine' }}</span>
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
            <span class="field-label">{{ isChinese ? '加速偏好' : 'Acceleration preference' }}</span>
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
            <span class="field-label">{{ isChinese ? '容错策略' : 'Fault tolerance' }}</span>
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

        <label class="editor-block">
          <span class="field-label">{{ isChinese ? 'SQL 编辑器' : 'SQL editor' }}</span>
          <el-input
            v-model="form.sqlText"
            type="textarea"
            :rows="14"
          />
        </label>

        <div class="parameter-panel">
          <div class="parameter-panel__header">
            <div>
              <p class="section-kicker sqlforge-code-label">{{ isChinese ? '参数绑定' : 'Parameter bindings' }}</p>
              <h3 class="parameter-panel__title">{{ isChinese ? '紧凑表格式输入' : 'Compact tabular bindings' }}</h3>
            </div>
            <el-button text @click="addParameter">{{ isChinese ? '新增参数' : 'Add parameter' }}</el-button>
          </div>

          <div class="parameter-table">
            <div class="parameter-table__head">
              <span>{{ isChinese ? '参数名' : 'Key' }}</span>
              <span>{{ isChinese ? '参数值' : 'Value' }}</span>
              <span>{{ isChinese ? '操作' : 'Action' }}</span>
            </div>
            <div
              v-for="row in parameterRows"
              :key="row.id"
              class="parameter-table__row"
            >
              <el-input v-model="row.key" />
              <el-input v-model="row.value" />
              <el-button text @click="removeParameter(row.id)">{{ isChinese ? '删除' : 'Remove' }}</el-button>
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
            {{ isChinese ? '执行 SQL' : 'Run SQL' }}
          </el-button>
          <el-button
            :loading="running"
            data-testid="query-flow-submit-recovery"
            @click="runQuery('recovery')"
          >
            {{ isChinese ? '恢复执行' : 'Recovery run' }}
          </el-button>
          <div class="submit-row__helpers">
            <el-button text @click="showBoundPreviewDrawer = true">{{ isChinese ? '查看 Bound SQL' : 'View bound SQL' }}</el-button>
            <el-button text @click="showGovernanceDrawer = true">{{ isChinese ? '治理摘要' : 'Governance summary' }}</el-button>
          </div>
        </div>
      </section>

      <aside class="result-rail surface-card">
        <div class="panel-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">governance summary</p>
            <h2 class="section-title">{{ isChinese ? '当前执行摘要' : 'Current execution summary' }}</h2>
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
          <p class="section-kicker sqlforge-code-label">{{ isChinese ? '最近执行' : 'Recent runs' }}</p>
          <p v-if="!executionHistory.length" class="muted-copy">
            {{ isChinese ? '执行后会在这里保留最近 6 条记录。' : 'The last six runs are listed here after execution.' }}
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
          <h2 class="section-title">{{ isChinese ? '结果、摘要、结构解析、路由与推荐' : 'Results, summary, structure, routing, and recommendation' }}</h2>
        </div>
      </div>

      <el-tabs v-model="activeResultTab">
        <el-tab-pane :label="isChinese ? '结果' : 'Rows'" name="rows">
          <div v-if="previewRows.length" class="table-shell">
            <el-table :data="previewRows" border>
              <el-table-column
                v-for="column in resultColumns"
                :key="column"
                :prop="column"
                :label="column"
                min-width="150"
              />
            </el-table>
          </div>
          <p v-else class="empty-copy">{{ isChinese ? '默认先展示结果表，执行后可直接查看返回行。' : 'The result table is the default landing state once execution completes.' }}</p>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '执行摘要' : 'Execution summary'" name="summary">
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

        <el-tab-pane :label="isChinese ? '结构解析' : 'Structure parse'" name="structure">
          <div class="detail-grid">
            <div
              v-for="item in structureRows"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '路由' : 'Routing'" name="routing">
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

        <el-tab-pane :label="isChinese ? '推荐' : 'Recommendation'" name="recommendation">
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

    <el-dialog v-model="showTemplateDialog" :title="isChinese ? '注释模板' : 'Annotation templates'" width="720px">
      <div class="dialog-list">
        <article
          v-for="template in sqlTemplates"
          :key="template.key"
          class="dialog-card"
        >
          <div class="dialog-card__header">
            <strong>{{ template.label }}</strong>
            <el-button text @click="applyTemplate(template)">{{ isChinese ? '应用' : 'Apply' }}</el-button>
          </div>
          <pre class="code-block">{{ template.content }}</pre>
        </article>
      </div>
    </el-dialog>

    <el-dialog v-model="showLibraryDialog" :title="isChinese ? 'SQL Library' : 'SQL library'" width="720px">
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
            <el-button text @click="loadLibrarySql(entry)">{{ isChinese ? '加载' : 'Load' }}</el-button>
          </div>
          <pre class="code-block">{{ entry.sqlText }}</pre>
        </article>
      </div>
    </el-dialog>

    <el-dialog v-model="showExplainDialog" :title="isChinese ? 'Explain 说明' : 'Explain guide'" width="640px">
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

    <el-drawer v-model="showBoundPreviewDrawer" :title="isChinese ? 'Bound SQL preview' : 'Bound SQL preview'" size="48%">
      <pre class="code-block">{{ boundSqlPreview }}</pre>
    </el-drawer>

    <el-drawer v-model="showGovernanceDrawer" :title="isChinese ? '治理摘要' : 'Governance summary'" size="42%">
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
          <strong>{{ isChinese ? '补偿队列快照' : 'Compensation queue snapshots' }}</strong>
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
  gap: 20px;
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
  .query-workbench__grid {
    grid-template-columns: 1fr;
  }

  .field-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
