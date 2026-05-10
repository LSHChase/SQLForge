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
    switchToLight: '切换浅色',
    actions: {
      copy: '复制',
      format: '格式化',
      viewRawJson: '查看原始 JSON',
      viewRawEvidence: '查看原始证据'
    },
    helpMark: '?',
    fields: {
      tenant: '租户',
      datasource: '数据源',
      schema: 'Schema',
      status: '状态',
      targetEngine: '目标引擎',
      serviceCode: '服务编码',
      historyId: 'History ID',
      traceId: 'Trace ID',
      reportCode: '报表编码',
      type: '类型',
      submittedAt: '提交时间',
      submittedBy: '提交人',
      title: '标题',
      summary: '说明',
      taskType: '任务类型',
      taskId: '任务 ID',
      verdict: '裁决',
      format: '格式',
      scenario: '场景',
      report: '报告',
      resultStatus: '结果状态'
    }
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
    operatorHeroTitle: '首页总览与主线待办',
    operatorHeroSummary: '首页重新聚合解析、治理、推荐与协同证据，但仍只消费当前仓库已有的已审计接口，不把局部样本夸大成全租户最终事实。',
    operatorFocusEyebrow: 'operator focus',
    openRisksTitle: '开放风险',
    openRisksSummary: '失败消息、dispatch failure、urgent SQL 与高风险 recommendation 会在这里合并成一个操作焦点。',
    coreKpiTitle: '核心 KPI',
    primaryEntriesTitle: '五大主入口',
    platformHealthRiskTitle: '平台健康与风险',
    issueDistributionTitle: '问题分布',
    recentActivityTitle: '最近活动',
    nextStepsTitle: '下一步建议',
    evidenceEyebrow: 'static evidence',
    evidenceBoundaryTitle: '证据边界',
    evidenceBoundarySummary: '首页只展示当前接口、窗口或协同模式可证明的事实，避免把样本扩写成全局结论。',
    evidenceRows: {
      parseOverview: {
        label: '解析总览',
        detail: '来自 parse-statistics overview 的当前样本，不代表全量租户历史。'
      },
      queryWindow: {
        label: '查询窗口',
        detail: '来自 query-history 当前页窗口，成功率、失败率和命中率均按窗口计算。'
      },
      messageStats: {
        label: '治理消息',
        detail: '来自 governance admin message stats，只合并 pending 与 failed 风险。'
      },
      dispatchMode: {
        label: '协同模式',
        detail: '继续显式展示 PULL_ONLY 或后端返回模式，不伪装主动下发。'
      }
    },
    actions: {
      openQueryWorkbench: '进入查询工作台',
      openSqlParse: '进入 SQL解析',
      refreshOverview: '刷新总览'
    },
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
    panorama: {
      kicker: 'project panorama',
      title: '项目全景',
      summary: '把愿景、里程碑、术语和规则速查固定在首页，避免驾驶舱只剩运行指标。',
      cardLabel: 'project index',
      cards: [
        {
          title: '项目目标',
          summary: '让每一条 BI SQL 在上线前经过解析、压测、改写、路由与加速治理，确保性能可预期、数据一致可信赖。',
          items: ['统一查询、解析、压测、加速与审计入口', '以后端权威校验、历史可追溯和规则入库为底座', '支持单区域起步并向多区域扩展预留']
        },
        {
          title: '项目规划',
          summary: '执行顺序按 Phase A-F 展开，先收口文档真值，再逐步补齐前后端、运行时门禁与交付闭环。',
          items: ['Phase D 收口查询执行与治理主链路', 'Phase E 构建驾驶舱、业务页面与交付视图', 'Phase F 接入部署、CI、runtime smoke 和恢复治理']
        },
        {
          title: '名词解释',
          summary: '统一项目中的角色和对象语义，减少前后端、架构与运维之间的理解漂移。',
          items: ['治理链路: query -> parse -> benchmark -> acceleration -> audit', '任务真值: tasks.md / tasks-done.md / validation-log / git', '研发驾驶舱: 正式首页摘要层，不替代各主业务页']
        },
        {
          title: '架构原则速查',
          summary: '规则库要求代码、文档、验证和交付回写保持同步，不允许把长期约束留在会话记忆里。',
          items: ['Java 8 + Spring Boot 2.x 是后端硬约束', '页面负责摘要与分发，权威判断和历史由后端承担', '非 trivial 任务必须走 foreman + task audit 审计链']
        }
      ]
    },
    architecture: {
      kicker: 'architecture design',
      title: '架构设计',
      summary: '首页以摘要方式固化 C4、服务拓扑、契约、数据模型、部署和测试设计，变更继续以 ADR 为准。',
      cardLabel: 'architecture index',
      cards: [
        {
          title: '总体架构',
          summary: '以 C4 Level 1-4 组织系统、容器、组件和关键模块，区分正式产品路径与交付辅助路径。',
          items: ['产品首页保留 `/dashboard`，五大业务页独立承载主流程', '后端坚持 controller -> service -> domain/infrastructure 分层', '领域边界以查询治理、优化、压测、系统管理等服务拆分']
        },
        {
          title: '服务拓扑',
          summary: '前后端分离部署，治理相关能力围绕查询执行、解析、优化、压测、审计与系统配置协作。',
          items: ['query-execution 承接 SQL 提交与治理编排', 'optimization / benchmark 独立演进并通过契约接入主链路', 'system / audit / delivery 负责配置、取证和交付观察']
        },
        {
          title: '接口契约与数据模型',
          summary: '所有重要行为通过明确契约和后端校验进入系统，历史、导出和审计使用可追溯关联键串联。',
          items: ['HTTP 返回明确 JSON 错误，不以模糊字符串替代', 'MySQL 是主持久化方向，历史对象支持查询、筛选、分页、导出', '实体不跨服务直接传输，跨域通过 DTO 和服务层编排']
        },
        {
          title: '数据生命周期',
          summary: '围绕冷热分层、不可变结果和长期历史留存设计治理数据，避免把浏览器状态当成事实来源。',
          items: ['结果主体视为证据，不允许随意重写', '审计日志至少保留 180 天并持续可查询', '加速、压测、修复与审计结果通过统一关联键回溯']
        },
        {
          title: '部署与流程',
          summary: '部署拓扑以华为云单区域起步，业务流程覆盖正常路径、异常回滚、补偿和加速支路。',
          items: ['前后端可独立构建与部署，路径兼容 amd64/arm64', '流程链路覆盖 query、parse、benchmark、acceleration、audit', '运维侧持续记录日志、告警、备份和恢复证据']
        },
        {
          title: '测试架构',
          summary: '验证分为构建、分层测试、runtime smoke、task audit 和交付回写，不把“已实现”当成“已验证”。',
          items: ['前端至少通过 build 与页面结构检查', '后端通过模块测试、契约测试和运行时 smoke 门禁', '架构变更需同步 ADR、验证日志与任务台账']
        }
      ]
    },
    progress: {
      kicker: 'delivery truth',
      title: '进度管理',
      badge: 'authoritative snapshot',
      openDelivery: '进入交付进度页',
      deliveryHidden: '临时交付页在生产环境默认隐藏',
      sourceTitle: '真实来源',
      sourceSummary: '任务台账、验证日志和主计划合并派生为只读快照；首页不维护第二份状态源。',
      cards: {
        active: '活动任务',
        activeDetail: '当前仍在 `tasks.md` 中推进的 todo / in_progress / in_review / blocked 总量。',
        done: '已归档任务',
        doneDetail: '来自 `tasks-done.md` 的累计完成数，用于判断真实交付沉淀。',
        validation: '验证记录',
        validationDetail: '来自 `docs/quality/validation-log.md` 的最近验证累计，反映质量门禁密度。',
        completion: '整体完成率',
        completionDetail: '按活动任务与已归档任务合并计算，用于观察当前交付波次收口程度。'
      },
      modulesTitle: '阶段完成度',
      modulesSummary: '按任务 ID 所在执行域汇总完成率，快速判断哪一阶段在推进、哪一阶段仍在堆积。',
      moduleMeta: '共 {total} 个任务，已完成 {done}，执行中 {progress}',
      recentTitle: '最近动作',
      recentSummary: '合并任务进度日志、归档记录和验证日志，保留最近一次真实动作时间线。',
      dependenciesTitle: '阻塞与依赖链',
      dependenciesSummary: '优先展示当前阻塞/待决原因，再看活动任务未闭合的依赖链。',
      blockerLabel: '当前待决',
      noBlockers: '当前无显式阻塞项；如出现新的待决原因，会从最新进度日志自动浮现。',
      noDependencies: '当前活动任务未形成可展示的依赖链。'
    },
    compliance: {
      kicker: 'compliance center',
      title: '合规中心',
      summary: '把等保专项要求直接挂到驾驶舱首页，明确身份、隔离、审计、加密和备份恢复的当前基线。',
      sourceTitle: '合规真值',
      sourceSummary: '当前板块只读映射 `docs/security/compliance.md` 中的 `R-111`~`R-115`，不在前端维护平行规范。',
      cards: [
        {
          ruleId: 'R-111',
          title: '身份鉴别',
          summary: '所有用户操作必须由后端完成身份校验，管理员与普通用户使用不同权限模型。',
          items: ['HTTP/API 请求必须携带可验证身份凭证', '认证失败或未登录请求返回明确 JSON 错误', '认证失败和越权行为都必须进入审计链']
        },
        {
          ruleId: 'R-112',
          title: '访问控制',
          summary: '系统以租户 ID 做数据隔离，路由、执行、导出和压测都要重新校验授权范围。',
          items: ['所有核心请求显式携带租户上下文', '数据源、查询任务、审计记录和导出记录都绑定租户', '默认拒绝无授权访问，不允许隐式放行']
        },
        {
          ruleId: 'R-113',
          title: '安全审计',
          summary: 'SQL 操作、登录登出和权限变更必须入审计日志，且至少保留 180 天、不可改删。',
          items: ['审计记录包含时间、租户、用户、对象、结果、耗时与链路 ID', '敏感内容必须脱敏或加密后再记录', '审计日志表需独立备份并限制写后改删']
        },
        {
          ruleId: 'R-114',
          title: '加密存储',
          summary: '数据库密码、API 密钥等敏感配置必须加密存储，禁止明文落库。',
          items: ['持久化前统一执行 AES-256 或等效加密', '配置文件、日志、异常栈、导出文件不得泄露明文敏感数据', '密钥管理与业务数据分离，可接独立密钥服务']
        },
        {
          ruleId: 'R-115',
          title: '备份恢复',
          summary: 'MySQL 生产环境必须具备主从或等效高可用方案，满足 RPO 与 RTO 目标并加密备份。',
          items: ['备份至少包含全量和必要增量/binlog 策略', '审计日志和核心元数据进入优先恢复清单', '恢复演练结果需要可追溯记录']
        }
      ]
    },
    rulebook: {
      kicker: 'codex rulebook',
      title: 'Codex 规则库',
      summary: '规则库板块只做只读摘要，强调 append-only、规则入库和任务审计链，不把长期约束留在 prompt 和记忆里。',
      cardLabel: 'rule cluster',
      sourceTitle: '规则来源',
      sourceSummary: '规则分类、编号和扩展状态来自 `docs/rules/codex-rules.md` 与合规基线文档的当前仓库真值。',
      cardsSummary: {
        baseline: '基线规则',
        baselineDetail: '基于 `R-001`~`R-115` 的架构与工程硬约束数量。',
        validation: '扩展验证规则',
        validationDetail: '`R-116+` 的验证、阶段门禁和治理追加规则数量。',
        compliance: '合规规则',
        complianceDetail: '来自安全合规文档的等保专项规则数量。',
        sources: '来源文档',
        sourcesDetail: '当前首页摘要直接依赖的规则/合规权威文档数量。'
      },
      cards: [
        {
          title: '文档与治理',
          summary: '规则要求执行前先读文档、维护任务台账、记录 closeout，并通过 append-only 历史账本保存长期约束。',
          items: ['文档优先于 prompt 和记忆', '长期规则必须入 `docs/` 和历史账本', '非 trivial 任务必须走 foreman / task audit / git 审计链']
        },
        {
          title: '架构与工程',
          summary: '前后端分离、Java 8 + Spring Boot 2.x、分层后端、MyBatis XML 和独立部署是当前工程硬边界。',
          items: ['前端固定 Vue 3 + JavaScript + CSS', '后端固定 Java 8 + Spring Boot 2.x', '领域目录 + 分层子目录是默认后端结构']
        },
        {
          title: '页面与边界',
          summary: '首页负责摘要与建议，主流程保持独立页面；前端可预校验，但后端永远是规则与历史的权威边界。',
          items: ['页面按上下文、状态、结果、下一步组织', '不能把多个核心流程继续堆在一个长页面', '已交付能力必须在 operator 页面可见']
        },
        {
          title: '验证与扩展',
          summary: '规则库后半段把阶段门禁、任务验证、delivery closeout 和严格模式约束继续追加为机器可审计规则。',
          items: ['阶段入口、阶段交付和渐进等保都有验证要求', '前端 build、知识 lint、task audit 是基础收口动作', '规则新增只能追加，不能覆写历史语义']
        }
      ]
    },
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
      dependencyDescription: '待活动任务补入 `Depends on` 后，这里会自动展示依赖结构。',
      progressLog: '暂无进度日志'
    }
  },
  sqlQuery: {
    title: 'SQL查询',
    summary: '统一提交 SQL、选择执行策略并进入后续治理链路。',
    hero: {
      eyebrow: 'query workbench'
    },
    metrics: {
      resultRows: '结果行',
      resultRowsDetail: '执行后展示当前响应返回行数，不替代后端历史总量。',
      validationTips: '预校验提示',
      validationTipsDetail: '前端只做输入提醒，最终校验仍以后端为准。',
      recentRuns: '最近执行',
      recentRunsDetail: '仅展示当前会话最近 6 次执行摘要。',
      pending: '待执行',
      review: '需复核',
      ready: '就绪',
      sessionOnly: 'session'
    },
    resultTabs: {
      access: '数据访问解析',
      history: '历史关联'
    },
    access: {
      queryDateStatus: 'query_date 状态',
      queryDateStart: 'query_date 起始',
      queryDateEnd: 'query_date 结束',
      queryDateFields: 'query_date 字段',
      bindingMode: '绑定模式',
      logicalObjects: '命中对象',
      parseStatus: '轻量解析状态',
      commentContext: '注释上下文'
    },
    historyAssociation: {
      sqlFingerprint: 'SQL 指纹',
      contractStage: '契约阶段',
      implementationStage: '实现阶段',
      downloadUrl: '下载地址',
      historyBoundary: '历史边界',
      backendHistory: '历史写入、导出和审计关联以后端 query-history 为准。'
    }
  },
  sqlHistory: {
    title: 'SQL历史',
    summary: '查询已落库的 query-history 列表、关联 trace 与详情证据。',
    actions: {
      refresh: '查询/刷新',
      lookup: '精确反查',
      clear: '清空条件',
      openRepairEvidence: '打开修复证据',
      openAuditForensics: '打开审计取证',
      exportEvidence: '导出取证',
      viewRawEvidence: '查看原始证据',
      copy: '复制'
    },
    options: {
      all: '全部',
      default: '默认'
    },
    filters: {
      tenant: '租户',
      tenantPlaceholder: '默认使用路由租户或 tenant-a',
      reportKey: 'SQL/报表标识',
      reportKeyPlaceholder: '输入 reportCode 或 SQL 指纹',
      datasource: '数据源',
      datasourcePlaceholder: '输入数据源编码',
      stage: '阶段',
      stagePlaceholder: '输入 stage',
      bizDate: '业务日期',
      queryDateStart: '查询日期起始',
      queryDateEnd: '查询日期结束',
      datePlaceholder: '选择日期',
      status: '执行状态',
      logicalObjectType: '逻辑对象类型',
      accessChannel: '接入渠道',
      engine: '目标引擎',
      submittedBy: '提交人',
      submittedByPlaceholder: '输入提交人',
      submittedStart: '提交起始',
      submittedEnd: '提交结束',
      cacheHit: '缓存命中',
      rewriteApplied: '轻量改写',
      accelerationApplied: '加速命中',
      parameterizedSql: '参数化 SQL',
      sortBy: '排序字段',
      sortOrder: '排序方向',
      traceIdPlaceholder: '输入 Trace ID',
      taskIdPlaceholder: '输入 Task ID',
      reportIdPlaceholder: '输入 Report ID',
      selectPlaceholder: '请选择',
      eyebrow: 'history filters',
      title: '历史筛选与精确反查',
      summary: '筛选项全部映射到 query-history 已有查询参数；默认空筛选不改变历史查询契约。'
    },
    metrics: {
      label: 'SQL 执行历史摘要',
      currentPage: '当前页记录',
      total: '执行历史总数',
      success: '成功',
      nonSuccess: '异常/部分成功',
      accessChannels: '接入渠道',
      queryDateResolved: 'query_date 已解析',
      parameterizedSql: '参数化 SQL'
    },
    queryStatus: {
      idle: '未查询',
      loading: '加载中',
      success: '已刷新',
      error: '查询失败'
    },
    table: {
      kicker: '执行历史表',
      title: '执行记录',
      historyId: 'History ID',
      reportKey: 'SQL/报表标识',
      requestTenant: '请求租户',
      datasource: '数据源',
      stage: '阶段',
      status: '执行状态',
      accessChannel: '接入渠道',
      targetEngine: '目标引擎',
      queryDate: 'query_date',
      sqlState: 'SQL 状态',
      governanceHits: '治理命中',
      submittedBy: '提交人',
      submittedAt: '提交时间',
      auditEventCount: '审计事件数'
    },
    states: {
      loading: '正在加载 SQL 执行历史',
      empty: '当前条件下没有 SQL 执行历史。',
      loadFailed: '列表加载失败，请查看上方错误信息。',
      errorTitle: 'SQL 历史查询失败',
      datasourceOptionsFallback: '数据源候选加载失败，保留手动输入。',
      noSignalEvidence: '当前详情没有可展示的解析、路由或上下文证据。'
    },
    messages: {
      lookupRequired: '至少输入 traceId、taskId、reportId 中的一项。',
      lookupEmpty: '没有命中记录。',
      lookupWithoutExecution: '命中了 trace，但没有 QUERY_EXECUTION 执行历史。'
    },
    detail: {
      title: '执行详情',
      historyId: 'History ID',
      traceId: 'Trace ID',
      resultId: 'Result ID',
      reportKey: 'SQL/报表标识',
      datasource: '数据源',
      datasourceType: '数据源类型',
      stage: '阶段',
      bizDate: '业务日期',
      queryDateStart: 'query_date 起始',
      queryDateEnd: 'query_date 结束',
      queryDateStatus: 'query_date 状态',
      status: '执行状态',
      accessChannel: '接入渠道',
      targetEngine: '目标引擎',
      submittedBy: '提交人',
      submittedAt: '提交时间',
      auditEventCount: '审计事件数'
    },
    execution: {
      cacheHit: '缓存命中',
      rewriteApplied: '轻量改写',
      accelerationApplied: '加速命中',
      returnedRows: '返回行数',
      errorCode: '错误码',
      errorMessage: '错误信息',
      routeDecision: '路由决策',
      cacheSummary: '缓存摘要'
    },
    sql: {
      sqlFingerprint: '执行指纹',
      templateFingerprint: '模板指纹',
      boundFingerprint: '绑定指纹',
      bindingMode: '绑定模式',
      bindingRender: '绑定渲染',
      parameterizedSql: '参数化 SQL',
      originalSql: '原始 SQL',
      templateSql: '模板 SQL',
      boundSql: '绑定 SQL'
    },
    governance: {
      cache: 'cache',
      rewrite: 'rewrite',
      acceleration: 'accel'
    },
    tabs: {
      overview: '执行概览',
      execution: '执行取证',
      sql: 'SQL 三态',
      signals: '解析与路由',
      refs: '关联证据',
      audit: '审计关联'
    },
    signals: {
      commentContext: '注释上下文',
      queryDateSummary: 'query_date 摘要',
      logicalObjectHits: '命中逻辑对象',
      structureParseSummary: '结构解析摘要',
      accessParseSummary: '数据访问解析摘要',
      bindingSummary: '参数绑定摘要',
      routeDecision: '路由决策',
      cacheSummary: '缓存摘要'
    },
    refs: {
      recommendationRefs: '推荐关联',
      benchmarkRefs: '压测关联',
      auditRefs: '审计关联',
      alertRefs: '告警关联'
    },
    audit: {
      service: '服务',
      operation: '操作',
      status: '状态',
      createdAt: '时间'
    },
    rawEvidence: {
      title: '原始证据'
    },
    export: {
      title: '导出 SQL 执行取证',
      format: '格式',
      includeTraceDetail: '包含 trace 详情',
      run: '执行导出'
    },
    footer: {
      currentPageCount: '当前页 {count} 条',
      totalCount: '总数 {count} 条',
      pageWindow: '第 {current}/{total} 页',
      lastQuery: '最近查询：{status} · {time}'
    }
  },
  parseRecord: {
    title: '解析历史查询',
    summary: '查询批量解析与报表导入历史，回看批次级解析记录。',
    issueSceneDetail: {
      actions: {
        viewDetail: '查看明细'
      },
      filters: {
        currentScene: '当前场景',
        report: '报表',
        logicalObject: '逻辑对象',
        clear: '清除'
      },
      sections: {
        reportDetail: '报表明细',
        logicalObjectDetail: '逻辑对象明细',
        sqlDetail: 'SQL 明细'
      },
      columns: {
        issueScene: '问题场景',
        affectedSql: '影响 SQL',
        severity: '严重度',
        reportCount: '报表数',
        logicalObjectCount: '逻辑对象数',
        ratio: '比例',
        actions: '操作',
        report: '报表',
        sqlCount: 'SQL 数',
        issueCount: '问题数',
        sceneIssueCount: '本场景问题数',
        logicalObjectKeys: '逻辑对象集合',
        object: '对象',
        reportCodes: '报表编码集合',
        reportSql: '报表 / SQL',
        priority: '优先级',
        logicalObjects: '逻辑对象',
        issueScenes: '问题场景',
        location: '定位'
      }
    }
  },
  governanceTrace: {
    tenantContext: '租户上下文',
    businessTenant: '业务租户',
    governanceTenant: '治理租户',
    lookupLimit: '返回数量',
    traceId: 'Trace ID',
    taskId: 'Task ID',
    reportId: 'Report ID',
    windowStart: '窗口开始',
    windowEnd: '窗口结束',
    clearCriteria: '清空条件',
    loadOlderEvidence: '加载更早证据',
    refreshQueueImpact: '刷新队列影响',
    matchedTraces: '命中 trace',
    compensationTraces: '补偿 trace',
    repairSignalChains: '修复信号链',
    reportLinkedTraces: '报告回写链',
    nonSuccessChains: '异常/修复链',
    olderEvidenceAvailable: '仍有更早证据链',
    olderRemediationAvailable: '仍有更早处置链',
    olderTracesAvailable: '仍有更早 trace',
    serviceCode: '服务编码',
    resourceType: '资源类型',
    resourceId: '资源标识',
    lastSeenAt: '最后发生时间',
    lookupMode: '命中维度',
    repairSignal: '修复信号',
    compensationTrace: '补偿链路',
    auditEvents: '审计事件',
    sqlFingerprint: 'SQL 指纹',
    errorCode: '错误码',
    targetEngine: '目标引擎',
    degraded: '降级',
    requestChain: '请求链路',
    task: '任务',
    report: '报告',
    fingerprint: '指纹',
    error: '错误',
    engine: '引擎',
    degradedRecovery: '降级恢复',
    queueImpact: '队列影响',
    queueTotal: '消息总数',
    queuePending: '待补偿',
    queueFailed: '失败消息',
    queueConsumed: '已消费',
    retryStatus: '重试状态',
    retriedCount: '重试数量',
    failedDelta: 'failed 降幅',
    repairOutcome: '修复结果',
    criteria: {
      traceId: 'Trace 反查',
      taskId: 'Task 反查',
      reportId: 'Report 反查',
      windowStart: '窗口开始',
      windowEnd: '窗口结束'
    }
  },
  repairEvidence: {
    title: '修复证据',
    summary: '按 trace、task、report 反查治理链路，确认补偿与修复结果。',
    refactorNote: '该页复用统一 trace lookup、命中列表与审计时间线组件，保留补偿、降级恢复和报告回写证据可见性。',
    lookupTitle: '追溯条件与命中结果',
    lookupSummary: '输入 trace、task 或 report 后查询治理追溯链，窗口字段会原样传给后端反查接口。',
    detailTitle: '补偿与修复证据明细',
    actions: {
      runLookup: '执行反查',
      openTroubleshooting: '打开处置决策'
    },
    messages: {
      requiredLookup: '至少输入 traceId、taskId、reportId 中的一项后再执行反查。',
      emptyCriteria: '输入 trace / task / report 后执行反查。',
      noMatches: '命中结果会展示对应 trace 列表，并允许继续下钻审计/修复时间线。',
      emptyDetail: '选择左侧命中 trace 后，这里会显示命中维度、修复信号和审计时间线。'
    }
  },
  auditForensics: {
    title: '审计取证',
    summary: '串联补偿、修复、回写与历史事件，形成可分页的取证链路。',
    refactorNote: '该页把失败链、补偿链、报告回写和审计事件收敛到同一套取证组件，并保留跨页 pivot。',
    lookupTitle: '取证条件与证据链命中',
    lookupSummary: 'trace、task、report 任一维度都可作为取证入口，分页 cursor 继续来自后端。',
    detailTitle: '审计取证详情与跨页 pivot',
    actions: {
      runLookup: '执行取证反查',
      openParseRecord: '跳回历史诊断',
      openRepairEvidence: '打开修复证据',
      openTroubleshooting: '打开处置决策'
    },
    messages: {
      requiredLookup: '至少输入 traceId、taskId、reportId 中的一项后再执行取证反查。',
      emptyCriteria: '输入 trace / task / report 后执行取证反查。',
      noMatches: '命中结果会展示失败链、补偿链与报告回写证据。',
      emptyDetail: '选择左侧命中 trace 后，这里会显示取证信号、历史事件和跨页跳转动作。'
    }
  },
  auditTroubleshooting: {
    title: '故障处置',
    summary: '汇总失败类型、补偿状态、回写状态与队列影响，并给出处置动作与验收信号。',
    refactorNote: '该页把 trace 取证与治理消息队列影响合并到统一处置视图，retry 仍调用后端权威入口。',
    lookupTitle: '故障范围、队列影响与决策输入',
    lookupSummary: '业务租户用于 trace 反查，治理租户用于 message stats 与 retry，不混写两个权限边界。',
    detailTitle: '处置动作与验收信号',
    actions: {
      runLookup: '执行处置反查',
      retryFailedMessages: '重试失败消息',
      openSystem: '打开治理 backlog',
      openRepairEvidence: '打开修复证据',
      openParseRecord: '回到历史诊断'
    },
    decision: {
      failureType: '失败类型',
      compensationState: '补偿状态',
      writeBackState: '回写状态',
      queueImpact: '队列影响',
      acceptanceState: '验收信号'
    },
    messages: {
      requiredLookup: '至少输入 traceId、taskId、reportId 中的一项后再执行处置决策反查。',
      emptyCriteria: '输入 trace / task / report 后执行处置决策反查。',
      noMatches: '命中结果会展示故障链与队列影响，并提供处置入口。',
      emptyDetail: '选择左侧命中 trace 后，这里会显示失败类型、补偿状态、回写状态和验收信号。'
    }
  },
  runtimeGates: {
    title: '运行时门禁',
    summary: '汇总阶段入口、交付、合规门禁证据以及当前仍未闭口的退出阻塞项。',
    heroEyebrow: 'phase-f runtime gates',
    heroTitle: '运行时门禁与退出阻断基线',
    heroSummary: '这里收口 Entry / Delivery / Compliance 三层门禁，避免 Phase-F 的 build、runtime、恢复和合规证据继续散在脚本与文档里。',
    heroNote: '当前重点不是再加展示页，而是明确哪些脚本已经成为阻断门禁、哪些仍是残余风险。',
    evidenceEyebrow: 'evidence map',
    evidenceTitle: '脚本与 workflow 入口',
    evidenceSummary: '仅列出仓库内脚本和 workflow 入口，不代表外部环境已经执行。',
    kpiTitle: '门禁 KPI 与证据边界',
    kpiSummary: '指标只统计本页列出的脚本、workflow 和残余阻塞，不外推运行时全局健康。',
    activityEyebrow: 'gate activity',
    activityTitle: '门禁活动流',
    activitySummary: '按 Entry、Delivery、Compliance 顺序展示阻断检查和脚本入口。',
    blockersEyebrow: 'residual blockers',
    blockersTitle: '仍需继续推进的阻塞项',
    blockersSummary: '这些仍是退出前要继续跟踪的风险项，不能写成已闭环事实。',
    gates: {
      entry: {
        title: 'Entry Gate',
        summary: '台账、治理编译物和仓库知识 lint 必须先对齐。'
      },
      delivery: {
        title: 'Delivery Gate',
        summary: '数据库脚本、构建、覆盖率和 Sonar 统一收口到阶段交付门禁。'
      },
      compliance: {
        title: 'Compliance Gate',
        summary: '恢复基线、可观测基线、Kafka gate 与敏感数据边界纳入 R-118 复验。'
      }
    },
    blockers: {
      coverage: 'Coverage threshold 已被 phase gate 真正执行，但当前仓库全量覆盖率仍需继续抬升到 Phase-1+ 85%。',
      sonar: 'Sonar 在 delivery/full gate 下已被强制要求，缺少 secrets 时会直接阻断。',
      workflowDispatch: 'Phase Gate workflow 仍然是显式 workflow_dispatch，不会自动绑定发布动作。'
    },
    evidenceRows: {
      defaultCi: 'CI 默认门禁',
      kafka: '真实 Kafka 门禁',
      phaseGate: 'Phase Gate 入口',
      phaseScript: '阶段脚本'
    },
    metrics: {
      gates: {
        label: '门禁层级',
        detail: '只统计当前页面明确列出的 Entry、Delivery 与 Compliance 门禁。'
      },
      checks: {
        label: '阻断检查',
        trend: '脚本入口',
        detail: '来自门禁活动流的命令清单，不推导外部环境状态。'
      },
      workflows: {
        label: 'workflow 入口',
        detail: '仅显示仓库内 workflow 文件引用，执行结果仍以验证日志为准。'
      },
      blockers: {
        label: '残余阻塞',
        trend: '需继续跟踪',
        detail: '这些项保留为风险队列，不能标记为已关闭事实。'
      }
    }
  },
  recoveryDrill: {
    title: '恢复演练',
    summary: '沉淀备份范围、恢复目标、责任边界与恢复后必过验收检查。',
    heroEyebrow: 'recovery drill baseline',
    heroTitle: '备份恢复与恢复后验收基线',
    heroSummary: '这一页把 F-TASK-008/009 形成的备份对象、恢复目标、责任边界和恢复后检查统一收在治理运维路径里。',
    heroNote: '恢复完成的定义不是库导回来了，而是健康探针、审计补偿、队列 backlog、导出/脱敏和敏感泄漏检查都通过。',
    objectivesEyebrow: 'rpo / rto',
    objectivesTitle: '恢复目标与责任人',
    objectivesSummary: '静态表格记录数据域、RPO/RTO 和责任人边界。',
    checklistEyebrow: 'acceptance checklist',
    checklistTitle: '恢复后必须复验的清单',
    checklistSummary: '恢复完成后必须复验的高风险证据，不以数据库导回作为唯一完成定义。',
    kpiTitle: '恢复演练 KPI 与验收边界',
    kpiSummary: '仅展示基线对象、RPO/RTO 目标和恢复后检查项，不把演练结果写成已生产验证事实。',
    activityEyebrow: 'recovery flow',
    activityTitle: '恢复对象活动流',
    activitySummary: '按恢复批次关注对象展示顺序和验收语义。',
    sameBackupBatch: '与备份批次同步',
    inventory: {
      core: '主库、核心追溯链、schema 版本与 migration 清单必须成批恢复。',
      audit: '审计留痕必须连续，`LOGIN/LOGOUT` 与 `audit/write` 抽样恢复后仍可落库。',
      export: '导出元数据和脱敏归档索引要能互相核对。',
      queue: '数据库兜底或 fallback backlog 恢复后必须还能继续补偿。',
      keys: '只恢复密文、不回流明文，`encryption_key_id` 必须与批次对应。'
    },
    checklist: {
      health: '4 个后端 `/actuator/health` 和治理 `/api/governance/health` 全部返回 `UP`。',
      audit: '恢复后复跑 `audit/write` 抽样、`LOGIN/LOGOUT` 审计样本。',
      backlog: '检查 `kafka_message_queue` backlog 或明确记录为何不适用。',
      export: '抽样 `export_record` 与 `history_id/result_id` 追溯键，确认脱敏地址未泄漏。',
      leak: '抽检日志平台和 `system_config`，确认没有密码、Token、密钥明文泄漏。'
    },
    metrics: {
      inventory: {
        label: '恢复对象',
        trend: '基线清单',
        detail: '只统计当前基线明确列出的恢复对象。'
      },
      rpo: {
        label: 'RPO 目标',
        detail: '治理元数据、审计、导出和队列域共同遵守的恢复点目标。'
      },
      rto: {
        label: 'RTO 目标',
        detail: '恢复后仍需通过健康、审计、队列和脱敏验收。'
      },
      checklist: {
        label: '复验检查',
        trend: 'post-restore',
        detail: '恢复完成后必须逐项复验，不以单点成功替代闭环。'
      }
    },
    table: {
      domain: '数据域',
      owner: '责任人'
    }
  },
  benchmark: {
    title: '压测报告',
    summary: '查看基线、峰值延迟、回归差异与准入判断。',
    eyebrow: 'benchmark center',
    boundarySummary: '当前 repo-side 已有真实 benchmark task/report 接口；模板与测试集仍以前端 session/catalog 组织，不伪装成后端 CRUD。',
    input: {
      eyebrow: 'benchmark input',
      title: '压测边界与关键输入',
      summary: '租户、任务类型与 SQL 是提交真实 benchmark task 的关键输入，成功与失败补偿流程在下方 tabs 执行。'
    },
    fields: {
      sql: 'SQL',
      initialStatus: '初始状态',
      failureCode: '失败码',
      retryable: '可重试',
      pendingBefore: '补偿前 pending',
      pendingAfter: '补偿后 pending',
      pendingDelta: 'pending 增量',
      totalDelta: 'total 增量',
      rawDataPath: '原始数据路径',
      scannedBytes: '扫描字节'
    },
    tabs: {
      templates: '模板',
      testSets: '测试集',
      taskFlow: '任务流',
      compensation: '失败补偿',
      report: '报告',
      sessionTasks: '会话任务'
    },
    templates: {
      eyebrow: 'template catalog',
      title: '模板列表',
      summary: '模板是当前页面的 task preset，用于快速填充真实 benchmark task 参数。'
    },
    testSets: {
      eyebrow: 'test-set catalog',
      title: '测试集列表',
      summary: '测试集为前端 session catalog，不宣称仓库已有独立 test-set API。'
    },
    taskFlow: {
      eyebrow: 'benchmark task flow',
      title: '成功任务流',
      summary: '使用当前选中模板的 taskContext 提交、轮询并读取报告。'
    },
    compensation: {
      eyebrow: 'failure compensation',
      title: '失败补偿流',
      summary: '失败链路会追加 FAIL_BENCHMARK，用于验证 governance compensation queue evidence。'
    },
    report: {
      eyebrow: 'benchmark report',
      title: '报告对比与回归结果',
      summary: '成功执行后展示真实 report 返回的 engine results、threshold assessments、trend charts 与 recommendations。',
      empty: '执行成功后会在这里显示实际 report 返回值。',
      returned: '报告 {reportId} 由真实接口返回，可用格式 {formats}。',
      engineEyebrow: 'engine comparison',
      comparisonTitle: '对比指标',
      regressionEyebrow: 'regression results',
      regressionTitle: '阈值与回归判断',
      trendEyebrow: 'trend & recommendation',
      trendTitle: '趋势与后续建议'
    },
    session: {
      eyebrow: 'session tasks',
      title: '会话任务列表',
      summary: '当前没有全局任务列表接口，因此仅保留本次会话发起的 benchmark 任务。',
      empty: '当前会话尚未发起 benchmark 任务。'
    },
    actions: {
      runTemplate: '执行当前模板',
      runCompensation: '执行失败恢复 + 补偿'
    }
  },
  parseBatchCenter: {
    title: '批量解析',
    summary: '独立处理批次创建、模板下载、文件导入、失败重试与报表清单解析。',
    reportStatistics: {
      tabs: {
        issueScenes: '问题场景'
      },
      columns: {
        issueScene: '问题场景',
        affectedSql: '影响 SQL',
        severity: '严重度',
        reportCount: '报表数',
        logicalObjectCount: '逻辑对象',
        ratio: '比例'
      },
      labels: {
        issueScenes: '问题场景',
        location: '定位'
      },
      states: {
        emptyIssueScenes: '当前没有问题场景统计。'
      }
    }
  },
  parseStatisticsCenter: {
    title: 'SQL解析统计中心',
    summary: '独立查看解析概览、问题分布、优先级矩阵、SQL 与报表维度统计。',
    severityView: '严重度视角',
    priorityView: '优先级视角',
    logicalObjectView: '逻辑对象视角',
    parseStatusSamples: '解析状态样本',
    severity: '严重度',
    issueScenes: '问题场景数',
    affectedSql: '影响 SQL',
    affectedIssues: '问题数',
    urgentScenes: '紧急场景',
    priority: '优先级',
    highestScore: '最高分',
    logicalObjectType: '对象类型',
    logicalObjectKey: '对象标识',
    samples: '样本数',
    resultStatus: '结果状态',
    cacheHit: '缓存命中',
    rewrite: '轻量改写',
    acceleration: '加速命中',
    errorLabels: {
      overview: '解析总览',
      issueScenes: '问题分布',
      sqlStats: 'SQL 清单',
      reportStats: '报表视角',
      priorityMatrix: '优先级矩阵',
      importantUrgent: '重要/紧急清单'
    }
  },
  assetCatalog: {
    title: '数据资产目录',
    summary: '查看 datasource、schema、table、logical view 与 db view 的列表和详情证据。',
    eyebrow: 'data asset catalog',
    workspaceSummary: '按资产类型筛选、查看列表和详情证据，并把 metadata snapshot、lineage、physical mappings、dependencies 与 SQL 候选留在同一工作面。',
    filters: {
      eyebrow: 'asset filters',
      title: '筛选与刷新',
      summary: '筛选状态与目录状态分离；刷新不会改变后端资产事实。',
      schemaPlaceholder: '仅 table 列表使用'
    },
    actions: {
      refresh: '刷新目录'
    },
    catalog: {
      eyebrow: 'catalog tabs',
      title: '资产目录',
      summary: '当前类型 {count} 条结果'
    },
    detail: {
      eyebrow: 'asset detail',
      title: '详情与证据',
      summary: '详情、健康状态、snapshot 与下钻证据在常驻区域展示。',
      connectionEndpoint: '连接地址',
      credentialMode: '凭证模式',
      lastFailureReason: '最近失败原因'
    },
    states: {
      emptyCatalog: '当前筛选下没有目录结果。',
      selectAsset: '从左侧选择一个资产后显示详情。'
    },
    health: {
      eyebrow: 'freshness / sla / heat',
      title: '数据到位与热度 proxy',
      usageHeatProxy: 'Usage heat proxy'
    },
    snapshot: {
      eyebrow: 'snapshot evidence',
      title: 'Metadata snapshot 旁证'
    },
    relatedSql: {
      eyebrow: 'related sql',
      title: '相关 SQL 候选',
      summary: '只读候选，不执行 SQL。',
      boundary: '当前 repo-side 没有 logical-object -> SQL 的独立查询接口，这里按 logical view `viewCode` 与 parse statistics `reportCode` 对齐展示候选。',
      empty: '当前 logical view 还没有匹配到 related SQL 候选。'
    }
  },
  routingGovernance: {
    title: '路由执行证据',
    summary: '查看当前路由校准、历史决策与注释协议摘要；当前仅支持只读证据查看。',
    eyebrow: 'routing execution evidence',
    pageTitle: '路由执行证据与历史决策',
    boundarySummary: '当前页只消费 route-calibration 与 query-history.routeDecision 的只读证据，不伪装成规则配置中心。',
    filters: {
      eyebrow: 'routing filters',
      title: '证据范围与操作'
    },
    fields: {
      traceLimit: 'Trace 数量',
      traceId: 'Trace ID',
      auditEvents: '审计事件数',
      lastSeenAt: '最后时间'
    },
    actions: {
      refresh: '刷新路由证据',
      viewPolicySource: '查看当前策略来源',
      createRule: '新增规则',
      editRule: '修改规则',
      openParseRecord: '打开历史详情页'
    },
    tabs: {
      calibration: 'Calibration',
      commentProtocol: '注释协议',
      recentTraces: 'Recent traces'
    },
    policy: {
      eyebrow: 'current policy',
      title: '当前策略快照'
    },
    comment: {
      eyebrow: 'comment protocol',
      title: '注释协议摘要'
    },
    traces: {
      eyebrow: 'routing-route-decision',
      title: '路由决策历史',
      summary: 'trace detail 通过弹窗下钻，raw route evidence 通过抽屉查看。',
      state: '当前 trace 结果 {count} 条'
    },
    detail: {
      dialogTitle: '路由决策详情',
      historyState: '当前 trace history {count} 条'
    },
    rawDrawerTitle: '路由原始证据'
  },
  recommendationCenter: {
    title: '推荐与加速中心',
    summary: '查看 recommendation 分类、收益风险、dispatch 状态与追溯关联。',
    eyebrow: 'recommendation center',
    pageTitle: '推荐与加速中心',
    boundarySummary: '页面消费 recommendation、dispatchEvents 与 traceability 证据；SQLForge 只管理建议、事件和回执，不执行推荐 SQL、不主动装数。',
    filters: {
      eyebrow: 'recommendation filters',
      title: '租户与刷新'
    },
    actions: {
      refresh: '刷新推荐中心',
      openRouting: '打开路由治理',
      openParse: '打开 SQL解析',
      openHistory: '打开历史页'
    },
    list: {
      eyebrow: 'recommendation categories',
      title: '推荐分类',
      summary: '当前租户 {count} 条 recommendation'
    },
    detail: {
      eyebrow: 'recommendation detail',
      title: '收益、风险与 SQL 详情'
    },
    fields: {
      benefitLevel: '收益',
      riskLevel: '风险',
      dispatch: '协同',
      expectedGain: '预期收益',
      riskSummary: '风险摘要',
      reason: '推荐原因',
      sourceSql: '源 SQL'
    },
    states: {
      selectRecommendation: '选择一个 recommendation 查看详情。',
      loadingDetail: '正在加载 recommendation detail…',
      emptyDetail: '当前没有可展示的 recommendation。',
      waitingCallback: '等待外部回执。'
    },
    tabs: {
      summary: 'Summary',
      sqlEvidence: 'SQL evidence',
      dispatchContract: 'Dispatch contract',
      traceability: 'Traceability',
      dispatchEvents: 'Dispatch events'
    },
    dispatch: {
      boundary: '当前协同边界固定为 coordinationMode=PULL_ONLY：外部模块负责真实装数、预热执行和底层变更，SQLForge 只保留 recommendation 与 dispatch 回执审计。'
    }
  },
  accessCenter: {
    title: '开放接入',
    summary: '查看 API、JDBC Agent、Java SDK、接入策略与 access audit 样例。',
    eyebrow: 'access workbench',
    pageTitle: '开放接入与审计样例',
    boundarySummary: '默认展示接入审计表格，渠道、JDBC Agent、SDK 和策略边界拆入 tabs；缺失写 API 的动作继续显式不可写。',
    filters: {
      eyebrow: 'access filters',
      title: '接入范围与操作'
    },
    fields: {
      accessChannel: '接入渠道',
      historyReport: 'History / Report',
      mode: 'Mode'
    },
    actions: {
      refresh: '刷新接入证据',
      createStrategy: '新增接入策略',
      editStrategy: '修改策略',
      boundaryHelp: '边界说明'
    },
    tabs: {
      audit: '接入审计',
      channels: 'Channels',
      jdbc: 'JDBC Agent',
      sdk: 'SDK / Client',
      policy: 'Policy boundary'
    },
    audit: {
      eyebrow: 'access audit sample',
      title: '接入审计样例',
      summary: '默认用表格呈现 query-history accessChannel 过滤结果。',
      boundary: '当前仓库还没有独立开放给前端的 `GET /api/governance/access-audit` 控制器，因此这里先用 query-history 的 `accessChannel` 过滤面呈现审计样例。',
      state: '当前审计样例 {count} 条'
    },
    channels: {
      eyebrow: 'access channels',
      title: '接入渠道'
    },
    jdbc: {
      state: '当前 JDBC Agent mode {count} 条'
    },
    detail: {
      dialogTitle: '接入审计详情'
    },
    rawDrawerTitle: '接入原始证据',
    policy: {
      dialogTitle: '接入边界说明'
    }
  },
  alertCenter: {
    title: '告警中心',
    summary: '查看派生告警、ACK 模拟状态与 notify simulated 结果。',
    pageTitle: '告警中心与通知状态',
    refactorSummary: '专用读接口 {readPath} 存在基线，但当前页面在正式接线前仍基于 backlog、dispatch event 和 important/urgent SQL 派生告警，并明确保留 ACK / notify simulated 边界。',
    controlsTitle: '告警刷新与模拟动作',
    controlsSummary: '筛选租户后刷新派生告警；新增规则和通知策略仍展示为缺失写接口的能力边界。',
    listTitle: '告警列表',
    detailTitle: '详情、ACK 与 notify 状态',
    metrics: {
      total: '总告警',
      open: '未 ACK',
      high: '高优先级',
      simulated: 'notify simulated'
    },
    fields: {
      alertId: '告警 ID',
      ackStatus: 'ACK 状态',
      notifyStatus: '通知状态',
      ackMode: 'ACK 模式'
    },
    actions: {
      refresh: '刷新告警',
      createRule: '新增告警规则',
      editNotify: '修改通知策略',
      ack: 'ACK 模拟确认',
      clearAck: '撤销模拟 ACK'
    },
    derived: {
      backlogTitle: '治理补偿 backlog 告警',
      backlogSummary: '当前 failed={failed}，pending={pending}。',
      dispatchTitle: '装数协同事件待处理',
      parseTitle: '解析优先级告警'
    },
    placeholder: {
      createTitle: '新增告警规则暂不可写',
      createCapability: '新增告警规则',
      createReason: '当前仓库没有独立的告警规则写接口，这一页仍然基于 backlog、dispatch 和 important/urgent SQL 派生证据。',
      createNextStep: '后续若补告警配置后端，再把新增表单接到这里。',
      editTitle: '修改通知策略暂不可写',
      editCapability: '修改通知策略',
      editReason: '当前页面的 ACK / notify 明确是 simulated，不应伪装成已经接通的真实通知控制面。',
      editNextStep: '需要真实通知接口和审计链后，再接入编辑动作。'
    },
    messages: {
      noAlert: '当前没有可展示的告警。'
    }
  },
  acceleration: {
    title: 'SQL解析',
    summary: '专注单条 SQL 解析、结构/访问结果阅读与历史追溯。'
  },
  system: {
    title: '系统管理',
    summary: '查看并维护数据源、报表接口、Redis 规则源、Dispatch 策略与系统证据。'
  }
}
