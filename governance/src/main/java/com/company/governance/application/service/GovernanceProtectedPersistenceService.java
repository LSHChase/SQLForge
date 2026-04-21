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
import com.company.sqlforge.common.security.SensitiveDataCryptoService;
import com.company.sqlforge.common.security.SensitiveDataProtectionService;
import java.nio.charset.StandardCharsets;
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
        protectExecutionResult(record);
        return executionResultMapper.insert(record);
    }

    public int saveQueryHistory(QueryHistoryRecord record) {
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
        protectExportRecord(record);
        return exportRecordMapper.insert(record);
    }

    public int saveAuditLog(AuditLogRecord record) {
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

    private void protectExecutionResult(ExecutionResultRecord record) {
        record.setResultSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getResultSummary()));
        record.setResultPayload(sensitiveDataProtectionService.encryptSensitiveJson(record.getResultPayload()));
        record.setErrorMessage(sensitiveDataProtectionService.maskSensitiveText(record.getErrorMessage()));
    }

    private void protectQueryHistory(QueryHistoryRecord record) {
        record.setQueryContext(sensitiveDataProtectionService.encryptSensitiveJson(record.getQueryContext()));
    }

    private void protectExportRecord(ExportRecord record) {
        record.setStorageUri(sensitiveDataProtectionService.maskSensitiveText(record.getStorageUri()));
        record.setExportOptions(sensitiveDataProtectionService.encryptSensitiveJson(record.getExportOptions()));
        record.setErrorMessage(sensitiveDataProtectionService.maskSensitiveText(record.getErrorMessage()));
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
}
