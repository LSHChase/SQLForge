<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import {
  createGovernanceDatasource,
  createGovernanceDispatchPolicy,
  createGovernanceRedisRuleSource,
  createGovernanceReportInterface,
  formatRuntimeError,
  getGovernanceDatasourceDetail,
  getGovernanceDatasources,
  getGovernanceDispatchPolicies,
  getGovernanceMessageStats,
  getGovernanceRedisRuleSources,
  getGovernanceReportInterfaces,
  getGovernanceTenantConfig,
  retryGovernanceFailedMessages,
  testGovernanceDatasourceConnection,
  updateGovernanceDatasource,
  updateGovernanceRedisRuleSource,
  updateGovernanceReportInterface
} from '../../services/runtimeGateApi'

const { locale } = useI18n()

const form = reactive({
  tenantId: 'system'
})

const loading = reactive({
  page: false,
  datasourceTest: false,
  retry: false,
  datasourceSubmit: false,
  reportSubmit: false,
  redisSubmit: false,
  dispatchSubmit: false
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

const datasourceDialogVisible = ref(false)
const reportDialogVisible = ref(false)
const redisDialogVisible = ref(false)
const dispatchDialogVisible = ref(false)
const placeholderDialogVisible = ref(false)

const datasourceDialogMode = ref('create')
const reportDialogMode = ref('create')
const redisDialogMode = ref('create')
const placeholderPayload = ref({
  title: '',
  capability: '',
  reason: '',
  nextStep: ''
})

const datasourceForm = reactive(buildDatasourceForm())
const reportForm = reactive(buildReportForm())
const redisForm = reactive(buildRedisForm())
const dispatchForm = reactive(buildDispatchForm())

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
    isChinese.value ? '支持新增、修改、查看详情和 test-connection；仍不暴露原始凭证。' : 'Supports create, edit, detail, and test-connection while keeping raw credentials hidden.'
  ),
  field(
    'reportInterfaceBoundary',
    isChinese.value ? '报表接口边界' : 'Report-interface boundary',
    isChinese.value ? '支持新增与修改；列表仍强调 resolverStatus / unavailableReason 的证据语义。' : 'Supports create and edit while keeping resolverStatus and unavailableReason evidence-first.'
  ),
  field(
    'redisBoundary',
    isChinese.value ? 'Redis 规则源边界' : 'Redis rule-source boundary',
    isChinese.value ? '支持新增与修改，但列表仍保留 CONFIG_ONLY / SIMULATED_READY 等证据口径。' : 'Supports create and edit while preserving CONFIG_ONLY and SIMULATED_READY evidence semantics.'
  ),
  field(
    'dispatchBoundary',
    isChinese.value ? 'Dispatch 边界' : 'Dispatch boundary',
    isChinese.value ? '当前仓库支持新增 Dispatch 策略；修改动作仍缺写接口，因此保留 EXTERNAL_MODULE_REQUIRED 的执行边界说明。' : 'The repository supports creating dispatch policies. Editing still lacks a write API, so the EXTERNAL_MODULE_REQUIRED execution boundary remains explicit.'
  )
])

function buildDatasourceForm() {
  return {
    datasourceId: '',
    tenantId: form.tenantId,
    datasourceCode: '',
    datasourceName: '',
    connectionMode: 'JDBC',
    stage: 'PROD',
    jdbcUrl: '',
    apiBaseUrl: '',
    clientEndpoint: '',
    gatewayEndpoint: '',
    proxyEndpoint: '',
    authMode: 'PASSWORD',
    credentialMode: 'REF',
    credentialRef: '',
    credentialSecret: '',
    tlsEnabled: true,
    verifyPeer: true,
    readonly: true,
    enabled: true,
    timeoutMs: 3000
  }
}

function buildReportForm() {
  return {
    configId: '',
    tenantId: form.tenantId,
    datasourceCode: 'hetu_main',
    stage: 'PROD',
    sourceType: 'REST',
    endpointCode: '',
    endpointName: '',
    baseUrl: '',
    pathTemplate: '/reports/{reportCode}',
    httpMethod: 'GET',
    reportCodeParamName: 'report_code',
    sqlJsonPath: '$.sql',
    authMode: 'NONE',
    timeoutMs: 3000,
    enabled: true
  }
}

