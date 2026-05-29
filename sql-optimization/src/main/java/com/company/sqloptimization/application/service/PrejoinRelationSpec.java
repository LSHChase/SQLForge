package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.cleanReference;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.qualifier;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.unqualifiedName;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.util.StringUtils;

final class PrejoinRelationSpec {

    final Set<String> references = new LinkedHashSet<String>();

    PrejoinRelationSpec(String tableName, String alias) {
        addReference(alias);
        addReference(tableName);
        addReference(unqualifiedName(tableName));
    }

    boolean owns(String sourceColumn) {
        String sourceQualifier = qualifier(sourceColumn);
        if (!StringUtils.hasText(sourceQualifier)) {
            return false;
        }
        return references.contains(sourceQualifier) || references.contains(unqualifiedName(sourceQualifier));
    }

    private void addReference(String value) {
        if (StringUtils.hasText(value)) {
            references.add(cleanReference(value));
        }
    }
}
