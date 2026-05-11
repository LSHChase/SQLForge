<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ROUTE_PATHS } from '../../config/routePaths.mjs'
import SectionHeader from '../common/SectionHeader.vue'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import ToolbarShell from '../common/ToolbarShell.vue'

const { t } = useI18n()
const router = useRouter()

const sourceModeConfig = {
  PARSE: {
    sourceKinds: ['STRUCTURE_PARSE', 'COMBINED_PARSE', 'PARSE_BATCH', 'REPORT_BATCH', 'END_OF_DAY_SLOW_SQL'],
    evidenceLevels: ['STATIC_PARSE', 'ACCESS_PARSE', 'MIXED'],
    defaultSourceKind: 'COMBINED_PARSE',
    defaultEvidenceLevel: 'STATIC_PARSE'
  },
  QUERY: {
    sourceKinds: ['QUERY_HISTORY', 'SLOW_SQL', 'HIGH_P99', 'HIGH_SCAN', 'BENCHMARK_REGRESSION', 'MANUAL'],
    evidenceLevels: ['RUNTIME_HISTORY', 'EXPLAIN_PLAN', 'BENCHMARK', 'MIXED'],
    defaultSourceKind: 'QUERY_HISTORY',
    defaultEvidenceLevel: 'RUNTIME_HISTORY'
  }
}

const flowNodes = [
  'ENTRY_EVIDENCE',
  'CANDIDATE_SUGGESTION',
  'SQL_DIFF',
  'PLAN_APPROVAL',
  'APPLY_VALIDATION',
  'MONITORING_ALERT',
  'ROLLBACK_DISCARD'
]

const form = reactive({
  tenantId: 'tenant-a',
  datasourceCode: 'hetu_main',
  schemaName: 'dwd',
  stage: 'PROD',
  reportCode: 'RPT_SALES_DAILY',
  sourceType: 'PARSE',
  sourceKind: 'COMBINED_PARSE',
  sourceId: 'parse-history-001',
  parseHistoryId: 'parse-history-001',
  historyId: '',
  sqlFingerprint: 'fp_sales_daily_20260401',
  evidenceLevel: 'STATIC_PARSE',
  enableHetuExplain: false,
  sqlText: "SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id"
})

const activeTab = ref('candidates')
const evidenceDrawerVisible = ref(false)
const evidenceDrawerTitle = ref('')
const evidenceDrawerPayload = ref(null)

const sourceModeOptions = computed(() => [
  { value: 'PARSE', label: t('accelerationGovernanceWorkbench.modes.parse') },
  { value: 'QUERY', label: t('accelerationGovernanceWorkbench.modes.query') }
])

const sourceKindOptions = computed(() => sourceModeConfig[form.sourceType].sourceKinds)
const evidenceLevelOptions = computed(() => sourceModeConfig[form.sourceType].evidenceLevels)

const resolvedSourceId = computed(() => {
  if (hasValue(form.sourceId)) {
    return form.sourceId
  }
  if (form.sourceType === 'PARSE') {
    return form.parseHistoryId || form.reportCode
  }
  return form.historyId
})

const sourceSummaryText = computed(() =>
  t('accelerationGovernanceWorkbench.states.sourceSummary', {
    sourceType: form.sourceType,
    sourceKind: form.sourceKind,
    sourceId: resolvedSourceId.value || '-',
    evidenceLevel: form.evidenceLevel
  })
)

const sourceSummaryRows = computed(() => [
  field('tenantId', t('accelerationGovernanceWorkbench.fields.tenantId'), form.tenantId),
  field('datasourceCode', t('accelerationGovernanceWorkbench.fields.datasourceCode'), form.datasourceCode),
  field('stage', t('accelerationGovernanceWorkbench.fields.stage'), form.stage),
  field('sourceType', t('accelerationGovernanceWorkbench.fields.sourceType'), form.sourceType),
  field('sourceKind', t('accelerationGovernanceWorkbench.fields.sourceKind'), form.sourceKind),
  field('sourceId', t('accelerationGovernanceWorkbench.fields.sourceId'), resolvedSourceId.value),
  field('sqlFingerprint', t('accelerationGovernanceWorkbench.fields.sqlFingerprint'), form.sqlFingerprint),
  field('evidenceLevel', t('accelerationGovernanceWorkbench.fields.evidenceLevel'), form.evidenceLevel)
])

