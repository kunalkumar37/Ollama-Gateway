package com.agentcore.gateway.controller;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.agentcore.gateway.filter.ApiKeyAuthFilter;
import com.agentcore.gateway.model.ApiKeyInfo;
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
    @SuppressWarnings("null")
    public ResponseEntity<?> listModels(HttpServletRequest request){
        recordRequest(request);
        String body = ollamaProxyService.getModels();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }


        @SuppressWarnings("null")
    private ResponseEntity<?> proxy(String ollamaPath, HttpMethod method, String requestBody){

        boolean streaming = ollamaProxyService.isStreamingRequest(requestBody);

        if(streaming){
            log.debug("Streaming -> ollama {}", ollamaPath);
            StreamingResponseBody stream = ollamaProxyService.forwardStreamingRequest(
                ollamaPath, method, requestBody
            );
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(stream);


        }
        else{
            log.debug("Standard -> Ollama {}", ollamaPath);
            String responseBody = ollamaProxyService.forwardRequest(
                ollamaPath, method, requestBody
            );

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
                

        }
    }

    private void recordRequest(HttpServletRequest request){
        ApiKeyInfo keyInfo = (ApiKeyInfo) request.getAttribute(ApiKeyAuthFilter.API_KEY_ATTR);
        if(keyInfo != null){
            apiKeyService.recordRequest(keyInfo.getKey());
            
        }
    }


}




