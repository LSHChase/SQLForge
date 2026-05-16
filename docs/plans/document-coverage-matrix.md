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
| `docs/SQLForge.md` | Archive | 早期 SQLForge 完整技术文档草稿 | Archived | 保留历史产品/架构输入，不替代当前 `docs/` 权威入口与计划矩阵 |
| `docs/SQLForgeV1.0.md` | Archive | 早期架构初始化与规则草稿 | Archived | 保留历史架构输入，当前规则以 `docs/architecture/init.md` 与 `docs/rules/codex-rules.md` 为准 |
| `docs/SQLTest.md` | Archive | 早期 Trino/OLAP 压测平台方案草稿 | Archived | 保留历史压测方案输入，当前执行以 benchmark 与 validation 文档为准 |
| `docs/architecture/c4-overview.md` | Authority | 当前权威的文字版 C4 架构总览与更新落点 | Referenced | 承接 `R-133` 的 C4 同步要求，统一维护 Level 1-4 文字架构说明 |
| `docs/architecture/init.md` | Authority | 总体架构、规则、阶段、接口契约、任务模板 | Consumed | 主计划主基线 |
| `docs/architecture/persistence.md` | Authority | MySQL 持久化、核心追溯链、MyBatis XML 与增量脚本基线 | Consumed | `R-031`,`R-055`,`R-065`,`R-129` 当前权威落点 |
| `docs/architecture/messaging-abstraction.md` | Authority | `R-144` 消息抽象模式 | Consumed | 配置、消息实现、部署切换 |
| `docs/architecture/service-capability-map.md` | Authority | 4 微服务与当前仓库模块的能力映射 | Consumed | 服务拆分、common 边界、`governance` 过渡约束 |
| `docs/architecture/service-interface-contract-baseline.md` | Authority | 统一身份、错误码、DTO/事件和审计契约基线 | Consumed | 服务实现前的接口级约束 |
| `docs/architecture/sql-governance-interface-extension-baseline.md` | Authority | SQL 治理产品线的查询、历史、解析、推荐、压测、开放接入与告警接口扩展基线 | Consumed | `HARN-042` 与后续 D/E/F Story 的接口契约权威落点 |
| `docs/architecture/sql-governance-data-model-extension.md` | Authority | SQL 治理产品线新增对象、扩表现状、落表策略与追溯键基线 | Consumed | `HARN-042` 与后续 D/E/F Story 的数据模型与持久化权威落点 |
| `docs/deliveries/init-completion.md` | Indexed | 阶段0交付记录 | Consumed | 阶段0真值、tag 回写、交付闭环 |
| `docs/deliveries/phase-f-story-003-ops-closeout.md` | Indexed | Phase-F Story-003 运维、审计与恢复交付记录 | Consumed | `F-TASK-009` 的 commit/tag/write-back 闭环与模板权威落点 |
| `docs/deployments/local-setup.md` | Authority | 本地部署与健康检查 | Consumed | 本地环境、smoke、脚本说明 |
| `docs/deployments/offline-setup.md` | Authority | 离线部署 | Consumed | 部署文档统一基线 |
| `docs/deployments/huawei-cloud-setup.md` | Authority | 华为云私有云部署 | Consumed | 生产部署与 `KAFKA` 模式切换 |
| `docs/deployments/hetu-test-environment-deployment-runbook.md` | Authority | 真实 Hetu / MRS 测试环境的最小部署、配置、启动、smoke 执行与证据留档手册 | Consumed | 外部测试环境 owner 部署 `governance` / `query-execution`、执行 `run-hetu-env-smoke.sh` 并保留 `JDBC` / `REST` / `CLIENT` 证据的权威落点 |
| `docs/deployments/test-environment-smoke-baseline.md` | Authority | 外部测试环境部署后 minimal smoke 入口、覆盖范围与证据边界 | Consumed | `F-TASK-033` 的环境无关 smoke 入口、外部 CI/CD 调用方式与 repo-closed 边界权威落点 |
| `docs/deployments/acceleration-rewrite-governance-smoke-runbook.md` | Authority | 加速与改写治理工作台 repo-closed smoke、生产改写闭环 browser smoke、契约检查与 environment-backed 边界 runbook | Consumed | `HARN-142` 的 `npm run smoke:acceleration-governance`、工作台 browser smoke、`PRW-012` 生产改写闭环 browser smoke、推荐/历史/告警契约检查与 `HARN-016` / `INBOX-002` 外部证据边界权威落点 |
| `docs/deployments/observability-baseline.md` | Authority | 当前 logs/metrics/alerts 运维落地清单 | Consumed | `F-TASK-007` 的可观测基线、实现映射与缺口权威落点 |
| `docs/deployments/backup-recovery-baseline.md` | Authority | 当前备份对象、恢复目标与演练模板基线 | Consumed | `F-TASK-008` 的恢复基线、责任分工与验收模板权威落点 |
| `docs/deployments/ci-capability-baseline.md` | Authority | 当前 GitHub Actions CI 覆盖与缺口基线 | Consumed | `F-TASK-004` 的 CI 盘点、门禁缺口与后续任务范围权威落点 |
| `docs/deployments/phase-gate-baseline.md` | Authority | 当前 R-116/R-117/R-118 阶段门禁脚本与 workflow 基线 | Consumed | `F-TASK-005` 的 phase gate 接线、阻断边界与后续缺口权威落点 |
| `docs/deployments/sonar-quality-gate-provisioning.md` | Authority | SonarQube secrets、GitHub Actions environment 与 release gate 接线 runbook | Consumed | `F-TASK-030` 的 Sonar provisioning、release environment 接线与外部配置边界权威落点 |
| `docs/generated/repo-map.md` | Indexed | 仓库结构导航快照 | Consumed | AI 导航、仓库结构入口与目录真值辅助说明 |
| `docs/frontend/design-system.md` | Authority | 前端视觉与页面设计规则 | Consumed | Dashboard、业务页、主题系统 |
| `docs/frontend/form-component-governance.md` | Authority | 页面表单组件语义治理基线 | Consumed | `HARN-045` 的日期时间、租户、数据源、枚举、布尔、数值与敏感输入组件映射权威落点 |
| `docs/product/sql-governance-platform-implementation-spec.md` | Authority | SQL 治理中后台 + 开放接入平台产品实施规格 | Consumed | `HARN-042` 与后续 D/E/F Story 的产品目标、页面与流程权威落点 |
| `docs/product/acceleration-rewrite-governance-workbench-spec.md` | Authority | 加速与改写治理工作台、SQL diff、改写记录、周期比对告警与任务拆分方案 | Consumed | `HARN-127`、`HARN-143`、`HARN-144` 与后续 `HARN-128` 至 `HARN-142` 的方案、页面、接口、数据模型与任务边界权威落点 |
| `docs/product/frontend-retrospective-gap-closure-baseline.md` | Authority | 前端复盘、规格补漏与 repo-closed 边界基线 | Consumed | `U-TASK-004` 的前端差距基线、导航/首页补漏边界与剩余待后端承接项权威落点 |
| `docs/report-import-parse-stress-sample.md` | Indexed | 报表导入解析 XLSX 压测样例说明 | Referenced | 记录本次报表导入测试样例的模板、规模、低质量 SQL 场景、自测边界与使用参数 |
| `docs/report-import-parse-stress-sample.xlsx` | Archive | 报表导入解析 XLSX 压测样例文件 | Archived | 供后续手动导入测试使用；按 `report_code` + `sql_1..sql_120` 宽表模板保留 |
| `docs/agent-prompts/auto-planner.md` | Indexed | requirement-driven auto planner prompt 模板 | Referenced | 约束 full-auto 模式下的 exec plan 与 manifest 自动生成，不允许绕过 Main Foreman 治理链 |
| `docs/agent-prompts/auto-foreman.md` | Indexed | autonomous Main Foreman prompt 模板 | Referenced | 约束 full-auto 模式下 collect 之后的 fan-in、验证与 closeout 收口行为 |
| `docs/agent-prompts/requirement-normalizer.md` | Indexed | requirement normalization prompt 模板 | Referenced | 约束从无 task 开始时的需求标准化输出，不允许把推断写成正式治理事实 |
| `docs/agent-prompts/plan-shaper.md` | Indexed | candidate execution plan shaper prompt 模板 | Referenced | 约束 candidate execution plan 必须映射到已存在的 master plan story，而不是发明新治理真值 |
| `docs/agent-prompts/task-shaper.md` | Indexed | candidate task pack shaper prompt 模板 | Referenced | 约束 candidate task pack 覆盖 task-spec 和 governance-extension 字段，但不直接落 ledger |
| `docs/agent-prompts/task-governance-reviewer.md` | Indexed | candidate task pack governance reviewer prompt 模板 | Referenced | 约束 formal materialization 之前的 go/no-go 审查，不允许绕过 human-confirmation 或审计链 |
| `docs/agent-prompts/main-foreman.md` | Indexed | Main Foreman 多 agent 收口 prompt 模板 | Referenced | 统一收口、fan-in、验证与 closeout 的角色模板 |
| `docs/agent-prompts/truth-explorer.md` | Indexed | truth explorer prompt 模板 | Referenced | 只读事实盘点、真值核对与证据提炼模板 |
| `docs/agent-prompts/boundary-explorer.md` | Indexed | boundary explorer prompt 模板 | Referenced | 路径边界、ownership 和冲突扫描模板 |
| `docs/agent-prompts/validation-explorer.md` | Indexed | validation explorer prompt 模板 | Referenced | 验证链、证据面和残余风险盘点模板 |
| `docs/agent-prompts/query-worker.md` | Indexed | query worker prompt 模板 | Referenced | query 相关实现 worker 的受控模板 |
| `docs/agent-prompts/optimization-worker.md` | Indexed | optimization worker prompt 模板 | Referenced | optimization 相关实现 worker 的受控模板 |
| `docs/agent-prompts/benchmark-worker.md` | Indexed | benchmark worker prompt 模板 | Referenced | benchmark 相关实现 worker 的受控模板 |
| `docs/agent-prompts/governance-worker.md` | Indexed | governance worker prompt 模板 | Referenced | governance/tooling 实现 worker 的受控模板 |
| `docs/agent-prompts/frontend-worker.md` | Indexed | frontend worker prompt 模板 | Referenced | frontend 实现 worker 的受控模板 |
| `docs/agent-prompts/ops-worker.md` | Indexed | ops worker prompt 模板 | Referenced | scripts/deployments/CI worker 的受控模板 |
| `docs/agent-prompts/validator.md` | Indexed | validator prompt 模板 | Referenced | 默认只验证不改实现的 validator 模板 |
| `docs/operations/README.md` | Indexed | 运维与协作索引 | Consumed | operations 文档入口 |
| `docs/operations/codex-mcp-playbook.md` | Authority | Codex MCP 只读边界、本地接入、manifest-level `mcp_profile` 约束和证据写回手册 | Consumed | `HARN-034` / `HARN-035` 的 MCP 手册、自动化入口、单 agent 边界与 `explorer/validator` 只读证据 contract 权威落点 |
| `docs/operations/best-practices.md` | Authority | 可泛化工程规则账本 | Referenced | 复用规则、Root Cause/Cure/Generalization 沉淀 |
| `docs/operations/foreman-workflow.md` | Authority | foreman 任务流、开发循环与上下文收缩/清理 | Consumed | 任务入口、开发循环、closeout 与上下文切换闭环 |
| `docs/operations/git-and-task-closeout.md` | Authority | Git 边界、上下文收缩/清理与任务关闭顺序 | Consumed | 单任务关闭、审计链、commit 规则与 closeout 顺序 |
| `docs/operations/human-collaboration.md` | Authority | 人机协作边界、命令可用性与脏工作树处理 | Consumed | stop/continue、冲突与 `/contract` / `/clear` 可用性约束 |
| `docs/operations/local-development.md` | Authority | 本地命令、脚本与环境入口 | Referenced | 本地开发验证与环境约束 |
| `docs/operations/multi-agent-playbook.md` | Authority | 多 agent 协作手册、`mcp_profiles` / `mcp_profile` 合同与 SQLForge demo runbook | Consumed | Main Foreman、worktree、manifest、prompt 模板，以及 autoplan/full-auto/prepare/launch/collect 与最终 validate/closeout 的权威操作落点；同时约束 `explorer / validator` 的只读 MCP 证据边界 |
| `docs/operations/requirements-to-task-playbook.md` | Authority | 从无 task 开始的治理自动化手册 | Consumed | 定义 requirement normalization、candidate task pack、materialization gate 与 governed full-cycle 的权威操作落点 |
| `docs/operations/sql-governance-degradation-matrix.md` | Authority | SQL 治理产品线在数据库、报表接口、Redis、装数协同、邮件通道与开放接入不可用时的降级矩阵 | Consumed | `HARN-042` 与后续 D/E/F Story 的降级与 environment-backed 边界权威落点 |
| `docs/plans/README.md` | Indexed | 计划导航 | Consumed | 计划入口与附录说明 |
| `docs/plans/master-execution-plan.md` | Authority | 当前主执行计划 | Consumed | 主控文档 |
| `docs/plans/phase-0-plan.md` | Indexed | 阶段0历史计划 | Consumed | 阶段0真值修正 |
| `docs/plans/document-coverage-matrix.md` | Indexed | 文档全量覆盖矩阵 | Consumed | 证明 `docs/` 全量纳入 |
| `docs/plans/document-gap-matrix.md` | Indexed | 冲突、漂移、缺失与残余实现缺口矩阵 | Consumed | 严格核验闭口检查 |
| `docs/plans/document-truth-baseline.md` | Authority | 当前仓库真值、历史记录、目标边界分层与漂移映射 | Consumed | 编码前真值判断、漂移治理、文档消费入口 |
| `docs/plans/codex-governance-integration-blueprint.md` | Authority | Codex 运行时接线蓝图与执行前/中/后治理契约 | Consumed | `AGENTS`、project-scoped `.codex`、hooks、foreman CLI 与真值体系的接线权威 |
| `docs/plans/implementation-readiness.md` | Authority | 编码前置消费顺序、主题权威来源、执行波次 | Consumed | 实施顺序、冲突处理、任务进入条件 |
| `docs/plans/phase-prerequisite-matrix.md` | Authority | 阶段输入文档、ADR、规则、验证和确认点矩阵 | Consumed | 阶段进入前置条件检查 |
| `docs/plans/production-rewrite-auto-apply-task-plan.md` | Indexed | 生产自动改写闭环任务拆解计划 | Consumed | 将改写审批、发布、运行时自动改写、历史留痕、差异暂停和页面追溯拆成可执行任务；暂不覆盖投产前核验闭环 |
| `docs/plans/process-flow-and-governance-audit-2026-04-20.md` | Indexed | 正式全流程说明、流程缺陷审计与整改建议 | Consumed | 供后续接手人与治理批次快速理解当前执行流程与缺陷闭口优先级 |
| `docs/plans/retrospective-template.md` | Indexed | 阶段与复杂批次复盘模板 | Referenced | 复盘闭环与后续治理沉淀 |
| `docs/plans/document-governance-retrospective-2026-04-20.md` | Indexed | 本轮文档治理复盘记录 | Consumed | 漂移、缺口和后续治理沉淀 |
| `docs/plans/document-governance-repair-retrospective-2026-04-20.md` | Indexed | 本轮严格核验缺口修复复盘记录 | Consumed | 治理闭口与 repair 批次追溯 |
| `docs/plans/task-spec-matrix.md` | Indexed | Harness Task 字段矩阵 | Consumed | 补齐 Task 的 10 字段 |
| `docs/plans/task-governance-extension-matrix.md` | Indexed | Task 的确认点、数据影响、回滚扩展矩阵 | Consumed | 严格治理扩展字段追踪 |
| `docs/quality/alibaba-java-guidelines.md` | Authority | Java 规范适配文档 | Consumed | Java 实现与扫描治理 |
| `docs/quality/frontend-backend-separation-baseline.md` | Authority | 前后端分离基线 | Consumed | 边界治理与脚本校验 |
| `docs/quality/validation-log.md` | Indexed | 验证行为审计日志 | Referenced | 验证证据追溯与关闭链路 |
| `docs/quality/validation-rules.md` | Authority | `R-116` 至 `R-186` 验证规则与 `R-168` 执行衔接 | Consumed | 任务、阶段、上下文收尾与前端视觉自检验证矩阵 |
| `docs/references/human-constraint-history.md` | Authority | 长期约束历史账本 | Consumed | 规则追加与人类决策追溯 |
| `docs/references/raw-requirements/alibaba-java-guidelines/Java开发手册(黄山版).pdf` | Archive | Java 规范原始 PDF | Archived | `R-154` 来源追溯 |
| `docs/references/raw-requirements/alibaba-java-guidelines/README.snapshot.md` | Archive | Java 规范原始 README 快照 | Archived | `R-154` 来源追溯 |
| `docs/references/raw-requirements/alibaba-java-guidelines/license.txt` | Archive | 原始资料许可证 | Archived | 归档保留，不作为执行基线 |
| `docs/references/raw-requirements/alibaba-java-guidelines/source-metadata.md` | Archive | 版本、来源、校验元数据 | Archived | `R-154` 来源追溯 |
| `docs/rules/codex-rules.md` | Authority | 仓库规则库 | Consumed | 所有实现与验证门禁 |
| `docs/rules/karpathy-guidelines.md` | Authority | Karpathy 风格 AI 编码行为完整说明 | Consumed | AI 编码行为说明、原则与 SQLForge 落点 |
| `docs/security/access-control-spec.md` | Authority | 访问控制专项规格 | Consumed | 身份、角色、资源、审计实现基线 |
| `docs/security/connectors.md` | Authority | Connector 与 MCP 的只读 category、安全边界、`mcp_profile` 角色约束和校验清单 | Consumed | `HARN-034` / `HARN-035` 的 MCP 基线、禁用能力、`explorer / validator` 角色边界和 connector intake / validation 权威落点 |
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
| `docs/exec-plans/completed/HARN-027-requirements-to-task-governed-full-cycle-plan.md` | Indexed | HARN-027 从无 task 开始的治理自动化执行计划 | Consumed | 约束 HARN-027 的 task-shaping/materialization/full-cycle 交付边界、验证顺序与 closeout 前 write scope |
| `docs/exec-plans/completed/HARN-026-full-auto-multi-agent-upgrade-plan.md` | Indexed | HARN-026 全自动多 agent 升级执行计划 | Consumed | 约束 HARN-026 的 full-auto 交付边界、验证顺序与 closeout 前 write scope |
| `docs/exec-plans/completed/HARN-025-semi-auto-multi-agent-foundation-plan.md` | Indexed | HARN-025 半自动多 agent 基础设施执行计划 | Referenced | 约束 HARN-025 的交付边界、验证顺序与 closeout 前 write scope |
| `docs/exec-plans/completed/HARN-028-governed-full-cycle-v2-hardening-plan.md` | Indexed | HARN-028 governed full-cycle V2 加固执行计划 | Consumed | 约束 HARN-028 的 intake/healthcheck、run summary、materialization rollback 与 closeout residue 修复边界、验证顺序和 closeout 前 write scope |
| `docs/exec-plans/completed/.gitkeep` | Indexed | 完成执行计划目录占位文件 | Referenced | 保持完成执行计划目录可追踪 |
| `docs/exec-plans/completed/HARN-009-closeout-boundary-repair-plan.md` | Indexed | 当前 closeout 边界与完成台账结构修复批次执行计划 | Consumed | 收口 HARN-008 复盘中发现的归档边界和 done-ledger 结构校验缺口 |
| `docs/exec-plans/completed/HARN-007-codex-runtime-integration-plan.md` | Indexed | 已完成的 Codex 运行时集成治理批次执行计划 | Referenced | 追溯 HARN-007 的原始批次目标、交付件与验证顺序 |
| `docs/exec-plans/completed/HARN-008-governance-runtime-hardening-plan.md` | Indexed | 已完成的 Codex 治理/runtime 闭口批次执行计划 | Referenced | 追溯 HARN-008 对 6 个治理闭口点的实现、验证与 closeout 顺序 |
| `docs/exec-plans/templates/multi-agent-run.template.json` | Indexed | 多 agent run manifest 模板 | Referenced | 作为任务级 active manifest 的起始模板与字段语义基线，供 Main Foreman 复制到 `docs/exec-plans/active/` 或由 auto-planner 生成 `full-auto` manifest |
| `docs/exec-plans/templates/candidate-task-pack.template.json` | Indexed | candidate task pack 模板 | Referenced | 作为从无 task 开始的 formal materialization 输入模板，约束 task-spec 与 governance-extension 必填字段 |
| `docs/exec-plans/completed/HARN-034-full-auto-execution-plan.md` | Indexed | HARN-034 formalized full-auto execution plan | Consumed | 约束 HARN-034 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope |
| `docs/references/raw-requirements/generated/HARN-034-requirement.md` | Archive | HARN-034 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |
| `docs/exec-plans/completed/HARN-035-full-auto-execution-plan.md` | Indexed | HARN-035 formalized full-auto execution plan | Consumed | 约束 HARN-035 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope |
| `docs/references/raw-requirements/generated/HARN-035-requirement.md` | Archive | HARN-035 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |
| `docs/exec-plans/completed/HARN-036-full-auto-execution-plan.md` | Indexed | HARN-036 formalized full-auto execution plan | Consumed | 约束 HARN-036 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope |
| `docs/references/raw-requirements/generated/HARN-036-requirement.md` | Archive | HARN-036 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |
| `docs/exec-plans/completed/HARN-037-full-auto-execution-plan.md` | Indexed | HARN-037 formalized full-auto execution plan | Consumed | 约束 HARN-037 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope |
| `docs/references/raw-requirements/generated/HARN-037-requirement.md` | Archive | HARN-037 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |
| `docs/exec-plans/completed/HARN-038-full-auto-execution-plan.md` | Indexed | HARN-038 formalized full-auto execution plan | Consumed | 约束 HARN-038 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope |
| `docs/references/raw-requirements/generated/HARN-038-requirement.md` | Archive | HARN-038 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/D-TASK-030-full-auto-execution-plan.md` | Indexed | D-TASK-030 formalized full-auto execution plan | Consumed | 约束 D-TASK-030 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/D-TASK-030-requirement.md` | Archive | D-TASK-030 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-041-full-auto-execution-plan.md` | Indexed | HARN-041 formalized full-auto execution plan | Consumed | 约束 HARN-041 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-041-requirement.md` | Archive | HARN-041 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/U-TASK-004-full-auto-execution-plan.md` | Indexed | U-TASK-004 formalized full-auto execution plan | Consumed | 约束 U-TASK-004 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/U-TASK-004-requirement.md` | Archive | U-TASK-004 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-045-full-auto-execution-plan.md` | Indexed | HARN-045 formalized full-auto execution plan | Consumed | 约束 HARN-045 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-045-requirement.md` | Archive | HARN-045 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/D-TASK-073-full-auto-execution-plan.md` | Indexed | D-TASK-073 formalized full-auto execution plan | Consumed | 约束 D-TASK-073 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/D-TASK-073-requirement.md` | Archive | D-TASK-073 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/D-TASK-074-full-auto-execution-plan.md` | Indexed | D-TASK-074 formalized full-auto execution plan | Consumed | 约束 D-TASK-074 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/D-TASK-074-requirement.md` | Archive | D-TASK-074 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/D-TASK-075-full-auto-execution-plan.md` | Indexed | D-TASK-075 formalized full-auto execution plan | Consumed | 约束 D-TASK-075 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/D-TASK-075-requirement.md` | Archive | D-TASK-075 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-049-full-auto-execution-plan.md` | Indexed | HARN-049 formalized full-auto execution plan | Consumed | 约束 HARN-049 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-049-requirement.md` | Archive | HARN-049 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |

