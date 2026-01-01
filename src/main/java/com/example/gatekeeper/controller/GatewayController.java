package com.example.gatekeeper.controller;


import com.example.gatekeeper.service.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Set<String> VALID_API_KEYS = Set.of(
            "test-key-1",
            "test-key-2",
            "test-key-3"
    );

    private final WebClient webClient;
    private final RateLimiterService rateLimiterService;

    public GatewayController(WebClient.Builder webClientBuilder,
                             RateLimiterService rateLimiterService) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8081")
                .build();
        this.rateLimiterService = rateLimiterService;
    }


    @GetMapping("/**")
    public Mono<ResponseEntity<String>> proxyGet(@RequestHeader(value = "X-API-KEY", required = false) String apiKey) {

        if (apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error invalid key"));
        }

        return rateLimiterService.allowRequest(apiKey)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body("Exceeded Request Limit"));
                    }
                    return webClient.post()
                            .uri("/post")
                            .retrieve()
                            .toEntity(String.class);
                });

    }

    @PostMapping("/**")
    public Mono<ResponseEntity<String>> proxyPost(@RequestHeader(value = "X-API-KEY", required = false)
                                                  String apiKey, @RequestBody(required = false) String body) {


        if (apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error invalid key"));
        }

        return rateLimiterService.allowRequest(apiKey)
                .flatMap(allowed -> {
                    if (!allowed) {
                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                .body("Exceeded Request Limit"));
                    }


                    return webClient.post()
                            .uri("/post")
                            .bodyValue(body != null ? body : "")
                            .retrieve().toEntity(String.class);

                });

    }

    @GetMapping("/limits")
    public Mono<ResponseEntity<String>> checkLimits(@RequestHeader (value = "X-API-KEY",required = false)
                                                    String apiKey) {
        if (apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error invalid key"));
        }

        return rateLimiterService.getRemainingRequests(apiKey)
                .map(remaining ->  ResponseEntity.ok(
                        "remaining: " + remaining + " limit:100, window:60s"
                ));
    }

}
