<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  createSqlRewriteRecord,
  formatRuntimeError,
  getDispatchContract,
  getDispatchEvents,
  getGovernanceQueryHistoryPage,
  getRecommendationDetail,
  getRecommendationDiff,
  getRecommendationPage,
  getRecommendationTrace,
  getRewriteValidationRuns,
  getSqlParseHistoryPage,
  getRewritePublishEligibility,
  getSqlRewriteRecord,
  getSqlRewriteRecords,
  listParseBatches,
  listReportBatches,
  pauseSqlRewriteRecord,
  publishSqlRewriteRecord,
  reviewSqlRewriteRecord,
  unpublishSqlRewriteRecord
} from '../../services/runtimeGateApi'
import SectionHeader from '../common/SectionHeader.vue'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlCompareBlock from '../common/SqlCompareBlock.vue'
import { buildRecommendedSqlDisplay } from '../common/sqlCompare.mjs'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const normalizeQueryValue = value => (Array.isArray(value) ? String(value[0] || '').trim() : String(value || '').trim())
const DETAIL_TAB_NAMES = [
  'summary',
  'sqlDiff',
  'rulesRisk',
  'sqlEvidence',
  'rewriteLifecycle',
  'dispatchContract',
  'traceability',
  'dispatchEvents'
]
const normalizeDetailTab = value => {
  const tabName = normalizeQueryValue(value)
  return DETAIL_TAB_NAMES.includes(tabName) ? tabName : ''
}

const form = reactive({
  tenantId: normalizeQueryValue(route.query.tenantId) || 'tenant-a'
})

const rewriteActionForm = reactive({
  reviewNote: '',
  actionReason: 'frontend recommendation rewrite lifecycle'
})

const loading = reactive({
  page: false,
  detail: false,
  lifecycle: false,
  lifecycleAction: '',
  validationRuns: false,
  sourceOptions: false
})

const recommendationFilters = reactive({
  recommendationType: '',
  status: '',
  benefitLevel: '',
  riskLevel: '',
  validationStatus: '',
  requiresDispatch: '',
  manualReviewRequired: '',
  sourceCategory: '',
  sourceObjectId: ''
})
const recommendations = ref([])
const sourceObjectOptions = ref([])
const dispatchContract = ref(null)
const dispatchEvents = ref([])
const selectedRecommendationId = ref(normalizeQueryValue(route.query.recommendationId))
const selectedRecommendation = ref(null)
const recommendationDiff = ref(null)
const recommendationTrace = ref(null)
const rewriteRecords = ref([])
const selectedRewriteRecordId = ref(normalizeQueryValue(route.query.rewriteRecordId))
const selectedRewriteRecord = ref(null)
const rewritePublishEligibility = ref(null)
const rewriteValidationRuns = ref([])
const errorMessage = ref('')
const diffErrorMessage = ref('')
const lifecycleErrorMessage = ref('')
const lifecycleSuccessMessage = ref('')
const validationRunErrorMessage = ref('')
const activeDetailTab = ref('summary')
const preferredDetailTab = ref(normalizeDetailTab(route.query.tab || route.query.detailTab))
const isRewriteRecordEntry = computed(() => normalizeDetailTab(route.query.tab || route.query.detailTab) === 'rewriteLifecycle')
const pageEyebrow = computed(() =>
  isRewriteRecordEntry.value ? t('navigation.modules.rewriteGovernance') : t('recommendationCenter.eyebrow')
)
const pageTitle = computed(() =>
  isRewriteRecordEntry.value ? t('navigation.items.rewriteRecords') : t('recommendationCenter.pageTitle')
)
const pageSummary = computed(() =>
  isRewriteRecordEntry.value ? t('recommendationCenter.rewriteLifecycle.boundary') : t('recommendationCenter.boundarySummary')
)
const selectedRuleDiffId = ref('')
const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)
const recommendationDetailDrawerVisible = ref(false)
const recommendationPager = reactive({
  page: 1,
  size: 8,
  totalCount: 0,
  pageCount: 0,
  hasMore: false,
  sortBy: 'createdAt',
  sortOrder: 'DESC'
})
const dispatchEventPager = reactive({
  page: 1,
  size: 6
})

// Static contract tokens: recommendation detail, coordinationMode, PULL_ONLY, dispatchEvents, benefitLevel, riskLevel, recommendedSqlText, logicalObjectKey, textDiff, astSummaryDiff, ruleChain, preconditions, semanticRisks, unappliedRules, manualReviewRequired, SqlCompareBlock.

