<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  applyAccelerationPlan,
  createAccelerationCandidate,
  createRewriteValidationRun,
  createSqlRewriteRecord,
  executeQuery,
  formatRuntimeError,
  getAccelerationCandidate,
  getAccelerationCandidates,
  getAccelerationPlan,
  getOptimizationTaskStatus,
  getQueryHistoryRewriteRecords,
  getRecommendationDiff,
  getRewriteValidationRuns,
  getSqlRewriteRecords,
  reviewAccelerationPlan,
  rollbackAccelerationPlan,
  submitAccelerationPlan,
  submitOptimizationTask,
  verifyAccelerationPlan
} from '../../services/runtimeGateApi'
import SectionHeader from '../common/SectionHeader.vue'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import ToolbarShell from '../common/ToolbarShell.vue'

const { t } = useI18n()
const router = useRouter()

const sourceModeConfig = {
  PARSE: {
    sourceKinds: ['STRUCTURE_PARSE', 'COMBINED_PARSE', 'PARSE_BATCH', 'REPORT_BATCH', 'END_OF_DAY_SLOW_SQL'],
    evidenceLevels: ['STATIC_PARSE', 'ACCESS_PARSE', 'MIXED'],
    defaultSourceKind: 'COMBINED_PARSE',
    defaultEvidenceLevel: 'STATIC_PARSE'
  },
  QUERY: {
    sourceKinds: ['QUERY_HISTORY', 'SLOW_SQL', 'HIGH_P99', 'HIGH_SCAN', 'BENCHMARK_REGRESSION', 'MANUAL'],
    evidenceLevels: ['RUNTIME_HISTORY', 'EXPLAIN_PLAN', 'BENCHMARK', 'MIXED'],
    defaultSourceKind: 'QUERY_HISTORY',
    defaultEvidenceLevel: 'RUNTIME_HISTORY'
  }
}

const datasourceTypeOptions = ['HETU', 'HIVE', 'SPARK', 'CLICKHOUSE', 'GAUSSDB', 'AUTO']
const suggestionTypeOptions = ['PRECOMPUTE', 'PARTITION', 'BUCKET', 'SPLIT', 'REPLACE']

// Static contract tokens: ENTRY_EVIDENCE, CANDIDATE_SUGGESTION, SQL_DIFF, PLAN_APPROVAL, APPLY_VALIDATION, MONITORING_ALERT, ROLLBACK_DISCARD.

const form = reactive({
  tenantId: 'tenant-a',
  datasourceCode: 'hetu_main',
  datasourceType: 'HETU',
  schemaName: 'dwd',
  stage: 'PROD',
  reportCode: 'RPT_SALES_DAILY',
  sourceType: 'PARSE',
  sourceKind: 'COMBINED_PARSE',
  sourceId: 'parse-history-001',
  parseHistoryId: 'parse-history-001',
  historyId: '',
  sqlFingerprint: 'fp_sales_daily_20260401',
  evidenceLevel: 'STATIC_PARSE',
  enableHetuExplain: false,
  sqlText: "SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id",
  candidateId: '',
  recommendationId: '',
  sourceTaskId: '',
  planId: '',
  rewriteRecordId: '',
  validationStatus: '',
  reviewNote: 'HARN-138 governed workbench review',
  actionReason: 'HARN-138 governed workbench smoke',
  selectedSuggestionTypes: ['PRECOMPUTE', 'PARTITION']
})

const loading = reactive({
  candidate: false,
  suggestion: false,
  diff: false,
  plan: false,
  approval: false,
  validation: false,
  monitoring: false,
  query: false
})

const activeTab = ref('candidates')
const sourceEditorVisible = ref(false)
const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)
const errorMessage = ref('')
const lastEvidence = ref(null)
const candidateList = ref([])
const candidateResponse = ref(null)
const suggestionTask = ref(null)
const suggestionTaskStatus = ref(null)
const diffResponse = ref(null)
const planResponse = ref(null)
const rewriteRecordResponse = ref(null)
const rewriteRecords = ref([])
const historyRewriteRecords = ref(null)
const validationRuns = ref([])
const validationRunResponse = ref(null)
const baselineResult = ref(null)
const acceleratedResult = ref(null)
const candidatePager = reactive({
  page: 1,
  size: 8
})
const rewritePager = reactive({
  page: 1,
  size: 6
})
const historyRewritePager = reactive({
  page: 1,
  size: 6
})
const validationRunPager = reactive({
  page: 1,
  size: 6
})

const sourceModeOptions = computed(() => [
  { value: 'PARSE', label: t('accelerationGovernanceWorkbench.modes.parse') },
  { value: 'QUERY', label: t('accelerationGovernanceWorkbench.modes.query') }
])
const sourceKindOptions = computed(() => sourceModeConfig[form.sourceType]?.sourceKinds || [])
const evidenceLevelOptions = computed(() => sourceModeConfig[form.sourceType]?.evidenceLevels || [])

const resolvedSourceId = computed(() => {
  if (hasValue(form.sourceId)) {
    return form.sourceId
  }
  if (form.sourceType === 'PARSE') {
    return form.parseHistoryId || form.reportCode
  }
  return form.historyId
})

const selectedPlanStatus = computed(() => String(planResponse.value?.status || '').toUpperCase())
const selectedValidationStatus = computed(() => {
  const recordStatus = rewriteRecordResponse.value?.validationStatus
  const firstRecordStatus = rewriteRecords.value[0]?.validationStatus
  const runStatus = validationRunResponse.value?.comparisonStatus
  return String(recordStatus || firstRecordStatus || runStatus || '').toUpperCase()
})
const selectedSourceTaskId = computed(() => form.sourceTaskId || suggestionTask.value?.taskId || '')
const canSubmitSource = computed(() => hasValue(form.tenantId) && hasValue(resolvedSourceId.value))
const canSubmitSuggestion = computed(() => hasValue(form.sqlText) || hasValue(form.sqlFingerprint))
const canSubmitPlan = computed(() => hasValue(form.tenantId) && hasValue(selectedSourceTaskId.value))
const canLoadDiff = computed(() => hasValue(form.recommendationId))
const canCreateRewriteRecord = computed(() => hasValue(diffResponse.value?.originalSql) && hasValue(diffResponse.value?.recommendedSql))
const canApplyPlan = computed(() => ['APPROVED', 'APPLY_FAILED'].includes(selectedPlanStatus.value))
const canVerifyPlan = computed(() => ['APPLIED', 'VERIFY_FAILED', 'VERIFIED'].includes(selectedPlanStatus.value))
const canRollbackPlan = computed(() => ['APPLIED', 'VERIFY_FAILED', 'VERIFIED', 'ROLLBACK_FAILED'].includes(selectedPlanStatus.value))
const diffAccelerationArtifact = computed(() => diffResponse.value?.accelerationArtifact || null)
const diffAccelerationArtifactCards = computed(() => {
  const artifact = diffAccelerationArtifact.value
  if (!artifact) {
    return []
  }
  return [
    field('artifactStatus', t('recommendationCenter.fields.artifactStatus'), artifact.artifactStatus),
    field('mvName', t('recommendationCenter.fields.mvName'), artifact.mvName),
    field('targetEngine', t('accelerationGovernanceWorkbench.fields.datasourceType'), artifact.targetEngine),
    field('dialect', t('recommendationCenter.fields.dialect'), artifact.dialect),
    field('refreshStrategy', t('recommendationCenter.fields.refreshStrategy'), artifact.refreshStrategy),
    field('runtimeRewriteBinding', t('recommendationCenter.fields.runtimeRewriteBinding'), artifact.runtimeRewriteBinding)
  ]
})
const diffAccelerationArtifactSqlBlocks = computed(() => {
  const artifact = diffAccelerationArtifact.value || {}
  return [
    field('ddlSql', t('recommendationCenter.fields.ddlSql'), artifact.ddlSql),
    field('refreshSql', t('recommendationCenter.fields.refreshSql'), artifact.refreshSql),
    field('validationSql', t('recommendationCenter.fields.validationSql'), artifact.validationSql),
    field('rollbackSql', t('recommendationCenter.fields.rollbackSql'), artifact.rollbackSql),
    field('rewriteSql', t('recommendationCenter.fields.rewriteSql'), artifact.rewriteSql)
  ].filter(item => hasValue(item.value) && item.value !== '-')
})

