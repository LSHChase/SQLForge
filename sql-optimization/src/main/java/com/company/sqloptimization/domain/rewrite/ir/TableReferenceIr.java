package com.company.sqloptimization.domain.rewrite.ir;

import java.util.List;
import java.util.Map;

public class TableReferenceIr {

    private final String referenceId;
    private final String source;
    private final String alias;
    private final TableReferenceSourceKind sourceKind;
    private final String accessPath;
    private final List<String> predicatePushdownCandidates;
    private final Map<String, Object> attributes;

    public TableReferenceIr(String referenceId,
                            String source,
                            String alias,
                            TableReferenceSourceKind sourceKind,
                            String accessPath,
                            List<String> predicatePushdownCandidates,
                            Map<String, Object> attributes) {
        this.referenceId = referenceId;
        this.source = source;
        this.alias = alias;
        this.sourceKind = sourceKind == null ? TableReferenceSourceKind.UNKNOWN : sourceKind;
        this.accessPath = accessPath;
        this.predicatePushdownCandidates = IrCollections.immutableStrings(predicatePushdownCandidates);
        this.attributes = IrCollections.immutableMap(attributes);
    }

    public RewriteIrLayer getLayer() {
        return RewriteIrLayer.L2_TABLE_REFERENCE;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getSource() {
        return source;
    }

    public String getAlias() {
        return alias;
    }

    public TableReferenceSourceKind getSourceKind() {
        return sourceKind;
    }

    public String getAccessPath() {
        return accessPath;
    }

    public List<String> getPredicatePushdownCandidates() {
        return predicatePushdownCandidates;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
