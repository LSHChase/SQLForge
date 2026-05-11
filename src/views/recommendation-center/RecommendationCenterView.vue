<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  formatRuntimeError,
  getDispatchContract,
  getDispatchEvents,
  getRecommendationDetail,
  getRecommendationDiff,
  getRecommendations,
  getRecommendationTrace
} from '../../services/runtimeGateApi'
import SectionHeader from '../common/SectionHeader.vue'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import ToolbarShell from '../common/ToolbarShell.vue'

const { t } = useI18n()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a'
})

const loading = reactive({
  page: false,
  detail: false
})

const activeFilter = ref('ALL')
const recommendations = ref([])
const dispatchContract = ref(null)
const dispatchEvents = ref([])
const selectedRecommendationId = ref('')
const selectedRecommendation = ref(null)
const recommendationDiff = ref(null)
const recommendationTrace = ref(null)
const errorMessage = ref('')
const diffErrorMessage = ref('')
const activeDetailTab = ref('summary')
const selectedRuleDiffId = ref('')


// Static contract tokens: recommendation detail, coordinationMode, PULL_ONLY, dispatchEvents, benefitLevel, riskLevel, recommendedSqlText, logicalObjectKey, textDiff, astSummaryDiff, ruleChain, preconditions, semanticRisks, unappliedRules, manualReviewRequired.

const filterOptions = computed(() => {
  const counts = {
    ALL: recommendations.value.length,
    REQUIRES_DISPATCH: 0,
    DISPATCH_READY: 0,
    HIGH_BENEFIT: 0,
    HIGH_RISK: 0,
    ACCELERATION: 0,
    REWRITE: 0
  }
  for (const item of recommendations.value) {
    if (item.requiresDispatch) {
      counts.REQUIRES_DISPATCH += 1
    }
    if (String(item.status || '').toUpperCase() === 'DISPATCH_READY') {
      counts.DISPATCH_READY += 1
    }
    if (String(item.benefitLevel || '').toUpperCase() === 'HIGH') {
      counts.HIGH_BENEFIT += 1
    }
    if (['HIGH', 'CRITICAL'].includes(String(item.riskLevel || '').toUpperCase())) {
      counts.HIGH_RISK += 1
    }
    if (String(item.recommendationType || '').toUpperCase() === 'ACCELERATION') {
      counts.ACCELERATION += 1
    }
    if (String(item.recommendationType || '').toUpperCase() === 'REWRITE') {
      counts.REWRITE += 1
    }
  }
  return [
    option('ALL', t('inline.viewsRecommendationCenterRecommendationCenterView.text001'), counts.ALL),
    option('REQUIRES_DISPATCH', t('inline.viewsRecommendationCenterRecommendationCenterView.text002'), counts.REQUIRES_DISPATCH),
    option('DISPATCH_READY', 'Dispatch ready', counts.DISPATCH_READY),
    option('HIGH_BENEFIT', t('inline.viewsRecommendationCenterRecommendationCenterView.text003'), counts.HIGH_BENEFIT),
    option('HIGH_RISK', t('inline.viewsRecommendationCenterRecommendationCenterView.text004'), counts.HIGH_RISK),
    option('ACCELERATION', 'ACCELERATION', counts.ACCELERATION),
    option('REWRITE', 'REWRITE', counts.REWRITE)
  ]
})

const filteredRecommendations = computed(() =>
  recommendations.value.filter(item => matchesFilter(item, activeFilter.value))
)

const selectedDispatchEvents = computed(() => {
  const traceEvents = recommendationTrace.value?.dispatchEvents || []
  if (traceEvents.length) {
    return traceEvents
  }
  return dispatchEvents.value.filter(item => item.recommendationId === selectedRecommendationId.value)
})

const summaryCards = computed(() => {
  const recommendation = selectedRecommendation.value
  if (!recommendation) {
    return []
  }
  return [
    field('recommendationType', t('inline.viewsRecommendationCenterRecommendationCenterView.text005'), recommendation.recommendationType),
    field('status', 'Status', recommendation.status),
    field('benefitLevel', 'benefitLevel', recommendation.benefitLevel),
    field('riskLevel', 'riskLevel', recommendation.riskLevel),
    field('validationStatus', t('recommendationCenter.fields.validationStatus'), recommendation.validationStatus),
    field('alertStatus', t('recommendationCenter.fields.alertStatus'), recommendation.alertStatus),
    field('manualReviewRequired', t('recommendationCenter.fields.manualReviewRequired'), boolText(recommendation.manualReviewRequired)),
    field('autoApplyAllowed', t('recommendationCenter.fields.autoApplyAllowed'), boolText(recommendation.autoApplyAllowed)),
    field('targetEngine', t('inline.viewsRecommendationCenterRecommendationCenterView.text006'), recommendation.targetEngine),
    field('targetDatasource', t('inline.viewsRecommendationCenterRecommendationCenterView.text007'), recommendation.targetDatasource),
    field('requiresDispatch', t('inline.viewsRecommendationCenterRecommendationCenterView.text008'), boolText(recommendation.requiresDispatch)),
    field('logicalObjectKey', 'logicalObjectKey', recommendation.logicalObjectKey)
  ]
})

