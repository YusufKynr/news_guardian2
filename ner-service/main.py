from fastapi import FastAPI, Query
from transformers import pipeline, AutoTokenizer, AutoModelForTokenClassification
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

# Model Yükleme
MODEL_NAME = "akdeniz27/bert-base-turkish-cased-ner"
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForTokenClassification.from_pretrained(MODEL_NAME)

ner_pipeline = pipeline("ner", model=model, tokenizer=tokenizer, aggregation_strategy="simple")


@app.get("/ner/extract")
def extract_entities(text: str = Query(..., description="Analiz edilecek metin")):
    """
    Belirtilen metindeki kişi, organizasyon, lokasyon gibi varlıkları tespit eder.
    """
    results = ner_pipeline(text)

    # Numpy float32 olan değerleri float() ile Python tipine çeviriyoruz
    cleaned_results = []
    for entity in results:
        entity["score"] = float(entity["score"])  # Skor değerini dönüştür
        cleaned_results.append(entity)

    return {"entities": cleaned_results}