const sourceSummaryText = computed(() =>
  t('accelerationGovernanceWorkbench.states.sourceSummary', {
    sourceType: form.sourceType,
    sourceKind: form.sourceKind,
    sourceId: resolvedSourceId.value || '-',
    evidenceLevel: form.evidenceLevel
  })
)

const sourceSummaryRows = computed(() => [
  field('tenantId', t('accelerationGovernanceWorkbench.fields.tenantId'), form.tenantId),
  field('datasourceCode', t('accelerationGovernanceWorkbench.fields.datasourceCode'), form.datasourceCode),
  field('stage', t('accelerationGovernanceWorkbench.fields.stage'), form.stage),
  field('sourceType', t('accelerationGovernanceWorkbench.fields.sourceType'), form.sourceType),
  field('sourceKind', t('accelerationGovernanceWorkbench.fields.sourceKind'), form.sourceKind),
  field('sourceId', t('accelerationGovernanceWorkbench.fields.sourceId'), resolvedSourceId.value),
  field('sqlFingerprint', t('accelerationGovernanceWorkbench.fields.sqlFingerprint'), form.sqlFingerprint),
  field('evidenceLevel', t('accelerationGovernanceWorkbench.fields.evidenceLevel'), form.evidenceLevel)
])

const runtimeSummaryRows = computed(() => [
  field('candidateId', t('accelerationGovernanceWorkbench.fields.candidateId'), form.candidateId || candidateResponse.value?.candidateId),
  field('sourceTaskId', t('accelerationGovernanceWorkbench.fields.sourceTaskId'), selectedSourceTaskId.value),
  field('recommendationId', t('accelerationGovernanceWorkbench.fields.recommendationId'), form.recommendationId),
  field('planId', t('accelerationGovernanceWorkbench.fields.planId'), form.planId || planResponse.value?.planId),
  field('planStatus', t('accelerationGovernanceWorkbench.fields.planStatus'), friendlyPlanStatus(selectedPlanStatus.value)),
  field('rewriteRecordId', t('accelerationGovernanceWorkbench.fields.rewriteRecordId'), form.rewriteRecordId || rewriteRecordResponse.value?.rewriteRecordId),
  field('validationStatus', t('accelerationGovernanceWorkbench.fields.validationStatus'), selectedValidationStatus.value),
  field('lastAction', t('accelerationGovernanceWorkbench.fields.lastAction'), lastEvidence.value?.action)
])

const sourceEvidence = computed(() => ({
  contractStage: 'HARN-138_REAL_INTERFACE_TABS',
  sourceType: form.sourceType,
  sourceKind: form.sourceKind,
  sourceId: resolvedSourceId.value,
  parseHistoryId: form.parseHistoryId,
  historyId: form.historyId,
  sqlFingerprint: form.sqlFingerprint,
  tenantId: form.tenantId,
  datasourceCode: form.datasourceCode,
  datasourceType: form.datasourceType,
  schemaName: form.schemaName,
  stage: form.stage,
  reportCode: form.reportCode,
  evidenceLevel: form.evidenceLevel,
  enableHetuExplain: form.enableHetuExplain,
  shellOnly: false,
  submitted: Boolean(lastEvidence.value)
}))

const routeTargets = computed(() => [
  {
    key: 'parseRecord',
    label: t('accelerationGovernanceWorkbench.actions.openParseRecord'),
    path: ROUTE_PATHS.parseRecord,
    query: compactObject({
      tenantId: form.tenantId,
      parseHistoryId: form.parseHistoryId,
      reportId: form.reportCode
    }),
    disabled: !hasValue(form.parseHistoryId) && !hasValue(form.reportCode)
  },
  {
    key: 'sqlHistory',
    label: t('accelerationGovernanceWorkbench.actions.openSqlHistory'),
    path: ROUTE_PATHS.sqlHistory,
    query: compactObject({
      tenantId: form.tenantId,
      historyId: form.historyId
    }),
    disabled: !hasValue(form.historyId)
  },
  {
    key: 'recommendationCenter',
    label: t('accelerationGovernanceWorkbench.actions.openRecommendationCenter'),
    path: ROUTE_PATHS.recommendationCenter,
    query: compactObject({ tenantId: form.tenantId, recommendationId: form.recommendationId }),
    disabled: false
  },
  {
    key: 'sqlQuery',
    label: t('accelerationGovernanceWorkbench.actions.openSqlQuery'),
    path: ROUTE_PATHS.sqlQuery,
    query: compactObject({ tenantId: form.tenantId }),
    disabled: !hasValue(form.sqlText)
  },
  {
    key: 'alertCenter',
    label: t('accelerationGovernanceWorkbench.actions.openAlertCenter'),
    path: ROUTE_PATHS.alertCenter,
    query: compactObject({
      tenantId: form.tenantId,
      alertType: 'SQL_REWRITE_RESULT_DIVERGENCE',
      alertStatus: 'OPEN',
      recommendationId: form.recommendationId,
      historyId: form.historyId,
      rewriteRecordId: form.rewriteRecordId || rewriteRecordResponse.value?.rewriteRecordId,
      validationRunId: validationRunResponse.value?.validationRunId || validationRuns.value[0]?.validationRunId,
      sqlFingerprint: form.sqlFingerprint
    }),
    disabled: false
  }
])

const interfaceActions = computed(() => [
  action('candidates', 'POST', '/api/sql-optimization/acceleration-candidates', canSubmitSource.value),
  action('candidates', 'GET', '/api/sql-optimization/acceleration-candidates', hasValue(form.tenantId)),
  action('candidates', 'POST', '/api/sql-optimization/tasks', canSubmitSuggestion.value),
  action('diff', 'GET', '/api/sql-optimization/recommendations/{recommendationId}/diff', canLoadDiff.value),
  action('approval', 'POST', '/api/sql-optimization/acceleration-plans', canSubmitPlan.value),
  action('approval', 'POST', '/api/sql-optimization/acceleration-plans/{planId}/approval', hasValue(form.planId)),
  action('validation', 'POST', '/api/sql-optimization/acceleration-plans/{planId}/apply', canApplyPlan.value),
  action('validation', 'POST', '/api/sql-optimization/acceleration-plans/{planId}/verify', canVerifyPlan.value),
  action('validation', 'POST', '/api/sql-optimization/acceleration-plans/{planId}/rollback', canRollbackPlan.value),
  action('monitoring', 'GET', '/api/governance/query-history/{historyId}/rewrite-records', hasValue(form.historyId)),
  action('monitoring', 'POST', '/api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs', hasValue(form.rewriteRecordId)),
  action('evidence', 'POST', '/api/query-execution/queries/execute', canSubmitSuggestion.value)
])

const tabDefinitions = computed(() => [
  tab('candidates', t('accelerationGovernanceWorkbench.tabs.candidates')),
  tab('diff', t('accelerationGovernanceWorkbench.tabs.diff')),
  tab('approval', t('accelerationGovernanceWorkbench.tabs.approval')),
  tab('validation', t('accelerationGovernanceWorkbench.tabs.validation')),
  tab('monitoring', t('accelerationGovernanceWorkbench.tabs.monitoring')),
  tab('evidence', t('accelerationGovernanceWorkbench.tabs.evidence'))
])

const candidateRows = computed(() => {
  if (candidateList.value.length) {
    return candidateList.value
  }
  return candidateResponse.value ? [candidateResponse.value] : []
})

const pagedCandidateRows = computed(() => paginate(candidateRows.value, candidatePager))

const rewriteRecordRows = computed(() => {
  if (rewriteRecords.value.length) {
    return rewriteRecords.value
  }
  return rewriteRecordResponse.value ? [rewriteRecordResponse.value] : []
})

const pagedRewriteRecordRows = computed(() => paginate(rewriteRecordRows.value, rewritePager))

