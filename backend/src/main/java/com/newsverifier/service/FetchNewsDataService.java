package com.newsverifier.service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FetchNewsDataService {
    private static final Logger logger = LoggerFactory.getLogger(FetchNewsDataService.class);
    private static final Pattern CONTENT_SELECTORS = Pattern
            .compile("article|main|[class*=content]|[class*=article]|[class*=story]");
    private static final int PAGE_TIMEOUT = 30000; // 30 saniye
    private static final int NAVIGATION_TIMEOUT = 20000; // 20 saniye

    @Value("${fastapi.ocr.url}")
    private String fastapiOcrUrl;

    private final RestTemplate restTemplate;

    public Map<String, String> fetchNewsData(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL boş olamaz");
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(PAGE_TIMEOUT));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setJavaScriptEnabled(true)
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36")
                    .setExtraHTTPHeaders(Map.of(
                            "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                            "Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
                            "Accept-Encoding", "gzip, deflate, br"))
                    .setLocale("tr-TR")
                    .setTimezoneId("Europe/Istanbul"));
                    
            context.route("**/*", route -> {
                String type = route.request().resourceType();
                if (type.equals("image") || type.equals("font") || type.equals("stylesheet") || type.equals("media")) {
                    route.abort();
                } else {
                    route.resume();
                }
            });

            Page page = context.newPage();
            page.setDefaultTimeout(PAGE_TIMEOUT);
            page.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT);

            try {
                // Sayfa yüklenirken oluşabilecek hataları yakala
                Response response = page.navigate(url, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(NAVIGATION_TIMEOUT));

                if (response == null || !response.ok()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Sayfa yüklenemedi: " + (response != null ? response.status() : "Yanıt alınamadı"));
                }

                // Sayfa yüklenene kadar bekle
                try {
                    page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                            new Page.WaitForLoadStateOptions().setTimeout(NAVIGATION_TIMEOUT));
                } catch (TimeoutError e) {
                    logger.warn("Sayfa tam olarak yüklenemedi, mevcut içerikle devam ediliyor: {}", url);
                }

                // Reklamları ve gereksiz elementleri kaldır
                page.evaluate("() => {" +
                        "document.querySelectorAll('iframe,img,video,script,style,link,meta,svg,button').forEach(e => e.remove());"
                        +
                        "document.querySelectorAll('[class*=\"ad\"],[class*=\"reklam\"],[id*=\"ad\"],[id*=\"reklam\"]').forEach(e => e.remove());"
                        +
                        "}");

                // İçerik alanını bul
                String content = page.evaluate("() => {" +
                        "const selectors = ['article', 'main', '[class*=\"content\"]', '[class*=\"article\"]', '[class*=\"story\"]'];"
                        +
                        "for (const selector of selectors) {" +
                        "  const element = document.querySelector(selector);" +
                        "  if (element) {" +
                        "    return element.textContent.trim();" +
                        "  }" +
                        "}" +
                        "return document.body.textContent.trim();" +
                        "}").toString();

                // Başlığı bul
                String title = page.evaluate("() => {" +
                        "const h1 = document.querySelector('h1');" +
                        "if (h1) return h1.textContent.trim();" +
                        "const ogTitle = document.querySelector('meta[property=\"og:title\"]');" +
                        "if (ogTitle) return ogTitle.getAttribute('content');" +
                        "return document.title.trim();" +
                        "}").toString();

                // Metni temizle ve karakter kodlamasını düzelt

                content = cleanText(content);
                title = cleanText(title);

                if (content.isEmpty() && title.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Sayfadan içerik alınamadı");
                }

                Map<String, String> result = new HashMap<>();
                result.put("title", title);
                result.put("content", content);
                result.put("summary", content.substring(0, Math.min(content.length(), 500)) + "...");

                return result;
            } catch (TimeoutError e) {
                logger.error("Sayfa yüklenirken zaman aşımı: {}", url, e);
                throw new ResponseStatusException(
                        HttpStatus.REQUEST_TIMEOUT,
                        "Sayfa yüklenirken zaman aşımına uğradı");
            } catch (PlaywrightException e) {
                logger.error("Playwright hatası: {}", url, e);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Sayfa işlenirken bir hata oluştu: " + e.getMessage());
            } finally {
                try {
                    page.close();
                    context.close();
                    browser.close();
                } catch (Exception e) {
                    logger.warn("Kaynaklar kapatılırken hata oluştu", e);
                }
            }
        } catch (Exception e) {
            logger.error("Haber verisi çekilemedi. URL: {}, Hata: {}", url, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Haber verisi alınırken bir hata oluştu: " + e.getMessage());
        }
    }

    private String cleanText(String text) {
        return text
                .replaceAll("\\s+", " ")
                .replaceAll("\\n+", " ")
                .replaceAll("\\t+", " ")
                .replaceAll("\\r+", " ")
                .trim();
    }
}
