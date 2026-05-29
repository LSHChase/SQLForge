package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class CommonSubgraphProfileCandidateExtractor {

    private CommonSubgraphProfileCandidateExtractor() {
    }

    static List<CommonSubgraphCandidate> subgraphCandidates(Map<String, Object> advancedStructureProfile) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return Collections.emptyList();
        }
        List<CommonSubgraphCandidate> result = new ArrayList<CommonSubgraphCandidate>();
        List<Map<String, Object>> ctes = mapList(advancedStructureProfile.get("ctes"));
        for (int index = 0; index < ctes.size(); index++) {
            Map<String, Object> cte = ctes.get(index);
            String query = CommonSubgraphSqlText.normalizeSubgraphSql(CommonSubgraphSqlText.text(cte.get("query")));
            if (!StringUtils.hasText(query)) {
                continue;
            }
            LinkedHashSet<String> materializedCteNames =
                CommonSubgraphCteDependencyPlanner.materializedCteNames(ctes, index);
            result.add(new CommonSubgraphCandidate(
                "CTE",
                CommonSubgraphSqlText.text(cte.get("name")),
                CommonSubgraphSqlText.text(cte.get("name")),
                CommonSubgraphCteDependencyPlanner.expandedCteSubgraphSql(ctes, index, materializedCteNames),
                CommonSubgraphCteDependencyPlanner.isRecursiveCteCandidate(ctes, index),
                materializedCteNames
            ));
        }
        for (Map<String, Object> subquery : mapList(advancedStructureProfile.get("subqueries"))) {
            String location = CommonSubgraphSqlText.upperText(subquery.get("location"));
            if (!"FROM".equals(location)) {
                continue;
            }
            String alias = CommonSubgraphSqlText.text(subquery.get("alias"));
            String query = CommonSubgraphSqlText.normalizeSubgraphSql(CommonSubgraphSqlText.removeTrailingAlias(
                CommonSubgraphSqlText.text(subquery.get("query")),
                alias
            ));
            if (!StringUtils.hasText(query)) {
                continue;
            }
            result.add(new CommonSubgraphCandidate(
                "DERIVED_TABLE",
                CommonSubgraphSqlText.firstText(alias, CommonSubgraphSqlText.text(subquery.get("subqueryId"))),
                alias,
                query,
                false
            ));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }
}
