# 去除审计取证、追踪查询、告警中心、运行门禁、恢复演练影响分析

关联任务：`USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520`

原始需求归档：`docs/references/raw-requirements/USER-CN-SIMPLIFY-ENGINE-REMOVE-OPS-SURFACES-20260520.md`

日期：2026-05-20

## 结论

把 SQLForge 从“SQL 生命周期治理平台”收缩为“纯执行引擎”在产品方向上是合理的：当前辅助治理页面、追溯查询、告警与恢复演练确实已经占用了大量前端、文档、脚本和验证复杂度，并且不是 SQL 执行主链路的第一入口。

但不能直接全量删除。当前这些能力已经被写入安全、持久化、跨服务契约、SQL 改写 runtime binding、安全审计、恢复基线和 CI/phase gate。若硬删，会破坏以下核心边界：

- `R-111` / `R-112` 身份鉴别、租户隔离和授权决策。
- `R-113` 审计日志不可改删和保留要求。
- `R-115` 备份恢复要求。
- SQL 历史、改写历史、自动改写是否真实发生的后端事实来源。
- `PREFER_ACCELERATED` / runtime rewrite 的 no-fail-open 安全约束。
- 仓库自身 `foreman` / `task_audit` / phase gate 审计链。

推荐方案是 **B：删除产品化辅助治理面，保留最小执行安全内核**。也就是去掉这些“中心 / 页面 / 查询面 / 运维演练产品功能”，但保留执行引擎必须有的请求上下文、租户授权、最小执行历史、traceId/requestId 日志关联、runtime binding 生效校验和工程交付门禁。

## 盘点依据

本轮已执行并审阅：

- `python3 scripts/foreman.py preflight`
- 默认入口：`docs/README.md`、`docs/plans/document-truth-baseline.md`、`docs/architecture/init.md`、`docs/rules/codex-rules.md`、`docs/quality/validation-rules.md`
- 任务台账：`tasks.md`、`tasks-done.md`、`INBOX.md`
- 产品与架构：`docs/product/sql-governance-platform-implementation-spec.md`、`docs/product/sql-rewrite-function-boundary-design.md`、`docs/plans/frontend-core-workflow-refocus-task-pack.md`、`docs/architecture/service-interface-contract-baseline.md`、`docs/architecture/persistence.md`
- 运维与验证：`docs/deployments/observability-baseline.md`、`docs/deployments/backup-recovery-baseline.md`、`docs/deployments/phase-gate-baseline.md`、`docs/operations/sql-governance-degradation-matrix.md`
- 代码与脚本搜索：`rg` 覆盖 `docs`、`src`、`governance`、`query-execution`、`sql-optimization`、`benchmark-engine`、`sqlforge-shared`、`scripts`、`.github`、`sql`

## 当前实现分布

| 能力 | 前端入口 | 后端 / 共享契约 | 数据库 | 脚本 / 验证 | 文档规则 |
|:---|:---|:---|:---|:---|:---|
| 审计取证 | `AuditForensicsView`、`AuditTroubleshootingView`、`RepairEvidenceView`、`GovernanceTrace*` 组件 | `/api/governance/internal/audit/write`、`GovernanceAuditTrailService`、`AuditLogMapper`、`AuditEvent` | `audit_log`、核心追溯链引用键 | `frontend-runtime-smoke.mjs`、manual governance smoke | `R-018`、`R-034`、`R-113`、`persistence.md`、`backup-recovery-baseline.md` |
| 追踪查询 | `/governance/history/*` 路由和 trace lookup 组件 | `GovernanceHistoryController`、`GovernanceHistoryApplicationService`、`Governance*Trace*` DTO | `config_snapshot`、`execution_result`、`query_history`、`export_record`、`governance_history_lookup_index` | history/detail contract、frontend runtime smoke | `service-interface-contract-baseline.md`、`document-truth-baseline.md` |
| 告警中心 | `AlertCenterView`、`getGovernanceAlerts` / `ackGovernanceAlert` | `GovernanceAlertController`、`AlertEmissionApplicationService`、`Governance*Alert*` DTO、rewrite divergence / benchmark regression alert clients | `alert_policy`、`alert_event`、`alert_notification_log`、`alert_status`、`alert_refs` | `check-alert-page-contract.mjs`、`run-rewrite-governance-smoke.sh` | `sql-governance-platform-implementation-spec.md`、`observability-baseline.md` |
| 运行门禁 | `RuntimeGatesView`、辅助治理菜单 | `rewrite-records/{id}/activation-eligibility`、`query-execution/internal/rewrite-bindings/*`、`acceleration-plans/activate|pause` | `sql_rewrite_record.activation_status`、runtime binding evidence | navigation contract、PRW browser smoke、phase gate 中的 runtime smoke 语义 | `sql-rewrite-function-boundary-design.md`、`service-interface-contract-baseline.md` |
| 恢复演练 | `RecoveryDrillView`、恢复演练 i18n | benchmark artifact recovery / cleanup operation surface、backup/recovery baseline | artifact storage evidence、`export_record`、`audit_log` recovery surface | `verify_compliance_baseline.py`、`run-phase-gates.sh` compliance | `R-115`、`backup-recovery-baseline.md`、`security/compliance.md` |

