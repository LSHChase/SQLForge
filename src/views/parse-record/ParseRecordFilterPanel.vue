<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

useI18n()

const {
  datasourceOptions,
  datasourceOptionsLoadFailed,
  form,
  hasLookupCriteria,
  isChinese,
  queryDateRange,
  sortModeLabel,
  submittedAtRange,
  tenantOptions,
  withCurrentOption
} = useParseRecordContext()
</script>

<template>
  <section class="surface-card filter-panel">
    <div class="field-grid filter-grid">
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
        <el-select
          v-model="form.tenantId"
          filterable
          allow-create
          clearable
          default-first-option
          :placeholder="isChinese ? '空条件，使用当前上下文租户' : 'Empty filter, use current context tenant'"
          data-testid="parse-record-tenant-select"
        >
          <el-option
            v-for="item in withCurrentOption(tenantOptions, form.tenantId)"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '报表编码' : 'Report code' }}</span>
        <el-input v-model="form.reportCode" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '数据源' : 'Datasource' }}</span>
        <el-select
          v-model="form.datasourceCode"
          filterable
          allow-create
          default-first-option
          data-testid="parse-record-datasource-filter"
        >
          <el-option
            v-for="item in withCurrentOption(datasourceOptions, form.datasourceCode)"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '阶段' : 'Stage' }}</span>
        <el-input v-model="form.stage" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '业务日期' : 'Biz date' }}</span>
        <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" placeholder="2026-04-27" />
      </label>
      <label class="field-block field-block-wide">
        <span class="field-label">{{ isChinese ? '查询日期区间' : 'Query date range' }}</span>
        <el-date-picker
          v-model="queryDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          unlink-panels
          :range-separator="isChinese ? '至' : 'to'"
          :start-placeholder="isChinese ? '开始日期' : 'Start date'"
          :end-placeholder="isChinese ? '结束日期' : 'End date'"
          data-testid="parse-record-query-date-range"
        />
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '结果状态' : 'Status' }}</span>
        <el-select v-model="form.status" data-testid="parse-record-filter-select">
          <el-option label="ALL" value="" />
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="PARTIAL" value="PARTIAL" />
          <el-option label="FAILED" value="FAILED" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '接入渠道' : 'Access channel' }}</span>
        <el-select v-model="form.accessChannel" data-testid="parse-record-status-filter">
          <el-option label="ALL" value="" />
          <el-option label="PAGE" value="PAGE" />
          <el-option label="API" value="API" />
          <el-option label="JDBC_AGENT" value="JDBC_AGENT" />
          <el-option label="SDK" value="SDK" />
          <el-option label="CLIENT" value="CLIENT" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '逻辑对象类型' : 'Logical object type' }}</span>
        <el-select v-model="form.logicalObjectType">
          <el-option label="ALL" value="" />
          <el-option label="BUSINESS_VIEW" value="BUSINESS_VIEW" />
          <el-option label="DB_VIEW" value="DB_VIEW" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '目标引擎' : 'Target engine' }}</span>
        <el-select v-model="form.engine" data-testid="parse-record-engine-filter">
          <el-option label="ALL" value="" />
          <el-option label="HETU" value="HETU" />
          <el-option label="HIVE" value="HIVE" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '提交人' : 'Submitted by' }}</span>
        <el-input v-model="form.submittedBy" />
      </label>
      <label class="field-block field-block-wide">
        <span class="field-label">{{ isChinese ? '提交时间区间' : 'Submitted time range' }}</span>
        <el-date-picker
          v-model="submittedAtRange"
          type="datetimerange"
          value-format="YYYY-MM-DD[T]HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          unlink-panels
          :range-separator="isChinese ? '至' : 'to'"
          :start-placeholder="isChinese ? '开始时间' : 'Start time'"
          :end-placeholder="isChinese ? '结束时间' : 'End time'"
          data-testid="parse-record-submitted-at-range"
        />
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '排序字段' : 'Sort by' }}</span>
        <el-select v-model="form.sortBy" clearable data-testid="parse-record-sort-select">
          <el-option :label="isChinese ? '默认' : 'Default'" value="" />
          <el-option label="submittedAt" value="submittedAt" />
          <el-option label="createTime" value="createTime" />
          <el-option label="finishedAt" value="finishedAt" />
          <el-option label="status" value="status" />
          <el-option label="rowCount" value="rowCount" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? '排序方向' : 'Sort order' }}</span>
        <el-select v-model="form.sortOrder" clearable>
          <el-option :label="isChinese ? '默认' : 'Default'" value="" />
          <el-option label="DESC" value="DESC" />
          <el-option label="ASC" value="ASC" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? 'Trace ID' : 'Trace ID' }}</span>
        <el-input v-model="form.traceId" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? 'Task ID' : 'Task ID' }}</span>
        <el-input v-model="form.taskId" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ isChinese ? 'Report ID' : 'Report ID' }}</span>
        <el-input v-model="form.reportId" />
      </label>
    </div>

    <div class="chip-row">
      <span class="chip">{{ isChinese ? 'History classification' : 'History classification' }}</span>
      <span class="chip">{{ isChinese ? 'Sort mode' : 'Sort mode' }}: {{ sortModeLabel }}</span>
      <span class="chip" data-testid="parse-record-page-mode">{{ hasLookupCriteria ? 'INDEXED' : 'PAGE' }}</span>
      <span v-if="datasourceOptionsLoadFailed" class="chip chip-warning" data-testid="parse-record-datasource-options-fallback">
        {{ isChinese ? '数据源候选加载失败，保留手动输入' : 'Datasource options unavailable; manual value allowed' }}
      </span>
    </div>
  </section>
</template>

<style scoped src="./parse-record.css"></style>
