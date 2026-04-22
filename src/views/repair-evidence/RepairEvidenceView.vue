<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  formatRuntimeError,
  getGovernanceTraceDetail,
  GOVERNANCE_COMPENSATION_TRACE_PREFIX,
  lookupGovernanceTraces
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()

const form = reactive({
  tenantId: 'tenant-a',
  traceId: '',
  taskId: '',
  reportId: '',
  limit: 12
})

const loadingLookup = ref(false)
const loadingDetail = ref(false)
const lookupResults = ref([])
const detail = ref(null)
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
const nonSuccessCount = computed(() =>
  lookupResults.value.filter(trace => {
    const status = String(trace.latestStatus || '').toUpperCase()
    return (trace.nonSuccessEventCount || 0) > 0 || (status && status !== 'SUCCESS' && status !== 'SUCCEEDED')
  }).length
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
const detailHighlights = computed(() => {
  if (!detail.value) {
    return []
  }
  return [
    {
      key: 'taskId',
      label: isChinese.value ? '任务 ID' : 'Task ID',
      value: detail.value.taskId
    },
    {
      key: 'reportId',
      label: isChinese.value ? '报告 ID' : 'Report ID',
      value: detail.value.reportId
    },
    {
      key: 'sqlFingerprint',
      label: isChinese.value ? 'SQL 指纹' : 'SQL fingerprint',
      value: detail.value.sqlFingerprint
    },
    {
      key: 'errorCode',
      label: isChinese.value ? '错误码' : 'Error code',
      value: detail.value.errorCode
    },
    {
      key: 'targetEngine',
      label: isChinese.value ? '目标引擎' : 'Target engine',
      value: detail.value.targetEngine
    },
    {
      key: 'degraded',
      label: isChinese.value ? '降级执行' : 'Degraded',
      value: typeof detail.value.degraded === 'boolean' ? String(detail.value.degraded) : ''
    }
  ].filter(item => displayValue(item.value) !== '-')
})
const selectedSignals = computed(() => {
  if (!detail.value) {
    return []
  }

  const signalValues = [
    {
      key: 'lookupMode',
      label: isChinese.value ? '命中维度' : 'Lookup match',
      value: resolveLookupMode(detail.value)
    },
    {
      key: 'compensationTrace',
      label: isChinese.value ? '补偿链路' : 'Compensation trace',
      value: isCompensationTrace(detail.value.traceId) ? 'true' : 'false'
    },
    {
      key: 'repairSignal',
      label: isChinese.value ? '修复信号' : 'Repair signal',
      value: resolveRepairSignal(detail.value)
    },
    {
      key: 'auditEvents',
      label: isChinese.value ? '审计事件' : 'Audit events',
      value: detail.value.auditEventCount
    }
  ]

  return signalValues.filter(item => displayValue(item.value) !== '-')
})

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

const resolveLookupMode = currentDetail => {
  if (hasDisplayValue(form.traceId) && form.traceId.trim() === currentDetail.traceId) {
    return 'TRACE'
  }
  if (hasDisplayValue(form.taskId) && form.taskId.trim() === currentDetail.taskId) {
    return 'TASK'
  }
  if (hasDisplayValue(form.reportId) && form.reportId.trim() === currentDetail.reportId) {
    return 'REPORT'
  }
  if (selectedSummary.value?.taskId && selectedSummary.value.taskId === currentDetail.taskId) {
    return 'TASK'
  }
  if (selectedSummary.value?.reportId && selectedSummary.value.reportId === currentDetail.reportId) {
    return 'REPORT'
  }
  return 'TRACE'
}

const resolveRepairSignal = currentDetail => {
  if (isCompensationTrace(currentDetail.traceId)) {
    return 'COMPENSATION_TRACE'
  }
  if ((currentDetail.exportRecordCount || 0) > 0 || hasDisplayValue(currentDetail.reportId)) {
    return 'REPORT_WRITEBACK'
  }
  if (currentDetail.degraded === true) {
    return 'DEGRADED_RECOVERY'
  }
  if ((currentDetail.nonSuccessEventCount || 0) > 0) {
    return 'FAILURE_CHAIN'
  }
  return 'STEADY_STATE'
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
      requestPrefix: 'frontend-repair-evidence-trace-detail'
    })
  } catch (error) {
    detail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingDetail.value = false
  }
}

