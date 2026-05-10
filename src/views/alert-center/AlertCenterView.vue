<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'
import {
  formatRuntimeError,
  getDispatchEvents,
  getGovernanceMessageStats,
  getParseStatisticsImportantUrgent
} from '../../services/runtimeGateApi'

const ACK_STORAGE_KEY = 'sqlforge-alert-center-acks'
const backendReadPath = 'GET /api/governance/alerts'

const { t } = useI18n()

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
const placeholderDialogVisible = ref(false)
const placeholderPayload = ref({
  title: '',
  capability: '',
  reason: '',
  nextStep: ''
})

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
      title: t('alertCenter.derived.backlogTitle'),
      summary: t('alertCenter.derived.backlogSummary', {
        failed: stats.failed || 0,
        pending: stats.pending || 0
      }),
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
      title: t('alertCenter.derived.dispatchTitle'),
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
      title: t('alertCenter.derived.parseTitle'),
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
    label: t('alertCenter.metrics.total'),
    value: derivedAlerts.value.length
  },
  {
    key: 'open',
    label: t('alertCenter.metrics.open'),
    value: derivedAlerts.value.filter(item => item.ackStatus === 'OPEN').length,
    tone: 'warning'
  },
  {
    key: 'high',
    label: t('alertCenter.metrics.high'),
    value: derivedAlerts.value.filter(item => item.severity === 'HIGH').length,
    tone: 'danger'
  },
  {
    key: 'simulated',
    label: t('alertCenter.metrics.simulated'),
    value: derivedAlerts.value.filter(item => item.notifyStatus === 'SIMULATED_NOTIFIED').length
  }
])

const selectedAlert = computed(() =>
  derivedAlerts.value.find(item => item.alertId === selectedAlertId.value) || null
)

const openPlaceholderAction = actionType => {
  placeholderPayload.value = actionType === 'create'
    ? {
        title: t('alertCenter.placeholder.createTitle'),
        capability: t('alertCenter.placeholder.createCapability'),
        reason: t('alertCenter.placeholder.createReason'),
        nextStep: t('alertCenter.placeholder.createNextStep')
      }
    : {
        title: t('alertCenter.placeholder.editTitle'),
        capability: t('alertCenter.placeholder.editCapability'),
        reason: t('alertCenter.placeholder.editReason'),
        nextStep: t('alertCenter.placeholder.editNextStep')
      }
  placeholderDialogVisible.value = true
}

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

