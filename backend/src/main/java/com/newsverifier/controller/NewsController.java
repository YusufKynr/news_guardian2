package com.newsverifier.controller;

import com.newsverifier.service.NewsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController 
@RequestMapping("/news")
@CrossOrigin(origins = "http://localhost:3000") // Allow requests from the frontend
public class NewsController {
    
    private NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/similar")
    public List<Map<String, Object>> getSimilarNews(@RequestParam String query) {
        return newsService.getSimilarNews(query);
    }

    @PostMapping("/uploadImg")
    public List<Map<String, Object>> uploadImageNews
    (@RequestParam MultipartFile file) {
        return newsService.uploadImageNews(file);
    }
}

