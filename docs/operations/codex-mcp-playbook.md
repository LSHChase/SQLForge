# Codex MCP Playbook

## Purpose

本手册定义 SQLForge 当前如何在不破坏既有 `foreman` / `task_audit` / `closeout` 审计链的前提下使用 Codex + MCP。

`HARN-034` 建立了只读 MCP 治理基线，`HARN-035` 把该基线扩展到 multi-agent `mcp_profile`。因此本手册当前覆盖：

- 只读 MCP
- 本地凭据与配置边界
- 自动化入口
- 外部证据如何写回仓库真值
- manifest-level `mcp_profile` 的当前允许范围

## Product Positioning

- SQLForge 当前对 MCP 的定位是受治理的只读证据增强。
- MCP 不是远端自动运维。
- MCP 不是可写控制面。
- 无论单 agent 还是 multi-agent，`Main Foreman` 仍是唯一 write-back / validate / closeout 入口。

## Current Boundary

- 只读 MCP 仅限 4 个 category：
  - 观测/日志
  - 部署证据
  - 对象存储元数据
  - 外部需求/工单检索
- 先看 [docs/security/connectors.md](../security/connectors.md)，再决定是否允许本地接入。
- Main Foreman 仍是唯一 write-back / validate / closeout 入口。
- 单 agent / Main Foreman 主路径不要求 repo-tracked `mcp_profile`。
- 当前允许的 repo-tracked `mcp_profile` 仅限 multi-agent manifest/template 中的符号化字段，且只能分配给 `explorer/validator`。
- `worker`、Main Foreman 和 Auto Foreman 不得声明或消费 manifest-declared `mcp_profile`。
- `.codex/policy/mcp-policy.json` 是编译后的 MCP 机器策略，不是新的长期真值。

## MCP Doctor

在尝试任何本地 MCP onboarding、multi-agent `mcp_profile` 试用或运行时诊断前，先执行：

```bash
python3 scripts/mcp_doctor.py --check
```

该 doctor 只检查本地治理边界、文档完备性、manifest/template 合同和 repo-tracked 配置红线；不会连接远端系统，也不会执行远端操作。repo 级运行态健康仍由：

```bash
python3 scripts/governed_healthcheck.py --check
```

负责。

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
  - multi-agent manifest/template 中的符号化 `mcp_profiles` / `mcp_profile`
- 仓库当前不要求提交 live server inventory，也不要求把本地 MCP 连接写进 `.codex/config.toml`。
- `.codex/config.toml` 继续禁止保存 live MCP server inventory、token、endpoint 或其他 repo-owned MCP runtime config。

## Required Entry Points

开始任何 MCP 相关任务前，至少执行：

1. `python3 scripts/foreman.py preflight --task <TASK_ID> --task-class standard --prompt "<PROMPT>"`
2. `python3 scripts/mcp_doctor.py --check`
3. `python3 scripts/foreman.py compile-governance`
4. `python3 scripts/validate_codex_runtime.py`

若本轮要使用 multi-agent `mcp_profile`，还必须让 manifest 链路经过：

1. `bash scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest <TASK_MANIFEST>`
2. `bash scripts/multi_agent_launch.sh --manifest <TASK_MANIFEST>`
3. `bash scripts/multi_agent_collect.sh --manifest <TASK_MANIFEST>`

任务完成前仍必须回到标准链路：

1. `python3 scripts/foreman.py validate <TASK_ID>`
2. `python3 scripts/task_audit.py --check --phase pre-closeout`
3. `python3 scripts/foreman.py closeout <TASK_ID> --stage-path <FILE> ...`

## Execution Flow

1. 先确认当前需求属于允许的只读 category，而且使用角色仍在 `explorer/validator` 范围内。
2. 在本地运行 `python3 scripts/mcp_doctor.py --check`、`compile-governance` 和 `validate_codex_runtime.py`，确认 onboarding 文档、`mcp-policy.json` 与 repo-tracked config 边界正常。
3. 若是单 agent 路径，用用户本地配置接入只读 MCP；若是 multi-agent 路径，只允许在 manifest 中声明 top-level `mcp_profiles` registry 和 `explorer/validator` 的 `mcp_profile`。
4. 用用户本地配置、环境变量或外部 secret store 解析真实 MCP 连接，采集外部证据。
5. 把需要长期保留的内容写回 SQLForge 审计链。Evidence write-back target 只允许落到：
   - `tasks.md` / `tasks-done.md`
   - `docs/quality/validation-log.md`
   - 执行计划
   - `INBOX.md`
   - 权威文档
6. 仍由 Main Foreman 完成 validate、closeout 和单任务提交。

## Category Onboarding Quickstart

- `observability_logs`
  - Local prerequisites: 用户本地只读日志/指标查询配置已就绪。
  - Evidence write-back target: 执行计划、验证日志、任务台账。
- `deployment_evidence`
  - Local prerequisites: 用户本地只读发布/构建证据访问已就绪。
  - Evidence write-back target: 执行计划、验证日志、任务台账。
- `object_storage_metadata`
  - Local prerequisites: 用户本地 object metadata 只读访问已就绪。
  - Evidence write-back target: 执行计划、验证日志、相关权威文档。
- `external_requirements_tickets`
  - Local prerequisites: 用户本地需求/工单系统只读检索已就绪。
  - Evidence write-back target: 任务台账、INBOX、执行计划、验证日志。

更细的 Allowed read operations、Explicitly forbidden operations 与 Local runtime location，统一以 [docs/security/connectors.md](../security/connectors.md) 为准。

## MCP Intake Mini Template

当你准备在本地试用一个只读 MCP 时，先补全下面这份最小模板：

- Category:
- Local server alias:
- Manifest profile name:
- Allowed roles:
- Evidence target:
- Allowed operations:
- Secret source:
- Expected write-back target in repo:
- Why this does not require repo-tracked live server inventory:

## Not Allowed In Current Baseline

- 可写云控
- SSH
- K8s `exec` / `apply` / `rollout`
- 数据库执行型 MCP
- 任何直接改变远端状态的工单/对象存储能力
- 在 repo-tracked 文件中写入 live server inventory、token、endpoint 或 tenant-specific secret
- 给 `worker`、Main Foreman 或 Auto Foreman 分配 `mcp_profile`
- 用外部 MCP 结果直接宣布 task 完成
- 让 `mcp_profile` 绕过 `prepare/launch/collect` 的 manifest 校验与 Main Foreman 写回链
- 把 `mcp_doctor.py` 包装成远端自动运维或远端探测入口

## Current Multi-Agent Extension

- `HARN-035` 现已允许 multi-agent manifest 通过 `mcp_profiles` / `mcp_profile` 给 `explorer/validator` 分配符号化只读证据配置。
- `multi_agent_prepare.sh` 会校验 profile registry、角色与 category。
- `multi_agent_launch.sh` 会把 `mcp_profile` 元数据写入 runtime assignment，并按 `agent.profile` 优先、`agent.mcp_profile` 次之的顺序解析 Codex profile。
- `multi_agent_collect.sh` 会记录 `mcp_profile`，并拒收非 `explorer/validator` 的 MCP 越界。

## Next Phase Boundary

后续若要继续扩展，仍需要新的 formal task，尤其是：

- 把只读 MCP 提升为可写 MCP
- 让 `worker` 或 Main Foreman 使用 MCP
- 在 repo-tracked 文件中保存 live server inventory 或 secret
- 让外部 MCP 绕过 validate / closeout / 提交标准动作
