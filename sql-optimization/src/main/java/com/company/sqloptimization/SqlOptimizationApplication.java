package com.company.sqloptimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SQL optimization service bootstrap.
 */
@SpringBootApplication(scanBasePackages = {"com.company.sqloptimization", "com.company.sqlforge.common"})
public class SqlOptimizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlOptimizationApplication.class, args);
    }
}
