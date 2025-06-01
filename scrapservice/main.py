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

def extract_entities(text: str):
    try:
        # 1. NER model ile varlık tespiti
        raw_entities = ner_pipeline(text)
        filtered = [e for e in raw_entities if e["entity_group"] in {"PER", "ORG", "LOC"}]
        ner_entities = [{"entity": e["entity_group"], "word": e["word"]} for e in filtered]
        
        # 2. Sayı tespiti
        number_entities = extract_numbers(text)
        
        # 3. Tüm entity'leri birleştir
        all_entities = ner_entities + number_entities
        
        return all_entities
    except Exception as e:
        # Hata durumunda sadece sayı tespiti yap
        return extract_numbers(text)

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
