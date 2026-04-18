package com.agentcore.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class ProxyController {
    private final OllamaProxyService ollamaProxyService;
    private final ApiKeyService apiKeyService;

    
}
