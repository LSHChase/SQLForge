# Semi-Auto Multi-Agent Playbook

## Purpose

本手册定义 SQLForge 的半自动多 agent 协作基础设施。目标是在不引入第二套长期真值、不依赖隐式 subagent、不破坏既有 `foreman` / `task_audit` / `closeout` 审计链的前提下，让 Main Foreman 可以通过多个 `codex exec` 会话和多个 `git worktree` 并行推进复杂任务。

本手册只适用于跨模块、可明确切 ownership、值得并行化的复杂任务。简单任务继续使用单 `codex` + `foreman` 工作流。

## Core Model

- Main Foreman 是唯一收口点。
- 多 agent 的默认技术载体是多个 `codex exec` 会话，不依赖隐式 subagent。
- 可写 agent 使用独立 `git worktree`；只读 explorer 可复用主 worktree。
- 整轮协作由一个 manifest 驱动。
- prompt 通过 `docs/agent-prompts/*.md` 模板化，不依赖每轮手工拼 prompt。
- worker 只交变更文件列表、实现摘要、已执行验证、residual risk；最终 fan-in、validate、audit、closeout 都由 Main Foreman 完成。

## Roles

### Main Foreman

- 负责 task shaping、manifest 审核、ownership 分配、冲突处理、fan-in、最终验证、台账更新和 closeout。
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

### Explorer

- 只读，不改文件。
- 输出事实核对、边界盘点、验证建议或 residual risk。
- 默认可复用主 worktree。

### Worker

- 只在 manifest 指定的 ownership 内改动。
- 不得修改台账、validation log 或 closeout 文档。
- 不得自行 closeout、归档、宣告任务单独完成。

### Validator

- 默认只验证，不主动改实现。
- 如验证暴露实现问题，只输出失败点、证据和建议，不直接修代码，除非 Main Foreman 重新分派为 worker。

## Non-Negotiable Rules

- worker 必须有明确 ownership。
- 两个 worker 的 ownership 不允许重叠。
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
- `.codex/` 下的多 agent 运行态文件仅是 runtime artifact，不是长期真值。

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
- `agents`
- `name`
- `role`
- `worktree`
- `prompt_file`
- `ownership`
- `forbidden_paths`
- `validation_scope`

当前模板额外支持：

- `run_root`
- `codex.sandbox`
- `codex.full_auto`
- `codex.color`
- `codex.extra_args`
- per-agent `model`
- per-agent `profile`
- per-agent `extra_args`
- per-agent `notes`

运行态输出目录默认是：

- `.codex/state/multi-agent/<TASK_ID>/`

其中会生成：

- rendered prompts
- launch scripts
- per-agent stdout/stderr logs
- per-agent last-message files
- collect summary

## Workflow

### 1. Shape The Task

1. `python3 scripts/foreman.py preflight`
2. 确认任务已进入主计划、task-spec matrix、task-governance matrix。
3. `python3 scripts/foreman.py instantiate <TASK_ID>`
4. Main Foreman 编写 active manifest，并为每个 agent 划定 ownership / forbidden paths / validation scope。

### 2. Prepare

执行：

```bash
bash scripts/multi_agent_prepare.sh --task <TASK_ID> --manifest "$TASK_MANIFEST"
```

prepare 必须检查：

- 当前 task 已绑定 preflight
- 任务已 instantiate 且仍在 `tasks.md`
- manifest `task_id` 与当前任务一致
- 当前无其他 active repo-side mainline 污染本轮
- ownership 不重叠
- worker 的 forbidden paths 覆盖治理禁区
- worktree 路径可创建或可复用
- 需要隔离的 worker 不复用主 worktree
- 目标 worktree 不会吸入无关脏改动

### 3. Launch

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
- 记录 agent 名称、角色、worktree、日志路径、last-message 路径和 pid

### 4. Collect

执行：

```bash
bash scripts/multi_agent_collect.sh --manifest "$TASK_MANIFEST"
```

