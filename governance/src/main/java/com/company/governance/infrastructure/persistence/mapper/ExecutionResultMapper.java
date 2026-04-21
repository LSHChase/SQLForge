package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.ExecutionResultRecord;

public interface ExecutionResultMapper {

    ExecutionResultRecord selectById(String resultId);

    int insert(ExecutionResultRecord executionResultRecord);
}
