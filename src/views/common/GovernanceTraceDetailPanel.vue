<script setup>
import { useI18n } from 'vue-i18n'
import SectionHeader from './SectionHeader.vue'
import {
  displayValue,
  eventIdentity,
  eventOccurredAt,
  formatTraceTimestamp,
  isSuccessfulTraceStatus,
  toKebabCase
} from './governanceTrace'

const props = defineProps({
  detail: {
    type: Object,
    default: null
  },
  prefix: {
    type: String,
    required: true
  },
  eyebrow: {
    type: String,
    default: 'trace detail'
  },
  titleKey: {
    type: String,
    required: true
  },
  emptyKey: {
    type: String,
    required: true
  },
  highlightItems: {
    type: Array,
    default: () => []
  },
  signalItems: {
    type: Array,
    default: () => []
  },
  eventHighlightFn: {
    type: Function,
    default: () => []
  },
  bannerText: {
    type: String,
    default: ''
  },
  bannerSuccess: {
    type: Boolean,
    default: false
  }
})

const { t } = useI18n()
</script>

<template>
  <article class="trace-detail-panel">
    <SectionHeader :eyebrow="props.eyebrow" :title="t(props.titleKey)" size="compact" />

    <p v-if="!props.detail" class="empty-state">
      {{ t(props.emptyKey) }}
    </p>

    <template v-if="props.detail">
      <div
        class="result-banner"
        :class="props.bannerSuccess || isSuccessfulTraceStatus(props.detail.latestStatus) ? 'result-banner-success' : 'result-banner-warning'"
      >
        <strong :data-testid="`${props.prefix}-detail-trace-id`">{{ props.detail.traceId }}</strong>
        <span :data-testid="`${props.prefix}-detail-status`">{{ props.bannerText || props.detail.latestStatus || '-' }}</span>
      </div>

      <div v-if="$slots.actions" class="action-row action-row-wrap">
        <slot name="actions" />
      </div>

      <div class="evidence-grid">
        <div class="evidence-item">
          <span>{{ t('governanceTrace.serviceCode') }}</span>
          <strong :data-testid="`${props.prefix}-detail-service-code`">{{ props.detail.serviceCode || '-' }}</strong>
        </div>
        <div class="evidence-item">
          <span>{{ t('governanceTrace.resourceType') }}</span>
          <strong>{{ props.detail.resourceType || '-' }}</strong>
        </div>
        <div class="evidence-item">
          <span>{{ t('governanceTrace.resourceId') }}</span>
          <strong>{{ props.detail.resourceId || '-' }}</strong>
        </div>
        <div class="evidence-item">
          <span>{{ t('governanceTrace.lastSeenAt') }}</span>
          <strong>{{ formatTraceTimestamp(props.detail.lastSeenAt) }}</strong>
        </div>
      </div>

      <div class="highlight-grid">
        <div
          v-for="item in props.signalItems"
          :key="item.key"
          class="highlight-chip highlight-chip-strong"
        >
          <span>{{ item.label }}</span>
          <strong :data-testid="`${props.prefix}-detail-${toKebabCase(item.key)}`">
            {{ displayValue(item.value) }}
          </strong>
        </div>
        <div
          v-for="item in props.highlightItems"
          :key="item.key"
          class="highlight-chip"
        >
          <span>{{ item.label }}</span>
          <strong :data-testid="`${props.prefix}-detail-${toKebabCase(item.key)}`">
            {{ displayValue(item.value) }}
          </strong>
        </div>
      </div>

      <div class="timeline-list">
        <article
          v-for="event in props.detail.auditEvents || []"
          :key="eventIdentity(event)"
          class="timeline-card"
          :data-testid="`${props.prefix}-audit-event`"
        >
          <div class="timeline-card-header">
            <div>
              <p class="timeline-card-id sqlforge-code-label">{{ event.serviceCode || '-' }}</p>
              <h3>{{ event.operationType || event.eventType || '-' }} · {{ event.targetId || event.resourceId || '-' }}</h3>
            </div>
            <span
              class="trace-status-pill"
              :class="isSuccessfulTraceStatus(event.status) ? 'trace-status-success' : 'trace-status-warning'"
            >
              {{ event.status || '-' }}
            </span>
          </div>
          <p class="timeline-card-line">
            {{ t('governanceTrace.requestChain') }}:
            {{ event.requestId || '-' }} / {{ event.traceId || '-' }}
          </p>
          <p class="timeline-card-line timeline-card-line-muted">
            {{ formatTraceTimestamp(eventOccurredAt(event)) }} · {{ event.costMs || 0 }}ms
          </p>
          <div class="timeline-card-meta">
            <span
              v-for="item in props.eventHighlightFn(event)"
              :key="`${eventIdentity(event)}-${item.label}`"
              class="timeline-meta-pill"
            >
              {{ item.label }}: {{ displayValue(item.value) }}
            </span>
          </div>
        </article>
      </div>
    </template>
  </article>
</template>

<style scoped>
.trace-detail-panel {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
  min-width: 0;
  padding: var(--sqlforge-space-6);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-surface-2);
}

.empty-state {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.result-banner {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
  justify-content: space-between;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.result-banner-success {
  border-color: var(--sqlforge-color-brand-border);
}

.result-banner-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.action-row,
.evidence-grid,
.highlight-grid,
.timeline-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
}

.evidence-grid,
.highlight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.evidence-item,
.highlight-chip,
.timeline-card {
  min-width: 0;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.evidence-item,
.highlight-chip {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
}

.evidence-item span,
.highlight-chip span,
.timeline-card-line {
  color: var(--sqlforge-text-secondary);
}

.highlight-chip-strong {
  border-color: var(--sqlforge-color-brand-border);
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
}

.timeline-card-header {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: flex-start;
  justify-content: space-between;
}

.timeline-card-id,
.timeline-card h3,
.timeline-card-line {
  margin: 0;
}

.timeline-card h3 {
  color: var(--sqlforge-text-primary);
  font-size: 16px;
  font-weight: 400;
}

.evidence-item strong,
.highlight-chip strong,
.timeline-card h3,
.timeline-card-line,
.timeline-meta-pill {
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.timeline-card-line {
  margin-top: var(--sqlforge-space-2);
}

.timeline-card-line-muted {
  color: var(--sqlforge-text-muted);
}

.timeline-card-meta {
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

@media (max-width: 820px) {
  .evidence-grid,
  .highlight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .evidence-grid,
  .highlight-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
