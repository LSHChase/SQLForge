#!/usr/bin/env node

import { execFileSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const root = path.resolve(path.dirname(__filename), '..')
const legacyScriptOutputBaselineRelativePath = 'docs/quality/developer-copy-language-script-legacy-baseline.json'
const legacyScriptOutputBaselinePath = path.join(root, legacyScriptOutputBaselineRelativePath)

const mode = process.argv.includes('--all') ? 'all' : process.argv.includes('--changed') ? 'changed' : ''

if (!mode) {
  console.error('用法：node scripts/check-developer-copy-language.mjs --changed|--all')
  process.exit(2)
}

const skipPathParts = new Set([
  '.git',
  '.idea',
  '.vscode',
  'node_modules',
  'target',
  'dist',
  'dist-portable',
  'build',
  'coverage',
  'vendor',
  'third_party',
  'third-party',
  'generated'
])

const exactAllowedEnglish = new Set([
  'OK',
  'N/A',
  'SQL',
  'JSON',
  'XML',
  'YAML',
  'HTTP',
  'HTTPS',
  'JDBC',
  'SDK',
  'API',
  'DTO',
  'VO',
  'ID',
  'URL',
  'URI',
  'MCP',
  'CI',
  'CD',
  'CI/CD',
  'JDK 8u112',
  'UTF-8',
  'LF'
])

const allowedTechnicalWords = new Set(
  [
    'access',
    'ack',
    'adr',
    'aes',
    'agent',
    'ai',
    'api',
    'arg',
    'args',
    'audit',
    'auto',
    'autoplan',
    'backend',
    'batch',
    'benchmark',
    'binding',
    'bi',
    'bootstrap',
    'browser',
    'cache',
    'catalog',
    'check',
    'checksum',
    'cipher',
    'ciphertext',
    'ci',
    'cli',
    'client',
    'closed',
    'closeout',
    'codex',
    'column',
    'columns',
    'command',
    'comment',
    'commit',
    'config',
    'connector',
    'console',
    'context',
    'contract',
    'controller',
    'csv',
    'cursor',
    'dashboard',
    'data',
    'database',
    'datasource',
    'ddl',
    'debug',
    'dev',
    'diff',
    'digest',
    'dispatch',
    'docker',
    'docs',
    'domain',
    'dto',
    'enum',
    'error',
    'errors',
    'evidence',
    'exception',
    'export',
    'fail',
    'failed',
    'fallback',
    'false',
    'fixture',
    'foreman',
    'frontend',
    'git',
    'github',
    'governance',
    'header',
    'health',
    'hetu',
    'history',
    'hive',
    'hudi',
    'html',
    'http',
    'https',
    'id',
    'ids',
    'index',
    'indexes',
    'infra',
    'infrastructure',
    'inbox',
    'java',
    'javascript',
    'jdbc',
    'jdk',
    'json',
    'kafka',
    'key',
    'keys',
    'kpi',
    'label',
    'lint',
    'local',
    'log',
    'main',
    'manifest',
    'mapper',
    'maven',
    'mcp',
    'metadata',
    'minio',
    'mode',
    'module',
    'mrs',
    'msg',
    'mysql',
    'mybatis',
    'node',
    'noop',
    'ops',
    'page',
    'parse',
    'parser',
    'partial',
    'passed',
    'payload',
    'pdf',
    'pending',
    'pinia',
    'plan',
    'playwright',
    'policy',
    'pom',
    'preview',
    'prod',
    'profile',
    'protocol',
    'query',
    'ready',
    'redis',
    'repo',
    'report',
    'request',
    'response',
    'result',
    'retry',
    'rewrite',
    'route',
    'runtime',
    'schema',
    'sdk',
    'service',
    'shell',
    'smoke',
    'spring',
    'sql',
    'sqlforge',
    'state',
    'status',
    'summary',
    'task',
    'tenant',
    'test',
    'tests',
    'tdsql',
    'token',
    'trace',
    'true',
    'ui',
    'url',
    'usage',
    'user',
    'uuid',
    'validate',
    'validator',
    'version',
    'vite',
    'vo',
    'vue',
    'warn',
    'warning',
    'worker',
    'workflow',
    'worktree',
    'xml',
    'yaml',
    'yml'
  ].map(item => item.toLowerCase())
)

const pureEnglishAllowPatterns = [
  /^[-\w./]+:\s*\$\{?[A-Z0-9_]+\}?$/,
  /^\[[a-z-]+\]\s+/,
  /^[A-Z_]+$/,
  /^[a-z][a-z0-9_-]*$/,
  /^[-\w./]+$/,
  /^https?:\/\//,
  /^SELECT\b/i,
  /^INSERT\b/i,
  /^UPDATE\b/i,
  /^DELETE\b/i,
  /^CREATE\b/i,
  /^ALTER\b/i
]

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

function runGitText(args) {
  try {
    return execFileSync('git', args, { cwd: root, encoding: 'utf8' })
  } catch {
    return ''
  }
}

function unique(items) {
  return Array.from(new Set(items)).sort()
}

function relativeFromRoot(absolutePath) {
  return path.relative(root, absolutePath).replace(/\\/g, '/')
}

function exists(relativePath) {
  return fs.existsSync(path.join(root, relativePath))
}

function shouldSkip(relativePath) {
  if (relativePath === 'src/locales/en-US.js') {
    return true
  }
  const parts = relativePath.split('/')
  return parts.some(part => skipPathParts.has(part)) || relativePath.startsWith('docs/generated/')
}

function classify(relativePath) {
  if (shouldSkip(relativePath)) {
    return ''
  }
  if (/\/src\/(?:main|test)\/java\/.*\.java$/.test(`/${relativePath}`)) {
    return 'java'
  }
  if (/^sql\/.*\.sql$/.test(relativePath)) {
    return 'sql'
  }
  if (/^scripts\/.*\.(?:py|sh|mjs|js)$/.test(relativePath)) {
    return 'script'
  }
  return ''
}

function walk(directory) {
  const files = []
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name)
    const relative = relativeFromRoot(absolute)
    if (shouldSkip(relative)) {
      continue
    }
    if (entry.isDirectory()) {
      files.push(...walk(absolute))
    } else {
      files.push(relative)
    }
  }
  return files
}

