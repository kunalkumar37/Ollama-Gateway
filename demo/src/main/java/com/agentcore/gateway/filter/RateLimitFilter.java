package com.agentcore.gateway.filter;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.agentcore.gateway.service.SystemMonitorService;
import com.agentcore.gateway.model.ApiKeyInfo;
import com.agentcore.gateway.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimiterService rateLimiterService;
    private final SystemMonitorService  monitorService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
        String path = request.getRequestURI();
        if(!path.startsWith("/v1/")){
            filterChain.doFilter(request,response);
            return;
        }

        //Read the key info that ApiKeyAuthFilter stored
        ApiKeyInfo keyInfo = (ApiKeyInfo) request.getAttribute(ApiKeyAuthFilter.API_KEY_ATTR);

        if(keyInfo == null){
            filterChain.doFilter(request,response);
            return;
        }

        RateLimiterService.RateLimitResult result = rateLimiterService.tryConsume(keyInfo);

        if(!result.allowed()){
            monitorService.onRequestRejected();
            sendRateLimitError(response, result);
            return;
        }

        monitorService.onRequestStart();
        try{
            filterChain.doFilter(request, response);
        }
        finally{
            rateLimiterService.release(keyInfo.getKey());
            monitorService.onRequestEnd();
        }
    }

    private void sendRateLimitError(HttpServletResponse response, RateLimiterService.RateLimitResult result) throws IOException{
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        //Standard header - tells client when to retry
        response.setHeader("Retry-After",String.valueOf((int) Math.ceil(result.retryAfterSeconds())));

        response.setHeader("X-RateLimit-Limit",String.valueOf(result.limit()));

        response.setHeader("X-RateLimit-Remaining","0");

         response.getWriter().write(objectMapper.writeValueAsString(Map.of(
            "error", Map.of(
                "message", String.format(
                    "Rate limit exceeded. Retry after %.2f seconds.",
                    result.retryAfterSeconds()),
                "type", "rate_limit_error",
                "code", 429,
                "retry_after_seconds", result.retryAfterSeconds()
            )
        )));

    }

    

}
