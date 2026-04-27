package com.company.sqlforge.common.jdbcagent;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.config.ServiceCodeConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.openaccess.OpenAccessHttpClientProperties;
import com.company.sqlforge.common.openaccess.OpenAccessRequestContext;
import com.company.sqlforge.common.openaccess.SqlForgeAccessAuditClient;
import com.company.sqlforge.common.openaccess.SqlForgeQueryExecutionClient;
import com.company.sqlforge.common.openaccess.SqlForgeQueryRequest;
import com.company.sqlforge.common.openaccess.SqlForgeQueryResponse;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

public class SqlForgeJdbcAgent {

    private static final String RESOURCE_TYPE = "JDBC_SQL";
    private static final String OBSERVE_OPERATION = "JDBC_AGENT_OBSERVE";
    private static final String GOVERNED_OPERATION = "JDBC_AGENT_GOVERNED_EXECUTE";
    private static final String REWRITE_OPERATION = "JDBC_AGENT_REWRITE_EXECUTE";
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JdbcAgentProperties properties;
    private final SqlForgeQueryExecutionClient queryExecutionClient;
    private final SqlForgeAccessAuditClient accessAuditClient;
    private final JdbcAgentRewriteRuleProvider rewriteRuleProvider;
    private final ExecutorService lightParseExecutor;

    public SqlForgeJdbcAgent(JdbcAgentProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this(properties, restTemplateBuilder, new RedisJdbcAgentRewriteRuleProvider());
    }

    public SqlForgeJdbcAgent(JdbcAgentProperties properties,
                             RestTemplateBuilder restTemplateBuilder,
                             JdbcAgentRewriteRuleProvider rewriteRuleProvider) {
        this(
            properties,
            new SqlForgeQueryExecutionClient(restTemplateBuilder, buildHttpClientProperties(properties)),
            new SqlForgeAccessAuditClient(restTemplateBuilder, buildHttpClientProperties(properties)),
            rewriteRuleProvider
        );
    }

    public SqlForgeJdbcAgent(JdbcAgentProperties properties,
                             SqlForgeQueryExecutionClient queryExecutionClient,
                             SqlForgeAccessAuditClient accessAuditClient,
                             JdbcAgentRewriteRuleProvider rewriteRuleProvider) {
        this.properties = properties;
        this.queryExecutionClient = queryExecutionClient;
        this.accessAuditClient = accessAuditClient;
        this.rewriteRuleProvider = rewriteRuleProvider == null ? new NoopJdbcAgentRewriteRuleProvider() : rewriteRuleProvider;
        this.lightParseExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "sqlforge-jdbc-agent-light-parse");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private static OpenAccessHttpClientProperties buildHttpClientProperties(JdbcAgentProperties properties) {
        OpenAccessHttpClientProperties httpClientProperties = new OpenAccessHttpClientProperties();
        httpClientProperties.setBaseUrl(properties == null ? null : properties.getApiBaseUrl());
        return httpClientProperties;
    }

    public <T> JdbcAgentExecutionResult<T> execute(OpenAccessRequestContext requestContext,
                                                   JdbcAgentSqlRequest request,
                                                   JdbcAgentDirectExecutor<T> directExecutor) {
        JdbcAgentExecutionMetadata metadata = new JdbcAgentExecutionMetadata();
        JdbcAgentObservation observation = observe(request);
        metadata.setObservation(observation);
        switch (properties.getAgentMode()) {
            case GOVERNED_EXECUTE:
                return executeGoverned(requestContext, request, directExecutor, metadata, observation);
            case LOCAL_REWRITE_DIRECT_JDBC:
                return executeLocalRewrite(requestContext, request, directExecutor, metadata, observation);
            case OBSERVE:
            default:
                return executeObserve(requestContext, request, directExecutor, metadata, observation);
        }
    }

