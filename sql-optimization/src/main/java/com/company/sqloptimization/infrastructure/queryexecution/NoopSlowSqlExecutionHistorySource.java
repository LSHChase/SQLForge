package com.company.sqloptimization.infrastructure.queryexecution;

import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistoryCandidate;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistoryQuery;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistorySource;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NoopSlowSqlExecutionHistorySource implements SlowSqlExecutionHistorySource {

    @Override
    public List<SlowSqlExecutionHistoryCandidate> findCandidates(SlowSqlExecutionHistoryQuery query) {
        return Collections.emptyList();
    }
}
