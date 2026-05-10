<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
const { t } = useI18n()
const statisticsTabs = new Set(['issue', 'important', 'report', 'sql', 'severity', 'priority', 'logical-object', 'parse-status'])

const form = reactive({
  tenantId: 'tenant-a'
})
const pageInfo = reactive({
  currentPage: 1,
  pageSize: 10
})

const loading = ref(false)
const errorMessage = ref('')
const activeTab = ref(routeStatisticsTab())
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
const statisticErrors = reactive({
  overview: '',
  issueScenes: '',
  sqlStats: '',
  reportStats: '',
  priorityMatrix: '',
  importantUrgent: ''
})

const statisticErrorMessages = computed(() =>
  Object.entries(statisticErrors)
    .filter(([, message]) => Boolean(message))
    .map(([key, message]) => ({
      key,
      label: statisticErrorLabel(key),
      message
    }))
)
const overviewCards = computed(() => {
  if (!overview.value) {
    return []
  }
  return [
    card(t('inline.viewsParseStatisticsParseStatisticsCenterView.text001'), overview.value.totalSqlCount, 'totalSqlCount'),
    card(t('inline.viewsParseStatisticsParseStatisticsCenterView.text002'), overview.value.issueSqlCount, 'issueSqlCount'),
    card(t('inline.viewsParseStatisticsParseStatisticsCenterView.text003'), overview.value.totalIssueCount, 'totalIssueCount'),
    card(t('inline.viewsParseStatisticsParseStatisticsCenterView.text004'), overview.value.issueSceneCount, 'issueSceneCount'),
    card(t('inline.viewsParseStatisticsParseStatisticsCenterView.text005'), overview.value.importantSqlCount, 'importantSqlCount'),
    card(t('inline.viewsParseStatisticsParseStatisticsCenterView.text006'), overview.value.urgentSqlCount, 'urgentSqlCount')
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
      ...normalizeDisplayList(item.logicalObjectKeys),
      ...normalizeDisplayList(item.logicalObjectTypes),
      ...normalizeDisplayList(item.logicalObjectKey),
      ...normalizeDisplayList(item.logicalObjectType)
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

function routeStatisticsTab() {
  const requestedStatisticsTab = String(route.query.analytics || 'issue')
  return statisticsTabs.has(requestedStatisticsTab) ? requestedStatisticsTab : 'issue'
}

const statisticErrorLabel = key => {
  const labels = {
    overview: t('parseStatisticsCenter.errorLabels.overview'),
    issueScenes: t('parseStatisticsCenter.errorLabels.issueScenes'),
    sqlStats: t('parseStatisticsCenter.errorLabels.sqlStats'),
    reportStats: t('parseStatisticsCenter.errorLabels.reportStats'),
    priorityMatrix: t('parseStatisticsCenter.errorLabels.priorityMatrix'),
    importantUrgent: t('parseStatisticsCenter.errorLabels.importantUrgent')
  }
  return labels[key] || key
}

const helpTextForKey = key => {
  const glossary = {
    totalSqlCount: t('inline.viewsParseStatisticsParseStatisticsCenterView.text007'),
    issueSqlCount: t('inline.viewsParseStatisticsParseStatisticsCenterView.text008'),
    totalIssueCount: t('inline.viewsParseStatisticsParseStatisticsCenterView.text009'),
    issueSceneCount: t('inline.viewsParseStatisticsParseStatisticsCenterView.text010'),
    importantSqlCount: t('inline.viewsParseStatisticsParseStatisticsCenterView.text011'),
    urgentSqlCount: t('inline.viewsParseStatisticsParseStatisticsCenterView.text012'),
    sqlList: t('inline.viewsParseStatisticsParseStatisticsCenterView.text013'),
    issueLocation: t('inline.viewsParseStatisticsParseStatisticsCenterView.text014')
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

const normalizeDisplayList = value => {
  if (Array.isArray(value)) {
    return value
      .filter(item => hasDisplayValue(item))
      .map(item => (item && typeof item === 'object' ? JSON.stringify(item) : String(item)))
  }
  if (!hasDisplayValue(value)) {
    return []
  }
  if (value && typeof value === 'object') {
    return [JSON.stringify(value)]
  }
  return [String(value)]
}

const displayList = value => {
  const items = normalizeDisplayList(value)
  return items.length ? items.join(', ') : '-'
}

const displayValue = value => {
  if (Array.isArray(value)) {
    return displayList(value)
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

const statisticLoaders = [
  {
    key: 'overview',
    load: tenantId => getParseStatisticsOverview(tenantId),
    assign: value => {
      overview.value = value && typeof value === 'object' && !Array.isArray(value) ? value : null
    },
    clear: () => {
      overview.value = null
    }
  },
  {
    key: 'issueScenes',
    load: tenantId => getParseStatisticsByIssueScene(tenantId),
    assign: value => {
      issueScenes.value = Array.isArray(value) ? value : []
    },
    clear: () => {
      issueScenes.value = []
    }
  },
  {
    key: 'sqlStats',
    load: tenantId => getParseStatisticsBySql(tenantId),
    assign: value => {
      sqlStats.value = Array.isArray(value) ? value : []
    },
    clear: () => {
      sqlStats.value = []
    }
  },
  {
    key: 'reportStats',
    load: tenantId => getParseStatisticsByReport(tenantId),
    assign: value => {
      reportStats.value = Array.isArray(value) ? value : []
    },
    clear: () => {
      reportStats.value = []
    }
  },
  {
    key: 'priorityMatrix',
    load: tenantId => getParseStatisticsPriorityMatrix(tenantId),
    assign: value => {
      priorityMatrix.value = Array.isArray(value) ? value : []
    },
    clear: () => {
      priorityMatrix.value = []
    }
  },
  {
    key: 'importantUrgent',
    load: tenantId => getParseStatisticsImportantUrgent(tenantId),
    assign: value => {
      importantUrgent.value = Array.isArray(value) ? value : []
    },
    clear: () => {
      importantUrgent.value = []
    }
  }
]

const resetStatisticErrors = () => {
  Object.keys(statisticErrors).forEach(key => {
    statisticErrors[key] = ''
  })
}

const loadStatistics = async () => {
  loading.value = true
  errorMessage.value = ''
  resetStatisticErrors()
  try {
    const tenantId = form.tenantId
    const results = await Promise.allSettled(
      statisticLoaders.map(loader => Promise.resolve().then(() => loader.load(tenantId)))
    )
    let successCount = 0
    let firstError = null
    results.forEach((result, index) => {
      const loader = statisticLoaders[index]
      if (result.status === 'fulfilled') {
        try {
          loader.assign(result.value)
          successCount += 1
        } catch (error) {
          firstError = firstError || error
          loader.clear()
          statisticErrors[loader.key] = formatRuntimeError(error)
        }
        return
      }
      firstError = firstError || result.reason
      loader.clear()
      statisticErrors[loader.key] = formatRuntimeError(result.reason)
    })
    if (successCount === 0 && firstError) {
      errorMessage.value = formatRuntimeError(firstError)
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadStatistics()
})

watch(
  () => route.query.analytics,
  () => {
    activeTab.value = routeStatisticsTab()
  }
)
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
          <span class="field-label">{{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text015') }}</span>
          <el-input v-model="form.tenantId" />
        </label>
        <el-button
          type="primary"
          :loading="loading"
          data-testid="statistics-refresh"
          @click="loadStatistics"
        >
          {{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text016') }}
        </el-button>
      </div>
    </header>

    <div
      v-if="errorMessage || statisticErrorMessages.length"
      class="inline-banner inline-banner-danger"
      data-testid="statistics-error"
    >
      <p v-if="errorMessage">{{ errorMessage }}</p>
      <p
        v-for="item in statisticErrorMessages"
        :key="item.key"
        data-testid="statistics-endpoint-error"
      >
        <strong>{{ item.label }}</strong>: {{ item.message }}
      </p>
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
              <h2 class="section-title">{{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text017') }}</h2>
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
              <h2 class="section-title">{{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text018') }}</h2>
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
          <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text019')" name="issue">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Issue distribution</p>
                <h2 class="section-title">{{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text020') }}</h2>
              </div>
            </div>
            <el-table :data="issueScenes" border>
              <el-table-column prop="issueScene" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text021')" min-width="180">
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
              <el-table-column prop="issueDomain" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text022')" min-width="120" />
              <el-table-column prop="severity" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text023')" min-width="120" />
              <el-table-column prop="priorityLevel" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text024')" min-width="120" />
              <el-table-column prop="affectedSqlCount" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text025')" min-width="120" />
              <el-table-column prop="affectedIssueCount" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text026')" min-width="100" />
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text027')" min-width="100">
                <template #default="{ row }">{{ formatRate(row.sqlRatio) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text028')" name="important">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">Important or urgent list</p>
                <h2 class="section-title">{{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text029') }}</h2>
              </div>
            </div>
            <el-table :data="importantUrgent" border>
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text030')" min-width="180">
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
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text031')" min-width="150">
                <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
              </el-table-column>
              <el-table-column prop="datasourceCode" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text032')" min-width="140" />
              <el-table-column prop="stage" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text033')" min-width="110" />
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text034')" min-width="220">
                <template #default="{ row }">{{ displayList(row.issueScenes) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text035')" name="report">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">By report</p>
                <h2 class="section-title">{{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text036') }}</h2>
              </div>
            </div>
            <el-table :data="reportStats" border>
              <el-table-column prop="reportCode" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text037')" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openDetailDialog(row.reportCode, row)">
                    {{ row.reportCode }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column prop="sqlCount" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text038')" min-width="120" />
              <el-table-column prop="issueCount" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text039')" min-width="120" />
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text040')" min-width="150">
                <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text041')" min-width="130">
                <template #default="{ row }">{{ boolText(row.important) }} / {{ boolText(row.urgent) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text042')" name="sql">
            <div class="table-heading">
              <div>
                <p class="section-kicker sqlforge-code-label">By SQL</p>
                <h2 class="section-title">
                  {{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text043') }}
                  <el-button text size="small" class="help-dot" aria-label="field help" @click="openFieldHelp('sqlList', t('inline.viewsParseStatisticsParseStatisticsCenterView.text044'))">
                    ?
                  </el-button>
                </h2>
              </div>
            </div>
            <el-table :data="sqlStats" border>
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text045')" min-width="180">
                <template #default="{ row }">
                  <button type="button" class="table-link" @click="openDetailDialog(row.reportCode || row.itemId || row.parseTaskId || 'sql', row)">
                    {{ row.reportCode || row.itemId || row.parseTaskId || '-' }}
                  </button>
                </template>
              </el-table-column>
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text046')" min-width="150">
                <template #default="{ row }">{{ row.highestPriorityLevel }} / {{ row.highestPriorityScore }}</template>
              </el-table-column>
              <el-table-column prop="datasourceCode" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text047')" min-width="140" />
              <el-table-column prop="stage" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text048')" min-width="110" />
              <el-table-column prop="issueCount" :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text049')" min-width="110" />
              <el-table-column :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text050')" min-width="220">
                <template #default="{ row }">{{ displayList(row.issueScenes) }}</template>
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
        <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text051')" name="summary">
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
        <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text052')" name="relations">
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
            {{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text053') }}
          </p>
        </el-tab-pane>
        <el-tab-pane :label="t('inline.viewsParseStatisticsParseStatisticsCenterView.text054')" name="raw">
          <pre class="code-block" data-testid="statistics-detail-raw-json">{{ JSON.stringify(detailPayload || {}, null, 2) }}</pre>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="fieldHelpDialogVisible" :title="fieldHelpDialogTitle || (t('inline.viewsParseStatisticsParseStatisticsCenterView.text055'))" width="560px">
      <p class="section-summary">{{ fieldHelpDialogMessage }}</p>
      <template #footer>
        <el-button type="primary" @click="fieldHelpDialogVisible = false">
          {{ t('inline.viewsParseStatisticsParseStatisticsCenterView.text056') }}
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

.inline-banner p {
  margin: 0;
}

.inline-banner p + p {
  margin-top: 6px;
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
