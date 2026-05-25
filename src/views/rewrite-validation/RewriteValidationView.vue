<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { SAMPLE_DATASOURCE_CODE, SAMPLE_TENANT_ID } from '../../config/tenantDefaults.mjs'
import {
  createRewriteValidationRun,
  createSqlRewriteRecord,
  formatRuntimeError,
  getGovernanceDatasources,
  getRecommendationPage,
  getRewriteValidationRuns,
  submitOptimizationTask,
  waitForOptimizationTask
} from '../../services/runtimeGateApi'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlCompareBlock from '../common/SqlCompareBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import { buildDatasourceOptions, withCurrentOption } from '../common/formComponentGovernance'
import {
  findDatasourceOption,
  firstDatasourceForEngine,
  groupGovernanceDatasources
} from '../common/governanceDatasourceOptions.mjs'

const { t } = useI18n()
const router = useRouter()

const form = reactive({
  tenantId: SAMPLE_TENANT_ID,
  datasourceType: 'AUTO',
  datasourceCode: SAMPLE_DATASOURCE_CODE,
  sourceKind: 'MANUAL',
  sourceId: 'manual-rewrite-validation',
  parseHistoryId: '',
  historyId: '',
  parseTaskId: '',
  batchId: '',
  reportCode: '',
  sqlFingerprint: '',
  evidenceLevel: 'STATIC_PARSE',
  parseDepth: 'DEEP',
  priority: 'NORMAL',
  issueScenes: ['SELECT_STAR'],
  validationRewriteRecordId: '',
  validationReason: t('rewriteValidation.defaults.validationReason'),
  sqlText:
    "SELECT * FROM orders WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id, id"
})

const loading = reactive({
  datasources: false,
  rewriteTask: false,
  recommendations: false,
  createRecord: false,
  createValidationRun: false,
  validationRuns: false
})

const governanceDatasources = ref([])
const activeTask = ref(null)
const relatedRecommendations = ref([])
const createdRewriteRecord = ref(null)
const validationRuns = ref([])
const selectedEvidencePayload = ref(null)
const evidenceDrawerVisible = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const followupActiveTab = ref('recommendations')

const datasourceTypeOptions = ['AUTO', 'HETU', 'HIVE', 'SPARK', 'CLICKHOUSE', 'GAUSSDB']
const sourceKindOptions = ['MANUAL', 'COMBINED_PARSE', 'STRUCTURE_PARSE', 'PARSE_BATCH', 'REPORT_BATCH', 'QUERY_HISTORY', 'SLOW_SQL']
const evidenceLevelOptions = ['STATIC_PARSE', 'ACCESS_PARSE', 'EXPLAIN_PLAN', 'RUNTIME_HISTORY', 'BENCHMARK', 'MIXED']
const parseDepthOptions = ['SHALLOW', 'NORMAL', 'DEEP']
const priorityOptions = ['LOW', 'NORMAL', 'HIGH']
const issueSceneOptions = [
  'SELECT_STAR',
  'OR_PREDICATE_INDEX_RISK',
  'NESTED_SUBQUERY_RISK',
  'LEADING_WILDCARD_LIKE_RISK'
]

const datasourceOptions = computed(() => buildDatasourceOptions(governanceDatasources.value))
const suggestion = computed(() => activeTask.value?.suggestion || null)
const artifacts = computed(() => normalizeArray(suggestion.value?.artifacts))
const benefits = computed(() => normalizeArray(suggestion.value?.benefits))
const costs = computed(() => normalizeArray(suggestion.value?.costs))
const risks = computed(() => normalizeArray(suggestion.value?.risks))
const recommendedSql = computed(() => artifactContent('REWRITTEN_SQL', 'candidateSql') || form.sqlText)
const appliedRules = computed(() => normalizeRuleTrace(artifactContent('REWRITE_RULE_TRACE', 'appliedRules')))
const astProfile = computed(() => parseJsonObject(artifactContent('AST_PROFILE', 'astProfile')))
const recommendationReport = computed(() =>
  parseJsonObject(artifactContent('REWRITE_RECOMMENDATION_REPORT', 'recommendationReport'))
)
const coreRecommendation = computed(() => {
  const selected = parseJsonObject(artifactContent('REWRITE_RECOMMENDATION_SELECTED', 'selectedRecommendation'))
  if (hasValue(selected.rewriteId)) {
    return selected
  }
  return normalizeArray(recommendationReport.value?.recommendations)[0] || {}
})
const algorithmConformance = computed(() =>
  parseJsonObject(artifactContent('REWRITE_ALGORITHM_CONFORMANCE', 'conformanceReport'))
)
const firstRecommendation = computed(() => relatedRecommendations.value[0] || null)
const activeRewriteRecordId = computed(
  () => form.validationRewriteRecordId || createdRewriteRecord.value?.rewriteRecordId || ''
)
const sourceType = computed(() => (['QUERY_HISTORY', 'SLOW_SQL'].includes(form.sourceKind) ? 'QUERY' : 'PARSE'))
const taskContextSourceType = computed(() => (form.sourceKind === 'MANUAL' ? 'COMBINED_PARSE' : form.sourceKind))
const recommendationSourceKind = computed(() => (form.sourceKind === 'MANUAL' ? 'COMBINED_PARSE' : form.sourceKind))
const canCreateRewriteRecord = computed(
  () => activeTask.value?.status === 'SUCCEEDED' && hasValue(form.sqlText) && hasValue(recommendedSql.value)
)
const canCreateValidationRun = computed(() => hasValue(activeRewriteRecordId.value))

