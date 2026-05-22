<script setup>
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import { useSystemManagement } from './useSystemManagement'

const { t } = useI18n()

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
  driverFileInputRef,
  reportForm,
  redisForm,
  dispatchForm,
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
} = useSystemManagement()
</script>

<template>
  <section class="system-page" data-testid="system-management-page">
    <header class="surface-card page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">{{ t('inline.viewsSystemSystemView.text001') }}</p>
        <h1 class="section-title">{{ t('inline.viewsSystemSystemView.text002') }}</h1>
        <p class="section-summary">
          {{
            t('inline.viewsSystemSystemView.text003')
          }}
        </p>
      </div>
      <div class="action-row">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsSystemSystemView.text004') }}</span>
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
          {{ t('inline.viewsSystemSystemView.text005') }}
        </el-button>
        <el-button :loading="loading.retry" @click="retryFailedMessages">
          {{ t('inline.viewsSystemSystemView.text006') }}
        </el-button>
        <el-button @click="openDatasourceCreate">{{ t('inline.viewsSystemSystemView.text007') }}</el-button>
        <el-button @click="openReportCreate">{{ t('inline.viewsSystemSystemView.text008') }}</el-button>
        <el-button @click="openRedisCreate">{{ t('inline.viewsSystemSystemView.text009') }}</el-button>
        <el-button @click="openDispatchCreate">{{ t('inline.viewsSystemSystemView.text010') }}</el-button>
        <el-button @click="helpDialogVisible = true">{{ t('inline.viewsSystemSystemView.text011') }}</el-button>
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
        <el-tab-pane :label="t('inline.viewsSystemSystemView.text012')" name="datasource">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">datasource actions</p>
              <h2 class="section-title">{{ t('inline.viewsSystemSystemView.text013') }}</h2>
            </div>
            <div class="action-row action-row-tight">
              <el-select v-model="datasourceFilter.engineType" clearable :placeholder="t('inline.viewsSystemSystemView.text014')" data-testid="system-datasource-engine-filter">
                <el-option
                  v-for="item in withCurrentOption(engineOptions, datasourceFilter.engineType)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-select v-model="datasourceFilter.connectionMode" clearable :placeholder="t('inline.viewsSystemSystemView.text015')" data-testid="system-datasource-mode-filter">
                <el-option
                  v-for="item in withCurrentOption(connectionModeOptions, datasourceFilter.connectionMode)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              <el-button data-testid="system-hetu-jdbc-create" @click="openHetuJdbcCreate">
                {{ t('inline.viewsSystemSystemView.text016') }}
              </el-button>
              <el-button @click="openHiveJdbcCreate">{{ t('inline.viewsSystemSystemView.text104') }}</el-button>
              <el-button @click="openTrinoJdbcCreate">{{ t('inline.viewsSystemSystemView.text105') }}</el-button>
              <el-button @click="openDatasourceCreate">{{ t('inline.viewsSystemSystemView.text017') }}</el-button>
            </div>
          </div>
          <el-table :data="filteredDatasources" border>
            <el-table-column prop="engineType" :label="t('inline.viewsSystemSystemView.text018')" min-width="110" />
            <el-table-column prop="datasourceCode" :label="t('inline.viewsSystemSystemView.text019')" min-width="160" />
            <el-table-column prop="datasourceName" :label="t('inline.viewsSystemSystemView.text020')" min-width="180" />
            <el-table-column prop="connectionMode" :label="t('inline.viewsSystemSystemView.text021')" min-width="140" />
            <el-table-column prop="credentialMask" :label="t('inline.viewsSystemSystemView.text022')" min-width="130" />
            <el-table-column prop="healthStatus" :label="t('inline.viewsSystemSystemView.text023')" min-width="140" />
            <el-table-column :label="t('inline.viewsSystemSystemView.text024')" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.lastCheckedAt) }}</template>
            </el-table-column>
            <el-table-column :label="t('inline.viewsSystemSystemView.text025')" min-width="260">
              <template #default="{ row }">
                <el-button text data-testid="system-datasource-card" @click="openDatasourceDetail(row.datasourceId)">
                  {{ t('inline.viewsSystemSystemView.text026') }}
                </el-button>
                <el-button text @click="openDatasourceEdit(row)">
                  {{ t('inline.viewsSystemSystemView.text027') }}
                </el-button>
                <el-button text data-testid="system-datasource-test" @click="runDatasourceTest(row.datasourceId)">
                  {{ t('inline.viewsSystemSystemView.text028') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsSystemSystemView.text106')" name="drivers">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">jdbc driver artifacts</p>
              <h2 class="section-title">{{ t('inline.viewsSystemSystemView.text107') }}</h2>
              <p class="driver-panel__hint">{{ t('inline.viewsSystemSystemView.text108') }}</p>
            </div>
            <div class="action-row action-row-tight">
              <el-button type="primary" @click="openDriverUpload">{{ t('inline.viewsSystemSystemView.text100') }}</el-button>
            </div>
          </div>
          <el-table :data="datasourceDrivers" border>
            <el-table-column prop="engineType" :label="t('inline.viewsSystemSystemView.text091')" min-width="110" />
            <el-table-column prop="versionLabel" :label="t('inline.viewsSystemSystemView.text092')" min-width="140" />
            <el-table-column prop="driverClassName" :label="t('inline.viewsSystemSystemView.text093')" min-width="220" />
            <el-table-column prop="originalFileName" :label="t('inline.viewsSystemSystemView.text094')" min-width="220" />
            <el-table-column prop="sha256" :label="t('inline.viewsSystemSystemView.text095')" min-width="220" show-overflow-tooltip />
            <el-table-column prop="status" :label="t('inline.viewsSystemSystemView.text096')" min-width="120" />
            <el-table-column :label="t('inline.viewsSystemSystemView.text097')" min-width="130">
              <template #default="{ row }">
                <el-button text :loading="driverDetailLoading" @click="inspectDriverArtifact(row.artifactId)">
                  {{ t('inline.viewsSystemSystemView.text109') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsSystemSystemView.text029')" name="report">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">report interface actions</p>
              <h2 class="section-title">{{ t('inline.viewsSystemSystemView.text030') }}</h2>
            </div>
            <el-button @click="openReportCreate">{{ t('inline.viewsSystemSystemView.text031') }}</el-button>
          </div>
          <el-table :data="reportInterfaces" border data-testid="system-report-interface-card">
            <el-table-column prop="endpointCode" :label="t('inline.viewsSystemSystemView.text032')" min-width="170" />
            <el-table-column prop="resolverStatus" :label="t('inline.viewsSystemSystemView.text033')" min-width="150" />
            <el-table-column :label="t('inline.viewsSystemSystemView.text034')" min-width="220">
              <template #default="{ row }">{{ maskValue(row.baseUrl) }}</template>
            </el-table-column>
            <el-table-column prop="pathTemplate" :label="t('inline.viewsSystemSystemView.text035')" min-width="220" />
            <el-table-column :label="t('inline.viewsSystemSystemView.text036')" min-width="180">
              <template #default="{ row }">
                <el-button text @click="openPayloadDrawer(row.endpointCode || 'report interface', row)">
                  {{ t('inline.viewsSystemSystemView.text037') }}
                </el-button>
                <el-button text @click="openReportEdit(row)">
                  {{ t('inline.viewsSystemSystemView.text038') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsSystemSystemView.text039')" name="redis">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">redis rule-source actions</p>
              <h2 class="section-title">{{ t('inline.viewsSystemSystemView.text040') }}</h2>
            </div>
            <el-button @click="openRedisCreate">{{ t('inline.viewsSystemSystemView.text041') }}</el-button>
          </div>
          <div data-testid="system-redis-rule-sources">
            <el-table :data="redisRuleSources" border>
              <el-table-column prop="sourceId" :label="t('inline.viewsSystemSystemView.text042')" min-width="170" />
              <el-table-column prop="activationMode" :label="t('inline.viewsSystemSystemView.text043')" min-width="150" />
              <el-table-column :label="t('inline.viewsSystemSystemView.text044')" min-width="180">
                <template #default="{ row }">{{ displayValue(row.redisNamespace) || 'CONFIG_ONLY' }}</template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text045')" min-width="220">
                <template #default="{ row }">{{ maskValue(row.redisEndpoints) }}</template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text046')" min-width="190">
                <template #default="{ row }">
                  <el-button text data-testid="system-redis-rule-source-card" @click="openPayloadDrawer(row.sourceId || 'redis rule source', row)">
                    {{ t('inline.viewsSystemSystemView.text047') }}
                  </el-button>
                  <el-button text @click="openRedisEdit(row)">
                    {{ t('inline.viewsSystemSystemView.text048') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsSystemSystemView.text049')" name="dispatch">
          <div class="table-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">dispatch policy actions</p>
              <h2 class="section-title">{{ t('inline.viewsSystemSystemView.text050') }}</h2>
            </div>
            <el-button @click="openDispatchCreate">{{ t('inline.viewsSystemSystemView.text051') }}</el-button>
          </div>
          <div data-testid="system-dispatch-policies">
            <el-table :data="dispatchPolicies" border>
              <el-table-column prop="policyId" :label="t('inline.viewsSystemSystemView.text052')" min-width="170" />
              <el-table-column prop="targetEngine" :label="t('inline.viewsSystemSystemView.text053')" min-width="140" />
              <el-table-column prop="targetDatasource" :label="t('inline.viewsSystemSystemView.text054')" min-width="160" />
              <el-table-column prop="ackMode" :label="t('inline.viewsSystemSystemView.text055')" min-width="130" />
              <el-table-column :label="t('inline.viewsSystemSystemView.text056')" min-width="200">
                <template #default="{ row }">{{ displayValue(row.executionBoundary || 'EXTERNAL_MODULE_REQUIRED') }}</template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text057')" min-width="190">
                <template #default="{ row }">
                  <el-button text data-testid="system-dispatch-policy-card" @click="openPayloadDrawer(row.policyId || 'dispatch policy', row)">
                    {{ t('inline.viewsSystemSystemView.text058') }}
                  </el-button>
                  <el-button text @click="openDispatchEditPlaceholder(row)">
                    {{ t('inline.viewsSystemSystemView.text059') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsSystemSystemView.text060')" name="tenant">
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

    <el-dialog v-model="testDialogVisible" :title="t('inline.viewsSystemSystemView.text061')" width="680px">
      <div class="detail-grid detail-grid-compact" data-testid="system-datasource-test-result">
        <div class="detail-grid__item">
          <span>{{ t('inline.viewsSystemSystemView.text062') }}</span>
          <strong>{{ datasourceTestResult?.realJdbcProbe ? 'true' : 'false' }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ t('inline.viewsSystemSystemView.text063') }}</span>
          <strong>{{ displayValue(datasourceTestResult?.connectionStatus) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ t('inline.viewsSystemSystemView.text064') }}</span>
          <strong>{{ displayValue(datasourceTestResult?.elapsedMs) }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>{{ t('inline.viewsSystemSystemView.text065') }}</span>
          <strong>{{ displayValue(datasourceTestResult?.lastFailureReason) }}</strong>
        </div>
      </div>
      <pre class="code-block">{{ formatJson(datasourceTestResult || {}) }}</pre>
    </el-dialog>

    <el-dialog v-model="datasourceDialogVisible" :title="datasourceDialogMode === 'create' ? (t('inline.viewsSystemSystemView.text066')) : (t('inline.viewsSystemSystemView.text067'))" width="860px">
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
          <span class="field-label">{{ t('inline.viewsSystemSystemView.text068') }}</span>
          <el-input v-model="datasourceForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsSystemSystemView.text069') }}</span>
          <el-input v-model="datasourceForm.datasourceName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsSystemSystemView.text070') }}</span>
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
          <span class="field-label">{{ t('inline.viewsSystemSystemView.text071') }}</span>
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
        <label class="field-block">
          <span class="field-label">driverSourceType</span>
          <el-select v-model="datasourceForm.driverSourceType">
            <el-option :label="t('inline.viewsSystemSystemView.text098')" value="CLASSPATH" />
            <el-option :label="t('inline.viewsSystemSystemView.text099')" value="UPLOADED" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">jdbcUrl</span>
          <el-input v-model="datasourceForm.jdbcUrl" />
        </label>
        <label class="field-block">
          <span class="field-label">jdbcDriverClassName</span>
          <el-input v-model="datasourceForm.jdbcDriverClassName" />
        </label>
        <label v-if="datasourceForm.driverSourceType === 'UPLOADED'" class="field-block field-block-wide">
          <span class="field-label">driverArtifactId</span>
          <div class="field-inline">
            <el-select
              v-model="datasourceForm.driverArtifactId"
              filterable
              clearable
              default-first-option
              @change="applyDriverArtifactSelection"
            >
              <el-option
                v-for="item in withCurrentOption(datasourceDriverOptions, datasourceForm.driverArtifactId)"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button :loading="driverDetailLoading" @click="inspectDriverArtifact(datasourceForm.driverArtifactId)">
              Inspect
            </el-button>
          </div>
        </label>
        <label v-if="datasourceForm.driverSourceType === 'UPLOADED'" class="field-block field-block-wide">
          <span class="field-label">driverArtifactMeta</span>
          <div class="detail-grid detail-grid-compact">
            <div class="detail-grid__item">
              <span>versionLabel</span>
              <strong>{{ displayValue(datasourceForm.driverVersionLabel) }}</strong>
            </div>
            <div class="detail-grid__item">
              <span>driverLoadStatus</span>
              <strong>{{ displayValue(datasourceForm.driverLoadStatus) }}</strong>
            </div>
            <div class="detail-grid__item field-span-full">
              <span>driverSha256</span>
              <strong class="monospace-text">{{ displayValue(datasourceForm.driverSha256) }}</strong>
            </div>
          </div>
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
        <el-button @click="datasourceDialogVisible = false">{{ t('inline.viewsSystemSystemView.text072') }}</el-button>
        <el-button type="primary" :loading="loading.datasourceSubmit" @click="submitDatasource">
          {{ datasourceDialogMode === 'create' ? (t('inline.viewsSystemSystemView.text073')) : (t('inline.viewsSystemSystemView.text074')) }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="driverDialogVisible" :title="t('inline.viewsSystemSystemView.text100')" width="760px">
      <div class="form-grid">
        <label class="field-block">
          <span class="field-label">tenantId</span>
          <el-select v-model="driverUploadForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, driverUploadForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">engineType</span>
          <el-select v-model="driverUploadForm.engineType">
            <el-option :label="t('inline.viewsSystemSystemView.text101')" value="TRINO" />
            <el-option :label="t('inline.viewsSystemSystemView.text102')" value="HETU" />
            <el-option :label="t('inline.viewsSystemSystemView.text103')" value="HIVE" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">driverClassName</span>
          <el-input v-model="driverUploadForm.driverClassName" />
        </label>
        <label class="field-block">
          <span class="field-label">versionLabel</span>
          <el-input v-model="driverUploadForm.versionLabel" />
        </label>
        <label class="field-block field-block-wide field-block-upload">
          <span class="field-label">{{ t('inline.viewsSystemSystemView.text110') }}</span>
          <input
            ref="driverFileInputRef"
            class="file-input"
            type="file"
            accept=".jar"
            @change="handleDriverFileChange"
          >
          <el-button text @click="clearDriverFile">{{ t('inline.viewsSystemSystemView.text114') }}</el-button>
          <span class="upload-file-name">
            {{ driverUploadForm.file?.name || t('inline.viewsSystemSystemView.text112') }}
          </span>
        </label>
      </div>
      <template #footer>
        <el-button @click="driverDialogVisible = false">{{ t('inline.viewsSystemSystemView.text072') }}</el-button>
        <el-button
          type="primary"
          :loading="loading.driverUpload"
          :disabled="!driverUploadForm.file"
          @click="submitDriverUpload"
        >
          {{ t('inline.viewsSystemSystemView.text113') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportDialogVisible" :title="reportDialogMode === 'create' ? (t('inline.viewsSystemSystemView.text075')) : (t('inline.viewsSystemSystemView.text076'))" width="860px">
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
        <el-button @click="reportDialogVisible = false">{{ t('inline.viewsSystemSystemView.text077') }}</el-button>
        <el-button type="primary" :loading="loading.reportSubmit" @click="submitReportInterface">
          {{ reportDialogMode === 'create' ? (t('inline.viewsSystemSystemView.text078')) : (t('inline.viewsSystemSystemView.text079')) }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="redisDialogVisible" :title="redisDialogMode === 'create' ? (t('inline.viewsSystemSystemView.text080')) : (t('inline.viewsSystemSystemView.text081'))" width="860px">
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
        <el-button @click="redisDialogVisible = false">{{ t('inline.viewsSystemSystemView.text082') }}</el-button>
        <el-button type="primary" :loading="loading.redisSubmit" @click="submitRedisRuleSource">
          {{ redisDialogMode === 'create' ? (t('inline.viewsSystemSystemView.text083')) : (t('inline.viewsSystemSystemView.text084')) }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchDialogVisible" :title="t('inline.viewsSystemSystemView.text085')" width="760px">
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
        <el-button @click="dispatchDialogVisible = false">{{ t('inline.viewsSystemSystemView.text086') }}</el-button>
        <el-button type="primary" :loading="loading.dispatchSubmit" @click="submitDispatchPolicy">
          {{ t('inline.viewsSystemSystemView.text087') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="helpDialogVisible" :title="t('inline.viewsSystemSystemView.text088')" width="760px">
      <div class="detail-grid">
        <div class="detail-grid__item">
          <span>CONFIG_ONLY</span>
          <strong>{{ t('inline.viewsSystemSystemView.text089') }}</strong>
        </div>
        <div class="detail-grid__item">
          <span>EXTERNAL_MODULE_REQUIRED</span>
          <strong>{{ t('inline.viewsSystemSystemView.text090') }}</strong>
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

.driver-panel {
  padding: 16px;
  margin-bottom: 16px;
}

.driver-panel__header {
  margin-bottom: 12px;
}

.driver-panel__hint {
  color: var(--sqlforge-text-secondary);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  min-width: 210px;
}

.field-block :deep(.el-select),
.field-block :deep(.el-input-number),
.field-block :deep(.el-upload) {
  width: 100%;
}

.field-inline {
  display: flex;
  gap: 8px;
}

.field-inline :deep(.el-select) {
  flex: 1;
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

.field-span-full {
  grid-column: 1 / -1;
}

.monospace-text {
  font-family: 'SFMono-Regular', 'Consolas', monospace;
  word-break: break-all;
}

.field-block-upload {
  align-items: flex-start;
}

.file-input {
  width: 100%;
  color: var(--sqlforge-text-primary);
}

.upload-file-name {
  color: var(--sqlforge-text-secondary);
  word-break: break-all;
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