function buildRedisForm() {
  return {
    sourceId: '',
    tenantId: form.tenantId,
    sourceName: '',
    redisEndpoints: '',
    redisNamespace: 'sqlforge:routing',
    keyPattern: 'routing:*',
    authMode: 'PASSWORD',
    credentialRef: '',
    bypassOnUnavailable: true,
    enabled: true
  }
}

function buildDispatchForm() {
  return {
    policyId: '',
    tenantId: form.tenantId,
    policyName: '',
    dispatchType: 'PULL',
    targetEngine: 'HETU',
    targetDatasource: 'hetu_main',
    ackMode: 'MANUAL',
    pullWindowSeconds: 60,
    maxBatchSize: 50,
    retryStrategy: 'EXPONENTIAL_BACKOFF',
    enabled: true
  }
}

const card = (key, label, value) => ({ key, label, value })
const field = (key, label, value) => ({ key, label, value })

const resetFormState = (target, factory) => {
  Object.assign(target, factory())
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

const openDatasourceCreate = () => {
  datasourceDialogMode.value = 'create'
  resetFormState(datasourceForm, buildDatasourceForm)
  datasourceDialogVisible.value = true
}

const openDatasourceEdit = row => {
  datasourceDialogMode.value = 'edit'
  resetFormState(datasourceForm, buildDatasourceForm)
  Object.assign(datasourceForm, {
    datasourceId: row.datasourceId,
    tenantId: row.tenantId || form.tenantId,
    datasourceCode: row.datasourceCode || '',
    datasourceName: row.datasourceName || '',
    connectionMode: row.connectionMode || 'JDBC',
    stage: row.stage || 'PROD',
    jdbcUrl: row.jdbcUrl || '',
    apiBaseUrl: row.apiBaseUrl || '',
    clientEndpoint: row.clientEndpoint || '',
    gatewayEndpoint: row.gatewayEndpoint || '',
    proxyEndpoint: row.proxyEndpoint || '',
    authMode: row.authMode || 'PASSWORD',
    credentialMode: row.credentialMode || 'REF',
    credentialRef: row.credentialRef || '',
    tlsEnabled: row.tlsEnabled ?? true,
    verifyPeer: row.verifyPeer ?? true,
    readonly: row.readonly ?? true,
    enabled: row.enabled ?? true,
    timeoutMs: row.timeoutMs ?? 3000
  })
  datasourceDialogVisible.value = true
}

const submitDatasource = async () => {
  loading.datasourceSubmit = true
  errorMessage.value = ''
  const payload = {
    tenantId: datasourceForm.tenantId,
    datasourceCode: datasourceForm.datasourceCode,
    datasourceName: datasourceForm.datasourceName,
    connectionMode: datasourceForm.connectionMode,
    stage: datasourceForm.stage,
    jdbcUrl: datasourceForm.jdbcUrl,
    apiBaseUrl: datasourceForm.apiBaseUrl,
    clientEndpoint: datasourceForm.clientEndpoint,
    gatewayEndpoint: datasourceForm.gatewayEndpoint,
    proxyEndpoint: datasourceForm.proxyEndpoint,
    authMode: datasourceForm.authMode,
    credentialMode: datasourceForm.credentialMode,
    credentialRef: datasourceForm.credentialRef,
    credentialSecret: datasourceForm.credentialSecret,
    tlsEnabled: datasourceForm.tlsEnabled,
    verifyPeer: datasourceForm.verifyPeer,
    readonly: datasourceForm.readonly,
    enabled: datasourceForm.enabled,
    timeoutMs: Number(datasourceForm.timeoutMs)
  }
  try {
    await (datasourceDialogMode.value === 'create'
      ? createGovernanceDatasource(payload, { requestPrefix: 'frontend-system-datasource-create' })
      : updateGovernanceDatasource(datasourceForm.datasourceId, payload, { requestPrefix: 'frontend-system-datasource-update' }))
    datasourceDialogVisible.value = false
    await loadSystemEvidence()
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.datasourceSubmit = false
  }
}

const openReportCreate = () => {
  reportDialogMode.value = 'create'
  resetFormState(reportForm, buildReportForm)
  reportDialogVisible.value = true
}

const openReportEdit = row => {
  reportDialogMode.value = 'edit'
  resetFormState(reportForm, buildReportForm)
  Object.assign(reportForm, {
    configId: row.configId,
    tenantId: row.tenantId || form.tenantId,
    datasourceCode: row.datasourceCode || 'hetu_main',
    stage: row.stage || 'PROD',
    sourceType: row.sourceType || 'REST',
    endpointCode: row.endpointCode || '',
    endpointName: row.endpointName || '',
    baseUrl: row.baseUrl || '',
    pathTemplate: row.pathTemplate || '',
    httpMethod: row.httpMethod || 'GET',
    reportCodeParamName: row.reportCodeParamName || 'report_code',
    sqlJsonPath: row.sqlJsonPath || '$.sql',
    authMode: row.authMode || 'NONE',
    timeoutMs: row.timeoutMs ?? 3000,
    enabled: row.enabled ?? true
  })
  reportDialogVisible.value = true
}

const submitReportInterface = async () => {
  loading.reportSubmit = true
  errorMessage.value = ''
  const payload = {
    tenantId: reportForm.tenantId,
    datasourceCode: reportForm.datasourceCode,
    stage: reportForm.stage,
    sourceType: reportForm.sourceType,
    endpointCode: reportForm.endpointCode,
    endpointName: reportForm.endpointName,
    baseUrl: reportForm.baseUrl,
    pathTemplate: reportForm.pathTemplate,
    httpMethod: reportForm.httpMethod,
    reportCodeParamName: reportForm.reportCodeParamName,
    sqlJsonPath: reportForm.sqlJsonPath,
    authMode: reportForm.authMode,
    timeoutMs: Number(reportForm.timeoutMs),
    enabled: reportForm.enabled
  }
  try {
    await (reportDialogMode.value === 'create'
      ? createGovernanceReportInterface(payload, { requestPrefix: 'frontend-system-report-interface-create' })
      : updateGovernanceReportInterface(reportForm.configId, payload, { requestPrefix: 'frontend-system-report-interface-update' }))
    reportDialogVisible.value = false
    await loadSystemEvidence()
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.reportSubmit = false
  }
}

const openRedisCreate = () => {
  redisDialogMode.value = 'create'
  resetFormState(redisForm, buildRedisForm)
  redisDialogVisible.value = true
}

const openRedisEdit = row => {
  redisDialogMode.value = 'edit'
  resetFormState(redisForm, buildRedisForm)
  Object.assign(redisForm, {
    sourceId: row.sourceId,
    tenantId: row.tenantId || form.tenantId,
    sourceName: row.sourceName || '',
    redisEndpoints: row.redisEndpoints || '',
    redisNamespace: row.redisNamespace || '',
    keyPattern: row.keyPattern || 'routing:*',
    authMode: row.authMode || 'PASSWORD',
    credentialRef: row.credentialRef || '',
    bypassOnUnavailable: row.bypassOnUnavailable ?? true,
    enabled: row.enabled ?? true
  })
  redisDialogVisible.value = true
}

const submitRedisRuleSource = async () => {
  loading.redisSubmit = true
  errorMessage.value = ''
  const payload = {
    tenantId: redisForm.tenantId,
    sourceName: redisForm.sourceName,
    redisEndpoints: redisForm.redisEndpoints,
    redisNamespace: redisForm.redisNamespace,
    keyPattern: redisForm.keyPattern,
    authMode: redisForm.authMode,
    credentialRef: redisForm.credentialRef,
    bypassOnUnavailable: redisForm.bypassOnUnavailable,
    enabled: redisForm.enabled
  }
  try {
    await (redisDialogMode.value === 'create'
      ? createGovernanceRedisRuleSource(payload, { requestPrefix: 'frontend-system-redis-rule-source-create' })
      : updateGovernanceRedisRuleSource(redisForm.sourceId, payload, { requestPrefix: 'frontend-system-redis-rule-source-update' }))
    redisDialogVisible.value = false
    await loadSystemEvidence()
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.redisSubmit = false
  }
}

const openDispatchCreate = () => {
  resetFormState(dispatchForm, buildDispatchForm)
  dispatchDialogVisible.value = true
}

const openDispatchEditPlaceholder = row => {
  placeholderPayload.value = {
    title: isChinese.value ? '修改 Dispatch 策略暂不可写' : 'Edit dispatch policy is not writable yet',
    capability: isChinese.value ? `修改策略 ${row.policyId}` : `Edit policy ${row.policyId}`,
    reason: isChinese.value
      ? '当前后端仓库只开放 Dispatch 策略新增接口，没有提供 PUT / PATCH 更新接口。'
      : 'The current backend repository exposes only Dispatch-policy creation and does not provide a PUT or PATCH update endpoint.',
    nextStep: isChinese.value
      ? '如需真实修改动作，先补后端更新契约，再把表单切换为可提交。'
      : 'Add a backend update contract before converting this action into a real editable form.'
  }
  placeholderDialogVisible.value = true
}

const submitDispatchPolicy = async () => {
  loading.dispatchSubmit = true
  errorMessage.value = ''
  const payload = {
    tenantId: dispatchForm.tenantId,
    policyName: dispatchForm.policyName,
    dispatchType: dispatchForm.dispatchType,
    targetEngine: dispatchForm.targetEngine,
    targetDatasource: dispatchForm.targetDatasource,
    ackMode: dispatchForm.ackMode,
    pullWindowSeconds: Number(dispatchForm.pullWindowSeconds),
    maxBatchSize: Number(dispatchForm.maxBatchSize),
    retryStrategy: dispatchForm.retryStrategy,
    enabled: dispatchForm.enabled
  }
  try {
    await createGovernanceDispatchPolicy(payload, { requestPrefix: 'frontend-system-dispatch-policy-create' })
    dispatchDialogVisible.value = false
    await loadSystemEvidence()
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.dispatchSubmit = false
  }
}

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
              ? '默认首页保持数据源管理，但新增、修改、测试和重试动作现在都在同一工作台内可见。'
              : 'Datasource management remains the default landing state, while create, edit, test, and retry actions are now visible in the same workspace.'
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
        <el-button @click="openDatasourceCreate">{{ isChinese ? '新增数据源' : 'Create datasource' }}</el-button>
        <el-button @click="openReportCreate">{{ isChinese ? '新增报表接口' : 'Create report interface' }}</el-button>
        <el-button @click="openRedisCreate">{{ isChinese ? '新增 Redis 规则源' : 'Create Redis rule source' }}</el-button>
        <el-button @click="openDispatchCreate">{{ isChinese ? '新增 Dispatch 策略' : 'Create Dispatch policy' }}</el-button>
        <el-button @click="helpDialogVisible = true">{{ isChinese ? '边界说明' : 'Boundary help' }}</el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <section class="summary-grid">
      <article v-for="item in summaryCards" :key="item.key" class="summary-card">
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="surface-card tab-stage">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="isChinese ? '数据源管理' : 'Datasource management'" name="datasource">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">datasource actions</p>
              <h2 class="section-title">{{ isChinese ? '数据源列表' : 'Datasource list' }}</h2>
            </div>
            <el-button @click="openDatasourceCreate">{{ isChinese ? '新增数据源' : 'Create datasource' }}</el-button>
          </div>
          <el-table :data="datasources" border>
            <el-table-column prop="datasourceCode" :label="isChinese ? '编码' : 'Code'" min-width="160" />
            <el-table-column prop="datasourceName" :label="isChinese ? '名称' : 'Name'" min-width="180" />
            <el-table-column prop="connectionMode" :label="isChinese ? '连接模式' : 'Connection mode'" min-width="140" />
            <el-table-column prop="healthStatus" :label="isChinese ? '健康状态' : 'Health status'" min-width="140" />
            <el-table-column :label="isChinese ? '最后检查' : 'Last checked at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.lastCheckedAt) }}</template>
            </el-table-column>
            <el-table-column :label="isChinese ? '操作' : 'Actions'" min-width="260">
              <template #default="{ row }">
                <el-button text data-testid="system-datasource-card" @click="openDatasourceDetail(row.datasourceId)">
                  {{ isChinese ? '查看详情' : 'View detail' }}
                </el-button>
                <el-button text @click="openDatasourceEdit(row)">
                  {{ isChinese ? '修改' : 'Edit' }}
                </el-button>
                <el-button text data-testid="system-datasource-test" @click="runDatasourceTest(row.datasourceId)">
                  {{ isChinese ? '测试连接' : 'Test connection' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '报表接口' : 'Report interfaces'" name="report">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">report interface actions</p>
              <h2 class="section-title">{{ isChinese ? '报表接口' : 'Report interfaces' }}</h2>
            </div>
            <el-button @click="openReportCreate">{{ isChinese ? '新增报表接口' : 'Create report interface' }}</el-button>
          </div>
          <el-table :data="reportInterfaces" border>
            <el-table-column prop="endpointCode" :label="isChinese ? '接口编码' : 'Endpoint code'" min-width="170" />
            <el-table-column prop="resolverStatus" :label="isChinese ? '解析状态' : 'Resolver status'" min-width="150" />
            <el-table-column :label="isChinese ? '基础地址' : 'Base URL'" min-width="220">
              <template #default="{ row }">{{ maskValue(row.baseUrl) }}</template>
            </el-table-column>
            <el-table-column prop="pathTemplate" :label="isChinese ? '路径模板' : 'Path template'" min-width="220" />
            <el-table-column :label="isChinese ? '操作' : 'Action'" min-width="180">
              <template #default="{ row }">
                <el-button text @click="openPayloadDrawer(row.endpointCode || 'report interface', row)">
                  {{ isChinese ? '查看' : 'View' }}
                </el-button>
                <el-button text @click="openReportEdit(row)">
                  {{ isChinese ? '修改' : 'Edit' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'Redis 规则源' : 'Redis rule sources'" name="redis">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">redis rule-source actions</p>
              <h2 class="section-title">{{ isChinese ? 'Redis 规则源' : 'Redis rule sources' }}</h2>
            </div>
            <el-button @click="openRedisCreate">{{ isChinese ? '新增 Redis 规则源' : 'Create Redis rule source' }}</el-button>
          </div>
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
              <el-table-column :label="isChinese ? '操作' : 'Action'" min-width="190">
                <template #default="{ row }">
                  <el-button text data-testid="system-redis-rule-source-card" @click="openPayloadDrawer(row.sourceId || 'redis rule source', row)">
                    {{ isChinese ? '查看' : 'View' }}
                  </el-button>
                  <el-button text @click="openRedisEdit(row)">
                    {{ isChinese ? '修改' : 'Edit' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'Dispatch 策略' : 'Dispatch policies'" name="dispatch">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">dispatch policy actions</p>
              <h2 class="section-title">{{ isChinese ? 'Dispatch 策略' : 'Dispatch policies' }}</h2>
            </div>
            <el-button @click="openDispatchCreate">{{ isChinese ? '新增 Dispatch 策略' : 'Create Dispatch policy' }}</el-button>
          </div>
          <div data-testid="system-dispatch-policies">
            <el-table :data="dispatchPolicies" border>
              <el-table-column prop="policyId" :label="isChinese ? 'Policy ID' : 'Policy ID'" min-width="170" />
              <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="140" />
              <el-table-column prop="targetDatasource" :label="isChinese ? '目标数据源' : 'Target datasource'" min-width="160" />
              <el-table-column prop="ackMode" :label="isChinese ? 'Ack 模式' : 'Ack mode'" min-width="130" />
              <el-table-column :label="isChinese ? '状态' : 'Status'" min-width="200">
                <template #default="{ row }">{{ displayValue(row.executionBoundary || 'EXTERNAL_MODULE_REQUIRED') }}</template>
              </el-table-column>
              <el-table-column :label="isChinese ? '操作' : 'Action'" min-width="190">
                <template #default="{ row }">
                  <el-button text data-testid="system-dispatch-policy-card" @click="openPayloadDrawer(row.policyId || 'dispatch policy', row)">
                    {{ isChinese ? '查看' : 'View' }}
                  </el-button>
                  <el-button text @click="openDispatchEditPlaceholder(row)">
                    {{ isChinese ? '修改' : 'Edit' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '租户参数 / 权限' : 'Tenant params / permission'" name="tenant">
          <div class="tenant-stage">
            <div class="detail-grid" data-testid="system-tenant-params">
              <div v-for="item in tenantParamCards" :key="item.key" class="detail-grid__item">
                <span>{{ item.label }}</span>
                <strong>{{ displayValue(item.value) }}</strong>
              </div>
            </div>

            <div class="detail-grid" data-testid="system-permission-audit">
              <div v-for="item in permissionAuditCards" :key="item.key" class="detail-grid__item">
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

    <el-dialog v-model="datasourceDialogVisible" :title="datasourceDialogMode === 'create' ? (isChinese ? '新增数据源' : 'Create datasource') : (isChinese ? '修改数据源' : 'Edit datasource')" width="860px">
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">tenantId</span>
          <el-input v-model="datasourceForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '编码' : 'Code' }}</span>
          <el-input v-model="datasourceForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '名称' : 'Name' }}</span>
          <el-input v-model="datasourceForm.datasourceName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '连接模式' : 'Connection mode' }}</span>
          <el-input v-model="datasourceForm.connectionMode" />
        </label>
        <label class="field-block">
          <span class="field-label">Stage</span>
          <el-input v-model="datasourceForm.stage" />
        </label>
        <label class="field-block">
          <span class="field-label">timeoutMs</span>
          <el-input v-model="datasourceForm.timeoutMs" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">jdbcUrl</span>
          <el-input v-model="datasourceForm.jdbcUrl" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">apiBaseUrl</span>
          <el-input v-model="datasourceForm.apiBaseUrl" />
        </label>
        <label class="field-block">
          <span class="field-label">credentialRef</span>
          <el-input v-model="datasourceForm.credentialRef" />
        </label>
        <label class="field-block">
          <span class="field-label">credentialSecret</span>
          <el-input v-model="datasourceForm.credentialSecret" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">readonly</span>
          <el-switch v-model="datasourceForm.readonly" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">enabled</span>
          <el-switch v-model="datasourceForm.enabled" />
        </label>
      </div>
      <template #footer>
        <el-button @click="datasourceDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.datasourceSubmit" @click="submitDatasource">
          {{ datasourceDialogMode === 'create' ? (isChinese ? '新增' : 'Create') : (isChinese ? '保存修改' : 'Save changes') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialogVisible" :title="reportDialogMode === 'create' ? (isChinese ? '新增报表接口' : 'Create report interface') : (isChinese ? '修改报表接口' : 'Edit report interface')" width="860px">
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">tenantId</span>
          <el-input v-model="reportForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">datasourceCode</span>
          <el-input v-model="reportForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">endpointCode</span>
          <el-input v-model="reportForm.endpointCode" />
        </label>
        <label class="field-block">
          <span class="field-label">endpointName</span>
          <el-input v-model="reportForm.endpointName" />
        </label>
        <label class="field-block">
          <span class="field-label">sourceType</span>
          <el-input v-model="reportForm.sourceType" />
        </label>
        <label class="field-block">
          <span class="field-label">httpMethod</span>
          <el-input v-model="reportForm.httpMethod" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">baseUrl</span>
          <el-input v-model="reportForm.baseUrl" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">pathTemplate</span>
          <el-input v-model="reportForm.pathTemplate" />
        </label>
        <label class="field-block">
          <span class="field-label">reportCodeParamName</span>
          <el-input v-model="reportForm.reportCodeParamName" />
        </label>
        <label class="field-block">
          <span class="field-label">sqlJsonPath</span>
          <el-input v-model="reportForm.sqlJsonPath" />
        </label>
      </div>
      <template #footer>
        <el-button @click="reportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.reportSubmit" @click="submitReportInterface">
          {{ reportDialogMode === 'create' ? (isChinese ? '新增' : 'Create') : (isChinese ? '保存修改' : 'Save changes') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="redisDialogVisible" :title="redisDialogMode === 'create' ? (isChinese ? '新增 Redis 规则源' : 'Create Redis rule source') : (isChinese ? '修改 Redis 规则源' : 'Edit Redis rule source')" width="860px">
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">tenantId</span>
          <el-input v-model="redisForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">sourceName</span>
          <el-input v-model="redisForm.sourceName" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">redisEndpoints</span>
          <el-input v-model="redisForm.redisEndpoints" />
        </label>
        <label class="field-block">
          <span class="field-label">redisNamespace</span>
          <el-input v-model="redisForm.redisNamespace" />
        </label>
        <label class="field-block">
          <span class="field-label">keyPattern</span>
          <el-input v-model="redisForm.keyPattern" />
        </label>
        <label class="field-block">
          <span class="field-label">authMode</span>
          <el-input v-model="redisForm.authMode" />
        </label>
        <label class="field-block">
          <span class="field-label">credentialRef</span>
          <el-input v-model="redisForm.credentialRef" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">bypassOnUnavailable</span>
          <el-switch v-model="redisForm.bypassOnUnavailable" />
        </label>
      </div>
      <template #footer>
        <el-button @click="redisDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.redisSubmit" @click="submitRedisRuleSource">
          {{ redisDialogMode === 'create' ? (isChinese ? '新增' : 'Create') : (isChinese ? '保存修改' : 'Save changes') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchDialogVisible" :title="isChinese ? '新增 Dispatch 策略' : 'Create Dispatch policy'" width="760px">
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">tenantId</span>
          <el-input v-model="dispatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">policyName</span>
          <el-input v-model="dispatchForm.policyName" />
        </label>
        <label class="field-block">
          <span class="field-label">dispatchType</span>
          <el-input v-model="dispatchForm.dispatchType" />
        </label>
        <label class="field-block">
          <span class="field-label">targetEngine</span>
          <el-input v-model="dispatchForm.targetEngine" />
        </label>
        <label class="field-block">
          <span class="field-label">targetDatasource</span>
          <el-input v-model="dispatchForm.targetDatasource" />
        </label>
        <label class="field-block">
          <span class="field-label">ackMode</span>
          <el-input v-model="dispatchForm.ackMode" />
        </label>
        <label class="field-block">
          <span class="field-label">pullWindowSeconds</span>
          <el-input v-model="dispatchForm.pullWindowSeconds" />
        </label>
        <label class="field-block">
          <span class="field-label">maxBatchSize</span>
          <el-input v-model="dispatchForm.maxBatchSize" />
        </label>
        <label class="field-block">
          <span class="field-label">retryStrategy</span>
          <el-input v-model="dispatchForm.retryStrategy" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">enabled</span>
          <el-switch v-model="dispatchForm.enabled" />
        </label>
      </div>
      <template #footer>
        <el-button @click="dispatchDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.dispatchSubmit" @click="submitDispatchPolicy">
          {{ isChinese ? '新增策略' : 'Create policy' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="helpDialogVisible" :title="isChinese ? '系统管理边界说明' : 'System-management boundary guide'" width="760px">
      <div class="detail-grid">
        <div class="detail-grid__item">
          <span>CONFIG_ONLY</span>
          <strong>{{ isChinese ? 'Redis 规则源仍保留配置证据语义，但现在支持新增和修改。' : 'Redis rule sources keep config-evidence semantics while now supporting create and edit.' }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>EXTERNAL_MODULE_REQUIRED</span>
          <strong>{{ isChinese ? 'Dispatch 策略支持新增，但当前仍不伪装为浏览器内执行，也没有修改接口。' : 'Dispatch policies support creation, but they are still not presented as browser-executed workflows and still lack an edit API.' }}</strong>
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
.tenant-stage,
.table-heading,
.form-grid {
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
.tenant-stage,
.table-heading {
  flex-wrap: wrap;
  align-items: center;
}

.table-heading {
  justify-content: space-between;
  margin-bottom: 14px;
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

.detail-grid,
.form-grid {
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
  .detail-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
