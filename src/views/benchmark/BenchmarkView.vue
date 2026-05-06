<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getBenchmarkReport,
  getBenchmarkTaskStatus,
  getGovernanceMessageStats,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  submitBenchmarkTask,
  waitForBenchmarkTask
} from '../../services/runtimeGateApi'
import SqlEditorField from '../common/SqlEditorField.vue'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  taskType: 'COMPARISON',
  sqlText: 'SELECT * FROM orders'
})

const running = ref(false)
const lastScenario = ref('success')
const selectedTemplateId = ref('comparison-dual-engine')
const submitResult = ref(null)
const taskStatus = ref(null)
const compensationStatus = ref(null)
const report = ref(null)
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const errorMessage = ref('')
const sessionTasks = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')

const templateCatalog = computed(() => [
  {
    templateId: 'baseline-snapshot',
    taskType: 'BASELINE',
    title: isChinese.value ? '基线快照模板' : 'Baseline snapshot template',
    summary: isChinese.value
      ? '沉淀稳定基线，供后续回归任务比对。'
      : 'Capture a stable baseline for later regression tasks.',
    datasetSummary: isChinese.value ? '单 SQL / 只读 / 影子环境必需' : 'Single SQL / readonly / shadow required',
    executionSummary: isChinese.value ? 'repo-side task preset，不代表独立模板管理接口已上线。' : 'A repo-side task preset rather than a dedicated template-management API.',
    sqlText: 'SELECT * FROM orders',
    taskContext: {
      priority: 'HIGH',
      targetEngines: ['HETU'],
      concurrency: 8,
      durationSeconds: 180,
      rampUpSeconds: 20,
      readonlyRequired: true,
      shadowEnvironmentMode: 'REQUIRED'
    }
  },
  {
    templateId: 'comparison-dual-engine',
    taskType: 'COMPARISON',
    title: isChinese.value ? '双引擎对比模板' : 'Dual-engine comparison template',
    summary: isChinese.value
      ? '比较 HETU/HIVE 指标，给路由和推荐中心提供基线证据。'
      : 'Compare HETU and HIVE metrics to support routing and recommendation evidence.',
    datasetSummary: isChinese.value ? '跨引擎对比 / 只读 / 中等并发' : 'Cross-engine comparison / readonly / medium concurrency',
    executionSummary: isChinese.value ? '当前页面用 task preset 组织模板，不宣称后端已有模板 CRUD。' : 'Templates are organized as task presets on this page without claiming backend template CRUD exists.',
    sqlText: 'SELECT * FROM orders',
    taskContext: {
      priority: 'HIGH',
      targetEngines: ['HETU', 'HIVE'],
      concurrency: 16,
      durationSeconds: 300,
      rampUpSeconds: 30,
      readonlyRequired: true,
      shadowEnvironmentMode: 'REQUIRED'
    }
  },
  {
    templateId: 'regression-guard',
    taskType: 'REGRESSION_GUARD',
    title: isChinese.value ? '回归守卫模板' : 'Regression guard template',
    summary: isChinese.value
      ? '关注 p99 延迟与扫描量，输出 release gate 风险。'
      : 'Focus on p99 latency and scan volume to produce a release-gate verdict.',
    datasetSummary: isChinese.value ? '回归验证 / 阈值门禁 / 只读' : 'Regression validation / threshold gate / readonly',
    executionSummary: isChinese.value ? '用于回归模式展示，不把“测试集管理”误写成已落库对象。' : 'Used to render the regression mode without pretending test-set management is already persisted.',
    sqlText: 'SELECT * FROM orders WHERE ds = CURRENT_DATE',
    taskContext: {
      priority: 'HIGH',
      targetEngines: ['HETU'],
      concurrency: 10,
      durationSeconds: 240,
      rampUpSeconds: 20,
      readonlyRequired: true,
      shadowEnvironmentMode: 'REQUIRED'
    }
  }
])