## 合理性与可行性

### 合理部分

- 前端信息架构已经确认核心链路应优先：`SQL 查询分析 -> SQL 历史查询 -> SQL 解析 -> 解析历史 -> 推荐结果 -> 改写记录 -> 改写历史`。现有辅助治理面可以继续后置，甚至删除。
- 告警中心、取证中心、追踪查询、恢复演练页多数面向运维 / 合规 / 排障，不是纯执行引擎必需入口。
- 当前 `docs/plans/frontend-core-workflow-refocus-task-pack.md` 已把这些内容定义为“辅助治理证据”，不是核心主链路。

### 不可直接删除部分

- `traceId` / `requestId` 不是“追踪查询中心”本身，而是受保护请求上下文和错误响应的一部分。删除会影响鉴权拦截器、日志定位、跨服务调用和错误响应。
- `audit_log` 不是单纯取证页面。它承载登录登出、授权变更、跨服务写入、query history / export 链接键和等保审计要求。
- runtime gate 不是只有 `RuntimeGatesView`。改写激活资格、runtime binding `ACTIVE` 校验和 no-fail-open 是自动改写安全边界，不能用前端隐藏代替。
- 工程 phase gate / task audit / foreman 是仓库交付治理，不属于产品“运行门禁”。用户本次需求不应解释为删除这些仓库治理约束。

## 核心影响与解决方案

### 1. 审计取证

问题：

- 全删审计会违反 `R-113`，同时破坏 `query-execution -> governance`、`sql-optimization -> governance`、`benchmark-engine -> governance` 的跨服务写入。
- SQL 历史导出、改写历史和 benchmark 报告下载目前都会补齐审计 / trace 引用键。

解决方案：

- 删除“审计取证产品面”：`AuditForensicsView`、`AuditTroubleshootingView`、取证导出专属入口、trace lookup UI。
- 保留或重命名为“最小执行事件”：只记录 `tenantId/userId/requestId/traceId/serviceCode/operation/status/elapsedMs/errorCode`，供执行定位和安全留痕。
- 若人类坚持完全无审计，需要同步废止 `R-113`，并确认项目不再声明等保三级安全审计目标。

### 2. 追踪查询

问题：

- 当前追踪查询不仅是 UI，还包括 `governance_history_lookup_index`、`GovernanceHistoryController`、`GovernanceHistoryApplicationService` 和多个服务写回的 trace payload。
- SQL 执行历史、改写历史和推荐 trace 会引用这些字段。

解决方案：

- 删除公开追踪查询中心和 lookup/detail 页面。
- 保留 `traceId/requestId` 在请求、日志和错误响应中传递；不提供按 trace 反查的产品接口。
- 将必须展示的执行事实收敛到 SQL 历史详情，不再提供独立 `GovernanceTraceDetail` 时间线。

