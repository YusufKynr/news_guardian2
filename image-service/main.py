"""OCR servisi için FastAPI tabanlı REST API.

Bu modül, görüntülerden metin çıkarmak için Tesseract OCR kullanır ve
sonuçları JSON formatında döndürür.
"""

import os
import io
import pytesseract
from PIL import Image
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

# CORS Middleware yapılandırması
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Tesseract yapılandırması
TESSERACT_CMD = os.getenv(
    "TESSERACT_CMD",
    r"C:\Program Files\Tesseract-OCR\tesseract.exe"
)
TESSDATA_PREFIX = os.getenv(
    "TESSDATA_PREFIX",
    r"C:\Program Files\Tesseract-OCR\tessdata"
)

# Tesseract yollarını ayarla
pytesseract.pytesseract.tesseract_cmd = TESSERACT_CMD
os.environ["TESSDATA_PREFIX"] = TESSDATA_PREFIX

# İzin verilen dosya türleri
ALLOWED_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "bmp", "tiff"}


def validate_image(file: UploadFile) -> bool:
    """Dosya uzantısını kontrol et"""
    return file.filename.lower().split(".")[-1] in ALLOWED_EXTENSIONS


@app.post("/img/convert")
async def convert_image(file: UploadFile = File(...)):
    """Yüklenen görüntüyü işleyerek içindeki metni çıkarır.

    Args:
        file (UploadFile): İşlenecek görüntü dosyası

    Returns:
        dict: Çıkarılan metin veya hata mesajı
    """
    if not file:
        raise HTTPException(status_code=400, detail="Dosya yüklenmedi")
    if not validate_image(file):
        error_msg = (
            f"Desteklenmeyen dosya türü. İzin verilen türler: "
            f"{', '.join(ALLOWED_EXTENSIONS)}"
        )
        raise HTTPException(status_code=400, detail=error_msg)       
    try:
        # Dosya boyutu kontrolü (10MB)
        contents = await file.read()
        if len(contents) > 10 * 1024 * 1024:
            raise HTTPException(
                status_code=400,
                detail="Dosya boyutu 10MB'dan büyük olamaz"
            )
        image = Image.open(io.BytesIO(contents))
        text = pytesseract.image_to_string(image, lang="tur")    
        if not text.strip():
            return {
                "text": "",
                "message": "Görüntüden metin çıkarılamadı"
            }
        return {"text": text}   
    except Image.UnidentifiedImageError as exc:
        raise HTTPException(
            status_code=400, 
            detail="Geçersiz görüntü dosyası"
        ) from exc
    except pytesseract.TesseractNotFoundError as exc:
        raise HTTPException(
            status_code=500,
            detail="Tesseract OCR sisteme yüklenmemiş veya yapılandırma hatalı"
        ) from exc
    except Exception as exc:
        error_detail = f"Beklenmeyen bir hata oluştu: {str(exc)}"
        raise HTTPException(
            status_code=500, 
            detail=error_detail
        ) from exc

