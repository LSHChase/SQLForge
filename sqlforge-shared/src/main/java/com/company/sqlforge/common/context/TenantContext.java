package com.company.sqlforge.common.context;

/**
 * 共享请求上下文的租户视图。
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
