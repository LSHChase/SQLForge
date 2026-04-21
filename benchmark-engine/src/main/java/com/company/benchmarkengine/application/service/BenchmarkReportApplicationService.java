package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkEngineMetricVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkThresholdAssessmentVO;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkReportApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkReportApplicationService.class);

    private static final String QUERY_OPERATION = "BENCHMARK_REPORT_QUERY";

    private final BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService;
    private final BenchmarkTaskRepository benchmarkTaskRepository;

    public BenchmarkReportApplicationService(BenchmarkTaskModelApplicationService benchmarkTaskModelApplicationService,
                                             BenchmarkTaskRepository benchmarkTaskRepository) {
        this.benchmarkTaskModelApplicationService = benchmarkTaskModelApplicationService;
        this.benchmarkTaskRepository = benchmarkTaskRepository;
    }

    public BenchmarkReportResponse getJsonReport(String reportId) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} format={} status=START", QUERY_OPERATION, reportId, BenchmarkReportFormat.JSON);
        try {
            BenchmarkReportResponse response = benchmarkTaskModelApplicationService.buildReportResponse(loadReport(reportId));
            logEnd(reportId, BenchmarkReportFormat.JSON, start);
            return response;
        } catch (RuntimeException ex) {
            logFailure(reportId, BenchmarkReportFormat.JSON, start, ex);
            throw ex;
        }
    }

    public BenchmarkReportRawDataResponse getRawDataReport(String reportId) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} format={} status=START", QUERY_OPERATION, reportId, "RAW_DATA");
        try {
            BenchmarkReportRawDataResponse response = benchmarkTaskModelApplicationService.buildRawDataResponse(loadReport(reportId));
            logEnd(reportId, null, start);
            return response;
        } catch (RuntimeException ex) {
            logFailure(reportId, null, start, ex);
            throw ex;
        }
    }

    public BenchmarkRenderedReport renderReport(String reportId, BenchmarkReportFormat format) {
        long start = System.currentTimeMillis();
        LOGGER.info("operation={} entity={} format={} status=START", QUERY_OPERATION, reportId, format);
        try {
            BenchmarkReportResponse response = benchmarkTaskModelApplicationService.buildReportResponse(loadReport(reportId));
            BenchmarkRenderedReport renderedReport = format == BenchmarkReportFormat.PDF
                ? renderPdf(response)
                : renderHtml(response);
            logEnd(reportId, format, start);
            return renderedReport;
        } catch (RuntimeException ex) {
            logFailure(reportId, format, start, ex);
            throw ex;
        }
    }

    public BenchmarkReportFormat parseFormat(String rawFormat) {
        if (rawFormat == null || rawFormat.trim().length() == 0) {
            return BenchmarkReportFormat.JSON;
        }
        try {
            return BenchmarkReportFormat.valueOf(rawFormat.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                "Unsupported benchmark report format: " + rawFormat
            );
        }
    }

    private BenchmarkReport loadReport(String reportId) {
        BenchmarkReport report = benchmarkTaskRepository.findReportByReportId(reportId);
        if (report == null) {
            throw new BizException(
                ErrorCodeConstants.BENCHMARK_REPORT_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Benchmark report does not exist for reportId=" + reportId
            );
        }
        verifyTenantAccess(report.getTenantId());
        return report;
    }

    private void verifyTenantAccess(String resourceTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException("Authenticated tenant cannot access this benchmark report");
        }
    }

    private BenchmarkRenderedReport renderPdf(BenchmarkReportResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("%PDF-1.4\n");
        builder.append("% SQLForge placeholder benchmark report\n");
        builder.append("SQLForge Benchmark Report Placeholder\n");
        builder.append("reportId=").append(response.getReportId()).append('\n');
        builder.append("taskId=").append(response.getTaskId()).append('\n');
        builder.append("taskType=").append(response.getTaskType()).append('\n');
        builder.append("verdict=").append(response.getVerdict()).append('\n');
        builder.append("targetEngines=").append(response.getTargetEngines()).append('\n');
        builder.append("generatedAt=").append(response.getGeneratedAt()).append('\n');
        appendEngineSummary(builder, response.getEngineResults());
        appendThresholdSummary(builder, response.getThresholdAssessments());
        appendRecommendationSummary(builder, response.getRecommendations());
        builder.append("%%EOF\n");
        return new BenchmarkRenderedReport(
            MediaType.APPLICATION_PDF,
            buildFileName(response, BenchmarkReportFormat.PDF),
            builder.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private BenchmarkRenderedReport renderHtml(BenchmarkReportResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        builder.append("<title>SQLForge Benchmark Report</title>");
        builder.append("<style>");
        builder.append("body{font-family:Helvetica,Arial,sans-serif;margin:32px;color:#1f2937;background:#f8fafc;}");
        builder.append("section{background:#ffffff;border:1px solid #dbe3ee;border-radius:12px;padding:20px;margin-bottom:20px;}");
        builder.append("table{width:100%;border-collapse:collapse;}th,td{padding:8px 10px;border-bottom:1px solid #e5e7eb;text-align:left;}");
        builder.append("h1,h2{margin-top:0;} .meta{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;}");
        builder.append("</style></head><body>");
        builder.append("<h1>SQLForge Benchmark Report</h1>");
        builder.append("<section><div class=\"meta\">");
        appendHtmlMeta(builder, "Report ID", response.getReportId());
        appendHtmlMeta(builder, "Task ID", response.getTaskId());
        appendHtmlMeta(builder, "Task Type", String.valueOf(response.getTaskType()));
        appendHtmlMeta(builder, "Verdict", String.valueOf(response.getVerdict()));
        appendHtmlMeta(builder, "Generated At", String.valueOf(response.getGeneratedAt()));
        appendHtmlMeta(builder, "Target Engines", String.valueOf(response.getTargetEngines()));
        builder.append("</div></section>");
        appendHtmlEngineTable(builder, response.getEngineResults());
        appendHtmlThresholdTable(builder, response.getThresholdAssessments());
        appendHtmlTrendCharts(builder, response);
        appendHtmlRecommendationList(builder, response.getRecommendations());
        builder.append("</body></html>");
        return new BenchmarkRenderedReport(
            MediaType.TEXT_HTML,
            buildFileName(response, BenchmarkReportFormat.HTML),
            builder.toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    private String buildFileName(BenchmarkReportResponse response, BenchmarkReportFormat format) {
        return "benchmark-report-" + response.getReportId() + "." + format.getFileExtension();
    }

    private void appendEngineSummary(StringBuilder builder, List<BenchmarkEngineMetricVO> engineResults) {
        for (BenchmarkEngineMetricVO engineResult : engineResults) {
            builder.append("engine=")
                .append(engineResult.getEngine())
                .append(", actualQps=")
                .append(engineResult.getActualQps())
                .append(", p99LatencyMs=")
                .append(engineResult.getP99LatencyMs())
                .append(", verdict=")
                .append(engineResult.getVerdict())
                .append('\n');
        }
    }

    private void appendThresholdSummary(StringBuilder builder,
                                        List<BenchmarkThresholdAssessmentVO> thresholdAssessments) {
        for (BenchmarkThresholdAssessmentVO assessment : thresholdAssessments) {
            builder.append("threshold=")
                .append(assessment.getMetric())
                .append(", actual=")
                .append(assessment.getActualValue())
                .append(", target=")
                .append(assessment.getTargetValue())
                .append(", verdict=")
                .append(assessment.getVerdict())
                .append('\n');
        }
    }

    private void appendRecommendationSummary(StringBuilder builder,
                                             List<BenchmarkRecommendationVO> recommendations) {
        for (BenchmarkRecommendationVO recommendation : recommendations) {
            builder.append("recommendation=")
                .append(recommendation.getCategory())
                .append(", title=")
                .append(recommendation.getTitle())
                .append(", risk=")
                .append(recommendation.getRiskLevel())
                .append('\n');
        }
    }

    private void appendHtmlMeta(StringBuilder builder, String label, String value) {
        builder.append("<div><strong>").append(label).append(":</strong> ").append(value).append("</div>");
    }

    private void appendHtmlEngineTable(StringBuilder builder, List<BenchmarkEngineMetricVO> engineResults) {
        builder.append("<section><h2>Engine Comparison</h2><table><thead><tr>");
        builder.append("<th>Engine</th><th>QPS</th><th>P50</th><th>P99</th><th>CPU</th><th>Memory</th><th>Verdict</th>");
        builder.append("</tr></thead><tbody>");
        for (BenchmarkEngineMetricVO engineResult : engineResults) {
            builder.append("<tr><td>").append(engineResult.getEngine()).append("</td><td>")
                .append(engineResult.getActualQps()).append("</td><td>")
                .append(engineResult.getP50LatencyMs()).append("</td><td>")
                .append(engineResult.getP99LatencyMs()).append("</td><td>")
                .append(engineResult.getCpuUsagePercent()).append("</td><td>")
                .append(engineResult.getMemoryUsageMb()).append("</td><td>")
                .append(engineResult.getVerdict()).append("</td></tr>");
        }
        builder.append("</tbody></table></section>");
    }

    private void appendHtmlThresholdTable(StringBuilder builder,
                                          List<BenchmarkThresholdAssessmentVO> thresholdAssessments) {
        builder.append("<section><h2>Threshold Assessments</h2><table><thead><tr>");
        builder.append("<th>Metric</th><th>Actual</th><th>Target</th><th>Verdict</th><th>Summary</th>");
        builder.append("</tr></thead><tbody>");
        for (BenchmarkThresholdAssessmentVO assessment : thresholdAssessments) {
            builder.append("<tr><td>").append(assessment.getMetric()).append("</td><td>")
                .append(assessment.getActualValue()).append("</td><td>")
                .append(assessment.getTargetValue()).append("</td><td>")
                .append(assessment.getVerdict()).append("</td><td>")
                .append(assessment.getSummary()).append("</td></tr>");
        }
        builder.append("</tbody></table></section>");
    }

    private void appendHtmlTrendCharts(StringBuilder builder, BenchmarkReportResponse response) {
        builder.append("<section><h2>Trend Charts</h2>");
        for (int index = 0; index < response.getTrendCharts().size(); index++) {
            builder.append("<article><h3>")
                .append(response.getTrendCharts().get(index).getTitle())
                .append("</h3><ul>");
            for (int seriesIndex = 0; seriesIndex < response.getTrendCharts().get(index).getSeries().size(); seriesIndex++) {
                builder.append("<li>")
                    .append(response.getTrendCharts().get(index).getSeries().get(seriesIndex).getSeriesName())
                    .append(": ")
                    .append(response.getTrendCharts().get(index).getSeries().get(seriesIndex).getPoints())
                    .append("</li>");
            }
            builder.append("</ul></article>");
        }
        builder.append("</section>");
    }

    private void appendHtmlRecommendationList(StringBuilder builder,
                                              List<BenchmarkRecommendationVO> recommendations) {
        builder.append("<section><h2>Recommendations</h2><ul>");
        for (BenchmarkRecommendationVO recommendation : recommendations) {
            builder.append("<li><strong>")
                .append(recommendation.getTitle())
                .append("</strong> - ")
                .append(recommendation.getSummary())
                .append(" (")
                .append(recommendation.getRiskLevel())
                .append(")</li>");
        }
        builder.append("</ul></section>");
    }

    private void logEnd(String reportId, BenchmarkReportFormat format, long start) {
        LOGGER.info(
            "operation={} entity={} format={} costMs={} status=END",
            QUERY_OPERATION,
            reportId,
            format,
            System.currentTimeMillis() - start
        );
    }

    private void logFailure(String reportId, BenchmarkReportFormat format, long start, RuntimeException ex) {
        LOGGER.error(
            "operation={} entity={} format={} costMs={} status=FAILED phase=EXCEPTION reason={}",
            QUERY_OPERATION,
            reportId,
            format,
            System.currentTimeMillis() - start,
            ex.getMessage(),
            ex
        );
    }
}
