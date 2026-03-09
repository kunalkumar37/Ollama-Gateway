package com.agentcore.gateway.client;

@Component
public class OllamaClient {
    private final WebClient webClient;

    public OllamaClient(Webclient webClient){
        this.webClient = webClient;
    }

    public String generate(String model, String prompt){
        return webClient.post()
        .uri("/api/generate")
        .bodyValue(new Request(model,prompt))
        .retrieve()
        .bodyToMono(String.class)
        .block();
    }

    record Request(String model, String prompt) {}
}
