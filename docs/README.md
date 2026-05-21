# SQLForge Docs

本目录是 SQLForge 的唯一长期知识入口，遵循 Harness Engineering 与 `R-006` 的文档优先顺序。

## 阅读顺序

1. [文档真值基线](./plans/document-truth-baseline.md)
   当前仓库事实、已确认目标、历史记录、原始归档和工程命名映射的分层说明；需要判断“现在仓库真实状态”时先读本文。
2. [架构初始化总文档](./architecture/init.md)
   入口总览，包含初始化规则基线、来源服务职责映射、13 项 ADR、4 阶段里程碑、等保规则、加速服务与异常回滚设计；后续追加规则以规则库为准，最终服务口径以已确认的 4 微服务目标为准。
3. [规则库](./rules/codex-rules.md)
   Codex 执行和仓库落地的 append-only 规则索引，按连续编号维护。
4. [Karpathy 行为准则](./rules/karpathy-guidelines.md)
   Karpathy 风格 AI 编码行为的完整说明，保留四原则、适用形态和 SQLForge 落点。
5. [验证规则](./quality/validation-rules.md)
   阶段门禁、任务验证、自动阻断、Java 规范和 harness 任务治理验证的快速索引。
6. [实现就绪规范](./plans/implementation-readiness.md)
   编码前文档消费顺序、主题权威来源、冲突处理方式和波次执行顺序。
7. [运维与协作文档](./operations/README.md)
   Foreman 工作流、人类协作、本地开发、MCP 使用手册、从无 task 开始的治理自动化（含 governed intake / healthcheck）、多 agent 协作、任务关闭与 best practices。
8. 产品/设计/接口文档
   [架构初始化总文档](./architecture/init.md) 仍是历史初始化基线与总览入口；当前 C4 权威更新落点以 [C4 文字总览](./architecture/c4-overview.md) 为准，当前服务边界以 [服务能力分配图](./architecture/service-capability-map.md) 为准，当前接口契约以 [服务接口契约基线](./architecture/service-interface-contract-baseline.md) 为准，后续新增文档统一补入 `docs/`。
   - [SQL 治理平台实施规格](./product/sql-governance-platform-implementation-spec.md)
   - [SQL 改写功能分层设计](./product/sql-rewrite-function-boundary-design.md)
   - [生产自动改写闭环任务拆解计划](./plans/production-rewrite-auto-apply-task-plan.md)
   - [L2 高级物化视图推荐任务拆解计划](./plans/l2-advanced-materialized-view-task-plan.md)
   - [前端复盘补漏基线](./product/frontend-retrospective-gap-closure-baseline.md)
   - [前端核心链路聚焦改造任务包](./plans/frontend-core-workflow-refocus-task-pack.md)
   - [C4 文字总览](./architecture/c4-overview.md)
   - [持久化基线](./architecture/persistence.md)
   - [消息抽象说明](./architecture/messaging-abstraction.md)
   - [服务能力分配图](./architecture/service-capability-map.md)
   - [服务接口契约基线](./architecture/service-interface-contract-baseline.md)
   - [SQL 治理接口扩展基线](./architecture/sql-governance-interface-extension-baseline.md)
   - [SQL 治理数据模型扩展](./architecture/sql-governance-data-model-extension.md)
   - [SQL 治理降级矩阵](./operations/sql-governance-degradation-matrix.md)
   - [前端设计系统](./frontend/design-system.md)
   - [前端表单组件治理](./frontend/form-component-governance.md)
   - [阿里 Java 规范适配](./quality/alibaba-java-guidelines.md)
   - [前后端分离基线检查](./quality/frontend-backend-separation-baseline.md)
8. [计划索引](./plans/README.md)
   执行计划入口，统一索引主执行计划、阶段计划与确认台账。
9. [Codex 集成治理蓝图](./plans/codex-governance-integration-blueprint.md)
   定义如何把 `docs/` 真值体系、任务台账、验证、closeout 与 Codex 的 `AGENTS`、project-scoped `.codex` 配置、hooks 和 foreman CLI 接到一条运行链上。
10. [主执行计划](./plans/master-execution-plan.md)
   当前全量执行控制文档，按 Harness Engineering 要求拆分阶段、Epic、Story、Task、验证矩阵与人工确认项。
11. [阶段0执行计划](./plans/phase-0-plan.md)
   初始化阶段历史计划，保留原任务顺序、依赖关系、验收口径与交付节奏。
