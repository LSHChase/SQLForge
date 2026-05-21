package com.company.sqloptimization.domain.rewrite.ir;

import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import java.util.List;
import java.util.Map;

public class RewriteCoreIrSnapshot {

    public static final String SCHEMA_VERSION = "rewrite-core-ir/v1";

    private final String schemaVersion;
    private final List<RewriteIrLayer> layers;
    private final AstNodeReference ast;
    private final List<TableReferenceIr> tableReferences;
    private final List<QueryBlockIr> queryBlocks;
    private final QueryBlockDag queryBlockDag;
    private final List<RelationalAlgebraNode> relationalAlgebra;
    private final RelationalRewritePlan relationalRewritePlan;
    private final BusinessIntentIr businessIntent;
    private final List<RewriteIrConflict> architectureConflicts;
    private final Map<String, Object> attributes;

    public RewriteCoreIrSnapshot(String schemaVersion,
                                 List<RewriteIrLayer> layers,
                                 AstNodeReference ast,
                                 List<TableReferenceIr> tableReferences,
                                 List<QueryBlockIr> queryBlocks,
                                 QueryBlockDag queryBlockDag,
                                 List<RelationalAlgebraNode> relationalAlgebra,
                                 RelationalRewritePlan relationalRewritePlan,
                                 BusinessIntentIr businessIntent,
                                 List<RewriteIrConflict> architectureConflicts,
                                 Map<String, Object> attributes) {
        this.schemaVersion = schemaVersion;
        this.layers = IrCollections.immutableList(layers);
        this.ast = ast;
        this.tableReferences = IrCollections.immutableList(tableReferences);
        this.queryBlocks = IrCollections.immutableList(queryBlocks);
        this.queryBlockDag = queryBlockDag;
        this.relationalAlgebra = IrCollections.immutableList(relationalAlgebra);
        this.relationalRewritePlan = relationalRewritePlan;
        this.businessIntent = businessIntent;
        this.architectureConflicts = IrCollections.immutableList(architectureConflicts);
        this.attributes = IrCollections.immutableMap(attributes);
    }

    public boolean containsLayer(RewriteIrLayer layer) {
        return layers.contains(layer);
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public List<RewriteIrLayer> getLayers() {
        return layers;
    }

    public AstNodeReference getAst() {
        return ast;
    }

    public List<TableReferenceIr> getTableReferences() {
        return tableReferences;
    }

    public List<QueryBlockIr> getQueryBlocks() {
        return queryBlocks;
    }

    public QueryBlockDag getQueryBlockDag() {
        return queryBlockDag;
    }

    public List<RelationalAlgebraNode> getRelationalAlgebra() {
        return relationalAlgebra;
    }

    public RelationalRewritePlan getRelationalRewritePlan() {
        return relationalRewritePlan;
    }

    public BusinessIntentIr getBusinessIntent() {
        return businessIntent;
    }

    public List<RewriteIrConflict> getArchitectureConflicts() {
        return architectureConflicts;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
