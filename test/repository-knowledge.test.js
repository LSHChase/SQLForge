import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const repoRoot = process.cwd();

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

test('repository contains the required harness-style knowledge map', () => {
  for (const relativePath of requiredPaths) {
    assert.equal(fs.existsSync(path.join(repoRoot, relativePath)), true, `${relativePath} should exist`);
  }
});

test('AGENTS.md remains a short navigation document', () => {
  const content = fs.readFileSync(path.join(repoRoot, 'AGENTS.md'), 'utf8');
  const lineCount = content.split('\n').length;

  assert.ok(lineCount <= 120, `AGENTS.md should stay short, got ${lineCount} lines`);
  assert.match(content, /ARCHITECTURE\.md/);
  assert.match(content, /docs\/design-docs\/index\.md/);
  assert.match(content, /docs\/product-specs\/index\.md/);
  assert.match(content, /docs\/references\/task-start-requirements\.md/);
  assert.match(content, /Mandatory Before Every Requirement Task/);
});

test('task start requirements encode the mandatory harness workflow', () => {
  const content = fs.readFileSync(
    path.join(repoRoot, 'docs/references/task-start-requirements.md'),
    'utf8'
  );

  assert.match(content, /mandatory reading before every new requirement task/i);
  assert.match(content, /Harness engineering discipline/i);
  assert.match(content, /Do not start implementation before reading the required documents/i);
  assert.match(content, /large module delivery is complete/i);
  assert.match(content, /create a git commit, create a git tag/i);
});
