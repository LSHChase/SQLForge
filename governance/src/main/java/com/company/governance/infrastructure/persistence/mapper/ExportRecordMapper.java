package com.company.governance.infrastructure.persistence.mapper;

import com.company.governance.domain.trace.entity.ExportRecord;

public interface ExportRecordMapper {

    ExportRecord selectById(String exportId);

    int insert(ExportRecord exportRecord);
}
