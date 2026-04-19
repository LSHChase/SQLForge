<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useTenantStore } from '../../stores'

const { t, locale } = useI18n()
const router = useRouter()
const tenantStore = useTenantStore()

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
    path: '/sql-query'
  },
  {
    title: t('parseRecord.title'),
    description: t('parseRecord.summary'),
    status: locale.value === 'zh-CN' ? '最近失败样本集中在窗口函数改写' : 'Recent failures cluster around window-function rewrites',
    path: '/parse-record'
  },
  {
    title: t('benchmark.title'),
    description: t('benchmark.summary'),
    status: locale.value === 'zh-CN' ? '回归基线已刷新，3 个任务待复测' : 'Regression baseline refreshed; 3 runs pending',
    path: '/benchmark'
  },
  {
    title: t('acceleration.title'),
    description: t('acceleration.summary'),
    status: locale.value === 'zh-CN' ? '近 7 天命中率回升，冷表策略可继续扩大' : 'Hit rate recovered in 7d; cold-table policy can expand',
    path: '/acceleration'
  },
  {
    title: t('system.title'),
    description: t('system.summary'),
    status: locale.value === 'zh-CN' ? '审计保留和默认路由策略今日有变更' : 'Audit retention and default routing changed today',
    path: '/system'
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
    path: '/benchmark'
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
    path: '/system'
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
    path: '/parse-record'
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
    path: '/benchmark'
  },
  {
    title: locale.value === 'zh-CN' ? '检查窗口函数改写失败样本' : 'Inspect parser failures around window functions',
    description:
      locale.value === 'zh-CN'
        ? '解析失败样本持续累积，已开始影响准入判断。'
        : 'Failure samples are accumulating and already affecting admission decisions.',
    path: '/parse-record'
  },
  {
    title: locale.value === 'zh-CN' ? '扩大冷表加速策略覆盖面' : 'Expand cold-table acceleration coverage',
    description:
      locale.value === 'zh-CN'
        ? '当前命中率回升，可继续放大已验证策略。'
        : 'Hit rate is recovering and validated policies can now expand.',
    path: '/acceleration'
  }
])

const overviewPills = computed(() => [
  tenantStore.tenantName,
  tenantStore.defaultEngine,
  locale.value === 'zh-CN' ? '审计保留 180d+' : 'Audit retention 180d+'
])

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
          <el-button class="hero-action hero-action-primary" @click="goTo('/sql-query')">
            {{ t('dashboard.heroPrimary') }}
          </el-button>
          <el-button class="hero-action hero-action-secondary" @click="goTo('/benchmark')">
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
</style>
