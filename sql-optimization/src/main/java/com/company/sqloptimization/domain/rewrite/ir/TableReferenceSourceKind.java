package com.company.sqloptimization.domain.rewrite.ir;

public enum TableReferenceSourceKind {
    PHYSICAL_TABLE,
    CTE,
    DERIVED_TABLE,
    VIEW,
    UNKNOWN
}
