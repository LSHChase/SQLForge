<script setup>
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  formatRuntimeError,
  getCombinedParseStatus,
  parseStructureSql,
  submitCombinedParse,
  waitForCombinedParse
} from '../../services/runtimeGateApi'

const { t, locale } = useI18n()

const terminalStatuses = new Set(['ACCESS_SUCCEEDED', 'PARTIAL_SUCCEEDED', 'FAILED', 'STRUCTURE_ONLY'])

const form = reactive({
  tenantId: 'tenant-a',
  datasourceCode: 'hetu_main',
  bindingMode: 'POSITIONAL',
  connectionRequired: true,
  sqlText: "SELECT * FROM vw_sales_daily WHERE dt = '2026-04-01' AND dt = '2026-04-01' ORDER BY id",
  sqlTemplateText: '',
  bindParametersText: '{\n  "limit": 100\n}',
  commentContextText: '{\n  "report_code": "RPT_SALES_DAILY",\n  "stage": "PROD",\n  "engine_hint": "hetu"\n}'
})

const running = ref(false)
const lastRunMode = ref('combined')
const parseResult = ref(null)
const errorMessage = ref('')

const isChinese = computed(() => locale.value === 'zh-CN')
const bindingModeOptions = computed(() => [
  {
    value: 'POSITIONAL',
    label: 'POSITIONAL'
  },
  {
    value: 'NAMED',
    label: 'NAMED'
  }
])
const activeStatus = computed(() => parseResult.value?.status || 'IDLE')
const activeConclusion = computed(() => parseResult.value?.conclusion || null)
const structureParse = computed(() => parseResult.value?.structureParse || null)
const accessParse = computed(() => parseResult.value?.accessParse || null)
const statusHistory = computed(() => parseResult.value?.statusHistory || [])
const logicalObjectHits = computed(() => structureParse.value?.logicalObjectHits || [])
const structureIssues = computed(() => structureParse.value?.issues || [])
const structureHighlights = computed(() => {
  if (!structureParse.value) {
    return []
  }
  return [
    {
      key: 'parseTaskId',
      label: isChinese.value ? 'Parse Task' : 'Parse task',
      value: structureParse.value.parseTaskId
    },
    {
      key: 'syntaxStatus',
      label: isChinese.value ? '语法状态' : 'Syntax status',
      value: structureParse.value.syntaxStatus
    },
    {
      key: 'complexityLevel',
      label: isChinese.value ? '复杂度' : 'Complexity',
      value: structureParse.value.complexityLevel
    },
    {
      key: 'sqlType',
      label: isChinese.value ? 'SQL 类型' : 'SQL type',
      value: structureParse.value.sqlType
    },
    {
      key: 'priorityLevel',
      label: isChinese.value ? '优先级' : 'Priority',
      value: structureParse.value.priorityLevel
    },
    {
      key: 'priorityScore',
      label: isChinese.value ? '评分' : 'Score',
      value: structureParse.value.priorityScore
    },
    {
      key: 'important',
      label: isChinese.value ? '重要' : 'Important',
      value: booleanLabel(structureParse.value.important)
    },
    {
      key: 'urgent',
      label: isChinese.value ? '紧急' : 'Urgent',
      value: booleanLabel(structureParse.value.urgent)
    }
  ].filter(item => hasDisplayValue(item.value))
})
const accessHighlights = computed(() => {
  if (!accessParse.value) {
    return []
  }
  return [
    {
      key: 'serviceStatus',
      label: isChinese.value ? '服务状态' : 'Service status',
      value: accessParse.value.serviceStatus
    },
    {
      key: 'connectionStatus',
      label: isChinese.value ? '连接状态' : 'Connection status',
      value: accessParse.value.connectionStatus
    },
    {
      key: 'objectResolutionStatus',
      label: isChinese.value ? '对象解析' : 'Object resolution',
      value: accessParse.value.objectResolutionStatus
    },
    {
      key: 'partitionStatus',
      label: isChinese.value ? '分区状态' : 'Partition status',
      value: accessParse.value.partitionStatus
    },
    {
      key: 'dataFreshnessStatus',
      label: isChinese.value ? '新鲜度' : 'Freshness',
      value: accessParse.value.dataFreshnessStatus
    },
    {
      key: 'slaStatus',
      label: isChinese.value ? 'SLA' : 'SLA',
      value: accessParse.value.slaStatus
    },
    {
      key: 'compatibilityStatus',
      label: isChinese.value ? '兼容性' : 'Compatibility',
      value: accessParse.value.compatibilityStatus
    }
  ].filter(item => hasDisplayValue(item.value))
})
const summaryCards = computed(() => {
  if (!parseResult.value) {
    return []
  }
  return [
    {
      label: isChinese.value ? '运行模式' : 'Run mode',
      value: lastRunMode.value === 'combined'
        ? (isChinese.value ? '综合解析' : 'Combined parse')
        : (isChinese.value ? '仅结构解析' : 'Structure only')
    },
    {
      label: isChinese.value ? '综合状态' : 'Overall status',
      value: activeConclusion.value?.overallStatus || activeStatus.value
    },
    {
      label: isChinese.value ? 'Access 可用' : 'Access available',
      value: booleanLabel(activeConclusion.value?.accessAvailable)
    },
    {
      label: isChinese.value ? '降级原因' : 'Degrade reason',
      value: activeConclusion.value?.degradeReason || parseResult.value?.degradeReason
    }
  ].filter(item => hasDisplayValue(item.value))
})
const requestSummary = computed(() => [
  {
    label: isChinese.value ? '租户' : 'Tenant',
    value: form.tenantId
  },
  {
    label: isChinese.value ? '数据源' : 'Datasource',
    value: form.datasourceCode || (isChinese.value ? '未指定' : 'Not specified')
  },
  {
    label: isChinese.value ? '绑定模式' : 'Binding mode',
    value: form.bindingMode
  },
  {
    label: isChinese.value ? 'Access Parse' : 'Access parse',
    value: form.connectionRequired ? 'ON' : 'OFF'
  }
])

