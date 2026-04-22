package com.company.benchmarkengine.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "benchmark-engine.queues", name = "mode", havingValue = "database-worker")
@MapperScan("com.company.benchmarkengine.infrastructure.persistence.mapper")
public class BenchmarkPersistenceMybatisConfiguration {
}
