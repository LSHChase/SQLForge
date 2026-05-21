package com.company.sqloptimization.domain.rewrite.parser;

import java.util.Map;

public class ParserMetadataTag {

    private final String tagId;
    private final String tool;
    private final String versionHint;
    private final int nestedDepth;
    private final String pattern;
    private final String matchedText;
    private final double confidence;
    private final Map<String, Object> attributes;

    public ParserMetadataTag(String tagId,
                             String tool,
                             String versionHint,
                             int nestedDepth,
                             String pattern,
                             String matchedText,
                             double confidence,
                             Map<String, Object> attributes) {
        this.tagId = ParserFusionCollections.text(tagId);
        this.tool = ParserFusionCollections.text(tool);
        this.versionHint = ParserFusionCollections.text(versionHint);
        this.nestedDepth = nestedDepth;
        this.pattern = ParserFusionCollections.text(pattern);
        this.matchedText = ParserFusionCollections.text(matchedText);
        this.confidence = confidence;
        this.attributes = ParserFusionCollections.immutableMap(attributes);
    }

    public String getTagId() {
        return tagId;
    }

    public String getTool() {
        return tool;
    }

    public String getVersionHint() {
        return versionHint;
    }

    public int getNestedDepth() {
        return nestedDepth;
    }

    public String getPattern() {
        return pattern;
    }

    public String getMatchedText() {
        return matchedText;
    }

    public double getConfidence() {
        return confidence;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
