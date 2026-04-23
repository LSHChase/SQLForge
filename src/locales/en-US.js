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
    panorama: {
      kicker: 'project panorama',
      title: 'Project Panorama',
      summary: 'Keep the vision, milestones, glossary and rule index on the homepage so the cockpit does not collapse into runtime metrics only.',
      cardLabel: 'project index',
      cards: [
        {
          title: 'Vision & mission',
          summary: 'Force every BI SQL statement through parsing, benchmarking, rewrite, routing and acceleration governance before release so performance and consistency stay predictable.',
          items: ['One control surface for query, parser, benchmark, acceleration and audit operations', 'Backend authority, traceable history and recorded rules stay as the platform baseline', 'Start with a single region and keep room for future multi-region expansion']
        },
        {
          title: 'Roadmap & milestones',
          summary: 'Execution moves through Phase A-F, starting from document truth and then closing frontend, backend, runtime-gate and delivery gaps in order.',
          items: ['Phase D closes the query-execution and governance mainline', 'Phase E builds the cockpit, product pages and delivery views', 'Phase F adds deployment, CI, runtime smoke and recovery governance']
        },
        {
          title: 'Glossary',
          summary: 'Normalize the project vocabulary so frontend, backend, architecture and operations do not drift on the meaning of core objects.',
          items: ['Governance chain: query -> parse -> benchmark -> acceleration -> audit', 'Task truth: tasks.md / tasks-done.md / validation log / git', 'R&D cockpit: the official homepage summary layer, not a substitute for workflow pages']
        },
        {
          title: 'Architecture rules quick reference',
          summary: 'The rule base requires code, docs, validation and delivery write-back to remain aligned instead of leaving long-term constraints in chat context.',
          items: ['Java 8 + Spring Boot 2.x remain hard backend constraints', 'Pages summarize and route; backend owns authority and history', 'Non-trivial work must go through foreman and task-audit governance']
        }
      ]
    },
    architecture: {
      kicker: 'architecture design',
      title: 'Architecture Design',
      summary: 'The dashboard keeps a compact summary of C4, topology, contracts, data model, deployment and test design, while ADRs remain the source of change truth.',
      cardLabel: 'architecture index',
      cards: [
        {
          title: 'System shape',
          summary: 'Use C4 Level 1-4 to organize system, container, component and key-module views while separating product paths from delivery-only paths.',
          items: ['`/dashboard` stays the product homepage and the five main workflow pages remain independent', 'Backend keeps the controller -> service -> domain/infrastructure layering contract', 'Bounded contexts split around query governance, optimization, benchmark and system management']
        },
        {
          title: 'Service topology',
          summary: 'Frontend and backend deploy separately, with governance capabilities coordinated across query execution, parsing, optimization, benchmark, audit and system control.',
          items: ['query-execution accepts SQL submission and orchestrates governance', 'optimization and benchmark evolve independently and join the mainline via explicit contracts', 'system, audit and delivery views cover configuration, forensics and delivery observation']
        },
        {
          title: 'Contracts & data model',
          summary: 'Important behavior enters through explicit contracts and backend validation, while history, export and audit are stitched together by traceable keys.',
          items: ['HTTP handlers return explicit JSON errors rather than ambiguous strings', 'MySQL is the primary persistence direction for queryable and exportable history objects', 'Entities do not cross services directly; DTOs and service orchestration carry cross-domain flow']
        },
        {
          title: 'Data lifecycle',
          summary: 'Governance data is designed around hot/cold storage, immutable evidence and long-lived history rather than browser-local state.',
          items: ['Completed results are treated as evidence and should not be rewritten casually', 'Audit logs stay queryable for at least 180 days', 'Acceleration, benchmark, repair and audit outputs can be traced through shared keys']
        },
        {
          title: 'Deployment & flow',
          summary: 'Deployment starts from a Huawei Cloud single-region topology, while business flows cover normal execution, rollback, compensation and acceleration branches.',
          items: ['Frontend and backend must build and deploy independently on amd64 and arm64', 'The chain covers query, parse, benchmark, acceleration and audit stages', 'Operations continue to retain logs, alerts, backup and recovery evidence']
        },
        {
          title: 'Test architecture',
          summary: 'Validation spans build, layered tests, runtime smoke, task audit and delivery write-back so “implemented” is never treated as “verified”.',
          items: ['Frontend passes build and IA structure checks at minimum', 'Backend passes module tests, contract tests and runtime smoke gates', 'Architecture changes must update ADRs, validation logs and task ledgers together']
        }
      ]
    },
    progress: {
      kicker: 'delivery truth',
      title: 'Progress Management',
      badge: 'authoritative snapshot',
      openDelivery: 'Open delivery progress',
      deliveryHidden: 'The temporary delivery page is hidden by default in production',
      sourceTitle: 'Truth sources',
      sourceSummary: 'Task ledgers, validation logs and the master plan are merged into a read-only snapshot; the dashboard does not maintain a second status source.',
      cards: {
        active: 'Active tasks',
        activeDetail: 'The current total of todo, in-progress, in-review and blocked items still present in `tasks.md`.',
        done: 'Archived tasks',
        doneDetail: 'The cumulative total derived from `tasks-done.md`, used to reflect durable delivery output.',
        validation: 'Validation entries',
        validationDetail: 'The validation-log total, used as a compact signal for gate activity and evidence density.',
        completion: 'Overall completion',
        completionDetail: 'Calculated from active plus archived tasks to show how much of the current delivery wave is actually closed.'
      },
      modulesTitle: 'Phase completion',
      modulesSummary: 'Aggregate completion by execution domain so architects can see which phase is advancing and which one is still accumulating work.',
      moduleMeta: '{total} tasks total, {done} done, {progress} in progress',
      recentTitle: 'Recent actions',
      recentSummary: 'Merge task progress logs, archive records and validation entries into one compact delivery timeline.',
      dependenciesTitle: 'Blockers & dependency chains',
      dependenciesSummary: 'Surface current blocker and pending reasons first, then show unresolved dependencies for active tasks.',
      blockerLabel: 'Pending now',
      noBlockers: 'There are no explicit blocker items right now; new pending reasons will surface automatically from the latest progress logs.',
      noDependencies: 'Active tasks do not currently expose a dependency chain worth rendering here.'
    },
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
