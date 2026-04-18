package com.company.common.audit;

public final class AuditContext {

    private static final ThreadLocal<AuditContextHolder> HOLDER = new ThreadLocal<AuditContextHolder>();

    private AuditContext() {
    }

    public static void set(String tenantId, String operator, String traceId) {
        HOLDER.set(new AuditContextHolder(tenantId, operator, traceId));
    }

    public static AuditContextHolder get() {
        return HOLDER.get();
    }

    public static AuditContextHolder snapshot() {
        AuditContextHolder current = HOLDER.get();
        return current == null ? null : new AuditContextHolder(current.getTenantId(), current.getOperator(), current.getTraceId());
    }

    public static void restore(AuditContextHolder holder) {
        if (holder == null) {
            clear();
            return;
        }
        HOLDER.set(new AuditContextHolder(holder.getTenantId(), holder.getOperator(), holder.getTraceId()));
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static final class AuditContextHolder {

        private final String tenantId;
        private final String operator;
        private final String traceId;

        public AuditContextHolder(String tenantId, String operator, String traceId) {
            this.tenantId = tenantId;
            this.operator = operator;
            this.traceId = traceId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getOperator() {
            return operator;
        }

        public String getTraceId() {
            return traceId;
        }
    }
}
