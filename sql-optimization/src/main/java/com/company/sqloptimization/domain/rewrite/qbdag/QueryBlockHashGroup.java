package com.company.sqloptimization.domain.rewrite.qbdag;

import java.util.List;

public class QueryBlockHashGroup {

    private final String structuralHash;
    private final String representativeBlockId;
    private final List<String> blockIds;
    private final String collisionStatus;
    private final String normalizedRelationalForm;

    public QueryBlockHashGroup(String structuralHash,
                               String representativeBlockId,
                               List<String> blockIds,
                               String collisionStatus,
                               String normalizedRelationalForm) {
        this.structuralHash = structuralHash;
        this.representativeBlockId = representativeBlockId;
        this.blockIds = QbDagCollections.immutableStrings(blockIds);
        this.collisionStatus = collisionStatus;
        this.normalizedRelationalForm = normalizedRelationalForm;
    }

    public String getStructuralHash() {
        return structuralHash;
    }

    public String getRepresentativeBlockId() {
        return representativeBlockId;
    }

    public List<String> getBlockIds() {
        return blockIds;
    }

    public String getCollisionStatus() {
        return collisionStatus;
    }

    public String getNormalizedRelationalForm() {
        return normalizedRelationalForm;
    }
}
