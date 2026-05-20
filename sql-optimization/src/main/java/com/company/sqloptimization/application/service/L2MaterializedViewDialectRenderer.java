package com.company.sqloptimization.application.service;

import java.util.Locale;
import org.springframework.util.StringUtils;

final class L2MaterializedViewDialectRenderer {

    private L2MaterializedViewDialectRenderer() {
    }

    static boolean supports(String targetEngine) {
        String engine = normalizeEngine(targetEngine);
        return "HETU".equals(engine) || "HIVE".equals(engine) || "SPARK".equals(engine);
    }

    static String dialect(String targetEngine) {
        String engine = normalizeEngine(targetEngine);
        if ("HETU".equals(engine)) {
            return "HETU_MATERIALIZED_VIEW";
        }
        if ("HIVE".equals(engine)) {
            return "HIVE_MATERIALIZED_VIEW";
        }
        if ("SPARK".equals(engine)) {
            return "SPARK_TABLE_AS_SELECT";
        }
        return "UNRESOLVED";
    }

    static RenderedSql render(String targetEngine, String mvName, String selectSql) {
        String engine = normalizeEngine(targetEngine);
        String selectBody = trimTrailingSemicolon(selectSql);
        if (!supports(engine) || !StringUtils.hasText(mvName) || !StringUtils.hasText(selectBody)) {
            return null;
        }
        if ("SPARK".equals(engine)) {
            return new RenderedSql(
                "CREATE TABLE " + mvName + " AS\n" + selectBody + ";",
                "INSERT OVERWRITE TABLE " + mvName + "\n" + selectBody + ";",
                "DROP TABLE IF EXISTS " + mvName + ";"
            );
        }
        if ("HIVE".equals(engine)) {
            return new RenderedSql(
                "CREATE MATERIALIZED VIEW " + mvName + " AS\n" + selectBody + ";",
                "ALTER MATERIALIZED VIEW " + mvName + " REBUILD;",
                "DROP MATERIALIZED VIEW " + mvName + ";"
            );
        }
        return new RenderedSql(
            "CREATE MATERIALIZED VIEW " + mvName + " AS\n" + selectBody + ";",
            "REFRESH MATERIALIZED VIEW " + mvName + ";",
            "DROP MATERIALIZED VIEW " + mvName + ";"
        );
    }

    private static String normalizeEngine(String targetEngine) {
        return StringUtils.hasText(targetEngine) ? targetEngine.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    static final class RenderedSql {

        private final String ddlSql;
        private final String refreshSql;
        private final String rollbackSql;

        private RenderedSql(String ddlSql, String refreshSql, String rollbackSql) {
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.rollbackSql = rollbackSql;
        }

        String getDdlSql() {
            return ddlSql;
        }

        String getRefreshSql() {
            return refreshSql;
        }

        String getRollbackSql() {
            return rollbackSql;
        }
    }
}
