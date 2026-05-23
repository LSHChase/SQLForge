package com.company.sqlforge.common.access;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import org.springframework.util.StringUtils;

public final class AccessAuditContract {

    private final AccessChannel accessChannel;
    private final String authSource;
    private final String tenantId;
    private final String userId;
    private final String requestId;
    private final String traceId;
    private final String sourceIp;
    private final String userAgent;

    public AccessAuditContract(AccessChannel accessChannel,
                               String authSource,
                               String tenantId,
                               String userId,
                               String requestId,
                               String traceId,
                               String sourceIp,
                               String userAgent) {
        this.accessChannel = accessChannel;
        this.authSource = authSource;
        this.tenantId = tenantId;
        this.userId = userId;
        this.requestId = requestId;
        this.traceId = traceId;
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
    }

    public static AccessAuditContract capture() {
        AccessChannel channel = AccessChannel.fromWireValue(RequestMetadataContext.getAccessChannel());
        if (channel == null) {
            channel = AccessChannel.API;
        }
        return new AccessAuditContract(
            channel,
            trimToNull(RequestContext.getAuthSource()),
            trimToNull(RequestContext.getTenantId()),
            trimToNull(RequestContext.getUserId()),
            trimToNull(RequestContext.getRequestId()),
            trimToNull(RequestContext.getTraceId()),
            trimToNull(RequestMetadataContext.getSourceIp()),
            trimToNull(RequestMetadataContext.getUserAgent())
        );
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public AccessChannel getAccessChannel() {
        return accessChannel;
    }

    public String getAuthSource() {
        return authSource;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }
}
