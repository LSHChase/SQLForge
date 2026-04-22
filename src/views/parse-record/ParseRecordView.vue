<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getGovernanceTraceDetail,
  getGovernanceTraceSummaries
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  traceId: '',
  limit: 12
})

const loadingList = ref(false)
const loadingDetail = ref(false)
const recentTraces = ref([])
const detail = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
const recentCount = computed(() => recentTraces.value.length)
const nonSuccessCount = computed(() =>
  recentTraces.value.filter(trace => {
    const status = String(trace.latestStatus || '').toUpperCase()
    return (trace.nonSuccessEventCount || 0) > 0 || (status && status !== 'SUCCESS' && status !== 'SUCCEEDED')
  }).length
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

const displayValue = value => {
  if (value === null || value === undefined || value === '') {
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

const loadTraceDetail = async (traceId, synchronizeInput = true) => {
  if (!traceId) {
    detail.value = null
    return
  }

  loadingDetail.value = true
  errorMessage.value = ''
  if (synchronizeInput) {
    form.traceId = traceId
  }

  try {
    detail.value = await getGovernanceTraceDetail(form.tenantId, traceId, 20, {
      requestPrefix: 'frontend-parse-record-trace-detail'
    })
  } catch (error) {
    detail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingDetail.value = false
  }
}

const loadRecentTraces = async (preserveSelectedTrace = false) => {
  loadingList.value = true
  errorMessage.value = ''
  const previousTraceId = preserveSelectedTrace ? detail.value?.traceId : ''

  try {
    const traces = await getGovernanceTraceSummaries(form.tenantId, form.limit, {
      requestPrefix: 'frontend-parse-record-trace-summaries'
    })
    recentTraces.value = Array.isArray(traces) ? traces : []

    const nextTraceId = form.traceId || previousTraceId || recentTraces.value[0]?.traceId || ''
    if (nextTraceId) {
      await loadTraceDetail(nextTraceId, !form.traceId)
    } else {
      detail.value = null
    }
  } catch (error) {
    recentTraces.value = []
    detail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loadingList.value = false
  }
}

const refreshEvidence = async () => {
  await loadRecentTraces(true)
}

const openTraceFromInput = async () => {
  await loadTraceDetail(form.traceId, true)
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

onMounted(() => {
  loadRecentTraces()
})
</script>

<template>
  <section class="runtime-page" data-testid="parse-record-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">frontend runtime gate</p>
        <h1 class="runtime-title">{{ t('parseRecord.title') }}</h1>
        <p class="runtime-summary">{{ t('parseRecord.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页直接读取 governance 审计追溯接口，汇总最近 trace、失败/补偿态与关联事件时间线，把 browser runtime gate 从执行页扩到历史诊断页。'
            : 'This page reads the live governance traceability APIs and renders recent traces, failure-compensation states, and linked audit timelines so the browser gate extends into historical diagnostics.'
        }}
      </p>
    </div>

    <div class="runtime-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">trace controls</p>
            <h2 class="section-title">
              {{ isChinese ? '追溯入口与最近记录' : 'Trace entry and recent records' }}
            </h2>
          </div>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户上下文' : 'Tenant context' }}</span>
            <el-input
              v-model="form.tenantId"
              data-testid="parse-record-tenant-id"
            />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '指定 Trace ID' : 'Trace ID lookup' }}</span>
            <el-input
              v-model="form.traceId"
              data-testid="parse-record-trace-id"
              :placeholder="isChinese ? '输入 trace id 直达审计链' : 'Enter a trace id to jump to one audit chain'"
            />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="loadingList"
            data-testid="parse-record-refresh"
            @click="refreshEvidence"
          >
            {{ isChinese ? '刷新历史证据' : 'Refresh trace evidence' }}
          </el-button>
          <el-button
            :loading="loadingDetail"
            data-testid="parse-record-load-trace"
            @click="openTraceFromInput"
          >
            {{ isChinese ? '按 Trace ID 打开' : 'Open trace detail' }}
          </el-button>
        </div>

        <div class="summary-card-grid">
          <article class="summary-card">
            <span class="summary-card-label">{{ isChinese ? '最近 trace 数' : 'Recent traces' }}</span>
            <strong data-testid="parse-record-recent-count">{{ recentCount }}</strong>
          </article>
          <article class="summary-card summary-card-warning">
            <span class="summary-card-label">{{ isChinese ? '异常/补偿 trace' : 'Non-success traces' }}</span>
            <strong data-testid="parse-record-non-success-count">{{ nonSuccessCount }}</strong>
          </article>
        </div>

        <p
          v-if="!recentTraces.length && !errorMessage"
          class="empty-state"
        >
          {{
            isChinese
              ? '刷新后会显示最近的 query / optimization / benchmark 治理追溯记录。'
              : 'Refresh to load the latest query, optimization, and benchmark governance traces.'
          }}
        </p>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="parse-record-error"
        >
          {{ errorMessage }}
        </div>

        <div class="trace-list">
          <button
            v-for="trace in recentTraces"
            :key="trace.traceId"
            type="button"
            class="trace-item"
            :class="{ 'trace-item-active': detail?.traceId === trace.traceId }"
            data-testid="parse-record-trace-item"
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
              {{ trace.operationType || '-' }} · {{ formatTimestamp(trace.lastSeenAt) }}
            </p>
            <div class="trace-item-foot">
              <span>{{ isChinese ? '审计事件' : 'Audit events' }}: {{ trace.auditEventCount }}</span>
              <span>{{ isChinese ? '历史记录' : 'History' }}: {{ trace.queryHistoryCount }}</span>
              <span>{{ isChinese ? '导出记录' : 'Exports' }}: {{ trace.exportRecordCount }}</span>
            </div>
          </button>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">trace detail</p>
            <h2 class="section-title">
              {{ isChinese ? '历史诊断与审计时间线' : 'Historical diagnosis and audit timeline' }}
            </h2>
          </div>
        </div>

        <p
          v-if="!detail && !errorMessage"
          class="empty-state"
        >
          {{
            isChinese
              ? '选择左侧 trace 后，这里会显示审计事件、关联任务/报告与历史链路摘要。'
              : 'After you select a trace, the linked audit events, task/report references, and history chain summary render here.'
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
            <strong data-testid="parse-record-detail-trace-id">{{ detail.traceId }}</strong>
            <span data-testid="parse-record-detail-status">{{ detail.latestStatus || '-' }}</span>
          </div>

          <div class="evidence-grid">
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '服务编码' : 'Service code' }}</span>
              <strong data-testid="parse-record-detail-service-code">{{ detail.serviceCode || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源类型' : 'Resource type' }}</span>
              <strong>{{ detail.resourceType || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '资源标识' : 'Resource id' }}</span>
              <strong data-testid="parse-record-detail-resource-id">{{ detail.resourceId || '-' }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '最后发生时间' : 'Last seen at' }}</span>
              <strong>{{ formatTimestamp(detail.lastSeenAt) }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '审计事件数' : 'Audit events' }}</span>
              <strong data-testid="parse-record-detail-audit-count">{{ detail.auditEventCount || 0 }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '异常事件数' : 'Non-success events' }}</span>
              <strong>{{ detail.nonSuccessEventCount || 0 }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '历史记录数' : 'History records' }}</span>
              <strong data-testid="parse-record-detail-query-history-count">{{ detail.queryHistoryCount || 0 }}</strong>
            </div>
            <div class="evidence-item">
              <span class="evidence-label">{{ isChinese ? '导出记录数' : 'Export records' }}</span>
              <strong data-testid="parse-record-detail-export-count">{{ detail.exportRecordCount || 0 }}</strong>
            </div>
          </div>

          <div class="highlight-grid">
            <div
              v-for="item in detailHighlights"
              :key="item.key"
              class="highlight-chip"
            >
              <span>{{ item.label }}</span>
              <strong :data-testid="`parse-record-detail-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                {{ displayValue(item.value) }}
              </strong>
            </div>
          </div>

          <div class="timeline-list">
            <article
              v-for="event in detail.auditEvents"
              :key="event.id"
              class="timeline-card"
              data-testid="parse-record-audit-event"
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
  grid-template-columns: minmax(320px, 0.92fr) minmax(0, 1.08fr);
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
.timeline-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 12px;
  font-size: 13px;
  color: #475569;
}

.trace-status-pill,
.timeline-meta-pill {
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

.timeline-meta-pill {
  background: rgba(226, 232, 240, 0.9);
  color: #334155;
  font-weight: 600;
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
