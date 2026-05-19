package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class L2AccelerationArtifactApplicationService {

    private final SqlOptimizationPipelineService pipelineService;

    @Autowired
    public L2AccelerationArtifactApplicationService(SqlOptimizationPipelineService pipelineService) {
        this.pipelineService = pipelineService;
    }

    public Map<String, Object> buildForRecommendation(AccelerationRecommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        SqlOptimizationPipelineService.ParsedSqlProfile profile = parseProfile(recommendation.getSourceSqlText());
        return L2AccelerationArtifactBuilder.buildForRecommendation(
            input(
                recommendation.getSourceSqlText(),
                recommendation.getTargetEngine(),
                recommendation.getTargetDatasource(),
                recommendation.getSqlFingerprint(),
                recommendation.getReportCode(),
                recommendation.getLogicalObjectKey(),
                recommendation.getRuleChain()
            ),
            profile
        );
    }

    public Map<String, Object> buildForProfile(String sourceSqlText,
                                               String targetEngine,
                                               String targetDatasource,
                                               String sqlFingerprint,
                                               String reportCode,
                                               String logicalObjectKey,
                                               SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            input(sourceSqlText, targetEngine, targetDatasource, sqlFingerprint, reportCode, logicalObjectKey, null),
            profile
        );
    }

    private SqlOptimizationPipelineService.ParsedSqlProfile parseProfile(String sourceSqlText) {
        try {
            return pipelineService.analyze(sourceSqlText, DataSourceTypeEnum.AUTO);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private L2AccelerationArtifactBuilder.AccelerationRecommendationInput input(String sourceSqlText,
                                                                                String targetEngine,
                                                                                String targetDatasource,
                                                                                String sqlFingerprint,
                                                                                String reportCode,
                                                                                String logicalObjectKey,
                                                                                java.util.List<java.util.Map<String, Object>> ruleChain) {
        return new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
            sourceSqlText,
            targetEngine,
            targetDatasource,
            sqlFingerprint,
            reportCode,
            logicalObjectKey,
            ruleChain
        );
    }
}
