package com.payhint.api.infrastructure.shared.security;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payhint.api.infrastructure.shared.configuration.RateLimitingProperties;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.local.LocalBucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitingProperties rateLimitingProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, LocalBucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, LocalBucket> registerBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitingProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();

        if (requestUri.startsWith("/api/auth/")) {
            String clientIp = getClientIp(request);
            logger.debug("Processing rate limit for IP: {} on endpoint: {}", clientIp, requestUri);

            LocalBucket bucket = null;
            long retryAfterSeconds = 0;

            if (requestUri.equals("/api/auth/login")) {
                bucket = loginBuckets.computeIfAbsent(clientIp,
                        k -> createBucket(rateLimitingProperties.getAuthEndpoints().getLogin()));
                retryAfterSeconds = rateLimitingProperties.getAuthEndpoints().getLogin().getRefillDuration()
                        .getSeconds();
            } else if (requestUri.equals("/api/auth/register")) {
                bucket = registerBuckets.computeIfAbsent(clientIp,
                        k -> createBucket(rateLimitingProperties.getAuthEndpoints().getRegister()));
                retryAfterSeconds = rateLimitingProperties.getAuthEndpoints().getRegister().getRefillDuration()
                        .getSeconds();
            }

            if (bucket != null) {
                if (bucket.tryConsume(1)) {
                    logger.debug("Rate limit check passed for IP: {} on endpoint: {}", clientIp, requestUri);
                    filterChain.doFilter(request, response);
                } else {
                    logger.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
                    handleRateLimitExceeded(request, response, retryAfterSeconds);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void handleRateLimitExceeded(HttpServletRequest request, HttpServletResponse response,
            long retryAfterSeconds) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again later.");
        problemDetail.setTitle("Rate Limit Exceeded");
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("retryAfter", retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }

    private LocalBucket createBucket(RateLimitingProperties.EndpointConfig config) {
        Bandwidth limit = Bandwidth.builder().capacity(config.getCapacity())
                .refillIntervally(config.getRefillTokens(), config.getRefillDuration()).build();

        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    public void clearBuckets() {
        loginBuckets.clear();
        registerBuckets.clear();
    }
}