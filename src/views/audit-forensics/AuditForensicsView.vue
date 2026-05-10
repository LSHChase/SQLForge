<script setup>
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import GovernanceTraceDetailPanel from '../common/GovernanceTraceDetailPanel.vue'
import GovernanceTraceLookupPanel from '../common/GovernanceTraceLookupPanel.vue'
import GovernanceTraceResultList from '../common/GovernanceTraceResultList.vue'
import SectionHeader from '../common/SectionHeader.vue'
import {
  displayValue,
  hasDisplayValue,
  isCompensationTrace,
  resolveTraceRepairSignal
} from '../common/governanceTrace'
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
  repairSignalCount,
  searchCriteria,
  selectedSummary,
  runLookup,
  loadMoreResults,
  clearLookup,
  loadTraceDetail,
  buildDrillQuery,
  hydrateFromRoute
} = useGovernanceTraceLookup({
  routePath: ROUTE_PATHS.auditForensics,
  router,
  requestPrefix: 'frontend-audit-forensics',
  requiredMessage: () => t('auditForensics.messages.requiredLookup')
})

const summaryCards = computed(() => [
  { key: 'match-count', label: t('governanceTrace.matchedTraces'), value: matchedCount.value, testId: 'audit-forensics-match-count' },
  { key: 'compensation-count', label: t('governanceTrace.compensationTraces'), value: compensationCount.value, testId: 'audit-forensics-compensation-count', tone: 'warning' },
  { key: 'repair-count', label: t('governanceTrace.repairSignalChains'), value: repairSignalCount.value, testId: 'audit-forensics-repair-count' },
  { key: 'report-count', label: t('governanceTrace.reportLinkedTraces'), value: reportLinkedCount.value, testId: 'audit-forensics-report-count' }
])

const selectedSignals = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    { key: 'lookupMode', label: t('governanceTrace.lookupMode'), value: resolveLookupMode(detail.value) },
    { key: 'repairSignal', label: t('governanceTrace.repairSignal'), value: resolveTraceRepairSignal(detail.value) },
    {
      key: 'compensationTrace',
      label: t('governanceTrace.compensationTrace'),
      value: isCompensationTrace(detail.value.traceId) ? 'true' : 'false'
    },
    { key: 'auditEvents', label: t('governanceTrace.auditEvents'), value: detail.value.auditEventCount }
  ].filter(item => displayValue(item.value) !== '-')
})

const detailHighlights = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    { key: 'taskId', label: t('governanceTrace.taskId'), value: detail.value.taskId },
    { key: 'reportId', label: t('governanceTrace.reportId'), value: detail.value.reportId },
    { key: 'sqlFingerprint', label: t('governanceTrace.sqlFingerprint'), value: detail.value.sqlFingerprint },
    { key: 'errorCode', label: t('governanceTrace.errorCode'), value: detail.value.errorCode },
    { key: 'targetEngine', label: t('governanceTrace.targetEngine'), value: detail.value.targetEngine },
    {
      key: 'degraded',
      label: t('governanceTrace.degraded'),
      value: typeof detail.value.degraded === 'boolean' ? String(detail.value.degraded) : ''
    }
  ].filter(item => displayValue(item.value) !== '-')
})

const resolveLookupMode = currentDetail => {
  if (hasDisplayValue(form.traceId) && form.traceId.trim() === currentDetail.traceId) {
    return 'TRACE'
  }
  if (hasDisplayValue(form.taskId) && form.taskId.trim() === currentDetail.taskId) {
    return 'TASK'
  }
  if (hasDisplayValue(form.reportId) && form.reportId.trim() === currentDetail.reportId) {
    return 'REPORT'
  }
  if (selectedSummary.value?.taskId && selectedSummary.value.taskId === currentDetail.taskId) {
    return 'TASK'
  }
  if (selectedSummary.value?.reportId && selectedSummary.value.reportId === currentDetail.reportId) {
    return 'REPORT'
  }
  return 'TRACE'
}

const eventHighlights = event => {
  const request = event?.requestParams || {}
  const response = event?.responseSummary || {}
  return [
    { label: t('governanceTrace.task'), value: response.taskId || request.taskId },
    { label: t('governanceTrace.report'), value: response.reportId || request.reportId },
    { label: t('governanceTrace.fingerprint'), value: request.sqlFingerprint },
    { label: t('governanceTrace.error'), value: response.errorCode },
    { label: t('governanceTrace.engine'), value: response.targetEngine }
  ].filter(item => displayValue(item.value) !== '-')
}

const openParseRecord = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: buildDrillQuery(source)
  })
}

const openRepairEvidence = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.repairEvidence,
    query: buildDrillQuery(source)
  })
}

const openTroubleshooting = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: ROUTE_PATHS.auditTroubleshooting,
    query: buildDrillQuery(source, { remediationTenantId: 'system' })
  })
}

const updateLookupField = ({ field, value }) => {
  form[field] = value
}

onMounted(async () => {
  hydrateFromRoute(route)
  if (form.traceId || form.taskId || form.reportId) {
    await runLookup()
  }
})
</script>

<template>
  <section class="ops-page" data-testid="audit-forensics-page">
    <SectionHeader
      eyebrow="audit forensics"
      :title="t('auditForensics.title')"
      :summary="t('auditForensics.summary')"
    />
    <p class="ops-note">{{ t('auditForensics.refactorNote') }}</p>

    <div class="ops-grid">
      <div class="ops-column">
        <GovernanceTraceLookupPanel
          :form="form"
          prefix="audit-forensics"
          eyebrow="forensic lookup"
          title-key="auditForensics.lookupTitle"
          summary-key="auditForensics.lookupSummary"
          empty-criteria-key="auditForensics.messages.emptyCriteria"
          run-label-key="auditForensics.actions.runLookup"
          :search-criteria="searchCriteria"
          :loading-lookup="loadingLookup"
          :has-more="hasMore"
          @run="runLookup"
          @clear="clearLookup"
          @load-more="loadMoreResults"
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

        <div v-if="hasMore" class="result-banner result-banner-warning" data-testid="audit-forensics-has-more">
          <strong>{{ t('governanceTrace.olderEvidenceAvailable') }}</strong>
          <span>{{ nextCursor || '-' }}</span>
        </div>

        <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="audit-forensics-error">
          {{ errorMessage }}
        </div>

        <p v-else-if="!lookupResults.length" class="empty-state">
          {{ t('auditForensics.messages.noMatches') }}
        </p>

        <GovernanceTraceResultList
          :traces="lookupResults"
          :active-trace-id="activeTraceId"
          prefix="audit-forensics"
          @select="loadTraceDetail"
        />
      </div>

      <GovernanceTraceDetailPanel
        :detail="detail"
        prefix="audit-forensics"
        eyebrow="forensic pivots"
        title-key="auditForensics.detailTitle"
        empty-key="auditForensics.messages.emptyDetail"
        :highlight-items="detailHighlights"
        :signal-items="selectedSignals"
        :event-highlight-fn="eventHighlights"
      >
        <template #actions>
          <el-button type="primary" data-testid="audit-forensics-open-parse-record" @click="openParseRecord">
            {{ t('auditForensics.actions.openParseRecord') }}
          </el-button>
          <el-button data-testid="audit-forensics-open-repair-evidence" @click="openRepairEvidence">
            {{ t('auditForensics.actions.openRepairEvidence') }}
          </el-button>
          <el-button data-testid="audit-forensics-open-troubleshooting" @click="openTroubleshooting">
            {{ t('auditForensics.actions.openTroubleshooting') }}
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
