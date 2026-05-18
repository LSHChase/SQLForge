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
import java.util.Map;

public class RewritePublishEligibilityPolicy {

    public static final String POLICY_ID = "DEFAULT_REWRITE_PUBLISH_ELIGIBILITY";

    public RewritePublishEligibility evaluate(SqlRewriteRecord record,
                                              List<RewriteValidationRun> validationRuns) {
        List<RewritePublishEligibilityReason> reasons = new ArrayList<RewritePublishEligibilityReason>();
        RewriteValidationRun latestValidationRun = latestValidationRun(record, validationRuns);

        requireReviewApproved(record, reasons);
        requireEquivalentValidation(record, latestValidationRun, reasons);
        requirePositiveRuntimeBenefit(latestValidationRun, reasons);
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
            "改写记录必须先通过审批，才能符合发布资格。",
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
                "改写记录 validationStatus 必须为 EQUIVALENT。",
                "validationStatus",
                record.getLastValidationRunId()
            ));
        }
        if (latestValidationRun == null) {
            reasons.add(reason(
                "EQUIVALENT_VALIDATION_RUN_MISSING",
                "至少需要一次成功且等价的校验运行。",
                "validationRuns",
                null
            ));
            return;
        }
        if (latestValidationRun.getStatus() != ValidationRunStatus.SUCCEEDED
            || latestValidationRun.getComparisonStatus() != ComparisonStatus.EQUIVALENT) {
            reasons.add(reason(
                "LATEST_VALIDATION_NOT_PASSED",
                "最近一次校验运行必须为 SUCCEEDED 且 EQUIVALENT。",
                "validationRuns",
                latestValidationRun.getValidationRunId()
            ));
        }
    }

    private void requirePositiveRuntimeBenefit(RewriteValidationRun latestValidationRun,
                                               List<RewritePublishEligibilityReason> reasons) {
        if (latestValidationRun == null) {
            return;
        }
        Map<String, Object> runtimeDelta = asMap(latestValidationRun.getExecutionEvidence().get("runtimeDelta"));
        if (runtimeDelta == null || runtimeDelta.isEmpty()) {
            reasons.add(reason(
                "RUNTIME_BENEFIT_EVIDENCE_MISSING",
                "最近一次等价校验必须包含运行时收益证据。",
                "validationRuns.executionEvidence.runtimeDelta",
                latestValidationRun.getValidationRunId()
            ));
            return;
        }
        Object benefitStatus = runtimeDelta.get("benefitStatus");
        if ("POSITIVE".equals(String.valueOf(benefitStatus))) {
            return;
        }
        reasons.add(reason(
            "RUNTIME_BENEFIT_NOT_POSITIVE",
            "最近一次等价校验的运行时收益必须为 POSITIVE。",
            "validationRuns.executionEvidence.runtimeDelta.benefitStatus",
            latestValidationRun.getValidationRunId()
        ));
    }

    private void requirePublishEvidence(SqlRewriteRecord record,
                                        List<RewritePublishEligibilityReason> reasons) {
        requireText(record.getTenantId(), "TENANT_ID_MISSING", "tenantId", reasons);
        requireText(record.getSqlFingerprint(), "SQL_FINGERPRINT_MISSING", "sqlFingerprint", reasons);
        requireText(record.getOriginalSqlText(), "ORIGINAL_SQL_MISSING", "originalSqlText", reasons);
        requireText(record.getRecommendedSqlText(), "RECOMMENDED_SQL_MISSING", "recommendedSqlText", reasons);
        if (record.getSourceType() == null) {
            reasons.add(reason("SOURCE_TYPE_MISSING", "sourceType 为必填项。", "sourceType", null));
        }
        if (record.getSourceKind() == null) {
            reasons.add(reason("SOURCE_KIND_MISSING", "sourceKind 为必填项。", "sourceKind", null));
        }
        requireText(record.getSourceId(), "SOURCE_ID_MISSING", "sourceId", reasons);
        if (record.getEvidenceLevel() == null) {
            reasons.add(reason("EVIDENCE_LEVEL_MISSING", "evidenceLevel 为必填项。", "evidenceLevel", null));
        }
    }

    private void requireAutoApplyAllowed(SqlRewriteRecord record,
                                         List<RewritePublishEligibilityReason> reasons) {
        if (record.isAutoApplyAllowed()) {
            return;
        }
        reasons.add(reason(
            "AUTO_APPLY_NOT_ALLOWED",
            "发布运行时改写规则前 autoApplyAllowed 必须为 true。",
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
                "存在未处理或未解决的改写校验告警，阻止发布。",
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
                "已暂停、已回滚、已废弃或已取消的改写记录不能发布。",
                "status",
                null
            ));
        }
        if (latestValidationRun != null && latestValidationRun.isAutoApplyPaused()) {
            reasons.add(reason(
                "VALIDATION_PAUSE_MARKER_PRESENT",
                "最近一次校验运行包含自动应用暂停标记。",
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
            "只有 UNPUBLISHED 或 PUBLISH_FAILED 状态的改写记录可以进入发布资格检查。",
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
            "datasourceCode 是保守运行时 schema 或方言证据的必填项。",
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
        reasons.add(reason(code, field + " 为必填项。", field, null));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }
        return (Map<String, Object>) value;
    }
}
