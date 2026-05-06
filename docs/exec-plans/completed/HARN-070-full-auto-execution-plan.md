## Candidate Execution Plan: HARN-070

### Story Landing
- Selected story: `E-STORY-007` | Phase-E | SQL 查询与历史前端增强
- Rationale: 需求核心是 SQL 输入/输出页面的前端展示与编辑体验，最贴近 SQL 查询、SQL 历史及相关 SQL 前端能力增强。跨页面覆盖范围需要在后续 preflight 和仓库读取阶段确认，不能在当前阶段写成事实。

### Governance Order
1. User confirmed this execution template on 2026-05-06; Main Foreman may materialize `HARN-070` as the fixed Phase-E / E-STORY-007 task.
2. Main Foreman must run `python3 scripts/foreman.py preflight`, bind the repository authority context, and instantiate the task through the standard ledger flow.
3. Based on preflight and code inspection, inventory every frontend page or component that accepts SQL input or displays SQL output.
4. Implementation must deliver code, tests, and documentation, then pass `validate -> task_audit pre-closeout -> closeout -> task_audit post-closeout`.
5. This is a normal standard task and must close with single-task single-commit discipline; it is not a delivery task.

### Candidate Work Slices
- 页面盘点：识别 SQL 输入页、SQL 输出页、混合输入输出页，并区分查询、历史、解析、推荐、压测、数据资产等入口。
- 交互能力：SQL 输入区域提供复制按钮和手动格式化按钮；SQL 输出区域提供复制按钮和展示时自动格式化。
- 展示能力：SQL 内容使用 SQL 语言高亮，覆盖关键字、字符串、注释等基础视觉区分。
- 阅读体验：为 SQL 展示/编辑框定义合理高度、宽度、滚动、换行、最大高度与响应式行为。
- 组件策略：优先复用现有前端组件和样式系统；formatter/highlighter 库选择留给后续仓库读取后确认。
- 测试策略：覆盖复制、格式化、自动格式化、只读输出、高亮渲染、长 SQL 滚动/换行等行为。
- 文档更新：记录适用页面范围、用户交互、实现约束与后续维护说明。

### Confirmed Boundaries
- 输入格式化只允许改变当前可编辑 UI 值，不得隐式改变执行 payload、历史记录或后端语义。
- 输出自动格式化只发生在组件接收、渲染或专用展示适配边界，不得改写 API 数据、查询结果或持久化内容。
- SQL 框尺寸必须转化为可验证约束，至少覆盖可用宽度、最小高度、最大高度、滚动、换行或横向滚动、响应式行为。
- formatter/highlighter 优先复用仓库现有工具或依赖；若新增依赖或轻量工具，必须保持 UI 范围内并在文档中记录。
- 测试和文档变更已授权，验证重点是复制、格式化、高亮、长 SQL 可读性，以及格式化不改变执行语义。
