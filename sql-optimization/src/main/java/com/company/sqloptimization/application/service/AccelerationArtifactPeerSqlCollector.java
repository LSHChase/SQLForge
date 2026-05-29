package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactValues.firstText;
import static com.company.sqloptimization.application.service.AccelerationArtifactValues.mapList;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistory;
import com.company.sqloptimization.domain.parsehistory.SqlParseHistoryFilter;
import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendationFilter;
import com.company.sqloptimization.domain.recommendation.repository.AccelerationRecommendationRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class AccelerationArtifactPeerSqlCollector {

    private static final int MAX_PEER_SQLS = 20;

    private final SqlOptimizationPipelineService pipelineService;
    private final AccelerationRecommendationRepository recommendationRepository;
    private final SqlParseHistoryRepository parseHistoryRepository;

    AccelerationArtifactPeerSqlCollector(SqlOptimizationPipelineService pipelineService,
                                         AccelerationRecommendationRepository recommendationRepository,
                                         SqlParseHistoryRepository parseHistoryRepository) {
        this.pipelineService = pipelineService;
        this.recommendationRepository = recommendationRepository;
        this.parseHistoryRepository = parseHistoryRepository;
    }

    List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> collect(
        AccelerationRecommendation recommendation,
        SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (recommendation == null || recommendationRepository == null || !hasCommonSubgraphSignal(profile)) {
            return Collections.emptyList();
        }
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peers =
            new ArrayList<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql>();
        collectRecommendationPeers(recommendation, peers);
        collectParseHistoryPeers(recommendation, peers);
        return peers;
    }

    private boolean hasCommonSubgraphSignal(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile == null) {
            return false;
        }
        Map<String, Object> advancedStructureProfile = profile.toAdvancedStructureProfile();
        return !mapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !mapList(advancedStructureProfile.get("subqueries")).isEmpty();
    }

    private void collectRecommendationPeers(AccelerationRecommendation recommendation,
                                            List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peers) {
        if (peers.size() >= MAX_PEER_SQLS) {
            return;
        }
        if (StringUtils.hasText(recommendation.getBatchId())) {
            AccelerationRecommendationFilter filter = new AccelerationRecommendationFilter();
            filter.setTenantId(recommendation.getTenantId());
            filter.setBatchId(recommendation.getBatchId());
            filter.setLimit(MAX_PEER_SQLS + 1);
            addRecommendationPeers(recommendation, recommendationRepository.findPage(filter), peers);
        }
        if (peers.size() >= MAX_PEER_SQLS || !StringUtils.hasText(recommendation.getReportCode())) {
            return;
        }
        AccelerationRecommendationFilter filter = new AccelerationRecommendationFilter();
        filter.setTenantId(recommendation.getTenantId());
        filter.setReportCode(recommendation.getReportCode());
        filter.setLimit(MAX_PEER_SQLS + 1);
        addRecommendationPeers(recommendation, recommendationRepository.findPage(filter), peers);
    }

    private void addRecommendationPeers(AccelerationRecommendation current,
                                        List<AccelerationRecommendation> rows,
                                        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peers) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (AccelerationRecommendation row : rows) {
            if (row == null
                || current.getRecommendationId().equals(row.getRecommendationId())
                || !StringUtils.hasText(row.getSourceSqlText())
                || hasPeer(peers, "RECOMMENDATION", row.getRecommendationId())
                || peers.size() >= MAX_PEER_SQLS) {
                continue;
            }
            SqlOptimizationPipelineService.ParsedSqlProfile profile = parseProfile(row.getSourceSqlText());
            if (profile == null) {
                continue;
            }
            peers.add(new L2AccelerationArtifactBuilder.CommonSubgraphPeerSql(
                row.getSourceSqlText(),
                row.getSqlFingerprint(),
                "RECOMMENDATION",
                row.getRecommendationId(),
                row.getReportCode(),
                profile.toAdvancedStructureProfile()
            ));
        }
    }

    private void collectParseHistoryPeers(AccelerationRecommendation recommendation,
                                          List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peers) {
        if (parseHistoryRepository == null
            || peers.size() >= MAX_PEER_SQLS
            || !StringUtils.hasText(recommendation.getReportCode())) {
            return;
        }
        SqlParseHistoryFilter filter = new SqlParseHistoryFilter();
        filter.setTenantId(recommendation.getTenantId());
        filter.setReportCode(recommendation.getReportCode());
        filter.setLimit(MAX_PEER_SQLS + 1);
        List<SqlParseHistory> histories = parseHistoryRepository.findPage(filter);
        if (histories == null || histories.isEmpty()) {
            return;
        }
        for (SqlParseHistory history : histories) {
            addParseHistoryPeer(history, peers);
        }
    }

    private void addParseHistoryPeer(SqlParseHistory history,
                                     List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peers) {
        if (history == null
            || peers.size() >= MAX_PEER_SQLS
            || hasPeer(peers, "PARSE_HISTORY", history.getParseHistoryId())) {
            return;
        }
        String sqlText = firstText(history.getSqlText(), history.getSqlTemplateText());
        if (!StringUtils.hasText(sqlText)) {
            return;
        }
        SqlOptimizationPipelineService.ParsedSqlProfile profile = parseProfile(sqlText);
        if (profile == null) {
            return;
        }
        peers.add(new L2AccelerationArtifactBuilder.CommonSubgraphPeerSql(
            sqlText,
            history.getSqlFingerprint(),
            "PARSE_HISTORY",
            history.getParseHistoryId(),
            history.getReportCode(),
            profile.toAdvancedStructureProfile()
        ));
    }

    private SqlOptimizationPipelineService.ParsedSqlProfile parseProfile(String sourceSqlText) {
        try {
            return pipelineService.analyze(sourceSqlText, DataSourceTypeEnum.AUTO);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean hasPeer(List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peers,
                            String sourceKind,
                            String sourceRef) {
        if (peers == null || peers.isEmpty()) {
            return false;
        }
        for (L2AccelerationArtifactBuilder.CommonSubgraphPeerSql peer : peers) {
            if (peer != null
                && sourceKind.equals(peer.getSourceKind())
                && sourceRef != null
                && sourceRef.equals(peer.getSourceRef())) {
                return true;
            }
        }
        return false;
    }
}
