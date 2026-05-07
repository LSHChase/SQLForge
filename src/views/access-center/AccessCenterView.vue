<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import {
  formatRuntimeError,
  getGovernanceQueryHistoryDetail,
  getGovernanceQueryHistoryPage
} from '../../services/runtimeGateApi'

const { locale } = useI18n()

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
    <header class="surface-card page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">{{ isChinese ? '接入工作台' : 'Access workbench' }}</p>
        <h1 class="section-title">{{ isChinese ? '开放接入与审计样例' : 'Open access and audit samples' }}</h1>
        <p class="section-summary">
          {{
            isChinese
              ? '默认首页展示接入审计表格，其余通道策略放到次级 tab。'
              : 'The landing tab focuses on access-audit tables, while channel policies stay in secondary tabs.'
          }}
        </p>
      </div>
      <div class="action-row">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="form.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '接入渠道' : 'Access channel' }}</span>
          <el-select v-model="form.accessChannel">
            <el-option label="ALL" value="ALL" />
            <el-option label="PAGE" value="PAGE" />
            <el-option label="API" value="API" />
            <el-option label="JDBC_AGENT" value="JDBC_AGENT" />
            <el-option label="SDK" value="SDK" />
            <el-option label="CLIENT" value="CLIENT" />
          </el-select>
        </label>
        <el-button type="primary" :loading="loading.page" data-testid="access-refresh" @click="refreshAudit">
          {{ isChinese ? '刷新接入证据' : 'Refresh access evidence' }}
        </el-button>
        <el-button @click="openPlaceholderAction('create')">
          {{ isChinese ? '新增接入策略' : 'Create access strategy' }}
        </el-button>
        <el-button @click="openPlaceholderAction('edit')">
          {{ isChinese ? '修改策略' : 'Edit strategy' }}
        </el-button>
        <el-button @click="policyDialogVisible = true">{{ isChinese ? '边界说明' : 'Boundary help' }}</el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <section class="summary-grid">
      <article
        v-for="item in accessChannelCards"
        :key="item.channel"
        class="summary-card"
        data-testid="access-channel-card"
      >
        <span class="summary-card-label">{{ item.channel }}</span>
        <strong>{{ item.title }}</strong>
        <p>{{ item.summary }}</p>
      </article>
    </section>

    <section class="surface-card tab-stage">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="isChinese ? '接入审计' : 'Access audit'" name="audit">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">access audit sample</p>
              <h2 class="section-title">{{ isChinese ? '接入审计样例' : 'Access-audit samples' }}</h2>
            </div>
            <div class="chip-row">
              <span
                v-for="(value, key) in classificationSummary"
                :key="key"
                class="chip"
              >
                {{ key }}: {{ typeof value === 'object' ? Object.keys(value).length : value }}
              </span>
            </div>
          </div>

          <p class="section-summary">
            {{
              isChinese
                ? '当前仓库还没有独立开放给前端的 `GET /api/governance/access-audit` 控制器，因此这里先用 query-history 的 `accessChannel` 过滤面呈现审计样例。'
                : 'The repository does not yet expose a dedicated frontend controller for `GET /api/governance/access-audit`, so this page currently renders audit samples through the query-history surface filtered by `accessChannel`.'
            }}
          </p>

          <el-table :data="auditItems" border>
            <el-table-column :label="isChinese ? 'History / Report' : 'History / Report'" min-width="220">
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
            <el-table-column prop="accessChannel" :label="isChinese ? '接入渠道' : 'Access channel'" min-width="130" />
            <el-table-column prop="resultStatus" :label="isChinese ? '状态' : 'Status'" min-width="120" />
            <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="120" />
            <el-table-column prop="submittedBy" :label="isChinese ? '提交人' : 'Submitted by'" min-width="140" />
            <el-table-column :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'JDBC Agent' : 'JDBC Agent'" name="jdbc">
          <el-table :data="jdbcAgentModes" border>
            <el-table-column prop="mode" label="Mode" min-width="180">
              <template #default="{ row }">
                <span data-testid="access-jdbc-agent-mode">{{ row.mode }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" :label="isChinese ? '标题' : 'Title'" min-width="160" />
            <el-table-column prop="summary" :label="isChinese ? '说明' : 'Summary'" min-width="320" />
          </el-table>

          <div class="detail-grid">
            <div
              v-for="item in jdbcPolicyFields"
              :key="item.key"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'SDK / Client' : 'SDK / Client'" name="sdk">
          <div class="detail-grid">
            <div
              v-for="item in sdkCards"
              :key="item.key"
              class="detail-grid__item"
              data-testid="access-sdk-card"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="detailDialogVisible" :title="selectedHistoryDetail?.reportCode || selectedHistoryDetail?.historyId || 'access audit detail'" width="760px">
      <div v-if="selectedHistoryDetail" class="detail-grid" data-testid="access-audit-detail">
        <div class="detail-grid__item">
          <span>History ID</span>
          <strong>{{ displayValue(selectedHistoryDetail.historyId) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>Trace ID</span>
          <strong>{{ displayValue(selectedHistoryDetail.traceId) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '接入渠道' : 'Access channel' }}</span>
          <strong>{{ displayValue(selectedHistoryDetail.accessChannel) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '目标引擎' : 'Target engine' }}</span>
          <strong>{{ displayValue(selectedHistoryDetail.targetEngine) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '结果状态' : 'Result status' }}</span>
          <strong>{{ displayValue(selectedHistoryDetail.resultStatus) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '报表编码' : 'Report code' }}</span>
          <strong>{{ displayValue(selectedHistoryDetail.reportCode) }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="rawDrawerVisible = true">{{ isChinese ? '查看原始证据' : 'View raw evidence' }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="rawDrawerVisible" :title="isChinese ? '接入原始证据' : 'Raw access evidence'" size="42%">
      <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="policyDialogVisible" :title="isChinese ? '接入边界说明' : 'Access boundary guide'" width="680px">
      <div class="detail-grid">
        <div class="detail-grid__item">
          <span>GET /api/governance/access-audit</span>
          <strong>query-history fallback</strong>
        </div>
        <div class="detail-grid__item">
          <span>JDBC_AGENT</span>
          <strong>OBSERVE / GOVERNED_EXECUTE / LOCAL_REWRITE_DIRECT_JDBC</strong>
        </div>
        <div class="detail-grid__item">
          <span>SDK_QUERY_EXECUTE</span>
          <strong>typed client + access audit</strong>
        </div>
      </div>
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
  gap: 20px;
}

.surface-card,
.summary-card,
.field-block,
.detail-grid__item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    var(--sqlforge-surface-2);
}

.page-shell,
.tab-stage {
  padding: 20px;
}

.page-shell,
.action-row,
.summary-grid,
.chip-row {
  display: flex;
  gap: 12px;
}

.page-shell {
  align-items: flex-start;
  justify-content: space-between;
}

.section-kicker,
.field-label,
.summary-card-label {
  margin: 0 0 6px;
  color: var(--sqlforge-text-muted);
}

.section-title,
.section-summary,
.summary-card p {
  margin: 0;
}

.section-summary,
.summary-card p,
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

.action-row,
.summary-grid,
.chip-row {
  flex-wrap: wrap;
  align-items: center;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  min-width: 210px;
}

.summary-card {
  padding: 14px;
  min-width: 180px;
  flex: 1 1 180px;
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

.table-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
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

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .page-shell,
  .table-heading {
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
