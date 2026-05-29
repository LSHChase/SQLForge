<script setup>
import { computed, ref, watch } from 'vue'
import { copyTextToClipboard, formatSqlText, highlightSql } from './sqlFormatting.mjs'

const props = defineProps({
  value: {
    type: [String, Number],
    default: ''
  },
  label: {
    type: String,
    default: 'SQL'
  },
  copyLabel: {
    type: String,
    default: 'Copy'
  },
  formatLabel: {
    type: String,
    default: 'Format'
  },
  rawLabel: {
    type: String,
    default: 'Raw'
  },
  emptyText: {
    type: String,
    default: '-'
  },
  dataTestid: {
    type: String,
    default: ''
  },
  autoFormat: {
    type: Boolean,
    default: true
  },
  formatEnabled: {
    type: Boolean,
    default: true
  },
  compact: {
    type: Boolean,
    default: false
  },
  maxHeight: {
    type: String,
    default: '760px'
  }
})

const showFormattedRaw = ref(false)

watch(
  () => [props.value, props.autoFormat],
  () => {
    showFormattedRaw.value = false
  }
)

const rawSql = computed(() => String(props.value ?? ''))
const hasRawSql = computed(() => rawSql.value.trim().length > 0)
const shouldAutoFormat = computed(() => props.formatEnabled && props.autoFormat)
const canToggleRawFormat = computed(() => props.formatEnabled && !props.autoFormat && hasRawSql.value)

const displaySql = computed(() => {
  const fallback = hasRawSql.value ? rawSql.value : props.emptyText
  if (shouldAutoFormat.value || (canToggleRawFormat.value && showFormattedRaw.value)) {
    return formatSqlText(fallback) || fallback
  }
  return fallback
})

const highlightedSql = computed(() => highlightSql(displaySql.value))

const copySql = () => copyTextToClipboard(displaySql.value)

const toggleRawFormat = () => {
  showFormattedRaw.value = !showFormattedRaw.value
}
</script>

<template>
  <section
    class="sql-code-panel"
    :class="{ 'sql-code-panel-compact': compact }"
    :data-testid="dataTestid || undefined"
  >
    <div class="sql-code-panel__header">
      <span class="sql-code-panel__label">{{ label }}</span>
      <div class="sql-code-panel__actions">
        <el-button text size="small" :disabled="!hasRawSql" @click.stop="copySql">{{ copyLabel }}</el-button>
        <el-button
          v-if="canToggleRawFormat"
          text
          size="small"
          data-testid="sql-code-format-toggle"
          @click.stop="toggleRawFormat"
        >
          {{ showFormattedRaw ? rawLabel : formatLabel }}
        </el-button>
      </div>
    </div>
    <!-- eslint-disable-next-line vue/no-v-html -->
    <pre class="sql-code-panel__body" :style="{ maxHeight }"><code v-html="highlightedSql" /></pre>
  </section>
</template>

<style scoped>
.sql-code-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.sql-code-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sql-code-panel__label {
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sql-code-panel__actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.sql-code-panel__body {
  min-height: 120px;
  margin: 0;
  padding: 14px 16px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 10px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-family: var(--sqlforge-font-mono);
  font-size: 13px;
  line-height: 1.65;
  overflow: auto;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.sql-code-panel-compact .sql-code-panel__body {
  min-height: 72px;
  max-height: 180px;
  padding: 12px;
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
</style>
