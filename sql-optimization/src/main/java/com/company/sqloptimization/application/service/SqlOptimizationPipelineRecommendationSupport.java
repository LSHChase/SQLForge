package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.rewrite.production.RewriteProductionCapabilityReport;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

abstract class SqlOptimizationPipelineRecommendationSupport extends SqlOptimizationPipelineRewriteSupport {

    protected void addExtendedStructuralRuleCandidates(ParsedSqlProfile profile,
                                                     List<Map<String, Object>> unappliedRules,
                                                     List<Map<String, Object>> preconditions,
                                                     List<Map<String, Object>> semanticRisks) {
        String sql = profile.getNormalizedSql();
        if (profile.isDistinctPresent()) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "DISTINCT_DEDUP_REVIEW",
                "DUPLICATE_INTENT_REQUIRED",
                "DISTINCT 需要确认是否为业务去重还是掩盖 join 放大。",
                "去重可能隐藏数据质量问题或改变重复行语义。"
            );
        }
        if (profile.isGroupByWithoutAggregate()) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "GROUP_BY_TO_DISTINCT",
                "DEDUP_SEMANTICS_REQUIRED",
                "无聚合 GROUP BY 可试算为 DISTINCT，但需确认排序和重复语义。",
                "GROUP BY 与 DISTINCT 在部分引擎的执行计划和 NULL 表现需复核。"
            );
        }
        if (matches(HAVING_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "HAVING_TO_WHERE_PUSHDOWN",
                "AGGREGATE_DEPENDENCY_PROOF_REQUIRED",
                "HAVING 中不依赖聚合的过滤可下推到 WHERE。",
                "误下推聚合过滤会改变分组结果。"
            );
        }
        if (matches(IN_SUBQUERY_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "IN_SUBQUERY_TO_SEMI_JOIN",
                "NULL_AND_DUPLICATE_SEMANTICS_REQUIRED",
                "IN 子查询可评审为 semi join，以便优化器使用 join 侧过滤。",
                "子查询 NULL、重复值和关联条件可能改变匹配语义。"
            );
        }
        if (matches(EXISTS_SUBQUERY_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "EXISTS_TO_SEMI_JOIN",
                "CORRELATION_SCOPE_REQUIRED",
                "EXISTS 子查询可评审为 semi join 或动态过滤候选。",
                "关联范围不清会导致行数放大或漏匹配。"
            );
        }
        if (matches(NOT_IN_SUBQUERY_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NOT_IN_TO_ANTI_JOIN",
                "NULL_SEMANTICS_PROOF_REQUIRED",
                "NOT IN 子查询可评审为 null-safe anti join。",
                "NOT IN 遇到 NULL 时语义敏感，不能自动改写。"
            );
        }
        if (matches(LEFT_JOIN_NULL_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LEFT_JOIN_NULL_TO_ANTI_JOIN",
                "JOIN_KEY_NULLABILITY_REQUIRED",
                "LEFT JOIN ... IS NULL 可评审为 anti join。",
                "右表重复键或 NULL 过滤位置可能改变反连接结果。"
            );
        }
        if (matches(CROSS_JOIN_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CROSS_JOIN_GUARD",
                "CARTESIAN_INTENT_REQUIRED",
                "CROSS JOIN 需要确认是否为真实笛卡尔意图或遗漏 join 条件。",
                "误保留笛卡尔积会造成行数爆炸。"
            );
        }
        if (matches(CAST_COMPARISON_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CAST_JOIN_KEY_NORMALIZE",
                "COLUMN_TYPE_AND_LOSSLESS_CAST_REQUIRED",
                "比较或 join key 上的 CAST 可评审为类型归一后的列比较。",
                "非无损转换或精度差异会改变匹配结果。"
            );
        }
        if (matches(STRING_NUMERIC_COMPARISON_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "IMPLICIT_TYPE_CAST_REVIEW",
                "COLUMN_TYPE_REQUIRED",
                "疑似字符串数字比较需要显式化类型或绑定参数类型。",
                "隐式类型转换在不同引擎之间可能导致结果或性能差异。"
            );
        }
        if (matches(PREFIX_LIKE_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LIKE_PREFIX_RANGE_REVIEW",
                "COLLATION_AND_ESCAPE_RULE_REQUIRED",
                "前缀 LIKE 可评审为范围谓词或前缀索引候选。",
                "排序规则、转义字符和大小写规则会影响范围边界。"
            );
        }
        if (matches(REGEXP_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "REGEXP_FILTER_TO_SEARCH_INDEX",
                "TEXT_INDEX_CAPABILITY_REQUIRED",
                "正则过滤可推荐搜索索引或预计算标签列。",
                "正则语义和分词规则依赖引擎，不能静态保证等价。"
            );
        }
        if (matches(LONG_IN_LIST_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LONG_IN_LIST_TO_TEMP_TABLE",
                "VALUE_SET_CARDINALITY_REQUIRED",
                "长 IN 列表可试算为临时值表或半连接。",
                "值集去重、类型绑定和权限边界需要复核。"
            );
        }
        if (matches(ROW_NUMBER_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "WINDOW_TOPN_REWRITE",
                "PARTITION_ORDER_DETERMINISM_REQUIRED",
                "ROW_NUMBER Top-N 形态可评审为分组 Top-N 或服务化预计算。",
                "排序不稳定或并列值处理会改变被保留的行。"
            );
        }
        if (matches(UNION_DISTINCT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "UNION_DEDUP_REVIEW",
                "DUPLICATE_POLICY_REQUIRED",
                "UNION 可评审是否改为 UNION ALL 后单独去重或保留重复。",
                "重复行策略属于业务语义，不能自动决定。"
            );
        }
        if (matches(INTERSECT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "INTERSECT_TO_SEMI_JOIN",
                "SET_SEMANTICS_REQUIRED",
                "INTERSECT 可评审为 semi join 或存在性过滤。",
                "集合去重和 NULL 处理必须与原语句一致。"
            );
        }
        if (matches(EXCEPT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "EXCEPT_TO_ANTI_JOIN",
                "SET_DIFFERENCE_SEMANTICS_REQUIRED",
                "EXCEPT/MINUS 可评审为 anti join 或差集预计算。",
                "集合去重、NULL 和列顺序规则必须校验。"
            );
        }
        if (matches(JSON_EXTRACT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "JSON_EXTRACT_MATERIALIZATION",
                "JSON_PATH_AND_TYPE_CONTRACT_REQUIRED",
                "JSON/VARIANT 提取可推荐物化为治理字段或展开视图。",
                "路径缺失、数组语义和类型转换可能改变结果。"
            );
        }
        if (matches(UNNEST_LATERAL_PATTERN, sql) || matches(SEMI_STRUCTURED_FLATTEN_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "UNNEST_LATERAL_REVIEW",
                "ARRAY_CARDINALITY_AND_NULL_POLICY_REQUIRED",
                "UNNEST/LATERAL/FLATTEN 可评审为预展开或半结构化索引候选。",
                "数组空值、重复元素和外连接展开语义需要校验。"
            );
        }
        addExtendedStructuralResourceRuleCandidates(profile, unappliedRules, preconditions, semanticRisks, sql);
    }

    protected void addExtendedStructuralResourceRuleCandidates(ParsedSqlProfile profile,
                                                               List<Map<String, Object>> unappliedRules,
                                                               List<Map<String, Object>> preconditions,
                                                               List<Map<String, Object>> semanticRisks,
                                                               String sql) {
        if (matches(NULL_SAFE_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NULL_SAFE_EQUALITY_REVIEW",
                "NULL_SENTINEL_AND_TYPE_REQUIRED",
                "COALESCE/NVL/NULL-safe 比较可评审为等价空值规范化。",
                "哨兵值与真实数据冲突会改变匹配或过滤结果。"
            );
        }
        if (matches(OFFSET_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "OFFSET_TO_KEYSET_PAGINATION",
                "STABLE_SORT_KEY_REQUIRED",
                "OFFSET 分页可评审为 keyset/seek 分页。",
                "缺少稳定排序键会改变翻页边界或漏/重复记录。"
            );
        }
        if (profile.getPredicateCount() == 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "FULL_SCAN_FILTER_GUARD",
                "BUSINESS_FILTER_OR_READ_SCOPE_REQUIRED",
                "无过滤 SELECT 必须补充业务范围、租户范围或只读扫描豁免证据。",
                "在 PB 级数据上全表扫描会造成资源争抢，且可能越过预期数据范围。"
            );
        }
        if (profile.getOrderByCount() > 0 && !profile.isLimitPresent()) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ORDER_BY_WITHOUT_LIMIT_GUARD",
                "TOPN_OR_CONSUMER_SORT_REQUIREMENT_REQUIRED",
                "无 LIMIT 排序需要确认消费者是否真的需要全量有序结果。",
                "全量排序可能引入宽 shuffle、spill 和长尾资源占用。"
            );
        }
        if (profile.isLimitPresent() && profile.getOrderByCount() == 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LIMIT_WITHOUT_ORDER_GUARD",
                "STABLE_ORDERING_INTENT_REQUIRED",
                "无 ORDER BY 的 LIMIT 需要确认是否允许非确定性抽样结果。",
                "不同执行计划可能返回不同前 N 行，影响报表可复现性。"
            );
        }
        if (profile.getRepeatedExpressionCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "REPEATED_EXPRESSION_TO_CTE",
                "EXPRESSION_DETERMINISM_REQUIRED",
                "重复复杂表达式可评审为 CTE、派生列或服务层预计算。",
                "非确定性函数或引擎 CTE 内联行为可能改变性能和结果稳定性。"
            );
        }
        if (profile.getUdfFunctionCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "UDF_EVALUATION_ISOLATION",
                "UDF_DETERMINISM_AND_COST_REQUIRED",
                "自定义函数应评审为前置过滤后执行、离线派生列或受治理服务函数。",
                "UDF 的确定性、权限和资源消耗不能由静态解析证明。"
            );
        }
        if (profile.getStringConcatenationCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "STRING_CONCAT_PRECOMPUTE",
                "OUTPUT_FORMAT_CONTRACT_REQUIRED",
                "高频字符串拼接可评审为展示层格式化或预计算展示字段。",
                "空值、分隔符和字符集规则会影响输出等价性。"
            );
        }
        if (profile.getLargeStringAggregateCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LARGE_STRING_AGGREGATE_OFFLOAD",
                "AGGREGATE_LENGTH_AND_ORDER_POLICY_REQUIRED",
                "大字符串聚合可评审为明细下钻、离线摘要或受限长度聚合。",
                "聚合顺序、截断策略和超长输出会影响业务可读性与内存占用。"
            );
        }
        if (profile.getWindowFunctionCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "WINDOW_FRAME_PRECOMPUTE",
                "WINDOW_FRAME_AND_ORDER_CONTRACT_REQUIRED",
                "窗口函数可评审为分区预计算、指标层快照或 Top-N 专项改写。",
                "窗口边界、排序并列值和分区基数会影响结果。"
            );
        }
        if (matches(WITH_CLAUSE_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CTE_MATERIALIZATION_POLICY",
                "ENGINE_CTE_MATERIALIZATION_BEHAVIOR_REQUIRED",
                "WITH 查询需要确认引擎会内联还是物化 CTE，并评审复用收益。",
                "不同引擎的 CTE 策略不同，可能重复扫描或提前物化大量中间结果。"
            );
        }
        if (matches(CASE_EXPRESSION_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CASE_EXPRESSION_NORMALIZATION",
                "CASE_BRANCH_EXCLUSIVITY_REQUIRED",
                "CASE 表达式可评审为维表映射、派生字段或指标口径统一。",
                "分支顺序、NULL 和兜底值会影响结果等价性。"
            );
        }
        if (countMatches(COUNT_DISTINCT_PATTERN, sql) >= 2) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "MULTI_COUNT_DISTINCT_DECOMPOSITION",
                "DISTINCT_CARDINALITY_AND_DEDUP_POLICY_REQUIRED",
                "多个 COUNT DISTINCT 可评审为分阶段聚合或 sketch 预计算。",
                "去重粒度和近似算法选择会影响精度与成本。"
            );
        }
        if (matches(NOT_EQUAL_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NEGATION_FILTER_REVIEW",
                "SELECTIVITY_AND_NULL_POLICY_REQUIRED",
                "否定谓词可评审为正向枚举、排除表或分区剪枝辅助条件。",
                "否定谓词通常选择性弱，且 NULL 语义容易被误改。"
            );
        }
        if (matches(IS_NULL_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NULL_FILTER_INDEX_REVIEW",
                "NULL_DISTRIBUTION_AND_INDEX_SUPPORT_REQUIRED",
                "IS NULL/IS NOT NULL 可评审为空值分布统计、索引或派生标记列。",
                "空值分布高度倾斜时，错误索引或物化策略可能收益很低。"
            );
        }
        if (matches(ORDER_BY_FUNCTION_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ORDER_BY_EXPRESSION_PRECOMPUTE",
                "SORT_EXPRESSION_DETERMINISM_REQUIRED",
                "函数排序键可评审为派生排序列或已排序服务对象。",
                "排序表达式的时区、大小写和 NULL 顺序需要与原语句一致。"
            );
        }
        if (matches(ARRAY_FUNCTION_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ARRAY_CONTAINS_INDEX_REVIEW",
                "ARRAY_PATH_AND_CARDINALITY_REQUIRED",
                "数组包含或基数过滤可评审为预展开、倒排索引或半结构化字段索引。",
                "数组顺序、重复元素和空数组语义会影响过滤结果。"
            );
        }
        if (matches(RANGE_JOIN_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "RANGE_JOIN_BUCKETIZATION",
                "RANGE_OVERLAP_AND_BUCKET_POLICY_REQUIRED",
                "范围 join 可评审为时间桶、区间索引或预展开桥表。",
                "区间重叠、边界闭开和桶粒度错误会改变匹配关系。"
            );
        }
        if (profile.isDistinctPresent() && profile.getOrderByCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "DISTINCT_ORDER_BY_ALIGNMENT",
                "ORDER_KEY_PROJECTION_COMPATIBILITY_REQUIRED",
                "DISTINCT + ORDER BY 需要确认排序键与去重投影兼容。",
                "排序键不在去重粒度内时，不同引擎可能产生不稳定排序或额外去重成本。"
            );
        }
        if (profile.getCorrelatedSubqueryCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CORRELATED_SUBQUERY_DECORRELATION",
                "CORRELATION_KEY_UNIQUENESS_REQUIRED",
                "关联子查询可评审为 decorrelation、semi join 或预聚合 join。",
                "关联键不唯一或谓词作用域不清会导致行数放大。"
            );
        }
        if (profile.getNestedSubqueryDepth() >= 2) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NESTED_SUBQUERY_FLATTENING",
                "NESTED_SCOPE_AND_NULL_POLICY_REQUIRED",
                "多层嵌套子查询可评审为分层 CTE 或显式 join 图。",
                "嵌套作用域、NULL 和聚合边界不清时，扁平化会改变结果。"
            );
        }
    }

    protected void addL2RuleCandidates(ParsedSqlProfile profile,
                                     List<Map<String, Object>> ruleChain,
                                     List<Map<String, Object>> preconditions,
                                     List<Map<String, Object>> semanticRisks) {
        if (!profile.getAggregateFunctions().isEmpty() || profile.getGroupByCount() > 0) {
            ruleChain.add(ruleEntry(
                "L2",
                "PRECOMPUTE_MV",
                "PULL_ONLY_CANDIDATE",
                MaterializedViewRecommendationPlanner.SOURCE_AST_IR,
                Boolean.FALSE,
                "高复用聚合形态进入 AST/IR/QBDAG/关系代数驱动的物化视图候选规划。"
            ));
            preconditions.add(preconditionEntry(
                "PRECOMPUTE_MV",
                "MV_COVERAGE_PROOF_AND_REFRESH_POLICY_REQUIRED",
                "分发前需要覆盖证明、运行时频次、新鲜度目标和刷新责任归属。"
            ));
        }
        if (!profile.getDatePredicateColumns().isEmpty()) {
            ruleChain.add(ruleEntry(
                "L2",
                "PARTITION_PRUNING",
                "PULL_ONLY_CANDIDATE",
                "STATIC_PARSE",
                Boolean.FALSE,
                "日期类谓词表明存在分区裁剪或分区键推荐机会。"
            ));
            preconditions.add(preconditionEntry(
                "PARTITION_PRUNING",
                "PARTITION_METADATA_REQUIRED",
                "外部执行前必须检查分区键元数据和当前存储布局。"
            ));
        }
        if (profile.getJoinCount() > 0) {
            ruleChain.add(ruleEntry(
                "L2",
                "BUCKET_JOIN",
                "PULL_ONLY_CANDIDATE",
                "STATIC_PARSE",
                Boolean.FALSE,
                "join 活动表明存在分桶对齐或共置推荐候选。"
            ));
            preconditions.add(preconditionEntry(
                "BUCKET_JOIN",
                "JOIN_KEY_DISTRIBUTION_REQUIRED",
                "修改分桶前需要 join 键稳定性、数据倾斜和存储责任归属证据。"
            ));
            semanticRisks.add(semanticRiskEntry(
                "BUCKET_JOIN",
                "PHYSICAL_LAYOUT_RISK",
                "MEDIUM",
                "分桶或共置变更属于外部物理协同，不能仅凭静态解析自动应用。"
            ));
        }
    }

    protected void addExtendedPhysicalRuleCandidates(ParsedSqlProfile profile,
                                                   List<Map<String, Object>> ruleChain,
                                                   List<Map<String, Object>> preconditions,
                                                   List<Map<String, Object>> semanticRisks) {
        String sql = profile.getNormalizedSql();
        if (profile.getJoinCount() >= 2) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "JOIN_REORDER_BY_STATS",
                "TABLE_STATISTICS_REQUIRED",
                "多 join 查询应使用表统计信息评估 join reorder。",
                "统计信息过期会让成本模型选择次优计划。"
            );
        }
        if (profile.getJoinCount() > 0 && profile.getPredicateCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "DYNAMIC_FILTERING_JOIN",
                "CONNECTOR_DYNAMIC_FILTER_SUPPORT_REQUIRED",
                "选择性维表过滤可推荐动态过滤或动态分区裁剪。",
                "connector 不支持时该建议不能被写成确定收益。"
            );
        }
        if (profile.getJoinCount() >= 2 && profile.getAggregateFunctionCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "STAR_SCHEMA_MV",
                "FACT_DIMENSION_GRAIN_REQUIRED",
                "宽表 join 加聚合可推荐星型模型物化视图或指标层。",
                "粒度、刷新和维表缓慢变化处理必须由治理流程确认。"
            );
        }
        if (profile.getComplexGraphScore() >= 5 || profile.isSetOperation()) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SPLIT_SQL",
                "STAGE_BOUNDARY_AND_IDEMPOTENCY_REQUIRED",
                "复杂查询图可拆为 CTE、临时服务对象或多阶段执行。",
                "阶段边界不当会增加中间数据量或破坏一致性。"
            );
        }
        if (profile.isLimitPresent()
            || profile.getAggregateFunctionCount() > 0
            || profile.getDatePredicateColumns().size() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "RESULT_CACHE",
                "SCHEMA_VERSION_AND_FRESHNESS_POLICY_REQUIRED",
                "稳定筛选、聚合或分页查询可推荐受治理结果缓存。",
                "缓存命中必须受 schemaVersion、新鲜度和租户边界约束。"
            );
        }
        if (profile.getRepeatedTableScanCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "REPORT_SQL_MERGE",
                "SOURCE_REPORT_AND_OVERLAP_EVIDENCE_REQUIRED",
                "同源报表或同 SQL 多次扫描可推荐合并查询或共享中间结果。",
                "合并多条 SQL 可能改变审计粒度和失败隔离边界。"
            );
        }
        if (profile.getJoinCount() >= 2
            || profile.getPredicateCount() >= 4
            || profile.getAggregateFunctionCount() >= 2) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "STATISTICS_REFRESH",
                "EXPLAIN_OR_RUNTIME_PLAN_EVIDENCE_REQUIRED",
                "复杂 join、谓词或聚合查询应检查统计信息刷新。",
                "没有真实计划证据时只能作为外部执行建议。"
            );
        }
        if (profile.getDatePredicateColumns().size() > 0 && profile.getPredicateCount() >= 2) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "FILE_COMPACTION",
                "FILE_LAYOUT_AND_SMALL_FILE_EVIDENCE_REQUIRED",
                "分区过滤查询可联动检查小文件合并和布局整理。",
                "文件整理属于外部存储操作，不能由 SQL 推荐自动执行。"
            );
        }
        if (profile.getProjectionCount() >= 6 || profile.getStringProjectionCount() >= 3) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PROJECTION_PRUNING",
                "DOWNSTREAM_COLUMN_USAGE_REQUIRED",
                "宽投影可推荐列裁剪或服务层字段白名单。",
                "裁剪列需要证明下游消费者不依赖被移除字段。"
            );
        }
        if (profile.getPredicateCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PREDICATE_PUSHDOWN",
                "CONNECTOR_PUSHDOWN_CAPABILITY_REQUIRED",
                "可下推过滤应被保留在扫描侧或外部数据源侧。",
                "connector 能力不同，静态解析不能宣称真实下推成功。"
            );
        }
        if (profile.isLimitPresent() && profile.getOrderByCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "TOPN_PUSHDOWN",
                "ORDER_KEY_AND_CONNECTOR_SUPPORT_REQUIRED",
                "ORDER BY + LIMIT 可推荐 Top-N 下推或服务层有序输出。",
                "排序稳定性和 connector 支持必须通过计划证据确认。"
            );
        }
        if (profile.getJoinCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SMALL_TABLE_BROADCAST_JOIN",
                "BUILD_SIDE_SIZE_STATS_REQUIRED",
                "维表较小时可推荐广播 join 或 replicated layout。",
                "构建侧过大时广播会增加内存压力。"
            );
        }
        if (profile.getJoinCount() >= 2 || profile.getOrPredicateCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SKEW_JOIN_SALTING_REVIEW",
                "KEY_DISTRIBUTION_EVIDENCE_REQUIRED",
                "热点键或多 join 查询可评审倾斜处理、salt 或 AQE 策略。",
                "盲目 salt 会改变 join 代价并增加数据膨胀。"
            );
        }
        if (matches(CASE_AGGREGATION_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PIVOT_AGGREGATE_PRECOMPUTE",
                "METRIC_DEFINITION_AND_CARDINALITY_REQUIRED",
                "CASE WHEN 聚合可推荐指标层预计算或宽表透视。",
                "指标口径和稀疏维度扩展需要治理确认。"
            );
        }
        if (matches(DATE_TRUNC_PATTERN, sql) && profile.getGroupByCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "DATE_GRANULARITY_MV",
                "TIMEZONE_AND_GRAIN_POLICY_REQUIRED",
                "日期粒度聚合可推荐日/周/月指标物化视图。",
                "时区、财务日历和粒度 rollup 会影响结果。"
            );
        }
        if (profile.getDatePredicateColumns().size() > 0 && profile.getAggregateFunctionCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PARTITION_COMPENSATION_UNION",
                "PARTITION_FRESHNESS_EVIDENCE_REQUIRED",
                "分区聚合可推荐新鲜分区走基表、历史分区走物化结果的补偿 UNION。",
                "分区新鲜度和变更追踪不完整时会产生陈旧结果。"
            );
        }
        if (matches(JSON_EXTRACT_PATTERN, sql) || matches(SEMI_STRUCTURED_FLATTEN_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SEMISTRUCTURED_COLUMN_INDEX",
                "PATH_FREQUENCY_AND_INDEX_SUPPORT_REQUIRED",
                "半结构化字段热点路径可推荐展开列、搜索优化或索引化。",
                "路径类型漂移和数组展开会影响结果与成本。"
            );
        }
        if (matches(APPROX_DISTINCT_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "APPROX_DISTINCT_SKETCH_MV",
                "SKETCH_ERROR_BOUND_AND_REFRESH_POLICY_REQUIRED",
                "近似去重可推荐 HLL/sketch 物化结果或指标层复用。",
                "近似误差、合并策略和刷新周期必须由业务接受。"
            );
        }
        if (matches(PERCENTILE_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PERCENTILE_SKETCH_PRECOMPUTE",
                "PERCENTILE_ACCURACY_AND_MERGE_POLICY_REQUIRED",
                "分位数聚合可推荐 sketch 预计算或离线指标层。",
                "近似分位数不可随意合并，精度与样本边界必须验证。"
            );
        }
        if (profile.getGroupByCount() >= 2 && profile.getAggregateFunctionCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "ROLLUP_AGGREGATE_LATTICE",
                "DIMENSION_HIERARCHY_AND_ROLLUP_POLICY_REQUIRED",
                "多维聚合可推荐 rollup/lattice 物化视图或指标宽表。",
                "维度层级、钻取路径和稀疏组合会影响存储成本与结果口径。"
            );
        }
    }

    protected void addUnappliedRuleIfAbsent(List<Map<String, Object>> unappliedRules,
                                          List<Map<String, Object>> preconditions,
                                          List<Map<String, Object>> semanticRisks,
                                          String level,
                                          String rule,
                                          String missingEvidence,
                                          String preconditionDescription,
                                          String riskDescription) {
        if (containsRule(unappliedRules, rule)) {
            return;
        }
        addUnappliedRule(
            unappliedRules,
            preconditions,
            semanticRisks,
            level,
            rule,
            missingEvidence,
            preconditionDescription,
            riskDescription
        );
    }

    protected void addPullOnlyRuleIfAbsent(List<Map<String, Object>> ruleChain,
                                         List<Map<String, Object>> preconditions,
                                         List<Map<String, Object>> semanticRisks,
                                         String rule,
                                         String missingEvidence,
                                         String description,
                                         String riskDescription) {
        addPullOnlyRuleIfAbsent(
            ruleChain,
            preconditions,
            semanticRisks,
            rule,
            missingEvidence,
            description,
            riskDescription,
            "STATIC_PARSE"
        );
    }

    protected void addPullOnlyRuleIfAbsent(List<Map<String, Object>> ruleChain,
                                         List<Map<String, Object>> preconditions,
                                         List<Map<String, Object>> semanticRisks,
                                         String rule,
                                         String missingEvidence,
                                         String description,
                                         String riskDescription,
                                         String evidenceLevel) {
        if (containsRule(ruleChain, rule)) {
            return;
        }
        ruleChain.add(ruleEntry(
            "L2",
            rule,
            "PULL_ONLY_CANDIDATE",
            evidenceLevel,
            Boolean.FALSE,
            description
        ));
        preconditions.add(preconditionEntry(rule, missingEvidence, description));
        semanticRisks.add(semanticRiskEntry(rule, "PHYSICAL_OR_RUNTIME_BOUNDARY_RISK", "MEDIUM", riskDescription));
    }

    protected boolean containsRule(List<Map<String, Object>> entries, String rule) {
        if (entries == null || !StringUtils.hasText(rule)) {
            return false;
        }
        for (Map<String, Object> entry : entries) {
            if (entry != null && rule.equals(entry.get("rule"))) {
                return true;
            }
        }
        return false;
    }

    protected boolean matches(Pattern pattern, String sql) {
        return pattern != null && StringUtils.hasText(sql) && pattern.matcher(sql).find();
    }

    protected int countMatches(Pattern pattern, String sql) {
        if (pattern == null || !StringUtils.hasText(sql)) {
            return 0;
        }
        int count = 0;
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    protected void addUnappliedRule(List<Map<String, Object>> unappliedRules,
                                  List<Map<String, Object>> preconditions,
                                  List<Map<String, Object>> semanticRisks,
                                  String level,
                                  String rule,
                                  String missingEvidence,
                                  String preconditionDescription,
                                  String riskDescription) {
        addUnappliedRule(
            unappliedRules,
            preconditions,
            semanticRisks,
            level,
            rule,
            missingEvidence,
            preconditionDescription,
            riskDescription,
            Collections.<String, Object>emptyMap()
        );
    }

    protected void addUnappliedRule(List<Map<String, Object>> unappliedRules,
                                  List<Map<String, Object>> preconditions,
                                  List<Map<String, Object>> semanticRisks,
                                  String level,
                                  String rule,
                                  String missingEvidence,
                                  String preconditionDescription,
                                  String riskDescription,
                                  Map<String, Object> evidence) {
        LinkedHashMap<String, Object> unapplied = new LinkedHashMap<String, Object>();
        unapplied.put("level", level);
        unapplied.put("rule", rule);
        unapplied.put("status", "NOT_APPLIED");
        unapplied.put("statusZh", RecommendationRuleExplanationService.statusZh("NOT_APPLIED"));
        unapplied.put("reason", missingEvidence);
        unapplied.put("manualReviewRequired", Boolean.TRUE);
        RecommendationRuleExplanationService.enrich(unapplied, rule);
        if (evidence != null && !evidence.isEmpty()) {
            unapplied.put("evidence", evidence);
        }
        unappliedRules.add(unapplied);
        preconditions.add(preconditionEntry(rule, missingEvidence, preconditionDescription));
        semanticRisks.add(semanticRiskEntry(rule, "SEMANTIC_EQUIVALENCE_RISK", "HIGH", riskDescription));
    }

    protected Map<String, Object> selectStarEvidence(ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("starItems", profile.getSelectStarItems());
        evidence.put("knownTables", profile.getTables());
        evidence.put("metadataRequirement", "TRUSTED_COLUMN_LIST_REQUIRED");
        evidence.put("rewritePolicy", "MANUAL_REVIEW_ONLY");
        return evidence;
    }

    protected Map<String, Object> functionPredicateEvidence(ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("predicateSamples", profile.getFunctionWrappedPredicateExpressions());
        evidence.put("requiredProofs", Arrays.asList("COLUMN_TYPE", "TIMEZONE", "BOUNDARY_PRECISION"));
        evidence.put("rewritePolicy", "MANUAL_REVIEW_ONLY");
        return evidence;
    }

    protected Map<String, Object> repeatedSubqueryEvidence(ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("repeatedSubqueryCount", Integer.valueOf(profile.getRepeatedSubqueryCount()));
        evidence.put("subquerySamples", profile.getRepeatedSubquerySamples());
        evidence.put("requiredProofs", Arrays.asList("SIDE_EFFECT_FREE", "ENGINE_CTE_BEHAVIOR"));
        evidence.put("rewritePolicy", "MANUAL_REVIEW_ONLY");
        return evidence;
    }

    protected Map<String, Object> ruleEntry(String level,
                                          String rule,
                                          String status,
                                          String evidenceLevel,
                                          Boolean autoApplyEligibleAfterValidation,
                                          String description) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("level", level);
        entry.put("rule", rule);
        entry.put("status", status);
        entry.put("evidenceLevel", evidenceLevel);
        entry.put("autoApplyEligibleAfterValidation", autoApplyEligibleAfterValidation);
        entry.put("description", description);
        RecommendationRuleExplanationService.enrich(entry, rule);
        entry.put("statusZh", RecommendationRuleExplanationService.statusZh(status));
        if (!Boolean.TRUE.equals(autoApplyEligibleAfterValidation)) {
            entry.put("manualReviewRequired", Boolean.TRUE);
        }
        return entry;
    }

    protected Map<String, Object> preconditionEntry(String rule, String code, String description) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("code", code);
        entry.put("description", description);
        RecommendationRuleExplanationService.enrich(entry, rule);
        return entry;
    }

    protected Map<String, Object> semanticRiskEntry(String rule, String category, String severity, String description) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("category", category);
        entry.put("severity", severity);
        entry.put("description", description);
        RecommendationRuleExplanationService.enrich(entry, rule);
        return entry;
    }

    protected String normalizeRecommendationRule(String appliedRule) {
        if ("COUNT_LITERAL_TO_COUNT_STAR".equals(appliedRule)) {
            return "COUNT_ONE_TO_COUNT_STAR";
        }
        if ("DEDUPLICATE_GROUP_BY_KEYS".equals(appliedRule) || "DEDUPLICATE_ORDER_BY_KEYS".equals(appliedRule)) {
            return "DUPLICATE_GROUP_ORDER_KEY";
        }
        return appliedRule;
    }

    protected String l0RuleDescription(String appliedRule) {
        if ("COUNT_LITERAL_TO_COUNT_STAR".equals(appliedRule)) {
            return "候选 SQL 中非空字面量上的 COUNT 已标准化为 COUNT(*)。";
        }
        if ("DEDUPLICATE_GROUP_BY_KEYS".equals(appliedRule) || "DEDUPLICATE_ORDER_BY_KEYS".equals(appliedRule)) {
            return "已移除重复分组或排序键，并保留首次出现顺序。";
        }
        return "已对候选 SQL 应用保守语法改写。";
    }

    protected boolean containsManualReviewRule(List<Map<String, Object>> ruleChain) {
        for (Map<String, Object> rule : ruleChain) {
            if (Boolean.TRUE.equals(rule.get("manualReviewRequired"))) {
                return true;
            }
        }
        return false;
    }

    protected Map<String, Object> expectedBenefit(ParsedSqlProfile profile,
                                                RewriteOutcome outcome,
                                                RewriteProductionCapabilityReport productionCapabilityReport) {
        LinkedHashMap<String, Object> benefit = new LinkedHashMap<String, Object>();
        benefit.put("evidenceType", "STATIC_HEURISTIC");
        benefit.put("claimBoundary", "NOT_REAL_EXECUTION_GAIN");
        benefit.put("summary", "仅基于解析器信号和规则覆盖估算。");
        benefit.put("appliedRuleCount", Integer.valueOf(outcome.appliedRules.size()));
        benefit.put("riskSignalCount", Integer.valueOf(profile.getWarnings().size()));
        benefit.put("level", staticBenefitLevel(profile, outcome));
        benefit.put("productionScaleGate", productionScaleGate());
        benefit.put("productionCapabilityGate", productionCapabilityReport.toSummaryMap());
        return benefit;
    }

    protected Map<String, Object> estimatedCost(ParsedSqlProfile profile,
                                              boolean manualReviewRequired,
                                              RewriteProductionCapabilityReport productionCapabilityReport) {
        LinkedHashMap<String, Object> cost = new LinkedHashMap<String, Object>();
        cost.put("evidenceType", "STATIC_HEURISTIC");
        cost.put("validation", "RESULT_DIFF_REQUIRED");
        cost.put("manualReviewRequired", Boolean.valueOf(manualReviewRequired));
        cost.put("followUp", manualReviewRequired ? "REVIEW_RULE_PRECONDITIONS" : "VALIDATE_L0_CANDIDATE");
        cost.put("riskSignalCount", Integer.valueOf(profile.getWarnings().size()));
        cost.put("productionScaleGate", productionScaleGate());
        cost.put("productionCapabilityGate", productionCapabilityReport.toSummaryMap());
        return cost;
    }

    protected Map<String, Object> productionScaleGate() {
        LinkedHashMap<String, Object> gate = new LinkedHashMap<String, Object>();
        gate.put("status", "EXTERNAL_EVIDENCE_REQUIRED");
        gate.put("claimBoundary", "PRODUCTION_SCALE_NOT_PROVEN_BY_STATIC_REWRITE");
        gate.put("targetConcurrency", Integer.valueOf(PRODUCTION_TARGET_CONCURRENCY));
        gate.put("targetDatasetSizeLabel", "THIRTY_PB");
        gate.put("targetDatasetSizeBytes", Long.valueOf(PRODUCTION_TARGET_DATASET_SIZE_BYTES));
        gate.put("targetDailyQueryVolume", Long.valueOf(PRODUCTION_TARGET_DAILY_QUERY_VOLUME));
        gate.put("requiredReplayHours", Integer.valueOf(PRODUCTION_TARGET_REPLAY_HOURS));
        gate.put("requiredEvidence", PRODUCTION_SCALE_REQUIRED_EVIDENCE);
        gate.put(
            "verifierCommand",
            "python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> "
                + "--output <external-evidence-dir>/verification-result.json"
        );
        gate.put("blockedTask", "USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518");
        return gate;
    }

    protected String staticBenefitLevel(ParsedSqlProfile profile, RewriteOutcome outcome) {
        if (outcome.appliedRules.size() >= 2 || profile.getAggregateFunctionCount() >= 4 || profile.getJoinCount() >= 3) {
            return "MEDIUM";
        }
        if (!outcome.appliedRules.isEmpty() || profile.isSelectStar() || profile.getDatePredicateColumns().size() > 0) {
            return "LOW";
        }
        return "UNKNOWN";
    }
}
