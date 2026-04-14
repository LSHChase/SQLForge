package com.sqlforge.backend.service;

import com.sqlforge.backend.model.TenantProfile;
import com.sqlforge.backend.repository.WorkflowBaselineRepository;
import com.sqlforge.backend.web.dto.BiReleaseRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BiReleaseWorkflowService {

    private final WorkflowBaselineRepository baselineRepository;
    private final TenantProfileProvider tenantProfileProvider;
    private final BenchmarkExecutorRegistry benchmarkExecutorRegistry;

    @Autowired
    public BiReleaseWorkflowService(
        WorkflowBaselineRepository baselineRepository,
        TenantProfileProvider tenantProfileProvider,
        BenchmarkExecutorRegistry benchmarkExecutorRegistry
    ) {
        this.baselineRepository = baselineRepository;
        this.tenantProfileProvider = tenantProfileProvider;
        this.benchmarkExecutorRegistry = benchmarkExecutorRegistry;
    }

    BiReleaseWorkflowService(WorkflowBaselineRepository baselineRepository) {
        this(
            baselineRepository,
            new StaticTenantProfileProvider(),
            new BenchmarkExecutorRegistry(java.util.Arrays.<BenchmarkExecutorProvider>asList(new DryRunBenchmarkExecutorProvider()))
        );
    }

    public Map<String, Object> execute(BiReleaseRequest request) {
        Map<String, Object> assessment = assessSql(request);
        Map<String, Object> tenant = castMap(assessment.get("tenant"));
        Map<String, Object> assessmentData = castMap(assessment.get("assessment"));
        String fingerprint = String.valueOf(assessmentData.get("fingerprint"));
        Map<String, Object> previousBaseline = baselineRepository.get(fingerprint);
        Map<String, Object> benchmarkPlan = createBenchmarkPlan(request, assessment);
        Map<String, Object> benchmarkAnalysis = analyzeBenchmark(request, assessment, previousBaseline);
        Map<String, Object> executorPlan = benchmarkExecutorRegistry.buildExecutionPlan(request, assessment, benchmarkPlan, benchmarkAnalysis);
        Map<String, Object> report = buildSqlPerformanceReport(assessment, benchmarkPlan, benchmarkAnalysis, executorPlan, tenant);

        int riskRank = riskRank(String.valueOf(assessment.get("riskLevel")));
        Map<String, Object> sanitized = castMap(benchmarkAnalysis.get("sanitized"));
        Map<String, Object> summary = castMap(sanitized.get("summary"));
        double p99Ms = asDouble(summary.get("p99Ms"), 0);

        String decision;
        if (riskRank >= riskRank("critical")) {
            decision = "blocked";
        } else if (p99Ms <= asDouble(request.getSlaMs(), 5000)) {
            decision = "approved";
        } else {
            decision = "needs-optimization";
        }

        baselineRepository.save(fingerprint, String.valueOf(tenant.get("id")), decision, summary);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("workflow", "bi-release-evaluation");
        result.put("decision", decision);
        result.put("sqlAssessment", assessment);
        result.put("benchmarkPlan", benchmarkPlan);
        result.put("benchmarkAnalysis", benchmarkAnalysis);
        result.put("executorPlan", executorPlan);
        result.put("previousBaseline", previousBaseline);
        result.put("report", report);
        return result;
    }

    private Map<String, Object> assessSql(BiReleaseRequest request) {
        Map<String, Object> tenant = tenantProfileProvider.resolve(request.getTenantId()).toMap();
        Map<String, Object> assessment = buildAssessment(request.getSql());
        String riskLevel = buildRiskLevel(castList(assessment.get("risks")));
        int targetConcurrency = request.getTargetConcurrency() == null ? 20 : request.getTargetConcurrency().intValue();

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("tenant", tenant);
        result.put("targetConcurrency", targetConcurrency);
        result.put("riskLevel", riskLevel);
        result.put("requiresBenchmark", Boolean.valueOf(riskRank(riskLevel) >= riskRank("high")));
        result.put("assessment", assessment);
        return result;
    }

    private Map<String, Object> createBenchmarkPlan(BiReleaseRequest request, Map<String, Object> assessmentResult) {
        Map<String, Object> assessment = castMap(assessmentResult.get("assessment"));
        List<String> tables = castStringList(assessment.get("tables"));
        Map<String, Object> samplingPlan = buildSamplingPlan(tables, request.getDataProfile());
        Map<String, Object> matrix = buildPressureMatrix(
            castMap(assessment.get("labels")),
            asInt(assessmentResult.get("targetConcurrency"), 20),
            request.getDataProfile(),
            castMap(assessmentResult.get("tenant"))
        );
        Map<String, Object> projectedFullScale = buildExtrapolation(request);

        Map<String, Object> result = new LinkedHashMap<String, Object>(assessmentResult);
        result.put("samplingPlan", samplingPlan);
        result.put("matrix", matrix);
        result.put("projectedFullScale", projectedFullScale);
        return result;
    }

    private Map<String, Object> analyzeBenchmark(
        BiReleaseRequest request,
        Map<String, Object> assessmentResult,
        Map<String, Object> previousBaseline
    ) {
        List<Map<String, Object>> metrics = request.getMetrics() == null || request.getMetrics().isEmpty()
            ? buildSyntheticMetrics(
                asInt(assessmentResult.get("targetConcurrency"), 20),
                String.valueOf(assessmentResult.get("riskLevel"))
            )
            : normalizeMetrics(request.getMetrics());

        Map<String, Object> assessment = castMap(assessmentResult.get("assessment"));
        Map<String, Object> labels = castMap(assessment.get("labels"));
        Map<String, Object> resourceBreakdown = request.getResourceBreakdown() == null
            ? buildDefaultResourceBreakdown(assessmentResult, labels, request)
            : request.getResourceBreakdown();

        List<Map<String, Object>> queryMix = request.getQueryMix() == null || request.getQueryMix().isEmpty()
            ? buildDefaultQueryMix(assessment, assessmentResult)
            : normalizeQueryMix(request.getQueryMix());

        Map<String, Object> sanitized = sanitize(metrics);
        Map<String, Object> bottleneck = detectBottleneck(resourceBreakdown);
        Map<String, Object> estimatedResources = estimateResources(
            queryMix,
            asInt(assessmentResult.get("targetConcurrency"), 20),
            asDouble(request.getDataProfile() == null ? null : request.getDataProfile().getHotDataGb(), 200)
        );
        Map<String, Object> baselineComparison = compareBaseline(previousBaseline, castMap(sanitized.get("summary")));
        List<Map<String, Object>> recommendations = buildRecommendations(assessment, bottleneck, baselineComparison);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sanitized", sanitized);
        result.put("bottleneck", bottleneck);
        result.put("estimatedResources", estimatedResources);
        result.put("baselineComparison", baselineComparison);
        result.put("recommendations", recommendations);
        return result;
    }

    private Map<String, Object> buildSqlPerformanceReport(
        Map<String, Object> assessmentResult,
        Map<String, Object> benchmarkPlan,
        Map<String, Object> benchmarkAnalysis,
        Map<String, Object> executorPlan,
        Map<String, Object> tenant
    ) {
        Map<String, Object> assessment = castMap(assessmentResult.get("assessment"));
        Map<String, Object> sanitized = castMap(benchmarkAnalysis.get("sanitized"));
        Map<String, Object> summary = castMap(sanitized.get("summary"));
        double p99 = asDouble(summary.get("p99Ms"), 0);

        Map<String, Object> reportSummary = new LinkedHashMap<String, Object>();
        reportSummary.put("decision", p99 <= 5000 ? "approve" : "optimize");
        reportSummary.put("riskLevel", benchmarkPlan.get("riskLevel"));
        reportSummary.put("tables", assessment.get("tables"));
        reportSummary.put("p99Ms", p99);

        Map<String, Object> benchmarkPlanSection = new LinkedHashMap<String, Object>();
        benchmarkPlanSection.put("targetConcurrency", benchmarkPlan.get("targetConcurrency"));
        benchmarkPlanSection.put("samplingPlan", benchmarkPlan.get("samplingPlan"));
        benchmarkPlanSection.put("matrix", benchmarkPlan.get("matrix"));
        benchmarkPlanSection.put("projectedFullScale", benchmarkPlan.get("projectedFullScale"));

        Map<String, Object> benchmarkAnalysisSection = new LinkedHashMap<String, Object>(benchmarkAnalysis);
        benchmarkAnalysisSection.put("baselineComparison", benchmarkAnalysis.get("baselineComparison"));
        benchmarkAnalysisSection.put("recommendations", benchmarkAnalysis.get("recommendations"));

        Map<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("reportType", "sql-performance");
        report.put("tenant", tenant.get("id"));
        report.put("fingerprint", assessment.get("fingerprint"));
        report.put("summary", reportSummary);
        report.put("validations", assessment.get("validations"));
        report.put("assessment", assessment);
        report.put("benchmarkPlan", benchmarkPlanSection);
        report.put("benchmarkAnalysis", benchmarkAnalysisSection);
        report.put("executorPlan", executorPlan);
        report.put("actions", benchmarkAnalysis.get("recommendations"));
        return report;
    }

    private Map<String, Object> buildAssessment(String sql) {
        String normalizedSql = normalizeSql(sql);
        List<String> tableReferences = detectTableReferences(normalizedSql);
        List<String> tables = unique(tableReferences);
        int joinCount = countMatches(normalizedSql.toLowerCase(Locale.ROOT), " join ");
        boolean hasOrderBy = normalizedSql.toLowerCase(Locale.ROOT).contains(" order by ");
        boolean hasSelectStar = normalizedSql.toLowerCase(Locale.ROOT).contains("select *");
        boolean hasLimit = normalizedSql.toLowerCase(Locale.ROOT).contains(" limit ");
        boolean hasGroupBy = normalizedSql.toLowerCase(Locale.ROOT).contains(" group by ");
        String scanPattern = detectScanPattern(normalizedSql);
        String joinType = detectJoinType(joinCount);
        String computeDensity = detectComputeDensity(normalizedSql);
        String resultSetRisk = detectResultSetRisk(hasLimit, hasGroupBy, normalizedSql);
        List<Map<String, Object>> repeatedTables = detectRepeatedTables(tableReferences);
        List<Map<String, Object>> repeatedExpressions = detectRepeatedExpressions(normalizedSql);

        Map<String, Object> labels = new LinkedHashMap<String, Object>();
        labels.put("scanPattern", scanPattern);
        labels.put("joinType", joinType);
        labels.put("computeDensity", computeDensity);
        labels.put("resourceType", detectResourceType(scanPattern, joinCount, computeDensity, hasOrderBy));
        labels.put("resultSetRisk", resultSetRisk);

        Map<String, Object> validations = buildValidations(
            normalizedSql,
            scanPattern,
            hasOrderBy,
            hasSelectStar,
            hasLimit,
            resultSetRisk,
            repeatedTables,
            repeatedExpressions
        );

        List<Map<String, Object>> risks = buildRisks(
            normalizedSql,
            scanPattern,
            joinCount,
            hasOrderBy,
            hasSelectStar,
            tables,
            computeDensity,
            repeatedTables,
            repeatedExpressions,
            resultSetRisk
        );

        Map<String, Object> resourceEstimate = new LinkedHashMap<String, Object>();
        resourceEstimate.put("score", estimateResourceScore(scanPattern, joinCount, computeDensity, tables.size(), risks.size()));

        Map<String, Object> assessment = new LinkedHashMap<String, Object>();
        assessment.put("sql", normalizedSql);
        assessment.put("fingerprint", md5(normalizedSql));
        assessment.put("tables", tables);
        assessment.put("labels", labels);
        assessment.put("validations", validations);
        assessment.put("risks", risks);
        assessment.put("resourceEstimate", resourceEstimate);
        return assessment;
    }

    private String buildRiskLevel(List<Map<String, Object>> risks) {
        boolean hasCritical = false;
        boolean hasHigh = false;

        for (Map<String, Object> risk : risks) {
            String severity = String.valueOf(risk.get("severity"));

            if ("critical".equals(severity)) {
                hasCritical = true;
            } else if ("high".equals(severity)) {
                hasHigh = true;
            }
        }

        if (hasCritical) {
            return "critical";
        }

        if (hasHigh) {
            return "high";
        }

        return risks.isEmpty() ? "low" : "medium";
    }

    private Map<String, Object> buildSamplingPlan(List<String> tables, BiReleaseRequest.DataProfile dataProfile) {
        double skew = asDouble(dataProfile == null ? null : dataProfile.getSkew(), 1);
        double fullRows = asDouble(dataProfile == null ? null : dataProfile.getFullRows(), 100000000);
        double sampleRows = asDouble(
            dataProfile == null ? null : dataProfile.getSampleRows(),
            Math.max(1000000, Math.floor(fullRows * 0.02))
        );
        double baseRatio = Math.max(0.01, Math.min(0.05, sampleRows / Math.max(fullRows, 1)));
        String skewStrategy =
            skew < 2 ? "simple-random" : skew < 10 ? "stratified-hotspot-oversampling" : "hot-key-full-cover";

        List<Map<String, Object>> tablePlans = new ArrayList<Map<String, Object>>();
        for (String table : tables) {
            Map<String, Object> tablePlan = new LinkedHashMap<String, Object>();
            tablePlan.put("table", table);
            tablePlan.put("sampleRows", Math.max(100000, (long) Math.floor(sampleRows / Math.max(tables.size(), 1))));
            tablePlan.put("mode", skewStrategy);
            tablePlans.add(tablePlan);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("sampleRatio", round(Math.round(baseRatio * 10000.0d) / 10000.0d));
        result.put("skew", skew);
        result.put("strategies", Arrays.asList(
            "retain-all-partition-keys",
            skewStrategy,
            "preserve-foreign-key-integrity",
            "preserve-commit-timeline-order"
        ));
        result.put("tablePlans", tablePlans);
        return result;
    }

    private Map<String, Object> buildPressureMatrix(
        Map<String, Object> labels,
        int targetConcurrency,
        BiReleaseRequest.DataProfile dataProfile,
        Map<String, Object> tenant
    ) {
        List<Integer> ladderSeeds = Arrays.asList(1, 5, 10, 20, 50, 100);
        List<Integer> ladder = new ArrayList<Integer>();

        for (Integer seed : ladderSeeds) {
            if (seed.intValue() <= targetConcurrency) {
                ladder.add(seed);
            }
        }

        if (!ladder.contains(Integer.valueOf(targetConcurrency))) {
            ladder.add(Integer.valueOf(targetConcurrency));
        }

        Collections.sort(ladder);
        boolean hasJoin = !"single-table".equals(String.valueOf(labels.get("joinType")));
        double skew = asDouble(dataProfile == null ? null : dataProfile.getSkew(), 1);
        String scanScale = "full-table".equals(String.valueOf(labels.get("scanPattern")))
            ? "full-check-required"
            : "sample-first";

        List<Map<String, Object>> concurrencyMatrix = new ArrayList<Map<String, Object>>();
        for (Integer concurrency : ladder) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            int tenantMax = asInt(tenant.get("maxConcurrency"), 1);
            item.put("concurrency", concurrency);
            item.put("expectedMode", concurrency.intValue() >= tenantMax ? "queue-risk" : "steady");
            item.put("queueRisk", round(((double) concurrency.intValue()) / Math.max(tenantMax, 1)));
            concurrencyMatrix.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("dataScaleMatrix", Arrays.asList(
            mapOf("name", "sample", "purpose", "快速反馈与上线前评估"),
            mapOf("name", "medium", "purpose", "验证分区裁剪与统计信息稳定性"),
            mapOf(
                "name",
                "full",
                "purpose",
                "full-check-required".equals(scanScale) ? "高风险 SQL 需审慎验证全量行为" : "关键 SQL 的月度全量验证"
            )
        ));
        result.put("concurrencyMatrix", concurrencyMatrix);
        result.put("distributionMatrix", Arrays.asList(
            mapOf("name", "uniform-baseline", "purpose", "理想基线"),
            mapOf("name", "production-like", "purpose", "贴近生产分布"),
            mapOf("name", skew >= 10 ? "extreme-skew" : "skew-aware", "purpose", "验证倾斜场景")
        ));
        result.put(
            "executionPlanMatrix",
            hasJoin
                ? Arrays.asList("broadcast", "shuffle", "repartition", "stats-aging-scan", "cbo-parameter-scan")
                : Arrays.asList("default-plan", "predicate-pushdown-check")
        );
        result.put("resourceConstraintMatrix", Arrays.asList("memory-gradient", "cpu-gradient", "io-latency-injection"));
        result.put(
            "queryStructureMatrix",
            Arrays.asList(
                "predicate-pushdown-validation",
                "bucket-join-validation",
                "expression-reuse-validation",
                "repeated-join-validation",
                "sort-elimination-validation",
                "projection-pushdown-validation"
            )
        );
        result.put("advancedFlags", mapOf("tenantIsolation", Boolean.TRUE, "shadowTestingReady", Boolean.TRUE));
        result.put(
            "turningPointRule",
            mapOf("method", "latency-curvature-or-tps-plateau", "hint", "当延迟增长曲率显著上升或 TPS 进入平台期时，判定吞吐拐点")
        );
        return result;
    }

    private Map<String, Object> buildExtrapolation(BiReleaseRequest request) {
        double sampleLatencyMs = asDouble(request.getSampleLatencyMs(), 1200);
        double fullRows = asDouble(request.getDataProfile() == null ? null : request.getDataProfile().getFullRows(), 100000000);
        double sampleRows = asDouble(request.getDataProfile() == null ? null : request.getDataProfile().getSampleRows(), 2000000);
        double skew = asDouble(request.getDataProfile() == null ? null : request.getDataProfile().getSkew(), 1);
        double alpha = 0.9;
        double beta = 0.3;
        double ratio = Math.max(1, fullRows / Math.max(sampleRows, 1));
        double fullLatencyMs = sampleLatencyMs * (1 + alpha * Math.log(ratio) + beta * Math.sqrt(Math.max(skew, 1)));
        double errorBand = Math.max(0.08, Math.min(0.35, 0.1 + skew * 0.01));

        Map<String, Object> confidence95 = new LinkedHashMap<String, Object>();
        confidence95.put("lowerMs", round(fullLatencyMs * (1 - errorBand)));
        confidence95.put("upperMs", round(fullLatencyMs * (1 + errorBand)));

        Map<String, Object> model = new LinkedHashMap<String, Object>();
        model.put("alpha", alpha);
        model.put("beta", beta);
        model.put("ratio", round(ratio));
        model.put("skew", skew);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("projectedLatencyMs", round(fullLatencyMs));
        result.put("confidence95", confidence95);
        result.put("model", model);
        return result;
    }

    private Map<String, Object> sanitize(List<Map<String, Object>> metrics) {
        List<Map<String, Object>> warmupDiscarded = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> stable = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> anomalies = new ArrayList<Map<String, Object>>();

        for (int index = 0; index < metrics.size(); index += 1) {
            if (index < 3) {
                warmupDiscarded.add(metrics.get(index));
            } else {
                stable.add(metrics.get(index));
            }
        }

        List<Double> durations = new ArrayList<Double>();
        for (Map<String, Object> item : stable) {
            durations.add(Double.valueOf(asDouble(item.get("latencyMs"), 0)));
        }

        double q1 = quantile(durations, 0.25);
        double q3 = quantile(durations, 0.75);
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        List<Map<String, Object>> cleaned = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> metric : stable) {
            double latency = asDouble(metric.get("latencyMs"), 0);
            double gcPauseMs = asDouble(metric.get("gcPauseMs"), 0);
            boolean isOutlier = latency < lowerBound || latency > upperBound;

            if (gcPauseMs > 100) {
                anomalies.add(mapOf("type", "gc-pause", "metric", metric));
            }

            if (isOutlier) {
                anomalies.add(mapOf("type", "iqr-outlier", "metric", metric));
            } else {
                cleaned.add(metric);
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("warmupDiscarded", warmupDiscarded);
        result.put("cleaned", cleaned);
        result.put("anomalies", anomalies);
        result.put("summary", summarizeDurations(cleaned));
        return result;
    }

    private Map<String, Object> detectBottleneck(Map<String, Object> resourceBreakdown) {
        double scan = asDouble(resourceBreakdown.get("scanRatio"), 0);
        double shuffle = asDouble(resourceBreakdown.get("shuffleRatio"), 0);
        double compute = asDouble(resourceBreakdown.get("computeRatio"), 0);
        double network = asDouble(resourceBreakdown.get("networkRatio"), 0);
        double waiting = asDouble(resourceBreakdown.get("waitingRatio"), 0);

        if (scan > 0.6) {
            return mapOf(
                "type",
                "io-bottleneck",
                "reason",
                "Scan 占比过高，优先检查分区裁剪、列裁剪和小文件问题",
                "suggestions",
                Arrays.asList("优化分区键", "合并小文件", "开启列式索引或数据跳过能力")
            );
        }

        if (shuffle > 0.3) {
            return mapOf(
                "type",
                "distribution-bottleneck",
                "reason",
                "Shuffle 占比过高，优先检查 Join Key 倾斜和分桶策略",
                "suggestions",
                Arrays.asList("增加分桶", "调整 Join Distribution Type", "引入热点 Key 专项优化")
            );
        }

        if (compute > 0.5) {
            return mapOf(
                "type",
                "cpu-bottleneck",
                "reason",
                "Compute 占比过高，优先检查复杂表达式、窗口函数和排序",
                "suggestions",
                Arrays.asList("预计算", "物化视图", "移除非必要排序")
            );
        }

        if (network > 0.2) {
            return mapOf(
                "type",
                "network-bottleneck",
                "reason",
                "网络传输占比较高，需控制结果集规模并检查跨区域访问",
                "suggestions",
                Arrays.asList("分页查询", "聚合下推", "结果压缩")
            );
        }

        if (waiting > 0.2) {
            return mapOf(
                "type",
                "scheduling-bottleneck",
                "reason",
                "等待时间偏高，说明已接近队列或资源池上限",
                "suggestions",
                Arrays.asList("扩容 Worker", "优化并行度", "调整租户队列策略")
            );
        }

        return mapOf(
            "type",
            "balanced",
            "reason",
            "未检测到单一主导瓶颈",
            "suggestions",
            Arrays.asList("继续扩大并发压测范围", "观察计划稳定性")
        );
    }

    private Map<String, Object> estimateResources(List<Map<String, Object>> queryMix, int targetConcurrency, double hotDataGb) {
        double cpuCoreDemand = 0;
        double executionMemoryGb = 0;

        for (Map<String, Object> query : queryMix) {
            cpuCoreDemand += (asDouble(query.get("cpuTimeSec"), 0) * asDouble(query.get("arrivalRate"), 0)) / (3600 * 0.7);
            executionMemoryGb += asDouble(query.get("memoryPeakGb"), 0);
        }

        double memoryGb = executionMemoryGb * Math.max(1, targetConcurrency * 0.15) * 1.5 + hotDataGb * 0.1 + 32;
        double diskTb = hotDataGb * 0.002 + memoryGb / 512 + 0.1;
        double networkGbps = Math.max(10, round((hotDataGb * 8 * 1.5) / 1024));

        return mapOf(
            "cpuCores",
            Integer.valueOf((int) Math.ceil(cpuCoreDemand)),
            "memoryGb",
            Integer.valueOf((int) Math.ceil(memoryGb)),
            "diskTb",
            round(diskTb),
            "networkGbps",
            networkGbps
        );
    }

    private Map<String, Object> compareBaseline(Map<String, Object> previousBaseline, Map<String, Object> currentSummary) {
        if (previousBaseline == null || !previousBaseline.containsKey("summary")) {
            return mapOf(
                "hasBaseline",
                Boolean.FALSE,
                "driftRatio",
                0,
                "status",
                "new-baseline",
                "message",
                "暂无历史基线，当前结果将作为新基线"
            );
        }

        Map<String, Object> previousSummary = castMap(previousBaseline.get("summary"));
        double previousP99 = asDouble(previousSummary.get("p99Ms"), 0);
        double currentP99 = asDouble(currentSummary.get("p99Ms"), 0);
        double driftRatio = previousP99 == 0 ? 0 : (currentP99 - previousP99) / previousP99;
        String status = driftRatio > 0.2 ? "regressed" : driftRatio < -0.1 ? "improved" : "stable";
        String message = driftRatio > 0.2
            ? "当前 P99 相比历史基线明显劣化"
            : driftRatio < -0.1
                ? "当前 P99 相比历史基线明显改善"
                : "当前结果与历史基线基本一致";

        return mapOf(
            "hasBaseline",
            Boolean.TRUE,
            "previousP99Ms",
            previousP99,
            "currentP99Ms",
            currentP99,
            "driftRatio",
            round(driftRatio),
            "status",
            status,
            "message",
            message
        );
    }

    private List<Map<String, Object>> buildRecommendations(
        Map<String, Object> assessment,
        Map<String, Object> bottleneck,
        Map<String, Object> baselineComparison
    ) {
        Map<String, Object> labels = castMap(assessment.get("labels"));
        Map<String, Object> validations = castMap(assessment.get("validations"));
        List<Map<String, Object>> recommendations = new ArrayList<Map<String, Object>>();

        if ("full-table".equals(String.valueOf(labels.get("scanPattern")))
            || "warn".equals(String.valueOf(castMap(validations.get("predicatePushdown")).get("status")))) {
            recommendations.add(recommendation(
                "high",
                "优化分区与谓词下推",
                "当前 SQL 存在全表扫描或谓词下推不稳风险，应优先检查分区键与过滤表达式写法",
                "降低 Scan 占比并显著缩短 P95/P99",
                "sql-or-table-design"
            ));
        }

        if ("distribution-bottleneck".equals(String.valueOf(bottleneck.get("type")))
            || "opportunity".equals(String.valueOf(castMap(validations.get("bucketJoin")).get("status")))) {
            recommendations.add(recommendation(
                "high",
                "验证分桶或 Broadcast 策略",
                "Join 具备优化空间，应比较 Broadcast、Shuffle、Bucket Join 的真实收益",
                "减少 Shuffle 和网络放大",
                "execution-plan"
            ));
        }

        if ("warn".equals(String.valueOf(castMap(validations.get("sortNecessity")).get("status")))) {
            recommendations.add(recommendation(
                "medium",
                "移除非必要排序",
                "ORDER BY 未配合 LIMIT，可能触发全局排序与额外内存开销",
                "降低 CPU 与内存峰值",
                "sql-rewrite"
            ));
        }

        if ("warn".equals(String.valueOf(castMap(validations.get("repeatedJoin")).get("status")))) {
            recommendations.add(recommendation(
                "medium",
                "物化重复关联",
                "检测到同表多次关联，可考虑使用 CTE、临时表或预聚合来减少重复扫描",
                "降低 Join 链复杂度与计划脆弱性",
                "sql-rewrite"
            ));
        }

        if ("warn".equals(String.valueOf(castMap(validations.get("expressionReuse")).get("status")))) {
            recommendations.add(recommendation(
                "medium",
                "复用重复表达式",
                "存在重复函数表达式，应考虑复用别名、CTE 或预计算",
                "降低 CPU 计算浪费",
                "sql-rewrite"
            ));
        }

        if ("warn".equals(String.valueOf(castMap(validations.get("projectionPushdown")).get("status")))) {
            recommendations.add(recommendation(
                "medium",
                "收缩投影列",
                "SELECT * 会放大扫描列数与结果传输量，应仅保留必要列",
                "提升投影下推效果，降低扫描和网络开销",
                "sql-rewrite"
            ));
        }

        if ("warn".equals(String.valueOf(castMap(validations.get("resultSetControl")).get("status")))) {
            recommendations.add(recommendation(
                "medium",
                "控制结果集规模",
                "结果集缺少边界，建议分页、预聚合或下推聚合",
                "降低客户端和网络瓶颈",
                "query-pattern"
            ));
        }

        if ("regressed".equals(String.valueOf(baselineComparison.get("status")))) {
            recommendations.add(recommendation(
                "high",
                "排查基线漂移与计划变化",
                "当前结果相对历史基线有明显劣化，需要验证统计信息、数据分布和执行计划是否发生变化",
                "尽快恢复已知稳定表现",
                "baseline-guard"
            ));
        }

        if (recommendations.isEmpty()) {
            recommendations.add(recommendation(
                "low",
                "继续观察并扩展样本",
                "当前没有明显单一缺陷，可继续扩大样本或并发梯度验证稳态表现",
                "提高结论置信度",
                "observe"
            ));
        }

        Collections.sort(recommendations, (left, right) -> priorityRank(String.valueOf(left.get("priority")))
            - priorityRank(String.valueOf(right.get("priority"))));
        return recommendations;
    }

    private Map<String, Object> buildDefaultResourceBreakdown(
        Map<String, Object> assessmentResult,
        Map<String, Object> labels,
        BiReleaseRequest request
    ) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("scanRatio", "io-bound".equals(String.valueOf(labels.get("resourceType"))) ? 0.65 : 0.35);
        result.put("shuffleRatio", "single-table".equals(String.valueOf(labels.get("joinType"))) ? 0.1 : 0.28);
        result.put("computeRatio", "light".equals(String.valueOf(labels.get("computeDensity"))) ? 0.2 : 0.38);
        result.put("networkRatio", 0.08);
        result.put(
            "waitingRatio",
            asInt(assessmentResult.get("targetConcurrency"), 20) > asInt(castMap(assessmentResult.get("tenant")).get("maxConcurrency"), 20)
                ? 0.25
                : 0.12
        );
        return result;
    }

    private List<Map<String, Object>> buildDefaultQueryMix(Map<String, Object> assessment, Map<String, Object> assessmentResult) {
        double score = asDouble(castMap(assessment.get("resourceEstimate")).get("score"), 0);
        Map<String, Object> labels = castMap(assessment.get("labels"));
        return Arrays.asList(
            mapOf(
                "name",
                "bi-release-query",
                "cpuTimeSec",
                score / 15,
                "arrivalRate",
                Math.max(60, asInt(assessmentResult.get("targetConcurrency"), 20) * 18),
                "memoryPeakGb",
                "single-table".equals(String.valueOf(labels.get("joinType"))) ? 2 : 6
            )
        );
    }

    private List<Map<String, Object>> buildSyntheticMetrics(int targetConcurrency, String riskLevel) {
        int baseLatency = "high".equals(riskLevel) ? 3200 : "medium".equals(riskLevel) ? 2200 : 1400;
        int pressurePenalty = Math.max(0, targetConcurrency - 10) * 80;
        List<Map<String, Object>> metrics = new ArrayList<Map<String, Object>>();

        for (int index = 0; index < 10; index += 1) {
            metrics.add(mapOf(
                "run",
                Integer.valueOf(index + 1),
                "latencyMs",
                Integer.valueOf(baseLatency + pressurePenalty + index * 120),
                "gcPauseMs",
                Integer.valueOf(index == 4 ? 120 : 20)
            ));
        }

        return metrics;
    }

    private List<Map<String, Object>> normalizeMetrics(List<BiReleaseRequest.Metric> metrics) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (BiReleaseRequest.Metric metric : metrics) {
            result.add(mapOf(
                "run",
                metric.getRun(),
                "latencyMs",
                metric.getLatencyMs(),
                "gcPauseMs",
                metric.getGcPauseMs()
            ));
        }

        return result;
    }

    private List<Map<String, Object>> normalizeQueryMix(List<BiReleaseRequest.QueryMixItem> items) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();

        for (BiReleaseRequest.QueryMixItem item : items) {
            result.add(mapOf(
                "name",
                item.getName(),
                "cpuTimeSec",
                item.getCpuTimeSec(),
                "arrivalRate",
                item.getArrivalRate(),
                "memoryPeakGb",
                item.getMemoryPeakGb()
            ));
        }

        return result;
    }

    private Map<String, Object> buildValidations(
        String sql,
        String scanPattern,
        boolean hasOrderBy,
        boolean hasSelectStar,
        boolean hasLimit,
        String resultSetRisk,
        List<Map<String, Object>> repeatedTables,
        List<Map<String, Object>> repeatedExpressions
    ) {
        boolean predicatePushdownRisk = "full-table".equals(scanPattern) || sql.toLowerCase(Locale.ROOT).contains(" where lower(");
        Map<String, Object> validations = new LinkedHashMap<String, Object>();
        validations.put(
            "predicatePushdown",
            mapOf(
                "status",
                predicatePushdownRisk ? "warn" : "pass",
                "message",
                predicatePushdownRisk ? "过滤条件可能无法有效下推，需要验证分区裁剪与表达式包裹" : "过滤条件具备较好的下推机会"
            )
        );
        validations.put(
            "bucketJoin",
            mapOf(
                "status",
                sql.toLowerCase(Locale.ROOT).contains(" join ") ? "opportunity" : "na",
                "message",
                sql.toLowerCase(Locale.ROOT).contains(" join ") ? "存在相同 Join Key，可验证 Bucket Join 与 Broadcast 策略" : "当前 SQL 无 Join，不涉及分桶 Join"
            )
        );
        validations.put(
            "expressionReuse",
            mapOf(
                "status",
                repeatedExpressions.isEmpty() ? "pass" : "warn",
                "message",
                repeatedExpressions.isEmpty() ? "未检测到明显重复表达式" : "检测到重复函数表达式，可考虑复用或物化"
            )
        );
        validations.put(
            "repeatedJoin",
            mapOf(
                "status",
                repeatedTables.isEmpty() ? "pass" : "warn",
                "message",
                repeatedTables.isEmpty() ? "未检测到重复关联同表" : "同表被多次关联，可考虑 CTE 或临时表物化"
            )
        );
        validations.put(
            "sortNecessity",
            mapOf(
                "status",
                hasOrderBy && !hasLimit ? "warn" : hasOrderBy ? "observe" : "pass",
                "message",
                hasOrderBy && !hasLimit ? "ORDER BY 未配合 LIMIT，需验证是否为非必要排序" : hasOrderBy ? "存在 ORDER BY，但结果集有界，建议结合业务语义确认必要性" : "未检测到显著排序风险"
            )
        );
        validations.put(
            "projectionPushdown",
            mapOf(
                "status",
                hasSelectStar ? "warn" : "pass",
                "message",
                hasSelectStar ? "SELECT * 可能导致列裁剪不足" : "查询具备较好的投影下推条件"
            )
        );
        validations.put(
            "resultSetControl",
            mapOf(
                "status",
                "wide-open".equals(resultSetRisk) ? "warn" : "pass",
                "message",
                "wide-open".equals(resultSetRisk) ? "结果集无明显边界，需防止返回过大结果" : "结果集规模具备一定控制"
            )
        );
        return validations;
    }

    private List<Map<String, Object>> buildRisks(
        String sql,
        String scanPattern,
        int joinCount,
        boolean hasOrderBy,
        boolean hasSelectStar,
        List<String> tables,
        String computeDensity,
        List<Map<String, Object>> repeatedTables,
        List<Map<String, Object>> repeatedExpressions,
        String resultSetRisk
    ) {
        List<Map<String, Object>> risks = new ArrayList<Map<String, Object>>();

        if ("full-table".equals(scanPattern)) {
            risks.add(risk("FULL_SCAN", "high", "查询缺少有效过滤条件，存在全表扫描风险"));
        }

        if (joinCount >= 2) {
            risks.add(risk("COMPLEX_JOIN", joinCount >= 4 ? "high" : "medium", "Join 链较长，存在 Shuffle 放大和计划脆弱风险"));
        }

        if (sql.toLowerCase(Locale.ROOT).contains(" join ") && !sql.toLowerCase(Locale.ROOT).contains(" on ")) {
            risks.add(risk("MISSING_JOIN_CONDITION", "critical", "疑似缺失 Join 条件，需排查 Cartesian Product"));
        }

        if (hasOrderBy && !sql.toLowerCase(Locale.ROOT).contains(" limit ")) {
            risks.add(risk("UNBOUNDED_SORT", "medium", "ORDER BY 未配合 LIMIT，可能触发非必要全局排序"));
        }

        if (hasSelectStar) {
            risks.add(risk("SELECT_STAR", "medium", "SELECT * 可能导致投影下推失效与网络放大"));
        }

        if (tables.size() >= 3) {
            risks.add(risk("MULTI_TABLE_TOUCH", "medium", "涉及多表访问，建议重点验证统计信息与分桶策略"));
        }

        if ("window".equals(computeDensity) || "udf-heavy".equals(computeDensity)) {
            risks.add(risk("HEAVY_COMPUTE", "medium", "存在窗口函数或高复杂表达式，需关注 CPU 与内存峰值"));
        }

        if (!repeatedTables.isEmpty()) {
            risks.add(risk("REPEATED_TABLE_JOIN", "medium", "检测到同表多次关联，可能需要 CTE 或物化来降低重复计算"));
        }

        if (!repeatedExpressions.isEmpty()) {
            risks.add(risk("REPEATED_EXPRESSION", "medium", "检测到重复表达式计算，可能存在 CPU 浪费"));
        }

        if ("wide-open".equals(resultSetRisk)) {
            risks.add(risk("LARGE_RESULT_SET", "medium", "查询结果无聚合或 LIMIT 约束，可能导致结果集过大"));
        }

        return risks;
    }

    private Map<String, Object> recommendation(
        String priority,
        String title,
        String rationale,
        String expectedImpact,
        String actionType
    ) {
        return mapOf(
            "priority",
            priority,
            "title",
            title,
            "rationale",
            rationale,
            "expectedImpact",
            expectedImpact,
            "actionType",
            actionType
        );
    }

    private Map<String, Object> risk(String code, String severity, String message) {
        return mapOf("code", code, "severity", severity, "message", message);
    }

    private String normalizeSql(String sql) {
        return sql == null ? "" : sql.trim().replaceAll("\\s+", " ");
    }

    private List<String> detectTableReferences(String sql) {
        List<String> references = new ArrayList<String>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b(?:from|join)\\s+([a-zA-Z0-9_.]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(sql);

        while (matcher.find()) {
            references.add(matcher.group(1));
        }

        return references;
    }

    private String detectScanPattern(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!lower.contains(" where ")) {
            return "full-table";
        }

        if (lower.contains(" between ") || lower.contains(" >") || lower.contains(" <") || lower.contains(">=") || lower.contains("<=")) {
            return "range";
        }

        return "point-lookup";
    }

    private String detectJoinType(int joinCount) {
        if (joinCount == 0) {
            return "single-table";
        }
        if (joinCount == 1) {
            return "star";
        }
        if (joinCount <= 3) {
            return "chain";
        }
        return "snowflake";
    }

    private String detectComputeDensity(String sql) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (lower.contains("over(") || lower.contains("over (")) {
            return "window";
        }
        if (lower.contains("sum(") || lower.contains("avg(") || lower.contains("max(") || lower.contains("min(") || lower.contains("count(")) {
            return "heavy-aggregation";
        }
        if (lower.contains("regexp_") || lower.contains("json_") || lower.contains("map_") || lower.contains("array_")) {
            return "udf-heavy";
        }
        return "light";
    }

    private String detectResourceType(String scanPattern, int joinCount, String computeDensity, boolean hasOrderBy) {
        if ("full-table".equals(scanPattern)) {
            return "io-bound";
        }
        if (joinCount >= 2) {
            return "network-mixed";
        }
        if (!"light".equals(computeDensity) || hasOrderBy) {
            return "cpu-bound";
        }
        return "memory-mixed";
    }

    private String detectResultSetRisk(boolean hasLimit, boolean hasGroupBy, String sql) {
        if (hasLimit) {
            return "bounded";
        }
        if (hasGroupBy || sql.toLowerCase(Locale.ROOT).contains("count(")) {
            return "aggregated";
        }
        return "wide-open";
    }

    private List<Map<String, Object>> detectRepeatedTables(List<String> tableReferences) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (String table : tableReferences) {
            Integer value = counts.get(table);
            counts.put(table, value == null ? 1 : value + 1);
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue().intValue() > 1) {
                result.add(mapOf("table", entry.getKey(), "count", entry.getValue()));
            }
        }
        return result;
    }

    private List<Map<String, Object>> detectRepeatedExpressions(String sql) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\bselect\\s+(.+?)\\s+from\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(sql);
        if (!matcher.find()) {
            return new ArrayList<Map<String, Object>>();
        }

        String[] expressions = matcher.group(1).split(",");
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();

        for (String expression : expressions) {
            String normalized = expression.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (normalized.contains("(")) {
                Integer value = counts.get(normalized);
                counts.put(normalized, value == null ? 1 : value + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue().intValue() > 1) {
                result.add(mapOf("expression", entry.getKey(), "count", entry.getValue()));
            }
        }
        return result;
    }

    private int estimateResourceScore(String scanPattern, int joinCount, String computeDensity, int tableCount, int riskCount) {
        int score = ("full-table".equals(scanPattern) ? 40 : "range".equals(scanPattern) ? 20 : 10)
            + joinCount * 8
            + ("light".equals(computeDensity) ? 5 : "heavy-aggregation".equals(computeDensity) ? 18 : 24)
            + tableCount * 3
            + riskCount * 5;
        return score;
    }

    private Map<String, Object> summarizeDurations(List<Map<String, Object>> metrics) {
        List<Double> values = new ArrayList<Double>();
        for (Map<String, Object> metric : metrics) {
            values.add(Double.valueOf(asDouble(metric.get("latencyMs"), 0)));
        }

        Collections.sort(values);
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("count", Integer.valueOf(values.size()));
        summary.put("avgMs", round(average(values)));
        summary.put("p50Ms", round(percentile(values, 50)));
        summary.put("p95Ms", round(percentile(values, 95)));
        summary.put("p99Ms", round(percentile(values, 99)));
        return summary;
    }

    private double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }

        double sum = 0;
        for (Double value : values) {
            sum += value.doubleValue();
        }
        return sum / values.size();
    }

    private double percentile(List<Double> values, int p) {
        if (values.isEmpty()) {
            return 0;
        }

        int index = Math.min(values.size() - 1, Math.max(0, (int) Math.ceil((p / 100.0d) * values.size()) - 1));
        return values.get(index).doubleValue();
    }

    private double quantile(List<Double> values, double q) {
        if (values.isEmpty()) {
            return 0;
        }

        List<Double> sorted = new ArrayList<Double>(values);
        Collections.sort(sorted);
        double position = (sorted.size() - 1) * q;
        int base = (int) Math.floor(position);
        double rest = position - base;
        double current = sorted.get(base).doubleValue();
        double next = sorted.get(Math.min(base + 1, sorted.size() - 1)).doubleValue();
        return current + rest * (next - current);
    }

    private int countMatches(String value, String token) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = value.indexOf(token, fromIndex)) >= 0) {
            count += 1;
            fromIndex += token.length();
        }
        return count;
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format("%02x", Byte.valueOf(item)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private int priorityRank(String priority) {
        if ("high".equals(priority)) {
            return 0;
        }
        if ("medium".equals(priority)) {
            return 1;
        }
        return 2;
    }

    private int riskRank(String riskLevel) {
        if ("critical".equals(riskLevel)) {
            return 3;
        }
        if ("high".equals(riskLevel)) {
            return 2;
        }
        if ("medium".equals(riskLevel)) {
            return 1;
        }
        return 0;
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private double asDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private int asInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> unique(List<String> values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (!result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<String, Object>() : (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<Map<String, Object>>() : (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return value == null ? new ArrayList<String>() : (List<String>) value;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }
}
