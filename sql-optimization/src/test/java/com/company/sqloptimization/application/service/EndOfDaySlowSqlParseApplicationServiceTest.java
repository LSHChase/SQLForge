package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistoryCandidate;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistoryQuery;
import com.company.sqloptimization.domain.parsehistory.SlowSqlExecutionHistorySource;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlParseHistoryRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndOfDaySlowSqlParseApplicationServiceTest {

    @Test
    void shouldPersistSlowSqlParseHistoryIdempotentlyByBatchKeyAndFingerprint() {
        InMemorySqlParseHistoryRepository repository = new InMemorySqlParseHistoryRepository();
        SqlParseHistoryApplicationService historyService = new SqlParseHistoryApplicationService(repository);
        StructureParseApplicationService structureService = new StructureParseApplicationService(
            new SqlOptimizationPipelineService(),
            mock(GovernanceCapabilityClient.class),
            historyService
        );
        SlowSqlExecutionHistoryCandidate candidate = new SlowSqlExecutionHistoryCandidate();
        candidate.setTenantId("tenant-a");
        candidate.setExecutionHistoryId("history-exec-001");
        candidate.setDatasourceCode("hetu_main");
        candidate.setReportCode("RPT_SLOW");
        candidate.setStageCode("PROD");
        candidate.setSqlText("SELECT * FROM orders WHERE dt = DATE '2026-05-07'");
        candidate.setElapsedMs(Long.valueOf(5000L));
        candidate.setSubmittedAt(Instant.parse("2026-05-07T23:00:00Z"));
        SlowSqlExecutionHistorySource source = new SlowSqlExecutionHistorySource() {
            @Override
            public List<SlowSqlExecutionHistoryCandidate> findCandidates(SlowSqlExecutionHistoryQuery query) {
                return Collections.singletonList(candidate);
            }
        };
        EndOfDaySlowSqlParseApplicationService service =
            new EndOfDaySlowSqlParseApplicationService(source, structureService, historyService);
        EndOfDaySlowSqlParseCommand command = new EndOfDaySlowSqlParseCommand();
        command.setTenantId("tenant-a");
        command.setWindowStart(Instant.parse("2026-05-07T00:00:00Z"));
        command.setWindowEnd(Instant.parse("2026-05-08T00:00:00Z"));
        command.setSlowThresholdMs(Long.valueOf(3000L));

        EndOfDaySlowSqlParseResult first = service.parseSlowSql(command);
        EndOfDaySlowSqlParseResult second = service.parseSlowSql(command);

        assertEquals(1, first.getCandidateCount());
        assertEquals(1, first.getPersistedCount());
        assertEquals(1, second.getPersistedCount());
        assertEquals(first.getBatchKey(), second.getBatchKey());
        assertEquals(1, historyService.findPage(
            "tenant-a",
            SqlParseHistoryApplicationService.SOURCE_END_OF_DAY_SLOW_SQL,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            10
        ).getTotalCount().intValue());
        assertTrue(first.getParseHistoryIds().get(0).startsWith("parse-history-"));
    }
}
