package com.newsverifier.service;

import com.newsverifier.model.NewsAnalysisResponse;
import com.newsverifier.model.NewsAnalysisResponse.EntityComparison;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.core.turkish.Turkish;
import zemberek.ner.NER;
import zemberek.ner.NERecognizer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NewsAnalysisService {
    private final TurkishMorphology morphology;
    private final NERecognizer recognizer;
    private final TurkishTokenizer tokenizer;

    public NewsAnalysisService() throws IOException {
        this.morphology = TurkishMorphology.createWithDefaults();
        this.recognizer = NER.loadModel("models/ner/model"); // Model dosyasının yolu
        this.tokenizer = TurkishTokenizer.DEFAULT;
    }

    public NewsAnalysisResponse analyzeNews(String inputNews, String comparisonNews) {
        NewsAnalysisResponse response = new NewsAnalysisResponse();

        // Varlıkları çıkar
        Map<String, List<String>> inputEntities = extractEntities(inputNews);
        Map<String, List<String>> comparisonEntities = extractEntities(comparisonNews);

        // Varlıkları karşılaştır
        List<EntityComparison> comparisons = compareEntities(inputEntities, comparisonEntities);

        // Tutarsızlıkları bul
        List<String> discrepancies = findDiscrepancies(comparisons);

        // Benzerlik skoru hesapla
        double similarityScore = calculateSimilarityScore(comparisons);

        response.setEntityComparisons(comparisons);
        response.setDiscrepancies(discrepancies);
        response.setSimilarityScore(similarityScore);
        response.setExtractedEntities(inputEntities);

        return response;
    }

    private Map<String, List<String>> extractEntities(String text) {
        Map<String, List<String>> entities = new HashMap<>();

        try {
            // Metni normalize et
            String normalizedText = text.toLowerCase(Turkish.LOCALE)
                    .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // Varlıkları tanı
            var sentences = tokenizer.tokenize(normalizedText);
            for (var sentence : sentences) {
                var nerResults = recognizer.findNamedEntities(sentence);
                for (var nerResult : nerResults) {
                    String type = nerResult.getType().toString();
                    String content = String.join(" ", nerResult.getTokens());

                    entities.computeIfAbsent(type, k -> new ArrayList<>())
                            .add(content);
                }
            }
        } catch (Exception e) {
            log.error("Varlık çıkarma hatası: ", e);
        }

        return entities;
    }

    private List<EntityComparison> compareEntities(
            Map<String, List<String>> inputEntities,
            Map<String, List<String>> comparisonEntities) {

        List<EntityComparison> comparisons = new ArrayList<>();

        // Her varlık tipi için karşılaştırma yap
        Set<String> allTypes = new HashSet<>();
        allTypes.addAll(inputEntities.keySet());
        allTypes.addAll(comparisonEntities.keySet());

        for (String type : allTypes) {
            List<String> inputList = inputEntities.getOrDefault(type, Collections.emptyList());
            List<String> comparisonList = comparisonEntities.getOrDefault(type, Collections.emptyList());

            // Her varlığı karşılaştır
            for (String inputEntity : inputList) {
                EntityComparison comparison = new EntityComparison();
                comparison.setEntityType(type);
                comparison.setInputEntity(inputEntity);

                // En benzer varlığı bul
                Optional<String> bestMatch = findBestMatch(inputEntity, comparisonList);

                if (bestMatch.isPresent()) {
                    comparison.setComparisonEntity(bestMatch.get());
                    comparison.setMatches(inputEntity.equalsIgnoreCase(bestMatch.get()));
                    comparison.setExplanation(generateExplanation(comparison));
                } else {
                    comparison.setMatches(false);
                    comparison.setExplanation("Karşılaştırma haberinde bulunamadı");
                }

                comparisons.add(comparison);
            }
        }

        return comparisons;
    }

    private Optional<String> findBestMatch(String entity, List<String> candidates) {
        return candidates.stream()
                .min(Comparator
                        .comparingInt(candidate -> levenshteinDistance(entity.toLowerCase(), candidate.toLowerCase())));
    }

    private List<String> findDiscrepancies(List<EntityComparison> comparisons) {
        return comparisons.stream()
                .filter(c -> !c.isMatches())
                .map(c -> String.format("%s: '%s' -> '%s' (%s)",
                        c.getEntityType(),
                        c.getInputEntity(),
                        c.getComparisonEntity(),
                        c.getExplanation()))
                .collect(Collectors.toList());
    }

    private double calculateSimilarityScore(List<EntityComparison> comparisons) {
        if (comparisons.isEmpty()) {
            return 0.0;
        }

        long matchCount = comparisons.stream()
                .filter(EntityComparison::isMatches)
                .count();

        return (double) matchCount / comparisons.size();
    }

    private String generateExplanation(EntityComparison comparison) {
        if (comparison.isMatches()) {
            return "Varlıklar eşleşiyor";
        }

        return String.format("Varlıklar farklı: '%s' yerine '%s' kullanılmış",
                comparison.getInputEntity(),
                comparison.getComparisonEntity());
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[s1.length()][s2.length()];
    }
}