const historyRewriteRecordRows = computed(() => historyRewriteRecords.value?.items || [])
const pagedHistoryRewriteRecordRows = computed(() => paginate(historyRewriteRecordRows.value, historyRewritePager))
const pagedValidationRuns = computed(() => paginate(validationRuns.value, validationRunPager))
const monitoringPauseEvidenceRows = computed(() => {
  const rows = []
  for (const record of [...rewriteRecordRows.value, ...historyRewriteRecordRows.value]) {
    const divergenceAlert = record?.traceRefs?.divergenceAlert || record?.traceRefs?.alertRefs || record?.alertRefs
    if (record?.alertStatus && record.alertStatus !== 'NONE') {
      rows.push({
        evidenceType: 'rewriteRecordAlert',
        rewriteRecordId: record.rewriteRecordId,
        recommendationId: record.recommendationId,
        validationStatus: record.validationStatus,
        alertStatus: record.alertStatus,
        lastValidationRunId: record.lastValidationRunId,
        autoApplyPaused: record.autoApplyPaused,
        divergenceAlert
      })
    }
  }
  for (const run of validationRuns.value) {
    if (run?.autoApplyPaused || String(run?.comparisonStatus || '').toUpperCase() === 'DIVERGED') {
      rows.push({
        evidenceType: 'validationRunPause',
        validationRunId: run.validationRunId,
        rewriteRecordId: run.rewriteRecordId,
        recommendationId: run.recommendationId,
        historyId: run.historyId,
        comparisonStatus: run.comparisonStatus,
        differenceType: run.differenceType,
        autoApplyPaused: run.autoApplyPaused,
        executionEvidence: run.executionEvidence
      })
    }
  }
  return rows
})

const handleModeSelect = mode => {
  if (!sourceModeConfig[mode] || form.sourceType === mode) {
    return
  }
  form.sourceType = mode
  form.sourceKind = sourceModeConfig[mode].defaultSourceKind
  form.evidenceLevel = sourceModeConfig[mode].defaultEvidenceLevel
  if (mode === 'PARSE') {
    form.sourceId = form.parseHistoryId || 'parse-history-001'
    return
  }
  form.sourceId = form.historyId || 'history-001'
}

const openRouteTarget = target => {
  if (target.disabled) {
    return
  }
  router.push({
    path: target.path,
    query: target.query
  })
}

const openEvidenceDrawer = (title, payload) => {
  evidenceDrawerTitle.value = title
  evidenceDrawerPayload.value = payload
  evidenceDrawerVisible.value = true
}

const openSourceEditor = () => {
  sourceEditorVisible.value = true
}

const createCandidate = () =>
  runAction(
    'candidate',
    'createAccelerationCandidate',
    'POST /api/sql-optimization/acceleration-candidates',
    buildCandidateRequest(),
    (requestPayload, requestOptions) => createAccelerationCandidate(requestPayload, requestOptions),
    response => {
      candidateResponse.value = response
      form.candidateId = response?.candidateId || form.candidateId
    }
  )

const refreshCandidates = () =>
  runAction(
    'candidate',
    'getAccelerationCandidates',
    'GET /api/sql-optimization/acceleration-candidates',
    { tenantId: form.tenantId },
    (_requestPayload, requestOptions) => getAccelerationCandidates(form.tenantId, requestOptions),
    response => {
      candidateList.value = Array.isArray(response) ? response : []
    }
  )

const loadCandidate = () => {
  if (!hasValue(form.candidateId)) {
    return
  }
  return runAction(
    'candidate',
    'getAccelerationCandidate',
    'GET /api/sql-optimization/acceleration-candidates/{candidateId}',
    { tenantId: form.tenantId, candidateId: form.candidateId },
    (_requestPayload, requestOptions) => getAccelerationCandidate(form.tenantId, form.candidateId, requestOptions),
    response => {
      candidateResponse.value = response
      candidateList.value = response ? [response] : candidateList.value
    }
  )
}

const submitSuggestion = () =>
  runAction(
    'suggestion',
    'submitOptimizationTask',
    'POST /api/sql-optimization/tasks',
    buildSuggestionTaskRequest(),
    (requestPayload, requestOptions) => submitOptimizationTask(requestPayload, requestOptions),
    response => {
      suggestionTask.value = response
      form.sourceTaskId = response?.taskId || form.sourceTaskId
    }
  )

const refreshSuggestionTask = () => {
  if (!hasValue(selectedSourceTaskId.value)) {
    return
  }
  return runAction(
    'suggestion',
    'getOptimizationTaskStatus',
    'GET /api/sql-optimization/tasks/{taskId}',
    { tenantId: form.tenantId, taskId: selectedSourceTaskId.value },
    (_requestPayload, requestOptions) => getOptimizationTaskStatus(selectedSourceTaskId.value, form.tenantId, requestOptions),
    response => {
      suggestionTaskStatus.value = response
    }
  )
}

const loadDiff = () =>
  runAction(
    'diff',
    'getRecommendationDiff',
    'GET /api/sql-optimization/recommendations/{recommendationId}/diff',
    { tenantId: form.tenantId, recommendationId: form.recommendationId },
    (_requestPayload, requestOptions) => getRecommendationDiff(form.tenantId, form.recommendationId, requestOptions),
    response => {
      diffResponse.value = response
      form.sqlFingerprint = response?.sqlFingerprint || form.sqlFingerprint
    }
  )

const createRewriteRecordFromDiff = () =>
  runAction(
    'monitoring',
    'createSqlRewriteRecord',
    'POST /api/sql-optimization/rewrite-records',
    buildRewriteRecordRequest(),
    (requestPayload, requestOptions) => createSqlRewriteRecord(requestPayload, requestOptions),
    response => {
      rewriteRecordResponse.value = response
      form.rewriteRecordId = response?.rewriteRecordId || form.rewriteRecordId
    }
  )

const submitPlan = () =>
  runAction(
    'plan',
    'submitAccelerationPlan',
    'POST /api/sql-optimization/acceleration-plans',
    buildPlanSubmitRequest(),
    (requestPayload, requestOptions) => submitAccelerationPlan(requestPayload, requestOptions),
    response => {
      planResponse.value = response
      form.planId = response?.planId || form.planId
    }
  )

const refreshPlan = () => {
  if (!hasValue(form.planId)) {
    return
  }
  return runAction(
    'plan',
    'getAccelerationPlan',
    'GET /api/sql-optimization/acceleration-plans/{planId}',
    { tenantId: form.tenantId, planId: form.planId },
    (_requestPayload, requestOptions) => getAccelerationPlan(form.tenantId, form.planId, requestOptions),
    response => {
      planResponse.value = response
    }
  )
}

const reviewPlan = approve =>
  runAction(
    'approval',
    approve ? 'approveAccelerationPlan' : 'rejectAccelerationPlan',
    'POST /api/sql-optimization/acceleration-plans/{planId}/approval',
    { approve, reviewNote: form.reviewNote },
    (requestPayload, requestOptions) => reviewAccelerationPlan(form.tenantId, form.planId, requestPayload, requestOptions),
    response => {
      planResponse.value = response
    }
  )

const applyPlan = () => mutatePlan('validation', 'applyAccelerationPlan', 'POST /api/sql-optimization/acceleration-plans/{planId}/apply', applyAccelerationPlan)
const verifyPlan = () => mutatePlan('validation', 'verifyAccelerationPlan', 'POST /api/sql-optimization/acceleration-plans/{planId}/verify', verifyAccelerationPlan)
const rollbackPlan = () => mutatePlan('validation', 'rollbackAccelerationPlan', 'POST /api/sql-optimization/acceleration-plans/{planId}/rollback', rollbackAccelerationPlan)

const runBaselineQuery = () => runQueryEvidence('baseline', 'NONE')
const runAcceleratedQuery = () => runQueryEvidence('accelerated', 'PREFER_ACCELERATED')

const refreshRewriteRecords = () =>
  runAction(
    'monitoring',
    'getSqlRewriteRecords',
    'GET /api/sql-optimization/rewrite-records',
    { tenantId: form.tenantId, historyId: form.historyId, recommendationId: form.recommendationId, validationStatus: form.validationStatus, sourceType: form.sourceType },
    (requestPayload, requestOptions) => getSqlRewriteRecords(form.tenantId, requestPayload, requestOptions),
    response => {
      rewriteRecords.value = Array.isArray(response) ? response : []
      if (rewriteRecords.value[0]?.rewriteRecordId && !form.rewriteRecordId) {
        form.rewriteRecordId = rewriteRecords.value[0].rewriteRecordId
      }
    }
  )

const refreshHistoryRewriteRecords = () => {
  if (!hasValue(form.historyId)) {
    return
  }
  return runAction(
    'monitoring',
    'getQueryHistoryRewriteRecords',
    'GET /api/governance/query-history/{historyId}/rewrite-records',
    { tenantId: form.tenantId, historyId: form.historyId },
    (_requestPayload, requestOptions) => getQueryHistoryRewriteRecords(form.tenantId, form.historyId, requestOptions),
    response => {
      historyRewriteRecords.value = response
    }
  )
}

