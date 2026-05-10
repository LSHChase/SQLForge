<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

const {
  classificationSummary,
  formatTimestamp,
  handleHistoryPageChange,
  handleHistoryPageSizeChange,
  historyPagination,
  LIST_PAGE_SIZE_OPTIONS,
  openHistoryDetail,
  rows,
  statusClass
} = useParseRecordContext()
</script>

<template>
  <el-tab-pane :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text001')" name="sqlHistory">
    <section class="surface-card table-panel" data-testid="parse-record-sql-history-tab">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">SQL parse history table</p>
          <h2 class="section-title">{{ t('inline.viewsParseRecordParseRecordSqlHistoryPane.text002') }}</h2>
        </div>
        <div class="chip-row">
          <span
            v-for="(value, key) in classificationSummary"
            :key="key"
            class="chip"
          >
            {{ key }}: {{ typeof value === 'object' ? Object.keys(value).length : value }}
          </span>
        </div>
      </div>

      <el-table :data="rows" border>
        <el-table-column :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text003')" min-width="220">
          <template #default="{ row }">
            <button
              type="button"
              class="table-link"
              data-testid="parse-record-trace-item"
              @click="openHistoryDetail(row.parseHistoryId || row.historyId)"
            >
              {{ row.reportCode || row.parseHistoryId || row.historyId }}
            </button>
            <div class="cell-subline">{{ row.parseHistoryId || row.historyId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="datasourceCode" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text004')" min-width="140" />
        <el-table-column prop="stageCode" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text005')" min-width="110" />
        <el-table-column :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text006')" min-width="150">
          <template #default="{ row }">{{ row.sourceType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="resultStatus" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text007')" min-width="120">
          <template #default="{ row }">
            <span :class="statusClass(row.resultStatus)">{{ row.resultStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="accessChannel" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text008')" min-width="130" />
        <el-table-column :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text009')" min-width="160">
          <template #default="{ row }">{{ row.logicalObjectTypes?.join(', ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="parseTaskId" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text010')" min-width="180" />
        <el-table-column prop="targetEngine" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text011')" min-width="120" />
        <el-table-column prop="submittedAt" :label="t('inline.viewsParseRecordParseRecordSqlHistoryPane.text012')" min-width="170">
          <template #default="{ row }">{{ formatTimestamp(row.submittedAt) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination-row"
        layout="total, sizes, prev, pager, next"
        :total="historyPagination.totalCount"
        :page-sizes="LIST_PAGE_SIZE_OPTIONS"
        :page-size="historyPagination.pageSize"
        :current-page="historyPagination.pageNo"
        @current-change="handleHistoryPageChange"
        @size-change="handleHistoryPageSizeChange"
      />
    </section>
  </el-tab-pane>
</template>

<style scoped src="./parse-record.css"></style>
