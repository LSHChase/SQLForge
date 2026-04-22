# SQLForge Document Coverage Matrix

本文件用于证明主执行计划已覆盖当前 `docs/` 目录下全部文档与原始资料归档，不遗漏、不改写、不跳过。

## 覆盖规则

- `Authority`：当前计划与后续实现必须直接遵循的权威文档
- `Indexed`：目录索引、模板、完成记录、导航文档
- `Archive`：原始资料、快照、来源元数据；必须保留并可追溯，但不直接替代权威文档
- `Status`：
  - `Consumed`：已被主执行计划显式纳入证据或追踪矩阵
  - `Referenced`：已在索引、部署、计划或 ADR 中被引用
  - `Archived`：已归档，按 `R-062`、`R-154`、`R-155` 保留

## Coverage Matrix

| Path | Type | Role | Current status | Plan usage |
|:---|:---|:---|:---|:---|
| `docs/README.md` | Authority | 文档入口 | Consumed | 主计划证据基础、阅读顺序入口 |
| `docs/architecture/c4-overview.md` | Authority | 当前权威的文字版 C4 架构总览与更新落点 | Referenced | 承接 `R-133` 的 C4 同步要求，统一维护 Level 1-4 文字架构说明 |
| `docs/architecture/init.md` | Authority | 总体架构、规则、阶段、接口契约、任务模板 | Consumed | 主计划主基线 |
| `docs/architecture/persistence.md` | Authority | MySQL 持久化、核心追溯链、MyBatis XML 与增量脚本基线 | Consumed | `R-031`,`R-055`,`R-065`,`R-129` 当前权威落点 |
| `docs/architecture/messaging-abstraction.md` | Authority | `R-144` 消息抽象模式 | Consumed | 配置、消息实现、部署切换 |
| `docs/architecture/service-capability-map.md` | Authority | 4 微服务与当前仓库模块的能力映射 | Consumed | 服务拆分、common 边界、`governance` 过渡约束 |
| `docs/architecture/service-interface-contract-baseline.md` | Authority | 统一身份、错误码、DTO/事件和审计契约基线 | Consumed | 服务实现前的接口级约束 |
| `docs/deliveries/init-completion.md` | Indexed | 阶段0交付记录 | Consumed | 阶段0真值、tag 回写、交付闭环 |
| `docs/deliveries/phase-f-story-003-ops-closeout.md` | Indexed | Phase-F Story-003 运维、审计与恢复交付记录 | Consumed | `F-TASK-009` 的 commit/tag/write-back 闭环与模板权威落点 |
| `docs/deployments/local-setup.md` | Authority | 本地部署与健康检查 | Consumed | 本地环境、smoke、脚本说明 |
| `docs/deployments/offline-setup.md` | Authority | 离线部署 | Consumed | 部署文档统一基线 |
| `docs/deployments/huawei-cloud-setup.md` | Authority | 华为云私有云部署 | Consumed | 生产部署与 `KAFKA` 模式切换 |
| `docs/deployments/observability-baseline.md` | Authority | 当前 logs/metrics/alerts 运维落地清单 | Consumed | `F-TASK-007` 的可观测基线、实现映射与缺口权威落点 |
| `docs/deployments/backup-recovery-baseline.md` | Authority | 当前备份对象、恢复目标与演练模板基线 | Consumed | `F-TASK-008` 的恢复基线、责任分工与验收模板权威落点 |
| `docs/deployments/ci-capability-baseline.md` | Authority | 当前 GitHub Actions CI 覆盖与缺口基线 | Consumed | `F-TASK-004` 的 CI 盘点、门禁缺口与后续任务范围权威落点 |
| `docs/deployments/phase-gate-baseline.md` | Authority | 当前 R-116/R-117/R-118 阶段门禁脚本与 workflow 基线 | Consumed | `F-TASK-005` 的 phase gate 接线、阻断边界与后续缺口权威落点 |
| `docs/generated/repo-map.md` | Indexed | 仓库结构导航快照 | Consumed | AI 导航、仓库结构入口与目录真值辅助说明 |
| `docs/frontend/design-system.md` | Authority | 前端视觉与页面设计规则 | Consumed | Dashboard、业务页、主题系统 |
| `docs/operations/README.md` | Indexed | 运维与协作索引 | Consumed | operations 文档入口 |
| `docs/operations/best-practices.md` | Authority | 可泛化工程规则账本 | Referenced | 复用规则、Root Cause/Cure/Generalization 沉淀 |
| `docs/operations/foreman-workflow.md` | Authority | foreman 任务流、开发循环与上下文收缩/清理 | Consumed | 任务入口、开发循环、closeout 与上下文切换闭环 |
| `docs/operations/git-and-task-closeout.md` | Authority | Git 边界、上下文收缩/清理与任务关闭顺序 | Consumed | 单任务关闭、审计链、commit 规则与 closeout 顺序 |
| `docs/operations/human-collaboration.md` | Authority | 人机协作边界、命令可用性与脏工作树处理 | Consumed | stop/continue、冲突与 `/contract` / `/clear` 可用性约束 |
| `docs/operations/local-development.md` | Authority | 本地命令、脚本与环境入口 | Referenced | 本地开发验证与环境约束 |
| `docs/plans/README.md` | Indexed | 计划导航 | Consumed | 计划入口与附录说明 |
| `docs/plans/master-execution-plan.md` | Authority | 当前主执行计划 | Consumed | 主控文档 |
| `docs/plans/phase-0-plan.md` | Indexed | 阶段0历史计划 | Consumed | 阶段0真值修正 |
| `docs/plans/document-coverage-matrix.md` | Indexed | 文档全量覆盖矩阵 | Consumed | 证明 `docs/` 全量纳入 |
| `docs/plans/document-gap-matrix.md` | Indexed | 冲突、漂移、缺失与残余实现缺口矩阵 | Consumed | 严格核验闭口检查 |
| `docs/plans/document-truth-baseline.md` | Authority | 当前仓库真值、历史记录、目标边界分层与漂移映射 | Consumed | 编码前真值判断、漂移治理、文档消费入口 |
| `docs/plans/codex-governance-integration-blueprint.md` | Authority | Codex 运行时接线蓝图与执行前/中/后治理契约 | Consumed | `AGENTS`、project-scoped `.codex`、hooks、foreman CLI 与真值体系的接线权威 |
| `docs/plans/implementation-readiness.md` | Authority | 编码前置消费顺序、主题权威来源、执行波次 | Consumed | 实施顺序、冲突处理、任务进入条件 |
| `docs/plans/phase-prerequisite-matrix.md` | Authority | 阶段输入文档、ADR、规则、验证和确认点矩阵 | Consumed | 阶段进入前置条件检查 |
| `docs/plans/process-flow-and-governance-audit-2026-04-20.md` | Indexed | 正式全流程说明、流程缺陷审计与整改建议 | Consumed | 供后续接手人与治理批次快速理解当前执行流程与缺陷闭口优先级 |
| `docs/plans/retrospective-template.md` | Indexed | 阶段与复杂批次复盘模板 | Referenced | 复盘闭环与后续治理沉淀 |
| `docs/plans/document-governance-retrospective-2026-04-20.md` | Indexed | 本轮文档治理复盘记录 | Consumed | 漂移、缺口和后续治理沉淀 |
| `docs/plans/document-governance-repair-retrospective-2026-04-20.md` | Indexed | 本轮严格核验缺口修复复盘记录 | Consumed | 治理闭口与 repair 批次追溯 |
| `docs/plans/task-spec-matrix.md` | Indexed | Harness Task 字段矩阵 | Consumed | 补齐 Task 的 10 字段 |
| `docs/plans/task-governance-extension-matrix.md` | Indexed | Task 的确认点、数据影响、回滚扩展矩阵 | Consumed | 严格治理扩展字段追踪 |
| `docs/quality/alibaba-java-guidelines.md` | Authority | Java 规范适配文档 | Consumed | Java 实现与扫描治理 |
| `docs/quality/frontend-backend-separation-baseline.md` | Authority | 前后端分离基线 | Consumed | 边界治理与脚本校验 |
| `docs/quality/validation-log.md` | Indexed | 验证行为审计日志 | Referenced | 验证证据追溯与关闭链路 |
| `docs/quality/validation-rules.md` | Authority | `R-116` 至 `R-161` 验证规则与 `R-168` 执行衔接 | Consumed | 任务、阶段与上下文收尾验证矩阵 |
| `docs/references/human-constraint-history.md` | Authority | 长期约束历史账本 | Consumed | 规则追加与人类决策追溯 |
| `docs/references/raw-requirements/alibaba-java-guidelines/Java开发手册(黄山版).pdf` | Archive | Java 规范原始 PDF | Archived | `R-154` 来源追溯 |
| `docs/references/raw-requirements/alibaba-java-guidelines/README.snapshot.md` | Archive | Java 规范原始 README 快照 | Archived | `R-154` 来源追溯 |
| `docs/references/raw-requirements/alibaba-java-guidelines/license.txt` | Archive | 原始资料许可证 | Archived | 归档保留，不作为执行基线 |
| `docs/references/raw-requirements/alibaba-java-guidelines/source-metadata.md` | Archive | 版本、来源、校验元数据 | Archived | `R-154` 来源追溯 |
| `docs/rules/codex-rules.md` | Authority | 仓库规则库 | Consumed | 所有实现与验证门禁 |
| `docs/security/access-control-spec.md` | Authority | 访问控制专项规格 | Consumed | 身份、角色、资源、审计实现基线 |
| `docs/security/compliance.md` | Authority | 等保合规说明 | Consumed | `R-111` 至 `R-115` 合规基线 |
| `docs/adr/README.md` | Indexed | ADR 索引 | Consumed | 决策索引、编号连续性检查 |
| `docs/adr/adr-template.md` | Indexed | ADR 模板 | Referenced | 新 ADR 继续使用该模板 |
| `docs/adr/ADR-001-order-service-database-selection-example.md` | Authority | 事务主库存储选型 | Consumed | 数据与持久化规划 |
| `docs/adr/ADR-002-microservice-splitting-and-bounded-contexts.md` | Authority | 4 微服务边界 | Consumed | 服务拆分主线 |
| `docs/adr/ADR-003-hudi-copy-on-write-write-strategy.md` | Authority | Hudi COW 策略 | Consumed | 数据湖与加速写策略 |
| `docs/adr/ADR-004-hetu-integration-boundary.md` | Authority | Hetu 三模式接入边界 | Consumed | 查询执行与华为云集成 |
| `docs/adr/ADR-005-sql-parser-build-vs-buy-boundary.md` | Authority | SQL parser 自研/复用边界 | Consumed | 解析与优化服务设计 |
| `docs/adr/ADR-006-lineage-and-metadata-management.md` | Authority | 血缘与元数据双存储方案 | Consumed | 公共管理服务设计 |
| `docs/adr/ADR-007-benchmark-engine-production-isolation.md` | Authority | 压测隔离策略 | Consumed | 压测引擎服务设计 |
| `docs/adr/ADR-008-yonghong-bi-integration-and-jdbc-driver-scope.md` | Authority | BI 接入与 JDBC 边界 | Referenced | 外部接入生态预留 |
| `docs/adr/ADR-009-data-retention-and-destruction.md` | Authority | 数据保留与销毁策略 | Consumed | 历史、审计、恢复规划 |
| `docs/adr/ADR-010-cross-border-data-and-multi-region-reserve.md` | Authority | 多区域扩展预留 | Referenced | 部署与后续阶段预留 |
| `docs/adr/ADR-011-cache-consistency-with-hudi-timestamp.md` | Authority | 缓存一致性策略 | Referenced | 查询执行缓存策略 |
| `docs/adr/ADR-012-saga-plus-local-transaction.md` | Authority | Saga + 本地事务 | Referenced | 跨服务事务编排 |
| `docs/adr/ADR-013-acceleration-service-and-materialized-view-strategy.md` | Authority | 加速与物化视图策略 | Consumed | SQL 优化服务设计 |
| `docs/exec-plans/active/.gitkeep` | Indexed | 活动执行计划目录占位文件 | Referenced | 保持活动执行计划目录可追踪 |
| `docs/exec-plans/completed/.gitkeep` | Indexed | 完成执行计划目录占位文件 | Referenced | 保持完成执行计划目录可追踪 |
| `docs/exec-plans/completed/HARN-009-closeout-boundary-repair-plan.md` | Indexed | 当前 closeout 边界与完成台账结构修复批次执行计划 | Consumed | 收口 HARN-008 复盘中发现的归档边界和 done-ledger 结构校验缺口 |
| `docs/exec-plans/completed/HARN-007-codex-runtime-integration-plan.md` | Indexed | 已完成的 Codex 运行时集成治理批次执行计划 | Referenced | 追溯 HARN-007 的原始批次目标、交付件与验证顺序 |
| `docs/exec-plans/completed/HARN-008-governance-runtime-hardening-plan.md` | Indexed | 已完成的 Codex 治理/runtime 闭口批次执行计划 | Referenced | 追溯 HARN-008 对 6 个治理闭口点的实现、验证与 closeout 顺序 |

## Completeness Statement

- 当前 `docs/` 目录下所有文件均已进入本矩阵。
- 主执行计划已显式引用当前执行所需的核心 `Authority` 文档与关键 `Indexed` 文档；其余条目通过本矩阵、ADR 索引、计划索引或目录索引追踪。
- `Archive` 文档已按 `R-062`、`R-154`、`R-155` 归档，不被遗漏，也不被误写成权威架构事实。

## Update Note 2026-04-20

- 新增治理文档已正式并入主矩阵，而不是只保留在增量附录中。
- 以上新增文档不替代历史初始化文本。
- `Authority` 类型文档已被 `docs/README.md` 与 `master-execution-plan.md` 主体部分显式引用。
