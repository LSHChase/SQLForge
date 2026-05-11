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
const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)
const recommendationPager = reactive({
  page: 1,
  size: 8
})
const dispatchEventPager = reactive({
  page: 1,
  size: 6
})

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

const pagedRecommendations = computed(() => {
  const start = (recommendationPager.page - 1) * recommendationPager.size
  return filteredRecommendations.value.slice(start, start + recommendationPager.size)
})

const selectedDispatchEvents = computed(() => {
  const traceEvents = recommendationTrace.value?.dispatchEvents || []
  if (traceEvents.length) {
    return traceEvents
  }
  return dispatchEvents.value.filter(item => item.recommendationId === selectedRecommendationId.value)
})

const pagedDispatchEvents = computed(() => {
  const start = (dispatchEventPager.page - 1) * dispatchEventPager.size
  return selectedDispatchEvents.value.slice(start, start + dispatchEventPager.size)
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

const selectFilter = filter => {
  activeFilter.value = filter
  recommendationPager.page = 1
}

const handleRecommendationPageChange = page => {
  recommendationPager.page = page
}

const handleRecommendationSizeChange = size => {
  recommendationPager.size = size
  recommendationPager.page = 1
}

const handleDispatchPageChange = page => {
  dispatchEventPager.page = page
}

const handleDispatchSizeChange = size => {
  dispatchEventPager.size = size
  dispatchEventPager.page = 1
}

const openEvidenceDrawer = (title, payload) => {
  evidenceDrawerTitle.value = title
  evidenceDrawerPayload.value = payload
  evidenceDrawerVisible.value = true
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

    <section class="workspace-frame">
      <div class="list-pane">
        <SectionHeader
          :eyebrow="t('recommendationCenter.list.eyebrow')"
          :title="t('recommendationCenter.list.title')"
          :summary="t('recommendationCenter.list.summary', { count: recommendations.length })"
          size="compact"
        />

        <el-tabs
          :model-value="activeFilter"
          class="filter-tabs"
          data-testid="recommendation-filter"
          @tab-change="selectFilter"
        >
          <el-tab-pane
            v-for="item in filterOptions"
            :key="item.value"
            :label="item.label"
            :name="item.value"
          />
        </el-tabs>

        <el-table
          :data="pagedRecommendations"
          row-key="recommendationId"
          highlight-current-row
          data-testid="recommendation-item"
          @row-click="row => loadRecommendation(row.recommendationId)"
        >
          <el-table-column prop="recommendationType" :label="t('inline.viewsRecommendationCenterRecommendationCenterView.text005')" min-width="140" />
          <el-table-column prop="summary" :label="t('recommendationCenter.detail.title')" min-width="260">
            <template #default="{ row }">
              <strong class="table-main-text">{{ row.summary || row.recommendationId }}</strong>
              <span class="table-muted-text">{{ row.expectedGain || row.reason || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="130">
            <template #default="{ row }">
              <el-tag :type="row.status === 'FAILED' ? 'danger' : 'info'">{{ row.status || 'UNKNOWN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="benefitLevel" :label="t('recommendationCenter.fields.benefitLevel')" min-width="110" />
          <el-table-column prop="riskLevel" :label="t('recommendationCenter.fields.riskLevel')" min-width="110">
            <template #default="{ row }">
              <el-tag :type="['HIGH', 'CRITICAL'].includes(String(row.riskLevel || '').toUpperCase()) ? 'warning' : 'info'">
                {{ row.riskLevel || 'UNKNOWN' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="requiresDispatch" :label="t('recommendationCenter.fields.dispatch')" min-width="110">
            <template #default="{ row }">{{ boolText(row.requiresDispatch) || 'false' }}</template>
          </el-table-column>
          <el-table-column prop="recommendationId" :label="t('accelerationGovernanceWorkbench.fields.recommendationId')" min-width="180" />
        </el-table>

        <el-pagination
          v-if="filteredRecommendations.length > recommendationPager.size"
          v-model:current-page="recommendationPager.page"
          background
          layout="sizes, prev, pager, next"
          :page-sizes="[8, 16, 32]"
          :page-size="recommendationPager.size"
          :total="filteredRecommendations.length"
          @current-change="handleRecommendationPageChange"
          @size-change="handleRecommendationSizeChange"
        />
      </div>

      <div class="detail-pane" data-testid="recommendation-detail">
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
              <div v-if="requiresReviewGuard" class="review-guard" data-testid="recommendation-review-guard">
                <div>
                  <p class="section-kicker sqlforge-code-label">{{ t('recommendationCenter.reviewGuard.eyebrow') }}</p>
                  <h3>{{ t('recommendationCenter.reviewGuard.title') }}</h3>
                </div>
                <el-tag type="warning">{{ t('recommendationCenter.fields.manualReviewRequired') }}</el-tag>
              </div>
              <dl class="description-grid">
                <div v-for="item in summaryCards" :key="item.key" class="description-item">
                  <dt>{{ item.label }}</dt>
                  <dd>{{ displayValue(item.value) }}</dd>
                </div>
              </dl>
              <dl class="description-grid">
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
                  :label="t('recommendationCenter.fields.recommendedSql')"
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
                <div class="sql-grid">
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
                <section class="evidence-table" data-testid="recommendation-text-diff">
                  <div class="evidence-heading">
                    <h3>{{ t('recommendationCenter.sections.textDiff') }}</h3>
                    <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.textDiff'), textDiffRows)">
                      {{ t('common.actions.viewRawEvidence') }}
                    </el-button>
                  </div>
                  <p v-if="!textDiffRows.length" class="muted-copy">{{ t('recommendationCenter.states.noDiffHunks') }}</p>
                  <el-table v-else :data="textDiffRows" row-key="hunkId">
                    <el-table-column prop="hunkId" :label="t('recommendationCenter.fields.hunk')" min-width="120" />
                    <el-table-column prop="type" :label="t('accelerationGovernanceWorkbench.fields.sourceType')" min-width="120" />
                    <el-table-column prop="originalText" :label="t('recommendationCenter.fields.originalSql')" min-width="220">
                      <template #default="{ row }">
                        <pre class="inline-code" :class="{ 'inline-code-active': hunkSelected(row) }">{{ displayValue(row.originalText) }}</pre>
                      </template>
                    </el-table-column>
                    <el-table-column prop="recommendedText" :label="t('recommendationCenter.fields.recommendedSql')" min-width="220">
                      <template #default="{ row }">
                        <pre class="inline-code" :class="{ 'inline-code-active': hunkSelected(row) }">{{ displayValue(row.recommendedText) }}</pre>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>
                <section class="evidence-table" data-testid="recommendation-ast-summary-diff">
                  <div class="evidence-heading">
                    <h3>{{ t('recommendationCenter.sections.astSummary') }}</h3>
                    <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.astSummary'), recommendationDiff.astSummaryDiff || {})">
                      {{ t('common.actions.viewRawEvidence') }}
                    </el-button>
                  </div>
                  <dl class="description-grid">
                    <div class="description-item">
                      <dt>{{ t('accelerationGovernanceWorkbench.fields.status') }}</dt>
                      <dd>{{ displayValue(recommendationDiff.astSummaryDiff?.parseStatus) }}</dd>
                    </div>
                  </dl>
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
              <section class="evidence-table" data-testid="recommendation-rule-diff">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.ruleDiff') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.ruleDiff'), ruleDiffRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p v-if="!ruleDiffRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <el-table v-else :data="ruleDiffRows" row-key="diffId" @row-click="row => (selectedRuleDiffId = row.diffId)">
                  <el-table-column prop="rule" :label="t('recommendationCenter.sections.ruleDiff')" min-width="180" />
                  <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="130" />
                  <el-table-column prop="level" :label="t('recommendationCenter.fields.evidenceLevel')" min-width="120" />
                  <el-table-column prop="manualReviewRequired" :label="t('recommendationCenter.fields.manualReviewRequired')" min-width="180">
                    <template #default="{ row }">{{ boolText(row.manualReviewRequired) }}</template>
                  </el-table-column>
                </el-table>
              </section>
              <section class="evidence-table" data-testid="recommendation-rule-chain">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.ruleChain') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.ruleChain'), ruleChainRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p class="muted-copy">{{ ruleChainRows.length || t('recommendationCenter.states.noRuleEvidence') }}</p>
              </section>
              <section class="evidence-table" data-testid="recommendation-preconditions">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.preconditions') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.preconditions'), preconditionRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p class="muted-copy">{{ preconditionRows.length || t('recommendationCenter.states.noRuleEvidence') }}</p>
              </section>
              <section class="evidence-table" data-testid="recommendation-semantic-risks">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.semanticRisks') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.semanticRisks'), semanticRiskRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p class="muted-copy">{{ semanticRiskRows.length || t('recommendationCenter.states.noRuleEvidence') }}</p>
              </section>
              <section class="evidence-table" data-testid="recommendation-unapplied-rules">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.unappliedRules') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.unappliedRules'), unappliedRuleRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p class="muted-copy">{{ unappliedRuleRows.length || t('recommendationCenter.states.noRuleEvidence') }}</p>
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
              <div class="trace-shell" data-testid="recommendation-trace-refs">
                <div class="pane-actions">
                  <el-button :disabled="!recommendationTrace?.reportCode" @click="openParseRecord">
                    {{ t('recommendationCenter.actions.openHistory') }}
                  </el-button>
                  <el-button data-testid="recommendation-open-alert-center" @click="openAlertCenter">
                    {{ t('recommendationCenter.actions.openAlertCenter') }}
                  </el-button>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.tabs.traceability'), recommendationTrace?.traceRefs || {})">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <dl class="description-grid">
                  <div v-for="item in traceabilityCards" :key="item.key" class="description-item">
                    <dt>{{ item.label }}</dt>
                    <dd>{{ displayValue(item.value) }}</dd>
                  </div>
                </dl>
                <section class="evidence-table" data-testid="recommendation-alert-linkage">
                  <div class="evidence-heading">
                    <h3>{{ t('recommendationCenter.sections.alertLinkage') }}</h3>
                    <el-tag type="warning">SQL_REWRITE_RESULT_DIVERGENCE</el-tag>
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
              <el-table
                :data="pagedDispatchEvents"
                row-key="dispatchEventId"
                data-testid="recommendation-dispatch-event"
                @row-click="row => openEvidenceDrawer(t('recommendationCenter.tabs.dispatchEvents'), row)"
              >
                <el-table-column prop="dispatchEventId" :label="t('accelerationGovernanceWorkbench.fields.endpoint')" min-width="210" />
                <el-table-column prop="dispatchType" :label="t('accelerationGovernanceWorkbench.fields.sourceType')" min-width="150" />
                <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="130">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'FAILED' ? 'danger' : 'info'">{{ row.status || '-' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="reportCode" :label="t('accelerationGovernanceWorkbench.fields.reportCode')" min-width="150" />
                <el-table-column prop="logicalObjectKey" :label="'logicalObjectKey'" min-width="180" />
                <el-table-column prop="targetEngine" :label="t('inline.viewsRecommendationCenterRecommendationCenterView.text006')" min-width="140" />
              </el-table>
              <el-pagination
                v-if="selectedDispatchEvents.length > dispatchEventPager.size"
                v-model:current-page="dispatchEventPager.page"
                background
                layout="sizes, prev, pager, next"
                :page-sizes="[6, 12, 24]"
                :page-size="dispatchEventPager.size"
                :total="selectedDispatchEvents.length"
                @current-change="handleDispatchPageChange"
                @size-change="handleDispatchSizeChange"
              />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </section>

    <el-drawer v-model="evidenceDrawerVisible" :title="evidenceDrawerTitle" size="52%">
      <pre class="code-block">{{ formatJson(evidenceDrawerPayload) }}</pre>
    </el-drawer>
  </section>
</template>

<style scoped>
.recommendation-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.workspace-frame {
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(0, 1.35fr);
  min-width: 0;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-2);
}

.list-pane,
.detail-pane {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
  padding: var(--sqlforge-space-5);
}

.list-pane {
  border-right: 1px solid var(--sqlforge-border-default);
}

.muted-copy {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.filter-grid,
.pane-actions,
.evidence-heading {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
}

.filter-grid {
  align-items: flex-end;
}

.field-block {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 220px;
}

.field-label,
.table-muted-text {
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.table-main-text,
.table-muted-text {
  display: block;
  min-width: 0;
  overflow-wrap: anywhere;
}

.table-main-text {
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.error-banner {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid rgba(212, 96, 96, 0.35);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(120, 28, 28, 0.18);
  color: #ffd6d6;
}

.filter-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.detail-tabs :deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
}

.review-guard,
.evidence-table,
.trace-shell {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
  min-width: 0;
  padding-block: var(--sqlforge-space-3);
  border-block: 1px solid var(--sqlforge-border-subtle);
}

.review-guard {
  flex-direction: row;
  justify-content: space-between;
  align-items: flex-start;
}

.review-guard h3,
.evidence-heading h3 {
  margin: 0;
}

.description-grid,
.sql-grid {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.description-grid {
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}

.sql-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.description-item {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  padding-bottom: var(--sqlforge-space-3);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.description-item dt {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.description-item dd {
  margin: 0;
  color: var(--sqlforge-text-primary);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.evidence-heading {
  justify-content: space-between;
}

.inline-code,
.code-block {
  margin: 0;
  overflow: auto;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.inline-code {
  max-height: 150px;
  padding: var(--sqlforge-space-3);
}

.inline-code-active {
  border-color: var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.08);
}

.code-block {
  min-height: 280px;
  padding: var(--sqlforge-space-4);
}

@media (max-width: 1280px) {
  .workspace-frame,
  .sql-grid {
    grid-template-columns: 1fr;
  }

  .list-pane {
    border-right: 0;
    border-bottom: 1px solid var(--sqlforge-border-default);
  }
}

@media (max-width: 760px) {
  .filter-grid,
  .pane-actions,
  .review-guard {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
