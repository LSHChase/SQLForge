<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getGovernanceDatasourceDetail,
  getGovernanceDatasources,
  getGovernanceDispatchPolicies,
  getGovernanceMessageStats,
  getGovernanceRedisRuleSources,
  getGovernanceReportInterfaces,
  getGovernanceTenantConfig,
  retryGovernanceFailedMessages,
  testGovernanceDatasourceConnection
} from '../../services/runtimeGateApi'

const { locale } = useI18n()

const form = reactive({
  tenantId: 'system'
})

const loading = reactive({
  page: false,
  datasourceDetail: false,
  datasourceTest: false,
  retry: false
})

const errorMessage = ref('')
const tenantConfig = ref(null)
const stats = ref(null)
const datasources = ref([])
const selectedDatasourceId = ref('')
const selectedDatasourceDetail = ref(null)
const datasourceTestResult = ref(null)
const reportInterfaces = ref([])
const redisRuleSources = ref([])
const dispatchPolicies = ref([])
const retryResult = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const selectedDatasource = computed(() =>
  selectedDatasourceDetail.value ||
  datasources.value.find(item => item.datasourceId === selectedDatasourceId.value) ||
  null
)

const summaryCards = computed(() => [
  {
    key: 'datasources',
    label: isChinese.value ? '数据源' : 'Datasources',
    value: datasources.value.length
  },
  {
    key: 'report-interfaces',
    label: isChinese.value ? '报表接口' : 'Report interfaces',
    value: reportInterfaces.value.length
  },
  {
    key: 'redis-rule-sources',
    label: isChinese.value ? 'Redis 规则源' : 'Redis rule sources',
    value: redisRuleSources.value.length
  },
  {
    key: 'dispatch-policies',
    label: isChinese.value ? 'Dispatch 策略' : 'Dispatch policies',
    value: dispatchPolicies.value.length
  }
])

const queueCards = computed(() => {
  if (!stats.value) {
    return []
  }
  return [
    { key: 'total', label: isChinese.value ? '消息总数' : 'Total messages', value: stats.value.total },
    { key: 'pending', label: isChinese.value ? '待补偿' : 'Pending backlog', value: stats.value.pending },
    { key: 'failed', label: isChinese.value ? '失败待修复' : 'Failed messages', value: stats.value.failed },
    { key: 'consumed', label: isChinese.value ? '已消费' : 'Consumed', value: stats.value.consumed }
  ]
})

const tenantParamCards = computed(() => {
  if (!tenantConfig.value) {
    return []
  }
  return [
    field('tenantId', 'tenantId', tenantConfig.value.tenantId),
    field('defaultEngine', 'defaultEngine', tenantConfig.value.defaultEngine),
    field('backupEngine', 'backupEngine', tenantConfig.value.backupEngine),
    field('auditLevel', 'auditLevel', tenantConfig.value.auditLevel),
    field('retentionDays', 'retentionDays', tenantConfig.value.retentionDays),
    field('quotaConcurrent', 'quotaConcurrent', tenantConfig.value.quotaConcurrent),
    field('accelerationQuota', 'accelerationQuota', tenantConfig.value.accelerationQuota)
  ]
})

const permissionAuditCards = computed(() => [
  field(
    'datasourceBoundary',
    isChinese.value ? '数据源边界' : 'Datasource boundary',
    isChinese.value ? '只读展示 + test-connection；不暴露原始凭证。' : 'Read-only rendering plus test-connection; raw credentials stay hidden.'
  ),
  field(
    'reportInterfaceBoundary',
    isChinese.value ? '报表接口边界' : 'Report-interface boundary',
    isChinese.value ? '展示 resolverStatus / unavailableReason，不把外部 API 默认写成已联通。' : 'Expose resolverStatus and unavailableReason without claiming live external connectivity by default.'
  ),
  field(
    'redisBoundary',
    isChinese.value ? 'Redis 规则源边界' : 'Redis rule-source boundary',
    isChinese.value ? '仅展示 CONFIG_ONLY / SIMULATED_READY 证据。' : 'Only exposes CONFIG_ONLY and SIMULATED_READY evidence.'
  ),
  field(
    'dispatchBoundary',
    isChinese.value ? 'Dispatch 边界' : 'Dispatch boundary',
    isChinese.value ? '显示 EXTERNAL_MODULE_REQUIRED，不把装数协同写成浏览器内已执行。' : 'Shows EXTERNAL_MODULE_REQUIRED instead of pretending dispatch is executed inside the browser.'
  )
])

