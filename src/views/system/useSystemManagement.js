import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ackModeOptions,
  authModeOptions,
  buildDatasourceOptions,
  buildTenantOptions,
  connectionModeOptions,
  credentialModeOptions,
  dispatchTypeOptions,
  engineOptions,
  httpMethodOptions,
  retryStrategyOptions,
  sourceTypeOptions,
  stageOptions,
  withCurrentOption
} from '../common/formComponentGovernance'
import {
  createGovernanceDatasource,
  createGovernanceDispatchPolicy,
  createGovernanceRedisRuleSource,
  createGovernanceReportInterface,
  formatRuntimeError,
  getGovernanceDatasourceDriverDetail,
  getGovernanceDatasourceDrivers,
  getGovernanceDatasourceDetail,
  getGovernanceDatasources,
  getGovernanceDispatchPolicies,
  getGovernanceMessageStats,
  getGovernanceRedisRuleSources,
  getGovernanceReportInterfaces,
  getGovernanceTenantConfig,
  retryGovernanceFailedMessages,
  testGovernanceDatasourceConnection,
  uploadGovernanceDatasourceDriver,
  updateGovernanceDatasource,
  updateGovernanceRedisRuleSource,
  updateGovernanceReportInterface
} from '../../services/runtimeGateApi'

export function useSystemManagement() {
  const { t, locale } = useI18n()

  const form = reactive({
    tenantId: 'system'
  })

  const loading = reactive({
    page: false,
    datasourceTest: false,
    driverUpload: false,
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
  const datasourceDrivers = ref([])
  const reportInterfaces = ref([])
  const redisRuleSources = ref([])
  const dispatchPolicies = ref([])
  const datasourceTestResult = ref(null)
  const retryResult = ref(null)
  const selectedDatasourceId = ref('')
  const datasourceFilter = reactive({
    engineType: '',
    connectionMode: ''
  })

  const datasourceDialogVisible = ref(false)
  const driverDialogVisible = ref(false)
  const reportDialogVisible = ref(false)
  const redisDialogVisible = ref(false)
  const dispatchDialogVisible = ref(false)
  const placeholderDialogVisible = ref(false)

  const datasourceDialogMode = ref('create')
  const reportDialogMode = ref('create')
  const redisDialogMode = ref('create')
  const driverDetailLoading = ref(false)
  const placeholderPayload = ref({
    title: '',
    capability: '',
    reason: '',
    nextStep: ''
  })

  function buildDatasourceForm() {
    return {
      datasourceId: '',
      tenantId: form.tenantId,
      datasourceCode: '',
      datasourceName: '',
      engineType: 'HETU',
      connectionMode: 'JDBC',
      stage: 'PROD',
      jdbcUrl: '',
      jdbcDriverClassName: 'io.prestosql.jdbc.PrestoDriver',
      driverSourceType: 'CLASSPATH',
      driverArtifactId: '',
      driverVersionLabel: '',
      driverSha256: '',
      driverLoadStatus: '',
      username: '',
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

  function buildDriverUploadForm() {
    return {
      tenantId: form.tenantId,
      engineType: 'TRINO',
      driverClassName: 'io.trino.jdbc.TrinoDriver',
      versionLabel: '',
      file: null
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

  const datasourceForm = reactive(buildDatasourceForm())
  const driverUploadForm = reactive(buildDriverUploadForm())
  const reportForm = reactive(buildReportForm())
  const redisForm = reactive(buildRedisForm())
  const dispatchForm = reactive(buildDispatchForm())

  const isChinese = computed(() => locale.value === 'zh-CN')
  const card = (key, label, value) => ({ key, label, value })
  const field = (key, label, value) => ({ key, label, value })

  const tenantOptions = computed(() =>
    buildTenantOptions(
      form.tenantId,
      tenantConfig.value,
      datasources.value,
      reportInterfaces.value,
      redisRuleSources.value,
      dispatchPolicies.value
    )
  )
  const datasourceOptions = computed(() => buildDatasourceOptions(datasources.value))
  const datasourceDriverOptions = computed(() =>
    datasourceDrivers.value.map(item => ({
      label: `${item.engineType} · ${item.versionLabel} · ${item.originalFileName}`,
      value: item.artifactId
    }))
  )
  const filteredDatasources = computed(() =>
    datasources.value.filter(item => {
      const engineMatched = !datasourceFilter.engineType || item.engineType === datasourceFilter.engineType
      const modeMatched = !datasourceFilter.connectionMode || item.connectionMode === datasourceFilter.connectionMode
      return engineMatched && modeMatched
    })
  )
  const summaryCards = computed(() => [
    card('datasources', t('inline.viewsSystemUseSystemManagement.text001'), datasources.value.length),
    card('report-interfaces', t('inline.viewsSystemUseSystemManagement.text002'), reportInterfaces.value.length),
    card('redis-rule-sources', t('inline.viewsSystemUseSystemManagement.text003'), redisRuleSources.value.length),
    card('dispatch-policies', t('inline.viewsSystemUseSystemManagement.text004'), dispatchPolicies.value.length),
    card('pending', t('inline.viewsSystemUseSystemManagement.text005'), stats.value?.pending ?? 0),
    card('failed', t('inline.viewsSystemUseSystemManagement.text006'), stats.value?.failed ?? 0)
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
      t('inline.viewsSystemUseSystemManagement.text007'),
      t('inline.viewsSystemUseSystemManagement.text008')
    ),
    field(
      'reportInterfaceBoundary',
      t('inline.viewsSystemUseSystemManagement.text009'),
      t('inline.viewsSystemUseSystemManagement.text010')
    ),
    field(
      'redisBoundary',
      t('inline.viewsSystemUseSystemManagement.text011'),
      t('inline.viewsSystemUseSystemManagement.text012')
    ),
    field(
      'dispatchBoundary',
      t('inline.viewsSystemUseSystemManagement.text013'),
      t('inline.viewsSystemUseSystemManagement.text014')
    )
  ])

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
      const datasourceDriversResult = await Promise.allSettled([
        getGovernanceDatasourceDrivers(tenantId, { requestPrefix: 'frontend-system-datasource-drivers' })
      ])
      tenantConfig.value = nextTenantConfig
      stats.value = nextStats
      datasources.value = Array.isArray(nextDatasources) ? nextDatasources : []
      datasourceDrivers.value =
        datasourceDriversResult[0].status === 'fulfilled' && Array.isArray(datasourceDriversResult[0].value)
          ? datasourceDriversResult[0].value
          : []
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

  const openJdbcDatasourceCreate = engineType => {
    datasourceDialogMode.value = 'create'
    resetFormState(datasourceForm, buildDatasourceForm)
    const normalizedEngineType = String(engineType || 'HETU').toUpperCase()
    const driverClassNames = {
      HETU: 'io.prestosql.jdbc.PrestoDriver',
      HIVE: 'org.apache.hive.jdbc.HiveDriver',
      TRINO: 'io.trino.jdbc.TrinoDriver'
    }
    const datasourceCodes = {
      HETU: 'hetu_main',
      HIVE: 'hive_lakehouse',
      TRINO: 'trino_main'
    }
    const datasourceNames = {
      HETU: 'Hetu JDBC',
      HIVE: 'Hive JDBC',
      TRINO: 'Trino JDBC'
    }
    Object.assign(datasourceForm, {
      engineType: normalizedEngineType,
      connectionMode: 'JDBC',
      datasourceCode: datasourceCodes[normalizedEngineType] || 'jdbc_main',
      datasourceName: datasourceNames[normalizedEngineType] || 'JDBC datasource',
      jdbcDriverClassName: driverClassNames[normalizedEngineType] || '',
      driverSourceType: 'CLASSPATH',
      authMode: 'PASSWORD',
      credentialMode: 'PASSWORD',
      readonly: true,
      enabled: true
    })
    datasourceFilter.engineType = normalizedEngineType
    datasourceFilter.connectionMode = 'JDBC'
    datasourceDialogVisible.value = true
  }

  const openHetuJdbcCreate = () => openJdbcDatasourceCreate('HETU')
  const openHiveJdbcCreate = () => openJdbcDatasourceCreate('HIVE')
  const openTrinoJdbcCreate = () => openJdbcDatasourceCreate('TRINO')

  const openDatasourceEdit = row => {
    datasourceDialogMode.value = 'edit'
    resetFormState(datasourceForm, buildDatasourceForm)
    Object.assign(datasourceForm, {
      datasourceId: row.datasourceId,
      tenantId: row.tenantId || form.tenantId,
      datasourceCode: row.datasourceCode || '',
      datasourceName: row.datasourceName || '',
      engineType: row.engineType || 'HETU',
      connectionMode: row.connectionMode || 'JDBC',
      stage: row.stage || 'PROD',
      jdbcUrl: row.jdbcUrl || '',
      jdbcDriverClassName: row.jdbcDriverClassName || '',
      driverSourceType: row.driverSourceType || 'CLASSPATH',
      driverArtifactId: row.driverArtifactId || '',
      driverVersionLabel: row.driverVersionLabel || '',
      driverSha256: row.driverSha256 || '',
      driverLoadStatus: row.driverLoadStatus || '',
      username: row.username || '',
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

  const applyDriverArtifactSelection = artifactId => {
    datasourceForm.driverArtifactId = artifactId || ''
    const artifact = datasourceDrivers.value.find(item => item.artifactId === artifactId)
    if (!artifact) {
      datasourceForm.driverVersionLabel = ''
      datasourceForm.driverSha256 = ''
      datasourceForm.driverLoadStatus = ''
      return
    }
    datasourceForm.jdbcDriverClassName = artifact.driverClassName || datasourceForm.jdbcDriverClassName
    datasourceForm.driverVersionLabel = artifact.versionLabel || ''
    datasourceForm.driverSha256 = artifact.sha256 || ''
    datasourceForm.driverLoadStatus = artifact.status || ''
  }

  const openDriverUpload = () => {
    resetFormState(driverUploadForm, buildDriverUploadForm)
    driverDialogVisible.value = true
  }

  const handleDriverFileChange = payload => {
    const nativeFile = payload?.target?.files?.[0]
    driverUploadForm.file = nativeFile || payload?.raw || null
  }

  const clearDriverFile = () => {
    driverUploadForm.file = null
  }

  const inspectDriverArtifact = async artifactId => {
    if (!artifactId) {
      return
    }
    driverDetailLoading.value = true
    errorMessage.value = ''
    try {
      const detail = await getGovernanceDatasourceDriverDetail(form.tenantId, artifactId, {
        requestPrefix: 'frontend-system-datasource-driver-detail'
      })
      datasourceForm.driverVersionLabel = detail.versionLabel || ''
      datasourceForm.driverSha256 = detail.sha256 || ''
      datasourceForm.driverLoadStatus = detail.status || ''
      openPayloadDrawer(detail.originalFileName || artifactId, detail)
    } catch (error) {
      errorMessage.value = formatRuntimeError(error)
    } finally {
      driverDetailLoading.value = false
    }
  }

  const submitDriverUpload = async () => {
    loading.driverUpload = true
    errorMessage.value = ''
    try {
      await uploadGovernanceDatasourceDriver(driverUploadForm, {
        requestPrefix: 'frontend-system-datasource-driver-upload'
      })
      driverDialogVisible.value = false
      await loadSystemEvidence()
    } catch (error) {
      errorMessage.value = formatRuntimeError(error)
    } finally {
      loading.driverUpload = false
    }
  }

  const submitDatasource = async () => {
    loading.datasourceSubmit = true
    errorMessage.value = ''
    const payload = {
      tenantId: datasourceForm.tenantId,
      datasourceCode: datasourceForm.datasourceCode,
      datasourceName: datasourceForm.datasourceName,
      engineType: datasourceForm.engineType,
      connectionMode: datasourceForm.connectionMode,
      stage: datasourceForm.stage,
      jdbcUrl: datasourceForm.jdbcUrl,
      jdbcDriverClassName: datasourceForm.jdbcDriverClassName,
      driverSourceType: datasourceForm.driverSourceType,
      driverArtifactId: datasourceForm.driverSourceType === 'UPLOADED' ? datasourceForm.driverArtifactId : '',
      username: datasourceForm.username,
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
      title: t('inline.viewsSystemUseSystemManagement.text015'),
      capability: isChinese.value ? `修改策略 ${row.policyId}` : `Edit policy ${row.policyId}`,
      reason: t('inline.viewsSystemUseSystemManagement.text016'),
      nextStep: t('inline.viewsSystemUseSystemManagement.text017')
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

  return {
    ackModeOptions,
    authModeOptions,
    connectionModeOptions,
    credentialModeOptions,
    dispatchTypeOptions,
    engineOptions,
    httpMethodOptions,
    retryStrategyOptions,
    sourceTypeOptions,
    stageOptions,
    withCurrentOption,
    form,
    loading,
    activeTab,
    helpDialogVisible,
    detailDrawerVisible,
    testDialogVisible,
    detailTitle,
    detailPayload,
    errorMessage,
    reportInterfaces,
    redisRuleSources,
    dispatchPolicies,
    datasourceTestResult,
    retryResult,
    datasourceFilter,
    datasourceDialogVisible,
    driverDialogVisible,
    reportDialogVisible,
    redisDialogVisible,
    dispatchDialogVisible,
    placeholderDialogVisible,
    datasourceDialogMode,
    reportDialogMode,
    redisDialogMode,
    driverDetailLoading,
    placeholderPayload,
    datasourceForm,
    datasourceDrivers,
    datasourceDriverOptions,
    driverUploadForm,
    reportForm,
    redisForm,
    dispatchForm,
    isChinese,
    tenantOptions,
    datasourceOptions,
    filteredDatasources,
    summaryCards,
    tenantParamCards,
    permissionAuditCards,
    displayValue,
    maskValue,
    formatJson,
    formatTimestamp,
    loadSystemEvidence,
    openDatasourceDetail,
    runDatasourceTest,
    retryFailedMessages,
    openPayloadDrawer,
    openDatasourceCreate,
    openDriverUpload,
    openHiveJdbcCreate,
    openHetuJdbcCreate,
    openTrinoJdbcCreate,
    openDatasourceEdit,
    applyDriverArtifactSelection,
    clearDriverFile,
    handleDriverFileChange,
    inspectDriverArtifact,
    submitDatasource,
    submitDriverUpload,
    openReportCreate,
    openReportEdit,
    submitReportInterface,
    openRedisCreate,
    openRedisEdit,
    submitRedisRuleSource,
    openDispatchCreate,
    openDispatchEditPlaceholder,
    submitDispatchPolicy
  }
}
