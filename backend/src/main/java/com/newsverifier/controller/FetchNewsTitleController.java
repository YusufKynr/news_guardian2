package com.newsverifier.controller;

import com.newsverifier.model.NamedEntityResult;
import com.newsverifier.model.NERApiResponseItem;
import com.newsverifier.model.NewsAnalysisRequest;
import com.newsverifier.model.TitleResponse;
import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.service.GoogleCustomSearchService;
import com.newsverifier.service.NamedEntityService;
import com.newsverifier.service.PageTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FetchNewsTitleController {

    private final GoogleCustomSearchService googleCustomSearchService;
    private final PageTitleService pageTitleService;
    private final NamedEntityService namedEntityService;

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message", "News Guardian API",
            "version", "1.0.0",
            "status", "Running",
            "endpoints", "POST /news/title, GET /status"
        );
    }

    @GetMapping("/news/title")
    public Map<String, String> newsInfo() {
        return Map.of(
            "message", "Bu endpoint POST method kullanır",
            "usage", "POST /news/title ile haber metnini gönderin",
            "contentType", "application/json",
            "example", "curl -X POST http://localhost:8000/news/title -H 'Content-Type: application/json' -d '{\"inputText\":\"haber metni\"}'"
        );
    }

    @PostMapping("/news/title")
    public TitleResponse fetchNewsTitle(@RequestBody NewsAnalysisRequest request) {
        String newsText = request.getInputText();
        
        if (newsText == null || newsText.trim().isEmpty()) {
            return new TitleResponse(
                "",
                List.of(),
                null,
                "Geçersiz metin",
                List.of(),
                "Metin boş veya geçersiz"
            );
        }
        
        // 1. Girilen metin için NER
        List<String> inputTexts = List.of(newsText);
        List<NERApiResponseItem> inputNER = namedEntityService.extractEntities(inputTexts);
        List<NamedEntityResult> inputEntities = inputNER.isEmpty() ? List.of() : inputNER.get(0).getEntities();

        // 2. Google Search
        List<GoogleSearchResultItem> items = googleCustomSearchService.searchNews(newsText);
        
        if (items.isEmpty()) {
            return new TitleResponse(
                newsText,
                inputEntities,
                null,
                "Google'dan sonuç alınamadı",
                List.of(),
                "Google aramada sonuç bulunamadı"
            );
        }

        String url = items.get(0).getLink();
        
        // 3. URL'den title çek
        TitleResponse titleResponse = pageTitleService.fetchSingleTitle(url);
        String pageTitle = titleResponse.getTitle();

        // 4. Title için NER
        List<String> titleTexts = List.of(pageTitle);
        List<NERApiResponseItem> titleNER = namedEntityService.extractEntities(titleTexts);
        List<NamedEntityResult> titleEntities = titleNER.isEmpty() ? List.of() : titleNER.get(0).getEntities();

        return new TitleResponse(
            newsText,           // Girilen haber metni
            inputEntities,      // Girilen haberin NER'leri
            url,                // Google'dan gelen URL
            pageTitle,          // URL'in title'ı
            titleEntities,      // Title'ın NER'leri
            "Başarıyla işlendi"
        );
    }
}
