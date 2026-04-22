package com.agentcore.gateway.service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.agentcore.gateway.config.GatewayProperties;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SystemMonitorService {
     private final GatewayProperties props;

    // ─── Current Values ──────────────────────────────────────────────────────
    @Getter private volatile double cpuPercent     = 0.0;
    @Getter private volatile long   usedMemMb      = 0;
    @Getter private volatile long   totalMemMb     = 0;
    @Getter private volatile double memPercent     = 0.0;

    @Getter private volatile double gpuPercent     = 0.0;
    @Getter private volatile long   gpuUsedMb      = 0;
    @Getter private volatile long   gpuTotalMb     = 0;
    @Getter private volatile double gpuMemPercent  = 0.0;
    @Getter private volatile double gpuTemperature = 0.0;
    @Getter private volatile boolean gpuAvailable  = false;

    // ─── Request Counters ────────────────────────────────────────────────────
    private final AtomicLong totalRequests  = new AtomicLong(0);
    private final AtomicLong activeRequests = new AtomicLong(0);
    private final AtomicLong totalRejected  = new AtomicLong(0);

    // ─── Rolling History (60 points = 2 min at 2s interval) ─────────────────
    private static final int HISTORY_SIZE = 60;

    @Getter private final List<Double> cpuHistory     = new ArrayList<>();
    @Getter private final List<Double> gpuHistory     = new ArrayList<>();
    @Getter private final List<Long>   requestHistory = new ArrayList<>();
    @Getter private final List<String> timeHistory    = new ArrayList<>();

    private long lastSnapshotRequests = 0;

    @Scheduled(fixedRate =2000)
    public void collectAll(){
        collectCpuAndMemory();
        if(props.getMonitoring().isGpuEnabled()){
            collectGpu();

        }
        updateHistory();
    }

    private void collectCpuAndMemory(){
        try{
            OperatingSystemMXBean os= ManagementFactory.getOperatingSystemMXBean();
            if(os instanceof com.sun.management.OperatingSystemMXBean sunOs){
                double cpu = sunOs.getCpuLoad()*100.0;
                cpuPercent = cpu<0 ? 0.0 : round(cpu,1);

                long freeBytes = sunOs.getFreeMemorySize();
                long totalBytes = sunOs.getTotalMemorySize();


                totalMemMb  = totalBytes / (1024 * 1024);
                usedMemMb   = (totalBytes - freeBytes) / (1024 * 1024);
                memPercent  = totalBytes > 0
                    ? round((double) usedMemMb / totalMemMb * 100.0, 1)
                    : 0.0;
            }
        } catch (Exception e) {
            log.debug("CPU/Memory collection error: {}", e.getMessage());
        }
    }
    



    public void onRequestStart() {
        // Monitor request start
    }
    
    public void onRequestEnd() {
        // Monitor request end
    }
    
    public void onRequestRejected() {
        // Monitor rejected request
        log.debug("Request rejected by rate limiter");
    }
}
