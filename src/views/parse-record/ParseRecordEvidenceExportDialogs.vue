<script setup>
import { useI18n } from 'vue-i18n'
import { useParseRecordContext } from './parseRecordContext'

const { t } = useI18n()

const {
  evidenceDrawerVisible,
  exportDialogVisible,
  exportForm,
  exportResult,
  formatJson,
  loading,
  runExport,
  selectedHistoryDetail
} = useParseRecordContext()

const exportFormatOptions = ['JSON', 'CSV', 'EXCEL', 'SQL_TEXT', 'PDF_REPORT']
</script>

<template>
  <el-drawer v-model="evidenceDrawerVisible" :title="t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text001')" size="44%">
    <pre class="code-block">{{ formatJson(selectedHistoryDetail || {}) }}</pre>
  </el-drawer>

  <el-dialog v-model="exportDialogVisible" :title="t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text002')" width="720px">
    <div class="dialog-stack">
      <div class="field-grid">
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text003') }}</span>
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
          <span class="field-label">{{ t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text004') }}</span>
          <el-input v-model="exportForm.exportReason" />
        </label>
        <label class="field-block">
          <span class="field-label">{{ t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text005') }}</span>
          <el-switch v-model="exportForm.includeTraceDetail" />
        </label>
      </div>
      <div class="dialog-actions">
        <el-button type="primary" :loading="loading.export" @click="runExport">{{ t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text006') }}</el-button>
      </div>
      <div v-if="exportResult" class="code-grid">
        <article class="code-card">
          <div class="code-card__header">
            <span>{{ t('inline.viewsParseRecordParseRecordEvidenceExportDialogs.text007') }}</span>
          </div>
          <pre class="code-block" data-testid="parse-record-export-result">{{ formatJson(exportResult) }}</pre>
        </article>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped src="./parse-record.css"></style>
