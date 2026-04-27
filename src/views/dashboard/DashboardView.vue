<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { useTenantStore } from '../../stores'
import {
  formatRuntimeError,
  getDispatchEvents,
  getGovernanceMessageStats,
  getGovernanceQueryHistoryPage,
  getParseStatisticsByIssueScene,
  getParseStatisticsImportantUrgent,
  getParseStatisticsOverview
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
const queryHistoryPage = ref(null)
const dispatchEvents = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')
const historyItems = computed(() => queryHistoryPage.value?.items || [])
const accessCounts = computed(() => queryHistoryPage.value?.classificationSummary?.accessChannelCounts || {})
const dispatchFailures = computed(() =>
  dispatchEvents.value.filter(item => String(item.status || '').toUpperCase() === 'FAILED')
)
const openRiskCount = computed(() => {
  const failed = Number(messageStats.value?.failed || 0)
  const urgent = importantUrgent.value.filter(item => item.urgent === true).length
  return failed + urgent + dispatchFailures.value.length
})

const overviewPills = computed(() => [
  form.tenantId,
  tenantStore.defaultEngine || 'HETU',
  isChinese.value ? 'audited sources only' : 'audited sources only'
])

const metricCards = computed(() => {
  const totalSqlCount = Number(overview.value?.totalSqlCount || 0)
  const issueSqlCount = Number(overview.value?.issueSqlCount || 0)
  const issueRate = totalSqlCount > 0 ? `${((issueSqlCount / totalSqlCount) * 100).toFixed(1)}%` : '0.0%'
  const backlogCount = Number(messageStats.value?.pending || 0) + Number(messageStats.value?.failed || 0)
  const recentTotal = Number(queryHistoryPage.value?.classificationSummary?.totalItems || historyItems.value.length || 0)
  return [
    {
      key: 'issue-sql',
      label: isChinese.value ? '问题 SQL' : 'Issue SQL',
      value: issueSqlCount,
      trend: issueRate,
      detail: isChinese.value
        ? '来自 parse-statistics overview 的问题 SQL 占比。'
        : 'Issue SQL ratio from parse-statistics overview.',
      tone: issueSqlCount > 0 ? 'warning' : 'success'
    },
    {
      key: 'priority-alerts',
      label: isChinese.value ? '重要 / 紧急' : 'Important / urgent',
      value: importantUrgent.value.length,
      trend: `${importantUrgent.value.filter(item => item.urgent === true).length} urgent`,
      detail: isChinese.value
        ? '直接来自 important-urgent 聚合，不在前端重算优先级。'
        : 'Comes directly from the important-urgent aggregation without client-side reprioritization.',
      tone: importantUrgent.value.length > 0 ? 'danger' : 'success'
    },
    {
      key: 'governance-backlog',
      label: isChinese.value ? '治理 backlog' : 'Governance backlog',
      value: backlogCount,
      trend: `${messageStats.value?.failed || 0} failed`,
      detail: isChinese.value
        ? '基于 governance admin message stats 的 pending + failed。'
        : 'Based on governance admin message stats pending + failed.',
      tone: backlogCount > 0 ? 'warning' : 'success'
    },
    {
      key: 'recent-audit',
      label: isChinese.value ? '最近审计样本' : 'Recent audit samples',
      value: recentTotal,
      trend: `${Object.keys(accessCounts.value).length} channels`,
      detail: isChinese.value
        ? '来自 query-history 当前页分类摘要，不夸大为全量历史。'
        : 'Taken from the current query-history page summary rather than overstated as full history.',
      tone: recentTotal > 0 ? 'neutral' : 'warning'
    }
  ]
})

const quickEntries = computed(() => [
  {
    title: isChinese.value ? '查询工作台' : 'Query workbench',
    description: isChinese.value ? '从 SQL 输入直达治理执行与结果面。' : 'Launch governed execution from SQL input to result evidence.',
    status: isChinese.value
      ? `最近样本 ${historyItems.value.length} 条`
      : `${historyItems.value.length} recent history samples`,
    path: ROUTE_PATHS.sqlQuery
  },
  {
    title: isChinese.value ? '解析统计' : 'Parse statistics',
    description: isChinese.value ? '查看问题 SQL、issue scene 和优先级矩阵。' : 'Inspect issue SQL, issue scenes, and priority matrices.',
    status: isChinese.value
      ? `${issueScenes.value.length} 个 issue scene`
      : `${issueScenes.value.length} issue scenes`,
    path: ROUTE_PATHS.parseStatisticsCenter
  },
  {
    title: isChinese.value ? '告警中心' : 'Alert center',
    description: isChinese.value ? '查看 derived alert、ACK 和 simulated notify。' : 'Inspect derived alerts, ACK state, and simulated notify flows.',
    status: isChinese.value
      ? `${openRiskCount.value} 个开放风险`
      : `${openRiskCount.value} open risks`,
    path: ROUTE_PATHS.alertCenter
  },
  {
    title: isChinese.value ? '系统管理' : 'System management',
    description: isChinese.value ? '查看 datasource、report-interface、rule-source 和 dispatch policy。' : 'Review datasource, report-interface, rule-source, and dispatch policy baselines.',
    status: isChinese.value
      ? `${messageStats.value?.failed || 0} failed message`
      : `${messageStats.value?.failed || 0} failed messages`,
    path: ROUTE_PATHS.system
  }
])

const issueDistributionCards = computed(() =>
  issueScenes.value.slice(0, 4).map(item => ({
    key: item.issueScene || item.sceneCode || item.issueCategory || 'UNKNOWN',
    label: item.issueScene || item.sceneCode || item.issueCategory || 'UNKNOWN',
    value: Number(item.sqlCount || item.issueSqlCount || 0),
    description: isChinese.value
      ? `${Number(item.issueCount || 0)} 个 issue / ${Number(item.reportCount || 0)} 个报表`
      : `${Number(item.issueCount || 0)} issues / ${Number(item.reportCount || 0)} reports`
  }))
)

const accessDistributionCards = computed(() =>
  Object.entries(accessCounts.value)
    .slice(0, 5)
    .map(([channel, count]) => ({
      key: channel,
      label: channel,
      value: Number(count || 0),
      description: isChinese.value
        ? '来自 query-history accessChannelCounts'
        : 'Derived from query-history accessChannelCounts'
    }))
)

const todoItems = computed(() => {
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
  importantUrgent.value.slice(0, 2).forEach((item, index) => {
    items.push({
      key: `important-${index}`,
      tone: item.urgent ? 'danger' : 'neutral',
      title: isChinese.value ? '下钻重要 / 紧急 SQL' : 'Drill into important or urgent SQL',
      description: `${item.reportCode || '-'} · ${item.highestPriorityLevel || '-'} · ${Number(item.issueCount || 0)} issues`,
      path: ROUTE_PATHS.parseStatisticsCenter
    })
  })
  if (items.length === 0) {
    items.push({
      key: 'no-open-items',
      tone: 'success',
      title: isChinese.value ? '当前待办为空' : 'No open todo items',
      description: isChinese.value
        ? '当前聚合证据里没有 backlog、dispatch failure 或重要紧急 SQL。'
        : 'No backlog, dispatch failures, or important-or-urgent SQL were found in the current evidence set.',
      path: ROUTE_PATHS.parseStatisticsCenter
    })
  }
  return items.slice(0, 4)
})

const activityItems = computed(() =>
  historyItems.value.slice(0, 6).map(item => ({
    key: item.historyId,
    type: item.accessChannel || 'UNKNOWN',
    target: item.reportCode || item.datasourceCode || item.traceId || item.historyId,
    status: item.resultStatus || 'UNKNOWN',
    time: formatTimestamp(item.submittedAt),
    engine: item.targetEngine || '-'
  }))
)

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
      nextQueryHistoryPage,
      nextDispatchEvents
    ] = await Promise.all([
      getParseStatisticsOverview(tenantId, { requestPrefix: 'frontend-dashboard-overview' }),
      getParseStatisticsByIssueScene(tenantId, { requestPrefix: 'frontend-dashboard-issue-scene' }),
      getParseStatisticsImportantUrgent(tenantId, { requestPrefix: 'frontend-dashboard-important-urgent' }),
      getGovernanceMessageStats(tenantId, { requestPrefix: 'frontend-dashboard-message-stats' }),
      getGovernanceQueryHistoryPage(
        {
          tenantId,
          pageNo: 1,
          pageSize: 8,
          sortBy: 'submittedAt',
          sortOrder: 'DESC'
        },
        {
          requestPrefix: 'frontend-dashboard-query-history'
        }
      ),
      getDispatchEvents(tenantId, '', { requestPrefix: 'frontend-dashboard-dispatch-events' })
    ])
    overview.value = nextOverview
    issueScenes.value = Array.isArray(nextIssueScenes) ? nextIssueScenes : []
    importantUrgent.value = Array.isArray(nextImportantUrgent) ? nextImportantUrgent : []
    messageStats.value = nextMessageStats
    queryHistoryPage.value = nextQueryHistoryPage
    dispatchEvents.value = Array.isArray(nextDispatchEvents) ? nextDispatchEvents : []
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.value = false
  }
}

