package com.company.sqloptimization.domain.rewrite.parser;

import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockHashGroup;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolutionReport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserStackFusionAnalyzer {

    private static final Pattern FANRUAN_ALIAS_PATTERN =
        Pattern.compile("(?i)\\bSub\\d+_[^\\s,()]+");
    private static final Pattern TIME_PARTITION_PATTERN =
        Pattern.compile("(?i)(^|[._`\"])(DTE|DT|BIZ_DATE|QUERY_DATE|DATE_DAY|DAY_ID)([._`\"]|$)");
    private static final Pattern DATE_RANGE_PATTERN =
        Pattern.compile("(?i)(BETWEEN|>=|<=|>|<|=).*\\b(DATE|TIMESTAMP)?\\s*'?[0-9]{4}-[0-9]{2}-[0-9]{2}");

    public ParserStackFusionReport analyze(String normalizedSql,
                                           String parserEngine,
                                           Map<String, Object> advancedStructureProfile,
                                           QueryBlockDag queryBlockDag,
                                           RelationalRewritePlan relationalRewritePlan,
                                           CostBasedRewriteSelectionReport costReport,
                                           RuleConflictResolutionReport ruleReport) {
        Map<String, Object> advancedProfile = advancedStructureProfile == null
            ? Collections.<String, Object>emptyMap()
            : new LinkedHashMap<String, Object>(advancedStructureProfile);
        List<ParserMetadataTag> metadataTags = metadataTags(normalizedSql, advancedProfile, queryBlockDag);
        List<RewriteConstraint> rewriteConstraints = rewriteConstraints(metadataTags, queryBlockDag, relationalRewritePlan);
        List<HetuPlanHint> hetuPlanHints = hetuPlanHints(
            advancedProfile,
            queryBlockDag,
            relationalRewritePlan,
            costReport
        );
        List<Map<String, Object>> plannerStages = calcitePlannerStages(
            queryBlockDag,
            relationalRewritePlan,
            costReport,
            ruleReport
        );
        LinkedHashMap<String, Object> attributes = baseAttributes();
        attributes.put("inputParserEngine", ParserFusionCollections.text(parserEngine));
        attributes.put("jsqlParserMetadataTagCount", Integer.valueOf(metadataTags.size()));
        attributes.put("rewriteConstraintCount", Integer.valueOf(rewriteConstraints.size()));
        attributes.put("hetuPlanHintCount", Integer.valueOf(hetuPlanHints.size()));
        attributes.put("calcitePlannerStageCount", Integer.valueOf(plannerStages.size()));
        attributes.put("fusionConstraintInjection", rewriteConstraints.isEmpty()
            ? "NO_METADATA_CONSTRAINT"
            : "JSQLPARSER_METADATA_INJECTED_AS_REWRITE_CONSTRAINT");
        attributes.put("relNodeBoundary", "STATIC_L4_RELATIONAL_ALGEBRA_SURROGATE_NOT_REAL_CALCITE_RELNODE");
        return new ParserStackFusionReport(
            ParserStackFusionReport.SCHEMA_VERSION,
            sourceSchemaVersion(queryBlockDag),
            fusionStatus(metadataTags, rewriteConstraints, hetuPlanHints),
            parserRoles(),
            plannerStages,
            metadataTags,
            rewriteConstraints,
            hetuPlanHints,
            attributes
        );
    }

    private List<Map<String, Object>> parserRoles() {
        List<Map<String, Object>> roles = new ArrayList<Map<String, Object>>();
        roles.add(role(
            "CALCITE",
            Arrays.asList("L1_TO_L3_QUERY_BLOCK_DECOMPOSITION", "L4_RELATIONAL_ALGEBRA_BUILD", "OPTIMIZER_INTEGRATION"),
            "SqlNode to RelNode 转换能力、HepPlanner/VolcanoPlanner 集成能力强；当前 repo-closed 路径使用 Calcite SqlNode 和 L4 surrogate，不执行真实 planner。"
        ));
        roles.add(role(
            "JSQLPARSER",
            Arrays.asList("L1_SYNTAX_DETAIL_EXTRACTION", "DIALECT_SPECIFIC_PATTERN_SCAN", "RAW_SQL_TEXT_MAPPING"),
            "MySQL/Oracle 方言和原始文本形态更灵活；用于保留别名、函数、谓词和 BI 工具生成模式标签。"
        ));
        return roles;
    }

    private Map<String, Object> role(String parser, List<String> responsibilities, String reason) {
        LinkedHashMap<String, Object> role = new LinkedHashMap<String, Object>();
        role.put("parser", parser);
        role.put("responsibilities", responsibilities);
        role.put("reason", reason);
        role.put("runtimeBoundary", "NO_SQL_EXECUTION");
        return role;
    }

    private List<ParserMetadataTag> metadataTags(String normalizedSql,
                                                 Map<String, Object> advancedProfile,
                                                 QueryBlockDag queryBlockDag) {
        List<ParserMetadataTag> tags = new ArrayList<ParserMetadataTag>();
        LinkedHashSet<String> matches = new LinkedHashSet<String>();
        Matcher matcher = FANRUAN_ALIAS_PATTERN.matcher(ParserFusionCollections.text(normalizedSql));
        while (matcher.find()) {
            matches.add(cleanAlias(matcher.group()));
        }
        addAliasMatches(matches, advancedProfile, "tables", "alias");
        addAliasMatches(matches, advancedProfile, "projections", "alias");
        addAliasMatches(matches, advancedProfile, "subqueries", "alias");
        for (QueryBlockNode block : queryBlockDag == null ? Collections.<QueryBlockNode>emptyList() : queryBlockDag.getBlocks()) {
            if (FANRUAN_ALIAS_PATTERN.matcher(ParserFusionCollections.text(block.getAlias())).find()) {
                matches.add(cleanAlias(block.getAlias()));
            }
            if (FANRUAN_ALIAS_PATTERN.matcher(ParserFusionCollections.text(block.getName())).find()) {
                matches.add(cleanAlias(block.getName()));
            }
        }
        int sequence = 1;
        int nestedDepth = nestedDepth(advancedProfile);
        for (String match : matches) {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("metadataSource", "JSQLPARSER_RAW_TEXT_AND_ADVANCED_PROFILE");
            attributes.put("aliasPattern", "SubXX_分组和汇总");
            attributes.put("constraintCandidate", "BI_GENERATED_REPEATED_BLOCK");
            tags.add(new ParserMetadataTag(
                "PARSER-TAG-" + sequence,
                "FANRUAN",
                "ALIAS_PATTERN_SUBXX_GROUP_SUMMARY",
                nestedDepth,
                "SubXX_分组和汇总",
                match,
                0.92,
                attributes
            ));
            sequence++;
        }
        return tags;
    }

    private void addAliasMatches(LinkedHashSet<String> matches,
                                 Map<String, Object> advancedProfile,
                                 String listKey,
                                 String aliasKey) {
        for (Map<String, Object> item : ParserFusionCollections.mapList(advancedProfile.get(listKey))) {
            String alias = ParserFusionCollections.text(item.get(aliasKey));
            if (FANRUAN_ALIAS_PATTERN.matcher(alias).find()) {
                matches.add(cleanAlias(alias));
            }
        }
    }

    private List<RewriteConstraint> rewriteConstraints(List<ParserMetadataTag> metadataTags,
                                                       QueryBlockDag queryBlockDag,
                                                       RelationalRewritePlan relationalRewritePlan) {
        if (metadataTags.isEmpty()) {
            return Collections.emptyList();
        }
        List<RewriteConstraint> constraints = new ArrayList<RewriteConstraint>();
        int sequence = 1;
        List<String> duplicateBlockIds = duplicateBlockIds(queryBlockDag);
        if (!duplicateBlockIds.isEmpty()) {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("injectedInto", "CALCITE_L4_REWRITE_PLANNING");
            attributes.put("requiresSemanticGate", Boolean.TRUE);
            attributes.put("sourceCandidateRule", RelationalRewriteRuleType.CSE_ELIMINATION.name());
            constraints.add(new RewriteConstraint(
                "REWRITE-CONSTRAINT-" + sequence,
                metadataTags.get(0).getTagId(),
                "BI_GENERATED_REPEATED_BLOCK_AGGRESSIVE_MERGE_ALLOWED",
                "ALLOW_AGGRESSIVE_CSE_WITH_SEMANTIC_GATE",
                duplicateBlockIds,
                "JSqlParser 识别出 BI 工具生成的重复块别名，可注入 Calcite L4 规划作为 CSE 合并约束。",
                attributes
            ));
            sequence++;
        }
        if (relationalRewritePlan != null && relationalRewritePlan.hasCandidate(RelationalRewriteRuleType.VERTICAL_FOLDING)) {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("injectedInto", "CALCITE_HEP_PLANNER_RULE_ORDER");
            attributes.put("requiresAggregationDecomposition", Boolean.TRUE);
            constraints.add(new RewriteConstraint(
                "REWRITE-CONSTRAINT-" + sequence,
                metadataTags.get(0).getTagId(),
                "BI_AGGREGATION_BLOCK_VERTICAL_FOLDING_PREFERRED",
                "PREFER_CASE_AGGREGATION_WHEN_OUTPUT_COLUMNS_COMPATIBLE",
                candidateSourceBlocks(relationalRewritePlan, RelationalRewriteRuleType.VERTICAL_FOLDING),
                "BI 工具常生成同源多指标聚合块，优先进入纵向折叠但仍保留语义门禁。",
                attributes
            ));
        }
        return constraints;
    }

    private List<HetuPlanHint> hetuPlanHints(Map<String, Object> advancedProfile,
                                             QueryBlockDag queryBlockDag,
                                             RelationalRewritePlan relationalRewritePlan,
                                             CostBasedRewriteSelectionReport costReport) {
        List<HetuPlanHint> result = new ArrayList<HetuPlanHint>();
        int[] sequence = new int[] {1};
        if (shouldSuggestMaterializedCte(queryBlockDag, relationalRewritePlan)) {
            result.add(hint(
                sequence,
                "HETU_MATERIALIZED_CTE",
                "WITH_CLAUSE",
                "PREFER_WITH_MATERIALIZED_CTE",
                "重复查询块或 CTE 引用适合优先生成 WITH 结构，并在 Hetu 环境验证 MATERIALIZED CTE 策略。",
                evidence("candidateRules", candidateRules(relationalRewritePlan)),
                syntaxBoundary()
            ));
        }
        if (hasJoinSignal(advancedProfile, queryBlockDag, relationalRewritePlan)) {
            result.add(hint(
                sequence,
                "HETU_DYNAMIC_FILTER_PUSHDOWN",
                "JOIN_CONDITION",
                "/*+ dynamic_filter */ JOIN_KEY_PUSHDOWN",
                "JOIN 条件和右侧过滤可作为 Hetu Dynamic Filter 下推候选。",
                evidence("joinGraph", ParserFusionCollections.mapList(advancedProfile.get("joinGraph"))),
                syntaxBoundary()
            ));
        }
        List<String> partitionColumns = partitionColumns(advancedProfile, queryBlockDag);
        if (!partitionColumns.isEmpty()) {
            LinkedHashMap<String, Object> attributes = syntaxBoundary();
            attributes.put("partitionColumns", partitionColumns);
            result.add(hint(
                sequence,
                "HETU_PARTITION_PRUNING",
                String.join(",", partitionColumns),
                "PARTITION_PRUNING(column=" + partitionColumns.get(0) + ")",
                "识别到时间分区列和日期范围谓词，可转换为分区裁剪提示。",
                evidence("predicateColumns", partitionColumns),
                attributes
            ));
        }
        if (hasAggregationSignal(advancedProfile, queryBlockDag, relationalRewritePlan)) {
            result.add(hint(
                sequence,
                "HETU_TWO_PHASE_DISTRIBUTED_AGGREGATION",
                "AGGREGATION",
                "PARTIAL_AGGREGATION_THEN_FINAL_AGGREGATION",
                "聚合与 GROUP BY 形态适合避免单节点聚合瓶颈，建议预聚合 + 最终聚合两阶段计划。",
                evidence("costSelectionStatus", costReport == null ? "" : costReport.getSelectionStatus()),
                syntaxBoundary()
            ));
        }
        return result;
    }

    private List<Map<String, Object>> calcitePlannerStages(QueryBlockDag queryBlockDag,
                                                           RelationalRewritePlan relationalRewritePlan,
                                                           CostBasedRewriteSelectionReport costReport,
                                                           RuleConflictResolutionReport ruleReport) {
        List<Map<String, Object>> stages = new ArrayList<Map<String, Object>>();
        stages.add(plannerStage(
            "SQL_NODE",
            queryBlockDag == null ? "UNAVAILABLE" : "AVAILABLE",
            "Calcite SqlNode drives QBDAG L1-L3 query block decomposition.",
            queryBlockDag == null ? Collections.<String, Object>emptyMap() : queryBlockDag.getAttributes()
        ));
        LinkedHashMap<String, Object> relNodeEvidence = new LinkedHashMap<String, Object>();
        relNodeEvidence.put("relationalRewriteCandidateCount", relationalRewritePlan == null
            ? Integer.valueOf(0)
            : Integer.valueOf(relationalRewritePlan.getCandidates().size()));
        relNodeEvidence.put("candidateRules", candidateRules(relationalRewritePlan));
        stages.add(plannerStage(
            "RELNODE_TREE",
            "STATIC_L4_RELATIONAL_ALGEBRA_SURROGATE",
            "当前仓库用 L4 RelationalAlgebraNode / RelationalRewritePlan 承载 RelNode tree surrogate，不执行真实 SqlToRelConverter。",
            relNodeEvidence
        ));
        stages.add(plannerStage(
            "HEP_PLANNER",
            "PLANNED_NOT_EXECUTED",
            "启发式规则顺序包括 SUBQUERY_DECORRELATION 和 PREDICATE_PUSHDOWN；当前只记录集成策略。",
            stageEvidence("rules", Arrays.asList("SUBQUERY_DECORRELATION", "PREDICATE_PUSHDOWN"))
        ));
        stages.add(plannerStage(
            "VOLCANO_PLANNER",
            "PLANNED_NOT_EXECUTED",
            "成本枚举由当前抽象代价模型和规则冲突报告提供 repo-closed surrogate。",
            volcanoEvidence(costReport, ruleReport)
        ));
        return stages;
    }

    private Map<String, Object> plannerStage(String stage,
                                             String status,
                                             String description,
                                             Map<String, Object> evidence) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("stage", stage);
        result.put("status", status);
        result.put("description", description);
        result.put("evidence", ParserFusionCollections.immutableMap(evidence));
        result.put("runtimeBoundary", "NO_SQL_EXECUTION");
        return result;
    }

    private HetuPlanHint hint(int[] sequence,
                              String optimizationType,
                              String target,
                              String hintText,
                              String reason,
                              List<Map<String, Object>> evidence,
                              Map<String, Object> attributes) {
        int value = sequence[0];
        sequence[0]++;
        return new HetuPlanHint(
            "HETU-HINT-" + value,
            optimizationType,
            target,
            hintText,
            reason,
            evidence,
            attributes
        );
    }

    private boolean shouldSuggestMaterializedCte(QueryBlockDag queryBlockDag,
                                                 RelationalRewritePlan relationalRewritePlan) {
        if (queryBlockDag != null && queryBlockDag.hasDuplicateStructuralBlocks()) {
            return true;
        }
        return relationalRewritePlan != null && relationalRewritePlan.hasCandidate(RelationalRewriteRuleType.CSE_ELIMINATION);
    }

    private boolean hasJoinSignal(Map<String, Object> advancedProfile,
                                  QueryBlockDag queryBlockDag,
                                  RelationalRewritePlan relationalRewritePlan) {
        if (!ParserFusionCollections.mapList(advancedProfile.get("joinGraph")).isEmpty()) {
            return true;
        }
        if (relationalRewritePlan != null && relationalRewritePlan.hasCandidate(RelationalRewriteRuleType.HORIZONTAL_UNNESTING)) {
            return true;
        }
        if (queryBlockDag != null) {
            for (QueryBlockNode block : queryBlockDag.getBlocks()) {
                if (block.getFromClause().toUpperCase(Locale.ROOT).contains("JOIN")) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasAggregationSignal(Map<String, Object> advancedProfile,
                                         QueryBlockDag queryBlockDag,
                                         RelationalRewritePlan relationalRewritePlan) {
        if (!ParserFusionCollections.mapList(advancedProfile.get("aggregations")).isEmpty()
            || !ParserFusionCollections.mapList(advancedProfile.get("groupBy")).isEmpty()) {
            return true;
        }
        if (relationalRewritePlan != null && relationalRewritePlan.hasCandidate(RelationalRewriteRuleType.VERTICAL_FOLDING)) {
            return true;
        }
        if (queryBlockDag != null) {
            for (QueryBlockNode block : queryBlockDag.getBlocks()) {
                if (!block.getGroupBy().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> partitionColumns(Map<String, Object> advancedProfile, QueryBlockDag queryBlockDag) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (Map<String, Object> predicate : ParserFusionCollections.mapList(advancedProfile.get("predicates"))) {
            String expression = ParserFusionCollections.text(predicate.get("expression"));
            if (!DATE_RANGE_PATTERN.matcher(expression).find()) {
                continue;
            }
            for (String column : stringList(predicate.get("sourceColumns"))) {
                if (TIME_PARTITION_PATTERN.matcher(column).find()) {
                    result.add(cleanColumn(column));
                }
            }
            if (TIME_PARTITION_PATTERN.matcher(expression).find()) {
                result.add(firstPartitionColumn(expression));
            }
        }
        if (queryBlockDag != null) {
            for (QueryBlockNode block : queryBlockDag.getBlocks()) {
                String predicate = block.getWhereClause();
                if (DATE_RANGE_PATTERN.matcher(predicate).find() && TIME_PARTITION_PATTERN.matcher(predicate).find()) {
                    result.add(firstPartitionColumn(predicate));
                }
            }
        }
        return new ArrayList<String>(result);
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (List<Object>) value) {
            String text = ParserFusionCollections.text(item);
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private String firstPartitionColumn(String text) {
        Matcher matcher = TIME_PARTITION_PATTERN.matcher(ParserFusionCollections.text(text));
        if (matcher.find()) {
            return cleanColumn(matcher.group());
        }
        return "DTE";
    }

    private String cleanColumn(String value) {
        String text = ParserFusionCollections.text(value)
            .replace("`", "")
            .replace("\"", "")
            .trim();
        if (text.contains(".")) {
            text = text.substring(text.lastIndexOf('.') + 1);
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private List<String> duplicateBlockIds(QueryBlockDag queryBlockDag) {
        if (queryBlockDag == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (QueryBlockHashGroup group : queryBlockDag.getDuplicateStructuralGroups()) {
            result.addAll(group.getBlockIds());
        }
        return new ArrayList<String>(result);
    }

    private List<String> candidateSourceBlocks(RelationalRewritePlan relationalRewritePlan,
                                               RelationalRewriteRuleType ruleType) {
        if (relationalRewritePlan == null || relationalRewritePlan.candidatesOf(ruleType).isEmpty()) {
            return Collections.emptyList();
        }
        return relationalRewritePlan.candidatesOf(ruleType).get(0).getSourceBlockIds();
    }

    private List<String> candidateRules(RelationalRewritePlan relationalRewritePlan) {
        if (relationalRewritePlan == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate candidate
            : relationalRewritePlan.getCandidates()) {
            result.add(candidate.getRuleType().name());
        }
        return new ArrayList<String>(result);
    }

    private int nestedDepth(Map<String, Object> advancedProfile) {
        int max = 0;
        for (Map<String, Object> subquery : ParserFusionCollections.mapList(advancedProfile.get("subqueries"))) {
            max = Math.max(max, ParserFusionCollections.intValue(subquery.get("nestedLevel")));
        }
        return max;
    }

    private List<Map<String, Object>> evidence(String key, Object value) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("type", key);
        item.put("value", value);
        return Collections.<Map<String, Object>>singletonList(item);
    }

    private Map<String, Object> stageEvidence(String key, Object value) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put(key, value);
        return item;
    }

    private Map<String, Object> volcanoEvidence(CostBasedRewriteSelectionReport costReport,
                                                RuleConflictResolutionReport ruleReport) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("costSelectionStatus", costReport == null ? "" : costReport.getSelectionStatus());
        evidence.put("paretoFrontierCandidateIds", costReport == null
            ? Collections.<String>emptyList()
            : costReport.getParetoFrontierCandidateIds());
        evidence.put("ruleConflictResolutionStatus", ruleReport == null ? "" : ruleReport.getResolutionStatus());
        evidence.put("selectedRuleIds", ruleReport == null ? Collections.<String>emptyList() : ruleReport.getSelectedRuleIds());
        return evidence;
    }

    private LinkedHashMap<String, Object> syntaxBoundary() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("engine", "HETU");
        attributes.put("syntaxBoundary", "HETU_ENVIRONMENT_VALIDATION_REQUIRED");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        return attributes;
    }

    private LinkedHashMap<String, Object> baseAttributes() {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "DUAL_PARSER_STACK_AND_REWRITE_CORE");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("workflow", "JSQLPARSER_METADATA_PLUS_CALCITE_L4_SURROGATE");
        attributes.put("calciteRole", "L1_TO_L3_L4_OPTIMIZER_INTEGRATION");
        attributes.put("jsqlParserRole", "DIALECT_PATTERN_AND_RAW_TEXT_MAPPING");
        attributes.put("hetuAdapterStatus", "STATIC_PLAN_HINTS_ONLY");
        return attributes;
    }

    private String fusionStatus(List<ParserMetadataTag> metadataTags,
                                List<RewriteConstraint> rewriteConstraints,
                                List<HetuPlanHint> hetuPlanHints) {
        if (!metadataTags.isEmpty() && !rewriteConstraints.isEmpty()) {
            return "METADATA_CONSTRAINTS_AND_HETU_HINTS_READY";
        }
        if (!hetuPlanHints.isEmpty()) {
            return "HETU_HINTS_READY";
        }
        return "DUAL_STACK_REPORT_READY";
    }

    private String sourceSchemaVersion(QueryBlockDag queryBlockDag) {
        return queryBlockDag == null ? "" : queryBlockDag.getSchemaVersion();
    }

    private String cleanAlias(String value) {
        return ParserFusionCollections.text(value)
            .replace("\"", "")
            .replace("`", "")
            .trim();
    }
}
