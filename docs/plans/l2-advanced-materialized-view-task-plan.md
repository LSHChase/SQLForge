# L2 高级物化视图推荐任务拆解计划

本文把 L2 物化视图推荐从“发现聚合/分组后按原 SQL 生成草案”升级为“从 SQL 中抽取可复用计算子图，生成可被一类查询复用的高级物化视图，并生成可审查的 rewrite 方案”的任务包。

本文明确排除 `EXACT_QUERY_MV`。后续任何任务不得把“一条原 SQL 原样包进 `CREATE MATERIALIZED VIEW ... AS <原 SQL>`”作为兜底、默认或低阶推荐能力。

## 目标闭环

目标路径固定为：

1. SQL 查询、解析或推荐任务进入 L2 分析。
2. 系统解析 SQL 形态，抽取表、投影、谓词、Join 图、聚合、分组、时间粒度、CTE/子查询等结构。
3. 系统判断 SQL 是否适合生成高级物化视图候选。
4. 系统只生成以下高级 MV 类型之一或多个候选：
   - `PARAMETERIZED_AGG_MV`
   - `PREJOIN_MV`
   - `STAR_AGG_MV`
   - `ROLLUP_MV`
   - `COMMON_SUBGRAPH_MV`
5. 系统对每个候选生成结构化证据、阻断原因、DDL 草案、刷新 SQL、验证 SQL、回滚 SQL 和 rewrite SQL。
6. 页面展示“为什么该 MV 可复用、覆盖哪些参数、保留哪些业务过滤、哪些谓词被外提、原 SQL 如何改写到 MV”。
7. SQLForge 仍保持 `PULL_ONLY` 边界，不直接执行生产建表、刷新、删除或真实数据改写。
8. 若外部完成建 MV、刷新和验证，后续 runtime 生效必须走现有 SQL 改写记录审批与发布链路；发布后的正常 SQL 执行是否生效，只能由 query-execution runtime binding 与执行历史审计证明。

## 非目标

- 不实现 `EXACT_QUERY_MV`。
- 不把 Redis 结果缓存替代成 MV 推荐。
- 不直接执行生产 DDL、刷新、回滚或数据装载。
- 不在缺少验证时自动发布 runtime rewrite binding。
- 不承诺所有 SQL 都能生成 MV；不能证明可复用或可改写时必须给出阻断原因。
- 不把 `manualReviewRequired` 当作审批通过。
- 不把加速计划审批页等同于 SQL 改写发布入口。
- 不在本计划中引入基于元数据、唯一性、分区、真实频次、成本模型的强收益判断；这些只作为后续增强，不作为本批任务的前置条件。

## 当前事实基线

- 当前 L2 规则已经能通过静态解析发现 `PRECOMPUTE_MV`、`PARTITION_PRUNING`、`BUCKET_JOIN`、`RESULT_CACHE`、`TOPN_PUSHDOWN` 等候选。
- 当前 `PRECOMPUTE_MV` 主要由聚合函数或 `GROUP BY` 触发。
- 当前物化视图产物生成依赖明确目标引擎，支持 `HETU`、`HIVE`、`SPARK`，缺失或为 `AUTO` 时会返回 `TARGET_ENGINE_REQUIRED`。
- 当前 `PARAMETERIZED_AGG_MV` 产物已由 AMV-005 生成参数外提型聚合 MV：参数字段进入 MV 维度，固定业务谓词进入 DDL base `WHERE`，安全谓词字段保留在 MV 维度并由 rewrite 继续过滤，`rewriteSql` 查询 MV 后二次过滤、二次聚合。
- 当前 AMV-005 只覆盖 `PARAMETERIZED_AGG_MV`；`PREJOIN_MV`、`STAR_AGG_MV`、`ROLLUP_MV`、`COMMON_SUBGRAPH_MV` 的专用 DDL/rewrite 生成仍是后续任务范围，遇到对应形态必须结构化阻断或复核，不得退回 exact-query-like 草案。
- 当前推荐中心/加速治理页面能展示 `accelerationArtifact`，但不会自动把 `accelerationArtifact.rewriteSql` 发布为运行时改写绑定。
- 当前 runtime 自动生效路径属于 SQL 改写记录的审批、发布和 runtime binding，不属于 L2 加速产物展示本身。

## 高级 MV 类型

### PARAMETERIZED_AGG_MV

