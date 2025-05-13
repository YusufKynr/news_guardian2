package com.newsverifier.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class NewsAnalysisResponse {
    private List<EntityComparison> entityComparisons;
    private double similarityScore;
    private List<String> discrepancies;
    private Map<String, List<String>> extractedEntities;

    @Data
    public static class EntityComparison {
        private String entityType;
        private String inputEntity;
        private String comparisonEntity;
        private boolean matches;
        private String explanation;
    }
}