collect 会：

- 汇总每个 agent 的状态
- 读取各 worktree changed files
- 检查 ownership / forbidden path 越界
- 输出 `acceptable patches`、`rejected patches`、冲突列表和建议 Main Foreman 下一步动作

### 5. Fan-In

Main Foreman 按 collect 输出执行：

1. 接收 explorer 结论
2. 审核 worker changed files 和 last message
3. 拒收越界 patch
4. 手工集成可接收 patch
5. 统一更新任务文档与必要索引
6. 统一执行验证与 closeout

## Validation And Closeout

最终标准顺序固定为：

```bash
python3 scripts/foreman.py preflight
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

### Agent Still Running

- collect 标记为 `running`。
- Main Foreman 可以继续等待，也可以只收已完成 agent 的结果。

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

目标：为一个跨 `benchmark-engine` / `governance` 的治理型任务并行安排 3 个角色：

- `benchmark-worker`
- `governance-worker`
- `validator`

### Example Manifest

路径：

- `docs/exec-plans/active/HARN-025-multi-agent-run.json`

示例结构：

```json
{
  "task_id": "HARN-025",
  "mode": "semi-auto",
  "main_worktree": ".",
  "run_root": ".codex/state/multi-agent/HARN-025-demo",
  "codex": {
    "sandbox": "workspace-write",
    "full_auto": true,
    "color": "never",
    "extra_args": []
  },
  "agents": [
    {
      "name": "benchmark-worker",
      "role": "worker",
      "worktree": "../sqlforge-harn025-benchmark",
      "prompt_file": "docs/agent-prompts/benchmark-worker.md",
      "ownership": ["benchmark-engine/**"],
      "forbidden_paths": ["tasks.md", "tasks-done.md", "INBOX.md", "docs/quality/validation-log.md"],
      "validation_scope": ["benchmark-engine module tests", "changed-file self-check"]
    },
    {
      "name": "governance-worker",
      "role": "worker",
      "worktree": "../sqlforge-harn025-governance",
      "prompt_file": "docs/agent-prompts/governance-worker.md",
      "ownership": ["docs/operations/**", "docs/agent-prompts/**", "docs/exec-plans/templates/**", "scripts/multi_agent_*.sh"],
      "forbidden_paths": ["tasks.md", "tasks-done.md", "INBOX.md", "docs/quality/validation-log.md"],
      "validation_scope": ["docs consistency", "script help/dry-run"]
    },
    {
      "name": "validator",
      "role": "validator",
      "worktree": ".",
      "prompt_file": "docs/agent-prompts/validator.md",
      "ownership": [],
      "forbidden_paths": ["tasks.md", "tasks-done.md", "INBOX.md", "docs/quality/validation-log.md"],
      "validation_scope": ["prepare/launch/collect usage", "risk review"]
    }
  ]
}
```

### Demo Command Sequence

```bash
python3 scripts/foreman.py preflight --task HARN-025 --task-class standard --prompt "Land the semi-automated multi-agent collaboration foundation under SQLForge governance."
bash scripts/multi_agent_prepare.sh --task HARN-025 --manifest docs/exec-plans/active/HARN-025-multi-agent-run.json
bash scripts/multi_agent_launch.sh --manifest docs/exec-plans/active/HARN-025-multi-agent-run.json --dry-run
bash scripts/multi_agent_launch.sh --manifest docs/exec-plans/active/HARN-025-multi-agent-run.json
bash scripts/multi_agent_collect.sh --manifest docs/exec-plans/active/HARN-025-multi-agent-run.json
```

### Demo Fan-In Rule

- Main Foreman 只接收 collect 标记为 acceptable 的 patch。
- 对 `benchmark-worker` 和 `governance-worker` 的结果分开审查。
- `validator` 只输出验证建议，不直接改代码。
- 最终仍由 Main Foreman 统一执行 validate、audit 和 closeout。
