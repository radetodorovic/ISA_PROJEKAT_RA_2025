package com.isa.backend.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveUserTrackingInterceptor implements HandlerInterceptor {
    private final ActiveUserTracker activeUserTracker;

    public ActiveUserTrackingInterceptor(ActiveUserTracker activeUserTracker) {
        this.activeUserTracker = activeUserTracker;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String principalString && !principalString.isBlank()) {
                activeUserTracker.recordActivity(principalString);
            }
        }
        return true;
    }
}
