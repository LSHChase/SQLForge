<script setup>
import { computed, reactive, ref } from 'vue'
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

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  taskType: 'COMPARISON',
  sqlText: 'SELECT * FROM orders'
})

const running = ref(false)
const lastScenario = ref('success')
const submitResult = ref(null)
const taskStatus = ref(null)
const compensationStatus = ref(null)
const report = ref(null)
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
const queuePendingDelta = computed(() => {
  if (!queueStatsBefore.value || !queueStatsAfter.value) {
    return 0
  }
  return queueStatsAfter.value.pending - queueStatsBefore.value.pending
})
const compensationDetected = computed(() => queuePendingDelta.value >= 1)

const resetEvidence = () => {
  submitResult.value = null
  taskStatus.value = null
  compensationStatus.value = null
  report.value = null
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  errorMessage.value = ''
}

const applySuccessPreset = () => {
  form.taskType = 'COMPARISON'
  form.sqlText = 'SELECT * FROM orders'
}

const applyFailurePreset = () => {
  form.taskType = 'BASELINE'
  form.sqlText = 'SELECT * FROM orders /*FAIL_BENCHMARK*/'
}

const runSuccessFlow = async () => {
  applySuccessPreset()
  lastScenario.value = 'success'
  running.value = true
  resetEvidence()

  try {
    submitResult.value = await submitBenchmarkTask({
      ...form,
      taskContext: {
        priority: 'HIGH',
        targetEngines: ['HETU', 'HIVE'],
        concurrency: 16,
        durationSeconds: 300,
        rampUpSeconds: 30,
        readonlyRequired: true,
        shadowEnvironmentMode: 'REQUIRED'
      }
    })
    taskStatus.value = await waitForBenchmarkTask(submitResult.value.taskId, form.tenantId)

    if (taskStatus.value.reportId) {
      report.value = await getBenchmarkReport(taskStatus.value.reportId, form.tenantId)
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const runFailureCompensationFlow = async () => {
  applyFailurePreset()
  lastScenario.value = 'failure'
  running.value = true
  resetEvidence()

  try {
    queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-benchmark-governance-stats-before'
    })
    submitResult.value = await submitBenchmarkTask({
      ...form
    })
    taskStatus.value = await waitForBenchmarkTask(submitResult.value.taskId, form.tenantId)
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
</script>

<template>
  <section class="runtime-page" data-testid="benchmark-flow-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('benchmark.title') }}</h1>
        <p class="runtime-summary">{{ t('benchmark.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页从浏览器触发 benchmark-engine 成功链路与失败补偿链路。成功场景继续读取真实 JSON report，失败场景则检查 governance 队列 pending 增量。'
            : 'This page drives both the benchmark-engine success path and the failure-compensation path from the browser. The success flow reads the live JSON report, while the failure flow checks governance queue pending growth.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">benchmark task</p>
            <h2 class="section-title">
              {{ isChinese ? '压测请求' : 'Benchmark request' }}
            </h2>
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

          <label class="field-block field-block-wide">
            <span class="field-label">SQL</span>
            <el-input
              v-model="form.sqlText"
              type="textarea"
              :rows="6"
            />
          </label>
        </div>

        <div class="request-note">
          {{
            isChinese
              ? '成功预置固定为 HETU/HIVE 只读对比，失败预置使用 FAIL_BENCHMARK 注入 worker 失败。'
              : 'The success preset runs readonly HETU/HIVE comparison; the failure preset injects FAIL_BENCHMARK to drive the worker failure path.'
          }}
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="running && lastScenario === 'success'"
            data-testid="benchmark-flow-submit"
            @click="runSuccessFlow"
          >
            {{ isChinese ? '执行成功任务' : 'Run success task' }}
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
            <p class="section-kicker sqlforge-code-label">runtime evidence</p>
            <h2 class="section-title">
              {{ isChinese ? '任务与报告结果' : 'Task and report evidence' }}
            </h2>
          </div>
        </div>

        <p v-if="!submitResult && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '成功场景会展示 report 回写结果；失败场景会展示终态错误和审计补偿队列证据。'
              : 'The success path shows report write-back evidence; the failure path shows terminal errors and audit-compensation queue evidence.'
          }}
        </p>

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
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '目标引擎' : 'Target engines' }}</span>
              <strong>{{ taskStatus.targetEngines?.join(' / ') || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '只读要求' : 'Readonly required' }}</span>
              <strong>{{ taskStatus.readonlyRequired ? 'true' : 'false' }}</strong>
            </div>
          </div>

          <div
            v-if="taskStatus.error"
            class="trace-card"
          >
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

        <template v-if="report">
          <div class="report-card" data-testid="benchmark-flow-report">
            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '裁决' : 'Verdict' }}</span>
                <strong>{{ report.verdict }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '返回格式' : 'Requested format' }}</span>
                <strong>{{ report.requestedFormat }}</strong>
              </div>
            </div>
            <p class="result-copy">
              {{
                isChinese
                  ? `报告 ${report.reportId} 已通过真实接口返回，支持格式 ${report.availableFormats.join(', ')}。`
                  : `Report ${report.reportId} was returned by the live API with formats ${report.availableFormats.join(', ')}.`
              }}
            </p>
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
                <span class="evidence-label">{{ isChinese ? '补偿状态查询' : 'Compensated status query' }}</span>
                <strong>{{ compensationStatus.currentPhase }}</strong>
              </div>
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
  border-radius: var(--sqlforge-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 48%),
    var(--sqlforge-surface-2);
  box-shadow: 0 18px 42px rgba(0, 0, 0, 0.18);
}

.runtime-hero,
.runtime-grid > article {
  padding: 24px;
}

.runtime-hero {
  display: grid;
  gap: 16px;
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
.empty-state,
.request-note,
.result-copy {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.runtime-grid,
.form-grid,
.evidence-grid {
  display: grid;
  gap: 24px;
}

.runtime-grid,
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

.request-note,
.action-row,
.report-card {
  margin-top: 20px;
}

.action-row-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.result-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: var(--sqlforge-radius-md);
  border: 1px solid transparent;
  margin: 18px 0;
}

.result-banner-success,
.report-card,
.compensation-card,
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

.report-card,
.compensation-card,
.trace-card {
  border: 1px solid transparent;
  border-radius: var(--sqlforge-radius-md);
  padding: 16px;
}

@media (max-width: 960px) {
  .runtime-grid,
  .form-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