const diffSummaryCards = computed(() => {
  const diff = recommendationDiff.value
  const summary = diff?.diffSummary || {}
  if (!diff) {
    return []
  }
  return [
    field('diffStatus', t('recommendationCenter.fields.diffStatus'), diff.diffStatus || summary.diffStatus),
    field('sourceType', t('recommendationCenter.fields.sourceType'), diff.sourceType),
    field('sourceKind', t('recommendationCenter.fields.sourceKind'), diff.sourceKind),
    field('sourceId', t('recommendationCenter.fields.sourceId'), diff.sourceId),
    field('evidenceLevel', t('recommendationCenter.fields.evidenceLevel'), diff.evidenceLevel),
    field('sqlFingerprint', t('recommendationCenter.fields.sqlFingerprint'), diff.sqlFingerprint),
    field('changeCount', t('recommendationCenter.fields.changeCount'), summary.changeCount),
    field('ruleDiffCount', t('recommendationCenter.fields.ruleDiffCount'), summary.ruleDiffCount),
    field('manualReviewRequired', t('recommendationCenter.fields.manualReviewRequired'), boolText(summary.manualReviewRequired)),
    field('autoApplyAllowed', t('recommendationCenter.fields.autoApplyAllowed'), boolText(summary.autoApplyAllowed)),
    field('writesBackRecommendation', t('recommendationCenter.fields.writesBackRecommendation'), boolText(summary.writesBackRecommendation)),
    field('evidenceBoundary', t('recommendationCenter.fields.evidenceBoundary'), summary.evidenceBoundary)
  ]
})

const reviewGuardCards = computed(() => {
  const recommendation = selectedRecommendation.value
  const summary = recommendationDiff.value?.diffSummary || {}
  if (!recommendation) {
    return []
  }
  return [
    field('riskLevel', 'riskLevel', recommendation.riskLevel),
    field('riskSummary', t('recommendationCenter.fields.riskSummary'), recommendation.riskSummary),
    field('validationMethod', t('recommendationCenter.fields.validationMethod'), recommendation.validationMethod),
    field('validationStatus', t('recommendationCenter.fields.validationStatus'), recommendation.validationStatus),
    field('manualReviewRequired', t('recommendationCenter.fields.manualReviewRequired'), boolText(firstDefined(recommendation.manualReviewRequired, summary.manualReviewRequired))),
    field('autoApplyAllowed', t('recommendationCenter.fields.autoApplyAllowed'), boolText(firstDefined(recommendation.autoApplyAllowed, summary.autoApplyAllowed))),
    field('diffBoundary', t('recommendationCenter.fields.evidenceBoundary'), summary.evidenceBoundary)
  ]
})

const textDiffRows = computed(() => normalizeArray(recommendationDiff.value?.textDiff))
const ruleDiffRows = computed(() => normalizeArray(recommendationDiff.value?.ruleDiff))
const ruleChainRows = computed(() => normalizeArray(selectedRecommendation.value?.ruleChain))
const preconditionRows = computed(() => normalizeArray(selectedRecommendation.value?.preconditions))
const semanticRiskRows = computed(() => normalizeArray(selectedRecommendation.value?.semanticRisks))
const unappliedRuleRows = computed(() => normalizeArray(selectedRecommendation.value?.unappliedRules))

const selectedRuleDiff = computed(() =>
  ruleDiffRows.value.find(item => item.diffId === selectedRuleDiffId.value) || null
)

const highlightedHunkIds = computed(() => {
  const ids = selectedRuleDiff.value?.textHunkIds
  return Array.isArray(ids) ? ids.map(item => String(item)) : []
})

const requiresReviewGuard = computed(() => {
  const riskLevel = String(selectedRecommendation.value?.riskLevel || '').toUpperCase()
  const manualReview = selectedRecommendation.value?.manualReviewRequired || recommendationDiff.value?.diffSummary?.manualReviewRequired
  return Boolean(manualReview) || ['HIGH', 'CRITICAL'].includes(riskLevel)
})

