# Hetu / MRS Test-Environment Deployment Runbook

## Purpose

本文用于指导你在 `Win10 + IDEA` 环境下，把 `governance` 与 `query-execution` 启到测试环境所需状态，并按真实 Hetu / MRS 接入方式完成配置准备。

本文按你新给出的约束重写，只采用以下口径：

- 部署环境：`Windows 10`
- 启动方式：直接从 `IDEA` 启动服务
- 测试环境：本轮不验证 `Kafka`
- MySQL / TDSQL：优先从 `yml` 配置文件读取
- 部署文档输出：以“你需要确认的清单和选项”为主，不把 smoke 脚本执行写成默认前置动作

## Scope

本文只覆盖：

- `governance`
- `query-execution`
- Win10 本地工作副本 / 测试机工作副本
- IDEA Run Configuration
- Hetu `JDBC` / `REST` / `CLIENT` 三种接入方式的 yml 配置口径

本文不覆盖：

- Kafka 测试验证
- Linux systemd / nohup / shell 守护
- CCE / K8s 编排
- 前端部署

## Fixed Constraints For This Round

本轮部署文档按以下固定约束执行：

1. 测试环境默认使用 `test` profile。
2. `governance` 使用 `DATABASE` messaging mode，不要求 Kafka。
3. MySQL / TDSQL 配置建议直接写入 yml。
4. 服务启动建议直接从 IDEA 启动。
5. 部署文档先输出确认清单；是否执行真实留证脚本由你后续确认。

## Why `test` Profile Is Recommended

推荐：

- `governance`: `test`
- `query-execution`: `test`

原因：

- `governance` `test` profile 默认 `messaging.mode=DATABASE`
- 当前测试环境不做 Kafka 验证
- `governance` / `query-execution` `test` profile 都接受 `header`
- 如果后续你决定执行 Hetu 留证脚本，`header` 路径最省改动

## Win10 Prerequisites

- Windows 10
- IntelliJ IDEA
- JDK 8u112
- Maven 3.8+
- MySQL 8.0 或兼容 TDSQL
- Redis 7.x 或兼容实例
- 一套可访问的 Hetu / MRS
- Maven 构建通过的本地工作副本

## Build

在 IDEA Terminal 或 Windows PowerShell 里于仓库根目录执行：

```bash
mvn -B -pl governance,query-execution -am clean package -DskipTests
```

产物：

- `governance/target/governance-0.1.0-SNAPSHOT.jar`
- `query-execution/target/query-execution-0.1.0-SNAPSHOT.jar`

即使你最终从 IDEA 直接启动，也建议先做一次构建，避免运行时才暴露 classpath 问题。

## Database Initialization

### Fresh Database

新测试库执行：

```sql
source sql/init-schema.sql;
source sql/init-data.sql;
```

### Existing Database

已有测试库按顺序补增量脚本：

- `sql/migrations/V20260421_011__core_traceability_chain.sql`
- `sql/migrations/V20260421_013__sensitive_data_encryption_baseline.sql`
- `sql/migrations/V20260422_014__sql_optimization_task_persistence.sql`
- `sql/migrations/V20260422_015__benchmark_engine_task_report_persistence.sql`
- `sql/migrations/V20260422_016__governance_history_lookup_index.sql`
- `sql/migrations/V20260423_017__drop_traceability_foreign_keys.sql`

## YML Configuration Recommendation

你要求 MySQL / TDSQL 在服务启动时从 yml 读取。这里给两种方式：

### Option A: 直接改 `application-test.yml`

适合：

- 这台 Win10 测试机只有你自己使用
- 你接受本地工作副本存在未提交的测试环境配置

优点：

- 最直接
- IDEA 启动最省事

代价：

- 本地工作副本会变脏

### Option B: 新建本地 overlay yml

适合：

- 你不想改仓库默认 `application-test.yml`
- 你希望本地测试配置和仓库真值分开

推荐做法：

- `governance/src/main/resources/application-test-local.yml`
- `query-execution/src/main/resources/application-test-local.yml`

然后在 IDEA 里使用：

- `Active profiles`: `test,test-local`

本文更推荐 `Option B`。

## Governance YML Example

把下面内容写到你选择的测试配置文件里：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://<db-host>:3306/sqlforge_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: <db-user>
    password: <db-password>
  redis:
    host: <redis-host>
    port: 6379

