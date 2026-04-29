package com.agentcore.gateway.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ApiKeyInfo {
    private String key;
    private String label;
    private boolean active;

    @Builder.Default
    private AtomicLong totalRequests = new AtomicLong(0);
    
    private Instant createdAt;
    private Instant lastUsedAt;

    private Integer requestPerMinuteOverride;

    public void incrementRequests(){
        totalRequests.incrementAndGet();
        lastUsedAt=Instant.now();
    }
}
