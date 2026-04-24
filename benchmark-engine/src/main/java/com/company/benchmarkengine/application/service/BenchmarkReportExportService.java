package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkEngineMetricVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkThresholdAssessmentVO;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.sqlforge.common.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkReportExportService {

    public List<BenchmarkReportArtifact> buildArtifacts(BenchmarkReportResponse response) {
        String jsonContent = JsonUtils.toJson(response);
        String pdfContent = renderPdf(response);
        String htmlContent = renderHtml(response);
        return Arrays.asList(
            buildArtifact(response, BenchmarkReportFormat.JSON, jsonContent),
            buildArtifact(response, BenchmarkReportFormat.PDF, pdfContent),
            buildArtifact(response, BenchmarkReportFormat.HTML, htmlContent)
        );
    }

    public BenchmarkRenderedReport toRenderedReport(BenchmarkReportArtifact artifact) {
        return new BenchmarkRenderedReport(
            MediaType.parseMediaType(artifact.getMediaType()),
            artifact.getFileName(),
            artifact.getContent().getBytes(StandardCharsets.UTF_8)
        );
    }

    private BenchmarkReportArtifact buildArtifact(BenchmarkReportResponse response,
                                                  BenchmarkReportFormat format,
                                                  String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new BenchmarkReportArtifact(
            format,
            buildFileName(response.getReportId(), format),
            format.getContentType(),
            Integer.valueOf(bytes.length),
            sha256(bytes),
            content
        );
    }

    private String buildFileName(String reportId, BenchmarkReportFormat format) {
        return "benchmark-report-" + reportId + "." + format.getFileExtension();
    }

    private String renderPdf(BenchmarkReportResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("%PDF-1.4\n");
        builder.append("% SQLForge benchmark export snapshot\n");
        builder.append("SQLForge Benchmark Report\n");
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
        return builder.toString();
    }

    private String renderHtml(BenchmarkReportResponse response) {
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
        return builder.toString();
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

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < hashed.length; index++) {
                builder.append(String.format(Locale.ROOT, "%02x", Byte.valueOf(hashed[index])));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
