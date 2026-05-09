package com.company.sqloptimization.infrastructure.plananalysis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Driver;
import org.junit.jupiter.api.Test;

class HetuJdbcDriverClasspathTest {

    @Test
    void shouldLoadConfiguredHetuJdbcDriverClass() throws Exception {
        Class<?> driverClass = Class.forName("io.prestosql.jdbc.PrestoDriver");

        assertTrue(Driver.class.isAssignableFrom(driverClass));
    }
}
