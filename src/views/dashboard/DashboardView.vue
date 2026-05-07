<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { useTenantStore } from '../../stores'
import {
  getDispatchContract,
  formatRuntimeError,
  getDispatchEvents,
  getGovernanceMessageStats,
  getGovernanceQueryHistoryPage,
  getParseStatisticsByIssueScene,
  getParseStatisticsImportantUrgent,
  getParseStatisticsOverview,
  getRecommendations
} from '../../services/runtimeGateApi'

const { locale } = useI18n()
const router = useRouter()
const tenantStore = useTenantStore()

const form = reactive({
  tenantId: tenantStore.tenantId || 'tenant-a'
})

const loading = ref(false)
const errorMessage = ref('')
const overview = ref(null)
const issueScenes = ref([])
const importantUrgent = ref([])
const messageStats = ref(null)
const dispatchContract = ref(null)
const queryHistoryPage = ref(null)
const dispatchEvents = ref([])
const recommendations = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')
const historyItems = computed(() => queryHistoryPage.value?.items || [])
const accessCounts = computed(() => queryHistoryPage.value?.classificationSummary?.accessChannelCounts || {})
const historyWindowStats = computed(() => {
  const stats = {
    total: historyItems.value.length,
    success: 0,
    failure: 0,
    partial: 0,
    cacheHit: 0,
    rewriteHit: 0,
    accelerationHit: 0
  }
  historyItems.value.forEach(item => {
    const status = String(item.resultStatus || '').toUpperCase()
    if (['SUCCESS', 'SUCCEEDED'].includes(status)) {
      stats.success += 1
    } else if (['FAILED', 'FAIL'].includes(status)) {
      stats.failure += 1
    } else if (status) {
      stats.partial += 1
    }
    if (item.cacheHit === true) {
      stats.cacheHit += 1
    }
    if (item.rewriteApplied === true) {
      stats.rewriteHit += 1
    }
    if (item.accelerationApplied === true) {
      stats.accelerationHit += 1
    }
  })
  return stats
})

const recommendationStats = computed(() => {
  const stats = {
    total: recommendations.value.length,
    requiresDispatch: 0,
    dispatchReady: 0,
    highRisk: 0
  }
  recommendations.value.forEach(item => {
    if (item.requiresDispatch === true) {
      stats.requiresDispatch += 1
    }
    if (String(item.status || '').toUpperCase() === 'DISPATCH_READY') {
      stats.dispatchReady += 1
    }
    if (['HIGH', 'CRITICAL'].includes(String(item.riskLevel || '').toUpperCase())) {
      stats.highRisk += 1
    }
  })
  return stats
})

const dispatchFailures = computed(() =>
  dispatchEvents.value.filter(item => String(item.status || '').toUpperCase() === 'FAILED')
)

const openRiskCount = computed(() => {
  const failed = Number(messageStats.value?.failed || 0)
  const urgent = importantUrgent.value.filter(item => item.urgent === true).length
  return failed + urgent + dispatchFailures.value.length + recommendationStats.value.highRisk
})

const historyWindowRate = value =>
  historyWindowStats.value.total > 0 ? `${((Number(value || 0) / historyWindowStats.value.total) * 100).toFixed(1)}%` : '0.0%'

const topEngineSample = computed(() => {
  const counts = {}
  historyItems.value.forEach(item => {
    const engine = String(item.targetEngine || '').trim()
    if (engine) {
      counts[engine] = (counts[engine] || 0) + 1
    }
  })
  const [engine, count] =
    Object.entries(counts).sort((left, right) => Number(right[1]) - Number(left[1]))[0] || []
  return {
    engine: engine || '-',
    count: Number(count || 0)
  }
})

const topAccessChannelSample = computed(() => {
  const [channel, count] =
    Object.entries(accessCounts.value).sort((left, right) => Number(right[1]) - Number(left[1]))[0] || []
  return {
    channel: channel || '-',
    count: Number(count || 0)
  }
})

const overviewPills = computed(() => [
  form.tenantId,
  tenantStore.defaultEngine || 'HETU',
  isChinese.value ? 'audited evidence only' : 'audited evidence only'
])

