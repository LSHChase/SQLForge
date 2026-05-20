package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;

public interface QueryExecutionRuntimeRewriteBindingClient {

    RuntimeRewriteBindingResponse activate(RuntimeRewriteBindingActivationRequest request);

    RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request);
}
