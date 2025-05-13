package com.newsverifier.controller;

import com.newsverifier.model.NewsAnalysisRequest;
import com.newsverifier.model.NewsAnalysisResponse;
import com.newsverifier.service.NewsAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyze")
@CrossOrigin(origins = "${cors.allowed-origins}")
@RequiredArgsConstructor
public class NewsAnalysisController {

    private final NewsAnalysisService newsAnalysisService;

    @PostMapping
    public NewsAnalysisResponse analyzeNews(@RequestBody NewsAnalysisRequest request) {
        return newsAnalysisService.analyzeNews(
                request.getInputNews(),
                request.getComparisonNews());
    }
}