## Candidate Execution Plan for D-TASK-074

### Story Placement

- Story: `D-STORY-007` | Phase-D | 结构解析与数据访问解析双轨闭环
- Rationale: 本任务是对 `D-TASK-073` 的闭环复盘补强，修复查询意图理解输出中的 SQL 指纹前处理缺口，使结构解析输出真正支持按标准化查询形态聚合。

### Governance Sequence

1. 复核 `D-TASK-073` 需求、完成台账、产品规格、后端实现和前端展示，确认主体能力已完成。
2. 将遗漏收敛为“SQL 指纹前处理契约不足”：当前仅空白折叠和小写化，未去除注释、未参数化字面量。
3. 通过 Main Foreman 物化 `D-TASK-074`，并在实现前执行 preflight。
4. 补强共享 `SqlFingerprintUtils`：在不执行 SQL、不访问数据库的前提下，对 SQL 注释、字符串/数字/日期时间字面量、连续空白、大小写和末尾分号做确定性标准化。
5. 补共享工具测试和结构解析 controller 回归，证明注释和字面量变化不会改变结构解析 `sqlFingerprint`。
6. 同步产品规格，明确 `sqlFingerprint` 的前处理语义与边界。
7. 执行模块测试、repository knowledge lint、foreman validate、pre-closeout/post-closeout audit。
8. 通过 standard closeout 形成单任务单 commit。

### Expected Outputs

- Code: SQL fingerprint preprocessing normalization.
- Tests: shared utility tests; structure parse controller regression for comment/literal-insensitive fingerprint.
- Docs: product spec review note for D-TASK-073 follow-up.

### Boundaries

- 不执行 SQL，不访问生产数据，不把指纹标准化写成语义等价证明。
- 不新增数据库字段，不回填历史 fingerprint。
- 本任务只修复 `D-TASK-073` 的指纹前处理遗漏；NL2SQL 仍为后续任务。
