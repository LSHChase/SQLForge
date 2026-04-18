package com.company.governance.application.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.common.constants.ErrorCodeConstants;
import com.company.governance.common.context.RequestContext;
import com.company.governance.common.context.TenantContext;
import com.company.governance.common.exception.UnauthorizedException;
import com.company.governance.config.AuthProperties;
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
        authProperties.setValidTokens(Collections.emptyList());
        AuthInterceptor authInterceptor = new AuthInterceptor(authProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        request.addHeader("X-Tenant-Id", "system");
        request.addHeader("X-User-Token", "dev-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(authInterceptor.preHandle(request, response, new Object()));
        assertEquals("system", TenantContext.get());
        assertEquals("dev-token", RequestContext.getUserIdentity());
        assertNotNull(RequestContext.getTraceId());
        assertEquals(RequestContext.getTraceId(), response.getHeader("X-Trace-Id"));

        authInterceptor.afterCompletion(request, response, new Object(), null);

        assertNull(TenantContext.get());
        assertNull(RequestContext.getTraceId());
        assertNull(RequestContext.getUserIdentity());
    }

    @Test
    void shouldClearContextsWhenStrictAuthFails() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setEnabled(true);
        authProperties.setValidTokens(Collections.singletonList("strict-token"));
        AuthInterceptor authInterceptor = new AuthInterceptor(authProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/governance/tenant-config");
        request.addHeader("X-User-Token", "strict-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UnauthorizedException ex = assertThrows(
            UnauthorizedException.class,
            () -> authInterceptor.preHandle(request, response, new Object())
        );

        assertEquals(ErrorCodeConstants.SYSTEM_UNAUTHORIZED, ex.getCode());
        assertNull(TenantContext.get());
        assertNull(RequestContext.getTraceId());
        assertNull(RequestContext.getUserIdentity());
    }
}
