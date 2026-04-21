# SQLForge Implementation Readiness

## Summary

本文件把“开始编码前必须先对齐什么”“每类实现应读哪些文档”“目标边界如何映射到当前仓库”固化成可直接执行的就绪规范。

适用范围：

- 新功能实现
- 架构拆分
- 文档驱动开发
- 安全、合规和部署能力补齐

## Document Consumption Contract

### Task start order

任一非 trivial 任务开始前，按以下顺序消费文档：

1. `docs/README.md`
2. `docs/plans/document-truth-baseline.md`
3. `docs/architecture/init.md`
4. `docs/rules/codex-rules.md`
5. `docs/quality/validation-rules.md`
6. 领域专项文档
7. `docs/plans/master-execution-plan.md`
8. `docs/plans/phase-prerequisite-matrix.md`
9. `docs/plans/task-spec-matrix.md`
10. `docs/plans/task-governance-extension-matrix.md`
11. 根级 `tasks.md` / `tasks-done.md` / `INBOX.md`

任务切换补充：

- 上一任务完成 closeout 后，不得直接沿用上一任务的局部推理、临时假设或未验证记忆。
- 切换到下一任务前，必须重新按上述 `Task start order` 建立上下文；`R-168` 要求的“上下文清理”只有在重新消费权威来源后才算真正完成。

### Authority by topic

| Topic | Primary authority | Supporting authority |
|:---|:---|:---|
| 当前仓库真值 | `docs/plans/document-truth-baseline.md` | 构建结果、验证日志、仓库结构 |
| 4 微服务边界 | `docs/architecture/init.md`, `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md` | `docs/architecture/service-capability-map.md` |
| 访问控制与合规 | `docs/security/access-control-spec.md`, `docs/security/compliance.md` | `docs/rules/codex-rules.md` |
| 前端信息架构与视觉 | `docs/frontend/design-system.md` | `docs/quality/frontend-backend-separation-baseline.md` |
| 验证口径 | `docs/quality/validation-rules.md` | `docs/quality/validation-log.md` |
| 当前执行顺序与依赖 | `docs/plans/master-execution-plan.md`, `docs/plans/phase-prerequisite-matrix.md` | `docs/plans/task-spec-matrix.md`, `docs/plans/task-governance-extension-matrix.md` |
| 历史原因追溯 | `docs/references/human-constraint-history.md` | `docs/references/raw-requirements/` |

### Conflict handling

1. 已实现代码与历史计划冲突时，以当前仓库真值为准，再回写计划。
2. 已确认目标与当前实现冲突时，以“目标仍有效、实现尚未完成”记录，不得反向改写目标。
3. 原始资料与当前权威文档冲突时，原始资料只用于追溯，当前权威文档用于执行。
4. 任何需要删除、废弃、重命名规则或主文档语义的动作，必须先进入人工确认流程。

## Current Coding Entry Criteria

进入编码前必须同时满足：

- 目标能力已能映射到 4 微服务之一。
- 目标能力已能映射到当前仓库中的承载位置或新模块计划。
- 输入、输出、错误码、审计要求、租户边界已知。
- 验证方式可执行，且能写回验证日志。
- 若涉及既有规则、接口或服务边界变化，已先更新相关文档或进入确认台账。
- 若上一任务刚完成 closeout，已重新按 `Task start order` 建立当前任务上下文，而不是沿用上一任务局部记忆。

## Execution Sequence

### Wave 1: Shared foundation

- 目标：让 `sqlforge-shared` 承载真正的跨服务公共能力。
- 首批建议能力：
  - error code constants
  - request/tenant context primitives
  - audit event contract
  - shared exception model
  - shared config and utility boundaries
- 完成标准：公共能力被 `governance` 消费，且不夹带业务实体。

### Wave 2: Public management service hardening

- 目标：把当前 `governance` 从“最小治理基线”推进到“公共管理服务可扩展底座”。
- 优先实现：
  - 身份上下文校验强化
  - 角色与租户边界
  - 数据源权限入口
  - 审计事件链路
  - 敏感字段存储策略
- 完成标准：管理域具备统一入口能力，但不吞并其他三个服务的业务职责。

### Wave 3: Query execution service skeleton

- 目标：建立查询执行服务的最小独立模块和接口骨架。
- 首批范围：
  - 查询提交
  - 同步执行占位闭环
  - 路由/缓存/轻量解析边界
  - 与公共管理服务的租户、数据源、审计交互
- 完成标准：边界清晰、接口可测，不要求一次性实现全部引擎能力。

### Wave 4: SQL optimization service skeleton

- 目标：建立异步解析、改写建议和加速建议骨架。
- 首批范围：
  - 任务提交
  - 任务状态查询
  - 建议模型
  - 与查询执行服务的异步协同契约
- 完成标准：异步任务模型独立，不阻塞联机查询主链路。

### Wave 5: Benchmark engine service skeleton

- 目标：建立独立压测模块，保持与生产路径隔离。
- 首批范围：
  - 压测任务模型
  - 报告模型
  - 任务提交流程
  - 报告查询
- 完成标准：压测与生产治理边界清晰，审计与隔离要求明确。

### Wave 6: Frontend cockpit and business routes

- 目标：让前端真实承载已交付能力，而不是只保留占位。
- 首批范围：
  - Dashboard 驾驶舱
  - 五大业务页独立路由
  - 已交付治理能力可见化
  - 深色设计系统 token 化
- 完成标准：前后端分离不退化，页面结构遵循任务流。

### Wave 7: Deployment and compliance closeout

- 目标：把文档、验证、部署和复盘形成闭环。
- 首批范围：
  - CI 门禁
  - Java 规范扫描
  - 部署说明与 compose 语义对齐
  - 备份恢复模板
  - 阶段复盘
- 完成标准：每轮交付都能留下验证证据和后续治理项。

## Delivery Rules For New Work

- 一个任务只实现一个高内聚目标。
- 不把“服务拆分”“安全加固”“前端重构”混成同一个 commit 目标。
- 任何新增模块必须先标注它属于哪一个最终服务域。
- 任何跨域调用必须先定义 DTO、事件或接口契约，再写实现。
- 任何新增安全逻辑必须同时定义验证场景和审计落点。

## Exit Criteria For Each Wave

- 代码、文档、验证三者一致。
- `Implemented Fact` 与 `Confirmed Target` 不再混写。
- 相关规则的验证命令可真实执行。
- 剩余缺口被显式列出，不隐含在提交说明之外。

## Related Documents

- `docs/plans/document-truth-baseline.md`
- `docs/plans/document-gap-matrix.md`
- `docs/plans/master-execution-plan.md`
- `docs/plans/phase-prerequisite-matrix.md`
- `docs/plans/task-spec-matrix.md`
- `docs/plans/task-governance-extension-matrix.md`
- `docs/architecture/service-capability-map.md`
- `docs/architecture/service-interface-contract-baseline.md`
