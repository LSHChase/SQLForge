import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()

const readJson = relativePath =>
  JSON.parse(fs.readFileSync(path.join(root, relativePath), 'utf8'))

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

const packageJson = readJson('package.json')
const packageLock = readJson('package-lock.json')
const vuePackage = readJson('node_modules/vue/package.json')

assert(packageJson.packageManager === 'npm@10.8.2', 'packageManager must be npm@10.8.2')
assert(packageJson.engines?.node === '18.20.8', 'Node engine must be 18.20.8')
assert(packageJson.engines?.npm === '10.8.2', 'NPM engine must be 10.8.2')
assert(packageJson.dependencies?.vue === 'file:vendor/vue-runtime', 'vue must resolve to the local runtime shim')
assert(packageJson.dependencies?.['@vue/runtime-dom'] === '3.5.13', '@vue/runtime-dom must be 3.5.13')
assert(packageJson.dependencies?.['vue-i18n'] === '10.0.8', 'vue-i18n must be 10.0.8')
assert(packageJson.devDependencies?.vite === '5.4.11', 'vite must be 5.4.11')

const bannedDeps = [
  '@vitejs/plugin-vue',
  '@vue/compiler-sfc',
  'eslint-plugin-vue',
  'vue-eslint-parser',
  'unplugin-auto-import',
  'unplugin-vue-components'
]

for (const dependencyName of bannedDeps) {
  assert(!(dependencyName in (packageJson.dependencies || {})), `${dependencyName} must be removed from package.json dependencies`)
  assert(!(dependencyName in (packageJson.devDependencies || {})), `${dependencyName} must be removed from package.json devDependencies`)
}

assert(vuePackage.name === 'vue', 'node_modules/vue must resolve to the local vue shim')
assert(vuePackage.version === '3.5.13', 'node_modules/vue must expose version 3.5.13')
assert(!('@vue/compiler-sfc' in (vuePackage.dependencies || {})), 'vue shim must not depend on @vue/compiler-sfc')
assert(!fs.existsSync(path.join(root, 'node_modules', '@vue', 'compiler-sfc')), 'node_modules must not contain @vue/compiler-sfc')
assert(!fs.existsSync(path.join(root, 'node_modules', '@vitejs', 'plugin-vue')), 'node_modules must not contain @vitejs/plugin-vue')

const rootPackageLock = packageLock.packages?.['']
assert(rootPackageLock?.engines?.node === '18.20.8', 'package-lock root Node engine must be 18.20.8')
assert(rootPackageLock?.engines?.npm === '10.8.2', 'package-lock root NPM engine must be 10.8.2')
assert(rootPackageLock?.dependencies?.vue === 'file:vendor/vue-runtime', 'package-lock must pin the vue shim')
assert(rootPackageLock?.dependencies?.['vue-i18n'] === '10.0.8', 'package-lock must pin vue-i18n 10.0.8')
assert(rootPackageLock?.dependencies?.['@vue/runtime-dom'] === '3.5.13', 'package-lock must pin @vue/runtime-dom 3.5.13')
assert(rootPackageLock?.devDependencies?.vite === '5.4.11', 'package-lock must pin vite 5.4.11')

for (const packageKey of Object.keys(packageLock.packages || {})) {
  assert(!packageKey.includes('@vitejs/plugin-vue'), 'package-lock must not include @vitejs/plugin-vue')
  assert(!packageKey.includes('@vue/compiler-sfc'), 'package-lock must not include @vue/compiler-sfc')
}
