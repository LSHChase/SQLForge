package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.AuditLogRecord;

public interface AuditLogMapper {

    AuditLogRecord selectById(Long id);

    int insert(AuditLogRecord auditLogRecord);
}
