package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PredicateClassifierValues.containsToken;
import static com.company.sqloptimization.application.service.PredicateClassifierValues.mapList;
import static com.company.sqloptimization.application.service.PredicateClassifierValues.stringList;
import static com.company.sqloptimization.application.service.PredicateClassifierValues.upperText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class PredicateClassifierEngine {

    private static final Set<String> SUPPORTED_CLAUSES =
        new LinkedHashSet<String>(Arrays.asList("WHERE", "HAVING"));
    private static final Set<String> UNSTABLE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP", "LOCALTIME", "LOCALTIMESTAMP",
            "NOW", "RAND", "RANDOM", "UUID", "CURRENT_USER", "SESSION_USER"
        ));
    private static final List<String> PARAMETER_COLUMNS =
        Collections.unmodifiableList(Arrays.asList(
            "BIZ_DATE", "QUERY_DATE", "ORDER_DATE", "TRADE_DATE", "STAT_DATE", "PARTITION_DATE",
            "DATE", "DT", "DAY", "MONTH", "REGION_ID", "REGION", "PROVINCE", "CITY", "AREA",
            "CHANNEL_ID", "CHANNEL", "SOURCE_CHANNEL", "CUSTOMER_ID", "CUSTOMER", "CUST_ID",
            "USER_ID", "MEMBER_ID", "PRODUCT_ID", "PRODUCT", "SKU_ID", "ITEM_ID", "GOODS_ID",
            "TENANT_ID", "TENANT"
        ));
    private static final List<String> RETAINED_COLUMNS =
        Collections.unmodifiableList(Arrays.asList(
            "STATUS", "STATE", "ORDER_STATUS", "PAY_STATUS", "RECORD_STATUS", "IS_DELETED",
            "DELETED", "DELETE_FLAG", "DEL_FLAG", "IS_ACTIVE", "ACTIVE_FLAG", "ENABLED",
            "ENABLE_FLAG", "IS_VALID", "VALID_FLAG"
        ));
    private static final List<String> SECURITY_COLUMNS =
        Collections.unmodifiableList(Arrays.asList(
            "PERMISSION_ID", "PERMISSION_DOMAIN", "PERMISSION_SCOPE", "AUTH_DOMAIN", "AUTH_SCOPE",
            "ACL_ID", "ACL_SCOPE", "ACCESS_DOMAIN", "ACCESS_SCOPE", "DATA_DOMAIN", "DATA_SCOPE",
            "SECURITY_DOMAIN", "SECURITY_LEVEL", "PRIVILEGE_ID", "ROLE_ID", "ORG_SCOPE",
            "RESOURCE_SCOPE"
        ));

    private PredicateClassifierEngine() {
    }

    static L2PredicateClassifier.PredicateClassificationResult classify(Map<String, Object> advancedStructureProfile) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return L2PredicateClassifier.PredicateClassificationResult.empty();
        }
        List<Map<String, Object>> predicates = mapList(advancedStructureProfile.get("predicates"));
        if (predicates.isEmpty()) {
            return L2PredicateClassifier.PredicateClassificationResult.empty();
        }
        Set<String> signalFunctions = unstableSignalFunctions(advancedStructureProfile);
        List<Map<String, Object>> externalized = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> retained = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> security = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> blocked = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> predicate : predicates) {
            classifyPredicate(predicate, signalFunctions, externalized, retained, security, blocked);
        }
        return new L2PredicateClassifier.PredicateClassificationResult(externalized, retained, security, blocked);
    }

    private static void classifyPredicate(Map<String, Object> predicate,
                                          Set<String> signalFunctions,
                                          List<Map<String, Object>> externalized,
                                          List<Map<String, Object>> retained,
                                          List<Map<String, Object>> security,
                                          List<Map<String, Object>> blocked) {
        if (!isSupportedClause(predicate)) {
            return;
        }
        List<String> matchedFunctions = matchedUnstableFunctions(predicate, signalFunctions);
        if (!matchedFunctions.isEmpty()) {
            blocked.add(classified(predicate, L2PredicateClassifier.BLOCKED_UNSTABLE_PREDICATE,
                "UNSTABLE_OR_CONTEXT_FUNCTION", matchedFunctions));
        } else if (matchesAny(predicate, PARAMETER_COLUMNS)) {
            externalized.add(classified(predicate, L2PredicateClassifier.EXTERNALIZED_PARAMETER_PREDICATE,
                "PARAMETER_FILTER_FIELD", Collections.<String>emptyList()));
        } else if (matchesAny(predicate, SECURITY_COLUMNS)) {
            security.add(classified(predicate, L2PredicateClassifier.SECURITY_PREDICATE,
                "EXPLICIT_NON_TENANT_SECURITY_BOUNDARY", Collections.<String>emptyList()));
        } else if (matchesAny(predicate, RETAINED_COLUMNS)) {
            retained.add(classified(predicate, L2PredicateClassifier.RETAINED_BUSINESS_PREDICATE,
                "FIXED_BUSINESS_FILTER_FIELD", Collections.<String>emptyList()));
        } else {
            retained.add(classified(predicate, L2PredicateClassifier.RETAINED_BUSINESS_PREDICATE,
                "DEFAULT_RETAINED_FILTER", Collections.<String>emptyList()));
        }
    }

    private static boolean isSupportedClause(Map<String, Object> predicate) {
        String clause = upperText(predicate == null ? null : predicate.get("clause"));
        return SUPPORTED_CLAUSES.contains(clause);
    }

    private static Map<String, Object> classified(Map<String, Object> predicate,
                                                  String classification,
                                                  String reason,
                                                  List<String> matchedFunctions) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        if (predicate != null) {
            item.putAll(predicate);
        }
        item.put("classification", classification);
        item.put("classificationReason", reason);
        if (matchedFunctions != null && !matchedFunctions.isEmpty()) {
            item.put("unstableFunctions", new ArrayList<String>(matchedFunctions));
        }
        return item;
    }

    private static List<String> matchedUnstableFunctions(Map<String, Object> predicate, Set<String> signalFunctions) {
        LinkedHashSet<String> matches = new LinkedHashSet<String>();
        for (String functionName : stringList(predicate == null ? null : predicate.get("functionNames"))) {
            String upperName = upperText(functionName);
            if (UNSTABLE_FUNCTIONS.contains(upperName)) {
                matches.add(upperName);
            }
        }
        String expression = upperText(predicate == null ? null : predicate.get("expression"));
        for (String functionName : UNSTABLE_FUNCTIONS) {
            if (containsToken(expression, functionName)) {
                matches.add(functionName);
            }
        }
        for (String functionName : signalFunctions) {
            if (containsToken(expression, functionName)) {
                matches.add(functionName);
            }
        }
        return new ArrayList<String>(matches);
    }

    private static Set<String> unstableSignalFunctions(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> functions = new LinkedHashSet<String>();
        collectUnstableSignalFunctions(functions, mapList(advancedStructureProfile.get("timeFunctions")));
        collectUnstableSignalFunctions(functions, mapList(advancedStructureProfile.get("nonDeterministicFunctions")));
        return functions;
    }

    private static void collectUnstableSignalFunctions(Set<String> functions, List<Map<String, Object>> signals) {
        for (Map<String, Object> signal : signals) {
            String functionName = upperText(signal.get("functionName"));
            if (UNSTABLE_FUNCTIONS.contains(functionName)) {
                functions.add(functionName);
            }
        }
    }

    private static boolean matchesAny(Map<String, Object> predicate, List<String> columnNames) {
        String text = predicateText(predicate);
        for (String columnName : columnNames) {
            if (containsToken(text, columnName)) {
                return true;
            }
        }
        return false;
    }

    private static String predicateText(Map<String, Object> predicate) {
        StringBuilder builder = new StringBuilder();
        appendText(builder, predicate == null ? null : predicate.get("expression"));
        for (String sourceColumn : stringList(predicate == null ? null : predicate.get("sourceColumns"))) {
            appendText(builder, sourceColumn);
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }

    private static void appendText(StringBuilder builder, Object value) {
        if (builder != null && value != null) {
            builder.append(' ');
            builder.append(String.valueOf(value));
        }
    }
}
