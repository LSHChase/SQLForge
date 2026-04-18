package com.company.governance;

import com.company.governance.config.AuthProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.company.governance")
@EnableConfigurationProperties(AuthProperties.class)
@MapperScan("com.company.governance.infrastructure.persistence.mapper")
public class GovernanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GovernanceApplication.class, args);
    }
}
