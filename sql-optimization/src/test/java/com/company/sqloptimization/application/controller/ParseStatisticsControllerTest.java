package com.company.sqloptimization.application.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.application.service.ParseStatisticsApplicationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ParseStatisticsController.class)
class ParseStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ParseStatisticsApplicationService parseStatisticsApplicationService;

    @Test
    void shouldExposeParseStatisticsOverviewEndpoint() throws Exception {
        ParseStatisticsOverviewVO overview = new ParseStatisticsOverviewVO();
        overview.setTotalSqlCount(Integer.valueOf(2));
        overview.setIssueSqlCount(Integer.valueOf(1));
        overview.setTotalIssueCount(Integer.valueOf(3));
        overview.setIssueSceneCount(Integer.valueOf(2));
        overview.setImportantSqlCount(Integer.valueOf(1));
        overview.setUrgentSqlCount(Integer.valueOf(1));
        LinkedHashMap<String, Integer> distribution = new LinkedHashMap<String, Integer>();
        distribution.put("P1", Integer.valueOf(1));
        overview.setPriorityDistribution(distribution);
        when(parseStatisticsApplicationService.overview()).thenReturn(overview);

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse-statistics/overview")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalSqlCount").value(2))
            .andExpect(jsonPath("$.priorityDistribution.P1").value(1));
    }

    @Test
    void shouldExposeParseStatisticsByReportEndpoint() throws Exception {
        ParseReportStatisticVO report = new ParseReportStatisticVO();
        report.setReportCode("RPT_A");
        report.setSqlCount(Integer.valueOf(2));
        report.setIssueSqlCount(Integer.valueOf(1));
        report.setIssueSqlRatio(Double.valueOf(0.5D));
        report.setHighestPriorityLevel("P1");
        when(parseStatisticsApplicationService.byReport()).thenReturn(Collections.singletonList(report));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/parse-statistics/by-report")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reportCode").value("RPT_A"))
            .andExpect(jsonPath("$[0].issueSqlRatio").value(0.5D));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "operator-001")
            .header(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
