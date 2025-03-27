package com.newsverifier.controller;

import java.util.List;
import java.util.Map;

import com.newsverifier.service.SimilarNewsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/similar")
@CrossOrigin(origins = "http://localhost:3000")
public class SimilarNewsController {
    
    SimilarNewsService newsService = new SimilarNewsService();

    public SimilarNewsController(SimilarNewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/similar")
    public List<Map<String, Object>> getSimilarNews(@RequestParam String query) {
        return newsService.getSimilarNews(query);
    }
}
