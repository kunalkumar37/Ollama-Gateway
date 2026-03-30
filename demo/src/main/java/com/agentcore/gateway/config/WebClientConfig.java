package com.agentcore.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ollamaWebClient(GatewayProperties props){
        HttpClient httpClient = HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,10_000)
                                .responseTimeout(Duration.ofMinutes(10))
                                .doOnConnected(conn->conn.addHandlerLast(new ReadTimeoutHandler(600,TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30,TimeUnit.SECONDS))
                            );
        
        return WebClient.builder()
                            .baseUrl(props.getOllamaBaseurl())
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .codecs(c->c.defaultCodecs().maxInMemorySize(100*1024*1024))
                            .build();
                            
                            
    }
}

