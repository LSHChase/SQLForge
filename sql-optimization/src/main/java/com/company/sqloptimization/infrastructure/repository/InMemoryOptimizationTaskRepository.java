package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.OptimizationTaskStatus;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "local-placeholder", matchIfMissing = true)
public class InMemoryOptimizationTaskRepository implements OptimizationTaskRepository {

    private final Map<String, OptimizationTask> store = new ConcurrentHashMap<String, OptimizationTask>();

    @Override
    public OptimizationTask save(OptimizationTask task) {
        store.put(task.getTaskId(), task);
        return task;
    }

    @Override
    public OptimizationTask findByTaskId(String taskId) {
        return store.get(taskId);
    }

    @Override
    public List<OptimizationTask> findQueuedTasksSubmittedBefore(Instant cutoff) {
        if (cutoff == null) {
            return Collections.emptyList();
        }
        List<OptimizationTask> tasks = new ArrayList<OptimizationTask>();
        for (OptimizationTask task : store.values()) {
            if (task.getStatus() == OptimizationTaskStatus.QUEUED && !task.getSubmittedAt().isAfter(cutoff)) {
                tasks.add(task);
            }
        }
        return tasks;
    }
}
