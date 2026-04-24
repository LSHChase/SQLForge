<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import {
  formatRuntimeError,
  getGovernanceMessageStats,
  getGovernanceTraceDetail,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  lookupGovernanceTraces,
  retryGovernanceFailedMessages
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a',
  remediationTenantId: 'system',
  traceId: '',
  taskId: '',
  reportId: '',
  windowStart: '',
  windowEnd: '',
  limit: 12
})

const loadingLookup = ref(false)
const loadingDetail = ref(false)
const loadingStats = ref(false)
const retrying = ref(false)
const lookupResults = ref([])
const detail = ref(null)
const stats = ref(null)
const statsBeforeRetry = ref(null)
const statsAfterRetry = ref(null)
const retryResult = ref(null)
const errorMessage = ref('')
const hasMore = ref(false)
const nextCursor = ref('')
const activeFilters = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const activeTraceId = computed(() => detail.value?.traceId || '')
const matchedCount = computed(() => lookupResults.value.length)
const compensationCount = computed(() =>
  lookupResults.value.filter(trace => isCompensationTrace(trace.traceId)).length
)
const reportLinkedCount = computed(() =>
  lookupResults.value.filter(trace => hasDisplayValue(trace.reportId) || (trace.exportRecordCount || 0) > 0).length
)
const searchCriteria = computed(() =>
  [
    {
      key: 'traceId',
      label: isChinese.value ? 'Trace 反查' : 'Trace lookup',
      value: form.traceId
    },
    {
      key: 'taskId',
      label: isChinese.value ? 'Task 反查' : 'Task lookup',
      value: form.taskId
    },
    {
      key: 'reportId',
      label: isChinese.value ? 'Report 反查' : 'Report lookup',
      value: form.reportId
    }
  ].filter(item => hasDisplayValue(item.value))
)
const selectedSummary = computed(() =>
  lookupResults.value.find(trace => trace.traceId === activeTraceId.value) || null
)
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
  if ((detail.value.exportRecordCount || 0) > 0 || hasDisplayValue(detail.value.reportId)) {
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
  return statsBeforeRetry.value.failed - statsAfterRetry.value.failed
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
    { key: 'total', label: isChinese.value ? '消息总数' : 'Total messages', value: stats.value.total },
    { key: 'pending', label: isChinese.value ? '待补偿' : 'Pending backlog', value: stats.value.pending },
    { key: 'failed', label: isChinese.value ? '失败待修复' : 'Failed messages', value: stats.value.failed },
    { key: 'consumed', label: isChinese.value ? '已消费' : 'Consumed', value: stats.value.consumed }
  ]
})
const decisionCards = computed(() => [
  {
    key: 'failure-type',
    label: isChinese.value ? '失败类型' : 'Failure type',
    value: failureType.value
  },
  {
    key: 'compensation-state',
    label: isChinese.value ? '补偿状态' : 'Compensation state',
    value: compensationState.value
  },
  {
    key: 'writeback-state',
    label: isChinese.value ? '回写状态' : 'Write-back state',
    value: writeBackState.value
  },
  {
    key: 'queue-impact',
    label: isChinese.value ? '队列影响' : 'Queue impact',
    value: queueImpact.value
  },
  {
    key: 'acceptance-state',
    label: isChinese.value ? '验收信号' : 'Acceptance state',
    value: acceptanceState.value
  }
])

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (!hasDisplayValue(value)) {
    return '-'
  }
  return String(value)
}

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

const isCompensationTrace = traceId => String(traceId || '').startsWith(GOVERNANCE_COMPENSATION_TRACE_PREFIX)

const normalizeFilters = () => ({
  traceId: String(form.traceId || '').trim(),
  taskId: String(form.taskId || '').trim(),
  reportId: String(form.reportId || '').trim(),
  windowStart: String(form.windowStart || '').trim(),
  windowEnd: String(form.windowEnd || '').trim()
})

const syncRouteQuery = query => {
  router.replace({
    path: ROUTE_PATHS.auditTroubleshooting,
    query
  })
}

