# SQL Rewrite Recommendation Research 2026-05-18

本文件归档 `USER-CN-SELECT-REWRITE-36-20260518` 的外部调研依据。它是参考资料和实现解释，不替代 `docs/plans/document-truth-baseline.md` 对当前实现事实的定义。

## Sources Reviewed

- Apache Calcite materialized view rewrite: <https://calcite.apache.org/docs/materialized_views.html>
  - 关键点：Calcite 把 MV 注册到优化器后，可基于视图替换和结构信息改写 query；结构规则覆盖 Join、Filter、Project、Aggregate、Union、partial rewrite 和 rollup。
- Trino cost-based optimizer: <https://trino.io/docs/current/optimizer/cost-based-optimizations.html>
  - 关键点：join enumeration 依赖 connector 表统计；无统计时只能降级，不能把静态建议写成确定收益。
- Trino dynamic filtering: <https://trino.io/docs/current/admin/dynamic-filtering.html>
  - 关键点：选择性维表过滤可在 join 运行时生成动态过滤，并在 Hive connector 等场景做动态分区裁剪。
- StarRocks async MV rewrite: <https://docs.starrocks.io/docs/using_starrocks/async_mv/use_cases/query_rewrite_with_materialized_views/>
  - 关键点：透明改写基于 SPJG 形态，适合高维预聚合、宽表 join 和 lakehouse 加速。
- BigQuery materialized view smart tuning: <https://docs.cloud.google.com/bigquery/docs/materialized-views-intro>
  - 关键点：当 query 的一部分可由 MV 解析时，系统可 reroute；freshness 不满足时回读基表增量或基表本身。
- Databricks materialized views: <https://docs.databricks.com/aws/en/ldp/dbsql/materialized>
  - 关键点：刷新可增量或全量，是否增量取决于 query 结构和源数据能力，需用 EXPLAIN/运行证据验证。
- Snowflake performance options: <https://docs.snowflake.com/en/user-guide/performance-query-options>
  - 关键点：MV、search optimization、query acceleration、clustering 分别覆盖 equality/range/search/sort/filter/aggregation 等不同场景。
- Azure Synapse materialized view recommendations: <https://learn.microsoft.com/en-us/azure/synapse-analytics/sql-data-warehouse/performance-tuning-materialized-views>
  - 关键点：优化器可透明使用 MV，并提供 `EXPLAIN WITH_RECOMMENDATIONS`；建议需要结合 workload 频率、成本和维护开销评估。
- Patent `US7991765B2`: <https://patents.google.com/patent/US7991765B2/en>
  - 关键点：cost-based MV rewrite 先判断列、行范围等适用性，再决策是否替换基表。
- Patent `US11868347B1`: <https://patents.google.com/patent/US11868347B1/en>
  - 关键点：当 MV stale 时，用 compensation view / delta query 把变更并入结果，保证 up-to-date 结果。
- Patent `US20220309063A1`: <https://patents.google.com/patent/US20220309063A1/en>
  - 关键点：logical partition change tracking 支持部分 stale MV 的 full/partial text match、delta join 和 rewrite hints。
- Rulescript 2026: <https://arxiv.org/abs/2605.05536>
  - 关键点：新近研究强调 rewrite rule 的匹配阶段和转换阶段分离，并追求 engine-agnostic / verifiable rule DSL。
- Efficient Cost-Based Rewrite in a Bottom-Up Optimizer 2026: <https://arxiv.org/abs/2605.05044>
  - 关键点：2026-05 新近研究指出 QRW 与 CBO 分离会让部分规则在无成本上下文时误判收益；可用中间 CBO 结果缓存和上界剪枝降低 cost-dependent rewrite 的优化开销。
- LASER / SQL-GRPO 2026: <https://arxiv.org/abs/2604.06804>
  - 关键点：2026-04 新近研究用执行验证的慢 SQL 语料、规则引导反模式扩展和 SQL-GRPO 训练小模型发现延迟敏感改写；工程落点是候选生成和离线 rule mining，不是生产自动改写。
- SLER learning-to-rank rule discovery 2026: <https://arxiv.org/abs/2603.04169>
  - 关键点：2026-03 新近研究通过标准化计划模板和 learning-to-rank 缩小规则枚举空间，并在真实 SQL workload 上生成大规模 rewrite rule 库；对 SQLForge 的价值是补充规则发现 backlog，而不是绕过等价性/收益验证。
