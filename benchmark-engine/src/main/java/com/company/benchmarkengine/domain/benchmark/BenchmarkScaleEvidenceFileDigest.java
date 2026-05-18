package com.company.benchmarkengine.domain.benchmark;

import java.util.Locale;
import java.util.regex.Pattern;

public class BenchmarkScaleEvidenceFileDigest {

    private static final Pattern SHA256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final String sha256;
    private final Long sizeBytes;

    public BenchmarkScaleEvidenceFileDigest(String sha256, Long sizeBytes) {
        this.sha256 = normalizeSha256(sha256);
        this.sizeBytes = sizeBytes;
    }

    public boolean hasAnyEvidence() {
        return hasText(sha256) || sizeBytes != null;
    }

    public boolean isComplete() {
        return hasText(sha256)
            && SHA256_PATTERN.matcher(sha256).matches()
            && sizeBytes != null
            && sizeBytes.longValue() > 0L;
    }

    public String getSha256() {
        return sha256;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    private String normalizeSha256(String value) {
        return hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }
}
