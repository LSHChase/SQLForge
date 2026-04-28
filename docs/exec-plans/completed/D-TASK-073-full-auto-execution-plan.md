## Candidate Execution Plan for D-TASK-073

### Story Placement

- Story: `D-STORY-007` | Phase-D | 结构解析与数据访问解析双轨闭环
- Rationale: 本需求是在已落地的结构解析入口上升级查询意图理解、多维特征、风险和资源估算输出，并保持 access parse 独立语义，属于结构解析双轨闭环的后续增强。

### Governance Sequence

1. 用户已确认执行查询意图理解升级计划，并选择“双 parser”方案、NL2SQL 后续预留。
2. 通过 Main Foreman 标准流程物化并实例化 `D-TASK-073`。
3. 非 trivial 实现开始前执行 `python3 scripts/foreman.py preflight`，并在任务上下文内读取结构解析、parser ADR、前端解析工作台和验证规则。
4. 后端实现先固化查询意图契约：SQL 指纹、分类标签、多维特征、风险清单、启发式资源估算与置信度。
5. 抽象 parser adapter：保留现有 JSQLParser 作为默认实现，新增 Trino adapter 并通过统一 profile 屏蔽 parser 差异；失败统一降级为结构解析问题，不阻断响应。
6. 基于统一 profile 实现扫描模式、Join 类型、计算密度、资源类型、SLA 等级与查询分类标签提取。
7. 将结构化风险与资源估算映射回现有 `riskTags/issues/priority` 兼容模型，并扩展结构解析响应 VO。
8. 前端解析工作台展示 SQL 指纹、意图标签、特征维度、风险清单和资源估算；NL2SQL 仅作为后续能力预留，不实现自然语言生成 SQL。
9. 补齐后端单元/契约测试、前端 contract/build/lint 验证和文档同步。
10. 执行 `python3 scripts/foreman.py validate D-TASK-073`。
11. 执行 `python3 scripts/task_audit.py --check --phase pre-closeout`。
12. 通过 `python3 scripts/foreman.py closeout D-TASK-073` 收口。
13. 执行 `python3 scripts/task_audit.py --check --phase post-closeout`。
14. 按 standard 任务形成单任务单 commit，不进入 delivery tag/write-back。

### Expected Outputs

- Code: parser adapter 抽象、查询意图模型、特征提取、风险/资源估算、响应映射、前端展示。
- Tests: parser/profile/feature/risk/resource 后端测试，结构解析 controller/contract 回归，前端解析工作台契约、build 与 lint。
- Docs: 产品规格与结构解析契约说明，明确启发式估算和 NL2SQL 后续边界。

### Boundaries

- 不执行 SQL、不访问生产数据、不把资源估算写成真实执行计划。
- 不破坏旧结构解析字段与 `syntaxStatus=INVALID` 降级语义。
- 不把权限、对象存在性、字段存在性从 access parse 移入结构解析。
- 若 Trino parser 依赖无法安全引入，必须暂停并记录人工确认点。
