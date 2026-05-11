<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'
import {
  ackGovernanceAlert,
  formatRuntimeError,
  getGovernanceAlertDetail,
  getGovernanceAlerts
} from '../../services/runtimeGateApi'

const backendReadPath = 'GET /api/governance/alerts'
const notificationBoundaryText = 'SIMULATED_EMAIL / SIMULATED_SENT / DEDUPE_SUPPRESSED'
const alertStatusOptions = ['', 'OPEN', 'ACKED']
const notifyStatusOptions = ['', 'SIMULATED_PENDING_NOTIFY', 'SIMULATED_NOTIFIED', 'SIMULATED_NOTIFY_FAILED']
const alertTypeOptions = [
  '',
  'SQL_REWRITE_RESULT_DIVERGENCE',
  'BENCHMARK_REGRESSION_FAILED',
  'DISPATCH_COORDINATION_FAILED',
  'AUDIT_WRITE_EXCEPTION',
  'DATASOURCE_UNAVAILABLE',
  'DEPENDENCY_SERVICE_UNAVAILABLE'
]

const { t } = useI18n()
const route = useRoute()

const form = reactive({
  tenantId: 'tenant-a',
  alertStatus: '',
  alertType: '',
  notifyStatus: '',
  pageNo: 1,
  pageSize: 8
})

const loading = reactive({
  page: false,
  detail: false,
  ack: false
})

const alertPage = ref({
  items: [],
  pageNo: 1,
  pageSize: 8,
  total: 0,
  hasNext: false
})
const selectedAlertId = ref('')
const selectedAlertDetail = ref(null)
const errorMessage = ref('')
const detailErrorMessage = ref('')
const placeholderDialogVisible = ref(false)
const placeholderPayload = ref({
  title: '',
  capability: '',
  reason: '',
  nextStep: ''
})

const alertRows = computed(() => alertPage.value.items || [])

const alertSummaryCards = computed(() => [
  {
    key: 'total',
    label: t('alertCenter.metrics.total'),
    value: alertPage.value.total ?? alertRows.value.length
  },
  {
    key: 'open',
    label: t('alertCenter.metrics.open'),
    value: alertRows.value.filter(item => item.alertStatus === 'OPEN').length,
    tone: 'warning'
  },
  {
    key: 'high',
    label: t('alertCenter.metrics.high'),
    value: alertRows.value.filter(item => ['HIGH', 'CRITICAL'].includes(item.alertLevel)).length,
    tone: 'danger'
  },
  {
    key: 'simulated',
    label: t('alertCenter.metrics.simulated'),
    value: alertRows.value.filter(item => String(item.notifyStatus || '').startsWith('SIMULATED')).length
  }
])

const selectedAlert = computed(() =>
  selectedAlertDetail.value || alertRows.value.find(item => item.alertId === selectedAlertId.value) || null
)

const selectedNotificationLogs = computed(() => selectedAlert.value?.notificationLogs || [])

const routeContextRows = computed(() =>
  ['recommendationId', 'historyId', 'rewriteRecordId', 'validationRunId', 'sqlFingerprint']
    .map(key => field(key, t(`alertCenter.fields.${key}`), route.query[key]))
    .filter(item => item.value !== '-')
)

