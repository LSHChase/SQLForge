package com.company.queryexecution.application.service;

import com.company.queryexecution.domain.rewrite.RuntimeRewriteBinding;

public interface JdbcAgentRewriteRuleSyncPort {

    JdbcAgentRewriteRuleSyncResult activate(RuntimeRewriteBinding binding);

    JdbcAgentRewriteRuleSyncResult disable(RuntimeRewriteBinding binding);
}