const traceabilityCards = computed(() => {
  const trace = recommendationTrace.value
  if (!trace) {
    return []
  }
  return [
    field('historyId', 'historyId', trace.historyId),
    field('parseTaskId', 'parseTaskId', trace.parseTaskId),
    field('batchId', 'batchId', trace.batchId),
    field('routeDecisionId', 'routeDecisionId', trace.routeDecisionId),
    field('alertId', 'alertId', trace.alertId),
    field('sqlFingerprint', 'sqlFingerprint', trace.sqlFingerprint),
    field('reportCode', 'reportCode', trace.reportCode),
    field('logicalObjectKey', 'logicalObjectKey', trace.logicalObjectKey)
  ].filter(item => displayValue(item.value) !== '-')
})

const alertLinkageCards = computed(() => {
  const trace = recommendationTrace.value || {}
  const recommendation = selectedRecommendation.value || {}
  const alertRefs = normalizeArray(trace.alertRefs || trace.traceRefs?.alertRefs)
  return [
    field('alertId', 'alertId', trace.alertId),
    field('alertStatus', t('recommendationCenter.fields.alertStatus'), firstDefined(recommendation.alertStatus, trace.alertStatus)),
    field('validationStatus', t('recommendationCenter.fields.validationStatus'), recommendation.validationStatus),
    field('manualReviewRequired', t('recommendationCenter.fields.manualReviewRequired'), boolText(recommendation.manualReviewRequired)),
    field('autoApplyAllowed', t('recommendationCenter.fields.autoApplyAllowed'), boolText(recommendation.autoApplyAllowed)),
    field('alertRefs', t('recommendationCenter.fields.alertRefs'), alertRefs.length)
  ]
})

const contractCards = computed(() => {
  const contract = dispatchContract.value
  if (!contract) {
    return []
  }
  return [
    field('coordinationMode', 'coordinationMode', contract.coordinationMode),
    field('sqlExecutionAllowed', 'sqlExecutionAllowed', boolText(contract.sqlExecutionAllowed)),
    field('dataLoadingAllowed', 'dataLoadingAllowed', boolText(contract.dataLoadingAllowed)),
    field('activeExternalPushAllowed', 'activeExternalPushAllowed', boolText(contract.activeExternalPushAllowed)),
    field('externalPullRequired', 'externalPullRequired', boolText(contract.externalPullRequired)),
    field('allowedEventStatuses', t('inline.viewsRecommendationCenterRecommendationCenterView.text009'), listText(contract.allowedEventStatuses)),
    field('allowedDispatchTypes', t('inline.viewsRecommendationCenterRecommendationCenterView.text010'), listText(contract.allowedDispatchTypes)),
    field('auditBoundary', t('inline.viewsRecommendationCenterRecommendationCenterView.text011'), contract.auditBoundary),
    field('residualOwner', t('inline.viewsRecommendationCenterRecommendationCenterView.text012'), contract.residualOwner)
  ]
})