    public JdbcAgentObservation observe(JdbcAgentSqlRequest request) {
        String originalSql = request == null ? null : request.getSqlText();
        String boundSqlText = StringUtils.hasText(request == null ? null : request.getBoundSqlText())
            ? request.getBoundSqlText()
            : originalSql;
        String templateSql = StringUtils.hasText(request == null ? null : request.getTemplateSql())
            ? request.getTemplateSql()
            : originalSql;
        Map<String, String> commentContext = JdbcAgentSqlCommentParser.parseLeadingComments(originalSql);
        JdbcAgentQueryDateSummary queryDateSummary = extractQueryDateSummary(boundSqlText);
        int parameterCount = request == null || request.getParameterSnapshot() == null ? 0 : request.getParameterSnapshot().size();
        return new JdbcAgentObservation(
            originalSql,
            templateSql,
            boundSqlText,
            SqlFingerprintUtils.fingerprint(boundSqlText),
            commentContext,
            queryDateSummary,
            StringUtils.hasText(templateSql) && StringUtils.hasText(boundSqlText) && !templateSql.equals(boundSqlText),
            parameterCount
        );
    }

    private <T> JdbcAgentExecutionResult<T> executeObserve(OpenAccessRequestContext requestContext,
                                                           JdbcAgentSqlRequest request,
                                                           JdbcAgentDirectExecutor<T> directExecutor,
                                                           JdbcAgentExecutionMetadata metadata,
                                                           JdbcAgentObservation observation) {
        metadata.setEffectiveMode(JdbcAgentMode.OBSERVE.name());
        try {
            JdbcAgentDirectResult<T> directResult = runDirectExecution(
                request,
                directExecutor,
                observation,
                resolveExecutableSql(request),
                null,
                false,
                "OBSERVE_ONLY"
            );
            reportAuditSafely(requestContext, OBSERVE_OPERATION, observation, metadata, directResult, null);
            return new JdbcAgentExecutionResult<T>(directResult, null, metadata);
        } catch (RuntimeException ex) {
            reportAuditSafely(requestContext, OBSERVE_OPERATION, observation, metadata, null, null);
            throw ex;
        }
    }

    private <T> JdbcAgentExecutionResult<T> executeGoverned(OpenAccessRequestContext requestContext,
                                                            JdbcAgentSqlRequest request,
                                                            JdbcAgentDirectExecutor<T> directExecutor,
                                                            JdbcAgentExecutionMetadata metadata,
                                                            JdbcAgentObservation observation) {
        metadata.setEffectiveMode(JdbcAgentMode.GOVERNED_EXECUTE.name());
        try {
            SqlForgeQueryResponse response = queryExecutionClient.execute(
                requestContext.withAccessChannel(AccessChannel.JDBC_AGENT),
                buildGovernedRequest(request, requestContext),
                AccessChannel.JDBC_AGENT
            );
            reportAuditSafely(requestContext, GOVERNED_OPERATION, observation, metadata, null, response);
            return new JdbcAgentExecutionResult<T>(null, response, metadata);
        } catch (RuntimeException ex) {
            metadata.setPlatformFailureReason(ex.getMessage());
            if (properties.getFallbackStrategy() != JdbcAgentFallbackStrategy.DIRECT_JDBC) {
                reportAuditSafely(requestContext, GOVERNED_OPERATION, observation, metadata, null, null);
                throw ex;
            }
            metadata.setFallbackApplied(true);
            try {
                JdbcAgentDirectResult<T> directResult = runDirectExecution(
                    request,
                    directExecutor,
                    observation,
                    resolveExecutableSql(request),
                    null,
                    false,
                    "PLATFORM_FAILURE_DIRECT_FALLBACK"
                );
                reportAuditSafely(requestContext, GOVERNED_OPERATION, observation, metadata, directResult, null);
                return new JdbcAgentExecutionResult<T>(directResult, null, metadata);
            } catch (RuntimeException fallbackFailure) {
                reportAuditSafely(requestContext, GOVERNED_OPERATION, observation, metadata, null, null);
                throw fallbackFailure;
            }
        }
    }

