package com.company.governance.common.context;

public final class RequestContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_IDENTITY = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String traceId, String userIdentity) {
        setTraceId(traceId);
        setUserIdentity(userIdentity);
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void setUserIdentity(String userIdentity) {
        USER_IDENTITY.set(userIdentity);
    }

    public static String getUserIdentity() {
        return USER_IDENTITY.get();
    }

    public static void clear() {
        TRACE_ID.remove();
        USER_IDENTITY.remove();
    }
}