const metricCards = computed(() => {
  const totalSqlCount = Number(overview.value?.totalSqlCount || 0)
  const issueSqlCount = Number(overview.value?.issueSqlCount || 0)
  const issueRate = totalSqlCount > 0 ? `${((issueSqlCount / totalSqlCount) * 100).toFixed(1)}%` : '0.0%'
  const governanceBacklog = Number(messageStats.value?.pending || 0) + Number(messageStats.value?.failed || 0)
  return [
    {
      key: 'total-sql',
      label: isChinese.value ? '解析 SQL 样本' : 'Parsed SQL samples',
      value: totalSqlCount,
      trend: `${Number(overview.value?.issueSceneCount || 0)} scenes`,
      detail: isChinese.value
        ? '来自 parse-statistics overview 的当前总览样本。'
        : 'Taken from the current parse-statistics overview evidence window.',
      tone: totalSqlCount > 0 ? 'success' : 'neutral'
    },
    {
      key: 'success-rate',
      label: isChinese.value ? '成功率样本' : 'Success-rate sample',
      value: historyWindowRate(historyWindowStats.value.success),
      trend: `${historyWindowStats.value.success}/${historyWindowStats.value.total || 0}`,
      detail: isChinese.value
        ? '来自当前 query-history 窗口，而不是租户全量历史。'
        : 'Derived from the current query-history window rather than full-tenant history.',
      tone: historyWindowStats.value.failure > 0 ? 'warning' : 'success'
    },
    {
      key: 'failure-rate',
      label: isChinese.value ? '失败率样本' : 'Failure-rate sample',
      value: historyWindowRate(historyWindowStats.value.failure),
      trend: `${historyWindowStats.value.failure}/${historyWindowStats.value.total || 0}`,
      detail: isChinese.value
        ? '只基于当前窗口结果状态，不夸大为完整失败总量。'
        : 'Calculated only from the visible result-status window, not a global failure total.',
      tone: historyWindowStats.value.failure > 0 ? 'danger' : 'success'
    },
    {
      key: 'cache-hit-rate',
      label: isChinese.value ? '缓存命中样本' : 'Cache-hit sample',
      value: historyWindowRate(historyWindowStats.value.cacheHit),
      trend: `${historyWindowStats.value.cacheHit}/${historyWindowStats.value.total || 0}`,
      detail: isChinese.value
        ? '只消费当前历史窗口中的 cacheHit 字段。'
        : 'Uses only the cacheHit field inside the current history window.',
      tone: historyWindowStats.value.cacheHit > 0 ? 'success' : 'neutral'
    },
    {
      key: 'rewrite-hit-rate',
      label: isChinese.value ? '轻量改写样本' : 'Rewrite-hit sample',
      value: historyWindowRate(historyWindowStats.value.rewriteHit),
      trend: `${historyWindowStats.value.rewriteHit}/${historyWindowStats.value.total || 0}`,
      detail: isChinese.value
        ? '当前窗口里已应用 rewrite 的 SQL 样本占比。'
        : 'Sample ratio of rows that already applied rewrite inside the visible window.',
      tone: historyWindowStats.value.rewriteHit > 0 ? 'success' : 'neutral'
    },
    {
      key: 'acceleration-hit-rate',
      label: isChinese.value ? '加速命中样本' : 'Acceleration-hit sample',
      value: historyWindowRate(historyWindowStats.value.accelerationHit),
      trend: `${historyWindowStats.value.accelerationHit}/${historyWindowStats.value.total || 0}`,
      detail: isChinese.value
        ? '当前窗口里 accelerationApplied=true 的样本比例。'
        : 'Sample ratio of rows marked with accelerationApplied=true.',
      tone: historyWindowStats.value.accelerationHit > 0 ? 'success' : 'neutral'
    },
    {
      key: 'issue-sql',
      label: isChinese.value ? '问题 SQL' : 'Issue SQL',
      value: issueSqlCount,
      trend: issueRate,
      detail: isChinese.value
        ? '直接展示问题 SQL 与当前样本占比。'
        : 'Shows issue SQL plus its ratio inside the current sample.',
      tone: issueSqlCount > 0 ? 'warning' : 'success'
    },
    {
      key: 'important-urgent',
      label: isChinese.value ? '重要 / 紧急' : 'Important / urgent',
      value: importantUrgent.value.length,
      trend: `${importantUrgent.value.filter(item => item.urgent === true).length} urgent`,
      detail: isChinese.value
        ? '来自 important-urgent 聚合，不在前端重算优先级。'
        : 'Comes from the important-urgent aggregation without client-side reprioritization.',
      tone: importantUrgent.value.length > 0 ? 'danger' : 'success'
    },
    {
      key: 'governance-backlog',
      label: isChinese.value ? '治理 backlog' : 'Governance backlog',
      value: governanceBacklog,
      trend: `${messageStats.value?.failed || 0} failed`,
      detail: isChinese.value
        ? '基于 governance admin message stats 的 pending + failed。'
        : 'Based on governance admin message stats pending + failed.',
      tone: governanceBacklog > 0 ? 'warning' : 'success'
    },
    {
      key: 'deep-recommendations',
      label: isChinese.value ? '深度建议样本' : 'Deep recommendation sample',
      value: recommendationStats.value.total,
      trend: `${recommendationStats.value.dispatchReady} ready`,
      detail: isChinese.value
        ? '当前可见 recommendation 数量已知，但采纳率仍缺专用后端聚合。'
        : 'Visible recommendation volume is known, while adoption rate still lacks a dedicated backend aggregate.',
      tone: recommendationStats.value.total > 0 ? 'success' : 'neutral'
    },
    {
      key: 'coordination-mode',
      label: isChinese.value ? '装数协同状态' : 'Dispatch coordination',
      value: dispatchContract.value?.coordinationMode || 'PULL_ONLY',
      trend: dispatchContract.value?.externalPullRequired === true ? 'external pull' : 'repo-side',
      detail: isChinese.value
        ? '当前仓库只承诺 PULL_ONLY 协同，不伪装主动推送或真实装数。'
        : 'The repository only commits to PULL_ONLY coordination without pretending active push or live data loading exists.',
      tone: 'neutral'
    },
    {
      key: 'open-alert-sample',
      label: isChinese.value ? '告警样本' : 'Alert sample',
      value: openRiskCount.value,
      trend: `${dispatchFailures.value.length} dispatch failures`,
      detail: isChinese.value
        ? '由 failed message、dispatch failure、urgent SQL 和高风险建议派生。'
        : 'Derived from failed messages, dispatch failures, urgent SQL, and high-risk recommendations.',
      tone: openRiskCount.value > 0 ? 'danger' : 'success'
    },
    {
      key: 'access-channel-sample',
      label: isChinese.value ? '接入方式样本' : 'Access-channel sample',
      value: topAccessChannelSample.value.channel,
      trend: `${topAccessChannelSample.value.count} hits`,
      detail: isChinese.value
        ? '来自 query-history 当前窗口的 accessChannel 分布样本。'
        : 'Taken from accessChannel distribution inside the current query-history window.',
      tone: topAccessChannelSample.value.count > 0 ? 'neutral' : 'warning'
    },
    {
      key: 'route-engine-sample',
      label: isChinese.value ? '主路由引擎样本' : 'Top route-engine sample',
      value: topEngineSample.value.engine,
      trend: `${topEngineSample.value.count} hits`,
      detail: isChinese.value
        ? '基于 query-history 当前页目标引擎样本，不冒充全租户分布。'
        : 'Based on target-engine samples from the current query-history page only.',
      tone: topEngineSample.value.count > 0 ? 'neutral' : 'warning'
    }
  ]
})