12. [流程与治理审计说明](./plans/process-flow-and-governance-audit-2026-04-20.md)
   汇总当前工程交付全流程、产品运行流程、流程缺陷、整改建议与逐文档修订建议。
13. 部署文档
   - [本地部署指南](./deployments/local-setup.md)
   - [离线部署指南](./deployments/offline-setup.md)
   - [华为云部署指南](./deployments/huawei-cloud-setup.md)
   - [Hetu/MRS 测试环境部署与取证手册](./deployments/hetu-test-environment-deployment-runbook.md)
   - [压测生产规模证据取证手册](./deployments/benchmark-production-evidence-runbook.md)
   - [测试环境 smoke 基线](./deployments/test-environment-smoke-baseline.md)
   - [改写治理 smoke runbook](./deployments/rewrite-governance-smoke-runbook.md)
   - [可观测基线](./deployments/observability-baseline.md)
   - [备份恢复基线](./deployments/backup-recovery-baseline.md)
   - [CI 能力基线](./deployments/ci-capability-baseline.md)
   - [阶段门禁基线](./deployments/phase-gate-baseline.md)
   - [Sonar 质量门禁配置](./deployments/sonar-quality-gate-provisioning.md)
14. 仓库约定、任务台账与 ADR
   - 根级任务台账：`tasks.md`
   - 完成归档：`tasks-done.md`
   - 人工决策入口：`INBOX.md`
   - 机器参数：`.agent/config.json`
   - [生成仓库地图](./generated/repo-map.md)
   - `docs/exec-plans/active/`
   - `docs/exec-plans/completed/`
   - [Phase-F Story-003 交付记录](./deliveries/phase-f-story-003-ops-closeout.md)
15. 仓库约定与 ADR
   - [ADR 索引](./adr/README.md)
   - [ADR 模板](./adr/adr-template.md)
   - [Connector 与 MCP 安全边界](./security/connectors.md)
   - [等保合规说明](./security/compliance.md)
   - [人类约束历史账本](./references/human-constraint-history.md)
16. 原始需求与归档
   - `docs/references/raw-requirements/`
17. 复盘与持续治理
   - [复盘模板](./plans/retrospective-template.md)
   - [本轮文档治理复盘](./plans/document-governance-retrospective-2026-04-20.md)
   - [本轮文档治理修复复盘](./plans/document-governance-repair-retrospective-2026-04-20.md)
   - [流程与治理审计说明](./plans/process-flow-and-governance-audit-2026-04-20.md)

## 文档地图

- `architecture/`
  架构总览与初始化基线。
  - `c4-overview.md`：当前权威的文字版 C4 总览与更新落点。
  - `persistence.md`：MySQL 主持久化、核心追溯链、MyBatis XML 与增量脚本权威基线。
  - `messaging-abstraction.md`：Kafka 与消息能力的本地开发抽象模式、切换方式和契约约束。
  - `service-interface-contract-baseline.md`：4 微服务统一身份、错误码、DTO/事件和审计契约基线。
  - `sql-governance-interface-extension-baseline.md`：SQL 治理产品线新增的查询、解析、批量解析、推荐协同、开放接入与告警接口扩展基线。
  - `sql-governance-data-model-extension.md`：SQL 治理产品线新增对象、扩表现状、JSON/结构化字段取舍与落表策略。
  - `sql-rewrite-core-ir-architecture.md`：SQL 改写核心 L1-L5 IR 架构骨架、QBDAG 查询块分解、结构哈希、层级契约和与既有推荐规则等级的命名冲突处理。
- `product/`
  产品实施规格与页面、流程、角色、分期设计权威入口。
  - `sql-governance-platform-implementation-spec.md`：SQL 治理中后台 + 开放接入平台实施规格。
  - `sql-rewrite-function-boundary-design.md`：SQL 改写验证、推荐结果 / 改写记录、真实 SQL 改写历史的产品分层、边界和联动方案。
  - `frontend-retrospective-gap-closure-baseline.md`：前端复盘、规格补漏与 repo-closed 边界基线。
- `rules/`
  规则库与可执行约束。
  - `karpathy-guidelines.md`：Karpathy 风格 AI 编码行为的完整说明，保留四原则、适用形态与 SQLForge 落点。
- `plans/`
  执行计划、任务拆解与治理入口。
  - `l2-advanced-materialized-view-task-plan.md`：L2 高级物化视图推荐任务包，覆盖参数外提聚合、预 Join、星型聚合、Rollup、公共子图 MV，明确禁止 `EXACT_QUERY_MV`。
