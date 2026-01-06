package com.isa.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Global filter that logs any unhandled exception (full stack trace) thrown downstream in the filter chain.
 * This ensures that 500 errors triggered during preflight or other filters are visible in the backend console.
 */
@Component
public class ExceptionLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        try {
            filterChain.doFilter(request, response);
        } catch (Throwable t) {
            // Log full stack trace for debugging
            logger.error("Unhandled exception processing request {} {}", request.getMethod(), path, t);
            // rethrow to keep default error handling behavior (so response status etc. remain)
            if (t instanceof ServletException) throw (ServletException) t;
            if (t instanceof IOException) throw (IOException) t;
            throw new ServletException(t);
        }
    }
}