messaging:
  mode: DATABASE
  kafka:
    enabled: false
  database:
    enabled: true
    poll-interval: 5000
    max-retry: 3

sqlforge:
  security:
    crypto:
      algorithm: AES256_GCM
      key-id: governance-test-key
      base64-key: <32-byte-base64-key>

auth:
  enabled: true
  trusted-auth-sources:
    - header
    - gateway
```

说明：

- 本轮测试环境不验证 Kafka，所以 `messaging.mode=DATABASE`
- `base64-key` 需要真实可用
- 如果你最终仍要切 `prod` profile，就不能再照搬这份配置

## Query-Execution YML Example

同样写入测试配置文件。

### Common

```yaml
query-execution:
  hetu:
    enabled: true
    calibration:
      profile: TEST_ENV_CANDIDATE
      route-order:
        - JDBC
        - REST
        - CLIENT
      skip-unready-modes: true
    cluster-evidence:
      evidence-source: ENVIRONMENT_SMOKE_CANDIDATE
      environment-label: win10-test-env
      cluster-name: <hetu-or-mrs-cluster-name>
      coordinator-endpoint: http://<hetu-host>:<port>
      runbook-ref: docs/deployments/hetu-test-environment-deployment-runbook.md
      evidence-ref: HARN-016/INBOX-002
      readonly-boundary: REPO_CLOSED_DEFAULT
      live-verification-status: READY_FOR_ENV_SMOKE
  governance:
    base-url: http://localhost:8080/api/governance/internal
    connect-timeout-ms: 3000
    read-timeout-ms: 5000

auth:
  enabled: true
  trusted-auth-sources:
    - header
    - gateway
```

### JDBC Option

```yaml
query-execution:
  hetu:
    enabled: true
    jdbc:
      url: jdbc:hetu://<hetu-host>:<port>/<catalog>/<schema>
      username: <hetu-user>
      password: <hetu-password>
      query-timeout-seconds: 30
      max-rows: 200
    rest:
      endpoint: ""
      auth-token: ""
    client:
      enabled: false
      endpoint: ""
```

### REST Option

```yaml
query-execution:
  hetu:
    enabled: true
    jdbc:
      url: ""
      username: ""
      password: ""
    rest:
      endpoint: http://<hetu-rest-host>:<port>/query
      auth-token: <optional-rest-token>
      connect-timeout-ms: 3000
      read-timeout-ms: 5000
      max-rows: 200
    client:
      enabled: false
      endpoint: ""
```

### CLIENT Option

```yaml
query-execution:
  hetu:
    enabled: true
    jdbc:
      url: ""
      username: ""
      password: ""
    rest:
      endpoint: ""
      auth-token: ""
    client:
      enabled: true
      endpoint: http://<hetu-client-host>:<port>/v1/statement
      user: <hetu-user>
      source: sqlforge-query-execution
      catalog: <catalog>
      schema: <schema>
      auth-token: <optional-client-token>
      connect-timeout-ms: 3000
      read-timeout-ms: 5000
      max-rows: 200
      max-pages: 10
```

说明：

- `query-execution` 默认会按 `hetu.calibration.route-order` 先排序，再回补 `allowed-modes` 中未显式列出的模式；仓库默认基线仍是 `JDBC -> REST -> CLIENT`
- 如果你只想证明某一个模式，其他模式字段最好清空或禁用
- `cluster-evidence.*` 只用于描述当前环境候选事实和留证位置，不应把这组值提交成仓库默认真值

## IDEA Startup

### Governance

IDEA Run Configuration 建议：

- `Main class`: `com.company.governance.GovernanceApplication`
- `Use classpath of module`: `governance`
- `JRE`: `JDK 8u112`
- `Active profiles`:
  - `test`
  - 如果用了本地 overlay：`test,test-local`
- `VM options`:
  - `-Dfile.encoding=UTF-8`

启动后检查：

- `http://localhost:8080/actuator/health`

### Query-Execution

IDEA Run Configuration 建议：

- `Main class`: `com.company.queryexecution.QueryExecutionApplication`
- `Use classpath of module`: `query-execution`
- `JRE`: `JDK 8u112`
- `Active profiles`:
  - `test`
  - 如果用了本地 overlay：`test,test-local`
- `VM options`:
  - `-Dfile.encoding=UTF-8`

启动后检查：

