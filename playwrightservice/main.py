from fastapi import FastAPI, Query, Request
from fastapi.responses import JSONResponse
from playwright.async_api import async_playwright, TimeoutError as PlaywrightTimeout
from urllib.parse import urlparse
from typing import List
from pydantic import BaseModel
import logging
import json
import traceback
import asyncio
import sys

# Windows'ta event loop sorununu çözmek için
if sys.platform == "win32":
    # SelectorEventLoop genellikle Windows'ta subprocess'ler için daha stabildir
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())

# Logging konfigürasyonu
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

class UrlList(BaseModel):
    urls: List[str]

class TitleResult(BaseModel):
    url: str
    title: str
    error: str = None

@app.get("/")
async def read_root():
    return {"message": "Playwright Service is running!"}

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

# DEBUG ENDPOINT - Java'dan gelen raw request'i yakalamak için
@app.post("/debug-request")
async def debug_request(request: Request):
    """Java'dan gelen request'i debug etmek için"""
    try:
        # Raw body'yi al
        body = await request.body()
        body_str = body.decode('utf-8')
        
        logger.info(f"🔍 DEBUG REQUEST:")
        logger.info(f"📋 Headers: {dict(request.headers)}")
        logger.info(f"📄 Raw Body: {body_str}")
        logger.info(f"📏 Body Length: {len(body_str)}")
        
        # JSON parse dene
        try:
            parsed_json = json.loads(body_str)
            logger.info(f"✅ JSON Parse Başarılı: {parsed_json}")
            logger.info(f"🔑 JSON Keys: {list(parsed_json.keys()) if isinstance(parsed_json, dict) else 'Not a dict'}")
        except json.JSONDecodeError as e:
            logger.error(f"❌ JSON Parse Hatası: {e}")
        
        return {
            "success": True,
            "raw_body": body_str,
            "headers": dict(request.headers),
            "parsed": parsed_json if 'parsed_json' in locals() else None
        }
        
    except Exception as e:
        logger.error(f"💥 Debug endpoint hatası: {e}")
        return {"success": False, "error": str(e)}

@app.post("/fetch-titles")
async def fetch_titles(url_list: UrlList):
    """Verilen URL listesinden title'ları çeker"""
    logger.info("=== FETCH TITLES BAŞLADI (ASYNC) ===")
    logger.info(f"Gelen URL sayısı: {len(url_list.urls)}")
    logger.info(f"URL'ler: {url_list.urls}")
    
    results = []
    
    try:
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            logger.info("✅ Browser başlatıldı (async)")
            
            for i, url in enumerate(url_list.urls):
                logger.info(f"🔄 [{i+1}/{len(url_list.urls)}] İşleniyor: {url}")
                try:
                    page = await browser.new_page()
                    await page.goto(url, timeout=10000)
                    await page.wait_for_load_state('domcontentloaded', timeout=5000)
                    
                    title = await page.title()
                    logger.info(f"✅ Title alındı: {title[:50]}...")
                    results.append(TitleResult(url=url, title=title, error=None))
                    await page.close()
                    
                except PlaywrightTimeout:
                    error_msg = "Zaman aşımı"
                    logger.warning(f"⚠️ {url} - {error_msg}")
                    results.append(TitleResult(url=url, title="", error=error_msg))
                except Exception as e:
                    error_msg = str(e)[:100]
                    logger.error(f"❌ {url} - {error_msg} - Traceback: {traceback.format_exc()}")
                    results.append(TitleResult(url=url, title="", error=error_msg))
            
            await browser.close()
            logger.info("✅ Browser kapatıldı (async)")
            
    except Exception as e:
        tb = traceback.format_exc()
        logger.error(f"💥 Genel hata (async): {str(e)} - Traceback: {tb}")
        return JSONResponse(status_code=500, content={"error": f"Genel hata (async): {str(e)} - Traceback: {tb}"})
    
    success_count = len([r for r in results if not r.error])
    error_count = len([r for r in results if r.error])
    logger.info(f"🎯 İşlem tamamlandı. Başarılı: {success_count}, Hatalı: {error_count}")
    logger.info("=== FETCH TITLES BİTTİ (ASYNC) ===")
    
    return results

@app.get("/fetch-content")
async def fetch_content(url: str = Query(..., description="Haber URL'si")):
    logger.info(f"fetch-content endpoint'ine istek geldi: {url}")
    try:
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            page = await browser.new_page()
            await page.goto(url, timeout=15000)
            await page.wait_for_load_state('networkidle', timeout=5000)

            # Title çek
            title = await page.title()
            logger.info(f"Title alındı: {title}")

            # Temel içerik alanını bul (body text veya ana makale kısmı)
            # Sayfaya göre özelleştirilebilir
            main_text = await page.locator("body").inner_text()
            main_text = main_text.strip().replace("\n", " ").replace("\r", "")

            await browser.close()
            return JSONResponse(content={
                "title": title,
                "content": main_text[:5000]  # max 5000 karakter dön
            })

    except PlaywrightTimeout:
        logger.error(f"Sayfa zaman aşımına uğradı: {url}")
        return JSONResponse(status_code=504, content={"error": "Sayfa zaman aşımına uğradı."})
    except Exception as e:
        tb = traceback.format_exc()
        logger.error(f"Hata oluştu: {str(e)} - Traceback: {tb}")
        return JSONResponse(status_code=500, content={"error": f"Hata oluştu: {str(e)} - Traceback: {tb}"})

# Test endpoint'i
@app.post("/test")
async def test_endpoint(data: dict):
    logger.info(f"Test endpoint - Gelen data: {data}")
    return {"success": True, "received": data}
