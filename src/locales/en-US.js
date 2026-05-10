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
    switchToLight: 'Switch light',
    actions: {
      copy: 'Copy',
      format: 'Format',
      viewRawJson: 'View raw JSON',
      viewRawEvidence: 'View raw evidence'
    },
    fields: {
      tenant: 'Tenant',
      datasource: 'Datasource',
      schema: 'Schema',
      status: 'Status',
      targetEngine: 'Target engine',
      serviceCode: 'Service code',
      historyId: 'History ID',
      traceId: 'Trace ID',
      reportCode: 'Report code',
      type: 'Type',
      submittedAt: 'Submitted at',
      submittedBy: 'Submitted by',
      title: 'Title',
      summary: 'Summary',
      taskType: 'Task type',
      taskId: 'Task ID',
      verdict: 'Verdict',
      format: 'Format',
      scenario: 'Scenario',
      report: 'Report',
      resultStatus: 'Result status'
    }
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
      auditEventCount: 'Audit event count'
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
      lastQuery: 'Last query: {status} · {time}'
    }
  },
  parseRecord: {
    title: 'Parse History Search',
    summary: 'Search batch parse and report-import history with batch-level parse records.',
    issueSceneDetail: {
      actions: {
        viewDetail: 'View detail'
      },
      sections: {
        reportDetail: 'Report detail',
        logicalObjectDetail: 'Logical object detail',
        sqlDetail: 'SQL detail'
      },
      columns: {
        issueScene: 'Issue scene',
        affectedSql: 'Affected SQL',
        severity: 'Severity',
        reportCount: 'Reports',
        logicalObjectCount: 'Logical objects',
        ratio: 'Ratio',
        actions: 'Actions',
        report: 'Report',
        sqlCount: 'SQL count',
        issueCount: 'Issues',
        logicalObjectKeys: 'Logical object keys',
        object: 'Object',
        reportCodes: 'Report codes',
        reportSql: 'Report / SQL',
        priority: 'Priority',
        logicalObjects: 'Logical objects',
        issueScenes: 'Issue scenes',
        location: 'Location'
      }
    }
  },
  governanceTrace: {
    tenantContext: 'Tenant context',
    businessTenant: 'Business tenant',
    governanceTenant: 'Governance tenant',
    lookupLimit: 'Lookup limit',
    traceId: 'Trace ID',
    taskId: 'Task ID',
    reportId: 'Report ID',
    windowStart: 'Window start',
    windowEnd: 'Window end',
    clearCriteria: 'Clear criteria',
    loadOlderEvidence: 'Load older evidence',
    refreshQueueImpact: 'Refresh queue impact',
    matchedTraces: 'Matched traces',
    compensationTraces: 'Compensation traces',
    repairSignalChains: 'Repair signal chains',
    reportLinkedTraces: 'Report-linked traces',
    nonSuccessChains: 'Non-success chains',
    olderEvidenceAvailable: 'Older evidence chains available',
    olderRemediationAvailable: 'Older remediation chains available',
    olderTracesAvailable: 'Older traces available',
    serviceCode: 'Service code',
    resourceType: 'Resource type',
    resourceId: 'Resource id',
    lastSeenAt: 'Last seen at',
    lookupMode: 'Lookup match',
    repairSignal: 'Repair signal',
    compensationTrace: 'Compensation trace',
    auditEvents: 'Audit events',
    sqlFingerprint: 'SQL fingerprint',
    errorCode: 'Error code',
    targetEngine: 'Target engine',
    degraded: 'Degraded',
    requestChain: 'Request chain',
    task: 'Task',
    report: 'Report',
    fingerprint: 'Fingerprint',
    error: 'Error',
    engine: 'Engine',
    degradedRecovery: 'Degraded recovery',
    queueImpact: 'Queue impact',
    queueTotal: 'Total messages',
    queuePending: 'Pending backlog',
    queueFailed: 'Failed messages',
    queueConsumed: 'Consumed',
    retryStatus: 'Retry status',
    retriedCount: 'Retried count',
    failedDelta: 'Failed delta',
    repairOutcome: 'Repair outcome',
    criteria: {
      traceId: 'Trace lookup',
      taskId: 'Task lookup',
      reportId: 'Report lookup',
      windowStart: 'Window start',
      windowEnd: 'Window end'
    }
  },
  repairEvidence: {
    title: 'Repair Evidence',
    summary: 'Reverse lookup trace, task and report evidence to confirm compensation and repair outcomes.',
    refactorNote: 'This page reuses the shared trace lookup, result list, and audit timeline components while keeping compensation, degraded recovery, and report write-back evidence visible.',
    lookupTitle: 'Lookup criteria and matched traces',
    lookupSummary: 'Enter a trace, task, or report id to query the governance trace chain; window fields are passed through to the backend lookup contract.',
    detailTitle: 'Compensation and repair detail',
    actions: {
      runLookup: 'Run reverse lookup',
      openTroubleshooting: 'Open remediation decision'
    },
    messages: {
      requiredLookup: 'Enter at least one of traceId, taskId, or reportId before running the lookup.',
      emptyCriteria: 'Enter a trace, task, or report id and then run the lookup.',
      noMatches: 'Matched traces render here so you can keep drilling into the linked audit and repair timeline.',
      emptyDetail: 'After you select a matched trace, the lookup mode, repair signals, and audit timeline render here.'
    }
  },
  auditForensics: {
    title: 'Audit Forensics',
    summary: 'Stitch compensation, repair, write-back and history events into a paged forensic chain.',
    refactorNote: 'This page consolidates failure chains, compensation traces, report write-back, and audit events into shared forensic components while preserving cross-page pivots.',
    lookupTitle: 'Forensic criteria and matched evidence chains',
    lookupSummary: 'Trace, task, or report can start the forensic lookup, and pagination cursor still comes from the backend.',
    detailTitle: 'Forensic detail and cross-page pivots',
    actions: {
      runLookup: 'Run forensic lookup',
      openParseRecord: 'Open parse record',
      openRepairEvidence: 'Open repair evidence',
      openTroubleshooting: 'Open remediation decision'
    },
    messages: {
      requiredLookup: 'Enter at least one of traceId, taskId, or reportId before running the forensic lookup.',
      emptyCriteria: 'Enter a trace, task, or report id and then run the forensic lookup.',
      noMatches: 'Matched chains render here with failure, compensation, and report write-back evidence.',
      emptyDetail: 'After you select a matched trace, the forensic signals, linked history events, and cross-page actions render here.'
    }
  },
  auditTroubleshooting: {
    title: 'Audit Troubleshooting',
    summary: 'Summarize failure type, compensation state, write-back state and queue impact, then expose remediation actions and acceptance signals.',
    refactorNote: 'This page merges trace forensics with governance queue impact in one remediation view, while retry still calls the backend authority.',
    lookupTitle: 'Failure scope, queue impact and decision inputs',
    lookupSummary: 'Business tenant drives trace lookup, while governance tenant drives message stats and retry so permission boundaries stay separate.',
    detailTitle: 'Remediation actions and acceptance signals',
    actions: {
      runLookup: 'Run remediation lookup',
      retryFailedMessages: 'Retry failed messages',
      openSystem: 'Open governance backlog',
      openRepairEvidence: 'Open repair evidence',
      openParseRecord: 'Back to parse record'
    },
    decision: {
      failureType: 'Failure type',
      compensationState: 'Compensation state',
      writeBackState: 'Write-back state',
      queueImpact: 'Queue impact',
      acceptanceState: 'Acceptance state'
    },
    messages: {
      requiredLookup: 'Enter at least one of traceId, taskId, or reportId before running the remediation lookup.',
      emptyCriteria: 'Enter a trace, task, or report id and then run the remediation lookup.',
      noMatches: 'Matched chains render here with queue impact and remediation actions.',
      emptyDetail: 'After you select a matched trace, the failure type, compensation state, write-back state, and acceptance signals render here.'
    }
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
    summary: 'Inspect baseline, peak latency, regression deltas and release readiness.',
    eyebrow: 'benchmark center',
    boundarySummary: 'The repo exposes live benchmark task and report APIs; templates and test sets remain frontend session/catalog surfaces rather than backend CRUD.',
    input: {
      eyebrow: 'benchmark input',
      title: 'Benchmark boundary and inputs',
      summary: 'Tenant, task type, and SQL are the key inputs for real benchmark task submission. Success and failure-compensation flows run in the tabs below.'
    },
    fields: {
      sql: 'SQL',
      initialStatus: 'Initial status',
      failureCode: 'Failure code',
      retryable: 'Retryable',
      pendingBefore: 'Pending before',
      pendingAfter: 'Pending after',
      pendingDelta: 'Pending delta',
      totalDelta: 'Total delta',
      rawDataPath: 'Raw-data path',
      scannedBytes: 'Scanned bytes'
    },
    tabs: {
      templates: 'Templates',
      testSets: 'Test sets',
      taskFlow: 'Task flow',
      compensation: 'Failure compensation',
      report: 'Report',
      sessionTasks: 'Session tasks'
    },
    templates: {
      eyebrow: 'template catalog',
      title: 'Template list',
      summary: 'Templates are task presets on this page for quickly filling real benchmark task parameters.'
    },
    testSets: {
      eyebrow: 'test-set catalog',
      title: 'Test-set list',
      summary: 'Test sets are frontend session catalog entries, not a claim that a dedicated test-set API exists.'
    },
    taskFlow: {
      eyebrow: 'benchmark task flow',
      title: 'Success task flow',
      summary: 'Submit, poll, and read the report with the taskContext from the selected template.'
    },
    compensation: {
      eyebrow: 'failure compensation',
      title: 'Failure compensation flow',
      summary: 'The failure path appends FAIL_BENCHMARK to verify governance compensation queue evidence.'
    },
    report: {
      eyebrow: 'benchmark report',
      title: 'Report comparison and regression results',
      summary: 'After a successful run, show live report engine results, threshold assessments, trend charts, and recommendations.',
      empty: 'A successful run will render the live report payload here.',
      returned: 'Report {reportId} was returned by the live API with formats {formats}.',
      engineEyebrow: 'engine comparison',
      comparisonTitle: 'Comparison metrics',
      regressionEyebrow: 'regression results',
      regressionTitle: 'Threshold and regression verdicts',
      trendEyebrow: 'trend & recommendation',
      trendTitle: 'Trend charts and follow-up recommendations'
    },
    session: {
      eyebrow: 'session tasks',
      title: 'Session task list',
      summary: 'There is no global benchmark-task list API yet, so this panel keeps tasks launched in this session.',
      empty: 'No benchmark task has been launched in this session.'
    },
    actions: {
      runTemplate: 'Run selected template',
      runCompensation: 'Run failure recovery + compensation'
    }
  },
  parseBatchCenter: {
    title: 'Batch Parse Center',
    summary: 'Handle batch creation, template download, file import, failure retry, and report-catalog SQL parsing on an independent page.'
  },
  parseStatisticsCenter: {
    title: 'SQL Parse Statistics Center',
    summary: 'Review parse overview, issue distribution, priority matrix, and SQL/report statistics on an independent page.',
    severityView: 'Severity view',
    priorityView: 'Priority view',
    logicalObjectView: 'Logical object view',
    parseStatusSamples: 'Parse status samples',
    severity: 'Severity',
    issueScenes: 'Issue scenes',
    affectedSql: 'Affected SQL',
    affectedIssues: 'Issues',
    urgentScenes: 'Urgent scenes',
    priority: 'Priority',
    highestScore: 'Highest score',
    logicalObjectType: 'Type',
    logicalObjectKey: 'Object key',
    samples: 'Samples',
    resultStatus: 'Result status',
    cacheHit: 'Cache hit',
    rewrite: 'Rewrite',
    acceleration: 'Acceleration',
    errorLabels: {
      overview: 'Parse overview',
      issueScenes: 'Issue distribution',
      sqlStats: 'By SQL',
      reportStats: 'By report',
      priorityMatrix: 'Priority matrix',
      importantUrgent: 'Important or urgent list'
    }
  },
  assetCatalog: {
    title: 'Data Asset Catalog',
    summary: 'Browse datasource, schema, table, logical-view, and db-view lists with detail evidence.',
    eyebrow: 'data asset catalog',
    workspaceSummary: 'Filter by asset type, inspect lists and detail evidence, and keep metadata snapshots, lineage, physical mappings, dependencies, and SQL candidates in one workspace.',
    filters: {
      eyebrow: 'asset filters',
      title: 'Filters and refresh',
      summary: 'Filter state and catalog state stay separate; refreshing does not change backend asset facts.',
      schemaPlaceholder: 'Used for table list only'
    },
    actions: {
      refresh: 'Refresh catalog'
    },
    catalog: {
      eyebrow: 'catalog tabs',
      title: 'Asset catalog',
      summary: '{count} entries in the current type'
    },
    detail: {
      eyebrow: 'asset detail',
      title: 'Detail and evidence',
      summary: 'Details, health status, snapshots, and drill-down evidence stay in the persistent detail region.',
      connectionEndpoint: 'Connection endpoint',
      credentialMode: 'Credential mode',
      lastFailureReason: 'Last failure reason'
    },
    states: {
      emptyCatalog: 'No catalog entries match the current filters.',
      selectAsset: 'Select an asset from the left to render the detail panel.'
    },
    health: {
      eyebrow: 'freshness / sla / heat',
      title: 'Freshness, SLA, and heat proxy',
      usageHeatProxy: 'Usage heat proxy'
    },
    snapshot: {
      eyebrow: 'snapshot evidence',
      title: 'Metadata snapshot evidence'
    },
    relatedSql: {
      eyebrow: 'related sql',
      title: 'Related SQL candidates',
      summary: 'Read-only candidates; SQL is not executed.',
      boundary: 'The current repo-side baseline has no dedicated logical-object to SQL endpoint, so candidates are aligned by logical-view `viewCode` and parse-statistics `reportCode`.',
      empty: 'No related SQL candidates matched this logical view yet.'
    }
  },
  routingGovernance: {
    title: 'Routing Execution Evidence',
    summary: 'Review routing calibration, historical decisions, and comment-protocol summaries through a read-only evidence surface.',
    eyebrow: 'routing execution evidence',
    pageTitle: 'Routing execution evidence and decision history',
    boundarySummary: 'This page only consumes read-only route-calibration and query-history.routeDecision evidence instead of pretending to be a rule-configuration center.',
    filters: {
      eyebrow: 'routing filters',
      title: 'Evidence scope and actions'
    },
    fields: {
      traceLimit: 'Trace limit',
      traceId: 'Trace ID',
      auditEvents: 'Audit events',
      lastSeenAt: 'Last seen at'
    },
    actions: {
      refresh: 'Refresh routing evidence',
      viewPolicySource: 'View current policy source',
      createRule: 'Create rule',
      editRule: 'Edit rule',
      openParseRecord: 'Open parse-record page'
    },
    tabs: {
      calibration: 'Calibration',
      commentProtocol: 'Comment protocol',
      recentTraces: 'Recent traces'
    },
    policy: {
      eyebrow: 'current policy',
      title: 'Current policy snapshot'
    },
    comment: {
      eyebrow: 'comment protocol',
      title: 'Comment protocol summary'
    },
    traces: {
      eyebrow: 'routing-route-decision',
      title: 'Routing decision history',
      summary: 'Trace detail opens in a dialog and raw route evidence opens in a drawer.',
      state: '{count} trace rows currently loaded'
    },
    detail: {
      dialogTitle: 'Routing decision detail',
      historyState: '{count} trace history rows currently loaded'
    },
    rawDrawerTitle: 'Raw routing evidence'
  },
  recommendationCenter: {
    title: 'Recommendation Center',
    summary: 'Review recommendation categories, benefit or risk, dispatch status, and traceability links.',
    eyebrow: 'recommendation center',
    pageTitle: 'Recommendation Center',
    boundarySummary: 'This center consumes recommendation, dispatchEvents, and traceability evidence while SQLForge manages suggestions, events, and callbacks only without executing recommended SQL or loading data.',
    filters: {
      eyebrow: 'recommendation filters',
      title: 'Tenant and refresh'
    },
    actions: {
      refresh: 'Refresh center',
      openRouting: 'Open routing governance',
      openParse: 'Open SQL Parse',
      openHistory: 'Open history page'
    },
    list: {
      eyebrow: 'recommendation categories',
      title: 'Recommendation categories',
      summary: '{count} recommendations for the current tenant'
    },
    detail: {
      eyebrow: 'recommendation detail',
      title: 'Benefit, risk, and SQL detail'
    },
    fields: {
      benefitLevel: 'benefit',
      riskLevel: 'risk',
      dispatch: 'dispatch',
      expectedGain: 'Expected gain',
      riskSummary: 'Risk summary',
      reason: 'Reason',
      sourceSql: 'Source SQL'
    },
    states: {
      selectRecommendation: 'Select a recommendation to inspect its detail.',
      loadingDetail: 'Loading recommendation detail…',
      emptyDetail: 'No recommendation is available to display yet.',
      waitingCallback: 'Waiting for an external callback.'
    },
    tabs: {
      summary: 'Summary',
      sqlEvidence: 'SQL evidence',
      dispatchContract: 'Dispatch contract',
      traceability: 'Traceability',
      dispatchEvents: 'Dispatch events'
    },
    dispatch: {
      boundary: 'The current collaboration boundary is fixed at coordinationMode=PULL_ONLY: external modules own real data loading, prewarm execution, and storage changes while SQLForge keeps recommendation plus dispatch callback evidence only.'
    }
  },
  accessCenter: {
    title: 'Open Access',
    summary: 'Review API, JDBC Agent, Java SDK, access strategies, and access-audit samples.',
    eyebrow: 'access workbench',
    pageTitle: 'Open access and audit samples',
    boundarySummary: 'The default view shows the access-audit table, while channels, JDBC Agent, SDK, and policy boundaries move into tabs. Actions without write APIs remain explicit placeholders.',
    filters: {
      eyebrow: 'access filters',
      title: 'Access scope and actions'
    },
    fields: {
      accessChannel: 'Access channel',
      historyReport: 'History / Report',
      mode: 'Mode'
    },
    actions: {
      refresh: 'Refresh access evidence',
      createStrategy: 'Create access strategy',
      editStrategy: 'Edit strategy',
      boundaryHelp: 'Boundary help'
    },
    tabs: {
      audit: 'Access audit',
      channels: 'Channels',
      jdbc: 'JDBC Agent',
      sdk: 'SDK / Client',
      policy: 'Policy boundary'
    },
    audit: {
      eyebrow: 'access audit sample',
      title: 'Access-audit samples',
      summary: 'Render query-history accessChannel filter results in a table by default.',
      boundary: 'The repository does not yet expose a dedicated frontend controller for `GET /api/governance/access-audit`, so this page currently renders audit samples through the query-history surface filtered by `accessChannel`.',
      state: '{count} access-audit sample rows currently loaded'
    },
    channels: {
      eyebrow: 'access channels',
      title: 'Access channels'
    },
    jdbc: {
      state: '{count} JDBC Agent modes currently loaded'
    },
    detail: {
      dialogTitle: 'Access audit detail'
    },
    rawDrawerTitle: 'Raw access evidence',
    policy: {
      dialogTitle: 'Access boundary guide'
    }
  },
  alertCenter: {
    title: 'Alert Center',
    summary: 'Review derived alerts, simulated ACK state, and notify-simulated outcomes.',
    pageTitle: 'Alert center and notification state',
    refactorSummary: 'The dedicated read path {readPath} has a baseline, but this page still derives alerts from backlog, dispatch events, and important-or-urgent SQL while keeping ACK and notify explicitly simulated until the dedicated path is wired.',
    controlsTitle: 'Alert refresh and simulated actions',
    controlsSummary: 'Refresh derived alerts by tenant; create-rule and notification-policy actions still expose the missing write-interface boundary.',
    listTitle: 'Alert list',
    detailTitle: 'Detail, ACK, and notify state',
    metrics: {
      total: 'Total alerts',
      open: 'Open',
      high: 'High severity',
      simulated: 'Notify simulated'
    },
    fields: {
      alertId: 'Alert ID',
      ackStatus: 'ACK status',
      notifyStatus: 'Notify status',
      ackMode: 'ACK mode'
    },
    actions: {
      refresh: 'Refresh alerts',
      createRule: 'Create alert rule',
      editNotify: 'Edit notify strategy',
      ack: 'Simulate ACK',
      clearAck: 'Clear simulated ACK'
    },
    derived: {
      backlogTitle: 'Governance backlog alert',
      backlogSummary: 'Current failed={failed}, pending={pending}.',
      dispatchTitle: 'Dispatch event requires attention',
      parseTitle: 'Parse-priority alert'
    },
    placeholder: {
      createTitle: 'Create alert rule is not writable yet',
      createCapability: 'Create alert rule',
      createReason: 'The repository does not expose a dedicated alert-rule write API, and this page still derives evidence from backlog, dispatch, and important-or-urgent SQL.',
      createNextStep: 'If alert-configuration APIs are added later, connect the create form here.',
      editTitle: 'Edit notification strategy is not writable yet',
      editCapability: 'Edit notification strategy',
      editReason: 'ACK and notify are explicitly simulated on this page and should not pretend to be a live notification control plane.',
      editNextStep: 'Introduce real notification APIs and audit coverage before wiring editing actions.'
    },
    messages: {
      noAlert: 'No alert is available to display.'
    }
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