const recommendationTypeOptions = ['REWRITE', 'ACCELERATION', 'CREATE_TABLE', 'PREWARM', 'MAINTENANCE']
const recommendationStatusOptions = ['RECOMMENDED', 'REVIEWING', 'DISPATCH_READY', 'CANCELLED']
const benefitLevelOptions = ['UNKNOWN', 'LOW', 'MEDIUM', 'HIGH']
const riskLevelOptions = ['UNKNOWN', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
const validationStatusOptions = ['NOT_VALIDATED', 'VALIDATING', 'EQUIVALENT', 'DIVERGED', 'FAILED', 'EXPIRED']
const booleanFilterOptions = [
  { label: 'true', value: true },
  { label: 'false', value: false }
]
const rewriteRecordSourceTypeOptions = ['PARSE', 'QUERY']
const rewriteRecordSourceKindOptions = [
  'STRUCTURE_PARSE',
  'COMBINED_PARSE',
  'PARSE_BATCH',
  'REPORT_BATCH',
  'END_OF_DAY_SLOW_SQL',
  'QUERY_HISTORY',
  'SLOW_SQL',
  'HIGH_P99',
  'HIGH_SCAN',
  'BENCHMARK_REGRESSION',
  'MANUAL'
]
const rewriteRecordEvidenceLevelOptions = ['STATIC_PARSE', 'ACCESS_PARSE', 'EXPLAIN_PLAN', 'RUNTIME_HISTORY', 'BENCHMARK', 'MIXED']
const rewriteRecordValidationStatusOptions = ['NOT_VALIDATED', 'VALIDATING', 'EQUIVALENT', 'DIVERGED', 'FAILED', 'EXPIRED']
const sourceCategoryOptions = computed(() => [
  {
    label: t('recommendationCenter.sourceCategories.query'),
    value: 'QUERY',
    sourceType: 'QUERY',
    sourceKind: 'QUERY_HISTORY',
    objectMode: 'QUERY_HISTORY'
  },
  {
    label: t('recommendationCenter.sourceCategories.sqlParse'),
    value: 'SQL_PARSE',
    sourceType: 'PARSE',
    sourceKind: 'STRUCTURE_PARSE,COMBINED_PARSE',
    objectMode: 'SQL_PARSE'
  },
  {
    label: t('recommendationCenter.sourceCategories.parseBatch'),
    value: 'PARSE_BATCH',
    sourceType: 'PARSE',
    sourceKind: 'PARSE_BATCH',
    objectMode: 'PARSE_BATCH'
  },
  {
    label: t('recommendationCenter.sourceCategories.reportBatch'),
    value: 'REPORT_BATCH',
    sourceType: 'PARSE',
    sourceKind: 'REPORT_BATCH',
    objectMode: 'REPORT_BATCH'
  }
])

const selectedSourceCategory = computed(() =>
  sourceCategoryOptions.value.find(item => item.value === recommendationFilters.sourceCategory) || null
)

const normalizeSourceCategoryFromRoute = () => {
  const explicit = normalizeQueryValue(route.query.sourceCategory).toUpperCase()
  if (sourceCategoryOptions.value.some(item => item.value === explicit)) {
    return explicit
  }
  const sourceType = normalizeQueryValue(route.query.sourceType).toUpperCase()
  const sourceKind = normalizeQueryValue(route.query.sourceKind).toUpperCase()
  if (sourceType === 'QUERY') {
    return 'QUERY'
  }
  if (sourceType === 'PARSE' && sourceKind.includes('REPORT_BATCH')) {
    return 'REPORT_BATCH'
  }
  if (sourceType === 'PARSE' && sourceKind.includes('PARSE_BATCH')) {
    return 'PARSE_BATCH'
  }
  if (sourceType === 'PARSE' || sourceKind.includes('STRUCTURE_PARSE') || sourceKind.includes('COMBINED_PARSE')) {
    return 'SQL_PARSE'
  }
  return ''
}

const routeSourceObjectId = () =>
  normalizeQueryValue(route.query.sourceObjectId) ||
  normalizeQueryValue(route.query.historyId) ||
  normalizeQueryValue(route.query.parseHistoryId) ||
  normalizeQueryValue(route.query.parseTaskId) ||
  normalizeQueryValue(route.query.batchId)

const routeSourceMeta = () => {
  const objectId = routeSourceObjectId()
  if (!objectId || objectId !== recommendationFilters.sourceObjectId) {
    return {}
  }
  return {
    sourceType: normalizeQueryValue(route.query.sourceType),
    sourceKind: normalizeQueryValue(route.query.sourceKind),
    historyId: normalizeQueryValue(route.query.historyId) || normalizeQueryValue(route.query.parseHistoryId),
    parseTaskId: normalizeQueryValue(route.query.parseTaskId),
    sourceId: normalizeQueryValue(route.query.sourceId),
    batchId: normalizeQueryValue(route.query.batchId),
    reportCode: normalizeQueryValue(route.query.reportCode)
  }
}

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

const rewriteRecordOptions = computed(() =>
  rewriteRecords.value.map(item => ({
    label: item.rewriteRecordId,
    value: item.rewriteRecordId,
    status: `${displayValue(item.reviewStatus)} / ${displayValue(item.publishStatus)}`
  }))
)

const selectedReviewStatus = computed(() => String(selectedRewriteRecord.value?.reviewStatus || '').toUpperCase())

const selectedPublishStatus = computed(() => String(selectedRewriteRecord.value?.publishStatus || '').toUpperCase())

const rewriteLifecycleState = computed(() => {
  if (!selectedRewriteRecord.value) {
    return {
      type: 'info',
      label: t('recommendationCenter.states.noRewriteRecord')
    }
  }
  if (selectedPublishStatus.value === 'PUBLISHED' && selectedRewriteRecord.value.runtimeBindingId) {
    return {
      type: 'success',
      label: t('recommendationCenter.states.runtimeActive')
    }
  }
  if (selectedReviewStatus.value === 'APPROVED' && ['UNPUBLISHED', 'PUBLISH_FAILED'].includes(selectedPublishStatus.value)) {
    return {
      type: 'warning',
      label: t('recommendationCenter.states.approvedNotPublished')
    }
  }
  if (selectedPublishStatus.value === 'PAUSED') {
    return {
      type: 'warning',
      label: t('recommendationCenter.states.runtimePaused')
    }
  }
  if (selectedReviewStatus.value === 'REJECTED') {
    return {
      type: 'danger',
      label: t('recommendationCenter.states.reviewRejected')
    }
  }
  return {
    type: 'info',
    label: t('recommendationCenter.states.awaitingRewriteReview')
  }
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

const focusSummaryCards = computed(() => {
  const recommendation = selectedRecommendation.value
  if (!recommendation) {
    return []
  }
  const diff = recommendationDiff.value || {}
  const summary = diff.diffSummary || {}
  const trace = recommendationTrace.value || {}
  return [
    field('sourceType', t('recommendationCenter.fields.sourceType'), firstDisplayValue(diff.sourceType, recommendation.sourceType, trace.sourceType)),
    field('sourceKind', t('recommendationCenter.fields.sourceKind'), firstDisplayValue(diff.sourceKind, recommendation.sourceKind, trace.sourceKind)),
    field('sourceId', t('recommendationCenter.fields.sourceId'), firstDisplayValue(diff.sourceId, recommendation.sourceId, trace.historyId, trace.reportCode)),
    field('benefitLevel', t('recommendationCenter.fields.benefitLevel'), recommendation.benefitLevel),
    field('expectedGain', t('recommendationCenter.fields.expectedGain'), recommendation.expectedGain),
    field('riskLevel', t('recommendationCenter.fields.riskLevel'), recommendation.riskLevel),
    field('diffStatus', t('recommendationCenter.fields.diffStatus'), firstDisplayValue(diff.diffStatus, summary.diffStatus)),
    field('ruleDiffCount', t('recommendationCenter.fields.ruleDiffCount'), summary.ruleDiffCount),
    field('manualReviewRequired', t('recommendationCenter.fields.manualReviewRequired'), boolText(firstDefined(recommendation.manualReviewRequired, summary.manualReviewRequired)))
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
const sourceProblemRows = computed(() => normalizeArray(selectedRecommendation.value?.sourceProblems))
const issueRuleLinkRows = computed(() => normalizeArray(selectedRecommendation.value?.issueRuleLinks))
const preconditionRows = computed(() => normalizeArray(selectedRecommendation.value?.preconditions))
const semanticRiskRows = computed(() => normalizeArray(selectedRecommendation.value?.semanticRisks))
const unappliedRuleRows = computed(() => normalizeArray(selectedRecommendation.value?.unappliedRules))
const frontendCompareOriginalSql = computed(() =>
  firstDisplayValue(selectedRecommendation.value?.sourceSqlText, recommendationDiff.value?.originalSql) || ''
)
const frontendCompareRecommendedSql = computed(() =>
  buildRecommendedSqlDisplay(
    frontendCompareOriginalSql.value,
    firstDisplayValue(selectedRecommendation.value?.recommendedSqlText, recommendationDiff.value?.recommendedSql) || ''
  )
)
const hasFrontendCompareSql = computed(
  () => String(frontendCompareOriginalSql.value || '').trim() && String(frontendCompareRecommendedSql.value || '').trim()
)

const isRewriteRecommendation = computed(() =>
  String(selectedRecommendation.value?.recommendationType || '').toUpperCase() === 'REWRITE'
)

const isRewriteReviewCandidate = computed(
  () => isRewriteRecommendation.value
    || Boolean(selectedRecommendation.value?.manualReviewRequired || recommendationDiff.value?.diffSummary?.manualReviewRequired)
)

const canCreateRewriteRecordFromRecommendation = computed(() => {
  const recommendation = selectedRecommendation.value
  if (!recommendation || !isRewriteReviewCandidate.value || !recommendation.recommendationId) {
    return false
  }
  const originalSql = firstDisplayValue(recommendation.sourceSqlText, recommendationDiff.value?.originalSql)
  const recommendedSql = firstDisplayValue(recommendation.recommendedSqlText, recommendationDiff.value?.recommendedSql)
  return hasDisplayValue(originalSql) && hasDisplayValue(recommendedSql)
})

const rewriteRecordEntryActionText = computed(() =>
  rewriteRecords.value.length
    ? t('recommendationCenter.actions.openRewriteReview')
    : t('recommendationCenter.actions.createRewriteRecordAndReview')
)

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

const rewriteLifecycleCards = computed(() => {
  const record = selectedRewriteRecord.value
  if (!record) {
    return []
  }
  return [
    field('reviewStatus', t('recommendationCenter.fields.reviewStatus'), record.reviewStatus),
    field('reviewedBy', t('recommendationCenter.fields.reviewedBy'), record.reviewedBy),
    field('reviewedAt', t('recommendationCenter.fields.reviewedAt'), record.reviewedAt),
    field('publishStatus', t('recommendationCenter.fields.publishStatus'), record.publishStatus),
    field('validationStatus', t('recommendationCenter.fields.validationStatus'), record.validationStatus),
    field('alertStatus', t('recommendationCenter.fields.alertStatus'), record.alertStatus),
    field('lastValidationRunId', t('recommendationCenter.fields.lastValidationRunId'), record.lastValidationRunId),
    field('autoApplyAllowed', t('recommendationCenter.fields.autoApplyAllowed'), boolText(record.autoApplyAllowed)),
    field('runtimeBindingId', t('recommendationCenter.fields.runtimeBindingId'), record.runtimeBindingId),
    field('runtimeRuleVersion', t('recommendationCenter.fields.runtimeRuleVersion'), record.runtimeRuleVersion),
    field('runtimeBindingScope', t('recommendationCenter.fields.runtimeBindingScope'), record.runtimeBindingScope),
    field('runtimeBindingAt', t('recommendationCenter.fields.runtimeBindingAt'), record.runtimeBindingAt),
    field('runtimeBindingBy', t('recommendationCenter.fields.runtimeBindingBy'), record.runtimeBindingBy)
  ]
})

const publishEligibilityCards = computed(() => {
  const eligibility = rewritePublishEligibility.value
  if (!eligibility) {
    return []
  }
  return [
    field('eligible', t('recommendationCenter.fields.publishEligible'), boolText(eligibility.eligible)),
    field('policyId', t('recommendationCenter.fields.policyId'), eligibility.policyId),
    field('reviewStatus', t('recommendationCenter.fields.reviewStatus'), eligibility.reviewStatus),
    field('validationStatus', t('recommendationCenter.fields.validationStatus'), eligibility.validationStatus),
    field('publishStatus', t('recommendationCenter.fields.publishStatus'), eligibility.publishStatus),
    field('alertStatus', t('recommendationCenter.fields.alertStatus'), eligibility.alertStatus),
    field('autoApplyAllowed', t('recommendationCenter.fields.autoApplyAllowed'), boolText(eligibility.autoApplyAllowed)),
    field('lastValidationRunId', t('recommendationCenter.fields.lastValidationRunId'), eligibility.lastValidationRunId)
  ]
})

const publishRefusalReasons = computed(() => normalizeArray(rewritePublishEligibility.value?.refusalReasons))

const canApproveRewrite = computed(() => selectedReviewStatus.value === 'PENDING_REVIEW')

const canRejectRewrite = computed(() => selectedReviewStatus.value === 'PENDING_REVIEW')

const canPublishRewrite = computed(
  () =>
    rewritePublishEligibility.value?.eligible === true &&
    ['UNPUBLISHED', 'PUBLISH_FAILED'].includes(selectedPublishStatus.value)
)

const canPauseRewrite = computed(() => selectedPublishStatus.value === 'PUBLISHED')

const canUnpublishRewrite = computed(() => ['PUBLISHED', 'PAUSED'].includes(selectedPublishStatus.value))

const resetRewriteLifecycle = () => {
  rewriteRecords.value = []
  selectedRewriteRecordId.value = ''
  selectedRewriteRecord.value = null
  rewritePublishEligibility.value = null
  rewriteValidationRuns.value = []
  lifecycleErrorMessage.value = ''
  lifecycleSuccessMessage.value = ''
  validationRunErrorMessage.value = ''
}

const syncActiveDetailTabFromRoute = () => {
  if (preferredDetailTab.value) {
    activeDetailTab.value = preferredDetailTab.value
  }
}

const syncRouteQueryState = () => {
  form.tenantId = normalizeQueryValue(route.query.tenantId) || 'tenant-a'
  const nextSourceCategory = normalizeSourceCategoryFromRoute()
  if (nextSourceCategory) {
    if (recommendationFilters.sourceCategory !== nextSourceCategory) {
      sourceObjectOptions.value = []
    }
    recommendationFilters.sourceCategory = nextSourceCategory
    recommendationFilters.sourceObjectId = routeSourceObjectId()
  }
  selectedRecommendationId.value = normalizeQueryValue(route.query.recommendationId)
  selectedRewriteRecordId.value = normalizeQueryValue(route.query.rewriteRecordId)
  preferredDetailTab.value = normalizeDetailTab(route.query.tab || route.query.detailTab)
  syncActiveDetailTabFromRoute()
}

const loadRewriteValidationRuns = async rewriteRecordId => {
  if (!rewriteRecordId) {
    rewriteValidationRuns.value = []
    validationRunErrorMessage.value = ''
    return
  }
  loading.validationRuns = true
  validationRunErrorMessage.value = ''
  try {
    const runs = await getRewriteValidationRuns(form.tenantId, rewriteRecordId, {
      requestPrefix: 'frontend-recommendation-rewrite-validation-runs'
    })
    rewriteValidationRuns.value = Array.isArray(runs) ? runs : []
  } catch (error) {
    rewriteValidationRuns.value = []
    validationRunErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.validationRuns = false
  }
}

const fetchRewriteRecordLifecycle = async rewriteRecordId => {
  const record = await getSqlRewriteRecord(form.tenantId, rewriteRecordId, {
    requestPrefix: 'frontend-recommendation-rewrite-record-detail'
  })
  selectedRewriteRecord.value = record
  selectedRewriteRecordId.value = record?.rewriteRecordId || rewriteRecordId
  rewritePublishEligibility.value = await getRewritePublishEligibility(form.tenantId, rewriteRecordId, {
    requestPrefix: 'frontend-recommendation-rewrite-publish-eligibility'
  })
  await loadRewriteValidationRuns(rewriteRecordId)
}

const loadRewriteRecordLifecycle = async rewriteRecordId => {
  if (!rewriteRecordId) {
    selectedRewriteRecordId.value = ''
    selectedRewriteRecord.value = null
    rewritePublishEligibility.value = null
    rewriteValidationRuns.value = []
    validationRunErrorMessage.value = ''
    return
  }
  loading.lifecycle = true
  lifecycleErrorMessage.value = ''
  lifecycleSuccessMessage.value = ''
  validationRunErrorMessage.value = ''
  try {
    await fetchRewriteRecordLifecycle(rewriteRecordId)
  } catch (error) {
    selectedRewriteRecord.value = null
    rewritePublishEligibility.value = null
    rewriteValidationRuns.value = []
    lifecycleErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lifecycle = false
  }
}

const loadRewriteRecordsForRecommendation = async (recommendationId, preferredRewriteRecordId = '') => {
  if (!recommendationId) {
    resetRewriteLifecycle()
    return
  }
  loading.lifecycle = true
  lifecycleErrorMessage.value = ''
  lifecycleSuccessMessage.value = ''
  validationRunErrorMessage.value = ''
  try {
    const records = await getSqlRewriteRecords(
      form.tenantId,
      {
        recommendationId
      },
      {
        requestPrefix: 'frontend-recommendation-rewrite-record-list'
      }
    )
    rewriteRecords.value = Array.isArray(records) ? records : []
    const candidateId = preferredRewriteRecordId || selectedRewriteRecordId.value
    const nextRecordId =
      candidateId && rewriteRecords.value.some(item => item.rewriteRecordId === candidateId)
        ? candidateId
        : rewriteRecords.value[0]?.rewriteRecordId || ''
    if (nextRecordId) {
      await fetchRewriteRecordLifecycle(nextRecordId)
    } else {
      selectedRewriteRecordId.value = ''
      selectedRewriteRecord.value = null
      rewritePublishEligibility.value = null
      rewriteValidationRuns.value = []
      validationRunErrorMessage.value = ''
    }
  } catch (error) {
    rewriteRecords.value = []
    selectedRewriteRecord.value = null
    rewritePublishEligibility.value = null
    rewriteValidationRuns.value = []
    lifecycleErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lifecycle = false
  }
}

const refreshPage = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    const [recommendationPage, contract, events] = await Promise.all([
      getRecommendationPage(form.tenantId, buildRecommendationPageQuery(), {
        requestPrefix: 'frontend-recommendation-center-page'
      }),
      getDispatchContract(form.tenantId, {
        requestPrefix: 'frontend-recommendation-center-contract'
      }),
      getDispatchEvents(form.tenantId, '', {
        requestPrefix: 'frontend-recommendation-center-events'
      })
    ])
    recommendations.value = Array.isArray(recommendationPage?.items) ? recommendationPage.items : []
    recommendationPager.page = Number(recommendationPage?.pageNo || recommendationPager.page || 1)
    recommendationPager.size = Number(recommendationPage?.pageSize || recommendationPager.size || 8)
    recommendationPager.totalCount = Number(recommendationPage?.totalCount || 0)
    recommendationPager.pageCount = Number(recommendationPage?.pageCount || 0)
    recommendationPager.hasMore = Boolean(recommendationPage?.hasMore)
    dispatchContract.value = contract
    dispatchEvents.value = Array.isArray(events) ? events : []

    if (selectedRecommendationId.value) {
      await loadRecommendation(selectedRecommendationId.value)
    } else {
      selectedRecommendationId.value = ''
      selectedRecommendation.value = null
      recommendationDiff.value = null
      recommendationTrace.value = null
      activeDetailTab.value = 'summary'
      syncActiveDetailTabFromRoute()
      resetRewriteLifecycle()
      recommendationDetailDrawerVisible.value = false
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
    activeDetailTab.value = 'summary'
    syncActiveDetailTabFromRoute()
    resetRewriteLifecycle()
    return
  }

  loading.detail = true
  errorMessage.value = ''
  diffErrorMessage.value = ''
  lifecycleErrorMessage.value = ''
  lifecycleSuccessMessage.value = ''
  recommendationDiff.value = null
  selectedRuleDiffId.value = ''
  selectedRecommendationId.value = recommendationId
  recommendationDetailDrawerVisible.value = true
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
      activeDetailTab.value = recommendationDiff.value || hasFrontendCompareSql.value ? 'sqlDiff' : 'summary'
      syncActiveDetailTabFromRoute()
    } catch (error) {
      diffErrorMessage.value = formatRuntimeError(error)
      activeDetailTab.value = hasFrontendCompareSql.value ? 'sqlDiff' : 'summary'
      syncActiveDetailTabFromRoute()
    }
    await loadRewriteRecordsForRecommendation(recommendationId, selectedRewriteRecordId.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const normalizePagedItems = payload => {
  if (Array.isArray(payload)) {
    return payload
  }
  return Array.isArray(payload?.items) ? payload.items : []
}

const sourceOption = (value, label, meta = {}) => ({
  value,
  label: label || value,
  meta
})

const optionLabel = (...parts) => parts.map(part => String(part || '').trim()).filter(Boolean).join(' · ')

const buildSourceObjectOptions = (mode, rows) => {
  if (mode === 'QUERY_HISTORY') {
    return rows
      .filter(row => row?.historyId)
      .map(row => sourceOption(
        row.historyId,
        optionLabel(row.historyId, row.reportCode, row.datasourceCode, row.status),
        { historyId: row.historyId, sourceId: row.historyId }
      ))
  }
  if (mode === 'SQL_PARSE') {
    return rows
      .filter(row => row?.parseHistoryId || row?.historyId || row?.parseTaskId)
      .map(row => {
        const historyId = row.parseHistoryId || row.historyId || ''
        const parseTaskId = row.parseTaskId || row.sourceId || ''
        return sourceOption(
          historyId || parseTaskId,
          optionLabel(historyId || parseTaskId, row.reportCode, row.datasourceCode, row.resultStatus),
          { historyId, parseTaskId, sourceId: row.sourceId || parseTaskId }
        )
      })
  }
  if (mode === 'PARSE_BATCH' || mode === 'REPORT_BATCH') {
    return rows
      .filter(row => row?.batchId)
      .map(row => sourceOption(
        row.batchId,
        optionLabel(row.batchName || row.batchId, row.batchId, row.datasourceCode, row.status),
        { batchId: row.batchId, reportCode: row.reportCode }
      ))
  }
  return []
}

const loadSourceObjectOptions = async () => {
  const category = selectedSourceCategory.value
  if (!category || !form.tenantId) {
    sourceObjectOptions.value = []
    return
  }
  loading.sourceOptions = true
  try {
    let payload = null
    if (category.objectMode === 'QUERY_HISTORY') {
      payload = await getGovernanceQueryHistoryPage(
        {
          tenantId: form.tenantId,
          requestTenantId: form.tenantId,
          pageNo: 1,
          pageSize: 50,
          sortBy: 'submittedAt',
          sortOrder: 'DESC'
        },
        { requestPrefix: 'frontend-recommendation-source-query-history' }
      )
    } else if (category.objectMode === 'SQL_PARSE') {
      payload = await getSqlParseHistoryPage(
        {
          tenantId: form.tenantId,
          requestTenantId: form.tenantId,
          pageNo: 1,
          pageSize: 50,
          sortBy: 'submittedAt',
          sortOrder: 'DESC'
        },
        { requestPrefix: 'frontend-recommendation-source-parse-history' }
      )
    } else if (category.objectMode === 'PARSE_BATCH') {
      payload = await listParseBatches(
        form.tenantId,
        { pageNo: 1, pageSize: 50 },
        { requestPrefix: 'frontend-recommendation-source-parse-batch' }
      )
    } else if (category.objectMode === 'REPORT_BATCH') {
      payload = await listReportBatches(
        form.tenantId,
        { pageNo: 1, pageSize: 50 },
        { requestPrefix: 'frontend-recommendation-source-report-batch' }
      )
    }
    sourceObjectOptions.value = buildSourceObjectOptions(category.objectMode, normalizePagedItems(payload))
  } catch (error) {
    sourceObjectOptions.value = []
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.sourceOptions = false
  }
}

const buildSourceFilterQuery = () => {
  const category = selectedSourceCategory.value
  if (!category) {
    return {}
  }
  const query = {
    sourceType: category.sourceType,
    sourceKind: category.sourceKind
  }
  const selectedObject = sourceObjectOptions.value.find(item => item.value === recommendationFilters.sourceObjectId)
  const meta = {
    ...routeSourceMeta(),
    ...(selectedObject?.meta || {})
  }
  if (meta.sourceType && meta.sourceType === category.sourceType) {
    query.sourceType = meta.sourceType
  }
  if (meta.sourceKind) {
    query.sourceKind = meta.sourceKind
  }
  if (recommendationFilters.sourceObjectId) {
    if (category.objectMode === 'QUERY_HISTORY') {
      query.historyId = meta.historyId || recommendationFilters.sourceObjectId
      query.sourceId = meta.sourceId || recommendationFilters.sourceObjectId
    } else if (category.objectMode === 'SQL_PARSE') {
      if (meta.historyId) {
        query.historyId = meta.historyId
      }
      if (meta.parseTaskId) {
        query.parseTaskId = meta.parseTaskId
      } else if (!meta.historyId) {
        query.parseTaskId = recommendationFilters.sourceObjectId
      }
      if (meta.sourceId) {
        query.sourceId = meta.sourceId
      }
    } else if (category.objectMode === 'PARSE_BATCH' || category.objectMode === 'REPORT_BATCH') {
      query.batchId = meta.batchId || recommendationFilters.sourceObjectId
      if (meta.reportCode) {
        query.reportCode = meta.reportCode
      }
    }
  }
  return query
}

const buildRecommendationPageQuery = () => ({
  pageNo: recommendationPager.page,
  pageSize: recommendationPager.size,
  sortBy: recommendationPager.sortBy,
  sortOrder: recommendationPager.sortOrder,
  recommendationType: recommendationFilters.recommendationType,
  status: recommendationFilters.status,
  benefitLevel: recommendationFilters.benefitLevel,
  riskLevel: recommendationFilters.riskLevel,
  validationStatus: recommendationFilters.validationStatus,
  requiresDispatch: recommendationFilters.requiresDispatch,
  manualReviewRequired: recommendationFilters.manualReviewRequired,
  ...buildSourceFilterQuery()
})

const refreshRecommendationFromFirstPage = () => {
  recommendationPager.page = 1
  refreshPage()
}

const handleSourceCategoryChange = () => {
  recommendationFilters.sourceObjectId = ''
  sourceObjectOptions.value = []
  loadSourceObjectOptions()
}

const resetRecommendationFilters = () => {
  recommendationFilters.recommendationType = ''
  recommendationFilters.status = ''
  recommendationFilters.benefitLevel = ''
  recommendationFilters.riskLevel = ''
  recommendationFilters.validationStatus = ''
  recommendationFilters.requiresDispatch = ''
  recommendationFilters.manualReviewRequired = ''
  recommendationFilters.sourceCategory = ''
  recommendationFilters.sourceObjectId = ''
  sourceObjectOptions.value = []
  recommendationPager.page = 1
  recommendationPager.size = 8
  recommendationPager.sortBy = 'createdAt'
  recommendationPager.sortOrder = 'DESC'
  refreshPage()
}

const openRecommendationDetail = row => {
  const recommendationId = typeof row === 'string' ? row : row?.recommendationId
  if (recommendationId) {
    loadRecommendation(recommendationId)
  }
}

const handleRecommendationDrawerClosed = () => {
  selectedRecommendationId.value = ''
  selectedRewriteRecordId.value = ''
}

const handleRecommendationSortChange = sortState => {
  const prop = sortState?.prop || 'createdAt'
  recommendationPager.sortBy = prop
  recommendationPager.sortOrder = sortState?.order === 'ascending' ? 'ASC' : 'DESC'
  recommendationPager.page = 1
  refreshPage()
}

const handleRecommendationPageChange = page => {
  recommendationPager.page = page
  refreshPage()
}

const handleRecommendationSizeChange = size => {
  recommendationPager.size = size
  recommendationPager.page = 1
  refreshPage()
}

const handleDispatchPageChange = page => {
  dispatchEventPager.page = page
}

const handleDispatchSizeChange = size => {
  dispatchEventPager.size = size
  dispatchEventPager.page = 1
}

const performRewriteLifecycleAction = async action => {
  const rewriteRecordId = selectedRewriteRecord.value?.rewriteRecordId || selectedRewriteRecordId.value
  if (!rewriteRecordId) {
    lifecycleErrorMessage.value = t('recommendationCenter.states.noRewriteRecord')
    return
  }
  const reviewNote = String(rewriteActionForm.reviewNote || '').trim()
  if (action === 'REJECT' && !reviewNote) {
    lifecycleErrorMessage.value = t('recommendationCenter.states.reviewNoteRequired')
    return
  }
  loading.lifecycleAction = action
  lifecycleErrorMessage.value = ''
  lifecycleSuccessMessage.value = ''
  try {
    let updatedRecord = null
    if (action === 'APPROVE') {
      updatedRecord = await reviewSqlRewriteRecord(
        form.tenantId,
        rewriteRecordId,
        {
          tenantId: form.tenantId,
          reviewStatus: 'APPROVED',
          reviewNote
        },
        {
          requestPrefix: 'frontend-recommendation-rewrite-review-approve'
        }
      )
    } else if (action === 'REJECT') {
      updatedRecord = await reviewSqlRewriteRecord(
        form.tenantId,
        rewriteRecordId,
        {
          tenantId: form.tenantId,
          reviewStatus: 'REJECTED',
          reviewNote
        },
        {
          requestPrefix: 'frontend-recommendation-rewrite-review-reject'
        }
      )
    } else if (action === 'PUBLISH') {
      updatedRecord = await publishSqlRewriteRecord(
        form.tenantId,
        rewriteRecordId,
        {
          tenantId: form.tenantId,
          reason: rewriteActionForm.actionReason
        },
        {
          requestPrefix: 'frontend-recommendation-rewrite-publish'
        }
      )
    } else if (action === 'PAUSE') {
      updatedRecord = await pauseSqlRewriteRecord(
        form.tenantId,
        rewriteRecordId,
        {
          tenantId: form.tenantId,
          reason: rewriteActionForm.actionReason
        },
        {
          requestPrefix: 'frontend-recommendation-rewrite-pause'
        }
      )
    } else if (action === 'UNPUBLISH') {
      updatedRecord = await unpublishSqlRewriteRecord(
        form.tenantId,
        rewriteRecordId,
        {
          tenantId: form.tenantId,
          reason: rewriteActionForm.actionReason
        },
        {
          requestPrefix: 'frontend-recommendation-rewrite-unpublish'
        }
      )
    }
    await loadRewriteRecordsForRecommendation(
      selectedRecommendationId.value,
      updatedRecord?.rewriteRecordId || rewriteRecordId
    )
    lifecycleSuccessMessage.value = t('recommendationCenter.states.lifecycleActionApplied')
  } catch (error) {
    lifecycleErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lifecycleAction = ''
  }
}

const normalizeEnumValue = (value, allowedValues, fallbackValue) => {
  const normalized = String(value || '').trim().toUpperCase()
  return allowedValues.includes(normalized) ? normalized : fallbackValue
}

const compactObject = payload => {
  const result = {}
  Object.entries(payload || {}).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      if (value.length) {
        result[key] = value
      }
      return
    }
    if (value && typeof value === 'object') {
      if (Object.keys(value).length) {
        result[key] = value
      }
      return
    }
    if (value === false || value === 0 || value === true) {
      result[key] = value
      return
    }
    if (hasDisplayValue(value)) {
      result[key] = value
    }
  })
  return result
}

const buildRewriteRecordCreatePayload = () => {
  const recommendation = selectedRecommendation.value || {}
  const diff = recommendationDiff.value || {}
  const diffSummary = diff.diffSummary || {}
  const trace = recommendationTrace.value || {}
  const sourceType = normalizeEnumValue(
    firstDisplayValue(recommendation.sourceType, diff.sourceType),
    rewriteRecordSourceTypeOptions,
    'PARSE'
  )
  const sourceKind = normalizeEnumValue(
    firstDisplayValue(recommendation.sourceKind, diff.sourceKind),
    rewriteRecordSourceKindOptions,
    sourceType === 'QUERY' ? 'QUERY_HISTORY' : 'STRUCTURE_PARSE'
  )
  const sourceId = firstDisplayValue(
    recommendation.sourceId,
    diff.sourceId,
    recommendation.sourceSqlId,
    trace.historyId,
    recommendation.historyId,
    trace.parseTaskId,
    recommendation.parseTaskId,
    recommendation.recommendationId
  )
  const originalSql = firstDisplayValue(recommendation.sourceSqlText, diff.originalSql)
  const recommendedSql = firstDisplayValue(recommendation.recommendedSqlText, diff.recommendedSql)
  const evidenceLevel = normalizeEnumValue(
    firstDisplayValue(recommendation.evidenceLevel, diff.evidenceLevel),
    rewriteRecordEvidenceLevelOptions,
    'STATIC_PARSE'
  )
  const validationStatus = normalizeEnumValue(
    recommendation.validationStatus,
    rewriteRecordValidationStatusOptions,
    'NOT_VALIDATED'
  )
  const historyId = firstDisplayValue(recommendation.historyId, trace.historyId)

  return compactObject({
    tenantId: form.tenantId,
    recommendationId: recommendation.recommendationId,
    sourceType,
    sourceKind,
    sourceId,
    evidenceLevel,
    historyId,
    parseHistoryId: sourceType === 'PARSE' ? historyId : '',
    sqlFingerprint: firstDisplayValue(recommendation.sqlFingerprint, diff.sqlFingerprint, trace.sqlFingerprint),
    datasourceCode: recommendation.targetDatasource,
    status: 'DRAFT',
    validationStatus,
    publishStatus: 'UNPUBLISHED',
    autoApplyAllowed: false,
    manualReviewRequired: firstDefined(recommendation.manualReviewRequired, diffSummary.manualReviewRequired, true),
    alertStatus: firstDisplayValue(recommendation.alertStatus, trace.alertStatus, 'NONE'),
    originalSqlText: originalSql,
    recommendedSqlText: recommendedSql,
    ruleChain: normalizeArray(recommendation.ruleChain),
    sourceProblems: normalizeArray(recommendation.sourceProblems),
    issueRuleLinks: normalizeArray(recommendation.issueRuleLinks),
    diffSummary: compactObject({
      diffStatus: firstDisplayValue(diff.diffStatus, diffSummary.diffStatus),
      validationMethod: recommendation.validationMethod,
      changeCount: diffSummary.changeCount,
      ruleDiffCount: diffSummary.ruleDiffCount,
      manualReviewRequired: firstDefined(recommendation.manualReviewRequired, diffSummary.manualReviewRequired),
      autoApplyAllowed: firstDefined(recommendation.autoApplyAllowed, diffSummary.autoApplyAllowed),
      evidenceBoundary: diffSummary.evidenceBoundary
    }),
    risk: compactObject({
      riskLevel: recommendation.riskLevel,
      riskSummary: recommendation.riskSummary,
      manualReviewRequired: firstDefined(recommendation.manualReviewRequired, diffSummary.manualReviewRequired),
      semanticRisks: normalizeArray(recommendation.semanticRisks),
      preconditions: normalizeArray(recommendation.preconditions),
      unappliedRules: normalizeArray(recommendation.unappliedRules)
    }),
    traceRefs: compactObject({
      recommendationId: recommendation.recommendationId,
      sourceType,
      sourceKind,
      sourceId,
      historyId,
      parseTaskId: firstDisplayValue(recommendation.parseTaskId, trace.parseTaskId),
      batchId: firstDisplayValue(recommendation.batchId, trace.batchId),
      reportCode: firstDisplayValue(recommendation.reportCode, trace.reportCode),
      alertId: firstDisplayValue(recommendation.alertId, trace.alertId),
      routeDecisionId: firstDisplayValue(recommendation.routeDecisionId, trace.routeDecisionId),
      sqlFingerprint: firstDisplayValue(recommendation.sqlFingerprint, diff.sqlFingerprint, trace.sqlFingerprint),
      createdFrom: 'RECOMMENDATION_CENTER'
    })
  })
}

const createRewriteRecordAndOpenReview = async () => {
  if (!selectedRecommendation.value) {
    lifecycleErrorMessage.value = t('recommendationCenter.states.selectRecommendation')
    activeDetailTab.value = 'rewriteLifecycle'
    return
  }
  activeDetailTab.value = 'rewriteLifecycle'
  lifecycleErrorMessage.value = ''
  lifecycleSuccessMessage.value = ''
  if (rewriteRecords.value.length) {
    const recordId = selectedRewriteRecordId.value || rewriteRecords.value[0]?.rewriteRecordId || ''
    if (recordId && recordId !== selectedRewriteRecordId.value) {
      await loadRewriteRecordLifecycle(recordId)
    }
    return
  }
  if (!canCreateRewriteRecordFromRecommendation.value) {
    lifecycleErrorMessage.value = t('recommendationCenter.states.rewriteRecordCreateUnavailable')
    return
  }
  loading.lifecycleAction = 'CREATE_RECORD'
  try {
    const created = await createSqlRewriteRecord(buildRewriteRecordCreatePayload(), {
      requestPrefix: 'frontend-recommendation-rewrite-record-create'
    })
    await loadRewriteRecordsForRecommendation(selectedRecommendationId.value, created?.rewriteRecordId || '')
    lifecycleSuccessMessage.value = t('recommendationCenter.states.rewriteRecordCreated')
  } catch (error) {
    lifecycleErrorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lifecycleAction = ''
  }
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

const firstDisplayValue = (...values) => values.find(hasDisplayValue)

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

const summarizeEvidenceItem = item => {
  if (!hasDisplayValue(item)) {
    return '-'
  }
  if (typeof item !== 'object' || Array.isArray(item)) {
    return displayValue(item)
  }
  const primary = firstDisplayValue(item.rule, item.ruleCode, item.code, item.name, item.diffId, item.id, item.type)
  const passed = item.passed === true || item.passed === false ? `passed=${boolText(item.passed)}` : ''
  const secondary = [
    item.status,
    item.action,
    item.severity,
    item.level,
    passed,
    item.reason,
    item.summary,
    item.description,
    item.evidenceLevel
  ]
    .filter(hasDisplayValue)
    .filter(value => String(value) !== String(primary || ''))
  const parts = [primary, ...secondary].filter(hasDisplayValue).map(displayValue)
  return parts.length ? parts.join(' · ') : formatJson(item)
}

const formatJson = value => JSON.stringify(value, null, 2)

onMounted(() => {
  syncRouteQueryState()
  refreshPage()
})

watch(
  () => form.tenantId,
  () => {
    recommendationFilters.sourceObjectId = ''
    sourceObjectOptions.value = []
  }
)

watch(
  () => route.fullPath,
  async () => {
    syncRouteQueryState()
    await refreshPage()
  }
)
</script>

<template>
  <section class="recommendation-page" data-testid="recommendation-page">
    <SectionHeader
      :eyebrow="pageEyebrow"
      :title="pageTitle"
      :summary="pageSummary"
      :level="1"
      size="compact"
    />

    <section class="recommendation-workbench" data-testid="recommendation-list">
      <div class="recommendation-filter-block">
        <SectionHeader
          :eyebrow="t('recommendationCenter.filters.eyebrow')"
          :title="t('recommendationCenter.filters.title')"
          :summary="t('recommendationCenter.list.summary', { count: recommendationPager.totalCount })"
          size="compact"
        />
        <div class="filter-grid">
          <label class="field-block">
            <span class="field-label">{{ t('common.fields.tenant') }}</span>
            <el-input v-model.trim="form.tenantId" data-testid="recommendation-tenant-input" @keyup.enter="refreshRecommendationFromFirstPage" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('recommendationCenter.fields.sourceCategory') }}</span>
            <el-select
              v-model="recommendationFilters.sourceCategory"
              clearable
              data-testid="recommendation-source-category-filter"
              @change="handleSourceCategoryChange"
            >
              <el-option v-for="item in sourceCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-block field-block-wide">
            <span class="field-label">{{ t('recommendationCenter.fields.sourceObject') }}</span>
            <el-select
              v-model="recommendationFilters.sourceObjectId"
              clearable
              filterable
              :disabled="!selectedSourceCategory"
              :loading="loading.sourceOptions"
              data-testid="recommendation-source-object-filter"
              @visible-change="visible => visible && loadSourceObjectOptions()"
            >
              <el-option v-for="item in sourceObjectOptions" :key="item.value" :label="item.label" :value="item.value">
                <span>{{ item.label }}</span>
              </el-option>
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('inline.viewsRecommendationCenterRecommendationCenterView.text005') }}</span>
            <el-select v-model="recommendationFilters.recommendationType" clearable data-testid="recommendation-type-filter">
              <el-option v-for="item in recommendationTypeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.status') }}</span>
            <el-select v-model="recommendationFilters.status" clearable data-testid="recommendation-status-filter">
              <el-option v-for="item in recommendationStatusOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('recommendationCenter.fields.benefitLevel') }}</span>
            <el-select v-model="recommendationFilters.benefitLevel" clearable data-testid="recommendation-benefit-filter">
              <el-option v-for="item in benefitLevelOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('recommendationCenter.fields.riskLevel') }}</span>
            <el-select v-model="recommendationFilters.riskLevel" clearable data-testid="recommendation-risk-filter">
              <el-option v-for="item in riskLevelOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('recommendationCenter.fields.validationStatus') }}</span>
            <el-select v-model="recommendationFilters.validationStatus" clearable data-testid="recommendation-validation-filter">
              <el-option v-for="item in validationStatusOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </label>
          <label class="field-block field-block-compact">
            <span class="field-label">{{ t('recommendationCenter.fields.dispatch') }}</span>
            <el-select v-model="recommendationFilters.requiresDispatch" clearable data-testid="recommendation-dispatch-filter">
              <el-option v-for="item in booleanFilterOptions" :key="`dispatch-${item.label}`" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <label class="field-block field-block-compact">
            <span class="field-label">{{ t('recommendationCenter.fields.manualReviewRequired') }}</span>
            <el-select v-model="recommendationFilters.manualReviewRequired" clearable data-testid="recommendation-manual-review-filter">
              <el-option v-for="item in booleanFilterOptions" :key="`review-${item.label}`" :label="item.label" :value="item.value" />
            </el-select>
          </label>
          <el-button type="primary" :loading="loading.page" data-testid="recommendation-refresh" @click="refreshRecommendationFromFirstPage">
            {{ t('recommendationCenter.actions.refresh') }}
          </el-button>
          <el-button data-testid="recommendation-reset-filters" @click="resetRecommendationFilters">
            {{ t('common.actions.reset') }}
          </el-button>
          <el-button @click="openRoutingGovernance">
            {{ t('recommendationCenter.actions.openRouting') }}
          </el-button>
          <el-button @click="openAccelerationWorkbench">
            {{ t('recommendationCenter.actions.openParse') }}
          </el-button>
        </div>
      </div>

      <p v-if="errorMessage" class="error-banner" data-testid="recommendation-error">{{ errorMessage }}</p>

      <div class="recommendation-list-panel">
        <SectionHeader
          :eyebrow="t('recommendationCenter.list.eyebrow')"
          :title="t('recommendationCenter.list.title')"
          size="compact"
        />

        <el-table
          v-loading="loading.page"
          :data="recommendations"
          row-key="recommendationId"
          highlight-current-row
          data-testid="recommendation-item"
          @row-click="openRecommendationDetail"
          @sort-change="handleRecommendationSortChange"
        >
          <el-table-column prop="recommendationType" :label="t('inline.viewsRecommendationCenterRecommendationCenterView.text005')" min-width="140" sortable="custom" />
          <el-table-column prop="summary" :label="t('recommendationCenter.detail.title')" min-width="260">
            <template #default="{ row }">
              <strong class="table-main-text">{{ row.summary || row.recommendationId }}</strong>
              <span class="table-muted-text">{{ row.expectedGain || row.reason || '-' }}</span>
              <span class="table-muted-text">
                {{ t('recommendationCenter.fields.sourceKind') }}:
                {{ displayValue(firstDisplayValue(row.sourceKind, row.sourceType)) }}
                · {{ t('recommendationCenter.fields.sourceId') }}:
                {{ displayValue(firstDisplayValue(row.sourceId, row.reportCode, row.historyId, row.logicalObjectKey)) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="130" sortable="custom">
            <template #default="{ row }">
              <el-tag :type="row.status === 'FAILED' ? 'danger' : 'info'">{{ row.status || 'UNKNOWN' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="benefitLevel" :label="t('recommendationCenter.fields.benefitLevel')" min-width="120" sortable="custom" />
          <el-table-column prop="riskLevel" :label="t('recommendationCenter.fields.riskLevel')" min-width="120" sortable="custom">
            <template #default="{ row }">
              <el-tag :type="['HIGH', 'CRITICAL'].includes(String(row.riskLevel || '').toUpperCase()) ? 'warning' : 'info'">
                {{ row.riskLevel || 'UNKNOWN' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="validationStatus" :label="t('recommendationCenter.fields.validationStatus')" min-width="150" sortable="custom" />
          <el-table-column prop="requiresDispatch" :label="t('recommendationCenter.fields.dispatch')" min-width="120" sortable="custom">
            <template #default="{ row }">{{ boolText(row.requiresDispatch) || 'false' }}</template>
          </el-table-column>
          <el-table-column prop="manualReviewRequired" :label="t('recommendationCenter.fields.manualReviewRequired')" min-width="170" sortable="custom">
            <template #default="{ row }">{{ boolText(row.manualReviewRequired) || 'false' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" :label="t('recommendationCenter.fields.createdAt')" min-width="170" sortable="custom" />
          <el-table-column prop="recommendationId" :label="t('accelerationGovernanceWorkbench.fields.recommendationId')" min-width="190" sortable="custom" />
        </el-table>

        <el-pagination
          v-if="recommendationPager.totalCount > recommendationPager.size"
          v-model:current-page="recommendationPager.page"
          background
          data-testid="recommendation-pagination"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[8, 16, 32, 64]"
          :page-size="recommendationPager.size"
          :total="recommendationPager.totalCount"
          @current-change="handleRecommendationPageChange"
          @size-change="handleRecommendationSizeChange"
        />
      </div>
    </section>

    <el-drawer
      v-model="recommendationDetailDrawerVisible"
      data-testid="recommendation-detail-drawer"
      size="72%"
      :title="selectedRecommendation?.recommendationId || t('recommendationCenter.detail.title')"
      @closed="handleRecommendationDrawerClosed"
    >
      <div class="detail-drawer-body" data-testid="recommendation-detail">
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
          <section class="focus-summary" data-testid="recommendation-focus-summary">
            <div class="evidence-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ t('recommendationCenter.sections.focusSummary') }}</p>
                <h3>{{ selectedRecommendation.summary || selectedRecommendation.recommendationId }}</h3>
              </div>
              <div class="pane-actions">
                <el-tag :type="requiresReviewGuard ? 'warning' : 'success'">
                  {{ requiresReviewGuard ? t('recommendationCenter.fields.manualReviewRequired') : displayValue(selectedRecommendation.validationStatus) }}
                </el-tag>
                <el-button
                  v-if="isRewriteReviewCandidate"
                  type="primary"
                  :disabled="!rewriteRecords.length && !canCreateRewriteRecordFromRecommendation"
                  :loading="loading.lifecycleAction === 'CREATE_RECORD'"
                  data-testid="recommendation-create-rewrite-record"
                  @click="createRewriteRecordAndOpenReview"
                >
                  {{ rewriteRecordEntryActionText }}
                </el-button>
              </div>
            </div>
            <dl class="description-grid">
              <div v-for="item in focusSummaryCards" :key="item.key" class="description-item">
                <dt>{{ item.label }}</dt>
                <dd>{{ displayValue(item.value) }}</dd>
              </div>
            </dl>
          </section>

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
              <section v-if="sourceProblemRows.length" class="evidence-table" data-testid="recommendation-source-problems">
                <div class="evidence-heading">
                  <h3>{{ t('rewriteTrial.sourceProblems') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('rewriteTrial.sourceProblems'), sourceProblemRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <el-table :data="sourceProblemRows" border>
                  <el-table-column prop="issueScene" :label="t('rewriteTrial.issueScene')" min-width="180" />
                  <el-table-column prop="problemType" :label="t('rewriteTrial.problemType')" min-width="150" />
                  <el-table-column prop="severity" :label="t('rewriteTrial.severity')" min-width="110" />
                  <el-table-column prop="priorityLevel" :label="t('rewriteTrial.priority')" min-width="100" />
                  <el-table-column prop="summary" :label="t('rewriteTrial.summary')" min-width="260" show-overflow-tooltip />
                </el-table>
              </section>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.sqlDiff')" name="sqlDiff">
              <div data-testid="recommendation-sql-diff">
                <p v-if="diffErrorMessage" class="error-banner" data-testid="recommendation-diff-error">{{ diffErrorMessage }}</p>
                <p v-if="!recommendationDiff && !hasFrontendCompareSql && !diffErrorMessage" class="muted-copy">
                  {{ t('recommendationCenter.states.emptyDiff') }}
                </p>
                <dl v-if="recommendationDiff" class="description-grid">
                  <div v-for="item in diffSummaryCards" :key="item.key" class="description-item">
                    <dt>{{ item.label }}</dt>
                    <dd>{{ displayValue(item.value) }}</dd>
                  </div>
                </dl>
                <section v-if="hasFrontendCompareSql" class="evidence-table">
                  <div class="evidence-heading">
                    <h3>{{ t('recommendationCenter.sections.compareView') }}</h3>
                    <el-button
                      v-if="textDiffRows.length"
                      @click="openEvidenceDrawer(t('recommendationCenter.sections.textDiff'), textDiffRows)"
                    >
                      {{ t('common.actions.viewRawEvidence') }}
                    </el-button>
                  </div>
                  <SqlCompareBlock
                    :original-sql="frontendCompareOriginalSql"
                    :recommended-sql="frontendCompareRecommendedSql"
                    :original-label="t('recommendationCenter.fields.originalSql')"
                    :recommended-label="t('recommendationCenter.fields.recommendedSql')"
                    :empty-text="t('recommendationCenter.states.noDiffHunks')"
                    :copy-label="t('common.actions.copy')"
                    :format-label="t('common.actions.format')"
                    data-testid="recommendation-sql-compare"
                  />
                </section>
                <section v-if="recommendationDiff" class="evidence-table" data-testid="recommendation-ast-summary-diff">
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
              </div>
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
                <p v-if="!ruleChainRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <ul v-else class="evidence-list" data-testid="recommendation-rule-chain-summary">
                  <li v-for="(item, index) in ruleChainRows" :key="`rule-chain-${index}`">
                    {{ summarizeEvidenceItem(item) }}
                  </li>
                </ul>
              </section>
              <section v-if="issueRuleLinkRows.length" class="evidence-table" data-testid="recommendation-issue-rule-links">
                <div class="evidence-heading">
                  <h3>{{ t('rewriteTrial.issueRuleLinks') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('rewriteTrial.issueRuleLinks'), issueRuleLinkRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <el-table :data="issueRuleLinkRows" border>
                  <el-table-column prop="sourceIssueScene" :label="t('rewriteTrial.sourceProblems')" min-width="180" />
                  <el-table-column prop="ruleCode" :label="t('rewriteTrial.ruleCode')" min-width="180" />
                  <el-table-column prop="ruleLevel" :label="t('rewriteTrial.ruleLevel')" min-width="90" />
                  <el-table-column prop="ruleAction" :label="t('rewriteTrial.ruleAction')" min-width="120" />
                  <el-table-column prop="trialConclusion" :label="t('rewriteTrial.trialConclusion')" min-width="170" />
                  <el-table-column prop="riskReason" :label="t('rewriteTrial.riskReason')" min-width="220" show-overflow-tooltip />
                </el-table>
              </section>
              <section class="evidence-table" data-testid="recommendation-preconditions">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.preconditions') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.preconditions'), preconditionRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p v-if="!preconditionRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <ul v-else class="evidence-list" data-testid="recommendation-preconditions-summary">
                  <li v-for="(item, index) in preconditionRows" :key="`precondition-${index}`">
                    {{ summarizeEvidenceItem(item) }}
                  </li>
                </ul>
              </section>
              <section class="evidence-table" data-testid="recommendation-semantic-risks">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.semanticRisks') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.semanticRisks'), semanticRiskRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p v-if="!semanticRiskRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <ul v-else class="evidence-list" data-testid="recommendation-semantic-risks-summary">
                  <li v-for="(item, index) in semanticRiskRows" :key="`semantic-risk-${index}`">
                    {{ summarizeEvidenceItem(item) }}
                  </li>
                </ul>
              </section>
              <section class="evidence-table" data-testid="recommendation-unapplied-rules">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.unappliedRules') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.unappliedRules'), unappliedRuleRows)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <p v-if="!unappliedRuleRows.length" class="muted-copy">{{ t('recommendationCenter.states.noRuleEvidence') }}</p>
                <ul v-else class="evidence-list" data-testid="recommendation-unapplied-rules-summary">
                  <li v-for="(item, index) in unappliedRuleRows" :key="`unapplied-rule-${index}`">
                    {{ summarizeEvidenceItem(item) }}
                  </li>
                </ul>
              </section>
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
                  :value="frontendCompareRecommendedSql"
                  :label="t('recommendationCenter.fields.recommendedSql')"
                  :copy-label="t('common.actions.copy')"
                  compact
                  data-testid="recommendation-recommended-sql"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane :label="t('recommendationCenter.tabs.rewriteLifecycle')" name="rewriteLifecycle">
              <section class="rewrite-lifecycle" data-testid="recommendation-rewrite-lifecycle">
                <div class="evidence-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ t('recommendationCenter.rewriteLifecycle.eyebrow') }}</p>
                    <h3>{{ t('recommendationCenter.rewriteLifecycle.title') }}</h3>
                  </div>
                  <el-tag :type="rewriteLifecycleState.type" data-testid="recommendation-rewrite-lifecycle-state">
                    {{ rewriteLifecycleState.label }}
                  </el-tag>
                </div>
                <p class="muted-copy">{{ t('recommendationCenter.rewriteLifecycle.boundary') }}</p>

                <div class="filter-grid">
                  <label class="field-block">
                    <span class="field-label">{{ t('recommendationCenter.fields.rewriteRecordId') }}</span>
                    <el-select
                      v-model="selectedRewriteRecordId"
                      :disabled="!rewriteRecordOptions.length"
                      :loading="loading.lifecycle"
                      filterable
                      data-testid="recommendation-rewrite-record-select"
                      @change="loadRewriteRecordLifecycle"
                    >
                      <el-option
                        v-for="item in rewriteRecordOptions"
                        :key="item.value"
                        :label="`${item.label} ${item.status}`"
                        :value="item.value"
                      />
                    </el-select>
                  </label>
                  <el-button
                    :loading="loading.lifecycle"
                    data-testid="recommendation-rewrite-record-refresh"
                    @click="loadRewriteRecordsForRecommendation(selectedRecommendationId, selectedRewriteRecordId)"
                  >
                    {{ t('recommendationCenter.actions.refreshRewriteRecords') }}
                  </el-button>
                </div>

                <p v-if="lifecycleErrorMessage" class="error-banner" data-testid="recommendation-rewrite-lifecycle-error">
                  {{ lifecycleErrorMessage }}
                </p>
                <p v-if="lifecycleSuccessMessage" class="success-banner" data-testid="recommendation-rewrite-lifecycle-success">
                  {{ lifecycleSuccessMessage }}
                </p>
                <p v-if="!rewriteRecords.length && !loading.lifecycle" class="muted-copy" data-testid="recommendation-rewrite-empty">
                  {{ t('recommendationCenter.states.noRewriteRecord') }}
                </p>
                <el-button
                  v-if="!rewriteRecords.length && isRewriteReviewCandidate"
                  type="primary"
                  :disabled="!canCreateRewriteRecordFromRecommendation"
                  :loading="loading.lifecycleAction === 'CREATE_RECORD'"
                  data-testid="recommendation-empty-create-rewrite-record"
                  @click="createRewriteRecordAndOpenReview"
                >
                  {{ t('recommendationCenter.actions.createRewriteRecordAndReview') }}
                </el-button>

                <template v-if="selectedRewriteRecord">
                  <dl class="description-grid" data-testid="recommendation-rewrite-lifecycle-status">
                    <div v-for="item in rewriteLifecycleCards" :key="item.key" class="description-item">
                      <dt>{{ item.label }}</dt>
                      <dd>{{ displayValue(item.value) }}</dd>
                    </div>
                  </dl>

                  <div class="rewrite-action-grid" data-testid="recommendation-rewrite-lifecycle-actions">
                    <label class="field-block">
                      <span class="field-label">{{ t('recommendationCenter.fields.reviewNote') }}</span>
                      <el-input
                        v-model="rewriteActionForm.reviewNote"
                        type="textarea"
                        :rows="3"
                        data-testid="recommendation-rewrite-review-note"
                      />
                    </label>
                    <label class="field-block">
                      <span class="field-label">{{ t('recommendationCenter.fields.actionReason') }}</span>
                      <el-input
                        v-model="rewriteActionForm.actionReason"
                        data-testid="recommendation-rewrite-action-reason"
                      />
                    </label>
                    <div class="pane-actions">
                      <el-button
                        type="success"
                        :disabled="!canApproveRewrite"
                        :loading="loading.lifecycleAction === 'APPROVE'"
                        data-testid="recommendation-rewrite-approve"
                        @click="performRewriteLifecycleAction('APPROVE')"
                      >
                        {{ t('recommendationCenter.actions.approveRewrite') }}
                      </el-button>
                      <el-button
                        type="danger"
                        :disabled="!canRejectRewrite"
                        :loading="loading.lifecycleAction === 'REJECT'"
                        data-testid="recommendation-rewrite-reject"
                        @click="performRewriteLifecycleAction('REJECT')"
                      >
                        {{ t('recommendationCenter.actions.rejectRewrite') }}
                      </el-button>
                      <el-button
                        type="primary"
                        :disabled="!canPublishRewrite"
                        :loading="loading.lifecycleAction === 'PUBLISH'"
                        data-testid="recommendation-rewrite-publish"
                        @click="performRewriteLifecycleAction('PUBLISH')"
                      >
                        {{ t('recommendationCenter.actions.publishRewrite') }}
                      </el-button>
                      <el-button
                        :disabled="!canPauseRewrite"
                        :loading="loading.lifecycleAction === 'PAUSE'"
                        data-testid="recommendation-rewrite-pause"
                        @click="performRewriteLifecycleAction('PAUSE')"
                      >
                        {{ t('recommendationCenter.actions.pauseRewrite') }}
                      </el-button>
                      <el-button
                        :disabled="!canUnpublishRewrite"
                        :loading="loading.lifecycleAction === 'UNPUBLISH'"
                        data-testid="recommendation-rewrite-unpublish"
                        @click="performRewriteLifecycleAction('UNPUBLISH')"
                      >
                        {{ t('recommendationCenter.actions.unpublishRewrite') }}
                      </el-button>
                    </div>
                  </div>

                  <section class="evidence-table" data-testid="recommendation-rewrite-publish-eligibility">
                    <div class="evidence-heading">
                      <h3>{{ t('recommendationCenter.sections.publishEligibility') }}</h3>
                      <el-tag :type="rewritePublishEligibility?.eligible ? 'success' : 'warning'">
                        {{ displayValue(boolText(rewritePublishEligibility?.eligible)) }}
                      </el-tag>
                    </div>
                    <dl class="description-grid">
                      <div v-for="item in publishEligibilityCards" :key="item.key" class="description-item">
                        <dt>{{ item.label }}</dt>
                        <dd>{{ displayValue(item.value) }}</dd>
                      </div>
                    </dl>
                    <el-table
                      v-if="publishRefusalReasons.length"
                      :data="publishRefusalReasons"
                      row-key="code"
                      data-testid="recommendation-rewrite-refusal-reasons"
                    >
                      <el-table-column prop="code" :label="t('recommendationCenter.fields.refusalCode')" min-width="170" />
                      <el-table-column prop="message" :label="t('recommendationCenter.fields.refusalMessage')" min-width="260" />
                      <el-table-column prop="field" :label="t('recommendationCenter.fields.refusalField')" min-width="150" />
                      <el-table-column prop="evidenceRef" :label="t('recommendationCenter.fields.evidenceRef')" min-width="170" />
                      <el-table-column prop="blocking" :label="t('recommendationCenter.fields.blocking')" min-width="110">
                        <template #default="{ row }">{{ boolText(row.blocking) }}</template>
                      </el-table-column>
                    </el-table>
                    <p v-else class="muted-copy" data-testid="recommendation-rewrite-no-refusal-reasons">
                      {{ t('recommendationCenter.states.noRefusalReasons') }}
                    </p>
                  </section>

                  <section class="evidence-table" data-testid="recommendation-rewrite-validation-runs">
                    <div class="evidence-heading">
                      <h3>{{ t('recommendationCenter.sections.validationRuns') }}</h3>
                      <el-button
                        :loading="loading.validationRuns"
                        data-testid="recommendation-rewrite-validation-runs-refresh"
                        @click="loadRewriteValidationRuns(selectedRewriteRecordId)"
                      >
                        {{ t('recommendationCenter.actions.refreshValidationRuns') }}
                      </el-button>
                    </div>
                    <p v-if="validationRunErrorMessage" class="error-banner" data-testid="recommendation-rewrite-validation-runs-error">
                      {{ validationRunErrorMessage }}
                    </p>
                    <el-table
                      v-loading="loading.validationRuns"
                      :data="rewriteValidationRuns"
                      row-key="validationRunId"
                      data-testid="recommendation-rewrite-validation-run-table"
                    >
                      <el-table-column prop="validationRunId" :label="t('recommendationCenter.fields.validationRunId')" min-width="190" />
                      <el-table-column prop="status" :label="t('recommendationCenter.fields.status')" min-width="120" />
                      <el-table-column prop="comparisonStatus" :label="t('recommendationCenter.fields.comparisonStatus')" min-width="160" />
                      <el-table-column prop="differenceType" :label="t('recommendationCenter.fields.differenceType')" min-width="160" />
                      <el-table-column prop="autoApplyPaused" :label="t('recommendationCenter.fields.autoApplyPaused')" min-width="150">
                        <template #default="{ row }">{{ boolText(row.autoApplyPaused) }}</template>
                      </el-table-column>
                      <el-table-column prop="startedAt" :label="t('recommendationCenter.fields.startedAt')" min-width="170" />
                      <el-table-column prop="finishedAt" :label="t('recommendationCenter.fields.finishedAt')" min-width="170" />
                    </el-table>
                    <p
                      v-if="!rewriteValidationRuns.length && !loading.validationRuns"
                      class="muted-copy"
                      data-testid="recommendation-rewrite-validation-runs-empty"
                    >
                      {{ t('recommendationCenter.states.noValidationRuns') }}
                    </p>
                  </section>
                </template>
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
    </el-drawer>

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

.recommendation-workbench,
.recommendation-list-panel,
.detail-drawer-body {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
}

.recommendation-workbench {
  padding: var(--sqlforge-space-5);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-2);
}

.recommendation-filter-block {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
  padding-bottom: var(--sqlforge-space-4);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.detail-drawer-body {
  padding: 0 var(--sqlforge-space-2) var(--sqlforge-space-5);
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

.field-block-compact {
  min-width: 170px;
}

.field-block-wide {
  min-width: 300px;
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

.success-banner {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid rgba(62, 207, 142, 0.35);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(21, 98, 73, 0.2);
  color: #bdf6dd;
}

.detail-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.detail-tabs :deep(.el-tabs__nav-wrap) {
  min-width: 0;
}

.detail-tabs :deep(.el-tabs__nav-scroll) {
  overflow-x: auto;
  scrollbar-width: thin;
}

.detail-tabs :deep(.el-tabs__nav) {
  white-space: nowrap;
}

.detail-tabs :deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
}

.focus-summary,
.review-guard,
.rewrite-lifecycle,
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

.rewrite-action-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr);
  gap: var(--sqlforge-space-3);
  align-items: end;
}

.rewrite-action-grid .pane-actions {
  grid-column: 1 / -1;
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

.evidence-list {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.evidence-list li {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--sqlforge-border-subtle);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  line-height: 1.55;
  overflow-wrap: anywhere;
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

:deep(.sql-token-keyword) {
  color: var(--sqlforge-color-brand);
  font-weight: 800;
}

:deep(.sql-token-identifier) {
  color: #d7d7d7;
}

:deep(.sql-token-literal) {
  color: #f0b86e;
  font-weight: 600;
}

:deep(.sql-token-number) {
  color: #9bc8ff;
  font-weight: 700;
}

:deep(.sql-token-comment) {
  color: #7f8a8a;
  font-style: italic;
}

:deep(.sql-token-operator) {
  color: #d6a7ff;
  font-weight: 700;
}

@media (max-width: 1280px) {
  .sql-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .filter-grid,
  .pane-actions,
  .review-guard {
    flex-direction: column;
    align-items: stretch;
  }

  .rewrite-action-grid {
    grid-template-columns: 1fr;
  }

  .detail-tabs :deep(.el-tabs__nav) {
    transform: none !important;
  }

  .detail-tabs :deep(.el-tabs__item) {
    padding-inline: var(--sqlforge-space-3);
  }
}
</style>
