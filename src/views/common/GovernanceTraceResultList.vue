<script setup>
import { useI18n } from 'vue-i18n'
import {
  displayValue,
  formatTraceTimestamp,
  isCompensationTrace,
  isSuccessfulTraceStatus,
  resolveTraceRepairSignal
} from './governanceTrace'

const props = defineProps({
  traces: {
    type: Array,
    default: () => []
  },
  activeTraceId: {
    type: String,
    default: ''
  },
  prefix: {
    type: String,
    required: true
  },
  showAuditFoot: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select'])

const { t } = useI18n()
</script>

<template>
  <div class="trace-list">
    <button
      v-for="trace in props.traces"
      :key="trace.traceId"
      type="button"
      class="trace-item"
      :class="{ 'trace-item-active': props.activeTraceId === trace.traceId }"
      :data-testid="`${props.prefix}-result-item`"
      @click="$emit('select', trace.traceId)"
    >
      <div class="trace-item-header">
        <div>
          <p class="trace-item-service sqlforge-code-label">{{ trace.serviceCode || '-' }}</p>
          <h3>{{ trace.resourceId || trace.traceId }}</h3>
        </div>
        <span
          class="trace-status-pill"
          :class="isSuccessfulTraceStatus(trace.latestStatus) ? 'trace-status-success' : 'trace-status-warning'"
        >
          {{ trace.latestStatus || '-' }}
        </span>
      </div>
      <p class="trace-item-meta">
        {{ trace.traceId }} · {{ formatTraceTimestamp(trace.lastSeenAt) }}
      </p>
      <div v-if="props.showAuditFoot" class="trace-item-foot">
        <span>{{ t('governanceTrace.auditEvents') }}: {{ trace.auditEventCount || 0 }}</span>
        <span>{{ t('governanceTrace.task') }}: {{ displayValue(trace.taskId) }}</span>
        <span>{{ t('governanceTrace.report') }}: {{ displayValue(trace.reportId) }}</span>
      </div>
      <div class="trace-item-tags">
        <span
          v-if="isCompensationTrace(trace.traceId)"
          class="timeline-meta-pill"
          :data-testid="`${props.prefix}-compensation-pill`"
        >
          {{ t('governanceTrace.compensationTrace') }}
        </span>
        <span class="timeline-meta-pill">
          {{ resolveTraceRepairSignal(trace) }}
        </span>
        <span v-if="displayValue(trace.taskId) !== '-'" class="timeline-meta-pill">
          {{ t('governanceTrace.task') }}: {{ displayValue(trace.taskId) }}
        </span>
        <span v-if="displayValue(trace.reportId) !== '-'" class="timeline-meta-pill">
          {{ t('governanceTrace.report') }}: {{ displayValue(trace.reportId) }}
        </span>
        <span v-if="trace.degraded" class="timeline-meta-pill">
          {{ t('governanceTrace.degradedRecovery') }}
        </span>
      </div>
    </button>
  </div>
</template>

<style scoped>
.trace-list {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
}

.trace-item {
  width: 100%;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-2);
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.trace-item-active {
  border-color: var(--sqlforge-color-brand-border);
}

.trace-item-header,
.trace-item-tags,
.trace-item-foot {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: flex-start;
}

.trace-item-header {
  justify-content: space-between;
}

.trace-item-service,
.trace-item h3,
.trace-item-meta {
  margin: 0;
}

.trace-item h3 {
  color: var(--sqlforge-text-primary);
  font-size: 18px;
  font-weight: 400;
  line-height: 1.35;
}

.trace-item-meta,
.trace-item-foot {
  margin-top: var(--sqlforge-space-2);
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.trace-item-tags {
  margin-top: var(--sqlforge-space-3);
}

.trace-status-pill,
.timeline-meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--sqlforge-space-3);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.trace-status-success {
  border-color: var(--sqlforge-color-brand-border);
  color: var(--sqlforge-color-brand);
}

.trace-status-warning {
  border-color: rgba(207, 166, 62, 0.32);
  color: #f6d58f;
}
</style>

