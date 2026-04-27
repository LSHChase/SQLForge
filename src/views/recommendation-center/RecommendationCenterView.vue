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
  getRecommendations,
  getRecommendationTrace
} from '../../services/runtimeGateApi'

const { locale } = useI18n()
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
const recommendationTrace = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')

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
    option('ALL', isChinese.value ? '全部' : 'All', counts.ALL),
    option('REQUIRES_DISPATCH', isChinese.value ? '需协同' : 'Requires dispatch', counts.REQUIRES_DISPATCH),
    option('DISPATCH_READY', 'Dispatch ready', counts.DISPATCH_READY),
    option('HIGH_BENEFIT', isChinese.value ? '高收益' : 'High benefit', counts.HIGH_BENEFIT),
    option('HIGH_RISK', isChinese.value ? '高风险' : 'High risk', counts.HIGH_RISK),
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
    field('recommendationType', isChinese.value ? '推荐类型' : 'Recommendation type', recommendation.recommendationType),
    field('status', 'Status', recommendation.status),
    field('benefitLevel', 'benefitLevel', recommendation.benefitLevel),
    field('riskLevel', 'riskLevel', recommendation.riskLevel),
    field('targetEngine', isChinese.value ? '目标引擎' : 'Target engine', recommendation.targetEngine),
    field('targetDatasource', isChinese.value ? '目标数据源' : 'Target datasource', recommendation.targetDatasource),
    field('requiresDispatch', isChinese.value ? '需协同' : 'Requires dispatch', boolText(recommendation.requiresDispatch)),
    field('logicalObjectKey', 'logicalObjectKey', recommendation.logicalObjectKey)
  ]
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
    field('allowedEventStatuses', isChinese.value ? '允许状态' : 'Allowed statuses', listText(contract.allowedEventStatuses)),
    field('allowedDispatchTypes', isChinese.value ? '允许类型' : 'Allowed types', listText(contract.allowedDispatchTypes)),
    field('auditBoundary', isChinese.value ? '审计边界' : 'Audit boundary', contract.auditBoundary),
    field('residualOwner', isChinese.value ? '剩余责任方' : 'Residual owner', contract.residualOwner)
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
    recommendationTrace.value = null
    return
  }

  loading.detail = true
  errorMessage.value = ''
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

const formatJson = value => JSON.stringify(value, null, 2)

onMounted(() => {
  refreshPage()
})
</script>

