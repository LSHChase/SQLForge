<script setup>
import SectionHeader from './SectionHeader.vue'

const props = defineProps({
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
  density: {
    type: String,
    default: 'default'
  },
  testId: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <section class="toolbar-shell" :class="`toolbar-shell-${props.density}`" :data-testid="props.testId || null">
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

    <div v-else-if="props.eyebrow || $slots.actions" class="toolbar-shell-strip">
      <p v-if="props.eyebrow" class="toolbar-shell-eyebrow sqlforge-code-label">{{ props.eyebrow }}</p>
      <div v-if="$slots.actions" class="toolbar-shell-actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="toolbar-shell-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.toolbar-shell {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
  padding: var(--sqlforge-space-5);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-3);
}

.toolbar-shell-compact {
  padding: var(--sqlforge-space-4);
}

.toolbar-shell-strip,
.toolbar-shell-actions,
.toolbar-shell-body {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
}

.toolbar-shell-strip {
  justify-content: space-between;
}

.toolbar-shell-eyebrow {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.toolbar-shell-body {
  align-items: flex-end;
}
</style>
