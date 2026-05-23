package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.company.governance.application.controller.dto.RedisRuleSourceUpsertRequest;
import com.company.governance.application.controller.vo.RedisRuleSourceVO;
import com.company.governance.infrastructure.repository.InMemoryRedisRuleSourceRepository;
import com.company.sqlforge.common.context.RequestContext;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RedisRuleSourceApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateUpdateAndListRedisRuleSourceConfigs() {
        RequestContext.set("tenant-a", "admin-010", "request-040", "trace-040", "header", 1L, 2L);
        RedisRuleSourceApplicationService service = new RedisRuleSourceApplicationService(new InMemoryRedisRuleSourceRepository());

        RedisRuleSourceUpsertRequest create = new RedisRuleSourceUpsertRequest();
        create.setTenantId("tenant-a");
        create.setSourceName("jdbc-agent-rules");
        create.setRedisEndpoints("redis://127.0.0.1:6379");
        RedisRuleSourceVO created = service.create(create);

        RedisRuleSourceUpsertRequest update = new RedisRuleSourceUpsertRequest();
        update.setTenantId("tenant-a");
        update.setSourceName("jdbc-agent-rules");
        update.setRedisNamespace("sqlforge:jdbc");
        update.setEnabled(Boolean.FALSE);
        RedisRuleSourceVO changed = service.update(created.getSourceId(), update);
        List<RedisRuleSourceVO> listed = service.list("tenant-a");

        assertEquals("SIMULATED_READY", created.getHealthStatus());
        assertEquals("DISABLED", changed.getHealthStatus());
        assertEquals(1, listed.size());
        assertEquals("sqlforge:jdbc", listed.get(0).getRedisNamespace());
    }
}
