<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceTraceDetail,
  getGovernanceTraceSummaries,
  getHetuRouteCalibration
} from '../../services/runtimeGateApi'

const { locale } = useI18n()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a',
  traceLimit: 10
})

const loading = reactive({
  page: false,
  detail: false,
  history: false
})

const errorMessage = ref('')
const routeCalibration = ref(null)
const recentTraces = ref([])
const traceDetail = ref(null)
const historyDetail = ref(null)
const selectedTraceId = ref('')
const selectedHistoryId = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')

const policyCards = computed(() => {
  const calibration = routeCalibration.value
  if (!calibration) {
    return []
  }
  return [
    field('routeProfile', 'Route profile', calibration.routeProfile),
    field('effectiveRouteOrder', 'effectiveRouteOrder', listText(calibration.effectiveRouteOrder)),
    field('declaredAllowedModes', isChinese.value ? '声明允许模式' : 'Declared modes', listText(calibration.declaredAllowedModes)),
    field('readonlyBoundary', 'readonlyBoundary', calibration.readonlyBoundary),
    field('liveVerificationStatus', isChinese.value ? '实时校验' : 'Live verification', calibration.liveVerificationStatus),
    field('evidenceSource', isChinese.value ? '证据来源' : 'Evidence source', calibration.evidenceSource),
    field('skipUnreadyModes', isChinese.value ? '跳过未就绪模式' : 'Skip unready modes', boolText(calibration.skipUnreadyModes)),
    field('implementationStage', isChinese.value ? '实现阶段' : 'Implementation stage', calibration.implementationStage)
  ]
})

const clusterEvidenceCards = computed(() => {
  const evidence = routeCalibration.value?.clusterEvidence
  if (!evidence) {
    return []
  }
  return [
    field('environmentLabel', isChinese.value ? '环境标签' : 'Environment', evidence.environmentLabel),
    field('clusterName', isChinese.value ? '集群名' : 'Cluster', evidence.clusterName),
    field('coordinatorEndpoint', isChinese.value ? '协调器入口' : 'Coordinator endpoint', evidence.coordinatorEndpoint),
    field('runbookRef', isChinese.value ? '运行手册' : 'Runbook ref', evidence.runbookRef),
    field('evidenceRef', isChinese.value ? '证据引用' : 'Evidence ref', evidence.evidenceRef),
    field('readonlyBoundary', 'readonlyBoundary', evidence.readonlyBoundary),
    field('liveVerificationStatus', isChinese.value ? '实时校验' : 'Live verification', evidence.liveVerificationStatus),
    field('operatorNotes', isChinese.value ? '运维备注' : 'Operator notes', evidence.operatorNotes)
  ]
})

const modeCards = computed(() => routeCalibration.value?.modeCalibrations || [])

const historyOptions = computed(() => traceDetail.value?.queryHistories || [])

const selectedHistorySummary = computed(() =>
  historyOptions.value.find(item => item.historyId === selectedHistoryId.value) || null
)

const decisionSummaryCards = computed(() => {
  const routeDecision = historyDetail.value?.routeDecision || {}
  const executionSummary = historyDetail.value?.executionSummary || {}
  return [
    field('selectedEngine', isChinese.value ? '选择引擎' : 'Selected engine', pick(routeDecision, ['selectedEngine', 'targetEngine', 'engine'])),
    field('ruleId', isChinese.value ? '规则标识' : 'Rule id', pick(routeDecision, ['ruleId', 'routeDecisionId', 'decisionId'])),
    field('routeProfile', 'Route profile', pick(routeDecision, ['routeProfile'])),
    field('routeOrder', 'Route order', listText(pick(routeDecision, ['routeOrder', 'attemptedModes']))),
    field('verification', isChinese.value ? '校验状态' : 'Verification', pick(routeDecision, ['verificationStatus', 'routeVerificationStatus'])),
    field('fallback', isChinese.value ? '回退说明' : 'Fallback note', pick(routeDecision, ['fallbackReason', 'degradeReason'])),
    field('executionMode', isChinese.value ? '执行模式' : 'Execution mode', executionSummary.executionMode),
    field('attemptedModes', isChinese.value ? '尝试模式' : 'Attempted modes', listText(executionSummary.attemptedModes))
  ].filter(item => displayValue(item.value) !== '-')
})

