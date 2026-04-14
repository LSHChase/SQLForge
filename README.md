# SQLForge

SQLForge 正在切换到您指定的生产栈：

- 前端：Vue + JavaScript + CSS
- 后端：Java 8 + Spring Boot
- 架构：前后端分离
- 数据引擎：MySQL、Trino、Presto、ClickHouse、MRS-Hetu、Kyligence
- 工程规范：UTF-8、Unix/LF、ARM 架构兼容

## 当前仓库状态

本仓库现在同时包含两部分：

- `frontend/`：新的 Vue 前端骨架
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

## 工程约束

- 全仓库文本文件按 UTF-8 编码
- 统一使用 Unix/LF 行尾
- 前后端独立构建、独立部署
- 后端连接器层预留 Kyligence 和 MRS-Hetu 扩展位
- 运行环境优先兼容 `amd64` 与 `arm64`

## 仓库知识入口

- [AGENTS.md](/models/project/codex/SQLForge/AGENTS.md)
- [ARCHITECTURE.md](/models/project/codex/SQLForge/ARCHITECTURE.md)
- [PLANS.md](/models/project/codex/SQLForge/PLANS.md)
- [docs/architecture.md](/models/project/codex/SQLForge/docs/architecture.md)
- [docs/design-docs/index.md](/models/project/codex/SQLForge/docs/design-docs/index.md)
- [docs/product-specs/index.md](/models/project/codex/SQLForge/docs/product-specs/index.md)