用于聚合报表中参数变化但计算粒度稳定的场景。

典型输入：

```sql
SELECT customer_id, SUM(amount) AS total_amount
FROM orders
WHERE dt BETWEEN :start_dt AND :end_dt
  AND region = :region
GROUP BY customer_id
```

推荐 MV 应把参数过滤字段提升为 MV 粒度：

```sql
CREATE MATERIALIZED VIEW mv_orders_customer_region_daily AS
SELECT
  dt,
  region,
  customer_id,
  SUM(amount) AS sum_amount,
  COUNT(*) AS cnt
FROM orders
GROUP BY dt, region, customer_id
```

rewrite SQL：

```sql
SELECT customer_id, SUM(sum_amount) AS total_amount
FROM mv_orders_customer_region_daily
WHERE dt BETWEEN :start_dt AND :end_dt
  AND region = :region
GROUP BY customer_id
```

适用条件：

- 存在 `GROUP BY` 或可重聚合指标。
- `WHERE` 中包含日期、区域、渠道、客户、商品、租户等参数谓词。
- 查询分组粒度不细于 MV 粒度。
- 聚合函数可从 MV 指标重算。

默认阻断：

- `SELECT *` 未展开。
- 聚合包含不可合并指标且没有安全拆解方式。
- 参数过滤字段无法作为 MV 维度保留。
- 安全字段被移除。

### PREJOIN_MV

用于 Join 成本高、过滤和聚合经常变化的场景。它预先展开稳定 Join 图，但不强制预聚合。

典型输入：

```sql
SELECT
  c.customer_level,
  p.category,
  SUM(o.amount) AS total_amount
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN products p ON o.product_id = p.product_id
WHERE o.dt BETWEEN :start_dt AND :end_dt
GROUP BY c.customer_level, p.category
```

推荐 MV：

```sql
CREATE MATERIALIZED VIEW mv_order_customer_product_detail AS
SELECT
  o.dt,
  o.order_id,
  o.customer_id,
  o.product_id,
  o.amount,
  c.customer_level,
  p.category
FROM orders o
JOIN customers c ON o.customer_id = c.customer_id
JOIN products p ON o.product_id = p.product_id
```

rewrite SQL：

```sql
SELECT
  customer_level,
  category,
  SUM(amount) AS total_amount
FROM mv_order_customer_product_detail
WHERE dt BETWEEN :start_dt AND :end_dt
GROUP BY customer_level, category
```

适用条件：

- Join 数量较多，Join key 明确。
- Join 图主要是事实表到维表的等值 Join。
- 查询瓶颈预期来自重复 Join，而不是单次最终聚合。
- 投影字段可从预 Join MV 覆盖原查询。

默认阻断：

- `CROSS JOIN`、非等值 Join、复杂表达式 Join。
- `OUTER JOIN` 需要人工复核，默认不直接生成可发布 rewrite。
- 疑似多对多 Join 或行数放大风险无法解释。
- Join 后字段别名冲突不能自动消解。

### STAR_AGG_MV

用于事实表 Join 维表后再聚合的 BI 报表场景。它把 Join 与聚合一起预计算。

推荐 MV 示例：

```sql
CREATE MATERIALIZED VIEW mv_sales_daily_category_city AS
SELECT
  o.dt,
  p.category,
  s.city,
  SUM(o.amount) AS sum_amount,
  COUNT(*) AS order_count
FROM orders o
JOIN products p ON o.product_id = p.product_id
JOIN shops s ON o.shop_id = s.shop_id
GROUP BY o.dt, p.category, s.city
```

适用条件：

- 存在事实表 + 维表 Join 图。
- 存在聚合函数。
- `GROUP BY` 字段主要来自时间字段和维表属性。
- 目标查询可由 MV 上的过滤和二次聚合得到。

默认阻断：

- Join 图无法推导主事实表。
- 聚合粒度无法覆盖原 SQL。
- 指标无法二次聚合。
- 安全过滤字段未保留。

### ROLLUP_MV

用于日、周、月、季度等时间粒度查询复用。它优先生成较细粒度 MV，再通过二次聚合支持更粗查询。

推荐原则：

- 优先生成日粒度或 SQL 中可推导的最细稳定时间粒度。
- 月、季度、年查询通过 `date_trunc` 或等价函数在 MV 上二次聚合。
- `AVG` 必须拆为 `SUM` + `COUNT`，不能直接对 `AVG` 再求 `AVG`。

