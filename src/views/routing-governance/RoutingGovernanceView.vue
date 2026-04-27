<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
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
  detail: false
})

const errorMessage = ref('')
const routeCalibration = ref(null)
const recentTraces = ref([])
const traceDetail = ref(null)
const historyDetail = ref(null)
const detailDialogVisible = ref(false)
const rawDrawerVisible = ref(false)
const placeholderDialogVisible = ref(false)
const placeholderPayload = ref({
  title: '',
  capability: '',
  reason: '',
  nextStep: ''
})

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
    field('implementationStage', isChinese.value ? '实现阶段' : 'Implementation stage', calibration.implementationStage)
  ]
})
const commentProtocolCards = computed(() => [
  {
    key: 'engineHint',
    title: 'engineHint',
    example: '/* engineHint=HETU */',
    summary: isChinese.value
      ? '通过注释表达目标引擎偏好；真正采用哪个引擎仍以后端 routeDecision 为准。'
      : 'Use a comment to express the preferred engine, while the backend routeDecision remains authoritative.'
  },
  {
    key: 'priority',
    title: 'priority',
    example: '/* priority=HIGH */',
    summary: isChinese.value
      ? '治理优先级进入上下文，但不会替代路由证据。'
      : 'Priority flows into context without replacing routing evidence.'
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
const traceHistoryRows = computed(() => traceDetail.value?.queryHistories || [])
const recommendationRefs = computed(() => historyDetail.value?.recommendationRefs || [])
const routeSignalGroups = computed(() =>
  [
    { key: 'routeDecision', title: 'routeDecision', payload: historyDetail.value?.routeDecision },
    { key: 'commentContext', title: isChinese.value ? '注释上下文' : 'Comment context', payload: historyDetail.value?.commentContext },
    { key: 'executionSummary', title: isChinese.value ? '执行摘要' : 'Execution summary', payload: historyDetail.value?.executionSummary },
    { key: 'queryContext', title: isChinese.value ? '查询上下文' : 'Query context', payload: historyDetail.value?.queryContext }
  ].filter(group => isNonEmpty(group.payload))
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
  } catch (error) {
    routeCalibration.value = null
    recentTraces.value = []
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const openPlaceholderAction = actionType => {
  const config = actionType === 'create'
    ? {
        title: isChinese.value ? '新增规则暂不可写' : 'Create-rule action is not writable yet',
        capability: isChinese.value ? '新增路由规则' : 'Create routing rule',
        reason: isChinese.value
          ? '当前仓库仅开放 route-calibration 与 query-history.routeDecision 的只读证据查看，尚未提供路由规则写接口。'
          : 'The current repository only exposes read-only route-calibration and query-history.routeDecision evidence. No writable routing-rule API is available yet.',
        nextStep: isChinese.value
          ? '需要后端新增规则写接口后，再把表单接入该证据页。'
          : 'Add a backend routing-rule write API before connecting a form here.'
      }
    : {
        title: isChinese.value ? '修改规则暂不可写' : 'Edit-rule action is not writable yet',
        capability: isChinese.value ? '修改路由规则' : 'Edit routing rule',
        reason: isChinese.value
          ? '当前页的职责是执行证据展示，不是前端配置中心；仓库真值也没有提供规则更新接口。'
          : 'This page is an execution-evidence surface rather than a frontend control plane, and the repository truth does not expose a rule-update API.',
        nextStep: isChinese.value
          ? '若后续开放写接口，应先补契约和审计链，再接入编辑动作。'
          : 'If a writable API is introduced later, wire contract and audit coverage before adding edit actions.'
      }
  placeholderPayload.value = config
  placeholderDialogVisible.value = true
}

const openDecisionDetail = async traceId => {
  if (!traceId) {
    return
  }
  loading.detail = true
  errorMessage.value = ''
  try {
    traceDetail.value = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-routing-governance-trace-detail'
    })
    const firstHistoryId = traceDetail.value?.queryHistories?.[0]?.historyId || ''
    historyDetail.value = firstHistoryId
      ? await getGovernanceQueryHistoryDetail(form.tenantId, firstHistoryId, {
          requestPrefix: 'frontend-routing-governance-history-detail'
        })
      : null
    detailDialogVisible.value = true
  } catch (error) {
    traceDetail.value = null
    historyDetail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const openEvidenceDetail = () => {
  rawDrawerVisible.value = true
}

const openParseRecord = () => {
  if (!traceDetail.value?.traceId) {
    return
  }
  router.push({
    path: ROUTE_PATHS.parseRecord,
    query: {
      tenantId: form.tenantId,
      traceId: traceDetail.value.traceId
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

const listText = value => {
  if (Array.isArray(value)) {
    return value.join(' -> ')
  }
  return displayValue(value)
}

const isNonEmpty = value => {
  if (Array.isArray(value)) {
    return value.length > 0
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).length > 0
  }
  return hasDisplayValue(value)
}

const formatJson = value => JSON.stringify(value, null, 2)
const formatTimestamp = value => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

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
    <header class="surface-card page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">routing execution evidence</p>
        <h1 class="section-title">{{ isChinese ? '路由执行证据与历史决策' : 'Routing execution evidence and decision history' }}</h1>
        <p class="section-summary">
          {{
            isChinese
              ? '当前页只消费 route-calibration 与 query-history.routeDecision 的只读证据，不再伪装成规则配置中心。'
              : 'This page only consumes read-only route-calibration and query-history.routeDecision evidence instead of pretending to be a rule-configuration center.'
          }}
        </p>
      </div>
      <div class="action-row">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="form.tenantId" data-testid="routing-tenant-input" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'Trace 数量' : 'Trace limit' }}</span>
          <el-input v-model="form.traceLimit" />
        </label>
        <el-button type="primary" :loading="loading.page" data-testid="routing-refresh" @click="refreshPage">
          {{ isChinese ? '刷新路由证据' : 'Refresh routing evidence' }}
        </el-button>
        <el-button @click="openEvidenceDetail">
          {{ isChinese ? '查看当前策略来源' : 'View current policy source' }}
        </el-button>
        <el-button @click="openPlaceholderAction('create')">
          {{ isChinese ? '新增规则' : 'Create rule' }}
        </el-button>
        <el-button @click="openPlaceholderAction('edit')">
          {{ isChinese ? '修改规则' : 'Edit rule' }}
        </el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <div class="workspace-grid">
      <section class="surface-card">
        <div class="table-heading" data-testid="routing-current-policy">
          <div>
            <p class="section-kicker sqlforge-code-label">current policy</p>
            <h2 class="section-title">{{ isChinese ? '当前策略快照' : 'Current policy snapshot' }}</h2>
          </div>
        </div>
        <div class="detail-grid">
          <div
            v-for="item in policyCards"
            :key="item.key"
            class="detail-grid__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ displayValue(item.value) }}</strong>
          </div>
        </div>
      </section>

      <section class="surface-card" data-testid="routing-comment-protocol">
        <div class="table-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">comment protocol</p>
            <h2 class="section-title">{{ isChinese ? '注释协议摘要' : 'Comment protocol summary' }}</h2>
          </div>
        </div>
        <div class="detail-grid">
          <div
            v-for="item in commentProtocolCards"
            :key="item.key"
            class="detail-grid__item"
          >
            <span>{{ item.title }}</span>
            <strong>{{ item.example }}</strong>
            <p>{{ item.summary }}</p>
          </div>
        </div>
      </section>
    </div>

    <section class="surface-card table-panel">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">routing-route-decision</p>
          <h2 class="section-title">{{ isChinese ? '路由决策历史' : 'Routing decision history' }}</h2>
        </div>
      </div>

      <el-table :data="recentTraces" border>
        <el-table-column prop="traceId" label="Trace ID" min-width="180">
          <template #default="{ row }">
            <button type="button" class="table-link" @click="openDecisionDetail(row.traceId)">
              {{ row.traceId }}
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="serviceCode" :label="isChinese ? '服务编码' : 'Service code'" min-width="150" />
        <el-table-column prop="latestStatus" :label="isChinese ? '状态' : 'Status'" min-width="120" />
        <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="120" />
        <el-table-column prop="auditEventCount" :label="isChinese ? '审计事件数' : 'Audit events'" min-width="120" />
        <el-table-column :label="isChinese ? '最后时间' : 'Last seen at'" min-width="170">
          <template #default="{ row }">{{ formatTimestamp(row.lastSeenAt) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="detailDialogVisible" :title="traceDetail?.traceId || 'routing decision detail'" width="980px">
      <div class="dialog-stack">
        <div class="dialog-actions">
          <el-button data-testid="routing-open-parse-record" @click="openParseRecord">
            {{ isChinese ? '打开历史详情页' : 'Open parse-record page' }}
          </el-button>
          <el-button @click="rawDrawerVisible = true">{{ isChinese ? '查看原始 JSON' : 'View raw JSON' }}</el-button>
        </div>

        <div class="detail-grid" data-testid="routing-route-decision">
          <div
            v-for="item in decisionSummaryCards"
            :key="item.key"
            class="detail-grid__item"
          >
            <span>{{ item.label }}</span>
            <strong>{{ displayValue(item.value) }}</strong>
          </div>
        </div>

        <el-table :data="traceHistoryRows" border>
          <el-table-column prop="historyId" label="History ID" min-width="170" />
          <el-table-column prop="reportCode" :label="isChinese ? '报表编码' : 'Report code'" min-width="180" />
          <el-table-column prop="historyType" :label="isChinese ? '类型' : 'Type'" min-width="140" />
          <el-table-column :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
            <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
          </el-table-column>
        </el-table>

        <div v-if="recommendationRefs.length" class="detail-grid">
          <div
            v-for="(item, index) in recommendationRefs"
            :key="`recommendation-${index}`"
            class="detail-grid__item"
          >
            <span>recommendationRefs</span>
            <strong>{{ displayValue(item.recommendationId || item.id || item) }}</strong>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="rawDrawerVisible" :title="isChinese ? '路由原始证据' : 'Raw routing evidence'" size="42%">
      <div class="drawer-stack">
        <div
          v-for="group in routeSignalGroups"
          :key="group.key"
          class="code-card"
        >
          <div class="code-card__header">{{ group.title }}</div>
          <pre class="code-block">{{ formatJson(group.payload) }}</pre>
        </div>
      </div>
    </el-drawer>

    <CapabilityPlaceholderDialog
      v-model="placeholderDialogVisible"
      :title="placeholderPayload.title"
      :capability="placeholderPayload.capability"
      :reason="placeholderPayload.reason"
      :next-step="placeholderPayload.nextStep"
    />
  </section>
</template>

<style scoped>
.routing-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.surface-card,
.field-block,
.detail-grid__item,
.code-card {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    var(--sqlforge-surface-2);
}

.page-shell,
.table-panel,
.surface-card {
  padding: 20px;
}

.page-shell,
.action-row,
.workspace-grid,
.table-heading,
.detail-grid,
.dialog-actions,
.drawer-stack {
  display: grid;
  gap: 12px;
}

.page-shell {
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
}

.section-kicker,
.field-label {
  margin: 0 0 6px;
  color: var(--sqlforge-text-muted);
}

.section-title,
.section-summary,
.detail-grid__item p {
  margin: 0;
}

.section-summary,
.detail-grid__item p {
  color: var(--sqlforge-text-secondary);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
}

.inline-banner {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.workspace-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-row {
  align-content: start;
}

.table-link {
  border: none;
  background: transparent;
  color: var(--sqlforge-color-brand);
  padding: 0;
  cursor: pointer;
}

.detail-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
}

.dialog-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-actions {
  grid-template-columns: repeat(2, max-content);
  justify-content: end;
}

.code-card {
  padding: 14px;
}

.code-card__header {
  margin-bottom: 8px;
  color: var(--sqlforge-text-secondary);
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .page-shell,
  .workspace-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