const refreshPage = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    const [recommendationList, contract, events] = await Promise.all([
      getRecommendations(form.tenantId, {
        requestPrefix: 'frontend-recommendation-center-list'
      }),
      getDispatchContract(form.tenantId, {
        requestPrefix: 'frontend-recommendation-center-contract'
      }),
      getDispatchEvents(form.tenantId, '', {
        requestPrefix: 'frontend-recommendation-center-events'
      })
    ])
    recommendations.value = Array.isArray(recommendationList) ? recommendationList : []
    dispatchContract.value = contract
    dispatchEvents.value = Array.isArray(events) ? events : []

    const fallbackId =
      selectedRecommendationId.value && recommendations.value.some(item => item.recommendationId === selectedRecommendationId.value)
        ? selectedRecommendationId.value
        : filteredRecommendations.value[0]?.recommendationId || recommendations.value[0]?.recommendationId || ''

    if (fallbackId) {
      await loadRecommendation(fallbackId)
    } else {
      selectedRecommendationId.value = ''
      selectedRecommendation.value = null
      recommendationDiff.value = null
      recommendationTrace.value = null
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadRecommendation = async recommendationId => {
  if (!recommendationId) {
    selectedRecommendationId.value = ''
    selectedRecommendation.value = null
    recommendationDiff.value = null
    recommendationTrace.value = null
    diffErrorMessage.value = ''
    return
  }

  loading.detail = true
  errorMessage.value = ''
  diffErrorMessage.value = ''
  recommendationDiff.value = null
  selectedRuleDiffId.value = ''
  selectedRecommendationId.value = recommendationId
  try {
    const [detail, trace] = await Promise.all([
      getRecommendationDetail(form.tenantId, recommendationId, {
        requestPrefix: 'frontend-recommendation-center-detail'
      }),
      getRecommendationTrace(form.tenantId, recommendationId, {
        requestPrefix: 'frontend-recommendation-center-trace'
      })
    ])
    selectedRecommendation.value = detail
    recommendationTrace.value = trace
    try {
      recommendationDiff.value = await getRecommendationDiff(form.tenantId, recommendationId, {
        requestPrefix: 'frontend-recommendation-center-diff'
      })
    } catch (error) {
      diffErrorMessage.value = formatRuntimeError(error)
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const openParseRecord = () => {
  const trace = recommendationTrace.value
  if (!trace?.reportCode) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: {
      tenantId: form.tenantId,
      reportId: trace.reportCode
    }
  })
}

const openAlertCenter = () => {
  router.push({
    path: ROUTE_PATHS.alertCenter,
    query: {
      tenantId: form.tenantId,
      alertType: 'SQL_REWRITE_RESULT_DIVERGENCE',
      alertId: recommendationTrace.value?.alertId || '',
      recommendationId: selectedRecommendationId.value,
      historyId: recommendationTrace.value?.historyId || '',
      sqlFingerprint: recommendationTrace.value?.sqlFingerprint || ''
    }
  })
}

const openRoutingGovernance = () => {
  router.push({
    path: ROUTE_PATHS.routingGovernance
  })
}

const openAccelerationWorkbench = () => {
  router.push({
    path: ROUTE_PATHS.acceleration
  })
}

const option = (value, label, count) => ({
  value,
  label: `${label} (${count})`
})

const field = (key, label, value) => ({ key, label, value })

const matchesFilter = (item, filter) => {
  if (filter === 'ALL') {
    return true
  }
  if (filter === 'REQUIRES_DISPATCH') {
    return Boolean(item.requiresDispatch)
  }
  if (filter === 'DISPATCH_READY') {
    return String(item.status || '').toUpperCase() === 'DISPATCH_READY'
  }
  if (filter === 'HIGH_BENEFIT') {
    return String(item.benefitLevel || '').toUpperCase() === 'HIGH'
  }
  if (filter === 'HIGH_RISK') {
    return ['HIGH', 'CRITICAL'].includes(String(item.riskLevel || '').toUpperCase())
  }
  return String(item.recommendationType || '').toUpperCase() === filter
}

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

const boolText = value => {
  if (value === true) {
    return 'true'
  }
  if (value === false) {
    return 'false'
  }
  return ''
}

const listText = value => {
  if (Array.isArray(value)) {
    return value.join(' -> ')
  }
  return value
}

const normalizeArray = value => (Array.isArray(value) ? value : [])

const firstDefined = (...values) => values.find(value => value !== null && value !== undefined)

const evidenceField = (item, key) => displayValue(item?.[key])

const hunkSelected = hunk => {
  if (!highlightedHunkIds.value.length) {
    return false
  }
  return highlightedHunkIds.value.includes(String(hunk?.hunkId || ''))
}

const formatJson = value => JSON.stringify(value, null, 2)

onMounted(() => {
  refreshPage()
})
</script>

<template>
  <section class="recommendation-page" data-testid="recommendation-page">
    <SectionHeader
      :eyebrow="t('recommendationCenter.eyebrow')"
      :title="t('recommendationCenter.pageTitle')"
      :summary="t('recommendationCenter.boundarySummary')"
      :level="1"
      size="compact"
    />

    <ToolbarShell
      :eyebrow="t('recommendationCenter.filters.eyebrow')"
      :title="t('recommendationCenter.filters.title')"
      density="compact"
    >
      <div class="filter-grid">
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.tenant') }}</span>
          <el-input v-model.trim="form.tenantId" data-testid="recommendation-tenant-input" />
        </label>
        <el-button type="primary" :loading="loading.page" data-testid="recommendation-refresh" @click="refreshPage">
          {{ t('recommendationCenter.actions.refresh') }}
        </el-button>
        <el-button @click="openRoutingGovernance">
          {{ t('recommendationCenter.actions.openRouting') }}
        </el-button>
        <el-button @click="openAccelerationWorkbench">
          {{ t('recommendationCenter.actions.openParse') }}
        </el-button>
      </div>
    </ToolbarShell>

    <p v-if="errorMessage" class="error-banner" data-testid="recommendation-error">{{ errorMessage }}</p>

    <div class="recommendation-workspace">
      <section class="recommendation-list-pane">
        <SectionHeader
          :eyebrow="t('recommendationCenter.list.eyebrow')"
          :title="t('recommendationCenter.list.title')"
          :summary="t('recommendationCenter.list.summary', { count: recommendations.length })"
          size="compact"
        />

        <div class="filter-row" data-testid="recommendation-filter">
          <button
            v-for="item in filterOptions"
            :key="item.value"
            type="button"
            class="filter-chip"
            :class="{ 'filter-chip-active': activeFilter === item.value }"
            @click="activeFilter = item.value"
          >
            {{ item.label }}
          </button>
        </div>

        <div class="recommendation-list">
          <button
            v-for="item in filteredRecommendations"
            :key="item.recommendationId"
            type="button"
            class="recommendation-row"
            :class="{ 'recommendation-row-active': selectedRecommendationId === item.recommendationId }"
            data-testid="recommendation-item"
            @click="loadRecommendation(item.recommendationId)"
          >
            <div class="recommendation-row-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.recommendationType }}</p>
                <h3>{{ item.summary || item.recommendationId }}</h3>
              </div>
              <span class="status-pill" :class="{ 'status-pill-warn': item.riskLevel === 'HIGH' || item.riskLevel === 'CRITICAL' }">
                {{ item.status || 'UNKNOWN' }}
              </span>
            </div>
            <p class="muted-copy">{{ item.expectedGain || item.reason || '-' }}</p>
            <div class="pill-row">
              <span class="mini-pill">{{ t('recommendationCenter.fields.benefitLevel') }} {{ item.benefitLevel || 'UNKNOWN' }}</span>
              <span class="mini-pill">{{ t('recommendationCenter.fields.riskLevel') }} {{ item.riskLevel || 'UNKNOWN' }}</span>
              <span class="mini-pill">{{ t('recommendationCenter.fields.dispatch') }} {{ boolText(item.requiresDispatch) || 'false' }}</span>
            </div>
          </button>
        </div>
      </section>

      <section class="recommendation-detail-pane" data-testid="recommendation-detail">
        <SectionHeader
          :eyebrow="t('recommendationCenter.detail.eyebrow')"
          :title="t('recommendationCenter.detail.title')"
          :summary="selectedRecommendation?.recommendationId || t('recommendationCenter.states.selectRecommendation')"
          size="compact"
        />

        <p v-if="loading.detail" class="muted-copy">
          {{ t('recommendationCenter.states.loadingDetail') }}
        </p>
        <p v-else-if="!selectedRecommendation" class="muted-copy">
          {{ t('recommendationCenter.states.emptyDetail') }}
        </p>
        <template v-else>
          <el-tabs v-model="activeDetailTab" class="detail-tabs">
            <el-tab-pane :label="t('recommendationCenter.tabs.summary')" name="summary">
              <div
                v-if="requiresReviewGuard"
                class="review-guard"
                data-testid="recommendation-review-guard"
              >
                <div>
                  <p class="section-kicker sqlforge-code-label">{{ t('recommendationCenter.reviewGuard.eyebrow') }}</p>
                  <h3>{{ t('recommendationCenter.reviewGuard.title') }}</h3>
                </div>
                <span class="status-pill status-pill-warn">{{ t('recommendationCenter.fields.manualReviewRequired') }}</span>
              </div>
              <dl class="description-grid">
                <div v-for="item in summaryCards" :key="item.key" class="description-item">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ displayValue(item.value) }}</dd>
                </div>
              </dl>
              <dl class="description-grid description-grid-copy">
                <div class="description-item">
                  <dt>{{ t('recommendationCenter.fields.expectedGain') }}</dt>
                  <dd>{{ selectedRecommendation.expectedGain || '-' }}</dd>
                </div>
                <div class="description-item">
                  <dt>{{ t('recommendationCenter.fields.riskSummary') }}</dt>
                  <dd>{{ selectedRecommendation.riskSummary || '-' }}</dd>
                </div>
                <div class="description-item">
                  <dt>{{ t('recommendationCenter.fields.reason') }}</dt>
                  <dd>{{ selectedRecommendation.reason || '-' }}</dd>
                </div>
              </dl>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.sqlEvidence')" name="sqlEvidence">
              <div class="sql-grid">
                <SqlCodeBlock
                  :value="selectedRecommendation.sourceSqlText || ''"
                  :label="t('recommendationCenter.fields.sourceSql')"
                  :copy-label="t('common.actions.copy')"
                  compact
                  data-testid="recommendation-source-sql"
                />
                <SqlCodeBlock
                  :value="selectedRecommendation.recommendedSqlText || ''"
                  label="recommendedSqlText"
                  :copy-label="t('common.actions.copy')"
                  compact
                  data-testid="recommendation-recommended-sql"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.sqlDiff')" name="sqlDiff">
              <p v-if="diffErrorMessage" class="error-banner" data-testid="recommendation-diff-error">{{ diffErrorMessage }}</p>
              <p v-if="!recommendationDiff && !diffErrorMessage" class="muted-copy">
                {{ t('recommendationCenter.states.emptyDiff') }}
              </p>
              <template v-else-if="recommendationDiff">
                <dl class="description-grid" data-testid="recommendation-sql-diff">
                  <div v-for="item in diffSummaryCards" :key="item.key" class="description-item">
                    <dt>{{ item.label }}</dt>
                    <dd>{{ displayValue(item.value) }}</dd>
                  </div>
                </dl>

                <div class="sql-grid sql-grid-spacious">
                  <SqlCodeBlock
                    :value="recommendationDiff.originalSql || selectedRecommendation.sourceSqlText || ''"
                    :label="t('recommendationCenter.fields.originalSql')"
                    :copy-label="t('common.actions.copy')"
                    compact
                  />
                  <SqlCodeBlock
                    :value="recommendationDiff.recommendedSql || selectedRecommendation.recommendedSqlText || ''"
                    :label="t('recommendationCenter.fields.recommendedSql')"
                    :copy-label="t('common.actions.copy')"
                    compact
                  />
                </div>

                <section class="evidence-group" data-testid="recommendation-text-diff">
                  <div class="evidence-group-header">
                    <h3>{{ t('recommendationCenter.sections.textDiff') }}</h3>
                    <span class="mini-pill">{{ textDiffRows.length }}</span>
                  </div>
                  <p v-if="!textDiffRows.length" class="muted-copy">{{ t('recommendationCenter.states.noDiffHunks') }}</p>
                  <article
                    v-for="hunk in textDiffRows"
                    :key="hunk.hunkId"
                    class="diff-hunk"
                    :class="{ 'diff-hunk-highlighted': hunkSelected(hunk) }"
                  >
                    <div class="recommendation-row-header">
                      <div class="pill-row">
                        <span class="mini-pill">{{ hunk.hunkId }}</span>
                        <span class="mini-pill">{{ hunk.type }}</span>
                        <span class="mini-pill">{{ hunk.granularity }}</span>
                      </div>
                      <span class="section-kicker sqlforge-code-label">
                        {{ t('recommendationCenter.fields.hunk') }}
                      </span>
                    </div>
                    <div class="diff-hunk-columns">
                      <pre class="code-block code-block-compact">{{ displayValue(hunk.originalText) }}</pre>
                      <pre class="code-block code-block-compact">{{ displayValue(hunk.recommendedText) }}</pre>
                    </div>
                  </article>
                </section>

                <section class="evidence-group" data-testid="recommendation-ast-summary-diff">
                  <div class="evidence-group-header">
                    <h3>{{ t('recommendationCenter.sections.astSummary') }}</h3>
                    <span class="mini-pill">{{ displayValue(recommendationDiff.astSummaryDiff?.parseStatus) }}</span>
                  </div>
                  <pre class="code-block code-block-compact">{{ formatJson(recommendationDiff.astSummaryDiff || {}) }}</pre>
                </section>
              </template>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.rulesRisk')" name="rulesRisk">
              <dl class="description-grid" data-testid="recommendation-rule-risk-summary">
                <div v-for="item in reviewGuardCards" :key="item.key" class="description-item">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ displayValue(item.value) }}</dd>
                </div>
              </dl>

              <section class="evidence-group" data-testid="recommendation-rule-diff">
                <div class="evidence-group-header">
                  <h3>{{ t('recommendationCenter.sections.ruleDiff') }}</h3>
                  <span class="mini-pill">{{ ruleDiffRows.length }}</span>
                </div>
                <p v-if="!ruleDiffRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <button
                  v-for="item in ruleDiffRows"
                  :key="item.diffId"
                  type="button"
                  class="rule-evidence-row"
                  :class="{ 'rule-evidence-row-active': selectedRuleDiffId === item.diffId }"
                  @click="selectedRuleDiffId = item.diffId"
                >
                  <div class="recommendation-row-header">
                    <div>
                      <p class="section-kicker sqlforge-code-label">{{ item.sourceType }}</p>
                      <h3>{{ displayValue(item.rule) }}</h3>
                    </div>
                    <span class="status-pill" :class="{ 'status-pill-warn': item.manualReviewRequired }">
                      {{ displayValue(item.status) }}
                    </span>
                  </div>
                  <div class="pill-row">
                    <span class="mini-pill">{{ displayValue(item.level) }}</span>
                    <span class="mini-pill">{{ displayValue(item.highlightStatus) }}</span>
                    <span class="mini-pill">{{ t('recommendationCenter.fields.manualReviewRequired') }} {{ boolText(item.manualReviewRequired) }}</span>
                  </div>
                </button>
              </section>

              <section class="evidence-group" data-testid="recommendation-rule-chain">
                <div class="evidence-group-header">
                  <h3>{{ t('recommendationCenter.sections.ruleChain') }}</h3>
                  <span class="mini-pill">{{ ruleChainRows.length }}</span>
                </div>
                <p v-if="!ruleChainRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <article v-for="(item, index) in ruleChainRows" :key="`rule-${index}`" class="evidence-row">
                  <div class="pill-row">
                    <span class="mini-pill">rule {{ evidenceField(item, 'rule') }}</span>
                    <span class="mini-pill">level {{ evidenceField(item, 'level') }}</span>
                    <span class="mini-pill">status {{ evidenceField(item, 'status') }}</span>
                  </div>
                  <pre class="code-block code-block-compact">{{ formatJson(item) }}</pre>
                </article>
              </section>

              <section class="evidence-group" data-testid="recommendation-preconditions">
                <div class="evidence-group-header">
                  <h3>{{ t('recommendationCenter.sections.preconditions') }}</h3>
                  <span class="mini-pill">{{ preconditionRows.length }}</span>
                </div>
                <p v-if="!preconditionRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <article v-for="(item, index) in preconditionRows" :key="`precondition-${index}`" class="evidence-row">
                  <pre class="code-block code-block-compact">{{ formatJson(item) }}</pre>
                </article>
              </section>

              <section class="evidence-group" data-testid="recommendation-semantic-risks">
                <div class="evidence-group-header">
                  <h3>{{ t('recommendationCenter.sections.semanticRisks') }}</h3>
                  <span class="mini-pill">{{ semanticRiskRows.length }}</span>
                </div>
                <p v-if="!semanticRiskRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <article v-for="(item, index) in semanticRiskRows" :key="`risk-${index}`" class="evidence-row">
                  <div class="pill-row">
                    <span class="mini-pill">rule {{ evidenceField(item, 'rule') }}</span>
                    <span class="mini-pill">severity {{ evidenceField(item, 'severity') }}</span>
                    <span class="mini-pill">category {{ evidenceField(item, 'category') }}</span>
                  </div>
                  <pre class="code-block code-block-compact">{{ formatJson(item) }}</pre>
                </article>
              </section>

              <section class="evidence-group" data-testid="recommendation-unapplied-rules">
                <div class="evidence-group-header">
                  <h3>{{ t('recommendationCenter.sections.unappliedRules') }}</h3>
                  <span class="mini-pill">{{ unappliedRuleRows.length }}</span>
                </div>
                <p v-if="!unappliedRuleRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <article v-for="(item, index) in unappliedRuleRows" :key="`unapplied-${index}`" class="evidence-row">
                  <div class="pill-row">
                    <span class="mini-pill">rule {{ evidenceField(item, 'rule') }}</span>
                    <span class="mini-pill">reason {{ evidenceField(item, 'reason') }}</span>
                    <span class="mini-pill">{{ t('recommendationCenter.fields.manualReviewRequired') }} true</span>
                  </div>
                  <pre class="code-block code-block-compact">{{ formatJson(item) }}</pre>
                </article>
              </section>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.dispatchContract')" name="dispatchContract">
              <dl class="description-grid" data-testid="recommendation-dispatch-contract">
                <div v-for="item in contractCards" :key="item.key" class="description-item">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ displayValue(item.value) }}</dd>
                </div>
              </dl>
              <p class="muted-copy">{{ t('recommendationCenter.dispatch.boundary') }}</p>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.traceability')" name="traceability">
              <div class="traceability-group" data-testid="recommendation-trace-refs">
                <div class="pane-actions">
                  <el-button :disabled="!recommendationTrace?.reportCode" @click="openParseRecord">
                    {{ t('recommendationCenter.actions.openHistory') }}
                  </el-button>
                  <el-button data-testid="recommendation-open-alert-center" @click="openAlertCenter">
                    {{ t('recommendationCenter.actions.openAlertCenter') }}
                  </el-button>
                </div>
                <div class="pill-row">
                  <span v-for="item in traceabilityCards" :key="item.key" class="mini-pill">
                    {{ item.label }}: {{ displayValue(item.value) }}
                  </span>
                </div>
                <pre class="code-block code-block-compact">{{ formatJson(recommendationTrace?.traceRefs || {}) }}</pre>
                <section class="evidence-group" data-testid="recommendation-alert-linkage">
                  <div class="evidence-group-header">
                    <h3>{{ t('recommendationCenter.sections.alertLinkage') }}</h3>
                    <span class="mini-pill">SQL_REWRITE_RESULT_DIVERGENCE</span>
                  </div>
                  <dl class="description-grid">
                    <div v-for="item in alertLinkageCards" :key="item.key" class="description-item">
                      <dt>{{ item.label }}</dt>
                      <dd>{{ displayValue(item.value) }}</dd>
                    </div>
                  </dl>
                </section>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.dispatchEvents')" name="dispatchEvents">
              <div class="dispatch-event-list">
                <article
                  v-for="item in selectedDispatchEvents"
                  :key="item.dispatchEventId"
                  class="dispatch-event-row"
                  data-testid="recommendation-dispatch-event"
                >
                  <div class="recommendation-row-header">
                    <div>
                      <p class="section-kicker sqlforge-code-label">{{ item.dispatchType }}</p>
                      <h3>{{ item.dispatchEventId }}</h3>
                    </div>
                    <span class="status-pill" :class="{ 'status-pill-warn': item.status === 'FAILED' }">
                      {{ item.status }}
                    </span>
                  </div>
                  <p class="muted-copy">{{ item.resultMessage || t('recommendationCenter.states.waitingCallback') }}</p>
                  <div class="pill-row">
                    <span class="mini-pill">report {{ item.reportCode || '-' }}</span>
                    <span class="mini-pill">logical {{ item.logicalObjectKey || '-' }}</span>
                    <span class="mini-pill">engine {{ item.targetEngine || '-' }}</span>
                  </div>
                  <pre class="code-block code-block-compact">{{ formatJson(item.statusHistory || []) }}</pre>
                </article>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </section>
    </div>
  </section>
