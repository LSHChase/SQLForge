package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.task.OptimizationTask;
import com.company.sqloptimization.domain.task.repository.OptimizationTaskRepository;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
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
}