const buildDrillQuery = source => {
  const filters = activeFilters.value
    ? activeFilters.value
    : {
        traceId: source?.traceId,
        taskId: source?.taskId,
        reportId: source?.reportId
      }
  const query = {
    tenantId: form.tenantId,
    remediationTenantId: form.remediationTenantId,
    limit: String(form.limit)
  }
  if (hasDisplayValue(filters?.traceId)) {
    query.traceId = String(filters.traceId)
  }
  if (hasDisplayValue(filters?.taskId)) {
    query.taskId = String(filters.taskId)
  }
  if (hasDisplayValue(filters?.reportId)) {
    query.reportId = String(filters.reportId)
  }
  if (hasDisplayValue(filters?.windowStart)) {
    query.windowStart = String(filters.windowStart)
  }
  if (hasDisplayValue(filters?.windowEnd)) {
    query.windowEnd = String(filters.windowEnd)
  }
  return query
}

const loadQueueStats = async () => {
  loadingStats.value = true
  try {
    stats.value = await getGovernanceMessageStats(form.remediationTenantId, {
      requestPrefix: 'frontend-audit-troubleshooting-message-stats'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingStats.value = false
  }
}

const loadTraceDetail = async traceId => {
  if (!traceId) {
    detail.value = null
    return
  }

  loadingDetail.value = true
  errorMessage.value = ''

  try {
    detail.value = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-audit-troubleshooting-trace-detail'
    })
  } catch (error) {
    detail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingDetail.value = false
  }
}

const applyLookupPage = async (pageResponse, append = false) => {
  const items = Array.isArray(pageResponse?.items) ? pageResponse.items : []
  lookupResults.value = append ? [...lookupResults.value, ...items] : items
  hasMore.value = Boolean(pageResponse?.hasMore)
  nextCursor.value = pageResponse?.nextCursor || ''

  if (!append) {
    await loadTraceDetail(lookupResults.value[0]?.traceId || '')
  }
}

const runLookup = async () => {
  if (!hasDisplayValue(form.traceId) && !hasDisplayValue(form.taskId) && !hasDisplayValue(form.reportId)) {
    errorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项后再执行处置决策反查。'
      : 'Enter at least one of traceId, taskId, or reportId before running the remediation lookup.'
    lookupResults.value = []
    detail.value = null
    hasMore.value = false
    nextCursor.value = ''
    activeFilters.value = null
    return
  }

  loadingLookup.value = true
  errorMessage.value = ''
  activeFilters.value = normalizeFilters()
  syncRouteQuery({
    tenantId: form.tenantId,
    remediationTenantId: form.remediationTenantId,
    limit: String(form.limit),
    ...activeFilters.value
  })

  try {
    const lookupPage = await lookupGovernanceTraces(form.tenantId, activeFilters.value, form.limit, {
      requestPrefix: 'frontend-audit-troubleshooting-lookups'
    })
    await Promise.all([
      applyLookupPage(lookupPage, false),
      loadQueueStats()
    ])
  } catch (error) {
    lookupResults.value = []
    detail.value = null
    hasMore.value = false
    nextCursor.value = ''
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingLookup.value = false
  }
}

const loadMoreResults = async () => {
  if (!hasMore.value || !nextCursor.value || !activeFilters.value) {
    return
  }

  loadingLookup.value = true
  errorMessage.value = ''

  try {
    const lookupPage = await lookupGovernanceTraces(
      form.tenantId,
      {
        ...activeFilters.value,
        cursor: nextCursor.value
      },
      form.limit,
      {
        requestPrefix: 'frontend-audit-troubleshooting-lookups-more'
      }
    )
    await applyLookupPage(lookupPage, true)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingLookup.value = false
  }
}

const retryFailedMessages = async () => {
  retrying.value = true
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
    retrying.value = false
  }
}

const clearLookup = async () => {
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  form.windowStart = ''
  form.windowEnd = ''
  lookupResults.value = []
  detail.value = null
  errorMessage.value = ''
  hasMore.value = false
  nextCursor.value = ''
  activeFilters.value = null
  retryResult.value = null
  statsBeforeRetry.value = null
  statsAfterRetry.value = null
  syncRouteQuery({
    tenantId: form.tenantId,
    remediationTenantId: form.remediationTenantId,
    limit: String(form.limit)
  })
  await loadQueueStats()
}

