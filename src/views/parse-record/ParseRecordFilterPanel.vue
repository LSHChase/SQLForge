<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

const {
  datasourceOptions,
  datasourceOptionsLoadFailed,
  form,
  hasLookupCriteria,
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
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text001') }}</span>
        <el-select
          v-model="form.tenantId"
          filterable
          allow-create
          clearable
          default-first-option
          :placeholder="t('inline.viewsParseRecordParseRecordFilterPanel.text002')"
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
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text003') }}</span>
        <el-input v-model="form.reportCode" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text004') }}</span>
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
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text005') }}</span>
        <el-input v-model="form.stage" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text006') }}</span>
        <el-date-picker v-model="form.bizDate" type="date" value-format="YYYY-MM-DD" format="YYYY-MM-DD" placeholder="2026-04-27" />
      </label>
      <label class="field-block field-block-wide">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text007') }}</span>
        <el-date-picker
          v-model="queryDateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          unlink-panels
          :range-separator="t('inline.viewsParseRecordParseRecordFilterPanel.text008')"
          :start-placeholder="t('inline.viewsParseRecordParseRecordFilterPanel.text009')"
          :end-placeholder="t('inline.viewsParseRecordParseRecordFilterPanel.text010')"
          data-testid="parse-record-query-date-range"
        />
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text011') }}</span>
        <el-select v-model="form.status" data-testid="parse-record-filter-select">
          <el-option label="ALL" value="" />
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="PARTIAL" value="PARTIAL" />
          <el-option label="FAILED" value="FAILED" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text012') }}</span>
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
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text013') }}</span>
        <el-select v-model="form.logicalObjectType">
          <el-option label="ALL" value="" />
          <el-option label="BUSINESS_VIEW" value="BUSINESS_VIEW" />
          <el-option label="DB_VIEW" value="DB_VIEW" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text014') }}</span>
        <el-select v-model="form.engine" data-testid="parse-record-engine-filter">
          <el-option label="ALL" value="" />
          <el-option label="HETU" value="HETU" />
          <el-option label="HIVE" value="HIVE" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text015') }}</span>
        <el-input v-model="form.submittedBy" />
      </label>
      <label class="field-block field-block-wide">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text016') }}</span>
        <el-date-picker
          v-model="submittedAtRange"
          type="datetimerange"
          value-format="YYYY-MM-DD[T]HH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          unlink-panels
          :range-separator="t('inline.viewsParseRecordParseRecordFilterPanel.text017')"
          :start-placeholder="t('inline.viewsParseRecordParseRecordFilterPanel.text018')"
          :end-placeholder="t('inline.viewsParseRecordParseRecordFilterPanel.text019')"
          data-testid="parse-record-submitted-at-range"
        />
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text020') }}</span>
        <el-select v-model="form.sortBy" clearable data-testid="parse-record-sort-select">
          <el-option :label="t('inline.viewsParseRecordParseRecordFilterPanel.text021')" value="" />
          <el-option label="submittedAt" value="submittedAt" />
          <el-option label="createTime" value="createTime" />
          <el-option label="finishedAt" value="finishedAt" />
          <el-option label="status" value="status" />
          <el-option label="rowCount" value="rowCount" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text022') }}</span>
        <el-select v-model="form.sortOrder" clearable>
          <el-option :label="t('inline.viewsParseRecordParseRecordFilterPanel.text023')" value="" />
          <el-option label="DESC" value="DESC" />
          <el-option label="ASC" value="ASC" />
        </el-select>
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text024') }}</span>
        <el-input v-model="form.traceId" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text025') }}</span>
        <el-input v-model="form.taskId" />
      </label>
      <label class="field-block">
        <span class="field-label">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text026') }}</span>
        <el-input v-model="form.reportId" />
      </label>
    </div>

    <div class="chip-row">
      <span class="chip">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text027') }}</span>
      <span class="chip">{{ t('inline.viewsParseRecordParseRecordFilterPanel.text028') }}: {{ sortModeLabel }}</span>
      <span class="chip" data-testid="parse-record-page-mode">{{ hasLookupCriteria ? 'INDEXED' : 'PAGE' }}</span>
      <span v-if="datasourceOptionsLoadFailed" class="chip chip-warning" data-testid="parse-record-datasource-options-fallback">
        {{ t('inline.viewsParseRecordParseRecordFilterPanel.text029') }}
      </span>
    </div>
  </section>
</template>

<style scoped src="./parse-record.css"></style>
