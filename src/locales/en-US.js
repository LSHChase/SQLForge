export default {
  common: {
    appName: 'SQL Lifecycle Governance Platform',
    platformTagline: 'governance cockpit',
    currentWorkspace: 'Current workspace',
    workspaceLabel: 'workspace',
    workspaceSummary: '{tenant} · default engine {engine}',
    brandSummary: 'A unified control surface for query, parsing, benchmarking, acceleration and audit operations.',
    sidebarLabel: 'navigation',
    runtimeLabel: 'runtime state',
    navGroups: {
      main: 'main workflows',
      governanceHistory: 'governance history',
      governanceOps: 'governance ops',
      temporary: 'temporary delivery'
    },
    defaultEngine: 'Default engine',
    backupEngine: 'Backup engine',
    currentTenant: 'Current tenant',
    desktopMode: 'Desktop baseline',
    temporaryPage: 'Temporary',
    nonProductionOnly: 'Non-prod only',
    switchToDark: 'Switch dark',
    switchToLight: 'Switch light'
  },
  dashboard: {
    title: 'Engineering Dashboard',
    summary: 'Aggregate SQL lifecycle status, risk signals and entry points for the five core workflows.',
    eyebrow: 'runtime health',
    heroTitle: 'Force every SQL statement through governance before it reaches production.',
    heroSummary:
      'The dashboard centralizes load, parser stability, benchmark readiness, acceleration hit rate and audit signals so operators can decide whether the platform is ready, traceable and actionable.',
    heroPrimary: 'Open SQL Query',
    heroSecondary: 'View Benchmark Report',
    heroFootnote: 'Dark-mode-native dashboard baseline',
    metricLabel: 'Key metrics',
    metrics: {
      queryVolume: 'Query volume',
      parseSuccess: 'Parser success rate',
      benchmarkPass: 'Benchmark pass rate',
      accelerationHit: 'Acceleration hit rate',
      auditSignal: 'Audit / anomaly events'
    },
    quickEntryTitle: 'Core workflow entries',
    quickEntrySummary: 'The five core workflows stay independent. Dashboard only summarizes, routes and recommends.',
    healthTitle: 'Health & risk',
    healthSummary: 'Lead with conclusions, then route into the next action.',
    activityTitle: 'Recent activity',
    activitySummary: 'Keep a runtime timeline instead of turning the homepage into static project copy.',
    nextTitle: 'Recommended actions',
    nextSummary: 'Move directly into the page that can resolve the current risk or drift.'
  },
  deliveryProgress: {
    title: 'AI Delivery Progress',
    summary: 'Non-production page for AI task status, validation evidence and module progress only.',
    eyebrow: 'delivery strict mode',
    heroTitle: 'Keep AI delivery truth anchored to the authoritative ledgers.',
    heroSummary:
      'This page renders a read-only snapshot derived from `tasks.md`, `tasks-done.md`, validation logs and execution plans. It does not replace the official business dashboard or maintain a parallel state source.',
    visibilityLabel: 'production hidden',
    visibilityTitle: 'This page only appears outside production',
    visibilitySummary: 'After production cutover the entry is hidden by default, while history remains in the task ledgers, validation log and git write-back trail.',
    summaryTitle: 'Status overview',
    summaryDescription: 'Start from the status distribution, then drill into active work, module progress and validation evidence.',
    activeTasksTitle: 'Active tasks',
    activeTasksDescription: 'Todo, in-progress, in-review and blocked tasks are all derived from the active ledger snapshot.',
    moduleTitle: 'Module progress',
    moduleDescription: 'Completion is aggregated by execution domain so architects can spot delivery density and blockers quickly.',
    recentChangesTitle: 'Recent changes',
    recentChangesDescription: 'Merge task progress write-back, archived tasks and validation entries into one read-only delivery timeline.',
    blockedTitle: 'Blockers and pending reasons',
    blockedDescription: 'List explicit blocked tasks first; if none exist, fall back to unresolved reasons extracted from the latest progress logs.',
    blockedFootnote: 'This list currently includes tasks explicitly marked as blocked in the ledger.',
    pendingFootnote: 'There are no explicit blocked tasks right now, so the list falls back to pending reasons from the latest progress logs.',
    dependencyTitle: 'Dependency chains',
    dependencyDescription: 'Break active-task dependencies apart and distinguish ledger-tracked, plan-only and untracked dependencies.',
    completedTitle: 'Recently completed',
    completedDescription: 'Archived tasks and commit subjects are derived from `tasks-done.md` only.',
    validationTitle: 'Recent validation',
    validationDescription: 'The validation timeline is a read-only view of the latest `validation-log.md` entries.',
    changeKind: {
      progress: 'Progress update',
      done: 'Archived task',
      validation: 'Validation'
    },
    cards: {
      todo: 'Todo',
      inProgress: 'In progress',
      inReview: 'In review',
      blocked: 'Blocked',
      done: 'Done'
    },
    status: {
      todo: 'Todo',
      in_progress: 'In progress',
      in_review: 'In review',
      blocked: 'Blocked',
      done: 'Done',
      planned: 'Planned',
      untracked: 'Untracked'
    },
    dependencySource: {
      ledger: 'Source: task ledger',
      plan: 'Source: execution plan',
      external: 'Source: human follow-up'
    },
    runtime: {
      modePill: 'Mode {mode}',
      modeLabel: 'Current mode',
      flagLabel: 'Flag state',
      scopeLabel: 'Visibility scope',
      scope: 'Visible outside production',
      reasonNonProduction: 'The current build is non-production and the temporary page has not been disabled via `VITE_ENABLE_DELIVERY_PROGRESS=false`.',
      reasonFlagDisabled: 'The current build is not production, but `VITE_ENABLE_DELIVERY_PROGRESS=false` explicitly disables route registration.',
      reasonProduction: 'Production builds never register this temporary route, so the page stays hidden after cutover.',
      flag: {
        enabled: 'Explicitly enabled',
        disabled: 'Explicitly disabled',
        default: 'Default on'
      }
    },
    meta: {
      priority: 'Priority',
      dependsOn: 'Depends on',
      totalTasks: 'tasks',
      blockedSource: 'Source: latest progress log',
      unresolvedCount: '{count} unresolved deps'
    },
    empty: {
      blockedTitle: 'No blocker items to display',
      blockedDescription: 'This area will refresh automatically once a task enters `blocked` or a new unresolved reason appears in the latest progress logs.',
      dependencyTitle: 'No dependency chains yet',
      dependencyDescription: 'Dependency structures will render here after active tasks carry `Depends on` entries.'
    }
  },
  sqlQuery: {
    title: 'SQL Query',
    summary: 'Submit SQL, select an execution strategy and enter the downstream governance flow.'
  },
  parseRecord: {
    title: 'Parse Record',
    summary: 'Track parser output, rewrite status, failed samples and historical diagnostics.'
  },
  repairEvidence: {
    title: 'Repair Evidence',
    summary: 'Reverse lookup trace, task and report evidence to confirm compensation and repair outcomes.'
  },
  auditForensics: {
    title: 'Audit Forensics',
    summary: 'Stitch compensation, repair, write-back and history events into a paged forensic chain.'
  },
  auditTroubleshooting: {
    title: 'Audit Troubleshooting',
    summary: 'Summarize failure type, compensation state, write-back state and queue impact, then expose remediation actions and acceptance signals.'
  },
  runtimeGates: {
    title: 'Runtime Gates',
    summary: 'Consolidate phase entry, delivery, and compliance runtime-gate evidence plus the remaining exit blockers.'
  },
  recoveryDrill: {
    title: 'Recovery Drill',
    summary: 'Track backup scope, restore objectives, ownership boundaries, and mandatory post-restore acceptance checks.'
  },
  benchmark: {
    title: 'Benchmark Report',
    summary: 'Inspect baseline, peak latency, regression deltas and release readiness.'
  },
  acceleration: {
    title: 'Acceleration',
    summary: 'Manage acceleration policy, materialized view guidance and hit performance.'
  },
  system: {
    title: 'System Management',
    summary: 'Control tenants, routing defaults, audit retention and global governance settings.'
  }
}
