package com.company.benchmarkengine.infrastructure.sqloptimization;

public class SqlOptimizationCombinedParseStatus {

    private String parseTaskId;
    private String status;
    private SqlOptimizationStructureParseStatus structureParse;

    public String getParseTaskId() {
        return parseTaskId;
    }

    public void setParseTaskId(String parseTaskId) {
        this.parseTaskId = parseTaskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public SqlOptimizationStructureParseStatus getStructureParse() {
        return structureParse;
    }

    public void setStructureParse(SqlOptimizationStructureParseStatus structureParse) {
        this.structureParse = structureParse;
    }
}
