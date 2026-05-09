<script setup>
import { useI18n } from 'vue-i18n'

useI18n()

defineProps({
  fields: {
    type: Array,
    default: () => []
  },
  displayValue: {
    type: Function,
    required: true
  },
  issueSceneListHelp: {
    type: Function,
    default: () => ''
  }
})
</script>

<template>
  <div class="detail-fields">
    <div
      v-for="field in fields"
      :key="field.label"
      class="detail-field"
      :class="{ 'detail-field-wide': field.wide }"
    >
      <span class="summary-card-label">{{ field.label }}</span>
      <strong>
        {{ displayValue(field.value) }}
        <span
          v-if="field.key === 'issueScenes' && issueSceneListHelp(field.value)"
          class="help-dot issue-scene-help"
          tabindex="0"
          aria-label="issue scene help"
          :data-tooltip="issueSceneListHelp(field.value)"
        >?</span>
      </strong>
    </div>
  </div>
</template>

<style scoped>
.detail-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-field {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 14px;
  background: rgba(35, 35, 35, 0.92);
}

.detail-field strong {
  overflow-wrap: anywhere;
}

.detail-field-wide {
  grid-column: 1 / -1;
}

.summary-card-label {
  display: flex;
  gap: 4px;
  align-items: center;
  margin-bottom: 8px;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.help-dot {
  min-width: 20px;
  height: 20px;
  padding: 0;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 50%;
  color: var(--sqlforge-text-secondary);
  line-height: 18px;
}

.issue-scene-help {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 20px;
  margin-left: 4px;
  cursor: help;
  background: rgba(20, 24, 31, 0.92);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: none;
  vertical-align: middle;
}

.issue-scene-help::after,
.issue-scene-help::before {
  position: absolute;
  z-index: 30;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.12s ease, visibility 0.12s ease;
  visibility: hidden;
}

.issue-scene-help::after {
  bottom: calc(100% + 8px);
  left: 50%;
  width: max-content;
  max-width: min(360px, calc(100vw - 48px));
  padding: 10px 12px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 6px;
  background: #111827;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.35);
  color: #f8fafc;
  content: attr(data-tooltip);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.6;
  text-align: left;
  text-transform: none;
  transform: translateX(-50%);
  white-space: normal;
}

.issue-scene-help::before {
  bottom: calc(100% + 3px);
  left: 50%;
  width: 8px;
  height: 8px;
  background: #111827;
  content: '';
  transform: translateX(-50%) rotate(45deg);
}

.issue-scene-help:hover::after,
.issue-scene-help:hover::before,
.issue-scene-help:focus::after,
.issue-scene-help:focus::before,
.issue-scene-help:focus-visible::after,
.issue-scene-help:focus-visible::before {
  opacity: 1;
  visibility: visible;
}
</style>
