import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const root = path.resolve(path.dirname(__filename), '..')

const read = relativePath => fs.readFileSync(path.join(root, relativePath), 'utf8')
const readParseRecordSource = () => fs
  .readdirSync(path.join(root, 'src/views/parse-record'))
  .filter(file => /\.(?:vue|js|css)$/.test(file))
  .sort()
  .map(file => fs.readFileSync(path.join(root, 'src/views/parse-record', file), 'utf8'))
  .join('\n')
const readContractSource = relativePath =>
  relativePath === 'src/views/parse-record/ParseRecordView.vue'
    ? readParseRecordSource()
    : read(relativePath)

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
const { buildRecommendedSqlDisplay, buildSqlCompareRows, extractLeadingSqlComments } = await import(
  pathToFileURL(path.join(root, 'src/views/common/sqlCompare.mjs')).href
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
const mixedSql = "select 名称, amount from orders where city = '北京' and note = '@@@@'"
const highlightedMixedSql = highlightSql(mixedSql)
if (!highlightedMixedSql.includes('名称') || !highlightedMixedSql.includes('sql-token-keyword')) {
  errors.push('highlightSql must preserve mixed Chinese/English SQL while still highlighting SQL keywords.')
}
if (!highlightedMixedSql.includes('&#39;北京&#39;') || !highlightedMixedSql.includes('&#39;@@@@&#39;')) {
  errors.push('highlightSql must preserve Chinese and symbol content inside quoted literals.')
}
const sourceSqlWithHeader = "-- report_code=RPT_DIFF\n/* owner: bi */\nselect * from sales.orders where dt = ?"
const recommendedSqlWithoutHeader = 'select id from sales.orders where dt = ?'
const leadingComments = extractLeadingSqlComments(sourceSqlWithHeader)
const recommendedDisplaySql = buildRecommendedSqlDisplay(sourceSqlWithHeader, recommendedSqlWithoutHeader)
if (!leadingComments.includes('-- report_code=RPT_DIFF') || !leadingComments.includes('/* owner: bi */')) {
  errors.push('extractLeadingSqlComments must read the opening contiguous SQL comments.')
}
if (!recommendedDisplaySql.startsWith(`${leadingComments}\nselect id`)) {
  errors.push('buildRecommendedSqlDisplay must prepend source SQL opening comments to recommended SQL display text.')
}
if (buildRecommendedSqlDisplay(sourceSqlWithHeader, recommendedDisplaySql).split('-- report_code=RPT_DIFF').length !== 2) {
  errors.push('buildRecommendedSqlDisplay must not duplicate an already-carried leading comment block.')
}
const compareRows = buildSqlCompareRows(sourceSqlWithHeader, recommendedDisplaySql)
const replaceRow = compareRows.find(row => row.type === 'REPLACE')
if (
  !replaceRow ||
  !replaceRow.originalHtml.includes('sql-compare-token-mark--delete') ||
  !replaceRow.recommendedHtml.includes('sql-compare-token-mark--insert')
) {
  errors.push('buildSqlCompareRows must emit token-level delete/insert marks for replacement lines.')
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
  'src/views/sql-history/SqlHistoryView.vue': [
    'SqlCodeBlock',
    'SQL_EXECUTION_HISTORY_TYPE',
    'sql-history-query-history-table'
  ],
  'src/views/recommendation-center/RecommendationCenterView.vue': [
    'SqlCodeBlock',
    'SqlCompareBlock',
    'buildRecommendedSqlDisplay',
    'frontendCompareRecommendedSql',
    'recommendation-source-sql',
    'recommendation-recommended-sql'
  ],
  'src/views/common/SqlCompareBlock.vue': [
    'buildSqlCompareRows',
    'sql-compare-block__viewport',
    'sql-compare-token-mark--insert',
    'sql-compare-token-mark--delete'
  ],
  'src/views/common/sqlCompare.mjs': [
    'extractLeadingSqlComments',
    'buildRecommendedSqlDisplay',
    'buildSqlCompareRows',
    'sql-compare-token-mark--insert',
    'sql-compare-token-mark--delete'
  ],
  'src/views/benchmark/BenchmarkView.vue': ['SqlEditorField', 'benchmark-sql-input']
}

for (const [relativePath, needles] of Object.entries(requiredFiles)) {
  const content = readContractSource(relativePath)
  for (const needle of needles) {
    if (!content.includes(needle)) {
      errors.push(`${relativePath} is missing ${needle}.`)
    }
  }
}

const sqlEditorField = read('src/views/common/SqlEditorField.vue')
if (/<pre[^>]*sql-editor-field__highlight[^>]*>\s+<code/.test(sqlEditorField)) {
  errors.push('SqlEditorField highlight pre must not inject leading template whitespace before code.')
}
if (!/<pre[^>]*sql-editor-field__highlight[^>]*><code\s+v-html="highlightedSql"\s*\/><\/pre>/.test(sqlEditorField)) {
  errors.push('SqlEditorField highlight pre/code must stay adjacent so line one aligns with the textarea.')
}
const sharedEditorStyle = sqlEditorField.match(
  /\.sql-editor-field__highlight,\n\.sql-editor-field__textarea\s*\{([\s\S]*?)\n\}/
)?.[1] ?? ''
for (const declaration of [
  'box-sizing: border-box',
  'font-family:',
  'line-height:',
  'white-space: pre-wrap',
  'word-break: break-word',
  'overflow-wrap: break-word'
]) {
  if (!sharedEditorStyle.includes(declaration)) {
    errors.push(`SqlEditorField highlight and textarea shared style is missing ${declaration}.`)
  }
}

const sqlCodeBlock = read('src/views/common/SqlCodeBlock.vue')
if (/<pre[^>]*sql-code-panel__body[^>]*>\s+<code/.test(sqlCodeBlock)) {
  errors.push('SqlCodeBlock body pre must not inject leading template whitespace before code.')
}
if (!/<pre[^>]*sql-code-panel__body[^>]*><code\s+v-html="highlightedSql"\s*\/><\/pre>/.test(sqlCodeBlock)) {
  errors.push('SqlCodeBlock body pre/code must stay adjacent so SQL output starts at column one.')
}

const rawSqlTextareaPattern =
  /<el-input[^>]+v-model="(?:form\.sqlText|form\.sqlTemplateText|parseBatchForm\.rawContent|reportBatchForm\.rawContent)"[^>]+type="textarea"|type="textarea"[^>]+v-model="(?:form\.sqlText|form\.sqlTemplateText|parseBatchForm\.rawContent|reportBatchForm\.rawContent)"/
const rawSqlPrePattern =
  /<pre[^>]*code-block[^>]*>\s*\{\{\s*(?:template\.content|entry\.sqlText|boundSqlPreview|item\.sqlText|selectedHistoryDetail\.sqlText|selectedRecommendation\.sourceSqlText|selectedRecommendation\.recommendedSqlText)/

for (const relativePath of Object.keys(requiredFiles).filter(item => item.endsWith('.vue'))) {
  const content = readContractSource(relativePath)
  if (rawSqlTextareaPattern.test(content)) {
    errors.push(`${relativePath} still contains a raw SQL textarea instead of SqlEditorField.`)
  }
  if (rawSqlPrePattern.test(content)) {
    errors.push(`${relativePath} still contains a raw SQL code block instead of SqlCodeBlock.`)
  }
}

const parseRecordView = readParseRecordSource()
if (!/key:\s*'sqlText'[\s\S]{0,180}autoFormat:\s*false/.test(parseRecordView)) {
  errors.push('ParseRecordView raw SQL variant must disable SqlCodeBlock auto formatting.')
}
if (!/key:\s*'sqlTemplateText'[\s\S]{0,220}autoFormat:\s*false/.test(parseRecordView)) {
  errors.push('ParseRecordView template SQL variant must disable SqlCodeBlock auto formatting.')
}
if (!/key:\s*'boundSqlText'[\s\S]{0,220}autoFormat:\s*false/.test(parseRecordView)) {
  errors.push('ParseRecordView bound SQL variant must disable SqlCodeBlock auto formatting.')
}
if (!/:auto-format="false"[\s\S]{0,120}data-testid="parse-record-history-original-sql-text"/.test(parseRecordView)) {
  errors.push('ParseRecordView parse-result Original SQL block must copy and display unformatted raw SQL.')
}
if (!/:auto-format="item\.autoFormat !== false"/.test(parseRecordView)) {
  errors.push('ParseRecordView SQL tri-state blocks must honor per-variant autoFormat settings.')
}

const sqlHistoryView = read('src/views/sql-history/SqlHistoryView.vue')
if (!/key:\s*'sqlText'[\s\S]{0,180}autoFormat:\s*false/.test(sqlHistoryView)) {
  errors.push('SqlHistoryView raw SQL variant must disable SqlCodeBlock auto formatting.')
}
if (!/key:\s*'sqlTemplateText'[\s\S]{0,220}autoFormat:\s*false/.test(sqlHistoryView)) {
  errors.push('SqlHistoryView template SQL variant must disable SqlCodeBlock auto formatting.')
}
if (!/key:\s*'boundSqlText'[\s\S]{0,220}autoFormat:\s*false/.test(sqlHistoryView)) {
  errors.push('SqlHistoryView bound SQL variant must disable SqlCodeBlock auto formatting.')
}
if (!/const sqlCodeBlockProps = item => \(\{[\s\S]{0,220}autoFormat: item\.autoFormat !== false/.test(sqlHistoryView)) {
  errors.push('SqlHistoryView SqlCodeBlock props must honor per-variant autoFormat settings.')
}
if (!/class="footer-status"[\s\S]{0,120}\{\{\s*paginationStateText\s*\}\}/.test(sqlHistoryView)) {
  errors.push('SqlHistoryView footer-status must contain only the latest query status text.')
}
if (
  !/data-testid="sql-history-pagination"[\s\S]{0,220}layout="total, sizes, prev, pager, next, jumper"[\s\S]{0,220}:total="pageInfo\.total"/.test(
    sqlHistoryView
  )
) {
  errors.push('SqlHistoryView pagination must use Element Plus total, sizes, pager and jumper layout.')
}
if (/paginationSummaryText|sqlHistory\.footer\.resultWindow|data-testid="sql-history-pagination-summary"/.test(sqlHistoryView)) {
  errors.push('SqlHistoryView must not reintroduce a custom mixed pagination summary.')
}

for (const relativePath of [
  'src/views/parse-record/ParseRecordView.vue',
  'src/views/parse-batch/ParseBatchCenterView.vue'
]) {
  const content = read(relativePath)
  if (/class="help-dot issue-scene-help"[\s\S]{0,260}:title=/.test(content)) {
    errors.push(`${relativePath} issue-scene help dots must not use native title tooltips.`)
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
