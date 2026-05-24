<script setup>
import { useI18n } from 'vue-i18n'
import CapabilityPlaceholderDialog from '../common/CapabilityPlaceholderDialog.vue'
import EvidencePanel from '../common/EvidencePanel.vue'
import MetricCard from '../common/MetricCard.vue'
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'
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
  driverFileInputRef,
  datasourceFormRef,
  driverUploadFormRef,
  reportFormRef,
  redisFormRef,
  dispatchFormRef,
  placeholderPayload,
  datasourceForm,
  datasourceDrivers,
  datasourceDriverOptions,
  driverUploadForm,
  reportForm,
  redisForm,
  dispatchForm,
  datasourceRules,
  driverUploadRules,
  reportRules,
  redisRules,
  dispatchRules,
  tenantOptions,
  datasourceOptions,
  filteredDatasources,
  summaryCards,
  tabSummaryMap,
  currentTabSummary,
  tenantParamCards,
  permissionAuditCards,
  displayValue,
  maskValue,
  formatJson,
  formatTimestamp,
  statusTagType,
  enabledTagType,
  enabledLabel,
  readonlyLabel,
  boundaryLabel,
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
  <!-- eslint-disable vue/multiline-html-element-content-newline, vue/html-self-closing, vue/html-indent -->
  <section class="system-page" data-testid="system-management-page">
    <SectionHeader
      :eyebrow="t('inline.viewsSystemSystemView.text001')"
      :title="t('inline.viewsSystemSystemView.text002')"
      :summary="t('inline.viewsSystemSystemView.text003')"
      :level="1"
    >
      <template #actions>
        <el-button @click="helpDialogVisible = true">{{ t('inline.viewsSystemSystemView.text011') }}</el-button>
      </template>
    </SectionHeader>

    <ToolbarShell class="system-context-shell" :eyebrow="t('inline.viewsSystemSystemView.text132')" density="compact">
      <label class="context-field">
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
      <div class="boundary-strip">
        <span class="boundary-chip">
          <strong>CONFIG_ONLY</strong>
          {{ t('inline.viewsSystemSystemView.text089') }}
        </span>
        <span class="boundary-chip">
          <strong>EXTERNAL_MODULE_REQUIRED</strong>
          {{ t('inline.viewsSystemSystemView.text090') }}
        </span>
      </div>
    </ToolbarShell>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">{{ errorMessage }}</div>

    <section class="metric-grid">
      <MetricCard
        v-for="item in summaryCards"
        :key="item.key"
        :label="item.label"
        :value="item.value"
        :detail="item.detail"
        :tone="item.tone"
      />
    </section>

    <EvidencePanel
      class="workspace-panel"
      :eyebrow="t('inline.viewsSystemSystemView.text001')"
      :title="t('inline.viewsSystemSystemView.text133')"
      :summary="currentTabSummary?.status"
    >
      <el-tabs v-model="activeTab" class="domain-tabs">
        <el-tab-pane name="datasource">
          <template #label>
            <span class="domain-tab-label" :class="`domain-tab-label-${tabSummaryMap.datasource.tone}`">
              <span>{{ t('inline.viewsSystemSystemView.text012') }}</span>
              <strong>{{ tabSummaryMap.datasource.count }}</strong>
            </span>
          </template>

          <div class="workspace-section">
            <SectionHeader
              eyebrow="datasource"
              :title="t('inline.viewsSystemSystemView.text013')"
              :summary="t('inline.viewsSystemSystemView.text134')"
              size="compact"
            >
              <template #actions>
                <el-button data-testid="system-hetu-jdbc-create" @click="openHetuJdbcCreate">
                  {{ t('inline.viewsSystemSystemView.text016') }}
                </el-button>
                <el-button @click="openHiveJdbcCreate">{{ t('inline.viewsSystemSystemView.text104') }}</el-button>
                <el-button @click="openTrinoJdbcCreate">{{ t('inline.viewsSystemSystemView.text105') }}</el-button>
                <el-button type="primary" @click="openDatasourceCreate">
                  {{ t('inline.viewsSystemSystemView.text017') }}
                </el-button>
              </template>
            </SectionHeader>

            <ToolbarShell class="workspace-toolbar" density="compact">
              <label class="toolbar-control">
                <span class="field-label">{{ t('inline.viewsSystemSystemView.text014') }}</span>
                <el-select
                  v-model="datasourceFilter.engineType"
                  clearable
                  :placeholder="t('inline.viewsSystemSystemView.text014')"
                  data-testid="system-datasource-engine-filter"
                >
                  <el-option
                    v-for="item in withCurrentOption(engineOptions, datasourceFilter.engineType)"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </label>
              <label class="toolbar-control">
                <span class="field-label">{{ t('inline.viewsSystemSystemView.text015') }}</span>
                <el-select
                  v-model="datasourceFilter.connectionMode"
                  clearable
                  :placeholder="t('inline.viewsSystemSystemView.text015')"
                  data-testid="system-datasource-mode-filter"
                >
                  <el-option
                    v-for="item in withCurrentOption(connectionModeOptions, datasourceFilter.connectionMode)"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </label>
            </ToolbarShell>

            <el-empty
              v-if="filteredDatasources.length === 0"
              class="workspace-empty"
              :description="t('inline.viewsSystemSystemView.text135')"
            />
            <el-table v-else :data="filteredDatasources" border stripe>
              <el-table-column :label="t('inline.viewsSystemSystemView.text018')" min-width="110">
                <template #default="{ row }">
                  <el-tag effect="dark">{{ displayValue(row.engineType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="datasourceCode" :label="t('inline.viewsSystemSystemView.text019')" min-width="160" />
              <el-table-column prop="datasourceName" :label="t('inline.viewsSystemSystemView.text020')" min-width="180" />
              <el-table-column :label="t('inline.viewsSystemSystemView.text021')" min-width="140">
                <template #default="{ row }">
                  <el-tag>{{ displayValue(row.connectionMode) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="credentialMask" :label="t('inline.viewsSystemSystemView.text022')" min-width="150">
                <template #default="{ row }">
                  <span class="masked-value">{{ displayValue(row.credentialMask) }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text023')" min-width="140">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.healthStatus)" effect="dark">
                    {{ displayValue(row.healthStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text137')" min-width="120">
                <template #default="{ row }">
                  <el-tag :type="enabledTagType(row.enabled)">{{ enabledLabel(row.enabled) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text136')" min-width="120">
                <template #default="{ row }">
                  <el-tag :type="row.readonly === false ? 'warning' : 'info'">{{ readonlyLabel(row.readonly) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text024')" min-width="170">
                <template #default="{ row }">{{ formatTimestamp(row.lastCheckedAt) }}</template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text025')" min-width="260" fixed="right">
                <template #default="{ row }">
                  <el-button text data-testid="system-datasource-card" @click="openDatasourceDetail(row.datasourceId)">
                    {{ t('inline.viewsSystemSystemView.text026') }}
                  </el-button>
                  <el-button text @click="openDatasourceEdit(row)">
                    {{ t('inline.viewsSystemSystemView.text027') }}
                  </el-button>
                  <el-button
                    text
                    :loading="loading.datasourceTest"
                    data-testid="system-datasource-test"
                    @click="runDatasourceTest(row.datasourceId)"
                  >
                    {{ t('inline.viewsSystemSystemView.text028') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane name="drivers">
          <template #label>
            <span class="domain-tab-label" :class="`domain-tab-label-${tabSummaryMap.drivers.tone}`">
              <span>{{ t('inline.viewsSystemSystemView.text106') }}</span>
              <strong>{{ tabSummaryMap.drivers.count }}</strong>
            </span>
          </template>

          <div class="workspace-section">
            <SectionHeader
              eyebrow="jdbc driver artifacts"
              :title="t('inline.viewsSystemSystemView.text107')"
              :summary="t('inline.viewsSystemSystemView.text108')"
              size="compact"
            >
              <template #actions>
                <el-button type="primary" @click="openDriverUpload">
                  {{ t('inline.viewsSystemSystemView.text100') }}
                </el-button>
              </template>
            </SectionHeader>

            <el-empty
              v-if="datasourceDrivers.length === 0"
              class="workspace-empty"
              :description="t('inline.viewsSystemSystemView.text138')"
            />
            <el-table v-else :data="datasourceDrivers" border stripe>
              <el-table-column :label="t('inline.viewsSystemSystemView.text091')" min-width="110">
                <template #default="{ row }">
                  <el-tag effect="dark">{{ displayValue(row.engineType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="versionLabel" :label="t('inline.viewsSystemSystemView.text092')" min-width="140" />
              <el-table-column prop="driverClassName" :label="t('inline.viewsSystemSystemView.text093')" min-width="220" />
              <el-table-column prop="originalFileName" :label="t('inline.viewsSystemSystemView.text094')" min-width="220" />
              <el-table-column
                prop="sha256"
                :label="t('inline.viewsSystemSystemView.text095')"
                min-width="220"
                show-overflow-tooltip
              />
              <el-table-column :label="t('inline.viewsSystemSystemView.text096')" min-width="120">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.status)" effect="dark">{{ displayValue(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text097')" min-width="130" fixed="right">
                <template #default="{ row }">
                  <el-button text :loading="driverDetailLoading" @click="inspectDriverArtifact(row.artifactId)">
                    {{ t('inline.viewsSystemSystemView.text109') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane name="report">
          <template #label>
            <span class="domain-tab-label" :class="`domain-tab-label-${tabSummaryMap.report.tone}`">
              <span>{{ t('inline.viewsSystemSystemView.text029') }}</span>
              <strong>{{ tabSummaryMap.report.count }}</strong>
            </span>
          </template>

          <div class="workspace-section">
            <SectionHeader
              eyebrow="report interface actions"
              :title="t('inline.viewsSystemSystemView.text030')"
              :summary="t('inline.viewsSystemSystemView.text140')"
              size="compact"
            >
              <template #actions>
                <el-button type="primary" @click="openReportCreate">
                  {{ t('inline.viewsSystemSystemView.text031') }}
                </el-button>
              </template>
            </SectionHeader>

            <el-empty
              v-if="reportInterfaces.length === 0"
              class="workspace-empty"
              :description="t('inline.viewsSystemSystemView.text141')"
            />
            <el-table v-else :data="reportInterfaces" border stripe data-testid="system-report-interface-card">
              <el-table-column prop="endpointCode" :label="t('inline.viewsSystemSystemView.text032')" min-width="170" />
              <el-table-column :label="t('inline.viewsSystemSystemView.text033')" min-width="150">
                <template #default="{ row }">
                  <el-tag :type="statusTagType(row.resolverStatus)" effect="dark">
                    {{ displayValue(row.resolverStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text137')" min-width="120">
                <template #default="{ row }">
                  <el-tag :type="enabledTagType(row.enabled)">{{ enabledLabel(row.enabled) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsSystemSystemView.text034')" min-width="220">
                <template #default="{ row }">{{ maskValue(row.baseUrl) }}</template>
              </el-table-column>
              <el-table-column prop="pathTemplate" :label="t('inline.viewsSystemSystemView.text035')" min-width="220" />
              <el-table-column :label="t('inline.viewsSystemSystemView.text036')" min-width="180" fixed="right">
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
          </div>
        </el-tab-pane>

        <el-tab-pane name="redis">
          <template #label>
            <span class="domain-tab-label" :class="`domain-tab-label-${tabSummaryMap.redis.tone}`">
              <span>{{ t('inline.viewsSystemSystemView.text039') }}</span>
              <strong>{{ tabSummaryMap.redis.count }}</strong>
            </span>
          </template>

          <div class="workspace-section">
            <SectionHeader
              eyebrow="redis rule-source actions"
              :title="t('inline.viewsSystemSystemView.text040')"
              :summary="t('inline.viewsSystemSystemView.text142')"
              size="compact"
            >
              <template #actions>
                <el-button type="primary" @click="openRedisCreate">
                  {{ t('inline.viewsSystemSystemView.text041') }}
                </el-button>
              </template>
            </SectionHeader>

            <div data-testid="system-redis-rule-sources">
              <el-empty
                v-if="redisRuleSources.length === 0"
                class="workspace-empty"
                :description="t('inline.viewsSystemSystemView.text143')"
              />
              <el-table v-else :data="redisRuleSources" border stripe>
                <el-table-column prop="sourceId" :label="t('inline.viewsSystemSystemView.text042')" min-width="170" />
                <el-table-column :label="t('inline.viewsSystemSystemView.text043')" min-width="150">
                  <template #default="{ row }">
                    <el-tag :type="statusTagType(row.activationMode)" effect="dark">
                      {{ displayValue(row.activationMode) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text137')" min-width="120">
                  <template #default="{ row }">
                    <el-tag :type="enabledTagType(row.enabled)">{{ enabledLabel(row.enabled) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text044')" min-width="180">
                  <template #default="{ row }">{{ displayValue(row.redisNamespace) || 'CONFIG_ONLY' }}</template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text045')" min-width="220">
                  <template #default="{ row }">{{ maskValue(row.redisEndpoints) }}</template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text046')" min-width="190" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      text
                      data-testid="system-redis-rule-source-card"
                      @click="openPayloadDrawer(row.sourceId || 'redis rule source', row)"
                    >
                      {{ t('inline.viewsSystemSystemView.text047') }}
                    </el-button>
                    <el-button text @click="openRedisEdit(row)">
                      {{ t('inline.viewsSystemSystemView.text048') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="dispatch">
          <template #label>
            <span class="domain-tab-label" :class="`domain-tab-label-${tabSummaryMap.dispatch.tone}`">
              <span>{{ t('inline.viewsSystemSystemView.text049') }}</span>
              <strong>{{ tabSummaryMap.dispatch.count }}</strong>
            </span>
          </template>

          <div class="workspace-section">
            <SectionHeader
              eyebrow="dispatch policy actions"
              :title="t('inline.viewsSystemSystemView.text050')"
              :summary="t('inline.viewsSystemSystemView.text144')"
              size="compact"
            >
              <template #actions>
                <el-button type="primary" @click="openDispatchCreate">
                  {{ t('inline.viewsSystemSystemView.text051') }}
                </el-button>
              </template>
            </SectionHeader>

            <div data-testid="system-dispatch-policies">
              <el-empty
                v-if="dispatchPolicies.length === 0"
                class="workspace-empty"
                :description="t('inline.viewsSystemSystemView.text145')"
              />
              <el-table v-else :data="dispatchPolicies" border stripe>
                <el-table-column prop="policyId" :label="t('inline.viewsSystemSystemView.text052')" min-width="170" />
                <el-table-column :label="t('inline.viewsSystemSystemView.text053')" min-width="140">
                  <template #default="{ row }">
                    <el-tag effect="dark">{{ displayValue(row.targetEngine) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="targetDatasource" :label="t('inline.viewsSystemSystemView.text054')" min-width="160" />
                <el-table-column :label="t('inline.viewsSystemSystemView.text055')" min-width="130">
                  <template #default="{ row }">
                    <el-tag>{{ displayValue(row.ackMode) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text056')" min-width="220">
                  <template #default="{ row }">
                    <el-tag type="warning">{{ boundaryLabel(row.executionBoundary) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text137')" min-width="120">
                  <template #default="{ row }">
                    <el-tag :type="enabledTagType(row.enabled)">{{ enabledLabel(row.enabled) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column :label="t('inline.viewsSystemSystemView.text057')" min-width="190" fixed="right">
                  <template #default="{ row }">
                    <el-button
                      text
                      data-testid="system-dispatch-policy-card"
                      @click="openPayloadDrawer(row.policyId || 'dispatch policy', row)"
                    >
                      {{ t('inline.viewsSystemSystemView.text058') }}
                    </el-button>
                    <el-button text @click="openDispatchEditPlaceholder(row)">
                      {{ t('inline.viewsSystemSystemView.text059') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="tenant">
          <template #label>
            <span class="domain-tab-label" :class="`domain-tab-label-${tabSummaryMap.tenant.tone}`">
              <span>{{ t('inline.viewsSystemSystemView.text060') }}</span>
              <strong>{{ tabSummaryMap.tenant.count }}</strong>
            </span>
          </template>

          <div class="tenant-stage">
            <section class="tenant-section">
              <SectionHeader eyebrow="tenant" :title="t('inline.viewsSystemSystemView.text146')" size="compact" />
              <div class="detail-grid" data-testid="system-tenant-params">
                <div v-for="item in tenantParamCards" :key="item.key" class="detail-grid__item">
                  <span>{{ item.label }}</span>
                  <strong>{{ displayValue(item.value) }}</strong>
                </div>
              </div>
            </section>

            <section class="tenant-section">
              <SectionHeader eyebrow="permission" :title="t('inline.viewsSystemSystemView.text147')" size="compact" />
              <div class="detail-grid" data-testid="system-permission-audit">
                <div v-for="item in permissionAuditCards" :key="item.key" class="detail-grid__item">
                  <span>{{ item.label }}</span>
                  <strong>{{ displayValue(item.value) }}</strong>
                </div>
              </div>
            </section>

            <div v-if="retryResult" class="inline-banner">
              {{ formatJson(retryResult) }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </EvidencePanel>

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

    <el-dialog
      v-model="datasourceDialogVisible"
      :title="
        datasourceDialogMode === 'create'
          ? t('inline.viewsSystemSystemView.text066')
          : t('inline.viewsSystemSystemView.text067')
      "
      width="860px"
    >
      <el-form
        ref="datasourceFormRef"
        :model="datasourceForm"
        :rules="datasourceRules"
        label-position="top"
        status-icon
        class="managed-form"
      >
        <el-form-item :label="t('inline.viewsSystemSystemView.text004')" prop="tenantId">
          <el-select v-model="datasourceForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, datasourceForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text068')" prop="datasourceCode">
          <el-input v-model="datasourceForm.datasourceCode" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text069')" prop="datasourceName">
          <el-input v-model="datasourceForm.datasourceName" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text070')" prop="engineType">
          <el-select v-model="datasourceForm.engineType" data-testid="system-datasource-engine-type">
            <el-option
              v-for="item in withCurrentOption(engineOptions, datasourceForm.engineType)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text071')" prop="connectionMode">
          <el-select v-model="datasourceForm.connectionMode">
            <el-option
              v-for="item in withCurrentOption(connectionModeOptions, datasourceForm.connectionMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text116')" prop="stage">
          <el-select v-model="datasourceForm.stage">
            <el-option
              v-for="item in withCurrentOption(stageOptions, datasourceForm.stage)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text117')" prop="timeoutMs">
          <el-input-number v-model="datasourceForm.timeoutMs" :min="100" :step="100" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text150')" prop="driverSourceType">
          <el-select v-model="datasourceForm.driverSourceType">
            <el-option :label="t('inline.viewsSystemSystemView.text098')" value="CLASSPATH" />
            <el-option :label="t('inline.viewsSystemSystemView.text099')" value="UPLOADED" />
          </el-select>
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text171')" prop="jdbcUrl">
          <el-input v-model="datasourceForm.jdbcUrl" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text172')" prop="jdbcDriverClassName">
          <el-input v-model="datasourceForm.jdbcDriverClassName" />
        </el-form-item>
        <el-form-item
          v-if="datasourceForm.driverSourceType === 'UPLOADED'"
          class="field-span-full"
          :label="t('inline.viewsSystemSystemView.text151')"
          prop="driverArtifactId"
        >
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
              {{ t('inline.viewsSystemSystemView.text109') }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item
          v-if="datasourceForm.driverSourceType === 'UPLOADED'"
          class="field-span-full"
          :label="t('inline.viewsSystemSystemView.text152')"
        >
          <div class="detail-grid detail-grid-compact">
            <div class="detail-grid__item">
              <span>{{ t('inline.viewsSystemSystemView.text153') }}</span>
              <strong>{{ displayValue(datasourceForm.driverVersionLabel) }}</strong>
            </div>
            <div class="detail-grid__item">
              <span>{{ t('inline.viewsSystemSystemView.text154') }}</span>
              <strong>{{ displayValue(datasourceForm.driverLoadStatus) }}</strong>
            </div>
            <div class="detail-grid__item field-span-full">
              <span>{{ t('inline.viewsSystemSystemView.text155') }}</span>
              <strong class="monospace-text">{{ displayValue(datasourceForm.driverSha256) }}</strong>
            </div>
          </div>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text156')" prop="username">
          <el-input v-model="datasourceForm.username" autocomplete="off" />
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text157')" prop="apiBaseUrl">
          <el-input v-model="datasourceForm.apiBaseUrl" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text158')" prop="credentialRef">
          <el-input v-model="datasourceForm.credentialRef" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text159')" prop="credentialSecret">
          <el-input v-model="datasourceForm.credentialSecret" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text126')" prop="authMode">
          <el-select v-model="datasourceForm.authMode">
            <el-option
              v-for="item in withCurrentOption(authModeOptions, datasourceForm.authMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text160')" prop="credentialMode">
          <el-select v-model="datasourceForm.credentialMode">
            <el-option
              v-for="item in withCurrentOption(credentialModeOptions, datasourceForm.credentialMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text161')" prop="tlsEnabled">
          <el-switch v-model="datasourceForm.tlsEnabled" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text162')" prop="verifyPeer">
          <el-switch v-model="datasourceForm.verifyPeer" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text163')" prop="readonly">
          <el-switch v-model="datasourceForm.readonly" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text164')" prop="enabled">
          <el-switch v-model="datasourceForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="datasourceDialogVisible = false">{{ t('inline.viewsSystemSystemView.text072') }}</el-button>
        <el-button type="primary" :loading="loading.datasourceSubmit" @click="submitDatasource">
          {{
            datasourceDialogMode === 'create'
              ? t('inline.viewsSystemSystemView.text073')
              : t('inline.viewsSystemSystemView.text074')
          }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="driverDialogVisible" :title="t('inline.viewsSystemSystemView.text100')" width="760px">
      <el-form
        ref="driverUploadFormRef"
        :model="driverUploadForm"
        :rules="driverUploadRules"
        label-position="top"
        status-icon
        class="managed-form"
      >
        <el-form-item :label="t('inline.viewsSystemSystemView.text004')" prop="tenantId">
          <el-select v-model="driverUploadForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, driverUploadForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text091')" prop="engineType">
          <el-select v-model="driverUploadForm.engineType">
            <el-option :label="t('inline.viewsSystemSystemView.text101')" value="TRINO" />
            <el-option :label="t('inline.viewsSystemSystemView.text102')" value="HETU" />
            <el-option :label="t('inline.viewsSystemSystemView.text103')" value="HIVE" />
          </el-select>
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text165')" prop="driverClassName">
          <el-input v-model="driverUploadForm.driverClassName" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text153')" prop="versionLabel">
          <el-input v-model="driverUploadForm.versionLabel" />
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text110')" prop="file">
          <div class="file-upload-row">
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
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="driverDialogVisible = false">{{ t('inline.viewsSystemSystemView.text072') }}</el-button>
        <el-button type="primary" :loading="loading.driverUpload" @click="submitDriverUpload">
          {{ t('inline.viewsSystemSystemView.text113') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reportDialogVisible"
      :title="
        reportDialogMode === 'create'
          ? t('inline.viewsSystemSystemView.text075')
          : t('inline.viewsSystemSystemView.text076')
      "
      width="860px"
    >
      <el-form
        ref="reportFormRef"
        :model="reportForm"
        :rules="reportRules"
        label-position="top"
        status-icon
        class="managed-form"
      >
        <el-form-item :label="t('inline.viewsSystemSystemView.text004')" prop="tenantId">
          <el-select v-model="reportForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, reportForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text118')" prop="datasourceCode">
          <el-select
            v-model="reportForm.datasourceCode"
            filterable
            allow-create
            default-first-option
            data-testid="system-report-datasource-select"
          >
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, reportForm.datasourceCode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text032')" prop="endpointCode">
          <el-input v-model="reportForm.endpointCode" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text119')" prop="endpointName">
          <el-input v-model="reportForm.endpointName" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text116')" prop="stage">
          <el-select v-model="reportForm.stage">
            <el-option
              v-for="item in withCurrentOption(stageOptions, reportForm.stage)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text120')" prop="sourceType">
          <el-select v-model="reportForm.sourceType">
            <el-option
              v-for="item in withCurrentOption(sourceTypeOptions, reportForm.sourceType)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text121')" prop="httpMethod">
          <el-select v-model="reportForm.httpMethod">
            <el-option
              v-for="item in withCurrentOption(httpMethodOptions, reportForm.httpMethod)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text034')" prop="baseUrl">
          <el-input v-model="reportForm.baseUrl" />
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text035')" prop="pathTemplate">
          <el-input v-model="reportForm.pathTemplate" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text122')" prop="reportCodeParamName">
          <el-input v-model="reportForm.reportCodeParamName" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text123')" prop="sqlJsonPath">
          <el-input v-model="reportForm.sqlJsonPath" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text117')" prop="timeoutMs">
          <el-input-number v-model="reportForm.timeoutMs" :min="100" :step="100" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text126')" prop="authMode">
          <el-select v-model="reportForm.authMode">
            <el-option
              v-for="item in withCurrentOption(authModeOptions, reportForm.authMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text164')" prop="enabled">
          <el-switch v-model="reportForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">{{ t('inline.viewsSystemSystemView.text077') }}</el-button>
        <el-button type="primary" :loading="loading.reportSubmit" @click="submitReportInterface">
          {{
            reportDialogMode === 'create'
              ? t('inline.viewsSystemSystemView.text078')
              : t('inline.viewsSystemSystemView.text079')
          }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="redisDialogVisible"
      :title="
        redisDialogMode === 'create'
          ? t('inline.viewsSystemSystemView.text080')
          : t('inline.viewsSystemSystemView.text081')
      "
      width="860px"
    >
      <el-form
        ref="redisFormRef"
        :model="redisForm"
        :rules="redisRules"
        label-position="top"
        status-icon
        class="managed-form"
      >
        <el-form-item :label="t('inline.viewsSystemSystemView.text004')" prop="tenantId">
          <el-select v-model="redisForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, redisForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text124')" prop="sourceName">
          <el-input v-model="redisForm.sourceName" />
        </el-form-item>
        <el-form-item class="field-span-full" :label="t('inline.viewsSystemSystemView.text166')" prop="redisEndpoints">
          <el-input v-model="redisForm.redisEndpoints" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text167')" prop="redisNamespace">
          <el-input v-model="redisForm.redisNamespace" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text125')" prop="keyPattern">
          <el-input v-model="redisForm.keyPattern" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text126')" prop="authMode">
          <el-select v-model="redisForm.authMode">
            <el-option
              v-for="item in withCurrentOption(authModeOptions, redisForm.authMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text158')" prop="credentialRef">
          <el-input v-model="redisForm.credentialRef" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text169')" prop="bypassOnUnavailable">
          <el-switch v-model="redisForm.bypassOnUnavailable" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text164')" prop="enabled">
          <el-switch v-model="redisForm.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="redisDialogVisible = false">{{ t('inline.viewsSystemSystemView.text082') }}</el-button>
        <el-button type="primary" :loading="loading.redisSubmit" @click="submitRedisRuleSource">
          {{
            redisDialogMode === 'create'
              ? t('inline.viewsSystemSystemView.text083')
              : t('inline.viewsSystemSystemView.text084')
          }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dispatchDialogVisible" :title="t('inline.viewsSystemSystemView.text085')" width="760px">
      <el-form
        ref="dispatchFormRef"
        :model="dispatchForm"
        :rules="dispatchRules"
        label-position="top"
        status-icon
        class="managed-form"
      >
        <el-form-item :label="t('inline.viewsSystemSystemView.text004')" prop="tenantId">
          <el-select v-model="dispatchForm.tenantId" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(tenantOptions, dispatchForm.tenantId)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text127')" prop="policyName">
          <el-input v-model="dispatchForm.policyName" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text128')" prop="dispatchType">
          <el-select v-model="dispatchForm.dispatchType">
            <el-option
              v-for="item in withCurrentOption(dispatchTypeOptions, dispatchForm.dispatchType)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text053')" prop="targetEngine">
          <el-select v-model="dispatchForm.targetEngine">
            <el-option
              v-for="item in withCurrentOption(engineOptions, dispatchForm.targetEngine)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text054')" prop="targetDatasource">
          <el-select v-model="dispatchForm.targetDatasource" filterable allow-create default-first-option>
            <el-option
              v-for="item in withCurrentOption(datasourceOptions, dispatchForm.targetDatasource)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text055')" prop="ackMode">
          <el-select v-model="dispatchForm.ackMode">
            <el-option
              v-for="item in withCurrentOption(ackModeOptions, dispatchForm.ackMode)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text129')" prop="pullWindowSeconds">
          <el-input-number v-model="dispatchForm.pullWindowSeconds" :min="1" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text130')" prop="maxBatchSize">
          <el-input-number v-model="dispatchForm.maxBatchSize" :min="1" :step="10" controls-position="right" />
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text131')" prop="retryStrategy">
          <el-select v-model="dispatchForm.retryStrategy">
            <el-option
              v-for="item in withCurrentOption(retryStrategyOptions, dispatchForm.retryStrategy)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inline.viewsSystemSystemView.text164')" prop="enabled">
          <el-switch v-model="dispatchForm.enabled" />
        </el-form-item>
      </el-form>
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
  gap: var(--sqlforge-space-5);
}

.system-context-shell :deep(.toolbar-shell-body) {
  align-items: flex-end;
}

.context-field,
.toolbar-control {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 220px;
}

.context-field :deep(.el-select),
.toolbar-control :deep(.el-select) {
  width: 100%;
}

.field-label {
  color: var(--sqlforge-text-muted);
  font-size: var(--sqlforge-text-meta);
}

.boundary-strip {
  display: flex;
  flex: 1 1 420px;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  min-width: 0;
}

.boundary-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--sqlforge-space-2);
  min-height: 34px;
  max-width: 100%;
  padding: 6px 10px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-meta);
}

.boundary-chip strong {
  color: var(--sqlforge-text-primary);
  font-family: 'SFMono-Regular', 'Consolas', monospace;
  font-weight: 600;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: var(--sqlforge-space-4);
}

.workspace-panel :deep(.evidence-panel-body) {
  min-width: 0;
}

.domain-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--sqlforge-space-5);
}

.domain-tabs :deep(.el-tabs__nav-wrap::after) {
  background: var(--sqlforge-border-subtle);
}

.domain-tab-label {
  display: inline-flex;
  align-items: center;
  gap: var(--sqlforge-space-2);
  min-width: 0;
}

.domain-tab-label strong {
  min-width: 24px;
  padding: 1px 8px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-pill);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: var(--sqlforge-text-meta);
  font-weight: 600;
  text-align: center;
}

.domain-tab-label-success strong {
  border-color: var(--sqlforge-color-brand-border);
}

.domain-tab-label-warning strong {
  border-color: rgba(207, 166, 62, 0.42);
}

.workspace-section,
.tenant-stage,
.tenant-section {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
}

.workspace-toolbar {
  background: var(--sqlforge-surface-2);
}

.workspace-empty {
  border: 1px dashed var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-1);
}

.domain-tabs :deep(.el-table) {
  min-width: 960px;
}

.domain-tabs :deep(.el-table__body-wrapper),
.domain-tabs :deep(.el-table__inner-wrapper) {
  background: var(--sqlforge-surface-1);
}

.masked-value,
.upload-file-name {
  color: var(--sqlforge-text-secondary);
  word-break: break-all;
}

.inline-banner {
  padding: 12px 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
  white-space: pre-wrap;
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sqlforge-space-3);
}

.detail-grid-compact {
  width: 100%;
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-md);
  background: var(--sqlforge-surface-2);
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
  font-size: var(--sqlforge-text-meta);
}

.detail-grid__item strong {
  min-width: 0;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
  word-break: break-word;
}

.managed-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sqlforge-space-4);
}

.managed-form :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 0;
}

.managed-form :deep(.el-form-item__label) {
  color: var(--sqlforge-text-muted);
  line-height: 1.3;
}

.managed-form :deep(.el-select),
.managed-form :deep(.el-input-number),
.managed-form :deep(.el-input),
.managed-form :deep(.el-textarea) {
  width: 100%;
}

.field-inline,
.file-upload-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-3);
  align-items: center;
  width: 100%;
  min-width: 0;
}

.field-inline :deep(.el-select) {
  flex: 1 1 280px;
}

.field-span-full {
  grid-column: 1 / -1;
}

.monospace-text {
  font-family: 'SFMono-Regular', 'Consolas', monospace;
  word-break: break-all;
}

.file-input {
  flex: 1 1 260px;
  min-width: 0;
  color: var(--sqlforge-text-primary);
}

.code-block {
  margin: 0;
  color: var(--sqlforge-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1320px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .metric-grid,
  .detail-grid,
  .managed-form {
    grid-template-columns: 1fr;
  }

  .field-span-full {
    grid-column: auto;
  }
}
</style>
