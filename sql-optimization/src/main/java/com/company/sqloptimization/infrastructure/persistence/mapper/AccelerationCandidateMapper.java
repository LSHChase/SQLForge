package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.AccelerationCandidateRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccelerationCandidateMapper {

    AccelerationCandidateRecord selectByCandidateId(@Param("candidateId") String candidateId);

    List<AccelerationCandidateRecord> selectByTenantId(@Param("tenantId") String tenantId);

    int insert(AccelerationCandidateRecord record);

    int update(AccelerationCandidateRecord record);
}