const hasDisplayValue = value => !(value === null || value === undefined || String(value).trim() === '')

function booleanLabel(value) {
  if (typeof value !== 'boolean') {
    return ''
  }
  return value ? 'true' : 'false'
}

const formatTimestamp = epochMs => {
  if (!epochMs) {
    return '-'
  }
  return new Date(epochMs).toLocaleString(isChinese.value ? 'zh-CN' : 'en-US')
}

const parseJsonInput = (rawValue, label) => {
  const trimmed = String(rawValue || '').trim()
  if (!trimmed) {
    return {}
  }
  let parsed
  try {
    parsed = JSON.parse(trimmed)
  } catch (error) {
    throw new Error(
      isChinese.value
        ? `${label} 需要是合法 JSON：${error.message}`
        : `${label} must be valid JSON: ${error.message}`
    )
  }
  if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error(
      isChinese.value ? `${label} 必须是 JSON 对象。` : `${label} must be a JSON object.`
    )
  }
  return parsed
}

const buildRequestPayload = () => {
  const payload = {
    tenantId: String(form.tenantId || '').trim(),
    sqlText: String(form.sqlText || '').trim(),
    datasourceCode: String(form.datasourceCode || '').trim(),
    bindingMode: String(form.bindingMode || '').trim(),
    connectionRequired: Boolean(form.connectionRequired)
  }

  const sqlTemplateText = String(form.sqlTemplateText || '').trim()
  if (sqlTemplateText) {
    payload.sqlTemplateText = sqlTemplateText
  }

  const bindParameters = parseJsonInput(
    form.bindParametersText,
    isChinese.value ? '绑定参数' : 'Bind parameters'
  )
  if (Object.keys(bindParameters).length > 0) {
    payload.bindParameters = bindParameters
  }

  const commentContext = parseJsonInput(
    form.commentContextText,
    isChinese.value ? '注释上下文' : 'Comment context'
  )
  if (Object.keys(commentContext).length > 0) {
    payload.commentContext = commentContext
  }

  return payload
}

const normalizeStructureResult = structureOnlyResult => ({
  parseTaskId: structureOnlyResult.parseTaskId,
  status: 'STRUCTURE_ONLY',
  structureParse: structureOnlyResult,
  accessParse: null,
  degradeReason: 'STRUCTURE_ONLY_MODE',
  conclusion: {
    overallStatus: 'STRUCTURE_ONLY',
    summary: isChinese.value
      ? '当前结果只包含结构解析证据，未触发 access parse。'
      : 'This result contains structure-parse evidence only and did not trigger access parse.',
    recommendedAction: isChinese.value
      ? '如需对象可达性与连接状态，请开启 access parse 后重新执行综合解析。'
      : 'Enable access parse and rerun the combined flow when reachability and connection evidence are required.',
    structureAvailable: true,
    accessAvailable: false,
    degradeReason: 'STRUCTURE_ONLY_MODE'
  },
  statusHistory: [
    {
      status: 'STRUCTURE_SUCCEEDED',
      note: isChinese.value ? '结构解析同步返回。' : 'Structure parse returned synchronously.',
      occurredAtEpochMs: Date.now()
    }
  ]
})

