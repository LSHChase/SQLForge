package com.company.benchmarkengine.infrastructure.persistence.mapper;

import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetCaseRecord;
import java.util.List;

public interface BenchmarkTestSetCaseMapper {

    List<BenchmarkTestSetCaseRecord> selectByTestSetId(String testSetId);

    int insert(BenchmarkTestSetCaseRecord record);

    int deleteByTestSetId(String testSetId);
}
