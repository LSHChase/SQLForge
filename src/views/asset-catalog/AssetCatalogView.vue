<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getDatabaseViewDetail,
  getDatabaseViews,
  getGovernanceDatasourceDetail,
  getGovernanceDatasources,
  getLogicalViewDetail,
  getLogicalViews,
  getMetadataSchemaDetail,
  getMetadataSchemas,
  getMetadataSnapshots,
  getMetadataTableDetail,
  getMetadataTables
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const filterForm = reactive({
  tenantId: 'tenant-a',
  datasourceCode: '',
  schemaName: ''
})

const loading = reactive({
  lists: false,
  detail: false
})

const activeTab = ref('datasources')
const selectedItemKey = ref('')
const selectedDetail = ref(null)
const selectedSnapshots = ref([])
const errorMessage = ref('')

const datasources = ref([])
const schemas = ref([])
const tables = ref([])
const logicalViews = ref([])
const databaseViews = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')
const tabOptions = computed(() => [
  {
    value: 'datasources',
    label: isChinese.value ? '数据源' : 'Datasources'
  },
  {
    value: 'schemas',
    label: isChinese.value ? 'Schema' : 'Schemas'
  },
  {
    value: 'tables',
    label: isChinese.value ? '表' : 'Tables'
  },
  {
    value: 'logicalViews',
    label: isChinese.value ? '逻辑视图' : 'Logical views'
  },
  {
    value: 'databaseViews',
    label: isChinese.value ? '数据库视图' : 'Database views'
  }
])

const listItems = computed(() => {
  if (activeTab.value === 'datasources') {
    return datasources.value
  }
  if (activeTab.value === 'schemas') {
    return schemas.value
  }
  if (activeTab.value === 'tables') {
    return tables.value
  }
  if (activeTab.value === 'logicalViews') {
    return logicalViews.value
  }
  return databaseViews.value
})

const datasourceOptions = computed(() =>
  datasources.value.map(item => ({
    label: item.datasourceCode || item.datasourceName || item.datasourceId,
    value: item.datasourceCode
  }))
)

