# 前端核心链路聚焦改造任务包

本文把“SQLForge 前端核心功能聚焦改造”拆成可由 Codex 后续逐个读取、生成单任务计划、再独立实现和 closeout 的小任务包。

本文中的 `HARN-FE-*` 是候选计划 ID，不等同于当前活动任务。进入实现前，必须按仓库治理流程将对应任务 materialize / instantiate 到任务台账或专项 exec plan，并执行 `python3 scripts/foreman.py preflight`、`python3 scripts/foreman.py instantiate <TASK_ID>`、`python3 scripts/foreman.py validate <TASK_ID>`、`python3 scripts/task_audit.py --check --phase pre-closeout|post-closeout`。

## 目标与原则

- 核心目标：让前端第一视觉聚焦真实 SQL 工作流：`SQL 查询分析 -> SQL 历史查询 -> SQL 解析 -> 解析历史 -> 推荐结果 -> 改写记录 -> 改写历史`。
- 产品原则：核心功能用于完成 SQL 分析闭环；`USER-CN-IMPLEMENT-ENGINE-OPTION-B-20260520` 之后，审计取证、公开追踪查询、告警中心、运行门禁页和恢复演练页不再作为产品入口暴露，只保留执行引擎需要的最小安全 evidence。
- 文档权威：HARN-FE-001A 已确认本文是前端展示层菜单与视觉权重的当前权威；`docs/product/sql-governance-platform-implementation-spec.md` 中的历史一级模块清单保留为产品能力域基线。
- 交付边界：本任务包默认不新增后端 API、不改数据库、不改变推荐、改写、复核、激活、自动应用的业务语义。
- 旧参考页边界：旧流程模拟参考页和辅助治理产品页已从当前产品页面、路由和验证入口中移除；推荐、改写与历史由各自核心页面承接。
- Codex 执行原则：每个任务都必须足够小、可独立验证、可单任务单 commit；不得以一个大任务同时改导航、首页、历史、推荐、改写和参考页。

## 最终菜单清单

| 一级菜单 | 二级菜单 | 作用 | 边界 |
|:---|:---|:---|:---|
| 首页总览 | - | 展示核心 SQL 链路待办、最近查询、最近解析、推荐结果、改写风险摘要。 | 只做摘要和跳转，不承载审计详情、复核流、运行门禁操作。 |
| SQL 查询分析 | - | 提供 SQL 输入、格式化、校验、执行、Explain、查询结果、解析摘要、推荐入口、历史关联。 | 不做批量解析管理，不做复核激活，不替代 SQL 历史查询。 |
| SQL 历史查询 | - | 查询执行历史、筛选历史 SQL、查看执行摘要、原 SQL / 模板 SQL / 绑定后 SQL、关联解析、推荐和改写记录。 | 保留执行事实与改写 evidence，不提供独立 trace 反查产品面。 |
| SQL 解析 | 单条 SQL 解析 | 输入单条 SQL，展示结构解析、数据访问解析、问题场景、风险提示和推荐动作。 | 默认突出单条解析，不让批量导入抢占首屏。 |
| SQL 解析 | 批量解析中心 | 承接 SQL 文件、表格、报表清单导入和批量解析结果概览。 | 属于解析能力的次级入口，不作为主链路第一视觉。 |
| 解析历史 | - | 查询解析历史、批次历史、报表解析历史，查看解析状态、失败原因、问题场景、逻辑对象和推荐关联。 | 不承担 SQL 执行历史；执行历史仍归 `SQL 历史查询`。 |
| 推荐结果 | - | 展示推荐列表、推荐来源、收益、风险、推荐 SQL、SQL diff、规则链和适用条件。 | 不包装为统一工作台；复核、dispatch、trace 为详情辅助信息。 |
| 改写治理 | 改写记录 | 查看由推荐生成或人工创建的改写记录，关注 review、activation、active、paused、validation 等状态。 | 复核只是记录操作的一部分，不单独作为核心菜单。 |
| 改写治理 | 改写历史 | 查询历史改写、SQL 历史关联改写、验证运行、结果差异、暂停原因和生命周期证据。 | 不替代推荐结果；推荐产生候选，改写历史沉淀执行与验证证据。 |
| 系统管理 | - | 管理数据源、系统配置和基础管理能力。 | 平台配置入口，不承载 SQL 分析主流程。 |
| 开放接入 | - | 展示 SDK、JDBC Agent、开放接入契约与接入状态。 | 面向接入管理，不参与核心 SQL 页面视觉竞争。 |
| 参考页面 | AI 交付 | 展示任务与交付参考信息。 | 非生产主功能，按现有临时页策略保留或隐藏。 |

