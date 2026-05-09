<script setup>
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
  pills: {
    type: Array,
    default: () => []
  },
  variant: {
    type: String,
    default: 'default'
  }
})
</script>

<template>
  <section class="page-hero" :class="`page-hero-${props.variant}`">
    <div class="page-hero-copy">
      <p v-if="props.eyebrow" class="page-hero-eyebrow sqlforge-code-label">{{ props.eyebrow }}</p>
      <h1 class="page-hero-title">{{ props.title }}</h1>
      <p v-if="props.summary" class="page-hero-summary">{{ props.summary }}</p>

      <div v-if="props.pills.length || $slots.pills" class="page-hero-pills">
        <slot name="pills">
          <span v-for="pill in props.pills" :key="pill" class="page-hero-pill">{{ pill }}</span>
        </slot>
      </div>

      <div v-if="$slots.actions" class="page-hero-actions">
        <slot name="actions" />
      </div>
    </div>

    <aside v-if="$slots.aside" class="page-hero-aside">
      <slot name="aside" />
    </aside>
  </section>
</template>

<style scoped>
.page-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(300px, 0.9fr);
  gap: var(--sqlforge-space-6);
  align-items: stretch;
  padding-bottom: var(--sqlforge-space-6);
  border-bottom: 1px solid var(--sqlforge-border-default);
}

.page-hero-copy,
.page-hero-aside {
  min-width: 0;
}

.page-hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--sqlforge-space-4);
}

.page-hero-eyebrow,
.page-hero-title,
.page-hero-summary {
  margin: 0;
}

.page-hero-eyebrow {
  color: var(--sqlforge-text-muted);
}

.page-hero-title {
  max-width: 920px;
  color: var(--sqlforge-text-primary);
  font-size: 56px;
  font-weight: 400;
  line-height: 1;
}

.page-hero-summary {
  max-width: 880px;
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-body);
  line-height: 1.65;
}

.page-hero-pills,
.page-hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
}

.page-hero-pill {
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

.page-hero-aside {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
}

@media (max-width: 1080px) {
  .page-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .page-hero-title {
    font-size: 40px;
  }
}
</style>
