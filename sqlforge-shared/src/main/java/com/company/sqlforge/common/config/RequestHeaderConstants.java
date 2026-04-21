package com.company.sqlforge.common.config;

/**
 * Canonical request header names for protected APIs.
 */
public final class RequestHeaderConstants {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String ROLE_CODES = "X-Role-Codes";
    public static final String REQUEST_ID = "X-Request-Id";
    public static final String TRACE_ID = "X-Trace-Id";
    public static final String AUTH_SOURCE = "X-Auth-Source";
    public static final String ISSUED_AT = "X-Issued-At";
    public static final String EXPIRES_AT = "X-Expires-At";

    private RequestHeaderConstants() {
    }
}
