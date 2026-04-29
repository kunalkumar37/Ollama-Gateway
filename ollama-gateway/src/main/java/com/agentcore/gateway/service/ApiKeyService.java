package com.agentcore.gateway.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.agentcore.gateway.config.GatewayProperties;
import com.agentcore.gateway.model.ApiKeyInfo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {
    private final GatewayProperties props;
    private final Map<String,ApiKeyInfo>keyStore = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init(){
        props.getApiKeys().forEach((key,label)->{
            Integer rpmOverride = props.getRateLimit()
                .getPerKeyLimits()
                .get(key);   

            ApiKeyInfo info = ApiKeyInfo.builder()
                .key(key)
                .label(label)
                .active(true)
                .createdAt(Instant.now())
                .requestPerMinuteOverride(rpmOverride)
                .build();
                
                keyStore.put(key,info);
                log.info("Loaded API key: {} -> {}",
                    maskKey(key),label);

        });

        log.info("Total API keys loaded: {}", keyStore.size());

    }

    public Optional<ApiKeyInfo> validate(String key){
        if(key == null || key.isBlank()) {
            return Optional.empty();

        }
        ApiKeyInfo info = keyStore.get(key);
        if(info == null || !info.isActive()){
            return Optional.empty();

        }
        return Optional.of(info);

    }

    public ApiKeyInfo addKey(String label){
        String newKey= "sk-" + UUID.randomUUID()
                                .toString()
                                .replace("-","")
                                .substring(0,24);

        ApiKeyInfo info = ApiKeyInfo.builder()
                            .key(newKey)
                            .label(label)
                            .active(true)
                            .createdAt(Instant.now())
                            .build();

        keyStore.put(newKey,info);
        log.info("New key created for: {} -> {}",label, maskKey(newKey));
        return info;

    }


    public boolean revokeKey(String key){
        ApiKeyInfo info = keyStore.get(key);
        if(info !=null){
            info.setActive(false);
            log.info("Key revoked: {}", maskKey(key));
            return true;
        }
        return false;

    }

    public List<ApiKeyInfo>getAllKeys(){
        return new ArrayList<>(keyStore.values());
    }

    public void recordRequest(String key){
        ApiKeyInfo info = keyStore.get(key);
        if(info!=null){
            info.incrementRequests();
        }
    }

    //show first 8 and last 4 characters only
    public static String maskKey(String key){
        if(key==null || key.length()<12){
            return "****";
        }
        return key.substring(0,8)+"..."+key.substring(key.length()-4);
    }

    
}