const detailHighlights = computed(() => {
  const detail = selectedDetail.value
  if (!detail) {
    return []
  }
  if (activeTab.value === 'datasources') {
    return [
      field('datasourceCode', isChinese.value ? '数据源编码' : 'Datasource code', detail.datasourceCode),
      field('datasourceName', isChinese.value ? '名称' : 'Name', detail.datasourceName),
      field('connectionMode', isChinese.value ? '连接模式' : 'Connection mode', detail.connectionMode),
      field('stage', 'Stage', detail.stage),
      field('healthStatus', isChinese.value ? '健康状态' : 'Health status', detail.healthStatus),
      field('readonly', isChinese.value ? '只读' : 'Readonly', boolText(detail.readonly)),
      field('enabled', isChinese.value ? '启用' : 'Enabled', boolText(detail.enabled)),
      field('timeoutMs', 'Timeout', detail.timeoutMs),
      field('contractStage', 'Contract', detail.contractStage),
      field('implementationStage', 'Implementation', detail.implementationStage)
    ]
  }
  if (activeTab.value === 'schemas') {
    return [
      field('schemaName', 'Schema', detail.schemaName),
      field('catalogName', 'Catalog', detail.catalogName),
      field('tableCount', isChinese.value ? '表数' : 'Tables', detail.tableCount),
      field('dbViewCount', isChinese.value ? 'DB View 数' : 'DB views', detail.dbViewCount),
      field('logicalViewCount', isChinese.value ? '逻辑视图数' : 'Logical views', detail.logicalViewCount),
      field('freshnessStatus', isChinese.value ? '新鲜度' : 'Freshness', detail.freshnessStatus),
      field('slaStatus', 'SLA', detail.slaStatus),
      field('queryabilityStatus', isChinese.value ? '可查询性' : 'Queryability', detail.queryabilityStatus),
      field('evidenceStatus', isChinese.value ? '证据状态' : 'Evidence', detail.evidenceStatus)
    ]
  }
  if (activeTab.value === 'tables') {
    return [
      field('tableName', isChinese.value ? '表名' : 'Table', detail.tableName),
      field('schemaName', 'Schema', detail.schemaName),
      field('objectKey', 'Object key', detail.objectKey),
      field('columnCount', isChinese.value ? '列数' : 'Columns', detail.columnCount),
      field('partitionCount', isChinese.value ? '分区数' : 'Partitions', detail.partitionCount),
      field('rowCount', isChinese.value ? '行数' : 'Rows', detail.rowCount),
      field('storageBytes', isChinese.value ? '存储字节' : 'Storage bytes', detail.storageBytes),
      field('upstreamCount', isChinese.value ? '上游数' : 'Upstream', detail.upstreamCount),
      field('downstreamCount', isChinese.value ? '下游数' : 'Downstream', detail.downstreamCount),
      field('freshnessStatus', isChinese.value ? '新鲜度' : 'Freshness', detail.freshnessStatus)
    ]
  }
  if (activeTab.value === 'logicalViews') {
    return [
      field('viewCode', isChinese.value ? '视图编码' : 'View code', detail.viewCode),
      field('viewName', isChinese.value ? '视图名称' : 'View name', detail.viewName),
      field('objectKey', 'Object key', detail.objectKey),
      field('subjectArea', isChinese.value ? '主题域' : 'Subject area', detail.subjectArea),
      field('ownerUser', isChinese.value ? 'Owner' : 'Owner', detail.ownerUser),
      field('queryable', isChinese.value ? '可查询' : 'Queryable', boolText(detail.queryable)),
      field('freshnessStatus', isChinese.value ? '新鲜度' : 'Freshness', detail.freshnessStatus),
      field('slaStatus', 'SLA', detail.slaStatus),
      field('upstreamCount', isChinese.value ? '上游数' : 'Upstream', detail.upstreamCount),
      field('downstreamCount', isChinese.value ? '下游数' : 'Downstream', detail.downstreamCount)
    ]
  }
  return [
    field('viewName', isChinese.value ? '视图名' : 'View name', detail.viewName),
    field('schemaName', 'Schema', detail.schemaName),
    field('catalogName', 'Catalog', detail.catalogName),
    field('objectKey', 'Object key', detail.objectKey),
    field('ownerUser', isChinese.value ? 'Owner' : 'Owner', detail.ownerUser),
    field('queryable', isChinese.value ? '可查询' : 'Queryable', boolText(detail.queryable)),
    field('freshnessStatus', isChinese.value ? '新鲜度' : 'Freshness', detail.freshnessStatus),
    field('slaStatus', 'SLA', detail.slaStatus),
    field('upstreamCount', isChinese.value ? '上游数' : 'Upstream', detail.upstreamCount),
    field('downstreamCount', isChinese.value ? '下游数' : 'Downstream', detail.downstreamCount)
  ]
})

const snapshotSummary = computed(() => {
  const items = selectedSnapshots.value
  return [
    {
      label: isChinese.value ? 'Snapshot 数' : 'Snapshots',
      value: items.length
    },
    {
      label: isChinese.value ? 'Freshness' : 'Freshness',
      value: summarizeDistinct(items.map(item => item.freshnessStatus))
    },
    {
      label: 'SLA',
      value: summarizeDistinct(items.map(item => item.slaStatus))
    },
    {
      label: isChinese.value ? 'Queryability' : 'Queryability',
      value: summarizeDistinct(items.map(item => item.queryabilityStatus))
    },
    {
      label: isChinese.value ? 'Evidence' : 'Evidence',
      value: summarizeDistinct(items.map(item => item.evidenceStatus))
    }
  ]
})

