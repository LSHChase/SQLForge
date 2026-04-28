package com.company.benchmarkengine.infrastructure.persistence.mapper;

import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetRecord;

public interface BenchmarkTestSetMapper {

    BenchmarkTestSetRecord selectByTestSetId(String testSetId);

    int insert(BenchmarkTestSetRecord record);

    int update(BenchmarkTestSetRecord record);
}