const normalizeFilters = () => ({
  traceId: form.traceId,
  taskId: form.taskId,
  reportId: form.reportId
})

const applyLookupPage = async (pageResponse, append = false) => {
  const items = Array.isArray(pageResponse?.items) ? pageResponse.items : []
  lookupResults.value = append ? [...lookupResults.value, ...items] : items
  hasMore.value = Boolean(pageResponse?.hasMore)
  nextCursor.value = pageResponse?.nextCursor || ''

  if (append) {
    return
  }

  await loadTraceDetail(lookupResults.value[0]?.traceId || '')
}

const runLookup = async () => {
  if (!hasDisplayValue(form.traceId) && !hasDisplayValue(form.taskId) && !hasDisplayValue(form.reportId)) {
    errorMessage.value = isChinese.value
      ? '至少输入 traceId、taskId、reportId 中的一项后再执行反查。'
      : 'Enter at least one of traceId, taskId, or reportId before running the lookup.'
    lookupResults.value = []
    detail.value = null
    return
  }

  loadingLookup.value = true
  errorMessage.value = ''
  activeFilters.value = normalizeFilters()

  try {
    const lookupPage = await lookupGovernanceTraces(
      form.tenantId,
      activeFilters.value,
      form.limit,
      {
        requestPrefix: 'frontend-repair-evidence-lookups'
      }
    )
    await applyLookupPage(lookupPage, false)
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
        requestPrefix: 'frontend-repair-evidence-lookups-more'
      }
    )
    await applyLookupPage(lookupPage, true)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingLookup.value = false
  }
}

const clearLookup = () => {
  form.traceId = ''
  form.taskId = ''
  form.reportId = ''
  lookupResults.value = []
  detail.value = null
  errorMessage.value = ''
  hasMore.value = false
  nextCursor.value = ''
  activeFilters.value = null
}

const buildTroubleshootingQuery = source => {
  const query = {
    tenantId: form.tenantId,
    remediationTenantId: 'system',
    limit: String(form.limit)
  }

  const filters = {
    traceId: form.traceId || source?.traceId,
    taskId: form.taskId || source?.taskId,
    reportId: form.reportId || source?.reportId
  }

  if (hasDisplayValue(filters.traceId)) {
    query.traceId = String(filters.traceId).trim()
  }
  if (hasDisplayValue(filters.taskId)) {
    query.taskId = String(filters.taskId).trim()
  }
  if (hasDisplayValue(filters.reportId)) {
    query.reportId = String(filters.reportId).trim()
  }

  return query
}

const openTroubleshooting = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: '/audit-troubleshooting',
    query: buildTroubleshootingQuery(source)
  })
}

const eventHighlights = event => {
  const request = event?.requestParams || {}
  const response = event?.responseSummary || {}
  return [
    {
      label: isChinese.value ? '任务' : 'Task',
      value: response.taskId || request.taskId
    },
    {
      label: isChinese.value ? '报告' : 'Report',
      value: response.reportId || request.reportId
    },
    {
      label: isChinese.value ? '指纹' : 'Fingerprint',
      value: request.sqlFingerprint
    },
    {
      label: isChinese.value ? '错误码' : 'Error',
      value: response.errorCode
    },
    {
      label: isChinese.value ? '目标引擎' : 'Engine',
      value: response.targetEngine
    },
    {
      label: isChinese.value ? '降级' : 'Degraded',
      value: typeof response.degraded === 'boolean' ? String(response.degraded) : ''
    }
  ].filter(item => displayValue(item.value) !== '-')
}

onMounted(async () => {
  if (hasDisplayValue(route.query.tenantId)) {
    form.tenantId = String(route.query.tenantId)
  }
  if (hasDisplayValue(route.query.limit)) {
    form.limit = Number(route.query.limit) || 12
  }
  if (hasDisplayValue(route.query.traceId)) {
    form.traceId = String(route.query.traceId)
  }
  if (hasDisplayValue(route.query.taskId)) {
    form.taskId = String(route.query.taskId)
  }
  if (hasDisplayValue(route.query.reportId)) {
    form.reportId = String(route.query.reportId)
  }
  if (form.traceId || form.taskId || form.reportId) {
    await runLookup()
  }
})
</script>

