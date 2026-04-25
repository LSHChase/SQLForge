# Multi-Agent Playbook

## Purpose

本手册定义 SQLForge 的多 agent 协作基础设施，覆盖 `semi-auto` 与 `full-auto` 两条路径。目标是在不引入第二套长期真值、不依赖隐式 subagent、不破坏既有 `foreman` / `task_audit` / `closeout` 审计链的前提下，让 Main Foreman 通过多个 `codex exec` 会话和多个 `git worktree` 并行推进复杂任务，并在 `HARN-035` 之后为 `explorer / validator` 提供受控 `mcp_profile` 只读证据能力。

本手册只适用于跨模块、可明确切 ownership、值得并行化的复杂任务。简单任务继续使用单 `codex` + `foreman` 工作流。

若当前只有规划/需求/计划、还没有正式 task，请先使用 `docs/operations/requirements-to-task-playbook.md` 提供的 task-shaping 流程，把 candidate task pack materialize 成正式 task，再进入本手册的 `semi-auto` 或 `full-auto` 入口。

对 MCP 的定位仍然固定为受治理的只读证据增强，而不是远端自动运维或可写控制面。

## Core Model

- Main Foreman 是唯一收口点。
- 多 agent 的默认技术载体是多个 `codex exec` 会话，不依赖隐式 subagent。
- 可写 agent 使用独立 `git worktree`；只读 explorer 可复用主 worktree。
- 整轮协作由一个 manifest 驱动。
- manifest 可以声明 top-level `mcp_profiles` registry，并且只允许 `explorer / validator` 通过 per-agent `mcp_profile` 读取只读外部证据。
- prompt 通过 `docs/agent-prompts/*.md` 模板化，不依赖每轮手工拼 prompt。
- worker 只交变更文件列表、实现摘要、已执行验证、residual risk；最终 fan-in、validate、audit、closeout 都由 Main Foreman 完成。
- `full-auto` 只是把“需求输入 -> exec plan/manifest 生成 -> 多 agent 协作 -> autonomous Main Foreman 收口”自动化；它不改变 Main Foreman 唯一收口、`foreman validate` / `task_audit` / `closeout` 仍然强制的治理事实。

## Modes

### Semi-Auto

- Main Foreman 手工编写 active exec plan 与 manifest。
- 然后执行 `prepare -> launch -> collect -> fan-in -> validate -> closeout`。
- 适合需求已经拆清、只需要标准化并行执行的任务。

### Full-Auto

- Main Foreman 先给出需求输入。
- `multi_agent_autoplan.sh` 调用 `auto-planner` 自动生成 exec plan 与 manifest。
- `multi_agent_full_auto.sh` 继续驱动 `prepare -> launch -> wait -> collect -> autonomous Main Foreman`。
- autonomous Main Foreman 仍只能在主 worktree 内做 fan-in、验证、台账更新和 closeout。

## Roles

### Main Foreman

- 负责 task shaping、manifest 审核、ownership 分配、冲突处理、fan-in、最终验证、台账更新和 closeout。
- 不得在 manifest 中声明或消费 `mcp_profile`。
- 唯一允许修改：
  - `tasks.md`
  - `tasks-done.md`
  - `INBOX.md`
  - `docs/quality/validation-log.md`
  - closeout / delivery write-back 相关文档
- 唯一允许执行：
  - `python3 scripts/foreman.py validate <TASK_ID>`
  - `python3 scripts/task_audit.py --check --phase pre-closeout`
  - `python3 scripts/foreman.py closeout <TASK_ID>`
  - `python3 scripts/task_audit.py --check --phase post-closeout`

### Auto Planner

- 只负责把需求输入转换为任务级 exec plan 与 manifest。
- 不直接修改台账，不直接 closeout。
- 必须复用现有 prompt 模板、ownership 规则和 forbidden paths 规则。

### Explorer