const loadSystemEvidence = async () => {
  loading.page = true
  errorMessage.value = ''
  retryResult.value = null
  datasourceTestResult.value = null
  try {
    const tenantId = form.tenantId
    const [
      nextTenantConfig,
      nextStats,
      nextDatasources,
      nextReportInterfaces,
      nextRedisRuleSources,
      nextDispatchPolicies
    ] = await Promise.all([
      getGovernanceTenantConfig(tenantId, { requestPrefix: 'frontend-system-tenant-config' }),
      getGovernanceMessageStats(tenantId, { requestPrefix: 'frontend-system-message-stats' }),
      getGovernanceDatasources(tenantId, { requestPrefix: 'frontend-system-datasources' }),
      getGovernanceReportInterfaces(tenantId, { requestPrefix: 'frontend-system-report-interfaces' }),
      getGovernanceRedisRuleSources(tenantId, { requestPrefix: 'frontend-system-redis-rule-sources' }),
      getGovernanceDispatchPolicies(tenantId, { requestPrefix: 'frontend-system-dispatch-policies' })
    ])
    tenantConfig.value = nextTenantConfig
    stats.value = nextStats
    datasources.value = Array.isArray(nextDatasources) ? nextDatasources : []
    reportInterfaces.value = Array.isArray(nextReportInterfaces) ? nextReportInterfaces : []
    redisRuleSources.value = Array.isArray(nextRedisRuleSources) ? nextRedisRuleSources : []
    dispatchPolicies.value = Array.isArray(nextDispatchPolicies) ? nextDispatchPolicies : []

    const firstDatasourceId = selectedDatasourceId.value || datasources.value[0]?.datasourceId || ''
    if (firstDatasourceId) {
      await loadDatasourceDetail(firstDatasourceId)
    } else {
      selectedDatasourceId.value = ''
      selectedDatasourceDetail.value = null
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const loadDatasourceDetail = async datasourceId => {
  if (!datasourceId) {
    selectedDatasourceId.value = ''
    selectedDatasourceDetail.value = null
    return
  }

  loading.datasourceDetail = true
  errorMessage.value = ''
  selectedDatasourceId.value = datasourceId
  try {
    selectedDatasourceDetail.value = await getGovernanceDatasourceDetail(form.tenantId, datasourceId, {
      requestPrefix: 'frontend-system-datasource-detail'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.datasourceDetail = false
  }
}

const runDatasourceTest = async () => {
  if (!selectedDatasourceId.value) {
    return
  }
  loading.datasourceTest = true
  errorMessage.value = ''
  try {
    datasourceTestResult.value = await testGovernanceDatasourceConnection(
      selectedDatasourceId.value,
      form.tenantId,
      {},
      {
        requestPrefix: 'frontend-system-datasource-test'
      }
    )
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.datasourceTest = false
  }
}

const retryFailedMessages = async () => {
  loading.retry = true
  errorMessage.value = ''
  try {
    retryResult.value = await retryGovernanceFailedMessages(form.tenantId, {
      requestPrefix: 'frontend-system-message-retry'
    })
    stats.value = await getGovernanceMessageStats(form.tenantId, {
      requestPrefix: 'frontend-system-message-stats-after-retry'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.retry = false
  }
}

const displayValue = value => {
  if (value === null || value === undefined || String(value).trim() === '') {
    return '-'
  }
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  return String(value)
}

const maskValue = value => {
  const text = String(value || '').trim()
  if (!text) {
    return '-'
  }
  return text
    .replace(/\/\/([^/@]+)@/g, '//***@')
    .replace(/(jdbc:[^:]+:\/\/[^/]+\/).+/i, '$1***')
}

const formatTimestamp = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const field = (key, label, value) => ({ key, label, value })

onMounted(() => {
  loadSystemEvidence()
})
</script>

<template>
  <section class="system-page" data-testid="system-management-page">
    <header class="system-hero sqlforge-panel">
      <div>
        <p class="section-kicker sqlforge-code-label">system management</p>
        <h1 class="page-title">{{ isChinese ? '系统管理与治理配置中心' : 'System management and governance config center' }}</h1>
        <p class="page-summary">
          {{
            isChinese
              ? '当前页把 datasource、report-interface、Redis rule source、dispatch policy、tenant 参数与 message retry 修复动作统一到一个只读治理中心。'
              : 'This page consolidates datasource, report-interface, Redis rule source, dispatch policy, tenant parameters, and message-retry remediation into one read-oriented governance center.'
          }}
        </p>
      </div>
      <div class="hero-actions">
        <label class="field-label">
          <span>{{ isChinese ? '租户' : 'Tenant' }}</span>
          <input
            v-model.trim="form.tenantId"
            class="text-input"
            data-testid="system-tenant-input"
          >
        </label>
        <button class="pill-button pill-button-primary" data-testid="system-refresh" @click="loadSystemEvidence">
          {{ isChinese ? '刷新系统证据' : 'Refresh system evidence' }}
        </button>
      </div>
    </header>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>

    <section class="summary-grid">
      <article
        v-for="item in summaryCards"
        :key="item.key"
        class="summary-card"
      >
        <span class="summary-label">{{ item.label }}</span>
        <strong class="summary-value">{{ item.value }}</strong>
      </article>
    </section>

    <section class="section-grid">
      <article class="sqlforge-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">datasource management</p>
            <h2 class="section-title">{{ isChinese ? '数据源与连接测试' : 'Datasource and connection tests' }}</h2>
          </div>
          <button
            class="pill-button"
            :disabled="loading.datasourceTest || !selectedDatasourceId"
            data-testid="system-datasource-test"
            @click="runDatasourceTest"
          >
            {{ isChinese ? '测试连接' : 'Test connection' }}
          </button>
        </div>

        <div class="inventory-grid">
          <button
            v-for="item in datasources"
            :key="item.datasourceId"
            type="button"
            class="inventory-card"
            :class="{ 'inventory-card-active': item.datasourceId === selectedDatasourceId }"
            data-testid="system-datasource-card"
            @click="loadDatasourceDetail(item.datasourceId)"
          >
            <p class="summary-label sqlforge-code-label">{{ item.datasourceCode }}</p>
            <h3 class="inventory-title">{{ item.datasourceName || item.datasourceCode }}</h3>
            <p class="inventory-meta">{{ item.connectionMode }} · {{ item.healthStatus || 'UNKNOWN' }}</p>
          </button>
        </div>

        <div v-if="selectedDatasource" class="detail-panel" data-testid="system-datasource-detail">
          <div class="detail-grid">
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '连接模式' : 'Connection mode' }}</span>
              <strong>{{ displayValue(selectedDatasource.connectionMode) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '阶段' : 'Stage' }}</span>
              <strong>{{ displayValue(selectedDatasource.stage) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? 'JDBC URL' : 'JDBC URL' }}</span>
              <strong>{{ maskValue(selectedDatasource.jdbcUrl) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? 'API Base URL' : 'API Base URL' }}</span>
              <strong>{{ maskValue(selectedDatasource.apiBaseUrl) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '凭证模式' : 'Credential mode' }}</span>
              <strong>{{ displayValue(selectedDatasource.credentialMode) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '凭证掩码' : 'Credential mask' }}</span>
              <strong>{{ displayValue(selectedDatasource.credentialMask) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">readonly</span>
              <strong>{{ displayValue(selectedDatasource.readonly) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '实现阶段' : 'Implementation stage' }}</span>
              <strong>{{ displayValue(selectedDatasource.implementationStage) }}</strong>
            </article>
          </div>
        </div>

        <div v-if="datasourceTestResult" class="result-panel" data-testid="system-datasource-test-result">
          <div class="detail-grid">
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '连接状态' : 'Connection status' }}</span>
              <strong>{{ displayValue(datasourceTestResult.connectionStatus) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '健康状态' : 'Health status' }}</span>
              <strong>{{ displayValue(datasourceTestResult.healthStatus) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">readonlyBoundary</span>
              <strong>{{ displayValue(datasourceTestResult.readonlyBoundary) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '检查时间' : 'Checked at' }}</span>
              <strong>{{ formatTimestamp(datasourceTestResult.checkedAt) }}</strong>
            </article>
          </div>
        </div>
      </article>

      <article class="sqlforge-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">report interfaces</p>
            <h2 class="section-title">{{ isChinese ? '报表接口配置' : 'Report-interface config' }}</h2>
          </div>
        </div>
        <div class="inventory-grid">
          <article
            v-for="item in reportInterfaces"
            :key="item.configId || item.endpointCode"
            class="detail-card"
            data-testid="system-report-interface-card"
          >
            <span class="summary-label sqlforge-code-label">{{ item.endpointCode }}</span>
            <strong>{{ item.endpointName || item.endpointCode }}</strong>
            <p class="inventory-meta">{{ displayValue(item.sourceType) }} · {{ displayValue(item.httpMethod) }}</p>
            <p class="inventory-meta">{{ maskValue(item.baseUrl) }}{{ item.pathTemplate || '' }}</p>
            <p class="inventory-meta">{{ displayValue(item.resolverStatus) }} · {{ displayValue(item.unavailableReason) }}</p>
          </article>
        </div>
      </article>
    </section>

    <section class="section-grid">
      <article class="sqlforge-panel" data-testid="system-redis-rule-sources">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">redis rule sources</p>
            <h2 class="section-title">{{ isChinese ? 'Redis 规则源' : 'Redis rule sources' }}</h2>
          </div>
        </div>
        <div class="inventory-grid">
          <article
            v-for="item in redisRuleSources"
            :key="item.sourceId"
            class="detail-card"
            data-testid="system-redis-rule-source-card"
          >
            <span class="summary-label sqlforge-code-label">{{ item.sourceName }}</span>
            <strong>{{ displayValue(item.activationMode) }}</strong>
            <p class="inventory-meta">{{ maskValue(item.redisEndpoints) }}</p>
            <p class="inventory-meta">{{ displayValue(item.redisNamespace) }} · {{ displayValue(item.keyPattern) }}</p>
            <p class="inventory-meta">{{ displayValue(item.healthStatus) }} · {{ displayValue(item.unavailableReason) }}</p>
          </article>
        </div>
      </article>

      <article class="sqlforge-panel" data-testid="system-dispatch-policies">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">dispatch policies</p>
            <h2 class="section-title">{{ isChinese ? '装数协同策略' : 'Dispatch policies' }}</h2>
          </div>
        </div>
        <div class="inventory-grid">
          <article
            v-for="item in dispatchPolicies"
            :key="item.policyId"
            class="detail-card"
            data-testid="system-dispatch-policy-card"
          >
            <span class="summary-label sqlforge-code-label">{{ item.policyName }}</span>
            <strong>{{ displayValue(item.dispatchType) }}</strong>
            <p class="inventory-meta">{{ displayValue(item.targetEngine) }} · {{ displayValue(item.targetDatasource) }}</p>
            <p class="inventory-meta">{{ displayValue(item.ackMode) }} · {{ displayValue(item.retryStrategy) }}</p>
            <p class="inventory-meta">{{ displayValue(item.executionBoundary) }} · {{ displayValue(item.enforcementStatus) }}</p>
          </article>
        </div>
      </article>
    </section>

    <section class="section-grid">
      <article class="sqlforge-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">tenant parameters</p>
            <h2 class="section-title">{{ isChinese ? '系统参数快照' : 'System parameter snapshot' }}</h2>
          </div>
        </div>
        <div class="detail-grid" data-testid="system-tenant-params">
          <article
            v-for="item in tenantParamCards"
            :key="item.key"
            class="detail-card"
          >
            <span class="summary-label">{{ item.label }}</span>
            <strong>{{ displayValue(item.value) }}</strong>
          </article>
        </div>
      </article>

      <article class="sqlforge-panel" data-testid="system-permission-audit">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">permission audit</p>
            <h2 class="section-title">{{ isChinese ? '权限与边界审计' : 'Permission and boundary audit' }}</h2>
          </div>
        </div>
        <div class="detail-grid">
          <article
            v-for="item in permissionAuditCards"
            :key="item.key"
            class="detail-card"
          >
            <span class="summary-label">{{ item.label }}</span>
            <strong>{{ displayValue(item.value) }}</strong>
          </article>
        </div>
      </article>
    </section>

    <section class="section-grid">
      <article class="sqlforge-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">message remediation</p>
            <h2 class="section-title">{{ isChinese ? '消息补偿与重试' : 'Message remediation and retry' }}</h2>
          </div>
          <button class="pill-button" :disabled="loading.retry" data-testid="system-message-retry" @click="retryFailedMessages">
            {{ isChinese ? '重试失败消息' : 'Retry failed messages' }}
          </button>
        </div>
        <div class="detail-grid">
          <article
            v-for="card in queueCards"
            :key="card.key"
            class="detail-card"
          >
            <span class="summary-label">{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
          </article>
        </div>
        <div v-if="retryResult" class="result-panel">
          <div class="detail-grid">
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '重试状态' : 'Retry status' }}</span>
              <strong>{{ displayValue(retryResult.status) }}</strong>
            </article>
            <article class="detail-card">
              <span class="summary-label">{{ isChinese ? '重试数量' : 'Retried count' }}</span>
              <strong>{{ displayValue(retryResult.retriedCount) }}</strong>
            </article>
          </div>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.system-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sqlforge-panel,
.summary-card,
.inventory-card,
.detail-card,
.result-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 16px;
  background: var(--sqlforge-surface-2);
}

.sqlforge-panel {
  padding: 24px;
}

.system-hero,
.section-grid,
.summary-grid,
.inventory-grid,
.detail-grid {
  display: grid;
  gap: 18px;
}

.system-hero,
.section-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.inventory-grid,
.detail-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.page-title,
.section-title,
.inventory-title {
  margin: 0;
  font-weight: 400;
  color: var(--sqlforge-text-primary);
}

.page-title {
  font-size: clamp(34px, 5vw, 54px);
  line-height: 1.02;
}

.page-summary,
.inventory-meta,
.error-banner {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.section-kicker,
.summary-label,
.field-label span {
  margin: 0;
  color: var(--sqlforge-text-muted);
}

.hero-actions,
.section-heading {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.hero-actions {
  flex-direction: column;
}

.field-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.text-input,
.pill-button {
  min-height: 42px;
  border-radius: 999px;
}

.text-input {
  padding: 0 14px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.pill-button {
  cursor: pointer;
  padding: 0 18px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.pill-button-primary {
  border-color: var(--sqlforge-text-primary);
}

.summary-card,
.inventory-card,
.detail-card,
.result-panel {
  padding: 18px;
}

.summary-value {
  font-size: 28px;
  line-height: 1;
  color: var(--sqlforge-text-primary);
}

.inventory-card {
  text-align: left;
  cursor: pointer;
}

.inventory-card-active {
  border-color: var(--sqlforge-color-brand-border);
}

.detail-panel,
.result-panel {
  margin-top: 18px;
}

.error-banner {
  padding: 14px 16px;
  border: 1px solid rgba(212, 96, 96, 0.35);
  border-radius: 14px;
  background: rgba(120, 28, 28, 0.18);
  color: #ffd6d6;
}

@media (max-width: 1100px) {
  .system-hero,
  .section-grid {
    grid-template-columns: 1fr;
  }
}
</style>
