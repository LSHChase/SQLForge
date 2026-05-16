package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class BenchmarkMetricsRecorder {

    private static final String METRIC_TASKS_SUBMITTED = "sqlforge.benchmark.engine.tasks.submitted";
    private static final String METRIC_TASKS_TERMINAL = "sqlforge.benchmark.engine.tasks.terminal";
    private static final String METRIC_REPORTS_GENERATED = "sqlforge.benchmark.engine.reports.generated";
    private static final String METRIC_WORKER_LATENCY = "sqlforge.benchmark.engine.worker.latency";
    private static final String METRIC_REPORT_REQUESTS = "sqlforge.benchmark.engine.report.requests";
    private static final String METRIC_REPORT_LATENCY = "sqlforge.benchmark.engine.report.latency";
    private static final String UNKNOWN_VALUE = "UNKNOWN";

    private final MeterRegistry meterRegistry;

    public BenchmarkMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    static BenchmarkMetricsRecorder noop() {
        return new BenchmarkMetricsRecorder(new SimpleMeterRegistry());
    }

    public void recordTaskSubmitted(BenchmarkTask task) {
        if (task == null) {
            return;
        }
        Counter.builder(METRIC_TASKS_SUBMITTED)
            .description("已提交到异步载体的 benchmark-engine 任务数。")
            .tags("task_type", taskType(task))
            .register(meterRegistry)
            .increment();
    }

    public void recordWorkerTerminal(BenchmarkTask task, long costMs) {
        if (task == null) {
            return;
        }
        String taskType = taskType(task);
        String resultStatus = taskStatus(task);
        Counter.builder(METRIC_TASKS_TERMINAL)
            .description("benchmark-engine worker 终态结果数。")
            .tags(
                "task_type", taskType,
                "result_status", resultStatus
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_WORKER_LATENCY)
            .description("benchmark-engine worker 处理延迟。")
            .tags(
                "task_type", taskType,
                "result_status", resultStatus
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    public void recordWorkerException(BenchmarkTask task, long costMs) {
        String taskType = task == null ? UNKNOWN_VALUE : taskType(task);
        Counter.builder(METRIC_TASKS_TERMINAL)
            .description("benchmark-engine worker 终态结果数。")
            .tags(
                "task_type", taskType,
                "result_status", "FAILED_EXCEPTION"
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_WORKER_LATENCY)
            .description("benchmark-engine worker 处理延迟。")
            .tags(
                "task_type", taskType,
                "result_status", "FAILED_EXCEPTION"
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    public void recordReportGenerated(BenchmarkReport report) {
        if (report == null) {
            return;
        }
        Counter.builder(METRIC_REPORTS_GENERATED)
            .description("异步 worker 生成的压测报告数。")
            .tags("task_type", reportTaskType(report))
            .register(meterRegistry)
            .increment();
    }

    public void recordReportResponse(BenchmarkReportFormat format, long costMs) {
        Counter.builder(METRIC_REPORT_REQUESTS)
            .description("benchmark-engine 处理的压测报告请求数。")
            .tags(
                "format", reportFormat(format),
                "result_status", "SUCCESS"
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_REPORT_LATENCY)
            .description("压测报告请求延迟。")
            .tags(
                "format", reportFormat(format),
                "result_status", "SUCCESS"
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    public void recordReportFailure(BenchmarkReportFormat format, long costMs) {
        Counter.builder(METRIC_REPORT_REQUESTS)
            .description("benchmark-engine 处理的压测报告请求数。")
            .tags(
                "format", reportFormat(format),
                "result_status", "FAILED"
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_REPORT_LATENCY)
            .description("压测报告请求延迟。")
            .tags(
                "format", reportFormat(format),
                "result_status", "FAILED"
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    private String taskType(BenchmarkTask task) {
        return task == null || task.getTaskType() == null ? UNKNOWN_VALUE : task.getTaskType().name();
    }

    private String taskStatus(BenchmarkTask task) {
        return task == null || task.getStatus() == null ? UNKNOWN_VALUE : task.getStatus().name();
    }

    private String reportTaskType(BenchmarkReport report) {
        return report == null || report.getTaskType() == null ? UNKNOWN_VALUE : report.getTaskType().name();
    }

    private String reportFormat(BenchmarkReportFormat format) {
        return format == null ? "RAW_DATA" : format.name();
    }
}