- Trino 481 release notes: <https://trino.io/docs/current/release/release-481.html>
  - 关键点：2026-05 release 继续围绕选择性 `AND`/`OR` 谓词、未知统计 join ordering、fresh materialized view、partition predicate 和 connector pushdown 优化；说明 BI 场景的收益仍高度依赖统计、connector 和引擎版本。
- Snowflake 2026 performance improvements: <https://docs.snowflake.com/en/release-notes/performance-improvements-2026>
  - 关键点：2026 官方记录覆盖 runtime pruning、skew join handling、QAS 并行扫描、CTE early aggregation、view subcolumn pushdown、range pruning、structured search optimization 和 CASE 编译优化；这些强化了 SQLForge 仅输出建议并要求外部引擎证据的边界。
- CIDR 2026 LLM rewrite verification: <https://www.vldb.org/cidrdb/papers/2026/p33-narasayya.pdf>
  - 关键点：LLM rewrite 有收益潜力，但约 32% 结果不等价；工程上必须借助优化器或结果验证证明等价。
- E3-Rewrite 2025: <https://arxiv.org/abs/2508.09023>
  - 关键点：LLM rewrite 必须同时满足 executable、equivalent、efficient，不能直接把 LLM 输出进入生产自动改写。

## SQLForge Design Implications

- Rule output remains layered:
  - `L0` safe syntax rewrites may produce candidate SQL, but still require result diff before approval.
  - `L1` structural rewrites are emitted as `unappliedRules` with required proof and semantic risk.
  - `L2` engine / physical recommendations are `PULL_ONLY_CANDIDATE`; SQLForge does not create MV, repartition data, compact files, or force engine config.
- Rewrite recommendation payloads must carry the same production-scale gate as benchmark evidence: 30PB data layout, 10000 concurrency, 24h replay, P95/P99, scan bytes, CPU, queue wait and cost bill require external `VERIFIED` artifacts before any BI-scale readiness claim.
- For BI-scale workloads, recommendation coverage must include SPJG/MV, join statistics, dynamic filtering, partition/freshness compensation, result cache, file layout, semi-structured extraction, window Top-N, set operations, and pagination.
- LLM or patent-inspired rewrite paths may become future candidate generation sources, but this task keeps the core deterministic and static-parse-based. Any generated SQL remains blocked by SQLForge approval and validation gates.
- 2026 cost-dependent / learned rewrite research strengthens the same boundary: SQLForge may use learned systems or cost-aware papers to prioritize future rule candidates, but a BI-scale recommendation still needs optimizer evidence, workload replay, cost bill and result equivalence before being considered production-ready.
- Recent Trino and Snowflake releases show that many effective optimizations now live in engine/runtime behavior such as pruning, skew handling, pushdown, QAS, MV freshness and CTE aggregation placement. SQLForge should therefore surface engine coordination and proof requirements instead of claiming static SQL text changes alone can guarantee 30PB / 10M daily-query benefit.
- `USER-CN-SELECT-REWRITE-50-20260518` extends the same deterministic boundary: new scenarios improve parse-time detection and recommendation evidence for additional common SELECT shapes, but still do not execute SQL, create materialized views, change storage layout, or allow automatic application without validation.

## Implemented Mapping In These Tasks

`USER-CN-SELECT-REWRITE-36-20260518` first expanded `SqlOptimizationPipelineService` to 36+ distinct SELECT recommendation scenarios across safe, structural, and physical categories. `USER-CN-SELECT-REWRITE-50-20260518` raises the auditable regression target to at least 50 explicit SELECT execution / parsing / recommendation rewrite scenarios.

