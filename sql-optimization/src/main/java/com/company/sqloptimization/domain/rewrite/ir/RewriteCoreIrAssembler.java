package com.company.sqloptimization.domain.rewrite.ir;

import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelector;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionAnalyzer;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionReport;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDagBuilder;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlanBuilder;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendationGenerator;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendationReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolutionReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolver;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceVerifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class RewriteCoreIrAssembler {

    private static final Pattern TIME_COLUMN_PATTERN =
        Pattern.compile("(?i)(^|[._])(date|time|dte|dt|day|month|year|biz_date|query_date)$");

    public RewriteCoreIrSnapshot assemble(String normalizedSql,
                                          String parserEngine,
                                          Map<String, Object> advancedStructureProfile) {
        Map<String, Object> advancedProfile = advancedStructureProfile == null
            ? Collections.<String, Object>emptyMap()
            : new LinkedHashMap<String, Object>(advancedStructureProfile);
        List<Map<String, Object>> tables = IrCollections.mapList(advancedProfile.get("tables"));
        List<Map<String, Object>> projections = IrCollections.mapList(advancedProfile.get("projections"));
        List<Map<String, Object>> predicates = IrCollections.mapList(advancedProfile.get("predicates"));
        List<Map<String, Object>> joinGraph = IrCollections.mapList(advancedProfile.get("joinGraph"));
        List<Map<String, Object>> aggregations = IrCollections.mapList(advancedProfile.get("aggregations"));
        List<Map<String, Object>> groupBy = IrCollections.mapList(advancedProfile.get("groupBy"));
        List<Map<String, Object>> ctes = IrCollections.mapList(advancedProfile.get("ctes"));
        List<Map<String, Object>> subqueries = IrCollections.mapList(advancedProfile.get("subqueries"));

        List<TableReferenceIr> tableReferences = buildTableReferences(tables, predicates);
        QueryBlockDag queryBlockDag = new QueryBlockDagBuilder().build(normalizedSql, advancedProfile);
        List<QueryBlockIr> queryBlocks = buildQueryBlocks(
            tableReferences,
            projections,
            predicates,
            aggregations,
            groupBy,
            ctes,
            subqueries,
            queryBlockDag
        );
        List<RelationalAlgebraNode> algebra = buildRelationalAlgebra(
            normalizedSql,
            tableReferences,
            projections,
            predicates,
            joinGraph,
            aggregations,
            groupBy
        );
        RelationalRewritePlan relationalRewritePlan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        SemanticEquivalenceReport semanticEquivalenceReport = new SemanticEquivalenceVerifier().verify(
            queryBlockDag,
            relationalRewritePlan
        );
        CostBasedRewriteSelectionReport costBasedRewriteSelectionReport = new CostBasedRewriteSelector().select(
            queryBlockDag,
            relationalRewritePlan,
            semanticEquivalenceReport
        );
        RuleConflictResolutionReport ruleConflictResolutionReport = new RuleConflictResolver().resolve(
            relationalRewritePlan,
            costBasedRewriteSelectionReport
        );
        ParserStackFusionReport parserStackFusionReport = new ParserStackFusionAnalyzer().analyze(
            normalizedSql,
            parserEngine,
            advancedProfile,
            queryBlockDag,
            relationalRewritePlan,
            costBasedRewriteSelectionReport,
            ruleConflictResolutionReport
        );
        RewriteRecommendationReport rewriteRecommendationReport = new RewriteRecommendationGenerator().generate(
            normalizedSql,
            queryBlockDag,
            relationalRewritePlan,
            semanticEquivalenceReport,
            costBasedRewriteSelectionReport,
            ruleConflictResolutionReport,
            parserStackFusionReport
        );
        BusinessIntentIr businessIntent = buildBusinessIntent(advancedProfile, projections, predicates, aggregations, groupBy);
        AstNodeReference ast = buildAstReference(normalizedSql, parserEngine, advancedProfile);

        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("source", "STATIC_PARSE_PROFILE");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("ruleLevelCompatibility", "IR 层码使用 L1_AST 至 L5_BUSINESS_INTENT，推荐规则等级继续使用 L0/L1/L2。");
        attributes.put("queryBlockDagStatus", queryBlockDag.getAttributes().get("decompositionStatus"));
        attributes.put("queryBlockDagBlockCount", Integer.valueOf(queryBlockDag.getBlocks().size()));
        attributes.put("duplicateStructuralGroupCount", Integer.valueOf(queryBlockDag.getDuplicateStructuralGroups().size()));
        attributes.put("relationalRewritePlanStatus", relationalRewritePlan.getAttributes().get("rewriteStatus"));
        attributes.put("relationalRewriteCandidateCount", Integer.valueOf(relationalRewritePlan.getCandidates().size()));
        attributes.put("semanticEquivalenceStatus", semanticEquivalenceReport.getStatus().name());
        attributes.put("semanticEquivalenceCheckCount", Integer.valueOf(semanticEquivalenceReport.getChecks().size()));
        attributes.put("costBasedSelectionStatus", costBasedRewriteSelectionReport.getSelectionStatus());
        attributes.put("costBasedSelectedCandidateId", costBasedRewriteSelectionReport.getSelectedCandidateId());
        attributes.put(
            "costBasedParetoFrontierCount",
            Integer.valueOf(costBasedRewriteSelectionReport.getParetoFrontierCandidateIds().size())
        );
        attributes.put("ruleConflictResolutionStatus", ruleConflictResolutionReport.getResolutionStatus());
        attributes.put("rewriteRuleDslMatchedCount", Integer.valueOf(ruleConflictResolutionReport.getMatchedRules().size()));
        attributes.put("rewriteRuleConflictCount", Integer.valueOf(ruleConflictResolutionReport.getConflicts().size()));
        attributes.put("rewriteRuleSelectedRuleIds", ruleConflictResolutionReport.getSelectedRuleIds());
        attributes.put("parserStackFusionStatus", parserStackFusionReport.getFusionStatus());
        attributes.put("parserMetadataTagCount", Integer.valueOf(parserStackFusionReport.getMetadataTags().size()));
        attributes.put("rewriteConstraintCount", Integer.valueOf(parserStackFusionReport.getRewriteConstraints().size()));
        attributes.put("hetuPlanHintCount", Integer.valueOf(parserStackFusionReport.getHetuPlanHints().size()));
        attributes.put("rewriteRecommendationStatus", rewriteRecommendationReport.getGenerationStatus());
        attributes.put(
            "rewriteRecommendationCount",
            Integer.valueOf(rewriteRecommendationReport.getRecommendations().size())
        );
        attributes.put("rewriteRecommendationSelectedId", rewriteRecommendationReport.getSelectedRecommendationId());
        attributes.put("rewriteRecommendationAutoApplyAllowed", Boolean.FALSE);

        return new RewriteCoreIrSnapshot(
            RewriteCoreIrSnapshot.SCHEMA_VERSION,
            RewriteIrLayer.ordered(),
            ast,
            tableReferences,
            queryBlocks,
            queryBlockDag,
            algebra,
            relationalRewritePlan,
            semanticEquivalenceReport,
            costBasedRewriteSelectionReport,
            ruleConflictResolutionReport,
            parserStackFusionReport,
            rewriteRecommendationReport,
            businessIntent,
            architectureConflicts(),
            attributes
        );
    }

    private AstNodeReference buildAstReference(String normalizedSql,
                                               String parserEngine,
                                               Map<String, Object> advancedProfile) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("profileStatus", advancedProfile.get("profileStatus"));
        summary.put("tableCount", Integer.valueOf(IrCollections.mapList(advancedProfile.get("tables")).size()));
        summary.put("projectionCount", Integer.valueOf(IrCollections.mapList(advancedProfile.get("projections")).size()));
        summary.put("predicateCount", Integer.valueOf(IrCollections.mapList(advancedProfile.get("predicates")).size()));
        summary.put("queryShape", "SELECT_OR_WITH");
        return new AstNodeReference(
            "AST-ROOT",
            parserEngine,
            dialectNodeKind(parserEngine),
            "SELECT_OR_WITH",
            normalizedSql,
            summary
        );
    }

    private String dialectNodeKind(String parserEngine) {
        String engine = parserEngine == null ? "" : parserEngine.trim().toUpperCase(Locale.ROOT);
        if ("APACHE_CALCITE".equals(engine)) {
            return "CALCITE_SQL_NODE";
        }
        if ("TRINO".equals(engine)) {
            return "TRINO_NODE";
        }
        if ("JSQLPARSER".equals(engine)) {
            return "JSQLPARSER_EXPRESSION";
        }
        return "UNKNOWN_DIALECT_NODE";
    }

    private List<TableReferenceIr> buildTableReferences(List<Map<String, Object>> tables,
                                                        List<Map<String, Object>> predicates) {
        if (tables.isEmpty()) {
            TableReferenceIr unknown = new TableReferenceIr(
                "TR1",
                "UNKNOWN",
                "",
                TableReferenceSourceKind.UNKNOWN,
                "UNRESOLVED_STATIC_PARSE",
                Collections.<String>emptyList(),
                Collections.<String, Object>emptyMap()
            );
            return Collections.singletonList(unknown);
        }
        List<TableReferenceIr> result = new ArrayList<TableReferenceIr>();
        int index = 1;
        for (Map<String, Object> table : tables) {
            String source = IrCollections.text(table.get("tableName"));
            String alias = IrCollections.text(table.get("alias"));
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("rawSourceType", table.get("sourceType"));
            attributes.put("schemaName", table.get("schemaName"));
            result.add(new TableReferenceIr(
                "TR" + index,
                source.isEmpty() ? "UNKNOWN" : source,
                alias,
                sourceKind(table),
                "STATIC_PARSE_ONLY",
                predicatePushdownCandidates(table, predicates, tables.size()),
                attributes
            ));
            index++;
        }
        return result;
    }

    private TableReferenceSourceKind sourceKind(Map<String, Object> table) {
        String sourceType = IrCollections.text(table.get("sourceType")).toUpperCase(Locale.ROOT);
        if ("CTE_REFERENCE".equals(sourceType)) {
            return TableReferenceSourceKind.CTE;
        }
        if ("DERIVED_TABLE".equals(sourceType)) {
            return TableReferenceSourceKind.DERIVED_TABLE;
        }
        if ("VIEW".equals(sourceType) || "DB_VIEW".equals(sourceType)) {
            return TableReferenceSourceKind.VIEW;
        }
        if (hasText(IrCollections.text(table.get("tableName")))) {
            return TableReferenceSourceKind.PHYSICAL_TABLE;
        }
        return TableReferenceSourceKind.UNKNOWN;
    }

    private List<String> predicatePushdownCandidates(Map<String, Object> table,
                                                     List<Map<String, Object>> predicates,
                                                     int tableCount) {
        List<String> result = new ArrayList<String>();
        String source = IrCollections.text(table.get("tableName"));
        String alias = IrCollections.text(table.get("alias"));
        String sourceTail = source.contains(".") ? source.substring(source.lastIndexOf('.') + 1) : source;
        for (Map<String, Object> predicate : predicates) {
            String clause = IrCollections.text(predicate.get("clause"));
            String expression = IrCollections.text(predicate.get("expression"));
            List<String> columns = IrCollections.stringList(predicate.get("sourceColumns"));
            if (!"WHERE".equalsIgnoreCase(clause) || expression.isEmpty()) {
                continue;
            }
            if (tableCount == 1 || referencesTable(columns, alias, source, sourceTail)) {
                result.add(expression);
            }
        }
        return result;
    }

    private boolean referencesTable(List<String> columns, String alias, String source, String sourceTail) {
        for (String column : columns) {
            String normalized = column.toUpperCase(Locale.ROOT);
            if (hasText(alias) && normalized.startsWith(alias.toUpperCase(Locale.ROOT) + ".")) {
                return true;
            }
            if (hasText(source) && normalized.startsWith(source.toUpperCase(Locale.ROOT) + ".")) {
                return true;
            }
            if (hasText(sourceTail) && normalized.startsWith(sourceTail.toUpperCase(Locale.ROOT) + ".")) {
                return true;
            }
        }
        return false;
    }

    private List<QueryBlockIr> buildQueryBlocks(List<TableReferenceIr> tableReferences,
                                                List<Map<String, Object>> projections,
                                                List<Map<String, Object>> predicates,
                                                List<Map<String, Object>> aggregations,
                                                List<Map<String, Object>> groupBy,
                                                List<Map<String, Object>> ctes,
                                                List<Map<String, Object>> subqueries,
                                                QueryBlockDag queryBlockDag) {
        if (queryBlockDag != null && !queryBlockDag.getBlocks().isEmpty()) {
            return buildQueryBlocksFromDag(queryBlockDag, tableReferences, projections, predicates, aggregations, groupBy);
        }
        List<QueryBlockIr> result = new ArrayList<QueryBlockIr>();
        LinkedHashMap<String, Object> rootAttributes = new LinkedHashMap<String, Object>();
        rootAttributes.put("groupBy", groupBy);
        rootAttributes.put("blockRole", "ROOT_QUERY");
        result.add(new QueryBlockIr(
            "QB-ROOT",
            "ROOT",
            referenceIds(tableReferences),
            projections,
            predicates,
            aggregations,
            rootAttributes
        ));
        int index = 1;
        for (Map<String, Object> cte : ctes) {
            result.add(namedBlock("QB-CTE-" + index, "CTE", "name", IrCollections.text(cte.get("name")), cte));
            index++;
        }
        index = 1;
        for (Map<String, Object> subquery : subqueries) {
            result.add(namedBlock(
                "QB-SUBQUERY-" + index,
                "SUBQUERY",
                "alias",
                IrCollections.text(subquery.get("alias")),
                subquery
            ));
            index++;
        }
        return result;
    }

    private List<QueryBlockIr> buildQueryBlocksFromDag(QueryBlockDag queryBlockDag,
                                                       List<TableReferenceIr> tableReferences,
                                                       List<Map<String, Object>> projections,
                                                       List<Map<String, Object>> predicates,
                                                       List<Map<String, Object>> aggregations,
                                                       List<Map<String, Object>> groupBy) {
        List<QueryBlockIr> result = new ArrayList<QueryBlockIr>();
        for (QueryBlockNode block : queryBlockDag.getBlocks()) {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.putAll(block.getAttributes());
            attributes.put("parentBlockId", block.getParentBlockId());
            attributes.put("name", block.getName());
            attributes.put("alias", block.getAlias());
            attributes.put("fromClause", block.getFromClause());
            attributes.put("whereClause", block.getWhereClause());
            attributes.put("havingClause", block.getHavingClause());
            attributes.put("groupBy", block.getGroupBy());
            attributes.put("outputColumns", block.getOutputColumns());
            attributes.put("localAliases", block.getLocalAliases());
            attributes.put("externalReferences", block.getExternalReferences());
            attributes.put("structuralHash", block.getStructuralHash());
            attributes.put("normalizedRelationalForm", block.getNormalizedRelationalForm());
            attributes.put("representativeBlockId", block.getRepresentativeBlockId());
            attributes.put("equivalentToRepresentative", Boolean.valueOf(block.isEquivalentToRepresentative()));
            if (queryBlockDag.getRootBlockId().equals(block.getBlockId())) {
                attributes.put("groupBy", groupBy);
            }
            result.add(new QueryBlockIr(
                block.getBlockId(),
                block.getBlockRole(),
                queryBlockDag.getRootBlockId().equals(block.getBlockId())
                    ? referenceIds(tableReferences)
                    : Collections.<String>emptyList(),
                queryBlockDag.getRootBlockId().equals(block.getBlockId())
                    ? projections
                    : stringMaps("expression", block.getSelectList()),
                queryBlockDag.getRootBlockId().equals(block.getBlockId())
                    ? predicates
                    : predicateMaps(block),
                queryBlockDag.getRootBlockId().equals(block.getBlockId())
                    ? aggregations
                    : Collections.<Map<String, Object>>emptyList(),
                attributes
            ));
        }
        return result;
    }

    private List<Map<String, Object>> stringMaps(String key, List<String> values) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String value : values) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put(key, value);
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> predicateMaps(QueryBlockNode block) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (hasText(block.getWhereClause())) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("clause", "WHERE");
            item.put("expression", block.getWhereClause());
            result.add(item);
        }
        if (hasText(block.getHavingClause())) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("clause", "HAVING");
            item.put("expression", block.getHavingClause());
            result.add(item);
        }
        return result;
    }

    private QueryBlockIr namedBlock(String blockId,
                                    String blockType,
                                    String nameKey,
                                    String name,
                                    Map<String, Object> source) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put(nameKey, name);
        attributes.put("source", source);
        return new QueryBlockIr(
            blockId,
            blockType,
            Collections.<String>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList(),
            attributes
        );
    }

    private List<String> referenceIds(List<TableReferenceIr> references) {
        List<String> ids = new ArrayList<String>();
        for (TableReferenceIr reference : references) {
            ids.add(reference.getReferenceId());
        }
        return ids;
    }

    private List<RelationalAlgebraNode> buildRelationalAlgebra(String normalizedSql,
                                                               List<TableReferenceIr> tableReferences,
                                                               List<Map<String, Object>> projections,
                                                               List<Map<String, Object>> predicates,
                                                               List<Map<String, Object>> joinGraph,
                                                               List<Map<String, Object>> aggregations,
                                                               List<Map<String, Object>> groupBy) {
        List<RelationalAlgebraNode> nodes = new ArrayList<RelationalAlgebraNode>();
        List<String> currentInputs = new ArrayList<String>();
        int index = 1;
        for (TableReferenceIr reference : tableReferences) {
            LinkedHashMap<String, Object> scanExpression = new LinkedHashMap<String, Object>();
            scanExpression.put("source", reference.getSource());
            scanExpression.put("alias", reference.getAlias());
            String nodeId = "RA" + index++;
            nodes.add(new RelationalAlgebraNode(
                nodeId,
                RelationalOperator.TABLE_SCAN,
                Collections.<String>emptyList(),
                Collections.<Map<String, Object>>singletonList(scanExpression),
                Collections.singletonList(reference.getReferenceId()),
                Collections.<String, Object>emptyMap()
            ));
            currentInputs.add(nodeId);
        }
        if (!joinGraph.isEmpty()) {
            currentInputs = addAlgebraNode(nodes, index++, RelationalOperator.JOIN, currentInputs, joinGraph);
        }
        if (!predicates.isEmpty()) {
            currentInputs = addAlgebraNode(nodes, index++, RelationalOperator.SIGMA, currentInputs, predicates);
        }
        if (!aggregations.isEmpty() || !groupBy.isEmpty()) {
            List<Map<String, Object>> aggregateExpressions = new ArrayList<Map<String, Object>>(aggregations);
            aggregateExpressions.addAll(groupBy);
            currentInputs = addAlgebraNode(nodes, index++, RelationalOperator.GAMMA, currentInputs, aggregateExpressions);
        }
        if (!projections.isEmpty()) {
            currentInputs = addAlgebraNode(nodes, index++, RelationalOperator.PI, currentInputs, projections);
        }
        addSetOperatorIfPresent(nodes, index, normalizedSql, currentInputs);
        return nodes;
    }

    private List<String> addAlgebraNode(List<RelationalAlgebraNode> nodes,
                                        int index,
                                        RelationalOperator operator,
                                        List<String> inputNodeIds,
                                        List<Map<String, Object>> expressions) {
        String nodeId = "RA" + index;
        nodes.add(new RelationalAlgebraNode(
            nodeId,
            operator,
            inputNodeIds,
            expressions,
            Collections.<String>emptyList(),
            Collections.<String, Object>emptyMap()
        ));
        return Collections.singletonList(nodeId);
    }

    private void addSetOperatorIfPresent(List<RelationalAlgebraNode> nodes,
                                         int index,
                                         String normalizedSql,
                                         List<String> currentInputs) {
        String sql = normalizedSql == null ? "" : normalizedSql.toUpperCase(Locale.ROOT);
        RelationalOperator operator = null;
        if (sql.contains(" UNION ")) {
            operator = RelationalOperator.UNION;
        } else if (sql.contains(" INTERSECT ")) {
            operator = RelationalOperator.INTERSECT;
        } else if (sql.contains(" EXCEPT ") || sql.contains(" MINUS ")) {
            operator = RelationalOperator.DIFFERENCE;
        }
        if (operator == null) {
            return;
        }
        nodes.add(new RelationalAlgebraNode(
            "RA" + index,
            operator,
            currentInputs,
            Collections.<Map<String, Object>>emptyList(),
            Collections.<String>emptyList(),
            Collections.<String, Object>emptyMap()
        ));
    }

    private BusinessIntentIr buildBusinessIntent(Map<String, Object> advancedProfile,
                                                 List<Map<String, Object>> projections,
                                                 List<Map<String, Object>> predicates,
                                                 List<Map<String, Object>> aggregations,
                                                 List<Map<String, Object>> groupBy) {
        List<Map<String, Object>> timeAnchors = new ArrayList<Map<String, Object>>();
        timeAnchors.addAll(IrCollections.mapList(advancedProfile.get("timeFunctions")));
        addTimePredicateAnchors(timeAnchors, predicates);

        List<Map<String, Object>> dimensions = new ArrayList<Map<String, Object>>(groupBy);
        for (Map<String, Object> projection : projections) {
            String projectionType = IrCollections.text(projection.get("expressionType"));
            if ("COLUMN".equals(projectionType) && !containsExpression(dimensions, projection.get("expression"))) {
                dimensions.add(projection);
            }
        }

        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("intentStatus", aggregations.isEmpty() && dimensions.isEmpty() ? "PARTIAL" : "DERIVED");
        attributes.put("derivationBoundary", "STATIC_HEURISTIC_NOT_BUSINESS_TRUTH");
        attributes.put("pattern", "{时间锚点, 度量, 维度, 筛选}");
        return new BusinessIntentIr(timeAnchors, aggregations, dimensions, predicates, attributes);
    }

    private void addTimePredicateAnchors(List<Map<String, Object>> timeAnchors,
                                         List<Map<String, Object>> predicates) {
        for (Map<String, Object> predicate : predicates) {
            List<String> columns = IrCollections.stringList(predicate.get("sourceColumns"));
            for (String column : columns) {
                if (TIME_COLUMN_PATTERN.matcher(column).find()) {
                    LinkedHashMap<String, Object> anchor = new LinkedHashMap<String, Object>();
                    anchor.put("source", "PREDICATE_COLUMN");
                    anchor.put("column", column);
                    anchor.put("expression", predicate.get("expression"));
                    timeAnchors.add(anchor);
                }
            }
        }
    }

    private boolean containsExpression(List<Map<String, Object>> values, Object expression) {
        String expected = IrCollections.text(expression);
        for (Map<String, Object> value : values) {
            if (expected.equals(IrCollections.text(value.get("expression")))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private List<RewriteIrConflict> architectureConflicts() {
        return Collections.singletonList(new RewriteIrConflict(
            "IR_LAYER_RULE_LEVEL_NAME_OVERLAP",
            "HARN-130 推荐规则等级与本任务改写 IR 层级都使用 L1/L2 字样",
            "如果共用 level 字段，推荐规则层级可能被误读为架构 IR 层级。",
            "使用 irLayerCode=L1_AST..L5_BUSINESS_INTENT 表示新 IR，保留 recommendation rule level=L0/L1/L2 表示规则等级。",
            Arrays.asList(
                "将业务意图架构改名为 B1-B5，但会偏离本次用户给定规范。",
                "立即重命名 HARN-130 规则等级字段，但会破坏现有推荐 payload 兼容。"
            )
        ));
    }
}