- `quality/`
  质量门禁、Java 规范治理与前后端分离检查基线。
- `operations/`
  Foreman 工作流、人类协作、本地开发、任务关闭与工程规则。
  - `codex-mcp-playbook.md`：Codex 在 SQLForge 中使用 MCP 的只读边界、manifest-level `mcp_profile` 约束、自动化入口和本地接入手册。
  - `sql-governance-degradation-matrix.md`：SQL 治理产品线在数据库、报表接口、Redis、外部装数与通知能力不可用时的降级矩阵。
- `agent-prompts/`
  多 agent 协作使用的角色 prompt 模板，由 Main Foreman / launcher 读取，不替代 `docs/` 与台账真值。
  - `auto-planner.md`：把需求输入转换为 exec plan 与 manifest 的全自动规划模板。
  - `auto-foreman.md`：从 collect summary 继续 fan-in / validate / closeout 的 autonomous Main Foreman 模板。
  - `requirement-normalizer.md`：把原始需求整理为标准化治理输入的模板。
  - `plan-shaper.md`：把标准化需求映射到现有 plan/story/task 边界的模板。
  - `task-shaper.md`：生成 candidate task pack 的模板。
  - `task-governance-reviewer.md`：在 materialize 之前审查 candidate task pack 是否满足治理门禁的模板。
- `adr/`
  架构决策记录与模板。
- `references/`
  原始需求、历史约束与长期输入归档。
  - `sql-rewrite-recommendation-research-2026-05-18.md`：SQL 推荐改写、物化视图透明改写、动态过滤、LLM 改写验证与相关专利的本轮调研归档。
- `security/`
  合规、安全与等保规则说明。
  - `connectors.md`：Connector 与 MCP 的允许范围、安全边界、禁用能力和验证清单。
- `frontend/`
  前端设计系统、组件与页面视觉规范。
  - `form-component-governance.md`：页面表单字段到日期时间、下拉、开关、数值、密码等组件的治理映射。
- `plans/`
  计划索引、主执行计划与分阶段执行计划。
  - `document-truth-baseline.md`：当前仓库事实、历史记录、目标边界与工程命名映射的真值分层。
  - `document-gap-matrix.md`：冲突、漂移、缺失与残余实现缺口矩阵。
  - `implementation-readiness.md`：编码前阅读顺序、主题权威来源和实施波次。
  - `codex-governance-integration-blueprint.md`：Codex 运行时接线蓝图，定义 `AGENTS`、`.codex/`、hooks、foreman CLI 与真值体系的关系。
  - `phase-prerequisite-matrix.md`：各阶段输入文档、ADR、规则、验证和确认点矩阵。
  - `process-flow-and-governance-audit-2026-04-20.md`：正式全流程说明、流程缺陷审计、整改建议和文档修订建议。
  - `production-rewrite-auto-apply-task-plan.md`：生产自动改写闭环任务拆解计划，覆盖改写审批、发布、运行时自动改写、历史留痕、差异暂停和页面追溯；暂不覆盖投产前本地/测试环境核验闭环。
  - `retrospective-template.md`：阶段或复杂批次复盘模板。
  - `document-governance-retrospective-2026-04-20.md`：本轮治理复盘实例。
  - `document-governance-repair-retrospective-2026-04-20.md`：本轮严格核验修复复盘实例。
  - `task-governance-extension-matrix.md`：Task 的人工确认点、数据影响、回滚扩展矩阵。
  - `frontend-core-workflow-refocus-task-pack.md`：HARN-FE-001A 确认后的前端展示层菜单与核心 SQL 工作流聚焦改造权威入口。
- `exec-plans/`
  已确认复杂执行计划的活动、归档与模板目录。
- `generated/`
  AI 导航和仓库结构快照生成物。
- `.codex/`
  Codex 项目级配置、hooks、policy 编译产物与运行态状态目录；这些文件不替代 `docs/` 真值。
