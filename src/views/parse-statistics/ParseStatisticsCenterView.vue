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

const { locale } = useI18n()

const form = reactive({
  tenantId: 'tenant-a'
})

const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref('issue')
const detailDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const detailTitle = ref('')
const detailPayload = ref(null)
const fieldHelpDialogVisible = ref(false)
const fieldHelpDialogTitle = ref('')
const fieldHelpDialogMessage = ref('')
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
    card(isChinese.value ? 'SQL 总数' : 'Total SQL', overview.value.totalSqlCount, 'totalSqlCount'),
    card(isChinese.value ? '问题 SQL' : 'Issue SQL', overview.value.issueSqlCount, 'issueSqlCount'),
    card(isChinese.value ? '问题总数' : 'Total issues', overview.value.totalIssueCount, 'totalIssueCount'),
    card(isChinese.value ? '问题场景数' : 'Issue scenes', overview.value.issueSceneCount, 'issueSceneCount'),
    card(isChinese.value ? 'Important SQL' : 'Important SQL', overview.value.importantSqlCount, 'importantSqlCount'),
    card(isChinese.value ? 'Urgent SQL' : 'Urgent SQL', overview.value.urgentSqlCount, 'urgentSqlCount')
  ].slice(0, 6)
})
const priorityDistribution = computed(() => Object.entries(overview.value?.priorityDistribution || {}))

const card = (label, value, key = '') => ({ label, value, key })

const helpTextForKey = key => {
  const glossary = {
    totalSqlCount: isChinese.value ? '当前统计范围内的 SQL 总量。' : 'Total SQL rows in the current statistics scope.',
    issueSqlCount: isChinese.value ? '至少命中一个问题场景的 SQL 数。' : 'SQL rows with at least one issue scene.',
    totalIssueCount: isChinese.value ? '所有 SQL 命中的问题总次数。' : 'Total issue hits across SQL rows.',
    issueSceneCount: isChinese.value ? '本次统计中出现过的不同问题场景数。' : 'Number of distinct issue scenes in this statistics scope.',
    importantSqlCount: isChinese.value ? '命中 important 判定的 SQL 数。' : 'SQL rows marked important by scoring.',
    urgentSqlCount: isChinese.value ? '命中 urgent 判定的 SQL 数。' : 'SQL rows marked urgent by scoring.',
    sqlList: isChinese.value ? 'SQL 清单只展示元信息和问题场景；完整 SQL 应进入独立 SQL 输出区。' : 'The SQL list shows metadata and issue scenes; full SQL belongs in a dedicated SQL output area.',
    issueLocation: isChinese.value ? '问题定位应使用命中的短 SQL 片段、失败 token 或行列信息，不展示整条 SQL。' : 'Issue location should use short matched snippets, failed tokens, or line/column data instead of full SQL.'
  }
  return glossary[key] || ''
}

const openFieldHelp = (key, label) => {
  const message = helpTextForKey(key)
  if (!message) {
    return
  }
  fieldHelpDialogTitle.value = label
  fieldHelpDialogMessage.value = message
  fieldHelpDialogVisible.value = true
}

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

const openDetailDialog = (title, payload) => {
  detailTitle.value = title
  detailPayload.value = payload
  detailDialogVisible.value = true
}

