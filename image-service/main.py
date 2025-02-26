import pytesseract
import os
import io
from PIL import Image

from fastapi import FastAPI, File, UploadFile
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

# CORS Middleware ekleme
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # Sadece frontend'in çalıştığı adres
    allow_credentials=True,
    allow_methods=["*"],  # Tüm HTTP metodlarına izin ver
    allow_headers=["*"],  # Tüm başlıklara izin ver
)

# Tesseract'ın tam yolunu belirt
pytesseract.pytesseract.tesseract_cmd = r"C:\Program Files\Tesseract-OCR\tesseract.exe"

# tessdata klasörünün yolunu belirtiyoruz
os.environ["TESSDATA_PREFIX"] = r"C:\Program Files\Tesseract-OCR\tessdata"


@app.post("/img/convert")
async def convert_image(file: UploadFile = File(...)):
    try:
        image = Image.open(io.BytesIO(await file.read()))
        text = pytesseract.image_to_string(image, lang="tur") 

        return {"text": text}
    except Exception as e:
        return {"error": str(e)}

