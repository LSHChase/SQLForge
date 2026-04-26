# Requirements-To-Task Playbook

## Purpose

本手册定义 SQLForge 从“只有规划/需求/计划、还没有正式 task”开始的治理自动化路径。它补齐 `HARN-026` 之前的一段上游流程：

- requirement normalization
- candidate execution plan shaping
- candidate task pack generation
- governance gate review
- formal materialization into plan/matrix/ledger
- instantiate
- handoff to existing `multi_agent_full_auto.sh`

本手册的目标不是绕过治理，而是把“无 task”场景也纳入现有 `foreman` / `task_audit` / `closeout` 审计链。

`HARN-028` 之后，日常入口优先使用：

- `bash scripts/governed_intake.sh --prompt "<TEXT>"`
- `bash scripts/governed_intake.sh --requirements-file <FILE>`
- `bash scripts/governed_intake.sh --task <TASK_ID>`

该入口会先生成 brief / machine-readable summary，等待显式 `--confirm-run`，而不是在第一次输入后立即落仓执行。

## Codex 日常输入模板

面向新手的 Codex 入口优先使用自然语言模板。Codex 收到模板后，只能先生成执行/任务模板给人类确认；在收到确认语句前，不得进入 materialization、编码实现或台账落仓。

### 最简业务功能输入模板

适用于“已有功能需求，想直接做实现”。

```text
需求：<一句话功能需求>
输出物：代码 / 测试 / 文档
先生成执行模板给我确认，不要直接执行。
```

如果已有任务号，进一步简化为：

```text
实现任务：<TASK_ID>
输出物：代码 / 测试 / 文档
先生成执行模板给我确认，不要直接执行。
```

### 最简治理任务输入模板

适用于“脚本、流程、文档、编排、质量门禁、治理能力”这类任务。

```text
治理需求：<一句话治理目标>
输出物：脚本 / 文档 / 模板
先生成任务模板给我确认，不要直接执行。
```

如果已有任务号：

```text
实现治理任务：<TASK_ID>
输出物：脚本 / 文档 / 模板
先生成执行模板给我确认，不要直接执行。
```

### 确认后执行模板

Codex 给出模板后，人类可以回复：

```text
确认没问题，开始执行。
```

如果要加限制：

```text
确认没问题，开始执行。
限制：不要改前端；不要动数据库；优先补测试。
```

自然语言确认句等价于进入 governed confirmation gate；Codex 仍必须遵守 `preflight`、`governed_healthcheck`、`instantiate`、`validate`、`task_audit`、`closeout`，不得把确认句解释为跳过仓库治理。`限制：...` 必须写入 candidate pack / execution plan 的 constraints、out-of-scope、validation focus 或 human confirmation point，保证后续实现和验证可追溯。

若需要把自然语言模板先转成机器可读入口，使用：

```bash
python3 scripts/codex_template_adapter.py --template-text '<TEMPLATE>'
```

该 adapter 只生成 `template-adapter-summary.json` 与推荐的 `governed_intake.sh` 命令；加 `--execute` 才会实际调用 intake。`HARN-033` 之后，adapter 输出稳定 `schema_version=2`，支持中文/英文冒号、多行需求正文、业务/治理/已有任务/已有治理任务四类模板。

若希望把这条入口收敛成 chat-native router，使用：

```bash
python3 scripts/chat_native_router.py --prompt-text '<TEMPLATE>' --json
```

`HARN-036` 之后，`chat_native_router.py` 与 `UserPromptSubmit` hook 会优先识别：

- `需求：...`
- `治理需求：...`
- `实现任务：...`
- `实现治理任务：...`
- `确认没问题，开始执行。`

router 会自动记住 `run_id`，先调用 `codex_template_adapter.py` 与 `governed_intake.sh` 生成统一 execution preview；只有收到显式确认后，才会进入 `--confirm-run`。若确认句带 `限制：...`，约束会并入 intake summary、execution preview 与 requirements artifact。