const quickEntries = computed(() => [
  {
    key: 'query',
    title: isChinese.value ? '查询工作台' : 'Query workbench',
    description: isChinese.value ? '从 SQL 输入直达执行、路由与结果证据。' : 'Launch governed execution from SQL input to route and result evidence.',
    status: isChinese.value ? `${historyItems.value.length} 条近期样本` : `${historyItems.value.length} recent samples`,
    path: ROUTE_PATHS.sqlQuery
  },
  {
    key: 'parse',
    title: isChinese.value ? 'SQL解析' : 'SQL Parse',
    description: isChinese.value ? '直达单条 SQL 解析、结果与历史追溯。' : 'Jump directly into single SQL parsing, result review, and history tracing.',
    status: isChinese.value ? `${Number(overview.value?.issueSceneCount || 0)} 个问题场景` : `${Number(overview.value?.issueSceneCount || 0)} issue scenes`,
    path: {
      path: ROUTE_PATHS.acceleration,
      query: {
        workspace: 'statistics',
        analytics: 'issue'
      }
    }
  },
  {
    key: 'history',
    title: isChinese.value ? 'SQL 历史' : 'SQL history',
    description: isChinese.value ? '回查 query history、关联 trace 和取证详情。' : 'Inspect query-history rows, linked traces, and forensic detail.',
    status: isChinese.value ? `${Number(queryHistoryPage.value?.classificationSummary?.totalItems || 0)} 条窗口记录` : `${Number(queryHistoryPage.value?.classificationSummary?.totalItems || 0)} windowed rows`,
    path: ROUTE_PATHS.sqlHistory
  },
  {
    key: 'benchmark',
    title: isChinese.value ? '压测工作台' : 'Benchmark workbench',
    description: isChinese.value ? '查看对比压测、阈值结论和回归守卫结果。' : 'Review comparison reports, threshold verdicts, and regression-guard evidence.',
    status: isChinese.value ? '会话任务 + 报告证据' : 'Session tasks + report evidence',
    path: ROUTE_PATHS.benchmark
  },
  {
    key: 'system',
    title: isChinese.value ? '系统管理' : 'System management',
    description: isChinese.value ? '处理 datasource、接口、规则源与消息补偿。' : 'Operate datasources, interfaces, rule sources, and message compensation.',
    status: isChinese.value ? `${messageStats.value?.failed || 0} 条失败消息` : `${messageStats.value?.failed || 0} failed messages`,
    path: ROUTE_PATHS.system
  }
])