const sourceEvidence = computed(() => ({
  contractStage: 'HARN-137_READ_ONLY_SHELL',
  sourceType: form.sourceType,
  sourceKind: form.sourceKind,
  sourceId: resolvedSourceId.value,
  parseHistoryId: form.parseHistoryId,
  historyId: form.historyId,
  sqlFingerprint: form.sqlFingerprint,
  tenantId: form.tenantId,
  datasourceCode: form.datasourceCode,
  schemaName: form.schemaName,
  stage: form.stage,
  reportCode: form.reportCode,
  evidenceLevel: form.evidenceLevel,
  enableHetuExplain: form.enableHetuExplain,
  shellOnly: true,
  submitted: false
}))

const routeTargets = computed(() => [
  {
    key: 'parseRecord',
    label: t('accelerationGovernanceWorkbench.actions.openParseRecord'),
    path: ROUTE_PATHS.parseRecord,
    query: compactQuery({
      tenantId: form.tenantId,
      parseHistoryId: form.parseHistoryId,
      reportId: form.reportCode
    }),
    disabled: !hasValue(form.parseHistoryId) && !hasValue(form.reportCode)
  },
  {
    key: 'sqlHistory',
    label: t('accelerationGovernanceWorkbench.actions.openSqlHistory'),
    path: ROUTE_PATHS.sqlHistory,
    query: compactQuery({
      tenantId: form.tenantId,
      historyId: form.historyId
    }),
    disabled: !hasValue(form.historyId)
  },
  {
    key: 'recommendationCenter',
    label: t('accelerationGovernanceWorkbench.actions.openRecommendationCenter'),
    path: ROUTE_PATHS.recommendationCenter,
    query: compactQuery({ tenantId: form.tenantId }),
    disabled: false
  },
  {
    key: 'sqlQuery',
    label: t('accelerationGovernanceWorkbench.actions.openSqlQuery'),
    path: ROUTE_PATHS.sqlQuery,
    query: compactQuery({ tenantId: form.tenantId }),
    disabled: !hasValue(form.sqlText)
  },
  {
    key: 'alertCenter',
    label: t('accelerationGovernanceWorkbench.actions.openAlertCenter'),
    path: ROUTE_PATHS.alertCenter,
    query: compactQuery({ tenantId: form.tenantId }),
    disabled: false
  }
])

const futureActions = computed(() => [
  action('candidates', 'POST /api/sql-optimization/acceleration-candidates'),
  action('candidates', 'POST /api/sql-optimization/tasks'),
  action('diff', 'GET /api/sql-optimization/recommendations/{recommendationId}/diff'),
  action('approval', 'POST /api/sql-optimization/acceleration-plans'),
  action('approval', 'POST /api/sql-optimization/acceleration-plans/{planId}/approval'),
  action('validation', 'POST /api/sql-optimization/acceleration-plans/{planId}/apply'),
  action('validation', 'POST /api/sql-optimization/acceleration-plans/{planId}/verify'),
  action('monitoring', 'GET /api/governance/query-history/{historyId}/rewrite-records'),
  action('monitoring', 'POST /api/sql-optimization/rewrite-records/{rewriteRecordId}/validation-runs'),
  action('evidence', 'POST /api/query-execution/queries/execute')
])

const tabDefinitions = computed(() => [
  tab('candidates', t('accelerationGovernanceWorkbench.tabs.candidates')),
  tab('diff', t('accelerationGovernanceWorkbench.tabs.diff')),
  tab('approval', t('accelerationGovernanceWorkbench.tabs.approval')),
  tab('validation', t('accelerationGovernanceWorkbench.tabs.validation')),
  tab('monitoring', t('accelerationGovernanceWorkbench.tabs.monitoring')),
  tab('evidence', t('accelerationGovernanceWorkbench.tabs.evidence'))
])

const handleModeSelect = mode => {
  if (!sourceModeConfig[mode] || form.sourceType === mode) {
    return
  }
  form.sourceType = mode
  form.sourceKind = sourceModeConfig[mode].defaultSourceKind
  form.evidenceLevel = sourceModeConfig[mode].defaultEvidenceLevel
  if (mode === 'PARSE') {
    form.sourceId = form.parseHistoryId || 'parse-history-001'
    return
  }
  form.sourceId = form.historyId || 'history-001'
}

