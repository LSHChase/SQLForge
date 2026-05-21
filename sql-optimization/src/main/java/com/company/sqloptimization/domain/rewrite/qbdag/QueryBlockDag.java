package com.company.sqloptimization.domain.rewrite.qbdag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryBlockDag {

    public static final String SCHEMA_VERSION = "query-block-dag/v1";

    private final String schemaVersion;
    private final String parserEngine;
    private final String rootBlockId;
    private final List<QueryBlockNode> blocks;
    private final List<QueryBlockEdge> edges;
    private final List<QueryBlockHashGroup> structuralHashGroups;
    private final List<QueryBlockDagIssue> issues;
    private final List<String> topologicalOrder;
    private final Map<String, Object> attributes;

    public QueryBlockDag(String schemaVersion,
                         String parserEngine,
                         String rootBlockId,
                         List<QueryBlockNode> blocks,
                         List<QueryBlockEdge> edges,
                         List<QueryBlockHashGroup> structuralHashGroups,
                         List<QueryBlockDagIssue> issues,
                         List<String> topologicalOrder,
                         Map<String, Object> attributes) {
        this.schemaVersion = schemaVersion;
        this.parserEngine = parserEngine;
        this.rootBlockId = rootBlockId;
        this.blocks = QbDagCollections.immutableList(blocks);
        this.edges = QbDagCollections.immutableList(edges);
        this.structuralHashGroups = QbDagCollections.immutableList(structuralHashGroups);
        this.issues = QbDagCollections.immutableList(issues);
        this.topologicalOrder = QbDagCollections.immutableStrings(topologicalOrder);
        this.attributes = QbDagCollections.immutableMap(attributes);
    }

    public static QueryBlockDag unavailable(String normalizedSql, String parserEngine, String reason) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("decompositionStatus", "UNAVAILABLE");
        attributes.put("runtimeBoundary", "NO_SQL_EXECUTION");
        attributes.put("sourceSqlLength", Integer.valueOf(normalizedSql == null ? 0 : normalizedSql.length()));
        List<QueryBlockDagIssue> issues = Collections.singletonList(new QueryBlockDagIssue(
            "QBDAG_UNAVAILABLE",
            "MEDIUM",
            "查询块 DAG 未能从当前 SQL 静态构建。",
            reason,
            Collections.<String>emptyList()
        ));
        return new QueryBlockDag(
            SCHEMA_VERSION,
            parserEngine,
            "",
            Collections.<QueryBlockNode>emptyList(),
            Collections.<QueryBlockEdge>emptyList(),
            Collections.<QueryBlockHashGroup>emptyList(),
            issues,
            Collections.<String>emptyList(),
            attributes
        );
    }

    public boolean hasDuplicateStructuralBlocks() {
        for (QueryBlockHashGroup group : structuralHashGroups) {
            if (group.getBlockIds().size() > 1) {
                return true;
            }
        }
        return false;
    }

    public List<QueryBlockHashGroup> getDuplicateStructuralGroups() {
        List<QueryBlockHashGroup> result = new ArrayList<QueryBlockHashGroup>();
        for (QueryBlockHashGroup group : structuralHashGroups) {
            if (group.getBlockIds().size() > 1) {
                result.add(group);
            }
        }
        return result;
    }

    public QueryBlockNode getBlock(String blockId) {
        for (QueryBlockNode block : blocks) {
            if (block.getBlockId().equals(blockId)) {
                return block;
            }
        }
        return null;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getParserEngine() {
        return parserEngine;
    }

    public String getRootBlockId() {
        return rootBlockId;
    }

    public List<QueryBlockNode> getBlocks() {
        return blocks;
    }

    public List<QueryBlockEdge> getEdges() {
        return edges;
    }

    public List<QueryBlockHashGroup> getStructuralHashGroups() {
        return structuralHashGroups;
    }

    public List<QueryBlockDagIssue> getIssues() {
        return issues;
    }

    public List<String> getTopologicalOrder() {
        return topologicalOrder;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
