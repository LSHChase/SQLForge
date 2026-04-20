# SQLForge Docs

本目录是 SQLForge 的唯一长期知识入口，遵循 Harness Engineering 与 `R-006` 的文档优先顺序。

## 阅读顺序

1. [文档真值基线](./plans/document-truth-baseline.md)
   当前仓库事实、已确认目标、历史记录和原始归档的分层说明；需要判断“现在仓库真实状态”时先读本文。
2. [架构初始化总文档](./architecture/init.md)
   入口总览，包含初始化规则基线、来源服务职责映射、13 项 ADR、4 阶段里程碑、等保规则、加速服务与异常回滚设计；后续追加规则以规则库为准，最终服务口径以已确认的 4 微服务目标为准。
3. [规则库](./rules/codex-rules.md)
   Codex 执行和仓库落地的 append-only 规则索引，按连续编号维护。
4. [验证规则](./quality/validation-rules.md)
   阶段门禁、任务验证、自动阻断、Java 规范和 harness 任务治理验证的快速索引。
5. [实现就绪规范](./plans/implementation-readiness.md)
   编码前文档消费顺序、主题权威来源、冲突处理方式和波次执行顺序。
6. [运维与协作文档](./operations/README.md)
   Foreman 工作流、人类协作、本地开发、任务关闭与 best practices。
7. 产品/设计/接口文档
   [架构初始化总文档](./architecture/init.md) 仍是历史初始化基线与总览入口；当前 C4 权威更新落点以 [C4 文字总览](./architecture/c4-overview.md) 为准，当前服务边界以 [服务能力分配图](./architecture/service-capability-map.md) 为准，当前接口契约以 [服务接口契约基线](./architecture/service-interface-contract-baseline.md) 为准，后续新增文档统一补入 `docs/`。
   - [C4 文字总览](./architecture/c4-overview.md)
   - [消息抽象说明](./architecture/messaging-abstraction.md)
   - [服务能力分配图](./architecture/service-capability-map.md)
   - [服务接口契约基线](./architecture/service-interface-contract-baseline.md)
   - [前端设计系统](./frontend/design-system.md)
   - [阿里 Java 规范适配](./quality/alibaba-java-guidelines.md)
   - [前后端分离基线检查](./quality/frontend-backend-separation-baseline.md)
8. [计划索引](./plans/README.md)
   执行计划入口，统一索引主执行计划、阶段计划与确认台账。
9. [主执行计划](./plans/master-execution-plan.md)
   当前全量执行控制文档，按 Harness Engineering 要求拆分阶段、Epic、Story、Task、验证矩阵与人工确认项。
10. [阶段0执行计划](./plans/phase-0-plan.md)
   初始化阶段历史计划，保留原任务顺序、依赖关系、验收口径与交付节奏。
11. [流程与治理审计说明](./plans/process-flow-and-governance-audit-2026-04-20.md)
   汇总当前工程交付全流程、产品运行流程、流程缺陷、整改建议与逐文档修订建议。
12. 部署文档
   - [本地部署指南](./deployments/local-setup.md)
   - [离线部署指南](./deployments/offline-setup.md)
   - [华为云部署指南](./deployments/huawei-cloud-setup.md)
13. 仓库约定、任务台账与 ADR
   - 根级任务台账：`tasks.md`
   - 完成归档：`tasks-done.md`
   - 人工决策入口：`INBOX.md`
   - 机器参数：`.agent/config.json`
   - [生成仓库地图](./generated/repo-map.md)
   - `docs/exec-plans/active/`
   - `docs/exec-plans/completed/`
14. 仓库约定与 ADR
   - [ADR 索引](./adr/README.md)
   - [ADR 模板](./adr/adr-template.md)
   - [等保合规说明](./security/compliance.md)
   - [人类约束历史账本](./references/human-constraint-history.md)
15. 原始需求与归档
   - `docs/references/raw-requirements/`
16. 复盘与持续治理
   - [复盘模板](./plans/retrospective-template.md)
   - [本轮文档治理复盘](./plans/document-governance-retrospective-2026-04-20.md)
   - [本轮文档治理修复复盘](./plans/document-governance-repair-retrospective-2026-04-20.md)
   - [流程与治理审计说明](./plans/process-flow-and-governance-audit-2026-04-20.md)

## 文档地图

- `architecture/`
  架构总览与初始化基线。
  - `c4-overview.md`：当前权威的文字版 C4 总览与更新落点。
  - `messaging-abstraction.md`：Kafka 与消息能力的本地开发抽象模式、切换方式和契约约束。
  - `service-interface-contract-baseline.md`：4 微服务统一身份、错误码、DTO/事件和审计契约基线。
- `rules/`
  规则库与可执行约束。
- `quality/`
  质量门禁、Java 规范治理与前后端分离检查基线。
- `operations/`
  Foreman 工作流、人类协作、本地开发、任务关闭与工程规则。
- `adr/`
  架构决策记录与模板。
- `references/`
  原始需求、历史约束与长期输入归档。
- `security/`
  合规、安全与等保规则说明。
