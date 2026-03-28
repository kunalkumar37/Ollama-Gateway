package com.agentcore.gateway.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")

public class GatewayProperties {
    private String ollamaBaseurl ="http://localhost:11434";
    private String adminSecretKey ="spiderman";

    private Map<String, String> apiKeys = new HashMap<>();
    private RateLimitProps rateLimit = new RateLimitProps();
    private MonitoringProps monitoring = new MonitoringProps();


    @Data
    public static class RateLimitProps{
        private boolean enabled =false;
        private int defaultRequestsPerMinute = 60;
        private int defaultMaxConcurrent = 3;
        private Map<String, Integer> perKeyLimits = new HashMap<>();

    }

    @Data
    public static class MonitoringProps{
        private boolean gpuEnabled = false;
        
    }
}
