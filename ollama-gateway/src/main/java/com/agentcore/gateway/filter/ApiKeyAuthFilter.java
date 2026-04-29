package com.agentcore.gateway.filter;

import com.agentcore.gateway.service.ApiKeyService;
import com.agentcore.gateway.model.ApiKeyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter{
    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;
    
    public static final String API_KEY_ATTR ="authenticated_api_key";

    @Override   
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, @org.springframework.lang.NonNull HttpServletResponse response, @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException{
                String path = request.getRequestURI();

                if(!path.startsWith("/v1/")){
                    filterChain.doFilter(request,response);
                    return;
                }

                ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
                String authHeader = request.getHeader("Authorization");
                String apiKey = null;

                if(authHeader !=null && authHeader.startsWith("Bearer ")){
                    apiKey = authHeader.substring(7).trim();

                }

                if(apiKey == null || apiKey.isBlank()){
                    apiKey = request.getHeader("x-api-key");
                }

                if(apiKey == null || apiKey.isBlank()){
                    log.warn("No API key on request to {} from IP {}",
                        path, getClientIp(request)
                    );
                    sendError(response, HttpStatus.UNAUTHORIZED.value(),"Missing API key. Use: Authorization: Bearer <your-key>");
                            return;
                }

                Optional<ApiKeyInfo> keyInfo = apiKeyService.validate(apiKey);

                if(keyInfo.isEmpty()){
                    log.warn("Invalid key [{}] on request to {} from IP {}",
                        ApiKeyService.maskKey(apiKey),path,getClientIp(request));
                        sendError(response, HttpStatus.UNAUTHORIZED.value(),"Invalid API key");
                        return;
                    
                }

                wrappedRequest.setAttribute(API_KEY_ATTR, keyInfo.get());
                log.debug("Auth OK : [{}] -> {}", keyInfo.get().getLabel(),path);
                filterChain.doFilter(wrappedRequest,response);


            }

            private void sendError(HttpServletResponse response, int status, String message) throws IOException {
                response.setStatus(status);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                     "error", Map.of(
                "message", message,
                "type", "authentication_error",
                "code", status
            )
        )));
        }

        private String getClientIp(HttpServletRequest request){
            String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) return cfIp;

        // Ngrok / other proxies use X-Forwarded-For
        String xForwarded = request.getHeader("X-Forwarded-For");
        if (xForwarded != null && !xForwarded.isBlank()) {
            return xForwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
        }


        

    
    
    
}



