package com.company.sqloptimization.domain.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OptimizationTaskSourceContext {

    private final String sourceType;
    private final String sourceId;
    private final String batchId;
    private final String reportCode;
    private final String historyId;
    private final String parseTaskId;
    private final String datasourceCode;
    private final List<String> issueScenes;

    @JsonCreator
    public OptimizationTaskSourceContext(@JsonProperty("sourceType") String sourceType,
                                         @JsonProperty("sourceId") String sourceId,
                                         @JsonProperty("batchId") String batchId,
                                         @JsonProperty("reportCode") String reportCode,
                                         @JsonProperty("historyId") String historyId,
                                         @JsonProperty("parseTaskId") String parseTaskId,
                                         @JsonProperty("datasourceCode") String datasourceCode,
                                         @JsonProperty("issueScenes") List<String> issueScenes) {
        this.sourceType = trimToNull(sourceType);
        this.sourceId = trimToNull(sourceId);
        this.batchId = trimToNull(batchId);
        this.reportCode = trimToNull(reportCode);
        this.historyId = trimToNull(historyId);
        this.parseTaskId = trimToNull(parseTaskId);
        this.datasourceCode = trimToNull(datasourceCode);
        this.issueScenes = normalizeIssueScenes(issueScenes);
    }

    public static OptimizationTaskSourceContext empty() {
        return new OptimizationTaskSourceContext(null, null, null, null, null, null, null, Collections.<String>emptyList());
    }

    public boolean isEmpty() {
        return sourceType == null
            && sourceId == null
            && batchId == null
            && reportCode == null
            && historyId == null
            && parseTaskId == null
            && datasourceCode == null
            && issueScenes.isEmpty();
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getReportCode() {
        return reportCode;
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getParseTaskId() {
        return parseTaskId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public List<String> getIssueScenes() {
        return issueScenes;
    }

    private static List<String> normalizeIssueScenes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<String>();
        for (String value : values) {
            String item = trimToNull(value);
            if (item != null) {
                normalized.add(item);
            }
        }
        return Collections.unmodifiableList(new ArrayList<String>(normalized));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
