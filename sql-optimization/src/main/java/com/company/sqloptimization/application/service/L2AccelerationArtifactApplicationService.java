package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class L2AccelerationArtifactApplicationService {

    private final SqlOptimizationPipelineService pipelineService;
    private final AccelerationArtifactPeerSqlCollector peerSqlCollector;

    @Autowired
    public L2AccelerationArtifactApplicationService(SqlOptimizationPipelineService pipelineService,
                                                    AccelerationRecommendationRepository recommendationRepository,
                                                    SqlParseHistoryRepository parseHistoryRepository) {
        this.pipelineService = pipelineService;
        this.peerSqlCollector = new AccelerationArtifactPeerSqlCollector(
            pipelineService,
            recommendationRepository,
            parseHistoryRepository
        );
    }

    public L2AccelerationArtifactApplicationService(SqlOptimizationPipelineService pipelineService) {
        this(pipelineService, null, null);
    }

    public Map<String, Object> buildForRecommendation(AccelerationRecommendation recommendation) {
        if (recommendation == null) {
            return null;
        }
        SqlOptimizationPipelineService.ParsedSqlProfile profile = parseProfile(recommendation.getSourceSqlText());
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls =
            peerSqlCollector.collect(recommendation, profile);
        return L2AccelerationArtifactBuilder.buildForRecommendation(
            input(
                recommendation.getSourceSqlText(),
                recommendation.getTargetEngine(),
                recommendation.getTargetDatasource(),
                recommendation.getSqlFingerprint(),
                recommendation.getReportCode(),
                recommendation.getLogicalObjectKey(),
                recommendation.getRuleChain(),
                peerSqls
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
        return input(
            sourceSqlText,
            targetEngine,
            targetDatasource,
            sqlFingerprint,
            reportCode,
            logicalObjectKey,
            ruleChain,
            Collections.<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql>emptyList()
        );
    }

    private L2AccelerationArtifactBuilder.AccelerationRecommendationInput input(String sourceSqlText,
                                                                                String targetEngine,
                                                                                String targetDatasource,
                                                                                String sqlFingerprint,
                                                                                String reportCode,
                                                                                String logicalObjectKey,
                                                                                java.util.List<java.util.Map<String, Object>> ruleChain,
                                                                                List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        return new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
            sourceSqlText,
            targetEngine,
            targetDatasource,
            sqlFingerprint,
            reportCode,
            logicalObjectKey,
            ruleChain,
            peerSqls
        );
    }
}
