<script setup>
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import { useSystemManagement } from './useSystemManagement'

useI18n()

const {
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
  reportDialogVisible,
  redisDialogVisible,
  dispatchDialogVisible,
  placeholderDialogVisible,
  datasourceDialogMode,
  reportDialogMode,
  redisDialogMode,
  placeholderPayload,
  datasourceForm,
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
  openHetuJdbcCreate,
  openDatasourceEdit,
  submitDatasource,
  openReportCreate,
  openReportEdit,
  submitReportInterface,
  openRedisCreate,
  openRedisEdit,
  submitRedisRuleSource,
  openDispatchCreate,
  openDispatchEditPlaceholder,
  submitDispatchPolicy
} = useSystemManagement()
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
          <el-select
            v-model="form.tenantId"
            filterable
            allow-create
            default-first-option
            data-testid="system-tenant-select"
          >
            <el-option v-for="item in tenantOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
            <div class="action-row action-row-tight">
              <el-select v-model="datasourceFilter.engineType" clearable :placeholder="isChinese ? '引擎' : 'Engine'" data-testid="system-datasource-engine-filter">
                <el-option
                  v-for="item in withCurrentOption(engineOptions, datasourceFilter.engineType)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-select v-model="datasourceFilter.connectionMode" clearable :placeholder="isChinese ? '模式' : 'Mode'" data-testid="system-datasource-mode-filter">
                <el-option
                  v-for="item in withCurrentOption(connectionModeOptions, datasourceFilter.connectionMode)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button data-testid="system-hetu-jdbc-create" @click="openHetuJdbcCreate">
                {{ isChinese ? 'Hetu JDBC 快捷创建' : 'Create Hetu JDBC' }}
              </el-button>
              <el-button @click="openDatasourceCreate">{{ isChinese ? '新增数据源' : 'Create datasource' }}</el-button>
            </div>
          </div>
          <el-table :data="filteredDatasources" border>
            <el-table-column prop="engineType" :label="isChinese ? '引擎' : 'Engine'" min-width="110" />
            <el-table-column prop="datasourceCode" :label="isChinese ? '编码' : 'Code'" min-width="160" />
            <el-table-column prop="datasourceName" :label="isChinese ? '名称' : 'Name'" min-width="180" />
            <el-table-column prop="connectionMode" :label="isChinese ? '连接模式' : 'Connection mode'" min-width="140" />
            <el-table-column prop="credentialMask" :label="isChinese ? '凭证' : 'Credential'" min-width="130" />
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
          <el-table :data="reportInterfaces" border data-testid="system-report-interface-card">
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
      <pre class="code-block" data-testid="system-datasource-detail">{{ formatJson(detailPayload || {}) }}</pre>
    </el-drawer>

    <el-dialog v-model="testDialogVisible" :title="isChinese ? '连接测试结果' : 'Connection test result'" width="680px">
      <div class="detail-grid detail-grid-compact" data-testid="system-datasource-test-result">
        <div class="detail-grid__item">
          <span>{{ isChinese ? '真实 JDBC 探测' : 'Real JDBC probe' }}</span>
          <strong>{{ datasourceTestResult?.realJdbcProbe ? 'true' : 'false' }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '连接状态' : 'Connection status' }}</span>
          <strong>{{ displayValue(datasourceTestResult?.connectionStatus) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '耗时 ms' : 'Elapsed ms' }}</span>
          <strong>{{ displayValue(datasourceTestResult?.elapsedMs) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ isChinese ? '失败原因' : 'Failure reason' }}</span>
          <strong>{{ displayValue(datasourceTestResult?.lastFailureReason) }}</strong>
        </div>
      </div>
      <pre class="code-block">{{ formatJson(datasourceTestResult || {}) }}</pre>
    </el-dialog>

    <el-dialog v-model="datasourceDialogVisible" :title="datasourceDialogMode === 'create' ? (isChinese ? '新增数据源' : 'Create datasource') : (isChinese ? '修改数据源' : 'Edit datasource')" width="860px">
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">tenantId</span>
          <el-select v-model="datasourceForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, datasourceForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <span class="field-label">{{ isChinese ? '引擎' : 'Engine' }}</span>
          <el-select v-model="datasourceForm.engineType" data-testid="system-datasource-engine-type">
            <el-option
              v-for="item in withCurrentOption(engineOptions, datasourceForm.engineType)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '连接模式' : 'Connection mode' }}</span>
          <el-select v-model="datasourceForm.connectionMode">
            <el-option
              v-for="item in withCurrentOption(connectionModeOptions, datasourceForm.connectionMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">Stage</span>
          <el-select v-model="datasourceForm.stage">
            <el-option
              v-for="item in withCurrentOption(stageOptions, datasourceForm.stage)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">timeoutMs</span>
          <el-input-number v-model="datasourceForm.timeoutMs" :min="100" :step="100" controls-position="right" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">jdbcUrl</span>
          <el-input v-model="datasourceForm.jdbcUrl" />
        </label>
        <label class="field-block">
          <span class="field-label">jdbcDriverClassName</span>
          <el-input v-model="datasourceForm.jdbcDriverClassName" />
        </label>
        <label class="field-block">
          <span class="field-label">username</span>
          <el-input v-model="datasourceForm.username" autocomplete="off" />
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
          <el-input v-model="datasourceForm.credentialSecret" type="password" show-password autocomplete="new-password" />
        </label>
        <label class="field-block">
          <span class="field-label">authMode</span>
          <el-select v-model="datasourceForm.authMode">
            <el-option
              v-for="item in withCurrentOption(authModeOptions, datasourceForm.authMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">credentialMode</span>
          <el-select v-model="datasourceForm.credentialMode">
            <el-option
              v-for="item in withCurrentOption(credentialModeOptions, datasourceForm.credentialMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">tlsEnabled</span>
          <el-switch v-model="datasourceForm.tlsEnabled" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">verifyPeer</span>
          <el-switch v-model="datasourceForm.verifyPeer" />
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
          <el-select v-model="reportForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, reportForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">datasourceCode</span>
          <el-select v-model="reportForm.datasourceCode" filterable allow-create default-first-option data-testid="system-report-datasource-select">
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, reportForm.datasourceCode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <span class="field-label">stage</span>
          <el-select v-model="reportForm.stage">
            <el-option
              v-for="item in withCurrentOption(stageOptions, reportForm.stage)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">sourceType</span>
          <el-select v-model="reportForm.sourceType">
            <el-option
              v-for="item in withCurrentOption(sourceTypeOptions, reportForm.sourceType)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">httpMethod</span>
          <el-select v-model="reportForm.httpMethod">
            <el-option
              v-for="item in withCurrentOption(httpMethodOptions, reportForm.httpMethod)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
        <label class="field-block">
          <span class="field-label">timeoutMs</span>
          <el-input-number v-model="reportForm.timeoutMs" :min="100" :step="100" controls-position="right" />
        </label>
        <label class="field-block">
          <span class="field-label">authMode</span>
          <el-select v-model="reportForm.authMode">
            <el-option
              v-for="item in withCurrentOption(authModeOptions, reportForm.authMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">enabled</span>
          <el-switch v-model="reportForm.enabled" />
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
          <el-select v-model="redisForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, redisForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          <el-select v-model="redisForm.authMode">
            <el-option
              v-for="item in withCurrentOption(authModeOptions, redisForm.authMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">credentialRef</span>
          <el-input v-model="redisForm.credentialRef" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">bypassOnUnavailable</span>
          <el-switch v-model="redisForm.bypassOnUnavailable" />
        </label>
        <label class="field-block field-block-toggle">
          <span class="field-label">enabled</span>
          <el-switch v-model="redisForm.enabled" />
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
          <el-select v-model="dispatchForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, dispatchForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">policyName</span>
          <el-input v-model="dispatchForm.policyName" />
        </label>
        <label class="field-block">
          <span class="field-label">dispatchType</span>
          <el-select v-model="dispatchForm.dispatchType">
            <el-option
              v-for="item in withCurrentOption(dispatchTypeOptions, dispatchForm.dispatchType)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">targetEngine</span>
          <el-select v-model="dispatchForm.targetEngine">
            <el-option
              v-for="item in withCurrentOption(engineOptions, dispatchForm.targetEngine)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">targetDatasource</span>
          <el-select v-model="dispatchForm.targetDatasource" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, dispatchForm.targetDatasource)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">ackMode</span>
          <el-select v-model="dispatchForm.ackMode">
            <el-option
              v-for="item in withCurrentOption(ackModeOptions, dispatchForm.ackMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">pullWindowSeconds</span>
          <el-input-number v-model="dispatchForm.pullWindowSeconds" :min="1" :step="10" controls-position="right" />
        </label>
        <label class="field-block">
          <span class="field-label">maxBatchSize</span>
          <el-input-number v-model="dispatchForm.maxBatchSize" :min="1" :step="10" controls-position="right" />
        </label>
        <label class="field-block">
          <span class="field-label">retryStrategy</span>
          <el-select v-model="dispatchForm.retryStrategy">
            <el-option
              v-for="item in withCurrentOption(retryStrategyOptions, dispatchForm.retryStrategy)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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

.tab-stage {
  overflow-x: auto;
}

.tab-stage :deep(.el-table) {
  min-width: 920px;
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

.field-block :deep(.el-select),
.field-block :deep(.el-input-number) {
  width: 100%;
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