- 只读，不改文件。
- 输出事实核对、边界盘点、验证建议或 residual risk。
- 默认可复用主 worktree。
- 可以声明 `mcp_profile`，但只能读取 manifest 允许的只读外部证据 category。

### Worker

- 只在 manifest 指定的 ownership 内改动。
- 不得声明 `mcp_profile`。
- 不得修改台账、validation log 或 closeout 文档。
- 不得自行 closeout、归档、宣告任务单独完成。

### Validator

- 默认只验证，不主动改实现。
- 可以声明 `mcp_profile`，但只能读取 manifest 允许的只读外部证据 category。
- 如验证暴露实现问题，只输出失败点、证据和建议，不直接修代码，除非 Main Foreman 重新分派为 worker。

### Auto Foreman

- 是 `full-auto` 模式下的 autonomous Main Foreman。
- 读取需求、自动生成的 exec plan、manifest、collect summary 与仓库真值后继续做 fan-in、验证与 closeout。
- 仍然只能按 Main Foreman 规则行动，不能绕过治理链。
- 不得在 manifest 中声明或消费 `mcp_profile`。

## Non-Negotiable Rules

- worker 必须有明确 ownership。
- 两个 worker 的 ownership 不允许重叠。
- 只有 `explorer / validator` 可以声明 `mcp_profile`；`worker`、Main Foreman 和 Auto Foreman 一律不得声明。
- `mcp_profiles` 只能保存符号化元数据，不得包含 live server inventory、token、endpoint 或其他 secret。
- 在任何使用 `mcp_profile` 的 multi-agent 轮次前，先运行 `python3 scripts/mcp_doctor.py --check`。
- forbidden paths 至少包含：
  - `tasks.md`
  - `tasks-done.md`
  - `INBOX.md`
  - `docs/quality/validation-log.md`
- 所有 worker 都必须在最终输出中给出：
  - changed files
  - implementation summary
  - validation performed
  - residual risk
- explorer 若产生文件改动，视为越界。
- validator 若主动改实现，视为越界。
- collect 若发现非 `explorer / validator` 角色声明 `mcp_profile`，必须直接 reject。
- `.codex/` 下的多 agent 运行态文件仅是 runtime artifact，不是长期真值。
- `full-auto` 不得绕过 task shaping、preflight、instantiate、validate、task audit 或 closeout。

## Ownership Guidance

单轮只能由一个 agent 拥有的路径：

- `governance/**`
- `sqlforge-shared/**`
- `src/router/**`
- `src/services/**`
- `scripts/foreman.py`
- `scripts/task_audit.py`
- `.github/workflows/**`
- `tasks.md`
- `tasks-done.md`
- `INBOX.md`
- `docs/quality/validation-log.md`

可以并行，但不得改同一文件的路径：

- `query-execution/**`
- `sql-optimization/**`
- `benchmark-engine/**`
- `src/views/**`
- `docs/deployments/**`

建议做法：

- Main Foreman 在 manifest 里为每个 worker 指定最小 ownership 集合。
- prepare 阶段先阻断 ownership 冲突，再创建 worktree。
- collect 阶段按 changed files 和 forbidden paths 决定 accept / reject。

## Manifest Contract

起始模板：`docs/exec-plans/templates/multi-agent-run.template.json`

活动 manifest 命名约定：

- 文件放在 `docs/exec-plans/active/` 下
- 文件名采用 `<TASK_ID>-multi-agent-run.json`
- 下文命令示例中的 `TASK_MANIFEST` 表示“由 Main Foreman 从模板复制并按任务填充后的 active manifest 文件”

最低字段：

- `task_id`
- `mode`
- `main_worktree`
- `mcp_profiles`
- `agents`
- `name`
- `role`
- `worktree`
- `prompt_file`
- `mcp_profile`
- `ownership`
- `forbidden_paths`
- `validation_scope`

允许的 `mode`：

- `semi-auto`
- `full-auto`

当前模板额外支持：

