package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryContextDTO;
import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.config.QueryExecutionRewriteProperties;
import com.company.queryexecution.domain.query.ActivatedAccelerationBinding;
import com.company.queryexecution.domain.query.QueryExecutionStatus;
import com.company.queryexecution.domain.query.ReadonlyQueryAssessment;
import com.company.queryexecution.domain.query.ReadonlyQueryGuard;
import com.company.queryexecution.infrastructure.adapter.QueryExecutionAdapter;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.access.AccessAuditContract;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.jdbcagent.JdbcAgentSqlCommentParser;
import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;

abstract class QueryExecutionApplicationServiceSupport {

    protected static final Logger LOGGER = LoggerFactory.getLogger(QueryExecutionApplicationService.class);

    protected static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    protected static final String IMPLEMENTATION_STAGE = "HETU_REAL_INTEGRATION";
    protected static final String OPERATION = "QUERY_EXECUTE_SYNC";
    protected static final String READONLY_SQL_REJECTION_MESSAGE = "当前同步查询路径仅允许只读单语句 SQL";
    protected static final String ROUTE_UNAVAILABLE_MESSAGE = "当前同步查询路径尚未为目标数据源开放执行路由";
    protected static final String QUERY_TIMEOUT_MESSAGE = "联机查询在当前超时阈值内未完成";
    protected static final String FALLBACK_IMMEDIATE_REASON = "当前容错策略要求立即走兜底路径。";
    protected static final String TIMEOUT_FALLBACK_REASON = "主引擎超过超时阈值，已应用兜底策略。";
    protected static final String STATE_REQUEST_ACCEPTED = "REQUEST_ACCEPTED";
    protected static final String STATE_RISK_REJECTED = "RISK_REJECTED";
    protected static final String STATE_ROUTE_UNAVAILABLE = "ROUTE_UNAVAILABLE";
    protected static final String STATE_PRIMARY_ROUTE_SELECTED = "PRIMARY_ROUTE_SELECTED";
    protected static final String STATE_PRIMARY_MODE_CHAIN_FAILED = "PRIMARY_MODE_CHAIN_FAILED";
    protected static final String STATE_PRIMARY_TIMEOUT = "PRIMARY_TIMEOUT";
    protected static final String STATE_RUNTIME_REWRITE_EXECUTION_FAILED = "RUNTIME_REWRITE_EXECUTION_FAILED";
    protected static final String STATE_RUNTIME_REWRITE_ORIGINAL_RETRY = "RUNTIME_REWRITE_ORIGINAL_RETRY";
    protected static final String STATE_DEV_REWRITE_DIRECT_SUCCESS = "DEV_REWRITE_DIRECT_SUCCESS";
    protected static final String STATE_LOCAL_ROLLBACK_MARKED = "LOCAL_ROLLBACK_MARKED";
    protected static final String STATE_FALLBACK_REQUESTED = "FALLBACK_REQUESTED";
    protected static final String STATE_LOCAL_COMPENSATION_MARKED = "LOCAL_COMPENSATION_MARKED";
    protected static final String STATE_COMPLETED = "COMPLETED";
    protected static final String MARKER_TIMEOUT_ROLLBACK = "LOCAL_TIMEOUT_ROLLBACK_MARKED";
    protected static final String MARKER_PRIMARY_ROUTE_FAILURE = "LOCAL_PRIMARY_ROUTE_FAILURE_MARKED";
    protected static final String MARKER_FALLBACK_COMPENSATION = "LOCAL_FALLBACK_COMPENSATION_MARKED";
    protected static final String MARKER_RUNTIME_REWRITE_ORIGINAL_RETRY = "LOCAL_RUNTIME_REWRITE_ORIGINAL_SQL_RETRY";
    protected static final String ACTION_CLOSE_PRIMARY_ATTEMPT_CONTEXT = "CLOSE_PRIMARY_ATTEMPT_CONTEXT";
    protected static final String ACTION_RECORD_DEGRADED_RESULT = "RECORD_DEGRADED_RESULT";
    protected static final String ACTION_RETRY_ORIGINAL_SQL = "RETRY_ORIGINAL_SQL_AFTER_REWRITE_FAILURE";
    protected static final String RESOURCE_TYPE_QUERY = "QUERY_EXECUTION_QUERY";
    protected static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    protected static final Pattern ISO_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    protected static final Pattern NAMED_BINDING_PATTERN = Pattern.compile(":[A-Za-z][A-Za-z0-9_]*");
    protected static final Pattern POSITIONAL_BINDING_PATTERN = Pattern.compile("\\?");
    protected static final Pattern LOGICAL_OBJECT_PATTERN = Pattern.compile("(?i)\\b(?:from|join|into|update)\\s+([A-Za-z0-9_$.]+)");

