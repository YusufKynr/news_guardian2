from playwright.sync_api import sync_playwright

def get_page_title(url):
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

# Örnek kullanım
if __name__ == "__main__":
    url = input("Başlığını çekmek istediğiniz URL'yi girin: ")
    print("Sayfa Başlığı:", get_page_title(url))
