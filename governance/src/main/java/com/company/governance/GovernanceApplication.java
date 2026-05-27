package com.company.governance;

import com.company.governance.config.AuthProperties;
import com.company.governance.config.GovernanceDatasourceDriverProperties;
import com.company.governance.config.GovernanceAuditProperties;
import com.company.governance.config.GovernanceAccessProperties;
import com.company.governance.config.GovernanceBenchmarkEngineProperties;
import com.company.governance.config.GovernanceSqlOptimizationProperties;
import com.company.governance.config.MessagingProperties;
import com.company.sqlforge.common.utils.DateUtils;
import java.util.TimeZone;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.company.governance", "com.company.sqlforge.common"})
@EnableConfigurationProperties({
    AuthProperties.class,
    MessagingProperties.class,
    GovernanceAccessProperties.class,
    GovernanceAuditProperties.class,
    GovernanceBenchmarkEngineProperties.class,
    GovernanceSqlOptimizationProperties.class,
    GovernanceDatasourceDriverProperties.class
})
@EnableScheduling
@MapperScan("com.company.governance.infrastructure.persistence.mapper")
public class GovernanceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(DateUtils.BEIJING_ZONE_ID));
        SpringApplication.run(GovernanceApplication.class, args);
    }
}
