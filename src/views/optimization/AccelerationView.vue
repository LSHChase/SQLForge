<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
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
const submitResult = ref(null)
const taskStatus = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
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

const runFlow = async () => {
  running.value = true
  submitResult.value = null
  taskStatus.value = null
  errorMessage.value = ''

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
            ? '该页把前端按钮直接接到 sql-optimization 任务提交和状态轮询，不再只展示占位信息。'
            : 'This page wires the button directly into sql-optimization submission and terminal-status polling instead of a placeholder.'
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

        <div class="action-row">
          <el-button
            type="primary"
            :loading="running"
            data-testid="optimization-flow-submit"
            @click="runFlow"
          >
            {{ isChinese ? '提交真实优化任务' : 'Run live optimization flow' }}
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
              ? '左侧提交后，这里会展示任务入队、轮询和终态结果。'
              : 'Submitting the form will populate task queueing, polling and terminal-state evidence here.'
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

          <p v-else-if="taskStatus.failure" class="result-copy">
            {{ taskStatus.failure.message }}
          </p>
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

.result-banner-success {
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

@media (max-width: 960px) {
  .runtime-grid,
  .form-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }
}
</style>