const openDetailDrawer = () => {
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
  <section class="statistics-page" data-testid="statistics-page">
    <header class="page-shell">
      <div>
        <p class="section-kicker sqlforge-code-label">parse statistics center</p>
        <h1 class="section-title">{{ isChinese ? '解析结果中心' : 'Parse result center' }}</h1>
        <p class="section-summary">{{ isChinese ? '顶部保留概览，明细统一进入表格与弹层。' : 'Top-level KPIs stay lightweight while detailed evidence moves into tables and overlays.' }}</p>
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
      class="inline-banner inline-banner-danger"
      data-testid="statistics-error"
    >
      {{ errorMessage }}
    </div>

    <section class="summary-grid">
      <article v-for="item in overviewCards" :key="item.label" class="summary-card">
        <span class="summary-card-label">
          {{ item.label }}
          <el-button
            v-if="helpTextForKey(item.key)"
            text
            size="small"
            class="help-dot"
            aria-label="field help"
            @click="openFieldHelp(item.key, item.label)"
          >
            ?
          </el-button>
        </span>
        <strong>{{ item.value ?? 0 }}</strong>
      </article>
    </section>

    <div class="workspace-grid">
      <aside class="overview-rail">
        <section class="surface-card">
          <div class="panel-heading">
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
        </section>

        <section class="surface-card">
          <div class="panel-heading">
            <div>
              <p class="section-kicker sqlforge-code-label">Priority matrix</p>
              <h2 class="section-title">{{ isChinese ? '优先级矩阵' : 'Priority matrix' }}</h2>
            </div>
          </div>

          <div class="matrix-list" data-testid="statistics-priority-matrix">
            <button
              v-for="item in priorityMatrix"
              :key="`${item.priorityLevel}-${item.urgencyBucket}`"
              type="button"
              class="matrix-item"
              @click="openDetailDialog(`${item.priorityLevel} / ${item.urgencyBucket}`, item)"
            >
              <strong>{{ item.priorityLevel }} / {{ item.urgencyBucket }}</strong>
              <span>{{ item.sqlCount }} SQL · {{ item.issueCount }} issues</span>
              <span>{{ item.reportCount }} reports</span>
            </button>
          </div>
        </section>
      </aside>

      <section class="surface-card result-stage">
        <el-tabs v-model="activeTab">
          <el-tab-pane :label="isChinese ? '问题分布' : 'Issue distribution'" name="issue">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Issue distribution</p>
                <h2 class="section-title">{{ isChinese ? '问题分布' : 'Issue distribution' }}</h2>
              </div>
            </div>
            <el-table :data="issueScenes" border>
              <el-table-column prop="issueScene" :label="isChinese ? '问题场景' : 'Issue scene'" min-width="180">
                <template #default="{ row }">
                  <button
                    type="button"
                    class="table-link"
                    data-testid="statistics-issue-scene"
                    @click="openDetailDialog(row.issueScene, row)"
                  >
                    {{ row.issueScene }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column prop="issueDomain" :label="isChinese ? '领域' : 'Domain'" min-width="120" />
              <el-table-column prop="severity" :label="isChinese ? '严重度' : 'Severity'" min-width="120" />
              <el-table-column prop="priorityLevel" :label="isChinese ? '优先级' : 'Priority'" min-width="120" />
              <el-table-column prop="affectedSqlCount" :label="isChinese ? '影响 SQL' : 'Affected SQL'" min-width="120" />
              <el-table-column prop="affectedIssueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="100" />
              <el-table-column :label="isChinese ? '占比' : 'Ratio'" min-width="100">
                <template #default="{ row }">{{ formatRate(row.sqlRatio) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '重要/紧急' : 'Important or urgent list'" name="important">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Important or urgent list</p>
                <h2 class="section-title">{{ isChinese ? 'Important / Urgent 清单' : 'Important or urgent list' }}</h2>
              </div>
            </div>
            <el-table :data="importantUrgent" border>
              <el-table-column :label="isChinese ? '对象' : 'Item'" min-width="180">
                <template #default="{ row }">
                  <button
                    type="button"
                    class="table-link"
                    data-testid="statistics-important-urgent"
                    @click="openDetailDialog(row.reportCode || row.itemId || row.parseTaskId || 'important', row)"
                  >
                    {{ row.reportCode || row.itemId || row.parseTaskId || '-' }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column :label="isChinese ? '最高优先级' : 'Highest priority'" min-width="150">
                <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
              </el-table-column>
              <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="140" />
              <el-table-column prop="stage" :label="isChinese ? '阶段' : 'Stage'" min-width="110" />
              <el-table-column :label="isChinese ? '问题场景' : 'Issue scenes'" min-width="220">
                <template #default="{ row }">{{ row.issueScenes?.join(', ') || '-' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? '报表视角' : 'By report'" name="report">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">By report</p>
                <h2 class="section-title">{{ isChinese ? '报表视角' : 'By report' }}</h2>
              </div>
            </div>
            <el-table :data="reportStats" border>
              <el-table-column prop="reportCode" :label="isChinese ? '报表编码' : 'Report code'" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openDetailDialog(row.reportCode, row)">
                    {{ row.reportCode }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column prop="sqlCount" :label="isChinese ? 'SQL 数' : 'SQL count'" min-width="120" />
              <el-table-column prop="issueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="120" />
              <el-table-column :label="isChinese ? '最高优先级' : 'Highest priority'" min-width="150">
                <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
              </el-table-column>
              <el-table-column :label="isChinese ? '重要 / 紧急' : 'Important / urgent'" min-width="130">
                <template #default="{ row }">{{ boolText(row.important) }} / {{ boolText(row.urgent) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="isChinese ? 'SQL 清单' : 'By SQL'" name="sql">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">By SQL</p>
                <h2 class="section-title">
                  {{ isChinese ? 'SQL 清单' : 'By SQL' }}
                  <el-button text size="small" class="help-dot" aria-label="field help" @click="openFieldHelp('sqlList', isChinese ? 'SQL 清单' : 'SQL list')">
                    ?
                  </el-button>
                </h2>
              </div>
            </div>
            <el-table :data="sqlStats" border>
              <el-table-column :label="isChinese ? '对象' : 'Item'" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openDetailDialog(row.reportCode || row.itemId || row.parseTaskId || 'sql', row)">
                    {{ row.reportCode || row.itemId || row.parseTaskId || '-' }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column :label="isChinese ? '优先级' : 'Priority'" min-width="150">
                <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
              </el-table-column>
              <el-table-column prop="datasourceCode" :label="isChinese ? '数据源' : 'Datasource'" min-width="140" />
              <el-table-column prop="stage" :label="isChinese ? '阶段' : 'Stage'" min-width="110" />
              <el-table-column prop="issueCount" :label="isChinese ? '问题数' : 'Issues'" min-width="110" />
              <el-table-column :label="isChinese ? '问题场景' : 'Issue scenes'" min-width="220">
                <template #default="{ row }">{{ row.issueScenes?.join(', ') || '-' }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>

    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="760px">
      <div class="detail-grid">
        <div
          v-for="[key, value] in Object.entries(detailPayload || {})"
          :key="key"
          class="detail-grid__item"
        >
          <span>{{ key }}</span>
          <strong>{{ Array.isArray(value) ? value.join(', ') : String(value) }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="openDetailDrawer">{{ isChinese ? '查看原始 JSON' : 'View raw JSON' }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" :title="detailTitle" size="42%">
      <pre class="code-block">{{ JSON.stringify(detailPayload || {}, null, 2) }}</pre>
    </el-drawer>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (isChinese ? '字段说明' : 'Field help')" width="560px">
      <p class="section-summary">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ isChinese ? '知道了' : 'Close' }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-shell,
.surface-card,
.summary-card,
.distribution-item,
.matrix-item,
.detail-grid__item {
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 34%),
    var(--sqlforge-surface-2);
}

.page-shell,
.surface-card {
  padding: 20px;
}

.page-shell,
.workspace-grid,
.summary-grid,
.panel-heading,
.hero-actions {
  display: grid;
  gap: 12px;
}

.page-shell {
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
}

.section-kicker,
.field-label {
  margin: 0 0 6px;
  color: var(--sqlforge-text-muted);
}

.section-title,
.section-summary {
  margin: 0;
}

.section-summary {
  color: var(--sqlforge-text-secondary);
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.inline-banner {
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(20, 24, 31, 0.82);
  color: var(--sqlforge-text-secondary);
}

.inline-banner-danger {
  border-color: rgba(248, 113, 113, 0.35);
  color: #fecaca;
}

.summary-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.summary-card {
  padding: 16px;
}

.summary-card-label {
  display: flex;
  gap: 4px;
  align-items: center;
  margin-bottom: 8px;
  color: var(--sqlforge-text-secondary);
}

.help-dot {
  min-width: 20px;
  height: 20px;
  padding: 0;
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 50%;
  color: var(--sqlforge-text-secondary);
  line-height: 18px;
}

.workspace-grid {
  grid-template-columns: minmax(280px, 0.78fr) minmax(0, 1.22fr);
}

.overview-rail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.distribution-list,
.matrix-list {
  display: grid;
  gap: 10px;
}

.distribution-item,
.matrix-item {
  padding: 12px 14px;
}

.matrix-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  color: inherit;
  cursor: pointer;
}

.result-stage {
  min-width: 0;
}

.table-heading {
  margin-bottom: 14px;
}

.table-link {
  border: none;
  background: transparent;
  color: var(--sqlforge-color-brand);
  cursor: pointer;
  padding: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
}

.detail-grid__item span {
  color: var(--sqlforge-text-secondary);
}

.code-block {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .page-shell,
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
