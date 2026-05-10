<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { formatRuntimeError, getGovernanceMessageStats, retryGovernanceFailedMessages } from '../../services/runtimeGateApi'
import GovernanceQueueRetryPanel from '../common/GovernanceQueueRetryPanel.vue'
import GovernanceTraceDetailPanel from '../common/GovernanceTraceDetailPanel.vue'
import GovernanceTraceLookupPanel from '../common/GovernanceTraceLookupPanel.vue'
import GovernanceTraceResultList from '../common/GovernanceTraceResultList.vue'
import SectionHeader from '../common/SectionHeader.vue'
import { displayValue, isCompensationTrace } from '../common/governanceTrace'
import { useGovernanceTraceLookup } from '../common/useGovernanceTraceLookup'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const {
  form,
  loadingLookup,
  lookupResults,
  detail,
  errorMessage,
  hasMore,
  nextCursor,
  activeTraceId,
  matchedCount,
  compensationCount,
  reportLinkedCount,
  searchCriteria,
  selectedSummary,
  runLookup,
  loadMoreResults,
  clearLookup: clearTraceLookup,
  loadTraceDetail,
  buildDrillQuery,
  hydrateFromRoute
} = useGovernanceTraceLookup({
  routePath: ROUTE_PATHS.auditTroubleshooting,
  router,
  requestPrefix: 'frontend-audit-troubleshooting',
  requiredMessage: () => t('auditTroubleshooting.messages.requiredLookup')
})

form.remediationTenantId = 'system'

const loading = reactive({
  stats: false,
  retry: false
})
const stats = ref(null)
const statsBeforeRetry = ref(null)
const statsAfterRetry = ref(null)
const retryResult = ref(null)

const failureType = computed(() => {
  if (!detail.value) {
    return '-'
  }
  if (detail.value.serviceCode === 'QUERY_EXECUTION' && detail.value.degraded === true) {
    return 'QUERY_DEGRADED_RECOVERY'
  }
  if (detail.value.serviceCode === 'SQL_OPTIMIZATION' && detail.value.latestStatus === 'FAILED') {
    return 'OPTIMIZATION_FAILURE'
  }
  if (detail.value.serviceCode === 'BENCHMARK_ENGINE' && detail.value.latestStatus === 'FAILED') {
    return 'BENCHMARK_FAILURE'
  }
  if ((detail.value.nonSuccessEventCount || 0) > 0) {
    return 'AUDIT_FAILURE_CHAIN'
  }
  return 'STEADY_STATE'
})

const compensationState = computed(() => {
  if (!detail.value) {
    return '-'
  }
  if (isCompensationTrace(detail.value.traceId)) {
    return 'COMPENSATION_TRACE'
  }
  if (compensationCount.value > 0) {
    return 'COMPENSATED_VISIBLE'
  }
  return 'NOT_VISIBLE'
})

const writeBackState = computed(() => {
  if (!detail.value) {
    return '-'
  }
  if ((detail.value.exportRecordCount || 0) > 0 || displayValue(detail.value.reportId) !== '-') {
    return 'WRITEBACK_VISIBLE'
  }
  return 'WRITEBACK_PENDING'
})

const queueImpact = computed(() => {
  if (!stats.value) {
    return '-'
  }
  if ((stats.value.failed || 0) > 0) {
    return 'FAILED_BACKLOG'
  }
  if ((stats.value.pending || 0) > 0) {
    return 'PENDING_BACKLOG'
  }
  return 'STEADY_QUEUE'
})

const failedDelta = computed(() => {
  if (!statsBeforeRetry.value || !statsAfterRetry.value) {
    return 0
  }
  return (statsBeforeRetry.value.failed || 0) - (statsAfterRetry.value.failed || 0)
})

const retryImproved = computed(() => failedDelta.value >= 1 || (retryResult.value?.retriedCount || 0) >= 1)

const acceptanceState = computed(() => {
  if (retryResult.value) {
    return retryImproved.value ? 'REPAIRED' : 'RETRY_ACCEPTED'
  }
  if (compensationState.value === 'COMPENSATION_TRACE' || writeBackState.value === 'WRITEBACK_VISIBLE') {
    return 'READY_FOR_ACCEPTANCE'
  }
  if (queueImpact.value === 'FAILED_BACKLOG') {
    return 'REMEDIATION_REQUIRED'
  }
  return 'UNDER_INVESTIGATION'
})