- `deployments/`
  本地与目标环境部署说明。
  - `hetu-test-environment-deployment-runbook.md`：真实 Hetu / MRS 测试环境的最小部署、配置、启动、smoke 执行与证据留档手册。
  - `benchmark-production-evidence-runbook.md`：真实生产规模压测证据目录格式、`provenance.json` 来源元数据、`verify-benchmark-production-evidence.py` 校验入口、`audit-rewrite-production-readiness.py` 完成度审计入口、`verificationBundle` 与 `evidenceFileDigests` 生成边界。
  - `test-environment-smoke-baseline.md`：外部测试环境独立 CI/CD 的最小部署后 smoke 入口、覆盖范围与证据边界。
  - `rewrite-governance-smoke-runbook.md`：改写治理 repo-closed smoke、`PRW-012` 生产改写闭环 browser smoke、推荐 / 历史 / 告警契约检查与 environment-backed 边界说明。
  - `observability-baseline.md`：当前 logs/metrics/alerts 运维落地清单与缺口基线。
  - `backup-recovery-baseline.md`：当前备份对象、恢复目标与演练模板基线。
  - `ci-capability-baseline.md`：当前 GitHub Actions CI 覆盖、缺口与后续门禁接入映射。
  - `phase-gate-baseline.md`：当前 R-116/R-117/R-118 阶段门禁脚本、workflow 接线，以及 `repo-closed` / `environment-backed` 双层门禁语义。
  - `sonar-quality-gate-provisioning.md`：SonarQube fallback scan 与环境恢复项 runbook，显式区分 provisioning / enable，不再代表仓库默认硬阻断。
- `deliveries/`
  交付记录与阶段回写。
  - `init-completion.md`：阶段 0 初始化交付记录与历史 repair 记录。
  - `phase-f-story-003-ops-closeout.md`：`F-TASK-009` 收口的 Phase-F 运维、审计与恢复交付记录与 write-back 模板。

## 当前阶段说明

当前计划体系以 [主执行计划](./plans/master-execution-plan.md) 为主控文档，[阶段0执行计划](./plans/phase-0-plan.md) 作为历史阶段计划保留。活动任务状态以仓库根级 `tasks.md` / `tasks-done.md` 为准，复杂执行计划放入 `docs/exec-plans/`，仓库结构快照见 [docs/generated/repo-map.md](./generated/repo-map.md)。实现代码必须以后端强制分层、前后端分离、多环境配置、等保嵌入和文档优先为前提，不得绕过本文档体系直接推进编码。

默认执行口径：

- AI 执行任务时默认开启严格模式，具体以 [codex-rules.md](./rules/codex-rules.md) 中的 `R-165` 为准。
- 任何不确定、冲突、缺失或无法从当前仓库事实证明的内容，都不得擅自补写为既成事实，必须转入人工决定或补充流程。
- 任何任务都不得以“压缩上下文”或“快速交付”为由丢失需求原文、验证证据、历史记录、任务状态或文档同步项。
- 当前仓库门禁默认采用 `repo-closed` 主路径：build/test/lint、coverage、db-script、runtime smoke、knowledge lint 与 compliance baseline 仍是主线。Sonar、真实 Kafka 与外部测试环境 CI/CD 仅作为 `environment-backed` 增强项，不能替代仓库闭环。
- 当前已新增测试环境 minimal smoke 入口，用于外部测试环境 CI/CD 在部署后执行最小可运行验证；但它仍属于 `environment-backed` 部署后验证层，不替代仓库 `repo-closed` 主路径。
- 任务完成后的 Harness Engineering 动作以“上下文收缩 + 上下文清理”为准；`/contract`、`/clear` 若当前环境支持，可作为可选实现手段，但不是唯一工程要求，具体见 `R-168`。
- 正式业务首页 `/dashboard` 保留为产品功能页面；若后续实现 AI 交付进度页，按 `R-166` 作为独立临时子页面处理，且生产环境默认隐藏。

## 2026-04-20 治理增量

本轮文档治理新增以下执行型文档，用于把“当前真值”“目标边界”和“编码前置条件”分开消费：

- [文档真值基线](./plans/document-truth-baseline.md)
  当前仓库真值、历史记录、工程命名映射和目标架构的分层说明，以及初始化目标落点与当前权威文档的漂移映射。
- [实现就绪规范](./plans/implementation-readiness.md)
  任务开始前的读文档顺序、主题权威来源、冲突处理方式和分波次执行顺序。
- [服务能力分配图](./architecture/service-capability-map.md)
  4 微服务目标与当前 `governance` / `sqlforge-shared` / `query-execution` 的过渡映射。
- [C4 文字总览](./architecture/c4-overview.md)
  作为 `R-133` 的 C4 权威更新落点，统一维护 Level 1-4 的文字版架构说明。
- [服务接口契约基线](./architecture/service-interface-contract-baseline.md)
  统一身份上下文字段、错误码归属、服务间 DTO/事件边界和审计契约。
