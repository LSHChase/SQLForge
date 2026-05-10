<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'
import {
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceQueryHistoryPage
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a',
  accessChannel: 'ALL'
})

const loading = reactive({
  page: false,
  detail: false
})

const activeTab = ref('audit')
const policyDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const rawDrawerVisible = ref(false)
const placeholderDialogVisible = ref(false)
const accessAuditPage = ref(null)
const selectedHistoryDetail = ref(null)
const errorMessage = ref('')
const placeholderPayload = ref({
  title: '',
  capability: '',
  reason: '',
  nextStep: ''
})

const isChinese = computed(() => locale.value === 'zh-CN')
const accessChannelOptions = [
  'ALL',
  'PAGE',
  'API',
  'JDBC_AGENT',
  'SDK',
  'CLIENT'
].map(value => ({ label: value, value }))
const accessChannelCards = computed(() => [
  channelCard('PAGE', isChinese.value ? '页面入口' : 'Page entry', isChinese.value ? '前端控制台与浏览器发起查询。' : 'Frontend console and browser-launched queries.'),
  channelCard('API', 'HTTP API', isChinese.value ? '受保护接口默认入口。' : 'Protected HTTP API baseline.'),
  channelCard('JDBC_AGENT', 'JDBC Agent', isChinese.value ? '支持 Observe / Governed Execute / Local Rewrite + Direct JDBC。' : 'Supports Observe / Governed Execute / Local Rewrite + Direct JDBC.'),
  channelCard('SDK', 'Java SDK', isChinese.value ? 'typed client + retry + access audit。' : 'Typed client + retry + access audit.'),
  channelCard('CLIENT', 'Direct Client', isChinese.value ? '保留 vocabulary，但不宣称独立 client SDK 已全面落地。' : 'Vocabulary is preserved without claiming a separate client SDK is fully rolled out.')
])
const jdbcAgentModes = computed(() => [
  {
    mode: 'OBSERVE',
    title: 'Observe',
    summary: isChinese.value
      ? '默认模式；采集 SQL、注释与 query-date 证据，不接管执行。'
      : 'Default mode; captures SQL, comment, and query-date evidence without taking over execution.'
  },
  {
    mode: 'GOVERNED_EXECUTE',
    title: 'Governed Execute',
    summary: isChinese.value
      ? '通过平台 API 执行 SQL，失败时依 fallbackStrategy 保留 direct JDBC 语义。'
      : 'Executes SQL through the platform API and preserves direct-JDBC fallback semantics according to fallbackStrategy.'
  },
  {
    mode: 'LOCAL_REWRITE_DIRECT_JDBC',
    title: 'Local Rewrite + Direct JDBC',
    summary: isChinese.value
      ? '本地轻量改写与路由，再直连 JDBC；改写失败不可 silent mutate。'
      : 'Performs lightweight local rewrite and routing before direct JDBC; rewrite failures must not silently mutate SQL.'
  }
])
const jdbcPolicyFields = computed(() => [
  field('agentMode', 'agentMode', 'OBSERVE'),
  field('redisEndpoints', 'redisEndpoints', isChinese.value ? '规则源可配置，但不把外部 Redis 写成默认事实。' : 'Rule-source endpoints are configurable without claiming external Redis is the default fact.'),
  field('redisNamespace', 'redisNamespace', 'sqlforge:jdbc-agent'),
  field('apiBaseUrl', 'apiBaseUrl', isChinese.value ? '必填运行时配置' : 'Required runtime configuration'),
  field('routeEnabled', 'routeEnabled', 'false/true'),
  field('rewriteEnabled', 'rewriteEnabled', 'false/true'),
  field('lightParseTimeoutMs', 'lightParseTimeoutMs', '20'),
  field('fallbackStrategy', 'fallbackStrategy', 'DIRECT_JDBC | ORIGINAL_SQL | FAIL_CLOSED'),
  field('historyReportEnabled', 'historyReportEnabled', 'true')
])
const sdkCards = computed(() => [
  field('accessChannel', 'accessChannel', 'SDK'),
  field('operationCode', 'operationCode', 'SDK_QUERY_EXECUTE'),
  field('resourceType', 'resourceType', 'SDK_QUERY'),
  field('serviceCode', 'serviceCode', 'JAVA_SDK'),
  field('auditWrite', isChinese.value ? '审计写回' : 'Audit write', isChinese.value ? '成功/失败都写 access audit 摘要。' : 'Writes access-audit summaries on both success and failure.'),
  field('routeProfile', isChinese.value ? '成功摘要' : 'Success summary', isChinese.value ? '会回写 routeProfile 与 executionMode。' : 'Persists routeProfile and executionMode in the success summary.')
])
const auditItems = computed(() => accessAuditPage.value?.items || [])
const classificationSummary = computed(() => accessAuditPage.value?.classificationSummary || {})