const detailAuxGroups = computed(() => {
  const detail = selectedDetail.value
  if (!detail) {
    return []
  }
  if (activeTab.value === 'tables') {
    return [
      {
        key: 'upstreamRefs',
        title: isChinese.value ? '上游血缘' : 'Upstream lineage',
        items: detail.upstreamRefs || []
      },
      {
        key: 'downstreamRefs',
        title: isChinese.value ? '下游血缘' : 'Downstream lineage',
        items: detail.downstreamRefs || []
      }
    ]
  }
  if (activeTab.value === 'logicalViews') {
    return [
      {
        key: 'physicalTargets',
        title: isChinese.value ? '物理映射' : 'Physical targets',
        items: detail.physicalTargets || []
      }
    ]
  }
  if (activeTab.value === 'databaseViews') {
    return [
      {
        key: 'dependencies',
        title: isChinese.value ? '依赖对象' : 'Dependencies',
        items: detail.dependencies || []
      }
    ]
  }
  return []
})

function field(key, label, value) {
  return { key, label, value }
}

function hasDisplayValue(value) {
  return !(value === null || value === undefined || String(value).trim() === '')
}

function boolText(value) {
  if (typeof value !== 'boolean') {
    return ''
  }
  return value ? 'true' : 'false'
}

function summarizeDistinct(values) {
  const unique = [...new Set(values.filter(hasDisplayValue))]
  return unique.length > 0 ? unique.join(' / ') : '-'
}

function datasourceEndpoint(detail) {
  if (!detail) {
    return '-'
  }
  return (
    detail.jdbcUrl ||
    detail.apiBaseUrl ||
    detail.clientEndpoint ||
    detail.gatewayEndpoint ||
    detail.proxyEndpoint ||
    '-'
  )
}

function auxPrimaryValue(item) {
  if (!item) {
    return '-'
  }
  return (
    item.targetObjectKey ||
    item.dependencyObjectKey ||
    item.objectKey ||
    item.targetObjectName ||
    item.dependencyObjectName ||
    item.objectName ||
    '-'
  )
}

function auxSecondaryValue(item) {
  if (!item) {
    return '-'
  }
  return item.targetObjectType || item.dependencyObjectType || item.objectType || '-'
}

function auxTertiaryValue(item) {
  if (!item) {
    return '-'
  }
  return item.mappingRole || item.relationshipType || '-'
}

function displayLabel(item) {
  if (!item) {
    return '-'
  }
  if (activeTab.value === 'datasources') {
    return item.datasourceCode || item.datasourceName || item.datasourceId
  }
  if (activeTab.value === 'schemas') {
    return item.schemaName
  }
  if (activeTab.value === 'tables') {
    return `${item.schemaName || '-'} . ${item.tableName || '-'}`
  }
  if (activeTab.value === 'logicalViews') {
    return item.viewCode || item.viewName
  }
  return item.viewName
}

function displayMeta(item) {
  if (!item) {
    return '-'
  }
  if (activeTab.value === 'datasources') {
    return `${item.connectionMode || '-'} · ${item.healthStatus || '-'}`
  }
  if (activeTab.value === 'schemas') {
    return `${item.datasourceCode || '-'} · ${item.tableCount || 0} tables`
  }
  if (activeTab.value === 'tables') {
    return `${item.datasourceCode || '-'} · ${item.rowCount || 0} rows`
  }
  if (activeTab.value === 'logicalViews') {
    return `${item.datasourceCode || '-'} · ${item.subjectArea || '-'}`
  }
  return `${item.datasourceCode || '-'} · ${item.schemaName || '-'}`
}

function itemKey(item) {
  if (!item) {
    return ''
  }
  if (activeTab.value === 'datasources') {
    return item.datasourceId
  }
  if (activeTab.value === 'schemas') {
    return `${item.datasourceCode}:${item.schemaName}`
  }
  if (activeTab.value === 'tables') {
    return item.objectKey || `${item.datasourceCode}:${item.schemaName}:${item.tableName}`
  }
  if (activeTab.value === 'logicalViews') {
    return item.viewCode
  }
  return item.objectKey || `${item.datasourceCode}:${item.viewName}`
}