const healthCards = computed(() => [
  {
    key: 'risk-open',
    title: isChinese.value ? '开放风险' : 'Open risks',
    value: openRiskCount.value,
    detail: isChinese.value
      ? '由 failed message、dispatch failure、urgent SQL 与高风险建议共同构成。'
      : 'Built from failed messages, dispatch failures, urgent SQL, and high-risk recommendations.',
    tone: openRiskCount.value > 0 ? 'danger' : 'success',
    path: ROUTE_PATHS.alertCenter,
    actionLabel: isChinese.value ? '查看告警与风险' : 'Open alerts and risks'
  },
  {
    key: 'dispatch-ready',
    title: isChinese.value ? '待下发推荐' : 'Dispatch-ready recommendations',
    value: recommendationStats.value.dispatchReady,
    detail: isChinese.value
      ? '这些建议已经接近协同动作，应回到推荐中心确认 trace 与 dispatch 状态。'
      : 'These recommendations are close to handoff and should be reviewed in the recommendation center.',
    tone: recommendationStats.value.dispatchReady > 0 ? 'warning' : 'neutral',
    path: ROUTE_PATHS.recommendationCenter,
    actionLabel: isChinese.value ? '打开推荐中心' : 'Open recommendation center'
  },
  {
    key: 'history-health',
    title: isChinese.value ? '接入分布' : 'Access-channel mix',
    value: Object.keys(accessCounts.value).length,
    detail: isChinese.value
      ? '当前只展示 query-history 当前页可见的接入渠道，不伪装成全局租户统计。'
      : 'Shows only access channels visible in the current query-history page window.',
    tone: Object.keys(accessCounts.value).length > 0 ? 'neutral' : 'warning',
    path: ROUTE_PATHS.sqlHistory,
    actionLabel: isChinese.value ? '检查历史样本' : 'Inspect history window'
  },
  {
    key: 'benchmark-evidence',
    title: isChinese.value ? '压测证据边界' : 'Benchmark evidence boundary',
    value: isChinese.value ? 'Session only' : 'Session only',
    detail: isChinese.value
      ? '当前仓库没有全局 benchmark task 列表接口，首页只保留工作台入口，不伪造总通过率。'
      : 'There is no global benchmark-task list API yet, so the homepage keeps the workbench entry without inventing pass-rate totals.',
    tone: 'neutral',
    path: ROUTE_PATHS.benchmark,
    actionLabel: isChinese.value ? '进入压测工作台' : 'Open benchmark workbench'
  }
])

const issueDistributionCards = computed(() =>
  issueScenes.value.slice(0, 4).map(item => ({
    key: item.issueScene || item.sceneCode || item.issueCategory || 'UNKNOWN',
    label: item.issueScene || item.sceneCode || item.issueCategory || 'UNKNOWN',
    value: Number(item.sqlCount || item.issueSqlCount || item.affectedSqlCount || 0),
    description: isChinese.value
      ? `${Number(item.issueCount || item.affectedIssueCount || 0)} 个 issue / ${Number(item.reportCount || 0)} 个报表`
      : `${Number(item.issueCount || item.affectedIssueCount || 0)} issues / ${Number(item.reportCount || 0)} reports`
  }))
)