- `codex.bypass_approvals_and_sandbox`
- `run_root`
- `codex.sandbox`
- `codex.full_auto`
- `codex.color`
- `codex.extra_args`
- per-agent `model`
- per-agent `profile`
- per-agent `extra_args`
- per-agent `notes`

### MCP Extension

当 manifest 需要受控 MCP 只读证据面时，必须遵守以下合同：

- top-level `mcp_profiles` 是 registry，而不是 live server inventory。
- 每个 profile 只允许声明：
  - `source`
  - `allowed_roles`
  - `allowed_categories`
  - `notes`
- `source` 只能是：
  - `local-user-config`
  - `env`
  - `external-secret-store`
- `allowed_categories` 只能来自当前 4 个只读 category：
  - `observability_logs`
  - `deployment_evidence`
  - `object_storage_metadata`
  - `external_requirements_tickets`
- per-agent `mcp_profile` 只能分配给 `explorer / validator`。
- `worker` 不得声明 `mcp_profile`，也不得通过 `profile` 字段伪装成 MCP 写角色。
- launcher 解析 Codex profile 时，优先使用 `agent.profile`；若未声明，再回退到 `agent.mcp_profile`。

`full-auto` 推荐额外写入：

- `requirements_artifact`
- `generated_plan`

运行态输出目录默认是：

- `.codex/state/multi-agent/<TASK_ID>/`

其中会生成：

- requirements snapshot
- autoplan prompt/schema/result
- rendered prompts
- launch scripts
- per-agent stdout/stderr logs
- per-agent last-message files
- collect summary
- autonomous Main Foreman logs

子 `codex exec` 默认策略：

- 自动化脚本默认按 `codex.bypass_approvals_and_sandbox = true` 生成或消费 manifest。
- 这是为了兼容“父级执行器已由外部环境托管沙箱”的场景，避免子会话再次落入不兼容的内层 `workspace-write` 沙箱。
- 如果运行环境支持 Codex 自带沙箱，可把 `codex.bypass_approvals_and_sandbox` 设为 `false`，再通过 `codex.full_auto` 或 `codex.sandbox` 控制子会话模式。
- 也可以通过环境变量 `SQLFORGE_CODEX_EXEC_MODE` 强制覆盖子 `codex exec` 模式；允许值是 `bypass`、`full-auto`、`read-only`、`workspace-write`、`danger-full-access`。

## Auto-Planning Contract

`multi_agent_autoplan.sh` 负责从需求输入生成：

- `docs/exec-plans/active/<TASK_ID>-full-auto-execution-plan.md`
- `docs/exec-plans/active/<TASK_ID>-multi-agent-run.json`

auto-planner 的输出必须满足：

- 只生成任务级 plan 与 manifest，不直接改台账。
- manifest 必须复用现有 prompt 模板。
- 若生成 `mcp_profiles` / `mcp_profile`，只能输出符号化只读 metadata，不得输出 live servers 或 secrets。
- `worker` 不得被分配 `mcp_profile`，且 `explorer / validator` 的 `mcp_profile` 必须引用已声明 registry。
- worker forbidden paths 必须覆盖治理禁区。
- worker ownership 不得重叠。
- 生成结果只落在 `docs/exec-plans/active/` 与 `.codex/state/multi-agent/<TASK_ID>/`。

## Workflow

### 1. Shape The Task

1. `python3 scripts/foreman.py preflight`
2. 确认任务已进入主计划、task-spec matrix、task-governance matrix。
3. `python3 scripts/foreman.py instantiate <TASK_ID>`
4. 若涉及 `mcp_profile`，先执行 `python3 scripts/mcp_doctor.py --check`。
5. 选择 `semi-auto` 或 `full-auto` 入口。

### 2. Semi-Auto Entry

Main Foreman 手工编写 active manifest，然后执行：

```bash
bash scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest "$TASK_MANIFEST"
bash scripts/multi_agent_launch.sh --manifest "$TASK_MANIFEST"
bash scripts/multi_agent_collect.sh --manifest "$TASK_MANIFEST"
```