### 3. 告警中心

问题：

- 告警中心可删，但告警语义已参与改写差异暂停和 benchmark regression linkage。
- `RewriteActivationEligibilityPolicy` 会消费 `alertStatus`，周期比对 divergence 会发 `SQL_REWRITE_RESULT_DIVERGENCE`。

解决方案：

- 删除 `AlertCenterView` 和 `/api/governance/alerts` 查询 / ACK 面。
- 用 `rewrite_validation_run.comparison_status`、`sql_rewrite_record.activation_status`、`pause_reason`、`last_validation_run_id` 直接表达差异和暂停，不再通过告警表绕一层。
- benchmark regression 结果回到 `benchmark_task_report.regression_summary_json`，不再生成治理告警 linkage。

### 4. 运行门禁

问题：

- 如果“运行门禁”指 `RuntimeGatesView` 和运维页，可以删除。
- 如果指 runtime binding / activation eligibility，则不能删除；删除后 `PREFER_ACCELERATED` 或自动改写可能 fail-open，导致未验证 SQL 进入执行链。

解决方案：

- 删除 `RuntimeGatesView` 和相关导航 / contract。
- 保留后端内联安全策略：改写记录只有 runtime binding 返回 `ACTIVE` 后才能生效；暂停必须调用 runtime binding；执行入口只认 `ACTIVE` binding。
- 文案从“运行门禁”改为“激活条件 / 运行时绑定检查”，避免被理解为独立功能中心。

### 5. 恢复演练

问题：

- 恢复演练页可删，但 `R-115`、`backup-recovery-baseline.md`、`verify_compliance_baseline.py` 仍要求恢复目标和恢复验证。
- benchmark artifact recovery / cleanup 当前已经写入 governance trace / audit surface。

解决方案：

- 删除 `RecoveryDrillView` 和恢复演练产品页面。
- 如项目仍保留 MySQL 持久化，保留最小备份恢复文档和 compliance 检查；不再把它做成产品功能。
- 如项目改为无持久化纯执行引擎，需要新 ADR 明确“执行历史、审计、恢复目标不由本项目承担”，并删除 / 取代 `R-115` 相关验证。

## 改造范围

若人类选择继续，必须按以下范围拆任务，不建议一个 commit 全部完成。

### 文档和规则

- 新增 ADR：SQLForge 是否从“生命周期治理平台”转为“纯执行引擎”。
- 追加规则替代旧规则，而不是直接篡改 append-only 规则：至少覆盖 `R-018`、`R-034`、`R-113`、`R-115`、`R-117`、`R-118`。
- 更新 `docs/README.md`、`document-truth-baseline.md`、`service-interface-contract-baseline.md`、`persistence.md`、`sql-governance-interface-extension-baseline.md`、`sql-governance-platform-implementation-spec.md`、`sql-rewrite-function-boundary-design.md`、`frontend-core-workflow-refocus-task-pack.md`、`observability-baseline.md`、`backup-recovery-baseline.md`、`phase-gate-baseline.md`、`sql-governance-degradation-matrix.md`。

### 前端

- 删除或隐藏：`AuditForensicsView.vue`、`AuditTroubleshootingView.vue`、`RepairEvidenceView.vue`、`AlertCenterView.vue`、`RuntimeGatesView.vue`、`RecoveryDrillView.vue`。
- 清理 `ROUTE_PATHS`、legacy redirect、`NAVIGATION_TREE`、router dynamic imports、i18n 文案、Dashboard / SQL 历史 / 推荐页中的跳转。
- 清理 `runtimeGateApi.js` 中 alerts、trace lookup、message retry 等不再暴露的 API wrapper。
- 更新 `check-navigation-shell-contract.mjs`、history / alert / runtime browser smoke。

### 后端与共享契约

