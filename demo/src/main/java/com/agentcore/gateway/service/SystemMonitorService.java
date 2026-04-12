package com.agentcore.gateway.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SystemMonitorService {
    
    public void onRequestStart() {
        // Monitor request start
    }
    
    public void onRequestEnd() {
        // Monitor request end
    }
    
    public void onRequestRejected() {
        // Monitor rejected request
        log.debug("Request rejected by rate limiter");
    }
}
