<script setup>
import SectionHeader from './SectionHeader.vue'

const props = defineProps({
  as: {
    type: String,
    default: 'section'
  },
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    default: ''
  },
  summary: {
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
  <component
    :is="props.as"
    class="evidence-panel"
    :class="`evidence-panel-${props.tone}`"
    :data-testid="props.testId || null"
  >
    <slot name="header">
      <SectionHeader
        v-if="props.title"
        :eyebrow="props.eyebrow"
        :title="props.title"
        :summary="props.summary"
        size="compact"
      >
        <template v-if="$slots.actions" #actions>
          <slot name="actions" />
        </template>
      </SectionHeader>
    </slot>

    <div v-if="$slots.default" class="evidence-panel-body">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="evidence-panel-footer">
      <slot name="footer" />
    </footer>
  </component>
</template>

<style scoped>
.evidence-panel {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
  min-width: 0;
  padding: var(--sqlforge-space-6);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-lg);
  background: var(--sqlforge-surface-2);
}

.evidence-panel-success {
  border-color: var(--sqlforge-color-brand-border);
}

.evidence-panel-warning {
  border-color: rgba(207, 166, 62, 0.32);
}

.evidence-panel-danger {
  border-color: rgba(212, 96, 96, 0.35);
}

.evidence-panel-body {
  min-width: 0;
}

.evidence-panel-footer {
  padding-top: var(--sqlforge-space-4);
  border-top: 1px solid var(--sqlforge-border-subtle);
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}
</style>
