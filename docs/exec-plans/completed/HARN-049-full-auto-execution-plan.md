## Candidate Execution Plan: HARN-049

### Story Landing
- Story: `E-STORY-008` | Phase-E | 解析工作台与批量解析中心
- Task class: `standard`
- Scope: 解析工作台 UX、批量解析页面、解析历史查询页面、解析结果展示治理、历史持久化与查询查看链路、测试与文档。

### Governance Sequence
1. Main Foreman 执行 `python3 scripts/foreman.py preflight` 建立上下文。
2. task-shaper 基于本 plan shape 生成 candidate task pack，不直接绕过台账。
3. Main Foreman materialize 后，通过 `python3 scripts/foreman.py instantiate <TASK_ID>` 进入实现。
4. 实现范围按单任务单 commit 控制，避免扩散到无关 parser/runtime 治理流程。
5. 完成后执行 `python3 scripts/foreman.py validate <TASK_ID>`。
6. closeout 前必须执行 `python3 scripts/task_audit.py --check --phase pre-closeout`。
7. 执行 `python3 scripts/foreman.py closeout <TASK_ID>`。
8. closeout 后必须执行 `python3 scripts/task_audit.py --check --phase post-closeout`。

### Implementation Shape
- 页面边界：拆分为解析工作台、批量解析、解析历史查询三个独立页面，分别维护路由、状态和业务逻辑。
- 单条解析展示：从左右并列调整为上下结果结构，移除重复“解析工作台”标题。
- 结果信息架构：合并“总结”和“结论”双卡，统一为“解析结果”主结构，并用小字备注区分结构解析、综合结论、风险判断、规则命中等语义。
- 状态颜色：`urgent=true` 使用红色提示，`priority=P1` 使用高亮红色；其他状态按现有设计规范保守补充。
- 中文化与 help：自然语言展示以中文为主，代码、字段名、缩写、英文技术标识保留但提供 tooltip 或小按钮解释入口。
- 历史能力：解析完成后写入解析历史存储，并在解析历史查询页面支持检索和查看详情。
- 文档：更新页面结构、历史能力、验证方式和治理执行结果说明。

### Boundary Controls
- 不重写核心 SQL/parser 算法。
- 不修改无关治理流程或 runtime 流程。
- 不扩展到未明确要求的其他产品页面。
- 不进入 delivery tag / write-back，除非后续 Main Foreman 明确归类为 delivery。
