package com.farmlink.api.service;

import com.farmlink.api.dto.SmartSellingDecisionResponse.MarketCard;
import com.farmlink.api.dto.SmartSellingDecisionResponse.TradeOffItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SmartSellingTradeOffEngine {

    public List<TradeOffItem> evaluateTradeOffs(List<MarketCard> rankedCards) {
        List<TradeOffItem> tradeOffs = new ArrayList<>();
        if (rankedCards == null || rankedCards.size() < 2) {
            return tradeOffs;
        }

        MarketCard primary = rankedCards.get(0);

        for (int i = 1; i < rankedCards.size(); i++) {
            MarketCard secondary = rankedCards.get(i);

            // 1. Raw Price vs Net Realization / Transport Cost Trade-Off
            if (secondary.getSelectedPrice() != null && primary.getSelectedPrice() != null) {
                // Secondary has higher raw price, but primary has higher net realization
                if (secondary.getSelectedPrice().compareTo(primary.getSelectedPrice()) > 0) {
                    BigDecimal netDiff = primary.getEstimatedNetRealization() != null && secondary.getEstimatedNetRealization() != null
                            ? primary.getEstimatedNetRealization().subtract(secondary.getEstimatedNetRealization())
                            : primary.getSelectedPrice().subtract(secondary.getSelectedPrice());

                    BigDecimal transportDiff = secondary.getTotalSellingCost() != null && primary.getTotalSellingCost() != null
                            ? secondary.getTotalSellingCost().subtract(primary.getTotalSellingCost())
                            : BigDecimal.ZERO;

                    String desc = "Market " + secondary.getMarket().getName() + " offers a higher raw price (₹"
                            + secondary.getSelectedPrice() + ") than " + primary.getMarket().getName() + " (₹" + primary.getSelectedPrice()
                            + "), but higher transport/selling costs (₹" + secondary.getTotalSellingCost() + " vs ₹" + primary.getTotalSellingCost()
                            + ") reduce its final return, making " + primary.getMarket().getName() + " ₹" + netDiff.abs() + " higher in net realization.";

                    tradeOffs.add(new TradeOffItem(
                            primary.getMarket().getId(), primary.getMarket().getName(),
                            secondary.getMarket().getId(), secondary.getMarket().getName(),
                            "HIGHER_PRICE_HIGHER_TRANSPORT",
                            "Higher Price vs Transportation Cost",
                            desc,
                            netDiff
                    ));
                }
            }

            // 2. Freshness Trade-Off (Secondary has higher gross margin, but stale price)
            if (secondary.isStalePrice() && !primary.isStalePrice()) {
                if (secondary.getSelectedPrice() != null && primary.getSelectedPrice() != null
                        && secondary.getSelectedPrice().compareTo(primary.getSelectedPrice()) >= 0) {
                    String desc = "Market " + secondary.getMarket().getName() + " exhibits a high price (₹"
                            + secondary.getSelectedPrice() + "), but relies on stale data from " + secondary.getPriceDate()
                            + ". " + primary.getMarket().getName() + " is recommended based on fresh verified data.";

                    tradeOffs.add(new TradeOffItem(
                            primary.getMarket().getId(), primary.getMarket().getName(),
                            secondary.getMarket().getId(), secondary.getMarket().getName(),
                            "STALE_PRICE_HIGHER_MARGIN",
                            "Price Freshness vs Margin",
                            desc,
                            BigDecimal.ZERO
                    ));
                }
            }

            // 3. Trend Momentum Trade-Off (Primary FALLING vs Secondary RISING)
            if ("FALLING".equalsIgnoreCase(primary.getTrendDirection()) && "RISING".equalsIgnoreCase(secondary.getTrendDirection())) {
                String desc = "Market " + primary.getMarket().getName() + " currently has higher net realization, but exhibits a FALLING 30-day trend. Market "
                        + secondary.getMarket().getName() + " is exhibiting a RISING trend momentum.";

                tradeOffs.add(new TradeOffItem(
                        primary.getMarket().getId(), primary.getMarket().getName(),
                        secondary.getMarket().getId(), secondary.getMarket().getName(),
                        "TREND_DIVERGENCE",
                        "Trend Momentum Divergence",
                        desc,
                        BigDecimal.ZERO
                ));
            }
        }

        return tradeOffs;
    }
}