    private <T> JdbcAgentExecutionResult<T> executeLocalRewrite(OpenAccessRequestContext requestContext,
                                                                JdbcAgentSqlRequest request,
                                                                JdbcAgentDirectExecutor<T> directExecutor,
                                                                JdbcAgentExecutionMetadata metadata,
                                                                JdbcAgentObservation observation) {
        metadata.setEffectiveMode(JdbcAgentMode.LOCAL_REWRITE_DIRECT_JDBC.name());
        JdbcAgentRewriteDecision rewriteDecision = resolveRewriteDecision(observation, metadata);
        String effectiveSql = rewriteDecision.isApplied() && StringUtils.hasText(rewriteDecision.getRewrittenSql())
            ? rewriteDecision.getRewrittenSql()
            : resolveExecutableSql(request);
        JdbcAgentDirectExecution execution = new JdbcAgentDirectExecution(
            request,
            observation,
            effectiveSql,
            rewriteDecision.getRouteHint(),
            rewriteDecision.isApplied(),
            rewriteDecision.getEvidence()
        );
        try {
            JdbcAgentDirectResult<T> directResult = requireDirectExecutor(directExecutor).execute(execution);
            reportAuditSafely(requestContext, REWRITE_OPERATION, observation, metadata, directResult, null);
            return new JdbcAgentExecutionResult<T>(directResult, null, metadata);
        } catch (Exception ex) {
            if (rewriteDecision.isApplied() || "LIGHT_PARSE_TIMEOUT_BYPASS".equals(rewriteDecision.getEvidence())
                || "RULE_PROVIDER_FAILURE_BYPASS".equals(rewriteDecision.getEvidence())) {
                if (properties.getFallbackStrategy() == JdbcAgentFallbackStrategy.ORIGINAL_SQL) {
                    metadata.setFallbackApplied(true);
                    JdbcAgentDirectResult<T> directResult = runDirectExecution(
                        request,
                        directExecutor,
                        observation,
                        resolveExecutableSql(request),
                        null,
                        false,
                        "REWRITE_FAILURE_ORIGINAL_SQL_FALLBACK"
                    );
                    reportAuditSafely(requestContext, REWRITE_OPERATION, observation, metadata, directResult, null);
                    return new JdbcAgentExecutionResult<T>(directResult, null, metadata);
                }
            }
            reportAuditSafely(requestContext, REWRITE_OPERATION, observation, metadata, null, null);
            throw wrapExecutionFailure(ex);
        }
    }

    private JdbcAgentRewriteDecision resolveRewriteDecision(final JdbcAgentObservation observation,
                                                            JdbcAgentExecutionMetadata metadata) {
        if (!properties.isRewriteEnabled() && !properties.isRouteEnabled()) {
            metadata.setLightParseStatus("BYPASSED_RULES_DISABLED");
            metadata.setRewriteEvidence("RULES_DISABLED");
            return JdbcAgentRewriteDecision.passthrough("RULES_DISABLED");
        }
        long timeoutMs = properties.getLightParseTimeoutMs();
        if (timeoutMs <= 0L) {
            timeoutMs = 20L;
        }
        Future<JdbcAgentRewriteDecision> future = lightParseExecutor.submit(new Callable<JdbcAgentRewriteDecision>() {
            @Override
            public JdbcAgentRewriteDecision call() throws Exception {
                return rewriteRuleProvider.resolve(observation, properties);
            }
        });
        try {
            JdbcAgentRewriteDecision decision = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            metadata.setLightParseStatus("COMPLETED");
            metadata.setRewriteApplied(decision.isApplied());
            metadata.setRouteHint(decision.getRouteHint());
            metadata.setRewriteEvidence(decision.getEvidence());
            return decision;
        } catch (TimeoutException ex) {
            future.cancel(true);
            metadata.setLightParseStatus("BYPASSED_TIMEOUT");
            metadata.setRewriteEvidence("LIGHT_PARSE_TIMEOUT_BYPASS");
            if (properties.getFallbackStrategy() == JdbcAgentFallbackStrategy.FAIL_CLOSED) {
                throw new BizException(
                    ErrorCodeConstants.SYSTEM_OPEN_ACCESS_ROUTE_INVALID,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "JDBC Agent lightweight parse timed out"
                );
            }
            return JdbcAgentRewriteDecision.passthrough("LIGHT_PARSE_TIMEOUT_BYPASS");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BizException(
                ErrorCodeConstants.SYSTEM_OPEN_ACCESS_ROUTE_INVALID,
                HttpStatus.SERVICE_UNAVAILABLE,
                "JDBC Agent lightweight parse was interrupted",
                ex
            );
        } catch (ExecutionException ex) {
            metadata.setLightParseStatus("BYPASSED_PROVIDER_FAILURE");
            metadata.setRewriteEvidence("RULE_PROVIDER_FAILURE_BYPASS");
            if (properties.getFallbackStrategy() == JdbcAgentFallbackStrategy.FAIL_CLOSED) {
                throw new BizException(
                    ErrorCodeConstants.SYSTEM_OPEN_ACCESS_ROUTE_INVALID,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "JDBC Agent rewrite rule provider failed",
                    ex.getCause() == null ? ex : ex.getCause()
                );
            }
            return JdbcAgentRewriteDecision.passthrough("RULE_PROVIDER_FAILURE_BYPASS");
        } catch (CancellationException ex) {
            metadata.setLightParseStatus("BYPASSED_CANCELLED");
            metadata.setRewriteEvidence("LIGHT_PARSE_CANCELLED");
            return JdbcAgentRewriteDecision.passthrough("LIGHT_PARSE_CANCELLED");
        }
    }

