package com.farmlink.api.router.engine;

import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.entity.AgriIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleBasedClassifierTest {

    private RuleBasedClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new RuleBasedClassifier();
    }

    @Test
    @DisplayName("Classifies MARKET_PRICE_INQUIRY correctly")
    void testMarketPriceInquiry() {
        String query = "What is the market price of tomato in Guntur?";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of("guntur"), List.of(), List.of(), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertFalse(scores.isEmpty());
        assertEquals(AgriIntent.MARKET_PRICE_INQUIRY, scores.get(0).intent());
        assertTrue(scores.get(0).score() >= 0.70);
    }

    @Test
    @DisplayName("Classifies MARKET_TREND_INQUIRY with trend keywords")
    void testMarketTrendInquiry() {
        String query = "Is the tomato price increasing this week?";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of(), List.of(), List.of(), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertEquals(AgriIntent.MARKET_TREND_INQUIRY, scores.get(0).intent());
    }

    @Test
    @DisplayName("Classifies PEST_MANAGEMENT correctly")
    void testPestManagement() {
        String query = "How to control fruit borer and spray for caterpillars in cotton?";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("cotton"), List.of(), List.of(), List.of(), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertEquals(AgriIntent.PEST_MANAGEMENT, scores.get(0).intent());
    }

    @Test
    @DisplayName("Classifies PLANT_DISEASE_INQUIRY correctly")
    void testPlantDiseaseInquiry() {
        String query = "Tomato leaves turning yellow with leaf curl disease";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of(), List.of(), List.of(), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertEquals(AgriIntent.PLANT_DISEASE_INQUIRY, scores.get(0).intent());
    }

    @Test
    @DisplayName("Classifies FERTILIZER_NUTRIENT correctly")
    void testFertilizerNutrient() {
        String query = "What is the recommended urea and DAP dosage for wheat per acre?";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("wheat"), List.of(), List.of(), List.of("1 acre"), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertEquals(AgriIntent.FERTILIZER_NUTRIENT, scores.get(0).intent());
    }

    @Test
    @DisplayName("Classifies WEATHER_INQUIRY correctly")
    void testWeatherInquiry() {
        String query = "Weather forecast and rainfall prediction for Warangal";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of(), List.of("warangal"), List.of(), List.of(), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertEquals(AgriIntent.WEATHER_INQUIRY, scores.get(0).intent());
    }

    @Test
    @DisplayName("Classifies FARM_ECONOMICS_INQUIRY correctly")
    void testFarmEconomicsInquiry() {
        String query = "Calculate my farm profit and production cost per acre for paddy";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("paddy"), List.of(), List.of(), List.of("1 acre"), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        assertEquals(AgriIntent.FARM_ECONOMICS_INQUIRY, scores.get(0).intent());
    }

    @Test
    @DisplayName("Classifies UNSUPPORTED_OR_UNCLEAR for greetings")
    void testUnsupportedGreeting() {
        String query = "Hello, who are you?";
        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, ExtractedEntitiesDto.empty());
        assertEquals(AgriIntent.UNSUPPORTED_OR_UNCLEAR, scores.get(0).intent());
    }

    @Test
    @DisplayName("Scores are strictly clamped between 0.0 and 1.0")
    void testScoreClamping() {
        String query = "price price price rate modal price of tomato in guntur mandi";
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of("guntur"), List.of("guntur mandi"), List.of(), Collections.emptyMap()
        );

        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);
        for (RuleBasedClassifier.CandidateScore score : scores) {
            assertTrue(score.score() >= 0.0 && score.score() <= 1.0, "Score must be clamped [0, 1]: " + score.score());
        }
    }
}