const formatJson = value => JSON.stringify(value, null, 2)

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
    <SectionHeader
      eyebrow="alert center"
      :title="t('alertCenter.pageTitle')"
      :summary="t('alertCenter.refactorSummary', { readPath: backendReadPath })"
    />

    <ToolbarShell
      eyebrow="alert controls"
      :title="t('alertCenter.controlsTitle')"
      :summary="t('alertCenter.controlsSummary')"
      test-id="alert-toolbar"
    >
      <label class="alert-field">
        <span>{{ t('governanceTrace.tenantContext') }}</span>
        <el-input v-model.trim="form.tenantId" data-testid="alert-tenant-id" />
      </label>
      <el-button type="primary" :loading="loading.page" data-testid="alert-refresh" @click="refreshAlerts">
        {{ t('alertCenter.actions.refresh') }}
      </el-button>
      <el-button @click="openPlaceholderAction('create')">
        {{ t('alertCenter.actions.createRule') }}
      </el-button>
      <el-button @click="openPlaceholderAction('edit')">
        {{ t('alertCenter.actions.editNotify') }}
      </el-button>
    </ToolbarShell>

    <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="alert-error">
      {{ errorMessage }}
    </div>

    <section class="summary-grid">
      <MetricCard
        v-for="item in alertSummaryCards"
        :key="item.key"
        v-bind="{ label: item.label, value: item.value, tone: item.tone || 'neutral' }"
      />
    </section>

    <div class="alert-grid">
      <EvidencePanel eyebrow="derived alerts" :title="t('alertCenter.listTitle')" tone="warning">
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
                <p class="alert-source sqlforge-code-label">{{ item.sourceType }}</p>
                <h3>{{ item.title }}</h3>
              </div>
              <span class="status-pill" :class="{ 'status-pill-warn': item.severity === 'HIGH' }">
                {{ item.severity }}
              </span>
            </div>
            <p class="alert-summary">{{ item.summary }}</p>
            <div class="pill-row">
              <span class="mini-pill">{{ item.ackStatus }}</span>
              <span class="mini-pill">{{ item.notifyStatus }}</span>
              <span class="mini-pill">{{ item.ackMode }}</span>
            </div>
          </button>
        </div>
      </EvidencePanel>

      <EvidencePanel data-testid="alert-detail" eyebrow="alert detail" :title="t('alertCenter.detailTitle')">
        <p v-if="!selectedAlert" class="empty-state">
          {{ t('alertCenter.messages.noAlert') }}
        </p>

        <template v-else>
          <div class="alert-detail-grid">
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.alertId') }}</span>
              <strong>{{ selectedAlert.alertId }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.ackStatus') }}</span>
              <strong data-testid="alert-ack-status">{{ selectedAlert.ackStatus }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.notifyStatus') }}</span>
              <strong data-testid="alert-notify-status">{{ selectedAlert.notifyStatus }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.ackMode') }}</span>
              <strong>{{ selectedAlert.ackMode }}</strong>
            </div>
          </div>

          <div class="action-row">
            <el-button
              type="primary"
              data-testid="alert-ack"
              :disabled="selectedAlert.ackStatus === 'ACK_SIMULATED'"
              @click="acknowledgeAlert(selectedAlert.alertId)"
            >
              {{ t('alertCenter.actions.ack') }}
            </el-button>
            <el-button
              :disabled="selectedAlert.ackStatus !== 'ACK_SIMULATED'"
              @click="clearAcknowledgement(selectedAlert.alertId)"
            >
              {{ t('alertCenter.actions.clearAck') }}
            </el-button>
          </div>

          <pre class="code-block">{{ formatJson(selectedAlert.evidence) }}</pre>
        </template>
      </EvidencePanel>
    </div>

    <CapabilityPlaceholderDialog
      v-model="placeholderDialogVisible"
      :title="placeholderPayload.title"
      :capability="placeholderPayload.capability"
      :reason="placeholderPayload.reason"
      :next-step="placeholderPayload.nextStep"
    />
  </section>
</template>

<style scoped>
.alert-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-6);
}

.alert-field {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 220px;
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.summary-grid,
.alert-grid {
  display: grid;
  gap: var(--sqlforge-space-5);
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.alert-grid {
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.9fr);
  align-items: start;
}

.alert-detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: var(--sqlforge-space-3);
}

.alert-detail-item {
  min-width: 0;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.alert-detail-item span,
.alert-detail-item strong {
  display: block;
  min-width: 0;
}

.alert-detail-item span {
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.alert-detail-item strong {
  margin-top: var(--sqlforge-space-2);
  color: var(--sqlforge-text-primary);
  font-size: 16px;
  font-weight: 500;
  line-height: 1.35;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
}

.alert-item {
  width: 100%;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.alert-item-active {
  border-color: rgba(207, 166, 62, 0.32);
}

.alert-item-header,
.pill-row,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
}

.alert-item-header {
  justify-content: space-between;
}

.alert-source,
.alert-item h3,
.alert-summary,
.empty-state {
  margin: 0;
}

.alert-item h3 {
  color: var(--sqlforge-text-primary);
  font-size: 18px;
  font-weight: 400;
}

.alert-summary,
.empty-state {
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.pill-row,
.action-row {
  margin-top: var(--sqlforge-space-4);
}

.status-pill,
.mini-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--sqlforge-space-3);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.status-pill-warn {
  border-color: rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

.result-banner {
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.result-banner-danger {
  border-color: rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

.code-block {
  margin: var(--sqlforge-space-4) 0 0;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .alert-grid {
    grid-template-columns: 1fr;
  }
}
</style>
