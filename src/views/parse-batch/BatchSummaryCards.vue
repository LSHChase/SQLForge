<script setup>
import { useI18n } from 'vue-i18n'

useI18n()

defineProps({
  items: {
    type: Array,
    default: () => []
  },
  helpTextForKey: {
    type: Function,
    default: () => ''
  },
  openFieldHelp: {
    type: Function,
    default: () => {}
  }
})
</script>

<template>
  <div class="summary-grid">
    <article v-for="item in items" :key="item.label" class="summary-card">
      <span class="summary-card-label">
        {{ item.label }}
        <el-button
          v-if="helpTextForKey(item.key)"
          text
          size="small"
          class="help-dot"
          aria-label="field help"
          @click="openFieldHelp(item.key, item.label)"
        >
          ?
        </el-button>
      </span>
      <strong>{{ item.value }}</strong>
    </article>
  </div>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.summary-card {
  padding: 14px 16px;
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
</style>