### 3. Full-Auto Entry

需求驱动入口：

```bash
bash scripts/multi_agent_autoplan.sh --task <TASK_ID> --requirements-file docs/references/raw-requirements/<file>.md
```

端到端入口：

```bash
bash scripts/multi_agent_full_auto.sh --task <TASK_ID> --requirements-file docs/references/raw-requirements/<file>.md
```

`multi_agent_full_auto.sh` 固定做以下动作：

1. 执行 `foreman.py preflight`
2. 必要时 instantiate 目标 task
3. 生成 exec plan 与 manifest
4. 执行 prepare
5. 执行 launch
6. 等待 worker/explorer/validator 停止
7. 执行 collect
8. 启动 autonomous Main Foreman 收口

### 4. Prepare

执行：

```bash
bash scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest "$TASK_MANIFEST"
```

prepare 必须检查：

- 当前 task 已绑定 preflight
- 任务已 instantiate 且仍在 `tasks.md`
- manifest `task_id` 与当前任务一致
- manifest `mode` 属于 `semi-auto` 或 `full-auto`
- 若声明 `mcp_profiles` / `mcp_profile`，其结构、角色和 category 必须满足只读合同
- 当前无其他 active repo-side mainline 污染本轮
- ownership 不重叠
- worker 的 forbidden paths 覆盖治理禁区
- worktree 路径可创建或可复用
- 需要隔离的 worker 不复用主 worktree
- 目标 worktree 不会吸入无关脏改动

### 5. Launch

执行：

```bash
bash scripts/multi_agent_launch.sh --manifest "$TASK_MANIFEST"
```

可选：

```bash
bash scripts/multi_agent_launch.sh --manifest ... --dry-run
bash scripts/multi_agent_launch.sh --manifest ... --only benchmark-worker
```

launch 会：

- 按模板渲染 prompt
- 为每个 agent 生成可复跑的 launch script
- 调用 `codex exec -C <worktree> ...`
- 把 `mcp_profile`、source、allowed categories 和只读边界写入 runtime assignment
- 按 `agent.profile -> agent.mcp_profile` 顺序解析子会话 profile
- 记录 agent 名称、角色、worktree、日志路径、last-message 路径和 pid

### 6. Collect

执行：

```bash
bash scripts/multi_agent_collect.sh --manifest "$TASK_MANIFEST"
```

collect 会：

- 汇总每个 agent 的状态
- 读取各 worktree changed files
- 检查 ownership / forbidden path 越界
- 记录每个 agent 的 `mcp_profile`
- 拒收任何非 `explorer / validator` 的 MCP 越界
- 输出 `acceptable patches`、`rejected patches`、冲突列表和建议 Main Foreman 下一步动作

### 7. Fan-In

Main Foreman 按 collect 输出执行：

1. 接收 explorer 结论
2. 审核 worker changed files 和 last message
3. 拒收越界 patch
4. 手工或 autonomous fan-in 吸收可接收 patch
5. 统一更新任务文档与必要索引
6. 统一执行验证与 closeout

## Validation And Closeout

最终标准顺序固定为：

```bash
python3 scripts/foreman.py preflight
python3 scripts/foreman.py compile-governance
python3 scripts/validate_codex_runtime.py
bash scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest "$TASK_MANIFEST"
bash scripts/multi_agent_launch.sh --manifest "$TASK_MANIFEST"
bash scripts/multi_agent_collect.sh --manifest "$TASK_MANIFEST"
python3 scripts/foreman.py validate <TASK_ID>
python3 scripts/task_audit.py --check --phase pre-closeout
python3 scripts/foreman.py closeout <TASK_ID> --stage-path ...
python3 scripts/task_audit.py --check --phase post-closeout
```

任务级统一验证至少包含：

- 适用的 `mvn clean compile` 或模块编译
- 适用的 `mvn test`
- 适用的 `npm run build`
- 适用的 `npm run lint`
- `node scripts/lint-repository-knowledge.js`
- 必要时 runtime smoke / browser smoke
- `python3 scripts/foreman.py validate <TASK_ID>`

