<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getBenchmarkReport,
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
const submitResult = ref(null)
const taskStatus = ref(null)
const report = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')

const runFlow = async () => {
  running.value = true
  submitResult.value = null
  taskStatus.value = null
  report.value = null
  errorMessage.value = ''

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
            ? '该页从浏览器触发 benchmark-engine 任务、轮询终态，并继续读取真实 JSON 报告。'
            : 'This page triggers benchmark-engine from the browser, polls to terminal status and then loads the real JSON report.'
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
              ? '默认固定对 HETU/HIVE 进行只读对比，shadow mode = REQUIRED。'
              : 'The request defaults to readonly HETU/HIVE comparison with shadow mode REQUIRED.'
          }}
        </div>

        <div class="action-row">
          <el-button
            type="primary"
            :loading="running"
            data-testid="benchmark-flow-submit"
            @click="runFlow"
          >
            {{ isChinese ? '提交真实压测任务' : 'Run live benchmark flow' }}
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
              ? '提交后会展示任务 ID、终态、reportId 和报告摘要。'
              : 'Submitting the form will reveal the task ID, terminal status, reportId and report summary.'
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
.report-card {
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

.report-card {
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
