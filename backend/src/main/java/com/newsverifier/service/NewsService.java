package com.newsverifier.service;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Service
public class NewsService {

    private static final String OCR_APIURL = "http://localhost:5000/img/convert"; // ✅ FastAPI'nin doğru endpointi

    public List<Map<String, Object>> uploadImageNews(MultipartFile file) {
        RestTemplate restTemplate = new RestTemplate();

        try {
            // ✅ Resmi byte dizisine çevir
            byte[] imageBytes = file.getBytes();

            // ✅ Multipart request için `MultiValueMap` kullanıyoruz
            MultiValueMap<String, Object> requestPayload = new LinkedMultiValueMap<>();
            requestPayload.add("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();  // Orijinal dosya adını koru
                }
            });

            //Header ayarlamaları (multipart/form-data)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // HTTP isteği oluştur
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);

            // FastAPI'ye resmi POST ediyoruz
            ResponseEntity<Map> response = restTemplate.postForEntity(OCR_APIURL, requestEntity, Map.class);

            // FastAPI’den cevap geldiyse, haberi arama işlemi yap
            if (response.getBody() != null && response.getBody().containsKey("text")) {
                String extractedText = (String) response.getBody().get("text");
                return getSimilarNews(extractedText);
            }

        } catch (Exception e) {
            throw new RuntimeException("Dosya işlenirken hata oluştu!", e);
        }

        return Collections.emptyList();  // OCR başarısızsa boş liste dön
    }

    // Haberleri Google API ile arayan fonksiyon (daha önce eklediğin)
    public List<Map<String, Object>> getSimilarNews(String query) {
        RestTemplate restTemplate = new RestTemplate(); 
        String SEARCH_URL = "https://www.googleapis.com/customsearch/v1?key=dcaa39e1e5093a734cdec715bce480980cc5e24d&cx=437d145e403364572&q=" + query;
        
        Map<String, Object> response = restTemplate.getForObject(SEARCH_URL, Map.class);

        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        if (items != null) {
            for (Map<String, Object> item : items) {
                Map<String, Object> news = new HashMap<>();
                news.put("title", (String) item.get("title"));
                news.put("link", (String) item.get("link"));
                news.put("snippet", (String) item.get("snippet"));
                results.add(news);
            }
        }

        return results;
    }
}