- [可观测基线](./deployments/observability-baseline.md)
  把当前 4 个后端服务的 logs/metrics/alerts 实现事实与生产运维清单收口到一处，避免实现与运维口径继续漂移。
- [备份恢复基线](./deployments/backup-recovery-baseline.md)
  把当前 MySQL、审计、导出元数据、消息兜底与密钥边界的备份恢复目标、责任分工和演练模板收口到一处，避免恢复 runbook 继续漂移。
- [CI 能力基线](./deployments/ci-capability-baseline.md)
  把当前 `.github/workflows/ci.yml` 已覆盖项、本地未入 CI 的命令，以及后续 phase gate / Java 扫描接入缺口收口到一处，避免 CI 口径继续漂移。
- [阶段门禁基线](./deployments/phase-gate-baseline.md)
  把当前 `R-116` / `R-117` / `R-118` 的脚本入口、GitHub Actions 接线和阻断边界收口到一处，避免“已接线”与“已自动阻断”继续混写。
- [多 agent 协作手册](./operations/multi-agent-playbook.md)
  定义 Main Foreman、manifest、worktree、prompt 模板，以及 `autoplan/full-auto/prepare/launch/collect` 脚本的治理边界、运行顺序和 SQLForge demo runbook。
- [从无 task 开始的治理自动化手册](./operations/requirements-to-task-playbook.md)
  定义 requirement normalization、candidate task pack、materialization gate，以及 `requirements_to_plan/task_materialize/governed_full_cycle` 脚本如何在不绕过审计链的前提下把“只有需求”推进到正式 task。
- [文档缺口矩阵](./plans/document-gap-matrix.md)
  把冲突、漂移、缺失项和残余实现缺口显式矩阵化。
- [阶段前置条件矩阵](./plans/phase-prerequisite-matrix.md)
  为每个阶段显式列出输入文档、ADR、规则、验证规则和人工确认点。
- [Task 治理扩展矩阵](./plans/task-governance-extension-matrix.md)
  在核心 10 字段之外，显式维护 Task 的人工确认点、数据影响和回滚策略。
- [复盘模板](./plans/retrospective-template.md)
  每轮复杂交付后的标准复盘骨架。
- [本轮文档治理修复复盘](./plans/document-governance-repair-retrospective-2026-04-20.md)
  本轮 7 项严格核验缺口修复的实际复盘记录。

## 2026-04-25 MCP 治理增量

- [Connector 与 MCP 安全边界](./security/connectors.md)
  固化 SQLForge 当前 MCP 治理基线、第一批只读 category、禁用 server 类型、manifest-level `mcp_profile` 角色边界、category onboarding、`mcp_doctor.py --check` 与 connector intake / validation checklist。
- [Codex MCP 使用手册](./operations/codex-mcp-playbook.md)
  固化 SQLForge 内部使用 Codex + MCP 的本地接入方式、自动化入口、`mcp_doctor.py --check`、证据写回规则，以及单 agent / multi-agent `mcp_profile` 的当前边界。
- [多 agent 协作手册](./operations/multi-agent-playbook.md)
  固化 `mcp_profiles` / `mcp_profile` manifest 契约、`explorer / validator` 的只读证据边界，以及 `prepare/launch/collect/full-auto` 的治理约束。

当前对 MCP 的产品定位固定为“受治理的只读证据增强”；它不是远端自动运维，也不是可写控制面。

后续开始任何非 trivial 编码任务时，优先阅读顺序调整为：

1. `docs/README.md`
2. `docs/plans/document-truth-baseline.md`
3. `docs/architecture/init.md`
4. `docs/rules/codex-rules.md`
5. `docs/rules/karpathy-guidelines.md`
6. `docs/quality/validation-rules.md`
7. 对应专项文档
8. `docs/plans/master-execution-plan.md`
9. [阶段前置条件矩阵](./plans/phase-prerequisite-matrix.md)
10. [Task 字段矩阵](./plans/task-spec-matrix.md)
11. [Task 治理扩展矩阵](./plans/task-governance-extension-matrix.md)
12. 根级任务台账与人工决策入口：`tasks.md` / `tasks-done.md` / `INBOX.md`

若任务涉及 MCP / 外部 connector，还必须追加阅读 [Connector 与 MCP 安全边界](./security/connectors.md) 与 [Codex MCP 使用手册](./operations/codex-mcp-playbook.md)；若同时涉及 multi-agent `mcp_profile`，再追加阅读 [多 agent 协作手册](./operations/multi-agent-playbook.md)。
