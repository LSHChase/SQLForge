# SQL 改写功能分层设计

## Summary

本文固化 SQLForge 中 SQL 改写相关能力的产品功能拆分、边界、联动和证据口径。它补充 `SQL 治理平台实施规格`、`生产自动改写闭环任务拆解计划` 和 `前端核心链路聚焦改造任务包`，用于避免把“试算验证”“推荐治理记录”和“真实生产执行历史”混为一个页面或一个业务语义。

本文只定义产品与架构边界，不声明新增功能已经实现，不新增后端 API、数据库 schema、前端路由或运行时行为。

## Design Decision

SQL 改写能力应拆成三个功能面：

| 功能面 | 推荐名称 | 核心问题 | 数据主责 | 是否代表生产已发生 |
|:---|:---|:---|:---|:---|
| 1 | `SQL 改写验证` | 这条或这批 SQL 能否改写，改成什么，是否等价，风险是什么 | `sql-optimization` 的推荐、diff、validation run | 否 |
| 2 | `推荐结果 / 改写记录` | 哪些推荐或改写治理对象可复核、审批、发布、暂停、撤销 | `sql-optimization` 的 recommendation、`sql_rewrite_record`、`rewrite_validation_run` | 否 |
| 3 | `真实 SQL 改写历史` | 哪次生产 SQL 执行真的发生了自动改写，执行了什么 SQL，来源是哪条记录 | `query-execution` 写入的执行历史，由 `governance` 查询聚合 | 是 |

命名建议：

- `SQL 改写验证` 保留“验证/试算”语义。
- `推荐结果` 表示候选建议，不表示已审批或已发布。
- `改写记录` 表示可进入 review / publish / validation 生命周期的治理对象。
- `真实 SQL 改写历史` 或展示层简称 `改写历史` 表示生产执行事实，必须能追溯到 SQL 执行历史。

不建议把第二类命名为 `SQL 改写验证列表`。验证列表容易被理解成第一类试算记录的历史，而第二类真正承载的是推荐和改写治理对象。

## Authority Alignment

本方案遵守以下既有边界：

- SQL 执行历史与 SQL 解析记录已经解耦：执行历史由 `query_history` 与 `/api/governance/query-history` 承载；解析记录由 `sql_parse_history` 与 `/api/sql-optimization/parse-history` 承载。
- 推荐改写 / 加速建议只负责治理与编排，不负责真实装数，不直接修改底层存储。
- `APPLIED` 只能表示配置或绑定已写入，不等同于运行时已生效。
- 自动改写是否已发生只能来自后端执行历史审计字段，不能由前端比较 SQL 文本差异推断。
- `rewrite_validation_run` 的数据主责归 `sql-optimization`；`query-execution` 和 `benchmark-engine` 只能作为只读执行或压测证据提供方。

## Function 1: SQL 改写验证

### Positioning

`SQL 改写验证` 是面向分析人员、DBA、优化人员和审核人员的试算工作台。它回答：

- 输入 SQL 是否存在可改写点。
- 可改写规则是什么。
- 推荐 SQL 是什么。
- 改写前后 diff 是什么。
- 是否能证明结果等价。
- 风险、前置条件和验证证据是否足够。

它类似 `SQL 查询分析` 和 `SQL 解析` 的操作入口，但不等于生产运行时生效入口。

### Entry Points

支持三类入口：

| 入口 | 场景 | 来源字段 |
|:---|:---|:---|
| 单条 SQL 输入 | 人工粘贴一条 SQL 做试算 | `sourceType=MANUAL` 或产品层映射为验证输入 |
| 批量 SQL 输入 | 文件、表格、报表清单或批量语句试算 | `batchId`, `batchItemId`, `reportBatchId`, `reportBatchItemId` |
| 已有证据带入 | 从解析历史、SQL 历史、推荐结果或改写记录进入复验 | `parseHistoryId`, `historyId`, `recommendationId`, `rewriteRecordId` |

