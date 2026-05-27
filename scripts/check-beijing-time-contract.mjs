#!/usr/bin/env node
import fs from 'node:fs'
import path from 'node:path'

const ROOT = process.cwd()
const errors = []

const read = file => fs.readFileSync(path.join(ROOT, file), 'utf8')

const walk = dir => {
  const absolute = path.join(ROOT, dir)
  if (!fs.existsSync(absolute)) {
    return []
  }
  return fs.readdirSync(absolute, { withFileTypes: true }).flatMap(entry => {
    const relative = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      if (['target', 'node_modules', 'dist', 'dist-portable'].includes(entry.name)) {
        return []
      }
      return walk(relative)
    }
    return [relative]
  })
}

const requireText = (file, text, message) => {
  if (!read(file).includes(text)) {
    errors.push(`${file}: ${message}`)
  }
}

const forbidPattern = (files, pattern, message, allow = () => false) => {
  for (const file of files) {
    if (allow(file)) {
      continue
    }
    const content = read(file)
    if (pattern.test(content)) {
      errors.push(`${file}: ${message}`)
    }
  }
}

const frontendFiles = walk('src').filter(file => /\.(js|mjs|vue)$/.test(file))
const javaMainFiles = [
  ...walk('governance/src/main/java'),
  ...walk('query-execution/src/main/java'),
  ...walk('sql-optimization/src/main/java'),
  ...walk('benchmark-engine/src/main/java'),
  ...walk('sqlforge-shared/src/main/java')
].filter(file => file.endsWith('.java'))
const scriptFiles = walk('scripts').filter(file => /\.(py|sh|mjs)$/.test(file))

requireText('src/views/common/beijingTime.mjs', "BEIJING_TIME_ZONE = 'Asia/Shanghai'", '前端必须声明北京时间时区。')
requireText('src/views/common/beijingTime.mjs', "hour12: false", '前端时间格式必须禁用 12 小时制。')
requireText('src/views/common/beijingTime.mjs', "hourCycle: 'h23'", '前端时间格式必须使用 00-23 小时循环。')

forbidPattern(frontendFiles, /toLocaleString\s*\(/, '页面不得直接使用浏览器本地时区格式化时间。', file =>
  file === 'src/views/common/beijingTime.mjs'
)
forbidPattern(frontendFiles, /replace\(['"]Z['"],\s*['"] UTC['"]\)/, '页面不得把 UTC 直接展示给操作者。')

forbidPattern(javaMainFiles, /ZoneOffset\.UTC/, '后端主代码不得用 UTC 解释业务时间。')
forbidPattern(javaMainFiles, /LocalDateTime\.now\s*\(/, '后端主代码必须通过 DateUtils.now() 生成本地业务时间。', file =>
  file === 'sqlforge-shared/src/main/java/com/company/sqlforge/common/utils/DateUtils.java'
)
requireText('sqlforge-shared/src/main/java/com/company/sqlforge/common/utils/DateUtils.java', 'BEIJING_ZONE_ID = "Asia/Shanghai"', '后端共享时间工具必须固定 Asia/Shanghai。')
requireText('sqlforge-shared/src/main/java/com/company/sqlforge/common/utils/DateUtils.java', 'DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")', '后端共享时间格式必须为 24 小时制。')

for (const file of [
  'governance/src/main/resources/application.yml',
  'query-execution/src/main/resources/application.yml',
  'sql-optimization/src/main/resources/application.yml',
  'benchmark-engine/src/main/resources/application.yml'
]) {
  requireText(file, 'time-zone: Asia/Shanghai', 'Spring JSON 时间时区必须为北京时间。')
  requireText(file, 'date-format: yyyy-MM-dd HH:mm:ss', 'Spring JSON 时间格式必须为 24 小时制。')
}

for (const file of [
  'governance/src/main/resources/logback-spring.xml',
  'query-execution/src/main/resources/logback-spring.xml',
  'sql-optimization/src/main/resources/logback-spring.xml',
  'benchmark-engine/src/main/resources/logback-spring.xml'
]) {
  requireText(file, 'Asia/Shanghai', '日志时间格式必须显式指定北京时间。')
}

forbidPattern(
  [
    ...walk('governance/src/main/resources'),
    ...walk('query-execution/src/main/resources'),
    ...walk('sql-optimization/src/main/resources'),
    ...walk('benchmark-engine/src/main/resources'),
    ...walk('sql'),
    ...scriptFiles
  ],
  /serverTimezone=UTC|datetime\.now\(timezone\.utc\)\.astimezone\(\)|date\.today\(\)/,
  '配置、SQL 和脚本不得使用 UTC 或宿主机日期作为用户可读时间基线。',
  file => file === 'scripts/check-beijing-time-contract.mjs'
)
forbidPattern(
  scriptFiles,
  /date\s+--iso-8601|\$\(\s*date \+%Y%m%d%H%M%S\s*\)/,
  '脚本可见时间不得使用宿主机本地时区，必须通过北京时间工具或显式 TZ=Asia/Shanghai。'
)

for (const file of ['docker-compose.yml', 'docker-compose-cn.yml', 'docker-compose-simple.yml']) {
  requireText(file, 'TZ: Asia/Shanghai', 'MySQL 容器必须固定系统时区为 Asia/Shanghai。')
  requireText(file, '--default-time-zone=+08:00', 'MySQL 默认会话时区必须为 +08:00。')
}
requireText('sql/init-schema.sql', "SET time_zone = '+08:00';", '初始化 schema 必须设置北京时间会话时区。')
requireText('sql/init-data.sql', "SET time_zone = '+08:00';", '初始化数据必须设置北京时间会话时区。')
requireText('sql/migrations/V20260526_002__beijing_time_baseline.sql', "SET time_zone = '+08:00';", '迁移基线必须记录北京时间会话时区。')

if (errors.length > 0) {
  console.error(`北京时间契约检查失败：\n- ${errors.join('\n- ')}`)
  process.exit(1)
}

console.log('北京时间契约检查通过：前端、后端、脚本和数据库均固定 Asia/Shanghai 24 小时制。')