const resetResult = () => {
  parseResult.value = null
  errorMessage.value = ''
}

const runStructurePreview = async () => {
  running.value = true
  lastRunMode.value = 'structure'
  errorMessage.value = ''

  try {
    const payload = buildRequestPayload()
    const structureOnlyResult = await parseStructureSql(payload, {
      requestPrefix: 'frontend-parse-workbench-structure'
    })
    parseResult.value = normalizeStructureResult(structureOnlyResult)
  } catch (error) {
    parseResult.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const runCombinedParseFlow = async () => {
  running.value = true
  lastRunMode.value = 'combined'
  errorMessage.value = ''

  try {
    const payload = buildRequestPayload()
    const initialResult = await submitCombinedParse(payload, {
      requestPrefix: 'frontend-parse-workbench-submit'
    })
    parseResult.value = initialResult
    if (!terminalStatuses.has(initialResult.status)) {
      parseResult.value = await waitForCombinedParse(initialResult.parseTaskId, payload.tenantId, {
        requestPrefix: 'frontend-parse-workbench-terminal'
      })
    }
  } catch (error) {
    parseResult.value = null
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}

const refreshParseStatus = async () => {
  const parseTaskId = parseResult.value?.parseTaskId
  if (!parseTaskId || lastRunMode.value !== 'combined') {
    return
  }
  running.value = true
  errorMessage.value = ''

  try {
    parseResult.value = await getCombinedParseStatus(parseTaskId, form.tenantId, {
      requestPrefix: 'frontend-parse-workbench-refresh'
    })
  } catch (error) {
    errorMessage.value = formatRuntimeError(error)
  } finally {
    running.value = false
  }
}
</script>

<template>
  <section class="runtime-page parse-workbench-page" data-testid="parse-workbench-page">
    <div class="runtime-hero surface-card">
      <div>
        <p class="runtime-eyebrow sqlforge-code-label">sql optimization parse workbench</p>
        <h1 class="runtime-title">{{ t('acceleration.title') }}</h1>
        <p class="runtime-summary">{{ t('acceleration.summary') }}</p>
      </div>
      <p class="runtime-note">
        {{
          isChinese
            ? '该页直接消费 /api/sql-optimization/parse 的结构解析与 combined parse 契约，把结构证据、access 证据和 partial-success 结论放到同一张工作台。'
            : 'This page consumes the structure-parse and combined-parse contracts from /api/sql-optimization/parse and keeps structure evidence, access evidence, and partial-success conclusions in one workbench.'
        }}
      </p>
    </div>

    <div class="parse-workbench__grid">
      <article class="surface-card composer-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">single sql input</p>
            <h2 class="section-title">{{ isChinese ? '解析输入与执行模式' : 'Parse input and execution mode' }}</h2>
          </div>
        </div>

        <div class="summary-chip-row">
          <span
            v-for="item in requestSummary"
            :key="item.label"
            class="summary-chip"
          >
            {{ item.label }}: <strong>{{ item.value }}</strong>
          </span>
        </div>

        <div class="form-grid">
          <label class="field-block">
            <span class="field-label">{{ isChinese ? '租户' : 'Tenant' }}</span>
            <el-input v-model="form.tenantId" />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '数据源编码' : 'Datasource code' }}</span>
            <el-input
              v-model="form.datasourceCode"
              :placeholder="isChinese ? 'hetu_main / 留空触发 partial-success' : 'hetu_main / leave blank to trigger partial-success'"
            />
          </label>

          <label class="field-block">
            <span class="field-label">{{ isChinese ? '绑定模式' : 'Binding mode' }}</span>
            <el-select v-model="form.bindingMode">
              <el-option
                v-for="option in bindingModeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </label>

          <label class="field-block field-block-toggle">
            <span class="field-label">{{ isChinese ? '执行 access parse' : 'Run access parse' }}</span>
            <el-switch v-model="form.connectionRequired" />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">SQL</span>
            <el-input
              v-model="form.sqlText"
              type="textarea"
              :rows="8"
              data-testid="parse-workbench-sql-input"
            />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '模板 SQL' : 'Template SQL' }}</span>
            <el-input
              v-model="form.sqlTemplateText"
              type="textarea"
              :rows="4"
            />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '绑定参数 JSON' : 'Bind parameters JSON' }}</span>
            <el-input
              v-model="form.bindParametersText"
              type="textarea"
              :rows="5"
            />
          </label>

          <label class="field-block field-block-wide">
            <span class="field-label">{{ isChinese ? '注释上下文 JSON' : 'Comment context JSON' }}</span>
            <el-input
              v-model="form.commentContextText"
              type="textarea"
              :rows="6"
            />
          </label>
        </div>

        <div class="action-row action-row-wrap">
          <el-button
            type="primary"
            :loading="running && lastRunMode === 'combined'"
            data-testid="parse-workbench-submit"
            @click="runCombinedParseFlow"
          >
            {{ isChinese ? '执行综合解析' : 'Run combined parse' }}
          </el-button>
          <el-button
            :loading="running && lastRunMode === 'structure'"
            data-testid="parse-workbench-structure-preview"
            @click="runStructurePreview"
          >
            {{ isChinese ? '仅结构解析' : 'Structure-only preview' }}
          </el-button>
          <el-button
            :disabled="!parseResult?.parseTaskId || lastRunMode !== 'combined'"
            :loading="running && lastRunMode === 'combined'"
            data-testid="parse-workbench-refresh-status"
            @click="refreshParseStatus"
          >
            {{ isChinese ? '刷新状态' : 'Refresh status' }}
          </el-button>
          <el-button @click="resetResult">
            {{ isChinese ? '清空结果' : 'Reset result' }}
          </el-button>
        </div>

        <div class="hint-card">
          <strong>{{ isChinese ? '调试提示' : 'Quick tip' }}</strong>
          <p>
            {{
              isChinese
                ? '把 datasourceCode 留空可以直接看到 structure-success + access-unavailable 的 partial-success 结论；填成包含 fail / unavailable 的编码可分别触发连接失败或服务不可用。'
                : 'Leave datasourceCode blank to surface a structure-success plus access-unavailable partial-success result; include fail or unavailable in the datasource code to trigger connection-failed or service-unavailable paths.'
            }}
          </p>
        </div>
      </article>

      <article class="surface-card evidence-rail">
        <div class="section-heading">
          <div>
            <p class="section-kicker sqlforge-code-label">combined conclusion</p>
            <h2 class="section-title">{{ isChinese ? '综合结论与双卡结果' : 'Combined conclusion and dual cards' }}</h2>
          </div>
        </div>

        <p v-if="!parseResult && !errorMessage" class="empty-state">
          {{
            isChinese
              ? '左侧输入一条 SQL 后可先做结构解析，也可直接执行 combined parse 查看 structure/access 双卡和综合状态机。'
              : 'Enter one SQL statement on the left to run a structure-only preview or a combined parse with structure/access dual cards and the aggregated state machine.'
          }}
        </p>

        <div
          v-if="errorMessage"
          class="result-banner result-banner-danger"
          data-testid="parse-workbench-error"
        >
          {{ errorMessage }}
        </div>

        <template v-if="parseResult">
          <div
            class="result-banner"
            :class="terminalStatuses.has(activeStatus) ? 'result-banner-success' : 'result-banner-warning'"
          >
            <strong data-testid="parse-workbench-status">{{ activeStatus }}</strong>
            <span>{{ parseResult.parseTaskId }}</span>
          </div>

          <div class="summary-card-grid">
            <article
              v-for="item in summaryCards"
              :key="item.label"
              class="summary-card"
            >
              <span class="summary-card-label">{{ item.label }}</span>
              <strong
                :data-testid="item.label === (isChinese ? '综合状态' : 'Overall status') ? 'parse-workbench-overall-status' : undefined"
              >
                {{ item.value }}
              </strong>
            </article>
          </div>

          <div v-if="activeConclusion" class="conclusion-card">
            <div class="conclusion-card__header">
              <span class="summary-card-label">{{ isChinese ? '综合结论' : 'Combined conclusion' }}</span>
              <strong data-testid="parse-workbench-overall-status">{{ activeConclusion.overallStatus }}</strong>
            </div>
            <p class="result-copy">{{ activeConclusion.summary }}</p>
            <p class="result-copy result-copy-muted">
              {{ activeConclusion.recommendedAction }}
            </p>
          </div>

          <div class="parse-card-grid">
            <article class="parse-card" data-testid="parse-workbench-structure-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">structure parse</p>
                  <h3 class="detail-title">{{ isChinese ? '结构解析卡' : 'Structure parse card' }}</h3>
                </div>
                <span class="status-pill" :class="structureParse?.syntaxStatus === 'VALID' ? 'status-pill-success' : 'status-pill-warning'">
                  {{ structureParse?.syntaxStatus || '-' }}
                </span>
              </div>

              <p v-if="!structureParse" class="empty-state">
                {{ isChinese ? '还没有结构解析结果。' : 'No structure-parse result yet.' }}
              </p>

              <template v-else>
                <div class="highlight-grid">
                  <div
                    v-for="item in structureHighlights"
                    :key="item.key"
                    class="highlight-chip"
                  >
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '查询日期摘要' : 'Query-date summary' }}</span>
                  <div class="summary-chip-row">
                    <span class="summary-chip">
                      {{ isChinese ? '起点' : 'Start' }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateStart || '-' }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ isChinese ? '终点' : 'End' }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateEnd || '-' }}</strong>
                    </span>
                    <span class="summary-chip">
                      {{ isChinese ? '状态' : 'Status' }}:
                      <strong>{{ structureParse.queryDateSummary?.queryDateStatus || '-' }}</strong>
                    </span>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '逻辑对象命中' : 'Logical object hits' }}</span>
                  <div v-if="logicalObjectHits.length" class="pill-grid">
                    <span
                      v-for="(item, index) in logicalObjectHits"
                      :key="`${item.objectKey || item.objectName || 'object'}-${index}`"
                      class="summary-chip"
                      data-testid="parse-workbench-logical-hit"
                    >
                      {{ item.objectType }}: <strong>{{ item.objectKey || item.objectName }}</strong>
                    </span>
                  </div>
                  <p v-else class="empty-inline">
                    {{ isChinese ? '未命中逻辑对象。' : 'No logical objects were resolved.' }}
                  </p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '风险标签 / 改写候选' : 'Risk tags / rewrite candidates' }}</span>
                  <div class="pill-grid">
                    <span
                      v-for="item in structureParse.riskTags || []"
                      :key="`risk-${item}`"
                      class="summary-chip summary-chip-warning"
                    >
                      {{ item }}
                    </span>
                    <span
                      v-for="item in structureParse.rewriteCandidates || []"
                      :key="`candidate-${item}`"
                      class="summary-chip summary-chip-success"
                    >
                      {{ item }}
                    </span>
                  </div>
                </div>

                <div class="issue-list">
                  <article
                    v-for="(issue, index) in structureIssues"
                    :key="`${issue.issueCode || 'issue'}-${index}`"
                    class="issue-card"
                    data-testid="parse-workbench-issue"
                  >
                    <div class="issue-card__header">
                      <strong>{{ issue.issueCode }}</strong>
                      <span>{{ issue.severity }} · {{ issue.priorityLevel }}</span>
                    </div>
                    <p class="issue-card__summary">{{ issue.summary }}</p>
                    <p class="issue-card__detail">{{ issue.detail }}</p>
                    <p class="issue-card__detail">
                      {{ isChinese ? '建议动作' : 'Suggested action' }}: {{ issue.suggestedAction }}
                    </p>
                  </article>
                </div>
              </template>
            </article>

            <article class="parse-card" data-testid="parse-workbench-access-card">
              <div class="parse-card__header">
                <div>
                  <p class="section-kicker sqlforge-code-label">access parse</p>
                  <h3 class="detail-title">{{ isChinese ? 'Access Parse 卡' : 'Access parse card' }}</h3>
                </div>
                <span
                  class="status-pill"
                  :class="accessParse?.serviceStatus === 'AVAILABLE' && accessParse?.connectionStatus === 'CONNECTED'
                    ? 'status-pill-success'
                    : 'status-pill-warning'"
                >
                  {{ accessParse?.serviceStatus || (isChinese ? '未执行' : 'Not run') }}
                </span>
              </div>

              <p v-if="!accessParse" class="empty-state">
                {{
                  isChinese
                    ? '结构解析模式不会生成 access parse 结果；综合解析会在结构成功后异步补跑。'
                    : 'Structure-only mode does not generate access-parse output; combined mode schedules it after structure success.'
                }}
              </p>

              <template v-else>
                <div class="highlight-grid">
                  <div
                    v-for="item in accessHighlights"
                    :key="item.key"
                    class="highlight-chip"
                  >
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? 'Plan Summary' : 'Plan summary' }}</span>
                  <p class="result-copy">{{ accessParse.planSummary || '-' }}</p>
                </div>

                <div class="mini-section">
                  <span class="summary-card-label">{{ isChinese ? '可用性告警' : 'Availability warning' }}</span>
                  <p class="result-copy result-copy-muted">{{ accessParse.availabilityWarning || '-' }}</p>
                </div>
              </template>
            </article>
          </div>

          <div class="history-panel">
            <div class="parse-card__header">
              <div>
                <p class="section-kicker sqlforge-code-label">status history</p>
                <h3 class="detail-title">{{ isChinese ? '综合状态机追溯' : 'Combined state history' }}</h3>
              </div>
            </div>

            <div class="history-list">
              <article
                v-for="(entry, index) in statusHistory"
                :key="`${entry.status || 'status'}-${index}`"
                class="history-item"
                data-testid="parse-workbench-history-entry"
              >
                <strong>{{ entry.status }}</strong>
                <span>{{ formatTimestamp(entry.occurredAtEpochMs) }}</span>
                <p>{{ entry.note || '-' }}</p>
              </article>
            </div>
          </div>
        </template>
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
  border: 1px solid var(--sqlforge-border-default);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(56, 189, 248, 0.08), transparent 38%),
    var(--sqlforge-surface-2);
  box-shadow: none;
}

