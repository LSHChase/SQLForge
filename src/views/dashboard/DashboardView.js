/* eslint-disable no-unused-vars */
import './DashboardView.css'

import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import codexRulesMarkdown from '../../../docs/rules/codex-rules.md?raw'
import complianceMarkdown from '../../../docs/security/compliance.md?raw'
import { deliveryProgressAvailability } from '../../config/runtimeFlags'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import { useTenantStore } from '../../stores'
import { createDeliveryProgressSnapshot } from '../delivery/progressSnapshot'


const __sfc__ = {
  __name: 'DashboardView',
  setup(__props, { expose: __expose }) {
  __expose();

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

const __returned__ = { t, tm, locale, router, tenantStore, progressSnapshot, ruleIdPattern, metricCards, quickEntries, healthCards, activities, nextSteps, overviewPills, normalizeInfoCards, panoramaCards, architectureCards, complianceCards, rulebookCards, progressSourceFiles, progressDeliveryPageVisible, extractUniqueRuleIds, codexRuleIds, complianceRuleIds, rulebookSourceFiles, rulebookSummaryCards, totalTrackedTasks, completionRate, progressSummaryCards, progressModules, progressRecentChanges, progressBlockers, progressDependencies, formatDependencySummary, goTo, computed, get useI18n() { return useI18n }, get useRouter() { return useRouter }, get codexRulesMarkdown() { return codexRulesMarkdown }, get complianceMarkdown() { return complianceMarkdown }, get deliveryProgressAvailability() { return deliveryProgressAvailability }, get ROUTE_PATHS() { return ROUTE_PATHS }, get useTenantStore() { return useTenantStore }, get createDeliveryProgressSnapshot() { return createDeliveryProgressSnapshot } }
Object.defineProperty(__returned__, '__isScriptSetup', { enumerable: false, value: true })
return __returned__
}

}

import { toDisplayString as _toDisplayString, createElementVNode as _createElementVNode, renderList as _renderList, Fragment as _Fragment, openBlock as _openBlock, createElementBlock as _createElementBlock, createTextVNode as _createTextVNode, resolveComponent as _resolveComponent, withCtx as _withCtx, createVNode as _createVNode, normalizeClass as _normalizeClass, createBlock as _createBlock, createCommentVNode as _createCommentVNode, normalizeStyle as _normalizeStyle } from "vue"