// Static contract tokens: GET /api/governance/access-audit, query-history, JDBC_AGENT, SDK_QUERY_EXECUTE, OBSERVE, GOVERNED_EXECUTE, LOCAL_REWRITE_DIRECT_JDBC.

const openPlaceholderAction = actionType => {
  const config = actionType === 'create'
    ? {
        title: isChinese.value ? '新增接入策略暂不可写' : 'Create access strategy is not writable yet',
        capability: isChinese.value ? '新增接入策略' : 'Create access strategy',
        reason: isChinese.value
          ? '当前仓库对开放接入页只暴露 query-history 视角和渠道说明，没有对应的接入策略写接口。'
          : 'The repository currently exposes only query-history evidence and channel guidance for the access page, without a writable access-strategy API.',
        nextStep: isChinese.value
          ? '如需真实新增能力，先补后端策略写接口和审计契约。'
          : 'Add a backend policy-write API and audit contract before enabling a real create flow.'
      }
    : {
        title: isChinese.value ? '修改策略暂不可写' : 'Edit access strategy is not writable yet',
        capability: isChinese.value ? '修改接入策略' : 'Edit access strategy',
        reason: isChinese.value
          ? '当前页仍以接入证据和渠道语义为主，没有可提交的策略更新后端落点。'
          : 'This page remains an evidence-first access surface and has no backend destination for submitted strategy updates.',
        nextStep: isChinese.value
          ? '后续开放写接口时，再把表单和列表明细接入这里。'
          : 'Connect forms and row-level editing here only after a writable API is introduced.'
      }
  placeholderPayload.value = config
  placeholderDialogVisible.value = true
}