function targetFiles() {
  if (mode === 'changed') {
    return unique([
      ...runGit(['diff', '--name-only']),
      ...runGit(['diff', '--cached', '--name-only']),
      ...runGit(['ls-files', '--others', '--exclude-standard'])
    ]).filter(relativePath => exists(relativePath) && classify(relativePath))
  }
  return walk(root).filter(relativePath => classify(relativePath))
}

const changedLineNumbersByFile = new Map()

function changedLineNumbers(relativePath) {
  if (mode !== 'changed') {
    return null
  }
  if (changedLineNumbersByFile.has(relativePath)) {
    return changedLineNumbersByFile.get(relativePath)
  }
  const untracked = runGit(['ls-files', '--others', '--exclude-standard', '--', relativePath]).includes(relativePath)
  if (untracked) {
    const content = fs.readFileSync(path.join(root, relativePath), 'utf8')
    const allLines = new Set(content.split(/\r?\n/).map((_, index) => index + 1))
    changedLineNumbersByFile.set(relativePath, allLines)
    return allLines
  }

  const added = new Set()
  for (const args of [
    ['diff', '--unified=0', '--', relativePath],
    ['diff', '--cached', '--unified=0', '--', relativePath]
  ]) {
    const output = runGitText(args)
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
        if (newLine > 0) {
          added.add(newLine)
        }
        newLine += 1
        continue
      }
      if (!rawLine.startsWith('-') && newLine > 0) {
        newLine += 1
      }
    }
  }
  changedLineNumbersByFile.set(relativePath, added)
  return added
}

function lineInScope(relativePath, lineNumber) {
  if (mode === 'all') {
    return true
  }
  const lines = changedLineNumbers(relativePath)
  return lines && lines.has(lineNumber)
}

function hasChinese(value) {
  return /[\u3400-\u9fff]/.test(value)
}

