<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  formatRuntimeError,
  getParseStatisticsByIssueScene,
  getParseStatisticsByReport,
  getParseStatisticsBySql,
  getParseStatisticsImportantUrgent,
  getParseStatisticsOverview,
  getParseStatisticsPriorityMatrix
} from '../../services/runtimeGateApi'

const route = useRoute()
const { t, locale } = useI18n()
const statisticsTabs = new Set(['issue', 'important', 'report', 'sql', 'severity', 'priority', 'logical-object', 'parse-status'])
const requestedStatisticsTab = String(route.query.analytics || 'issue')

const form = reactive({
  tenantId: 'tenant-a'
})
const pageInfo = reactive({
  currentPage: 1,
  pageSize: 10
})

const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref(statisticsTabs.has(requestedStatisticsTab) ? requestedStatisticsTab : 'issue')
const activeDetailTab = ref('summary')
const detailDialogVisible = ref(false)
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
const detailSummaryEntries = computed(() =>
  Object.entries(detailPayload.value || {})
    .filter(([, value]) => value === null || typeof value !== 'object' || Array.isArray(value))
)
const detailRelationEntries = computed(() => {
  const payload = detailPayload.value || {}
  return [
    ['reportCode', payload.reportCode],
    ['itemId', payload.itemId],
    ['parseTaskId', payload.parseTaskId],
    ['datasourceCode', payload.datasourceCode],
    ['stage', payload.stage],
    ['issueScenes', payload.issueScenes],
    ['issueScene', payload.issueScene],
    ['sqlCount', payload.sqlCount],
    ['issueCount', payload.issueCount],
    ['affectedSqlCount', payload.affectedSqlCount],
    ['reportCount', payload.reportCount],
    ['logicalObjectKeys', payload.logicalObjectKeys]
  ].filter(([, value]) => hasDisplayValue(value) || (Array.isArray(value) && value.length > 0))
})
const severityStats = computed(() => {
  const groups = new Map()
  issueScenes.value.forEach(item => {
    const key = String(item.severity || 'UNKNOWN')
    const current = groups.get(key) || {
      severity: key,
      issueSceneCount: 0,
      affectedSqlCount: 0,
      affectedIssueCount: 0,
      urgentCount: 0
    }
    current.issueSceneCount += 1
    current.affectedSqlCount += Number(item.affectedSqlCount || 0)
    current.affectedIssueCount += Number(item.affectedIssueCount || 0)
    if (item.urgent === true || String(item.priorityLevel || '').toUpperCase() === 'P1') {
      current.urgentCount += 1
    }
    groups.set(key, current)
  })
  return Array.from(groups.values()).sort((left, right) => right.affectedSqlCount - left.affectedSqlCount)
})
const priorityStats = computed(() => {
  const groups = new Map()
  issueScenes.value.forEach(item => {
    const key = String(item.priorityLevel || 'UNKNOWN')
    const current = groups.get(key) || {
      priorityLevel: key,
      issueSceneCount: 0,
      affectedSqlCount: 0,
      affectedIssueCount: 0,
      highestPriorityScore: 0
    }
    current.issueSceneCount += 1
    current.affectedSqlCount += Number(item.affectedSqlCount || 0)
    current.affectedIssueCount += Number(item.affectedIssueCount || 0)
    current.highestPriorityScore = Math.max(current.highestPriorityScore, Number(item.priorityScore || 0))
    groups.set(key, current)
  })
  return Array.from(groups.values()).sort((left, right) => right.affectedSqlCount - left.affectedSqlCount)
})
const logicalObjectStats = computed(() => {
  const groups = new Map()
  sqlStats.value.forEach(item => {
    const keys = [
      ...(Array.isArray(item.logicalObjectKeys) ? item.logicalObjectKeys : []),
      ...(Array.isArray(item.logicalObjectTypes) ? item.logicalObjectTypes : [])
    ]
    keys.forEach(key => {
      const normalizedKey = String(key || 'OBJECT')
      const current = groups.get(normalizedKey) || {
        logicalObjectKey: normalizedKey,
        logicalObjectType: item.logicalObjectType || 'OBJECT',
        sampleCount: 0
      }
      current.sampleCount += 1
      groups.set(normalizedKey, current)
    })
  })
  return Array.from(groups.values()).sort((left, right) => right.sampleCount - left.sampleCount)
})
const parseStatusStats = computed(() => {
  const groups = new Map()
  sqlStats.value.forEach(item => {
    const key = String(item.resultStatus || item.parseStatus || item.stage || 'UNKNOWN')
    const current = groups.get(key) || {
      resultStatus: key,
      sampleCount: 0,
      cacheHitCount: 0,
      rewriteCount: 0,
      accelerationCount: 0
    }
    current.sampleCount += 1
    if (item.cacheHit === true) {
      current.cacheHitCount += 1
    }
    if (item.rewriteApplied === true) {
      current.rewriteCount += 1
    }
    if (item.accelerationApplied === true) {
      current.accelerationCount += 1
    }
    groups.set(key, current)
  })
  return Array.from(groups.values()).sort((left, right) => right.sampleCount - left.sampleCount)
})

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

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

