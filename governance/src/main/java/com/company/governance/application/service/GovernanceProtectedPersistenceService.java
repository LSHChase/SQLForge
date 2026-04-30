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
import com.company.sqlforge.common.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
        projectExecutionResultSurface(record);
        protectExecutionResult(record);
        return executionResultMapper.insert(record);
    }

    public int updateExecutionResult(ExecutionResultRecord record) {
        validateExecutionResultReference(record);
        projectExecutionResultSurface(record);
        protectExecutionResult(record);
        return executionResultMapper.updateById(record);
    }

    public int saveQueryHistory(QueryHistoryRecord record) {
        validateQueryHistoryReference(record);
        projectQueryHistorySurface(record);
        protectQueryHistory(record);
        return queryHistoryMapper.insert(record);
    }

    public int updateQueryHistory(QueryHistoryRecord record) {
        validateQueryHistoryReference(record);
        projectQueryHistorySurface(record);
        protectQueryHistory(record);
        return queryHistoryMapper.updateById(record);
    }

    public int saveQueryHistoryWithSqlText(QueryHistoryRecord record, String sqlText) {
        return saveQueryHistoryWithSqlSurfaces(record, sqlText, null, null);
    }

    public int saveQueryHistoryWithSqlSurfaces(QueryHistoryRecord record,
                                               String sqlText,
                                               String sqlTemplateText,
                                               String boundSqlText) {
        if (StringUtils.hasText(sqlText)) {
            record.setSqlTextCipher(sensitiveDataCryptoService.encryptBytes(sqlText.getBytes(StandardCharsets.UTF_8)));
        }
        if (StringUtils.hasText(sqlTemplateText)) {
            record.setSqlTemplateCipher(sensitiveDataCryptoService.encryptBytes(sqlTemplateText.getBytes(StandardCharsets.UTF_8)));
        }
        if (StringUtils.hasText(boundSqlText)) {
            record.setBoundSqlTextCipher(sensitiveDataCryptoService.encryptBytes(boundSqlText.getBytes(StandardCharsets.UTF_8)));
        }
        return saveQueryHistory(record);
    }

    public int updateQueryHistoryWithSqlSurfaces(QueryHistoryRecord record,
                                                 String sqlText,
                                                 String sqlTemplateText,
                                                 String boundSqlText) {
        if (StringUtils.hasText(sqlText)) {
            record.setSqlTextCipher(sensitiveDataCryptoService.encryptBytes(sqlText.getBytes(StandardCharsets.UTF_8)));
        }
        if (StringUtils.hasText(sqlTemplateText)) {
            record.setSqlTemplateCipher(sensitiveDataCryptoService.encryptBytes(sqlTemplateText.getBytes(StandardCharsets.UTF_8)));
        }
        if (StringUtils.hasText(boundSqlText)) {
            record.setBoundSqlTextCipher(sensitiveDataCryptoService.encryptBytes(boundSqlText.getBytes(StandardCharsets.UTF_8)));
        }
        return updateQueryHistory(record);
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
        record.setHitTableSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getHitTableSummary()));
        record.setRouteSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getRouteSummary()));
        record.setCacheSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getCacheSummary()));
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
        record.setCommentContext(sensitiveDataProtectionService.maskSensitiveJson(record.getCommentContext()));
        record.setBindingSummary(sensitiveDataProtectionService.encryptSensitiveJson(record.getBindingSummary()));
        record.setLogicalObjectHits(sensitiveDataProtectionService.maskSensitiveJson(record.getLogicalObjectHits()));
        record.setRouteSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getRouteSummary()));
        record.setCacheSummary(sensitiveDataProtectionService.maskSensitiveJson(record.getCacheSummary()));
        record.setQueryContext(sensitiveDataProtectionService.encryptSensitiveJson(record.getQueryContext()));
    }

    private void projectExecutionResultSurface(ExecutionResultRecord record) {
        if (record == null) {
            return;
        }
        Map<String, Object> summary = parseJsonMap(record.getResultSummary());
        Map<String, Object> payload = parseJsonMap(record.getResultPayload());
        Map<String, Object> routeSummary = firstMap(readMap(summary, "routeSummary"), readMap(payload, "routeSummary"));
        Map<String, Object> cacheSummary = firstMap(readMap(summary, "cacheSummary"), readMap(payload, "cacheSummary"));

        if (!StringUtils.hasText(record.getAccessChannel())) {
            record.setAccessChannel(firstText(readText(summary, "accessChannel"), readText(payload, "accessChannel")));
        }
        if (!StringUtils.hasText(record.getTargetEngine())) {
            record.setTargetEngine(firstText(
                readText(summary, "targetEngine"),
                readText(routeSummary, "selectedEngine"),
                readText(routeSummary, "targetEngine"),
                readText(payload, "targetEngine")
            ));
        }
        if (record.getReturnedRowCount() == null) {
            record.setReturnedRowCount(firstLong(
                readLong(summary, "returnedRowCount"),
                readLong(summary, "rowCount"),
                readLong(summary, "rows"),
                readLong(payload, "returnedRowCount"),
                readLong(payload, "rowCount"),
                readLong(payload, "rows")
            ));
        }
        if (record.getCacheHit() == null) {
            record.setCacheHit(firstBoolean(readBoolean(summary, "cacheHit"), readBoolean(cacheSummary, "cacheHit")));
        }
        if (record.getRewriteApplied() == null) {
            record.setRewriteApplied(firstBoolean(
                readBoolean(summary, "rewriteApplied"),
                readBoolean(payload, "rewriteApplied")
            ));
        }
        if (record.getAccelerationApplied() == null) {
            record.setAccelerationApplied(firstBoolean(
                readBoolean(summary, "accelerationApplied"),
                readBoolean(payload, "accelerationApplied")
            ));
        }
        if (!StringUtils.hasText(record.getHitTableSummary())) {
            record.setHitTableSummary(toJsonIfPresent(firstNonNull(payload.get("hitTables"), summary.get("hitTables"))));
        }
        if (!StringUtils.hasText(record.getRouteSummary()) && !routeSummary.isEmpty()) {
            record.setRouteSummary(JsonUtils.toJson(routeSummary));
        }
        if (!StringUtils.hasText(record.getCacheSummary())) {
            Map<String, Object> normalizedCacheSummary = normalizeCacheSummary(summary, payload, cacheSummary);
            if (!normalizedCacheSummary.isEmpty()) {
                record.setCacheSummary(JsonUtils.toJson(normalizedCacheSummary));
            }
        }
    }

    private void projectQueryHistorySurface(QueryHistoryRecord record) {
        if (record == null) {
            return;
        }
        Map<String, Object> queryContext = parseJsonMap(record.getQueryContext());
        Map<String, Object> explicitCommentContext = readMap(queryContext, "commentContext");
        Map<String, Object> storedCommentContext = parseJsonMap(record.getCommentContext());
        Map<String, Object> commentContext = firstMap(explicitCommentContext, storedCommentContext);
        Map<String, Object> bindingSummary = readMap(queryContext, "bindingSummary");
        Object logicalObjectHits = firstNonNull(queryContext.get("logicalObjectHits"), queryContext.get("logicalObjects"));
        Map<String, Object> routeSummary = readMap(queryContext, "routeSummary");
        Map<String, Object> cacheSummary = normalizeCacheSummary(
            queryContext,
            Collections.<String, Object>emptyMap(),
            readMap(queryContext, "cacheSummary")
        );

        if (!StringUtils.hasText(record.getDatasourceCode())) {
            record.setDatasourceCode(firstText(
                readText(commentContext, "datasource"),
                readText(queryContext, "datasourceCode"),
                readText(queryContext, "datasource")
            ));
        }
        if (!StringUtils.hasText(record.getReportCode())) {
            record.setReportCode(firstText(readText(commentContext, "report_code"), readText(queryContext, "reportCode")));
        }
        if (!StringUtils.hasText(record.getStageCode())) {
            record.setStageCode(firstText(readText(commentContext, "stage"), readText(queryContext, "stage")));
        }
        if (record.getBizDate() == null) {
            record.setBizDate(firstDate(readDate(commentContext, "biz_date"), readDate(queryContext, "bizDate")));
        }
        if (record.getQueryDateStart() == null) {
            record.setQueryDateStart(firstDate(readDate(queryContext, "queryDateStart"), readDate(queryContext, "queryDate")));
        }
        if (record.getQueryDateEnd() == null) {
            record.setQueryDateEnd(firstDate(readDate(queryContext, "queryDateEnd"), readDate(queryContext, "queryDate")));
        }
        if (!StringUtils.hasText(record.getQueryDateStatus())) {
            record.setQueryDateStatus(readText(queryContext, "queryDateStatus"));
        }
        if (!StringUtils.hasText(record.getAccessChannel())) {
            record.setAccessChannel(firstText(readText(queryContext, "accessChannel"), readText(commentContext, "access_channel")));
        }
        if (record.getParameterizedSqlFlag() == null) {
            record.setParameterizedSqlFlag(firstBoolean(
                readBoolean(bindingSummary, "parameterizedSqlFlag"),
                readBoolean(queryContext, "parameterizedSqlFlag")
            ));
        }
        if (!StringUtils.hasText(record.getBindingMode())) {
            record.setBindingMode(firstText(readText(bindingSummary, "bindingMode"), readText(queryContext, "bindingMode")));
        }
        if (!StringUtils.hasText(record.getBindingRenderStatus())) {
            record.setBindingRenderStatus(firstText(
                readText(bindingSummary, "bindingRenderStatus"),
                readText(queryContext, "bindingRenderStatus")
            ));
        }
        if (!StringUtils.hasText(record.getSqlTemplateFingerprint())) {
            record.setSqlTemplateFingerprint(firstText(
                readText(bindingSummary, "sqlTemplateFingerprint"),
                readText(queryContext, "sqlTemplateFingerprint")
            ));
        }
        if (!StringUtils.hasText(record.getBoundSqlFingerprint())) {
            record.setBoundSqlFingerprint(firstText(
                readText(bindingSummary, "boundSqlFingerprint"),
                readText(queryContext, "boundSqlFingerprint")
            ));
        }
        if (!StringUtils.hasText(record.getCommentContext()) && !commentContext.isEmpty()) {
            record.setCommentContext(JsonUtils.toJson(commentContext));
        }
        if (!StringUtils.hasText(record.getBindingSummary()) && !bindingSummary.isEmpty()) {
            record.setBindingSummary(JsonUtils.toJson(bindingSummary));
        }
        if (!StringUtils.hasText(record.getLogicalObjectHits()) && logicalObjectHits != null) {
            record.setLogicalObjectHits(toJsonIfPresent(logicalObjectHits));
        }
        if (!StringUtils.hasText(record.getRouteSummary()) && !routeSummary.isEmpty()) {
            record.setRouteSummary(JsonUtils.toJson(routeSummary));
        }
        if (!StringUtils.hasText(record.getCacheSummary()) && !cacheSummary.isEmpty()) {
            record.setCacheSummary(JsonUtils.toJson(cacheSummary));
        }
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

    private Map<String, Object> normalizeCacheSummary(Map<String, Object> primary,
                                                      Map<String, Object> secondary,
                                                      Map<String, Object> existing) {
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<String, Object>();
        putIfPresent(normalized, "cacheHit", firstBoolean(readBoolean(primary, "cacheHit"), readBoolean(secondary, "cacheHit")));
        putIfPresent(normalized, "cacheGovernanceStatus", firstText(
            readText(primary, "cacheGovernanceStatus"),
            readText(secondary, "cacheGovernanceStatus")
        ));
        Object cacheEvidence = firstNonNull(primary.get("cacheGovernanceEvidence"), secondary.get("cacheGovernanceEvidence"));
        if (cacheEvidence != null) {
            normalized.put("cacheGovernanceEvidence", cacheEvidence);
        }
        return normalized;
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return JsonUtils.objectMapper().readValue(
                json,
                JsonUtils.objectMapper().getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
            );
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> readMap(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || !StringUtils.hasText(key)) {
            return Collections.emptyMap();
        }
        Object value = source.get(key);
        if (!(value instanceof Map)) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<String, Object>();
        copy.putAll((Map<String, Object>) value);
        return copy;
    }

    private String readText(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Boolean readBoolean(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    private Long readLong(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || !StringUtils.hasText(key)) {
            return null;
        }
        Object value = source.get(key);
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate readDate(Map<String, Object> source, String key) {
        String value = readText(source, key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String toJsonIfPresent(Object value) {
        return value == null ? null : JsonUtils.toJson(value);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || !StringUtils.hasText(key) || value == null) {
            return;
        }
        target.put(key, value);
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private Boolean firstBoolean(Boolean... values) {
        if (values == null) {
            return null;
        }
        for (Boolean value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long firstLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private LocalDate firstDate(LocalDate... values) {
        if (values == null) {
            return null;
        }
        for (LocalDate value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Map<String, Object> firstMap(Map<String, Object>... values) {
        if (values == null) {
            return Collections.emptyMap();
        }
        for (Map<String, Object> value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return Collections.emptyMap();
    }

    private Object firstNonNull(Object first, Object second) {
        return first != null ? first : second;
    }

    private BizException invalidTraceabilityReference(String fieldName, String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            message + " (" + fieldName + ")"
        );
    }
}