## 菜单命名映射

| 当前或历史名称 | 目标名称 | 处理方式 |
|:---|:---|:---|
| SQL 查询 | SQL 查询分析 | 正式核心菜单名。 |
| SQL历史 / 历史列表 | SQL 历史查询 | 正式核心菜单名。 |
| 解析与加速 | SQL 解析 | 一级核心菜单聚焦解析，不以“加速”抢占主线。 |
| SQL 解析 | 单条 SQL 解析 | 保留为 SQL 解析下的主入口。 |
| 解析历史查询 | 解析历史 | 正式核心菜单名。 |
| 推荐与加速中心 | 推荐结果 | 正式核心菜单名。 |
| 审计取证 / 故障处置 / 告警中心 / 运行门禁 / 恢复演练 | 已移除产品入口 | 保留最小执行安全 evidence，不再作为导航分组。 |

## 执行波次

### Wave A：文档与导航基线

先固化产品真值和导航骨架，避免后续页面改造继续沿用旧命名。

### Wave B：首页与核心历史链路

再调整首页、SQL 历史和解析历史，让用户第一路径能完成查询、解析、推荐和改写追溯。

### Wave C：推荐、改写与参考页收口

最后调整推荐结果、改写记录/历史，以及旧参考页移除后的验证门禁。

## 候选任务包

每个任务都包含 Harness Engineering 核心字段：目标、上下文、接口契约、技术约束、分层定位、测试策略、依赖、环境要求、人工确认点、回滚策略。

### HARN-FE-001：固化核心菜单与功能边界文档

**目标**：将本任务包作为长期计划入口写入 `docs/plans/` 并挂入计划索引，明确最终菜单清单、核心/辅助/参考页面边界和后续任务拆分。

**上下文**：`docs/product/sql-governance-platform-implementation-spec.md`、`docs/frontend/design-system.md`、`docs/plans/README.md`。

**接口契约**：文档契约；不改变前端路由、后端 API、数据库 schema 或运行时配置。

**技术约束**：只修改计划文档和索引；候选任务 ID 不直接写入活动台账，必须等后续 materialize。

**分层定位**：`docs`。

**测试策略**：`node scripts/lint-repository-knowledge.js`、`git diff --check`，必要时执行 `python3 scripts/foreman.py compile-governance --check`。

**依赖**：无。

**环境要求**：docs-only，repo-closed。

**人工确认点**：若要重新新增统一流程工作台类页面，必须人工确认。

**回滚策略**：回退或追加修正文档，不删除已有历史记录。

### HARN-FE-002：重排导航树与核心菜单命名

**目标**：调整 `src/config/routePaths.mjs` 的 `NAVIGATION_TREE`，让核心菜单优先展示，并保持统一流程工作台类页面不进入正式核心菜单。

**上下文**：HARN-FE-001、本文件最终菜单清单、`src/config/routePaths.mjs`、`src/App.vue`。

**接口契约**：保留现有 `ROUTE_PATHS`、route name、legacy redirect；只改变菜单树和可见分组。

**技术约束**：不得删除现有页面组件；不得改变旧地址跳转；参考页可沿用 `deliveryProgressEnabled` 类似策略或低优先级参考分组；必须在同一任务内同步导航、工作台和页面治理契约脚本，避免旧 contract 继续把参考页强制为核心菜单。

**分层定位**：`frontend/router`。

**测试策略**：`npm run lint`、`npm run build`、`npm run test:frontend-page-governance`、`node scripts/check-navigation-shell-contract.mjs`、前端 before/after 截图自检、`git diff --check`。

**依赖**：HARN-FE-001。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若需要删除现有路由、移除 legacy redirect 或让参考页生产默认可见，必须人工确认。

**回滚策略**：回退导航树变更，保留旧路由和页面不变。

### HARN-FE-003：统一 i18n 文案与页面标题

**目标**：更新 `src/locales/zh-CN.js` 与 `src/locales/en-US.js`，统一核心菜单、页面标题、摘要、breadcrumb 和参考页说明。

**上下文**：HARN-FE-002、`src/locales/zh-CN.js`、`src/locales/en-US.js`。

**接口契约**：只改 UI 文案，不改接口字段、状态枚举、后端契约名称或数据 payload。

