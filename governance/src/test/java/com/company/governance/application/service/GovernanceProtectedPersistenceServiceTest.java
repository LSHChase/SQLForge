package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
import com.company.sqlforge.common.security.SensitiveDataCryptoProperties;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.security.SensitiveDataProtectionService;
import java.nio.charset.StandardCharsets;
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
        configSnapshotRecord.setSnapshotPayload("{\"jdbcPassword\":\"plain-secret\",\"engine\":\"HETU\"}");
        configSnapshotRecord.setSnapshotReason("rotation token=raw-token");

        ExecutionResultRecord executionResultRecord = new ExecutionResultRecord();
        executionResultRecord.setResultSummary("{\"apiToken\":\"secret-token\",\"rows\":10}");
        executionResultRecord.setResultPayload("{\"secretKey\":\"ak-value\",\"plan\":\"P1\"}");
        executionResultRecord.setErrorMessage("secret=raw");

        QueryHistoryRecord queryHistoryRecord = new QueryHistoryRecord();
        queryHistoryRecord.setQueryContext("{\"sessionToken\":\"session-secret\"}");

        ExportRecord exportRecord = new ExportRecord();
        exportRecord.setStorageUri("https://example.test/report?token=download-secret");
        exportRecord.setExportOptions("{\"accessKey\":\"ak-001\",\"format\":\"PDF\"}");
        exportRecord.setErrorMessage("download token=oops");

        AuditLogRecord auditLogRecord = new AuditLogRecord();
        auditLogRecord.setRequestParams("{\"password\":\"p@ssw0rd\",\"sqlFingerprint\":\"abc\"}");
        auditLogRecord.setResponseSummary("permission changed with secret=grant-token");

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
}
