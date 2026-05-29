<script setup>
import { computed, ref, watch } from 'vue'
import { buildFormattedSqlDisplayText, buildRawSqlDisplayText, buildSqlCompareRows } from './sqlCompare.mjs'
import { copyTextToClipboard, highlightSql } from './sqlFormatting.mjs'

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
  }
})

const originalFormatActive = ref(true)
const recommendedFormatActive = ref(true)
const originalPaneViewportRef = ref(null)
const recommendedPaneViewportRef = ref(null)
let syncPaneScrollGuard = false

watch(
  () => [props.originalSql, props.recommendedSql],
  () => {
    originalFormatActive.value = true
    recommendedFormatActive.value = true
  },
  { immediate: true }
)

const originalRawSql = computed(() => buildRawSqlDisplayText(props.originalSql))
const recommendedRawSql = computed(() => buildRawSqlDisplayText(props.recommendedSql))
const originalFormattedSql = computed(() => buildFormattedSqlDisplayText(props.originalSql))
const recommendedFormattedSql = computed(() => buildFormattedSqlDisplayText(props.recommendedSql))
const originalDisplaySql = computed(() => (originalFormatActive.value ? originalFormattedSql.value : originalRawSql.value))
const recommendedDisplaySql = computed(() => (recommendedFormatActive.value ? recommendedFormattedSql.value : recommendedRawSql.value))
const originalHasSql = computed(() => originalRawSql.value.length > 0)
const recommendedHasSql = computed(() => recommendedRawSql.value.length > 0)

const compareRows = computed(() =>
  buildSqlCompareRows(originalDisplaySql.value, recommendedDisplaySql.value, {
    originalAutoFormat: false,
    recommendedAutoFormat: false
  })
)

const paneRows = side =>
  compareRows.value.map(row => ({
    key: `${row.key}-${side}`,
    type: row.type.toLowerCase(),
    line: side === 'original' ? row.originalIndex : row.recommendedIndex,
    html: side === 'original' ? row.originalHtml : row.recommendedHtml
  }))

const originalPaneRows = computed(() => paneRows('original'))
const recommendedPaneRows = computed(() => paneRows('recommended'))
const emptyLineHtml = computed(() => highlightSql(props.emptyText))

const currentPaneDisplayText = pane => (pane === 'original' ? originalDisplaySql.value : recommendedDisplaySql.value)

const isPaneFormatted = pane => (pane === 'original' ? originalFormatActive.value : recommendedFormatActive.value)
const hasPaneSql = pane => (pane === 'original' ? originalHasSql.value : recommendedHasSql.value)
const paneFormatLabel = pane => (isPaneFormatted(pane) ? props.rawLabel : props.formatLabel)

const setPaneFormatActive = (pane, value) => {
  if (pane === 'original') {
    originalFormatActive.value = value
    return
  }
  recommendedFormatActive.value = value
}

const currentPaneCopyText = pane => currentPaneDisplayText(pane) || props.emptyText

const copyPaneSql = pane => copyTextToClipboard(currentPaneCopyText(pane))

const formatPaneSql = pane => {
  if (!hasPaneSql(pane)) {
    return
  }
  setPaneFormatActive(pane, !isPaneFormatted(pane))
}

const peerViewportForPane = pane => (pane === 'original' ? recommendedPaneViewportRef.value : originalPaneViewportRef.value)

const syncPaneScroll = (pane, event) => {
  if (syncPaneScrollGuard) {
    return
  }

  const peer = peerViewportForPane(pane)
  if (!peer) {
    return
  }

  syncPaneScrollGuard = true
  peer.scrollTop = event.target.scrollTop
  peer.scrollLeft = event.target.scrollLeft
  requestAnimationFrame(() => {
    syncPaneScrollGuard = false
  })
}
</script>