    protected final QueryExecutionAdapter queryExecutionAdapter;
    protected final GovernanceCapabilityClient governanceCapabilityClient;
    protected final QueryExecutionMetricsRecorder metricsRecorder;
    protected final QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService;
    protected final QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService;
    protected final QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService;
    protected final QueryExecutionRewriteProperties rewriteProperties;
    protected final Executor queryHistoryWriteExecutor;

    public QueryExecutionApplicationServiceSupport(QueryExecutionAdapter queryExecutionAdapter,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            QueryExecutionMetricsRecorder metricsRecorder,
                                            QueryExecutionAccelerationRuntimeService queryExecutionAccelerationRuntimeService,
                                            QueryExecutionCacheGovernanceRuntimeService queryExecutionCacheGovernanceRuntimeService,
                                            QueryExecutionRuntimeRewriteBindingService queryExecutionRuntimeRewriteBindingService,
                                            QueryExecutionRewriteProperties rewriteProperties,
                                            @Qualifier("tenantAwareTaskExecutor") Executor queryHistoryWriteExecutor) {
        this.queryExecutionAdapter = queryExecutionAdapter;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.metricsRecorder = metricsRecorder;
        this.queryExecutionAccelerationRuntimeService = queryExecutionAccelerationRuntimeService;
        this.queryExecutionCacheGovernanceRuntimeService = queryExecutionCacheGovernanceRuntimeService;
        this.queryExecutionRuntimeRewriteBindingService = queryExecutionRuntimeRewriteBindingService;
        this.rewriteProperties = rewriteProperties == null
            ? new QueryExecutionRewriteProperties()
            : rewriteProperties;
        this.queryHistoryWriteExecutor = queryHistoryWriteExecutor == null
            ? directExecutor()
            : queryHistoryWriteExecutor;
    }