### Outputs

最小输出应包括：

- `originalSql`
- `recommendedSql`
- `sqlFingerprint`
- `sourceType`
- `sourceKind`
- `sourceId`
- `parseHistoryId` 或 `historyId`
- `evidenceLevel`
- `ruleChain[]`
- `unappliedRules[]`
- `preconditions[]`
- `semanticRisks[]`
- `diffSummary`
- `validationMethod`
- `validationStatus`
- `comparisonStatus`
- `differenceType`
- `autoApplyAllowed`
- `manualReviewRequired`

### Single SQL Design

单条 SQL 改写验证页面应包含：

- SQL 输入区：原 SQL、数据源、schema、方言、环境、租户。
- 解析证据区：结构解析、访问解析、对象命中、指纹。
- 改写建议区：推荐 SQL、规则链、未采用规则、风险。
- SQL diff 区：文本 diff、规则级 diff、AST 摘要差异。
- 验证区：验证方法、结果摘要、digest、差异样本、验证 run。
- 动作区：生成推荐、创建改写记录、重新验证、跳转推荐详情。

单条验证不应直接提供“生产生效”按钮。发布只能走 `改写记录` 的审批与发布状态接口。

### Batch SQL Design

批量 SQL 改写验证页面应包含：

- 批次导入：复用批量解析导入边界，不改写原始 SQL 文本。
- 批次概览：总数、可改写数、需人工复核数、验证通过数、验证失败数、静态证据数。
- 明细表：每条 SQL 的 source、fingerprint、推荐状态、风险、验证状态。
- 批量操作：批量生成推荐、批量复验、导出验证摘要。
- 明细下钻：进入单条验证详情或推荐详情。

批量能力应只做治理对象生成和验证，不自动批量发布生产改写规则。

### Boundaries

`SQL 改写验证` 可以：

- 调用解析、推荐、diff 和 validation run 能力。
- 创建或关联 recommendation。
- 在用户确认后创建草稿或待复核的 `sql_rewrite_record`。
- 作为解析历史和 SQL 历史的下游试算入口。

`SQL 改写验证` 不可以：

- 标记 `rewriteApplied=true`。
- 修改执行历史。
- 绕过改写记录审批直接改发布状态。
- 把静态解析证据显示成真实扫描量、真实耗时或真实收益。
- 在前端自行判定 SQL 语义等价。

## Function 2: 推荐结果 / 改写记录

### Positioning

`推荐结果 / 改写记录` 是 SQL 改写治理对象的主列表与详情。它回答：

- 系统生成了哪些改写推荐。
- 推荐来自解析、执行历史、慢 SQL、人工输入还是批量来源。
- 推荐是否已经被采纳为改写记录。
- 改写记录当前处于什么审批、发布、验证和暂停状态。
- 发布状态参考和拒绝原因是什么。

该功能面可以在导航中拆成 `推荐结果` 和 `改写记录` 两个入口，但产品语义属于同一治理域。

### Recommendation List

推荐列表的核心筛选：

- `tenantId`
- `recommendationType=REWRITE`
- `sourceType`
- `sourceKind`
- `evidenceLevel`
- `sqlFingerprint`
- `datasourceCode`
- `stage`
- `riskLevel`
- `benefitLevel`
- `validationStatus`
- `manualReviewRequired`
- `autoApplyAllowed`
- `createdAt`

推荐详情应展示：

- 原 SQL / 推荐 SQL。
- SQL diff。
- 规则链和未采用规则。
- 前置条件、风险、收益和信心。
- 来源证据。
- 关联解析记录。
- 关联 SQL 历史。
- 关联改写记录。
- 创建改写记录动作。

推荐仍然是建议，不代表已审批、已发布或已生产执行。

### Rewrite Record List

改写记录列表的核心筛选：

