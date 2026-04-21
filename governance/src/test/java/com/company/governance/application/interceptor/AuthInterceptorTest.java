package com.company.governance.application.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.governance.config.AuthProperties;
import com.company.sqlforge.common.config.AuthSourceConstants;
import com.company.sqlforge.common.config.RequestHeaderConstants;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.context.TenantContext;
import com.company.sqlforge.common.exception.UnauthorizedException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthInterceptorTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestContext.clear();
    }

    @Test
    void shouldPopulateAndClearThreadLocalContexts() throws Exception {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setEnabled(false);
        authProperties.setTrustedAuthSources(Collections.emptyList());
        AuthInterceptor authInterceptor = new AuthInterceptor(authProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        addProtectedHeaders(request);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(authInterceptor.preHandle(request, response, new Object()));
        assertEquals("system", TenantContext.get());
        assertEquals("operator-001", RequestContext.getUserId());
        assertNotNull(RequestContext.getTraceId());
        assertEquals(RequestContext.getTraceId(), response.getHeader(RequestHeaderConstants.TRACE_ID));
        assertEquals(RequestContext.getRequestId(), response.getHeader(RequestHeaderConstants.REQUEST_ID));
        assertEquals(Arrays.asList("TENANT_ADMIN", "OPERATOR"), RequestContext.getRoleCodes());

        authInterceptor.afterCompletion(request, response, new Object(), null);

        assertNull(TenantContext.get());
        assertNull(RequestContext.getTraceId());
        assertNull(RequestContext.getUserId());
    }

    @Test
    void shouldClearContextsWhenStrictAuthFails() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setEnabled(true);
        authProperties.setTrustedAuthSources(Collections.singletonList(AuthSourceConstants.GATEWAY));
        AuthInterceptor authInterceptor = new AuthInterceptor(authProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        addProtectedHeaders(request);
        MockHttpServletResponse response = new MockHttpServletResponse();

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> {
                request.removeHeader(RequestHeaderConstants.TENANT_ID);
                authInterceptor.preHandle(request, response, new Object());
            }
        );

        assertEquals(ErrorCodeConstants.SYSTEM_UNAUTHORIZED, ex.getCode());
        assertNull(TenantContext.get());
        assertNull(RequestContext.getTraceId());
        assertNull(RequestContext.getUserId());
    }

    @Test
    void shouldRejectExpiredAuthenticationContextWhenAuthEnabled() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setEnabled(true);
        authProperties.setTrustedAuthSources(Collections.singletonList(AuthSourceConstants.HEADER));
        AuthInterceptor authInterceptor = new AuthInterceptor(authProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        addProtectedHeaders(request);
        request.removeHeader(RequestHeaderConstants.EXPIRES_AT);
        request.addHeader(RequestHeaderConstants.EXPIRES_AT, String.valueOf(System.currentTimeMillis() - 1000L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> authInterceptor.preHandle(request, response, new Object())
        );

        assertEquals(ErrorCodeConstants.SYSTEM_UNAUTHORIZED, ex.getCode());
    }

    private void addProtectedHeaders(MockHttpServletRequest request) {
        long now = System.currentTimeMillis();
        request.addHeader(RequestHeaderConstants.TENANT_ID, "system");
        request.addHeader(RequestHeaderConstants.USER_ID, "operator-001");
        request.addHeader(RequestHeaderConstants.ROLE_CODES, "TENANT_ADMIN,OPERATOR");
        request.addHeader(RequestHeaderConstants.REQUEST_ID, "request-001");
        request.addHeader(RequestHeaderConstants.TRACE_ID, "trace-001");
        request.addHeader(RequestHeaderConstants.AUTH_SOURCE, AuthSourceConstants.HEADER);
        request.addHeader(RequestHeaderConstants.ISSUED_AT, String.valueOf(now - 1000L));
        request.addHeader(RequestHeaderConstants.EXPIRES_AT, String.valueOf(now + 60000L));
    }
}
