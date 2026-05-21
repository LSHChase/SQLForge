package com.company.sqloptimization.domain.rewrite.parser;

import java.util.List;
import java.util.Map;

public class ParserStackFusionReport {

    public static final String SCHEMA_VERSION = "parser-stack-fusion/v1";

    private final String schemaVersion;
    private final String sourceSchemaVersion;
    private final String fusionStatus;
    private final List<Map<String, Object>> parserRoles;
    private final List<Map<String, Object>> calcitePlannerStages;
    private final List<ParserMetadataTag> metadataTags;
    private final List<RewriteConstraint> rewriteConstraints;
    private final List<HetuPlanHint> hetuPlanHints;
    private final Map<String, Object> attributes;

    public ParserStackFusionReport(String schemaVersion,
                                   String sourceSchemaVersion,
                                   String fusionStatus,
                                   List<Map<String, Object>> parserRoles,
                                   List<Map<String, Object>> calcitePlannerStages,
                                   List<ParserMetadataTag> metadataTags,
                                   List<RewriteConstraint> rewriteConstraints,
                                   List<HetuPlanHint> hetuPlanHints,
                                   Map<String, Object> attributes) {
        this.schemaVersion = ParserFusionCollections.text(schemaVersion);
        this.sourceSchemaVersion = ParserFusionCollections.text(sourceSchemaVersion);
        this.fusionStatus = ParserFusionCollections.text(fusionStatus);
        this.parserRoles = ParserFusionCollections.immutableMaps(parserRoles);
        this.calcitePlannerStages = ParserFusionCollections.immutableMaps(calcitePlannerStages);
        this.metadataTags = ParserFusionCollections.immutableList(metadataTags);
        this.rewriteConstraints = ParserFusionCollections.immutableList(rewriteConstraints);
        this.hetuPlanHints = ParserFusionCollections.immutableList(hetuPlanHints);
        this.attributes = ParserFusionCollections.immutableMap(attributes);
    }

    public boolean hasMetadataTag(String tool) {
        for (ParserMetadataTag tag : metadataTags) {
            if (tag.getTool().equals(ParserFusionCollections.text(tool))) {
                return true;
            }
        }
        return false;
    }

    public ParserMetadataTag firstMetadataTag(String tool) {
        for (ParserMetadataTag tag : metadataTags) {
            if (tag.getTool().equals(ParserFusionCollections.text(tool))) {
                return tag;
            }
        }
        return null;
    }

    public boolean hasRewriteConstraint(String constraintType) {
        for (RewriteConstraint constraint : rewriteConstraints) {
            if (constraint.getConstraintType().equals(ParserFusionCollections.text(constraintType))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasHetuPlanHint(String optimizationType) {
        return firstHetuPlanHint(optimizationType) != null;
    }

    public HetuPlanHint firstHetuPlanHint(String optimizationType) {
        for (HetuPlanHint hint : hetuPlanHints) {
            if (hint.getOptimizationType().equals(ParserFusionCollections.text(optimizationType))) {
                return hint;
            }
        }
        return null;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getSourceSchemaVersion() {
        return sourceSchemaVersion;
    }

    public String getFusionStatus() {
        return fusionStatus;
    }

    public List<Map<String, Object>> getParserRoles() {
        return parserRoles;
    }

    public List<Map<String, Object>> getCalcitePlannerStages() {
        return calcitePlannerStages;
    }

    public List<ParserMetadataTag> getMetadataTags() {
        return metadataTags;
    }

    public List<RewriteConstraint> getRewriteConstraints() {
        return rewriteConstraints;
    }

    public List<HetuPlanHint> getHetuPlanHints() {
        return hetuPlanHints;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
