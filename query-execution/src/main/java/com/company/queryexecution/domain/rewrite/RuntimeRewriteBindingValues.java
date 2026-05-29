package com.company.queryexecution.domain.rewrite;

import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RuntimeRewriteBindingValues {

    private RuntimeRewriteBindingValues() {
    }

    static String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        return trimmed;
    }

    static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    static List<String> immutableStringList(List<String> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (String item : value) {
            String normalized = trimToNull(item);
            if (normalized != null && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return Collections.unmodifiableList(result);
    }

    static List<LogicalObjectSurface> immutableSurfaceList(List<LogicalObjectSurface> value) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<LogicalObjectSurface>(value));
    }
}