const refreshValidationRuns = () => {
  if (!hasValue(form.rewriteRecordId)) {
    return
  }
  return runAction(
    'monitoring',
    'getRewriteValidationRuns',
    'GET /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs',
    { tenantId: form.tenantId, rewriteRecordId: form.rewriteRecordId },
    (_requestPayload, requestOptions) => getRewriteValidationRuns(form.tenantId, form.rewriteRecordId, requestOptions),
    response => {
      validationRuns.value = Array.isArray(response) ? response : []
    }
  )
}

const createValidationRun = () => {
  if (!hasValue(form.rewriteRecordId)) {
    return
  }
  return runAction(
    'monitoring',
    'createRewriteValidationRun',
    'POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs',
    buildValidationRunRequest(),
    (requestPayload, requestOptions) => createRewriteValidationRun(form.tenantId, form.rewriteRecordId, requestPayload, requestOptions),
    response => {
      validationRunResponse.value = response
      validationRuns.value = [response, ...validationRuns.value.filter(item => item.validationRunId !== response?.validationRunId)]
    }
  )
}

const selectCandidate = row => {
  form.candidateId = row?.candidateId || form.candidateId
  candidateResponse.value = row || candidateResponse.value
}

const selectRewriteRecord = row => {
  form.rewriteRecordId = row?.rewriteRecordId || form.rewriteRecordId
  rewriteRecordResponse.value = row || rewriteRecordResponse.value
}

const handlePagerChange = (pager, page) => {
  pager.page = page
}

const handlePagerSizeChange = (pager, size) => {
  pager.size = size
  pager.page = 1
}

const mutatePlan = (loadingKey, actionName, endpoint, mutator) =>
  runAction(
    loadingKey,
    actionName,
    endpoint,
    { reason: form.actionReason },
    (requestPayload, requestOptions) => mutator(form.tenantId, form.planId, requestPayload, requestOptions),
    response => {
      planResponse.value = response
    }
  )

const runQueryEvidence = (scenario, accelerationPreference) =>
  runAction(
    'query',
    scenario === 'baseline' ? 'executeBaselineQuery' : 'executeAcceleratedQuery',
    'POST /api/query-execution/queries/execute',
    buildQueryRequest(accelerationPreference),
    (requestPayload, requestOptions) => executeQuery(requestPayload, requestOptions),
    response => {
      if (scenario === 'baseline') {
        baselineResult.value = response
        return
      }
      acceleratedResult.value = response
    }
  )

const runAction = async (loadingKey, actionName, endpoint, requestPayload, runner, onSuccess) => {
  loading[loadingKey] = true
  errorMessage.value = ''
  const requestOptions = {
    requestPrefix: `frontend-acceleration-workbench-${loadingKey}`
  }
  const evidence = {
    action: actionName,
    endpoint,
    status: 'PENDING',
    requestedAt: new Date().toISOString(),
    requestHeaders: {
      'X-SQLForge-Dev-Tenant-Id': form.tenantId,
      'X-SQLForge-Dev-Request-Prefix': requestOptions.requestPrefix
    },
    request: requestPayload,
    response: null
  }
  lastEvidence.value = evidence
  try {
    const response = await runner(requestPayload, requestOptions)
    const completedEvidence = {
      ...evidence,
      status: 'SUCCEEDED',
      response,
      completedAt: new Date().toISOString()
    }
    lastEvidence.value = completedEvidence
    if (onSuccess) {
      onSuccess(response)
    }
    return response
  } catch (error) {
    const message = formatRuntimeError(error)
    const failedEvidence = {
      ...evidence,
      status: 'FAILED',
      error: message,
      response: error?.response?.data || null,
      completedAt: new Date().toISOString()
    }
    errorMessage.value = message
    lastEvidence.value = failedEvidence
    return null
  } finally {
    loading[loadingKey] = false
  }
}

function buildSourceSnapshot() {
  return compactObject({
    tenantId: form.tenantId,
    sourceType: form.sourceType,
    sourceKind: form.sourceKind,
    sourceId: resolvedSourceId.value,
    historyId: form.historyId,
    parseHistoryId: form.parseHistoryId,
    sqlFingerprint: form.sqlFingerprint,
    datasourceCode: form.datasourceCode,
    datasourceType: form.datasourceType,
    stage: form.stage,
    reportCode: form.reportCode,
    evidenceLevel: form.evidenceLevel
  })
}

function buildCandidateRequest() {
  const sourceSnapshot = buildSourceSnapshot()
  return compactObject({
    ...sourceSnapshot,
    candidateType: 'ACCELERATION_AND_REWRITE',
    status: 'DRAFT',
    confidence: form.evidenceLevel === 'STATIC_PARSE' ? 0.62 : 0.78,
    priority: form.evidenceLevel === 'STATIC_PARSE' ? 2 : 1,
    schemaVersion: 'HARN-138',
    sourceEvidence: {
      ...sourceSnapshot,
      staticOnly: form.evidenceLevel === 'STATIC_PARSE',
      enableHetuExplain: form.enableHetuExplain
    },
    issueEvidence: {
      sqlPreviewAvailable: hasValue(form.sqlText),
      sourceSummary: sourceSummaryText.value
    },
    runtimeEvidence: {
      evidenceLevel: form.evidenceLevel,
      runtimeMetricsUnavailable: form.evidenceLevel === 'STATIC_PARSE'
    },
    benefitEstimate: {
      evidenceLevel: form.evidenceLevel,
      verifiedBenefit: false
    },
    costEstimate: {
      externalExecutionRequired: form.enableHetuExplain
    },
    risk: {
      manualReviewRequired: form.evidenceLevel === 'STATIC_PARSE',
      boundary: 'APPLIED is not treated as ACTIVE before verification'
    }
  })
}

function buildSuggestionTaskRequest() {
  return compactObject({
    tenantId: form.tenantId,
    taskType: 'ACCELERATION_SUGGESTION',
    sqlText: form.sqlText,
    sqlFingerprint: form.sqlFingerprint,
    datasourceType: form.datasourceType,
    taskContext: compactObject({
      parseDepth: 'DEEP',
      priority: 'NORMAL',
      requestedSuggestionTypes: form.selectedSuggestionTypes,
      sourceType: form.sourceType,
      sourceId: resolvedSourceId.value,
      historyId: form.historyId,
      parseTaskId: form.parseHistoryId,
      datasourceCode: form.datasourceCode,
      reportCode: form.reportCode
    })
  })
}

function buildPlanSubmitRequest() {
  return {
    tenantId: form.tenantId,
    sourceTaskId: selectedSourceTaskId.value,
    selectedSuggestionTypes: form.selectedSuggestionTypes
  }
}

function buildRewriteRecordRequest() {
  const sourceSnapshot = buildSourceSnapshot()
  return compactObject({
    tenantId: form.tenantId,
    recommendationId: form.recommendationId || diffResponse.value?.recommendationId,
    optimizationTaskId: selectedSourceTaskId.value,
    sourceType: form.sourceType,
    sourceKind: form.sourceKind,
    sourceId: resolvedSourceId.value,
    evidenceLevel: form.evidenceLevel,
    historyId: form.historyId,
    parseHistoryId: form.parseHistoryId,
    sqlFingerprint: form.sqlFingerprint || diffResponse.value?.sqlFingerprint,
    datasourceCode: form.datasourceCode,
    status: 'DRAFT',
    validationStatus: 'NOT_VALIDATED',
    autoApplyAllowed: false,
    manualReviewRequired: true,
    alertStatus: 'NONE',
    originalSqlText: diffResponse.value?.originalSql,
    recommendedSqlText: diffResponse.value?.recommendedSql,
    ruleChain: diffResponse.value?.ruleDiff || [],
    diffSummary: diffResponse.value?.diffSummary || {},
    risk: {
      sourceEvidenceLevel: form.evidenceLevel,
      manualReviewRequired: true
    },
    traceRefs: sourceSnapshot
  })
}

function buildValidationRunRequest() {
  return compactObject({
    tenantId: form.tenantId,
    recommendationId: form.recommendationId,
    historyId: form.historyId,
    sqlFingerprint: form.sqlFingerprint,
    datasourceType: form.datasourceType,
    triggerReason: form.actionReason,
    status: 'PENDING',
    comparisonStatus: 'NOT_COMPARED',
    differenceType: 'UNKNOWN',
    autoApplyPaused: false,
    comparisonPolicy: {
      source: 'HARN-138_WORKBENCH',
      fullResultPulledToFrontend: false
    },
    executionEvidence: {
      requestedBy: 'acceleration-governance-workbench',
      evidenceLevel: form.evidenceLevel
    }
  })
}

