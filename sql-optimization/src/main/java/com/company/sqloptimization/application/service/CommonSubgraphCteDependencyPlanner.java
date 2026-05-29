package com.company.sqloptimization.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class CommonSubgraphCteDependencyPlanner {

    private CommonSubgraphCteDependencyPlanner() {
    }

    static LinkedHashSet<String> materializedCteNames(List<Map<String, Object>> ctes, int candidateIndex) {
        LinkedHashSet<Integer> dependencyIndexes = new LinkedHashSet<Integer>();
        collectCteDependencyIndexes(ctes, candidateIndex, dependencyIndexes, new LinkedHashSet<Integer>());
        dependencyIndexes.add(Integer.valueOf(candidateIndex));
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (Integer index : dependencyIndexes) {
            if (index == null || index.intValue() < 0 || index.intValue() >= ctes.size()) {
                continue;
            }
            CommonSubgraphSqlText.addIfText(names, cteName(ctes.get(index.intValue())));
        }
        return names;
    }

    static String expandedCteSubgraphSql(List<Map<String, Object>> ctes,
                                         int candidateIndex,
                                         Set<String> materializedCteNames) {
        String query = CommonSubgraphSqlText.normalizeSubgraphSql(
            CommonSubgraphSqlText.text(ctes.get(candidateIndex).get("query"))
        );
        if (materializedCteNames == null || materializedCteNames.size() <= 1) {
            return query;
        }
        String candidateName = cteName(ctes.get(candidateIndex));
        StringBuilder builder = new StringBuilder();
        builder.append("WITH ");
        boolean first = true;
        for (int index = 0; index < ctes.size(); index++) {
            String name = cteName(ctes.get(index));
            if (!CommonSubgraphRelationName.containsRelationName(materializedCteNames, name)
                || CommonSubgraphRelationName.relationKey(name).equals(
                    CommonSubgraphRelationName.relationKey(candidateName)
                )) {
                continue;
            }
            if (!first) {
                builder.append(", ");
            }
            builder.append(CommonSubgraphSqlText.cleanIdentifier(name))
                .append(" AS (")
                .append(CommonSubgraphSqlText.normalizeSubgraphSql(
                    CommonSubgraphSqlText.text(ctes.get(index).get("query"))
                ))
                .append(")");
            first = false;
        }
        if (first) {
            return query;
        }
        builder.append(" ").append(query);
        return builder.toString();
    }

    static boolean isRecursiveCteCandidate(List<Map<String, Object>> ctes, int index) {
        if (ctes == null || index < 0 || index >= ctes.size()) {
            return false;
        }
        Map<String, Object> cte = ctes.get(index);
        if (!Boolean.TRUE.equals(cte.get("recursive"))) {
            return false;
        }
        String name = cteName(cte);
        String query = CommonSubgraphSqlText.text(cte.get("query"));
        if (!StringUtils.hasText(name)) {
            return true;
        }
        if (CommonSubgraphRelationReferences.relationReferenced(query, name)) {
            return true;
        }
        return ctes.size() == 1;
    }

    private static void collectCteDependencyIndexes(List<Map<String, Object>> ctes,
                                                    int cteIndex,
                                                    LinkedHashSet<Integer> result,
                                                    Set<Integer> visiting) {
        if (ctes == null || cteIndex < 0 || cteIndex >= ctes.size() || visiting.contains(Integer.valueOf(cteIndex))) {
            return;
        }
        visiting.add(Integer.valueOf(cteIndex));
        Map<String, Object> cte = ctes.get(cteIndex);
        LinkedHashSet<String> knownNames = new LinkedHashSet<String>();
        for (int index = 0; index < ctes.size(); index++) {
            if (index != cteIndex) {
                CommonSubgraphRelationName.addRelationKey(knownNames, cteName(ctes.get(index)));
            }
        }
        for (String dependencyName : referencedCteNames(CommonSubgraphSqlText.text(cte.get("query")), knownNames)) {
            int dependencyIndex = findCteIndex(ctes, dependencyName);
            if (dependencyIndex < 0 || dependencyIndex == cteIndex) {
                continue;
            }
            collectCteDependencyIndexes(ctes, dependencyIndex, result, visiting);
            result.add(Integer.valueOf(dependencyIndex));
        }
        visiting.remove(Integer.valueOf(cteIndex));
    }

    private static LinkedHashSet<String> referencedCteNames(String query, Set<String> knownNames) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (!StringUtils.hasText(query) || knownNames == null || knownNames.isEmpty()) {
            return result;
        }
        try {
            LinkedHashSet<String> relations = new LinkedHashSet<String>();
            CommonSubgraphRelationReferences.collectRelationReferences(
                CommonSubgraphSqlParser.parseStatement(query),
                relations
            );
            for (String relation : relations) {
                String key = CommonSubgraphRelationName.relationKey(relation);
                String unqualified = CommonSubgraphRelationName.relationKey(
                    CommonSubgraphRelationName.unqualifiedName(relation)
                );
                if (knownNames.contains(key) || knownNames.contains(unqualified)) {
                    result.add(CommonSubgraphRelationName.unqualifiedName(relation));
                }
            }
        } catch (RuntimeException ex) {
            return result;
        }
        return result;
    }

    private static int findCteIndex(List<Map<String, Object>> ctes, String dependencyName) {
        String expected = CommonSubgraphRelationName.relationKey(dependencyName);
        for (int index = 0; index < ctes.size(); index++) {
            String actual = CommonSubgraphRelationName.relationKey(cteName(ctes.get(index)));
            String actualTail = CommonSubgraphRelationName.relationKey(CommonSubgraphRelationName.unqualifiedName(actual));
            String expectedTail = CommonSubgraphRelationName.relationKey(
                CommonSubgraphRelationName.unqualifiedName(expected)
            );
            if (actual.equals(expected) || actualTail.equals(expectedTail)) {
                return index;
            }
        }
        return -1;
    }

    private static String cteName(Map<String, Object> cte) {
        return CommonSubgraphSqlText.text(cte == null ? null : cte.get("name"));
    }
}
