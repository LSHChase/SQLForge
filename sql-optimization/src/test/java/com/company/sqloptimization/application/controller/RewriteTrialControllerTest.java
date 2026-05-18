package com.company.sqloptimization.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.dto.RewriteTrialRequest;
import com.company.sqloptimization.application.controller.vo.RewriteTrialItemVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialRunVO;
import com.company.sqloptimization.application.service.RewriteTrialApplicationService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(RewriteTrialController.class)
class RewriteTrialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewriteTrialApplicationService rewriteTrialApplicationService;

    @Test
    void shouldCreateAndFetchRewriteTrialRun() throws Exception {
        RewriteTrialRunVO run = trialRun();
        when(rewriteTrialApplicationService.createTrial(any(RewriteTrialRequest.class))).thenReturn(run);
        when(rewriteTrialApplicationService.getTrial("trial-run-001")).thenReturn(run);

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/rewrite-trials"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sqlText\":\"SELECT COUNT(1) FROM orders\","
                    + "\"datasourceCode\":\"hetu_main\",\"sourceKind\":\"STRUCTURE_PARSE\","
                    + "\"parseHistoryId\":\"parse-history-001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value("trial-run-001"))
            .andExpect(jsonPath("$.trialStatus").value("RECOMMENDED"))
            .andExpect(jsonPath("$.items[0].sourceProblems[0].issueScene").value("COUNT_LITERAL_TO_COUNT_STAR"))
            .andExpect(jsonPath("$.items[0].issueRuleLinks[0].trialConclusion").value("CANDIDATE_GENERATED"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/rewrite-trials/trial-run-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value("trial-run-001"))
            .andExpect(jsonPath("$.items[0].recommendationId").value("rewrite-reco-001"));
    }

    private RewriteTrialRunVO trialRun() {
        RewriteTrialItemVO item = new RewriteTrialItemVO();
        item.setTrialItemId("trial-item-001");
        item.setRunId("trial-run-001");
        item.setTrialStatus("RECOMMENDED");
        item.setRecommendationId("rewrite-reco-001");
        item.setCandidateSql("SELECT COUNT(*) FROM orders");
        item.setSourceProblems(Collections.singletonList(problem("COUNT_LITERAL_TO_COUNT_STAR")));
        item.setIssueRuleLinks(Collections.singletonList(ruleLink("COUNT_LITERAL_TO_COUNT_STAR")));

        RewriteTrialRunVO run = new RewriteTrialRunVO();
        run.setRunId("trial-run-001");
        run.setTenantId("tenant-a");
        run.setSourceKind("STRUCTURE_PARSE");
        run.setTrialStatus("RECOMMENDED");
        run.setTotalCount(Integer.valueOf(1));
        run.setAcceptedCount(Integer.valueOf(1));
        run.setSkippedCount(Integer.valueOf(0));
        run.setRecommendedCount(Integer.valueOf(1));
        run.setItems(Collections.singletonList(item));
        return run;
    }

    private Map<String, Object> problem(String issueScene) {
        LinkedHashMap<String, Object> problem = new LinkedHashMap<String, Object>();
        problem.put("problemType", "REWRITE_CANDIDATE");
        problem.put("issueScene", issueScene);
        problem.put("severity", "LOW");
        return problem;
    }

    private Map<String, Object> ruleLink(String issueScene) {
        LinkedHashMap<String, Object> link = new LinkedHashMap<String, Object>();
        link.put("sourceIssueScene", issueScene);
        link.put("ruleCode", "COUNT_ONE_TO_COUNT_STAR");
        link.put("ruleLevel", "L0");
        link.put("ruleAction", "APPLIED");
        link.put("trialConclusion", "CANDIDATE_GENERATED");
        return link;
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