- `http://localhost:8081/actuator/health`

## What This Runbook Does Not Ask You To Validate

本轮部署文档明确不要求你先做这些：

- Kafka
- Kafka topic / bootstrap / 安全参数
- Linux 守护启动
- Docker / k8s 编排
- 一键 shell smoke

这些都不是你这轮 Win10 + IDEA 测试环境部署的前置项。

## Deployment Confirmation Checklist

下面是你需要确认的清单和选项。

### 1. YML 写法

二选一：

- `A`：直接修改 `application-test.yml`
- `B`：新增 `application-test-local.yml`，IDEA 使用 `test,test-local`

推荐：`B`

### 2. Hetu 接入方式

三选一：

- `A`：`JDBC`
- `B`：`REST`
- `C`：`CLIENT`

推荐：如果只是先打通第一条真实链路，优先 `JDBC`

### 3. Governance / Query-Execution 运行位置

二选一：

- `A`：两个服务都在当前 Win10 机器上由 IDEA 启动
- `B`：服务在当前 Win10 机器上，Hetu / MRS 在远端测试集群

推荐：`B`

### 4. 鉴权来源

二选一：

- `A`：`test` profile + `header`
- `B`：`prod` profile + `gateway`

推荐：`A`

### 5. 查询验证数据

二选一：

- `A`：Hetu 里已有 `orders` 表，可直接沿用默认查询
- `B`：没有 `orders` 表，需要你指定一张替代表

如果选 `B`，后续我再给你对应的查询/留证调整口径。

### 6. 是否需要下一步给你真实留证命令

二选一：

- `A`：先只完成部署和服务启动，我确认后再要真实留证命令
- `B`：部署完成后立刻需要 JDBC / REST / CLIENT 的留证命令模板

推荐：先选 `A`

## Recommended Minimal Flow

1. 在 Win10 工作副本里完成一次 Maven 构建。
2. 初始化或迁移测试库。
3. 选定 yml 写法：`A` 或 `B`。
4. 在 yml 中填好 `governance` 的 MySQL / Redis / crypto 配置。
5. 在 yml 中填好 `query-execution` 的 governance base-url 和 Hetu 模式参数。
6. 在 IDEA 中分别启动 `governance` 与 `query-execution`。
7. 先只看两个 `/actuator/health`。
8. 对照上面的 6 项确认清单，把你的选项定下来。

## Route Calibration And Evidence Capture

完成服务启动后，可先读取只读 calibration 快照，再执行真实 Hetu smoke：

```bash
export QUERY_EXECUTION_BASE_URL=http://localhost:8081
export EXPECTED_QUERY_EXECUTION_MODE=REAL
export EXPECTED_HETU_ROUTE_PROFILE=TEST_ENV_CANDIDATE
export EXPECTED_HETU_ROUTE_ORDER=JDBC,REST,CLIENT
export EXPECTED_HETU_EVIDENCE_SOURCE=ENVIRONMENT_SMOKE_CANDIDATE
export EXPECTED_HETU_LIVE_VERIFICATION_STATUS=READY_FOR_ENV_SMOKE
export HETU_ENV_EVIDENCE_OUTPUT_PATH=./hetu-env-evidence.json
bash scripts/run-hetu-env-smoke.sh
```

脚本会先读取 `GET /api/query-execution/internal/hetu/route-calibration`，再执行一次真实 `POST /api/query-execution/queries/execute`，并在设置 `HETU_ENV_EVIDENCE_OUTPUT_PATH` 时输出结构化证据包：

- `routeCalibration`: 当前 route profile / mode priority / ready-unready / cluster evidence 快照
- `executionResponse`: 真实查询执行返回，含 `executionMode`、`attemptedModes[]`、`routeProfile`、`routeOrder[]`

该证据文件属于 environment-backed 证据，默认不入仓；请按你们的测试环境归档规则保存在外部证据位置。

## Related Documents

- [Huawei Cloud Setup](/models/project/codex/SQLForge/docs/deployments/huawei-cloud-setup.md)
- [Test Environment Smoke Baseline](/models/project/codex/SQLForge/docs/deployments/test-environment-smoke-baseline.md)
- [Observability Baseline](/models/project/codex/SQLForge/docs/deployments/observability-baseline.md)
- [Backup Recovery Baseline](/models/project/codex/SQLForge/docs/deployments/backup-recovery-baseline.md)
