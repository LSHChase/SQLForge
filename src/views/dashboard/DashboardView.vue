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
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import PageHero from '../common/PageHero.vue'
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'

const { t, locale } = useI18n()
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
  t('inline.viewsDashboardDashboardView.text001')
])

const metricCards = computed(() => {
  const totalSqlCount = Number(overview.value?.totalSqlCount || 0)
  const issueSqlCount = Number(overview.value?.issueSqlCount || 0)
  const issueRate = totalSqlCount > 0 ? `${((issueSqlCount / totalSqlCount) * 100).toFixed(1)}%` : '0.0%'
  const governanceBacklog = Number(messageStats.value?.pending || 0) + Number(messageStats.value?.failed || 0)
  return [
    {
      key: 'total-sql',
      label: t('inline.viewsDashboardDashboardView.text002'),
      value: totalSqlCount,
      trend: `${Number(overview.value?.issueSceneCount || 0)} scenes`,
      detail: t('inline.viewsDashboardDashboardView.text003'),
      tone: totalSqlCount > 0 ? 'success' : 'neutral'
    },
    {
      key: 'success-rate',
      label: t('inline.viewsDashboardDashboardView.text004'),
      value: historyWindowRate(historyWindowStats.value.success),
      trend: `${historyWindowStats.value.success}/${historyWindowStats.value.total || 0}`,
      detail: t('inline.viewsDashboardDashboardView.text005'),
      tone: historyWindowStats.value.failure > 0 ? 'warning' : 'success'
    },
    {
      key: 'failure-rate',
      label: t('inline.viewsDashboardDashboardView.text006'),
      value: historyWindowRate(historyWindowStats.value.failure),
      trend: `${historyWindowStats.value.failure}/${historyWindowStats.value.total || 0}`,
      detail: t('inline.viewsDashboardDashboardView.text007'),
      tone: historyWindowStats.value.failure > 0 ? 'danger' : 'success'
    },
    {
      key: 'cache-hit-rate',
      label: t('inline.viewsDashboardDashboardView.text008'),
      value: historyWindowRate(historyWindowStats.value.cacheHit),
      trend: `${historyWindowStats.value.cacheHit}/${historyWindowStats.value.total || 0}`,
      detail: t('inline.viewsDashboardDashboardView.text009'),
      tone: historyWindowStats.value.cacheHit > 0 ? 'success' : 'neutral'
    },
    {
      key: 'rewrite-hit-rate',
      label: t('inline.viewsDashboardDashboardView.text010'),
      value: historyWindowRate(historyWindowStats.value.rewriteHit),
      trend: `${historyWindowStats.value.rewriteHit}/${historyWindowStats.value.total || 0}`,
      detail: t('inline.viewsDashboardDashboardView.text011'),
      tone: historyWindowStats.value.rewriteHit > 0 ? 'success' : 'neutral'
    },
    {
      key: 'acceleration-hit-rate',
      label: t('inline.viewsDashboardDashboardView.text012'),
      value: historyWindowRate(historyWindowStats.value.accelerationHit),
      trend: `${historyWindowStats.value.accelerationHit}/${historyWindowStats.value.total || 0}`,
      detail: t('inline.viewsDashboardDashboardView.text013'),
      tone: historyWindowStats.value.accelerationHit > 0 ? 'success' : 'neutral'
    },
    {
      key: 'issue-sql',
      label: t('inline.viewsDashboardDashboardView.text014'),
      value: issueSqlCount,
      trend: issueRate,
      detail: t('inline.viewsDashboardDashboardView.text015'),
      tone: issueSqlCount > 0 ? 'warning' : 'success'
    },
    {
      key: 'important-urgent',
      label: t('inline.viewsDashboardDashboardView.text016'),
      value: importantUrgent.value.length,
      trend: `${importantUrgent.value.filter(item => item.urgent === true).length} urgent`,
      detail: t('inline.viewsDashboardDashboardView.text017'),
      tone: importantUrgent.value.length > 0 ? 'danger' : 'success'
    },
    {
      key: 'governance-backlog',
      label: t('inline.viewsDashboardDashboardView.text018'),
      value: governanceBacklog,
      trend: `${messageStats.value?.failed || 0} failed`,
      detail: t('inline.viewsDashboardDashboardView.text019'),
      tone: governanceBacklog > 0 ? 'warning' : 'success'
    },
    {
      key: 'deep-recommendations',
      label: t('inline.viewsDashboardDashboardView.text020'),
      value: recommendationStats.value.total,
      trend: `${recommendationStats.value.dispatchReady} ready`,
      detail: t('inline.viewsDashboardDashboardView.text021'),
      tone: recommendationStats.value.total > 0 ? 'success' : 'neutral'
    },
    {
      key: 'coordination-mode',
      label: t('inline.viewsDashboardDashboardView.text022'),
      value: dispatchContract.value?.coordinationMode || 'PULL_ONLY',
      trend: dispatchContract.value?.externalPullRequired === true ? 'external pull' : 'repo-side',
      detail: t('inline.viewsDashboardDashboardView.text023'),
      tone: 'neutral'
    },
    {
      key: 'open-alert-sample',
      label: t('inline.viewsDashboardDashboardView.text024'),
      value: openRiskCount.value,
      trend: `${dispatchFailures.value.length} dispatch failures`,
      detail: t('inline.viewsDashboardDashboardView.text025'),
      tone: openRiskCount.value > 0 ? 'danger' : 'success'
    },
    {
      key: 'access-channel-sample',
      label: t('inline.viewsDashboardDashboardView.text026'),
      value: topAccessChannelSample.value.channel,
      trend: `${topAccessChannelSample.value.count} hits`,
      detail: t('inline.viewsDashboardDashboardView.text027'),
      tone: topAccessChannelSample.value.count > 0 ? 'neutral' : 'warning'
    },
    {
      key: 'route-engine-sample',
      label: t('inline.viewsDashboardDashboardView.text028'),
      value: topEngineSample.value.engine,
      trend: `${topEngineSample.value.count} hits`,
      detail: t('inline.viewsDashboardDashboardView.text029'),
      tone: topEngineSample.value.count > 0 ? 'neutral' : 'warning'
    }
  ]
})

