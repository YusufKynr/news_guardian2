package com.newsverifier.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TitleResponse {
    private String inputText;                    // Girilen haber metni
    private List<NamedEntityResult> inputNER;   // Girilen haberin NER'leri
    private String url;                          // Google'dan gelen URL
    private String title;                        // URL'in title'ı
    private List<NamedEntityResult> titleNER;   // Title'ın NER'leri
    private String explanation;                  // Açıklama
}
