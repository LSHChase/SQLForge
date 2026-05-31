package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class L2DynamicSnapshotAggregateMvCandidateGeneratorTest {

    private final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    @Test
    void shouldGenerateDocsTest01ExpectedSqlFromDynamicShapeNotStaticFixture() throws Exception {
        String sourceSql = readSqlFixture("docs/test01.sql");
        L2DynamicSnapshotAggregateMvCandidateGenerator.RewriteCandidate candidate = rewriteCandidate(sourceSql);

        assertNotNull(candidate);
        assertEquals(Boolean.FALSE, candidate.getEvidence().get("staticTest01TemplateUsed"));
        assertEquals("DYNAMIC_AST_PROFILE_SNAPSHOT_AGGREGATE", candidate.getEvidence().get("generator"));
        assertTrue(candidate.getRewriteSql().contains("raw_customer_snapshot"));
        assertTrue(candidate.getRewriteSql().contains("report_customer_snapshot"));
        assertTrue(candidate.getRewriteSql().contains("base_100_anchor"));
        assertTrue(candidate.getRewriteSql().contains("metric_by_org"));
        assertTrue(candidate.getRewriteSql().contains("growth_by_org"));

        String variantSql = sourceSql
            .replace("41H006", "41H008")
            .replace("20260430", "20260331")
            .replace("20260519", "20260531");
        L2DynamicSnapshotAggregateMvCandidateGenerator.RewriteCandidate variantCandidate = rewriteCandidate(variantSql);

        assertNotNull(variantCandidate);
        assertTrue(variantCandidate.getRewriteSql().contains("41H008"), variantCandidate.getRewriteSql());
        assertTrue(variantCandidate.getRewriteSql().contains("20260331"), variantCandidate.getRewriteSql());
        assertTrue(variantCandidate.getRewriteSql().contains("20260531"), variantCandidate.getRewriteSql());
        assertFalse(normalizeExecutableSql(candidate.getRewriteSql())
            .equals(normalizeExecutableSql(variantCandidate.getRewriteSql())));
    }

    @Test
    void shouldGenerateMvRewriteThatOnlyReadsMaterializedView() throws Exception {
        String sourceSql = readSqlFixture("docs/test01.sql");
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sourceSql, DataSourceTypeEnum.HETU);

        L2DynamicSnapshotAggregateMvCandidateGenerator.CandidateSql candidate =
            L2DynamicSnapshotAggregateMvCandidateGenerator.generate(sourceSql, "mv_test01_dynamic", "HETU", profile);

        assertNotNull(candidate);
        assertTrue(candidate.getBlockingReasons().isEmpty(), String.valueOf(candidate.getBlockingReasons()));
        assertTrue(candidate.getDdlSql().contains("CREATE MATERIALIZED VIEW mv_test01_dynamic AS"), candidate.getDdlSql());
        assertTrue(candidate.getRewriteSql().contains("FROM mv_test01_dynamic"), candidate.getRewriteSql());
        assertFalse(candidate.getRewriteSql().contains("BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM"), candidate.getRewriteSql());
        assertTrue(candidate.getValidationSql().contains("RESULT_SET_EXCEPT_DIFF"), candidate.getValidationSql());
        assertEquals(Boolean.TRUE, candidate.getCoverage("mv_test01_dynamic").get("rewriteSqlReferencesMv"));
    }

    @Test
    void shouldExternalizeOrgLevelAndThresholdsFromPredicates() throws Exception {
        String sourceSql = readSqlFixture("docs/test01.sql")
            .replace("ORG_LVL = 4", "ORG_LVL = 5")
            .replace(">= 1000000", ">= 2000000")
            .replace("< 6000000", "< 9000000")
            .replace(">= 6000000", ">= 9000000");

        L2DynamicSnapshotAggregateMvCandidateGenerator.RewriteCandidate candidate = rewriteCandidate(sourceSql);

        assertNotNull(candidate);
        assertTrue(candidate.getRewriteSql().contains("ORG_LVL = 5"), candidate.getRewriteSql());
        assertTrue(candidate.getRewriteSql().contains("base_aum >= 2000000"), candidate.getRewriteSql());
        assertTrue(candidate.getRewriteSql().contains("snapshot_aum < 9000000"), candidate.getRewriteSql());
        assertEquals("5", String.valueOf(candidate.getEvidence().get("orgLevelValue")));
        assertEquals("2000000", String.valueOf(candidate.getEvidence().get("lowThreshold")));
        assertEquals("9000000", String.valueOf(candidate.getEvidence().get("highThreshold")));
    }

    @Test
    void shouldRejectSnapshotRewriteWhenThresholdShapeIsIncomplete() throws Exception {
        String sourceSql = readSqlFixture("docs/test01.sql")
            .replace("6000000", "1000000");

        assertTrue(sourceSql.contains("1000000"));
        assertFalse(sourceSql.contains("6000000"));
        assertNull(rewriteCandidate(sourceSql));
    }

    private L2DynamicSnapshotAggregateMvCandidateGenerator.RewriteCandidate rewriteCandidate(String sql) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        return L2DynamicSnapshotAggregateMvCandidateGenerator.rewriteCandidate(sql, profile);
    }

    private String readSqlFixture(String relativePath) throws Exception {
        return new String(Files.readAllBytes(repositoryRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private String normalizeExecutableSql(String sql) {
        return sql == null ? "" : sql.replaceAll("(?s)--.*?$", "")
            .replaceAll("\\s+", " ")
            .replaceAll("\\s*;\\s*$", "")
            .trim();
    }

    private Path repositoryRoot() {
        Path current = Paths.get("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("docs/test01.sql"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("repository root not found");
    }
}
