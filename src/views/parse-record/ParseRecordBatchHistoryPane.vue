<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

const {
  batchHistoryErrorMessage,
  batchHistoryTab,
  formatTimestamp,
  handleParseBatchHistoryPageChange,
  handleParseBatchHistoryPageSizeChange,
  handleReportBatchHistoryPageChange,
  handleReportBatchHistoryPageSizeChange,
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
  <el-tab-pane :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text001')" name="batchHistory">
    <section class="surface-card batch-history-panel" data-testid="parse-record-batch-report-history-tab">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">batch history</p>
          <h2 class="section-title">{{ t('inline.viewsParseRecordParseRecordBatchHistoryPane.text002') }}</h2>
        </div>
        <div class="chip-row">
          <span class="chip">{{ t('inline.viewsParseRecordParseRecordBatchHistoryPane.text003') }}</span>
          <span class="chip">{{ t('inline.viewsParseRecordParseRecordBatchHistoryPane.text004') }}</span>
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
        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text005')" name="parse">
          <section class="summary-grid batch-history-summary">
            <article v-for="item in parseBatchHistorySummary" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </section>
          <el-table :data="parseBatchHistoryRows" border>
            <el-table-column prop="batchName" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text006')" min-width="200">
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
            <el-table-column prop="status" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text007')" min-width="120" />
            <el-table-column prop="importMode" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text008')" min-width="140" />
            <el-table-column prop="fileType" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text009')" min-width="120" />
            <el-table-column prop="totalRecords" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text010')" min-width="110" />
            <el-table-column prop="successRecords" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text011')" min-width="100" />
            <el-table-column prop="failedRecords" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text012')" min-width="100" />
            <el-table-column prop="createdAt" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text013')" min-width="170">
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

        <el-tab-pane :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text014')" name="report">
          <section class="summary-grid batch-history-summary">
            <article v-for="item in reportBatchHistorySummary" :key="item.label" class="summary-card">
              <span class="summary-card-label">{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </section>
          <el-table :data="reportBatchHistoryRows" border>
            <el-table-column prop="batchName" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text015')" min-width="200">
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
            <el-table-column prop="status" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text016')" min-width="120" />
            <el-table-column prop="fileType" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text017')" min-width="120" />
            <el-table-column prop="totalReports" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text018')" min-width="120" />
            <el-table-column prop="resolvedReports" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text019')" min-width="110" />
            <el-table-column prop="failedReports" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text020')" min-width="100" />
            <el-table-column prop="createdAt" :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text021')" min-width="170">
              <template #default="{ row }">{{ formatTimestamp(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="t('inline.viewsParseRecordParseRecordBatchHistoryPane.text022')" min-width="120">
              <template #default="{ row }">
                <el-button text :loading="loading.reportBatchDetail" @click="openReportBatchCenter(row.batchId)">
                  {{ t('inline.viewsParseRecordParseRecordBatchHistoryPane.text023') }}
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
