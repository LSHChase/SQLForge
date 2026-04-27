<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getParseStatisticsByIssueScene,
  getParseStatisticsByReport,
  getParseStatisticsBySql,
  getParseStatisticsImportantUrgent,
  getParseStatisticsOverview,
  getParseStatisticsPriorityMatrix
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a'
})

const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref('issue')
const detailDrawerVisible = ref(false)
const detailDrawerTitle = ref('')
const detailDrawerPayload = ref(null)
const overview = ref(null)
const issueScenes = ref([])
const sqlStats = ref([])
const reportStats = ref([])
const priorityMatrix = ref([])
const importantUrgent = ref([])

const isChinese = computed(() => locale.value === 'zh-CN')
const overviewCards = computed(() => {
  if (!overview.value) {
    return []
  }
  return [
    card(isChinese.value ? 'SQL 总数' : 'Total SQL', overview.value.totalSqlCount),
    card(isChinese.value ? '问题 SQL' : 'Issue SQL', overview.value.issueSqlCount),
    card(isChinese.value ? '问题总数' : 'Total issues', overview.value.totalIssueCount),
    card(isChinese.value ? '问题场景数' : 'Issue scenes', overview.value.issueSceneCount),
    card(isChinese.value ? 'Important SQL' : 'Important SQL', overview.value.importantSqlCount),
    card(isChinese.value ? 'Urgent SQL' : 'Urgent SQL', overview.value.urgentSqlCount)
  ]
})
const priorityDistribution = computed(() => Object.entries(overview.value?.priorityDistribution || {}))

const card = (label, value) => ({ label, value })

const formatRate = value => {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '-'
  }
  return `${(Number(value) * 100).toFixed(1)}%`
}

const boolText = value => {
  if (typeof value !== 'boolean') {
    return '-'
  }
  return value ? 'true' : 'false'
}

const openDetailDrawer = (title, payload) => {
  detailDrawerTitle.value = title
  detailDrawerPayload.value = payload
  detailDrawerVisible.value = true
}

const loadStatistics = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const tenantId = form.tenantId
    const [
      nextOverview,
      nextIssueScenes,
      nextSqlStats,
      nextReportStats,
      nextPriorityMatrix,
      nextImportantUrgent
    ] = await Promise.all([
      getParseStatisticsOverview(tenantId),
      getParseStatisticsByIssueScene(tenantId),
      getParseStatisticsBySql(tenantId),
      getParseStatisticsByReport(tenantId),
      getParseStatisticsPriorityMatrix(tenantId),
      getParseStatisticsImportantUrgent(tenantId)
    ])
    overview.value = nextOverview
    issueScenes.value = Array.isArray(nextIssueScenes) ? nextIssueScenes : []
    sqlStats.value = Array.isArray(nextSqlStats) ? nextSqlStats : []
    reportStats.value = Array.isArray(nextReportStats) ? nextReportStats : []
    priorityMatrix.value = Array.isArray(nextPriorityMatrix) ? nextPriorityMatrix : []
    importantUrgent.value = Array.isArray(nextImportantUrgent) ? nextImportantUrgent : []
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStatistics()
})
</script>

