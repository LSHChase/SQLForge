package com.company.queryexecution.domain.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReadonlyQueryGuardTest {

    @Test
    void shouldAllowReadonlySqlWithLeadingLineComments() {
        ReadonlyQueryAssessment assessment = ReadonlyQueryGuard.assess(
            "--report_code=RPT_SQL_QUERY\n--stage=PROD\nSELECT 1 AS ok"
        );

        assertTrue(assessment.isReadonly());
    }

    @Test
    void shouldAllowReadonlySqlWithLeadingBlockComment() {
        ReadonlyQueryAssessment assessment = ReadonlyQueryGuard.assess(
            "/* governance context */\nSELECT 1 AS ok"
        );

        assertTrue(assessment.isReadonly());
    }

    @Test
    void shouldRejectWriteSqlEvenAfterLeadingComment() {
        ReadonlyQueryAssessment assessment = ReadonlyQueryGuard.assess(
            "--report_code=RPT_MUTATION\nDELETE FROM orders"
        );

        assertFalse(assessment.isReadonly());
    }
}
