package com.sqlforge.backend.service;

import com.sqlforge.backend.model.SqlStructureAst;

public interface SqlParserAdapter {

    SqlStructureAst parse(String normalizedSql);
}