function buildQueryRequest(accelerationPreference) {
  return compactObject({
    tenantId: form.tenantId,
    sqlText: form.sqlText,
    datasourceType: form.datasourceType,
    accelerationPreference,
    faultToleranceStrategy: 'FAIL_FAST',
    queryContext: compactObject({
      source: 'ACCELERATION_GOVERNANCE_WORKBENCH',
      planId: form.planId,
      sqlFingerprint: form.sqlFingerprint
    })
  })
}

function action(tabName, method, endpoint, enabled) {
  return {
    tab: tabName,
    method,
    endpoint,
    enabled,
    state: enabled
      ? t('accelerationGovernanceWorkbench.states.realInterfaceReady')
      : t('accelerationGovernanceWorkbench.states.missingTraceKey')
  }
}

function tab(name, label) {
  return { name, label }
}

function field(key, label, value) {
  return { key, label, value: displayValue(value) }
}

function displayValue(value) {
  if (!hasValue(value)) {
    return '-'
  }
  if (Array.isArray(value)) {
    return value.length ? value.join(' / ') : '-'
  }
  return String(value)
}

function hasValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function compactObject(query) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => {
      if (Array.isArray(value)) {
        return value.length > 0
      }
      if (value && typeof value === 'object') {
        return Object.keys(value).length > 0
      }
      return hasValue(value)
    })
  )
}

function paginate(rows, pager) {
  const start = (pager.page - 1) * pager.size
  return rows.slice(start, start + pager.size)
}

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2)
}

function friendlyPlanStatus(status) {
  if (status === 'APPLIED') {
    return t('accelerationGovernanceWorkbench.statuses.appliedPendingVerification')
  }
  if (status === 'VERIFIED' || status === 'ACTIVE') {
    return t('accelerationGovernanceWorkbench.statuses.active')
  }
  if (status === 'VERIFY_FAILED') {
    return t('accelerationGovernanceWorkbench.statuses.reviewRequired')
  }
  return status || '-'
}

function tagType(value) {
  const normalized = String(value || '').toUpperCase()
  if (['SUCCEEDED', 'SUCCESS', 'VERIFIED', 'ACTIVE', 'EQUIVALENT', 'APPROVED'].includes(normalized)) {
    return 'success'
  }
  if (['APPLIED', 'PENDING_APPROVAL', 'PENDING', 'RUNNING', 'NOT_COMPARED'].includes(normalized)) {
    return 'warning'
  }
  if (['FAILED', 'VERIFY_FAILED', 'DIVERGED', 'ROLLBACK_FAILED', 'APPLY_FAILED', 'REJECTED'].includes(normalized)) {
    return 'danger'
  }
  return 'info'
}

function queryAccelerationApplied(result) {
  const value = result?.metadata?.accelerationApplied
  if (value === true) {
    return 'true'
  }
  if (value === false) {
    return 'false'
  }
  return '-'
}
</script>

