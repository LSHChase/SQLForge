package com.company.benchmarkengine.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnExpression("'${benchmark-engine.queues.mode:database-worker}' != 'local-placeholder'")
@MapperScan("com.company.benchmarkengine.infrastructure.persistence.mapper")
public class BenchmarkPersistenceMybatisConfiguration {
}
