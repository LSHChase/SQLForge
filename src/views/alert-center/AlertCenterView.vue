<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getDispatchEvents,
  getGovernanceMessageStats,
  getParseStatisticsImportantUrgent
} from '../../services/runtimeGateApi'

const ACK_STORAGE_KEY = 'sqlforge-alert-center-acks'

const { locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a'
})

const loading = reactive({
  page: false
})

const messageStats = ref(null)
const dispatchEvents = ref([])
const importantUrgentItems = ref([])
const selectedAlertId = ref('')
const acknowledgedIds = ref([])
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')

const loadAcknowledgements = () => {
  try {
    const raw = window.localStorage.getItem(ACK_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    acknowledgedIds.value = Array.isArray(parsed) ? parsed : []
  } catch (error) {
    acknowledgedIds.value = []
  }
}

const persistAcknowledgements = () => {
  window.localStorage.setItem(ACK_STORAGE_KEY, JSON.stringify(acknowledgedIds.value))
}

const derivedAlerts = computed(() => {
  const alerts = []
  const stats = messageStats.value
  if (stats && ((stats.failed || 0) > 0 || (stats.pending || 0) > 0)) {
    alerts.push({
      alertId: 'derived-governance-backlog',
      sourceType: 'MESSAGE_BACKLOG',
      severity: (stats.failed || 0) > 0 ? 'HIGH' : 'MEDIUM',
      title: isChinese.value ? '治理补偿 backlog 告警' : 'Governance backlog alert',
      summary: isChinese.value
        ? `当前 failed=${stats.failed || 0}，pending=${stats.pending || 0}。`
        : `Current failed=${stats.failed || 0}, pending=${stats.pending || 0}.`,
      notifyStatus: 'SIMULATED_PENDING_NOTIFY',
      evidence: stats
    })
  }

  for (const item of dispatchEvents.value) {
    const status = String(item.status || '').toUpperCase()
    if (!['FAILED', 'PULLED', 'PUBLISHED'].includes(status)) {
      continue
    }
    alerts.push({
      alertId: `dispatch-${item.dispatchEventId}`,
      sourceType: 'DISPATCH_EVENT',
      severity: status === 'FAILED' ? 'HIGH' : 'MEDIUM',
      title: isChinese.value ? '装数协同事件待处理' : 'Dispatch event requires attention',
      summary: `${item.dispatchEventId} · ${status} · ${item.resultMessage || '-'}`,
      notifyStatus: status === 'FAILED' ? 'SIMULATED_NOTIFIED' : 'SIMULATED_PENDING_NOTIFY',
      evidence: item
    })
  }

  for (const item of importantUrgentItems.value) {
    alerts.push({
      alertId: `parse-${item.itemId || item.parseTaskId || item.sqlDigest}`,
      sourceType: 'IMPORTANT_URGENT_SQL',
      severity: item.urgent ? 'HIGH' : 'MEDIUM',
      title: isChinese.value ? '解析优先级告警' : 'Parse-priority alert',
      summary: `${item.reportCode || '-'} · ${item.highestPriorityLevel || '-'} · ${item.issueCount || 0} issues`,
      notifyStatus: item.urgent ? 'SIMULATED_NOTIFIED' : 'SIMULATED_PENDING_NOTIFY',
      evidence: item
    })
  }

  return alerts
    .sort(compareAlertSeverity)
    .map(item => ({
      ...item,
      ackStatus: acknowledgedIds.value.includes(item.alertId) ? 'ACK_SIMULATED' : 'OPEN',
      ackMode: 'FRONTEND_SIMULATED'
    }))
})

const alertSummaryCards = computed(() => [
  {
    key: 'total',
    label: isChinese.value ? '总告警' : 'Total alerts',
    value: derivedAlerts.value.length
  },
  {
    key: 'open',
    label: isChinese.value ? '未 ACK' : 'Open',
    value: derivedAlerts.value.filter(item => item.ackStatus === 'OPEN').length
  },
  {
    key: 'high',
    label: isChinese.value ? '高优先级' : 'High severity',
    value: derivedAlerts.value.filter(item => item.severity === 'HIGH').length
  },
  {
    key: 'simulated',
    label: isChinese.value ? 'notify simulated' : 'Notify simulated',
    value: derivedAlerts.value.filter(item => item.notifyStatus === 'SIMULATED_NOTIFIED').length
  }
])

const selectedAlert = computed(() =>
  derivedAlerts.value.find(item => item.alertId === selectedAlertId.value) || null
)

const refreshAlerts = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    const [stats, events, importantUrgent] = await Promise.all([
      getGovernanceMessageStats(form.tenantId, {
        requestPrefix: 'frontend-alert-center-message-stats'
      }),
      getDispatchEvents(form.tenantId, '', {
        requestPrefix: 'frontend-alert-center-dispatch-events'
      }),
      getParseStatisticsImportantUrgent(form.tenantId, {
        requestPrefix: 'frontend-alert-center-important-urgent'
      })
    ])
    messageStats.value = stats
    dispatchEvents.value = Array.isArray(events) ? events : []
    importantUrgentItems.value = Array.isArray(importantUrgent) ? importantUrgent : []

    if (!selectedAlertId.value && derivedAlerts.value.length) {
      selectedAlertId.value = derivedAlerts.value[0].alertId
    } else if (selectedAlertId.value && !derivedAlerts.value.some(item => item.alertId === selectedAlertId.value)) {
      selectedAlertId.value = derivedAlerts.value[0]?.alertId || ''
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const acknowledgeAlert = alertId => {
  if (!alertId || acknowledgedIds.value.includes(alertId)) {
    return
  }
  acknowledgedIds.value = [...acknowledgedIds.value, alertId]
  persistAcknowledgements()
}

const clearAcknowledgement = alertId => {
  acknowledgedIds.value = acknowledgedIds.value.filter(item => item !== alertId)
  persistAcknowledgements()
}

const compareAlertSeverity = (left, right) => {
  const rank = {
    HIGH: 2,
    MEDIUM: 1,
    LOW: 0
  }
  return (rank[right.severity] || 0) - (rank[left.severity] || 0)
}

watch(
  () => form.tenantId,
  () => {
    selectedAlertId.value = ''
  }
)

onMounted(() => {
  loadAcknowledgements()
  refreshAlerts()
})
</script>

<template>
  <section class="alert-page" data-testid="alert-page">
    <header class="alert-hero sqlforge-panel">
      <div>
        <p class="section-kicker sqlforge-code-label">alert center</p>
        <h1>{{ isChinese ? '告警中心与通知状态' : 'Alert center and notification state' }}</h1>
        <p class="hero-summary">
          {{
            isChinese
              ? '当前仓库还没有独立 `GET /api/governance/alerts` 控制器，因此本页先基于 backlog、dispatch event 和 important/urgent SQL 派生告警，并把 ACK / notify 明确标成 simulated。'
              : 'The repository does not yet expose a dedicated `GET /api/governance/alerts` controller, so this page derives alerts from backlog, dispatch events, and important-or-urgent SQL while marking ACK and notify as simulated.'
          }}
        </p>
      </div>
      <div class="hero-actions">
        <label class="field-label">
          <span>{{ isChinese ? '租户' : 'Tenant' }}</span>
          <input v-model.trim="form.tenantId" class="text-input">
        </label>
        <button class="primary-button" data-testid="alert-refresh" @click="refreshAlerts">
          {{ isChinese ? '刷新告警' : 'Refresh alerts' }}
        </button>
      </div>
    </header>

    <p v-if="errorMessage" class="error-banner" data-testid="alert-error">{{ errorMessage }}</p>

    <section class="summary-grid">
      <article
        v-for="item in alertSummaryCards"
        :key="item.key"
        class="summary-card"
      >
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <div class="alert-grid">
      <article class="sqlforge-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">derived alerts</p>
            <h2>{{ isChinese ? '告警列表' : 'Alert list' }}</h2>
          </div>
        </div>

        <div class="alert-list">
          <button
            v-for="item in derivedAlerts"
            :key="item.alertId"
            type="button"
            class="alert-item"
            :class="{ 'alert-item-active': selectedAlertId === item.alertId }"
            data-testid="alert-item"
            @click="selectedAlertId = item.alertId"
          >
            <div class="alert-item-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.sourceType }}</p>
                <h3>{{ item.title }}</h3>
              </div>
              <span class="status-pill" :class="{ 'status-pill-warn': item.severity === 'HIGH' }">
                {{ item.severity }}
              </span>
            </div>
            <p class="hero-summary">{{ item.summary }}</p>
            <div class="pill-row">
              <span class="mini-pill">{{ item.ackStatus }}</span>
              <span class="mini-pill">{{ item.notifyStatus }}</span>
              <span class="mini-pill">{{ item.ackMode }}</span>
            </div>
          </button>
        </div>
      </article>

      <article class="sqlforge-panel" data-testid="alert-detail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">alert detail</p>
            <h2>{{ isChinese ? '详情、ACK 与 notify 状态' : 'Detail, ACK, and notify state' }}</h2>
          </div>
        </div>

        <p v-if="!selectedAlert" class="hero-summary">
          {{ isChinese ? '当前没有可展示的告警。' : 'No alert is available to display.' }}
        </p>

        <template v-else>
          <div class="summary-grid">
            <article class="summary-card">
              <span class="summary-card-label">alertId</span>
              <strong>{{ selectedAlert.alertId }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? 'ACK 状态' : 'ACK status' }}</span>
              <strong data-testid="alert-ack-status">{{ selectedAlert.ackStatus }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '通知状态' : 'Notify status' }}</span>
              <strong data-testid="alert-notify-status">{{ selectedAlert.notifyStatus }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? 'ACK 模式' : 'ACK mode' }}</span>
              <strong>{{ selectedAlert.ackMode }}</strong>
            </article>
          </div>

          <div class="action-row">
            <button
              class="primary-button"
              data-testid="alert-ack"
              :disabled="selectedAlert.ackStatus === 'ACK_SIMULATED'"
              @click="acknowledgeAlert(selectedAlert.alertId)"
            >
              {{ isChinese ? 'ACK 模拟确认' : 'Simulate ACK' }}
            </button>
            <button
              class="secondary-button"
              :disabled="selectedAlert.ackStatus !== 'ACK_SIMULATED'"
              @click="clearAcknowledgement(selectedAlert.alertId)"
            >
              {{ isChinese ? '撤销模拟 ACK' : 'Clear simulated ACK' }}
            </button>
          </div>

          <pre class="code-block">{{ formatJson(selectedAlert.evidence) }}</pre>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.alert-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.alert-hero,