const queueCards = computed(() => {
  if (!stats.value) {
    return []
  }
  return [
    { key: 'total', label: t('governanceTrace.queueTotal'), value: stats.value.total },
    { key: 'pending', label: t('governanceTrace.queuePending'), value: stats.value.pending },
    { key: 'failed', label: t('governanceTrace.queueFailed'), value: stats.value.failed },
    { key: 'consumed', label: t('governanceTrace.queueConsumed'), value: stats.value.consumed }
  ]
})

const summaryCards = computed(() => [
  { key: 'match-count', label: t('governanceTrace.matchedTraces'), value: matchedCount.value, testId: 'audit-troubleshooting-match-count' },
  { key: 'compensation-count', label: t('governanceTrace.compensationTraces'), value: compensationCount.value, testId: 'audit-troubleshooting-compensation-count', tone: 'warning' },
  { key: 'report-count', label: t('governanceTrace.reportLinkedTraces'), value: reportLinkedCount.value, testId: 'audit-troubleshooting-report-count' }
])

const decisionCards = computed(() => [
  { key: 'failure-type', label: t('auditTroubleshooting.decision.failureType'), value: failureType.value },
  { key: 'compensation-state', label: t('auditTroubleshooting.decision.compensationState'), value: compensationState.value },
  { key: 'writeback-state', label: t('auditTroubleshooting.decision.writeBackState'), value: writeBackState.value },
  { key: 'queue-impact', label: t('auditTroubleshooting.decision.queueImpact'), value: queueImpact.value },
  { key: 'acceptance-state', label: t('auditTroubleshooting.decision.acceptanceState'), value: acceptanceState.value }
])

const detailHighlights = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    { key: 'taskId', label: t('governanceTrace.taskId'), value: detail.value.taskId },
    { key: 'reportId', label: t('governanceTrace.reportId'), value: detail.value.reportId },
    { key: 'lastSeenAt', label: t('governanceTrace.lastSeenAt'), value: detail.value.lastSeenAt }
  ].filter(item => displayValue(item.value) !== '-')
})

const eventHighlights = event => {
  const request = event?.requestParams || {}
  const response = event?.responseSummary || {}
  return [
    { label: t('governanceTrace.task'), value: response.taskId || request.taskId },
    { label: t('governanceTrace.report'), value: response.reportId || request.reportId },
    { label: t('governanceTrace.error'), value: response.errorCode },
    { label: t('governanceTrace.engine'), value: response.targetEngine }
  ].filter(item => displayValue(item.value) !== '-')
}

const loadQueueStats = async () => {
  loading.stats = true
  try {
    stats.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.stats = false
  }
}

const runDecisionLookup = async () => {
  const didLookup = await runLookup({ remediationTenantId: form.remediationTenantId })
  if (didLookup) {
    await loadQueueStats()
  }
}

const retryFailedMessages = async () => {
  loading.retry = true
  errorMessage.value = ''
  retryResult.value = null

  try {
    statsBeforeRetry.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats-before-retry'
    })
    retryResult.value = await retryGovernanceFailedMessages(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-retry'
    })
    statsAfterRetry.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats-after-retry'
    })
    stats.value = statsAfterRetry.value
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.retry = false
  }
}

const clearLookup = async () => {
  clearTraceLookup({ remediationTenantId: form.remediationTenantId })
  retryResult.value = null
  statsBeforeRetry.value = null
  statsAfterRetry.value = null
  await loadQueueStats()
}

const openSystemBacklog = () => {
  router.push(ROUTE_PATHS.system)
}

const openRepairEvidence = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.repairEvidence,
    query: buildDrillQuery(source, { remediationTenantId: form.remediationTenantId })
  })
}

const openParseRecord = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: buildDrillQuery(source, { remediationTenantId: form.remediationTenantId })
  })
}

const updateLookupField = ({ field, value }) => {
  form[field] = value
}

onMounted(async () => {
  hydrateFromRoute(route)
  if (route.query.remediationTenantId) {
    form.remediationTenantId = String(route.query.remediationTenantId)
  }
  await loadQueueStats()
  if (form.traceId || form.taskId || form.reportId) {
    await runDecisionLookup()
  }
})
</script>

