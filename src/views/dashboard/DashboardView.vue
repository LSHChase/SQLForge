<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import codexRulesMarkdown from '../../../docs/rules/codex-rules.md?raw'
import complianceMarkdown from '../../../docs/security/compliance.md?raw'
import { deliveryProgressAvailability } from '../../config/runtimeFlags'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { useTenantStore } from '../../stores'
import { createDeliveryProgressSnapshot } from '../delivery/progressSnapshot'

const { t, tm, locale } = useI18n()
const router = useRouter()
const tenantStore = useTenantStore()
const progressSnapshot = createDeliveryProgressSnapshot()
const ruleIdPattern = /\bR-\d+\b/g

const metricCards = computed(() => [
  {
    key: 'queryVolume',
    value: '14.8M',
    detail: locale.value === 'zh-CN' ? '近 24 小时查询负载，主租户流量稳定' : '24h workload with stable traffic on the primary tenant',
    trend: '+8.4%',
    tone: 'neutral'
  },
  {
    key: 'parseSuccess',
    value: '98.6%',
    detail: locale.value === 'zh-CN' ? '改写失败样本下降，语法漂移可控' : 'Rewrite failures are down and syntax drift is contained',
    trend: '+1.2%',
    tone: 'success'
  },
  {
    key: 'benchmarkPass',
    value: '93.1%',
    detail: locale.value === 'zh-CN' ? '准入任务仍有 3 个待复测' : 'Three admission runs still require revalidation',
    trend: '-0.7%',
    tone: 'warning'
  },
  {
    key: 'accelerationHit',
    value: '76.4%',
    detail: locale.value === 'zh-CN' ? '冷表命中提升，热链路仍有优化空间' : 'Cold tables improved while hot paths still need tuning',
    trend: '+3.9%',
    tone: 'success'
  },
  {
    key: 'auditSignal',
    value: '12',
    detail: locale.value === 'zh-CN' ? '近 24 小时出现 12 条高优先级审计/异常信号' : 'Twelve high-priority audit or anomaly signals in the last 24h',
    trend: 'P1',
    tone: 'danger'
  }
])

const quickEntries = computed(() => [
  {
    title: t('sqlQuery.title'),
    description: t('sqlQuery.summary'),
    status: locale.value === 'zh-CN' ? '默认走 HETU 主路径，待处理慢查询 4 条' : 'HETU is primary; 4 slow queries need attention',
    path: ROUTE_PATHS.sqlQuery
  },
  {
    title: t('benchmark.title'),
    description: t('benchmark.summary'),
    status: locale.value === 'zh-CN' ? '回归基线已刷新，3 个任务待复测' : 'Regression baseline refreshed; 3 runs pending',
    path: ROUTE_PATHS.benchmark
  },
  {
    title: t('acceleration.title'),
    description: t('acceleration.summary'),
    status: locale.value === 'zh-CN' ? '近 7 天命中率回升，冷表策略可继续扩大' : 'Hit rate recovered in 7d; cold-table policy can expand',
    path: ROUTE_PATHS.acceleration
  },
  {
    title: t('system.title'),
    description: t('system.summary'),
    status: locale.value === 'zh-CN' ? '审计保留和默认路由策略今日有变更' : 'Audit retention and default routing changed today',
    path: ROUTE_PATHS.system
  }
])

const healthCards = computed(() => [
  {
    label: locale.value === 'zh-CN' ? '服务健康' : 'Service health',
    title: locale.value === 'zh-CN' ? '治理链路整体可用，但压测准入仍有缺口' : 'Governance path is available, but benchmark admission still has gaps',
    description:
      locale.value === 'zh-CN'
        ? '查询、解析、加速链路正常，压测准入链路中 3 个任务仍未回写结果。'
        : 'Query, parser and acceleration paths are healthy, while 3 admission runs have not written back results.',
    tone: 'warning',
    actionLabel: locale.value === 'zh-CN' ? '进入压测报告' : 'Open Benchmark',
    path: ROUTE_PATHS.benchmark
  },
  {
    label: locale.value === 'zh-CN' ? '规则一致性' : 'Rule integrity',
    title: locale.value === 'zh-CN' ? '规则与运行假设一致，未发现越权路径漂移' : 'Rules and runtime assumptions align with no detected access-control drift',
    description:
      locale.value === 'zh-CN'
        ? '最近规则追加已入账，审计链路仍满足当前约束。'
        : 'Recent rule additions are recorded and the audit path remains aligned with current constraints.',
    tone: 'success',
    actionLabel: locale.value === 'zh-CN' ? '查看系统管理' : 'Open System',
    path: ROUTE_PATHS.system
  },
  {
    label: locale.value === 'zh-CN' ? '待处理风险' : 'Open risk',
    title: locale.value === 'zh-CN' ? '解析失败样本正在积累，需要尽快下钻' : 'Parser failure samples are accumulating and need drill-down now',
    description:
      locale.value === 'zh-CN'
        ? '窗口函数与多层子查询是当前主要故障面。'
        : 'Window functions and nested subqueries are the primary failure surface.',
    tone: 'danger',
    actionLabel: locale.value === 'zh-CN' ? '进入解析记录' : 'Open Parse Record',
    path: ROUTE_PATHS.parseRecord
  }
])