const traceDetailCards = computed(() => {
  const detail = traceDetail.value
  if (!detail) {
    return []
  }
  return [
    field('serviceCode', isChinese.value ? '服务编码' : 'Service code', detail.serviceCode),
    field('latestStatus', isChinese.value ? '最新状态' : 'Latest status', detail.latestStatus),
    field('queryHistoryCount', isChinese.value ? '历史数' : 'History count', detail.queryHistoryCount),
    field('nonSuccessEventCount', isChinese.value ? '异常事件数' : 'Non-success events', detail.nonSuccessEventCount),
    field('targetEngine', isChinese.value ? '目标引擎' : 'Target engine', detail.targetEngine),
    field('lastSeenAt', isChinese.value ? '最后时间' : 'Last seen at', formatTimestamp(detail.lastSeenAt))
  ]
})

const commentProtocolCards = computed(() => [
  {
    key: 'engineHint',
    title: 'engineHint',
    example: '/* engineHint=HETU */',
    summary: isChinese.value
      ? '通过注释或策略表达目标引擎偏好；真正采用哪个引擎仍以后端 routeDecision 为准。'
      : 'Use a comment or policy to express the preferred engine, while the backend routeDecision remains authoritative.'
  },
  {
    key: 'priority',
    title: 'priority',
    example: '/* priority=HIGH */',
    summary: isChinese.value
      ? '治理优先级可进入查询、批量、推荐与压测上下文，但不会替代路由证据。'
      : 'Priority flows into query, batch, recommendation, and benchmark context without replacing route evidence.'
  },
  {
    key: 'readonlyBoundary',
    title: 'readonlyBoundary',
    example: 'REPO_CLOSED_DEFAULT',
    summary: isChinese.value
      ? '当前页只读展示 calibration 快照和历史 routeDecision，不提供前端改写规则入口。'
      : 'This page is read-only and exposes calibration snapshots plus historical routeDecision evidence rather than rule editing.'
  }
])

const commentContextPreview = computed(() => historyDetail.value?.commentContext || {})
const recommendationRefs = computed(() => historyDetail.value?.recommendationRefs || [])

