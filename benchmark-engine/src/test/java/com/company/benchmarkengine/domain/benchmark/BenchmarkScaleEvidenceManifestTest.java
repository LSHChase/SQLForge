package com.company.benchmarkengine.domain.benchmark;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BenchmarkScaleEvidenceManifestTest {

    @Test
    void shouldRequireStructuredBundleBeforeAcceptingVerifiedStatus() {
        BenchmarkScaleEvidenceManifest manifest = manifestWithBundle(
            BenchmarkScaleEvidenceManifest.STATUS_VERIFIED,
            null
        );

        assertFalse(manifest.isExternallyVerified(Integer.valueOf(10000)));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(10000)).contains("productionEvidenceBundle"));
    }

    @Test
    void shouldRejectBundleBelowProductionScaleThresholds() {
        BenchmarkScaleEvidenceManifest manifest = manifestWithBundle(
            BenchmarkScaleEvidenceManifest.STATUS_VERIFIED,
            new BenchmarkScaleEvidenceBundle(
                Integer.valueOf(9999),
                Long.valueOf(9999999L),
                Long.valueOf(29999999999999999L),
                new BigDecimal("23.99"),
                new BigDecimal("120"),
                new BigDecimal("240"),
                Long.valueOf(9876543210L),
                new BigDecimal("72.5"),
                BigDecimal.ZERO,
                new BigDecimal("12345.67"),
                "USD",
                "prod-run-20260518/verifier.json"
            )
        );

        assertFalse(manifest.isExternallyVerified(Integer.valueOf(10000)));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(10000)).toString().contains("concurrency"));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(10000)).toString().contains("dailyQueryVolume"));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(10000)).toString().contains("dataLayout30Pb"));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(10000)).toString().contains("longReplay"));
    }

    @Test
    void shouldAcceptVerifiedManifestOnlyWhenBundleCoversProductionEvidence() {
        BenchmarkScaleEvidenceManifest manifest = manifestWithBundle(
            BenchmarkScaleEvidenceManifest.STATUS_VERIFIED,
            productionBundle(Integer.valueOf(12000))
        );

        assertTrue(manifest.isExternallyVerified(Integer.valueOf(12000)));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(12000)).isEmpty());
        assertTrue(manifest.satisfiedVerificationEvidence(Integer.valueOf(12000)).contains("productionEvidenceBundle.costBill"));
        assertTrue(manifest.satisfiedVerificationEvidence(Integer.valueOf(12000))
            .contains("productionEvidenceManifest.evidenceFileDigests"));
    }

    @Test
    void shouldRejectVerifiedManifestWithoutCompleteEvidenceFileDigests() {
        BenchmarkScaleEvidenceManifest manifest = new BenchmarkScaleEvidenceManifest(
            "PROD_REPLAY",
            "prod-run-20260518/concurrency.log",
            "prod-run-20260518/daily-query-volume.json",
            "prod-run-20260518/data-layout-30pb.json",
            "prod-run-20260518/replay-window.log",
            "2026-05-17T00:00Z/2026-05-18T00:00Z",
            "prod-run-20260518/p95-p99.csv",
            "prod-run-20260518/scan-cpu-queue.csv",
            "prod-run-20260518/cost-bill.csv",
            BenchmarkScaleEvidenceManifest.STATUS_VERIFIED,
            null,
            productionBundle(Integer.valueOf(12000))
        );

        assertFalse(manifest.isExternallyVerified(Integer.valueOf(12000)));
        assertTrue(manifest.missingVerificationEvidence(Integer.valueOf(12000)).toString()
            .contains("evidenceFileDigests"));
    }

    private BenchmarkScaleEvidenceManifest manifestWithBundle(String status, BenchmarkScaleEvidenceBundle bundle) {
        return new BenchmarkScaleEvidenceManifest(
            "PROD_REPLAY",
            "prod-run-20260518/concurrency.log",
            "prod-run-20260518/daily-query-volume.json",
            "prod-run-20260518/data-layout-30pb.json",
            "prod-run-20260518/replay-window.log",
            "2026-05-17T00:00Z/2026-05-18T00:00Z",
            "prod-run-20260518/p95-p99.csv",
            "prod-run-20260518/scan-cpu-queue.csv",
            "prod-run-20260518/cost-bill.csv",
            status,
            productionDigests(),
            bundle
        );
    }

    private Map<String, BenchmarkScaleEvidenceFileDigest> productionDigests() {
        Map<String, BenchmarkScaleEvidenceFileDigest> digests =
            new LinkedHashMap<String, BenchmarkScaleEvidenceFileDigest>();
        for (String fileName : BenchmarkScaleEvidenceManifest.REQUIRED_EVIDENCE_FILES) {
            digests.put(fileName, new BenchmarkScaleEvidenceFileDigest(repeat("a", 64), Long.valueOf(128L)));
        }
        return digests;
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private BenchmarkScaleEvidenceBundle productionBundle(Integer observedConcurrency) {
        return new BenchmarkScaleEvidenceBundle(
            observedConcurrency,
            Long.valueOf(10000000L),
            Long.valueOf(30000000000000000L),
            new BigDecimal("24"),
            new BigDecimal("120"),
            new BigDecimal("240"),
            Long.valueOf(9876543210L),
            new BigDecimal("72.5"),
            BigDecimal.ZERO,
            new BigDecimal("12345.67"),
            "USD",
            "prod-run-20260518/verifier.json"
        );
    }
}
