package com.company.sqloptimization.domain.rewrite.qbdag;

import java.util.List;
import java.util.Map;

public class QueryBlockNode {

    private final String blockId;
    private final String blockRole;
    private final String parentBlockId;
    private final String name;
    private final String alias;
    private final List<String> selectList;
    private final String fromClause;
    private final String whereClause;
    private final List<String> groupBy;
    private final String havingClause;
    private final List<String> outputColumns;
    private final List<String> localAliases;
    private final List<String> externalReferences;
    private final String structuralHash;
    private final String normalizedRelationalForm;
    private final String representativeBlockId;
    private final boolean equivalentToRepresentative;
    private final Map<String, Object> attributes;

    public QueryBlockNode(String blockId,
                          String blockRole,
                          String parentBlockId,
                          String name,
                          String alias,
                          List<String> selectList,
                          String fromClause,
                          String whereClause,
                          List<String> groupBy,
                          String havingClause,
                          List<String> outputColumns,
                          List<String> localAliases,
                          List<String> externalReferences,
                          String structuralHash,
                          String normalizedRelationalForm,
                          String representativeBlockId,
                          boolean equivalentToRepresentative,
                          Map<String, Object> attributes) {
        this.blockId = blockId;
        this.blockRole = blockRole;
        this.parentBlockId = parentBlockId;
        this.name = name;
        this.alias = alias;
        this.selectList = QbDagCollections.immutableStrings(selectList);
        this.fromClause = fromClause;
        this.whereClause = whereClause;
        this.groupBy = QbDagCollections.immutableStrings(groupBy);
        this.havingClause = havingClause;
        this.outputColumns = QbDagCollections.immutableStrings(outputColumns);
        this.localAliases = QbDagCollections.immutableStrings(localAliases);
        this.externalReferences = QbDagCollections.immutableStrings(externalReferences);
        this.structuralHash = structuralHash;
        this.normalizedRelationalForm = normalizedRelationalForm;
        this.representativeBlockId = representativeBlockId;
        this.equivalentToRepresentative = equivalentToRepresentative;
        this.attributes = QbDagCollections.immutableMap(attributes);
    }

    QueryBlockNode withRepresentative(String representativeBlockId, boolean equivalentToRepresentative) {
        return new QueryBlockNode(
            blockId,
            blockRole,
            parentBlockId,
            name,
            alias,
            selectList,
            fromClause,
            whereClause,
            groupBy,
            havingClause,
            outputColumns,
            localAliases,
            externalReferences,
            structuralHash,
            normalizedRelationalForm,
            representativeBlockId,
            equivalentToRepresentative,
            attributes
        );
    }

    public String getBlockId() {
        return blockId;
    }

    public String getBlockRole() {
        return blockRole;
    }

    public String getParentBlockId() {
        return parentBlockId;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public List<String> getSelectList() {
        return selectList;
    }

    public String getFromClause() {
        return fromClause;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public List<String> getGroupBy() {
        return groupBy;
    }

    public String getHavingClause() {
        return havingClause;
    }

    public List<String> getOutputColumns() {
        return outputColumns;
    }

    public List<String> getLocalAliases() {
        return localAliases;
    }

    public List<String> getExternalReferences() {
        return externalReferences;
    }

    public String getStructuralHash() {
        return structuralHash;
    }

    public String getNormalizedRelationalForm() {
        return normalizedRelationalForm;
    }

    public String getRepresentativeBlockId() {
        return representativeBlockId;
    }

    public boolean isEquivalentToRepresentative() {
        return equivalentToRepresentative;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
