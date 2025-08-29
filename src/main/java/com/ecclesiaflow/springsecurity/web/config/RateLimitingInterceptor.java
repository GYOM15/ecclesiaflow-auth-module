package com.ecclesiaflow.springsecurity.web.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Limite : 5 tentatives par minute par IP
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key != null ? key : "unknown", k -> createNewBucket());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIP(request);
        String requestURI = request.getRequestURI();
        if (requestURI == null) {
            return true;
        }
        // Appliquer le rate limiting seulement sur les endpoints sensibles
        if (requestURI.contains("/ecclesiaflow/auth/") || requestURI.contains("/ecclesiaflow/members/signup")) {
            Bucket bucket = resolveBucket(clientIp);
            if (bucket.tryConsume(1)) {
                return true;
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Trop de tentatives. Veuillez réessayer plus tard.\"}");
                return false;
            }
        }
        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            // Gérer le cas où l'adresse IP est null
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr != null ? remoteAddr : "unknown";
        }
        return xfHeader.split(",")[0];
    }
}