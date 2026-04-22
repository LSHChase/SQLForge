export default {
  common: {
    appName: 'SQL生命周期治理平台',
    platformTagline: 'governance cockpit',
    currentWorkspace: '当前工作区',
    workspaceLabel: 'workspace',
    workspaceSummary: '{tenant} · 默认引擎 {engine}',
    brandSummary: '面向 SQL 查询、解析、压测、加速与审计的统一治理工作台。',
    sidebarLabel: 'navigation',
    runtimeLabel: 'runtime state',
    navGroups: {
      main: '主线业务',
      governanceHistory: '治理历史',
      governanceOps: '治理运维',
      temporary: '临时交付'
    },
    defaultEngine: '默认引擎',
    backupEngine: '备用引擎',
    currentTenant: '当前租户',
    desktopMode: '桌面端基线',
    temporaryPage: '临时页',
    nonProductionOnly: '仅非生产',
    switchToDark: '切换深色',
    switchToLight: '切换浅色'
  },
  dashboard: {
    title: '研发驾驶舱',
    summary: '汇总 SQL 生命周期治理状态、风险建议与五大功能入口。',
    eyebrow: 'runtime health',
    heroTitle: '让每一条 SQL 在进入生产前先通过治理总览。',
    heroSummary:
      '首页集中呈现查询负载、解析稳定性、压测通过、加速命中与审计信号，帮助你快速判断平台是否处于可发布、可追踪、可处置状态。',
    heroPrimary: '进入 SQL 查询',
    heroSecondary: '查看压测报告',
    heroFootnote: 'Dark-mode-native dashboard baseline',
    metricLabel: '关键指标',
    metrics: {
      queryVolume: '总查询量',
      parseSuccess: '解析成功率',
      benchmarkPass: '压测通过率',
      accelerationHit: '加速命中率',
      auditSignal: '审计/异常事件'
    },
    quickEntryTitle: '核心工作流入口',
    quickEntrySummary: '五个主功能继续独立存在，Dashboard 只做摘要、分发和建议。',
    healthTitle: '平台健康与风险',
    healthSummary: '先给出结论，再给出下一步动作。',
    activityTitle: '最近活动',
    activitySummary: '保留运行时间线视角，避免首页退化成静态介绍页。',
    nextTitle: '建议动作',
    nextSummary: '根据当前风险与运行状态，直接进入对应业务页面处理。'
  },
  deliveryProgress: {
    title: 'AI交付进度',
    summary: '仅在研发/交付阶段展示 AI 编码任务推进状态、验证结果与模块进度。',
    eyebrow: 'delivery strict mode',
    heroTitle: '把 AI 编码任务的推进真值固定在权威台账之上。',
    heroSummary:
      '该页面只读展示 `tasks.md`、`tasks-done.md`、验证日志与执行计划派生出的交付快照，不替代正式业务首页，也不维护平行状态源。',
    visibilityLabel: 'production hidden',
    visibilityTitle: '该页面仅在非生产环境展示',
    visibilitySummary: '项目投产后默认隐藏入口，历史记录继续保留在任务台账、验证日志和 Git 回写中。',
    summaryTitle: '状态总览',
    summaryDescription: '先看任务状态分布，再下钻具体执行、模块进度与验证证据。',
    activeTasksTitle: '活动任务',
    activeTasksDescription: '当前进行中、待办、待审与阻塞任务都来自活动任务台账快照。',
    moduleTitle: '模块进度',
    moduleDescription: '按任务 ID 所在执行域汇总完成率，便于快速判断推进密度与堵点。',
    recentChangesTitle: '最近变更',
    recentChangesDescription: '将任务进度日志、归档记录与验证日志合并成一个只读时间线，便于回溯最近一次真实动作。',
    blockedTitle: '阻塞与待决原因',
    blockedDescription: '优先列出显式 blocked 任务；若当前无 blocked，则回退展示最新进展日志中仍未闭合的待决项。',
    blockedFootnote: '当前列表包含台账中已显式标记为 blocked 的任务。',
    pendingFootnote: '当前无显式 blocked 任务，以下为从最新进度日志提取的待决原因。',
    dependencyTitle: '依赖链',
    dependencyDescription: '把活动任务依赖拆开展示，区分已在台账、仅在计划和未被跟踪的依赖。',
    completedTitle: '最近完成',
    completedDescription: '已归档任务与 commit subject 只从 `tasks-done.md` 派生，不手填。',
    validationTitle: '最近验证',
    validationDescription: '验证时间线只读展示 `validation-log.md` 的最新记录。',
    changeKind: {
      progress: '进度回写',
      done: '任务归档',
      validation: '验证记录'
    },
    cards: {
      todo: '待开始',
      inProgress: '执行中',
      inReview: '待验收',
      blocked: '已阻塞',
      done: '已完成'
    },
    status: {
      todo: '待开始',
      in_progress: '执行中',
      in_review: '待验收',
      blocked: '已阻塞',
      done: '已完成',
      planned: '计划中',
      untracked: '未跟踪'
    },
    dependencySource: {
      ledger: '来源: 任务台账',
      plan: '来源: 执行计划',
      external: '来源: 待人工补齐'
    },
    runtime: {
      modePill: '模式 {mode}',
      modeLabel: '当前模式',
      flagLabel: '开关状态',
      scopeLabel: '可见范围',
      scope: '非生产可见',
      reasonNonProduction: '当前为非生产构建，且未通过 `VITE_ENABLE_DELIVERY_PROGRESS=false` 关闭该临时页面入口。',
      reasonFlagDisabled: '当前构建虽然不是生产模式，但显式设置了 `VITE_ENABLE_DELIVERY_PROGRESS=false`，因此入口不会注册。',
      reasonProduction: '生产构建不会注册该临时页面路由；投产后默认不可见。',
      flag: {
        enabled: '已显式启用',
        disabled: '已显式关闭',
        default: '默认开启'
      }
    },
    meta: {
      priority: '优先级',
      dependsOn: '依赖',
      totalTasks: '个任务',
      blockedSource: '来源: 最新进度日志',
      unresolvedCount: '未闭合依赖 {count} 个'
    },
    empty: {
      blockedTitle: '当前没有待展示的阻塞项',
      blockedDescription: '若后续任务进入 `blocked`，或最新进度日志写入新的待决原因，这里会自动刷新。',
      dependencyTitle: '当前没有依赖链',
      dependencyDescription: '待活动任务补入 `Depends on` 后，这里会自动展示依赖结构。'
    }
  },
  sqlQuery: {
    title: 'SQL查询',
    summary: '统一提交 SQL、选择执行策略并进入后续治理链路。'
  },
  parseRecord: {
    title: '解析记录',
    summary: '跟踪解析结果、改写状态、失败样本与历史诊断。'
  },
  repairEvidence: {
    title: '修复证据',
    summary: '按 trace、task、report 反查治理链路，确认补偿与修复结果。'
  },
  auditForensics: {
    title: '审计取证',
    summary: '串联补偿、修复、回写与历史事件，形成可分页的取证链路。'
  },
  auditTroubleshooting: {
    title: '故障处置',
    summary: '汇总失败类型、补偿状态、回写状态与队列影响，并给出处置动作与验收信号。'
  },
  runtimeGates: {
    title: '运行时门禁',
    summary: '汇总阶段入口、交付、合规门禁证据以及当前仍未闭口的退出阻塞项。'
  },
  recoveryDrill: {
    title: '恢复演练',
    summary: '沉淀备份范围、恢复目标、责任边界与恢复后必过验收检查。'
  },
  benchmark: {
    title: '压测报告',
    summary: '查看基线、峰值延迟、回归差异与准入判断。'
  },
  acceleration: {
    title: '加速配置',
    summary: '管理加速策略、物化视图建议与命中表现。'
  },
  system: {
    title: '系统管理',
    summary: '维护租户、路由默认值、审计保留与全局控制项。'
  }
}
