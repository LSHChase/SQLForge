package com.company.sqloptimization.domain.rewrite.ir;

import java.util.List;

public class RewriteIrConflict {

    private final String conflictCode;
    private final String scope;
    private final String impact;
    private final String selectedOption;
    private final List<String> alternatives;

    public RewriteIrConflict(String conflictCode,
                             String scope,
                             String impact,
                             String selectedOption,
                             List<String> alternatives) {
        this.conflictCode = conflictCode;
        this.scope = scope;
        this.impact = impact;
        this.selectedOption = selectedOption;
        this.alternatives = IrCollections.immutableStrings(alternatives);
    }

    public String getConflictCode() {
        return conflictCode;
    }

    public String getScope() {
        return scope;
    }

    public String getImpact() {
        return impact;
    }

    public String getSelectedOption() {
        return selectedOption;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }
}
