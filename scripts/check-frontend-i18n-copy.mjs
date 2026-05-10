import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const root = path.resolve(path.dirname(__filename), '..')

const localePaths = {
  zh: 'src/locales/zh-CN.js',
  en: 'src/locales/en-US.js'
}

const allowedPureEnglish = new Set([
  'Schema',
  'Owner',
  'Trace ID',
  'Task ID',
  'Report ID',
  'History ID',
  'Result ID',
  'Source ID',
  'Policy ID',
  'SQL',
  'JDBC Agent',
  'SDK / Client',
  'RPO / RTO',
  'EN'
])

const allowedUppercaseOrCodePattern =
  /^(?:R-\d+|[A-Z][A-Z0-9_/-]*(?:\s*\/\s*[A-Z][A-Z0-9_/-]*)*|query_date)$/
const allowedTechnicalWords = new Set([
  'SQLForge',
  'SQL',
  'JSON',
  'API',
  'HTTP',
  'HTTPS',
  'JDBC',
  'SDK',
  'MySQL',
  'Redis',
  'Kafka',
  'HETU',
  'HIVE',
  'Java',
  'Spring',
  'Boot',
  'MyBatis',
  'XML',
  'DTO',
  'C4',
  'ADR',
  'BI',
  'AI',
  'KPI',
  'SLA',
  'RPO',
  'RTO',
  'CRUD',
  'ACK',
  'UP',
  'Phase',
  'Codex',
  'Vue',
  'Element',
  'Plus',
  'Vite',
  'Pinia',
  'Element',
  'Plus',
  'Playwright',
  'Foreman',
  'Sonar',
  'SonarQube',
  'Hetu',
  'MRS',
  'access',
  'audit',
  'backlog',
  'blocked',
  'commit',
  'controller',
  'cursor',
  'datasourceCode',
  'dispatch',
  'domain',
  'event',
  'fail',
  'failed',
  'fallback',
  'foreman',
  'git',
  'governance',
  'history',
  'infrastructure',
  'lint',
  'logical',
  'lookup',
  'migration',
  'parse',
  'partial',
  'pending',
  'report',
  'retry',
  'schema',
  'service',
  'smoke',
  'statistics',
  'structure',
  'subject',
  'success',
  'task',
  'trace',
  'trivial',
  'unavailable',
  'view',
  'workflow',
  'workflow_dispatch'
])

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8')
}

function localeObject(relativePath) {
  const source = read(relativePath)
  const body = source.replace(/^\s*export\s+default\s+/, 'return ')
  return Function(body)()
}

function flattenLeaves(value, prefix = '') {
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => flattenLeaves(item, `${prefix}[${index}]`))
  }
  if (value && typeof value === 'object') {
    return Object.keys(value).flatMap(key => flattenLeaves(value[key], prefix ? `${prefix}.${key}` : key))
  }
  return [{ key: prefix, value: String(value ?? '') }]
}

