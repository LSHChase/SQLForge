package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;

/**
 * 当前确定性同步执行路径的基础设施抽象。
 */
public interface QueryExecutionAdapter {

    QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                               String actualSql,
                               QueryExecuteRequest request,
                               boolean degradedPath);
}