const nextStepItems = computed(() => {
  const items = []
  if (Number(messageStats.value?.failed || 0) > 0) {
    items.push({
      key: 'failed-messages',
      tone: 'danger',
      title: isChinese.value ? '处理失败消息补偿' : 'Repair failed-message compensation',
      description: isChinese.value
        ? `当前 failed=${messageStats.value?.failed || 0}，先进入系统管理确认 retry 影响面。`
        : `Current failed=${messageStats.value?.failed || 0}; verify retry scope from system management first.`,
      path: ROUTE_PATHS.system
    })
  }
  if (dispatchFailures.value.length > 0) {
    items.push({
      key: 'dispatch-failures',
      tone: 'warning',
      title: isChinese.value ? '复核 dispatch failure' : 'Review dispatch failures',
      description: isChinese.value
        ? `${dispatchFailures.value.length} 条 dispatch event 失败或待修复。`
        : `${dispatchFailures.value.length} dispatch events failed or need follow-up.`,
      path: ROUTE_PATHS.recommendationCenter
    })
  }
  if (recommendationStats.value.dispatchReady > 0) {
    items.push({
      key: 'dispatch-ready',
      tone: 'warning',
      title: isChinese.value ? '推进待下发推荐' : 'Advance dispatch-ready recommendations',
      description: isChinese.value
        ? `${recommendationStats.value.dispatchReady} 条 recommendation 已到 dispatch-ready。`
        : `${recommendationStats.value.dispatchReady} recommendations are already dispatch-ready.`,
      path: ROUTE_PATHS.recommendationCenter
    })
  }
  importantUrgent.value.slice(0, 2).forEach((item, index) => {
    items.push({
      key: `important-${index}`,
      tone: item.urgent ? 'danger' : 'neutral',
      title: isChinese.value ? '下钻重要 / 紧急 SQL' : 'Drill into important or urgent SQL',
      description: `${item.reportCode || '-'} · ${item.highestPriorityLevel || '-'} · ${Number(item.issueCount || 0)} issues`,
      path: {
        path: ROUTE_PATHS.acceleration,
        query: {
          workspace: 'statistics',
          analytics: 'important'
        }
      }
    })
  })
  if (items.length === 0) {
    items.push({
      key: 'no-open-items',
      tone: 'success',
      title: isChinese.value ? '当前待办为空' : 'No open next steps',
      description: isChinese.value
        ? '当前聚合证据里没有 backlog、dispatch failure 或重要紧急 SQL。'
        : 'No backlog, dispatch failures, or important-or-urgent SQL were found in the current evidence set.',
      path: ROUTE_PATHS.acceleration
    })
  }
  return items.slice(0, 5)
})

const activityItems = computed(() => {
  const historyActivities = historyItems.value.map(item => ({
    key: `history-${item.historyId}`,
    type: isChinese.value ? '查询历史' : 'Query history',
    target: item.reportCode || item.datasourceCode || item.traceId || item.historyId,
    status: item.resultStatus || 'UNKNOWN',
    time: formatTimestamp(item.submittedAt),
    path: ROUTE_PATHS.sqlHistory,
    sortValue: toEpoch(item.submittedAt)
  }))
  const dispatchActivities = dispatchEvents.value.map(item => ({
    key: `dispatch-${item.dispatchEventId || item.recommendationId || item.reportCode || `${item.status || 'UNKNOWN'}-${resolveDispatchTime(item)}`}`,
    type: isChinese.value ? '协同事件' : 'Dispatch event',
    target: item.reportCode || item.dispatchEventId || item.recommendationId || '-',
    status: item.status || 'UNKNOWN',
    time: formatTimestamp(resolveDispatchTime(item)),
    path: ROUTE_PATHS.recommendationCenter,
    sortValue: toEpoch(resolveDispatchTime(item))
  }))
  return [...historyActivities, ...dispatchActivities]
    .sort((left, right) => right.sortValue - left.sortValue)
    .slice(0, 8)
})

