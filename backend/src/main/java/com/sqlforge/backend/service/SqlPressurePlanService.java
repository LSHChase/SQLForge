package com.sqlforge.backend.service;

import com.sqlforge.backend.web.dto.SqlIntentAnalysisRequest;
import com.sqlforge.backend.web.dto.SqlPressurePlanRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SqlPressurePlanService {

    private final SqlIntentBatchAnalysisService sqlIntentBatchAnalysisService;
    private final SqlIntentAnalysisService sqlIntentAnalysisService;

    public SqlPressurePlanService(
        SqlIntentBatchAnalysisService sqlIntentBatchAnalysisService,
        SqlIntentAnalysisService sqlIntentAnalysisService
    ) {
        this.sqlIntentBatchAnalysisService = sqlIntentBatchAnalysisService;
        this.sqlIntentAnalysisService = sqlIntentAnalysisService;
    }

    public Map<String, Object> buildPlan(SqlPressurePlanRequest request) {
        List<SqlIntentAnalysisRequest.StatementInput> items = sqlIntentBatchAnalysisService.buildStatementInputs(
            request.getBatchId(),
            request.getSource(),
            request.getRawSqlText()
        );

        SqlIntentAnalysisRequest analysisRequest = new SqlIntentAnalysisRequest();
        analysisRequest.setStatements(items);
        Map<String, Object> analysis = sqlIntentAnalysisService.analyze(analysisRequest);
        List<Map<String, Object>> analyses = castList(analysis.get("analyses"));
        List<Map<String, Object>> ranked = sortByComplexity(analyses);
        Map<String, Object> pressureSummary = buildPressureSummary(ranked);
        List<Map<String, Object>> cohorts = buildCohorts(ranked);
        Map<String, Object> candidatePack = buildCandidatePack(ranked);
        Map<String, Object> executionMatrix = buildExecutionMatrix(
            request.getTargetConcurrency() == null ? 20 : request.getTargetConcurrency().intValue(),
            ranked
        );
        Map<String, Object> samplingPlan = buildSamplingPlan(cohorts, ranked.size());

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("batchId", request.getBatchId());
        result.put("source", request.getSource());
        result.put("analysisMode", "pure-structure-pressure-plan");
        result.put("parsedStatementCount", Integer.valueOf(items.size()));
        result.put("analysis", analysis);
        result.put("pressureSummary", pressureSummary);
        result.put("cohorts", cohorts);
        result.put("candidatePack", candidatePack);
        result.put("executionMatrix", executionMatrix);
        result.put("samplingPlan", samplingPlan);
        return result;
    }

    private List<Map<String, Object>> sortByComplexity(List<Map<String, Object>> analyses) {
        List<Map<String, Object>> ranked = new ArrayList<Map<String, Object>>(analyses);
        Collections.sort(ranked, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                int leftScore = asInt(castMap(left.get("pressureProfile")).get("complexityScore"), 0);
                int rightScore = asInt(castMap(right.get("pressureProfile")).get("complexityScore"), 0);
                return rightScore - leftScore;
            }
        });
        return ranked;
    }

    private Map<String, Object> buildPressureSummary(List<Map<String, Object>> ranked) {
        int high = 0;
        int medium = 0;
        int low = 0;
        Set<String> loadClasses = new LinkedHashSet<String>();
        Set<String> pressureSignals = new LinkedHashSet<String>();

        for (Map<String, Object> item : ranked) {
            Map<String, Object> profile = castMap(item.get("pressureProfile"));
            String tier = String.valueOf(profile.get("complexityTier"));
            loadClasses.add(String.valueOf(profile.get("loadClass")));
            for (String signal : castStringList(profile.get("pressureSignals"))) {
                pressureSignals.add(signal);
            }

            if ("high".equals(tier)) {
                high += 1;
            } else if ("medium".equals(tier)) {
                medium += 1;
            } else {
                low += 1;
            }
        }

        return mapOf(
            "highComplexityCount",
            Integer.valueOf(high),
            "mediumComplexityCount",
            Integer.valueOf(medium),
            "lowComplexityCount",
            Integer.valueOf(low),
            "loadClasses",
            new ArrayList<String>(loadClasses),
            "dominantSignals",
            topSignals(new ArrayList<String>(pressureSignals))
        );
    }

    private List<Map<String, Object>> buildCohorts(List<Map<String, Object>> ranked) {
        Map<String, List<Map<String, Object>>> buckets = new LinkedHashMap<String, List<Map<String, Object>>>();

        for (Map<String, Object> item : ranked) {
            Map<String, Object> profile = castMap(item.get("pressureProfile"));
            String loadClass = String.valueOf(profile.get("loadClass"));
            List<Map<String, Object>> bucket = buckets.get(loadClass);
            if (bucket == null) {
                bucket = new ArrayList<Map<String, Object>>();
                buckets.put(loadClass, bucket);
            }
            bucket.add(mapOf(
                "statementId",
                item.get("statementId"),
                "fingerprint",
                item.get("fingerprint"),
                "complexityScore",
                profile.get("complexityScore"),
                "complexityTier",
                profile.get("complexityTier"),
                "intentTags",
                item.get("intentTags")
            ));
        }

        List<Map<String, Object>> cohorts = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : buckets.entrySet()) {
            cohorts.add(mapOf(
                "loadClass",
                entry.getKey(),
                "statementCount",
                Integer.valueOf(entry.getValue().size()),
                "statements",
                entry.getValue()
            ));
        }
        return cohorts;
    }

    private Map<String, Object> buildCandidatePack(List<Map<String, Object>> ranked) {
        return mapOf(
            "smokeSet",
            pickDiverse(ranked, 3, false),
            "standardSet",
            pickDiverse(ranked, 6, false),
            "heavySet",
            pickDiverse(ranked, 8, true)
        );
    }

    private List<Map<String, Object>> pickDiverse(List<Map<String, Object>> ranked, int limit, boolean requireMediumOrHigher) {
        List<Map<String, Object>> selected = new ArrayList<Map<String, Object>>();
        Set<String> seenLoadClasses = new LinkedHashSet<String>();

        for (Map<String, Object> item : ranked) {
            Map<String, Object> profile = castMap(item.get("pressureProfile"));
            String tier = String.valueOf(profile.get("complexityTier"));
            String loadClass = String.valueOf(profile.get("loadClass"));
            if (requireMediumOrHigher && "low".equals(tier)) {
                continue;
            }
            if (seenLoadClasses.add(loadClass)) {
                selected.add(candidateItem(item));
            }
            if (selected.size() >= limit) {
                return selected;
            }
        }

        for (Map<String, Object> item : ranked) {
            if (selected.size() >= limit) {
                break;
            }
            Map<String, Object> candidate = candidateItem(item);
            if (!containsStatement(selected, String.valueOf(candidate.get("statementId")))) {
                if (!requireMediumOrHigher || !"low".equals(String.valueOf(candidate.get("complexityTier")))) {
                    selected.add(candidate);
                }
            }
        }

        return selected;
    }

    private boolean containsStatement(List<Map<String, Object>> items, String statementId) {
        for (Map<String, Object> item : items) {
            if (statementId.equals(String.valueOf(item.get("statementId")))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> candidateItem(Map<String, Object> item) {
        Map<String, Object> profile = castMap(item.get("pressureProfile"));
        return mapOf(
            "statementId",
            item.get("statementId"),
            "fingerprint",
            item.get("fingerprint"),
            "loadClass",
            profile.get("loadClass"),
            "complexityTier",
            profile.get("complexityTier"),
            "complexityScore",
            profile.get("complexityScore")
        );
    }

    private Map<String, Object> buildExecutionMatrix(int targetConcurrency, List<Map<String, Object>> ranked) {
        boolean hasHeavy = false;
        boolean hasWideRead = false;
        boolean hasJoinHeavy = false;

        for (Map<String, Object> item : ranked) {
            Map<String, Object> profile = castMap(item.get("pressureProfile"));
            String loadClass = String.valueOf(profile.get("loadClass"));
            if ("high".equals(String.valueOf(profile.get("complexityTier")))) {
                hasHeavy = true;
            }
            if ("wide-read".equals(loadClass) || "ordered-wide-read".equals(loadClass)) {
                hasWideRead = true;
            }
            if ("multi-join-heavy".equals(loadClass)) {
                hasJoinHeavy = true;
            }
        }

        return mapOf(
            "concurrencyLadder",
            buildConcurrencyLadder(targetConcurrency),
            "dataScaleMatrix",
            Arrays.asList(
                mapOf("name", "sample", "purpose", "快速验证结构分层是否合理"),
                mapOf("name", "medium", "purpose", "验证分组下的资源与计划稳定性"),
                mapOf("name", "full", "purpose", hasHeavy || hasWideRead ? "高复杂 SQL 需要全量回放校验" : "关键样本的周期性全量校验")
            ),
            "distributionMatrix",
            hasJoinHeavy
                ? Arrays.asList("uniform-baseline", "production-like", "skew-aware", "hot-key-check")
                : Arrays.asList("uniform-baseline", "production-like"),
            "resourceConstraintMatrix",
            hasWideRead
                ? Arrays.asList("memory-gradient", "io-latency-injection", "network-throttle")
                : Arrays.asList("cpu-gradient", "memory-gradient"),
            "turningPointRule",
            mapOf("method", "latency-curvature-or-error-rise", "hint", "关注延迟曲率、错误率和队列增长的共同拐点")
        );
    }

    private List<Integer> buildConcurrencyLadder(int targetConcurrency) {
        List<Integer> seeds = Arrays.asList(
            Integer.valueOf(1),
            Integer.valueOf(5),
            Integer.valueOf(10),
            Integer.valueOf(20),
            Integer.valueOf(50),
            Integer.valueOf(100)
        );
        List<Integer> ladder = new ArrayList<Integer>();
        for (Integer seed : seeds) {
            if (seed.intValue() <= targetConcurrency) {
                ladder.add(seed);
            }
        }
        if (!ladder.contains(Integer.valueOf(targetConcurrency))) {
            ladder.add(Integer.valueOf(targetConcurrency));
        }
        Collections.sort(ladder);
        return ladder;
    }

    private Map<String, Object> buildSamplingPlan(List<Map<String, Object>> cohorts, int statementCount) {
        List<Map<String, Object>> rules = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> cohort : cohorts) {
            int count = asInt(cohort.get("statementCount"), 0);
            String loadClass = String.valueOf(cohort.get("loadClass"));
            String ratio = count <= 2 ? "1.0" : count <= 5 ? "0.6" : "0.35";
            String reason = count <= 2 ? "小样本全部保留" : "保留代表性样本并压缩重复负载";
            rules.add(mapOf("loadClass", loadClass, "sampleRatio", ratio, "reason", reason));
        }

        return mapOf(
            "batchSize",
            Integer.valueOf(statementCount),
            "rules",
            rules,
            "guardrails",
            Arrays.asList(
                "每个 load class 至少保留 1 条样本",
                "high complexity SQL 默认全部进入候选集",
                "结构重复且 load class 一致的样本可以按比例抽样"
            )
        );
    }

    private List<String> topSignals(List<String> signals) {
        if (signals.size() <= 5) {
            return signals;
        }
        return signals.subList(0, 5);
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? new ArrayList<Map<String, Object>>() : (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return value == null ? new ArrayList<String>() : (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value == null ? new LinkedHashMap<String, Object>() : (Map<String, Object>) value;
    }

    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return result;
    }
}
