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
    id: 'engine-hetu',
    label: 'Hetu clusters',
    children: [
      {
        id: 'hetu-main',
        label: 'hetu_main',
        caption: 'Primary governed engine',
        datasourceType: 'HETU',
        subject: 'sales / governed execute'
      },
      {
        id: 'hetu-shadow',
        label: 'hetu_shadow',
        caption: 'Shadow validation lane',
        datasourceType: 'HETU',
        subject: 'route verification'
      }
    ]
  },
  {
    id: 'engine-hive',
    label: 'Hive datasets',
    children: [
      {
        id: 'hive-lakehouse',
        label: 'hive_lakehouse',
        caption: 'Cold-path warehouse lane',
        datasourceType: 'HIVE',
        subject: 'batch / fallback'
      }
    ]
  }
]

const form = reactive({
  tenantId: 'tenant-a',
  sqlText: '--report_code=RPT_SALES_DAILY\nSELECT * FROM orders WHERE query_date = :query_date LIMIT 100',
  datasourceType: 'HETU',
  accelerationPreference: 'PREFER_ACCELERATED',
  faultToleranceStrategy: 'FAIL_FAST'
})

const parameterRows = ref([
  { id: 1, key: 'query_date', value: '2026-04-26' },
  { id: 2, key: 'limit', value: '100' }
])

const running = ref(false)
const result = ref(null)
const errorMessage = ref('')
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const selectedScenario = ref('standard')
const selectedDatasourceId = ref('hetu-main')
const activeResultTab = ref('rows')
const nextParameterId = ref(3)

const isChinese = computed(() => locale.value === 'zh-CN')
const selectedDatasource = computed(() => {
  for (const group of datasourceTree) {
    for (const item of group.children || []) {
      if (item.id === selectedDatasourceId.value) {
        return item
      }
    }
  }
  return datasourceTree[0].children[0]
})
const previewRows = computed(() => result.value?.rows || [])
const resultColumns = computed(() => {
  const firstRow = previewRows.value[0]
  return firstRow ? Object.keys(firstRow) : []
})
const retryPath = computed(() => result.value?.retryPath || [])
const queuePendingDelta = computed(() => {
  if (!queueStatsBefore.value || !queueStatsAfter.value) {
    return 0
  }
  return queueStatsAfter.value.pending - queueStatsBefore.value.pending
})
const queueTotalDelta = computed(() => {
  if (!queueStatsBefore.value || !queueStatsAfter.value) {
    return 0
  }
  return queueStatsAfter.value.total - queueStatsBefore.value.total
})
const compensationDetected = computed(() => queuePendingDelta.value >= 1 || queueTotalDelta.value >= 1)
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
const parameterCount = computed(() => Object.keys(parameterSnapshot.value).length)
const datasourceOptions = computed(() => ['HETU', 'HIVE'])
const accelerationOptions = computed(() => [
  {
    value: 'NONE',
    label: isChinese.value ? '不偏好加速' : 'No acceleration preference'
  },
  {
    value: 'PREFER_ACCELERATED',
    label: isChinese.value ? '优先加速链路' : 'Prefer accelerated path'
  }
])
const toleranceOptions = computed(() => [
  {
    value: 'FAIL_FAST',
    label: isChinese.value ? '快速失败' : 'Fail fast'
  },
  {
    value: 'RETRY_THEN_FALLBACK',
    label: isChinese.value ? '重试后回退' : 'Retry then fallback'
  }
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
    {
      label: isChinese.value ? '数据源' : 'Datasource',
      value: selectedDatasource.value.label
    },
    {
      label: isChinese.value ? '执行模式' : 'Execution mode',
      value: metadata.executionMode || '-'
    },
    {
      label: isChinese.value ? '目标引擎' : 'Target engine',
      value: metadata.targetEngine || form.datasourceType
    },
    {
      label: isChinese.value ? '路由配置' : 'Route profile',
      value: metadata.routeProfile || '-'
    },
    {
      label: isChinese.value ? '缓存状态' : 'Cache governance',
      value: metadata.cacheGovernanceStatus || '-'
    },
    {
      label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint',
      value: result.value?.sqlFingerprint || '-'
    },
    {
      label: isChinese.value ? '耗时' : 'Elapsed',
      value: metadata.elapsedMs == null ? '-' : `${metadata.elapsedMs}ms`
    },
    {
      label: isChinese.value ? '参数数' : 'Params',
      value: String(parameterCount.value)
    }
  ]
})
const workbenchNote = computed(() =>
  isChinese.value
    ? '左侧聚焦数据源与执行策略，中间用于编辑 SQL 与参数，右侧持续展示治理摘要、执行结果和降级证据。'
    : 'Use the left rail for datasource context, the center rail for SQL and parameters, and the right rail for governance summary, result tabs, and degraded evidence.'
)

