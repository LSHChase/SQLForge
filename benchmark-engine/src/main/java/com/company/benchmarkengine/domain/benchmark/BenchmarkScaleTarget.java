package com.company.benchmarkengine.domain.benchmark;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BenchmarkScaleTarget {

    public static final String STATUS_TARGET_DECLARED_UNVERIFIED = "TARGET_DECLARED_UNVERIFIED";
    public static final String DEFAULT_EVIDENCE_BOUNDARY =
        "Scale target is requested benchmark intent only; production proof requires completed benchmark/report evidence.";

    private static final List<String> DEFAULT_REQUIRED_EVIDENCE = Collections.unmodifiableList(
        Arrays.asList(
            "completedBenchmarkTask",
            "p95P99Latency",
            "scanBytes",
            "cpuAndMemory",
            "costBillOrResourceUnit",
            "workloadWindow"
        )
    );

    private final Integer targetConcurrency;
    private final String targetDatasetSizeLabel;
    private final Long targetDailyQueryVolume;
    private final String targetComplexityProfile;
    private final String targetCostEfficiency;
    private final String evidenceStatus;
    private final String evidenceBoundary;
    private final List<String> requiredEvidence;

    public BenchmarkScaleTarget(Integer targetConcurrency,
                                String targetDatasetSizeLabel,
                                Long targetDailyQueryVolume,
                                String targetComplexityProfile,
                                String targetCostEfficiency,
                                String evidenceStatus,
                                String evidenceBoundary,
                                List<String> requiredEvidence) {
        this.targetConcurrency = targetConcurrency;
        this.targetDatasetSizeLabel = normalizeText(targetDatasetSizeLabel);
        this.targetDailyQueryVolume = targetDailyQueryVolume;
        this.targetComplexityProfile = normalizeText(targetComplexityProfile);
        this.targetCostEfficiency = normalizeText(targetCostEfficiency);
        this.evidenceStatus = hasText(evidenceStatus)
            ? evidenceStatus.trim()
            : STATUS_TARGET_DECLARED_UNVERIFIED;
        this.evidenceBoundary = hasText(evidenceBoundary)
            ? evidenceBoundary.trim()
            : DEFAULT_EVIDENCE_BOUNDARY;
        this.requiredEvidence = normalizeRequiredEvidence(requiredEvidence);
    }

    public boolean hasAnyTarget() {
        return targetConcurrency != null
            || hasText(targetDatasetSizeLabel)
            || targetDailyQueryVolume != null
            || hasText(targetComplexityProfile)
            || hasText(targetCostEfficiency);
    }

    public Integer getTargetConcurrency() {
        return targetConcurrency;
    }

    public String getTargetDatasetSizeLabel() {
        return targetDatasetSizeLabel;
    }

    public Long getTargetDailyQueryVolume() {
        return targetDailyQueryVolume;
    }

    public String getTargetComplexityProfile() {
        return targetComplexityProfile;
    }

    public String getTargetCostEfficiency() {
        return targetCostEfficiency;
    }

    public String getEvidenceStatus() {
        return evidenceStatus;
    }

    public String getEvidenceBoundary() {
        return evidenceBoundary;
    }

    public List<String> getRequiredEvidence() {
        return requiredEvidence;
    }

    private List<String> normalizeRequiredEvidence(List<String> requestedEvidence) {
        if (requestedEvidence == null || requestedEvidence.isEmpty()) {
            return DEFAULT_REQUIRED_EVIDENCE;
        }
        List<String> items = new ArrayList<String>(requestedEvidence.size());
        for (String item : requestedEvidence) {
            if (hasText(item)) {
                items.add(item.trim());
            }
        }
        return items.isEmpty()
            ? DEFAULT_REQUIRED_EVIDENCE
            : Collections.unmodifiableList(items);
    }

    private String normalizeText(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
