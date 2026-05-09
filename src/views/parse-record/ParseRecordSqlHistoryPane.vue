<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

useI18n()

const {
  classificationSummary,
  formatTimestamp,
  handleHistoryPageChange,
  handleHistoryPageSizeChange,
  historyPagination,
  isChinese,
  LIST_PAGE_SIZE_OPTIONS,
  openHistoryDetail,
  rows,
  statusClass
} = useParseRecordContext()
</script>

<template>
  <el-tab-pane :label="isChinese ? 'SQL 解析记录' : 'SQL parse records'" name="sqlHistory">
    <section class="surface-card table-panel" data-testid="parse-record-sql-history-tab">
      <div class="table-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">SQL parse history table</p>
          <h2 class="section-title">{{ isChinese ? 'SQL 解析记录' : 'SQL parse records' }}</h2>
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
        <el-table-column :label="isChinese ? '解析记录 / 报表' : 'Parse record / Report'" min-width="220">
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
        <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="140" />
        <el-table-column prop="stageCode" :label="isChinese ? '阶段' : 'Stage'" min-width="110" />
        <el-table-column :label="isChinese ? '来源类型' : 'Source type'" min-width="150">
          <template #default="{ row }">{{ row.sourceType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="resultStatus" :label="isChinese ? '状态' : 'Status'" min-width="120">
          <template #default="{ row }">
            <span :class="statusClass(row.resultStatus)">{{ row.resultStatus || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="accessChannel" :label="isChinese ? '接入渠道' : 'Access channel'" min-width="130" />
        <el-table-column :label="isChinese ? '逻辑对象类型' : 'Logical objects'" min-width="160">
          <template #default="{ row }">{{ row.logicalObjectTypes?.join(', ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="parseTaskId" :label="isChinese ? '解析任务' : 'Parse task'" min-width="180" />
        <el-table-column prop="targetEngine" :label="isChinese ? '目标引擎' : 'Target engine'" min-width="120" />
        <el-table-column prop="submittedAt" :label="isChinese ? '提交时间' : 'Submitted at'" min-width="170">
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
