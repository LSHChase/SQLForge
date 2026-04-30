package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.system.entity.SystemConfigRecord;
import com.company.governance.domain.trace.entity.AuditLogRecord;
import com.company.governance.domain.trace.entity.ConfigSnapshotRecord;
import com.company.governance.domain.trace.entity.ExecutionResultRecord;
import com.company.governance.domain.trace.entity.ExportRecord;
import com.company.governance.domain.trace.entity.QueryHistoryRecord;
import com.company.governance.infrastructure.persistence.mapper.AuditLogMapper;
import com.company.governance.infrastructure.persistence.mapper.ConfigSnapshotMapper;
import com.company.governance.infrastructure.persistence.mapper.ExecutionResultMapper;
import com.company.governance.infrastructure.persistence.mapper.ExportRecordMapper;
import com.company.governance.infrastructure.persistence.mapper.QueryHistoryMapper;
import com.company.governance.infrastructure.persistence.mapper.SystemConfigMapper;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.security.SensitiveDataCryptoProperties;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.security.SensitiveDataProtectionService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GovernanceProtectedPersistenceServiceTest {

    private static final String TEST_BASE64_KEY = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    @Test
    void shouldProtectTraceabilityRecordsBeforePersistence() {
        ConfigSnapshotMapper configSnapshotMapper = mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        SystemConfigMapper systemConfigMapper = mock(SystemConfigMapper.class);
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceProtectedPersistenceService service = new GovernanceProtectedPersistenceService(
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            exportRecordMapper,
            auditLogMapper,
            systemConfigMapper,
            new SensitiveDataProtectionService(cryptoService),
            cryptoService
        );

        ConfigSnapshotRecord configSnapshotRecord = new ConfigSnapshotRecord();
        configSnapshotRecord.setConfigSnapshotId("cfg-001");
        configSnapshotRecord.setTenantId("tenant-a");
        configSnapshotRecord.setSnapshotPayload("{\"jdbcPassword\":\"plain-secret\",\"engine\":\"HETU\"}");
        configSnapshotRecord.setSnapshotReason("rotation token=raw-token");

        ExecutionResultRecord executionResultRecord = new ExecutionResultRecord();
        executionResultRecord.setConfigSnapshotId("cfg-001");
        executionResultRecord.setTenantId("tenant-a");
        executionResultRecord.setResultId("res-001");
        executionResultRecord.setResultSummary(
            "{\"apiToken\":\"secret-token\",\"rows\":10,\"accessChannel\":\"PAGE\",\"targetEngine\":\"HETU\","
                + "\"cacheHit\":true,\"rewriteApplied\":true,\"accelerationApplied\":false,"
                + "\"cacheGovernanceStatus\":\"HIT\",\"cacheGovernanceEvidence\":{\"policyId\":\"cache-1\"}}"
        );
        executionResultRecord.setResultPayload(
            "{\"secretKey\":\"ak-value\",\"plan\":\"P1\",\"hitTables\":[\"sales.orders\"],"
                + "\"routeSummary\":{\"selectedEngine\":\"HETU\",\"ruleId\":\"route-001\"}}"
        );
        executionResultRecord.setErrorMessage("secret=raw");

        QueryHistoryRecord queryHistoryRecord = new QueryHistoryRecord();
        queryHistoryRecord.setHistoryId("hist-001");
        queryHistoryRecord.setResultId("res-001");
        queryHistoryRecord.setTenantId("tenant-a");
        queryHistoryRecord.setQueryContext(
            "{\"sessionToken\":\"session-secret\",\"accessChannel\":\"PAGE\",\"queryDateStart\":\"2026-04-24\","
                + "\"queryDateEnd\":\"2026-04-25\",\"queryDateStatus\":\"RESOLVED\","
                + "\"commentContext\":{\"report_code\":\"RPT_SALES_DAILY\",\"stage\":\"PROD\",\"biz_date\":\"2026-04-25\","
                + "\"datasource\":\"hetu_main\"},\"bindingSummary\":{\"parameterizedSqlFlag\":true,"
                + "\"bindingMode\":\"POSITIONAL\",\"bindingRenderStatus\":\"SUCCESS\","
                + "\"sqlTemplateFingerprint\":\"tmpl-fp\",\"boundSqlFingerprint\":\"bound-fp\"},"
                + "\"logicalObjectHits\":[\"vw_sales_daily\",\"sales.orders\"],"
                + "\"routeSummary\":{\"selectedEngine\":\"HETU\"},"
                + "\"cacheSummary\":{\"cacheHit\":true}}"
        );

        ExportRecord exportRecord = new ExportRecord();
        exportRecord.setExportId("exp-001");
        exportRecord.setHistoryId("hist-001");
        exportRecord.setResultId("res-001");
        exportRecord.setTenantId("tenant-a");
        exportRecord.setStorageUri("https://example.test/report?token=download-secret");
        exportRecord.setExportOptions("{\"accessKey\":\"ak-001\",\"format\":\"PDF\"}");
        exportRecord.setErrorMessage("download token=oops");

        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setTenantId("tenant-a");
        auditLogRecord.setConfigSnapshotId("cfg-001");
        auditLogRecord.setResultId("res-001");
        auditLogRecord.setHistoryId("hist-001");
        auditLogRecord.setExportId("exp-001");
        auditLogRecord.setRequestParams("{\"password\":\"p@ssw0rd\",\"sqlFingerprint\":\"abc\"}");
        auditLogRecord.setResponseSummary("permission changed with secret=grant-token");

        when(configSnapshotMapper.selectById("cfg-001")).thenReturn(configSnapshotRecord);
        when(executionResultMapper.selectById("res-001")).thenReturn(executionResultRecord);
        when(queryHistoryMapper.selectById("hist-001")).thenReturn(queryHistoryRecord);
        when(exportRecordMapper.selectById("exp-001")).thenReturn(exportRecord);

        service.saveConfigSnapshot(configSnapshotRecord);
        service.saveExecutionResult(executionResultRecord);
        service.saveQueryHistoryWithSqlText(queryHistoryRecord, "select * from secret_table");
        service.saveExportRecord(exportRecord);
        service.saveAuditLog(auditLogRecord);

        ArgumentCaptor<ConfigSnapshotRecord> configCaptor = ArgumentCaptor.forClass(ConfigSnapshotRecord.class);
        verify(configSnapshotMapper).insert(configCaptor.capture());
        assertTrue(configCaptor.getValue().getSnapshotPayload().contains("\"ciphertext\":\"ENC::AES256_GCM::"));
        assertFalse(configCaptor.getValue().getSnapshotPayload().contains("plain-secret"));
        assertFalse(configCaptor.getValue().getSnapshotReason().contains("raw-token"));

        ArgumentCaptor<ExecutionResultRecord> executionCaptor = ArgumentCaptor.forClass(ExecutionResultRecord.class);
        verify(executionResultMapper).insert(executionCaptor.capture());
        assertEquals("PAGE", executionCaptor.getValue().getAccessChannel());
        assertEquals("HETU", executionCaptor.getValue().getTargetEngine());
        assertEquals(Long.valueOf(10L), executionCaptor.getValue().getReturnedRowCount());
        assertEquals(Boolean.TRUE, executionCaptor.getValue().getCacheHit());
        assertEquals(Boolean.TRUE, executionCaptor.getValue().getRewriteApplied());
        assertEquals(Boolean.FALSE, executionCaptor.getValue().getAccelerationApplied());
        assertTrue(executionCaptor.getValue().getHitTableSummary().contains("sales.orders"));
        assertTrue(executionCaptor.getValue().getRouteSummary().contains("selectedEngine"));
        assertTrue(executionCaptor.getValue().getCacheSummary().contains("cacheGovernanceStatus"));
        assertTrue(executionCaptor.getValue().getResultSummary().contains("***"));
        assertFalse(executionCaptor.getValue().getResultSummary().contains("secret-token"));
        assertTrue(executionCaptor.getValue().getResultPayload().contains("\"ciphertext\":\"ENC::AES256_GCM::"));
        assertFalse(executionCaptor.getValue().getResultPayload().contains("ak-value"));
        assertFalse(executionCaptor.getValue().getErrorMessage().contains("raw"));

        ArgumentCaptor<QueryHistoryRecord> historyCaptor = ArgumentCaptor.forClass(QueryHistoryRecord.class);
        verify(queryHistoryMapper).insert(historyCaptor.capture());
        assertNotNull(historyCaptor.getValue().getSqlTextCipher());
        assertEquals(
            "select * from secret_table",
            new String(cryptoService.decryptBytes(historyCaptor.getValue().getSqlTextCipher()), StandardCharsets.UTF_8)
        );
        assertEquals("hetu_main", historyCaptor.getValue().getDatasourceCode());
        assertEquals("RPT_SALES_DAILY", historyCaptor.getValue().getReportCode());
        assertEquals("PROD", historyCaptor.getValue().getStageCode());
        assertEquals("RESOLVED", historyCaptor.getValue().getQueryDateStatus());
        assertEquals("PAGE", historyCaptor.getValue().getAccessChannel());
        assertEquals(Boolean.TRUE, historyCaptor.getValue().getParameterizedSqlFlag());
        assertEquals("POSITIONAL", historyCaptor.getValue().getBindingMode());
        assertEquals("SUCCESS", historyCaptor.getValue().getBindingRenderStatus());
        assertEquals("tmpl-fp", historyCaptor.getValue().getSqlTemplateFingerprint());
        assertEquals("bound-fp", historyCaptor.getValue().getBoundSqlFingerprint());
        assertTrue(historyCaptor.getValue().getCommentContext().contains("RPT_SALES_DAILY"));
        assertTrue(historyCaptor.getValue().getBindingSummary().contains("POSITIONAL"));
        assertTrue(historyCaptor.getValue().getLogicalObjectHits().contains("vw_sales_daily"));
        assertTrue(historyCaptor.getValue().getQueryContext().contains("\"ciphertext\":\"ENC::AES256_GCM::"));

        ArgumentCaptor<ExportRecord> exportCaptor = ArgumentCaptor.forClass(ExportRecord.class);
        verify(exportRecordMapper).insert(exportCaptor.capture());
        assertFalse(exportCaptor.getValue().getStorageUri().contains("download-secret"));
        assertTrue(exportCaptor.getValue().getStorageUri().contains("***"));
        assertTrue(exportCaptor.getValue().getExportOptions().contains("\"ciphertext\":\"ENC::AES256_GCM::"));
        assertFalse(exportCaptor.getValue().getExportOptions().contains("ak-001"));
        assertFalse(exportCaptor.getValue().getErrorMessage().contains("oops"));

        ArgumentCaptor<AuditLogRecord> auditCaptor = ArgumentCaptor.forClass(AuditLogRecord.class);
        verify(auditLogMapper).insert(auditCaptor.capture());
        assertFalse(auditCaptor.getValue().getRequestParams().contains("p@ssw0rd"));
        assertTrue(auditCaptor.getValue().getRequestParams().contains("***"));
        assertFalse(auditCaptor.getValue().getResponseSummary().contains("grant-token"));
    }

    @Test
    void shouldProjectLegacyCommentContextWhenQueryContextHasNoNestedCommentContext() {
        ConfigSnapshotMapper configSnapshotMapper = mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceProtectedPersistenceService service = new GovernanceProtectedPersistenceService(
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            mock(ExportRecordMapper.class),
            mock(AuditLogMapper.class),
            mock(SystemConfigMapper.class),
            new SensitiveDataProtectionService(cryptoService),
            cryptoService
        );
        ExecutionResultRecord executionResultRecord = new ExecutionResultRecord();
        executionResultRecord.setResultId("res-legacy");
        executionResultRecord.setTenantId("tenant-a");
        when(executionResultMapper.selectById("res-legacy")).thenReturn(executionResultRecord);

        QueryHistoryRecord record = new QueryHistoryRecord();
        record.setHistoryId("hist-legacy");
        record.setResultId("res-legacy");
        record.setTenantId("tenant-a");
        record.setHistoryType("SQL_PARSE");
        record.setCommentContext(
            "{\"report_code\":\"RPT_LEGACY\",\"stage\":\"PROD\",\"biz_date\":\"2026-04-29\",\"datasource\":\"hetu_main\"}"
        );
        record.setQueryContext("{\"structureParseSummary\":{\"syntaxStatus\":\"VALID\"}}");

        service.saveQueryHistoryWithSqlText(record, "SELECT 1");

        ArgumentCaptor<QueryHistoryRecord> historyCaptor = ArgumentCaptor.forClass(QueryHistoryRecord.class);
        verify(queryHistoryMapper).insert(historyCaptor.capture());
        assertEquals("RPT_LEGACY", historyCaptor.getValue().getReportCode());
        assertEquals("PROD", historyCaptor.getValue().getStageCode());
        assertEquals(LocalDate.parse("2026-04-29"), historyCaptor.getValue().getBizDate());
        assertEquals("hetu_main", historyCaptor.getValue().getDatasourceCode());
    }

    @Test
    void shouldEncryptSensitiveSystemConfigValuesWhileKeepingPlainConfigReadable() {
        SystemConfigMapper systemConfigMapper = mock(SystemConfigMapper.class);
        doAnswer(invocation -> 1).when(systemConfigMapper).insertOrUpdate(org.mockito.ArgumentMatchers.any(SystemConfigRecord.class));
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceProtectedPersistenceService service = new GovernanceProtectedPersistenceService(
            mock(ConfigSnapshotMapper.class),
            mock(ExecutionResultMapper.class),
            mock(QueryHistoryMapper.class),
            mock(ExportRecordMapper.class),
            mock(AuditLogMapper.class),
            systemConfigMapper,
            new SensitiveDataProtectionService(cryptoService),
            cryptoService
        );

        SystemConfigRecord sensitive = new SystemConfigRecord();
        sensitive.setConfigKey("system.obs.secret-key");
        sensitive.setConfigValue("minioadmin");
        sensitive.setConfigType("STRING");

        SystemConfigRecord plain = new SystemConfigRecord();
        plain.setConfigKey("system.defaultEngine");
        plain.setConfigValue("HETU");
        plain.setConfigType("STRING");

        service.saveSystemConfig(sensitive);
        service.saveSystemConfig(plain);

        ArgumentCaptor<SystemConfigRecord> captor = ArgumentCaptor.forClass(SystemConfigRecord.class);
        verify(systemConfigMapper, org.mockito.Mockito.times(2)).insertOrUpdate(captor.capture());
        SystemConfigRecord sensitiveStored = captor.getAllValues().get(0);
        SystemConfigRecord plainStored = captor.getAllValues().get(1);

        assertTrue(Boolean.TRUE.equals(sensitiveStored.getSensitiveFlag()));
        assertNull(sensitiveStored.getConfigValue());
        assertTrue(sensitiveStored.getValueCiphertext().startsWith("ENC::AES256_GCM::"));
        assertNotNull(sensitiveStored.getValueMask());
        assertEquals("AES256_GCM", sensitiveStored.getEncryptionAlgorithm());
        assertEquals("minioadmin", cryptoService.decrypt(sensitiveStored.getValueCiphertext()));

        assertTrue(Boolean.FALSE.equals(plainStored.getSensitiveFlag()));
        assertEquals("HETU", plainStored.getConfigValue());
        assertNull(plainStored.getValueCiphertext());
    }

    @Test
    void shouldRejectMissingOrInconsistentTraceabilityReferencesWithoutForeignKeys() {
        ConfigSnapshotMapper configSnapshotMapper = mock(ConfigSnapshotMapper.class);
        ExecutionResultMapper executionResultMapper = mock(ExecutionResultMapper.class);
        QueryHistoryMapper queryHistoryMapper = mock(QueryHistoryMapper.class);
        ExportRecordMapper exportRecordMapper = mock(ExportRecordMapper.class);
        AuditLogMapper auditLogMapper = mock(AuditLogMapper.class);
        SystemConfigMapper systemConfigMapper = mock(SystemConfigMapper.class);
        SensitiveDataCryptoProperties cryptoProperties = new SensitiveDataCryptoProperties();
        cryptoProperties.setBase64Key(TEST_BASE64_KEY);
        SensitiveDataCryptoService cryptoService = new SensitiveDataCryptoService(cryptoProperties);
        GovernanceProtectedPersistenceService service = new GovernanceProtectedPersistenceService(
            configSnapshotMapper,
            executionResultMapper,
            queryHistoryMapper,
            exportRecordMapper,
            auditLogMapper,
            systemConfigMapper,
            new SensitiveDataProtectionService(cryptoService),
            cryptoService
        );

        ExecutionResultRecord executionResultRecord = new ExecutionResultRecord();
        executionResultRecord.setConfigSnapshotId("cfg-missing");
        executionResultRecord.setTenantId("tenant-a");

        BizException missingSnapshot = assertThrows(BizException.class, () -> service.saveExecutionResult(executionResultRecord));
        assertTrue(missingSnapshot.getMessage().contains("missing config snapshot"));

        ConfigSnapshotRecord snapshotRecord = new ConfigSnapshotRecord();
        snapshotRecord.setConfigSnapshotId("cfg-001");
        snapshotRecord.setTenantId("tenant-a");
        when(configSnapshotMapper.selectById("cfg-001")).thenReturn(snapshotRecord);

        ExecutionResultRecord resultRecord = new ExecutionResultRecord();
        resultRecord.setResultId("res-001");
        resultRecord.setConfigSnapshotId("cfg-001");
        resultRecord.setTenantId("tenant-a");
        when(executionResultMapper.selectById("res-001")).thenReturn(resultRecord);

        QueryHistoryRecord historyRecord = new QueryHistoryRecord();
        historyRecord.setHistoryId("hist-001");
        historyRecord.setResultId("res-001");
        historyRecord.setTenantId("tenant-b");
        when(queryHistoryMapper.selectById("hist-001")).thenReturn(historyRecord);

        ExportRecord exportRecord = new ExportRecord();
        exportRecord.setExportId("exp-001");
        exportRecord.setHistoryId("hist-001");
        exportRecord.setResultId("res-001");
        exportRecord.setTenantId("tenant-a");
        when(exportRecordMapper.selectById("exp-001")).thenReturn(exportRecord);

        QueryHistoryRecord queryHistoryRecord = new QueryHistoryRecord();
        queryHistoryRecord.setResultId("res-001");
        queryHistoryRecord.setTenantId("tenant-c");
        BizException tenantMismatch = assertThrows(BizException.class, () -> service.saveQueryHistory(queryHistoryRecord));
        assertTrue(tenantMismatch.getMessage().contains("tenantId does not match"));

        ExportRecord inconsistentExport = new ExportRecord();
        inconsistentExport.setHistoryId("hist-001");
        inconsistentExport.setResultId("res-001");
        inconsistentExport.setTenantId("tenant-a");
        BizException inconsistentHistoryTenant = assertThrows(BizException.class, () -> service.saveExportRecord(inconsistentExport));
        assertTrue(inconsistentHistoryTenant.getMessage().contains("tenantId does not match"));

        historyRecord.setTenantId("tenant-a");
        historyRecord.setResultId("res-002");
        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setTenantId("tenant-a");
        auditLogRecord.setHistoryId("hist-001");
        auditLogRecord.setResultId("res-001");
        auditLogRecord.setExportId("exp-001");
        BizException inconsistentAudit = assertThrows(BizException.class, () -> service.saveAuditLog(auditLogRecord));
        assertTrue(inconsistentAudit.getMessage().contains("history/result references are inconsistent"));

        verify(executionResultMapper, never()).insert(any(ExecutionResultRecord.class));
        verify(queryHistoryMapper, never()).insert(any(QueryHistoryRecord.class));
        verify(exportRecordMapper, never()).insert(any(ExportRecord.class));
        verify(auditLogMapper, never()).insert(any(AuditLogRecord.class));
    }
}
