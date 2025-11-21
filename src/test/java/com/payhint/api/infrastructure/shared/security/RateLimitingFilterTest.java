package com.payhint.api.infrastructure.shared.security;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payhint.api.infrastructure.shared.configuration.RateLimitingProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Unit Tests")
class RateLimitingFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private RateLimitingProperties rateLimitingProperties;
    private RateLimitingFilter rateLimitingFilter;
    private ObjectMapper objectMapper;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        rateLimitingProperties = new RateLimitingProperties();
        rateLimitingProperties.setEnabled(true);

        RateLimitingProperties.EndpointConfig loginConfig = new RateLimitingProperties.EndpointConfig(3, 3,
                Duration.ofSeconds(60));
        RateLimitingProperties.EndpointConfig registerConfig = new RateLimitingProperties.EndpointConfig(2, 2,
                Duration.ofSeconds(3600));

        RateLimitingProperties.AuthEndpoints authEndpoints = new RateLimitingProperties.AuthEndpoints();
        authEndpoints.setLogin(loginConfig);
        authEndpoints.setRegister(registerConfig);
        rateLimitingProperties.setAuthEndpoints(authEndpoints);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        rateLimitingFilter = new RateLimitingFilter(rateLimitingProperties, objectMapper);
    }

    @Test
    @DisplayName("Should allow request when rate limit is disabled")
    void shouldAllowRequestWhenRateLimitDisabled() throws Exception {
        rateLimitingProperties.setEnabled(false);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow request for non-auth endpoints")
    void shouldAllowRequestForNonAuthEndpoints() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/customers");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should allow login requests within rate limit")
    void shouldAllowLoginRequestsWithinRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block login request when rate limit exceeded")
    void shouldBlockLoginRequestWhenRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    @DisplayName("Should allow register requests within rate limit")
    void shouldAllowRegisterRequestsWithinRateLimit() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should block register request when rate limit exceeded")
    void shouldBlockRegisterRequestWhenRateLimitExceeded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        verify(response).setHeader("Retry-After", "3600");
    }

    @Test
    @DisplayName("Should track rate limits per IP address independently")
    void shouldTrackRateLimitsPerIpIndependently() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        when(request.getRemoteAddr()).thenReturn("127.0.0.2");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(4)).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedForHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 198.51.100.1");
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP header when X-Forwarded-For is absent")
    void shouldExtractIpFromXRealIpHeader() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.5");
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    @DisplayName("Should use remote address when proxy headers are absent")
    void shouldUseRemoteAddressWhenProxyHeadersAbsent() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("203.0.113.10");
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(3)).doFilter(request, response);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setHeader("Retry-After", "60");
    }

    @Test
    @DisplayName("Should not interfere with login and register rate limits")
    void shouldNotInterfereWithLoginAndRegisterRateLimits() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(request.getRequestURI()).thenReturn("/api/auth/login");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        when(request.getRequestURI()).thenReturn("/api/auth/register");
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(5)).doFilter(request, response);
    }
}
