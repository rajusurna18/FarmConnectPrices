package com.farmlink.api.router.engine;

import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.entity.AgriIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntentDisambiguatorTest {

    private IntentDisambiguator disambiguator;

    @BeforeEach
    void setUp() {
        disambiguator = new IntentDisambiguator();
    }

    @Test
    @DisplayName("Single clear top intent requires no clarification when all entities present")
    void testClearTopIntent() {
        List<RuleBasedClassifier.CandidateScore> scores = List.of(
                new RuleBasedClassifier.CandidateScore(AgriIntent.MARKET_PRICE_INQUIRY, 0.85, 0.8, 1.0, 0.8),
                new RuleBasedClassifier.CandidateScore(AgriIntent.MARKET_TREND_INQUIRY, 0.50, 0.4, 0.5, 0.4)
        );
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("tomato"), List.of("guntur"), List.of(), List.of(), Collections.emptyMap()
        );

        IntentDisambiguator.DisambiguationResult result = disambiguator.disambiguate(scores, entities);
        assertEquals(AgriIntent.MARKET_PRICE_INQUIRY, result.selectedIntent());
        assertFalse(result.clarification().required());
    }

    @Test
    @DisplayName("Tie within 0.05 threshold triggers clarification with both candidate intents")
    void testTieTriggersClarification() {
        List<RuleBasedClassifier.CandidateScore> scores = List.of(
                new RuleBasedClassifier.CandidateScore(AgriIntent.PEST_MANAGEMENT, 0.72, 0.7, 0.8, 0.6),
                new RuleBasedClassifier.CandidateScore(AgriIntent.PLANT_DISEASE_INQUIRY, 0.70, 0.7, 0.8, 0.6)
        );
        ExtractedEntitiesDto entities = new ExtractedEntitiesDto(
                List.of("cotton"), List.of(), List.of(), List.of(), Collections.emptyMap()
        );

        IntentDisambiguator.DisambiguationResult result = disambiguator.disambiguate(scores, entities);
        assertTrue(result.clarification().required());
        assertTrue(result.clarification().candidateIntents().contains(AgriIntent.PEST_MANAGEMENT));
        assertTrue(result.clarification().candidateIntents().contains(AgriIntent.PLANT_DISEASE_INQUIRY));
        assertNotNull(result.clarification().prompt());
    }

    @Test
    @DisplayName("Market price inquiry missing crop entity requests clarification")
    void testMissingCropEntityClarification() {
        List<RuleBasedClassifier.CandidateScore> scores = List.of(
                new RuleBasedClassifier.CandidateScore(AgriIntent.MARKET_PRICE_INQUIRY, 0.80, 0.8, 0.8, 0.0)
        );
        ExtractedEntitiesDto entities = ExtractedEntitiesDto.empty();

        IntentDisambiguator.DisambiguationResult result = disambiguator.disambiguate(scores, entities);
        assertTrue(result.clarification().required());
        assertTrue(result.clarification().missingEntities().contains("crop"));
    }

    @Test
    @DisplayName("Low confidence (< 0.35) maps to UNSUPPORTED_OR_UNCLEAR")
    void testLowConfidenceUnsupported() {
        List<RuleBasedClassifier.CandidateScore> scores = List.of(
                new RuleBasedClassifier.CandidateScore(AgriIntent.CROP_SELECTION, 0.25, 0.2, 0.2, 0.0)
        );

        IntentDisambiguator.DisambiguationResult result = disambiguator.disambiguate(scores, ExtractedEntitiesDto.empty());
        assertEquals(AgriIntent.UNSUPPORTED_OR_UNCLEAR, result.selectedIntent());
        assertTrue(result.clarification().required());
    }
}

