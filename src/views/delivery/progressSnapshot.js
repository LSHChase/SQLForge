import activeTasksMarkdown from '../../../tasks.md?raw'
import completedTasksMarkdown from '../../../tasks-done.md?raw'
import validationLogMarkdown from '../../../docs/quality/validation-log.md?raw'
import masterPlanMarkdown from '../../../docs/plans/master-execution-plan.md?raw'

const SECTION_STATUS_MAP = {
  Todo: 'todo',
  'In Progress': 'in_progress',
  'In Review': 'in_review',
  Blocked: 'blocked',
  Done: 'done'
}

const STATUS_LABEL_MAP = {
  todo: 'todo',
  in_progress: 'in_progress',
  in_review: 'in_review',
  blocked: 'blocked',
  done: 'done',
  planned: 'planned',
  untracked: 'untracked'
}

const UNRESOLVED_MARKERS = /(待|仍需|尚未|缺失|暂不|partial|placeholder|blocked|阻塞|后续|closeout|人工确认|未完成)/i
const EXPLICIT_BLOCKED_REASON_MARKERS = /(blocked|阻塞|待|仍需|尚未|缺失|partial|placeholder|closeout|未完成)/i

function splitSections(markdown) {
  const matches = Array.from(markdown.matchAll(/^##\s+(.+)$/gm))
  return matches.map((match, index) => {
    const title = match[1].trim()
    const start = match.index + match[0].length
    const end = index + 1 < matches.length ? matches[index + 1].index : markdown.length
    return {
      title,
      content: markdown.slice(start, end)
    }
  })
}

function parseProgressLog(body) {
  const marker = '- Progress log:'
  const markerIndex = body.indexOf(marker)
  if (markerIndex === -1) {
    return []
  }

  const progressSection = body.slice(markerIndex + marker.length)
  return Array.from(progressSection.matchAll(/^\s*-\s+(.+)$/gm)).map(match => match[1].trim())
}

function parseValidationList(body) {
  const marker = '- Validation:'
  const markerIndex = body.indexOf(marker)
  if (markerIndex === -1) {
    return []
  }

  const validationSection = body.slice(markerIndex + marker.length)
  return Array.from(validationSection.matchAll(/^\s*-\s+(.+)$/gm)).map(match => match[1].trim())
}

function parseTaskBlocks(markdown) {
  return splitSections(markdown).flatMap(section =>
    Array.from(section.content.matchAll(/^###\s+(.+?)\n([\s\S]*?)(?=^###\s+|$)/gm)).map(match => {
      const heading = match[1].trim()
      const body = match[2]
      const separatorIndex = heading.indexOf(':')
      const taskId = separatorIndex === -1 ? heading : heading.slice(0, separatorIndex).trim()
      const name = separatorIndex === -1 ? heading : heading.slice(separatorIndex + 1).trim()
      const explicitStatus = body.match(/^- Status:\s*(.+)$/m)?.[1]?.trim()
      const completedAt = body.match(/^- Completed at:\s*(.+)$/m)?.[1]?.trim() || ''
      const priority = body.match(/^- Priority:\s*(.+)$/m)?.[1]?.trim() || ''
      const dependsOn = body.match(/^- Depends on:\s*(.+)$/m)?.[1]?.trim() || 'none'
      const scope = body.match(/^- Scope:\s*(.+)$/m)?.[1]?.trim() || ''
      const commitSubject = body.match(/^- Commit subject:\s*`(.+)`$/m)?.[1]?.trim() || ''
      const status = explicitStatus || SECTION_STATUS_MAP[section.title] || 'unknown'

      return {
        taskId,
        name,
        section: section.title,
        status,
        statusKey: STATUS_LABEL_MAP[status] || status,
        completedAt,
        priority,
        dependsOn,
        scope,
        commitSubject,
        validation: parseValidationList(body),
        progressLog: parseProgressLog(body)
      }
    })
  )
}

function parseValidationEntries(markdown) {
  return markdown
    .split(/\r?\n/)
    .map(line => line.trim())
    .filter(line => /^\d{4}-\d{2}-\d{2}T/.test(line))
    .map(line => {
      const parts = line.split('|').map(part => part.trim())
      return {
        timestamp: parts[0] || '',
        trigger: parts[1] || '',
        rules: parts[2] || '',
        result: parts[3] || '',
        evidence: parts[4] || ''
      }
    })
}

function parsePlannedTaskIds(markdown) {
  return new Set(Array.from(markdown.matchAll(/`([A-Z]+-[A-Z]+-\d+)`/g)).map(match => match[1]))
}

function deriveModuleLabel(taskId) {
  if (/^[A-Z]-TASK-\d+$/.test(taskId)) {
    return `Phase ${taskId.charAt(0)}`
  }
  if (taskId.startsWith('DOC-GOV')) {
    return 'Doc Governance'
  }
  if (taskId.startsWith('HARN')) {
    return 'Harness'
  }
  return taskId.split('-')[0]
}

function buildModuleProgress(tasks) {
  const buckets = new Map()

  tasks.forEach(task => {
    const moduleLabel = deriveModuleLabel(task.taskId)
    if (!buckets.has(moduleLabel)) {
      buckets.set(moduleLabel, {
        moduleLabel,
        total: 0,
        done: 0,
        in_progress: 0,
        in_review: 0,
        blocked: 0,
        todo: 0
      })
    }

    const bucket = buckets.get(moduleLabel)
    bucket.total += 1
    if (bucket[task.status] !== undefined) {
      bucket[task.status] += 1
    }
  })

  return Array.from(buckets.values())
    .map(bucket => ({
      ...bucket,
      completionRate: bucket.total === 0 ? 0 : Math.round((bucket.done / bucket.total) * 100)
    }))
    .sort((left, right) => right.total - left.total || left.moduleLabel.localeCompare(right.moduleLabel))
}

function parseDateValue(value) {
  if (!value) {
    return 0
  }

  const normalized = /^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value}T00:00:00` : value
  const parsed = Date.parse(normalized)
  return Number.isNaN(parsed) ? 0 : parsed
}

function splitLogTimestamp(logLine) {
  const match = logLine.match(/^(\d{4}-\d{2}-\d{2})(?::\s*(.*))?$/)

  if (!match) {
    return {
      timestampLabel: '',
      timestamp: 0,
      detail: logLine
    }
  }

  return {
    timestampLabel: match[1],
    timestamp: parseDateValue(match[1]),
    detail: match[2] || logLine
  }
}

function buildRecentChanges(activeTasks, completedTasks, validationEntries) {
  const progressChanges = activeTasks.flatMap(task =>
    task.progressLog.map(logLine => {
      const logEntry = splitLogTimestamp(logLine)
      return {
        itemKey: `${task.taskId}-${logLine}`,
        kind: 'progress',
        taskId: task.taskId,
        title: task.name,
        timestamp: logEntry.timestamp,
        timestampLabel: logEntry.timestampLabel || task.status,
        detail: logEntry.detail,
        auxiliary: task.scope,
        sourceFile: 'tasks.md'
      }
    })
  )

  const completedChanges = completedTasks.map(task => ({
    itemKey: `${task.taskId}-${task.completedAt}`,
    kind: 'done',
    taskId: task.taskId,
    title: task.name,
    timestamp: parseDateValue(task.completedAt),
    timestampLabel: task.completedAt,
    detail: task.commitSubject || task.scope,
    auxiliary: task.scope,
    sourceFile: 'tasks-done.md'
  }))

  const validationChanges = validationEntries.map(entry => ({
    itemKey: `${entry.timestamp}-${entry.trigger}`,
    kind: 'validation',
    taskId: entry.trigger,
    title: entry.result,
    timestamp: parseDateValue(entry.timestamp),
    timestampLabel: entry.timestamp,
    detail: entry.evidence,
    auxiliary: entry.rules,
    sourceFile: 'docs/quality/validation-log.md'
  }))

  return [...progressChanges, ...completedChanges, ...validationChanges]
    .sort((left, right) => right.timestamp - left.timestamp || left.itemKey.localeCompare(right.itemKey))
    .slice(0, 8)
}

function buildBlockerItems(activeTasks) {
  const blockerItems = []
  const addedTaskIds = new Set()

  activeTasks
    .filter(task => task.status === 'blocked')
    .forEach(task => {
      const reasonLine = [...task.progressLog].reverse().find(line => EXPLICIT_BLOCKED_REASON_MARKERS.test(line))
      blockerItems.push({
        taskId: task.taskId,
        name: task.name,
        status: task.status,
        dependsOn: task.dependsOn,
        reason: reasonLine ? splitLogTimestamp(reasonLine).detail : task.scope
      })
      addedTaskIds.add(task.taskId)
    })

  activeTasks
    .filter(task => task.status !== 'blocked')
    .forEach(task => {
      if (addedTaskIds.has(task.taskId)) {
        return
      }

      const issueLine = [...task.progressLog].reverse().find(line => UNRESOLVED_MARKERS.test(line))
      if (!issueLine) {
        return
      }

      blockerItems.push({
        taskId: task.taskId,
        name: task.name,
        status: task.status,
        dependsOn: task.dependsOn,
        reason: splitLogTimestamp(issueLine).detail
      })
    })

  const statusWeight = {
    blocked: 0,
    in_progress: 1,
    in_review: 2,
    todo: 3
  }

  return blockerItems
    .sort(
      (left, right) =>
        (statusWeight[left.status] ?? 9) - (statusWeight[right.status] ?? 9) ||
        left.taskId.localeCompare(right.taskId)
    )
    .slice(0, 6)
}

function parseDependencyList(dependsOn) {
  if (!dependsOn || dependsOn === 'none') {
    return []
  }

  return dependsOn
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)
}

function buildDependencyChains(activeTasks, allTasks, plannedTaskIds) {
  const allTaskMap = new Map(allTasks.map(task => [task.taskId, task]))

  return activeTasks
    .map(task => {
      const dependencies = parseDependencyList(task.dependsOn).map(dependencyId => {
        const dependencyTask = allTaskMap.get(dependencyId)
        const isPhaseMilestone = /^Phase-[A-Z]$/i.test(dependencyId) || /^Phase-[A-Z0-9]+$/i.test(dependencyId)

        if (dependencyTask) {
          return {
            dependencyId,
            label: dependencyTask.name,
            status: dependencyTask.status,
            statusKey: STATUS_LABEL_MAP[dependencyTask.status] || dependencyTask.status,
            source: 'ledger'
          }
        }

        if (plannedTaskIds.has(dependencyId) || isPhaseMilestone) {
          return {
            dependencyId,
            label: dependencyId,
            status: 'planned',
            statusKey: 'planned',
            source: 'plan'
          }
        }

        return {
          dependencyId,
          label: dependencyId,
          status: 'untracked',
          statusKey: 'untracked',
          source: 'external'
        }
      })

      return {
        taskId: task.taskId,
        name: task.name,
        status: task.status,
        statusKey: task.statusKey,
        dependencies,
        unresolvedCount: dependencies.filter(item => item.status !== 'done').length
      }
    })
    .filter(task => task.dependencies.length)
    .sort(
      (left, right) =>
        right.unresolvedCount - left.unresolvedCount ||
        right.dependencies.length - left.dependencies.length ||
        left.taskId.localeCompare(right.taskId)
    )
}

export function createDeliveryProgressSnapshot() {
  const activeTasks = parseTaskBlocks(activeTasksMarkdown)
  const completedTasks = parseTaskBlocks(completedTasksMarkdown)
  const allTasks = [...activeTasks, ...completedTasks]
  const validationEntries = parseValidationEntries(validationLogMarkdown)
  const plannedTaskIds = parsePlannedTaskIds(masterPlanMarkdown)

  const statusCounts = {
    todo: activeTasks.filter(task => task.status === 'todo').length,
    in_progress: activeTasks.filter(task => task.status === 'in_progress').length,
    in_review: activeTasks.filter(task => task.status === 'in_review').length,
    blocked: activeTasks.filter(task => task.status === 'blocked').length,
    done: completedTasks.length
  }

  return {
    sourceFiles: [
      'tasks.md',
      'tasks-done.md',
      'docs/quality/validation-log.md',
      'docs/plans/master-execution-plan.md'
    ],
    statusCounts,
    activeTasks,
    completedTasks,
    validationEntries,
    moduleProgress: buildModuleProgress(allTasks),
    recentChanges: buildRecentChanges(activeTasks, completedTasks, validationEntries),
    blockerItems: buildBlockerItems(activeTasks),
    hasExplicitBlocked: activeTasks.some(task => task.status === 'blocked'),
    dependencyChains: buildDependencyChains(activeTasks, allTasks, plannedTaskIds)
  }
}
