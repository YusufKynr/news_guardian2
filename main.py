from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional
import logging
import re
from difflib import SequenceMatcher
import json

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

class NewsAnalysisRequest(BaseModel):
    input_news: str
    comparison_news: str

class EntityMatch(BaseModel):
    entity_type: str
    input_entity: str
    comparison_entity: Optional[str]
    matches: bool
    explanation: str
    confidence: float

class NewsAnalysisResponse(BaseModel):
    entity_comparisons: List[EntityMatch]
    similarity_score: float
    discrepancies: List[str]
    extracted_entities: Dict[str, List[str]]
    fact_check_summary: str

def extract_entities(text: str) -> Dict[str, List[str]]:
    """Metinden varlıkları çıkarır."""
    entities = {
        "KİŞİ": [],
        "YER": [],
        "KURUM": [],
        "TARİH": [],
        "SAYI": [],
        "PARA": [],
        "OLAY": []
    }
    
    # Kişi isimleri (büyük harfle başlayan 2+ kelime)
    person_pattern = r'\b[A-ZĞÜŞİÖÇ][a-zğüşıöç]+(?:\s+[A-ZĞÜŞİÖÇ][a-zğüşıöç]+)+\b'
    persons = re.findall(person_pattern, text)
    entities["KİŞİ"].extend(persons)
    
    # Yer isimleri
    location_pattern = r'\b(?:İstanbul|Ankara|İzmir|Bursa|Antalya|Adana|Konya|Gaziantep|Şanlıurfa|Mersin|Diyarbakır|Denizli|Samsun|Tekirdağ)\b'
    locations = re.findall(location_pattern, text)
    entities["YER"].extend(locations)
    
    # Kurumlar
    org_pattern = r'\b(?:[A-ZĞÜŞİÖÇ][A-ZĞÜŞİÖÇa-zğüşıöç]*\s*)+(?:Bakanlığı|Kurumu|Başkanlığı|Müdürlüğü|Üniversitesi|Hastanesi|Derneği|Vakfı|Şirketi|A\.Ş\.|Ltd\.|Holding)\b'
    orgs = re.findall(org_pattern, text)
    entities["KURUM"].extend(orgs)
    
    # Tarihler
    date_pattern = r'\b\d{1,2}[-./]\d{1,2}[-./]\d{2,4}\b|\b(?:Ocak|Şubat|Mart|Nisan|Mayıs|Haziran|Temmuz|Ağustos|Eylül|Ekim|Kasım|Aralık)\s+\d{4}\b'
    dates = re.findall(date_pattern, text)
    entities["TARİH"].extend(dates)
    
    # Sayılar
    number_pattern = r'\b(?:\d{1,3}(?:\.\d{3})*(?:,\d+)?|\d+(?:,\d+)?)\s*(?:bin|milyon|milyar|trilyon)?\b'
    numbers = re.findall(number_pattern, text)
    entities["SAYI"].extend(numbers)
    
    # Para birimleri
    money_pattern = r'\b(?:\d{1,3}(?:\.\d{3})*(?:,\d+)?|\d+(?:,\d+)?)\s*(?:TL|Dolar|Euro|₺|\$|€)\b'
    money = re.findall(money_pattern, text)
    entities["PARA"].extend(money)
    
    # Önemli olaylar (büyük harfle yazılan kelime grupları)
    event_pattern = r'\b[A-ZĞÜŞİÖÇ][A-ZĞÜŞİÖÇ\s]+\b'
    events = re.findall(event_pattern, text)
    entities["OLAY"].extend([e for e in events if len(e.split()) > 1])
    
    # Tekrar eden varlıkları temizle
    for key in entities:
        entities[key] = list(set(entities[key]))
    
    return entities

def calculate_similarity(text1: str, text2: str) -> float:
    """İki metin arasındaki benzerliği hesaplar."""
    return SequenceMatcher(None, text1.lower(), text2.lower()).ratio()

