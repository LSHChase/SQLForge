package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ParseBatchRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParseBatchMapper {

    ParseBatchRecord selectByBatchId(@Param("batchId") String batchId);

    int insert(ParseBatchRecord record);

    int update(ParseBatchRecord record);
}
