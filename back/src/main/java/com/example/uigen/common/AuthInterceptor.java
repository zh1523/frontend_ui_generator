package com.example.uigen.common;

import com.example.uigen.model.entity.AppUser;
import com.example.uigen.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        if (isPublicEndpoint(uri)) {
            return true;
        }
        String token = request.getHeader(HttpHeadersConst.AUTH_TOKEN);
        AppUser user = authService.requireUserByToken(token);
        AuthContextHolder.setUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/v1/auth/") || uri.startsWith("/api/v1/workspaces/anonymous");
    }
}