<template>
  <section class="acceleration-governance-page" data-testid="acceleration-governance-workbench-page">
    <SectionHeader
      :eyebrow="t('accelerationGovernanceWorkbench.eyebrow')"
      :title="t('accelerationGovernanceWorkbench.pageTitle')"
      :summary="t('accelerationGovernanceWorkbench.boundarySummary')"
      :level="1"
      size="compact"
    >
      <template #actions>
        <el-button @click="openSourceEditor">
          {{ t('accelerationGovernanceWorkbench.source.title') }}
        </el-button>
        <el-button data-testid="acceleration-workbench-source-evidence" @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.drawers.source'), sourceEvidence)">
          {{ t('accelerationGovernanceWorkbench.actions.viewSourceEvidence') }}
        </el-button>
      </template>
    </SectionHeader>

    <section class="status-strip" data-testid="acceleration-workbench-status-strip">
      <div v-for="item in sourceSummaryRows" :key="item.key" class="status-cell">
        <span>{{ item.label }}</span>
        <strong :data-testid="`acceleration-workbench-summary-${item.key}`">{{ item.value }}</strong>
      </div>
    </section>

    <ToolbarShell
      :eyebrow="t('accelerationGovernanceWorkbench.source.eyebrow')"
      :title="t('accelerationGovernanceWorkbench.source.title')"
      :summary="sourceSummaryText"
      density="compact"
      test-id="acceleration-workbench-source-shell"
    >
      <div class="source-mode-row" data-testid="acceleration-workbench-source-mode">
        <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.currentMode') }}</span>
        <div class="segmented-control">
          <button
            v-for="item in sourceModeOptions"
            :key="item.value"
            type="button"
            class="segmented-option"
            :class="{ 'segmented-option-active': form.sourceType === item.value }"
            :data-testid="`acceleration-workbench-mode-${item.value}`"
            @click="handleModeSelect(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div class="source-grid source-grid-compact">
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.tenantId') }}</span>
          <el-input v-model.trim="form.tenantId" data-testid="acceleration-workbench-tenant-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceType') }}</span>
          <el-input v-model="form.sourceType" readonly data-testid="acceleration-workbench-source-type" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceKind') }}</span>
          <el-select v-model="form.sourceKind" data-testid="acceleration-workbench-source-kind">
            <el-option v-for="item in sourceKindOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceId') }}</span>
          <el-input v-model.trim="form.sourceId" data-testid="acceleration-workbench-source-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sqlFingerprint') }}</span>
          <el-input v-model.trim="form.sqlFingerprint" data-testid="acceleration-workbench-sql-fingerprint" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.evidenceLevel') }}</span>
          <el-select v-model="form.evidenceLevel" data-testid="acceleration-workbench-evidence-level">
            <el-option v-for="item in evidenceLevelOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
      </div>
      <SqlEditorField
        v-model="form.sqlText"
        class="field-block-wide"
        :label="t('accelerationGovernanceWorkbench.fields.sqlText')"
        :rows="4"
        data-testid="acceleration-workbench-sql-text"
      />
    </ToolbarShell>

    <section class="route-strip">
      <button
        v-for="item in routeTargets"
        :key="item.key"
        type="button"
        class="route-link"
        :class="{ 'route-link-disabled': item.disabled }"
        :data-testid="`acceleration-workbench-open-${item.key}`"
        :disabled="item.disabled"
        @click="openRouteTarget(item)"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.disabled ? t('accelerationGovernanceWorkbench.states.missingTraceKey') : item.path }}</strong>
      </button>
      <el-button data-testid="acceleration-workbench-route-evidence" @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.drawers.routes'), routeTargets)">
        {{ t('accelerationGovernanceWorkbench.actions.viewRouteEvidence') }}
      </el-button>
    </section>

    <section class="runtime-strip" data-testid="acceleration-workbench-runtime-panel">
      <div v-for="item in runtimeSummaryRows" :key="item.key" class="runtime-cell">
        <span>{{ item.label }}</span>
        <strong :data-testid="`acceleration-workbench-runtime-${item.key}`">{{ item.value }}</strong>
      </div>
    </section>
    <p v-if="errorMessage" class="error-note" data-testid="acceleration-workbench-error">{{ errorMessage }}</p>

    <section class="workflow-shell" data-testid="acceleration-workbench-tabs-panel">
      <el-tabs v-model="activeTab" data-testid="acceleration-workbench-tabs">
        <el-tab-pane v-for="tabItem in tabDefinitions" :key="tabItem.name" :label="tabItem.label" :name="tabItem.name">
          <div class="tab-shell" :data-testid="`acceleration-workbench-tab-${tabItem.name}`">
            <div class="tab-source-strip">
              <span>{{ t('accelerationGovernanceWorkbench.sections.tabSummary') }}</span>
              <strong>{{ sourceSummaryText }}</strong>
            </div>

            <template v-if="tabItem.name === 'candidates'">
              <div class="tab-grid">
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.candidateId') }}</span>
                  <el-input v-model.trim="form.candidateId" data-testid="acceleration-workbench-candidate-id" />
                </label>
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceTaskId') }}</span>
                  <el-input v-model.trim="form.sourceTaskId" data-testid="acceleration-workbench-source-task-id" />
                </label>
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.selectedSuggestionTypes') }}</span>
                  <el-select v-model="form.selectedSuggestionTypes" multiple collapse-tags data-testid="acceleration-workbench-suggestion-types">
                    <el-option v-for="item in suggestionTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </label>
              </div>
              <div class="action-row">
                <el-button type="primary" :loading="loading.candidate" :disabled="!canSubmitSource" data-testid="acceleration-workbench-create-candidate" @click="createCandidate">
                  {{ t('accelerationGovernanceWorkbench.actions.createCandidate') }}
                </el-button>
                <el-button :loading="loading.candidate" data-testid="acceleration-workbench-list-candidates" @click="refreshCandidates">
                  {{ t('accelerationGovernanceWorkbench.actions.listCandidates') }}
                </el-button>
                <el-button :loading="loading.candidate" :disabled="!form.candidateId" data-testid="acceleration-workbench-load-candidate" @click="loadCandidate">
                  {{ t('accelerationGovernanceWorkbench.actions.loadCandidate') }}
                </el-button>
                <el-button type="primary" :loading="loading.suggestion" :disabled="!canSubmitSuggestion" data-testid="acceleration-workbench-submit-suggestion" @click="submitSuggestion">
                  {{ t('accelerationGovernanceWorkbench.actions.submitSuggestion') }}
                </el-button>
                <el-button :loading="loading.suggestion" :disabled="!selectedSourceTaskId" data-testid="acceleration-workbench-refresh-suggestion" @click="refreshSuggestionTask">
                  {{ t('accelerationGovernanceWorkbench.actions.refreshSuggestion') }}
                </el-button>
              </div>
              <el-table :data="pagedCandidateRows" stripe data-testid="acceleration-workbench-candidate-table" @row-click="selectCandidate">
                <el-table-column prop="candidateId" :label="t('accelerationGovernanceWorkbench.fields.candidateId')" min-width="180" />
                <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="120" />
                <el-table-column prop="candidateType" :label="t('accelerationGovernanceWorkbench.fields.candidateType')" min-width="180" />
                <el-table-column prop="evidenceLevel" :label="t('accelerationGovernanceWorkbench.fields.evidenceLevel')" min-width="150" />
                <el-table-column prop="sourceKind" :label="t('accelerationGovernanceWorkbench.fields.sourceKind')" min-width="170" />
              </el-table>
              <el-pagination
                v-if="candidateRows.length > candidatePager.size"
                v-model:current-page="candidatePager.page"
                background
                layout="sizes, prev, pager, next"
                :page-sizes="[8, 16, 32]"
                :page-size="candidatePager.size"
                :total="candidateRows.length"
                @current-change="page => handlePagerChange(candidatePager, page)"
                @size-change="size => handlePagerSizeChange(candidatePager, size)"
              />
              <div class="evidence-summary">
                <el-tag :type="tagType(suggestionTaskStatus?.status || suggestionTask?.status)">{{ displayValue(suggestionTaskStatus?.status || suggestionTask?.status) }}</el-tag>
                <span>{{ displayValue(suggestionTaskStatus?.taskId || suggestionTask?.taskId) }}</span>
              </div>
            </template>

            <template v-else-if="tabItem.name === 'diff'">
              <div class="tab-grid">
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.recommendationId') }}</span>
                  <el-input v-model.trim="form.recommendationId" data-testid="acceleration-workbench-recommendation-id" />
                </label>
              </div>
              <div class="action-row">
                <el-button type="primary" :loading="loading.diff" :disabled="!canLoadDiff" data-testid="acceleration-workbench-load-diff" @click="loadDiff">
                  {{ t('accelerationGovernanceWorkbench.actions.loadDiff') }}
                </el-button>
                <el-button :loading="loading.monitoring" :disabled="!canCreateRewriteRecord" data-testid="acceleration-workbench-create-rewrite-record" @click="createRewriteRecordFromDiff">
                  {{ t('accelerationGovernanceWorkbench.actions.createRewriteRecord') }}
                </el-button>
                <el-button @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.tabs.diff'), { diffSummary: diffResponse?.diffSummary, ruleDiff: diffResponse?.ruleDiff, astSummaryDiff: diffResponse?.astSummaryDiff })">
                  {{ t('common.actions.viewRawEvidence') }}
                </el-button>
              </div>
              <div class="sql-diff-grid" data-testid="acceleration-workbench-diff-view">
                <SqlCodeBlock
                  :value="diffResponse?.originalSql || form.sqlText"
                  :label="t('accelerationGovernanceWorkbench.fields.originalSql')"
                  :copy-label="t('common.actions.copy')"
                  compact
                />
                <SqlCodeBlock
                  :value="diffResponse?.recommendedSql || ''"
                  :label="t('accelerationGovernanceWorkbench.fields.recommendedSql')"
                  :copy-label="t('common.actions.copy')"
                  compact
                />
              </div>
              <section v-if="diffAccelerationArtifact" class="evidence-table" data-testid="acceleration-workbench-acceleration-artifact">
                <div class="evidence-heading">
                  <h3>{{ t('recommendationCenter.sections.accelerationArtifact') }}</h3>
                  <el-button @click="openEvidenceDrawer(t('recommendationCenter.sections.accelerationArtifact'), diffAccelerationArtifact)">
                    {{ t('common.actions.viewRawEvidence') }}
                  </el-button>
                </div>
                <dl class="summary-grid">
                  <div v-for="item in diffAccelerationArtifactCards" :key="item.key" class="summary-cell">
                    <span>{{ item.label }}</span>
                    <strong>{{ displayValue(item.value) }}</strong>
                  </div>
                </dl>
                <div v-if="diffAccelerationArtifactSqlBlocks.length" class="sql-diff-grid">
                  <SqlCodeBlock
                    v-for="item in diffAccelerationArtifactSqlBlocks"
                    :key="item.key"
                    :value="item.value"
                    :label="item.label"
                    :copy-label="t('common.actions.copy')"
                    compact
                  />
                </div>
              </section>
            </template>

            <template v-else-if="tabItem.name === 'approval'">
              <div class="tab-grid">
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceTaskId') }}</span>
                  <el-input v-model.trim="form.sourceTaskId" data-testid="acceleration-workbench-plan-source-task-id" />
                </label>
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.planId') }}</span>
                  <el-input v-model.trim="form.planId" data-testid="acceleration-workbench-plan-id" />
                </label>
                <label class="field-block field-block-wide">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.reviewNote') }}</span>
                  <el-input v-model="form.reviewNote" data-testid="acceleration-workbench-review-note" />
                </label>
              </div>
              <div class="action-row">
                <el-button type="primary" :loading="loading.plan" :disabled="!canSubmitPlan" data-testid="acceleration-workbench-submit-plan" @click="submitPlan">
                  {{ t('accelerationGovernanceWorkbench.actions.submitPlan') }}
                </el-button>
                <el-button :loading="loading.plan" :disabled="!form.planId" data-testid="acceleration-workbench-refresh-plan" @click="refreshPlan">
                  {{ t('accelerationGovernanceWorkbench.actions.refreshPlan') }}
                </el-button>
                <el-button type="primary" :loading="loading.approval" :disabled="!form.planId" data-testid="acceleration-workbench-approve-plan" @click="reviewPlan(true)">
                  {{ t('accelerationGovernanceWorkbench.actions.approvePlan') }}
                </el-button>
                <el-button :loading="loading.approval" :disabled="!form.planId" data-testid="acceleration-workbench-reject-plan" @click="reviewPlan(false)">
                  {{ t('accelerationGovernanceWorkbench.actions.rejectPlan') }}
                </el-button>
              </div>
              <div class="plan-summary" data-testid="acceleration-workbench-plan-summary">
                <el-tag :type="tagType(planResponse?.status)">{{ friendlyPlanStatus(selectedPlanStatus) }}</el-tag>
                <span>{{ displayValue(planResponse?.planSummary || planResponse?.statusQueryPath) }}</span>
                <span>{{ displayValue(planResponse?.configSnapshotId) }}</span>
                <span>{{ displayValue(planResponse?.resultId) }}</span>
              </div>
              <el-table :data="planResponse?.statusHistory || []" stripe>
                <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="130" />
                <el-table-column prop="reason" :label="t('accelerationGovernanceWorkbench.fields.reason')" min-width="180" />
                <el-table-column prop="createdAt" :label="t('accelerationGovernanceWorkbench.fields.createdAt')" min-width="180" />
              </el-table>
            </template>

            <template v-else-if="tabItem.name === 'validation'">
              <div class="tab-grid">
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.planId') }}</span>
                  <el-input v-model.trim="form.planId" data-testid="acceleration-workbench-validation-plan-id" />
                </label>
                <label class="field-block field-block-wide">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.actionReason') }}</span>
                  <el-input v-model="form.actionReason" data-testid="acceleration-workbench-action-reason" />
                </label>
              </div>
              <div class="action-row">
                <el-button type="primary" :loading="loading.validation" :disabled="!canApplyPlan" data-testid="acceleration-workbench-apply-plan" @click="applyPlan">
                  {{ t('accelerationGovernanceWorkbench.actions.applyPlan') }}
                </el-button>
                <el-button type="primary" :loading="loading.validation" :disabled="!canVerifyPlan" data-testid="acceleration-workbench-verify-plan" @click="verifyPlan">
                  {{ t('accelerationGovernanceWorkbench.actions.verifyPlan') }}
                </el-button>
                <el-button :loading="loading.validation" :disabled="!canRollbackPlan" data-testid="acceleration-workbench-rollback-plan" @click="rollbackPlan">
                  {{ t('accelerationGovernanceWorkbench.actions.rollbackPlan') }}
                </el-button>
                <el-button :loading="loading.query" :disabled="!canSubmitSuggestion" data-testid="acceleration-workbench-baseline-query" @click="runBaselineQuery">
                  {{ t('accelerationGovernanceWorkbench.actions.baselineQuery') }}
                </el-button>
                <el-button :loading="loading.query" :disabled="!canSubmitSuggestion" data-testid="acceleration-workbench-accelerated-query" @click="runAcceleratedQuery">
                  {{ t('accelerationGovernanceWorkbench.actions.acceleratedQuery') }}
                </el-button>
                <el-button @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.tabs.validation'), { runtimeBindingJson: planResponse?.runtimeBindingJson, verificationEvidenceJson: planResponse?.verificationEvidenceJson, rollbackEvidenceJson: planResponse?.rollbackEvidenceJson })">
                  {{ t('common.actions.viewRawEvidence') }}
                </el-button>
              </div>
              <div class="query-result-grid">
                <div class="result-box" data-testid="acceleration-workbench-baseline-result">
                  <span>{{ t('accelerationGovernanceWorkbench.fields.baselineResult') }}</span>
                  <strong>{{ displayValue(baselineResult?.status) }}</strong>
                  <small>{{ t('accelerationGovernanceWorkbench.fields.accelerationApplied') }}={{ queryAccelerationApplied(baselineResult) }}</small>
                </div>
                <div class="result-box" data-testid="acceleration-workbench-accelerated-result">
                  <span>{{ t('accelerationGovernanceWorkbench.fields.acceleratedResult') }}</span>
                  <strong>{{ displayValue(acceleratedResult?.status) }}</strong>
                  <small>{{ t('accelerationGovernanceWorkbench.fields.accelerationApplied') }}={{ queryAccelerationApplied(acceleratedResult) }}</small>
                </div>
              </div>
            </template>

            <template v-else-if="tabItem.name === 'monitoring'">
              <div class="tab-grid">
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.historyId') }}</span>
                  <el-input v-model.trim="form.historyId" data-testid="acceleration-workbench-monitor-history-id" />
                </label>
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.rewriteRecordId') }}</span>
                  <el-input v-model.trim="form.rewriteRecordId" data-testid="acceleration-workbench-rewrite-record-id" />
                </label>
                <label class="field-block">
                  <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.validationStatus') }}</span>
                  <el-input v-model.trim="form.validationStatus" data-testid="acceleration-workbench-validation-status-filter" />
                </label>
              </div>
              <div class="action-row">
                <el-button type="primary" :loading="loading.monitoring" data-testid="acceleration-workbench-list-rewrite-records" @click="refreshRewriteRecords">
                  {{ t('accelerationGovernanceWorkbench.actions.listRewriteRecords') }}
                </el-button>
                <el-button :loading="loading.monitoring" :disabled="!form.historyId" data-testid="acceleration-workbench-history-rewrite-records" @click="refreshHistoryRewriteRecords">
                  {{ t('accelerationGovernanceWorkbench.actions.historyRewriteRecords') }}
                </el-button>
                <el-button :loading="loading.monitoring" :disabled="!form.rewriteRecordId" data-testid="acceleration-workbench-list-validation-runs" @click="refreshValidationRuns">
                  {{ t('accelerationGovernanceWorkbench.actions.listValidationRuns') }}
                </el-button>
                <el-button :loading="loading.monitoring" :disabled="!form.rewriteRecordId" data-testid="acceleration-workbench-create-validation-run" @click="createValidationRun">
                  {{ t('accelerationGovernanceWorkbench.actions.createValidationRun') }}
                </el-button>
              </div>
              <el-table :data="pagedRewriteRecordRows" stripe data-testid="acceleration-workbench-rewrite-record-table" @row-click="selectRewriteRecord">
                <el-table-column prop="rewriteRecordId" :label="t('accelerationGovernanceWorkbench.fields.rewriteRecordId')" min-width="190" />
                <el-table-column prop="validationStatus" :label="t('accelerationGovernanceWorkbench.fields.validationStatus')" min-width="160" />
                <el-table-column prop="alertStatus" :label="t('accelerationGovernanceWorkbench.fields.alertStatus')" min-width="130" />
                <el-table-column prop="autoApplyAllowed" :label="t('accelerationGovernanceWorkbench.fields.autoApplyAllowed')" min-width="150" />
                <el-table-column prop="manualReviewRequired" :label="t('accelerationGovernanceWorkbench.fields.manualReviewRequired')" min-width="180" />
              </el-table>
              <el-pagination
                v-if="rewriteRecordRows.length > rewritePager.size"
                v-model:current-page="rewritePager.page"
                background
                layout="sizes, prev, pager, next"
                :page-sizes="[6, 12, 24]"
                :page-size="rewritePager.size"
                :total="rewriteRecordRows.length"
                @current-change="page => handlePagerChange(rewritePager, page)"
                @size-change="size => handlePagerSizeChange(rewritePager, size)"
              />
              <el-table :data="pagedHistoryRewriteRecordRows" stripe data-testid="acceleration-workbench-history-rewrite-record-table">
                <el-table-column prop="rewriteRecordId" :label="t('accelerationGovernanceWorkbench.fields.historyRewriteRecordId')" min-width="210" />
                <el-table-column prop="validationStatus" :label="t('accelerationGovernanceWorkbench.fields.validationStatus')" min-width="160" />
                <el-table-column prop="alertStatus" :label="t('accelerationGovernanceWorkbench.fields.alertStatus')" min-width="130" />
              </el-table>
              <el-pagination
                v-if="historyRewriteRecordRows.length > historyRewritePager.size"
                v-model:current-page="historyRewritePager.page"
                background
                layout="sizes, prev, pager, next"
                :page-sizes="[6, 12, 24]"
                :page-size="historyRewritePager.size"
                :total="historyRewriteRecordRows.length"
                @current-change="page => handlePagerChange(historyRewritePager, page)"
                @size-change="size => handlePagerSizeChange(historyRewritePager, size)"
              />
              <el-table :data="pagedValidationRuns" stripe data-testid="acceleration-workbench-validation-run-table">
                <el-table-column prop="validationRunId" :label="t('accelerationGovernanceWorkbench.fields.validationRunId')" min-width="190" />
                <el-table-column prop="status" :label="t('accelerationGovernanceWorkbench.fields.status')" min-width="120" />
                <el-table-column prop="comparisonStatus" :label="t('accelerationGovernanceWorkbench.fields.comparisonStatus')" min-width="160" />
                <el-table-column prop="differenceType" :label="t('accelerationGovernanceWorkbench.fields.differenceType')" min-width="170" />
                <el-table-column prop="autoApplyPaused" :label="t('accelerationGovernanceWorkbench.fields.autoApplyPaused')" min-width="150" />
              </el-table>
              <el-pagination
                v-if="validationRuns.length > validationRunPager.size"
                v-model:current-page="validationRunPager.page"
                background
                layout="sizes, prev, pager, next"
                :page-sizes="[6, 12, 24]"
                :page-size="validationRunPager.size"
                :total="validationRuns.length"
                @current-change="page => handlePagerChange(validationRunPager, page)"
                @size-change="size => handlePagerSizeChange(validationRunPager, size)"
              />
              <section class="pause-evidence-panel" data-testid="acceleration-workbench-auto-pause-evidence">
                <div class="table-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">SQL_REWRITE_RESULT_DIVERGENCE</p>
                    <h3 class="section-title section-title-small">{{ t('accelerationGovernanceWorkbench.sections.autoPauseEvidence') }}</h3>
                  </div>
                  <div class="pause-evidence-count">
                    <span class="sqlforge-code-label">autoApplyPaused</span>
                    <el-tag type="warning">{{ t('accelerationGovernanceWorkbench.fields.autoApplyPaused') }} {{ monitoringPauseEvidenceRows.length }}</el-tag>
                  </div>
                </div>
                <el-empty
                  v-if="!monitoringPauseEvidenceRows.length"
                  :description="t('accelerationGovernanceWorkbench.states.noPauseEvidence')"
                />
                <el-table
                  v-else
                  :data="monitoringPauseEvidenceRows"
                  stripe
                  data-testid="acceleration-workbench-pause-evidence-row"
                  @row-click="row => openEvidenceDrawer(t('accelerationGovernanceWorkbench.sections.autoPauseEvidence'), row)"
                >
                  <el-table-column prop="evidenceType" :label="t('accelerationGovernanceWorkbench.fields.sourceType')" min-width="180" />
                  <el-table-column prop="autoApplyPaused" :label="t('accelerationGovernanceWorkbench.fields.autoApplyPaused')" min-width="160" />
                  <el-table-column prop="alertStatus" :label="t('accelerationGovernanceWorkbench.fields.alertStatus')" min-width="150">
                    <template #default="{ row }">{{ displayValue(row.alertStatus || row.comparisonStatus) }}</template>
                  </el-table-column>
                  <el-table-column prop="rewriteRecordId" :label="t('accelerationGovernanceWorkbench.fields.rewriteRecordId')" min-width="190" />
                </el-table>
              </section>
            </template>

            <template v-else>
              <div class="interface-action-list" data-testid="acceleration-workbench-interface-actions">
                <article v-for="item in interfaceActions" :key="`${item.method}:${item.endpoint}`" class="interface-action-row">
                  <div>
                    <span class="sqlforge-code-label">{{ item.method }}</span>
                    <strong>{{ item.endpoint }}</strong>
                  </div>
                  <el-tag :type="item.enabled ? 'success' : 'warning'">{{ item.state }}</el-tag>
                </article>
              </div>
              <pre class="code-block" data-testid="acceleration-workbench-last-evidence">{{ formatJson(lastEvidence) }}</pre>
            </template>
          </div>
        </el-tab-pane>
      </el-tabs>
      <div class="evidence-actions">
        <el-button data-testid="acceleration-workbench-action-evidence" @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.drawers.actions'), { interfaceActions, lastEvidence })">
          {{ t('accelerationGovernanceWorkbench.actions.viewActionEvidence') }}
        </el-button>
      </div>
    </section>

    <el-dialog v-model="sourceEditorVisible" :title="t('accelerationGovernanceWorkbench.source.title')" width="72%">
      <div class="source-grid">
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.datasourceCode') }}</span>
          <el-input v-model.trim="form.datasourceCode" data-testid="acceleration-workbench-datasource-code" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.datasourceType') }}</span>
          <el-select v-model="form.datasourceType" data-testid="acceleration-workbench-datasource-type">
            <el-option v-for="item in datasourceTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.schemaName') }}</span>
          <el-input v-model.trim="form.schemaName" data-testid="acceleration-workbench-schema-name" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.stage') }}</span>
          <el-input v-model.trim="form.stage" data-testid="acceleration-workbench-stage" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.parseHistoryId') }}</span>
          <el-input v-model.trim="form.parseHistoryId" data-testid="acceleration-workbench-parse-history-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.historyId') }}</span>
          <el-input v-model.trim="form.historyId" data-testid="acceleration-workbench-history-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.reportCode') }}</span>
          <el-input v-model.trim="form.reportCode" data-testid="acceleration-workbench-report-code" />
        </label>
        <label class="field-block field-block-switch">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.enableHetuExplain') }}</span>
          <el-switch v-model="form.enableHetuExplain" data-testid="acceleration-workbench-hetu-explain" />
        </label>
      </div>
    </el-dialog>

    <el-drawer v-model="evidenceDrawerVisible" :title="evidenceDrawerTitle" size="48%" data-testid="acceleration-workbench-evidence-drawer">
      <pre class="code-block">{{ formatJson(evidenceDrawerPayload) }}</pre>
    </el-drawer>
  </section>
