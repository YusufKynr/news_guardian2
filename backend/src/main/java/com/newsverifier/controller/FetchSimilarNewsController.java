package com.newsverifier.controller;

import com.newsverifier.model.response.SimilarNewsModel;
import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.service.FetchSimilarNewsService;
import com.newsverifier.service.GoogleCustomSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FetchSimilarNewsController {

    private final FetchSimilarNewsService fetchSimilarNewsService;
    private final GoogleCustomSearchService googleCustomSearchService;

    @PostMapping("/news/links")
    public List<String> fetchNewsLinks(@RequestBody String newsText) {
        return googleCustomSearchService.searchNews(newsText).stream()
                .map(GoogleSearchResultItem::getLink)   // sadece link alanı
                .toList();
    }

}
