package com.company.sqloptimization.domain.rewrite.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewritePublishStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class RewritePublishEligibilityPolicyTest {

    private final RewritePublishEligibilityPolicy policy = new RewritePublishEligibilityPolicy();

    @Test
    void shouldPassWhenAllPublishEligibilityGatesAreSatisfied() {
        SqlRewriteRecord record = eligibleRecordBuilder()
            .build();
        RewriteValidationRun run = equivalentRun("validation-001", "rewrite-001", false);

        RewritePublishEligibility eligibility = policy.evaluate(record, Collections.singletonList(run));

        assertTrue(eligibility.isEligible());
        assertTrue(eligibility.getRefusalReasons().isEmpty());
    }

    @Test
    void shouldRejectReviewValidationAndAutoApplyGateFailures() {
        SqlRewriteRecord record = eligibleRecordBuilder()
            .reviewStatus(RewriteReviewStatus.PENDING_REVIEW)
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .autoApplyAllowed(false)
            .build();

        RewritePublishEligibility eligibility = policy.evaluate(record, Collections.<RewriteValidationRun>emptyList());

        assertFalse(eligibility.isEligible());
        assertTrue(hasReason(eligibility, "REVIEW_NOT_APPROVED"));
        assertTrue(hasReason(eligibility, "VALIDATION_STATUS_NOT_EQUIVALENT"));
        assertTrue(hasReason(eligibility, "EQUIVALENT_VALIDATION_RUN_MISSING"));
        assertTrue(hasReason(eligibility, "AUTO_APPLY_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectMissingFingerprintAndRuntimeDialectEvidence() {
        SqlRewriteRecord record = eligibleRecordBuilder()
            .sqlFingerprint(" ")
            .datasourceCode(" ")
            .build();
        RewriteValidationRun run = equivalentRun("validation-001", "rewrite-001", false);

        RewritePublishEligibility eligibility = policy.evaluate(record, Collections.singletonList(run));

        assertFalse(eligibility.isEligible());
        assertTrue(hasReason(eligibility, "SQL_FINGERPRINT_MISSING"));
        assertTrue(hasReason(eligibility, "RUNTIME_DIALECT_EVIDENCE_MISSING"));
    }

    @Test
    void shouldRejectOpenAlertPauseMarkerAndClosedRecordStatus() {
        SqlRewriteRecord record = eligibleRecordBuilder()
            .status(RewriteRecordStatus.PAUSED)
            .alertStatus(RewriteAlertStatus.OPEN)
            .lastValidationRunId("validation-paused")
            .build();
        RewriteValidationRun pausedRun = RewriteValidationRun.builder()
            .validationRunId("validation-paused")
            .tenantId("tenant-a")
            .rewriteRecordId("rewrite-001")
            .status(ValidationRunStatus.FAILED)
            .comparisonStatus(ComparisonStatus.DIVERGED)
            .autoApplyPaused(true)
            .startedAt(Instant.parse("2026-05-11T00:00:00Z"))
            .finishedAt(Instant.parse("2026-05-11T00:00:01Z"))
            .build();

        RewritePublishEligibility eligibility = policy.evaluate(record, Collections.singletonList(pausedRun));

        assertFalse(eligibility.isEligible());
        assertTrue(hasReason(eligibility, "LATEST_VALIDATION_NOT_PASSED"));
        assertTrue(hasReason(eligibility, "REWRITE_ALERT_UNRESOLVED"));
        assertTrue(hasReason(eligibility, "REWRITE_RECORD_PAUSED_OR_CLOSED"));
        assertTrue(hasReason(eligibility, "VALIDATION_PAUSE_MARKER_PRESENT"));
    }

    @Test
    void shouldRejectAlreadyPublishedStatus() {
        SqlRewriteRecord record = eligibleRecordBuilder()
            .publishStatus(RewritePublishStatus.PUBLISHED)
            .build();
        RewriteValidationRun run = equivalentRun("validation-001", "rewrite-001", false);

        RewritePublishEligibility eligibility = policy.evaluate(record, Collections.singletonList(run));

        assertFalse(eligibility.isEligible());
        assertTrue(hasReason(eligibility, "PUBLISH_STATUS_NOT_READY"));
    }

    private SqlRewriteRecord.Builder eligibleRecordBuilder() {
        return SqlRewriteRecord.builder()
            .rewriteRecordId("rewrite-001")
            .tenantId("tenant-a")
            .sourceType(GovernanceSourceType.QUERY)
            .sourceKind(GovernanceSourceKind.QUERY_HISTORY)
            .sourceId("history-001")
            .evidenceLevel(EvidenceLevel.RUNTIME_HISTORY)
            .historyId("history-001")
            .sqlFingerprint("fp-001")
            .datasourceCode("HETU")
            .status(RewriteRecordStatus.APPROVED)
            .validationStatus(RewriteValidationStatus.EQUIVALENT)
            .autoApplyAllowed(true)
            .manualReviewRequired(true)
            .reviewStatus(RewriteReviewStatus.APPROVED)
            .publishStatus(RewritePublishStatus.UNPUBLISHED)
            .alertStatus(RewriteAlertStatus.NONE)
            .originalSqlText("SELECT * FROM orders")
            .recommendedSqlText("SELECT id FROM orders")
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-11T00:00:00Z"));
    }

    private RewriteValidationRun equivalentRun(String validationRunId,
                                               String rewriteRecordId,
                                               boolean autoApplyPaused) {
        return RewriteValidationRun.builder()
            .validationRunId(validationRunId)
            .tenantId("tenant-a")
            .rewriteRecordId(rewriteRecordId)
            .status(ValidationRunStatus.SUCCEEDED)
            .comparisonStatus(ComparisonStatus.EQUIVALENT)
            .autoApplyPaused(autoApplyPaused)
            .startedAt(Instant.parse("2026-05-11T00:00:00Z"))
            .finishedAt(Instant.parse("2026-05-11T00:00:01Z"))
            .build();
    }

    private boolean hasReason(RewritePublishEligibility eligibility, String code) {
        for (RewritePublishEligibilityReason reason : eligibility.getRefusalReasons()) {
            if (code.equals(reason.getCode())) {
                return true;
            }
        }
        return false;
    }
}
