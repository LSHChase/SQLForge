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
assert(packageJson.dependencies?.vue === '3.5.13', 'vue must be 3.5.13')
assert(packageJson.dependencies?.['vue-i18n'] === '10.0.8', 'vue-i18n must be 10.0.8')
assert(packageJson.devDependencies?.vite === '5.4.11', 'vite must be 5.4.11')
assert(packageJson.devDependencies?.['@vitejs/plugin-vue'] === '5.2.4', '@vitejs/plugin-vue must be 5.2.4')
assert(packageJson.devDependencies?.['@vue/compiler-sfc'] === '3.5.13', '@vue/compiler-sfc must be 3.5.13')
assert(packageJson.devDependencies?.['eslint-plugin-vue'] === '9.33.0', 'eslint-plugin-vue must be 9.33.0')
assert(packageJson.devDependencies?.['vue-eslint-parser'] === '10.4.0', 'vue-eslint-parser must be 10.4.0')

assert(vuePackage.name === 'vue', 'node_modules/vue must resolve to the vue package')
assert(vuePackage.version === '3.5.13', 'node_modules/vue must expose version 3.5.13')
assert(fs.existsSync(path.join(root, 'node_modules', '@vue', 'compiler-sfc')), 'node_modules must contain @vue/compiler-sfc')
assert(fs.existsSync(path.join(root, 'node_modules', '@vitejs', 'plugin-vue')), 'node_modules must contain @vitejs/plugin-vue')

const rootPackageLock = packageLock.packages?.['']
assert(rootPackageLock?.engines?.node === '18.20.8', 'package-lock root Node engine must be 18.20.8')
assert(rootPackageLock?.engines?.npm === '10.8.2', 'package-lock root NPM engine must be 10.8.2')
assert(rootPackageLock?.dependencies?.vue === '3.5.13', 'package-lock must pin vue 3.5.13')
assert(rootPackageLock?.dependencies?.['vue-i18n'] === '10.0.8', 'package-lock must pin vue-i18n 10.0.8')
assert(rootPackageLock?.devDependencies?.vite === '5.4.11', 'package-lock must pin vite 5.4.11')
assert(rootPackageLock?.devDependencies?.['@vitejs/plugin-vue'] === '5.2.4', 'package-lock must pin @vitejs/plugin-vue 5.2.4')
assert(rootPackageLock?.devDependencies?.['@vue/compiler-sfc'] === '3.5.13', 'package-lock must pin @vue/compiler-sfc 3.5.13')