const openRouteTarget = target => {
  if (target.disabled) {
    return
  }
  router.push({
    path: target.path,
    query: target.query
  })
}

const openEvidenceDrawer = (title, payload) => {
  evidenceDrawerTitle.value = title
  evidenceDrawerPayload.value = payload
  evidenceDrawerVisible.value = true
}

const tabActions = tabName => futureActions.value.filter(item => item.tab === tabName)

function action(tabName, endpoint) {
  return {
    tab: tabName,
    endpoint,
    ownerTask: 'HARN-138',
    state: t('accelerationGovernanceWorkbench.states.futureTask')
  }
}

function tab(name, label) {
  return { name, label }
}

function field(key, label, value) {
  return { key, label, value: displayValue(value) }
}

function displayValue(value) {
  if (!hasValue(value)) {
    return '-'
  }
  return String(value)
}

function hasValue(value) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function compactQuery(query) {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => hasValue(value))
  )
}

function formatJson(value) {
  return JSON.stringify(value || {}, null, 2)
}

function flowNodeLabel(node) {
  return t(`accelerationGovernanceWorkbench.flowNodes.${node}`)
}
</script>

<template>
  <section class="acceleration-governance-page" data-testid="acceleration-governance-workbench-page">
    <SectionHeader
      :eyebrow="t('accelerationGovernanceWorkbench.eyebrow')"
      :title="t('accelerationGovernanceWorkbench.pageTitle')"
      :summary="t('accelerationGovernanceWorkbench.boundarySummary')"
      :level="1"
      size="compact"
    >
      <template #actions>
        <el-button data-testid="acceleration-workbench-source-evidence" @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.drawers.source'), sourceEvidence)">
          {{ t('accelerationGovernanceWorkbench.actions.viewSourceEvidence') }}
        </el-button>
      </template>
    </SectionHeader>

    <section class="status-strip" data-testid="acceleration-workbench-status-strip">
      <div v-for="item in sourceSummaryRows" :key="item.key" class="status-cell">
        <span>{{ item.label }}</span>
        <strong :data-testid="`acceleration-workbench-summary-${item.key}`">{{ item.value }}</strong>
      </div>
    </section>

    <ToolbarShell
      :eyebrow="t('accelerationGovernanceWorkbench.source.eyebrow')"
      :title="t('accelerationGovernanceWorkbench.source.title')"
      :summary="t('accelerationGovernanceWorkbench.source.summary')"
      density="compact"
      test-id="acceleration-workbench-source-shell"
    >
      <div class="source-mode-row" data-testid="acceleration-workbench-source-mode">
        <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.currentMode') }}</span>
        <div class="segmented-control">
          <button
            v-for="item in sourceModeOptions"
            :key="item.value"
            type="button"
            class="segmented-option"
            :class="{ 'segmented-option-active': form.sourceType === item.value }"
            :data-testid="`acceleration-workbench-mode-${item.value}`"
            @click="handleModeSelect(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <div class="source-grid">
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.tenantId') }}</span>
          <el-input v-model.trim="form.tenantId" data-testid="acceleration-workbench-tenant-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.datasourceCode') }}</span>
          <el-input v-model.trim="form.datasourceCode" data-testid="acceleration-workbench-datasource-code" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.schemaName') }}</span>
          <el-input v-model.trim="form.schemaName" data-testid="acceleration-workbench-schema-name" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.stage') }}</span>
          <el-input v-model.trim="form.stage" data-testid="acceleration-workbench-stage" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceType') }}</span>
          <el-input v-model="form.sourceType" readonly data-testid="acceleration-workbench-source-type" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceKind') }}</span>
          <el-select v-model="form.sourceKind" data-testid="acceleration-workbench-source-kind">
            <el-option v-for="item in sourceKindOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sourceId') }}</span>
          <el-input v-model.trim="form.sourceId" data-testid="acceleration-workbench-source-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.parseHistoryId') }}</span>
          <el-input v-model.trim="form.parseHistoryId" data-testid="acceleration-workbench-parse-history-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.historyId') }}</span>
          <el-input v-model.trim="form.historyId" data-testid="acceleration-workbench-history-id" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.reportCode') }}</span>
          <el-input v-model.trim="form.reportCode" data-testid="acceleration-workbench-report-code" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sqlFingerprint') }}</span>
          <el-input v-model.trim="form.sqlFingerprint" data-testid="acceleration-workbench-sql-fingerprint" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.evidenceLevel') }}</span>
          <el-select v-model="form.evidenceLevel" data-testid="acceleration-workbench-evidence-level">
            <el-option v-for="item in evidenceLevelOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block field-block-switch">
          <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.enableHetuExplain') }}</span>
          <el-switch v-model="form.enableHetuExplain" data-testid="acceleration-workbench-hetu-explain" />
        </label>
      </div>
      <label class="field-block field-block-wide">
        <span class="field-label">{{ t('accelerationGovernanceWorkbench.fields.sqlText') }}</span>
        <el-input v-model="form.sqlText" type="textarea" :rows="4" data-testid="acceleration-workbench-sql-text" />
      </label>
    </ToolbarShell>

    <section class="workbench-panel" data-testid="acceleration-workbench-flow-panel">
      <SectionHeader
        :eyebrow="t('accelerationGovernanceWorkbench.flow.eyebrow')"
        :title="t('accelerationGovernanceWorkbench.flow.title')"
        :summary="t('accelerationGovernanceWorkbench.flow.summary')"
        size="compact"
      />
      <ol class="flow-map" data-testid="acceleration-workbench-flow-map">
        <li v-for="(node, index) in flowNodes" :key="node" class="flow-node" :data-testid="`acceleration-workbench-flow-${node}`">
          <span class="flow-index">{{ index + 1 }}</span>
          <strong>{{ flowNodeLabel(node) }}</strong>
          <span>{{ index === 0 ? t('accelerationGovernanceWorkbench.states.shellOnly') : t('accelerationGovernanceWorkbench.states.futureTask') }}</span>
        </li>
      </ol>
    </section>

    <section class="workbench-panel" data-testid="acceleration-workbench-navigation-panel">
      <SectionHeader
        :title="t('accelerationGovernanceWorkbench.sections.jumpTitle')"
        :summary="t('accelerationGovernanceWorkbench.sections.jumpSummary')"
        size="compact"
      >
        <template #actions>
          <el-button data-testid="acceleration-workbench-route-evidence" @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.drawers.routes'), routeTargets)">
            {{ t('accelerationGovernanceWorkbench.actions.viewRouteEvidence') }}
          </el-button>
        </template>
      </SectionHeader>
      <div class="jump-grid">
        <button
          v-for="item in routeTargets"
          :key="item.key"
          type="button"
          class="jump-button"
          :class="{ 'jump-button-disabled': item.disabled }"
          :data-testid="`acceleration-workbench-open-${item.key}`"
          :disabled="item.disabled"
          @click="openRouteTarget(item)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.disabled ? t('accelerationGovernanceWorkbench.states.missingTraceKey') : item.path }}</strong>
        </button>
      </div>
    </section>

    <section class="workbench-panel" data-testid="acceleration-workbench-tabs-panel">
      <el-tabs v-model="activeTab" data-testid="acceleration-workbench-tabs">
        <el-tab-pane v-for="tabItem in tabDefinitions" :key="tabItem.name" :label="tabItem.label" :name="tabItem.name">
          <div class="tab-shell" :data-testid="`acceleration-workbench-tab-${tabItem.name}`">
            <div class="tab-source-strip">
              <span>{{ t('accelerationGovernanceWorkbench.sections.tabSummary') }}</span>
              <strong>{{ sourceSummaryText }}</strong>
            </div>
            <div class="future-action-list">
              <article v-for="item in tabActions(tabItem.name)" :key="`${tabItem.name}:${item.endpoint}`" class="future-action-row">
                <div>
                  <span class="sqlforge-code-label">{{ t('accelerationGovernanceWorkbench.fields.endpoint') }}</span>
                  <strong>{{ item.endpoint }}</strong>
                </div>
                <div>
                  <span class="sqlforge-code-label">{{ t('accelerationGovernanceWorkbench.fields.ownerTask') }}</span>
                  <strong>{{ item.ownerTask }}</strong>
                </div>
                <el-button disabled data-testid="acceleration-workbench-future-action">
                  {{ t('accelerationGovernanceWorkbench.actions.futureAction') }}
                </el-button>
              </article>
            </div>
            <p class="state-note">{{ t('accelerationGovernanceWorkbench.states.disabledUntilNextTask') }}</p>
          </div>
        </el-tab-pane>
      </el-tabs>
      <div class="evidence-actions">
        <el-button data-testid="acceleration-workbench-action-evidence" @click="openEvidenceDrawer(t('accelerationGovernanceWorkbench.drawers.actions'), futureActions)">
          {{ t('accelerationGovernanceWorkbench.actions.viewActionEvidence') }}
        </el-button>
      </div>
    </section>

    <section class="workbench-panel" data-testid="acceleration-workbench-sql-preview">
      <SectionHeader
        :title="t('accelerationGovernanceWorkbench.sections.sqlPreview')"
        :summary="sourceSummaryText"
        size="compact"
      />
      <SqlCodeBlock
        :value="form.sqlText"
        :label="t('accelerationGovernanceWorkbench.fields.sqlText')"
        :copy-label="t('common.actions.copy')"
        compact
      />
    </section>

    <el-drawer v-model="evidenceDrawerVisible" :title="evidenceDrawerTitle" size="48%" data-testid="acceleration-workbench-evidence-drawer">
      <pre class="code-block">{{ formatJson(evidenceDrawerPayload) }}</pre>
    </el-drawer>
  </section>
