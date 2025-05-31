package com.newsverifier.controller;

import com.newsverifier.model.NamedEntityResult;
import com.newsverifier.model.NERApiResponseItem;
import com.newsverifier.model.TitleResponse;
import com.newsverifier.model.search.GoogleSearchResultItem;
import com.newsverifier.service.GoogleCustomSearchService;
import com.newsverifier.service.NamedEntityService;
import com.newsverifier.service.PageTitleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FetchNewsTitleController {

    private final GoogleCustomSearchService googleCustomSearchService;
    private final PageTitleService pageTitleService;
    private final NamedEntityService namedEntityService;

    @PostMapping("/news/title")
    public TitleResponse fetchNewsTitle(@RequestBody String newsText) {
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
