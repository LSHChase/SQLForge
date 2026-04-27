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

const activeTab = ref('datasource')
const errorMessage = ref('')
const tenantConfig = ref(null)
const stats = ref(null)
const datasources = ref([])
const selectedDatasourceId = ref('')
const selectedDatasourceDetail = ref(null)
const datasourceDetailDrawerVisible = ref(false)
const datasourceTestResult = ref(null)
const datasourceTestDialogVisible = ref(false)
const reportInterfaces = ref([])
const reportInterfaceDrawerVisible = ref(false)
const selectedReportInterface = ref(null)
const redisRuleSources = ref([])
const redisRuleDrawerVisible = ref(false)
const selectedRedisRuleSource = ref(null)
const dispatchPolicies = ref([])
const dispatchPolicyDrawerVisible = ref(false)
const selectedDispatchPolicy = ref(null)
const retryResult = ref(null)

const isChinese = computed(() => locale.value === 'zh-CN')
const selectedDatasource = computed(() =>
  selectedDatasourceDetail.value ||
  datasources.value.find(item => item.datasourceId === selectedDatasourceId.value) ||
  null
)

const summaryCards = computed(() => [
  card('datasources', isChinese.value ? '数据源' : 'Datasources', datasources.value.length),
  card('report-interfaces', isChinese.value ? '报表接口' : 'Report interfaces', reportInterfaces.value.length),
  card('redis-rule-sources', isChinese.value ? 'Redis 规则源' : 'Redis rule sources', redisRuleSources.value.length),
  card('dispatch-policies', isChinese.value ? 'Dispatch 策略' : 'Dispatch policies', dispatchPolicies.value.length)
])

