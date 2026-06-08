package com.scrolldoom.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, RateLimitEntry> cache = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        String key = clientIp + ":" + path;

        int maxRequests;
        long windowMs = 600_000;

        if (path.contains("/login-with-password") || path.contains("/register")) {
            maxRequests = 5;
        } else {
            maxRequests = 10;
        }

        long now = System.currentTimeMillis();
        RateLimitEntry entry = cache.compute(key, (k, v) -> {
            if (v == null || (now - v.startTime) > windowMs) {
                return new RateLimitEntry(1, now);
            }
            v.count++;
            return v;
        });

        if (entry.count > maxRequests) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> (now - entry.getValue().startTime) > 600_000);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        int count;
        final long startTime;

        RateLimitEntry(int count, long startTime) {
            this.count = count;
            this.startTime = startTime;
        }
    }
}
