# SQLForge Docs

本目录是 SQLForge 的唯一长期知识入口，遵循 Harness Engineering 与 `R-006` 的文档优先顺序。

## 阅读顺序

1. [架构初始化总文档](./architecture/init.md)
   入口总览，包含初始化规则基线、来源服务职责映射、13 项 ADR、4 阶段里程碑、等保规则、加速服务与异常回滚设计；后续追加规则以规则库为准，最终服务口径以已确认的 4 微服务目标为准。
2. [规则库](./rules/codex-rules.md)
   Codex 执行和仓库落地的 append-only 规则索引，按连续编号维护。
3. [验证规则](./quality/validation-rules.md)
   阶段门禁、任务验证、自动阻断、Java 规范和 harness 任务治理验证的快速索引。
4. [运维与协作文档](./operations/README.md)
   Foreman 工作流、人类协作、本地开发、任务关闭与 best practices。
5. 产品/设计/接口文档
   当前阶段以 [架构初始化总文档](./architecture/init.md) 中的接口契约与服务边界为准，后续新增文档统一补入 `docs/`。
   - [消息抽象说明](./architecture/messaging-abstraction.md)
   - [前端设计系统](./frontend/design-system.md)
   - [阿里 Java 规范适配](./quality/alibaba-java-guidelines.md)
   - [前后端分离基线检查](./quality/frontend-backend-separation-baseline.md)
6. [计划索引](./plans/README.md)
   执行计划入口，统一索引主执行计划、阶段计划与确认台账。
7. [主执行计划](./plans/master-execution-plan.md)
   当前全量执行控制文档，按 Harness Engineering 要求拆分阶段、Epic、Story、Task、验证矩阵与人工确认项。
8. [阶段0执行计划](./plans/phase-0-plan.md)
   初始化阶段历史计划，保留原任务顺序、依赖关系、验收口径与交付节奏。
9. 部署文档
   - [本地部署指南](./deployments/local-setup.md)
   - [离线部署指南](./deployments/offline-setup.md)
   - [华为云部署指南](./deployments/huawei-cloud-setup.md)
10. 仓库约定、任务台账与 ADR
   - 根级任务台账：`tasks.md`
   - 完成归档：`tasks-done.md`
   - 人工决策入口：`INBOX.md`
   - 机器参数：`.agent/config.json`
   - [生成仓库地图](./generated/repo-map.md)
   - `docs/exec-plans/active/`
   - `docs/exec-plans/completed/`
11. 仓库约定与 ADR
   - [ADR 索引](./adr/README.md)
   - [ADR 模板](./adr/adr-template.md)
   - [等保合规说明](./security/compliance.md)
   - [人类约束历史账本](./references/human-constraint-history.md)
12. 原始需求与归档
   - `docs/references/raw-requirements/`

## 文档地图

- `architecture/`
  架构总览与初始化基线。
  - `messaging-abstraction.md`：Kafka 与消息能力的本地开发抽象模式、切换方式和契约约束。
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
- `exec-plans/`
  已确认复杂执行计划的活动与归档目录。
- `generated/`
  AI 导航和仓库结构快照生成物。
- `deployments/`
  本地与目标环境部署说明。

## 当前阶段说明

当前计划体系以 [主执行计划](./plans/master-execution-plan.md) 为主控文档，[阶段0执行计划](./plans/phase-0-plan.md) 作为历史阶段计划保留。活动任务状态以仓库根级 `tasks.md` / `tasks-done.md` 为准，复杂执行计划放入 `docs/exec-plans/`，仓库结构快照见 [docs/generated/repo-map.md](./generated/repo-map.md)。实现代码必须以后端强制分层、前后端分离、多环境配置、等保嵌入和文档优先为前提，不得绕过本文档体系直接推进编码。