const taskSummaryCards = computed(() => {
  const task = activeTask.value || {}
  return [
    card(t('rewriteValidation.fields.taskId'), task.taskId),
    card(t('rewriteValidation.fields.taskStatus'), task.status),
    card(t('rewriteValidation.fields.currentPhase'), task.currentPhase),
    card(t('rewriteValidation.fields.priority'), task.priority),
    card(t('rewriteValidation.fields.confidence'), suggestion.value?.confidenceScore),
    card(t('rewriteValidation.fields.sqlFingerprint'), form.sqlFingerprint || t('rewriteValidation.messages.backendFingerprint'))
  ].filter(item => hasValue(item.value))
})

const evidenceCards = computed(() => [
  card(t('rewriteValidation.fields.sourceType'), sourceType.value),
  card(t('rewriteValidation.fields.sourceKind'), form.sourceKind),
  card(t('rewriteValidation.fields.sourceId'), resolvedSourceId()),
  card(t('rewriteValidation.fields.evidenceLevel'), form.evidenceLevel),
  card(t('rewriteValidation.fields.validationMethod'), 'STATIC_REWRITE_TASK'),
  card(t('rewriteValidation.fields.comparisonStatus'), 'NOT_COMPARED')
])

const ruleRows = computed(() =>
  appliedRules.value.map((rule, index) => ({
    ruleCode: typeof rule === 'string' ? rule : rule.ruleCode || rule.rule || `RULE_${index + 1}`,
    action: typeof rule === 'string' ? 'APPLIED' : rule.action || rule.status || 'APPLIED',
    evidence: typeof rule === 'string' ? t('rewriteValidation.messages.ruleFromTrace') : displayValue(rule.evidence || rule.reason)
  }))
)

const astProfileCards = computed(() => {
  const profile = astProfile.value || {}
  return [
    card(t('rewriteValidation.fields.parserEngine'), profile.parserEngine),
    card(t('rewriteValidation.fields.statementType'), profile.statementType),
    card(t('rewriteValidation.fields.tableCount'), normalizeArray(profile.tables).length),
    card(t('rewriteValidation.fields.projectionCount'), profile.projectionCount),
    card(t('rewriteValidation.fields.predicateCount'), profile.predicateCount),
    card(t('rewriteValidation.fields.joinCount'), profile.joinCount),
    card(t('rewriteValidation.fields.duplicateOrderByKeyCount'), profile.duplicateOrderByKeyCount),
    card(t('rewriteValidation.fields.duplicateGroupByKeyCount'), profile.duplicateGroupByKeyCount),
    card(t('rewriteValidation.fields.selectStar'), boolText(profile.selectStar)),
    card(t('rewriteValidation.fields.repeatedSubqueryCount'), profile.repeatedSubqueryCount)
  ].filter(item => hasValue(item.value))
})

const coreRecommendationCards = computed(() =>
  [
    card(t('rewriteValidation.fields.algorithmStatus'), statusLabel(algorithmConformance.value?.algorithmStatus)),
    card(t('rewriteValidation.fields.generationStatus'), statusLabel(recommendationReport.value?.generationStatus)),
    card(t('rewriteValidation.fields.selectedRecommendationId'), coreRecommendation.value?.rewriteId),
    card(t('rewriteValidation.fields.confidence'), coreRecommendation.value?.confidence),
    card(t('rewriteValidation.fields.riskLevel'), coreRecommendation.value?.performance?.riskLevel || coreRecommendation.value?.attributes?.riskLevel),
    card(t('rewriteValidation.fields.scanReduction'), coreRecommendation.value?.performance?.scanReduction),
    card(t('rewriteValidation.fields.recommendationScore'), coreRecommendation.value?.score),
    card(t('rewriteValidation.fields.autoApplyAllowed'), boolText(coreRecommendation.value?.autoApplyAllowed))
  ].filter(item => hasValue(item.value))
)

const rewriteShapeChecks = computed(() => {
  const sql = String(recommendedSql.value || '')
  const upperSql = sql.toUpperCase()
  return [
    shapeCheck('rawCustomerSnapshot', sql.includes('raw_customer_snapshot'), 'raw_customer_snapshot'),
    shapeCheck('reportCustomerSnapshot', sql.includes('report_customer_snapshot'), 'report_customer_snapshot'),
    shapeCheck('baseAnchor', sql.includes('base_100_anchor'), 'base_100_anchor'),
    shapeCheck('metricByOrg', sql.includes('metric_by_org'), 'metric_by_org'),
    shapeCheck('growthByOrg', sql.includes('growth_by_org'), 'growth_by_org'),
    shapeCheck('unionAll', upperSql.includes('UNION ALL'), 'UNION ALL'),
    shapeCheck('noGroupingSets', !upperSql.includes('GROUPING SETS'), t('rewriteValidation.messages.noGroupingSetsDetected'))
  ]
})

const validationRunRows = computed(() => normalizeArray(validationRuns.value))

function card(label, value) {
  return { label, value }
}

