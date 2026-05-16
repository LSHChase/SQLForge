package com.company.sqlforge.common.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 所有后端服务共享的线程本地请求上下文。
 */
public final class RequestContext {

    private static final ThreadLocal<ContextValue> HOLDER = new ThreadLocal<ContextValue>();

    private RequestContext() {
    }

    public static void set(String tenantId,
                           String userId,
                           List<String> roleCodes,
                           String requestId,
                           String traceId,
                           String authSource,
                           long issuedAt,
                           long expiresAt) {
        HOLDER.set(new ContextValue(
            tenantId,
            userId,
            roleCodes,
            requestId,
            traceId,
            authSource,
            issuedAt,
            expiresAt
        ));
    }

    public static void set(ContextValue contextValue) {
        HOLDER.set(contextValue);
    }

    public static ContextValue get() {
        return HOLDER.get();
    }

    public static ContextValue snapshot() {
        ContextValue current = HOLDER.get();
        return current == null ? null : new ContextValue(
            current.getTenantId(),
            current.getUserId(),
            current.getRoleCodes(),
            current.getRequestId(),
            current.getTraceId(),
            current.getAuthSource(),
            current.getIssuedAt(),
            current.getExpiresAt()
        );
    }

    public static void restore(ContextValue contextValue) {
        if (contextValue == null) {
            clear();
            return;
        }
        HOLDER.set(contextValue);
    }

    public static String getTenantId() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? null : contextValue.getTenantId();
    }

    public static String getUserId() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? null : contextValue.getUserId();
    }

    public static List<String> getRoleCodes() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? Collections.<String>emptyList() : contextValue.getRoleCodes();
    }

    public static String getRequestId() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? null : contextValue.getRequestId();
    }

    public static String getTraceId() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? null : contextValue.getTraceId();
    }

    public static String getAuthSource() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? null : contextValue.getAuthSource();
    }

    public static long getIssuedAt() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? 0L : contextValue.getIssuedAt();
    }

    public static long getExpiresAt() {
        ContextValue contextValue = HOLDER.get();
        return contextValue == null ? 0L : contextValue.getExpiresAt();
    }

    public static boolean hasRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return false;
        }
        List<String> roleCodes = getRoleCodes();
        return !CollectionUtils.isEmpty(roleCodes) && roleCodes.contains(roleCode);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static final class ContextValue {

        private final String tenantId;
        private final String userId;
        private final List<String> roleCodes;
        private final String requestId;
        private final String traceId;
        private final String authSource;
        private final long issuedAt;
        private final long expiresAt;

        public ContextValue(String tenantId,
                            String userId,
                            List<String> roleCodes,
                            String requestId,
                            String traceId,
                            String authSource,
                            long issuedAt,
                            long expiresAt) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.roleCodes = Collections.unmodifiableList(new ArrayList<String>(
                roleCodes == null ? Collections.<String>emptyList() : roleCodes
            ));
            this.requestId = requestId;
            this.traceId = traceId;
            this.authSource = authSource;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getUserId() {
            return userId;
        }

        public List<String> getRoleCodes() {
            return roleCodes;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getTraceId() {
            return traceId;
        }

        public String getAuthSource() {
            return authSource;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }
}
