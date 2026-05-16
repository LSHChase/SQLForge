<script setup>
import { useI18n } from 'vue-i18n'
import SqlCodeBlock from '../common/SqlCodeBlock.vue'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

const {
  activeDialogTab,
  auditEvents,
  detailDialogVisible,
  detailSummaryCards,
  displayDetailValue,
  displayValue,
  evidenceDrawerVisible,
  firstValue,
  formatJson,
  formatTimestamp,
  hasSqlHistoryLink,
  historyAccessHighlights,
  historyAccessParse,
  historyLogicalObjectHits,
  historyParseStatisticCards,
  historyParseStatus,
  historyParseSummaryCards,
  historyParseTaskId,
  historyResultSummary,
  historyStructureFeatureHighlights,
  historyStructureHighlights,
  historyStructureIntentLabels,
  historyStructureIssues,
  historyStructureParse,
  historyStructureResourceHighlights,
  historyStructureRiskChecklist,
  issueSceneHelp,
  loading,
  logicalObjectHits,
  normalizeArray,
  openAuditForensics,
  openExportDialog,
  openRepairEvidence,
  openRecommendationCenterFromDetail,
  openSqlHistoryFromDetail,
  primaryRecommendationRef,
  recommendationRefRows,
  referenceGroups,
  resultBannerClass,
  resultValueClass,
  riskDisplayText,
  selectedHistoryDetail,
  signalGroups,
  sqlStateHighlights,
  sqlVariants,
  statusClass,
  traceSummaryCards
} = useParseRecordContext()

const auditEventPagination = {
  pageSize: 100
}
</script>

