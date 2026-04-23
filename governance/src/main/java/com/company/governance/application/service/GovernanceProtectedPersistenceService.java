package com.company.governance.application.service;

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
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.security.SensitiveDataProtectionService;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceProtectedPersistenceService {

    private final ConfigSnapshotMapper configSnapshotMapper;
    private final ExecutionResultMapper executionResultMapper;
    private final QueryHistoryMapper queryHistoryMapper;
    private final ExportRecordMapper exportRecordMapper;
    private final AuditLogMapper auditLogMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final SensitiveDataProtectionService sensitiveDataProtectionService;
    private final SensitiveDataCryptoService sensitiveDataCryptoService;

    public GovernanceProtectedPersistenceService(ConfigSnapshotMapper configSnapshotMapper,
                                                 ExecutionResultMapper executionResultMapper,
                                                 QueryHistoryMapper queryHistoryMapper,
                                                 ExportRecordMapper exportRecordMapper,
                                                 AuditLogMapper auditLogMapper,
                                                 SystemConfigMapper systemConfigMapper,
                                                 SensitiveDataProtectionService sensitiveDataProtectionService,
                                                 SensitiveDataCryptoService sensitiveDataCryptoService) {
        this.configSnapshotMapper = configSnapshotMapper;
        this.executionResultMapper = executionResultMapper;
        this.queryHistoryMapper = queryHistoryMapper;
        this.exportRecordMapper = exportRecordMapper;
        this.auditLogMapper = auditLogMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.sensitiveDataProtectionService = sensitiveDataProtectionService;
        this.sensitiveDataCryptoService = sensitiveDataCryptoService;
    }

    public int saveConfigSnapshot(ConfigSnapshotRecord record) {
        protectConfigSnapshot(record);
        return configSnapshotMapper.insert(record);
    }

    public int saveExecutionResult(ExecutionResultRecord record) {
        validateExecutionResultReference(record);
        protectExecutionResult(record);
        return executionResultMapper.insert(record);
    }

    public int saveQueryHistory(QueryHistoryRecord record) {
        validateQueryHistoryReference(record);
        protectQueryHistory(record);
        return queryHistoryMapper.insert(record);
    }

    public int saveQueryHistoryWithSqlText(QueryHistoryRecord record, String sqlText) {
        if (StringUtils.hasText(sqlText)) {
            record.setSqlTextCipher(sensitiveDataCryptoService.encryptBytes(sqlText.getBytes(StandardCharsets.UTF_8)));
        }
        return saveQueryHistory(record);
    }

    public int saveExportRecord(ExportRecord record) {
        validateExportRecordReferences(record);
        protectExportRecord(record);
        return exportRecordMapper.insert(record);
    }

    public int saveAuditLog(AuditLogRecord record) {
        validateAuditLogReferences(record);
        protectAuditLog(record);
        return auditLogMapper.insert(record);
    }

    public int saveSystemConfig(SystemConfigRecord record) {
        protectSystemConfig(record);
        return systemConfigMapper.insertOrUpdate(record);
    }

    private void protectConfigSnapshot(ConfigSnapshotRecord record) {
        record.setSnapshotPayload(sensitiveDataProtectionService.encryptSensitiveJson(record.getSnapshotPayload()));
        record.setSnapshotReason(sensitiveDataProtectionService.maskSensitiveText(record.getSnapshotReason()));
    }

    private void validateExecutionResultReference(ExecutionResultRecord record) {
        requireRecord(record, "execution_result");
        ConfigSnapshotRecord snapshotRecord = requireConfigSnapshot(
            record.getConfigSnapshotId(),
            "execution_result.config_snapshot_id"
        );
        assertSameTenant("execution_result", record.getTenantId(), "config_snapshot", snapshotRecord.getTenantId());
    }

    private void protectExecutionResult(ExecutionResultRecord record) {
        record.setResultSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getResultSummary()));
        record.setResultPayload(sensitiveDataProtectionService.encryptSensitiveJson(record.getResultPayload()));
        record.setErrorMessage(sensitiveDataProtectionService.maskSensitiveText(record.getErrorMessage()));
    }

    private void validateQueryHistoryReference(QueryHistoryRecord record) {
        requireRecord(record, "query_history");
        ExecutionResultRecord resultRecord = requireExecutionResult(record.getResultId(), "query_history.result_id");
        assertSameTenant("query_history", record.getTenantId(), "execution_result", resultRecord.getTenantId());
    }

    private void protectQueryHistory(QueryHistoryRecord record) {
        record.setQueryContext(sensitiveDataProtectionService.encryptSensitiveJson(record.getQueryContext()));
    }

    private void validateExportRecordReferences(ExportRecord record) {
        requireRecord(record, "export_record");
        QueryHistoryRecord historyRecord = requireQueryHistory(record.getHistoryId(), "export_record.history_id");
        ExecutionResultRecord resultRecord = requireExecutionResult(record.getResultId(), "export_record.result_id");
        assertSameTenant("export_record", record.getTenantId(), "query_history", historyRecord.getTenantId());
        assertSameTenant("export_record", record.getTenantId(), "execution_result", resultRecord.getTenantId());
        if (!record.getResultId().equals(historyRecord.getResultId())) {
            throw invalidTraceabilityReference(
                "export_record.result_id",
                "export_record history/result references are inconsistent"
            );
        }
    }

    private void protectExportRecord(ExportRecord record) {
        record.setStorageUri(sensitiveDataProtectionService.maskSensitiveText(record.getStorageUri()));
        record.setExportOptions(sensitiveDataProtectionService.encryptSensitiveJson(record.getExportOptions()));
        record.setErrorMessage(sensitiveDataProtectionService.maskSensitiveText(record.getErrorMessage()));
    }

    private void validateAuditLogReferences(AuditLogRecord record) {
        requireRecord(record, "audit_log");
        ConfigSnapshotRecord snapshotRecord = findConfigSnapshot(record.getConfigSnapshotId());
        ExecutionResultRecord resultRecord = findExecutionResult(record.getResultId());
        QueryHistoryRecord historyRecord = findQueryHistory(record.getHistoryId());
        ExportRecord exportRecord = findExportRecord(record.getExportId());

        assertSameTenantIfPresent("audit_log", record.getTenantId(), "config_snapshot", snapshotRecord == null ? null : snapshotRecord.getTenantId());
        assertSameTenantIfPresent("audit_log", record.getTenantId(), "execution_result", resultRecord == null ? null : resultRecord.getTenantId());
        assertSameTenantIfPresent("audit_log", record.getTenantId(), "query_history", historyRecord == null ? null : historyRecord.getTenantId());
        assertSameTenantIfPresent("audit_log", record.getTenantId(), "export_record", exportRecord == null ? null : exportRecord.getTenantId());

        if (historyRecord != null && resultRecord != null && !historyRecord.getResultId().equals(resultRecord.getResultId())) {
            throw invalidTraceabilityReference(
                "audit_log.history_id",
                "audit_log history/result references are inconsistent"
            );
        }
        if (exportRecord != null && historyRecord != null && !exportRecord.getHistoryId().equals(historyRecord.getHistoryId())) {
            throw invalidTraceabilityReference(
                "audit_log.export_id",
                "audit_log export/history references are inconsistent"
            );
        }
        if (exportRecord != null && resultRecord != null && !exportRecord.getResultId().equals(resultRecord.getResultId())) {
            throw invalidTraceabilityReference(
                "audit_log.export_id",
                "audit_log export/result references are inconsistent"
            );
        }
    }

    private void protectAuditLog(AuditLogRecord record) {
        record.setRequestParams(sensitiveDataProtectionService.maskSensitiveJson(record.getRequestParams()));
        record.setResponseSummary(sensitiveDataProtectionService.maskSensitiveText(record.getResponseSummary()));
    }

    private void protectSystemConfig(SystemConfigRecord record) {
        if (record == null) {
            return;
        }
        if (sensitiveDataProtectionService.isSensitiveKey(record.getConfigKey())) {
            String rawValue = record.getConfigValue();
            record.setSensitiveFlag(Boolean.TRUE);
            record.setConfigValue(null);
            record.setValueCiphertext(sensitiveDataCryptoService.encrypt(rawValue == null ? "" : rawValue));
            record.setValueMask(sensitiveDataProtectionService.maskDisplayValue(rawValue));
            record.setEncryptionAlgorithm(sensitiveDataCryptoService.getAlgorithm());
            record.setEncryptionKeyId(sensitiveDataCryptoService.getKeyId());
            return;
        }
        record.setSensitiveFlag(Boolean.FALSE);
        record.setValueCiphertext(null);
        record.setValueMask(null);
        record.setEncryptionAlgorithm(null);
        record.setEncryptionKeyId(null);
    }

    private void requireRecord(Object record, String entityName) {
        if (record == null) {
            throw invalidTraceabilityReference(entityName, entityName + " record must not be null");
        }
    }

    private ConfigSnapshotRecord requireConfigSnapshot(String configSnapshotId, String fieldName) {
        ConfigSnapshotRecord record = findConfigSnapshot(configSnapshotId);
        if (record == null) {
            throw invalidTraceabilityReference(fieldName, fieldName + " references a missing config snapshot");
        }
        return record;
    }

    private ExecutionResultRecord requireExecutionResult(String resultId, String fieldName) {
        ExecutionResultRecord record = findExecutionResult(resultId);
        if (record == null) {
            throw invalidTraceabilityReference(fieldName, fieldName + " references a missing execution result");
        }
        return record;
    }

    private QueryHistoryRecord requireQueryHistory(String historyId, String fieldName) {
        QueryHistoryRecord record = findQueryHistory(historyId);
        if (record == null) {
            throw invalidTraceabilityReference(fieldName, fieldName + " references a missing query history");
        }
        return record;
    }

    private ConfigSnapshotRecord findConfigSnapshot(String configSnapshotId) {
        if (!StringUtils.hasText(configSnapshotId)) {
            return null;
        }
        return configSnapshotMapper.selectById(configSnapshotId.trim());
    }

    private ExecutionResultRecord findExecutionResult(String resultId) {
        if (!StringUtils.hasText(resultId)) {
            return null;
        }
        return executionResultMapper.selectById(resultId.trim());
    }

    private QueryHistoryRecord findQueryHistory(String historyId) {
        if (!StringUtils.hasText(historyId)) {
            return null;
        }
        return queryHistoryMapper.selectById(historyId.trim());
    }

    private ExportRecord findExportRecord(String exportId) {
        if (!StringUtils.hasText(exportId)) {
            return null;
        }
        return exportRecordMapper.selectById(exportId.trim());
    }

    private void assertSameTenant(String sourceEntity, String sourceTenantId, String referenceEntity, String referenceTenantId) {
        assertSameTenantIfPresent(sourceEntity, sourceTenantId, referenceEntity, referenceTenantId);
        if (!StringUtils.hasText(referenceTenantId)) {
            throw invalidTraceabilityReference(
                sourceEntity + ".tenant_id",
                referenceEntity + " tenantId must not be empty"
            );
        }
    }

    private void assertSameTenantIfPresent(String sourceEntity, String sourceTenantId, String referenceEntity, String referenceTenantId) {
        if (!StringUtils.hasText(referenceTenantId)) {
            return;
        }
        if (!StringUtils.hasText(sourceTenantId) || !sourceTenantId.trim().equals(referenceTenantId.trim())) {
            throw invalidTraceabilityReference(
                sourceEntity + ".tenant_id",
                sourceEntity + " tenantId does not match " + referenceEntity + " tenantId"
            );
        }
    }

    private BizException invalidTraceabilityReference(String fieldName, String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            message + " (" + fieldName + ")"
        );
    }
}
