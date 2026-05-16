package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.domain.query.FaultToleranceStrategy;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QueryExecutionResultDigestService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "READONLY_RESULT_DIGEST_BASELINE";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int DEFAULT_SAMPLE_LIMIT = 5;
    private static final int MAX_SAMPLE_LIMIT = 20;

    private final QueryExecutionApplicationService queryExecutionApplicationService;

    public QueryExecutionResultDigestService(QueryExecutionApplicationService queryExecutionApplicationService) {
        this.queryExecutionApplicationService = queryExecutionApplicationService;
    }

    public QueryExecutionResultDigestResponse executeDigest(QueryExecutionResultDigestRequest request) {
        QueryExecuteResponse queryResponse = queryExecutionApplicationService.executeSynchronously(toQueryRequest(request));
        QueryExecutionResultDigestResponse response = new QueryExecutionResultDigestResponse();
        response.setTenantId(request == null ? null : request.getTenantId());
        response.setValidationRunId(request == null ? null : request.getValidationRunId());
        response.setRewriteRecordId(request == null ? null : request.getRewriteRecordId());
        response.setSqlFingerprint(resolveFingerprint(request, queryResponse));
        response.setStatus(queryResponse.getStatus().name());
        response.setTargetEngine(queryResponse.getMetadata() == null ? null : queryResponse.getMetadata().getTargetEngine());
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);
        response.setExecutionEvidence(buildExecutionEvidence(queryResponse, request));
        if (QueryExecutionStatus.SUCCESS == queryResponse.getStatus()
            || QueryExecutionStatus.PARTIAL == queryResponse.getStatus()) {
            List<Map<String, Object>> rows = safeRows(queryResponse.getRows());
            int sampleLimit = resolveSampleLimit(request == null ? null : request.getComparisonPolicy());
            response.setResultDigest(buildResultDigest(rows, sampleLimit, request));
            response.setLimitedSample(copyLimitedSample(rows, sampleLimit));
        } else {
            response.setResultDigest(Collections.<String, Object>emptyMap());
            response.setLimitedSample(Collections.<Map<String, Object>>emptyList());
            QueryErrorDetailVO error = queryResponse.getError();
            response.setErrorCode(error == null ? null : String.valueOf(error.getCode()));
            response.setErrorMessage(error == null ? queryResponse.getDegradeReason() : error.getMessage());
        }
        return response;
    }

    private QueryExecuteRequest toQueryRequest(QueryExecutionResultDigestRequest request) {
        QueryExecuteRequest queryRequest = new QueryExecuteRequest();
        queryRequest.setTenantId(request == null ? null : request.getTenantId());
        queryRequest.setSqlText(request == null ? null : request.getSqlText());
        queryRequest.setDatasourceType(resolveDatasourceType(request));
        queryRequest.setFaultToleranceStrategy(FaultToleranceStrategy.FAIL_FAST);
        return queryRequest;
    }

    private DataSourceTypeEnum resolveDatasourceType(QueryExecutionResultDigestRequest request) {
        if (request == null || request.getDatasourceType() == null) {
            return DataSourceTypeEnum.AUTO;
        }
        return request.getDatasourceType();
    }

    private String resolveFingerprint(QueryExecutionResultDigestRequest request, QueryExecuteResponse queryResponse) {
        if (queryResponse != null && StringUtils.hasText(queryResponse.getSqlFingerprint())) {
            return queryResponse.getSqlFingerprint();
        }
        if (request != null && StringUtils.hasText(request.getSqlFingerprint())) {
            return request.getSqlFingerprint();
        }
        return SqlFingerprintUtils.fingerprint(request == null ? null : request.getSqlText());
    }

    private Map<String, Object> buildResultDigest(List<Map<String, Object>> rows,
                                                  int sampleLimit,
                                                  QueryExecutionResultDigestRequest request) {
        List<Map<String, Object>> schema = buildSchema(rows);
        List<String> schemaColumns = schemaColumnNames(schema);
        List<String> rowHashes = rowHashes(rows, schemaColumns);
        Map<String, Object> digest = new LinkedHashMap<String, Object>();
        digest.put("schema", schema);
        digest.put("schemaDigest", sha256Hex(JsonUtils.toJson(schema)));
        digest.put("rowCount", Long.valueOf(rows.size()));
        digest.put("sampleRowCount", Integer.valueOf(Math.min(rows.size(), sampleLimit)));
        digest.put("sampleLimit", Integer.valueOf(sampleLimit));
        digest.put("sampled", Boolean.valueOf(rows.size() > sampleLimit));
        digest.put("rowHashAlgorithm", HASH_ALGORITHM);
        digest.put("orderDigest", sha256Hex(JsonUtils.toJson(rowHashes)));
        digest.put("checksumDigest", sha256Hex(JsonUtils.toJson(sortedCopy(rowHashes))));
        digest.put("keySetDigest", buildKeySetDigest(rows, resolveStringList(policyValue(request, "keyColumns"))));
        digest.put("orderSensitive", Boolean.valueOf(resolveBoolean(policyValue(request, "orderSensitive"), false)));
        digest.put("readonlyDigestOnly", Boolean.TRUE);
        return digest;
    }

    private List<Map<String, Object>> buildSchema(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> first = rows.get(0);
        List<Map<String, Object>> schema = new ArrayList<Map<String, Object>>();
        int index = 0;
        for (Map.Entry<String, Object> entry : first.entrySet()) {
            Map<String, Object> column = new LinkedHashMap<String, Object>();
            column.put("name", entry.getKey());
            column.put("ordinal", Integer.valueOf(index++));
            column.put("type", typeName(entry.getValue()));
            if (entry.getValue() instanceof BigDecimal) {
                column.put("scale", Integer.valueOf(((BigDecimal) entry.getValue()).scale()));
            }
            schema.add(column);
        }
        return schema;
    }

    private String typeName(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Integer || value instanceof Long || value instanceof Short) {
            return "INTEGER";
        }
        if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            return "DECIMAL";
        }
        if (value instanceof Boolean) {
            return "BOOLEAN";
        }
        if (value instanceof TemporalAccessor || value instanceof java.util.Date) {
            return "TEMPORAL";
        }
        return "STRING";
    }

    private List<String> schemaColumnNames(List<Map<String, Object>> schema) {
        List<String> names = new ArrayList<String>(schema.size());
        for (Map<String, Object> column : schema) {
            Object name = column.get("name");
            if (name != null) {
                names.add(String.valueOf(name));
            }
        }
        return names;
    }

    private List<String> rowHashes(List<Map<String, Object>> rows, List<String> schemaColumns) {
        List<String> hashes = new ArrayList<String>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> canonical = new LinkedHashMap<String, Object>();
            for (String column : schemaColumns) {
                canonical.put(column, normalizeValue(row.get(column)));
            }
            hashes.add(sha256Hex(JsonUtils.toJson(canonical)));
        }
        return hashes;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return value;
    }

    private String buildKeySetDigest(List<Map<String, Object>> rows, List<String> keyColumns) {
        if (keyColumns.isEmpty()) {
            return "";
        }
        List<String> keyHashes = new ArrayList<String>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> key = new LinkedHashMap<String, Object>();
            for (String column : keyColumns) {
                key.put(column, normalizeValue(row.get(column)));
            }
            keyHashes.add(sha256Hex(JsonUtils.toJson(key)));
        }
        return sha256Hex(JsonUtils.toJson(sortedCopy(keyHashes)));
    }

    private Map<String, Object> buildExecutionEvidence(QueryExecuteResponse response,
                                                       QueryExecutionResultDigestRequest request) {
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("executionContract", IMPLEMENTATION_STAGE);
        evidence.put("readonlyDigestOnly", Boolean.TRUE);
        evidence.put("validationRunId", request == null ? null : request.getValidationRunId());
        evidence.put("rewriteRecordId", request == null ? null : request.getRewriteRecordId());
        evidence.put("requestedDatasourceType", request == null || request.getDatasourceType() == null
            ? null
            : request.getDatasourceType().name());
        evidence.put("datasourceCode", request == null ? null : request.getDatasourceCode());
        evidence.put("status", response.getStatus().name());
        evidence.put("degraded", Boolean.valueOf(response.isDegraded()));
        evidence.put("degradeReason", response.getDegradeReason());
        QueryExecutionMetadataVO metadata = response.getMetadata();
        if (metadata != null) {
            evidence.put("targetEngine", metadata.getTargetEngine());
            evidence.put("elapsedMs", Long.valueOf(metadata.getElapsedMs()));
            evidence.put("scannedRows", Long.valueOf(metadata.getScannedRows()));
            evidence.put("returnedRowCount", Integer.valueOf(metadata.getRowCount()));
            evidence.put("executionMode", metadata.getExecutionMode());
            evidence.put("attemptedModes", metadata.getAttemptedModes());
            evidence.put("routeProfile", metadata.getRouteProfile());
            evidence.put("routeOrder", metadata.getRouteOrder());
            evidence.put("routeEvidenceSource", metadata.getRouteEvidenceSource());
            evidence.put("routeVerificationStatus", metadata.getRouteVerificationStatus());
            evidence.put("cacheGovernanceStatus", metadata.getCacheGovernanceStatus());
            evidence.put("cacheGovernanceEvidence", metadata.getCacheGovernanceEvidence());
        }
        return evidence;
    }

    private List<Map<String, Object>> copyLimitedSample(List<Map<String, Object>> rows, int sampleLimit) {
        List<Map<String, Object>> sample = new ArrayList<Map<String, Object>>();
        int limit = Math.min(rows.size(), sampleLimit);
        for (int index = 0; index < limit; index++) {
            sample.add(new LinkedHashMap<String, Object>(rows.get(index)));
        }
        return sample;
    }

    private Object policyValue(QueryExecutionResultDigestRequest request, String key) {
        if (request == null || request.getComparisonPolicy() == null) {
            return null;
        }
        return request.getComparisonPolicy().get(key);
    }

    private int resolveSampleLimit(Map<String, Object> policy) {
        Object value = policy == null ? null : policy.get("sampleLimit");
        int parsed = DEFAULT_SAMPLE_LIMIT;
        if (value instanceof Number) {
            parsed = ((Number) value).intValue();
        } else if (value != null) {
            try {
                parsed = Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                parsed = DEFAULT_SAMPLE_LIMIT;
            }
        }
        if (parsed < 1) {
            return DEFAULT_SAMPLE_LIMIT;
        }
        return Math.min(parsed, MAX_SAMPLE_LIMIT);
    }

    private List<String> resolveStringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<String> result = new ArrayList<String>(raw.size());
        for (Object item : raw) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private boolean resolveBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
    }

    private List<Map<String, Object>> safeRows(List<Map<String, Object>> rows) {
        return rows == null ? Collections.<Map<String, Object>>emptyList() : rows;
    }

    private List<String> sortedCopy(List<String> values) {
        List<String> copy = new ArrayList<String>(values);
        Collections.sort(copy);
        return copy;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item & 0xff));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("无法计算结果摘要", ex);
        }
    }
}
