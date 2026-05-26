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
import { resolveRuntimeTenantId } from '../../config/tenantDefaults.mjs'
import SectionHeader from '../common/SectionHeader.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import ToolbarShell from '../common/ToolbarShell.vue'

const { t } = useI18n()

const form = reactive({
  tenantId: resolveRuntimeTenantId(),
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
const activeTab = ref('templates')


// Static contract tokens: template catalog, test-set catalog, Comparison metrics, regression results.

const templateCatalog = computed(() => [
  {
    templateId: 'baseline-snapshot',
    taskType: 'BASELINE',
    title: t('inline.viewsBenchmarkBenchmarkView.text001'),
    summary: t('inline.viewsBenchmarkBenchmarkView.text002'),
    datasetSummary: t('inline.viewsBenchmarkBenchmarkView.text003'),
    executionSummary: t('inline.viewsBenchmarkBenchmarkView.text004'),
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
    title: t('inline.viewsBenchmarkBenchmarkView.text005'),
    summary: t('inline.viewsBenchmarkBenchmarkView.text006'),
    datasetSummary: t('inline.viewsBenchmarkBenchmarkView.text007'),
    executionSummary: t('inline.viewsBenchmarkBenchmarkView.text008'),
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
    title: t('inline.viewsBenchmarkBenchmarkView.text009'),
    summary: t('inline.viewsBenchmarkBenchmarkView.text010'),
    datasetSummary: t('inline.viewsBenchmarkBenchmarkView.text011'),
    executionSummary: t('inline.viewsBenchmarkBenchmarkView.text012'),
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
    title: t('inline.viewsBenchmarkBenchmarkView.text013'),
    summary: t('inline.viewsBenchmarkBenchmarkView.text014'),
    mode: 'COMPARISON',
    implementationStage: 'SESSION_CATALOG_ONLY'
  },
  {
    testSetId: 'set-baseline-capture',
    title: t('inline.viewsBenchmarkBenchmarkView.text015'),
    summary: t('inline.viewsBenchmarkBenchmarkView.text016'),
    mode: 'BASELINE',
    implementationStage: 'SESSION_CATALOG_ONLY'
  },
  {
    testSetId: 'set-regression-gate',
    title: t('inline.viewsBenchmarkBenchmarkView.text017'),
    summary: t('inline.viewsBenchmarkBenchmarkView.text018'),
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
    field('taskType', t('inline.viewsBenchmarkBenchmarkView.text019'), taskStatus.value.taskType),
    field('status', 'Status', taskStatus.value.status),
    field('currentPhase', t('inline.viewsBenchmarkBenchmarkView.text020'), taskStatus.value.currentPhase),
    field('priority', 'Priority', taskStatus.value.priority),
    field('progressPercent', t('inline.viewsBenchmarkBenchmarkView.text021'), taskStatus.value.progressPercent),
    field('queueMode', t('inline.viewsBenchmarkBenchmarkView.text022'), taskStatus.value.queueMode),
    field('shadowEnvironmentMode', t('inline.viewsBenchmarkBenchmarkView.text023'), taskStatus.value.shadowEnvironmentMode)
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
  <section class="benchmark-page" data-testid="benchmark-page">
    <SectionHeader
      :eyebrow="t('benchmark.eyebrow')"
      :title="t('benchmark.title')"
      :summary="t('benchmark.boundarySummary')"
      :level="1"
      size="compact"
    />

    <ToolbarShell
      :eyebrow="t('benchmark.input.eyebrow')"
      :title="t('benchmark.input.title')"
      :summary="t('benchmark.input.summary')"
      density="compact"
    >
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.tenant') }}</span>
          <el-input v-model="form.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.taskType') }}</span>
          <el-input v-model="form.taskType" />
        </label>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="form.sqlText"
            :label="t('benchmark.fields.sql')"
            :rows="5"
            :copy-label="t('common.actions.copy')"
            :format-label="t('common.actions.format')"
            data-testid="benchmark-sql-input"
          />
        </div>
      </div>
    </ToolbarShell>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="benchmark-flow-error"
    >
      {{ errorMessage }}
    </div>

    <section class="tab-stage">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('benchmark.tabs.templates')" name="templates">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('benchmark.templates.eyebrow')"
              :title="t('benchmark.templates.title')"
              :summary="t('benchmark.templates.summary')"
              size="compact"
            />
            <div class="row-list">
              <button
                v-for="item in templateCatalog"
                :key="item.templateId"
                type="button"
                class="select-row"
                :class="{ 'select-row-active': selectedTemplateId === item.templateId }"
                data-testid="benchmark-template-card"
                @click="chooseTemplate(item.templateId)"
              >
                <div class="row-heading">
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
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('benchmark.tabs.testSets')" name="testSets">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('benchmark.testSets.eyebrow')"
              :title="t('benchmark.testSets.title')"
              :summary="t('benchmark.testSets.summary')"
              size="compact"
            />
            <div class="row-list">
              <article
                v-for="item in testSetCatalog"
                :key="item.testSetId"
                class="evidence-row"
                data-testid="benchmark-test-set-card"
              >
                <div class="row-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.mode }}</p>
                    <h3>{{ item.title }}</h3>
                  </div>
                  <span class="catalog-pill">{{ item.implementationStage }}</span>
                </div>
                <p class="result-copy">{{ item.summary }}</p>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('benchmark.tabs.taskFlow')" name="taskFlow">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('benchmark.taskFlow.eyebrow')"
              :title="t('benchmark.taskFlow.title')"
              :summary="t('benchmark.taskFlow.summary')"
              size="compact"
            />
            <div class="action-row-wrap">
              <el-button
                type="primary"
                :loading="running && lastScenario === 'success'"
                data-testid="benchmark-flow-submit"
                @click="runSuccessFlow"
              >
                {{ t('benchmark.actions.runTemplate') }}
              </el-button>
            </div>

            <template v-if="submitResult">
              <dl class="evidence-grid">
                <div class="evidence-item">
                  <dt>{{ t('common.fields.taskId') }}</dt>
                  <dd>{{ submitResult.taskId }}</dd>
                </div>
                <div class="evidence-item">
                  <dt>{{ t('benchmark.fields.initialStatus') }}</dt>
                  <dd>{{ submitResult.status }}</dd>
                </div>
              </dl>
            </template>

            <template v-if="taskStatus">
              <div
                class="result-banner"
                :class="taskStatus.status === 'SUCCEEDED' ? 'result-banner-success' : 'result-banner-warning'"
              >
                <strong data-testid="benchmark-flow-status">{{ taskStatus.status }}</strong>
                <span data-testid="benchmark-flow-report-id">{{ taskStatus.reportId || '-' }}</span>
              </div>

              <dl class="evidence-grid">
                <div v-for="item in activeTaskCards" :key="item.key" class="evidence-item">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ displayValue(item.value) }}</dd>
                </div>
              </dl>

              <div v-if="taskStatus.error" class="trace-panel">
                <dl class="evidence-grid">
                  <div class="evidence-item">
                    <dt>{{ t('benchmark.fields.failureCode') }}</dt>
                    <dd data-testid="benchmark-flow-failure-code">{{ taskStatus.error.code }}</dd>
                  </div>
                  <div class="evidence-item">
                    <dt>{{ t('benchmark.fields.retryable') }}</dt>
                    <dd>{{ taskStatus.error.retryable ? 'true' : 'false' }}</dd>
                  </div>
                </dl>
                <p class="result-copy">{{ taskStatus.error.message }}</p>
              </div>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('benchmark.tabs.compensation')" name="compensation">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('benchmark.compensation.eyebrow')"
              :title="t('benchmark.compensation.title')"
              :summary="t('benchmark.compensation.summary')"
              size="compact"
            />
            <div class="action-row-wrap">
              <el-button
                :loading="running && lastScenario === 'failure'"
                data-testid="benchmark-flow-submit-failure"
                @click="runFailureCompensationFlow"
              >
                {{ t('benchmark.actions.runCompensation') }}
              </el-button>
            </div>

            <template v-if="compensationStatus">
              <div class="result-banner" :class="compensationDetected ? 'result-banner-success' : 'result-banner-danger'">
                <strong data-testid="benchmark-flow-compensation-indicator">
                  {{ compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED' }}
                </strong>
                <span data-testid="benchmark-flow-compensation-status">{{ compensationStatus.status }}</span>
              </div>

              <dl class="evidence-grid">
                <div class="evidence-item">
                  <dt>{{ t('benchmark.fields.pendingBefore') }}</dt>
                  <dd data-testid="benchmark-flow-queue-pending-before">{{ queueStatsBefore?.pending ?? 0 }}</dd>
                </div>
                <div class="evidence-item">
                  <dt>{{ t('benchmark.fields.pendingAfter') }}</dt>
                  <dd data-testid="benchmark-flow-queue-pending-after">{{ queueStatsAfter?.pending ?? 0 }}</dd>
                </div>
                <div class="evidence-item">
                  <dt>{{ t('benchmark.fields.pendingDelta') }}</dt>
                  <dd data-testid="benchmark-flow-queue-pending-delta">{{ queuePendingDelta }}</dd>
                </div>
                <div class="evidence-item">
                  <dt>{{ t('benchmark.fields.totalDelta') }}</dt>
                  <dd data-testid="benchmark-flow-queue-total-delta">{{ queueTotalDelta }}</dd>
                </div>
              </dl>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('benchmark.tabs.report')" name="report">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('benchmark.report.eyebrow')"
              :title="t('benchmark.report.title')"
              :summary="t('benchmark.report.summary')"
              size="compact"
            />

            <p v-if="!report" class="empty-state">
              {{ t('benchmark.report.empty') }}
            </p>

            <template v-else>
              <div class="report-panel" data-testid="benchmark-flow-report">
                <dl class="evidence-grid">
                  <div class="evidence-item">
                    <dt>{{ t('common.fields.verdict') }}</dt>
                    <dd>{{ report.verdict }}</dd>
                  </div>
                  <div class="evidence-item">
                    <dt>{{ t('common.fields.taskType') }}</dt>
                    <dd>{{ report.taskType }}</dd>
                  </div>
                  <div class="evidence-item">
                    <dt>{{ t('common.fields.format') }}</dt>
                    <dd>{{ report.requestedFormat }}</dd>
                  </div>
                  <div class="evidence-item">
                    <dt>{{ t('benchmark.fields.rawDataPath') }}</dt>
                    <dd>{{ report.rawDataDownloadPath || '-' }}</dd>
                  </div>
                </dl>
                <p class="result-copy">
                  {{ t('benchmark.report.returned', { reportId: report.reportId, formats: report.availableFormats.join(', ') }) }}
                </p>
              </div>

              <section class="report-section" data-testid="benchmark-report-compare">
                <SectionHeader
                  :eyebrow="t('benchmark.report.engineEyebrow')"
                  :title="t('benchmark.report.comparisonTitle')"
                  size="compact"
                />
                <div class="row-list">
                  <article v-for="item in reportEngineResults" :key="item.engine" class="evidence-row">
                    <div class="row-heading">
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
                      <span>{{ t('benchmark.fields.scannedBytes') }} {{ item.scannedDataBytes }}</span>
                    </div>
                    <p class="result-copy">{{ item.notes || '-' }}</p>
                  </article>
                </div>
              </section>

              <section class="report-section" data-testid="benchmark-regression-results">
                <SectionHeader
                  :eyebrow="t('benchmark.report.regressionEyebrow')"
                  :title="t('benchmark.report.regressionTitle')"
                  size="compact"
                />
                <div class="row-list">
                  <article
                    v-for="item in reportThresholdAssessments"
                    :key="`${item.metric}-${item.verdict}`"
                    class="evidence-row"
                  >
                    <div class="row-heading">
                      <div>
                        <p class="section-kicker sqlforge-code-label">{{ item.metric }}</p>
                        <h3>{{ item.verdict }}</h3>
                      </div>
                      <span class="catalog-pill">{{ item.actualValue }} / {{ item.targetValue }}</span>
                    </div>
                    <p class="result-copy">{{ item.summary }}</p>
                  </article>
                </div>
              </section>

              <section class="report-section">
                <SectionHeader
                  :eyebrow="t('benchmark.report.trendEyebrow')"
                  :title="t('benchmark.report.trendTitle')"
                  size="compact"
                />
                <div class="row-list">
                  <article v-for="item in reportTrendCharts" :key="item.title" class="evidence-row">
                    <div class="row-heading">
                      <div>
                        <p class="section-kicker sqlforge-code-label">{{ item.chartType }}</p>
                        <h3>{{ item.title }}</h3>
                      </div>
                      <span class="catalog-pill">{{ item.series?.length || 0 }} series</span>
                    </div>
                    <p class="result-copy">{{ item.xAxisLabel }} / {{ item.yAxisLabel }}</p>
                  </article>
                  <article v-for="item in reportRecommendations" :key="item.title" class="evidence-row">
                    <div class="row-heading">
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
              </section>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('benchmark.tabs.sessionTasks')" name="sessionTasks">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('benchmark.session.eyebrow')"
              :title="t('benchmark.session.title')"
              :summary="t('benchmark.session.summary')"
              size="compact"
            />

            <p v-if="!sessionTasks.length" class="empty-state">
              {{ t('benchmark.session.empty') }}
            </p>

            <div v-else class="row-list">
              <article
                v-for="item in sessionTasks"
                :key="item.taskId"
                class="evidence-row"
                data-testid="benchmark-session-task"
              >
                <div class="row-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.taskType }}</p>
                    <h3>{{ item.taskId }}</h3>
                  </div>
                  <span class="catalog-pill">{{ item.status }}</span>
                </div>
                <p class="result-copy">{{ item.currentPhase || '-' }}</p>
                <div class="catalog-meta-row">
                  <span>{{ t('common.fields.scenario') }}: {{ item.scenario }}</span>
                  <span>{{ t('common.fields.report') }}: {{ item.reportId || '-' }}</span>
                  <span>{{ t('common.fields.verdict') }}: {{ item.verdict || '-' }}</span>
                </div>
              </article>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
  </section>
</template>

<style scoped>
.benchmark-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.form-grid,
.evidence-grid {
  display: grid;
  gap: var(--sqlforge-space-4);
}

.form-grid {
  width: 100%;
  grid-template-columns: minmax(180px, 0.35fr) minmax(180px, 0.35fr) minmax(320px, 1fr);
}

.section-kicker,
.field-label,
.catalog-footnote {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.field-block,
.evidence-item {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
}

.field-block-wide {
  grid-column: span 1;
}

.tab-stage,
.select-row,
.evidence-row,
.report-panel,
.trace-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(35, 35, 35, 0.72);
}

.empty-state,
.request-note,
.result-copy,
.catalog-meta-row {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.tab-stage {
  padding: var(--sqlforge-space-5);
}

.tab-panel,
.row-list,
.report-section {
  display: grid;
  gap: var(--sqlforge-space-4);
}

.evidence-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.select-row,
.evidence-row,
.report-panel,
.trace-panel {
  padding: 16px;
  text-align: left;
}

.row-heading,
.result-banner {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.row-heading h3 {
  margin: 0;
}

.select-row {
  width: 100%;
  cursor: pointer;
}

.select-row-active {
  border-color: var(--sqlforge-color-brand-border);
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
}

.result-banner {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid transparent;
  margin: 18px 0;
}

.result-banner-success {
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

.evidence-item {
  padding: 12px 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(20, 24, 31, 0.48);
}

.evidence-item dt {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.evidence-item dd {
  margin: 0;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
  overflow-wrap: anywhere;
}

@media (max-width: 1080px) {
  .form-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
