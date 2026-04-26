package com.company.benchmarkengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties({
    com.company.benchmarkengine.config.AuthProperties.class,
    com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties.class,
    com.company.benchmarkengine.config.BenchmarkTaskQueueProperties.class,
    com.company.benchmarkengine.config.BenchmarkEngineGovernanceProperties.class,
    com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties.class,
    com.company.benchmarkengine.config.BenchmarkEngineQueryExecutionProperties.class
})
@SpringBootApplication(scanBasePackages = {"com.company.benchmarkengine", "com.company.sqlforge.common"})
public class BenchmarkEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkEngineApplication.class, args);
    }
}
