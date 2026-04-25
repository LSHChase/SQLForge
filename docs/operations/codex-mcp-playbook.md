# Codex MCP Playbook

## Purpose

本手册定义 SQLForge 当前如何在不破坏既有 `foreman` / `task_audit` / `closeout` 审计链的前提下使用 Codex + MCP。

`HARN-034` 的范围是“只读 MCP 治理基线”，因此本手册只覆盖：

- 只读 MCP
- 本地凭据与配置边界
- 自动化入口
- 外部证据如何写回仓库真值

## Day-1 Boundary

- 只读 MCP 仅限 4 个 category：
  - 观测/日志
  - 部署证据
  - 对象存储元数据
  - 外部需求/工单检索
- 先看 [docs/security/connectors.md](../security/connectors.md)，再决定是否允许本地接入。
- Main Foreman 仍是唯一 write-back / validate / closeout 入口。
- 当前不在 repo-tracked 配置中启用 `mcp_profile`。
- `.codex/policy/mcp-policy.json` 是编译后的 MCP 机器策略，不是新的长期真值。

## Local Connection Model

- 真实 server endpoint、token、密码、租户配置必须来自：
  - 用户本地 Codex 配置
  - 环境变量
  - 外部 secret store
- 仓库可以保存：
  - category 级治理规则
  - playbook
  - 非 secret 占位说明
  - `.codex/policy/mcp-policy.json`
- 仓库当前不要求提交 live server inventory，也不要求把本地 MCP 连接写进 `.codex/config.toml`。

## Required Entry Points

开始任何 MCP 相关任务前，至少执行：

1. `python3 scripts/foreman.py preflight --task <TASK_ID> --task-class standard --prompt "<PROMPT>"`
2. `python3 scripts/foreman.py compile-governance`
3. `python3 scripts/validate_codex_runtime.py`

任务完成前仍必须回到标准链路：

1. `python3 scripts/foreman.py validate <TASK_ID>`
2. `python3 scripts/task_audit.py --check --phase pre-closeout`
3. `python3 scripts/foreman.py closeout <TASK_ID> --stage-path <FILE> ...`

## Execution Flow

1. 先确认当前需求属于允许的只读 category，而不是可写 MCP。
2. 在本地运行 `compile-governance` 和 `validate_codex_runtime.py`，确认 `mcp-policy.json` 与 repo-tracked config 边界正常。
3. 用用户本地配置接入只读 MCP，采集外部证据。
4. 把需要长期保留的内容写回 SQLForge 审计链：
   - `tasks.md` / `tasks-done.md`
   - `docs/quality/validation-log.md`
   - 执行计划
   - `INBOX.md`
   - 权威文档
5. 仍由 Main Foreman 完成 validate、closeout 和单任务提交。

## MCP Intake Mini Template

当你准备在本地试用一个只读 MCP 时，先补全下面这份最小模板：

- Category:
- Local server alias:
- Evidence target:
- Allowed operations:
- Secret source:
- Expected write-back target in repo:
- Why this does not require repo-tracked `mcp_profile`:

## Not Allowed In HARN-034

- 可写云控
- SSH
- K8s `exec` / `apply` / `rollout`
- 数据库执行型 MCP
- 任何直接改变远端状态的工单/对象存储能力
- 用外部 MCP 结果直接宣布 task 完成
- 在 repo-tracked 配置、manifest 或模板中提前启用 `mcp_profile`

## Next Phase Boundary

`HARN-035` 之后，仓库才会考虑把 `mcp_profile` 带入 multi-agent manifest，并且只允许 explorer / validator 读取外部证据。

在那之前，保持当前规则：

- 单 agent / Main Foreman 主路径不变
- 只读 MCP 不越过 repo write-back 边界
- validate / closeout / 提交仍只通过现有标准动作执行
