package com.company.sqloptimization.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "sql-optimization.queues", name = "mode", havingValue = "database-worker")
@MapperScan("com.company.sqloptimization.infrastructure.persistence.mapper")
public class OptimizationPersistenceMybatisConfiguration {
}