</template>

<style scoped>
.recommendation-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.recommendation-list-pane,
.recommendation-detail-pane {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
  padding-top: var(--sqlforge-space-5);
  border-top: 1px solid var(--sqlforge-border-default);
}

.recommendation-row-header h3 {
  margin: 0;
  overflow-wrap: anywhere;
}

.muted-copy,
.detail-copy-text {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
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
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
}

.filter-row,
.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-chip,
.recommendation-row,
.rule-evidence-row {
  cursor: pointer;
}

.error-banner {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(120, 28, 28, 0.18);
  border: 1px solid rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

.recommendation-workspace {
  display: grid;
  gap: var(--sqlforge-space-5);
  grid-template-columns: minmax(280px, 0.9fr) minmax(480px, 1.4fr);
}

.recommendation-list,
.dispatch-event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-row,
.dispatch-event-row,
.traceability-group,
.review-guard,
.evidence-group,
.evidence-row,
.diff-hunk,
.rule-evidence-row,
.description-item {
  padding: 16px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(35, 35, 35, 0.72);
}

.recommendation-row,
.rule-evidence-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.rule-evidence-row {
  width: 100%;
  font: inherit;
  color: inherit;
}

.recommendation-row-active {
  border-color: var(--sqlforge-color-brand-border);
}

.rule-evidence-row-active,
.diff-hunk-highlighted {
  border-color: var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.08);
}

