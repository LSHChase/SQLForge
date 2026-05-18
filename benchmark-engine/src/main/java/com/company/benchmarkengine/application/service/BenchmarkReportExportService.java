package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.vo.BenchmarkEngineMetricVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationVO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkThresholdAssessmentVO;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleEvidenceManifest;
import com.company.sqlforge.common.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkReportExportService {

    public List<BenchmarkReportArtifact> buildArtifacts(BenchmarkReportResponse response) {
        List<BenchmarkReportArtifact> artifacts = new ArrayList<BenchmarkReportArtifact>(3);
        artifacts.add(buildReportArtifact(response, BenchmarkReportFormat.JSON));
        artifacts.add(buildReportArtifact(response, BenchmarkReportFormat.PDF));
        artifacts.add(buildReportArtifact(response, BenchmarkReportFormat.HTML));
        return artifacts;
    }

    public BenchmarkReportArtifact buildRawDataArtifact(BenchmarkReportRawDataResponse response) {
        return buildArtifact(
            "raw-data",
            BenchmarkReportArtifactKind.RAW_DATA_SNAPSHOT,
            response.getReportId(),
            BenchmarkReportFormat.JSON,
            JsonUtils.toJson(response)
        );
    }

    public BenchmarkReportArtifact buildReportArtifact(BenchmarkReportResponse response, BenchmarkReportFormat format) {
        if (format == BenchmarkReportFormat.JSON) {
            return buildArtifact(
                "json-export",
                BenchmarkReportArtifactKind.REPORT_EXPORT,
                response.getReportId(),
                BenchmarkReportFormat.JSON,
                JsonUtils.toJson(response)
            );
        }
        if (format == BenchmarkReportFormat.PDF) {
            return buildArtifact(
                "pdf-export",
                BenchmarkReportArtifactKind.REPORT_EXPORT,
                response.getReportId(),
                BenchmarkReportFormat.PDF,
                renderPdf(response)
            );
        }
        if (format == BenchmarkReportFormat.HTML) {
            return buildArtifact(
                "html-export",
                BenchmarkReportArtifactKind.REPORT_EXPORT,
                response.getReportId(),
                BenchmarkReportFormat.HTML,
                renderHtml(response)
            );
        }
        throw new IllegalArgumentException("不支持用于构建产物的压测报告格式：" + format);
    }

    public BenchmarkRenderedReport toRenderedReport(BenchmarkReportArtifact artifact) {
        return new BenchmarkRenderedReport(
            MediaType.parseMediaType(artifact.getMediaType()),
            artifact.getFileName(),
            artifact.getContent().getBytes(StandardCharsets.UTF_8)
        );
    }

    private BenchmarkReportArtifact buildArtifact(String artifactKey,
                                                  BenchmarkReportArtifactKind artifactKind,
                                                  String reportId,
                                                  BenchmarkReportFormat format,
                                                  String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new BenchmarkReportArtifact(
            artifactKey,
            artifactKind,
            format,
            buildFileName(reportId, artifactKind, format),
            format.getContentType(),
            Integer.valueOf(bytes.length),
            sha256(bytes),
            null,
            null,
            null,
            content
        );
    }

    private String buildFileName(String reportId, BenchmarkReportArtifactKind artifactKind, BenchmarkReportFormat format) {
        if (artifactKind == BenchmarkReportArtifactKind.RAW_DATA_SNAPSHOT) {
            return "benchmark-raw-data-" + reportId + "." + format.getFileExtension();
        }
        return "benchmark-report-" + reportId + "." + format.getFileExtension();
    }

    private String renderPdf(BenchmarkReportResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("%PDF-1.4\n");
        builder.append("% SQLForge 压测导出快照\n");
        builder.append("SQLForge 压测报告\n");
        builder.append("reportId=").append(response.getReportId()).append('\n');
        builder.append("taskId=").append(response.getTaskId()).append('\n');
        builder.append("taskType=").append(response.getTaskType()).append('\n');
        builder.append("verdict=").append(response.getVerdict()).append('\n');
        builder.append("targetEngines=").append(response.getTargetEngines()).append('\n');
        builder.append("generatedAt=").append(response.getGeneratedAt()).append('\n');
        appendScaleReadinessSummary(builder, response);
        appendEngineSummary(builder, response.getEngineResults());
        appendThresholdSummary(builder, response.getThresholdAssessments());
        appendRecommendationSummary(builder, response.getRecommendations());
        builder.append("%%EOF\n");
        return builder.toString();
    }

    private String renderHtml(BenchmarkReportResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        builder.append("<title>SQLForge 压测报告</title>");
        builder.append("<style>");
        builder.append("body{font-family:Helvetica,Arial,sans-serif;margin:32px;color:#1f2937;background:#f8fafc;}");
        builder.append("section{background:#ffffff;border:1px solid #dbe3ee;border-radius:12px;padding:20px;margin-bottom:20px;}");
        builder.append("table{width:100%;border-collapse:collapse;}th,td{padding:8px 10px;border-bottom:1px solid #e5e7eb;text-align:left;}");
        builder.append("h1,h2{margin-top:0;} .meta{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;}");
        builder.append("</style></head><body>");
        builder.append("<h1>SQLForge 压测报告</h1>");
        builder.append("<section><div class=\"meta\">");
        appendHtmlMeta(builder, "报告 ID", response.getReportId());
        appendHtmlMeta(builder, "任务 ID", response.getTaskId());
        appendHtmlMeta(builder, "任务类型", String.valueOf(response.getTaskType()));
        appendHtmlMeta(builder, "结论", String.valueOf(response.getVerdict()));
        appendHtmlMeta(builder, "生成时间", String.valueOf(response.getGeneratedAt()));
        appendHtmlMeta(builder, "目标引擎", String.valueOf(response.getTargetEngines()));
        builder.append("</div></section>");
        appendHtmlScaleReadiness(builder, response);
        appendHtmlEngineTable(builder, response.getEngineResults());
        appendHtmlThresholdTable(builder, response.getThresholdAssessments());
        appendHtmlTrendCharts(builder, response);
        appendHtmlRecommendationList(builder, response.getRecommendations());
        builder.append("</body></html>");
        return builder.toString();
    }

    private void appendEngineSummary(StringBuilder builder, List<BenchmarkEngineMetricVO> engineResults) {
        for (BenchmarkEngineMetricVO engineResult : engineResults) {
            builder.append("引擎=")
                .append(engineResult.getEngine())
                .append("，实际 QPS=")
                .append(engineResult.getActualQps())
                .append("，P99 延迟毫秒=")
                .append(engineResult.getP99LatencyMs())
                .append("，结论=")
                .append(engineResult.getVerdict())
                .append('\n');
        }
    }

    private void appendScaleReadinessSummary(StringBuilder builder, BenchmarkReportResponse response) {
        if (response.getScaleReadiness() == null) {
            return;
        }
        builder.append("scaleReadiness=")
            .append(response.getScaleReadiness().getReadinessStatus())
            .append(", workloadEvidence=")
            .append(response.getScaleReadiness().getWorkloadEvidenceStatus())
            .append(", productionEvidence=")
            .append(productionEvidenceSummary(response))
            .append(", missing=")
            .append(response.getScaleReadiness().getMissingEvidence())
            .append('\n');
    }

    private void appendThresholdSummary(StringBuilder builder,
                                        List<BenchmarkThresholdAssessmentVO> thresholdAssessments) {
        for (BenchmarkThresholdAssessmentVO assessment : thresholdAssessments) {
            builder.append("阈值=")
                .append(assessment.getMetric())
                .append("，实际值=")
                .append(assessment.getActualValue())
                .append("，目标值=")
                .append(assessment.getTargetValue())
                .append("，结论=")
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
        builder.append("<section><h2>引擎对比</h2><table><thead><tr>");
        builder.append("<th>引擎</th><th>QPS</th><th>P50</th><th>P99</th><th>CPU</th><th>内存</th><th>结论</th>");
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

    private void appendHtmlScaleReadiness(StringBuilder builder, BenchmarkReportResponse response) {
        if (response.getScaleReadiness() == null) {
            return;
        }
        builder.append("<section><h2>规模就绪</h2><div class=\"meta\">");
        appendHtmlMeta(builder, "状态", String.valueOf(response.getScaleReadiness().getReadinessStatus()));
        appendHtmlMeta(builder, "Workload 证据", response.getScaleReadiness().getWorkloadEvidenceStatus());
        appendHtmlMeta(builder, "生产证据", productionEvidenceSummary(response));
        appendHtmlMeta(builder, "投影日容量", String.valueOf(response.getScaleReadiness().getProjectedDailyQueryCapacity()));
        appendHtmlMeta(builder, "资源单元/百万查询", String.valueOf(response.getScaleReadiness().getEstimatedResourceUnitPerMillionQueries()));
        appendHtmlMeta(builder, "缺失证据", String.valueOf(response.getScaleReadiness().getMissingEvidence()));
        builder.append("</div></section>");
    }

    private String productionEvidenceSummary(BenchmarkReportResponse response) {
        BenchmarkScaleEvidenceManifest evidenceManifest = response.getScaleReadiness() == null
            || response.getScaleReadiness().getScaleTarget() == null
            ? null
            : response.getScaleReadiness().getScaleTarget().getEvidenceManifest();
        if (evidenceManifest == null) {
            return "MISSING";
        }
        return "source=" + evidenceManifest.getEvidenceSource()
            + ",verification=" + evidenceManifest.getExternalVerificationStatus()
            + ",concurrencyRef=" + evidenceManifest.getConcurrencyProofRef()
            + ",dataLayoutRef=" + evidenceManifest.getDataLayoutProofRef()
            + ",replayRef=" + evidenceManifest.getWorkloadReplayProofRef()
            + ",metricRef=" + evidenceManifest.getP95P99MetricProofRef()
            + ",scanCpuQueueRef=" + evidenceManifest.getScanCpuQueueMetricProofRef()
            + ",costBillRef=" + evidenceManifest.getCostBillProofRef();
    }

    private void appendHtmlThresholdTable(StringBuilder builder,
                                          List<BenchmarkThresholdAssessmentVO> thresholdAssessments) {
        builder.append("<section><h2>阈值评估</h2><table><thead><tr>");
        builder.append("<th>指标</th><th>实际值</th><th>目标值</th><th>结论</th><th>摘要</th>");
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
        builder.append("<section><h2>趋势图表</h2>");
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
        builder.append("<section><h2>建议</h2><ul>");
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
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }
}
