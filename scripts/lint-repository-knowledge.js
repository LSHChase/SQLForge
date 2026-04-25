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
  'docs/architecture/service-capability-map.md',
  'docs/architecture/service-interface-contract-baseline.md',
  'docs/plans/README.md',
  'docs/plans/master-execution-plan.md',
  'docs/plans/document-coverage-matrix.md',
  'docs/plans/document-gap-matrix.md',
  'docs/plans/document-truth-baseline.md',
  'docs/plans/codex-governance-integration-blueprint.md',
  'docs/plans/implementation-readiness.md',
  'docs/plans/phase-prerequisite-matrix.md',
  'docs/plans/retrospective-template.md',
  'docs/plans/document-governance-retrospective-2026-04-20.md',
  'docs/plans/document-governance-repair-retrospective-2026-04-20.md',
  'docs/plans/task-spec-matrix.md',
  'docs/plans/task-governance-extension-matrix.md',
  'docs/quality',
  'docs/quality/alibaba-java-guidelines.md',
  'docs/quality/frontend-backend-separation-baseline.md',
  'docs/quality/validation-rules.md',
  'docs/quality/validation-log.md',
  'docs/rules',
  'docs/rules/codex-rules.md',
  'docs/operations',
  'docs/operations/README.md',
  'docs/operations/codex-mcp-playbook.md',
  'docs/operations/foreman-workflow.md',
  'docs/operations/human-collaboration.md',
  'docs/operations/local-development.md',
  'docs/operations/best-practices.md',
  'docs/operations/git-and-task-closeout.md',
  'docs/adr',
  'docs/adr/README.md',
  'docs/adr/adr-template.md',
  'docs/generated',
  'docs/generated/repo-map.md',
  'docs/exec-plans',
  'docs/exec-plans/active',
  'docs/exec-plans/completed',
  'docs/references',
  'docs/references/human-constraint-history.md',
  'docs/references/raw-requirements',
  'docs/security',
  'docs/security/connectors.md',
  'docs/security/compliance.md',
  'docs/plans',
  'docs/plans/phase-0-plan.md',
  '.codex',
  '.codex/config.toml',
  '.codex/hooks.json',
  '.codex/hooks',
  '.codex/hooks/shared.py',
  '.codex/hooks/user_prompt_submit.py',
  '.codex/hooks/pre_tool_use.py',
  '.codex/hooks/permission_request.py',
  '.codex/hooks/stop.py',
  '.codex/policy',
  '.codex/policy/mcp-policy.json',
  '.codex/policy/current-task.schema.json',
  '.codex/state',
  '.codex/state/.gitkeep',
  '.codex/state/current-task.example.json'
]

const requiredGovernancePaths = [
  'tasks.md',
  'tasks-done.md',
  'INBOX.md',
  '.agent/config.json',
  'scripts/task_audit.py',
  'scripts/foreman.py'
]

const requiredReadmeMarkers = [
  'docs/README.md',
  'docs/operations/README.md',
  'docs/operations/codex-mcp-playbook.md',
  'docs/architecture/init.md',
  'docs/rules/codex-rules.md',
  'docs/quality/alibaba-java-guidelines.md',
  'docs/adr/README.md',
  'docs/security/connectors.md',
  'docs/security/compliance.md',
  'docs/plans/phase-0-plan.md',
  'docs/plans/codex-governance-integration-blueprint.md'
]

