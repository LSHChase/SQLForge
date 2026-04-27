package com.company.sqloptimization.application.service.report;

import java.time.LocalDate;
import org.springframework.util.StringUtils;

public final class MockReportSqlFactory {

    private MockReportSqlFactory() {
    }

    public static ReportSqlResolveResult resolve(ReportSqlResolveRequest request, String fallbackReason) {
        String reportCode = firstNonBlank(request == null ? null : request.getReportCode(), "UNSPECIFIED_REPORT");
        String stage = firstNonBlank(request == null ? null : request.getStage(), "PROD");
        String sql = "SELECT '"
            + escapeSqlLiteral(reportCode)
            + "' AS report_code, '"
            + escapeSqlLiteral(stage)
            + "' AS stage, dt, COUNT(*) AS metric_value FROM mock_report_source WHERE report_code = '"
            + escapeSqlLiteral(reportCode)
            + "' AND dt = '"
            + escapeSqlLiteral(LocalDate.now().toString())
            + "' GROUP BY dt";
        return new ReportSqlResolveResult(sql, "TXT_MOCK_SOURCE", "MOCK_FALLBACK", fallbackReason);
    }

    private static String firstNonBlank(String first, String fallback) {
        return StringUtils.hasText(first) ? first.trim() : fallback;
    }

    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