<template>
  <section class="ops-page" data-testid="audit-troubleshooting-page">
    <SectionHeader
      eyebrow="audit troubleshooting"
      :title="t('auditTroubleshooting.title')"
      :summary="t('auditTroubleshooting.summary')"
    />
    <p class="ops-note">{{ t('auditTroubleshooting.refactorNote') }}</p>

    <div class="ops-grid">
      <div class="ops-column">
        <GovernanceTraceLookupPanel
          :form="form"
          prefix="audit-troubleshooting"
          eyebrow="decision lookup"
          title-key="auditTroubleshooting.lookupTitle"
          summary-key="auditTroubleshooting.lookupSummary"
          empty-criteria-key="auditTroubleshooting.messages.emptyCriteria"
          run-label-key="auditTroubleshooting.actions.runLookup"
          include-remediation-tenant
          show-refresh-queue
          :search-criteria="searchCriteria"
          :loading-lookup="loadingLookup"
          :loading-stats="loading.stats"
          :has-more="hasMore"
          @run="runDecisionLookup"
          @clear="clearLookup"
          @load-more="loadMoreResults"
          @refresh-queue="loadQueueStats"
          @update-field="updateLookupField"
        />

        <div class="summary-card-grid">
          <article
            v-for="card in summaryCards"
            :key="card.key"
            class="summary-card"
            :class="{ 'summary-card-warning': card.tone === 'warning' }"
          >
            <span>{{ card.label }}</span>
            <strong :data-testid="card.testId">{{ card.value }}</strong>
          </article>
        </div>

        <GovernanceQueueRetryPanel
          prefix="audit-troubleshooting"
          :queue-cards="queueCards"
          :retry-result="retryResult"
          :failed-delta="failedDelta"
          :retry-improved="retryImproved"
        />

        <div v-if="hasMore" class="result-banner result-banner-warning" data-testid="audit-troubleshooting-has-more">
          <strong>{{ t('governanceTrace.olderRemediationAvailable') }}</strong>
          <span>{{ nextCursor || '-' }}</span>
        </div>

        <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="audit-troubleshooting-error">
          {{ errorMessage }}
        </div>

        <p v-else-if="!lookupResults.length" class="empty-state">
          {{ t('auditTroubleshooting.messages.noMatches') }}
        </p>

        <GovernanceTraceResultList
          :traces="lookupResults"
          :active-trace-id="activeTraceId"
          prefix="audit-troubleshooting"
          @select="loadTraceDetail"
        />
      </div>

      <GovernanceTraceDetailPanel
        :detail="detail"
        prefix="audit-troubleshooting"
        eyebrow="remediation decision"
        title-key="auditTroubleshooting.detailTitle"
        empty-key="auditTroubleshooting.messages.emptyDetail"
        :highlight-items="detailHighlights"
        :signal-items="decisionCards"
        :event-highlight-fn="eventHighlights"
        :banner-text="acceptanceState"
        :banner-success="acceptanceState === 'REPAIRED' || acceptanceState === 'READY_FOR_ACCEPTANCE'"
      >
        <template #actions>
          <el-button type="primary" :loading="loading.retry" data-testid="audit-troubleshooting-retry" @click="retryFailedMessages">
            {{ t('auditTroubleshooting.actions.retryFailedMessages') }}
          </el-button>
          <el-button data-testid="audit-troubleshooting-open-system" @click="openSystemBacklog">
            {{ t('auditTroubleshooting.actions.openSystem') }}
          </el-button>
          <el-button data-testid="audit-troubleshooting-open-repair-evidence" @click="openRepairEvidence">
            {{ t('auditTroubleshooting.actions.openRepairEvidence') }}
          </el-button>
          <el-button data-testid="audit-troubleshooting-open-parse-record" @click="openParseRecord">
            {{ t('auditTroubleshooting.actions.openParseRecord') }}
          </el-button>
        </template>
      </GovernanceTraceDetailPanel>
    </div>
  </section>
</template>

<style scoped>
.ops-page,
.ops-column {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-6);
}

.ops-note {
  margin: 0;
  max-width: 980px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.65;
}

.ops-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(360px, 0.94fr);
  gap: var(--sqlforge-space-6);
  align-items: start;
}

.summary-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--sqlforge-space-3);
}

.summary-card {
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-2);
}

.summary-card-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.summary-card span,
.empty-state {
  color: var(--sqlforge-text-secondary);
}

.summary-card span {
  display: block;
  margin-bottom: var(--sqlforge-space-2);
  font-size: 12px;
}

.result-banner {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  justify-content: space-between;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.result-banner-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.result-banner-danger {
  border-color: rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

.empty-state {
  margin: 0;
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .ops-grid {
    grid-template-columns: 1fr;
  }
}
</style>
