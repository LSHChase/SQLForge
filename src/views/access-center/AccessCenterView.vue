<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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

const activeTab = ref('overview')
const policyDialogVisible = ref(false)
const auditDrawerVisible = ref(false)
const accessAuditPage = ref(null)
const selectedHistoryId = ref('')
const selectedHistoryDetail = ref(null)
const errorMessage = ref('')

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

const refreshAudit = async () => {
  loading.page = true
  errorMessage.value = ''
  try {
    accessAuditPage.value = await getGovernanceQueryHistoryPage(
      {
        tenantId: form.tenantId,
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
    const firstHistoryId = accessAuditPage.value?.items?.[0]?.historyId || ''
    if (firstHistoryId) {
      await loadAuditDetail(firstHistoryId)
    } else {
      selectedHistoryId.value = ''
      selectedHistoryDetail.value = null
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadAuditDetail = async historyId => {
  if (!historyId) {
    selectedHistoryId.value = ''
    selectedHistoryDetail.value = null
    return
  }
  loading.detail = true
  errorMessage.value = ''
  selectedHistoryId.value = historyId
  try {
    selectedHistoryDetail.value = await getGovernanceQueryHistoryDetail(form.tenantId, historyId, {
      requestPrefix: 'frontend-access-center-audit-detail'
    })
    auditDrawerVisible.value = true
  } catch (error) {
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

onMounted(() => {
  refreshAudit()
})
</script>

<template>
  <section class="access-page" data-testid="access-page">
    <header class="page-hero shell-panel">
      <div>
        <p class="section-kicker sqlforge-code-label">{{ isChinese ? '接入筛选' : 'Access filters' }}</p>
        <h2>{{ isChinese ? '租户、来源与接入审计抽样' : 'Tenant, channel, and sampled access audits' }}</h2>
        <p class="hero-summary">
          {{
            isChinese
              ? '主页面只保留筛选、总览和样例；原始证据进入抽屉，不再直接堆在首页。'
              : 'The main page stays limited to filters, overview, and samples. Raw evidence lives in drawers instead of the first screen.'
          }}
        </p>
      </div>
      <div class="hero-actions">
        <label class="field-label">
          <span>{{ isChinese ? '租户' : 'Tenant' }}</span>
          <input v-model.trim="form.tenantId" class="text-input">
        </label>
        <label class="field-label">
          <span>{{ isChinese ? '接入来源' : 'Access channel' }}</span>
          <select v-model="form.accessChannel" class="text-input">
            <option value="ALL">ALL</option>
            <option value="PAGE">PAGE</option>
            <option value="API">API</option>
            <option value="JDBC_AGENT">JDBC_AGENT</option>
            <option value="SDK">SDK</option>
            <option value="CLIENT">CLIENT</option>
          </select>
        </label>
        <button class="primary-button" data-testid="access-refresh" @click="refreshAudit">
          {{ isChinese ? '刷新接入证据' : 'Refresh access evidence' }}
        </button>
      </div>
    </header>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

    <main class="shell-panel workspace-panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="isChinese ? '接入总览' : 'Access overview'" name="overview">
          <div class="tab-stack">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">access channels</p>
                <h2>{{ isChinese ? '接入渠道基线' : 'Access-channel baseline' }}</h2>
              </div>
              <button class="ghost-button" @click="policyDialogVisible = true">
                {{ isChinese ? '查看边界说明' : 'View policy boundary' }}
              </button>
            </div>
            <div class="card-grid">
              <article
                v-for="item in accessChannelCards"
                :key="item.channel"
                class="info-card"
                data-testid="access-channel-card"
              >
                <div class="info-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.channel }}</p>
                    <h3>{{ item.title }}</h3>
                  </div>
                </div>
                <p class="hero-summary">{{ item.summary }}</p>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'JDBC Agent' : 'JDBC Agent'" name="jdbc">
          <div class="tab-stack">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">jdbc agent</p>
                <h2>{{ isChinese ? 'JDBC Agent 策略' : 'JDBC Agent strategy' }}</h2>
              </div>
            </div>

            <div class="card-grid">
              <article
                v-for="item in jdbcAgentModes"
                :key="item.mode"
                class="info-card"
                data-testid="access-jdbc-agent-mode"
              >
                <div class="info-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.mode }}</p>
                    <h3>{{ item.title }}</h3>
                  </div>
                </div>
                <p class="hero-summary">{{ item.summary }}</p>
              </article>
            </div>

            <div class="summary-grid">
              <article v-for="item in jdbcPolicyFields" :key="item.key" class="summary-card">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'SDK / Client' : 'SDK / Client'" name="sdk">
          <div class="tab-stack">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">java sdk</p>
                <h2>{{ isChinese ? 'Java SDK 基线' : 'Java SDK baseline' }}</h2>
              </div>
            </div>
            <div class="summary-grid">
              <article v-for="item in sdkCards" :key="item.key" class="summary-card" data-testid="access-sdk-card">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '接入审计' : 'Access audit'" name="audit">
          <div class="tab-stack">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">access audit sample</p>
                <h2>{{ isChinese ? '接入审计样例' : 'Access-audit samples' }}</h2>
              </div>
              <span class="section-badge">{{ auditItems.length }}</span>
            </div>

            <p class="hero-summary">
              {{
                isChinese
                  ? '当前仓库还没有独立开放给前端的 `GET /api/governance/access-audit` 控制器，因此这里先用 query-history 的 `accessChannel` 过滤面呈现审计样例。'
                  : 'The repository does not yet expose a dedicated frontend controller for `GET /api/governance/access-audit`, so this page currently renders audit samples through the query-history surface filtered by `accessChannel`.'
              }}
            </p>

            <div class="pill-row">
              <span
                v-for="(value, key) in classificationSummary"
                :key="key"
                class="section-badge"
              >
                {{ key }}: {{ value }}
              </span>
            </div>

            <div class="audit-list">
              <button
                v-for="item in auditItems"
                :key="item.historyId"
                type="button"
                class="audit-item"
                :class="{ 'audit-item-active': selectedHistoryId === item.historyId }"
                data-testid="access-audit-item"
                @click="loadAuditDetail(item.historyId)"
              >
                <div class="info-card-header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">{{ item.accessChannel || 'UNKNOWN' }}</p>
                    <h3>{{ item.reportCode || item.historyId }}</h3>
                  </div>
                  <span class="section-badge">{{ item.resultStatus || '-' }}</span>
                </div>
                <p class="hero-summary">
                  {{ item.targetEngine || '-' }} · {{ item.datasourceCode || item.datasourceType || '-' }} · {{ item.submittedBy || '-' }}
                </p>
              </button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-dialog v-model="policyDialogVisible" :title="isChinese ? '接入边界说明' : 'Access boundary policy'" width="760px">
      <div class="summary-grid">
        <article v-for="item in jdbcPolicyFields.slice(0, 5)" :key="item.key" class="summary-card">
          <span class="summary-card-label">{{ item.label }}</span>
          <strong>{{ displayValue(item.value) }}</strong>
        </article>
      </div>
      <p class="hero-summary">
        {{
          isChinese
            ? '开放接入页只展示仓库已证实的模式、字段和审计语义，不把真实外部连通或未实现 controller 直接写成事实。'
            : 'The access page only presents modes, fields, and audit semantics that are evidenced in the repository. It does not present live external connectivity or unimplemented controllers as facts.'
        }}
      </p>
    </el-dialog>

    <el-drawer
      v-model="auditDrawerVisible"
      :title="isChinese ? 'Access Audit 详情' : 'Access audit detail'"
      size="40%"
      data-testid="access-audit-detail"
    >
      <p v-if="loading.detail" class="hero-summary">
        {{ isChinese ? '正在加载 access audit 详情…' : 'Loading access-audit detail…' }}
      </p>
      <pre v-else class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
    </el-drawer>
  </section>
</template>

<style scoped>
.access-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shell-panel,
.info-card,
.summary-card,
.audit-item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 18px;
  background: var(--sqlforge-surface-2);
}

.page-hero,
.hero-actions,
.workspace-panel,
.tab-stack,
.card-grid,
.summary-grid,
.pill-row,
.audit-list {
  display: grid;
  gap: 12px;
}

.page-hero {
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  padding: 20px;
}

.hero-summary,
.field-label {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-actions {
  align-content: start;
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
}

.text-input,
.primary-button,
.ghost-button {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.primary-button,
.ghost-button {
  cursor: pointer;
}

.primary-button {
  border-color: var(--sqlforge-text-primary);
}

.workspace-panel {
  padding: 20px;
}

.card-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
}

.info-card,
.summary-card,
.audit-item {
  padding: 16px;
}

.info-card-header,
.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.summary-card-label {
  display: inline-flex;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--sqlforge-text-muted);
}

.pill-row {
  grid-template-columns: repeat(auto-fit, minmax(120px, max-content));
}

.section-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.audit-item {
  text-align: left;
  cursor: pointer;
}

.audit-item-active {
  border-color: var(--sqlforge-color-brand-border);
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.error-banner {
  margin: 0;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(120, 28, 28, 0.18);
  border: 1px solid rgba(212, 96, 96, 0.35);
  color: #ffd6d6;
}

@media (max-width: 1100px) {
  .page-hero {
    grid-template-columns: 1fr;
  }
}
</style>
