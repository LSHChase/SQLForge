package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.ParseBatchItemRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ParseBatchItemMapper {

    void insert(ParseBatchItemRecord record);

    void update(ParseBatchItemRecord record);

    ParseBatchItemRecord selectByItemId(@Param("itemId") String itemId);

    List<ParseBatchItemRecord> selectByBatchId(@Param("batchId") String batchId);

    List<ParseBatchItemRecord> selectAll();
}
