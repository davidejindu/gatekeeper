package com.example.gatekeeper.service;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CircuitBreakerService {

    private final ReactiveCircuitBreaker circuitBreaker;
    private final WebClient webClient;

    public CircuitBreakerService(ReactiveCircuitBreakerFactory circuitBreakerFactory,
                                 WebClient.Builder webClientBuilder) {
        this.circuitBreaker = circuitBreakerFactory.create("backend");
        this.webClient = webClientBuilder.baseUrl("http://localhost:8081").build();
    }

    public Mono<String> proxyRequest(String method, String uri, String body) {
        Mono<String> request;

        if("POST".equalsIgnoreCase(method)) {
            request = webClient.post()
                    .uri(uri)
                    .bodyValue(body != null ? body : "")
                    .retrieve()
                    .bodyToMono(String.class);
        } else {
            request = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class);
        }

        return circuitBreaker.run(
                request,
                throwable -> {
                    System.out.println("Circuit breaker fallback triggered: " + throwable.getMessage());
                    return Mono.just("Service temporarily unavailable: " + throwable.getMessage());
                }
        );
    }
}
