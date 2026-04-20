package com.company.queryexecution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Query execution service bootstrap.
 */
@SpringBootApplication(scanBasePackages = {"com.company.queryexecution", "com.company.sqlforge.common"})
public class QueryExecutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryExecutionApplication.class, args);
    }
}
