治理需求：在 A-STORY-007 下新增 formal task HARN-036，用于修复 governed intake / full-auto 入口的 P0/P1 关键缺口。
目标范围：
1. 修 existing-task 路径：scripts/governed_intake.sh --task 不能直接把 --task 丢给 multi_agent_full_auto.sh；必须先生成 requirements artifact，并把 输出物/限制 一并向下传递。
2. 把治理硬门禁写进 scripts/multi_agent_full_auto.sh，至少补 python3 scripts/foreman.py compile-governance 和 python3 scripts/validate_codex_runtime.py，不能只靠 prompt 约束。
3. 定义统一 execution preview，固定包含：任务类型、existing-task/no-task、single-agent/multi-agent-full-auto、candidate/formal task id、输出物、限制条件、planned agents、MCP 是否启用及给谁启用、validation/closeout 路径、风险、等待确认语句/confirm-run 标识。
4. 把模板入口做成 chat-native router，接到 UserPromptSubmit 或 repo skill/plugin 路径，识别 需求/治理需求/实现任务/实现治理任务/确认没问题开始执行 等模板，自动调用 codex_template_adapter.py 与 governed_intake.sh，并记住 run_id，在确认后再执行 --confirm-run。
5. 增加 execution mode router：简单任务走 single-agent foreman；跨模块且 ownership 可切再走 multi-agent；边界不清只出 preview 不执行。
治理边界：Main Foreman 仍是唯一 write-back/validate/closeout 入口；不要削弱 tasks.md/tasks-done.md/INBOX.md/validation-log 审计链；不要把这批修复默认做成 multi-agent 执行。
建议执行模式：single-agent。
输出物：脚本、hook/adapter/runtime router、执行预览 contract、文档与规则对齐、自动化验证。
限制：严格模式；不丢需求；保留现有只读 MCP 边界；不要把可写 MCP、SSH、K8s、数据库执行型能力接入主路径。
