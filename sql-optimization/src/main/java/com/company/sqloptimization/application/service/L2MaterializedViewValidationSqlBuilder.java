package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ValidationSqlValues.immutableMapList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class L2MaterializedViewValidationSqlBuilder {

    private L2MaterializedViewValidationSqlBuilder() {
    }

    static ValidationSqlResult build(ValidationInput input) {
        return ValidationSqlCoordinator.build(input);
    }

    static final class ValidationInput {

        final String mvType;
        final String sourceSql;
        final String rewriteSql;
        final String mvName;
        final Map<String, Object> advancedStructureProfile;
        final List<Map<String, Object>> measures;
        final String commonSubgraphSql;
        final List<String> commonSubgraphOutputColumns;

        ValidationInput(String mvType,
                        String sourceSql,
                        String rewriteSql,
                        String mvName,
                        Map<String, Object> advancedStructureProfile,
                        List<Map<String, Object>> measures,
                        String commonSubgraphSql,
                        List<String> commonSubgraphOutputColumns) {
            this.mvType = mvType;
            this.sourceSql = sourceSql;
            this.rewriteSql = rewriteSql;
            this.mvName = mvName;
            this.advancedStructureProfile = advancedStructureProfile == null
                ? Collections.<String, Object>emptyMap()
                : new LinkedHashMap<String, Object>(advancedStructureProfile);
            this.measures = immutableMapList(measures);
            this.commonSubgraphSql = commonSubgraphSql;
            this.commonSubgraphOutputColumns = commonSubgraphOutputColumns == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(commonSubgraphOutputColumns));
        }
    }

    static final class ValidationSqlResult {

        private final String validationSql;
        private final List<Map<String, Object>> blockingReasons;

        private ValidationSqlResult(String validationSql, List<Map<String, Object>> blockingReasons) {
            this.validationSql = validationSql;
            this.blockingReasons = immutableMapList(blockingReasons);
        }

        static ValidationSqlResult generated(String validationSql) {
            return new ValidationSqlResult(validationSql, Collections.<Map<String, Object>>emptyList());
        }

        static ValidationSqlResult blocked(List<Map<String, Object>> blockingReasons) {
            return new ValidationSqlResult(null, blockingReasons);
        }

        boolean isGenerated() {
            return StringUtils.hasText(validationSql) && blockingReasons.isEmpty();
        }

        String getValidationSql() {
            return validationSql;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }
    }
}
