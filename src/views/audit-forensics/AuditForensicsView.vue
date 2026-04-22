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
const repairSignalCount = computed(() =>
  lookupResults.value.filter(trace => resolveTraceRepairSignal(trace) !== 'STEADY_STATE').length
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
  return [
    {
      key: 'lookupMode',
      label: isChinese.value ? '命中维度' : 'Lookup match',
      value: resolveLookupMode(detail.value)
    },
    {
      key: 'repairSignal',
      label: isChinese.value ? '修复信号' : 'Repair signal',
      value: resolveRepairSignal(detail.value)
    },
    {
      key: 'compensationTrace',
      label: isChinese.value ? '补偿链路' : 'Compensation trace',
      value: isCompensationTrace(detail.value.traceId) ? 'true' : 'false'
    },
    {
      key: 'auditEvents',
      label: isChinese.value ? '审计事件' : 'Audit events',
      value: detail.value.auditEventCount
    }
  ].filter(item => displayValue(item.value) !== '-')
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

const resolveTraceRepairSignal = trace => {
  if (isCompensationTrace(trace.traceId)) {
    return 'COMPENSATION_TRACE'
  }
  if ((trace.exportRecordCount || 0) > 0 || hasDisplayValue(trace.reportId)) {
    return 'REPORT_WRITEBACK'
  }
  if (trace.degraded === true) {
    return 'DEGRADED_RECOVERY'
  }
  const status = String(trace.latestStatus || '').toUpperCase()
  if ((trace.nonSuccessEventCount || 0) > 0 || (status && status !== 'SUCCESS' && status !== 'SUCCEEDED')) {
    return 'FAILURE_CHAIN'
  }
  return 'STEADY_STATE'
}

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

const normalizeFilters = () => ({
  traceId: String(form.traceId || '').trim(),
  taskId: String(form.taskId || '').trim(),
  reportId: String(form.reportId || '').trim()
})

const syncRouteQuery = query => {
  router.replace({
    path: '/audit-forensics',
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
  return query
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
      requestPrefix: 'frontend-audit-forensics-trace-detail'
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
      ? '至少输入 traceId、taskId、reportId 中的一项后再执行取证反查。'
      : 'Enter at least one of traceId, taskId, or reportId before running the forensic lookup.'
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
    limit: String(form.limit),
    ...activeFilters.value
  })

  try {
    const lookupPage = await lookupGovernanceTraces(form.tenantId, activeFilters.value, form.limit, {
      requestPrefix: 'frontend-audit-forensics-lookups'
    })
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
        requestPrefix: 'frontend-audit-forensics-lookups-more'
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
  syncRouteQuery({
    tenantId: form.tenantId,
    limit: String(form.limit)
  })
}

const openParseRecord = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: '/parse-record',
    query: buildDrillQuery(source)
  })
}

