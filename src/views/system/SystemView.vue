<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getGovernanceMessageStats,
  getGovernanceTenantConfig,
  retryGovernanceFailedMessages
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'system'
})

const loading = ref(false)
const retrying = ref(false)
const tenantConfig = ref(null)
const stats = ref(null)
const retryResult = ref(null)
const statsBeforeRetry = ref(null)
const statsAfterRetry = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
const failedDelta = computed(() => {
  if (!statsBeforeRetry.value || !statsAfterRetry.value) {
    return 0
  }
  return statsBeforeRetry.value.failed - statsAfterRetry.value.failed
})
const retryImproved = computed(() => failedDelta.value >= 1 || (retryResult.value?.retriedCount || 0) >= 1)
const queueCards = computed(() => {
  if (!stats.value) {
    return []
  }
  return [
    { key: 'total', label: isChinese.value ? '消息总数' : 'Total messages', value: stats.value.total },
    { key: 'pending', label: isChinese.value ? '待补偿' : 'Pending backlog', value: stats.value.pending },
    { key: 'failed', label: isChinese.value ? '失败待修复' : 'Failed messages', value: stats.value.failed },
    { key: 'consumed', label: isChinese.value ? '已消费' : 'Consumed', value: stats.value.consumed }
  ]
})

const loadEvidence = async () => {
  loading.value = true
  retryResult.value = null
  statsBeforeRetry.value = null
  statsAfterRetry.value = null
  errorMessage.value = ''

  try {
    const [tenantConfigResponse, statsResponse] = await Promise.all([
      getGovernanceTenantConfig(form.tenantId, {
        requestPrefix: 'frontend-system-tenant-config'
      }),
      getGovernanceMessageStats(form.tenantId, {
        requestPrefix: 'frontend-system-message-stats'
      })
    ])
    tenantConfig.value = tenantConfigResponse
    stats.value = statsResponse
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.value = false
  }
}

const retryFailedMessages = async () => {
  retrying.value = true
  errorMessage.value = ''
  retryResult.value = null

  try {
    statsBeforeRetry.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-system-message-stats-before-retry'
    })
    retryResult.value = await retryGovernanceFailedMessages(form.tenantId, {
      requestPrefix: 'frontend-system-message-retry'
    })
    statsAfterRetry.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-system-message-stats-after-retry'
    })
    stats.value = statsAfterRetry.value
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    retrying.value = false
  }
}

onMounted(() => {
  loadEvidence()
})
</script>

