package com.company.sqloptimization.domain.rewrite.rule;

import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import java.util.List;
import java.util.Map;

public class RewriteRuleDefinition {

    private final String id;
    private final String name;
    private final RewriteRuleCategory category;
    private final RewriteRuleSeverity severity;
    private final RelationalRewriteRuleType relationalRuleType;
    private final RewriteRulePattern pattern;
    private final List<RewriteRulePrecondition> preconditions;
    private final List<RewriteRuleAction> actions;
    private final RewriteRuleVerification verification;
    private final RewriteRuleCostImpact costImpact;
    private final List<String> mustRunAfterRuleIds;
    private final Map<String, Object> attributes;

    public RewriteRuleDefinition(String id,
                                 String name,
                                 RewriteRuleCategory category,
                                 RewriteRuleSeverity severity,
                                 RelationalRewriteRuleType relationalRuleType,
                                 RewriteRulePattern pattern,
                                 List<RewriteRulePrecondition> preconditions,
                                 List<RewriteRuleAction> actions,
                                 RewriteRuleVerification verification,
                                 RewriteRuleCostImpact costImpact,
                                 List<String> mustRunAfterRuleIds,
                                 Map<String, Object> attributes) {
        this.id = RuleEngineCollections.text(id);
        this.name = RuleEngineCollections.text(name);
        this.category = category;
        this.severity = severity;
        this.relationalRuleType = relationalRuleType;
        this.pattern = pattern;
        this.preconditions = RuleEngineCollections.immutableList(preconditions);
        this.actions = RuleEngineCollections.immutableList(actions);
        this.verification = verification;
        this.costImpact = costImpact;
        this.mustRunAfterRuleIds = RuleEngineCollections.immutableStrings(mustRunAfterRuleIds);
        this.attributes = RuleEngineCollections.immutableMap(attributes);
    }

    public boolean hasActionType(String actionType) {
        for (RewriteRuleAction action : actions) {
            if (action.isType(actionType)) {
                return true;
            }
        }
        return false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public RewriteRuleCategory getCategory() {
        return category;
    }

    public RewriteRuleSeverity getSeverity() {
        return severity;
    }

    public RelationalRewriteRuleType getRelationalRuleType() {
        return relationalRuleType;
    }

    public RewriteRulePattern getPattern() {
        return pattern;
    }

    public List<RewriteRulePrecondition> getPreconditions() {
        return preconditions;
    }

    public List<RewriteRuleAction> getActions() {
        return actions;
    }

    public RewriteRuleVerification getVerification() {
        return verification;
    }

    public RewriteRuleCostImpact getCostImpact() {
        return costImpact;
    }

    public List<String> getMustRunAfterRuleIds() {
        return mustRunAfterRuleIds;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