</template>

<style scoped>
.acceleration-governance-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
  min-width: 0;
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-border-default);
}

.runtime-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-border-default);
}

.status-cell,
.runtime-cell {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  padding: var(--sqlforge-space-4);
  background: var(--sqlforge-surface-2);
}

.status-cell span,
.runtime-cell span,
.tab-source-strip span,
.state-note,
.result-box span,
.result-box small {
  color: var(--sqlforge-text-muted);
}

.status-cell strong,
.runtime-cell strong,
.tab-source-strip strong,
.interface-action-row strong,
.route-link strong,
.result-box strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.source-mode-row,
.source-grid,
.route-strip,
.action-row,
.evidence-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: flex-end;
}

.source-mode-row {
  align-items: center;
}

.segmented-control {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
}

.segmented-option {
  min-height: 32px;
  padding: 0 var(--sqlforge-space-4);
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--sqlforge-text-secondary);
  cursor: pointer;
}

.segmented-option-active {
  border-color: var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.12);
  color: var(--sqlforge-text-primary);
}

.source-grid,
.tab-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  width: 100%;
  gap: var(--sqlforge-space-3);
}

.source-grid-compact {
  grid-template-columns: repeat(3, minmax(180px, 1fr));
}

.field-block {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
}

.field-block-wide {
  width: 100%;
  grid-column: 1 / -1;
}

