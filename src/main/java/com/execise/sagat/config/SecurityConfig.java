package com.execise.sagat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
public class SecurityConfig extends OncePerRequestFilter {
    private final String apiKey;
    
    public SecurityConfig(@Value("${notification.api-key:change-me}") String apiKey) { this.apiKey = apiKey; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/notifications") &&
                !apiKey.equals(request.getHeader("X-API-Key"))) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "X-API-Key invalida o ausente"); return;
        }
        chain.doFilter(request, response);
    }
}
