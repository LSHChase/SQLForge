package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.message.entity.MessageQueueRecord;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface MessageQueueMapper {

    int insert(MessageQueueRecord messageQueueRecord);

    List<MessageQueueRecord> selectPendingMessages(@Param("topic") String topic, @Param("batchSize") int batchSize);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorLog") String errorLog,
                     @Param("consumedAtSql") String consumedAtSql);

    int retryFailedMessages();

    MessageQueueRecord selectQueueStats();
}