.runtime-hero,
.parse-workbench__grid > article {
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
  color: #0f766e;
}

.runtime-title,
.section-title,
.detail-title {
  margin: 0;
  color: var(--sqlforge-text-primary);
}

.runtime-summary,
.runtime-note,
.result-copy,
.issue-card__summary,
.issue-card__detail,
.history-item p {
  margin: 0;
  color: var(--sqlforge-text-secondary);
  line-height: 1.6;
}

.result-copy-muted {
  color: #475569;
}

.parse-workbench__grid {
  display: grid;
  gap: 24px;
  grid-template-columns: minmax(320px, 1.05fr) minmax(360px, 1.35fr);
}

.composer-rail,
.evidence-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.field-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-block-wide {
  grid-column: 1 / -1;
}

.field-block-toggle {
  justify-content: flex-end;
}

.field-label {
  font-size: 13px;
  color: #475569;
}

.action-row {
  display: flex;
  gap: 12px;
}

.action-row-wrap,
.summary-chip-row,
.pill-grid {
  flex-wrap: wrap;
}

.summary-chip-row,
.pill-grid {
  display: flex;
  gap: 10px;
}

.summary-chip {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: var(--sqlforge-bg-page-deep);
  color: var(--sqlforge-text-primary);
  font-size: 12px;
}

