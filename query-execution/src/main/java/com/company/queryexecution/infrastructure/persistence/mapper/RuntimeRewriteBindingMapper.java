package com.company.queryexecution.infrastructure.persistence.mapper;

import com.company.queryexecution.infrastructure.persistence.entity.RuntimeRewriteBindingRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RuntimeRewriteBindingMapper {

    RuntimeRewriteBindingRecord selectByRuntimeBindingId(@Param("runtimeBindingId") String runtimeBindingId);

    RuntimeRewriteBindingRecord selectActiveByTenantIdAndSqlFingerprint(@Param("tenantId") String tenantId,
                                                                        @Param("sqlFingerprint") String sqlFingerprint);

    RuntimeRewriteBindingRecord selectLatestByTenantIdAndSqlFingerprint(@Param("tenantId") String tenantId,
                                                                        @Param("sqlFingerprint") String sqlFingerprint);

    List<RuntimeRewriteBindingRecord> selectByTenantIdAndSqlFingerprint(@Param("tenantId") String tenantId,
                                                                        @Param("sqlFingerprint") String sqlFingerprint);

    List<RuntimeRewriteBindingRecord> selectActiveByTenantId(@Param("tenantId") String tenantId);

    int insert(RuntimeRewriteBindingRecord record);

    int update(RuntimeRewriteBindingRecord record);
}
