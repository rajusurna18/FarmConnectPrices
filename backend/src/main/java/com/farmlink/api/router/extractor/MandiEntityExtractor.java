package com.farmlink.api.router.extractor;

import com.farmlink.api.dto.MarketSummaryResponse;
import com.farmlink.api.service.MarketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MandiEntityExtractor implements AgriculturalEntityExtractor<String> {

    private static final Logger logger = LoggerFactory.getLogger(MandiEntityExtractor.class);

    private static final List<String> CANONICAL_MANDIS = List.of(
            "mandi", "apmc", "rythu bazaar", "rythu bazar", "grain market", "sabzi mandi",
            "azadpur", "bowenpally", "vashi", "guntur mandi", "warangal mandi", "khammam mandi",
            "lasalgaon", "khanna", "gadag", "gulbarga", "doddaballapur"
    );

    private final MarketService marketService;

    public MandiEntityExtractor(@Autowired(required = false) MarketService marketService) {
        this.marketService = marketService;
    }

    @Override
    public List<String> extract(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> knownMandis = new LinkedHashSet<>(CANONICAL_MANDIS);
        if (marketService != null) {
            try {
                List<MarketSummaryResponse> dbMarkets = marketService.getMarkets(null, null, null, null, null, null, 100);
                if (dbMarkets != null) {
                    for (MarketSummaryResponse m : dbMarkets) {
                        if (m.getName() != null && !m.getName().isBlank()) {
                            knownMandis.add(m.getName().toLowerCase().trim());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not fetch markets from MarketService: {}", e.getMessage());
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> sortedMandis = new ArrayList<>(knownMandis);
        sortedMandis.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String mandi : sortedMandis) {
            Pattern pattern = Pattern.compile("(?i)\\b" + Pattern.quote(mandi) + "\\b");
            Matcher matcher = pattern.matcher(query);
            if (matcher.find()) {
                if (!matched.contains(mandi)) {
                    matched.add(mandi);
                }
            }
        }

        return Collections.unmodifiableList(matched);
    }
}
