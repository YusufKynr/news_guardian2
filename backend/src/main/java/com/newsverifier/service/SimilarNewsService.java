package com.newsverifier.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SimilarNewsService {

    @Value("${google.api.key}")
    private String GOOGLE_API_KEY;

    @Value("${google.search.engine.id}")
    private String SEARCH_ENGINE_ID;

    private final RestTemplate restTemplate = new RestTemplate();
    
    private final FetchNewsDataService fetchNewsDataService = new FetchNewsDataService();

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSimilarNews(String query) {
        String SEARCH_URL = String.format(
                "https://www.googleapis.com/customsearch/v1?key=%s&cx=%s&q=%s",
                GOOGLE_API_KEY, SEARCH_ENGINE_ID, query);

        try {
            Map<String, Object> response = restTemplate.getForObject(SEARCH_URL, Map.class);
            if (response == null) {
                System.err.println("Google API'den boş yanıt alındı.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            List<Map<String, Object>> items = new ArrayList<>();

            Object rawItems = response.get("items");
            if (rawItems instanceof List<?>) {
                for (Object itemObj : (List<?>) rawItems) {
                    if (itemObj instanceof Map<?, ?>) {
                        items.add((Map<String, Object>) itemObj);
                    }
                }
            }

            for (Map<String, Object> item : items) {
                Map<String, Object> news = new HashMap<>();

                String link = (String) item.get("link");
                Map<String, String> extracted = fetchNewsDataService.fetchNewsData(link);

                news.put("title", extracted.get("title"));
                news.put("summary", extracted.get("summary"));

                results.add(news);
                System.out.println(news);
            }

            return results;

        } catch (Exception e) {
            System.err.println("Google API isteği başarısız: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}