子 `codex exec` 的默认执行模式是 `SQLFORGE_CODEX_EXEC_MODE=bypass`。这意味着 task-shaping、governance review 和 downstream auto-foreman 会假设父级自动化已经由可信外部环境托管沙箱。如果需要强制子会话使用 Codex 自带沙箱，可改成 `full-auto`、`read-only`、`workspace-write` 或 `danger-full-access`。

为了避免 candidate task id 污染主运行态，`requirements_to_plan.sh` 和 `task_materialize.sh` 内部用于 shaping/review 的子 `codex exec` 会默认禁用 `codex_hooks`。正式进入 materialization 之后，仍由 Main Foreman 重新执行标准 `preflight` / `instantiate` / `validate` / `closeout` 链。

`HARN-041` 之后，candidate reservation 额外支持以下审计状态：

- `paused`
  - 候选包与 shaping 证据继续保留，但不得继续 `confirm-run` 或 `task_materialize`
  - 仅当 authority 边界未变化、candidate pack 仍然有效时，才允许 `--resume-candidate`
- `archived`
  - 保留为历史候选/模板证据，不再视为可直接恢复的 active candidate
  - 若需求要重新进入，必须重新走 governed intake/shaping，生成新的 candidate 包
- `abandoned`
  - 明确放弃该候选，保留历史证据但不再继续当前 candidate id 的实现路径
  - 若需求重启，同样必须重新塑形

上述状态变更统一通过 `python3 scripts/governed_runtime_dashboard.py` 执行，不得手工删除 `.codex/state/task-id-reservations/*.json` 掩盖问题。

## Core Model

- 从无 task 开始的自动化分为两段：
  - `task-shaping`: 只生成候选产物，不直接编码
  - `task-execution`: 只有正式 task materialize 并 instantiate 后才允许进入
