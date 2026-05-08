package com.company.sqloptimization.domain.parsehistory;

import java.util.List;

public interface SlowSqlExecutionHistorySource {

    List<SlowSqlExecutionHistoryCandidate> findCandidates(SlowSqlExecutionHistoryQuery query);
}
