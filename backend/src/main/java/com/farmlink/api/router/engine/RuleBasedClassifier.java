package com.farmlink.api.router.engine;

import com.farmlink.api.router.dto.ExtractedEntitiesDto;
import com.farmlink.api.router.entity.AgriIntent;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class RuleBasedClassifier {

    public record CandidateScore(
            AgriIntent intent,
            double score,
            double keywordScore,
            double patternScore,
            double entityScore
    ) {}

    private static final Map<AgriIntent, List<Pattern>> PRIMARY_PATTERNS = new EnumMap<>(AgriIntent.class);
    private static final Map<AgriIntent, List<String>> KEYWORDS = new EnumMap<>(AgriIntent.class);

    static {
        // CROP_SUITABILITY
        PRIMARY_PATTERNS.put(AgriIntent.CROP_SUITABILITY, List.of(
                Pattern.compile("(?i).*\\b(?:suitable|suitability|can\\s+i\\s+grow|fit\\s+for|ideal\\s+for)\\b.*"),
                Pattern.compile("(?i).*\\b(?:grow\\s+in\\s+(?:black|red|sandy|clay|loamy)\\s+soil)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.CROP_SUITABILITY, List.of(
                "suitable", "suitability", "can i grow", "soil suitability", "climate suitability"
        ));

        // CROP_SELECTION
        PRIMARY_PATTERNS.put(AgriIntent.CROP_SELECTION, List.of(
                Pattern.compile("(?i).*\\b(?:which\\s+crop|best\\s+crop|what\\s+to\\s+grow|crop\\s+selection)\\b.*"),
                Pattern.compile("(?i).*\\b(?:what\\s+should\\s+i\\s+(?:plant|grow))\\b.*")
        ));
        KEYWORDS.put(AgriIntent.CROP_SELECTION, List.of(
                "which crop", "best crop", "what to grow", "profitable crop", "crop selection"
        ));

        // SOIL_MANAGEMENT
        PRIMARY_PATTERNS.put(AgriIntent.SOIL_MANAGEMENT, List.of(
                Pattern.compile("(?i).*\\b(?:soil\\s+(?:health|test|testing|ph|fertility|erosion|salinity|texture))\\b.*"),
                Pattern.compile("(?i).*\\b(?:how\\s+to\\s+improve\\s+soil)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.SOIL_MANAGEMENT, List.of(
                "soil health", "soil test", "soil ph", "soil fertility", "saline soil", "alkaline soil"
        ));

        // FERTILIZER_NUTRIENT
        PRIMARY_PATTERNS.put(AgriIntent.FERTILIZER_NUTRIENT, List.of(
                Pattern.compile("(?i).*\\b(?:fertilizer|nutrient|urea|dap|npk|potash|micronutrient|manure|compost)\\b.*"),
                Pattern.compile("(?i).*\\b(?:dosage|dose|how\\s+much\\s+urea|fertilizer\\s+schedule)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.FERTILIZER_NUTRIENT, List.of(
                "fertilizer", "nutrient", "urea", "dap", "npk", "potash", "dosage", "manure"
        ));

        // IRRIGATION_MANAGEMENT
        PRIMARY_PATTERNS.put(AgriIntent.IRRIGATION_MANAGEMENT, List.of(
                Pattern.compile("(?i).*\\b(?:irrigation|water|watering|drip|sprinkler|borewell)\\b.*"),
                Pattern.compile("(?i).*\\b(?:how\\s+often\\s+to\\s+water|water\\s+requirement|irrigation\\s+schedule)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.IRRIGATION_MANAGEMENT, List.of(
                "irrigation", "water", "watering", "drip irrigation", "sprinkler", "water requirement"
        ));

        // PEST_MANAGEMENT
        PRIMARY_PATTERNS.put(AgriIntent.PEST_MANAGEMENT, List.of(
                Pattern.compile("(?i).*\\b(?:pest|insect|caterpillar|borer|aphid|whitefly|thrips|locust)\\b.*"),
                Pattern.compile("(?i).*\\b(?:spray\\s+for|control\\s+pest|kill\\s+insects|pesticide|insecticide)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.PEST_MANAGEMENT, List.of(
                "pest", "insect", "borer", "spray", "pesticide", "insecticide", "caterpillar", "infestation"
        ));

        // PLANT_DISEASE_INQUIRY
        PRIMARY_PATTERNS.put(AgriIntent.PLANT_DISEASE_INQUIRY, List.of(
                Pattern.compile("(?i).*\\b(?:disease|fungus|fungal|blight|wilt|rot|mildew|leaf\\s+curl|yellow\\s+leaves|spots)\\b.*"),
                Pattern.compile("(?i).*\\b(?:leaves\\s+turning|infected|fungicide|cure\\s+disease)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.PLANT_DISEASE_INQUIRY, List.of(
                "disease", "blight", "wilt", "rot", "mildew", "leaf curl", "yellow leaves", "fungicide"
        ));

        // CROP_CULTIVATION_PRACTICES
        PRIMARY_PATTERNS.put(AgriIntent.CROP_CULTIVATION_PRACTICES, List.of(
                Pattern.compile("(?i).*\\b(?:sowing|planting|spacing|nursery|transplanting|pruning|weeding|cultivation)\\b.*"),
                Pattern.compile("(?i).*\\b(?:how\\s+to\\s+(?:grow|sow|plant|cultivate))\\b.*")
        ));
        KEYWORDS.put(AgriIntent.CROP_CULTIVATION_PRACTICES, List.of(
                "cultivation", "sowing", "planting", "spacing", "nursery", "transplanting", "pruning", "weeding"
        ));

        // CROP_ROTATION
        PRIMARY_PATTERNS.put(AgriIntent.CROP_ROTATION, List.of(
                Pattern.compile("(?i).*\\b(?:crop\\s+rotation|rotate\\s+crop|what\\s+to\\s+grow\\s+after|next\\s+crop)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.CROP_ROTATION, List.of(
                "crop rotation", "rotate", "grow after", "next crop", "subsequent crop"
        ));

        // HARVEST_POST_HARVEST
        PRIMARY_PATTERNS.put(AgriIntent.HARVEST_POST_HARVEST, List.of(
                Pattern.compile("(?i).*\\b(?:harvest|harvesting|storage|store|drying|post\\s+harvest|shelf\\s+life|cold\\s+storage)\\b.*"),
                Pattern.compile("(?i).*\\b(?:when\\s+to\\s+harvest|how\\s+to\\s+store)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.HARVEST_POST_HARVEST, List.of(
                "harvest", "harvesting", "storage", "drying", "cold storage", "shelf life", "post harvest"
        ));

        // WEATHER_INQUIRY
        PRIMARY_PATTERNS.put(AgriIntent.WEATHER_INQUIRY, List.of(
                Pattern.compile("(?i).*\\b(?:weather|rain|rainfall|monsoon|temperature|humidity|forecast|climate)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.WEATHER_INQUIRY, List.of(
                "weather", "rain", "rainfall", "monsoon", "temperature", "forecast", "cloudy"
        ));

        // MARKET_PRICE_INQUIRY
        PRIMARY_PATTERNS.put(AgriIntent.MARKET_PRICE_INQUIRY, List.of(
                Pattern.compile("(?i).*\\b(?:price|rate|cost|modal\\s+price|mandi\\s+price|market\\s+rate)\\b.*"),
                Pattern.compile("(?i).*\\b(?:what\\s+is\\s+the\\s+(?:price|rate)\\s+of)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.MARKET_PRICE_INQUIRY, List.of(
                "price", "rate", "modal price", "mandi price", "market price", "current rate"
        ));

        // MARKET_TREND_INQUIRY
        PRIMARY_PATTERNS.put(AgriIntent.MARKET_TREND_INQUIRY, List.of(
                Pattern.compile("(?i).*\\b(?:trend|trending|future\\s+price|price\\s+forecast|price\\s+movement|increasing|falling|rise|drop)\\b.*"),
                Pattern.compile("(?i).*\\b(?:will\\s+price\\s+(?:go\\s+up|increase|fall|decrease))\\b.*")
        ));
        KEYWORDS.put(AgriIntent.MARKET_TREND_INQUIRY, List.of(
                "trend", "price trend", "increasing", "falling", "future price", "price prediction", "price outlook"
        ));

        // FARM_ECONOMICS_INQUIRY
        PRIMARY_PATTERNS.put(AgriIntent.FARM_ECONOMICS_INQUIRY, List.of(
                Pattern.compile("(?i).*\\b(?:my\\s+farm|my\\s+crop|my\\s+profit|my\\s+loss|my\\s+cost|my\\s+expense|my\\s+yield|my\\s+revenue|my\\s+income)\\b.*"),
                Pattern.compile("(?i).*\\b(?:cost\\s+of\\s+cultivation\\s+per\\s+acre|farm\\s+budget|profit\\s+calculation)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.FARM_ECONOMICS_INQUIRY, List.of(
                "my profit", "my cost", "my yield", "my income", "my expenses", "farm economics", "profit per acre"
        ));

        // MARKETPLACE_DISCOVERY
        PRIMARY_PATTERNS.put(AgriIntent.MARKETPLACE_DISCOVERY, List.of(
                Pattern.compile("(?i).*\\b(?:buyer|buyers|seller|sell\\s+my|where\\s+to\\s+sell|connect\\s+with\\s+buyer|marketplace|traders)\\b.*")
        ));
        KEYWORDS.put(AgriIntent.MARKETPLACE_DISCOVERY, List.of(
                "buyer", "buyers", "sell", "where to sell", "find buyers", "marketplace", "traders"
        ));

        // GENERAL_AGRI_KNOWLEDGE
        PRIMARY_PATTERNS.put(AgriIntent.GENERAL_AGRI_KNOWLEDGE, List.of(
                Pattern.compile("(?i).*\\b(?:what\\s+is\\s+(?:organic\\s+farming|hydroponics|polyhouse|greenhouse|mulching|permaculture|apiculture|sericulture))\\b.*"),
                Pattern.compile("(?i).*\\b(?:explain\\s+(?:organic\\s+farming|hydroponics|drip\\s+irrigation))\\b.*")
        ));
        KEYWORDS.put(AgriIntent.GENERAL_AGRI_KNOWLEDGE, List.of(
                "organic farming", "hydroponics", "polyhouse", "mulching", "permaculture", "explain"
        ));

        // UNSUPPORTED_OR_UNCLEAR
        PRIMARY_PATTERNS.put(AgriIntent.UNSUPPORTED_OR_UNCLEAR, List.of(
                Pattern.compile("(?i)^(?:hi|hello|hey|namaste|good\\s+morning|who\\s+are\\s+you|what\\s+can\\s+you\\s+do|thanks|thank\\s+you)\\s*[.?!]*$")
        ));
        KEYWORDS.put(AgriIntent.UNSUPPORTED_OR_UNCLEAR, List.of(
                "hi", "hello", "hey", "namaste", "thanks", "who are you"
        ));
    }

    public List<CandidateScore> classify(String query, ExtractedEntitiesDto entities) {
        if (query == null || query.isBlank()) {
            return List.of(new CandidateScore(AgriIntent.UNSUPPORTED_OR_UNCLEAR, 1.0, 0, 1.0, 0));
        }

        String lowerQuery = query.toLowerCase();
        List<CandidateScore> scores = new ArrayList<>();

        for (AgriIntent intent : AgriIntent.values()) {
            double keywordScore = calculateKeywordScore(intent, lowerQuery);
            double patternScore = calculatePatternScore(intent, query);
            double entityScore = calculateEntityScore(intent, entities);
            double penalty = calculatePenalty(intent, lowerQuery, query);

            double rawScore = (0.40 * patternScore) + (0.35 * keywordScore) + (0.25 * entityScore) - penalty;
            double finalScore = Math.max(0.0, Math.min(1.0, rawScore));
            finalScore = round(finalScore);

            scores.add(new CandidateScore(intent, finalScore, keywordScore, patternScore, entityScore));
        }

        scores.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scores;
    }

    private double calculateKeywordScore(AgriIntent intent, String lowerQuery) {
        List<String> words = KEYWORDS.get(intent);
        if (words == null || words.isEmpty()) {
            return 0.0;
        }

        int matches = 0;
        for (String w : words) {
            if (lowerQuery.contains(w.toLowerCase())) {
                matches++;
            }
        }

        return Math.min(1.0, (double) matches / Math.min(words.size(), 3));
    }

    private double calculatePatternScore(AgriIntent intent, String query) {
        List<Pattern> patterns = PRIMARY_PATTERNS.get(intent);
        if (patterns == null || patterns.isEmpty()) {
            return 0.0;
        }

        for (Pattern p : patterns) {
            if (p.matcher(query).matches()) {
                return 1.0;
            }
            if (p.matcher(query).find()) {
                return 0.8;
            }
        }
        return 0.0;
    }

    private double calculateEntityScore(AgriIntent intent, ExtractedEntitiesDto entities) {
        if (entities == null) {
            return 0.0;
        }

        return switch (intent) {
            case MARKET_PRICE_INQUIRY, MARKET_TREND_INQUIRY -> {
                double score = 0.0;
                if (!entities.crops().isEmpty()) score += 0.6;
                if (!entities.mandis().isEmpty() || !entities.locations().isEmpty()) score += 0.4;
                yield score;
            }
            case CROP_SUITABILITY, CROP_SELECTION, CROP_ROTATION -> {
                double score = 0.0;
                if (!entities.crops().isEmpty()) score += 0.7;
                if (!entities.locations().isEmpty()) score += 0.3;
                yield score;
            }
            case FERTILIZER_NUTRIENT, PEST_MANAGEMENT, PLANT_DISEASE_INQUIRY, IRRIGATION_MANAGEMENT,
                 CROP_CULTIVATION_PRACTICES, HARVEST_POST_HARVEST, FARM_ECONOMICS_INQUIRY -> {
                yield !entities.crops().isEmpty() ? 0.8 : 0.2;
            }
            case WEATHER_INQUIRY -> !entities.locations().isEmpty() ? 1.0 : 0.3;
            case MARKETPLACE_DISCOVERY -> {
                double score = 0.0;
                if (!entities.crops().isEmpty()) score += 0.5;
                if (!entities.quantities().isEmpty()) score += 0.5;
                yield score;
            }
            default -> 0.0;
        };
    }

    private double calculatePenalty(AgriIntent intent, String lowerQuery, String query) {
        // Penalty for conflicting terms
        if (intent == AgriIntent.MARKET_PRICE_INQUIRY) {
            if (lowerQuery.contains("trend") || lowerQuery.contains("future") || lowerQuery.contains("increasing") || lowerQuery.contains("falling")) {
                return 0.30; // Better fits MARKET_TREND_INQUIRY
            }
            if (lowerQuery.contains("my profit") || lowerQuery.contains("my cost") || lowerQuery.contains("my yield")) {
                return 0.40; // Better fits FARM_ECONOMICS_INQUIRY
            }
        }
        if (intent == AgriIntent.MARKET_TREND_INQUIRY) {
            if (!lowerQuery.contains("trend") && !lowerQuery.contains("forecast") && !lowerQuery.contains("increasing") && !lowerQuery.contains("future") && !lowerQuery.contains("falling")) {
                return 0.30;
            }
        }
        if (intent == AgriIntent.PEST_MANAGEMENT) {
            if (lowerQuery.contains("yellow leaves") || lowerQuery.contains("wilt") || lowerQuery.contains("fungus") || lowerQuery.contains("disease")) {
                return 0.25; // Better fits PLANT_DISEASE_INQUIRY
            }
        }
        if (intent == AgriIntent.PLANT_DISEASE_INQUIRY) {
            if (lowerQuery.contains("caterpillar") || lowerQuery.contains("borer") || lowerQuery.contains("locust") || lowerQuery.contains("insect")) {
                return 0.25; // Better fits PEST_MANAGEMENT
            }
        }
        return 0.0;
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}

