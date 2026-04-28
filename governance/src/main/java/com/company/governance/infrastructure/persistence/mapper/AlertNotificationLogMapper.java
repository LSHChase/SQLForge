package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface AlertNotificationLogMapper {

    int insert(AlertNotificationLogRecord record);

    List<AlertNotificationLogRecord> selectByAlertId(@Param("alertId") String alertId);
}