const openRepairEvidence = () => {
  const source = detail.value || selectedSummary.value
  if (!source) {
    return
  }
  router.push({
    path: '/repair-evidence',
    query: buildDrillQuery(source)
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
  form.traceId = String(route.query.traceId || '')
  form.taskId = String(route.query.taskId || '')
  form.reportId = String(route.query.reportId || '')

  if (form.traceId || form.taskId || form.reportId) {
    await runLookup()
  }
})
</script>

<template>
  <section class="runtime-page" data-testid="audit-forensics-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('auditForensics.title') }}</h1>
        <p class="runtime-summary">{{ t('auditForensics.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页把 trace、task、report 命中的失败链、补偿链、报告回写和审计事件串成可分页取证链，并支持在 parse-record 与 repair-evidence 之间做双向钻取。'
            : 'This page stitches failure chains, compensation traces, report write-back, and audit events into a paged forensic chain across trace, task, and report lookups, with drill-through into parse-record and repair-evidence.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">forensic lookup</p>
            <h2 class="section-title">
              {{ isChinese ? '取证条件与证据链命中' : 'Forensic criteria and matched evidence chains' }}
            </h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户上下文' : 'Tenant context' }}</span>
            <el-input v-model="form.tenantId" data-testid="audit-forensics-tenant-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '返回数量' : 'Lookup limit' }}</span>
            <el-input v-model="form.limit" data-testid="audit-forensics-limit" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? 'Trace ID' : 'Trace ID' }}</span>
            <el-input v-model="form.traceId" data-testid="audit-forensics-trace-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Task ID' : 'Task ID' }}</span>
            <el-input v-model="form.taskId" data-testid="audit-forensics-task-id" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? 'Report ID' : 'Report ID' }}</span>
            <el-input v-model="form.reportId" data-testid="audit-forensics-report-id" />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="loadingLookup"
            data-testid="audit-forensics-run-lookup"
            @click="runLookup"
          >
            {{ isChinese ? '执行取证反查' : 'Run forensic lookup' }}
          </el-button>
          <el-button data-testid="audit-forensics-clear-lookup" @click="clearLookup">
            {{ isChinese ? '清空条件' : 'Clear criteria' }}
          </el-button>
          <el-button
            v-if="hasMore"
            :loading="loadingLookup"
            data-testid="audit-forensics-load-more"
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
                ? '输入 trace / task / report 后执行取证反查。'
                : 'Enter a trace, task, or report id and then run the forensic lookup.'
            }}
          </span>
        </div>

        <div class="summary-card-grid">
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '命中 trace' : 'Matched traces' }}</span>
            <strong data-testid="audit-forensics-match-count">{{ matchedCount }}</strong>
          </article>
          <article class="summary-card summary-card-warning">
            <span class="summary-card-label">{{ isChinese ? '补偿 trace' : 'Compensation traces' }}</span>
            <strong data-testid="audit-forensics-compensation-count">{{ compensationCount }}</strong>
          </article>
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '修复信号链' : 'Repair signal chains' }}</span>
            <strong data-testid="audit-forensics-repair-count">{{ repairSignalCount }}</strong>
          </article>
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '报告回写链' : 'Report-linked traces' }}</span>
            <strong data-testid="audit-forensics-report-count">{{ reportLinkedCount }}</strong>
          </article>
        </div>

        <div
          v-if="hasMore"
          class="result-banner result-banner-warning"
          data-testid="audit-forensics-has-more"
        >
          <strong>{{ isChinese ? '仍有更早证据链' : 'Older evidence chains available' }}</strong>
          <span>{{ nextCursor || '-' }}</span>
        </div>

        <div v-if="errorMessage" class="result-banner result-banner-danger" data-testid="audit-forensics-error">
          {{ errorMessage }}
        </div>

        <p v-else-if="!lookupResults.length" class="empty-state">
          {{
            isChinese
              ? '命中结果会展示失败链、补偿链与报告回写证据，并支持跳回历史诊断页继续下钻。'
              : 'Matched chains render here with failure, compensation, and report write-back evidence, plus drill-through back into the history diagnosis page.'
          }}
        </p>

        <div class="trace-list">
          <button
            v-for="trace in lookupResults"
            :key="trace.traceId"
            type="button"
            class="trace-item"
            :class="{ 'trace-item-active': activeTraceId === trace.traceId }"
            data-testid="audit-forensics-result-item"
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
            <div class="trace-item-tags">
              <span
                v-if="isCompensationTrace(trace.traceId)"
                class="timeline-meta-pill"
                data-testid="audit-forensics-compensation-pill"
              >
                {{ isChinese ? '补偿 trace' : 'Compensation trace' }}
              </span>
              <span class="timeline-meta-pill">
                {{ resolveTraceRepairSignal(trace) }}
              </span>
              <span class="timeline-meta-pill">
                {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(trace.taskId) }}
              </span>
              <span class="timeline-meta-pill">
                {{ isChinese ? '报告' : 'Report' }}: {{ displayValue(trace.reportId) }}
              </span>
            </div>
          </button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">forensic pivots</p>
            <h2 class="section-title">
              {{ isChinese ? '审计取证详情与跨页 pivot' : 'Forensic detail and cross-page pivots' }}
            </h2>
          </div>
        </div>

        <p v-if="!detail && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '选择左侧命中 trace 后，这里会显示取证信号、历史事件和跨页跳转动作。'
              : 'After you select a matched trace, the forensic signals, linked history events, and cross-page actions render here.'
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
            <strong data-testid="audit-forensics-detail-trace-id">{{ detail.traceId }}</strong>
            <span data-testid="audit-forensics-detail-status">{{ detail.latestStatus || '-' }}</span>
          </div>

          <div class="action-row action-row-wrap">
            <el-button
              type="primary"
              data-testid="audit-forensics-open-parse-record"
              @click="openParseRecord"
            >
              {{ isChinese ? '跳回历史诊断' : 'Open parse record' }}
            </el-button>
            <el-button
              data-testid="audit-forensics-open-repair-evidence"
              @click="openRepairEvidence"
            >
              {{ isChinese ? '打开修复证据' : 'Open repair evidence' }}
            </el-button>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '服务编码' : 'Service code' }}</span>
              <strong data-testid="audit-forensics-detail-service-code">{{ detail.serviceCode || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源类型' : 'Resource type' }}</span>
              <strong>{{ detail.resourceType || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源标识' : 'Resource id' }}</span>
              <strong>{{ detail.resourceId || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '最后发生时间' : 'Last seen at' }}</span>
              <strong>{{ formatTimestamp(detail.lastSeenAt) }}</strong>
            </div>
          </div>

          <div class="highlight-grid">
            <div
              v-for="item in detailHighlights"
              :key="item.key"
              class="highlight-chip"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
            <div
              v-for="item in selectedSignals"
              :key="item.key"
              class="highlight-chip highlight-chip-strong"
            >
              <span>{{ item.label }}</span>
              <strong :data-testid="`audit-forensics-detail-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                {{ displayValue(item.value) }}
              </strong>
            </div>
          </div>

          <div class="timeline-list">
            <article
              v-for="event in detail.auditEvents"
              :key="event.id"
              class="timeline-card"
              data-testid="audit-forensics-audit-event"
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
  grid-template-columns: minmax(320px, 0.94fr) minmax(0, 1.06fr);
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
.summary-card,
.evidence-item,
.highlight-chip,
.trace-item,
.timeline-card {
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
.evidence-label {
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
.evidence-item,
.highlight-chip {
  padding: 16px 18px;
}

.summary-card,
.highlight-chip {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.summary-card strong,
.evidence-item strong,
.highlight-chip strong {
  font-size: 18px;
  color: #0f172a;
}

.summary-card-warning,
.highlight-chip-strong {
  background: rgba(255, 247, 237, 0.92);
  border-color: rgba(251, 146, 60, 0.18);
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

.trace-list,
.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 18px;
}

.trace-item,
.timeline-card {
  width: 100%;
  text-align: left;
  padding: 16px 18px;
}

.trace-item {
  cursor: pointer;
}

.trace-item-active {
  border-color: rgba(14, 165, 233, 0.4);
  box-shadow: 0 14px 28px rgba(14, 165, 233, 0.12);
}

.trace-item-header,
.timeline-card-header,
.timeline-card-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.trace-item-header,
.timeline-card-header {
  align-items: flex-start;
}

.trace-item-header h3,
.timeline-card-header h3 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
}

.trace-item-meta,
.timeline-card-line {
  margin: 10px 0 0;
}

.trace-item-tags,
.timeline-card-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
