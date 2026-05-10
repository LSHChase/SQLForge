<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

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
  reportBatchIssueSceneDetailDialogTitle,
  reportBatchIssueSceneDetailDialogVisible,
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
              <el-table
                :data="reportBatchIssueStatistics"
                border
                data-testid="parse-record-report-statistics-issue-scene"
              >
                <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.issueScene') }" min-width="220">
                  <template #default="{ row }">
                    <span class="issue-scene-line">
                      <span class="issue-scene-code issue-scene-code-button">{{ row.issueScene }}</span>
                      <span
                        v-if="issueSceneHelp(row.issueScene)"
                        class="help-dot issue-scene-help"
                        tabindex="0"
                        aria-label="issue scene help"
                        :data-tooltip="issueSceneHelp(row.issueScene)"
                      >?</span>
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="affectedSqlCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.affectedSql') }" min-width="110" />
                <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.severity') }" min-width="110">
                  <template #default="{ row }">{{ displayValue(row.severity) }}</template>
                </el-table-column>
                <el-table-column prop="reportCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.reportCount') }" min-width="110" />
                <el-table-column prop="logicalObjectCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.logicalObjectCount') }" min-width="130" />
                <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.ratio') }" min-width="100">
                  <template #default="{ row }">{{ formatPercent(row.ratio) }}</template>
                </el-table-column>
                <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.actions') }" min-width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button text :loading="loading.reportBatchIssueSceneDetail" @click="openReportBatchIssueSceneDetail(row)">
                      {{ t('parseRecord.issueSceneDetail.actions.viewDetail') }}
                    </el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <p class="empty-copy">
                    {{ isChinese ? '当前没有问题场景统计。' : 'No issue statistics in this report batch.' }}
                  </p>
                </template>
              </el-table>
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

  <el-dialog
    v-model="reportBatchIssueSceneDetailDialogVisible"
    v-bind="{ title: reportBatchIssueSceneDetailDialogTitle }"
    width="1120px"
    data-testid="parse-record-report-issue-scene-detail-dialog"
  >
    <div
      v-if="selectedReportIssueSceneDetail"
      class="dialog-stack"
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

      <section class="issue-scene-detail-section">
        <div class="detail-section-heading">
          <span>{{ t('parseRecord.issueSceneDetail.sections.reportDetail') }}</span>
        </div>
        <el-table
          :data="normalizeArray(selectedReportIssueSceneDetail.reportDetails)"
          border
          data-testid="parse-record-report-issue-scene-report"
        >
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.report') }" min-width="220">
            <template #default="{ row }">
              <div class="table-cell-stack">
                <strong>{{ displayValue(row.reportCode) }}</strong>
                <span class="cell-subline">{{ displayValue(row.reportName) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="sqlCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.sqlCount') }" min-width="100" />
          <el-table-column prop="issueCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.issueCount') }" min-width="100" />
          <el-table-column prop="logicalObjectCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.logicalObjectCount') }" min-width="120" />
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.logicalObjectKeys') }" min-width="260">
            <template #default="{ row }">{{ displayValue(row.logicalObjectKeys) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section class="issue-scene-detail-section">
        <div class="detail-section-heading">
          <span>{{ t('parseRecord.issueSceneDetail.sections.logicalObjectDetail') }}</span>
        </div>
        <el-table
          :data="normalizeArray(selectedReportIssueSceneDetail.logicalObjectDetails)"
          border
          data-testid="parse-record-report-issue-scene-object"
        >
          <el-table-column prop="objectKey" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.object') }" min-width="240" />
          <el-table-column prop="sqlCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.sqlCount') }" min-width="100" />
          <el-table-column prop="issueCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.issueCount') }" min-width="100" />
          <el-table-column prop="reportCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.reportCount') }" min-width="100" />
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.reportCodes') }" min-width="260">
            <template #default="{ row }">{{ displayValue(row.reportCodes) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <section class="issue-scene-detail-section">
        <div class="detail-section-heading">
          <span>{{ t('parseRecord.issueSceneDetail.sections.sqlDetail') }}</span>
        </div>
        <el-table
          :data="normalizeArray(selectedReportIssueSceneDetail.sqlStatistics)"
          border
          data-testid="parse-record-report-issue-scene-sql"
        >
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.reportSql') }" min-width="240">
            <template #default="{ row }">
              <div class="table-cell-stack">
                <strong>{{ displayValue(row.reportCode) }}</strong>
                <span class="cell-subline">{{ displayValue(row.sqlColumnName || row.itemId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.priority') }" min-width="110">
            <template #default="{ row }">{{ displayValue(row.highestPriorityLevel) }}</template>
          </el-table-column>
          <el-table-column prop="issueCount" v-bind="{ label: t('parseRecord.issueSceneDetail.columns.issueCount') }" min-width="100" />
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.logicalObjects') }" min-width="220">
            <template #default="{ row }">{{ displayValue(row.logicalObjectKeys) }}</template>
          </el-table-column>
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.issueScenes') }" min-width="240">
            <template #default="{ row }">
              <span
                v-if="issueSceneListHelp(row.issueScenes)"
                class="help-dot issue-scene-help"
                tabindex="0"
                aria-label="issue scene help"
                :data-tooltip="issueSceneListHelp(row.issueScenes)"
              >?</span>
              {{ displayValue(row.issueScenes) }}
            </template>
          </el-table-column>
          <el-table-column v-bind="{ label: t('parseRecord.issueSceneDetail.columns.location') }" min-width="220">
            <template #default="{ row }">{{ issueLocationText(row) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="Number(selectedReportIssueSceneDetail.sqlStatisticTotalCount || 0) > reportBatchIssueScenePagination.pageSize"
          class="pagination-row"
          layout="total, prev, pager, next"
          :total="Number(selectedReportIssueSceneDetail.sqlStatisticTotalCount || 0)"
          :page-size="reportBatchIssueScenePagination.pageSize"
          :current-page="reportBatchIssueScenePagination.pageNumber"
          @current-change="handleReportBatchIssueScenePageChange"
        />
      </section>
    </div>
  </el-dialog>
</template>

<style scoped src="./parse-record.css"></style>
