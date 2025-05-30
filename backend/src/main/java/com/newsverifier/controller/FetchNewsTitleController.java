package com.newsverifier.controller;

import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.service.GoogleCustomSearchService;
import com.newsverifier.service.PageTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FetchNewsTitleController {

    private final GoogleCustomSearchService googleCustomSearchService;
    private final PageTitleService pageTitleService;

    @PostMapping("/news/title")
    public PageTitleService.TitleResponse fetchNewsTitle(@RequestBody String newsText) {
        List<GoogleSearchResultItem> items = googleCustomSearchService.searchNews(newsText);

        if (items.isEmpty()) {
            return new PageTitleService.TitleResponse("N/A", "Google'dan link alınamadı");
        }

        String url = items.get(0).getLink();
        return pageTitleService.fetchSingleTitle(url);
    }
}
