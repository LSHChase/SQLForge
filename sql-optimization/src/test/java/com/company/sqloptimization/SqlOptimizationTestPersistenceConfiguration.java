package com.company.sqloptimization;

import com.company.sqloptimization.domain.parsehistory.repository.SqlParseHistoryRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlParseHistoryRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class SqlOptimizationTestPersistenceConfiguration {

    @Bean
    public SqlParseHistoryRepository sqlParseHistoryRepository() {
        return new InMemorySqlParseHistoryRepository();
    }
}
