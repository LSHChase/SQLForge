package com.company.sqloptimization.infrastructure.metadata;

public interface DatasourceViewMetadataClient {

    DatasourceViewMetadataResponse resolveView(DatasourceViewMetadataRequest request);

    static DatasourceViewMetadataClient unavailable() {
        return new DatasourceViewMetadataClient() {
            @Override
            public DatasourceViewMetadataResponse resolveView(DatasourceViewMetadataRequest request) {
                return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_UNAVAILABLE");
            }
        };
    }
}
