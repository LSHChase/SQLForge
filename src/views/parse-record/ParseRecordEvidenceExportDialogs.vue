<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

useI18n()

const {
  evidenceDrawerVisible,
  exportDialogVisible,
  exportForm,
  exportResult,
  formatJson,
  isChinese,
  loading,
  runExport,
  selectedHistoryDetail
} = useParseRecordContext()

const exportFormatOptions = ['JSON', 'CSV', 'EXCEL', 'SQL_TEXT', 'PDF_REPORT']
</script>

<template>
  <el-drawer v-model="evidenceDrawerVisible" :title="isChinese ? '原始证据' : 'Raw evidence'" size="44%">
    <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
  </el-drawer>

  <el-dialog v-model="exportDialogVisible" :title="isChinese ? '导出取证' : 'Export evidence'" width="720px">
    <div class="dialog-stack">
      <div class="field-grid">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '导出格式' : 'Export format' }}</span>
          <el-select v-model="exportForm.exportFormat">
            <el-option
              v-for="item in exportFormatOptions"
              :key="item"
              :value="item"
            >
              {{ item }}
            </el-option>
          </el-select>
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '导出原因' : 'Export reason' }}</span>
          <el-input v-model="exportForm.exportReason" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '携带 Trace 明细' : 'Include trace detail' }}</span>
          <el-switch v-model="exportForm.includeTraceDetail" />
        </label>
      </div>
      <div class="dialog-actions">
        <el-button type="primary" :loading="loading.export" @click="runExport">{{ isChinese ? '执行导出' : 'Run export' }}</el-button>
      </div>
      <div v-if="exportResult" class="code-grid">
        <article class="code-card">
          <div class="code-card__header">
            <span>{{ isChinese ? '导出摘要' : 'Export summary' }}</span>
          </div>
          <pre class="code-block" data-testid="parse-record-export-result">{{ formatJson(exportResult) }}</pre>
        </article>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped src="./parse-record.css"></style>
