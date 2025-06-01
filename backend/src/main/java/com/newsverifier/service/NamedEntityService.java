package com.newsverifier.service;

import com.newsverifier.model.NERApiResponseItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NamedEntityService {

    private final RestTemplate restTemplate;

    @Value("${ner.api.url}")
    private String nerApiUrl;  // application.properties'den alınacak

    public List<NERApiResponseItem> extractEntities(List<String> texts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Python servisine metinleri gönder
        Map<String, Object> body = Map.of("texts", texts);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            log.info("NER isteği gönderiliyor: {} URL: {}", texts, nerApiUrl);
            
            ResponseEntity<NERApiResponseItem[]> response = restTemplate.exchange(
                    nerApiUrl,
                    HttpMethod.POST,
                    entity,
                    NERApiResponseItem[].class
            );

            log.info("NER response alındı: {}", response.getBody());
            return response.getBody() != null ? Arrays.asList(response.getBody()) : List.of();
        } catch (Exception e) {
            log.error("NER servisi hatası: {}", e.getMessage());
            // Hata durumunda boş liste döndür
            return List.of();
        }
    }
}
