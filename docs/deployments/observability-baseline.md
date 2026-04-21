# SQLForge Observability Baseline

## Purpose

本文件作为 `F-TASK-007` 的当前权威落点，用于把 SQLForge 已落地的 `logs / metrics / alerts` 能力收口为一份可执行的运维清单。

本文只写两类内容：

- 已被仓库代码、配置或测试证明的当前实现基线
- 基于当前实现必须补到生产运维侧的采集、看板和告警动作

凡仓库中尚未实现的能力，必须显式标记为缺口，不得写成既成事实。

## Scope

- 适用模块：
  - `governance`
  - `query-execution`
  - `sql-optimization`
  - `benchmark-engine`
- 关联规则：
  - `R-039`
  - `R-060`
  - `R-064`
  - `R-083`
  - `R-113`
  - `R-114`
  - `R-115`
- 关联 ADR：
  - `ADR-007`
  - `ADR-009`
  - `ADR-012`

## Implemented Baseline

### Logging

| Area | Current implementation | Evidence |
|:---|:---|:---|
| Shared file/console logging | 4 个后端服务均存在 `logback-spring.xml`，同时输出控制台日志和滚动文件日志 | `governance/src/main/resources/logback-spring.xml`, `query-execution/src/main/resources/logback-spring.xml`, `sql-optimization/src/main/resources/logback-spring.xml`, `benchmark-engine/src/main/resources/logback-spring.xml` |
| Rolling policy | 本地文件日志统一落到 `logs/${appName}`，单文件 `20MB`，保留 `30` 天，总量上限 `1GB` | 各服务 `logback-spring.xml` |
| Production console shape | `prod` profile 下统一输出 JSON console 日志，字段至少包含 `timestamp/level/service/thread/logger/message` | 各服务 `logback-spring.xml` |
| Sensitive masking | 日志消息统一对 `password|token|secret` 进行掩码替换，避免明文落日志 | 各服务 `logback-spring.xml` |
| Query execution flow logs | 查询执行路径已覆盖 `START / STATE_CHANGE / END / FAILED`，并记录 `operation/entity/tenantId/requestedDatasource/faultTolerance/costMs/resultStatus` | `query-execution/src/main/java/com/company/queryexecution/application/service/QueryExecutionApplicationService.java` |
| Governance auth/audit logs | 治理服务已记录鉴权上下文建立、审计落库成功、审计消息降级入队、数据库队列轮询完成、失败消息重试等关键日志 | `governance/src/main/java/com/company/governance/application/interceptor/AuthInterceptor.java`, `governance/src/main/java/com/company/governance/application/service/GovernanceAuditTrailService.java`, `governance/src/main/java/com/company/governance/infrastructure/messaging/DatabaseMessagePollingJob.java`, `governance/src/main/java/com/company/governance/application/service/MessageAdminApplicationService.java` |
| Async task flow logs | SQL 优化与压测引擎均已记录任务提交、查询、占位执行器状态变更、结束和异常失败日志 | `sql-optimization/src/main/java/com/company/sqloptimization/application/service/OptimizationTaskApplicationService.java`, `sql-optimization/src/main/java/com/company/sqloptimization/application/service/OptimizationTaskPlaceholderExecutor.java`, `benchmark-engine/src/main/java/com/company/benchmarkengine/application/service/BenchmarkTaskApplicationService.java`, `benchmark-engine/src/main/java/com/company/benchmarkengine/application/service/BenchmarkTaskPlaceholderExecutor.java`, `benchmark-engine/src/main/java/com/company/benchmarkengine/application/service/BenchmarkReportApplicationService.java` |
| AOP operation logs | 部分治理接口已通过共享 `OperationLogAspect` 补齐 `operation/entity/requestId/traceId/status/costMs` 结构化日志 | `sqlforge-shared/src/main/java/com/company/sqlforge/common/log/OperationLogAspect.java` |

### Metrics And Health

| Area | Current implementation | Evidence |
|:---|:---|:---|
| Actuator dependency | 4 个后端模块均已接入 `spring-boot-starter-actuator` | `governance/pom.xml`, `query-execution/pom.xml`, `sql-optimization/pom.xml`, `benchmark-engine/pom.xml` |
| Exposed endpoints | 4 个后端服务统一暴露 `health`, `info`, `metrics`, `prometheus` | 各服务 `src/main/resources/application.yml` |
| Health detail policy | 4 个后端服务统一配置 `management.endpoint.health.show-details=always` | 各服务 `src/main/resources/application.yml` |
| Public health path | `governance` 额外提供 `/api/governance/health` 公开健康接口，且鉴权拦截器默认放行该路径 | `governance/src/main/java/com/company/governance/application/controller/HealthController.java`, `governance/src/main/java/com/company/governance/application/service/HealthStatusApplicationService.java`, `governance/src/main/java/com/company/governance/config/WebMvcConfig.java` |
| Custom business metrics | 当前仓库中未发现 `MeterRegistry`、`Counter`、`Gauge`、`Timer` 或 `@Timed` 等自定义业务指标实现 | 仓库检索结果；属于基于代码扫描的当前事实判断 |

结论：

- 当前 metrics 基线仅覆盖 Spring Boot Actuator / Micrometer 默认指标和 Prometheus 导出。
- 业务级 backlog、鉴权失败、审计降级、异步失败等信号，目前仍主要依赖日志、管理接口或数据库查询，而非内建业务指标。

## Logs / Metrics / Alerts Delivery Checklist

