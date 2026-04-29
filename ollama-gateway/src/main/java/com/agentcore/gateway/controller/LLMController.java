package com.agentcore.gateway.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentcore.gateway.model.ChatRequest;
import com.agentcore.gateway.service.LLMService;

@RestController
@RequestMapping("/v1/llm")
public class LLMController {
    private final LLMService llmService;

    public LLMController(LLMService llmService){
        this.llmService = llmService;

    }

    @PostMapping("/generate")
    public String generate(@RequestBody ChatRequest request){
        return llmService.generate(
            request.getModel(),
            request.getPrompt()
        );
    }
}
