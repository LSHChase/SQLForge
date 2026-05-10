<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'
import {
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceTraceDetail,
  getGovernanceTraceSummaries,
  getHetuRouteCalibration
} from '../../services/runtimeGateApi'

const { t } = useI18n()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a',
  traceLimit: 10
})

const loading = reactive({
  page: false,
  detail: false
})

const errorMessage = ref('')
const routeCalibration = ref(null)
const recentTraces = ref([])
const traceDetail = ref(null)
const historyDetail = ref(null)
const detailDialogVisible = ref(false)
const rawDrawerVisible = ref(false)
const placeholderDialogVisible = ref(false)
const activeTab = ref('recentTraces')
const placeholderPayload = ref({
  title: '',
  capability: '',
  reason: '',
  nextStep: ''
})

// Static contract tokens: routing execution evidence, Open parse-record page, View current policy source, Create rule, Edit rule.

const policyCards = computed(() => {
  const calibration = routeCalibration.value
  if (!calibration) {
    return []
  }
  return [
    field('routeProfile', 'Route profile', calibration.routeProfile),
    field('effectiveRouteOrder', 'effectiveRouteOrder', listText(calibration.effectiveRouteOrder)),
    field('declaredAllowedModes', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text001'), listText(calibration.declaredAllowedModes)),
    field('readonlyBoundary', 'readonlyBoundary', calibration.readonlyBoundary),
    field('liveVerificationStatus', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text002'), calibration.liveVerificationStatus),
    field('implementationStage', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text003'), calibration.implementationStage)
  ]
})
const commentProtocolCards = computed(() => [
  {
    key: 'engineHint',
    title: 'engineHint',
    example: '/* engineHint=HETU */',
    summary: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text004')
  },
  {
    key: 'priority',
    title: 'priority',
    example: '/* priority=HIGH */',
    summary: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text005')
  },
  {
    key: 'readonlyBoundary',
    title: 'readonlyBoundary',
    example: 'REPO_CLOSED_DEFAULT',
    summary: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text006')
  }
])
const decisionSummaryCards = computed(() => {
  const routeDecision = historyDetail.value?.routeDecision || {}
  const executionSummary = historyDetail.value?.executionSummary || {}
  return [
    field('selectedEngine', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text007'), pick(routeDecision, ['selectedEngine', 'targetEngine', 'engine'])),
    field('ruleId', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text008'), pick(routeDecision, ['ruleId', 'routeDecisionId', 'decisionId'])),
    field('routeProfile', 'Route profile', pick(routeDecision, ['routeProfile'])),
    field('routeOrder', 'Route order', listText(pick(routeDecision, ['routeOrder', 'attemptedModes']))),
    field('verification', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text009'), pick(routeDecision, ['verificationStatus', 'routeVerificationStatus'])),
    field('fallback', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text010'), pick(routeDecision, ['fallbackReason', 'degradeReason'])),
    field('executionMode', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text011'), executionSummary.executionMode),
    field('attemptedModes', t('inline.viewsRoutingGovernanceRoutingGovernanceView.text012'), listText(executionSummary.attemptedModes))
  ].filter(item => displayValue(item.value) !== '-')
})
const traceHistoryRows = computed(() => traceDetail.value?.queryHistories || [])
const recommendationRefs = computed(() => historyDetail.value?.recommendationRefs || [])
const routeSignalGroups = computed(() =>
  [
    { key: 'routeDecision', title: 'routeDecision', payload: historyDetail.value?.routeDecision },
    { key: 'commentContext', title: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text013'), payload: historyDetail.value?.commentContext },
    { key: 'executionSummary', title: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text014'), payload: historyDetail.value?.executionSummary },
    { key: 'queryContext', title: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text015'), payload: historyDetail.value?.queryContext }
  ].filter(group => isNonEmpty(group.payload))
)

const refreshPage = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    const [calibration, traces] = await Promise.all([
      getHetuRouteCalibration(form.tenantId, {
        requestPrefix: 'frontend-routing-governance-calibration'
      }),
      getGovernanceTraceSummaries(form.tenantId, form.traceLimit, {
        requestPrefix: 'frontend-routing-governance-traces'
      })
    ])
    routeCalibration.value = calibration
    recentTraces.value = Array.isArray(traces) ? traces : []
  } catch (error) {
    routeCalibration.value = null
    recentTraces.value = []
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const openPlaceholderAction = actionType => {
  const config = actionType === 'create'
    ? {
        title: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text016'),
        capability: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text017'),
        reason: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text018'),
        nextStep: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text019')
      }
    : {
        title: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text020'),
        capability: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text021'),
        reason: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text022'),
        nextStep: t('inline.viewsRoutingGovernanceRoutingGovernanceView.text023')
      }
  placeholderPayload.value = config
  placeholderDialogVisible.value = true
}

const openDecisionDetail = async traceId => {
  if (!traceId) {
    return
  }
  loading.detail = true
  errorMessage.value = ''
  try {
    traceDetail.value = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-routing-governance-trace-detail'
    })
    const firstHistoryId = traceDetail.value?.queryHistories?.[0]?.historyId || ''
    historyDetail.value = firstHistoryId
      ? await getGovernanceQueryHistoryDetail(form.tenantId, firstHistoryId, {
          requestPrefix: 'frontend-routing-governance-history-detail'
        })
      : null
    detailDialogVisible.value = true
  } catch (error) {
    traceDetail.value = null
    historyDetail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const openEvidenceDetail = () => {
  rawDrawerVisible.value = true
}

const openParseRecord = () => {
  if (!traceDetail.value?.traceId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.sqlHistory,
    query: {
      tenantId: form.tenantId,
      traceId: traceDetail.value.traceId
    }
  })
}

const field = (key, label, value) => ({ key, label, value })

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

const listText = value => {
  if (Array.isArray(value)) {
    return value.join(' -> ')
  }
  return displayValue(value)
}

const isNonEmpty = value => {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).length > 0
  }
  return hasDisplayValue(value)
}

const formatJson = value => JSON.stringify(value, null, 2)
const formatTimestamp = value => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

const pick = (payload, keys) => {
  for (const key of keys) {
    const value = payload?.[key]
    if (Array.isArray(value) && value.length) {
      return value
    }
    if (hasDisplayValue(value)) {
      return value
    }
  }
  return ''
}

onMounted(() => {
  refreshPage()
})
</script>

<template>
  <section class="routing-page" data-testid="routing-page">
    <SectionHeader
      :eyebrow="t('routingGovernance.eyebrow')"
      :title="t('routingGovernance.pageTitle')"
      :summary="t('routingGovernance.boundarySummary')"
      :level="1"
      size="compact"
    />

    <ToolbarShell
      :eyebrow="t('routingGovernance.filters.eyebrow')"
      :title="t('routingGovernance.filters.title')"
      density="compact"
    >
      <div class="filter-grid">
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.tenant') }}</span>
          <el-input v-model="form.tenantId" data-testid="routing-tenant-input" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('routingGovernance.fields.traceLimit') }}</span>
          <el-input v-model="form.traceLimit" />
        </label>
        <el-button type="primary" :loading="loading.page" data-testid="routing-refresh" @click="refreshPage">
          {{ t('routingGovernance.actions.refresh') }}
        </el-button>
        <el-button @click="openEvidenceDetail">
          {{ t('routingGovernance.actions.viewPolicySource') }}
        </el-button>
        <el-button @click="openPlaceholderAction('create')">
          {{ t('routingGovernance.actions.createRule') }}
        </el-button>
        <el-button @click="openPlaceholderAction('edit')">
          {{ t('routingGovernance.actions.editRule') }}
        </el-button>
      </div>
    </ToolbarShell>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <section class="tab-stage">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('routingGovernance.tabs.calibration')" name="calibration">
          <div class="tab-panel" data-testid="routing-current-policy">
            <SectionHeader
              :eyebrow="t('routingGovernance.policy.eyebrow')"
              :title="t('routingGovernance.policy.title')"
              size="compact"
            />
            <dl class="detail-grid">
              <div
                v-for="item in policyCards"
                :key="item.key"
                class="detail-grid__item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ displayValue(item.value) }}</dd>
              </div>
            </dl>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('routingGovernance.tabs.commentProtocol')" name="commentProtocol">
          <div class="tab-panel" data-testid="routing-comment-protocol">
            <SectionHeader
              :eyebrow="t('routingGovernance.comment.eyebrow')"
              :title="t('routingGovernance.comment.title')"
              size="compact"
            />
            <dl class="detail-grid">
              <div
                v-for="item in commentProtocolCards"
                :key="item.key"
                class="detail-grid__item detail-grid__item-wide"
              >
                <dt>{{ item.title }}</dt>
                <dd>{{ item.example }}</dd>
                <small>{{ item.summary }}</small>
              </div>
            </dl>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('routingGovernance.tabs.recentTraces')" name="recentTraces">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('routingGovernance.traces.eyebrow')"
              :title="t('routingGovernance.traces.title')"
              :summary="t('routingGovernance.traces.summary')"
              size="compact"
            />

            <el-table :data="recentTraces" border>
              <el-table-column prop="traceId" :label="t('routingGovernance.fields.traceId')" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openDecisionDetail(row.traceId)">
                    {{ row.traceId }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column prop="serviceCode" :label="t('common.fields.serviceCode')" min-width="150" />
              <el-table-column prop="latestStatus" :label="t('common.fields.status')" min-width="120" />
              <el-table-column prop="targetEngine" :label="t('common.fields.targetEngine')" min-width="120" />
              <el-table-column prop="auditEventCount" :label="t('routingGovernance.fields.auditEvents')" min-width="120" />
              <el-table-column :label="t('routingGovernance.fields.lastSeenAt')" min-width="170">
                <template #default="{ row }">{{ formatTimestamp(row.lastSeenAt) }}</template>
              </el-table-column>
            </el-table>
            <footer class="table-pagination-state">
              {{ t('routingGovernance.traces.state', { count: recentTraces.length }) }}
            </footer>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="detailDialogVisible"
      v-bind="{ title: traceDetail?.traceId || t('routingGovernance.detail.dialogTitle') }"
      width="980px"
    >
      <div class="dialog-stack">
        <div class="dialog-actions">
          <el-button data-testid="routing-open-parse-record" @click="openParseRecord">
            {{ t('routingGovernance.actions.openParseRecord') }}
          </el-button>
          <el-button @click="rawDrawerVisible = true">{{ t('common.actions.viewRawJson') }}</el-button>
        </div>

        <dl class="detail-grid" data-testid="routing-route-decision">
          <div
            v-for="item in decisionSummaryCards"
            :key="item.key"
            class="detail-grid__item"
          >
            <dt>{{ item.label }}</dt>
            <dd>{{ displayValue(item.value) }}</dd>
          </div>
        </dl>

        <el-table :data="traceHistoryRows" border>
          <el-table-column prop="historyId" :label="t('common.fields.historyId')" min-width="170" />
          <el-table-column prop="reportCode" :label="t('common.fields.reportCode')" min-width="180" />
          <el-table-column prop="historyType" :label="t('common.fields.type')" min-width="140" />
          <el-table-column :label="t('common.fields.submittedAt')" min-width="170">
            <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
          </el-table-column>
        </el-table>
        <footer class="table-pagination-state">
          {{ t('routingGovernance.detail.historyState', { count: traceHistoryRows.length }) }}
        </footer>

        <dl v-if="recommendationRefs.length" class="detail-grid">
          <div
            v-for="(item, index) in recommendationRefs"
            :key="`recommendation-${index}`"
            class="detail-grid__item"
          >
            <dt>recommendationRefs</dt>
            <dd>{{ displayValue(item.recommendationId || item.id || item) }}</dd>
          </div>
        </dl>
      </div>
    </el-dialog>

    <el-drawer v-model="rawDrawerVisible" :title="t('routingGovernance.rawDrawerTitle')" size="42%">
      <div class="drawer-stack">
        <div
          v-for="group in routeSignalGroups"
          :key="group.key"
          class="code-card"
        >
          <div class="code-card__header">{{ group.title }}</div>
          <pre class="code-block">{{ formatJson(group.payload) }}</pre>
        </div>
      </div>
    </el-drawer>

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
.routing-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.filter-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-4);
  align-items: end;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 220px;
}

.field-label {
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.tab-stage,
.detail-grid__item,
.code-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(35, 35, 35, 0.72);
}

.tab-stage {
  padding: var(--sqlforge-space-5);
}

.tab-panel,
.detail-grid,
.dialog-actions,
.drawer-stack {
  display: grid;
  gap: var(--sqlforge-space-4);
}

.inline-banner {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.table-link {
  border: none;
  background: transparent;
  color: var(--sqlforge-color-brand);
  padding: 0;
  cursor: pointer;
}

.detail-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-grid__item {
  display: grid;
  gap: var(--sqlforge-space-2);
  padding: 12px 14px;
}

.detail-grid__item dt {
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.detail-grid__item dd {
  margin: 0;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
  overflow-wrap: anywhere;
}

.detail-grid__item small {
  color: var(--sqlforge-text-secondary);
  line-height: 1.55;
}

.dialog-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-actions {
  grid-template-columns: repeat(2, max-content);
  justify-content: end;
}

.table-pagination-state {
  padding-top: var(--sqlforge-space-3);
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.code-card {
  padding: 14px;
}

.code-card__header {
  margin-bottom: 8px;
  color: var(--sqlforge-text-secondary);
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