    protected static Executor directExecutor() {
        return new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    protected Map<String, Object> buildHistoryQueryContext(QueryExecuteRequest request,
                                                         QueryExecuteResponse response,
                                                         String resultStatus,
                                                         String failureReason,
                                                         AccessAuditContract accessAuditContract,
                                                         RuntimeRewriteResolution fallbackRewriteResolution) {
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("serviceCode", ServiceCodeConstants.QUERY_EXECUTION);
        payload.put("operationCode", OPERATION);
        payload.put("resourceType", RESOURCE_TYPE_QUERY);
        payload.put("accessChannel", accessAuditContract.getAccessChannel().name());
        payload.put("authSource", accessAuditContract.getAuthSource());
        payload.put("tenantId", request.getTenantId());
        payload.put("datasourceType", request.getDatasourceType() == null ? null : request.getDatasourceType().name());
        payload.put("faultToleranceStrategy", request.getFaultToleranceStrategy() == null
            ? null
            : request.getFaultToleranceStrategy().name());
        payload.put("accelerationPreference", request.getAccelerationPreference() == null
            ? null
            : request.getAccelerationPreference().name());
        payload.put("requestContext", buildRequestContextPayload(request.getQueryContext()));
        payload.put("resultStatus", resultStatus);
        payload.put("targetEngine", response == null || response.getMetadata() == null
            ? null
            : response.getMetadata().getTargetEngine());
        payload.put("returnedRowCount", response == null || response.getMetadata() == null
            ? Long.valueOf(0L)
            : Long.valueOf(response.getMetadata().getRowCount()));
        payload.put("scannedRows", response == null || response.getMetadata() == null
            ? Long.valueOf(0L)
            : Long.valueOf(response.getMetadata().getScannedRows()));
        payload.put("elapsedMs", response == null || response.getMetadata() == null
            ? null
            : Long.valueOf(response.getMetadata().getElapsedMs()));
        payload.put("degraded", Boolean.valueOf(response != null && response.isDegraded()));
        payload.put("degradeReason", response == null ? null : response.getDegradeReason());
        payload.put("retryPathSize", response == null || response.getRetryPath() == null
            ? Integer.valueOf(0)
            : Integer.valueOf(response.getRetryPath().size()));
        payload.put("commentContext", response == null ? null : response.getCommentContext());
        payload.put("queryDateSummary", response == null ? null : response.getQueryDateSummary());
        payload.put("bindingSummary", response == null ? null : response.getBindingSummary());
        payload.put("logicalObjectHits", response == null ? null : response.getLogicalObjectHits());
        payload.put("routeSummary", response == null ? null : response.getRouteSummary());
        payload.put("cacheSummary", response == null ? null : response.getCacheSummary());
        payload.put("rewriteApplied", metadata == null
            ? Boolean.valueOf(fallbackRewriteResolution != null && fallbackRewriteResolution.isRewriteApplied())
            : Boolean.valueOf(metadata.isRewriteApplied()));
        payload.put("rewriteRecordId", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteRecordId()
            : metadata.getRewriteRecordId());
        payload.put("runtimeBindingId", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeBindingId()
            : metadata.getRuntimeBindingId());
        payload.put("ruleVersion", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuleVersion()
            : metadata.getRuleVersion());
        payload.put("runtimeRuleVersion", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeRuleVersion()
            : metadata.getRuntimeRuleVersion());
        payload.put("runtimeRewriteStatus", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRuntimeStatus()
            : metadata.getRuntimeRewriteStatus());
        payload.put("rewriteActivationStatusSnapshot", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteActivationStatusSnapshot()
            : metadata.getRewriteActivationStatusSnapshot());
        payload.put("rewriteFallbackReason", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getRewriteFallbackReason()
            : metadata.getRewriteFallbackReason());
        payload.put("originalSqlFingerprint", response == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getOriginalSqlFingerprint()
            : response.getSqlFingerprint());
        payload.put("actualSqlFingerprint", metadata == null
            ? fallbackRewriteResolution == null ? null : fallbackRewriteResolution.getActualSqlFingerprint()
            : SqlFingerprintUtils.fingerprint(metadata.getActualSql()));
        if (response != null && response.getQueryDateSummary() != null) {
            payload.put("queryDateStart", response.getQueryDateSummary().get("queryDateStart"));
            payload.put("queryDateEnd", response.getQueryDateSummary().get("queryDateEnd"));
            payload.put("queryDateStatus", response.getQueryDateSummary().get("queryDateStatus"));
        }
        payload.put("failureReason", failureReason);
        return payload;
    }

    protected Map<String, Object> buildRequestContextPayload(QueryContextDTO queryContext) {
        if (queryContext == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("databaseName", queryContext.getDatabaseName());
        payload.put("schemaVersion", queryContext.getSchemaVersion());
        payload.put("timeoutMs", queryContext.getTimeoutMs());
        payload.put("sessionVariables", queryContext.getSessionVariables());
        return payload;
    }

    protected String resolveDatasourceCode(QueryExecuteRequest request, QueryExecuteResponse response) {
        Map<String, String> commentContext = response == null ? null : response.getCommentContext();
        return firstText(
            request == null ? null : request.getDatasourceCode(),
            extractRuntimeDatasourceCode(response),
            commentContext == null ? null : commentContext.get("datasource"),
            commentContext == null ? null : commentContext.get("datasource_code"),
            request == null || request.getQueryContext() == null ? null : request.getQueryContext().getDatabaseName(),
            request == null || request.getDatasourceType() == null ? null : request.getDatasourceType().name()
        );
    }

    protected String extractRuntimeDatasourceCode(QueryExecuteResponse response) {
        if (response == null || response.getBindingSummary() == null) {
            return null;
        }
        Object datasourceCode = response.getBindingSummary().get("runtimeDatasourceCode");
        return datasourceCode == null ? null : String.valueOf(datasourceCode);
    }

    protected String toJson(Object payload) {
        return payload == null ? null : JsonUtils.toJson(payload);
    }

    protected Map<String, String> buildCommentContext(String sqlText) {
        return JdbcAgentSqlCommentParser.parseLeadingComments(sqlText);
    }

    protected Map<String, Object> buildQueryDateSummary(String sqlText) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        if (!StringUtils.hasText(sqlText)) {
            summary.put("queryDateFields", Collections.<String>emptyList());
            summary.put("queryDateStatus", "UNRESOLVED");
            return summary;
        }
        List<LocalDate> dates = new ArrayList<LocalDate>();
        Matcher matcher = ISO_DATE_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            LocalDate parsed = tryParseDate(matcher.group(1));
            if (parsed != null) {
                dates.add(parsed);
            }
        }
        List<String> fields = detectQueryDateFields(sqlText);
        summary.put("queryDateFields", fields);
        if (!dates.isEmpty()) {
            dates.sort(Comparator.naturalOrder());
            summary.put("queryDateStart", dates.get(0).format(ISO_DATE));
            summary.put("queryDateEnd", dates.get(dates.size() - 1).format(ISO_DATE));
            summary.put("queryDateStatus", "RESOLVED");
            return summary;
        }
        summary.put("queryDateStatus", fields.isEmpty() ? "UNRESOLVED" : "PARTIAL");
        return summary;
    }