### Logs

生产运维必须落实以下动作：

1. 统一采集 4 个服务的标准输出日志；`prod` profile 下默认消费 JSON console。
2. 保留滚动文件日志目录 `logs/${appName}`，用于本机排障和短期留存，不得关闭脱敏规则。
3. 日志平台至少按以下字段建索引：
   - `service`
   - `level`
   - `logger`
   - `message`
   - `traceId` 或等价链路字段
   - `requestId` 或等价请求字段
   - `tenantId` 或等价租户字段
   - `operation`
   - `entity`
4. 针对 `R-114`，额外执行敏感信息抽检，确认日志平台中不存在密码、Token、密钥明文。
5. 对 `audit_log` 的数据库留存按 `ADR-009` 与 `R-113` 单独执行保留策略；本地文件滚动历史不替代审计留存。

### Metrics

生产运维必须落实以下动作：

1. 对 4 个服务统一抓取 `/actuator/prometheus`。
2. 至少建立以下基础看板：
   - 服务可用性与重启次数
   - JVM 内存、GC、线程与 CPU
   - HTTP 请求量、错误率、延迟分布
   - 每实例 scrape 成功率
3. 将 `governance` 的 `/api/governance/health` 作为对外探活补充探针；内部实例仍以 `/actuator/health` 为准。
4. 在外部监控系统中补齐当前仓库尚未内建的业务观测项：
   - 审计消息兜底入队次数
   - 数据库消息队列 `pending/failed` 数量
   - 查询执行 timeout / degraded 事件次数
   - SQL 优化任务失败次数
   - 压测任务失败次数

### Alerts

以下清单是当前阶段必须接入的最小告警集。若告警平台暂不支持直接消费日志，应通过 Prometheus exporter、数据库轮询或管理接口采样补位。

| Alert domain | Current signal source | Baseline alert condition | Operator action |
|:---|:---|:---|:---|
| Service down | `/actuator/health` 或 `governance` `/api/governance/health` | 任一实例非 `UP` 或健康探针连续失败 | 先确认实例、依赖和网络，再进入日志排障 |
| Prometheus scrape failure | `/actuator/prometheus` | 任一服务 scrape 中断 | 检查应用暴露、鉴权、网关和抓取配置 |
| Query execution degraded / timeout | `query-execution` 结构化状态流日志 | 出现持续的 `STATE_PRIMARY_TIMEOUT`、`degraded=true` 或 `status=FAILED phase=EXCEPTION` | 排查目标引擎、fallback、超时阈值和只读保护路径 |
| Governance audit route fallback | `governance` warn 日志 `Primary audit message delivery failed, queued fallback message` | 任意生产出现即触发高优先级告警 | 优先检查消息主路由、Kafka/Database 模式、队列堆积和补偿路径 |
| Database queue backlog | `MessageAdminApplicationService#getMessageStats()` 或 `kafka_message_queue` | `pendingCount` 持续增长或 `failedCount > 0` | 执行消息重试、检查消费者和下游可用性 |
| Authentication rejection spike | `audit_log` 中 `LOGIN` 失败事件或鉴权拒绝日志 | 失败事件异常上升 | 判断为攻击、配置错误或上游鉴权异常 |
| SQL optimization async failure | `sql-optimization` 任务/执行器日志 | `status=FAILED phase=EXCEPTION` 或任务失败持续出现 | 排查占位执行器、回调地址、租户上下文和任务载体 |
| Benchmark async failure | `benchmark-engine` 任务/执行器日志 | `status=FAILED phase=EXCEPTION` 或任务失败持续出现 | 排查影子环境要求、只读约束、执行器状态和报告链 |
| Sensitive data leak | 日志平台全文扫描 | 任意命中明文密码、Token、密钥 | 立即下线相关日志访问、轮换凭据并修复脱敏规则 |

## Current Gaps

以下能力仍未在仓库内建完成，必须明确视为后续任务，而不是本任务已落地项：

1. 未实现业务级 Micrometer 指标，当前没有内建 `Counter/Gauge/Timer`。
2. 未提供仓库内的 PrometheusRule / Alertmanager / Grafana dashboard 配置文件。
3. 未提供 ELK / Loki / OpenSearch 的日志采集清单或 pipeline 模板。
4. 未接入 SkyWalking、OpenTelemetry 或等价链路追踪埋点。
5. 审计留存、鉴权失败统计、消息堆积等仍依赖外部 SQL/接口采样，而非仓库内建 exporter。

## Exit Criteria For F-TASK-007

`F-TASK-007` 达成完成态时，必须满足：

1. 本文明确区分了当前已实现能力与待补缺口。
2. `logs / metrics / alerts` 三类都有对应的运维落地清单。
3. 关键已实现能力都能回指到仓库中的代码或配置事实。
4. 新文档已经进入 `docs/README.md`、部署入口或覆盖矩阵，不是孤立文件。

## Related Documents

- [Huawei Cloud Setup](/models/project/codex/SQLForge/docs/deployments/huawei-cloud-setup.md)
- [Local Setup](/models/project/codex/SQLForge/docs/deployments/local-setup.md)
- [Compliance](/models/project/codex/SQLForge/docs/security/compliance.md)
- [Access Control Spec](/models/project/codex/SQLForge/docs/security/access-control-spec.md)
- [ADR-009 Data Retention And Destruction](/models/project/codex/SQLForge/docs/adr/ADR-009-data-retention-and-destruction.md)