const activities = computed(() => [
  {
    type: locale.value === 'zh-CN' ? 'SQL Query' : 'SQL Query',
    target: 'tenant/system/order_revenue_rollup',
    time: locale.value === 'zh-CN' ? '09:12' : '09:12',
    status: locale.value === 'zh-CN' ? '已进入治理链路' : 'Entered governance flow'
  },
  {
    type: locale.value === 'zh-CN' ? 'Parse Record' : 'Parse Record',
    target: 'rewrite/window_rank_daily',
    time: locale.value === 'zh-CN' ? '08:47' : '08:47',
    status: locale.value === 'zh-CN' ? '改写失败，待人工分析' : 'Rewrite failed, awaiting review'
  },
  {
    type: locale.value === 'zh-CN' ? 'Benchmark' : 'Benchmark',
    target: 'campaign/bi-p99-regression',
    time: locale.value === 'zh-CN' ? '08:21' : '08:21',
    status: locale.value === 'zh-CN' ? '基线回写完成' : 'Baseline write-back finished'
  },
  {
    type: locale.value === 'zh-CN' ? 'Acceleration' : 'Acceleration',
    target: 'policy/materialized_mv_customer',
    time: locale.value === 'zh-CN' ? '07:58' : '07:58',
    status: locale.value === 'zh-CN' ? '命中率提升至 81%' : 'Hit rate increased to 81%'
  },
  {
    type: locale.value === 'zh-CN' ? 'System' : 'System',
    target: 'audit/retention-policy',
    time: locale.value === 'zh-CN' ? '07:16' : '07:16',
    status: locale.value === 'zh-CN' ? '保留策略已更新' : 'Retention policy updated'
  }
])

const nextSteps = computed(() => [
  {
    title: locale.value === 'zh-CN' ? '优先复测未回写的压测任务' : 'Re-run admission tasks missing write-back',
    description:
      locale.value === 'zh-CN'
        ? '当前 benchmark pass rate 下滑主要来自 3 个未复测任务。'
        : 'The benchmark pass-rate drop is driven by 3 runs pending revalidation.',
    path: ROUTE_PATHS.benchmark
  },
  {
    title: locale.value === 'zh-CN' ? '检查窗口函数改写失败样本' : 'Inspect parser failures around window functions',
    description:
      locale.value === 'zh-CN'
        ? '解析失败样本持续累积，已开始影响准入判断。'
        : 'Failure samples are accumulating and already affecting admission decisions.',
    path: ROUTE_PATHS.parseRecord
  },
  {
    title: locale.value === 'zh-CN' ? '扩大冷表加速策略覆盖面' : 'Expand cold-table acceleration coverage',
    description:
      locale.value === 'zh-CN'
        ? '当前命中率回升，可继续放大已验证策略。'
        : 'Hit rate is recovering and validated policies can now expand.',
    path: ROUTE_PATHS.acceleration
  }
])

const overviewPills = computed(() => [
  tenantStore.tenantName,
  tenantStore.defaultEngine,
  locale.value === 'zh-CN' ? '审计保留 180d+' : 'Audit retention 180d+'
])

const normalizeInfoCards = path => {
  const cards = tm(path)
  return Array.isArray(cards)
    ? cards.map(card => ({
        ...card,
        items: Array.isArray(card.items) ? card.items : []
      }))
    : []
}