    private <T> JdbcAgentDirectResult<T> runDirectExecution(JdbcAgentSqlRequest request,
                                                            JdbcAgentDirectExecutor<T> directExecutor,
                                                            JdbcAgentObservation observation,
                                                            String sqlText,
                                                            String routeHint,
                                                            boolean rewritten,
                                                            String rewriteEvidence) {
        try {
            return requireDirectExecutor(directExecutor).execute(
                new JdbcAgentDirectExecution(
                    request,
                    observation,
                    sqlText,
                    routeHint,
                    rewritten,
                    rewriteEvidence
                )
            );
        } catch (Exception ex) {
            throw wrapExecutionFailure(ex);
        }
    }

    private RuntimeException wrapExecutionFailure(Exception ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new IllegalStateException("JDBC Agent direct execution failed", ex);
    }

    private <T> JdbcAgentDirectExecutor<T> requireDirectExecutor(JdbcAgentDirectExecutor<T> directExecutor) {
        if (directExecutor == null) {
            throw new IllegalArgumentException("JDBC Agent direct executor is required for the selected mode");
        }
        return directExecutor;
    }

    private SqlForgeQueryRequest buildGovernedRequest(JdbcAgentSqlRequest request, OpenAccessRequestContext requestContext) {
        SqlForgeQueryRequest governedRequest = new SqlForgeQueryRequest();
        governedRequest.setSqlText(resolveExecutableSql(request));
        governedRequest.setTenantId(StringUtils.hasText(request.getTenantId()) ? request.getTenantId() : requestContext.getTenantId());
        governedRequest.setDatasourceType(request.getDatasourceType());
        governedRequest.setQueryContext(request.getQueryContext());
        return governedRequest;
    }

    private void reportAuditSafely(OpenAccessRequestContext requestContext,
                                   String operationCode,
                                   JdbcAgentObservation observation,
                                   JdbcAgentExecutionMetadata metadata,
                                   JdbcAgentDirectResult<?> directResult,
                                   SqlForgeQueryResponse governedResponse) {
        try {
            accessAuditClient.writeAudit(
                requestContext.withAccessChannel(AccessChannel.JDBC_AGENT),
                ServiceCodeConstants.JDBC_AGENT,
                operationCode,
                RESOURCE_TYPE,
                observation.getSqlFingerprint(),
                resolveAuditResultStatus(directResult, governedResponse),
                resolveAuditElapsedMs(directResult, governedResponse),
                buildAuditRequestSummary(observation),
                buildAuditResponseSummary(metadata, directResult, governedResponse)
            );
            metadata.setAuditReported(true);
        } catch (RuntimeException ex) {
            metadata.setAuditReported(false);
            metadata.setAuditFailureReason(ex.getMessage());
        }
    }

    private Map<String, Object> buildAuditRequestSummary(JdbcAgentObservation observation) {
        Map<String, Object> requestSummary = new LinkedHashMap<String, Object>();
        requestSummary.put("sqlFingerprint", observation.getSqlFingerprint());
        requestSummary.put("commentContext", observation.getCommentContext());
        requestSummary.put("queryDateStart", observation.getQueryDateSummary().getQueryDateStart());
        requestSummary.put("queryDateEnd", observation.getQueryDateSummary().getQueryDateEnd());
        requestSummary.put("queryDateStatus", observation.getQueryDateSummary().getQueryDateStatus());
        requestSummary.put("queryDateFields", observation.getQueryDateSummary().getQueryDateFields());
        requestSummary.put("parameterized", Boolean.valueOf(observation.isParameterized()));
        requestSummary.put("parameterCount", Integer.valueOf(observation.getParameterCount()));
        requestSummary.put("agentMode", properties.getAgentMode().name());
        requestSummary.put("routeEnabled", Boolean.valueOf(properties.isRouteEnabled()));
        requestSummary.put("rewriteEnabled", Boolean.valueOf(properties.isRewriteEnabled()));
        requestSummary.put("historyReportEnabled", Boolean.valueOf(properties.isHistoryReportEnabled()));
        return requestSummary;
    }

