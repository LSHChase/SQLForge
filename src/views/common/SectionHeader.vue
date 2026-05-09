<script setup>
import { computed } from 'vue'

const props = defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    required: true
  },
  summary: {
    type: String,
    default: ''
  },
  level: {
    type: Number,
    default: 2
  },
  size: {
    type: String,
    default: 'default'
  }
})

const headingTag = computed(() => `h${Math.min(Math.max(Number(props.level) || 2, 1), 6)}`)
</script>

<template>
  <div class="section-header" :class="`section-header-${props.size}`">
    <div class="section-header-copy">
      <p v-if="props.eyebrow" class="section-header-eyebrow sqlforge-code-label">{{ props.eyebrow }}</p>
      <component :is="headingTag" class="section-header-title">{{ props.title }}</component>
      <p v-if="props.summary" class="section-header-summary">{{ props.summary }}</p>
    </div>

    <div v-if="$slots.actions" class="section-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sqlforge-space-4);
}

.section-header-copy {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
}

.section-header-eyebrow,
.section-header-title,
.section-header-summary {
  margin: 0;
}

.section-header-eyebrow {
  color: var(--sqlforge-text-muted);
}

.section-header-title {
  color: var(--sqlforge-text-primary);
  font-size: 28px;
  font-weight: 400;
  line-height: 1.12;
}

.section-header-compact .section-header-title {
  font-size: 22px;
}

.section-header-summary {
  max-width: 760px;
  color: var(--sqlforge-text-secondary);
  line-height: 1.65;
}

.section-header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--sqlforge-space-3);
}

@media (max-width: 760px) {
  .section-header {
    flex-direction: column;
  }

  .section-header-actions {
    justify-content: flex-start;
  }
}
</style>