const testSetCatalog = computed(() => [
  {
    testSetId: 'set-route-comparison',
    title: isChinese.value ? '路由对比集' : 'Routing comparison set',
    summary: isChinese.value
      ? '以 comparison template 驱动跨引擎样本，供 route governance 和 recommendation 复用。'
      : 'A comparison-driven sample set reused by route-governance and recommendation pages.',
    mode: 'COMPARISON',
    implementationStage: 'SESSION_CATALOG_ONLY'
  },
  {
    testSetId: 'set-baseline-capture',
    title: isChinese.value ? '基线沉淀集' : 'Baseline capture set',
    summary: isChinese.value
      ? '在 repo-side 以预置任务参数承载，不宣称独立 test-set API 已存在。'
      : 'Carried as repo-side task presets instead of claiming a dedicated test-set API already exists.',
    mode: 'BASELINE',
    implementationStage: 'SESSION_CATALOG_ONLY'
  },
  {
    testSetId: 'set-regression-gate',
    title: isChinese.value ? '回归门禁集' : 'Regression gate set',
    summary: isChinese.value
      ? '聚焦阈值结论、recommendation 建议与回归阻断。'
      : 'Focuses on threshold verdicts, recommendations, and regression blocking.',
    mode: 'REGRESSION_GUARD',
    implementationStage: 'SESSION_CATALOG_ONLY'
  }
])

const activeTemplate = computed(() =>
  templateCatalog.value.find(item => item.templateId === selectedTemplateId.value) || templateCatalog.value[0]
)

const activeTaskCards = computed(() => {
  if (!taskStatus.value) {
    return []
  }
  return [
    field('taskId', 'Task ID', taskStatus.value.taskId),
    field('taskType', isChinese.value ? '任务类型' : 'Task type', taskStatus.value.taskType),
    field('status', 'Status', taskStatus.value.status),
    field('currentPhase', isChinese.value ? '当前阶段' : 'Current phase', taskStatus.value.currentPhase),
    field('priority', 'Priority', taskStatus.value.priority),
    field('progressPercent', isChinese.value ? '进度' : 'Progress', taskStatus.value.progressPercent),
    field('queueMode', isChinese.value ? '队列模式' : 'Queue mode', taskStatus.value.queueMode),
    field('shadowEnvironmentMode', isChinese.value ? '影子环境' : 'Shadow environment', taskStatus.value.shadowEnvironmentMode)
  ].filter(item => displayValue(item.value) !== '-')
})

const reportEngineResults = computed(() => report.value?.engineResults || [])
const reportThresholdAssessments = computed(() => report.value?.thresholdAssessments || [])
const reportRecommendations = computed(() => report.value?.recommendations || [])
const reportTrendCharts = computed(() => report.value?.trendCharts || [])

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

const resetEvidence = () => {
  submitResult.value = null
  taskStatus.value = null
  compensationStatus.value = null
  report.value = null
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  errorMessage.value = ''
}

const chooseTemplate = templateId => {
  const template = templateCatalog.value.find(item => item.templateId === templateId)
  if (!template) {
    return
  }
  selectedTemplateId.value = template.templateId
  form.taskType = template.taskType
  form.sqlText = template.sqlText
}

const buildTaskContext = () => ({ ...(activeTemplate.value?.taskContext || {}) })

const rememberSessionTask = payload => {
  const taskId = payload.taskId || payload.id
  if (!taskId) {
    return
  }
  const next = {
    taskId,
    taskType: payload.taskType || form.taskType,
    status: payload.status || 'SUBMITTED',
    currentPhase: payload.currentPhase || '',
    reportId: payload.reportId || '',
    verdict: payload.verdict || '',
    scenario: payload.scenario || lastScenario.value
  }
  const index = sessionTasks.value.findIndex(item => item.taskId === taskId)
  if (index >= 0) {
    sessionTasks.value.splice(index, 1, {
      ...sessionTasks.value[index],
      ...next
    })
    return
  }
  sessionTasks.value.unshift(next)
}