<template>
  <section class="recommendation-page" data-testid="recommendation-page">
    <header class="recommendation-hero sqlforge-panel">
      <div class="hero-copy">
        <p class="section-kicker sqlforge-code-label">recommendation center</p>
        <h1>{{ isChinese ? '推荐与加速中心' : 'Recommendation and acceleration center' }}</h1>
        <p class="hero-summary">
          {{
            isChinese
              ? '页面负责消费 recommendation、dispatchEvents 与 traceability 证据；SQLForge 仍然只管理建议、事件和回执，不执行推荐 SQL、不主动装数。'
              : 'This center consumes recommendation, dispatchEvents, and traceability evidence while SQLForge continues to manage suggestions, events, and callbacks only without executing recommended SQL or loading data.'
          }}
        </p>
      </div>

      <div class="hero-actions">
        <label class="field-label">
          <span>{{ isChinese ? '租户' : 'Tenant' }}</span>
          <input v-model.trim="form.tenantId" class="text-input" data-testid="recommendation-tenant-input">
        </label>
        <div class="hero-button-row">
          <button class="primary-button" data-testid="recommendation-refresh" @click="refreshPage">
            {{ isChinese ? '刷新推荐中心' : 'Refresh center' }}
          </button>
          <button class="secondary-button" @click="openAccelerationWorkbench">
            {{ isChinese ? '打开解析工作台' : 'Open parse workbench' }}
          </button>
          <button class="secondary-button" @click="openRoutingGovernance">
            {{ isChinese ? '打开路由治理' : 'Open routing governance' }}
          </button>
        </div>
      </div>
    </header>

    <p v-if="errorMessage" class="error-banner" data-testid="recommendation-error">{{ errorMessage }}</p>

    <div class="recommendation-grid">
      <article class="sqlforge-panel recommendation-list-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">recommendation categories</p>
            <h2>{{ isChinese ? '推荐分类' : 'Recommendation categories' }}</h2>
          </div>
          <span class="section-badge">{{ recommendations.length }}</span>
        </div>

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
            class="recommendation-card"
            :class="{ 'recommendation-card-active': selectedRecommendationId === item.recommendationId }"
            data-testid="recommendation-card"
            @click="loadRecommendation(item.recommendationId)"
          >
            <div class="recommendation-card-header">
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
              <span class="mini-pill">benefit {{ item.benefitLevel || 'UNKNOWN' }}</span>
              <span class="mini-pill">risk {{ item.riskLevel || 'UNKNOWN' }}</span>
              <span class="mini-pill">dispatch {{ boolText(item.requiresDispatch) || 'false' }}</span>
            </div>
          </button>
        </div>
      </article>

      <article class="sqlforge-panel recommendation-detail-card" data-testid="recommendation-detail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">recommendation detail</p>
            <h2>{{ isChinese ? '收益、风险与 SQL 详情' : 'Benefit, risk, and SQL detail' }}</h2>
          </div>
          <span class="section-badge">{{ selectedRecommendation?.recommendationId || '-' }}</span>
        </div>

        <p v-if="loading.detail" class="muted-copy">
          {{ isChinese ? '正在加载 recommendation detail…' : 'Loading recommendation detail…' }}
        </p>
        <p v-else-if="!selectedRecommendation" class="muted-copy">
          {{ isChinese ? '当前没有可展示的 recommendation。' : 'No recommendation is available to display yet.' }}
        </p>
        <template v-else>
          <div class="summary-grid">
            <article v-for="item in summaryCards" :key="item.key" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </article>
          </div>

          <div class="detail-copy-grid">
            <article class="detail-copy-card">
              <div class="signal-card__header">
                <span class="summary-card-label">{{ isChinese ? '预期收益' : 'Expected gain' }}</span>
              </div>
              <p class="detail-copy-text">{{ selectedRecommendation.expectedGain || '-' }}</p>
            </article>
            <article class="detail-copy-card">
              <div class="signal-card__header">
                <span class="summary-card-label">{{ isChinese ? '风险摘要' : 'Risk summary' }}</span>
              </div>
              <p class="detail-copy-text">{{ selectedRecommendation.riskSummary || '-' }}</p>
            </article>
            <article class="detail-copy-card">
              <div class="signal-card__header">
                <span class="summary-card-label">{{ isChinese ? '推荐原因' : 'Reason' }}</span>
              </div>
              <p class="detail-copy-text">{{ selectedRecommendation.reason || '-' }}</p>
            </article>
          </div>

          <div class="sql-grid">
            <article class="sql-card">
              <div class="signal-card__header">
                <span class="summary-card-label">{{ isChinese ? '源 SQL' : 'Source SQL' }}</span>
              </div>
              <pre class="code-block">{{ selectedRecommendation.sourceSqlText || '' }}</pre>
            </article>
            <article class="sql-card">
              <div class="signal-card__header">
                <span class="summary-card-label">recommendedSqlText</span>
              </div>
              <pre class="code-block">{{ selectedRecommendation.recommendedSqlText || '' }}</pre>
            </article>
          </div>
        </template>
      </article>

      <article class="sqlforge-panel dispatch-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">dispatch governance</p>
            <h2>{{ isChinese ? 'dispatch 合同与追溯' : 'Dispatch contract and traceability' }}</h2>
          </div>
          <button class="secondary-button compact-button" :disabled="!recommendationTrace?.reportCode" @click="openParseRecord">
            {{ isChinese ? '打开历史页' : 'Open history page' }}
          </button>
        </div>

        <div class="summary-grid" data-testid="recommendation-dispatch-contract">
          <article v-for="item in contractCards" :key="item.key" class="summary-card summary-card-compact">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ displayValue(item.value) }}</strong>
          </article>
        </div>

        <p class="muted-copy">
          {{
            isChinese
              ? '当前协同边界固定为 coordinationMode=PULL_ONLY：外部模块负责真实装数、预热执行和底层变更，SQLForge 只保留 recommendation 与 dispatch 回执审计。'
              : 'The current collaboration boundary is fixed at coordinationMode=PULL_ONLY: external modules own real data loading, prewarm execution, and storage changes while SQLForge keeps recommendation plus dispatch callback evidence only.'
          }}
        </p>

        <div class="traceability-group" data-testid="recommendation-trace-refs">
          <div class="signal-card__header">
            <span class="summary-card-label">{{ isChinese ? '关联追溯键' : 'Traceability refs' }}</span>
          </div>
          <div class="pill-row">
            <span v-for="item in traceabilityCards" :key="item.key" class="mini-pill">
              {{ item.label }}: {{ displayValue(item.value) }}
            </span>
          </div>
          <pre class="code-block code-block-compact">{{ formatJson(recommendationTrace?.traceRefs || {}) }}</pre>
        </div>

        <div class="dispatch-event-list">
          <article
            v-for="item in selectedDispatchEvents"
            :key="item.dispatchEventId"
            class="dispatch-event-card"
            data-testid="recommendation-dispatch-event"
          >
            <div class="recommendation-card-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.dispatchType }}</p>
                <h3>{{ item.dispatchEventId }}</h3>
              </div>
              <span class="status-pill" :class="{ 'status-pill-warn': item.status === 'FAILED' }">
                {{ item.status }}
              </span>
            </div>
            <p class="muted-copy">{{ item.resultMessage || (isChinese ? '等待外部回执。' : 'Waiting for an external callback.') }}</p>
            <div class="pill-row">
              <span class="mini-pill">report {{ item.reportCode || '-' }}</span>
              <span class="mini-pill">logical {{ item.logicalObjectKey || '-' }}</span>
              <span class="mini-pill">engine {{ item.targetEngine || '-' }}</span>
            </div>
            <pre class="code-block code-block-compact">{{ formatJson(item.statusHistory || []) }}</pre>
          </article>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.recommendation-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.recommendation-hero,