**技术约束**：代码字段、枚举和 API 标识保持英文原值；展示文案可业务化。

**分层定位**：`frontend/i18n`。

**测试策略**：`npm run lint`、`npm run build`、`npm run test:sql-ui-contract`、`npm run test:frontend-page-governance`、`npm run test:i18n-copy`、`git diff --check`。

**依赖**：HARN-FE-002。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若命名需要改变产品语义，例如把推荐结果改成已执行结果，必须人工确认。

**回滚策略**：回退文案键值，保留路由和页面结构。

### HARN-FE-004：首页总览聚焦核心链路

**目标**：改造 Dashboard 的入口排序和首屏信息权重，只突出核心 SQL 工作流摘要和入口。

**上下文**：HARN-FE-003、`src/views/dashboard/DashboardView.vue`、Dashboard 现有 sample-aware KPI 边界。

**接口契约**：不新增后端聚合接口；继续使用现有 repo-side 证据、样本或静态摘要。

**技术约束**：不得把 sample、window、session 或 PULL_ONLY 证据写成全租户事实；不得重新引入审计取证、告警中心、运行门禁或恢复演练产品入口。

**分层定位**：`frontend/views/dashboard`。

**测试策略**：`npm run lint`、`npm run build`、`npm run test:frontend-page-governance`、Dashboard contract 检查、before/after 截图自检、`git diff --check`。

**依赖**：HARN-FE-003。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若要新增全局 KPI 后端聚合或改变 Dashboard 事实口径，必须人工确认。

**回滚策略**：回退 Dashboard 入口和 KPI 排序，保留现有 sample 边界。

### HARN-FE-005：SQL 历史与解析历史强化关联链路

**目标**：强化 `SQL 历史查询` 与 `解析历史` 的页面定位，让历史查询自然串联解析、推荐和改写关联。

**上下文**：HARN-FE-003、`src/views/sql-history/SqlHistoryView.vue`、`src/views/parse-record/ParseRecordView.vue`、现有 history / parse-history API。

**接口契约**：复用现有 SQL 历史、解析历史、推荐引用、改写记录引用接口；不新增后端字段。

**技术约束**：原始 SQL、模板 SQL、绑定后 SQL 不得被前端自动改写；审计、trace、告警只作为详情证据或折叠区。

**分层定位**：`frontend/views/sql-history`、`frontend/views/parse-record`。

**测试策略**：`npm run lint`、`npm run build`、`npm run test:sql-ui-contract`、history page/detail contract、parse history contract、before/after 截图自检、`git diff --check`。

**依赖**：HARN-FE-003。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若需要调整历史查询参数、导出字段、脱敏策略或分页语义，必须人工确认。

**回滚策略**：回退页面结构和入口排序，保持历史查询 API 消费不变。

### HARN-FE-006：推荐结果页面聚焦 SQL diff 与收益风险

**目标**：将 `RecommendationCenterView.vue` 的主视觉调整为正式 `推荐结果` 页面，突出推荐列表、SQL diff、收益风险、规则链和推荐来源。

**上下文**：HARN-FE-003、HARN-146 SQL compare 结果、`src/views/recommendation-center/RecommendationCenterView.vue`、recommendation diff API。

**接口契约**：不改变后端 recommendation diff 语义；不改变推荐状态、复核、激活、自动应用行为。

**技术约束**：dispatch、trace、复核证据进入折叠区或证据抽屉；前端只展示后端权威结果，不自行判断语义等价。

**分层定位**：`frontend/views/recommendation-center`。

**测试策略**：`npm run lint`、`npm run build`、`node scripts/check-recommendation-page-contract.mjs`、`npm run test:frontend-page-governance`、`npm run test:sql-ui-contract`、before/after 截图自检、`git diff --check`。

**依赖**：HARN-FE-003。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若要把推荐结果页面扩展为复核中心、激活中心或自动应用入口，必须人工确认。

**回滚策略**：回退页面信息层级，保留 HARN-146 diff compare 能力。

### HARN-FE-007：改写记录与改写历史入口收口

**目标**：在正式导航和推荐详情中呈现 `改写记录`、`改写历史`，复用现有 rewrite records、query-history rewrite records 和 validation runs 能力。

**上下文**：HARN-FE-006、`src/views/recommendation-center/RecommendationCenterView.vue`、`src/views/sql-history/SqlHistoryView.vue`、rewrite record API。

