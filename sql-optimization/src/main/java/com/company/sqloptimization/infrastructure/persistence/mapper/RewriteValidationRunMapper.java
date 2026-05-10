package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.RewriteValidationRunRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RewriteValidationRunMapper {

    RewriteValidationRunRecord selectByValidationRunId(@Param("validationRunId") String validationRunId);

    List<RewriteValidationRunRecord> selectByRewriteRecordId(@Param("rewriteRecordId") String rewriteRecordId);

    int insert(RewriteValidationRunRecord record);

    int update(RewriteValidationRunRecord record);
}