const openSystemBacklog = () => {
  router.push('/system')
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

onMounted(async () => {
  if (hasDisplayValue(route.query.tenantId)) {
    form.tenantId = String(route.query.tenantId)
  }
  if (hasDisplayValue(route.query.remediationTenantId)) {
    form.remediationTenantId = String(route.query.remediationTenantId)
  }
  if (hasDisplayValue(route.query.limit)) {
    form.limit = Number(route.query.limit) || 12
  }
  form.traceId = String(route.query.traceId || '')
  form.taskId = String(route.query.taskId || '')
  form.reportId = String(route.query.reportId || '')
  form.windowStart = String(route.query.windowStart || '')
  form.windowEnd = String(route.query.windowEnd || '')

  await loadQueueStats()
  if (form.traceId || form.taskId || form.reportId) {
    await runLookup()
  }
})
</script>

<template>
  <section class="runtime-page" data-testid="audit-troubleshooting-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('auditTroubleshooting.title') }}</h1>
        <p class="runtime-summary">{{ t('auditTroubleshooting.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页把 trace/task/report 取证结果与治理消息队列影响合并成处置决策视图，支持真实 retry、跳转 backlog、回到历史诊断与打开修复证据。'
            : 'This page merges trace/task/report forensics with governance queue impact into one remediation decision view, exposing real retry, backlog drill-through, parse-record navigation, and repair evidence actions.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">decision lookup</p>
            <h2 class="section-title">
              {{ isChinese ? '故障范围、队列影响与决策输入' : 'Failure scope, queue impact and decision inputs' }}
            </h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '业务租户' : 'Business tenant' }}</span>
            <el-input v-model="form.tenantId" data-testid="audit-troubleshooting-tenant-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '治理租户' : 'Governance tenant' }}</span>
            <el-input v-model="form.remediationTenantId" data-testid="audit-troubleshooting-remediation-tenant-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '返回数量' : 'Lookup limit' }}</span>
            <el-input v-model="form.limit" data-testid="audit-troubleshooting-limit" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? 'Trace ID' : 'Trace ID' }}</span>
            <el-input v-model="form.traceId" data-testid="audit-troubleshooting-trace-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Task ID' : 'Task ID' }}</span>
            <el-input v-model="form.taskId" data-testid="audit-troubleshooting-task-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Report ID' : 'Report ID' }}</span>
            <el-input v-model="form.reportId" data-testid="audit-troubleshooting-report-id" />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="loadingLookup"
            data-testid="audit-troubleshooting-run-lookup"
            @click="runLookup"
          >
            {{ isChinese ? '执行处置反查' : 'Run remediation lookup' }}
          </el-button>
          <el-button
            :loading="loadingStats"
            data-testid="audit-troubleshooting-refresh-queue"
            @click="loadQueueStats"
          >
            {{ isChinese ? '刷新队列影响' : 'Refresh queue impact' }}
          </el-button>
          <el-button data-testid="audit-troubleshooting-clear-lookup" @click="clearLookup">
            {{ isChinese ? '清空条件' : 'Clear criteria' }}
          </el-button>
          <el-button
            v-if="hasMore"
            :loading="loadingLookup"
            data-testid="audit-troubleshooting-load-more"
            @click="loadMoreResults"
          >
            {{ isChinese ? '加载更早证据' : 'Load older evidence' }}
          </el-button>
        </div>

        <div class="lookup-chip-list">
          <span
            v-for="item in searchCriteria"
            :key="item.key"
            class="lookup-chip"
          >
            {{ item.label }}: {{ item.value }}
          </span>
          <span v-if="!searchCriteria.length" class="lookup-chip lookup-chip-muted">
            {{
              isChinese
                ? '输入 trace / task / report 后执行处置决策反查。'
                : 'Enter a trace, task, or report id and then run the remediation lookup.'
            }}
          </span>
        </div>

        <div class="summary-card-grid">
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '命中 trace' : 'Matched traces' }}</span>
            <strong data-testid="audit-troubleshooting-match-count">{{ matchedCount }}</strong>
          </article>
          <article class="summary-card summary-card-warning">
            <span class="summary-card-label">{{ isChinese ? '补偿 trace' : 'Compensation traces' }}</span>
            <strong data-testid="audit-troubleshooting-compensation-count">{{ compensationCount }}</strong>
          </article>
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '报告回写链' : 'Report-linked traces' }}</span>
            <strong data-testid="audit-troubleshooting-report-count">{{ reportLinkedCount }}</strong>
          </article>
        </div>

        <div class="queue-card-grid">
          <article
            v-for="card in queueCards"
            :key="card.key"
            class="queue-card"
          >
            <span class="queue-card-label">{{ card.label }}</span>
            <strong
              class="queue-card-value"
              :data-testid="`audit-troubleshooting-queue-${card.key}`"
            >
              {{ card.value }}
            </strong>
          </article>
        </div>

        <div
          v-if="hasMore"
          class="result-banner result-banner-warning"
          data-testid="audit-troubleshooting-has-more"
        >
          <strong>{{ isChinese ? '仍有更早处置链' : 'Older remediation chains available' }}</strong>
          <span>{{ nextCursor || '-' }}</span>
        </div>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="audit-troubleshooting-error"
        >
          {{ errorMessage }}
        </div>

        <p v-else-if="!lookupResults.length" class="empty-state">
          {{
            isChinese
              ? '命中结果会展示故障链与队列影响，并提供处置入口。'
              : 'Matched chains render here with queue impact and remediation actions.'
          }}
        </p>

        <div class="trace-list">
          <button
            v-for="trace in lookupResults"
            :key="trace.traceId"
            type="button"
            class="trace-item"
            :class="{ 'trace-item-active': activeTraceId === trace.traceId }"
            data-testid="audit-troubleshooting-result-item"
            @click="loadTraceDetail(trace.traceId)"
          >
            <div class="trace-item-header">
              <div>
                <p class="trace-item-service sqlforge-code-label">{{ trace.serviceCode || '-' }}</p>
                <h3>{{ trace.resourceId || trace.traceId }}</h3>
              </div>
              <span
                class="trace-status-pill"
                :class="
                  trace.latestStatus === 'SUCCESS' || trace.latestStatus === 'SUCCEEDED'
                    ? 'trace-status-success'
                    : 'trace-status-warning'
                "
              >
                {{ trace.latestStatus || '-' }}
              </span>
            </div>
            <p class="trace-item-meta">
              {{ trace.traceId }} · {{ formatTimestamp(trace.lastSeenAt) }}
            </p>
          </button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">remediation decision</p>
            <h2 class="section-title">
              {{ isChinese ? '处置动作与验收信号' : 'Remediation actions and acceptance signals' }}
            </h2>
          </div>
        </div>

        <p v-if="!detail && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '选择左侧命中 trace 后，这里会显示失败类型、补偿状态、回写状态和验收信号。'
              : 'After you select a matched trace, the failure type, compensation state, write-back state, and acceptance signals render here.'
          }}
        </p>

        <template v-if="detail">
          <div
            class="result-banner"
            :class="
              acceptanceState === 'REPAIRED' || acceptanceState === 'READY_FOR_ACCEPTANCE'
                ? 'result-banner-success'
                : 'result-banner-warning'
            "
          >
            <strong data-testid="audit-troubleshooting-detail-trace-id">{{ detail.traceId }}</strong>
            <span data-testid="audit-troubleshooting-acceptance-banner">{{ acceptanceState }}</span>
          </div>

          <div class="decision-grid">
            <article
              v-for="card in decisionCards"
              :key="card.key"
              class="decision-card"
            >
              <span class="decision-card-label">{{ card.label }}</span>
              <strong :data-testid="`audit-troubleshooting-${card.key}`">{{ card.value }}</strong>
            </article>
          </div>

          <div class="action-row action-row-wrap">
            <el-button
              type="primary"
              :loading="retrying"
              data-testid="audit-troubleshooting-retry"
              @click="retryFailedMessages"
            >
              {{ isChinese ? '重试失败消息' : 'Retry failed messages' }}
            </el-button>
            <el-button
              data-testid="audit-troubleshooting-open-system"
              @click="openSystemBacklog"
            >
              {{ isChinese ? '打开治理 backlog' : 'Open governance backlog' }}
            </el-button>
            <el-button
              data-testid="audit-troubleshooting-open-repair-evidence"
              @click="openRepairEvidence"
            >
              {{ isChinese ? '打开修复证据' : 'Open repair evidence' }}
            </el-button>
            <el-button
              data-testid="audit-troubleshooting-open-parse-record"
              @click="openParseRecord"
            >
              {{ isChinese ? '回到历史诊断' : 'Back to parse record' }}
            </el-button>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '服务编码' : 'Service code' }}</span>
              <strong data-testid="audit-troubleshooting-detail-service-code">{{ detail.serviceCode || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '任务 ID' : 'Task ID' }}</span>
              <strong data-testid="audit-troubleshooting-detail-task-id">{{ displayValue(detail.taskId) }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '报告 ID' : 'Report ID' }}</span>
              <strong data-testid="audit-troubleshooting-detail-report-id">{{ displayValue(detail.reportId) }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '最后发生时间' : 'Last seen at' }}</span>
              <strong>{{ formatTimestamp(detail.lastSeenAt) }}</strong>
            </div>
          </div>

          <template v-if="retryResult">
            <div class="retry-summary-grid">
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '重试状态' : 'Retry status' }}</span>
                <strong data-testid="audit-troubleshooting-retry-status">{{ retryResult.status }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '重试数量' : 'Retried count' }}</span>
                <strong data-testid="audit-troubleshooting-retry-count">{{ retryResult.retriedCount }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? 'failed 降幅' : 'Failed delta' }}</span>
                <strong data-testid="audit-troubleshooting-failed-delta">{{ failedDelta }}</strong>
              </div>
              <div class="evidence-item">
                <span class="evidence-label">{{ isChinese ? '修复结果' : 'Repair outcome' }}</span>
                <strong data-testid="audit-troubleshooting-repair-outcome">
                  {{ retryImproved ? 'REPAIRED' : 'RETRY_ACCEPTED' }}
                </strong>
              </div>
            </div>
          </template>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96)),
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.12), transparent 45%);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.08);
}