</template>

<style scoped>
.acceleration-governance-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
  min-width: 0;
}

.status-strip,
.workbench-panel,
.tab-shell {
  min-width: 0;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-2);
}

.status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  overflow: hidden;
  background: var(--sqlforge-border-default);
}

.status-cell {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  padding: var(--sqlforge-space-4);
  background: var(--sqlforge-surface-2);
}

.status-cell span,
.tab-source-strip span,
.state-note {
  color: var(--sqlforge-text-muted);
}

.status-cell strong,
.tab-source-strip strong,
.future-action-row strong,
.jump-button strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
}

.source-mode-row,
.source-grid,
.jump-grid,
.future-action-row,
.evidence-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: flex-end;
}

.source-mode-row {
  align-items: center;
}

.segmented-control {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
}

.segmented-option {
  min-height: 32px;
  padding: 0 var(--sqlforge-space-4);
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--sqlforge-text-secondary);
  cursor: pointer;
}

.segmented-option-active {
  border-color: var(--sqlforge-color-brand-border);
  background: rgba(62, 207, 142, 0.12);
  color: var(--sqlforge-text-primary);
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  width: 100%;
}

.field-block {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
}

.field-block-wide {
  width: 100%;
}

.field-block-switch {
  align-content: end;
  min-height: 64px;
}

