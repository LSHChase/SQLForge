## Candidate Execution Plan for HARN-045

### Story Placement

- Story: `E-STORY-003` | Phase-E | 前后端分离持续治理
- Rationale: 该需求是跨页面的前端表单组件语义治理，重点在页面实现、接口提交格式、校验与回显的一致性，属于前后端分离后的持续页面治理范畴，而不是某个单一业务页面增强。

### Governance Sequence

1. 用户已于 2026-04-28 确认该执行模板。
2. 由 Main Foreman 进入正式流程，不在 plan-shaper 阶段 materialize task。
3. 非 trivial 实现开始前执行：`python3 scripts/foreman.py preflight`。
4. 通过标准入口实例化任务：`python3 scripts/foreman.py instantiate HARN-045`。
5. 在任务上下文内读取治理入口文档、任务台账、相关页面实现、字段来源、API schema、校验逻辑和现有测试。
6. 建立字段级组件映射表：区分自由文本、日期时间、枚举、关联资源、布尔值、数值、搜索选择等类型；无法确认的字段记录原因并等待确认或保守处理。
7. 改写页面实现：将日期/时间字段替换为带时间能力的日期时间选择器；将租户、数据源等受控候选字段替换为下拉框或等价选择组件；其他字段按语义选用合适控件。
8. 保持既有业务流程、数据提交格式、默认值、回显、校验行为和权限边界不被破坏。
9. 补充或调整测试：覆盖关键组件交互、表单提交、默认值、回显、边界输入和候选值加载失败等场景。
10. 补充必要文档：记录页面组件治理原则、字段映射依据、无法确认项和验证方式。
11. 执行标准验证：`python3 scripts/foreman.py validate HARN-045`。
12. 执行 pre-closeout 审计：`python3 scripts/task_audit.py --check --phase pre-closeout`。
13. 通过标准 closeout：`python3 scripts/foreman.py closeout HARN-045`。
14. 执行 post-closeout 审计：`python3 scripts/task_audit.py --check --phase post-closeout`。
15. 按普通 standard 任务要求形成单任务单 commit；不进入 delivery tag/write-back，除非后续 Main Foreman 明确改判为 delivery 类任务。

### Expected Outputs After Confirmation

- Code: 页面组件替换与必要的候选值加载/适配逻辑。
- Tests: 组件交互、提交、回显、默认值和边界输入覆盖。
- Docs: 字段组件治理原则、字段映射依据和验证说明。

### Boundaries

- template preview 阶段已结束；确认后通过 governed intake / foreman 标准动作进入正式任务流程。
- 后续实现不得把“页面当前都是输入框”写成已确认事实，必须在 preflight 和任务上下文中验证。
- 若字段候选值来源、权限边界或提交格式存在冲突，应在任务执行中显式记录并请用户确认。
