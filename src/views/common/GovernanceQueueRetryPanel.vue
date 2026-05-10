<script setup>
import { useI18n } from 'vue-i18n'
import EvidencePanel from './EvidencePanel.vue'

const props = defineProps({
  queueCards: {
    type: Array,
    default: () => []
  },
  retryResult: {
    type: Object,
    default: null
  },
  failedDelta: {
    type: Number,
    default: 0
  },
  retryImproved: {
    type: Boolean,
    default: false
  },
  prefix: {
    type: String,
    required: true
  },
  titleKey: {
    type: String,
    default: 'governanceTrace.queueImpact'
  }
})

const { t } = useI18n()
</script>

<template>
  <EvidencePanel :title="t(props.titleKey)" eyebrow="queue retry" tone="warning" :test-id="`${props.prefix}-queue-panel`">
    <div class="queue-card-grid">
      <article
        v-for="card in props.queueCards"
        :key="card.key"
        class="queue-card"
      >
        <span>{{ card.label }}</span>
        <strong :data-testid="`${props.prefix}-queue-${card.key}`">{{ card.value }}</strong>
      </article>
    </div>

    <div v-if="props.retryResult" class="retry-summary-grid">
      <div class="queue-card">
        <span>{{ t('governanceTrace.retryStatus') }}</span>
        <strong :data-testid="`${props.prefix}-retry-status`">{{ props.retryResult.status || props.retryResult.mode || '-' }}</strong>
      </div>
      <div class="queue-card">
        <span>{{ t('governanceTrace.retriedCount') }}</span>
        <strong :data-testid="`${props.prefix}-retry-count`">{{ props.retryResult.retriedCount || 0 }}</strong>
      </div>
      <div class="queue-card">
        <span>{{ t('governanceTrace.failedDelta') }}</span>
        <strong :data-testid="`${props.prefix}-failed-delta`">{{ props.failedDelta }}</strong>
      </div>
      <div class="queue-card">
        <span>{{ t('governanceTrace.repairOutcome') }}</span>
        <strong :data-testid="`${props.prefix}-repair-outcome`">
          {{ props.retryImproved ? 'REPAIRED' : 'RETRY_ACCEPTED' }}
        </strong>
      </div>
    </div>
  </EvidencePanel>
</template>

<style scoped>
.queue-card-grid,
.retry-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--sqlforge-space-3);
}

.retry-summary-grid {
  margin-top: var(--sqlforge-space-4);
}

.queue-card {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
}

.queue-card span {
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}
</style>