function shapeCheck(key, passed, evidence) {
  return {
    key,
    label: t(`rewriteValidation.shapeChecks.${key}`),
    passed,
    evidence: passed ? evidence : t('rewriteValidation.messages.shapeTokenMissing', { token: evidence })
  }
}

function statusLabel(value) {
  return hasValue(value) ? String(value).replace(/_/g, ' ') : value
}

function hasValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function displayValue(value) {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasValue(value)) {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function boolText(value) {
  if (typeof value !== 'boolean') {
    return value
  }
  return value ? 'true' : 'false'
}

function normalizeArray(value) {
  return Array.isArray(value) ? value : []
}

function parseJsonObject(value) {
  if (!hasValue(value)) {
    return {}
  }
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  } catch {
    return {}
  }
}

function normalizeRuleTrace(value) {
  if (!hasValue(value)) {
    return []
  }
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function artifactContent(category, name) {
  const artifact = artifacts.value.find(item => item.category === category && item.name === name)
  return artifact?.content || ''
}

function compactObject(payload) {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) => {
      if (Array.isArray(value)) {
        return value.length > 0
      }
      return value !== undefined && value !== null && String(value).trim() !== ''
    })
  )
}

function resolvedSourceId() {
  return form.sourceId || form.historyId || form.parseHistoryId || form.parseTaskId || form.batchId || 'manual-rewrite-validation'
}

function buildTaskPayload() {
  return {
    tenantId: form.tenantId,
    taskType: 'REWRITE',
    sqlText: form.sqlText,
    sqlFingerprint: form.sqlFingerprint,
    datasourceType: form.datasourceType,
    taskContext: compactObject({
      parseDepth: form.parseDepth,
      priority: form.priority,
      sourceType: taskContextSourceType.value,
      sourceId: resolvedSourceId(),
      historyId: form.historyId || form.parseHistoryId,
      parseTaskId: form.parseTaskId,
      batchId: form.batchId,
      reportCode: form.reportCode,
      datasourceCode: form.datasourceCode,
      issueScenes: form.issueScenes
    })
  }
}

function buildRecommendationQuery() {
  return compactObject({
    pageNo: 1,
    pageSize: 5,
    recommendationType: 'REWRITE',
    sourceType: sourceType.value,
    sourceKind: recommendationSourceKind.value,
    sourceId: resolvedSourceId(),
    historyId: form.historyId || form.parseHistoryId,
    parseTaskId: form.parseTaskId,
    batchId: form.batchId,
    reportCode: form.reportCode,
    sortBy: 'createdAt',
    sortOrder: 'DESC'
  })
}

function buildRewriteRecordPayload() {
  return {
    tenantId: form.tenantId,
    recommendationId: firstRecommendation.value?.recommendationId || '',
    optimizationTaskId: activeTask.value?.taskId || '',
    sourceType: sourceType.value,
    sourceKind: form.sourceKind,
    sourceId: resolvedSourceId(),
    evidenceLevel: form.evidenceLevel,
    historyId: form.historyId,
    parseHistoryId: form.parseHistoryId,
    sqlFingerprint: form.sqlFingerprint,
    datasourceCode: form.datasourceCode,
    status: 'DRAFT',
    validationStatus: 'NOT_VALIDATED',
    activationStatus: 'INACTIVE',
    autoApplyAllowed: false,
    manualReviewRequired: true,
    alertStatus: 'NONE',
    originalSqlText: form.sqlText,
    recommendedSqlText: recommendedSql.value,
    ruleChain: ruleRows.value,
    diffSummary: {
      validationMethod: 'STATIC_REWRITE_TASK',
      comparisonStatus: 'NOT_COMPARED',
      differenceType: 'UNKNOWN',
      appliedRuleCount: appliedRules.value.length,
      evidenceBoundary: 'trial validation only'
    },
    risk: {
      manualReviewRequired: true,
      risks: risks.value
    },
    traceRefs: compactObject({
      optimizationTaskId: activeTask.value?.taskId,
      sourceKind: form.sourceKind,
      sourceId: resolvedSourceId(),
      historyId: form.historyId,
      parseHistoryId: form.parseHistoryId,
      parseTaskId: form.parseTaskId,
      batchId: form.batchId,
      reportCode: form.reportCode
    })
  }
}

function buildValidationRunPayload() {
  return {
    tenantId: form.tenantId,
    recommendationId: firstRecommendation.value?.recommendationId || createdRewriteRecord.value?.recommendationId || '',
    historyId: form.historyId,
    sqlFingerprint: form.sqlFingerprint,
    datasourceType: form.datasourceType,
    triggerReason: form.validationReason,
    status: 'SUCCEEDED',
    comparisonStatus: 'NOT_COMPARED',
    differenceType: 'UNKNOWN',
    autoApplyPaused: false,
    finishedAt: new Date().toISOString(),
    comparisonPolicy: {
      validationMethod: 'STATIC_REWRITE_TASK',
      readonlyOnly: true,
      fullResultPulledToFrontend: false
    },
    originalResultDigest: {
      source: 'FRONTEND_STATIC_REWRITE_VALIDATION',
      sqlLength: String(form.sqlText || '').length
    },
    recommendedResultDigest: {
      source: 'FRONTEND_STATIC_REWRITE_VALIDATION',
      sqlLength: String(recommendedSql.value || '').length
    },
    differenceSample: {
      evidenceBoundary: 'NOT_COMPARED_BY_FRONTEND',
      appliedRuleCount: appliedRules.value.length
    },
    executionEvidence: {
      source: 'SQL_REWRITE_VALIDATION_PAGE',
      optimizationTaskId: activeTask.value?.taskId || ''
    }
  }
}

