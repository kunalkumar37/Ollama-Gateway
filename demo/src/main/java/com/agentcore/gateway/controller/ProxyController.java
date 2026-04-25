package com.agentcore.gateway.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentcore.gateway.service.ApiKeyService;
import com.agentcore.gateway.service.OllamaProxyService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ProxyController {
    private final OllamaProxyService ollamaProxyService;
    private final ApiKeyService apiKeyService;

    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(@RequestBody(required = false) String requestBody, HttpServletRequest request){
        recordRequest(request);
        return proxy("/v1/chat/completions", HttpMethod.POST, requestBody);
        
    }

    @PostMapping("/completions")
    public ResponseEntity<?> completions(@RequestBody(required = false) String requestBody, HttpServletRequest request){
        recordRequest(request);
        return proxy("/v1/completions", HttpMethod.POST, requestBody);

    }

    @PostMapping("/embeddings")
    public ResponseEntity<?> embeddings(@RequestBody(required = false) String requestBody, HttpServletRequest request){
        recordRequest(request);
        return proxy("/v1/embeddings",HttpMethod.POST,requestBody);

    }

    @GetMapping("/models")
    
}
