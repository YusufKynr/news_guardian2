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
import java.util.HashMap;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequiredArgsConstructor
public class FetchNewsTitleController {

    private final GoogleCustomSearchService googleCustomSearchService;
    private final PageTitleService pageTitleService;
    private final NamedEntityService namedEntityService;
    private static final Logger log = LoggerFactory.getLogger(FetchNewsTitleController.class);

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
        
        // 5. Entity tiplerini normalize et (girilen haberi referans al)
        List<NamedEntityResult> normalizedTitleEntities = normalizeEntityTypes(inputEntities, titleEntities);

        return new TitleResponse(
            newsText,           // Girilen haber metni
            inputEntities,      // Girilen haberin NER'leri
            url,                // Google'dan gelen URL
            pageTitle,          // URL'in title'ı
            normalizedTitleEntities, // Normalize edilmiş title NER'leri
            "Başarıyla işlendi"
        );
    }
    
    /**
     * Bulunan haberdeki entity tiplerini girilen haberi referans alarak normalize eder
     */
    private List<NamedEntityResult> normalizeEntityTypes(List<NamedEntityResult> inputEntities, List<NamedEntityResult> titleEntities) {
        // Girilen haberdeki entity'leri word bazında map'e dönüştür
        Map<String, String> inputEntityMap = new HashMap<>();
        for (NamedEntityResult entity : inputEntities) {
            if (entity.getWord() != null) {
                inputEntityMap.put(entity.getWord().toLowerCase().trim(), entity.getEntity());
            }
        }
        
        // Title entity'lerini normalize et
        List<NamedEntityResult> normalizedEntities = new ArrayList<>();
        for (NamedEntityResult titleEntity : titleEntities) {
            if (titleEntity.getWord() != null) {
                String normalizedWord = titleEntity.getWord().toLowerCase().trim();
                
                // Aynı kelime girilen haberde var mı?
                if (inputEntityMap.containsKey(normalizedWord)) {
                    // Girilen haberdeki tipi kullan
                    NamedEntityResult normalized = new NamedEntityResult();
                    normalized.setWord(titleEntity.getWord());
                    normalized.setEntity(inputEntityMap.get(normalizedWord));
                    normalizedEntities.add(normalized);
                    
                    log.info("Entity normalize edildi: {} -> {} (referans: girilen haber)", 
                        titleEntity.getWord() + " (" + titleEntity.getEntity() + ")",
                        titleEntity.getWord() + " (" + inputEntityMap.get(normalizedWord) + ")");
                } else {
                    // Girilen haberde yok, orijinal tipi kullan
                    normalizedEntities.add(titleEntity);
                }
            }
        }
        
        return normalizedEntities;
    }
}
