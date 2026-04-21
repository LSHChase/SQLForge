package com.company.queryexecution.domain.query;

/**
 * Runtime acceleration preference requested by the caller.
 */
public enum AccelerationPreference {
    PREFER_ACCELERATED,
    PREFER_FRESH,
    NONE
}
