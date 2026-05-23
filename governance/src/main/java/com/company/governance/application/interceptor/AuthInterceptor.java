package com.company.governance.application.interceptor;

import com.company.governance.config.AuthProperties;
import com.company.governance.application.service.GovernanceAuditTrailService;
import com.company.sqlforge.common.audit.AuditContext;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.RequestMetadataContext;
import com.company.sqlforge.common.exception.UnauthorizedException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptor.class);

    private final AuthProperties authProperties;
    private final GovernanceAuditTrailService governanceAuditTrailService;

    public AuthInterceptor(AuthProperties authProperties,
                           GovernanceAuditTrailService governanceAuditTrailService) {
        this.authProperties = authProperties;
        this.governanceAuditTrailService = governanceAuditTrailService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String requestId = requireHeader(request, RequestHeaderConstants.REQUEST_ID);
            String traceId = requireHeader(request, RequestHeaderConstants.TRACE_ID);
            request.setAttribute(RequestHeaderConstants.REQUEST_ID, requestId);
            request.setAttribute(RequestHeaderConstants.TRACE_ID, traceId);

            String tenantId = requireHeader(request, RequestHeaderConstants.TENANT_ID);
            String userId = requireHeader(request, RequestHeaderConstants.USER_ID);
            String authSource = requireHeader(request, RequestHeaderConstants.AUTH_SOURCE);
            long issuedAt = parseEpochMilli(request, RequestHeaderConstants.ISSUED_AT);
            long expiresAt = parseEpochMilli(request, RequestHeaderConstants.EXPIRES_AT);

            validateTimeWindow(issuedAt, expiresAt);
            validateAuthSource(authSource);
            validateNotExpired(expiresAt);

            RequestContext.set(tenantId, userId, requestId, traceId, authSource, issuedAt, expiresAt);
            RequestMetadataContext.set(resolveSourceIp(request), resolveUserAgent(request), resolveAccessChannel(request));
            response.setHeader(RequestHeaderConstants.REQUEST_ID, requestId);
            response.setHeader(RequestHeaderConstants.TRACE_ID, traceId);

            LOGGER.info("已解析请求上下文，requestId={}, traceId={}, tenantId={}, userId={}, uri={}",
                requestId,
                traceId,
                tenantId,
                userId,
                request.getRequestURI());
        } catch (RuntimeException ex) {
            try {
                governanceAuditTrailService.recordAuthenticationRejected(request, ex);
            } catch (RuntimeException auditEx) {
                RequestContext.clear();
                RequestMetadataContext.clear();
                AuditContext.clear();
                throw auditEx;
            }
            RequestContext.clear();
            RequestMetadataContext.clear();
            AuditContext.clear();
            throw ex;
        }
        try {
            governanceAuditTrailService.recordAuthenticationAccepted(request);
        } catch (RuntimeException ex) {
            RequestContext.clear();
            RequestMetadataContext.clear();
            AuditContext.clear();
            throw ex;
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           org.springframework.web.servlet.ModelAndView modelAndView) {
        governanceAuditTrailService.recordAuthenticationReleased(request, null);
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        if (ex != null) {
            governanceAuditTrailService.recordAuthenticationReleased(request, ex);
        }
        RequestContext.clear();
        RequestMetadataContext.clear();
        AuditContext.clear();
    }

    private String requireHeader(HttpServletRequest request, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue == null || headerValue.trim().isEmpty()) {
            throw new UnauthorizedException("缺少 " + headerName + " 请求头");
        }
        return headerValue.trim();
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

    private void validateAuthSource(String authSource) {
        if (!authProperties.isEnabled()) {
            return;
        }
        List<String> trustedAuthSources = authProperties.getTrustedAuthSources();
        if (!CollectionUtils.isEmpty(trustedAuthSources) && !trustedAuthSources.contains(authSource)) {
            throw new UnauthorizedException("不支持的 " + RequestHeaderConstants.AUTH_SOURCE + " 请求头");
        }
    }

    private void validateNotExpired(long expiresAt) {
        if (authProperties.isEnabled() && expiresAt < System.currentTimeMillis()) {
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
