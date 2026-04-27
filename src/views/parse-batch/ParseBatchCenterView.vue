<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  createParseBatch,
  formatRuntimeError,
  getParseBatch,
  getReportBatch,
  importReportBatch,
  ingestParseBatch,
  resolveReportBatchSqls,
  retryParseBatchAccess
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const activeWorkspace = ref('parse')
const parseUploadFile = ref(null)
const reportUploadFile = ref(null)
const parseBatchSessions = ref([])
const reportBatchSessions = ref([])
const parseBatchDetail = ref(null)
const reportBatchDetail = ref(null)
const errorMessage = ref('')

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
  rawContent:
    "report_code,datasource,sql_text\nRPT_A,hetu_main,SELECT * FROM orders WHERE dt = '2026-04-01'\nRPT_B,unavailable_ds,SELECT * FROM vw_orders WHERE dt = '2026-04-02'\n"
})

const retryForm = reactive({
  failureFilter: 'UNAVAILABLE',
  datasourceCode: 'hetu_main',
  forceRecheckAvailability: false
})

const reportBatchForm = reactive({
  tenantId: 'tenant-a',
  batchName: 'report-batch-alpha',
  fileType: 'TXT',
  reportCodeField: 'report_code',
  datasourceCode: 'hetu_main',
  stage: 'PROD',
  priority: 'high',
  rawContent: 'RPT_A|Revenue Report|hetu_main|PROD|high\nRPT_B|Ops Report|hetu_main|PROD|medium\n'
})

const isChinese = computed(() => locale.value === 'zh-CN')
const parseBatchStatusCards = computed(() => {
  if (!parseBatchDetail.value) {
    return []
  }
  return [
    {
      label: isChinese.value ? '批次状态' : 'Batch status',
      value: parseBatchDetail.value.status
    },
    {
      label: isChinese.value ? '总记录数' : 'Total records',
      value: parseBatchDetail.value.totalRecords
    },
    {
      label: isChinese.value ? '成功' : 'Success',
      value: parseBatchDetail.value.successRecords
    },
    {
      label: isChinese.value ? '部分成功' : 'Partial success',
      value: parseBatchDetail.value.partialSuccessRecords
    },
    {
      label: isChinese.value ? '失败' : 'Failed',
      value: parseBatchDetail.value.failedRecords
    },
    {
      label: isChinese.value ? 'Structure 成功率' : 'Structure rate',
      value: formatRate(parseBatchDetail.value.structureParseSuccessRate)
    },
    {
      label: isChinese.value ? 'Access 成功率' : 'Access rate',
      value: formatRate(parseBatchDetail.value.accessParseSuccessRate)
    }
  ].filter(item => item.value !== null && item.value !== undefined && item.value !== '')
})
const reportBatchStatusCards = computed(() => {
  if (!reportBatchDetail.value) {
    return []
  }
  return [
    {
      label: isChinese.value ? '导入状态' : 'Import status',
      value: reportBatchDetail.value.status
    },
    {
      label: isChinese.value ? '报表总数' : 'Total reports',
      value: reportBatchDetail.value.totalReports
    },
    {
      label: isChinese.value ? '已解析 SQL' : 'Resolved reports',
      value: reportBatchDetail.value.resolvedReports
    },
    {
      label: isChinese.value ? '失败数' : 'Failed reports',
      value: reportBatchDetail.value.failedReports
    },
    {
      label: isChinese.value ? '阶段' : 'Stage',
      value: reportBatchDetail.value.stage
    },
    {
      label: isChinese.value ? '优先级' : 'Priority',
      value: reportBatchDetail.value.priority
    }
  ].filter(item => item.value !== null && item.value !== undefined && item.value !== '')
})

const parseFileTypeOptions = ['CSV', 'TXT', 'SQL', 'XLS', 'XLSX', 'ET']
const reportFileTypeOptions = ['TXT', 'CSV', 'XLSX']
const parseImportModeOptions = ['TABULAR_FILE', 'SQL_FILE', 'REPORT_CATALOG']
const retryFilterOptions = ['ALL', 'UNAVAILABLE', 'FAILED']

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

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

const clearError = () => {
  errorMessage.value = ''
}

