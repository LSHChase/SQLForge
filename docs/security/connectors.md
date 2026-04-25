# Connector And MCP Security Boundaries

## Purpose

本文件是 SQLForge 关于外部 connector 与 Codex MCP 的统一安全锚点，承接 `R-047` 与 `HARN-034` 的最小可落地 MCP 治理基线。

目标不是先把 live server 配置或凭据落仓，而是先冻结：

- 当前允许的 category
- 允许的只读操作
- 明确禁止的操作
- 凭据和本地配置边界
- 证据如何写回仓库真值

## Current Baseline

- `HARN-034` 只建立治理基线，不代表本仓库已经默认接通任何 live MCP server。
- 第一批只允许 4 类只读 MCP：
  - 观测/日志
  - 部署证据
  - 对象存储元数据
  - 外部需求/工单检索
- 当前禁止把 repo-tracked `mcp_profile`、live server inventory、token、endpoint 或租户私有配置直接写入仓库。
- Main Foreman 仍是唯一 write-back / validate / closeout 入口。

## Approved First-Batch Read-Only Categories

| Category | 典型 server / 证据面 | 允许的只读操作 | 典型产出 | 明确禁止 |
|:---|:---|:---|:---|:---|
| 观测/日志 | 日志检索、告警上下文、指标元数据 | `list`、`search`、`read`、`tail`、`fetch-metadata` | 异常日志片段、指标快照、告警上下文 | ack / silence / close alert、删日志、改保留期 |
| 部署证据 | 发布状态、构建产物、变更证据 | `list`、`read`、`inspect-status`、`fetch-metadata` | release state、build metadata、rollout evidence | deploy、rollback、approve、promote、delete |
| 对象存储元数据 | bucket/prefix、object head、版本元数据 | `list`、`head`、`read-metadata`、`inspect-version-history` | object metadata、retention/etag/version 证据 | upload、delete、restore、retag、改 retention |
| 外部需求/工单检索 | 需求系统、工单系统、附件只读检索 | `search`、`list`、`read`、`download-readonly-attachment` | requirement/ticket 内容、附件只读副本 | create、comment、assign、transition、close |

## Explicitly Forbidden Server Types

以下 server 类型在当前基线中一律不接：

- 可写云控 / cloud control
- SSH
- K8s 执行型能力，包括 `exec`、`apply`、`rollout` 控制
- 数据库执行型 MCP
- 会改变工单状态、审批链或远端配置的 workflow 型 connector
- 任何对象上传、删除、恢复或生命周期修改型 connector

若需求要扩展到上述能力，必须新开 formal task，并同步更新规则、验证规则、playbook 与 compiled policy。

## Global Controls

- 外部 MCP 证据不是仓库长期真值。
  - 只有当证据被明确写回任务台账、执行计划、验证日志、INBOX 或权威文档后，才进入审计链。
- 仓库不得保存真实 MCP secret。
  - token、密码、endpoint、租户配置、临时 session 信息必须来自用户本地配置、环境变量或外部 secret store。
- `python3 scripts/foreman.py compile-governance` 必须生成 `.codex/policy/mcp-policy.json`。
- `python3 scripts/validate_codex_runtime.py` 必须验证 `mcp-policy.json`、索引文档和 repo-tracked runtime config 边界。
- 在 `HARN-034` 基线阶段，不得在 repo-tracked 运行配置、manifest 或模板中启用 `mcp_profile`。

## Connector Intake Record Template

新增具体 connector 或准备让本地 Codex 使用某个只读 MCP 时，先按以下模板记录，再决定是否需要单独 formal task：

- Category:
- Server / source name:
- Evidence needed:
- Allowed read operations:
- Explicitly forbidden operations:
- Secret source:
- Local runtime location:
- Why repo-tracked config is unnecessary:
- Write-back target in SQLForge:
- Human confirmation point:

## Validation Checklist

1. `docs/security/connectors.md` 已更新 category / server 边界，而不是只在 prompt 中口头说明。
2. `docs/rules/codex-rules.md` 已包含 `R-170` 至 `R-173`。
3. `docs/quality/validation-rules.md` 已包含对应 MCP 验证规则。
4. `python3 scripts/foreman.py compile-governance` 已生成 `.codex/policy/mcp-policy.json`。
5. `python3 scripts/validate_codex_runtime.py` 已通过，并确认 repo-tracked `.codex/config.toml` 未启用 live MCP runtime config。
6. 若任务真的消费了外部 MCP 证据，证据已通过任务台账、执行计划、验证日志或 INBOX 写回仓库审计链。

## Current Repository Position

当前仓库对 MCP 的工程立场是：

- 先做治理，再做接入。
- 先做只读 category，再做具体 server。
- 先保留 Main Foreman 唯一写边界，再讨论 `mcp_profile`、多 agent 和更深层自动化。
