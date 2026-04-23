# Hetu / MRS Test-Environment Deployment Runbook

## Purpose

本文用于指导外部测试环境 owner 只部署 `governance` 与 `query-execution` 两个服务，并在真实 Hetu / MRS 上执行 `bash scripts/run-hetu-env-smoke.sh`，把 `JDBC` / `REST` / `CLIENT` 任一真实返回证据留档。

本文只覆盖当前仓库已具备的能力：

- `governance` 统一授权入口
- `query-execution` 真实 Hetu `JDBC` / `REST` / `CLIENT` 接入
- 外部环境 Hetu smoke 脚本
- 审计链、授权矩阵和最小部署后证据保留方式

本文不把外部测试环境写成仓库 `repo-closed` 主路径的替代品。

## Recommended Profile Choice

测试环境推荐优先使用：

- `governance`: `SPRING_PROFILES_ACTIVE=test`
- `query-execution`: `SPRING_PROFILES_ACTIVE=test`

原因：

- `governance` `test` profile 默认 `messaging.mode=DATABASE`，不要求先接 Kafka
- `governance` `test` profile 信任 `header,gateway`
- `query-execution` `test` profile 信任 `header,gateway`
- `run-hetu-env-smoke.sh` 默认发送 `X-Auth-Source: header`，可直接兼容

只有在测试环境已经具备完整网关透传、Kafka 和生产式鉴权链时，才建议改用 `prod` profile。

## Deployment Checklist

- JDK 8 运行时
- Maven 3.8+
- MySQL 8.0 或兼容 TDSQL
- Redis 7.x 或兼容实例
- 一套真实 Hetu / MRS 可访问入口
- `query-execution` 到 `governance` 的 HTTP 连通
- `query-execution` 到 Hetu / MRS 的网络连通
- 一把可用的 `SQLFORGE_TEST_CRYPTO_KEY_BASE64`
- Hetu 侧可执行 `SELECT * FROM orders` 的测试数据
- 为 `tenant-a` 保留默认治理授权，或提前准备你自己的租户/数据源初始化

## Current Default Assumptions

当前仓库默认 happy path 依赖以下事实：

- `run-hetu-env-smoke.sh` 默认使用 `REQUEST_TENANT_ID=tenant-a`
- `run-hetu-env-smoke.sh` 默认使用 `REQUEST_ROLE_CODES=TENANT_ADMIN,ANALYST`
- `query-execution` 默认把 `HETU` 映射到治理数据源 `query-hetu`
- `governance` 默认授权矩阵里 `tenant-a -> query-hetu -> USE` 为 `ACTIVE`
- smoke SQL 固定为 `SELECT * FROM orders`

如果你不使用这些默认值，必须在部署前同步修改环境变量或初始化治理矩阵。

## Build Artifacts

在仓库根目录执行：

```bash
mvn -B -pl governance,query-execution -am clean package -DskipTests
```

产物默认位于：

- `governance/target/governance-0.1.0-SNAPSHOT.jar`
- `query-execution/target/query-execution-0.1.0-SNAPSHOT.jar`

建议同时记录当前部署 commit：

```bash
git rev-parse HEAD
```

## Database Initialization

### Fresh Database

新测试环境建议直接初始化完整最新 schema：

```bash
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/init-schema.sql
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/init-data.sql
```

### Existing Database

如果测试环境数据库已存在历史数据，不要重复覆盖 `init-schema.sql`。改为按顺序补增量脚本：

```bash
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/migrations/V20260421_011__core_traceability_chain.sql
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/migrations/V20260421_013__sensitive_data_encryption_baseline.sql
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/migrations/V20260422_014__sql_optimization_task_persistence.sql
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/migrations/V20260422_015__benchmark_engine_task_report_persistence.sql
mysql -h <db-host> -u <db-user> -p'<db-password>' < sql/migrations/V20260422_016__governance_history_lookup_index.sql
```

## Required Environment Variables

