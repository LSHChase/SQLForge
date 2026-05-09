<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

useI18n()

const {
  activeReportBatchDetailTab,
  activeReportBatchStatisticsTab,
  applyReportBatchIssueSceneFilter,
  applyReportBatchSqlFilter,
  displayValue,
  formatPercent,
  handleReportBatchIssueScenePageChange,
  handleReportBatchSqlPageChange,
  handleReportBatchSqlPageSizeChange,
  isChinese,
  issueLocationText,
  issueSceneCodesForItem,
  issueSceneHelp,
  issueSceneListHelp,
  loading,
  loadReportBatchItemDetail,
  normalizeArray,
  openReportBatchIssueSceneDetail,
  openReportSqlParseDetail,
  REPORT_SQL_PAGE_SIZE_OPTIONS,
  reportBatchDetailCards,
  reportBatchDetailDrawerVisible,
  reportBatchIssueSceneDetailCards,
  reportBatchIssueScenePagination,
  reportBatchIssueStatistics,
  reportBatchItemDetailErrorMessage,
  reportBatchLogicalObjectStatistics,
  reportBatchParseDetailSummary,
  reportBatchSqlPagination,
  reportHistoryIdForItem,
  reportItemParseDetail,
  selectedReportBackendReportStatistics,
  selectedReportBackendSqlStatistics,
  selectedReportBatchDetail,
  selectedReportGroups,
  selectedReportImportanceStatistics,
  selectedReportIssueSceneDetail,
  selectedReportItems,
  selectedReportPriorityMatrix
} = useParseRecordContext()
</script>