- `rewriteRecordId`
- `recommendationId`
- `sourceType`
- `sourceKind`
- `sourceId`
- `parseHistoryId`
- `historyId`
- `sqlFingerprint`
- `reviewStatus`
- `publishStatus`
- `validationStatus`
- `autoApplyAllowed`
- `manualReviewRequired`
- `lastValidationRunId`
- `alertStatus`

改写记录详情应展示：

- 来源推荐。
- 原 SQL / 推荐 SQL / 最近执行 SQL。
- SQL diff。
- review 状态、审批人、审批时间、审批意见。
- publish 状态、状态 trace、历史 runtime binding / 规则版本追踪字段。
- validation run 列表。
- 周期比对状态。
- 暂停、撤销、告警和 trace 证据。

### Lifecycle

推荐和改写记录的关系：

1. 解析、执行历史、慢 SQL 或人工输入生成 recommendation。
2. 用户或策略把 recommendation 转换成 `sql_rewrite_record`。
3. 改写记录进入待复核或待审批。
4. 审批和驳回只走改写记录领域状态机；发布、暂停和撤销必须先走统一授权入口和后端状态接口，再调用 query-execution runtime binding。
5. 发布动作只有在 runtime binding 返回 `ACTIVE` 后才能把 `publishStatus` 置为 `PUBLISHED`；暂停动作只有在 runtime 返回 `PAUSED` 后才能把 `publishStatus` 置为 `PAUSED`。
6. 发布资格、验证结果和告警是后端门禁和审计证据，页面按钮不得绕过这些接口直接改状态。
7. 周期比对失败时，运行时暂停走 runtime binding；若暂停失败，只能记录失败 trace 和告警，不得直接改数据库状态伪造运行时暂停。

`manualReviewRequired=true` 只表示需要人工查看，不表示审批通过。

### Boundaries

`推荐结果 / 改写记录` 可以：

- 展示推荐、diff、风险、收益和来源证据。
- 创建和查看改写记录。
- 承载审批、发布、暂停、撤销动作。
- 展示发布状态参考和拒绝原因。
- 展示 validation runs 和告警引用。
- 跳转 SQL 解析记录和 SQL 历史。

`推荐结果 / 改写记录` 不可以：

- 把推荐状态写成生产执行事实。
- 把审批通过写成运行时已生效。
- 绕过后端状态接口。
- 把 `APPLIED` 展示为默认自动改写已生效。
- 直接修改 `query_history` 的真实执行结果。

## Function 3: 真实 SQL 改写历史

### Positioning

`真实 SQL 改写历史` 是生产执行事实的只读审计面。它回答：

- 哪次 SQL 执行真实发生了自动改写。
- 原始 SQL 是什么。
- 实际执行 SQL 是什么。
- 命中了哪个 `rewriteRecordId`，以及生产执行链路写入的 runtime binding / 规则版本证据。
- 当时的发布状态、验证状态和告警状态是什么。
- 这次执行是否与后续差异比对或暂停动作有关。

该功能应优先挂在 `SQL 历史查询` 的筛选、列表标识和详情 tab 下；展示层可以提供 `改写历史` 菜单深链，但不应脱离 SQL 执行历史成为第二套真值。

### List Filters

真实改写历史的核心筛选：

- 时间范围。
- `tenantId`
- `datasourceCode`
- `stage`
- `reportCode`
- `sqlFingerprint`
- `rewriteApplied`
- `hasRewriteRecord`
- `rewriteRecordId`
- `runtimeBindingId`
- `runtimeRuleVersion`
- `rewriteValidationStatus`
- `publishStatusSnapshot`
- `alertStatus`

### Detail Fields

详情至少展示：

- `historyId`
- `executionId`
- `originalSql`
- `boundSqlText`
- `executedSql`
- `rewriteApplied`
- `rewriteRecordId`
- `recommendationId`
- `runtimeBindingId`
- `runtimeRuleVersion`
- `publishedSqlFingerprint`
- `publishStatusSnapshot`
- `validationRunId`
- `validationStatus`
- `differenceType`
- `autoApplyPaused`
- `traceRefs`
- `alertRefs`

