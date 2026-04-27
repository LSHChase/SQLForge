package com.company.sqlforge.common.openaccess;

import com.company.sqlforge.common.access.AccessChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpenAccessRequestContext {

    private final String tenantId;
    private final String userId;
    private final List<String> roleCodes;
    private final String requestId;
    private final String traceId;
    private final String authSource;
    private final long issuedAt;
    private final long expiresAt;
    private final String sourceIp;
    private final String userAgent;
    private final AccessChannel accessChannel;

    public OpenAccessRequestContext(String tenantId,
                                    String userId,
                                    List<String> roleCodes,
                                    String requestId,
                                    String traceId,
                                    String authSource,
                                    long issuedAt,
                                    long expiresAt,
                                    String sourceIp,
                                    String userAgent,
                                    AccessChannel accessChannel) {
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
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
        this.accessChannel = accessChannel;
    }

    public OpenAccessRequestContext withAccessChannel(AccessChannel channel) {
        return new OpenAccessRequestContext(
            tenantId,
            userId,
            roleCodes,
            requestId,
            traceId,
            authSource,
            issuedAt,
            expiresAt,
            sourceIp,
            userAgent,
            channel
        );
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

    public String getSourceIp() {
        return sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public AccessChannel getAccessChannel() {
        return accessChannel;
    }
}