| `docs/exec-plans/completed/HARN-056-parse-history-and-batch-center-plan.md` | Indexed | HARN-056 SQL 解析历史与批量解析中心重构执行计划 | Consumed | 约束 HARN-056 的 parse history 持久化、批量解析中心重构、自动文件类型识别、统计与历史展示边界 |
| `docs/references/raw-requirements/generated/HARN-056-requirement.md` | Archive | HARN-056 raw requirement snapshot | Archived | 保存 HARN-056 原始需求输入，供 task-shaping 与后续审计追溯 |
| `docs/exec-plans/completed/HARN-057-parse-history-refresh-plan.md` | Indexed | HARN-057 解析历史默认查询与刷新修复执行计划 | Consumed | 约束 HARN-057 的解析历史默认空筛选、请求上下文租户分离、批量历史刷新与验证边界 |
| `docs/references/raw-requirements/generated/HARN-057-requirement.md` | Archive | HARN-057 raw requirement snapshot | Archived | 保存 HARN-057 用户复核优化输入，供解析历史刷新与默认查询条件修复追溯 |
| `docs/exec-plans/completed/HARN-058-parse-history-traceability-plan.md` | Indexed | HARN-058 解析历史投影与报表导入明细修复执行计划 | Consumed | 约束 HARN-058 的 query_history 归一化投影、access 解析合并、报表导入 SQL 级与报表级详情展示及验证边界 |
| `docs/references/raw-requirements/generated/HARN-058-requirement.md` | Archive | HARN-058 raw requirement snapshot | Archived | 保存 HARN-058 用户追问输入，供解析历史可见性和报表导入明细修复追溯 || `docs/exec-plans/completed/HARN-062-full-auto-execution-plan.md` | Indexed | HARN-062 formalized full-auto execution plan | Consumed | 约束 HARN-062 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-062-requirement.md` | Archive | HARN-062 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-063-full-auto-execution-plan.md` | Indexed | HARN-063 formalized full-auto execution plan | Consumed | 约束 HARN-063 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-063-requirement.md` | Archive | HARN-063 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-064-full-auto-execution-plan.md` | Indexed | HARN-064 formalized full-auto execution plan | Consumed | 约束 HARN-064 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-064-requirement.md` | Archive | HARN-064 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-066-full-auto-execution-plan.md` | Indexed | HARN-066 formalized full-auto execution plan | Consumed | 约束 HARN-066 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-066-requirement.md` | Archive | HARN-066 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-070-full-auto-execution-plan.md` | Indexed | HARN-070 formalized full-auto execution plan | Consumed | 约束 HARN-070 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-070-requirement.md` | Archive | HARN-070 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/HARN-071-full-auto-execution-plan.md` | Indexed | HARN-071 formalized full-auto execution plan | Consumed | 约束 HARN-071 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/HARN-071-requirement.md` | Archive | HARN-071 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |
| `docs/exec-plans/completed/HARN-084-sql-parse-history-decoupling-plan.md` | Indexed | HARN-084 SQL 解析记录与 SQL 执行历史解耦执行计划 | Consumed | 约束 HARN-084 的 `sql_parse_history` 持久化、parse-history API、前端 API 拆分、日终慢 SQL 解析骨架与验证顺序 |
| `docs/references/raw-requirements/HARN-084-sql-parse-history-decoupling.md` | Archive | HARN-084 raw requirement snapshot | Archived | 保存 HARN-084 用户确认计划输入，供解析历史解耦实现和后续审计追溯 |
| `docs/references/raw-requirements/HARN-127-acceleration-rewrite-governance.md` | Archive | HARN-127 raw requirement snapshot | Archived | 保存本轮加速与改写治理工作台方案、SQL diff、改写记录和任务拆分原始需求，供后续 `HARN-128` 至 `HARN-142` 追溯 |
| `docs/references/raw-requirements/HARN-143-acceleration-rewrite-governance-review.md` | Archive | HARN-143 raw requirement snapshot | Archived | 保存对 HARN-127 方案、输入输出、历史、文档、任务依赖和缺失点的严格复核要求，供 `HARN-128` 之后实施时追溯复核修正基线 |
| `docs/references/raw-requirements/HARN-144-acceleration-rewrite-governance-doc-consistency.md` | Archive | HARN-144 raw requirement snapshot | Archived | 保存对 HARN-143 后方案、输入输出、历史、文档、任务依赖和一致性缺口的复核要求，供 `HARN-128` 之后实施时追溯最新修正基线 || `docs/exec-plans/completed/PRW-001-full-auto-execution-plan.md` | Indexed | PRW-001 formalized full-auto execution plan | Consumed | 约束 PRW-001 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-001-requirement.md` | Archive | PRW-001 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-002-full-auto-execution-plan.md` | Indexed | PRW-002 formalized full-auto execution plan | Consumed | 约束 PRW-002 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-002-requirement.md` | Archive | PRW-002 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-003-full-auto-execution-plan.md` | Indexed | PRW-003 formalized full-auto execution plan | Consumed | 约束 PRW-003 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-003-requirement.md` | Archive | PRW-003 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-004-full-auto-execution-plan.md` | Indexed | PRW-004 formalized full-auto execution plan | Consumed | 约束 PRW-004 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-004-requirement.md` | Archive | PRW-004 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-005-full-auto-execution-plan.md` | Indexed | PRW-005 formalized full-auto execution plan | Consumed | 约束 PRW-005 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-005-requirement.md` | Archive | PRW-005 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-006-full-auto-execution-plan.md` | Indexed | PRW-006 formalized full-auto execution plan | Consumed | 约束 PRW-006 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-006-requirement.md` | Archive | PRW-006 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-007-full-auto-execution-plan.md` | Indexed | PRW-007 formalized full-auto execution plan | Consumed | 约束 PRW-007 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-007-requirement.md` | Archive | PRW-007 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-008-full-auto-execution-plan.md` | Indexed | PRW-008 formalized full-auto execution plan | Consumed | 约束 PRW-008 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-008-requirement.md` | Archive | PRW-008 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-009-full-auto-execution-plan.md` | Indexed | PRW-009 formalized full-auto execution plan | Consumed | 约束 PRW-009 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-009-requirement.md` | Archive | PRW-009 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-010-full-auto-execution-plan.md` | Indexed | PRW-010 formalized full-auto execution plan | Consumed | 约束 PRW-010 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-010-requirement.md` | Archive | PRW-010 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-011-full-auto-execution-plan.md` | Indexed | PRW-011 formalized full-auto execution plan | Consumed | 约束 PRW-011 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-011-requirement.md` | Archive | PRW-011 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-012-full-auto-execution-plan.md` | Indexed | PRW-012 formalized full-auto execution plan | Consumed | 约束 PRW-012 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-012-requirement.md` | Archive | PRW-012 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 || `docs/exec-plans/completed/PRW-013-full-auto-execution-plan.md` | Indexed | PRW-013 formalized full-auto execution plan | Consumed | 约束 PRW-013 在 formal materialization 之后的 downstream full-auto 执行边界、验证顺序与 closeout 前 write scope || `docs/references/raw-requirements/generated/PRW-013-requirement.md` | Archive | PRW-013 raw requirement snapshot | Archived | 保存 formal materialization 对应的原始需求输入，供 task-shaping 与后续审计追溯 |

## Completeness Statement

- 当前 `docs/` 目录下所有文件均已进入本矩阵。
- 主执行计划已显式引用当前执行所需的核心 `Authority` 文档与关键 `Indexed` 文档；其余条目通过本矩阵、ADR 索引、计划索引或目录索引追踪。
- `Archive` 文档已按 `R-062`、`R-154`、`R-155` 归档，不被遗漏，也不被误写成权威架构事实。

## Update Note 2026-04-20

- 新增治理文档已正式并入主矩阵，而不是只保留在增量附录中。
- 以上新增文档不替代历史初始化文本。
- `Authority` 类型文档已被 `docs/README.md` 与 `master-execution-plan.md` 主体部分显式引用。