### Boundaries

`真实 SQL 改写历史` 可以：

- 从执行历史证明自动改写已发生。
- 反链推荐、改写记录、validation run、runtime binding 和告警。
- 支持审计、导出、取证和问题复盘。
- 展示原 SQL 与实际执行 SQL 的 diff。

`真实 SQL 改写历史` 不可以：

- 接收纯解析记录或纯验证记录作为“真实发生”数据。
- 由前端通过原 SQL 和推荐 SQL 不同来推断 `rewriteApplied=true`。
- 成为第二套改写记录生命周期状态机。
- 承载审批、发布、暂停和撤销主动作；这些动作归 `改写记录`。

## Parse History Sourced Rewrite Ownership

SQL 解析记录对应的改写必须按以下规则归属：

| 场景 | 主归属 | 展示位置 | 是否进入真实改写历史 |
|:---|:---|:---|:---|
| 单条解析发现可改写点 | `推荐结果` | 解析历史详情展示关联推荐入口 | 否 |
| 批量解析生成多条推荐 | `推荐结果` | 批量解析详情展示推荐统计与下钻 | 否 |
| 解析推荐被保存为治理对象 | `改写记录` | 推荐详情和解析历史详情展示关联改写记录 | 否 |
| 解析推荐经审批发布为 runtime binding | `改写记录` | 改写记录详情展示 publish / binding 状态 | 否 |
| 后续生产执行命中该 binding | `真实 SQL 改写历史` | SQL 历史详情和改写历史深链 | 是 |

因此：

- 解析历史不是改写记录主列表。
- 解析记录对应的改写建议应进入 `推荐结果`。
- 解析记录对应的已采纳治理对象应进入 `改写记录`。
- 解析历史页面只做关联入口、来源证据和回跳。
- 只有生产执行链路真实写入 `rewriteApplied`、`executedSql`、`rewriteRecordId`、`runtimeBindingId` 等字段后，才进入 `真实 SQL 改写历史`。

## End-to-End Flow

推荐主链路：

1. 用户在 `SQL 解析`、`解析历史`、`SQL 查询分析` 或 `SQL 历史查询` 中发现可优化 SQL。
2. 进入 `SQL 改写验证` 生成推荐 SQL、diff 和验证结果。
3. 试算结果写入 `推荐结果`。
4. 用户采纳推荐并创建 `改写记录`。
5. 审核人员在 `改写记录` 中完成 review。
6. 审批通过后，用户在 `改写记录` 中把 `publishStatus` 改为 `PUBLISHED`。
7. 发布、暂停、撤销均只变更改写记录状态并写入 trace。
8. 后续生产 SQL 执行是否真实改写，只能由执行链路自己的证据写入。
9. `真实 SQL 改写历史` 展示生产事实，并反链到推荐和改写记录。
10. 周期比对发现差异时，把改写记录状态改为 paused / diverged，写入 validation run 和告警。

## Page Interaction Model

### From SQL 解析 / 解析历史

- 展示“生成改写推荐”入口。
- 展示已关联推荐数量。
- 展示已关联改写记录数量。
- 点击推荐进入 `推荐结果` 详情。
- 点击改写记录进入 `改写记录` 详情。
- 不显示“真实已改写”结论，除非存在关联 SQL 执行历史证据。

### From SQL 历史查询

- 列表支持 `rewriteApplied`、`hasRewriteRecord`、`rewriteValidationStatus` 筛选。
- 详情展示改写记录 tab。
- 若本次执行真实发生改写，展示原 SQL / 实际执行 SQL / runtime binding 证据。
- 若只是存在关联推荐或改写记录，但本次执行未改写，必须明确展示为“有关联治理对象，但本次未发生自动改写”。

### From 推荐结果