const quickEntries = computed(() => [
  {
    key: 'query',
    title: t('inline.viewsDashboardDashboardView.text030'),
    description: t('inline.viewsDashboardDashboardView.text031'),
    status: isChinese.value ? `${historyItems.value.length} 条近期样本` : `${historyItems.value.length} recent samples`,
    path: ROUTE_PATHS.sqlQuery
  },
  {
    key: 'parse',
    title: t('inline.viewsDashboardDashboardView.text032'),
    description: t('inline.viewsDashboardDashboardView.text033'),
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
    title: t('inline.viewsDashboardDashboardView.text034'),
    description: t('inline.viewsDashboardDashboardView.text035'),
    status: isChinese.value ? `${Number(queryHistoryPage.value?.classificationSummary?.totalItems || 0)} 条窗口记录` : `${Number(queryHistoryPage.value?.classificationSummary?.totalItems || 0)} windowed rows`,
    path: ROUTE_PATHS.sqlHistory
  },
  {
    key: 'benchmark',
    title: t('inline.viewsDashboardDashboardView.text036'),
    description: t('inline.viewsDashboardDashboardView.text037'),
    status: t('inline.viewsDashboardDashboardView.text038'),
    path: ROUTE_PATHS.benchmark
  },
  {
    key: 'system',
    title: t('inline.viewsDashboardDashboardView.text039'),
    description: t('inline.viewsDashboardDashboardView.text040'),
    status: isChinese.value ? `${messageStats.value?.failed || 0} 条失败消息` : `${messageStats.value?.failed || 0} failed messages`,
    path: ROUTE_PATHS.system
  }
])

