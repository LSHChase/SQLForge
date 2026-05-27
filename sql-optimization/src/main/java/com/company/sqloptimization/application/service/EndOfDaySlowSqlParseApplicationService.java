package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistoryCandidate;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistoryQuery;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistorySource;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EndOfDaySlowSqlParseApplicationService {

    private static final long DEFAULT_SLOW_THRESHOLD_MS = 3000L;
    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;
    private static final DateTimeFormatter BATCH_KEY_TIME =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(DateUtils.BEIJING_ZONE);

    private final SlowSqlExecutionHistorySource slowSqlExecutionHistorySource;
    private final StructureParseApplicationService structureParseApplicationService;
    private final SqlParseHistoryApplicationService sqlParseHistoryApplicationService;

    public EndOfDaySlowSqlParseApplicationService(SlowSqlExecutionHistorySource slowSqlExecutionHistorySource,
                                                  StructureParseApplicationService structureParseApplicationService,
                                                  SqlParseHistoryApplicationService sqlParseHistoryApplicationService) {
        this.slowSqlExecutionHistorySource = slowSqlExecutionHistorySource;
        this.structureParseApplicationService = structureParseApplicationService;
        this.sqlParseHistoryApplicationService = sqlParseHistoryApplicationService;
    }

    public EndOfDaySlowSqlParseResult parseSlowSql(EndOfDaySlowSqlParseCommand command) {
        SlowSqlExecutionHistoryQuery query = buildQuery(command);
        String batchKey = batchKey(query);
        EndOfDaySlowSqlParseResult result = new EndOfDaySlowSqlParseResult();
        result.setBatchKey(batchKey);
        List<SlowSqlExecutionHistoryCandidate> candidates = slowSqlExecutionHistorySource.findCandidates(query);
        if (candidates == null) {
            candidates = Collections.emptyList();
        }
        result.setCandidateCount(candidates.size());
        RequestContext.ContextValue previousContext = RequestContext.snapshot();
        try {
            for (SlowSqlExecutionHistoryCandidate candidate : candidates) {
                if (!eligible(candidate, query)) {
                    continue;
                }
                restoreBatchContext(candidate, query);
                try {
                    StructureParseRequest request = buildStructureRequest(candidate, command);
                    StructureParseResponseVO structureParse = structureParseApplicationService.parse(request);
                    SqlParseHistoryWriteResult writeResult = sqlParseHistoryApplicationService.writeEndOfDaySlowSqlHistory(
                        structureParse,
                        request,
                        batchKey,
                        candidate.getExecutionHistoryId(),
                        "VALID".equals(structureParse.getSyntaxStatus()) ? "SUCCESS" : "FAILED"
                    );
                    result.record(writeResult);
                } catch (RuntimeException ex) {
                    result.recordFailure();
                }
            }
            return result;
        } finally {
            RequestContext.restore(previousContext);
        }
    }

    private SlowSqlExecutionHistoryQuery buildQuery(EndOfDaySlowSqlParseCommand command) {
        EndOfDaySlowSqlParseCommand safeCommand = command == null ? new EndOfDaySlowSqlParseCommand() : command;
        Instant windowEnd = safeCommand.getWindowEnd() == null ? Instant.now() : safeCommand.getWindowEnd();
        Instant windowStart = safeCommand.getWindowStart() == null
            ? windowEnd.minusSeconds(24L * 60L * 60L)
            : safeCommand.getWindowStart();
        SlowSqlExecutionHistoryQuery query = new SlowSqlExecutionHistoryQuery();
        query.setTenantId(resolveTenantId(safeCommand.getTenantId()));
        query.setWindowStart(windowStart);
        query.setWindowEnd(windowEnd);
        query.setSlowThresholdMs(safeCommand.getSlowThresholdMs() == null
            ? DEFAULT_SLOW_THRESHOLD_MS
            : Math.max(1L, safeCommand.getSlowThresholdMs().longValue()));
        int limit = safeCommand.getLimit() == null ? DEFAULT_LIMIT : safeCommand.getLimit().intValue();
        query.setLimit(Math.min(MAX_LIMIT, Math.max(1, limit)));
        return query;
    }

    private StructureParseRequest buildStructureRequest(SlowSqlExecutionHistoryCandidate candidate,
                                                        EndOfDaySlowSqlParseCommand command) {
        StructureParseRequest request = new StructureParseRequest();
        request.setSqlText(candidate.getSqlText());
        request.setSqlTemplateText(candidate.getSqlTemplateText());
        request.setBindParameters(candidate.getBindParameters());
        request.setBindingMode(candidate.getBindingMode());
        request.setDatasourceCode(candidate.getDatasourceCode());
        request.setParserMode(command == null ? null : command.getParserMode());
        request.setCommentContext(buildCommentContext(candidate));
        request.setHistoryWriteEnabled(Boolean.FALSE);
        return request;
    }

    private Map<String, Object> buildCommentContext(SlowSqlExecutionHistoryCandidate candidate) {
        Map<String, Object> context = new LinkedHashMap<String, Object>();
        putIfPresent(context, "execution_history_id", candidate.getExecutionHistoryId());
        putIfPresent(context, "report_code", candidate.getReportCode());
        putIfPresent(context, "stage", candidate.getStageCode());
        putIfPresent(context, "biz_date", candidate.getBizDate());
        putIfPresent(context, "submitted_by", candidate.getSubmittedBy());
        if (candidate.getSubmittedAt() != null) {
            context.put("execution_submitted_at", candidate.getSubmittedAt().toString());
        }
        if (candidate.getElapsedMs() != null) {
            context.put("elapsed_ms", candidate.getElapsedMs());
        }
        return context;
    }

    private boolean eligible(SlowSqlExecutionHistoryCandidate candidate, SlowSqlExecutionHistoryQuery query) {
        if (candidate == null || !StringUtils.hasText(candidate.getSqlText())) {
            return false;
        }
        if (candidate.getElapsedMs() != null && candidate.getElapsedMs().longValue() < query.getSlowThresholdMs()) {
            return false;
        }
        if (candidate.getSubmittedAt() != null
            && (candidate.getSubmittedAt().isBefore(query.getWindowStart())
                || candidate.getSubmittedAt().isAfter(query.getWindowEnd()))) {
            return false;
        }
        return true;
    }

    private void restoreBatchContext(SlowSqlExecutionHistoryCandidate candidate, SlowSqlExecutionHistoryQuery query) {
        long now = System.currentTimeMillis();
        RequestContext.set(
            resolveTenantId(candidate.getTenantId(), query.getTenantId()),
            StringUtils.hasText(candidate.getSubmittedBy()) ? candidate.getSubmittedBy() : "system",
            "eod-slow-sql-" + now,
            "eod-slow-sql-" + now,
            AuthSourceConstants.HEADER,
            now,
            now + 300000L
        );
    }

    private String batchKey(SlowSqlExecutionHistoryQuery query) {
        return "eod-slow-sql:"
            + query.getTenantId()
            + ":"
            + BATCH_KEY_TIME.format(query.getWindowStart())
            + "-"
            + BATCH_KEY_TIME.format(query.getWindowEnd())
            + ":"
            + query.getSlowThresholdMs();
    }

    private void putIfPresent(Map<String, Object> context, String key, String value) {
        if (StringUtils.hasText(value)) {
            context.put(key, value);
        }
    }

    private String resolveTenantId(String primary) {
        return resolveTenantId(primary, RequestContext.getTenantId());
    }

    private String resolveTenantId(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary;
        }
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        return "system";
    }
}
