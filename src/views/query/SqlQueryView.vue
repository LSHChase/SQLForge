<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  executeQuery,
  formatRuntimeError,
  getGovernanceMessageStats
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  sqlText: 'SELECT * FROM orders',
  datasourceType: 'HETU',
  accelerationPreference: 'PREFER_ACCELERATED',
  faultToleranceStrategy: 'FAIL_FAST'
})

const running = ref(false)
const lastScenario = ref('success')
const result = ref(null)
const errorMessage = ref('')
const queueStatsBefore = ref(null)
const queueStatsAfter = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const previewRows = computed(() => result.value?.rows || [])
const retryPath = computed(() => result.value?.retryPath || [])
const queuePendingDelta = computed(() => {
  if (!queueStatsBefore.value || !queueStatsAfter.value) {
    return 0
  }
  return queueStatsAfter.value.pending - queueStatsBefore.value.pending
})
const compensationDetected = computed(() => queuePendingDelta.value >= 1)

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

const resetEvidence = () => {
  result.value = null
  errorMessage.value = ''
  queueStatsBefore.value = null
  queueStatsAfter.value = null
}

const applySuccessPreset = () => {
  form.sqlText = 'SELECT * FROM orders'
  form.datasourceType = 'HETU'
  form.accelerationPreference = 'PREFER_ACCELERATED'
  form.faultToleranceStrategy = 'FAIL_FAST'
}

const applyRecoveryPreset = () => {
  form.sqlText = 'SELECT * FROM orders'
  form.datasourceType = 'HETU'
  form.accelerationPreference = 'PREFER_ACCELERATED'
  form.faultToleranceStrategy = 'RETRY_THEN_FALLBACK'
}

const runSuccessFlow = async () => {
  applySuccessPreset()
  lastScenario.value = 'success'
  running.value = true
  resetEvidence()

  try {
    result.value = await executeQuery({
      ...form
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const runRecoveryFlow = async () => {
  applyRecoveryPreset()
  lastScenario.value = 'recovery'
  running.value = true
  resetEvidence()

  try {
    queueStatsBefore.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-query-governance-stats-before'
    })
    result.value = await executeQuery({
      ...form,
      queryContext: {
        timeoutMs: 30
      }
    })
    queueStatsAfter.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-query-governance-stats-after'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}
</script>

<template>
  <section class="runtime-page" data-testid="query-flow-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('sqlQuery.title') }}</h1>
        <p class="runtime-summary">{{ t('sqlQuery.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页面同时覆盖成功执行与降级恢复场景。补偿场景会先读取 governance 队列 stats，再触发浏览器侧真实请求并检查 pending 增量。'
            : 'This page covers both the success path and degraded recovery. The compensation path reads governance queue stats before and after the live browser request and checks for a pending-count increase.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">live request</p>
            <h2 class="section-title">
              {{ isChinese ? '执行参数' : 'Execution request' }}
            </h2>
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

          <label class="field-block field-block-wide">
            <span class="field-label">SQL</span>
            <el-input
              v-model="form.sqlText"
              type="textarea"
              :rows="6"
            />
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

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="running && lastScenario === 'success'"
            data-testid="query-flow-submit"
            @click="runSuccessFlow"
          >
            {{ isChinese ? '执行成功链路' : 'Run success gate' }}
          </el-button>
          <el-button
            :loading="running && lastScenario === 'recovery'"
            data-testid="query-flow-submit-recovery"
            @click="runRecoveryFlow"
          >
            {{ isChinese ? '执行降级恢复 + 补偿' : 'Run degraded recovery + compensation' }}
          </el-button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">runtime evidence</p>
            <h2 class="section-title">
              {{ isChinese ? '返回结果' : 'Runtime evidence' }}
            </h2>
          </div>
        </div>

        <p v-if="!result && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '点击左侧按钮后，这里会展示 query-execution 的真实响应，以及补偿场景的 governance 队列证据。'
              : 'After you trigger a scenario, the live query-execution response and any governance queue compensation evidence will render here.'
          }}
        </p>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="query-flow-error"
        >
          {{ errorMessage }}
        </div>

        <template v-if="result">
          <div
            class="result-banner"
            :class="result.status === 'SUCCESS' ? 'result-banner-success' : 'result-banner-warning'"
          >
            <strong data-testid="query-flow-status">{{ result.status }}</strong>
            <span data-testid="query-flow-engine">{{ result.metadata?.targetEngine || '-' }}</span>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? 'SQL 指纹' : 'SQL fingerprint' }}</span>
              <strong>{{ result.sqlFingerprint }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '降级执行' : 'Degraded' }}</span>
              <strong data-testid="query-flow-degraded">{{ result.degraded ? 'true' : 'false' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '补偿重试步数' : 'Retry path size' }}</span>
              <strong data-testid="query-flow-retry-path-size">{{ retryPath.length }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '实现阶段' : 'Implementation stage' }}</span>
              <strong>{{ result.implementationStage }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '降级原因' : 'Degrade reason' }}</span>
              <strong data-testid="query-flow-degrade-reason">{{ result.degradeReason || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '返回行数' : 'Rows returned' }}</span>
              <strong data-testid="query-flow-row-count">{{ previewRows.length }}</strong>
            </div>
          </div>

          <div
            v-if="retryPath.length > 0"
            class="trace-card"
            data-testid="query-flow-retry-path"
          >
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

          <div
            v-if="queueStatsBefore && queueStatsAfter"
            class="compensation-card"
          >
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
                <span class="evidence-label">{{ isChinese ? '失败消息数' : 'Failed messages' }}</span>
                <strong>{{ queueStatsAfter.failed }}</strong>
              </div>
            </div>
          </div>

          <pre class="result-json">{{ JSON.stringify(previewRows.slice(0, 2), null, 2) }}</pre>
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
  align-items: start;
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

.trace-card {
  display: grid;
  gap: 12px;
}

.trace-step {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  color: var(--sqlforge-text-secondary);
}

.result-json {
  margin: 20px 0 0;
  padding: 16px;
  border-radius: var(--sqlforge-radius-md);
  background: rgba(7, 13, 28, 0.62);
  color: var(--sqlforge-text-secondary);
  overflow: auto;
}

@media (max-width: 960px) {
  .runtime-grid,
  .form-grid,
  .evidence-grid,
  .trace-step {
    grid-template-columns: 1fr;
  }
}
</style>
