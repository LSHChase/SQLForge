## Candidate Execution Plan for D-TASK-075

### Story Placement

- Story: `D-STORY-007` | Phase-D | 结构解析与数据访问解析双轨闭环
- Rationale: 本任务是 D-TASK-073 查询意图理解能力的复杂 SQL 验证与补强，目标是让结构解析递归识别嵌套子查询和常见反模式风险。

### Governance Sequence

1. 用户已提供复杂反模式 SQL，并要求先生成预期解析结果，再对比系统解析结果并优化。
2. 通过 Main Foreman 物化并实例化 `D-TASK-075`。
3. 实现前执行 `python3 scripts/foreman.py preflight`。
4. 先在测试中固化人工预期：复杂 SQL 应返回 VALID/EXTREME/HEAVY/REPORT_LT_30S，并暴露子查询、OR、函数谓词、前导 LIKE、随机排序等风险。
5. 运行当前解析能力得到失败/差距，用作回归驱动。
6. 补强 JSQLParser 递归 profile：遍历 SELECT item、WHERE/HAVING 表达式、IN/EXISTS/NOT EXISTS、比较子查询和嵌套子查询。
7. 扩展 ParsedSqlProfile、featureSummary、riskChecklist、issues 和 intent/resource 启发式评分。
8. 同步产品规格，明确新增信号均为静态结构风险。
9. 执行模块测试、静态门禁、foreman validate、pre-closeout/post-closeout audit。
10. 通过 standard closeout 形成单任务单 commit。

### Expected Outputs

- Code: recursive AST feature extraction and anti-pattern risk mapping.
- Tests: complex SQL pipeline/profile tests and structure parse controller contract regression.
- Docs: product specification update for complex anti-pattern static signals.

### Boundaries

- 不执行 SQL，不访问数据库，不读取生产元数据。
- 不声明真实索引存在性或真实执行计划成本。
- 不新增持久化字段；保持既有响应字段兼容。
- Trino adapter 保持兼容，不作为本任务主要补强目标。
