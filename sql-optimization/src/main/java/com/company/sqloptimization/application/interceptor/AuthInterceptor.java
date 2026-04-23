package com.company.sqloptimization.application.interceptor;

import com.company.sqloptimization.application.context.RequestMetadataContext;
import com.company.sqloptimization.config.AuthProperties;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.UnauthorizedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthProperties authProperties;

    public AuthInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
        validateAuthSource(authSource);
        validateNotExpired(expiresAt);

        RequestContext.set(tenantId, userId, roleCodes, requestId, traceId, authSource, issuedAt, expiresAt);
        RequestMetadataContext.set(resolveSourceIp(request), resolveUserAgent(request));
        response.setHeader(RequestHeaderConstants.REQUEST_ID, requestId);
        response.setHeader(RequestHeaderConstants.TRACE_ID, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        RequestContext.clear();
        RequestMetadataContext.clear();
    }

    private String requireHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.trim().isEmpty()) {
            throw new UnauthorizedException("Missing " + headerName + " header");
        }
        return headerValue.trim();
    }

    private List<String> parseRoleCodes(String rawRoleCodes) {
        List<String> roleCodes = Arrays.stream(rawRoleCodes.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roleCodes)) {
            throw new UnauthorizedException("Missing " + RequestHeaderConstants.ROLE_CODES + " header");
        }
        return new ArrayList<String>(roleCodes);
    }

    private long parseEpochMilli(HttpServletRequest request, String headerName) {
        String value = requireHeader(request, headerName);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid " + headerName + " header");
        }
    }

    private void validateTimeWindow(long issuedAt, long expiresAt) {
        if (issuedAt <= 0L || expiresAt <= 0L || issuedAt > expiresAt) {
            throw new UnauthorizedException("Invalid authentication time window");
        }
    }

    private void validateAuthSource(String authSource) {
        if (!authProperties.isEnabled()) {
            return;
        }
        List<String> trustedAuthSources = authProperties.getTrustedAuthSources();
        if (!CollectionUtils.isEmpty(trustedAuthSources) && !trustedAuthSources.contains(authSource)) {
            throw new UnauthorizedException("Unsupported " + RequestHeaderConstants.AUTH_SOURCE + " header");
        }
    }

    private void validateNotExpired(long expiresAt) {
        if (authProperties.isEnabled() && expiresAt < System.currentTimeMillis()) {
            throw new UnauthorizedException("Authentication context has expired");
        }
    }

    private String resolveSourceIp(HttpServletRequest request) {
        return request == null ? "UNKNOWN" : request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request == null ? null : request.getHeader("User-Agent");
        return userAgent == null || userAgent.trim().isEmpty() ? "UNKNOWN" : userAgent.trim();
    }
}