- `governance`：拆分“仍保留的最小授权 / 配置 / 执行历史”与“删除的追溯查询 / 告警 / artifact operation / 审计取证”。
- `query-execution`：继续保留执行、只读 guard、runtime binding active check、必要执行历史；若去掉 governance 写入，需要迁移到本服务本地最小历史或无历史模式。
- `sql-optimization`：改写 divergence 不再发治理告警，改为直接写 validation run / rewrite record 状态；acceleration plan trace 写回需要改为本服务本地 evidence 或删除。
- `benchmark-engine`：regression alert linkage、artifact recovery/cleanup governance operation 需要删除或本地化。
- `sqlforge-shared`：清理 `Governance*Alert*`、`Governance*Trace*`、`GovernanceAuditWriteRequest` 等跨服务 DTO 前，需要确认是否还有 Java SDK / JDBC Agent 依赖。

### SQL 与迁移

- 不建议直接 drop 表。推荐先加“停止写入 / 保留只读历史 / 新表替代”的迁移阶段。
- 可能受影响表：`audit_log`、`alert_policy`、`alert_event`、`alert_notification_log`、`governance_history_lookup_index`、`config_snapshot`、`execution_result`、`query_history`、`export_record`、`sql_rewrite_record.alert_status/trace_refs_json`、`benchmark_task_report.alert_linkages_json`。
- 若人类确认允许破坏性数据迁移，需要单独 migration 方案、回滚脚本和历史数据保留策略。

### 脚本与验证

- 可删除或重写产品 smoke：`check-alert-page-contract.mjs`、`frontend-runtime-smoke.mjs` 中取证 / 处置 / 恢复段、`run-rewrite-governance-smoke.sh` 中告警契约、history detail trace 断言。
- 不应删除仓库治理入口：`foreman.py`、`task_audit.py`、`run-phase-gates.sh`、`verify_compliance_baseline.py`，除非新的 ADR 明确项目治理也降级。
- `verify_compliance_baseline.py` 若仍保留，应改成检查“最小执行安全基线”，而不是审计 / 恢复演练产品能力。

## 决策选项

| 选项 | 内容 | 优点 | 风险 | 建议 |
|:---|:---|:---|:---|:---|
| A | 只删除前端辅助治理页面和导航，后端契约保留 | 快、风险低，能立刻简化用户视角 | 代码和文档复杂度仍在 | 可作为第一步，但不满足“彻底” |
| B | 删除产品化辅助治理面和公开追踪 / 告警 API，保留最小执行安全内核 | 达到纯执行引擎观感，同时不破坏安全、历史和 runtime binding | 需要中等规模跨模块改造 | 推荐 |
| C | 全量删除审计、追踪、告警、runtime gate、恢复基线 | 仓库最简 | 破坏等保、改写安全、历史事实和当前规则；需要重大 ADR 与破坏性迁移 | 不推荐，除非确认放弃治理平台定位 |

## 推荐落地顺序

1. 决策 ADR 与规则增量：先确认项目定位和最低安全边界。
2. 前端瘦身：移除辅助治理菜单、页面、legacy redirect、i18n 和专属 contract。
3. 后端接口收口：先移除公开查询面，再逐步替换内部 trace / alert 写入。
4. 数据模型迁移：先停止新增写入和保留只读历史，再按确认结果 drop / archive。
5. 脚本验证重写：把 smoke 从“治理证据链”改为“执行引擎主链路 + activation check + 最小历史”。
6. 文档真值回写：统一 README、产品规格、接口契约、持久化、部署、验证和任务矩阵。

## 需要人类决定

请明确选择：

- `A`：仅删除前端辅助治理入口。
- `B`：删除产品化辅助治理面，保留最小执行安全内核。
- `C`：彻底删除相关后端、数据、规则与验证，并接受等保 / 历史 / 自动改写安全语义降级。

同时需要确认：

1. 是否继续要求等保三级相关 `R-111` 至 `R-115`。
2. 是否保留 SQL 执行历史和真实改写历史。
3. 是否保留 runtime binding `ACTIVE` 后才允许自动改写 / 加速命中的安全约束。
4. 历史 `audit_log` / `alert_event` / trace 数据是保留只读、迁移归档还是允许 drop。
5. benchmark artifact recovery / cleanup 是否也从项目边界移除。
