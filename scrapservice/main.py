from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import asyncio
from playwright.async_api import async_playwright

app = FastAPI()

class URLListRequest(BaseModel):
    urls: List[str]

async def get_title(url: str) -> str:
    try:
        async with async_playwright() as p:
            browser = await p.chromium.launch(headless=True)
            page = await browser.new_page()
            await page.goto(url, timeout=15000)
            title = await page.title()
            await browser.close()
            return title
    except Exception as e:
        return f"Hata: {e}"

@app.post("/get-titles")
async def get_titles(request: URLListRequest):
    titles = await asyncio.gather(*[get_title(url) for url in request.urls])
    return [{"url": u, "title": t} for u, t in zip(request.urls, titles)]
