package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.firstText;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.cleanReference;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.relationKey;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.unqualifiedName;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class StarAggRelationSpec {

    final String key;
    final String tableName;
    final String alias;
    final Set<String> references = new LinkedHashSet<String>();

    StarAggRelationSpec(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
        this.key = relationKey(firstText(alias, tableName));
        addReference(alias);
        addReference(tableName);
        addReference(unqualifiedName(tableName));
    }

    Map<String, Object> toMap() {
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("tableName", tableName);
        item.put("alias", alias);
        return item;
    }

    private void addReference(String value) {
        if (StringUtils.hasText(value)) {
            references.add(cleanReference(value));
        }
    }
}
