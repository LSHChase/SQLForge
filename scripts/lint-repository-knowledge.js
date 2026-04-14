import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import url from 'node:url';

const requiredPaths = [
  'AGENTS.md',
  'ARCHITECTURE.md',
  'DESIGN.md',
  'PLANS.md',
  'PRODUCT_SENSE.md',
  'QUALITY_SCORE.md',
  'RELIABILITY.md',
  'SECURITY.md',
  'docs/design-docs/index.md',
  'docs/design-docs/core-beliefs.md',
  'docs/product-specs/index.md',
  'docs/exec-plans/active/README.md',
  'docs/exec-plans/completed/2026-04-13-initial-scaffold.md',
  'docs/exec-plans/tech-debt-tracker.md',
  'docs/generated/db-schema.md',
  'docs/references/repository-conventions.md',
  'docs/references/task-start-requirements.md'
];

export function runRepositoryKnowledgeLint(repoRoot) {
  const issues = [];

  for (const relativePath of requiredPaths) {
    if (!fs.existsSync(path.join(repoRoot, relativePath))) {
      issues.push(`missing required repository knowledge path: ${relativePath}`);
    }
  }

  const agentsPath = path.join(repoRoot, 'AGENTS.md');
  if (fs.existsSync(agentsPath)) {
    const content = fs.readFileSync(agentsPath, 'utf8');
    const lineCount = content.split('\n').length;

    if (lineCount > 120) {
      issues.push(`AGENTS.md should stay short, got ${lineCount} lines`);
    }

    const requiredAgentsPatterns = [
      /ARCHITECTURE\.md/,
      /docs\/design-docs\/index\.md/,
      /docs\/product-specs\/index\.md/,
      /docs\/references\/task-start-requirements\.md/,
      /Mandatory Before Every Requirement Task/
    ];

    for (const pattern of requiredAgentsPatterns) {
      if (!pattern.test(content)) {
        issues.push(`AGENTS.md is missing required reference: ${pattern}`);
      }
    }
  }

  const taskStartPath = path.join(repoRoot, 'docs/references/task-start-requirements.md');
  if (fs.existsSync(taskStartPath)) {
    const content = fs.readFileSync(taskStartPath, 'utf8');
    const requiredTaskStartPatterns = [
      /mandatory reading before every new requirement task/i,
      /Harness engineering discipline/i,
      /Do not start implementation before reading the required documents/i,
      /large module delivery is complete/i,
      /create a git commit, create a git tag/i
    ];

    for (const pattern of requiredTaskStartPatterns) {
      if (!pattern.test(content)) {
        issues.push(`task-start requirements are missing required rule: ${pattern}`);
      }
    }
  }

  return issues;
}

export function formatIssues(issues) {
  return issues.map((issue) => `- ${issue}`).join('\n');
}

const currentFilePath = url.fileURLToPath(import.meta.url);

if (process.argv[1] && path.resolve(process.argv[1]) === currentFilePath) {
  const repoRoot = process.cwd();
  const issues = runRepositoryKnowledgeLint(repoRoot);

  if (issues.length) {
    console.error('repository knowledge lint failed');
    console.error(formatIssues(issues));
    process.exit(1);
  }

  console.log('repository knowledge lint passed');
}