适用条件：

- SQL 存在时间过滤或时间分组。
- 指标可重聚合。
- 原查询的时间粒度不细于 MV 粒度。

默认阻断：

- 精确 `COUNT(DISTINCT)`、中位数、百分位等不能安全合并的指标。
- 时间表达式不可归一化。
- 时间字段存在非确定函数且不能稳定展开。

### COMMON_SUBGRAPH_MV

用于多个 SQL 或一个 SQL 内多个 CTE/子查询复用同一复杂计算子图的场景。

适用条件：

- SQL 存在复杂 CTE、派生表或重复子查询。
- 子图可以独立表达为只读 `SELECT`。
- 子图输出字段能覆盖上层查询需要的字段。
- 子图中不包含不能稳定物化的非确定函数或临时上下文。

默认阻断：

- 子查询依赖外层作用域且无法解关联。
- 子图包含窗口函数、排序分页或非确定函数，且无法证明可物化。
- 子图物化后会破坏原查询过滤作用域。

## 推荐决策模型

### SQL 结构抽取

实现必须从 SQL 中抽取以下结构，不得只用字符串包含判断作为主逻辑：

- 表、别名、库表限定名。
- 投影字段和表达式。
- `WHERE` 谓词。
- Join 类型、Join key、Join 条件。
- `GROUP BY` 字段。
- 聚合函数、聚合参数、指标别名。
- `HAVING` 条件。
- `ORDER BY` 与 `LIMIT`。
- CTE、派生表、子查询。
- 时间函数、日期字段和日期粒度。
- 非确定函数，例如当前时间、随机函数、会话函数。

### 谓词分类

每个谓词必须归入以下类别之一：

- `EXTERNALIZED_PARAMETER_PREDICATE`：参数型过滤，例如日期、区域、渠道、客户、商品、租户等。`tenant_id` 按普通参数过滤处理，字段必须保留在 MV 粒度或输出中，rewrite SQL 再应用该过滤。
- `RETAINED_BUSINESS_PREDICATE`：业务固定过滤，例如 `status='PAID'`、`is_deleted=0`。可以保留在 MV DDL 中，但必须展示为保留谓词。
- `SECURITY_PREDICATE`：显式非租户安全边界过滤，例如权限域、数据域、访问域。不得丢失；要么保留在 MV 中，要么保留为 MV 字段并在 rewrite 中强制过滤。缺少显式非租户安全谓词不得单独阻断 `GENERATED`。
- `BLOCKED_UNSTABLE_PREDICATE`：非确定或上下文相关谓词，例如 `current_date`、随机函数、会话变量。默认阻断高级 MV，除非后续任务明确提供稳定化策略。

### 粒度推导

MV 粒度必须由以下字段组成：

- 原查询 `GROUP BY` 字段。
- 被外提的参数谓词字段。
- 必需安全字段。
- Join rewrite 所需的 Join key。
- 时间 Rollup 所需的最细稳定时间字段。

原则：

- MV 可以比原查询更细，原查询在 MV 上二次聚合。
- MV 不能比原查询更粗，否则无法还原原查询。
- 任何被 rewrite SQL 过滤、分组、投影引用的字段，要么是 MV 维度，要么是可从 MV 指标安全计算得到。

### 指标推导

支持优先级：

- P0：`SUM`、`COUNT`、`MIN`、`MAX`。
- P1：`AVG` 拆为 `SUM` + `COUNT`。
- P2：比例类指标拆为分子与分母。
- P3：`COUNT(DISTINCT)`、百分位、中位数、复杂 UDAF 默认阻断；后续可以在特定引擎支持 sketch 或聚合状态时新增人工复核型候选。

### Rewrite 命中判断

只有同时满足以下条件，候选才允许生成可审查 `rewriteSql`：

- 原查询需要的表集合可由 MV 覆盖。
- 原查询投影字段可由 MV 字段或 MV 指标安全计算得到。
- 原查询过滤字段存在于 MV 输出或 MV 粒度中。
- 原查询分组粒度不细于 MV 粒度。
- 原查询指标可由 MV 指标重算。
- 显式非租户安全谓词没有被丢失；缺少显式非租户安全谓词不得单独阻断 `GENERATED`。
- 推荐 SQL 仍是只读 SQL。

