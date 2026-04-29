package com.agentcore.gateway.service;

import org.springframework.stereotype.Service;

import com.agentcore.gateway.client.OllamaClient;

@Service
public class LLMService {
    private final OllamaClient ollamaClient;

    public LLMService(OllamaClient ollamaClient){
        this.ollamaClient=ollamaClient;
    }

    public String generate(String model, String prompt){
        return ollamaClient.generate(model,prompt);
        
    }
}
