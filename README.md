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

## 已提供的后端接口骨架

### 健康检查

```bash
curl http://localhost:8080/api/v1/system/health
```

### 支持的数据引擎列表

```bash
curl http://localhost:8080/api/v1/system/engines
```

### 已登记连接列表

```bash
curl http://localhost:8080/api/v1/connections
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
