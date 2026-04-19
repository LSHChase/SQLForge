# SQLForge Docs

本目录是 SQLForge 的唯一长期知识入口，遵循 Harness Engineering 与 `R-006` 的文档优先顺序。

## 阅读顺序

1. [架构初始化总文档](./architecture/init.md)
   入口总览，包含 115 条规则、11 个原始服务、13 项 ADR、4 阶段里程碑、等保规则、加速服务与异常回滚设计。
2. [规则库](./rules/codex-rules.md)
   Codex 执行和仓库落地的 append-only 规则索引，按 `R-001` 至 `R-115` 编号维护。
3. 产品/设计/接口文档
   当前阶段以 [架构初始化总文档](./architecture/init.md) 中的接口契约与服务边界为准，后续新增文档统一补入 `docs/`。
   - [消息抽象说明](./architecture/messaging-abstraction.md)
4. [阶段0执行计划](./plans/phase-0-plan.md)
   当前初始化阶段的任务顺序、依赖关系、验收口径与交付节奏。
5. [本地部署指南](./deployments/local-setup.md)
   本地 Docker 依赖、启动脚本、健康检查与故障排查说明。
6. 仓库约定与 ADR
   - [ADR 索引](./adr/README.md)
   - [ADR 模板](./adr/adr-template.md)
   - [等保合规说明](./security/compliance.md)
   - [人类约束历史账本](./references/human-constraint-history.md)
7. 原始需求与归档
   - `docs/references/raw-requirements/`

## 文档地图

- `architecture/`
  架构总览与初始化基线。
  - `messaging-abstraction.md`：Kafka 与消息能力的本地开发抽象模式、切换方式和契约约束。
- `rules/`
  规则库与可执行约束。
- `adr/`
  架构决策记录与模板。
- `references/`
  原始需求、历史约束与长期输入归档。
- `security/`
  合规、安全与等保规则说明。
- `plans/`
  分阶段执行计划。
- `deployments/`
  本地与目标环境部署说明。

## 当前阶段说明

阶段 0 的目标是完成项目基础设施初始化。实现代码必须以后端强制分层、前后端分离、多环境配置、等保嵌入和文档优先为前提，不得绕过本文档体系直接推进编码。