**接口契约**：复用既有改写记录、激活资格、验证运行、SQL 历史关联改写接口；不新增独立后端 API。

**技术约束**：`改写记录`关注当前记录状态；`改写历史`关注历史关联、验证运行、结果差异、暂停原因。v1 默认复用现有 `推荐结果` 与 `SQL 历史查询` route，并通过 query/tab 深链进入对应 tab；除非人工确认，不新增独立 view、独立 route 或后端接口。复核是详情动作，不成为独立核心菜单。

**分层定位**：`frontend/router`、`frontend/views/recommendation-center`、`frontend/views/sql-history`。

**测试策略**：`npm run lint`、`npm run build`、`npm run test:sql-ui-contract`、recommendation page contract、history detail contract、before/after 截图自检、`git diff --check`。

**依赖**：HARN-FE-006。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若需要新增独立后端改写历史接口、改变复核/激活状态机或运行时绑定语义，必须人工确认。

**回滚策略**：回退新增入口和 tab/query 参数，保留原推荐中心与历史详情能力。

### HARN-FE-008：旧参考页移除后的最终验证

**目标**：确认旧流程模拟参考页不再作为当前产品页面、路由、菜单、i18n 或专属 smoke 入口存在，并完成全链路验收。

**上下文**：HARN-FE-002 至 HARN-FE-007、`src/config/routePaths.mjs`、`src/router/index.js`、`package.json`。

**接口契约**：不删除推荐中心、SQL 历史、告警中心、生产改写闭环、后端加速计划或运行时绑定能力；只移除旧参考页入口与专属验证。

**技术约束**：主导航和 Dashboard 不得恢复旧流程模拟参考页；核心验证继续通过推荐、历史、告警与生产改写闭环 smoke 覆盖。

**分层定位**：`frontend/router`、`frontend/i18n`、`scripts`、`docs`。

**测试策略**：`npm run lint`、`npm run build`、`npm run test:frontend-page-governance`、`npm run test:sql-ui-contract`、`npm run smoke:rewrite-governance`、`git diff --check`。

**依赖**：HARN-FE-002 至 HARN-FE-007。

**环境要求**：前端本地 Node/Vite 环境。

**人工确认点**：若要重新新增统一流程工作台类页面，必须人工确认。

**回滚策略**：通过新任务恢复页面入口、文案和专属验证；不影响核心菜单已完成改造。

## 推荐执行顺序

1. HARN-FE-001：先固化产品真值文档。
2. HARN-FE-002、HARN-FE-003：完成导航和文案基线。
3. HARN-FE-004：改首页入口权重。
4. HARN-FE-005：改历史与解析链路。
5. HARN-FE-006、HARN-FE-007：改推荐与改写链路。
6. HARN-FE-008：确认旧参考页移除后的全量验证。

## 通用执行要求

- 非 trivial 实现任务必须先执行 `python3 scripts/foreman.py preflight`，再按任务 ID instantiate。
- 普通任务保持单任务单 commit。
- 禁止使用 `git add .`、`git add -A`、`git commit -a`、`git reset --hard`。
- 每个任务开始时必须重新读取本文件、`docs/README.md`、`docs/rules/codex-rules.md`、`docs/quality/validation-rules.md` 和命中的产品/前端文档。
- 不改后端接口、不新增数据库迁移、不改变推荐/改写状态语义，除非该任务重新被人工确认并拆出后端专项任务。
- 涉及前端页面、路由、菜单或 i18n 的任务，默认执行 `npm run lint`、`npm run build`、`npm run test:frontend-page-governance` 和 R-186 截图自检；命中 SQL 展示、历史或推荐页面时追加 `npm run test:sql-ui-contract` 与对应 contract 脚本。
- 每个任务 closeout 前运行 `python3 scripts/task_audit.py --check --phase pre-closeout`，closeout 后运行 post-closeout 检查。

## 验收总口径

- 核心菜单能完成：查询、历史、解析、解析历史、推荐、改写记录、改写历史闭环。
- 审计取证、公开追踪查询、告警中心、运行门禁和恢复演练不再作为产品入口暴露；复核、激活、暂停 evidence 留在推荐 / 改写 / 历史主链路。
- 旧流程模拟参考页不再作为当前产品页面、路由、菜单、i18n 或专属 smoke 入口存在。
- 核心改写 smoke 不被破坏。
- 所有页面继续遵守前端设计系统、页面治理、SQL UI 契约和文案国际化要求。
