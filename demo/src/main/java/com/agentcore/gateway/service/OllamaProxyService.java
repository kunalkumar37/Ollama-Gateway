package com.agentcore.gateway.service;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaProxyService {

    private final WebClient ollamaWebClient;

    /**
     * Forward a standard (non-streaming) request to Ollama.
     * Blocks until full response is received.
     */
    @SuppressWarnings("null")
    public String forwardRequest(String path,
                                  HttpMethod method,
                                  String requestBody) {
        log.debug("Proxying {} {}", method, path);

        var spec = ollamaWebClient
            .method(method)
            .uri(path)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON);

        if (requestBody != null && !requestBody.isBlank()) {
            spec.bodyValue(requestBody);
        }

        return spec.retrieve()
            .bodyToMono(String.class)
            .onErrorMap(WebClientResponseException.class, ex -> {
                log.error("Ollama returned error {}: {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
                return new ResponseStatusException(
                    ex.getStatusCode(),
                    "Ollama error: " + ex.getResponseBodyAsString());
            })
            .onErrorMap(Exception.class, ex -> {
                log.error("Ollama unreachable: {}", ex.getMessage());
                return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Ollama is not reachable. Is it running on port 11434?");
            })
            .block();
    }

    /**
     * Forward a streaming request to Ollama.
     *
     * Ollama streams newline-delimited JSON (NDJSON).
     * Each chunk is one JSON object on one line.
     * We pipe each chunk directly to the client's output stream
     * without buffering the whole response — critical for LLMs
     * that generate thousands of tokens.
     */
    @SuppressWarnings("null")
    public StreamingResponseBody forwardStreamingRequest(
            String path,
            HttpMethod method,
            String requestBody) {

        log.debug("Proxying STREAM {} {}", method, path);

        return (OutputStream outputStream) -> {
            Flux<String> streamFlux = ollamaWebClient
                .method(method)
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON,
                        MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody != null ? requestBody : "{}")
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorMap(ex -> {
                    log.error("Ollama stream error: {}", ex.getMessage());
                    return new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Ollama stream failed: " + ex.getMessage());
                });

            streamFlux.doOnNext(chunk -> {
                try {
                    outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                    outputStream.write('\n');
                    outputStream.flush(); // Push chunk to client immediately
                } catch (Exception e) {
                    throw new RuntimeException("Stream write failed", e);
                }
            })
            .doOnError(e -> log.error("Stream terminated with error: {}",
                e.getMessage()))
            .blockLast(); // Wait for stream to complete
        };
    }

    /**
     * Check the request body to see if client wants streaming.
     * We look for "stream": true in the JSON body.
     */
    public boolean isStreamingRequest(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) return false;
        return requestBody.contains("\"stream\"")
            && requestBody.contains("true");
    }

    /**
     * Get list of models from Ollama.
     * Used by GET /v1/models endpoint.
     * Returns fallback JSON if Ollama is down.
     */
    public String getModels() {
        try {
            return ollamaWebClient
                .get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
        } catch (Exception e) {
            log.warn("Could not fetch models from Ollama: {}", e.getMessage());
            return "{\"models\":[],\"error\":\"Ollama not reachable\"}";
        }
    }

    /**
     * Quick check if Ollama is alive.
     * Used by admin /health and dashboard.
     */
    public boolean isOllamaAlive() {
        try {
            String result = ollamaWebClient
                .get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .block();
            return result != null;
        } catch (Exception e) {
            return false;
        }
    }
}

