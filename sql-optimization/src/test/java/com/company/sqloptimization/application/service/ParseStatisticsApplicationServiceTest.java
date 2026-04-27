package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseSqlIssueStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchFileType;
import com.company.sqloptimization.domain.batch.ParseBatchImportMode;
import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.ParseBatchItemStatus;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ParseStatisticsApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldAggregateOverviewSceneAndSqlStatisticsWithinTenant() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        InMemoryParseBatchRepository batchRepository = new InMemoryParseBatchRepository();
        InMemoryParseBatchItemRepository itemRepository = new InMemoryParseBatchItemRepository();
        Instant now = Instant.now();
        ParseBatch tenantBatch = ParseBatch.initialize(
            "batch-a",
            "tenant-a",
            "batch-a",
            ParseBatchImportMode.TABULAR_FILE,
            com.company.sqloptimization.domain.batch.ParseBatchSourceType.FILE_UPLOAD,
            ParseBatchFileType.CSV,
            "v1",
            "hetu_main",
            false,
            "operator-001",
            now
        );
        ParseBatch otherTenantBatch = ParseBatch.initialize(
            "batch-b",
            "tenant-b",
            "batch-b",
            ParseBatchImportMode.TABULAR_FILE,
            com.company.sqloptimization.domain.batch.ParseBatchSourceType.FILE_UPLOAD,
            ParseBatchFileType.CSV,
            "v1",
            "hetu_main",
            false,
            "operator-002",
            now
        );
        batchRepository.save(tenantBatch);
        batchRepository.save(otherTenantBatch);
        itemRepository.save(item("item-a1", "batch-a", 1, "RPT_A", Arrays.asList("MISSING_FILTER", "WIDE_PROJECTION"), now));
        itemRepository.save(item("item-a2", "batch-a", 2, "RPT_B", Arrays.asList("GENERAL_WARNING"), now));
        itemRepository.save(item("item-b1", "batch-b", 1, "RPT_C", Arrays.asList("ROUTE_HINT_CONFLICT"), now));

        ParseStatisticsApplicationService service = new ParseStatisticsApplicationService(batchRepository, itemRepository);

        ParseStatisticsOverviewVO overview = service.overview();
        List<ParseIssueSceneStatisticVO> byScene = service.byIssueScene();
        List<ParseSqlIssueStatisticVO> bySql = service.bySql();

        assertEquals(Integer.valueOf(2), overview.getTotalSqlCount());
        assertEquals(Integer.valueOf(3), overview.getTotalIssueCount());
        assertEquals(Integer.valueOf(2), overview.getIssueSqlCount());
        assertEquals(Integer.valueOf(1), overview.getUrgentSqlCount());
        assertEquals("MISSING_FILTER", byScene.get(0).getIssueScene());
        assertEquals(Integer.valueOf(1), byScene.get(0).getAffectedSqlCount());
        assertEquals("item-a1", bySql.get(0).getItemId());
        assertEquals("P1", bySql.get(0).getHighestPriorityLevel());
    }

    private ParseBatchItem item(String itemId,
                                String batchId,
                                int sequenceNumber,
                                String reportCode,
                                List<String> issueScenes,
                                Instant now) {
        ParseBatchItem item = ParseBatchItem.create(
            itemId,
            batchId,
            sequenceNumber,
            reportCode,
            reportCode,
            "hetu_main",
            "PROD",
            "2026-04-25",
            "high",
            "owner",
            "",
            "SELECT * FROM orders",
            null,
            null,
            null,
            now
        );
        item.complete(
            "parse-" + itemId,
            "VALID",
            "AVAILABLE",
            "CONNECTED",
            ParseBatchItemStatus.SUCCESS,
            null,
            issueScenes,
            java.util.Collections.<String>emptyList(),
            now
        );
        return item;
    }
}
