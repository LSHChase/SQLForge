## Candidate Execution Plan: HARN-063

### Scope
- 修复批量解析中解析失败的报表记录无法点击查看解析详情的问题。
- 增强批量解析列表或相关视图中的报表解析结果可见性。
- 增强解析详情展示，支持用户定位导入解析问题。
- 覆盖代码、测试和文档更新。

### Governance Flow
1. 用户已在 2026-05-05T01:32:57-05:00 确认本 candidate execution plan；后续按 standard 单任务执行。
2. Main Foreman 执行 `python3 scripts/foreman.py preflight` 建立上下文。
3. 通过标准治理入口实例化任务：`python3 scripts/foreman.py instantiate HARN-063`。
4. 在实例化任务约束内基于仓库事实确认具体代码范围、页面入口、接口/数据结构与文档位置。
5. 实现失败记录详情入口、解析结果展示、详情展示能力。
6. 增加或更新关键路径测试。
7. 更新相关文档，说明批量解析结果与详情查看行为。
8. 执行 `python3 scripts/foreman.py validate HARN-063`。
9. 执行 `python3 scripts/task_audit.py --check --phase pre-closeout`。
10. 执行 `python3 scripts/foreman.py closeout HARN-063`。
11. 执行 `python3 scripts/task_audit.py --check --phase post-closeout`。
12. 按普通任务单任务单 commit 收尾；不进入 delivery tag/write-back，除非后续明确升级为 delivery。

### Confirmed Boundary
- “中报表导入解析”按批量解析流程内的报表导入解析处理。
- 失败、成功及其他解析状态应保持结果可见性与详情入口语义一致。
- 详情字段以仓库现有模型和接口事实为准，目标是足够定位导入解析问题，并沿用既有敏感信息处理与权限边界。
- 不新增权限模型、导入格式、核心解析算法重构或数据模型变更；如实现阶段发现必须触及这些边界，暂停并另行确认。

### Candidate Work Packages
- 结果入口：确认批量解析记录中成功、失败及其他状态的详情入口规则，修复失败记录不可点击问题。
- 结果展示：确认并补齐批量解析列表或相关页面中报表解析状态、摘要信息、详情入口的一致性。
- 详情展示：确认详情页面或弹层应展示的错误、状态、报表标识、解析摘要等字段，避免把未确认字段提前固化为事实。
- 测试覆盖：覆盖失败记录详情查看、结果状态展示、详情入口可用性、入口与状态一致性。
- 文档更新：补充批量解析结果与详情查看行为说明，具体文档范围由实例化后的仓库上下文决定。
