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
    title: '解析工作台',
    summary: '对单条 SQL 执行结构解析、access parse 和综合结论判定。'
  },
  system: {
    title: '系统管理',
    summary: '维护租户、路由默认值、审计保留与全局控制项。'
  }
}
