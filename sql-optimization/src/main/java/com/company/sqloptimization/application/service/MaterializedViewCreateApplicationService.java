package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import com.company.sqloptimization.application.controller.dto.MaterializedViewCreateRequest;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.domain.rewrite.repository.SqlRewriteRecordRepository;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionMaterializedViewClient;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MaterializedViewCreateApplicationService {

    private static final String RULE_PRECOMPUTE_MV = "PRECOMPUTE_MV";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";

    private final AccelerationRecommendationRepository recommendationRepository;
    private final SqlRewriteRecordRepository sqlRewriteRecordRepository;
    private final QueryExecutionMaterializedViewClient materializedViewClient;

    public MaterializedViewCreateApplicationService(AccelerationRecommendationRepository recommendationRepository,
                                                    SqlRewriteRecordRepository sqlRewriteRecordRepository,
                                                    QueryExecutionMaterializedViewClient materializedViewClient) {
        this.recommendationRepository = recommendationRepository;
        this.sqlRewriteRecordRepository = sqlRewriteRecordRepository;
        this.materializedViewClient = materializedViewClient;
    }

    public QueryExecutionMaterializedViewCreateResponse create(String recommendationId,
                                                              MaterializedViewCreateRequest request) {
        String tenantId = requireAuthorizedTenant(request == null ? null : request.getTenantId());
        AccelerationRecommendation recommendation = requireRecommendation(recommendationId);
        if (!tenantId.equals(recommendation.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权创建该推荐的物化视图");
        }
        Map<String, Object> artifact = requireCreatableArtifact(recommendation);
        SqlRewriteRecord rewriteRecord = resolveRewriteRecord(request, tenantId, recommendation.getRecommendationId());
        QueryExecutionMaterializedViewCreateResponse response =
            materializedViewClient.create(buildQueryExecutionRequest(recommendation, artifact, rewriteRecord, request));
        if (response == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_ACCELERATION_PLAN_APPLY_FAILURE,
                HttpStatus.SERVICE_UNAVAILABLE,
                "query-execution 未返回物化视图创建结果"
            );
        }
        if (rewriteRecord != null) {
            sqlRewriteRecordRepository.saveRecord(
                rewriteRecord.withTraceRefs(
                    MaterializedViewCreateEvidenceBuilder.withEvidence(
                        rewriteRecord.getTraceRefs(),
                        response,
                        artifact,
                        request
                    ),
                    Instant.now()
                )
            );
        }
        return response;
    }

    private AccelerationRecommendation requireRecommendation(String recommendationId) {
        String normalizedRecommendationId = requireText(recommendationId, "recommendationId");
        AccelerationRecommendation recommendation =
            recommendationRepository.findByRecommendationId(normalizedRecommendationId);
        if (recommendation == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "推荐不存在：" + normalizedRecommendationId
            );
        }
        return recommendation;
    }

    private Map<String, Object> requireCreatableArtifact(AccelerationRecommendation recommendation) {
        Map<String, Object> artifact =
            AccelerationArtifactSnapshotSanitizer.sanitize(recommendation.getAccelerationArtifact());
        if (artifact == null || artifact.isEmpty()) {
            throw invalidArgument("accelerationArtifact", "推荐缺少可创建的 PRECOMPUTE_MV 产物快照");
        }
        artifact = withRecommendationTargets(artifact, recommendation);
        requireEquals(RULE_PRECOMPUTE_MV, textValue(artifact.get("rule")), "accelerationArtifact.rule");
        String artifactStatus = textValue(artifact.get("artifactStatus"));
        if (!STATUS_GENERATED.equals(artifactStatus) && !STATUS_REVIEW_REQUIRED.equals(artifactStatus)) {
            throw invalidArgument("accelerationArtifact.artifactStatus", "只允许 GENERATED 或 REVIEW_REQUIRED 产物创建真实 MV");
        }
        if (hasBlockingReasons(artifact.get("blockingReasons"))) {
            throw invalidArgument("accelerationArtifact.blockingReasons", "存在阻断原因的 MV 产物不能创建真实 MV");
        }
        requireText(textValue(artifact.get("ddlSql")), "accelerationArtifact.ddlSql");
        requireText(textValue(artifact.get("refreshSql")), "accelerationArtifact.refreshSql");
        requireText(textValue(artifact.get("mvName")), "accelerationArtifact.mvName");
        requireText(textValue(artifact.get("targetDatasource")), "accelerationArtifact.targetDatasource");
        requireText(textValue(artifact.get("targetEngine")), "accelerationArtifact.targetEngine");
        return artifact;
    }

    private Map<String, Object> withRecommendationTargets(Map<String, Object> artifact,
                                                          AccelerationRecommendation recommendation) {
        Map<String, Object> result = new LinkedHashMap<String, Object>(artifact);
        if (!StringUtils.hasText(textValue(result.get("targetDatasource")))) {
            result.put("targetDatasource", recommendation.getTargetDatasource());
        }
        if (!StringUtils.hasText(textValue(result.get("targetEngine")))
            || "AUTO".equalsIgnoreCase(textValue(result.get("targetEngine")))) {
            result.put("targetEngine", recommendation.getTargetEngine());
        }
        return result;
    }

    private SqlRewriteRecord resolveRewriteRecord(MaterializedViewCreateRequest request,
                                                  String tenantId,
                                                  String recommendationId) {
        String rewriteRecordId = trimToNull(request == null ? null : request.getRewriteRecordId());
        if (rewriteRecordId == null) {
            return null;
        }
        SqlRewriteRecord rewriteRecord = sqlRewriteRecordRepository.findRecordById(rewriteRecordId);
        if (rewriteRecord == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "SQL 改写记录不存在：" + rewriteRecordId
            );
        }
        if (!tenantId.equals(rewriteRecord.getTenantId())) {
            throw new AccessDeniedException("当前认证租户无权使用该改写记录创建物化视图");
        }
        if (!recommendationId.equals(rewriteRecord.getRecommendationId())) {
            throw invalidArgument("rewriteRecordId", "rewriteRecordId 不属于当前推荐");
        }
        return rewriteRecord;
    }

    private QueryExecutionMaterializedViewCreateRequest buildQueryExecutionRequest(
        AccelerationRecommendation recommendation,
        Map<String, Object> artifact,
        SqlRewriteRecord rewriteRecord,
        MaterializedViewCreateRequest request
    ) {
        QueryExecutionMaterializedViewCreateRequest createRequest =
            new QueryExecutionMaterializedViewCreateRequest();
        createRequest.setTenantId(recommendation.getTenantId());
        createRequest.setRecommendationId(recommendation.getRecommendationId());
        createRequest.setRewriteRecordId(rewriteRecord == null ? null : rewriteRecord.getRewriteRecordId());
        createRequest.setMvName(textValue(artifact.get("mvName")));
        createRequest.setTargetEngine(textValue(artifact.get("targetEngine")));
        createRequest.setTargetDatasource(textValue(artifact.get("targetDatasource")));
        createRequest.setDdlSql(textValue(artifact.get("ddlSql")));
        createRequest.setRefreshSql(textValue(artifact.get("refreshSql")));
        createRequest.setReason(trimToNull(request == null ? null : request.getReason()));
        return createRequest;
    }

    private boolean hasBlockingReasons(Object value) {
        if (value instanceof List) {
            return !((List<?>) value).isEmpty();
        }
        return value != null && StringUtils.hasText(String.valueOf(value));
    }

    private void requireEquals(String expected, String actual, String field) {
        if (expected.equals(actual)) {
            return;
        }
        throw invalidArgument(field, field + " 必须为 " + expected);
    }

    private String requireAuthorizedTenant(String requestTenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId"
            );
        }
        if (StringUtils.hasText(requestTenantId) && !contextTenantId.equals(requestTenantId.trim())) {
            throw new AccessDeniedException("请求 tenantId 与已认证租户上下文不一致");
        }
        return contextTenantId;
    }

    private String requireText(String value, String field) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw invalidArgument(field, field + " 为必填项");
        }
        return trimmed;
    }

    private BizException invalidArgument(String field, String message) {
        return new BizException(
            ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
            HttpStatus.BAD_REQUEST,
            field + ": " + message
        );
    }

    private String textValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
