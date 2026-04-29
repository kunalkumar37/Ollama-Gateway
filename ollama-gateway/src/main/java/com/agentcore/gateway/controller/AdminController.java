package com.agentcore.gateway.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentcore.gateway.config.GatewayProperties;
import com.agentcore.gateway.model.ApiKeyInfo;
import com.agentcore.gateway.service.ApiKeyService;
import com.agentcore.gateway.service.OllamaProxyService;
import com.agentcore.gateway.service.RateLimiterService;
import com.agentcore.gateway.service.SystemMonitorService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final GatewayProperties props;
    private final ApiKeyService apiKeyService;
    private final RateLimiterService rateLimiterService;
    private final SystemMonitorService monitorService;
    private final OllamaProxyService ollamaProxyService;


    private boolean isAdmin(HttpServletRequest request){
    String auth = request.getHeader("Authorization");
    if(auth!=null && auth.startsWith("Bearer ")){
        return auth.substring(7).trim().equals(props.getAdminSecretKey());

    }
    String adminKey = request.getHeader("X-Admin-Key");
    return props.getAdminSecretKey().equals(adminKey);
}
    private ResponseEntity<?> deny(){
        return ResponseEntity.status(401).body(Map.of(
            "error", "Invalid admin key."
        ));

    }

    @GetMapping("/keys")
    public ResponseEntity<?> listKeys(HttpServletRequest req){
        if(!isAdmin(req))
        {
            return deny();
        }

        List<Map<String, Object>> result = apiKeyService.getAllKeys()
        .stream()
        .map(k -> Map.<String, Object> of (
             "full_key",       k.getKey(),
                "key_preview",    ApiKeyService.maskKey(k.getKey()),
                "label",          k.getLabel(),
                "active",         k.isActive(),
                "total_requests", k.getTotalRequests().get(),
                "created_at",     k.getCreatedAt() != null
                                    ? k.getCreatedAt().toString() : "N/A",
                "last_used_at",   k.getLastUsedAt() != null
                                    ? k.getLastUsedAt().toString() : "Never"
        ))
        .toList();

        return ResponseEntity.ok(Map.of("keys", result, "total", result.size()));

    }
 @PostMapping("/keys")
    public ResponseEntity<?> createKey(@RequestBody Map<String, String> body,
                                        HttpServletRequest req) {
        if (!isAdmin(req)) return deny();

        String label = body.getOrDefault("label", "Unnamed");
        ApiKeyInfo created = apiKeyService.addKey(label);

        return ResponseEntity.ok(Map.of(
            "message",    "Key created. Save it — shown only once.",
            "key",        created.getKey(),
            "label",      created.getLabel(),
            "created_at", created.getCreatedAt().toString()
        ));
    }

    @DeleteMapping("/keys/{key}")
    public ResponseEntity<?> revokeKey(@PathVariable String key,
                                        HttpServletRequest req) {
        if (!isAdmin(req)) return deny();
        boolean revoked = apiKeyService.revokeKey(key);
        if (revoked) return ResponseEntity.ok(Map.of("message", "Revoked."));
        return ResponseEntity.notFound().build();
    }

    // ─── Stats ───────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> stats(HttpServletRequest req) {
        if (!isAdmin(req)) return deny();
        return ResponseEntity.ok(Map.of(
            "system",       monitorService.getSnapshot(),
            "rate_limits",  rateLimiterService.getAllBucketStatus(),
            "ollama_alive", ollamaProxyService.isOllamaAlive()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health(HttpServletRequest req) {
        if (!isAdmin(req)) return deny();
        return ResponseEntity.ok(Map.of(
            "gateway",        "UP",
            "ollama",         ollamaProxyService.isOllamaAlive() ? "UP" : "DOWN",
            "total_keys",     apiKeyService.getAllKeys().size(),
            "active_requests", monitorService.getActiveRequests()
        ));
    }
}


