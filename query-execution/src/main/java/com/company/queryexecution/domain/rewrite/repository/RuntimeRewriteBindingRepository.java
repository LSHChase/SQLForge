package com.company.queryexecution.domain.rewrite.repository;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;
import java.util.List;

public interface RuntimeRewriteBindingRepository {

    RuntimeRewriteBinding save(RuntimeRewriteBinding binding);

    RuntimeRewriteBinding findByRuntimeBindingId(String runtimeBindingId);

    RuntimeRewriteBinding findActiveByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint);

    RuntimeRewriteBinding findLatestByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint);

    List<RuntimeRewriteBinding> findByTenantIdAndSqlFingerprint(String tenantId, String sqlFingerprint);

    List<RuntimeRewriteBinding> findActiveByTenantId(String tenantId);
}
