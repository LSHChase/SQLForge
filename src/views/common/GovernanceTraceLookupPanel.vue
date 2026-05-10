<script setup>
import { useI18n } from 'vue-i18n'
import ToolbarShell from './ToolbarShell.vue'

const props = defineProps({
  form: {
    type: Object,
    required: true
  },
  prefix: {
    type: String,
    required: true
  },
  eyebrow: {
    type: String,
    default: 'trace lookup'
  },
  titleKey: {
    type: String,
    required: true
  },
  summaryKey: {
    type: String,
    default: ''
  },
  emptyCriteriaKey: {
    type: String,
    required: true
  },
  runLabelKey: {
    type: String,
    required: true
  },
  includeRemediationTenant: {
    type: Boolean,
    default: false
  },
  searchCriteria: {
    type: Array,
    default: () => []
  },
  loadingLookup: {
    type: Boolean,
    default: false
  },
  loadingStats: {
    type: Boolean,
    default: false
  },
  hasMore: {
    type: Boolean,
    default: false
  },
  showRefreshQueue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['run', 'clear', 'load-more', 'refresh-queue', 'update-field'])

const { t } = useI18n()

const updateField = (field, value) => {
  emit('update-field', { field, value })
}
</script>

<template>
  <ToolbarShell
    :eyebrow="props.eyebrow"
    :title="t(props.titleKey)"
    :summary="props.summaryKey ? t(props.summaryKey) : ''"
    :test-id="`${props.prefix}-lookup-panel`"
  >
    <div class="governance-lookup-grid">
      <label class="governance-field">
        <span>{{ props.includeRemediationTenant ? t('governanceTrace.businessTenant') : t('governanceTrace.tenantContext') }}</span>
        <el-input
          :model-value="props.form.tenantId"
          :data-testid="`${props.prefix}-tenant-id`"
          @update:model-value="value => updateField('tenantId', value)"
        />
      </label>

      <label v-if="props.includeRemediationTenant" class="governance-field">
        <span>{{ t('governanceTrace.governanceTenant') }}</span>
        <el-input
          :model-value="props.form.remediationTenantId"
          :data-testid="`${props.prefix}-remediation-tenant-id`"
          @update:model-value="value => updateField('remediationTenantId', value)"
        />
      </label>

      <label class="governance-field">
        <span>{{ t('governanceTrace.lookupLimit') }}</span>
        <el-input-number
          :model-value="props.form.limit"
          :min="1"
          :max="100"
          controls-position="right"
          :data-testid="`${props.prefix}-limit`"
          @update:model-value="value => updateField('limit', value)"
        />
      </label>

      <label class="governance-field governance-field-wide">
        <span>{{ t('governanceTrace.traceId') }}</span>
        <el-input
          :model-value="props.form.traceId"
          :data-testid="`${props.prefix}-trace-id`"
          @update:model-value="value => updateField('traceId', value)"
        />
      </label>

      <label class="governance-field">
        <span>{{ t('governanceTrace.taskId') }}</span>
        <el-input
          :model-value="props.form.taskId"
          :data-testid="`${props.prefix}-task-id`"
          @update:model-value="value => updateField('taskId', value)"
        />
      </label>

      <label class="governance-field">
        <span>{{ t('governanceTrace.reportId') }}</span>
        <el-input
          :model-value="props.form.reportId"
          :data-testid="`${props.prefix}-report-id`"
          @update:model-value="value => updateField('reportId', value)"
        />
      </label>

      <label class="governance-field">
        <span>{{ t('governanceTrace.windowStart') }}</span>
        <el-input
          :model-value="props.form.windowStart"
          :data-testid="`${props.prefix}-window-start`"
          @update:model-value="value => updateField('windowStart', value)"
        />
      </label>

      <label class="governance-field">
        <span>{{ t('governanceTrace.windowEnd') }}</span>
        <el-input
          :model-value="props.form.windowEnd"
          :data-testid="`${props.prefix}-window-end`"
          @update:model-value="value => updateField('windowEnd', value)"
        />
      </label>
    </div>

    <div class="governance-action-row">
      <el-button type="primary" :loading="props.loadingLookup" :data-testid="`${props.prefix}-run-lookup`" @click="$emit('run')">
        {{ t(props.runLabelKey) }}
      </el-button>
      <el-button
        v-if="props.showRefreshQueue"
        :loading="props.loadingStats"
        :data-testid="`${props.prefix}-refresh-queue`"
        @click="$emit('refresh-queue')"
      >
        {{ t('governanceTrace.refreshQueueImpact') }}
      </el-button>
      <el-button :data-testid="`${props.prefix}-clear-lookup`" @click="$emit('clear')">
        {{ t('governanceTrace.clearCriteria') }}
      </el-button>
      <el-button
        v-if="props.hasMore"
        :loading="props.loadingLookup"
        :data-testid="`${props.prefix}-load-more`"
        @click="$emit('load-more')"
      >
        {{ t('governanceTrace.loadOlderEvidence') }}
      </el-button>
    </div>

    <div class="lookup-chip-list">
      <span
        v-for="item in props.searchCriteria"
        :key="item.key"
        class="lookup-chip"
      >
        {{ t(`governanceTrace.criteria.${item.key}`) }}: {{ item.value }}
      </span>
      <span v-if="!props.searchCriteria.length" class="lookup-chip lookup-chip-muted">
        {{ t(props.emptyCriteriaKey) }}
      </span>
    </div>
  </ToolbarShell>
</template>

<style scoped>
.governance-lookup-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--sqlforge-space-3);
  width: 100%;
}

.governance-field {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.governance-field-wide {
  grid-column: span 2;
}

.governance-field :deep(.el-input-number) {
  width: 100%;
}

.governance-action-row,
.lookup-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
}

.lookup-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--sqlforge-space-3);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.lookup-chip-muted {
  color: var(--sqlforge-text-muted);
}

@media (max-width: 760px) {
  .governance-field-wide {
    grid-column: span 1;
  }
}
</style>
