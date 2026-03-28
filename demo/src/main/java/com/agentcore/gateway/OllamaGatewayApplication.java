package com.agentcore.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OllamaGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(OllamaGatewayApplication.class, args);
		System.out.println("""
                
                ╔══════════════════════════════════════════════╗
                ║        Ollama Gateway Started                ║
                ║  API  →  http://localhost:8080/v1            ║
                ║  Dash →  http://localhost:8080/dash          ║
                ║  Admin → http://localhost:8080/admin         ║
                ╚══════════════════════════════════════════════╝
                """);
	}

}
