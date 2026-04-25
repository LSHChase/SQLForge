治理需求：在 MCP 治理底座落地后，再为 multi-agent 增加受控 `mcp_profile`，让 explorer/validator 能读取外部只读证据，但 Main Foreman 仍是唯一 write-back / validate / closeout 入口。

范围：
- 更新 multi-agent playbook
- 更新相关 agent prompt 模板
- 扩展 multi-agent manifest 模板
- 扩展 prepare / launch / collect / full-auto 脚本的 `mcp_profile` 契约

约束：
- 依赖 `HARN-034`
- 只允许 explorer / validator 读取外部只读证据
- worker 默认不得通过 MCP 修改治理台账、validation log、closeout 文档或业务数据
- 不新增可写 MCP 默认主路径

输出物：
- 文档
- 模板
- 脚本
- manifest 契约
- Codex multi-agent MCP 使用手册