不满足时，候选仍可展示为 `BLOCKED` 或 `REVIEW_REQUIRED`，但不得生成可发布 runtime binding 草案。

## 产物契约

高级 MV 产物应扩展为结构化对象：

```json
{
  "rule": "PRECOMPUTE_MV",
  "mvType": "PARAMETERIZED_AGG_MV",
  "artifactStatus": "GENERATED | BLOCKED | REVIEW_REQUIRED",
  "mvName": "mv_orders_customer_region_daily",
  "targetEngine": "HETU",
  "targetDatasource": "hetu_main",
  "dialect": "HETU_MATERIALIZED_VIEW",
  "grain": ["dt", "region", "customer_id"],
  "dimensions": ["dt", "region", "customer_id"],
  "measures": [
    {
      "name": "sum_amount",
      "sourceExpression": "SUM(amount)",
      "rewriteExpression": "SUM(sum_amount)",
      "mergeable": true
    }
  ],
  "joinGraph": [],
  "externalizedPredicates": [],
  "retainedPredicates": [],
  "securityPredicates": [],
  "blockedPredicates": [],
  "coverage": {
    "coversProjection": true,
    "coversFilters": true,
    "coversGrouping": true,
    "coversMeasures": true,
    "coversSecurity": true
  },
  "blockingReasons": [],
  "reviewWarnings": [],
  "ddlSql": "...",
  "refreshSql": "...",
  "validationSql": "...",
  "rollbackSql": "...",
  "rewriteSql": "...",
  "runtimeRewriteBinding": "NOT_CREATED",
  "governanceBoundary": "PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE"
}
```

字段要求：

- `mvType` 必填，且不能为 `EXACT_QUERY_MV`。
- `artifactStatus=GENERATED` 时必须有 `ddlSql`、`refreshSql`、`validationSql`、`rollbackSql`、`rewriteSql`。
- `artifactStatus=BLOCKED` 时必须有 `blockingReasons`。
- `artifactStatus=REVIEW_REQUIRED` 时必须有 `reviewWarnings`，且不得自动发布。
- `rewriteSql` 必须查询 MV，不能仍指向原始基表。
- `governanceBoundary` 必须继续声明 SQLForge 不执行生产 DDL。

## 页面与治理闭环

页面应展示：

- MV 类型中文名。
- 粒度、维度、指标。
- 外提谓词、保留谓词、安全谓词、阻断谓词。
- Join 图摘要。
- 原 SQL 到 MV 查询的覆盖证明。
- DDL、刷新、验证、回滚、rewrite SQL。
- 阻断原因和人工复核原因。

让正常 SQL 执行直接生效必须走以下链路：

1. 外部执行 `ddlSql`。
2. 外部执行 `refreshSql`。
3. 执行 `validationSql` 或等价验证。
4. 创建 SQL 改写记录，且 `recommendedSqlText` 必须来自 `accelerationArtifact.rewriteSql`。
5. 审批改写记录。
6. 发布改写记录。
7. query-execution runtime binding 返回 `ACTIVE`。
8. 后续同租户、同 SQL 指纹、同数据源证据的正常 SQL 执行命中 binding，执行历史记录 `rewriteApplied=true`。

## 任务包

以下 ID 是本文档内的计划 ID，不等同于当前活动任务。进入实现前，必须按仓库治理流程逐个 materialize 到 `tasks.md`，并执行 `python3 scripts/foreman.py instantiate <TASK_ID>`。

### AMV-001：固化高级 MV 契约与禁止 EXACT_QUERY_MV 边界

**目标**：把高级 MV 推荐的产品边界、产物契约、禁止项和治理路径写入长期文档。

**范围**：

- 更新产品或架构文档，引用本文的 MV 类型、产物字段和非目标。
- 明确 `EXACT_QUERY_MV` 不允许作为本项目推荐类型。
- 明确 L2 产物不是运行时生效，运行时生效必须通过 SQL 改写记录发布链路。
- 不修改业务代码。

**验收**：

