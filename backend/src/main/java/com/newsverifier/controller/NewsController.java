package com.newsverifier.controller;

import com.newsverifier.service.NewsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "http://localhost:3000")
public class NewsController {

    
    private NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/similar")
    public List<Map<String, Object>> getSimilarNews(@RequestParam String query) {
        return newsService.getSimilarNews(query);
    }
}
