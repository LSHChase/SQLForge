package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;

public interface JdbcAgentRewriteRuleSyncPort {

    JdbcAgentRewriteRuleSyncResult publish(RuntimeRewriteBinding binding);

    JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding);
}
