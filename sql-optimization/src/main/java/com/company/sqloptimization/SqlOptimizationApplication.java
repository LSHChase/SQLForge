package com.company.sqloptimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SQL optimization service bootstrap.
 */
@EnableScheduling
@EnableConfigurationProperties({
    com.company.sqloptimization.config.AuthProperties.class,
    com.company.sqloptimization.config.OptimizationTaskExecutionProperties.class,
    com.company.sqloptimization.config.OptimizationGovernanceProperties.class
})
@SpringBootApplication(scanBasePackages = {"com.company.sqloptimization", "com.company.sqlforge.common"})
public class SqlOptimizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlOptimizationApplication.class, args);
    }
}
