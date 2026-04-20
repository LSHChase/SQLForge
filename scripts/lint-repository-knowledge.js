#!/usr/bin/env node

const fs = require('fs')
const path = require('path')

const rootDir = process.cwd()

const requiredDocsPaths = [
  'docs',
  'docs/README.md',
  'docs/architecture',
  'docs/architecture/init.md',
  'docs/architecture/messaging-abstraction.md',
  'docs/quality',
  'docs/quality/alibaba-java-guidelines.md',
  'docs/quality/frontend-backend-separation-baseline.md',
  'docs/quality/validation-rules.md',
  'docs/rules',
  'docs/rules/codex-rules.md',
  'docs/adr',
  'docs/adr/README.md',
  'docs/adr/adr-template.md',
  'docs/references',
  'docs/references/human-constraint-history.md',
  'docs/references/raw-requirements',
  'docs/security',
  'docs/security/compliance.md',
  'docs/plans',
  'docs/plans/phase-0-plan.md'
]

const requiredReadmeMarkers = [
  'docs/README.md',
  'docs/architecture/init.md',
  'docs/rules/codex-rules.md',
  'docs/quality/alibaba-java-guidelines.md',
  'docs/adr/README.md',
  'docs/security/compliance.md',
  'docs/plans/phase-0-plan.md'
]

const expectedRuleEnd = 155
const expectedValidationIndexRanges = [
  [116, 144],
  [151, 154]
]
const requiredMessagingConfigs = [
  'governance-service/src/main/resources/application-dev.yml',
  'governance-service/src/main/resources/application-test.yml',
  'governance-service/src/main/resources/application-prod.yml',
  'governance-service/src/test/resources/application-test.yml'
]

function formatRuleId(id) {
  return `R-${String(id).padStart(3, '0')}`
}

function absolutePath(relativePath) {
  return path.join(rootDir, relativePath)
}

function pathExists(relativePath) {
  return fs.existsSync(absolutePath(relativePath))
}

function readFile(relativePath) {
  return fs.readFileSync(absolutePath(relativePath), 'utf8')
}

function ensureDocsStructure(errors, checks) {
  const missing = requiredDocsPaths.filter(item => !pathExists(item))
  if (missing.length > 0) {
    errors.push(`Missing required docs paths:\n- ${missing.join('\n- ')}`)
    return
  }

  checks.push(`docs structure ok (${requiredDocsPaths.length} paths)`)
}

function ensureAlibabaGuidelineArchive(errors, checks) {
  const archiveDir = 'docs/references/raw-requirements/alibaba-java-guidelines'
  const requiredArchiveFiles = [
    `${archiveDir}/Java开发手册(黄山版).pdf`,
    `${archiveDir}/README.snapshot.md`,
    `${archiveDir}/source-metadata.md`
  ]
  const missing = requiredArchiveFiles.filter(item => !pathExists(item))
  if (missing.length > 0) {
    errors.push(`Missing Alibaba guideline archive files:\n- ${missing.join('\n- ')}`)
    return
  }

  checks.push(`Alibaba guideline archive ok (${requiredArchiveFiles.length} files)`)
}

function ensureAdrTemplate(errors, checks) {
  const adrTemplatePath = 'docs/adr/adr-template.md'
  if (!pathExists(adrTemplatePath)) {
    errors.push(`Missing ADR template: ${adrTemplatePath}`)
    return
  }

  const content = readFile(adrTemplatePath)
  const requiredSections = [
    '## Context',
    '## Decision',
    '## Consequences',
    '## Compliance Impact'
  ]
  const missingSections = requiredSections.filter(section => !content.includes(section))
  if (missingSections.length > 0) {
    errors.push(`ADR template missing sections:\n- ${missingSections.join('\n- ')}`)
    return
  }

  checks.push('adr-template.md ok')
}

function ensureRuleContinuity(errors, checks) {
  const rulesPath = 'docs/rules/codex-rules.md'
  if (!pathExists(rulesPath)) {
    errors.push(`Missing rules file: ${rulesPath}`)
    return
  }

  const content = readFile(rulesPath)
  const matches = Array.from(content.matchAll(/R-(\d{3})/g)).map(item => Number(item[1]))
  const uniqueSorted = Array.from(new Set(matches)).sort((a, b) => a - b)

  const missing = []
  for (let index = 1; index <= expectedRuleEnd; index += 1) {
    if (!uniqueSorted.includes(index)) {
      missing.push(formatRuleId(index))
    }
  }

  if (missing.length > 0) {
    errors.push(`codex-rules.md missing rule ids:\n- ${missing.join('\n- ')}`)
    return
  }

  const maxRuleId = uniqueSorted[uniqueSorted.length - 1] || 0
  if (maxRuleId < expectedRuleEnd) {
    errors.push(`codex-rules.md max rule id must be >= ${formatRuleId(expectedRuleEnd)}, found ${formatRuleId(maxRuleId)}`)
    return
  }

  checks.push(`codex-rules.md rule continuity ok (${formatRuleId(1)} to ${formatRuleId(expectedRuleEnd)})`)
  checks.push(`codex-rules.md max rule id ok (${formatRuleId(maxRuleId)})`)
}

