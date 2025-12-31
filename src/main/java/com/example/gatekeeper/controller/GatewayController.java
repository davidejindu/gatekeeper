package com.example.gatekeeper.controller;


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

    public GatewayController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://localhost:8081")
                .build();
    }


    @GetMapping("/**")
    public Mono<ResponseEntity<String>> proxyGet(@RequestHeader(value = "X-API-KEY", required = false) String apiKey) {
        System.out.println("Request retrieved with api key: " + apiKey);

        if(apiKey == null || !VALID_API_KEYS.contains(apiKey)) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error invalid key"));
        }

        System.out.println("Valid key: " + apiKey);


        return webClient.get()
                .uri("/get")
                .retrieve()
                .toEntity(String.class);
    }

    @PostMapping("/**")
    public Mono<ResponseEntity<String>> proxyPost(@RequestHeader(value = "X-API-KEY", required = false)
                                                      String apiKey, @RequestBody(required = false) String body) {
        System.out.println("POST request with api key:" + apiKey);
        System.out.println("Received body: " + body);



        return webClient.post()
                .uri("/post")
                .bodyValue(body != null ? body : "")
                .retrieve().toEntity(String.class);

    }

}
