package com.newsverifier.service;

import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.model.response.SimilarNewsModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FetchSimilarNewsService {

    private final GoogleCustomSearchService google;

    public List<SimilarNewsModel> getSimilarNews(String text) {
        List<GoogleSearchResultItem> items = google.searchNews(text);

        return items.stream()
                .map(it -> new SimilarNewsModel(it.getTitle(), it.getSnippet(), 0.0))
                .collect(Collectors.toList());
    }
}
