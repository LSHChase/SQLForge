package com.company.sqloptimization.domain.rewrite.policy;

import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewritePublishStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RewritePublishEligibilityPolicy {

    public static final String POLICY_ID = "DEFAULT_REWRITE_PUBLISH_ELIGIBILITY";

    public RewritePublishEligibility evaluate(SqlRewriteRecord record,
                                              List<RewriteValidationRun> validationRuns) {
        List<RewritePublishEligibilityReason> reasons = new ArrayList<RewritePublishEligibilityReason>();
        RewriteValidationRun latestValidationRun = latestValidationRun(record, validationRuns);

        requireReviewApproved(record, reasons);
        requireEquivalentValidation(record, latestValidationRun, reasons);
        requirePublishEvidence(record, reasons);
        requireAutoApplyAllowed(record, reasons);
        requireNoOpenAlertOrPause(record, latestValidationRun, reasons);
        requirePublishStatusReady(record, reasons);
        requireRuntimeDialectEvidence(record, reasons);

        return new RewritePublishEligibility(
            record.getRewriteRecordId(),
            record.getTenantId(),
            POLICY_ID,
            reasons.isEmpty(),
            name(record.getReviewStatus()),
            name(record.getValidationStatus()),
            name(record.getPublishStatus()),
            name(record.getAlertStatus()),
            Boolean.valueOf(record.isAutoApplyAllowed()),
            latestValidationRun == null ? record.getLastValidationRunId() : latestValidationRun.getValidationRunId(),
            reasons
        );
    }

    private void requireReviewApproved(SqlRewriteRecord record,
                                       List<RewritePublishEligibilityReason> reasons) {
        if (record.getReviewStatus() == RewriteReviewStatus.APPROVED) {
            return;
        }
        reasons.add(reason(
            "REVIEW_NOT_APPROVED",
            "Rewrite record must be approved before publish eligibility can pass.",
            "reviewStatus",
            null
        ));
    }

    private void requireEquivalentValidation(SqlRewriteRecord record,
                                             RewriteValidationRun latestValidationRun,
                                             List<RewritePublishEligibilityReason> reasons) {
        if (record.getValidationStatus() != RewriteValidationStatus.EQUIVALENT) {
            reasons.add(reason(
                "VALIDATION_STATUS_NOT_EQUIVALENT",
                "Rewrite record validationStatus must be EQUIVALENT.",
                "validationStatus",
                record.getLastValidationRunId()
            ));
        }
        if (latestValidationRun == null) {
            reasons.add(reason(
                "EQUIVALENT_VALIDATION_RUN_MISSING",
                "At least one successful equivalent validation run is required.",
                "validationRuns",
                null
            ));
            return;
        }
        if (latestValidationRun.getStatus() != ValidationRunStatus.SUCCEEDED
            || latestValidationRun.getComparisonStatus() != ComparisonStatus.EQUIVALENT) {
            reasons.add(reason(
                "LATEST_VALIDATION_NOT_PASSED",
                "Latest validation run must be SUCCEEDED and EQUIVALENT.",
                "validationRuns",
                latestValidationRun.getValidationRunId()
            ));
        }
    }

    private void requirePublishEvidence(SqlRewriteRecord record,
                                        List<RewritePublishEligibilityReason> reasons) {
        requireText(record.getTenantId(), "TENANT_ID_MISSING", "tenantId", reasons);
        requireText(record.getSqlFingerprint(), "SQL_FINGERPRINT_MISSING", "sqlFingerprint", reasons);
        requireText(record.getOriginalSqlText(), "ORIGINAL_SQL_MISSING", "originalSqlText", reasons);
        requireText(record.getRecommendedSqlText(), "RECOMMENDED_SQL_MISSING", "recommendedSqlText", reasons);
        if (record.getSourceType() == null) {
            reasons.add(reason("SOURCE_TYPE_MISSING", "sourceType is required.", "sourceType", null));
        }
        if (record.getSourceKind() == null) {
            reasons.add(reason("SOURCE_KIND_MISSING", "sourceKind is required.", "sourceKind", null));
        }
        requireText(record.getSourceId(), "SOURCE_ID_MISSING", "sourceId", reasons);
        if (record.getEvidenceLevel() == null) {
            reasons.add(reason("EVIDENCE_LEVEL_MISSING", "evidenceLevel is required.", "evidenceLevel", null));
        }
    }

    private void requireAutoApplyAllowed(SqlRewriteRecord record,
                                         List<RewritePublishEligibilityReason> reasons) {
        if (record.isAutoApplyAllowed()) {
            return;
        }
        reasons.add(reason(
            "AUTO_APPLY_NOT_ALLOWED",
            "autoApplyAllowed must be true before publishing a runtime rewrite rule.",
            "autoApplyAllowed",
            null
        ));
    }

    private void requireNoOpenAlertOrPause(SqlRewriteRecord record,
                                           RewriteValidationRun latestValidationRun,
                                           List<RewritePublishEligibilityReason> reasons) {
        if (record.getAlertStatus() != RewriteAlertStatus.NONE
            && record.getAlertStatus() != RewriteAlertStatus.RESOLVED) {
            reasons.add(reason(
                "REWRITE_ALERT_UNRESOLVED",
                "Open or unresolved rewrite validation alerts block publishing.",
                "alertStatus",
                record.getLastValidationRunId()
            ));
        }
        if (record.getStatus() == RewriteRecordStatus.PAUSED
            || record.getStatus() == RewriteRecordStatus.ROLLED_BACK
            || record.getStatus() == RewriteRecordStatus.DEPRECATED
            || record.getStatus() == RewriteRecordStatus.CANCELLED) {
            reasons.add(reason(
                "REWRITE_RECORD_PAUSED_OR_CLOSED",
                "Paused, rolled back, deprecated, or cancelled rewrite records cannot be published.",
                "status",
                null
            ));
        }
        if (latestValidationRun != null && latestValidationRun.isAutoApplyPaused()) {
            reasons.add(reason(
                "VALIDATION_PAUSE_MARKER_PRESENT",
                "Latest validation run contains an auto-apply pause marker.",
                "validationRuns",
                latestValidationRun.getValidationRunId()
            ));
        }
    }

    private void requirePublishStatusReady(SqlRewriteRecord record,
                                           List<RewritePublishEligibilityReason> reasons) {
        if (record.getPublishStatus() == RewritePublishStatus.UNPUBLISHED
            || record.getPublishStatus() == RewritePublishStatus.PUBLISH_FAILED) {
            return;
        }
        reasons.add(reason(
            "PUBLISH_STATUS_NOT_READY",
            "Only UNPUBLISHED or PUBLISH_FAILED rewrite records can enter publish eligibility.",
            "publishStatus",
            record.getRuntimeBindingId()
        ));
    }

    private void requireRuntimeDialectEvidence(SqlRewriteRecord record,
                                               List<RewritePublishEligibilityReason> reasons) {
        if (hasText(record.getDatasourceCode())) {
            return;
        }
        reasons.add(reason(
            "RUNTIME_DIALECT_EVIDENCE_MISSING",
            "datasourceCode is required as conservative runtime schema or dialect evidence.",
            "datasourceCode",
            record.getValidationPolicyId()
        ));
    }

    private RewriteValidationRun latestValidationRun(SqlRewriteRecord record,
                                                     List<RewriteValidationRun> validationRuns) {
        List<RewriteValidationRun> safeRuns = validationRuns == null
            ? Collections.<RewriteValidationRun>emptyList()
            : validationRuns;
        String lastValidationRunId = trimToNull(record.getLastValidationRunId());
        if (lastValidationRunId != null) {
            for (RewriteValidationRun run : safeRuns) {
                if (matchesRecord(record, run) && lastValidationRunId.equals(run.getValidationRunId())) {
                    return run;
                }
            }
        }
        RewriteValidationRun latest = null;
        for (RewriteValidationRun run : safeRuns) {
            if (!matchesRecord(record, run)) {
                continue;
            }
            if (latest == null || compareValidationTime(run, latest) > 0) {
                latest = run;
            }
        }
        return latest;
    }

    private int compareValidationTime(RewriteValidationRun left, RewriteValidationRun right) {
        Instant leftTime = validationTime(left);
        Instant rightTime = validationTime(right);
        if (leftTime == null && rightTime == null) {
            return left.getValidationRunId().compareTo(right.getValidationRunId());
        }
        if (leftTime == null) {
            return -1;
        }
        if (rightTime == null) {
            return 1;
        }
        return leftTime.compareTo(rightTime);
    }

    private Instant validationTime(RewriteValidationRun run) {
        return run.getFinishedAt() == null ? run.getStartedAt() : run.getFinishedAt();
    }

    private boolean matchesRecord(SqlRewriteRecord record, RewriteValidationRun run) {
        return run != null
            && record.getRewriteRecordId().equals(run.getRewriteRecordId())
            && record.getTenantId().equals(run.getTenantId());
    }

    private void requireText(String value,
                             String code,
                             String field,
                             List<RewritePublishEligibilityReason> reasons) {
        if (hasText(value)) {
            return;
        }
        reasons.add(reason(code, field + " is required.", field, null));
    }

    private RewritePublishEligibilityReason reason(String code,
                                                   String message,
                                                   String field,
                                                   String evidenceRef) {
        return new RewritePublishEligibilityReason(code, message, true, field, evidenceRef);
    }

    private boolean hasText(String value) {
        return trimToNull(value) != null;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
