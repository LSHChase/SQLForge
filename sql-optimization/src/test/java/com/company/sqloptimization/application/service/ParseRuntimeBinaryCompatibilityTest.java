package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class ParseRuntimeBinaryCompatibilityTest {

    @Test
    void shouldNotDependOnEnumSwitchHelperClassForPriorityScoring() {
        assertThrows(
            ClassNotFoundException.class,
            () -> Class.forName(StructureParsePriorityScorer.class.getName() + "$1")
        );
    }

    @Test
    void reportSourceRowShouldExposeStableNoArgConstructorOnly() throws Exception {
        Class<?> rowClass = Class.forName(ReportBatchApplicationService.class.getName() + "$ReportSourceRow");
        Constructor<?>[] constructors = rowClass.getDeclaredConstructors();

        assertEquals(1, constructors.length);
        assertEquals(0, constructors[0].getParameterTypes().length);
        assertTrue(constructors[0].getDeclaringClass().getName().endsWith("$ReportSourceRow"));
        for (Method method : rowClass.getDeclaredMethods()) {
            assertTrue(!method.getName().startsWith("access$"), "ReportSourceRow 不应依赖 synthetic access bridge");
        }
    }
}