const refreshAudit = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    accessAuditPage.value = await getGovernanceQueryHistoryPage(
      {
        tenantId: form.tenantId,
        historyType: 'QUERY_EXECUTION',
        accessChannel: form.accessChannel === 'ALL' ? '' : form.accessChannel,
        sortBy: 'submittedAt',
        sortOrder: 'DESC',
        pageNo: 1,
        pageSize: 8
      },
      {
        requestPrefix: 'frontend-access-center-audit-page'
      }
    )
  } catch (error) {
    accessAuditPage.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const openAuditDetail = async historyId => {
  if (!historyId) {
    return
  }
  loading.detail = true
  errorMessage.value = ''
  try {
    selectedHistoryDetail.value = await getGovernanceQueryHistoryDetail(form.tenantId, historyId, {
      requestPrefix: 'frontend-access-center-audit-detail'
    })
    detailDialogVisible.value = true
  } catch (error) {
    selectedHistoryDetail.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

const channelCard = (channel, title, summary) => ({
  channel,
  title,
  summary
})

const field = (key, label, value) => ({ key, label, value })

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (value === null || value === undefined || String(value).trim() === '') {
    return '-'
  }
  return String(value)
}

const formatJson = value => JSON.stringify(value, null, 2)
const formatTimestamp = value => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

onMounted(() => {
  refreshAudit()
})
</script>

<template>
  <section class="access-page" data-testid="access-page">
    <SectionHeader
      :eyebrow="t('accessCenter.eyebrow')"
      :title="t('accessCenter.pageTitle')"
      :summary="t('accessCenter.boundarySummary')"
      :level="1"
      size="compact"
    />

    <ToolbarShell
      :eyebrow="t('accessCenter.filters.eyebrow')"
      :title="t('accessCenter.filters.title')"
      density="compact"
    >
      <div class="filter-grid">
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.tenant') }}</span>
          <el-input v-model="form.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('accessCenter.fields.accessChannel') }}</span>
          <el-select v-model="form.accessChannel">
            <el-option
              v-for="item in accessChannelOptions"
              :key="item.value"
              v-bind="{ label: item.label, value: item.value }"
            />
          </el-select>
        </label>
        <el-button type="primary" :loading="loading.page" data-testid="access-refresh" @click="refreshAudit">
          {{ t('accessCenter.actions.refresh') }}
        </el-button>
        <el-button @click="openPlaceholderAction('create')">
          {{ t('accessCenter.actions.createStrategy') }}
        </el-button>
        <el-button @click="openPlaceholderAction('edit')">
          {{ t('accessCenter.actions.editStrategy') }}
        </el-button>
        <el-button @click="policyDialogVisible = true">{{ t('accessCenter.actions.boundaryHelp') }}</el-button>
      </div>
    </ToolbarShell>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <section class="tab-stage">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="t('accessCenter.tabs.audit')" name="audit">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('accessCenter.audit.eyebrow')"
              :title="t('accessCenter.audit.title')"
              :summary="t('accessCenter.audit.summary')"
              size="compact"
            />
            <div class="chip-row">
              <span
                v-for="(value, key) in classificationSummary"
                :key="key"
                class="chip"
              >
                {{ key }}: {{ typeof value === 'object' ? Object.keys(value).length : value }}
              </span>
            </div>

            <p class="section-summary">{{ t('accessCenter.audit.boundary') }}</p>

            <el-table :data="auditItems" border>
              <el-table-column :label="t('accessCenter.fields.historyReport')" min-width="220">
                <template #default="{ row }">
                  <button
                    type="button"
                    class="table-link"
                    data-testid="access-audit-item"
                    @click="openAuditDetail(row.historyId)"
                  >
                    {{ row.reportCode || row.historyId }}
                  </button>
                  <div class="cell-subline">{{ row.historyId }}</div>
                </template>
              </el-table-column>
              <el-table-column prop="accessChannel" :label="t('accessCenter.fields.accessChannel')" min-width="130" />
              <el-table-column prop="resultStatus" :label="t('common.fields.status')" min-width="120" />
              <el-table-column prop="targetEngine" :label="t('common.fields.targetEngine')" min-width="120" />
              <el-table-column prop="submittedBy" :label="t('common.fields.submittedBy')" min-width="140" />
              <el-table-column :label="t('common.fields.submittedAt')" min-width="170">
                <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
              </el-table-column>
            </el-table>
            <footer class="table-pagination-state">
              {{ t('accessCenter.audit.state', { count: auditItems.length }) }}
            </footer>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('accessCenter.tabs.channels')" name="channels">
          <div class="tab-panel">
            <SectionHeader
              :eyebrow="t('accessCenter.channels.eyebrow')"
              :title="t('accessCenter.channels.title')"
              size="compact"
            />
            <div class="row-list">
              <article
                v-for="item in accessChannelCards"
                :key="item.channel"
                class="evidence-row"
                data-testid="access-channel-card"
              >
                <div>
                  <p class="section-kicker sqlforge-code-label">{{ item.channel }}</p>
                  <h3>{{ item.title }}</h3>
                </div>
                <p class="section-summary">{{ item.summary }}</p>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('accessCenter.tabs.jdbc')" name="jdbc">
          <div class="tab-panel">
            <el-table :data="jdbcAgentModes" border>
              <el-table-column prop="mode" :label="t('accessCenter.fields.mode')" min-width="180">
                <template #default="{ row }">
                  <span data-testid="access-jdbc-agent-mode">{{ row.mode }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="title" :label="t('common.fields.title')" min-width="160" />
              <el-table-column prop="summary" :label="t('common.fields.summary')" min-width="320" />
            </el-table>
            <footer class="table-pagination-state">
              {{ t('accessCenter.jdbc.state', { count: jdbcAgentModes.length }) }}
            </footer>

            <dl class="detail-grid">
              <div
                v-for="item in jdbcPolicyFields"
                :key="item.key"
                class="detail-grid__item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ displayValue(item.value) }}</dd>
              </div>
            </dl>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('accessCenter.tabs.sdk')" name="sdk">
          <dl class="detail-grid">
            <div
              v-for="item in sdkCards"
              :key="item.key"
              class="detail-grid__item"
              data-testid="access-sdk-card"
            >
              <dt>{{ item.label }}</dt>
              <dd>{{ displayValue(item.value) }}</dd>
            </div>
          </dl>
        </el-tab-pane>

        <el-tab-pane :label="t('accessCenter.tabs.policy')" name="policy">
          <dl class="detail-grid">
            <div class="detail-grid__item">
              <dt>GET /api/governance/access-audit</dt>
              <dd>query-history fallback</dd>
            </div>
            <div class="detail-grid__item">
              <dt>JDBC_AGENT</dt>
              <dd>OBSERVE / GOVERNED_EXECUTE / LOCAL_REWRITE_DIRECT_JDBC</dd>
            </div>
            <div class="detail-grid__item">
              <dt>SDK_QUERY_EXECUTE</dt>
              <dd>typed client + access audit</dd>
            </div>
          </dl>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="detailDialogVisible"
      v-bind="{ title: selectedHistoryDetail?.reportCode || selectedHistoryDetail?.historyId || t('accessCenter.detail.dialogTitle') }"
      width="760px"
    >
      <dl v-if="selectedHistoryDetail" class="detail-grid" data-testid="access-audit-detail">
        <div class="detail-grid__item">
          <dt>{{ t('common.fields.historyId') }}</dt>
          <dd>{{ displayValue(selectedHistoryDetail.historyId) }}</dd>
        </div>
        <div class="detail-grid__item">
          <dt>{{ t('common.fields.traceId') }}</dt>
          <dd>{{ displayValue(selectedHistoryDetail.traceId) }}</dd>
        </div>
        <div class="detail-grid__item">
          <dt>{{ t('accessCenter.fields.accessChannel') }}</dt>
          <dd>{{ displayValue(selectedHistoryDetail.accessChannel) }}</dd>
        </div>
        <div class="detail-grid__item">
          <dt>{{ t('common.fields.targetEngine') }}</dt>
          <dd>{{ displayValue(selectedHistoryDetail.targetEngine) }}</dd>
        </div>
        <div class="detail-grid__item">
          <dt>{{ t('common.fields.resultStatus') }}</dt>
          <dd>{{ displayValue(selectedHistoryDetail.resultStatus) }}</dd>
        </div>
        <div class="detail-grid__item">
          <dt>{{ t('common.fields.reportCode') }}</dt>
          <dd>{{ displayValue(selectedHistoryDetail.reportCode) }}</dd>
        </div>
      </dl>
      <template #footer>
        <el-button @click="rawDrawerVisible = true">{{ t('common.actions.viewRawEvidence') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="rawDrawerVisible" :title="t('accessCenter.rawDrawerTitle')" size="42%">
      <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="policyDialogVisible" :title="t('accessCenter.policy.dialogTitle')" width="680px">
      <dl class="detail-grid">
        <div class="detail-grid__item">
          <dt>GET /api/governance/access-audit</dt>
          <dd>query-history fallback</dd>
        </div>
        <div class="detail-grid__item">
          <dt>JDBC_AGENT</dt>
          <dd>OBSERVE / GOVERNED_EXECUTE / LOCAL_REWRITE_DIRECT_JDBC</dd>
        </div>
        <div class="detail-grid__item">
          <dt>SDK_QUERY_EXECUTE</dt>
          <dd>typed client + access audit</dd>
        </div>
      </dl>
    </el-dialog>

    <CapabilityPlaceholderDialog
      v-model="placeholderDialogVisible"
      :title="placeholderPayload.title"
      :capability="placeholderPayload.capability"
      :reason="placeholderPayload.reason"
      :next-step="placeholderPayload.nextStep"
    />
  </section>
</template>

<style scoped>
.access-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.filter-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-4);
  align-items: end;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 210px;
}

.field-label {
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.tab-stage,
.evidence-row,
.detail-grid__item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(35, 35, 35, 0.72);
}

.tab-stage {
  padding: var(--sqlforge-space-5);
}

.section-kicker {
  margin: 0 0 6px;
  color: var(--sqlforge-text-muted);
}

.section-summary,
.evidence-row h3 {
  margin: 0;
}

.section-summary,
.cell-subline {
  color: var(--sqlforge-text-secondary);
}

.inline-banner {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.tab-panel,
.row-list {
  display: grid;
  gap: var(--sqlforge-space-4);
}

.chip-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--sqlforge-space-2);
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.table-link {
  border: none;
  background: transparent;
  color: var(--sqlforge-color-brand);
  padding: 0;
  cursor: pointer;
}

.cell-subline {
  margin-top: 4px;
  font-size: 12px;
}

.evidence-row {
  display: grid;
  gap: var(--sqlforge-space-3);
  padding: 14px 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid__item {
  display: grid;
  gap: var(--sqlforge-space-2);
  padding: 12px 14px;
}

.detail-grid__item dt {
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.detail-grid__item dd {
  margin: 0;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
  overflow-wrap: anywhere;
}

.table-pagination-state {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
