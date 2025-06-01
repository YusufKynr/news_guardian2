from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import asyncio
import re
from playwright.async_api import async_playwright

app = FastAPI()

from transformers import AutoModelForTokenClassification, AutoTokenizer, pipeline

model_name = "savasy/bert-base-turkish-ner-cased"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForTokenClassification.from_pretrained(model_name)

ner_pipeline = pipeline("ner", model=model, tokenizer=tokenizer, aggregation_strategy="simple")

# Sayı tespit etmek için regex pattern'ları
NUMBER_PATTERN = re.compile(r'\b\d{1,3}(?:[.,]\d{3})*(?:[.,]\d+)?\b|\b\d+[.,]\d+\b|\b\d+\b')

# Saat tespit etmek için regex pattern'ları
TIME_PATTERN = re.compile(r'\b(?:[01]?\d|2[0-3])[:.][0-5]\d\b')

# Türkçe tarih tespit etmek için regex pattern'ları (14 Mayıs 2025 veya 14 Ekim formatı)
DATE_PATTERN = re.compile(r'\b(?:[1-9]|[12]\d|3[01])\s+(?:Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık)(?:\s+(?:19|20)\d{2})?\b', re.IGNORECASE)

def extract_numbers(text: str):
    """Metinden sayıları tespit eder"""
    numbers = []
    if not text:
        return numbers
    
    matches = NUMBER_PATTERN.finditer(text)
    found_numbers = set()  # Tekrarları önlemek için
    
    for match in matches:
        number_str = match.group().strip()
        if len(number_str) >= 1 and number_str not in found_numbers:
            numbers.append({"entity": "NUMBER", "word": number_str})
            found_numbers.add(number_str)
    
    return numbers

def extract_times(text: str):
    """Metinden saatleri tespit eder (12:46 veya 12.46 formatında)"""
    times = []
    if not text:
        return times
    
    matches = TIME_PATTERN.finditer(text)
    found_times = set()  # Tekrarları önlemek için
    
    for match in matches:
        time_str = match.group().strip()
        if time_str not in found_times:
            times.append({"entity": "TIME", "word": time_str})
            found_times.add(time_str)
    
    return times

def extract_dates(text: str):
    """Metinden Türkçe tarihleri tespit eder (14 Mayıs 2025 formatında)"""
    dates = []
    if not text:
        return dates
    
    matches = DATE_PATTERN.finditer(text)
    found_dates = set()  # Tekrarları önlemek için
    
    for match in matches:
        date_str = match.group().strip()
        if date_str not in found_dates:
            dates.append({"entity": "DATE", "word": date_str})
            found_dates.add(date_str)
    
    return dates

def extract_entities(text: str):
    try:
        # 1. Önce tarih tespiti yap
        date_entities = extract_dates(text)
        
        # 2. Tarih bulunan yerlerini metinden geçici olarak çıkar
        text_without_dates = text
        for date_entity in date_entities:
            text_without_dates = text_without_dates.replace(date_entity["word"], " [DATE_PLACEHOLDER] ")
        
        # 3. NER model ile varlık tespiti (temizlenmiş metinde)
        raw_entities = ner_pipeline(text_without_dates)
        filtered = [e for e in raw_entities if e["entity_group"] in {"PER", "ORG", "LOC"}]
        ner_entities = [{"entity": e["entity_group"], "word": e["word"]} for e in filtered]
        
        # 4. Saat tespiti yap (zaten temizlenmiş metinde)
        time_entities = extract_times(text_without_dates)
        
        # 5. Saat bulunan yerlerini de çıkar
        text_without_dates_times = text_without_dates
        for time_entity in time_entities:
            text_without_dates_times = text_without_dates_times.replace(time_entity["word"], " [TIME_PLACEHOLDER] ")
        
        # 6. Temizlenmiş metinde sayı tespiti yap
        number_entities = extract_numbers(text_without_dates_times)
        
        # 7. Tüm entity'leri birleştir
        all_entities = ner_entities + date_entities + time_entities + number_entities
        
        return all_entities
    except Exception as e:
        # Hata durumunda sadece tarih, saat ve sayı tespiti yap
        dates = extract_dates(text)
        text_without_dates = text
        for date_entity in dates:
            text_without_dates = text_without_dates.replace(date_entity["word"], " [DATE_PLACEHOLDER] ")
        
        times = extract_times(text_without_dates)
        text_without_dates_times = text_without_dates
        for time_entity in times:
            text_without_dates_times = text_without_dates_times.replace(time_entity["word"], " [TIME_PLACEHOLDER] ")
        
        numbers = extract_numbers(text_without_dates_times)
        return dates + times + numbers

class URLListRequest(BaseModel):
    urls: List[str]

class TextListRequest(BaseModel):
    texts: List[str]

async def get_title(url: str) -> str:
    try:
        async with async_playwright() as p:
            browser = await p.chromium.launch(
                headless=True,
                args=[
                    '--no-sandbox',
                    '--disable-dev-shm-usage',
                    '--disable-blink-features=AutomationControlled'
                ]
            )
            context = await browser.new_context(
                user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
            )
            page = await context.new_page()
            
            await page.goto(url, timeout=60000, wait_until='domcontentloaded')
            
            await page.wait_for_timeout(2000)
            
            title = await page.title()
            await browser.close()
            return title if title else "Başlık bulunamadı"
    except Exception as e:
        return f"Hata: {str(e)}"

@app.post("/get-titles")
async def get_titles(request: URLListRequest):
    titles = await asyncio.gather(*[get_title(url) for url in request.urls])
    result = []

    for url, title in zip(request.urls, titles):
        result.append({
            "url": url,
            "title": title
        })

    return result

@app.post("/extract-entities")
async def extract_entities_endpoint(request: TextListRequest):
    result = []
    
    for text in request.texts:
        entities = extract_entities(text)
        result.append({
            "text": text,
            "entities": entities
        })
    
    return result
