from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import spacy
from typing import List, Dict, Optional
import logging

# Loglama yapılandırması
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# FastAPI uygulamasını oluştur
app = FastAPI(title="News Verifier NER Service")

# CORS ayarları
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# spaCy modelini yükle (Transformer tabanlı)
try:
    nlp = spacy.load("tr_core_news_trf")
    logger.info("spaCy Transformer modeli başarıyla yüklendi")
except Exception as e:
    logger.error(f"Model yükleme hatası: {str(e)}")
    raise


class NewsAnalysisRequest(BaseModel):
    input_news: str
    comparison_news: str

class EntityMatch(BaseModel):
    entity_type: str
    input_entity: str
    comparison_entity: Optional[str]
    matches: bool
    explanation: str

class NewsAnalysisResponse(BaseModel):
    entity_comparisons: List[EntityMatch]
    similarity_score: float
    discrepancies: List[str]
    extracted_entities: Dict[str, List[str]]

def extract_entities(text: str) -> Dict[str, List[str]]:
    """Metinden varlıkları çıkarır."""
    doc = nlp(text)
    entities = {}
    
    for ent in doc.ents:
        if ent.label_ not in entities:
            entities[ent.label_] = []
        if ent.text not in entities[ent.label_]:
            entities[ent.label_].append(ent.text)
    
    return entities

def calculate_similarity(text1: str, text2: str) -> float:
    """İki metin arasındaki benzerliği hesaplar."""
    doc1 = nlp(text1)
    doc2 = nlp(text2)
    return doc1.similarity(doc2)

def find_best_match(entity: str, candidates: List[str]) -> Optional[str]:
    """Verilen varlık için en iyi eşleşmeyi bulur."""
    if not candidates:
        return None
    
    max_similarity = -1
    best_match = None
    entity_doc = nlp(entity)
    
    for candidate in candidates:
        candidate_doc = nlp(candidate)
        similarity = entity_doc.similarity(candidate_doc)
        if similarity > max_similarity:
            max_similarity = similarity
            best_match = candidate
    
    return best_match if max_similarity > 0.7 else None

@app.post("/analyze", response_model=NewsAnalysisResponse)
async def analyze_news(request: NewsAnalysisRequest):
    """İki haber metnini analiz eder ve karşılaştırır."""
    try:
        # Varlıkları çıkar
        input_entities = extract_entities(request.input_news)
        comparison_entities = extract_entities(request.comparison_news)
        
        # Karşılaştırmaları yap
        entity_comparisons = []
        discrepancies = []
        
        # Her varlık tipi için karşılaştırma yap
        all_types = set(list(input_entities.keys()) + list(comparison_entities.keys()))
        for entity_type in all_types:
            input_list = input_entities.get(entity_type, [])
            comparison_list = comparison_entities.get(entity_type, [])
            
            for input_entity in input_list:
                best_match = find_best_match(input_entity, comparison_list)
                matches = best_match == input_entity if best_match else False
                
                comparison = EntityMatch(
                    entity_type=entity_type,
                    input_entity=input_entity,
                    comparison_entity=best_match,
                    matches=matches,
                    explanation=generate_explanation(entity_type, input_entity, best_match, matches)
                )
                
                entity_comparisons.append(comparison)
                
                if not matches:
                    discrepancies.append(
                        f"{entity_type}: '{input_entity}' -> '{best_match if best_match else 'bulunamadı'}'"
                    )
        
        # Benzerlik skorunu hesapla
        similarity_score = calculate_similarity(request.input_news, request.comparison_news)
        
        return NewsAnalysisResponse(
            entity_comparisons=entity_comparisons,
            similarity_score=similarity_score,
            discrepancies=discrepancies,
            extracted_entities=input_entities
        )
        
    except Exception as e:
        logger.error(f"Analiz hatası: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Analiz sırasında bir hata oluştu: {str(e)}")

def generate_explanation(entity_type: str, input_entity: str, comparison_entity: Optional[str], matches: bool) -> str:
    """Karşılaştırma sonucu için açıklama üretir."""
    if matches:
        return f"{entity_type} varlığı her iki metinde de aynı: '{input_entity}'"
    elif comparison_entity:
        return f"{entity_type} varlığı farklı: '{input_entity}' yerine '{comparison_entity}' kullanılmış"
    else:
        return f"{entity_type} varlığı '{input_entity}' karşılaştırma metninde bulunamadı"
