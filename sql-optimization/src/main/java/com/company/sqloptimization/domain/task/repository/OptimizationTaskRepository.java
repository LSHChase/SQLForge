package com.company.sqloptimization.domain.task.repository;

import com.company.sqloptimization.domain.task.OptimizationTask;
import java.time.Instant;
import java.util.List;

public interface OptimizationTaskRepository {

    OptimizationTask save(OptimizationTask task);

    OptimizationTask findByTaskId(String taskId);

    List<OptimizationTask> findQueuedTasksSubmittedBefore(Instant cutoff);
}
