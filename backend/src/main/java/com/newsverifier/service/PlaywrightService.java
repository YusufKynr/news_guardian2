package com.newsverifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlaywrightService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${playwright.service.url:http://localhost:8080}")
    private String playwrightServiceUrl;

    public List<TitleResult> fetchTitlesFromUrls(List<String> urls) {
        log.info("Playwright servisine istek atılıyor: {} URL", urls.size());
        log.info("Playwright service URL: {}", playwrightServiceUrl);
        
        try {
            String url = playwrightServiceUrl + "/fetch-titles";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = Map.of("urls", urls);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            log.info("Request body: {}", requestBody);
            log.info("Request headers: {}", headers);
            
            ResponseEntity<TitleResult[]> response = restTemplate.postForEntity(
                url, entity, TitleResult[].class
            );
            
            log.info("Response status: {}", response.getStatusCode());
            log.info("Response headers: {}", response.getHeaders());
            
            TitleResult[] results = response.getBody();
            List<TitleResult> resultList = results != null ? Arrays.asList(results) : List.of();
            
            log.info("Playwright response: {} results", resultList.size());
            if (resultList.isEmpty()) {
                log.warn("Playwright'ten boş sonuç döndü!");
            }
            return resultList;
            
        } catch (Exception e) {
            log.error("Playwright servisinden title'lar alınırken hata oluştu: ", e);
            log.error("Exception type: {}", e.getClass().getSimpleName());
            log.error("Exception message: {}", e.getMessage());
            return List.of();
        }
    }

    public static class TitleResult {
        public String url;
        public String title;
        public String error;
        
        public TitleResult() {}
        
        public TitleResult(String url, String title, String error) {
            this.url = url;
            this.title = title;
            this.error = error;
        }
        
        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
} 