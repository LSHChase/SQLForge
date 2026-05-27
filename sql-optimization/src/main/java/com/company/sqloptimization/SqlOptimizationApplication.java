package com.company.sqloptimization;

import com.company.sqlforge.common.utils.DateUtils;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SQL 优化服务启动入口。
 */
@EnableScheduling
@EnableConfigurationProperties({
    com.company.sqloptimization.config.AuthProperties.class,
    com.company.sqloptimization.config.OptimizationTaskExecutionProperties.class,
    com.company.sqloptimization.config.OptimizationGovernanceProperties.class,
    com.company.sqloptimization.config.OptimizationQueryExecutionProperties.class,
    com.company.sqloptimization.config.OptimizationViewMetadataProperties.class,
    com.company.sqloptimization.config.HetuPlanAnalysisProperties.class,
    com.company.sqloptimization.config.RewriteProductionGateProperties.class,
    com.company.sqloptimization.config.RewriteValidationSchedulerProperties.class
})
@SpringBootApplication(scanBasePackages = {"com.company.sqloptimization", "com.company.sqlforge.common"})
public class SqlOptimizationApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(DateUtils.BEIJING_ZONE_ID));
        SpringApplication.run(SqlOptimizationApplication.class, args);
    }
}
