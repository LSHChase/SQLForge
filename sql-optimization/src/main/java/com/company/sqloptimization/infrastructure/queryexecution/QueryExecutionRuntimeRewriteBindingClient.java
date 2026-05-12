package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;

public interface QueryExecutionRuntimeRewriteBindingClient {

    RuntimeRewriteBindingResponse publish(RuntimeRewriteBindingPublishRequest request);

    RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request);

    RuntimeRewriteBindingResponse unpublish(RuntimeRewriteBindingStateChangeRequest request);
}