    private Map<String, Object> buildAuditResponseSummary(JdbcAgentExecutionMetadata metadata,
                                                          JdbcAgentDirectResult<?> directResult,
                                                          SqlForgeQueryResponse governedResponse) {
        Map<String, Object> responseSummary = new LinkedHashMap<String, Object>();
        responseSummary.put("effectiveMode", metadata.getEffectiveMode());
        responseSummary.put("fallbackApplied", Boolean.valueOf(metadata.isFallbackApplied()));
        responseSummary.put("rewriteApplied", Boolean.valueOf(metadata.isRewriteApplied()));
        responseSummary.put("routeHint", metadata.getRouteHint());
        responseSummary.put("rewriteEvidence", metadata.getRewriteEvidence());
        responseSummary.put("lightParseStatus", metadata.getLightParseStatus());
        responseSummary.put("platformFailureReason", metadata.getPlatformFailureReason());
        if (directResult != null) {
            responseSummary.put("resultStatus", directResult.getResultStatus());
            responseSummary.put("rowCount", directResult.getRowCount());
        } else if (governedResponse != null) {
            responseSummary.put("resultStatus", governedResponse.getStatus() == null ? null : governedResponse.getStatus().name());
            responseSummary.put("executionMode", governedResponse.getMetadata() == null ? null : governedResponse.getMetadata().getExecutionMode());
            responseSummary.put("degraded", Boolean.valueOf(governedResponse.isDegraded()));
        } else {
            responseSummary.put("resultStatus", "FAILED");
        }
        return responseSummary;
    }

    private String resolveAuditResultStatus(JdbcAgentDirectResult<?> directResult, SqlForgeQueryResponse governedResponse) {
        if (directResult != null && StringUtils.hasText(directResult.getResultStatus())) {
            return directResult.getResultStatus();
        }
        if (governedResponse != null && governedResponse.getStatus() != null) {
            return governedResponse.getStatus().name();
        }
        return "FAILED";
    }

    private long resolveAuditElapsedMs(JdbcAgentDirectResult<?> directResult, SqlForgeQueryResponse governedResponse) {
        if (governedResponse != null && governedResponse.getMetadata() != null && governedResponse.getMetadata().getElapsedMs() != null) {
            return governedResponse.getMetadata().getElapsedMs().longValue();
        }
        return 0L;
    }

    private String resolveExecutableSql(JdbcAgentSqlRequest request) {
        if (StringUtils.hasText(request.getBoundSqlText())) {
            return request.getBoundSqlText();
        }
        return request.getSqlText();
    }

    private JdbcAgentQueryDateSummary extractQueryDateSummary(String sqlText) {
        JdbcAgentQueryDateSummary summary = new JdbcAgentQueryDateSummary();
        if (!StringUtils.hasText(sqlText)) {
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
        summary.setQueryDateFields(fields);
        if (!dates.isEmpty()) {
            dates.sort(Comparator.naturalOrder());
            summary.setQueryDateStart(dates.get(0).format(ISO_DATE));
            summary.setQueryDateEnd(dates.get(dates.size() - 1).format(ISO_DATE));
            summary.setQueryDateStatus("RESOLVED");
            return summary;
        }
        if (!fields.isEmpty()) {
            summary.setQueryDateStatus("PARTIAL");
        }
        return summary;
    }

    private List<String> detectQueryDateFields(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return Collections.emptyList();
        }
        String lower = sqlText.toLowerCase();
        List<String> candidates = Arrays.asList("query_date", "biz_date", "dt", "date");
        List<String> hits = new ArrayList<String>();
        for (String candidate : candidates) {
            if (lower.contains(candidate) && !hits.contains(candidate)) {
                hits.add(candidate);
            }
        }
        return hits;
    }

    private LocalDate tryParseDate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        try {
            return LocalDate.parse(candidate, ISO_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
