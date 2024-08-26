package com.daewon.xeno_backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthController {


    @Value("${health.message}")
    private String healthMessage;

    @GetMapping
    public String checkHealth() {
        return healthMessage;
    }
}