function normalizeTextForEnglishCheck(value) {
  return value
    .replace(/`[^`]*`/g, ' ')
    .replace(/\{[^}]*\}/g, ' ')
    .replace(/\/[A-Za-z0-9_./:-]+/g, ' ')
    .replace(/[A-Za-z]+(?:[A-Z][a-z0-9]+)+/g, ' ')
    .replace(/[A-Za-z0-9]+(?:[_-][A-Za-z0-9]+)+/g, ' ')
    .replace(/\b[A-Z]{2,}[A-Z0-9_/-]*\b/g, ' ')
    .replace(/\bR-\d+\b/g, ' ')
    .replace(/\bPhase-[A-Z0-9+]+\b/g, ' ')
}

function hasSuspiciousEnglishPhrase(value) {
  const normalized = normalizeTextForEnglishCheck(value)
  const words = normalized.match(/[A-Za-z]{3,}/g) || []
  const nonTechnicalWords = words.filter(word => !allowedTechnicalWords.has(word))
  return nonTechnicalWords.length >= 2
}

function checkLocaleKeyAlignment(zhLeaves, enLeaves, errors) {
  const zhKeys = new Set(zhLeaves.map(item => item.key))
  const enKeys = new Set(enLeaves.map(item => item.key))
  const missingInEn = Array.from(zhKeys).filter(key => !enKeys.has(key)).sort()
  const missingInZh = Array.from(enKeys).filter(key => !zhKeys.has(key)).sort()
  if (missingInEn.length > 0) {
    errors.push(`src/locales/en-US.js is missing keys:\n- ${missingInEn.join('\n- ')}`)
  }
  if (missingInZh.length > 0) {
    errors.push(`src/locales/zh-CN.js is missing keys:\n- ${missingInZh.join('\n- ')}`)
  }
}

function checkChineseCopy(zhLeaves, errors) {
  for (const { key, value } of zhLeaves) {
    if (!/[A-Za-z]/.test(value)) {
      continue
    }
    if (allowedPureEnglish.has(value) || allowedUppercaseOrCodePattern.test(value)) {
      continue
    }
    if (!/[\u3400-\u9fff]/.test(value)) {
      errors.push(`${localePaths.zh}:${key} is pure English in zh-CN: ${value}`)
      continue
    }
    if (hasSuspiciousEnglishPhrase(value)) {
      errors.push(`${localePaths.zh}:${key} has an English prose fragment in zh-CN: ${value}`)
    }
  }
}

function checkSourceChineseBranches(errors) {
  const viewFiles = fs
    .readdirSync(path.join(root, 'src/views'), { recursive: true })
    .filter(file => /\.(?:vue|js|mjs)$/.test(file))
    .map(file => `src/views/${file}`)
  const configFiles = fs
    .readdirSync(path.join(root, 'src/config'), { recursive: true })
    .filter(file => /\.(?:js|mjs)$/.test(file))
    .map(file => `src/config/${file}`)
  const sourceFiles = viewFiles
    .concat(configFiles)
    .concat(['src/App.vue'])
    .filter(file => fs.existsSync(path.join(root, file)))

  const chineseBranchPattern =
    /(?:isChinese(?:\.value)?|locale(?:\.value)?\s*===\s*'zh-CN')\s*\?\s*'([^']*[A-Za-z][^']*)'\s*:/g

  for (const relativePath of sourceFiles) {
    const source = read(relativePath)
    for (const match of source.matchAll(chineseBranchPattern)) {
      const value = match[1]
      if (allowedPureEnglish.has(value) || allowedUppercaseOrCodePattern.test(value)) {
        continue
      }
      if (!/[\u3400-\u9fff]/.test(value) && hasSuspiciousEnglishPhrase(value)) {
        errors.push(`${relativePath} keeps English copy in the zh-CN branch: ${value}`)
      }
    }
  }
}

function checkBypassedLocaleObjects(errors) {
  const sourceFiles = ['src/App.vue', 'src/config/routePaths.mjs']
  const bypassPattern = /\{\s*zh:\s*['"][^'"]+['"]\s*,\s*en:\s*['"][^'"]+['"]\s*\}/g
  for (const relativePath of sourceFiles) {
    const source = read(relativePath)
    for (const match of source.matchAll(bypassPattern)) {
      errors.push(`${relativePath} bypasses locale files with a zh/en object: ${match[0]}`)
    }
  }
}

const errors = []
const zhLeaves = flattenLeaves(localeObject(localePaths.zh))
const enLeaves = flattenLeaves(localeObject(localePaths.en))

checkLocaleKeyAlignment(zhLeaves, enLeaves, errors)
checkChineseCopy(zhLeaves, errors)
checkSourceChineseBranches(errors)
checkBypassedLocaleObjects(errors)

if (errors.length > 0) {
  console.error('Frontend i18n copy check failed.')
  for (const error of errors) {
    console.error(`- ${error}`)
  }
  process.exit(1)
}

console.log('Frontend i18n copy check passed.')
