package com.company.benchmarkengine.infrastructure.persistence.mapper;

import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
import java.time.LocalDateTime;
import java.util.List;

public interface BenchmarkTaskMapper {

    BenchmarkTaskRecord selectByTaskId(String taskId);

    List<BenchmarkTaskRecord> selectQueuedTasksSubmittedBefore(LocalDateTime cutoff);

    int insert(BenchmarkTaskRecord record);

    int update(BenchmarkTaskRecord record);
}
