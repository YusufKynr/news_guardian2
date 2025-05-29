package com.newsverifier.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarNewsModel { //bu model frontend de gözükecek
    private  String title;
    private  String snippet; // sadece UI için
    private  double similarityScore;

}