- `task-shaping` 的运行态文件只落在 `.codex/state/task-shaping/<RUN_ID>/`
- 只有通过治理 gate 的 candidate task pack，才能写入：
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/task-spec-matrix.md`
  - `docs/plans/task-governance-extension-matrix.md`
  - `tasks.md`
  - `docs/exec-plans/active/<TASK_ID>-full-auto-execution-plan.md`
- Main Foreman 仍是唯一最终收口点

## Roles

### Requirement Normalizer

- 把原始需求整理成标准化输入
- 输出目标、约束、验收、范围外项和推荐 task class
- 不直接写台账

### Plan Shaper

- 把标准化需求映射到现有 master plan 的 phase/story/task 边界
- 选择 story id 并生成 candidate execution plan
- 不直接 materialize task

### Task Shaper

- 在固定 candidate task id 下生成 candidate task pack
- 补齐 task-spec matrix 和 governance extension matrix 所需字段
- 不直接修改 ledger

### Task Governance Reviewer

- 只读审查 candidate task pack
- 判断是否满足 SQLForge 现有治理门禁
- 输出 `go` / `no-go` 与 blocker 列表

### Main Foreman

- 唯一允许执行 formal materialization、instantiate、validate、audit 和 closeout
- 唯一允许修改 `tasks.md` / `tasks-done.md` / `INBOX.md` / `docs/quality/validation-log.md`

## Runtime Artifacts

默认 run root：

- `.codex/state/task-shaping/<RUN_ID>/`

其中会生成：

- `raw-requirement.md`
- `normalized-requirements.json`
- `normalized-requirements.md`
- `candidate-execution-plan.md`
- `candidate-task-pack.json`
- `governance-review.json`
- `run-summary.json`
- `materialization-summary.json`
- rendered prompts
- per-step schema/log/result files

此外，`governed_intake.sh` 会在 `.codex/state/intake/<RUN_ID>/` 下生成：

- `intake-summary.json`
- `execution-preview.json`
- `execution-preview.md`
- `requirements-artifact.md`（`existing-task` 路径强制生成；`no-task-shaping` 路径在 preview 中先引用 `raw-requirement.md`）
- `healthcheck-summary.json`（若调用 `python3 scripts/governed_healthcheck.py --check`）
- `template-adapter-summary.json`（若从 template adapter / chat-native router 进入）

这些文件是运行态证据，不是长期 authority。

## Machine-Readable Summary Contract

关键 summary 文件至少包含：

- `executed_commands`
- `suggestions`
- `authority_fields_to_confirm`
- `suggested_integrity_checks`

说明：

- `executed_commands` 用于审计本轮自动化实际调用了哪些命令。
- `authority_fields_to_confirm` 用于把“要确认哪些权限/权威字段”结构化暴露给人类。
- `suggested_integrity_checks` 用于把建议重跑的完整性检查命令写回 summary，而不是只留在终端文本里。

## Unified Execution Preview Contract

`HARN-036` 之后，intake preview 的固定字段至少包含：

- `task_type`
- `path_selected`
- `execution_mode`
- `formal_task_id`
- `candidate_task_id`
- `outputs`
- `constraints`
- `planned_agents`
- `mcp`
- `requirements_artifact_ref`
- `validation_path`
- `closeout_path`
- `risks`
- `confirm_run_command`
- `wait_for_confirmation_text`

execution preview 是确认前唯一标准界面；router、`governed_intake.sh` 与 downstream routing 都必须复用它，而不是各自输出不同格式的人类摘要。

## Execution Mode Router

`HARN-036` 之后，`governed_intake.sh` 会先决定 execution mode，再允许确认执行：

- `single-agent`
  - 默认路径，尤其适用于治理/脚本/文档类任务和 ownership 不值得切分的任务。
  - `existing-task` 确认后不再强制进入 `multi_agent_full_auto.sh`；而是绑定 Main Foreman 当前会话，继续单 agent 实施。
- `multi-agent-full-auto`
  - 只在 routed input 明确要求 multi-agent，或边界明确可并行切 ownership 时启用。
  - 进入该路径前，必须已有 requirements artifact。
- `preview-only`
  - 当 ownership / scope / routing signal 仍不清晰时，只生成 preview，不自动 materialize 或启动 execution。

## Candidate Task Pack Contract

模板：

- `docs/exec-plans/templates/candidate-task-pack.template.json`

最低字段：

- `task_id`
- `title`
- `task_class`
- `priority`
- `story_id`
- `story_title`
- `depends_on`
- `scope`
- `adr_refs`
- `rule_refs`
- `context_aliases`
- `contract`
- `tech`
- `layer`
- `tests`
- `env`
- `human_confirmation_point`
- `requires_human_decision`
- `data_impact`
- `rollback_recovery`
- `task_summary`
- `residual_risk`
- `materialization_ready`

约束：

- `materialization_ready` 默认由 `task_materialize.sh` 在 gate 通过后置为 `true`
- `requires_human_decision = true` 时，必须停止，不得自动 materialize
- `story_id` 必须指向 `master-execution-plan.md` 中已存在的 story
- `task_id` 在 materialize 前不得已存在于 plan/matrix/ledger

## Workflow

### 1. Preferred Intake Entry

执行：

```bash
bash scripts/governed_intake.sh --prompt "<TEXT>"
```

或：

```bash
bash scripts/governed_intake.sh --requirements-file <FILE>
```

或对已有 formal task：

```bash
bash scripts/governed_intake.sh --task <TASK_ID>
```

该步骤会：

1. 选择 `existing-task` 或 `no-task-shaping` 路径
2. 生成 `intake-summary.json`、`execution-preview.json`、`execution-preview.md`
3. 对 `existing-task` 路径强制先生成 `requirements-artifact.md`，不得直接把 `--task` 透传给 `multi_agent_full_auto.sh`
4. 决定 `single-agent` / `multi-agent-full-auto` / `preview-only`
5. 保留 `confirmation_state=awaiting-confirmation`
6. 要求显式执行 `--confirm-run <RUN_ID>`
7. 真实 `--confirm-run` 会先运行 `python3 scripts/governed_healthcheck.py --check`；若存在 tracked dirty、stale/conflict reservation 或 closeout tail drift，会直接停止

确认执行：

```bash
bash scripts/governed_intake.sh --confirm-run <RUN_ID>
```

带追加约束的确认：

```bash
bash scripts/governed_intake.sh --confirm-run <RUN_ID> --constraints "优先补测试；不要引入可写 MCP。"
```

### 2. Generate Candidate Artifacts Directly

执行：

```bash
bash scripts/requirements_to_plan.sh --requirements-file <FILE>
```

或：

```bash
bash scripts/requirements_to_plan.sh --prompt "<TEXT>"
```

该步骤会：

1. 规范化需求
2. 生成 candidate execution plan
3. 生成 candidate task pack

### 3. Governance Gate And Materialization

执行：

```bash
bash scripts/task_materialize.sh --task-pack .codex/state/task-shaping/<RUN_ID>/candidate-task-pack.json
```

该步骤会：

1. 校验 candidate task pack 结构
2. 审查 task id 是否冲突
3. 审查是否触发人工确认点
4. 调用 `task-governance-reviewer`
5. 对 candidate task 做 `foreman.py preflight`
6. 写入 plan/matrix/coverage/raw-requirement/final exec plan
7. 再次 `preflight`
8. `instantiate`

### 4. End-To-End Governed Full Cycle

执行：

```bash
bash scripts/governed_full_cycle.sh --requirements-file <FILE>
```

固定顺序：

1. `requirements_to_plan`
2. `task_materialize`
3. 根据 execution mode 进入：
   - `single-agent` Main Foreman continuation
   - 或 `multi_agent_full_auto.sh --task <TASK_ID> --requirements-file <ARTIFACT>`

## Stop Points

`governed_full_cycle.sh` 支持：

- `--stop-after normalize`
- `--stop-after plan`
- `--stop-after materialize`
- `--stop-after collect`
- `--stop-after closeout`

说明：

- `normalize`: 只产出标准化需求
- `plan`: 产出标准化需求与 candidate execution plan
- `materialize`: 正式建 task，但不进入 execution
- `collect`: 执行到 downstream full-auto collect
- `closeout`: 端到端执行到最终 closeout

推荐：

- 在当前 SQLForge 自动化环境中，直接使用默认值即可，不需要先退出或重启 Codex。
- 如果子会话出现 `bwrap` / `workspace-write` 相关错误，优先检查 `SQLFORGE_CODEX_EXEC_MODE` 是否被外部环境覆盖。

## Governance Gate Rules

以下情况必须阻断自动 materialization：

- `task_id` 已存在于 plan/matrix/ledger
- `story_id` 不存在
- candidate task pack 缺字段
- `requires_human_decision = true`
- reviewer 返回 `no-go`
- 试图绕过 `preflight` / `instantiate`
- 试图直接让 worker 修改 ledger 或 validation log

## Healthcheck

执行：

```bash
python3 scripts/governed_healthcheck.py --check
```

可选附带 candidate task pack：

```bash
python3 scripts/governed_healthcheck.py --check \
  --task-pack .codex/state/task-shaping/<RUN_ID>/candidate-task-pack.json
