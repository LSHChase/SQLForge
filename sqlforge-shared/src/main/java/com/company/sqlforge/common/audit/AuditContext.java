package com.company.sqlforge.common.audit;

/**
 * 跨异步边界传递的线程本地审计事件持有器。
 */
public final class AuditContext {

    private static final ThreadLocal<AuditEvent> HOLDER = new ThreadLocal<AuditEvent>();

    private AuditContext() {
    }

    public static void set(AuditEvent auditEvent) {
        HOLDER.set(auditEvent);
    }

    public static AuditEvent get() {
        return HOLDER.get();
    }

    public static AuditEvent snapshot() {
        return HOLDER.get();
    }

    public static void restore(AuditEvent auditEvent) {
        if (auditEvent == null) {
            clear();
            return;
        }
        HOLDER.set(auditEvent);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