function removeQuotedCode(value) {
  return value
    .replace(/`[^`]*`/g, ' ')
    .replace(/'[^']*'/g, match => (/[A-Za-z ]{3,}/.test(match) ? match : ' '))
    .replace(/"[^"]*"/g, match => (/[A-Za-z ]{3,}/.test(match) ? match : ' '))
}

function normalizeForWordScan(value) {
  return removeQuotedCode(value)
    .replace(/\\[nrt]/g, ' ')
    .replace(/https?:\/\/\S+/g, ' ')
    .replace(/[A-Za-z]:\\[^\s]+/g, ' ')
    .replace(/\/[A-Za-z0-9_.:-]+(?:\/[A-Za-z0-9_.:-]+)+/g, ' ')
    .replace(/\$\{[^}]*\}/g, ' ')
    .replace(/\{[^}]*\}/g, ' ')
    .replace(/%[-+#0-9.]*[A-Za-z]/g, ' ')
    .replace(/\bR-\d+\b/g, ' ')
    .replace(/\bHARN-\d+\b/g, ' ')
    .replace(/\b[A-Z][A-Z0-9_/-]{1,}\b/g, ' ')
    .replace(/\b[A-Za-z]+(?:[A-Z][a-z0-9]+)+\b/g, ' ')
    .replace(/\b[A-Za-z][A-Za-z0-9]*(?:[_-][A-Za-z0-9]+)+\b/g, ' ')
    .replace(/\b(?:com|org|net|io|src|docs|scripts)\.[A-Za-z0-9_.]+\b/g, ' ')
}

function nonTechnicalWords(value) {
  const words = normalizeForWordScan(value).match(/[A-Za-z]{3,}/g) || []
  return words.filter(word => !allowedTechnicalWords.has(word.toLowerCase()))
}

function isAllowedPureEnglish(value, options = {}) {
  const trimmed = value.trim()
  if (!trimmed || exactAllowedEnglish.has(trimmed)) {
    return true
  }
  if (pureEnglishAllowPatterns.some(pattern => pattern.test(trimmed))) {
    return true
  }
  if (/^[A-Z0-9_./:-]+$/.test(trimmed)) {
    return true
  }
  if (/^[a-zA-Z_$][\w$]*(?:\.[a-zA-Z_$][\w$]*)*$/.test(trimmed)) {
    return true
  }
  const allowLooseIdentifierPhrase = options.allowLooseIdentifierPhrase !== false
  if (allowLooseIdentifierPhrase && /^[./\w:-]+(?:\s+[./\w:=-]+)*$/.test(trimmed) && !/[.!?]$/.test(trimmed)) {
    return true
  }
  return false
}

function needsChinese(value, threshold, options = {}) {
  const trimmed = value.trim()
  if (!trimmed || hasChinese(trimmed) || isAllowedPureEnglish(trimmed, options)) {
    return false
  }
  return nonTechnicalWords(trimmed).length >= threshold
}

function addIssue(errors, relativePath, lineNumber, kind, value, words, category = kind) {
  if (!lineInScope(relativePath, lineNumber)) {
    return
  }
  errors.push({
    relativePath,
    lineNumber,
    kind,
    value: value.trim(),
    words,
    category
  })
}

function formatIssue(issue) {
  const suffix = issue.words.length > 0 ? `；英文词：${issue.words.slice(0, 6).join(', ')}` : ''
  return `${issue.relativePath}:${issue.lineNumber} ${issue.kind} 应使用中文或进入 allowlist：${issue.value}${suffix}`
}

function extractStringLiterals(line) {
  return extractStringLiteralsWithOffsets(line).map(item => item.value)
}

function extractStringLiteralsWithOffsets(line) {
  const literals = []
  const pattern = /(["'`])((?:\\.|(?!\1)[\s\S])*?)\1/g
  for (const match of line.matchAll(pattern)) {
    literals.push({ value: match[2], index: match.index })
  }
  return literals
}

function splitTopLevelArgumentRanges(value) {
  const args = []
  let current = ''
  let argumentStart = 0
  let depth = 0
  let quote = ''
  let escaped = false
  for (let index = 0; index < value.length; index += 1) {
    const char = value[index]
    if (quote) {
      current += char
      if (escaped) {
        escaped = false
      } else if (char === '\\') {
        escaped = true
      } else if (char === quote) {
        quote = ''
      }
      continue
    }
    if (char === '"' || char === "'" || char === '`') {
      quote = char
      current += char
      continue
    }
    if (char === '(' || char === '[' || char === '{') {
      depth += 1
      current += char
      continue
    }
    if (char === ')' || char === ']' || char === '}') {
      depth = Math.max(0, depth - 1)
      current += char
      continue
    }
    if (char === ',' && depth === 0) {
      if (current.trim()) {
        args.push({ raw: current, start: argumentStart })
      }
      current = ''
      argumentStart = index + 1
      continue
    }
    current += char
  }
  if (current.trim()) {
    args.push({ raw: current, start: argumentStart })
  }
  return args
}