.runtime-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.9fr);
  gap: 20px;
  padding: 28px;
}

.runtime-eyebrow,
.section-kicker,
.trace-item-service {
  margin: 0 0 8px;
  color: #0f766e;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.runtime-title,
.section-title {
  margin: 0;
  color: #0f172a;
}

.runtime-summary,
.runtime-note,
.trace-item-meta,
.empty-state {
  color: #475569;
}

.runtime-note {
  margin: 0;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(226, 232, 240, 0.7);
  line-height: 1.6;
}

.runtime-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.96fr) minmax(0, 1.04fr);
  gap: 20px;
}

.runtime-grid > .surface-card {
  padding: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.form-grid,
.summary-card-grid,
.queue-card-grid,
.evidence-grid,
.retry-summary-grid,
.decision-grid {
  display: grid;
  gap: 14px;
}

.form-grid,
.summary-card-grid,
.queue-card-grid,
.decision-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.evidence-grid,
.retry-summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 18px;
}

.field-block,
.summary-card,
.queue-card,
.evidence-item,
.decision-card,
.trace-item {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.82);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.field-block-wide {
  grid-column: span 2;
}

.field-label,
.summary-card-label,
.queue-card-label,
.evidence-label,
.decision-card-label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.action-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-top: 18px;
}

