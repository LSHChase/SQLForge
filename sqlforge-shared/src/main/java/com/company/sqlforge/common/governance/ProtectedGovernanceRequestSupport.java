package com.company.sqlforge.common.governance;

import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

public final class ProtectedGovernanceRequestSupport {

    private ProtectedGovernanceRequestSupport() {
    }

    public static HttpHeaders buildProtectedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(RequestHeaderConstants.TENANT_ID, requiredContextValue(RequestContext.getTenantId(), "tenantId"));
        headers.set(RequestHeaderConstants.USER_ID, requiredContextValue(RequestContext.getUserId(), "userId"));
        headers.set(RequestHeaderConstants.ROLE_CODES, String.join(",", RequestContext.getRoleCodes()));
        headers.set(RequestHeaderConstants.REQUEST_ID, requiredContextValue(RequestContext.getRequestId(), "requestId"));
        headers.set(RequestHeaderConstants.TRACE_ID, requiredContextValue(RequestContext.getTraceId(), "traceId"));
        headers.set(RequestHeaderConstants.AUTH_SOURCE, requiredContextValue(RequestContext.getAuthSource(), "authSource"));
        headers.set(RequestHeaderConstants.ISSUED_AT, String.valueOf(RequestContext.getIssuedAt()));
        headers.set(RequestHeaderConstants.EXPIRES_AT, String.valueOf(RequestContext.getExpiresAt()));
        return headers;
    }

    public static String resolveSourceIp(String fallback) {
        return resolveMetadata(RequestMetadataContext.getSourceIp(), fallback);
    }

    public static String resolveUserAgent(String fallback) {
        return resolveMetadata(RequestMetadataContext.getUserAgent(), fallback);
    }

    public static String requiredContextValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "Missing protected request context field: " + fieldName
            );
        }
        return value;
    }

    public static String resolveMetadata(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