const panoramaCards = computed(() => normalizeInfoCards('dashboard.panorama.cards'))
const architectureCards = computed(() => normalizeInfoCards('dashboard.architecture.cards'))
const complianceCards = computed(() => normalizeInfoCards('dashboard.compliance.cards'))
const rulebookCards = computed(() => normalizeInfoCards('dashboard.rulebook.cards'))
const progressSourceFiles = computed(() => progressSnapshot.sourceFiles)
const progressDeliveryPageVisible = computed(() => deliveryProgressAvailability.enabled)
const extractUniqueRuleIds = markdown =>
  Array.from(new Set(Array.from(markdown.matchAll(ruleIdPattern)).map(match => match[0]))).sort(
    (left, right) => Number(left.slice(2)) - Number(right.slice(2))
  )

const codexRuleIds = extractUniqueRuleIds(codexRulesMarkdown)
const complianceRuleIds = extractUniqueRuleIds(complianceMarkdown)
const rulebookSourceFiles = computed(() => ['docs/rules/codex-rules.md', 'docs/security/compliance.md'])
const rulebookSummaryCards = computed(() => [
  {
    key: 'baseline',
    value: codexRuleIds.filter(ruleId => Number(ruleId.slice(2)) <= 115).length,
    detail: t('dashboard.rulebook.cardsSummary.baselineDetail')
  },
  {
    key: 'validation',
    value: codexRuleIds.filter(ruleId => Number(ruleId.slice(2)) > 115).length,
    detail: t('dashboard.rulebook.cardsSummary.validationDetail')
  },
  {
    key: 'compliance',
    value: complianceRuleIds.length,
    detail: t('dashboard.rulebook.cardsSummary.complianceDetail')
  },
  {
    key: 'sources',
    value: rulebookSourceFiles.value.length,
    detail: t('dashboard.rulebook.cardsSummary.sourcesDetail')
  }
])
const totalTrackedTasks = computed(() =>
  Object.values(progressSnapshot.statusCounts).reduce((sum, value) => sum + value, 0)
)
const completionRate = computed(() =>
  totalTrackedTasks.value === 0 ? 0 : Math.round((progressSnapshot.statusCounts.done / totalTrackedTasks.value) * 100)
)
const progressSummaryCards = computed(() => [
  {
    key: 'active',
    value: progressSnapshot.activeTasks.length,
    detail: t('dashboard.progress.cards.activeDetail')
  },
  {
    key: 'done',
    value: progressSnapshot.statusCounts.done,
    detail: t('dashboard.progress.cards.doneDetail')
  },
  {
    key: 'validation',
    value: progressSnapshot.validationEntries.length,
    detail: t('dashboard.progress.cards.validationDetail')
  },
  {
    key: 'completion',
    value: `${completionRate.value}%`,
    detail: t('dashboard.progress.cards.completionDetail')
  }
])
const progressModules = computed(() => progressSnapshot.moduleProgress.slice(0, 4))
const progressRecentChanges = computed(() => progressSnapshot.recentChanges.slice(0, 4))
const progressBlockers = computed(() => progressSnapshot.blockerItems.slice(0, 4))
const progressDependencies = computed(() => progressSnapshot.dependencyChains.slice(0, 3))

const formatDependencySummary = dependencies =>
  dependencies
    .map(dependency => `${dependency.dependencyId} · ${t(`deliveryProgress.status.${dependency.statusKey}`)}`)
    .join(' / ')

const goTo = path => {
  router.push(path)
}
</script>

