<script setup>
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import SqlEditorField from '../common/SqlEditorField.vue'
import { useI18n } from 'vue-i18n'
import BatchDetailFields from './BatchDetailFields.vue'
import BatchSummaryCards from './BatchSummaryCards.vue'
import { useParseBatchCenter } from './useParseBatchCenter'

const { t } = useI18n()

const {
  activeWorkspace,
  parseBatchSessions,
  reportBatchSessions,
  parseBatchDetail,
  reportBatchDetail,
  errorMessage,
  parseCreateDialogVisible,
  parseImportDialogVisible,
  parseTemplateDialogVisible,
  reportTemplateDialogVisible,
  reportImportDialogVisible,
  batchSelectorDrawerVisible,
  batchSelectorKind,
  parseResultDialogVisible,
  parseStatisticsDialogVisible,
  reportResultDialogVisible,
  reportStatisticsDialogVisible,
  activeParseResultTab,
  activeParseStatisticsTab,
  activeReportResultTab,
  activeReportGroupDetailTab,
  activeReportStatisticsTab,
  parseItemDetailDialogVisible,
  reportItemDetailDialogVisible,
  reportGroupDetailDialogVisible,
  fieldHelpDialogVisible,
  fieldHelpDialogTitle,
  fieldHelpDialogMessage,
  selectedParseItem,
  selectedReportItem,
  reportSqlDetailSearchCode,
  reportSqlPagination,
  parseBatchListPagination,
  reportBatchListPagination,
  reportStatisticsSqlPagination,
  reportStatisticsIssueScenePagination,
  loading,
  parseBatchForm,
  reportBatchForm,
  parseFileTypeOptions,
  parseImportModeOptions,
  directInputModeOptions,
  parserModeOptions,
  DIRECT_SQL_PREVIEW_LIMIT,
  DASHBOARD_PREVIEW_LIMIT,
  REPORT_SQL_PAGE_SIZE_OPTIONS,
  LIST_PAGE_SIZE_OPTIONS,
  isChinese,
  parseBatchStatusCards,
  reportBatchStatusCards,
  parseFailureRecords,
  reportItems,
  parseImportedRecords,
  parseIssueStatistics,
  parseReportStatistics,
  reportParseStatistics,
  reportImportanceStatistics,
  reportBackendSqlStatistics,
  reportPriorityMatrix,
  reportSqlStatisticsCards,
  reportSqlDetailItems,
  reportSqlDetailFailureItems,
  reportSqlDetailGroups,
  selectedReportGroup,
  selectedReportGroupFailureItems,
  reportSqlDetailTotalCount,
  reportGroupDetailTitle,
  reportIssueStatistics,
  reportLogicalObjectStatistics,
  parseImportedRecordsPreview,
  parseImportedRecordsOmittedCount,
  parseFailureRecordsPreview,
  parseFailureRecordsOmittedCount,
  parseIssueStatisticsPreview,
  parseIssueStatisticsOmittedCount,
  parseReportStatisticsPreview,
  parseReportStatisticsOmittedCount,
  reportGroupsDashboardPreview,
  reportGroupsDashboardOmittedCount,
  reportIssueStatisticsPage,
  reportImportanceStatisticsPreview,
  reportImportanceStatisticsOmittedCount,
  reportViewStatisticsPreview,
  reportViewStatisticsOmittedCount,
  reportSqlStatisticsPreview,
  reportSqlStatisticsOmittedCount,
  reportPriorityMatrixPreview,
  reportPriorityMatrixOmittedCount,
  reportLogicalObjectStatisticsPreview,
  reportLogicalObjectStatisticsOmittedCount,
  templateColumns,
  directSqlRows,
  directSqlPreview,
  directSqlOmittedCount,
  parseSessionsSummary,
  reportSessionsSummary,
  formatPercent,
  formatInstant,
  displayValue,
  formatJson,
  issueSceneHelp,
  issueSceneListHelp,
  helpTextForKey,
  openFieldHelp,
  parseItemDetailFields,
  reportItemDetailFields,
  hasIssueOrFailure,
  buildDiagnosticSummary,
  issueSceneCodesForItem,
  issueLocationText,
  parseTemplatePreview,
  reportTemplatePreview,
  handleParseFileChange,
  handleReportFileChange,
  createParseBatchFlow,
  downloadTemplate,
  ingestParseBatchFlow,
  refreshParseBatchDetail,
  retryAccessFlow,
  openParseItemDetail,
  importReportBatchFlow,
  refreshReportBatchDetail,
  resolveReportSqlsFlow,
  openReportGroupDetail,
  openWholeReportBatchSqlDetail,
  openReportStatistics,
  applyWholeReportSqlFilter,
  handleReportSqlPageChange,
  handleReportSqlPageSizeChange,
  applyReportStatisticsSqlFilter,
  handleReportStatisticsSqlPageChange,
  handleReportStatisticsSqlPageSizeChange,
  handleReportStatisticsIssueScenePageChange,
  handleReportStatisticsIssueScenePageSizeChange,
  openReportItemDetail,
  openBatchSelector,
  openParseSession,
  openReportSession,
  handleBatchSelectorPageChange,
  handleBatchSelectorPageSizeChange
} = useParseBatchCenter()
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

    <el-tabs v-model="activeWorkspace" class="workspace-tabs">
      <el-tab-pane :label="isChinese ? '批量解析' : 'Parse batches'" name="parse">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">current batch workbench</p>
            <h2 class="section-title">{{ isChinese ? '当前批量解析批次' : 'Current parse batch' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '本页主体只展示当前批次概览、解析结果和解析统计；创建与导入参数都在弹窗中完成。'
                  : 'The page body only shows the current batch overview, parse results, and statistics. Create and ingest parameters stay in dialogs.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button data-testid="batch-import-download-template" @click="parseTemplateDialogVisible = true">
              {{ isChinese ? '批量模板' : 'Batch template' }}
            </el-button>
            <el-button type="primary" data-testid="batch-import-create" @click="parseCreateDialogVisible = true">
              {{ isChinese ? '创建批次' : 'Create batch' }}
            </el-button>
            <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-ingest" @click="parseImportDialogVisible = true">
              {{ isChinese ? '导入内容' : 'Ingest content' }}
            </el-button>
            <el-button :loading="loading.refreshParseBatch" @click="refreshParseBatchDetail()">
              {{ isChinese ? '刷新详情' : 'Refresh detail' }}
            </el-button>
            <el-button @click="openBatchSelector('parse')">
              {{ isChinese ? '选择批次' : 'Select batch' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid current-batch-grid" data-testid="batch-import-current-workbench">
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
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-parse-detail" @click="parseResultDialogVisible = true">
                  {{ isChinese ? '解析结果' : 'Parse results' }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-parse-statistics" @click="parseStatisticsDialogVisible = true">
                  {{ isChinese ? '解析统计' : 'Parse statistics' }}
                </el-button>
              </div>
            </div>

            <BatchSummaryCards v-if="parseBatchDetail" :items="parseBatchStatusCards" />

            <div v-else class="empty-stage">
              <strong>{{ isChinese ? '暂无 parse batch' : 'No parse batch selected' }}</strong>
              <p>{{ isChinese ? '使用“创建批次”建立批次，再通过“导入内容”上传模板文件或粘贴多条 SQL。' : 'Use Create batch first, then Ingest content to upload a template file or paste multiple SQL statements.' }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? '报表导入' : 'Report catalog import'" name="report">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">report catalog import</p>
            <h2 class="section-title">{{ isChinese ? '当前报表导入批次' : 'Current report import batch' }}</h2>
            <p class="section-summary">
              {{
                isChinese
                  ? '报表导入按 report_code 分组，导入参数在弹窗中完成，首屏保留批次概览与结果入口。'
                  : 'Report imports are grouped by report_code. Import parameters stay in the dialog, while the first screen keeps the batch overview and result entry points.'
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button data-testid="batch-import-report-template" @click="reportTemplateDialogVisible = true">
              {{ isChinese ? '宽表模板' : 'Wide template' }}
            </el-button>
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
            <el-button @click="openBatchSelector('report')">
              {{ isChinese ? '选择批次' : 'Select batch' }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid current-batch-grid" data-testid="batch-import-report-current-workbench">
          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">current report batch</p>
                <h3 class="section-title">{{ isChinese ? '当前批次概览' : 'Current batch overview' }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :disabled="!reportBatchDetail?.batchId"
                  data-testid="batch-import-report-batch-sql-detail"
                  @click="openWholeReportBatchSqlDetail"
                >
                  {{ isChinese ? '整个批次 SQL 详情' : 'Whole batch SQL detail' }}
                </el-button>
                <el-button :disabled="!reportBatchDetail?.batchId" data-testid="batch-import-report-statistics" @click="openReportStatistics">
                  {{ isChinese ? '解析统计' : 'Parse statistics' }}
                </el-button>
              </div>
            </div>

            <BatchSummaryCards
              v-if="reportBatchDetail"
              :items="reportBatchStatusCards"
              :help-text-for-key="helpTextForKey"
              :open-field-help="openFieldHelp"
            />

            <BatchSummaryCards
              v-if="reportBatchDetail"
              data-testid="batch-import-report-sql-statistics"
              :items="reportSqlStatisticsCards"
              :help-text-for-key="helpTextForKey"
              :open-field-help="openFieldHelp"
            />

            <div v-if="reportBatchDetail" class="report-list" data-testid="batch-import-report-sql-detail">
              <button
                v-for="group in reportGroupsDashboardPreview"
                :key="group.reportCode"
                type="button"
                class="report-item"
                data-testid="batch-import-report-group-open"
                @click="openReportGroupDetail(group)"
              >
                <div class="session-item-top">
                  <strong>{{ group.reportCode }}</strong>
                  <span class="status-pill">{{ group.total }} SQL</span>
                </div>
                <p>{{ displayValue(group.reportName) }}</p>
                <p>
                  {{ isChinese ? '已解析' : 'Resolved' }}: {{ group.resolved }}
                  · {{ isChinese ? '失败' : 'Failed' }}: {{ group.failed }}
                  · Structure: {{ formatPercent(group.structureRate) }}
                  · Access: {{ formatPercent(group.accessRate) }}
                </p>
                <span class="detail-link">{{ isChinese ? '查看本报表 SQL 明细' : 'View this report SQL detail' }}</span>
              </button>
              <div v-if="reportGroupsDashboardOmittedCount > 0" class="preview-note">
                {{
                  isChinese
                    ? `仅展示前 ${DASHBOARD_PREVIEW_LIMIT} 个报表分组入口，另有 ${reportGroupsDashboardOmittedCount} 个分组未展开。`
                    : `Showing the first ${DASHBOARD_PREVIEW_LIMIT} report groups; ${reportGroupsDashboardOmittedCount} more groups are omitted.`
                }}
              </div>
              <div v-if="!reportItems.length" class="empty-state">
                {{ isChinese ? '导入后会在这里看到本批次报表与 SQL 概览。' : 'Imported report and SQL overview appears here.' }}
              </div>
            </div>

            <div v-else class="empty-stage">
              <strong>{{ isChinese ? '暂无 report batch' : 'No report batch selected' }}</strong>
              <p>{{ isChinese ? '使用“导入报表批次”上传文件或粘贴 report_code + 多 SQL 宽表。' : 'Use Import report batch to upload a file or paste a report_code + multi-SQL wide table.' }}</p>
            </div>
          </main>
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
        <label class="field-block">
          <span class="field-label">解析工具 / Parser tool</span>
          <el-select v-model="parseBatchForm.parserMode" data-testid="batch-import-parse-batch-parser-mode">
            <el-option
              v-for="option in parserModeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
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
        <label class="field-block field-block-wide">
          <span class="field-label">{{ isChinese ? '上传文件' : 'Upload file' }}</span>
          <input type="file" data-testid="batch-import-file-input" @change="handleParseFileChange">
        </label>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="parseBatchForm.rawContent"
            :label="parseBatchForm.directInputMode === 'SQL_LINES' ? (isChinese ? '多条 SQL 直接输入' : 'Direct multi-SQL input') : (isChinese ? '内联内容' : 'Inline content')"
            :rows="10"
            :copy-label="isChinese ? '复制' : 'Copy'"
            :format-label="isChinese ? '格式化' : 'Format'"
            :format-enabled="parseBatchForm.directInputMode === 'SQL_LINES'"
            data-testid="batch-import-dialog-sql-input"
          />
        </div>
      </div>

      <div v-if="directSqlPreview.length" class="preview-list">
        <article v-for="item in directSqlPreview" :key="item.reportCode" class="preview-item">
          <strong>{{ item.reportCode }}</strong>
          <span>{{ item.datasource }}</span>
          <SqlCodeBlock
            :value="item.sqlText"
            :label="item.reportCode"
            :copy-label="isChinese ? '复制' : 'Copy'"
            compact
          />
        </article>
        <div
          v-if="directSqlOmittedCount > 0"
          class="preview-note"
          data-testid="batch-import-direct-sql-preview-truncated"
        >
          {{
            isChinese
              ? `仅预览前 ${DIRECT_SQL_PREVIEW_LIMIT} 条，提交时仍导入全部 ${directSqlRows.length} 条 SQL。`
              : `Previewing the first ${DIRECT_SQL_PREVIEW_LIMIT}; submit still imports all ${directSqlRows.length} SQL rows.`
          }}
        </div>
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
          <div v-if="!templateColumns.length" class="empty-state">
            {{ isChinese ? '先创建批次，拿到模板列契约后再下载模板。' : 'Create a batch first so the template-column contract can be downloaded.' }}
          </div>
        </div>
        <SqlCodeBlock
          v-if="templateColumns.length"
          :value="parseTemplatePreview"
          :label="isChinese ? '模板预览' : 'Template preview'"
          :copy-label="isChinese ? '复制' : 'Copy'"
          :auto-format="false"
        />
      </div>
      <template #footer>
        <el-button @click="parseTemplateDialogVisible = false">{{ isChinese ? '关闭' : 'Close' }}</el-button>
        <el-button :disabled="!templateColumns.length" @click="downloadTemplate">
          {{ isChinese ? '下载模板' : 'Download template' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportTemplateDialogVisible" :title="isChinese ? '宽表模板' : 'Wide table template'" width="760px">
      <div class="template-sheet" data-testid="batch-import-report-template-dialog">
        <p class="section-kicker sqlforge-code-label">report_code,sql_1,sql_2,sql_3,...,sql_100</p>
        <p class="result-copy">
          {{
            isChinese
              ? '每行代表一个报表，report_code 作为分组键，sql_1、sql_2 等列承载同一报表下的多条 SQL；空单元格会被忽略。'
              : 'Each row is one report. report_code is the grouping key, while sql_1, sql_2, and later columns hold SQL rows under the same report; empty cells are ignored.'
          }}
        </p>
        <SqlCodeBlock
          :value="reportTemplatePreview"
          :label="isChinese ? '宽表模板预览' : 'Wide template preview'"
          :copy-label="isChinese ? '复制' : 'Copy'"
          :auto-format="false"
        />
      </div>
      <template #footer>
        <el-button @click="reportTemplateDialogVisible = false">{{ isChinese ? '关闭' : 'Close' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="parseResultDialogVisible"
      :title="isChinese ? '当前批次解析结果' : 'Current batch parse results'"
      width="980px"
      data-testid="batch-import-parse-detail"
    >
      <el-tabs v-model="activeParseResultTab" data-testid="batch-import-parse-result-tabs">
        <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
          <div class="summary-grid">
            <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? 'SQL 明细' : 'SQL detail'" name="sql">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">SQL-level parse detail</p>
            <div
              v-if="parseImportedRecordsOmittedCount > 0"
              class="preview-note"
              data-testid="batch-import-large-batch-preview"
            >
              {{
                isChinese
                  ? `仅展示前 ${parseImportedRecordsPreview.length} 条 SQL 明细，另有 ${parseImportedRecordsOmittedCount} 条已省略；统计概览仍按完整批次计算。`
                  : `Showing the first ${parseImportedRecordsPreview.length} SQL rows; ${parseImportedRecordsOmittedCount} more are omitted while summaries still use the full batch.`
              }}
            </div>
            <div class="report-list">
              <article
                v-for="(item, index) in parseImportedRecordsPreview"
                :key="item.itemId || item.recordId || index"
                class="report-item"
                data-testid="batch-import-parse-sql-detail"
              >
                <div class="session-item-top">
                  <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
                  <span class="status-pill">{{ displayValue(item.status) }}</span>
                </div>
                <p>
                  {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                  · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="item.sqlColumnName || item.itemId || 'SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="batch-import-parse-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-parse-item-detail-open" @click="openParseItemDetail(item)">
                    {{ isChinese ? '查看详情' : 'View detail' }}
                  </el-button>
                </div>
              </article>
              <div v-if="!parseImportedRecords.length" class="empty-state">
                {{ isChinese ? '当前批次还没有 SQL 明细。' : 'No SQL rows in the current batch yet.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '失败记录' : 'Failure records'" name="failures">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">Failure records</p>
            <div class="failure-list">
              <article
                v-for="(item, index) in parseFailureRecordsPreview"
                :key="item.recordId || item.id || index"
                class="failure-item"
                data-testid="batch-import-failure-record"
              >
                <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
                <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <p v-if="issueSceneCodesForItem(item).length">
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText || item.sqlPreview"
                  :value="item.sqlText || item.sqlPreview"
                  :label="isChinese ? '失败 SQL' : 'Failed SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                />
                <p v-else>{{ displayValue(item.message) }}</p>
                <button
                  type="button"
                  class="detail-link detail-link-button"
                  data-testid="batch-import-parse-failure-detail-open"
                  @click="openParseItemDetail(item)"
                >
                  {{ isChinese ? '查看解析详情' : 'View parse detail' }}
                </button>
              </article>
              <div
                v-if="parseFailureRecordsOmittedCount > 0"
                class="preview-note"
                data-testid="batch-import-parse-failure-preview"
              >
                {{
                  isChinese
                    ? `仅展示前 ${parseFailureRecordsPreview.length} 条失败记录，另有 ${parseFailureRecordsOmittedCount} 条已省略。`
                    : `Showing the first ${parseFailureRecordsPreview.length} failed rows; ${parseFailureRecordsOmittedCount} more are omitted.`
                }}
              </div>
              <div v-if="!parseFailureRecords.length" class="empty-state">
                {{ isChinese ? '当前没有失败记录。' : 'No failure records in the current batch.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '原始证据' : 'Raw evidence'" name="raw">
          <pre class="code-block">{{ formatJson(parseBatchDetail) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="parseItemDetailDialogVisible"
      :title="isChinese ? '批量解析记录详情' : 'Parse batch item detail'"
      width="920px"
      data-testid="batch-import-parse-item-detail"
    >
      <div v-if="selectedParseItem" class="dialog-stack">
        <BatchDetailFields
          :fields="parseItemDetailFields"
          :display-value="displayValue"
          :issue-scene-list-help="issueSceneListHelp"
        />
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">SQL text</p>
          <SqlCodeBlock
            :value="displayValue(selectedParseItem.sqlText || selectedParseItem.sqlTemplateText)"
            :label="isChinese ? 'SQL 文本' : 'SQL text'"
            :copy-label="isChinese ? '复制' : 'Copy'"
            data-testid="batch-import-selected-parse-sql"
          />
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="parseStatisticsDialogVisible" :title="isChinese ? '当前批次解析统计' : 'Current batch parse statistics'" width="920px">
      <el-tabs v-model="activeParseStatisticsTab" data-testid="batch-import-parse-statistics-tabs">
        <el-tab-pane :label="isChinese ? '问题场景' : 'Issue scenes'" name="issue">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">parse statistics</p>
            <div class="stat-list">
              <div v-for="item in parseIssueStatisticsPreview" :key="item.issueScene" class="contract-item">
                <strong>
                  {{ item.issueScene }}
                  <span
                    v-if="issueSceneHelp(item.issueScene)"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneHelp(item.issueScene)"
                  >?</span>
                </strong>
                <span>{{ displayValue(item.affectedRecords) }} · {{ formatPercent(item.ratio) }}</span>
              </div>
              <div
                v-if="parseIssueStatisticsOmittedCount > 0"
                class="preview-note preview-note-compact"
                data-testid="batch-import-parse-statistics-preview"
              >
                {{
                  isChinese
                    ? `另有 ${parseIssueStatisticsOmittedCount} 个问题场景未展开。`
                    : `${parseIssueStatisticsOmittedCount} more issue scenes are omitted.`
                }}
              </div>
              <div v-if="!parseIssueStatistics.length" class="empty-state">
                {{ isChinese ? '当前没有问题场景统计。' : 'No issue statistics yet.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '报表维度' : 'By report'" name="report">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">report-level statistics</p>
            <div class="stat-list">
              <div v-for="item in parseReportStatisticsPreview" :key="item.reportCode" class="contract-item">
                <strong>{{ item.reportCode }}</strong>
                <span>{{ displayValue(item.sqlCount) }} SQL · {{ displayValue(item.issueCount) }} issues</span>
              </div>
              <div v-if="parseReportStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                {{
                  isChinese
                    ? `另有 ${parseReportStatisticsOmittedCount} 个报表统计项未展开。`
                    : `${parseReportStatisticsOmittedCount} more report statistic rows are omitted.`
                }}
              </div>
              <div v-if="!parseReportStatistics.length" class="empty-state">
                {{ isChinese ? '当前没有报表维度统计。' : 'No report statistics yet.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '失败记录' : 'Failure records'" name="failures">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">Failure records</p>
            <div class="failure-list">
              <article
                v-for="(item, index) in parseFailureRecordsPreview"
                :key="item.recordId || item.id || index"
                class="failure-item"
                data-testid="batch-import-failure-record"
              >
                <strong>{{ item.reportCode || item.recordId || item.id || `#${index + 1}` }}</strong>
                <span>{{ displayValue(item.failureReason || item.errorCode || item.status) }}</span>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <p v-if="issueSceneCodesForItem(item).length">
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText || item.sqlPreview"
                  :value="item.sqlText || item.sqlPreview"
                  :label="isChinese ? '失败 SQL' : 'Failed SQL'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                />
                <p v-else>{{ displayValue(item.message) }}</p>
                <button
                  type="button"
                  class="detail-link detail-link-button"
                  data-testid="batch-import-parse-failure-detail-open"
                  @click="openParseItemDetail(item)"
                >
                  {{ isChinese ? '查看解析详情' : 'View parse detail' }}
                </button>
              </article>
              <div
                v-if="parseFailureRecordsOmittedCount > 0"
                class="preview-note"
                data-testid="batch-import-parse-failure-preview"
              >
                {{
                  isChinese
                    ? `仅展示前 ${parseFailureRecordsPreview.length} 条失败记录，另有 ${parseFailureRecordsOmittedCount} 条已省略。`
                    : `Showing the first ${parseFailureRecordsPreview.length} failed rows; ${parseFailureRecordsOmittedCount} more are omitted.`
                }}
              </div>
              <div v-if="!parseFailureRecords.length" class="empty-state">
                {{ isChinese ? '当前没有失败记录。' : 'No failure records in the current batch.' }}
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

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
          <span class="field-label">解析工具 / Parser tool</span>
          <el-select v-model="reportBatchForm.parserMode" data-testid="batch-import-report-batch-parser-mode">
            <el-option
              v-for="option in parserModeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
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
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="reportBatchForm.rawContent"
            :label="isChinese ? '内联清单' : 'Inline report catalog'"
            :rows="8"
            :copy-label="isChinese ? '复制' : 'Copy'"
            :format-label="isChinese ? '格式化' : 'Format'"
            :format-enabled="false"
            data-testid="batch-import-report-dialog-sql-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ isChinese ? '取消' : 'Cancel' }}</el-button>
        <el-button type="primary" :loading="loading.importReportBatch" @click="importReportBatchFlow">
          {{ isChinese ? '导入报表批次' : 'Import report batch' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reportResultDialogVisible"
      :title="isChinese ? '整个报表批次 SQL 详情' : 'Whole report batch SQL detail'"
      width="1040px"
      data-testid="batch-import-report-result-dialog"
    >
      <el-tabs v-model="activeReportResultTab" data-testid="batch-import-report-result-tabs">
        <el-tab-pane :label="isChinese ? '报表分组' : 'Report groups'" name="groups">
          <div class="report-list">
            <article
              v-for="group in reportSqlDetailGroups"
              :key="group.reportCode"
              class="report-item"
            >
              <div class="session-item-top">
                <strong>{{ group.reportCode }}</strong>
                <span class="status-pill">{{ group.total }} SQL</span>
              </div>
              <p>
                {{ displayValue(group.reportName) }}
                · {{ isChinese ? '已解析' : 'Resolved' }} {{ group.resolved }}
                · {{ isChinese ? '失败' : 'Failed' }} {{ group.failed }}
                · Structure {{ formatPercent(group.structureRate) }}
                · Access {{ formatPercent(group.accessRate) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportGroupDetail(group)">
                {{ isChinese ? '打开本报表 SQL 明细' : 'Open report SQL detail' }}
              </button>
            </article>
            <div v-if="!reportSqlDetailGroups.length" class="empty-state">
              {{ isChinese ? '当前没有报表分组。' : 'No report groups yet.' }}
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sql">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">SQL-level parse detail</p>
            <div class="filter-row" data-testid="batch-import-report-sql-filter">
              <el-input
                v-model="reportSqlDetailSearchCode"
                :placeholder="isChinese ? '按报表编码筛选' : 'Filter by report code'"
                clearable
              />
              <el-button :loading="loading.reportSqlDetail" @click="applyWholeReportSqlFilter">
                {{ isChinese ? '查询' : 'Search' }}
              </el-button>
            </div>
            <div
              v-if="reportSqlDetailTotalCount > reportSqlDetailItems.length"
              class="preview-note"
              data-testid="batch-import-report-large-batch-preview"
            >
              {{
                isChinese
                  ? `当前第 ${reportSqlPagination.pageNumber} 页展示 ${reportSqlDetailItems.length} 条 SQL，筛选后共 ${reportSqlDetailTotalCount} 条；概览仍按完整批次汇总。`
                  : `Page ${reportSqlPagination.pageNumber} shows ${reportSqlDetailItems.length} SQL rows out of ${reportSqlDetailTotalCount}; summaries still use the full batch.`
              }}
            </div>
            <div v-loading="loading.reportSqlDetail" class="report-list">
              <article
                v-for="(item, index) in reportSqlDetailItems"
                :key="item.itemId || `${item.reportCode}-${index}`"
                class="report-item"
                data-testid="batch-import-report-drawer-sql-detail"
              >
                <div class="session-item-top">
                  <strong>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }}</strong>
                  <span class="status-pill">{{ displayValue(item.status) }}</span>
                </div>
                <p>
                  {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                  · {{ isChinese ? 'SQL 序号' : 'SQL ordinal' }}: {{ displayValue(item.sqlOrdinalInReport) }}
                  · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-report-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="isChinese ? 'SQL 输出' : 'SQL output'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="batch-import-report-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-report-item-detail-open" @click="openReportItemDetail(item)">
                    {{ isChinese ? '查看详情' : 'View detail' }}
                  </el-button>
                </div>
              </article>
              <div v-if="!reportSqlDetailItems.length" class="empty-state">
                {{ isChinese ? '当前没有 SQL 清单。' : 'No SQL rows yet.' }}
              </div>
            </div>
            <el-pagination
              v-if="reportSqlDetailTotalCount > reportSqlPagination.pageSize"
              class="pagination-row"
              layout="total, sizes, prev, pager, next"
              :total="reportSqlDetailTotalCount"
              :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
              :page-size="reportSqlPagination.pageSize"
              :current-page="reportSqlPagination.pageNumber"
              @current-change="handleReportSqlPageChange"
              @size-change="handleReportSqlPageSizeChange"
            />
          </section>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '失败 SQL' : 'Failed SQL'" name="failures">
          <p class="section-kicker sqlforge-code-label">failed sql detail</p>
          <div class="failure-list">
            <article
              v-for="(item, index) in reportSqlDetailFailureItems"
              :key="item.itemId || index"
              class="failure-item"
              data-testid="batch-import-report-failure-record"
            >
              <strong>{{ item.reportCode || item.itemId || `#${index + 1}` }}</strong>
              <span>{{ displayValue(item.failureReason || item.status) }}</span>
              <p>{{ displayValue(item.sqlColumnName || item.sqlOrdinalInReport) }} · {{ displayValue(item.parseTaskId) }}</p>
              <p
                v-if="hasIssueOrFailure(item)"
                class="diagnostic-line"
                data-testid="batch-import-report-diagnostic"
              >
                {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportItemDetail(item)">
                {{ isChinese ? '查看失败详情' : 'View failure detail' }}
              </button>
            </article>
            <div v-if="!reportSqlDetailFailureItems.length" class="empty-state">
              {{ isChinese ? '当前没有失败 SQL。' : 'No failed SQL rows.' }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="reportGroupDetailDialogVisible"
      :title="reportGroupDetailTitle"
      width="1040px"
      data-testid="batch-import-report-group-detail-dialog"
    >
      <el-tabs v-if="selectedReportGroup" v-model="activeReportGroupDetailTab" data-testid="batch-import-report-group-tabs">
        <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
          <div class="summary-grid" data-testid="batch-import-report-scoped-summary">
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '报表编码' : 'Report code' }}</span>
              <strong>{{ selectedReportGroup.reportCode }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">SQL</span>
              <strong>{{ selectedReportGroup.total }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '已解析' : 'Resolved' }}</span>
              <strong>{{ selectedReportGroup.resolved }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ isChinese ? '失败' : 'Failed' }}</span>
              <strong>{{ selectedReportGroup.failed }}</strong>
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sql">
          <section class="detail-card" data-testid="batch-import-report-scoped-sql-detail">
            <p class="section-kicker sqlforge-code-label">single report SQL-level parse detail</p>
            <div v-loading="loading.reportSqlDetail" class="report-list">
              <article
                v-for="(item, index) in selectedReportGroup.previewItems"
                :key="item.itemId || `${selectedReportGroup.reportCode}-${index}`"
                class="report-item"
                data-testid="batch-import-report-scoped-sql-row"
              >
                <div class="session-item-top">
                  <strong>{{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }}</strong>
                  <span class="status-pill">{{ displayValue(item.status) }}</span>
                </div>
                <p>
                  {{ displayValue(item.reportName) }} · {{ displayValue(item.datasourceCode || item.stage) }}
                  · {{ displayValue(item.priority) }}
                </p>
                <p>
                  {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                  · {{ isChinese ? 'SQL 序号' : 'SQL ordinal' }}: {{ displayValue(item.sqlOrdinalInReport) }}
                  · {{ isChinese ? '状态' : 'Status' }}: {{ displayValue(item.status) }}
                </p>
                <p>
                  Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-report-diagnostic"
                >
                  {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="isChinese ? 'SQL 输出' : 'SQL output'"
                  :copy-label="isChinese ? '复制' : 'Copy'"
                  compact
                  data-testid="batch-import-report-scoped-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-report-item-detail-open" @click="openReportItemDetail(item)">
                    {{ isChinese ? '查看详情' : 'View detail' }}
                  </el-button>
                </div>
              </article>
              <div v-if="selectedReportGroup.omittedItemCount > 0" class="preview-note">
                {{
                  isChinese
                    ? `该报表仅展示前 ${selectedReportGroup.previewItems.length} 条 SQL，另有 ${selectedReportGroup.omittedItemCount} 条已省略。`
                    : `This report shows the first ${selectedReportGroup.previewItems.length} SQL rows; ${selectedReportGroup.omittedItemCount} more are omitted.`
                }}
              </div>
            </div>
            <el-pagination
              v-if="reportSqlDetailTotalCount > reportSqlPagination.pageSize"
              class="pagination-row"
              layout="total, sizes, prev, pager, next"
              :total="reportSqlDetailTotalCount"
              :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
              :page-size="reportSqlPagination.pageSize"
              :current-page="reportSqlPagination.pageNumber"
              @current-change="handleReportSqlPageChange"
              @size-change="handleReportSqlPageSizeChange"
            />
          </section>
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '失败 SQL' : 'Failed SQL'" name="failures">
          <p class="section-kicker sqlforge-code-label">failed sql detail</p>
          <div class="failure-list">
            <article
              v-for="(item, index) in selectedReportGroupFailureItems"
              :key="item.itemId || index"
              class="failure-item"
              data-testid="batch-import-report-failure-record"
            >
              <strong>{{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }}</strong>
              <span>{{ displayValue(item.failureReason || item.status) }}</span>
              <p
                v-if="hasIssueOrFailure(item)"
                class="diagnostic-line"
                data-testid="batch-import-report-diagnostic"
              >
                {{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportItemDetail(item)">
                {{ isChinese ? '查看失败详情' : 'View failure detail' }}
              </button>
            </article>
            <div v-if="!selectedReportGroupFailureItems.length" class="empty-state">
              {{ isChinese ? '本报表没有失败 SQL。' : 'No failed SQL rows in this report.' }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="reportItemDetailDialogVisible"
      :title="isChinese ? '报表 SQL 解析详情' : 'Report SQL parse detail'"
      width="920px"
      data-testid="batch-import-report-item-detail"
    >
      <div v-if="selectedReportItem" class="dialog-stack">
        <BatchDetailFields
          :fields="reportItemDetailFields"
          :display-value="displayValue"
          :issue-scene-list-help="issueSceneListHelp"
        />
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">SQL output</p>
          <SqlCodeBlock
            :value="displayValue(selectedReportItem.sqlText)"
            :label="isChinese ? 'SQL 输出' : 'SQL output'"
            :copy-label="isChinese ? '复制' : 'Copy'"
            data-testid="batch-import-selected-report-sql"
          />
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="reportStatisticsDialogVisible" :title="isChinese ? '当前报表批次解析统计' : 'Current report batch parse statistics'" width="960px">
      <div class="dialog-stack">
        <section class="detail-card">
          <p class="section-kicker sqlforge-code-label">report-level statistics</p>
          <el-tabs v-model="activeReportStatisticsTab" class="statistics-tabs" data-testid="batch-import-report-statistics-tabs">
            <el-tab-pane :label="t('parseBatchCenter.reportStatistics.tabs.issueScenes')" name="issueScene">
              <el-table
                v-loading="loading.reportStatistics"
                :data="reportIssueStatisticsPage"
                border
                data-testid="batch-import-report-statistics-issue-scene"
              >
                <el-table-column :label="t('parseBatchCenter.reportStatistics.columns.issueScene')" min-width="260" show-overflow-tooltip>
                  <template #default="{ row }">
                    <span class="issue-scene-line">
                      <span class="issue-scene-code">{{ row.issueScene }}</span>
                      <el-tooltip
                        v-if="issueSceneHelp(row.issueScene)"
                        v-bind="{ content: issueSceneHelp(row.issueScene) }"
                        placement="top"
                        teleported
                      >
                        <span class="help-dot issue-scene-help" tabindex="0" aria-label="issue scene help">{{ t('common.helpMark') }}</span>
                      </el-tooltip>
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="affectedSqlCount" :label="t('parseBatchCenter.reportStatistics.columns.affectedSql')" width="100" />
                <el-table-column :label="t('parseBatchCenter.reportStatistics.columns.severity')" width="100">
                  <template #default="{ row }">{{ displayValue(row.severity) }}</template>
                </el-table-column>
                <el-table-column prop="reportCount" :label="t('parseBatchCenter.reportStatistics.columns.reportCount')" width="90" />
                <el-table-column prop="logicalObjectCount" :label="t('parseBatchCenter.reportStatistics.columns.logicalObjectCount')" width="110" />
                <el-table-column :label="t('parseBatchCenter.reportStatistics.columns.ratio')" width="90">
                  <template #default="{ row }">{{ formatPercent(row.ratio) }}</template>
                </el-table-column>
                <template #empty>
                  <p class="empty-copy">
                    {{ t('parseBatchCenter.reportStatistics.states.emptyIssueScenes') }}
                  </p>
                </template>
              </el-table>
              <el-pagination
                v-if="reportIssueStatistics.length > reportStatisticsIssueScenePagination.pageSize"
                class="pagination-row"
                layout="total, sizes, prev, pager, next"
                :total="reportIssueStatistics.length"
                :page-sizes="LIST_PAGE_SIZE_OPTIONS"
                :page-size="reportStatisticsIssueScenePagination.pageSize"
                :current-page="reportStatisticsIssueScenePagination.pageNumber"
                @current-change="handleReportStatisticsIssueScenePageChange"
                @size-change="handleReportStatisticsIssueScenePageSizeChange"
              />
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '重要程度' : 'Importance'" name="importance">
              <div class="stat-list">
                <div
                  v-for="item in reportImportanceStatisticsPreview"
                  :key="item.importanceBucket"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-importance"
                >
                  <strong>{{ item.importanceBucket }}</strong>
                  <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</span>
                </div>
                <div v-if="reportImportanceStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportImportanceStatisticsOmittedCount} 个重要程度统计项未展开。`
                      : `${reportImportanceStatisticsOmittedCount} more importance rows are omitted.`
                  }}
                </div>
                <div v-if="!reportImportanceStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有重要程度统计。' : 'No importance statistics yet.' }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '报表视角' : 'Report view'" name="report">
              <div class="stat-list">
                <div
                  v-for="item in reportViewStatisticsPreview"
                  :key="item.reportCode"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-report-view"
                >
                  <strong>{{ item.reportCode }}</strong>
                  <span>
                    {{ displayValue(item.sqlCount ?? item.total) }} SQL
                    · {{ displayValue(item.issueCount ?? item.failed) }} issues
                    · {{ formatPercent(item.issueSqlRatio ?? item.structureRate) }}
                  </span>
                  <span v-if="item.mergeCandidate" class="preview-note preview-note-compact">
                    {{
                      isChinese
                        ? `建议合并复核：${displayValue(item.mergeCandidateSqlCount)} 条 SQL。${displayValue(item.mergeCandidateReason)}`
                        : `Merge review: ${displayValue(item.mergeCandidateSqlCount)} SQL. ${displayValue(item.mergeCandidateReason)}`
                    }}
                  </span>
                </div>
                <div v-if="reportViewStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportViewStatisticsOmittedCount} 个报表统计项未展开。`
                      : `${reportViewStatisticsOmittedCount} more report rows are omitted.`
                  }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sqlList">
              <div class="filter-row" data-testid="batch-import-report-statistics-sql-filter">
                <el-input
                  v-model="reportStatisticsSqlPagination.reportCode"
                  :placeholder="isChinese ? '按报表编码筛选 SQL 清单' : 'Filter SQL list by report code'"
                  clearable
                />
                <el-button :loading="loading.reportStatistics" @click="applyReportStatisticsSqlFilter">
                  {{ isChinese ? '查询' : 'Search' }}
                </el-button>
              </div>
              <div class="stat-list">
                <div
                  v-for="item in reportSqlStatisticsPreview"
                  :key="item.itemId"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-sql-list"
                >
                  <strong>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId }}</strong>
                  <span>{{ item.highestPriorityLevel }} · {{ item.issueCount }} issues · {{ displayValue(item.logicalObjectKeys) }}</span>
                  <span>
                    {{ t('parseBatchCenter.reportStatistics.labels.issueScenes') }}:
                    <el-tooltip
                      v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                      v-bind="{ content: issueSceneListHelp(issueSceneCodesForItem(item)) }"
                      placement="top"
                      teleported
                    >
                      <span class="help-dot issue-scene-help" tabindex="0" aria-label="issue scene help">{{ t('common.helpMark') }}</span>
                    </el-tooltip>
                    {{ displayValue(issueSceneCodesForItem(item)) }}
                  </span>
                  <span>{{ t('parseBatchCenter.reportStatistics.labels.location') }}: {{ issueLocationText(item) }}</span>
                </div>
                <div
                  v-if="reportSqlStatisticsOmittedCount > 0"
                  class="preview-note preview-note-compact"
                  data-testid="batch-import-report-sql-statistics-preview"
                >
                  {{
                    isChinese
                      ? `SQL 清单仅展示前 ${reportSqlStatisticsPreview.length} 条，另有 ${reportSqlStatisticsOmittedCount} 条未展开。`
                      : `SQL list shows the first ${reportSqlStatisticsPreview.length} rows; ${reportSqlStatisticsOmittedCount} more are omitted.`
                  }}
                </div>
                <div v-if="!reportBackendSqlStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有 SQL 清单统计。' : 'No SQL list statistics yet.' }}
                </div>
              </div>
              <el-pagination
                v-if="Number(reportParseStatistics.sqlStatisticTotalCount || 0) > reportStatisticsSqlPagination.pageSize"
                class="pagination-row"
                layout="total, sizes, prev, pager, next"
                :total="Number(reportParseStatistics.sqlStatisticTotalCount || 0)"
                :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
                :page-size="reportStatisticsSqlPagination.pageSize"
                :current-page="reportStatisticsSqlPagination.pageNumber"
                @current-change="handleReportStatisticsSqlPageChange"
                @size-change="handleReportStatisticsSqlPageSizeChange"
              />
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '优先级视角' : 'Priority view'" name="priority">
              <div class="stat-list">
                <div
                  v-for="item in reportPriorityMatrixPreview"
                  :key="`${item.priorityLevel}-${item.urgencyBucket}`"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-priority"
                >
                  <strong>{{ item.priorityLevel }} · {{ item.urgencyBucket }}</strong>
                  <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</span>
                </div>
                <div v-if="reportPriorityMatrixOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportPriorityMatrixOmittedCount} 个优先级矩阵项未展开。`
                      : `${reportPriorityMatrixOmittedCount} more priority rows are omitted.`
                  }}
                </div>
                <div v-if="!reportPriorityMatrix.length" class="empty-state">
                  {{ isChinese ? '当前没有优先级矩阵统计。' : 'No priority matrix statistics yet.' }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="isChinese ? '逻辑对象视角' : 'Logical objects'" name="logicalObject">
              <div class="stat-list">
                <div
                  v-for="item in reportLogicalObjectStatisticsPreview"
                  :key="item.objectKey"
                  class="contract-item"
                  data-testid="batch-import-report-statistics-logical-object"
                >
                  <strong>{{ item.objectKey }}</strong>
                  <span>{{ item.hitCount }} SQL · {{ displayValue(item.reportCodes) }}</span>
                </div>
                <div v-if="reportLogicalObjectStatisticsOmittedCount > 0" class="preview-note preview-note-compact">
                  {{
                    isChinese
                      ? `另有 ${reportLogicalObjectStatisticsOmittedCount} 个逻辑对象未展开。`
                      : `${reportLogicalObjectStatisticsOmittedCount} more logical objects are omitted.`
                  }}
                </div>
                <div v-if="!reportLogicalObjectStatistics.length" class="empty-state">
                  {{ isChinese ? '当前没有逻辑对象命中。' : 'No logical object hits yet.' }}
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (isChinese ? '字段说明' : 'Field help')" width="560px">
      <p class="result-copy">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ isChinese ? '知道了' : 'Close' }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="batchSelectorDrawerVisible"
      :title="batchSelectorKind === 'report' ? (isChinese ? '选择报表批次' : 'Select report batch') : (isChinese ? '选择解析批次' : 'Select parse batch')"
      size="38%"
    >
      <div class="session-list">
        <button
          v-for="item in (batchSelectorKind === 'report' ? reportBatchSessions : parseBatchSessions)"
          :key="item.batchId"
          type="button"
          class="session-item"
          :class="{
            'session-item-active': batchSelectorKind === 'report'
              ? reportBatchDetail?.batchId === item.batchId
              : parseBatchDetail?.batchId === item.batchId
          }"
          :data-testid="batchSelectorKind === 'report' ? 'batch-import-report-item' : 'batch-import-parse-batch-item'"
          @click="batchSelectorKind === 'report' ? openReportSession(item.batchId) : openParseSession(item.batchId)"
        >
          <div class="session-item-top">
            <strong>{{ item.batchName || item.batchId }}</strong>
            <span class="status-pill">{{ item.status || '-' }}</span>
          </div>
          <p>{{ item.batchId }}</p>
          <span>{{ formatInstant(item.createdAt || item.updatedAt) }}</span>
        </button>
      </div>
      <el-pagination
        class="pagination-row"
        layout="total, sizes, prev, pager, next"
        :total="batchSelectorKind === 'report' ? reportBatchListPagination.totalCount : parseBatchListPagination.totalCount"
        :page-sizes="LIST_PAGE_SIZE_OPTIONS"
        :page-size="batchSelectorKind === 'report' ? reportBatchListPagination.pageSize : parseBatchListPagination.pageSize"
        :current-page="batchSelectorKind === 'report' ? reportBatchListPagination.pageNo : parseBatchListPagination.pageNo"
        @current-change="handleBatchSelectorPageChange"
        @size-change="handleBatchSelectorPageSizeChange"
      />
    </el-drawer>
  </section>
</template>

<style scoped src="./parse-batch-center.css"></style>