const _hoisted_1 = { class: "dashboard-page" }
const _hoisted_2 = { class: "dashboard-hero" }
const _hoisted_3 = { class: "hero-copy" }
const _hoisted_4 = { class: "hero-eyebrow sqlforge-code-label" }
const _hoisted_5 = { class: "hero-title" }
const _hoisted_6 = { class: "hero-summary" }
const _hoisted_7 = { class: "hero-pills" }
const _hoisted_8 = { class: "hero-actions" }
const _hoisted_9 = { class: "hero-aside" }
const _hoisted_10 = { class: "hero-aside-card" }
const _hoisted_11 = { class: "hero-aside-label sqlforge-code-label" }
const _hoisted_12 = { class: "hero-signal" }
const _hoisted_13 = { class: "dashboard-section" }
const _hoisted_14 = { class: "section-heading" }
const _hoisted_15 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_16 = { class: "sqlforge-section-title" }
const _hoisted_17 = { class: "metric-grid" }
const _hoisted_18 = { class: "metric-label" }
const _hoisted_19 = { class: "metric-value-row" }
const _hoisted_20 = { class: "metric-value" }
const _hoisted_21 = { class: "metric-trend" }
const _hoisted_22 = { class: "metric-detail" }
const _hoisted_23 = { class: "dashboard-section" }
const _hoisted_24 = { class: "section-heading" }
const _hoisted_25 = { class: "sqlforge-section-title" }
const _hoisted_26 = { class: "section-summary" }
const _hoisted_27 = { class: "entry-grid" }
const _hoisted_28 = { class: "entry-label sqlforge-code-label" }
const _hoisted_29 = { class: "entry-title" }
const _hoisted_30 = { class: "entry-description" }
const _hoisted_31 = { class: "entry-status" }
const _hoisted_32 = { class: "dashboard-split" }
const _hoisted_33 = { class: "dashboard-section" }
const _hoisted_34 = { class: "section-heading" }
const _hoisted_35 = { class: "sqlforge-section-title" }
const _hoisted_36 = { class: "section-summary" }
const _hoisted_37 = { class: "health-list" }
const _hoisted_38 = { class: "health-label sqlforge-code-label" }
const _hoisted_39 = { class: "health-title" }
const _hoisted_40 = { class: "health-description" }
const _hoisted_41 = { class: "dashboard-section" }
const _hoisted_42 = { class: "section-heading" }
const _hoisted_43 = { class: "sqlforge-section-title" }
const _hoisted_44 = { class: "section-summary" }
const _hoisted_45 = { class: "activity-list" }
const _hoisted_46 = { class: "activity-type" }
const _hoisted_47 = { class: "activity-target" }
const _hoisted_48 = { class: "activity-status" }
const _hoisted_49 = { class: "activity-time" }
const _hoisted_50 = { class: "dashboard-section" }
const _hoisted_51 = { class: "section-heading" }
const _hoisted_52 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_53 = { class: "sqlforge-section-title" }
const _hoisted_54 = { class: "section-summary" }
const _hoisted_55 = { class: "knowledge-grid" }
const _hoisted_56 = { class: "knowledge-card-label sqlforge-code-label" }
const _hoisted_57 = { class: "knowledge-card-title" }
const _hoisted_58 = { class: "knowledge-card-summary" }
const _hoisted_59 = { class: "knowledge-card-list" }
const _hoisted_60 = { class: "dashboard-section" }
const _hoisted_61 = { class: "section-heading" }
const _hoisted_62 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_63 = { class: "sqlforge-section-title" }
const _hoisted_64 = { class: "section-summary" }
const _hoisted_65 = { class: "knowledge-grid knowledge-grid-architecture" }
const _hoisted_66 = { class: "knowledge-card-label sqlforge-code-label" }
const _hoisted_67 = { class: "knowledge-card-title" }
const _hoisted_68 = { class: "knowledge-card-summary" }
const _hoisted_69 = { class: "knowledge-card-list" }
const _hoisted_70 = { class: "dashboard-section" }
const _hoisted_71 = { class: "section-heading" }
const _hoisted_72 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_73 = { class: "sqlforge-section-title" }
const _hoisted_74 = { class: "section-heading-actions" }
const _hoisted_75 = { class: "section-badge sqlforge-code-label" }
const _hoisted_76 = {
  key: 1,
  class: "section-note"
}
const _hoisted_77 = { class: "progress-source-strip" }
const _hoisted_78 = { class: "progress-source-label sqlforge-code-label" }
const _hoisted_79 = { class: "progress-source-summary" }
const _hoisted_80 = { class: "progress-source-pills" }
const _hoisted_81 = { class: "progress-summary-grid" }
const _hoisted_82 = { class: "progress-summary-label" }
const _hoisted_83 = { class: "progress-summary-value" }
const _hoisted_84 = { class: "progress-summary-detail" }
const _hoisted_85 = { class: "progress-board" }
const _hoisted_86 = { class: "progress-panel" }
const _hoisted_87 = { class: "progress-panel-heading" }
const _hoisted_88 = { class: "progress-panel-label sqlforge-code-label" }
const _hoisted_89 = { class: "progress-panel-title" }
const _hoisted_90 = { class: "progress-panel-summary" }
const _hoisted_91 = { class: "progress-module-list" }
const _hoisted_92 = { class: "progress-module-header" }
const _hoisted_93 = { class: "progress-module-label sqlforge-code-label" }
const _hoisted_94 = { class: "progress-module-meta" }
const _hoisted_95 = { class: "progress-module-rate" }
const _hoisted_96 = { class: "progress-module-bar" }
const _hoisted_97 = { class: "progress-panel" }
const _hoisted_98 = { class: "progress-panel-heading" }
const _hoisted_99 = { class: "progress-panel-label sqlforge-code-label" }
const _hoisted_100 = { class: "progress-panel-title" }
const _hoisted_101 = { class: "progress-panel-summary" }
const _hoisted_102 = { class: "progress-list" }
const _hoisted_103 = { class: "progress-list-meta" }
const _hoisted_104 = { class: "progress-list-title" }
const _hoisted_105 = { class: "progress-list-detail" }
const _hoisted_106 = { class: "progress-panel" }
const _hoisted_107 = { class: "progress-panel-heading" }
const _hoisted_108 = { class: "progress-panel-label sqlforge-code-label" }
const _hoisted_109 = { class: "progress-panel-title" }
const _hoisted_110 = { class: "progress-panel-summary" }
const _hoisted_111 = {
  key: 0,
  class: "progress-list"
}
const _hoisted_112 = { class: "progress-list-meta" }
const _hoisted_113 = { class: "progress-list-title" }
const _hoisted_114 = { class: "progress-list-detail" }
const _hoisted_115 = {
  key: 1,
  class: "progress-empty"
}
const _hoisted_116 = {
  key: 2,
  class: "progress-list"
}
const _hoisted_117 = { class: "progress-list-meta" }
const _hoisted_118 = { class: "progress-list-title" }
const _hoisted_119 = { class: "progress-list-detail" }
const _hoisted_120 = {
  key: 3,
  class: "progress-empty"
}
const _hoisted_121 = { class: "dashboard-section" }
const _hoisted_122 = { class: "section-heading" }
const _hoisted_123 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_124 = { class: "sqlforge-section-title" }
const _hoisted_125 = { class: "section-summary" }
const _hoisted_126 = { class: "progress-source-strip" }
const _hoisted_127 = { class: "progress-source-label sqlforge-code-label" }
const _hoisted_128 = { class: "progress-source-summary" }
const _hoisted_129 = { class: "knowledge-grid knowledge-grid-compliance" }
const _hoisted_130 = { class: "knowledge-card-label sqlforge-code-label" }
const _hoisted_131 = { class: "knowledge-card-title" }
const _hoisted_132 = { class: "knowledge-card-summary" }
const _hoisted_133 = { class: "knowledge-card-list" }
const _hoisted_134 = { class: "dashboard-section" }
const _hoisted_135 = { class: "section-heading" }
const _hoisted_136 = { class: "section-kicker sqlforge-code-label" }
const _hoisted_137 = { class: "sqlforge-section-title" }
const _hoisted_138 = { class: "section-summary" }
const _hoisted_139 = { class: "progress-source-strip" }
const _hoisted_140 = { class: "progress-source-label sqlforge-code-label" }
const _hoisted_141 = { class: "progress-source-summary" }
const _hoisted_142 = { class: "progress-source-pills" }
const _hoisted_143 = { class: "progress-summary-grid" }
const _hoisted_144 = { class: "progress-summary-label" }
const _hoisted_145 = { class: "progress-summary-value" }
const _hoisted_146 = { class: "progress-summary-detail" }
const _hoisted_147 = { class: "knowledge-grid" }
const _hoisted_148 = { class: "knowledge-card-label sqlforge-code-label" }
const _hoisted_149 = { class: "knowledge-card-title" }
const _hoisted_150 = { class: "knowledge-card-summary" }
const _hoisted_151 = { class: "knowledge-card-list" }
const _hoisted_152 = { class: "dashboard-section" }
const _hoisted_153 = { class: "section-heading" }
const _hoisted_154 = { class: "sqlforge-section-title" }
const _hoisted_155 = { class: "section-summary" }
const _hoisted_156 = { class: "next-grid" }
const _hoisted_157 = { class: "next-title" }
const _hoisted_158 = { class: "next-description" }