async function refreshLists() {
  loading.lists = true
  errorMessage.value = ''
  try {
    const tenantId = filterForm.tenantId
    const datasourceCode = filterForm.datasourceCode
    const schemaName = filterForm.schemaName
    const [nextDatasources, nextSchemas, nextTables, nextLogicalViews, nextDatabaseViews] = await Promise.all([
      getGovernanceDatasources(tenantId),
      getMetadataSchemas(tenantId, datasourceCode),
      getMetadataTables(tenantId, datasourceCode, schemaName),
      getLogicalViews(tenantId, datasourceCode),
      getDatabaseViews(tenantId, datasourceCode)
    ])
    datasources.value = Array.isArray(nextDatasources) ? nextDatasources : []
    schemas.value = Array.isArray(nextSchemas) ? nextSchemas : []
    tables.value = Array.isArray(nextTables) ? nextTables : []
    logicalViews.value = Array.isArray(nextLogicalViews) ? nextLogicalViews : []
    databaseViews.value = Array.isArray(nextDatabaseViews) ? nextDatabaseViews : []

    if (!filterForm.datasourceCode && datasources.value.length > 0) {
      filterForm.datasourceCode = datasources.value[0].datasourceCode || ''
    }

    const currentItems = listItems.value
    if (currentItems.length > 0) {
      const existing = currentItems.find(item => itemKey(item) === selectedItemKey.value)
      await loadDetail(existing || currentItems[0])
    } else {
      selectedItemKey.value = ''
      selectedDetail.value = null
      selectedSnapshots.value = []
    }
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.lists = false
  }
}

async function loadSnapshots(filters) {
  try {
    const response = await getMetadataSnapshots(filters, {
      requestPrefix: 'frontend-asset-catalog-snapshots'
    })
    selectedSnapshots.value = Array.isArray(response) ? response : []
  } catch (error) {
    selectedSnapshots.value = []
    errorMessage.value = formatRuntimeError(error)
  }
}

async function loadDetail(item) {
  if (!item) {
    selectedItemKey.value = ''
    selectedDetail.value = null
    selectedSnapshots.value = []
    return
  }
  loading.detail = true
  errorMessage.value = ''
  selectedItemKey.value = itemKey(item)
  try {
    const tenantId = filterForm.tenantId
    if (activeTab.value === 'datasources') {
      selectedDetail.value = await getGovernanceDatasourceDetail(tenantId, item.datasourceId)
      await loadSnapshots({
        tenantId,
        datasourceCode: item.datasourceCode
      })
    } else if (activeTab.value === 'schemas') {
      selectedDetail.value = await getMetadataSchemaDetail(tenantId, item.datasourceCode, item.schemaName)
      await loadSnapshots({
        tenantId,
        datasourceCode: item.datasourceCode
      })
    } else if (activeTab.value === 'tables') {
      selectedDetail.value = await getMetadataTableDetail(tenantId, item.datasourceCode, item.schemaName, item.tableName)
      await loadSnapshots({
        tenantId,
        datasourceCode: item.datasourceCode,
        objectType: 'TABLE',
        objectKey: selectedDetail.value.objectKey
      })
    } else if (activeTab.value === 'logicalViews') {
      selectedDetail.value = await getLogicalViewDetail(tenantId, item.viewCode)
      await loadSnapshots({
        tenantId,
        datasourceCode: selectedDetail.value.datasourceCode,
        objectType: 'LOGICAL_VIEW',
        objectKey: selectedDetail.value.objectKey
      })
    } else {
      selectedDetail.value = await getDatabaseViewDetail(tenantId, item.datasourceCode, item.viewName)
      await loadSnapshots({
        tenantId,
        datasourceCode: item.datasourceCode,
        objectType: 'DB_VIEW',
        objectKey: selectedDetail.value.objectKey
      })
    }
  } catch (error) {
    selectedDetail.value = null
    selectedSnapshots.value = []
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.detail = false
  }
}

