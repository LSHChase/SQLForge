package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class L2PredicateClassifierTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldClassifyParameterBusinessSecurityAndHavingPredicates() {
        Map<String, Object> profile = advancedProfile(
            Arrays.asList(
                predicate("WHERE", "dt >= DATE '2026-05-01'", "dt"),
                predicate("WHERE", "region_id = 'CN'", "region_id"),
                predicate("WHERE", "channel = 'APP'", "channel"),
                predicate("WHERE", "customer_id = 7", "customer_id"),
                predicate("WHERE", "product_id IN (1, 2)", "product_id"),
                predicate("WHERE", "tenant_id = 'tenant-a'", "tenant_id"),
                predicate("WHERE", "status = 'PAID'", "status"),
                predicate("WHERE", "is_deleted = 0", "is_deleted"),
                predicate("WHERE", "access_domain = 'BI'", "access_domain"),
                predicate("HAVING", "SUM(amount) > 100", "amount", "SUM")
            ),
            Collections.<Map<String, Object>>emptyList(),
            Collections.<Map<String, Object>>emptyList()
        );

        L2PredicateClassifier.PredicateClassificationResult result = L2PredicateClassifier.classify(profile);

        assertEquals(6, result.getExternalizedPredicates().size());
        assertTrue(hasExpression(result.getExternalizedPredicates(), "tenant_id"));
        assertTrue(hasExpression(result.getExternalizedPredicates(), "product_id"));
        assertEquals(3, result.getRetainedPredicates().size());
        assertTrue(hasExpression(result.getRetainedPredicates(), "status"));
        assertTrue(hasExpression(result.getRetainedPredicates(), "SUM(amount)"));
        assertEquals(1, result.getSecurityPredicates().size());
        assertTrue(hasExpression(result.getSecurityPredicates(), "access_domain"));
        assertTrue(result.getBlockedPredicates().isEmpty());
    }

    @Test
    void shouldClassifyCurrentRandomAndSessionFunctionsAsBlockedPredicates() {
        Map<String, Object> profile = advancedProfile(
            Arrays.asList(
                predicate("WHERE", "dt >= CURRENT_DATE", "dt", "CURRENT_DATE"),
                predicate("WHERE", "RAND() > 0.5", "", "RAND"),
                predicate("HAVING", "SUM(amount) > 0 AND CURRENT_USER = owner_id", "owner_id", "CURRENT_USER"),
                predicate("JOIN_ON", "o.customer_id = c.customer_id", "o.customer_id")
            ),
            Collections.singletonList(signal("CURRENT_DATE")),
            Arrays.asList(signal("RAND"), signal("CURRENT_USER"))
        );

        L2PredicateClassifier.PredicateClassificationResult result = L2PredicateClassifier.classify(profile);

        assertEquals(3, result.getBlockedPredicates().size());
        assertTrue(hasExpression(result.getBlockedPredicates(), "CURRENT_DATE"));
        assertTrue(hasExpression(result.getBlockedPredicates(), "RAND()"));
        assertTrue(hasExpression(result.getBlockedPredicates(), "CURRENT_USER"));
        assertTrue(result.hasBlockedPredicates());
    }

    @Test
    void shouldExposePredicateClassificationArraysOnGeneratedArtifact() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt >= DATE '2026-05-01' AND tenant_id = 'tenant-a' AND status = 'PAID' "
                + "GROUP BY customer_id HAVING SUM(amount) > 100",
            DataSourceTypeEnum.HETU
        );

        Map<String, Object> artifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            input("report-sales"),
            profile
        );

        assertEquals("GENERATED", artifact.get("artifactStatus"));
        assertTrue(hasExpression(maps(artifact.get("externalizedPredicates")), "dt >="));
        assertTrue(hasExpression(maps(artifact.get("externalizedPredicates")), "tenant_id"));
        assertTrue(hasExpression(maps(artifact.get("retainedPredicates")), "status"));
        assertTrue(hasExpression(maps(artifact.get("retainedPredicates")), "SUM(amount)"));
        assertTrue(maps(artifact.get("securityPredicates")).isEmpty());
        assertTrue(maps(artifact.get("blockedPredicates")).isEmpty());
        assertTrue(String.valueOf(artifact.get("ddlSql")).contains("CREATE MATERIALIZED VIEW"));
    }

    @Test
    void shouldBlockArtifactAndOmitPublishableSqlForUnstablePredicates() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT customer_id, SUM(amount) AS total_amount FROM orders "
                + "WHERE dt >= CURRENT_DATE GROUP BY customer_id",
            DataSourceTypeEnum.HETU
        );

        Map<String, Object> artifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            input("report-unstable"),
            profile
        );

        assertEquals("BLOCKED", artifact.get("artifactStatus"));
        assertTrue(hasExpression(maps(artifact.get("blockedPredicates")), "CURRENT_DATE"));
        assertTrue(hasReason(maps(artifact.get("blockingReasons")), "BLOCKED_UNSTABLE_PREDICATE"));
        assertNull(artifact.get("ddlSql"));
        assertNull(artifact.get("rewriteSql"));
    }

    @Test
    void shouldPreserveOrPredicateLogicalContextAndGroupId() {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(
            "SELECT region, SUM(amount) AS total_amount FROM orders "
                + "WHERE (region = 'CN' OR region = 'US') AND status = 'PAID' GROUP BY region",
            DataSourceTypeEnum.HETU
        );

        List<Map<String, Object>> predicates = maps(profile.toAdvancedStructureProfile().get("predicates"));
        List<Map<String, Object>> regionPredicates = expressionsContaining(predicates, "region =");

        assertEquals(2, regionPredicates.size());
        assertEquals("OR", regionPredicates.get(0).get("logicalContext"));
        assertEquals("OR", regionPredicates.get(1).get("logicalContext"));
        assertFalse(String.valueOf(regionPredicates.get(0).get("groupId")).isEmpty());
        assertEquals(regionPredicates.get(0).get("groupId"), regionPredicates.get(1).get("groupId"));
        assertTrue(hasAndPredicateWithoutGroup(predicates, "status ="));
    }

    private static L2AccelerationArtifactBuilder.AccelerationRecommendationInput input(String reportCode) {
        return new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
            null,
            "HETU",
            "datasource-a",
            "fingerprint-001",
            reportCode,
            null,
            null
        );
    }

    private static Map<String, Object> advancedProfile(List<Map<String, Object>> predicates,
                                                       List<Map<String, Object>> timeFunctions,
                                                       List<Map<String, Object>> nonDeterministicFunctions) {
        LinkedHashMap<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("predicates", predicates);
        profile.put("timeFunctions", timeFunctions);
        profile.put("nonDeterministicFunctions", nonDeterministicFunctions);
        return profile;
    }

    private static Map<String, Object> predicate(String clause,
                                                 String expression,
                                                 String sourceColumn,
                                                 String... functions) {
        List<String> sourceColumns = sourceColumn == null || sourceColumn.isEmpty()
            ? Collections.<String>emptyList()
            : Collections.singletonList(sourceColumn);
        return predicate(clause, expression, sourceColumns, functions);
    }

    private static Map<String, Object> predicate(String clause,
                                                 String expression,
                                                 List<String> sourceColumns,
                                                 String... functions) {
        LinkedHashMap<String, Object> predicate = new LinkedHashMap<String, Object>();
        predicate.put("clause", clause);
        predicate.put("expression", expression);
        predicate.put("sourceColumns", sourceColumns);
        predicate.put("functionNames", Arrays.asList(functions));
        predicate.put("logicalContext", "AND");
        predicate.put("groupId", "");
        return predicate;
    }

    private static Map<String, Object> signal(String functionName) {
        LinkedHashMap<String, Object> signal = new LinkedHashMap<String, Object>();
        signal.put("functionName", functionName);
        signal.put("expression", functionName);
        return signal;
    }

    private static boolean hasExpression(List<Map<String, Object>> predicates, String expected) {
        for (Map<String, Object> predicate : predicates) {
            if (String.valueOf(predicate.get("expression")).contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasReason(List<Map<String, Object>> reasons, String code) {
        for (Map<String, Object> reason : reasons) {
            if (code.equals(reason.get("code"))) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, Object>> expressionsContaining(List<Map<String, Object>> predicates,
                                                                   String expected) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> predicate : predicates) {
            if (String.valueOf(predicate.get("expression")).contains(expected)) {
                result.add(predicate);
            }
        }
        return result;
    }

    private static boolean hasAndPredicateWithoutGroup(List<Map<String, Object>> predicates, String expected) {
        for (Map<String, Object> predicate : predicates) {
            if (String.valueOf(predicate.get("expression")).contains(expected)
                && "AND".equals(predicate.get("logicalContext"))
                && String.valueOf(predicate.get("groupId")).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
