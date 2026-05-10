<script setup>
import { useI18n } from 'vue-i18n'
import { provide } from 'vue'
import ParseRecordFilterPanel from './ParseRecordFilterPanel.vue'
import ParseRecordEvidenceExportDialogs from './ParseRecordEvidenceExportDialogs.vue'
import ParseRecordHistoryDetailDialog from './ParseRecordHistoryDetailDialog.vue'
import ParseRecordReportDetailDrawer from './ParseRecordReportDetailDrawer.vue'
import ParseRecordReportSqlDetailDialog from './ParseRecordReportSqlDetailDialog.vue'
import ParseRecordBatchHistoryPane from './ParseRecordBatchHistoryPane.vue'
import ParseRecordSqlHistoryPane from './ParseRecordSqlHistoryPane.vue'
import { PARSE_RECORD_CONTEXT_KEY } from './parseRecordContext'
import { useParseRecordView } from './useParseRecordView'

const { t } = useI18n()

const parseRecordContext = useParseRecordView()
provide(PARSE_RECORD_CONTEXT_KEY, parseRecordContext)
const {
  activeHistoryWorkbenchTab,
  clearFilters,
  errorMessage,
  historyWorkbenchKicker,
  historyWorkbenchTitle,
  loading,
  pageSummaryCards,
  runIndexedLookup,
  searchWorkbench
} = parseRecordContext
</script>

<template>
  <section class="history-page" data-testid="parse-record-page">
    <header class="surface-card page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">{{ historyWorkbenchKicker }}</p>
        <h1 class="section-title">{{ historyWorkbenchTitle }}</h1>
      </div>
      <div class="action-row">
        <el-button type="primary" :loading="loading.page" data-testid="parse-record-refresh" @click="searchWorkbench">
          {{ t('inline.viewsParseRecordParseRecordView.text001') }}
        </el-button>
        <el-button :loading="loading.lookup" data-testid="parse-record-run-lookup" @click="runIndexedLookup">
          {{ t('inline.viewsParseRecordParseRecordView.text002') }}
        </el-button>
        <el-button @click="clearFilters">{{ t('inline.viewsParseRecordParseRecordView.text003') }}</el-button>
      </div>
    </header>

    <div v-if="errorMessage" class="inline-banner inline-banner-danger">
      {{ errorMessage }}
    </div>

    <ParseRecordFilterPanel />

    <section class="summary-grid">
      <article
        v-for="item in pageSummaryCards"
        :key="item.label"
        class="summary-card"
      >
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <el-tabs
      v-model="activeHistoryWorkbenchTab"
      class="history-workbench-tabs"
      data-testid="parse-record-history-workbench-tabs"
    >
      <ParseRecordSqlHistoryPane />

      <ParseRecordBatchHistoryPane />
    </el-tabs>

    <ParseRecordReportDetailDrawer />

    <ParseRecordReportSqlDetailDialog />

    <ParseRecordHistoryDetailDialog />

    <ParseRecordEvidenceExportDialogs />
  </section>
</template>

<style scoped src="./parse-record.css"></style>