<template>
  <section class="dashboard-page">
    <section class="dashboard-hero">
      <div class="hero-copy">
        <p class="hero-eyebrow sqlforge-code-label">{{ t('dashboard.eyebrow') }}</p>
        <h1 class="hero-title">{{ t('dashboard.heroTitle') }}</h1>
        <p class="hero-summary">{{ t('dashboard.heroSummary') }}</p>
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
          <el-button class="hero-action hero-action-primary" @click="goTo(ROUTE_PATHS.sqlQuery)">
            {{ t('dashboard.heroPrimary') }}
          </el-button>
          <el-button class="hero-action hero-action-secondary" @click="goTo(ROUTE_PATHS.benchmark)">
            {{ t('dashboard.heroSecondary') }}
          </el-button>
        </div>
      </div>

      <div class="hero-aside">
        <div class="hero-aside-card">
          <p class="hero-aside-label sqlforge-code-label">{{ t('dashboard.heroFootnote') }}</p>
          <div class="hero-signal">
            <span class="hero-signal-value">03</span>
            <div>
              <h2>{{ locale === 'zh-CN' ? '关键风险待处置' : 'Critical risks open' }}</h2>
              <p>
                {{
                  locale === 'zh-CN'
                    ? '压测准入、解析失败样本和审计异常是当前首页最需要处理的三个信号。'
                    : 'Benchmark admission, parser failures and audit anomalies are the three signals needing action now.'
                }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('dashboard.metricLabel') }}</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.metricLabel') }}</h2>
        </div>
      </div>
      <div class="metric-grid">
        <article
          v-for="metric in metricCards"
          :key="metric.key"
          class="metric-card"
          :class="`metric-card-${metric.tone}`"
        >
          <p class="metric-label">{{ t(`dashboard.metrics.${metric.key}`) }}</p>
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
          <h2 class="sqlforge-section-title">{{ t('dashboard.quickEntryTitle') }}</h2>
        </div>
        <p class="section-summary">{{ t('dashboard.quickEntrySummary') }}</p>
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
          <el-button text class="entry-action" @click="goTo(entry.path)">
            {{ locale === 'zh-CN' ? '进入' : 'Open' }}
          </el-button>
        </article>
      </div>
    </section>

    <section class="dashboard-split">
      <section class="dashboard-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">risk & health</p>
            <h2 class="sqlforge-section-title">{{ t('dashboard.healthTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('dashboard.healthSummary') }}</p>
        </div>
        <div class="health-list">
          <article
            v-for="item in healthCards"
            :key="item.title"
            class="health-card"
            :class="`health-card-${item.tone}`"
          >
            <p class="health-label sqlforge-code-label">{{ item.label }}</p>
            <h3 class="health-title">{{ item.title }}</h3>
            <p class="health-description">{{ item.description }}</p>
            <el-button text class="health-action" @click="goTo(item.path)">
              {{ item.actionLabel }}
            </el-button>
          </article>
        </div>
      </section>

      <section class="dashboard-section">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">activity stream</p>
            <h2 class="sqlforge-section-title">{{ t('dashboard.activityTitle') }}</h2>
          </div>
          <p class="section-summary">{{ t('dashboard.activitySummary') }}</p>
        </div>
        <div class="activity-list">
          <article
            v-for="item in activities"
            :key="`${item.type}-${item.target}`"
            class="activity-item"
          >
            <div>
              <p class="activity-type">{{ item.type }}</p>
              <strong class="activity-target">{{ item.target }}</strong>
              <p class="activity-status">{{ item.status }}</p>
            </div>
            <span class="activity-time">{{ item.time }}</span>
          </article>
        </div>
      </section>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('dashboard.panorama.kicker') }}</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.panorama.title') }}</h2>
        </div>
        <p class="section-summary">{{ t('dashboard.panorama.summary') }}</p>
      </div>
      <div class="knowledge-grid">
        <article
          v-for="card in panoramaCards"
          :key="card.title"
          class="knowledge-card"
        >
          <p class="knowledge-card-label sqlforge-code-label">{{ t('dashboard.panorama.cardLabel') }}</p>
          <h3 class="knowledge-card-title">{{ card.title }}</h3>
          <p class="knowledge-card-summary">{{ card.summary }}</p>
          <ul class="knowledge-card-list">
            <li
              v-for="item in card.items"
              :key="item"
            >
              {{ item }}
            </li>
          </ul>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('dashboard.architecture.kicker') }}</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.architecture.title') }}</h2>
        </div>
        <p class="section-summary">{{ t('dashboard.architecture.summary') }}</p>
      </div>
      <div class="knowledge-grid knowledge-grid-architecture">
        <article
          v-for="card in architectureCards"
          :key="card.title"
          class="knowledge-card"
        >
          <p class="knowledge-card-label sqlforge-code-label">{{ t('dashboard.architecture.cardLabel') }}</p>
          <h3 class="knowledge-card-title">{{ card.title }}</h3>
          <p class="knowledge-card-summary">{{ card.summary }}</p>
          <ul class="knowledge-card-list">
            <li
              v-for="item in card.items"
              :key="item"
            >
              {{ item }}
            </li>
          </ul>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('dashboard.progress.kicker') }}</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.progress.title') }}</h2>
        </div>
        <div class="section-heading-actions">
          <span class="section-badge sqlforge-code-label">{{ t('dashboard.progress.badge') }}</span>
          <el-button
            v-if="progressDeliveryPageVisible"
            text
            class="section-link"
            @click="goTo(ROUTE_PATHS.deliveryProgress)"
          >
            {{ t('dashboard.progress.openDelivery') }}
          </el-button>
          <p
            v-else
            class="section-note"
          >
            {{ t('dashboard.progress.deliveryHidden') }}
          </p>
        </div>
      </div>

      <div class="progress-source-strip">
        <div>
          <p class="progress-source-label sqlforge-code-label">{{ t('dashboard.progress.sourceTitle') }}</p>
          <p class="progress-source-summary">{{ t('dashboard.progress.sourceSummary') }}</p>
        </div>
        <div class="progress-source-pills">
          <span
            v-for="sourceFile in progressSourceFiles"
            :key="sourceFile"
            class="hero-pill"
          >
            {{ sourceFile }}
          </span>
        </div>
      </div>

      <div class="progress-summary-grid">
        <article
          v-for="card in progressSummaryCards"
          :key="card.key"
          class="progress-summary-card"
        >
          <p class="progress-summary-label">{{ t(`dashboard.progress.cards.${card.key}`) }}</p>
          <strong class="progress-summary-value">{{ card.value }}</strong>
          <p class="progress-summary-detail">{{ card.detail }}</p>
        </article>
      </div>

      <div class="progress-board">
        <article class="progress-panel">
          <div class="progress-panel-heading">
            <div>
              <p class="progress-panel-label sqlforge-code-label">{{ t('dashboard.progress.modulesTitle') }}</p>
              <h3 class="progress-panel-title">{{ t('dashboard.progress.modulesTitle') }}</h3>
            </div>
            <p class="progress-panel-summary">{{ t('dashboard.progress.modulesSummary') }}</p>
          </div>
          <div class="progress-module-list">
            <article
              v-for="module in progressModules"
              :key="module.moduleLabel"
              class="progress-module-card"
            >
              <div class="progress-module-header">
                <div>
                  <p class="progress-module-label sqlforge-code-label">{{ module.moduleLabel }}</p>
                  <p class="progress-module-meta">
                    {{ t('dashboard.progress.moduleMeta', { total: module.total, done: module.done, progress: module.in_progress }) }}
                  </p>
                </div>
                <strong class="progress-module-rate">{{ module.completionRate }}%</strong>
              </div>
              <div class="progress-module-bar">
                <div
                  class="progress-module-bar-fill"
                  :style="{ width: `${module.completionRate}%` }"
                />
              </div>
            </article>
          </div>
        </article>

        <article class="progress-panel">
          <div class="progress-panel-heading">
            <div>
              <p class="progress-panel-label sqlforge-code-label">{{ t('dashboard.progress.recentTitle') }}</p>
              <h3 class="progress-panel-title">{{ t('dashboard.progress.recentTitle') }}</h3>
            </div>
            <p class="progress-panel-summary">{{ t('dashboard.progress.recentSummary') }}</p>
          </div>
          <div class="progress-list">
            <article
              v-for="item in progressRecentChanges"
              :key="item.itemKey"
              class="progress-list-item"
            >
              <p class="progress-list-meta">{{ item.timestampLabel }} · {{ item.sourceFile }}</p>
              <h3 class="progress-list-title">{{ item.taskId }} · {{ item.title }}</h3>
              <p class="progress-list-detail">{{ item.detail || item.auxiliary }}</p>
            </article>
          </div>
        </article>

        <article class="progress-panel">
          <div class="progress-panel-heading">
            <div>
              <p class="progress-panel-label sqlforge-code-label">{{ t('dashboard.progress.dependenciesTitle') }}</p>
              <h3 class="progress-panel-title">{{ t('dashboard.progress.dependenciesTitle') }}</h3>
            </div>
            <p class="progress-panel-summary">{{ t('dashboard.progress.dependenciesSummary') }}</p>
          </div>

          <div
            v-if="progressBlockers.length"
            class="progress-list"
          >
            <article
              v-for="item in progressBlockers"
              :key="`${item.taskId}-${item.reason}`"
              class="progress-list-item progress-list-item-danger"
            >
              <p class="progress-list-meta">{{ t('dashboard.progress.blockerLabel') }}</p>
              <h3 class="progress-list-title">{{ item.taskId }} · {{ item.name }}</h3>
              <p class="progress-list-detail">{{ item.reason }}</p>
            </article>
          </div>
          <p
            v-else
            class="progress-empty"
          >
            {{ t('dashboard.progress.noBlockers') }}
          </p>

          <div
            v-if="progressDependencies.length"
            class="progress-list"
          >
            <article
              v-for="task in progressDependencies"
              :key="task.taskId"
              class="progress-list-item"
            >
              <p class="progress-list-meta">
                {{ t('deliveryProgress.meta.unresolvedCount', { count: task.unresolvedCount }) }}
              </p>
              <h3 class="progress-list-title">{{ task.taskId }} · {{ task.name }}</h3>
              <p class="progress-list-detail">{{ formatDependencySummary(task.dependencies) }}</p>
            </article>
          </div>
          <p
            v-else
            class="progress-empty"
          >
            {{ t('dashboard.progress.noDependencies') }}
          </p>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('dashboard.compliance.kicker') }}</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.compliance.title') }}</h2>
        </div>
        <p class="section-summary">{{ t('dashboard.compliance.summary') }}</p>
      </div>
      <div class="progress-source-strip">
        <div>
          <p class="progress-source-label sqlforge-code-label">{{ t('dashboard.compliance.sourceTitle') }}</p>
          <p class="progress-source-summary">{{ t('dashboard.compliance.sourceSummary') }}</p>
        </div>
        <div class="progress-source-pills">
          <span class="hero-pill">docs/security/compliance.md</span>
        </div>
      </div>
      <div class="knowledge-grid knowledge-grid-compliance">
        <article
          v-for="card in complianceCards"
          :key="card.title"
          class="knowledge-card"
        >
          <p class="knowledge-card-label sqlforge-code-label">{{ card.ruleId }}</p>
          <h3 class="knowledge-card-title">{{ card.title }}</h3>
          <p class="knowledge-card-summary">{{ card.summary }}</p>
          <ul class="knowledge-card-list">
            <li
              v-for="item in card.items"
              :key="item"
            >
              {{ item }}
            </li>
          </ul>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">{{ t('dashboard.rulebook.kicker') }}</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.rulebook.title') }}</h2>
        </div>
        <p class="section-summary">{{ t('dashboard.rulebook.summary') }}</p>
      </div>
      <div class="progress-source-strip">
        <div>
          <p class="progress-source-label sqlforge-code-label">{{ t('dashboard.rulebook.sourceTitle') }}</p>
          <p class="progress-source-summary">{{ t('dashboard.rulebook.sourceSummary') }}</p>
        </div>
        <div class="progress-source-pills">
          <span
            v-for="sourceFile in rulebookSourceFiles"
            :key="sourceFile"
            class="hero-pill"
          >
            {{ sourceFile }}
          </span>
        </div>
      </div>
      <div class="progress-summary-grid">
        <article
          v-for="card in rulebookSummaryCards"
          :key="card.key"
          class="progress-summary-card"
        >
          <p class="progress-summary-label">{{ t(`dashboard.rulebook.cardsSummary.${card.key}`) }}</p>
          <strong class="progress-summary-value">{{ card.value }}</strong>
          <p class="progress-summary-detail">{{ card.detail }}</p>
        </article>
      </div>
      <div class="knowledge-grid">
        <article
          v-for="card in rulebookCards"
          :key="card.title"
          class="knowledge-card"
        >
          <p class="knowledge-card-label sqlforge-code-label">{{ t('dashboard.rulebook.cardLabel') }}</p>
          <h3 class="knowledge-card-title">{{ card.title }}</h3>
          <p class="knowledge-card-summary">{{ card.summary }}</p>
          <ul class="knowledge-card-list">
            <li
              v-for="item in card.items"
              :key="item"
            >
              {{ item }}
            </li>
          </ul>
        </article>
      </div>
    </section>

    <section class="dashboard-section">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">recommended next</p>
          <h2 class="sqlforge-section-title">{{ t('dashboard.nextTitle') }}</h2>
        </div>
        <p class="section-summary">{{ t('dashboard.nextSummary') }}</p>
      </div>
      <div class="next-grid">
        <article
          v-for="step in nextSteps"
          :key="step.title"
          class="next-card"
        >
          <h3 class="next-title">{{ step.title }}</h3>
          <p class="next-description">{{ step.description }}</p>
          <el-button class="next-action" @click="goTo(step.path)">
            {{ locale === 'zh-CN' ? '立即处理' : 'Take action' }}
          </el-button>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-7);
}

