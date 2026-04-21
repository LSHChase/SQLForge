package com.company.sqlforge.common.context;

/**
 * Tenant-focused view over the shared request context.
 */
public final class TenantContext {

    private TenantContext() {
    }

    public static String get() {
        return RequestContext.getTenantId();
    }

    public static void clear() {
        RequestContext.clear();
    }
}
