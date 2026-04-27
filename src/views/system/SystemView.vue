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
  datasourceTest: false,
  retry: false
})

const activeTab = ref('datasource')
const helpDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const testDialogVisible = ref(false)
const detailTitle = ref('')
const detailPayload = ref(null)
const errorMessage = ref('')
const tenantConfig = ref(null)
const stats = ref(null)
const datasources = ref([])
const reportInterfaces = ref([])
const redisRuleSources = ref([])
const dispatchPolicies = ref([])
const datasourceTestResult = ref(null)
const retryResult = ref(null)
const selectedDatasourceId = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
const summaryCards = computed(() => [
  card('datasources', isChinese.value ? '数据源' : 'Datasources', datasources.value.length),
  card('report-interfaces', isChinese.value ? '报表接口' : 'Report interfaces', reportInterfaces.value.length),
  card('redis-rule-sources', isChinese.value ? 'Redis 规则源' : 'Redis rule sources', redisRuleSources.value.length),
  card('dispatch-policies', isChinese.value ? 'Dispatch 策略' : 'Dispatch policies', dispatchPolicies.value.length),
  card('pending', isChinese.value ? '待补偿' : 'Pending backlog', stats.value?.pending ?? 0),
  card('failed', isChinese.value ? '失败消息' : 'Failed messages', stats.value?.failed ?? 0)
].slice(0, 6))
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
    selectedDatasourceId.value = datasources.value[0]?.datasourceId || ''
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.page = false
  }
}

const openDatasourceDetail = async datasourceId => {
  try {
    detailPayload.value = await getGovernanceDatasourceDetail(form.tenantId, datasourceId, {
      requestPrefix: 'frontend-system-datasource-detail'
    })
    detailTitle.value = detailPayload.value.datasourceCode || datasourceId
    detailDrawerVisible.value = true
    selectedDatasourceId.value = datasourceId
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  }
}

