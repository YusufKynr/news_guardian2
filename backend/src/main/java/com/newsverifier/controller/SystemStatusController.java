package com.newsverifier.controller;

import com.newsverifier.model.health.SystemStatusModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemStatusController {

    @GetMapping("/status")
    public SystemStatusModel healthCheck() {
        return new SystemStatusModel("UP", "News Guardian backend is running");
    }
}
