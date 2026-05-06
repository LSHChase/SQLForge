## Candidate Execution Plan for HARN-066

### Story Landing
- Landing story: `D-STORY-010` 解析统计与优先级分层
- Rationale: 需求核心是解析统计口径、维度分层和历史统计可追溯；批量解析/报表导入解析与解析历史展示是统计能力的消费入口。

### Governance Path
1. Main Foreman 后续确认进入实现时，先执行 SQLForge 标准 `preflight`。
2. 由 task-shaper 基于本 plan shape 生成 candidate task pack，不在当前步骤 materialize task。
3. 正式实现前按台账体系 instantiate `HARN-066`。
4. 实现范围应包含代码、测试、文档。
5. 验证后执行 validate，并在 closeout 前后执行强制 task audit。
6. closeout 后再进入归档与单任务单 commit。

### Execution Slices
1. 统计口径确认：对齐 SQL 解析页面既有解析统计口径，明确可复用项、差异项与不可复用原因。
2. 批量/报表解析统计：选择批次解析后生成并返回解析统计，覆盖问题场景、重要程度、报表视角、SQL 清单、优先级视角、逻辑对象视角。
3. 解析历史统计：历史记录可查看同类统计，后续需确定聚合粒度为批次、导入任务、历史记录或 SQL 级别。
4. 单 SQL 解析隔离修复：确保连续解析时状态、错误、缓存或上下文不会污染后续解析。
5. SQL 注释兼容：支持 `--` 行注释，不因注释造成截断、误判或解析失败。
6. 失败诊断增强：单 SQL 解析失败时返回用户可理解的失败原因，并尽可能提供失败位置。
7. 前端消费入口：解析工作台/批量解析中心与解析历史展示统计入口，但具体 UI 布局留待 task-shaper 结合仓库上下文确认。
8. 测试与文档：覆盖批量统计、历史统计、连续单 SQL 解析隔离、`--` 注释、失败原因与位置展示，并更新相关使用说明或限制说明。

### Boundary Notes
- 不在当前 plan shape 中确认接口字段、数据库结构、组件复用方式或统计计算实现。
- 若 SQL 解析页面现有统计口径无法复用，后续 task pack 必须把差异说明列为交付物。
- 本计划不越过 instantiate/validate/closeout 链。
