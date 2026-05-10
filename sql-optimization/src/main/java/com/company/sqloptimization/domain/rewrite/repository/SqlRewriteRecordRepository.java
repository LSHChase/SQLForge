package com.company.sqloptimization.domain.rewrite.repository;

import com.company.sqloptimization.domain.rewrite.RewriteValidationRun;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import java.util.List;

public interface SqlRewriteRecordRepository {

    SqlRewriteRecord saveRecord(SqlRewriteRecord rewriteRecord);

    SqlRewriteRecord findRecordById(String rewriteRecordId);

    List<SqlRewriteRecord> findRecordsByTenantId(String tenantId);

    RewriteValidationRun saveValidationRun(RewriteValidationRun validationRun);

    List<RewriteValidationRun> findValidationRunsByRewriteRecordId(String rewriteRecordId);
}
