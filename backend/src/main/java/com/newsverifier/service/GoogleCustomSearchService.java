package com.newsverifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsverifier.model.CustomSearchResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GoogleCustomSearchService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleCustomSearchService.class);

    @Value("${google.api.key}")
    private String googleApiKey;

    @Value("${google.search.engine.id}")
    private String searchEngineId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CustomSearchResponse search(String query, int numResults) {
        try {
            String searchUrl = String.format(
                    "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s&num=%d",
                    googleApiKey, searchEngineId, query, numResults);

            logger.info("Google API'ye istek gönderiliyor. URL: {}", maskApiKey(searchUrl));

            CustomSearchResponse response = restTemplate.getForObject(searchUrl, CustomSearchResponse.class);

            if (response == null) {
                logger.error("Google API'den boş yanıt alındı");
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Google API servisine şu anda ulaşılamıyor");
            }

            logger.info("Google API yanıtı alındı: {}", objectMapper.writeValueAsString(response));
            return response;

        } catch (Exception e) {
            logger.error("Google API isteği başarısız: {}", e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Google API hatası: " + e.getMessage());
        }
    }

    private String maskApiKey(String url) {
        return url.replaceAll("key=[^&]+", "key=***");
    }
}