- 文档中不存在把原 SQL 原样物化作为兜底推荐的描述。
- 文档必须显式说明当前 V1 exact-query-like MV 草案行为是后续 AMV 实现任务待修正缺口，不是高级 MV 已完成事实。
- 文档明确 `recommendedSqlText` 若要生效必须取自 `accelerationArtifact.rewriteSql` 并经过审批发布。
- 执行 `node scripts/lint-repository-knowledge.js`、`git diff --check`、`python3 scripts/task_audit.py --check --phase pre-closeout`、`python3 scripts/foreman.py validate <TASK_ID>`。
- closeout 后继续执行 `python3 scripts/task_audit.py --check --phase post-closeout`。

### AMV-002：扩展 SQL 结构画像

**目标**：为高级 MV 推荐提供结构化 SQL 输入，避免只靠字符串规则。

**范围**：

- 扩展解析画像，覆盖表、别名、投影、谓词、Join 图、聚合、分组、HAVING、ORDER/LIMIT、CTE、子查询、时间函数和非确定函数。
- 保留现有 L1/L2 规则行为兼容。
- 不在本任务生成 MV DDL。

**验收**：

- 单表聚合、多表 Join、CTE、子查询、时间分组、非确定函数均有测试画像。
- 旧的 `PRECOMPUTE_MV`、`PARTITION_PRUNING`、`BUCKET_JOIN` 等规则不回退。
- 画像输出可被后续任务直接消费。

### AMV-003：实现谓词分类器

**目标**：把 `WHERE` / `HAVING` 条件拆成参数外提、业务保留、安全边界和不稳定阻断四类。

**范围**：

- 新增谓词分类服务或组件。
- 识别日期、区域、渠道、客户、商品、租户等参数谓词，`tenant_id` 归入 `EXTERNALIZED_PARAMETER_PREDICATE`。
- 识别 `status`、`is_deleted` 等业务固定谓词。
- 识别权限域、数据域、访问域等显式非租户安全谓词。
- 识别当前时间、随机函数、会话函数等不稳定谓词。

**验收**：

- 每类谓词有单元测试。
- 分类结果进入 `accelerationArtifact`。
- 不稳定谓词触发结构化 `blockingReasons`。
- 显式非租户安全谓词一旦出现必须进入 `securityPredicates`，缺少显式非租户安全谓词不得单独阻断 `GENERATED`。

### AMV-004：实现粒度与指标推导模型

**目标**：根据 SQL 结构推导 MV 粒度、维度和可重聚合指标。

**范围**：

- 推导 `grain`、`dimensions`、`measures`。
- 支持 `SUM`、`COUNT`、`MIN`、`MAX`。
- 将 `AVG` 拆成 `SUM` + `COUNT`。
- 比例类指标拆成分子和分母。
- 对 `COUNT(DISTINCT)`、百分位、中位数、复杂 UDAF 输出阻断或人工复核。

**验收**：

- 日/月 rollup、普通聚合、AVG 拆解、比例指标均有测试。
- 不可合并指标不会生成可发布 rewrite。
- 产物中清楚展示指标来源表达式与 rewrite 表达式。

### AMV-005：实现 PARAMETERIZED_AGG_MV 候选生成

**目标**：针对聚合报表生成参数外提型聚合 MV。

**范围**：

- 根据谓词分类结果把参数字段提升为 MV 维度。
- 根据粒度与指标推导生成 DDL、刷新、验证、回滚和 rewrite SQL。
- 生成覆盖证明。
- 保留 `PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE`。

**验收**：

- 日期范围、区域、渠道、客户等参数变化 SQL 能生成同一 MV 形态。
- rewrite SQL 在 MV 上重新过滤和二次聚合。
- `SELECT *`、不可合并指标、不稳定谓词、安全字段缺失会阻断。

### AMV-006：实现 PREJOIN_MV 候选生成

**目标**：针对 Join 成本高且查询参数变化的 SQL 生成预 Join 宽表 MV。

**范围**：

- 从 Join 图中抽取可物化的等值 Join。
- 生成字段去重、别名消歧和 Join key 保留策略。
- 输出 Join 图证据、行数放大风险说明和 rewrite SQL。
- 不处理不可证明安全的 `CROSS JOIN`、复杂非等值 Join。

**验收**：

- 多表等值 Join SQL 生成预 Join MV。
- rewrite SQL 不再访问原 Join 表，而是查询 MV。
- outer join、多对多风险、字段冲突均有阻断或人工复核原因。

### AMV-007：实现 STAR_AGG_MV 候选生成

**目标**：针对事实表 Join 维表后聚合的 SQL 生成星型聚合 MV。