<template>
  <el-drawer
    v-model="reportBatchDetailDrawerVisible"
    :title="selectedReportBatchDetail?.batchName || selectedReportBatchDetail?.batchId || (isChinese ? '报表导入详情' : 'Report import detail')"
    size="64%"
    data-testid="parse-record-report-batch-detail"
  >
    <el-tabs v-model="activeReportBatchDetailTab" data-testid="parse-record-report-detail-tabs">
      <el-tab-pane :label="isChinese ? '概览' : 'Overview'" name="overview">
        <div class="summary-grid">
          <article v-for="item in reportBatchDetailCards" :key="item.label" class="summary-card">
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? '报表级统计' : 'Report-level statistics'" name="statistics">
        <section class="code-card">
          <div class="code-card__header">
            <span>{{ isChinese ? '报表级解析统计' : 'Report-level parse statistics' }}</span>
          </div>
          <el-tabs
            v-model="activeReportBatchStatisticsTab"
            class="statistics-tabs"
            data-testid="parse-record-report-statistics-tabs"
          >
            <el-tab-pane :label="isChinese ? '问题场景' : 'Issue scenes'" name="issueScene">
              <div class="detail-grid">
                <article
                  v-for="item in reportBatchIssueStatistics"
                  :key="item.issueScene"
                  class="detail-grid__item issue-scene-detail-button"
                  data-testid="parse-record-report-statistics-issue-scene"
                >
                  <span class="issue-scene-line">
                    <button type="button" class="table-link issue-scene-code-button" @click="openReportBatchIssueSceneDetail(item)">
                      {{ item.issueScene }}
                    </button>
                    <span
                      v-if="issueSceneHelp(item.issueScene)"
                      class="help-dot issue-scene-help"
                      tabindex="0"
                      aria-label="issue scene help"
                      :data-tooltip="issueSceneHelp(item.issueScene)"
                    >?</span>
                  </span>
                  <strong>
                    {{ item.affectedSqlCount }} SQL · {{ displayValue(item.severity) }} · {{ formatPercent(item.ratio) }}
                    <span v-if="item.reportCount"> · {{ item.reportCount }} {{ isChinese ? '报表' : 'reports' }}</span>
                    <span v-if="item.logicalObjectCount"> · {{ item.logicalObjectCount }} {{ isChinese ? '对象' : 'objects' }}</span>
                  </strong>
                </article>
                <p v-if="!reportBatchIssueStatistics.length" class="empty-copy">
                  {{ isChinese ? '当前没有问题场景统计。' : 'No issue statistics in this report batch.' }}
                </p>
              </div>
              <div
                v-if="selectedReportIssueSceneDetail"
                class="issue-scene-detail-panel"
                data-testid="parse-record-report-issue-scene-detail"
              >
                <div class="summary-chip-row">
                  <span v-for="item in reportBatchIssueSceneDetailCards" :key="item.label" class="summary-chip">
                    {{ item.label }}:
                    <strong>{{ displayValue(item.value) }}</strong>
                    <span
                      v-if="item.key === 'issueScene' && issueSceneHelp(item.value)"
                      class="help-dot issue-scene-help"
                      tabindex="0"
                      aria-label="issue scene help"
                      :data-tooltip="issueSceneHelp(item.value)"
                    >?</span>
                  </span>
                  <span v-if="loading.reportBatchIssueSceneDetail" class="summary-chip summary-chip-warning">
                    {{ isChinese ? '正在加载场景详情' : 'Loading scene detail' }}
                  </span>
                </div>
                <div class="filter-row">
                  <el-input
                    v-model="reportBatchIssueScenePagination.reportCode"
                    :placeholder="isChinese ? '按报表编码筛选' : 'Filter by report code'"
                    clearable
                  />
                  <el-input
                    v-model="reportBatchIssueScenePagination.logicalObjectKey"
                    :placeholder="isChinese ? '按逻辑对象筛选' : 'Filter by logical object'"
                    clearable
                  />
                  <el-button :loading="loading.reportBatchIssueSceneDetail" @click="applyReportBatchIssueSceneFilter">
                    {{ isChinese ? '查询详情' : 'Search detail' }}
                  </el-button>
                </div>
                <div class="detail-grid detail-grid-secondary">
                  <div
                    v-for="item in normalizeArray(selectedReportIssueSceneDetail.reportDetails).slice(0, 8)"
                    :key="item.reportCode"
                    class="detail-grid__item"
                    data-testid="parse-record-report-issue-scene-report"
                  >
                    <span>{{ item.reportCode }}</span>
                    <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues</strong>
                    <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                  </div>
                </div>
                <div class="detail-grid detail-grid-secondary">
                  <div
                    v-for="item in normalizeArray(selectedReportIssueSceneDetail.logicalObjectDetails).slice(0, 8)"
                    :key="item.objectKey"
                    class="detail-grid__item"
                    data-testid="parse-record-report-issue-scene-object"
                  >
                    <span>{{ item.objectKey }}</span>
                    <strong>{{ item.sqlCount }} SQL · {{ item.reportCount }} reports</strong>
                    <p>{{ displayValue(item.reportCodes) }}</p>
                  </div>
                </div>
                <div class="report-sql-list">
                  <article
                    v-for="item in normalizeArray(selectedReportIssueSceneDetail.sqlStatistics)"
                    :key="item.itemId"
                    class="detail-grid__item report-sql-card"
                    data-testid="parse-record-report-issue-scene-sql"
                  >
                    <span>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId }}</span>
                    <strong>{{ item.highestPriorityLevel }} · {{ item.issueCount }} issues</strong>
                    <p>{{ isChinese ? '逻辑对象' : 'Logical objects' }}: {{ displayValue(item.logicalObjectKeys) }}</p>
                    <p>
                      {{ isChinese ? '问题场景' : 'Issue scenes' }}:
                      <span
                        v-if="issueSceneListHelp(item.issueScenes)"
                        class="help-dot issue-scene-help"
                        tabindex="0"
                        aria-label="issue scene help"
                        :data-tooltip="issueSceneListHelp(item.issueScenes)"
                      >?</span>
                      {{ displayValue(item.issueScenes) }}
                    </p>
                  </article>
                </div>
                <el-pagination
                  v-if="Number(selectedReportIssueSceneDetail.sqlStatisticTotalCount || 0) > reportBatchIssueScenePagination.pageSize"
                  class="pagination-row"
                  layout="total, prev, pager, next"
                  :total="Number(selectedReportIssueSceneDetail.sqlStatisticTotalCount || 0)"
                  :page-size="reportBatchIssueScenePagination.pageSize"
                  :current-page="reportBatchIssueScenePagination.pageNumber"
                  @current-change="handleReportBatchIssueScenePageChange"
                />
              </div>
              <p v-else class="empty-copy">
                {{ isChinese ? '在“报表级统计”中点击问题场景后查看详情。' : 'Select an issue scene in Report-level statistics to inspect details.' }}
              </p>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '重要程度' : 'Importance'" name="importance">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportImportanceStatistics"
                  :key="item.importanceBucket"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-importance"
                >
                  <span>{{ item.importanceBucket }}</span>
                  <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</strong>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '报表视角' : 'Report view'" name="report">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportBackendReportStatistics"
                  :key="item.reportCode"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-report-view"
                >
                  <span>{{ item.reportCode }}</span>
                  <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ formatPercent(item.issueSqlRatio) }}</strong>
                  <p v-if="item.mergeCandidate">
                    {{
                      isChinese
                        ? `建议合并复核：${displayValue(item.mergeCandidateSqlCount)} 条 SQL。${displayValue(item.mergeCandidateReason)}`
                        : `Merge review: ${displayValue(item.mergeCandidateSqlCount)} SQL. ${displayValue(item.mergeCandidateReason)}`
                    }}
                  </p>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sqlList">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportBackendSqlStatistics.slice(0, 8)"
                  :key="item.itemId"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-sql-list"
                >
                  <span>{{ item.reportCode }} · {{ item.sqlColumnName || item.itemId }}</span>
                  <strong>{{ item.highestPriorityLevel }} · {{ item.issueCount }} issues</strong>
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
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '优先级视角' : 'Priority view'" name="priority">
              <div class="detail-grid">
                <div
                  v-for="item in selectedReportPriorityMatrix"
                  :key="`${item.priorityLevel}-${item.urgencyBucket}`"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-priority"
                >
                  <span>{{ item.priorityLevel }} · {{ item.urgencyBucket }}</span>
                  <strong>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues · {{ item.reportCount }} reports</strong>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane :label="isChinese ? '逻辑对象视角' : 'Logical objects'" name="logicalObject">
              <div class="detail-grid">
                <div
                  v-for="item in reportBatchLogicalObjectStatistics"
                  :key="item.objectKey"
                  class="detail-grid__item"
                  data-testid="parse-record-report-statistics-logical-object"
                >
                  <span>{{ item.objectKey }}</span>
                  <strong>{{ item.hitCount }} SQL · {{ displayValue(item.reportCodes) }}</strong>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </section>
      </el-tab-pane>

      <el-tab-pane :label="isChinese ? 'SQL 清单' : 'SQL list'" name="sqlList">
        <section class="code-card">
          <div class="code-card__header">
            <span>{{ isChinese ? 'SQL 级解析详情' : 'SQL-level parse detail' }}</span>
          </div>
          <div class="filter-row" data-testid="parse-record-report-sql-filter">
            <el-input
              v-model="reportBatchSqlPagination.reportCode"
              :placeholder="isChinese ? '按报表编码筛选' : 'Filter by report code'"
              clearable
            />
            <el-button :loading="loading.reportBatchDetail" @click="applyReportBatchSqlFilter">
              {{ isChinese ? '查询' : 'Search' }}
            </el-button>
          </div>
          <div class="summary-chip-row" data-testid="parse-record-report-sql-detail-statistics">
            <span v-for="item in reportBatchParseDetailSummary" :key="item.label" class="summary-chip">
              {{ item.label }}: <strong>{{ item.value }}</strong>
            </span>
            <span v-if="loading.reportBatchItemDetails" class="summary-chip summary-chip-warning">
              {{ isChinese ? '正在加载每条 SQL 的解析详情' : 'Loading per-SQL parse details' }}
            </span>
          </div>
          <p v-if="reportBatchItemDetailErrorMessage" class="empty-copy" data-testid="parse-record-report-sql-detail-load-warning">
            {{ reportBatchItemDetailErrorMessage }}
          </p>
          <div class="report-sql-list">
            <section
              v-for="group in selectedReportGroups"
              :key="group.reportCode"
              class="report-sql-group"
              data-testid="parse-record-report-group"
            >
              <div class="session-item-top">
                <strong>{{ group.reportCode }}</strong>
                <span class="summary-chip">{{ group.total }} SQL</span>
              </div>
              <p>
                {{ displayValue(group.reportName) }}
                · {{ isChinese ? '已解析' : 'Resolved' }} {{ group.resolved }}
                · {{ isChinese ? '失败' : 'Failed' }} {{ group.failed }}
                · Structure {{ formatPercent(group.structureRate) }}
                · Access {{ formatPercent(group.accessRate) }}
                · {{ isChinese ? '问题场景' : 'Issue scenes' }} {{ group.issueSceneCount }}
                · {{ isChinese ? '逻辑对象' : 'Logical objects' }} {{ group.logicalObjectCount }}
              </p>
              <div class="report-sql-list">
                <article
                  v-for="(item, index) in group.items"
                  :key="item.itemId || `${group.reportCode}-${index}`"
                  class="detail-grid__item report-sql-card"
                  data-testid="parse-record-report-sql-detail"
                >
                  <span>{{ item.sqlColumnName || item.itemId || `SQL ${index + 1}` }} · {{ displayValue(item.status) }}</span>
                  <strong>{{ displayValue(item.reportName || group.reportCode) }}</strong>
                  <p>
                    {{ isChinese ? '任务' : 'Task' }}: {{ displayValue(item.parseTaskId) }}
                    · Structure: {{ displayValue(item.structureSyntaxStatus) }}
                    · Access: {{ displayValue(item.accessServiceStatus) }}/{{ displayValue(item.accessConnectionStatus) }}
                  </p>
                  <p data-testid="parse-record-report-sql-history-link">
                    {{ isChinese ? '解析历史' : 'Parse history' }}:
                    {{ displayValue(reportHistoryIdForItem(item)) }}
                    · {{ displayValue(item.historyPersistenceStatus) }}
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
                  <p class="empty-copy">{{ isChinese ? '定位' : 'Location' }}: {{ issueLocationText(item) }}</p>
                  <div v-if="reportHistoryIdForItem(item) && !reportItemParseDetail(item)" class="dialog-actions">
                    <el-button text :loading="loading.reportBatchItemDetails" @click="loadReportBatchItemDetail(item)">
                      {{ isChinese ? '加载解析详情' : 'Load parse detail' }}
                    </el-button>
                  </div>
                  <div v-else-if="reportItemParseDetail(item)" class="dialog-actions">
                    <el-button text data-testid="parse-record-report-sql-parse-detail" @click="openReportSqlParseDetail(item)">
                      {{ isChinese ? '查看解析详情弹窗' : 'Open parse detail dialog' }}
                    </el-button>
                  </div>
                  <p v-else-if="reportHistoryIdForItem(item) && !loading.reportBatchItemDetails" class="empty-copy" data-testid="parse-record-report-sql-detail-missing">
                    {{ isChinese ? '点击加载解析详情后展示结构化解析统计。' : 'Load parse detail to show structured parse statistics.' }}
                  </p>
                  <p v-else class="empty-copy" data-testid="parse-record-report-sql-history-detail-unavailable">
                    {{ isChinese ? '解析历史暂不可用；当前仅展示报表批次内的 SQL 解析证据。' : 'Parse history is unavailable; this card shows report-batch SQL evidence only.' }}
                  </p>
                </article>
              </div>
            </section>
            <p v-if="!selectedReportItems.length" class="empty-copy">
              {{ isChinese ? '导入解析后会展示 SQL 明细。' : 'SQL detail appears after report SQL resolution.' }}
            </p>
            <el-pagination
              v-if="Number(selectedReportBatchDetail?.itemTotalCount || 0) > reportBatchSqlPagination.pageSize"
              class="pagination-row"
              layout="total, sizes, prev, pager, next"
              :total="Number(selectedReportBatchDetail?.itemTotalCount || 0)"
              :page-sizes="REPORT_SQL_PAGE_SIZE_OPTIONS"
              :page-size="reportBatchSqlPagination.pageSize"
              :current-page="reportBatchSqlPagination.pageNumber"
              @current-change="handleReportBatchSqlPageChange"
              @size-change="handleReportBatchSqlPageSizeChange"
            />
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style scoped src="./parse-record.css"></style>