watch(activeTab, async () => {
  const items = listItems.value
  await loadDetail(items[0] || null)
})

onMounted(async () => {
  await refreshLists()
})
</script>

<template>
  <section class="runtime-page asset-page" data-testid="asset-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">data asset catalog</p>
        <h1 class="runtime-title">{{ t('assetCatalog.title') }}</h1>
        <p class="runtime-summary">{{ t('assetCatalog.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页把 datasource、schema、table、logical view 和 db view 的目录与详情证据放到同一工作面，并把 metadata snapshot 作为详情旁证。'
            : 'This page keeps datasource, schema, table, logical-view, and db-view catalogs in one workspace and attaches metadata snapshots as detail evidence.'
        }}
      </p>
    </div>

    <article class="surface-card control-card">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">asset filters</p>
          <h2 class="section-title">{{ isChinese ? '筛选与刷新' : 'Filters and refresh' }}</h2>
        </div>
      </div>
      <div class="filter-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="filterForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
          <el-select
            v-model="filterForm.datasourceCode"
            clearable
            data-testid="asset-filter-datasource"
          >
            <el-option
              v-for="item in datasourceOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'Schema' : 'Schema' }}</span>
          <el-input v-model="filterForm.schemaName" :placeholder="isChinese ? '仅 table 列表使用' : 'Used for table list only'" />
        </label>
        <el-button
          type="primary"
          :loading="loading.lists"
          data-testid="asset-refresh"
          @click="refreshLists"
        >
          {{ isChinese ? '刷新目录' : 'Refresh catalog' }}
        </el-button>
      </div>
    </article>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="asset-error"
    >
      {{ errorMessage }}
    </div>

    <div class="asset-grid">
      <article class="surface-card catalog-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">catalog tabs</p>
            <h2 class="section-title">{{ isChinese ? '资产目录' : 'Asset catalog' }}</h2>
          </div>
        </div>

        <el-tabs v-model="activeTab" class="catalog-tabs">
          <el-tab-pane
            v-for="option in tabOptions"
            :key="option.value"
            :label="option.label"
            :name="option.value"
            :data-testid="`asset-tab-${option.value}`"
          />
        </el-tabs>

        <div class="catalog-list">
          <button
            v-for="item in listItems"
            :key="itemKey(item)"
            type="button"
            class="catalog-item"
            :class="{ 'catalog-item-active': selectedItemKey === itemKey(item) }"
            data-testid="asset-item"
            @click="loadDetail(item)"
          >
            <strong>{{ displayLabel(item) }}</strong>
            <span>{{ displayMeta(item) }}</span>
          </button>
        </div>

        <p v-if="!listItems.length" class="empty-state">
          {{ isChinese ? '当前筛选下没有目录结果。' : 'No catalog entries match the current filters.' }}
        </p>
      </article>

      <article class="surface-card detail-rail" data-testid="asset-detail-panel">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">asset detail</p>
            <h2 class="section-title">{{ isChinese ? '详情与证据' : 'Detail and evidence' }}</h2>
          </div>
        </div>

        <p v-if="!selectedDetail && !loading.detail" class="empty-state">
          {{ isChinese ? '从左侧选择一个资产后显示详情。' : 'Select an asset from the left to render the detail panel.' }}
        </p>

        <template v-else>
          <div class="summary-card-grid">
            <article
              v-for="item in detailHighlights"
              :key="item.key"
              class="summary-card"
            >
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ hasDisplayValue(item.value) ? item.value : '-' }}</strong>
            </article>
          </div>

          <div v-if="activeTab === 'datasources'" class="detail-copy">
            <p>
              {{ isChinese ? '连接地址' : 'Connection endpoint' }}:
              {{ datasourceEndpoint(selectedDetail) }}
            </p>
            <p>
              {{ isChinese ? '凭证模式' : 'Credential mode' }}:
              {{ selectedDetail?.credentialMode || '-' }}
              /
              {{ selectedDetail?.credentialMask || '-' }}
            </p>
            <p>
              {{ isChinese ? '最近失败原因' : 'Last failure reason' }}:
              {{ selectedDetail?.lastFailureReason || '-' }}
            </p>
          </div>

          <div v-if="activeTab === 'logicalViews' && hasDisplayValue(selectedDetail?.description)" class="detail-copy">
            <p>{{ selectedDetail.description }}</p>
          </div>

          <div class="evidence-card">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">snapshot evidence</p>
                <h3 class="detail-subtitle">{{ isChinese ? 'Metadata Snapshot 旁证' : 'Metadata snapshot evidence' }}</h3>
              </div>
            </div>
            <div class="summary-card-grid">
              <article
                v-for="item in snapshotSummary"
                :key="item.label"
                class="summary-card"
              >
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
            <div class="snapshot-list">
              <div
                v-for="snapshot in selectedSnapshots"
                :key="snapshot.snapshotId || snapshot.objectKey"
                class="snapshot-item"
              >
                <strong>{{ snapshot.objectKey || snapshot.objectName || snapshot.objectType }}</strong>
                <span>{{ snapshot.objectType || '-' }} · {{ snapshot.datasourceCode || '-' }}</span>
                <span>{{ snapshot.freshnessStatus || '-' }} / {{ snapshot.slaStatus || '-' }} / {{ snapshot.queryabilityStatus || '-' }}</span>
              </div>
            </div>
          </div>

          <div
            v-for="group in detailAuxGroups"
            :key="group.key"
            class="evidence-card"
          >
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">{{ group.key }}</p>
                <h3 class="detail-subtitle">{{ group.title }}</h3>
              </div>
            </div>
            <div class="snapshot-list">
              <div
                v-for="(item, index) in group.items"
                :key="`${group.key}-${index}`"
                class="snapshot-item"
              >
                <strong>{{ auxPrimaryValue(item) }}</strong>
                <span>{{ auxSecondaryValue(item) }}</span>
                <span>{{ auxTertiaryValue(item) }}</span>
              </div>
            </div>
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.94));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.runtime-hero,
.control-card,
.asset-grid > article {
  padding: 24px;
}