<template>
  <section class="sql-compare-block">
    <div class="sql-compare-block__panes">
      <section class="sql-compare-pane sql-compare-pane--original" data-testid="sql-compare-pane-original">
        <div class="sql-compare-pane__toolbar">
          <span class="sql-compare-pane__label">{{ originalLabel }}</span>
          <div class="sql-compare-pane__actions">
            <el-button text size="small" :disabled="!originalHasSql" data-testid="sql-compare-copy-original" @click.stop="copyPaneSql('original')">
              {{ copyLabel }}
            </el-button>
            <el-button text size="small" :disabled="!originalHasSql" data-testid="sql-compare-format-original" @click.stop="formatPaneSql('original')">
              {{ paneFormatLabel('original') }}
            </el-button>
          </div>
        </div>
        <div
          ref="originalPaneViewportRef"
          class="sql-compare-block__viewport sql-compare-pane__viewport"
          data-testid="sql-compare-original-viewport"
          @scroll="syncPaneScroll('original', $event)"
        >
          <div class="sql-compare-pane__content">
            <div class="sql-compare-pane__header">
              <span>#</span>
              <span>{{ originalLabel }}</span>
            </div>
            <div
              v-for="row in originalPaneRows"
              :key="row.key"
              class="sql-compare-pane__row sql-compare-row"
              :class="`sql-compare-row--${row.type}`"
            >
              <span class="sql-compare-line">{{ row.line || '' }}</span>
              <!-- eslint-disable-next-line vue/no-v-html -->
              <pre class="sql-compare-code"><code v-html="row.html" /></pre>
            </div>
            <div v-if="!originalPaneRows.length" class="sql-compare-pane__row sql-compare-row sql-compare-row--empty">
              <span class="sql-compare-line" />
              <!-- eslint-disable-next-line vue/no-v-html -->
              <pre class="sql-compare-code sql-compare-code--empty"><code v-html="emptyLineHtml" /></pre>
            </div>
          </div>
        </div>
      </section>

      <section class="sql-compare-pane sql-compare-pane--recommended" data-testid="sql-compare-pane-recommended">
        <div class="sql-compare-pane__toolbar">
          <span class="sql-compare-pane__label">{{ recommendedLabel }}</span>
          <div class="sql-compare-pane__actions">
            <el-button text size="small" :disabled="!recommendedHasSql" data-testid="sql-compare-copy-recommended" @click.stop="copyPaneSql('recommended')">
              {{ copyLabel }}
            </el-button>
            <el-button text size="small" :disabled="!recommendedHasSql" data-testid="sql-compare-format-recommended" @click.stop="formatPaneSql('recommended')">
              {{ paneFormatLabel('recommended') }}
            </el-button>
          </div>
        </div>
        <div
          ref="recommendedPaneViewportRef"
          class="sql-compare-block__viewport sql-compare-pane__viewport"
          data-testid="sql-compare-recommended-viewport"
          @scroll="syncPaneScroll('recommended', $event)"
        >
          <div class="sql-compare-pane__content">
            <div class="sql-compare-pane__header">
              <span>#</span>
              <span>{{ recommendedLabel }}</span>
            </div>
            <div
              v-for="row in recommendedPaneRows"
              :key="row.key"
              class="sql-compare-pane__row sql-compare-row"
              :class="`sql-compare-row--${row.type}`"
            >
              <span class="sql-compare-line">{{ row.line || '' }}</span>
              <!-- eslint-disable-next-line vue/no-v-html -->
              <pre class="sql-compare-code"><code v-html="row.html" /></pre>
            </div>
            <div v-if="!recommendedPaneRows.length" class="sql-compare-pane__row sql-compare-row sql-compare-row--empty">
              <span class="sql-compare-line" />
              <!-- eslint-disable-next-line vue/no-v-html -->
              <pre class="sql-compare-code sql-compare-code--empty"><code v-html="emptyLineHtml" /></pre>
            </div>
          </div>
        </div>
      </section>
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

.sql-compare-block__panes {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(360px, 1fr);
  gap: var(--sqlforge-space-3);
  min-width: 0;
  overflow-x: auto;
  padding-bottom: 2px;
}

.sql-compare-pane {
  display: flex;
  flex-direction: column;
  min-width: 360px;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
}

.sql-compare-pane--recommended {
  border-color: var(--sqlforge-border-strong);
}

.sql-compare-pane__toolbar {
  position: sticky;
  top: 0;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 8px 10px 8px 12px;
  border-bottom: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-2);
}

.sql-compare-pane__label {
  min-width: 0;
  overflow: hidden;
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-overflow: ellipsis;
  text-transform: uppercase;
  white-space: nowrap;
}

.sql-compare-pane__actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.sql-compare-block__viewport {
  max-height: 520px;
  overflow: auto;
}

.sql-compare-pane__content {
  width: max-content;
  min-width: 100%;
}

.sql-compare-pane__header,
.sql-compare-pane__row {
  display: grid;
  grid-template-columns: 54px minmax(320px, max-content);
  min-width: 100%;
  width: max-content;
}

.sql-compare-pane__header {
  position: sticky;
  top: 0;
  z-index: 2;
  border-bottom: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-surface-3);
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  font-weight: 700;
}

.sql-compare-pane__header span,
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
  background: rgba(255, 255, 255, 0.012);
  color: var(--sqlforge-text-muted);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  text-align: right;
  user-select: none;
}

.sql-compare-code {
  min-height: 34px;
  min-width: 320px;
  margin: 0;
  padding: 8px 12px;
  border-left: 1px solid var(--sqlforge-border-subtle);
  color: var(--sqlforge-text-primary);
  font-family: var(--sqlforge-font-mono);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre;
}

.sql-compare-code--empty {
  color: var(--sqlforge-text-secondary);
}

.sql-compare-pane--original .sql-compare-row--delete .sql-compare-line,
.sql-compare-pane--original .sql-compare-row--delete .sql-compare-code,
.sql-compare-pane--original .sql-compare-row--replace .sql-compare-line,
.sql-compare-pane--original .sql-compare-row--replace .sql-compare-code {
  background: rgba(120, 28, 28, 0.2);
}

.sql-compare-pane--recommended .sql-compare-row--insert .sql-compare-line,
.sql-compare-pane--recommended .sql-compare-row--insert .sql-compare-code,
.sql-compare-pane--recommended .sql-compare-row--replace .sql-compare-line,
.sql-compare-pane--recommended .sql-compare-row--replace .sql-compare-code {
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
  .sql-compare-block__panes {
    grid-template-columns: minmax(320px, 1fr) minmax(320px, 1fr);
  }

  .sql-compare-pane {
    min-width: 320px;
  }

  .sql-compare-pane__header,
  .sql-compare-pane__row {
    grid-template-columns: 44px minmax(280px, max-content);
  }

  .sql-compare-code {
    min-width: 280px;
  }
}
</style>
