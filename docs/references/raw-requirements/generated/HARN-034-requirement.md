治理需求：为 SQLForge 落地最小可用 MCP 治理底座与第一批只读 MCP 接入边界。

范围：
- 先补 `docs/security/connectors.md`
- 追加 MCP 规则和验证规则
- 扩展 `compile-governance` 与 `validate_codex_runtime`
- 新增 Codex MCP 使用手册与本地使用入口说明

约束：
- 不得引入第二套长期真值
- 不得绕过 `foreman` / `task_audit` / `closeout`
- 第一批只允许观测/日志、部署证据、对象存储元数据、外部需求/工单检索这类只读 MCP
- 不要把 multi-agent `mcp_profile` 扩展混入本 task

输出物：
- 文档
- 规则
- 验证规则
- 脚本
- 配置/编译产物
- 自动化流程
- Codex 手册
