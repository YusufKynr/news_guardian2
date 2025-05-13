package com.newsverifier.model;

import lombok.Data;
 
@Data
public class NewsAnalysisRequest {
    private String inputNews;
    private String comparisonNews;
}