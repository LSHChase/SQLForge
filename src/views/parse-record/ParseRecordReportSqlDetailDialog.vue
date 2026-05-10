<script setup>
import { useI18n } from 'vue-i18n'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

const {
  displayDetailValue,
  displayValue,
  firstValue,
  issueSceneHelp,
  normalizeArray,
  openHistoryDetail,
  reportHistoryIdForItem,
  reportItemAccessHighlights,
  reportItemParseDetail,
  reportItemParseStatisticCards,
  reportItemParseStatus,
  reportItemParseSummaryCards,
  reportItemSqlOutput,
  reportItemStructureHighlights,
  reportItemStructureParse,
  reportSqlParseDetailDialogVisible,
  resultBannerClass,
  resultValueClass,
  riskDisplayText,
  selectedReportSqlDetailItem
} = useParseRecordContext()
</script>

<template>
  <el-dialog
    v-model="reportSqlParseDetailDialogVisible"
    :title="selectedReportSqlDetailItem?.reportCode || selectedReportSqlDetailItem?.itemId || (t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text001'))"
    width="1040px"
    data-testid="parse-record-report-sql-parse-detail-dialog"
  >
    <div v-if="selectedReportSqlDetailItem" class="dialog-stack">
      <div class="result-banner" :class="resultBannerClass(reportItemParseStatus(selectedReportSqlDetailItem))">
        <strong>{{ reportItemParseStatus(selectedReportSqlDetailItem) || '-' }}</strong>
        <span>{{ reportItemParseDetail(selectedReportSqlDetailItem)?.historyId || reportHistoryIdForItem(selectedReportSqlDetailItem) }}</span>
      </div>
      <div class="summary-chip-row">
        <span v-for="detailItem in reportItemParseSummaryCards(selectedReportSqlDetailItem)" :key="`dialog-${detailItem.label}`" class="summary-chip">
          {{ detailItem.label }}: <strong>{{ displayValue(detailItem.value) }}</strong>
        </span>
      </div>
      <SqlCodeBlock
        v-if="reportItemSqlOutput(selectedReportSqlDetailItem)"
        :value="reportItemSqlOutput(selectedReportSqlDetailItem)"
        :label="t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text002')"
        :copy-label="t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text003')"
        data-testid="parse-record-report-sql-code"
      />
      <div class="detail-grid detail-grid-secondary">
        <div v-for="detailItem in reportItemParseStatisticCards(selectedReportSqlDetailItem)" :key="`dialog-stat-${detailItem.label}`" class="detail-grid__item">
          <span>{{ detailItem.label }}</span>
          <strong>{{ displayValue(detailItem.value) }}</strong>
        </div>
      </div>
      <div class="parse-card-grid">
        <article v-if="reportItemStructureHighlights(selectedReportSqlDetailItem).length" class="parse-card">
          <div class="parse-card__header">
            <div>
              <p class="section-kicker sqlforge-code-label">structure parse</p>
              <h3 class="detail-title">{{ t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text004') }}</h3>
            </div>
          </div>
          <div class="highlight-grid">
            <div
              v-for="detailItem in reportItemStructureHighlights(selectedReportSqlDetailItem)"
              :key="`dialog-structure-${detailItem.key}`"
              class="highlight-chip"
              :class="resultValueClass(detailItem)"
            >
              <span>{{ detailItem.label }}</span>
              <strong>{{ displayValue(detailItem.value) }}</strong>
            </div>
          </div>
          <div v-if="normalizeArray(reportItemStructureParse(selectedReportSqlDetailItem).riskChecklist).length" class="issue-list issue-list-compact">
            <article
              v-for="(risk, riskIndex) in normalizeArray(reportItemStructureParse(selectedReportSqlDetailItem).riskChecklist)"
              :key="`dialog-risk-${risk.riskCode || riskIndex}`"
              class="issue-card"
              data-testid="parse-record-report-sql-risk"
            >
              <div class="issue-card__header">
                <strong>
                  {{ risk.riskCode || '-' }}
                  <span
                    v-if="issueSceneHelp(risk)"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneHelp(risk)"
                  >?</span>
                </strong>
                <span>{{ risk.severity || '-' }}</span>
              </div>
              <p class="issue-card__summary">{{ riskDisplayText(risk, 'summary') }}</p>
              <p class="issue-card__detail">{{ riskDisplayText(risk, 'evidence') }}</p>
              <p class="issue-card__detail">{{ t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text005') }}: {{ riskDisplayText(risk, 'suggestedAction') }}</p>
            </article>
          </div>
          <div v-if="normalizeArray(reportItemStructureParse(selectedReportSqlDetailItem).issues).length" class="issue-list">
            <article
              v-for="(issue, issueIndex) in normalizeArray(reportItemStructureParse(selectedReportSqlDetailItem).issues)"
              :key="`dialog-issue-${issue.issueCode || issueIndex}`"
              class="issue-card"
              data-testid="parse-record-report-sql-issue"
            >
              <div class="issue-card__header">
                <strong>
                  {{ issue.issueCode || '-' }}
                  <span
                    v-if="issueSceneHelp(issue)"
                    class="help-dot issue-scene-help"
                    tabindex="0"
                    aria-label="issue scene help"
                    :data-tooltip="issueSceneHelp(issue)"
                  >?</span>
                </strong>
                <span>{{ displayValue(firstValue(issue.severity, issue.priorityLevel)) }}</span>
              </div>
              <p class="issue-card__summary">{{ displayDetailValue(issue.summary) }}</p>
              <p class="issue-card__detail">{{ displayDetailValue(issue.detail) }}</p>
              <p v-if="issue.failureLine || issue.failureColumn || issue.failureToken || issue.failureSnippet" class="issue-card__detail">
                {{ t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text006') }}:
                <span v-if="issue.failureLine && issue.failureColumn">line {{ issue.failureLine }}, column {{ issue.failureColumn }}</span>
                <span v-if="issue.failureToken"> · token {{ issue.failureToken }}</span>
                <span v-if="issue.failureSnippet"> · {{ issue.failureSnippet }}</span>
              </p>
              <p class="issue-card__detail">{{ t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text007') }}: {{ displayDetailValue(issue.suggestedAction) }}</p>
            </article>
          </div>
        </article>
        <article v-if="reportItemAccessHighlights(selectedReportSqlDetailItem).length" class="parse-card">
          <div class="parse-card__header">
            <div>
              <p class="section-kicker sqlforge-code-label">access parse</p>
              <h3 class="detail-title">{{ t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text008') }}</h3>
            </div>
          </div>
          <div class="highlight-grid">
            <div
              v-for="detailItem in reportItemAccessHighlights(selectedReportSqlDetailItem)"
              :key="`dialog-access-${detailItem.key}`"
              class="highlight-chip"
              :class="resultValueClass(detailItem)"
            >
              <span>{{ detailItem.label }}</span>
              <strong>{{ displayValue(detailItem.value) }}</strong>
            </div>
          </div>
        </article>
      </div>
      <div v-if="reportHistoryIdForItem(selectedReportSqlDetailItem)" class="dialog-actions">
        <el-button text @click="openHistoryDetail(reportItemParseDetail(selectedReportSqlDetailItem).historyId || reportHistoryIdForItem(selectedReportSqlDetailItem))">
          {{ t('inline.viewsParseRecordParseRecordReportSqlDetailDialog.text009') }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped src="./parse-record.css"></style>
