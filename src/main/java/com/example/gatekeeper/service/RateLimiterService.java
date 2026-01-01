package com.example.gatekeeper.service;


import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RateLimiterService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final long MAX_REQUESTS = 100;
    private static final long WINDOW_SECONDS = 60;


    public RateLimiterService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Boolean> allowRequest(String apiKey) {
        String key = "ratelimit:" + apiKey;

        return redisTemplate.opsForValue()
                .increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS))
                                .thenReturn(true);
                    }

                    if (count > MAX_REQUESTS) {
                        return Mono.just(false);
                    }

                    System.out.println("Request for key: " + apiKey + ", request: " + count + "/" + MAX_REQUESTS);
                    return Mono.just(true);
                });
    }

    public Mono<Long> getRemainingRequests(String apiKey) {
        String key = "ratelimit:" + apiKey;

        return redisTemplate.opsForValue()
                .get(key)
                .map(value -> {
                    long current = Long.parseLong(value);
                    return Math.max(0, MAX_REQUESTS - current);
                })
                .defaultIfEmpty(MAX_REQUESTS);
    }



}
