package com.company.benchmarkengine.infrastructure.repository;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTestSetRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "benchmark-engine.queues", name = "mode", havingValue = "local-placeholder", matchIfMissing = true)
public class InMemoryBenchmarkTaskRepository implements BenchmarkTaskRepository, BenchmarkTestSetRepository {

    private final Map<String, BenchmarkTask> taskStore = new ConcurrentHashMap<String, BenchmarkTask>();
    private final Map<String, BenchmarkReport> reportByTaskStore = new ConcurrentHashMap<String, BenchmarkReport>();
    private final Map<String, BenchmarkReport> reportByReportIdStore = new ConcurrentHashMap<String, BenchmarkReport>();
    private final Map<String, BenchmarkTestSet> testSetStore = new ConcurrentHashMap<String, BenchmarkTestSet>();

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

    @Override
    public List<BenchmarkTask> findQueuedTasksSubmittedBefore(Instant cutoff) {
        if (cutoff == null) {
            return Collections.emptyList();
        }
        List<BenchmarkTask> tasks = new ArrayList<BenchmarkTask>();
        for (BenchmarkTask task : taskStore.values()) {
            if (task.getStatus() == BenchmarkTaskStatus.QUEUED && !task.getSubmittedAt().isAfter(cutoff)) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    @Override
    public BenchmarkTestSet saveTestSet(BenchmarkTestSet testSet) {
        testSetStore.put(testSet.getTestSetId(), testSet);
        return testSet;
    }

    @Override
    public BenchmarkTestSet findTestSetByTestSetId(String testSetId) {
        return testSetStore.get(testSetId);
    }
}
