package com.newsverifier.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsverifier.model.CustomSearchResponse;
import com.newsverifier.model.SearchItem;

@Service
@RequiredArgsConstructor
public class SimilarNewsService {
    private static final Logger logger = LoggerFactory.getLogger(SimilarNewsService.class);
    private static final double SIMILARITY_THRESHOLD = 0.3; // Benzerlik eşiği

    @Value("${google.api.key}")
    private String googleApiKey;

    @Value("${google.search.engine.id}")
    private String searchEngineId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final FetchNewsDataService fetchNewsDataService;
    private final GoogleCustomSearchService googleCustomSearchService;

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSimilarNews(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arama sorgusu boş olamaz");
        }

        logger.info("Google API Konfigürasyonu:");
        logger.info("API Key: {}", maskApiKey(googleApiKey));
        logger.info("Search Engine ID: {}", searchEngineId);

        try {
            CustomSearchResponse searchResponse = googleCustomSearchService.search(query, 10);
            if (searchResponse == null || searchResponse.getItems() == null) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> similarNews = new ArrayList<>();
            for (SearchItem item : searchResponse.getItems()) {
                try {
                    // Haber içeriğini çek
                    Map<String, String> newsData = fetchNewsDataService.fetchNewsData(item.getLink());

                    // Benzerlik oranını hesapla
                    double similarity = calculateSimilarity(query,
                            newsData.get("title") + " " + newsData.get("content"));

                    // Sadece belirli bir eşik değerinin üzerindeki haberleri ekle
                    if (similarity >= SIMILARITY_THRESHOLD) {
                        Map<String, Object> newsItem = new HashMap<>();
                        newsItem.put("title", newsData.get("title"));
                        newsItem.put("snippet", newsData.get("summary"));
                        newsItem.put("link", item.getLink());
                        newsItem.put("similarity", similarity);
                        similarNews.add(newsItem);
                    }
                } catch (Exception e) {
                    logger.error("Haber verisi çekilemedi: {}", item.getLink(), e);
                }
            }

            return similarNews;
        } catch (Exception e) {
            logger.error("Benzer haberler alınırken hata oluştu", e);
            return Collections.emptyList();
        }
    }

    private double calculateSimilarity(String query, String newsContent) {
        // Metinleri küçük harfe çevir ve kelimelere ayır
        String[] queryWords = query.toLowerCase().split("\\s+");
        String[] newsWords = newsContent.toLowerCase().split("\\s+");

        // Ortak kelimeleri say
        int commonWords = 0;
        for (String queryWord : queryWords) {
            for (String newsWord : newsWords) {
                if (queryWord.equals(newsWord)) {
                    commonWords++;
                    break;
                }
            }
        }

        // Benzerlik oranını hesapla (ortak kelime sayısı / sorgu kelime sayısı)
        return (double) commonWords / queryWords.length;
    }

    private String maskApiKey(String input) {
        if (input == null)
            return null;
        return input.replaceAll("AIza[0-9A-Za-z-_]{32}", "AIza***MASKED***");
    }

    private Map<String, Object> sanitizeResponse(Map<String, Object> response) {
        if (response == null)
            return null;

        Map<String, Object> sanitized = new HashMap<>(response);
        if (sanitized.containsKey("queries")) {
            sanitized.put("queries", "***MASKED***");
        }
        return sanitized;
    }
}