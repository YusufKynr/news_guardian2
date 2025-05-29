package com.newsverifier.service;

import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.model.response.SimilarNewsModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FetchSimilarNewsService {

    private final GoogleCustomSearchService google;
    private final PlaywrightService playwrightService;

    public List<SimilarNewsModel> getSimilarNews(String text) {
        List<GoogleSearchResultItem> items = google.searchNews(text);

        return items.stream()
                .map(it -> new SimilarNewsModel(it.getTitle(), it.getSnippet(), 0.0))
                .collect(Collectors.toList());
    }
    
    public List<SimilarNewsModel> getSimilarNewsWithPlaywright(String text) {
        log.info("getSimilarNewsWithPlaywright başladı: {}", text);
        
        // Önce URL'leri al
        List<GoogleSearchResultItem> searchResults = google.searchNews(text);
        log.info("Google search'ten {} sonuç alındı", searchResults.size());
        
        List<String> urls = searchResults.stream()
                .map(GoogleSearchResultItem::getLink)
                .toList();
        
        log.info("URL'ler: {}", urls);
        
        if (urls.isEmpty()) {
            log.warn("URL listesi boş!");
            return List.of();
        }
        
        // Playwright ile title'ları al
        log.info("Playwright servisine istek gönderiliyor...");
        List<PlaywrightService.TitleResult> titleResults = playwrightService.fetchTitlesFromUrls(urls);
        log.info("Playwright'ten {} title sonucu alındı", titleResults.size());
        
        // Sonuçları birleştir
        List<SimilarNewsModel> finalResults = titleResults.stream()
                .filter(result -> result.getError() == null || result.getError().isEmpty())
                .map(result -> {
                    log.info("Title result: URL={}, Title={}, Error={}", result.getUrl(), result.getTitle(), result.getError());
                    
                    // Orjinal search result'dan snippet bul
                    String snippet = searchResults.stream()
                            .filter(item -> item.getLink().equals(result.getUrl()))
                            .findFirst()
                            .map(GoogleSearchResultItem::getSnippet)
                            .orElse("");
                    
                    return new SimilarNewsModel(result.getTitle(), snippet, 0.0);
                })
                .collect(Collectors.toList());
        
        log.info("Final sonuç count: {}", finalResults.size());
        return finalResults;
    }
}
