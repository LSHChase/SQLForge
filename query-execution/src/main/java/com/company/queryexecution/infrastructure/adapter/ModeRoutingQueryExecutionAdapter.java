package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.service.HetuRouteCalibrationService;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.HetuRouteCalibrationModeSnapshot;
import com.company.queryexecution.domain.query.HetuRouteCalibrationSnapshot;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ModeRoutingQueryExecutionAdapter implements QueryExecutionAdapter {

    private final HetuRouteCalibrationService hetuRouteCalibrationService;
    private final DeterministicQueryExecutionAdapter deterministicQueryExecutionAdapter;
    private final Map<QueryExecutionAccessMode, HetuExecutionModeAdapter> modeAdapters;

    public ModeRoutingQueryExecutionAdapter(QueryExecutionHetuProperties properties,
                                            HetuRouteCalibrationService hetuRouteCalibrationService,
                                            List<HetuExecutionModeAdapter> adapters) {
        this.hetuRouteCalibrationService = hetuRouteCalibrationService;
        this.deterministicQueryExecutionAdapter = new DeterministicQueryExecutionAdapter();
        this.modeAdapters = new LinkedHashMap<QueryExecutionAccessMode, HetuExecutionModeAdapter>();
        for (HetuExecutionModeAdapter adapter : adapters) {
            this.modeAdapters.put(adapter.getMode(), adapter);
        }
    }

    @Override
    public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                      String actualSql,
                                      QueryExecuteRequest request,
                                      boolean degradedPath) {
        if (targetEngine != DataSourceTypeEnum.HETU) {
            return deterministicQueryExecutionAdapter.execute(targetEngine, actualSql, request, degradedPath);
        }
        HetuRouteCalibrationSnapshot calibrationSnapshot = hetuRouteCalibrationService.currentSnapshot();
        if (!calibrationSnapshot.isEnabled()) {
            throw new HetuExecutionUnavailableException(
                "当前环境已禁用 Hetu 执行链路",
                java.util.Collections.singletonList("CHAIN_DISABLED"),
                calibrationSnapshot.getRouteProfile(),
                calibrationSnapshot.routeOrderNames(),
                calibrationSnapshot.getEvidenceSource(),
                calibrationSnapshot.getLiveVerificationStatus()
            );
        }
        if (calibrationSnapshot.getEffectiveRouteOrder().isEmpty()) {
            throw new HetuExecutionUnavailableException(
                "当前环境未配置 Hetu 执行模式",
                java.util.Collections.singletonList("CHAIN_UNCONFIGURED"),
                calibrationSnapshot.getRouteProfile(),
                calibrationSnapshot.routeOrderNames(),
                calibrationSnapshot.getEvidenceSource(),
                calibrationSnapshot.getLiveVerificationStatus()
            );
        }
        List<String> attemptedModes = new ArrayList<String>();
        RuntimeException lastFailure = null;
        for (QueryExecutionAccessMode mode : calibrationSnapshot.getEffectiveRouteOrder()) {
            HetuRouteCalibrationModeSnapshot modeSnapshot = calibrationSnapshot.getModeSnapshot(mode);
            if (modeSnapshot != null && !modeSnapshot.isWillAttemptInCurrentPolicy()) {
                attemptedModes.add(mode.name() + ":SKIPPED_" + modeSnapshot.getReadinessStatus());
                continue;
            }
            HetuExecutionModeAdapter adapter = modeAdapters.get(mode);
            if (adapter == null) {
                attemptedModes.add(mode.name() + ":SKIPPED_ADAPTER_UNAVAILABLE");
                continue;
            }
            try {
                attemptedModes.add(mode.name());
                QueryExecutionStep step = adapter.execute(actualSql, request, degradedPath);
                return step.withAttemptedModes(new ArrayList<String>(attemptedModes)).withRouteCalibration(
                    calibrationSnapshot.getRouteProfile(),
                    calibrationSnapshot.routeOrderNames(),
                    calibrationSnapshot.getEvidenceSource(),
                    calibrationSnapshot.getLiveVerificationStatus()
                );
            } catch (RuntimeException ex) {
                attemptedModes.add(mode.name() + ":" + hetuRouteCalibrationService.classifyFailure(ex));
                lastFailure = ex;
            }
        }
        if (lastFailure != null) {
            throw new HetuExecutionUnavailableException(
                "已校准的 Hetu 执行模式均未成功，attemptedModes=" + attemptedModes,
                attemptedModes,
                lastFailure,
                calibrationSnapshot.getRouteProfile(),
                calibrationSnapshot.routeOrderNames(),
                calibrationSnapshot.getEvidenceSource(),
                calibrationSnapshot.getLiveVerificationStatus()
            );
        }
        throw new HetuExecutionUnavailableException(
            "当前没有可路由的已校准 Hetu 执行模式",
            attemptedModes,
            calibrationSnapshot.getRouteProfile(),
            calibrationSnapshot.routeOrderNames(),
            calibrationSnapshot.getEvidenceSource(),
            calibrationSnapshot.getLiveVerificationStatus()
        );
    }
}