.action-row-wrap {
  flex-wrap: wrap;
}

.summary-card,
.queue-card,
.evidence-item,
.decision-card {
  padding: 16px 18px;
}

.summary-card strong,
.queue-card strong,
.evidence-item strong,
.decision-card strong {
  font-size: 18px;
  color: #0f172a;
}

.summary-card-warning {
  background: rgba(255, 247, 237, 0.92);
  border-color: rgba(251, 146, 60, 0.18);
}

.lookup-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.lookup-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
  font-size: 13px;
}

.lookup-chip-muted {
  background: rgba(241, 245, 249, 0.9);
  color: #64748b;
}

.result-banner {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-banner-success {
  background: rgba(236, 253, 245, 0.9);
  color: #166534;
}

.result-banner-warning {
  background: rgba(255, 247, 237, 0.92);
  color: #9a3412;
}

.result-banner-danger {
  background: rgba(254, 242, 242, 0.92);
  color: #b91c1c;
}

.trace-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.trace-item {
  width: 100%;
  text-align: left;
  padding: 16px 18px;
  cursor: pointer;
}

.trace-item-active {
  border-color: rgba(14, 165, 233, 0.4);
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.12);
}

.trace-item-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.trace-item-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
}

.trace-status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.trace-status-success {
  background: rgba(220, 252, 231, 0.95);
  color: #166534;
}

.trace-status-warning {
  background: rgba(255, 237, 213, 0.95);
  color: #9a3412;
}

.empty-state {
  margin: 18px 0 0;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(241, 245, 249, 0.9);
}

@media (max-width: 1100px) {
  .runtime-grid,
  .runtime-hero,
  .form-grid,
  .summary-card-grid,
  .queue-card-grid,
  .evidence-grid,
  .retry-summary-grid,
  .decision-grid {
    grid-template-columns: 1fr;
  }

  .field-block-wide {
    grid-column: span 1;
  }
}
</style>