- Safe candidate SQL: `COUNT_ONE_TO_COUNT_STAR`, `DEDUPLICATE_WHERE_PREDICATES`, `DEDUPLICATE_HAVING_PREDICATES`, `DUPLICATE_GROUP_ORDER_KEY`.
- Structural/manual review: `SELECT_STAR_EXPANSION`, `OR_TO_UNION_ALL`, `FUNCTION_PREDICATE_TO_RANGE`, `SCALAR_SUBQUERY_TO_JOIN`, `REPEATED_SUBQUERY_TO_CTE`, `NOT_EXISTS_TO_ANTI_JOIN`, `LEADING_LIKE_REVIEW`, `ORDER_RANDOM_REVIEW`, `DISTINCT_DEDUP_REVIEW`, `GROUP_BY_TO_DISTINCT`, `HAVING_TO_WHERE_PUSHDOWN`, `IN_SUBQUERY_TO_SEMI_JOIN`, `EXISTS_TO_SEMI_JOIN`, `NOT_IN_TO_ANTI_JOIN`, `LEFT_JOIN_NULL_TO_ANTI_JOIN`, `CROSS_JOIN_GUARD`, `CAST_JOIN_KEY_NORMALIZE`, `IMPLICIT_TYPE_CAST_REVIEW`, `LIKE_PREFIX_RANGE_REVIEW`, `REGEXP_FILTER_TO_SEARCH_INDEX`, `LONG_IN_LIST_TO_TEMP_TABLE`, `WINDOW_TOPN_REWRITE`, `UNION_DEDUP_REVIEW`, `INTERSECT_TO_SEMI_JOIN`, `EXCEPT_TO_ANTI_JOIN`, `JSON_EXTRACT_MATERIALIZATION`, `UNNEST_LATERAL_REVIEW`, `NULL_SAFE_EQUALITY_REVIEW`, `OFFSET_TO_KEYSET_PAGINATION`.
- Engine / physical coordination: `PRECOMPUTE_MV`, `PARTITION_PRUNING`, `BUCKET_JOIN`, `JOIN_REORDER_BY_STATS`, `DYNAMIC_FILTERING_JOIN`, `STAR_SCHEMA_MV`, `SPLIT_SQL`, `RESULT_CACHE`, `REPORT_SQL_MERGE`, `STATISTICS_REFRESH`, `FILE_COMPACTION`, `PROJECTION_PRUNING`, `PREDICATE_PUSHDOWN`, `TOPN_PUSHDOWN`, `SMALL_TABLE_BROADCAST_JOIN`, `SKEW_JOIN_SALTING_REVIEW`, `PIVOT_AGGREGATE_PRECOMPUTE`, `DATE_GRANULARITY_MV`, `PARTITION_COMPENSATION_UNION`, `SEMISTRUCTURED_COLUMN_INDEX`.
- `USER-CN-SELECT-REWRITE-50-20260518` adds explicit coverage for additional common and complex SELECT shapes: `FULL_SCAN_FILTER_GUARD`, `ORDER_BY_WITHOUT_LIMIT_GUARD`, `LIMIT_WITHOUT_ORDER_GUARD`, `REPEATED_EXPRESSION_TO_CTE`, `UDF_EVALUATION_ISOLATION`, `STRING_CONCAT_PRECOMPUTE`, `LARGE_STRING_AGGREGATE_OFFLOAD`, `WINDOW_FRAME_PRECOMPUTE`, `CTE_MATERIALIZATION_POLICY`, `CASE_EXPRESSION_NORMALIZATION`, `MULTI_COUNT_DISTINCT_DECOMPOSITION`, `NEGATION_FILTER_REVIEW`, `NULL_FILTER_INDEX_REVIEW`, `ORDER_BY_EXPRESSION_PRECOMPUTE`, `ARRAY_CONTAINS_INDEX_REVIEW`, `RANGE_JOIN_BUCKETIZATION`, `DISTINCT_ORDER_BY_ALIGNMENT`, `CORRELATED_SUBQUERY_DECORRELATION`, `NESTED_SUBQUERY_FLATTENING`, `APPROX_DISTINCT_SKETCH_MV`, `PERCENTILE_SKETCH_PRECOMPUTE`, `ROLLUP_AGGREGATE_LATTICE`.
- `USER-CN-REWRITE-PRODUCTION-SCALE-GATE-20260518` adds `productionScaleGate` into rewrite recommendation benefit/cost payloads so downstream persisted recommendations do not treat static rewrite coverage or EXPLAIN-only evidence as production 30PB / 10M daily query readiness.

Coverage evidence is held by `SqlOptimizationPipelineServiceTest.shouldExposeAtLeastFiftySelectRewriteRecommendationScenarios`.