.summary-chip-warning {
  background: rgba(251, 191, 36, 0.18);
}

.summary-chip-success {
  background: rgba(16, 185, 129, 0.18);
}

.hint-card,
.conclusion-card,
.history-panel,
.parse-card {
  border-radius: 22px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(35, 35, 35, 0.92);
  padding: 18px;
}

.summary-card-grid,
.parse-card-grid,
.highlight-grid {
  display: grid;
  gap: 14px;
}

.summary-card-grid {
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.parse-card-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-card,
.highlight-chip,
.history-item,
.issue-card {
  border-radius: 18px;
  border: 1px solid var(--sqlforge-border-default);
  background: rgba(35, 35, 35, 0.92);
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

.conclusion-card__header,
.parse-card__header,
.issue-card__header,
.history-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.conclusion-card__header,
.parse-card__header,
.issue-card__header {
  align-items: flex-start;
}

.highlight-chip {
  display: grid;
  gap: 8px;
}

.highlight-chip span,
.history-item span {
  color: #475569;
  font-size: 12px;
}

.status-pill {
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.status-pill-success {
  background: rgba(16, 185, 129, 0.16);
  color: #047857;
}

.status-pill-warning {
  background: rgba(251, 191, 36, 0.16);
  color: #b45309;
}

.mini-section,
.issue-list,
.history-list {
  display: grid;
  gap: 12px;
}

.empty-state,
.empty-inline {
  margin: 0;
  color: #64748b;
}

.history-item {
  align-items: center;
}

.history-item p {
  grid-column: 1 / -1;
}

.result-banner {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border-radius: 18px;
  padding: 14px 16px;
}

.result-banner-success {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.result-banner-warning {
  background: rgba(251, 191, 36, 0.16);
  color: #92400e;
}

.result-banner-danger {
  background: rgba(239, 68, 68, 0.14);
  color: #b91c1c;
}

@media (max-width: 1080px) {
  .parse-workbench__grid,
  .parse-card-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
