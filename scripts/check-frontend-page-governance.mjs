import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const root = path.resolve(path.dirname(__filename), '..')

const frontendPathPattern = /^(src\/views\/.*\.vue|src\/components\/.*\.vue|src\/locales\/.+)$/
const vuePathPattern = /^(src\/views\/.*\.vue|src\/components\/.*\.vue)$/
const managementVuePathPattern = /^src\/views\/(?!common\/|dashboard\/|delivery\/).+\.vue$|^src\/components\/.+\.vue$/
const managementViewPagePattern = /^src\/views\/(?!common\/|dashboard\/|delivery\/|runtime-gates\/|recovery-drill\/).+\.vue$/
const localePathPattern = /^src\/locales\//
const filterControlPattern = /<(?:el-input|el-select|el-date-picker|el-autocomplete|el-cascader|el-radio-group|el-checkbox-group|el-form-item)\b/g
const responsiveFilterPattern =
  /(?:SearchForm|ToolbarShell|filter-grid|search-grid|filters-grid|toolbar-grid|control-grid|filter-row|display:\s*(?:grid|flex)|grid-template-columns|repeat\(|minmax\(|flex-wrap\s*:\s*wrap|\binline\b|:inline=)/
const duplicatePaginationSummaryPattern =
  /paginationSummary(?:Text)?|resultWindow(?:Text)?|data-testid=["'][^"']*(?:pagination-summary|table-summary)|class=["'][^"']*(?:pagination-summary|result-window|page-summary)|(?:当前页|总数|第\s*(?:\{\{[^}]+\}\}|\d+)\s*页)|current\s+page|total\s+(?:rows|pages)|\bt\(['"][^'"]*(?:pagination|footer|table)\.[^'"]*(?:summary|resultWindow|currentPage|pageTotal|totalPages)['"]\)/i
const footerStatusPaginationPattern =
  /\b(pageInfo|currentPage|pageSize|totalPages|pageCount|resultWindow|paginationSummary|pageWindow)\b|当前页|总数|第\s*.*页|current\s+page|total\s+(?:rows|pages)/i
const obsoleteCopyPattern =
  /paginationSummary(?:Text)?|resultWindow(?:Text)?|data-testid=["'][^"']*(?:pagination-summary|table-summary)|sqlHistory\.(?:footer\.resultWindow|table\.summary)|执行记录说明|当前页\s*[/,，]|总数\s*[/,，]|第\s*(?:几|\{|\d).*页|current\s+page|total\s+pages|lorem ipsum/i

function runGit(args) {
  try {
    return execFileSync('git', args, { cwd: root, encoding: 'utf8' })
      .split(/\r?\n/)
      .map(item => item.trim())
      .filter(Boolean)
  } catch {
    return []
  }
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

function exists(relativePath) {
  return fs.existsSync(path.join(root, relativePath))
}

function unique(items) {
  return Array.from(new Set(items)).sort()
}

function changedFiles() {
  return unique([
    ...runGit(['diff', '--name-only']),
    ...runGit(['diff', '--cached', '--name-only']),
    ...runGit(['ls-files', '--others', '--exclude-standard'])
  ]).filter(item => frontendPathPattern.test(item) && exists(item))
}

function diffAddedLines(relativePath) {
  const outputs = []
  for (const args of [
    ['diff', '--unified=0', '--', relativePath],
    ['diff', '--cached', '--unified=0', '--', relativePath]
  ]) {
    try {
      outputs.push(execFileSync('git', args, { cwd: root, encoding: 'utf8' }))
    } catch {
      // `git diff` exits 0 for normal diffs, but keep this tolerant for unusual file states.
    }
  }

  if (runGit(['ls-files', '--others', '--exclude-standard', '--', relativePath]).includes(relativePath)) {
    return read(relativePath).split(/\r?\n/).map((line, index) => ({ line, sourceLine: index + 1 }))
  }

  const added = []
  for (const output of outputs) {
    let newLine = 0
    for (const rawLine of output.split(/\r?\n/)) {
      const hunk = rawLine.match(/^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@/)
      if (hunk) {
        newLine = Number(hunk[1])
        continue
      }
      if (rawLine.startsWith('+++') || rawLine.startsWith('---') || rawLine.startsWith('diff --git')) {
        continue
      }
      if (rawLine.startsWith('+')) {
        added.push({ line: rawLine.slice(1), sourceLine: newLine || 0 })
        newLine += 1
        continue
      }
      if (!rawLine.startsWith('-') && newLine > 0) {
        newLine += 1
      }
    }
  }
  return added
}

function lineRef(relativePath, lineNumber) {
  return lineNumber ? `${relativePath}:${lineNumber}` : relativePath
}

function countMatches(value, pattern) {
  return (value.match(pattern) || []).length
}

function extractTemplate(content) {
  const match = content.match(/<template[^>]*>([\s\S]*?)<\/template>/)
  return match ? match[1] : content
}

function hasVisibleText(value) {
  const normalized = value
    .replace(/\{\{[\s\S]*?\}\}/g, '')
    .replace(/&nbsp;|&#160;/g, '')
    .trim()
  return /[\u3400-\u9fff]/.test(normalized) || /[A-Za-z][A-Za-z ]{2,}/.test(normalized)
}

function visibleLiteralFromAttribute(line) {
  const matches = Array.from(
    line.matchAll(/\b(label|title|placeholder|empty-text|content|confirm-button-text|cancel-button-text)=["']([^"']+)["']/g)
  )
  return matches.find(([, , value]) => hasVisibleText(value))
}

function checkHardcodedText(relativePath, addedLines, errors) {
  for (const item of addedLines) {
    const line = item.line.trim()
    if (!line || line.startsWith('//') || line.startsWith('*') || line.startsWith('/*')) {
      continue
    }
    if (/data-testid=|aria-|class=|:class=|data-/.test(line)) {
      continue
    }

    const attr = visibleLiteralFromAttribute(line)
    if (attr) {
      errors.push(`${lineRef(relativePath, item.sourceLine)} has hardcoded visible ${attr[1]} text; use i18n.`)
    }

    if (/isChinese(?:\.value)?\s*\?/.test(line) && /['"][^'"]*[\u3400-\u9fffA-Za-z][^'"]*['"]/.test(line)) {
      errors.push(`${lineRef(relativePath, item.sourceLine)} adds locale ternary text; use i18n keys instead.`)
    }

    if (/>[^<{]*[\u3400-\u9fff][^<]*</.test(line)) {
      errors.push(`${lineRef(relativePath, item.sourceLine)} has hardcoded Chinese template text; use i18n.`)
    }

    if (/\b(errorMessage|emptyMessage|successMessage|title|summary|label|message)\w*\s*=\s*['"][^'"]*[\u3400-\u9fff][^'"]*['"]/.test(line)) {
      errors.push(`${lineRef(relativePath, item.sourceLine)} assigns hardcoded user-facing text; use i18n.`)
    }
  }
}

function checkObsoleteCopy(relativePath, addedLines, errors) {
  for (const item of addedLines) {
    const line = item.line.trim()
    if (!line || line.startsWith('//') || line.startsWith('*') || line.startsWith('/*')) {
      continue
    }
    if (obsoleteCopyPattern.test(line)) {
      errors.push(`${lineRef(relativePath, item.sourceLine)} adds obsolete pagination/table summary copy; remove the custom summary text.`)
    }
  }
}

function checkCardNesting(relativePath, content, errors) {
  let depth = 0
  const lines = content.split(/\r?\n/)
  lines.forEach((line, index) => {
    const tags = line.match(/<\/?el-card\b[^>]*>/g) || []
    for (const tag of tags) {
      if (tag.startsWith('</')) {
        depth = Math.max(0, depth - 1)
        continue
      }
      depth += 1
      if (depth > 1) {
        errors.push(`${relativePath}:${index + 1} nests <el-card>; use sections, tabs, drawers, or tables instead.`)
      }
      if (tag.endsWith('/>')) {
        depth = Math.max(0, depth - 1)
      }
    }
  })
}

function checkPageShell(relativePath, addedLines, errors) {
  if (!managementViewPagePattern.test(relativePath)) {
    return
  }
  const addedText = addedLines.map(item => item.line).join('\n')
  if (/(?:<PageHero\b|import\s+PageHero\b|from ['"][^'"]*PageHero\.vue['"])/.test(addedText)) {
    errors.push(`${relativePath} adds PageHero on a management page; use compact SectionHeader/page sections unless the route is an overview dashboard.`)
  }
}

function checkPaginationSummaryDuplication(relativePath, content, addedLines, errors) {
  const template = extractTemplate(content)
  if (!/<el-pagination\b/.test(template)) {
    return
  }

  const addedText = addedLines.map(item => item.line).join('\n')
  if (!/(?:<el-pagination\b|paginationSummary|resultWindow|pagination-summary|table-summary|currentPage|pageInfo|当前页|总数)/.test(addedText)) {
    return
  }

  if (duplicatePaginationSummaryPattern.test(template)) {
    errors.push(`${relativePath} mixes a custom pagination/result summary with Element Plus pagination; keep the single pagination component as the page-count source.`)
  }
}

function checkTableFooterSemantics(relativePath, content, addedLines, errors) {
  const template = extractTemplate(content)
  if (!/<el-pagination\b/.test(template) || !/(?:table-footer|footer-status|pagination-cluster)/.test(template)) {
    return
  }

  const addedText = addedLines.map(item => item.line).join('\n')
  if (!/(?:table-footer|footer-status|pagination|resultWindow|paginationSummary|pageInfo|currentPage|pageSize|total)/.test(addedText)) {
    return
  }

  const lines = template.split(/\r?\n/)
  lines.forEach((line, index) => {
    if (!/class=["'][^"']*footer-status/.test(line)) {
      return
    }

    const block = lines.slice(index, Math.min(lines.length, index + 10)).join('\n')
    if (footerStatusPaginationPattern.test(block)) {
      errors.push(`${relativePath}:${index + 1} mixes pagination totals into footer-status; keep status text separate from the pagination cluster.`)
    }
  })
}

function checkFilterDensity(relativePath, content, addedLines, errors) {
  const addedText = addedLines.map(item => item.line).join('\n')
  const addedControls = countMatches(addedText, filterControlPattern)
  if (addedControls < 4 || !/(?:filter|search|筛选|查询|tenant|datasource|schema|status)/i.test(addedText)) {
    return
  }

  if (!responsiveFilterPattern.test(content)) {
    errors.push(`${relativePath} adds a dense filter surface without a responsive grid/flex wrapping pattern.`)
  }
}

function checkManagementPatterns(relativePath, content, addedLines, errors) {
  const addedText = addedLines.map(item => item.line).join('\n')
  const addedTable = /<el-table\b/.test(addedText)
  const addedSelect = /<el-select\b/.test(addedText)
  const addedDialog = /<el-dialog\b/.test(addedText)

  if (!content.includes('useI18n')) {
    errors.push(`${relativePath} is a management frontend surface but does not use vue-i18n.`)
  }

  if (addedTable && !/<el-pagination\b/.test(content) && !/\b(pageInfo|pagination|pageNo|currentPage|pageSize)\b/.test(content)) {
    errors.push(`${relativePath} adds <el-table> without an explicit pagination/state pattern.`)
  }

  if (
    addedSelect &&
    !/useDict|formComponentGovernance|build[A-Z]\w*Options|withCurrentOption|getGovernanceDatasources|\w+Options/.test(content)
  ) {
    errors.push(`${relativePath} adds <el-select> without a shared dictionary/options source.`)
  }

  if (addedDialog && !/<el-dialog\b[^>]*v-model/.test(content)) {
    errors.push(`${relativePath} adds <el-dialog> without v-model visibility control.`)
  }

  if (addedDialog && /(?:Dialog|dialog).*(?:Form|form)|(?:Form|form).*(?:Dialog|dialog)/.test(content) && !/<el-form\b/.test(content)) {
    errors.push(`${relativePath} has dialog form state but no <el-form> validation surface.`)
  }

  checkPageShell(relativePath, addedLines, errors)
  checkPaginationSummaryDuplication(relativePath, content, addedLines, errors)
  checkTableFooterSemantics(relativePath, content, addedLines, errors)
  checkFilterDensity(relativePath, content, addedLines, errors)
}

function localeObject(relativePath) {
  const source = read(relativePath)
  const body = source.replace(/^\s*export\s+default\s+/, 'return ')
  return Function(body)()
}

function flattenKeys(value, prefix = '') {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return [prefix].filter(Boolean)
  }
  return Object.keys(value).flatMap(key => flattenKeys(value[key], prefix ? `${prefix}.${key}` : key))
}

function checkLocaleKeys(errors) {
  const zhPath = 'src/locales/zh-CN.js'
  const enPath = 'src/locales/en-US.js'
  if (!exists(zhPath) || !exists(enPath)) {
    errors.push('src/locales/zh-CN.js and src/locales/en-US.js must both exist.')
    return
  }

  let zhKeys
  let enKeys
  try {
    zhKeys = new Set(flattenKeys(localeObject(zhPath)))
    enKeys = new Set(flattenKeys(localeObject(enPath)))
  } catch (error) {
    errors.push(`Unable to parse locale files for key alignment: ${error.message}`)
    return
  }

  const missingInEn = Array.from(zhKeys).filter(key => !enKeys.has(key)).sort()
  const missingInZh = Array.from(enKeys).filter(key => !zhKeys.has(key)).sort()
  if (missingInEn.length > 0) {
    errors.push(`src/locales/en-US.js is missing keys:\n- ${missingInEn.join('\n- ')}`)
  }
  if (missingInZh.length > 0) {
    errors.push(`src/locales/zh-CN.js is missing keys:\n- ${missingInZh.join('\n- ')}`)
  }
}

function assertSelfTest(name, condition) {
  if (!condition) {
    throw new Error(`frontend governance self-test failed: ${name}`)
  }
}

function runSelfTest() {
  const managementHeroErrors = []
  checkPageShell(
    'src/views/system/SystemView.vue',
    [{ line: '<PageHero title="System" />', sourceLine: 3 }],
    managementHeroErrors
  )
  assertSelfTest('management PageHero is rejected', managementHeroErrors.length === 1)

  const overviewHeroErrors = []
  checkPageShell(
    'src/views/dashboard/DashboardView.vue',
    [{ line: '<PageHero title="Dashboard" />', sourceLine: 3 }],
    overviewHeroErrors
  )
  assertSelfTest('overview PageHero is allowed', overviewHeroErrors.length === 0)

  const duplicateSummaryErrors = []
  checkPaginationSummaryDuplication(
    'src/views/sql-history/SqlHistoryView.vue',
    '<template><div class="pagination-summary">{{ paginationSummaryText }}</div><el-pagination /></template>',
    [{ line: '<el-pagination />', sourceLine: 4 }],
    duplicateSummaryErrors
  )
  assertSelfTest('duplicate pagination summary is rejected', duplicateSummaryErrors.length === 1)

  const validFooterErrors = []
  checkTableFooterSemantics(
    'src/views/sql-history/SqlHistoryView.vue',
    '<template><div class="table-footer"><div class="footer-status">{{ paginationStateText }}</div><div class="pagination-cluster"><el-pagination /></div></div></template>',
    [{ line: '<div class="table-footer">', sourceLine: 4 }],
    validFooterErrors
  )
  assertSelfTest('separated footer status is allowed', validFooterErrors.length === 0)

  const mixedFooterErrors = []
  checkTableFooterSemantics(
    'src/views/sql-history/SqlHistoryView.vue',
    '<template><div class="table-footer"><div class="footer-status">{{ pageInfo.currentPage }} / {{ pageInfo.total }}</div><el-pagination /></div></template>',
    [{ line: '<div class="footer-status">{{ pageInfo.currentPage }} / {{ pageInfo.total }}</div>', sourceLine: 4 }],
    mixedFooterErrors
  )
  assertSelfTest('mixed footer status is rejected', mixedFooterErrors.length === 1)

  const filterDensityErrors = []
  checkFilterDensity(
    'src/views/system/SystemView.vue',
    '<template><section><el-input /><el-select /><el-date-picker /><el-input /></section></template>',
    [
      { line: '<el-input v-model="filterForm.tenantId" />', sourceLine: 4 },
      { line: '<el-select v-model="filterForm.status" />', sourceLine: 5 },
      { line: '<el-date-picker v-model="filterForm.queryDate" />', sourceLine: 6 },
      { line: '<el-input v-model="filterForm.datasourceCode" />', sourceLine: 7 }
    ],
    filterDensityErrors
  )
  assertSelfTest('low density filter is rejected', filterDensityErrors.length === 1)

  console.log('Frontend page governance self-test passed.')
}

if (process.argv.includes('--self-test')) {
  runSelfTest()
  process.exit(0)
}

const files = changedFiles()
const errors = []
const addedLinesByFile = new Map(files.map(file => [file, diffAddedLines(file)]))

if (files.some(file => localePathPattern.test(file))) {
  checkLocaleKeys(errors)
}

for (const relativePath of files) {
  checkObsoleteCopy(relativePath, addedLinesByFile.get(relativePath) || [], errors)
}

for (const relativePath of files.filter(file => vuePathPattern.test(file) && managementVuePathPattern.test(file))) {
  const content = read(relativePath)
  const addedLines = addedLinesByFile.get(relativePath) || []
  checkHardcodedText(relativePath, addedLines, errors)
  checkCardNesting(relativePath, content, errors)
  checkManagementPatterns(relativePath, content, addedLines, errors)
}

if (errors.length > 0) {
  console.error('Frontend page governance check failed.')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

if (files.length === 0) {
  console.log('Frontend page governance check passed: no changed frontend page, component, or locale files.')
} else {
  console.log(`Frontend page governance check passed (${files.length} changed frontend governance target(s)).`)
}
