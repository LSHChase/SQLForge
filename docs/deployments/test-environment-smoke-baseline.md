# Test Environment Smoke Baseline

## Purpose

本文作为 `F-TASK-033` 的当前权威落点，用于定义一条适用于外部测试环境 CI/CD 的最小 smoke 入口。

本文只写两类内容：

- 已被仓库脚本与本地验证证明的当前实现基线
- 外部测试环境 owner 需要如何调用该入口，才能把“部署成功”升级为“最小可运行验证已完成”

本文不把测试环境 smoke 写成仓库 `repo-closed` 主路径的替代品。

## Scope

- 外部测试环境部署后最小验证入口：`bash scripts/run-env-smoke.sh`
- 关联脚本：
  - `scripts/smoke-lib.sh`
  - `scripts/manual-query-governance-smoke.sh`
  - `scripts/manual-sql-optimization-governance-smoke.sh`
  - `scripts/manual-benchmark-governance-smoke.sh`
- 关联规则：
  - `R-117`
  - `R-123`
  - `R-126`
  - `R-141`

## Current Implemented Baseline

仓库当前已提供一条环境无关的最小 smoke 入口：

- `bash scripts/run-env-smoke.sh`

当前入口具备以下特征：

1. 通过环境变量接收服务 URL，而不是硬绑定 `localhost`、固定端口、容器名或本地 `docker exec mysql`
2. 默认覆盖：
   - `governance` health
   - `query-execution` health
   - `sql-optimization` health
   - `benchmark-engine` health
   - 前端可达性
   - `query-execution -> governance` 最小业务链路
   - `sql-optimization -> governance` 最小业务链路
   - `benchmark-engine -> governance` 最小业务链路
   - 至少一条受保护请求头 / tenant / role 上下文有效性验证
3. 失败时输出步骤名、URL、HTTP 状态码和响应摘要，便于外部流水线定位问题
4. 只依赖 HTTP 可观察行为，不把测试环境 smoke 默认绑定到容器内 DB 命令
5. 不替换现有 `scripts/run-runtime-smoke.sh` 本地 richer smoke 编排

## Default Command

```bash
bash scripts/run-env-smoke.sh
```

## Required Inputs

外部测试环境至少需要提供以下环境变量：

- `GOVERNANCE_BASE_URL`
- `QUERY_EXECUTION_BASE_URL`
- `SQL_OPTIMIZATION_BASE_URL`
- `BENCHMARK_ENGINE_BASE_URL`
- `FRONTEND_BASE_URL`
- `REQUEST_TENANT_ID`
- `REQUEST_USER_ID`
- `REQUEST_ROLE_CODES`

常见可选项：

- `GOVERNANCE_HEALTH_URL`
- `QUERY_EXECUTION_HEALTH_URL`
- `SQL_OPTIMIZATION_HEALTH_URL`
- `BENCHMARK_ENGINE_HEALTH_URL`
- `GOVERNANCE_PROTECTED_CHECK_URL`
- `REQUEST_AUTH_SOURCE`
- `ENV_SMOKE_POLL_ATTEMPTS`
- `ENV_SMOKE_POLL_INTERVAL_SECONDS`

## Minimal Coverage Contract

测试环境 minimal smoke 当前只承诺以下部署后最小验证：

1. 四个后端服务都能返回健康状态
2. 前端入口可访问
3. `query-execution` 能在受保护请求头下完成最小查询执行链路
4. `sql-optimization` 能提交任务并轮询到终态
5. `benchmark-engine` 能提交任务、轮询到终态并读取报告
6. `governance` 的受保护接口能接受合法请求头，并拒绝缺失请求头的匿名访问

以下能力不在本入口范围：

- Sonar
- 真实 Kafka secure/runtime smoke
- Hetu/MRS 真实环境 smoke
- 完整浏览器 E2E
- 外部测试环境内部数据库直查
- 仓库 `repo-closed` build/test/lint/coverage/db-script/runtime smoke 的替代

上面这条“Hetu/MRS 真实环境 smoke”仍不属于 `run-env-smoke.sh` 的最小契约；当前仓库为它单独提供了 environment-backed 入口：

- `bash scripts/run-hetu-env-smoke.sh`

该脚本只验证 `query-execution` 在外部环境里是否真的返回 `JDBC` / `REST` / `CLIENT` 之一，而不是 `SIMULATED`。

## Usage Pattern For External CI/CD

推荐由外部测试环境 owner 在部署完成后调用：

```bash
export GOVERNANCE_BASE_URL="https://governance.test.example.com"
export QUERY_EXECUTION_BASE_URL="https://query-execution.test.example.com"
export SQL_OPTIMIZATION_BASE_URL="https://sql-optimization.test.example.com"
export BENCHMARK_ENGINE_BASE_URL="https://benchmark-engine.test.example.com"
export FRONTEND_BASE_URL="https://sqlforge.test.example.com"
export REQUEST_TENANT_ID="tenant-a"
export REQUEST_USER_ID="smoke-bot"
export REQUEST_ROLE_CODES="TENANT_ADMIN,ANALYST"

bash scripts/run-env-smoke.sh
```

如果只需要先校验参数，可先运行：

```bash
bash scripts/run-env-smoke.sh --check-config
```

如果外部测试环境还需要单独追加 Hetu / MRS 实链验证，可再执行：

```bash
export QUERY_EXECUTION_BASE_URL="https://query-execution.test.example.com"
export REQUEST_TENANT_ID="tenant-a"
export REQUEST_USER_ID="hetu-smoke-bot"
export REQUEST_ROLE_CODES="TENANT_ADMIN,ANALYST"

bash scripts/run-hetu-env-smoke.sh
```

## Relationship To Existing Gates

三者必须明确区分：

1. `repo-closed` 本地主路径
   - `build/test/lint/coverage/db-script/runtime smoke/knowledge lint/compliance baseline`
   - 由仓库内 CI、phase gate 和本地标准动作保证
2. 测试环境 minimal smoke
   - 由外部测试环境 CI/CD 在部署后调用 `bash scripts/run-env-smoke.sh`
   - 只证明“系统已部署且最小关键链路能跑”
3. `environment-backed` fallback
   - Sonar
   - 真实 Kafka gate
   - 更高成本的外部环境验证

## Local Validation Boundary

仓库内能证明的只有：

1. `scripts/run-env-smoke.sh` 存在、参数化正确、帮助可用
2. 该脚本可在本地用显式 URL 跑通最小逻辑闭环
3. 现有 `scripts/run-runtime-smoke.sh` 未被破坏

仓库内不能伪造证明的内容：

1. 外部测试环境网络、域名、网关和证书实际可用
2. 外部测试环境 owner 已把该脚本接入独立 CI/CD
3. 外部测试环境部署产物与仓库 HEAD 一致

这些残余项必须由外部测试环境 owner 在独立流水线中保留证据。

## Current Limitation

仓库现在只负责提供统一脚本入口和文档真值；测试环境没有 Codex，且外部环境由独立 owner 管理。因此：

- 仓库不能代替外部 owner 持续执行测试环境 smoke
- 若外部 CI/CD 不调用该脚本，测试环境仍不能被记为“具备部署后最小验证闭环”

## Related Documents

- [CI Capability Baseline](/models/project/codex/SQLForge/docs/deployments/ci-capability-baseline.md)
- [Phase Gate Baseline](/models/project/codex/SQLForge/docs/deployments/phase-gate-baseline.md)
- [Local Development](/models/project/codex/SQLForge/docs/operations/local-development.md)
- [Document Truth Baseline](/models/project/codex/SQLForge/docs/plans/document-truth-baseline.md)
