package com.company.queryexecution.domain.query;

/**
 * Result of the read-only SQL guard evaluation.
 */
public class ReadonlyQueryAssessment {

    private final boolean readonly;
    private final String suggestedAction;

    private ReadonlyQueryAssessment(boolean readonly, String suggestedAction) {
        this.readonly = readonly;
        this.suggestedAction = suggestedAction;
    }

    public static ReadonlyQueryAssessment allow() {
        return new ReadonlyQueryAssessment(true, null);
    }

    public static ReadonlyQueryAssessment reject(String suggestedAction) {
        return new ReadonlyQueryAssessment(false, suggestedAction);
    }

    public boolean isReadonly() {
        return readonly;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }
}