async function loadGovernanceDatasources() {
  loading.datasources = true
  try {
    const response = await getGovernanceDatasources(form.tenantId, {
      requestPrefix: 'frontend-rewrite-validation-governance-datasources'
    })
    governanceDatasources.value = Array.isArray(response) ? response : []
    const grouped = groupGovernanceDatasources(governanceDatasources.value)
    const selectedDatasource =
      findDatasourceOption(grouped, form.datasourceCode, form.datasourceType)
      || firstDatasourceForEngine(grouped, form.datasourceType)
    if (selectedDatasource && selectedDatasource.datasourceCode !== form.datasourceCode) {
      form.datasourceCode = selectedDatasource.datasourceCode
      form.datasourceType = selectedDatasource.datasourceType || form.datasourceType
    }
  } catch {
    governanceDatasources.value = []
  } finally {
    loading.datasources = false
  }
}

async function loadRelatedRecommendations() {
  loading.recommendations = true
  try {
    const page = await getRecommendationPage(form.tenantId, buildRecommendationQuery(), {
      requestPrefix: 'frontend-rewrite-validation-recommendation-page'
    })
    relatedRecommendations.value = Array.isArray(page?.items) ? page.items : []
  } catch (error) {
    relatedRecommendations.value = []
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.recommendations = false
  }
}

async function runRewriteValidation() {
  loading.rewriteTask = true
  errorMessage.value = ''
  successMessage.value = ''
  createdRewriteRecord.value = null
  validationRuns.value = []
  followupActiveTab.value = 'recommendations'
  try {
    const initialTask = await submitOptimizationTask(buildTaskPayload(), {
      requestPrefix: 'frontend-rewrite-validation-task-submit'
    })
    activeTask.value = initialTask
    const terminalTask = await waitForOptimizationTask(initialTask.taskId, form.tenantId, {
      requestPrefix: 'frontend-rewrite-validation-task'
    })
    activeTask.value = terminalTask
    await loadRelatedRecommendations()
    successMessage.value = t('rewriteValidation.messages.taskSucceeded')
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.rewriteTask = false
  }
}

async function refreshRelatedRecommendations() {
  errorMessage.value = ''
  await loadRelatedRecommendations()
}

async function createRewriteRecordFromResult() {
  if (!canCreateRewriteRecord.value) {
    return
  }
  loading.createRecord = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    createdRewriteRecord.value = await createSqlRewriteRecord(buildRewriteRecordPayload(), {
      requestPrefix: 'frontend-rewrite-validation-record-create'
    })
    form.validationRewriteRecordId = createdRewriteRecord.value?.rewriteRecordId || ''
    followupActiveTab.value = 'validationRuns'
    successMessage.value = t('rewriteValidation.messages.recordCreated')
    await loadValidationRuns()
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.createRecord = false
  }
}

async function createValidationRunForRecord() {
  if (!canCreateValidationRun.value) {
    return
  }
  loading.createValidationRun = true
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const validationRun = await createRewriteValidationRun(
      form.tenantId,
      activeRewriteRecordId.value,
      buildValidationRunPayload(),
      {
        requestPrefix: 'frontend-rewrite-validation-run-create'
      }
    )
    successMessage.value = t('rewriteValidation.messages.validationRunCreated')
    validationRuns.value = [validationRun, ...validationRuns.value.filter(item => item.validationRunId !== validationRun.validationRunId)]
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.createValidationRun = false
  }
}

async function loadValidationRuns() {
  if (!activeRewriteRecordId.value) {
    validationRuns.value = []
    return
  }
  loading.validationRuns = true
  errorMessage.value = ''
  try {
    const runs = await getRewriteValidationRuns(form.tenantId, activeRewriteRecordId.value, {
      requestPrefix: 'frontend-rewrite-validation-run-list'
    })
    validationRuns.value = Array.isArray(runs) ? runs : []
  } catch (error) {
    validationRuns.value = []
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.validationRuns = false
  }
}

function openRecommendationCenter(recommendation = null) {
  router.push({
    path: ROUTE_PATHS.recommendationCenter,
    query: compactObject({
      tenantId: form.tenantId,
      recommendationId: recommendation?.recommendationId,
      sourceCategory: sourceType.value === 'QUERY' ? 'QUERY' : 'SQL_PARSE',
      sourceObjectId: resolvedSourceId(),
      sourceType: sourceType.value,
      sourceKind: recommendationSourceKind.value,
      sourceId: resolvedSourceId(),
      historyId: form.historyId || form.parseHistoryId,
      parseTaskId: form.parseTaskId,
      tab: recommendation?.recommendationId ? 'sqlDiff' : ''
    })
  })
}

function openRewriteRecordCenter() {
  if (!activeRewriteRecordId.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.recommendationCenter,
    query: compactObject({
      tenantId: form.tenantId,
      rewriteRecordId: activeRewriteRecordId.value,
      recommendationId: firstRecommendation.value?.recommendationId || createdRewriteRecord.value?.recommendationId || '',
      tab: 'rewriteLifecycle'
    })
  })
}

