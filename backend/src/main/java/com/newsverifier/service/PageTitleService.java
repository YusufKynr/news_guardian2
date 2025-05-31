package com.newsverifier.service;

import com.newsverifier.model.TitleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageTitleService {

    private final RestTemplate restTemplate;

    @Value("${python.service.url}")
    private String pythonServiceUrl;

    public TitleResponse fetchSingleTitle(String url) {
        Map<String, Object> body = Map.of("urls", List.of(url));

        try {
            TitleResponse[] response = restTemplate.postForObject(
                    pythonServiceUrl,
                    body,
                    TitleResponse[].class
            );

            return (response != null && response.length > 0)
                    ? new TitleResponse(
                    "",                    // inputText - burada kullanılmıyor
                    List.of(),            // inputNER - burada kullanılmıyor
                    url,                  // url
                    response[0].getTitle(), // title
                    List.of(),            // titleNER - burada kullanılmıyor
                    "Başlık başarıyla çekildi"
            )
                    : new TitleResponse(
                    "",                    // inputText
                    List.of(),            // inputNER
                    url,                  // url
                    "Başlık alınamadı",   // title
                    List.of(),            // titleNER
                    "Başlık alınamadı"
            );

        } catch (Exception e) {
            log.error("Python scraping servis hatası: {}", e.getMessage());
            return new TitleResponse(
                    "",                    // inputText
                    List.of(),            // inputNER
                    url,                  // url
                    "Başlık alınamadı (HATA)", // title
                    List.of(),            // titleNER
                    "Python servis hatası: " + e.getMessage()
            );
        }
    }
}
