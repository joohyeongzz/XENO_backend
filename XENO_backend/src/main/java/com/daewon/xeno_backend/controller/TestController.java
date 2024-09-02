package com.daewon.xeno_backend.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/track")
    public ResponseEntity<?> trackDelivery(
            @RequestParam String carrierId,
            @RequestParam String trackingNumber
    ) {
        // API 엔드포인트 URL
        String url = "https://apis.tracker.delivery/graphql";

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "TRACKQL-API-KEY 97vetlqkkgltulpe7tmfq9pr2:3ii271qrmmir9pvnsj23s34cvv2l3nokhhn9desbbtkb08cqjoi");

        // GraphQL 요청 내용
        String query = "query Track($carrierId: ID!, $trackingNumber: String!) {" +
                "track(carrierId: $carrierId, trackingNumber: $trackingNumber) {" +
                "lastEvent {" +
                "time " +
                "status {" +
                "code " +
                "}" +
                "}" +
                "}" +
                "}";

        // 요청 바디 작성
        Map<String, Object> variables = new HashMap<>();
        variables.put("carrierId", carrierId);
        variables.put("trackingNumber", trackingNumber);

        Map<String, Object> body = new HashMap<>();
        body.put("query", query);
        body.put("variables", variables);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // API 요청
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        // 응답 반환
        return ResponseEntity.ok(response.getBody());
    }
}