.alert-grid > article {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.alert-hero {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
}

.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(41, 41, 41, 0.84);
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
}

.text-input {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.primary-button,
.secondary-button,
.alert-item {
  cursor: pointer;
}

.primary-button,
.secondary-button {
  min-height: 42px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  padding: 0 18px;
  font-weight: 500;
}

.primary-button {
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  border-color: rgba(212, 96, 96, 0.35);
}

.secondary-button {
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
}

.summary-grid,
.alert-grid {
  display: grid;
  gap: 20px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.alert-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card,
.alert-item {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.summary-card-label {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--sqlforge-text-muted);
}

.alert-list,
.pill-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pill-row {
  flex-direction: row;
  flex-wrap: wrap;
  gap: 8px;
}

.alert-item {
  text-align: left;
}

.alert-item-active {
  border-color: rgba(212, 96, 96, 0.35);
}

.alert-item-header,
.section-heading,
.action-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.status-pill,
.mini-pill {
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

.status-pill-warn {
  border-color: rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

.hero-summary,
.error-banner {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.error-banner {
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(212, 96, 96, 0.35);
  background: rgba(120, 28, 28, 0.18);
  color: #ffd6d6;
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .alert-hero,
  .alert-grid {
    grid-template-columns: 1fr;
  }
}
</style>