.recommendation-list-card,
.recommendation-detail-card,
.dispatch-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.recommendation-hero {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.9fr);
}

.hero-copy h1,
.section-heading h2,
.recommendation-card-header h3 {
  margin: 0;
}

.hero-summary,
.muted-copy,
.detail-copy-text {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-actions,
.summary-card,
.detail-copy-card,
.sql-card,
.dispatch-event-card,
.recommendation-card {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 14px;
  background: rgba(41, 41, 41, 0.84);
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: var(--sqlforge-text-secondary);
}

.text-input {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.hero-button-row,
.filter-row,
.pill-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.primary-button,
.secondary-button,
.filter-chip,
.recommendation-card {
  cursor: pointer;
}

.primary-button,
.secondary-button {
  min-height: 42px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  padding: 0 18px;
  font-weight: 500;
}

.compact-button {
  min-height: 36px;
}

.primary-button {
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  border-color: var(--sqlforge-text-primary);
}

.secondary-button {
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
}

.secondary-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.error-banner {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(120, 28, 28, 0.18);
  border: 1px solid rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

.recommendation-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.2fr) minmax(0, 1fr);
}

.recommendation-list,
.dispatch-event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recommendation-card,
.dispatch-event-card,
.detail-copy-card,
.sql-card,
.traceability-group {
  padding: 16px;
  border-radius: 14px;
}

.recommendation-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: left;
}

.recommendation-card-active {
  border-color: var(--sqlforge-color-brand-border);
}

.recommendation-card-header,
.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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

.summary-grid,
.detail-copy-grid,
.sql-grid {
  display: grid;
  gap: 14px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.detail-copy-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.sql-grid {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.summary-card {
  padding: 16px;
  border-radius: 18px;
}

.summary-card-compact {
  padding: 14px;
}

.summary-card-label {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--sqlforge-text-muted);
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
  .recommendation-grid,
  .recommendation-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-button-row,
  .filter-row,
  .pill-row {
    flex-direction: column;
  }

  .primary-button,
  .secondary-button {
    width: 100%;
  }
}
</style>
