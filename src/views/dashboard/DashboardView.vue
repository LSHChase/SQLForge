<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { useTenantStore } from '../../stores'
import {
  getDispatchContract,
  formatRuntimeError,
  getGovernanceMessageStats,
  getGovernanceQueryHistoryPage,
  getParseStatisticsByIssueScene,
  getParseStatisticsImportantUrgent,
  getParseStatisticsOverview,
  getRecommendations,
  getSqlParseHistoryPage,
  getSqlRewriteRecords
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
const recommendations = ref([])
const parseHistoryPage = ref(null)
const rewriteRecords = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')
const historyItems = computed(() => queryHistoryPage.value?.items || [])
const parseHistoryItems = computed(() => parseHistoryPage.value?.items || [])
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

const parseHistoryStats = computed(() => {
  const stats = {
    total: parseHistoryItems.value.length,
    failed: 0,
    partial: 0,
    completed: 0
  }
  parseHistoryItems.value.forEach(item => {
    const status = String(item.status || item.resultStatus || item.stage || '').toUpperCase()
    if (['FAILED', 'FAIL', 'ERROR'].includes(status)) {
      stats.failed += 1
    } else if (['PARTIAL', 'PARTIAL_SUCCESS'].includes(status)) {
      stats.partial += 1
    } else if (['COMPLETED', 'SUCCESS', 'SUCCEEDED', 'DONE'].includes(status)) {
      stats.completed += 1
    }
  })
  return stats
})

const rewriteStats = computed(() => {
  const stats = {
    total: rewriteRecords.value.length,
    attention: 0,
    paused: 0,
    validationRisk: 0
  }
  rewriteRecords.value.forEach(item => {
    const validationStatus = String(item.validationStatus || item.rewriteValidationStatus || '').toUpperCase()
    const publishStatus = String(item.publishStatus || item.runtimeRewriteStatus || '').toUpperCase()
    const reviewStatus = String(item.reviewStatus || '').toUpperCase()
    const paused = item.autoApplyPaused === true || ['PAUSED', 'SUSPENDED'].includes(publishStatus)
    const validationRisk = ['FAILED', 'FAIL', 'DIVERGED', 'SQL_REWRITE_RESULT_DIVERGENCE'].includes(validationStatus)
    const needsReview = ['PENDING', 'REVIEWING', 'REJECTED'].includes(reviewStatus)
    if (paused) {
      stats.paused += 1
    }
    if (validationRisk) {
      stats.validationRisk += 1
    }
    if (paused || validationRisk || needsReview) {
      stats.attention += 1
    }
  })
  return stats
})

const coreAttentionCount = computed(() => {
  const urgent = importantUrgent.value.filter(item => item.urgent === true).length
  return historyWindowStats.value.failure + parseHistoryStats.value.failed + urgent + recommendationStats.value.highRisk + rewriteStats.value.attention
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
  return [
    {
      key: 'query-window',
      label: t('inline.viewsDashboardDashboardView.text002'),
      value: historyWindowStats.value.total,
      trend: historyWindowRate(historyWindowStats.value.success),
      detail: t('inline.viewsDashboardDashboardView.text003'),
      tone: historyWindowStats.value.failure > 0 ? 'warning' : 'success'
    },
    {
      key: 'parse-overview',
      label: t('inline.viewsDashboardDashboardView.text004'),
      value: totalSqlCount,
      trend: `${Number(overview.value?.issueSceneCount || 0)} scenes`,
      detail: t('inline.viewsDashboardDashboardView.text005'),
      tone: totalSqlCount > 0 ? 'success' : 'neutral'
    },
    {
      key: 'parse-history-window',
      label: t('inline.viewsDashboardDashboardView.text006'),
      value: parseHistoryStats.value.total,
      trend: `${parseHistoryStats.value.failed} failed`,
      detail: t('inline.viewsDashboardDashboardView.text007'),
      tone: parseHistoryStats.value.failed > 0 ? 'warning' : 'neutral'
    },
    {
      key: 'important-urgent',
      label: t('inline.viewsDashboardDashboardView.text008'),
      value: importantUrgent.value.length,
      trend: `${importantUrgent.value.filter(item => item.urgent === true).length} urgent`,
      detail: t('inline.viewsDashboardDashboardView.text009'),
      tone: importantUrgent.value.length > 0 ? 'danger' : 'success'
    },
    {
      key: 'recommendation-results',
      label: t('inline.viewsDashboardDashboardView.text010'),
      value: recommendationStats.value.total,
      trend: `${recommendationStats.value.dispatchReady} ready`,
      detail: t('inline.viewsDashboardDashboardView.text011'),
      tone: recommendationStats.value.total > 0 ? 'success' : 'neutral'
    },
    {
      key: 'rewrite-records',
      label: t('inline.viewsDashboardDashboardView.text012'),
      value: rewriteStats.value.total,
      trend: `${rewriteStats.value.attention} attention`,
      detail: t('inline.viewsDashboardDashboardView.text013'),
      tone: rewriteStats.value.attention > 0 ? 'warning' : 'neutral'
    }
  ]
})

const quickEntries = computed(() => [
  {
    key: 'query',
    step: '01',
    title: t('inline.viewsDashboardDashboardView.text014'),
    description: t('inline.viewsDashboardDashboardView.text015'),
    status: isChinese.value ? `${historyItems.value.length} 条近期样本` : `${historyItems.value.length} recent samples`,
    path: ROUTE_PATHS.sqlQuery
  },
  {
    key: 'history',
    step: '02',
    title: t('inline.viewsDashboardDashboardView.text016'),
    description: t('inline.viewsDashboardDashboardView.text017'),
    status: isChinese.value ? `${Number(queryHistoryPage.value?.classificationSummary?.totalItems || 0)} 条窗口记录` : `${Number(queryHistoryPage.value?.classificationSummary?.totalItems || 0)} windowed rows`,
    path: ROUTE_PATHS.sqlHistory
  },
  {
    key: 'parse',
    step: '03',
    title: t('inline.viewsDashboardDashboardView.text018'),
    description: t('inline.viewsDashboardDashboardView.text019'),
    status: isChinese.value ? `${Number(overview.value?.issueSceneCount || 0)} 个问题场景` : `${Number(overview.value?.issueSceneCount || 0)} issue scenes`,
    path: ROUTE_PATHS.acceleration
  },
  {
    key: 'parse-history',
    step: '04',
    title: t('inline.viewsDashboardDashboardView.text020'),
    description: t('inline.viewsDashboardDashboardView.text021'),
    status: isChinese.value ? `${parseHistoryStats.value.total} 条解析样本` : `${parseHistoryStats.value.total} parse samples`,
    path: ROUTE_PATHS.parseRecord
  },
  {
    key: 'recommendations',
    step: '05',
    title: t('inline.viewsDashboardDashboardView.text022'),
    description: t('inline.viewsDashboardDashboardView.text023'),
    status: isChinese.value ? `${recommendationStats.value.total} 条推荐结果` : `${recommendationStats.value.total} recommendation results`,
    path: ROUTE_PATHS.recommendationCenter
  },
  {
    key: 'rewrite',
    step: '06',
    title: t('inline.viewsDashboardDashboardView.text024'),
    description: t('inline.viewsDashboardDashboardView.text025'),
    status: isChinese.value ? `${rewriteStats.value.total} 条改写记录样本` : `${rewriteStats.value.total} rewrite-record samples`,
    path: ROUTE_PATHS.recommendationCenter
  }
])

const auxiliarySignals = computed(() => [
  {
    key: 'coordination-mode',
    title: t('inline.viewsDashboardDashboardView.text026'),
    value: dispatchContract.value?.coordinationMode || 'PULL_ONLY',
    detail: t('inline.viewsDashboardDashboardView.text027'),
    tone: 'neutral',
    path: ROUTE_PATHS.recommendationCenter,
    actionLabel: t('inline.viewsDashboardDashboardView.text028')
  },
  {
    key: 'access-channel-sample',
    title: t('inline.viewsDashboardDashboardView.text029'),
    value: topAccessChannelSample.value.channel,
    detail: isChinese.value
      ? `${topAccessChannelSample.value.count} 条 query-history 窗口命中`
      : `${topAccessChannelSample.value.count} query-history window hits`,
    tone: topAccessChannelSample.value.count > 0 ? 'neutral' : 'warning',
    path: ROUTE_PATHS.sqlHistory,
    actionLabel: t('inline.viewsDashboardDashboardView.text030')
  },
  {
    key: 'route-engine-sample',
    title: t('inline.viewsDashboardDashboardView.text031'),
    value: topEngineSample.value.engine,
    detail: isChinese.value
      ? `${topEngineSample.value.count} 条 query-history 窗口命中`
      : `${topEngineSample.value.count} query-history window hits`,
    tone: topEngineSample.value.count > 0 ? 'neutral' : 'warning',
    path: ROUTE_PATHS.sqlHistory,
    actionLabel: t('inline.viewsDashboardDashboardView.text030')
  },
  {
    key: 'governance-message-boundary',
    title: t('inline.viewsDashboardDashboardView.text032'),
    value: Number(messageStats.value?.pending || 0) + Number(messageStats.value?.failed || 0),
    detail: t('inline.viewsDashboardDashboardView.text033'),
    tone: Number(messageStats.value?.failed || 0) > 0 ? 'warning' : 'neutral',
    path: ROUTE_PATHS.system,
    actionLabel: t('inline.viewsDashboardDashboardView.text034')
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
    key: 'rewrite-records',
    label: t('dashboard.evidenceRows.rewriteRecords.label'),
    value: rewriteStats.value.total,
    detail: t('dashboard.evidenceRows.rewriteRecords.detail')
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
  if (historyWindowStats.value.failure > 0) {
    items.push({
      key: 'failed-query-window',
      tone: 'danger',
      title: t('inline.viewsDashboardDashboardView.text035'),
      description: isChinese.value
        ? `当前查询窗口 failed=${historyWindowStats.value.failure}，先从 SQL 历史查询回看原 SQL、绑定后 SQL 与关联解析。`
        : `Current query window has failed=${historyWindowStats.value.failure}; inspect SQL history, bound SQL, and parse refs first.`,
      path: ROUTE_PATHS.sqlHistory
    })
  }
  if (parseHistoryStats.value.failed > 0) {
    items.push({
      key: 'failed-parse-history',
      tone: 'warning',
      title: t('inline.viewsDashboardDashboardView.text036'),
      description: isChinese.value
        ? `${parseHistoryStats.value.failed} 条解析历史样本失败，优先查看失败原因、问题场景与逻辑对象。`
        : `${parseHistoryStats.value.failed} parse-history samples failed; inspect failure reason, issue scene, and logical objects.`,
      path: ROUTE_PATHS.parseRecord
    })
  }
  if (recommendationStats.value.total > 0) {
    items.push({
      key: 'recommendation-results',
      tone: recommendationStats.value.highRisk > 0 ? 'warning' : 'neutral',
      title: t('inline.viewsDashboardDashboardView.text037'),
      description: isChinese.value
        ? `${recommendationStats.value.total} 条推荐结果可复核收益、风险、SQL diff 与规则链。`
        : `${recommendationStats.value.total} recommendation results can be reviewed for benefit, risk, SQL diff, and rule chain.`,
      path: ROUTE_PATHS.recommendationCenter
    })
  }
  if (rewriteStats.value.attention > 0) {
    items.push({
      key: 'rewrite-record-attention',
      tone: 'warning',
      title: t('inline.viewsDashboardDashboardView.text038'),
      description: isChinese.value
        ? `${rewriteStats.value.attention} 条改写记录样本需要关注 review、publish、paused 或 validation 状态。`
        : `${rewriteStats.value.attention} rewrite-record samples need review, publish, paused, or validation attention.`,
      path: ROUTE_PATHS.recommendationCenter
    })
  }
  importantUrgent.value.slice(0, 2).forEach((item, index) => {
    items.push({
      key: `important-${index}`,
      tone: item.urgent ? 'danger' : 'neutral',
      title: t('inline.viewsDashboardDashboardView.text039'),
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
      title: t('inline.viewsDashboardDashboardView.text040'),
      description: t('inline.viewsDashboardDashboardView.text041'),
      path: ROUTE_PATHS.sqlQuery
    })
  }
  return items.slice(0, 5)
})

const activityItems = computed(() => {
  const historyActivities = historyItems.value.map(item => ({
    key: `history-${item.historyId}`,
    type: t('inline.viewsDashboardDashboardView.text042'),
    target: item.reportCode || item.datasourceCode || item.traceId || item.historyId,
    status: item.resultStatus || 'UNKNOWN',
    time: formatTimestamp(item.submittedAt),
    path: ROUTE_PATHS.sqlHistory,
    sortValue: toEpoch(item.submittedAt)
  }))
  const parseActivities = parseHistoryItems.value.map(item => ({
    key: `parse-${item.parseHistoryId || item.parseTaskId || item.historyId || item.sqlFingerprint}`,
    type: t('inline.viewsDashboardDashboardView.text043'),
    target: item.reportCode || item.parseTaskId || item.parseHistoryId || item.sqlFingerprint || '-',
    status: item.status || item.resultStatus || item.stage || 'UNKNOWN',
    time: formatTimestamp(item.submittedAt || item.completedAt || item.createdAt),
    path: ROUTE_PATHS.parseRecord,
    sortValue: toEpoch(item.submittedAt || item.completedAt || item.createdAt)
  }))
  const recommendationActivities = recommendations.value.slice(0, 4).map(item => ({
    key: `recommendation-${item.recommendationId || item.sqlFingerprint || item.summary}`,
    type: t('inline.viewsDashboardDashboardView.text044'),
    target: item.recommendationId || item.reportCode || item.sqlFingerprint || '-',
    status: item.status || item.riskLevel || 'UNKNOWN',
    time: formatTimestamp(item.updatedAt || item.createdAt || item.generatedAt),
    path: ROUTE_PATHS.recommendationCenter,
    sortValue: toEpoch(item.updatedAt || item.createdAt || item.generatedAt)
  }))
  const rewriteActivities = rewriteRecords.value.slice(0, 4).map(item => ({
    key: `rewrite-${item.rewriteRecordId || item.recommendationId || item.sqlFingerprint}`,
    type: t('inline.viewsDashboardDashboardView.text045'),
    target: item.rewriteRecordId || item.recommendationId || item.sqlFingerprint || '-',
    status: item.validationStatus || item.publishStatus || item.reviewStatus || 'UNKNOWN',
    time: formatTimestamp(item.updatedAt || item.lastComparedAt || item.createdAt),
    path: ROUTE_PATHS.recommendationCenter,
    sortValue: toEpoch(item.updatedAt || item.lastComparedAt || item.createdAt)
  }))
  return [...historyActivities, ...parseActivities, ...recommendationActivities, ...rewriteActivities]
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
      nextRecommendations,
      nextParseHistoryPage,
      nextRewriteRecords
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
      getRecommendations(tenantId, { requestPrefix: 'frontend-dashboard-recommendations' }),
      getSqlParseHistoryPage(
        {
          tenantId,
          requestTenantId: tenantId,
          pageNo: 1,
          pageSize: 6,
          sortBy: 'submittedAt',
          sortOrder: 'DESC'
        },
        {
          requestPrefix: 'frontend-dashboard-parse-history'
        }
      ),
      getSqlRewriteRecords(tenantId, {}, { requestPrefix: 'frontend-dashboard-rewrite-records' })
    ])
    overview.value = nextOverview
    issueScenes.value = Array.isArray(nextIssueScenes) ? nextIssueScenes : []
    importantUrgent.value = Array.isArray(nextImportantUrgent) ? nextImportantUrgent : []
    messageStats.value = nextMessageStats
    dispatchContract.value = nextDispatchContract
    queryHistoryPage.value = nextQueryHistoryPage
    recommendations.value = Array.isArray(nextRecommendations) ? nextRecommendations : []
    parseHistoryPage.value = nextParseHistoryPage
    rewriteRecords.value = Array.isArray(nextRewriteRecords) ? nextRewriteRecords : []
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.value = false
  }
}

const goTo = path => {
  router.push(path)
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
            <span class="hero-signal-value">{{ coreAttentionCount }}</span>
            <div>
              <h2>{{ t('dashboard.coreAttentionTitle') }}</h2>
              <p>{{ t('dashboard.coreAttentionSummary') }}</p>
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
          <p class="entry-label sqlforge-code-label">{{ entry.step }}</p>
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
      eyebrow="boundary evidence"
      :title="t('dashboard.auxiliarySignalsTitle')"
      :summary="t('dashboard.auxiliarySignalsSummary')"
      test-id="dashboard-health"
    >
      <div class="health-grid">
        <article
          v-for="card in auxiliarySignals"
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
        <div v-if="issueDistributionCards.length" class="distribution-grid">
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
        <p v-else class="empty-state" data-testid="dashboard-issue-distribution-empty">
          {{ t('inline.viewsDashboardDashboardView.text048') }}
        </p>
      </EvidencePanel>

      <EvidencePanel
        as="article"
        class="dashboard-section"
        eyebrow="activity stream"
        :title="t('dashboard.recentActivityTitle')"
        test-id="dashboard-activity-stream"
      >
        <div v-if="activityItems.length" class="activity-list">
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
              <button class="link-button" @click="goTo(item.path)">{{ t('inline.viewsDashboardDashboardView.text046') }}</button>
            </div>
          </article>
        </div>
        <p v-else class="empty-state" data-testid="dashboard-activity-empty">
          {{ t('inline.viewsDashboardDashboardView.text049') }}
        </p>
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
            {{ t('inline.viewsDashboardDashboardView.text047') }}
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
.activity-status,
.empty-state {
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
