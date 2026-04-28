package com.company.benchmarkengine.domain.benchmark.repository;

import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;

public interface BenchmarkTestSetRepository {

    BenchmarkTestSet saveTestSet(BenchmarkTestSet testSet);

    BenchmarkTestSet findTestSetByTestSetId(String testSetId);
}
