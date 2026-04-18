package com.company.governance.application.interceptor;

import com.company.governance.common.context.RequestContext;
import com.company.governance.common.context.TenantContext;
import com.company.governance.common.exception.UnauthorizedException;
import com.company.governance.config.AuthProperties;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String USER_TOKEN_HEADER = "X-User-Token";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final AuthProperties authProperties;

    public AuthInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String traceId = resolveTraceId(request);
            String tenantId = request.getHeader(TENANT_HEADER);
            String userToken = request.getHeader(USER_TOKEN_HEADER);

            RequestContext.set(traceId, userToken);
            response.setHeader(TRACE_ID_HEADER, traceId);

            if (StringUtils.hasText(tenantId)) {
                TenantContext.set(tenantId);
            } else if (authProperties.isEnabled()) {
                throw new UnauthorizedException("Missing X-Tenant-Id header");
            } else {
                log.warn("Missing X-Tenant-Id header, traceId={}, uri={}", traceId, request.getRequestURI());
            }

            validateToken(traceId, userToken, request.getRequestURI());

            log.info("Resolved request context, traceId={}, tenantId={}, uri={}",
                traceId,
                StringUtils.hasText(tenantId) ? tenantId : "UNKNOWN",
                request.getRequestURI());
            return true;
        } catch (RuntimeException ex) {
            TenantContext.clear();
            RequestContext.clear();
            throw ex;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
        RequestContext.clear();
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        return StringUtils.hasText(traceId) ? traceId : UUID.randomUUID().toString();
    }

    private void validateToken(String traceId, String userToken, String requestUri) {
        if (!StringUtils.hasText(userToken)) {
            if (authProperties.isEnabled()) {
                throw new UnauthorizedException("Missing X-User-Token header");
            }
            log.warn("Missing X-User-Token header, traceId={}, uri={}", traceId, requestUri);
            return;
        }

        List<String> validTokens = authProperties.getValidTokens();
        if (!CollectionUtils.isEmpty(validTokens) && !validTokens.contains(userToken)) {
            throw new UnauthorizedException("Invalid X-User-Token");
        }

        log.info("Token校验预留, traceId={}, uri={}", traceId, requestUri);
    }
}
