<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getGovernanceMessageStats,
  getOptimizationTaskStatus,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  submitOptimizationTask,
  waitForOptimizationTask
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  taskType: 'REWRITE',
  sqlText: 'SELECT * FROM orders',
  datasourceType: 'HETU'
})

const running = ref(false)
const lastScenario = ref('success')
const submitResult = ref(null)
const taskStatus = ref(null)
const compensationStatus = ref(null)
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
const taskTypeOptions = computed(() => [
  {
    value: 'REWRITE',
    label: isChinese.value ? '改写建议' : 'Rewrite suggestion'
  },
  {
    value: 'ACCELERATION_SUGGESTION',
    label: isChinese.value ? '加速建议' : 'Acceleration suggestion'
  }
])

const resetEvidence = () => {
  submitResult.value = null
  taskStatus.value = null
  compensationStatus.value = null
  queueStatsBefore.value = null
  queueStatsAfter.value = null
  errorMessage.value = ''
}

const applySuccessPreset = () => {
  form.taskType = 'REWRITE'
  form.sqlText = 'SELECT * FROM orders'
  form.datasourceType = 'HETU'
}

const applyFailurePreset = () => {
  form.taskType = 'ACCELERATION_SUGGESTION'
  form.sqlText = 'SELECT * FROM orders /*FAIL_OPTIMIZATION*/'
  form.datasourceType = 'HETU'
}

const runSuccessFlow = async () => {
  applySuccessPreset()
  lastScenario.value = 'success'
  running.value = true
  resetEvidence()

  try {
    submitResult.value = await submitOptimizationTask({
      ...form,
      taskContext: {
        priority: 'HIGH',
        parseDepth: 'DEEP'
      }
    })
    taskStatus.value = await waitForOptimizationTask(submitResult.value.taskId, form.tenantId)
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
      requestPrefix: 'frontend-sql-optimization-governance-stats-before'
    })
    submitResult.value = await submitOptimizationTask({
      ...form
    })
    taskStatus.value = await waitForOptimizationTask(submitResult.value.taskId, form.tenantId)
    compensationStatus.value = await getOptimizationTaskStatus(submitResult.value.taskId, form.tenantId, {
      requestPrefix: 'frontend-sql-optimization-compensation-status',
      tracePrefix: GOVERNANCE_COMPENSATION_TRACE_PREFIX
    })
    queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-sql-optimization-governance-stats-after'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}
</script>

<template>
  <section class="runtime-page" data-testid="optimization-flow-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('acceleration.title') }}</h1>
        <p class="runtime-summary">{{ t('acceleration.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页把浏览器按钮直接接到 sql-optimization 提交、终态轮询和补偿状态查询。失败场景会校验 governance 队列 pending 是否增长。'
            : 'This page wires browser actions to sql-optimization submission, terminal polling, and compensated status queries. The failure path checks whether governance queue pending count increases.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">async task</p>
            <h2 class="section-title">
              {{ isChinese ? '优化任务参数' : 'Optimization request' }}
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
            <el-select v-model="form.taskType">
              <el-option
                v-for="option in taskTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
            <el-input v-model="form.datasourceType" />
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

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="running && lastScenario === 'success'"
            data-testid="optimization-flow-submit"
            @click="runSuccessFlow"
          >
            {{ isChinese ? '执行成功任务' : 'Run success task' }}
          </el-button>
          <el-button
            :loading="running && lastScenario === 'failure'"
            data-testid="optimization-flow-submit-failure"
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
              {{ isChinese ? '任务状态' : 'Task evidence' }}
            </h2>
          </div>
        </div>

        <p v-if="!submitResult && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '左侧按钮会分别展示成功摘要，或失败终态 + 补偿队列证据。'
              : 'The actions on the left render either the success summary or the failed terminal state plus compensation queue evidence.'
          }}
        </p>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="optimization-flow-error"
        >
          {{ errorMessage }}
        </div>

        <template v-if="submitResult">
          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '任务 ID' : 'Task ID' }}</span>
              <strong data-testid="optimization-flow-task-id">{{ submitResult.taskId }}</strong>
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
            <strong data-testid="optimization-flow-status">{{ taskStatus.status }}</strong>
            <span>{{ taskStatus.currentPhase }}</span>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '进度' : 'Progress' }}</span>
              <strong>{{ taskStatus.progressPercent }}%</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '实现阶段' : 'Implementation stage' }}</span>
              <strong>{{ taskStatus.implementationStage }}</strong>
            </div>
          </div>

          <p
            v-if="taskStatus.suggestion?.summary"
            class="result-copy"
            data-testid="optimization-flow-summary"
          >
            {{ taskStatus.suggestion.summary }}
          </p>

          <div
            v-else-if="taskStatus.failure"
            class="trace-card"
          >
            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '失败码' : 'Failure code' }}</span>
                <strong data-testid="optimization-flow-failure-code">{{ taskStatus.failure.code }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '可重试' : 'Retryable' }}</span>
                <strong>{{ taskStatus.failure.retryable ? 'true' : 'false' }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '失败阶段' : 'Failed phase' }}</span>
                <strong>{{ taskStatus.failure.failedPhase }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '建议动作' : 'Suggested action' }}</span>
                <strong>{{ taskStatus.failure.suggestedAction }}</strong>
              </div>
            </div>
            <p class="result-copy">{{ taskStatus.failure.message }}</p>
          </div>
        </template>

        <template v-if="compensationStatus">
          <div class="compensation-card">
            <div class="result-banner" :class="compensationDetected ? 'result-banner-success' : 'result-banner-danger'">
              <strong data-testid="optimization-flow-compensation-indicator">
                {{ compensationDetected ? 'COMPENSATED' : 'NOT_COMPENSATED' }}
              </strong>
              <span data-testid="optimization-flow-compensation-status">{{ compensationStatus.status }}</span>
            </div>

            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '补偿前 pending' : 'Pending before' }}</span>
                <strong data-testid="optimization-flow-queue-pending-before">{{ queueStatsBefore?.pending ?? 0 }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '补偿后 pending' : 'Pending after' }}</span>
                <strong data-testid="optimization-flow-queue-pending-after">{{ queueStatsAfter?.pending ?? 0 }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? 'pending 增量' : 'Pending delta' }}</span>
                <strong data-testid="optimization-flow-queue-pending-delta">{{ queuePendingDelta }}</strong>
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

.action-row {
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

.trace-card,
.compensation-card {
  border: 1px solid transparent;
  border-radius: var(--sqlforge-radius-md);
  padding: 16px;
  margin-top: 20px;
}

@media (max-width: 960px) {
  .runtime-grid,
  .form-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