### Governance

推荐以 `test` profile 启动：

```bash
export SPRING_PROFILES_ACTIVE=test

export SQLFORGE_TEST_DB_URL='jdbc:mysql://<db-host>:3306/sqlforge_test?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export SQLFORGE_TEST_DB_USER='<db-user>'
export SQLFORGE_TEST_DB_PASSWORD='<db-password>'
export SQLFORGE_TEST_REDIS_HOST='<redis-host>'
export SQLFORGE_TEST_REDIS_PORT='6379'
export SQLFORGE_TEST_CRYPTO_KEY_ID='governance-test-key'
export SQLFORGE_TEST_CRYPTO_KEY_BASE64='<32-byte-key-in-base64>'
```

说明：

- `SQLFORGE_TEST_CRYPTO_KEY_BASE64` 必填；缺失会影响敏感字段加密能力
- 如果你的测试环境不是 `sqlforge_test` 库名，请同步修改 URL
- `governance` `test` profile 默认会启用授权矩阵并接受 `header` 鉴权来源

### Query-Execution Common

同样推荐 `test` profile：

```bash
export SPRING_PROFILES_ACTIVE=test

export QUERY_EXECUTION_GOVERNANCE_BASE_URL='http://<governance-host>:8080/api/governance/internal'
export QUERY_EXECUTION_GOVERNANCE_CONNECT_TIMEOUT_MS='3000'
export QUERY_EXECUTION_GOVERNANCE_READ_TIMEOUT_MS='5000'

export QUERY_EXECUTION_HETU_ENABLED='true'
```

说明：

- `QUERY_EXECUTION_GOVERNANCE_BASE_URL` 必须指向 `governance` 的内部能力入口，而不是网关首页
- `QUERY_EXECUTION_HETU_ENABLED=true` 是真实 Hetu 链路前提
- `query-execution` 默认按 `JDBC -> REST -> CLIENT` 顺序尝试

### Mode-Specific Configuration

#### JDBC

只保留 JDBC 所需参数，其他模式保持空值：

```bash
export QUERY_EXECUTION_HETU_JDBC_URL='jdbc:hetu://<hetu-host>:<port>/<catalog>/<schema>'
export QUERY_EXECUTION_HETU_JDBC_USERNAME='<hetu-user>'
export QUERY_EXECUTION_HETU_JDBC_PASSWORD='<hetu-password>'
export QUERY_EXECUTION_HETU_JDBC_QUERY_TIMEOUT_SECONDS='30'
export QUERY_EXECUTION_HETU_JDBC_MAX_ROWS='200'

unset QUERY_EXECUTION_HETU_REST_ENDPOINT
unset QUERY_EXECUTION_HETU_REST_AUTH_TOKEN
export QUERY_EXECUTION_HETU_CLIENT_ENABLED='false'
unset QUERY_EXECUTION_HETU_CLIENT_ENDPOINT
```

#### REST

只保留 REST 所需参数，避免 JDBC 抢先成功：

```bash
unset QUERY_EXECUTION_HETU_JDBC_URL
unset QUERY_EXECUTION_HETU_JDBC_USERNAME
unset QUERY_EXECUTION_HETU_JDBC_PASSWORD

export QUERY_EXECUTION_HETU_REST_ENDPOINT='http://<hetu-rest-host>:<port>/query'
export QUERY_EXECUTION_HETU_REST_AUTH_TOKEN='<optional-rest-bearer-token>'
export QUERY_EXECUTION_HETU_REST_CONNECT_TIMEOUT_MS='3000'
export QUERY_EXECUTION_HETU_REST_READ_TIMEOUT_MS='5000'
export QUERY_EXECUTION_HETU_REST_MAX_ROWS='200'

export QUERY_EXECUTION_HETU_CLIENT_ENABLED='false'
unset QUERY_EXECUTION_HETU_CLIENT_ENDPOINT
```

#### CLIENT

