package com.company.sqloptimization.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.company.sqloptimization.infrastructure.persistence.mapper.SqlParseHistoryMapper;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlParseHistoryRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SqlParseHistoryRepositorySelectionTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withBean(SqlParseHistoryMapper.class, () -> mock(SqlParseHistoryMapper.class))
        .withUserConfiguration(MybatisSqlParseHistoryRepository.class);

    @Test
    void shouldSelectDatabaseRepositoryWhenConfigured() {
        contextRunner
            .withPropertyValues("sql-optimization.parse-history.repository=database")
            .run(context -> {
                assertTrue(context.containsBean("mybatisSqlParseHistoryRepository"));
                assertFalse(context.containsBean("inMemorySqlParseHistoryRepository"));
                Map<String, SqlParseHistoryRepository> repositories =
                    context.getBeansOfType(SqlParseHistoryRepository.class);
                assertTrue(repositories.get("mybatisSqlParseHistoryRepository")
                    instanceof MybatisSqlParseHistoryRepository);
            });
    }

    @Test
    void shouldNotRegisterInMemoryRepositoryAsRuntimeFallback() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("inMemorySqlParseHistoryRepository"));
            assertTrue(context.getBeansOfType(InMemorySqlParseHistoryRepository.class).isEmpty());
        });
    }
}
