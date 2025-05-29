from fastapi import FastAPI, Query
from fastapi.responses import JSONResponse
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeout
from urllib.parse import urlparse

app = FastAPI()

@app.get("/fetch-content")
def fetch_content(url: str = Query(..., description="Haber URL'si")):
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            page.goto(url, timeout=15000)
            page.wait_for_load_state('networkidle', timeout=5000)

            # Title çek
            title = page.title()

            # Temel içerik alanını bul (body text veya ana makale kısmı)
            # Sayfaya göre özelleştirilebilir
            main_text = page.locator("body").inner_text()
            main_text = main_text.strip().replace("\n", " ").replace("\r", "")

            browser.close()
            return JSONResponse(content={
                "title": title,
                "content": main_text[:5000]  # max 5000 karakter dön
            })

    except PlaywrightTimeout:
        return JSONResponse(status_code=504, content={"error": "Sayfa zaman aşımına uğradı."})
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": f"Hata oluştu: {str(e)}"})
