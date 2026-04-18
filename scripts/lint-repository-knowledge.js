#!/usr/bin/env node

const fs = require('fs')
const path = require('path')

const rootDir = process.cwd()

const requiredDocsPaths = [
  'docs',
  'docs/README.md',
  'docs/architecture',
  'docs/architecture/init.md',
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
  'docs/adr/README.md',
  'docs/security/compliance.md',
  'docs/plans/phase-0-plan.md'
]

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
  for (let index = 1; index <= 115; index += 1) {
    if (!uniqueSorted.includes(index)) {
      missing.push(`R-${String(index).padStart(3, '0')}`)
    }
  }

  if (missing.length > 0) {
    errors.push(`codex-rules.md missing rule ids:\n- ${missing.join('\n- ')}`)
    return
  }

  if (uniqueSorted.length !== 115) {
    errors.push(`codex-rules.md expected 115 unique rule ids, found ${uniqueSorted.length}`)
    return
  }

  checks.push('codex-rules.md rule continuity ok (R-001 to R-115)')
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

function main() {
  const errors = []
  const checks = []

  ensureDocsStructure(errors, checks)
  ensureAdrTemplate(errors, checks)
  ensureRuleContinuity(errors, checks)
  ensureReadmeIndex(errors, checks)

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
