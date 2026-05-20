<script setup>
import { computed, ref } from 'vue'
import { copyTextToClipboard, formatSqlText, highlightSql } from './sqlFormatting.mjs'

const props = defineProps({
  modelValue: {
    type: String,
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
  rows: {
    type: Number,
    default: 8
  },
  placeholder: {
    type: String,
    default: ''
  },
  dataTestid: {
    type: String,
    default: ''
  },
  formatEnabled: {
    type: Boolean,
    default: true
  },
  maxHeight: {
    type: String,
    default: '720px'
  }
})

const emit = defineEmits(['update:modelValue', 'format'])

const highlightRef = ref(null)

const minHeight = computed(() => `${Math.max(props.rows, 4) * 22 + 32}px`)
const displayValue = computed(() => String(props.modelValue ?? ''))
const highlightedSql = computed(() => highlightSql(displayValue.value || props.placeholder))

const updateValue = event => {
  emit('update:modelValue', event.target.value)
}

const formatValue = () => {
  const formatted = formatSqlText(displayValue.value)
  emit('update:modelValue', formatted)
  emit('format', formatted)
}

const copySql = () => copyTextToClipboard(displayValue.value)

const syncScroll = event => {
  if (!highlightRef.value) {
    return
  }
  highlightRef.value.scrollTop = event.target.scrollTop
  highlightRef.value.scrollLeft = event.target.scrollLeft
}
</script>

<template>
  <section class="sql-editor-field" :data-testid="dataTestid ? `${dataTestid}-container` : undefined">
    <div class="sql-editor-field__header">
      <span class="sql-editor-field__label">{{ label }}</span>
      <div class="sql-editor-field__actions">
        <el-button text size="small" :disabled="!displayValue" @click.stop="copySql">{{ copyLabel }}</el-button>
        <el-button text size="small" :disabled="!formatEnabled" @click.stop="formatValue">{{ formatLabel }}</el-button>
      </div>
    </div>
    <div class="sql-editor-field__shell" :style="{ minHeight, maxHeight }">
      <!-- eslint-disable vue/no-v-html -->
      <pre ref="highlightRef" class="sql-editor-field__highlight" aria-hidden="true"><code v-html="highlightedSql" /></pre>
      <!-- eslint-enable vue/no-v-html -->
      <textarea
        class="sql-editor-field__textarea"
        :value="displayValue"
        :placeholder="placeholder"
        :data-testid="dataTestid || undefined"
        spellcheck="false"
        @input="updateValue"
        @scroll="syncScroll"
      />
    </div>
  </section>
</template>

<style scoped>
.sql-editor-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.sql-editor-field__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sql-editor-field__label {
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sql-editor-field__actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.sql-editor-field__shell {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 10px;
  background: var(--sqlforge-bg-page-deep);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.01);
}

.sql-editor-field__highlight,
.sql-editor-field__textarea {
  position: absolute;
  inset: 0;
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 14px 16px;
  border: 0;
  font-family: var(--sqlforge-font-mono), "SFMono-Regular", Consolas, "Liberation Mono", Menlo, monospace;
  font-size: 13px;
  font-variant-ligatures: none;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-wrap: break-word;
  tab-size: 2;
}

.sql-editor-field__highlight {
  overflow: hidden;
  color: var(--sqlforge-text-primary);
  pointer-events: none;
}

.sql-editor-field__highlight code {
  display: block;
  box-sizing: border-box;
  min-height: 100%;
  font: inherit;
  line-height: inherit;
  white-space: inherit;
  word-break: inherit;
  overflow-wrap: inherit;
  tab-size: inherit;
}

.sql-editor-field__textarea {
  resize: none;
  overflow: auto;
  background: transparent;
  color: transparent;
  caret-color: var(--sqlforge-color-brand);
  outline: none;
  -webkit-text-fill-color: transparent;
}

.sql-editor-field__textarea::selection {
  background: rgba(62, 207, 142, 0.24);
}

.sql-editor-field__textarea::placeholder {
  color: rgba(137, 137, 137, 0.64);
  -webkit-text-fill-color: rgba(137, 137, 137, 0.64);
}

.sql-editor-field__shell:focus-within {
  border-color: var(--sqlforge-color-brand-border);
  box-shadow: 0 0 0 1px var(--sqlforge-color-brand-border) inset;
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