const resetEvidence = () => {
  result.value = null
  errorMessage.value = ''
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  activeResultTab.value = 'rows'
}

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

const runQuery = async scenario => {
  selectedScenario.value = scenario
  running.value = true
  resetEvidence()

  try {
    if (scenario === 'recovery') {
      form.faultToleranceStrategy = 'RETRY_THEN_FALLBACK'
      queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
        requestPrefix: 'frontend-query-governance-stats-before'
      })
    }

    const payload = {
      tenantId: form.tenantId,
      sqlText: boundSqlPreview.value,
      datasourceType: form.datasourceType,
      accelerationPreference: form.accelerationPreference,
      faultToleranceStrategy: form.faultToleranceStrategy
    }
    if (scenario === 'recovery') {
      payload.queryContext = {
        timeoutMs: 30
      }
    }

    result.value = await executeQuery(payload)

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
</script>

<template>
  <section class="query-workbench" data-testid="query-flow-page">
    <div class="query-workbench__hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">sql workbench</p>
        <h1 class="runtime-title">{{ t('sqlQuery.title') }}</h1>
        <p class="runtime-summary">{{ t('sqlQuery.summary') }}</p>
      </div>
      <p class="runtime-note">{{ workbenchNote }}</p>
    </div>

    <div class="query-workbench__grid">
      <aside class="query-rail surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">datasource tree</p>
            <h2 class="section-title">{{ isChinese ? '数据源与执行场景' : 'Datasource context' }}</h2>
          </div>
        </div>

        <el-tree
          class="datasource-tree"
          :data="datasourceTree"
          node-key="id"
          :expand-on-click-node="false"
          default-expand-all
          highlight-current
          :current-node-key="selectedDatasourceId"
          @current-change="syncDatasourceSelection"
        >
          <template #default="{ data }">
            <div class="tree-node" :class="{ 'tree-node--leaf': !data.children }">
              <div>
                <strong>{{ data.label }}</strong>
                <p v-if="data.caption" class="tree-node__caption">{{ data.caption }}</p>
              </div>
              <span v-if="data.datasourceType" class="tree-node__badge">{{ data.datasourceType }}</span>
            </div>
          </template>
        </el-tree>

        <div class="selection-card">
          <p class="selection-card__label">{{ isChinese ? '当前选择' : 'Selected lane' }}</p>
          <h3>{{ selectedDatasource.label }}</h3>
          <p>{{ selectedDatasource.subject }}</p>
        </div>

        <div class="scenario-stack">
          <button
            class="scenario-card"
            :class="{ 'scenario-card--active': selectedScenario === 'standard' }"
            type="button"
            @click="selectedScenario = 'standard'"
          >
            <strong>{{ isChinese ? '标准执行' : 'Standard execute' }}</strong>
            <span>{{ isChinese ? '聚焦正常结果与治理摘要' : 'Focus on the default result and governance summary.' }}</span>
          </button>
          <button
            class="scenario-card"
            :class="{ 'scenario-card--active': selectedScenario === 'recovery' }"
            type="button"
            @click="selectedScenario = 'recovery'"
          >
            <strong>{{ isChinese ? '降级恢复' : 'Degraded recovery' }}</strong>
            <span>{{ isChinese ? '同时观测 retry path 与 governance queue 补偿信号' : 'Inspect retry-path and governance queue compensation evidence.' }}</span>
          </button>
        </div>
      </aside>

      <main class="editor-rail">
        <article class="surface-card editor-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">sql editor</p>
              <h2 class="section-title">{{ isChinese ? 'SQL 与执行策略' : 'SQL and execution policy' }}</h2>
            </div>
          </div>

          <div class="form-grid">
            <label class="field-block">
              <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
              <el-input v-model="form.tenantId" />
            </label>

            <label class="field-block">
              <span class="field-label">{{ isChinese ? '目标引擎' : 'Target engine' }}</span>
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
              :rows="10"
            />
          </label>

          <div class="action-row action-row-wrap">
            <el-button
              type="primary"
              :loading="running && selectedScenario === 'standard'"
              data-testid="query-flow-submit"
              @click="runQuery('standard')"
            >
              {{ isChinese ? '执行查询' : 'Execute query' }}
            </el-button>
            <el-button
              :loading="running && selectedScenario === 'recovery'"
              data-testid="query-flow-submit-recovery"
              @click="runQuery('recovery')"
            >
              {{ isChinese ? '执行降级恢复' : 'Run degraded recovery' }}
            </el-button>
          </div>
        </article>

        <article class="surface-card editor-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">parameter inputs</p>
              <h2 class="section-title">{{ isChinese ? '参数输入与绑定预览' : 'Parameters and bound preview' }}</h2>
            </div>
            <el-button text @click="addParameter">
              {{ isChinese ? '新增参数' : 'Add param' }}
            </el-button>
          </div>

          <div class="parameter-list">
            <div
              v-for="item in parameterRows"
              :key="item.id"
              class="parameter-row"
            >
              <el-input v-model="item.key" :placeholder="isChinese ? '参数名' : 'Name'" />
              <el-input v-model="item.value" :placeholder="isChinese ? '参数值' : 'Value'" />
              <el-button text @click="removeParameter(item.id)">
                {{ isChinese ? '移除' : 'Remove' }}
              </el-button>
            </div>
          </div>

          <div class="bound-preview">
            <p class="bound-preview__label">{{ isChinese ? '绑定后 SQL 预览' : 'Bound SQL preview' }}</p>
            <pre class="result-json">{{ boundSqlPreview }}</pre>
          </div>
        </article>
      </main>

      <aside class="result-rail">
        <article class="surface-card summary-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">governance summary</p>
              <h2 class="section-title">{{ isChinese ? '治理摘要' : 'Governance summary' }}</h2>
            </div>
          </div>

          <div class="evidence-grid">
            <div
              v-for="item in governanceSummary"
              :key="item.label"
              class="evidence-item"
            >
              <span class="evidence-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div
            v-if="errorMessage"
            class="result-banner result-banner-danger"
            data-testid="query-flow-error"
          >
            {{ errorMessage }}
          </div>

          <div
            v-else-if="result"
            class="result-banner"
            :class="result.status === 'SUCCESS' ? 'result-banner-success' : 'result-banner-warning'"
          >
            <strong data-testid="query-flow-status">{{ result.status }}</strong>
            <span data-testid="query-flow-engine">{{ result.metadata?.targetEngine || '-' }}</span>
          </div>

          <p v-else class="empty-state">
            {{
              isChinese
                ? '执行查询后，这里会持续刷新 SQL 指纹、执行模式、缓存与路由摘要。'
                : 'Run the query to populate SQL fingerprint, execution mode, cache signals, and route evidence.'
            }}
          </p>
        </article>

        <article class="surface-card results-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">result tabs</p>
              <h2 class="section-title">{{ isChinese ? '结果与证据' : 'Results and evidence' }}</h2>
            </div>
          </div>

          <el-tabs v-model="activeResultTab">
            <el-tab-pane :label="isChinese ? '结果预览' : 'Preview rows'" name="rows">
              <p v-if="!previewRows.length" class="empty-state">
                {{ isChinese ? '暂无结果行。' : 'No result rows yet.' }}
              </p>
              <template v-else>
                <div class="mini-metrics">
                  <span><strong data-testid="query-flow-row-count">{{ previewRows.length }}</strong> {{ isChinese ? '行预览' : 'rows previewed' }}</span>
                  <span><strong data-testid="query-flow-degraded">{{ result?.degraded ? 'true' : 'false' }}</strong> {{ isChinese ? '降级' : 'degraded' }}</span>
                </div>
                <el-table :data="previewRows.slice(0, 20)" size="small">
                  <el-table-column
                    v-for="column in resultColumns"
                    :key="column"
                    :prop="column"
                    :label="column"
                    min-width="120"
                  />
                </el-table>
              </template>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '执行摘要' : 'Execution summary'" name="summary">
              <div class="evidence-grid">
                <div class="evidence-item">
                  <span class="evidence-label">{{ isChinese ? '重试步数' : 'Retry path size' }}</span>
                  <strong data-testid="query-flow-retry-path-size">{{ retryPath.length }}</strong>
                </div>
                <div class="evidence-item">
                  <span class="evidence-label">{{ isChinese ? '实现阶段' : 'Implementation stage' }}</span>
                  <strong>{{ result?.implementationStage || '-' }}</strong>
                </div>
                <div class="evidence-item">
                  <span class="evidence-label">{{ isChinese ? '降级原因' : 'Degrade reason' }}</span>
                  <strong data-testid="query-flow-degrade-reason">{{ result?.degradeReason || '-' }}</strong>
                </div>
                <div class="evidence-item">
                  <span class="evidence-label">{{ isChinese ? '契约阶段' : 'Contract stage' }}</span>
                  <strong>{{ result?.contractStage || '-' }}</strong>
                </div>
              </div>

              <div v-if="retryPath.length" class="trace-card" data-testid="query-flow-retry-path">
                <div
                  v-for="(step, index) in retryPath"
                  :key="`${step.engine}-${index}`"
                  class="trace-step"
                >
                  <strong>{{ step.engine }}</strong>
                  <span>{{ step.resultStatus }}</span>
                  <span>{{ step.elapsedMs }}ms</span>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '恢复证据' : 'Recovery evidence'" name="recovery">
              <p v-if="!queueStatsBefore || !queueStatsAfter" class="empty-state">
                {{
                  isChinese
                    ? '切到“执行降级恢复”后，这里会展示 governance queue 补偿前后对比。'
                    : 'Run the degraded recovery scenario to render before/after governance queue evidence.'
                }}
              </p>
              <template v-else>
                <div class="result-banner" :class="compensationDetected ? 'result-banner-success' : 'result-banner-danger'">
                  <strong data-testid="query-flow-compensation-status">
                    {{ compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED' }}
                  </strong>
                  <span>{{ isChinese ? 'governance queue pending' : 'governance queue pending' }}</span>
                </div>

                <div class="evidence-grid">
                  <div class="evidence-item">
                    <span class="evidence-label">{{ isChinese ? '补偿前 pending' : 'Pending before' }}</span>
                    <strong data-testid="query-flow-queue-pending-before">{{ queueStatsBefore.pending }}</strong>
                  </div>
                  <div class="evidence-item">
                    <span class="evidence-label">{{ isChinese ? '补偿后 pending' : 'Pending after' }}</span>
                    <strong data-testid="query-flow-queue-pending-after">{{ queueStatsAfter.pending }}</strong>
                  </div>
                  <div class="evidence-item">
                    <span class="evidence-label">{{ isChinese ? 'pending 增量' : 'Pending delta' }}</span>
                    <strong data-testid="query-flow-queue-pending-delta">{{ queuePendingDelta }}</strong>
                  </div>
                  <div class="evidence-item">
                    <span class="evidence-label">{{ isChinese ? 'total 增量' : 'Total delta' }}</span>
                    <strong data-testid="query-flow-queue-total-delta">{{ queueTotalDelta }}</strong>
                  </div>
                </div>
              </template>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '原始 JSON' : 'Raw JSON'" name="json">
              <pre class="result-json">{{ JSON.stringify(result, null, 2) }}</pre>
            </el-tab-pane>
          </el-tabs>
        </article>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.query-workbench {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 48%),
    var(--sqlforge-surface-2);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.18);
}

