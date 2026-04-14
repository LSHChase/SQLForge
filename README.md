# SQLForge

SQLForge 正在切换到您指定的生产栈：

- 前端：Vue + JavaScript + CSS
- 后端：Java 8 + Spring Boot
- 架构：前后端分离
- 数据引擎：MySQL、Trino、Presto、ClickHouse、MRS-Hetu、Kyligence
- 工程规范：UTF-8、Unix/LF、ARM 架构兼容

## 当前仓库状态

本仓库现在同时包含两部分：

- `frontend/`：Vue 多菜单双语控制台（中文 / English）
- `backend/`：新的 Spring Boot 后端骨架

历史 `src/` 与 `test/` 目录保留为早期 Node.js 原型参考，不再作为主交付结构。

## 目标目录

```text
frontend/              Vue + JS + CSS 前端
backend/               Java 8 + Spring Boot 后端
docs/                  架构、产品规格、执行计划
src/                   历史 Node.js 原型参考
test/                  历史 Node.js 原型测试
```

## 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

启动后默认进入多工作区界面，包含：

- 总览
- 连接管理
- SQL 结构分析
- Java 工作流
- 压测编排
- 交付门禁

界面内置中英文切换，不依赖额外 i18n 库。

`Java 工作流` 工作区目前已接入：

- BI 发布评估：`POST /api/v1/workflows/bi-release`
- BI 基线历史：`GET /api/v1/workflows/bi-release/baselines`
- 容量规划：`POST /api/v1/workflows/capacity-plan`
- 执行计划稳定性：`POST /api/v1/workflows/plan-stability`

其中 BI 发布评估结果会写入本地 baseline 存储，前端工作流工作区可直接刷新最近 12 条历史基线，用于跨重启回看 fingerprint、tenant、decision 与摘要指标。

## 后端启动

```bash
cd backend
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

## 容器启动

```bash
docker compose up --build
```

该交付路径使用前后端独立 Dockerfile，并选用支持 `amd64` / `arm64` 的基础镜像。

## 已提供的后端接口骨架

### 健康检查

```bash
curl http://localhost:8080/api/v1/system/health
```

### 支持的数据引擎列表

```bash
curl http://localhost:8080/api/v1/system/engines
```

### 驱动审计

```bash
curl http://localhost:8080/api/v1/system/driver-audit
```

### 已登记连接列表

```bash
curl http://localhost:8080/api/v1/connections
```

### 删除连接

```bash
curl -X DELETE http://localhost:8080/api/v1/connections/<connection-id>
```

### 连接离线校验

```bash
curl -X POST http://localhost:8080/api/v1/connections/validate \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Primary Trino",
    "engineCode": "trino",
    "host": "trino.sqlforge.local",
    "port": 8443,
    "catalog": "lakehouse",
    "username": "analyst",
    "password": "changeit",
    "sslEnabled": true
  }'
```

### 新增连接

```bash
curl -X POST http://localhost:8080/api/v1/connections \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Primary Trino",
    "engineCode": "trino",
    "host": "trino.sqlforge.local",
    "port": 8443,
    "catalog": "lakehouse",
    "username": "analyst",
    "password": "changeit",
    "sslEnabled": true
  }'
```

当前连接信息会以元数据形式持久化到后端本地文件，默认路径为 `${java.io.tmpdir}/sqlforge/connections.json`。密码不会出现在 API 返回结果里，也不会被写入该文件。

BI 发布评估工作流的历史 baseline 也会以文件形式持久化，默认路径为 `${java.io.tmpdir}/sqlforge/workflow-baselines.json`，用于跨重启保留上一次评估摘要。
该持久化 baseline 现已通过独立查询接口暴露给前端工作区，便于直接查看最近历史记录而不必再次执行工作流。
后端的租户容量画像目前通过共享的 tenant profile provider 提供，后续可替换为真实控制面配置来源。
BI 发布评估结果当前还会返回 `executorPlan`，用于描述当前 benchmark executor contract。现阶段默认 provider 为 `dry-run`，用于在真实执行器接入前固定执行边界。

### 连通性探测

```bash
curl -X POST http://localhost:8080/api/v1/connections/probe \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Primary Trino",
    "engineCode": "trino",
    "host": "127.0.0.1",
    "port": 8080,
    "catalog": "lakehouse",
    "username": "analyst",
    "password": "changeit",
    "sslEnabled": true
  }'
```

该接口会返回真实 TCP 连通性结果，以及按引擎生成的 JDBC URL 诊断信息。
如果当前服务 classpath 中存在对应 JDBC 驱动，还会继续尝试真实的 `DriverManager` 连接，并返回驱动层状态。

### SQL 预览

```bash
curl -X POST http://localhost:8080/api/v1/connections/query-preview \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Preview Trino",
    "engineCode": "trino",
    "host": "127.0.0.1",
    "port": 8080,
    "catalog": "lakehouse",
    "username": "analyst",
    "password": "changeit",
    "sslEnabled": true,
    "sql": "select 1 as health_check",
    "maxRows": 20
  }'