<template>
  <el-dialog
    v-model="detailDialogVisible"
    :title="selectedHistoryDetail?.reportCode || selectedHistoryDetail?.parseHistoryId || selectedHistoryDetail?.historyId || 'parse history detail'"
    width="1120px"
  >
    <div v-if="selectedHistoryDetail" class="dialog-stack">
      <div class="dialog-header">
        <div class="banner-row">
          <strong data-testid="parse-record-detail-trace-id">{{ selectedHistoryDetail.traceId || '-' }}</strong>
          <span :class="statusClass(selectedHistoryDetail.resultStatus)">{{ selectedHistoryDetail.resultStatus || '-' }}</span>
        </div>
        <div class="dialog-actions">
          <el-button type="primary" @click="openRepairEvidence">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text001') }}</el-button>
          <el-button @click="openAuditForensics">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text002') }}</el-button>
          <el-button :disabled="!hasSqlHistoryLink" data-testid="parse-record-open-sql-history" @click="openSqlHistoryFromDetail">
            {{ t('parseRecord.actions.openSqlHistory') }}
          </el-button>
          <el-button
            :disabled="!primaryRecommendationRef"
            data-testid="parse-record-open-recommendation-result"
            @click="openRecommendationCenterFromDetail(primaryRecommendationRef)"
          >
            {{ t('parseRecord.actions.openRecommendationResult') }}
          </el-button>
          <el-button :loading="loading.export" data-testid="parse-record-export" @click="openExportDialog">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text003') }}</el-button>
          <el-button @click="evidenceDrawerVisible = true">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text004') }}</el-button>
        </div>
      </div>

      <el-tabs v-model="activeDialogTab">
        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text005')" name="overview">
          <div class="detail-grid">
            <div
              v-for="item in detailSummaryCards"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>
          <div v-if="traceSummaryCards.length" class="detail-grid detail-grid-secondary">
            <div
              v-for="item in traceSummaryCards"
              :key="item.label"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong>{{ displayValue(item.value) }}</strong>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text006')" name="parseResult">
          <div class="history-result-panel" data-testid="parse-record-history-parse-detail">
            <article class="code-card code-card-wide" data-testid="parse-record-history-original-sql">
              <div class="code-card__header">
                <span>{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text007') }}</span>
              </div>
              <SqlCodeBlock
                :value="selectedHistoryDetail.sqlText || '-'"
                :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text008')"
                :copy-label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text009')"
                :auto-format="false"
                data-testid="parse-record-history-original-sql-text"
              />
            </article>

            <div class="result-banner" :class="resultBannerClass(historyParseStatus)">
              <strong data-testid="parse-record-history-parse-status">{{ historyParseStatus || '-' }}</strong>
              <span>{{ historyParseTaskId || selectedHistoryDetail.historyId }}</span>
            </div>

            <div class="result-overview-card" data-testid="parse-record-history-parse-statistics">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">parse result</p>
                  <h3 class="detail-title">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text010') }}</h3>
                </div>
              </div>
              <div v-if="historyParseSummaryCards.length" class="summary-chip-row">
                <span v-for="item in historyParseSummaryCards" :key="item.label" class="summary-chip">
                  {{ item.label }}: <strong>{{ displayValue(item.value) }}</strong>
                </span>
              </div>
              <div v-if="historyParseStatisticCards.length" class="detail-grid detail-grid-secondary">
                <div v-for="item in historyParseStatisticCards" :key="item.label" class="detail-grid__item">
                  <span>{{ item.label }}</span>
                  <strong>{{ displayValue(item.value) }}</strong>
                </div>
              </div>
              <p v-if="historyResultSummary.summary" class="result-copy">{{ displayDetailValue(historyResultSummary.summary) }}</p>
              <p v-if="historyResultSummary.recommendedAction" class="result-copy result-copy-muted">
                {{ displayDetailValue(historyResultSummary.recommendedAction) }}
              </p>
            </div>

            <div
              v-if="!historyStructureHighlights.length && !historyAccessHighlights.length"
              class="empty-copy"
              data-testid="parse-record-history-parse-detail-empty"
            >
              {{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text011') }}
            </div>

            <div class="parse-card-grid">
              <article v-if="historyStructureHighlights.length" class="parse-card" data-testid="parse-record-history-structure-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">structure parse</p>
                    <h3 class="detail-title">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text012') }}</h3>
                  </div>
                  <span class="pill" :class="statusClass(historyStructureParse.syntaxStatus || historyParseStatus)">
                    {{ historyStructureParse.syntaxStatus || '-' }}
                  </span>
                </div>

                <div class="highlight-grid">
                  <div
                    v-for="item in historyStructureHighlights"
                    :key="item.key"
                    class="highlight-chip"
                    :class="resultValueClass(item)"
                  >
                    <span>{{ item.label }}</span>
                    <strong>{{ displayValue(item.value) }}</strong>
                  </div>
                </div>

                <div v-if="historyStructureIntentLabels.length" class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text013') }}</span>
                  <div class="pill-grid">
                    <span
                      v-for="item in historyStructureIntentLabels"
                      :key="`history-intent-${item}`"
                      class="summary-chip summary-chip-success"
                    >
                      {{ item }}
                    </span>
                  </div>
                </div>

                <div v-if="historyStructureFeatureHighlights.length" class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text014') }}</span>
                  <div class="highlight-grid highlight-grid-compact">
                    <div
                      v-for="item in historyStructureFeatureHighlights"
                      :key="item.key"
                      class="highlight-chip"
                      :class="resultValueClass(item)"
                    >
                      <span>{{ item.label }}</span>
                      <strong>{{ displayValue(item.value) }}</strong>
                    </div>
                  </div>
                </div>

                <div v-if="historyStructureResourceHighlights.length" class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text015') }}</span>
                  <div class="summary-chip-row">
                    <span
                      v-for="item in historyStructureResourceHighlights"
                      :key="`history-resource-${item.key}`"
                      class="summary-chip"
                    >
                      {{ item.label }}: <strong>{{ displayValue(item.value) }}</strong>
                    </span>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text016') }}</span>
                  <div class="summary-chip-row">
                    <span class="summary-chip">
                      {{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text017') }}:
                      <strong>{{ displayValue(historyStructureParse.queryDateSummary?.queryDateStart || selectedHistoryDetail.queryDateSummary?.queryDateStart) }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text018') }}:
                      <strong>{{ displayValue(historyStructureParse.queryDateSummary?.queryDateEnd || selectedHistoryDetail.queryDateSummary?.queryDateEnd) }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text019') }}:
                      <strong>{{ displayValue(historyStructureParse.queryDateSummary?.queryDateStatus || selectedHistoryDetail.queryDateSummary?.queryDateStatus) }}</strong>
                    </span>
                  </div>
                </div>

                <div v-if="historyLogicalObjectHits.length" class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text020') }}</span>
                  <div class="pill-grid">
                    <span
                      v-for="(item, index) in historyLogicalObjectHits"
                      :key="`${item.objectKey || item.logicalObjectKey || item.objectName || index}`"
                      class="summary-chip"
                    >
                      {{ item.objectType || item.logicalObjectType || 'OBJECT' }}:
                      <strong>{{ item.objectKey || item.logicalObjectKey || item.objectName || '-' }}</strong>
                    </span>
                  </div>
                </div>

                <div
                  v-if="normalizeArray(historyStructureParse.riskTags).length || normalizeArray(historyStructureParse.rewriteCandidates).length"
                  class="mini-section"
                >
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text021') }}</span>
                  <div class="pill-grid">
                    <span
                      v-for="item in normalizeArray(historyStructureParse.riskTags)"
                      :key="`history-risk-${item}`"
                      class="summary-chip summary-chip-warning"
                    >
                      {{ item }}
                      <span
                        v-if="issueSceneHelp(item)"
                        class="help-dot issue-scene-help"
                        tabindex="0"
                        aria-label="issue scene help"
                        :data-tooltip="issueSceneHelp(item)"
                      >?</span>
                    </span>
                    <span
                      v-for="item in normalizeArray(historyStructureParse.rewriteCandidates)"
                      :key="`history-candidate-${item}`"
                      class="summary-chip summary-chip-success"
                    >
                      {{ item }}
                    </span>
                  </div>
                </div>

                <div v-if="historyStructureRiskChecklist.length" class="issue-list issue-list-compact">
                  <article
                    v-for="(risk, index) in historyStructureRiskChecklist"
                    :key="`${risk.riskCode || 'risk'}-${index}`"
                    class="issue-card"
                    data-testid="parse-record-history-risk"
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
                    <p class="issue-card__detail">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text022') }}: {{ riskDisplayText(risk, 'suggestedAction') }}</p>
                  </article>
                </div>

                <div v-if="historyStructureIssues.length" class="issue-list">
                  <article
                    v-for="(issue, index) in historyStructureIssues"
                    :key="`${issue.issueCode || 'issue'}-${index}`"
                    class="issue-card"
                    data-testid="parse-record-history-issue"
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
                      {{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text023') }}:
                      <span v-if="issue.failureLine && issue.failureColumn">line {{ issue.failureLine }}, column {{ issue.failureColumn }}</span>
                      <span v-if="issue.failureToken"> · token {{ issue.failureToken }}</span>
                      <span v-if="issue.failureSnippet"> · {{ issue.failureSnippet }}</span>
                    </p>
                    <p class="issue-card__detail">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text024') }}: {{ displayDetailValue(issue.suggestedAction) }}</p>
                  </article>
                </div>
              </article>

              <article v-if="historyAccessHighlights.length" class="parse-card" data-testid="parse-record-history-access-card">
                <div class="parse-card__header">
                  <div>
                    <p class="section-kicker sqlforge-code-label">access parse</p>
                    <h3 class="detail-title">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text025') }}</h3>
                  </div>
                  <span
                    class="pill"
                    :class="statusClass(historyAccessParse.serviceStatus === 'AVAILABLE' && historyAccessParse.connectionStatus === 'CONNECTED' ? 'SUCCESS' : historyAccessParse.serviceStatus)"
                  >
                    {{ historyAccessParse.serviceStatus || '-' }}
                  </span>
                </div>

                <div class="highlight-grid">
                  <div
                    v-for="item in historyAccessHighlights"
                    :key="item.key"
                    class="highlight-chip"
                    :class="resultValueClass(item)"
                  >
                    <span>{{ item.label }}</span>
                    <strong>{{ displayValue(item.value) }}</strong>
                  </div>
                </div>

                <div v-if="historyAccessParse.planSummary" class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text026') }}</span>
                  <p class="result-copy">{{ displayDetailValue(historyAccessParse.planSummary) }}</p>
                </div>

                <div v-if="historyAccessParse.availabilityWarning" class="mini-section">
                  <span class="summary-card-label">{{ t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text027') }}</span>
                  <p class="result-copy result-copy-muted">{{ displayDetailValue(historyAccessParse.availabilityWarning) }}</p>
                </div>
              </article>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text028')" name="sql">
          <div class="detail-grid">
            <div
              v-for="item in sqlStateHighlights"
              :key="item.key"
              class="detail-grid__item"
            >
              <span>{{ item.label }}</span>
              <strong :data-testid="`parse-record-history-${item.key.replace(/[A-Z]/g, match => `-${match.toLowerCase()}`)}`">
                {{ displayValue(item.value) }}
              </strong>
            </div>
          </div>
          <div class="code-grid">
            <article
              v-for="item in sqlVariants"
              :key="item.key"
              class="code-card"
            >
              <div class="code-card__header">
                <span>{{ item.label }}</span>
              </div>
              <SqlCodeBlock
                :value="item.value"
                :label="item.label"
                :copy-label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text029')"
                :auto-format="item.autoFormat !== false"
                :data-testid="`parse-record-${item.key}`"
              />
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text030')" name="signals">
          <div class="code-grid">
            <article
              v-for="group in signalGroups"
              :key="group.key"
              class="code-card"
            >
              <div class="code-card__header">
                <span>{{ group.title }}</span>
              </div>
              <pre class="code-block" :data-testid="`parse-record-${group.key}`">{{ formatJson(group.payload) }}</pre>
            </article>
          </div>
        </el-tab-pane>

        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text031')" name="refs">
          <div v-if="recommendationRefRows.length" class="reference-action-list" data-testid="parse-record-recommendation-ref-actions">
            <button
              v-for="item in recommendationRefRows"
              :key="item.recommendationId || item.id"
              type="button"
              class="reference-action"
              @click="openRecommendationCenterFromDetail(item)"
            >
              <span>{{ t('parseRecord.actions.openRecommendationResult') }}</span>
              <strong>{{ item.recommendationId || item.id }}</strong>
            </button>
          </div>

          <div v-if="logicalObjectHits.length" class="detail-grid">
            <div
              v-for="(item, index) in logicalObjectHits"
              :key="`${item.objectKey || item.logicalObjectKey || index}`"
              class="detail-grid__item"
            >
              <span>{{ item.objectType || item.logicalObjectType || 'OBJECT' }}</span>
              <strong>{{ item.objectKey || item.logicalObjectKey || item.objectName || '-' }}</strong>
            </div>
          </div>

          <div v-if="referenceGroups.length" class="code-grid">
            <article
              v-for="group in referenceGroups"
              :key="group.key"
              class="code-card"
            >
              <div class="code-card__header">
                <span>{{ group.title }}</span>
              </div>
              <pre class="code-block">{{ formatJson(group.items) }}</pre>
            </article>
          </div>

          <el-table :data="auditEvents" :max-height="auditEventPagination.pageSize * 4" border>
            <el-table-column prop="serviceCode" :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text032')" min-width="150" />
            <el-table-column prop="operationType" :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text033')" min-width="160" />
            <el-table-column prop="status" :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text034')" min-width="120" />
            <el-table-column :label="t('inline.viewsParseRecordParseRecordHistoryDetailDialog.text035')" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.createTime) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<style scoped src="./parse-record.css"></style>
