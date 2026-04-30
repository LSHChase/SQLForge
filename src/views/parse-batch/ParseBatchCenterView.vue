<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  createParseBatch,
  formatRuntimeError,
  getParseBatch,
  getReportBatch,
  importReportBatch,
  ingestParseBatch,
  listParseBatches,
  listReportBatches,
  resolveReportBatchSqls,
  retryParseBatchAccess
} from '../../services/runtimeGateApi'

const { locale } = useI18n()
const route = useRoute()

const activeWorkspace = ref('parse')
const parseUploadFile = ref(null)
const reportUploadFile = ref(null)
const parseBatchSessions = ref([])
const reportBatchSessions = ref([])
const parseBatchDetail = ref(null)
const reportBatchDetail = ref(null)
const errorMessage = ref('')
const parseCreateDialogVisible = ref(false)
const parseImportDialogVisible = ref(false)
const parseTemplateDialogVisible = ref(false)
const parseDetailDrawerVisible = ref(false)
const reportImportDialogVisible = ref(false)
const reportDetailDrawerVisible = ref(false)

const loading = reactive({
  createParseBatch: false,
  ingestParseBatch: false,
  refreshParseBatch: false,
  retryParseBatch: false,
  importReportBatch: false,
  resolveReportBatch: false,
  refreshReportBatch: false
})

const parseBatchForm = reactive({
  tenantId: 'tenant-a',
  batchName: 'batch-alpha',
  importMode: 'TABULAR_FILE',
  fileType: 'CSV',
  templateVersion: 'v1',
  datasourceCode: 'hetu_main',
  structureParseOnly: false,
  directInputMode: 'SQL_LINES',
  rawContent:
    "SELECT * FROM orders WHERE dt = '2026-04-01';\nSELECT * FROM vw_orders WHERE dt = '2026-04-02';"
})

const retryForm = reactive({
  failureFilter: 'UNAVAILABLE',
  datasourceCode: 'hetu_main',
  forceRecheckAvailability: false
})

const reportBatchForm = reactive({
  tenantId: 'tenant-a',
  batchName: 'report-batch-alpha',
  reportCodeField: 'report_code',
  datasourceCode: 'hetu_main',
  stage: 'PROD',
  priority: 'high',
  rawContent: 'RPT_A|Revenue Report|hetu_main|PROD|high\nRPT_B|Ops Report|hetu_main|PROD|medium\n'
})

const parseFileTypeOptions = ['CSV', 'TXT', 'SQL', 'XLS', 'XLSX', 'ET']
const parseImportModeOptions = ['TABULAR_FILE', 'SQL_FILE', 'REPORT_CATALOG']
const retryFilterOptions = ['ALL', 'UNAVAILABLE', 'FAILED']
const directInputModeOptions = ['SQL_LINES', 'TABULAR_TEXT']

const isChinese = computed(() => locale.value === 'zh-CN')
const parseBatchStatusCards = computed(() => {
  if (!parseBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '批次状态' : 'Batch status', parseBatchDetail.value.status),
    card(isChinese.value ? '总记录数' : 'Total records', parseBatchDetail.value.totalRecords),
    card(isChinese.value ? '成功' : 'Success', parseBatchDetail.value.successRecords),
    card(isChinese.value ? '部分成功' : 'Partial success', parseBatchDetail.value.partialSuccessRecords),
    card(isChinese.value ? '失败' : 'Failed', parseBatchDetail.value.failedRecords),
    card(isChinese.value ? 'Structure 成功率' : 'Structure rate', formatRate(parseBatchDetail.value.structureParseSuccessRate)),
    card(isChinese.value ? 'Access 成功率' : 'Access rate', formatRate(parseBatchDetail.value.accessParseSuccessRate))
  ].filter(item => hasDisplayValue(item.value))
})
const reportBatchStatusCards = computed(() => {
  if (!reportBatchDetail.value) {
    return []
  }
  return [
    card(isChinese.value ? '导入状态' : 'Import status', reportBatchDetail.value.status),
    card(isChinese.value ? '报表总数' : 'Total reports', reportBatchDetail.value.totalReports),
    card(isChinese.value ? '已解析 SQL' : 'Resolved reports', reportBatchDetail.value.resolvedReports),
    card(isChinese.value ? '失败数' : 'Failed reports', reportBatchDetail.value.failedReports),
    card(isChinese.value ? '阶段' : 'Stage', reportBatchDetail.value.stage),
    card(isChinese.value ? '优先级' : 'Priority', reportBatchDetail.value.priority)
  ].filter(item => hasDisplayValue(item.value))
})
const parseFailureRecords = computed(() => {
  const detail = parseBatchDetail.value || {}
  const source =
    detail.failureRecords ||
    detail.failedItems ||
    detail.recordResults ||
    detail.records ||
    []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})
