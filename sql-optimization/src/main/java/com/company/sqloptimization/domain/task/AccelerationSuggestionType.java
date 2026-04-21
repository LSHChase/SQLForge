package com.company.sqloptimization.domain.task;

/**
 * Supported acceleration-suggestion categories.
 */
public enum AccelerationSuggestionType {
    PRECOMPUTE,
    PARTITION,
    BUCKET,
    SPLIT,
    REPLACE,
    ALL
}
