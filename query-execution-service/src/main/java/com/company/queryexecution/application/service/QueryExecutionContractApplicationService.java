package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.util.Collections;
import org.springframework.stereotype.Service;

/**
 * Produces the query execution HTTP contract shape before the runtime pipeline exists.
 */
@Service
public class QueryExecutionContractApplicationService {

    public QueryExecuteResponse describeExecutionContract(QueryExecuteRequest request) {
        String actualSql = request.getSqlText().trim();
        return new QueryExecuteResponse(
            QueryExecutionStatus.FAILED,
            Collections.emptyList(),
            null,
            new QueryExecutionMetadataVO(
                request.getDatasourceType().name(),
                actualSql,
                0L,
                0L,
                false,
                false
            ),
            false,
            null,
            Collections.emptyList(),
            new QueryErrorDetailVO(
                ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_PIPELINE_NOT_READY,
                ErrorCodeConstants.QUERY_EXECUTION_PIPELINE_NOT_READY_MESSAGE,
                "Continue with D-TASK-003 to implement the synchronous execution pipeline.",
                false
            ),
            SqlFingerprintUtils.fingerprint(actualSql),
            "LONG_TERM_BASELINE",
            "TRANSITIONAL_SKELETON"
        );
    }
}