```

用途：

- 发现 reservation 冲突
- 发现 `current-task` 与 ledger 不一致
- 发现 `validation-log` closeout tail drift
- 发现 candidate task pack 缺字段或仍卡在 human-confirmation gate

`HARN-041` 之后，healthcheck 对 candidate reservation 的语义如下：

- `reserved` / `candidate_ready` / `blocked` / `failed` / `materializing`
  - 仍视为 active candidate reservation；stale 且无 repo truth 时会继续阻断 confirm-run，直到显式 cleanup 或状态决策完成
- `paused` / `archived` / `abandoned`
  - 视为已明确治理决策，不再触发 `reservation_conflict`
- `released`
  - 若只是旧 dry-run/cleanup 残留，也不再直接阻断 healthcheck；但应进一步归档为历史候选证据，而不是继续保留为可恢复 candidate

若 healthcheck 失败，summary 会给出：

- `authority_fields_to_confirm`
- `suggested_integrity_checks`

## Demo Runbook

### Demo Input

```bash
bash scripts/governed_intake.sh \
  --prompt "Create a governance/tooling task that standardizes a new repository verification helper under existing SQLForge audit rules."
```

### Demo Review

```bash
bash scripts/governed_intake.sh --confirm-run <RUN_ID> --dry-run
```

### Demo End-To-End Dry-Run

```bash
python3 scripts/governed_healthcheck.py --check
```

## Failure Handling

### Candidate Task Id Conflict

- `task_materialize.sh` 直接失败
- 重新运行 `requirements_to_plan.sh` 生成新的 candidate task id

### Candidate Pause / Resume / Re-entry

暂停候选：

```bash
python3 scripts/governed_runtime_dashboard.py \
  --pause-candidate <TASK_ID> \
  --reason "<WHY>"