CLIENT 模式必须显式打开：

```bash
unset QUERY_EXECUTION_HETU_JDBC_URL
unset QUERY_EXECUTION_HETU_JDBC_USERNAME
unset QUERY_EXECUTION_HETU_JDBC_PASSWORD
unset QUERY_EXECUTION_HETU_REST_ENDPOINT
unset QUERY_EXECUTION_HETU_REST_AUTH_TOKEN

export QUERY_EXECUTION_HETU_CLIENT_ENABLED='true'
export QUERY_EXECUTION_HETU_CLIENT_ENDPOINT='http://<hetu-client-host>:<port>/v1/statement'
export QUERY_EXECUTION_HETU_CLIENT_USER='<hetu-user>'
export QUERY_EXECUTION_HETU_CLIENT_SOURCE='sqlforge-query-execution'
export QUERY_EXECUTION_HETU_CLIENT_CATALOG='<catalog>'
export QUERY_EXECUTION_HETU_CLIENT_SCHEMA='<schema>'
export QUERY_EXECUTION_HETU_CLIENT_AUTH_TOKEN='<optional-client-bearer-token>'
export QUERY_EXECUTION_HETU_CLIENT_CONNECT_TIMEOUT_MS='3000'
export QUERY_EXECUTION_HETU_CLIENT_READ_TIMEOUT_MS='5000'
export QUERY_EXECUTION_HETU_CLIENT_MAX_ROWS='200'
export QUERY_EXECUTION_HETU_CLIENT_MAX_PAGES='10'
```

## Startup Order

### 1. Start Governance

建议单独 shell 加载 `governance` 变量后启动：

```bash
nohup java -jar governance/target/governance-0.1.0-SNAPSHOT.jar \
  > /tmp/sqlforge-governance.log 2>&1 &
```

健康检查：

```bash
curl -fsS http://<governance-host>:8080/actuator/health
```

### 2. Start Query-Execution

再加载 `query-execution` 变量并启动：

```bash
nohup java -jar query-execution/target/query-execution-0.1.0-SNAPSHOT.jar \
  > /tmp/sqlforge-query-execution.log 2>&1 &
```

健康检查：

```bash
curl -fsS http://<query-execution-host>:8081/actuator/health
```

## Smoke Execution

### Default Command

在仓库根目录执行：

```bash
export QUERY_EXECUTION_BASE_URL='http://<query-execution-host>:8081'
export REQUEST_TENANT_ID='tenant-a'
export REQUEST_USER_ID='hetu-smoke-bot'
export REQUEST_ROLE_CODES='TENANT_ADMIN,ANALYST'
export REQUEST_AUTH_SOURCE='header'
export EXPECTED_QUERY_EXECUTION_MODE='JDBC'   # JDBC | REST | CLIENT | REAL

mkdir -p evidence/hetu-smoke
bash scripts/run-hetu-env-smoke.sh | tee evidence/hetu-smoke/run-$(date +%Y%m%d%H%M%S).log
```

模式说明：

- 想证明“任一真实模式都可以”，用 `EXPECTED_QUERY_EXECUTION_MODE=REAL`
- 想固化单一路径证据，分别用 `JDBC`、`REST` 或 `CLIENT`

### If You Use Prod Profile

如果服务使用 `prod` profile：

- `governance` 和 `query-execution` 默认只信任 `gateway,token`
- `run-hetu-env-smoke.sh` 默认发 `REQUEST_AUTH_SOURCE=header`

因此必须二选一：

- 通过真实网关入口执行，并让网关透传受保护请求头，同时设置 `REQUEST_AUTH_SOURCE=gateway`
- 或者显式把测试环境的 trusted auth source 改为包含 `header`

如果这一步没处理，smoke 会在鉴权阶段直接被拒绝。

## Evidence Retention

最少保留以下留档材料：