const runDatasourceTest = async datasourceId => {
  loading.datasourceTest = true
  errorMessage.value = ''
  try {
    datasourceTestResult.value = await testGovernanceDatasourceConnection(
      datasourceId,
      form.tenantId,
      {},
      {
        requestPrefix: 'frontend-system-datasource-test'
      }
    )
    testDialogVisible.value = true
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

const openPayloadDrawer = (title, payload) => {
  detailTitle.value = title
  detailPayload.value = payload
  detailDrawerVisible.value = true
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

const formatJson = value => JSON.stringify(value, null, 2)
const formatTimestamp = value => (value ? String(value).replace('T', ' ').slice(0, 19) : '-')

onMounted(() => {
  loadSystemEvidence()
})
</script>

<template>
  <section class="system-page" data-testid="system-management-page">
    <header class="surface-card page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">{{ isChinese ? '系统管理' : 'System management' }}</p>
        <h1 class="section-title">{{ isChinese ? '数据源、接口与运行治理' : 'Datasources, interfaces, and runtime governance' }}</h1>
        <p class="section-summary">
          {{
            isChinese
              ? '默认首页是数据源管理，其余配置通过 tab、drawer 和测试弹窗展开。'
              : 'Datasource management is the default landing state, while other configuration surfaces open through tabs, drawers, and test dialogs.'
          }}
        </p>
      </div>
      <div class="action-row">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="form.tenantId" data-testid="system-tenant-input" />
        </label>
        <el-button type="primary" :loading="loading.page" data-testid="system-refresh" @click="loadSystemEvidence">
          {{ isChinese ? '刷新系统证据' : 'Refresh system evidence' }}
        </el-button>
        <el-button :loading="loading.retry" @click="retryFailedMessages">
          {{ isChinese ? '重试失败消息' : 'Retry failed messages' }}
        </el-button>
        <el-button @click="helpDialogVisible = true">{{ isChinese ? '边界说明' : 'Boundary help' }}</el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <section class="summary-grid">
      <article
        v-for="item in summaryCards"
        :key="item.key"
        class="summary-card"
      >
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="surface-card tab-stage">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="isChinese ? '数据源管理' : 'Datasource management'" name="datasource">
          <el-table :data="datasources" border>
            <el-table-column prop="datasourceCode" :label="isChinese ? '编码' : 'Code'" min-width="160" />
            <el-table-column prop="datasourceName" :label="isChinese ? '名称' : 'Name'" min-width="180" />
            <el-table-column prop="connectionMode" :label="isChinese ? '连接模式' : 'Connection mode'" min-width="140" />
            <el-table-column prop="healthStatus" :label="isChinese ? '健康状态' : 'Health status'" min-width="140" />
            <el-table-column :label="isChinese ? '最后检查' : 'Last checked at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.lastCheckedAt) }}</template>
            </el-table-column>
            <el-table-column :label="isChinese ? '操作' : 'Actions'" min-width="200">
              <template #default="{ row }">
                <el-button text data-testid="system-datasource-card" @click="openDatasourceDetail(row.datasourceId)">
                  {{ isChinese ? '查看详情' : 'View detail' }}
                </el-button>
                <el-button text data-testid="system-datasource-test" @click="runDatasourceTest(row.datasourceId)">
                  {{ isChinese ? '测试连接' : 'Test connection' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '报表接口' : 'Report interfaces'" name="report">
          <el-table :data="reportInterfaces" border>
            <el-table-column prop="endpointCode" :label="isChinese ? '接口编码' : 'Endpoint code'" min-width="170" />
            <el-table-column prop="resolverStatus" :label="isChinese ? '解析状态' : 'Resolver status'" min-width="150" />
            <el-table-column :label="isChinese ? '基础地址' : 'Base URL'" min-width="220">
              <template #default="{ row }">{{ maskValue(row.baseUrl) }}</template>
            </el-table-column>
            <el-table-column prop="pathTemplate" :label="isChinese ? '路径模板' : 'Path template'" min-width="220" />
            <el-table-column :label="isChinese ? '操作' : 'Action'" min-width="120">
              <template #default="{ row }">
                <el-button text @click="openPayloadDrawer(row.endpointCode || 'report interface', row)">
                  {{ isChinese ? '查看' : 'View' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'Redis 规则源' : 'Redis rule sources'" name="redis">
          <div data-testid="system-redis-rule-sources">
            <el-table :data="redisRuleSources" border>
              <el-table-column prop="sourceId" :label="isChinese ? 'Source ID' : 'Source ID'" min-width="170" />
              <el-table-column prop="activationMode" :label="isChinese ? '激活模式' : 'Activation mode'" min-width="150" />
              <el-table-column :label="isChinese ? '命名空间' : 'Namespace'" min-width="180">
                <template #default="{ row }">{{ displayValue(row.redisNamespace) || 'CONFIG_ONLY' }}</template>
              </el-table-column>
              <el-table-column :label="isChinese ? '端点' : 'Endpoints'" min-width="220">
                <template #default="{ row }">{{ maskValue(row.redisEndpoints) }}</template>
              </el-table-column>
              <el-table-column :label="isChinese ? '操作' : 'Action'" min-width="120">
                <template #default="{ row }">
                  <el-button text data-testid="system-redis-rule-source-card" @click="openPayloadDrawer(row.sourceId || 'redis rule source', row)">
                    {{ isChinese ? '查看' : 'View' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'Dispatch 策略' : 'Dispatch policies'" name="dispatch">
          <div data-testid="system-dispatch-policies">
            <el-table :data="dispatchPolicies" border>
              <el-table-column prop="policyId" :label="isChinese ? 'Policy ID' : 'Policy ID'" min-width="170" />
              <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="140" />
              <el-table-column prop="targetDatasource" :label="isChinese ? '目标数据源' : 'Target datasource'" min-width="160" />
              <el-table-column prop="ackMode" :label="isChinese ? 'Ack 模式' : 'Ack mode'" min-width="130" />
              <el-table-column :label="isChinese ? '状态' : 'Status'" min-width="180">
                <template #default="{ row }">{{ displayValue(row.dispatchBoundary || 'EXTERNAL_MODULE_REQUIRED') }}</template>
              </el-table-column>
              <el-table-column :label="isChinese ? '操作' : 'Action'" min-width="120">
                <template #default="{ row }">
                  <el-button text data-testid="system-dispatch-policy-card" @click="openPayloadDrawer(row.policyId || 'dispatch policy', row)">
                    {{ isChinese ? '查看' : 'View' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '租户参数 / 权限' : 'Tenant params / permission'" name="tenant">
          <div class="tenant-stage">
            <div class="detail-grid" data-testid="system-tenant-params">
              <div
                v-for="item in tenantParamCards"
                :key="item.key"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>

            <div class="detail-grid" data-testid="system-permission-audit">
              <div
                v-for="item in permissionAuditCards"
                :key="item.key"
                class="detail-grid__item"
              >
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>

            <div v-if="retryResult" class="inline-banner">
              {{ formatJson(retryResult) }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-drawer v-model="detailDrawerVisible" :title="detailTitle" size="42%">
      <pre class="code-block">{{ formatJson(detailPayload || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="testDialogVisible" :title="isChinese ? '连接测试结果' : 'Connection test result'" width="680px">
      <pre class="code-block">{{ formatJson(datasourceTestResult || {}) }}</pre>
    </el-dialog>

    <el-dialog v-model="helpDialogVisible" :title="isChinese ? '系统管理边界说明' : 'System-management boundary guide'" width="720px">
      <div class="detail-grid">
        <div class="detail-grid__item">
          <span>CONFIG_ONLY</span>
          <strong>{{ isChinese ? 'Redis 规则源只展示配置证据。' : 'Redis rule sources expose config-only evidence.' }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>EXTERNAL_MODULE_REQUIRED</span>
          <strong>{{ isChinese ? 'Dispatch 策略不伪装为浏览器内执行。' : 'Dispatch policies are not presented as browser-executed workflows.' }}</strong>
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.system-page {
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
.tenant-stage {
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
.section-summary {
  margin: 0;
}

.section-summary {
  color: var(--sqlforge-text-secondary);
}

.action-row,
.summary-grid,
.tenant-stage {
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
  min-width: 170px;
  flex: 1 1 170px;
}

.inline-banner {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
  white-space: pre-wrap;
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.tenant-stage {
  display: flex;
  flex-direction: column;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
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
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
