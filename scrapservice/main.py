from fastapi import FastAPI
from playwright.sync_api import sync_playwright

app = FastAPI()

def get_page_title_internal(url):
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            page.goto(url, timeout=15000)
            title = page.title()
        except Exception as e:
            title = f"Hata oluştu: {e}"
        browser.close()
        return title

@app.get("/get-title")
async def get_title_endpoint(url: str):
    return {"title": get_page_title_internal(url)}

# Örnek kullanım (Artık doğrudan çalıştırılmayacak, uvicorn ile servis edilecek)
# if __name__ == "__main__":
#     url = input("Başlığını çekmek istediğiniz URL'yi girin: ")
#     print("Sayfa Başlığı:", get_page_title_internal(url))