.dashboard-hero,
.dashboard-section,
.health-card,
.activity-list,
.next-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 30%),
    var(--sqlforge-surface-3);
}

.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
  gap: var(--sqlforge-space-6);
  padding: 28px;
}

.hero-eyebrow,
.section-kicker {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.hero-title {
  max-width: 760px;
  margin: 12px 0 0;
  font-size: clamp(64px, 5vw, 72px);
  font-weight: 400;
  line-height: 1;
  letter-spacing: -0.02em;
}

.hero-summary {
  max-width: 760px;
  margin: 16px 0 0;
  color: var(--sqlforge-text-secondary);
  font-size: 16px;
  line-height: 1.6;
}

.hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.hero-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid var(--sqlforge-border-strong);
  border-radius: var(--sqlforge-radius-pill);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.hero-action {
  min-height: 42px;
  padding: 0 24px;
}

.hero-action-primary {
  border-color: #fafafa;
  background: var(--sqlforge-bg-page-deep);
  color: #fafafa;
}

.hero-action-secondary {
  border-color: var(--sqlforge-border-default);
  background: transparent;
  color: var(--sqlforge-text-primary);
}

.hero-aside {
  display: flex;
}

.hero-aside-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  padding: 20px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: rgba(15, 15, 15, 0.48);
}

