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
import com.company.sqloptimization.domain.parse.SqlParserMode;
import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.AstVisitor;
import io.trino.sql.tree.DereferenceExpression;
import io.trino.sql.tree.FunctionCall;
import io.trino.sql.tree.JoinCriteria;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.SingleColumn;
import io.trino.sql.tree.Table;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsList;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
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
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

@Service
public class SqlOptimizationPipelineService {

    private static final Pattern DATE_PREDICATE_PATTERN =
        Pattern.compile("([A-Z0-9_\\.]*?(DATE|TIME|DT|DAY))[\\s]*(=|>|<|BETWEEN|IN)");
    private static final Set<String> AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("COUNT", "SUM", "AVG", "MIN", "MAX", "APPROX_DISTINCT", "GROUP_CONCAT"));
    private static final Set<String> BUILT_IN_SCALAR_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("DATE_TRUNC", "CAST", "COALESCE", "IF", "NULLIF", "LOWER", "UPPER"));

    @Value("${sql-optimization.parser.strategy:JSQLPARSER}")
    private String parserStrategy = "JSQLPARSER";

    private final Map<SqlParserMode, SqlStructureParserAdapter> parserAdapters =
        new LinkedHashMap<SqlParserMode, SqlStructureParserAdapter>();

    public SqlOptimizationPipelineService() {
        registerParserAdapter(new JsqlParserAdapter());
        registerParserAdapter(new ApacheCalciteParserAdapter());
    }

    private void registerParserAdapter(SqlStructureParserAdapter adapter) {
        parserAdapters.put(adapter.parserMode(), adapter);
    }

    public ParsedSqlProfile analyze(String sqlText, DataSourceTypeEnum datasourceType) {
        return analyze(sqlText, datasourceType, null);
    }

    public ParsedSqlProfile analyze(String sqlText, DataSourceTypeEnum datasourceType, SqlParserMode parserMode) {
        String normalizedSql = normalizeSql(sqlText);
        if (normalizedSql.trim().isEmpty()) {
            throw invalidTask(
                "Real optimization pipeline requires SQL text instead of an empty payload.",
                "Submit the original SQL text so parser, rewrite, and acceleration analysis can run."
            );
        }
        SqlParserMode resolvedMode = (parserMode == null ? resolveDefaultParserMode() : parserMode).structureMode();
        SqlStructureParserAdapter adapter = parserAdapters.get(resolvedMode);
        if (adapter == null) {
            adapter = parserAdapters.get(SqlParserMode.JSQLPARSER);
        }
        return adapter.analyze(normalizedSql, datasourceType);
    }

    private SqlParserMode resolveDefaultParserMode() {
        String strategy = parserStrategy == null ? "JSQLPARSER" : parserStrategy.trim().toUpperCase(Locale.ROOT);
        return SqlParserMode.resolveDefault(strategy);
    }

    private interface SqlStructureParserAdapter {
        SqlParserMode parserMode();

        ParsedSqlProfile analyze(String normalizedSql, DataSourceTypeEnum datasourceType);
    }

    private final class JsqlParserAdapter implements SqlStructureParserAdapter {
        @Override
        public SqlParserMode parserMode() {
            return SqlParserMode.JSQLPARSER;
        }

        @Override
        public ParsedSqlProfile analyze(String normalizedSql, DataSourceTypeEnum datasourceType) {
            return analyzeWithJsqlParser(normalizedSql, datasourceType);
        }
    }

    private final class ApacheCalciteParserAdapter implements SqlStructureParserAdapter {
        @Override
        public SqlParserMode parserMode() {
            return SqlParserMode.APACHE_CALCITE;
        }

        @Override
        public ParsedSqlProfile analyze(String normalizedSql, DataSourceTypeEnum datasourceType) {
            return analyzeWithApacheCalciteParser(normalizedSql, datasourceType);
        }
    }

    private ParsedSqlProfile analyzeWithJsqlParser(String normalizedSql, DataSourceTypeEnum datasourceType) {
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
                ex,
                normalizedSql
            );
        }
        if (!(statement instanceof net.sf.jsqlparser.statement.select.Select)) {
            throw invalidTask(
                "Real optimization currently supports SELECT/WITH statements only.",
                "Submit a read-oriented SELECT statement for parse, rewrite, or acceleration analysis."
            );
        }
        Select select = (Select) statement;
        ParsedSqlProfile profile = new ParsedSqlProfile(normalizedSql);
        profile.parserEngine = "JSQLPARSER";
        List<String> discoveredTables = new TablesNamesFinder().getTableList(statement);
        profile.tables.addAll(deduplicate(discoveredTables));
        if (select.getWithItemsList() != null) {
            for (WithItem withItem : select.getWithItemsList()) {
                if (withItem.getSubSelect() != null) {
                    analyzeSelectBody(withItem.getSubSelect().getSelectBody(), profile);
                }
            }
        }
        analyzeSelectBody(select.getSelectBody(), profile);
        profile.rewriteOutcome = applyRewriteRules(select);
        finalizeWarnings(profile);
        return profile;
    }

    private ParsedSqlProfile analyzeWithTrinoParser(String normalizedSql, DataSourceTypeEnum datasourceType) {
        io.trino.sql.tree.Statement statement;
        try {
            statement = new SqlParser().createStatement(normalizedSql, new ParsingOptions());
        } catch (RuntimeException ex) {
            throw parserFailure(
                "Trino parser could not build an AST for the submitted statement.",
                "Submit a single supported SELECT/WITH query or keep the parser strategy on JSQLPARSER for this datasource.",
                Collections.singletonList(
                    new OptimizationTaskRisk(
                        "HIGH",
                        "UNSUPPORTED_TRINO_DIALECT",
                        "The submitted SQL could not be parsed by the Trino parser adapter.",
                        "Use a supported Trino SELECT query or keep the existing JSQLParser strategy."
                    )
                ),
                ex,
                normalizedSql
            );
        }
        if (!(statement instanceof Query)) {
            throw invalidTask(
                "Trino parser adapter currently supports SELECT/WITH statements only.",
                "Submit a read-oriented SELECT statement for query-intent analysis."
            );
        }
        ParsedSqlProfile profile = new ParsedSqlProfile(normalizedSql);
        profile.parserEngine = "TRINO";
        new TrinoProfileVisitor().process(statement, profile);
        finalizeWarnings(profile);
        return profile;
    }

    private ParsedSqlProfile analyzeWithApacheCalciteParser(String normalizedSql, DataSourceTypeEnum datasourceType) {
        SqlNode statement;
        try {
            org.apache.calcite.sql.parser.SqlParser.Config parserConfig =
                org.apache.calcite.sql.parser.SqlParser.config().withConformance(SqlConformanceEnum.LENIENT);
            statement = org.apache.calcite.sql.parser.SqlParser.create(normalizedSql, parserConfig).parseStmt();
        } catch (Exception ex) {
            throw parserFailure(
                "Apache Calcite parser could not build an AST for the submitted statement.",
                "Submit a single supported SELECT/WITH query or keep parserMode on JSQLPARSER for this datasource.",
                Collections.singletonList(
                    new OptimizationTaskRisk(
                        "HIGH",
                        "UNSUPPORTED_CALCITE_DIALECT",
                        "The submitted SQL could not be parsed by the Apache Calcite parser adapter.",
                        "Use a supported Calcite SELECT query or keep the existing JSQLParser mode."
                    )
                ),
                ex,
                normalizedSql
            );
        }
        if (!isCalciteSelectLike(statement)) {
            throw invalidTask(
                "Apache Calcite parser adapter currently supports SELECT/WITH statements only.",
                "Submit a read-oriented SELECT statement for query-intent analysis."
            );
        }
        ParsedSqlProfile profile = new ParsedSqlProfile(normalizedSql);
        profile.parserEngine = "APACHE_CALCITE";
        new ApacheCalciteProfileCollector().collect(statement, profile, true);
        finalizeWarnings(profile);
        return profile;
    }

    private boolean isCalciteSelectLike(SqlNode node) {
        if (node instanceof SqlSelect || node instanceof SqlWith || node instanceof SqlOrderBy) {
            return true;
        }
        if (node instanceof SqlCall) {
            SqlKind kind = node.getKind();
            return kind == SqlKind.UNION || kind == SqlKind.INTERSECT || kind == SqlKind.EXCEPT;
        }
        return false;
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
        RewriteOutcome outcome = profile == null ? RewriteOutcome.empty() : profile.getRewriteOutcome();
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

    public List<String> deriveRewriteCandidateRules(ParsedSqlProfile profile) {
        if (profile == null) {
            return Collections.emptyList();
        }
        return profile.getRewriteOutcome().appliedRules;
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
        profile.pushAliasScope(collectAliases(plainSelect));
        try {
            recordFromItemScan(plainSelect.getFromItem(), profile);
            if (plainSelect.getSelectItems() != null) {
                profile.projectionCount += plainSelect.getSelectItems().size();
                for (SelectItem selectItem : plainSelect.getSelectItems()) {
                    if (selectItem instanceof AllColumns || selectItem instanceof AllTableColumns) {
                        profile.selectStar = true;
                        continue;
                    }
                    if (selectItem instanceof SelectExpressionItem) {
                        Expression expression = ((SelectExpressionItem) selectItem).getExpression();
                        profile.recordExpression(expression);
                        if (expression instanceof SubSelect) {
                            profile.scalarSubqueryCount++;
                        }
                        collectExpressionSignals(expression, profile.projectedColumns, profile.aggregateFunctions, profile);
                    }
                }
            }
            if (plainSelect.getJoins() != null) {
                profile.joinCount += plainSelect.getJoins().size();
                for (Join join : plainSelect.getJoins()) {
                    profile.joinTypes.add(join.isInner() ? "INNER" : join.toString().split("\\s+")[0].toUpperCase(Locale.ROOT));
                    recordFromItemScan(join.getRightItem(), profile);
                    analyzeFromItem(join.getRightItem(), profile);
                    if (join.getOnExpression() != null) {
                        profile.predicateCount += countPredicates(join.getOnExpression());
                        profile.joinCriteriaCount += countPredicates(join.getOnExpression());
                        profile.recordExpression(join.getOnExpression());
                        collectExpressionSignals(join.getOnExpression(), null, null, profile);
                    }
                }
            }
            if (plainSelect.getWhere() != null) {
                profile.predicateCount += countPredicates(plainSelect.getWhere());
                profile.recordExpression(plainSelect.getWhere());
                profile.datePredicateColumns.addAll(extractDatePredicateColumns(plainSelect.getWhere()));
                collectExpressionSignals(plainSelect.getWhere(), null, null, profile);
            }
            if (plainSelect.getHaving() != null) {
                profile.predicateCount += countPredicates(plainSelect.getHaving());
                profile.recordExpression(plainSelect.getHaving());
                collectExpressionSignals(plainSelect.getHaving(), null, null, profile);
            }
            if (plainSelect.getGroupBy() != null && plainSelect.getGroupBy().getGroupByExpressions() != null) {
                profile.groupByCount += plainSelect.getGroupBy().getGroupByExpressions().size();
                for (Expression expression : plainSelect.getGroupBy().getGroupByExpressions()) {
                    profile.recordExpression(expression);
                    collectExpressionSignals(expression, null, profile.aggregateFunctions, profile);
                }
            }
            if (plainSelect.getOrderByElements() != null) {
                profile.orderByCount += plainSelect.getOrderByElements().size();
                for (OrderByElement orderByElement : plainSelect.getOrderByElements()) {
                    profile.recordExpression(orderByElement);
                    collectOrderBySignals(orderByElement, profile);
                }
            }
            if (plainSelect.getLimit() != null) {
                profile.limitPresent = true;
            }
            if (plainSelect.getDistinct() != null) {
                profile.distinctPresent = true;
            }
            analyzeFromItem(plainSelect.getFromItem(), profile);
        } finally {
            profile.popAliasScope();
        }
    }

    private void analyzeFromItem(FromItem fromItem, ParsedSqlProfile profile) {
        if (fromItem instanceof SubSelect) {
            processSubSelect((SubSelect) fromItem, profile);
        }
    }

    private Set<String> collectAliases(PlainSelect plainSelect) {
        LinkedHashSet<String> aliases = new LinkedHashSet<String>();
        addAlias(aliases, plainSelect.getFromItem());
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                addAlias(aliases, join.getRightItem());
            }
        }
        return aliases;
    }

    private void addAlias(Set<String> aliases, FromItem fromItem) {
        if (fromItem == null || fromItem.getAlias() == null || fromItem.getAlias().getName() == null) {
            return;
        }
        aliases.add(fromItem.getAlias().getName().toUpperCase(Locale.ROOT));
    }

    private void recordFromItemScan(FromItem fromItem, ParsedSqlProfile profile) {
        if (fromItem instanceof net.sf.jsqlparser.schema.Table) {
            net.sf.jsqlparser.schema.Table table = (net.sf.jsqlparser.schema.Table) fromItem;
            profile.recordTableScan(table.getFullyQualifiedName());
        }
    }

    private void collectExpressionSignals(Expression expression,
                                          Set<String> projectedColumns,
                                          Set<String> aggregateFunctions,
                                          ParsedSqlProfile profile) {
        if (expression == null) {
            return;
        }
        if (expression instanceof SubSelect) {
            processSubSelect((SubSelect) expression, profile);
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
                if (!AGGREGATE_FUNCTIONS.contains(upperName) && !BUILT_IN_SCALAR_FUNCTIONS.contains(upperName)) {
                    profile.udfFunctions.add(upperName);
                }
            }
            if (function.getParameters() != null && function.getParameters().getExpressions() != null) {
                for (Expression parameter : function.getParameters().getExpressions()) {
                    collectExpressionSignals(parameter, projectedColumns, aggregateFunctions, profile);
                }
            }
            if (function.getAttribute() != null) {
                collectExpressionSignals(function.getAttribute(), projectedColumns, aggregateFunctions, profile);
            }
            return;
        }
        if (expression instanceof AnalyticExpression) {
            profile.windowFunctionCount++;
            return;
        }
        if (expression instanceof OrExpression) {
            profile.orPredicateCount++;
        }
        if (expression instanceof NotExpression) {
            Expression innerExpression = ((NotExpression) expression).getExpression();
            if (innerExpression instanceof ExistsExpression) {
                profile.notExistsCount++;
            }
            collectExpressionSignals(innerExpression, projectedColumns, aggregateFunctions, profile);
            return;
        }
        if (expression instanceof ExistsExpression) {
            ExistsExpression existsExpression = (ExistsExpression) expression;
            if (existsExpression.isNot()) {
                profile.notExistsCount++;
            }
            collectExpressionSignals(existsExpression.getRightExpression(), projectedColumns, aggregateFunctions, profile);
            return;
        }
        if (expression instanceof InExpression) {
            InExpression inExpression = (InExpression) expression;
            collectExpressionSignals(inExpression.getLeftExpression(), projectedColumns, aggregateFunctions, profile);
            collectExpressionSignals(inExpression.getRightExpression(), projectedColumns, aggregateFunctions, profile);
            collectItemsListSignals(inExpression.getRightItemsList(), projectedColumns, aggregateFunctions, profile);
            return;
        }
        if (expression instanceof LikeExpression) {
            LikeExpression likeExpression = (LikeExpression) expression;
            if (isLeadingWildcardLike(likeExpression)) {
                profile.leadingWildcardLikeCount++;
            }
        }
        if (expression instanceof ComparisonOperator && hasFunctionWrappedOperand((ComparisonOperator) expression)) {
            profile.functionWrappedPredicateCount++;
        }
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            collectExpressionSignals(binaryExpression.getLeftExpression(), projectedColumns, aggregateFunctions, profile);
            collectExpressionSignals(binaryExpression.getRightExpression(), projectedColumns, aggregateFunctions, profile);
            return;
        }
        if (expression instanceof Parenthesis) {
            collectExpressionSignals(((Parenthesis) expression).getExpression(), projectedColumns, aggregateFunctions, profile);
        }
    }

    private void collectItemsListSignals(ItemsList itemsList,
                                         Set<String> projectedColumns,
                                         Set<String> aggregateFunctions,
                                         ParsedSqlProfile profile) {
        if (itemsList instanceof SubSelect) {
            processSubSelect((SubSelect) itemsList, profile);
            return;
        }
        if (itemsList instanceof ExpressionList) {
            ExpressionList expressionList = (ExpressionList) itemsList;
            if (expressionList.getExpressions() != null) {
                for (Expression item : expressionList.getExpressions()) {
                    collectExpressionSignals(item, projectedColumns, aggregateFunctions, profile);
                }
            }
        }
    }

    private void processSubSelect(SubSelect subSelect, ParsedSqlProfile profile) {
        if (subSelect == null) {
            return;
        }
        profile.subqueryCount++;
        profile.updateNestedSubqueryDepth();
        if (profile.referencesVisibleAlias(subSelect.toString())) {
            profile.correlatedSubqueryCount++;
        }
        profile.subqueryDepth++;
        try {
            if (subSelect.getWithItemsList() != null) {
                for (WithItem withItem : subSelect.getWithItemsList()) {
                    if (withItem.getSubSelect() != null) {
                        processSubSelect(withItem.getSubSelect(), profile);
                    }
                }
            }
            analyzeSelectBody(subSelect.getSelectBody(), profile);
        } finally {
            profile.subqueryDepth--;
        }
    }

    private void collectOrderBySignals(OrderByElement orderByElement, ParsedSqlProfile profile) {
        if (orderByElement == null) {
            return;
        }
        Expression expression = orderByElement.getExpression();
        if (expression instanceof Function) {
            Function function = (Function) expression;
            if ("RAND".equalsIgnoreCase(function.getName()) || "RANDOM".equalsIgnoreCase(function.getName())) {
                profile.randomOrderCount++;
            }
        }
        collectExpressionSignals(expression, null, null, profile);
    }

    private boolean isLeadingWildcardLike(LikeExpression expression) {
        if (!(expression.getRightExpression() instanceof StringValue)) {
            return false;
        }
        String value = ((StringValue) expression.getRightExpression()).getValue();
        return value != null && value.startsWith("%");
    }

    private boolean hasFunctionWrappedOperand(ComparisonOperator expression) {
        return isColumnFunction(expression.getLeftExpression()) || isColumnFunction(expression.getRightExpression());
    }

    private boolean isColumnFunction(Expression expression) {
        if (!(expression instanceof Function)) {
            return false;
        }
        Function function = (Function) expression;
        if (function.getParameters() == null || function.getParameters().getExpressions() == null) {
            return false;
        }
        for (Expression parameter : function.getParameters().getExpressions()) {
            if (parameter instanceof Column) {
                return true;
            }
        }
        return false;
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
        if (expression instanceof OrExpression) {
            OrExpression orExpression = (OrExpression) expression;
            return countPredicates(orExpression.getLeftExpression()) + countPredicates(orExpression.getRightExpression());
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
        profile.recalculateRepeatedExpressions();
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
        if (profile.joinCount >= 2 && profile.joinCriteriaCount <= profile.joinCount) {
            profile.warnings.add("LARGE_JOIN_PAIR_RISK");
        }
        if (profile.repeatedExpressionCount > 0) {
            profile.warnings.add("REPEATED_EXPRESSION_COMPUTE");
        }
        if (!profile.limitPresent && (profile.selectStar || profile.predicateCount == 0)) {
            profile.warnings.add("LARGE_RESULT_SET_RISK");
        }
        if (profile.scalarSubqueryCount > 0) {
            profile.warnings.add("SCALAR_SUBQUERY_IN_SELECT");
        }
        if (profile.subqueryCount >= 3 || profile.nestedSubqueryDepth >= 2) {
            profile.warnings.add("NESTED_SUBQUERY_RISK");
        }
        if (profile.correlatedSubqueryCount > 0) {
            profile.warnings.add("CORRELATED_SUBQUERY_RISK");
        }
        if (profile.functionWrappedPredicateCount > 0) {
            profile.warnings.add("FUNCTION_WRAPPED_PREDICATE");
        }
        if (profile.notExistsCount > 0) {
            profile.warnings.add("NOT_EXISTS_ANTI_JOIN_RISK");
        }
        if (profile.leadingWildcardLikeCount > 0) {
            profile.warnings.add("LEADING_WILDCARD_LIKE_RISK");
        }
        if (profile.orPredicateCount > 0) {
            profile.warnings.add("OR_PREDICATE_INDEX_RISK");
        }
        if (profile.randomOrderCount > 0) {
            profile.warnings.add("ORDER_BY_RANDOM_RISK");
        }
        if (profile.repeatedTableScanCount > 0) {
            profile.warnings.add("REPEATED_TABLE_SCAN_RISK");
        }
        if (profile.subqueryCount + profile.joinCount >= 5 || profile.predicateCount >= 8) {
            profile.warnings.add("COMPLEX_QUERY_GRAPH_RISK");
        }
    }

    private RewriteOutcome applyRewriteRules(Select select) {
        LinkedHashSet<String> appliedRules = new LinkedHashSet<String>();
        if (select == null) {
            return new RewriteOutcome("", new ArrayList<String>(appliedRules));
        }
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
        if (profile == null) {
            return risks;
        }
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
            null,
            null
        );
    }

    private SqlOptimizationExecutionException parserFailure(String message,
                                                            String suggestedAction,
                                                            List<OptimizationTaskRisk> risks,
                                                            Exception cause,
                                                            String sqlText) {
        SqlFailurePosition position = analyzeSqlFailurePosition(cause, sqlText);
        return new SqlOptimizationExecutionException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_PARSER_FAILURE,
            message,
            suggestedAction,
            false,
            OptimizationTaskPhase.DEEP_PARSING,
            risks,
            cause,
            position
        );
    }

    private String normalizeSql(String sqlText) {
        if (sqlText == null) {
            return "";
        }
        String normalized = stripLineComments(sqlText);
        normalized = removeTrailingSemicolons(normalized);
        return normalized;
    }

    private String stripLineComments(String sqlText) {
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        for (int index = 0; index < sqlText.length(); index++) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (inLineComment) {
                if (current == '\n' || current == '\r') {
                    inLineComment = false;
                    builder.append(current);
                } else {
                    builder.append(' ');
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '-' && next == '-') {
                inLineComment = true;
                builder.append(' ');
                builder.append(' ');
                index++;
                continue;
            }
            builder.append(current);
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index++;
                } else {
                    inSingleQuote = !inSingleQuote;
                }
            } else if (current == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }
        }
        return builder.toString();
    }

    private String removeTrailingSemicolons(String sqlText) {
        String normalized = sqlText == null ? "" : sqlText;
        int end = normalized.length();
        while (end > 0) {
            while (end > 0 && Character.isWhitespace(normalized.charAt(end - 1))) {
                end--;
            }
            if (end > 0 && normalized.charAt(end - 1) == ';') {
                normalized = normalized.substring(0, end - 1);
                end = normalized.length();
            } else {
                break;
            }
        }
        return normalized;
    }

    private SqlFailurePosition analyzeSqlFailurePosition(Throwable cause, String sqlText) {
        String message = collectExceptionMessage(cause);
        Integer line = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)"), 1);
        Integer column = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)"), 2);
        if (line == null || column == null) {
            line = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*:\\s*(\\d+)"), 1);
            column = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*:\\s*(\\d+)"), 2);
        }
        String token = firstRegexGroup(message, Pattern.compile("(?i)unexpected token:\\s*\"([^\"]+)\""));
        if (token == null) {
            token = firstRegexGroup(message, Pattern.compile("(?i)mismatched input ['\"]([^'\"]+)['\"]"));
        }
        if (token == null) {
            token = firstRegexGroup(message, Pattern.compile("(?i)extraneous input ['\"]([^'\"]+)['\"]"));
        }
        SqlFailurePosition selectProjectionFailure = guessSelectProjectionFailure(sqlText);
        if (selectProjectionFailure != null) {
            return selectProjectionFailure;
        }
        Integer offset = toOffset(sqlText, line, column);
        Integer tokenOffset = findTokenOffset(sqlText, token, line, offset);
        if (tokenOffset != null && (offset == null || !startsWithToken(sqlText, offset.intValue(), token))) {
            offset = tokenOffset;
            int[] lineColumn = toLineColumn(sqlText, tokenOffset.intValue());
            line = Integer.valueOf(lineColumn[0]);
            column = Integer.valueOf(lineColumn[1]);
        }
        return new SqlFailurePosition(line, column, offset, token, snippet(sqlText, offset));
    }

    private SqlFailurePosition guessSelectProjectionFailure(String sqlText) {
        if (sqlText == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?is)^\\s*SELECT\\s+(FROM|WHERE|GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT)\\b")
            .matcher(sqlText);
        if (!matcher.find()) {
            return null;
        }
        int offset = matcher.start(1);
        String token = matcher.group(1).trim().split("\\s+")[0];
        int[] lineColumn = toLineColumn(sqlText, offset);
        return new SqlFailurePosition(
            Integer.valueOf(lineColumn[0]),
            Integer.valueOf(lineColumn[1]),
            Integer.valueOf(offset),
            token,
            snippet(sqlText, Integer.valueOf(offset))
        );
    }

    private String collectExceptionMessage(Throwable cause) {
        StringBuilder builder = new StringBuilder();
        Throwable current = cause;
        while (current != null) {
            if (current.getMessage() != null) {
                if (builder.length() > 0) {
                    builder.append(" | ");
                }
                builder.append(current.getMessage());
            }
            current = current.getCause();
        }
        return builder.toString();
    }

    private Integer findFirstInteger(String message, Pattern pattern, int group) {
        if (message == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group(group));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String firstRegexGroup(String message, Pattern pattern) {
        if (message == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Integer findTokenOffset(String sqlText, String token, Integer line, Integer anchorOffset) {
        if (sqlText == null || token == null || token.trim().isEmpty() || token.startsWith("<")) {
            return null;
        }
        String upperSql = sqlText.toUpperCase(Locale.ROOT);
        String upperToken = token.toUpperCase(Locale.ROOT);
        int start = 0;
        int end = sqlText.length();
        if (line != null && line.intValue() > 0) {
            int[] bounds = lineBounds(sqlText, line.intValue());
            start = bounds[0];
            end = bounds[1];
        } else if (anchorOffset != null) {
            start = Math.max(0, Math.min(anchorOffset.intValue(), sqlText.length()));
        }
        int found = upperSql.indexOf(upperToken, start);
        if (found >= 0 && found < end) {
            return Integer.valueOf(found);
        }
        found = upperSql.indexOf(upperToken);
        return found >= 0 ? Integer.valueOf(found) : null;
    }

    private int[] lineBounds(String sqlText, int line) {
        int currentLine = 1;
        int start = 0;
        for (int index = 0; index < sqlText.length(); index++) {
            if (currentLine == line) {
                start = index;
                break;
            }
            if (sqlText.charAt(index) == '\n') {
                currentLine++;
                start = index + 1;
            }
        }
        int end = sqlText.length();
        for (int index = start; index < sqlText.length(); index++) {
            if (sqlText.charAt(index) == '\n' || sqlText.charAt(index) == '\r') {
                end = index;
                break;
            }
        }
        return new int[] {start, end};
    }

    private boolean startsWithToken(String sqlText, int offset, String token) {
        if (sqlText == null || token == null || offset < 0 || offset + token.length() > sqlText.length()) {
            return false;
        }
        return sqlText.regionMatches(true, offset, token, 0, token.length());
    }

    private Integer toOffset(String sqlText, Integer line, Integer column) {
        if (sqlText == null || line == null || column == null || line.intValue() <= 0 || column.intValue() <= 0) {
            return null;
        }
        int currentLine = 1;
        int currentColumn = 1;
        for (int index = 0; index < sqlText.length(); index++) {
            if (currentLine == line.intValue() && currentColumn == column.intValue()) {
                return Integer.valueOf(index);
            }
            char current = sqlText.charAt(index);
            if (current == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }
        return currentLine == line.intValue() && currentColumn == column.intValue()
            ? Integer.valueOf(sqlText.length())
            : null;
    }

    private int[] toLineColumn(String sqlText, int offset) {
        int line = 1;
        int column = 1;
        int safeOffset = Math.max(0, Math.min(offset, sqlText == null ? 0 : sqlText.length()));
        for (int index = 0; index < safeOffset; index++) {
            char current = sqlText.charAt(index);
            if (current == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[] {line, column};
    }

    private String snippet(String sqlText, Integer offset) {
        if (sqlText == null || offset == null) {
            return null;
        }
        int safeOffset = Math.max(0, Math.min(offset.intValue(), sqlText.length()));
        int start = Math.max(0, safeOffset - 30);
        int end = Math.min(sqlText.length(), safeOffset + 30);
        return sqlText.substring(start, end).replace('\n', ' ').replace('\r', ' ').trim();
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

    private final class ApacheCalciteProfileCollector {

        private final Set<String> cteNames = new LinkedHashSet<String>();

        private void collect(SqlNode node, ParsedSqlProfile profile, boolean root) {
            if (node == null) {
                return;
            }
            if (node instanceof SqlOrderBy) {
                collectOrderBy((SqlOrderBy) node, profile, root);
                return;
            }
            if (node instanceof SqlWith) {
                collectWith((SqlWith) node, profile, root);
                return;
            }
            if (node instanceof SqlSelect) {
                collectSelect((SqlSelect) node, profile, root);
                return;
            }
            if (node instanceof SqlJoin) {
                collectJoin((SqlJoin) node, profile);
                return;
            }
            if (node instanceof SqlIdentifier) {
                collectIdentifier((SqlIdentifier) node, profile);
                return;
            }
            if (node instanceof SqlBasicCall) {
                collectCall((SqlBasicCall) node, profile);
                return;
            }
            if (node instanceof SqlCall) {
                collectGenericCall((SqlCall) node, profile);
            }
        }

        private void collectOrderBy(SqlOrderBy orderBy, ParsedSqlProfile profile, boolean root) {
            collect(orderBy.query, profile, root);
            collectOrderList(orderBy.orderList, profile);
            if (orderBy.fetch != null) {
                profile.limitPresent = true;
            }
        }

        private void collectWith(SqlWith with, ParsedSqlProfile profile, boolean root) {
            List<String> addedCteNames = new ArrayList<String>();
            try {
                if (with.withList != null) {
                    for (SqlNode itemNode : with.withList.getList()) {
                        if (itemNode instanceof SqlWithItem) {
                            String cteName = normalizeCalciteIdentifier(((SqlWithItem) itemNode).name);
                            if (StringUtils.hasText(cteName) && cteNames.add(cteName.toUpperCase(Locale.ROOT))) {
                                addedCteNames.add(cteName.toUpperCase(Locale.ROOT));
                            }
                        }
                    }
                    for (SqlNode itemNode : with.withList.getList()) {
                        if (itemNode instanceof SqlWithItem) {
                            SqlWithItem item = (SqlWithItem) itemNode;
                            collect(item.query, profile, true);
                        } else {
                            collect(itemNode, profile, false);
                        }
                    }
                }
                collect(with.body, profile, root);
            } finally {
                for (String cteName : addedCteNames) {
                    cteNames.remove(cteName);
                }
            }
        }

        private void collectSelect(SqlSelect select, ParsedSqlProfile profile, boolean root) {
            if (!root) {
                profile.subqueryCount++;
                profile.updateNestedSubqueryDepth();
                profile.subqueryDepth++;
            }
            try {
                collectSelectList(select.getSelectList(), profile);
                collectFrom(select.getFrom(), profile);
                if (select.getWhere() != null) {
                    collectPredicate(select.getWhere(), profile);
                }
                SqlNodeList group = select.getGroup();
                if (group != null) {
                    profile.groupByCount += group.size();
                    for (SqlNode item : group.getList()) {
                        profile.recordExpression(item);
                        collectExpression(item, profile);
                    }
                }
                if (select.getHaving() != null) {
                    collectPredicate(select.getHaving(), profile);
                }
                collectOrderList(select.getOrderList(), profile);
                if (select.getFetch() != null) {
                    profile.limitPresent = true;
                }
                if (select.isDistinct()) {
                    profile.distinctPresent = true;
                }
            } finally {
                if (!root) {
                    profile.subqueryDepth--;
                }
            }
        }

        private void collectSelectList(SqlNodeList selectList, ParsedSqlProfile profile) {
            if (selectList == null) {
                return;
            }
            profile.projectionCount += selectList.size();
            for (SqlNode item : selectList.getList()) {
                if (isStar(item)) {
                    profile.selectStar = true;
                    continue;
                }
                profile.recordExpression(item);
                collectExpression(item, profile);
            }
        }

        private void collectFrom(SqlNode from, ParsedSqlProfile profile) {
            if (from == null) {
                return;
            }
            if (from instanceof SqlIdentifier) {
                String table = normalizeCalciteIdentifier(from);
                if (isCteReference(table)) {
                    return;
                }
                if (!profile.tables.contains(table)) {
                    profile.tables.add(table);
                }
                profile.recordTableScan(table);
                return;
            }
            if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
                if (!operands.isEmpty()) {
                    collectFrom(operands.get(0), profile);
                }
                return;
            }
            collect(from, profile, false);
        }

        private boolean isCteReference(String tableName) {
            if (!StringUtils.hasText(tableName)) {
                return false;
            }
            return cteNames.contains(tableName.toUpperCase(Locale.ROOT));
        }

        private String normalizeCalciteIdentifier(SqlNode node) {
            if (node == null) {
                return "";
            }
            return node.toString()
                .replace("\"", "")
                .replace("`", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        }

        private void collectJoin(SqlJoin join, ParsedSqlProfile profile) {
            profile.joinCount++;
            if (join.getJoinType() != null) {
                profile.joinTypes.add(join.getJoinType().name());
            }
            collectFrom(join.getLeft(), profile);
            collectFrom(join.getRight(), profile);
            if (join.getCondition() != null) {
                int predicateCount = countCalcitePredicates(join.getCondition().toString());
                profile.predicateCount += predicateCount;
                profile.joinCriteriaCount += predicateCount;
                profile.recordExpression(join.getCondition());
                collectExpression(join.getCondition(), profile);
            }
        }

        private void collectIdentifier(SqlIdentifier identifier, ParsedSqlProfile profile) {
            if (identifier != null && !identifier.isStar()) {
                profile.projectedColumns.add(identifier.toString());
            }
        }

        private void collectPredicate(SqlNode predicate, ParsedSqlProfile profile) {
            String predicateSql = predicate.toString();
            profile.predicateCount += countCalcitePredicates(predicateSql);
            profile.datePredicateColumns.addAll(extractCalciteDatePredicateColumns(predicateSql));
            profile.recordExpression(predicateSql);
            collectExpression(predicate, profile);
        }

        private void collectOrderList(SqlNodeList orderList, ParsedSqlProfile profile) {
            if (orderList == null) {
                return;
            }
            profile.orderByCount += orderList.size();
            for (SqlNode item : orderList.getList()) {
                profile.recordExpression(item);
                if (isRandomOrder(item)) {
                    profile.randomOrderCount++;
                }
                collectExpression(item, profile);
            }
        }

        private void collectExpression(SqlNode node, ParsedSqlProfile profile) {
            collect(node, profile, false);
        }

        private void collectCall(SqlBasicCall call, ParsedSqlProfile profile) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.AS) {
                List<SqlNode> operands = call.getOperandList();
                if (!operands.isEmpty()) {
                    collectExpression(operands.get(0), profile);
                }
                return;
            }
            if (kind == SqlKind.OR) {
                profile.orPredicateCount++;
            }
            if (kind == SqlKind.LIKE && isLeadingWildcardCalciteLike(call)) {
                profile.leadingWildcardLikeCount++;
            }
            if (kind == SqlKind.NOT && call.toString().toUpperCase(Locale.ROOT).contains("NOT EXISTS")) {
                profile.notExistsCount++;
            }
            if (isComparisonKind(kind) && hasCalciteFunctionWrappedOperand(call)) {
                profile.functionWrappedPredicateCount++;
            }
            if (kind == SqlKind.OVER) {
                profile.windowFunctionCount++;
            }
            recordCalciteFunction(call, profile);
            collectGenericCall(call, profile);
        }

        private void collectGenericCall(SqlCall call, ParsedSqlProfile profile) {
            if (call.getKind() == SqlKind.UNION || call.getKind() == SqlKind.INTERSECT || call.getKind() == SqlKind.EXCEPT) {
                profile.setOperation = true;
            }
            for (SqlNode operand : call.getOperandList()) {
                collect(operand, profile, false);
            }
        }

        private void recordCalciteFunction(SqlBasicCall call, ParsedSqlProfile profile) {
            String functionName = call.getOperator() == null ? "" : call.getOperator().getName();
            if (!StringUtils.hasText(functionName)) {
                return;
            }
            String upperName = functionName.toUpperCase(Locale.ROOT);
            if (AGGREGATE_FUNCTIONS.contains(upperName)) {
                profile.aggregateFunctions.add(upperName);
            } else if (looksLikeScalarFunction(call) && !BUILT_IN_SCALAR_FUNCTIONS.contains(upperName)) {
                profile.udfFunctions.add(upperName);
            }
        }

        private boolean looksLikeScalarFunction(SqlBasicCall call) {
            SqlKind kind = call.getKind();
            return kind == SqlKind.OTHER_FUNCTION || kind == SqlKind.OTHER || kind == SqlKind.CAST;
        }

        private boolean isStar(SqlNode node) {
            if (node instanceof SqlIdentifier) {
                return ((SqlIdentifier) node).isStar();
            }
            return node != null && "*".equals(node.toString().trim());
        }

        private boolean isLeadingWildcardCalciteLike(SqlBasicCall call) {
            List<SqlNode> operands = call.getOperandList();
            if (operands.size() < 2 || operands.get(1) == null) {
                return false;
            }
            String right = operands.get(1).toString().trim();
            return right.startsWith("'%") || right.startsWith("\"%");
        }

        private boolean hasCalciteFunctionWrappedOperand(SqlBasicCall call) {
            for (SqlNode operand : call.getOperandList()) {
                if (operand instanceof SqlBasicCall && ((SqlBasicCall) operand).getKind() != SqlKind.OTHER) {
                    String operatorName = ((SqlBasicCall) operand).getOperator() == null
                        ? ""
                        : ((SqlBasicCall) operand).getOperator().getName();
                    if (StringUtils.hasText(operatorName) && !isComparisonKind(((SqlBasicCall) operand).getKind())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isComparisonKind(SqlKind kind) {
            return kind == SqlKind.EQUALS
                || kind == SqlKind.NOT_EQUALS
                || kind == SqlKind.LESS_THAN
                || kind == SqlKind.LESS_THAN_OR_EQUAL
                || kind == SqlKind.GREATER_THAN
                || kind == SqlKind.GREATER_THAN_OR_EQUAL;
        }

        private boolean isRandomOrder(SqlNode node) {
            String text = node == null ? "" : node.toString().toUpperCase(Locale.ROOT);
            return text.contains("RAND(") || text.contains("RANDOM(");
        }

        private int countCalcitePredicates(String expressionSql) {
            if (expressionSql == null || expressionSql.trim().isEmpty()) {
                return 0;
            }
            String normalized = expressionSql.toUpperCase(Locale.ROOT);
            int count = 1;
            Matcher matcher = Pattern.compile("\\bAND\\b|\\bOR\\b").matcher(normalized);
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        private List<String> extractCalciteDatePredicateColumns(String expressionSql) {
            if (expressionSql == null) {
                return Collections.emptyList();
            }
            LinkedHashSet<String> columns = new LinkedHashSet<String>();
            Matcher matcher = DATE_PREDICATE_PATTERN.matcher(expressionSql.toUpperCase(Locale.ROOT));
            while (matcher.find()) {
                columns.add(matcher.group(1));
            }
            return new ArrayList<String>(columns);
        }
    }

    private static final class TrinoProfileVisitor extends AstVisitor<Void, ParsedSqlProfile> {

        @Override
        protected Void visitNode(Node node, ParsedSqlProfile profile) {
            for (Node child : node.getChildren()) {
                process(child, profile);
            }
            return null;
        }

        @Override
        protected Void visitQuery(Query node, ParsedSqlProfile profile) {
            if (node.getWith().isPresent()) {
                process(node.getWith().get(), profile);
            }
            process(node.getQueryBody(), profile);
            if (node.getOrderBy().isPresent()) {
                profile.orderByCount += node.getOrderBy().get().getSortItems().size();
                profile.recordExpression(node.getOrderBy().get());
            }
            if (node.getLimit().isPresent()) {
                profile.limitPresent = true;
            }
            return null;
        }

        @Override
        protected Void visitQuerySpecification(QuerySpecification node, ParsedSqlProfile profile) {
            profile.projectionCount += node.getSelect().getSelectItems().size();
            process(node.getSelect(), profile);
            if (node.getFrom().isPresent()) {
                process(node.getFrom().get(), profile);
            }
            if (node.getWhere().isPresent()) {
                String whereSql = node.getWhere().get().toString();
                profile.predicateCount += countTrinoPredicates(whereSql);
                profile.datePredicateColumns.addAll(extractTrinoDatePredicateColumns(whereSql));
                profile.recordExpression(whereSql);
                process(node.getWhere().get(), profile);
            }
            if (node.getGroupBy().isPresent()) {
                profile.groupByCount++;
                profile.recordExpression(node.getGroupBy().get());
                process(node.getGroupBy().get(), profile);
            }
            if (node.getHaving().isPresent()) {
                profile.predicateCount += countTrinoPredicates(node.getHaving().get().toString());
                profile.recordExpression(node.getHaving().get());
                process(node.getHaving().get(), profile);
            }
            if (node.getOrderBy().isPresent()) {
                profile.orderByCount += node.getOrderBy().get().getSortItems().size();
                profile.recordExpression(node.getOrderBy().get());
                process(node.getOrderBy().get(), profile);
            }
            if (node.getLimit().isPresent()) {
                profile.limitPresent = true;
            }
            return null;
        }

        @Override
        protected Void visitTable(Table node, ParsedSqlProfile profile) {
            profile.tables.add(node.getName().toString());
            profile.recordTableScan(node.getName().toString());
            return null;
        }

        @Override
        protected Void visitJoin(io.trino.sql.tree.Join node, ParsedSqlProfile profile) {
            profile.joinCount++;
            profile.joinTypes.add(node.getType().name());
            if (node.getCriteria().isPresent()) {
                JoinCriteria criteria = node.getCriteria().get();
                String criteriaSql = criteria.toString();
                int predicateCount = countTrinoPredicates(criteriaSql);
                profile.predicateCount += predicateCount;
                profile.joinCriteriaCount += predicateCount;
                profile.recordExpression(criteriaSql);
            }
            process(node.getLeft(), profile);
            process(node.getRight(), profile);
            return null;
        }

        @Override
        protected Void visitSingleColumn(SingleColumn node, ParsedSqlProfile profile) {
            profile.recordExpression(node.getExpression());
            process(node.getExpression(), profile);
            return null;
        }

        @Override
        protected Void visitAllColumns(io.trino.sql.tree.AllColumns node, ParsedSqlProfile profile) {
            profile.selectStar = true;
            return null;
        }

        @Override
        protected Void visitFunctionCall(FunctionCall node, ParsedSqlProfile profile) {
            QualifiedName name = node.getName();
            String functionName = name == null ? "" : name.toString().toUpperCase(Locale.ROOT);
            if (AGGREGATE_FUNCTIONS.contains(functionName)) {
                profile.aggregateFunctions.add(functionName);
            } else if (!BUILT_IN_SCALAR_FUNCTIONS.contains(functionName)) {
                profile.udfFunctions.add(functionName);
            }
            if (node.getWindow().isPresent()) {
                profile.windowFunctionCount++;
            }
            for (io.trino.sql.tree.Expression argument : node.getArguments()) {
                process(argument, profile);
            }
            return null;
        }

        @Override
        protected Void visitDereferenceExpression(DereferenceExpression node, ParsedSqlProfile profile) {
            profile.projectedColumns.add(node.toString());
            return null;
        }

        @Override
        protected Void visitSubqueryExpression(io.trino.sql.tree.SubqueryExpression node, ParsedSqlProfile profile) {
            profile.subqueryCount++;
            return visitNode(node, profile);
        }

        private static int countTrinoPredicates(String expressionSql) {
            if (expressionSql == null || expressionSql.trim().isEmpty()) {
                return 0;
            }
            String normalized = expressionSql.toUpperCase(Locale.ROOT);
            int count = 1;
            Matcher matcher = Pattern.compile("\\bAND\\b|\\bOR\\b").matcher(normalized);
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        private static List<String> extractTrinoDatePredicateColumns(String expressionSql) {
            if (expressionSql == null) {
                return Collections.emptyList();
            }
            LinkedHashSet<String> columns = new LinkedHashSet<String>();
            Matcher matcher = DATE_PREDICATE_PATTERN.matcher(expressionSql.toUpperCase(Locale.ROOT));
            while (matcher.find()) {
                columns.add(matcher.group(1));
            }
            return new ArrayList<String>(columns);
        }
    }

    public static final class ParsedSqlProfile {

        private final String normalizedSql;
        private final List<String> tables = new ArrayList<String>();
        private final Set<String> projectedColumns = new LinkedHashSet<String>();
        private final Set<String> aggregateFunctions = new LinkedHashSet<String>();
        private final Set<String> udfFunctions = new LinkedHashSet<String>();
        private final Set<String> datePredicateColumns = new LinkedHashSet<String>();
        private final Set<String> joinTypes = new LinkedHashSet<String>();
        private final List<String> warnings = new ArrayList<String>();
        private final LinkedHashMap<String, Integer> expressionFrequency = new LinkedHashMap<String, Integer>();
        private final LinkedHashMap<String, Integer> tableScanFrequency = new LinkedHashMap<String, Integer>();
        private final Deque<Set<String>> aliasScopes = new ArrayDeque<Set<String>>();
        private String parserEngine = "JSQLPARSER";
        private int projectionCount;
        private int predicateCount;
        private int joinCount;
        private int joinCriteriaCount;
        private int groupByCount;
        private int orderByCount;
        private int windowFunctionCount;
        private int subqueryCount;
        private int scalarSubqueryCount;
        private int nestedSubqueryDepth;
        private int correlatedSubqueryCount;
        private int orPredicateCount;
        private int functionWrappedPredicateCount;
        private int leadingWildcardLikeCount;
        private int randomOrderCount;
        private int notExistsCount;
        private int repeatedTableScanCount;
        private int subqueryDepth;
        private int repeatedExpressionCount;
        private boolean selectStar;
        private boolean limitPresent;
        private boolean distinctPresent;
        private boolean setOperation;
        private RewriteOutcome rewriteOutcome = RewriteOutcome.empty();

        private ParsedSqlProfile(String normalizedSql) {
            this.normalizedSql = normalizedSql;
        }

        private Map<String, Object> toAstProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("statementType", "SELECT");
            payload.put("parserEngine", parserEngine);
            payload.put("tables", tables);
            payload.put("projectionCount", Integer.valueOf(projectionCount));
            payload.put("projectedColumns", new ArrayList<String>(projectedColumns));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("joinTypes", new ArrayList<String>(joinTypes));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("orderByCount", Integer.valueOf(orderByCount));
            payload.put("windowFunctionCount", Integer.valueOf(windowFunctionCount));
            payload.put("udfFunctions", new ArrayList<String>(udfFunctions));
            payload.put("subqueryCount", Integer.valueOf(subqueryCount));
            payload.put("scalarSubqueryCount", Integer.valueOf(scalarSubqueryCount));
            payload.put("nestedSubqueryDepth", Integer.valueOf(nestedSubqueryDepth));
            payload.put("correlatedSubqueryCount", Integer.valueOf(correlatedSubqueryCount));
            payload.put("orPredicateCount", Integer.valueOf(orPredicateCount));
            payload.put("functionWrappedPredicateCount", Integer.valueOf(functionWrappedPredicateCount));
            payload.put("leadingWildcardLikeCount", Integer.valueOf(leadingWildcardLikeCount));
            payload.put("randomOrderCount", Integer.valueOf(randomOrderCount));
            payload.put("notExistsCount", Integer.valueOf(notExistsCount));
            payload.put("repeatedTableScanCount", Integer.valueOf(repeatedTableScanCount));
            payload.put("repeatedExpressionCount", Integer.valueOf(repeatedExpressionCount));
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
            payload.put("parserEngine", parserEngine);
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("windowFunctionCount", Integer.valueOf(windowFunctionCount));
            payload.put("udfFunctions", new ArrayList<String>(udfFunctions));
            payload.put("subqueryCount", Integer.valueOf(subqueryCount));
            payload.put("correlatedSubqueryCount", Integer.valueOf(correlatedSubqueryCount));
            payload.put("orPredicateCount", Integer.valueOf(orPredicateCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("warnings", warnings);
            return payload;
        }

        public List<String> getTables() {
            return new ArrayList<String>(tables);
        }

        public String getParserEngine() {
            return parserEngine;
        }

        public String getNormalizedSql() {
            return normalizedSql;
        }

        private RewriteOutcome getRewriteOutcome() {
            return rewriteOutcome == null ? RewriteOutcome.empty() : rewriteOutcome;
        }

        public Set<String> getAggregateFunctions() {
            return new LinkedHashSet<String>(aggregateFunctions);
        }

        public Set<String> getDatePredicateColumns() {
            return new LinkedHashSet<String>(datePredicateColumns);
        }

        public List<String> getWarnings() {
            return new ArrayList<String>(warnings);
        }

        public int getProjectionCount() {
            return projectionCount;
        }

        public int getPredicateCount() {
            return predicateCount;
        }

        public int getJoinCount() {
            return joinCount;
        }

        public int getJoinCriteriaCount() {
            return joinCriteriaCount;
        }

        public int getGroupByCount() {
            return groupByCount;
        }

        public int getOrderByCount() {
            return orderByCount;
        }

        public int getWindowFunctionCount() {
            return windowFunctionCount;
        }

        public int getUdfFunctionCount() {
            return udfFunctions.size();
        }

        public int getSubqueryCount() {
            return subqueryCount;
        }

        public int getScalarSubqueryCount() {
            return scalarSubqueryCount;
        }

        public int getNestedSubqueryDepth() {
            return nestedSubqueryDepth;
        }

        public int getCorrelatedSubqueryCount() {
            return correlatedSubqueryCount;
        }

        public int getOrPredicateCount() {
            return orPredicateCount;
        }

        public int getFunctionWrappedPredicateCount() {
            return functionWrappedPredicateCount;
        }

        public int getLeadingWildcardLikeCount() {
            return leadingWildcardLikeCount;
        }

        public int getRandomOrderCount() {
            return randomOrderCount;
        }

        public int getNotExistsCount() {
            return notExistsCount;
        }

        public int getRepeatedTableScanCount() {
            return repeatedTableScanCount;
        }

        public int getComplexGraphScore() {
            return subqueryCount + joinCount + orPredicateCount + functionWrappedPredicateCount + randomOrderCount;
        }

        public int getRepeatedExpressionCount() {
            return repeatedExpressionCount;
        }

        public boolean isSelectStar() {
            return selectStar;
        }

        public boolean isLimitPresent() {
            return limitPresent;
        }

        public boolean isSetOperation() {
            return setOperation;
        }

        public List<String> getJoinTypes() {
            return new ArrayList<String>(joinTypes);
        }

        private void recordExpression(Object expression) {
            if (expression == null) {
                return;
            }
            if (expression instanceof OrderByElement) {
                recordExpression(((OrderByElement) expression).getExpression());
                return;
            }
            if (expression instanceof Parenthesis) {
                recordExpression(((Parenthesis) expression).getExpression());
                return;
            }
            String key = expression.toString().trim().toUpperCase(Locale.ROOT);
            if (key.length() < 4 || isSimpleExpressionReference(expression, key)) {
                return;
            }
            Integer current = expressionFrequency.get(key);
            expressionFrequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        private boolean isSimpleExpressionReference(Object expression, String key) {
            if (expression instanceof Column
                || expression instanceof SqlIdentifier
                || expression instanceof DereferenceExpression
                || expression instanceof LongValue
                || expression instanceof DoubleValue
                || expression instanceof StringValue
                || expression instanceof DateValue
                || expression instanceof TimestampValue
                || expression instanceof NullValue) {
                return true;
            }
            return key.matches("[A-Z_][A-Z0-9_]*(\\.[A-Z_][A-Z0-9_]*)*")
                || key.matches("\"[^\"]+\"(\\.\"[^\"]+\")*")
                || key.matches("`[^`]+`(\\.`[^`]+`)*");
        }

        private void recalculateRepeatedExpressions() {
            int repeated = 0;
            for (Integer count : expressionFrequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeated += count.intValue() - 1;
                }
            }
            repeatedExpressionCount = repeated;
            repeatedTableScanCount = 0;
            for (Integer count : tableScanFrequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeatedTableScanCount += count.intValue() - 1;
                }
            }
        }

        private void recordTableScan(String table) {
            if (table == null) {
                return;
            }
            String key = table.trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty()) {
                return;
            }
            Integer current = tableScanFrequency.get(key);
            tableScanFrequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        private void pushAliasScope(Set<String> aliases) {
            aliasScopes.push(aliases == null ? Collections.<String>emptySet() : aliases);
        }

        private void popAliasScope() {
            if (!aliasScopes.isEmpty()) {
                aliasScopes.pop();
            }
        }

        private boolean referencesVisibleAlias(String sql) {
            if (sql == null || aliasScopes.isEmpty()) {
                return false;
            }
            String normalized = sql.toUpperCase(Locale.ROOT);
            for (Set<String> scope : aliasScopes) {
                for (String alias : scope) {
                    if (alias != null && !alias.isEmpty() && normalized.contains(alias + ".")) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void updateNestedSubqueryDepth() {
            nestedSubqueryDepth = Math.max(nestedSubqueryDepth, subqueryDepth + 1);
        }
    }

    private static final class RewriteOutcome {

        private final String rewrittenSql;
        private final List<String> appliedRules;

        private RewriteOutcome(String rewrittenSql, List<String> appliedRules) {
            this.rewrittenSql = rewrittenSql;
            this.appliedRules = appliedRules;
        }

        private static RewriteOutcome empty() {
            return new RewriteOutcome("", Collections.<String>emptyList());
        }
    }

    public static final class SqlFailurePosition {

        private final Integer line;
        private final Integer column;
        private final Integer offset;
        private final String token;
        private final String snippet;

        private SqlFailurePosition(Integer line, Integer column, Integer offset, String token, String snippet) {
            this.line = line;
            this.column = column;
            this.offset = offset;
            this.token = token;
            this.snippet = snippet;
        }

        public Integer getLine() {
            return line;
        }

        public Integer getColumn() {
            return column;
        }

        public Integer getOffset() {
            return offset;
        }

        public String getToken() {
            return token;
        }

        public String getSnippet() {
            return snippet;
        }
    }

    public static final class SqlOptimizationExecutionException extends RuntimeException {

        private final int code;
        private final String suggestedAction;
        private final boolean retryable;
        private final OptimizationTaskPhase failedPhase;
        private final List<OptimizationTaskRisk> risks;
        private final SqlFailurePosition failurePosition;

        private SqlOptimizationExecutionException(int code,
                                                  String message,
                                                  String suggestedAction,
                                                  boolean retryable,
                                                  OptimizationTaskPhase failedPhase,
                                                  List<OptimizationTaskRisk> risks,
                                                  Throwable cause,
                                                  SqlFailurePosition failurePosition) {
            super(message, cause);
            this.code = code;
            this.suggestedAction = suggestedAction;
            this.retryable = retryable;
            this.failedPhase = failedPhase;
            this.risks = risks == null ? Collections.<OptimizationTaskRisk>emptyList() : risks;
            this.failurePosition = failurePosition;
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

        public SqlFailurePosition getFailurePosition() {
            return failurePosition;
        }
    }
}
