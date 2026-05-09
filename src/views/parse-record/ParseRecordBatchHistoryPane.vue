<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

useI18n()

const {
  batchHistoryErrorMessage,
  batchHistoryTab,
  formatTimestamp,
  handleParseBatchHistoryPageChange,
  handleParseBatchHistoryPageSizeChange,
  handleReportBatchHistoryPageChange,
  handleReportBatchHistoryPageSizeChange,
  isChinese,
  LIST_PAGE_SIZE_OPTIONS,
  loading,
  openParseBatchCenter,
  openReportBatchCenter,
  openReportBatchDetail,
  parseBatchHistoryPagination,
  parseBatchHistoryRows,
  parseBatchHistorySummary,
  reportBatchHistoryPagination,
  reportBatchHistoryRows,
  reportBatchHistorySummary
} = useParseRecordContext()
</script>

<template>
  <el-tab-pane :label="isChinese ? '批量解析与报表导入历史' : 'Batch parse and report-import history'" name="batchHistory">
    <section class="surface-card batch-history-panel" data-testid="parse-record-batch-report-history-tab">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">batch history</p>
          <h2 class="section-title">{{ isChinese ? '批量解析与报表导入历史' : 'Batch parse and report-import history' }}</h2>
        </div>
        <div class="chip-row">
          <span class="chip">{{ isChinese ? '服务端持久化' : 'Server persisted' }}</span>
          <span class="chip">{{ isChinese ? '可回跳批量中心' : 'Deep links to batch center' }}</span>
        </div>
      </div>

      <div
        v-if="batchHistoryErrorMessage"
        class="inline-banner inline-banner-danger"
        data-testid="parse-record-batch-history-error"
      >
        {{ batchHistoryErrorMessage }}
      </div>

      <el-tabs v-model="batchHistoryTab">
        <el-tab-pane :label="isChinese ? '批量解析历史' : 'Batch parse history'" name="parse">
          <section class="summary-grid batch-history-summary">
            <article v-for="item in parseBatchHistorySummary" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </section>
          <el-table :data="parseBatchHistoryRows" border>
            <el-table-column prop="batchName" :label="isChinese ? '批次名称' : 'Batch name'" min-width="200">
              <template #default="{ row }">
                <button
                  type="button"
                  class="table-link"
                  data-testid="parse-record-batch-history-parse"
                  @click="openParseBatchCenter(row.batchId)"
                >
                  {{ row.batchName || row.batchId }}
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
            <el-table-column prop="importMode" :label="isChinese ? '导入模式' : 'Import mode'" min-width="140" />
            <el-table-column prop="fileType" :label="isChinese ? '文件类型' : 'File type'" min-width="120" />
            <el-table-column prop="totalRecords" :label="isChinese ? '总记录' : 'Total records'" min-width="110" />
            <el-table-column prop="successRecords" :label="isChinese ? '成功' : 'Success'" min-width="100" />
            <el-table-column prop="failedRecords" :label="isChinese ? '失败' : 'Failed'" min-width="100" />
            <el-table-column prop="createdAt" :label="isChinese ? '创建时间' : 'Created at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pagination-row"
            layout="total, sizes, prev, pager, next"
            :total="parseBatchHistoryPagination.totalCount"
            :page-sizes="LIST_PAGE_SIZE_OPTIONS"
            :page-size="parseBatchHistoryPagination.pageSize"
            :current-page="parseBatchHistoryPagination.pageNo"
            @current-change="handleParseBatchHistoryPageChange"
            @size-change="handleParseBatchHistoryPageSizeChange"
          />
        </el-tab-pane>

        <el-tab-pane :label="isChinese ? '报表导入历史' : 'Report import history'" name="report">
          <section class="summary-grid batch-history-summary">
            <article v-for="item in reportBatchHistorySummary" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </section>
          <el-table :data="reportBatchHistoryRows" border>
            <el-table-column prop="batchName" :label="isChinese ? '批次名称' : 'Batch name'" min-width="200">
              <template #default="{ row }">
                <button
                  type="button"
                  class="table-link"
                  data-testid="parse-record-batch-history-report"
                  @click="openReportBatchDetail(row)"
                >
                  {{ row.batchName || row.batchId }}
                </button>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="isChinese ? '状态' : 'Status'" min-width="120" />
            <el-table-column prop="fileType" :label="isChinese ? '文件类型' : 'File type'" min-width="120" />
            <el-table-column prop="totalReports" :label="isChinese ? '报表总数' : 'Total reports'" min-width="120" />
            <el-table-column prop="resolvedReports" :label="isChinese ? '已解析' : 'Resolved'" min-width="110" />
            <el-table-column prop="failedReports" :label="isChinese ? '失败' : 'Failed'" min-width="100" />
            <el-table-column prop="createdAt" :label="isChinese ? '创建时间' : 'Created at'" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="isChinese ? '操作' : 'Actions'" min-width="120">
              <template #default="{ row }">
                <el-button text :loading="loading.reportBatchDetail" @click="openReportBatchCenter(row.batchId)">
                  {{ isChinese ? '批量中心' : 'Batch center' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pagination-row"
            layout="total, sizes, prev, pager, next"
            :total="reportBatchHistoryPagination.totalCount"
            :page-sizes="LIST_PAGE_SIZE_OPTIONS"
            :page-size="reportBatchHistoryPagination.pageSize"
            :current-page="reportBatchHistoryPagination.pageNo"
            @current-change="handleReportBatchHistoryPageChange"
            @size-change="handleReportBatchHistoryPageSizeChange"
          />
        </el-tab-pane>
      </el-tabs>
    </section>
  </el-tab-pane>
</template>

<style scoped src="./parse-record.css"></style>
