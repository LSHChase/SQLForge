package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResultDigestComparisonResult {

    private final ValidationRunStatus validationRunStatus;
    private final ComparisonStatus comparisonStatus;
    private final DifferenceType differenceType;
    private final boolean autoApplyPaused;
    private final Map<String, Object> differenceSample;
    private final Map<String, Object> executionEvidence;

    public ResultDigestComparisonResult(ValidationRunStatus validationRunStatus,
                                        ComparisonStatus comparisonStatus,
                                        DifferenceType differenceType,
                                        boolean autoApplyPaused,
                                        Map<String, Object> differenceSample,
                                        Map<String, Object> executionEvidence) {
        this.validationRunStatus = validationRunStatus;
        this.comparisonStatus = comparisonStatus;
        this.differenceType = differenceType;
        this.autoApplyPaused = autoApplyPaused;
        this.differenceSample = immutableCopy(differenceSample);
        this.executionEvidence = immutableCopy(executionEvidence);
    }

    public ValidationRunStatus getValidationRunStatus() { return validationRunStatus; }
    public ComparisonStatus getComparisonStatus() { return comparisonStatus; }
    public DifferenceType getDifferenceType() { return differenceType; }
    public boolean isAutoApplyPaused() { return autoApplyPaused; }
    public Map<String, Object> getDifferenceSample() { return differenceSample; }
    public Map<String, Object> getExecutionEvidence() { return executionEvidence; }

    private Map<String, Object> immutableCopy(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
    }
}