function openEvidence(payload) {
  selectedEvidencePayload.value = payload
  evidenceDrawerVisible.value = true
}

function resetWorkspace() {
  activeTask.value = null
  relatedRecommendations.value = []
  createdRewriteRecord.value = null
  validationRuns.value = []
  errorMessage.value = ''
  successMessage.value = ''
  followupActiveTab.value = 'recommendations'
}

watch(
  () => form.tenantId,
  () => {
    loadGovernanceDatasources()
  }
)

watch(
  () => activeRewriteRecordId.value,
  () => {
    loadValidationRuns()
  }
)

onMounted(loadGovernanceDatasources)
</script>

<template>
  <section class="runtime-page rewrite-validation-page" data-testid="rewrite-validation-page">
    <header class="rewrite-hero">
      <div class="rewrite-hero__copy">
        <p class="section-kicker sqlforge-code-label">{{ t('rewriteValidation.eyebrow') }}</p>
        <h1 class="runtime-title" data-testid="rewrite-validation-title">{{ t('rewriteValidation.title') }}</h1>
        <p class="runtime-summary" data-testid="rewrite-validation-summary">{{ t('rewriteValidation.summary') }}</p>
        <p class="boundary-copy" data-testid="rewrite-validation-boundary">
          {{ t('rewriteValidation.boundary') }}
        </p>
      </div>
      <div class="rewrite-hero__actions">
        <el-button :loading="loading.recommendations" @click="refreshRelatedRecommendations">
          {{ t('rewriteValidation.actions.refreshRecommendations') }}
        </el-button>
        <el-button @click="resetWorkspace">{{ t('rewriteValidation.actions.reset') }}</el-button>
      </div>
    </header>

    <p v-if="errorMessage" class="message-banner message-banner-danger" data-testid="rewrite-validation-error">
      {{ errorMessage }}
    </p>
    <p v-if="successMessage" class="message-banner message-banner-success" data-testid="rewrite-validation-success">
      {{ successMessage }}
    </p>

    <div class="rewrite-layout">
      <article class="workspace-panel rewrite-input-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('rewriteValidation.sections.inputKicker') }}</p>
            <h2 class="section-title">{{ t('rewriteValidation.sections.inputTitle') }}</h2>
            <p class="section-summary">{{ t('rewriteValidation.sections.inputSummary') }}</p>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.tenantId') }}</span>
            <el-input v-model.trim="form.tenantId" data-testid="rewrite-validation-tenant-id" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.datasourceType') }}</span>
            <el-select v-model="form.datasourceType" data-testid="rewrite-validation-datasource-type">
              <el-option v-for="option in datasourceTypeOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.datasourceCode') }}</span>
            <el-select
              v-model="form.datasourceCode"
              filterable
              allow-create
              default-first-option
              :loading="loading.datasources"
              data-testid="rewrite-validation-datasource-code"
            >
              <el-option
                v-for="item in withCurrentOption(datasourceOptions, form.datasourceCode)"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.sourceKind') }}</span>
            <el-select v-model="form.sourceKind" data-testid="rewrite-validation-source-kind">
              <el-option v-for="option in sourceKindOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.sourceId') }}</span>
            <el-input v-model.trim="form.sourceId" data-testid="rewrite-validation-source-id" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.evidenceLevel') }}</span>
            <el-select v-model="form.evidenceLevel" data-testid="rewrite-validation-evidence-level">
              <el-option v-for="option in evidenceLevelOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.historyId') }}</span>
            <el-input v-model.trim="form.historyId" data-testid="rewrite-validation-history-id" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.parseHistoryId') }}</span>
            <el-input v-model.trim="form.parseHistoryId" data-testid="rewrite-validation-parse-history-id" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.parseTaskId') }}</span>
            <el-input v-model.trim="form.parseTaskId" data-testid="rewrite-validation-parse-task-id" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.sqlFingerprint') }}</span>
            <el-input v-model.trim="form.sqlFingerprint" data-testid="rewrite-validation-sql-fingerprint" />
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.parseDepth') }}</span>
            <el-select v-model="form.parseDepth" data-testid="rewrite-validation-parse-depth">
              <el-option v-for="option in parseDepthOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>
          <label class="field-block">
            <span class="field-label">{{ t('rewriteValidation.fields.priority') }}</span>
            <el-select v-model="form.priority" data-testid="rewrite-validation-priority">
              <el-option v-for="option in priorityOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>
          <label class="field-block field-block-wide">
            <span class="field-label">{{ t('rewriteValidation.fields.issueScenes') }}</span>
            <el-select v-model="form.issueScenes" multiple filterable data-testid="rewrite-validation-issue-scenes">
              <el-option v-for="option in issueSceneOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </label>
          <div class="field-block field-block-wide">
            <SqlEditorField
              v-model="form.sqlText"
              :label="t('rewriteValidation.fields.originalSql')"
              :rows="24"
              max-height="840px"
              :copy-label="t('rewriteValidation.actions.copy')"
              :format-label="t('rewriteValidation.actions.format')"
              data-testid="rewrite-validation-sql-input"
            />
          </div>
        </div>

        <div class="action-row">
          <el-button
            type="primary"
            :loading="loading.rewriteTask"
            data-testid="rewrite-validation-submit"
            @click="runRewriteValidation"
          >
            {{ t('rewriteValidation.actions.runRewriteValidation') }}
          </el-button>
          <el-button
            :disabled="!canCreateRewriteRecord"
            :loading="loading.createRecord"
            data-testid="rewrite-validation-create-record"
            @click="createRewriteRecordFromResult"
          >
            {{ t('rewriteValidation.actions.createRewriteRecord') }}
          </el-button>
          <el-button :disabled="!activeTask" @click="openEvidence(activeTask)">
            {{ t('rewriteValidation.actions.viewTaskEvidence') }}
          </el-button>
        </div>
      </article>

      <article class="workspace-panel rewrite-result-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">{{ t('rewriteValidation.sections.resultKicker') }}</p>
            <h2 class="section-title">{{ t('rewriteValidation.sections.resultTitle') }}</h2>
            <p class="section-summary">{{ t('rewriteValidation.sections.resultSummary') }}</p>
          </div>
          <el-button data-testid="rewrite-validation-open-recommendations" @click="openRecommendationCenter()">
            {{ t('rewriteValidation.actions.openRecommendationCenter') }}
          </el-button>
        </div>

        <p v-if="!activeTask" class="empty-state" data-testid="rewrite-validation-empty">
          {{ t('rewriteValidation.messages.emptyResult') }}
        </p>

        <template v-else>
          <div class="status-banner" :class="activeTask.status === 'SUCCEEDED' ? 'status-banner-success' : 'status-banner-warning'">
            <strong data-testid="rewrite-validation-status">{{ activeTask.status }}</strong>
            <span>{{ activeTask.currentPhase }}</span>
          </div>

          <div class="summary-grid">
            <article v-for="item in taskSummaryCards" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <section class="result-section" data-testid="rewrite-validation-recommendation-summary">
            <h3 class="detail-title">{{ t('rewriteValidation.sections.suggestionTitle') }}</h3>
            <p class="result-copy">{{ displayValue(suggestion?.summary) }}</p>
            <p class="result-copy result-copy-muted">{{ displayValue(suggestion?.primaryRecommendation) }}</p>
          </section>

          <section class="result-section" data-testid="rewrite-validation-core-recommendation">
            <div class="section-heading section-heading-tight">
              <div>
                <h3 class="detail-title">{{ t('rewriteValidation.sections.coreRecommendationTitle') }}</h3>
                <p class="section-summary">{{ t('rewriteValidation.sections.coreRecommendationSummary') }}</p>
              </div>
              <el-button text @click="openEvidence({ recommendationReport, coreRecommendation, algorithmConformance })">
                {{ t('rewriteValidation.actions.viewRawEvidence') }}
              </el-button>
            </div>
            <div class="summary-grid">
              <article v-for="item in coreRecommendationCards" :key="item.label" class="summary-card summary-card-compact">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
            <div class="rewrite-check-grid" data-testid="rewrite-validation-shape-checks">
              <article
                v-for="item in rewriteShapeChecks"
                :key="item.key"
                class="rewrite-check-item"
                :class="{ 'rewrite-check-item-pass': item.passed }"
              >
                <el-tag :type="item.passed ? 'success' : 'warning'" size="small">
                  {{ item.passed ? t('rewriteValidation.messages.shapePass') : t('rewriteValidation.messages.shapeReview') }}
                </el-tag>
                <div>
                  <strong>{{ item.label }}</strong>
                  <p>{{ item.evidence }}</p>
                </div>
              </article>
            </div>
          </section>

          <section class="result-section" data-testid="rewrite-validation-sql-compare">
            <h3 class="detail-title">{{ t('rewriteValidation.sections.diffTitle') }}</h3>
            <SqlCompareBlock
              :original-sql="form.sqlText"
              :recommended-sql="recommendedSql"
              :original-label="t('rewriteValidation.fields.originalSql')"
              :recommended-label="t('rewriteValidation.fields.recommendedSql')"
              :copy-label="t('rewriteValidation.actions.copy')"
              :format-label="t('rewriteValidation.actions.format')"
            />
          </section>

          <section class="result-section">
            <div class="section-heading section-heading-tight">
              <h3 class="detail-title">{{ t('rewriteValidation.sections.ruleRiskTitle') }}</h3>
              <el-button text @click="openEvidence({ artifacts, benefits, costs, risks })">
                {{ t('rewriteValidation.actions.viewRawEvidence') }}
              </el-button>
            </div>
            <div class="evidence-grid">
              <div class="evidence-list" data-testid="rewrite-validation-rule-chain">
                <span class="summary-card-label">{{ t('rewriteValidation.fields.ruleChain') }}</span>
                <article v-for="(item, index) in ruleRows" :key="`${item.ruleCode}-${index}`" class="evidence-item">
                  <strong>{{ item.ruleCode }}</strong>
                  <span>{{ item.action }}</span>
                  <p>{{ item.evidence }}</p>
                </article>
                <p v-if="!ruleRows.length" class="empty-inline">{{ t('rewriteValidation.messages.noRuleApplied') }}</p>
              </div>
              <div class="evidence-list" data-testid="rewrite-validation-risk-list">
                <span class="summary-card-label">{{ t('rewriteValidation.fields.semanticRisks') }}</span>
                <article v-for="(item, index) in risks" :key="`${item.category || 'risk'}-${index}`" class="evidence-item">
                  <strong>{{ displayValue(item.category) }}</strong>
                  <span>{{ displayValue(item.level) }}</span>
                  <p>{{ displayValue(item.description) }}</p>
                  <p>{{ displayValue(item.suggestion) }}</p>
                </article>
                <p v-if="!risks.length" class="empty-inline">{{ t('rewriteValidation.messages.noRisk') }}</p>
              </div>
            </div>
          </section>

          <section class="result-section" data-testid="rewrite-validation-parse-evidence">
            <h3 class="detail-title">{{ t('rewriteValidation.sections.parseEvidenceTitle') }}</h3>
            <div class="summary-grid">
              <article v-for="item in astProfileCards" :key="item.label" class="summary-card summary-card-compact">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
          </section>

          <section class="result-section rewrite-followup-section">
            <el-tabs v-model="followupActiveTab" class="rewrite-followup-tabs">
              <el-tab-pane :label="t('rewriteValidation.sections.recommendationTitle')" name="recommendations">
                <section class="followup-pane" data-testid="rewrite-validation-recommendation-links">
                  <div class="section-heading section-heading-tight">
                    <div>
                      <p class="section-kicker sqlforge-code-label">{{ t('rewriteValidation.sections.recommendationKicker') }}</p>
                      <h3 class="detail-title">{{ t('rewriteValidation.sections.recommendationTitle') }}</h3>
                    </div>
                    <el-button :loading="loading.recommendations" @click="refreshRelatedRecommendations">
                      {{ t('rewriteValidation.actions.refreshRecommendations') }}
                    </el-button>
                  </div>
                  <div class="summary-grid">
                    <article v-for="item in evidenceCards" :key="item.label" class="summary-card summary-card-compact">
                      <span class="summary-card-label">{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                    </article>
                  </div>
                  <el-table :data="relatedRecommendations" border class="data-table" data-testid="rewrite-validation-recommendation-table">
                    <el-table-column prop="recommendationId" :label="t('rewriteValidation.fields.recommendationId')" min-width="220" />
                    <el-table-column prop="validationStatus" :label="t('rewriteValidation.fields.validationStatus')" min-width="150" />
                    <el-table-column prop="riskLevel" :label="t('rewriteValidation.fields.riskLevel')" min-width="120" />
                    <el-table-column prop="benefitLevel" :label="t('rewriteValidation.fields.benefitLevel')" min-width="120" />
                    <el-table-column :label="t('rewriteValidation.fields.actions')" min-width="130">
                      <template #default="{ row }">
                        <el-button text @click="openRecommendationCenter(row)">
                          {{ t('rewriteValidation.actions.openDetail') }}
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <p v-if="!relatedRecommendations.length" class="empty-state">{{ t('rewriteValidation.messages.noRecommendation') }}</p>
                </section>
              </el-tab-pane>
              <el-tab-pane :label="t('rewriteValidation.sections.validationTitle')" name="validationRuns">
                <section class="followup-pane" data-testid="rewrite-validation-run-panel">
                  <div class="section-heading">
                    <div>
                      <p class="section-kicker sqlforge-code-label">{{ t('rewriteValidation.sections.validationKicker') }}</p>
                      <h3 class="detail-title">{{ t('rewriteValidation.sections.validationTitle') }}</h3>
                      <p class="section-summary">{{ t('rewriteValidation.sections.validationSummary') }}</p>
                    </div>
                  </div>
                  <div class="form-grid form-grid-single">
                    <label class="field-block">
                      <span class="field-label">{{ t('rewriteValidation.fields.rewriteRecordId') }}</span>
                      <el-input v-model.trim="form.validationRewriteRecordId" data-testid="rewrite-validation-record-id" />
                    </label>
                    <label class="field-block">
                      <span class="field-label">{{ t('rewriteValidation.fields.validationReason') }}</span>
                      <el-input v-model.trim="form.validationReason" data-testid="rewrite-validation-reason" />
                    </label>
                  </div>
                  <div class="action-row">
                    <el-button
                      :disabled="!canCreateValidationRun"
                      :loading="loading.createValidationRun"
                      data-testid="rewrite-validation-create-run"
                      @click="createValidationRunForRecord"
                    >
                      {{ t('rewriteValidation.actions.createValidationRun') }}
                    </el-button>
                    <el-button :disabled="!canCreateValidationRun" :loading="loading.validationRuns" @click="loadValidationRuns">
                      {{ t('rewriteValidation.actions.refreshValidationRuns') }}
                    </el-button>
                    <el-button :disabled="!canCreateValidationRun" data-testid="rewrite-validation-open-record" @click="openRewriteRecordCenter">
                      {{ t('rewriteValidation.actions.openRewriteRecord') }}
                    </el-button>
                  </div>
                  <el-table :data="validationRunRows" border class="data-table" data-testid="rewrite-validation-run-table">
                    <el-table-column prop="validationRunId" :label="t('rewriteValidation.fields.validationRunId')" min-width="220" />
                    <el-table-column prop="status" :label="t('rewriteValidation.fields.validationRunStatus')" min-width="130" />
                    <el-table-column prop="comparisonStatus" :label="t('rewriteValidation.fields.comparisonStatus')" min-width="150" />
                    <el-table-column prop="differenceType" :label="t('rewriteValidation.fields.differenceType')" min-width="160" />
                    <el-table-column prop="autoApplyPaused" :label="t('rewriteValidation.fields.autoApplyPaused')" min-width="150" />
                  </el-table>
                  <p v-if="!validationRunRows.length" class="empty-state">{{ t('rewriteValidation.messages.noValidationRun') }}</p>
                </section>
              </el-tab-pane>
            </el-tabs>
          </section>
        </template>
      </article>
    </div>

    <el-drawer v-model="evidenceDrawerVisible" :title="t('rewriteValidation.sections.rawEvidenceTitle')" size="46%">
      <SqlCodeBlock
        :value="JSON.stringify(selectedEvidencePayload || {}, null, 2)"
        :label="t('rewriteValidation.sections.rawEvidenceTitle')"
        :copy-label="t('rewriteValidation.actions.copy')"
        :format-enabled="false"
        data-testid="rewrite-validation-raw-evidence"
      />
    </el-drawer>
  </section>
</template>

<style scoped>
.rewrite-validation-page {
  gap: var(--sqlforge-space-4);
}

.workspace-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 8px;
  background: var(--sqlforge-surface-2);
}