const healthCards = computed(() => [
  {
    key: 'risk-open',
    title: t('inline.viewsDashboardDashboardView.text041'),
    value: openRiskCount.value,
    detail: t('inline.viewsDashboardDashboardView.text042'),
    tone: openRiskCount.value > 0 ? 'danger' : 'success',
    path: ROUTE_PATHS.alertCenter,
    actionLabel: t('inline.viewsDashboardDashboardView.text043')
  },
  {
    key: 'dispatch-ready',
    title: t('inline.viewsDashboardDashboardView.text044'),
    value: recommendationStats.value.dispatchReady,
    detail: t('inline.viewsDashboardDashboardView.text045'),
    tone: recommendationStats.value.dispatchReady > 0 ? 'warning' : 'neutral',
    path: ROUTE_PATHS.recommendationCenter,
    actionLabel: t('inline.viewsDashboardDashboardView.text046')
  },
  {
    key: 'history-health',
    title: t('inline.viewsDashboardDashboardView.text047'),
    value: Object.keys(accessCounts.value).length,
    detail: t('inline.viewsDashboardDashboardView.text048'),
    tone: Object.keys(accessCounts.value).length > 0 ? 'neutral' : 'warning',
    path: ROUTE_PATHS.sqlHistory,
    actionLabel: t('inline.viewsDashboardDashboardView.text049')
  },
  {
    key: 'benchmark-evidence',
    title: t('inline.viewsDashboardDashboardView.text050'),
    value: t('inline.viewsDashboardDashboardView.text051'),
    detail: t('inline.viewsDashboardDashboardView.text052'),
    tone: 'neutral',
    path: ROUTE_PATHS.benchmark,
    actionLabel: t('inline.viewsDashboardDashboardView.text053')
  }
])

const dashboardEvidenceRows = computed(() => [
  {
    key: 'parse-overview',
    label: t('dashboard.evidenceRows.parseOverview.label'),
    value: Number(overview.value?.totalSqlCount || 0),
    detail: t('dashboard.evidenceRows.parseOverview.detail')
  },
  {
    key: 'query-window',
    label: t('dashboard.evidenceRows.queryWindow.label'),
    value: historyWindowStats.value.total,
    detail: t('dashboard.evidenceRows.queryWindow.detail')
  },
  {
    key: 'message-stats',
    label: t('dashboard.evidenceRows.messageStats.label'),
    value: Number(messageStats.value?.pending || 0) + Number(messageStats.value?.failed || 0),
    detail: t('dashboard.evidenceRows.messageStats.detail')
  },
  {
    key: 'dispatch-mode',
    label: t('dashboard.evidenceRows.dispatchMode.label'),
    value: dispatchContract.value?.coordinationMode || 'PULL_ONLY',
    detail: t('dashboard.evidenceRows.dispatchMode.detail')
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
      title: t('inline.viewsDashboardDashboardView.text054'),
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
      title: t('inline.viewsDashboardDashboardView.text055'),
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
      title: t('inline.viewsDashboardDashboardView.text056'),
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
      title: t('inline.viewsDashboardDashboardView.text057'),
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
      title: t('inline.viewsDashboardDashboardView.text058'),
      description: t('inline.viewsDashboardDashboardView.text059'),
      path: ROUTE_PATHS.acceleration
    })
  }
  return items.slice(0, 5)
})