def find_best_match(entity: str, candidates: List[str]) -> tuple[Optional[str], float]:
    """Verilen varlık için en iyi eşleşmeyi ve benzerlik skorunu bulur."""
    if not candidates:
        return None, 0.0
    
    max_similarity = -1
    best_match = None
    
    for candidate in candidates:
        similarity = SequenceMatcher(None, entity.lower(), candidate.lower()).ratio()
        if similarity > max_similarity:
            max_similarity = similarity
            best_match = candidate
    
    return best_match, max_similarity

def generate_fact_check_summary(entity_comparisons: List[EntityMatch], similarity_score: float) -> str:
    """Analiz sonuçlarına göre bir özet metin oluşturur."""
    if similarity_score >= 0.8:
        confidence = "yüksek"
    elif similarity_score >= 0.5:
        confidence = "orta"
    else:
        confidence = "düşük"
    
    discrepancy_count = len([ec for ec in entity_comparisons if not ec.matches])
    
    if discrepancy_count == 0:
        return f"Analiz sonucunda haberin doğruluğu {confidence} güvenilirlikle teyit edilmiştir. Karşılaştırılan kaynaklarla %{(similarity_score * 100):.1f} oranında benzerlik göstermektedir."
    else:
        return f"Analiz sonucunda haberde {discrepancy_count} adet tutarsızlık tespit edilmiştir. Karşılaştırılan kaynaklarla %{(similarity_score * 100):.1f} oranında benzerlik göstermektedir. Lütfen farklılıklar bölümünü inceleyiniz."

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
        for entity_type in input_entities:
            input_list = input_entities[entity_type]
            comparison_list = comparison_entities.get(entity_type, [])
            
            for input_entity in input_list:
                best_match, confidence = find_best_match(
                    input_entity, comparison_list
                )
                
                matches = confidence > 0.8
                
                comparison = EntityMatch(
                    entity_type=entity_type,
                    input_entity=input_entity,
                    comparison_entity=best_match,
                    matches=matches,
                    confidence=confidence,
                    explanation=generate_explanation(
                        entity_type, input_entity, best_match, matches, confidence
                    )
                )
                
                entity_comparisons.append(comparison)
                
                if not matches and best_match:
                    discrepancies.append(
                        f"{entity_type}: '{input_entity}' -> '{best_match}' "
                        f"(Benzerlik: %{confidence * 100:.1f})"
                    )
                elif not matches:
                    discrepancies.append(
                        f"{entity_type}: '{input_entity}' karşılaştırma metninde bulunamadı"
                    )
        
        # Benzerlik skorunu hesapla
        similarity_score = calculate_similarity(
            request.input_news, request.comparison_news
        )
        
        # Özet metin oluştur
        fact_check_summary = generate_fact_check_summary(
            entity_comparisons, similarity_score
        )
        
        return NewsAnalysisResponse(
            entity_comparisons=entity_comparisons,
            similarity_score=similarity_score,
            discrepancies=discrepancies,
            extracted_entities=input_entities,
            fact_check_summary=fact_check_summary
        )
        
    except Exception as e:
        logger.error(f"Analiz hatası: {str(e)}")
        raise HTTPException(
            status_code=500,
            detail=f"Analiz sırasında bir hata oluştu: {str(e)}"
        )

def generate_explanation(
    entity_type: str,
    input_entity: str,
    comparison_entity: Optional[str],
    matches: bool,
    confidence: float
) -> str:
    """Karşılaştırma sonucu için açıklama üretir."""
    if matches:
        return (
            f"{entity_type} varlığı her iki metinde de aynı: "
            f"'{input_entity}' (Benzerlik: %{confidence * 100:.1f})"
        )
    elif comparison_entity:
        return (
            f"{entity_type} varlığı farklı: '{input_entity}' yerine "
            f"'{comparison_entity}' kullanılmış (Benzerlik: %{confidence * 100:.1f})"
        )
    else:
        return (
            f"{entity_type} varlığı '{input_entity}' karşılaştırma "
            f"metninde bulunamadı"
        ) 