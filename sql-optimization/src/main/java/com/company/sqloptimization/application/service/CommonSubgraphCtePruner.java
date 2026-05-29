package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.parser.SqlParserPos;

final class CommonSubgraphCtePruner {

    private CommonSubgraphCtePruner() {
    }

    static SqlNode pruneMaterializedAndUnusedCtes(SqlWith with, Set<String> materializedCteNames) {
        if (with == null || with.withList == null || with.withList.isEmpty()) {
            return with == null ? null : with.body;
        }
        LinkedHashMap<String, SqlWithItem> remainingByName = new LinkedHashMap<String, SqlWithItem>();
        for (SqlNode item : with.withList.getList()) {
            if (!(item instanceof SqlWithItem)) {
                continue;
            }
            SqlWithItem withItem = (SqlWithItem) item;
            String name = withItem.name == null ? "" : withItem.name.toString();
            if (CommonSubgraphRelationName.containsRelationName(materializedCteNames, name)) {
                continue;
            }
            remainingByName.put(CommonSubgraphRelationName.relationKey(name), withItem);
        }
        if (remainingByName.isEmpty()) {
            return with.body;
        }
        LinkedHashSet<String> neededNames = neededRemainingCtes(with.body, remainingByName);
        List<SqlNode> retained = new ArrayList<SqlNode>();
        for (Map.Entry<String, SqlWithItem> entry : remainingByName.entrySet()) {
            if (neededNames.contains(entry.getKey())) {
                retained.add(entry.getValue());
            }
        }
        if (retained.isEmpty()) {
            return with.body;
        }
        with.withList = new SqlNodeList(
            retained,
            with.withList.getParserPosition() == null ? SqlParserPos.ZERO : with.withList.getParserPosition()
        );
        return with;
    }

    private static LinkedHashSet<String> neededRemainingCtes(SqlNode body,
                                                             Map<String, SqlWithItem> remainingByName) {
        LinkedHashSet<String> neededNames = new LinkedHashSet<String>();
        addReferencedRemainingCtes(body, remainingByName, neededNames);
        boolean changed = true;
        while (changed) {
            changed = false;
            List<String> snapshot = new ArrayList<String>(neededNames);
            for (String name : snapshot) {
                SqlWithItem item = remainingByName.get(name);
                int before = neededNames.size();
                if (item != null) {
                    addReferencedRemainingCtes(item.query, remainingByName, neededNames);
                }
                changed = changed || neededNames.size() > before;
            }
        }
        return neededNames;
    }

    private static void addReferencedRemainingCtes(SqlNode node,
                                                   Map<String, SqlWithItem> remainingByName,
                                                   Set<String> neededNames) {
        if (node == null || remainingByName == null || remainingByName.isEmpty()) {
            return;
        }
        LinkedHashSet<String> relations = new LinkedHashSet<String>();
        CommonSubgraphRelationReferences.collectRelationReferences(node, relations);
        for (String relation : relations) {
            String key = CommonSubgraphRelationName.relationKey(relation);
            String unqualified = CommonSubgraphRelationName.relationKey(
                CommonSubgraphRelationName.unqualifiedName(relation)
            );
            if (remainingByName.containsKey(key)) {
                neededNames.add(key);
            }
            if (remainingByName.containsKey(unqualified)) {
                neededNames.add(unqualified);
            }
        }
    }
}