.hero-aside-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.hero-signal {
  display: flex;
  gap: 18px;
  margin-top: 24px;
}

.hero-signal-value {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 1px solid var(--sqlforge-color-brand-border);
  border-radius: var(--sqlforge-radius-pill);
  background: rgba(62, 207, 142, 0.08);
  color: var(--sqlforge-text-primary);
  font-size: 28px;
  line-height: 1;
}

.hero-signal h2,
.entry-title,
.health-title,
.next-title {
  margin: 0;
  font-size: 24px;
  font-weight: 400;
  line-height: 1.2;
  letter-spacing: -0.16px;
}

.hero-signal p,
.section-summary,
.entry-description,
.entry-status,
.health-description,
.next-description {
  margin: 10px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.dashboard-section {
  padding: 24px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.section-summary {
  max-width: 480px;
}

.section-heading-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.section-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid var(--sqlforge-border-strong);
  border-radius: var(--sqlforge-radius-pill);
  color: var(--sqlforge-text-muted);
}

.section-link,
.section-note {
  margin: 0;
  padding: 0;
  color: var(--sqlforge-color-link);
}

.knowledge-grid,
.progress-summary-grid,
.progress-board {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.knowledge-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.knowledge-grid-architecture {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.knowledge-grid-compliance {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.knowledge-card,
.progress-summary-card,
.progress-panel {
  padding: 18px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(15, 15, 15, 0.34);
}

.knowledge-card-label,
.progress-source-label,
.progress-panel-label,
.progress-module-label,
.progress-list-meta {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.knowledge-card-title,
.progress-panel-title,
.progress-list-title {
  margin: 12px 0 0;
  font-size: 22px;
  font-weight: 400;
  line-height: 1.25;
}

.knowledge-card-summary,
.progress-source-summary,
.progress-panel-summary,
.progress-summary-detail,
.progress-list-detail,
.progress-empty {
  margin: 10px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.knowledge-card-list {
  margin: 16px 0 0;
  padding-left: 18px;
  color: var(--sqlforge-text-secondary);
}

.knowledge-card-list li + li {
  margin-top: 8px;
}

.progress-source-strip {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-top: 20px;
  padding: 18px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(15, 15, 15, 0.2);
}

.progress-source-pills {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.progress-summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.progress-summary-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.progress-summary-value {
  display: block;
  margin-top: 12px;
  font-size: 30px;
  font-weight: 400;
  line-height: 1;
}

.progress-board {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.progress-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.progress-panel-heading {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.progress-module-list,
.progress-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-module-card,
.progress-list-item {
  padding: 14px;
  border: 1px solid var(--sqlforge-border-subtle);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(255, 255, 255, 0.02);
}

.progress-list-item-danger {
  border-color: rgba(235, 84, 84, 0.45);
  background: linear-gradient(180deg, var(--sqlforge-status-danger), transparent 75%), rgba(255, 255, 255, 0.02);
}

.progress-module-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.progress-module-meta {
  margin: 8px 0 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.5;
}

.progress-module-rate {
  font-size: 24px;
  font-weight: 400;
}

.progress-module-bar {
  height: 8px;
  margin-top: 14px;
  overflow: hidden;
  border-radius: var(--sqlforge-radius-pill);
  background: rgba(255, 255, 255, 0.08);
}

.progress-module-bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(62, 207, 142, 0.55), rgba(62, 207, 142, 0.92));
}

.metric-grid,
.entry-grid,
.next-grid {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.metric-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.entry-grid {
  grid-template-columns: repeat(5, minmax(0, 1fr));
}

.next-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.metric-card,
.entry-card,
.next-card {
  padding: 18px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(15, 15, 15, 0.34);
}

.metric-card-success {
  border-color: var(--sqlforge-color-brand-border);
}

.metric-card-warning {
  background: linear-gradient(180deg, var(--sqlforge-status-warning), transparent 68%), rgba(15, 15, 15, 0.34);
}

.metric-card-danger {
  background: linear-gradient(180deg, var(--sqlforge-status-danger), transparent 68%), rgba(15, 15, 15, 0.34);
}

.metric-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.metric-value-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.metric-value {
  font-size: 30px;
  font-weight: 400;
  line-height: 1;
}

.metric-trend {
  color: var(--sqlforge-text-secondary);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  letter-spacing: 1.2px;
}

.metric-detail {
  margin: 14px 0 0;
  color: var(--sqlforge-text-secondary);
  font-size: 14px;
  line-height: 1.55;
}

.entry-label,
.health-label {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.entry-title,
.health-title,
.next-title {
  margin-top: 12px;
}

.entry-action,
.health-action {
  margin-top: 14px;
  padding: 0;
  color: var(--sqlforge-color-link);
}

.dashboard-split {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(340px, 0.9fr);
  gap: 24px;
}

.health-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 20px;
}

.health-card {
  padding: 18px;
}

.health-card-success {
  border-color: var(--sqlforge-color-brand-border);
}

.health-card-warning {
  background: linear-gradient(180deg, var(--sqlforge-status-warning), transparent 70%), var(--sqlforge-surface-3);
}

.health-card-danger {
  background: linear-gradient(180deg, var(--sqlforge-status-danger), transparent 70%), var(--sqlforge-surface-3);
}

.activity-list {
  padding: 0;
  overflow: hidden;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px;
}

.activity-item + .activity-item {
  border-top: 1px solid var(--sqlforge-border-subtle);
}

.activity-type,
.activity-status,
.activity-time {
  margin: 0;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.activity-target {
  display: block;
  margin-top: 8px;
  color: var(--sqlforge-text-primary);
  font-size: 15px;
  font-weight: 500;
  line-height: 1.45;
}

.activity-status {
  margin-top: 8px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.5;
}

.next-action {
  margin-top: 18px;
  min-height: 40px;
  padding: 0 18px;
  border-radius: var(--sqlforge-radius-pill);
  border-color: var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

@media (max-width: 1240px) {
  .metric-grid,
  .entry-grid,
  .knowledge-grid,
  .knowledge-grid-architecture,
  .knowledge-grid-compliance,
  .progress-summary-grid,
  .progress-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .dashboard-hero,
  .dashboard-split,
  .metric-grid,
  .entry-grid,
  .next-grid,
  .knowledge-grid,
  .knowledge-grid-architecture,
  .knowledge-grid-compliance,
  .progress-summary-grid,
  .progress-board {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-title {
    font-size: clamp(40px, 10vw, 56px);
  }

  .section-heading,
  .progress-source-strip {
    flex-direction: column;
  }

  .section-heading-actions,
  .hero-actions,
  .progress-source-pills {
    width: 100%;
    justify-content: flex-start;
  }

  .hero-actions {
    flex-direction: column;
  }
}
</style>