- 推荐详情展示 SQL diff、规则链、来源证据和创建改写记录动作。
- 若来源是解析记录，展示 parseHistoryId 回跳。
- 若来源是执行历史，展示 historyId 回跳。
- 若已有改写记录，展示生命周期摘要和跳转。

### From 改写记录

- 展示 review、publish、validation、binding、pause、unpublish 状态。
- 展示最近真实命中的 SQL 历史。
- 展示全部 validation runs。
- 操作动作只能调用后端真实门禁接口。

## State and Evidence Rules

产品状态展示应使用以下证据层级：

| UI Label | Backend Evidence | Meaning |
|:---|:---|:---|
| 可改写 | recommendation 存在，且有 ruleChain / recommendedSql | 只是建议 |
| 已验证 | validation run 通过 | 证明某次验证通过，不代表已发布 |
| 待审批 | `reviewStatus=PENDING_REVIEW` | 等待人工处理 |
| 已审批 | `reviewStatus=APPROVED` | 可点击发布状态按钮 |
| 未发布 | `publishStatus=UNPUBLISHED` | 不会被生产运行时使用 |
| 已发布 | `publishStatus=PUBLISHED` | 仅表示改写记录状态已发布，不自动证明运行时生效 |
| 本次已自动改写 | 执行历史 `rewriteApplied=true` 且有 binding 证据 | 生产事实 |
| 已暂停 | `publishStatus=PAUSED` | 改写记录状态暂停 |
| 差异待复核 | validation run `DIVERGED` 或差异告警未关闭 | 需要人工处理 |

## API and Data Ownership

推荐的数据主责：

- `sql-optimization` owns recommendation、diff、rewrite record、validation run。
- `governance` owns query history query surface and history aggregation。
- `query-execution` owns runtime execution evidence and binding application in execution path。
- `benchmark-engine` can provide regression evidence but cannot become rewrite validation truth.

跨页面只能通过 ID 和只读聚合关联：

- `recommendationId`
- `rewriteRecordId`
- `validationRunId`
- `parseHistoryId`
- `historyId`
- `executionId`
- `runtimeBindingId`
- `sqlFingerprint`
- `traceId`

## Non-Goals

本文不要求：

- 新增独立后端接口。
- 新增独立前端 route。
- 新增数据库表或字段。
- 调整审批、发布或自动应用状态机。
- 把推荐结果改成生产执行事实。
- 把解析历史改成改写历史。
- 把本地测试验证闭环混入生产自动改写闭环。
- 让前端承担 SQL 语义等价判断。

## Implementation Guidance

后续若要按本方案实现，应遵守以下拆分：

1. 文案和导航可先把 `SQL 改写验证`、`推荐结果 / 改写记录`、`改写历史` 的说明对齐。
2. v1 可复用现有 `推荐结果` 与 `SQL 历史查询` route，通过 query/tab 深链呈现 `改写记录` 和 `改写历史`。
3. 除非人工确认，不新增独立 `改写历史` 后端 API；优先复用 `query-history/{historyId}/rewrite-records` 和执行历史详情。
4. 若要新增 `SQL 改写验证` 独立页面，应先确认它不会替代 `SQL 解析`、`推荐结果` 或 `改写记录`。
5. 任何改写记录发布、暂停或撤销动作都必须在 `改写记录` 生命周期内完成，并通过后端状态接口记录 trace。

## Acceptance Criteria

后续任务使用本方案作为验收依据时，至少应满足：

- 用户能区分试算验证、推荐治理对象和真实生产历史。
- 解析记录来源的改写不会被展示为真实改写历史。
- 真实改写历史只来自执行历史审计字段。
- 推荐详情、改写记录详情和 SQL 历史详情之间能按 ID 双向追溯。
- 审批通过、发布成功、运行时 active 和本次执行已改写四个状态不会混淆。
- 周期比对差异能反映到改写记录、validation run、告警和历史追溯。
