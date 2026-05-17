<script setup>
import { computed } from 'vue'
import { formatSqlText, highlightSql } from './sqlFormatting.mjs'

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

const originalLines = computed(() => splitSqlLines(props.originalSql))
const recommendedLines = computed(() => splitSqlLines(props.recommendedSql))
const compareRows = computed(() => buildCompareRows(originalLines.value, recommendedLines.value))

const splitSqlLines = value => {
  const raw = String(value ?? '')
  const formatted = formatSqlText(raw) || raw.trim()
  return formatted ? formatted.split('\n') : []
}

const buildCompareRows = (original, recommended) => {
  if (!original.length && !recommended.length) {
    return []
  }
  const ops = buildDiffOps(original, recommended)
  const rows = []
  let cursor = 0
  while (cursor < ops.length) {
    const op = ops[cursor]
    if (op.type === 'EQUAL') {
      rows.push(rowFromPair('EQUAL', op.originalLine, op.recommendedLine, op.originalIndex, op.recommendedIndex))
      cursor += 1
      continue
    }
    const group = []
    while (cursor < ops.length && ops[cursor].type !== 'EQUAL') {
      group.push(ops[cursor])
      cursor += 1
    }
    rows.push(...coalesceChangeGroup(group))
  }
  return rows.map((row, index) => ({ ...row, key: `${row.type}-${index}` }))
}

const buildDiffOps = (original, recommended) => {
  const dp = Array.from({ length: original.length + 1 }, () => Array(recommended.length + 1).fill(0))
  for (let left = original.length - 1; left >= 0; left -= 1) {
    for (let right = recommended.length - 1; right >= 0; right -= 1) {
      if (original[left] === recommended[right]) {
        dp[left][right] = dp[left + 1][right + 1] + 1
      } else {
        dp[left][right] = Math.max(dp[left + 1][right], dp[left][right + 1])
      }
    }
  }

  const ops = []
  let left = 0
  let right = 0
  while (left < original.length || right < recommended.length) {
    if (left < original.length && right < recommended.length && original[left] === recommended[right]) {
      ops.push({
        type: 'EQUAL',
        originalLine: original[left],
        recommendedLine: recommended[right],
        originalIndex: left + 1,
        recommendedIndex: right + 1
      })
      left += 1
      right += 1
    } else if (right >= recommended.length || (left < original.length && dp[left + 1][right] >= dp[left][right + 1])) {
      ops.push({
        type: 'DELETE',
        originalLine: original[left],
        originalIndex: left + 1
      })
      left += 1
    } else {
      ops.push({
        type: 'INSERT',
        recommendedLine: recommended[right],
        recommendedIndex: right + 1
      })
      right += 1
    }
  }
  return ops
}

const coalesceChangeGroup = group => {
  const deletes = group.filter(item => item.type === 'DELETE')
  const inserts = group.filter(item => item.type === 'INSERT')
  const count = Math.max(deletes.length, inserts.length)
  const rows = []
  for (let index = 0; index < count; index += 1) {
    const deleted = deletes[index]
    const inserted = inserts[index]
    if (deleted && inserted) {
      rows.push(rowFromPair('REPLACE', deleted.originalLine, inserted.recommendedLine, deleted.originalIndex, inserted.recommendedIndex))
    } else if (deleted) {
      rows.push(rowFromPair('DELETE', deleted.originalLine, '', deleted.originalIndex, null))
    } else if (inserted) {
      rows.push(rowFromPair('INSERT', '', inserted.recommendedLine, null, inserted.recommendedIndex))
    }
  }
  return rows
}

const rowFromPair = (type, originalLine, recommendedLine, originalIndex, recommendedIndex) => ({
  type,
  originalLine: originalLine || '',
  recommendedLine: recommendedLine || '',
  originalIndex,
  recommendedIndex,
  originalHtml: lineHtml(originalLine),
  recommendedHtml: lineHtml(recommendedLine)
})

const lineHtml = value => {
  const text = String(value ?? '')
  return text ? highlightSql(text) : '&nbsp;'
}
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