**范围**：

- 基于 Join 图识别事实表倾向和维度字段。
- 将 Join 后聚合维度、时间字段和指标写入 MV。
- 生成可二次聚合 rewrite SQL。
- 输出事实表、维表、Join key、维度来源和指标来源证据。

**验收**：

- Join 数量 >= 2 且存在聚合函数的报表 SQL 能生成候选。
- 查询按维表字段切片时可以改写到 MV。
- 事实表无法识别、粒度不覆盖、指标不可合并时阻断。

### AMV-008：实现 ROLLUP_MV 候选生成

**目标**：支持日、周、月、季度等时间粒度的二次聚合复用。

**范围**：

- 识别时间字段与时间表达式。
- 推导最细稳定 MV 时间粒度。
- 生成从细粒度 MV 聚合到粗粒度查询的 rewrite SQL。
- 与 `PARAMETERIZED_AGG_MV` 可组合，但产物 `mvType` 必须明确。

**验收**：

- 日粒度 MV 可覆盖月粒度查询。
- `AVG` 不会被直接二次平均，必须拆成 `SUM` + `COUNT`。
- 时间表达式不可归一时阻断。

### AMV-009：实现 COMMON_SUBGRAPH_MV 候选生成

**目标**：针对复杂 CTE、派生表和重复子查询生成公共子图 MV。

**范围**：

- 抽取可独立物化的 CTE 或派生表。
- 判断子图输出能否覆盖上层查询。
- 生成子图 MV DDL 与上层 rewrite SQL。
- 对相关子查询、窗口函数、非确定函数给出阻断或人工复核。

**验收**：

- 多 CTE 报表可生成公共子图 MV。
- 上层查询 rewrite 到公共子图 MV。
- 不能独立物化的子查询不会生成 `GENERATED`。

### AMV-010：实现方言渲染与命名规范

**目标**：为 `HETU`、`HIVE`、`SPARK` 渲染高级 MV DDL、刷新和回滚 SQL，并统一 MV 命名。

**范围**：

- 新增或扩展方言渲染组件。
- 命名基于逻辑对象、主表、粒度、维度和 SQL 指纹短 hash，避免超长与冲突。
- 渲染 `CREATE MATERIALIZED VIEW`、`REFRESH MATERIALIZED VIEW`、`DROP MATERIALIZED VIEW` 或引擎等价语句。
- 未支持引擎返回 `UNSUPPORTED_TARGET_ENGINE`。

**验收**：

- `HETU`、`HIVE`、`SPARK` 均有渲染测试。
- `targetEngine=AUTO` 继续返回 `TARGET_ENGINE_REQUIRED`。
- 生成 SQL 不包含未替换占位符。

### AMV-011：实现 rewrite SQL 生成与静态覆盖校验

**目标**：证明原 SQL 可以改写到 MV，并生成查询 MV 的 `rewriteSql`。

**范围**：

- 校验投影覆盖、过滤覆盖、分组覆盖、指标覆盖、安全覆盖。
- 根据 MV 字段与指标生成 rewrite SQL。
- 对不能覆盖的场景生成结构化阻断。
- 确保 rewrite SQL 是只读 SQL。

**验收**：

- 每种 MV 类型至少有一条成功 rewrite 测试。
- 覆盖缺失时不会生成 `GENERATED`。
- `rewriteSql` 必须引用 MV 名称，不能仍访问原基表。

### AMV-012：扩展 accelerationArtifact API 与持久/展示契约

**目标**：让高级 MV 产物在推荐详情、diff、加速任务和工作台中保持一致。

**范围**：

- 扩展后端 VO/DTO/JSON 字段。
- 兼容当前 `ddlSql`、`refreshSql`、`validationSql`、`rollbackSql`、`rewriteSql`。
- 新增 `mvType`、`grain`、`dimensions`、`measures`、`predicate`、`coverage`、`joinGraph` 等字段。
- 更新接口契约测试。

**验收**：

- 推荐详情和 diff 返回同一结构的高级 MV 产物。
- 旧前端在缺少新字段时不崩溃。
- `mvType=EXACT_QUERY_MV` 不出现在响应中。

### AMV-013：升级前端展示与用户动作路径

**目标**：让页面能解释高级 MV 推荐，不再只展示工程码和 SQL 块。

**范围**：