- smoke 原始日志：`evidence/hetu-smoke/run-*.log`
- 实际部署 commit SHA
- 运行模式：`JDBC` / `REST` / `CLIENT`
- 执行时间、操作者、目标 URL
- 脱敏后的环境快照
- 至少一段真实返回 JSON 证据，能看见：
  - `payload.status == SUCCESS`
  - `payload.metadata.targetEngine == HETU`
  - `payload.metadata.executionMode == <mode>`
  - `payload.implementationStage == HETU_REAL_INTEGRATION`

建议额外生成一个摘要文件：

```bash
{
  echo "timestamp=$(date --iso-8601=seconds)"
  echo "operator=$(whoami)"
  echo "commit=$(git rev-parse HEAD)"
  echo "query_execution_base_url=${QUERY_EXECUTION_BASE_URL}"
  echo "expected_mode=${EXPECTED_QUERY_EXECUTION_MODE}"
} > evidence/hetu-smoke/summary-$(date +%Y%m%d%H%M%S).txt
```

脱敏环境快照示例：

```bash
env | grep -E '^(SPRING_PROFILES_ACTIVE|SQLFORGE_TEST_|QUERY_EXECUTION_|REQUEST_|EXPECTED_QUERY_EXECUTION_MODE)' \
  | sed -E 's/(PASSWORD|TOKEN|KEY_BASE64)=.*/\\1=<redacted>/g' \
  > evidence/hetu-smoke/env-$(date +%Y%m%d%H%M%S).txt
```

## Recommended Archive Layout

```text
evidence/hetu-smoke/
├── run-20260423T120000.log
├── env-20260423T120000.txt
└── summary-20260423T120000.txt
```

## Common Failures

### 401 / 403 Before Query Execution

优先检查：

- `REQUEST_AUTH_SOURCE` 是否与服务 profile 的 trusted auth sources 匹配
- `REQUEST_TENANT_ID` / `REQUEST_ROLE_CODES` 是否与治理矩阵匹配
- `query-execution` 调 `governance` 的 `QUERY_EXECUTION_GOVERNANCE_BASE_URL` 是否正确

### Access Denied For Datasource

优先检查：

- 当前租户是否仍为 `tenant-a`
- `query-execution` 是否仍把 `HETU` 映射到 `query-hetu`
- `governance` 中 `tenant -> datasource -> USE` 是否为 `ACTIVE`

### JDBC / REST / CLIENT Mode Mismatch

优先检查：

- 你是否还保留了其他模式的可用配置
- `EXPECTED_QUERY_EXECUTION_MODE` 是否与当前配置一致
- `query-execution` 默认会按 `JDBC -> REST -> CLIENT` 顺序尝试

### Hetu Query Failed

优先检查：

- Hetu 侧是否真的存在 `orders` 表或等效视图
- `catalog` / `schema` 是否与 `orders` 所在位置一致
- JDBC URL、REST endpoint、CLIENT endpoint 是否来自同一套可用环境
- `query-execution` 主机到 Hetu / MRS 的网络和安全组是否放通

## Minimal Operator Flow

1. 构建 `governance` 和 `query-execution` JAR。
2. 初始化数据库。
3. 用 `test` profile 启动 `governance`。
4. 配好 Hetu 模式参数后启动 `query-execution`。
5. 先做两个 `/actuator/health` 检查。
6. 运行 `bash scripts/run-hetu-env-smoke.sh` 并 `tee` 到证据目录。
7. 保存日志、commit、脱敏环境快照和模式摘要。

## Related Documents

- [Huawei Cloud Setup](/models/project/codex/SQLForge/docs/deployments/huawei-cloud-setup.md)
- [Test Environment Smoke Baseline](/models/project/codex/SQLForge/docs/deployments/test-environment-smoke-baseline.md)
- [Observability Baseline](/models/project/codex/SQLForge/docs/deployments/observability-baseline.md)
- [Backup Recovery Baseline](/models/project/codex/SQLForge/docs/deployments/backup-recovery-baseline.md)
