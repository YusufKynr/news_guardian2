package com.newsverifier.service;

import com.newsverifier.model.NERApiResponseItem;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NamedEntityService {

    private final RestTemplate restTemplate;

    @Value("${ner.api.url}")
    private String nerApiUrl;

    public List<NERApiResponseItem> extractEntities(List<String> texts) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Python servisine metinleri gönder
        Map<String, Object> body = Map.of("texts", texts);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<NERApiResponseItem[]> response = restTemplate.exchange(
                    nerApiUrl.replace("/get-titles", "/extract-entities"),
                    HttpMethod.POST,
                    entity,
                    NERApiResponseItem[].class
            );

            return response.getBody() != null ? Arrays.asList(response.getBody()) : List.of();
        } catch (Exception e) {
            // Hata durumunda boş liste döndür
            return List.of();
        }
    }
}
