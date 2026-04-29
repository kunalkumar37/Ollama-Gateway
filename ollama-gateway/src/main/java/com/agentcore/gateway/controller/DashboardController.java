package com.agentcore.gateway.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import com.agentcore.gateway.service.SystemMonitorService;
import com.agentcore.gateway.service.ApiKeyService;
import com.agentcore.gateway.service.RateLimiterService;
import com.agentcore.gateway.service.OllamaProxyService;
import com.agentcore.gateway.model.ApiKeyInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class DashboardController {

    private final SystemMonitorService monitorService;
    private final ApiKeyService apiKeyService;
    private final RateLimiterService rateLimiterService;
    private final OllamaProxyService ollamaProxyService;

    // Serves the HTML page from templates/dashboard.html
    @GetMapping({"/", "/dash"})
    public String dashboard(Model model) {
        model.addAttribute("ollamaAlive",
            ollamaProxyService.isOllamaAlive());
        model.addAttribute("totalKeys",
            apiKeyService.getAllKeys().size());
        return "dashboard";
    }

    /**
     * GET /dash/metrics
     *
     * Polled by the dashboard page every 2 seconds.
     * Returns everything needed to update all widgets.
     */
    @GetMapping("/dash/metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> metrics() {
        Map<String, Object> snap = monitorService.getSnapshot();

        Map<String, Object> data = new HashMap<>();

        // Current system state
        data.put("cpu",          snap.get("cpu_percent"));
        data.put("memUsed",      snap.get("used_mem_mb"));
        data.put("memTotal",     snap.get("total_mem_mb"));
        data.put("memPercent",   snap.get("mem_percent"));
        data.put("gpuAvailable", snap.get("gpu_available"));
        data.put("gpu",          snap.get("gpu_percent"));
        data.put("gpuMemUsed",   snap.get("gpu_used_mb"));
        data.put("gpuMemTotal",  snap.get("gpu_total_mb"));
        data.put("gpuMemPercent",snap.get("gpu_mem_percent"));
        data.put("gpuTemp",      snap.get("gpu_temp_c"));

        // Request counters
        data.put("totalRequests",    snap.get("total_requests"));
        data.put("activeRequests",   monitorService.getActiveRequests());
        data.put("rejectedRequests", 0);
        data.put("ollamaAlive",      ollamaProxyService.isOllamaAlive());

        // History arrays for charts
        synchronized (monitorService.getCpuHistory()) {
            data.put("cpuHistory",  List.copyOf(monitorService.getCpuHistory()));
            data.put("gpuHistory",  List.copyOf(monitorService.getGpuHistory()));
            data.put("reqHistory",  List.copyOf(monitorService.getRequestHistory()));
            data.put("timeHistory", List.copyOf(monitorService.getTimeHistory()));
        }

        // Rate limit bucket status (key is masked)
        Map<String, Object> rl = new HashMap<>();
        rateLimiterService.getAllBucketStatus().forEach((key, status) -> {
            rl.put(ApiKeyService.maskKey(key), Map.of(
                "available",   status.availableTokens(),
                "max",         status.maxTokens(),
                "concurrent",  status.currentConcurrent(),
                "maxConcurrent", status.maxConcurrent(),
                "rejected",    status.totalRejected()
            ));
        });
        data.put("rateLimits", rl);

        // Per-key usage stats (active keys only)
        List<Map<String, Object>> keyStats = apiKeyService.getAllKeys()
            .stream()
            .filter(ApiKeyInfo::isActive)
            .map(k -> Map.<String, Object>of(
                "label",    k.getLabel(),
                "requests", k.getTotalRequests().get(),
                "lastUsed", k.getLastUsedAt() != null
                              ? k.getLastUsedAt().toString() : "Never"
            ))
            .toList();
        data.put("keyStats", keyStats);

        return ResponseEntity.ok(data);
    }
}
