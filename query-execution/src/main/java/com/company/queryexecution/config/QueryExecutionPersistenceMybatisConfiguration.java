package com.company.queryexecution.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.company.queryexecution.infrastructure.persistence.mapper")
public class QueryExecutionPersistenceMybatisConfiguration {
}