.recommendation-row-header,
.pane-actions,
.review-guard,
.evidence-group-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  flex-wrap: wrap;
}

.recommendation-row-header > div,
.evidence-group-header > div {
  min-width: 0;
}

.review-guard h3,
.evidence-group h3,
.rule-evidence-row h3 {
  margin: 0;
}

.filter-chip,
.mini-pill,
.status-pill,
.section-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  max-width: 100%;
  white-space: normal;
  overflow-wrap: anywhere;
  text-align: center;
}

.filter-chip {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
}

.filter-chip-active,
.mini-pill,
.status-pill,
.section-badge {
  border: 1px solid var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.08);
  color: var(--sqlforge-color-brand);
}

.status-pill-warn {
  background: rgba(176, 85, 18, 0.12);
  color: #9a4b15;
}

.description-grid,
.sql-grid {
  display: grid;
  gap: 14px;
}

.description-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.description-grid-copy {
  margin-top: var(--sqlforge-space-4);
}

.detail-tabs :deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
}

.sql-grid {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.sql-grid-spacious {
  margin-top: var(--sqlforge-space-2);
}

.evidence-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.diff-hunk-columns {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.description-item {
  display: grid;
  gap: var(--sqlforge-space-2);
}

.description-item dt {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--sqlforge-text-muted);
}

.description-item dd {
  margin: 0;
  color: var(--sqlforge-text-primary);
  line-height: 1.55;
  overflow-wrap: anywhere;
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

.code-block-compact {
  padding: 12px;
}

@media (max-width: 1200px) {
  .recommendation-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .filter-row,
  .pill-row {
    flex-direction: column;
  }
}
</style>