.field-label {
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.workbench-panel {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  padding: var(--sqlforge-space-5);
}

.flow-map {
  display: grid;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
  gap: var(--sqlforge-space-3);
  padding: 0;
  margin: 0;
  list-style: none;
}

.flow-node {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  min-height: 126px;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
}

.flow-node strong {
  overflow-wrap: anywhere;
}

.flow-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--sqlforge-color-brand-border);
  border-radius: 999px;
  color: var(--sqlforge-color-brand);
}

.flow-node span:last-child {
  color: var(--sqlforge-text-muted);
}

.jump-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
}

.jump-button {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-height: 86px;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
  color: var(--sqlforge-text-primary);
  text-align: left;
  cursor: pointer;
}

.jump-button-disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.tab-shell {
  display: grid;
  gap: var(--sqlforge-space-4);
  padding: var(--sqlforge-space-4);
}

.tab-source-strip {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: var(--sqlforge-space-3);
  padding-bottom: var(--sqlforge-space-3);
  border-bottom: 1px solid var(--sqlforge-border-subtle);
}

.future-action-list {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.future-action-row {
  justify-content: space-between;
  padding: var(--sqlforge-space-4);
  border: 1px solid var(--sqlforge-border-subtle);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-surface-1);
}

.future-action-row > div {
  display: grid;
  gap: var(--sqlforge-space-2);
  min-width: 0;
}

.state-note {
  margin: 0;
  line-height: 1.6;
}

.evidence-actions {
  justify-content: flex-end;
}

.code-block {
  min-height: 100%;
  margin: 0;
  padding: var(--sqlforge-space-4);
  overflow: auto;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 1280px) {
  .status-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .source-grid,
  .jump-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .flow-map {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .status-strip,
  .source-grid,
  .jump-grid,
  .flow-map {
    grid-template-columns: minmax(0, 1fr);
  }

  .segmented-control,
  .segmented-option,
  .future-action-row {
    width: 100%;
  }
}
</style>
