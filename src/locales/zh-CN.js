export default {
  common: {
    appName: 'SQL生命周期治理平台',
    platformTagline: 'governance cockpit',
    currentWorkspace: '当前工作区',
    workspaceLabel: 'workspace',
    workspaceSummary: '{tenant} · 默认引擎 {engine}',
    brandSummary: '面向 SQL 查询、解析、压测、加速与审计的统一治理工作台。',
    sidebarLabel: 'workflow entry',
    runtimeLabel: 'runtime state',
    defaultEngine: '默认引擎',
    backupEngine: '备用引擎',
    currentTenant: '当前租户',
    desktopMode: '桌面端基线',
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
  sqlQuery: {
    title: 'SQL查询',
    summary: '统一提交 SQL、选择执行策略并进入后续治理链路。'
  },
  parseRecord: {
    title: '解析记录',
    summary: '跟踪解析结果、改写状态、失败样本与历史诊断。'
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