.query-workbench__hero,
.query-rail,
.editor-card,
.summary-card,
.results-card {
  padding: 24px;
}

.query-workbench__hero {
  display: grid;
  gap: 16px;
  align-items: start;
}

.query-workbench__grid {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(240px, 280px) minmax(0, 1fr) minmax(340px, 420px);
  align-items: start;
}

.query-rail,
.editor-rail,
.result-rail {
  display: grid;
  gap: 20px;
}

.runtime-eyebrow,
.section-kicker,
.evidence-label,
.field-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.runtime-title,
.section-title {
  margin: 8px 0 0;
  font-size: 28px;
  line-height: 1.1;
}

.runtime-summary,
.runtime-note,
.empty-state {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.section-heading,
.action-row-wrap,
.mini-metrics {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.datasource-tree {
  margin-top: 18px;
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 12px;
  width: 100%;
  padding: 6px 0;
}

.tree-node__caption {
  margin: 4px 0 0;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.tree-node__badge {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(90, 185, 255, 0.12);
  color: var(--sqlforge-accent-primary);
  font-size: 12px;
}

.selection-card,
.scenario-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--sqlforge-radius-md);
  padding: 16px;
  background: rgba(7, 13, 28, 0.38);
}

.selection-card {
  margin-top: 18px;
}

.selection-card__label {
  margin: 0 0 8px;
  color: var(--sqlforge-text-muted);
}

.selection-card h3,
.selection-card p {
  margin: 0;
}

.selection-card p {
  margin-top: 8px;
  color: var(--sqlforge-text-secondary);
}

.scenario-stack {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.scenario-card {
  display: grid;
  gap: 6px;
  text-align: left;
  color: var(--sqlforge-text-primary);
  cursor: pointer;
}

.scenario-card span {
  color: var(--sqlforge-text-secondary);
}

.scenario-card--active {
  border-color: rgba(90, 185, 255, 0.42);
  background: rgba(90, 185, 255, 0.12);
}

.form-grid,
.evidence-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block,
.evidence-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide,
.bound-preview {
  margin-top: 20px;
}

.parameter-list {
  display: grid;
  gap: 12px;
}

.parameter-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}

.bound-preview__label {
  margin: 0 0 10px;
  color: var(--sqlforge-text-muted);
}

.result-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--sqlforge-radius-md);
  border: 1px solid transparent;
  margin-top: 18px;
}

.result-banner-success,
.trace-card {
  border-color: rgba(62, 207, 142, 0.28);
  background: rgba(62, 207, 142, 0.1);
}

.result-banner-warning {
  border-color: rgba(214, 179, 48, 0.28);
  background: rgba(214, 179, 48, 0.12);
}

.result-banner-danger {
  border-color: rgba(232, 82, 82, 0.3);
  background: rgba(232, 82, 82, 0.12);
}

.trace-card {
  border: 1px solid transparent;
  border-radius: var(--sqlforge-radius-md);
  padding: 16px;
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.trace-step {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  color: var(--sqlforge-text-secondary);
}

.mini-metrics {
  margin-bottom: 14px;
  color: var(--sqlforge-text-secondary);
  flex-wrap: wrap;
}

.result-json {
  margin: 0;
  padding: 16px;
  border-radius: var(--sqlforge-radius-md);
  background: rgba(7, 13, 28, 0.62);
  color: var(--sqlforge-text-secondary);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .query-workbench__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .form-grid,
  .evidence-grid,
  .parameter-row,
  .trace-step {
    grid-template-columns: 1fr;
  }
}
</style>
