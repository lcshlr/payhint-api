package com.payhint.api.infrastructure.shared.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "application.rate-limiting")
public class RateLimitingProperties {

    private boolean enabled = true;
    private AuthEndpoints authEndpoints = new AuthEndpoints();

    @Data
    public static class AuthEndpoints {
        private EndpointConfig login = new EndpointConfig(5, 5, Duration.ofSeconds(60));
        private EndpointConfig register = new EndpointConfig(3, 3, Duration.ofSeconds(3600));
    }

    @Data
    public static class EndpointConfig {
        private long capacity;
        private long refillTokens;
        private Duration refillDuration;

        public EndpointConfig() {
        }

        public EndpointConfig(long capacity, long refillTokens, Duration refillDuration) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillDuration = refillDuration;
        }
    }
}
