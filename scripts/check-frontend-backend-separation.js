#!/usr/bin/env node

const fs = require('fs')
const path = require('path')

const rootDir = process.cwd()
const frontendRoot = path.join(rootDir, 'src')
const backendRoots = [
  path.join(rootDir, 'governance-service'),
  path.join(rootDir, 'sqlforge-common')
]
const ignoredDirs = new Set([
  '.git',
  'node_modules',
  'target',
  'dist'
])

const findings = []

function walk(directory, visitor) {
  if (!fs.existsSync(directory)) {
    return
  }

  const entries = fs.readdirSync(directory, { withFileTypes: true })
  entries.forEach(entry => {
    if (ignoredDirs.has(entry.name)) {
      return
    }
    const fullPath = path.join(directory, entry.name)
    if (entry.isDirectory()) {
      walk(fullPath, visitor)
      return
    }
    visitor(fullPath)
  })
}

function relativePath(fullPath) {
  return path.relative(rootDir, fullPath).replace(/\\/g, '/')
}

function addFinding(category, severity, filePath, message, suggestion) {
  findings.push({
    category,
    severity,
    path: filePath,
    message,
    suggestion
  })
}

function checkFrontendBoundary() {
  if (!fs.existsSync(frontendRoot)) {
    addFinding(
      'structure',
      'error',
      'src',
      'Frontend source directory is missing.',
      'Keep the frontend entry directory present and independently buildable.'
    )
    return
  }

  walk(frontendRoot, filePath => {
    const relPath = relativePath(filePath)
    if (relPath.endsWith('.java') || relPath.endsWith('.xml') && relPath.includes('/mapper/')) {
      addFinding(
        'structure',
        'error',
        relPath,
        'Frontend source tree contains backend implementation artifacts.',
        'Move Java, Mapper, and backend persistence artifacts into backend modules.'
      )
    }

    if (/\.(js|vue)$/.test(relPath)) {
      const content = fs.readFileSync(filePath, 'utf8')
      const suspiciousMarkers = [
        'JdbcTemplate',
        '@Mapper',
        'Repository',
        'sqlSessionFactory',
        'SELECT ',
        'INSERT INTO ',
        'UPDATE ',
        'DELETE FROM '
      ]
      suspiciousMarkers.forEach(marker => {
        if (content.includes(marker)) {
          addFinding(
            'logic',
            'warning',
            relPath,
            `Frontend file contains suspicious backend-oriented marker: ${marker}`,
            'Keep SQL access and repository semantics on the backend side; expose them via HTTP APIs.'
          )
        }
      })
    }
  })
}

function checkBackendBoundary() {
  backendRoots.forEach(backendRoot => {
    if (!fs.existsSync(backendRoot)) {
      addFinding(
        'structure',
        'error',
        relativePath(backendRoot),
        'Backend module is missing.',
        'Keep backend modules present and independently buildable.'
      )
      return
    }

    const forbiddenFiles = ['package.json', 'vite.config.js']
    forbiddenFiles.forEach(fileName => {
      const candidate = path.join(backendRoot, fileName)
      if (fs.existsSync(candidate)) {
        addFinding(
          'structure',
          'error',
          relativePath(candidate),
          'Backend module contains frontend build entry file.',
          'Remove frontend build assets from backend modules.'
        )
      }
    })

    walk(backendRoot, filePath => {
      const relPath = relativePath(filePath)
      if (relPath.endsWith('.vue') || relPath.endsWith('.scss') || relPath.endsWith('.css')) {
        addFinding(
          'structure',
          'error',
          relPath,
          'Backend module contains frontend source artifacts.',
          'Move frontend assets to the frontend project root.'
        )
      }
    })

    const staticDir = path.join(backendRoot, 'src/main/resources/static')
    const templatesDir = path.join(backendRoot, 'src/main/resources/templates')
    ;[staticDir, templatesDir].forEach(resourceDir => {
      if (fs.existsSync(resourceDir)) {
        const files = fs.readdirSync(resourceDir)
        if (files.length > 0) {
          addFinding(
            'deployment',
            'warning',
            relativePath(resourceDir),
            'Backend resources contain static or template assets that may couple frontend runtime to backend packaging.',
            'Keep deployment boundaries explicit; avoid making frontend runtime depend on backend resource packaging.'
          )
        }
      }
    })
  })
}

function checkBuildEntrypoints() {
  if (!fs.existsSync(path.join(rootDir, 'package.json'))) {
    addFinding(
      'build',
      'error',
      'package.json',
      'Frontend package manifest is missing.',
      'Keep frontend build entry files in the repository root.'
    )
  }

  if (!fs.existsSync(path.join(rootDir, 'pom.xml'))) {
    addFinding(
      'build',
      'error',
      'pom.xml',
      'Backend Maven parent POM is missing.',
      'Keep backend build entry files in the repository root.'
    )
  }
}

function printFindings() {
  if (findings.length === 0) {
    console.log('Frontend-backend separation check passed:')
    console.log('- category=structure severity=info path=. message=No structural or boundary violations detected suggestion=Keep independent build and deployment boundaries intact')
    return
  }

  console.error('Frontend-backend separation check failed:')
  findings.forEach(item => {
    console.error(`- category=${item.category} severity=${item.severity} path=${item.path} message=${item.message} suggestion=${item.suggestion}`)
  })
}

function main() {
  checkBuildEntrypoints()
  checkFrontendBoundary()
  checkBackendBoundary()
  printFindings()
  if (findings.some(item => item.severity === 'error')) {
    process.exit(1)
  }
}

main()
