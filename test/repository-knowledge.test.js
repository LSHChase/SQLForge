import test from 'node:test';
import assert from 'node:assert/strict';
import { runRepositoryKnowledgeLint } from '../scripts/lint-repository-knowledge.js';

const repoRoot = process.cwd();

test('repository knowledge lint passes', () => {
  const issues = runRepositoryKnowledgeLint(repoRoot);
  assert.deepEqual(issues, []);
});
