package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SqlOptimizationManualReviewSampleTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldKeepManualReviewSamplesAsUnappliedRecommendations() {
        List<Sample> samples = Arrays.asList(
            sample(
                "select-star",
                "SELECT * FROM orders WHERE order_date >= DATE '2026-01-01';",
                "SELECT_STAR_EXPANSION"
            ),
            sample(
                "or-to-union",
                "SELECT order_id, customer_id FROM orders WHERE status = 'PAID' OR channel = 'APP';",
                "OR_TO_UNION_ALL"
            ),
            sample(
                "function-predicate",
                "SELECT order_id FROM orders WHERE YEAR(order_date) = 2026;",
                "FUNCTION_PREDICATE_TO_RANGE"
            ),
            sample(
                "scalar-subquery",
                "SELECT c.customer_id, "
                    + "(SELECT MAX(o.order_amount) FROM orders o WHERE o.customer_id = c.customer_id) AS max_amount "
                    + "FROM customers c;",
                "SCALAR_SUBQUERY_TO_JOIN"
            ),
            sample(
                "repeated-subquery",
                "SELECT c.customer_id, "
                    + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_a, "
                    + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS order_count_b "
                    + "FROM customers c;",
                "REPEATED_SUBQUERY_TO_CTE"
            ),
            sample(
                "not-exists",
                "SELECT c.customer_id FROM customers c "
                    + "WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id);",
                "NOT_EXISTS_TO_ANTI_JOIN"
            ),
            sample(
                "leading-like",
                "SELECT customer_id FROM customers WHERE customer_name LIKE '%vip%';",
                "LEADING_LIKE_REVIEW"
            ),
            sample(
                "order-random",
                "SELECT order_id, customer_id FROM orders ORDER BY RAND() LIMIT 100;",
                "ORDER_RANDOM_REVIEW"
            )
        );

        for (Sample sample : samples) {
            SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
                sample.sql,
                DataSourceTypeEnum.HETU
            );
            OptimizationTaskSuggestion suggestion = service.buildRewriteSuggestion(profile);
            SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
                service.buildRecommendationRuleOutputModel(profile);
            List<String> appliedRules = service.deriveRewriteCandidateRules(profile);
            List<String> unappliedRules = ruleNames(model.getUnappliedRules());

            System.out.println(
                sample.name
                    + " | appliedRules=" + appliedRules
                    + " | candidateSql=" + artifact(suggestion, "REWRITTEN_SQL", "candidateSql")
                    + " | unappliedRules=" + unappliedRules
                    + " | manualReviewRequired=" + model.isManualReviewRequired()
            );

            assertTrue(appliedRules.isEmpty(), sample.name + " 不应生成安全候选改写规则");
            assertEquals("[]", artifact(suggestion, "REWRITE_RULE_TRACE", "appliedRules"));
            assertTrue(unappliedRules.contains(sample.expectedUnappliedRule), sample.name);
            assertTrue(model.isManualReviewRequired(), sample.name);
            assertFalse(model.isAutoApplyAllowed(), sample.name);
            assertEquals("RESULT_DIFF_THEN_MANUAL_REVIEW", model.getValidationMethod());
        }
    }

    private Sample sample(String name, String sql, String expectedUnappliedRule) {
        return new Sample(name, sql, expectedUnappliedRule);
    }

    private String artifact(OptimizationTaskSuggestion suggestion, String category, String name) {
        for (OptimizationTaskArtifact artifact : suggestion.getArtifacts()) {
            if (category.equals(artifact.getCategory()) && name.equals(artifact.getName())) {
                return artifact.getContent();
            }
        }
        return "";
    }

    private List<String> ruleNames(List<Map<String, Object>> rules) {
        List<String> names = new ArrayList<String>();
        for (Map<String, Object> rule : rules) {
            Object name = rule.get("rule");
            if (name != null) {
                names.add(String.valueOf(name));
            }
        }
        return names;
    }

    private static final class Sample {
        private final String name;
        private final String sql;
        private final String expectedUnappliedRule;

        private Sample(String name, String sql, String expectedUnappliedRule) {
            this.name = name;
            this.sql = sql;
            this.expectedUnappliedRule = expectedUnappliedRule;
        }
    }
}
