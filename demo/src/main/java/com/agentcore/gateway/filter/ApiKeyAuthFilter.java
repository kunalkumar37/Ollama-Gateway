package com.agentcore.gateway.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.agentcore.gateway.service.ApiKeyService;
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
public class ApiKeyAuthFilter extends OncePerRequestFilter{
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    
    public static final String API_KEY_ATTR ="authenticated_api_key";

    @Override   
    protected void doFilterInternal(HttpServletRequest request, HttpServletRequest response, FilterChain filterChain)
            
    
    
    
}