const runSuccessFlow = async () => {
  running.value = true
  lastScenario.value = 'success'
  resetEvidence()

  try {
    submitResult.value = await submitBenchmarkTask({
      ...form,
      taskContext: buildTaskContext()
    })
    rememberSessionTask({
      ...submitResult.value,
      taskType: form.taskType,
      scenario: 'success'
    })
    taskStatus.value = await waitForBenchmarkTask(submitResult.value.taskId, form.tenantId)
    rememberSessionTask({
      ...taskStatus.value,
      scenario: 'success'
    })

    if (taskStatus.value.reportId) {
      report.value = await getBenchmarkReport(taskStatus.value.reportId, form.tenantId)
      rememberSessionTask({
        taskId: taskStatus.value.taskId,
        taskType: taskStatus.value.taskType,
        status: taskStatus.value.status,
        currentPhase: taskStatus.value.currentPhase,
        reportId: taskStatus.value.reportId,
        verdict: report.value?.verdict,
        scenario: 'success'
      })
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const runFailureCompensationFlow = async () => {
  running.value = true
  lastScenario.value = 'failure'
  resetEvidence()

  try {
    queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-benchmark-governance-stats-before'
    })
    submitResult.value = await submitBenchmarkTask({
      ...form,
      sqlText: `${form.sqlText} /*FAIL_BENCHMARK*/`,
      taskContext: buildTaskContext()
    })
    rememberSessionTask({
      ...submitResult.value,
      taskType: form.taskType,
      scenario: 'failure'
    })
    taskStatus.value = await waitForBenchmarkTask(submitResult.value.taskId, form.tenantId)
    rememberSessionTask({
      ...taskStatus.value,
      scenario: 'failure'
    })
    compensationStatus.value = await getBenchmarkTaskStatus(submitResult.value.taskId, form.tenantId, {
      requestPrefix: 'frontend-benchmark-compensation-status',
      tracePrefix: GOVERNANCE_COMPENSATION_TRACE_PREFIX
    })
    queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-benchmark-governance-stats-after'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const field = (key, label, value) => ({ key, label, value })

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

onMounted(() => {
  chooseTemplate(selectedTemplateId.value)
})
</script>

<template>
  <section class="benchmark-page runtime-page" data-testid="benchmark-page">
    <header class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">benchmark center</p>
        <h1 class="runtime-title">{{ t('benchmark.title') }}</h1>
        <p class="runtime-summary">{{ t('benchmark.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '当前 repo-side 已有真实 benchmark task/report 接口，但“模板详情”和“测试集”仍以前端 catalog 组织，不宣称后端已有独立模板/测试集 CRUD。'
            : 'The repo already exposes live benchmark task and report APIs, while template detail and test sets are still organized as frontend catalogs rather than dedicated backend CRUD surfaces.'
        }}
      </p>
    </header>

    <div class="catalog-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">template catalog</p>
            <h2 class="section-title">{{ isChinese ? '模板详情' : 'Template detail' }}</h2>
          </div>
        </div>
        <div class="catalog-list">
          <button
            v-for="item in templateCatalog"
            :key="item.templateId"
            type="button"
            class="catalog-card"
            :class="{ 'catalog-card-active': selectedTemplateId === item.templateId }"
            data-testid="benchmark-template-card"
            @click="chooseTemplate(item.templateId)"
          >
            <div class="catalog-card-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.taskType }}</p>
                <h3>{{ item.title }}</h3>
              </div>
              <span class="catalog-pill">{{ item.templateId }}</span>
            </div>
            <p class="result-copy">{{ item.summary }}</p>
            <p class="request-note">{{ item.datasetSummary }}</p>
            <p class="catalog-footnote">{{ item.executionSummary }}</p>
          </button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">test-set catalog</p>
            <h2 class="section-title">{{ isChinese ? '测试集' : 'Test sets' }}</h2>
          </div>
        </div>
        <div class="catalog-list">
          <article
            v-for="item in testSetCatalog"
            :key="item.testSetId"
            class="catalog-card catalog-card-static"
            data-testid="benchmark-test-set-card"
          >
            <div class="catalog-card-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.mode }}</p>
                <h3>{{ item.title }}</h3>
              </div>
              <span class="catalog-pill">{{ item.implementationStage }}</span>
            </div>
            <p class="result-copy">{{ item.summary }}</p>
          </article>
        </div>
      </article>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">benchmark task</p>
            <h2 class="section-title">{{ isChinese ? '压测任务' : 'Benchmark task' }}</h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
            <el-input v-model="form.tenantId" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '任务类型' : 'Task type' }}</span>
            <el-input v-model="form.taskType" />
          </label>

          <div class="field-block field-block-wide">
            <SqlEditorField
              v-model="form.sqlText"
              label="SQL"
              :rows="6"
              :copy-label="isChinese ? '复制' : 'Copy'"
              :format-label="isChinese ? '格式化' : 'Format'"
              data-testid="benchmark-sql-input"
            />
          </div>
        </div>

        <p class="request-note">
          {{
            isChinese
              ? '成功链路使用当前选中模板的 taskContext；失败链路会在 SQL 上附加 `FAIL_BENCHMARK`，用于检查 governance 补偿证据。'
              : 'The success path uses the taskContext of the selected template, while the failure path appends `FAIL_BENCHMARK` to verify governance compensation evidence.'
          }}
        </p>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="running && lastScenario === 'success'"
            data-testid="benchmark-flow-submit"
            @click="runSuccessFlow"
          >
            {{ isChinese ? '执行当前模板' : 'Run selected template' }}
          </el-button>
          <el-button
            :loading="running && lastScenario === 'failure'"
            data-testid="benchmark-flow-submit-failure"
            @click="runFailureCompensationFlow"
          >
            {{ isChinese ? '执行失败恢复 + 补偿' : 'Run failure recovery + compensation' }}
          </el-button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">session tasks</p>
            <h2 class="section-title">{{ isChinese ? '会话任务列表' : 'Session task list' }}</h2>
          </div>
        </div>

        <p v-if="!sessionTasks.length" class="empty-state">
          {{
            isChinese
              ? '当前页面没有全局任务列表接口，因此这里保留本次会话中发起过的 benchmark 任务。'
              : 'There is no global benchmark-task list API yet, so this panel keeps the tasks launched in the current session.'
          }}
        </p>

        <div v-else class="catalog-list">
          <article
            v-for="item in sessionTasks"
            :key="item.taskId"
            class="catalog-card catalog-card-static"
            data-testid="benchmark-session-task"
          >
            <div class="catalog-card-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.taskType }}</p>
                <h3>{{ item.taskId }}</h3>
              </div>
              <span class="catalog-pill">{{ item.status }}</span>
            </div>
            <p class="result-copy">{{ item.currentPhase || '-' }}</p>
            <div class="catalog-meta-row">
              <span>{{ isChinese ? '场景' : 'Scenario' }}: {{ item.scenario }}</span>
              <span>{{ isChinese ? '报告' : 'Report' }}: {{ item.reportId || '-' }}</span>
              <span>{{ isChinese ? '裁决' : 'Verdict' }}: {{ item.verdict || '-' }}</span>
            </div>
          </article>
        </div>
      </article>
    </div>

    <div class="runtime-grid runtime-grid-results">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">runtime evidence</p>
            <h2 class="section-title">{{ isChinese ? '任务与补偿结果' : 'Task and compensation evidence' }}</h2>
          </div>
        </div>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="benchmark-flow-error"
        >
          {{ errorMessage }}
        </div>

        <template v-if="submitResult">
          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '任务 ID' : 'Task ID' }}</span>
              <strong>{{ submitResult.taskId }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '初始状态' : 'Initial status' }}</span>
              <strong>{{ submitResult.status }}</strong>
            </div>
          </div>
        </template>

        <template v-if="taskStatus">
          <div
            class="result-banner"
            :class="taskStatus.status === 'SUCCEEDED' ? 'result-banner-success' : 'result-banner-warning'"
          >
            <strong data-testid="benchmark-flow-status">{{ taskStatus.status }}</strong>
            <span data-testid="benchmark-flow-report-id">{{ taskStatus.reportId || '-' }}</span>
          </div>

          <div class="evidence-grid">
            <div v-for="item in activeTaskCards" :key="item.key" class="evidence-item">
              <span class="evidence-label">{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>

          <div v-if="taskStatus.error" class="trace-card">
            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '失败码' : 'Failure code' }}</span>
                <strong data-testid="benchmark-flow-failure-code">{{ taskStatus.error.code }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '可重试' : 'Retryable' }}</span>
                <strong>{{ taskStatus.error.retryable ? 'true' : 'false' }}</strong>
              </div>
            </div>
            <p class="result-copy">{{ taskStatus.error.message }}</p>
          </div>
        </template>

        <template v-if="compensationStatus">
          <div class="compensation-card">
            <div class="result-banner" :class="compensationDetected ? 'result-banner-success' : 'result-banner-danger'">
              <strong data-testid="benchmark-flow-compensation-indicator">
                {{ compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED' }}
              </strong>
              <span data-testid="benchmark-flow-compensation-status">{{ compensationStatus.status }}</span>
            </div>

            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '补偿前 pending' : 'Pending before' }}</span>
                <strong data-testid="benchmark-flow-queue-pending-before">{{ queueStatsBefore?.pending ?? 0 }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '补偿后 pending' : 'Pending after' }}</span>
                <strong data-testid="benchmark-flow-queue-pending-after">{{ queueStatsAfter?.pending ?? 0 }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? 'pending 增量' : 'Pending delta' }}</span>
                <strong data-testid="benchmark-flow-queue-pending-delta">{{ queuePendingDelta }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? 'total 增量' : 'Total delta' }}</span>
                <strong data-testid="benchmark-flow-queue-total-delta">{{ queueTotalDelta }}</strong>
              </div>
            </div>
          </div>
        </template>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">benchmark report</p>
            <h2 class="section-title">{{ isChinese ? '报告对比与回归结果' : 'Report comparison and regression results' }}</h2>
          </div>
        </div>

        <p v-if="!report" class="empty-state">
          {{
            isChinese
              ? '执行成功后会在这里显示实际 report 返回值，包括 comparison 指标、threshold verdict、trend chart 和 benchmark recommendation。'
              : 'After a successful run, this panel shows the live report payload, including comparison metrics, threshold verdicts, trend charts, and benchmark recommendations.'
          }}
        </p>

        <template v-else>
          <div class="report-card" data-testid="benchmark-flow-report">
            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '裁决' : 'Verdict' }}</span>
                <strong>{{ report.verdict }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '任务类型' : 'Task type' }}</span>
                <strong>{{ report.taskType }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '格式' : 'Format' }}</span>
                <strong>{{ report.requestedFormat }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '原始数据路径' : 'Raw-data path' }}</span>
                <strong>{{ report.rawDataDownloadPath || '-' }}</strong>
              </div>
            </div>
            <p class="result-copy">
              {{
                isChinese
                  ? `报告 ${report.reportId} 由真实接口返回，可用格式 ${report.availableFormats.join(', ')}。`
                  : `Report ${report.reportId} was returned by the live API with formats ${report.availableFormats.join(', ')}.`
              }}
            </p>
          </div>

          <div class="report-section" data-testid="benchmark-report-compare">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">engine comparison</p>
                <h3 class="section-title section-title-small">{{ isChinese ? '对比指标' : 'Comparison metrics' }}</h3>
              </div>
            </div>
            <div class="catalog-list">
              <article v-for="item in reportEngineResults" :key="item.engine" class="catalog-card catalog-card-static">
                <div class="catalog-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.engine }}</p>
                    <h3>{{ item.verdict }}</h3>
                  </div>
                  <span class="catalog-pill">{{ item.actualQps }} QPS</span>
                </div>
                <div class="catalog-meta-row">
                  <span>P50 {{ item.p50LatencyMs }}ms</span>
                  <span>P95 {{ item.p95LatencyMs }}ms</span>
                  <span>P99 {{ item.p99LatencyMs }}ms</span>
                  <span>{{ isChinese ? '扫描字节' : 'Scanned bytes' }} {{ item.scannedDataBytes }}</span>
                </div>
                <p class="result-copy">{{ item.notes || '-' }}</p>
              </article>
            </div>
          </div>

          <div class="report-section" data-testid="benchmark-regression-results">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">regression results</p>
                <h3 class="section-title section-title-small">{{ isChinese ? '阈值与回归判断' : 'Threshold and regression verdicts' }}</h3>
              </div>
            </div>
            <div class="catalog-list">
              <article
                v-for="item in reportThresholdAssessments"
                :key="`${item.metric}-${item.verdict}`"
                class="catalog-card catalog-card-static"
              >
                <div class="catalog-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.metric }}</p>
                    <h3>{{ item.verdict }}</h3>
                  </div>
                  <span class="catalog-pill">{{ item.actualValue }} / {{ item.targetValue }}</span>
                </div>
                <p class="result-copy">{{ item.summary }}</p>
              </article>
            </div>
          </div>

          <div class="report-section">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">trend & recommendation</p>
                <h3 class="section-title section-title-small">{{ isChinese ? '趋势与后续建议' : 'Trend charts and follow-up recommendations' }}</h3>
              </div>
            </div>
            <div class="catalog-list">
              <article v-for="item in reportTrendCharts" :key="item.title" class="catalog-card catalog-card-static">
                <div class="catalog-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.chartType }}</p>
                    <h3>{{ item.title }}</h3>
                  </div>
                  <span class="catalog-pill">{{ item.series?.length || 0 }} series</span>
                </div>
                <p class="result-copy">{{ item.xAxisLabel }} / {{ item.yAxisLabel }}</p>
              </article>
              <article v-for="item in reportRecommendations" :key="item.title" class="catalog-card catalog-card-static">
                <div class="catalog-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.category }}</p>
                    <h3>{{ item.title }}</h3>
                  </div>
                  <span class="catalog-pill">{{ item.riskLevel }}</span>
                </div>
                <p class="result-copy">{{ item.summary }}</p>
                <p class="catalog-footnote">{{ item.expectedBenefit }}</p>
              </article>
            </div>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 16px;
  background: var(--sqlforge-surface-2);
}