const queueCards = computed(() => {
  if (!stats.value) {
    return []
  }
  return [
    card('total', isChinese.value ? '消息总数' : 'Total messages', stats.value.total),
    card('pending', isChinese.value ? '待补偿' : 'Pending backlog', stats.value.pending),
    card('failed', isChinese.value ? '失败待修复' : 'Failed messages', stats.value.failed),
    card('consumed', isChinese.value ? '已消费' : 'Consumed', stats.value.consumed)
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

const card = (key, label, value) => ({ key, label, value })
const field = (key, label, value) => ({ key, label, value })

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

const openDatasourceDetail = async datasourceId => {
  await loadDatasourceDetail(datasourceId)
  datasourceDetailDrawerVisible.value = true
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
    datasourceTestDialogVisible.value = true
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

const openReportInterface = item => {
  selectedReportInterface.value = item
  reportInterfaceDrawerVisible.value = true
}

const openRedisRuleSource = item => {
  selectedRedisRuleSource.value = item
  redisRuleDrawerVisible.value = true
}

const openDispatchPolicy = item => {
  selectedDispatchPolicy.value = item
  dispatchPolicyDrawerVisible.value = true
}

onMounted(() => {
  loadSystemEvidence()
})
</script>

<template>
  <section class="system-page" data-testid="system-management-page">
    <header class="page-hero shell-panel">
      <div>
        <p class="section-kicker sqlforge-code-label">{{ isChinese ? '配置筛选' : 'Config filters' }}</p>
        <h2 class="page-title">{{ isChinese ? '租户配置、数据源与消息补偿入口' : 'Tenant config, datasources, and remediation entry' }}</h2>
        <p class="page-summary">
          {{
            isChinese
              ? '首屏只保留筛选和摘要，具体详情通过 tab、抽屉和测试结果弹窗展开。'
              : 'The first screen is limited to filters and summary cards. Detailed evidence expands through tabs, drawers, and test-result dialogs.'
          }}
        </p>
      </div>
      <div class="hero-actions">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
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

    <main class="shell-panel workspace-panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="isChinese ? '数据源管理' : 'Datasource management'" name="datasource">
          <div class="workspace-grid">
            <aside class="rail-panel">
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
                  @click="openDatasourceDetail(item.datasourceId)"
                >
                  <p class="summary-label sqlforge-code-label">{{ item.datasourceCode }}</p>
                  <h3 class="inventory-title">{{ item.datasourceName || item.datasourceCode }}</h3>
                  <p class="inventory-meta">{{ item.connectionMode }} · {{ item.healthStatus || 'UNKNOWN' }}</p>
                </button>
              </div>
            </aside>

            <section class="detail-stage">
              <div class="section-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">selected datasource</p>
                  <h2 class="section-title">{{ isChinese ? '当前数据源摘要' : 'Selected datasource summary' }}</h2>
                </div>
              </div>

              <div v-if="selectedDatasource" class="detail-grid">
                <article class="detail-card">
                  <span class="summary-label">{{ isChinese ? '连接模式' : 'Connection mode' }}</span>
                  <strong>{{ displayValue(selectedDatasource.connectionMode) }}</strong>
                </article>
                <article class="detail-card">
                  <span class="summary-label">{{ isChinese ? '阶段' : 'Stage' }}</span>
                  <strong>{{ displayValue(selectedDatasource.stage) }}</strong>
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
              <div v-else class="empty-state">
                {{ isChinese ? '请选择一个数据源查看详情。' : 'Select a datasource to inspect details.' }}
              </div>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '接口与规则源' : 'Interfaces and rule sources'" name="interfaces">
          <div class="stack-grid">
            <section class="inner-panel">
              <div class="section-heading">
                <div>
                  <p class="section-kicker sqlforge-code-label">report interfaces</p>
                  <h2 class="section-title">{{ isChinese ? '报表接口配置' : 'Report-interface config' }}</h2>
                </div>
              </div>
              <div class="inventory-grid">
                <button
                  v-for="item in reportInterfaces"
                  :key="item.configId || item.endpointCode"
                  type="button"
                  class="detail-card detail-card-button"
                  data-testid="system-report-interface-card"
                  @click="openReportInterface(item)"
                >
                  <span class="summary-label sqlforge-code-label">{{ item.endpointCode }}</span>
                  <strong>{{ item.endpointName || item.endpointCode }}</strong>
                  <p class="inventory-meta">{{ displayValue(item.sourceType) }} · {{ displayValue(item.httpMethod) }}</p>
                  <p class="inventory-meta">{{ displayValue(item.resolverStatus) }} · {{ displayValue(item.unavailableReason) }}</p>
                </button>
              </div>
            </section>

            <section class="workspace-grid compact-grid">
              <article class="inner-panel" data-testid="system-redis-rule-sources">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">redis rule sources</p>
                    <h2 class="section-title">{{ isChinese ? 'Redis 规则源' : 'Redis rule sources' }}</h2>
                  </div>
                </div>
                <div class="inventory-grid">
                  <button
                    v-for="item in redisRuleSources"
                    :key="item.sourceId"
                    type="button"
                    class="detail-card detail-card-button"
                    data-testid="system-redis-rule-source-card"
                    @click="openRedisRuleSource(item)"
                  >
                    <span class="summary-label sqlforge-code-label">{{ item.sourceName }}</span>
                    <strong>{{ displayValue(item.activationMode) }}</strong>
                    <p class="inventory-meta">{{ displayValue(item.healthStatus) }} · {{ displayValue(item.unavailableReason) }}</p>
                  </button>
                </div>
              </article>

              <article class="inner-panel" data-testid="system-dispatch-policies">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">dispatch policies</p>
                    <h2 class="section-title">{{ isChinese ? '装数协同策略' : 'Dispatch policies' }}</h2>
                  </div>
                </div>
                <div class="inventory-grid">
                  <button
                    v-for="item in dispatchPolicies"
                    :key="item.policyId"
                    type="button"
                    class="detail-card detail-card-button"
                    data-testid="system-dispatch-policy-card"
                    @click="openDispatchPolicy(item)"
                  >
                    <span class="summary-label sqlforge-code-label">{{ item.policyName }}</span>
                    <strong>{{ displayValue(item.dispatchType) }}</strong>
                    <p class="inventory-meta">{{ displayValue(item.targetEngine) }} · {{ displayValue(item.targetDatasource) }}</p>
                    <p class="inventory-meta">{{ displayValue(item.executionBoundary) }} · {{ displayValue(item.enforcementStatus) }}</p>
                  </button>
                </div>
              </article>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '参数与审计' : 'Parameters and audit'" name="config">
          <div class="workspace-grid compact-grid">
            <article class="inner-panel">
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

            <article class="inner-panel" data-testid="system-permission-audit">
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
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '消息补偿' : 'Message remediation'" name="queue">
          <section class="inner-panel">
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
                v-for="queueItem in queueCards"
                :key="queueItem.key"
                class="detail-card"
              >
                <span class="summary-label">{{ queueItem.label }}</span>
                <strong>{{ queueItem.value }}</strong>
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
          </section>
        </el-tab-pane>
      </el-tabs>
    </main>

    <el-drawer
      v-model="datasourceDetailDrawerVisible"
      :title="isChinese ? '数据源详情' : 'Datasource detail'"
      size="40%"
      data-testid="system-datasource-detail"
    >
      <div v-if="selectedDatasource" class="detail-grid">
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
      </div>
    </el-drawer>

    <el-dialog
      v-model="datasourceTestDialogVisible"
      :title="isChinese ? '连接测试结果' : 'Connection test result'"
      width="680px"
      data-testid="system-datasource-test-result"
    >
      <div class="detail-grid">
        <article class="detail-card">
          <span class="summary-label">{{ isChinese ? '连接状态' : 'Connection status' }}</span>
          <strong>{{ displayValue(datasourceTestResult?.connectionStatus) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">{{ isChinese ? '健康状态' : 'Health status' }}</span>
          <strong>{{ displayValue(datasourceTestResult?.healthStatus) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">readonlyBoundary</span>
          <strong>{{ displayValue(datasourceTestResult?.readonlyBoundary) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">{{ isChinese ? '检查时间' : 'Checked at' }}</span>
          <strong>{{ formatTimestamp(datasourceTestResult?.checkedAt) }}</strong>
        </article>
      </div>
    </el-dialog>

    <el-drawer v-model="reportInterfaceDrawerVisible" :title="isChinese ? '报表接口详情' : 'Report-interface detail'" size="36%">
      <div class="detail-grid">
        <article class="detail-card">
          <span class="summary-label">endpointCode</span>
          <strong>{{ displayValue(selectedReportInterface?.endpointCode) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">resolverStatus</span>
          <strong>{{ displayValue(selectedReportInterface?.resolverStatus) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">baseUrl</span>
          <strong>{{ maskValue(selectedReportInterface?.baseUrl) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">pathTemplate</span>
          <strong>{{ displayValue(selectedReportInterface?.pathTemplate) }}</strong>
        </article>
      </div>
    </el-drawer>

    <el-drawer v-model="redisRuleDrawerVisible" :title="isChinese ? 'Redis 规则源详情' : 'Redis rule source detail'" size="36%">
      <div class="detail-grid">
        <article class="detail-card">
          <span class="summary-label">redisEndpoints</span>
          <strong>{{ maskValue(selectedRedisRuleSource?.redisEndpoints) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">redisNamespace</span>
          <strong>{{ displayValue(selectedRedisRuleSource?.redisNamespace) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">keyPattern</span>
          <strong>{{ displayValue(selectedRedisRuleSource?.keyPattern) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">activationMode</span>
          <strong>{{ displayValue(selectedRedisRuleSource?.activationMode) }}</strong>
        </article>
      </div>
    </el-drawer>

    <el-drawer v-model="dispatchPolicyDrawerVisible" :title="isChinese ? 'Dispatch 策略详情' : 'Dispatch policy detail'" size="36%">
      <div class="detail-grid">
        <article class="detail-card">
          <span class="summary-label">targetEngine</span>
          <strong>{{ displayValue(selectedDispatchPolicy?.targetEngine) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">targetDatasource</span>
          <strong>{{ displayValue(selectedDispatchPolicy?.targetDatasource) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">ackMode</span>
          <strong>{{ displayValue(selectedDispatchPolicy?.ackMode) }}</strong>
        </article>
        <article class="detail-card">
          <span class="summary-label">retryStrategy</span>
          <strong>{{ displayValue(selectedDispatchPolicy?.retryStrategy) }}</strong>
        </article>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.system-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shell-panel,
.summary-card,
.inner-panel,
.rail-panel,
.detail-card,
.inventory-card,
.result-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 18px;
  background: var(--sqlforge-surface-2);
}

.page-hero,
.hero-actions,
.summary-grid,
.workspace-grid,
.stack-grid,
.compact-grid,
.detail-grid,
.inventory-grid,
.section-heading {
  display: grid;
  gap: 12px;
}

.page-hero {
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  padding: 20px;
}

.hero-actions {
  align-content: start;
}

.workspace-panel,
.inner-panel,
.rail-panel {
  padding: 20px;
}

.workspace-grid {
  grid-template-columns: minmax(280px, 0.88fr) minmax(0, 1.12fr);
}

.compact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.section-kicker,
.summary-label {
  color: var(--sqlforge-color-brand-text);
}

.section-kicker {
  margin: 0 0 8px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 12px;
}

.page-title,
.section-title,
.inventory-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.page-summary,
.inventory-meta,
.empty-state {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label,
.summary-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.text-input {
  min-height: 42px;
  padding: 10px 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
}

.pill-button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-text-primary);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  cursor: pointer;
}

.pill-button-primary {
  border-color: var(--sqlforge-color-brand-border);
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.summary-card,
.detail-card,
.inventory-card {
  padding: 14px 16px;
}

.inventory-card,
.detail-card-button {
  text-align: left;
  cursor: pointer;
}

.inventory-card-active {
  border-color: var(--sqlforge-color-brand-border);
  box-shadow: inset 0 0 0 1px rgba(16, 185, 129, 0.25);
}

.detail-stage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.result-panel {
  padding: 16px;
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
  .page-hero,
  .workspace-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
