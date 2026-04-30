## Candidate Execution Plan: HARN-056

### Story Placement
- Task: `HARN-056`
- Task class: `standard`
- Scope: 解析历史统一展示、批量解析中心重构、报表上传自动识别文件类型、批量解析与文件导入统计展示、测试与文档。

### Governance Sequence
1. Main Foreman 已执行 `python3 scripts/foreman.py preflight` 建立上下文。
2. 由 Main Foreman 维护任务台账，确保 `HARN-056` 处于 `in_progress`。
3. 先补执行计划与需求归档，再进入实现。
4. 代码改动遵循单任务单 commit，避免扩散到无关 parser/runtime 流程。
5. 实现后执行 `python3 scripts/foreman.py validate HARN-056`。
6. closeout 前必须执行 `python3 scripts/task_audit.py --check --phase pre-closeout`。
7. 执行 `python3 scripts/foreman.py closeout HARN-056`。
8. closeout 后必须执行 `python3 scripts/task_audit.py --check --phase post-closeout`。

### Implementation Shape
- 数据层：补齐 report batch 的数据库仓储能力，扩展 parse batch 的列表能力，并为历史页提供可查询的 batch 视图。
- 服务层：补充 batch / report batch 的列表接口，报表导入改为自动识别文件类型，保持结构解析与后端解析的现有落库链路。
- 前端层：重做批量解析中心的信息架构，补齐批量解析与文件导入历史展示，增加统计入口与结果区联动。
- 文档层：更新产品/计划文档，说明统一历史展示、自动识别和统计口径。

### Boundary Controls
- 不重写核心 SQL/parser 算法。
- 不删除现有治理/审计/权限边界。
- 不把批量解析中心再退化为单一长表单页面。
- 不进入 delivery tag / write-back，除非后续明确改判为 delivery 类任务。
