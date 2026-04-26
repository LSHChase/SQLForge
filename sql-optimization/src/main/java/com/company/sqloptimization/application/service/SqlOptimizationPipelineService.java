package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.WithItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Service;

@Service
public class SqlOptimizationPipelineService {

    private static final Pattern DATE_PREDICATE_PATTERN =
        Pattern.compile("([A-Z0-9_\\.]*?(DATE|TIME|DT|DAY))[\\s]*(=|>|<|BETWEEN|IN)");
    private static final Set<String> AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("COUNT", "SUM", "AVG", "MIN", "MAX", "APPROX_DISTINCT"));

    public ParsedSqlProfile analyze(String sqlText, DataSourceTypeEnum datasourceType) {
        String normalizedSql = normalizeSql(sqlText);
        if (normalizedSql.isEmpty()) {
            throw invalidTask(
                "Real optimization pipeline requires SQL text instead of an empty payload.",
                "Submit the original SQL text so parser, rewrite, and acceleration analysis can run."
            );
        }
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(normalizedSql);
        } catch (JSQLParserException ex) {
            throw parserFailure(
                "SQL parser could not build an AST for the submitted statement.",
                "Submit a single supported SELECT statement, or extend parser support for datasource "
                    + datasourceType.name() + ".",
                Collections.singletonList(
                    new OptimizationTaskRisk(
                        "HIGH",
                        "UNSUPPORTED_DIALECT",
                        "The submitted SQL could not be parsed into a supported AST.",
                        "Keep the statement to a single SELECT/WITH query or extend the parser coverage for this datasource."
                    )
                ),
                ex
            );
        }
        if (!(statement instanceof Select)) {
            throw invalidTask(
                "Real optimization currently supports SELECT/WITH statements only.",
                "Submit a read-oriented SELECT statement for parse, rewrite, or acceleration analysis."
            );
        }
        Select select = (Select) statement;
        ParsedSqlProfile profile = new ParsedSqlProfile(normalizedSql, select);
        profile.tables.addAll(deduplicate(new TablesNamesFinder().getTableList(statement)));
        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                if (withItem.getSubSelect() != null) {
                    analyzeSelectBody(withItem.getSubSelect().getSelectBody(), profile);
                }
            }
        }
        analyzeSelectBody(select.getSelectBody(), profile);
        finalizeWarnings(profile);
        return profile;
    }

    public OptimizationTaskSuggestion buildParseSuggestion(ParsedSqlProfile profile) {
        List<OptimizationTaskArtifact> artifacts = new ArrayList<OptimizationTaskArtifact>();
        artifacts.add(new OptimizationTaskArtifact("AST_PROFILE", "astProfile", JsonUtils.toJson(profile.toAstProfile())));
        artifacts.add(new OptimizationTaskArtifact("TABLE_LINEAGE", "tables", JsonUtils.toJson(profile.tables)));
        artifacts.add(new OptimizationTaskArtifact("HOTSPOT_FLAGS", "warnings", JsonUtils.toJson(profile.warnings)));

        List<OptimizationTaskBenefit> benefits = Arrays.asList(
            new OptimizationTaskBenefit(
                "REWRITE_READINESS",
                Integer.valueOf(clamp(40 + profile.predicateCount * 5 + profile.joinCount * 4, 20, 85)),
                "AST analysis isolates tables, predicates, and aggregates so later rewrite rules can stay deterministic."
            ),
            new OptimizationTaskBenefit(
                "ACCELERATION_SIGNAL",
                Integer.valueOf(clamp(35 + profile.aggregateFunctions.size() * 10 + profile.datePredicateColumns.size() * 8, 15, 80)),
                "The parsed shape already exposes partition, precompute, and replacement candidates."
            )
        );
        List<OptimizationTaskCost> costs = Collections.singletonList(
            new OptimizationTaskCost(
                "PARSER_OVERHEAD",
                "LOW",
                "The parser runs in-process and only materializes statement metadata for the offline optimization task."
            )
        );
        List<OptimizationTaskRisk> risks = buildShapeRisks(profile);
        return new OptimizationTaskSuggestion(
            buildParseSummary(profile),
            buildParseRecommendation(profile),
            Integer.valueOf(calculateParseConfidence(profile)),
            artifacts,
            benefits,
            costs,
            risks
        );
    }

    public OptimizationTaskSuggestion buildRewriteSuggestion(ParsedSqlProfile profile) {
        RewriteOutcome outcome = applyRewriteRules(profile.selectStatement);
        List<OptimizationTaskRisk> risks = new ArrayList<OptimizationTaskRisk>(buildShapeRisks(profile));
        if (outcome.appliedRules.isEmpty()) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "NO_SAFE_AUTOMATIC_REWRITE",
                    "No conservative AST rewrite rule matched the submitted statement.",
                    "Use the parse artifact and plan trace to review projection, predicates, and engine-specific tuning manually."
                )
            );
        } else {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "SEMANTIC_VALIDATION_REQUIRED",
                    "Even safe syntactic rewrites still require result-set diff validation before approval.",
                    "Compare the rewritten SQL against the original statement on a representative sample dataset."
                )
            );
        }
        List<OptimizationTaskArtifact> artifacts = Arrays.asList(
            new OptimizationTaskArtifact("REWRITTEN_SQL", "candidateSql", outcome.rewrittenSql),
            new OptimizationTaskArtifact("REWRITE_RULE_TRACE", "appliedRules", JsonUtils.toJson(outcome.appliedRules)),
            new OptimizationTaskArtifact("AST_PROFILE", "astProfile", JsonUtils.toJson(profile.toAstProfile()))
        );
        List<OptimizationTaskBenefit> benefits = Arrays.asList(
            new OptimizationTaskBenefit(
                "PLAN_SIMPLIFICATION",
                Integer.valueOf(clamp(20 + outcome.appliedRules.size() * 12, 10, 70)),
                "Removing duplicate predicates, grouping keys, or order-by items keeps the logical plan smaller and easier to verify."
            ),
            new OptimizationTaskBenefit(
                "RULE_TRACEABILITY",
                Integer.valueOf(clamp(30 + outcome.appliedRules.size() * 10, 15, 75)),
                "Each rewrite is recorded as a deterministic rule trace instead of an opaque placeholder summary."
            )
        );
        List<OptimizationTaskCost> costs = Arrays.asList(
            new OptimizationTaskCost(
                "VALIDATION",
                outcome.appliedRules.isEmpty() ? "LOW" : "MEDIUM",
                "The candidate SQL should be diff-checked against the original statement before any later approval or apply step."
            ),
            new OptimizationTaskCost(
                "RULE_COVERAGE",
                "LOW",
                "Current rewrite rules stay intentionally conservative and do not attempt schema-dependent projection expansion."
            )
        );
        String summary = outcome.appliedRules.isEmpty()
            ? "Parsed the statement successfully but found no conservative automatic rewrite candidate."
            : "Generated a rewritten SQL candidate with " + outcome.appliedRules.size() + " safe AST rule(s).";
        String recommendation = outcome.appliedRules.isEmpty()
            ? "Use the parse artifact to review projection width, filter placement, and engine-specific hints manually."
            : "Validate the rewritten candidate against the original statement, then carry the approved SQL into the next governance step.";
        return new OptimizationTaskSuggestion(
            summary,
            recommendation,
            Integer.valueOf(calculateRewriteConfidence(profile, outcome)),
            artifacts,
            benefits,
            costs,
            risks
        );
    }

    public OptimizationTaskSuggestion buildAccelerationSuggestion(ParsedSqlProfile profile,
                                                                 List<AccelerationSuggestionType> requestedTypes) {
        LinkedHashMap<AccelerationSuggestionType, String> reasons = deriveAccelerationReasons(profile);
        LinkedHashMap<AccelerationSuggestionType, String> filteredReasons = filterRequestedTypes(reasons, requestedTypes);
        if (filteredReasons.isEmpty()) {
            filteredReasons.put(
                AccelerationSuggestionType.REPLACE,
                "No strong physical-design signal was detected, so the safe default is to replace the wide raw query with a curated serving view."
            );
        }
        List<OptimizationTaskArtifact> artifacts = Arrays.asList(
            new OptimizationTaskArtifact("ACCELERATION_PLAN", "recommendedTypes", JsonUtils.toJson(filteredReasons)),
            new OptimizationTaskArtifact("SIGNAL_PROFILE", "signalProfile", JsonUtils.toJson(profile.toAccelerationSignalProfile())),
            new OptimizationTaskArtifact("TABLE_LINEAGE", "tables", JsonUtils.toJson(profile.tables))
        );
        List<OptimizationTaskBenefit> benefits = Arrays.asList(
            new OptimizationTaskBenefit(
                "LATENCY",
                Integer.valueOf(clamp(25 + filteredReasons.size() * 12 + profile.aggregateFunctions.size() * 6, 20, 85)),
                "The derived plan targets the query shapes that dominate parse-time hotspots and repeated heavy scans."
            ),
            new OptimizationTaskBenefit(
                "SCANNED_ROWS",
                Integer.valueOf(clamp(20 + profile.datePredicateColumns.size() * 15 + profile.joinCount * 8, 15, 88)),
                "Partition, bucket, or replacement hints reduce the amount of hot data touched by repeated executions."
            )
        );
        List<OptimizationTaskCost> costs = Arrays.asList(
            new OptimizationTaskCost(
                "STORAGE_OR_REFRESH",
                filteredReasons.containsKey(AccelerationSuggestionType.PRECOMPUTE) ? "HIGH" : "MEDIUM",
                "Precompute and replacement strategies add storage or refresh overhead that must be justified by repeated query demand."
            ),
            new OptimizationTaskCost(
                "GOVERNANCE_FOLLOW_UP",
                "MEDIUM",
                "Acceleration remains a governed object and still needs later approval, validation, and rollback semantics."
            )
        );
        List<OptimizationTaskRisk> risks = new ArrayList<OptimizationTaskRisk>(buildShapeRisks(profile));
        risks.add(
            new OptimizationTaskRisk(
                "MEDIUM",
                "FRESHNESS_AND_ROLLBACK",
                "Acceleration plans can trade freshness or operational simplicity for speed if they are applied without governance.",
                "Keep approval, activation, validation, and rollback evidence explicit before any future apply step."
            )
        );
        String summary = "Derived " + filteredReasons.size() + " acceleration recommendation(s) from the real SQL shape.";
        String recommendation = "Start with the highest-signal acceleration type, then validate benefit and freshness before any later apply workflow.";
        return new OptimizationTaskSuggestion(
            summary,
            recommendation,
            Integer.valueOf(calculateAccelerationConfidence(profile, filteredReasons)),
            artifacts,
            benefits,
            costs,
            risks
        );
    }

    private void analyzeSelectBody(SelectBody selectBody, ParsedSqlProfile profile) {
        if (selectBody == null) {
            return;
        }
        if (selectBody instanceof PlainSelect) {
            analyzePlainSelect((PlainSelect) selectBody, profile);
            return;
        }
        if (selectBody instanceof SetOperationList) {
            profile.setOperation = true;
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (setOperationList.getSelects() != null) {
                for (SelectBody nestedBody : setOperationList.getSelects()) {
                    analyzeSelectBody(nestedBody, profile);
                }
            }
            if (setOperationList.getOrderByElements() != null) {
                profile.orderByCount += setOperationList.getOrderByElements().size();
            }
            if (setOperationList.getLimit() != null) {
                profile.limitPresent = true;
            }
            return;
        }
        if (selectBody instanceof WithItem) {
            WithItem withItem = (WithItem) selectBody;
            if (withItem.getSubSelect() != null) {
                analyzeSelectBody(withItem.getSubSelect().getSelectBody(), profile);
            }
        }
    }

    private void analyzePlainSelect(PlainSelect plainSelect, ParsedSqlProfile profile) {
        if (plainSelect.getSelectItems() != null) {
            profile.projectionCount += plainSelect.getSelectItems().size();
            for (SelectItem selectItem : plainSelect.getSelectItems()) {
                if (selectItem instanceof AllColumns || selectItem instanceof AllTableColumns) {
                    profile.selectStar = true;
                    continue;
                }
                if (selectItem instanceof SelectExpressionItem) {
                    Expression expression = ((SelectExpressionItem) selectItem).getExpression();
                    collectExpressionSignals(expression, profile.projectedColumns, profile.aggregateFunctions);
                }
            }
        }
        if (plainSelect.getJoins() != null) {
            profile.joinCount += plainSelect.getJoins().size();
            for (Join join : plainSelect.getJoins()) {
                if (join.getOnExpression() != null) {
                    profile.predicateCount += countPredicates(join.getOnExpression());
                    collectExpressionSignals(join.getOnExpression(), null, null);
                }
            }
        }
        if (plainSelect.getWhere() != null) {
            profile.predicateCount += countPredicates(plainSelect.getWhere());
            profile.datePredicateColumns.addAll(extractDatePredicateColumns(plainSelect.getWhere()));
        }
        if (plainSelect.getHaving() != null) {
            profile.predicateCount += countPredicates(plainSelect.getHaving());
        }
        if (plainSelect.getGroupBy() != null && plainSelect.getGroupBy().getGroupByExpressions() != null) {
            profile.groupByCount += plainSelect.getGroupBy().getGroupByExpressions().size();
            for (Expression expression : plainSelect.getGroupBy().getGroupByExpressions()) {
                collectExpressionSignals(expression, null, profile.aggregateFunctions);
            }
        }
        if (plainSelect.getOrderByElements() != null) {
            profile.orderByCount += plainSelect.getOrderByElements().size();
        }
        if (plainSelect.getLimit() != null) {
            profile.limitPresent = true;
        }
        if (plainSelect.getDistinct() != null) {
            profile.distinctPresent = true;
        }
        if (plainSelect.getFromItem() instanceof SubSelect) {
            analyzeSelectBody(((SubSelect) plainSelect.getFromItem()).getSelectBody(), profile);
        }
    }

    private void collectExpressionSignals(Expression expression, Set<String> projectedColumns, Set<String> aggregateFunctions) {
        if (expression == null) {
            return;
        }
        if (expression instanceof Column) {
            if (projectedColumns != null) {
                projectedColumns.add(((Column) expression).getFullyQualifiedName());
            }
            return;
        }
        if (expression instanceof Function) {
            Function function = (Function) expression;
            if (function.getName() != null) {
                String upperName = function.getName().toUpperCase(Locale.ROOT);
                if (aggregateFunctions != null && AGGREGATE_FUNCTIONS.contains(upperName)) {
                    aggregateFunctions.add(upperName);
                }
            }
            if (function.getParameters() != null && function.getParameters().getExpressions() != null) {
                for (Expression parameter : function.getParameters().getExpressions()) {
                    collectExpressionSignals(parameter, projectedColumns, aggregateFunctions);
                }
            }
            return;
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            collectExpressionSignals(binaryExpression.getLeftExpression(), projectedColumns, aggregateFunctions);
            collectExpressionSignals(binaryExpression.getRightExpression(), projectedColumns, aggregateFunctions);
            return;
        }
        if (expression instanceof Parenthesis) {
            collectExpressionSignals(((Parenthesis) expression).getExpression(), projectedColumns, aggregateFunctions);
        }
    }

    private int countPredicates(Expression expression) {
        if (expression == null) {
            return 0;
        }
        if (expression instanceof Parenthesis) {
            return countPredicates(((Parenthesis) expression).getExpression());
        }
        if (expression instanceof AndExpression) {
            AndExpression andExpression = (AndExpression) expression;
            return countPredicates(andExpression.getLeftExpression()) + countPredicates(andExpression.getRightExpression());
        }
        return 1;
    }

    private List<String> extractDatePredicateColumns(Expression expression) {
        if (expression == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        Matcher matcher = DATE_PREDICATE_PATTERN.matcher(expression.toString().toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return new ArrayList<String>(columns);
    }

    private void finalizeWarnings(ParsedSqlProfile profile) {
        if (profile.selectStar) {
            profile.warnings.add("SELECT_STAR");
        }
        if (profile.predicateCount == 0) {
            profile.warnings.add("NO_PREDICATE");
        }
        if (profile.orderByCount > 0 && !profile.limitPresent) {
            profile.warnings.add("ORDER_BY_WITHOUT_LIMIT");
        }
        if (profile.joinCount >= 3) {
            profile.warnings.add("HEAVY_JOIN_GRAPH");
        }
    }

    private RewriteOutcome applyRewriteRules(Select select) {
        LinkedHashSet<String> appliedRules = new LinkedHashSet<String>();
        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                if (withItem.getSubSelect() != null) {
                    applyRewriteRules(withItem.getSubSelect().getSelectBody(), appliedRules);
                }
            }
        }
        applyRewriteRules(select.getSelectBody(), appliedRules);
        return new RewriteOutcome(select.toString(), new ArrayList<String>(appliedRules));
    }

    private void applyRewriteRules(SelectBody selectBody, Set<String> appliedRules) {
        if (selectBody == null) {
            return;
        }
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            rewriteCountLiteralFunctions(plainSelect, appliedRules);
            deduplicateAndPredicates(plainSelect, appliedRules);
            deduplicateGroupBy(plainSelect, appliedRules);
            deduplicateOrderBy(plainSelect, appliedRules);
            if (plainSelect.getFromItem() instanceof SubSelect) {
                applyRewriteRules(((SubSelect) plainSelect.getFromItem()).getSelectBody(), appliedRules);
            }
            return;
        }
        if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            if (setOperationList.getSelects() != null) {
                for (SelectBody nestedBody : setOperationList.getSelects()) {
                    applyRewriteRules(nestedBody, appliedRules);
                }
            }
        }
    }

    private void rewriteCountLiteralFunctions(PlainSelect plainSelect, Set<String> appliedRules) {
        if (plainSelect.getSelectItems() == null) {
            return;
        }
        for (SelectItem selectItem : plainSelect.getSelectItems()) {
            if (!(selectItem instanceof SelectExpressionItem)) {
                continue;
            }
            Expression expression = ((SelectExpressionItem) selectItem).getExpression();
            if (!(expression instanceof Function)) {
                continue;
            }
            Function function = (Function) expression;
            if (!"COUNT".equalsIgnoreCase(function.getName())
                || function.isAllColumns()
                || function.getParameters() == null
                || function.getParameters().getExpressions() == null
                || function.getParameters().getExpressions().size() != 1) {
                continue;
            }
            Expression parameter = function.getParameters().getExpressions().get(0);
            if (isNonNullLiteral(parameter)) {
                function.setAllColumns(true);
                function.setParameters(null);
                appliedRules.add("COUNT_LITERAL_TO_COUNT_STAR");
            }
        }
    }

    private void deduplicateAndPredicates(PlainSelect plainSelect, Set<String> appliedRules) {
        if (plainSelect.getWhere() != null) {
            Expression deduplicatedWhere = deduplicateAndExpression(plainSelect.getWhere());
            if (!plainSelect.getWhere().toString().equals(deduplicatedWhere.toString())) {
                plainSelect.setWhere(deduplicatedWhere);
                appliedRules.add("DEDUPLICATE_WHERE_PREDICATES");
            }
        }
        if (plainSelect.getHaving() != null) {
            Expression deduplicatedHaving = deduplicateAndExpression(plainSelect.getHaving());
            if (!plainSelect.getHaving().toString().equals(deduplicatedHaving.toString())) {
                plainSelect.setHaving(deduplicatedHaving);
                appliedRules.add("DEDUPLICATE_HAVING_PREDICATES");
            }
        }
    }

    private Expression deduplicateAndExpression(Expression expression) {
        List<Expression> flattened = new ArrayList<Expression>();
        flattenAndExpression(expression, flattened);
        LinkedHashMap<String, Expression> unique = new LinkedHashMap<String, Expression>();
        for (Expression item : flattened) {
            unique.put(item.toString().toUpperCase(Locale.ROOT), item);
        }
        return rebuildAndExpression(unique.values());
    }

    private void flattenAndExpression(Expression expression, List<Expression> collector) {
        if (expression instanceof Parenthesis) {
            flattenAndExpression(((Parenthesis) expression).getExpression(), collector);
            return;
        }
        if (expression instanceof AndExpression) {
            AndExpression andExpression = (AndExpression) expression;
            flattenAndExpression(andExpression.getLeftExpression(), collector);
            flattenAndExpression(andExpression.getRightExpression(), collector);
            return;
        }
        collector.add(expression);
    }

    private Expression rebuildAndExpression(Collection<Expression> expressions) {
        Expression result = null;
        for (Expression expression : expressions) {
            if (result == null) {
                result = expression;
            } else {
                result = new AndExpression(result, expression);
            }
        }
        return result;
    }

    private void deduplicateGroupBy(PlainSelect plainSelect, Set<String> appliedRules) {
        GroupByElement groupBy = plainSelect.getGroupBy();
        if (groupBy == null || groupBy.getGroupByExpressions() == null || groupBy.getGroupByExpressions().isEmpty()) {
            return;
        }
        LinkedHashMap<String, Expression> unique = new LinkedHashMap<String, Expression>();
        for (Expression expression : groupBy.getGroupByExpressions()) {
            unique.put(expression.toString().toUpperCase(Locale.ROOT), expression);
        }
        if (unique.size() != groupBy.getGroupByExpressions().size()) {
            groupBy.setGroupByExpressions(new ArrayList<Expression>(unique.values()));
            appliedRules.add("DEDUPLICATE_GROUP_BY_KEYS");
        }
    }

    private void deduplicateOrderBy(PlainSelect plainSelect, Set<String> appliedRules) {
        if (plainSelect.getOrderByElements() == null || plainSelect.getOrderByElements().isEmpty()) {
            return;
        }
        LinkedHashMap<String, OrderByElement> unique = new LinkedHashMap<String, OrderByElement>();
        for (OrderByElement orderByElement : plainSelect.getOrderByElements()) {
            unique.put(orderByElement.toString().toUpperCase(Locale.ROOT), orderByElement);
        }
        if (unique.size() != plainSelect.getOrderByElements().size()) {
            plainSelect.setOrderByElements(new ArrayList<OrderByElement>(unique.values()));
            appliedRules.add("DEDUPLICATE_ORDER_BY_KEYS");
        }
    }

    private LinkedHashMap<AccelerationSuggestionType, String> deriveAccelerationReasons(ParsedSqlProfile profile) {
        LinkedHashMap<AccelerationSuggestionType, String> reasons = new LinkedHashMap<AccelerationSuggestionType, String>();
        if (!profile.aggregateFunctions.isEmpty() || profile.groupByCount > 0) {
            reasons.put(
                AccelerationSuggestionType.PRECOMPUTE,
                "Aggregate functions and grouping keys indicate repeated precompute or materialized-view value."
            );
        }
        if (!profile.datePredicateColumns.isEmpty() || profile.predicateCount >= 2) {
            reasons.put(
                AccelerationSuggestionType.PARTITION,
                "Date-like filters or repeated predicates indicate partition pruning opportunities."
            );
        }
        if (profile.joinCount > 0) {
            reasons.put(
                AccelerationSuggestionType.BUCKET,
                "Join activity suggests bucket alignment or co-location may reduce shuffle overhead."
            );
        }
        if (profile.joinCount >= 3 || profile.setOperation) {
            reasons.put(
                AccelerationSuggestionType.SPLIT,
                "The query graph is large enough that staged execution or decomposition should be evaluated."
            );
        }
        if (profile.selectStar || profile.tables.size() >= 4) {
            reasons.put(
                AccelerationSuggestionType.REPLACE,
                "Wide projections or large table graphs suggest replacing the raw query with a curated serving object."
            );
        }
        return reasons;
    }

    private LinkedHashMap<AccelerationSuggestionType, String> filterRequestedTypes(
        LinkedHashMap<AccelerationSuggestionType, String> reasons,
        List<AccelerationSuggestionType> requestedTypes
    ) {
        if (requestedTypes == null || requestedTypes.isEmpty() || requestedTypes.contains(AccelerationSuggestionType.ALL)) {
            return new LinkedHashMap<AccelerationSuggestionType, String>(reasons);
        }
        LinkedHashMap<AccelerationSuggestionType, String> filtered = new LinkedHashMap<AccelerationSuggestionType, String>();
        for (AccelerationSuggestionType requestedType : requestedTypes) {
            if (reasons.containsKey(requestedType)) {
                filtered.put(requestedType, reasons.get(requestedType));
            }
        }
        return filtered;
    }

    private List<OptimizationTaskRisk> buildShapeRisks(ParsedSqlProfile profile) {
        List<OptimizationTaskRisk> risks = new ArrayList<OptimizationTaskRisk>();
        if (profile.selectStar) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "SELECT_STAR",
                    "Wide projection hides the actual column footprint and makes downstream rewrites less deterministic.",
                    "Replace star projection with explicit columns before approving rewrites or accelerations."
                )
            );
        }
        if (profile.predicateCount == 0) {
            risks.add(
                new OptimizationTaskRisk(
                    "HIGH",
                    "FULL_SCAN_RISK",
                    "The parsed statement has no filtering predicate and is likely to scan the full dataset.",
                    "Add tenant, time, or business-key predicates before online use."
                )
            );
        }
        if (profile.orderByCount > 0 && !profile.limitPresent) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "ORDER_BY_WITHOUT_LIMIT",
                    "Sorting without a limiting clause can create unnecessary wide shuffle or memory pressure.",
                    "Add a limit or move sort work to a precomputed serving object."
                )
            );
        }
        if (profile.joinCount >= 3) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "HEAVY_JOIN_GRAPH",
                    "The parsed query joins multiple datasets and may need staged execution or stronger acceleration.",
                    "Review join keys, filter placement, and serving-layer alternatives before approval."
                )
            );
        }
        return risks;
    }

    private String buildParseSummary(ParsedSqlProfile profile) {
        return "Parsed a SELECT statement across "
            + profile.tables.size()
            + " table(s), "
            + profile.joinCount
            + " join(s), and "
            + profile.predicateCount
            + " predicate fragment(s).";
    }

    private String buildParseRecommendation(ParsedSqlProfile profile) {
        if (profile.selectStar) {
            return "Replace the star projection first, then use the same AST profile for rewrite and acceleration review.";
        }
        if (profile.predicateCount == 0) {
            return "Add explicit filters before carrying this statement into rewrite or acceleration planning.";
        }
        if (!profile.aggregateFunctions.isEmpty()) {
            return "Use the parsed aggregates and group keys to evaluate precompute or serving-layer acceleration.";
        }
        return "Use the AST profile as the canonical input for later rewrite validation and acceleration planning.";
    }

    private int calculateParseConfidence(ParsedSqlProfile profile) {
        return clamp(92 - profile.warnings.size() * 8 - profile.joinCount * 2, 45, 96);
    }

    private int calculateRewriteConfidence(ParsedSqlProfile profile, RewriteOutcome outcome) {
        return clamp(58 + outcome.appliedRules.size() * 9 - profile.warnings.size() * 4, 35, 92);
    }

    private int calculateAccelerationConfidence(ParsedSqlProfile profile,
                                                LinkedHashMap<AccelerationSuggestionType, String> reasons) {
        return clamp(54 + reasons.size() * 8 + profile.aggregateFunctions.size() * 3 - profile.warnings.size() * 3, 40, 90);
    }

    private boolean isNonNullLiteral(Expression expression) {
        return expression instanceof LongValue
            || expression instanceof DoubleValue
            || expression instanceof StringValue
            || expression instanceof DateValue
            || expression instanceof TimestampValue
            || (expression != null && !(expression instanceof NullValue) && expression.toString().startsWith(":"));
    }

    private SqlOptimizationExecutionException invalidTask(String message, String suggestedAction) {
        return new SqlOptimizationExecutionException(
            ErrorCodeConstants.SQL_OPTIMIZATION_TASK_INVALID,
            message,
            suggestedAction,
            false,
            OptimizationTaskPhase.DEEP_PARSING,
            Collections.singletonList(
                new OptimizationTaskRisk(
                    "HIGH",
                    "TASK_INPUT_INVALID",
                    message,
                    suggestedAction
                )
            ),
            null
        );
    }

    private SqlOptimizationExecutionException parserFailure(String message,
                                                            String suggestedAction,
                                                            List<OptimizationTaskRisk> risks,
                                                            Exception cause) {
        return new SqlOptimizationExecutionException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_PARSER_FAILURE,
            message,
            suggestedAction,
            false,
            OptimizationTaskPhase.DEEP_PARSING,
            risks,
            cause
        );
    }

    private String normalizeSql(String sqlText) {
        if (sqlText == null) {
            return "";
        }
        String normalized = sqlText.trim();
        while (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private List<String> deduplicate(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(new LinkedHashSet<String>(input));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static final class ParsedSqlProfile {

        private final String normalizedSql;
        private final Select selectStatement;
        private final List<String> tables = new ArrayList<String>();
        private final Set<String> projectedColumns = new LinkedHashSet<String>();
        private final Set<String> aggregateFunctions = new LinkedHashSet<String>();
        private final Set<String> datePredicateColumns = new LinkedHashSet<String>();
        private final List<String> warnings = new ArrayList<String>();
        private int projectionCount;
        private int predicateCount;
        private int joinCount;
        private int groupByCount;
        private int orderByCount;
        private boolean selectStar;
        private boolean limitPresent;
        private boolean distinctPresent;
        private boolean setOperation;

        private ParsedSqlProfile(String normalizedSql, Select selectStatement) {
            this.normalizedSql = normalizedSql;
            this.selectStatement = selectStatement;
        }

        private Map<String, Object> toAstProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("statementType", "SELECT");
            payload.put("tables", tables);
            payload.put("projectionCount", Integer.valueOf(projectionCount));
            payload.put("projectedColumns", new ArrayList<String>(projectedColumns));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("orderByCount", Integer.valueOf(orderByCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("limitPresent", Boolean.valueOf(limitPresent));
            payload.put("distinctPresent", Boolean.valueOf(distinctPresent));
            payload.put("setOperation", Boolean.valueOf(setOperation));
            return payload;
        }

        private Map<String, Object> toAccelerationSignalProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("tables", tables);
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("warnings", warnings);
            return payload;
        }
    }

    private static final class RewriteOutcome {

        private final String rewrittenSql;
        private final List<String> appliedRules;

        private RewriteOutcome(String rewrittenSql, List<String> appliedRules) {
            this.rewrittenSql = rewrittenSql;
            this.appliedRules = appliedRules;
        }
    }

    public static final class SqlOptimizationExecutionException extends RuntimeException {

        private final int code;
        private final String suggestedAction;
        private final boolean retryable;
        private final OptimizationTaskPhase failedPhase;
        private final List<OptimizationTaskRisk> risks;

        private SqlOptimizationExecutionException(int code,
                                                  String message,
                                                  String suggestedAction,
                                                  boolean retryable,
                                                  OptimizationTaskPhase failedPhase,
                                                  List<OptimizationTaskRisk> risks,
                                                  Throwable cause) {
            super(message, cause);
            this.code = code;
            this.suggestedAction = suggestedAction;
            this.retryable = retryable;
            this.failedPhase = failedPhase;
            this.risks = risks == null ? Collections.<OptimizationTaskRisk>emptyList() : risks;
        }

        public int getCode() {
            return code;
        }

        public String getSuggestedAction() {
            return suggestedAction;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public OptimizationTaskPhase getFailedPhase() {
            return failedPhase;
        }

        public List<OptimizationTaskRisk> getRisks() {
            return risks;
        }
    }
}
