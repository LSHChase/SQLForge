package com.company.sqloptimization.domain.rewrite.qbdag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;

public class QueryBlockDagBuilder {

    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'(?:''|[^'])*'");
    private static final Pattern DATE_LITERAL_PATTERN =
        Pattern.compile("(?i)\\b(DATE|TIME|TIMESTAMP)\\s*<STRING>|\\b(DATE|TIME|TIMESTAMP)\\s*'(?:''|[^'])*'");
    private static final Pattern NUMBER_LITERAL_PATTERN =
        Pattern.compile("(?<![A-Z0-9_$.])-?\\b\\d+(?:\\.\\d+)?\\b(?![A-Z0-9_$])", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROM_JOIN_ALIAS_PATTERN = Pattern.compile(
        "(?i)\\b(FROM|JOIN)\\s+([A-Z0-9_.$`\"]+)(?:\\s+(?:AS\\s+)?([A-Z_][A-Z0-9_$]*))?"
    );

    public QueryBlockDag build(String normalizedSql, Map<String, Object> advancedStructureProfile) {
        try {
            return buildFromSql(normalizedSql);
        } catch (RuntimeException ex) {
            return buildFromAdvancedProfile(normalizedSql, advancedStructureProfile, ex.getMessage());
        }
    }

    public QueryBlockDag buildFromSql(String normalizedSql) {
        try {
            SqlParser.Config parserConfig = SqlParser.config()
                .withConformance(SqlConformanceEnum.LENIENT)
                .withUnquotedCasing(Casing.UNCHANGED);
            SqlNode statement = SqlParser.create(normalizedSql, parserConfig).parseStmt();
            return build(statement, normalizedSql);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Calcite 查询块解析失败: " + ex.getMessage(), ex);
        }
    }

    public QueryBlockDag build(SqlNode ast, String normalizedSql) {
        if (ast == null) {
            return QueryBlockDag.unavailable(normalizedSql, "APACHE_CALCITE", "Calcite AST 为空。");
        }
        BuildState state = new BuildState();
        String rootBlockId = collectRoot(ast, state);
        if (rootBlockId == null || rootBlockId.isEmpty()) {
            return QueryBlockDag.unavailable(normalizedSql, "APACHE_CALCITE", "未识别到 SqlSelect 查询块。");
        }
        resolveReferenceEdges(state);
        markStructuralEquivalence(state);
        List<String> topologicalOrder = topologicalSort(state.blocks, state.edges, state.issues);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("decompositionStatus", "AVAILABLE");
        attributes.put("decompositionSource", "CALCITE_SQL_NODE");
        attributes.put("subqueryBoundaryRule", "DEPTH_FIRST_SQL_SELECT_SCAN");
        attributes.put("structuralHashRule", "SHA256_NORMALIZED_RELATIONAL_FORM");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("blockCount", Integer.valueOf(state.blocks.size()));
        attributes.put("duplicateStructuralGroupCount", Integer.valueOf(duplicateGroupCount(state.hashGroups)));
        attributes.put("lateralJoinPromotionCount", Integer.valueOf(lateralPromotionCount(state.edges)));
        return new QueryBlockDag(
            QueryBlockDag.SCHEMA_VERSION,
            "APACHE_CALCITE",
            rootBlockId,
            state.blocks,
            state.edges,
            state.hashGroups,
            state.issues,
            topologicalOrder,
            attributes
        );
    }

    private QueryBlockDag buildFromAdvancedProfile(String normalizedSql,
                                                   Map<String, Object> advancedStructureProfile,
                                                   String fallbackReason) {
        BuildState state = new BuildState();
        Map<String, Object> advancedProfile = advancedStructureProfile == null
            ? Collections.<String, Object>emptyMap()
            : new LinkedHashMap<String, Object>(advancedStructureProfile);
        List<Map<String, Object>> tables = QbDagCollections.mapList(advancedProfile.get("tables"));
        List<Map<String, Object>> projections = QbDagCollections.mapList(advancedProfile.get("projections"));
        List<Map<String, Object>> predicates = QbDagCollections.mapList(advancedProfile.get("predicates"));
        List<Map<String, Object>> aggregations = QbDagCollections.mapList(advancedProfile.get("aggregations"));
        List<Map<String, Object>> groupBy = QbDagCollections.mapList(advancedProfile.get("groupBy"));
        List<Map<String, Object>> ctes = QbDagCollections.mapList(advancedProfile.get("ctes"));
        List<Map<String, Object>> subqueries = QbDagCollections.mapList(advancedProfile.get("subqueries"));

        String rootBlockId = nextBlockId(state);
        List<String> rootSelectList = expressionList(projections, "expression");
        List<String> rootPredicates = expressionList(predicates, "expression");
        List<String> rootGroupBy = expressionList(groupBy, "expression");
        String rootForm = textRelationalForm(rootSelectList, tableList(tables), rootPredicates, rootGroupBy, expressionList(aggregations, "expression"));
        state.blocks.add(new QueryBlockNode(
            rootBlockId,
            "ROOT",
            "",
            "ROOT",
            "",
            rootSelectList,
            String.join(", ", tableList(tables)),
            String.join(" AND ", rootPredicates),
            rootGroupBy,
            "",
            outputListFromProjectionMaps(projections),
            aliasListFromTables(tables),
            Collections.<String>emptyList(),
            sha256(rootForm),
            rootForm,
            rootBlockId,
            false,
            attributes("queryBlockMode", "ADVANCED_PROFILE_FALLBACK")
        ));

        for (Map<String, Object> cte : ctes) {
            String blockId = addProfileBlock(state, rootBlockId, "CTE", QbDagCollections.text(cte.get("name")),
                QbDagCollections.text(cte.get("name")), QbDagCollections.text(cte.get("query")), false);
            state.edges.add(edge(rootBlockId, blockId, QueryBlockEdge.OUTPUT_REFERENCE,
                Collections.singletonList(QbDagCollections.text(cte.get("name"))), false, ""));
        }
        for (Map<String, Object> subquery : subqueries) {
            boolean correlated = Boolean.TRUE.equals(subquery.get("correlated"));
            String blockId = addProfileBlock(state, rootBlockId, "SUBQUERY", QbDagCollections.text(subquery.get("subqueryId")),
                QbDagCollections.text(subquery.get("alias")), QbDagCollections.text(subquery.get("query")), correlated);
            state.edges.add(edge(rootBlockId, blockId, QueryBlockEdge.CONTAINS,
                Collections.singletonList(QbDagCollections.text(subquery.get("location"))), false, ""));
            if (correlated) {
                state.edges.add(edge(blockId, rootBlockId, QueryBlockEdge.LATERAL_JOIN_PROMOTION,
                    Collections.singletonList("ADVANCED_PROFILE_CORRELATED_SUBQUERY"), true, "PROMOTE_CORRELATED_SUBQUERY_TO_LATERAL_JOIN"));
                state.issues.add(new QueryBlockDagIssue(
                    "QBDAG_CORRELATED_SUBQUERY_CYCLE_BROKEN",
                    "MEDIUM",
                    "查询块 " + blockId + " 带有关联引用，已标记为 lateral join 提升候选以打破环。",
                    "后续改写阶段需要把该子查询提升为 LATERAL JOIN 或等价半连接。",
                    Arrays.asList(blockId, rootBlockId)
                ));
            }
        }
        markStructuralEquivalence(state);
        List<String> topologicalOrder = topologicalSort(state.blocks, state.edges, state.issues);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("decompositionStatus", "PARTIAL");
        attributes.put("decompositionSource", "ADVANCED_STRUCTURE_PROFILE_FALLBACK");
        attributes.put("calciteFallbackReason", fallbackReason == null ? "" : fallbackReason);
        attributes.put("structuralHashRule", "SHA256_NORMALIZED_RELATIONAL_FORM");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("pageImpact", "NO_FRONTEND_PAGE_CHANGE");
        attributes.put("blockCount", Integer.valueOf(state.blocks.size()));
        attributes.put("duplicateStructuralGroupCount", Integer.valueOf(duplicateGroupCount(state.hashGroups)));
        attributes.put("lateralJoinPromotionCount", Integer.valueOf(lateralPromotionCount(state.edges)));
        return new QueryBlockDag(
            QueryBlockDag.SCHEMA_VERSION,
            QbDagCollections.text(advancedProfile.get("parserEngine")),
            rootBlockId,
            state.blocks,
            state.edges,
            state.hashGroups,
            state.issues,
            topologicalOrder,
            attributes
        );
    }

    private String collectRoot(SqlNode node, BuildState state) {
        if (node instanceof SqlOrderBy) {
            return collectRoot(((SqlOrderBy) node).query, state);
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            String bodyBlockId = collectRoot(with.body, state);
            if (with.withList != null) {
                for (SqlNode itemNode : with.withList.getList()) {
                    if (itemNode instanceof SqlWithItem) {
                        SqlWithItem item = (SqlWithItem) itemNode;
                        String name = cleanIdentifier(item.name);
                        String cteBlockId = collectSelectLike(item.query, bodyBlockId, "CTE", name, name, state);
                        if (cteBlockId != null && !cteBlockId.isEmpty()) {
                            state.cteBlockIds.put(name.toUpperCase(Locale.ROOT), cteBlockId);
                            state.edges.add(edge(bodyBlockId, cteBlockId, QueryBlockEdge.OUTPUT_REFERENCE,
                                Collections.singletonList(name), false, ""));
                        }
                    }
                }
            }
            return bodyBlockId;
        }
        return collectSelectLike(node, "", "ROOT", "ROOT", "", state);
    }

    private String collectSelectLike(SqlNode node,
                                     String parentBlockId,
                                     String role,
                                     String name,
                                     String alias,
                                     BuildState state) {
        if (node == null) {
            return "";
        }
        if (node instanceof SqlOrderBy) {
            return collectSelectLike(((SqlOrderBy) node).query, parentBlockId, role, name, alias, state);
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            String bodyBlockId = collectSelectLike(with.body, parentBlockId, role, name, alias, state);
            if (with.withList != null) {
                for (SqlNode itemNode : with.withList.getList()) {
                    if (itemNode instanceof SqlWithItem) {
                        SqlWithItem item = (SqlWithItem) itemNode;
                        String cteName = cleanIdentifier(item.name);
                        String cteBlockId = collectSelectLike(item.query, bodyBlockId, "CTE", cteName, cteName, state);
                        if (cteBlockId != null && !cteBlockId.isEmpty()) {
                            state.cteBlockIds.put(cteName.toUpperCase(Locale.ROOT), cteBlockId);
                            state.edges.add(edge(bodyBlockId, cteBlockId, QueryBlockEdge.OUTPUT_REFERENCE,
                                Collections.singletonList(cteName), false, ""));
                        }
                    }
                }
            }
            return bodyBlockId;
        }
        if (!(node instanceof SqlSelect)) {
            collectNestedSelects(node, parentBlockId, state);
            return "";
        }
        SqlSelect select = (SqlSelect) node;
        if (state.seenSelects.containsKey(select)) {
            return state.seenSelects.get(select);
        }

        String blockId = nextBlockId(state);
        state.seenSelects.put(select, blockId);
        QueryBlockScope scope = QueryBlockScope.from(select.getFrom());
        List<String> selectList = selectList(select.getSelectList());
        List<String> groupBy = nodeList(select.getGroup());
        List<String> outputs = outputColumns(select.getSelectList());
        List<String> externalReferences = externalReferences(select, scope);
        String normalizedForm = normalizedRelationalForm(select, scope);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("queryBlockMode", "CALCITE_AST");
        attributes.put("calciteNodeKind", select.getKind() == null ? "SELECT" : select.getKind().name());
        attributes.put("hasWhere", Boolean.valueOf(select.getWhere() != null));
        attributes.put("hasHaving", Boolean.valueOf(select.getHaving() != null));
        attributes.put("hasGroupBy", Boolean.valueOf(select.getGroup() != null && select.getGroup().size() > 0));
        QueryBlockNode block = new QueryBlockNode(
            blockId,
            role,
            parentBlockId == null ? "" : parentBlockId,
            name == null ? "" : name,
            alias == null ? "" : alias,
            selectList,
            normalizeText(select.getFrom()),
            normalizeText(select.getWhere()),
            groupBy,
            normalizeText(select.getHaving()),
            outputs,
            scope.aliases(),
            externalReferences,
            sha256(normalizedForm),
            normalizedForm,
            blockId,
            false,
            attributes
        );
        state.blocks.add(block);
        if (parentBlockId != null && !parentBlockId.isEmpty()) {
            state.edges.add(edge(parentBlockId, blockId, QueryBlockEdge.CONTAINS,
                Collections.singletonList(role), false, ""));
        }

        collectNestedSelects(select.getSelectList(), blockId, state);
        collectNestedSelects(select.getFrom(), blockId, state);
        collectNestedSelects(select.getWhere(), blockId, state);
        collectNestedSelects(select.getGroup(), blockId, state);
        collectNestedSelects(select.getHaving(), blockId, state);
        collectNestedSelects(select.getOrderList(), blockId, state);
        return blockId;
    }

    private void collectNestedSelects(SqlNode node, String parentBlockId, BuildState state) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlSelect) {
            collectSelectLike(node, parentBlockId, "SUBQUERY", "", "", state);
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectNestedSelects(((SqlOrderBy) node).query, parentBlockId, state);
            return;
        }
        if (node instanceof SqlWith) {
            collectSelectLike(node, parentBlockId, "SUBQUERY", "", "", state);
            return;
        }
        if (node instanceof SqlWithItem) {
            SqlWithItem item = (SqlWithItem) node;
            collectSelectLike(item.query, parentBlockId, "CTE", cleanIdentifier(item.name), cleanIdentifier(item.name), state);
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectNestedSelects(item, parentBlockId, state);
            }
            return;
        }
        if (node instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) node;
            collectNestedSelects(join.getLeft(), parentBlockId, state);
            collectNestedSelects(join.getRight(), parentBlockId, state);
            collectNestedSelects(join.getCondition(), parentBlockId, state);
            return;
        }
        if (node instanceof SqlBasicCall) {
            SqlBasicCall call = (SqlBasicCall) node;
            if (call.getKind() == SqlKind.AS && call.getOperandList().size() >= 2) {
                SqlNode relation = call.getOperandList().get(0);
                String alias = cleanIdentifier(call.getOperandList().get(1));
                if (relation instanceof SqlSelect || relation instanceof SqlOrderBy || relation instanceof SqlWith) {
                    collectSelectLike(relation, parentBlockId, "DERIVED_TABLE", alias, alias, state);
                    return;
                }
            }
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectNestedSelects(operand, parentBlockId, state);
            }
        }
    }

    private void resolveReferenceEdges(BuildState state) {
        Map<String, QueryBlockNode> byId = blocksById(state.blocks);
        for (QueryBlockNode block : new ArrayList<QueryBlockNode>(state.blocks)) {
            for (String externalReference : block.getExternalReferences()) {
                String qualifier = qualifier(externalReference);
                QueryBlockNode ancestor = findAncestorWithAlias(block, qualifier, byId);
                if (ancestor == null) {
                    continue;
                }
                boolean createsCycle = isAncestor(block, ancestor.getBlockId(), byId);
                String edgeType = createsCycle
                    ? QueryBlockEdge.LATERAL_JOIN_PROMOTION
                    : QueryBlockEdge.EXTERNAL_REFERENCE;
                state.edges.add(edge(
                    block.getBlockId(),
                    ancestor.getBlockId(),
                    edgeType,
                    Collections.singletonList(externalReference),
                    createsCycle,
                    createsCycle ? "PROMOTE_CORRELATED_SUBQUERY_TO_LATERAL_JOIN" : ""
                ));
                if (createsCycle) {
                    state.issues.add(new QueryBlockDagIssue(
                        "QBDAG_CORRELATED_SUBQUERY_CYCLE_BROKEN",
                        "MEDIUM",
                        "查询块 " + block.getBlockId() + " 引用了外层别名 " + qualifier + "，已打破 parent-child 环。",
                        "后续改写阶段需要把该子查询提升为 LATERAL JOIN 或等价半连接。",
                        Arrays.asList(block.getBlockId(), ancestor.getBlockId())
                    ));
                }
            }
            resolveCteReferences(state, block);
        }
    }

    private void resolveCteReferences(BuildState state, QueryBlockNode block) {
        String haystack = (block.getFromClause() + " " + block.getWhereClause() + " " + block.getSelectList())
            .toUpperCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : state.cteBlockIds.entrySet()) {
            if (block.getBlockId().equals(entry.getValue())) {
                continue;
            }
            if (haystack.contains(entry.getKey())) {
                state.edges.add(edge(block.getBlockId(), entry.getValue(), QueryBlockEdge.OUTPUT_REFERENCE,
                    Collections.singletonList(entry.getKey()), false, ""));
            }
        }
    }

    private void markStructuralEquivalence(BuildState state) {
        LinkedHashMap<String, List<QueryBlockNode>> byHash = new LinkedHashMap<String, List<QueryBlockNode>>();
        for (QueryBlockNode block : state.blocks) {
            List<QueryBlockNode> group = byHash.get(block.getStructuralHash());
            if (group == null) {
                group = new ArrayList<QueryBlockNode>();
                byHash.put(block.getStructuralHash(), group);
            }
            group.add(block);
        }

        List<QueryBlockNode> updatedBlocks = new ArrayList<QueryBlockNode>();
        for (Map.Entry<String, List<QueryBlockNode>> entry : byHash.entrySet()) {
            List<QueryBlockNode> group = entry.getValue();
            String representative = group.get(0).getBlockId();
            List<String> blockIds = new ArrayList<String>();
            LinkedHashSet<String> forms = new LinkedHashSet<String>();
            for (QueryBlockNode block : group) {
                blockIds.add(block.getBlockId());
                forms.add(block.getNormalizedRelationalForm());
            }
            String collisionStatus = forms.size() == 1
                ? "VERIFIED_BY_CANONICAL_FORM"
                : "SEMANTIC_EQUIVALENCE_REQUIRED_2_3_2";
            state.hashGroups.add(new QueryBlockHashGroup(
                entry.getKey(),
                representative,
                blockIds,
                collisionStatus,
                group.get(0).getNormalizedRelationalForm()
            ));
            for (QueryBlockNode block : group) {
                updatedBlocks.add(block.withRepresentative(representative, !block.getBlockId().equals(representative)));
            }
            if (forms.size() > 1) {
                state.issues.add(new QueryBlockDagIssue(
                    "QBDAG_STRUCTURAL_HASH_COLLISION_REQUIRES_SEMANTIC_CHECK",
                    "HIGH",
                    "结构哈希 " + entry.getKey() + " 命中多个不同规范形。",
                    "进入 2.3.2 语义等价验证，不允许直接合并查询块。",
                    blockIds
                ));
            }
        }
        Collections.sort(updatedBlocks, new Comparator<QueryBlockNode>() {
            @Override
            public int compare(QueryBlockNode left, QueryBlockNode right) {
                return numericBlockId(left.getBlockId()) - numericBlockId(right.getBlockId());
            }
        });
        state.blocks.clear();
        state.blocks.addAll(updatedBlocks);
    }

    private List<String> topologicalSort(List<QueryBlockNode> blocks,
                                         List<QueryBlockEdge> edges,
                                         List<QueryBlockDagIssue> issues) {
        LinkedHashMap<String, Integer> indegree = new LinkedHashMap<String, Integer>();
        LinkedHashMap<String, List<String>> outgoing = new LinkedHashMap<String, List<String>>();
        for (QueryBlockNode block : blocks) {
            indegree.put(block.getBlockId(), Integer.valueOf(0));
            outgoing.put(block.getBlockId(), new ArrayList<String>());
        }
        for (QueryBlockEdge edge : edges) {
            if (edge.isBreaksCycle()) {
                continue;
            }
            if (!indegree.containsKey(edge.getFromBlockId()) || !indegree.containsKey(edge.getToBlockId())) {
                continue;
            }
            outgoing.get(edge.getFromBlockId()).add(edge.getToBlockId());
            indegree.put(edge.getToBlockId(), Integer.valueOf(indegree.get(edge.getToBlockId()).intValue() + 1));
        }
        Deque<String> ready = new ArrayDeque<String>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue().intValue() == 0) {
                ready.add(entry.getKey());
            }
        }
        List<String> order = new ArrayList<String>();
        while (!ready.isEmpty()) {
            String blockId = ready.removeFirst();
            order.add(blockId);
            for (String next : outgoing.get(blockId)) {
                int count = indegree.get(next).intValue() - 1;
                indegree.put(next, Integer.valueOf(count));
                if (count == 0) {
                    ready.add(next);
                }
            }
        }
        if (order.size() != blocks.size()) {
            List<String> unresolved = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
                if (entry.getValue().intValue() > 0) {
                    unresolved.add(entry.getKey());
                }
            }
            issues.add(new QueryBlockDagIssue(
                "QBDAG_UNRESOLVED_CYCLE",
                "HIGH",
                "查询块引用图仍存在未打破的环。",
                "需要进入人工规则设计，明确是否可 lateral join、semi join 或 CTE 化。",
                unresolved
            ));
            for (QueryBlockNode block : blocks) {
                if (!order.contains(block.getBlockId())) {
                    order.add(block.getBlockId());
                }
            }
        }
        return order;
    }

    private String addProfileBlock(BuildState state,
                                   String parentBlockId,
                                   String role,
                                   String name,
                                   String alias,
                                   String query,
                                   boolean correlated) {
        String blockId = nextBlockId(state);
        String form = textStructuralForm(query);
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("queryBlockMode", "ADVANCED_PROFILE_FALLBACK");
        attributes.put("correlated", Boolean.valueOf(correlated));
        state.blocks.add(new QueryBlockNode(
            blockId,
            role,
            parentBlockId,
            name,
            alias,
            Collections.singletonList(query),
            "",
            "",
            Collections.<String>emptyList(),
            "",
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            sha256(form),
            form,
            blockId,
            false,
            attributes
        ));
        return blockId;
    }

    private String normalizedRelationalForm(SqlSelect select, QueryBlockScope scope) {
        List<String> projections = new ArrayList<String>();
        SqlNodeList selectList = select.getSelectList();
        if (selectList != null) {
            for (SqlNode item : selectList.getList()) {
                projections.add(normalizeExpression(stripAlias(item), scope));
            }
        }
        Collections.sort(projections);

        List<String> groupBy = new ArrayList<String>();
        SqlNodeList group = select.getGroup();
        if (group != null) {
            for (SqlNode item : group.getList()) {
                groupBy.add(normalizeExpression(item, scope));
            }
        }
        Collections.sort(groupBy);

        List<String> aggregations = new ArrayList<String>();
        for (String projection : projections) {
            if (projection.matches("(?i).*(COUNT|SUM|AVG|MIN|MAX|APPROX_DISTINCT|GROUP_CONCAT|STRING_AGG|LISTAGG).*")) {
                aggregations.add(projection);
            }
        }
        Collections.sort(aggregations);

        StringBuilder builder = new StringBuilder();
        builder.append("FROM[").append(normalizeFrom(select.getFrom(), scope)).append("]");
        builder.append("|SIGMA[").append(normalizePredicate(select.getWhere(), scope)).append("]");
        builder.append("|GAMMA[group=").append(groupBy).append(",agg=").append(aggregations).append("]");
        builder.append("|HAVING[").append(normalizePredicate(select.getHaving(), scope)).append("]");
        builder.append("|PI[").append(projections).append("]");
        return builder.toString();
    }

    private String normalizeFrom(SqlNode from, QueryBlockScope scope) {
        if (from == null) {
            return "";
        }
        if (from instanceof SqlIdentifier) {
            String source = cleanIdentifier(from);
            return "TABLE(" + source.toUpperCase(Locale.ROOT) + " AS " + scope.tokenForSource(source) + ")";
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            return "JOIN(" + normalizeFrom(join.getLeft(), scope)
                + "," + normalizeFrom(join.getRight(), scope)
                + "," + normalizeExpression(join.getCondition(), scope) + ")";
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                String alias = cleanIdentifier(operands.get(1));
                String token = scope.tokenForSource(alias);
                SqlNode relation = operands.get(0);
                if (relation instanceof SqlIdentifier) {
                    return "TABLE(" + cleanIdentifier(relation).toUpperCase(Locale.ROOT) + " AS " + token + ")";
                }
                if (relation instanceof SqlSelect || relation instanceof SqlWith || relation instanceof SqlOrderBy) {
                    return "DERIVED(" + token + ")";
                }
            }
        }
        if (from instanceof SqlCall) {
            List<String> items = new ArrayList<String>();
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                items.add(normalizeFrom(operand, scope));
            }
            Collections.sort(items);
            return from.getKind().name() + items;
        }
        return textStructuralForm(from.toString());
    }

    private String normalizePredicate(SqlNode predicate, QueryBlockScope scope) {
        if (predicate == null) {
            return "";
        }
        List<List<String>> expanded = expandPredicate(predicate, scope);
        List<String> alternatives = new ArrayList<String>();
        for (List<String> atoms : expanded) {
            Collections.sort(atoms);
            alternatives.add(String.join("&", atoms));
        }
        Collections.sort(alternatives);
        return String.join("|", alternatives);
    }

    private List<List<String>> expandPredicate(SqlNode predicate, QueryBlockScope scope) {
        if (predicate instanceof SqlBasicCall) {
            SqlBasicCall call = (SqlBasicCall) predicate;
            List<SqlNode> operands = call.getOperandList();
            if (call.getKind() == SqlKind.AND && operands.size() >= 2) {
                return crossProduct(expandPredicate(operands.get(0), scope), expandPredicate(operands.get(1), scope));
            }
            if (call.getKind() == SqlKind.OR && operands.size() >= 2) {
                List<List<String>> result = new ArrayList<List<String>>();
                result.addAll(expandPredicate(operands.get(0), scope));
                result.addAll(expandPredicate(operands.get(1), scope));
                return result;
            }
        }
        List<String> atom = Collections.singletonList(normalizeExpression(predicate, scope));
        return Collections.singletonList(atom);
    }

    private List<List<String>> crossProduct(List<List<String>> left, List<List<String>> right) {
        List<List<String>> result = new ArrayList<List<String>>();
        for (List<String> leftItems : left) {
            for (List<String> rightItems : right) {
                List<String> combined = new ArrayList<String>();
                combined.addAll(leftItems);
                combined.addAll(rightItems);
                result.add(combined);
            }
        }
        return result;
    }

    private String normalizeExpression(SqlNode node, QueryBlockScope scope) {
        if (node == null) {
            return "";
        }
        if (node instanceof SqlIdentifier) {
            SqlIdentifier identifier = (SqlIdentifier) node;
            if (identifier.isStar()) {
                return "*";
            }
            return scope.columnToken(cleanIdentifier(identifier));
        }
        if (node instanceof SqlLiteral) {
            return literalToken(node);
        }
        if (node instanceof SqlNodeList) {
            List<String> items = new ArrayList<String>();
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                items.add(normalizeExpression(item, scope));
            }
            Collections.sort(items);
            return items.toString();
        }
        if (node instanceof SqlBasicCall) {
            SqlBasicCall call = (SqlBasicCall) node;
            if (call.getKind() == SqlKind.AS && !call.getOperandList().isEmpty()) {
                return normalizeExpression(call.getOperandList().get(0), scope);
            }
            String operatorName = call.getOperator() == null ? call.getKind().name() : call.getOperator().getName();
            List<String> operands = new ArrayList<String>();
            for (SqlNode operand : call.getOperandList()) {
                operands.add(normalizeExpression(operand, scope));
            }
            if (isCountDistinct(call)) {
                return "COUNT_DISTINCT(" + String.join(",", operands) + ")";
            }
            if (isCommutativeComparison(call.getKind())) {
                Collections.sort(operands);
            }
            return operatorName.toUpperCase(Locale.ROOT) + "(" + String.join(",", operands) + ")";
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<String> operands = new ArrayList<String>();
            for (SqlNode operand : call.getOperandList()) {
                operands.add(normalizeExpression(operand, scope));
            }
            return call.getKind().name() + "(" + String.join(",", operands) + ")";
        }
        return fallbackNormalizeExpression(node.toString(), scope);
    }

    private boolean isCountDistinct(SqlBasicCall call) {
        String text = call == null ? "" : call.toString().toUpperCase(Locale.ROOT);
        String operatorName = call == null || call.getOperator() == null ? "" : call.getOperator().getName();
        return "COUNT".equalsIgnoreCase(operatorName) && text.contains("DISTINCT");
    }

    private boolean isCommutativeComparison(SqlKind kind) {
        return kind == SqlKind.EQUALS;
    }

    private String literalToken(SqlNode node) {
        String text = node == null ? "" : node.toString().trim();
        String upper = text.toUpperCase(Locale.ROOT);
        if ("NULL".equals(upper)) {
            return "<NULL>";
        }
        if ("TRUE".equals(upper) || "FALSE".equals(upper)) {
            return "<BOOLEAN>";
        }
        if (upper.startsWith("DATE ") || upper.startsWith("TIME ") || upper.startsWith("TIMESTAMP ")) {
            return "<DATE>";
        }
        if (text.startsWith("'") || text.startsWith("\"")) {
            return "<STRING>";
        }
        if (text.matches("-?\\d+(?:\\.\\d+)?")) {
            return "<NUMBER>";
        }
        return "<LITERAL>";
    }

    private String fallbackNormalizeExpression(String text, QueryBlockScope scope) {
        String normalized = textStructuralForm(text);
        for (String alias : scope.aliases()) {
            normalized = normalized.replaceAll("(?i)\\b" + Pattern.quote(alias) + "\\.", scope.tokenForSource(alias) + ".");
        }
        return normalized;
    }

    private String textStructuralForm(String sql) {
        if (sql == null) {
            return "";
        }
        String normalized = sql.replace('\n', ' ').replace('\r', ' ').trim();
        normalized = STRING_LITERAL_PATTERN.matcher(normalized).replaceAll("<STRING>");
        normalized = DATE_LITERAL_PATTERN.matcher(normalized).replaceAll("<DATE>");
        normalized = NUMBER_LITERAL_PATTERN.matcher(normalized).replaceAll("<NUMBER>");
        normalized = normalized.replaceAll("(?i)COUNT\\s*\\(\\s*DISTINCT\\s+([^)]*)\\)", "COUNT_DISTINCT($1)");
        normalized = replaceTextAliases(normalized);
        return normalized.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }

    private String replaceTextAliases(String sql) {
        Matcher matcher = FROM_JOIN_ALIAS_PATTERN.matcher(sql.toUpperCase(Locale.ROOT));
        LinkedHashMap<String, String> aliases = new LinkedHashMap<String, String>();
        int index = 1;
        while (matcher.find()) {
            String source = cleanIdentifier(matcher.group(2));
            String alias = cleanIdentifier(matcher.group(3));
            String key = alias == null || alias.isEmpty() || "WHERE".equals(alias) || "JOIN".equals(alias)
                ? sourceTail(source)
                : alias;
            if (key != null && !key.isEmpty() && !aliases.containsKey(key)) {
                aliases.put(key, "$" + index);
                index++;
            }
        }
        String result = sql;
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            result = result.replaceAll("(?i)\\b" + Pattern.quote(entry.getKey()) + "\\.", entry.getValue() + ".");
        }
        return result;
    }

    private List<String> externalReferences(SqlSelect select, QueryBlockScope scope) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        collectExternalReferences(select.getSelectList(), scope, result);
        collectExternalReferences(select.getWhere(), scope, result);
        collectExternalReferences(select.getGroup(), scope, result);
        collectExternalReferences(select.getHaving(), scope, result);
        collectExternalReferences(select.getOrderList(), scope, result);
        return new ArrayList<String>(result);
    }

    private void collectExternalReferences(SqlNode node, QueryBlockScope scope, Set<String> result) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlIdentifier) {
            SqlIdentifier identifier = (SqlIdentifier) node;
            if (identifier.isStar()) {
                return;
            }
            String text = cleanIdentifier(identifier);
            String qualifier = qualifier(text);
            if (qualifier != null && !qualifier.isEmpty() && !scope.hasAlias(qualifier)) {
                result.add(text);
            }
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectExternalReferences(item, scope, result);
            }
            return;
        }
        if (node instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) node;
            collectExternalReferences(join.getCondition(), scope, result);
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
            if (!operands.isEmpty()) {
                collectExternalReferences(operands.get(0), scope, result);
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectExternalReferences(operand, scope, result);
            }
        }
    }

    private List<String> selectList(SqlNodeList selectList) {
        if (selectList == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (SqlNode item : selectList.getList()) {
            result.add(normalizeText(item));
        }
        return result;
    }

    private List<String> nodeList(SqlNodeList nodeList) {
        if (nodeList == null || nodeList.size() == 0) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (SqlNode item : nodeList.getList()) {
            result.add(normalizeText(item));
        }
        return result;
    }

    private List<String> outputColumns(SqlNodeList selectList) {
        if (selectList == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        int index = 1;
        for (SqlNode item : selectList.getList()) {
            String alias = aliasFromSelectItem(item);
            if (alias == null || alias.isEmpty()) {
                alias = outputName(stripAlias(item), index);
            }
            result.add(alias);
            index++;
        }
        return result;
    }

    private String aliasFromSelectItem(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (operands.size() >= 2) {
                return cleanIdentifier(operands.get(1));
            }
        }
        return "";
    }

    private SqlNode stripAlias(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (!operands.isEmpty()) {
                return operands.get(0);
            }
        }
        return item;
    }

    private String outputName(SqlNode node, int index) {
        if (node instanceof SqlIdentifier) {
            String text = cleanIdentifier(node);
            int dot = text.lastIndexOf('.');
            return dot >= 0 ? text.substring(dot + 1) : text;
        }
        return "expr_" + index;
    }

    private QueryBlockNode findAncestorWithAlias(QueryBlockNode block,
                                                 String qualifier,
                                                 Map<String, QueryBlockNode> byId) {
        if (qualifier == null || qualifier.isEmpty()) {
            return null;
        }
        QueryBlockNode current = block;
        while (current != null && current.getParentBlockId() != null && !current.getParentBlockId().isEmpty()) {
            current = byId.get(current.getParentBlockId());
            if (current != null && containsIgnoreCase(current.getLocalAliases(), qualifier)) {
                return current;
            }
        }
        return null;
    }

    private boolean isAncestor(QueryBlockNode block, String ancestorBlockId, Map<String, QueryBlockNode> byId) {
        QueryBlockNode current = block;
        while (current != null && current.getParentBlockId() != null && !current.getParentBlockId().isEmpty()) {
            if (ancestorBlockId.equals(current.getParentBlockId())) {
                return true;
            }
            current = byId.get(current.getParentBlockId());
        }
        return false;
    }

    private boolean containsIgnoreCase(List<String> values, String expected) {
        for (String value : values) {
            if (expected.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, QueryBlockNode> blocksById(List<QueryBlockNode> blocks) {
        LinkedHashMap<String, QueryBlockNode> result = new LinkedHashMap<String, QueryBlockNode>();
        for (QueryBlockNode block : blocks) {
            result.put(block.getBlockId(), block);
        }
        return result;
    }

    private QueryBlockEdge edge(String fromBlockId,
                                String toBlockId,
                                String edgeType,
                                List<String> evidence,
                                boolean breaksCycle,
                                String rewriteAction) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("edgeDirection", "FROM_CONSUMER_TO_REFERENCED_BLOCK");
        return new QueryBlockEdge(fromBlockId, toBlockId, edgeType, evidence, breaksCycle, rewriteAction, attributes);
    }

    private String nextBlockId(BuildState state) {
        state.blockIndex++;
        return "QB" + state.blockIndex;
    }

    private int duplicateGroupCount(List<QueryBlockHashGroup> groups) {
        int count = 0;
        for (QueryBlockHashGroup group : groups) {
            if (group.getBlockIds().size() > 1) {
                count++;
            }
        }
        return count;
    }

    private int lateralPromotionCount(List<QueryBlockEdge> edges) {
        int count = 0;
        for (QueryBlockEdge edge : edges) {
            if (edge.isBreaksCycle() || QueryBlockEdge.LATERAL_JOIN_PROMOTION.equals(edge.getEdgeType())) {
                count++;
            }
        }
        return count;
    }

    private List<String> expressionList(List<Map<String, Object>> values, String key) {
        List<String> result = new ArrayList<String>();
        for (Map<String, Object> value : values) {
            String text = QbDagCollections.text(value.get(key));
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private List<String> outputListFromProjectionMaps(List<Map<String, Object>> projections) {
        List<String> result = new ArrayList<String>();
        int index = 1;
        for (Map<String, Object> projection : projections) {
            String alias = QbDagCollections.text(projection.get("alias"));
            result.add(alias.isEmpty() ? "expr_" + index : alias);
            index++;
        }
        return result;
    }

    private List<String> tableList(List<Map<String, Object>> tables) {
        List<String> result = new ArrayList<String>();
        for (Map<String, Object> table : tables) {
            String tableName = QbDagCollections.text(table.get("tableName"));
            if (!tableName.isEmpty()) {
                result.add(tableName);
            }
        }
        return result;
    }

    private List<String> aliasListFromTables(List<Map<String, Object>> tables) {
        List<String> result = new ArrayList<String>();
        for (Map<String, Object> table : tables) {
            String alias = QbDagCollections.text(table.get("alias"));
            if (!alias.isEmpty()) {
                result.add(alias);
            }
        }
        return result;
    }

    private String textRelationalForm(List<String> projections,
                                      List<String> tables,
                                      List<String> predicates,
                                      List<String> groupBy,
                                      List<String> aggregations) {
        List<String> normalizedPredicates = new ArrayList<String>();
        for (String predicate : predicates) {
            normalizedPredicates.add(textStructuralForm(predicate));
        }
        Collections.sort(normalizedPredicates);
        return "FROM" + textStructuralForm(tables.toString())
            + "|SIGMA" + normalizedPredicates
            + "|GAMMA[group=" + textStructuralForm(groupBy.toString()) + ",agg=" + textStructuralForm(aggregations.toString()) + "]"
            + "|PI" + textStructuralForm(projections.toString());
    }

    private Map<String, Object> attributes(String key, Object value) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(key, value);
        return result;
    }

    private int numericBlockId(String blockId) {
        if (blockId == null) {
            return 0;
        }
        return Integer.parseInt(blockId.replaceAll("[^0-9]", ""));
    }

    private String qualifier(String reference) {
        if (reference == null) {
            return "";
        }
        String clean = cleanIdentifier(reference);
        int dot = clean.indexOf('.');
        return dot > 0 ? clean.substring(0, dot) : "";
    }

    private String sourceTail(String source) {
        if (source == null) {
            return "";
        }
        String clean = cleanIdentifier(source);
        int dot = clean.lastIndexOf('.');
        return dot >= 0 ? clean.substring(dot + 1) : clean;
    }

    private String cleanIdentifier(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace("\"", "").replace("`", "").trim();
    }

    private String normalizeText(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replace('\n', ' ').replace('\r', ' ').trim().replaceAll("\\s+", " ");
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", Byte.valueOf(item)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private static final class BuildState {
        private final List<QueryBlockNode> blocks = new ArrayList<QueryBlockNode>();
        private final List<QueryBlockEdge> edges = new ArrayList<QueryBlockEdge>();
        private final List<QueryBlockHashGroup> hashGroups = new ArrayList<QueryBlockHashGroup>();
        private final List<QueryBlockDagIssue> issues = new ArrayList<QueryBlockDagIssue>();
        private final IdentityHashMap<SqlSelect, String> seenSelects = new IdentityHashMap<SqlSelect, String>();
        private final LinkedHashMap<String, String> cteBlockIds = new LinkedHashMap<String, String>();
        private int blockIndex;
    }

    private static final class QueryBlockScope {
        private final LinkedHashMap<String, String> sourceTokenByAlias = new LinkedHashMap<String, String>();
        private final LinkedHashMap<String, LinkedHashMap<String, Integer>> columnOrdinalBySource =
            new LinkedHashMap<String, LinkedHashMap<String, Integer>>();

        private static QueryBlockScope from(SqlNode from) {
            QueryBlockScope scope = new QueryBlockScope();
            scope.collectSources(from);
            return scope;
        }

        private void collectSources(SqlNode from) {
            if (from == null) {
                return;
            }
            if (from instanceof SqlIdentifier) {
                String source = clean(from);
                addSource(sourceTail(source));
                addSource(source);
                return;
            }
            if (from instanceof SqlJoin) {
                SqlJoin join = (SqlJoin) from;
                collectSources(join.getLeft());
                collectSources(join.getRight());
                return;
            }
            if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
                if (operands.size() >= 2) {
                    String alias = clean(operands.get(1));
                    addSource(alias);
                    SqlNode relation = operands.get(0);
                    if (relation instanceof SqlIdentifier) {
                        String source = clean(relation);
                        addAlias(sourceTail(source), tokenForSource(alias));
                        addAlias(source, tokenForSource(alias));
                    }
                }
                return;
            }
            if (from instanceof SqlCall) {
                for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                    collectSources(operand);
                }
            }
        }

        private void addSource(String alias) {
            if (alias == null || alias.trim().isEmpty()) {
                return;
            }
            String cleanAlias = alias.trim();
            if (!sourceTokenByAlias.containsKey(cleanAlias.toUpperCase(Locale.ROOT))) {
                String token = "$" + (sourceTokenByAlias.size() + 1);
                sourceTokenByAlias.put(cleanAlias.toUpperCase(Locale.ROOT), token);
            }
        }

        private void addAlias(String alias, String token) {
            if (alias == null || alias.trim().isEmpty() || token == null || token.isEmpty()) {
                return;
            }
            sourceTokenByAlias.put(alias.trim().toUpperCase(Locale.ROOT), token);
        }

        private boolean hasAlias(String alias) {
            return alias != null && sourceTokenByAlias.containsKey(alias.toUpperCase(Locale.ROOT));
        }

        private String tokenForSource(String alias) {
            if (alias == null || alias.trim().isEmpty()) {
                return "$0";
            }
            String token = sourceTokenByAlias.get(alias.trim().toUpperCase(Locale.ROOT));
            if (token != null) {
                return token;
            }
            addSource(alias);
            return sourceTokenByAlias.get(alias.trim().toUpperCase(Locale.ROOT));
        }

        private String columnToken(String identifier) {
            String clean = clean(identifier);
            int dot = clean.lastIndexOf('.');
            String qualifier = dot >= 0 ? clean.substring(0, dot) : "";
            String column = dot >= 0 ? clean.substring(dot + 1) : clean;
            String sourceToken = qualifier.isEmpty() ? "$0" : tokenForSource(qualifier);
            String sourceKey = sourceToken.toUpperCase(Locale.ROOT);
            LinkedHashMap<String, Integer> columns = columnOrdinalBySource.get(sourceKey);
            if (columns == null) {
                columns = new LinkedHashMap<String, Integer>();
                columnOrdinalBySource.put(sourceKey, columns);
            }
            String columnKey = column.toUpperCase(Locale.ROOT);
            Integer ordinal = columns.get(columnKey);
            if (ordinal == null) {
                ordinal = Integer.valueOf(columns.size() + 1);
                columns.put(columnKey, ordinal);
            }
            return sourceToken + ".$" + ordinal;
        }

        private List<String> aliases() {
            List<String> result = new ArrayList<String>();
            for (String alias : sourceTokenByAlias.keySet()) {
                result.add(alias.toLowerCase(Locale.ROOT));
            }
            return result;
        }

        private static String clean(Object value) {
            if (value == null) {
                return "";
            }
            return value.toString().replace("\"", "").replace("`", "").trim();
        }

        private static String sourceTail(String source) {
            if (source == null) {
                return "";
            }
            String clean = clean(source);
            int dot = clean.lastIndexOf('.');
            return dot >= 0 ? clean.substring(dot + 1) : clean;
        }
    }
}
