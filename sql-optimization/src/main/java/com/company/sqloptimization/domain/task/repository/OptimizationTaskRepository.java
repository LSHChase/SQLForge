package com.company.sqloptimization.domain.task.repository;

import com.company.sqloptimization.domain.task.OptimizationTask;

public interface OptimizationTaskRepository {

    OptimizationTask save(OptimizationTask task);

    OptimizationTask findByTaskId(String taskId);
}
