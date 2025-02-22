package com.newsverifier.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class NewsService {

    private static final String API_KEY = "AIzaSyAnDu0ZWjSxxH4QT3H1Xe8uEjm62UcH7A4";
    private static final String CSE_ID = "437d145e403364572";
    private static final String SEARCH_URL = "https://www.googleapis.com/customsearch/v1?key=" + API_KEY + "&cx=" + CSE_ID + "&q=";

    public List<Map<String, Object>> getSimilarNews(String query) {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(SEARCH_URL + query, Map.class);

        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        if (items != null) {
            for (Map<String, Object> item : items) {
                Map<String, String> news = new HashMap<>();
                news.put("title", (String) item.get("title"));
                news.put("link", (String) item.get("link"));
                news.put("snippet", (String) item.get("snippet"));
                results.add(response);
            }
        }
        return results;
    }
}