- 推荐中心和加速治理工作台展示 MV 类型、粒度、维度、指标、谓词分类、Join 图、覆盖证明、阻断原因。
- 展示 `rewriteSql` 可作为 SQL 改写记录的 `recommendedSqlText` 来源。
- 页面文案明确 SQLForge 不执行 DDL，runtime 生效需要审批发布。

**验收**：

- 每种 MV 类型有可读中文说明。
- BLOCKED / REVIEW_REQUIRED / GENERATED 状态展示清晰。
- 不把“产物生成成功”展示成“运行时已生效”。

### AMV-014：打通从 MV rewriteSql 创建改写记录的治理入口

**目标**：让高级 MV 产物能进入现有 SQL 改写记录审批发布链路，但不绕过审批。

**范围**：

- 在推荐中心或加速治理工作台提供“用 MV rewriteSql 创建改写记录”的动作。
- 创建改写记录时：
  - `originalSqlText` 来自原 SQL。
  - `recommendedSqlText` 来自 `accelerationArtifact.rewriteSql`。
  - `traceRefs` 包含 `mvType`、`mvName`、`accelerationArtifact` 摘要。
- 后续审批、发布、暂停、撤销仍走现有改写记录接口。

**验收**：

- 创建出的改写记录可进入审批流程。
- 未执行外部验证或未审批时不能自动发布。
- 运行时生效仍以 query-execution binding `ACTIVE` 为准。

### AMV-015：增强验证 SQL 与等价验证证据

**目标**：为高级 MV rewrite 提供更有价值的验证草案。

**范围**：

- 生成行数对比、聚合指标对比、关键维度分组对比。
- 对 `PARAMETERIZED_AGG_MV` 和 `ROLLUP_MV` 生成原 SQL 与 rewrite SQL 的对比查询。
- 对 `PREJOIN_MV` 和 `STAR_AGG_MV` 生成 Join 后行数、指标和分组差异检查草案。
- 不直接执行验证 SQL。

**验收**：

- 验证 SQL 同时包含原查询与 MV rewrite 查询。
- 验证 SQL 可读、可复制、字段来源清楚。
- 无法生成验证 SQL 时必须输出阻断原因。

### AMV-016：补齐测试 SQL、契约测试和 smoke

**目标**：为后续实现提供完整测试样例和回归边界。

**范围**：

- 新增各 MV 类型的测试 SQL 样例。
- 覆盖成功、阻断、人工复核三类样例。
- 补充后端单元测试、接口契约测试、前端 contract 或 smoke。
- 更新文档中的测试说明。

**验收**：

- 每种 MV 类型至少有成功样例和阻断样例。
- `EXACT_QUERY_MV` 作为非法类型有测试防回归。
- `python3 scripts/foreman.py validate <TASK_ID>` 通过。

## 推荐执行顺序

1. `AMV-001` 固化契约和边界。
2. `AMV-002` 至 `AMV-004` 建立 SQL 结构、谓词、粒度和指标基础。
3. `AMV-005` 与 `AMV-008` 优先实现聚合和 Rollup，因为收益高且 rewrite 语义最清晰。
4. `AMV-006` 与 `AMV-007` 实现 Join 与星型聚合。
5. `AMV-009` 实现公共子图。
6. `AMV-010` 与 `AMV-011` 完成方言渲染和 rewrite 覆盖校验。
7. `AMV-012` 与 `AMV-013` 完成 API 与页面展示。
8. `AMV-014` 接入现有 SQL 改写记录治理链路。
9. `AMV-015` 与 `AMV-016` 补齐验证和回归测试。

## 总体验收标准

- 系统不再输出 `EXACT_QUERY_MV`。
- 至少支持 `PARAMETERIZED_AGG_MV`、`PREJOIN_MV`、`STAR_AGG_MV`、`ROLLUP_MV`、`COMMON_SUBGRAPH_MV` 的结构化候选。
- 每个候选都有 MV 类型、粒度、维度、指标、谓词分类、覆盖证明、阻断原因和治理边界。
- `GENERATED` 候选必须提供 DDL、刷新、验证、回滚和 rewrite SQL。
- `rewriteSql` 必须查询 MV，并能解释原 SQL 是如何由 MV 子集或二次聚合得到。
- 页面不得把产物生成展示成生产生效。
- 生产生效仍必须通过 SQL 改写记录审批发布和 query-execution runtime binding。