const displayValue = value => {
  if (Array.isArray(value)) {
    return value.length ? value.join(', ') : '-'
  }
  if (!hasDisplayValue(value)) {
    return '-'
  }
  if (value && typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

const summaryWindow = items => items.slice(0, pageInfo.pageSize)

const openDetailDialog = (title, payload) => {
  detailTitle.value = title
  detailPayload.value = payload
  activeDetailTab.value = 'summary'
  detailDialogVisible.value = true
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
        <p class="section-kicker sqlforge-code-label">sql parse statistics</p>
        <h1 class="section-title">{{ t('parseStatisticsCenter.title') }}</h1>
        <p class="section-summary">{{ t('parseStatisticsCenter.summary') }}</p>
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

          <el-tab-pane :label="t('parseStatisticsCenter.severityView')" name="severity">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Severity view</p>
                <h2 class="section-title">{{ t('parseStatisticsCenter.severityView') }}</h2>
              </div>
            </div>
            <el-table :data="summaryWindow(severityStats)" border>
              <el-table-column prop="severity" :label="t('parseStatisticsCenter.severity')" min-width="140" />
              <el-table-column prop="issueSceneCount" :label="t('parseStatisticsCenter.issueScenes')" min-width="140" />
              <el-table-column prop="affectedSqlCount" :label="t('parseStatisticsCenter.affectedSql')" min-width="140" />
              <el-table-column prop="affectedIssueCount" :label="t('parseStatisticsCenter.affectedIssues')" min-width="120" />
              <el-table-column prop="urgentCount" :label="t('parseStatisticsCenter.urgentScenes')" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('parseStatisticsCenter.priorityView')" name="priority">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Priority view</p>
                <h2 class="section-title">{{ t('parseStatisticsCenter.priorityView') }}</h2>
              </div>
            </div>
            <el-table :data="summaryWindow(priorityStats)" border>
              <el-table-column prop="priorityLevel" :label="t('parseStatisticsCenter.priority')" min-width="140" />
              <el-table-column prop="issueSceneCount" :label="t('parseStatisticsCenter.issueScenes')" min-width="140" />
              <el-table-column prop="affectedSqlCount" :label="t('parseStatisticsCenter.affectedSql')" min-width="140" />
              <el-table-column prop="affectedIssueCount" :label="t('parseStatisticsCenter.affectedIssues')" min-width="120" />
              <el-table-column prop="highestPriorityScore" :label="t('parseStatisticsCenter.highestScore')" min-width="140" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('parseStatisticsCenter.logicalObjectView')" name="logical-object">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Logical object view</p>
                <h2 class="section-title">{{ t('parseStatisticsCenter.logicalObjectView') }}</h2>
              </div>
            </div>
            <el-table :data="summaryWindow(logicalObjectStats)" border>
              <el-table-column prop="logicalObjectType" :label="t('parseStatisticsCenter.logicalObjectType')" min-width="150" />
              <el-table-column prop="logicalObjectKey" :label="t('parseStatisticsCenter.logicalObjectKey')" min-width="240" />
              <el-table-column prop="sampleCount" :label="t('parseStatisticsCenter.samples')" min-width="120" />
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('parseStatisticsCenter.parseStatusSamples')" name="parse-status">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Parse status samples</p>
                <h2 class="section-title">{{ t('parseStatisticsCenter.parseStatusSamples') }}</h2>
              </div>
            </div>
            <el-table :data="summaryWindow(parseStatusStats)" border>
              <el-table-column prop="resultStatus" :label="t('parseStatisticsCenter.resultStatus')" min-width="150" />
              <el-table-column prop="sampleCount" :label="t('parseStatisticsCenter.samples')" min-width="120" />
              <el-table-column prop="cacheHitCount" :label="t('parseStatisticsCenter.cacheHit')" min-width="120" />
              <el-table-column prop="rewriteCount" :label="t('parseStatisticsCenter.rewrite')" min-width="120" />
              <el-table-column prop="accelerationCount" :label="t('parseStatisticsCenter.acceleration')" min-width="130" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>

    <el-dialog v-model="detailDialogVisible" :title="detailTitle" width="760px" data-testid="statistics-detail-dialog">
      <el-tabs v-model="activeDetailTab" data-testid="statistics-detail-tabs">
        <el-tab-pane :label="isChinese ? '摘要' : 'Summary'" name="summary">
          <div class="detail-grid">
            <div
              v-for="[key, value] in detailSummaryEntries"
              :key="key"
              class="detail-grid__item"
            >
              <span>{{ key }}</span>
              <strong>{{ displayValue(value) }}</strong>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '关联 SQL/报表' : 'Related SQL/report'" name="relations">
          <div class="detail-grid">
            <div
              v-for="[key, value] in detailRelationEntries"
              :key="key"
              class="detail-grid__item"
              data-testid="statistics-detail-relation"
            >
              <span>{{ key }}</span>
              <strong>{{ displayValue(value) }}</strong>
            </div>
          </div>
          <p v-if="!detailRelationEntries.length" class="section-summary">
            {{ isChinese ? '当前记录没有关联 SQL 或报表定位。' : 'No SQL or report locator is available for this record.' }}
          </p>
        </el-tab-pane>
        <el-tab-pane :label="isChinese ? '原始 JSON' : 'Raw JSON'" name="raw">
          <pre class="code-block" data-testid="statistics-detail-raw-json">{{ JSON.stringify(detailPayload || {}, null, 2) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

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
  grid-template-columns: 1fr;
}

.overview-rail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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
  overflow-x: auto;
}

.result-stage :deep(.el-table) {
  min-width: 100%;
}

.result-stage :deep(.el-table .cell) {
  word-break: normal;
  white-space: nowrap;
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

  .overview-rail {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