const upsertSession = (collection, item) => {
  const next = collection.value.filter(entry => entry.batchId !== item.batchId)
  collection.value = [item, ...next]
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

const loadPayloadBase64 = async (file, rawContent, emptyMessage) => {
  if (file) {
    const buffer = await file.arrayBuffer()
    return {
      fileName: file.name,
      contentBase64: encodeArrayBufferToBase64(buffer)
    }
  }
  if (hasDisplayValue(rawContent)) {
    return {
      fileName: 'inline-upload.txt',
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

const buildTemplatePreview = templateColumns => {
  const headers = templateColumns.map(item => item.columnKey).join(',')
  const placeholders = templateColumns
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
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.createParseBatch = false
  }
}

const downloadTemplate = () => {
  if (!parseBatchDetail.value?.templateColumns?.length) {
    errorMessage.value = isChinese.value
      ? '先创建批次，拿到模板列契约后再下载模板。'
      : 'Create a batch first so the template-column contract can be downloaded.'
    return
  }
  downloadTextFile(
    `${parseBatchDetail.value.batchId || 'parse-batch-template'}.csv`,
    buildTemplatePreview(parseBatchDetail.value.templateColumns)
  )
}

const ingestParseBatchFlow = async () => {
  if (!parseBatchDetail.value?.batchId) {
    errorMessage.value = isChinese.value
      ? '请先创建 parse batch。'
      : 'Create a parse batch first.'
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
    const payload = await loadPayloadBase64(
      reportUploadFile.value,
      reportBatchForm.rawContent,
      isChinese.value ? '请上传报表清单文件或填写模拟内容。' : 'Upload a report catalog file or provide inline mock content.'
    )
    reportBatchDetail.value = await importReportBatch({
      tenantId: reportBatchForm.tenantId,
      batchName: reportBatchForm.batchName,
      fileType: reportBatchForm.fileType,
      reportCodeField: reportBatchForm.reportCodeField,
      datasourceCode: reportBatchForm.datasourceCode,
      stage: reportBatchForm.stage,
      priority: reportBatchForm.priority,
      ...payload,
      charset: 'UTF-8'
    })
    upsertSession(reportBatchSessions, reportBatchDetail.value)
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
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.resolveReportBatch = false
  }
}
</script>

<template>
  <section class="runtime-page batch-import-page" data-testid="batch-import-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">parse batch center</p>
        <h1 class="runtime-title">{{ t('parseBatchCenter.title') }}</h1>
        <p class="runtime-summary">{{ t('parseBatchCenter.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页把 parse-batches 与 report-batches/import 两条批量入口放到同一工作面，覆盖模板下载、上传、批次详情、失败记录和 SQL 解析补跑。'
            : 'This page keeps parse-batches and report-batches/import in one workspace, covering template download, upload, batch detail, failure evidence, and SQL-resolution follow-up.'
        }}
      </p>
    </div>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="batch-import-error"
    >
      {{ errorMessage }}
    </div>

    <el-tabs v-model="activeWorkspace" class="batch-tabs">
      <el-tab-pane :label="isChinese ? '批量解析' : 'Parse batches'" name="parse">
        <div class="batch-center__grid">
          <article class="surface-card composer-rail">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">template download + upload</p>
                <h2 class="section-title">{{ isChinese ? 'Parse Batch 配置与模板' : 'Parse batch configuration and template' }}</h2>
              </div>
            </div>

            <div class="form-grid">
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
                  <el-option
                    v-for="item in parseImportModeOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '文件类型' : 'File type' }}</span>
                <el-select v-model="parseBatchForm.fileType">
                  <el-option
                    v-for="item in parseFileTypeOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
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
              <label class="field-block field-block-toggle">
                <span class="field-label">{{ isChinese ? '仅结构解析' : 'Structure-only batch' }}</span>
                <el-switch v-model="parseBatchForm.structureParseOnly" />
              </label>
              <label class="field-block field-block-wide">
                <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
                <input
                  type="file"
                  data-testid="batch-import-file-input"
                  @change="handleParseFileChange"
                >
              </label>
              <label class="field-block field-block-wide">
                <span class="field-label">{{ isChinese ? '内联内容' : 'Inline content' }}</span>
                <el-input
                  v-model="parseBatchForm.rawContent"
                  type="textarea"
                  :rows="8"
                />
              </label>
            </div>

            <div class="action-row action-row-wrap">
              <el-button
                type="primary"
                :loading="loading.createParseBatch"
                data-testid="batch-import-create"
                @click="createParseBatchFlow"
              >
                {{ isChinese ? '创建批次' : 'Create batch' }}
              </el-button>
              <el-button
                :disabled="!parseBatchDetail?.batchId"
                data-testid="batch-import-download-template"
                @click="downloadTemplate"
              >
                {{ isChinese ? '下载模板' : 'Download template' }}
              </el-button>
              <el-button
                :disabled="!parseBatchDetail?.batchId"
                :loading="loading.ingestParseBatch"
                data-testid="batch-import-ingest"
                @click="ingestParseBatchFlow"
              >
                {{ isChinese ? '上传并解析' : 'Upload and ingest' }}
              </el-button>
              <el-button
                :disabled="!parseBatchDetail?.batchId"
                :loading="loading.refreshParseBatch"
                @click="refreshParseBatchDetail()"
              >
                {{ isChinese ? '刷新批次' : 'Refresh batch' }}
              </el-button>
            </div>

            <div v-if="parseBatchDetail?.templateColumns?.length" class="table-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">template columns</p>
                  <h3 class="detail-title">{{ isChinese ? '模板列契约' : 'Template-column contract' }}</h3>
                </div>
              </div>
              <div class="inline-table">
                <div
                  v-for="item in parseBatchDetail.templateColumns"
                  :key="item.columnKey"
                  class="inline-table__row"
                >
                  <strong>{{ item.columnKey }}</strong>
                  <span>{{ item.displayName }}</span>
                  <span>{{ item.required ? 'required' : 'optional' }}</span>
                </div>
              </div>
            </div>

            <div class="table-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">retry access</p>
                  <h3 class="detail-title">{{ isChinese ? '失败 access parse 补跑' : 'Retry failed access parse' }}</h3>
                </div>
              </div>
              <div class="form-grid">
                <label class="field-block">
                  <span class="field-label">{{ isChinese ? '失败筛选' : 'Failure filter' }}</span>
                  <el-select v-model="retryForm.failureFilter">
                    <el-option
                      v-for="item in retryFilterOptions"
                      :key="item"
                      :label="item"
                      :value="item"
                    />
                  </el-select>
                </label>
                <label class="field-block">
                  <span class="field-label">{{ isChinese ? '覆盖数据源' : 'Override datasource' }}</span>
                  <el-input v-model="retryForm.datasourceCode" />
                </label>
                <label class="field-block field-block-toggle">
                  <span class="field-label">{{ isChinese ? '强制重查可用性' : 'Force recheck availability' }}</span>
                  <el-switch v-model="retryForm.forceRecheckAvailability" />
                </label>
              </div>
              <el-button
                :disabled="!parseBatchDetail?.batchId"
                :loading="loading.retryParseBatch"
                data-testid="batch-import-retry-access"
                @click="retryAccessFlow"
              >
                {{ isChinese ? '执行补跑' : 'Retry access parse' }}
              </el-button>
            </div>
          </article>

          <article class="surface-card evidence-rail" data-testid="batch-import-parse-detail">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">batch list + detail</p>
                <h2 class="section-title">{{ isChinese ? '批次列表、详情与失败记录' : 'Batch list, detail, and failure records' }}</h2>
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
                @click="refreshParseBatchDetail(item.batchId)"
              >
                <strong>{{ item.batchName || item.batchId }}</strong>
                <span>{{ item.status }} · {{ item.fileType || '-' }}</span>
              </button>
            </div>

            <p v-if="!parseBatchDetail" class="empty-state">
              {{ isChinese ? '创建批次后，这里会显示批次详情、统计和失败记录。' : 'After a batch is created, detail, statistics, and failure records appear here.' }}
            </p>

            <template v-else>
              <div class="summary-card-grid">
                <article
                  v-for="item in parseBatchStatusCards"
                  :key="item.label"
                  class="summary-card"
                >
                  <span class="summary-card-label">{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>

              <div class="metric-grid">
                <article class="metric-card">
                  <span class="summary-card-label">Structure parse</span>
                  <strong>
                    {{ parseBatchDetail.structureParseStatistics?.successRecords ?? 0 }}
                    / {{ parseBatchDetail.totalRecords ?? 0 }}
                  </strong>
                </article>
                <article class="metric-card">
                  <span class="summary-card-label">Access parse</span>
                  <strong>
                    {{ parseBatchDetail.accessParseStatistics?.successRecords ?? 0 }}
                    / {{ parseBatchDetail.totalRecords ?? 0 }}
                  </strong>
                </article>
              </div>

              <div class="table-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">report statistics</p>
                    <h3 class="detail-title">{{ isChinese ? '报表统计' : 'Report statistics' }}</h3>
                  </div>
                </div>
                <div class="list-grid">
                  <div
                    v-for="item in parseBatchDetail.reportStatistics || []"
                    :key="item.reportCode"
                    class="list-row"
                  >
                    <strong>{{ item.reportCode }}</strong>
                    <span>{{ item.sqlCount }} SQL</span>
                    <span>{{ item.issueCount }} issues</span>
                    <span>{{ formatRate(item.ratio) }}</span>
                  </div>
                </div>
              </div>

              <div class="table-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">imported records</p>
                    <h3 class="detail-title">{{ isChinese ? '导入记录' : 'Imported records' }}</h3>
                  </div>
                </div>
                <div class="list-grid">
                  <div
                    v-for="item in parseBatchDetail.importedRecords || []"
                    :key="item.itemId"
                    class="list-row list-row-tall"
                  >
                    <strong>{{ item.reportCode || item.itemId }}</strong>
                    <span>{{ item.status }} · {{ item.datasourceCode || '-' }}</span>
                    <span>{{ item.structureSyntaxStatus || '-' }} / {{ item.accessServiceStatus || '-' }}</span>
                    <span>{{ item.issueScenes?.join(', ') || '-' }}</span>
                  </div>
                </div>
              </div>

              <div class="table-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">failure records</p>
                    <h3 class="detail-title">{{ isChinese ? '失败记录' : 'Failure records' }}</h3>
                  </div>
                </div>
                <div class="list-grid">
                  <div
                    v-for="item in parseBatchDetail.failureRecords || []"
                    :key="item.itemId"
                    class="list-row list-row-tall list-row-warning"
                    data-testid="batch-import-failure-record"
                  >
                    <strong>{{ item.reportCode || item.itemId }}</strong>
                    <span>{{ item.status }} · {{ item.datasourceCode || '-' }}</span>
                    <span>{{ item.failureReason || '-' }}</span>
                  </div>
                </div>
              </div>

              <div class="table-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">status history</p>
                    <h3 class="detail-title">{{ isChinese ? '状态轨迹' : 'Status history' }}</h3>
                  </div>
                </div>
                <div class="list-grid">
                  <div
                    v-for="(item, index) in parseBatchDetail.statusHistory || []"
                    :key="`${item.currentStatus || 'history'}-${index}`"
                    class="list-row"
                  >
                    <strong>{{ item.currentStatus || item.status }}</strong>
                    <span>{{ formatInstant(item.occurredAt || item.occurredAtEpochMs) }}</span>
                    <span>{{ item.note || '-' }}</span>
                  </div>
                </div>
              </div>
            </template>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? '报表清单导入' : 'Report catalog import'" name="report">
        <div class="batch-center__grid">
          <article class="surface-card composer-rail">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">report catalog import</p>
                <h2 class="section-title">{{ isChinese ? '报表清单上传与 SQL 解析' : 'Report catalog upload and SQL resolution' }}</h2>
              </div>
            </div>

            <div class="form-grid">
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
                <el-input v-model="reportBatchForm.tenantId" />
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '批次名称' : 'Batch name' }}</span>
                <el-input v-model="reportBatchForm.batchName" />
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '文件类型' : 'File type' }}</span>
                <el-select v-model="reportBatchForm.fileType">
                  <el-option
                    v-for="item in reportFileTypeOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '报表编码字段' : 'Report-code field' }}</span>
                <el-input v-model="reportBatchForm.reportCodeField" />
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '默认数据源' : 'Default datasource' }}</span>
                <el-input v-model="reportBatchForm.datasourceCode" />
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '阶段' : 'Stage' }}</span>
                <el-input v-model="reportBatchForm.stage" />
              </label>
              <label class="field-block">
                <span class="field-label">{{ isChinese ? '优先级' : 'Priority' }}</span>
                <el-input v-model="reportBatchForm.priority" />
              </label>
              <label class="field-block field-block-wide">
                <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
                <input
                  type="file"
                  data-testid="batch-import-report-file-input"
                  @change="handleReportFileChange"
                >
              </label>
              <label class="field-block field-block-wide">
                <span class="field-label">{{ isChinese ? '内联目录内容' : 'Inline catalog content' }}</span>
                <el-input
                  v-model="reportBatchForm.rawContent"
                  type="textarea"
                  :rows="8"
                />
              </label>
            </div>

            <div class="action-row action-row-wrap">
              <el-button
                type="primary"
                :loading="loading.importReportBatch"
                data-testid="batch-import-report-import"
                @click="importReportBatchFlow"
              >
                {{ isChinese ? '导入清单' : 'Import catalog' }}
              </el-button>
              <el-button
                :disabled="!reportBatchDetail?.batchId"
                :loading="loading.resolveReportBatch"
                data-testid="batch-import-report-resolve"
                @click="resolveReportSqlsFlow"
              >
                {{ isChinese ? '解析 SQL' : 'Resolve SQLs' }}
              </el-button>
              <el-button
                :disabled="!reportBatchDetail?.batchId"
                :loading="loading.refreshReportBatch"
                @click="refreshReportBatchDetail()"
              >
                {{ isChinese ? '刷新批次' : 'Refresh batch' }}
              </el-button>
            </div>
          </article>

          <article class="surface-card evidence-rail" data-testid="batch-import-report-detail">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">report batch detail</p>
                <h2 class="section-title">{{ isChinese ? '报表批次列表与详情' : 'Report-batch list and detail' }}</h2>
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
                @click="refreshReportBatchDetail(item.batchId)"
              >
                <strong>{{ item.batchName || item.batchId }}</strong>
                <span>{{ item.status }} · {{ item.fileType || '-' }}</span>
              </button>
            </div>

            <p v-if="!reportBatchDetail" class="empty-state">
              {{ isChinese ? '导入报表清单后，这里会显示待解析和已解析 SQL。' : 'After importing a report catalog, pending and resolved SQL entries appear here.' }}
            </p>

            <template v-else>
              <div class="summary-card-grid">
                <article
                  v-for="item in reportBatchStatusCards"
                  :key="item.label"
                  class="summary-card"
                >
                  <span class="summary-card-label">{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>

              <div class="table-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">report items</p>
                    <h3 class="detail-title">{{ isChinese ? '报表条目' : 'Report items' }}</h3>
                  </div>
                </div>
                <div class="list-grid">
                  <div
                    v-for="item in reportBatchDetail.reportItems || []"
                    :key="item.itemId"
                    class="list-row list-row-tall"
                  >
                    <strong>{{ item.reportCode }}</strong>
                    <span>{{ item.reportName || '-' }}</span>
                    <span>{{ item.status }} · {{ item.structureSyntaxStatus || '-' }}</span>
                    <span>{{ item.issueScenes?.join(', ') || '-' }}</span>
                  </div>
                </div>
              </div>

              <div class="table-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">status history</p>
                    <h3 class="detail-title">{{ isChinese ? '状态轨迹' : 'Status history' }}</h3>
                  </div>
                </div>
                <div class="list-grid">
                  <div
                    v-for="(item, index) in reportBatchDetail.statusHistory || []"
                    :key="`${item.currentStatus || item.status}-${index}`"
                    class="list-row"
                  >
                    <strong>{{ item.currentStatus || item.status }}</strong>
                    <span>{{ formatInstant(item.occurredAt || item.occurredAtEpochMs) }}</span>
                    <span>{{ item.note || '-' }}</span>
                  </div>
                </div>
              </div>
            </template>
          </article>
        </div>
      </el-tab-pane>
    </el-tabs>
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
    radial-gradient(circle at top right, rgba(251, 191, 36, 0.09), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.94));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.runtime-hero,
