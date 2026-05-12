package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;

final class NoopJdbcAgentRewriteRuleSyncPort implements JdbcAgentRewriteRuleSyncPort {

    @Override
    public JdbcAgentRewriteRuleSyncResult publish(RuntimeRewriteBinding binding) {
        return JdbcAgentRewriteRuleSyncResult.skipped("PUBLISH", "JDBC Agent Redis sync port is not configured");
    }

    @Override
    public JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding) {
        return JdbcAgentRewriteRuleSyncResult.skipped("DISABLE", "JDBC Agent Redis sync port is not configured");
    }
}
