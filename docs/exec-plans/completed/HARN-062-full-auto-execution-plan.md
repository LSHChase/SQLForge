## Candidate Execution Plan: HARN-062

### Story Landing
- Story: `D-STORY-009` | Phase-D | 批量解析与报表清单解析
- Task class: `standard`
- Scope: code, tests, docs only

### Execution Boundary
- 用户已在 2026-05-04T23:12:52-05:00 确认执行预览，可进入 formal materialization。
- 执行模式为 single-agent，由 Main Foreman 通过标准治理链路处理。
- 实现边界：沿用仓库现有导入格式与解析入口；空单元格按 trim 后为空跳过；第二列及之后每个非空单元格作为该行 report code 的一条 SQL；不新增 UI、权限或新导入格式。

### Proposed Task Shape
1. 通过标准治理入口 instantiate `HARN-062` 后再进入实现。
2. 定位批量报表导入/报表清单解析逻辑。
3. 修正解析规则：
   - 第一列稳定作为 report code。
   - 从第二列开始遍历该行全部列。
   - 对每个非空单元格生成该 report code 下的一条 SQL。
   - 空单元格跳过。
   - 支持 100+ SQL 列，不依赖固定列数。
4. 补齐测试场景：
   - 仅一列 SQL。
   - 多列 SQL。
   - 中间存在空列。
   - 100+ 后续列。
   - 防止只识别一列 SQL 的回归。
5. 如导入规则说明存在对应文档，同步更新文档，明确第一列与后续列规则。
6. 通过标准 validate 动作执行验证。
7. 执行 pre-closeout audit。
8. closeout 后执行 post-closeout audit。
9. 普通任务按单任务单 commit 收口。

### Governance Sequence
`preflight` -> `instantiate HARN-062` -> implementation -> `validate HARN-062` -> `task_audit --check --phase pre-closeout` -> `closeout HARN-062` -> `task_audit --check --phase post-closeout` -> single-task commit