const reportItems = computed(() => {
  const detail = reportBatchDetail.value || {}
  const source =
    detail.reportItems ||
    detail.items ||
    detail.reports ||
    detail.records ||
    []
  return Array.isArray(source) ? source.filter(item => typeof item === 'object') : []
})
const templateColumns = computed(() => parseBatchDetail.value?.templateColumns || [])
const directSqlPreview = computed(() => {
  if (parseBatchForm.directInputMode !== 'SQL_LINES') {
    return []
  }
  return parseBatchForm.rawContent
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
    .map((sqlText, index) => ({
      reportCode: `INLINE_${String(index + 1).padStart(3, '0')}`,
      datasource: parseBatchForm.datasourceCode,
      sqlText
    }))
})
const parseSessionsSummary = computed(() =>
  `${parseBatchSessions.value.length} ${isChinese.value ? '个会话' : 'sessions'}`
)
const reportSessionsSummary = computed(() =>
  `${reportBatchSessions.value.length} ${isChinese.value ? '个批次' : 'batches'}`
)
const parseBatchHistorySummary = computed(() => {
  const totalRecords = parseBatchSessions.value.reduce(
    (sum, item) => sum + Number(item.totalRecords || 0),
    0
  )
  const failedBatches = parseBatchSessions.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(isChinese.value ? '批次总数' : 'Batches', parseBatchSessions.value.length),
    card(isChinese.value ? '总记录数' : 'Records', totalRecords),
    card(isChinese.value ? '失败批次' : 'Failed batches', failedBatches)
  ]
})
const reportBatchHistorySummary = computed(() => {
  const totalReports = reportBatchSessions.value.reduce(
    (sum, item) => sum + Number(item.totalReports || 0),
    0
  )
  const resolvedReports = reportBatchSessions.value.reduce(
    (sum, item) => sum + Number(item.resolvedReports || 0),
    0
  )
  const failedBatches = reportBatchSessions.value.filter(item => String(item.status || '').includes('FAILED')).length
  return [
    card(isChinese.value ? '批次总数' : 'Batches', reportBatchSessions.value.length),
    card(isChinese.value ? '报表总数' : 'Reports', totalReports),
    card(isChinese.value ? '已解析' : 'Resolved', resolvedReports),
    card(isChinese.value ? '失败批次' : 'Failed batches', failedBatches)
  ]
})

const card = (label, value) => ({ label, value })

const hasDisplayValue = value =>
  !(value === null || value === undefined || String(value).trim() === '')

const formatRate = value => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${Number(value).toFixed(2)}`
}

const formatInstant = value => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').replace('Z', ' UTC')
}

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (value === null || value === undefined || String(value).trim() === '') {
    return '-'
  }
  return String(value)
}

const clearError = () => {
  errorMessage.value = ''
}

const upsertSession = (collection, item) => {
  const next = collection.value.filter(entry => entry.batchId !== item.batchId)
  collection.value = [item, ...next]
}

const loadBatchHistories = async () => {
  clearError()
  const [parseResult, reportResult] = await Promise.allSettled([
    listParseBatches(parseBatchForm.tenantId),
    listReportBatches(reportBatchForm.tenantId)
  ])
  if (parseResult.status === 'fulfilled') {
    parseBatchSessions.value = Array.isArray(parseResult.value) ? parseResult.value : []
  }
  if (reportResult.status === 'fulfilled') {
    reportBatchSessions.value = Array.isArray(reportResult.value) ? reportResult.value : []
  }
  if (parseResult.status === 'rejected') {
    errorMessage.value = formatRuntimeError(parseResult.reason)
  }
  if (reportResult.status === 'rejected' && !errorMessage.value) {
    errorMessage.value = formatRuntimeError(reportResult.reason)
  }
}

const encodeArrayBufferToBase64 = buffer => {
  const bytes = new Uint8Array(buffer)
  const chunkSize = 0x8000
  let binary = ''
  for (let index = 0; index < bytes.length; index += chunkSize) {
    const chunk = bytes.subarray(index, index + chunkSize)
    binary += String.fromCharCode(...chunk)
  }
  return window.btoa(binary)
}

const encodeTextToBase64 = text => {
  const encoder = new TextEncoder()
  return encodeArrayBufferToBase64(encoder.encode(text).buffer)
}

const escapeCsvCell = value => `"${String(value || '').replace(/"/g, '""')}"`

const buildInlineSqlCsv = () => {
  const rows = directSqlPreview.value.map(item =>
    [item.reportCode, item.datasource, item.sqlText].map(escapeCsvCell).join(',')
  )
  return ['report_code,datasource,sql_text', ...rows].join('\n')
}