- `frontend/`
  前端设计系统、组件与页面视觉规范。
- `plans/`
  计划索引、主执行计划与分阶段执行计划。
  - `document-truth-baseline.md`：当前仓库事实、历史记录与目标边界的真值分层。
  - `document-gap-matrix.md`：冲突、漂移、缺失与残余实现缺口矩阵。
  - `implementation-readiness.md`：编码前阅读顺序、主题权威来源和实施波次。
  - `phase-prerequisite-matrix.md`：各阶段输入文档、ADR、规则、验证和确认点矩阵。
  - `process-flow-and-governance-audit-2026-04-20.md`：正式全流程说明、流程缺陷审计、整改建议和文档修订建议。
  - `retrospective-template.md`：阶段或复杂批次复盘模板。
  - `document-governance-retrospective-2026-04-20.md`：本轮治理复盘实例。
  - `document-governance-repair-retrospective-2026-04-20.md`：本轮严格核验修复复盘实例。
  - `task-governance-extension-matrix.md`：59 个 Task 的人工确认点、数据影响、回滚扩展矩阵。
- `exec-plans/`
  已确认复杂执行计划的活动与归档目录。
- `generated/`
  AI 导航和仓库结构快照生成物。
- `deployments/`
  本地与目标环境部署说明。

## 当前阶段说明

当前计划体系以 [主执行计划](./plans/master-execution-plan.md) 为主控文档，[阶段0执行计划](./plans/phase-0-plan.md) 作为历史阶段计划保留。活动任务状态以仓库根级 `tasks.md` / `tasks-done.md` 为准，复杂执行计划放入 `docs/exec-plans/`，仓库结构快照见 [docs/generated/repo-map.md](./generated/repo-map.md)。实现代码必须以后端强制分层、前后端分离、多环境配置、等保嵌入和文档优先为前提，不得绕过本文档体系直接推进编码。

默认执行口径：

- AI 执行任务时默认开启严格模式，具体以 [codex-rules.md](./rules/codex-rules.md) 中的 `R-165` 为准。
- 任何不确定、冲突、缺失或无法从当前仓库事实证明的内容，都不得擅自补写为既成事实，必须转入人工决定或补充流程。
- 任何任务都不得以“压缩上下文”或“快速交付”为由丢失需求原文、验证证据、历史记录、任务状态或文档同步项。
- 正式业务首页 `/dashboard` 保留为产品功能页面；若后续实现 AI 交付进度页，按 `R-166` 作为独立临时子页面处理，且生产环境默认隐藏。

## 2026-04-20 治理增量

本轮文档治理新增以下执行型文档，用于把“当前真值”“目标边界”和“编码前置条件”分开消费：

- [文档真值基线](./plans/document-truth-baseline.md)
  当前仓库真值、历史记录和目标架构的分层说明，以及初始化目标落点与当前权威文档的漂移映射。
- [实现就绪规范](./plans/implementation-readiness.md)
  任务开始前的读文档顺序、主题权威来源、冲突处理方式和分波次执行顺序。
- [服务能力分配图](./architecture/service-capability-map.md)
  4 微服务目标与当前 `governance-service` / `sqlforge-common` 的过渡映射。
- [C4 文字总览](./architecture/c4-overview.md)
  作为 `R-133` 的 C4 权威更新落点，统一维护 Level 1-4 的文字版架构说明。
- [服务接口契约基线](./architecture/service-interface-contract-baseline.md)
  统一身份上下文字段、错误码归属、服务间 DTO/事件边界和审计契约。
- [文档缺口矩阵](./plans/document-gap-matrix.md)
  把冲突、漂移、缺失项和残余实现缺口显式矩阵化。
- [阶段前置条件矩阵](./plans/phase-prerequisite-matrix.md)
  为每个阶段显式列出输入文档、ADR、规则、验证规则和人工确认点。
- [Task 治理扩展矩阵](./plans/task-governance-extension-matrix.md)
  在核心 10 字段之外，显式维护 59 个 Task 的人工确认点、数据影响和回滚策略。
- [复盘模板](./plans/retrospective-template.md)
  每轮复杂交付后的标准复盘骨架。
- [本轮文档治理修复复盘](./plans/document-governance-repair-retrospective-2026-04-20.md)
  本轮 7 项严格核验缺口修复的实际复盘记录。

后续开始任何非 trivial 编码任务时，优先阅读顺序调整为：

1. `docs/README.md`
2. `docs/plans/document-truth-baseline.md`
3. `docs/architecture/init.md`
4. `docs/rules/codex-rules.md`
5. `docs/quality/validation-rules.md`
6. 对应专项文档
7. `docs/plans/master-execution-plan.md`
8. [阶段前置条件矩阵](./plans/phase-prerequisite-matrix.md)
9. [Task 字段矩阵](./plans/task-spec-matrix.md)
10. [Task 治理扩展矩阵](./plans/task-governance-extension-matrix.md)
11. 根级任务台账与人工决策入口：`tasks.md` / `tasks-done.md` / `INBOX.md`