const goTo = path => {
  router.push(path)
}

const formatTimestamp = value => {
  if (!value) {
    return '-'
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
        <h1 class="hero-title">{{ isChinese ? '首页总览与待办态势' : 'Overview and action cockpit' }}</h1>
        <p class="hero-summary">
          {{
            isChinese
              ? '首页 KPI、分布和待办只聚合已审计查询面：parse-statistics、governance history、dispatch events 与 message stats。'
              : 'The dashboard only aggregates audited surfaces for KPI, distribution, and todo blocks: parse-statistics, governance history, dispatch events, and message stats.'
          }}
        </p>
        <div class="hero-pills">
          <span
            v-for="pill in overviewPills"
            :key="pill"
            class="hero-pill"
          >
            {{ pill }}
          </span>
        </div>
        <div class="hero-actions">
          <button class="pill-button pill-button-primary" @click="goTo(ROUTE_PATHS.sqlQuery)">
            {{ isChinese ? '进入查询工作台' : 'Open query workbench' }}
          </button>
          <button class="pill-button" @click="goTo(ROUTE_PATHS.alertCenter)">
            {{ isChinese ? '打开告警中心' : 'Open alert center' }}
          </button>
        </div>
      </div>

      <aside class="hero-aside sqlforge-subpanel">
        <p class="hero-aside-label sqlforge-code-label">risk focus</p>
        <div class="hero-signal">
          <span class="hero-signal-value">{{ openRiskCount }}</span>
          <div>
            <h2>{{ isChinese ? '开放风险' : 'Open risks' }}</h2>
            <p>
              {{
                isChinese
                  ? '由 failed message、dispatch failure 和 urgent SQL 样本共同构成。'
                  : 'Derived from failed messages, dispatch failures, and urgent SQL samples.'
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
          <h2 class="section-title">{{ isChinese ? '工作台入口' : 'Workbench entry' }}</h2>
        </div>
      </div>
      <div class="entry-grid">
        <article
          v-for="entry in quickEntries"
          :key="entry.path"
          class="entry-card"
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

      <article class="dashboard-section sqlforge-panel" data-testid="dashboard-access-distribution">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">access distribution</p>
            <h2 class="section-title">{{ isChinese ? '接入分布' : 'Access distribution' }}</h2>
          </div>
        </div>
        <div class="distribution-grid">
          <article
            v-for="item in accessDistributionCards"
            :key="item.key"
            class="distribution-card"
          >
            <span class="distribution-label">{{ item.label }}</span>
            <strong class="distribution-value">{{ item.value }}</strong>
            <p class="distribution-description">{{ item.description }}</p>
          </article>
        </div>
      </article>
    </section>

    <section class="dashboard-split">
      <article class="dashboard-section sqlforge-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">todo</p>
            <h2 class="section-title">{{ isChinese ? '待处理清单' : 'Todo queue' }}</h2>
          </div>
        </div>
        <div class="todo-list">
          <article
            v-for="item in todoItems"
            :key="item.key"
            class="todo-item"
            :class="`todo-item-${item.tone}`"
            data-testid="dashboard-todo-item"
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
      </article>

      <article class="dashboard-section sqlforge-panel">
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
              <p class="activity-status">{{ item.status }} · {{ item.engine }}</p>
            </div>
            <span class="activity-time">{{ item.time }}</span>
          </article>
        </div>
      </article>
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
.todo-item,
.activity-item,
.progress-card,
.blocker-item {
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
.distribution-grid {
  display: grid;
  gap: 18px;
}

.dashboard-hero,
.dashboard-split {
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 1fr);
}

.hero-copy,
.hero-aside,
.todo-list,
.activity-list,
.blocker-list {
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

.metric-grid,
.entry-grid,
.distribution-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.metric-card,
.entry-card,
.distribution-card {
  border-radius: 14px;
  padding: 18px;
}

.metric-card-danger {
  border-color: rgba(212, 96, 96, 0.35);
}

.metric-card-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.metric-card-success {
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

.todo-item-danger {
  border-color: rgba(212, 96, 96, 0.35);
}

.todo-item-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.todo-item-success {
  border-color: var(--sqlforge-color-brand-border);
}

@media (max-width: 1080px) {
  .dashboard-hero,
  .dashboard-split {
    grid-template-columns: 1fr;
  }
}
</style>
