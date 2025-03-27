package com.newsverifier.service;

import com.microsoft.playwright.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class FetchNewsDataService {
 
     public Map<String, String> fetchNewsData(String url) {
        Map<String, String> newsData = new HashMap<>();
    
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.navigate(url);
    
            // TITLE
            String title = null;
            Locator h1s = page.locator("h1");
            if (h1s.count() > 0) {
                title = h1s.first().textContent();
            } else {
                title = page.title();
            }
    
            // SUMMARY
            String summary = null;
            Locator metaDesc = page.locator("meta[name=description]");
            if (metaDesc.count() > 0) {
                summary = metaDesc.first().getAttribute("content");
            }
            if ((summary == null || summary.isEmpty())) {
                Locator fallback = page.locator(".summary, .lead, p");
                if (fallback.count() > 0) {
                    summary = fallback.first().textContent();
                }
            }
    
            newsData.put("title", title);
            newsData.put("summary", summary);
    
            browser.close();
        } catch (Exception e) {
            System.err.println("Playwright scraping hatası: " + e.getMessage());
        }
    
        return newsData;
    }


    
}