const loadPayloadBase64 = async (file, rawContent, emptyMessage) => {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    const normalizedContent =
      parseBatchForm.directInputMode === 'SQL_LINES' ? buildInlineSqlCsv() : String(rawContent)
    return {
      fileName:
        parseBatchForm.directInputMode === 'SQL_LINES' ? 'inline-multi-sql.csv' : 'inline-upload.txt',
      contentBase64: encodeTextToBase64(normalizedContent)
    }
  }
  throw new Error(emptyMessage)
}

const loadReportPayloadBase64 = async (file, rawContent, emptyMessage) => {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    return {
      fileName: 'inline-report-import.txt',
      contentBase64: encodeTextToBase64(String(rawContent))
    }
  }
  throw new Error(emptyMessage)
}

const downloadTextFile = (fileName, content) => {
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(link.href)
}

const buildTemplatePreview = columns => {
  const headers = columns.map(item => item.columnKey).join(',')
  const placeholders = columns
    .map(item => {
      if (item.columnKey === 'sql_text') {
        return "SELECT * FROM orders WHERE dt = '2026-04-01'"
      }
      if (item.columnKey === 'datasource') {
        return 'hetu_main'
      }
      if (item.columnKey === 'report_code') {
        return 'RPT_SAMPLE'
      }
      if (item.columnKey === 'stage') {
        return 'PROD'
      }
      if (item.columnKey === 'priority') {
        return 'high'
      }
      return ''
    })
    .join(',')
  return `${headers}\n${placeholders}\n`
}

const parseTemplatePreview = computed(() => buildTemplatePreview(templateColumns.value))

const handleParseFileChange = event => {
  const [file] = event.target.files || []
  parseUploadFile.value = file || null
}

const handleReportFileChange = event => {
  const [file] = event.target.files || []
  reportUploadFile.value = file || null
}

