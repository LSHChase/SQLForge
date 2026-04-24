package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.OptimizationTask;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class OptimizationMetricsRecorder {

    private static final String METRIC_TASKS_SUBMITTED = "sqlforge.sql.optimization.tasks.submitted";
    private static final String METRIC_TASKS_TERMINAL = "sqlforge.sql.optimization.tasks.terminal";
    private static final String METRIC_WORKER_LATENCY = "sqlforge.sql.optimization.worker.latency";
    private static final String UNKNOWN_VALUE = "UNKNOWN";

    private final MeterRegistry meterRegistry;

    public OptimizationMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    static OptimizationMetricsRecorder noop() {
        return new OptimizationMetricsRecorder(new SimpleMeterRegistry());
    }

    public void recordTaskSubmitted(OptimizationTask task) {
        if (task == null) {
            return;
        }
        Counter.builder(METRIC_TASKS_SUBMITTED)
            .description("Accepted sql-optimization tasks submitted to the async carrier.")
            .tags(
                "task_type", taskType(task),
                "datasource_type", datasourceType(task.getDatasourceType())
            )
            .register(meterRegistry)
            .increment();
    }

    public void recordWorkerTerminal(OptimizationTask task, long costMs) {
        if (task == null) {
            return;
        }
        String taskType = taskType(task);
        String resultStatus = taskStatus(task);
        Counter.builder(METRIC_TASKS_TERMINAL)
            .description("Terminal sql-optimization worker outcomes.")
            .tags(
                "task_type", taskType,
                "result_status", resultStatus
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_WORKER_LATENCY)
            .description("Sql-optimization worker processing latency.")
            .tags(
                "task_type", taskType,
                "result_status", resultStatus
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    public void recordWorkerException(OptimizationTask task, long costMs) {
        String taskType = task == null ? UNKNOWN_VALUE : taskType(task);
        Counter.builder(METRIC_TASKS_TERMINAL)
            .description("Terminal sql-optimization worker outcomes.")
            .tags(
                "task_type", taskType,
                "result_status", "FAILED_EXCEPTION"
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_WORKER_LATENCY)
            .description("Sql-optimization worker processing latency.")
            .tags(
                "task_type", taskType,
                "result_status", "FAILED_EXCEPTION"
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    private String taskType(OptimizationTask task) {
        return task == null || task.getTaskType() == null ? UNKNOWN_VALUE : task.getTaskType().name();
    }

    private String taskStatus(OptimizationTask task) {
        return task == null || task.getStatus() == null ? UNKNOWN_VALUE : task.getStatus().name();
    }

    private String datasourceType(DataSourceTypeEnum datasourceType) {
        return datasourceType == null ? UNKNOWN_VALUE : datasourceType.name();
    }
}