```

### BI 发布评估工作流

```bash
curl -X POST http://localhost:8080/api/v1/workflows/bi-release \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "tenant-a",
    "sql": "select user_id, sum(amount) from lake.orders where ds >= current_date - interval '\''7'\'' day group by 1",
    "slaMs": 5000,
    "targetConcurrency": 20
  }'
```

### BI 发布 baseline 历史

```bash
curl "http://localhost:8080/api/v1/workflows/bi-release/baselines?limit=12"
```

### 容量规划工作流

```bash
curl -X POST http://localhost:8080/api/v1/workflows/capacity-plan \
  -H 'Content-Type: application/json' \
  -d '{
    "baselineQps": 120,
    "trafficGrowthFactor": 2.4,
    "avgServiceTimeSec": 0.135,
    "currentWorkers": 24,
    "targetP99Ms": 4000,
    "hotDataGb": 2400
  }'
```

### 执行计划稳定性分析工作流

```bash
curl -X POST http://localhost:8080/api/v1/workflows/plan-stability \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId": "tenant-a",
    "sql": "select o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id group by 1",
    "slaMs": 5000,
    "targetConcurrency": 20,
    "currentPlan": {
      "planHash": "curr-1",
      "latencyMs": 1700,
      "distribution": "broadcast",
      "statsAgeHours": 36,
      "joinOrder": ["orders", "dim_users"]
    }
  }'
```

### SQL 意图结构分析

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis \
  -H 'Content-Type: application/json' \
  -d '{
    "statements": [
      {
        "id": "daily-report",
        "source": "manual-sample",
        "sql": "with recent_orders as (select user_id, amount from lake.orders where ds >= '\''2026-04-01'\'') select user_id, sum(amount) from recent_orders group by 1 order by sum(amount) desc"
      }
    ]
  }'
```

该接口执行纯结构分析：只基于 SQL 文本输出指纹、结构画像、意图标签和压测导向的负载分类，不连接数据库、不执行查询。
当前后端已经把结构解析收敛到可替换的 parser adapter 与 typed AST contract，便于后续接入真实 Trino parser。

### SQL 日批量结构分析

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/daily-batch \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;"
  }'
```

该接口支持将多条 SQL 以分号或空行分隔后一次性提交，返回批量结构分析摘要和逐条结构画像。

### SQL 压测准备计划

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/pressure-plan \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会基于纯结构分析结果，为一批 SQL 生成压测候选集、并发梯度、采样规则和分层摘要。

### SQL 场景化执行蓝图

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/scenario-blueprint \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会把每日 SQL 批次转成分阶段的压测蓝图，包括阶段目标、并发区间、候选语句集合、workload mix 和执行检查项。

### SQL 执行清单 Manifest

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/execution-manifest \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会把场景蓝图进一步转换成机器可读的执行清单，包含阶段顺序、入口/退出条件、指标关注点和全局 guardrails。

### SQL Campaign Schedule

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/campaign-schedule \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会在 execution manifest 之上生成阶段排期，包含 warmup/sample/cooldown 时间窗、promotion gate、fallback 行为和 handoff notes。

### SQL Run Package

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/run-package \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会把分析、计划、蓝图、manifest 和 schedule 组合成统一交付包，并给出建议文件名与 handoff checklist。

### SQL Briefing Report

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/briefing-report \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会把 run package 压缩成面向评审和交接的人类可读汇报稿，包含执行摘要、风险焦点、下一步动作和 review agenda。

### SQL Readiness Gate

```bash
curl -X POST http://localhost:8080/api/v1/sql/intent-analysis/readiness-gate \
  -H 'Content-Type: application/json' \
  -d '{
    "batchId": "daily-sql-batch",
    "source": "stress-sample",
    "targetConcurrency": 48,
    "rawSqlText": "select id, user_name from lake.users where id = 42 limit 1;\n\nselect o.user_id, sum(o.amount) from lake.orders o join lake.dim_users u on o.user_id = u.user_id where o.ds between '\''2026-04-01'\'' and '\''2026-04-14'\'' group by 1;\n\nselect user_id, row_number() over(partition by ds order by amount desc) from lake.orders"
  }'
```

该接口会基于结构侧产物给出 `blocked`、`caution` 或 `ready` 结论，并列出 blocker、放行前条件与建议动作。

## 工程约束

- 全仓库文本文件按 UTF-8 编码
- 统一使用 Unix/LF 行尾
- 前后端独立构建、独立部署
- 后端连接器层预留 Kyligence 和 MRS-Hetu 扩展位
- 运行环境优先兼容 `amd64` 与 `arm64`
- 仓库知识结构可通过 `npm run lint:docs` 做独立校验

## 仓库知识入口

- [AGENTS.md](/models/project/codex/SQLForge/AGENTS.md)
- [ARCHITECTURE.md](/models/project/codex/SQLForge/ARCHITECTURE.md)
- [PLANS.md](/models/project/codex/SQLForge/PLANS.md)
- [docs/architecture.md](/models/project/codex/SQLForge/docs/architecture.md)
- [docs/design-docs/index.md](/models/project/codex/SQLForge/docs/design-docs/index.md)
- [docs/product-specs/index.md](/models/project/codex/SQLForge/docs/product-specs/index.md)