function lineNumberForOffset(value, startLineNumber, offset) {
  const prefix = value.slice(0, offset)
  return startLineNumber + (prefix.match(/\n/g) || []).length
}

const assertionMinimumArgumentsBeforeMessage = new Map([
  ['assertTrue', 1],
  ['assertFalse', 1],
  ['assertNull', 1],
  ['assertNotNull', 1],
  ['assertEquals', 2],
  ['assertNotEquals', 2],
  ['assertSame', 2],
  ['assertNotSame', 2],
  ['assertArrayEquals', 2],
  ['assertIterableEquals', 2],
  ['assertLinesMatch', 2],
  ['assertThrows', 2],
  ['assertDoesNotThrow', 1]
])

function checkJavaAssertionMessage(relativePath, lineNumber, statement, errors) {
  const match = statement.match(/\b(assertTrue|assertFalse|assertNull|assertNotNull|assertEquals|assertNotEquals|assertSame|assertNotSame|assertArrayEquals|assertIterableEquals|assertLinesMatch|assertThrows|assertDoesNotThrow)\s*\(/)
  if (!match) {
    return
  }
  const openIndex = statement.indexOf('(', match.index)
  const closeIndex = statement.lastIndexOf(')')
  if (openIndex === -1 || closeIndex <= openIndex) {
    return
  }
  const argumentText = statement.slice(openIndex + 1, closeIndex)
  const args = splitTopLevelArgumentRanges(argumentText)
  const minimum = assertionMinimumArgumentsBeforeMessage.get(match[1])
  if (!minimum || args.length <= minimum) {
    return
  }
  const messageArgument = args[args.length - 1]
  for (const literal of extractStringLiteralsWithOffsets(messageArgument.raw)) {
    const literalLineNumber = lineNumberForOffset(
      statement,
      lineNumber,
      openIndex + 1 + messageArgument.start + literal.index
    )
    if (needsChinese(literal.value, 2, { allowLooseIdentifierPhrase: false })) {
      addIssue(
        errors,
        relativePath,
        literalLineNumber,
        'Java 断言失败消息',
        literal.value,
        nonTechnicalWords(literal.value),
        'java-assertion-message'
      )
    }
  }
}

function countChar(line, char) {
  return (line.match(new RegExp(`\\${char}`, 'g')) || []).length
}

function stripCommentPrefix(line) {
  return line
    .replace(/^\s*\/\*\*?/, '')
    .replace(/\*\/\s*$/, '')
    .replace(/^\s*\*/, '')
    .replace(/^\s*\/\//, '')
    .replace(/^\s*#/, '')
    .trim()
}

function isIgnorableComment(value) {
  return (
    !value ||
    value.startsWith('!/') ||
    value.startsWith('/usr/bin/env') ||
    value.startsWith('shellcheck') ||
    value.startsWith('eslint') ||
    value.startsWith('prettier') ||
    value.startsWith('noinspection') ||
    value.startsWith('@') ||
    /^TODO\b|^FIXME\b/.test(value)
  )
}

function checkComment(relativePath, lineNumber, comment, errors, threshold = 5) {
  const value = stripCommentPrefix(comment)
  if (isIgnorableComment(value)) {
    return
  }
  if (needsChinese(value, threshold)) {
    addIssue(errors, relativePath, lineNumber, '注释', value, nonTechnicalWords(value))
  }
}

function scanJava(relativePath, content, errors) {
  const lines = content.split(/\r?\n/)
  let inBlockComment = false
  let activeHumanStringContext = false
  let activeAssertionStatement = null
  let parenDepth = 0

  lines.forEach((line, index) => {
    const lineNumber = index + 1
    const trimmed = line.trim()

    if (inBlockComment) {
      checkComment(relativePath, lineNumber, trimmed, errors)
      if (trimmed.includes('*/')) {
        inBlockComment = false
      }
      return
    }
    if (trimmed.startsWith('/*')) {
      checkComment(relativePath, lineNumber, trimmed, errors)
      if (!trimmed.includes('*/')) {
        inBlockComment = true
      }
      return
    }
    if (trimmed.startsWith('//')) {
      checkComment(relativePath, lineNumber, trimmed, errors)
      return
    }

    if (activeAssertionStatement) {
      activeAssertionStatement.statement += `\n${line}`
      activeAssertionStatement.depth += countChar(line, '(') - countChar(line, ')')
      if (activeAssertionStatement.depth <= 0) {
        checkJavaAssertionMessage(
          relativePath,
          activeAssertionStatement.startLineNumber,
          activeAssertionStatement.statement,
          errors
        )
        activeAssertionStatement = null
      }
    } else if (/\b(assertTrue|assertFalse|assertNull|assertNotNull|assertEquals|assertNotEquals|assertSame|assertNotSame|assertArrayEquals|assertIterableEquals|assertLinesMatch|assertThrows|assertDoesNotThrow)\s*\(/.test(line)) {
      activeAssertionStatement = {
        startLineNumber: lineNumber,
        statement: line,
        depth: countChar(line, '(') - countChar(line, ')')
      }
      if (activeAssertionStatement.depth <= 0) {
        checkJavaAssertionMessage(relativePath, lineNumber, line, errors)
        activeAssertionStatement = null
      }
    }

    const contextStarted =
      /\b(?:LOGGER|log|logger)\s*\.\s*(?:trace|debug|info|warn|error)\s*\(/.test(line) ||
      /\bthrow\s+new\s+\w*(?:Exception|Error)\s*\(/.test(line) ||
      /\bnew\s+\w*(?:Exception|Error)\s*\(/.test(line) ||
      /\bfail\s*\(/.test(line) ||
      /\bnew\s+AssertionError\s*\(/.test(line) ||
      /\bmessage\s*=\s*"/.test(line) ||
      /\b(?:setMessage|setMsg)\s*\(/.test(line) ||
      /\.(?:message|msg)\s*\(/.test(line) ||
      /\bput\s*\(\s*"msg"\s*,/.test(line) ||
      /\bput\s*\(\s*"message"\s*,/.test(line)

    if (contextStarted) {
      activeHumanStringContext = true
      parenDepth = 0
    }

    if (activeHumanStringContext) {
      for (const literal of extractStringLiterals(line)) {
        if (needsChinese(literal, 2)) {
          addIssue(errors, relativePath, lineNumber, 'Java 可读字符串', literal, nonTechnicalWords(literal), 'java-human-string')
        }
      }
      parenDepth += countChar(line, '(') - countChar(line, ')')
      if (parenDepth <= 0 && /[);]\s*$/.test(trimmed)) {
        activeHumanStringContext = false
      }
    }
  })
}

function scanSqlComments(relativePath, content, errors) {
  const lines = content.split(/\r?\n/)
  lines.forEach((line, index) => {
    for (const match of line.matchAll(/\bCOMMENT\s+'((?:''|[^'])*)'/gi)) {
      const value = match[1].replace(/''/g, "'")
      if (needsChinese(value, 2)) {
        addIssue(errors, relativePath, index + 1, 'SQL COMMENT', value, nonTechnicalWords(value), 'sql-comment')
      }
    }
  })
}

function isScriptHumanStringLine(line) {
  return (
    /\b(?:description|help|epilog)\s*=/.test(line) ||
    /\b(?:print|fail|raise\s+SystemExit)\s*\(/.test(line) ||
    /\bconsole\.(?:log|error|warn)\s*\(/.test(line) ||
    /\bthrow\s+new\s+Error\s*\(/.test(line) ||
    /^\s*echo\s+/.test(line)
  )
}

function scanScript(relativePath, content, errors) {
  const lines = content.split(/\r?\n/)
  let inBlockComment = false
  lines.forEach((line, index) => {
    const lineNumber = index + 1
    const trimmed = line.trim()

    if (inBlockComment) {
      checkComment(relativePath, lineNumber, trimmed, errors, 6)
      if (trimmed.includes('*/')) {
        inBlockComment = false
      }
      return
    }
    if (trimmed.startsWith('/*')) {
      checkComment(relativePath, lineNumber, trimmed, errors, 6)
      if (!trimmed.includes('*/')) {
        inBlockComment = true
      }
      return
    }
    if (trimmed.startsWith('#') && !trimmed.startsWith('#!')) {
      checkComment(relativePath, lineNumber, trimmed, errors, 6)
    }
    if (trimmed.startsWith('//')) {
      checkComment(relativePath, lineNumber, trimmed, errors, 6)
    }

    for (const match of line.matchAll(/\bCOMMENT\s+'((?:''|[^'])*)'/gi)) {
      const value = match[1].replace(/''/g, "'")
      if (needsChinese(value, 2)) {
        addIssue(errors, relativePath, lineNumber, '脚本内 SQL COMMENT', value, nonTechnicalWords(value), 'script-sql-comment')
      }
    }

    if (isScriptHumanStringLine(line)) {
      for (const literal of extractStringLiterals(line)) {
        if (needsChinese(literal, 3, { allowLooseIdentifierPhrase: false })) {
          addIssue(
            errors,
            relativePath,
            lineNumber,
            '脚本可读字符串',
            literal,
            nonTechnicalWords(literal),
            'script-human-string'
          )
        }
      }
    }
  })
}

function scanFile(relativePath, errors) {
  const content = fs.readFileSync(path.join(root, relativePath), 'utf8')
  const type = classify(relativePath)
  if (type === 'java') {
    scanJava(relativePath, content, errors)
  } else if (type === 'sql') {
    scanSqlComments(relativePath, content, errors)
  } else if (type === 'script') {
    scanScript(relativePath, content, errors)
  }
}

function loadLegacyScriptOutputBaseline() {
  if (!fs.existsSync(legacyScriptOutputBaselinePath)) {
    return { maxIssuesByFile: {} }
  }
  return JSON.parse(fs.readFileSync(legacyScriptOutputBaselinePath, 'utf8'))
}

function isLegacyScriptOutputIssue(issue) {
  return issue.category === 'script-human-string'
}

function countLegacyScriptOutputIssues(issues) {
  const counts = new Map()
  for (const issue of issues.filter(isLegacyScriptOutputIssue)) {
    counts.set(issue.relativePath, (counts.get(issue.relativePath) || 0) + 1)
  }
  return counts
}

function validateLegacyScriptOutputDebt(issues, errors) {
  if (mode !== 'all') {
    return { issueCount: 0, fileCount: 0 }
  }
  const baseline = loadLegacyScriptOutputBaseline()
  const allowedByFile = baseline.maxIssuesByFile || {}
  const counts = countLegacyScriptOutputIssues(issues)
  for (const [relativePath, count] of counts.entries()) {
    const allowed = allowedByFile[relativePath] || 0
    if (count > allowed) {
      errors.push(
        `${relativePath} 新增或超出脚本英文 help/error/print/echo 存量：当前 ${count} 条，baseline 允许 ${allowed} 条。`
      )
    }
  }
  return {
    issueCount: Array.from(counts.values()).reduce((sum, count) => sum + count, 0),
    fileCount: counts.size
  }
}

const files = targetFiles()
const issues = []

files.forEach(relativePath => scanFile(relativePath, issues))

const validationErrors = []
const legacyScriptOutputDebt = validateLegacyScriptOutputDebt(issues, validationErrors)
const blockingIssues = issues.filter(issue => !(mode === 'all' && isLegacyScriptOutputIssue(issue)))

if (validationErrors.length > 0 || blockingIssues.length > 0) {
  console.error('开发者可读文本中文化检查失败：')
  validationErrors.forEach(error => console.error(`- ${error}`))
  blockingIssues.forEach(issue => console.error(`- ${formatIssue(issue)}`))
  process.exit(1)
}

if (mode === 'all' && legacyScriptOutputDebt.issueCount > 0) {
  console.log(
    `脚本英文 help/error/print/echo 历史存量已按 ${legacyScriptOutputBaselineRelativePath} 登记（${legacyScriptOutputDebt.fileCount} 个文件，${legacyScriptOutputDebt.issueCount} 条）`
  )
}
console.log(`开发者可读文本中文化检查通过（${mode}，${files.length} 个文件）`)
