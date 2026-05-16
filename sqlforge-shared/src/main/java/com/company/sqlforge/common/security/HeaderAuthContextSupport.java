package com.company.sqlforge.common.security;

import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.UnauthorizedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.util.CollectionUtils;

public class HeaderAuthContextSupport {

    public void establish(HttpServletRequest request,
                          HttpServletResponse response,
                          boolean authEnabled,
                          List<String> trustedAuthSources) {
        String requestId = requireHeader(request, RequestHeaderConstants.REQUEST_ID);
        String traceId = requireHeader(request, RequestHeaderConstants.TRACE_ID);
        request.setAttribute(RequestHeaderConstants.REQUEST_ID, requestId);
        request.setAttribute(RequestHeaderConstants.TRACE_ID, traceId);

        String tenantId = requireHeader(request, RequestHeaderConstants.TENANT_ID);
        String userId = requireHeader(request, RequestHeaderConstants.USER_ID);
        List<String> roleCodes = parseRoleCodes(requireHeader(request, RequestHeaderConstants.ROLE_CODES));
        String authSource = requireHeader(request, RequestHeaderConstants.AUTH_SOURCE);
        long issuedAt = parseEpochMilli(request, RequestHeaderConstants.ISSUED_AT);
        long expiresAt = parseEpochMilli(request, RequestHeaderConstants.EXPIRES_AT);

        validateTimeWindow(issuedAt, expiresAt);
        validateAuthSource(authEnabled, trustedAuthSources, authSource);
        validateNotExpired(authEnabled, expiresAt);

        RequestContext.set(tenantId, userId, roleCodes, requestId, traceId, authSource, issuedAt, expiresAt);
        RequestMetadataContext.set(
            resolveSourceIp(request),
            resolveUserAgent(request),
            resolveAccessChannel(request)
        );
        response.setHeader(RequestHeaderConstants.REQUEST_ID, requestId);
        response.setHeader(RequestHeaderConstants.TRACE_ID, traceId);
    }

    public void clear() {
        RequestContext.clear();
        RequestMetadataContext.clear();
    }

    private String requireHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.trim().isEmpty()) {
            throw new UnauthorizedException("缺少 " + headerName + " 请求头");
        }
        return headerValue.trim();
    }

    private List<String> parseRoleCodes(String rawRoleCodes) {
        List<String> roleCodes = Arrays.stream(rawRoleCodes.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roleCodes)) {
            throw new UnauthorizedException("缺少 " + RequestHeaderConstants.ROLE_CODES + " 请求头");
        }
        return new ArrayList<String>(roleCodes);
    }

    private long parseEpochMilli(HttpServletRequest request, String headerName) {
        String value = requireHeader(request, headerName);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("无效的 " + headerName + " 请求头");
        }
    }

    private void validateTimeWindow(long issuedAt, long expiresAt) {
        if (issuedAt <= 0L || expiresAt <= 0L || issuedAt > expiresAt) {
            throw new UnauthorizedException("认证时间窗口无效");
        }
    }

    private void validateAuthSource(boolean authEnabled, List<String> trustedAuthSources, String authSource) {
        if (!authEnabled) {
            return;
        }
        if (!CollectionUtils.isEmpty(trustedAuthSources) && !trustedAuthSources.contains(authSource)) {
            throw new UnauthorizedException("不支持的 " + RequestHeaderConstants.AUTH_SOURCE + " 请求头");
        }
    }

    private void validateNotExpired(boolean authEnabled, long expiresAt) {
        if (authEnabled && expiresAt < System.currentTimeMillis()) {
            throw new UnauthorizedException("认证上下文已过期");
        }
    }

    private String resolveSourceIp(HttpServletRequest request) {
        return request == null ? "UNKNOWN" : request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request == null ? null : request.getHeader("User-Agent");
        return userAgent == null || userAgent.trim().isEmpty() ? "UNKNOWN" : userAgent.trim();
    }

    private String resolveAccessChannel(HttpServletRequest request) {
        String accessChannel = request == null ? null : request.getHeader(RequestHeaderConstants.ACCESS_CHANNEL);
        return accessChannel == null || accessChannel.trim().isEmpty() ? null : accessChannel.trim();
    }
}