.runtime-hero {
  display: grid;
  gap: 14px;
}

.runtime-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #047857;
}

.runtime-title,
.section-title,
.detail-subtitle {
  margin: 0;
  color: #0f172a;
}

.runtime-summary,
.runtime-note,
.empty-state,
.detail-copy p {
  margin: 0;
  color: #334155;
  line-height: 1.6;
}

.section-heading,
.filter-grid {
  display: flex;
  gap: 16px;
  align-items: end;
  justify-content: space-between;
}

.filter-grid {
  flex-wrap: wrap;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}

.field-label {
  color: #475569;
  font-size: 13px;
}

.asset-grid {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(300px, 0.9fr) minmax(380px, 1.3fr);
}

.catalog-list,
.snapshot-list {
  display: grid;
  gap: 12px;
}

.catalog-item,
.snapshot-item,
.summary-card,
.evidence-card {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.88);
  padding: 14px 16px;
}

.catalog-item {
  display: grid;
  gap: 4px;
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.catalog-item span,
.snapshot-item span {
  color: #475569;
  font-size: 12px;
}

.catalog-item-active {
  border-color: rgba(16, 185, 129, 0.45);
  box-shadow: 0 16px 30px rgba(16, 185, 129, 0.12);
}

.summary-card-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.summary-card-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #475569;
}

.detail-rail,
.catalog-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-copy,
.evidence-card {
  display: grid;
  gap: 12px;
}

.result-banner {
  border-radius: 18px;
  padding: 14px 16px;
}

.result-banner-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

@media (max-width: 1080px) {
  .asset-grid {
    grid-template-columns: 1fr;
  }

  .filter-grid {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
