## Candidate Execution Plan for `HARN-036`

### Story Placement
- Story: `A-STORY-007`
- Rationale: 本需求集中在 governed intake、full-auto 编排、requirements artifact、execution preview、router 分流与 Foreman 治理链强化，属于“从无 task 开始的治理自动化”范围内的治理入口与执行编排补强。

### Shaping Intent
- 以单一 formal task 覆盖 governed intake / full-auto 主路径的高优先级治理缺口修复。
- 保持 Main Foreman 为唯一 write-back / validate / closeout 入口。
- 将 prompt 级约束下沉为脚本级硬门禁，并把 preview / router / artifact contract 纳入统一审计语义。

### Proposed Execution Slices
1. Requirements Artifact Gate
- 收敛 existing-task 路径的前置要求：任何 existing-task 执行前，先形成 requirements artifact。
- 明确 artifact 必须向下传递的最小 contract：输出物、限制条件、task 身份、执行模式候选、风险与确认要求。
- 禁止 `scripts/governed_intake.sh --task` 直接透传进入 full-auto 主执行而绕过 requirements 编译。

2. Governance Hard Gates
- 在 `scripts/multi_agent_full_auto.sh` 前置固化治理编译与运行时校验。
- 候选 task pack 应要求把 `python3 scripts/foreman.py compile-governance` 与 `python3 scripts/validate_codex_runtime.py` 类步骤提升为主路径硬门禁。
- 任何门禁失败都应阻断执行，并保留可审计失败信号，不能退化为 prompt 提示。

3. Unified Execution Preview Contract
- 定义统一 execution preview 作为执行前唯一标准确认界面。
- preview 固定包含：任务类型、existing-task/no-task、single-agent/multi-agent-full-auto、candidate/formal task id、输出物、限制条件、planned agents、MCP 是否启用及分配对象、validation/closeout 路径、风险、等待确认语句与 `confirm-run` 标识。
- preview 先于执行产生，并作为 router 与后续执行链的共享 contract。

4. Chat-Native Intake Router
- 将模板入口塑形为 chat-native router，而不是零散 adapter 直连。
- router 负责识别模板类型，并自动串联 `codex_template_adapter.py` 与 `governed_intake.sh`。
- router 负责保存 `run_id`，且未收到明确确认前不得触发 `--confirm-run`。

5. Execution Mode Router
- 引入执行模式路由层，但默认建议 single-agent。
- simple / 边界清晰任务路由到 single-agent foreman。
- 仅当跨模块且 ownership 可切分时，允许显式进入 multi-agent。
- 边界不清、ownership 不稳或治理信息不足时，只生成 preview，不执行。

6. Documentation and Validation Alignment
- 将脚本、adapter/router、preview contract、规则文档与验证要求对齐成单一交付面。
- 候选 task pack 应显式覆盖自动化验证与治理文档更新，但不得绕过 instantiate / validate / closeout 标准链。
- 交付验收必须落回 SQLForge 审计链，而不是新增旁路。

### Governance Sequence Constraints
- 先进入 formal task shaping，再经 `instantiate` 落台账。
- 实施后必须经 `validate` 验证治理门禁、preview contract 与 router 行为。
- 仅在满足治理要求后进入 `closeout`，不得把“turn stop”视为任务完成。
- 不得弱化 `tasks.md` / `tasks-done.md` / `INBOX.md` / `docs/quality/validation-log.md` 的现有审计链。

### Candidate Acceptance Focus
- existing-task 路径不再绕过 requirements artifact。
- full-auto 主路径具备脚本级硬门禁。
- preview contract 字段完整、稳定、可审计。
- chat-native router 与 execution mode router 的职责边界清晰。
- single-agent 为默认建议，multi-agent 仅条件满足时启用。
- 只读 MCP 边界保持不变，未引入写能力或外部执行面。