    protected Map<String, Object> buildBindingSummary(String sqlText, String sqlFingerprint) {
        return buildBindingSummary(
            sqlText,
            RuntimeRewriteResolution.noRewrite(sqlText, sqlFingerprint)
        );
    }

    protected Map<String, Object> buildBindingSummary(String sqlText,
                                                    RuntimeRewriteResolution runtimeRewriteResolution) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        int namedBindings = countMatches(NAMED_BINDING_PATTERN, sqlText);
        int positionalBindings = countMatches(POSITIONAL_BINDING_PATTERN, sqlText);
        boolean parameterized = namedBindings > 0 || positionalBindings > 0;
        summary.put("parameterizedSqlFlag", Boolean.valueOf(parameterized));
        if (namedBindings > 0) {
            summary.put("bindingMode", "NAMED");
        } else if (positionalBindings > 0) {
            summary.put("bindingMode", "POSITIONAL");
        } else {
            summary.put("bindingMode", "NONE");
        }
        summary.put("bindingRenderStatus", parameterized ? "PARTIAL" : "SUCCESS");
        summary.put("bindingParameterCount", Integer.valueOf(namedBindings + positionalBindings));
        summary.put("sqlTemplateFingerprint", runtimeRewriteResolution.getOriginalSqlFingerprint());
        summary.put("boundSqlFingerprint", parameterized ? null : runtimeRewriteResolution.getActualSqlFingerprint());
        summary.put("actualSqlFingerprint", runtimeRewriteResolution.getActualSqlFingerprint());
        summary.put("rewriteApplied", Boolean.valueOf(runtimeRewriteResolution.isRewriteApplied()));
        summary.put("runtimeRewriteStatus", runtimeRewriteResolution.getRuntimeStatus());
        summary.put("rewriteRecordId", runtimeRewriteResolution.getRewriteRecordId());
        summary.put("runtimeBindingId", runtimeRewriteResolution.getRuntimeBindingId());
        summary.put("ruleVersion", runtimeRewriteResolution.getRuleVersion());
        summary.put("runtimeRuleVersion", runtimeRewriteResolution.getRuntimeRuleVersion());
        summary.put("runtimeDatasourceCode", runtimeRewriteResolution.getDatasourceCode());
        summary.put("rewriteActivationStatusSnapshot", runtimeRewriteResolution.getRewriteActivationStatusSnapshot());
        if (StringUtils.hasText(runtimeRewriteResolution.getRewriteFallbackReason())) {
            summary.put("rewriteFallbackReason", runtimeRewriteResolution.getRewriteFallbackReason());
        }
        return summary;
    }

    protected List<LogicalObjectSurface> buildLogicalObjectHits(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        List<LogicalObjectSurface> hits = new ArrayList<LogicalObjectSurface>();
        List<String> seenKeys = new ArrayList<String>();
        Matcher matcher = LOGICAL_OBJECT_PATTERN.matcher(sqlText);
        while (matcher.find()) {
            String rawReference = sanitizeObjectReference(matcher.group(1));
            if (!StringUtils.hasText(rawReference)) {
                continue;
            }
            LogicalObjectSurface hit = toLogicalObjectSurface(rawReference);
            if (hit.getObjectKey() == null || seenKeys.contains(hit.getObjectKey())) {
                continue;
            }
            seenKeys.add(hit.getObjectKey());
            hits.add(hit);
        }
        return hits;
    }

    protected Map<String, Object> buildRouteSummary(String selectedEngine,
                                                  String executionMode,
                                                  String routeProfile,
                                                  List<String> routeOrder,
                                                  String routeEvidenceSource,
                                                  String routeVerificationStatus,
                                                  boolean degraded,
                                                  String degradeReason) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("selectedEngine", selectedEngine);
        summary.put("executionMode", executionMode);
        summary.put("routeProfile", routeProfile);
        summary.put("routeOrder", routeOrder == null ? Collections.<String>emptyList() : routeOrder);
        summary.put("routeEvidenceSource", routeEvidenceSource);
        summary.put("routeVerificationStatus", routeVerificationStatus);
        summary.put("degraded", Boolean.valueOf(degraded));
        if (StringUtils.hasText(degradeReason)) {
            summary.put("degradeReason", degradeReason);
        }
        return summary;
    }

    protected Map<String, Object> buildCacheSummary(boolean cacheHit,
                                                  String cacheGovernanceStatus,
                                                  String cacheGovernanceEvidence) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("cacheHit", Boolean.valueOf(cacheHit));
        summary.put("cacheGovernanceStatus", cacheGovernanceStatus);
        summary.put("cacheGovernanceEvidence", cacheGovernanceEvidence);
        return summary;
    }

    protected Map<String, Object> buildLightweightParseSummary(String sqlText,
                                                             QueryExecutionStatus status,
                                                             QueryErrorDetailVO errorDetail) {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        String normalized = sqlText == null ? "" : sqlText.trim();
        ReadonlyQueryAssessment assessment = ReadonlyQueryGuard.assess(normalized);
        String sqlType = resolveSqlType(normalized);
        List<String> riskTags = detectRiskTags(normalized);
        List<String> rewriteCandidates = detectRewriteCandidates(riskTags, buildQueryDateSummary(normalized));
        List<String> issueCodes = new ArrayList<String>();
        if (!assessment.isReadonly()) {
            issueCodes.add("NON_READONLY_STATEMENT");
        }
        if (errorDetail != null && errorDetail.getCode() == ErrorCodeConstants.QUERY_EXECUTION_RISK_REJECTED
            && !issueCodes.contains("NON_READONLY_STATEMENT")) {
            issueCodes.add("NON_READONLY_STATEMENT");
        }
        summary.put("syntaxStatus", issueCodes.isEmpty() ? "VALID" : "INVALID");
        summary.put("sqlType", sqlType);
        summary.put("readonly", Boolean.valueOf(assessment.isReadonly()));
        summary.put("complexityLevel", resolveComplexityLevel(riskTags));
        summary.put("riskTags", riskTags);
        summary.put("rewriteCandidates", rewriteCandidates);
        summary.put("issueCodes", issueCodes);
        summary.put("issueCount", Integer.valueOf(issueCodes.size()));
        summary.put("resultStatus", status == null ? null : status.name());
        return summary;
    }

    protected List<String> detectQueryDateFields(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        String lower = sqlText.toLowerCase(Locale.ROOT);
        List<String> hits = new ArrayList<String>();
        if (lower.contains("query_date")) {
            hits.add("query_date");
        }
        if (lower.contains("biz_date")) {
            hits.add("biz_date");
        }
        if (lower.contains(" dt ") || lower.contains(".dt") || lower.contains("dt=")) {
            hits.add("dt");
        }
        if (hits.isEmpty() && (lower.contains(" date ") || lower.contains(".date") || lower.contains("date="))) {
            hits.add("date");
        }
        return hits;
    }

    protected LocalDate tryParseDate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        try {
            return LocalDate.parse(candidate, ISO_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    protected int countMatches(Pattern pattern, String sqlText) {
        if (pattern == null || !StringUtils.hasText(sqlText)) {
            return 0;
        }
        int count = 0;
        Matcher matcher = pattern.matcher(sqlText);
        while (matcher.find()) {
            count += 1;
        }
        return count;
    }

    protected String sanitizeObjectReference(String rawReference) {
        if (!StringUtils.hasText(rawReference)) {
            return null;
        }
        String sanitized = rawReference.trim();
        while (sanitized.endsWith(",") || sanitized.endsWith(")") || sanitized.endsWith(";")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1).trim();
        }
        return sanitized;
    }

    protected LogicalObjectSurface toLogicalObjectSurface(String objectReference) {
        LogicalObjectType objectType = resolveLogicalObjectType(objectReference);
        String[] parts = objectReference.split("\\.");
        String objectName = parts.length == 0 ? objectReference : parts[parts.length - 1];
        LogicalObjectSurface surface = new LogicalObjectSurface();
        surface.setObjectType(objectType.name());
        surface.setObjectName(objectName);
        surface.setObjectKey(LogicalObjectRef.buildObjectKey(objectType, objectName));
        if (parts.length >= 3) {
            surface.setCatalogName(parts[0]);
            surface.setSchemaName(parts[1]);
        } else if (parts.length == 2) {
            surface.setSchemaName(parts[0]);
        }
        surface.setMatchSource("SQL_TOKEN");
        surface.setResolved(Boolean.TRUE);
        surface.setMappedPhysicalTargets(Collections.<String>emptyList());
        return surface;
    }

    protected LogicalObjectType resolveLogicalObjectType(String objectReference) {
        String lower = objectReference == null ? "" : objectReference.toLowerCase(Locale.ROOT);
        if (lower.startsWith("business_view")
            || lower.contains(".business_view.")
            || lower.endsWith("_logic")
            || lower.contains("customer_360")) {
            return LogicalObjectType.BUSINESS_VIEW;
        }
        if (lower.contains("vw_") || lower.endsWith("_view") || lower.contains(".view.")) {
            return LogicalObjectType.DB_VIEW;
        }
        return LogicalObjectType.TABLE;
    }

    protected String resolveSqlType(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "UNKNOWN";
        }
        String upper = stripLeadingComments(sqlText).toUpperCase(Locale.ROOT);
        if (upper.startsWith("EXPLAIN")) {
            return "EXPLAIN";
        }
        if (upper.startsWith("WITH")) {
            return "WITH";
        }
        if (upper.startsWith("SELECT")) {
            return "SELECT";
        }
        if (upper.startsWith("INSERT")) {
            return "INSERT";
        }
        if (upper.startsWith("UPDATE")) {
            return "UPDATE";
        }
        if (upper.startsWith("DELETE")) {
            return "DELETE";
        }
        return "UNKNOWN";
    }

    protected String stripLeadingComments(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "";
        }
        String[] lines = sqlText.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder builder = new StringBuilder();
        boolean copying = false;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (!copying && trimmed.startsWith("--")) {
                continue;
            }
            copying = true;
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }
        return builder.toString().trim();
    }

    protected List<String> detectRiskTags(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        String lower = sqlText.toLowerCase(Locale.ROOT);
        List<String> riskTags = new ArrayList<String>();
        if (lower.contains("select *")) {
            riskTags.add("SELECT_STAR");
        }
        if (lower.contains(" join ")) {
            riskTags.add("JOIN");
        }
        if (lower.contains(" group by ")) {
            riskTags.add("AGGREGATION");
        }
        if (lower.contains(" over ")) {
            riskTags.add("WINDOW");
        }
        if (lower.contains(" limit ")) {
            riskTags.add("LIMIT");
        }
        return riskTags;
    }

    protected List<String> detectRewriteCandidates(List<String> riskTags, Map<String, Object> queryDateSummary) {
        List<String> rewriteCandidates = new ArrayList<String>();
        if (riskTags.contains("SELECT_STAR")) {
            rewriteCandidates.add("NARROW_SELECT_COLUMNS");
        }
        if (riskTags.contains("JOIN")) {
            rewriteCandidates.add("VALIDATE_JOIN_FILTERS");
        }
        Object queryDateStatus = queryDateSummary == null ? null : queryDateSummary.get("queryDateStatus");
        if ("PARTIAL".equals(queryDateStatus)) {
            rewriteCandidates.add("RESOLVE_QUERY_DATE_BINDINGS");
        }
        return rewriteCandidates;
    }

    protected String resolveComplexityLevel(List<String> riskTags) {
        if (riskTags == null || riskTags.isEmpty()) {
            return "SIMPLE";
        }
        if (riskTags.size() >= 3 || riskTags.contains("WINDOW")) {
            return "COMPLEX";
        }
        return "MODERATE";
    }

    protected ActivatedAccelerationBinding resolveActivatedAccelerationBinding(QueryExecuteRequest request,
                                                                           String sqlFingerprint,
                                                                           DataSourceTypeEnum primaryEngine) {
        if (request.getAccelerationPreference() != com.company.queryexecution.domain.query.AccelerationPreference.PREFER_ACCELERATED) {
            return null;
        }
        if (primaryEngine == null) {
            return null;
        }
        return queryExecutionAccelerationRuntimeService.resolveActiveBinding(
            request.getTenantId(),
            sqlFingerprint,
            primaryEngine.name()
        );
    }

    protected QueryExecuteRequest normalizeAccelerationRequest(QueryExecuteRequest request,
                                                            boolean accelerationAllowed,
                                                            DataSourceTypeEnum primaryEngine,
                                                            RuntimeRewriteResolution runtimeRewriteResolution) {
        QueryExecuteRequest normalized = new QueryExecuteRequest();
        String actualSql = runtimeRewriteResolution == null
            ? request.getSqlText()
            : runtimeRewriteResolution.getActualSql();
        normalized.setSqlText(actualSql);
        normalized.setTenantId(request.getTenantId());
        normalized.setDatasourceType(primaryEngine == null ? request.getDatasourceType() : primaryEngine);
        normalized.setDatasourceCode(firstText(
            request.getDatasourceCode(),
            runtimeRewriteResolution == null ? null : runtimeRewriteResolution.getDatasourceCode()
        ));
        normalized.setQueryContext(request.getQueryContext());
        normalized.setFaultToleranceStrategy(request.getFaultToleranceStrategy());
        normalized.setAccelerationPreference(accelerationAllowed
            ? request.getAccelerationPreference()
            : com.company.queryexecution.domain.query.AccelerationPreference.NONE);
        return normalized;
    }

    protected String firstText(String... values) {
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

    protected String stableHash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (parts != null) {
                for (String part : parts) {
                    digest.update((part == null ? "" : part).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte) '|');
                }
            }
            byte[] hash = digest.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12 && i < hash.length; i += 1) {
                builder.append(String.format("%02x", Integer.valueOf(hash[i] & 0xff)));
            }
            return builder.toString();
        } catch (Exception ex) {
            return String.valueOf(Math.abs(Arrays.hashCode(parts)));
        }
    }

    protected static final class RuntimeRewriteResolution {
        protected final String originalSql;
        protected final String originalSqlFingerprint;
        protected final String actualSql;
        protected final String actualSqlFingerprint;
        protected final boolean rewriteApplied;
        protected final String runtimeStatus;
        protected final String rewriteRecordId;
        protected final String runtimeBindingId;
        protected final Long ruleVersion;
        protected final String runtimeRuleVersion;
        protected final String datasourceCode;
        protected final String rewriteActivationStatusSnapshot;
        protected final String rewriteFallbackReason;

        protected RuntimeRewriteResolution(String originalSql,
                                         String originalSqlFingerprint,
                                         String actualSql,
                                         String actualSqlFingerprint,
                                         boolean rewriteApplied,
                                         String runtimeStatus,
                                         String rewriteRecordId,
                                         String runtimeBindingId,
                                         Long ruleVersion,
                                         String runtimeRuleVersion,
                                         String datasourceCode,
                                         String rewriteActivationStatusSnapshot,
                                         String rewriteFallbackReason) {
            this.originalSql = originalSql;
            this.originalSqlFingerprint = originalSqlFingerprint;
            this.actualSql = actualSql;
            this.actualSqlFingerprint = actualSqlFingerprint;
            this.rewriteApplied = rewriteApplied;
            this.runtimeStatus = runtimeStatus;
            this.rewriteRecordId = rewriteRecordId;
            this.runtimeBindingId = runtimeBindingId;
            this.ruleVersion = ruleVersion;
            this.runtimeRuleVersion = runtimeRuleVersion;
            this.datasourceCode = trimToNull(datasourceCode);
            this.rewriteActivationStatusSnapshot = rewriteActivationStatusSnapshot;
            this.rewriteFallbackReason = rewriteFallbackReason;
        }

        static RuntimeRewriteResolution noRewrite(String originalSql, String originalSqlFingerprint) {
            return noRewrite(originalSql, originalSql, originalSqlFingerprint);
        }

        static RuntimeRewriteResolution noRewrite(String originalSql, String actualSql, String originalSqlFingerprint) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                actualSql,
                SqlFingerprintUtils.fingerprint(actualSql),
                false,
                "NOT_LOOKED_UP",
                null,
                null,
                null,
                null,
                null,
                "INACTIVE",
                null
            );
        }

        static RuntimeRewriteResolution inactive(String originalSql,
                                                 String actualSql,
                                                 String originalSqlFingerprint,
                                                 String runtimeStatus,
                                                 String summary) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                actualSql,
                SqlFingerprintUtils.fingerprint(actualSql),
                false,
                StringUtils.hasText(runtimeStatus) ? runtimeStatus : "MISSING",
                null,
                null,
                null,
                null,
                null,
                toRewriteActivationStatusSnapshot(runtimeStatus, null),
                summary
            );
        }

        static RuntimeRewriteResolution applied(String originalSql,
                                                String originalSqlFingerprint,
                                                String recommendedSql,
                                                RuntimeRewriteBindingResponse response) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                recommendedSql,
                SqlFingerprintUtils.fingerprint(recommendedSql),
                true,
                response.getStatus(),
                response.getRewriteRecordId(),
                response.getRuntimeBindingId(),
                response.getRuleVersion(),
                response.getRuntimeRuleVersion(),
                response.getDatasourceCode(),
                toRewriteActivationStatusSnapshot(response.getStatus(), response.getRewriteRecordId()),
                null
            );
        }

        static RuntimeRewriteResolution fallback(String originalSql,
                                                 String originalSqlFingerprint,
                                                 RuntimeRewriteBindingResponse response,
                                                 String reason) {
            return fallback(originalSql, originalSql, originalSqlFingerprint, response, reason);
        }

        static RuntimeRewriteResolution fallback(String originalSql,
                                                 String actualSql,
                                                 String originalSqlFingerprint,
                                                 RuntimeRewriteBindingResponse response,
                                                 String reason) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                actualSql,
                SqlFingerprintUtils.fingerprint(actualSql),
                false,
                response == null ? "LOOKUP_FAILED" : response.getStatus(),
                response == null ? null : response.getRewriteRecordId(),
                response == null ? null : response.getRuntimeBindingId(),
                response == null ? null : response.getRuleVersion(),
                response == null ? null : response.getRuntimeRuleVersion(),
                null,
                response == null ? "UNKNOWN" : toRewriteActivationStatusSnapshot(response.getStatus(), response.getRewriteRecordId()),
                reason
            );
        }

        RuntimeRewriteResolution fallbackAfterRewriteExecutionFailure(String fallbackActualSql, String reason) {
            return new RuntimeRewriteResolution(
                originalSql,
                originalSqlFingerprint,
                fallbackActualSql,
                SqlFingerprintUtils.fingerprint(fallbackActualSql),
                false,
                runtimeStatus,
                rewriteRecordId,
                runtimeBindingId,
                ruleVersion,
                runtimeRuleVersion,
                datasourceCode,
                rewriteActivationStatusSnapshot,
                reason
            );
        }

        protected static String toRewriteActivationStatusSnapshot(String runtimeStatus, String rewriteRecordId) {
            if ("ACTIVE".equals(runtimeStatus)) {
                return "ACTIVE";
            }
            if ("PAUSED".equals(runtimeStatus)) {
                return runtimeStatus;
            }
            if (StringUtils.hasText(rewriteRecordId)) {
                return "UNKNOWN";
            }
            return "INACTIVE";
        }

        String getOriginalSql() { return originalSql; }
        String getOriginalSqlFingerprint() { return originalSqlFingerprint; }
        String getActualSql() { return actualSql; }
        String getActualSqlFingerprint() { return actualSqlFingerprint; }
        boolean isRewriteApplied() { return rewriteApplied; }
        String getRuntimeStatus() { return runtimeStatus; }
        String getRewriteRecordId() { return rewriteRecordId; }
        String getRuntimeBindingId() { return runtimeBindingId; }
        Long getRuleVersion() { return ruleVersion; }
        String getRuntimeRuleVersion() { return runtimeRuleVersion; }
        String getDatasourceCode() { return datasourceCode; }
        String getRewriteActivationStatusSnapshot() { return rewriteActivationStatusSnapshot; }
        String getRewriteFallbackReason() { return rewriteFallbackReason; }

        protected static String trimToNull(String value) {
            return !StringUtils.hasText(value) ? null : value.trim();
        }
    }
}
