package com.company.sqlforge.common.openaccess;

import com.company.sqlforge.common.access.AccessChannel;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

public final class OpenAccessHeaderSupport {

    private OpenAccessHeaderSupport() {
    }

    public static HttpHeaders buildHeaders(OpenAccessRequestContext requestContext, AccessChannel defaultChannel) {
        if (requestContext == null) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "缺少开放接入请求上下文"
            );
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(RequestHeaderConstants.TENANT_ID, requiredValue(requestContext.getTenantId(), "tenantId"));
        headers.set(RequestHeaderConstants.USER_ID, requiredValue(requestContext.getUserId(), "userId"));
        headers.set(RequestHeaderConstants.REQUEST_ID, requiredValue(requestContext.getRequestId(), "requestId"));
        headers.set(RequestHeaderConstants.TRACE_ID, requiredValue(requestContext.getTraceId(), "traceId"));
        headers.set(RequestHeaderConstants.AUTH_SOURCE, requiredValue(requestContext.getAuthSource(), "authSource"));
        headers.set(RequestHeaderConstants.ISSUED_AT, String.valueOf(requestContext.getIssuedAt()));
        headers.set(RequestHeaderConstants.EXPIRES_AT, String.valueOf(requestContext.getExpiresAt()));
        AccessChannel accessChannel = requestContext.getAccessChannel() == null ? defaultChannel : requestContext.getAccessChannel();
        if (accessChannel != null) {
            headers.set(RequestHeaderConstants.ACCESS_CHANNEL, accessChannel.name());
        }
        return headers;
    }

    private static String requiredValue(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "开放接入请求上下文缺少字段：" + fieldName
            );
        }
        return value.trim();
    }
}
