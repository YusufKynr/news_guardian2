package com.newsverifier.service;

import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.model.search.GoogleSearchResultList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCustomSearchService {

    @Value("${google.api.key}") private String apiKey;
    @Value("${google.api.cx}")  private String cx;
    private final RestTemplate restTemplate;   // dışarıdan enjekte

    public List<GoogleSearchResultItem> searchNews(String query) {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme("https").host("www.googleapis.com").path("/customsearch/v1")
                .queryParam("key", apiKey)
                .queryParam("cx", cx)
                .queryParam("q", query)
                .queryParam("num", 10)
                .queryParam("fields", "items(title,snippet,link)")
                .encode()                // UTF-8
                .build()
                .toUri();

        try {
            GoogleSearchResultList res =
                    restTemplate.getForObject(uri, GoogleSearchResultList.class);
            return (res == null || res.getItems() == null) ? List.of() : res.getItems();
        } catch (Exception ex) {
            log.warn("Google search error for [{}] – {}", query, ex.getMessage());
            return List.of();
        }
    }
}