const autoApplyPausedText = computed(() =>
  boolText(firstDefined(
    selectedAlert.value?.evidence?.autoApplyPaused,
    selectedAlert.value?.evidence?.validationRun?.autoApplyPaused,
    selectedAlert.value?.evidence?.rewriteRecord?.autoApplyPaused,
    selectedAlert.value?.evidence?.autoApplyPauseEvidence?.autoApplyPaused
  ))
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

const refreshAlerts = async ({ preserveSelection = false } = {}) => {
  loading.page = true
  errorMessage.value = ''
  try {
    const response = await getGovernanceAlerts(form.tenantId, {
      alertStatus: form.alertStatus,
      alertType: form.alertType,
      notifyStatus: form.notifyStatus,
      pageNo: form.pageNo,
      pageSize: form.pageSize
    }, {
      requestPrefix: 'frontend-alert-center-alerts'
    })
    alertPage.value = normalizeAlertPage(response)

    const requestedId = preserveSelection ? selectedAlertId.value : String(route.query.alertId || '')
    const nextSelectedId = requestedId || alertRows.value[0]?.alertId || ''
    if (nextSelectedId) {
      selectedAlertId.value = nextSelectedId
      await loadAlertDetail(nextSelectedId)
    } else {
      selectedAlertId.value = ''
      selectedAlertDetail.value = null
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadAlertDetail = async alertId => {
  if (!alertId) {
    return
  }
  loading.detail = true
  detailErrorMessage.value = ''
  try {
    selectedAlertDetail.value = await getGovernanceAlertDetail(form.tenantId, alertId, {
      requestPrefix: 'frontend-alert-center-detail'
    })
  } catch (error) {
    detailErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const selectAlert = item => {
  selectedAlertId.value = item.alertId
  selectedAlertDetail.value = null
  loadAlertDetail(item.alertId)
}

const acknowledgeAlert = async () => {
  if (!selectedAlertId.value || selectedAlert.value?.alertStatus !== 'OPEN') {
    return
  }
  loading.ack = true
  detailErrorMessage.value = ''
  try {
    const response = await ackGovernanceAlert(form.tenantId, selectedAlertId.value, {
      requestPrefix: 'frontend-alert-center-ack'
    })
    selectedAlertDetail.value = response
    alertPage.value = {
      ...alertPage.value,
      items: alertRows.value.map(item => item.alertId === response.alertId ? { ...item, ...response } : item)
    }
  } catch (error) {
    detailErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.ack = false
  }
}

const handlePageChange = pageNo => {
  form.pageNo = pageNo
  refreshAlerts()
}

const handlePageSizeChange = pageSize => {
  form.pageSize = pageSize
  form.pageNo = 1
  refreshAlerts()
}

const applyRouteQuery = () => {
  form.tenantId = String(route.query.tenantId || form.tenantId)
  form.alertStatus = String(route.query.alertStatus || form.alertStatus)
  form.alertType = String(route.query.alertType || form.alertType)
  form.notifyStatus = String(route.query.notifyStatus || form.notifyStatus)
  selectedAlertId.value = String(route.query.alertId || '')
}

const formatJson = value => JSON.stringify(value, null, 2)
const formatTimestamp = value => displayValue(value)

const normalizeAlertPage = response => ({
  items: Array.isArray(response?.items) ? response.items : [],
  pageNo: response?.pageNo || form.pageNo,
  pageSize: response?.pageSize || form.pageSize,
  total: response?.total ?? (Array.isArray(response?.items) ? response.items.length : 0),
  hasNext: Boolean(response?.hasNext)
})

const statusClass = value => ({
  'status-pill': true,
  'status-pill-ok': value === 'ACKED',
  'status-pill-warn': value === 'OPEN',
  'status-pill-danger': ['CRITICAL', 'HIGH', 'SIMULATED_NOTIFY_FAILED'].includes(value)
})

function field(key, label, value) {
  return { key, label, value: displayValue(value) }
}

function displayValue(value) {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '-'
  }
  return String(value)
}

function boolText(value) {
  if (value === true) {
    return 'true'
  }
  if (value === false) {
    return 'false'
  }
  return '-'
}

function firstDefined(...values) {
  return values.find(value => value !== null && value !== undefined)
}

watch(
  () => [form.tenantId, form.alertStatus, form.alertType, form.notifyStatus],
  () => {
    selectedAlertId.value = ''
    selectedAlertDetail.value = null
    form.pageNo = 1
  }
)

onMounted(() => {
  applyRouteQuery()
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
      <label class="alert-field">
        <span>{{ t('alertCenter.fields.alertStatus') }}</span>
        <el-select v-model="form.alertStatus" data-testid="alert-status-filter">
          <el-option
            v-for="item in alertStatusOptions"
            :key="item || 'all-status'"
            :label="item || t('alertCenter.options.all')"
            :value="item"
          />
        </el-select>
      </label>
      <label class="alert-field">
        <span>{{ t('alertCenter.fields.alertType') }}</span>
        <el-select v-model="form.alertType" filterable data-testid="alert-type-filter">
          <el-option
            v-for="item in alertTypeOptions"
            :key="item || 'all-type'"
            :label="item || t('alertCenter.options.all')"
            :value="item"
          />
        </el-select>
      </label>
      <label class="alert-field">
        <span>{{ t('alertCenter.fields.notifyStatus') }}</span>
        <el-select v-model="form.notifyStatus" data-testid="alert-notify-status-filter">
          <el-option
            v-for="item in notifyStatusOptions"
            :key="item || 'all-notify'"
            :label="item || t('alertCenter.options.all')"
            :value="item"
          />
        </el-select>
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

    <section v-if="routeContextRows.length" class="context-panel" data-testid="alert-linkage-context">
      <div v-for="item in routeContextRows" :key="item.key" class="context-cell">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </section>

    <div class="alert-grid">
      <EvidencePanel eyebrow="backend alerts" :title="t('alertCenter.listTitle')" tone="warning">
        <div class="alert-list">
          <button
            v-for="item in alertRows"
            :key="item.alertId"
            type="button"
            class="alert-item"
            :class="{ 'alert-item-active': selectedAlertId === item.alertId }"
            data-testid="alert-item"
            @click="selectAlert(item)"
          >
            <div class="alert-item-header">
              <div>
                <p class="alert-source sqlforge-code-label">{{ item.sourceService || '-' }}</p>
                <h3>{{ item.alertType }}</h3>
              </div>
              <span :class="statusClass(item.alertLevel)">
                {{ item.alertLevel }}
              </span>
            </div>
            <p class="alert-summary">{{ item.summary }}</p>
            <div class="pill-row">
              <span class="mini-pill">{{ item.alertStatus }}</span>
              <span class="mini-pill">{{ item.notifyStatus }}</span>
              <span class="mini-pill">{{ formatTimestamp(item.createdAt) }}</span>
            </div>
          </button>
          <el-empty v-if="!alertRows.length" :description="t('alertCenter.messages.noAlert')" />
        </div>
        <el-pagination
          class="alert-pagination"
          background
          layout="total, sizes, prev, pager, next"
          :current-page="alertPage.pageNo"
          :page-size="alertPage.pageSize"
          :page-sizes="[8, 16, 32]"
          :total="alertPage.total"
          data-testid="alert-pagination"
          @current-change="handlePageChange"
          @size-change="handlePageSizeChange"
        />
      </EvidencePanel>

      <EvidencePanel data-testid="alert-detail" eyebrow="alert detail" :title="t('alertCenter.detailTitle')">
        <p v-if="detailErrorMessage" class="error-banner" data-testid="alert-detail-error">{{ detailErrorMessage }}</p>
        <p v-if="!selectedAlert" class="empty-state">
          {{ t('alertCenter.messages.noAlert') }}
        </p>

        <template v-else>
          <div v-loading="loading.detail" class="alert-detail-grid">
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.alertId') }}</span>
              <strong>{{ selectedAlert.alertId }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.alertType') }}</span>
              <strong>{{ selectedAlert.alertType }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.alertLevel') }}</span>
              <strong>{{ selectedAlert.alertLevel }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.alertStatus') }}</span>
              <strong data-testid="alert-ack-status">{{ selectedAlert.alertStatus }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.notifyStatus') }}</span>
              <strong data-testid="alert-notify-status">{{ selectedAlert.notifyStatus }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.notifyBoundary') }}</span>
              <strong data-testid="alert-notify-boundary">{{ notificationBoundaryText }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.sourceService') }}</span>
              <strong>{{ displayValue(selectedAlert.sourceService) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.policyId') }}</span>
              <strong>{{ displayValue(selectedAlert.policyId) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.dedupeKey') }}</span>
              <strong>{{ displayValue(selectedAlert.dedupeKey) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.recommendationId') }}</span>
              <strong>{{ displayValue(selectedAlert.recommendationId) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.historyId') }}</span>
              <strong>{{ displayValue(selectedAlert.historyId) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.sqlFingerprint') }}</span>
              <strong>{{ displayValue(selectedAlert.sqlFingerprint) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.autoApplyPaused') }}</span>
              <strong data-testid="alert-auto-apply-paused">{{ autoApplyPausedText }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.notifiedAt') }}</span>
              <strong>{{ formatTimestamp(selectedAlert.notifiedAt) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.ackedBy') }}</span>
              <strong>{{ displayValue(selectedAlert.ackedBy) }}</strong>
            </div>
            <div class="alert-detail-item">
              <span>{{ t('alertCenter.fields.ackedAt') }}</span>
              <strong>{{ formatTimestamp(selectedAlert.ackedAt) }}</strong>
            </div>
          </div>

          <div class="action-row">
            <el-button
              type="primary"
              :loading="loading.ack"
              data-testid="alert-ack"
              :disabled="selectedAlert.alertStatus !== 'OPEN'"
              @click="acknowledgeAlert"
            >
              {{ t('alertCenter.actions.ack') }}
            </el-button>
          </div>

          <section class="notification-section" data-testid="alert-notification-log">
            <div class="alert-section-heading">
              <h3>{{ t('alertCenter.sections.notificationLogs') }}</h3>
              <span class="mini-pill">{{ selectedNotificationLogs.length }}</span>
            </div>
            <article v-for="item in selectedNotificationLogs" :key="item.notificationLogId" class="notification-log-row">
              <div class="pill-row">
                <span class="mini-pill">{{ item.deliveryStatus }}</span>
                <span class="mini-pill">{{ item.notifyChannel }}</span>
                <span class="mini-pill">{{ item.templateCode }}</span>
              </div>
              <p>{{ displayValue(item.deliverySummary || item.messageSubject || item.messageBody) }}</p>
              <pre class="code-block code-block-compact">{{ formatJson(item.payload || {}) }}</pre>
            </article>
            <el-empty v-if="!selectedNotificationLogs.length" :description="t('alertCenter.messages.noNotificationLog')" />
          </section>

          <section class="notification-section">
            <div class="alert-section-heading">
              <h3>{{ t('alertCenter.sections.evidence') }}</h3>
              <span class="mini-pill">autoApplyPaused {{ autoApplyPausedText }}</span>
            </div>
            <pre class="code-block" data-testid="alert-evidence">{{ formatJson(selectedAlert.evidence || {}) }}</pre>
          </section>
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
.alert-grid,
.context-panel {
  display: grid;
  gap: var(--sqlforge-space-5);
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.context-panel {
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-1);
}

.context-cell {
  min-width: 0;
}

.context-cell span,
.context-cell strong {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.context-cell span {
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.context-cell strong {
  margin-top: var(--sqlforge-space-2);
  color: var(--sqlforge-text-primary);
  font-size: 14px;
  font-weight: 500;
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

.alert-pagination {
  margin-top: var(--sqlforge-space-4);
}

.alert-item-header,
.alert-section-heading,
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
.alert-section-heading h3,
.alert-item h3,
.notification-log-row p,
.alert-summary,
.empty-state {
  margin: 0;
}

.alert-section-heading {
  justify-content: space-between;
}

.alert-section-heading h3 {
  color: var(--sqlforge-text-primary);
  font-size: 16px;
  font-weight: 500;
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

.status-pill-danger {
  border-color: rgba(212, 96, 96, 0.45);
  color: #ffd6d6;
}

.status-pill-ok {
  border-color: rgba(62, 207, 142, 0.38);
  color: #bdf7d6;
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

.error-banner {
  margin: 0 0 var(--sqlforge-space-4);
  padding: var(--sqlforge-space-3);
  border: 1px solid rgba(212, 96, 96, 0.35);
  border-radius: var(--sqlforge-radius-md);
  color: #ffd6d6;
}

.notification-section,
.notification-log-row {
  margin-top: var(--sqlforge-space-4);
}

.notification-log-row {
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.notification-log-row p {
  margin-top: var(--sqlforge-space-3);
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
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

.code-block-compact {
  margin-top: var(--sqlforge-space-3);
  max-height: 180px;
}

@media (max-width: 1100px) {
  .alert-grid {
    grid-template-columns: 1fr;
  }
}
</style>