const activityItems = computed(() => {
  const historyActivities = historyItems.value.map(item => ({
    key: `history-${item.historyId}`,
    type: t('inline.viewsDashboardDashboardView.text060'),
    target: item.reportCode || item.datasourceCode || item.traceId || item.historyId,
    status: item.resultStatus || 'UNKNOWN',
    time: formatTimestamp(item.submittedAt),
    path: ROUTE_PATHS.sqlHistory,
    sortValue: toEpoch(item.submittedAt)
  }))
  const dispatchActivities = dispatchEvents.value.map(item => ({
    key: `dispatch-${item.dispatchEventId || item.recommendationId || item.reportCode || `${item.status || 'UNKNOWN'}-${resolveDispatchTime(item)}`}`,
    type: t('inline.viewsDashboardDashboardView.text061'),
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
    <PageHero
      :eyebrow="t('common.platformTagline')"
      :title="t('dashboard.operatorHeroTitle')"
      :summary="t('dashboard.operatorHeroSummary')"
      :pills="overviewPills"
    >
      <template #actions>
        <div class="hero-actions">
          <button class="pill-button pill-button-primary" @click="goTo(ROUTE_PATHS.sqlQuery)">
            {{ t('dashboard.actions.openQueryWorkbench') }}
          </button>
          <button class="pill-button" @click="goTo(ROUTE_PATHS.acceleration)">
            {{ t('dashboard.actions.openSqlParse') }}
          </button>
        </div>
      </template>

      <template #aside>
        <ToolbarShell class="dashboard-focus-shell" :eyebrow="t('dashboard.operatorFocusEyebrow')" density="compact">
          <div class="hero-signal">
            <span class="hero-signal-value">{{ openRiskCount }}</span>
            <div>
              <h2>{{ t('dashboard.openRisksTitle') }}</h2>
              <p>{{ t('dashboard.openRisksSummary') }}</p>
            </div>
          </div>
          <label class="field-label">
            <span>{{ t('common.currentTenant') }}</span>
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
            {{ t('dashboard.actions.refreshOverview') }}
          </button>
        </ToolbarShell>
      </template>
    </PageHero>

    <p v-if="errorMessage" class="error-banner" data-testid="dashboard-error">{{ errorMessage }}</p>

    <section class="dashboard-section">
      <SectionHeader eyebrow="kpi" :title="t('dashboard.coreKpiTitle')" />
      <div class="metric-grid">
        <MetricCard
          v-for="metric in metricCards"
          :key="metric.key"
          :label="metric.label"
          :value="metric.value"
          :trend="metric.trend"
          :detail="metric.detail"
          :tone="metric.tone"
          test-id="dashboard-kpi-card"
        />
      </div>
    </section>

    <section class="dashboard-section">
      <SectionHeader eyebrow="workflow entry" :title="t('dashboard.primaryEntriesTitle')" />
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
            {{ t('inline.viewsDashboardDashboardView.text062') }}
          </button>
        </article>
      </div>
    </section>

    <EvidencePanel
      class="dashboard-section"
      eyebrow="health & risk"
      :title="t('dashboard.platformHealthRiskTitle')"
      test-id="dashboard-health"
    >
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
    </EvidencePanel>

    <EvidencePanel
      class="dashboard-section"
      :eyebrow="t('dashboard.evidenceEyebrow')"
      :title="t('dashboard.evidenceBoundaryTitle')"
      :summary="t('dashboard.evidenceBoundarySummary')"
      test-id="dashboard-static-evidence"
    >
      <div class="evidence-boundary-grid">
        <article
          v-for="row in dashboardEvidenceRows"
          :key="row.key"
          class="evidence-boundary-item"
        >
          <span class="evidence-boundary-label sqlforge-code-label">{{ row.label }}</span>
          <strong>{{ row.value }}</strong>
          <p>{{ row.detail }}</p>
        </article>
      </div>
    </EvidencePanel>

    <section class="dashboard-split">
      <EvidencePanel
        as="article"
        class="dashboard-section"
        eyebrow="issue distribution"
        :title="t('dashboard.issueDistributionTitle')"
        test-id="dashboard-issue-distribution"
      >
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
      </EvidencePanel>

      <EvidencePanel
        as="article"
        class="dashboard-section"
        eyebrow="activity stream"
        :title="t('dashboard.recentActivityTitle')"
        test-id="dashboard-activity-stream"
      >
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
              <button class="link-button" @click="goTo(item.path)">{{ t('inline.viewsDashboardDashboardView.text063') }}</button>
            </div>
          </article>
        </div>
      </EvidencePanel>
    </section>

    <EvidencePanel
      class="dashboard-section"
      eyebrow="next step queue"
      :title="t('dashboard.nextStepsTitle')"
      test-id="dashboard-next-steps"
    >
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
            {{ t('inline.viewsDashboardDashboardView.text064') }}
          </button>
        </article>
      </div>
    </EvidencePanel>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.entry-card,
.distribution-card,
.health-card,
.todo-item,
.activity-item {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.dashboard-split,
.metric-grid,
.entry-grid,
.distribution-grid,
.health-grid,
.evidence-boundary-grid {
  display: grid;
  gap: 18px;
}

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

.evidence-boundary-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.todo-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-label,
.distribution-label,
.field-label span,
.activity-type {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

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

.hero-actions,
.metric-value-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.dashboard-focus-shell :deep(.toolbar-shell-body) {
  flex-direction: column;
  align-items: stretch;
}

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

.entry-card,
.distribution-card,
.health-card,
.evidence-boundary-item {
  border-radius: 14px;
  padding: 18px;
}

.health-card-danger,
.todo-item-danger {
  border-color: rgba(212, 96, 96, 0.35);
}

.health-card-warning,
.todo-item-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.health-card-success,
.todo-item-success {
  border-color: var(--sqlforge-color-brand-border);
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

.evidence-boundary-item {
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.evidence-boundary-item strong {
  display: block;
  margin-top: 10px;
  color: var(--sqlforge-text-primary);
  font-size: 26px;
  font-weight: 400;
  line-height: 1.1;
}

.evidence-boundary-item p {
  margin: 10px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.evidence-boundary-label {
  color: var(--sqlforge-text-muted);
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
