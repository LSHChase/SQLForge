package com.company.queryexecution;

import com.company.sqlforge.common.utils.DateUtils;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 查询执行服务启动入口。
 */
@EnableConfigurationProperties({
    com.company.queryexecution.config.AuthProperties.class,
    com.company.queryexecution.config.QueryExecutionGovernanceProperties.class,
    com.company.queryexecution.config.QueryExecutionHetuProperties.class,
    com.company.queryexecution.config.QueryExecutionCacheBackendProperties.class,
    com.company.queryexecution.config.QueryExecutionJdbcAgentRedisProperties.class,
    com.company.queryexecution.config.QueryExecutionRewriteProperties.class
})
@SpringBootApplication(scanBasePackages = {"com.company.queryexecution", "com.company.sqlforge.common"})
public class QueryExecutionApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(DateUtils.BEIJING_ZONE_ID));
        SpringApplication.run(QueryExecutionApplication.class, args);
    }
}
