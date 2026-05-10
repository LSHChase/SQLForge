package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;

public interface QueryExecutionResultDigestClient {

    QueryExecutionResultDigestResponse executeDigest(QueryExecutionResultDigestRequest request);
}
