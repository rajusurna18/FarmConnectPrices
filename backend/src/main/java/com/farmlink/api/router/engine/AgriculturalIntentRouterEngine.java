package com.farmlink.api.router.engine;

import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.dto.RouterTurnResponse;
import com.farmlink.api.router.entity.AgriIntent;
import com.farmlink.api.router.entity.ConfidenceBand;
import com.farmlink.api.router.extractor.CompositeAgriculturalEntityExtractor;
import com.farmlink.api.router.security.EconomicDataPrivacyGuard;
import com.farmlink.api.router.security.UserDomainRole;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgriculturalIntentRouterEngine {

    private final CompositeAgriculturalEntityExtractor entityExtractor;
    private final RuleBasedClassifier classifier;
    private final IntentDisambiguator disambiguator;
    private final EconomicDataPrivacyGuard privacyGuard;

    public AgriculturalIntentRouterEngine(
            CompositeAgriculturalEntityExtractor entityExtractor,
            RuleBasedClassifier classifier,
            IntentDisambiguator disambiguator,
            EconomicDataPrivacyGuard privacyGuard) {
        this.entityExtractor = entityExtractor;
        this.classifier = classifier;
        this.disambiguator = disambiguator;
        this.privacyGuard = privacyGuard;
    }

    public RouterTurnResponse processTurn(
            String sessionId,
            long turnIndex,
            String query,
            UserDomainRole callerRole,
            String authenticatedUid,
            String targetOwnerUid) {

        // 1. Entity Extraction
        ExtractedEntitiesDto entities = entityExtractor.extractEntities(query);

        // 2. Intent Scoring
        List<RuleBasedClassifier.CandidateScore> scores = classifier.classify(query, entities);

        // 3. Disambiguation
        IntentDisambiguator.DisambiguationResult disambiguation = disambiguator.disambiguate(scores, entities);
        AgriIntent selectedIntent = disambiguation.selectedIntent();
        double confidence = disambiguation.confidence();
        ConfidenceBand confidenceBand = ConfidenceBand.fromScore(confidence);

        // 4. Privacy & Authorization Guard
        privacyGuard.enforcePrivacyBoundaries(selectedIntent, callerRole, authenticatedUid, targetOwnerUid);

        return new RouterTurnResponse(
                sessionId,
                turnIndex,
                selectedIntent,
                confidence,
                confidenceBand,
                entities,
                disambiguation.clarification(),
                true,
                false
        );
    }
}