const loadDashboardEvidence = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const tenantId = form.tenantId
    const [
      nextOverview,
      nextIssueScenes,
      nextImportantUrgent,
      nextMessageStats,
      nextDispatchContract,
      nextQueryHistoryPage,
      nextDispatchEvents,
      nextRecommendations
    ] = await Promise.all([
      getParseStatisticsOverview(tenantId, { requestPrefix: 'frontend-dashboard-overview' }),
      getParseStatisticsByIssueScene(tenantId, { requestPrefix: 'frontend-dashboard-issue-scene' }),
      getParseStatisticsImportantUrgent(tenantId, { requestPrefix: 'frontend-dashboard-important-urgent' }),
      getGovernanceMessageStats(tenantId, { requestPrefix: 'frontend-dashboard-message-stats' }),
      getDispatchContract(tenantId, { requestPrefix: 'frontend-dashboard-dispatch-contract' }),
      getGovernanceQueryHistoryPage(
        {
          tenantId,
          historyType: 'QUERY_EXECUTION',
          pageNo: 1,
          pageSize: 8,
          sortBy: 'submittedAt',
          sortOrder: 'DESC'
        },
        {
          requestPrefix: 'frontend-dashboard-query-history'
        }
      ),
      getDispatchEvents(tenantId, '', { requestPrefix: 'frontend-dashboard-dispatch-events' }),
      getRecommendations(tenantId, { requestPrefix: 'frontend-dashboard-recommendations' })
    ])
    overview.value = nextOverview
    issueScenes.value = Array.isArray(nextIssueScenes) ? nextIssueScenes : []
    importantUrgent.value = Array.isArray(nextImportantUrgent) ? nextImportantUrgent : []
    messageStats.value = nextMessageStats
    dispatchContract.value = nextDispatchContract
    queryHistoryPage.value = nextQueryHistoryPage
    dispatchEvents.value = Array.isArray(nextDispatchEvents) ? nextDispatchEvents : []
    recommendations.value = Array.isArray(nextRecommendations) ? nextRecommendations : []
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.value = false
  }
}

const goTo = path => {
  router.push(path)
}

function resolveDispatchTime(item) {
  return item.updatedAt || item.dispatchedAt || item.occurredAt || item.createdAt || ''
}

function toEpoch(value) {
  if (!value) {
    return 0
  }
  const numeric = Number(value)
  if (!Number.isNaN(numeric) && numeric > 0) {
    return numeric
  }
  const parsed = Date.parse(String(value))
  return Number.isNaN(parsed) ? 0 : parsed
}

