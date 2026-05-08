package com.company.sqloptimization.infrastructure.metadata;

public class DatasourceViewMetadataResponse {

    private Boolean view;
    private String viewDefinitionSql;
    private Boolean resolved;
    private String failureReason;
    private String metadataSource;

    public static DatasourceViewMetadataResponse table() {
        DatasourceViewMetadataResponse response = new DatasourceViewMetadataResponse();
        response.setView(Boolean.FALSE);
        response.setResolved(Boolean.TRUE);
        response.setMetadataSource("LIVE_METADATA");
        return response;
    }

    public static DatasourceViewMetadataResponse view(String definitionSql) {
        DatasourceViewMetadataResponse response = new DatasourceViewMetadataResponse();
        response.setView(Boolean.TRUE);
        response.setViewDefinitionSql(definitionSql);
        response.setResolved(Boolean.TRUE);
        response.setMetadataSource("LIVE_METADATA");
        return response;
    }

    public static DatasourceViewMetadataResponse unresolved(String failureReason) {
        DatasourceViewMetadataResponse response = new DatasourceViewMetadataResponse();
        response.setResolved(Boolean.FALSE);
        response.setFailureReason(failureReason);
        response.setMetadataSource("LIVE_METADATA_UNRESOLVED");
        return response;
    }

    public Boolean getView() {
        return view;
    }

    public void setView(Boolean view) {
        this.view = view;
    }

    public String getViewDefinitionSql() {
        return viewDefinitionSql;
    }

    public void setViewDefinitionSql(String viewDefinitionSql) {
        this.viewDefinitionSql = viewDefinitionSql;
    }

    public Boolean getResolved() {
        return resolved;
    }

    public void setResolved(Boolean resolved) {
        this.resolved = resolved;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getMetadataSource() {
        return metadataSource;
    }

    public void setMetadataSource(String metadataSource) {
        this.metadataSource = metadataSource;
    }
}
