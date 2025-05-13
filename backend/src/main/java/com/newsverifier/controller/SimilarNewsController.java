package com.newsverifier.controller;

import java.util.List;
import java.util.Map;

import com.newsverifier.service.SimilarNewsService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:3000}")
@RestController
@RequestMapping("/similar")
@RequiredArgsConstructor
public class SimilarNewsController {
    private static final Logger logger = LoggerFactory.getLogger(SimilarNewsController.class);

    private final SimilarNewsService newsService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getSimilarNews(@RequestParam String query) {
        logger.info("Benzer haberler aranıyor. Sorgu: {}", query);
        try {
            List<Map<String, Object>> results = newsService.getSimilarNews(query);
            logger.info("Benzer haberler bulundu. Sonuç sayısı: {}", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Benzer haberler aranırken hata oluştu: {}", e.getMessage(), e);
            throw e;
        }
    }
}
