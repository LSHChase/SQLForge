<script setup>
const props = defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    required: true
  },
  trend: {
    type: String,
    default: ''
  },
  detail: {
    type: String,
    default: ''
  },
  tone: {
    type: String,
    default: 'neutral'
  },
  testId: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <article class="metric-card" :class="`metric-card-${props.tone}`" :data-testid="props.testId || null">
    <p class="metric-card-label">{{ props.label }}</p>
    <div class="metric-card-value-row">
      <strong class="metric-card-value">
        <slot name="value">{{ props.value }}</slot>
      </strong>
      <span v-if="props.trend" class="metric-card-trend">{{ props.trend }}</span>
    </div>
    <p v-if="props.detail" class="metric-card-detail">{{ props.detail }}</p>
    <slot />
  </article>
</template>

<style scoped>
.metric-card {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
  min-width: 0;
  padding: 18px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-2);
}

.metric-card-danger {
  border-color: rgba(212, 96, 96, 0.35);
}

.metric-card-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.metric-card-success {
  border-color: var(--sqlforge-color-brand-border);
}

.metric-card-label,
.metric-card-detail {
  margin: 0;
}

.metric-card-label {
  color: var(--sqlforge-text-muted);
}

.metric-card-value-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sqlforge-space-3);
}

.metric-card-value {
  color: var(--sqlforge-text-primary);
  font-size: 34px;
  font-weight: 400;
  line-height: 1;
}

.metric-card-trend {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-pill);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-meta);
}

.metric-card-detail {
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}
</style>