const routeSignalGroups = computed(() =>
  [
    {
      key: 'routeDecision',
      title: 'routeDecision',
      payload: historyDetail.value?.routeDecision
    },
    {
      key: 'commentContext',
      title: isChinese.value ? '注释上下文' : 'Comment context',
      payload: historyDetail.value?.commentContext
    },
    {
      key: 'executionSummary',
      title: isChinese.value ? '执行摘要' : 'Execution summary',
      payload: historyDetail.value?.executionSummary
    },
    {
      key: 'queryContext',
      title: isChinese.value ? '查询上下文' : 'Query context',
      payload: historyDetail.value?.queryContext
    }
  ].filter(group => isNonEmptyObject(group.payload))
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
    const nextTraceId = selectedTraceId.value || recentTraces.value[0]?.traceId || ''
    if (nextTraceId) {
      await loadTraceDetail(nextTraceId)
    } else {
      traceDetail.value = null
      historyDetail.value = null
      selectedTraceId.value = ''
      selectedHistoryId.value = ''
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadTraceDetail = async traceId => {
  if (!traceId) {
    traceDetail.value = null
    historyDetail.value = null
    selectedTraceId.value = ''
    selectedHistoryId.value = ''
    return
  }

  loading.detail = true
  errorMessage.value = ''
  selectedTraceId.value = traceId

  try {
    const detail = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-routing-governance-trace-detail'
    })
    traceDetail.value = detail
    const firstHistoryId = detail?.queryHistories?.[0]?.historyId || ''
    if (firstHistoryId) {
      await loadHistoryDetail(firstHistoryId)
    } else {
      historyDetail.value = null
      selectedHistoryId.value = ''
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const loadHistoryDetail = async historyId => {
  if (!historyId) {
    historyDetail.value = null
    selectedHistoryId.value = ''
    return
  }

  loading.history = true
  errorMessage.value = ''
  selectedHistoryId.value = historyId

  try {
    historyDetail.value = await getGovernanceQueryHistoryDetail(form.tenantId, historyId, {
      requestPrefix: 'frontend-routing-governance-history-detail'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.history = false
  }
}

const openParseRecord = () => {
  if (!selectedTraceId.value) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: {
      tenantId: form.tenantId,
      traceId: selectedTraceId.value
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

const isNonEmptyObject = value => value && typeof value === 'object' && !Array.isArray(value) && Object.keys(value).length > 0

const formatJson = value => JSON.stringify(value, null, 2)

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

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
    <header class="routing-hero sqlforge-panel">
      <div class="hero-copy">
        <p class="section-kicker sqlforge-code-label">routing governance</p>
        <h1>{{ isChinese ? '路由治理与历史决策详情' : 'Routing governance and decision detail' }}</h1>
        <p class="hero-summary">
          {{
            isChinese
              ? '当前仓库只暴露 route calibration 快照、query-history routeDecision 样例和 comment protocol 说明；不把不存在的 routing-rules 写接口伪装成已实现能力。'
              : 'The repository currently exposes route-calibration snapshots, query-history routeDecision samples, and the comment protocol baseline without pretending a missing routing-rules editor already exists.'
          }}
        </p>
      </div>

      <div class="hero-actions">
        <label class="field-label">
          <span>{{ isChinese ? '租户' : 'Tenant' }}</span>
          <input v-model.trim="form.tenantId" class="text-input" data-testid="routing-tenant-input">
        </label>
        <label class="field-label">
          <span>{{ isChinese ? 'Trace 数量' : 'Trace limit' }}</span>
          <input v-model.number="form.traceLimit" type="number" min="1" max="20" class="text-input">
        </label>
        <div class="hero-button-row">
          <button class="primary-button" data-testid="routing-refresh" @click="refreshPage">
            {{ isChinese ? '刷新路由证据' : 'Refresh routing evidence' }}
          </button>
          <button
            class="secondary-button"
            data-testid="routing-open-parse-record"
            :disabled="!selectedTraceId"
            @click="openParseRecord"
          >
            {{ isChinese ? '打开历史详情页' : 'Open parse-record page' }}
          </button>
        </div>
      </div>
    </header>

    <p v-if="errorMessage" class="error-banner" data-testid="routing-error">{{ errorMessage }}</p>

    <div class="routing-grid">
      <article class="sqlforge-panel current-policy-card" data-testid="routing-current-policy">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">current policy</p>
            <h2>{{ isChinese ? '当前路由校准' : 'Current route calibration' }}</h2>
          </div>
          <span class="status-pill" :class="{ 'status-pill-warn': routeCalibration?.liveVerificationStatus !== 'VERIFIED' }">
            {{ routeCalibration?.liveVerificationStatus || 'UNKNOWN' }}
          </span>
        </div>

        <p class="muted-copy">
          {{ routeCalibration?.summary || (isChinese ? '尚未返回 calibration summary。' : 'No calibration summary returned yet.') }}
        </p>

        <div class="summary-grid">
          <article v-for="item in policyCards" :key="item.key" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong :data-testid="`routing-policy-${item.key}`">{{ displayValue(item.value) }}</strong>
          </article>
        </div>

        <div class="section-heading nested-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">cluster evidence</p>
            <h3>{{ isChinese ? '集群证据与边界' : 'Cluster evidence and boundary' }}</h3>
          </div>
        </div>

        <div class="summary-grid">
          <article v-for="item in clusterEvidenceCards" :key="item.key" class="summary-card summary-card-compact">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ displayValue(item.value) }}</strong>
          </article>
        </div>

        <div class="mode-grid">
          <article
            v-for="item in modeCards"
            :key="item.mode"
            class="mode-card"
            data-testid="routing-mode-card"
          >
            <div class="mode-card-header">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ item.mode }}</p>
                <h3>{{ isChinese ? '路由模式校准' : 'Mode calibration' }}</h3>
              </div>
              <span class="status-pill" :class="{ 'status-pill-warn': !item.ready }">
                {{ item.readinessStatus || 'UNKNOWN' }}
              </span>
            </div>
            <div class="mode-chip-row">
              <span class="mode-chip">priority {{ item.priority }}</span>
              <span class="mode-chip">allowed {{ boolText(item.allowed) }}</span>
              <span class="mode-chip">ready {{ boolText(item.ready) }}</span>
              <span class="mode-chip">configured {{ boolText(item.configured) }}</span>
              <span class="mode-chip">adapter {{ boolText(item.adapterAvailable) }}</span>
              <span class="mode-chip">attempt {{ boolText(item.willAttemptInCurrentPolicy) }}</span>
            </div>
            <p class="muted-copy">{{ item.readinessReason || '-' }}</p>
            <pre class="code-block code-block-compact">{{ formatJson(item.routeParameters || {}) }}</pre>
          </article>
        </div>
      </article>

      <article class="sqlforge-panel trace-list-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">history samples</p>
            <h2>{{ isChinese ? '最近 Trace 与路由样例' : 'Recent traces and route samples' }}</h2>
          </div>
          <span class="section-badge">{{ recentTraces.length }}</span>
        </div>

        <p class="muted-copy">
          {{
            isChinese
              ? '这里展示治理历史中的 route decision 样例。真正的“当前规则”以上方 calibration 快照为准。'
              : 'This area shows route-decision samples from governance history. The actual current policy remains the calibration snapshot above.'
          }}
        </p>

        <div class="trace-list">
          <button
            v-for="item in recentTraces"
            :key="item.traceId"
            type="button"
            class="trace-item"
            :class="{ 'trace-item-active': selectedTraceId === item.traceId }"
            data-testid="routing-trace-item"
            @click="loadTraceDetail(item.traceId)"
          >
            <div class="trace-item-header">
              <strong>{{ item.traceId }}</strong>
              <span class="status-pill" :class="{ 'status-pill-warn': item.latestStatus !== 'SUCCESS' && item.latestStatus !== 'SUCCEEDED' }">
                {{ item.latestStatus || 'UNKNOWN' }}
              </span>
            </div>
            <span>{{ item.serviceCode || '-' }} · {{ formatTimestamp(item.lastSeenAt) }}</span>
            <span>{{ isChinese ? '历史' : 'History' }} {{ item.queryHistoryCount || 0 }} · {{ isChinese ? '异常' : 'Risk' }} {{ item.nonSuccessEventCount || 0 }}</span>
          </button>
        </div>

        <div v-if="traceDetail" class="trace-detail-section">
          <div class="summary-grid">
            <article v-for="item in traceDetailCards" :key="item.key" class="summary-card summary-card-compact">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </article>
          </div>

          <div class="history-pill-row">
            <button
              v-for="history in historyOptions"
              :key="history.historyId"
              type="button"
              class="history-pill"
              :class="{ 'history-pill-active': selectedHistoryId === history.historyId }"
              data-testid="routing-history-pill"
              @click="loadHistoryDetail(history.historyId)"
            >
              <strong>{{ history.reportCode || history.historyId }}</strong>
              <span>{{ history.datasourceCode || history.datasourceType || '-' }} · {{ history.historyType || '-' }}</span>
            </button>
          </div>
        </div>
      </article>
    </div>

    <div class="routing-grid routing-grid-bottom">
      <article class="sqlforge-panel decision-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">decision detail</p>
            <h2>{{ isChinese ? '历史路由决策详情' : 'Historical route decision detail' }}</h2>
          </div>
          <span class="section-badge">{{ selectedHistorySummary?.historyId || '-' }}</span>
        </div>

        <p v-if="loading.history" class="muted-copy">
          {{ isChinese ? '正在加载 query-history detail…' : 'Loading query-history detail…' }}
        </p>
        <p v-else-if="!historyDetail" class="muted-copy">
          {{ isChinese ? '当前没有可展示的历史决策详情。' : 'No historical decision detail is available yet.' }}
        </p>
        <template v-else>
          <div class="summary-grid">
            <article v-for="item in decisionSummaryCards" :key="item.key" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </article>
          </div>

          <div class="signal-grid">
            <article
              v-for="group in routeSignalGroups"
              :key="group.key"
              class="signal-card"
            >
              <div class="signal-card__header">
                <span class="summary-card-label">{{ group.title }}</span>
              </div>
              <pre class="code-block" :data-testid="group.key === 'routeDecision' ? 'routing-route-decision' : undefined">{{ formatJson(group.payload) }}</pre>
            </article>
          </div>

          <div v-if="recommendationRefs.length" class="reference-group">
            <div class="section-heading nested-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">recommendation refs</p>
                <h3>{{ isChinese ? '关联 recommendationRefs' : 'Related recommendationRefs' }}</h3>
              </div>
            </div>
            <div class="reference-list">
              <pre
                v-for="(item, index) in recommendationRefs"
                :key="`${index}-${item.recommendationId || item.id || 'recommendation'}`"
                class="code-block code-block-compact"
              >{{ formatJson(item) }}</pre>
            </div>
          </div>
        </template>
      </article>

      <article class="sqlforge-panel protocol-card" data-testid="routing-comment-protocol">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">comment protocol</p>
            <h2>{{ isChinese ? '注释协议与路由说明' : 'Comment protocol and routing notes' }}</h2>
          </div>
        </div>

        <p class="muted-copy">
          {{
            isChinese
              ? '仓库真值只定义 `engineHint` 与 `priority` 这类注释/上下文字段，不提供前端直接编辑 routing rule 的能力。'
              : 'Repository truth only defines comment and context fields such as `engineHint` and `priority`; it does not expose a frontend rule editor.'
          }}
        </p>

        <div class="protocol-grid">
          <article v-for="item in commentProtocolCards" :key="item.key" class="protocol-card-item">
            <div class="signal-card__header">
              <span class="summary-card-label">{{ item.title }}</span>
            </div>
            <pre class="code-block code-block-compact">{{ item.example }}</pre>
            <p class="muted-copy">{{ item.summary }}</p>
          </article>
        </div>

        <div class="section-heading nested-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">comment context sample</p>
            <h3>{{ isChinese ? '当前选中样例的 commentContext' : 'commentContext of the selected sample' }}</h3>
          </div>
        </div>
        <pre class="code-block">{{ formatJson(commentContextPreview) }}</pre>
      </article>
    </div>
  </section>
</template>

<style scoped>
.routing-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.routing-hero,
.current-policy-card,
.trace-list-card,
.decision-card,
.protocol-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.routing-hero {
  display: grid;
  gap: 20px;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 0.9fr);
}

.hero-copy h1,
.section-heading h2,
.section-heading h3,
.mode-card-header h3 {
  margin: 0;
}

.hero-summary,
.muted-copy {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px;
  border-radius: 14px;
  background: rgba(41, 41, 41, 0.84);
  border: 1px solid var(--sqlforge-border-default);
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

.hero-button-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.primary-button,
.secondary-button,
.trace-item,
.history-pill {
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

.routing-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
}

.routing-grid-bottom {
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
}

.summary-grid,
.mode-grid,
.signal-grid,
.protocol-grid {
  display: grid;
  gap: 14px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.summary-card,
.mode-card,
.signal-card,
.protocol-card-item {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
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

.summary-card strong,
.mode-card strong,
.trace-item strong,
.history-pill strong {
  color: var(--sqlforge-text-primary);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.nested-heading {
  margin-top: 4px;
}

.section-badge,
.status-pill,
.mode-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.status-pill-warn {
  border-color: rgba(214, 179, 48, 0.28);
  color: #ffd98a;
}

.mode-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.mode-card {
  gap: 14px;
}

.mode-card,
.trace-item,
.history-pill,
.protocol-card-item {
  display: flex;
  flex-direction: column;
}

.mode-card-header,
.trace-item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.mode-chip-row,
.history-pill-row,
.reference-list,
.trace-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.trace-list {
  flex-direction: column;
}

.trace-item,
.history-pill {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
  border-radius: 14px;
  padding: 14px 16px;
  gap: 6px;
  text-align: left;
}

.trace-item-active,
.history-pill-active {
  border-color: var(--sqlforge-color-brand-border);
}

.trace-detail-section,
.reference-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.signal-grid,
.protocol-grid {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.signal-card__header {
  margin-bottom: 10px;
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

@media (max-width: 1100px) {
  .routing-hero,
  .routing-grid,
  .routing-grid-bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .hero-button-row,
  .history-pill-row,
  .reference-list {
    flex-direction: column;
  }

  .primary-button,
  .secondary-button {
    width: 100%;
  }
}
</style>
