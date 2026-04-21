package com.company.benchmarkengine.domain.benchmark.repository;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;

public interface BenchmarkTaskRepository {

    BenchmarkTask saveTask(BenchmarkTask task);

    BenchmarkTask findTaskByTaskId(String taskId);

    BenchmarkReport saveReport(BenchmarkReport report);

    BenchmarkReport findReportByTaskId(String taskId);

    BenchmarkReport findReportByReportId(String reportId);
}
