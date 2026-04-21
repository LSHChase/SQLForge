package com.company.benchmarkengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.company.benchmarkengine", "com.company.sqlforge.common"})
public class BenchmarkEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchmarkEngineApplication.class, args);
    }
}
