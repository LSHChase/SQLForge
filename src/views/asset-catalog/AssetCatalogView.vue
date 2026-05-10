<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getParseStatisticsBySql,
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
import SectionHeader from '../common/SectionHeader.vue'
import ToolbarShell from '../common/ToolbarShell.vue'

// Static contract tokens: data asset catalog, Asset catalog, Metadata snapshot evidence, Logical views, Database views.

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
const parseSqlStats = ref([])
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

const usageHeatModel = computed(() => {
  const detail = selectedDetail.value
  if (!detail || !['tables', 'logicalViews', 'databaseViews'].includes(activeTab.value)) {
    return null
  }
  const upstream = Number(detail.upstreamCount || 0)
  const downstream = Number(detail.downstreamCount || 0)
  const snapshots = selectedSnapshots.value.length
  const queryableBoost = detail.queryable ? 10 : 0
  const score = Math.min(100, upstream * 18 + downstream * 22 + snapshots * 8 + queryableBoost)
  let level = 'COLD'
  if (score >= 70) {
    level = 'HOT'
  } else if (score >= 35) {
    level = 'WARM'
  }
  return {
    score,
    level,
    source: isChinese.value
      ? '依据上/下游计数、snapshot 覆盖和 queryable 状态计算的 proxy，不代表真实运行热度。'
      : 'A proxy derived from upstream/downstream counts, snapshot coverage, and queryable state rather than live runtime heat.'
  }
})

