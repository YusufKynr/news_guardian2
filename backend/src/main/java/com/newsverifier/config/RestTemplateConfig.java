// src/main/java/com/newsverifier/config/RestTemplateConfig.java
package com.newsverifier.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // HTTP/1.1 zorlayarak HTTP/2 upgrade sorununu önle
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);  // 30 saniye
        factory.setReadTimeout(60000);     // 60 saniye
        
        return builder
                .requestFactory(() -> factory)
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(60))
                .build();
    }
}
