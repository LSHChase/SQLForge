package com.company.sqloptimization.domain.rewrite.policy;

public class RewritePublishEligibilityReason {

    private final String code;
    private final String message;
    private final boolean blocking;
    private final String field;
    private final String evidenceRef;

    public RewritePublishEligibilityReason(String code,
                                           String message,
                                           boolean blocking,
                                           String field,
                                           String evidenceRef) {
        this.code = code;
        this.message = message;
        this.blocking = blocking;
        this.field = field;
        this.evidenceRef = evidenceRef;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public boolean isBlocking() { return blocking; }
    public String getField() { return field; }
    public String getEvidenceRef() { return evidenceRef; }
}