function formatTimestamp(value) {
  if (!value) {
    return '-'
  }
  const numeric = Number(value)
  if (!Number.isNaN(numeric) && numeric > 0) {
    return new Date(numeric).toLocaleString(isChinese.value ? 'zh-CN' : 'en-US')
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(() => {
  loadDashboardEvidence()
})
</script>

<template>
  <section class="dashboard-page" data-testid="dashboard-page">
    <section class="dashboard-hero sqlforge-panel">
      <div class="hero-copy">
        <p class="hero-eyebrow sqlforge-code-label">governance cockpit</p>
        <h1 class="hero-title">{{ isChinese ? '首页总览与主线待办' : 'Overview and operator cockpit' }}</h1>
        <p class="hero-summary">
          {{
            isChinese
              ? '首页重新聚合解析、治理、推荐与协同证据，但仍只消费当前仓库已有的已审计接口，不把局部样本夸大成全租户最终事实。'
              : 'The homepage now aggregates parse, governance, recommendation, and dispatch evidence while staying inside audited repository surfaces instead of overstating partial samples.'
          }}
        </p>
        <div class="hero-pills">
          <span v-for="pill in overviewPills" :key="pill" class="hero-pill">{{ pill }}</span>
        </div>
        <div class="hero-actions">
          <button class="pill-button pill-button-primary" @click="goTo(ROUTE_PATHS.sqlQuery)">
            {{ isChinese ? '进入查询工作台' : 'Open query workbench' }}
          </button>
          <button class="pill-button" @click="goTo(ROUTE_PATHS.acceleration)">
            {{ isChinese ? '进入 SQL解析' : 'Open SQL Parse' }}
          </button>
        </div>
      </div>

      <aside class="hero-aside sqlforge-subpanel">
        <p class="hero-aside-label sqlforge-code-label">operator focus</p>
        <div class="hero-signal">
          <span class="hero-signal-value">{{ openRiskCount }}</span>
          <div>
            <h2>{{ isChinese ? '开放风险' : 'Open risks' }}</h2>
            <p>
              {{
                isChinese
                  ? '失败消息、dispatch failure、urgent SQL 与高风险 recommendation 会在这里合并成一个操作焦点。'
                  : 'Failed messages, dispatch failures, urgent SQL, and high-risk recommendations are merged into one operator focus here.'
              }}
            </p>
          </div>
        </div>
        <label class="field-label">
          <span>{{ isChinese ? '租户' : 'Tenant' }}</span>
          <input
            v-model.trim="form.tenantId"
            class="text-input"
            data-testid="dashboard-tenant-input"
          >
        </label>
        <button
          class="pill-button pill-button-primary"
          :disabled="loading"
          data-testid="dashboard-refresh"
          @click="loadDashboardEvidence"
        >
          {{ isChinese ? '刷新总览' : 'Refresh overview' }}
        </button>
      </aside>
    </section>

    <p v-if="errorMessage" class="error-banner" data-testid="dashboard-error">{{ errorMessage }}</p>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">kpi</p>
          <h2 class="section-title">{{ isChinese ? '核心 KPI' : 'Core KPI' }}</h2>
        </div>
      </div>
      <div class="metric-grid">
        <article
          v-for="metric in metricCards"
          :key="metric.key"
          class="metric-card"
          :class="`metric-card-${metric.tone}`"
          data-testid="dashboard-kpi-card"
        >
          <p class="metric-label">{{ metric.label }}</p>
          <div class="metric-value-row">
            <strong class="metric-value">{{ metric.value }}</strong>
            <span class="metric-trend">{{ metric.trend }}</span>
          </div>
          <p class="metric-detail">{{ metric.detail }}</p>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">workflow entry</p>
          <h2 class="section-title">{{ isChinese ? '五大主入口' : 'Five primary entries' }}</h2>
        </div>
      </div>
      <div class="entry-grid">
        <article
          v-for="entry in quickEntries"
          :key="entry.key"
          class="entry-card"
          data-testid="dashboard-workbench-entry"
        >
          <p class="entry-label sqlforge-code-label">{{ entry.title }}</p>
          <h3 class="entry-title">{{ entry.title }}</h3>
          <p class="entry-description">{{ entry.description }}</p>
          <p class="entry-status">{{ entry.status }}</p>
          <button class="link-button" @click="goTo(entry.path)">
            {{ isChinese ? '进入' : 'Open' }}
          </button>
        </article>
      </div>
    </section>

    <section class="dashboard-section sqlforge-panel" data-testid="dashboard-health">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">health & risk</p>
          <h2 class="section-title">{{ isChinese ? '平台健康与风险' : 'Platform health and risk' }}</h2>
        </div>
      </div>
      <div class="health-grid">
        <article
          v-for="card in healthCards"
          :key="card.key"
          class="health-card"
          :class="`health-card-${card.tone}`"
          data-testid="dashboard-health-card"
        >
          <div class="metric-value-row">
            <p class="metric-label">{{ card.title }}</p>
            <span class="metric-trend">{{ card.value }}</span>
          </div>
          <p class="metric-detail">{{ card.detail }}</p>
          <button class="link-button" @click="goTo(card.path)">
            {{ card.actionLabel }}
          </button>
        </article>
      </div>
    </section>

    <section class="dashboard-split">
      <article class="dashboard-section sqlforge-panel" data-testid="dashboard-issue-distribution">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">issue distribution</p>
            <h2 class="section-title">{{ isChinese ? '问题分布' : 'Issue distribution' }}</h2>
          </div>
        </div>
        <div class="distribution-grid">
          <article
            v-for="item in issueDistributionCards"
            :key="item.key"
            class="distribution-card"
          >
            <span class="distribution-label">{{ item.label }}</span>
            <strong class="distribution-value">{{ item.value }}</strong>
            <p class="distribution-description">{{ item.description }}</p>
          </article>
        </div>
      </article>

      <article class="dashboard-section sqlforge-panel" data-testid="dashboard-activity-stream">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">activity stream</p>
            <h2 class="section-title">{{ isChinese ? '最近活动' : 'Recent activity' }}</h2>
          </div>
        </div>
        <div class="activity-list">
          <article
            v-for="item in activityItems"
            :key="item.key"
            class="activity-item"
            data-testid="dashboard-activity-item"
          >
            <div>
              <p class="activity-type">{{ item.type }}</p>
              <strong class="activity-target">{{ item.target }}</strong>
              <p class="activity-status">{{ item.status }}</p>
            </div>
            <div class="activity-meta">
              <span class="activity-time">{{ item.time }}</span>
              <button class="link-button" @click="goTo(item.path)">{{ isChinese ? '查看' : 'Open' }}</button>
            </div>
          </article>
        </div>
      </article>
    </section>

    <section class="dashboard-section sqlforge-panel" data-testid="dashboard-next-steps">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">next step queue</p>
          <h2 class="section-title">{{ isChinese ? '下一步建议' : 'Recommended next steps' }}</h2>
        </div>
      </div>
      <div class="todo-list">
        <article
          v-for="item in nextStepItems"
          :key="item.key"
          class="todo-item"
          :class="`todo-item-${item.tone}`"
          data-testid="dashboard-next-step-item"
        >
          <div>
            <h3 class="todo-title">{{ item.title }}</h3>
            <p class="todo-description">{{ item.description }}</p>
          </div>
          <button class="link-button" @click="goTo(item.path)">
            {{ isChinese ? '处理' : 'Open' }}
          </button>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sqlforge-panel,
.sqlforge-subpanel,
.metric-card,
.entry-card,
.distribution-card,
.health-card,
.todo-item,
.activity-item {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.sqlforge-panel {
  border-radius: 16px;
  padding: 24px;
}

.sqlforge-subpanel {
  border-radius: 14px;
  padding: 18px;
  background: rgba(41, 41, 41, 0.84);
}

.dashboard-hero,
.dashboard-split,
.metric-grid,
.entry-grid,
.distribution-grid,
.health-grid {
  display: grid;
  gap: 18px;
}

.dashboard-hero,
.dashboard-split {
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 1fr);
}

.metric-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.entry-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.health-grid {
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
}

.distribution-grid {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.hero-copy,
.hero-aside,
.todo-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-eyebrow,
.hero-aside-label,
.section-kicker,
.metric-label,
.distribution-label,
.field-label span,
.activity-type {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.hero-title {
  margin: 0;
  font-size: clamp(40px, 6vw, 68px);
  line-height: 1;
  font-weight: 400;
  color: var(--sqlforge-text-primary);
}

.hero-summary,
.distribution-description,
.todo-description,
.metric-detail,
.entry-description,
.entry-status,
.activity-status {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-pills,
.hero-actions,
.section-heading,
.metric-value-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-pill,
.metric-trend,
.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.pill-button,
.link-button,
.text-input {
  min-height: 42px;
  border-radius: 999px;
}

.pill-button,
.link-button {
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.pill-button {
  padding: 0 20px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.pill-button-primary {
  border-color: var(--sqlforge-text-primary);
}

.pill-button:hover,
.link-button:hover {
  border-color: var(--sqlforge-color-brand-border);
  color: var(--sqlforge-color-brand);
}

.link-button {
  width: fit-content;
  min-height: auto;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--sqlforge-color-link);
}

.hero-signal {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 16px;
  align-items: start;
}

.hero-signal h2,
.section-title,
.entry-title,
.todo-title {
  margin: 0;
  font-weight: 400;
  color: var(--sqlforge-text-primary);
}

.hero-signal p {
  margin: 6px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-signal-value {
  font-size: 42px;
  line-height: 1;
  color: var(--sqlforge-color-brand);
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.text-input {
  padding: 0 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.error-banner {
  margin: 0;
  padding: 14px 16px;
  border: 1px solid rgba(212, 96, 96, 0.35);
  border-radius: 14px;
  background: rgba(120, 28, 28, 0.18);
  color: #ffd6d6;
}

.dashboard-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-heading {
  justify-content: space-between;
}

.section-title {
  font-size: 28px;
  line-height: 1.1;
}

.metric-card,
.entry-card,
.distribution-card,
.health-card {
  border-radius: 14px;
  padding: 18px;
}

.metric-card-danger,
.health-card-danger,
.todo-item-danger {
  border-color: rgba(212, 96, 96, 0.35);
}

.metric-card-warning,
.health-card-warning,
.todo-item-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.metric-card-success,
.health-card-success,
.todo-item-success {
  border-color: var(--sqlforge-color-brand-border);
}

.metric-value {
  font-size: 34px;
  line-height: 1;
  font-weight: 400;
  color: var(--sqlforge-text-primary);
}

.entry-label,
.activity-time {
  color: var(--sqlforge-text-muted);
}

.entry-title {
  font-size: 22px;
  line-height: 1.2;
}

.distribution-value,
.activity-target {
  color: var(--sqlforge-text-primary);
}

.distribution-value {
  font-size: 26px;
  line-height: 1.1;
  font-weight: 400;
}

.todo-item,
.activity-item {
  border-radius: 14px;
  padding: 18px;
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.activity-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

@media (max-width: 1080px) {
  .dashboard-hero,
  .dashboard-split {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .activity-item,
  .todo-item {
    flex-direction: column;
  }

  .activity-meta {
    align-items: flex-start;
  }
}
</style>
