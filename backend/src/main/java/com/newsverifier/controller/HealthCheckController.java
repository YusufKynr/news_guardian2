package com.newsverifier.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/health")
public class HealthCheckController {
    
    @GetMapping
    public String healthCheck() {
        return "News Verifier is up and running!";
    }
}