const expectedRuleEnd = 176
const expectedValidationIndexRanges = [
  [116, 144],
  [151, 154],
  [156, 161],
  [168, 168],
  [170, 176]
]
const requiredMessagingConfigs = [
  'governance/src/main/resources/application-dev.yml',
  'governance/src/main/resources/application-test.yml',
  'governance/src/main/resources/application-prod.yml',
  'governance/src/test/resources/application-test.yml'
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

function ensureGovernanceStructure(errors, checks) {
  const missing = requiredGovernancePaths.filter(item => !pathExists(item))
  if (missing.length > 0) {
    errors.push(`Missing required governance paths:\n- ${missing.join('\n- ')}`)
    return
  }

  checks.push(`governance structure ok (${requiredGovernancePaths.length} paths)`)
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

function ensureDocsReadmeIndex(errors, checks) {
  const docsReadmePath = 'docs/README.md'
  if (!pathExists(docsReadmePath)) {
    errors.push(`Missing docs/README.md`)
    return
  }

  const content = readFile(docsReadmePath)
  const requiredMarkers = [
    './quality/validation-rules.md',
    './operations/README.md',
    './generated/repo-map.md',
    './plans/document-truth-baseline.md',
    './plans/document-gap-matrix.md',
    './plans/implementation-readiness.md',
    './plans/phase-prerequisite-matrix.md',
    './architecture/service-capability-map.md',
    './architecture/service-interface-contract-baseline.md',
    './plans/retrospective-template.md',
    './plans/document-governance-retrospective-2026-04-20.md',
    './plans/document-governance-repair-retrospective-2026-04-20.md',
    './plans/task-spec-matrix.md',
    './plans/task-governance-extension-matrix.md',
    './security/connectors.md',
    './operations/codex-mcp-playbook.md',
    'tasks.md',
    'tasks-done.md',
    'INBOX.md'
  ]
  const missingMarkers = requiredMarkers.filter(marker => !content.includes(marker))
  if (missingMarkers.length > 0) {
    errors.push(`docs/README.md missing governance index references:\n- ${missingMarkers.join('\n- ')}`)
    return
  }

  checks.push('docs/README.md governance index ok')
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

function listDocsFiles(directory) {
  const items = fs.readdirSync(directory, { withFileTypes: true })
  const files = []
  items.forEach(item => {
    const absolute = path.join(directory, item.name)
    if (item.isDirectory()) {
      files.push(...listDocsFiles(absolute))
      return
    }

    const relative = path.relative(rootDir, absolute).replace(/\\/g, '/')
    files.push(relative)
  })
  return files
}

function ensureCoverageMatrixCompleteness(errors, checks) {
  const coveragePath = 'docs/plans/document-coverage-matrix.md'
  if (!pathExists(coveragePath)) {
    errors.push(`Missing coverage matrix: ${coveragePath}`)
    return
  }

  const content = readFile(coveragePath)
  const matrixPaths = Array.from(new Set(Array.from(content.matchAll(/`(docs\/[^`]+)`/g)).map(item => item[1]))).sort()
  const docsFiles = listDocsFiles(absolutePath('docs')).sort()
  const missing = docsFiles.filter(item => !matrixPaths.includes(item))
  const extra = matrixPaths.filter(item => !pathExists(item))

  if (missing.length > 0) {
    errors.push(`document-coverage-matrix.md missing docs file entries:\n- ${missing.join('\n- ')}`)
    return
  }

  if (extra.length > 0) {
    errors.push(`document-coverage-matrix.md contains nonexistent docs paths:\n- ${extra.join('\n- ')}`)
    return
  }

  checks.push(`document-coverage-matrix.md completeness ok (${docsFiles.length} docs files)`)
}

function ensureMcpGovernanceDocs(errors, checks) {
  const connectorsPath = 'docs/security/connectors.md'
  const playbookPath = 'docs/operations/codex-mcp-playbook.md'
  const policyPath = '.codex/policy/mcp-policy.json'
  const manifestTemplatePath = 'docs/exec-plans/templates/multi-agent-run.template.json'
  const multiAgentPlaybookPath = 'docs/operations/multi-agent-playbook.md'
  const missingFiles = [connectorsPath, playbookPath, policyPath].filter(item => !pathExists(item))
  if (missingFiles.length > 0) {
    errors.push(`MCP governance baseline files missing:\n- ${missingFiles.join('\n- ')}`)
    return
  }

  const connectorsContent = readFile(connectorsPath)
  const connectorMarkers = [
    '观测/日志',
    '部署证据',
    '对象存储元数据',
    '外部需求/工单检索',
    'SSH',
    'K8s',
    '数据库执行型',
    'Main Foreman',
    'explorer / validator'
  ]
  const missingConnectorMarkers = connectorMarkers.filter(marker => !connectorsContent.includes(marker))
  if (missingConnectorMarkers.length > 0) {
    errors.push(`docs/security/connectors.md missing MCP boundary markers:\n- ${missingConnectorMarkers.join('\n- ')}`)
    return
  }

  const playbookContent = readFile(playbookPath)
  const playbookMarkers = [
    '只读 MCP',
    'docs/security/connectors.md',
    'mcp-policy.json',
    'Main Foreman',
    'mcp_profile',
    'explorer/validator',
    'compile-governance',
    'validate_codex_runtime.py'
  ]
  const missingPlaybookMarkers = playbookMarkers.filter(marker => !playbookContent.includes(marker))
  if (missingPlaybookMarkers.length > 0) {
    errors.push(`docs/operations/codex-mcp-playbook.md missing MCP playbook markers:\n- ${missingPlaybookMarkers.join('\n- ')}`)
    return
  }

  let policy
  try {
    policy = JSON.parse(readFile(policyPath))
  } catch (error) {
    errors.push(`Unable to parse ${policyPath}: ${error.message}`)
    return
  }

  const categories = Array.isArray(policy.read_only_categories) ? policy.read_only_categories : []
  const requiredCategoryIds = [
    'observability_logs',
    'deployment_evidence',
    'object_storage_metadata',
    'external_requirements_tickets'
  ]
  const missingCategoryIds = requiredCategoryIds.filter(id => !categories.some(item => item.id === id && item.scope === 'read-only'))
  if (missingCategoryIds.length > 0) {
    errors.push(`mcp-policy.json missing approved read-only categories:\n- ${missingCategoryIds.join('\n- ')}`)
    return
  }

  const runtimeConstraints = policy.runtime_constraints || {}
  if (
    runtimeConstraints.allow_repo_tracked_mcp_profile !== true ||
    runtimeConstraints.allow_repo_tracked_mcp_config !== false ||
    runtimeConstraints.allow_repo_tracked_server_inventory !== false ||
    runtimeConstraints.allow_repo_stored_secrets !== false ||
    runtimeConstraints.multi_agent_mcp_profile_enabled !== true ||
    runtimeConstraints.main_foreman_is_only_writeback_entry !== true
  ) {
    errors.push('mcp-policy.json runtime constraints drifted from the HARN-035 read-only multi-agent boundary')
    return
  }

  if (policy.scope?.profile !== 'multi-agent-read-only-evidence') {
    errors.push('mcp-policy.json scope.profile must remain multi-agent-read-only-evidence')
    return
  }

  const missingMultiAgentFiles = [manifestTemplatePath, multiAgentPlaybookPath].filter(item => !pathExists(item))
  if (missingMultiAgentFiles.length > 0) {
    errors.push(`Multi-agent MCP contract files missing:\n- ${missingMultiAgentFiles.join('\n- ')}`)
    return
  }

  const multiAgentPlaybook = readFile(multiAgentPlaybookPath)
  const multiAgentMarkers = ['mcp_profile', 'mcp_profiles', 'explorer / validator', 'worker']
  const missingMultiAgentMarkers = multiAgentMarkers.filter(marker => !multiAgentPlaybook.includes(marker))
  if (missingMultiAgentMarkers.length > 0) {
    errors.push(`docs/operations/multi-agent-playbook.md missing MCP multi-agent markers:\n- ${missingMultiAgentMarkers.join('\n- ')}`)
    return
  }

  let manifestTemplate
  try {
    manifestTemplate = JSON.parse(readFile(manifestTemplatePath))
  } catch (error) {
    errors.push(`Unable to parse ${manifestTemplatePath}: ${error.message}`)
    return
  }

  const templateProfiles = manifestTemplate.mcp_profiles || {}
  const readonlyTemplate = templateProfiles['readonly-evidence']
  if (!readonlyTemplate) {
    errors.push('multi-agent-run.template.json missing readonly-evidence mcp_profiles entry')
    return
  }
  if (JSON.stringify(readonlyTemplate.allowed_roles || []) !== JSON.stringify(['explorer', 'validator'])) {
    errors.push('multi-agent-run.template.json readonly-evidence allowed_roles must stay [explorer, validator]')
    return
  }

  const templateAgents = Array.isArray(manifestTemplate.agents) ? manifestTemplate.agents : []
  const truthExplorer = templateAgents.find(item => item.name === 'truth-explorer')
  const validator = templateAgents.find(item => item.name === 'validator')
  const worker = templateAgents.find(item => item.name === 'worker-name')
  if (!truthExplorer || truthExplorer.mcp_profile !== 'readonly-evidence') {
    errors.push('multi-agent-run.template.json truth-explorer must use mcp_profile=readonly-evidence')
    return
  }
  if (!validator || validator.mcp_profile !== 'readonly-evidence') {
    errors.push('multi-agent-run.template.json validator must use mcp_profile=readonly-evidence')
    return
  }
  if (worker && Object.prototype.hasOwnProperty.call(worker, 'mcp_profile')) {
    errors.push('multi-agent-run.template.json worker-name must not declare mcp_profile')
    return
  }

  checks.push('MCP governance docs and compiled policy ok')
}

function ensureFrontendDevSmokeBoundary(errors, checks) {
  const localDevPath = 'docs/operations/local-development.md'
  const ciBaselinePath = 'docs/deployments/ci-capability-baseline.md'
  const phaseGatePath = 'docs/deployments/phase-gate-baseline.md'
  const packageJsonPath = 'package.json'
  const forbiddenWiringPaths = [
    '.github/workflows/ci.yml',
    '.github/workflows/phase-gate.yml',
    '.github/workflows/release-phase-gate.yml',
    'scripts/run-runtime-smoke.sh',
    'scripts/run-phase-gates.sh'
  ]

  const missingFiles = [localDevPath, ciBaselinePath, phaseGatePath, packageJsonPath].filter(item => !pathExists(item))
  if (missingFiles.length > 0) {
    errors.push(`Frontend dev smoke boundary files missing:\n- ${missingFiles.join('\n- ')}`)
    return
  }

  const packageJson = readFile(packageJsonPath)
  if (!packageJson.includes('"smoke:frontend-dev"')) {
    errors.push(`package.json missing smoke:frontend-dev script`)
    return
  }

  const localDevContent = readFile(localDevPath)
  const localDevMarkers = [
    'npm run smoke:frontend-dev',
    '本地 `repo-closed`',
    '不替代 `npm run smoke:frontend-runtime`',
    '不得被提升为默认 CI'
  ]
  const missingLocalDevMarkers = localDevMarkers.filter(marker => !localDevContent.includes(marker))
  if (missingLocalDevMarkers.length > 0) {
    errors.push(`docs/operations/local-development.md missing frontend dev smoke boundary markers:\n- ${missingLocalDevMarkers.join('\n- ')}`)
    return
  }

  const ciBaselineContent = readFile(ciBaselinePath)
  const ciMarkers = [
    'Frontend dev browser smoke | Not in CI by design',
    '只作为本地 `repo-closed` 开发回归基线',
    '不属于默认 CI/browser runtime gate'
  ]
  const missingCiMarkers = ciMarkers.filter(marker => !ciBaselineContent.includes(marker))
  if (missingCiMarkers.length > 0) {
    errors.push(`docs/deployments/ci-capability-baseline.md missing frontend dev smoke CI-boundary markers:\n- ${missingCiMarkers.join('\n- ')}`)
    return
  }

  const phaseGateContent = readFile(phaseGatePath)
  const phaseGateMarkers = [
    'npm run smoke:frontend-dev',
    '不属于默认 phase/release runtime gate 主路径',
    '不进入 `Phase Gate` 或 `Release Phase Gate` 的默认 browser runtime gate'
  ]
  const missingPhaseGateMarkers = phaseGateMarkers.filter(marker => !phaseGateContent.includes(marker))
  if (missingPhaseGateMarkers.length > 0) {
    errors.push(`docs/deployments/phase-gate-baseline.md missing frontend dev smoke gate-boundary markers:\n- ${missingPhaseGateMarkers.join('\n- ')}`)
    return
  }

  const forbiddenHits = []
  forbiddenWiringPaths.forEach(relativePath => {
    if (!pathExists(relativePath)) {
      return
    }
    const content = readFile(relativePath)
    if (content.includes('smoke:frontend-dev') || content.includes('check-dev-frontend.mjs')) {
      forbiddenHits.push(relativePath)
    }
  })
  if (forbiddenHits.length > 0) {
    errors.push(`Frontend dev smoke must remain local-only, but these runtime/CI entrypoints reference it:\n- ${forbiddenHits.join('\n- ')}`)
    return
  }

  checks.push('frontend dev smoke boundary ok (docs and gate wiring keep it local-only)')
}

function main() {
  const errors = []
  const checks = []

  ensureDocsStructure(errors, checks)
  ensureGovernanceStructure(errors, checks)
  ensureAlibabaGuidelineArchive(errors, checks)
  ensureAdrTemplate(errors, checks)
  ensureRuleContinuity(errors, checks)
  ensureValidationRules(errors, checks)
  ensureAlibabaGuidelineDoc(errors, checks)
  ensureReadmeIndex(errors, checks)
  ensureDocsReadmeIndex(errors, checks)
  ensureMessagingModeConfig(errors, checks)
  ensureMessagingSchema(errors, checks)
  ensureCoverageMatrixCompleteness(errors, checks)
  ensureMcpGovernanceDocs(errors, checks)
  ensureFrontendDevSmokeBoundary(errors, checks)

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
