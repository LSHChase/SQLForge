import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const repoRoot = process.cwd();
const srcRoot = path.join(repoRoot, 'src');

function listJsFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];

  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);

    if (entry.isDirectory()) {
      files.push(...listJsFiles(fullPath));
      continue;
    }

    if (entry.isFile() && fullPath.endsWith('.js')) {
      files.push(fullPath);
    }
  }

  return files;
}

function parseImports(filePath) {
  const source = fs.readFileSync(filePath, 'utf8');
  const imports = [];

  for (const match of source.matchAll(/from\s+['"]([^'"]+)['"]/g)) {
    imports.push(match[1]);
  }

  for (const match of source.matchAll(/import\s*\(\s*['"]([^'"]+)['"]\s*\)/g)) {
    imports.push(match[1]);
  }

  return imports;
}

function classify(filePath) {
  const relative = path.relative(srcRoot, filePath).split(path.sep);

  if (relative[0] === 'index.js') {
    return { kind: 'root' };
  }

  if (relative[0] === 'modules') {
    return {
      kind: 'module',
      domain: relative[1],
      layer: relative[2]
    };
  }

  return {
    kind: 'top',
    layer: relative[0]
  };
}

function isBuiltin(specifier) {
  return specifier.startsWith('node:');
}

function allowedImport(sourceMeta, targetMeta) {
  if (sourceMeta.kind === 'root') {
    return true;
  }

  if (sourceMeta.kind === 'top') {
    if (sourceMeta.layer === 'bootstrap') {
      return true;
    }

    if (sourceMeta.layer === 'app') {
      return targetMeta.kind === 'top' && ['app', 'config', 'shared'].includes(targetMeta.layer);
    }

    if (sourceMeta.layer === 'config') {
      return targetMeta.kind === 'top' && ['config', 'shared'].includes(targetMeta.layer);
    }

    if (sourceMeta.layer === 'shared') {
      return targetMeta.kind === 'top' && targetMeta.layer === 'shared';
    }

    if (sourceMeta.layer === 'infrastructure') {
      return (
        (targetMeta.kind === 'top' && ['infrastructure', 'shared', 'config'].includes(targetMeta.layer)) ||
        (targetMeta.kind === 'module' && targetMeta.layer === 'domain')
      );
    }
  }

  if (sourceMeta.kind === 'module') {
    if (sourceMeta.layer === 'domain') {
      return (
        (targetMeta.kind === 'top' && ['shared', 'config'].includes(targetMeta.layer)) ||
        (targetMeta.kind === 'module' &&
          targetMeta.domain === sourceMeta.domain &&
          targetMeta.layer === 'domain')
      );
    }

    if (sourceMeta.layer === 'application') {
      return (
        (targetMeta.kind === 'top' && ['shared', 'config'].includes(targetMeta.layer)) ||
        (targetMeta.kind === 'module' && ['domain', 'application'].includes(targetMeta.layer))
      );
    }
  }

  return false;
}

test('source tree respects declared architectural dependency rules', () => {
  const files = listJsFiles(srcRoot);
  const violations = [];

  for (const filePath of files) {
    const imports = parseImports(filePath);
    const sourceMeta = classify(filePath);

    for (const specifier of imports) {
      if (isBuiltin(specifier) || !specifier.startsWith('.')) {
        continue;
      }

      const resolved = path.resolve(path.dirname(filePath), specifier);
      const candidate = resolved.endsWith('.js') ? resolved : `${resolved}.js`;

      if (!candidate.startsWith(srcRoot) || !fs.existsSync(candidate)) {
        continue;
      }

      const targetMeta = classify(candidate);

      if (!allowedImport(sourceMeta, targetMeta)) {
        violations.push(
          `${path.relative(repoRoot, filePath)} -> ${path.relative(repoRoot, candidate)} is not allowed`
        );
      }
    }
  }

  assert.deepEqual(violations, []);
});
