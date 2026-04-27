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
    {
      label: isChinese.value ? 'SQL 总数' : 'Total SQL',
      value: overview.value.totalSqlCount
    },
    {
      label: isChinese.value ? '问题 SQL' : 'Issue SQL',
      value: overview.value.issueSqlCount
    },
    {
      label: isChinese.value ? '问题总数' : 'Total issues',
      value: overview.value.totalIssueCount
    },
    {
      label: isChinese.value ? '问题场景数' : 'Issue scenes',
      value: overview.value.issueSceneCount
    },
    {
      label: isChinese.value ? 'Important SQL' : 'Important SQL',
      value: overview.value.importantSqlCount
    },
    {
      label: isChinese.value ? 'Urgent SQL' : 'Urgent SQL',
      value: overview.value.urgentSqlCount
    }
  ]
})
const priorityDistribution = computed(() => Object.entries(overview.value?.priorityDistribution || {}))

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
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">parse statistics center</p>
        <h1 class="runtime-title">{{ t('parseStatisticsCenter.title') }}</h1>
        <p class="runtime-summary">{{ t('parseStatisticsCenter.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页直接聚合 parse-statistics 的 overview、issue scene、priority matrix 和 important/urgent 清单，避免在前端重算优先级。'
            : 'This page aggregates parse-statistics overview, issue-scene distribution, the priority matrix, and important or urgent lists directly from the backend instead of recomputing priority client-side.'
        }}
      </p>
    </div>

    <article class="surface-card control-card">
      <div class="section-heading">
        <div>
          <p class="section-kicker sqlforge-code-label">statistics controls</p>
          <h2 class="section-title">{{ isChinese ? '统计刷新入口' : 'Statistics refresh' }}</h2>
        </div>
      </div>
      <div class="action-row">
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
    </article>

    <div
      v-if="errorMessage"
      class="result-banner result-banner-danger"
      data-testid="statistics-error"
    >
      {{ errorMessage }}
    </div>

    <div class="statistics-grid">
      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">overview</p>
            <h2 class="section-title">{{ isChinese ? '解析总览' : 'Parse overview' }}</h2>
          </div>
        </div>

        <div class="summary-card-grid">
          <article
            v-for="item in overviewCards"
            :key="item.label"
            class="summary-card"
          >
            <span class="summary-card-label">{{ item.label }}</span>
            <strong>{{ item.value ?? 0 }}</strong>
          </article>
        </div>

        <div class="matrix-grid">
          <article
            v-for="[priority, count] in priorityDistribution"
            :key="priority"
            class="matrix-cell"
          >
            <span class="summary-card-label">{{ priority }}</span>
            <strong>{{ count }}</strong>
          </article>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">priority matrix</p>
            <h2 class="section-title">{{ isChinese ? '优先级矩阵' : 'Priority matrix' }}</h2>
          </div>
        </div>

        <div class="matrix-grid" data-testid="statistics-priority-matrix">
          <article
            v-for="item in priorityMatrix"
            :key="`${item.priorityLevel}-${item.urgencyBucket}`"
            class="matrix-cell"
          >
            <span class="summary-card-label">{{ item.priorityLevel }} / {{ item.urgencyBucket }}</span>
            <strong>{{ item.sqlCount }} SQL</strong>
            <span>{{ item.issueCount }} issues</span>
            <span>{{ item.reportCount }} reports</span>
          </article>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">issue distribution</p>
            <h2 class="section-title">{{ isChinese ? '问题分布' : 'Issue distribution' }}</h2>
          </div>
        </div>

        <div class="list-grid">
          <div
            v-for="item in issueScenes"
            :key="item.issueScene"
            class="list-row"
            data-testid="statistics-issue-scene"
          >
            <strong>{{ item.issueScene }}</strong>
            <span>{{ item.issueDomain }} · {{ item.severity }}</span>
            <span>{{ item.priorityLevel }} / {{ item.priorityScore }}</span>
            <span>{{ item.affectedSqlCount }} SQL · {{ item.affectedIssueCount }} issues</span>
            <span>{{ formatRate(item.sqlRatio) }}</span>
            <span>{{ boolText(item.important) }} / {{ boolText(item.urgent) }}</span>
          </div>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">important urgent</p>
            <h2 class="section-title">{{ isChinese ? 'Important / Urgent 清单' : 'Important or urgent list' }}</h2>
          </div>
        </div>

        <div class="list-grid">
          <div
            v-for="item in importantUrgent"
            :key="item.itemId || item.parseTaskId"
            class="list-row"
            data-testid="statistics-important-urgent"
          >
            <strong>{{ item.reportCode || item.itemId }}</strong>
            <span>{{ item.highestPriorityLevel }} / {{ item.highestPriorityScore }}</span>
            <span>{{ item.datasourceCode || '-' }} · {{ item.stage || '-' }}</span>
            <span>{{ item.issueScenes?.join(', ') || '-' }}</span>
            <span>{{ boolText(item.important) }} / {{ boolText(item.urgent) }}</span>
          </div>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">by report</p>
            <h2 class="section-title">{{ isChinese ? '报表视角' : 'By report' }}</h2>
          </div>
        </div>

        <div class="list-grid">
          <div
            v-for="item in reportStats"
            :key="item.reportCode"
            class="list-row"
          >
            <strong>{{ item.reportCode }}</strong>
            <span>{{ item.sqlCount }} SQL</span>
            <span>{{ item.issueCount }} issues</span>
            <span>{{ item.highestPriorityLevel }} / {{ item.highestPriorityScore }}</span>
            <span>{{ boolText(item.important) }} / {{ boolText(item.urgent) }}</span>
          </div>
        </div>
      </article>

      <article class="surface-card">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">by sql</p>
            <h2 class="section-title">{{ isChinese ? 'SQL 清单' : 'By SQL' }}</h2>
          </div>
        </div>

        <div class="list-grid">
          <div
            v-for="item in sqlStats"
            :key="item.itemId || item.parseTaskId"
            class="list-row"
          >
            <strong>{{ item.reportCode || item.itemId }}</strong>
            <span>{{ item.highestPriorityLevel }} / {{ item.highestPriorityScore }}</span>
            <span>{{ item.datasourceCode || '-' }} · {{ item.stage || '-' }}</span>
            <span>{{ item.issueCount }} issues</span>
            <span>{{ item.issueScenes?.join(', ') || '-' }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.runtime-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.surface-card {
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.09), transparent 38%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.94));
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.runtime-hero,
.control-card,
.statistics-grid > article {
  padding: 24px;
}

.runtime-hero {
  display: grid;
  gap: 14px;
}

.runtime-eyebrow,
.section-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #0369a1;
}

.runtime-title,
.section-title {
  margin: 0;
  color: #0f172a;
}

.runtime-summary,
.runtime-note,
.field-label,
.list-row span {
  color: #334155;
}

.action-row,
.section-heading {
  display: flex;
  gap: 16px;
  align-items: end;
  justify-content: space-between;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}

.statistics-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.summary-card-grid,
.matrix-grid,
.list-grid {
  display: grid;
  gap: 12px;
}

.summary-card-grid,
.matrix-grid {
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
}

.summary-card,
.matrix-cell,
.list-row {
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.88);
  padding: 14px 16px;
}

.summary-card-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #475569;
}

.list-row {
  display: grid;
  gap: 6px;
}

.result-banner {
  border-radius: 18px;
  padding: 14px 16px;
}

.result-banner-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

@media (max-width: 1080px) {
  .statistics-grid {
    grid-template-columns: 1fr;
  }

  .action-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
