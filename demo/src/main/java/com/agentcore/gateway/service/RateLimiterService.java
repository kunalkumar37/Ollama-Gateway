package com.agentcore.gateway.service;

import com.agentcore.gateway.config.GatewayProperties;
import com.agentcore.gateway.model.ApiKeyInfo;
import com.agentcore.gateway.model.TokenBucket;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final GatewayProperties props;
    
    private final Map<String,TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitResult tryConsume(ApiKeyInfo keyInfo){
        if(!props.getRateLimit().isEnabled()){
            return RateLimitResult.allowed();
        }

        TokenBucket bucket = getOrCreateBucket(keyInfo);
        boolean allowed = bucket.tryConsume();
        if(allowed){
            return RateLimitResult.allowed();

        }
        double retryAfter = bucket.getRetryAfterSeconds();
        log.warn("Rate limited: [{}] retryAfter={}s",
            ApiKeyService.maskKey(keyInfo.getKey()),
            String.format("%.2f",retryAfter)
        );
        return RateLimitResult.rejected(retryAfter,bucket.getMaxTokens());

    }


    public void release(String apiKey){
        TokenBucket bucket = buckets.get(apiKey);
        if(bucket!=null){
            bucket.release();
        }
    }

    private TokenBucket getOrCreateBucket(ApiKeyInfo keyInfo){
        return buckets.computeIfAbsent(keyInfo.getKey(),k->{
            int rpm = keyInfo.getRequestPerMinuteOverride()!=null
                ? keyInfo.getRequestPerMinuteOverride()
                : props.getRateLimit().getDefaultRequestsPerMinute();

                int maxConcurrent = props.getRateLimit().getDefaultMaxConcurrent();

                log.info("Creating bucket for [{}]: {}/min, {} concurrent",
                    ApiKeyService.maskKey(k),rpm,maxConcurrent
                );

                return new TokenBucket(k,rpm, maxConcurrent);

        });

    }

    public Map<String, BucketStatus> getAllBucketStatus(){
        Map<String,BucketStatus> status = new HashMap<>();
        buckets.forEach((key, bucket)->status.put(key, new BucketStatus(
            bucket.getAvailableTokens(),
            bucket.getMaxTokens(),
            bucket.getCurrentConcurrent().get(),
            bucket.getMaxConcurrent(),
            bucket.getTotalRejected().get()
        )));
        return status;
    }

    public record RateLimitResult(boolean allowed, double retryAfterSeconds, int limit){
        static RateLimitResult allowed(){
            return new RateLimitResult(true,0,0);

        }

        static RateLimitResult rejected(double retryAfter, int limit){
            return new RateLimitResult(false,retryAfter,limit);
        }
    }

    public record BucketStatus(int availableTokens, int maxTokens, int currentConcurrent, int maxConcurrent, long totalRejected){

    }

}