function render(_ctx, _cache, $props, $setup, $data, $options) {
  const _component_el_button = _resolveComponent("el-button")

  return (_openBlock(), _createElementBlock("section", _hoisted_1, [
    _createElementVNode("section", _hoisted_2, [
      _createElementVNode("div", _hoisted_3, [
        _createElementVNode("p", _hoisted_4, _toDisplayString($setup.t('dashboard.eyebrow')), 1 /* TEXT */),
        _createElementVNode("h1", _hoisted_5, _toDisplayString($setup.t('dashboard.heroTitle')), 1 /* TEXT */),
        _createElementVNode("p", _hoisted_6, _toDisplayString($setup.t('dashboard.heroSummary')), 1 /* TEXT */),
        _createElementVNode("div", _hoisted_7, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.overviewPills, (pill) => {
            return (_openBlock(), _createElementBlock("span", {
              key: pill,
              class: "hero-pill"
            }, _toDisplayString(pill), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */))
        ]),
        _createElementVNode("div", _hoisted_8, [
          _createVNode(_component_el_button, {
            class: "hero-action hero-action-primary",
            onClick: _cache[0] || (_cache[0] = $event => ($setup.goTo($setup.ROUTE_PATHS.sqlQuery)))
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.t('dashboard.heroPrimary')), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          }),
          _createVNode(_component_el_button, {
            class: "hero-action hero-action-secondary",
            onClick: _cache[1] || (_cache[1] = $event => ($setup.goTo($setup.ROUTE_PATHS.benchmark)))
          }, {
            default: _withCtx(() => [
              _createTextVNode(_toDisplayString($setup.t('dashboard.heroSecondary')), 1 /* TEXT */)
            ]),
            _: 1 /* STABLE */
          })
        ])
      ]),
      _createElementVNode("div", _hoisted_9, [
        _createElementVNode("div", _hoisted_10, [
          _createElementVNode("p", _hoisted_11, _toDisplayString($setup.t('dashboard.heroFootnote')), 1 /* TEXT */),
          _createElementVNode("div", _hoisted_12, [
            _cache[3] || (_cache[3] = _createElementVNode("span", { class: "hero-signal-value" }, "03", -1 /* HOISTED */)),
            _createElementVNode("div", null, [
              _createElementVNode("h2", null, _toDisplayString($setup.locale === 'zh-CN' ? '关键风险待处置' : 'Critical risks open'), 1 /* TEXT */),
              _createElementVNode("p", null, _toDisplayString($setup.locale === 'zh-CN'
                    ? '压测准入、解析失败样本和审计异常是当前首页最需要处理的三个信号。'
                    : 'Benchmark admission, parser failures and audit anomalies are the three signals needing action now.'), 1 /* TEXT */)
            ])
          ])
        ])
      ])
    ]),
    _createElementVNode("section", _hoisted_13, [
      _createElementVNode("div", _hoisted_14, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_15, _toDisplayString($setup.t('dashboard.metricLabel')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_16, _toDisplayString($setup.t('dashboard.metricLabel')), 1 /* TEXT */)
        ])
      ]),
      _createElementVNode("div", _hoisted_17, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.metricCards, (metric) => {
          return (_openBlock(), _createElementBlock("article", {
            key: metric.key,
            class: _normalizeClass(["metric-card", `metric-card-${metric.tone}`])
          }, [
            _createElementVNode("p", _hoisted_18, _toDisplayString($setup.t(`dashboard.metrics.${metric.key}`)), 1 /* TEXT */),
            _createElementVNode("div", _hoisted_19, [
              _createElementVNode("strong", _hoisted_20, _toDisplayString(metric.value), 1 /* TEXT */),
              _createElementVNode("span", _hoisted_21, _toDisplayString(metric.trend), 1 /* TEXT */)
            ]),
            _createElementVNode("p", _hoisted_22, _toDisplayString(metric.detail), 1 /* TEXT */)
          ], 2 /* CLASS */))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_23, [
      _createElementVNode("div", _hoisted_24, [
        _createElementVNode("div", null, [
          _cache[4] || (_cache[4] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "workflow entry", -1 /* HOISTED */)),
          _createElementVNode("h2", _hoisted_25, _toDisplayString($setup.t('dashboard.quickEntryTitle')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_26, _toDisplayString($setup.t('dashboard.quickEntrySummary')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_27, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.quickEntries, (entry) => {
          return (_openBlock(), _createElementBlock("article", {
            key: entry.path,
            class: "entry-card"
          }, [
            _createElementVNode("p", _hoisted_28, _toDisplayString(entry.title), 1 /* TEXT */),
            _createElementVNode("h3", _hoisted_29, _toDisplayString(entry.title), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_30, _toDisplayString(entry.description), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_31, _toDisplayString(entry.status), 1 /* TEXT */),
            _createVNode(_component_el_button, {
              text: "",
              class: "entry-action",
              onClick: $event => ($setup.goTo(entry.path))
            }, {
              default: _withCtx(() => [
                _createTextVNode(_toDisplayString($setup.locale === 'zh-CN' ? '进入' : 'Open'), 1 /* TEXT */)
              ]),
              _: 2 /* DYNAMIC */
            }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["onClick"])
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_32, [
      _createElementVNode("section", _hoisted_33, [
        _createElementVNode("div", _hoisted_34, [
          _createElementVNode("div", null, [
            _cache[5] || (_cache[5] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "risk & health", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_35, _toDisplayString($setup.t('dashboard.healthTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_36, _toDisplayString($setup.t('dashboard.healthSummary')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_37, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.healthCards, (item) => {
            return (_openBlock(), _createElementBlock("article", {
              key: item.title,
              class: _normalizeClass(["health-card", `health-card-${item.tone}`])
            }, [
              _createElementVNode("p", _hoisted_38, _toDisplayString(item.label), 1 /* TEXT */),
              _createElementVNode("h3", _hoisted_39, _toDisplayString(item.title), 1 /* TEXT */),
              _createElementVNode("p", _hoisted_40, _toDisplayString(item.description), 1 /* TEXT */),
              _createVNode(_component_el_button, {
                text: "",
                class: "health-action",
                onClick: $event => ($setup.goTo(item.path))
              }, {
                default: _withCtx(() => [
                  _createTextVNode(_toDisplayString(item.actionLabel), 1 /* TEXT */)
                ]),
                _: 2 /* DYNAMIC */
              }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["onClick"])
            ], 2 /* CLASS */))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("section", _hoisted_41, [
        _createElementVNode("div", _hoisted_42, [
          _createElementVNode("div", null, [
            _cache[6] || (_cache[6] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "activity stream", -1 /* HOISTED */)),
            _createElementVNode("h2", _hoisted_43, _toDisplayString($setup.t('dashboard.activityTitle')), 1 /* TEXT */)
          ]),
          _createElementVNode("p", _hoisted_44, _toDisplayString($setup.t('dashboard.activitySummary')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_45, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.activities, (item) => {
            return (_openBlock(), _createElementBlock("article", {
              key: `${item.type}-${item.target}`,
              class: "activity-item"
            }, [
              _createElementVNode("div", null, [
                _createElementVNode("p", _hoisted_46, _toDisplayString(item.type), 1 /* TEXT */),
                _createElementVNode("strong", _hoisted_47, _toDisplayString(item.target), 1 /* TEXT */),
                _createElementVNode("p", _hoisted_48, _toDisplayString(item.status), 1 /* TEXT */)
              ]),
              _createElementVNode("span", _hoisted_49, _toDisplayString(item.time), 1 /* TEXT */)
            ]))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ])
    ]),
    _createElementVNode("section", _hoisted_50, [
      _createElementVNode("div", _hoisted_51, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_52, _toDisplayString($setup.t('dashboard.panorama.kicker')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_53, _toDisplayString($setup.t('dashboard.panorama.title')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_54, _toDisplayString($setup.t('dashboard.panorama.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_55, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.panoramaCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.title,
            class: "knowledge-card"
          }, [
            _createElementVNode("p", _hoisted_56, _toDisplayString($setup.t('dashboard.panorama.cardLabel')), 1 /* TEXT */),
            _createElementVNode("h3", _hoisted_57, _toDisplayString(card.title), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_58, _toDisplayString(card.summary), 1 /* TEXT */),
            _createElementVNode("ul", _hoisted_59, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(card.items, (item) => {
                return (_openBlock(), _createElementBlock("li", { key: item }, _toDisplayString(item), 1 /* TEXT */))
              }), 128 /* KEYED_FRAGMENT */))
            ])
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_60, [
      _createElementVNode("div", _hoisted_61, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_62, _toDisplayString($setup.t('dashboard.architecture.kicker')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_63, _toDisplayString($setup.t('dashboard.architecture.title')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_64, _toDisplayString($setup.t('dashboard.architecture.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_65, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.architectureCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.title,
            class: "knowledge-card"
          }, [
            _createElementVNode("p", _hoisted_66, _toDisplayString($setup.t('dashboard.architecture.cardLabel')), 1 /* TEXT */),
            _createElementVNode("h3", _hoisted_67, _toDisplayString(card.title), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_68, _toDisplayString(card.summary), 1 /* TEXT */),
            _createElementVNode("ul", _hoisted_69, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(card.items, (item) => {
                return (_openBlock(), _createElementBlock("li", { key: item }, _toDisplayString(item), 1 /* TEXT */))
              }), 128 /* KEYED_FRAGMENT */))
            ])
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_70, [
      _createElementVNode("div", _hoisted_71, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_72, _toDisplayString($setup.t('dashboard.progress.kicker')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_73, _toDisplayString($setup.t('dashboard.progress.title')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_74, [
          _createElementVNode("span", _hoisted_75, _toDisplayString($setup.t('dashboard.progress.badge')), 1 /* TEXT */),
          ($setup.progressDeliveryPageVisible)
            ? (_openBlock(), _createBlock(_component_el_button, {
                key: 0,
                text: "",
                class: "section-link",
                onClick: _cache[2] || (_cache[2] = $event => ($setup.goTo($setup.ROUTE_PATHS.deliveryProgress)))
              }, {
                default: _withCtx(() => [
                  _createTextVNode(_toDisplayString($setup.t('dashboard.progress.openDelivery')), 1 /* TEXT */)
                ]),
                _: 1 /* STABLE */
              }))
            : (_openBlock(), _createElementBlock("p", _hoisted_76, _toDisplayString($setup.t('dashboard.progress.deliveryHidden')), 1 /* TEXT */))
        ])
      ]),
      _createElementVNode("div", _hoisted_77, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_78, _toDisplayString($setup.t('dashboard.progress.sourceTitle')), 1 /* TEXT */),
          _createElementVNode("p", _hoisted_79, _toDisplayString($setup.t('dashboard.progress.sourceSummary')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_80, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.progressSourceFiles, (sourceFile) => {
            return (_openBlock(), _createElementBlock("span", {
              key: sourceFile,
              class: "hero-pill"
            }, _toDisplayString(sourceFile), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("div", _hoisted_81, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.progressSummaryCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.key,
            class: "progress-summary-card"
          }, [
            _createElementVNode("p", _hoisted_82, _toDisplayString($setup.t(`dashboard.progress.cards.${card.key}`)), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_83, _toDisplayString(card.value), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_84, _toDisplayString(card.detail), 1 /* TEXT */)
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ]),
      _createElementVNode("div", _hoisted_85, [
        _createElementVNode("article", _hoisted_86, [
          _createElementVNode("div", _hoisted_87, [
            _createElementVNode("div", null, [
              _createElementVNode("p", _hoisted_88, _toDisplayString($setup.t('dashboard.progress.modulesTitle')), 1 /* TEXT */),
              _createElementVNode("h3", _hoisted_89, _toDisplayString($setup.t('dashboard.progress.modulesTitle')), 1 /* TEXT */)
            ]),
            _createElementVNode("p", _hoisted_90, _toDisplayString($setup.t('dashboard.progress.modulesSummary')), 1 /* TEXT */)
          ]),
          _createElementVNode("div", _hoisted_91, [
            (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.progressModules, (module) => {
              return (_openBlock(), _createElementBlock("article", {
                key: module.moduleLabel,
                class: "progress-module-card"
              }, [
                _createElementVNode("div", _hoisted_92, [
                  _createElementVNode("div", null, [
                    _createElementVNode("p", _hoisted_93, _toDisplayString(module.moduleLabel), 1 /* TEXT */),
                    _createElementVNode("p", _hoisted_94, _toDisplayString($setup.t('dashboard.progress.moduleMeta', { total: module.total, done: module.done, progress: module.in_progress })), 1 /* TEXT */)
                  ]),
                  _createElementVNode("strong", _hoisted_95, _toDisplayString(module.completionRate) + "%", 1 /* TEXT */)
                ]),
                _createElementVNode("div", _hoisted_96, [
                  _createElementVNode("div", {
                    class: "progress-module-bar-fill",
                    style: _normalizeStyle({ width: `${module.completionRate}%` })
                  }, null, 4 /* STYLE */)
                ])
              ]))
            }), 128 /* KEYED_FRAGMENT */))
          ])
        ]),
        _createElementVNode("article", _hoisted_97, [
          _createElementVNode("div", _hoisted_98, [
            _createElementVNode("div", null, [
              _createElementVNode("p", _hoisted_99, _toDisplayString($setup.t('dashboard.progress.recentTitle')), 1 /* TEXT */),
              _createElementVNode("h3", _hoisted_100, _toDisplayString($setup.t('dashboard.progress.recentTitle')), 1 /* TEXT */)
            ]),
            _createElementVNode("p", _hoisted_101, _toDisplayString($setup.t('dashboard.progress.recentSummary')), 1 /* TEXT */)
          ]),
          _createElementVNode("div", _hoisted_102, [
            (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.progressRecentChanges, (item) => {
              return (_openBlock(), _createElementBlock("article", {
                key: item.itemKey,
                class: "progress-list-item"
              }, [
                _createElementVNode("p", _hoisted_103, _toDisplayString(item.timestampLabel) + " · " + _toDisplayString(item.sourceFile), 1 /* TEXT */),
                _createElementVNode("h3", _hoisted_104, _toDisplayString(item.taskId) + " · " + _toDisplayString(item.title), 1 /* TEXT */),
                _createElementVNode("p", _hoisted_105, _toDisplayString(item.detail || item.auxiliary), 1 /* TEXT */)
              ]))
            }), 128 /* KEYED_FRAGMENT */))
          ])
        ]),
        _createElementVNode("article", _hoisted_106, [
          _createElementVNode("div", _hoisted_107, [
            _createElementVNode("div", null, [
              _createElementVNode("p", _hoisted_108, _toDisplayString($setup.t('dashboard.progress.dependenciesTitle')), 1 /* TEXT */),
              _createElementVNode("h3", _hoisted_109, _toDisplayString($setup.t('dashboard.progress.dependenciesTitle')), 1 /* TEXT */)
            ]),
            _createElementVNode("p", _hoisted_110, _toDisplayString($setup.t('dashboard.progress.dependenciesSummary')), 1 /* TEXT */)
          ]),
          ($setup.progressBlockers.length)
            ? (_openBlock(), _createElementBlock("div", _hoisted_111, [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.progressBlockers, (item) => {
                  return (_openBlock(), _createElementBlock("article", {
                    key: `${item.taskId}-${item.reason}`,
                    class: "progress-list-item progress-list-item-danger"
                  }, [
                    _createElementVNode("p", _hoisted_112, _toDisplayString($setup.t('dashboard.progress.blockerLabel')), 1 /* TEXT */),
                    _createElementVNode("h3", _hoisted_113, _toDisplayString(item.taskId) + " · " + _toDisplayString(item.name), 1 /* TEXT */),
                    _createElementVNode("p", _hoisted_114, _toDisplayString(item.reason), 1 /* TEXT */)
                  ]))
                }), 128 /* KEYED_FRAGMENT */))
              ]))
            : (_openBlock(), _createElementBlock("p", _hoisted_115, _toDisplayString($setup.t('dashboard.progress.noBlockers')), 1 /* TEXT */)),
          ($setup.progressDependencies.length)
            ? (_openBlock(), _createElementBlock("div", _hoisted_116, [
                (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.progressDependencies, (task) => {
                  return (_openBlock(), _createElementBlock("article", {
                    key: task.taskId,
                    class: "progress-list-item"
                  }, [
                    _createElementVNode("p", _hoisted_117, _toDisplayString($setup.t('deliveryProgress.meta.unresolvedCount', { count: task.unresolvedCount })), 1 /* TEXT */),
                    _createElementVNode("h3", _hoisted_118, _toDisplayString(task.taskId) + " · " + _toDisplayString(task.name), 1 /* TEXT */),
                    _createElementVNode("p", _hoisted_119, _toDisplayString($setup.formatDependencySummary(task.dependencies)), 1 /* TEXT */)
                  ]))
                }), 128 /* KEYED_FRAGMENT */))
              ]))
            : (_openBlock(), _createElementBlock("p", _hoisted_120, _toDisplayString($setup.t('dashboard.progress.noDependencies')), 1 /* TEXT */))
        ])
      ])
    ]),
    _createElementVNode("section", _hoisted_121, [
      _createElementVNode("div", _hoisted_122, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_123, _toDisplayString($setup.t('dashboard.compliance.kicker')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_124, _toDisplayString($setup.t('dashboard.compliance.title')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_125, _toDisplayString($setup.t('dashboard.compliance.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_126, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_127, _toDisplayString($setup.t('dashboard.compliance.sourceTitle')), 1 /* TEXT */),
          _createElementVNode("p", _hoisted_128, _toDisplayString($setup.t('dashboard.compliance.sourceSummary')), 1 /* TEXT */)
        ]),
        _cache[7] || (_cache[7] = _createElementVNode("div", { class: "progress-source-pills" }, [
          _createElementVNode("span", { class: "hero-pill" }, "docs/security/compliance.md")
        ], -1 /* HOISTED */))
      ]),
      _createElementVNode("div", _hoisted_129, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.complianceCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.title,
            class: "knowledge-card"
          }, [
            _createElementVNode("p", _hoisted_130, _toDisplayString(card.ruleId), 1 /* TEXT */),
            _createElementVNode("h3", _hoisted_131, _toDisplayString(card.title), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_132, _toDisplayString(card.summary), 1 /* TEXT */),
            _createElementVNode("ul", _hoisted_133, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(card.items, (item) => {
                return (_openBlock(), _createElementBlock("li", { key: item }, _toDisplayString(item), 1 /* TEXT */))
              }), 128 /* KEYED_FRAGMENT */))
            ])
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_134, [
      _createElementVNode("div", _hoisted_135, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_136, _toDisplayString($setup.t('dashboard.rulebook.kicker')), 1 /* TEXT */),
          _createElementVNode("h2", _hoisted_137, _toDisplayString($setup.t('dashboard.rulebook.title')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_138, _toDisplayString($setup.t('dashboard.rulebook.summary')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_139, [
        _createElementVNode("div", null, [
          _createElementVNode("p", _hoisted_140, _toDisplayString($setup.t('dashboard.rulebook.sourceTitle')), 1 /* TEXT */),
          _createElementVNode("p", _hoisted_141, _toDisplayString($setup.t('dashboard.rulebook.sourceSummary')), 1 /* TEXT */)
        ]),
        _createElementVNode("div", _hoisted_142, [
          (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.rulebookSourceFiles, (sourceFile) => {
            return (_openBlock(), _createElementBlock("span", {
              key: sourceFile,
              class: "hero-pill"
            }, _toDisplayString(sourceFile), 1 /* TEXT */))
          }), 128 /* KEYED_FRAGMENT */))
        ])
      ]),
      _createElementVNode("div", _hoisted_143, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.rulebookSummaryCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.key,
            class: "progress-summary-card"
          }, [
            _createElementVNode("p", _hoisted_144, _toDisplayString($setup.t(`dashboard.rulebook.cardsSummary.${card.key}`)), 1 /* TEXT */),
            _createElementVNode("strong", _hoisted_145, _toDisplayString(card.value), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_146, _toDisplayString(card.detail), 1 /* TEXT */)
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ]),
      _createElementVNode("div", _hoisted_147, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.rulebookCards, (card) => {
          return (_openBlock(), _createElementBlock("article", {
            key: card.title,
            class: "knowledge-card"
          }, [
            _createElementVNode("p", _hoisted_148, _toDisplayString($setup.t('dashboard.rulebook.cardLabel')), 1 /* TEXT */),
            _createElementVNode("h3", _hoisted_149, _toDisplayString(card.title), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_150, _toDisplayString(card.summary), 1 /* TEXT */),
            _createElementVNode("ul", _hoisted_151, [
              (_openBlock(true), _createElementBlock(_Fragment, null, _renderList(card.items, (item) => {
                return (_openBlock(), _createElementBlock("li", { key: item }, _toDisplayString(item), 1 /* TEXT */))
              }), 128 /* KEYED_FRAGMENT */))
            ])
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ]),
    _createElementVNode("section", _hoisted_152, [
      _createElementVNode("div", _hoisted_153, [
        _createElementVNode("div", null, [
          _cache[8] || (_cache[8] = _createElementVNode("p", { class: "section-kicker sqlforge-code-label" }, "recommended next", -1 /* HOISTED */)),
          _createElementVNode("h2", _hoisted_154, _toDisplayString($setup.t('dashboard.nextTitle')), 1 /* TEXT */)
        ]),
        _createElementVNode("p", _hoisted_155, _toDisplayString($setup.t('dashboard.nextSummary')), 1 /* TEXT */)
      ]),
      _createElementVNode("div", _hoisted_156, [
        (_openBlock(true), _createElementBlock(_Fragment, null, _renderList($setup.nextSteps, (step) => {
          return (_openBlock(), _createElementBlock("article", {
            key: step.title,
            class: "next-card"
          }, [
            _createElementVNode("h3", _hoisted_157, _toDisplayString(step.title), 1 /* TEXT */),
            _createElementVNode("p", _hoisted_158, _toDisplayString(step.description), 1 /* TEXT */),
            _createVNode(_component_el_button, {
              class: "next-action",
              onClick: $event => ($setup.goTo(step.path))
            }, {
              default: _withCtx(() => [
                _createTextVNode(_toDisplayString($setup.locale === 'zh-CN' ? '立即处理' : 'Take action'), 1 /* TEXT */)
              ]),
              _: 2 /* DYNAMIC */
            }, 1032 /* PROPS, DYNAMIC_SLOTS */, ["onClick"])
          ]))
        }), 128 /* KEYED_FRAGMENT */))
      ])
    ])
  ]))
}
__sfc__.__scopeId = 'data-v-ed7dad95'
__sfc__.render = render
__sfc__.__file = "src/views/dashboard/DashboardView.js"

export default __sfc__
