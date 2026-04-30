package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ParseBatchRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParseBatchMapper {

    ParseBatchRecord selectByBatchId(@Param("batchId") String batchId);

    List<ParseBatchRecord> selectAll();

    int insert(ParseBatchRecord record);

    int update(ParseBatchRecord record);
}