## Failure Handling And Rollback

### Ownership Conflict

- prepare 直接失败。
- Main Foreman 必须先重写 manifest，再重跑 prepare。

### Dirty Worktree

- prepare 直接失败。
- Main Foreman 必须清理或重建目标 worktree，不得在脏树上继续 launch。

### Worker Touched Forbidden Paths

- collect 标记为 reject。
- Main Foreman 不得直接吸收该 patch。

### Invalid MCP Profile Contract

- prepare 直接失败。
- Main Foreman 必须先修正 `mcp_profiles` / `mcp_profile`，再重跑 prepare。

### MCP Boundary Violation

- collect 标记为 reject。
- Main Foreman 必须移除错误角色的 `mcp_profile` 或回退 manifest，再重新 launch / collect。

### Agent Still Running

- collect 标记为 `running`。
- Main Foreman 可以继续等待，也可以只收已完成 agent 的结果。

### Autoplan Produced Bad Manifest

- prepare 直接失败或 collect 出现大面积 reject。
- Main Foreman 必须修正 prompt/requirements 或手工回退到 semi-auto manifest。

### Need To Abort Multi-Agent Mode

- 停止继续 launch 新 agent。
- 保留已生成日志与 collect summary 作为审计证据。
- 回到单 agent `foreman` 路径继续推进。

## When Not To Use This

- 单文件或单模块小改动
- 任务边界不清晰，ownership 无法稳定切分
- 需要频繁改同一文件
- 当前主工作树已经存在无法分离的脏改动
- 任务本身主要是 closeout / write-back / 台账修正

## SQLForge Demo Runbook

### Scenario

目标：为一个治理/工具任务自动生成多 agent 计划，并让 Main Foreman 在 `docs/scripts` 范围内驱动并行实现。

推荐角色：

- `truth-explorer`
- `governance-worker`
- `ops-worker`
- `validator`

### Requirements File Example

路径：

- `docs/references/raw-requirements/multi-agent-demo.md`

建议内容：

```md
# Requirement

为 SQLForge 新增一轮治理工具增强：
- 更新 docs/operations 和 docs/agent-prompts
- 新增一个 scripts 工具脚本
- 保持 Main Foreman 唯一收口
- worker 不得修改 tasks.md/tasks-done.md/INBOX.md/docs/quality/validation-log.md
- 最终仍走 foreman validate/task_audit/closeout
```

### Full-Auto Command Sequence

```bash
python3 scripts/foreman.py preflight --task HARN-026 --task-class standard --prompt "Requirement-driven full-auto multi-agent orchestration demo."
bash scripts/multi_agent_autoplan.sh --task HARN-026 --requirements-file docs/references/raw-requirements/multi-agent-demo.md
bash scripts/multi_agent_prepare.sh --task HARN-026 --manifest docs/exec-plans/active/HARN-026-multi-agent-run.json
bash scripts/multi_agent_launch.sh --manifest docs/exec-plans/active/HARN-026-multi-agent-run.json --dry-run
bash scripts/multi_agent_full_auto.sh --task HARN-026 --requirements-file docs/references/raw-requirements/multi-agent-demo.md --stop-after collect
```

### Expected Generated Artifacts

- `docs/exec-plans/active/HARN-026-full-auto-execution-plan.md`
- `docs/exec-plans/active/HARN-026-multi-agent-run.json`
- `.codex/state/multi-agent/HARN-026/requirements.md`
- `.codex/state/multi-agent/HARN-026/collect-summary.md`

### Demo Fan-In Rule

- Main Foreman 只接收 collect 标记为 acceptable 的 patch。
- `governance-worker` 与 `ops-worker` 必须拥有不重叠的 write scope。
- `validator` 只输出验证建议，不直接改代码。
- 最终仍由 Main Foreman 统一执行 validate、audit 和 closeout。
