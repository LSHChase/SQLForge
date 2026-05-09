import { readFileSync } from 'node:fs'
import { readdirSync } from 'node:fs'

const read = path => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8')
const readParseRecordSource = () => readdirSync(new URL('../src/views/parse-record', import.meta.url))
  .filter(file => /\.(?:vue|js|css)$/.test(file))
  .sort()
  .map(file => read(`src/views/parse-record/${file}`))
  .join('\n')
const readSource = path => path === 'src/views/parse-record/ParseRecordView.vue' ? readParseRecordSource() : read(path)

const checks = [
  {
    path: 'src/views/system/SystemView.vue',
    required: [
      'data-testid="system-tenant-select"',
      'v-model="datasourceForm.connectionMode"',
      '<el-input-number v-model="datasourceForm.timeoutMs"',
      'v-model="reportForm.datasourceCode"',
      'data-testid="system-report-datasource-select"',
      'v-model="redisForm.authMode"',
      'v-model="dispatchForm.targetDatasource"',
      '<el-input-number v-model="dispatchForm.maxBatchSize"',
      'type="password" show-password'
    ],
    forbidden: ['data-testid="system-tenant-input"']
  },
  {
    path: 'src/views/parse-record/ParseRecordView.vue',
    required: [
      'getGovernanceDatasources',
      'data-testid="parse-record-tenant-select"',
      'data-testid="parse-record-datasource-filter"',
      'v-model="form.bizDate" type="date"',
      'v-model="queryDateRange"',
      'type="daterange"',
      'data-testid="parse-record-query-date-range"',
      'v-model="submittedAtRange"',
      'type="datetimerange"',
      'data-testid="parse-record-submitted-at-range"',
      'value-format="YYYY-MM-DD[T]HH:mm:ss"',
      'data-testid="parse-record-datasource-options-fallback"'
    ],
    forbidden: [
      '<el-input v-model="form.bizDate"',
      '<el-input v-model="form.queryDateStart"',
      '<el-input v-model="form.submittedStart"',
      'type="datetime"'
    ]
  },
  {
    path: 'src/main.js',
    required: ['ElDatePicker', 'ElInputNumber'],
    forbidden: []
  },
  {
    path: 'src/views/common/formComponentGovernance.js',
    required: [
      'export const buildTenantOptions',
      'export const buildDatasourceOptions',
      'export const withCurrentOption',
      'connectionModeOptions',
      'stageOptions'
    ],
    forbidden: []
  }
]

const failures = []

for (const check of checks) {
  const content = readSource(check.path)
  for (const snippet of check.required) {
    if (!content.includes(snippet)) {
      failures.push(`${check.path} missing required component marker: ${snippet}`)
    }
  }
  for (const snippet of check.forbidden) {
    if (content.includes(snippet)) {
      failures.push(`${check.path} still contains forbidden input marker: ${snippet}`)
    }
  }
}

if (failures.length > 0) {
  console.error(failures.join('\n'))
  process.exit(1)
}

console.log('form component governance markers passed')
