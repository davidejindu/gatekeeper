package com.example.gatekeeper.controller;


import com.example.gatekeeper.service.CircuitBreakerService;
import com.example.gatekeeper.service.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Set<String> VALID_API_KEYS;

    static {
        VALID_API_KEYS = new HashSet<>();
        for (int i = 1; i <= 100; i++) {
            VALID_API_KEYS.add("test-key-" + i);
        }
    }

    private final RateLimiterService rateLimiterService;
    private final CircuitBreakerService circuitBreakerService;

    public GatewayController(WebClient.Builder webClientBuilder, RateLimiterService rateLimiterService,
                             CircuitBreakerService circuitBreakerService) {

        this.rateLimiterService = rateLimiterService;
        this.circuitBreakerService = circuitBreakerService;
    }


    @GetMapping("/health")
    public Mono<ResponseEntity<String>> health() {
        return Mono.just(ResponseEntity.ok("OK"));
    }

    @GetMapping("/**")
    public Mono<ResponseEntity<String>> proxyGet(@RequestHeader(value = "X-API-Key", required = false) String apiKey) {

        if (apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error invalid key"));
        }
//
//        return rateLimiterService.allowRequest(apiKey)
//                .flatMap(allowed -> {
//                    if (!allowed) {
//                        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//                                .body("Exceeded Request Limit"));
//                    }

//
//                });

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8081")
                .build();

        return webClient.get()
                .uri("/get")
                .retrieve()
                .bodyToMono(String.class)
                .map(ResponseEntity::ok);

//      return circuitBreakerService.proxyRequest("GET", "/get", null)
//              .map(ResponseEntity::ok);


    }

    @PostMapping("/**")
    public Mono<ResponseEntity<String>> proxyPost(@RequestHeader(value = "X-API-Key", required = false)
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

                    return circuitBreakerService.proxyRequest("POST", "/post", body)
                            .map(ResponseEntity::ok);

                });

    }

    @GetMapping("/limits")
    public Mono<ResponseEntity<String>> checkLimits(@RequestHeader (value = "X-API-Key",required = false)
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