.field-block-switch {
  align-content: end;
  min-height: 64px;
}

.field-label {
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.route-strip {
  align-items: stretch;
  padding: var(--sqlforge-space-3) 0;
  border-block: 1px solid var(--sqlforge-border-subtle);
}

.route-link {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 180px;
  min-height: 64px;
  padding: var(--sqlforge-space-3) var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  text-align: left;
  cursor: pointer;
}

.route-link-disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.workflow-shell {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
  padding: var(--sqlforge-space-5);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-2);
}

.tab-shell {
  display: grid;
  gap: var(--sqlforge-space-4);
  min-width: 0;
  padding-top: var(--sqlforge-space-2);
}

.tab-source-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--sqlforge-space-3);
  padding-bottom: var(--sqlforge-space-3);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.action-row {
  align-items: center;
}

.sql-diff-grid,
.query-result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--sqlforge-space-4);
}

.result-box,
.interface-action-row,
.evidence-summary,
.plan-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
  min-width: 0;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-subtle);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
}

.result-box {
  display: grid;
  align-items: start;
}

.interface-action-list {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.interface-action-row {
  justify-content: space-between;
}

.pause-evidence-panel {
  display: grid;
  gap: var(--sqlforge-space-3);
  min-width: 0;
  padding-block: var(--sqlforge-space-3);
  border-block: 1px solid var(--sqlforge-border-subtle);
}

.table-heading {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sqlforge-space-3);
}

.table-heading h3,
.table-heading p {
  margin: 0;
}

.pause-evidence-count {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sqlforge-space-2);
}

.interface-action-row > div {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
}

.error-note {
  margin: 0;
  padding: var(--sqlforge-space-3) var(--sqlforge-space-4);
  border: 1px solid rgba(255, 119, 117, 0.35);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(255, 119, 117, 0.08);
  color: #ffb4b4;
}

.state-note {
  margin: 0;
  line-height: 1.6;
}

.evidence-actions {
  justify-content: flex-end;
}

.code-block {
  min-height: 100%;
  margin: 0;
  padding: var(--sqlforge-space-4);
  overflow: auto;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: 13px;
  line-height: 1.55;
}

.code-block-compact {
  max-height: 260px;
}

@media (max-width: 1280px) {
  .status-strip,
  .runtime-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .source-grid,
  .tab-grid,
  .sql-diff-grid,
  .query-result-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .status-strip,
  .runtime-strip,
  .source-grid,
  .tab-grid,
  .sql-diff-grid,
  .query-result-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .segmented-control,
  .segmented-option,
  .action-row .el-button,
  .interface-action-row,
  .route-link {
    width: 100%;
  }
}
</style>
