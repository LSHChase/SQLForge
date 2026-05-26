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
  parseRewriteTrialRun,
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
  parseRewriteTrialItems,
  parseRewriteTrialCards,
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
  rewriteTrialForParseItem,
  sourceProblemLabels,
  issueRuleLinkLabels,
  parseTemplatePreview,
  reportTemplatePreview,
  handleParseFileChange,
  handleReportFileChange,
  createParseBatchFlow,
  downloadTemplate,
  ingestParseBatchFlow,
  refreshParseBatchDetail,
  createBatchRewriteTrialFlow,
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
        <p class="runtime-eyebrow sqlforge-code-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text001') }}</p>
        <h2 class="runtime-title">{{ t('inline.viewsParseBatchParseBatchCenterView.text002') }}</h2>
        <p class="runtime-summary">
          {{
            t('inline.viewsParseBatchParseBatchCenterView.text003')
          }}
        </p>
      </div>
      <div class="hero-inline">
        <span class="hero-pill">{{ parseSessionsSummary }}</span>
        <span class="hero-pill">{{ reportSessionsSummary }}</span>
        <span class="hero-pill hero-pill-muted">{{ t('inline.viewsParseBatchParseBatchCenterView.text004') }}</span>
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
      <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text005')" name="parse">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">current batch workbench</p>
            <h2 class="section-title">{{ t('inline.viewsParseBatchParseBatchCenterView.text006') }}</h2>
            <p class="section-summary">
              {{
                t('inline.viewsParseBatchParseBatchCenterView.text007')
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button data-testid="batch-import-download-template" @click="parseTemplateDialogVisible = true">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text008') }}
            </el-button>
            <el-button type="primary" data-testid="batch-import-create" @click="parseCreateDialogVisible = true">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text009') }}
            </el-button>
            <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-ingest" @click="parseImportDialogVisible = true">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text010') }}
            </el-button>
            <el-button :loading="loading.refreshParseBatch" @click="refreshParseBatchDetail()">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text011') }}
            </el-button>
            <el-button @click="openBatchSelector('parse')">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text012') }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid current-batch-grid" data-testid="batch-import-current-workbench">
          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">batch result</p>
                <h3 class="section-title">{{ t('inline.viewsParseBatchParseBatchCenterView.text013') }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :disabled="!parseBatchDetail?.batchId || !retryForm.datasourceCode"
                  data-testid="batch-import-retry-access"
                  @click="retryAccessFlow"
                >
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text014') }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-parse-detail" @click="parseResultDialogVisible = true">
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text015') }}
                </el-button>
                <el-button :disabled="!parseBatchDetail?.batchId" data-testid="batch-import-parse-statistics" @click="parseStatisticsDialogVisible = true">
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text016') }}
                </el-button>
                <el-button
                  :disabled="!parseBatchDetail?.batchId"
                  :loading="loading.rewriteTrial"
                  data-testid="batch-import-create-rewrite-trial"
                  @click="createBatchRewriteTrialFlow"
                >
                  {{ t('rewriteTrial.batchCreate') }}
                </el-button>
              </div>
            </div>

            <BatchSummaryCards v-if="parseBatchDetail" :items="parseBatchStatusCards" />

            <BatchSummaryCards
              v-if="parseRewriteTrialRun"
              data-testid="batch-import-rewrite-trial-summary"
              :items="parseRewriteTrialCards"
            />

            <div v-else class="empty-stage">
              <strong>{{ t('inline.viewsParseBatchParseBatchCenterView.text017') }}</strong>
              <p>{{ t('inline.viewsParseBatchParseBatchCenterView.text018') }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text019')" name="report">
        <div class="workspace-toolbar shell-panel">
          <div class="toolbar-copy">
            <p class="section-kicker sqlforge-code-label">report catalog import</p>
            <h2 class="section-title">{{ t('inline.viewsParseBatchParseBatchCenterView.text020') }}</h2>
            <p class="section-summary">
              {{
                t('inline.viewsParseBatchParseBatchCenterView.text021')
              }}
            </p>
          </div>
          <div class="toolbar-actions">
            <el-button data-testid="batch-import-report-template" @click="reportTemplateDialogVisible = true">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text022') }}
            </el-button>
            <el-button type="primary" data-testid="batch-import-report-import" @click="reportImportDialogVisible = true">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text023') }}
            </el-button>
            <el-button :loading="loading.refreshReportBatch" @click="refreshReportBatchDetail()">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text024') }}
            </el-button>
            <el-button
              :disabled="!reportBatchDetail?.batchId"
              data-testid="batch-import-report-resolve"
              @click="resolveReportSqlsFlow"
            >
              {{ t('inline.viewsParseBatchParseBatchCenterView.text025') }}
            </el-button>
            <el-button @click="openBatchSelector('report')">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text026') }}
            </el-button>
          </div>
        </div>

        <div class="workspace-grid current-batch-grid" data-testid="batch-import-report-current-workbench">
          <main class="shell-panel detail-stage">
            <div class="section-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">current report batch</p>
                <h3 class="section-title">{{ t('inline.viewsParseBatchParseBatchCenterView.text027') }}</h3>
              </div>
              <div class="toolbar-actions">
                <el-button
                  :disabled="!reportBatchDetail?.batchId"
                  data-testid="batch-import-report-batch-sql-detail"
                  @click="openWholeReportBatchSqlDetail"
                >
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text028') }}
                </el-button>
                <el-button :disabled="!reportBatchDetail?.batchId" data-testid="batch-import-report-statistics" @click="openReportStatistics">
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text029') }}
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text030') }}: {{ group.resolved }}
                  · {{ t('inline.viewsParseBatchParseBatchCenterView.text031') }}: {{ group.failed }}
                  · Structure: {{ formatPercent(group.structureRate) }}
                  · Access: {{ formatPercent(group.accessRate) }}
                </p>
                <span class="detail-link">{{ t('inline.viewsParseBatchParseBatchCenterView.text032') }}</span>
              </button>
              <div v-if="reportGroupsDashboardOmittedCount > 0" class="preview-note">
                {{
                  isChinese
                    ? `仅展示前 ${DASHBOARD_PREVIEW_LIMIT} 个报表分组入口，另有 ${reportGroupsDashboardOmittedCount} 个分组未展开。`
                    : `Showing the first ${DASHBOARD_PREVIEW_LIMIT} report groups; ${reportGroupsDashboardOmittedCount} more groups are omitted.`
                }}
              </div>
              <div v-if="!reportItems.length" class="empty-state">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text033') }}
              </div>
            </div>

            <div v-else class="empty-stage">
              <strong>{{ t('inline.viewsParseBatchParseBatchCenterView.text034') }}</strong>
              <p>{{ t('inline.viewsParseBatchParseBatchCenterView.text035') }}</p>
            </div>
          </main>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="parseCreateDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text036')" width="760px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text037') }}</span>
          <el-input v-model="parseBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text038') }}</span>
          <el-input v-model="parseBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text039') }}</span>
          <el-select v-model="parseBatchForm.importMode">
            <el-option v-for="item in parseImportModeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text040') }}</span>
          <el-select v-model="parseBatchForm.fileType">
            <el-option v-for="item in parseFileTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text041') }}</span>
          <el-input v-model="parseBatchForm.templateVersion" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text042') }}</span>
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
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text043') }}</span>
          <el-switch v-model="parseBatchForm.structureParseOnly" />
        </label>
      </div>
      <template #footer>
        <el-button @click="parseCreateDialogVisible = false">{{ t('inline.viewsParseBatchParseBatchCenterView.text044') }}</el-button>
        <el-button
          type="primary"
          :disabled="!parseBatchForm.datasourceCode"
          :loading="loading.createParseBatch"
          @click="createParseBatchFlow"
        >
          {{ t('inline.viewsParseBatchParseBatchCenterView.text045') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseImportDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text046')" width="820px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text047') }}</span>
          <el-select v-model="parseBatchForm.directInputMode">
            <el-option v-for="item in directInputModeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text048') }}</span>
          <input type="file" data-testid="batch-import-file-input" @change="handleParseFileChange">
        </label>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="parseBatchForm.rawContent"
            :label="parseBatchForm.directInputMode === 'SQL_LINES' ? (t('inline.viewsParseBatchParseBatchCenterView.text049')) : (t('inline.viewsParseBatchParseBatchCenterView.text050'))"
            :rows="10"
            :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text051')"
            :format-label="t('inline.viewsParseBatchParseBatchCenterView.text052')"
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
            :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text053')"
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
        <el-button @click="parseImportDialogVisible = false">{{ t('inline.viewsParseBatchParseBatchCenterView.text054') }}</el-button>
        <el-button type="primary" :loading="loading.ingestParseBatch" @click="ingestParseBatchFlow">
          {{ t('inline.viewsParseBatchParseBatchCenterView.text055') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="parseTemplateDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text056')" width="760px">
      <div class="template-sheet">
        <p class="section-kicker sqlforge-code-label">Template-column contract</p>
        <div class="contract-list">
          <div v-for="item in templateColumns" :key="item.columnKey" class="contract-item">
            <strong>{{ item.columnKey }}</strong>
            <span>{{ displayValue(item.required) }} · {{ displayValue(item.columnType) }}</span>
          </div>
          <div v-if="!templateColumns.length" class="empty-state">
            {{ t('inline.viewsParseBatchParseBatchCenterView.text057') }}
          </div>
        </div>
        <SqlCodeBlock
          v-if="templateColumns.length"
          :value="parseTemplatePreview"
          :label="t('inline.viewsParseBatchParseBatchCenterView.text058')"
          :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text059')"
          :auto-format="false"
        />
      </div>
      <template #footer>
        <el-button @click="parseTemplateDialogVisible = false">{{ t('inline.viewsParseBatchParseBatchCenterView.text060') }}</el-button>
        <el-button :disabled="!templateColumns.length" @click="downloadTemplate">
          {{ t('inline.viewsParseBatchParseBatchCenterView.text061') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportTemplateDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text062')" width="760px">
      <div class="template-sheet" data-testid="batch-import-report-template-dialog">
        <p class="section-kicker sqlforge-code-label">report_code,sql_1,sql_2,sql_3,...,sql_100</p>
        <p class="result-copy">
          {{
            t('inline.viewsParseBatchParseBatchCenterView.text063')
          }}
        </p>
        <SqlCodeBlock
          :value="reportTemplatePreview"
          :label="t('inline.viewsParseBatchParseBatchCenterView.text064')"
          :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text065')"
          :auto-format="false"
        />
      </div>
      <template #footer>
        <el-button @click="reportTemplateDialogVisible = false">{{ t('inline.viewsParseBatchParseBatchCenterView.text066') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="parseResultDialogVisible"
      :title="t('inline.viewsParseBatchParseBatchCenterView.text067')"
      width="980px"
      data-testid="batch-import-parse-detail"
    >
      <el-tabs v-model="activeParseResultTab" data-testid="batch-import-parse-result-tabs">
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text068')" name="overview">
          <div class="summary-grid">
            <article v-for="item in parseBatchStatusCards" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="t('rewriteTrial.title')" name="rewriteTrial">
          <section class="detail-card" data-testid="batch-import-rewrite-trial-tab">
            <p class="section-kicker sqlforge-code-label">rewrite trial</p>
            <div v-if="parseRewriteTrialRun" class="summary-grid">
              <article v-for="item in parseRewriteTrialCards" :key="item.label" class="summary-card">
                <span class="summary-card-label">{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </article>
            </div>
            <div v-if="parseRewriteTrialItems.length" class="report-list">
              <article
                v-for="item in parseRewriteTrialItems"
                :key="item.trialItemId"
                class="report-item"
                data-testid="batch-import-rewrite-trial-item"
              >
                <div class="session-item-top">
                  <strong>{{ item.trialStatus }}</strong>
                  <span class="status-pill">{{ displayValue(item.validationStatus || 'NOT_VALIDATED') }}</span>
                </div>
                <p>{{ t('rewriteTrial.sourceProblems') }}: {{ displayValue(sourceProblemLabels(item)) }}</p>
                <p>{{ t('rewriteTrial.issueRuleLinks') }}: {{ displayValue(issueRuleLinkLabels(item)) }}</p>
                <p>
                  taskId: {{ displayValue(item.taskId) }}
                  · recommendationId: {{ displayValue(item.recommendationId) }}
                  · rewriteRecordId: {{ displayValue(item.rewriteRecordId) }}
                </p>
                <SqlCodeBlock
                  v-if="item.candidateSql"
                  :value="item.candidateSql"
                  :label="t('rewriteTrial.candidateSql')"
                  :copy-label="t('common.actions.copy')"
                  compact
                  data-testid="batch-import-rewrite-trial-candidate-sql"
                />
                <p v-if="item.failureReason" class="diagnostic-line">{{ item.failureReason }}</p>
              </article>
            </div>
            <div v-else class="empty-state">
              {{ t('rewriteTrial.emptyBatch') }}
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text069')" name="sql">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text070') }}: {{ displayValue(item.parseTaskId) }}
                  · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text071') }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p v-if="rewriteTrialForParseItem(item)" data-testid="batch-import-parse-rewrite-trial-status">
                  {{ t('rewriteTrial.title') }}:
                  {{ displayValue(rewriteTrialForParseItem(item).trialStatus) }}
                  · {{ t('rewriteTrial.sourceProblems') }} {{ displayValue(sourceProblemLabels(rewriteTrialForParseItem(item))) }}
                  · {{ t('rewriteTrial.validationStatus') }} {{ displayValue(rewriteTrialForParseItem(item).validationStatus) }}
                </p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-parse-diagnostic"
                >
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text072') }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="item.sqlColumnName || item.itemId || 'SQL'"
                  :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text073')"
                  compact
                  data-testid="batch-import-parse-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-parse-item-detail-open" @click="openParseItemDetail(item)">
                    {{ t('inline.viewsParseBatchParseBatchCenterView.text074') }}
                  </el-button>
                </div>
              </article>
              <div v-if="!parseImportedRecords.length" class="empty-state">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text075') }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text076')" name="failures">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text077') }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <p v-if="issueSceneCodesForItem(item).length">
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text078') }}:
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
                  :label="t('inline.viewsParseBatchParseBatchCenterView.text079')"
                  :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text080')"
                  compact
                />
                <p v-else>{{ displayValue(item.message) }}</p>
                <button
                  type="button"
                  class="detail-link detail-link-button"
                  data-testid="batch-import-parse-failure-detail-open"
                  @click="openParseItemDetail(item)"
                >
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text081') }}
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
                {{ t('inline.viewsParseBatchParseBatchCenterView.text082') }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text083')" name="raw">
          <pre class="code-block">{{ formatJson(parseBatchDetail) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="parseItemDetailDialogVisible"
      :title="t('inline.viewsParseBatchParseBatchCenterView.text084')"
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
            :label="t('inline.viewsParseBatchParseBatchCenterView.text085')"
            :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text086')"
            data-testid="batch-import-selected-parse-sql"
          />
        </section>
        <section v-if="rewriteTrialForParseItem(selectedParseItem)" class="detail-card" data-testid="batch-import-selected-parse-rewrite-trial">
          <p class="section-kicker sqlforge-code-label">rewrite trial</p>
          <div class="summary-chip-row">
            <span class="summary-chip">
              {{ t('rewriteTrial.status') }}: <strong>{{ displayValue(rewriteTrialForParseItem(selectedParseItem).trialStatus) }}</strong>
            </span>
            <span class="summary-chip">
              {{ t('rewriteTrial.sourceProblems') }}: <strong>{{ displayValue(sourceProblemLabels(rewriteTrialForParseItem(selectedParseItem))) }}</strong>
            </span>
            <span class="summary-chip">
              {{ t('rewriteTrial.validationStatus') }}: <strong>{{ displayValue(rewriteTrialForParseItem(selectedParseItem).validationStatus) }}</strong>
            </span>
          </div>
          <p class="diagnostic-line">
            {{ t('rewriteTrial.issueRuleLinks') }}: {{ displayValue(issueRuleLinkLabels(rewriteTrialForParseItem(selectedParseItem))) }}
          </p>
          <SqlCodeBlock
            v-if="rewriteTrialForParseItem(selectedParseItem).candidateSql"
            :value="rewriteTrialForParseItem(selectedParseItem).candidateSql"
            :label="t('rewriteTrial.candidateSql')"
            :copy-label="t('common.actions.copy')"
            compact
          />
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="parseStatisticsDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text087')" width="920px">
      <el-tabs v-model="activeParseStatisticsTab" data-testid="batch-import-parse-statistics-tabs">
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text088')" name="issue">
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
                {{ t('inline.viewsParseBatchParseBatchCenterView.text089') }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text090')" name="report">
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
                {{ t('inline.viewsParseBatchParseBatchCenterView.text091') }}
              </div>
            </div>
          </section>
        </el-tab-pane>
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text092')" name="failures">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text093') }}: {{ buildDiagnosticSummary(item) }}
                </p>
                <p v-if="issueSceneCodesForItem(item).length">
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text094') }}:
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
                  :label="t('inline.viewsParseBatchParseBatchCenterView.text095')"
                  :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text096')"
                  compact
                />
                <p v-else>{{ displayValue(item.message) }}</p>
                <button
                  type="button"
                  class="detail-link detail-link-button"
                  data-testid="batch-import-parse-failure-detail-open"
                  @click="openParseItemDetail(item)"
                >
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text097') }}
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
                {{ t('inline.viewsParseBatchParseBatchCenterView.text098') }}
              </div>
            </div>
          </section>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="reportImportDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text099')" width="820px">
      <div class="dialog-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text100') }}</span>
          <el-input v-model="reportBatchForm.tenantId" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text101') }}</span>
          <el-input v-model="reportBatchForm.batchName" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text102') }}</span>
          <el-input v-model="reportBatchForm.reportCodeField" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text103') }}</span>
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
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text104') }}</span>
          <el-input v-model="reportBatchForm.priority" />
        </label>
        <label class="field-block field-block-wide">
          <span class="field-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text105') }}</span>
          <input type="file" @change="handleReportFileChange">
        </label>
        <div class="field-note field-block-wide">
          {{ t('inline.viewsParseBatchParseBatchCenterView.text106') }}
        </div>
        <div class="field-block field-block-wide">
          <SqlEditorField
            v-model="reportBatchForm.rawContent"
            :label="t('inline.viewsParseBatchParseBatchCenterView.text107')"
            :rows="8"
            :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text108')"
            :format-label="t('inline.viewsParseBatchParseBatchCenterView.text109')"
            :format-enabled="false"
            data-testid="batch-import-report-dialog-sql-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="reportImportDialogVisible = false">{{ t('inline.viewsParseBatchParseBatchCenterView.text110') }}</el-button>
        <el-button
          type="primary"
          :disabled="!reportBatchForm.datasourceCode"
          :loading="loading.importReportBatch"
          @click="importReportBatchFlow"
        >
          {{ t('inline.viewsParseBatchParseBatchCenterView.text111') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="reportResultDialogVisible"
      :title="t('inline.viewsParseBatchParseBatchCenterView.text112')"
      width="1040px"
      data-testid="batch-import-report-result-dialog"
    >
      <el-tabs v-model="activeReportResultTab" data-testid="batch-import-report-result-tabs">
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text113')" name="groups">
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
                · {{ t('inline.viewsParseBatchParseBatchCenterView.text114') }} {{ group.resolved }}
                · {{ t('inline.viewsParseBatchParseBatchCenterView.text115') }} {{ group.failed }}
                · Structure {{ formatPercent(group.structureRate) }}
                · Access {{ formatPercent(group.accessRate) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportGroupDetail(group)">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text116') }}
              </button>
            </article>
            <div v-if="!reportSqlDetailGroups.length" class="empty-state">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text117') }}
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text118')" name="sql">
          <section class="detail-card">
            <p class="section-kicker sqlforge-code-label">SQL-level parse detail</p>
            <div class="filter-row" data-testid="batch-import-report-sql-filter">
              <el-input
                v-model="reportSqlDetailSearchCode"
                :placeholder="t('inline.viewsParseBatchParseBatchCenterView.text119')"
                clearable
              />
              <el-button :loading="loading.reportSqlDetail" @click="applyWholeReportSqlFilter">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text120') }}
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text121') }}: {{ displayValue(item.parseTaskId) }}
                  · {{ t('inline.viewsParseBatchParseBatchCenterView.text122') }}: {{ displayValue(item.sqlOrdinalInReport) }}
                  · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text123') }}:
                  <span
                    v-if="issueSceneListHelp(issueSceneCodesForItem(item))"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneListHelp(issueSceneCodesForItem(item))"
                  >?</span>
                  {{ displayValue(issueSceneCodesForItem(item)) }}
                </p>
                <p>{{ t('inline.viewsParseBatchParseBatchCenterView.text124') }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                <p
                  v-if="hasIssueOrFailure(item)"
                  class="diagnostic-line"
                  data-testid="batch-import-report-diagnostic"
                >
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text125') }}: {{ issueLocationText(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="t('inline.viewsParseBatchParseBatchCenterView.text126')"
                  :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text127')"
                  compact
                  data-testid="batch-import-report-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-report-item-detail-open" @click="openReportItemDetail(item)">
                    {{ t('inline.viewsParseBatchParseBatchCenterView.text128') }}
                  </el-button>
                </div>
              </article>
              <div v-if="!reportSqlDetailItems.length" class="empty-state">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text129') }}
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

        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text130')" name="failures">
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
                {{ t('inline.viewsParseBatchParseBatchCenterView.text131') }}: {{ issueLocationText(item) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportItemDetail(item)">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text132') }}
              </button>
            </article>
            <div v-if="!reportSqlDetailFailureItems.length" class="empty-state">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text133') }}
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
        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text134')" name="overview">
          <div class="summary-grid" data-testid="batch-import-report-scoped-summary">
            <article class="summary-card">
              <span class="summary-card-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text135') }}</span>
              <strong>{{ selectedReportGroup.reportCode }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">SQL</span>
              <strong>{{ selectedReportGroup.total }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text136') }}</span>
              <strong>{{ selectedReportGroup.resolved }}</strong>
            </article>
            <article class="summary-card">
              <span class="summary-card-label">{{ t('inline.viewsParseBatchParseBatchCenterView.text137') }}</span>
              <strong>{{ selectedReportGroup.failed }}</strong>
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text138')" name="sql">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text139') }}: {{ displayValue(item.parseTaskId) }}
                  · {{ t('inline.viewsParseBatchParseBatchCenterView.text140') }}: {{ displayValue(item.sqlOrdinalInReport) }}
                  · {{ t('inline.viewsParseBatchParseBatchCenterView.text141') }}: {{ displayValue(item.status) }}
                </p>
                <p>
                  Structure: {{ displayValue(item.structureSyntaxStatus) }}
                  · Plan: {{ displayValue(item.planAnalysisStatus) }}
                  · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  · Analysis: {{ displayValue(item.analysisStatus) }}
                </p>
                <p>
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text142') }}:
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text143') }}: {{ issueLocationText(item) }}
                </p>
                <SqlCodeBlock
                  v-if="item.sqlText"
                  :value="item.sqlText"
                  :label="t('inline.viewsParseBatchParseBatchCenterView.text144')"
                  :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text145')"
                  compact
                  data-testid="batch-import-report-scoped-sql-code"
                />
                <div class="item-actions">
                  <el-button text data-testid="batch-import-report-item-detail-open" @click="openReportItemDetail(item)">
                    {{ t('inline.viewsParseBatchParseBatchCenterView.text146') }}
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

        <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text147')" name="failures">
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
                {{ t('inline.viewsParseBatchParseBatchCenterView.text148') }}: {{ issueLocationText(item) }}
              </p>
              <button type="button" class="detail-link detail-link-button" @click="openReportItemDetail(item)">
                {{ t('inline.viewsParseBatchParseBatchCenterView.text149') }}
              </button>
            </article>
            <div v-if="!selectedReportGroupFailureItems.length" class="empty-state">
              {{ t('inline.viewsParseBatchParseBatchCenterView.text150') }}
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog
      v-model="reportItemDetailDialogVisible"
      :title="t('inline.viewsParseBatchParseBatchCenterView.text151')"
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
            :label="t('inline.viewsParseBatchParseBatchCenterView.text152')"
            :copy-label="t('inline.viewsParseBatchParseBatchCenterView.text153')"
            data-testid="batch-import-selected-report-sql"
          />
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="reportStatisticsDialogVisible" :title="t('inline.viewsParseBatchParseBatchCenterView.text154')" width="960px">
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

            <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text155')" name="importance">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text156') }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text157')" name="report">
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

            <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text158')" name="sqlList">
              <div class="filter-row" data-testid="batch-import-report-statistics-sql-filter">
                <el-input
                  v-model="reportStatisticsSqlPagination.reportCode"
                  :placeholder="t('inline.viewsParseBatchParseBatchCenterView.text159')"
                  clearable
                />
                <el-button :loading="loading.reportStatistics" @click="applyReportStatisticsSqlFilter">
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text160') }}
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text161') }}
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

            <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text162')" name="priority">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text163') }}
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane :label="t('inline.viewsParseBatchParseBatchCenterView.text164')" name="logicalObject">
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
                  {{ t('inline.viewsParseBatchParseBatchCenterView.text165') }}
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (t('inline.viewsParseBatchParseBatchCenterView.text166'))" width="560px">
      <p class="result-copy">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ t('inline.viewsParseBatchParseBatchCenterView.text167') }}
        </el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="batchSelectorDrawerVisible"
      :title="batchSelectorKind === 'report' ? (t('inline.viewsParseBatchParseBatchCenterView.text168')) : (t('inline.viewsParseBatchParseBatchCenterView.text169'))"
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
