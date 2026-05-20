# SQLForge Plans

本目录用于承载 SQLForge 的执行计划、阶段计划、确认台账与后续阶段验收记录。

## 计划阅读顺序

1. [主执行计划](./master-execution-plan.md)
   当前全量执行控制文档，负责汇总事实基线、阶段拆解、Epic/Story/Task 结构、验证矩阵与人工确认项。
2. [阶段0执行计划](./phase-0-plan.md)
   初始化阶段历史计划，保留原任务顺序、依赖关系与验收口径，供阶段真值对齐时引用。
3. [Codex 集成治理蓝图](./codex-governance-integration-blueprint.md)
   定义如何把 SQLForge 的文档真值、任务台账、验证、closeout、Git 审计链与 Codex 的 `AGENTS`、project-scoped `.codex/`、hooks 和 foreman CLI 接到同一执行链。
4. [文档覆盖矩阵](./document-coverage-matrix.md)
   用于证明 `docs/` 目录全部文档与归档资料已被主计划纳入，不遗漏。
5. [Task 字段矩阵](./task-spec-matrix.md)
   为主计划中的全部 Task 补齐 Harness Engineering 所要求的 10 项字段。
6. [流程与治理审计说明](./process-flow-and-governance-audit-2026-04-20.md)
   对当前工程交付全流程、产品运行流程、流程缺陷、整改建议和逐文档修订建议做正式归档。
7. [生产自动改写闭环任务拆解计划](./production-rewrite-auto-apply-task-plan.md)
   将“解析/执行后发现改写推荐，经人类审批后在生产执行路径自动改写”的闭环拆成可由 Codex 逐项执行的小任务；暂不覆盖投产前本地/测试环境核验闭环。
8. [L2 高级物化视图推荐任务拆解计划](./l2-advanced-materialized-view-task-plan.md)
   将“从 SQL 中抽取可复用计算子图并生成高级物化视图候选”的能力拆成可逐项 materialize 的任务包，并明确禁止 `EXACT_QUERY_MV`。
9. [前端核心链路聚焦改造任务包](./frontend-core-workflow-refocus-task-pack.md)
   将“SQL 查询分析 / SQL 历史查询 / SQL 解析 / 解析历史 / 推荐结果 / 改写记录 / 改写历史”聚焦改造拆成可逐个 materialize、规划、实现和 closeout 的小任务；旧工作台参考页不再作为当前产品页面保留。

## 计划治理要求

- 所有计划、阶段、Epic、Story、Task 必须符合 Harness Engineering 的“先固化意图、约束、接口、验证，再执行”要求。
- 所有跨步骤、跨前后端、跨领域工作必须先落到计划文档，再进入实现。
- 涉及 Codex 执行路径、project-scoped `.codex/` 配置、hooks、foreman CLI 或任务运行态接线的治理改造，必须同时对齐本目录中的 Codex 集成蓝图与对应活动 exec plan。
- 所有 Task 必须具备独立验证计划，且验证内容必须真实执行，不得以推测替代。
- 已存在的阶段计划作为历史记录保留；新的全局计划统一追加在本目录，不覆盖历史语义。

## 当前状态

- 当前主控计划文档：`master-execution-plan.md`
- 当前历史阶段文档：`phase-0-plan.md`
- 当前文档全量覆盖证明：`document-coverage-matrix.md`
- 当前 Task 字段补全集：`task-spec-matrix.md`
- 当前未闭合事项：以主执行计划中的 `Human Confirmation Ledger` 为准

## 2026-04-20 新增治理文档

- `document-truth-baseline.md`
  用于隔离当前仓库事实、已确认目标、历史记录和归档原文。
- `implementation-readiness.md`
  用于定义任务开始前的文档消费顺序、主题权威、冲突处理和波次执行顺序。
- `codex-governance-integration-blueprint.md`
  用于定义 Codex 执行前、中、后如何接入仓库文档真值、台账、验证、closeout 与 Git 审计链。
- `document-gap-matrix.md`
  用于显式维护本轮治理中的冲突、漂移、缺失与残余实现缺口。
- `phase-prerequisite-matrix.md`
  用于为每个阶段显式列出输入文档、ADR、规则、验证和人工确认点。
- `task-governance-extension-matrix.md`
  用于补齐 Task 的人工确认点、数据影响与回滚扩展字段。
- `retrospective-template.md`
  用于阶段或复杂批次结束后的标准复盘。
- `document-governance-retrospective-2026-04-20.md`
  本轮文档治理专项的实际复盘记录。
- `document-governance-repair-retrospective-2026-04-20.md`
  本轮严格核验缺口修复的实际复盘记录。
- `process-flow-and-governance-audit-2026-04-20.md`
  基于当前仓库文档全量扫描形成的正式流程说明与治理审计文档。
- `production-rewrite-auto-apply-task-plan.md`
  基于当前生产闭环缺口，将改写复核、激活、运行时自动改写、历史留痕、差异暂停和页面追溯拆成多个可执行任务。
- `l2-advanced-materialized-view-task-plan.md`
  基于当前 L2 物化视图能力缺口，将参数外提聚合、预 Join、星型聚合、Rollup、公共子图 MV 推荐拆成多个可执行任务，并明确不实现 `EXACT_QUERY_MV`。
- `frontend-core-workflow-refocus-task-pack.md`
  基于当前前端信息架构重心漂移，将核心 SQL 工作流聚焦改造拆成多个候选前端任务，并明确核心菜单、辅助治理和参考页面边界。

使用方式：

- 判断“现在仓库里到底有什么”时，先读 `document-truth-baseline.md`
- 判断“接下来这轮编码按什么顺序推进”时，读 `implementation-readiness.md`
- 判断“当前还有哪些治理缺口、是否已经闭口”时，读 `document-gap-matrix.md`
- 判断“某阶段是否具备进入条件”时，读 `phase-prerequisite-matrix.md`
- 判断“某个 Task 的人工确认点、数据影响和回滚策略”时，读 `task-governance-extension-matrix.md`
- 需要在交付后沉淀复盘时，使用 `retrospective-template.md`
- 需要追溯本轮治理专项的实际结果时，读 `document-governance-retrospective-2026-04-20.md`
- 需要追溯本轮严格核验修复结果时，读 `document-governance-repair-retrospective-2026-04-20.md`
- 需要一次性理解当前全流程、流程缺陷和整改优先级时，读 `process-flow-and-governance-audit-2026-04-20.md`
- 需要逐步改造前端核心菜单、首页、历史、推荐、改写和参考页边界时，读 `frontend-core-workflow-refocus-task-pack.md`