function ensureValidationRules(errors, checks) {
  const validationRulesPath = 'docs/quality/validation-rules.md'
  if (!pathExists(validationRulesPath)) {
    errors.push(`Missing validation rules file: ${validationRulesPath}`)
    return
  }

  const content = readFile(validationRulesPath)
  if (!content.trim()) {
    errors.push(`${validationRulesPath} is empty`)
    return
  }

  if (!content.includes('## 索引')) {
    errors.push(`${validationRulesPath} missing index section`)
    return
  }

  const indexSection = content.includes('## 阶段质量门禁')
    ? content.split('## 阶段质量门禁')[0]
    : content

  const missingIndexRules = []
  expectedValidationIndexRanges.forEach(([start, end]) => {
    for (let index = start; index <= end; index += 1) {
      const ruleId = formatRuleId(index)
      if (!indexSection.includes(ruleId)) {
        missingIndexRules.push(ruleId)
      }
    }
  })

  if (missingIndexRules.length > 0) {
    errors.push(`validation-rules.md missing indexed validation rules:\n- ${missingIndexRules.join('\n- ')}`)
    return
  }

  checks.push(`validation-rules.md exists and is non-empty (${validationRulesPath})`)
  const indexDescriptions = expectedValidationIndexRanges
    .map(([start, end]) => `${formatRuleId(start)} to ${formatRuleId(end)}`)
    .join(', ')
  checks.push(`validation-rules.md index ok (${indexDescriptions})`)
}

function ensureAlibabaGuidelineDoc(errors, checks) {
  const guidelineDocPath = 'docs/quality/alibaba-java-guidelines.md'
  if (!pathExists(guidelineDocPath)) {
    errors.push(`Missing Alibaba guideline document: ${guidelineDocPath}`)
    return
  }

  const content = readFile(guidelineDocPath)
  const requiredMarkers = [
    '黄山版',
    '2022-02-03',
    '强制执行',
    '推荐执行',
    '人工评审',
    'Controller',
    'Service',
    'Repository'
  ]
  const missingMarkers = requiredMarkers.filter(marker => !content.includes(marker))
  if (missingMarkers.length > 0) {
    errors.push(`Alibaba guideline document missing markers:\n- ${missingMarkers.join('\n- ')}`)
    return
  }

  checks.push('Alibaba guideline document ok')
}

function ensureReadmeIndex(errors, checks) {
  const readmePath = 'README.md'
  if (!pathExists(readmePath)) {
    errors.push(`Missing README.md`)
    return
  }

  const content = readFile(readmePath)
  const missingMarkers = requiredReadmeMarkers.filter(marker => !content.includes(marker))
  if (missingMarkers.length > 0) {
    errors.push(`README.md missing documentation index references:\n- ${missingMarkers.join('\n- ')}`)
    return
  }

  const requiredHeadings = ['## 项目简介', '## 快速启动', '## 文档索引']
  const missingHeadings = requiredHeadings.filter(heading => !content.includes(heading))
  if (missingHeadings.length > 0) {
    errors.push(`README.md missing sections:\n- ${missingHeadings.join('\n- ')}`)
    return
  }

  checks.push('README.md documentation index ok')
}

function ensureMessagingModeConfig(errors, checks) {
  const missingFiles = requiredMessagingConfigs.filter(item => !pathExists(item))
  if (missingFiles.length > 0) {
    errors.push(`R-144 missing application config files:\n- ${missingFiles.join('\n- ')}`)
    return
  }

  const missingMode = requiredMessagingConfigs.filter(item => !readFile(item).includes('messaging:\n  mode:'))
  if (missingMode.length > 0) {
    errors.push(`R-144 application configs missing messaging.mode:\n- ${missingMode.join('\n- ')}`)
    return
  }

  checks.push(`R-144 messaging.mode config ok (${requiredMessagingConfigs.length} files)`)
}

function ensureMessagingSchema(errors, checks) {
  const schemaPath = 'sql/init-schema.sql'
  if (!pathExists(schemaPath)) {
    errors.push(`Missing schema file: ${schemaPath}`)
    return
  }

  const content = readFile(schemaPath)
  if (!content.includes('CREATE TABLE IF NOT EXISTS kafka_message_queue')) {
    errors.push(`R-144 schema missing kafka_message_queue table in ${schemaPath}`)
    return
  }

  checks.push(`R-144 schema ok (${schemaPath} contains kafka_message_queue)`)
}

function main() {
  const errors = []
  const checks = []

  ensureDocsStructure(errors, checks)
  ensureAlibabaGuidelineArchive(errors, checks)
  ensureAdrTemplate(errors, checks)
  ensureRuleContinuity(errors, checks)
  ensureValidationRules(errors, checks)
  ensureAlibabaGuidelineDoc(errors, checks)
  ensureReadmeIndex(errors, checks)
  ensureMessagingModeConfig(errors, checks)
  ensureMessagingSchema(errors, checks)

  if (errors.length > 0) {
    console.error('Repository knowledge lint failed:\n')
    errors.forEach(error => {
      console.error(error)
      console.error('')
    })
    process.exit(1)
  }

  console.log('Repository knowledge lint passed:')
  checks.forEach(item => console.log(`- ${item}`))
}

main()
