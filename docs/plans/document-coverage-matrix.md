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
| `docs/architecture/init.md` | Authority | 总体架构、规则、阶段、接口契约、任务模板 | Consumed | 主计划主基线 |
| `docs/architecture/messaging-abstraction.md` | Authority | `R-144` 消息抽象模式 | Consumed | 配置、消息实现、部署切换 |
| `docs/deliveries/init-completion.md` | Indexed | 阶段0交付记录 | Consumed | 阶段0真值、tag 回写、交付闭环 |
| `docs/deployments/local-setup.md` | Authority | 本地部署与健康检查 | Consumed | 本地环境、smoke、脚本说明 |
| `docs/deployments/offline-setup.md` | Authority | 离线部署 | Consumed | 部署文档统一基线 |
| `docs/deployments/huawei-cloud-setup.md` | Authority | 华为云私有云部署 | Consumed | 生产部署与 `KAFKA` 模式切换 |
| `docs/frontend/design-system.md` | Authority | 前端视觉与页面设计规则 | Consumed | Dashboard、业务页、主题系统 |
| `docs/plans/README.md` | Indexed | 计划导航 | Consumed | 计划入口与附录说明 |
| `docs/plans/master-execution-plan.md` | Authority | 当前主执行计划 | Consumed | 主控文档 |
| `docs/plans/phase-0-plan.md` | Indexed | 阶段0历史计划 | Consumed | 阶段0真值修正 |
| `docs/plans/document-coverage-matrix.md` | Indexed | 文档全量覆盖矩阵 | Consumed | 证明 `docs/` 全量纳入 |
| `docs/plans/task-spec-matrix.md` | Indexed | Harness Task 字段矩阵 | Consumed | 补齐 59 个 Task 的 10 字段 |
| `docs/quality/alibaba-java-guidelines.md` | Authority | Java 规范适配文档 | Consumed | Java 实现与扫描治理 |
| `docs/quality/frontend-backend-separation-baseline.md` | Authority | 前后端分离基线 | Consumed | 边界治理与脚本校验 |
| `docs/quality/validation-rules.md` | Authority | `R-116` 至 `R-154` 验证规则 | Consumed | 任务和阶段验证矩阵 |
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

## Completeness Statement

- 当前 `docs/` 目录下所有文件均已进入本矩阵。
- 主执行计划对 `Authority` 与 `Indexed` 文档全部建立了显式引用关系。
- `Archive` 文档已按 `R-062`、`R-154`、`R-155` 归档，不被遗漏，也不被误写成权威架构事实。
