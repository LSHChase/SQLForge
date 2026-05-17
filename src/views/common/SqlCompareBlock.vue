<script setup>
import { computed } from 'vue'
import { buildSqlCompareRows } from './sqlCompare.mjs'

const props = defineProps({
  originalSql: {
    type: String,
    default: ''
  },
  recommendedSql: {
    type: String,
    default: ''
  },
  originalLabel: {
    type: String,
    default: 'Original SQL'
  },
  recommendedLabel: {
    type: String,
    default: 'Recommended SQL'
  },
  emptyText: {
    type: String,
    default: '-'
  }
})

const compareRows = computed(() => buildSqlCompareRows(props.originalSql, props.recommendedSql))
</script>

<template>
  <section class="sql-compare-block">
    <div class="sql-compare-block__labels">
      <span>{{ originalLabel }}</span>
      <span>{{ recommendedLabel }}</span>
    </div>
    <p v-if="!compareRows.length" class="sql-compare-block__empty">{{ emptyText }}</p>
    <div v-else class="sql-compare-block__viewport">
      <div class="sql-compare-block__grid sql-compare-block__grid--header">
        <span>#</span>
        <span>{{ originalLabel }}</span>
        <span>#</span>
        <span>{{ recommendedLabel }}</span>
      </div>
      <div
        v-for="row in compareRows"
        :key="row.key"
        class="sql-compare-block__grid sql-compare-row"
        :class="`sql-compare-row--${row.type.toLowerCase()}`"
      >
        <span class="sql-compare-line">{{ row.originalIndex || '' }}</span>
        <!-- eslint-disable-next-line vue/no-v-html -->
        <pre class="sql-compare-code"><code v-html="row.originalHtml" /></pre>
        <span class="sql-compare-line">{{ row.recommendedIndex || '' }}</span>
        <!-- eslint-disable-next-line vue/no-v-html -->
        <pre class="sql-compare-code"><code v-html="row.recommendedHtml" /></pre>
      </div>
    </div>
  </section>
</template>

<style scoped>
.sql-compare-block {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-3);
  min-width: 0;
}

.sql-compare-block__labels,
.sql-compare-block__grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--sqlforge-space-3);
  min-width: 0;
}

.sql-compare-block__labels {
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.sql-compare-block__empty {
  margin: 0;
  color: var(--sqlforge-text-secondary);
}

.sql-compare-block__viewport {
  max-height: 520px;
  overflow: auto;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
}

.sql-compare-block__grid {
  grid-template-columns: 54px minmax(360px, 1fr) 54px minmax(360px, 1fr);
  gap: 0;
  min-width: 880px;
}

.sql-compare-block__grid--header {
  position: sticky;
  top: 0;
  z-index: 1;
  border-bottom: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  font-weight: 700;
}

.sql-compare-block__grid--header span,
.sql-compare-line {
  padding: 8px 10px;
}

.sql-compare-row {
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.sql-compare-row:last-child {
  border-bottom: 0;
}

.sql-compare-line {
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  text-align: right;
  user-select: none;
}

.sql-compare-code {
  min-height: 34px;
  margin: 0;
  padding: 8px 12px;
  border-left: 1px solid var(--sqlforge-border-subtle);
  color: var(--sqlforge-text-primary);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre;
}

.sql-compare-code:nth-child(4) {
  border-left-color: var(--sqlforge-border-default);
  box-shadow: inset 10px 0 0 rgba(255, 255, 255, 0.025);
}

.sql-compare-row--delete .sql-compare-line:first-child,
.sql-compare-row--delete .sql-compare-code:nth-child(2),
.sql-compare-row--replace .sql-compare-line:first-child,
.sql-compare-row--replace .sql-compare-code:nth-child(2) {
  background: rgba(120, 28, 28, 0.2);
}

.sql-compare-row--insert .sql-compare-line:nth-child(3),
.sql-compare-row--insert .sql-compare-code:nth-child(4),
.sql-compare-row--replace .sql-compare-line:nth-child(3),
.sql-compare-row--replace .sql-compare-code:nth-child(4) {
  background: rgba(21, 98, 73, 0.22);
}

:deep(.sql-token-keyword) {
  color: var(--sqlforge-color-brand);
  font-weight: 800;
}

:deep(.sql-token-identifier) {
  color: #d7d7d7;
}

:deep(.sql-token-literal) {
  color: #f0b86e;
  font-weight: 600;
}

:deep(.sql-token-number) {
  color: #9bc8ff;
  font-weight: 700;
}

:deep(.sql-token-comment) {
  color: #7f8a8a;
  font-style: italic;
}

:deep(.sql-token-operator) {
  color: #d6a7ff;
  font-weight: 700;
}

:deep(.sql-compare-token-mark) {
  display: inline-block;
  min-width: 0.7em;
  padding: 0 3px;
  border-radius: 4px;
  outline: 1px solid transparent;
}

:deep(.sql-compare-token-mark--delete) {
  background: rgba(238, 90, 90, 0.3);
  outline-color: rgba(238, 90, 90, 0.45);
}

:deep(.sql-compare-token-mark--insert) {
  background: rgba(59, 196, 139, 0.28);
  outline-color: rgba(59, 196, 139, 0.42);
}

@media (max-width: 760px) {
  .sql-compare-block__labels {
    display: none;
  }

  .sql-compare-block__grid {
    grid-template-columns: 44px minmax(300px, 1fr) 44px minmax(300px, 1fr);
    min-width: 700px;
  }
}
</style>