const createParseBatchFlow = async () => {
  loading.createParseBatch = true
  clearError()
  try {
    parseBatchDetail.value = await createParseBatch({
      tenantId: parseBatchForm.tenantId,
      batchName: parseBatchForm.batchName,
      importMode: parseBatchForm.importMode,
      fileType: parseBatchForm.fileType,
      templateVersion: parseBatchForm.templateVersion,
      datasourceCode: parseBatchForm.datasourceCode,
      structureParseOnly: parseBatchForm.structureParseOnly
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    await loadBatchHistories()
    parseCreateDialogVisible.value = false
    parseDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.createParseBatch = false
  }
}

const downloadTemplate = () => {
  if (!templateColumns.value.length) {
    errorMessage.value = isChinese.value
      ? '先创建批次，拿到模板列契约后再下载模板。'
      : 'Create a batch first so the template-column contract can be downloaded.'
    return
  }
  downloadTextFile(
    `${parseBatchDetail.value.batchId || 'parse-batch-template'}.csv`,
    parseTemplatePreview.value
  )
}

const ingestParseBatchFlow = async () => {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value ? '请先创建 parse batch。' : 'Create a parse batch first.'
    return
  }
  loading.ingestParseBatch = true
  clearError()
  try {
    const payload = await loadPayloadBase64(
      parseUploadFile.value,
      parseBatchForm.rawContent,
      isChinese.value ? '请上传文件或填写批量内容。' : 'Upload a file or provide inline batch content.'
    )
    parseBatchDetail.value = await ingestParseBatch(parseBatchDetail.value.batchId, parseBatchForm.tenantId, {
      ...payload,
      charset: 'UTF-8'
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    await loadBatchHistories()
    parseImportDialogVisible.value = false
    parseDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.ingestParseBatch = false
  }
}

const refreshParseBatchDetail = async batchId => {
  const targetBatchId = batchId || parseBatchDetail.value?.batchId
  if (!targetBatchId) {
    return
  }
  loading.refreshParseBatch = true
  clearError()
  try {
    parseBatchDetail.value = await getParseBatch(targetBatchId, parseBatchForm.tenantId)
    upsertSession(parseBatchSessions, parseBatchDetail.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.refreshParseBatch = false
  }
}

const retryAccessFlow = async () => {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value
      ? '请先选择一个 parse batch。'
      : 'Select a parse batch first.'
    return
  }
  loading.retryParseBatch = true
  clearError()
  try {
    parseBatchDetail.value = await retryParseBatchAccess(parseBatchDetail.value.batchId, parseBatchForm.tenantId, {
      failureFilter: retryForm.failureFilter,
      datasourceCode: retryForm.datasourceCode,
      forceRecheckAvailability: retryForm.forceRecheckAvailability
    })
    upsertSession(parseBatchSessions, parseBatchDetail.value)
    await loadBatchHistories()
    parseDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.retryParseBatch = false
  }
}

const importReportBatchFlow = async () => {
  loading.importReportBatch = true
  clearError()
  try {
    const payload = await loadReportPayloadBase64(
      reportUploadFile.value,
      reportBatchForm.rawContent,
      isChinese.value ? '请上传报表清单文件或填写模拟内容。' : 'Upload a report catalog file or provide inline mock content.'
    )
    reportBatchDetail.value = await importReportBatch({
      tenantId: reportBatchForm.tenantId,
      batchName: reportBatchForm.batchName,
      reportCodeField: reportBatchForm.reportCodeField,
      datasourceCode: reportBatchForm.datasourceCode,
      stage: reportBatchForm.stage,
      priority: reportBatchForm.priority,
      ...payload,
      charset: 'UTF-8'
    })
    upsertSession(reportBatchSessions, reportBatchDetail.value)
    await loadBatchHistories()
    reportImportDialogVisible.value = false
    reportDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.importReportBatch = false
  }
}

const refreshReportBatchDetail = async batchId => {
  const targetBatchId = batchId || reportBatchDetail.value?.batchId
  if (!targetBatchId) {
    return
  }
  loading.refreshReportBatch = true
  clearError()
  try {
    reportBatchDetail.value = await getReportBatch(targetBatchId, reportBatchForm.tenantId)
    upsertSession(reportBatchSessions, reportBatchDetail.value)
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.refreshReportBatch = false
  }
}

const resolveReportSqlsFlow = async () => {
  if (!reportBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value
      ? '请先导入一个报表批次。'
      : 'Import a report batch first.'
    return
  }
  loading.resolveReportBatch = true
  clearError()
  try {
    reportBatchDetail.value = await resolveReportBatchSqls(reportBatchDetail.value.batchId, reportBatchForm.tenantId)
    upsertSession(reportBatchSessions, reportBatchDetail.value)
    await loadBatchHistories()
    reportDetailDrawerVisible.value = true
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.resolveReportBatch = false
  }
}

const openParseSession = async batchId => {
  await refreshParseBatchDetail(batchId)
  parseDetailDrawerVisible.value = true
}

const openReportSession = async batchId => {
  await refreshReportBatchDetail(batchId)
  reportDetailDrawerVisible.value = true
}

onMounted(async () => {
  const tenantId = String(route.query.tenantId || '').trim()
  if (tenantId) {
    parseBatchForm.tenantId = tenantId
    reportBatchForm.tenantId = tenantId
  }
  await loadBatchHistories()
  const batchId = String(route.query.batchId || '').trim()
  if (!batchId) {
    return
  }
  const kind = String(route.query.kind || 'parse').trim()
  activeWorkspace.value = 'history'
  if (kind === 'report') {
    await openReportSession(batchId)
    return
  }
  await openParseSession(batchId)
})
</script>

<template>
  <section class="runtime-page batch-import-page" data-testid="batch-import-page">
    <header class="page-hero shell-panel">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">{{ isChinese ? '批次筛选' : 'Batch filters' }}</p>
        <h2 class="runtime-title">{{ isChinese ? '创建批次、导入内容与查看结果' : 'Create batches, ingest content, and inspect results' }}</h2>
        <p class="runtime-summary">
          {{
            isChinese
              ? '首屏只保留批次入口与当前结果区；模板、导入和详情都转入弹窗或抽屉。'
              : 'The first screen stays focused on batch entry points and the current result stage. Templates, imports, and details move into dialogs or drawers.'
          }}
        </p>
      </div>
      <div class="hero-inline">
        <span class="hero-pill">{{ parseSessionsSummary }}</span>
        <span class="hero-pill">{{ reportSessionsSummary }}</span>
        <span class="hero-pill hero-pill-muted">{{ isChinese ? '查询条件 + 结果区 + 抽屉' : 'Filters + results + drawers' }}</span>
      </div>
    </header>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="batch-import-error"
    >
      {{ errorMessage }}
    </div>

    <section class="summary-grid history-summary-grid">
      <article v-for="item in parseBatchHistorySummary" :key="`parse-${item.label}`" class="summary-card">
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
      <article v-for="item in reportBatchHistorySummary" :key="`report-${item.label}`" class="summary-card">
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <el-tabs v-model="activeWorkspace" class="workspace-tabs">
      <el-tab-pane :label="isChinese ? '批量解析' : 'Parse batches'" name="parse">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">template download + upload</p>
            <h2 class="section-title">{{ isChinese ? '批次创建、导入与补跑' : 'Batch creation, import, and recovery' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '支持多条 SQL 直接粘贴，也支持模板文件导入；批次详情与失败记录放进抽屉，不再整页堆叠。'
                  : 'Direct multi-SQL paste and template-file import both stay available; batch detail and failures move into drawers instead of filling the whole page.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" data-testid="batch-import-create" @click="parseCreateDialogVisible = true">
              {{ isChinese ? '创建批次' : 'Create batch' }}
            </el-button>
            <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-ingest" @click="parseImportDialogVisible = true">
              {{ isChinese ? '导入内容' : 'Ingest content' }}
            </el-button>
            <el-button :disabled="!templateColumns.length" data-testid="batch-import-download-template" @click="parseTemplateDialogVisible = true">
              {{ isChinese ? '查看模板' : 'Preview template' }}
            </el-button>
            <el-button :loading="loading.refreshParseBatch" @click="refreshParseBatchDetail()">
              {{ isChinese ? '刷新详情' : 'Refresh detail' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid">
          <aside class="shell-panel session-rail">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">parse sessions</p>
                <h3 class="section-title">{{ isChinese ? '批次会话' : 'Batch sessions' }}</h3>
              </div>
            </div>
            <div class="session-list">
              <button
                v-for="item in parseBatchSessions"
                :key="item.batchId"
                type="button"
                class="session-item"
                :class="{ 'session-item-active': parseBatchDetail?.batchId === item.batchId }"
                data-testid="batch-import-parse-batch-item"
                @click="openParseSession(item.batchId)"
              >
                <div class="session-item-top">
                  <strong>{{ item.batchName || item.batchId }}</strong>
                  <span class="status-pill">{{ item.status || 'CREATED' }}</span>
                </div>
                <p>{{ item.batchId }}</p>
                <span>{{ formatInstant(item.createdAt || item.updatedAt) }}</span>
              </button>
              <div v-if="!parseBatchSessions.length" class="empty-state">
                {{ isChinese ? '先创建一个 parse batch。' : 'Create a parse batch to start.' }}
              </div>
            </div>
          </aside>

          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">batch result</p>
                <h3 class="section-title">{{ isChinese ? '当前批次概览' : 'Current batch overview' }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :disabled="!parseBatchDetail?.batchId"
                  data-testid="batch-import-retry-access"
                  @click="retryAccessFlow"
                >
                  {{ isChinese ? '补跑 Access' : 'Retry access' }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" @click="parseDetailDrawerVisible = true">
                  {{ isChinese ? '打开详情抽屉' : 'Open detail drawer' }}
                </el-button>
              </div>
            </div>

            <div v-if="parseBatchDetail" class="summary-grid">
              <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>

            <div v-if="parseBatchDetail" class="result-layout">
              <section class="detail-card">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">Template-column contract</p>
                    <h4 class="detail-title">{{ isChinese ? '模板列契约' : 'Template-column contract' }}</h4>
                  </div>
                  <el-button text @click="parseTemplateDialogVisible = true">
                    {{ isChinese ? '查看模板预览' : 'View template preview' }}
                  </el-button>
                </div>
                <div class="contract-list">
                  <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
                    <strong>{{ item.columnKey }}</strong>
                    <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
                  </div>
                  <div v-if="!templateColumns.length" class="empty-state">
                    {{ isChinese ? '创建批次后会返回模板列契约。' : 'Template-column contract arrives after batch creation.' }}
                  </div>
                </div>
              </section>

              <section class="detail-card">
                <div class="section-heading">
                  <div>
                    <p class="section-kicker sqlforge-code-label">Failure records</p>
                    <h4 class="detail-title">{{ isChinese ? '失败记录' : 'Failure records' }}</h4>
                  </div>
                </div>
                <div class="failure-list">
                  <article
                    v-for="(item, index) in parseFailureRecords.slice(0, 6)"
                    :key="item.recordId || item.id || index"
                    class="failure-item"
                    data-testid="batch-import-failure-record"
                  >
                    <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
                    <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
                    <p>{{ displayValue(item.sqlText || item.message || item.sqlPreview) }}</p>
                  </article>
                  <div v-if="!parseFailureRecords.length" class="empty-state">
                    {{ isChinese ? '当前没有失败记录。' : 'No failure records in the current batch.' }}
                  </div>
                </div>
              </section>
            </div>

            <div v-else class="empty-stage">
              <strong>{{ isChinese ? '暂无 parse batch' : 'No parse batch selected' }}</strong>
              <p>{{ isChinese ? '先创建批次，然后通过弹窗导入多条 SQL 或模板文件。' : 'Create a batch first, then import multi-SQL text or template files through dialogs.' }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? '报表导入' : 'Report catalog import'" name="report">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">report catalog import</p>
            <h2 class="section-title">{{ isChinese ? '报表清单导入与 SQL 解析' : 'Report catalog import and SQL resolution' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '批次表、详情抽屉和导入弹窗分离，避免把报表清单、导入说明和结果全塞在一个长页面里。'
                  : 'Session list, detail drawers, and import dialogs are separated so the report workflow no longer lives in one long stacked page.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button type="primary" data-testid="batch-import-report-import" @click="reportImportDialogVisible = true">
              {{ isChinese ? '导入报表批次' : 'Import report batch' }}
            </el-button>
            <el-button :loading="loading.refreshReportBatch" @click="refreshReportBatchDetail()">
              {{ isChinese ? '刷新详情' : 'Refresh detail' }}
            </el-button>
            <el-button
              :disabled="!reportBatchDetail?.batchId"
              data-testid="batch-import-report-resolve"
              @click="resolveReportSqlsFlow"
            >
              {{ isChinese ? '解析报表 SQL' : 'Resolve report SQLs' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid">
          <aside class="shell-panel session-rail">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">report sessions</p>
                <h3 class="section-title">{{ isChinese ? '报表批次' : 'Report batches' }}</h3>
              </div>
            </div>
            <div class="session-list">
              <button
                v-for="item in reportBatchSessions"
                :key="item.batchId"
                type="button"
                class="session-item"
                :class="{ 'session-item-active': reportBatchDetail?.batchId === item.batchId }"
                data-testid="batch-import-report-item"
                @click="openReportSession(item.batchId)"
              >
                <div class="session-item-top">
                  <strong>{{ item.batchName || item.batchId }}</strong>
                  <span class="status-pill">{{ item.status || 'IMPORTED' }}</span>
                </div>
                <p>{{ item.batchId }}</p>
                <span>{{ formatInstant(item.createdAt || item.updatedAt) }}</span>
              </button>
              <div v-if="!reportBatchSessions.length" class="empty-state">
                {{ isChinese ? '先导入一个报表批次。' : 'Import a report batch to start.' }}
              </div>
            </div>
          </aside>

          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">report items</p>
                <h3 class="section-title">{{ isChinese ? '报表项概览' : 'Report items' }}</h3>
              </div>
              <el-button :disabled="!reportBatchDetail?.batchId" @click="reportDetailDrawerVisible = true">
                {{ isChinese ? '打开详情抽屉' : 'Open detail drawer' }}
              </el-button>
            </div>

            <div v-if="reportBatchDetail" class="summary-grid">
              <article v-for="item in reportBatchStatusCards" :key="item.label" class="summary-card">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>

            <div v-if="reportBatchDetail" class="report-list">
              <article
                v-for="(item, index) in reportItems.slice(0, 8)"
                :key="item.reportCode || item.itemId || index"
                class="report-item"
              >
                <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
                <span>{{ displayValue(item.reportName || item.status) }}</span>
                <p>{{ displayValue(item.datasourceCode || item.stage) }} · {{ displayValue(item.priority || item.resolutionStatus) }}</p>
              </article>
              <div v-if="!reportItems.length" class="empty-state">
                {{ isChinese ? '导入后会在这里看到报表清单。' : 'Imported report items appear here.' }}
              </div>
            </div>

            <div v-else class="empty-stage">
              <strong>{{ isChinese ? '暂无 report batch' : 'No report batch selected' }}</strong>
              <p>{{ isChinese ? '通过导入弹窗上传报表清单，再在详情抽屉里查看解析证据。' : 'Use the import dialog to upload the report catalog, then review evidence in the detail drawer.' }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? '历史总览' : 'History overview'" name="history">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">history overview</p>
            <h2 class="section-title">{{ isChinese ? '解析历史总览' : 'Parse history overview' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '这里直接展示服务端持久化后的批量解析与报表导入记录，可快速回到任意批次详情。'
                  : 'This tab surfaces persisted batch parse and report-import history so you can jump back into any batch detail.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button :loading="loading.refreshParseBatch || loading.refreshReportBatch" @click="loadBatchHistories">
              {{ isChinese ? '刷新历史' : 'Refresh history' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid history-grid">
          <section class="shell-panel">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">parse history</p>
                <h3 class="section-title">{{ isChinese ? '批量解析历史' : 'Batch parse history' }}</h3>
              </div>
            </div>
            <el-table :data="parseBatchSessions" border>
              <el-table-column prop="batchName" :label="isChinese ? '批次名称' : 'Batch name'" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openParseSession(row.batchId)">
                    {{ row.batchName || row.batchId }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
              <el-table-column prop="totalRecords" :label="isChinese ? '总记录' : 'Total records'" min-width="120" />
              <el-table-column prop="successRecords" :label="isChinese ? '成功' : 'Success'" min-width="100" />
              <el-table-column prop="failedRecords" :label="isChinese ? '失败' : 'Failed'" min-width="100" />
              <el-table-column prop="createdAt" :label="isChinese ? '创建时间' : 'Created at'" min-width="170">
                <template #default="{ row }">{{ formatInstant(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </section>

          <section class="shell-panel">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">report history</p>
                <h3 class="section-title">{{ isChinese ? '报表导入历史' : 'Report import history' }}</h3>
              </div>
            </div>
            <el-table :data="reportBatchSessions" border>
              <el-table-column prop="batchName" :label="isChinese ? '批次名称' : 'Batch name'" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openReportSession(row.batchId)">
                    {{ row.batchName || row.batchId }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
              <el-table-column prop="totalReports" :label="isChinese ? '报表总数' : 'Total reports'" min-width="120" />
              <el-table-column prop="resolvedReports" :label="isChinese ? '已解析' : 'Resolved'" min-width="100" />
              <el-table-column prop="failedReports" :label="isChinese ? '失败' : 'Failed'" min-width="100" />
              <el-table-column prop="createdAt" :label="isChinese ? '创建时间' : 'Created at'" min-width="170">
                <template #default="{ row }">{{ formatInstant(row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="parseCreateDialogVisible" :title="isChinese ? '创建 Parse Batch' : 'Create parse batch'" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="parseBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '批次名称' : 'Batch name' }}</span>
          <el-input v-model="parseBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '导入模式' : 'Import mode' }}</span>
          <el-select v-model="parseBatchForm.importMode">
            <el-option v-for="item in parseImportModeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '文件类型' : 'File type' }}</span>
          <el-select v-model="parseBatchForm.fileType">
            <el-option v-for="item in parseFileTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '模板版本' : 'Template version' }}</span>
          <el-input v-model="parseBatchForm.templateVersion" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '默认数据源' : 'Default datasource' }}</span>
          <el-input v-model="parseBatchForm.datasourceCode" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '仅结构解析' : 'Structure-only batch' }}</span>
          <el-switch v-model="parseBatchForm.structureParseOnly" />
        </label>
      </div>
      <template #footer>
        <el-button @click="parseCreateDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.createParseBatch" @click="createParseBatchFlow">
          {{ isChinese ? '创建批次' : 'Create batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseImportDialogVisible" :title="isChinese ? '导入批量内容' : 'Import batch content'" width="820px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '输入方式' : 'Input mode' }}</span>
          <el-select v-model="parseBatchForm.directInputMode">
            <el-option v-for="item in directInputModeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? 'Access 补跑筛选' : 'Retry filter' }}</span>
          <el-select v-model="retryForm.failureFilter">
            <el-option v-for="item in retryFilterOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" data-testid="batch-import-file-input" @change="handleParseFileChange">
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">
            {{ parseBatchForm.directInputMode === 'SQL_LINES' ? (isChinese ? '多条 SQL 直接输入' : 'Direct multi-SQL input') : (isChinese ? '内联内容' : 'Inline content') }}
          </span>
          <el-input v-model="parseBatchForm.rawContent" type="textarea" :rows="10" />
        </label>
      </div>

      <div v-if="directSqlPreview.length" class="preview-list">
        <article v-for="item in directSqlPreview.slice(0, 5)" :key="item.reportCode" class="preview-item">
          <strong>{{ item.reportCode }}</strong>
          <span>{{ item.datasource }}</span>
          <p>{{ item.sqlText }}</p>
        </article>
      </div>

      <template #footer>
        <el-button @click="parseImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.ingestParseBatch" @click="ingestParseBatchFlow">
          {{ isChinese ? '确认导入' : 'Confirm import' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseTemplateDialogVisible" :title="isChinese ? '模板列契约与预览' : 'Template-column contract and preview'" width="760px">
      <div class="template-sheet">
        <p class="section-kicker sqlforge-code-label">Template-column contract</p>
        <div class="contract-list">
          <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
            <strong>{{ item.columnKey }}</strong>
            <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
          </div>
        </div>
        <pre class="code-block">{{ parseTemplatePreview }}</pre>
      </div>
      <template #footer>
        <el-button @click="parseTemplateDialogVisible = false">{{ isChinese ? '关闭' : 'Close' }}</el-button>
        <el-button :disabled="!templateColumns.length" @click="downloadTemplate">
          {{ isChinese ? '下载模板' : 'Download template' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="parseDetailDrawerVisible"
      :title="isChinese ? 'Parse Batch 详情' : 'Parse batch detail'"
      size="46%"
      data-testid="batch-import-parse-detail"
    >
      <div class="drawer-stack">
        <div class="summary-grid">
          <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">Template-column contract</p>
          <div class="contract-list">
            <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
              <strong>{{ item.columnKey }}</strong>
              <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
            </div>
          </div>
        </section>
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">Failure records</p>
          <div class="failure-list">
            <article
              v-for="(item, index) in parseFailureRecords"
              :key="item.recordId || item.id || index"
              class="failure-item"
            >
              <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
              <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
              <p>{{ displayValue(item.sqlText || item.message || item.sqlPreview) }}</p>
            </article>
          </div>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="reportImportDialogVisible" :title="isChinese ? '导入报表批次' : 'Import report batch'" width="820px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="reportBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '批次名称' : 'Batch name' }}</span>
          <el-input v-model="reportBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '报表编码字段' : 'Report code field' }}</span>
          <el-input v-model="reportBatchForm.reportCodeField" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '默认数据源' : 'Default datasource' }}</span>
          <el-input v-model="reportBatchForm.datasourceCode" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '优先级' : 'Priority' }}</span>
          <el-input v-model="reportBatchForm.priority" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" @change="handleReportFileChange">
        </label>
        <div class="field-note field-block-wide">
          {{ isChinese ? '文件类型会根据文件名和内容自动识别，无需手动选择。' : 'File type is auto-detected from the filename and payload content.' }}
        </div>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '内联清单' : 'Inline report catalog' }}</span>
          <el-input v-model="reportBatchForm.rawContent" type="textarea" :rows="8" />
        </label>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.importReportBatch" @click="importReportBatchFlow">
          {{ isChinese ? '导入报表批次' : 'Import report batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="reportDetailDrawerVisible"
      :title="isChinese ? 'Report Batch 详情' : 'Report batch detail'"
      size="46%"
    >
      <div class="drawer-stack">
        <div class="summary-grid">
          <article v-for="item in reportBatchStatusCards" :key="item.label" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">Report items</p>
          <div class="report-list">
            <article
              v-for="(item, index) in reportItems"
              :key="item.reportCode || item.itemId || index"
              class="report-item"
            >
              <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
              <span>{{ displayValue(item.reportName || item.status) }}</span>
              <p>{{ displayValue(item.datasourceCode || item.stage) }} · {{ displayValue(item.priority || item.resolutionStatus) }}</p>
            </article>
          </div>
        </section>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shell-panel {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08), transparent 32%),
    var(--sqlforge-surface-2);
  padding: 20px;
}

.page-hero,
.workspace-toolbar,
.section-heading,
.toolbar-actions,
.hero-inline,
.session-item-top {
  display: flex;
  gap: 12px;
}

.page-hero,
.workspace-toolbar,
.section-heading {
  justify-content: space-between;
  align-items: flex-start;
}

.page-hero,
.workspace-toolbar {
  flex-wrap: wrap;
}

.runtime-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  color: var(--sqlforge-color-brand);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 12px;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.runtime-summary,
.section-summary,
.empty-state,
.empty-stage p,
.session-item p,
.session-item span,
.failure-item p,
.report-item p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.hero-inline,
.toolbar-actions {
  flex-wrap: wrap;
}

.hero-pill,
.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-secondary);
  font-size: 12px;
}

.hero-pill-muted {
  color: var(--sqlforge-text-muted);
}

.history-summary-grid {
  margin-top: -2px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.84fr) minmax(0, 1.16fr);
  gap: 20px;
}

.session-rail,
.detail-stage,
.detail-card,
.summary-card,
.session-item,
.failure-item,
.report-item,
.contract-item,
.preview-item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 16px;
  background: rgba(35, 35, 35, 0.92);
}

.session-list,
.drawer-stack,
.contract-list,
.failure-list,
.report-list,
.preview-list {
  display: grid;
  gap: 12px;
}

.session-item {
  width: 100%;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.table-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--sqlforge-color-brand);
  font: inherit;
  text-align: left;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.field-note {
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.session-item-active {
  border-color: var(--sqlforge-color-brand-border);
  box-shadow: inset 0 0 0 1px rgba(16, 185, 129, 0.25);
}

.detail-stage,
.empty-stage {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-grid,
.dialog-grid {
  display: grid;
  gap: 12px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.result-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.detail-card,
.failure-item,
.report-item,
.contract-item,
.preview-item {
  padding: 14px 16px;
}

.summary-card-label,
.field-label {
  display: block;
  margin-bottom: 8px;
  color: var(--sqlforge-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.dialog-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.template-sheet {
  display: grid;
  gap: 16px;
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.empty-stage {
  align-items: center;
  justify-content: center;
  min-height: 260px;
  border: 1px dashed var(--sqlforge-border-default);
  border-radius: 16px;
}

@media (max-width: 1100px) {
  .workspace-grid,
  .result-layout,
  .dialog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
