package com.agentcore.gateway.model;

import lombok.Data;

@Data 
public class ChatRequest {
    private String model;
    private String prompt;
    
}
