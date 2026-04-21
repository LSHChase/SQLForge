package com.company.benchmarkengine.infrastructure.repository;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryBenchmarkTaskRepository implements BenchmarkTaskRepository {

    private final Map<String, BenchmarkTask> taskStore = new ConcurrentHashMap<String, BenchmarkTask>();
    private final Map<String, BenchmarkReport> reportByTaskStore = new ConcurrentHashMap<String, BenchmarkReport>();
    private final Map<String, BenchmarkReport> reportByReportIdStore = new ConcurrentHashMap<String, BenchmarkReport>();

    @Override
    public BenchmarkTask saveTask(BenchmarkTask task) {
        taskStore.put(task.getTaskId(), task);
        return task;
    }

    @Override
    public BenchmarkTask findTaskByTaskId(String taskId) {
        return taskStore.get(taskId);
    }

    @Override
    public BenchmarkReport saveReport(BenchmarkReport report) {
        reportByTaskStore.put(report.getTaskId(), report);
        reportByReportIdStore.put(report.getReportId(), report);
        return report;
    }

    @Override
    public BenchmarkReport findReportByTaskId(String taskId) {
        return reportByTaskStore.get(taskId);
    }

    @Override
    public BenchmarkReport findReportByReportId(String reportId) {
        return reportByReportIdStore.get(reportId);
    }
}