.rewrite-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: var(--sqlforge-space-4);
  align-items: start;
  padding: 0 0 var(--sqlforge-space-4);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.rewrite-hero__copy,
.rewrite-hero__actions,
.workspace-panel,
.result-section,
.evidence-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.rewrite-hero__actions {
  flex-flow: row wrap;
  justify-content: flex-end;
  align-items: flex-start;
}

.boundary-copy,
.runtime-summary,
.section-summary,
.result-copy,
.empty-state,
.empty-inline,
.evidence-item p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.boundary-copy {
  max-width: 980px;
  color: var(--sqlforge-text-primary);
}

.rewrite-layout {
  display: grid;
  grid-template-columns: minmax(340px, 0.78fr) minmax(0, 1.22fr);
  gap: var(--sqlforge-space-4);
  align-items: start;
}

.workspace-panel {
  padding: var(--sqlforge-space-4);
  min-width: 0;
  overflow: hidden;
}

.section-heading,
.action-row {
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.section-heading {
  align-items: flex-start;
}

.section-heading-tight {
  align-items: center;
}

.action-row {
  flex-wrap: wrap;
  justify-content: flex-start;
}

.section-kicker,
.field-label,
.summary-card-label {
  margin: 0 0 8px;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.form-grid,
.summary-grid,
.evidence-grid {
  display: grid;
  gap: 12px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(150px, 1fr));
}

.rewrite-input-panel .form-grid {
  grid-template-columns: 1fr;
}

.form-grid-single {
  grid-template-columns: 1fr;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.field-block :deep(.el-input),
.field-block :deep(.el-select),
.field-block :deep(.el-select__wrapper) {
  width: 100%;
  min-width: 0;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.summary-card,
.evidence-item {
  min-width: 0;
  padding: 0 0 var(--sqlforge-space-3);
}

.summary-card {
  min-height: auto;
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.summary-card-compact {
  min-height: auto;
}

.summary-card strong,
.evidence-item strong {
  color: var(--sqlforge-text-primary);
  word-break: break-word;
}

.evidence-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.rewrite-check-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 10px;
}

.rewrite-check-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
  padding: 10px 0;
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.rewrite-check-item strong {
  color: var(--sqlforge-text-primary);
}

.rewrite-check-item p {
  margin: 4px 0 0;
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.result-section {
  padding-top: var(--sqlforge-space-3);
  border-top: 1px solid var(--sqlforge-border-subtle);
}

.rewrite-followup-section {
  padding-top: var(--sqlforge-space-4);
}

.rewrite-followup-tabs :deep(.el-tabs__header) {
  margin: 0 0 var(--sqlforge-space-3);
}

.rewrite-followup-tabs :deep(.el-tab-pane),
.followup-pane {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
  min-width: 0;
}

.evidence-item {
  border-left: 2px solid var(--sqlforge-border-strong);
  background: transparent;
  padding-left: var(--sqlforge-space-3);
}

.rewrite-result-panel :deep(.sql-compare-block__panes) {
  grid-template-columns: repeat(2, minmax(300px, 1fr));
}

.rewrite-result-panel :deep(.sql-compare-pane) {
  min-width: 300px;
}

.evidence-item span {
  display: block;
  margin-top: 4px;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.result-copy-muted {
  color: var(--sqlforge-text-muted);
}

.status-banner,
.message-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-radius: 8px;
  padding: 12px 14px;
}

.status-banner-success,
.message-banner-success {
  border: 1px solid rgba(16, 185, 129, 0.3);
  background: rgba(16, 185, 129, 0.14);
  color: var(--sqlforge-text-primary);
}

.status-banner-warning {
  border: 1px solid rgba(251, 191, 36, 0.28);
  background: rgba(251, 191, 36, 0.12);
  color: var(--sqlforge-text-primary);
}

.message-banner-danger {
  border: 1px solid rgba(248, 113, 113, 0.36);
  background: rgba(239, 68, 68, 0.14);
  color: #fecaca;
}

.data-table {
  width: 100%;
}

@media (max-width: 900px) {
  .rewrite-hero,
  .rewrite-layout,
  .form-grid,
  .evidence-grid {
    grid-template-columns: 1fr;
  }

  .rewrite-hero__actions {
    align-items: flex-start;
  }
}
</style>
