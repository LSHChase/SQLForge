package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.QueryHistoryRecord;

public interface QueryHistoryMapper {

    QueryHistoryRecord selectById(String historyId);

    int insert(QueryHistoryRecord queryHistoryRecord);
}
