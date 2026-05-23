package com.company.sqloptimization.application.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqloptimization.application.controller.vo.RewriteActivationEligibilityReasonVO;
import com.company.sqloptimization.application.controller.vo.RewriteActivationEligibilityVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.application.service.SqlRewriteRecordApplicationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(SqlRewriteRecordController.class)
class SqlRewriteRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SqlRewriteRecordApplicationService sqlRewriteRecordApplicationService;

    @Test
    void shouldExposeRewriteRecordAndValidationRunContractEndpoints() throws Exception {
        SqlRewriteRecordVO record = new SqlRewriteRecordVO();
        record.setRewriteRecordId("rewrite-001");
        record.setTenantId("tenant-a");
        record.setRecommendationId("rec-001");
        record.setSourceType("QUERY");
        record.setSourceKind("QUERY_HISTORY");
        record.setSourceId("history-001");
        record.setEvidenceLevel("RUNTIME_HISTORY");
        record.setValidationStatus("NOT_VALIDATED");
        record.setAutoApplyAllowed(Boolean.FALSE);
        record.setManualReviewRequired(Boolean.TRUE);
        record.setReviewStatus("PENDING_REVIEW");
        record.setActivationStatus("INACTIVE");
        record.setRuntimeBindingId("binding-001");
        record.setRuntimeRuleVersion("rule-v1");
        record.setContractStage("LONG_TERM_BASELINE");
        record.setImplementationStage("ACCELERATION_REWRITE_CONTRACT_BASELINE");
        SqlRewriteRecordVO activatedRecord = new SqlRewriteRecordVO();
        activatedRecord.setRewriteRecordId("rewrite-001");
        activatedRecord.setTenantId("tenant-a");
        activatedRecord.setActivationStatus("ACTIVE");
        SqlRewriteRecordVO pausedRecord = new SqlRewriteRecordVO();
        pausedRecord.setRewriteRecordId("rewrite-001");
        pausedRecord.setTenantId("tenant-a");
        pausedRecord.setActivationStatus("PAUSED");
        SqlRewriteRecordVO approvedRecord = new SqlRewriteRecordVO();
        approvedRecord.setRewriteRecordId("rewrite-001");
        approvedRecord.setTenantId("tenant-a");
        approvedRecord.setReviewStatus("APPROVED");
        approvedRecord.setAutoApplyAllowed(Boolean.TRUE);
        RewriteActivationEligibilityReasonVO reason = new RewriteActivationEligibilityReasonVO();
        reason.setCode("VALIDATION_STATUS_NOT_EQUIVALENT");
        reason.setMessage("改写记录 validationStatus 必须为 EQUIVALENT。");
        reason.setBlocking(Boolean.TRUE);
        reason.setField("validationStatus");
        reason.setEvidenceRef("validation-001");
        RewriteActivationEligibilityVO eligibility = new RewriteActivationEligibilityVO();
        eligibility.setRewriteRecordId("rewrite-001");
        eligibility.setTenantId("tenant-a");
        eligibility.setPolicyId("DEFAULT_REWRITE_ACTIVATION_ELIGIBILITY");
        eligibility.setEligible(Boolean.FALSE);
        eligibility.setReviewStatus("APPROVED");
        eligibility.setValidationStatus("NOT_VALIDATED");
        eligibility.setActivationStatus("INACTIVE");
        eligibility.setAlertStatus("NONE");
        eligibility.setAutoApplyAllowed(Boolean.TRUE);
        eligibility.setLastValidationRunId("validation-001");
        eligibility.setRefusalReasons(Collections.singletonList(reason));
        eligibility.setContractStage("LONG_TERM_BASELINE");
        eligibility.setImplementationStage("ACCELERATION_REWRITE_CONTRACT_BASELINE");
        RewriteValidationRunVO run = new RewriteValidationRunVO();
        run.setValidationRunId("validation-001");
        run.setRewriteRecordId("rewrite-001");
        run.setTenantId("tenant-a");
        run.setComparisonStatus("DIVERGED");
        run.setDifferenceType("VALUE_DIFF");
        run.setAutoApplyPaused(Boolean.TRUE);
        when(sqlRewriteRecordApplicationService.createRewriteRecord(any())).thenReturn(record);
        when(sqlRewriteRecordApplicationService.getRewriteRecord("rewrite-001")).thenReturn(record);
        when(sqlRewriteRecordApplicationService.listRewriteRecords("history-001", null, null, null))
            .thenReturn(Collections.singletonList(record));
        when(sqlRewriteRecordApplicationService.getActivationEligibility("rewrite-001")).thenReturn(eligibility);
        when(sqlRewriteRecordApplicationService.reviewRewriteRecord(any(), any())).thenReturn(approvedRecord);
        when(sqlRewriteRecordApplicationService.activateRewriteRecord(any(), any())).thenReturn(activatedRecord);
        when(sqlRewriteRecordApplicationService.pauseRewriteRecord(any(), any())).thenReturn(pausedRecord);
        when(sqlRewriteRecordApplicationService.createValidationRun(any(), any())).thenReturn(run);
        when(sqlRewriteRecordApplicationService.listValidationRuns("rewrite-001"))
            .thenReturn(Collections.singletonList(run));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/rewrite-records"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"sourceType\":\"QUERY\",\"sourceKind\":\"QUERY_HISTORY\","
                    + "\"sourceId\":\"history-001\",\"evidenceLevel\":\"RUNTIME_HISTORY\","
                    + "\"originalSqlText\":\"SELECT * FROM orders\",\"recommendedSqlText\":\"SELECT id FROM orders\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceKind").value("QUERY_HISTORY"))
            .andExpect(jsonPath("$.evidenceLevel").value("RUNTIME_HISTORY"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/rewrite-records")
                .param("historyId", "history-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].validationStatus").value("NOT_VALIDATED"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/rewrite-records/rewrite-001")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.manualReviewRequired").value(true))
            .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"))
            .andExpect(jsonPath("$.activationStatus").value("INACTIVE"))
            .andExpect(jsonPath("$.runtimeBindingId").value("binding-001"))
            .andExpect(jsonPath("$.runtimeRuleVersion").value("rule-v1"));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/rewrite-records/rewrite-001/activation-eligibility")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.policyId").value("DEFAULT_REWRITE_ACTIVATION_ELIGIBILITY"))
            .andExpect(jsonPath("$.eligible").value(false))
            .andExpect(jsonPath("$.refusalReasons[0].code").value("VALIDATION_STATUS_NOT_EQUIVALENT"))
            .andExpect(jsonPath("$.refusalReasons[0].blocking").value(true));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/rewrite-records/rewrite-001/review"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"reviewStatus\":\"APPROVED\","
                    + "\"reviewNote\":\"allow runtime activation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
            .andExpect(jsonPath("$.autoApplyAllowed").value(true));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/rewrite-records/rewrite-001/activate"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"reason\":\"activate validated rewrite\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activationStatus").value("ACTIVE"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/rewrite-records/rewrite-001/pause"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":\"tenant-a\",\"reason\":\"validation divergence\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activationStatus").value("PAUSED"));

        mockMvc.perform(addProtectedHeaders(post("/api/sql-optimization/rewrite-records/rewrite-001/validation-runs"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comparisonStatus\":\"DIVERGED\",\"differenceType\":\"VALUE_DIFF\","
                    + "\"autoApplyPaused\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.autoApplyPaused").value(true));

        mockMvc.perform(addProtectedHeaders(get("/api/sql-optimization/rewrite-records/rewrite-001/validation-runs")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].differenceType").value("VALUE_DIFF"));
    }

    private MockHttpServletRequestBuilder addProtectedHeaders(MockHttpServletRequestBuilder builder) {
        long now = System.currentTimeMillis();
        return builder
            .header(RequestHeaderConstants.TENANT_ID, "tenant-a")
            .header(RequestHeaderConstants.USER_ID, "user-001")
            .header(RequestHeaderConstants.REQUEST_ID, "request-001")
            .header(RequestHeaderConstants.TRACE_ID, "trace-001")
            .header(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER)
            .header(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L))
            .header(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
