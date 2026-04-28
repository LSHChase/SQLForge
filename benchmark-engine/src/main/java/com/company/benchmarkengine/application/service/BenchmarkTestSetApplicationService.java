package com.company.benchmarkengine.application.service;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetFieldMappingDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTestSetRepository;
import com.company.benchmarkengine.infrastructure.governance.BenchmarkAuditRecord;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BenchmarkTestSetApplicationService {

    private static final String CREATE_OPERATION = "BENCHMARK_TEST_SET_CREATE";
    private static final String QUERY_OPERATION = "BENCHMARK_TEST_SET_QUERY";
    private static final String RESOURCE_TYPE_TEST_SET = "BENCHMARK_ENGINE_TEST_SET";
    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.objectMapper();

    private final BenchmarkTestSetModelApplicationService benchmarkTestSetModelApplicationService;
    private final BenchmarkTestSetRepository benchmarkTestSetRepository;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public BenchmarkTestSetApplicationService(BenchmarkTestSetModelApplicationService benchmarkTestSetModelApplicationService,
                                              BenchmarkTestSetRepository benchmarkTestSetRepository,
                                              GovernanceCapabilityClient governanceCapabilityClient) {
        this.benchmarkTestSetModelApplicationService = benchmarkTestSetModelApplicationService;
        this.benchmarkTestSetRepository = benchmarkTestSetRepository;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    public BenchmarkTestSetResponse createTestSet(BenchmarkTestSetCreateRequest request) {
        long start = System.currentTimeMillis();
        request.setTenantId(requireAuthorizedTenant(request.getTenantId()));
        String testSetId = UUID.randomUUID().toString();
        try {
            String importBatchId = "import-" + UUID.randomUUID().toString();
            List<ImportedRow> rows = parseRows(request);
            if (rows.isEmpty()) {
                throw invalidArgument("contentBase64", "No parseable benchmark test-set rows were found in the uploaded payload");
            }
            List<BenchmarkTestSetCase> cases = toCases(testSetId, rows);
            BenchmarkTestSet testSet = benchmarkTestSetModelApplicationService.buildImportedTestSet(
                request,
                testSetId,
                importBatchId,
                cases,
                Instant.now(),
                RequestContext.getUserId()
            );
            assertAuthorization(testSet.getTenantId(), testSetId, CREATE_OPERATION);
            benchmarkTestSetRepository.saveTestSet(testSet);
            BenchmarkTestSetResponse response = benchmarkTestSetModelApplicationService.buildResponse(testSet);
            writeAudit(
                CREATE_OPERATION,
                testSetId,
                testSet.getStatus().name(),
                System.currentTimeMillis() - start,
                buildRequestSummary(request),
                buildResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            writeAudit(
                CREATE_OPERATION,
                testSetId,
                "FAILED",
                System.currentTimeMillis() - start,
                buildRequestSummary(request),
                buildResponseSummary(null, ex.getMessage())
            );
            throw ex;
        }
    }

    public BenchmarkTestSetResponse getTestSet(String testSetId) {
        long start = System.currentTimeMillis();
        try {
            BenchmarkTestSet testSet = benchmarkTestSetRepository.findTestSetByTestSetId(requireText(testSetId, "testSetId"));
            if (testSet == null) {
                throw new BizException(
                    ErrorCodeConstants.BENCHMARK_TASK_NOT_FOUND,
                    HttpStatus.NOT_FOUND,
                    "Benchmark test set does not exist for testSetId=" + testSetId
                );
            }
            verifyTenantAccess(testSet.getTenantId(), "Authenticated tenant cannot access this benchmark test set");
            assertAuthorization(testSet.getTenantId(), testSetId, QUERY_OPERATION);
            BenchmarkTestSetResponse response = benchmarkTestSetModelApplicationService.buildResponse(testSet);
            writeAudit(
                QUERY_OPERATION,
                testSetId,
                testSet.getStatus().name(),
                System.currentTimeMillis() - start,
                buildResourceSummary(testSet),
                buildResponseSummary(response, null)
            );
            return response;
        } catch (RuntimeException ex) {
            writeAudit(
                QUERY_OPERATION,
                testSetId,
                "FAILED",
                System.currentTimeMillis() - start,
                null,
                buildResponseSummary(null, ex.getMessage())
            );
            throw ex;
        }
    }

    private List<ImportedRow> parseRows(BenchmarkTestSetCreateRequest request) {
        byte[] content = decodeBase64(requireText(request.getImportRequest().getContentBase64(), "contentBase64"));
        switch (request.getImportRequest().getFileType()) {
            case CSV:
            case TXT:
                return parseDelimitedRows(request, content);
            case XLSX:
            case XLS:
                return parseWorkbookRows(request, content);
            default:
                throw invalidArgument("fileType", "Unsupported benchmark test-set file type");
        }
    }

    private List<ImportedRow> parseDelimitedRows(BenchmarkTestSetCreateRequest request, byte[] content) {
        Charset charset = resolveCharset(request.getImportRequest().getCharset());
        char delimiter = resolveDelimiter(request);
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreSurroundingSpaces(true)
                .setTrim(true)
                .setDelimiter(delimiter)
                .build();
            CSVParser parser = format.parse(new InputStreamReader(new ByteArrayInputStream(content), charset));
            return toImportedRows(parser.getRecords(), parser.getHeaderMap().keySet(), request);
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse benchmark test-set delimited file: " + ex.getMessage());
        }
    }

    private List<ImportedRow> parseWorkbookRows(BenchmarkTestSetCreateRequest request, byte[] content) {
        try {
            Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content));
            Sheet sheet = workbook.getNumberOfSheets() <= 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 0) {
                return Collections.emptyList();
            }
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            List<String> headers = new ArrayList<String>();
            int lastCellIndex = headerRow == null ? 0 : headerRow.getLastCellNum();
            int cellIndex;
            for (cellIndex = 0; cellIndex < lastCellIndex; cellIndex++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(cellIndex)));
            }
            List<Map<String, String>> records = new ArrayList<Map<String, String>>();
            int rowIndex;
            for (rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<String, String>();
                for (cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    values.put(headers.get(cellIndex), formatter.formatCellValue(row.getCell(cellIndex)));
                }
                if (!isBlankRecord(values)) {
                    records.add(values);
                }
            }
            return toImportedRows(records, headers, request);
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "Failed to parse benchmark test-set workbook: " + ex.getMessage());
        }
    }

    private List<ImportedRow> toImportedRows(Iterable<CSVRecord> records,
                                             Set<String> headers,
                                             BenchmarkTestSetCreateRequest request) {
        ensureRequiredHeaders(headers, request.getImportRequest().getFieldMappings());
        List<ImportedRow> rows = new ArrayList<ImportedRow>();
        int sequence = 0;
        for (CSVRecord record : records) {
            Map<String, String> values = new LinkedHashMap<String, String>();
            for (String header : headers) {
                values.put(header, record.get(header));
            }
            if (isBlankRecord(values)) {
                continue;
            }
            sequence++;
            rows.add(buildImportedRow(sequence, (int) record.getRecordNumber() + 1, values, request));
        }
        return rows;
    }

    private List<ImportedRow> toImportedRows(List<Map<String, String>> records,
                                             List<String> headers,
                                             BenchmarkTestSetCreateRequest request) {
        ensureRequiredHeaders(new LinkedHashSet<String>(headers), request.getImportRequest().getFieldMappings());
        List<ImportedRow> rows = new ArrayList<ImportedRow>();
        int sequence = 0;
        for (Map<String, String> record : records) {
            if (isBlankRecord(record)) {
                continue;
            }
            sequence++;
            rows.add(buildImportedRow(sequence, sequence + 1, record, request));
        }
        return rows;
    }

    private ImportedRow buildImportedRow(int sequence,
                                         int sourceLineNumber,
                                         Map<String, String> values,
                                         BenchmarkTestSetCreateRequest request) {
        Map<BenchmarkTestSetField, String> fieldValues = new LinkedHashMap<BenchmarkTestSetField, String>();
        for (BenchmarkTestSetFieldMappingDTO mapping : request.getImportRequest().getFieldMappings()) {
            fieldValues.put(mapping.getField(), trimToNull(values.get(mapping.getColumnName())));
        }
        return new ImportedRow(sequence, sourceLineNumber, values, fieldValues);
    }

    private List<BenchmarkTestSetCase> toCases(String testSetId, List<ImportedRow> rows) {
        List<BenchmarkTestSetCase> cases = new ArrayList<BenchmarkTestSetCase>(rows.size());
        for (ImportedRow row : rows) {
            String rejectionReason = validateImportedRow(row);
            String sqlText = row.getFieldValue(BenchmarkTestSetField.SQL_TEXT);
            String sqlFingerprint = row.getFieldValue(BenchmarkTestSetField.SQL_FINGERPRINT);
            if (sqlFingerprint == null && sqlText != null) {
                sqlFingerprint = SqlFingerprintUtils.fingerprint(sqlText);
            }
            cases.add(
                new BenchmarkTestSetCase(
                    UUID.randomUUID().toString(),
                    testSetId,
                    Integer.valueOf(row.sequenceNumber),
                    Integer.valueOf(row.sourceLineNumber),
                    firstNonBlank(row.getFieldValue(BenchmarkTestSetField.CASE_NAME), "CASE-" + row.sequenceNumber),
                    sqlText,
                    sqlFingerprint,
                    row.getFieldValue(BenchmarkTestSetField.DATASOURCE_CODE),
                    row.getFieldValue(BenchmarkTestSetField.REPORT_CODE),
                    parseTags(row.getFieldValue(BenchmarkTestSetField.TAGS)),
                    normalizeBindParameters(row.getFieldValue(BenchmarkTestSetField.BIND_PARAMETERS_JSON)),
                    rejectionReason == null ? BenchmarkTestSetCaseStatus.ACCEPTED : BenchmarkTestSetCaseStatus.REJECTED,
                    rejectionReason,
                    JsonUtils.toJson(row.rawValues)
                )
            );
        }
        return cases;
    }

    private String validateImportedRow(ImportedRow row) {
        String sqlText = row.getFieldValue(BenchmarkTestSetField.SQL_TEXT);
        if (sqlText == null) {
            return "sqlText is required";
        }
        String readonlyFailure = BenchmarkReadonlySqlSupport.validateReadonlySql(sqlText);
        if (readonlyFailure != null) {
            return readonlyFailure;
        }
        String bindParametersJson = row.getFieldValue(BenchmarkTestSetField.BIND_PARAMETERS_JSON);
        if (bindParametersJson != null) {
            try {
                OBJECT_MAPPER.readTree(bindParametersJson);
            } catch (Exception ex) {
                return "bindParametersJson must be valid JSON";
            }
        }
        return null;
    }

    private String normalizeBindParameters(String bindParametersJson) {
        if (bindParametersJson == null) {
            return null;
        }
        try {
            return JsonUtils.toJson(OBJECT_MAPPER.readTree(bindParametersJson));
        } catch (Exception ex) {
            return bindParametersJson;
        }
    }

    private List<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalized = rawTags.replace(';', ',');
        String[] parts = normalized.split(",");
        List<String> tags = new ArrayList<String>();
        int index;
        for (index = 0; index < parts.length; index++) {
            String value = trimToNull(parts[index]);
            if (value != null) {
                tags.add(value);
            }
        }
        return tags;
    }

    private void ensureRequiredHeaders(Set<String> headers, List<BenchmarkTestSetFieldMappingDTO> fieldMappings) {
        int index;
        for (index = 0; index < fieldMappings.size(); index++) {
            BenchmarkTestSetFieldMappingDTO mapping = fieldMappings.get(index);
            if (!headers.contains(mapping.getColumnName())) {
                throw invalidArgument("fieldMappings", "Mapped column is missing from uploaded file header: " + mapping.getColumnName());
            }
        }
    }

    private boolean isBlankRecord(Map<String, String> values) {
        for (String value : values.values()) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Charset resolveCharset(String charsetName) {
        if (charsetName == null || charsetName.trim().isEmpty()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(charsetName.trim());
        } catch (Exception ex) {
            throw invalidArgument("charset", "Unsupported charset: " + charsetName);
        }
    }

    private char resolveDelimiter(BenchmarkTestSetCreateRequest request) {
        String delimiter = trimToNull(request.getImportRequest().getDelimiter());
        if (delimiter != null) {
            return delimiter.charAt(0);
        }
        return request.getImportRequest().getFileType() == com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType.TXT
            ? '\t'
            : ',';
    }

    private byte[] decodeBase64(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (Exception ex) {
            throw invalidArgument("contentBase64", "contentBase64 is not valid Base64");
        }
    }

    private void assertAuthorization(String tenantId, String resourceId, String operationCode) {
        governanceCapabilityClient.assertAuthorization(
            tenantId,
            DataSourceTypeEnum.HETU,
            RESOURCE_TYPE_TEST_SET,
            resourceId,
            operationCode
        );
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (requestTenantId != null && requestTenantId.trim().length() > 0
            && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("Request tenantId does not match authenticated tenant context");
        }
        return contextTenantId;
    }

    private void verifyTenantAccess(String resourceTenantId, String message) {
        String contextTenantId = RequestContext.getTenantId();
        if (contextTenantId == null || contextTenantId.trim().isEmpty()) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context"
            );
        }
        if (!contextTenantId.equals(resourceTenantId)) {
            throw new AccessDeniedException(message);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw invalidArgument(fieldName, fieldName + " is required");
        }
        return value.trim();
    }

    private BizException invalidArgument(String fieldName, String message) {
        return new BizException(
            ErrorCodeConstants.BENCHMARK_TASK_INVALID,
            HttpStatus.BAD_REQUEST,
            "Invalid benchmark test set " + fieldName + ": " + message
        );
    }

    private void writeAudit(String operationCode,
                            String resourceId,
                            String resultStatus,
                            long elapsedMs,
                            String requestParams,
                            String responseSummary) {
        governanceCapabilityClient.writeAudit(
            new BenchmarkAuditRecord(
                operationCode,
                RESOURCE_TYPE_TEST_SET,
                resourceId,
                resultStatus,
                elapsedMs,
                null,
                null,
                null,
                null,
                null,
                requestParams,
                responseSummary
            )
        );
    }

    private String buildRequestSummary(BenchmarkTestSetCreateRequest request) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", request.getTenantId());
        payload.put("testSetName", request.getTestSetName());
        payload.put("templateId", request.getTemplateId());
        payload.put("templateType", request.getTemplateType() == null ? null : request.getTemplateType().name());
        payload.put("testSetSource", request.getTestSetSource().name());
        payload.put("fileType", request.getImportRequest().getFileType().name());
        payload.put("fileName", request.getImportRequest().getFileName());
        return JsonUtils.toJson(payload);
    }

    private String buildResourceSummary(BenchmarkTestSet testSet) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.BENCHMARK_ENGINE);
        payload.put("tenantId", testSet.getTenantId());
        payload.put("testSetId", testSet.getTestSetId());
        payload.put("testSetSource", testSet.getTestSetSource().name());
        payload.put("status", testSet.getStatus().name());
        payload.put("acceptedCases", testSet.getAcceptedCases());
        payload.put("rejectedCases", testSet.getRejectedCases());
        return JsonUtils.toJson(payload);
    }

    private String buildResponseSummary(BenchmarkTestSetResponse response, String failureReason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("resultStatus", response == null ? "FAILED" : response.getStatus().name());
        payload.put("acceptedCases", response == null ? null : response.getAcceptedCases());
        payload.put("rejectedCases", response == null ? null : response.getRejectedCases());
        payload.put("failureReason", failureReason);
        return JsonUtils.toJson(payload);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.trim().isEmpty() ? fallback : primary;
    }

    private static final class ImportedRow {
        private final int sequenceNumber;
        private final int sourceLineNumber;
        private final Map<String, String> rawValues;
        private final Map<BenchmarkTestSetField, String> fieldValues;

        private ImportedRow(int sequenceNumber,
                            int sourceLineNumber,
                            Map<String, String> rawValues,
                            Map<BenchmarkTestSetField, String> fieldValues) {
            this.sequenceNumber = sequenceNumber;
            this.sourceLineNumber = sourceLineNumber;
            this.rawValues = rawValues;
            this.fieldValues = fieldValues;
        }

        private String getFieldValue(BenchmarkTestSetField field) {
            return fieldValues.get(field);
        }
    }
}