.runtime-hero,
.catalog-grid > article,
.runtime-grid > article {
  padding: 24px;
}

.runtime-hero {
  display: grid;
  gap: 16px;
}

.catalog-grid,
.runtime-grid,
.form-grid,
.evidence-grid {
  display: grid;
  gap: 24px;
}

.catalog-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.runtime-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.runtime-grid-results {
  align-items: start;
}

.runtime-eyebrow,
.section-kicker,
.evidence-label,
.field-label,
.catalog-footnote {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.runtime-title,
.section-title,
.catalog-card-header h3 {
  margin: 8px 0 0;
}

.section-title {
  font-size: 26px;
  line-height: 1.1;
}

.section-title-small {
  font-size: 18px;
}

.runtime-summary,
.runtime-note,
.empty-state,
.request-note,
.result-copy,
.catalog-meta-row {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.form-grid,
.evidence-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block,
.evidence-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.catalog-list,
.report-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.catalog-card,
.report-card,
.compensation-card,
.trace-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 14px;
  padding: 16px;
  background: var(--sqlforge-surface-2);
}

.catalog-card {
  text-align: left;
}

.catalog-card-header,
.section-heading,
.result-banner {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.catalog-card-active {
  border-color: var(--sqlforge-color-brand-border);
}

.catalog-card-static {
  cursor: default;
}

.catalog-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.catalog-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
}

.action-row-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.result-banner {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid transparent;
  margin: 18px 0;
}

.result-banner-success,
.report-card,
.compensation-card,
.trace-card {
  border-color: var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.08);
}

.result-banner-warning {
  border-color: rgba(214, 179, 48, 0.28);
  background: rgba(214, 179, 48, 0.12);
}

.result-banner-danger {
  border-color: rgba(232, 82, 82, 0.3);
  background: rgba(232, 82, 82, 0.12);
}

@media (max-width: 1080px) {
  .catalog-grid,
  .runtime-grid,
  .form-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
