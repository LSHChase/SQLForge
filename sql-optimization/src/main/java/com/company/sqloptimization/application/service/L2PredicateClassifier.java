package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class L2PredicateClassifier {

    static final String EXTERNALIZED_PARAMETER_PREDICATE = "EXTERNALIZED_PARAMETER_PREDICATE";
    static final String RETAINED_BUSINESS_PREDICATE = "RETAINED_BUSINESS_PREDICATE";
    static final String SECURITY_PREDICATE = "SECURITY_PREDICATE";
    static final String BLOCKED_UNSTABLE_PREDICATE = "BLOCKED_UNSTABLE_PREDICATE";

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

    private L2PredicateClassifier() {
    }

    static PredicateClassificationResult classify(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return classify(profile == null ? null : profile.toAdvancedStructureProfile());
    }

    static PredicateClassificationResult classify(Map<String, Object> advancedStructureProfile) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return PredicateClassificationResult.empty();
        }
        List<Map<String, Object>> predicates = mapList(advancedStructureProfile.get("predicates"));
        if (predicates.isEmpty()) {
            return PredicateClassificationResult.empty();
        }
        Set<String> signalFunctions = unstableSignalFunctions(advancedStructureProfile);
        List<Map<String, Object>> externalized = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> retained = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> security = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> blocked = new ArrayList<Map<String, Object>>();

        for (Map<String, Object> predicate : predicates) {
            if (!isSupportedClause(predicate)) {
                continue;
            }
            List<String> matchedFunctions = matchedUnstableFunctions(predicate, signalFunctions);
            if (!matchedFunctions.isEmpty()) {
                blocked.add(classified(
                    predicate,
                    BLOCKED_UNSTABLE_PREDICATE,
                    "UNSTABLE_OR_CONTEXT_FUNCTION",
                    matchedFunctions
                ));
            } else if (matchesAny(predicate, PARAMETER_COLUMNS)) {
                externalized.add(classified(
                    predicate,
                    EXTERNALIZED_PARAMETER_PREDICATE,
                    "PARAMETER_FILTER_FIELD",
                    Collections.<String>emptyList()
                ));
            } else if (matchesAny(predicate, SECURITY_COLUMNS)) {
                security.add(classified(
                    predicate,
                    SECURITY_PREDICATE,
                    "EXPLICIT_NON_TENANT_SECURITY_BOUNDARY",
                    Collections.<String>emptyList()
                ));
            } else if (matchesAny(predicate, RETAINED_COLUMNS)) {
                retained.add(classified(
                    predicate,
                    RETAINED_BUSINESS_PREDICATE,
                    "FIXED_BUSINESS_FILTER_FIELD",
                    Collections.<String>emptyList()
                ));
            } else {
                retained.add(classified(
                    predicate,
                    RETAINED_BUSINESS_PREDICATE,
                    "DEFAULT_RETAINED_FILTER",
                    Collections.<String>emptyList()
                ));
            }
        }
        return new PredicateClassificationResult(externalized, retained, security, blocked);
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
        if (builder == null || value == null) {
            return;
        }
        builder.append(' ');
        builder.append(String.valueOf(value));
    }

    private static boolean containsToken(String text, String token) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(token)) {
            return false;
        }
        String normalizedText = text.toUpperCase(Locale.ROOT).replace('`', ' ').replace('"', ' ');
        String normalizedToken = token.toUpperCase(Locale.ROOT);
        Pattern pattern = Pattern.compile("(^|[^A-Z0-9_])" + Pattern.quote(normalizedToken) + "([^A-Z0-9_]|$)");
        return pattern.matcher(normalizedText).find();
    }

    private static String upperText(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (Iterable<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add(copyMap((Map<?, ?>) item));
            }
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    static final class PredicateClassificationResult {

        private final List<Map<String, Object>> externalizedPredicates;
        private final List<Map<String, Object>> retainedPredicates;
        private final List<Map<String, Object>> securityPredicates;
        private final List<Map<String, Object>> blockedPredicates;

        private PredicateClassificationResult(List<Map<String, Object>> externalizedPredicates,
                                              List<Map<String, Object>> retainedPredicates,
                                              List<Map<String, Object>> securityPredicates,
                                              List<Map<String, Object>> blockedPredicates) {
            this.externalizedPredicates = immutableCopy(externalizedPredicates);
            this.retainedPredicates = immutableCopy(retainedPredicates);
            this.securityPredicates = immutableCopy(securityPredicates);
            this.blockedPredicates = immutableCopy(blockedPredicates);
        }

        private static PredicateClassificationResult empty() {
            return new PredicateClassificationResult(
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList()
            );
        }

        List<Map<String, Object>> getExternalizedPredicates() {
            return externalizedPredicates;
        }

        List<Map<String, Object>> getRetainedPredicates() {
            return retainedPredicates;
        }

        List<Map<String, Object>> getSecurityPredicates() {
            return securityPredicates;
        }

        List<Map<String, Object>> getBlockedPredicates() {
            return blockedPredicates;
        }

        boolean hasBlockedPredicates() {
            return !blockedPredicates.isEmpty();
        }

        private static List<Map<String, Object>> immutableCopy(List<Map<String, Object>> source) {
            if (source == null || source.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> item : source) {
                result.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(item)));
            }
            return Collections.unmodifiableList(result);
        }
    }
}