.batch-center__grid > article {
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
  color: #92400e;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: #0f172a;
}

.runtime-summary,
.runtime-note,
.empty-state {
  margin: 0;
  color: #334155;
  line-height: 1.6;
}

.batch-center__grid {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 1.2fr);
  gap: 24px;
}

.batch-tabs :deep(.el-tabs__item) {
  font-weight: 600;
}

.composer-rail,
.evidence-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-heading,
.parse-card__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.field-block-toggle {
  justify-content: flex-end;
}

.field-label {
  font-size: 13px;
  color: #475569;
}

.action-row {
  display: flex;
  gap: 12px;
}

.action-row-wrap {
  flex-wrap: wrap;
}

.table-card,
.metric-card,
.summary-card,
.session-item,
.list-row {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.88);
  padding: 14px 16px;
}

.summary-card-grid,
.metric-grid,
.list-grid,
.session-list {
  display: grid;
  gap: 12px;
}

.summary-card-grid,
.metric-grid {
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

.session-item {
  display: grid;
  gap: 4px;
  text-align: left;
  cursor: pointer;
}

.session-item span,
.list-row span {
  color: #475569;
  font-size: 12px;
}

.session-item-active {
  border-color: rgba(251, 191, 36, 0.45);
  box-shadow: 0 16px 30px rgba(251, 191, 36, 0.12);
}

.metric-card strong,
.summary-card strong,
.list-row strong {
  color: #0f172a;
}

.inline-table {
  display: grid;
  gap: 10px;
}

.inline-table__row,
.list-row {
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr 1fr;
  gap: 12px;
  align-items: center;
}

.list-row-tall {
  grid-template-columns: 1.2fr 1fr 1fr 1.2fr;
}

.list-row-warning {
  background: rgba(254, 242, 242, 0.92);
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
  .batch-center__grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .inline-table__row,
  .list-row,
  .list-row-tall {
    grid-template-columns: 1fr;
  }
}
</style>
