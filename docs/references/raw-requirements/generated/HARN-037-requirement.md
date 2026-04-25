治理需求：在 A-STORY-008 下新增 formal task HARN-037，用于把现有只读 MCP 治理从“有规则”推进到“可落地可诊断可上手”。
目标范围：
1. 补 MCP onboarding/doctor：为每个允许 category 提供本地接入说明，补充 mcp doctor / healthcheck，明确 evidence 写回位置，但仍不把 secret、token、endpoint、server inventory 落仓。
2. 明确产品定位：不是远端自动运维，不是可写控制面，而是受治理的只读证据增强。
3. 对齐 docs/security/connectors.md、docs/operations/codex-mcp-playbook.md、docs/operations/multi-agent-playbook.md、docs/README.md、docs/operations/README.md 与 validate/compile/runtime 相关入口，让单 agent 本地 MCP 与 multi-agent mcp_profile 只读边界说明一致。
4. doctor/healthcheck 应覆盖：允许 category 是否声明完整、本地 prerequisites 是否清晰、evidence 写回目标是否明确、是否误引入可写 MCP / SSH / K8s / 数据库执行型 server、是否试图把 secret 或 live inventory 落仓。
治理边界：保留 HARN-034/HARN-035 的只读 MCP 基线；explorer/validator 才能在 multi-agent 下使用 manifest-level mcp_profile；Main Foreman 仍是唯一 write-back/validate/closeout 入口。
输出物：文档、doctor/healthcheck 脚本或扩展、自动化验证、使用手册。
限制：严格模式；不要引入可写 MCP；不要把远端操作包装成 doctor；不要新建第二套长期真值。