<template>
  <section class="runtime-page" data-testid="system-flow-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('system.title') }}</h1>
        <p class="runtime-summary">{{ t('system.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页直接调用 governance 管理接口，展示租户配置、消息 backlog 和失败消息 retry 修复结果，用于把浏览器门禁继续扩到治理修复动作。'
            : 'This page calls the live governance admin APIs and renders tenant config, message backlog, and failed-message retry evidence so the browser gate can cover governance remediation actions.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">governance admin</p>
            <h2 class="section-title">
              {{ isChinese ? '治理管理动作' : 'Governance admin actions' }}
            </h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户上下文' : 'Tenant context' }}</span>
            <el-input
              v-model="form.tenantId"
              data-testid="system-flow-tenant-id"
            />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="loading"
            data-testid="system-flow-refresh"
            @click="loadEvidence"
          >
            {{ isChinese ? '刷新治理证据' : 'Refresh governance evidence' }}
          </el-button>
          <el-button
            :loading="retrying"
            data-testid="system-flow-retry"
            @click="retryFailedMessages"
          >
            {{ isChinese ? '重试失败消息' : 'Retry failed messages' }}
          </el-button>
        </div>

        <div class="remediation-card">
          <p class="remediation-title">{{ isChinese ? '修复动作说明' : 'Remediation guidance' }}</p>
          <p class="remediation-copy">
            {{
              isChinese
                ? '当 failed > 0 时，先确认 backlog 归因，再执行 retry。若 retriedCount 增加且 failed 降低，即视为补偿修复动作真实生效。'
                : 'When failed > 0, confirm the backlog source and then run retry. If retriedCount increases and failed decreases, the compensation repair action is considered effective.'
            }}
          </p>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">runtime evidence</p>
            <h2 class="section-title">
              {{ isChinese ? '治理与修复证据' : 'Governance and remediation evidence' }}
            </h2>
          </div>
        </div>

        <p v-if="!tenantConfig && !stats && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '页面会自动加载治理租户配置与消息队列统计。'
              : 'The page automatically loads the governance tenant config and message queue stats.'
          }}
        </p>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="system-flow-error"
        >
          {{ errorMessage }}
        </div>

        <template v-if="tenantConfig">
          <div class="result-banner result-banner-success">
            <strong data-testid="system-flow-tenant-config-status">{{ tenantConfig.tenantId }}</strong>
            <span>{{ tenantConfig.defaultEngine }} / {{ tenantConfig.backupEngine }}</span>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '审计级别' : 'Audit level' }}</span>
              <strong data-testid="system-flow-audit-level">{{ tenantConfig.auditLevel }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '保留天数' : 'Retention days' }}</span>
              <strong>{{ tenantConfig.retentionDays }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '并发配额' : 'Concurrency quota' }}</span>
              <strong>{{ tenantConfig.quotaConcurrent }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '加速配额' : 'Acceleration quota' }}</span>
              <strong>{{ tenantConfig.accelerationQuota }}</strong>
            </div>
          </div>
        </template>

        <template v-if="stats">
          <div class="queue-card-grid">
            <article
              v-for="card in queueCards"
              :key="card.key"
              class="queue-card"
            >
              <span class="queue-card-label">{{ card.label }}</span>
              <strong
                class="queue-card-value"
                :data-testid="`system-flow-queue-${card.key}`"
              >
                {{ card.value }}
              </strong>
            </article>
          </div>
        </template>

        <template v-if="retryResult">
          <div class="compensation-card">
            <div class="result-banner" :class="retryImproved ? 'result-banner-success' : 'result-banner-warning'">
              <strong data-testid="system-flow-retry-status">{{ retryResult.status }}</strong>
              <span data-testid="system-flow-retry-count">{{ retryResult.retriedCount }}</span>
            </div>

            <div class="evidence-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '重试前 failed' : 'Failed before retry' }}</span>
                <strong data-testid="system-flow-failed-before-retry">{{ statsBeforeRetry?.failed ?? 0 }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '重试后 failed' : 'Failed after retry' }}</span>
                <strong data-testid="system-flow-failed-after-retry">{{ statsAfterRetry?.failed ?? 0 }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? 'failed 降幅' : 'Failed delta' }}</span>
                <strong data-testid="system-flow-failed-delta">{{ failedDelta }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '修复结论' : 'Repair outcome' }}</span>
                <strong data-testid="system-flow-repair-outcome">
                  {{ retryImproved ? 'REPAIRED' : 'RETRY_ACCEPTED' }}
                </strong>
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
.field-label,
.queue-card-label {
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
.remediation-copy {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.7;
}

.runtime-grid,
.form-grid,
.evidence-grid,
.queue-card-grid {
  display: grid;
  gap: 24px;
}

.runtime-grid,
.evidence-grid,
.queue-card-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block,
.evidence-item,
.queue-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.queue-card,
.remediation-card,
.compensation-card {
  border: 1px solid transparent;
  border-radius: var(--sqlforge-radius-md);
  padding: 16px;
}

.queue-card,
.remediation-card {
  border-color: rgba(78, 143, 255, 0.28);
  background: rgba(78, 143, 255, 0.1);
}

.queue-card-value,
.remediation-title {
  color: var(--sqlforge-text-primary);
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
.compensation-card {
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
  .evidence-grid,
  .queue-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