<template>
  <section class="runtime-page" data-testid="repair-evidence-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('repairEvidence.title') }}</h1>
        <p class="runtime-summary">{{ t('repairEvidence.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页直接调用 governance 历史反查接口，按 trace、task、report 汇总失败链、补偿链和报告回写证据，把 browser runtime gate 继续扩到治理追溯门禁。'
            : 'This page calls the live governance reverse-lookup APIs and correlates failure chains, compensation traces, and report write-back evidence by trace, task, and report so the browser runtime gate extends into governance forensics.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">reverse lookup</p>
            <h2 class="section-title">
              {{ isChinese ? '追溯条件与命中结果' : 'Lookup criteria and matched traces' }}
            </h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户上下文' : 'Tenant context' }}</span>
            <el-input v-model="form.tenantId" data-testid="repair-evidence-tenant-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '返回数量' : 'Lookup limit' }}</span>
            <el-input v-model="form.limit" data-testid="repair-evidence-limit" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? 'Trace ID' : 'Trace ID' }}</span>
            <el-input
              v-model="form.traceId"
              data-testid="repair-evidence-trace-id"
              :placeholder="isChinese ? '输入 trace id 反查单条执行链' : 'Enter a trace id to reverse lookup one execution chain'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Task ID' : 'Task ID' }}</span>
            <el-input
              v-model="form.taskId"
              data-testid="repair-evidence-task-id"
              :placeholder="isChinese ? '输入异步任务 id 追补偿证据' : 'Enter an async task id to trace compensation evidence'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Report ID' : 'Report ID' }}</span>
            <el-input
              v-model="form.reportId"
              data-testid="repair-evidence-report-id"
              :placeholder="isChinese ? '输入报告 id 追回写与导出证据' : 'Enter a report id to trace write-back and export evidence'"
            />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="loadingLookup"
            data-testid="repair-evidence-run-lookup"
            @click="runLookup"
          >
            {{ isChinese ? '执行反查' : 'Run reverse lookup' }}
          </el-button>
          <el-button data-testid="repair-evidence-clear-lookup" @click="clearLookup">
            {{ isChinese ? '清空条件' : 'Clear criteria' }}
          </el-button>
          <el-button
            v-if="hasMore"
            :loading="loadingLookup"
            data-testid="repair-evidence-load-more"
            @click="loadMoreResults"
          >
            {{ isChinese ? '加载更早结果' : 'Load older matches' }}
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
                ? '输入 trace / task / report 后执行反查。'
                : 'Enter a trace, task, or report id and then run the lookup.'
            }}
          </span>
        </div>

        <div class="summary-card-grid">
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '命中 trace' : 'Matched traces' }}</span>
            <strong data-testid="repair-evidence-match-count">{{ matchedCount }}</strong>
          </article>
          <article class="summary-card summary-card-warning">
            <span class="summary-card-label">{{ isChinese ? '补偿 trace' : 'Compensation traces' }}</span>
            <strong data-testid="repair-evidence-compensation-count">{{ compensationCount }}</strong>
          </article>
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '报告关联 trace' : 'Report-linked traces' }}</span>
            <strong data-testid="repair-evidence-report-count">{{ reportLinkedCount }}</strong>
          </article>
          <article class="summary-card summary-card-warning">
            <span class="summary-card-label">{{ isChinese ? '异常/修复链' : 'Non-success chains' }}</span>
            <strong data-testid="repair-evidence-non-success-count">{{ nonSuccessCount }}</strong>
          </article>
        </div>

        <div
          v-if="hasMore"
          class="result-banner result-banner-warning"
          data-testid="repair-evidence-has-more"
        >
          <strong>{{ isChinese ? '仍有更早 trace' : 'Older traces available' }}</strong>
          <span>{{ nextCursor || '-' }}</span>
        </div>

        <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="repair-evidence-error">
          {{ errorMessage }}
        </div>

        <p v-else-if="!lookupResults.length" class="empty-state">
          {{
            isChinese
              ? '命中结果会展示对应 trace 列表，并允许你下钻具体审计/修复时间线。'
              : 'Matched traces render here so you can drill into the linked audit and repair timeline.'
          }}
        </p>

        <div class="trace-list">
          <button
            v-for="trace in lookupResults"
            :key="trace.traceId"
            type="button"
            class="trace-item"
            :class="{ 'trace-item-active': activeTraceId === trace.traceId }"
            data-testid="repair-evidence-result-item"
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
            <div class="trace-item-foot">
              <span>{{ isChinese ? '审计事件' : 'Audit events' }}: {{ trace.auditEventCount }}</span>
              <span>{{ isChinese ? '任务' : 'Task' }}: {{ displayValue(trace.taskId) }}</span>
              <span>{{ isChinese ? '报告' : 'Report' }}: {{ displayValue(trace.reportId) }}</span>
            </div>
            <div class="trace-item-tags">
              <span
                v-if="isCompensationTrace(trace.traceId)"
                class="timeline-meta-pill"
                data-testid="repair-evidence-compensation-pill"
              >
                {{ isChinese ? '补偿 trace' : 'Compensation trace' }}
              </span>
              <span v-if="hasDisplayValue(trace.reportId)" class="timeline-meta-pill">
                {{ isChinese ? '报告回写' : 'Report write-back' }}
              </span>
              <span v-if="trace.degraded" class="timeline-meta-pill">
                {{ isChinese ? '降级恢复' : 'Degraded recovery' }}
              </span>
            </div>
          </button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">repair detail</p>
            <h2 class="section-title">
              {{ isChinese ? '补偿与修复证据明细' : 'Compensation and repair detail' }}
            </h2>
          </div>
        </div>

        <p v-if="!detail && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '选择左侧命中 trace 后，这里会显示命中维度、修复信号和审计时间线。'
              : 'After you select a matched trace, the lookup mode, repair signals, and audit timeline render here.'
          }}
        </p>

        <template v-if="detail">
          <div
            class="result-banner"
            :class="
              detail.latestStatus === 'SUCCESS' || detail.latestStatus === 'SUCCEEDED'
                ? 'result-banner-success'
                : 'result-banner-warning'
            "
          >
            <strong data-testid="repair-evidence-detail-trace-id">{{ detail.traceId }}</strong>
            <span data-testid="repair-evidence-detail-status">{{ detail.latestStatus || '-' }}</span>
          </div>

          <div class="action-row action-row-wrap">
            <el-button
              type="primary"
              data-testid="repair-evidence-open-troubleshooting"
              @click="openTroubleshooting"
            >
              {{ isChinese ? '打开处置决策' : 'Open remediation decision' }}
            </el-button>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '服务编码' : 'Service code' }}</span>
              <strong data-testid="repair-evidence-detail-service-code">{{ detail.serviceCode || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源标识' : 'Resource id' }}</span>
              <strong>{{ detail.resourceId || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '最后发生时间' : 'Last seen at' }}</span>
              <strong>{{ formatTimestamp(detail.lastSeenAt) }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '异常事件数' : 'Non-success events' }}</span>
              <strong>{{ detail.nonSuccessEventCount || 0 }}</strong>
            </div>
          </div>

          <div class="highlight-grid">
            <div
              v-for="item in selectedSignals"
              :key="item.key"
              class="highlight-chip"
            >
              <span>{{ item.label }}</span>
              <strong :data-testid="`repair-evidence-detail-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                {{ displayValue(item.value) }}
              </strong>
            </div>
            <div
              v-for="item in detailHighlights"
              :key="item.key"
              class="highlight-chip"
            >
              <span>{{ item.label }}</span>
              <strong :data-testid="`repair-evidence-detail-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                {{ displayValue(item.value) }}
              </strong>
            </div>
          </div>

          <div class="timeline-list">
            <article
              v-for="event in detail.auditEvents"
              :key="event.id"
              class="timeline-card"
              data-testid="repair-evidence-audit-event"
            >
              <div class="timeline-card-header">
                <div>
                  <p class="timeline-card-id sqlforge-code-label">{{ event.serviceCode }}</p>
                  <h3>{{ event.operationType }} · {{ event.targetId }}</h3>
                </div>
                <span
                  class="trace-status-pill"
                  :class="
                    event.status === 'SUCCESS' || event.status === 'SUCCEEDED'
                      ? 'trace-status-success'
                      : 'trace-status-warning'
                  "
                >
                  {{ event.status }}
                </span>
              </div>
              <p class="timeline-card-line">
                {{ isChinese ? '请求链路' : 'Request chain' }}:
                {{ event.requestId }} / {{ event.traceId }}
              </p>
              <p class="timeline-card-line timeline-card-line-muted">
                {{ formatTimestamp(event.createTime) }} · {{ event.costMs || 0 }}ms
              </p>
              <div class="timeline-card-meta">
                <span
                  v-for="item in eventHighlights(event)"
                  :key="`${event.id}-${item.label}`"
                  class="timeline-meta-pill"
                >
                  {{ item.label }}: {{ displayValue(item.value) }}
                </span>
              </div>
            </article>
          </div>
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
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.12), transparent 45%);
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
.trace-item-service,
.timeline-card-id {
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
.timeline-card-line,
.timeline-card-line-muted,
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
  grid-template-columns: minmax(320px, 0.95fr) minmax(0, 1.05fr);
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
.evidence-grid,
.summary-card-grid,
.highlight-grid {
  display: grid;
  gap: 14px;
}

.form-grid,
.evidence-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card-grid,
.highlight-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block,
.evidence-item,
.summary-card,
.highlight-chip,
.timeline-card,
.trace-item {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: span 2;
}

.field-label,
.evidence-label,
.summary-card-label,
.highlight-chip span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.action-row-wrap {
  flex-wrap: wrap;
}

.lookup-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.lookup-chip,
.timeline-meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
  font-size: 12px;
  font-weight: 600;
}