const healthSignalCards = computed(() => {
  const detail = selectedDetail.value
  if (!detail) {
    return []
  }
  return [
    {
      key: 'freshness',
      label: isChinese.value ? 'Freshness' : 'Freshness',
      value: detail.freshnessStatus || summarizeDistinct(selectedSnapshots.value.map(item => item.freshnessStatus)),
      evidence: isChinese.value ? 'metadata snapshot 聚合' : 'metadata snapshot aggregate'
    },
    {
      key: 'sla',
      label: 'SLA',
      value: detail.slaStatus || summarizeDistinct(selectedSnapshots.value.map(item => item.slaStatus)),
      evidence: isChinese.value ? 'metadata snapshot 聚合' : 'metadata snapshot aggregate'
    },
    {
      key: 'queryability',
      label: isChinese.value ? 'Queryability' : 'Queryability',
      value: detail.queryabilityStatus || summarizeDistinct(selectedSnapshots.value.map(item => item.queryabilityStatus)),
      evidence: isChinese.value ? '详情字段 + snapshot' : 'detail field + snapshots'
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

const relatedSqlCandidates = computed(() => {
  const detail = selectedDetail.value
  if (!detail || activeTab.value !== 'logicalViews') {
    return []
  }
  const reportCode = String(detail.viewCode || '').trim()
  if (!reportCode) {
    return []
  }
  return parseSqlStats.value.filter(item => String(item.reportCode || '').trim() === reportCode).slice(0, 6)
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
    const [nextDatasources, nextSchemas, nextTables, nextLogicalViews, nextDatabaseViews, nextParseSqlStats] = await Promise.all([
      getGovernanceDatasources(tenantId),
      getMetadataSchemas(tenantId, datasourceCode),
      getMetadataTables(tenantId, datasourceCode, schemaName),
      getLogicalViews(tenantId, datasourceCode),
      getDatabaseViews(tenantId, datasourceCode),
      getParseStatisticsBySql(tenantId)
    ])
    datasources.value = Array.isArray(nextDatasources) ? nextDatasources : []
    schemas.value = Array.isArray(nextSchemas) ? nextSchemas : []
    tables.value = Array.isArray(nextTables) ? nextTables : []
    logicalViews.value = Array.isArray(nextLogicalViews) ? nextLogicalViews : []
    databaseViews.value = Array.isArray(nextDatabaseViews) ? nextDatabaseViews : []
    parseSqlStats.value = Array.isArray(nextParseSqlStats) ? nextParseSqlStats : []

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
  <section class="asset-page" data-testid="asset-page">
    <SectionHeader
      :eyebrow="t('assetCatalog.eyebrow')"
      :title="t('assetCatalog.title')"
      :summary="t('assetCatalog.workspaceSummary')"
      :level="1"
      size="compact"
    />

    <ToolbarShell
      :eyebrow="t('assetCatalog.filters.eyebrow')"
      :title="t('assetCatalog.filters.title')"
      :summary="t('assetCatalog.filters.summary')"
      density="compact"
    >
      <div class="filter-grid">
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.tenant') }}</span>
          <el-input v-model="filterForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('common.fields.datasource') }}</span>
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
          <span class="field-label">{{ t('common.fields.schema') }}</span>
          <el-input v-model="filterForm.schemaName" :placeholder="t('assetCatalog.filters.schemaPlaceholder')" />
        </label>
        <el-button
          type="primary"
          :loading="loading.lists"
          data-testid="asset-refresh"
          @click="refreshLists"
        >
          {{ t('assetCatalog.actions.refresh') }}
        </el-button>
      </div>
    </ToolbarShell>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="asset-error"
    >
      {{ errorMessage }}
    </div>

    <div class="asset-workspace">
      <section class="asset-list-pane">
        <SectionHeader
          :eyebrow="t('assetCatalog.catalog.eyebrow')"
          :title="t('assetCatalog.catalog.title')"
          :summary="t('assetCatalog.catalog.summary', { count: listItems.length })"
          size="compact"
        />

        <el-tabs v-model="activeTab" class="catalog-tabs">
          <el-tab-pane
            v-for="option in tabOptions"
            :key="option.value"
            :label="option.label"
            :name="option.value"
            :data-testid="`asset-tab-${option.value}`"
          />
        </el-tabs>

        <div class="asset-list">
          <button
            v-for="item in listItems"
            :key="itemKey(item)"
            type="button"
            class="asset-list-item"
            :class="{ 'asset-list-item-active': selectedItemKey === itemKey(item) }"
            data-testid="asset-item"
            @click="loadDetail(item)"
          >
            <strong>{{ displayLabel(item) }}</strong>
            <span>{{ displayMeta(item) }}</span>
          </button>
        </div>

        <p v-if="!listItems.length" class="empty-state">
          {{ t('assetCatalog.states.emptyCatalog') }}
        </p>
      </section>

      <section class="asset-detail-pane" data-testid="asset-detail-panel">
        <SectionHeader
          :eyebrow="t('assetCatalog.detail.eyebrow')"
          :title="t('assetCatalog.detail.title')"
          :summary="selectedDetail ? t('assetCatalog.detail.summary') : ''"
          size="compact"
        />

        <p v-if="!selectedDetail && !loading.detail" class="empty-state">
          {{ t('assetCatalog.states.selectAsset') }}
        </p>

        <template v-else>
          <dl class="description-grid">
            <div
              v-for="item in detailHighlights"
              :key="item.key"
              class="description-item"
            >
              <dt>{{ item.label }}</dt>
              <dd>{{ hasDisplayValue(item.value) ? item.value : '-' }}</dd>
            </div>
          </dl>

          <div v-if="activeTab === 'datasources'" class="detail-copy">
            <p>
              {{ t('assetCatalog.detail.connectionEndpoint') }}:
              {{ datasourceEndpoint(selectedDetail) }}
            </p>
            <p>
              {{ t('assetCatalog.detail.credentialMode') }}:
              {{ selectedDetail?.credentialMode || '-' }}
              /
              {{ selectedDetail?.credentialMask || '-' }}
            </p>
            <p>
              {{ t('assetCatalog.detail.lastFailureReason') }}:
              {{ selectedDetail?.lastFailureReason || '-' }}
            </p>
          </div>

          <div v-if="activeTab === 'logicalViews' && hasDisplayValue(selectedDetail?.description)" class="detail-copy">
            <p>{{ selectedDetail.description }}</p>
          </div>

          <section class="detail-section">
            <SectionHeader
              :eyebrow="t('assetCatalog.health.eyebrow')"
              :title="t('assetCatalog.health.title')"
              size="compact"
            />
            <dl class="description-grid">
              <div
                v-for="item in healthSignalCards"
                :key="item.key"
                class="description-item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value || '-' }}</dd>
                <small>{{ item.evidence }}</small>
              </div>
              <div v-if="usageHeatModel" class="description-item heat-item" data-testid="logical-object-usage-heat">
                <dt>{{ t('assetCatalog.health.usageHeatProxy') }}</dt>
                <dd>{{ usageHeatModel.level }} / {{ usageHeatModel.score }}</dd>
                <small>{{ usageHeatModel.source }}</small>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <SectionHeader
              :eyebrow="t('assetCatalog.snapshot.eyebrow')"
              :title="t('assetCatalog.snapshot.title')"
              size="compact"
            />
            <dl class="description-grid">
              <div
                v-for="item in snapshotSummary"
                :key="item.label"
                class="description-item"
              >
                <dt>{{ item.label }}</dt>
                <dd>{{ item.value }}</dd>
              </div>
            </dl>
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
          </section>

          <section
            v-if="activeTab === 'logicalViews'"
            class="detail-section"
            data-testid="logical-object-related-sql"
          >
            <SectionHeader
              :eyebrow="t('assetCatalog.relatedSql.eyebrow')"
              :title="t('assetCatalog.relatedSql.title')"
              :summary="t('assetCatalog.relatedSql.summary')"
              size="compact"
            />
            <p class="detail-copy">
              {{ t('assetCatalog.relatedSql.boundary') }}
            </p>
            <div class="snapshot-list">
              <div
                v-for="item in relatedSqlCandidates"
                :key="item.itemId || item.parseTaskId"
                class="snapshot-item"
              >
                <strong>{{ item.reportCode || item.itemId }}</strong>
                <span>{{ item.highestPriorityLevel || '-' }} / {{ item.highestPriorityScore || '-' }}</span>
                <span>{{ item.datasourceCode || '-' }} · {{ item.issueScenes?.join(', ') || '-' }}</span>
              </div>
            </div>
            <p v-if="!relatedSqlCandidates.length" class="empty-state">
              {{ t('assetCatalog.relatedSql.empty') }}
            </p>
          </section>

          <section
            v-for="group in detailAuxGroups"
            :key="group.key"
            class="detail-section"
          >
            <SectionHeader v-bind="{ eyebrow: group.key, title: group.title }" size="compact" />
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
          </section>
        </template>
      </section>
    </div>
  </section>
</template>

<style scoped>
.asset-page {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-5);
}

.empty-state,
.detail-copy p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.filter-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sqlforge-space-4);
  align-items: end;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-2);
  min-width: 220px;
}

.field-label {
  color: var(--sqlforge-text-secondary);
  font-size: 13px;
}

.asset-workspace {
  display: grid;
  gap: var(--sqlforge-space-5);
  grid-template-columns: minmax(300px, 0.9fr) minmax(380px, 1.3fr);
}

.asset-list-pane,
.asset-detail-pane {
  display: flex;
  flex-direction: column;
  gap: var(--sqlforge-space-4);
  min-width: 0;
  padding-top: var(--sqlforge-space-5);
  border-top: 1px solid var(--sqlforge-border-default);
}

.asset-list,
.snapshot-list {
  display: grid;
  gap: var(--sqlforge-space-3);
}

.asset-list-item,
.snapshot-item,
.description-item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: var(--sqlforge-radius-sm);
  background: rgba(35, 35, 35, 0.72);
  padding: 14px 16px;
}

.asset-list-item {
  display: grid;
  gap: 4px;
  width: 100%;
  text-align: left;
  cursor: pointer;
}

.asset-list-item span,
.snapshot-item span {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
}

.asset-list-item-active {
  border-color: rgba(16, 185, 129, 0.45);
}

.description-grid {
  display: grid;
  gap: var(--sqlforge-space-3);
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.description-item {
  display: grid;
  gap: var(--sqlforge-space-2);
}

.description-item dt {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--sqlforge-text-muted);
}

.description-item dd {
  margin: 0;
  color: var(--sqlforge-text-primary);
  font-weight: 500;
  overflow-wrap: anywhere;
}

.description-item small {
  color: var(--sqlforge-text-secondary);
  font-size: 11px;
  line-height: 1.5;
}

.detail-copy,
.detail-section {
  display: grid;
  gap: var(--sqlforge-space-4);
}

.detail-section {
  padding-top: var(--sqlforge-space-4);
  border-top: 1px solid var(--sqlforge-border-subtle);
}

.result-banner {
  border-radius: var(--sqlforge-radius-sm);
  padding: 14px 16px;
}

.result-banner-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

@media (max-width: 1080px) {
  .asset-workspace {
    grid-template-columns: 1fr;
  }

  .filter-grid {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
