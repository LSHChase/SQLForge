package com.company.sqloptimization.application.service.report;

public class ReportSqlResolveRequest {

    private final String tenantId;
    private final String datasourceCode;
    private final String stage;
    private final String priority;
    private final String reportCode;

    public ReportSqlResolveRequest(String tenantId,
                                   String datasourceCode,
                                   String stage,
                                   String priority,
                                   String reportCode) {
        this.tenantId = tenantId;
        this.datasourceCode = datasourceCode;
        this.stage = stage;
        this.priority = priority;
        this.reportCode = reportCode;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDatasourceCode() {
        return datasourceCode;
    }

    public String getStage() {
        return stage;
    }

    public String getPriority() {
        return priority;
    }

    public String getReportCode() {
        return reportCode;
    }
}