```

归档候选模板：

```bash
python3 scripts/governed_runtime_dashboard.py \
  --archive-candidate <TASK_ID> \
  --reason "<WHY>"
```

明确放弃候选：

```bash
python3 scripts/governed_runtime_dashboard.py \
  --abandon-candidate <TASK_ID> \
  --reason "<WHY>"
```

恢复暂停候选：

```bash
python3 scripts/governed_runtime_dashboard.py \
  --resume-candidate <TASK_ID> \
  --reason "<WHY>"
```

规则：

- 只有 `paused` candidate 可以原地恢复到 `candidate_ready`
- `archived` / `abandoned` / released dry-run candidate 一律视为“保留证据但必须重新塑形”
- candidate 一旦被暂停/归档/放弃，`task_materialize.sh` 不得继续把它 materialize 成正式 task
- HARN-040 这类“保留后续恢复入口但当前不继续 confirm-run”的候选，应使用 `paused`，而不是继续维持 `candidate_ready`
- HARN-029 / HARN-030 这类 dry-run shaping 证据，应归档保留，不应继续充当 active reservation

### Human Decision Required

- 输出 blocker
- 停止，不写 ledger

### Reviewer Returned No-Go

- 输出 blocker 和建议
- 停止，不写 ledger

### Materialization Succeeded, Downstream Full-Auto Failed

- 保留 formal task、exec plan 和 task-shaping 运行态证据
- 回到 Main Foreman 手工或半自动路径继续推进

### Closeout Tail Drift

- 先执行 `python3 scripts/governed_healthcheck.py --check`
- 若提示 `closeout_tail_drift`，先修正 tracked residue，再继续新的 closeout
- `HARN-031` 之后，closeout 证据采用 precommit projected log + post-commit actual audit/check 的组合；projected 证据只能表示“预期将在 commit 后执行”，不得在真实 post-closeout 执行前写成 `passed`
- `HARN-032` 之后，真实 post-closeout audit/check 结果写入 `.codex/state/closeout/<TASK_ID>/post-closeout-actual.json`，记录 commit sha、命令、exit code、stdout/stderr 摘要与最终状态；该文件是 runtime evidence，不写入 tracked commit
- `HARN-033` 之后，`governed_healthcheck.py` 会汇总近期 closeout actual evidence；`final_status` 非 `passed` 的 HARN-032 之后 evidence 会成为 blocker，旧任务缺失 runtime evidence 只作为可见状态记录
- `HARN-038` 之后，如果要把 `python3 scripts/governed_healthcheck.py --check` 用作 closeout 的 post-check，必须带 `--post-closeout-task <TASK_ID>` 上下文；通过 `python3 scripts/foreman.py closeout ... --post-check "python3 scripts/governed_healthcheck.py --check"` 进入时，Foreman 会自动补上该上下文，避免对当前刚归档任务的 own actual evidence 产生自引用误判
- `HARN-038` 之后，若 commit 已成功但 post-closeout check 失败，标准恢复入口是 `python3 scripts/foreman.py closeout-repair <TASK_ID>`；它会复用失败 evidence 中记录的 `post_check_commands` 重跑 post-closeout actual checks，并在成功后恢复 runtime 到可继续执行的新任务状态；不得再手工拼接 JSON 修复 `.codex/state`
- `python3 scripts/governed_healthcheck.py --check --cleanup-dry-run` 会预览 stale reservation 与未关联 intake/task-shaping runtime 证据；存在 preview 且无 blocker 时，`final_outcome=cleanup_preview_found`
- `python3 scripts/governed_runtime_dashboard.py --json` 汇总 intake、reservation、task-shaping、multi-agent、closeout evidence；默认只预览，只有显式 `--release-stale-reservations` 或 `--archive-reviewed-runtime-evidence` 才会改变 `.codex/state`
