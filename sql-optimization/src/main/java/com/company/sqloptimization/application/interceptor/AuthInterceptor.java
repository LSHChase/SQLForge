package com.company.sqloptimization.application.interceptor;

import com.company.sqloptimization.config.AuthProperties;
import com.company.sqlforge.common.security.HeaderAuthContextSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthProperties authProperties;
    private final HeaderAuthContextSupport headerAuthContextSupport = new HeaderAuthContextSupport();

    public AuthInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        headerAuthContextSupport.establish(
            request,
            response,
            authProperties.isEnabled(),
            authProperties.getTrustedAuthSources()
        );
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        headerAuthContextSupport.clear();
    }
}
