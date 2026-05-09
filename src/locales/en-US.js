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
    operatorHeroTitle: 'Overview and operator cockpit',
    operatorHeroSummary: 'The homepage now aggregates parse, governance, recommendation, and dispatch evidence while staying inside audited repository surfaces instead of overstating partial samples.',
    operatorFocusEyebrow: 'operator focus',
    openRisksTitle: 'Open risks',
    openRisksSummary: 'Failed messages, dispatch failures, urgent SQL, and high-risk recommendations are merged into one operator focus here.',
    coreKpiTitle: 'Core KPI',
    primaryEntriesTitle: 'Five primary entries',
    platformHealthRiskTitle: 'Platform health and risk',
    issueDistributionTitle: 'Issue distribution',
    recentActivityTitle: 'Recent activity',
    nextStepsTitle: 'Recommended next steps',
    evidenceEyebrow: 'static evidence',
    evidenceBoundaryTitle: 'Evidence boundaries',
    evidenceBoundarySummary: 'The homepage shows only facts proven by the current API response, window, or coordination mode instead of turning samples into global conclusions.',
    evidenceRows: {
      parseOverview: {
        label: 'Parse overview',
        detail: 'Taken from the current parse-statistics overview sample, not full-tenant history.'
      },
      queryWindow: {
        label: 'Query window',
        detail: 'Taken from the current query-history page window; success, failure and hit rates are window-based.'
      },
      messageStats: {
        label: 'Governance messages',
        detail: 'Taken from governance admin message stats and only merges pending plus failed risk.'
      },
      dispatchMode: {
        label: 'Dispatch mode',
        detail: 'Keeps showing PULL_ONLY or the backend-returned mode without pretending active push exists.'
      }
    },
    actions: {
      openQueryWorkbench: 'Open query workbench',
      openSqlParse: 'Open SQL Parse',
      refreshOverview: 'Refresh overview'
    },
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
    compliance: {
      kicker: 'compliance center',
      title: 'Compliance Center',
      summary: 'Expose the graded-protection baseline directly on the cockpit homepage so identity, isolation, audit, encryption and backup expectations remain visible.',
      sourceTitle: 'Compliance truth',
      sourceSummary: 'This section is a read-only projection of `R-111` through `R-115` from `docs/security/compliance.md`; the frontend does not maintain a parallel policy source.',
      cards: [
        {
          ruleId: 'R-111',
          title: 'Identity authentication',
          summary: 'Every user action must be authenticated by the backend, with separate privilege models for administrators and standard users.',
          items: ['HTTP and API requests must carry verifiable credentials', 'Unauthenticated or failed-auth requests return explicit JSON errors', 'Authentication failures and privilege abuse both enter the audit trail']
        },
        {
          ruleId: 'R-112',
          title: 'Access control',
          summary: 'The system isolates data by tenant ID and revalidates authorization across routing, execution, export and benchmark operations.',
          items: ['All core requests carry explicit tenant context', 'Data sources, query tasks, audit records and export records are tenant-bound', 'Unauthorized access is denied by default with no implicit allow path']
        },
        {
          ruleId: 'R-113',
          title: 'Security audit',
          summary: 'SQL actions, sign-in or sign-out events, and permission changes must be recorded in audit logs that stay immutable for at least 180 days.',
          items: ['Audit entries include time, tenant, user, object, result, latency and trace ID', 'Sensitive content must be masked or encrypted before logging', 'Audit storage is backed up independently and protected from direct mutation']
        },
        {
          ruleId: 'R-114',
          title: 'Encrypted storage',
          summary: 'Database passwords, API keys and similar secrets must be encrypted at rest rather than stored in plaintext.',
          items: ['Sensitive fields are encrypted before persistence with AES-256 or an equivalent baseline', 'Config files, logs, stack traces and exports must not leak plaintext secrets', 'Key management stays separate from business data and can integrate with a dedicated key service']
        },
        {
          ruleId: 'R-115',
          title: 'Backup & recovery',
          summary: 'Production MySQL must use replication or an equivalent HA setup, meet the RPO/RTO targets, and keep backups encrypted.',
          items: ['Backups include full plus required incremental or binlog strategy', 'Audit logs and core metadata sit on the priority recovery list', 'Recovery drills must leave a traceable record']
        }
      ]
    },
    rulebook: {
      kicker: 'codex rulebook',
      title: 'Codex Rulebook',
      summary: 'This section stays read-only and highlights append-only rules, repository truth and the task-audit chain instead of leaving durable constraints inside prompts or memory.',
      cardLabel: 'rule cluster',
      sourceTitle: 'Rule sources',
      sourceSummary: 'Rule categories, numbering and extension state are derived from the current repository truth in `docs/rules/codex-rules.md` and the compliance baseline.',
      cardsSummary: {
        baseline: 'Baseline rules',
        baselineDetail: 'The count of architecture and engineering constraints rooted in `R-001` through `R-115`.',
        validation: 'Extended validation rules',
        validationDetail: 'The count of `R-116+` validation, phase-gate and governance-extension rules.',
        compliance: 'Compliance rules',
        complianceDetail: 'The count of graded-protection rules derived from the security compliance document.',
        sources: 'Source documents',
        sourcesDetail: 'The number of rule and compliance authority documents directly summarized on this homepage.'
      },
      cards: [
        {
          title: 'Docs & governance',
          summary: 'Rules require document-first execution, task ledgers, closeout discipline, and append-only history so long-term constraints survive beyond chat state.',
          items: ['Documents outrank prompt memory', 'Long-term rules must enter `docs/` and the history ledgers', 'Non-trivial work must pass through foreman, task-audit and git traceability']
        },
        {
          title: 'Architecture & engineering',
          summary: 'Frontend/backend separation, Java 8 + Spring Boot 2.x, layered backend structure, MyBatis XML and independent deployment remain hard engineering limits.',
          items: ['Frontend stays on Vue 3 + JavaScript + CSS', 'Backend stays on Java 8 + Spring Boot 2.x', 'Domain directories plus layered subdirectories remain the default backend shape']
        },
        {
          title: 'Pages & boundaries',
          summary: 'The dashboard summarizes and routes while major workflows stay on independent pages; frontend can prevalidate, but backend remains the authority for rules and history.',
          items: ['Pages are organized around context, state, result and next step', 'Multiple core workflows should not collapse back into one long page', 'Delivered capabilities must stay visible on operator-facing pages']
        },
        {
          title: 'Validation & extension',
          summary: 'The later part of the rulebook adds phase gates, task validation, delivery closeout and strict-mode constraints as machine-auditable rules.',
          items: ['Phase entry, phase delivery and progressive compliance each have validation gates', 'Frontend build, knowledge lint and task-audit are part of the default closeout floor', 'New rules can only be appended, not used to overwrite historical meaning']
        }
      ]
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
      dependencyDescription: 'Dependency structures will render here after active tasks carry `Depends on` entries.',
      progressLog: 'No progress log yet'
    }
  },
  sqlQuery: {
    title: 'SQL Query',
    summary: 'Submit SQL, select an execution strategy and enter the downstream governance flow.',
    hero: {
      eyebrow: 'query workbench'
    },
    metrics: {
      resultRows: 'Result rows',
      resultRowsDetail: 'Shows rows returned by the current response, not backend history totals.',
      validationTips: 'Validation tips',
      validationTipsDetail: 'The frontend only gives input hints; backend validation remains authoritative.',
      recentRuns: 'Recent runs',
      recentRunsDetail: 'Shows only the latest six execution summaries in this session.',
      pending: 'Pending',
      review: 'Review',
      ready: 'Ready',
      sessionOnly: 'session'
    },
    resultTabs: {
      access: 'Access parse',
      history: 'History links'
    },
    access: {
      queryDateStatus: 'query_date status',
      queryDateStart: 'query_date start',
      queryDateEnd: 'query_date end',
      queryDateFields: 'query_date fields',
      bindingMode: 'Binding mode',
      logicalObjects: 'Logical objects',
      parseStatus: 'Lightweight parse status',
      commentContext: 'Comment context'
    },
    historyAssociation: {
      sqlFingerprint: 'SQL fingerprint',
      contractStage: 'Contract stage',
      implementationStage: 'Implementation stage',
      downloadUrl: 'Download URL',
      historyBoundary: 'History boundary',
      backendHistory: 'History persistence, export, and audit links are governed by backend query-history.'
    }
  },
  sqlHistory: {
    title: 'SQL History',
    summary: 'Search persisted query-history rows, linked traces, and detail evidence.',
    actions: {
      refresh: 'Search / refresh',
      lookup: 'Indexed lookup',
      clear: 'Clear filters',
      openRepairEvidence: 'Open repair evidence',
      openAuditForensics: 'Open audit forensics',
      exportEvidence: 'Export evidence',
      viewRawEvidence: 'View raw evidence',
      copy: 'Copy'
    },
    options: {
      all: 'All',
      default: 'Default'
    },
    filters: {
      tenant: 'Tenant',
      tenantPlaceholder: 'Uses route tenant or tenant-a by default',
      reportKey: 'SQL/report key',
      reportKeyPlaceholder: 'Enter reportCode or SQL fingerprint',
      datasource: 'Datasource',
      datasourcePlaceholder: 'Enter datasource code',
      stage: 'Stage',
      stagePlaceholder: 'Enter stage',
      bizDate: 'Business date',
      queryDateStart: 'Query date start',
      queryDateEnd: 'Query date end',
      datePlaceholder: 'Select date',
      status: 'Execution status',
      logicalObjectType: 'Logical object type',
      accessChannel: 'Access channel',
      engine: 'Target engine',
      submittedBy: 'Submitted by',
      submittedByPlaceholder: 'Enter submitter',
      submittedStart: 'Submitted start',
      submittedEnd: 'Submitted end',
      cacheHit: 'Cache hit',
      rewriteApplied: 'Rewrite applied',
      accelerationApplied: 'Acceleration applied',
      parameterizedSql: 'Parameterized SQL',
      sortBy: 'Sort by',
      sortOrder: 'Sort order',
      traceIdPlaceholder: 'Enter Trace ID',
      taskIdPlaceholder: 'Enter Task ID',
      reportIdPlaceholder: 'Enter Report ID',
      selectPlaceholder: 'Select',
      eyebrow: 'history filters',
      title: 'History filters and indexed lookup',
      summary: 'Every filter maps to an existing query-history parameter; empty defaults preserve the history query contract.'
    },
    metrics: {
      label: 'SQL execution history summary',
      currentPage: 'Current page',
      total: 'Execution records',
      success: 'Success',
      nonSuccess: 'Non-success',
      accessChannels: 'Access channels',
      queryDateResolved: 'query_date resolved',
      parameterizedSql: 'Parameterized SQL'
    },
    queryStatus: {
      idle: 'Not queried',
      loading: 'Loading',
      success: 'Refreshed',
      error: 'Query failed'
    },
    table: {
      kicker: 'Execution history table',
      title: 'Execution records',
      historyId: 'History ID',
      reportKey: 'SQL/report key',
      requestTenant: 'Request tenant',
      datasource: 'Datasource',
      stage: 'Stage',
      status: 'Execution status',
      accessChannel: 'Access channel',
      targetEngine: 'Target engine',
      queryDate: 'query_date',
      sqlState: 'SQL state',
      governanceHits: 'Governance hits',
      submittedBy: 'Submitted by',
      submittedAt: 'Submitted at',
      auditEventCount: 'Audit event count',
      summary: 'The list consumes backend pagination, filters, and classification summary; details drill down through the drawer.'
    },
    states: {
      loading: 'Loading SQL execution history',
      empty: 'No SQL execution history matches the current filters.',
      loadFailed: 'The list failed to load. Check the error above.',
      errorTitle: 'SQL history query failed',
      datasourceOptionsFallback: 'Datasource options are unavailable; manual values remain enabled.',
      noSignalEvidence: 'No parse, route, or context evidence is available for this detail.'
    },
    messages: {
      lookupRequired: 'Enter at least one of traceId, taskId, or reportId.',
      lookupEmpty: 'No history matched the lookup criteria.',
      lookupWithoutExecution: 'A trace was found but no QUERY_EXECUTION history is available.'
    },
    detail: {
      title: 'Execution detail',
      historyId: 'History ID',
      traceId: 'Trace ID',
      resultId: 'Result ID',
      reportKey: 'SQL/report key',
      datasource: 'Datasource',
      datasourceType: 'Datasource type',
      stage: 'Stage',
      bizDate: 'Business date',
      queryDateStart: 'query_date start',
      queryDateEnd: 'query_date end',
      queryDateStatus: 'query_date status',
      status: 'Execution status',
      accessChannel: 'Access channel',
      targetEngine: 'Target engine',
      submittedBy: 'Submitted by',
      submittedAt: 'Submitted at',
      auditEventCount: 'Audit event count'
    },
    execution: {
      cacheHit: 'Cache hit',
      rewriteApplied: 'Rewrite applied',
      accelerationApplied: 'Acceleration applied',
      returnedRows: 'Returned rows',
      errorCode: 'Error code',
      errorMessage: 'Error message',
      routeDecision: 'Route decision',
      cacheSummary: 'Cache summary'
    },
    sql: {
      sqlFingerprint: 'SQL fingerprint',
      templateFingerprint: 'Template fingerprint',
      boundFingerprint: 'Bound fingerprint',
      bindingMode: 'Binding mode',
      bindingRender: 'Binding render',
      parameterizedSql: 'Parameterized SQL',
      originalSql: 'Original SQL',
      templateSql: 'Template SQL',
      boundSql: 'Bound SQL'
    },
    governance: {
      cache: 'cache',
      rewrite: 'rewrite',
      acceleration: 'accel'
    },
    tabs: {
      overview: 'Execution overview',
      execution: 'Execution evidence',
      sql: 'SQL tri-state',
      signals: 'Parse and route',
      refs: 'Linked evidence',
      audit: 'Audit links'
    },
    signals: {
      commentContext: 'Comment context',
      queryDateSummary: 'query_date summary',
      logicalObjectHits: 'Logical object hits',
      structureParseSummary: 'Structure parse summary',
      accessParseSummary: 'Access parse summary',
      bindingSummary: 'Binding summary',
      routeDecision: 'Route decision',
      cacheSummary: 'Cache summary'
    },
    refs: {
      recommendationRefs: 'Recommendation refs',
      benchmarkRefs: 'Benchmark refs',
      auditRefs: 'Audit refs',
      alertRefs: 'Alert refs'
    },
    audit: {
      service: 'Service',
      operation: 'Operation',
      status: 'Status',
      createdAt: 'Created at'
    },
    rawEvidence: {
      title: 'Raw evidence'
    },
    export: {
      title: 'Export SQL execution evidence',
      format: 'Format',
      includeTraceDetail: 'Include trace detail',
      run: 'Run export'
    },
    footer: {
      currentPageCount: '{count} on this page',
      totalCount: '{count} total',
      pageWindow: 'Page {current}/{total}',
      resultWindow: '{count} on this page / {total} total / page {current}/{pages}',
      lastQuery: 'Last query: {status} · {time}'
    }
  },
  parseRecord: {
    title: 'Parse History Search',
    summary: 'Search batch parse and report-import history with batch-level parse records.'
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
    summary: 'Consolidate phase entry, delivery, and compliance runtime-gate evidence plus the remaining exit blockers.',
    heroEyebrow: 'phase-f runtime gates',
    heroTitle: 'Runtime Gates And Exit Blocking Baseline',
    heroSummary: 'This page consolidates the Entry, Delivery, and Compliance gates so Phase-F build, runtime, recovery, and compliance evidence no longer drift across scripts and docs.',
    heroNote: 'The point is not more display pages, but a clear line between blocking gates and residual risks.',
    evidenceEyebrow: 'evidence map',
    evidenceTitle: 'Scripts And Workflow Entry Points',
    evidenceSummary: 'Lists repository scripts and workflow entry points only; it does not claim an external environment has already executed them.',
    kpiTitle: 'Gate KPIs And Evidence Boundaries',
    kpiSummary: 'Metrics count only the scripts, workflows and residual blockers listed on this page, without extrapolating global runtime health.',
    activityEyebrow: 'gate activity',
    activityTitle: 'Gate Activity Stream',
    activitySummary: 'Shows blocking checks and script entry points in Entry, Delivery and Compliance order.',
    blockersEyebrow: 'residual blockers',
    blockersTitle: 'Residual Blockers Still Open',
    blockersSummary: 'These remain risks to track before exit and must not be presented as already closed facts.',
    gates: {
      entry: {
        title: 'Entry Gate',
        summary: 'Task ledger, compiled governance policy, and repository knowledge lint must pass first.'
      },
      delivery: {
        title: 'Delivery Gate',
        summary: 'Database scripts, build, coverage, and Sonar are wired into the delivery gate.'
      },
      compliance: {
        title: 'Compliance Gate',
        summary: 'Recovery baseline, observability baseline, Kafka gate, and sensitive-data controls now feed R-118 rechecks.'
      }
    },
    blockers: {
      coverage: 'The phase gate now enforces coverage thresholds, but the repository still needs higher overall line coverage to reach the Phase-1+ 85% bar.',
      sonar: 'Sonar is now mandatory in delivery/full gates and will block when secrets are missing.',
      workflowDispatch: 'The Phase Gate workflow remains an explicit workflow_dispatch gate instead of an automatically bound release action.'
    },
    evidenceRows: {
      defaultCi: 'Default CI gate',
      kafka: 'Real Kafka gate',
      phaseGate: 'Phase Gate entrypoint',
      phaseScript: 'Phase script'
    },
    metrics: {
      gates: {
        label: 'Gate layers',
        detail: 'Counts only the Entry, Delivery and Compliance gates explicitly listed here.'
      },
      checks: {
        label: 'Blocking checks',
        trend: 'Script entrypoints',
        detail: 'Derived from the command list in the gate activity stream, not from external environment state.'
      },
      workflows: {
        label: 'Workflow entries',
        detail: 'Shows repository workflow file references only; execution results remain tied to validation logs.'
      },
      blockers: {
        label: 'Residual blockers',
        trend: 'Track further',
        detail: 'These items stay in the risk queue and must not be marked as closed facts.'
      }
    }
  },
  recoveryDrill: {
    title: 'Recovery Drill',
    summary: 'Track backup scope, restore objectives, ownership boundaries, and mandatory post-restore acceptance checks.',
    heroEyebrow: 'recovery drill baseline',
    heroTitle: 'Backup Recovery And Post-Restore Acceptance Baseline',
    heroSummary: 'This page gathers the backup inventory, recovery objectives, ownership boundaries, and post-restore checks established by F-TASK-008/009.',
    heroNote: 'Restore completion means more than database replay: health probes, audit compensation, queue backlog, export/desensitization, and leak checks must all pass.',
    objectivesEyebrow: 'rpo / rto',
    objectivesTitle: 'Recovery Objectives And Owners',
    objectivesSummary: 'This static table records data domains, RPO/RTO targets, and owner boundaries.',
    checklistEyebrow: 'acceptance checklist',
    checklistTitle: 'Mandatory Post-Restore Checklist',
    checklistSummary: 'High-risk evidence must be rechecked after restore; replaying the database is not the only completion signal.',
    kpiTitle: 'Recovery Drill KPIs And Acceptance Boundaries',
    kpiSummary: 'Shows only baseline objects, RPO/RTO targets and post-restore checks, without presenting drill results as production-verified facts.',
    activityEyebrow: 'recovery flow',
    activityTitle: 'Recovery Object Activity Stream',
    activitySummary: 'Shows the recovery batch focus objects in order with their acceptance semantics.',
    sameBackupBatch: 'Same batch as backup',
    inventory: {
      core: 'Primary metadata tables, schema version, and migration inventory must recover as one batch.',
      audit: 'Audit evidence must remain continuous and still accept `LOGIN/LOGOUT` and `audit/write` samples after restore.',
      export: 'Export metadata and sanitized archive pointers must reconcile with each other.',
      queue: 'Database fallback backlog must remain replayable after recovery.',
      keys: 'Restore ciphertext only, never plaintext, and keep `encryption_key_id` aligned with the backup batch.'
    },
    checklist: {
      health: 'All backend `/actuator/health` probes and governance `/api/governance/health` return `UP`.',
      audit: 'Replay `audit/write` and `LOGIN/LOGOUT` audit samples after restore.',
      backlog: 'Check `kafka_message_queue` backlog or explicitly record why it is not applicable.',
      export: 'Sample `export_record` against `history_id/result_id` and confirm storage pointers remain sanitized.',
      leak: 'Sample log platforms and `system_config` to confirm no password, token, or key leaks.'
    },
    metrics: {
      inventory: {
        label: 'Recovery objects',
        trend: 'Baseline inventory',
        detail: 'Counts only the recovery objects explicitly listed in the current baseline.'
      },
      rpo: {
        label: 'RPO target',
        detail: 'Recovery point target shared by governance metadata, audit, export and queue domains.'
      },
      rto: {
        label: 'RTO target',
        detail: 'Post-restore health, audit, queue and desensitization checks still have to pass.'
      },
      checklist: {
        label: 'Recheck items',
        trend: 'post-restore',
        detail: 'Every item must be rechecked after restore; a single success signal does not close the loop.'
      }
    },
    table: {
      domain: 'Domain',
      owner: 'Owner'
    }
  },
  benchmark: {
    title: 'Benchmark Report',
    summary: 'Inspect baseline, peak latency, regression deltas and release readiness.'
  },
  parseBatchCenter: {
    title: 'Batch Parse Center',
    summary: 'Handle batch creation, template download, file import, failure retry, and report-catalog SQL parsing on an independent page.'
  },
  parseStatisticsCenter: {
    title: 'SQL Parse Statistics Compatibility Entry',
    summary: 'This route is kept only as a compatibility redirect and lands back on SQL Parse statistics views.'
  },
  assetCatalog: {
    title: 'Data Asset Catalog',
    summary: 'Browse datasource, schema, table, logical-view, and db-view lists with detail evidence.'
  },
  routingGovernance: {
    title: 'Routing Execution Evidence',
    summary: 'Review routing calibration, historical decisions, and comment-protocol summaries through a read-only evidence surface.'
  },
  recommendationCenter: {
    title: 'Recommendation Center',
    summary: 'Review recommendation categories, benefit or risk, dispatch status, and traceability links.'
  },
  accessCenter: {
    title: 'Open Access',
    summary: 'Review API, JDBC Agent, Java SDK, access strategies, and access-audit samples.'
  },
  alertCenter: {
    title: 'Alert Center',
    summary: 'Review derived alerts, simulated ACK state, and notify-simulated outcomes.'
  },
  acceleration: {
    title: 'SQL Parse',
    summary: 'Focus on single-SQL parsing, structure/access results, and history traceability.'
  },
  system: {
    title: 'System Management',
    summary: 'Inspect and manage datasources, report interfaces, Redis rule sources, Dispatch policies, and system evidence.'
  }
}
