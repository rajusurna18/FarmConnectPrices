package com.farmlink.api.service;

import com.farmlink.api.dto.SmartSellingDecisionResponse.MarketCard;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class SmartSellingRankingEngine {

    public void rankMarketCards(List<MarketCard> cards, boolean hasComparableEconomics) {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        Comparator<MarketCard> comparator;

        if (hasComparableEconomics) {
            comparator = Comparator
                    .comparing(MarketCard::getEstimatedNetRealization, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MarketCard::getEstimatedNetProfit, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MarketCard::getSelectedPrice, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MarketCard::isStalePrice) // false (fresh) before true (stale)
                    .thenComparing(MarketCard::getPriceDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(card -> getTrendPriority(card.getTrendDirection()), Comparator.reverseOrder())
                    .thenComparing(MarketCard::getTotalSellingCost, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(card -> card.getMarket() != null ? card.getMarket().getId() : "", Comparator.naturalOrder());
        } else {
            comparator = Comparator
                    .comparing(MarketCard::getSelectedPrice, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(MarketCard::isStalePrice)
                    .thenComparing(MarketCard::getPriceDate, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(card -> getTrendPriority(card.getTrendDirection()), Comparator.reverseOrder())
                    .thenComparing(card -> card.getMarket() != null ? card.getMarket().getId() : "", Comparator.naturalOrder());
        }

        cards.sort(comparator);

        // Assign ranks 1 to N
        for (int i = 0; i < cards.size(); i++) {
            cards.get(i).setRank(i + 1);
        }
    }

    public String computeOverallConfidence(List<MarketCard> rankedCards, boolean hasComparableEconomics) {
        if (rankedCards == null || rankedCards.isEmpty()) {
            return "LOW";
        }

        MarketCard topCard = rankedCards.get(0);
        if (topCard.getSelectedPrice() == null || topCard.getSelectedPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "LOW";
        }

        if (topCard.isStalePrice()) {
            return "MEDIUM";
        }

        if (hasComparableEconomics) {
            return "HIGH";
        }

        return "MEDIUM"; // Fresh price without full economics
    }

    private int getTrendPriority(String trendDirection) {
        if (trendDirection == null) return 0;
        switch (trendDirection.toUpperCase()) {
            case "RISING":
                return 3;
            case "STABLE":
                return 2;
            case "FALLING":
                return 1;
            default:
                return 0;
        }
    }
}
