import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const root = path.resolve(path.dirname(__filename), '..')

const read = relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8')

const fail = messages => {
  console.error('SQL UI contract check failed.')
  for (const message of messages) {
    console.error(`- ${message}`)
  }
  process.exit(1)
}

const errors = []

const { formatSqlText, highlightSql } = await import(
  pathToFileURL(path.join(root, 'src/views/common/sqlFormatting.mjs')).href
)

const sampleSql = "--report_code=RPT\nselect * from orders where name = 'select from orders' and ds = '2026-04-01'"
const formattedSql = formatSqlText(sampleSql)
if (!formattedSql.includes('SELECT') || !formattedSql.includes('\nFROM') || !formattedSql.includes('\nWHERE')) {
  errors.push('formatSqlText must uppercase core SQL clauses and break long statements into readable lines.')
}
if (!formattedSql.includes("'select from orders'")) {
  errors.push('formatSqlText must preserve quoted literal content while formatting SQL clauses.')
}

const highlightedSql = highlightSql(formattedSql)
for (const tokenClass of ['sql-token-keyword', 'sql-token-literal', 'sql-token-comment']) {
  if (!highlightedSql.includes(tokenClass)) {
    errors.push(`highlightSql must emit ${tokenClass} markup.`)
  }
}

const requiredFiles = {
  'src/views/common/SqlEditorField.vue': [
    'copyTextToClipboard',
    'formatSqlText',
    'highlightSql',
    'formatEnabled',
    'maxHeight'
  ],
  'src/views/common/SqlCodeBlock.vue': [
    'copyTextToClipboard',
    'formatSqlText',
    'highlightSql',
    'maxHeight'
  ],
  'src/views/query/SqlQueryView.vue': [
    'SqlEditorField',
    'query-flow-sql-editor',
    'SqlCodeBlock',
    'query-flow-bound-sql'
  ],
  'src/views/optimization/AccelerationView.vue': [
    'SqlEditorField',
    'parse-workbench-sql-input',
    'parse-workbench-template-sql-input',
    'SqlCodeBlock'
  ],
  'src/views/parse-batch/ParseBatchCenterView.vue': [
    'SqlEditorField',
    'batch-import-dialog-sql-input',
    'batch-import-report-dialog-sql-input',
    'SqlCodeBlock'
  ],
  'src/views/parse-record/ParseRecordView.vue': [
    'SqlCodeBlock',
    'parse-record-history-original-sql-text',
    'parse-record-report-sql-code'
  ],
  'src/views/recommendation-center/RecommendationCenterView.vue': [
    'SqlCodeBlock',
    'recommendation-source-sql',
    'recommendation-recommended-sql'
  ],
  'src/views/benchmark/BenchmarkView.vue': ['SqlEditorField', 'benchmark-sql-input']
}

for (const [relativePath, needles] of Object.entries(requiredFiles)) {
  const content = read(relativePath)
  for (const needle of needles) {
    if (!content.includes(needle)) {
      errors.push(`${relativePath} is missing ${needle}.`)
    }
  }
}

const rawSqlTextareaPattern =
  /<el-input[^>]+v-model="(?:form\.sqlText|form\.sqlTemplateText|parseBatchForm\.rawContent|reportBatchForm\.rawContent)"[^>]+type="textarea"|type="textarea"[^>]+v-model="(?:form\.sqlText|form\.sqlTemplateText|parseBatchForm\.rawContent|reportBatchForm\.rawContent)"/
const rawSqlPrePattern =
  /<pre[^>]*code-block[^>]*>\s*\{\{\s*(?:template\.content|entry\.sqlText|boundSqlPreview|item\.sqlText|selectedHistoryDetail\.sqlText|selectedRecommendation\.sourceSqlText|selectedRecommendation\.recommendedSqlText)/

for (const relativePath of Object.keys(requiredFiles).filter(item => item.endsWith('.vue'))) {
  const content = read(relativePath)
  if (rawSqlTextareaPattern.test(content)) {
    errors.push(`${relativePath} still contains a raw SQL textarea instead of SqlEditorField.`)
  }
  if (rawSqlPrePattern.test(content)) {
    errors.push(`${relativePath} still contains a raw SQL code block instead of SqlCodeBlock.`)
  }
}

const docs = read('docs/frontend/form-component-governance.md')
if (!docs.includes('HARN-070 SQL Input Output Display Contract')) {
  errors.push('docs/frontend/form-component-governance.md is missing the HARN-070 SQL display contract section.')
}

if (errors.length) {
  fail(errors)
}

console.log('SQL UI contract check passed.')
