package com.company.sqloptimization.infrastructure.persistence.mapper;

import com.company.sqloptimization.infrastructure.persistence.entity.OptimizationTaskRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface OptimizationTaskMapper {

    OptimizationTaskRecord selectByTaskId(String taskId);

    List<OptimizationTaskRecord> selectQueuedTasksSubmittedBefore(@Param("cutoff") LocalDateTime cutoff);

    int insert(OptimizationTaskRecord record);

    int update(OptimizationTaskRecord record);
}