.lookup-chip-muted {
  background: rgba(241, 245, 249, 0.9);
}

.summary-card,
.highlight-chip {
  padding: 16px 18px;
}

.summary-card strong,
.highlight-chip strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  word-break: break-word;
}

.summary-card-warning {
  background: rgba(254, 243, 199, 0.72);
}

.trace-list,
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.trace-item {
  width: 100%;
  padding: 16px 18px;
  text-align: left;
  cursor: pointer;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.trace-item:hover,
.trace-item-active {
  transform: translateY(-1px);
  border-color: rgba(15, 118, 110, 0.35);
  box-shadow: 0 16px 28px rgba(15, 23, 42, 0.08);
}

.trace-item-header,
.timeline-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.trace-item-header h3,
.timeline-card-header h3 {
  margin: 0;
  color: #0f172a;
  word-break: break-word;
}

.trace-item-foot,
.timeline-card-meta,
.trace-item-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 12px;
  font-size: 13px;
  color: #475569;
}

.trace-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.trace-status-success {
  background: rgba(187, 247, 208, 0.8);
  color: #166534;
}

.trace-status-warning {
  background: rgba(254, 215, 170, 0.82);
  color: #9a3412;
}

.timeline-card {
  padding: 18px;
}

.timeline-card-line {
  margin: 10px 0 0;
}

.timeline-card-line-muted {
  margin-top: 6px;
}

.result-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 18px;
  font-weight: 600;
}

.result-banner-success {
  background: rgba(220, 252, 231, 0.92);
  color: #166534;
}

.result-banner-warning {
  background: rgba(255, 237, 213, 0.92);
  color: #9a3412;
}

.result-banner-danger {
  margin-top: 18px;
  background: rgba(254, 226, 226, 0.92);
  color: #991b1b;
}

@media (max-width: 960px) {
  .runtime-hero,
  .runtime-grid,
  .form-grid,
  .evidence-grid,
  .summary-card-grid,
  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .field-block-wide {
    grid-column: span 1;
  }
}
</style>