<template>
  <section class="runtime-page statistics-page" data-testid="statistics-page">
    <header class="page-hero shell-panel">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">parse statistics center</p>
        <h1 class="runtime-title">{{ t('parseStatisticsCenter.title') }}</h1>
        <p class="runtime-summary">{{ t('parseStatisticsCenter.summary') }}</p>
      </div>
      <div class="hero-actions">
        <label class="field-block">
          <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
          <el-input v-model="form.tenantId" />
        </label>
        <el-button
          type="primary"
          :loading="loading"
          data-testid="statistics-refresh"
          @click="loadStatistics"
        >
          {{ isChinese ? '刷新统计' : 'Refresh statistics' }}
        </el-button>
      </div>
    </header>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="statistics-error"
    >
      {{ errorMessage }}
    </div>

    <section class="summary-grid">
      <article v-for="item in overviewCards" :key="item.label" class="summary-card">
        <span class="summary-card-label">{{ item.label }}</span>
        <strong>{{ item.value ?? 0 }}</strong>
      </article>
    </section>

    <div class="workspace-grid">
      <aside class="shell-panel insight-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">Parse overview</p>
            <h2 class="section-title">{{ isChinese ? '解析总览' : 'Parse overview' }}</h2>
          </div>
        </div>

        <div class="distribution-list">
          <div
            v-for="[priority, count] in priorityDistribution"
            :key="priority"
            class="distribution-item"
          >
            <span>{{ priority }}</span>
            <strong>{{ count }}</strong>
          </div>
        </div>

        <section class="detail-card">
          <div class="section-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">Priority matrix</p>
              <h3 class="detail-title">{{ isChinese ? '优先级矩阵' : 'Priority matrix' }}</h3>
            </div>
          </div>
          <div class="matrix-list" data-testid="statistics-priority-matrix">
            <button
              v-for="item in priorityMatrix"
              :key="`${item.priorityLevel}-${item.urgencyBucket}`"
              type="button"
              class="matrix-item"
              @click="openDetailDrawer(`${item.priorityLevel} / ${item.urgencyBucket}`, item)"
            >
              <strong>{{ item.priorityLevel }} / {{ item.urgencyBucket }}</strong>
              <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues</span>
              <p>{{ item.reportCount }} reports</p>
            </button>
          </div>
        </section>
      </aside>

      <main class="shell-panel result-stage">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="isChinese ? '问题分布' : 'Issue distribution'" name="issue">
            <div class="table-header">
              <div>
                <p class="section-kicker sqlforge-code-label">Issue distribution</p>
                <h2 class="section-title">{{ isChinese ? '问题分布' : 'Issue distribution' }}</h2>
              </div>
            </div>
            <div class="list-grid">
              <button
                v-for="item in issueScenes"
                :key="item.issueScene"
                type="button"
                class="list-row"
                data-testid="statistics-issue-scene"
                @click="openDetailDrawer(item.issueScene, item)"
              >
                <strong>{{ item.issueScene }}</strong>
                <span>{{ item.issueDomain }} · {{ item.severity }}</span>
                <span>{{ item.priorityLevel }} / {{ item.priorityScore }}</span>
                <span>{{ item.affectedSqlCount }} SQL · {{ item.affectedIssueCount }} issues</span>
                <span>{{ formatRate(item.sqlRatio) }}</span>
                <span>{{ boolText(item.important) }} / {{ boolText(item.urgent) }}</span>
              </button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '重要/紧急' : 'Important or urgent list'" name="important">
            <div class="table-header">
              <div>
                <p class="section-kicker sqlforge-code-label">Important or urgent list</p>
                <h2 class="section-title">{{ isChinese ? 'Important / Urgent 清单' : 'Important or urgent list' }}</h2>
              </div>
            </div>
            <div class="list-grid">
              <button
                v-for="item in importantUrgent"
                :key="item.itemId || item.parseTaskId"
                type="button"
                class="list-row"
                data-testid="statistics-important-urgent"
                @click="openDetailDrawer(item.reportCode || item.itemId || 'important', item)"
              >
                <strong>{{ item.reportCode || item.itemId }}</strong>
                <span>{{ item.highestPriorityLevel }} / {{ item.highestPriorityScore }}</span>
                <span>{{ item.datasourceCode || '-' }} · {{ item.stage || '-' }}</span>
                <span>{{ item.issueScenes?.join(', ') || '-' }}</span>
              </button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '报表视角' : 'By report'" name="report">
            <div class="table-header">
              <div>
                <p class="section-kicker sqlforge-code-label">By report</p>
                <h2 class="section-title">{{ isChinese ? '报表视角' : 'By report' }}</h2>
              </div>
            </div>
            <div class="list-grid">
              <button
                v-for="item in reportStats"
                :key="item.reportCode"
                type="button"
                class="list-row"
                @click="openDetailDrawer(item.reportCode, item)"
              >
                <strong>{{ item.reportCode }}</strong>
                <span>{{ item.sqlCount }} SQL</span>
                <span>{{ item.issueCount }} issues</span>
                <span>{{ item.highestPriorityLevel }} / {{ item.highestPriorityScore }}</span>
                <span>{{ boolText(item.important) }} / {{ boolText(item.urgent) }}</span>
              </button>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? 'SQL 清单' : 'By SQL'" name="sql">
            <div class="table-header">
              <div>
                <p class="section-kicker sqlforge-code-label">By SQL</p>
                <h2 class="section-title">{{ isChinese ? 'SQL 清单' : 'By SQL' }}</h2>
              </div>
            </div>
            <div class="list-grid">
              <button
                v-for="item in sqlStats"
                :key="item.itemId || item.parseTaskId"
                type="button"
                class="list-row"
                @click="openDetailDrawer(item.reportCode || item.itemId || 'sql', item)"
              >
                <strong>{{ item.reportCode || item.itemId }}</strong>
                <span>{{ item.highestPriorityLevel }} / {{ item.highestPriorityScore }}</span>
                <span>{{ item.datasourceCode || '-' }} · {{ item.stage || '-' }}</span>
                <span>{{ item.issueCount }} issues</span>
                <span>{{ item.issueScenes?.join(', ') || '-' }}</span>
              </button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-drawer v-model="detailDrawerVisible" :title="detailDrawerTitle" size="40%">
      <pre class="code-block">{{ JSON.stringify(detailDrawerPayload || {}, null, 2) }}</pre>
    </el-drawer>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.shell-panel,
.summary-card,
.distribution-item,
.detail-card,
.matrix-item,
.list-row {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 18px;
  background: var(--sqlforge-surface-2);
}

.page-hero,
.hero-actions,
.workspace-grid,
.summary-grid,
.section-heading,
.distribution-list,
.matrix-list,
.list-grid {
  display: grid;
  gap: 12px;
}

.page-hero {
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  padding: 20px;
}

.hero-actions {
  align-content: start;
}

.runtime-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  color: var(--sqlforge-color-brand-text);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-size: 12px;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.runtime-summary,
.field-label,
.list-row span,
.list-row p {
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
}

.summary-card,
.distribution-item,
.detail-card,
.matrix-item,
.list-row {
  padding: 14px 16px;
}

.summary-card-label,
.field-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--sqlforge-text-muted);
}

.workspace-grid {
  grid-template-columns: minmax(290px, 0.82fr) minmax(0, 1.18fr);
}

.insight-rail,
.result-stage {
  padding: 20px;
}

.distribution-item,
.matrix-item,
.list-row {
  text-align: left;
}

.distribution-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.matrix-item,
.list-row {
  width: 100%;
}

.matrix-item strong,
.list-row strong {
  color: var(--sqlforge-text-primary);
}

.list-grid {
  margin-top: 8px;
}

.table-header {
  margin-bottom: 12px;
}

.result-banner {
  border-radius: 16px;
  padding: 14px 16px;
}

.result-banner-danger {
  background: rgba(120, 28, 28, 0.18);
  color: #ffd6d6;
}

.code-block {
  margin: 0;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .page-hero,
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
